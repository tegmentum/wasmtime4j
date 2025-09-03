package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Advanced failure analysis and debugging tools for WebAssembly test execution. Provides detailed
 * failure categorization, root cause analysis, and actionable debugging information.
 */
public final class WasmTestFailureAnalyzer {
  private static final Logger LOGGER = Logger.getLogger(WasmTestFailureAnalyzer.class.getName());

  // Failure pattern categories
  private static final Map<FailureCategory, List<Pattern>> FAILURE_PATTERNS = createFailurePatterns();

  // Analysis cache for performance
  private static final ConcurrentMap<String, TestFailureAnalysis> analysisCache = new ConcurrentHashMap<>();

  private WasmTestFailureAnalyzer() {
    // Utility class - prevent instantiation
  }

  /**
   * Analyzes a test failure and provides detailed debugging information.
   *
   * @param testName the name of the failed test
   * @param execution the failed test execution
   * @return detailed failure analysis
   */
  public static TestFailureAnalysis analyzeFailure(final String testName, 
                                                   final RuntimeTestExecution execution) {
    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(execution, "execution cannot be null");

    if (execution.isSuccessful()) {
      throw new IllegalArgumentException("Cannot analyze successful test execution");
    }

    // Check cache first
    final String cacheKey = createCacheKey(testName, execution);
    final TestFailureAnalysis cached = analysisCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    LOGGER.info("Analyzing failure for test: " + testName);

    final TestFailureAnalysis.Builder analysisBuilder = new TestFailureAnalysis.Builder(testName);
    analysisBuilder.runtimeType(execution.getRuntimeType());
    analysisBuilder.executionTime(execution.getDuration());

    if (execution.isSkipped()) {
      analysisBuilder.category(FailureCategory.SKIPPED);
      analysisBuilder.summary("Test was skipped");
      analysisBuilder.recommendation("Check test execution conditions and runtime availability");
    } else {
      // Analyze the exception and categorize the failure
      final Exception exception = execution.getException();
      if (exception != null) {
        analyzeException(exception, analysisBuilder);
      } else {
        analysisBuilder.category(FailureCategory.UNKNOWN);
        analysisBuilder.summary("Test failed without exception information");
      }
    }

    final TestFailureAnalysis analysis = analysisBuilder.build();
    analysisCache.put(cacheKey, analysis);

    LOGGER.info("Failure analysis completed for " + testName + ": " + analysis.getCategory().name());
    return analysis;
  }

  /**
   * Analyzes failures across multiple runtimes to identify patterns and inconsistencies.
   *
   * @param testName the test name
   * @param executions map of runtime executions
   * @return cross-runtime failure analysis
   */
  public static CrossRuntimeFailureAnalysis analyzeCrossRuntimeFailure(
      final String testName,
      final Map<RuntimeType, RuntimeTestExecution> executions) {
    
    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(executions, "executions cannot be null");

    LOGGER.info("Analyzing cross-runtime failure for test: " + testName);

    final CrossRuntimeFailureAnalysis.Builder analysisBuilder = 
        new CrossRuntimeFailureAnalysis.Builder(testName);

    final Map<RuntimeType, TestFailureAnalysis> runtimeAnalyses = new HashMap<>();
    final List<RuntimeType> successfulRuntimes = new ArrayList<>();
    final List<RuntimeType> failedRuntimes = new ArrayList<>();

    // Analyze each runtime execution
    for (final Map.Entry<RuntimeType, RuntimeTestExecution> entry : executions.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final RuntimeTestExecution execution = entry.getValue();

      if (execution.isSuccessful()) {
        successfulRuntimes.add(runtimeType);
      } else {
        failedRuntimes.add(runtimeType);
        final TestFailureAnalysis analysis = analyzeFailure(testName, execution);
        runtimeAnalyses.put(runtimeType, analysis);
      }
    }

    analysisBuilder.successfulRuntimes(successfulRuntimes);
    analysisBuilder.failedRuntimes(failedRuntimes);
    analysisBuilder.runtimeAnalyses(runtimeAnalyses);

    // Determine inconsistency type
    if (!successfulRuntimes.isEmpty() && !failedRuntimes.isEmpty()) {
      analysisBuilder.inconsistencyType(InconsistencyType.PARTIAL_FAILURE);
      analysisBuilder.summary("Test succeeded on some runtimes but failed on others");
    } else if (failedRuntimes.size() > 1) {
      // Check if failures are consistent across runtimes
      final Set<FailureCategory> failureCategories = runtimeAnalyses.values().stream()
          .map(TestFailureAnalysis::getCategory)
          .collect(Collectors.toSet());

      if (failureCategories.size() == 1) {
        analysisBuilder.inconsistencyType(InconsistencyType.CONSISTENT_FAILURE);
        analysisBuilder.summary("Test failed consistently across all runtimes");
      } else {
        analysisBuilder.inconsistencyType(InconsistencyType.DIFFERENT_FAILURES);
        analysisBuilder.summary("Test failed with different error types across runtimes");
      }
    } else {
      analysisBuilder.inconsistencyType(InconsistencyType.NONE);
      analysisBuilder.summary("Test behavior is consistent across runtimes");
    }

    return analysisBuilder.build();
  }

