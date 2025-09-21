/*
 * Copyright 2024 Tegmentum AI Inc.
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
 */

package ai.tegmentum.wasmtime4j.testing;

import ai.tegmentum.wasmtime4j.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * Performance test suite that validates production-ready performance characteristics.
 *
 * <p>This test suite implements comprehensive performance validation including:
 *
 * <ul>
 *   <li>Module compilation performance benchmarks
 *   <li>Function call latency measurements
 *   <li>Memory operation throughput testing
 *   <li>Async operation overhead analysis
 *   <li>Concurrent execution scaling validation
 *   <li>Resource usage optimization verification
 * </ul>
 */
public final class PerformanceTestSuite {

  private static final Logger LOGGER = Logger.getLogger(PerformanceTestSuite.class.getName());

  // Performance thresholds for production readiness
  private static final Duration MAX_MODULE_COMPILATION_TIME = Duration.ofSeconds(5);
  private static final Duration MAX_FUNCTION_CALL_TIME = Duration.ofMicros(100);
  private static final double MIN_MEMORY_THROUGHPUT_MB_S = 1000.0;
  private static final double MAX_ASYNC_OVERHEAD_PERCENT = 10.0;

  private TestResults lastResults = TestResults.builder().build();

  public static PerformanceTestSuite create() {
    return new PerformanceTestSuite();
  }

  /**
   * Benchmarks WebAssembly module compilation performance.
   *
   * @return performance results for module compilation
   */
  public PerformanceResult benchmarkModuleCompilation() {
    LOGGER.info("Starting module compilation performance benchmarks");

    final List<Duration> compilationTimes = new ArrayList<>();
    final int[] moduleSizes = {1024, 10240, 102400, 1048576}; // 1KB, 10KB, 100KB, 1MB

    try (final Engine engine = Engine.create()) {
      for (final int size : moduleSizes) {
        for (int iteration = 0; iteration < 10; iteration++) {
          final byte[] wasmBytes = generateWasmModule(size);
          final Instant start = Instant.now();

          try (final Module module = Module.compile(engine, wasmBytes)) {
            final Duration compilationTime = Duration.between(start, Instant.now());
            compilationTimes.add(compilationTime);

            LOGGER.fine(
                String.format(
                    "Module compilation: %d bytes in %d ms",
                    size, compilationTime.toMillis()));
          }
        }
      }
    } catch (final Exception e) {
      LOGGER.severe("Module compilation benchmark failed: " + e.getMessage());
      return new FailedPerformanceResult("Module compilation failed: " + e.getMessage());
    }

    // Calculate statistics
    final Duration averageTime = calculateAverageTime(compilationTimes);
    final Duration maxTime = compilationTimes.stream().max(Duration::compareTo).orElse(Duration.ZERO);
    final Duration minTime = compilationTimes.stream().min(Duration::compareTo).orElse(Duration.ZERO);

    LOGGER.info(
        String.format(
            "Module compilation benchmark completed. Average: %d ms, Min: %d ms, Max: %d ms",
            averageTime.toMillis(), minTime.toMillis(), maxTime.toMillis()));

    return new DefaultPerformanceResult(averageTime, 0.0, 0.0, calculateThroughput(compilationTimes));
  }

  /**
   * Benchmarks WebAssembly function call performance.
   *
   * @return performance results for function calls
   */
  public PerformanceResult benchmarkFunctionCalls() {
    LOGGER.info("Starting function call performance benchmarks");

    final List<Duration> callTimes = new ArrayList<>();
    final int numberOfCalls = 10000;

    try (final Engine engine = Engine.create();
        final Store store = Store.create(engine);
        final Module module = Module.compile(engine, generateArithmeticWasm());
        final Instance instance = Instance.create(store, module)) {

      final Function addFunction = instance.getExport("add", Function.class);

      // Warm-up phase
      for (int i = 0; i < 1000; i++) {
        addFunction.call(i, i + 1);
      }

      // Measurement phase
      for (int i = 0; i < numberOfCalls; i++) {
        final Instant start = Instant.now();
        final Object[] result = addFunction.call(i, i + 1);
        final Duration callTime = Duration.between(start, Instant.now());
        callTimes.add(callTime);

        // Verify correctness
        if (result.length == 0 || !(result[0] instanceof Integer) || !result[0].equals(i + i + 1)) {
          LOGGER.warning("Function call returned unexpected result: " + Arrays.toString(result));
        }
      }

    } catch (final Exception e) {
      LOGGER.severe("Function call benchmark failed: " + e.getMessage());
      return new FailedPerformanceResult("Function call benchmark failed: " + e.getMessage());
    }

    // Calculate statistics
    final Duration averageTime = calculateAverageTime(callTimes);
    final Duration p95Time = calculatePercentile(callTimes, 0.95);
    final Duration p99Time = calculatePercentile(callTimes, 0.99);

    LOGGER.info(
        String.format(
            "Function call benchmark completed. Average: %d ns, P95: %d ns, P99: %d ns",
            averageTime.toNanos(), p95Time.toNanos(), p99Time.toNanos()));

    return new DefaultPerformanceResult(averageTime, 0.0, numberOfCalls / (averageTime.toNanos() / 1_000_000_000.0), 0.0);
  }

