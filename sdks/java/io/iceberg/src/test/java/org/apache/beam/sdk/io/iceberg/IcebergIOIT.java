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
package org.apache.beam.sdk.io.iceberg;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.managed.Managed;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.vendor.guava.v32_1_2_jre.com.google.common.collect.ImmutableMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.CombinedScanTask;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.GenericParquetReaders;
import org.apache.iceberg.data.parquet.GenericParquetWriter;
import org.apache.iceberg.encryption.InputFilesDecryptor;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.parquet.Parquet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for {@link IcebergIO} source and sink. */
@RunWith(JUnit4.class)
public class IcebergIOIT implements Serializable {
  private static final org.apache.beam.sdk.schemas.Schema DOUBLY_NESTED_ROW_SCHEMA =
      org.apache.beam.sdk.schemas.Schema.builder()
          .addStringField("doubly_nested_str")
          .addInt64Field("doubly_nested_float")
          .build();

  private static final org.apache.beam.sdk.schemas.Schema NESTED_ROW_SCHEMA =
      org.apache.beam.sdk.schemas.Schema.builder()
          .addStringField("nested_str")
          .addRowField("nested_row", DOUBLY_NESTED_ROW_SCHEMA)
          .addInt32Field("nested_int")
          .addFloatField("nested_float")
          .build();
  private static final org.apache.beam.sdk.schemas.Schema BEAM_SCHEMA =
      org.apache.beam.sdk.schemas.Schema.builder()
          .addStringField("str")
          .addBooleanField("bool")
          .addInt32Field("int")
          .addRowField("row", NESTED_ROW_SCHEMA)
          .addArrayField("arr_long", org.apache.beam.sdk.schemas.Schema.FieldType.INT64)
          .addNullableRowField("nullable_row", NESTED_ROW_SCHEMA)
          .addNullableInt64Field("nullable_long")
          .build();

  private static final SimpleFunction<Long, Row> ROW_FUNC =
      new SimpleFunction<Long, Row>() {
        @Override
        public Row apply(Long num) {
          String strNum = Long.toString(num);
          Row nestedRow =
              Row.withSchema(NESTED_ROW_SCHEMA)
                  .addValue("nested_str_value_" + strNum)
                  .addValue(
                      Row.withSchema(DOUBLY_NESTED_ROW_SCHEMA)
                          .addValue("doubly_nested_str_value_" + strNum)
                          .addValue(num)
                          .build())
                  .addValue(Integer.valueOf(strNum))
                  .addValue(Float.valueOf(strNum + "." + strNum))
                  .build();

          return Row.withSchema(BEAM_SCHEMA)
              .addValue("str_value_" + strNum)
              .addValue(num % 2 == 0)
              .addValue(Integer.valueOf(strNum))
              .addValue(nestedRow)
              .addValue(LongStream.range(0, num % 10).boxed().collect(Collectors.toList()))
              .addValue(num % 2 == 0 ? null : nestedRow)
              .addValue(num)
              .build();
        }
      };

  private static final org.apache.iceberg.Schema ICEBERG_SCHEMA =
      IcebergUtils.beamSchemaToIcebergSchema(BEAM_SCHEMA);
  private static final SimpleFunction<Row, Record> RECORD_FUNC =
      new SimpleFunction<Row, Record>() {
        @Override
        public Record apply(Row input) {
          return IcebergUtils.beamRowToIcebergRecord(ICEBERG_SCHEMA, input);
        }
      };
  private static final Integer NUM_RECORDS = 1000;
  private static final Integer NUM_SHARDS = 10;

  @Rule public TestPipeline pipeline = TestPipeline.create();

  static GcpOptions options;

  static Configuration catalogHadoopConf;

  @Rule public TestName testName = new TestName();

  private String warehouseLocation;

  private TableIdentifier tableId;
  private Catalog catalog;

  @BeforeClass
  public static void beforeClass() {
    options = TestPipeline.testingPipelineOptions().as(GcpOptions.class);

    catalogHadoopConf = new Configuration();
    catalogHadoopConf.set("fs.gs.project.id", options.getProject());
    catalogHadoopConf.set("fs.gs.auth.type", "APPLICATION_DEFAULT");
  }

  @Before
  public void setUp() {
    warehouseLocation =
        String.format(
            "%s/IcebergIOIT/%s/%s",
            options.getTempLocation(), testName.getMethodName(), UUID.randomUUID());

    tableId =
        TableIdentifier.of(
            testName.getMethodName(), "table" + Long.toString(UUID.randomUUID().hashCode(), 16));
    catalog = new HadoopCatalog(catalogHadoopConf, warehouseLocation);
  }

