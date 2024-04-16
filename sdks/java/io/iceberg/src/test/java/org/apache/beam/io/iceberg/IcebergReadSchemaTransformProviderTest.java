/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.io.iceberg;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.beam.sdk.schemas.NoSuchSchemaException;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.SchemaRegistry;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionRowTuple;
import org.apache.beam.sdk.values.Row;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.TableIdentifier;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IcebergReadSchemaTransformProviderTest {

  @ClassRule public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

  @Rule public TestDataWarehouse warehouse = new TestDataWarehouse(TEMPORARY_FOLDER, "default");

  @Rule public TestPipeline testPipeline = TestPipeline.create();

  @Test
  public void testBuildTransformWithRow() throws NoSuchSchemaException {
    Row catalogConfigRow =
        Row.withSchema(SchemaRegistry.createDefault().getSchema(SchemaTransformCatalogConfig.class))
            .withFieldValue("catalogName", "test_name")
            .withFieldValue("catalogType", "test_type")
            .withFieldValue("catalogImplementation", "testImplementation")
            .withFieldValue("warehouseLocation", "test_location")
            .build();
    Row transformConfigRow =
        Row.withSchema(new IcebergReadSchemaTransformProvider().configurationSchema())
            .withFieldValue("table", "test_table_identifier")
            .withFieldValue("catalogConfig", catalogConfigRow)
            .build();

    new IcebergReadSchemaTransformProvider().from(transformConfigRow);
  }

  @Test
  public void testSimpleScan() throws Exception {
    String identifier = "default.table_" + Long.toString(UUID.randomUUID().hashCode(), 16);
    TableIdentifier tableId = TableIdentifier.parse(identifier);

    Table simpleTable = warehouse.createTable(tableId, TestFixtures.SCHEMA);
    final Schema schema = SchemaAndRowConversions.icebergSchemaToBeamSchema(TestFixtures.SCHEMA);

    simpleTable
        .newFastAppend()
        .appendFile(
            warehouse.writeRecords(
                "file1s1.parquet", simpleTable.schema(), TestFixtures.FILE1SNAPSHOT1))
        .appendFile(
            warehouse.writeRecords(
                "file2s1.parquet", simpleTable.schema(), TestFixtures.FILE2SNAPSHOT1))
        .appendFile(
            warehouse.writeRecords(
                "file3s1.parquet", simpleTable.schema(), TestFixtures.FILE3SNAPSHOT1))
        .commit();

    final List<Row> expectedRows =
        Stream.of(
                TestFixtures.FILE1SNAPSHOT1,
                TestFixtures.FILE2SNAPSHOT1,
                TestFixtures.FILE3SNAPSHOT1)
            .flatMap(List::stream)
            .map(record -> SchemaAndRowConversions.recordToRow(schema, record))
            .collect(Collectors.toList());

    SchemaTransformCatalogConfig catalogConfig =
        SchemaTransformCatalogConfig.builder()
            .setCatalogName("hadoop")
            .setCatalogType(CatalogUtil.ICEBERG_CATALOG_TYPE_HADOOP)
            .setWarehouseLocation(warehouse.location)
            .build();

    IcebergReadSchemaTransformProvider.Config readConfig =
        IcebergReadSchemaTransformProvider.Config.builder()
            .setTable(identifier)
            .setCatalogConfig(catalogConfig)
            .build();

    PCollection<Row> output =
        PCollectionRowTuple.empty(testPipeline)
            .apply(new IcebergReadSchemaTransformProvider().from(readConfig))
            .get(IcebergReadSchemaTransformProvider.OUTPUT_TAG);

    PAssert.that(output)
        .satisfies(
            (Iterable<Row> rows) -> {
              assertThat(rows, containsInAnyOrder(expectedRows.toArray()));
              return null;
            });

    testPipeline.run();
  }
}
