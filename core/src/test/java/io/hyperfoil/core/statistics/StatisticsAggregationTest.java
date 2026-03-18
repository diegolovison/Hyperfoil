package io.hyperfoil.core.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import io.hyperfoil.api.statistics.Statistics;

public class StatisticsAggregationTest {
   private static final Logger log = LogManager.getLogger(StatisticsAggregationTest.class);

   @Test
   public void testStatisticsTimeDistributionMismatch() {
      long baseTime = 1710774000000L;
      long millis = 1000L;
      Map<Long, Integer> requestData = new LinkedHashMap<>();
      requestData.put(baseTime + (1 * millis), 1);
      requestData.put(baseTime + (3 * millis), 1);
      requestData.put(baseTime + (9 * millis), 1);
      requestData.put(baseTime + (11 * millis), 3);
      requestData.put(baseTime + (13 * millis), 2);
      requestData.put(baseTime + (14 * millis), 1);
      requestData.put(baseTime + (15 * millis), 1);
      requestData.put(baseTime + (16 * millis), 3);
      requestData.put(baseTime + (18 * millis), 1);
      requestData.put(baseTime + (19 * millis), 6);
      requestData.put(baseTime + (20 * millis), 2);
      requestData.put(baseTime + (21 * millis), 1);
      requestData.put(baseTime + (22 * millis), 1);
      requestData.put(baseTime + (23 * millis), 5);
      requestData.put(baseTime + (24 * millis), 7);
      requestData.put(baseTime + (25 * millis), 2);
      requestData.put(baseTime + (26 * millis), 2);
      requestData.put(baseTime + (27 * millis), 2);
      requestData.put(baseTime + (28 * millis), 9);
      requestData.put(baseTime + (29 * millis), 5);
      requestData.put(baseTime + (30 * millis), 2);
      requestData.put(baseTime + (31 * millis), 3);
      requestData.put(baseTime + (32 * millis), 6);
      requestData.put(baseTime + (33 * millis), 7);
      requestData.put(baseTime + (34 * millis), 5);
      requestData.put(baseTime + (35 * millis), 6);
      requestData.put(baseTime + (36 * millis), 4);
      requestData.put(baseTime + (37 * millis), 8);
      requestData.put(baseTime + (38 * millis), 7);
      requestData.put(baseTime + (39 * millis), 6);
      requestData.put(baseTime + (40 * millis), 12);
      requestData.put(baseTime + (41 * millis), 8);
      requestData.put(baseTime + (42 * millis), 7);
      requestData.put(baseTime + (43 * millis), 13);
      requestData.put(baseTime + (44 * millis), 8);
      requestData.put(baseTime + (45 * millis), 7);
      requestData.put(baseTime + (46 * millis), 11);
      requestData.put(baseTime + (47 * millis), 9);
      requestData.put(baseTime + (48 * millis), 9);
      requestData.put(baseTime + (49 * millis), 13);
      requestData.put(baseTime + (50 * millis), 13);
      requestData.put(baseTime + (51 * millis), 13);
      requestData.put(baseTime + (52 * millis), 9);
      requestData.put(baseTime + (53 * millis), 7);
      requestData.put(baseTime + (54 * millis), 11);
      requestData.put(baseTime + (55 * millis), 13);
      requestData.put(baseTime + (56 * millis), 11);
      requestData.put(baseTime + (57 * millis), 10);
      requestData.put(baseTime + (58 * millis), 16);
      requestData.put(baseTime + (59 * millis), 14);
      requestData.put(baseTime + (60 * millis), 13);
      requestData.put(baseTime + (61 * millis), 14);
      requestData.put(baseTime + (62 * millis), 22);
      requestData.put(baseTime + (63 * millis), 17);
      requestData.put(baseTime + (64 * millis), 13);
      requestData.put(baseTime + (65 * millis), 15);
      requestData.put(baseTime + (66 * millis), 3);

      Statistics stats = new Statistics(baseTime);
      long end = 0;
      int totalRecorded = 0;
      for (Map.Entry<Long, Integer> entry : requestData.entrySet()) {
         long timestamp = entry.getKey();
         int count = entry.getValue();
         for (int i = 0; i < count; i++) {
            stats.recordResponse(timestamp, 100_000);
         }
         totalRecorded += count;
         end = timestamp + millis;
      }
      stats.end(end);

      Map<Long, Integer> statsPerTimestamp = new HashMap<>();
      stats.visitSnapshots(snapshot -> {
         if (snapshot.responseCount > 0) {
            statsPerTimestamp.put(snapshot.histogram.getStartTimeStamp(), snapshot.responseCount);
         }
      });

      int totalCollected = statsPerTimestamp.values().stream().mapToInt(Integer::intValue).sum();
      assertEquals(totalRecorded, totalCollected);

      log.info("Per-timestamp comparison:");
      log.info("Offset(s) | JFR | Statistics | Match");
      log.info("----------|-----|------------|------");

      for (Map.Entry<Long, Integer> entry : requestData.entrySet()) {
         long timestamp = entry.getKey();
         int value = entry.getValue();
         int stat = statsPerTimestamp.getOrDefault(timestamp, 0);
         boolean match = value == stat;
         String matchStr = match ? "✓" : "✗";
         int offsetSeconds = (int) ((timestamp - baseTime) / millis);
         log.info(String.format("   +%2ds    | %3d |    %3d     |  %s", offsetSeconds, value, stat, matchStr));
      }

      for (Map.Entry<Long, Integer> entry : requestData.entrySet()) {
         long timestamp = entry.getKey();
         int value = entry.getValue();
         int stat = statsPerTimestamp.getOrDefault(timestamp, 0);
         assertEquals(value, stat, "Timestamp " + timestamp + ": value=" + value + " but Statistics=" + stat);
      }
   }
}
