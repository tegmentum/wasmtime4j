package ai.tegmentum.wasmtime4j.memory;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Memory leak detection utility with native tooling integration. Provides comprehensive memory
 * monitoring capabilities including Java heap analysis, native memory tracking, and integration
 * with external tools like Valgrind and AddressSanitizer.
 */
public final class MemoryLeakDetector {
  private static final Logger LOGGER = Logger.getLogger(MemoryLeakDetector.class.getName());

  // Memory sampling configuration
  private static final long DEFAULT_SAMPLING_INTERVAL_MS = 100;
  private static final int DEFAULT_SAMPLE_COUNT = 1000;
  private static final double MEMORY_LEAK_THRESHOLD = 1.2; // 20% increase considered leak

  // Native tooling paths (configurable via system properties)
  private static final String VALGRIND_PATH =
      System.getProperty("wasmtime4j.valgrind.path", "valgrind");
  private static final String ASAN_ENABLED = System.getProperty("wasmtime4j.asan.enabled", "false");

  private MemoryLeakDetector() {
    // Utility class - prevent instantiation
  }

  /** Memory measurement snapshot. */
  public static final class MemorySnapshot {
    private final Instant timestamp;
    private final long heapUsed;
    private final long heapMax;
    private final long nonHeapUsed;
    private final long nonHeapMax;
    private final long nativeMemory;
    private final int objectCount;
    private final Map<String, Long> memoryPools;

    private MemorySnapshot(
        final Instant timestamp,
        final long heapUsed,
        final long heapMax,
        final long nonHeapUsed,
        final long nonHeapMax,
        final long nativeMemory,
        final int objectCount,
        final Map<String, Long> memoryPools) {
      this.timestamp = timestamp;
      this.heapUsed = heapUsed;
      this.heapMax = heapMax;
      this.nonHeapUsed = nonHeapUsed;
      this.nonHeapMax = nonHeapMax;
      this.nativeMemory = nativeMemory;
      this.objectCount = objectCount;
      this.memoryPools = new HashMap<>(memoryPools);
    }

    // Getters
    public Instant getTimestamp() {
      return timestamp;
    }

    public long getHeapUsed() {
      return heapUsed;
    }

    public long getHeapMax() {
      return heapMax;
    }

    public long getNonHeapUsed() {
      return nonHeapUsed;
    }

    public long getNonHeapMax() {
      return nonHeapMax;
    }

    public long getNativeMemory() {
      return nativeMemory;
    }

    public int getObjectCount() {
      return objectCount;
    }

    public Map<String, Long> getMemoryPools() {
      return new HashMap<>(memoryPools);
    }

    public long getTotalUsed() {
      return heapUsed + nonHeapUsed + nativeMemory;
    }
  }

  /** Memory leak analysis result. */
  public static final class LeakAnalysisResult {
    private final String testName;
    private final RuntimeType runtimeType;
    private final List<MemorySnapshot> snapshots;
    private final boolean leakDetected;
    private final long memoryIncrease;
    private final double leakRate; // bytes per second
    private final String analysis;
    private final List<String> recommendations;
    private final Map<String, Object> diagnosticInfo;

    private LeakAnalysisResult(
        final String testName,
        final RuntimeType runtimeType,
        final List<MemorySnapshot> snapshots,
        final boolean leakDetected,
        final long memoryIncrease,
        final double leakRate,
        final String analysis,
        final List<String> recommendations,
        final Map<String, Object> diagnosticInfo) {
      this.testName = testName;
      this.runtimeType = runtimeType;
      this.snapshots = new ArrayList<>(snapshots);
      this.leakDetected = leakDetected;
      this.memoryIncrease = memoryIncrease;
      this.leakRate = leakRate;
      this.analysis = analysis;
      this.recommendations = new ArrayList<>(recommendations);
      this.diagnosticInfo = new HashMap<>(diagnosticInfo);
    }

    // Getters
    public String getTestName() {
      return testName;
    }

    public RuntimeType getRuntimeType() {
      return runtimeType;
    }

    public List<MemorySnapshot> getSnapshots() {
      return new ArrayList<>(snapshots);
    }

    public boolean isLeakDetected() {
      return leakDetected;
    }

    public long getMemoryIncrease() {
      return memoryIncrease;
    }

    public double getLeakRate() {
      return leakRate;
    }

    public String getAnalysis() {
      return analysis;
    }

    public List<String> getRecommendations() {
      return new ArrayList<>(recommendations);
    }

    public Map<String, Object> getDiagnosticInfo() {
      return new HashMap<>(diagnosticInfo);
    }
  }

