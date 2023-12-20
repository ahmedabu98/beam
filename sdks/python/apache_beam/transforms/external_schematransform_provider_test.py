#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import logging
import os
import shutil
import unittest

import pytest

import apache_beam as beam
import yaml
from apache_beam.testing.test_pipeline import TestPipeline
from apache_beam.testing.util import assert_that
from apache_beam.testing.util import equal_to
from apache_beam.transforms.external import BeamJarExpansionService
from apache_beam.transforms.external_schematransform_provider import STANDARD_URN_PATTERN
from apache_beam.transforms.external_schematransform_provider import ExternalSchemaTransform
from apache_beam.transforms.external_schematransform_provider import ExternalSchemaTransformProvider
from apache_beam.transforms.external_schematransform_provider import camel_case_to_snake_case
from apache_beam.transforms.external_schematransform_provider import infer_name_from_identifier
from apache_beam.transforms.external_schematransform_provider import snake_case_to_lower_camel_case
from apache_beam.transforms.external_schematransform_provider import snake_case_to_upper_camel_case
from gen_xlang_wrappers import PYTHON_SUFFIX
from gen_xlang_wrappers import delete_generated_files
from gen_xlang_wrappers import generate_transform_configs
from gen_xlang_wrappers import get_wrappers_from_transform_configs
from gen_xlang_wrappers import run_script
from gen_xlang_wrappers import write_wrappers_to_destinations


class NameUtilsTest(unittest.TestCase):
  def test_snake_case_to_upper_camel_case(self):
    test_cases = [("", ""), ("test", "Test"), ("test_name", "TestName"),
                  ("test_double_underscore", "TestDoubleUnderscore"),
                  ("TEST_CAPITALIZED", "TestCapitalized"),
                  ("_prepended_underscore", "PrependedUnderscore"),
                  ("appended_underscore_", "AppendedUnderscore")]
    for case in test_cases:
      self.assertEqual(case[1], snake_case_to_upper_camel_case(case[0]))

  def test_snake_case_to_lower_camel_case(self):
    test_cases = [("", ""), ("test", "test"), ("test_name", "testName"),
                  ("test_double_underscore", "testDoubleUnderscore"),
                  ("TEST_CAPITALIZED", "testCapitalized"),
                  ("_prepended_underscore", "prependedUnderscore"),
                  ("appended_underscore_", "appendedUnderscore")]
    for case in test_cases:
      self.assertEqual(case[1], snake_case_to_lower_camel_case(case[0]))

  def test_camel_case_to_snake_case(self):
    test_cases = [("", ""), ("Test", "test"), ("TestName", "test_name"),
                  ("TestDoubleUnderscore",
                   "test_double_underscore"), ("MyToLoFo", "my_to_lo_fo"),
                  ("BEGINNINGAllCaps",
                   "beginning_all_caps"), ("AllCapsENDING", "all_caps_ending"),
                  ("AllCapsMIDDLEWord", "all_caps_middle_word"),
                  ("lowerCamelCase", "lower_camel_case")]
    for case in test_cases:
      self.assertEqual(case[1], camel_case_to_snake_case(case[0]))

  def test_infer_name_from_identifier(self):
    standard_test_cases = [
        ("beam:schematransform:org.apache.beam:transform:v1", "Transform"),
        ("beam:schematransform:org.apache.beam:my_transform:v1",
         "MyTransform"), (
             "beam:schematransform:org.apache.beam:my_transform:v2",
             "MyTransformV2"),
        ("beam:schematransform:org.apache.beam:fe_fi_fo_fum:v2", "FeFiFoFumV2"),
        ("beam:schematransform:bad_match:my_transform:v1", None)
    ]
    for case in standard_test_cases:
      self.assertEqual(
          case[1], infer_name_from_identifier(case[0], STANDARD_URN_PATTERN))

    custom_pattern_cases = [
        # (<pattern>, <urn>, <expected output>)
        (
            r"^custom:transform:([\w-]+):(\w+)$",
            "custom:transform:my_transform:v1",
            "MyTransformV1"),
        (
            r"^org.user:([\w-]+):([\w-]+):([\w-]+):external$",
            "org.user:some:custom_transform:we_made:external",
            "SomeCustomTransformWeMade"),
        (
            r"^([\w-]+):user.transforms",
            "my_eXTErnal:user.transforms",
            "MyExternal"),
        (r"^([\w-]+):user.transforms", "my_external:badinput.transforms", None),
    ]
    for case in custom_pattern_cases:
      self.assertEqual(case[2], infer_name_from_identifier(case[1], case[0]))