  /**
   * Generates a comprehensive failure report for all failed tests in a test suite.
   *
   * @param suiteResults the test suite results
   * @return detailed failure report
   */
  public static TestSuiteFailureReport generateFailureReport(final WasmTestSuiteResults suiteResults) {
    Objects.requireNonNull(suiteResults, "suiteResults cannot be null");

    LOGGER.info("Generating failure report for test suite: " + suiteResults.getSuiteType().name());

    final TestSuiteFailureReport.Builder reportBuilder = 
        new TestSuiteFailureReport.Builder(suiteResults.getSuiteType());

    final Map<String, Set<RuntimeType>> failedTests = suiteResults.getFailedTests();
    final Set<String> inconsistentTests = suiteResults.getInconsistentTests();

    reportBuilder.totalTests(suiteResults.getTotalTestsExecuted());
    reportBuilder.failedTestCount(failedTests.size());
    reportBuilder.inconsistentTestCount(inconsistentTests.size());

    // Analyze each failed test
    for (final String testName : failedTests.keySet()) {
      final Map<RuntimeType, RuntimeTestExecution> testExecutions = new HashMap<>();
      
      for (final Map.Entry<RuntimeType, Map<String, RuntimeTestExecution>> entry : 
           suiteResults.getAllRuntimeResults().entrySet()) {
        final RuntimeType runtimeType = entry.getKey();
        final RuntimeTestExecution execution = entry.getValue().get(testName);
        if (execution != null) {
          testExecutions.put(runtimeType, execution);
        }
      }

      if (!testExecutions.isEmpty()) {
        final CrossRuntimeFailureAnalysis analysis = 
            analyzeCrossRuntimeFailure(testName, testExecutions);
        reportBuilder.addFailureAnalysis(analysis);
      }
    }

    return reportBuilder.build();
  }

