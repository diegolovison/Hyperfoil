# Hyperfoil Usage Guide

This guide shows how to run benchmarks using both the interactive CLI and the standalone `run` command.

## Prerequisites

Start a simple HTTP server for testing:
```bash
# Terminal 1 - Start Python HTTP server
python3 -m http.server 8080
```

## Build Hyperfoil
```bash
mvn clean package -DskipTests
```

## Method 1: Interactive CLI

### Step 2: Start the CLI
```bash
java -jar /home/dlovison/github/Hyperfoil/Hyperfoil/clustering/target/cli.jar
```

### Step 3: Start Local Controller
```
[hyperfoil]$ start-local
```

### Step 4: Upload Benchmark
```
[hyperfoil@in-vm]$ upload /home/dlovison/test.hf.yaml
```

### Step 5: Run Benchmark
```
[hyperfoil@in-vm]$ run test
```

### Step 6: View Statistics (Optional - if not shown automatically)
```
[hyperfoil@in-vm]$ stats -t
```

### Expected Output
```
Total stats from run 0001
PHASE     METRIC  THROUGHPUT     REQUESTS  MEAN     STD_DEV    MAX      p50      p90      p99      p99.9    p99.99   TIMEOUTS  ERRORS  BLOCKED  2xx  3xx  4xx  5xx  CACHE
loadTest  test    48.88 req/s    984       1.26 ms  640.27 µs  5.77 ms  1.11 ms  1.84 ms  4.01 ms  5.77 ms  5.77 ms  0         6       0 ns     978  0    0    0    0
```

---

## Method 2: Standalone Run Command

### Step 2: Run Benchmark Directly
```bash
# Terminal 2 - Run benchmark
java -jar /home/dlovison/github/Hyperfoil/Hyperfoil/clustering/target/run.jar /home/dlovison/test.hf.yaml
```

### What Happens
1. Automatically starts an in-VM controller
2. Uploads the benchmark
3. Runs the benchmark
4. **Automatically displays statistics** (new feature!)

## Sample Benchmark File

Create `/home/dlovison/test.hf.yaml`:
```yaml
name: test
http:
  host: http://localhost:8080
  sharedConnections: 100
phases:
- loadTest:
    always:
      users: 50
      duration: 20s
      scenario:
      - test:
        - httpRequest:
            GET: /
```
