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
package org.apache.beam.sdk.io.gcp.pubsub;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.apache.beam.runners.core.construction.renderer.PipelineDotRenderer;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.extensions.protobuf.Proto3SchemaMessages.Primitive;
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubClient.TopicPath;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.TestPipelineOptions;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.base.Supplier;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.ImmutableList;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for PubsubIO Read and Write transforms. */
@RunWith(JUnit4.class)
public class PubsubIOWriteIT {
  private static final String TOPIC_PATH = "projects/google.com:clouddfe/topics/test-topic-proto";
  @Rule public transient TestPipeline writePipeline = TestPipeline.create();
  @Rule public transient TestPipeline readPipeline = TestPipeline.create();
  @Rule public transient TestPubsubSignal signal = TestPubsubSignal.create();

  @Test
  public void testWriteReadProtos() throws Exception {
    readPipeline.getOptions().as(TestPipelineOptions.class).setBlockOnRun(false);

    Timestamp time = Timestamp.newBuilder().setSeconds(100).setNanos(10).build();
    ImmutableList<Primitive> inputs =
        ImmutableList.of(
            Primitive.newBuilder().setPrimitiveInt32(10).build(),
            Primitive.newBuilder().setPrimitiveBool(true).build(),
            Primitive.newBuilder().setPrimitiveString("Hello, World").build(),
            Primitive.newBuilder().setPrimitiveTimestamp(time).build());

    writePipeline
        .apply(Create.of(inputs))
        .apply(
            PubsubIO.writeProtos(Primitive.class)
                .to(TOPIC_PATH));

    PCollection<Primitive> messages =
        readPipeline
            .apply(
                PubsubIO.readProtoDynamicMessages(Primitive.getDescriptor())
                    .fromTopic(TOPIC_PATH))
            .apply(
                "Return To Primitive",
                MapElements.into(TypeDescriptor.of(Primitive.class))
                    .via(
                        (DynamicMessage message) -> {
                          System.out.println("MESSAGE:" + message);

                          try {
                            return Primitive.parseFrom(message.toByteArray());
                          } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException("Could not return to Primitive", e);
                          }
                        }));

    messages.apply(signal.signalSuccessWhen(messages.getCoder(), anyMessages -> true));

    Supplier<Void> start = signal.waitForStart(Duration.standardMinutes(1));
    readPipeline.apply(signal.signalStart());
    PipelineResult job = readPipeline.run();
    start.get();
    writePipeline.run();

    signal.waitForSuccess(Duration.standardMinutes(1));



    job.cancel();
  }
}
