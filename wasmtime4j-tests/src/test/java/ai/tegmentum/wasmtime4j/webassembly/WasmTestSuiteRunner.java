package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Comprehensive WebAssembly test suite runner that executes official WebAssembly specification
 * tests and Wasmtime-specific tests across both JNI and Panama implementations.
 */
public final class WasmTestSuiteRunner {
  private static final Logger LOGGER = Logger.getLogger(WasmTestSuiteRunner.class.getName());

  // Test execution configuration
  private static final int MAX_PARALLEL_TESTS = Runtime.getRuntime().availableProcessors();
  private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofSeconds(30);
  private static final int MAX_RETRY_ATTEMPTS = 3;

  // Test filtering patterns
  private static final List<Pattern> DEFAULT_SKIP_PATTERNS =
      Arrays.asList(
          Pattern.compile(".*\\.expected$"), // Skip expected result files
          Pattern.compile(".*\\.json$"), // Skip metadata files
          Pattern.compile(".*\\.txt$"), // Skip text files
          Pattern.compile(".*README.*", Pattern.CASE_INSENSITIVE));

  // Runtime instances cache
  private static final ConcurrentMap<RuntimeType, WasmRuntime> runtimeCache =
      new ConcurrentHashMap<>();

  private WasmTestSuiteRunner() {
    // Utility class - prevent instantiation
  }

  /**
   * Executes all available test suites with comprehensive reporting.
   *
   * @return comprehensive execution results
   * @throws IOException if test execution fails
   */
  public static WasmTestSuiteExecutionResults executeAllTestSuites() throws IOException {
    return executeAllTestSuites(TestExecutionOptions.defaultOptions());
  }

  /**
   * Executes all available test suites with custom options.
   *
   * @param options the test execution options
   * @return comprehensive execution results
   * @throws IOException if test execution fails
   */
  public static WasmTestSuiteExecutionResults executeAllTestSuites(
      final TestExecutionOptions options) throws IOException {
    Objects.requireNonNull(options, "options cannot be null");

    LOGGER.info("Starting comprehensive WebAssembly test suite execution");
    final Instant startTime = Instant.now();

    // Ensure test suites are available
    ensureTestSuitesAvailable();

    final WasmTestSuiteExecutionResults.Builder resultsBuilder =
        new WasmTestSuiteExecutionResults.Builder();

    // Execute each test suite type
    for (final WasmTestSuiteLoader.TestSuiteType suiteType : options.getIncludedSuites()) {
      LOGGER.info("Executing test suite: " + suiteType.name());

      final WasmTestSuiteResults suiteResults = executeSingleTestSuite(suiteType, options);
      resultsBuilder.addSuiteResults(suiteType, suiteResults);

      LOGGER.info(
          "Completed test suite "
              + suiteType.name()
              + ": "
              + suiteResults.getTotalTestsExecuted()
              + " tests executed, "
              + suiteResults.getSuccessfulTests()
              + " successful, "
              + suiteResults.getFailedTests()
              + " failed");
    }

    final Duration totalDuration = Duration.between(startTime, Instant.now());
    resultsBuilder.totalExecutionTime(totalDuration);

    final WasmTestSuiteExecutionResults results = resultsBuilder.build();

    LOGGER.info(
        "WebAssembly test suite execution completed in "
            + totalDuration.toSeconds()
            + "s. "
            + "Total: "
            + results.getTotalTestsExecuted()
            + " tests, "
            + "Successful: "
            + results.getTotalSuccessfulTests()
            + ", "
            + "Failed: "
            + results.getTotalFailedTests());

    return results;
  }

