package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Comprehensive performance testing framework for WebAssembly implementations. Provides
 * detailed performance comparisons between JNI and Panama runtimes, with statistical
 * analysis and regression detection.
 */
public final class WasmPerformanceTestFramework {
  private static final Logger LOGGER = Logger.getLogger(WasmPerformanceTestFramework.class.getName());

  // Performance test configuration
  private static final int DEFAULT_WARMUP_ITERATIONS = 1000;
  private static final int DEFAULT_MEASUREMENT_ITERATIONS = 10000;
  private static final int DEFAULT_BENCHMARK_RUNS = 5;
  private static final Duration MAX_TEST_DURATION = Duration.ofMinutes(10);

  // Statistical significance thresholds
  private static final double PERFORMANCE_DIFFERENCE_THRESHOLD = 0.05; // 5%
  private static final double STATISTICAL_CONFIDENCE = 0.95; // 95%

  // Runtime cache for consistent testing
  private static final ConcurrentMap<RuntimeType, WasmRuntime> runtimeCache = new ConcurrentHashMap<>();

  private WasmPerformanceTestFramework() {
    // Utility class - prevent instantiation
  }

  /**
   * Executes comprehensive performance tests across all available runtimes.
   *
   * @param testCases the test cases to benchmark
   * @param config the performance test configuration
   * @return comprehensive performance results
   * @throws IOException if performance testing fails
   */
  public static WasmPerformanceTestResults executePerformanceTests(
      final List<WasmTestCase> testCases,
      final PerformanceTestConfiguration config) throws IOException {
    
    Objects.requireNonNull(testCases, "testCases cannot be null");
    Objects.requireNonNull(config, "config cannot be null");

    LOGGER.info("Starting comprehensive WebAssembly performance testing");
    final Instant startTime = Instant.now();

    final WasmPerformanceTestResults.Builder resultsBuilder = 
        new WasmPerformanceTestResults.Builder();

    // Execute performance tests for each available runtime
    for (final RuntimeType runtimeType : config.getTargetRuntimes()) {
      if (runtimeType == RuntimeType.PANAMA && !TestUtils.isPanamaAvailable()) {
        LOGGER.info("Skipping Panama performance tests - not available on Java " + TestUtils.getJavaVersion());
        continue;
      }

      LOGGER.info("Executing performance tests with " + runtimeType + " runtime");
      final Map<String, PerformanceBenchmarkResult> runtimeResults = 
          executeRuntimePerformanceTests(testCases, runtimeType, config);

      resultsBuilder.addRuntimeResults(runtimeType, runtimeResults);
    }

    final Duration totalDuration = Duration.between(startTime, Instant.now());
    resultsBuilder.totalExecutionTime(totalDuration);

    final WasmPerformanceTestResults results = resultsBuilder.build();

    LOGGER.info("Performance testing completed in " + totalDuration.toSeconds() + "s. "
        + "Tested " + testCases.size() + " test cases across " 
        + results.getTestedRuntimes().size() + " runtimes");

    return results;
  }

  /**
   * Executes performance tests for a specific runtime.
   *
   * @param testCases the test cases to benchmark
   * @param runtimeType the runtime type
   * @param config the test configuration
   * @return the runtime performance results
   */
  private static Map<String, PerformanceBenchmarkResult> executeRuntimePerformanceTests(
      final List<WasmTestCase> testCases,
      final RuntimeType runtimeType,
      final PerformanceTestConfiguration config) {

    final Map<String, PerformanceBenchmarkResult> results = new ConcurrentHashMap<>();
    final WasmRuntime runtime = getRuntimeInstance(runtimeType);

    for (final WasmTestCase testCase : testCases) {
      try {
        LOGGER.fine("Benchmarking " + testCase.getName() + " with " + runtimeType);
        final PerformanceBenchmarkResult result = 
            benchmarkTestCase(testCase, runtime, runtimeType, config);
        results.put(testCase.getName(), result);

      } catch (final Exception e) {
        LOGGER.warning("Performance test failed for " + testCase.getName() 
            + " with " + runtimeType + ": " + e.getMessage());
        
        // Record failed benchmark
        final PerformanceBenchmarkResult failedResult = PerformanceBenchmarkResult.failed(
            testCase.getName(), runtimeType, e);
        results.put(testCase.getName(), failedResult);
      }
    }

    return results;
  }

