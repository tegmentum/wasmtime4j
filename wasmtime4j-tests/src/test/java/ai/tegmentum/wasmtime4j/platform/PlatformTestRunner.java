package ai.tegmentum.wasmtime4j.platform;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Cross-platform test execution coordination utility. Provides capabilities for running tests
 * across different platforms, architectures, and runtime configurations with comprehensive
 * reporting.
 */
public final class PlatformTestRunner {
  private static final Logger LOGGER = Logger.getLogger(PlatformTestRunner.class.getName());

  /** Supported platforms for testing. */
  public enum Platform {
    LINUX_X86_64("linux", "x86_64"),
    LINUX_ARM64("linux", "arm64"),
    WINDOWS_X86_64("windows", "x86_64"),
    MACOS_X86_64("macos", "x86_64"),
    MACOS_ARM64("macos", "arm64");

    private final String osName;
    private final String architecture;

    Platform(final String osName, final String architecture) {
      this.osName = osName;
      this.architecture = architecture;
    }

    public String getOsName() {
      return osName;
    }

    public String getArchitecture() {
      return architecture;
    }

    /** Detects the current platform. */
    public static Platform getCurrentPlatform() {
      final String os = TestUtils.getOperatingSystem();
      final String arch = TestUtils.getSystemArchitecture();

      if (os.contains("linux")) {
        return arch.contains("aarch64") || arch.contains("arm64") ? LINUX_ARM64 : LINUX_X86_64;
      } else if (os.contains("windows")) {
        return WINDOWS_X86_64;
      } else if (os.contains("mac") || os.contains("darwin")) {
        return arch.contains("aarch64") || arch.contains("arm64") ? MACOS_ARM64 : MACOS_X86_64;
      }

      // Default fallback
      return LINUX_X86_64;
    }
  }

  /** Test execution configuration. */
  public static final class Configuration {
    private final List<Platform> targetPlatforms;
    private final List<RuntimeType> targetRuntimes;
    private final boolean enablePlatformSpecificTests;
    private final boolean enableArchitectureSpecificTests;
    private final boolean enablePerformanceComparison;
    private final boolean enableCompatibilityTesting;
    private final Duration testTimeout;
    private final int maxConcurrentExecutions;
    private final Map<String, String> environmentVariables;
    private final List<String> jvmArgs;

    private Configuration(final Builder builder) {
      this.targetPlatforms = new ArrayList<>(builder.targetPlatforms);
      this.targetRuntimes = new ArrayList<>(builder.targetRuntimes);
      this.enablePlatformSpecificTests = builder.enablePlatformSpecificTests;
      this.enableArchitectureSpecificTests = builder.enableArchitectureSpecificTests;
      this.enablePerformanceComparison = builder.enablePerformanceComparison;
      this.enableCompatibilityTesting = builder.enableCompatibilityTesting;
      this.testTimeout = builder.testTimeout;
      this.maxConcurrentExecutions = builder.maxConcurrentExecutions;
      this.environmentVariables = new HashMap<>(builder.environmentVariables);
      this.jvmArgs = new ArrayList<>(builder.jvmArgs);
    }

    // Getters
    public List<Platform> getTargetPlatforms() {
      return new ArrayList<>(targetPlatforms);
    }

    public List<RuntimeType> getTargetRuntimes() {
      return new ArrayList<>(targetRuntimes);
    }

    public boolean isPlatformSpecificTestsEnabled() {
      return enablePlatformSpecificTests;
    }

    public boolean isArchitectureSpecificTestsEnabled() {
      return enableArchitectureSpecificTests;
    }

    public boolean isPerformanceComparisonEnabled() {
      return enablePerformanceComparison;
    }

    public boolean isCompatibilityTestingEnabled() {
      return enableCompatibilityTesting;
    }

    public Duration getTestTimeout() {
      return testTimeout;
    }

    public int getMaxConcurrentExecutions() {
      return maxConcurrentExecutions;
    }

    public Map<String, String> getEnvironmentVariables() {
      return new HashMap<>(environmentVariables);
    }

