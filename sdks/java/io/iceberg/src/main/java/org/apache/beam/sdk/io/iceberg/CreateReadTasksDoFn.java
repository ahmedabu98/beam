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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.iceberg.CombinedScanTask;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.IncrementalAppendScan;
import org.apache.iceberg.ScanTaskParser;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.io.CloseableIterable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans the given {@link SnapshotRange}, and creates multiple {@link ReadTask}s. Each task
 * represents a portion of a data file that was appended within the snapshot range.
 */
class CreateReadTasksDoFn extends DoFn<SnapshotRange, KV<ReadTaskDescriptor, ReadTask>> {
  private static final Logger LOG = LoggerFactory.getLogger(CreateReadTasksDoFn.class);
  private static final Counter numFileScanTasks =
      Metrics.counter(CreateReadTasksDoFn.class, "numFileScanTasks");
  private final IcebergCatalogConfig catalogConfig;

  CreateReadTasksDoFn(IcebergCatalogConfig catalogConfig) {
    this.catalogConfig = catalogConfig;
  }

  @ProcessElement
  public void process(
      @Element SnapshotRange range, OutputReceiver<KV<ReadTaskDescriptor, ReadTask>> out)
      throws IOException, ExecutionException {
    Table table = TableCache.get(range.getTableIdentifier(), catalogConfig.catalog());
    @Nullable Long fromSnapshot = range.getFromSnapshotExclusive();
    long toSnapshot = range.getToSnapshot();

    LOG.info("Planning to scan snapshot range ({}, {}]", fromSnapshot, toSnapshot);
    IncrementalAppendScan scan =
        table
            .newIncrementalAppendScan()
            .toSnapshot(toSnapshot)
            .option(TableProperties.SPLIT_SIZE, String.valueOf(TableProperties.SPLIT_SIZE_DEFAULT));
    if (fromSnapshot != null) {
      scan = scan.fromSnapshotExclusive(fromSnapshot);
    }

    try (CloseableIterable<CombinedScanTask> combinedScanTasks = scan.planTasks()) {
      for (CombinedScanTask combinedScanTask : combinedScanTasks) {
        // A single DataFile can be broken up into multiple FileScanTasks
        // if it is large enough.
        for (FileScanTask fileScanTask : combinedScanTask.tasks()) {
          ReadTask task =
              ReadTask.builder()
                  .setTableIdentifierString(range.getTableIdentifierString())
                  .setFileScanTaskJson(ScanTaskParser.toJson(fileScanTask))
                  .setByteSize(fileScanTask.sizeBytes())
                  .build();
          ReadTaskDescriptor descriptor =
              ReadTaskDescriptor.builder()
                  .setTableIdentifierString(range.getTableIdentifierString())
                  .build();
          out.output(KV.of(descriptor, task));
          numFileScanTasks.inc();
        }
      }
    }
  }
}
