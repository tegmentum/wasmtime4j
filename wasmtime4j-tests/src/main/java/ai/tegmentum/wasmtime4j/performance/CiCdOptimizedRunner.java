package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Specialized test runner optimized for CI/CD environments.
 *
 * <p>This runner implements aggressive optimizations specifically for continuous integration:
 *
 * <ul>
 *   <li>Aggressive caching with build-to-build persistence
 *   <li>Fast-fail strategies for critical issues
 *   <li>Resource-aware execution based on CI environment
 *   <li>Parallel test matrix execution for different configurations
 *   <li>Comprehensive reporting for CI dashboard integration
 *   <li>Automatic performance regression detection
 * </ul>
 *
 * <p>The runner automatically detects CI environments and adjusts execution strategy to optimize
 * for both speed and reliability in automated build pipelines.
 */
public final class CiCdOptimizedRunner {
  private static final Logger LOGGER = Logger.getLogger(CiCdOptimizedRunner.class.getName());

  // CI/CD specific constants
  private static final Duration CI_TARGET_TIME = Duration.ofMinutes(25); // Aggressive target for CI
  private static final Duration CI_WARNING_TIME = Duration.ofMinutes(20);
  private static final double CI_MINIMUM_SUCCESS_RATE = 98.0; // Higher standard for CI
  private static final int CI_MAX_RETRIES = 2;
  private static final Path CI_CACHE_BASE =
      Paths.get(System.getProperty("user.home"), ".wasmtime4j-ci-cache");

  // CI environment detection
  private static final Map<String, String> CI_ENVIRONMENT_VARS =
      Map.of(
          "GITHUB_ACTIONS", "GitHub Actions",
          "JENKINS_URL", "Jenkins",
          "TRAVIS", "Travis CI",
          "CIRCLECI", "CircleCI",
          "GITLAB_CI", "GitLab CI",
          "BUILDKITE", "Buildkite",
          "CI", "Generic CI");

  private final CiEnvironment ciEnvironment;
  private final CiConfiguration configuration;
  private final AtomicReference<ExecutionPhase> currentPhase;

  /** Detected CI environment information. */
  public static final class CiEnvironment {
    private final String name;
    private final boolean isCI;
    private final int availableParallelism;
    private final long availableMemoryMB;
    private final String buildId;
    private final String branchName;
    private final Map<String, String> environmentInfo;

    public CiEnvironment(
        final String name,
        final boolean isCI,
        final int availableParallelism,
        final long availableMemoryMB,
        final String buildId,
        final String branchName,
        final Map<String, String> environmentInfo) {
      this.name = name;
      this.isCI = isCI;
      this.availableParallelism = availableParallelism;
      this.availableMemoryMB = availableMemoryMB;
      this.buildId = buildId;
      this.branchName = branchName;
      this.environmentInfo = environmentInfo;
    }

    public String getName() {
      return name;
    }

    public boolean isCI() {
      return isCI;
    }

    public int getAvailableParallelism() {
      return availableParallelism;
    }

    public long getAvailableMemoryMB() {
      return availableMemoryMB;
    }

    public String getBuildId() {
      return buildId;
    }

    public String getBranchName() {
      return branchName;
    }

    public Map<String, String> getEnvironmentInfo() {
      return environmentInfo;
    }

    @Override
    public String toString() {
      return String.format(
          "CiEnvironment{name='%s', isCI=%s, parallelism=%d, memory=%dMB, build='%s', branch='%s'}",
          name, isCI, availableParallelism, availableMemoryMB, buildId, branchName);
    }
  }

  /** CI-specific configuration optimizations. */
  public static final class CiConfiguration {
    private final boolean enableAggressiveCaching;
    private final boolean enableFastFail;
    private final boolean enablePerformanceRegression;
    private final Duration targetExecutionTime;
    private final double minimumSuccessRate;
    private final int maxRetries;
    private final TestExecutionStrategy strategy;
    private final Path cacheDirectory;

    private CiConfiguration(final Builder builder) {
      this.enableAggressiveCaching = builder.enableAggressiveCaching;
      this.enableFastFail = builder.enableFastFail;
      this.enablePerformanceRegression = builder.enablePerformanceRegression;
      this.targetExecutionTime = builder.targetExecutionTime;
      this.minimumSuccessRate = builder.minimumSuccessRate;
      this.maxRetries = builder.maxRetries;
      this.strategy = builder.strategy;
      this.cacheDirectory = builder.cacheDirectory;
    }

