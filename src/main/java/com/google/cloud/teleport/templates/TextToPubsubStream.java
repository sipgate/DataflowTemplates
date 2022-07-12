/*
 * Copyright (C) 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.teleport.templates;

import com.google.cloud.teleport.templates.TextToPubsub.Options;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Watch;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

import java.util.Arrays;
import java.util.List;

/**
 * The {@code TextToPubsubStream} is a streaming version of {@code TextToPubsub} pipeline that
 * publishes records to Cloud Pub/Sub from a set of files. The pipeline continuously polls for new
 * files, reads them row-by-row and publishes each record as a string message. The polling interval
 * is fixed and equals to 10 seconds. At the moment, publishing messages with attributes is
 * unsupported.
 *
 * <p>Example Usage:
 *
 * <pre>
 * {@code mvn compile exec:java \
 * -Dexec.mainClass=com.google.cloud.teleport.templates.TextToPubsubStream \
 * -Dexec.args=" \
 * --project=${PROJECT_ID} \
 * --stagingLocation=gs://${STAGING_BUCKET}/dataflow/pipelines/${PIPELINE_FOLDER}/staging \
 * --tempLocation=gs://${STAGING_BUCKET}/dataflow/pipelines/${PIPELINE_FOLDER}/temp \
 * --netwworkdeinemudda=gs://${STAGING_BUCKET}/dataflow/pipelines/${PIPELINE_FOLDER}/temp \
 * --runner=DataflowRunner \
 * --inputFilePattern=gs://path/to/*.csv"
 * }
 * </pre>
 */
public class TextToPubsubStream extends TextToPubsub {
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.standardSeconds(10);

    /**
     * Main entry-point for the pipeline. Reads in the command-line arguments, parses them, and
     * executes the pipeline.
     *
     * @param args Arguments passed in from the command-line.
     */
    public static void main(String[] args) {

        // Parse the user options passed from the command-line
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

        run(options);
    }

    /**
     * Executes the pipeline with the provided execution parameters.
     *
     * @param options The execution parameters.
     */
    public static PipelineResult run(Options options) {
        // Create the pipeline.
        Pipeline pipeline = Pipeline.create(options);

        /*
         * Steps:
         *  1) Read from the text source.
         *  2) Write each text record to Pub/Sub
         */
        final List<String> topics = Arrays.asList(
                "projects/dev-db05-subscriber/topics/jobqueue",
                "projects/dev-db05-subscriber/topics/jobqueue2"
        );
        //final List<String> topics = GehInDatastoreUndHoleDieListeDerTopicsAufDieNachrichtenAnkoimmenSollen();

        final PCollection<String> read_text_data = pipeline
                .apply(
                        "Read Text Data",
                        TextIO.read()
                                .from(options.getInputFilePattern())
                                .watchForNewFiles(DEFAULT_POLL_INTERVAL, Watch.Growth.never()));

        for (String topic : topics) {
            read_text_data.apply("Write to PubSub", PubsubIO.writeStrings().to(topic));
        }

        return pipeline.run();
    }
}
