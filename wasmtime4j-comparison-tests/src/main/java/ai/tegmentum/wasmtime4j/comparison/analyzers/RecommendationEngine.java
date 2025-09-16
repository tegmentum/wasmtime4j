package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
 * Comprehensive recommendation engine that analyzes behavioral discrepancies, performance issues,
 * and coverage gaps to generate actionable insights for addressing identified compatibility issues
 * and optimization opportunities.
 *
 * <p>Key functionality includes:
 * <ul>
 *   <li>Rule-based analysis for common compatibility issue patterns</li>
 *   <li>Priority scoring based on impact assessment and frequency analysis</li>
 *   <li>Actionable recommendation generation with specific implementation guidance</li>
 *   <li>Issue categorization and severity assessment</li>
 *   <li>Integration with behavioral, performance, and coverage analysis results</li>
 * </ul>
 *
 * <p>The engine uses a sophisticated rule-based system combined with historical analysis
 * to provide targeted recommendations for improving runtime compatibility and performance.
 *
 * @since 1.0.0
 */
public final class RecommendationEngine {
  private static final Logger LOGGER = Logger.getLogger(RecommendationEngine.class.getName());

  // Rule weights for priority scoring
  private static final double CRITICAL_BEHAVIORAL_WEIGHT = 1.0;
  private static final double PERFORMANCE_REGRESSION_WEIGHT = 0.8;
  private static final double COVERAGE_GAP_WEIGHT = 0.6;
  private static final double FREQUENCY_WEIGHT = 0.4;

  // Issue severity thresholds
  private static final double HIGH_SEVERITY_THRESHOLD = 0.8;
  private static final double MEDIUM_SEVERITY_THRESHOLD = 0.5;

  private final Map<String, IssuePattern> knownPatterns;
  private final Map<String, RecommendationTemplate> recommendationTemplates;
  private final IssueFrequencyTracker frequencyTracker;

  /** Creates a new RecommendationEngine with default patterns and templates. */
  public RecommendationEngine() {
    this.knownPatterns = new ConcurrentHashMap<>();
    this.recommendationTemplates = new ConcurrentHashMap<>();
    this.frequencyTracker = new IssueFrequencyTracker();
    initializeKnownPatterns();
    initializeRecommendationTemplates();
  }

  /**
   * Generates comprehensive recommendations based on analysis results from multiple sources.
   *
   * @param testName the name of the test being analyzed
   * @param behavioralResults the behavioral analysis results
   * @param performanceResults the performance analysis results
   * @param coverageResults the coverage analysis results
   * @return comprehensive recommendation result with prioritized actionable insights
   */
  public RecommendationResult generateRecommendations(
      final String testName,
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults) {

    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(behavioralResults, "behavioralResults cannot be null");
    Objects.requireNonNull(performanceResults, "performanceResults cannot be null");
    Objects.requireNonNull(coverageResults, "coverageResults cannot be null");

    LOGGER.info(String.format("Generating recommendations for test: %s", testName));

    final RecommendationResult.Builder resultBuilder = new RecommendationResult.Builder(testName);

    // Analyze behavioral issues and generate recommendations
    final List<ActionableRecommendation> behavioralRecommendations =
        analyzeBehavioralIssues(behavioralResults);
    resultBuilder.behavioralRecommendations(behavioralRecommendations);

    // Analyze performance issues and generate recommendations
    final List<ActionableRecommendation> performanceRecommendations =
        analyzePerformanceIssues(performanceResults);
    resultBuilder.performanceRecommendations(performanceRecommendations);

    // Analyze coverage gaps and generate recommendations
    final List<ActionableRecommendation> coverageRecommendations =
        analyzeCoverageGaps(coverageResults);
    resultBuilder.coverageRecommendations(coverageRecommendations);

    // Generate integration recommendations based on combined analysis
    final List<ActionableRecommendation> integrationRecommendations =
        analyzeIntegrationIssues(behavioralResults, performanceResults, coverageResults);
    resultBuilder.integrationRecommendations(integrationRecommendations);

    // Calculate overall priority scores and sort recommendations
    final List<ActionableRecommendation> allRecommendations = new ArrayList<>();
    allRecommendations.addAll(behavioralRecommendations);
    allRecommendations.addAll(performanceRecommendations);
    allRecommendations.addAll(coverageRecommendations);
    allRecommendations.addAll(integrationRecommendations);

    final List<ActionableRecommendation> prioritizedRecommendations =
        prioritizeRecommendations(allRecommendations, testName);
    resultBuilder.prioritizedRecommendations(prioritizedRecommendations);

    // Generate summary insights
    final RecommendationSummary summary = generateSummary(prioritizedRecommendations);
    resultBuilder.summary(summary);

    // Update frequency tracking
    updateFrequencyTracking(allRecommendations);

    final RecommendationResult result = resultBuilder.build();

    LOGGER.info(String.format("Generated %d recommendations for %s (High: %d, Medium: %d, Low: %d)",
        prioritizedRecommendations.size(), testName,
        summary.getHighPriorityCount(), summary.getMediumPriorityCount(), summary.getLowPriorityCount()));

    return result;
  }

