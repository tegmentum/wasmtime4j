package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Performance testing harness integrated with JMH framework concepts. Provides comprehensive
 * performance measurement utilities with baseline comparison, warmup handling, and statistical
 * analysis.
 */
public final class PerformanceTestHarness {
  private static final Logger LOGGER = Logger.getLogger(PerformanceTestHarness.class.getName());

  // Default configuration constants
  private static final int DEFAULT_WARMUP_ITERATIONS = 5;
  private static final int DEFAULT_MEASUREMENT_ITERATIONS = 10;
  private static final Duration DEFAULT_ITERATION_TIME = Duration.ofSeconds(1);
  private static final int DEFAULT_FORKS = 1;
  private static final int DEFAULT_THREADS = 1;

  private PerformanceTestHarness() {
    // Utility class - prevent instantiation
  }

  /** Performance test configuration. */
  public static final class Configuration {
    private final int warmupIterations;
    private final int measurementIterations;
    private final Duration iterationTime;
    private final int forks;
    private final int threads;
    private final boolean enableGC;
    private final Map<String, Object> parameters;

    private Configuration(final Builder builder) {
      this.warmupIterations = builder.warmupIterations;
      this.measurementIterations = builder.measurementIterations;
      this.iterationTime = builder.iterationTime;
      this.forks = builder.forks;
      this.threads = builder.threads;
      this.enableGC = builder.enableGC;
      this.parameters = new HashMap<>(builder.parameters);
    }

    public int getWarmupIterations() {
      return warmupIterations;
    }

    public int getMeasurementIterations() {
      return measurementIterations;
    }

    public Duration getIterationTime() {
      return iterationTime;
    }

    public int getForks() {
      return forks;
    }

    public int getThreads() {
      return threads;
    }

    public boolean isGCEnabled() {
      return enableGC;
    }

    public Map<String, Object> getParameters() {
      return new HashMap<>(parameters);
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Builder for configuring PerformanceTestHarness instances. */
    public static final class Builder {
      private int warmupIterations = DEFAULT_WARMUP_ITERATIONS;
      private int measurementIterations = DEFAULT_MEASUREMENT_ITERATIONS;
      private Duration iterationTime = DEFAULT_ITERATION_TIME;
      private int forks = DEFAULT_FORKS;
      private int threads = DEFAULT_THREADS;
      private boolean enableGC = true;
      private final Map<String, Object> parameters = new HashMap<>();

      public Builder warmupIterations(final int iterations) {
        this.warmupIterations = iterations;
        return this;
      }

      public Builder measurementIterations(final int iterations) {
        this.measurementIterations = iterations;
        return this;
      }

      public Builder iterationTime(final Duration duration) {
        this.iterationTime = duration;
        return this;
      }

      public Builder forks(final int forks) {
        this.forks = forks;
        return this;
      }

      public Builder threads(final int threads) {
        this.threads = threads;
        return this;
      }

      public Builder enableGC(final boolean enable) {
        this.enableGC = enable;
        return this;
      }

      public Builder parameter(final String key, final Object value) {
        this.parameters.put(key, value);
        return this;
      }

      public Configuration build() {
        return new Configuration(this);
      }
    }
  }

  /** Performance measurement result. */
  public static final class MeasurementResult {
    private final String testName;
    private final RuntimeType runtimeType;
    private final List<Double> measurements; // nanoseconds per operation
    private final double mean;
    private final double standardDeviation;
    private final double median;
    private final double min;
    private final double max;
    private final double q1;
    private final double q3;
    private final long totalOperations;
    private final Duration totalTime;

