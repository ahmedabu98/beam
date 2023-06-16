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

"""Unit tests for BigTable service."""

# pytype: skip-file
from datetime import datetime, timezone
import os
import secrets
import string
import time
import unittest
import uuid
from random import choice
import logging

import pytest
from mock import MagicMock
from mock import patch

import apache_beam as beam
from apache_beam.internal.metrics.metric import ServiceCallMetric
from apache_beam.io.gcp import bigtableio
from apache_beam.io.gcp import resource_identifiers
from apache_beam.metrics import monitoring_infos
from apache_beam.metrics.execution import MetricsEnvironment

from apache_beam.testing.test_pipeline import TestPipeline

_LOGGER = logging.getLogger(__name__)

# Protect against environments where bigtable library is not available.
try:
  from apitools.base.py.exceptions import HttpError
  from google.cloud.bigtable import client
  from google.cloud.bigtable.row_filters import TimestampRange
  from google.cloud.bigtable.instance import Instance
  from google.cloud.bigtable.row import DirectRow, PartialRowData, Cell
  from google.cloud.bigtable.table import Table
  from google.cloud.bigtable_admin_v2.types import instance
  from google.rpc.code_pb2 import OK, ALREADY_EXISTS
  from google.rpc.status_pb2 import Status
except ImportError as e:
  client = None
  HttpError = None


