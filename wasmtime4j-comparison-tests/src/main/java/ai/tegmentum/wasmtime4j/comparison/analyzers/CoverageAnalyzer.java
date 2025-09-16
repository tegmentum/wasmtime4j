package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Comprehensive coverage analyzer that maps WebAssembly feature usage against test execution
 * results to identify coverage gaps and ensure comprehensive validation across runtime implementations.
 *
 * <p>Key functionality includes:
 * <ul>
 *   <li>WebAssembly feature coverage mapping against test results
 *   <li>Coverage gap analysis for incomplete test scenarios
 *   <li>Feature usage pattern analysis across different runtimes
 *   <li>Test coverage completeness assessment
 *   <li>Integration with behavioral and performance analysis results
 * </ul>
 *
 * <p>This analyzer works with results from {@link BehavioralAnalyzer} and {@link PerformanceAnalyzer}
 * to provide comprehensive insights into test coverage and feature validation.
 *
 * @since 1.0.0
 */
public final class CoverageAnalyzer {
  private static final Logger LOGGER = Logger.getLogger(CoverageAnalyzer.class.getName());

  // WebAssembly feature categories for comprehensive coverage tracking
  private static final Map<String, Set<String>> WASM_FEATURE_CATEGORIES;

  static {
    final Map<String, Set<String>> categories = new HashMap<>();

    // Core WebAssembly features
    categories.put("CORE", Set.of(
        "memory_operations", "control_flow", "function_calls", "local_variables",
        "global_variables", "constants", "arithmetic_operations", "comparison_operations",
        "logical_operations", "conversion_operations", "reinterpret_operations"
    ));

    // Memory management features
    categories.put("MEMORY", Set.of(
        "linear_memory", "memory_grow", "memory_size", "memory_copy", "memory_fill",
        "data_segments", "bulk_memory_operations", "memory_init", "data_drop"
    ));

    // Table and reference type features
    categories.put("TABLES", Set.of(
        "table_operations", "table_grow", "table_size", "table_get", "table_set",
        "table_copy", "table_fill", "table_init", "elem_drop", "reference_types"
    ));

    // Import/Export features
    categories.put("IMPORTS_EXPORTS", Set.of(
        "function_imports", "memory_imports", "table_imports", "global_imports",
        "function_exports", "memory_exports", "table_exports", "global_exports"
    ));

    // Exception handling (proposal)
    categories.put("EXCEPTIONS", Set.of(
        "try_catch", "throw", "rethrow", "exception_handling", "exception_types"
    ));

    // SIMD operations (proposal)
    categories.put("SIMD", Set.of(
        "v128_operations", "load_splat", "load_extend", "load_zero", "shuffle",
        "swizzle", "arithmetic_simd", "comparison_simd", "conversion_simd"
    ));

    // Threading and atomics (proposal)
    categories.put("THREADING", Set.of(
        "atomic_load", "atomic_store", "atomic_rmw", "atomic_cmpxchg",
        "atomic_wait", "atomic_notify", "shared_memory"
    ));

    // WASI features
    categories.put("WASI", Set.of(
        "file_operations", "environment_access", "command_line_args", "random_generation",
        "time_operations", "process_exit", "stdio_operations", "network_operations"
    ));

    WASM_FEATURE_CATEGORIES = Map.copyOf(categories);
  }

  private final Map<String, CoverageTracker> coverageTrackers;
  private final Set<String> analyzedTests;

  /** Creates a new CoverageAnalyzer with empty coverage tracking. */
  public CoverageAnalyzer() {
    this.coverageTrackers = new ConcurrentHashMap<>();
    this.analyzedTests = ConcurrentHashMap.newKeySet();
    initializeCoverageTrackers();
  }

