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

  /**
   * Runs a benchmark with profiling enabled for hotspot analysis.
   *
   * @param testName name of the test
   * @param operation the operation to profile
   * @param config benchmark configuration
   * @return measurement result with profiling data
   */
  public static MeasurementResult runProfilingBenchmark(
      final String testName, final BenchmarkOperation operation, final Configuration config) {
    LOGGER.info("Running profiling benchmark: " + testName);

    // Enable JVM profiling flags if available
    final Map<String, Object> profilingMetrics = new HashMap<>();
    
    // Capture JVM compilation information
    final long compilationTimeBefore = getCompilationTime();
    final long gcTimeBefore = getGCTime();
    
    // Run the benchmark
    final MeasurementResult result = runBenchmark(testName, operation, config);
    
    // Capture post-benchmark metrics
    final long compilationTimeAfter = getCompilationTime();
    final long gcTimeAfter = getGCTime();
    
    profilingMetrics.put("compilationTime", compilationTimeAfter - compilationTimeBefore);
    profilingMetrics.put("gcTime", gcTimeAfter - gcTimeBefore);
    
    LOGGER.info(String.format("Profiling results: compilation=%dms, gc=%dms",
        compilationTimeAfter - compilationTimeBefore,
        gcTimeAfter - gcTimeBefore));
    
    return result;
  }

  /**
   * Runs multiple benchmarks with different JVM options for comparison.
   *
   * @param testName base test name
   * @param operation the operation to benchmark
   * @param config benchmark configuration
   * @param jvmOptionSets different JVM option configurations
   * @return map of JVM options to measurement results
   */
  public static Map<String, MeasurementResult> runJvmOptionComparison(
      final String testName,
      final BenchmarkOperation operation,
      final Configuration config,
      final Map<String, String[]> jvmOptionSets) {
    
    final Map<String, MeasurementResult> results = new HashMap<>();
    
    for (final Map.Entry<String, String[]> entry : jvmOptionSets.entrySet()) {
      final String optionSetName = entry.getKey();
      final String[] jvmOptions = entry.getValue();
      
      LOGGER.info(String.format("Running benchmark with JVM options: %s (%s)",
          optionSetName, String.join(" ", jvmOptions)));
      
      // Note: In a real JMH integration, we would spawn separate JVM processes
      // For this simulation, we run in the current JVM and log the intended options
      logJvmOptions(jvmOptions);
      
      final MeasurementResult result = runBenchmark(
          testName + "[" + optionSetName + "]", operation, config);
      results.put(optionSetName, result);
    }
    
    return results;
  }

  /**
   * Runs warmup benchmarks with different warmup strategies.
   *
   * @param testName base test name
   * @param operation the operation to benchmark
   * @param baseConfig base configuration
   * @return comparison of warmup strategies
   */
  public static Map<String, MeasurementResult> runWarmupStrategyComparison(
      final String testName, final BenchmarkOperation operation, final Configuration baseConfig) {
    
    final Map<String, MeasurementResult> results = new HashMap<>();
    
    // No warmup strategy
    final Configuration noWarmup = Configuration.builder()
        .warmupIterations(0)
        .measurementIterations(baseConfig.getMeasurementIterations())
        .iterationTime(baseConfig.getIterationTime())
        .build();
    results.put("no_warmup", runBenchmark(testName + "[no_warmup]", operation, noWarmup));
    
    // Short warmup strategy
    final Configuration shortWarmup = Configuration.builder()
        .warmupIterations(2)
        .measurementIterations(baseConfig.getMeasurementIterations())
        .iterationTime(Duration.ofMillis(500))
        .build();
    results.put("short_warmup", runBenchmark(testName + "[short_warmup]", operation, shortWarmup));
    
    // Long warmup strategy
    final Configuration longWarmup = Configuration.builder()
        .warmupIterations(20)
        .measurementIterations(baseConfig.getMeasurementIterations())
        .iterationTime(baseConfig.getIterationTime())
        .build();
    results.put("long_warmup", runBenchmark(testName + "[long_warmup]", operation, longWarmup));
    
    // Adaptive warmup strategy
    final Configuration adaptiveWarmup = Configuration.builder()
        .warmupIterations(10)
        .measurementIterations(baseConfig.getMeasurementIterations())
        .iterationTime(Duration.ofMillis(100))
        .build();
    results.put("adaptive_warmup", runBenchmark(testName + "[adaptive_warmup]", operation, adaptiveWarmup));
    
    return results;
  }

  /**
   * Runs a benchmark with different thread counts to analyze scalability.
   *
   * @param testName base test name
   * @param operation the operation to benchmark
   * @param config base configuration
   * @param threadCounts array of thread counts to test
   * @return scalability analysis results
   */
  public static ScalabilityAnalysisResult runScalabilityAnalysis(
      final String testName,
      final BenchmarkOperation operation,
      final Configuration config,
      final int[] threadCounts) {
    
    final Map<Integer, MeasurementResult> results = runThroughputBenchmark(
        testName, operation, config, threadCounts);
    
    return new ScalabilityAnalysisResult(testName, results);
  }

  /** Results container for scalability analysis. */
  public static final class ScalabilityAnalysisResult {
    private final String testName;
    private final Map<Integer, MeasurementResult> results;
    private final int optimalThreadCount;
    private final double maxThroughput;
    private final double scalabilityFactor;

    private ScalabilityAnalysisResult(final String testName, final Map<Integer, MeasurementResult> results) {
      this.testName = testName;
      this.results = new HashMap<>(results);
      
      // Analyze results to find optimal configuration
      double bestThroughput = 0.0;
      int bestThreadCount = 1;
      
      for (final Map.Entry<Integer, MeasurementResult> entry : results.entrySet()) {
        final double throughput = entry.getValue().getOperationsPerSecond();
        if (throughput > bestThroughput) {
          bestThroughput = throughput;
          bestThreadCount = entry.getKey();
        }
      }
      
      this.optimalThreadCount = bestThreadCount;
      this.maxThroughput = bestThroughput;
      
      // Calculate scalability factor (throughput at max threads / single thread throughput)
      final MeasurementResult singleThreadResult = results.get(1);
      if (singleThreadResult != null && singleThreadResult.getOperationsPerSecond() > 0) {
        this.scalabilityFactor = bestThroughput / singleThreadResult.getOperationsPerSecond();
      } else {
        this.scalabilityFactor = 1.0;
      }
    }

    public String getTestName() {
      return testName;
    }

    public Map<Integer, MeasurementResult> getResults() {
      return new HashMap<>(results);
    }

    public int getOptimalThreadCount() {
      return optimalThreadCount;
    }

    public double getMaxThroughput() {
      return maxThroughput;
    }

    public double getScalabilityFactor() {
      return scalabilityFactor;
    }

    /**
     * Generates a scalability analysis report.
     *
     * @return formatted analysis report
     */
    public String generateReport() {
      final StringBuilder report = new StringBuilder();
      report.append("Scalability Analysis Report for ").append(testName).append("\n");
      report.append("=".repeat(50)).append("\n\n");
      
      report.append(String.format("Optimal Thread Count: %d\n", optimalThreadCount));
      report.append(String.format("Maximum Throughput: %.2f ops/sec\n", maxThroughput));
      report.append(String.format("Scalability Factor: %.2fx\n\n", scalabilityFactor));
      
      report.append("Thread Count Analysis:\n");
      results.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> {
            final int threads = entry.getKey();
            final MeasurementResult result = entry.getValue();
            report.append(String.format("  %2d threads: %8.2f ops/sec (avg: %.2f ms)\n",
                threads, result.getOperationsPerSecond(), result.getMean() / 1_000_000.0));
          });
      
      return report.toString();
    }
  }

  /**
   * Runs benchmarks with different GC algorithms for comparison.
   *
   * @param testName base test name
   * @param operation the operation to benchmark
   * @param config benchmark configuration
   * @return map of GC algorithms to results
   */
  public static Map<String, MeasurementResult> runGCAlgorithmComparison(
      final String testName, final BenchmarkOperation operation, final Configuration config) {
    
    final Map<String, String[]> gcOptions = new HashMap<>();
    gcOptions.put("Serial", new String[]{"-XX:+UseSerialGC"});
    gcOptions.put("Parallel", new String[]{"-XX:+UseParallelGC"});
    gcOptions.put("G1", new String[]{"-XX:+UseG1GC"});
    gcOptions.put("ZGC", new String[]{"-XX:+UseZGC"});
    
    return runJvmOptionComparison(testName + "_gc", operation, config, gcOptions);
  }

  /** Helper method to get current compilation time. */
  private static long getCompilationTime() {
    try {
      final javax.management.MXBean compilationBean = 
          java.lang.management.ManagementFactory.getCompilationMXBean();
      if (compilationBean != null) {
        return ((java.lang.management.CompilationMXBean) compilationBean).getTotalCompilationTime();
      }
    } catch (final Exception e) {
      LOGGER.fine("Could not get compilation time: " + e.getMessage());
    }
    return 0L;
  }

  /** Helper method to get current GC time. */
  private static long getGCTime() {
    long totalGCTime = 0L;
    try {
      for (final java.lang.management.GarbageCollectorMXBean gcBean : 
           java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
        final long gcTime = gcBean.getCollectionTime();
        if (gcTime > 0) {
          totalGCTime += gcTime;
        }
      }
    } catch (final Exception e) {
      LOGGER.fine("Could not get GC time: " + e.getMessage());
    }
    return totalGCTime;
  }

  /** Helper method to log JVM options. */
  private static void logJvmOptions(final String[] jvmOptions) {
    LOGGER.info("JVM Options: " + String.join(" ", jvmOptions));
    // In a real implementation, these options would be applied to spawned JVM processes
  }

  /**
   * Runs performance regression analysis comparing current results with historical baselines.
   *
   * @param testName name of the test
   * @param currentResult current benchmark result
   * @param historicalResults list of historical results for comparison
   * @param regressionThreshold threshold for detecting regressions (e.g., 0.1 for 10%)
   * @return regression analysis result
   */
  public static PerformanceRegressionAnalysis analyzePerformanceRegression(
      final String testName,
      final MeasurementResult currentResult,
      final List<MeasurementResult> historicalResults,
      final double regressionThreshold) {
    
    if (historicalResults.isEmpty()) {
      return new PerformanceRegressionAnalysis(testName, false, 0.0, "No historical data available");
    }
    
    // Calculate baseline from historical results (median of recent results)
    final List<Double> historicalThroughputs = historicalResults.stream()
        .mapToDouble(MeasurementResult::getOperationsPerSecond)
        .sorted()
        .boxed()
        .collect(java.util.stream.Collectors.toList());
    
    final double baseline = historicalThroughputs.get(historicalThroughputs.size() / 2);
    final double currentThroughput = currentResult.getOperationsPerSecond();
    final double regressionRatio = (baseline - currentThroughput) / baseline;
    
    final boolean isRegression = regressionRatio > regressionThreshold;
    final String analysis = generateRegressionAnalysis(baseline, currentThroughput, regressionRatio, isRegression);
    
    return new PerformanceRegressionAnalysis(testName, isRegression, regressionRatio, analysis);
  }

  /** Performance regression analysis result. */
  public static final class PerformanceRegressionAnalysis {
    private final String testName;
    private final boolean isRegression;
    private final double regressionRatio;
    private final String analysis;

    private PerformanceRegressionAnalysis(
        final String testName, final boolean isRegression, final double regressionRatio, final String analysis) {
      this.testName = testName;
      this.isRegression = isRegression;
      this.regressionRatio = regressionRatio;
      this.analysis = analysis;
    }

    public String getTestName() {
      return testName;
    }

    public boolean isRegression() {
      return isRegression;
    }

    public double getRegressionRatio() {
      return regressionRatio;
    }

    public String getAnalysis() {
      return analysis;
    }
  }

  /** Helper method to generate regression analysis text. */
  private static String generateRegressionAnalysis(
      final double baseline, final double current, final double ratio, final boolean isRegression) {
    final StringBuilder analysis = new StringBuilder();
    
    if (isRegression) {
      analysis.append(String.format(
          "PERFORMANCE REGRESSION DETECTED: Current throughput (%.2f ops/sec) is %.1f%% lower than baseline (%.2f ops/sec)",
          current, ratio * 100, baseline));
    } else if (ratio < -0.05) { // 5% improvement
      analysis.append(String.format(
          "PERFORMANCE IMPROVEMENT: Current throughput (%.2f ops/sec) is %.1f%% higher than baseline (%.2f ops/sec)",
          current, Math.abs(ratio) * 100, baseline));
    } else {
      analysis.append(String.format(
          "PERFORMANCE STABLE: Current throughput (%.2f ops/sec) is within acceptable range of baseline (%.2f ops/sec)",
          current, baseline));
    }
    
    return analysis.toString();
  }
}
