package ai.tegmentum.wasmtime4j.stress;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Comprehensive stress testing framework with configurable load parameters. Provides capabilities
 * for concurrent load testing, resource exhaustion testing, long-running stability tests, and
 * performance degradation analysis.
 */
public final class StressTestFramework {
  private static final Logger LOGGER = Logger.getLogger(StressTestFramework.class.getName());

  // Default configuration values
  private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
  private static final Duration DEFAULT_DURATION = Duration.ofMinutes(5);
  private static final int DEFAULT_OPERATIONS_PER_SECOND = 1000;
  private static final Duration DEFAULT_RAMP_UP_TIME = Duration.ofSeconds(30);

  private StressTestFramework() {
    // Utility class - prevent instantiation
  }

  /** Stress test configuration with comprehensive load parameters. */
  public static final class Configuration {
    private final int threadCount;
    private final Duration testDuration;
    private final int operationsPerSecond;
    private final Duration rampUpTime;
    private final Duration rampDownTime;
    private final boolean enableThroughputMonitoring;
    private final boolean enableLatencyMonitoring;
    private final boolean enableResourceMonitoring;
    private final Duration monitoringInterval;
    private final int maxConcurrentConnections;
    private final long maxMemoryUsage;
    private final double errorThreshold;
    private final Map<String, Object> customParameters;

    private Configuration(final Builder builder) {
      this.threadCount = builder.threadCount;
      this.testDuration = builder.testDuration;
      this.operationsPerSecond = builder.operationsPerSecond;
      this.rampUpTime = builder.rampUpTime;
      this.rampDownTime = builder.rampDownTime;
      this.enableThroughputMonitoring = builder.enableThroughputMonitoring;
      this.enableLatencyMonitoring = builder.enableLatencyMonitoring;
      this.enableResourceMonitoring = builder.enableResourceMonitoring;
      this.monitoringInterval = builder.monitoringInterval;
      this.maxConcurrentConnections = builder.maxConcurrentConnections;
      this.maxMemoryUsage = builder.maxMemoryUsage;
      this.errorThreshold = builder.errorThreshold;
      this.customParameters = new HashMap<>(builder.customParameters);
    }

    // Getters
    public int getThreadCount() {
      return threadCount;
    }

    public Duration getTestDuration() {
      return testDuration;
    }

    public int getOperationsPerSecond() {
      return operationsPerSecond;
    }

    public Duration getRampUpTime() {
      return rampUpTime;
    }

    public Duration getRampDownTime() {
      return rampDownTime;
    }

    public boolean isThroughputMonitoringEnabled() {
      return enableThroughputMonitoring;
    }

    public boolean isLatencyMonitoringEnabled() {
      return enableLatencyMonitoring;
    }

    public boolean isResourceMonitoringEnabled() {
      return enableResourceMonitoring;
    }

    public Duration getMonitoringInterval() {
      return monitoringInterval;
    }

    public int getMaxConcurrentConnections() {
      return maxConcurrentConnections;
    }

    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }

    public double getErrorThreshold() {
      return errorThreshold;
    }