  /**
   * Analyzes test coverage for WebAssembly features based on execution results and behavioral analysis.
   *
   * @param testName the name of the test being analyzed
   * @param executionResults the execution results for all runtimes
   * @param behavioralResults the behavioral analysis results
   * @param performanceResults the performance analysis results
   * @return comprehensive coverage analysis result
   */
  public CoverageAnalysisResult analyzeCoverage(
      final String testName,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults,
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults) {

    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(executionResults, "executionResults cannot be null");
    Objects.requireNonNull(behavioralResults, "behavioralResults cannot be null");
    Objects.requireNonNull(performanceResults, "performanceResults cannot be null");

    LOGGER.info(String.format("Analyzing coverage for test: %s", testName));

    final CoverageAnalysisResult.Builder resultBuilder =
        new CoverageAnalysisResult.Builder(testName);

    // Detect WebAssembly features used in this test
    final Set<String> detectedFeatures = detectWebAssemblyFeatures(testName, executionResults);
    resultBuilder.detectedFeatures(detectedFeatures);

    // Analyze feature coverage across runtimes
    final Map<RuntimeType, Set<String>> runtimeFeatureCoverage =
        analyzeRuntimeFeatureCoverage(executionResults, detectedFeatures);
    resultBuilder.runtimeFeatureCoverage(runtimeFeatureCoverage);

    // Calculate coverage metrics
    final CoverageMetrics coverageMetrics = calculateCoverageMetrics(
        detectedFeatures, runtimeFeatureCoverage, executionResults);
    resultBuilder.coverageMetrics(coverageMetrics);

    // Identify coverage gaps
    final List<CoverageGap> coverageGaps = identifyCoverageGaps(
        detectedFeatures, runtimeFeatureCoverage, behavioralResults);
    resultBuilder.coverageGaps(coverageGaps);

    // Analyze feature interaction patterns
    final FeatureInteractionAnalysis interactionAnalysis =
        analyzeFeatureInteractions(detectedFeatures, executionResults, behavioralResults);
    resultBuilder.featureInteractionAnalysis(interactionAnalysis);

    // Update global coverage tracking
    updateCoverageTracking(testName, detectedFeatures, runtimeFeatureCoverage);

    final CoverageAnalysisResult result = resultBuilder.build();
    analyzedTests.add(testName);

    LOGGER.info(String.format("Coverage analysis completed for %s: %d features detected, %.2f%% overall coverage",
        testName, detectedFeatures.size(), coverageMetrics.getOverallCoveragePercentage()));

    return result;
  }

  /**
   * Generates a comprehensive coverage report across all analyzed tests.
   *
   * @return comprehensive coverage report
   */
  public ComprehensiveCoverageReport generateComprehensiveReport() {
    LOGGER.info("Generating comprehensive coverage report");

    final Map<String, Double> categoryCompleteness = calculateCategoryCompleteness();
    final List<String> uncoveredFeatures = identifyUncoveredFeatures();
    final Map<RuntimeType, Double> runtimeCoverageScores = calculateRuntimeCoverageScores();
    final List<CoverageRecommendation> recommendations = generateCoverageRecommendations();
    final CoverageTrend coverageTrend = analyzeCoverageTrend();

    return new ComprehensiveCoverageReport(
        categoryCompleteness,
        uncoveredFeatures,
        runtimeCoverageScores,
        recommendations,
        coverageTrend,
        analyzedTests.size(),
        Instant.now()
    );
  }

  /**
   * Gets the current global coverage statistics.
   *
   * @return global coverage statistics
   */
  public GlobalCoverageStatistics getGlobalCoverageStatistics() {
    final int totalFeatures = WASM_FEATURE_CATEGORIES.values().stream()
        .mapToInt(Set::size)
        .sum();

    final long coveredFeatures = coverageTrackers.values().stream()
        .mapToLong(tracker -> tracker.getCoveredFeatures().size())
        .sum();

    final double overallPercentage = totalFeatures > 0 ?
        (double) coveredFeatures / totalFeatures * 100.0 : 0.0;

    return new GlobalCoverageStatistics(
        totalFeatures,
        (int) coveredFeatures,
        overallPercentage,
        analyzedTests.size(),
        coverageTrackers.size()
    );
  }

  /** Clears all coverage tracking data. */
  public void clearCoverageData() {
    coverageTrackers.clear();
    analyzedTests.clear();
    initializeCoverageTrackers();
    LOGGER.info("Coverage tracking data cleared");
  }

  private void initializeCoverageTrackers() {
    for (final String category : WASM_FEATURE_CATEGORIES.keySet()) {
      coverageTrackers.put(category, new CoverageTracker(category));
    }
  }

