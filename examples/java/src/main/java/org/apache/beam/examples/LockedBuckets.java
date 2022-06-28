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

import com.google.common.primitives.Ints;
import java.util.stream.IntStream;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;

/*
gradle clean execute -DmainClass=org.apache.beam.examples.LockedBuckets
 */
public class LockedBuckets {

  public interface writeTextIOOptions extends PipelineOptions {
    @Description("Output to write to")
    @Default.String("gs://ahmedabualsaud-usc/file2")
    String getOutput();

    void setOutput(String value);

    @Description("Directory for temp files")
    @Default.String("gs://ahmedabualsaud-euw/tmp")
    String getTemp();

    void setTemp(String value);

    @Description("Max Number")
    @Default.Integer(20)
    Integer getMaxNumber();

    void setMaxNumber(Integer value);
  }

  public static void main(String[] args) {
    writeTextIOOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(writeTextIOOptions.class);
    System.out.println(options.getOutput());

    Pipeline p = Pipeline.create(options);

    int[] rangeIntegers = IntStream.range(0, options.getMaxNumber()).toArray();
    Iterable<Integer> elements = Ints.asList(rangeIntegers);

    ResourceId tempDirectory = FileSystems.matchNewResource(options.getTemp(), true);

    p.apply(Create.of(elements))
        .apply(
            ParDo.of(
                new DoFn<Integer, String>() {
                  @ProcessElement
                  public void processElement(ProcessContext c) {
                    c.output("This is element number " + c.element().toString());
                  }
                }))
        .apply(
            "Write Files",
            TextIO.write()
                .to(options.getOutput())
                .withTempDirectory(tempDirectory)
                .withoutSharding());

    p.run().waitUntilFinish();
  }
}