    public Map<String, Object> getCustomParameters() {
      return new HashMap<>(customParameters);
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Builder for configuring StressTestFramework instances. */
    public static final class Builder {
      private int threadCount = DEFAULT_THREAD_COUNT;
      private Duration testDuration = DEFAULT_DURATION;
      private int operationsPerSecond = DEFAULT_OPERATIONS_PER_SECOND;
      private Duration rampUpTime = DEFAULT_RAMP_UP_TIME;
      private Duration rampDownTime = Duration.ZERO;
      private boolean enableThroughputMonitoring = true;
      private boolean enableLatencyMonitoring = true;
      private boolean enableResourceMonitoring = true;
      private Duration monitoringInterval = Duration.ofSeconds(1);
      private int maxConcurrentConnections = 1000;
      private long maxMemoryUsage = Runtime.getRuntime().maxMemory() / 2;
      private double errorThreshold = 0.01; // 1% error rate threshold
      private final Map<String, Object> customParameters = new HashMap<>();

      public Builder threadCount(final int threads) {
        this.threadCount = threads;
        return this;
      }

      public Builder testDuration(final Duration duration) {
        this.testDuration = duration;
        return this;
      }

      public Builder operationsPerSecond(final int ops) {
        this.operationsPerSecond = ops;
        return this;
      }

      public Builder rampUpTime(final Duration rampUp) {
        this.rampUpTime = rampUp;
        return this;
      }

      public Builder rampDownTime(final Duration rampDown) {
        this.rampDownTime = rampDown;
        return this;
      }

      public Builder enableThroughputMonitoring(final boolean enable) {
        this.enableThroughputMonitoring = enable;
        return this;
      }

      public Builder enableLatencyMonitoring(final boolean enable) {
        this.enableLatencyMonitoring = enable;
        return this;
      }

      public Builder enableResourceMonitoring(final boolean enable) {
        this.enableResourceMonitoring = enable;
        return this;
      }

      public Builder monitoringInterval(final Duration interval) {
        this.monitoringInterval = interval;
        return this;
      }

      public Builder maxConcurrentConnections(final int maxConnections) {
        this.maxConcurrentConnections = maxConnections;
        return this;
      }

      public Builder maxMemoryUsage(final long maxMemory) {
        this.maxMemoryUsage = maxMemory;
        return this;
      }

      public Builder errorThreshold(final double threshold) {
        this.errorThreshold = threshold;
        return this;
      }

      public Builder customParameter(final String key, final Object value) {
        this.customParameters.put(key, value);
        return this;
      }

      public Configuration build() {
        return new Configuration(this);
      }
    }
  }

  /** Stress test execution result with comprehensive metrics. */
  public static final class StressTestResult {
    private final String testName;
    private final RuntimeType runtimeType;
    private final Configuration configuration;
    private final Duration actualDuration;
    private final long totalOperations;
    private final long successfulOperations;
    private final long failedOperations;
    private final double averageThroughput;
    private final double peakThroughput;
    private final double averageLatency;
    private final double p95Latency;
    private final double p99Latency;
    private final long peakMemoryUsage;
    private final List<PerformanceDataPoint> performanceData;
    private final Map<String, Object> additionalMetrics;
    private final boolean testPassed;
    private final List<String> errors;

    private StressTestResult(
        final String testName,
        final RuntimeType runtimeType,
        final Configuration configuration,
        final Duration actualDuration,
        final long totalOperations,
        final long successfulOperations,
        final long failedOperations,
        final double averageThroughput,
        final double peakThroughput,
        final double averageLatency,
        final double p95Latency,
        final double p99Latency,
        final long peakMemoryUsage,
        final List<PerformanceDataPoint> performanceData,
        final Map<String, Object> additionalMetrics,
        final boolean testPassed,
        final List<String> errors) {
      this.testName = testName;
      this.runtimeType = runtimeType;
      this.configuration = configuration;
      this.actualDuration = actualDuration;
      this.totalOperations = totalOperations;
      this.successfulOperations = successfulOperations;
      this.failedOperations = failedOperations;
      this.averageThroughput = averageThroughput;
      this.peakThroughput = peakThroughput;
      this.averageLatency = averageLatency;
      this.p95Latency = p95Latency;
      this.p99Latency = p99Latency;
      this.peakMemoryUsage = peakMemoryUsage;
      this.performanceData = new ArrayList<>(performanceData);
      this.additionalMetrics = new HashMap<>(additionalMetrics);
      this.testPassed = testPassed;
      this.errors = new ArrayList<>(errors);
    }

    // Getters
    public String getTestName() {
      return testName;
    }

    public RuntimeType getRuntimeType() {
      return runtimeType;
    }

    public Configuration getConfiguration() {
      return configuration;
    }

    public Duration getActualDuration() {
      return actualDuration;
    }

    public long getTotalOperations() {
      return totalOperations;
    }

    public long getSuccessfulOperations() {
      return successfulOperations;
    }