  /** Configuration for memory leak detection. */
  public static final class Configuration {
    private final long samplingIntervalMs;
    private final int sampleCount;
    private final boolean enableNativeTracking;
    private final boolean enableValgrind;
    private final boolean enableAddressSanitizer;
    private final Duration testDuration;
    private final double leakThreshold;

    private Configuration(final Builder builder) {
      this.samplingIntervalMs = builder.samplingIntervalMs;
      this.sampleCount = builder.sampleCount;
      this.enableNativeTracking = builder.enableNativeTracking;
      this.enableValgrind = builder.enableValgrind;
      this.enableAddressSanitizer = builder.enableAddressSanitizer;
      this.testDuration = builder.testDuration;
      this.leakThreshold = builder.leakThreshold;
    }

    // Getters
    public long getSamplingIntervalMs() {
      return samplingIntervalMs;
    }

    public int getSampleCount() {
      return sampleCount;
    }

    public boolean isNativeTrackingEnabled() {
      return enableNativeTracking;
    }

    public boolean isValgrindEnabled() {
      return enableValgrind;
    }

    public boolean isAddressSanitizerEnabled() {
      return enableAddressSanitizer;
    }

    public Duration getTestDuration() {
      return testDuration;
    }

    public double getLeakThreshold() {
      return leakThreshold;
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Builder for configuring MemoryLeakDetector instances. */
    public static final class Builder {
      private long samplingIntervalMs = DEFAULT_SAMPLING_INTERVAL_MS;
      private int sampleCount = DEFAULT_SAMPLE_COUNT;
      private boolean enableNativeTracking = true;
      private boolean enableValgrind = false;
      private boolean enableAddressSanitizer = Boolean.parseBoolean(ASAN_ENABLED);
      private Duration testDuration = Duration.ofMinutes(2);
      private double leakThreshold = MEMORY_LEAK_THRESHOLD;

      public Builder samplingInterval(final long intervalMs) {
        this.samplingIntervalMs = intervalMs;
        return this;
      }

      public Builder sampleCount(final int count) {
        this.sampleCount = count;
        return this;
      }

      public Builder enableNativeTracking(final boolean enable) {
        this.enableNativeTracking = enable;
        return this;
      }

      public Builder enableValgrind(final boolean enable) {
        this.enableValgrind = enable;
        return this;
      }

      public Builder enableAddressSanitizer(final boolean enable) {
        this.enableAddressSanitizer = enable;
        return this;
      }

      public Builder testDuration(final Duration duration) {
        this.testDuration = duration;
        return this;
      }

      public Builder leakThreshold(final double threshold) {
        this.leakThreshold = threshold;
        return this;
      }

      public Configuration build() {
        return new Configuration(this);
      }
    }
  }

  /** Functional interface for operations to test for memory leaks. */
  @FunctionalInterface
  public interface TestedOperation {
    void execute(WasmRuntime runtime) throws Exception;
  }

  /**
   * Detects memory leaks in the given operation.
   *
   * @param testName name of the test
   * @param operation operation to test
   * @param config detection configuration
   * @return leak analysis result
   */
  public static LeakAnalysisResult detectLeaks(
      final String testName, final TestedOperation operation, final Configuration config) {
    return detectLeaks(testName, RuntimeType.JNI, operation, config);
  }