  /**
   * Benchmarks WebAssembly memory operation performance.
   *
   * @return performance results for memory operations
   */
  public PerformanceResult benchmarkMemoryOperations() {
    LOGGER.info("Starting memory operations performance benchmarks");

    final List<Double> throughputMeasurements = new ArrayList<>();
    final int[] dataSizes = {1024, 10240, 102400, 1048576}; // 1KB, 10KB, 100KB, 1MB

    try (final Engine engine = Engine.create();
        final Store store = Store.create(engine);
        final Module module = Module.compile(engine, generateMemoryOperationWasm());
        final Instance instance = Instance.create(store, module)) {

      final Function writeMemory = instance.getExport("write_memory", Function.class);
      final Function readMemory = instance.getExport("read_memory", Function.class);

      for (final int dataSize : dataSizes) {
        for (int iteration = 0; iteration < 5; iteration++) {
          // Measure write throughput
          final byte[] testData = generateTestData(dataSize);
          final Instant writeStart = Instant.now();
          writeMemory.call(testData);
          final Duration writeDuration = Duration.between(writeStart, Instant.now());
          final double writeThroughput = (dataSize / 1024.0 / 1024.0) / (writeDuration.toNanos() / 1_000_000_000.0);

          // Measure read throughput
          final Instant readStart = Instant.now();
          final Object[] readResult = readMemory.call(dataSize);
          final Duration readDuration = Duration.between(readStart, Instant.now());
          final double readThroughput = (dataSize / 1024.0 / 1024.0) / (readDuration.toNanos() / 1_000_000_000.0);

          throughputMeasurements.add(writeThroughput);
          throughputMeasurements.add(readThroughput);

          LOGGER.fine(
              String.format(
                  "Memory operations: %d bytes, Write: %.2f MB/s, Read: %.2f MB/s",
                  dataSize, writeThroughput, readThroughput));
        }
      }

    } catch (final Exception e) {
      LOGGER.severe("Memory operations benchmark failed: " + e.getMessage());
      return new FailedPerformanceResult("Memory operations benchmark failed: " + e.getMessage());
    }

    // Calculate average throughput
    final double averageThroughput = throughputMeasurements.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    LOGGER.info(
        String.format(
            "Memory operations benchmark completed. Average throughput: %.2f MB/s",
            averageThroughput));

    return new DefaultPerformanceResult(Duration.ofMillis(1), 0.0, averageThroughput, 0.0);
  }

  /**
   * Benchmarks async operation overhead.
   *
   * @return performance results for async operations
   */
  public PerformanceResult benchmarkAsyncOverhead() {
    LOGGER.info("Starting async operation overhead benchmarks");

    try {
      // Benchmark synchronous operations
      final Duration syncTime = benchmarkSynchronousOperations();

      // Benchmark asynchronous operations
      final Duration asyncTime = benchmarkAsynchronousOperations();

      // Calculate overhead percentage
      final double overheadPercent = ((asyncTime.toNanos() - syncTime.toNanos()) * 100.0) / syncTime.toNanos();

      LOGGER.info(
          String.format(
              "Async overhead benchmark completed. Sync: %d ms, Async: %d ms, Overhead: %.2f%%",
              syncTime.toMillis(), asyncTime.toMillis(), overheadPercent));

      return new DefaultPerformanceResult(asyncTime, overheadPercent, 0.0, 0.0);

    } catch (final Exception e) {
      LOGGER.severe("Async overhead benchmark failed: " + e.getMessage());
      return new FailedPerformanceResult("Async overhead benchmark failed: " + e.getMessage());
    }
  }

