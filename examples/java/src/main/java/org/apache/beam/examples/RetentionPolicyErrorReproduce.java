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
package org.apache.beam.examples;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RetentionPolicyErrorReproduce {

  public static void main(String[] args) throws IOException {
    // The ID of your GCP project
    String projectId = "google:clouddfe";

    // The ID of your GCS bucket
    String bucketName = "ahmedabualsaud-usc";

    // The ID of your GCS object
    String objectName = "file";

    // The string of contents you wish to upload
    String contents = "Hello world!";

    Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    BlobId blobId = BlobId.of(bucketName, objectName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    byte[] content = contents.getBytes(StandardCharsets.UTF_8);
    storage.createFrom(blobInfo, new ByteArrayInputStream(content));

    System.out.println(
        "Object "
            + objectName
            + " uploaded to bucket "
            + bucketName
            + " with contents "
            + contents);
  }
}
