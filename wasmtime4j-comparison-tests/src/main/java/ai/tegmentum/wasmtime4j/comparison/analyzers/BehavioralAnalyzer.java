package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Core behavioral analysis engine that performs deep comparison of test execution results across
 * different WebAssembly runtime implementations. Implements sophisticated comparison logic with
 * configurable tolerance levels for different data types.
 *
 * <p>The analyzer follows a Chain of Responsibility pattern for multiple analysis types and uses
 * reflection-based comparison for complex object hierarchies. It supports semantic comparison for
 * equivalent but differently formatted results.
 *
 * @since 1.0.0
 */
public final class BehavioralAnalyzer {
  private static final Logger LOGGER = Logger.getLogger(BehavioralAnalyzer.class.getName());

  // Default tolerance values for different comparison types
  private static final double DEFAULT_FLOATING_POINT_TOLERANCE = 1e-9;
  private static final Duration DEFAULT_TIMING_TOLERANCE = Duration.ofMillis(50);
  private static final int DEFAULT_MAX_REFLECTION_DEPTH = 10;

  // Analysis cache for performance optimization
  private static final Map<String, ComparisonResult> analysisCache = new ConcurrentHashMap<>();

  private final ToleranceConfiguration toleranceConfig;
  private final DiscrepancyDetector discrepancyDetector;
  private final ResultComparator resultComparator;

  /** Creates a new BehavioralAnalyzer with default tolerance configuration. */
  public BehavioralAnalyzer() {
    this(ToleranceConfiguration.defaultConfig());
  }

  /**
   * Creates a new BehavioralAnalyzer with custom tolerance configuration.
   *
   * @param toleranceConfig the tolerance configuration to use
   */
  public BehavioralAnalyzer(final ToleranceConfiguration toleranceConfig) {
    this.toleranceConfig =
        Objects.requireNonNull(toleranceConfig, "toleranceConfig cannot be null");
    this.discrepancyDetector = new DiscrepancyDetector(toleranceConfig);
    this.resultComparator = new ResultComparator(toleranceConfig);
  }

  /**
   * Performs comprehensive behavioral analysis comparing execution results across multiple
   * runtimes.
   *
   * @param testName the name of the test being analyzed
   * @param executionResults map of runtime execution results
   * @return detailed behavioral analysis results
   */
  public BehavioralAnalysisResult analyze(
      final String testName, final Map<RuntimeType, TestExecutionResult> executionResults) {
    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(executionResults, "executionResults cannot be null");

    if (executionResults.isEmpty()) {
      throw new IllegalArgumentException("executionResults cannot be empty");
    }

    LOGGER.info("Starting behavioral analysis for test: " + testName);

    final BehavioralAnalysisResult.Builder resultBuilder =
        new BehavioralAnalysisResult.Builder(testName);

    // Perform pairwise comparison analysis
    final List<RuntimeComparison> pairwiseComparisons =
        performPairwiseComparisons(executionResults);
    resultBuilder.pairwiseComparisons(pairwiseComparisons);

    // Detect behavioral discrepancies
    final List<BehavioralDiscrepancy> discrepancies =
        discrepancyDetector.detectDiscrepancies(executionResults);
    resultBuilder.discrepancies(discrepancies);

    // Analyze execution patterns
    final ExecutionPattern executionPattern = analyzeExecutionPattern(executionResults);
    resultBuilder.executionPattern(executionPattern);

    // Calculate overall consistency score
    final double consistencyScore = calculateConsistencyScore(pairwiseComparisons, discrepancies);
    resultBuilder.consistencyScore(consistencyScore);

    // Determine behavioral verdict
    final BehavioralVerdict verdict =
        determineBehavioralVerdict(consistencyScore, discrepancies, executionPattern);
    resultBuilder.verdict(verdict);

    final BehavioralAnalysisResult result = resultBuilder.build();

    LOGGER.info(
        "Behavioral analysis completed for "
            + testName
            + ": verdict="
            + verdict
            + ", score="
            + String.format("%.2f", consistencyScore));

    return result;
  }