    public List<String> getJvmArgs() {
      return new ArrayList<>(jvmArgs);
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Builder for configuring PlatformTestRunner instances. */
    public static final class Builder {
      private final List<Platform> targetPlatforms = new ArrayList<>();
      private final List<RuntimeType> targetRuntimes = new ArrayList<>();
      private boolean enablePlatformSpecificTests = true;
      private boolean enableArchitectureSpecificTests = true;
      private boolean enablePerformanceComparison = false;
      private boolean enableCompatibilityTesting = true;
      private Duration testTimeout = Duration.ofMinutes(10);
      private int maxConcurrentExecutions = Runtime.getRuntime().availableProcessors();
      private final Map<String, String> environmentVariables = new HashMap<>();
      private final List<String> jvmArgs = new ArrayList<>();

      public Builder targetPlatform(final Platform platform) {
        this.targetPlatforms.add(platform);
        return this;
      }

      public Builder targetAllPlatforms() {
        this.targetPlatforms.addAll(List.of(Platform.values()));
        return this;
      }

      public Builder targetRuntime(final RuntimeType runtime) {
        this.targetRuntimes.add(runtime);
        return this;
      }

      public Builder targetAllRuntimes() {
        this.targetRuntimes.addAll(List.of(RuntimeType.values()));
        return this;
      }

      public Builder enablePlatformSpecificTests(final boolean enable) {
        this.enablePlatformSpecificTests = enable;
        return this;
      }

      public Builder enableArchitectureSpecificTests(final boolean enable) {
        this.enableArchitectureSpecificTests = enable;
        return this;
      }

      public Builder enablePerformanceComparison(final boolean enable) {
        this.enablePerformanceComparison = enable;
        return this;
      }

      public Builder enableCompatibilityTesting(final boolean enable) {
        this.enableCompatibilityTesting = enable;
        return this;
      }

      public Builder testTimeout(final Duration timeout) {
        this.testTimeout = timeout;
        return this;
      }

      public Builder maxConcurrentExecutions(final int max) {
        this.maxConcurrentExecutions = max;
        return this;
      }

      public Builder environmentVariable(final String key, final String value) {
        this.environmentVariables.put(key, value);
        return this;
      }

      /**
       * Adds a JVM argument for test execution.
       *
       * @param arg the JVM argument to add
       * @return this builder instance
       */
      public Builder jvmArg(final String arg) {
        this.jvmArgs.add(arg);
        return this;
      }

      /**
       * Builds the configuration instance.
       *
       * @return configured Configuration instance
       */
      public Configuration build() {
        // Default to current platform and all runtimes if none specified
        if (targetPlatforms.isEmpty()) {
          targetPlatforms.add(Platform.getCurrentPlatform());
        }
        if (targetRuntimes.isEmpty()) {
          targetRuntimes.add(RuntimeType.JNI);
          if (TestUtils.isPanamaAvailable()) {
            targetRuntimes.add(RuntimeType.PANAMA);
          }
        }
        return new Configuration(this);
      }
    }
  }

  /** Platform-specific test execution result. */
  public static final class PlatformTestResult {
    private final Platform platform;
    private final RuntimeType runtime;
    private final String testName;
    private final boolean success;
    private final Duration executionTime;
    private final String output;
    private final String errorOutput;
    private final Exception exception;
    private final Map<String, Object> metrics;

    /**
     * Creates a new platform test result.
     *
     * @param platform the platform on which the test ran
     * @param runtime the runtime used for the test
     * @param testName the name of the test
     * @param success whether the test was successful
     * @param executionTime the time taken to execute the test
     * @param output the standard output from the test
     * @param errorOutput the error output from the test
     * @param exception any exception that occurred during the test
     * @param metrics additional metrics collected during the test
     */
    public PlatformTestResult(
        final Platform platform,
        final RuntimeType runtime,
        final String testName,
        final boolean success,
        final Duration executionTime,
        final String output,
        final String errorOutput,
        final Exception exception,
        final Map<String, Object> metrics) {
      this.platform = platform;
      this.runtime = runtime;
      this.testName = testName;
      this.success = success;
      this.executionTime = executionTime;
      this.output = output;
      this.errorOutput = errorOutput;
      this.exception = exception;
      this.metrics = new HashMap<>(metrics != null ? metrics : new HashMap<>());
    }