  private Set<String> detectWebAssemblyFeatures(
      final String testName,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {

    final Set<String> detectedFeatures = new HashSet<>();

    // Feature detection based on test name patterns
    final String testNameLower = testName.toLowerCase();

    // Core features detection
    if (testNameLower.contains("memory") || testNameLower.contains("load") || testNameLower.contains("store")) {
      detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("MEMORY"));
    }
    if (testNameLower.contains("table") || testNameLower.contains("elem")) {
      detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("TABLES"));
    }
    if (testNameLower.contains("import") || testNameLower.contains("export")) {
      detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("IMPORTS_EXPORTS"));
    }
    if (testNameLower.contains("exception") || testNameLower.contains("try") || testNameLower.contains("catch")) {
      detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("EXCEPTIONS"));
    }
    if (testNameLower.contains("simd") || testNameLower.contains("v128")) {
      detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("SIMD"));
    }
    if (testNameLower.contains("atomic") || testNameLower.contains("thread")) {
      detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("THREADING"));
    }
    if (testNameLower.contains("wasi") || testNameLower.contains("file") || testNameLower.contains("env")) {
      detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("WASI"));
    }

    // Always include core features for any test
    detectedFeatures.addAll(Set.of(
        "function_calls", "control_flow", "arithmetic_operations", "comparison_operations"
    ));

    // Feature detection based on execution results and exceptions
    for (final BehavioralAnalyzer.TestExecutionResult result : executionResults.values()) {
      if (!result.isSuccessful() && result.getException() != null) {
        final String exceptionMessage = result.getException().getMessage();
        if (exceptionMessage != null) {
          final String messageLower = exceptionMessage.toLowerCase();
          if (messageLower.contains("memory")) {
            detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("MEMORY"));
          }
          if (messageLower.contains("table")) {
            detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("TABLES"));
          }
          if (messageLower.contains("import") || messageLower.contains("link")) {
            detectedFeatures.addAll(WASM_FEATURE_CATEGORIES.get("IMPORTS_EXPORTS"));
          }
        }
      }
    }

    LOGGER.fine(String.format("Detected %d features for test %s: %s",
        detectedFeatures.size(), testName, detectedFeatures));

    return detectedFeatures;
  }

  private Map<RuntimeType, Set<String>> analyzeRuntimeFeatureCoverage(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults,
      final Set<String> detectedFeatures) {

    final Map<RuntimeType, Set<String>> runtimeCoverage = new EnumMap<>(RuntimeType.class);

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry :
         executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult result = entry.getValue();

      final Set<String> runtimeFeatures = new HashSet<>();

      if (result.isSuccessful()) {
        // If execution was successful, assume all detected features are covered
        runtimeFeatures.addAll(detectedFeatures);
      } else {
        // If execution failed, only include features that could be partially validated
        // based on the type of failure
        final Exception exception = result.getException();
        if (exception != null) {
          final String exceptionClass = exception.getClass().getSimpleName().toLowerCase();
          if (exceptionClass.contains("compilation") || exceptionClass.contains("validation")) {
            // Compilation/validation errors mean features were at least parsed
            runtimeFeatures.addAll(detectedFeatures.stream()
                .filter(feature -> WASM_FEATURE_CATEGORIES.get("CORE").contains(feature))
                .collect(Collectors.toSet()));
          } else if (exceptionClass.contains("runtime") || exceptionClass.contains("trap")) {
            // Runtime errors mean compilation succeeded, so more features are covered
            runtimeFeatures.addAll(detectedFeatures);
          }
        }
      }

      runtimeCoverage.put(runtime, runtimeFeatures);
    }

    return runtimeCoverage;
  }

  private CoverageMetrics calculateCoverageMetrics(
      final Set<String> detectedFeatures,
      final Map<RuntimeType, Set<String>> runtimeFeatureCoverage,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {

    final int totalDetectedFeatures = detectedFeatures.size();

    // Calculate per-runtime coverage
    final Map<RuntimeType, Double> runtimeCoveragePercentages = new EnumMap<>(RuntimeType.class);
    for (final Map.Entry<RuntimeType, Set<String>> entry : runtimeFeatureCoverage.entrySet()) {
      final double percentage = totalDetectedFeatures > 0 ?
          (double) entry.getValue().size() / totalDetectedFeatures * 100.0 : 100.0;
      runtimeCoveragePercentages.put(entry.getKey(), percentage);
    }

    // Calculate overall coverage (intersection of all runtimes)
    Set<String> commonFeatures = new HashSet<>(detectedFeatures);
    for (final Set<String> runtimeFeatures : runtimeFeatureCoverage.values()) {
      commonFeatures.retainAll(runtimeFeatures);
    }

    final double overallCoveragePercentage = totalDetectedFeatures > 0 ?
        (double) commonFeatures.size() / totalDetectedFeatures * 100.0 : 100.0;

    // Calculate success rate
    final long successfulRuntimes = executionResults.values().stream()
        .mapToLong(result -> result.isSuccessful() ? 1 : 0)
        .sum();
    final double successRate = executionResults.size() > 0 ?
        (double) successfulRuntimes / executionResults.size() * 100.0 : 0.0;

    return new CoverageMetrics(
        totalDetectedFeatures,
        commonFeatures.size(),
        overallCoveragePercentage,
        runtimeCoveragePercentages,
        successRate
    );
  }

  private List<CoverageGap> identifyCoverageGaps(
      final Set<String> detectedFeatures,
      final Map<RuntimeType, Set<String>> runtimeFeatureCoverage,
      final BehavioralAnalysisResult behavioralResults) {

    final List<CoverageGap> gaps = new ArrayList<>();

    // Identify features missing from specific runtimes
    for (final RuntimeType runtime : RuntimeType.values()) {
      if (!runtimeFeatureCoverage.containsKey(runtime)) {
        gaps.add(new CoverageGap(
            CoverageGapType.RUNTIME_MISSING,
            String.format("Runtime %s not tested", runtime),
            detectedFeatures,
            Set.of(runtime),
            GapSeverity.HIGH
        ));
        continue;
      }

      final Set<String> runtimeFeatures = runtimeFeatureCoverage.get(runtime);
      final Set<String> missingFeatures = new HashSet<>(detectedFeatures);
      missingFeatures.removeAll(runtimeFeatures);

      if (!missingFeatures.isEmpty()) {
        final GapSeverity severity = determineSeverity(missingFeatures, behavioralResults);
        gaps.add(new CoverageGap(
            CoverageGapType.FEATURE_INCOMPLETE,
            String.format("Runtime %s missing %d features", runtime, missingFeatures.size()),
            missingFeatures,
            Set.of(runtime),
            severity
        ));
      }
    }

    // Identify category-level gaps
    for (final Map.Entry<String, Set<String>> categoryEntry : WASM_FEATURE_CATEGORIES.entrySet()) {
      final String category = categoryEntry.getKey();
      final Set<String> categoryFeatures = categoryEntry.getValue();
      final Set<String> detectedCategoryFeatures = new HashSet<>(detectedFeatures);
      detectedCategoryFeatures.retainAll(categoryFeatures);

      if (detectedCategoryFeatures.isEmpty()) {
        gaps.add(new CoverageGap(
            CoverageGapType.CATEGORY_UNTESTED,
            String.format("Category %s not tested", category),
            categoryFeatures,
            Set.of(RuntimeType.values()),
            GapSeverity.MEDIUM
        ));
      }
    }

    return gaps;
  }

  private FeatureInteractionAnalysis analyzeFeatureInteractions(
      final Set<String> detectedFeatures,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults,
      final BehavioralAnalysisResult behavioralResults) {

    final Map<String, Set<String>> featureCombinations = new HashMap<>();
    final List<String> problematicInteractions = new ArrayList<>();

    // Analyze feature combinations based on detected features
    final List<String> featureList = new ArrayList<>(detectedFeatures);
    for (int i = 0; i < featureList.size(); i++) {
      for (int j = i + 1; j < featureList.size(); j++) {
        final String feature1 = featureList.get(i);
        final String feature2 = featureList.get(j);
        final String combinationKey = feature1 + "+" + feature2;

        featureCombinations.put(combinationKey, Set.of(feature1, feature2));

        // Check if this combination causes issues based on behavioral results
        if (behavioralResults.getConsistencyScore() < 0.8) {
          problematicInteractions.add(combinationKey);
        }
      }
    }

    return new FeatureInteractionAnalysis(
        featureCombinations,
        problematicInteractions,
        calculateInteractionComplexity(detectedFeatures)
    );
  }

  private void updateCoverageTracking(
      final String testName,
      final Set<String> detectedFeatures,
      final Map<RuntimeType, Set<String>> runtimeFeatureCoverage) {

    for (final Map.Entry<String, Set<String>> categoryEntry : WASM_FEATURE_CATEGORIES.entrySet()) {
      final String category = categoryEntry.getKey();
      final Set<String> categoryFeatures = categoryEntry.getValue();
      final CoverageTracker tracker = coverageTrackers.get(category);

      for (final String feature : categoryFeatures) {
        if (detectedFeatures.contains(feature)) {
          tracker.addCoveredFeature(feature, testName);

          for (final Map.Entry<RuntimeType, Set<String>> runtimeEntry : runtimeFeatureCoverage.entrySet()) {
            if (runtimeEntry.getValue().contains(feature)) {
              tracker.addRuntimeCoverage(feature, runtimeEntry.getKey());
            }
          }
        }
      }
    }
  }

  private Map<String, Double> calculateCategoryCompleteness() {
    final Map<String, Double> completeness = new HashMap<>();

    for (final Map.Entry<String, Set<String>> categoryEntry : WASM_FEATURE_CATEGORIES.entrySet()) {
      final String category = categoryEntry.getKey();
      final Set<String> categoryFeatures = categoryEntry.getValue();
      final CoverageTracker tracker = coverageTrackers.get(category);

      final double percentage = categoryFeatures.isEmpty() ? 100.0 :
          (double) tracker.getCoveredFeatures().size() / categoryFeatures.size() * 100.0;

      completeness.put(category, percentage);
    }

    return completeness;
  }

  private List<String> identifyUncoveredFeatures() {
    final List<String> uncovered = new ArrayList<>();

    for (final Map.Entry<String, Set<String>> categoryEntry : WASM_FEATURE_CATEGORIES.entrySet()) {
      final Set<String> categoryFeatures = categoryEntry.getValue();
      final CoverageTracker tracker = coverageTrackers.get(categoryEntry.getKey());

      for (final String feature : categoryFeatures) {
        if (!tracker.getCoveredFeatures().contains(feature)) {
          uncovered.add(feature);
        }
      }
    }

    return uncovered;
  }

  private Map<RuntimeType, Double> calculateRuntimeCoverageScores() {
    final Map<RuntimeType, Double> scores = new EnumMap<>(RuntimeType.class);

    for (final RuntimeType runtime : RuntimeType.values()) {
      double totalCoverage = 0.0;
      int categoryCount = 0;

      for (final CoverageTracker tracker : coverageTrackers.values()) {
        final double categoryScore = tracker.getRuntimeCoverageScore(runtime);
        totalCoverage += categoryScore;
        categoryCount++;
      }

      final double averageScore = categoryCount > 0 ? totalCoverage / categoryCount : 0.0;
      scores.put(runtime, averageScore);
    }

    return scores;
  }

  private List<CoverageRecommendation> generateCoverageRecommendations() {
    final List<CoverageRecommendation> recommendations = new ArrayList<>();

    // Recommend testing for uncovered categories
    for (final Map.Entry<String, Double> categoryEntry : calculateCategoryCompleteness().entrySet()) {
      final String category = categoryEntry.getKey();
      final double completeness = categoryEntry.getValue();

      if (completeness < 50.0) {
        recommendations.add(new CoverageRecommendation(
            RecommendationType.INCREASE_CATEGORY_COVERAGE,
            String.format("Increase test coverage for %s category (currently %.1f%%)", category, completeness),
            RecommendationPriority.HIGH,
            Set.of(category)
        ));
      } else if (completeness < 80.0) {
        recommendations.add(new CoverageRecommendation(
            RecommendationType.IMPROVE_CATEGORY_COVERAGE,
            String.format("Improve test coverage for %s category (currently %.1f%%)", category, completeness),
            RecommendationPriority.MEDIUM,
            Set.of(category)
        ));
      }
    }

    // Recommend runtime-specific improvements
    final Map<RuntimeType, Double> runtimeScores = calculateRuntimeCoverageScores();
    for (final Map.Entry<RuntimeType, Double> runtimeEntry : runtimeScores.entrySet()) {
      final RuntimeType runtime = runtimeEntry.getKey();
      final double score = runtimeEntry.getValue();

      if (score < 70.0) {
        recommendations.add(new CoverageRecommendation(
            RecommendationType.IMPROVE_RUNTIME_COVERAGE,
            String.format("Improve test coverage for %s runtime (currently %.1f%%)", runtime, score),
            RecommendationPriority.HIGH,
            Set.of(runtime.name())
        ));
      }
    }

    return recommendations;
  }

  private CoverageTrend analyzeCoverageTrend() {
    // Simplified trend analysis - in a real implementation, this would track historical data
    final GlobalCoverageStatistics current = getGlobalCoverageStatistics();

    return new CoverageTrend(
        current.getOverallCoveragePercentage(),
        0.0, // Previous coverage (would be tracked historically)
        current.getOverallCoveragePercentage(), // Change (simplified)
        TrendDirection.STABLE
    );
  }

  private GapSeverity determineSeverity(
      final Set<String> missingFeatures,
      final BehavioralAnalysisResult behavioralResults) {

    // High severity if core features are missing
    final Set<String> coreFeatures = WASM_FEATURE_CATEGORIES.get("CORE");
    if (missingFeatures.stream().anyMatch(coreFeatures::contains)) {
      return GapSeverity.HIGH;
    }

    // High severity if many features are missing and there are behavioral issues
    if (missingFeatures.size() > 5 && behavioralResults.getConsistencyScore() < 0.7) {
      return GapSeverity.HIGH;
    }

    // Medium severity for moderate number of missing features
    if (missingFeatures.size() > 2) {
      return GapSeverity.MEDIUM;
    }

    return GapSeverity.LOW;
  }

  private double calculateInteractionComplexity(final Set<String> features) {
    // Simplified complexity calculation based on number of possible interactions
    final int featureCount = features.size();
    if (featureCount <= 1) {
      return 1.0;
    }

    // Calculate combinations: n! / (2! * (n-2)!)
    final double combinations = featureCount * (featureCount - 1) / 2.0;

    // Normalize to a reasonable scale
    return Math.min(10.0, Math.log10(combinations + 1) * 2);
  }

  /** Tracks coverage for a specific feature category. */
  private static final class CoverageTracker {
    private final String category;
    private final Set<String> coveredFeatures;
    private final Map<String, Set<String>> featureToTests;
    private final Map<String, Set<RuntimeType>> featureToRuntimes;

    public CoverageTracker(final String category) {
      this.category = category;
      this.coveredFeatures = ConcurrentHashMap.newKeySet();
      this.featureToTests = new ConcurrentHashMap<>();
      this.featureToRuntimes = new ConcurrentHashMap<>();
    }

    public void addCoveredFeature(final String feature, final String testName) {
      coveredFeatures.add(feature);
      featureToTests.computeIfAbsent(feature, k -> ConcurrentHashMap.newKeySet()).add(testName);
    }

    public void addRuntimeCoverage(final String feature, final RuntimeType runtime) {
      featureToRuntimes.computeIfAbsent(feature, k -> ConcurrentHashMap.newKeySet()).add(runtime);
    }

    public Set<String> getCoveredFeatures() {
      return Set.copyOf(coveredFeatures);
    }

    public double getRuntimeCoverageScore(final RuntimeType runtime) {
      final Set<String> categoryFeatures = WASM_FEATURE_CATEGORIES.get(category);
      if (categoryFeatures.isEmpty()) {
        return 100.0;
      }

      final long runtimeCoveredCount = categoryFeatures.stream()
          .mapToLong(feature -> {
            final Set<RuntimeType> runtimes = featureToRuntimes.get(feature);
            return runtimes != null && runtimes.contains(runtime) ? 1 : 0;
          })
          .sum();

      return (double) runtimeCoveredCount / categoryFeatures.size() * 100.0;
    }
  }
}