@pytest.mark.uses_gcp_java_expansion_service
# @unittest.skipUnless(
#   os.environ.get('EXPANSION_PORT'),
#   "EXPANSION_PORT environment var is not provided.")
@unittest.skipIf(client is None, 'Bigtable dependencies are not installed')
class TestWriteToBigtableXlang(unittest.TestCase):
  INSTANCE = "bt-write-xlang-tests"
  TABLE_ID = "test-table"

  @classmethod
  def setUpClass(cls):
    cls.test_pipeline = TestPipeline(is_integration_test=True)
    cls.project = cls.test_pipeline.get_option('project')
    cls.args = cls.test_pipeline.get_full_options_as_args()

    instance_id = '%s-%s' % (cls.INSTANCE, str(int(time.time())))

    cls.client = client.Client(admin=True, project=cls.project)
    # create cluster and instance
    cls.instance = cls.client.instance(
        instance_id,
        display_name=cls.INSTANCE,
        instance_type=instance.Instance.Type.DEVELOPMENT)
    cluster = cls.instance.cluster("test-cluster", "us-central1-a")
    operation = cls.instance.create(clusters=[cluster])
    operation.result(timeout=500)
    _LOGGER.info(
        "Created instance [%s] in project [%s]",
        cls.instance.instance_id,
        cls.project)

  def setUp(self):
    # create table inside instance
    self.table: Table = self.instance.table(
        '%s-%s-%s' %
        (self.TABLE_ID, str(int(time.time())), secrets.token_hex(3)))
    self.table.create()
    _LOGGER.info("Created table [%s]", self.table.table_id)

  def tearDown(self):
    try:
      _LOGGER.info("Deleting table [%s]", self.table.table_id)
      self.table.delete()
    except HttpError:
      _LOGGER.debug("Failed to clean up table [%s]", self.table.table_id)

  @classmethod
  def tearDownClass(cls):
    try:
      _LOGGER.info("Deleting instance [%s]", cls.instance.instance_id)
      cls.instance.delete()
    except HttpError:
      _LOGGER.debug(
          "Failed to clean up instance [%s]", cls.instance.instance_id)

  def test_set_mutation(self):
    row1: DirectRow = DirectRow('key-1')
    row2: DirectRow = DirectRow('key-2')
    col_fam = self.table.column_family('col_fam')
    col_fam.create()
    # expected cells
    row1_col1_cell = Cell(b'val1-1', 100_000_000)
    row1_col2_cell = Cell(b'val1-2', 200_000_000)
    row2_col1_cell = Cell(b'val2-1', 100_000_000)
    row2_col2_cell = Cell(b'val2-2', 200_000_000)
    # rows sent to write transform
    row1.set_cell(
        'col_fam', b'col-1', row1_col1_cell.value, row1_col1_cell.timestamp)
    row1.set_cell(
        'col_fam', b'col-2', row1_col2_cell.value, row1_col2_cell.timestamp)
    row2.set_cell(
        'col_fam', b'col-1', row2_col1_cell.value, row2_col1_cell.timestamp)
    row2.set_cell(
        'col_fam', b'col-2', row2_col2_cell.value, row2_col2_cell.timestamp)

    self.run_pipeline([row1, row2])

    # after write transform executes, get actual rows from table
    actual_row1: PartialRowData = self.table.read_row('key-1')
    actual_row2: PartialRowData = self.table.read_row('key-2')

    # check actual rows match with expected rows (value and timestamp)
    self.assertEqual(
        row1_col1_cell, actual_row1.find_cells('col_fam', b'col-1')[0])
    self.assertEqual(
        row1_col2_cell, actual_row1.find_cells('col_fam', b'col-2')[0])
    self.assertEqual(
        row2_col1_cell, actual_row2.find_cells('col_fam', b'col-1')[0])
    self.assertEqual(
        row2_col2_cell, actual_row2.find_cells('col_fam', b'col-2')[0])

  def test_delete_cells_mutation(self):
    col_fam = self.table.column_family('col_fam')
    col_fam.create()
    # write a row with two columns to the table beforehand.
    write_row: DirectRow = DirectRow('key-1', self.table)
    write_row.set_cell('col_fam', b'col-1', b'val-1')
    write_row.set_cell('col_fam', b'col-2', b'val-2')
    write_row.commit()

    # prepare a row that will delete cells in one of the columns.
    delete_row: DirectRow = DirectRow('key-1')
    delete_row.delete_cell('col_fam', b'col-1')

    self.run_pipeline([delete_row])

    # after transform executes, get actual row from table
    actual_row: PartialRowData = self.table.read_row('key-1')

    # we deleted all the cells in 'col-1', so this check should throw an error
    with self.assertRaises(KeyError):
      actual_row.find_cells('col_fam', b'col-1')

    # check the cell in col-2 is still there
    col2_cells = actual_row.find_cells('col_fam', b'col-2')
    self.assertEqual(1, len(col2_cells))
    self.assertEqual(b'val-2', col2_cells[0].value)

  def test_delete_cells_with_timerange_mutation(self):
    col_fam = self.table.column_family('col_fam')
    col_fam.create()
    # write two cells in a column to the table beforehand.
    write_row: DirectRow = DirectRow('key-1', self.table)
    write_row.set_cell(
        'col_fam',
        b'col',
        b'val',
        datetime.fromtimestamp(100_000_000, tz=timezone.utc))
    write_row.commit()
    write_row.set_cell(
        'col_fam',
        b'col',
        b'new-val',
        datetime.fromtimestamp(200_000_000, tz=timezone.utc))
    write_row.commit()

    # prepare a row that will delete cells within a timestamp range.
    delete_row: DirectRow = DirectRow('key-1')
    delete_row.delete_cell(
        'col_fam',
        b'col',
        time_range=TimestampRange(
            start=datetime.fromtimestamp(99_999_999, tz=timezone.utc),
            end=datetime.fromtimestamp(100_000_001, tz=timezone.utc)))

    self.run_pipeline([delete_row])

    # after transform executes, get actual row from table
    actual_row: PartialRowData = self.table.read_row('key-1')

    # we deleted one cell within the timestamp range.
    # check the other (newer) cell still exists.
    cells = actual_row.find_cells('col_fam', b'col')
    self.assertEqual(1, len(cells))
    self.assertEqual(b'new-val', cells[0].value)
    self.assertEqual(
        datetime.fromtimestamp(200_000_000, tz=timezone.utc),
        cells[0].timestamp)

  def test_delete_column_family_mutation(self):
    # create two column families
    col_fam = self.table.column_family('col_fam-1')
    col_fam.create()
    col_fam = self.table.column_family('col_fam-2')
    col_fam.create()
    # write a row with values in both column families to the table beforehand.
    write_row: DirectRow = DirectRow('key-1', self.table)
    write_row.set_cell('col_fam-1', b'col', b'val')
    write_row.set_cell('col_fam-2', b'col', b'val')
    write_row.commit()

    # prepare a row that will delete a column family from the row
    delete_row: DirectRow = DirectRow('key-1')
    delete_row.delete_cells('col_fam-1', delete_row.ALL_COLUMNS)

    self.run_pipeline([delete_row])

    # after transform executes, get actual row from table
    actual_row: PartialRowData = self.table.read_row('key-1')

    # we deleted the column family 'col_fam-1', so this check should throw an error
    with self.assertRaises(KeyError):
      actual_row.find_cells('col_fam-1', b'col')

    # check we have one column family left with the correct cell value
    self.assertEqual(1, len(actual_row.cells))
    self.assertEqual(b'val', actual_row.cell_value('col_fam-2', b'col'))

  def test_delete_row_mutation(self):
    write_row1: DirectRow = DirectRow('key-1', self.table)
    write_row2: DirectRow = DirectRow('key-2', self.table)
    col_fam = self.table.column_family('col_fam')
    col_fam.create()
    # write a couple of rows to the table beforehand
    write_row1.set_cell('col_fam', b'col', b'val-1')
    write_row1.commit()
    write_row2.set_cell('col_fam', b'col', b'val-2')
    write_row2.commit()

    # prepare a row that will delete itself
    delete_row: DirectRow = DirectRow('key-1')
    delete_row.delete()

    self.run_pipeline([delete_row])

    # after write transform executes, get actual rows from table
    actual_row1: PartialRowData = self.table.read_row('key-1')
    actual_row2: PartialRowData = self.table.read_row('key-2')

    # we deleted row with key 'key-1', check it doesn't exist anymore
    # the Bigtable API doesn't throw an error here, just returns a None value
    self.assertEqual(None, actual_row1)
    # check row 2 exists with the correct cell value in col
    self.assertEqual(b'val-2', actual_row2.cell_value('col_fam', b'col'))

  def run_pipeline(self, rows):
    with beam.Pipeline(argv=self.args) as p:
      _ = (
          p
          | beam.Create(rows)
          | bigtableio.WriteToBigtableXlang(
              table_id=self.table.table_id,
              instance_id=self.instance.instance_id,
              project_id=self.project))