    public long getFailedOperations() {
      return failedOperations;
    }

    public double getAverageThroughput() {
      return averageThroughput;
    }

    public double getPeakThroughput() {
      return peakThroughput;
    }

    public double getAverageLatency() {
      return averageLatency;
    }

    public double getP95Latency() {
      return p95Latency;
    }

    public double getP99Latency() {
      return p99Latency;
    }

    public long getPeakMemoryUsage() {
      return peakMemoryUsage;
    }

    public List<PerformanceDataPoint> getPerformanceData() {
      return new ArrayList<>(performanceData);
    }

    public Map<String, Object> getAdditionalMetrics() {
      return new HashMap<>(additionalMetrics);
    }

    public boolean isTestPassed() {
      return testPassed;
    }

    public List<String> getErrors() {
      return new ArrayList<>(errors);
    }

    public double getErrorRate() {
      return totalOperations > 0 ? (double) failedOperations / totalOperations : 0.0;
    }

    public double getSuccessRate() {
      return totalOperations > 0 ? (double) successfulOperations / totalOperations : 0.0;
    }
  }

  /** Performance data point for monitoring during stress test. */
  public static final class PerformanceDataPoint {
    private final Instant timestamp;
    private final double throughput;
    private final double latency;
    private final long memoryUsage;
    private final int activeThreads;
    private final long errorCount;

    /**
     * Creates a new performance data point.
     *
     * @param timestamp when this data point was recorded
     * @param throughput the throughput measurement (operations per second)
     * @param latency the latency measurement (milliseconds)
     * @param memoryUsage the memory usage (bytes)
     * @param activeThreads the number of active threads
     * @param errorCount the cumulative error count
     */
    public PerformanceDataPoint(
        final Instant timestamp,
        final double throughput,
        final double latency,
        final long memoryUsage,
        final int activeThreads,
        final long errorCount) {
      this.timestamp = timestamp;
      this.throughput = throughput;
      this.latency = latency;
      this.memoryUsage = memoryUsage;
      this.activeThreads = activeThreads;
      this.errorCount = errorCount;
    }

    // Getters
    public Instant getTimestamp() {
      return timestamp;
    }

    public double getThroughput() {
      return throughput;
    }

    public double getLatency() {
      return latency;
    }

    public long getMemoryUsage() {
      return memoryUsage;
    }

    public int getActiveThreads() {
      return activeThreads;
    }

    public long getErrorCount() {
      return errorCount;
    }
  }

  /** Functional interface for stress test operations. */
  @FunctionalInterface
  public interface StressTestOperation {
    void execute(WasmRuntime runtime, int threadId, long operationId) throws Exception;
  }

  /** Thread-safe metrics collector for stress testing. */
  private static final class MetricsCollector {
    private final AtomicLong totalOperations = new AtomicLong();
    private final AtomicLong successfulOperations = new AtomicLong();
    private final AtomicLong failedOperations = new AtomicLong();
    private final AtomicLong totalLatencyNanos = new AtomicLong();
    private final List<Long> latencyMeasurements = Collections.synchronizedList(new ArrayList<>());
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private final List<PerformanceDataPoint> performanceData =
        Collections.synchronizedList(new ArrayList<>());

    void recordOperation(final boolean success, final long latencyNanos, final String error) {
      totalOperations.incrementAndGet();
      if (success) {
        successfulOperations.incrementAndGet();
        totalLatencyNanos.addAndGet(latencyNanos);
        latencyMeasurements.add(latencyNanos);
      } else {
        failedOperations.incrementAndGet();
        if (error != null) {
          errors.add(error);
        }
      }
    }

    void recordPerformanceData(final PerformanceDataPoint dataPoint) {
      performanceData.add(dataPoint);
    }

    double getAverageLatency() {
      final long successful = successfulOperations.get();
      return successful > 0 ? (double) totalLatencyNanos.get() / successful / 1_000_000.0 : 0.0;
    }

