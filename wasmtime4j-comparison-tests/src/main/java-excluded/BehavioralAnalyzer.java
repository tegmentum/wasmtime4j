package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
   * Performs comprehensive behavioral analysis comparing execution results across multiple runtimes
   * with enhanced cross-runtime validation capabilities.
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

    LOGGER.info("Starting enhanced behavioral analysis for test: " + testName);

    final BehavioralAnalysisResult.Builder resultBuilder =
        new BehavioralAnalysisResult.Builder(testName);

    // Perform pairwise comparison analysis
    final List<RuntimeComparison> pairwiseComparisons =
        performPairwiseComparisons(executionResults);
    resultBuilder.pairwiseComparisons(pairwiseComparisons);

    // Detect behavioral discrepancies with enhanced detection
    final List<BehavioralDiscrepancy> discrepancies =
        discrepancyDetector.detectDiscrepancies(executionResults);
    resultBuilder.discrepancies(discrepancies);

    // Analyze execution patterns
    final ExecutionPattern executionPattern = analyzeExecutionPattern(executionResults);
    resultBuilder.executionPattern(executionPattern);

    // Perform enhanced cross-runtime validation
    final CrossRuntimeValidationResult crossRuntimeValidation =
        performCrossRuntimeValidation(executionResults);
    resultBuilder.crossRuntimeValidation(crossRuntimeValidation);

    // Validate execution path consistency
    final ExecutionPathValidationResult executionPathValidation =
        validateExecutionPaths(executionResults);
    resultBuilder.executionPathValidation(executionPathValidation);

    // Detect side effects across runtimes
    final SideEffectAnalysisResult sideEffectAnalysis = analyzeSideEffects(executionResults);
    resultBuilder.sideEffectAnalysis(sideEffectAnalysis);

    // Calculate enhanced consistency score
    final double consistencyScore =
        calculateEnhancedConsistencyScore(
            pairwiseComparisons,
            discrepancies,
            crossRuntimeValidation,
            executionPathValidation,
            sideEffectAnalysis);
    resultBuilder.consistencyScore(consistencyScore);

    // Determine behavioral verdict with enhanced criteria
    final BehavioralVerdict verdict =
        determineBehavioralVerdict(
            consistencyScore, discrepancies, executionPattern, crossRuntimeValidation);
    resultBuilder.verdict(verdict);

    final BehavioralAnalysisResult result = resultBuilder.build();

    LOGGER.info(
        "Enhanced behavioral analysis completed for "
            + testName
            + ": verdict="
            + verdict
            + ", score="
            + String.format("%.2f", consistencyScore)
            + ", cross-runtime consistency="
            + String.format("%.2f", crossRuntimeValidation.getConsistencyScore()));

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

  /** Determines the overall behavioral verdict based on enhanced analysis results. */
  private BehavioralVerdict determineBehavioralVerdict(
      final double consistencyScore,
      final List<BehavioralDiscrepancy> discrepancies,
      final ExecutionPattern executionPattern,
      final CrossRuntimeValidationResult crossRuntimeValidation) {

    // Check for critical discrepancies
    final boolean hasCriticalDiscrepancies =
        discrepancies.stream().anyMatch(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL);

    if (hasCriticalDiscrepancies) {
      return BehavioralVerdict.INCOMPATIBLE;
    }

    // Check cross-runtime validation results
    if (crossRuntimeValidation.getConsistencyScore() < 0.98) {
      // Enhanced threshold for cross-runtime consistency
      return BehavioralVerdict.INCOMPATIBLE;
    }

    // Enhanced consistency score thresholds for >98% requirement
    if (consistencyScore >= 0.98) {
      return BehavioralVerdict.CONSISTENT;
    } else if (consistencyScore >= 0.95) {
      return BehavioralVerdict.MOSTLY_CONSISTENT;
    } else if (consistencyScore >= 0.85) {
      return BehavioralVerdict.INCONSISTENT;
    } else {
      return BehavioralVerdict.INCOMPATIBLE;
    }
  }

  /** Legacy method for backward compatibility. */
  private BehavioralVerdict determineBehavioralVerdict(
      final double consistencyScore,
      final List<BehavioralDiscrepancy> discrepancies,
      final ExecutionPattern executionPattern) {
    // Default to empty cross-runtime validation for legacy calls
    final CrossRuntimeValidationResult defaultValidation =
        new CrossRuntimeValidationResult.Builder().consistencyScore(1.0).build();
    return determineBehavioralVerdict(
        consistencyScore, discrepancies, executionPattern, defaultValidation);
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

  /** Performs enhanced cross-runtime validation with strict requirements. */
  private CrossRuntimeValidationResult performCrossRuntimeValidation(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    final CrossRuntimeValidationResult.Builder resultBuilder =
        new CrossRuntimeValidationResult.Builder();

    if (executionResults.size() < 2) {
      return resultBuilder.consistencyScore(1.0).build();
    }

    // Validate JNI vs Panama equivalence if both are present
    if (executionResults.containsKey(RuntimeType.JNI)
        && executionResults.containsKey(RuntimeType.PANAMA)) {
      final TestExecutionResult jniResult = executionResults.get(RuntimeType.JNI);
      final TestExecutionResult panamaResult = executionResults.get(RuntimeType.PANAMA);

      final JniPanamaEquivalenceResult equivalenceResult =
          validateJniPanamaEquivalence(jniResult, panamaResult);
      resultBuilder.jniPanamaEquivalence(equivalenceResult);
    }

    // Calculate cross-runtime consistency score
    final double crossRuntimeScore = calculateCrossRuntimeConsistencyScore(executionResults);
    resultBuilder.consistencyScore(crossRuntimeScore);

    // Validate performance consistency
    final PerformanceConsistencyResult performanceConsistency =
        validatePerformanceConsistency(executionResults);
    resultBuilder.performanceConsistency(performanceConsistency);

    return resultBuilder.build();
  }

  /** Validates JNI vs Panama equivalence with strict tolerance. */
  private JniPanamaEquivalenceResult validateJniPanamaEquivalence(
      final TestExecutionResult jniResult, final TestExecutionResult panamaResult) {
    final JniPanamaEquivalenceResult.Builder resultBuilder =
        new JniPanamaEquivalenceResult.Builder();

    // Check execution status equivalence
    final boolean statusEquivalent =
        jniResult.isSuccessful() == panamaResult.isSuccessful()
            && jniResult.isSkipped() == panamaResult.isSkipped();
    resultBuilder.statusEquivalent(statusEquivalent);

    // Check return value equivalence for successful executions
    if (jniResult.isSuccessful() && panamaResult.isSuccessful()) {
      final ValueComparisonResult valueComparison =
          resultComparator.compareValues(jniResult.getReturnValue(), panamaResult.getReturnValue());
      resultBuilder.valueEquivalent(valueComparison.isEquivalent());
      resultBuilder.valueComparisonDetails(valueComparison);
    }

    // Check exception equivalence for failed executions
    if (!jniResult.isSuccessful() && !panamaResult.isSuccessful()) {
      final ExceptionComparisonResult exceptionComparison =
          compareExceptions(jniResult.getException(), panamaResult.getException());
      resultBuilder.exceptionEquivalent(exceptionComparison.getScore() >= 0.8);
      resultBuilder.exceptionComparisonDetails(exceptionComparison);
    }

    // Check performance equivalence within tolerance
    final TimingComparisonResult timingComparison =
        compareExecutionTiming(jniResult.getExecutionTime(), panamaResult.getExecutionTime());
    resultBuilder.performanceEquivalent(timingComparison.isWithinTolerance());
    resultBuilder.timingComparisonDetails(timingComparison);

    return resultBuilder.build();
  }

  /** Validates execution path consistency across runtimes. */
  private ExecutionPathValidationResult validateExecutionPaths(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    final ExecutionPathValidationResult.Builder resultBuilder =
        new ExecutionPathValidationResult.Builder();

    // Track execution paths and detect divergences
    final Map<RuntimeType, String> executionPaths = extractExecutionPaths(executionResults);
    final List<ExecutionPathDivergence> divergences =
        detectExecutionPathDivergences(executionPaths);

    resultBuilder.executionPaths(executionPaths);
    resultBuilder.divergences(divergences);
    resultBuilder.pathConsistencyScore(calculatePathConsistencyScore(divergences));

    return resultBuilder.build();
  }

  /** Analyzes side effects across runtimes. */
  private SideEffectAnalysisResult analyzeSideEffects(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    final SideEffectAnalysisResult.Builder resultBuilder = new SideEffectAnalysisResult.Builder();

    // Analyze memory state changes
    final Map<RuntimeType, MemoryStateChange> memoryStateChanges =
        analyzeMemoryStateChanges(executionResults);
    resultBuilder.memoryStateChanges(memoryStateChanges);

    // Analyze system state changes
    final Map<RuntimeType, SystemStateChange> systemStateChanges =
        analyzeSystemStateChanges(executionResults);
    resultBuilder.systemStateChanges(systemStateChanges);

    // Calculate side effect consistency score
    final double sideEffectScore =
        calculateSideEffectConsistencyScore(memoryStateChanges, systemStateChanges);
    resultBuilder.consistencyScore(sideEffectScore);

    return resultBuilder.build();
  }

  /** Calculates enhanced consistency score with additional validation factors. */
  private double calculateEnhancedConsistencyScore(
      final List<RuntimeComparison> pairwiseComparisons,
      final List<BehavioralDiscrepancy> discrepancies,
      final CrossRuntimeValidationResult crossRuntimeValidation,
      final ExecutionPathValidationResult executionPathValidation,
      final SideEffectAnalysisResult sideEffectAnalysis) {

    if (pairwiseComparisons.isEmpty()) {
      return 1.0; // Perfect consistency for single runtime
    }

    // Base score from pairwise comparisons
    double baseScore = 0.0;
    for (final RuntimeComparison comparison : pairwiseComparisons) {
      baseScore += comparison.getComparisonResult().getOverallScore();
    }
    baseScore /= pairwiseComparisons.size();

    // Weight factors for enhanced scoring
    final double crossRuntimeWeight = 0.3;
    final double executionPathWeight = 0.2;
    final double sideEffectWeight = 0.1;
    final double discrepancyPenaltyWeight = 0.4;

    // Calculate weighted score
    double enhancedScore =
        baseScore * (1.0 - crossRuntimeWeight - executionPathWeight - sideEffectWeight);
    enhancedScore += crossRuntimeValidation.getConsistencyScore() * crossRuntimeWeight;
    enhancedScore += executionPathValidation.getPathConsistencyScore() * executionPathWeight;
    enhancedScore += sideEffectAnalysis.getConsistencyScore() * sideEffectWeight;

    // Apply discrepancy penalty
    final double discrepancyPenalty = Math.min(0.5, discrepancies.size() * 0.05);
    enhancedScore = Math.max(0.0, enhancedScore - discrepancyPenalty * discrepancyPenaltyWeight);

    return enhancedScore;
  }

  /** Calculates cross-runtime consistency score. */
  private double calculateCrossRuntimeConsistencyScore(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    if (executionResults.size() < 2) {
      return 1.0;
    }

    final List<TestExecutionResult> results = new ArrayList<>(executionResults.values());
    int totalComparisons = 0;
    int consistentComparisons = 0;

    for (int i = 0; i < results.size(); i++) {
      for (int j = i + 1; j < results.size(); j++) {
        totalComparisons++;
        if (areResultsEquivalent(results.get(i), results.get(j))) {
          consistentComparisons++;
        }
      }
    }

    return totalComparisons > 0 ? (double) consistentComparisons / totalComparisons : 1.0;
  }

  /** Validates performance consistency across runtimes. */
  private PerformanceConsistencyResult validatePerformanceConsistency(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    final PerformanceConsistencyResult.Builder resultBuilder =
        new PerformanceConsistencyResult.Builder();

    final List<Duration> executionTimes =
        executionResults.values().stream()
            .map(TestExecutionResult::getExecutionTime)
            .collect(Collectors.toList());

    if (executionTimes.size() < 2) {
      return resultBuilder.consistent(true).varianceRatio(0.0).build();
    }

    final Duration minTime = executionTimes.stream().min(Duration::compareTo).orElse(Duration.ZERO);
    final Duration maxTime = executionTimes.stream().max(Duration::compareTo).orElse(Duration.ZERO);

    final double varianceRatio =
        minTime.toNanos() > 0 ? (double) maxTime.toNanos() / minTime.toNanos() : 1.0;

    // Performance is consistent if variance is within 20% (1.2x) for enhanced validation
    final boolean consistent = varianceRatio <= 1.2;

    return resultBuilder.consistent(consistent).varianceRatio(varianceRatio).build();
  }

  /** Extracts execution paths from test results for consistency analysis. */
  private Map<RuntimeType, String> extractExecutionPaths(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    final Map<RuntimeType, String> paths = new HashMap<>();

    for (final Map.Entry<RuntimeType, TestExecutionResult> entry : executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final TestExecutionResult result = entry.getValue();

      // Generate execution path signature based on result characteristics
      final StringBuilder pathBuilder = new StringBuilder();
      pathBuilder.append(result.isSuccessful() ? "SUCCESS" : "FAILURE");
      pathBuilder.append("-").append(result.isSkipped() ? "SKIPPED" : "EXECUTED");

      if (result.isSuccessful() && result.getReturnValue() != null) {
        pathBuilder.append("-RET:").append(result.getReturnValue().getClass().getSimpleName());
      } else if (!result.isSuccessful() && result.getException() != null) {
        pathBuilder.append("-EXC:").append(result.getException().getClass().getSimpleName());
      }

      paths.put(runtime, pathBuilder.toString());
    }

    return paths;
  }

  /** Detects execution path divergences across runtimes. */
  private List<ExecutionPathDivergence> detectExecutionPathDivergences(
      final Map<RuntimeType, String> executionPaths) {
    final List<ExecutionPathDivergence> divergences = new ArrayList<>();

    if (executionPaths.size() < 2) {
      return divergences;
    }

    final Set<String> uniquePaths = new HashSet<>(executionPaths.values());
    if (uniquePaths.size() > 1) {
      // Group runtimes by execution path
      final Map<String, Set<RuntimeType>> pathGroups = new HashMap<>();
      for (final Map.Entry<RuntimeType, String> entry : executionPaths.entrySet()) {
        pathGroups.computeIfAbsent(entry.getValue(), k -> new HashSet<>()).add(entry.getKey());
      }

      // Create divergence entries for different path groups
      final List<String> pathList = new ArrayList<>(pathGroups.keySet());
      for (int i = 0; i < pathList.size(); i++) {
        for (int j = i + 1; j < pathList.size(); j++) {
          final String path1 = pathList.get(i);
          final String path2 = pathList.get(j);
          final Set<RuntimeType> group1 = pathGroups.get(path1);
          final Set<RuntimeType> group2 = pathGroups.get(path2);

          divergences.add(
              new ExecutionPathDivergence(
                  group1, path1, group2, path2, calculatePathDivergenceSeverity(path1, path2)));
        }
      }
    }

    return divergences;
  }

  /** Calculates path consistency score based on divergences. */
  private double calculatePathConsistencyScore(final List<ExecutionPathDivergence> divergences) {
    if (divergences.isEmpty()) {
      return 1.0;
    }

    // Penalize based on number and severity of divergences
    double penalty = 0.0;
    for (final ExecutionPathDivergence divergence : divergences) {
      switch (divergence.getSeverity()) {
        case CRITICAL:
          penalty += 0.5;
          break;
        case MAJOR:
          penalty += 0.3;
          break;
        case MODERATE:
          penalty += 0.1;
          break;
        default:
          penalty += 0.05;
          break;
      }
    }

    return Math.max(0.0, 1.0 - penalty);
  }

  /** Analyzes memory state changes across runtimes. */
  private Map<RuntimeType, MemoryStateChange> analyzeMemoryStateChanges(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    final Map<RuntimeType, MemoryStateChange> changes = new HashMap<>();

    for (final Map.Entry<RuntimeType, TestExecutionResult> entry : executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final TestExecutionResult result = entry.getValue();

      if (result.getMemoryUsage().isPresent()) {
        final BehavioralAnalyzer.MemoryUsage memoryUsage = result.getMemoryUsage().get();
        changes.put(
            runtime,
            new MemoryStateChange(
                memoryUsage.getHeapUsed(),
                memoryUsage.getNonHeapUsed(),
                result.getExecutionTime()));
      }
    }

    return changes;
  }

  /** Analyzes system state changes across runtimes. */
  private Map<RuntimeType, SystemStateChange> analyzeSystemStateChanges(
      final Map<RuntimeType, TestExecutionResult> executionResults) {
    final Map<RuntimeType, SystemStateChange> changes = new HashMap<>();

    for (final Map.Entry<RuntimeType, TestExecutionResult> entry : executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final TestExecutionResult result = entry.getValue();

      // Track system state changes (simplified implementation)
      changes.put(
          runtime,
          new SystemStateChange(
              result.isSuccessful(), result.getExecutionTime(), getCurrentSystemState()));
    }

    return changes;
  }

  /** Calculates side effect consistency score. */
  private double calculateSideEffectConsistencyScore(
      final Map<RuntimeType, MemoryStateChange> memoryStateChanges,
      final Map<RuntimeType, SystemStateChange> systemStateChanges) {
    double score = 1.0;

    // Analyze memory state consistency
    if (memoryStateChanges.size() > 1) {
      final List<MemoryStateChange> memoryChanges = new ArrayList<>(memoryStateChanges.values());
      final double memoryConsistency = calculateMemoryConsistency(memoryChanges);
      score *= memoryConsistency;
    }

    // Analyze system state consistency
    if (systemStateChanges.size() > 1) {
      final List<SystemStateChange> systemChanges = new ArrayList<>(systemStateChanges.values());
      final double systemConsistency = calculateSystemConsistency(systemChanges);
      score *= systemConsistency;
    }

    return score;
  }

  /** Helper methods for internal calculations. */
  private boolean areResultsEquivalent(
      final TestExecutionResult result1, final TestExecutionResult result2) {
    // Check execution status
    if (result1.isSuccessful() != result2.isSuccessful()
        || result1.isSkipped() != result2.isSkipped()) {
      return false;
    }

    // For successful results, compare return values
    if (result1.isSuccessful() && result2.isSuccessful()) {
      final ValueComparisonResult valueComparison =
          resultComparator.compareValues(result1.getReturnValue(), result2.getReturnValue());
      return valueComparison.isEquivalent();
    }

    // For failed results, compare exception types
    if (!result1.isSuccessful() && !result2.isSuccessful()) {
      final ExceptionComparisonResult exceptionComparison =
          compareExceptions(result1.getException(), result2.getException());
      return exceptionComparison.getScore() >= 0.8;
    }

    return true;
  }

  private DiscrepancySeverity calculatePathDivergenceSeverity(
      final String path1, final String path2) {
    if (path1.startsWith("SUCCESS") != path2.startsWith("SUCCESS")) {
      return DiscrepancySeverity.CRITICAL;
    } else if (path1.contains("SKIPPED") != path2.contains("SKIPPED")) {
      return DiscrepancySeverity.MAJOR;
    } else {
      return DiscrepancySeverity.MODERATE;
    }
  }

  private String getCurrentSystemState() {
    // Simplified system state representation
    return String.format(
        "heap:%d,threads:%d",
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
        Thread.activeCount());
  }

  private double calculateMemoryConsistency(final List<MemoryStateChange> memoryChanges) {
    if (memoryChanges.size() < 2) {
      return 1.0;
    }

    final long maxHeap =
        memoryChanges.stream().mapToLong(MemoryStateChange::getHeapUsed).max().orElse(0);
    final long minHeap =
        memoryChanges.stream().mapToLong(MemoryStateChange::getHeapUsed).min().orElse(0);

    if (minHeap == 0) {
      return maxHeap == 0 ? 1.0 : 0.0;
    }

    final double heapRatio = (double) maxHeap / minHeap;
    return Math.max(0.0, 1.0 - Math.max(0.0, heapRatio - 1.5) / 2.0); // Penalize >1.5x differences
  }

  private double calculateSystemConsistency(final List<SystemStateChange> systemChanges) {
    // Simplified system consistency calculation
    final long uniqueStates =
        systemChanges.stream().map(SystemStateChange::getSystemState).distinct().count();

    return systemChanges.size() > 0 ? 1.0 / uniqueStates : 1.0;
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
