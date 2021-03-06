/*
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.hyperfoil.cli.commands;

import io.hyperfoil.api.config.Benchmark;
import io.hyperfoil.api.config.Protocol;
import io.hyperfoil.api.http.HttpMethod;
import io.hyperfoil.api.statistics.LongValue;
import io.hyperfoil.api.statistics.StatisticsSnapshot;
import io.hyperfoil.cli.context.HyperfoilCommandInvocation;
import io.hyperfoil.api.config.BenchmarkBuilder;
import io.hyperfoil.api.config.PhaseBuilder;
import io.hyperfoil.core.builders.StepCatalog;
import io.hyperfoil.core.handlers.ByteBufSizeRecorder;
import io.hyperfoil.core.impl.LocalBenchmarkData;
import io.hyperfoil.core.impl.LocalSimulationRunner;
import io.hyperfoil.core.impl.statistics.StatisticsCollector;
import io.hyperfoil.core.util.Util;

import org.HdrHistogram.HistogramIterationValue;
import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.utils.ANSI;
import org.aesh.utils.Config;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;


public class Wrk {

    //ignore logging when running in the console below severe
   static {
      Handler[] handlers =
              Logger.getLogger( "" ).getHandlers();
      for ( int index = 0; index < handlers.length; index++ ) {
         handlers[index].setLevel( Level.SEVERE);
      }
   }


   public static void main(String[] args) {

      //set logger impl
      System.setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, "io.vertx.core.logging.Log4j2LogDelegateFactory");

      try {
         AeshRuntimeRunner.builder().command(WrkCommand.class).args(args).execute();
      }
      catch (Exception e) {
         System.out.println("Failed to execute command:"+ e.getMessage());
         e.printStackTrace();
         //todo: should provide help info here, will be added in newer version of æsh
         //System.out.println(runtime.commandInfo("wrk"));
      }
   }

   @CommandDefinition(name = "wrk", description = "Runs a workload simluation against one endpoint using the same vm")
   public class WrkCommand implements Command<CommandInvocation> {
      @Option(shortName = 'c', description = "Total number of HTTP connections to keep open", required = true)
      int connections;

      @Option(shortName = 'd', description = "Duration of the test, e.g. 2s, 2m, 2h", required = true)
      String duration;

      @Option(shortName = 't', description = "Total number of threads to use.")
      int threads;

      @Option(shortName = 'R', description = "Work rate (throughput)", required = true)
      int rate;

      @Option(shortName = 's', description = "!!!NOT SUPPORTED: LuaJIT script")
      String script;

      @Option(shortName = 'h', hasValue = false, overrideRequired = true)
      boolean help;

      @OptionList(shortName = 'H', name = "header", description = "HTTP header to add to request, e.g. \"User-Agent: wrk\"")
      List<String> headers;

      @Option(description = "Print detailed latency statistics", hasValue = false)
      boolean latency;

      @Option(description = "Record a timeout if a response is not received within this amount of time.", defaultValue = "60s")
      String timeout;

      @Argument(description = "URL that should be accessed", required = true)
      String url;

      String path;
      String[][] parsedHeaders;

      private boolean executedInCli = false;

      @Override
      public CommandResult execute(CommandInvocation commandInvocation) {
          if(help) {
             commandInvocation.println(commandInvocation.getHelpInfo("wrk"));
             return CommandResult.SUCCESS;
          }
         if (script != null) {
            commandInvocation.println("Scripting is not supported at this moment.");
         }
         if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
         }
         URI uri;
         try {
            uri = new URI(url);
         } catch (URISyntaxException e) {
            commandInvocation.println("Failed to parse URL: "+ e.getMessage());
            return CommandResult.FAILURE;
         }
         path = uri.getPath();
         if (uri.getQuery() != null) {
            path = path + "?" + uri.getQuery();
         }
         if (uri.getFragment() != null) {
            path = path + "#" + uri.getFragment();
         }
         if (headers != null) {
            parsedHeaders = new String[headers.size()][];
            for (int i = 0; i < headers.size(); i++) {
               String h = headers.get(i);
               int colonIndex = h.indexOf(':');
               if (colonIndex < 0) {
                  commandInvocation.println(String.format("Cannot parse header '%s', ignoring.", h));
                  continue;
               }
               String header = h.substring(0, colonIndex).trim();
               String value = h.substring(colonIndex + 1).trim();
               parsedHeaders[i] = new String[] { header, value };
            }
         }
         else {
            parsedHeaders = null;
         }
         //check if we're running in the cli
         if(commandInvocation instanceof HyperfoilCommandInvocation)
            executedInCli = true;

         Protocol protocol = Protocol.fromScheme(uri.getScheme());
         BenchmarkBuilder builder = new BenchmarkBuilder(null, new LocalBenchmarkData())
               .name("wrk " + new SimpleDateFormat("YY/MM/dd HH:mm:ss").format(new Date()))
               .http()
                  .protocol(protocol).host(uri.getHost()).port(protocol.portOrDefault(uri.getPort()))
                  .sharedConnections(connections)
               .endHttp()
               .threads(this.threads);

         addPhase(builder, "calibration", "1s");
         addPhase(builder, "test", duration).startAfter("calibration").maxDuration(duration);
         Benchmark benchmark = builder.build();

         // TODO: allow running the benchmark from remote instance
         LocalSimulationRunner runner = new LocalSimulationRunner(benchmark);
         commandInvocation.println("Running for " + duration + " test @ " + url);
         commandInvocation.println(threads + " threads and " + connections + " connections");

         if(executedInCli) {
            ((HyperfoilCommandInvocation) commandInvocation).context().setBenchmark(benchmark);
            startRunnerInCliMode(runner, benchmark, (HyperfoilCommandInvocation) commandInvocation);
         } else {
            runner.run();
            StatisticsCollector collector = new StatisticsCollector(benchmark);
            runner.visitStatistics(collector);
            StatisticsSnapshot total = new StatisticsSnapshot();
            collector.visitStatistics((phase, stepId, metric, stats, countDown) -> {
               if ("test".equals(phase.name())) {
                  stats.addInto(total);
               }
            }, null);
            printStats(total, commandInvocation);
         }

         return CommandResult.SUCCESS;
      }

      private void startRunnerInCliMode(LocalSimulationRunner runner, Benchmark benchmark,
                                        HyperfoilCommandInvocation invocation) {

         CountDownLatch latch = new CountDownLatch(1);
         Thread thread  = new Thread(() -> {runner.run(); latch.countDown();});
         thread.start();

         long startTime = System.currentTimeMillis();
         StatisticsCollector collector = new StatisticsCollector(benchmark);
         StatisticsSnapshot total = new StatisticsSnapshot();
         while(latch.getCount() > 0) {

            long duration = System.currentTimeMillis() - startTime;
            if(duration % 800 == 0) {
               invocation.getShell().write(ANSI.CURSOR_START);
               invocation.getShell().write(ANSI.ERASE_WHOLE_LINE);
               runner.visitStatistics(collector);
               collector.visitStatistics((phase, stepId, metric, stats, countDown) -> {
                  if ("test".equals(phase.name())) {
                     double durationSeconds = (stats.histogram.getEndTimeStamp() - stats.histogram.getStartTimeStamp()) / 1000d;
                     invocation.print("Requests/sec: " + String.format("%.02f", stats.histogram.getTotalCount() / durationSeconds));
                     stats.addInto(total);
                  }
               }, null);

               try {
                  Thread.sleep(10);
               } catch(InterruptedException e) {
                  //if we're interrupted, lets try to interrupt the benchmark...
                  invocation.println("Interrupt received, trying to abort run...");
                  thread.interrupt();
                  latch.countDown();
               }
            }
         }
         invocation.context().setRunning(false);
         invocation.println(Config.getLineSeparator()+"benchmark finished");
         runner.visitStatistics(collector);
         collector.visitStatistics((phase, stepId, metric, stats, countDown) -> {
            if ("test".equals(phase.name())) {
               stats.addInto(total);
            }
         }, null);
         printStats(total, invocation);
      }

      private PhaseBuilder addPhase(BenchmarkBuilder benchmarkBuilder, String phase, String duration) {
         return benchmarkBuilder.addPhase(phase).constantPerSec(rate)
                  .duration(duration)
                  .maxSessionsEstimate(rate * 15)
                  .scenario()
                     .initialSequence("request")
                        .step(StepCatalog.class).httpRequest(HttpMethod.GET)
                           .path(path)
                           .headerAppender((session, request) -> {
                              if (parsedHeaders != null) {
                                 for (String[] header : parsedHeaders) {
                                    request.putHeader(header[0], header[1]);
                                 }
                              }
                           })
                           .timeout(timeout)
                           .handler()
                              .rawBytesHandler(new ByteBufSizeRecorder("bytes"))
                           .endHandler()
                        .endStep()
                        .step(StepCatalog.class).awaitAllResponses()
                     .endSequence()
                  .endScenario();
      }

      private void printStats(StatisticsSnapshot stats, CommandInvocation invocation) {
         long dataRead = ((LongValue) stats.custom.get("bytes")).value();
         double durationSeconds = (stats.histogram.getEndTimeStamp() - stats.histogram.getStartTimeStamp()) / 1000d;
         invocation.println("                  Avg     Stdev       Max");
         invocation.println("Latency:    " + Util.prettyPrintNanos((long) stats.histogram.getMean()) + " "
               + Util.prettyPrintNanos((long) stats.histogram.getStdDeviation()) + " " + Util.prettyPrintNanos(stats.histogram.getMaxValue()));
         if (latency) {
            invocation.println("Latency Distribution");
            for (double percentile : new double[] { 0.5, 0.75, 0.9, 0.99, 0.999, 0.9999, 0.99999, 1.0}) {
               invocation.println(String.format("%7.3f", 100 * percentile)+" "+Util.prettyPrintNanos(stats.histogram.getValueAtPercentile(100 * percentile)));
            }
            invocation.println("----------------------------------------------------------");
            invocation.println("Detailed Percentile Spectrum");
            invocation.println("    Value  Percentile  TotalCount  1/(1-Percentile)");
            for (HistogramIterationValue value : stats.histogram.percentiles(5)) {
               invocation.println(Util.prettyPrintNanos(value.getValueIteratedTo())+" "+String.format("%9.5f%%  %10d  %15.2f",
                     value.getPercentile(), value.getTotalCountToThisValue(), 100/(100 - value.getPercentile())));
            }
            invocation.println("----------------------------------------------------------");
         }
         invocation.println(stats.histogram.getTotalCount()+" requests in "+durationSeconds+"s, "+ Util.prettyPrintData(dataRead)+" read");
         invocation.println("Requests/sec: "+String.format("%.02f", stats.histogram.getTotalCount() / durationSeconds));
         if (stats.errors() > 0) {
            invocation.println("Socket errors: connect "+stats.connectFailureCount+", reset "+stats.resetCount+", timeout "+stats.timeouts);
            invocation.println("Non-2xx or 3xx responses: "+ stats.status_4xx + stats.status_5xx + stats.status_other);
         }
         invocation.println("Transfer/sec: "+ Util.prettyPrintData(dataRead / durationSeconds));
      }

   }

}
