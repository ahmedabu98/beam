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
import org.apache.beam.sdk.schemas.AutoValueSchema;
import org.apache.beam.sdk.schemas.NoSuchSchemaException;
import org.apache.beam.sdk.schemas.SchemaCoder;
import org.apache.beam.sdk.schemas.SchemaRegistry;
import org.apache.beam.sdk.schemas.annotations.DefaultSchema;
import org.apache.beam.sdk.schemas.annotations.SchemaIgnore;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.ScanTaskParser;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

@DefaultSchema(AutoValueSchema.class)
@AutoValue
abstract class ReadTask {
  private static @MonotonicNonNull SchemaCoder<ReadTask> coder;

  static SchemaCoder<ReadTask> getCoder() {
    if (coder == null) {
      try {
        coder = SchemaRegistry.createDefault().getSchemaCoder(ReadTask.class);
      } catch (NoSuchSchemaException e) {
        throw new RuntimeException(e);
      }
    }
    return coder;
  }

  private transient @MonotonicNonNull FileScanTask cachedFileScanTask;

  static Builder builder() {
    return new AutoValue_ReadTask.Builder();
  }

  abstract String getTableIdentifierString();

  abstract String getFileScanTaskJson();

  abstract long getByteSize();

  @SchemaIgnore
  FileScanTask getFileScanTask() {
    if (cachedFileScanTask == null) {
      cachedFileScanTask = ScanTaskParser.fromJson(getFileScanTaskJson(), true);
    }
    return cachedFileScanTask;
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setTableIdentifierString(String table);

    abstract Builder setFileScanTaskJson(String jsonTask);

    abstract Builder setByteSize(long size);

    @SchemaIgnore
    Builder setFileScanTask(FileScanTask task) {
      return setFileScanTaskJson(ScanTaskParser.toJson(task));
    }

    abstract ReadTask build();
  }
}
