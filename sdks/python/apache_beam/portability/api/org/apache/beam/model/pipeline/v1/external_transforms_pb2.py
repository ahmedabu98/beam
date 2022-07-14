# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: org/apache/beam/model/pipeline/v1/external_transforms.proto
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from . import schema_pb2 as org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_schema__pb2
from . import beam_runner_api_pb2 as org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='org/apache/beam/model/pipeline/v1/external_transforms.proto',
  package='org.apache.beam.model.pipeline.v1',
  syntax='proto3',
  serialized_options=b'\n!org.apache.beam.model.pipeline.v1B\022ExternalTransformsZHgithub.com/apache/beam/sdks/v2/go/pkg/beam/model/pipeline_v1;pipeline_v1',
  create_key=_descriptor._internal_create_key,
  serialized_pb=b'\n;org/apache/beam/model/pipeline/v1/external_transforms.proto\x12!org.apache.beam.model.pipeline.v1\x1a.org/apache/beam/model/pipeline/v1/schema.proto\x1a\x37org/apache/beam/model/pipeline/v1/beam_runner_api.proto\"j\n\x1c\x45xternalConfigurationPayload\x12\x39\n\x06schema\x18\x01 \x01(\x0b\x32).org.apache.beam.model.pipeline.v1.Schema\x12\x0f\n\x07payload\x18\x02 \x01(\x0c\"d\n\x10\x45xpansionMethods\"P\n\x04\x45num\x12H\n\x11JAVA_CLASS_LOOKUP\x10\x00\x1a\x31\xa2\xb4\xfa\xc2\x05+beam:expansion:payload:java_class_lookup:v1\"\xf7\x01\n\x16JavaClassLookupPayload\x12\x12\n\nclass_name\x18\x01 \x01(\t\x12\x1a\n\x12\x63onstructor_method\x18\x02 \x01(\t\x12\x45\n\x12\x63onstructor_schema\x18\x03 \x01(\x0b\x32).org.apache.beam.model.pipeline.v1.Schema\x12\x1b\n\x13\x63onstructor_payload\x18\x04 \x01(\x0c\x12I\n\x0f\x62uilder_methods\x18\x05 \x03(\x0b\x32\x30.org.apache.beam.model.pipeline.v1.BuilderMethod\"i\n\rBuilderMethod\x12\x0c\n\x04name\x18\x01 \x01(\t\x12\x39\n\x06schema\x18\x02 \x01(\x0b\x32).org.apache.beam.model.pipeline.v1.Schema\x12\x0f\n\x07payload\x18\x03 \x01(\x0c\x42\x81\x01\n!org.apache.beam.model.pipeline.v1B\x12\x45xternalTransformsZHgithub.com/apache/beam/sdks/v2/go/pkg/beam/model/pipeline_v1;pipeline_v1b\x06proto3'
  ,
  dependencies=[org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_schema__pb2.DESCRIPTOR,org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_beam__runner__api__pb2.DESCRIPTOR,])