    public boolean isAggressiveCachingEnabled() {
      return enableAggressiveCaching;
    }

    public boolean isFastFailEnabled() {
      return enableFastFail;
    }

    public boolean isPerformanceRegressionEnabled() {
      return enablePerformanceRegression;
    }

    public Duration getTargetExecutionTime() {
      return targetExecutionTime;
    }

    public double getMinimumSuccessRate() {
      return minimumSuccessRate;
    }

    public int getMaxRetries() {
      return maxRetries;
    }

    public TestExecutionStrategy getStrategy() {
      return strategy;
    }

    public Path getCacheDirectory() {
      return cacheDirectory;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private boolean enableAggressiveCaching = true;
      private boolean enableFastFail = true;
      private boolean enablePerformanceRegression = true;
      private Duration targetExecutionTime = CI_TARGET_TIME;
      private double minimumSuccessRate = CI_MINIMUM_SUCCESS_RATE;
      private int maxRetries = CI_MAX_RETRIES;
      private TestExecutionStrategy strategy = TestExecutionStrategy.BALANCED;
      private Path cacheDirectory = CI_CACHE_BASE;

      public Builder enableAggressiveCaching(final boolean enable) {
        this.enableAggressiveCaching = enable;
        return this;
      }

      public Builder enableFastFail(final boolean enable) {
        this.enableFastFail = enable;
        return this;
      }

      public Builder enablePerformanceRegression(final boolean enable) {
        this.enablePerformanceRegression = enable;
        return this;
      }

      public Builder targetExecutionTime(final Duration target) {
        this.targetExecutionTime = target;
        return this;
      }

      public Builder minimumSuccessRate(final double rate) {
        this.minimumSuccessRate = rate;
        return this;
      }

      public Builder maxRetries(final int retries) {
        this.maxRetries = retries;
        return this;
      }

      public Builder strategy(final TestExecutionStrategy strategy) {
        this.strategy = strategy;
        return this;
      }

      public Builder cacheDirectory(final Path directory) {
        this.cacheDirectory = directory;
        return this;
      }

      public CiConfiguration build() {
        return new CiConfiguration(this);
      }
    }
  }

  /** Test execution strategies for different CI scenarios. */
  public enum TestExecutionStrategy {
    /** Fast execution for pull request validation. */
    FAST_PR_CHECK,
    /** Comprehensive testing for main branch. */
    FULL_COVERAGE,
    /** Balanced approach for regular CI builds. */
    BALANCED,
    /** Regression-focused testing for release branches. */
    REGRESSION_FOCUSED
  }

  /** Execution phases for tracking progress. */
  public enum ExecutionPhase {
    INITIALIZING,
    ENVIRONMENT_DETECTION,
    CACHE_PREPARATION,
    TEST_SCHEDULING,
    TEST_EXECUTION,
    RESULT_ANALYSIS,
    REPORTING,
    CLEANUP,
    COMPLETED,
    FAILED
  }

  /** Comprehensive CI execution results. */
  public static final class CiExecutionResults {
    private final OptimizedTestExecutor.ExecutionResults executionResults;
    private final CiEnvironment environment;
    private final boolean regressionDetected;
    private final Duration actualExecutionTime;
    private final String ciReport;
    private final boolean passedCiCriteria;

    public CiExecutionResults(
        final OptimizedTestExecutor.ExecutionResults executionResults,
        final CiEnvironment environment,
        final boolean regressionDetected,
        final String ciReport,
        final boolean passedCiCriteria) {
      this.executionResults = executionResults;
      this.environment = environment;
      this.regressionDetected = regressionDetected;
      this.actualExecutionTime = executionResults.getTotalExecutionTime();
      this.ciReport = ciReport;
      this.passedCiCriteria = passedCiCriteria;
    }

    public OptimizedTestExecutor.ExecutionResults getExecutionResults() {
      return executionResults;
    }

    public CiEnvironment getEnvironment() {
      return environment;
    }