    private MeasurementResult(
        final String testName,
        final RuntimeType runtimeType,
        final List<Double> measurements,
        final long totalOperations,
        final Duration totalTime) {
      this.testName = testName;
      this.runtimeType = runtimeType;
      this.measurements = new ArrayList<>(measurements);
      this.totalOperations = totalOperations;
      this.totalTime = totalTime;

      // Calculate statistics
      final double[] sortedMeasurements =
          measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray();

      this.mean = Arrays.stream(sortedMeasurements).average().orElse(0.0);
      this.median = calculatePercentile(sortedMeasurements, 50.0);
      this.min = sortedMeasurements.length > 0 ? sortedMeasurements[0] : 0.0;
      this.max =
          sortedMeasurements.length > 0 ? sortedMeasurements[sortedMeasurements.length - 1] : 0.0;
      this.q1 = calculatePercentile(sortedMeasurements, 25.0);
      this.q3 = calculatePercentile(sortedMeasurements, 75.0);

      // Calculate standard deviation
      final double variance =
          measurements.stream().mapToDouble(m -> Math.pow(m - mean, 2)).average().orElse(0.0);
      this.standardDeviation = Math.sqrt(variance);
    }

    private static double calculatePercentile(final double[] sortedData, final double percentile) {
      if (sortedData.length == 0) {
        return 0.0;
      }

      final double index = percentile / 100.0 * (sortedData.length - 1);
      final int lowerIndex = (int) Math.floor(index);
      final int upperIndex = (int) Math.ceil(index);

      if (lowerIndex == upperIndex) {
        return sortedData[lowerIndex];
      }

      final double weight = index - lowerIndex;
      return sortedData[lowerIndex] * (1 - weight) + sortedData[upperIndex] * weight;
    }

    // Getters
    public String getTestName() {
      return testName;
    }

    public RuntimeType getRuntimeType() {
      return runtimeType;
    }

    public List<Double> getMeasurements() {
      return new ArrayList<>(measurements);
    }

    public double getMean() {
      return mean;
    }

    public double getStandardDeviation() {
      return standardDeviation;
    }

    public double getMedian() {
      return median;
    }

    public double getMin() {
      return min;
    }

    public double getMax() {
      return max;
    }

    public double getQ1() {
      return q1;
    }

    public double getQ3() {
      return q3;
    }

    public long getTotalOperations() {
      return totalOperations;
    }

    public Duration getTotalTime() {
      return totalTime;
    }

    /** Gets operations per second based on mean measurement. */
    public double getOperationsPerSecond() {
      return mean > 0 ? 1_000_000_000.0 / mean : 0.0;
    }

    /** Gets coefficient of variation (relative standard deviation). */
    public double getCoefficientOfVariation() {
      return mean > 0 ? (standardDeviation / mean) * 100.0 : 0.0;
    }
  }

  /** Benchmark comparison result. */
  public static final class ComparisonResult {
    private final MeasurementResult baseline;
    private final MeasurementResult comparison;
    private final double speedupRatio;
    private final double pvalue;
    private final boolean statisticallySignificant;

    private ComparisonResult(final MeasurementResult baseline, final MeasurementResult comparison) {
      this.baseline = baseline;
      this.comparison = comparison;
      this.speedupRatio = baseline.getMean() / comparison.getMean();
      this.pvalue = calculateWelchTTest(baseline, comparison);
      this.statisticallySignificant = pvalue < 0.05; // 95% confidence
    }

    /** Performs Welch's t-test to determine statistical significance. */
    private static double calculateWelchTTest(
        final MeasurementResult result1, final MeasurementResult result2) {
      final double mean1 = result1.getMean();
      final double mean2 = result2.getMean();
      final double var1 = Math.pow(result1.getStandardDeviation(), 2);
      final double var2 = Math.pow(result2.getStandardDeviation(), 2);
      final int n1 = result1.getMeasurements().size();
      final int n2 = result2.getMeasurements().size();

      if (var1 == 0 && var2 == 0) {
        return mean1 == mean2 ? 1.0 : 0.0;
      }

      final double pooledSE = Math.sqrt(var1 / n1 + var2 / n2);
      if (pooledSE == 0) {
        return 1.0;
      }

      final double t = Math.abs(mean1 - mean2) / pooledSE;

      // Approximate p-value calculation (simplified)
      // In practice, would use more sophisticated statistical libraries
      return Math.max(0.001, Math.min(1.0, 2 * Math.exp(-0.717 * t - 0.416 * t * t)));
    }

    // Getters
    public MeasurementResult getBaseline() {
      return baseline;
    }