    double getPercentileLatency(final double percentile) {
      if (latencyMeasurements.isEmpty()) {
        return 0.0;
      }

      final List<Long> sortedLatencies = new ArrayList<>(latencyMeasurements);
      Collections.sort(sortedLatencies);

      final int index = (int) Math.ceil(sortedLatencies.size() * percentile / 100.0) - 1;
      final int safeIndex = Math.max(0, Math.min(index, sortedLatencies.size() - 1));

      return sortedLatencies.get(safeIndex) / 1_000_000.0; // Convert to milliseconds
    }

    // Getters
    long getTotalOperations() {
      return totalOperations.get();
    }

    long getSuccessfulOperations() {
      return successfulOperations.get();
    }

    long getFailedOperations() {
      return failedOperations.get();
    }

    List<String> getErrors() {
      return new ArrayList<>(errors);
    }

    List<PerformanceDataPoint> getPerformanceData() {
      return new ArrayList<>(performanceData);
    }
  }

  /**
   * Runs a comprehensive stress test with the given configuration.
   *
   * @param testName name of the test
   * @param operation operation to stress test
   * @param config stress test configuration
   * @return stress test result
   */
  public static StressTestResult runStressTest(
      final String testName, final StressTestOperation operation, final Configuration config) {
    return runStressTest(testName, RuntimeType.JNI, operation, config);
  }