    public boolean isRegressionDetected() {
      return regressionDetected;
    }

    public Duration getActualExecutionTime() {
      return actualExecutionTime;
    }

    public String getCiReport() {
      return ciReport;
    }

    public boolean hasPassedCiCriteria() {
      return passedCiCriteria;
    }

    public int getCiExitCode() {
      return passedCiCriteria ? 0 : 1;
    }
  }

  /**
   * Creates a new CI/CD optimized runner.
   *
   * @param configuration CI configuration
   */
  public CiCdOptimizedRunner(final CiConfiguration configuration) {
    this.configuration = configuration;
    this.ciEnvironment = detectCiEnvironment();
    this.currentPhase = new AtomicReference<>(ExecutionPhase.INITIALIZING);

    LOGGER.info("Initialized CI/CD optimized runner");
    LOGGER.info("Detected environment: " + ciEnvironment);
    LOGGER.info(
        "Configuration: target="
            + configuration.getTargetExecutionTime()
            + ", strategy="
            + configuration.getStrategy()
            + ", aggressive_cache="
            + configuration.isAggressiveCachingEnabled());
  }

  /**
   * Creates a runner with optimal CI/CD configuration.
   *
   * @return CI-optimized runner
   */
  public static CiCdOptimizedRunner createOptimal() {
    final CiEnvironment env = detectCiEnvironment();
    final CiConfiguration config =
        CiConfiguration.builder()
            .strategy(determineOptimalStrategy(env))
            .targetExecutionTime(env.isCI() ? CI_TARGET_TIME : Duration.ofMinutes(30))
            .build();

    return new CiCdOptimizedRunner(config);
  }

  /**
   * Executes tests with CI/CD optimizations.
   *
   * @return comprehensive CI execution results
   * @throws IOException if execution fails
   */
  public CiExecutionResults executeForCi() throws IOException {
    final Instant executionStart = Instant.now();
    boolean passedCriteria = false;
    boolean regressionDetected = false;
    OptimizedTestExecutor.ExecutionResults executionResults = null;

    try {
      currentPhase.set(ExecutionPhase.ENVIRONMENT_DETECTION);
      logPhaseStart("Environment Detection");
      validateCiEnvironment();

      currentPhase.set(ExecutionPhase.CACHE_PREPARATION);
      logPhaseStart("Cache Preparation");
      prepareCiCache();

      currentPhase.set(ExecutionPhase.TEST_SCHEDULING);
      logPhaseStart("Test Scheduling");
      final OptimizedTestExecutor executor = createOptimizedExecutor();

      currentPhase.set(ExecutionPhase.TEST_EXECUTION);
      logPhaseStart("Test Execution");
      executionResults = executeTestsWithStrategy(executor);

      currentPhase.set(ExecutionPhase.RESULT_ANALYSIS);
      logPhaseStart("Result Analysis");
      regressionDetected = analyzeForRegression(executionResults);

      passedCriteria = validateCiCriteria(executionResults, regressionDetected);

      currentPhase.set(ExecutionPhase.REPORTING);
      logPhaseStart("Reporting");
      final String ciReport = generateCiReport(executionResults, regressionDetected);

      currentPhase.set(ExecutionPhase.CLEANUP);
      logPhaseStart("Cleanup");
      performCiCleanup();

      currentPhase.set(ExecutionPhase.COMPLETED);
      final Duration totalTime = Duration.between(executionStart, Instant.now());

      LOGGER.info(
          String.format(
              "CI execution completed in %s. Passed criteria: %s", totalTime, passedCriteria));

      return new CiExecutionResults(
          executionResults, ciEnvironment, regressionDetected, ciReport, passedCriteria);

    } catch (final Exception e) {
      currentPhase.set(ExecutionPhase.FAILED);
      LOGGER.severe("CI execution failed: " + e.getMessage());

      // Create failure results
      if (executionResults == null) {
        executionResults = createEmptyResults();
      }

      final String failureReport = generateFailureReport(e);
      return new CiExecutionResults(executionResults, ciEnvironment, false, failureReport, false);
    }
  }

  /**
   * Gets the current execution phase.
   *
   * @return current execution phase
   */
  public ExecutionPhase getCurrentPhase() {
    return currentPhase.get();
  }