@pytest.mark.uses_io_java_expansion_service
@unittest.skipUnless(
    os.environ.get('EXPANSION_PORT'),
    "EXPANSION_PORT environment var is not provided.")
class ExternalSchemaTransformProviderTest(unittest.TestCase):
  def setUp(self):
    self.test_pipeline = TestPipeline(is_integration_test=True)

  def test_generate_sequence_config_schema_and_description(self):
    provider = ExternalSchemaTransformProvider(
        BeamJarExpansionService(":sdks:java:io:expansion-service:shadowJar"))

    self.assertTrue((
        'GenerateSequence',
        'beam:schematransform:org.apache.beam:generate_sequence:v1'
    ) in provider.get_available())

    GenerateSequence = provider.get('GenerateSequence')
    config_schema = GenerateSequence.configuration_schema
    for param in ['start', 'end', 'rate']:
      self.assertTrue(param in config_schema)

    description_substring = (
        "Outputs a PCollection of Beam Rows, each "
        "containing a single INT64")
    self.assertTrue(description_substring in GenerateSequence.description)

  def test_run_generate_sequence(self):
    provider = ExternalSchemaTransformProvider(
        BeamJarExpansionService(":sdks:java:io:expansion-service:shadowJar"))

    with beam.Pipeline() as p:
      numbers = p | provider.GenerateSequence(
          start=0, end=10) | beam.Map(lambda row: row.value)

      assert_that(numbers, equal_to([i for i in range(10)]))