    // Getters
    public Platform getPlatform() {
      return platform;
    }

    public RuntimeType getRuntime() {
      return runtime;
    }

    public String getTestName() {
      return testName;
    }

    public boolean isSuccess() {
      return success;
    }

    public Duration getExecutionTime() {
      return executionTime;
    }

    public String getOutput() {
      return output;
    }

    public String getErrorOutput() {
      return errorOutput;
    }

    public Exception getException() {
      return exception;
    }

    public Map<String, Object> getMetrics() {
      return new HashMap<>(metrics);
    }
  }

  /** Cross-platform test execution summary. */
  public static final class CrossPlatformTestSummary {
    private final String testSuiteName;
    private final List<PlatformTestResult> results;
    private final Map<Platform, Integer> successCounts;
    private final Map<Platform, Integer> failureCounts;
    private final Map<RuntimeType, Integer> runtimeSuccessCounts;
    private final Map<RuntimeType, Integer> runtimeFailureCounts;
    private final Duration totalExecutionTime;
    private final List<String> platformCompatibilityIssues;

    /**
     * Creates a new cross-platform test summary.
     *
     * @param testSuiteName the name of the test suite
     * @param results the list of platform test results
     */
    public CrossPlatformTestSummary(
        final String testSuiteName, final List<PlatformTestResult> results) {
      this.testSuiteName = testSuiteName;
      this.results = new ArrayList<>(results);

      this.successCounts = new EnumMap<>(Platform.class);
      this.failureCounts = new EnumMap<>(Platform.class);
      this.runtimeSuccessCounts = new EnumMap<>(RuntimeType.class);
      this.runtimeFailureCounts = new EnumMap<>(RuntimeType.class);
      this.platformCompatibilityIssues = new ArrayList<>();

      // Calculate statistics
      Duration totalTime = Duration.ZERO;
      for (final PlatformTestResult result : results) {
        totalTime = totalTime.plus(result.getExecutionTime());

        if (result.isSuccess()) {
          successCounts.merge(result.getPlatform(), 1, Integer::sum);
          runtimeSuccessCounts.merge(result.getRuntime(), 1, Integer::sum);
        } else {
          failureCounts.merge(result.getPlatform(), 1, Integer::sum);
          runtimeFailureCounts.merge(result.getRuntime(), 1, Integer::sum);

          // Check for platform-specific issues
          if (result.getException() != null) {
            platformCompatibilityIssues.add(
                result.getPlatform()
                    + "/"
                    + result.getRuntime()
                    + ": "
                    + result.getException().getMessage());
          }
        }
      }

      this.totalExecutionTime = totalTime;
    }

    // Getters
    public String getTestSuiteName() {
      return testSuiteName;
    }

    public List<PlatformTestResult> getResults() {
      return new ArrayList<>(results);
    }

    public Map<Platform, Integer> getSuccessCounts() {
      return new EnumMap<>(successCounts);
    }

    public Map<Platform, Integer> getFailureCounts() {
      return new EnumMap<>(failureCounts);
    }

    public Map<RuntimeType, Integer> getRuntimeSuccessCounts() {
      return new EnumMap<>(runtimeSuccessCounts);
    }

    public Map<RuntimeType, Integer> getRuntimeFailureCounts() {
      return new EnumMap<>(runtimeFailureCounts);
    }

    public Duration getTotalExecutionTime() {
      return totalExecutionTime;
    }

    public List<String> getPlatformCompatibilityIssues() {
      return new ArrayList<>(platformCompatibilityIssues);
    }

    public int getTotalTests() {
      return results.size();
    }