  /**
   * Generates batch recommendations for multiple test results.
   *
   * @param testResults map of test names to their analysis results
   * @return batch recommendation result with aggregated insights
   */
  public BatchRecommendationResult generateBatchRecommendations(
      final Map<String, TestAnalysisResults> testResults) {

    Objects.requireNonNull(testResults, "testResults cannot be null");

    LOGGER.info(String.format("Generating batch recommendations for %d tests", testResults.size()));

    final Map<String, RecommendationResult> testRecommendations = new HashMap<>();
    final List<ActionableRecommendation> commonIssues = new ArrayList<>();
    final Map<IssueCategory, Integer> issueCategoryCounts = new HashMap<>();

    // Generate recommendations for each test
    for (final Map.Entry<String, TestAnalysisResults> entry : testResults.entrySet()) {
      final String testName = entry.getKey();
      final TestAnalysisResults results = entry.getValue();

      final RecommendationResult testRecommendation = generateRecommendations(
          testName,
          results.getBehavioralResults(),
          results.getPerformanceResults(),
          results.getCoverageResults()
      );

      testRecommendations.put(testName, testRecommendation);

      // Track issue categories
      for (final ActionableRecommendation recommendation : testRecommendation.getPrioritizedRecommendations()) {
        issueCategoryCounts.merge(recommendation.getCategory(), 1, Integer::sum);
      }
    }

    // Identify common issues across multiple tests
    final Map<String, Integer> patternFrequency = new HashMap<>();
    for (final RecommendationResult result : testRecommendations.values()) {
      for (final ActionableRecommendation recommendation : result.getPrioritizedRecommendations()) {
        final String pattern = recommendation.getIssuePattern();
        patternFrequency.merge(pattern, 1, Integer::sum);
      }
    }

    // Create common issue recommendations for frequently occurring patterns
    for (final Map.Entry<String, Integer> entry : patternFrequency.entrySet()) {
      if (entry.getValue() >= Math.max(2, testResults.size() / 3)) { // Appears in at least 1/3 of tests
        final String pattern = entry.getKey();
        final IssuePattern knownPattern = knownPatterns.get(pattern);
        if (knownPattern != null) {
          commonIssues.add(createCommonIssueRecommendation(pattern, entry.getValue(), knownPattern));
        }
      }
    }

    return new BatchRecommendationResult(
        testRecommendations,
        commonIssues,
        issueCategoryCounts,
        generateBatchSummary(testRecommendations.values()),
        Instant.now()
    );
  }