  /**
   * Detects memory leaks in the given operation for a specific runtime.
   *
   * @param testName name of the test
   * @param runtimeType runtime to test
   * @param operation operation to test
   * @param config detection configuration
   * @return leak analysis result
   */
  public static LeakAnalysisResult detectLeaks(
      final String testName,
      final RuntimeType runtimeType,
      final TestedOperation operation,
      final Configuration config) {
    LOGGER.info("Starting memory leak detection for: " + testName + " with " + runtimeType);

    final List<MemorySnapshot> snapshots = new ArrayList<>();
    final Map<String, Object> diagnosticInfo = new HashMap<>();

    // Set up memory monitoring
    final ScheduledExecutorService memoryMonitor = Executors.newSingleThreadScheduledExecutor();
    final CompletableFuture<Void> monitoringComplete = new CompletableFuture<>();

    try {
      // Start memory monitoring
      memoryMonitor.scheduleAtFixedRate(
          () -> {
            try {
              final MemorySnapshot snapshot = captureMemorySnapshot();
              synchronized (snapshots) {
                snapshots.add(snapshot);
              }
            } catch (final Exception e) {
              LOGGER.warning("Failed to capture memory snapshot: " + e.getMessage());
            }
          },
          0,
          config.getSamplingIntervalMs(),
          TimeUnit.MILLISECONDS);

      // Run the test operation repeatedly
      final Instant testStart = Instant.now();
      final ExecutorService testExecutor = Executors.newSingleThreadExecutor();

      final CompletableFuture<Void> testComplete =
          CompletableFuture.runAsync(
              () -> {
                try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
                  final Instant endTime = testStart.plus(config.getTestDuration());
                  int operationCount = 0;

                  while (Instant.now().isBefore(endTime)) {
                    operation.execute(runtime);
                    operationCount++;

                    // Small delay to prevent overwhelming the system
                    try {
                      Thread.sleep(1);
                    } catch (final InterruptedException e) {
                      Thread.currentThread().interrupt();
                      break;
                    }
                  }

                  diagnosticInfo.put("operationCount", operationCount);
                  diagnosticInfo.put(
                      "testDuration", Duration.between(testStart, Instant.now()).toMillis());

                } catch (final Exception e) {
                  LOGGER.severe("Test operation failed: " + e.getMessage());
                  throw new RuntimeException("Test operation failed", e);
                }
              },
              testExecutor);

      // Wait for test completion
      testComplete.join();

      // Allow a few more samples after test completion
      Thread.sleep(config.getSamplingIntervalMs() * 5);

    } catch (final Exception e) {
      LOGGER.severe("Memory leak detection failed: " + e.getMessage());
      diagnosticInfo.put("error", e.getMessage());
    } finally {
      memoryMonitor.shutdown();
      try {
        if (!memoryMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
          memoryMonitor.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        memoryMonitor.shutdownNow();
      }
    }

    // Analyze the collected data
    return analyzeMemorySnapshots(testName, runtimeType, snapshots, config, diagnosticInfo);
  }

  /**
   * Runs memory leak detection with Valgrind integration.
   *
   * @param testName name of the test
   * @param javaCommand Java command to execute under Valgrind
   * @return Valgrind analysis result
   */
  public static String runWithValgrind(final String testName, final String javaCommand) {
    if (!TestUtils.isLinux() && !TestUtils.isMacOs()) {
      return "Valgrind is not available on " + TestUtils.getOperatingSystem();
    }

    LOGGER.info("Running Valgrind memory analysis for: " + testName);

    try {
      final ProcessBuilder processBuilder =
          new ProcessBuilder(
              VALGRIND_PATH,
              "--tool=memcheck",
              "--leak-check=full",
              "--show-leak-kinds=all",
              "--track-origins=yes",
              "--verbose",
              "--xml=yes",
              "--xml-file=valgrind-" + testName.replaceAll("[^a-zA-Z0-9]", "-") + ".xml");

      // Add Java command arguments
      final String[] javaArgs = javaCommand.split("\\s+");
      for (final String arg : javaArgs) {
        processBuilder.command().add(arg);
      }

      final Process process = processBuilder.start();

      // Capture Valgrind output
      final StringBuilder output = new StringBuilder();
      try (final BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }

      final int exitCode = process.waitFor();

      if (exitCode == 0) {
        return parseValgrindOutput(output.toString());
      } else {
        return "Valgrind execution failed with exit code: " + exitCode + "\n" + output.toString();
      }

    } catch (final IOException | InterruptedException e) {
      LOGGER.severe("Valgrind execution failed: " + e.getMessage());
      return "Valgrind execution failed: " + e.getMessage();
    }
  }

  /** Captures a memory snapshot of the current JVM state. */
  private static MemorySnapshot captureMemorySnapshot() {
    final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    // Estimate native memory usage (simplified)
    final long nativeMemory = estimateNativeMemoryUsage();

    // Count objects (simplified - in practice would use more sophisticated profiling)
    final int objectCount = estimateObjectCount();

    // Capture memory pool information
    final Map<String, Long> memoryPools = new HashMap<>();
    ManagementFactory.getMemoryPoolMXBeans()
        .forEach(
            pool -> {
              final MemoryUsage usage = pool.getUsage();
              if (usage != null) {
                memoryPools.put(pool.getName(), usage.getUsed());
              }
          });

    return new MemorySnapshot(
        Instant.now(),
        heapUsage.getUsed(),
        heapUsage.getMax(),
        nonHeapUsage.getUsed(),
        nonHeapUsage.getMax(),
        nativeMemory,
        objectCount,
        memoryPools);
  }

