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

import static org.apache.beam.sdk.io.iceberg.IcebergReadSchemaTransformProvider.IcebergReadSchemaTransform;
import static org.apache.beam.sdk.io.iceberg.IcebergWriteSchemaTransformProvider.INPUT_TAG;
import static org.apache.beam.sdk.io.iceberg.IcebergWriteSchemaTransformProvider.IcebergWriteSchemaTransform;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.beam.model.pipeline.v1.RunnerApi;
import org.apache.beam.model.pipeline.v1.SchemaAwareTransforms;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.RowCoder;
import org.apache.beam.sdk.managed.ManagedTransformConstants;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.schemas.NoSuchSchemaException;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.SchemaTranslation;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.util.construction.PipelineTranslation;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionRowTuple;
import org.apache.beam.sdk.values.Row;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.catalog.TableIdentifier;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class IcebergSchemaTransformTranslationTest {
  @ClassRule public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

  @Rule
  public transient TestDataWarehouse warehouse = new TestDataWarehouse(TEMPORARY_FOLDER, "default");

  @Rule public transient ExpectedException thrown = ExpectedException.none();

  static final IcebergWriteSchemaTransformProvider WRITE_PROVIDER =
      new IcebergWriteSchemaTransformProvider();
  static final IcebergReadSchemaTransformProvider READ_PROVIDER =
      new IcebergReadSchemaTransformProvider();

  @Test
  public void testReCreateWriteTransformFromRow() throws NoSuchSchemaException {
    Row catalogConfigRow =
        Row.withSchema(IcebergSchemaTransformCatalogConfig.SCHEMA)
            .withFieldValue("catalogName", "test_name")
            .withFieldValue("catalogType", "test_type")
            .withFieldValue("catalogImplementation", "testImplementation")
            .withFieldValue("warehouseLocation", "test_location")
            .build();
    Row transformConfigRow =
        Row.withSchema(WRITE_PROVIDER.configurationSchema())
            .withFieldValue("table", "test_table_identifier")
            .withFieldValue("catalogConfig", catalogConfigRow)
            .build();
    IcebergWriteSchemaTransform writeTransform =
        (IcebergWriteSchemaTransform) WRITE_PROVIDER.from(transformConfigRow);

    IcebergSchemaTransformTranslation.IcebergWriteSchemaTransformTranslator translator =
        new IcebergSchemaTransformTranslation.IcebergWriteSchemaTransformTranslator();
    Row row = translator.toConfigRow(writeTransform);

    IcebergWriteSchemaTransform writeTransformFromRow =
        translator.fromConfigRow(row, PipelineOptionsFactory.create());

    assertEquals(transformConfigRow, writeTransformFromRow.getConfigurationRow());
  }

  @Test
  public void testWriteTransformProtoTranslation() throws Exception {
    // First build a pipeline
    Pipeline p = Pipeline.create();
    Schema inputSchema = Schema.builder().addStringField("str").build();
    PCollection<Row> input =
        p.apply(
                Create.of(
                    Collections.singletonList(Row.withSchema(inputSchema).addValue("a").build())))
            .setRowSchema(inputSchema);

    Row catalogConfigRow =
        Row.withSchema(IcebergSchemaTransformCatalogConfig.SCHEMA)
            .withFieldValue("catalogName", "test_catalog")
            .withFieldValue("catalogType", CatalogUtil.ICEBERG_CATALOG_TYPE_HADOOP)
            .withFieldValue("catalogImplementation", "test_implementation")
            .withFieldValue("warehouseLocation", warehouse.location)
            .build();
    Row transformConfigRow =
        Row.withSchema(WRITE_PROVIDER.configurationSchema())
            .withFieldValue("table", "test_identifier")
            .withFieldValue("catalogConfig", catalogConfigRow)
            .build();

    IcebergWriteSchemaTransform writeTransform =
        (IcebergWriteSchemaTransform) WRITE_PROVIDER.from(transformConfigRow);
    PCollectionRowTuple.of(INPUT_TAG, input).apply(writeTransform);

    // Then translate the pipeline to a proto and extract IcebergWriteSchemaTransform proto
    RunnerApi.Pipeline pipelineProto = PipelineTranslation.toProto(p);
    List<RunnerApi.PTransform> writeTransformProto =
        pipelineProto.getComponents().getTransformsMap().values().stream()
            .filter(tr -> tr.getSpec().getUrn().equals(ManagedTransformConstants.ICEBERG_WRITE))
            .collect(Collectors.toList());
    assertEquals(1, writeTransformProto.size());
    RunnerApi.FunctionSpec spec = writeTransformProto.get(0).getSpec();

    // Check that the proto contains correct values
    SchemaAwareTransforms.SchemaAwareTransformPayload payload =
        SchemaAwareTransforms.SchemaAwareTransformPayload.parseFrom(spec.getPayload());
    Schema schemaFromSpec = SchemaTranslation.schemaFromProto(payload.getExpansionSchema());
    assertEquals(WRITE_PROVIDER.configurationSchema(), schemaFromSpec);
    System.out.println(
        "spec: " + schemaFromSpec.getField("catalogConfig").getType().getRowSchema());
    Row rowFromSpec = RowCoder.of(schemaFromSpec).decode(payload.getExpansionPayload().newInput());

    assertEquals(transformConfigRow, rowFromSpec);

    // Use the information in the proto to recreate the IcebergWriteSchemaTransform
    IcebergSchemaTransformTranslation.IcebergWriteSchemaTransformTranslator translator =
        new IcebergSchemaTransformTranslation.IcebergWriteSchemaTransformTranslator();
    IcebergWriteSchemaTransform writeTransformFromSpec =
        translator.fromConfigRow(rowFromSpec, PipelineOptionsFactory.create());

    assertEquals(transformConfigRow, writeTransformFromSpec.getConfigurationRow());
  }

  @Test
  public void testReCreateReadTransformFromRow() throws NoSuchSchemaException {
    // setting a subset of fields here.
    Row catalogConfigRow =
        Row.withSchema(IcebergSchemaTransformCatalogConfig.SCHEMA)
            .withFieldValue("catalogName", "test_name")
            .withFieldValue("catalogType", "test_type")
            .withFieldValue("catalogImplementation", "testImplementation")
            .withFieldValue("warehouseLocation", "test_location")
            .build();
    Row transformConfigRow =
        Row.withSchema(READ_PROVIDER.configurationSchema())
            .withFieldValue("table", "test_table_identifier")
            .withFieldValue("catalogConfig", catalogConfigRow)
            .build();

    IcebergReadSchemaTransform readTransform =
        (IcebergReadSchemaTransform) READ_PROVIDER.from(transformConfigRow);

    IcebergSchemaTransformTranslation.IcebergReadSchemaTransformTranslator translator =
        new IcebergSchemaTransformTranslation.IcebergReadSchemaTransformTranslator();
    Row row = translator.toConfigRow(readTransform);

    IcebergReadSchemaTransform readTransformFromRow =
        translator.fromConfigRow(row, PipelineOptionsFactory.create());

    assertEquals(transformConfigRow, readTransformFromRow.getConfigurationRow());
  }

  @Test
  public void testReadTransformProtoTranslation() throws Exception {
    // First build a pipeline
    Pipeline p = Pipeline.create();
    Row catalogConfigRow =
        Row.withSchema(IcebergSchemaTransformCatalogConfig.SCHEMA)
            .withFieldValue("catalogName", "test_catalog")
            .withFieldValue("catalogType", CatalogUtil.ICEBERG_CATALOG_TYPE_HADOOP)
            .withFieldValue("warehouseLocation", warehouse.location)
            .build();
    String identifier = "default.table_" + Long.toString(UUID.randomUUID().hashCode(), 16);
    warehouse.createTable(TableIdentifier.parse(identifier), TestFixtures.SCHEMA);

    Row transformConfigRow =
        Row.withSchema(READ_PROVIDER.configurationSchema())
            .withFieldValue("table", identifier)
            .withFieldValue("catalogConfig", catalogConfigRow)
            .build();

    IcebergReadSchemaTransform readTransform =
        (IcebergReadSchemaTransform) READ_PROVIDER.from(transformConfigRow);

    PCollectionRowTuple.empty(p).apply(readTransform);

    // Then translate the pipeline to a proto and extract IcebergReadSchemaTransform proto
    RunnerApi.Pipeline pipelineProto = PipelineTranslation.toProto(p);
    List<RunnerApi.PTransform> readTransformProto =
        pipelineProto.getComponents().getTransformsMap().values().stream()
            .filter(tr -> tr.getSpec().getUrn().equals(ManagedTransformConstants.ICEBERG_READ))
            .collect(Collectors.toList());
    assertEquals(1, readTransformProto.size());
    RunnerApi.FunctionSpec spec = readTransformProto.get(0).getSpec();

    // Check that the proto contains correct values
    SchemaAwareTransforms.SchemaAwareTransformPayload payload =
        SchemaAwareTransforms.SchemaAwareTransformPayload.parseFrom(spec.getPayload());
    Schema schemaFromSpec = SchemaTranslation.schemaFromProto(payload.getExpansionSchema());
    assertEquals(READ_PROVIDER.configurationSchema(), schemaFromSpec);
    Row rowFromSpec = RowCoder.of(schemaFromSpec).decode(payload.getExpansionPayload().newInput());
    assertEquals(transformConfigRow, rowFromSpec);

    // Use the information in the proto to recreate the IcebergReadSchemaTransform
    IcebergSchemaTransformTranslation.IcebergReadSchemaTransformTranslator translator =
        new IcebergSchemaTransformTranslation.IcebergReadSchemaTransformTranslator();
    IcebergReadSchemaTransform readTransformFromSpec =
        translator.fromConfigRow(rowFromSpec, PipelineOptionsFactory.create());

    assertEquals(transformConfigRow, readTransformFromSpec.getConfigurationRow());
  }
}