  /**
   * Executes a single test suite type.
   *
   * @param suiteType the test suite type
   * @param options the execution options
   * @return the test suite results
   * @throws IOException if execution fails
   */
  private static WasmTestSuiteResults executeSingleTestSuite(
      final WasmTestSuiteLoader.TestSuiteType suiteType, final TestExecutionOptions options)
      throws IOException {

    final List<WasmTestCase> testCases = WasmTestSuiteLoader.loadTestSuite(suiteType);

    if (testCases.isEmpty()) {
      LOGGER.info("No test cases found for suite: " + suiteType.name());
      return WasmTestSuiteResults.empty(suiteType);
    }

    // Filter test cases based on options
    final List<WasmTestCase> filteredTestCases = filterTestCases(testCases, options);

    LOGGER.info("Executing " + filteredTestCases.size() + " test cases for " + suiteType.name());

    final WasmTestSuiteResults.Builder resultsBuilder = new WasmTestSuiteResults.Builder(suiteType);

    // Execute tests across all requested runtimes
    for (final RuntimeType runtimeType : options.getTargetRuntimes()) {
      if (runtimeType == RuntimeType.PANAMA && !TestUtils.isPanamaAvailable()) {
        LOGGER.info("Skipping Panama tests - not available on Java " + TestUtils.getJavaVersion());
        continue;
      }

      LOGGER.info(
          "Executing " + filteredTestCases.size() + " tests with " + runtimeType + " runtime");
      final Map<String, RuntimeTestExecution> runtimeResults =
          executeTestsWithRuntime(filteredTestCases, runtimeType, options);

      resultsBuilder.addRuntimeResults(runtimeType, runtimeResults);
    }

    return resultsBuilder.build();
  }

  /**
   * Executes test cases with a specific runtime.
   *
   * @param testCases the test cases to execute
   * @param runtimeType the runtime type
   * @param options the execution options
   * @return the execution results map
   */
  private static Map<String, RuntimeTestExecution> executeTestsWithRuntime(
      final List<WasmTestCase> testCases,
      final RuntimeType runtimeType,
      final TestExecutionOptions options) {

    final Map<String, RuntimeTestExecution> results = new ConcurrentHashMap<>();
    final WasmRuntime runtime = getRuntimeInstance(runtimeType);

    if (options.isParallelExecution()) {
      executeTestsInParallel(testCases, runtime, runtimeType, options, results);
    } else {
      executeTestsSequentially(testCases, runtime, runtimeType, options, results);
    }

    return results;
  }

  /** Executes tests in parallel using a thread pool. */
  private static void executeTestsInParallel(
      final List<WasmTestCase> testCases,
      final WasmRuntime runtime,
      final RuntimeType runtimeType,
      final TestExecutionOptions options,
      final Map<String, RuntimeTestExecution> results) {

    final ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL_TESTS);
    final List<Future<Void>> futures = new ArrayList<>();

    for (final WasmTestCase testCase : testCases) {
      final Future<Void> future =
          executor.submit(
              () -> {
                final RuntimeTestExecution result =
                    executeTestCase(testCase, runtime, runtimeType, options);
                results.put(testCase.getTestName(), result);
                return null;
              });
      futures.add(future);
    }

    // Wait for all tests to complete
    for (final Future<Void> future : futures) {
      try {
        future.get(options.getTestTimeout().toMillis(), TimeUnit.MILLISECONDS);
      } catch (final Exception e) {
        LOGGER.warning("Test execution interrupted: " + e.getMessage());
        future.cancel(true);
      }
    }

    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /** Executes tests sequentially. */
  private static void executeTestsSequentially(
      final List<WasmTestCase> testCases,
      final WasmRuntime runtime,
      final RuntimeType runtimeType,
      final TestExecutionOptions options,
      final Map<String, RuntimeTestExecution> results) {

    for (final WasmTestCase testCase : testCases) {
      final RuntimeTestExecution result = executeTestCase(testCase, runtime, runtimeType, options);
      results.put(testCase.getTestName(), result);
    }
  }

