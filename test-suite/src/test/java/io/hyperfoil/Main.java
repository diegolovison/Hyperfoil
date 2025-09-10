package io.hyperfoil;

import io.hyperfoil.api.config.Benchmark;
import io.hyperfoil.core.parser.BenchmarkParser;
import io.hyperfoil.core.parser.ParserException;
import io.hyperfoil.core.test.TestUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException, ParserException {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("AGENT_1", "server01");
        arguments.put("AGENT_2", "server02");
        arguments.put("AGENT_3", "server03");
        arguments.put("INSURANCE_URL", "insurance-url");
        arguments.put("SHARED_CONNECTIONS", "1");
        arguments.put("VEHICLE_URL", "vehicle-url");
        arguments.put("TX_RATE", "100");
        arguments.put("MAX_SESSIONS", "100");
        arguments.put("RAMP_UP_DURATION", "100");


        Benchmark benchmark = loadBenchmark(new FileInputStream("/path/to/assets/benchmark.yaml"), arguments);
    }

    private static Benchmark loadBenchmark(InputStream config, Map<String, String> arguments) throws IOException, ParserException {
        return BenchmarkParser.instance().buildBenchmark(config, TestUtil.benchmarkData(), arguments);
    }
}