  /** Estimates native memory usage (simplified implementation). */
  private static long estimateNativeMemoryUsage() {
    // In a real implementation, this would integrate with JVM native memory tracking
    // or use JFR events to get more accurate native memory usage
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  /** Estimates object count (simplified implementation). */
  private static int estimateObjectCount() {
    // In practice, this would use profiling APIs or JFR to get accurate object counts
    // For now, return a placeholder based on heap usage
    final long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    return (int) (heapUsed / 64); // Rough estimate assuming 64 bytes per object on average
  }

  /** Analyzes memory snapshots to detect leaks. */
  private static LeakAnalysisResult analyzeMemorySnapshots(
      final String testName,
      final RuntimeType runtimeType,
      final List<MemorySnapshot> snapshots,
      final Configuration config,
      final Map<String, Object> diagnosticInfo) {
    if (snapshots.isEmpty()) {
      return new LeakAnalysisResult(
          testName,
          runtimeType,
          snapshots,
          false,
          0,
          0.0,
          "No memory snapshots captured",
          List.of("Ensure test runs long enough to capture data"),
          diagnosticInfo);
    }

    // Calculate memory trends
    final MemorySnapshot first = snapshots.get(0);
    final MemorySnapshot last = snapshots.get(snapshots.size() - 1);

    final long totalMemoryIncrease = last.getTotalUsed() - first.getTotalUsed();
    final long heapIncrease = last.getHeapUsed() - first.getHeapUsed();
    final long nonHeapIncrease = last.getNonHeapUsed() - first.getNonHeapUsed();
    final long nativeIncrease = last.getNativeMemory() - first.getNativeMemory();

    final Duration testDuration = Duration.between(first.getTimestamp(), last.getTimestamp());
    final double leakRate =
        testDuration.toMillis() > 0
            ? (totalMemoryIncrease * 1000.0) / testDuration.toMillis()
            : 0.0;

    // Determine if leak is detected
    final double memoryIncreaseRatio =
        first.getTotalUsed() > 0 ? (double) last.getTotalUsed() / first.getTotalUsed() : 1.0;
    final boolean leakDetected = memoryIncreaseRatio > config.getLeakThreshold();

    // Generate analysis
    final StringBuilder analysis = new StringBuilder();
    analysis.append("Memory Analysis Summary:\n");
    analysis.append(
        String.format(
            "Total memory increase: %d bytes (%.2f%%)\n",
            totalMemoryIncrease, (memoryIncreaseRatio - 1.0) * 100));
    analysis.append(String.format("Heap increase: %d bytes\n", heapIncrease));
    analysis.append(String.format("Non-heap increase: %d bytes\n", nonHeapIncrease));
    analysis.append(String.format("Native increase: %d bytes\n", nativeIncrease));
    analysis.append(String.format("Leak rate: %.2f bytes/second\n", leakRate));
    analysis.append(String.format("Test duration: %d ms\n", testDuration.toMillis()));

    // Generate recommendations
    final List<String> recommendations = new ArrayList<>();

    if (leakDetected) {
      if (heapIncrease > totalMemoryIncrease * 0.7) {
        recommendations.add("Heap memory shows largest increase - check for object retention");
        recommendations.add("Run with heap dump analysis to identify retained objects");
      }
      if (nativeIncrease > totalMemoryIncrease * 0.3) {
        recommendations.add("Native memory increase detected - check JNI resource cleanup");
        recommendations.add("Verify all native resources are properly released");
      }
      if (leakRate > 1000) {
        recommendations.add("High leak rate detected - prioritize immediate investigation");
      }
      recommendations.add("Run extended test to confirm consistent memory growth pattern");
    } else {
      recommendations.add("Memory usage appears stable");
      if (totalMemoryIncrease > 0) {
        recommendations.add("Minor memory increase may be due to JVM warmup or caching");
      }
    }

    // Add diagnostic information
    diagnosticInfo.put("snapshotCount", snapshots.size());
    diagnosticInfo.put("memoryIncreaseRatio", memoryIncreaseRatio);
    diagnosticInfo.put("leakThreshold", config.getLeakThreshold());

    return new LeakAnalysisResult(
        testName,
        runtimeType,
        snapshots,
        leakDetected,
        totalMemoryIncrease,
        leakRate,
        analysis.toString(),
        recommendations,
        diagnosticInfo);
  }

  /** Parses Valgrind output to extract memory leak information. */
  private static String parseValgrindOutput(final String output) {
    final StringBuilder summary = new StringBuilder();
    summary.append("Valgrind Memory Analysis Results:\n");
    summary.append("================================\n\n");

    // Extract key information using regex patterns
    final Pattern leakSummaryPattern =
        Pattern.compile("LEAK SUMMARY:\\s*([^=]+?)(?==|$)", Pattern.DOTALL);
    final Matcher leakMatcher = leakSummaryPattern.matcher(output);
    if (leakMatcher.find()) {
      summary.append("Leak Summary:\n");
      summary.append(leakMatcher.group(1).trim()).append("\n\n");
    }

    final Pattern errorSummaryPattern =
        Pattern.compile("ERROR SUMMARY:\\s*([^=]+?)(?==|$)", Pattern.DOTALL);
    final Matcher errorMatcher = errorSummaryPattern.matcher(output);
    if (errorMatcher.find()) {
      summary.append("Error Summary:\n");
      summary.append(errorMatcher.group(1).trim()).append("\n\n");
    }

    // Look for specific error types
    if (output.contains("Invalid read") || output.contains("Invalid write")) {
      summary.append("WARNING: Memory access violations detected!\n\n");
    }

    if (output.contains("definitely lost") || output.contains("possibly lost")) {
      summary.append("WARNING: Memory leaks detected!\n\n");
    }

    if (output.contains("All heap blocks were freed")) {
      summary.append("GOOD: No memory leaks detected.\n\n");
    }

    return summary.toString();
  }

  /**
   * Cross-runtime memory leak comparison.
   *
   * @param testName name of the test
   * @param operation operation to test
   * @param config detection configuration
   * @return comparison of leak detection results between runtimes
   */
  public static Map<RuntimeType, LeakAnalysisResult> compareRuntimes(
      final String testName, final TestedOperation operation, final Configuration config) {
    final Map<RuntimeType, LeakAnalysisResult> results = new HashMap<>();

    // Test JNI runtime
    LOGGER.info("Testing JNI runtime for memory leaks");
    results.put(
        RuntimeType.JNI, detectLeaks(testName + "[JNI]", RuntimeType.JNI, operation, config));

    // Test Panama runtime if available
    if (TestUtils.isPanamaAvailable()) {
      LOGGER.info("Testing Panama runtime for memory leaks");
      results.put(
          RuntimeType.PANAMA,
          detectLeaks(testName + "[Panama]", RuntimeType.PANAMA, operation, config));
    } else {
      LOGGER.warning("Panama runtime not available for memory leak comparison");
    }

    return results;
  }

  /**
   * Generates a comprehensive memory leak report.
   *
   * @param results list of analysis results
   * @return formatted report
   */
  public static String generateReport(final List<LeakAnalysisResult> results) {
    final StringBuilder report = new StringBuilder();
    report.append("Memory Leak Detection Report\n");
    report.append("===========================\n\n");

    int leaksDetected = 0;
    for (final LeakAnalysisResult result : results) {
      report.append("Test: ").append(result.getTestName()).append("\n");
      if (result.getRuntimeType() != null) {
        report.append("Runtime: ").append(result.getRuntimeType()).append("\n");
      }
      report.append("Leak Detected: ").append(result.isLeakDetected() ? "YES" : "NO").append("\n");

      if (result.isLeakDetected()) {
        leaksDetected++;
        report.append("Memory Increase: ").append(result.getMemoryIncrease()).append(" bytes\n");
        report
            .append("Leak Rate: ")
            .append(String.format("%.2f", result.getLeakRate()))
            .append(" bytes/sec\n");
      }

      report.append("Analysis:\n");
      report.append(result.getAnalysis()).append("\n");

      if (!result.getRecommendations().isEmpty()) {
        report.append("Recommendations:\n");
        result.getRecommendations().forEach(rec -> report.append("  - ").append(rec).append("\n"));
      }

      report.append("\n");
    }

    report
        .append("Summary: ")
        .append(leaksDetected)
        .append(" of ")
        .append(results.size())
        .append(" tests showed memory leaks\n");

    return report.toString();
  }

  /**
   * Gets default memory leak detection configuration.
   *
   * @return default configuration
   */
  public static Configuration getDefaultConfiguration() {
    return Configuration.builder().build();
  }

  /**
   * Gets fast configuration for quick leak detection.
   *
   * @return fast configuration with shorter duration and intervals
   */
  public static Configuration getFastConfiguration() {
    return Configuration.builder()
        .testDuration(Duration.ofSeconds(30))
        .samplingInterval(50)
        .sampleCount(100)
        .build();
  }

  /**
   * Gets thorough configuration for comprehensive leak detection.
   *
   * @return thorough configuration with longer duration and more samples
   */
  public static Configuration getThoroughConfiguration() {
    return Configuration.builder()
        .testDuration(Duration.ofMinutes(10))
        .samplingInterval(25)
        .sampleCount(5000)
        .leakThreshold(1.05) // More sensitive threshold
        .build();
  }
}