    public MeasurementResult getComparison() {
      return comparison;
    }

    public double getSpeedupRatio() {
      return speedupRatio;
    }

    public double getPValue() {
      return pvalue;
    }

    public boolean isStatisticallySignificant() {
      return statisticallySignificant;
    }

    /** Determines if the comparison shows improvement. */
    public boolean isImprovement() {
      return speedupRatio > 1.0 && statisticallySignificant;
    }

    /** Determines if the comparison shows regression. */
    public boolean isRegression() {
      return speedupRatio < 1.0 && statisticallySignificant;
    }
  }

  /** Functional interface for benchmark operations. */
  @FunctionalInterface
  public interface BenchmarkOperation {
    void execute() throws Exception;
  }

  /** Functional interface for runtime-specific benchmark operations. */
  @FunctionalInterface
  public interface RuntimeBenchmarkOperation {
    void execute(WasmRuntime runtime) throws Exception;
  }

  /**
   * Runs a performance benchmark with the given configuration.
   *
   * @param testName name of the test
   * @param operation the operation to benchmark
   * @param config benchmark configuration
   * @return measurement result
   */
  public static MeasurementResult runBenchmark(
      final String testName, final BenchmarkOperation operation, final Configuration config) {
    LOGGER.info("Running benchmark: " + testName);

    final List<Double> measurements = new ArrayList<>();
    long totalOperations = 0;
    final Instant overallStart = Instant.now();

    // Warmup phase
    LOGGER.info("Warmup phase: " + config.getWarmupIterations() + " iterations");
    for (int i = 0; i < config.getWarmupIterations(); i++) {
      runSingleIteration(operation, config.getIterationTime(), true);
      if (config.isGCEnabled()) {
        System.gc();
        try {
          Thread.sleep(100); // Allow GC to complete
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    // Measurement phase
    LOGGER.info("Measurement phase: " + config.getMeasurementIterations() + " iterations");
    for (int i = 0; i < config.getMeasurementIterations(); i++) {
      final IterationResult result =
          runSingleIteration(operation, config.getIterationTime(), false);
      measurements.add(result.nanosPerOperation);
      totalOperations += result.operationCount;

      if (config.isGCEnabled()) {
        System.gc();
        try {
          Thread.sleep(50);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    final Duration totalTime = Duration.between(overallStart, Instant.now());
    return new MeasurementResult(testName, null, measurements, totalOperations, totalTime);
  }

  /**
   * Runs a cross-runtime performance comparison.
   *
   * @param testName name of the test
   * @param operation runtime-specific operation
   * @param config benchmark configuration
   * @return comparison result
   */
  public static ComparisonResult runCrossRuntimeBenchmark(
      final String testName,
      final RuntimeBenchmarkOperation operation,
      final Configuration config) {
    LOGGER.info("Running cross-runtime benchmark: " + testName);

    // Benchmark JNI runtime
    final MeasurementResult jniResult =
        runRuntimeBenchmark(testName + "[JNI]", RuntimeType.JNI, operation, config);

    // Benchmark Panama runtime if available
    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama not available, using JNI result for both measurements");
      return new ComparisonResult(jniResult, jniResult);
    }

    final MeasurementResult panamaResult =
        runRuntimeBenchmark(testName + "[Panama]", RuntimeType.PANAMA, operation, config);

    return new ComparisonResult(jniResult, panamaResult);
  }

  /** Runs a benchmark for a specific runtime. */
  private static MeasurementResult runRuntimeBenchmark(
      final String testName,
      final RuntimeType runtimeType,
      final RuntimeBenchmarkOperation operation,
      final Configuration config) {
    LOGGER.info("Benchmarking " + runtimeType + " runtime");

    // Create wrapper that manages runtime lifecycle
    final BenchmarkOperation wrappedOperation =
        () -> {
          try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
            operation.execute(runtime);
          }
        };

    final MeasurementResult result = runBenchmark(testName, wrappedOperation, config);

    // Create new result with runtime type information
    return new MeasurementResult(
        result.getTestName(),
        runtimeType,
        result.getMeasurements(),
        result.getTotalOperations(),
        result.getTotalTime());
  }

  /**
   * Runs multiple benchmarks in parallel for throughput testing.
   *
   * @param testName base name for tests
   * @param operation the operation to benchmark
   * @param config benchmark configuration
   * @param threadCounts array of thread counts to test
   * @return map of thread count to measurement results
   */
  public static Map<Integer, MeasurementResult> runThroughputBenchmark(
      final String testName,
      final BenchmarkOperation operation,
      final Configuration config,
      final int[] threadCounts) {

    final Map<Integer, MeasurementResult> results = new HashMap<>();

    for (final int threadCount : threadCounts) {
      LOGGER.info("Running throughput test with " + threadCount + " threads");

      final Configuration threadConfig =
          Configuration.builder()
              .warmupIterations(config.getWarmupIterations())
              .measurementIterations(config.getMeasurementIterations())
              .iterationTime(config.getIterationTime())
              .threads(threadCount)
              .enableGC(config.isGCEnabled())
              .build();

      final MeasurementResult result =
          runConcurrentBenchmark(testName + "[" + threadCount + "T]", operation, threadConfig);

      results.put(threadCount, result);
    }

    return results;
  }

  /** Runs a concurrent benchmark with multiple threads. */
  private static MeasurementResult runConcurrentBenchmark(
      final String testName, final BenchmarkOperation operation, final Configuration config) {
    final ExecutorService executor = Executors.newFixedThreadPool(config.getThreads());

    try {
      final List<Double> allMeasurements = new ArrayList<>();
      long totalOperations = 0;
      final Instant overallStart = Instant.now();

      // Warmup phase
      for (int iteration = 0; iteration < config.getWarmupIterations(); iteration++) {
        runConcurrentIteration(executor, operation, config, true);
      }

      // Measurement phase
      for (int iteration = 0; iteration < config.getMeasurementIterations(); iteration++) {
        final List<IterationResult> results =
            runConcurrentIteration(executor, operation, config, false);

        for (final IterationResult result : results) {
          allMeasurements.add(result.nanosPerOperation);
          totalOperations += result.operationCount;
        }
      }

      final Duration totalTime = Duration.between(overallStart, Instant.now());
      return new MeasurementResult(testName, null, allMeasurements, totalOperations, totalTime);

    } finally {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executor.shutdownNow();
      }
    }
  }

  /** Runs a single concurrent iteration. */
  private static List<IterationResult> runConcurrentIteration(
      final ExecutorService executor,
      final BenchmarkOperation operation,
      final Configuration config,
      final boolean isWarmup) {
    final List<CompletableFuture<IterationResult>> futures =
        IntStream.range(0, config.getThreads())
            .mapToObj(
                i ->
                    CompletableFuture.supplyAsync(
                        () -> runSingleIteration(operation, config.getIterationTime(), isWarmup),
                        executor))
            .collect(Collectors.toList());

    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

  /** Result of a single benchmark iteration. */
  private static final class IterationResult {
    final long operationCount;
    final double nanosPerOperation;

    IterationResult(final long operationCount, final double nanosPerOperation) {
      this.operationCount = operationCount;
      this.nanosPerOperation = nanosPerOperation;
    }
  }

  /** Runs a single benchmark iteration. */
  private static IterationResult runSingleIteration(
      final BenchmarkOperation operation, final Duration iterationTime, final boolean isWarmup) {
    final long iterationNanos = iterationTime.toNanos();
    final Instant start = Instant.now();
    long operationCount = 0;

    final long startNanos = System.nanoTime();
    final long endNanos = startNanos + iterationNanos;

    try {
      while (System.nanoTime() < endNanos) {
        operation.execute();
        operationCount++;
      }
    } catch (final Exception e) {
      if (!isWarmup) {
        LOGGER.severe("Benchmark operation failed: " + e.getMessage());
      }
      throw new RuntimeException("Benchmark operation failed", e);
    }

    final long actualNanos = System.nanoTime() - startNanos;
    final double nanosPerOperation =
        operationCount > 0 ? (double) actualNanos / operationCount : 0.0;

    if (!isWarmup) {
      LOGGER.fine(
          String.format(
              "Iteration: %d ops in %d ns (%.2f ns/op)",
              operationCount, actualNanos, nanosPerOperation));
    }

    return new IterationResult(operationCount, nanosPerOperation);
  }

  /**
   * Creates a performance report from benchmark results.
   *
   * @param results list of measurement results to include in report
   * @return formatted performance report
   */
  public static String generateReport(final List<MeasurementResult> results) {
    final StringBuilder report = new StringBuilder();
    report.append("Performance Test Report\n");
    report.append("======================\n\n");

    for (final MeasurementResult result : results) {
      report.append("Test: ").append(result.getTestName()).append("\n");
      if (result.getRuntimeType() != null) {
        report.append("Runtime: ").append(result.getRuntimeType()).append("\n");
      }
      report.append(
          String.format(
              "Mean: %.2f ns/op (%.2f ops/sec)\n",
              result.getMean(), result.getOperationsPerSecond()));
      report.append(String.format("Median: %.2f ns/op\n", result.getMedian()));
      report.append(
          String.format("Min: %.2f ns/op, Max: %.2f ns/op\n", result.getMin(), result.getMax()));
      report.append(
          String.format(
              "Std Dev: %.2f ns/op (CV: %.2f%%)\n",
              result.getStandardDeviation(), result.getCoefficientOfVariation()));
      report.append(
          String.format(
              "Total Operations: %d, Total Time: %d ms\n",
              result.getTotalOperations(), result.getTotalTime().toMillis()));
      report.append("\n");
    }

    return report.toString();
  }

  /**
   * Creates a comparison report between two benchmark results.
   *
   * @param comparison comparison result
   * @return formatted comparison report
   */
  public static String generateComparisonReport(final ComparisonResult comparison) {
    final StringBuilder report = new StringBuilder();
    report.append("Performance Comparison Report\n");
    report.append("============================\n\n");

    final MeasurementResult baseline = comparison.getBaseline();
    final MeasurementResult comp = comparison.getComparison();

    report.append("Baseline: ").append(baseline.getTestName()).append("\n");
    report.append(
        String.format(
            "  Mean: %.2f ns/op (%.2f ops/sec)\n",
            baseline.getMean(), baseline.getOperationsPerSecond()));

    report.append("Comparison: ").append(comp.getTestName()).append("\n");
    report.append(
        String.format(
            "  Mean: %.2f ns/op (%.2f ops/sec)\n", comp.getMean(), comp.getOperationsPerSecond()));

    report.append("\nComparison Results:\n");
    report.append(String.format("  Speedup Ratio: %.2fx\n", comparison.getSpeedupRatio()));
    report.append(String.format("  P-Value: %.4f\n", comparison.getPValue()));
    report
        .append("  Statistically Significant: ")
        .append(comparison.isStatisticallySignificant())
        .append("\n");

    if (comparison.isImprovement()) {
      report.append("  Result: IMPROVEMENT\n");
    } else if (comparison.isRegression()) {
      report.append("  Result: REGRESSION\n");
    } else {
      report.append("  Result: NO SIGNIFICANT CHANGE\n");
    }

    return report.toString();
  }

  /**
   * Gets the default benchmark configuration.
   *
   * @return default configuration
   */
  public static Configuration getDefaultConfiguration() {
    return Configuration.builder().build();
  }

  /**
   * Gets a fast benchmark configuration for quick testing.
   *
   * @return fast configuration with fewer iterations
   */
  public static Configuration getFastConfiguration() {
    return Configuration.builder()
        .warmupIterations(2)
        .measurementIterations(3)
        .iterationTime(Duration.ofMillis(500))
        .build();
  }

  /**
   * Gets a thorough benchmark configuration for comprehensive testing.
   *
   * @return thorough configuration with many iterations
   */
  public static Configuration getThoroughConfiguration() {
    return Configuration.builder()
        .warmupIterations(10)
        .measurementIterations(20)
        .iterationTime(Duration.ofSeconds(2))
        .build();
  }
}