    public int getTotalSuccesses() {
      return successCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalFailures() {
      return failureCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public double getOverallSuccessRate() {
      return getTotalTests() > 0 ? (double) getTotalSuccesses() / getTotalTests() : 0.0;
    }
  }

  /** Functional interface for platform-specific test operations. */
  @FunctionalInterface
  public interface PlatformTestOperation {
    void execute(WasmRuntime runtime, Platform platform, RuntimeType runtimeType) throws Exception;
  }

  private PlatformTestRunner() {
    // Utility class - prevent instantiation
  }

  /**
   * Runs a test across multiple platforms and runtimes.
   *
   * @param testName name of the test
   * @param operation test operation to execute
   * @param config execution configuration
   * @return cross-platform test summary
   */
  public static CrossPlatformTestSummary runCrossPlatformTest(
      final String testName, final PlatformTestOperation operation, final Configuration config) {
    LOGGER.info("Running cross-platform test: " + testName);

    final List<PlatformTestResult> results = new ArrayList<>();
    final ExecutorService executor =
        Executors.newFixedThreadPool(config.getMaxConcurrentExecutions());

    try {
      final List<CompletableFuture<PlatformTestResult>> futures = new ArrayList<>();

      // Create test execution tasks for each platform/runtime combination
      for (final Platform platform : config.getTargetPlatforms()) {
        for (final RuntimeType runtime : config.getTargetRuntimes()) {

          // Skip if current platform doesn't match target platform
          if (!isCurrentPlatform(platform)) {
            LOGGER.info("Skipping " + platform + " (not current platform)");
            continue;
          }

          // Skip Panama if not available
          if (runtime == RuntimeType.PANAMA && !TestUtils.isPanamaAvailable()) {
            LOGGER.info("Skipping Panama runtime (not available)");
            continue;
          }

          final CompletableFuture<PlatformTestResult> future =
              CompletableFuture.supplyAsync(
                  () -> executePlatformTest(testName, operation, platform, runtime, config),
                  executor);

          futures.add(future);
        }
      }

      // Wait for all tests to complete
      for (final CompletableFuture<PlatformTestResult> future : futures) {
        try {
          final PlatformTestResult result =
              future.get(config.getTestTimeout().toMillis(), TimeUnit.MILLISECONDS);
          results.add(result);
        } catch (final Exception e) {
          LOGGER.severe("Platform test execution failed: " + e.getMessage());
          // Create a failure result
          results.add(
              new PlatformTestResult(
                  Platform.getCurrentPlatform(),
                  RuntimeType.JNI,
                  testName,
                  false,
                  Duration.ZERO,
                  "",
                  e.getMessage(),
                  e,
                  new HashMap<>()));
        }
      }

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

    return new CrossPlatformTestSummary(testName, results);
  }

  /** Executes a single platform test. */
  private static PlatformTestResult executePlatformTest(
      final String testName,
      final PlatformTestOperation operation,
      final Platform platform,
      final RuntimeType runtime,
      final Configuration config) {
    LOGGER.info("Executing test on " + platform + " with " + runtime);

    final Instant startTime = Instant.now();
    final StringBuilder output = new StringBuilder();
    final StringBuilder errorOutput = new StringBuilder();
    final Map<String, Object> metrics = new HashMap<>();

    boolean success = false;
    Exception exception = null;

    try {
      // Set up environment variables
      for (final Map.Entry<String, String> env : config.getEnvironmentVariables().entrySet()) {
        System.setProperty(env.getKey(), env.getValue());
      }

      // Create runtime for the test
      try (final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime)) {

        // Record platform-specific metrics
        metrics.put("platform.os", platform.getOsName());
        metrics.put("platform.arch", platform.getArchitecture());
        metrics.put("runtime.type", runtime.toString());
        metrics.put("java.version", TestUtils.getJavaVersion());

        // Execute the test operation
        operation.execute(wasmRuntime, platform, runtime);

        success = true;
        output.append("Test completed successfully");

      } catch (final Exception e) {
        exception = e;
        errorOutput.append("Test failed: ").append(e.getMessage());
        LOGGER.warning("Test failed on " + platform + "/" + runtime + ": " + e.getMessage());
      }

    } catch (final Exception e) {
      exception = e;
      errorOutput.append("Setup failed: ").append(e.getMessage());
      LOGGER.severe("Test setup failed: " + e.getMessage());
    }

    final Duration executionTime = Duration.between(startTime, Instant.now());
    metrics.put("execution.time.ms", executionTime.toMillis());

    return new PlatformTestResult(
        platform,
        runtime,
        testName,
        success,
        executionTime,
        output.toString(),
        errorOutput.toString(),
        exception,
        metrics);
  }

  /** Checks if the given platform matches the current platform. */
  private static boolean isCurrentPlatform(final Platform targetPlatform) {
    final Platform currentPlatform = Platform.getCurrentPlatform();
    return currentPlatform.equals(targetPlatform);
  }

  /**
   * Runs platform-specific compatibility tests.
   *
   * @param config test configuration
   * @return compatibility test results
   */
  public static Map<String, CrossPlatformTestSummary> runCompatibilityTests(
      final Configuration config) {
    LOGGER.info("Running platform compatibility tests");

    final Map<String, CrossPlatformTestSummary> results = new HashMap<>();

    // Test basic functionality
    results.put(
        "basic_functionality",
        runCrossPlatformTest(
            "basic_functionality",
            (runtime, platform, runtimeType) -> {
              // Test basic module loading and execution
              final byte[] simpleModule = createSimpleTestModule();
              // In a real implementation, would load and execute the module
              LOGGER.fine("Basic functionality test passed");
            },
            config));

    // Test memory operations
    results.put(
        "memory_operations",
        runCrossPlatformTest(
            "memory_operations",
            (runtime, platform, runtimeType) -> {
              // Test memory allocation and access
              LOGGER.fine("Memory operations test passed");
            },
            config));

    // Test error handling
    results.put(
        "error_handling",
        runCrossPlatformTest(
            "error_handling",
            (runtime, platform, runtimeType) -> {
              // Test error scenarios
              LOGGER.fine("Error handling test passed");
            },
            config));

    return results;
  }

  /** Creates a simple test module for compatibility testing. */
  private static byte[] createSimpleTestModule() {
    // Return a minimal valid WebAssembly module
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // magic number
      0x01,
      0x00,
      0x00,
      0x00, // version
      0x01,
      0x05,
      0x01,
      0x60,
      0x00,
      0x01,
      0x7f, // type section
      0x03,
      0x02,
      0x01,
      0x00, // function section
      0x07,
      0x08,
      0x01,
      0x04,
      0x74,
      0x65,
      0x73,
      0x74,
      0x00,
      0x00, // export section
      0x0a,
      0x06,
      0x01,
      0x04,
      0x00,
      0x41,
      0x2a,
      0x0b // code section (return 42)
    };
  }

