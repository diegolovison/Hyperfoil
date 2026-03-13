package io.hyperfoil.benchmark.standalone;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.hyperfoil.benchmark.BaseBenchmarkTest;
import io.hyperfoil.cli.commands.LoadAndRun;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

@Tag("io.hyperfoil.test.Benchmark")
public class RunInsuranceTest extends BaseBenchmarkTest {

   private final AtomicInteger responseTimer = new AtomicInteger(1);
   private final AtomicInteger requestCounter = new AtomicInteger(0);

   private int getNextDelay() {
      int current = responseTimer.incrementAndGet();
      return current % 2000 == 0 ? 1000 : 1;
   }

   private boolean shouldFail() {
      int current = requestCounter.incrementAndGet();
      return current % 500 == 0;
   }

   @Override
   protected Handler<HttpServerRequest> getRequestHandler() {
      return req -> {
         String path = req.path();
         String method = req.method().name();
         int delayMs = getNextDelay();
         boolean fail = shouldFail();

         if (fail) {
            req.response()
                  .setStatusCode(500)
                  .end();
         } else if (path.equals("/insurance/rest/secure/policyHolder/get")) {
            vertx.setTimer(delayMs, id -> req.response()
                  .putHeader("content-type", "application/json")
                  .end("{\"id\": 1, \"firstName\": \"John\", \"lastName\": \"Doe\", \"gender\": \"MALE\", \"birthDate\": \"1990-01-01\", \"email\": \"john.doe@example.com\", \"movingViolations\": 0, \"claims\": 0, \"accidents\": 0, \"address\": {\"city\": \"New York\", \"state\": \"NY\", \"street1\": \"123 Main St\", \"street2\": \"Apt 4\", \"country\": \"USA\", \"zip\": \"10001\", \"phone\": \"5551234567\"}, \"vehicles\": [{\"vin\": \"VIN123\", \"annualMiles\": 12000, \"description\": {\"make\": \"Toyota\", \"model\": \"Camry\", \"year\": \"2022\", \"transmission\": \"Automatic\"}}]}"));
         } else if (path.equals("/insurance/rest/registration/add") && method.equals("POST")) {
            vertx.setTimer(delayMs, id -> req.response()
                  .putHeader("content-type", "application/json")
                  .setStatusCode(200)
                  .end("{\"status\": \"success\"}"));
         } else if (path.startsWith("/insurance/rest/authentication/getToken")) {
            vertx.setTimer(delayMs, id -> req.response()
                  .putHeader("content-type", "application/json")
                  .end("{\"token\": \"dummy-token-12345\"}"));
         }
         // Default response for unhandled endpoints
         else {
            req.response()
                  .setStatusCode(500)
                  .end();
         }
      };
   }

   @Test
   public void testRunMain() {
      String benchmark = getBenchmarkPath("scenarios/insurance.hf.yaml");
      String reportPath = System.getProperty("java.io.tmpdir") + "/insurance-report.html";
      LoadAndRun.main(new String[] {
            "-PINSURANCE_URL=http://localhost:" + httpServer.actualPort(),
            "-PSHARED_CONNECTIONS=100",
            "-PTX_RATE=2000",
            "-PMAX_SESSIONS=500",
            "-PRAMP_UP_DURATION=20s",
            "-PSTEADY_DURATION=60s",
            "--output=" + reportPath,
            benchmark
      });
      System.out.println("HTML report generated at: " + reportPath);
   }
}