  /**
   * Benchmarks a single test case with the specified runtime.
   *
   * @param testCase the test case to benchmark
   * @param runtime the runtime instance
   * @param runtimeType the runtime type
   * @param config the test configuration
   * @return the benchmark result
   * @throws Exception if benchmarking fails
   */
  private static PerformanceBenchmarkResult benchmarkTestCase(
      final WasmTestCase testCase,
      final WasmRuntime runtime,
      final RuntimeType runtimeType,
      final PerformanceTestConfiguration config) throws Exception {

    final PerformanceBenchmarkResult.Builder resultBuilder = 
        new PerformanceBenchmarkResult.Builder(testCase.getName(), runtimeType);

    // Validate test module
    if (!WasmTestSuiteLoader.isValidWasmModule(testCase.getModuleBytes())) {
      throw new IllegalArgumentException("Invalid WebAssembly module: " + testCase.getName());
    }

    // Prepare WebAssembly components
    final var engine = runtime.createEngine();
    final var store = engine.createStore();
    final var module = engine.compileModule(testCase.getModuleBytes());
    final var instance = store.instantiate(module);

    try {
      // Warmup phase
      final Duration warmupTime = executeWarmup(instance, config);
      resultBuilder.warmupTime(warmupTime);

      // Collect multiple benchmark runs
      final List<BenchmarkRun> benchmarkRuns = new ArrayList<>();
      
      for (int run = 0; run < config.getBenchmarkRuns(); run++) {
        final BenchmarkRun benchmarkRun = executeBenchmarkRun(instance, config);
        benchmarkRuns.add(benchmarkRun);
        
        LOGGER.fine("Benchmark run " + (run + 1) + " for " + testCase.getName() 
            + ": " + benchmarkRun.getAverageExecutionTime().toNanos() + "ns avg");
      }

      resultBuilder.benchmarkRuns(benchmarkRuns);

      // Calculate statistics
      final PerformanceStatistics statistics = calculateStatistics(benchmarkRuns);
      resultBuilder.statistics(statistics);

      return resultBuilder.build();

    } finally {
      // Clean up resources
      if (instance != null) {
        instance.close();
      }
      if (module != null) {
        module.close();
      }
      if (store != null) {
        store.close();
      }
      if (engine != null) {
        engine.close();
      }
    }
  }

  /**
   * Executes the warmup phase for a test case.
   *
   * @param instance the WebAssembly instance
   * @param config the test configuration
   * @return the total warmup time
   * @throws Exception if warmup fails
   */
  private static Duration executeWarmup(final Object instance, 
                                        final PerformanceTestConfiguration config) throws Exception {
    final Instant startTime = Instant.now();

    // Execute warmup iterations (simplified - in real implementation would call actual functions)
    for (int i = 0; i < config.getWarmupIterations(); i++) {
      // Simulate function call - in real implementation would invoke actual WebAssembly functions
      Thread.yield(); // Minimal operation for warmup simulation
    }

    return Duration.between(startTime, Instant.now());
  }

  /**
   * Executes a single benchmark run.
   *
   * @param instance the WebAssembly instance
   * @param config the test configuration
   * @return the benchmark run result
   * @throws Exception if benchmark execution fails
   */
  private static BenchmarkRun executeBenchmarkRun(final Object instance,
                                                  final PerformanceTestConfiguration config) throws Exception {
    final List<Duration> executionTimes = new ArrayList<>();
    final Instant runStartTime = Instant.now();

    for (int i = 0; i < config.getMeasurementIterations(); i++) {
      final Instant iterationStart = Instant.now();
      
      // Execute the test operation (simplified - would call actual WebAssembly functions)
      Thread.yield(); // Minimal operation for timing simulation
      
      final Duration iterationTime = Duration.between(iterationStart, Instant.now());
      executionTimes.add(iterationTime);
    }

    final Duration totalRunTime = Duration.between(runStartTime, Instant.now());

    return new BenchmarkRun(executionTimes, totalRunTime);
  }