  /**
   * Performs detailed comparison between two specific runtime execution results.
   *
   * @param runtime1 the first runtime type
   * @param result1 the first execution result
   * @param runtime2 the second runtime type
   * @param result2 the second execution result
   * @return detailed comparison result
   */
  public ComparisonResult compareExecutions(
      final RuntimeType runtime1,
      final TestExecutionResult result1,
      final RuntimeType runtime2,
      final TestExecutionResult result2) {
    Objects.requireNonNull(runtime1, "runtime1 cannot be null");
    Objects.requireNonNull(result1, "result1 cannot be null");
    Objects.requireNonNull(runtime2, "runtime2 cannot be null");
    Objects.requireNonNull(result2, "result2 cannot be null");

    final String cacheKey = createComparisonCacheKey(runtime1, result1, runtime2, result2);
    final ComparisonResult cached = analysisCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    LOGGER.fine("Comparing executions: " + runtime1 + " vs " + runtime2);

    final ComparisonResult.Builder resultBuilder = new ComparisonResult.Builder(runtime1, runtime2);

    // Compare execution status
    final boolean statusMatch = compareExecutionStatus(result1, result2);
    resultBuilder.statusMatch(statusMatch);

    // Compare return values if both succeeded
    if (result1.isSuccessful() && result2.isSuccessful()) {
      final ValueComparisonResult valueComparison =
          resultComparator.compareValues(result1.getReturnValue(), result2.getReturnValue());
      resultBuilder.valueComparison(valueComparison);
    }

    // Compare exception information if both failed
    if (!result1.isSuccessful() && !result2.isSuccessful()) {
      final ExceptionComparisonResult exceptionComparison =
          compareExceptions(result1.getException(), result2.getException());
      resultBuilder.exceptionComparison(exceptionComparison);
    }

    // Compare execution timing with tolerance
    final TimingComparisonResult timingComparison =
        compareExecutionTiming(result1.getExecutionTime(), result2.getExecutionTime());
    resultBuilder.timingComparison(timingComparison);

    // Compare memory usage if available
    if (result1.getMemoryUsage().isPresent() && result2.getMemoryUsage().isPresent()) {
      final MemoryComparisonResult memoryComparison =
          compareMemoryUsage(result1.getMemoryUsage().get(), result2.getMemoryUsage().get());
      resultBuilder.memoryComparison(memoryComparison);
    }

    final ComparisonResult result = resultBuilder.build();
    analysisCache.put(cacheKey, result);

    return result;
  }

  /** Performs pairwise comparison analysis across all runtime combinations. */
  private List<RuntimeComparison> performPairwiseComparisons(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    final List<RuntimeComparison> comparisons = new ArrayList<>();
    final RuntimeType[] runtimes = executionResults.keySet().toArray(new RuntimeType[0]);

    for (int i = 0; i < runtimes.length; i++) {
      for (int j = i + 1; j < runtimes.length; j++) {
        final RuntimeType runtime1 = runtimes[i];
        final RuntimeType runtime2 = runtimes[j];
        final TestExecutionResult result1 = executionResults.get(runtime1);
        final TestExecutionResult result2 = executionResults.get(runtime2);

        final ComparisonResult comparisonResult =
            compareExecutions(runtime1, result1, runtime2, result2);

        comparisons.add(new RuntimeComparison(runtime1, runtime2, comparisonResult));
      }
    }

    return comparisons;
  }

  /** Analyzes the execution pattern across all runtimes. */
  private ExecutionPattern analyzeExecutionPattern(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    int successCount = 0;
    int failureCount = 0;
    int skipCount = 0;

    final List<Duration> executionTimes = new ArrayList<>();
    final Set<String> uniqueExceptionTypes = ConcurrentHashMap.newKeySet();
    final Set<Object> uniqueReturnValues = ConcurrentHashMap.newKeySet();

    for (final TestExecutionResult result : executionResults.values()) {
      if (result.isSkipped()) {
        skipCount++;
      } else if (result.isSuccessful()) {
        successCount++;
        executionTimes.add(result.getExecutionTime());
        if (result.getReturnValue() != null) {
          uniqueReturnValues.add(result.getReturnValue());
        }
      } else {
        failureCount++;
        if (result.getException() != null) {
          uniqueExceptionTypes.add(result.getException().getClass().getSimpleName());
        }
      }
    }

    return new ExecutionPattern(
        successCount,
        failureCount,
        skipCount,
        uniqueReturnValues.size(),
        uniqueExceptionTypes.size(),
        calculateTimingVariance(executionTimes));
  }