  /**
   * Gets recommendations for a specific issue category.
   *
   * @param category the issue category
   * @return list of relevant recommendations
   */
  public List<ActionableRecommendation> getRecommendationsForCategory(final IssueCategory category) {
    return recommendationTemplates.values().stream()
        .filter(template -> template.getCategory() == category)
        .map(template -> new ActionableRecommendation(
            template.getTitle(),
            template.getDescription(),
            template.getImplementationSteps(),
            template.getCategory(),
            IssueSeverity.MEDIUM, // Default severity
            0.5, // Default priority score
            template.getAffectedRuntimes(),
            "general_guidance"
        ))
        .collect(Collectors.toList());
  }

  /** Clears all frequency tracking data. */
  public void clearFrequencyData() {
    frequencyTracker.clear();
    LOGGER.info("Cleared recommendation frequency tracking data");
  }

  private void initializeKnownPatterns() {
    // Behavioral patterns
    knownPatterns.put("inconsistent_exception_handling", new IssuePattern(
        "inconsistent_exception_handling",
        "Different exception types or messages across runtimes",
        IssueCategory.BEHAVIORAL,
        Set.of("exception_mapping", "error_handling", "runtime_compatibility")
    ));

    knownPatterns.put("floating_point_precision_differences", new IssuePattern(
        "floating_point_precision_differences",
        "Floating-point calculation results differ between runtimes",
        IssueCategory.BEHAVIORAL,
        Set.of("floating_point", "precision", "mathematical_operations")
    ));

    knownPatterns.put("memory_layout_inconsistencies", new IssuePattern(
        "memory_layout_inconsistencies",
        "Memory allocation or layout differs across implementations",
        IssueCategory.BEHAVIORAL,
        Set.of("memory_management", "allocation", "layout")
    ));

    // Performance patterns
    knownPatterns.put("jni_overhead_significant", new IssuePattern(
        "jni_overhead_significant",
        "JNI implementation shows significant performance overhead",
        IssueCategory.PERFORMANCE,
        Set.of("jni", "overhead", "performance")
    ));

    knownPatterns.put("panama_optimization_opportunity", new IssuePattern(
        "panama_optimization_opportunity",
        "Panama implementation could benefit from optimization",
        IssueCategory.PERFORMANCE,
        Set.of("panama", "optimization", "performance")
    ));

    knownPatterns.put("memory_usage_excessive", new IssuePattern(
        "memory_usage_excessive",
        "Memory usage is higher than expected for the operation",
        IssueCategory.PERFORMANCE,
        Set.of("memory", "usage", "optimization")
    ));

    // Coverage patterns
    knownPatterns.put("feature_not_implemented", new IssuePattern(
        "feature_not_implemented",
        "WebAssembly feature is not implemented in one or more runtimes",
        IssueCategory.COVERAGE,
        Set.of("feature", "implementation", "coverage")
    ));

    knownPatterns.put("incomplete_wasi_support", new IssuePattern(
        "incomplete_wasi_support",
        "WASI feature support is incomplete",
        IssueCategory.COVERAGE,
        Set.of("wasi", "features", "implementation")
    ));
  }