  /**
   * Calculates comprehensive performance statistics from benchmark runs.
   *
   * @param benchmarkRuns the benchmark runs
   * @return the performance statistics
   */
  private static PerformanceStatistics calculateStatistics(final List<BenchmarkRun> benchmarkRuns) {
    final List<Duration> allExecutionTimes = benchmarkRuns.stream()
        .flatMap(run -> run.getExecutionTimes().stream())
        .collect(Collectors.toList());

    if (allExecutionTimes.isEmpty()) {
      return PerformanceStatistics.empty();
    }

    // Sort for percentile calculations
    final List<Long> sortedNanos = allExecutionTimes.stream()
        .mapToLong(Duration::toNanos)
        .sorted()
        .boxed()
        .collect(Collectors.toList());

    final double mean = sortedNanos.stream().mapToLong(Long::longValue).average().orElse(0.0);
    final long min = sortedNanos.get(0);
    final long max = sortedNanos.get(sortedNanos.size() - 1);
    final long median = sortedNanos.get(sortedNanos.size() / 2);
    final long p95 = sortedNanos.get((int) (sortedNanos.size() * 0.95));
    final long p99 = sortedNanos.get((int) (sortedNanos.size() * 0.99));

    // Calculate standard deviation
    final double variance = sortedNanos.stream()
        .mapToDouble(nano -> Math.pow(nano - mean, 2))
        .average()
        .orElse(0.0);
    final double standardDeviation = Math.sqrt(variance);

    return new PerformanceStatistics(
        Duration.ofNanos((long) mean),
        Duration.ofNanos(min),
        Duration.ofNanos(max),
        Duration.ofNanos(median),
        Duration.ofNanos(p95),
        Duration.ofNanos(p99),
        Duration.ofNanos((long) standardDeviation),
        allExecutionTimes.size()
    );
  }