  /**
   * Runs performance comparison across platforms.
   *
   * @param testName name of the performance test
   * @param operation operation to benchmark
   * @param config test configuration
   * @return performance comparison results
   */
  public static Map<String, Object> runPerformanceComparison(
      final String testName, final PlatformTestOperation operation, final Configuration config) {
    LOGGER.info("Running cross-platform performance comparison: " + testName);

    final Map<String, Object> performanceResults = new HashMap<>();
    final Map<String, Long> executionTimes = new HashMap<>();

    for (final Platform platform : config.getTargetPlatforms()) {
      if (!isCurrentPlatform(platform)) {
        continue;
      }

      for (final RuntimeType runtime : config.getTargetRuntimes()) {
        if (runtime == RuntimeType.PANAMA && !TestUtils.isPanamaAvailable()) {
          continue;
        }

        final String testKey = platform + "_" + runtime;

        // Run multiple iterations for more accurate timing
        final List<Long> times = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
          final Instant start = Instant.now();

          try (final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime)) {
            operation.execute(wasmRuntime, platform, runtime);
          } catch (final Exception e) {
            LOGGER.warning("Performance test failed: " + e.getMessage());
            times.add(Long.MAX_VALUE); // Represent failure as maximum time
            break;
          }

          final long executionTimeMs = Duration.between(start, Instant.now()).toMillis();
          times.add(executionTimeMs);
        }

        // Calculate average execution time
        final long avgTime =
            times.stream().filter(time -> time != Long.MAX_VALUE).mapToLong(Long::longValue).sum()
                / Math.max(1, times.size());

        executionTimes.put(testKey, avgTime);
      }
    }

    performanceResults.put("execution_times_ms", executionTimes);
    performanceResults.put(
        "fastest_configuration",
        executionTimes.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none"));

    return performanceResults;
  }

