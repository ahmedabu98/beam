package org.apache.beam.examples;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RetentionPolicyErrorReproduce {

  public static void main(String[] args) throws IOException{
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