  private void initializeRecommendationTemplates() {
    // Behavioral recommendation templates
    recommendationTemplates.put("fix_exception_mapping", new RecommendationTemplate(
        "Fix Exception Mapping",
        "Standardize exception types and messages across runtime implementations",
        List.of(
            "Review exception handling in native implementations",
            "Create unified exception mapping layer",
            "Implement consistent error message formatting",
            "Add exception type validation tests"
        ),
        IssueCategory.BEHAVIORAL,
        Set.of(RuntimeType.JNI, RuntimeType.PANAMA)
    ));

    recommendationTemplates.put("improve_floating_point_precision", new RecommendationTemplate(
        "Improve Floating-Point Precision",
        "Address floating-point precision differences between runtimes",
        List.of(
            "Review floating-point implementation in native code",
            "Ensure consistent rounding modes across runtimes",
            "Add tolerance-based comparison for floating-point results",
            "Document expected precision differences"
        ),
        IssueCategory.BEHAVIORAL,
        Set.of(RuntimeType.JNI, RuntimeType.PANAMA)
    ));

    // Performance recommendation templates
    recommendationTemplates.put("optimize_jni_calls", new RecommendationTemplate(
        "Optimize JNI Call Overhead",
        "Reduce JNI call overhead through batching and optimization",
        List.of(
            "Implement JNI call batching where possible",
            "Cache frequently used JNI references",
            "Minimize data marshalling overhead",
            "Profile and optimize critical JNI paths"
        ),
        IssueCategory.PERFORMANCE,
        Set.of(RuntimeType.JNI)
    ));

    recommendationTemplates.put("optimize_panama_performance", new RecommendationTemplate(
        "Optimize Panama Performance",
        "Improve Panama Foreign Function API performance",
        List.of(
            "Use arena-based memory management",
            "Optimize memory layout for Panama FFI",
            "Implement efficient data transfer mechanisms",
            "Profile Panama-specific performance bottlenecks"
        ),
        IssueCategory.PERFORMANCE,
        Set.of(RuntimeType.PANAMA)
    ));

    // Coverage recommendation templates
    recommendationTemplates.put("implement_missing_features", new RecommendationTemplate(
        "Implement Missing Features",
        "Add support for missing WebAssembly features",
        List.of(
            "Identify specific missing features from test results",
            "Prioritize features based on usage and importance",
            "Implement features in native runtime",
            "Add comprehensive tests for new features"
        ),
        IssueCategory.COVERAGE,
        Set.of(RuntimeType.JNI, RuntimeType.PANAMA)
    ));

    recommendationTemplates.put("improve_wasi_support", new RecommendationTemplate(
        "Improve WASI Support",
        "Enhance WASI feature implementation and compatibility",
        List.of(
            "Audit current WASI feature implementation",
            "Implement missing WASI functions",
            "Ensure WASI compatibility across runtimes",
            "Add WASI-specific integration tests"
        ),
        IssueCategory.COVERAGE,
        Set.of(RuntimeType.JNI, RuntimeType.PANAMA)
    ));
  }

  private List<ActionableRecommendation> analyzeBehavioralIssues(
      final BehavioralAnalysisResult behavioralResults) {

    final List<ActionableRecommendation> recommendations = new ArrayList<>();

    // Analyze discrepancies for known patterns
    for (final BehavioralDiscrepancy discrepancy : behavioralResults.getDiscrepancies()) {
      final String patternKey = identifyDiscrepancyPattern(discrepancy);
      if (patternKey != null) {
        final ActionableRecommendation recommendation = createRecommendationFromPattern(
            patternKey, discrepancy.getSeverity(), discrepancy.getAffectedRuntimes()
        );
        if (recommendation != null) {
          recommendations.add(recommendation);
        }
      }
    }

    // Analyze consistency score
    if (behavioralResults.getConsistencyScore() < 0.8) {
      recommendations.add(new ActionableRecommendation(
          "Improve Runtime Consistency",
          String.format("Overall consistency score is low (%.2f). Investigate runtime differences.",
              behavioralResults.getConsistencyScore()),
          List.of(
              "Review all behavioral discrepancies",
              "Identify common patterns in inconsistencies",
              "Implement runtime-specific fixes",
              "Add regression tests for consistency"
          ),
          IssueCategory.BEHAVIORAL,
          IssueSeverity.HIGH,
          calculatePriorityScore(behavioralResults.getConsistencyScore(), 1.0),
          Set.of(RuntimeType.values()),
          "consistency_improvement"
      ));
    }

    return recommendations;
  }