_EXPANSIONMETHODS_ENUM = _descriptor.EnumDescriptor(
  name='Enum',
  full_name='org.apache.beam.model.pipeline.v1.ExpansionMethods.Enum',
  filename=None,
  file=DESCRIPTOR,
  create_key=_descriptor._internal_create_key,
  values=[
    _descriptor.EnumValueDescriptor(
      name='JAVA_CLASS_LOOKUP', index=0, number=0,
      serialized_options=b'\242\264\372\302\005+beam:expansion:payload:java_class_lookup:v1',
      type=None,
      create_key=_descriptor._internal_create_key),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=331,
  serialized_end=411,
)
_sym_db.RegisterEnumDescriptor(_EXPANSIONMETHODS_ENUM)


_EXTERNALCONFIGURATIONPAYLOAD = _descriptor.Descriptor(
  name='ExternalConfigurationPayload',
  full_name='org.apache.beam.model.pipeline.v1.ExternalConfigurationPayload',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='schema', full_name='org.apache.beam.model.pipeline.v1.ExternalConfigurationPayload.schema', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='payload', full_name='org.apache.beam.model.pipeline.v1.ExternalConfigurationPayload.payload', index=1,
      number=2, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value=b"",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=203,
  serialized_end=309,
)


_EXPANSIONMETHODS = _descriptor.Descriptor(
  name='ExpansionMethods',
  full_name='org.apache.beam.model.pipeline.v1.ExpansionMethods',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _EXPANSIONMETHODS_ENUM,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=311,
  serialized_end=411,
)


_JAVACLASSLOOKUPPAYLOAD = _descriptor.Descriptor(
  name='JavaClassLookupPayload',
  full_name='org.apache.beam.model.pipeline.v1.JavaClassLookupPayload',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='class_name', full_name='org.apache.beam.model.pipeline.v1.JavaClassLookupPayload.class_name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='constructor_method', full_name='org.apache.beam.model.pipeline.v1.JavaClassLookupPayload.constructor_method', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='constructor_schema', full_name='org.apache.beam.model.pipeline.v1.JavaClassLookupPayload.constructor_schema', index=2,
      number=3, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='constructor_payload', full_name='org.apache.beam.model.pipeline.v1.JavaClassLookupPayload.constructor_payload', index=3,
      number=4, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value=b"",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='builder_methods', full_name='org.apache.beam.model.pipeline.v1.JavaClassLookupPayload.builder_methods', index=4,
      number=5, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=414,
  serialized_end=661,
)


_BUILDERMETHOD = _descriptor.Descriptor(
  name='BuilderMethod',
  full_name='org.apache.beam.model.pipeline.v1.BuilderMethod',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='org.apache.beam.model.pipeline.v1.BuilderMethod.name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='schema', full_name='org.apache.beam.model.pipeline.v1.BuilderMethod.schema', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='payload', full_name='org.apache.beam.model.pipeline.v1.BuilderMethod.payload', index=2,
      number=3, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value=b"",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=663,
  serialized_end=768,
)

_EXTERNALCONFIGURATIONPAYLOAD.fields_by_name['schema'].message_type = org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_schema__pb2._SCHEMA
_EXPANSIONMETHODS_ENUM.containing_type = _EXPANSIONMETHODS
_JAVACLASSLOOKUPPAYLOAD.fields_by_name['constructor_schema'].message_type = org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_schema__pb2._SCHEMA
_JAVACLASSLOOKUPPAYLOAD.fields_by_name['builder_methods'].message_type = _BUILDERMETHOD
_BUILDERMETHOD.fields_by_name['schema'].message_type = org_dot_apache_dot_beam_dot_model_dot_pipeline_dot_v1_dot_schema__pb2._SCHEMA
DESCRIPTOR.message_types_by_name['ExternalConfigurationPayload'] = _EXTERNALCONFIGURATIONPAYLOAD
DESCRIPTOR.message_types_by_name['ExpansionMethods'] = _EXPANSIONMETHODS
DESCRIPTOR.message_types_by_name['JavaClassLookupPayload'] = _JAVACLASSLOOKUPPAYLOAD
DESCRIPTOR.message_types_by_name['BuilderMethod'] = _BUILDERMETHOD
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

ExternalConfigurationPayload = _reflection.GeneratedProtocolMessageType('ExternalConfigurationPayload', (_message.Message,), {
  'DESCRIPTOR' : _EXTERNALCONFIGURATIONPAYLOAD,
  '__module__' : 'org.apache.beam.model.pipeline.v1.external_transforms_pb2'
  # @@protoc_insertion_point(class_scope:org.apache.beam.model.pipeline.v1.ExternalConfigurationPayload)
  })
_sym_db.RegisterMessage(ExternalConfigurationPayload)

ExpansionMethods = _reflection.GeneratedProtocolMessageType('ExpansionMethods', (_message.Message,), {
  'DESCRIPTOR' : _EXPANSIONMETHODS,
  '__module__' : 'org.apache.beam.model.pipeline.v1.external_transforms_pb2'
  # @@protoc_insertion_point(class_scope:org.apache.beam.model.pipeline.v1.ExpansionMethods)
  })
_sym_db.RegisterMessage(ExpansionMethods)

JavaClassLookupPayload = _reflection.GeneratedProtocolMessageType('JavaClassLookupPayload', (_message.Message,), {
  'DESCRIPTOR' : _JAVACLASSLOOKUPPAYLOAD,
  '__module__' : 'org.apache.beam.model.pipeline.v1.external_transforms_pb2'
  # @@protoc_insertion_point(class_scope:org.apache.beam.model.pipeline.v1.JavaClassLookupPayload)
  })
_sym_db.RegisterMessage(JavaClassLookupPayload)

BuilderMethod = _reflection.GeneratedProtocolMessageType('BuilderMethod', (_message.Message,), {
  'DESCRIPTOR' : _BUILDERMETHOD,
  '__module__' : 'org.apache.beam.model.pipeline.v1.external_transforms_pb2'
  # @@protoc_insertion_point(class_scope:org.apache.beam.model.pipeline.v1.BuilderMethod)
  })
_sym_db.RegisterMessage(BuilderMethod)


DESCRIPTOR._options = None
_EXPANSIONMETHODS_ENUM.values_by_name["JAVA_CLASS_LOOKUP"]._options = None
# @@protoc_insertion_point(module_scope)