@pytest.mark.uses_io_java_expansion_service
# @unittest.skipUnless(
#   os.environ.get('EXPANSION_PORT'),
#   "EXPANSION_PORT environment var is not provided.")
class AutoGenerationScriptTest(unittest.TestCase):
  """
  This class tests the generation and regeneration operations in
  `sdks/python/gen_xlang_wrappers.py`.
  """
  TEST_DIR = os.path.join(
      os.path.abspath(os.path.dirname(__file__)), 'test_gen_script')
  SERVICE_CONFIG_PATH = os.path.join(
      TEST_DIR, "test_expansion_service_config.yaml")
  TRANSFORMS_CONFIG_PATH = os.path.join(TEST_DIR, "test_transform_config.yaml")
  # tests cases will use GenerateSequence
  GEN_SEQ_IDENTIFIER = \
    'beam:schematransform:org.apache.beam:generate_sequence:v1'

  def setUp(self):
    shutil.rmtree(self.TEST_DIR, ignore_errors=True)
    os.mkdir(self.TEST_DIR)

  def tearDown(self):
    shutil.rmtree(self.TEST_DIR)

  def test_script_workflow(self):
    expansion_service_config = {
        "gradle_target": 'sdks:java:io:expansion-service:shadowJar',
        'destinations': {
            'python': 'apache_beam/transforms/test_gen_script'
        }
    }
    with open(self.SERVICE_CONFIG_PATH, 'w') as f:
      yaml.dump([expansion_service_config], f)

    # test that transform config YAML file is created
    generate_transform_configs(
        self.SERVICE_CONFIG_PATH, self.TRANSFORMS_CONFIG_PATH)
    self.assertTrue(os.path.exists(self.TRANSFORMS_CONFIG_PATH))

    # test that transform config is populated correctly
    with open(self.TRANSFORMS_CONFIG_PATH) as f:
      transforms = yaml.safe_load(f)
      gen_seq_config = None
      for transform in transforms:
        if transform['identifier'] == self.GEN_SEQ_IDENTIFIER:
          gen_seq_config = transform
      self.assertIsNotNone(gen_seq_config)
      self.assertEqual(
          gen_seq_config['default_service'],
          expansion_service_config['gradle_target'])
      self.assertEqual(gen_seq_config['name'], 'GenerateSequence')
      self.assertEqual(
          gen_seq_config['destinations']['python'],
          'apache_beam/transforms/test_gen_script/generate_sequence')
      self.assertIn("end", gen_seq_config['fields'])
      self.assertIn("start", gen_seq_config['fields'])
      self.assertIn("rate", gen_seq_config['fields'])

    # test that the code for GenerateSequence is set to the right destination
    grouped_wrappers = get_wrappers_from_transform_configs(
        self.TRANSFORMS_CONFIG_PATH)
    self.assertIn(
        'apache_beam/transforms/test_gen_script/generate_sequence',
        grouped_wrappers)
    # only the GenerateSequence wrapper is set to this destination
    self.assertEqual(
        len(
            grouped_wrappers[
                'apache_beam/transforms/test_gen_script/generate_sequence']),
        1)

    # test that the correct destination is created
    write_wrappers_to_destinations(grouped_wrappers)
    self.assertTrue(
        os.path.exists(
            os.path.join(self.TEST_DIR, 'generate_sequence' + PYTHON_SUFFIX)))
    # check the wrapper exists in this destination and has correct properties
    from .test_gen_script.generate_sequence_et import GenerateSequence
    self.assertTrue(
        isinstance(GenerateSequence(start=0), ExternalSchemaTransform))
    self.assertEqual(GenerateSequence.identifier, self.GEN_SEQ_IDENTIFIER)

    # test that we successfully delete the destination
    delete_generated_files(self.TEST_DIR)
    self.assertFalse(
        os.path.exists(
            os.path.join(self.TEST_DIR, 'generate_sequence' + PYTHON_SUFFIX)))

  def test_script_workflow_with_modified_transforms(self):
    modified_name = 'ModifiedSequence'
    modified_dest = \
      'apache_beam/transforms/test_gen_script/new_dir/modified_gen_seq'
    expansion_service_config = {
        "gradle_target": 'sdks:java:io:expansion-service:shadowJar',
        'destinations': {
            'python': 'apache_beam/transforms/test_gen_script'
        },
        'transforms': {
            'beam:schematransform:org.apache.beam:generate_sequence:v1': {
                'name': modified_name,
                'destinations': {
                    'python': modified_dest
                }
            }
        }
    }
    os.mkdir(os.path.join(self.TEST_DIR, 'new_dir'))

    with open(self.SERVICE_CONFIG_PATH, 'w') as f:
      yaml.dump([expansion_service_config], f)

    # test that transform config YAML file is successfully created
    generate_transform_configs(
        self.SERVICE_CONFIG_PATH, self.TRANSFORMS_CONFIG_PATH)
    self.assertTrue(os.path.exists(self.TRANSFORMS_CONFIG_PATH))

    # test that transform config is populated correctly
    with open(self.TRANSFORMS_CONFIG_PATH) as f:
      transforms = yaml.safe_load(f)
      gen_seq_config = None
      for transform in transforms:
        if transform['identifier'] == self.GEN_SEQ_IDENTIFIER:
          gen_seq_config = transform
      self.assertIsNotNone(gen_seq_config)
      self.assertEqual(
          gen_seq_config['default_service'],
          expansion_service_config['gradle_target'])
      self.assertEqual(gen_seq_config['name'], modified_name)
      self.assertEqual(gen_seq_config['destinations']['python'], modified_dest)

    # test that the code for 'ModifiedSequence' is set to the right destination
    grouped_wrappers = get_wrappers_from_transform_configs(
        self.TRANSFORMS_CONFIG_PATH)
    self.assertIn(modified_dest, grouped_wrappers)
    self.assertIn(modified_name, grouped_wrappers[modified_dest][0])
    # only one wrapper is set to this destination
    self.assertEqual(len(grouped_wrappers[modified_dest]), 1)

    # test that the modified destination is successfully created
    write_wrappers_to_destinations(grouped_wrappers)
    self.assertTrue(
        os.path.exists(
            os.path.join(
                self.TEST_DIR, 'new_dir', 'modified_gen_seq' + PYTHON_SUFFIX)))
    # check the modified wrapper exists in the modified destination
    # and check it has the correct properties
    from .test_gen_script.new_dir.modified_gen_seq_et import ModifiedSequence
    self.assertTrue(
        isinstance(ModifiedSequence(start=0), ExternalSchemaTransform))
    self.assertEqual(ModifiedSequence.identifier, self.GEN_SEQ_IDENTIFIER)

    # test that we successfully delete the destination
    delete_generated_files(self.TEST_DIR)
    self.assertFalse(
        os.path.exists(
            os.path.join(
                self.TEST_DIR, 'new_dir', 'modified_gen_seq' + PYTHON_SUFFIX)))

  def test_script_workflow_with_multiple_wrappers_same_destination(self):
    modified_dest = 'apache_beam/transforms/test_gen_script/my_wrappers'
    expansion_service_config = {
        "gradle_target": 'sdks:java:io:expansion-service:shadowJar',
        'destinations': {
            'python': 'apache_beam/transforms/test_gen_script'
        },
        'transforms': {
            'beam:schematransform:org.apache.beam:generate_sequence:v1': {
                'destinations': {
                    'python': modified_dest
                }
            },
            'beam:schematransform:org.apache.beam:kafka_read:v1': {
                'destinations': {
                    'python': modified_dest
                }
            },
            'beam:schematransform:org.apache.beam:kafka_write:v1': {
                'destinations': {
                    'python': modified_dest
                }
            }
        }
    }

    with open(self.SERVICE_CONFIG_PATH, 'w') as f:
      yaml.dump([expansion_service_config], f)

    # test that transform config YAML file is successfully created
    generate_transform_configs(
        self.SERVICE_CONFIG_PATH, self.TRANSFORMS_CONFIG_PATH)
    self.assertTrue(os.path.exists(self.TRANSFORMS_CONFIG_PATH))

    # test that our transform configs have the same destination
    with open(self.TRANSFORMS_CONFIG_PATH) as f:
      transforms = yaml.safe_load(f)
      for transform in transforms:
        if transform['identifier'] in expansion_service_config['transforms']:
          self.assertEqual(transform['destinations']['python'], modified_dest)

    grouped_wrappers = get_wrappers_from_transform_configs(
        self.TRANSFORMS_CONFIG_PATH)
    # check all 3 wrappers are set to this destination
    self.assertEqual(len(grouped_wrappers[modified_dest]), 3)

    # write wrappers to destination then check that all 3 exist there
    write_wrappers_to_destinations(grouped_wrappers)
    from .test_gen_script import my_wrappers_et
    self.assertTrue(hasattr(my_wrappers_et, 'GenerateSequence'))
    self.assertTrue(hasattr(my_wrappers_et, 'KafkaWrite'))
    self.assertTrue(hasattr(my_wrappers_et, 'KafkaRead'))

  def test_script_workflow_with_ignored_transform(self):
    expansion_service_config = {
        "gradle_target": 'sdks:java:io:expansion-service:shadowJar',
        'destinations': {
            'python': 'apache_beam/io'
        },
        'ignore': ['beam:schematransform:org.apache.beam:generate_sequence:v1']
    }

    with open(self.SERVICE_CONFIG_PATH, 'w') as f:
      yaml.dump([expansion_service_config], f)

    generate_transform_configs(
        self.SERVICE_CONFIG_PATH, self.TRANSFORMS_CONFIG_PATH)

    # test that transform config is populated correctly
    with open(self.TRANSFORMS_CONFIG_PATH) as f:
      transforms = yaml.safe_load(f)
      gen_seq_config = None
      for transform in transforms:
        if transform['identifier'] == self.GEN_SEQ_IDENTIFIER:
          gen_seq_config = transform
      self.assertIsNone(gen_seq_config)

  def test_run_pipeline_with_script_generated_transform(self):
    expansion_service_config = {
        "gradle_target": 'sdks:java:io:expansion-service:shadowJar',
        'destinations': {
            'python': 'apache_beam/transforms/test_gen_script'
        },
        'transforms': {
            'beam:schematransform:org.apache.beam:generate_sequence:v1': {
                'name': 'MyGenSeq',
                'destinations': {
                    'python': 'apache_beam/transforms/test_gen_script/gen_seq'
                }
            }
        }
    }
    with open(self.SERVICE_CONFIG_PATH, 'w') as f:
      yaml.dump([expansion_service_config], f)

    run_script(True, self.SERVICE_CONFIG_PATH, self.TRANSFORMS_CONFIG_PATH)
    from .test_gen_script.gen_seq_et import MyGenSeq

    with beam.Pipeline() as p:
      numbers = (
          p | MyGenSeq(start=0, end=10) | beam.Map(lambda row: row.value))

      assert_that(numbers, equal_to([i for i in range(10)]))


if __name__ == '__main__':
  logging.getLogger().setLevel(logging.INFO)
  unittest.main()
