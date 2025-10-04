package ai.tegmentum.wasmtime4j.comparison.runners;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ToleranceConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Comprehensive cross-runtime validation test runner that executes tests across multiple
 * WebAssembly runtime implementations (JNI and Panama) and performs detailed behavioral analysis to
 * ensure >98% consistency and zero functional discrepancies.
 *
 * <p>This runner implements parallel test execution, comprehensive result comparison, and detailed
 * reporting to validate runtime equivalence for production readiness.
 *
 * @since 1.0.0
 */
public final class CrossRuntimeValidationRunner {
  private static final Logger LOGGER =
      Logger.getLogger(CrossRuntimeValidationRunner.class.getName());

  // Validation execution parameters
  private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofMinutes(5);
  private static final Duration DEFAULT_RUNNER_TIMEOUT = Duration.ofHours(2);

  // Consistency thresholds for production readiness
  private static final double PRODUCTION_CONSISTENCY_THRESHOLD = 0.98;
  private static final double ZERO_DISCREPANCY_THRESHOLD = 1.0;

  private final BehavioralAnalyzer behavioralAnalyzer;
  private final ExecutorService executorService;
  private final ValidationConfiguration validationConfig;
  private final Map<String, CrossRuntimeTestResult> testResults;

  /**
   * Creates a new CrossRuntimeValidationRunner with specified configuration.
   *
   * @param validationConfig the validation configuration to use
   */
  public CrossRuntimeValidationRunner(final ValidationConfiguration validationConfig) {
    this.validationConfig =
        Objects.requireNonNull(validationConfig, "validationConfig cannot be null");
    this.behavioralAnalyzer = new BehavioralAnalyzer(validationConfig.getToleranceConfiguration());
    this.executorService = Executors.newFixedThreadPool(validationConfig.getThreadPoolSize());
    this.testResults = new ConcurrentHashMap<>();
  }