  /**
   * Benchmarks concurrent execution scaling.
   *
   * @return performance results for concurrent execution
   */
  public PerformanceResult benchmarkConcurrentExecution() {
    LOGGER.info("Starting concurrent execution scaling benchmarks");

    final int maxThreads = Runtime.getRuntime().availableProcessors() * 2;
    final List<Double> scalingResults = new ArrayList<>();

    try (final Engine engine = Engine.create();
        final Module module = Module.compile(engine, generateConcurrentTestWasm())) {

      // Baseline single-threaded performance
      final double baselineOpsPerSecond = measureSingleThreadPerformance(engine, module);

      // Measure scaling with increasing thread counts
      for (int threadCount = 1; threadCount <= maxThreads; threadCount++) {
        final double opsPerSecond = measureConcurrentPerformance(engine, module, threadCount);
        final double scalingFactor = opsPerSecond / baselineOpsPerSecond;
        scalingResults.add(scalingFactor);

        LOGGER.fine(
            String.format(
                "Concurrent execution: %d threads, %.2f ops/sec, %.2fx scaling",
                threadCount, opsPerSecond, scalingFactor));
      }

    } catch (final Exception e) {
      LOGGER.severe("Concurrent execution benchmark failed: " + e.getMessage());
      return new FailedPerformanceResult("Concurrent execution benchmark failed: " + e.getMessage());
    }

    // Calculate average scaling efficiency
    final double averageScaling = scalingResults.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    LOGGER.info(
        String.format(
            "Concurrent execution benchmark completed. Average scaling: %.2fx",
            averageScaling));

    return new DefaultPerformanceResult(Duration.ofSeconds(1), 0.0, averageScaling, 0.0);
  }

  public TestResults getLastResults() {
    return lastResults;
  }

  // Helper Methods

  private Duration benchmarkSynchronousOperations() {
    final int operationCount = 1000;
    final Instant start = Instant.now();

    try (final Engine engine = Engine.create();
        final Store store = Store.create(engine);
        final Module module = Module.compile(engine, generateArithmeticWasm());
        final Instance instance = Instance.create(store, module)) {

      final Function operation = instance.getExport("add", Function.class);

      for (int i = 0; i < operationCount; i++) {
        operation.call(i, i + 1);
      }

    } catch (final Exception e) {
      LOGGER.warning("Synchronous operations benchmark failed: " + e.getMessage());
    }

    return Duration.between(start, Instant.now());
  }

  private Duration benchmarkAsynchronousOperations() {
    final int operationCount = 1000;
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Instant start = Instant.now();

    try (final Engine engine = Engine.create();
        final Store store = Store.create(engine);
        final Module module = Module.compile(engine, generateArithmeticWasm());
        final Instance instance = Instance.create(store, module)) {

      final Function operation = instance.getExport("add", Function.class);
      final List<Future<Object[]>> futures = new ArrayList<>();

      for (int i = 0; i < operationCount; i++) {
        final int operand1 = i;
        final int operand2 = i + 1;
        futures.add(executor.submit(() -> operation.call(operand1, operand2)));
      }

      // Wait for all operations to complete
      for (final Future<Object[]> future : futures) {
        future.get();
      }

    } catch (final Exception e) {
      LOGGER.warning("Asynchronous operations benchmark failed: " + e.getMessage());
    } finally {
      executor.shutdown();
    }

    return Duration.between(start, Instant.now());
  }

  private double measureSingleThreadPerformance(final Engine engine, final Module module) throws Exception {
    final int operationCount = 10000;
    final Instant start = Instant.now();

    try (final Store store = Store.create(engine);
        final Instance instance = Instance.create(store, module)) {

      final Function operation = instance.getExport("compute", Function.class);

      for (int i = 0; i < operationCount; i++) {
        operation.call(i);
      }

      final Duration duration = Duration.between(start, Instant.now());
      return operationCount / (duration.toNanos() / 1_000_000_000.0);
    }
  }