  /** Calculates variance in execution timing. */
  private double calculateTimingVariance(final List<Duration> executionTimes) {
    if (executionTimes.size() < 2) {
      return 0.0;
    }

    final double mean = executionTimes.stream().mapToLong(Duration::toNanos).average().orElse(0.0);

    final double variance =
        executionTimes.stream()
            .mapToLong(Duration::toNanos)
            .mapToDouble(time -> Math.pow(time - mean, 2))
            .average()
            .orElse(0.0);

    return Math.sqrt(variance) / 1_000_000.0; // Convert to milliseconds
  }

  /** Calculates overall consistency score based on comparisons and discrepancies. */
  private double calculateConsistencyScore(
      final List<RuntimeComparison> comparisons, final List<BehavioralDiscrepancy> discrepancies) {
    if (comparisons.isEmpty()) {
      return 1.0; // Perfect consistency for single runtime
    }

    double totalScore = 0.0;
    for (final RuntimeComparison comparison : comparisons) {
      totalScore += comparison.getComparisonResult().getOverallScore();
    }

    final double baseScore = totalScore / comparisons.size();

    // Apply penalty for discrepancies
    final double discrepancyPenalty = Math.min(0.5, discrepancies.size() * 0.1);

    return Math.max(0.0, baseScore - discrepancyPenalty);
  }

  /** Determines the overall behavioral verdict based on analysis results. */
  private BehavioralVerdict determineBehavioralVerdict(
      final double consistencyScore,
      final List<BehavioralDiscrepancy> discrepancies,
      final ExecutionPattern executionPattern) {

    // Check for critical discrepancies
    final boolean hasCriticalDiscrepancies =
        discrepancies.stream().anyMatch(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL);

    if (hasCriticalDiscrepancies) {
      return BehavioralVerdict.INCOMPATIBLE;
    }

    // Check consistency score thresholds
    if (consistencyScore >= 0.95) {
      return BehavioralVerdict.CONSISTENT;
    } else if (consistencyScore >= 0.80) {
      return BehavioralVerdict.MOSTLY_CONSISTENT;
    } else if (consistencyScore >= 0.60) {
      return BehavioralVerdict.INCONSISTENT;
    } else {
      return BehavioralVerdict.INCOMPATIBLE;
    }
  }

  /** Compares execution status between two results. */
  private boolean compareExecutionStatus(
      final TestExecutionResult result1, final TestExecutionResult result2) {
    return result1.isSuccessful() == result2.isSuccessful()
        && result1.isSkipped() == result2.isSkipped();
  }

  /** Compares exception information between two results. */
  private ExceptionComparisonResult compareExceptions(
      final Exception exception1, final Exception exception2) {
    if (exception1 == null && exception2 == null) {
      return new ExceptionComparisonResult(true, true, true, 1.0);
    }

    if (exception1 == null || exception2 == null) {
      return new ExceptionComparisonResult(false, false, false, 0.0);
    }

    final boolean typeMatch = exception1.getClass().equals(exception2.getClass());
    final boolean messageMatch = Objects.equals(exception1.getMessage(), exception2.getMessage());
    final boolean semanticMatch = analyzeExceptionSemantic(exception1, exception2);

    final double score = calculateExceptionScore(typeMatch, messageMatch, semanticMatch);

    return new ExceptionComparisonResult(typeMatch, messageMatch, semanticMatch, score);
  }

  /** Analyzes semantic similarity between exceptions. */
  private boolean analyzeExceptionSemantic(final Exception exception1, final Exception exception2) {
    // Check if exceptions belong to the same category
    final String category1 = categorizeException(exception1);
    final String category2 = categorizeException(exception2);

    return Objects.equals(category1, category2);
  }

  /** Categorizes an exception into a semantic category. */
  private String categorizeException(final Exception exception) {
    final String className = exception.getClass().getSimpleName().toLowerCase();

    if (className.contains("compilation")) {
      return "compilation";
    } else if (className.contains("instantiation") || className.contains("link")) {
      return "instantiation";
    } else if (className.contains("runtime") || className.contains("trap")) {
      return "runtime";
    } else if (className.contains("validation")) {
      return "validation";
    } else if (className.contains("memory") || className.contains("outofmemory")) {
      return "memory";
    } else if (className.contains("timeout")) {
      return "timeout";
    } else {
      return "other";
    }
  }

  /** Calculates exception comparison score. */
  private double calculateExceptionScore(
      final boolean typeMatch, final boolean messageMatch, final boolean semanticMatch) {
    double score = 0.0;

    if (typeMatch) {
      score += 0.5;
    }
    if (messageMatch) {
      score += 0.3;
    }
    if (semanticMatch) {
      score += 0.2;
    }

    return score;
  }