  /** Populates the Iceberg table and Returns a {@link List<Row>} of expected elements. */
  private List<Row> populateTable(Table table) throws IOException {
    double recordsPerShardFraction = NUM_RECORDS.doubleValue() / NUM_SHARDS;
    long maxRecordsPerShard = Math.round(Math.ceil(recordsPerShardFraction));

    AppendFiles appendFiles = table.newAppend();
    List<Row> expectedRows = new ArrayList<>(NUM_RECORDS);
    int totalRecords = 0;
    for (int shardNum = 0; shardNum < NUM_SHARDS; ++shardNum) {
      String filepath = table.location() + "/" + UUID.randomUUID();
      OutputFile file = table.io().newOutputFile(filepath);
      DataWriter<Record> writer =
          Parquet.writeData(file)
              .schema(ICEBERG_SCHEMA)
              .createWriterFunc(GenericParquetWriter::buildWriter)
              .overwrite()
              .withSpec(table.spec())
              .build();

      for (int recordNum = 0;
          recordNum < maxRecordsPerShard && totalRecords < NUM_RECORDS;
          ++recordNum, ++totalRecords) {

        Row expectedBeamRow = ROW_FUNC.apply((long) recordNum);
        Record icebergRecord = RECORD_FUNC.apply(expectedBeamRow);

        writer.write(icebergRecord);
        expectedRows.add(expectedBeamRow);
      }
      writer.close();
      appendFiles.appendFile(writer.toDataFile());
    }
    appendFiles.commit();

    return expectedRows;
  }

  private List<Record> readRecords(Table table) {
    TableScan tableScan = table.newScan().project(ICEBERG_SCHEMA);
    List<Record> writtenRecords = new ArrayList<>();
    for (CombinedScanTask task : tableScan.planTasks()) {
      InputFilesDecryptor decryptor = new InputFilesDecryptor(task, table.io(), table.encryption());
      for (FileScanTask fileTask : task.files()) {
        InputFile inputFile = decryptor.getInputFile(fileTask);
        CloseableIterable<Record> iterable =
            Parquet.read(inputFile)
                .split(fileTask.start(), fileTask.length())
                .project(ICEBERG_SCHEMA)
                .createReaderFunc(
                    fileSchema -> GenericParquetReaders.buildReader(ICEBERG_SCHEMA, fileSchema))
                .filter(fileTask.residual())
                .build();

        for (Record rec : iterable) {
          writtenRecords.add(rec);
        }
      }
    }
    return writtenRecords;
  }

  private Map<String, Object> managedIcebergConfig() {
    return ImmutableMap.<String, Object>builder()
        .put("table", tableId.toString())
        .put("catalog_name", "test-name")
        .put(
            "catalog_properties",
            ImmutableMap.<String, String>builder()
                .put("type", CatalogUtil.ICEBERG_CATALOG_TYPE_HADOOP)
                .put("warehouse", warehouseLocation)
                .build())
        .build();
  }

  /**
   * Test of a predetermined moderate number of records written directly to Iceberg then read via a
   * Beam pipeline. Table initialization is done on a single process using the Iceberg APIs so the
   * data cannot be "big".
   */
  @Test
  public void testRead() throws Exception {
    Table table = catalog.createTable(tableId, ICEBERG_SCHEMA);

    List<Row> expectedRows = populateTable(table);

    Map<String, Object> config = managedIcebergConfig();

    PCollection<Row> rows =
        pipeline.apply(Managed.read(Managed.ICEBERG).withConfig(config)).getSinglePCollection();

    PAssert.that(rows).containsInAnyOrder(expectedRows);
    pipeline.run().waitUntilFinish();
  }

  private static final List<Row> INPUT_ROWS =
      LongStream.range(0, NUM_RECORDS).boxed().map(ROW_FUNC::apply).collect(Collectors.toList());

  /**
   * Test of a predetermined moderate number of records written to Iceberg using a Beam pipeline,
   * then read directly using Iceberg API.
   */
  @Test
  public void testWrite() {
    Table table = catalog.createTable(tableId, ICEBERG_SCHEMA);

    // Write with Beam
    Map<String, Object> config = managedIcebergConfig();
    PCollection<Row> input = pipeline.apply(Create.of(INPUT_ROWS)).setRowSchema(BEAM_SCHEMA);
    input.apply(Managed.write(Managed.ICEBERG).withConfig(config));
    pipeline.run().waitUntilFinish();

    // Read back and check records are correct
    List<Record> returnedRecords = readRecords(table);
    assertThat(
        returnedRecords, containsInAnyOrder(INPUT_ROWS.stream().map(RECORD_FUNC::apply).toArray()));
  }

  @Test
  public void testWritePartitionedData() {
    PartitionSpec partitionSpec =
        PartitionSpec.builderFor(ICEBERG_SCHEMA)
            .identity("str")
            .identity("bool")
            .identity("int")
            .build();
    Table table = catalog.createTable(tableId, ICEBERG_SCHEMA, partitionSpec);

    // Write with Beam
    Map<String, Object> config = managedIcebergConfig();
    PCollection<Row> input = pipeline.apply(Create.of(INPUT_ROWS)).setRowSchema(BEAM_SCHEMA);
    input.apply(Managed.write(Managed.ICEBERG).withConfig(config));
    pipeline.run().waitUntilFinish();

    // Read back and check records are correct
    List<Record> returnedRecords = readRecords(table);
    assertThat(
        returnedRecords, containsInAnyOrder(INPUT_ROWS.stream().map(RECORD_FUNC::apply).toArray()));
  }
}