  /**
   * Executes comprehensive cross-runtime validation across all specified test suites.
   *
   * @param testSuites the test suites to execute
   * @return comprehensive validation results
   */
  public CrossRuntimeValidationResults executeValidation(
      final List<CrossRuntimeTestSuite> testSuites) {
    Objects.requireNonNull(testSuites, "testSuites cannot be null");

    LOGGER.info("Starting cross-runtime validation with " + testSuites.size() + " test suites");

    final Instant startTime = Instant.now();
    final CrossRuntimeValidationResults.Builder resultsBuilder =
        new CrossRuntimeValidationResults.Builder();

    try {
      // Execute test suites in parallel
      final List<CompletableFuture<TestSuiteValidationResult>> futures =
          testSuites.stream()
              .map(
                  testSuite ->
                      CompletableFuture.supplyAsync(
                          () -> executeTestSuite(testSuite), executorService))
              .collect(Collectors.toList());

      // Wait for all test suites to complete
      final List<TestSuiteValidationResult> suiteResults =
          futures.stream()
              .map(
                  future -> {
                    try {
                      return future.get(
                          validationConfig.getRunnerTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    } catch (final Exception e) {
                      LOGGER.severe("Test suite execution failed: " + e.getMessage());
                      return createFailedSuiteResult(e);
                    }
                  })
              .collect(Collectors.toList());

      // Aggregate results and perform overall analysis
      final OverallValidationResult overallResult = aggregateValidationResults(suiteResults);

      final Duration totalDuration = Duration.between(startTime, Instant.now());

      return resultsBuilder
          .suiteResults(suiteResults)
          .overallResult(overallResult)
          .executionDuration(totalDuration)
          .startTime(startTime)
          .endTime(Instant.now())
          .build();

    } catch (final Exception e) {
      LOGGER.severe("Cross-runtime validation failed: " + e.getMessage());
      return resultsBuilder.executionFailed(true).failureReason(e.getMessage()).build();
    }
  }

  /** Executes a single test suite across all configured runtimes. */
  private TestSuiteValidationResult executeTestSuite(final CrossRuntimeTestSuite testSuite) {
    LOGGER.info("Executing test suite: " + testSuite.getName());

    final TestSuiteValidationResult.Builder resultBuilder =
        new TestSuiteValidationResult.Builder(testSuite.getName());

    final List<CrossRuntimeTestResult> testResults = new ArrayList<>();

    try {
      // Execute each test across all runtimes
      for (final CrossRuntimeTest test : testSuite.getTests()) {
        final CrossRuntimeTestResult testResult = executeTest(test);
        testResults.add(testResult);
        this.testResults.put(test.getName(), testResult);
      }

      // Analyze suite-level consistency
      final SuiteConsistencyAnalysis suiteAnalysis = analyzeSuiteConsistency(testResults);

      return resultBuilder
          .testResults(testResults)
          .suiteAnalysis(suiteAnalysis)
          .executionSuccessful(true)
          .build();

    } catch (final Exception e) {
      LOGGER.warning(
          "Test suite execution failed: " + testSuite.getName() + " - " + e.getMessage());
      return resultBuilder
          .executionSuccessful(false)
          .failureReason(e.getMessage())
          .testResults(testResults)
          .build();
    }
  }

  /** Executes a single test across all configured runtimes. */
  private CrossRuntimeTestResult executeTest(final CrossRuntimeTest test) {
    LOGGER.fine("Executing cross-runtime test: " + test.getName());

    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
        new ConcurrentHashMap<>();

    // Execute test on each runtime
    for (final RuntimeType runtime : validationConfig.getTargetRuntimes()) {
      try {
        final BehavioralAnalyzer.TestExecutionResult result = executeTestOnRuntime(test, runtime);
        executionResults.put(runtime, result);
      } catch (final Exception e) {
        LOGGER.warning("Test execution failed on " + runtime + ": " + e.getMessage());
        final BehavioralAnalyzer.TestExecutionResult failedResult =
            createFailedExecutionResult(e, runtime);
        executionResults.put(runtime, failedResult);
      }
    }

    // Perform behavioral analysis
    final BehavioralAnalysisResult behavioralAnalysis =
        behavioralAnalyzer.analyze(test.getName(), executionResults);

    return new CrossRuntimeTestResult(
        test.getName(),
        executionResults,
        behavioralAnalysis,
        calculateTestConsistencyScore(behavioralAnalysis),
        meetsProductionRequirements(behavioralAnalysis));
  }

  /** Executes a test on a specific runtime. */
  private BehavioralAnalyzer.TestExecutionResult executeTestOnRuntime(
      final CrossRuntimeTest test, final RuntimeType runtime) {
    final Instant startTime = Instant.now();

    try {
      // Execute the test logic
      final Object result = test.execute(runtime);
      final Duration executionTime = Duration.between(startTime, Instant.now());

      return new BehavioralAnalyzer.TestExecutionResult.Builder()
          .successful(true)
          .returnValue(result)
          .executionTime(executionTime)
          .runtime(runtime)
          .build();

    } catch (final Exception e) {
      final Duration executionTime = Duration.between(startTime, Instant.now());

      return new BehavioralAnalyzer.TestExecutionResult.Builder()
          .successful(false)
          .exception(e)
          .executionTime(executionTime)
          .runtime(runtime)
          .build();
    }
  }

  /** Creates a failed execution result for error cases. */
  private BehavioralAnalyzer.TestExecutionResult createFailedExecutionResult(
      final Exception exception, final RuntimeType runtime) {
    return new BehavioralAnalyzer.TestExecutionResult.Builder()
        .successful(false)
        .exception(exception)
        .executionTime(Duration.ZERO)
        .runtime(runtime)
        .build();
  }

  /** Analyzes consistency across all tests in a test suite. */
  private SuiteConsistencyAnalysis analyzeSuiteConsistency(
      final List<CrossRuntimeTestResult> testResults) {
    final SuiteConsistencyAnalysis.Builder analysisBuilder = new SuiteConsistencyAnalysis.Builder();

    final double averageConsistency =
        testResults.stream()
            .mapToDouble(CrossRuntimeTestResult::getConsistencyScore)
            .average()
            .orElse(0.0);

    final long productionReadyTests =
        testResults.stream()
            .mapToLong(result -> result.meetsProductionRequirements() ? 1 : 0)
            .sum();

    final double productionReadinessRate =
        testResults.isEmpty() ? 0.0 : (double) productionReadyTests / testResults.size();

    return analysisBuilder
        .averageConsistencyScore(averageConsistency)
        .productionReadinessRate(productionReadinessRate)
        .totalTests(testResults.size())
        .productionReadyTests((int) productionReadyTests)
        .build();
  }

  /** Aggregates validation results from all test suites. */
  private OverallValidationResult aggregateValidationResults(
      final List<TestSuiteValidationResult> suiteResults) {
    final OverallValidationResult.Builder resultBuilder = new OverallValidationResult.Builder();

    final int totalTests =
        suiteResults.stream().mapToInt(suite -> suite.getTestResults().size()).sum();

    final int totalSuites = suiteResults.size();

    final double overallConsistencyScore =
        suiteResults.stream()
            .flatMap(suite -> suite.getTestResults().stream())
            .mapToDouble(CrossRuntimeTestResult::getConsistencyScore)
            .average()
            .orElse(0.0);

    final long productionReadyTests =
        suiteResults.stream()
            .flatMap(suite -> suite.getTestResults().stream())
            .mapToLong(result -> result.meetsProductionRequirements() ? 1 : 0)
            .sum();

    final boolean meetsProductionRequirements =
        overallConsistencyScore >= PRODUCTION_CONSISTENCY_THRESHOLD
            && (totalTests == 0
                || (double) productionReadyTests / totalTests >= PRODUCTION_CONSISTENCY_THRESHOLD);

    final boolean achievesZeroDiscrepancy =
        overallConsistencyScore >= ZERO_DISCREPANCY_THRESHOLD && productionReadyTests == totalTests;

    return resultBuilder
        .totalTests(totalTests)
        .totalSuites(totalSuites)
        .overallConsistencyScore(overallConsistencyScore)
        .productionReadyTests((int) productionReadyTests)
        .meetsProductionRequirements(meetsProductionRequirements)
        .achievesZeroDiscrepancy(achievesZeroDiscrepancy)
        .build();
  }

  /** Creates a failed test suite result for error cases. */
  private TestSuiteValidationResult createFailedSuiteResult(final Exception exception) {
    return new TestSuiteValidationResult.Builder("FAILED_SUITE")
        .executionSuccessful(false)
        .failureReason(exception.getMessage())
        .testResults(List.of())
        .build();
  }

  /** Calculates consistency score for a single test. */
  private double calculateTestConsistencyScore(final BehavioralAnalysisResult analysis) {
    return analysis.getConsistencyScore();
  }

  /** Checks if a test meets production requirements. */
  private boolean meetsProductionRequirements(final BehavioralAnalysisResult analysis) {
    return analysis.meetsProductionRequirements();
  }

  /** Shuts down the validation runner and releases resources. */
  public void shutdown() {
    LOGGER.info("Shutting down cross-runtime validation runner");

    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (final InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /** Configuration for cross-runtime validation execution. */
  public static final class ValidationConfiguration {
    private final ToleranceConfiguration toleranceConfiguration;
    private final Set<RuntimeType> targetRuntimes;
    private final int threadPoolSize;
    private final Duration testTimeout;
    private final Duration runnerTimeout;
    private final boolean strictValidation;

    private ValidationConfiguration(final Builder builder) {
      this.toleranceConfiguration = builder.toleranceConfiguration;
      this.targetRuntimes = EnumSet.copyOf(builder.targetRuntimes);
      this.threadPoolSize = builder.threadPoolSize;
      this.testTimeout = builder.testTimeout;
      this.runnerTimeout = builder.runnerTimeout;
      this.strictValidation = builder.strictValidation;
    }

    public ToleranceConfiguration getToleranceConfiguration() {
      return toleranceConfiguration;
    }

    public Set<RuntimeType> getTargetRuntimes() {
      return targetRuntimes;
    }

    public int getThreadPoolSize() {
      return threadPoolSize;
    }

    public Duration getTestTimeout() {
      return testTimeout;
    }

    public Duration getRunnerTimeout() {
      return runnerTimeout;
    }

    public boolean isStrictValidation() {
      return strictValidation;
    }

    /** Builder for ValidationConfiguration. */
    public static final class Builder {
      private ToleranceConfiguration toleranceConfiguration =
          ToleranceConfiguration.defaultConfiguration();
      private Set<RuntimeType> targetRuntimes = EnumSet.of(RuntimeType.JNI, RuntimeType.PANAMA);
      private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
      private Duration testTimeout = DEFAULT_TEST_TIMEOUT;
      private Duration runnerTimeout = DEFAULT_RUNNER_TIMEOUT;
      private boolean strictValidation = true;

      public Builder toleranceConfiguration(final ToleranceConfiguration toleranceConfiguration) {
        this.toleranceConfiguration = Objects.requireNonNull(toleranceConfiguration);
        return this;
      }

      public Builder targetRuntimes(final Set<RuntimeType> targetRuntimes) {
        this.targetRuntimes = Objects.requireNonNull(targetRuntimes);
        return this;
      }

      /**
       * Sets the thread pool size for parallel test execution.
       *
       * @param threadPoolSize number of threads in the pool (must be positive)
       * @return this builder instance
       */
      public Builder threadPoolSize(final int threadPoolSize) {
        if (threadPoolSize <= 0) {
          throw new IllegalArgumentException("threadPoolSize must be positive");
        }
        this.threadPoolSize = threadPoolSize;
        return this;
      }

      public Builder testTimeout(final Duration testTimeout) {
        this.testTimeout = Objects.requireNonNull(testTimeout);
        return this;
      }

      public Builder runnerTimeout(final Duration runnerTimeout) {
        this.runnerTimeout = Objects.requireNonNull(runnerTimeout);
        return this;
      }

      public Builder strictValidation(final boolean strictValidation) {
        this.strictValidation = strictValidation;
        return this;
      }

      public ValidationConfiguration build() {
        return new ValidationConfiguration(this);
      }
    }
  }
}