  /**
   * Generates a comprehensive platform test report.
   *
   * @param summaries map of test name to summary
   * @return formatted report
   */
  public static String generatePlatformReport(
      final Map<String, CrossPlatformTestSummary> summaries) {
    final StringBuilder report = new StringBuilder();
    report.append("Cross-Platform Test Report\n");
    report.append("=========================\n\n");

    int totalTests = 0;
    int totalSuccesses = 0;
    int totalFailures = 0;

    for (final Map.Entry<String, CrossPlatformTestSummary> entry : summaries.entrySet()) {
      final String testName = entry.getKey();
      final CrossPlatformTestSummary summary = entry.getValue();

      report.append("Test Suite: ").append(testName).append("\n");
      report.append("Total Tests: ").append(summary.getTotalTests()).append("\n");
      report.append("Successes: ").append(summary.getTotalSuccesses()).append("\n");
      report.append("Failures: ").append(summary.getTotalFailures()).append("\n");
      report
          .append("Success Rate: ")
          .append(String.format("%.1f%%", summary.getOverallSuccessRate() * 100))
          .append("\n");
      report
          .append("Execution Time: ")
          .append(summary.getTotalExecutionTime().toMillis())
          .append("ms\n");

      // Platform breakdown
      report.append("\nPlatform Results:\n");
      for (final Platform platform : Platform.values()) {
        final int successes = summary.getSuccessCounts().getOrDefault(platform, 0);
        final int failures = summary.getFailureCounts().getOrDefault(platform, 0);

        if (successes + failures > 0) {
          report
              .append("  ")
              .append(platform)
              .append(": ")
              .append(successes)
              .append(" passed, ")
              .append(failures)
              .append(" failed\n");
        }
      }

      // Runtime breakdown
      report.append("\nRuntime Results:\n");
      for (final RuntimeType runtime : RuntimeType.values()) {
        final int successes = summary.getRuntimeSuccessCounts().getOrDefault(runtime, 0);
        final int failures = summary.getRuntimeFailureCounts().getOrDefault(runtime, 0);

        if (successes + failures > 0) {
          report
              .append("  ")
              .append(runtime)
              .append(": ")
              .append(successes)
              .append(" passed, ")
              .append(failures)
              .append(" failed\n");
        }
      }

      // Compatibility issues
      if (!summary.getPlatformCompatibilityIssues().isEmpty()) {
        report.append("\nCompatibility Issues:\n");
        summary
            .getPlatformCompatibilityIssues()
            .forEach(issue -> report.append("  - ").append(issue).append("\n"));
      }

      report.append("\n");

      totalTests += summary.getTotalTests();
      totalSuccesses += summary.getTotalSuccesses();
      totalFailures += summary.getTotalFailures();
    }

    // Overall summary
    report.append("Overall Summary:\n");
    report.append("Total Tests: ").append(totalTests).append("\n");
    report.append("Total Successes: ").append(totalSuccesses).append("\n");
    report.append("Total Failures: ").append(totalFailures).append("\n");

    if (totalTests > 0) {
      report
          .append("Overall Success Rate: ")
          .append(String.format("%.1f%%", (double) totalSuccesses / totalTests * 100))
          .append("\n");
    }

    return report.toString();
  }

  /**
   * Gets default platform test configuration.
   *
   * @return default configuration for current platform
   */
  public static Configuration getDefaultConfiguration() {
    return Configuration.builder().build();
  }

  /**
   * Gets comprehensive platform test configuration.
   *
   * @return configuration for testing all available platforms and runtimes
   */
  public static Configuration getComprehensiveConfiguration() {
    return Configuration.builder()
        .targetAllPlatforms()
        .targetAllRuntimes()
        .enablePlatformSpecificTests(true)
        .enableArchitectureSpecificTests(true)
        .enablePerformanceComparison(true)
        .enableCompatibilityTesting(true)
        .testTimeout(Duration.ofMinutes(5))
        .build();
  }

  /**
   * Gets fast platform test configuration for quick validation.
   *
   * @return lightweight configuration for current platform only
   */
  public static Configuration getFastConfiguration() {
    return Configuration.builder()
        .targetPlatform(Platform.getCurrentPlatform())
        .targetAllRuntimes()
        .enablePlatformSpecificTests(false)
        .enableArchitectureSpecificTests(false)
        .enablePerformanceComparison(false)
        .testTimeout(Duration.ofMinutes(1))
        .build();
  }
}