  /**
   * Generates a CI-friendly summary report.
   *
   * @param results execution results
   * @return CI summary report
   */
  public String generateCiSummary(final CiExecutionResults results) {
    final StringBuilder summary = new StringBuilder();

    summary.append("# Wasmtime4j CI Execution Summary\n\n");

    // Overall status
    summary.append("## Overall Status\n");
    summary.append(
        String.format(
            "- **Status**: %s\n", results.hasPassedCiCriteria() ? "✅ PASSED" : "❌ FAILED"));
    summary.append(
        String.format(
            "- **Execution Time**: %s (target: %s)\n",
            formatDuration(results.getActualExecutionTime()),
            formatDuration(configuration.getTargetExecutionTime())));
    summary.append(String.format("- **Environment**: %s\n", results.getEnvironment().getName()));
    summary.append(String.format("- **Build ID**: %s\n", results.getEnvironment().getBuildId()));
    summary.append("\n");

    // Performance metrics
    final OptimizedTestExecutor.ExecutionResults execResults = results.getExecutionResults();
    summary.append("## Performance Metrics\n");
    summary.append(String.format("- **Tests Executed**: %d\n", execResults.getTestsExecuted()));
    summary.append(String.format("- **Tests Cached**: %d\n", execResults.getTestsSkipped()));
    summary.append(String.format("- **Success Rate**: %.1f%%\n", execResults.getSuccessRate()));
    summary.append(String.format("- **Cache Hits**: %d\n", execResults.getCacheHits()));
    summary.append(
        String.format(
            "- **Execution Efficiency**: %.1f%%\n", execResults.getExecutionEfficiency()));
    summary.append("\n");

    // Regression analysis
    if (configuration.isPerformanceRegressionEnabled()) {
      summary.append("## Regression Analysis\n");
      summary.append(
          String.format(
              "- **Regression Detected**: %s\n",
              results.isRegressionDetected() ? "⚠️ YES" : "✅ NO"));
      summary.append("\n");
    }

    // Resource utilization
    summary.append("## Resource Utilization\n");
    summary.append(
        String.format(
            "- **Parallelism Used**: %d cores\n",
            results.getEnvironment().getAvailableParallelism()));
    summary.append(
        String.format(
            "- **Memory Available**: %d MB\n", results.getEnvironment().getAvailableMemoryMB()));
    summary.append("\n");

    return summary.toString();
  }

  // Private implementation methods

