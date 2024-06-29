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

import com.google.auto.value.AutoValue;
import java.io.Serializable;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.beam.sdk.util.ReleaseInfo;
import org.apache.beam.vendor.guava.v32_1_2_jre.com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogUtil;
import org.checkerframework.dataflow.qual.Pure;

@AutoValue
public abstract class IcebergCatalogConfig implements Serializable {
  @Pure
  public abstract Properties getProperties();

  @Pure
  public abstract @Nullable Configuration getConfiguration();

  @Pure
  public static Builder builder() {
    return new AutoValue_IcebergCatalogConfig.Builder();
  }

  public org.apache.iceberg.catalog.Catalog catalog() {
    Configuration conf = getConfiguration();
    String catalogName = "apache-beam-" + ReleaseInfo.getReleaseInfo().getVersion();
    if (conf == null) {
      conf = new Configuration();
    }
    return CatalogUtil.buildIcebergCatalog(catalogName, Maps.fromProperties(getProperties()), conf);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProperties(Properties props);

    public abstract Builder setConfiguration(@Nullable Configuration conf);

    public abstract IcebergCatalogConfig build();
  }
}
