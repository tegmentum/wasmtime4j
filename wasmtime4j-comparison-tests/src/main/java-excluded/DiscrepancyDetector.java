package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Advanced discrepancy detection engine that identifies meaningful differences between test
 * execution results across different WebAssembly runtime implementations. Uses sophisticated
 * algorithms to distinguish between acceptable variations and actual behavioral discrepancies.
 *
 * <p>The detector implements pattern recognition for systematic differences and maintains a
 * categorization system for different types of behavioral discrepancies with severity levels.
 *
 * @since 1.0.0
 */
public final class DiscrepancyDetector {
  private static final Logger LOGGER = Logger.getLogger(DiscrepancyDetector.class.getName());

  // Pattern matchers for common discrepancy types
  private static final Map<DiscrepancyType, List<Pattern>> DISCREPANCY_PATTERNS =
      createDiscrepancyPatterns();

  // Systematic difference detection thresholds
  private static final double SYSTEMATIC_FAILURE_THRESHOLD = 0.8; // 80% of comparisons fail
  private static final double PERFORMANCE_DEVIATION_THRESHOLD = 2.0; // 2x performance difference
  private static final int MIN_SAMPLE_SIZE_FOR_PATTERNS = 3;

  // Wasmtime-specific detection thresholds for zero discrepancy requirement
  private static final double WASMTIME_ZERO_TOLERANCE =
      1e-15; // Near-zero tolerance for Wasmtime equivalence
  private static final double WASMTIME_FLOAT_PRECISION_TOLERANCE =
      1e-12; // Wasmtime floating-point precision
  private static final int WASMTIME_REGRESSION_WINDOW = 10; // Window for regression detection

  private final ToleranceConfiguration toleranceConfig;
  private final Map<String, DiscrepancyPattern> detectedPatterns;
  private final WasmtimeCompatibilityValidator wasmtimeValidator;
  private final RegressionDetector regressionDetector;

  /**
   * Creates a new DiscrepancyDetector with the specified tolerance configuration.
   *
   * @param toleranceConfig the tolerance configuration to use
   */
  public DiscrepancyDetector(final ToleranceConfiguration toleranceConfig) {
    this.toleranceConfig =
        Objects.requireNonNull(toleranceConfig, "toleranceConfig cannot be null");
    this.detectedPatterns = new ConcurrentHashMap<>();
    this.wasmtimeValidator = new WasmtimeCompatibilityValidator(toleranceConfig);
    this.regressionDetector = new RegressionDetector();
  }

  /**
   * Detects behavioral discrepancies across multiple runtime execution results.
   *
   * @param executionResults map of runtime execution results
   * @return list of detected behavioral discrepancies
   */
  public List<BehavioralDiscrepancy> detectDiscrepancies(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    Objects.requireNonNull(executionResults, "executionResults cannot be null");

    if (executionResults.size() < 2) {
      return List.of(); // No discrepancies possible with single runtime
    }

    LOGGER.fine("Detecting discrepancies across " + executionResults.size() + " runtimes");

    final List<BehavioralDiscrepancy> discrepancies = new ArrayList<>();

    // Detect execution status discrepancies
    discrepancies.addAll(detectExecutionStatusDiscrepancies(executionResults));

    // Detect return value discrepancies
    discrepancies.addAll(detectReturnValueDiscrepancies(executionResults));

    // Detect exception type discrepancies
    discrepancies.addAll(detectExceptionDiscrepancies(executionResults));

    // Detect performance discrepancies
    discrepancies.addAll(detectPerformanceDiscrepancies(executionResults));

    // Detect memory usage discrepancies
    discrepancies.addAll(detectMemoryDiscrepancies(executionResults));

    // Detect systematic patterns
    discrepancies.addAll(detectSystematicPatterns(executionResults, discrepancies));

    // Wasmtime-specific validations for zero discrepancy requirement
    discrepancies.addAll(detectWasmtimeCompatibilityIssues(executionResults));

    // Detect regression patterns
    discrepancies.addAll(detectRegressionPatterns(executionResults));

    // Validate zero discrepancy requirement
    discrepancies.addAll(validateZeroDiscrepancyRequirement(executionResults, discrepancies));

    LOGGER.fine("Detected " + discrepancies.size() + " total discrepancies");
    return discrepancies;
  }