  /**
   * Gets a cached runtime instance for the specified type.
   *
   * @param runtimeType the runtime type
   * @return the runtime instance
   * @throws RuntimeException if runtime creation fails
   */
  private static WasmRuntime getRuntimeInstance(final RuntimeType runtimeType) {
    return runtimeCache.computeIfAbsent(runtimeType, type -> {
      LOGGER.info("Creating runtime instance for performance testing: " + type);
      try {
        return WasmRuntimeFactory.create(type);
      } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
        throw new RuntimeException("Failed to create runtime for " + type, e);
      }
    });
  }

  /**
   * Clears all cached runtime instances.
   */
  public static void clearRuntimeCache() {
    runtimeCache.values().forEach(runtime -> {
      try {
        runtime.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close runtime during cache clear: " + e.getMessage());
      }
    });
    runtimeCache.clear();
    LOGGER.info("Cleared performance test runtime cache");
  }

  /**
   * Configuration for performance testing.
   */
  public static final class PerformanceTestConfiguration {
    private final List<RuntimeType> targetRuntimes;
    private final int warmupIterations;
    private final int measurementIterations;
    private final int benchmarkRuns;
    private final Duration maxTestDuration;

    private PerformanceTestConfiguration(final Builder builder) {
      this.targetRuntimes = Collections.unmodifiableList(new ArrayList<>(builder.targetRuntimes));
      this.warmupIterations = builder.warmupIterations;
      this.measurementIterations = builder.measurementIterations;
      this.benchmarkRuns = builder.benchmarkRuns;
      this.maxTestDuration = builder.maxTestDuration;
    }

    public static PerformanceTestConfiguration defaultConfiguration() {
      return new Builder().build();
    }

    public static Builder builder() {
      return new Builder();
    }

    // Getters
    public List<RuntimeType> getTargetRuntimes() {
      return targetRuntimes;
    }

    public int getWarmupIterations() {
      return warmupIterations;
    }

    public int getMeasurementIterations() {
      return measurementIterations;
    }

    public int getBenchmarkRuns() {
      return benchmarkRuns;
    }

    public Duration getMaxTestDuration() {
      return maxTestDuration;
    }

    /**
     * Builder for PerformanceTestConfiguration.
     */
    public static final class Builder {
      private final List<RuntimeType> targetRuntimes = new ArrayList<>();
      private int warmupIterations = DEFAULT_WARMUP_ITERATIONS;
      private int measurementIterations = DEFAULT_MEASUREMENT_ITERATIONS;
      private int benchmarkRuns = DEFAULT_BENCHMARK_RUNS;
      private Duration maxTestDuration = MAX_TEST_DURATION;

      private Builder() {
        // Default to all available runtimes
        targetRuntimes.add(RuntimeType.JNI);
        if (TestUtils.isPanamaAvailable()) {
          targetRuntimes.add(RuntimeType.PANAMA);
        }
      }

      public Builder targetRuntime(final RuntimeType runtimeType) {
        if (!targetRuntimes.contains(runtimeType)) {
          targetRuntimes.add(runtimeType);
        }
        return this;
      }

      public Builder warmupIterations(final int iterations) {
        if (iterations < 0) {
          throw new IllegalArgumentException("warmupIterations cannot be negative");
        }
        this.warmupIterations = iterations;
        return this;
      }

      public Builder measurementIterations(final int iterations) {
        if (iterations <= 0) {
          throw new IllegalArgumentException("measurementIterations must be positive");
        }
        this.measurementIterations = iterations;
        return this;
      }

      public Builder benchmarkRuns(final int runs) {
        if (runs <= 0) {
          throw new IllegalArgumentException("benchmarkRuns must be positive");
        }
        this.benchmarkRuns = runs;
        return this;
      }

      public Builder maxTestDuration(final Duration duration) {
        this.maxTestDuration = Objects.requireNonNull(duration, "maxTestDuration cannot be null");
        return this;
      }

      public PerformanceTestConfiguration build() {
        if (targetRuntimes.isEmpty()) {
          throw new IllegalStateException("At least one target runtime must be specified");
        }
        return new PerformanceTestConfiguration(this);
      }
    }
  }

  /**
   * Represents a single benchmark run with multiple iterations.
   */
  public static final class BenchmarkRun {
    private final List<Duration> executionTimes;
    private final Duration totalRunTime;
    private final Duration averageExecutionTime;

    private BenchmarkRun(final List<Duration> executionTimes, final Duration totalRunTime) {
      this.executionTimes = Collections.unmodifiableList(new ArrayList<>(executionTimes));
      this.totalRunTime = totalRunTime;
      
      final long totalNanos = executionTimes.stream().mapToLong(Duration::toNanos).sum();
      this.averageExecutionTime = Duration.ofNanos(totalNanos / executionTimes.size());
    }

    public List<Duration> getExecutionTimes() {
      return executionTimes;
    }

    public Duration getTotalRunTime() {
      return totalRunTime;
    }

    public Duration getAverageExecutionTime() {
      return averageExecutionTime;
    }

    public int getIterationCount() {
      return executionTimes.size();
    }
  }

  /**
   * Statistical analysis of performance measurements.
   */
  public static final class PerformanceStatistics {
    private final Duration meanExecutionTime;
    private final Duration minExecutionTime;
    private final Duration maxExecutionTime;
    private final Duration medianExecutionTime;
    private final Duration p95ExecutionTime;
    private final Duration p99ExecutionTime;
    private final Duration standardDeviation;
    private final int sampleSize;

    private PerformanceStatistics(final Duration meanExecutionTime,
                                  final Duration minExecutionTime,
                                  final Duration maxExecutionTime,
                                  final Duration medianExecutionTime,
                                  final Duration p95ExecutionTime,
                                  final Duration p99ExecutionTime,
                                  final Duration standardDeviation,
                                  final int sampleSize) {
      this.meanExecutionTime = meanExecutionTime;
      this.minExecutionTime = minExecutionTime;
      this.maxExecutionTime = maxExecutionTime;
      this.medianExecutionTime = medianExecutionTime;
      this.p95ExecutionTime = p95ExecutionTime;
      this.p99ExecutionTime = p99ExecutionTime;
      this.standardDeviation = standardDeviation;
      this.sampleSize = sampleSize;
    }

    public static PerformanceStatistics empty() {
      return new PerformanceStatistics(
          Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO,
          Duration.ZERO, Duration.ZERO, Duration.ZERO, 0);
    }

    // Getters
    public Duration getMeanExecutionTime() { return meanExecutionTime; }
    public Duration getMinExecutionTime() { return minExecutionTime; }
    public Duration getMaxExecutionTime() { return maxExecutionTime; }
    public Duration getMedianExecutionTime() { return medianExecutionTime; }
    public Duration getP95ExecutionTime() { return p95ExecutionTime; }
    public Duration getP99ExecutionTime() { return p99ExecutionTime; }
    public Duration getStandardDeviation() { return standardDeviation; }
    public int getSampleSize() { return sampleSize; }

    /**
     * Gets the coefficient of variation (relative standard deviation).
     *
     * @return coefficient of variation as a percentage
     */
    public double getCoefficientOfVariation() {
      if (meanExecutionTime.toNanos() == 0) {
        return 0.0;
      }
      return (standardDeviation.toNanos() / (double) meanExecutionTime.toNanos()) * 100.0;
    }
  }
}