@unittest.skipIf(client is None, 'Bigtable dependencies are not installed')
class TestWriteBigTable(unittest.TestCase):
  TABLE_PREFIX = "python-test"
  _PROJECT_ID = TABLE_PREFIX + "-" + str(uuid.uuid4())[:8]
  _INSTANCE_ID = TABLE_PREFIX + "-" + str(uuid.uuid4())[:8]
  _TABLE_ID = TABLE_PREFIX + "-" + str(uuid.uuid4())[:8]

  def setUp(self):
    client = MagicMock()
    instance = Instance(self._INSTANCE_ID, client)
    self.table = Table(self._TABLE_ID, instance)

  def test_write_metrics(self):
    MetricsEnvironment.process_wide_container().reset()
    write_fn = bigtableio._BigTableWriteFn(
        self._PROJECT_ID, self._INSTANCE_ID, self._TABLE_ID)
    write_fn.table = self.table
    write_fn.start_bundle()
    number_of_rows = 2
    error = Status()
    error.message = 'Entity already exists.'
    error.code = ALREADY_EXISTS
    success = Status()
    success.message = 'Success'
    success.code = OK
    rows_response = [error, success] * number_of_rows
    with patch.object(Table, 'mutate_rows', return_value=rows_response):
      direct_rows = [self.generate_row(i) for i in range(number_of_rows * 2)]
      for direct_row in direct_rows:
        write_fn.process(direct_row)
      try:
        write_fn.finish_bundle()
      except:  # pylint: disable=bare-except
        # Currently we fail the bundle when there are any failures.
        # TODO(https://github.com/apache/beam/issues/21396): remove after
        # bigtableio can selectively retry.
        pass
      self.verify_write_call_metric(
          self._PROJECT_ID,
          self._INSTANCE_ID,
          self._TABLE_ID,
          ServiceCallMetric.bigtable_error_code_to_grpc_status_string(
              ALREADY_EXISTS),
          2)
      self.verify_write_call_metric(
          self._PROJECT_ID,
          self._INSTANCE_ID,
          self._TABLE_ID,
          ServiceCallMetric.bigtable_error_code_to_grpc_status_string(OK),
          2)

  def generate_row(self, index=0):
    rand = choice(string.ascii_letters + string.digits)
    value = ''.join(rand for i in range(100))
    column_family_id = 'cf1'
    key = "beam_key%s" % ('{0:07}'.format(index))
    direct_row = DirectRow(row_key=key)
    for column_id in range(10):
      direct_row.set_cell(
          column_family_id, ('field%s' % column_id).encode('utf-8'),
          value,
          datetime.now())
    return direct_row

  def verify_write_call_metric(
      self, project_id, instance_id, table_id, status, count):
    """Check if a metric was recorded for the Datastore IO write API call."""
    process_wide_monitoring_infos = list(
        MetricsEnvironment.process_wide_container().
        to_runner_api_monitoring_infos(None).values())
    resource = resource_identifiers.BigtableTable(
        project_id, instance_id, table_id)
    labels = {
        monitoring_infos.SERVICE_LABEL: 'BigTable',
        monitoring_infos.METHOD_LABEL: 'google.bigtable.v2.MutateRows',
        monitoring_infos.RESOURCE_LABEL: resource,
        monitoring_infos.BIGTABLE_PROJECT_ID_LABEL: project_id,
        monitoring_infos.INSTANCE_ID_LABEL: instance_id,
        monitoring_infos.TABLE_ID_LABEL: table_id,
        monitoring_infos.STATUS_LABEL: status
    }
    expected_mi = monitoring_infos.int64_counter(
        monitoring_infos.API_REQUEST_COUNT_URN, count, labels=labels)
    expected_mi.ClearField("start_time")

    found = False
    for actual_mi in process_wide_monitoring_infos:
      actual_mi.ClearField("start_time")
      if expected_mi == actual_mi:
        found = True
        break
    self.assertTrue(
        found, "Did not find write call metric with status: %s" % status)


if __name__ == '__main__':
  unittest.main()
