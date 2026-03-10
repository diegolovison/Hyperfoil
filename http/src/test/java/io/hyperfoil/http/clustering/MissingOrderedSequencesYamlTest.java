package io.hyperfoil.http.clustering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.hyperfoil.api.config.Benchmark;
import io.hyperfoil.api.statistics.StatisticsSnapshot;
import io.hyperfoil.core.parser.BenchmarkParser;
import io.hyperfoil.core.test.TestUtil;
import io.hyperfoil.http.BaseHttpScenarioTest;

public class MissingOrderedSequencesYamlTest extends BaseHttpScenarioTest {

   @Override
   protected void initRouter() {
      // A lightning-fast dummy endpoint. It responds instantly (0ms).
      router.get("/ping/:lastName").handler(ctx -> {
         String lastName = ctx.pathParam("lastName");
         ctx.response().setStatusCode(200).end("lastName: " + lastName);
      });
   }

   @Test
   public void testMissingOrderedSequencesTrap() throws Exception {

      // Ensure server is running
      assertThat(server).isNotNull();
      assertThat(server.actualPort()).isGreaterThan(0);

      // Language=yaml
      String yaml = """
            name: missing-ordered-sequences
            http:
              host: !concat [ "http://localhost:", !param PORT ]
            ergonomics:
              compensateInternalLatency: true
            phases:
            - test:
                constantRate:
                  usersPerSec: 10
                  duration: 1s
                  maxSessions: 10
                  scenario:
                   orderedSequences:
                   - testSequence:
                     - randomItem:
                         file: data/lastNames.txt
                         toVar: lastName
                     - httpRequest:
                         GET: /ping/${lastName}
                         metric: getInsuranceMetric
            """;

      System.out.println("Server port: " + server.actualPort());
      System.out.println("=== Testing compensateInternalLatency with randomItem before httpRequest ===");
      System.out.println("Expected: httpRequest should use session start time (it's the first HTTP request)");
      System.out.println("Expected: randomItem should NOT use session start time (it's not an HTTP request)");

      // Parse the YAML exactly as Hyperfoil would via the CLI
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
      Benchmark benchmark = BenchmarkParser.instance().buildBenchmark(
            inputStream, TestUtil.benchmarkData(), Map.of("PORT", String.valueOf(server.actualPort())));

      Map<String, StatisticsSnapshot> stats = runScenario(benchmark);

      // Fetch the statistics recorded during the run
      StatisticsSnapshot victimStats = stats.get("getInsuranceMetric");

      assertThat(victimStats).isNotNull();
      assertThat(victimStats.requestCount).isGreaterThan(0);
      System.out.println("Test completed successfully - httpRequest was executed as the first HTTP request step");
   }
}
