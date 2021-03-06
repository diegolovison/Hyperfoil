package io.hyperfoil.client;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.hyperfoil.api.config.Benchmark;
import io.hyperfoil.api.statistics.StatisticsSummary;

/**
 * API for server control
 */
public interface Client {
   BenchmarkRef register(Benchmark benchmark);

   List<String> benchmarks();

   BenchmarkRef benchmark(String name);

   List<String> runs();

   RunRef run(String id);

   long ping();

   interface BenchmarkRef {
      String name();
      Benchmark get();
      RunRef start();
   }

   interface RunRef {
      String id();
      Run get();
      RunRef kill();
      Map<String, Map<String, MinMax>> sessionStatsRecent();
      Map<String, Map<String, MinMax>> sessionStatsTotal();
      // TODO: server should expose JSON-formatted variants
      Collection<String> sessions();
      Collection<String> connections();
      RequestStatisticsResponse statsRecent();
      RequestStatisticsResponse statsTotal();
      Collection<CustomStats> customStats();
   }

   class Agent {
      public final String name;
      public final String address;
      public final String status;

      @JsonCreator
      public Agent(@JsonProperty("name") String name, @JsonProperty("address") String address, @JsonProperty("status") String status) {
         this.name = name;
         this.address = address;
         this.status = status;
      }
   }

   class Phase {
      public final String name;
      public final String status;
      public final String type;
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss.S")
      public final Date started;
      public final String remaining;
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss.S")
      public final Date completed;
      public final String totalDuration;
      public final String description;

      @JsonCreator
      public Phase(@JsonProperty("name") String name, @JsonProperty("status") String status,
                   @JsonProperty("type") String type,
                   @JsonProperty("started") Date started, @JsonProperty("remaining") String remaining,
                   @JsonProperty("completed") Date completed, @JsonProperty("totalDuration") String totalDuration,
                   @JsonProperty("description") String description) {
         this.name = name;
         this.status = status;
         this.type = type;
         this.started = started;
         this.remaining = remaining;
         this.completed = completed;
         this.totalDuration = totalDuration;
         this.description = description;
      }
   }

   class Run {
      public final String id;
      public final String benchmark;
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss.S")
      public final Date started;
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss.S")
      public final Date terminated;
      public final String description;
      public final Collection<Phase> phases;
      public final Collection<Agent> agents;
      public final Collection<String> errors;

      @JsonCreator
      public Run(@JsonProperty("id") String id, @JsonProperty("benchmark") String benchmark,
                 @JsonProperty("started") Date started, @JsonProperty("terminated") Date terminated,
                 @JsonProperty("description") String description,
                 @JsonProperty("phases") Collection<Phase> phases,
                 @JsonProperty("agents") Collection<Agent> agents,
                 @JsonProperty("errors") Collection<String> errors) {
         this.id = id;
         this.benchmark = benchmark;
         this.started = started;
         this.terminated = terminated;
         this.description = description;
         this.phases = phases;
         this.agents = agents;
         this.errors = errors;
      }

   }

   class MinMax {
      public final int min;
      public final int max;

      @JsonCreator
      public MinMax(@JsonProperty("min") int min, @JsonProperty("max") int max) {
         this.min = min;
         this.max = max;
      }
   }

   class RequestStatisticsResponse {
      public final String status;
      public final List<RequestStats> statistics;

      @JsonCreator
      public RequestStatisticsResponse(@JsonProperty("status") String status,
                                       @JsonProperty("statistics") List<RequestStats> statistics) {
         this.status = status;
         this.statistics = statistics;
      }
   }

   class RequestStats {
      public final String phase;
      public final String metric;
      public final StatisticsSummary summary;

      @JsonCreator
      public RequestStats(@JsonProperty("phase") String phase, @JsonProperty("metric") String metric,
                          @JsonProperty("summary") StatisticsSummary summary) {
         this.phase = phase;
         this.metric = metric;
         this.summary = summary;
      }
   }

   class CustomStats {
      public final String phase;
      public final int stepId;
      public final String metric;
      public final String customName;
      public final String value;

      @JsonCreator
      public CustomStats(@JsonProperty("phase") String phase, @JsonProperty("stepId") int stepId,
                         @JsonProperty("metric") String metric, @JsonProperty("customName") String customName,
                         @JsonProperty("value") String value) {
         this.phase = phase;
         this.stepId = stepId;
         this.metric = metric;
         this.customName = customName;
         this.value = value;
      }
   }
}