  /** Detects execution status discrepancies (success/failure/skip inconsistencies). */
  private List<BehavioralDiscrepancy> detectExecutionStatusDiscrepancies(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> discrepancies = new ArrayList<>();

    final Map<String, Set<RuntimeType>> statusGroups = groupByExecutionStatus(executionResults);

    // Check for mixed success/failure patterns
    if (statusGroups.size() > 1) {
      final Set<RuntimeType> successfulRuntimes = statusGroups.get("successful");
      final Set<RuntimeType> failedRuntimes = statusGroups.get("failed");
      final Set<RuntimeType> skippedRuntimes = statusGroups.get("skipped");

      if (successfulRuntimes != null && failedRuntimes != null) {
        final DiscrepancySeverity severity =
            calculateStatusDiscrepancySeverity(successfulRuntimes.size(), failedRuntimes.size());

        discrepancies.add(
            new BehavioralDiscrepancy(
                DiscrepancyType.EXECUTION_STATUS_MISMATCH,
                severity,
                String.format(
                    "Execution status inconsistency: %d successful, %d failed runtimes",
                    successfulRuntimes.size(), failedRuntimes.size()),
                String.format("Successful: %s, Failed: %s", successfulRuntimes, failedRuntimes),
                generateStatusDiscrepancyRecommendation(successfulRuntimes, failedRuntimes)));
      }

      if (skippedRuntimes != null && !skippedRuntimes.isEmpty()) {
        discrepancies.add(
            new BehavioralDiscrepancy(
                DiscrepancyType.SKIP_INCONSISTENCY,
                DiscrepancySeverity.MODERATE,
                "Some runtimes skipped test execution",
                "Skipped runtimes: " + skippedRuntimes,
                "Investigate runtime availability and test execution conditions"));
      }
    }

    return discrepancies;
  }

  /** Groups execution results by their execution status. */
  private Map<String, Set<RuntimeType>> groupByExecutionStatus(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final Map<String, Set<RuntimeType>> groups = new ConcurrentHashMap<>();

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry :
        executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult result = entry.getValue();

      final String status;
      if (result.isSkipped()) {
        status = "skipped";
      } else if (result.isSuccessful()) {
        status = "successful";
      } else {
        status = "failed";
      }

      groups.computeIfAbsent(status, k -> ConcurrentHashMap.newKeySet()).add(runtime);
    }