  /**
   * Runs a stress test for a specific runtime.
   *
   * @param testName name of the test
   * @param runtimeType runtime to test
   * @param operation operation to stress test
   * @param config stress test configuration
   * @return stress test result
   */
  public static StressTestResult runStressTest(
      final String testName,
      final RuntimeType runtimeType,
      final StressTestOperation operation,
      final Configuration config) {
    LOGGER.info("Starting stress test: " + testName + " with " + runtimeType);

    final MetricsCollector metrics = new MetricsCollector();
    final Instant testStart = Instant.now();
    final AtomicInteger activeThreads = new AtomicInteger();
    final AtomicLong operationIdCounter = new AtomicLong();

    // Create thread pool for test execution
    final ExecutorService testExecutor =
        Executors.newFixedThreadPool(
            config.getThreadCount(),
            new ThreadFactory() {
              private final AtomicInteger threadCounter = new AtomicInteger();

              @Override
              public Thread newThread(final Runnable r) {
                final Thread thread =
                    new Thread(r, "StressTest-" + threadCounter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
              }
          });

    // Set up performance monitoring
    final ScheduledExecutorService monitoringExecutor =
        Executors.newSingleThreadScheduledExecutor();
    final CompletableFuture<Void> monitoringComplete = new CompletableFuture<>();

    if (config.isResourceMonitoringEnabled() || config.isThroughputMonitoringEnabled()) {
      monitoringExecutor.scheduleAtFixedRate(
          () -> {
            try {
              final Instant now = Instant.now();
              final double throughput = calculateCurrentThroughput(metrics, now, testStart);
              final double avgLatency = metrics.getAverageLatency();
              final long memoryUsage =
                  Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
              final int currentActiveThreads = activeThreads.get();
              final long currentErrors = metrics.getFailedOperations();

              final PerformanceDataPoint dataPoint =
                  new PerformanceDataPoint(
                      now,
                      throughput,
                      avgLatency,
                      memoryUsage,
                      currentActiveThreads,
                      currentErrors);

              metrics.recordPerformanceData(dataPoint);

              LOGGER.fine(
                  String.format(
                      "Performance: %.2f ops/sec, %.2f ms latency, %d MB memory, %d threads",
                      throughput, avgLatency, memoryUsage / 1024 / 1024, currentActiveThreads));

            } catch (final Exception e) {
              LOGGER.warning("Performance monitoring failed: " + e.getMessage());
            }
          },
          config.getMonitoringInterval().toMillis(),
          config.getMonitoringInterval().toMillis(),
          TimeUnit.MILLISECONDS);
    }

    try {
      // Calculate test phases
      final Instant rampUpEnd = testStart.plus(config.getRampUpTime());
      final Instant steadyStateEnd = rampUpEnd.plus(config.getTestDuration());
      final Instant testEnd = steadyStateEnd.plus(config.getRampDownTime());

      // Launch worker threads
      final List<CompletableFuture<Void>> workerFutures =
          IntStream.range(0, config.getThreadCount())
              .mapToObj(
                  threadId ->
                      CompletableFuture.runAsync(
                          () -> {
                            runWorkerThread(
                                threadId,
                                operation,
                                runtimeType,
                                config,
                                metrics,
                                testStart,
                                rampUpEnd,
                                steadyStateEnd,
                                testEnd,
                                activeThreads,
                                operationIdCounter);
                          },
                          testExecutor))
              .collect(Collectors.toList());

      // Wait for all workers to complete
      CompletableFuture.allOf(workerFutures.toArray(new CompletableFuture<?>[0])).join();

    } finally {
      testExecutor.shutdown();
      monitoringExecutor.shutdown();

      try {
        if (!testExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
          testExecutor.shutdownNow();
        }
        if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          monitoringExecutor.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        testExecutor.shutdownNow();
        monitoringExecutor.shutdownNow();
      }
    }

    final Instant testActualEnd = Instant.now();
    final Duration actualDuration = Duration.between(testStart, testActualEnd);

    return buildStressTestResult(testName, runtimeType, config, actualDuration, metrics);
  }

  /** Worker thread execution logic with load control. */
  private static void runWorkerThread(
      final int threadId,
      final StressTestOperation operation,
      final RuntimeType runtimeType,
      final Configuration config,
      final MetricsCollector metrics,
      final Instant testStart,
      final Instant rampUpEnd,
      final Instant steadyStateEnd,
      final Instant testEnd,
      final AtomicInteger activeThreads,
      final AtomicLong operationIdCounter) {
    activeThreads.incrementAndGet();

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {

      while (Instant.now().isBefore(testEnd)) {
        final Instant now = Instant.now();
        final long operationId = operationIdCounter.incrementAndGet();

        // Calculate target operations per second based on current phase
        int targetOpsPerSecond = config.getOperationsPerSecond();

        if (now.isBefore(rampUpEnd)) {
          // Ramp-up phase: gradually increase load
          final double rampProgress =
              (double) Duration.between(testStart, now).toMillis()
                  / config.getRampUpTime().toMillis();
          targetOpsPerSecond = (int) (config.getOperationsPerSecond() * rampProgress);
        } else if (now.isAfter(steadyStateEnd)) {
          // Ramp-down phase: gradually decrease load
          final double rampDownProgress =
              (double) Duration.between(steadyStateEnd, now).toMillis()
                  / config.getRampDownTime().toMillis();
          targetOpsPerSecond = (int) (config.getOperationsPerSecond() * (1.0 - rampDownProgress));
        }

        // Calculate delay to achieve target operations per second
        final long delayMs =
            Math.max(0, 1000L * config.getThreadCount() / Math.max(1, targetOpsPerSecond));

        // Execute operation
        final Instant operationStart = Instant.now();
        boolean success = false;
        String error = null;

        try {
          operation.execute(runtime, threadId, operationId);
          success = true;
        } catch (final Exception e) {
          error = e.getClass().getSimpleName() + ": " + e.getMessage();
          LOGGER.fine("Operation failed in thread " + threadId + ": " + error);
        }

        final long latencyNanos = Duration.between(operationStart, Instant.now()).toNanos();
        metrics.recordOperation(success, latencyNanos, error);

        // Apply load control delay
        if (delayMs > 0) {
          try {
            Thread.sleep(delayMs);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }

    } catch (final Exception e) {
      LOGGER.severe("Worker thread " + threadId + " failed: " + e.getMessage());
    } finally {
      activeThreads.decrementAndGet();
    }
  }

  /** Calculates current throughput in operations per second. */
  private static double calculateCurrentThroughput(
      final MetricsCollector metrics, final Instant now, final Instant testStart) {
    final long elapsedSeconds = Duration.between(testStart, now).toSeconds();
    return elapsedSeconds > 0 ? (double) metrics.getTotalOperations() / elapsedSeconds : 0.0;
  }

  /** Builds the final stress test result from collected metrics. */
  private static StressTestResult buildStressTestResult(
      final String testName,
      final RuntimeType runtimeType,
      final Configuration config,
      final Duration actualDuration,
      final MetricsCollector metrics) {
    final long totalOps = metrics.getTotalOperations();
    final long successfulOps = metrics.getSuccessfulOperations();
    final long failedOps = metrics.getFailedOperations();

    final double avgThroughput =
        actualDuration.toSeconds() > 0 ? (double) totalOps / actualDuration.toSeconds() : 0.0;

    final List<PerformanceDataPoint> perfData = metrics.getPerformanceData();
    final double peakThroughput =
        perfData.stream().mapToDouble(PerformanceDataPoint::getThroughput).max().orElse(0.0);

    final long peakMemory =
        perfData.stream().mapToLong(PerformanceDataPoint::getMemoryUsage).max().orElse(0L);

    final double avgLatency = metrics.getAverageLatency();
    final double p95Latency = metrics.getPercentileLatency(95.0);
    final double p99Latency = metrics.getPercentileLatency(99.0);

    // Determine if test passed
    final double errorRate = totalOps > 0 ? (double) failedOps / totalOps : 0.0;
    final boolean testPassed = errorRate <= config.getErrorThreshold();

    // Additional metrics
    final Map<String, Object> additionalMetrics = new HashMap<>();
    additionalMetrics.put("errorRate", errorRate);
    additionalMetrics.put("targetThroughput", config.getOperationsPerSecond());
    additionalMetrics.put("throughputAchievement", avgThroughput / config.getOperationsPerSecond());
    additionalMetrics.put("memoryEfficiency", peakMemory / (double) config.getMaxMemoryUsage());

    return new StressTestResult(
        testName,
        runtimeType,
        config,
        actualDuration,
        totalOps,
        successfulOps,
        failedOps,
        avgThroughput,
        peakThroughput,
        avgLatency,
        p95Latency,
        p99Latency,
        peakMemory,
        perfData,
        additionalMetrics,
        testPassed,
        metrics.getErrors());
  }

  /**
   * Runs cross-runtime stress test comparison.
   *
   * @param testName name of the test
   * @param operation operation to stress test
   * @param config stress test configuration
   * @return comparison results for different runtimes
   */
  public static Map<RuntimeType, StressTestResult> runCrossRuntimeStressTest(
      final String testName, final StressTestOperation operation, final Configuration config) {

    final Map<RuntimeType, StressTestResult> results = new HashMap<>();

    // Test JNI runtime
    LOGGER.info("Running stress test with JNI runtime");
    results.put(
        RuntimeType.JNI, runStressTest(testName + "[JNI]", RuntimeType.JNI, operation, config));

    // Test Panama runtime if available
    if (TestUtils.isPanamaAvailable()) {
      LOGGER.info("Running stress test with Panama runtime");
      results.put(
          RuntimeType.PANAMA,
          runStressTest(testName + "[Panama]", RuntimeType.PANAMA, operation, config));
    } else {
      LOGGER.warning("Panama runtime not available for stress test comparison");
    }

    return results;
  }

  /**
   * Runs a scalability test with increasing load levels.
   *
   * @param testName base test name
   * @param operation operation to test
   * @param baseConfig base configuration
   * @param loadLevels array of multipliers for operations per second
   * @return results for each load level
   */
  public static Map<Integer, StressTestResult> runScalabilityTest(
      final String testName,
      final StressTestOperation operation,
      final Configuration baseConfig,
      final int[] loadLevels) {
    final Map<Integer, StressTestResult> results = new HashMap<>();

    for (final int loadLevel : loadLevels) {
      LOGGER.info("Running scalability test at " + loadLevel + "x load");

      final Configuration config =
          Configuration.builder()
              .threadCount(baseConfig.getThreadCount())
              .testDuration(baseConfig.getTestDuration())
              .operationsPerSecond(baseConfig.getOperationsPerSecond() * loadLevel)
              .rampUpTime(baseConfig.getRampUpTime())
              .rampDownTime(baseConfig.getRampDownTime())
              .enableThroughputMonitoring(baseConfig.isThroughputMonitoringEnabled())
              .enableLatencyMonitoring(baseConfig.isLatencyMonitoringEnabled())
              .enableResourceMonitoring(baseConfig.isResourceMonitoringEnabled())
              .monitoringInterval(baseConfig.getMonitoringInterval())
              .maxConcurrentConnections(baseConfig.getMaxConcurrentConnections())
              .maxMemoryUsage(baseConfig.getMaxMemoryUsage())
              .errorThreshold(baseConfig.getErrorThreshold())
              .build();

      final StressTestResult result =
          runStressTest(testName + "[" + loadLevel + "x]", operation, config);
      results.put(loadLevel, result);
    }

    return results;
  }

  /**
   * Generates a comprehensive stress test report.
   *
   * @param results list of stress test results
   * @return formatted report
   */
  public static String generateReport(final List<StressTestResult> results) {
    final StringBuilder report = new StringBuilder();
    report.append("Stress Test Report\n");
    report.append("==================\n\n");

    int passedTests = 0;
    for (final StressTestResult result : results) {
      report.append("Test: ").append(result.getTestName()).append("\n");
      if (result.getRuntimeType() != null) {
        report.append("Runtime: ").append(result.getRuntimeType()).append("\n");
      }
      report.append("Status: ").append(result.isTestPassed() ? "PASSED" : "FAILED").append("\n");
      report.append("Duration: ").append(result.getActualDuration().toMillis()).append(" ms\n");
      report.append(
          String.format(
              "Operations: %d total, %d successful (%.2f%% success rate)\n",
              result.getTotalOperations(),
              result.getSuccessfulOperations(),
              result.getSuccessRate() * 100));
      report.append(
          String.format(
              "Throughput: %.2f ops/sec avg, %.2f ops/sec peak\n",
              result.getAverageThroughput(), result.getPeakThroughput()));
      report.append(
          String.format(
              "Latency: %.2f ms avg, %.2f ms p95, %.2f ms p99\n",
              result.getAverageLatency(), result.getP95Latency(), result.getP99Latency()));
      report.append(
          String.format(
              "Memory: %.2f MB peak usage\n", result.getPeakMemoryUsage() / 1024.0 / 1024.0));

      if (result.isTestPassed()) {
        passedTests++;
      } else {
        report.append("Errors: ").append(result.getFailedOperations()).append("\n");
        if (!result.getErrors().isEmpty()) {
          report.append("Sample errors:\n");
          result.getErrors().stream()
              .limit(5)
              .forEach(error -> report.append("  - ").append(error).append("\n"));
        }
      }

      report.append("\n");
    }

    report
        .append("Summary: ")
        .append(passedTests)
        .append(" of ")
        .append(results.size())
        .append(" tests passed\n");

    return report.toString();
  }

  /**
   * Gets default stress test configuration.
   *
   * @return default configuration
   */
  public static Configuration getDefaultConfiguration() {
    return Configuration.builder().build();
  }

  /**
   * Gets light stress test configuration for quick testing.
   *
   * @return light configuration with lower load
   */
  public static Configuration getLightConfiguration() {
    return Configuration.builder()
        .threadCount(2)
        .testDuration(Duration.ofMinutes(1))
        .operationsPerSecond(100)
        .rampUpTime(Duration.ofSeconds(10))
        .build();
  }

  /**
   * Gets heavy stress test configuration for comprehensive testing.
   *
   * @return heavy configuration with high load
   */
  public static Configuration getHeavyConfiguration() {
    return Configuration.builder()
        .threadCount(Runtime.getRuntime().availableProcessors() * 2)
        .testDuration(Duration.ofMinutes(10))
        .operationsPerSecond(5000)
        .rampUpTime(Duration.ofMinutes(1))
        .rampDownTime(Duration.ofSeconds(30))
        .build();
  }
}