  private List<ActionableRecommendation> analyzePerformanceIssues(
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults) {

    final List<ActionableRecommendation> recommendations = new ArrayList<>();

    // Analyze significant performance differences
    for (final String difference : performanceResults.getSignificantDifferences()) {
      recommendations.add(new ActionableRecommendation(
          "Address Performance Difference",
          String.format("Significant performance difference detected: %s", difference),
          List.of(
              "Profile the slower runtime implementation",
              "Identify performance bottlenecks",
              "Implement targeted optimizations",
              "Validate performance improvements"
          ),
          IssueCategory.PERFORMANCE,
          IssueSeverity.MEDIUM,
          0.7,
          Set.of(RuntimeType.values()),
          "performance_difference"
      ));
    }

    // Analyze regression warnings
    for (final String regression : performanceResults.getRegressionWarnings()) {
      recommendations.add(new ActionableRecommendation(
          "Fix Performance Regression",
          String.format("Performance regression detected: %s", regression),
          List.of(
              "Identify the cause of performance regression",
              "Revert problematic changes if possible",
              "Implement performance fixes",
              "Add performance monitoring to prevent future regressions"
          ),
          IssueCategory.PERFORMANCE,
          IssueSeverity.HIGH,
          0.9,
          Set.of(RuntimeType.values()),
          "performance_regression"
      ));
    }

    // Analyze overhead
    final PerformanceAnalyzer.OverheadAnalysis overheadAnalysis = performanceResults.getOverheadAnalysis();
    if (overheadAnalysis.hasSignificantOverhead()) {
      for (final Map.Entry<String, Double> entry : overheadAnalysis.getRuntimeOverheads().entrySet()) {
        if (entry.getValue() > 0.2) { // 20% overhead threshold
          recommendations.add(new ActionableRecommendation(
              "Reduce Runtime Overhead",
              String.format("Runtime %s has %.1f%% overhead compared to baseline",
                  entry.getKey(), entry.getValue() * 100),
              List.of(
                  "Profile runtime-specific overhead sources",
                  "Optimize critical performance paths",
                  "Reduce data marshalling costs",
                  "Implement runtime-specific optimizations"
              ),
              IssueCategory.PERFORMANCE,
              IssueSeverity.MEDIUM,
              calculatePriorityScore(entry.getValue(), 0.8),
              Set.of(RuntimeType.valueOf(entry.getKey().toUpperCase())),
              "runtime_overhead"
          ));
        }
      }
    }

    return recommendations;
  }

  private List<ActionableRecommendation> analyzeCoverageGaps(final CoverageAnalysisResult coverageResults) {
    final List<ActionableRecommendation> recommendations = new ArrayList<>();

    // Analyze coverage gaps
    for (final CoverageGap gap : coverageResults.getCoverageGaps()) {
      final IssueSeverity severity = mapGapSeverityToIssueSeverity(gap.getSeverity());
      final double priorityScore = calculateGapPriorityScore(gap);

      recommendations.add(new ActionableRecommendation(
          "Address Coverage Gap",
          String.format("Coverage gap detected: %s", gap.getDescription()),
          List.of(
              "Identify missing test scenarios",
              "Implement tests for uncovered features",
              "Ensure runtime compatibility for missing features",
              "Validate coverage improvements"
          ),
          IssueCategory.COVERAGE,
          severity,
          priorityScore,
          gap.getAffectedRuntimes(),
          "coverage_gap"
      ));
    }

    // Analyze overall coverage
    if (coverageResults.getCoverageMetrics().getOverallCoveragePercentage() < 80.0) {
      recommendations.add(new ActionableRecommendation(
          "Improve Overall Test Coverage",
          String.format("Overall coverage is low (%.1f%%). Increase test coverage across all features.",
              coverageResults.getCoverageMetrics().getOverallCoveragePercentage()),
          List.of(
              "Audit current test coverage",
              "Identify untested WebAssembly features",
              "Implement comprehensive feature tests",
              "Ensure cross-runtime compatibility testing"
          ),
          IssueCategory.COVERAGE,
          IssueSeverity.HIGH,
          0.8,
          Set.of(RuntimeType.values()),
          "low_overall_coverage"
      ));
    }

    return recommendations;
  }