    return groups;
  }

  /** Calculates severity for execution status discrepancies. */
  private DiscrepancySeverity calculateStatusDiscrepancySeverity(
      final int successCount, final int failCount) {
    final int total = successCount + failCount;
    final double failureRate = (double) failCount / total;

    if (failureRate >= 0.75 || failureRate <= 0.25) {
      return DiscrepancySeverity.CRITICAL;
    } else if (failureRate >= 0.6 || failureRate <= 0.4) {
      return DiscrepancySeverity.MAJOR;
    } else {
      return DiscrepancySeverity.MODERATE;
    }
  }

  /** Detects return value discrepancies among successful executions. */
  private List<BehavioralDiscrepancy> detectReturnValueDiscrepancies(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> discrepancies = new ArrayList<>();

    final Map<RuntimeType, Object> successfulResults =
        executionResults.entrySet().stream()
            .filter(entry -> entry.getValue().isSuccessful())
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getReturnValue()));

    if (successfulResults.size() < 2) {
      return discrepancies; // Need at least 2 successful results to compare
    }

    // Group results by equivalence
    final Map<String, Set<RuntimeType>> valueGroups = groupByReturnValue(successfulResults);

    if (valueGroups.size() > 1) {
      final DiscrepancySeverity severity = calculateReturnValueDiscrepancySeverity(valueGroups);

      discrepancies.add(
          new BehavioralDiscrepancy(
              DiscrepancyType.RETURN_VALUE_MISMATCH,
              severity,
              "Return value inconsistency across runtimes",
              formatReturnValueGroups(valueGroups, successfulResults),
              "Investigate value serialization, type conversion, or runtime-specific behavior"));
    }

    return discrepancies;
  }

  /** Groups successful execution results by their return value equivalence. */
  private Map<String, Set<RuntimeType>> groupByReturnValue(
      final Map<RuntimeType, Object> successfulResults) {
    final Map<String, Set<RuntimeType>> groups = new ConcurrentHashMap<>();
    final ResultComparator comparator = new ResultComparator(toleranceConfig);

    for (final Map.Entry<RuntimeType, Object> entry : successfulResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final Object value = entry.getValue();

      // Find existing group that matches this value
      String matchingGroup = null;
      for (final Map.Entry<String, Set<RuntimeType>> groupEntry : groups.entrySet()) {
        final String groupKey = groupEntry.getKey();
        final RuntimeType existingRuntime = groupEntry.getValue().iterator().next();
        final Object existingValue = successfulResults.get(existingRuntime);

        final ValueComparisonResult comparison = comparator.compareValues(value, existingValue);
        if (comparison.isEquivalent()) {
          matchingGroup = groupKey;
          break;
        }
      }

      if (matchingGroup != null) {
        groups.get(matchingGroup).add(runtime);
      } else {
        final String newGroupKey = "group_" + groups.size();
        groups.put(newGroupKey, ConcurrentHashMap.newKeySet());
        groups.get(newGroupKey).add(runtime);
      }
    }

    return groups;
  }

  /** Detects exception type and message discrepancies among failed executions. */
  private List<BehavioralDiscrepancy> detectExceptionDiscrepancies(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> discrepancies = new ArrayList<>();

    final Map<RuntimeType, Exception> failedResults =
        executionResults.entrySet().stream()
            .filter(entry -> !entry.getValue().isSuccessful() && !entry.getValue().isSkipped())
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getException()));

    if (failedResults.size() < 2) {
      return discrepancies; // Need at least 2 failed results to compare
    }

    // Analyze exception type consistency
    final Set<String> exceptionTypes =
        failedResults.values().stream()
            .filter(Objects::nonNull)
            .map(e -> e.getClass().getSimpleName())
            .collect(Collectors.toSet());

    if (exceptionTypes.size() > 1) {
      discrepancies.add(
          new BehavioralDiscrepancy(
              DiscrepancyType.EXCEPTION_TYPE_MISMATCH,
              DiscrepancySeverity.MAJOR,
              "Different exception types across failed runtimes",
              "Exception types: " + exceptionTypes,
              "Investigate runtime-specific error handling and exception mapping"));
    }

    // Analyze exception message consistency
    final Set<String> exceptionMessages =
        failedResults.values().stream()
            .filter(Objects::nonNull)
            .map(Exception::getMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    if (exceptionMessages.size() > 1) {
      discrepancies.add(
          new BehavioralDiscrepancy(
              DiscrepancyType.EXCEPTION_MESSAGE_MISMATCH,
              DiscrepancySeverity.MODERATE,
              "Different exception messages across failed runtimes",
              "Message count: " + exceptionMessages.size(),
              "Review error message consistency and localization"));
    }

    return discrepancies;
  }

  /** Detects performance discrepancies across runtimes. */
  private List<BehavioralDiscrepancy> detectPerformanceDiscrepancies(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> discrepancies = new ArrayList<>();

    final Map<RuntimeType, Duration> executionTimes =
        executionResults.entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getExecutionTime()));

    if (executionTimes.size() < 2) {
      return discrepancies;
    }

    final Duration minTime =
        executionTimes.values().stream().min(Duration::compareTo).orElse(Duration.ZERO);
    final Duration maxTime =
        executionTimes.values().stream().max(Duration::compareTo).orElse(Duration.ZERO);

    if (minTime.toNanos() > 0) {
      final double performanceRatio = (double) maxTime.toNanos() / minTime.toNanos();

      if (performanceRatio > PERFORMANCE_DEVIATION_THRESHOLD) {
        final DiscrepancySeverity severity =
            calculatePerformanceDiscrepancySeverity(performanceRatio);

        discrepancies.add(
            new BehavioralDiscrepancy(
                DiscrepancyType.PERFORMANCE_DEVIATION,
                severity,
                String.format(
                    "Significant performance variation: %.2fx difference", performanceRatio),
                String.format("Min: %dms, Max: %dms", minTime.toMillis(), maxTime.toMillis()),
                "Analyze performance bottlenecks and runtime-specific optimizations"));
      }
    }

    return discrepancies;
  }

  /** Detects memory usage discrepancies across runtimes. */
  private List<BehavioralDiscrepancy> detectMemoryDiscrepancies(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> discrepancies = new ArrayList<>();

    final Map<RuntimeType, BehavioralAnalyzer.MemoryUsage> memoryUsages =
        executionResults.entrySet().stream()
            .filter(entry -> entry.getValue().getMemoryUsage().isPresent())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, entry -> entry.getValue().getMemoryUsage().get()));

    if (memoryUsages.size() < 2) {
      return discrepancies;
    }

    // Analyze heap memory variation
    final long minHeap =
        memoryUsages.values().stream()
            .mapToLong(BehavioralAnalyzer.MemoryUsage::getHeapUsed)
            .min()
            .orElse(0);
    final long maxHeap =
        memoryUsages.values().stream()
            .mapToLong(BehavioralAnalyzer.MemoryUsage::getHeapUsed)
            .max()
            .orElse(0);

    if (minHeap > 0) {
      final double heapRatio = (double) maxHeap / minHeap;
      if (heapRatio > 2.0) { // More than 2x difference
        discrepancies.add(
            new BehavioralDiscrepancy(
                DiscrepancyType.MEMORY_USAGE_DEVIATION,
                DiscrepancySeverity.MODERATE,
                String.format("Significant heap memory variation: %.2fx difference", heapRatio),
                String.format("Min heap: %d bytes, Max heap: %d bytes", minHeap, maxHeap),
                "Investigate memory allocation patterns and garbage collection behavior"));
      }
    }

    return discrepancies;
  }

  /** Detects systematic patterns across multiple discrepancies. */
  private List<BehavioralDiscrepancy> detectSystematicPatterns(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults,
      final List<BehavioralDiscrepancy> existingDiscrepancies) {
    final List<BehavioralDiscrepancy> patterns = new ArrayList<>();

    if (existingDiscrepancies.size() < MIN_SAMPLE_SIZE_FOR_PATTERNS) {
      return patterns;
    }

    // Detect runtime-specific failure patterns
    final Map<RuntimeType, Integer> runtimeFailureCounts = new EnumMap<>(RuntimeType.class);
    for (final BehavioralDiscrepancy discrepancy : existingDiscrepancies) {
      // Count discrepancies that affect specific runtimes
      // This is a simplified pattern detection - could be enhanced with ML techniques
    }

    // Detect temporal patterns (if timing data is available)
    // This could include detecting performance degradation over time

    return patterns;
  }

  /** Creates discrepancy patterns for pattern matching. */
  private static Map<DiscrepancyType, List<Pattern>> createDiscrepancyPatterns() {
    final Map<DiscrepancyType, List<Pattern>> patterns = new EnumMap<>(DiscrepancyType.class);

    patterns.put(
        DiscrepancyType.EXECUTION_STATUS_MISMATCH,
        List.of(
            Pattern.compile("status.*mismatch", Pattern.CASE_INSENSITIVE),
            Pattern.compile("success.*failure", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        DiscrepancyType.RETURN_VALUE_MISMATCH,
        List.of(
            Pattern.compile("value.*mismatch", Pattern.CASE_INSENSITIVE),
            Pattern.compile("result.*different", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        DiscrepancyType.EXCEPTION_TYPE_MISMATCH,
        List.of(
            Pattern.compile("exception.*type", Pattern.CASE_INSENSITIVE),
            Pattern.compile("error.*type.*different", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        DiscrepancyType.PERFORMANCE_DEVIATION,
        List.of(
            Pattern.compile("performance.*deviation", Pattern.CASE_INSENSITIVE),
            Pattern.compile("timing.*difference", Pattern.CASE_INSENSITIVE)));

    return patterns;
  }

  /** Calculates severity for return value discrepancies. */
  private DiscrepancySeverity calculateReturnValueDiscrepancySeverity(
      final Map<String, Set<RuntimeType>> valueGroups) {
    final int groupCount = valueGroups.size();
    final int totalRuntimes = valueGroups.values().stream().mapToInt(Set::size).sum();

    final double consistency = 1.0 - (double) groupCount / totalRuntimes;

    if (consistency < 0.5) {
      return DiscrepancySeverity.CRITICAL;
    } else if (consistency < 0.7) {
      return DiscrepancySeverity.MAJOR;
    } else {
      return DiscrepancySeverity.MODERATE;
    }
  }

  /** Calculates severity for performance discrepancies. */
  private DiscrepancySeverity calculatePerformanceDiscrepancySeverity(
      final double performanceRatio) {
    if (performanceRatio > 10.0) {
      return DiscrepancySeverity.CRITICAL;
    } else if (performanceRatio > 5.0) {
      return DiscrepancySeverity.MAJOR;
    } else {
      return DiscrepancySeverity.MODERATE;
    }
  }

  /** Formats return value groups for display. */
  private String formatReturnValueGroups(
      final Map<String, Set<RuntimeType>> valueGroups,
      final Map<RuntimeType, Object> successfulResults) {
    final StringBuilder sb = new StringBuilder();
    for (final Map.Entry<String, Set<RuntimeType>> entry : valueGroups.entrySet()) {
      final Set<RuntimeType> runtimes = entry.getValue();
      final RuntimeType sampleRuntime = runtimes.iterator().next();
      final Object sampleValue = successfulResults.get(sampleRuntime);

      sb.append(runtimes).append(": ").append(sampleValue).append("; ");
    }
    return sb.toString();
  }

  /** Generates recommendation for status discrepancies. */
  private String generateStatusDiscrepancyRecommendation(
      final Set<RuntimeType> successfulRuntimes, final Set<RuntimeType> failedRuntimes) {
    if (successfulRuntimes.size() == 1) {
      return "Investigate why only "
          + successfulRuntimes.iterator().next()
          + " runtime succeeded while others failed";
    } else if (failedRuntimes.size() == 1) {
      return "Investigate why "
          + failedRuntimes.iterator().next()
          + " runtime failed while others succeeded";
    } else {
      return "Investigate fundamental compatibility differences between runtime groups";
    }
  }

  /** Detects Wasmtime-specific compatibility issues for zero discrepancy requirement. */
  private List<BehavioralDiscrepancy> detectWasmtimeCompatibilityIssues(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    return wasmtimeValidator.validateCompatibility(executionResults);
  }

  /** Detects regression patterns using historical data. */
  private List<BehavioralDiscrepancy> detectRegressionPatterns(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    return regressionDetector.detectRegressions(executionResults);
  }

  /** Validates the zero discrepancy requirement against Wasmtime standards. */
  private List<BehavioralDiscrepancy> validateZeroDiscrepancyRequirement(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults,
      final List<BehavioralDiscrepancy> existingDiscrepancies) {
    final List<BehavioralDiscrepancy> validationIssues = new ArrayList<>();

    // Check if any critical discrepancies exist that violate zero discrepancy requirement
    final long criticalCount =
        existingDiscrepancies.stream()
            .filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL)
            .count();

    if (criticalCount > 0) {
      validationIssues.add(
          new BehavioralDiscrepancy(
              DiscrepancyType.SYSTEMATIC_PATTERN,
              DiscrepancySeverity.CRITICAL,
              "Zero discrepancy requirement violated",
              String.format(
                  "%d critical discrepancies detected, violating zero tolerance requirement",
                  criticalCount),
              "Review and fix all critical discrepancies to achieve zero discrepancy goal",
              "validation",
              executionResults.keySet()));
    }

    // Check for any behavioral inconsistencies between JNI and Panama
    if (executionResults.containsKey(RuntimeType.JNI)
        && executionResults.containsKey(RuntimeType.PANAMA)) {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          executionResults.get(RuntimeType.JNI);
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          executionResults.get(RuntimeType.PANAMA);

      if (!areResultsEquivalent(jniResult, panamaResult)) {
        validationIssues.add(
            new BehavioralDiscrepancy(
                DiscrepancyType.EXECUTION_STATUS_MISMATCH,
                DiscrepancySeverity.CRITICAL,
                "JNI vs Panama behavioral divergence detected",
                "Execution results differ between JNI and Panama implementations",
                "Investigate and fix behavioral differences to ensure complete equivalence",
                "jni-panama-equivalence",
                Set.of(RuntimeType.JNI, RuntimeType.PANAMA)));
      }
    }

    return validationIssues;
  }

  /** Checks if two execution results are equivalent within Wasmtime tolerance. */
  private boolean areResultsEquivalent(
      final BehavioralAnalyzer.TestExecutionResult result1,
      final BehavioralAnalyzer.TestExecutionResult result2) {
    // Check execution status
    if (result1.isSuccessful() != result2.isSuccessful()
        || result1.isSkipped() != result2.isSkipped()) {
      return false;
    }

    // For successful results, compare return values with Wasmtime precision
    if (result1.isSuccessful() && result2.isSuccessful()) {
      return areValuesEquivalent(result1.getReturnValue(), result2.getReturnValue());
    }

    // For failed results, compare exception types
    if (!result1.isSuccessful() && !result2.isSuccessful()) {
      return areExceptionsEquivalent(result1.getException(), result2.getException());
    }

    return true;
  }

  /** Checks if two values are equivalent within Wasmtime precision tolerance. */
  private boolean areValuesEquivalent(final Object value1, final Object value2) {
    if (Objects.equals(value1, value2)) {
      return true;
    }

    if (value1 == null || value2 == null) {
      return false;
    }

    // Handle floating-point comparisons with Wasmtime precision
    if (value1 instanceof Number && value2 instanceof Number) {
      final double d1 = ((Number) value1).doubleValue();
      final double d2 = ((Number) value2).doubleValue();
      return Math.abs(d1 - d2) <= WASMTIME_FLOAT_PRECISION_TOLERANCE;
    }

    // Handle array comparisons
    if (value1.getClass().isArray() && value2.getClass().isArray()) {
      return areArraysEquivalent(value1, value2);
    }

    return false;
  }

  /** Checks if two arrays are equivalent within Wasmtime precision. */
  private boolean areArraysEquivalent(final Object array1, final Object array2) {
    if (array1 instanceof byte[] && array2 instanceof byte[]) {
      return java.util.Arrays.equals((byte[]) array1, (byte[]) array2);
    }
    if (array1 instanceof int[] && array2 instanceof int[]) {
      return java.util.Arrays.equals((int[]) array1, (int[]) array2);
    }
    if (array1 instanceof long[] && array2 instanceof long[]) {
      return java.util.Arrays.equals((long[]) array1, (long[]) array2);
    }
    if (array1 instanceof float[] && array2 instanceof float[]) {
      final float[] f1 = (float[]) array1;
      final float[] f2 = (float[]) array2;
      if (f1.length != f2.length) {
        return false;
      }
      for (int i = 0; i < f1.length; i++) {
        if (Math.abs(f1[i] - f2[i]) > WASMTIME_FLOAT_PRECISION_TOLERANCE) {
          return false;
        }
      }
      return true;
    }
    if (array1 instanceof double[] && array2 instanceof double[]) {
      final double[] d1 = (double[]) array1;
      final double[] d2 = (double[]) array2;
      if (d1.length != d2.length) {
        return false;
      }
      for (int i = 0; i < d1.length; i++) {
        if (Math.abs(d1[i] - d2[i]) > WASMTIME_FLOAT_PRECISION_TOLERANCE) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /** Checks if two exceptions are equivalent. */
  private boolean areExceptionsEquivalent(final Exception exception1, final Exception exception2) {
    if (exception1 == null && exception2 == null) {
      return true;
    }
    if (exception1 == null || exception2 == null) {
      return false;
    }

    // Check if exceptions belong to the same semantic category
    return categorizeException(exception1).equals(categorizeException(exception2));
  }

  /** Pattern detected by the discrepancy detector. */
  public static final class DiscrepancyPattern {
    private final String patternId;
    private final DiscrepancyType type;
    private final String description;
    private final int frequency;

    /**
     * Creates a new discrepancy pattern with the specified details.
     *
     * @param patternId unique identifier for this pattern
     * @param type the type of discrepancy this pattern represents
     * @param description human-readable description of the pattern
     * @param frequency how frequently this pattern has been observed
     */
    public DiscrepancyPattern(
        final String patternId,
        final DiscrepancyType type,
        final String description,
        final int frequency) {
      this.patternId = Objects.requireNonNull(patternId, "patternId cannot be null");
      this.type = Objects.requireNonNull(type, "type cannot be null");
      this.description = Objects.requireNonNull(description, "description cannot be null");
      this.frequency = frequency;
    }

    public String getPatternId() {
      return patternId;
    }

    public DiscrepancyType getType() {
      return type;
    }

    public String getDescription() {
      return description;
    }

    public int getFrequency() {
      return frequency;
    }
  }
}