  /** Compares execution timing between two results. */
  private TimingComparisonResult compareExecutionTiming(
      final Duration time1, final Duration time2) {
    final Duration difference = Duration.ofNanos(Math.abs(time1.toNanos() - time2.toNanos()));
    final boolean withinTolerance = difference.compareTo(toleranceConfig.getTimingTolerance()) <= 0;

    final double ratio =
        (double) Math.max(time1.toNanos(), time2.toNanos())
            / Math.max(1, Math.min(time1.toNanos(), time2.toNanos()));

    return new TimingComparisonResult(difference, withinTolerance, ratio);
  }

  /** Compares memory usage between two results. */
  private MemoryComparisonResult compareMemoryUsage(
      final MemoryUsage memory1, final MemoryUsage memory2) {
    final long heapDifference = Math.abs(memory1.getHeapUsed() - memory2.getHeapUsed());
    final long nonHeapDifference = Math.abs(memory1.getNonHeapUsed() - memory2.getNonHeapUsed());

    final boolean heapWithinTolerance = heapDifference <= toleranceConfig.getMemoryToleranceBytes();
    final boolean nonHeapWithinTolerance =
        nonHeapDifference <= toleranceConfig.getMemoryToleranceBytes();

    return new MemoryComparisonResult(
        heapDifference, nonHeapDifference,
        heapWithinTolerance, nonHeapWithinTolerance);
  }

  /** Creates a cache key for comparison results. */
  private String createComparisonCacheKey(
      final RuntimeType runtime1,
      final TestExecutionResult result1,
      final RuntimeType runtime2,
      final TestExecutionResult result2) {
    return String.format(
        "%s:%s:%s:%s", runtime1.name(), result1.hashCode(), runtime2.name(), result2.hashCode());
  }

  /** Clears the analysis cache. */
  public static void clearCache() {
    analysisCache.clear();
    LOGGER.info("Cleared behavioral analysis cache");
  }

  /**
   * Gets cache statistics.
   *
   * @return cache statistics
   */
  public static CacheStatistics getCacheStatistics() {
    return new CacheStatistics(analysisCache.size());
  }

  /** Cache statistics for behavioral analysis. */
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

  // Placeholder for TestExecutionResult - will be defined based on Task 002 requirements
  /** Represents the result of executing a single test across WebAssembly runtimes. */
  public static final class TestExecutionResult {
    private final boolean successful;
    private final boolean skipped;
    private final Object returnValue;
    private final Exception exception;
    private final Duration executionTime;
    private final Optional<MemoryUsage> memoryUsage;

    /**
     * Creates a new test execution result with the specified execution details.
     *
     * @param successful whether the test execution was successful
     * @param skipped whether the test was skipped
     * @param returnValue the return value from test execution (null if failed/skipped)
     * @param exception the exception thrown during execution (null if successful)
     * @param executionTime the time taken to execute the test
     * @param memoryUsage optional memory usage statistics during execution
     */
    public TestExecutionResult(
        final boolean successful,
        final boolean skipped,
        final Object returnValue,
        final Exception exception,
        final Duration executionTime,
        final MemoryUsage memoryUsage) {
      this.successful = successful;
      this.skipped = skipped;
      this.returnValue = returnValue;
      this.exception = exception;
      this.executionTime = Objects.requireNonNull(executionTime, "executionTime cannot be null");
      this.memoryUsage = Optional.ofNullable(memoryUsage);
    }

    public boolean isSuccessful() {
      return successful;
    }

    public boolean isSkipped() {
      return skipped;
    }

    public Object getReturnValue() {
      return returnValue;
    }

    public Exception getException() {
      return exception;
    }

    public Duration getExecutionTime() {
      return executionTime;
    }

    public Optional<MemoryUsage> getMemoryUsage() {
      return memoryUsage;
    }

    @Override
    public int hashCode() {
      return Objects.hash(successful, skipped, returnValue, exception, executionTime);
    }
  }

  /** Represents memory usage statistics for monitoring resource consumption. */
  public static final class MemoryUsage {
    private final long heapUsed;
    private final long nonHeapUsed;

    public MemoryUsage(final long heapUsed, final long nonHeapUsed) {
      this.heapUsed = heapUsed;
      this.nonHeapUsed = nonHeapUsed;
    }

    public long getHeapUsed() {
      return heapUsed;
    }

    public long getNonHeapUsed() {
      return nonHeapUsed;
    }
  }
}