  /**
   * Analyzes an exception and categorizes the failure type.
   *
   * @param exception the exception to analyze
   * @param analysisBuilder the analysis builder to populate
   */
  private static void analyzeException(final Exception exception, 
                                       final TestFailureAnalysis.Builder analysisBuilder) {
    final String exceptionMessage = exception.getMessage();
    final String exceptionClass = exception.getClass().getSimpleName();
    final String stackTrace = getStackTrace(exception);

    analysisBuilder.exceptionType(exceptionClass);
    analysisBuilder.exceptionMessage(exceptionMessage);
    analysisBuilder.stackTrace(stackTrace);

    // Categorize the failure based on exception patterns
    FailureCategory category = FailureCategory.UNKNOWN;
    String summary = "Unknown failure";
    String recommendation = "Review exception details and stack trace";

    for (final Map.Entry<FailureCategory, List<Pattern>> entry : FAILURE_PATTERNS.entrySet()) {
      final FailureCategory candidateCategory = entry.getKey();
      final List<Pattern> patterns = entry.getValue();

      for (final Pattern pattern : patterns) {
        if (pattern.matcher(exceptionMessage != null ? exceptionMessage : "").find() ||
            pattern.matcher(exceptionClass).find()) {
          category = candidateCategory;
          break;
        }
      }

      if (category != FailureCategory.UNKNOWN) {
        break;
      }
    }

    // Set category-specific summary and recommendations
    switch (category) {
      case COMPILATION_ERROR:
        summary = "WebAssembly module compilation failed";
        recommendation = "Check module validity, ensure WebAssembly binary format is correct";
        break;
      case INSTANTIATION_ERROR:
        summary = "WebAssembly module instantiation failed";
        recommendation = "Verify module imports, check memory/table requirements";
        break;
      case RUNTIME_ERROR:
        summary = "Runtime execution error occurred";
        recommendation = "Check function calls, memory access, and runtime constraints";
        break;
      case VALIDATION_ERROR:
        summary = "WebAssembly module validation failed";
        recommendation = "Ensure module conforms to WebAssembly specification";
        break;
      case MEMORY_ERROR:
        summary = "Memory-related error occurred";
        recommendation = "Check memory allocation, bounds, and cleanup";
        break;
      case TIMEOUT:
        summary = "Test execution timed out";
        recommendation = "Optimize test performance or increase timeout limits";
        break;
      case NATIVE_ERROR:
        summary = "Native library error occurred";
        recommendation = "Check native library installation and compatibility";
        break;
      case CONFIGURATION_ERROR:
        summary = "Configuration or setup error";
        recommendation = "Verify test environment and runtime configuration";
        break;
      default:
        // Keep default values
        break;
    }

    analysisBuilder.category(category);
    analysisBuilder.summary(summary);
    analysisBuilder.recommendation(recommendation);
  }