  private List<ActionableRecommendation> analyzeIntegrationIssues(
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults) {

    final List<ActionableRecommendation> recommendations = new ArrayList<>();

    // Analyze correlation between behavioral issues and performance problems
    if (behavioralResults.getConsistencyScore() < 0.7 && performanceResults.hasRegressions()) {
      recommendations.add(new ActionableRecommendation(
          "Investigate Behavioral-Performance Correlation",
          "Both behavioral inconsistencies and performance regressions detected, which may be related",
          List.of(
              "Investigate if performance optimizations caused behavioral changes",
              "Review recent implementation changes",
              "Ensure behavioral consistency is maintained during optimization",
              "Add integrated behavioral-performance tests"
          ),
          IssueCategory.INTEGRATION,
          IssueSeverity.HIGH,
          0.9,
          Set.of(RuntimeType.values()),
          "behavioral_performance_correlation"
      ));
    }

    // Analyze coverage gaps with behavioral issues
    final long highSeverityGaps = coverageResults.getHighSeverityGapCount();
    if (highSeverityGaps > 0 && !behavioralResults.isCompatible()) {
      recommendations.add(new ActionableRecommendation(
          "Address Coverage and Compatibility Issues",
          "High-severity coverage gaps combined with behavioral incompatibilities require immediate attention",
          List.of(
              "Prioritize implementing missing critical features",
              "Ensure behavioral consistency for newly implemented features",
              "Add comprehensive integration tests",
              "Validate runtime compatibility after implementation"
          ),
          IssueCategory.INTEGRATION,
          IssueSeverity.HIGH,
          0.95,
          Set.of(RuntimeType.values()),
          "coverage_compatibility_issues"
      ));
    }

    return recommendations;
  }

  private List<ActionableRecommendation> prioritizeRecommendations(
      final List<ActionableRecommendation> recommendations,
      final String testName) {

    // Calculate priority scores considering frequency
    final List<ActionableRecommendation> prioritized = new ArrayList<>();
    for (final ActionableRecommendation recommendation : recommendations) {
      final double frequencyBoost = frequencyTracker.getFrequencyBoost(recommendation.getIssuePattern());
      final double adjustedScore = Math.min(1.0, recommendation.getPriorityScore() + frequencyBoost);

      prioritized.add(new ActionableRecommendation(
          recommendation.getTitle(),
          recommendation.getDescription(),
          recommendation.getImplementationSteps(),
          recommendation.getCategory(),
          recommendation.getSeverity(),
          adjustedScore,
          recommendation.getAffectedRuntimes(),
          recommendation.getIssuePattern()
      ));
    }

    // Sort by priority score (descending)
    prioritized.sort((r1, r2) -> Double.compare(r2.getPriorityScore(), r1.getPriorityScore()));

    return prioritized;
  }

  private RecommendationSummary generateSummary(final List<ActionableRecommendation> recommendations) {
    int highPriority = 0;
    int mediumPriority = 0;
    int lowPriority = 0;

    final Map<IssueCategory, Integer> categoryCount = new HashMap<>();

    for (final ActionableRecommendation recommendation : recommendations) {
      // Count by severity
      switch (recommendation.getSeverity()) {
        case HIGH:
          highPriority++;
          break;
        case MEDIUM:
          mediumPriority++;
          break;
        case LOW:
          lowPriority++;
          break;
      }

      // Count by category
      categoryCount.merge(recommendation.getCategory(), 1, Integer::sum);
    }

    return new RecommendationSummary(
        recommendations.size(),
        highPriority,
        mediumPriority,
        lowPriority,
        categoryCount
    );
  }

  private void updateFrequencyTracking(final List<ActionableRecommendation> recommendations) {
    for (final ActionableRecommendation recommendation : recommendations) {
      frequencyTracker.recordIssue(recommendation.getIssuePattern());
    }
  }

