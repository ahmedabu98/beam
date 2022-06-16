package org.apache.beam.examples;

import com.google.common.primitives.Ints;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.extensions.gcp.util.gcsfs.GcsPath;
import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;


import java.util.stream.IntStream;

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

  public static void main(String[] args){
    writeTextIOOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(writeTextIOOptions.class);
    System.out.println(options.getOutput());

    Pipeline p = Pipeline.create(options);

    int[] rangeIntegers = IntStream.range(0, options.getMaxNumber()).toArray();
    Iterable<Integer> elements = Ints.asList(rangeIntegers);

    ResourceId tempDirectory = FileSystems.matchNewResource(options.getTemp(), true);

    p
        .apply(Create.of(elements))
        .apply(ParDo.of(new DoFn<Integer, String>() {
          @ProcessElement
          public void processElement(ProcessContext c) {
            c.output("This is element number " + c.element().toString());
          }
        }))
        .apply("Write Files", TextIO.write()
            .to(options.getOutput())
            .withTempDirectory(tempDirectory)
            .withoutSharding());

    p.run().waitUntilFinish();
  }
}