  /**
   * Creates failure patterns for categorizing exceptions.
   *
   * @return map of failure categories to patterns
   */
  private static Map<FailureCategory, List<Pattern>> createFailurePatterns() {
    final Map<FailureCategory, List<Pattern>> patterns = new HashMap<>();

    patterns.put(FailureCategory.COMPILATION_ERROR, List.of(
        Pattern.compile("compilation.*(failed|error)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("invalid.*module", Pattern.CASE_INSENSITIVE),
        Pattern.compile("CompilationException"),
        Pattern.compile("parse.*error", Pattern.CASE_INSENSITIVE)
    ));

    patterns.put(FailureCategory.INSTANTIATION_ERROR, List.of(
        Pattern.compile("instantiation.*(failed|error)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("import.*not.*found", Pattern.CASE_INSENSITIVE),
        Pattern.compile("InstantiationException"),
        Pattern.compile("link.*error", Pattern.CASE_INSENSITIVE)
    ));

    patterns.put(FailureCategory.RUNTIME_ERROR, List.of(
        Pattern.compile("runtime.*error", Pattern.CASE_INSENSITIVE),
        Pattern.compile("execution.*failed", Pattern.CASE_INSENSITIVE),
        Pattern.compile("RuntimeException"),
        Pattern.compile("trap.*occurred", Pattern.CASE_INSENSITIVE)
    ));

    patterns.put(FailureCategory.VALIDATION_ERROR, List.of(
        Pattern.compile("validation.*(failed|error)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ValidationException"),
        Pattern.compile("invalid.*format", Pattern.CASE_INSENSITIVE)
    ));

    patterns.put(FailureCategory.MEMORY_ERROR, List.of(
        Pattern.compile("memory.*error", Pattern.CASE_INSENSITIVE),
        Pattern.compile("out.*of.*memory", Pattern.CASE_INSENSITIVE),
        Pattern.compile("OutOfMemoryError"),
        Pattern.compile("bounds.*check", Pattern.CASE_INSENSITIVE)
    ));

    patterns.put(FailureCategory.TIMEOUT, List.of(
        Pattern.compile("timeout", Pattern.CASE_INSENSITIVE),
        Pattern.compile("TimeoutException"),
        Pattern.compile("execution.*time.*exceeded", Pattern.CASE_INSENSITIVE)
    ));

    patterns.put(FailureCategory.NATIVE_ERROR, List.of(
        Pattern.compile("native.*library", Pattern.CASE_INSENSITIVE),
        Pattern.compile("JNI.*error", Pattern.CASE_INSENSITIVE),
        Pattern.compile("panama.*error", Pattern.CASE_INSENSITIVE),
        Pattern.compile("UnsatisfiedLinkError")
    ));

    patterns.put(FailureCategory.CONFIGURATION_ERROR, List.of(
        Pattern.compile("configuration.*error", Pattern.CASE_INSENSITIVE),
        Pattern.compile("IllegalStateException"),
        Pattern.compile("setup.*failed", Pattern.CASE_INSENSITIVE)
    ));

    return patterns;
  }

  /**
   * Gets the stack trace as a string.
   *
   * @param exception the exception
   * @return the stack trace string
   */
  private static String getStackTrace(final Exception exception) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (final PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
      exception.printStackTrace(ps);
      return baos.toString(StandardCharsets.UTF_8);
    }
  }

  /**
   * Creates a cache key for failure analysis.
   *
   * @param testName the test name
   * @param execution the execution result
   * @return the cache key
   */
  private static String createCacheKey(final String testName, final RuntimeTestExecution execution) {
    final String exceptionKey = execution.getException() != null ? 
        execution.getException().getClass().getSimpleName() + ":" + execution.getException().getMessage() :
        "no-exception";
    return testName + ":" + execution.getRuntimeType().name() + ":" + exceptionKey;
  }

  /**
   * Clears the analysis cache.
   */
  public static void clearCache() {
    analysisCache.clear();
    LOGGER.info("Cleared failure analysis cache");
  }

  /**
   * Gets cache statistics.
   *
   * @return cache statistics
   */
  public static CacheStatistics getCacheStatistics() {
    return new CacheStatistics(analysisCache.size());
  }

  /**
   * Failure categories for test failures.
   */
  public enum FailureCategory {
    COMPILATION_ERROR("Module compilation failed"),
    INSTANTIATION_ERROR("Module instantiation failed"),
    RUNTIME_ERROR("Runtime execution error"),
    VALIDATION_ERROR("Module validation error"),
    MEMORY_ERROR("Memory-related error"),
    TIMEOUT("Test execution timeout"),
    NATIVE_ERROR("Native library error"),
    CONFIGURATION_ERROR("Configuration or setup error"),
    SKIPPED("Test was skipped"),
    UNKNOWN("Unknown failure type");

    private final String description;

    FailureCategory(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Types of cross-runtime inconsistencies.
   */
  public enum InconsistencyType {
    NONE("No inconsistency detected"),
    PARTIAL_FAILURE("Test succeeded on some runtimes but failed on others"),
    DIFFERENT_FAILURES("Test failed with different error types across runtimes"),
    CONSISTENT_FAILURE("Test failed consistently across all runtimes");

    private final String description;

    InconsistencyType(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Cache statistics for failure analysis.
   */
  public static final class CacheStatistics {
    private final int cacheSize;

    private CacheStatistics(final int cacheSize) {
      this.cacheSize = cacheSize;
    }

    public int getCacheSize() {
      return cacheSize;
    }

    @Override
    public String toString() {
      return String.format("CacheStatistics{size=%d}", cacheSize);
    }
  }
}