  private String identifyDiscrepancyPattern(final BehavioralDiscrepancy discrepancy) {
    final String description = discrepancy.getDescription().toLowerCase();

    if (description.contains("exception") || description.contains("error")) {
      return "inconsistent_exception_handling";
    }
    if (description.contains("floating") || description.contains("precision")) {
      return "floating_point_precision_differences";
    }
    if (description.contains("memory") || description.contains("allocation")) {
      return "memory_layout_inconsistencies";
    }

    return null; // Unknown pattern
  }

  private ActionableRecommendation createRecommendationFromPattern(
      final String patternKey,
      final DiscrepancySeverity discrepancySeverity,
      final Set<RuntimeType> affectedRuntimes) {

    final IssuePattern pattern = knownPatterns.get(patternKey);
    if (pattern == null) {
      return null;
    }

    final RecommendationTemplate template = findTemplateForPattern(pattern);
    if (template == null) {
      return null;
    }

    final IssueSeverity severity = mapDiscrepancySeverityToIssueSeverity(discrepancySeverity);
    final double priorityScore = calculatePriorityScore(severity, pattern.getCategory());

    return new ActionableRecommendation(
        template.getTitle(),
        template.getDescription(),
        template.getImplementationSteps(),
        template.getCategory(),
        severity,
        priorityScore,
        affectedRuntimes,
        patternKey
    );
  }

  private RecommendationTemplate findTemplateForPattern(final IssuePattern pattern) {
    for (final RecommendationTemplate template : recommendationTemplates.values()) {
      if (template.getCategory() == pattern.getCategory()) {
        // Simple heuristic - find template with matching keywords
        final String patternKeywords = String.join(" ", pattern.getKeywords());
        if (template.getDescription().toLowerCase().contains(patternKeywords.toLowerCase())) {
          return template;
        }
      }
    }
    return null;
  }

  private ActionableRecommendation createCommonIssueRecommendation(
      final String pattern,
      final int frequency,
      final IssuePattern knownPattern) {

    return new ActionableRecommendation(
        "Address Common Issue Pattern",
        String.format("Pattern '%s' appears in %d tests and should be addressed systematically",
            pattern, frequency),
        List.of(
            "Analyze all occurrences of this pattern",
            "Implement a systematic fix",
            "Add preventive measures",
            "Update testing to catch similar issues"
        ),
        knownPattern.getCategory(),
        IssueSeverity.HIGH,
        0.9 + Math.min(0.1, frequency * 0.01), // Boost score based on frequency
        Set.of(RuntimeType.values()),
        pattern
    );
  }

  private BatchRecommendationSummary generateBatchSummary(
      final Iterable<RecommendationResult> results) {
    int totalRecommendations = 0;
    int totalHighPriority = 0;
    final Map<IssueCategory, Integer> categoryTotals = new HashMap<>();

    for (final RecommendationResult result : results) {
      final RecommendationSummary summary = result.getSummary();
      totalRecommendations += summary.getTotalRecommendations();
      totalHighPriority += summary.getHighPriorityCount();

      for (final Map.Entry<IssueCategory, Integer> entry : summary.getCategoryBreakdown().entrySet()) {
        categoryTotals.merge(entry.getKey(), entry.getValue(), Integer::sum);
      }
    }

    return new BatchRecommendationSummary(totalRecommendations, totalHighPriority, categoryTotals);
  }

  private double calculatePriorityScore(final double value, final double weight) {
    return Math.min(1.0, value * weight);
  }

  private double calculatePriorityScore(final IssueSeverity severity, final IssueCategory category) {
    double baseScore = 0.5;

    switch (severity) {
      case HIGH:
        baseScore = 0.9;
        break;
      case MEDIUM:
        baseScore = 0.6;
        break;
      case LOW:
        baseScore = 0.3;
        break;
    }

    // Adjust based on category
    switch (category) {
      case BEHAVIORAL:
        baseScore *= CRITICAL_BEHAVIORAL_WEIGHT;
        break;
      case PERFORMANCE:
        baseScore *= PERFORMANCE_REGRESSION_WEIGHT;
        break;
      case COVERAGE:
        baseScore *= COVERAGE_GAP_WEIGHT;
        break;
    }

    return Math.min(1.0, baseScore);
  }