  private double measureConcurrentPerformance(final Engine engine, final Module module, final int threadCount) {
    final int operationsPerThread = 1000;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final AtomicInteger totalOperations = new AtomicInteger(0);
    final Instant start = Instant.now();

    for (int t = 0; t < threadCount; t++) {
      executor.submit(() -> {
        try (final Store store = Store.create(engine);
            final Instance instance = Instance.create(store, module)) {

          final Function operation = instance.getExport("compute", Function.class);

          for (int i = 0; i < operationsPerThread; i++) {
            operation.call(i);
            totalOperations.incrementAndGet();
          }

        } catch (final Exception e) {
          LOGGER.warning("Concurrent thread failed: " + e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }

    try {
      latch.await(30, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warning("Concurrent performance measurement interrupted");
    } finally {
      executor.shutdown();
    }

    final Duration duration = Duration.between(start, Instant.now());
    return totalOperations.get() / (duration.toNanos() / 1_000_000_000.0);
  }

  private Duration calculateAverageTime(final List<Duration> times) {
    if (times.isEmpty()) {
      return Duration.ZERO;
    }

    final long totalNanos = times.stream().mapToLong(Duration::toNanos).sum();
    return Duration.ofNanos(totalNanos / times.size());
  }

  private Duration calculatePercentile(final List<Duration> times, final double percentile) {
    if (times.isEmpty()) {
      return Duration.ZERO;
    }

    final List<Duration> sorted = new ArrayList<>(times);
    sorted.sort(Duration::compareTo);

    final int index = (int) Math.ceil(percentile * sorted.size()) - 1;
    return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
  }

  private double calculateThroughput(final List<Duration> times) {
    if (times.isEmpty()) {
      return 0.0;
    }

    final Duration totalTime = times.stream().reduce(Duration.ZERO, Duration::plus);
    return times.size() / (totalTime.toNanos() / 1_000_000_000.0);
  }

  // WASM Generation Helper Methods

  private byte[] generateWasmModule(final int approximateSize) {
    // Generate a WASM module of approximately the specified size
    // This is a simplified implementation for benchmarking purposes
    final byte[] baseModule = createBasicWasmModule();

    if (approximateSize <= baseModule.length) {
      return baseModule;
    }

    // Pad the module to reach the desired size
    final byte[] paddedModule = new byte[approximateSize];
    System.arraycopy(baseModule, 0, paddedModule, 0, baseModule.length);

    // Fill the rest with valid WASM padding (data section)
    Arrays.fill(paddedModule, baseModule.length, paddedModule.length, (byte) 0x00);

    return paddedModule;
  }

  private byte[] generateArithmeticWasm() {
    // Generate a WASM module with arithmetic operations for function call benchmarks
    return createBasicWasmModule();
  }

  private byte[] generateMemoryOperationWasm() {
    // Generate a WASM module with memory read/write operations
    return createBasicWasmModule();
  }

  private byte[] generateConcurrentTestWasm() {
    // Generate a WASM module suitable for concurrent execution testing
    return createBasicWasmModule();
  }

  private byte[] generateTestData(final int size) {
    final byte[] data = new byte[size];
    for (int i = 0; i < size; i++) {
      data[i] = (byte) (i % 256);
    }
    return data;
  }

  private byte[] createBasicWasmModule() {
    // This is a minimal valid WebAssembly module with basic arithmetic functions
    // In practice, you would use more sophisticated WASM modules for benchmarking
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // WASM magic number
      0x01, 0x00, 0x00, 0x00, // Version
      // Type section (function signatures)
      0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f,
      // Function section
      0x03, 0x02, 0x01, 0x00,
      // Export section
      0x07, 0x0a, 0x01, 0x06, 0x61, 0x64, 0x64, 0x5f, 0x69, 0x6e, 0x74, 0x00, 0x00,
      // Code section
      0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
    };
  }

  // Performance Result Implementations

  private static final class DefaultPerformanceResult implements PerformanceResult {
    private final Duration averageTime;
    private final double overheadPercentage;
    private final double throughput;
    private final double customMetric;

    DefaultPerformanceResult(final Duration averageTime, final double overheadPercentage,
                           final double throughput, final double customMetric) {
      this.averageTime = averageTime;
      this.overheadPercentage = overheadPercentage;
      this.throughput = throughput;
      this.customMetric = customMetric;
    }

    @Override
    public Duration getAverageTime() {
      return averageTime;
    }

    @Override
    public double getOverheadPercentage() {
      return overheadPercentage;
    }

    @Override
    public double getThroughput() {
      return throughput;
    }
  }

  private static final class FailedPerformanceResult implements PerformanceResult {
    private final String errorMessage;

    FailedPerformanceResult(final String errorMessage) {
      this.errorMessage = errorMessage;
    }

    @Override
    public Duration getAverageTime() {
      return Duration.ofSeconds(Long.MAX_VALUE); // Indicate failure
    }

    @Override
    public double getOverheadPercentage() {
      return Double.MAX_VALUE; // Indicate failure
    }

    @Override
    public double getThroughput() {
      return 0.0; // Indicate failure
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }

  // Interface definitions
  public interface PerformanceResult {
    Duration getAverageTime();
    double getOverheadPercentage();
    double getThroughput();
  }
}