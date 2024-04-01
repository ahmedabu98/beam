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
package org.apache.beam.sdk.managed;

import static org.apache.beam.vendor.guava.v32_1_2_jre.com.google.common.base.Preconditions.checkArgument;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.schemas.AutoValueSchema;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.annotations.DefaultSchema;
import org.apache.beam.sdk.schemas.annotations.SchemaFieldDescription;
import org.apache.beam.sdk.schemas.transforms.SchemaTransform;
import org.apache.beam.sdk.schemas.transforms.SchemaTransformProvider;
import org.apache.beam.sdk.schemas.transforms.TypedSchemaTransformProvider;
import org.apache.beam.sdk.values.PCollectionRowTuple;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.vendor.grpc.v1p60p1.com.google.common.base.Preconditions;
import org.apache.beam.vendor.guava.v32_1_2_jre.com.google.common.base.Strings;
import org.apache.beam.vendor.guava.v32_1_2_jre.com.google.common.collect.Sets;

@AutoService(SchemaTransformProvider.class)
class ManagedSchemaTransformProvider
    extends TypedSchemaTransformProvider<ManagedSchemaTransformProvider.ManagedConfig> {
  private static final String MANAGED_NAMESPACE = "managed";

  @Override
  public String identifier() {
    return "beam:schematransform:org.apache.beam:managed:v1";
  }

  private final Map<String, SchemaTransformProvider> schemaTransformProviders = new HashMap<>();

  private ManagedSchemaTransformProvider(Pattern pattern) {
    try {
      for (SchemaTransformProvider schemaTransformProvider : ServiceLoader.load(SchemaTransformProvider.class)) {
        if (schemaTransformProviders.containsKey(schemaTransformProvider.identifier())) {
          throw new IllegalArgumentException(
                  "Found multiple SchemaTransformProvider implementations with the same identifier "
                          + schemaTransformProvider.identifier());
        }
        schemaTransformProviders.put(schemaTransformProvider.identifier(), schemaTransformProvider);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

    schemaTransformProviders.entrySet().removeIf(e -> !pattern.matcher(e.getKey()).matches());
  }

  private static @Nullable ManagedSchemaTransformProvider managedProvider = null;

  public static ManagedSchemaTransformProvider of(Pattern pattern) {
    if (managedProvider == null) {
      managedProvider = new ManagedSchemaTransformProvider(pattern);
    }
    return managedProvider;
  }

  @DefaultSchema(AutoValueSchema.class)
  @AutoValue
  public abstract static class ManagedConfig {
    public static Builder builder() {
      return new AutoValue_ManagedSchemaTransformProvider_ManagedConfig.Builder();
    }

    @SchemaFieldDescription("Identifier of the underlying IO to instantiate.")
    public abstract String getTransformIdentifier();

    @SchemaFieldDescription("URL path to the YAML config file used to build the underlying IO.")
    public abstract @Nullable String getConfigUrl();

    @SchemaFieldDescription("YAML string config used to build the underlying IO.")
    public abstract @Nullable String getConfig();

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setTransformIdentifier(String identifier);

      public abstract Builder setConfigUrl(String configUrl);

      public abstract Builder setConfig(String config);

      public abstract ManagedConfig build();
    }

    protected void validate() {
      boolean configExists = !Strings.isNullOrEmpty(getConfig());
      boolean configUrlExists = !Strings.isNullOrEmpty(getConfigUrl());
      checkArgument(
          !(configExists && configUrlExists) && (configExists || configUrlExists),
          "Please specify a config or a config URL, but not both.");
    }
  }

  @Override
  protected SchemaTransform from(ManagedConfig managedConfig) {
    managedConfig.validate();
    SchemaTransformProvider schemaTransformProvider = Preconditions.checkNotNull(
            schemaTransformProviders.get(managedConfig.getTransformIdentifier()),
            "Could not find transform with identifier %s, or it may not be supported",
            managedConfig.getTransformIdentifier());

    // parse config before expansion to check if it matches underlying transform's config schema
    Schema transformConfigSchema = schemaTransformProvider.configurationSchema();
    Row transformConfig;
    try {
      transformConfig = getRowConfig(managedConfig, transformConfigSchema);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(
              "Specified configuration does not align with the underlying transform's configuration schema [%s].",
              transformConfigSchema),
          e);
    }

    return new ManagedSchemaTransform(transformConfig, schemaTransformProvider);
  }

  protected static class ManagedSchemaTransform extends SchemaTransform {
    private final Row transformConfig;
    private final SchemaTransformProvider underlyingTransformProvider;

    ManagedSchemaTransform(
        Row transformConfig,
        SchemaTransformProvider underlyingTransformProvider) {
      this.transformConfig = transformConfig;
      this.underlyingTransformProvider = underlyingTransformProvider;
    }

    @Override
    public PCollectionRowTuple expand(PCollectionRowTuple input) {
      SchemaTransform underlyingTransform = underlyingTransformProvider.from(transformConfig);

      return input.apply(underlyingTransform);
    }
  }

  private static Row getRowConfig(ManagedConfig config, Schema transformSchema) throws Exception {
    String transformYamlConfig;
    if (!Strings.isNullOrEmpty(config.getConfigUrl())) {
      try {
        transformYamlConfig =
            FileSystems.open(FileSystems.matchSingleFileSpec(Preconditions.checkNotNull(config.getConfigUrl())).resourceId())
                .toString();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      transformYamlConfig = config.getConfig();
    }

    return yamlToBeamRow(Preconditions.checkNotNull(transformYamlConfig), transformSchema);
  }

  // TODO: implement this method
  private static Row yamlToBeamRow(String yaml, Schema schema) throws Exception {
    // parse yaml string and convert to Row
    // throw an exception if there are missing required fields or if types don't match
    System.out.println(yaml);
    return Row.nullRow(schema);
  }
}