  private double calculateGapPriorityScore(final CoverageGap gap) {
    double score = 0.5;

    switch (gap.getSeverity()) {
      case HIGH:
        score = 0.8;
        break;
      case MEDIUM:
        score = 0.6;
        break;
      case LOW:
        score = 0.4;
        break;
    }

    // Boost score for gaps affecting multiple runtimes
    if (gap.getAffectedRuntimes().size() > 1) {
      score += 0.1;
    }

    return Math.min(1.0, score);
  }

  private IssueSeverity mapDiscrepancySeverityToIssueSeverity(final DiscrepancySeverity discrepancySeverity) {
    switch (discrepancySeverity) {
      case CRITICAL:
        return IssueSeverity.HIGH;
      case MAJOR:
        return IssueSeverity.HIGH;
      case MINOR:
        return IssueSeverity.MEDIUM;
      case INFO:
        return IssueSeverity.LOW;
      default:
        return IssueSeverity.MEDIUM;
    }
  }

  private IssueSeverity mapGapSeverityToIssueSeverity(final GapSeverity gapSeverity) {
    switch (gapSeverity) {
      case HIGH:
        return IssueSeverity.HIGH;
      case MEDIUM:
        return IssueSeverity.MEDIUM;
      case LOW:
        return IssueSeverity.LOW;
      default:
        return IssueSeverity.MEDIUM;
    }
  }

  /** Tracks the frequency of issues to help with prioritization. */
  private static final class IssueFrequencyTracker {
    private final Map<String, Integer> issueFrequencies = new ConcurrentHashMap<>();

    public void recordIssue(final String issuePattern) {
      issueFrequencies.merge(issuePattern, 1, Integer::sum);
    }

    public double getFrequencyBoost(final String issuePattern) {
      final int frequency = issueFrequencies.getOrDefault(issuePattern, 0);
      return Math.min(0.2, frequency * 0.05); // Max 20% boost, 5% per occurrence
    }

    public void clear() {
      issueFrequencies.clear();
    }
  }

  /** Represents a known issue pattern for pattern matching. */
  private static final class IssuePattern {
    private final String patternId;
    private final String description;
    private final IssueCategory category;
    private final Set<String> keywords;

    public IssuePattern(
        final String patternId,
        final String description,
        final IssueCategory category,
        final Set<String> keywords) {
      this.patternId = patternId;
      this.description = description;
      this.category = category;
      this.keywords = Set.copyOf(keywords);
    }

    public String getPatternId() {
      return patternId;
    }

    public String getDescription() {
      return description;
    }

    public IssueCategory getCategory() {
      return category;
    }

    public Set<String> getKeywords() {
      return keywords;
    }
  }

  /** Template for generating recommendations. */
  private static final class RecommendationTemplate {
    private final String title;
    private final String description;
    private final List<String> implementationSteps;
    private final IssueCategory category;
    private final Set<RuntimeType> affectedRuntimes;

    public RecommendationTemplate(
        final String title,
        final String description,
        final List<String> implementationSteps,
        final IssueCategory category,
        final Set<RuntimeType> affectedRuntimes) {
      this.title = title;
      this.description = description;
      this.implementationSteps = List.copyOf(implementationSteps);
      this.category = category;
      this.affectedRuntimes = Set.copyOf(affectedRuntimes);
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public List<String> getImplementationSteps() {
      return implementationSteps;
    }

    public IssueCategory getCategory() {
      return category;
    }

    public Set<RuntimeType> getAffectedRuntimes() {
      return affectedRuntimes;
    }
  }
}