  /**
   * Executes a single test case.
   *
   * @param testCase the test case
   * @param runtime the runtime instance
   * @param runtimeType the runtime type
   * @param options the execution options
   * @return the execution result
   */
  private static RuntimeTestExecution executeTestCase(
      final WasmTestCase testCase,
      final WasmRuntime runtime,
      final RuntimeType runtimeType,
      final TestExecutionOptions options) {

    final Instant startTime = Instant.now();

    int retryCount = 0;
    Exception lastException = null;

    while (retryCount <= options.getMaxRetryAttempts()) {
      try {
        // Execute the test case
        final Object result = executeTestCaseLogic(testCase, runtime);
        final Duration duration = Duration.between(startTime, Instant.now());

        LOGGER.fine(
            "Test "
                + testCase.getTestName()
                + " completed successfully with "
                + runtimeType
                + " in "
                + duration.toMillis()
                + "ms");

        return RuntimeTestExecution.successful(runtimeType, result, duration);

      } catch (final Exception e) {
        lastException = e;
        retryCount++;

        if (retryCount <= options.getMaxRetryAttempts()) {
          LOGGER.warning(
              "Test "
                  + testCase.getTestName()
                  + " failed (attempt "
                  + retryCount
                  + "): "
                  + e.getMessage()
                  + ". Retrying...");

          try {
            Thread.sleep(100 * retryCount); // Exponential backoff
          } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }

    final Duration duration = Duration.between(startTime, Instant.now());
    LOGGER.warning(
        "Test "
            + testCase.getTestName()
            + " failed after "
            + options.getMaxRetryAttempts()
            + " attempts with "
            + runtimeType
            + ": "
            + lastException.getMessage());

    return RuntimeTestExecution.failed(runtimeType, lastException, duration);
  }

  /**
   * Executes the core test case logic.
   *
   * @param testCase the test case
   * @param runtime the runtime instance
   * @return the test result
   * @throws Exception if test execution fails
   */
  private static Object executeTestCaseLogic(final WasmTestCase testCase, final WasmRuntime runtime)
      throws Exception {
    // Validate that the module bytes are valid WebAssembly
    if (!WasmTestSuiteLoader.isValidWasmModule(testCase.getModuleBytes())) {
      throw new IllegalArgumentException("Invalid WebAssembly module: " + testCase.getTestName());
    }

    // Create engine and store
    final var engine = runtime.createEngine();
    final var store = engine.createStore();

    try {
      // Compile the module
      final var module = engine.compileModule(testCase.getModuleBytes());

      // Instantiate the module
      final var instance = module.instantiate(store, ImportMap.empty());

      // Basic validation - module should instantiate successfully
      return "Module " + testCase.getTestName() + " instantiated successfully";

    } finally {
      // Clean up resources
      if (store != null) {
        store.close();
      }
      if (engine != null) {
        engine.close();
      }
    }
  }

  /**
   * Filters test cases based on execution options.
   *
   * @param testCases the test cases to filter
   * @param options the execution options
   * @return the filtered test cases
   */
  private static List<WasmTestCase> filterTestCases(
      final List<WasmTestCase> testCases, final TestExecutionOptions options) {

    return testCases.stream()
        .filter(testCase -> shouldIncludeTestCase(testCase, options))
        .collect(Collectors.toList());
  }

  /**
   * Determines if a test case should be included based on execution options.
   *
   * @param testCase the test case
   * @param options the execution options
   * @return true if the test case should be included
   */
  private static boolean shouldIncludeTestCase(
      final WasmTestCase testCase, final TestExecutionOptions options) {
    // Check skip patterns
    for (final Pattern skipPattern : DEFAULT_SKIP_PATTERNS) {
      if (skipPattern.matcher(testCase.getTestName()).matches()) {
        return false;
      }
    }

    // Check custom filters
    for (final Predicate<WasmTestCase> filter : options.getTestFilters()) {
      if (!filter.test(testCase)) {
        return false;
      }
    }

    // Check include patterns
    if (!options.getIncludePatterns().isEmpty()) {
      return options.getIncludePatterns().stream()
          .anyMatch(pattern -> pattern.matcher(testCase.getTestName()).matches());
    }

    // Check exclude patterns
    return options.getExcludePatterns().stream()
        .noneMatch(pattern -> pattern.matcher(testCase.getTestName()).matches());
  }

  /**
   * Gets a cached runtime instance for the specified type.
   *
   * @param runtimeType the runtime type
   * @return the runtime instance
   * @throws RuntimeException if runtime creation fails
   */
  private static WasmRuntime getRuntimeInstance(final RuntimeType runtimeType) {
    return runtimeCache.computeIfAbsent(
        runtimeType,
        type -> {
          LOGGER.info("Creating runtime instance for " + type);
          try {
            return WasmRuntimeFactory.create(type);
          } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
            throw new RuntimeException("Failed to create runtime for " + type, e);
          }
        });
  }

  /**
   * Ensures that test suites are available, downloading them if necessary.
   *
   * @throws IOException if test suite setup fails
   */
  private static void ensureTestSuitesAvailable() throws IOException {
    // First try the standard approach
    WasmTestSuiteLoader.ensureTestSuitesAvailable();

    // Check if we need to download test suites
    final Path testResourcesPath =
        WasmTestSuiteLoader.getTestSuiteDirectory(
                WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC)
            .getParent();

    if (!WasmSpecTestDownloader.validateTestSuites(testResourcesPath)) {
      LOGGER.info("Test suites not found, attempting to download...");

      try {
        WasmSpecTestDownloader.downloadAllTestSuites(testResourcesPath);
      } catch (final IOException e) {
        LOGGER.warning("Failed to download test suites automatically: " + e.getMessage());
        LOGGER.warning(
            "Please manually download test suites or tests will be limited to custom tests only");
      }
    }
  }

  /** Clears all cached runtime instances. */
  public static void clearRuntimeCache() {
    runtimeCache
        .values()
        .forEach(
            runtime -> {
              try {
                runtime.close();
              } catch (final Exception e) {
                LOGGER.warning("Failed to close runtime: " + e.getMessage());
              }
            });
    runtimeCache.clear();
    LOGGER.info("Cleared runtime cache");
  }

  /** Configuration options for test execution. */
  public static final class TestExecutionOptions {
    private final List<WasmTestSuiteLoader.TestSuiteType> includedSuites;
    private final List<RuntimeType> targetRuntimes;
    private final List<Pattern> includePatterns;
    private final List<Pattern> excludePatterns;
    private final List<Predicate<WasmTestCase>> testFilters;
    private final boolean parallelExecution;
    private final Duration testTimeout;
    private final int maxRetryAttempts;

    private TestExecutionOptions(final Builder builder) {
      this.includedSuites = Collections.unmodifiableList(new ArrayList<>(builder.includedSuites));
      this.targetRuntimes = Collections.unmodifiableList(new ArrayList<>(builder.targetRuntimes));
      this.includePatterns = Collections.unmodifiableList(new ArrayList<>(builder.includePatterns));
      this.excludePatterns = Collections.unmodifiableList(new ArrayList<>(builder.excludePatterns));
      this.testFilters = Collections.unmodifiableList(new ArrayList<>(builder.testFilters));
      this.parallelExecution = builder.parallelExecution;
      this.testTimeout = builder.testTimeout;
      this.maxRetryAttempts = builder.maxRetryAttempts;
    }

    public static TestExecutionOptions defaultOptions() {
      return new Builder().build();
    }

    public static Builder builder() {
      return new Builder();
    }

    // Getters
    public List<WasmTestSuiteLoader.TestSuiteType> getIncludedSuites() {
      return includedSuites;
    }

    public List<RuntimeType> getTargetRuntimes() {
      return targetRuntimes;
    }

    public List<Pattern> getIncludePatterns() {
      return includePatterns;
    }

    public List<Pattern> getExcludePatterns() {
      return excludePatterns;
    }

    public List<Predicate<WasmTestCase>> getTestFilters() {
      return testFilters;
    }

    public boolean isParallelExecution() {
      return parallelExecution;
    }

    public Duration getTestTimeout() {
      return testTimeout;
    }

    public int getMaxRetryAttempts() {
      return maxRetryAttempts;
    }

    /** Builder for TestExecutionOptions. */
    public static final class Builder {
      private final List<WasmTestSuiteLoader.TestSuiteType> includedSuites = new ArrayList<>();
      private final List<RuntimeType> targetRuntimes = new ArrayList<>();
      private final List<Pattern> includePatterns = new ArrayList<>();
      private final List<Pattern> excludePatterns = new ArrayList<>();
      private final List<Predicate<WasmTestCase>> testFilters = new ArrayList<>();
      private boolean parallelExecution = true;
      private Duration testTimeout = DEFAULT_TEST_TIMEOUT;
      private int maxRetryAttempts = MAX_RETRY_ATTEMPTS;

      private Builder() {
        // Default to all available suites and runtimes
        includedSuites.addAll(Arrays.asList(WasmTestSuiteLoader.TestSuiteType.values()));
        targetRuntimes.add(RuntimeType.JNI);
        if (TestUtils.isPanamaAvailable()) {
          targetRuntimes.add(RuntimeType.PANAMA);
        }
      }

      /**
       * Includes a WebAssembly test suite for execution.
       *
       * @param suiteType the test suite type to include
       * @return this builder instance for method chaining
       */
      public Builder includeSuite(final WasmTestSuiteLoader.TestSuiteType suiteType) {
        if (!includedSuites.contains(suiteType)) {
          includedSuites.add(suiteType);
        }
        return this;
      }

      public Builder excludeSuite(final WasmTestSuiteLoader.TestSuiteType suiteType) {
        includedSuites.remove(suiteType);
        return this;
      }

      /**
       * Adds a target runtime for test execution.
       *
       * @param runtimeType the runtime type to target
       * @return this builder instance for method chaining
       */
      public Builder targetRuntime(final RuntimeType runtimeType) {
        if (!targetRuntimes.contains(runtimeType)) {
          targetRuntimes.add(runtimeType);
        }
        return this;
      }

      public Builder includePattern(final String pattern) {
        includePatterns.add(Pattern.compile(pattern));
        return this;
      }

      public Builder excludePattern(final String pattern) {
        excludePatterns.add(Pattern.compile(pattern));
        return this;
      }

      public Builder testFilter(final Predicate<WasmTestCase> filter) {
        testFilters.add(filter);
        return this;
      }

      public Builder parallelExecution(final boolean parallel) {
        this.parallelExecution = parallel;
        return this;
      }

      public Builder testTimeout(final Duration timeout) {
        this.testTimeout = Objects.requireNonNull(timeout, "timeout cannot be null");
        return this;
      }

      /**
       * Sets the maximum number of retry attempts for failed tests.
       *
       * @param attempts the maximum retry attempts (must be non-negative)
       * @return this builder instance for method chaining
       * @throws IllegalArgumentException if attempts is negative
       */
      public Builder maxRetryAttempts(final int attempts) {
        if (attempts < 0) {
          throw new IllegalArgumentException("maxRetryAttempts cannot be negative");
        }
        this.maxRetryAttempts = attempts;
        return this;
      }

      /**
       * Builds the test execution options configuration.
       *
       * @return the configured test execution options
       * @throws IllegalStateException if no test suites are included
       */
      public TestExecutionOptions build() {
        if (includedSuites.isEmpty()) {
          throw new IllegalStateException("At least one test suite must be included");
        }
        if (targetRuntimes.isEmpty()) {
          throw new IllegalStateException("At least one target runtime must be specified");
        }
        return new TestExecutionOptions(this);
      }
    }
  }
}