  private static CiEnvironment detectCiEnvironment() {
    final Map<String, String> env = System.getenv();
    final Properties props = System.getProperties();

    String ciName = "Local Development";
    boolean isCI = false;

    // Detect CI environment
    for (final Map.Entry<String, String> entry : CI_ENVIRONMENT_VARS.entrySet()) {
      if (env.containsKey(entry.getKey())) {
        ciName = entry.getValue();
        isCI = true;
        break;
      }
    }

    // Calculate available resources
    final int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors());
    final long memoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);

    // Extract build information
    final String buildId =
        env.getOrDefault(
            "BUILD_ID",
            env.getOrDefault("GITHUB_RUN_ID", env.getOrDefault("TRAVIS_BUILD_ID", "unknown")));

    final String branchName =
        env.getOrDefault(
            "BRANCH_NAME",
            env.getOrDefault("GITHUB_REF_NAME", env.getOrDefault("TRAVIS_BRANCH", "unknown")));

    final Map<String, String> environmentInfo =
        Map.of(
            "java.version", props.getProperty("java.version", "unknown"),
            "os.name", props.getProperty("os.name", "unknown"),
            "os.arch", props.getProperty("os.arch", "unknown"),
            "ci.detected", String.valueOf(isCI));

    return new CiEnvironment(
        ciName, isCI, parallelism, memoryMB, buildId, branchName, environmentInfo);
  }

  private static TestExecutionStrategy determineOptimalStrategy(final CiEnvironment env) {
    if (!env.isCI()) {
      return TestExecutionStrategy.BALANCED;
    }

    // Determine strategy based on branch and environment
    final String branch = env.getBranchName().toLowerCase();
    if (branch.contains("main") || branch.contains("master")) {
      return TestExecutionStrategy.FULL_COVERAGE;
    } else if (branch.contains("release")) {
      return TestExecutionStrategy.REGRESSION_FOCUSED;
    } else {
      return TestExecutionStrategy.FAST_PR_CHECK;
    }
  }

  private void validateCiEnvironment() {
    if (ciEnvironment.getAvailableMemoryMB() < 1024) {
      LOGGER.warning("Low memory detected: " + ciEnvironment.getAvailableMemoryMB() + "MB");
    }

    if (ciEnvironment.getAvailableParallelism() < 2) {
      LOGGER.warning("Limited parallelism: " + ciEnvironment.getAvailableParallelism() + " cores");
    }

    LOGGER.info("CI environment validation completed");
  }

  private void prepareCiCache() throws IOException {
    if (!configuration.isAggressiveCachingEnabled()) {
      return;
    }

    final Path cacheDir = configuration.getCacheDirectory();

    // Create cache directory structure
    Files.createDirectories(cacheDir);

    // Clean old cache entries if needed
    cleanOldCacheEntries(cacheDir);

    LOGGER.info("CI cache preparation completed: " + cacheDir);
  }

  private void cleanOldCacheEntries(final Path cacheDir) {
    try {
      final Instant cutoff = Instant.now().minus(Duration.ofDays(7));

      Files.walk(cacheDir)
          .filter(Files::isRegularFile)
          .filter(
              path -> {
                try {
                  return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                } catch (final IOException e) {
                  return false;
                }
              })
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (final IOException e) {
                  LOGGER.fine("Failed to delete old cache file: " + path);
                }
              });

    } catch (final IOException e) {
      LOGGER.warning("Failed to clean old cache entries: " + e.getMessage());
    }
  }

  private OptimizedTestExecutor createOptimizedExecutor() {
    // Create aggressive cache configuration for CI
    final TestResultCache.CacheConfiguration cacheConfig =
        TestResultCache.CacheConfiguration.builder()
            .enabled(configuration.isAggressiveCachingEnabled())
            .persistToDisk(true)
            .cacheDirectory(configuration.getCacheDirectory())
            .cacheTtl(Duration.ofDays(1)) // Shorter TTL for CI
            .maxSizeMB(500) // Reasonable size for CI
            .build();

    // Create scheduler configuration optimized for CI
    final IntelligentTestScheduler.SchedulerConfiguration schedulerConfig =
        IntelligentTestScheduler.SchedulerConfiguration.builder()
            .maxParallelism(ciEnvironment.getAvailableParallelism())
            .priorityStrategy(selectPriorityStrategy())
            .enableAdaptiveScheduling(true)
            .enableMemoryManagement(true)
            .enableLoadBalancing(true)
            .maxTestTimeout(Duration.ofMinutes(3)) // Aggressive timeout for CI
            .build();

    // Create executor configuration
    final OptimizedTestExecutor.ExecutorConfiguration executorConfig =
        OptimizedTestExecutor.ExecutorConfiguration.builder()
            .enableCaching(configuration.isAggressiveCachingEnabled())
            .enableMemoryManagement(true)
            .enablePerformanceMonitoring(true)
            .enableIncrementalExecution(true)
            .targetExecutionTime(configuration.getTargetExecutionTime())
            .maxParallelism(ciEnvironment.getAvailableParallelism())
            .cacheConfig(cacheConfig)
            .schedulerConfig(schedulerConfig)
            .build();

    return new OptimizedTestExecutor(executorConfig);
  }

  private IntelligentTestScheduler.TestPriorityStrategy selectPriorityStrategy() {
    switch (configuration.getStrategy()) {
      case FAST_PR_CHECK:
        return IntelligentTestScheduler.TestPriorityStrategy.FASTEST_FIRST;
      case FULL_COVERAGE:
        return IntelligentTestScheduler.TestPriorityStrategy.BALANCED;
      case REGRESSION_FOCUSED:
        return IntelligentTestScheduler.TestPriorityStrategy.COMPLEXITY_BASED;
      case BALANCED:
      default:
        return IntelligentTestScheduler.TestPriorityStrategy.BALANCED;
    }
  }

  private OptimizedTestExecutor.ExecutionResults executeTestsWithStrategy(
      final OptimizedTestExecutor executor) throws IOException {

    final List<WasmTestSuiteLoader.TestSuiteType> suites = selectTestSuites();

    LOGGER.info(
        String.format(
            "Executing %d test suites with strategy: %s",
            suites.size(), configuration.getStrategy()));

    return executor.executeTestSuites(suites);
  }

  private List<WasmTestSuiteLoader.TestSuiteType> selectTestSuites() {
    switch (configuration.getStrategy()) {
      case FAST_PR_CHECK:
        return Arrays.asList(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);
      case FULL_COVERAGE:
        return Arrays.asList(WasmTestSuiteLoader.TestSuiteType.values());
      case REGRESSION_FOCUSED:
        return Arrays.asList(
            WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS,
            WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC);
      case BALANCED:
      default:
        return Arrays.asList(
            WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS,
            WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC);
    }
  }

  private boolean analyzeForRegression(final OptimizedTestExecutor.ExecutionResults results) {
    if (!configuration.isPerformanceRegressionEnabled()) {
      return false;
    }

    // Simple regression detection based on execution time
    final Duration actualTime = results.getTotalExecutionTime();
    final Duration warningThreshold = CI_WARNING_TIME;

    if (actualTime.compareTo(warningThreshold) > 0) {
      LOGGER.warning(
          String.format("Performance regression detected: %s > %s", actualTime, warningThreshold));
      return true;
    }

    return false;
  }

  private boolean validateCiCriteria(
      final OptimizedTestExecutor.ExecutionResults results, final boolean regressionDetected) {
    boolean passed = true;

    // Check execution time
    if (!results.isTargetTimeMet()) {
      LOGGER.severe("Failed CI criteria: execution time exceeded target");
      passed = false;
    }

    // Check success rate
    if (results.getSuccessRate() < configuration.getMinimumSuccessRate()) {
      LOGGER.severe(
          String.format(
              "Failed CI criteria: success rate %.1f%% < %.1f%%",
              results.getSuccessRate(), configuration.getMinimumSuccessRate()));
      passed = false;
    }

    // Check for regression
    if (regressionDetected && configuration.isPerformanceRegressionEnabled()) {
      LOGGER.severe("Failed CI criteria: performance regression detected");
      passed = false;
    }

    return passed;
  }

  private String generateCiReport(
      final OptimizedTestExecutor.ExecutionResults results, final boolean regressionDetected) {
    final StringBuilder report = new StringBuilder();

    report.append("CI Execution Report\n");
    report.append("==================\n\n");

    report.append("Environment: ").append(ciEnvironment.getName()).append("\n");
    report.append("Strategy: ").append(configuration.getStrategy()).append("\n");
    report
        .append("Target Time: ")
        .append(formatDuration(configuration.getTargetExecutionTime()))
        .append("\n");
    report
        .append("Actual Time: ")
        .append(formatDuration(results.getTotalExecutionTime()))
        .append("\n");
    report
        .append("Success Rate: ")
        .append(String.format("%.1f%%", results.getSuccessRate()))
        .append("\n");
    report.append("Regression: ").append(regressionDetected ? "DETECTED" : "NONE").append("\n");
    report.append("\n");

    report.append(results.getPerformanceSummary());

    return report.toString();
  }

  private String generateFailureReport(final Exception e) {
    return String.format(
        "CI Execution Failed\n==================\n\nError: %s\nPhase: %s\n",
        e.getMessage(), currentPhase.get());
  }

  private OptimizedTestExecutor.ExecutionResults createEmptyResults() {
    return new OptimizedTestExecutor.ExecutionResults(
        List.of(),
        Duration.ZERO,
        0,
        0,
        0,
        new TestResultCache(TestResultCache.CacheConfiguration.builder().enabled(false).build())
            .getStatistics(),
        "Execution failed");
  }

  private void performCiCleanup() {
    // Cleanup temporary files, optimize cache, etc.
    LOGGER.info("CI cleanup completed");
  }

  private void logPhaseStart(final String phaseName) {
    LOGGER.info(String.format("Starting phase: %s", phaseName));
  }

  private String formatDuration(final Duration duration) {
    return String.format("%d:%02d", duration.toMinutes(), duration.toSecondsPart());
  }
}
