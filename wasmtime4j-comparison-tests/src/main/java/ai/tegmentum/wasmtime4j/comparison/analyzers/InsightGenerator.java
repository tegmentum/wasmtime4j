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
 * Advanced insight generator that performs comprehensive analysis across behavioral, performance,
 * and coverage results to generate deep insights for performance optimization opportunities,
 * runtime-specific recommendations, and strategic development guidance.
 *
 * <p>Key functionality includes:
 * <ul>
 *   <li>Performance optimization opportunity identification</li>
 *   <li>Runtime-specific insight generation</li>
 *   <li>Cross-cutting pattern analysis</li>
 *   <li>Strategic development recommendations</li>
 *   <li>Trend analysis and predictive insights</li>
 * </ul>
 *
 * <p>The generator uses advanced analytics to identify patterns across multiple data sources
 * and provide actionable insights for improving overall system performance and compatibility.
 *
 * @since 1.0.0
 */
public final class InsightGenerator {
  private static final Logger LOGGER = Logger.getLogger(InsightGenerator.class.getName());

  // Analysis thresholds for insight generation
  private static final double SIGNIFICANT_PERFORMANCE_DIFFERENCE_THRESHOLD = 0.15; // 15%
  private static final double OPTIMIZATION_OPPORTUNITY_THRESHOLD = 0.10; // 10%
  private static final double PATTERN_CONFIDENCE_THRESHOLD = 0.75; // 75%
  private static final int MIN_SAMPLE_SIZE_FOR_INSIGHTS = 3;

  private final Map<String, PerformancePattern> performancePatterns;
  private final Map<RuntimeType, RuntimeCharacteristics> runtimeCharacteristics;
  private final TrendTracker trendTracker;

  /** Creates a new InsightGenerator with default configuration. */
  public InsightGenerator() {
    this.performancePatterns = new ConcurrentHashMap<>();
    this.runtimeCharacteristics = new ConcurrentHashMap<>();
    this.trendTracker = new TrendTracker();
    initializeRuntimeCharacteristics();
  }

  /**
   * Generates comprehensive insights from integrated analysis results.
   *
   * @param testName the name of the test being analyzed
   * @param behavioralResults the behavioral analysis results
   * @param performanceResults the performance analysis results
   * @param coverageResults the coverage analysis results
   * @param recommendationResults the recommendation results
   * @return comprehensive insight analysis result
   */
  public InsightAnalysisResult generateInsights(
      final String testName,
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults,
      final RecommendationResult recommendationResults) {

    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(behavioralResults, "behavioralResults cannot be null");
    Objects.requireNonNull(performanceResults, "performanceResults cannot be null");
    Objects.requireNonNull(coverageResults, "coverageResults cannot be null");
    Objects.requireNonNull(recommendationResults, "recommendationResults cannot be null");

    LOGGER.info(String.format("Generating insights for test: %s", testName));

    final InsightAnalysisResult.Builder resultBuilder = new InsightAnalysisResult.Builder(testName);

    // Generate performance optimization insights
    final List<PerformanceInsight> performanceInsights =
        generatePerformanceOptimizationInsights(performanceResults, behavioralResults);
    resultBuilder.performanceInsights(performanceInsights);

    // Generate runtime-specific insights
    final Map<RuntimeType, RuntimeInsight> runtimeInsights =
        generateRuntimeSpecificInsights(behavioralResults, performanceResults, coverageResults);
    resultBuilder.runtimeInsights(runtimeInsights);

    // Generate cross-cutting insights
    final List<CrossCuttingInsight> crossCuttingInsights =
        generateCrossCuttingInsights(behavioralResults, performanceResults, coverageResults);
    resultBuilder.crossCuttingInsights(crossCuttingInsights);

    // Generate strategic insights
    final List<StrategicInsight> strategicInsights =
        generateStrategicInsights(performanceResults, coverageResults, recommendationResults);
    resultBuilder.strategicInsights(strategicInsights);

    // Generate pattern insights
    final List<PatternInsight> patternInsights = generatePatternInsights(testName, performanceResults);
    resultBuilder.patternInsights(patternInsights);

    // Update trend tracking
    updateTrendTracking(testName, performanceResults, behavioralResults);

    // Generate trend insights
    final List<TrendInsight> trendInsights = generateTrendInsights(testName);
    resultBuilder.trendInsights(trendInsights);

    // Calculate insight confidence scores
    final InsightConfidenceMetrics confidenceMetrics = calculateConfidenceMetrics(
        performanceInsights, runtimeInsights, crossCuttingInsights, strategicInsights);
    resultBuilder.confidenceMetrics(confidenceMetrics);

    final InsightAnalysisResult result = resultBuilder.build();

    LOGGER.info(String.format("Generated insights for %s: %d performance, %d runtime, %d cross-cutting, %d strategic",
        testName, performanceInsights.size(), runtimeInsights.size(),
        crossCuttingInsights.size(), strategicInsights.size()));

    return result;
  }

  /**
   * Generates batch insights across multiple test results to identify global patterns.
   *
   * @param testResults map of test names to their analysis results
   * @return batch insight result with global patterns and recommendations
   */
  public BatchInsightResult generateBatchInsights(
      final Map<String, CompleteTestAnalysis> testResults) {

    Objects.requireNonNull(testResults, "testResults cannot be null");

    LOGGER.info(String.format("Generating batch insights for %d tests", testResults.size()));

    final Map<String, InsightAnalysisResult> testInsights = new HashMap<>();
    final List<GlobalPattern> globalPatterns = new ArrayList<>();
    final Map<RuntimeType, RuntimeHealthScore> runtimeHealthScores = new HashMap<>();

    // Generate insights for each test
    for (final Map.Entry<String, CompleteTestAnalysis> entry : testResults.entrySet()) {
      final String testName = entry.getKey();
      final CompleteTestAnalysis analysis = entry.getValue();

      final InsightAnalysisResult testInsight = generateInsights(
          testName,
          analysis.getBehavioralResults(),
          analysis.getPerformanceResults(),
          analysis.getCoverageResults(),
          analysis.getRecommendationResults()
      );

      testInsights.put(testName, testInsight);
    }

    // Analyze global patterns
    globalPatterns.addAll(identifyGlobalPerformancePatterns(testInsights));
    globalPatterns.addAll(identifyGlobalBehavioralPatterns(testResults));
    globalPatterns.addAll(identifyGlobalCoveragePatterns(testResults));

    // Calculate runtime health scores
    for (final RuntimeType runtime : RuntimeType.values()) {
      final RuntimeHealthScore healthScore = calculateRuntimeHealthScore(runtime, testResults);
      runtimeHealthScores.put(runtime, healthScore);
    }

    // Generate executive insights
    final List<ExecutiveInsight> executiveInsights = generateExecutiveInsights(
        globalPatterns, runtimeHealthScores, testResults.size());

    return new BatchInsightResult(
        testInsights,
        globalPatterns,
        runtimeHealthScores,
        executiveInsights,
        calculateBatchConfidenceScore(testInsights.values()),
        Instant.now()
    );
  }

  /**
   * Gets optimization recommendations based on current insights.
   *
   * @return list of optimization recommendations
   */
  public List<OptimizationRecommendation> getOptimizationRecommendations() {
    final List<OptimizationRecommendation> recommendations = new ArrayList<>();

    // Generate recommendations based on performance patterns
    for (final PerformancePattern pattern : performancePatterns.values()) {
      if (pattern.getConfidenceScore() >= PATTERN_CONFIDENCE_THRESHOLD) {
        recommendations.add(createOptimizationRecommendation(pattern));
      }
    }

    // Generate recommendations based on runtime characteristics
    for (final Map.Entry<RuntimeType, RuntimeCharacteristics> entry : runtimeCharacteristics.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final RuntimeCharacteristics characteristics = entry.getValue();

      if (characteristics.hasOptimizationOpportunities()) {
        recommendations.addAll(createRuntimeOptimizationRecommendations(runtime, characteristics));
      }
    }

    return recommendations;
  }

  /** Clears all accumulated pattern and trend data. */
  public void clearAnalysisData() {
    performancePatterns.clear();
    runtimeCharacteristics.clear();
    trendTracker.clear();
    initializeRuntimeCharacteristics();
    LOGGER.info("Cleared insight generation analysis data");
  }

  private void initializeRuntimeCharacteristics() {
    for (final RuntimeType runtime : RuntimeType.values()) {
      runtimeCharacteristics.put(runtime, new RuntimeCharacteristics(runtime));
    }
  }

  private List<PerformanceInsight> generatePerformanceOptimizationInsights(
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final BehavioralAnalysisResult behavioralResults) {

    final List<PerformanceInsight> insights = new ArrayList<>();

    // Analyze execution time patterns
    final Map<String, PerformanceAnalyzer.PerformanceMetrics> metricsByRuntime =
        performanceResults.getMetricsByRuntime();

    for (final Map.Entry<String, PerformanceAnalyzer.PerformanceMetrics> entry : metricsByRuntime.entrySet()) {
      final String runtime = entry.getKey();
      final PerformanceAnalyzer.PerformanceMetrics metrics = entry.getValue();

      // Identify optimization opportunities based on coefficient of variation
      if (metrics.getCoefficientOfVariation() > 25.0) {
        insights.add(new PerformanceInsight(
            PerformanceInsightType.HIGH_VARIABILITY,
            String.format("Runtime %s shows high performance variability (CV: %.1f%%)", runtime, metrics.getCoefficientOfVariation()),
            runtime,
            List.of(
                "Investigate performance inconsistencies",
                "Identify variable factors affecting performance",
                "Implement performance stabilization measures",
                "Add performance monitoring and alerting"
            ),
            InsightSeverity.MEDIUM,
            0.7
        ));
      }

      // Identify memory optimization opportunities
      if (metrics.getPeakMemoryUsage() > metrics.getMeanMemoryUsage() * 2) {
        insights.add(new PerformanceInsight(
            PerformanceInsightType.MEMORY_SPIKE,
            String.format("Runtime %s shows significant memory spikes (Peak: %d MB, Average: %d MB)",
                runtime, metrics.getPeakMemoryUsage() / (1024 * 1024), metrics.getMeanMemoryUsage() / (1024 * 1024)),
            runtime,
            List.of(
                "Profile memory allocation patterns",
                "Optimize memory usage and allocation",
                "Implement memory pooling if appropriate",
                "Monitor memory growth patterns"
            ),
            InsightSeverity.MEDIUM,
            0.6
        ));
      }

      // Identify outlier performance patterns
      if (metrics.getPercentile99() > metrics.getMedianExecutionTimeMs() * 3) {
        insights.add(new PerformanceInsight(
            PerformanceInsightType.PERFORMANCE_OUTLIERS,
            String.format("Runtime %s has significant performance outliers (P99: %.2fms, Median: %.2fms)",
                runtime, metrics.getPercentile99(), metrics.getMedianExecutionTimeMs()),
            runtime,
            List.of(
                "Identify and eliminate performance outliers",
                "Implement outlier detection and handling",
                "Optimize worst-case performance scenarios",
                "Add performance guardrails"
            ),
            InsightSeverity.LOW,
            0.5
        ));
      }
    }

    // Analyze cross-runtime performance patterns
    if (metricsByRuntime.size() > 1) {
      final List<String> runtimes = new ArrayList<>(metricsByRuntime.keySet());
      for (int i = 0; i < runtimes.size(); i++) {
        for (int j = i + 1; j < runtimes.size(); j++) {
          final String runtime1 = runtimes.get(i);
          final String runtime2 = runtimes.get(j);
          final PerformanceAnalyzer.PerformanceMetrics metrics1 = metricsByRuntime.get(runtime1);
          final PerformanceAnalyzer.PerformanceMetrics metrics2 = metricsByRuntime.get(runtime2);

          final double performanceDiff = Math.abs(metrics1.getMeanExecutionTimeMs() - metrics2.getMeanExecutionTimeMs())
              / Math.min(metrics1.getMeanExecutionTimeMs(), metrics2.getMeanExecutionTimeMs());

          if (performanceDiff > SIGNIFICANT_PERFORMANCE_DIFFERENCE_THRESHOLD) {
            final String fasterRuntime = metrics1.getMeanExecutionTimeMs() < metrics2.getMeanExecutionTimeMs() ?
                runtime1 : runtime2;
            final String slowerRuntime = fasterRuntime.equals(runtime1) ? runtime2 : runtime1;

            insights.add(new PerformanceInsight(
                PerformanceInsightType.RUNTIME_PERFORMANCE_GAP,
                String.format("Significant performance gap between %s and %s (%.1f%% difference)",
                    fasterRuntime, slowerRuntime, performanceDiff * 100),
                slowerRuntime,
                List.of(
                    String.format("Analyze why %s outperforms %s", fasterRuntime, slowerRuntime),
                    String.format("Optimize %s implementation", slowerRuntime),
                    "Consider architecture improvements",
                    "Validate optimization results"
                ),
                InsightSeverity.HIGH,
                0.8
            ));
          }
        }
      }
    }

    return insights;
  }

  private Map<RuntimeType, RuntimeInsight> generateRuntimeSpecificInsights(
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults) {

    final Map<RuntimeType, RuntimeInsight> insights = new HashMap<>();

    for (final RuntimeType runtime : RuntimeType.values()) {
      final List<String> observations = new ArrayList<>();
      final List<String> recommendations = new ArrayList<>();
      RuntimeInsightType insightType = RuntimeInsightType.GENERAL;
      InsightSeverity severity = InsightSeverity.LOW;

      // Analyze behavioral consistency for this runtime
      final long behavioralIssues = behavioralResults.getDiscrepancies().stream()
          .filter(discrepancy -> discrepancy.getAffectedRuntimes().contains(runtime))
          .count();

      if (behavioralIssues > 0) {
        observations.add(String.format("Runtime shows %d behavioral discrepancies", behavioralIssues));
        recommendations.add("Review and fix behavioral inconsistencies");
        insightType = RuntimeInsightType.BEHAVIORAL_ISSUES;
        severity = InsightSeverity.MEDIUM;
      }

      // Analyze performance characteristics
      final Map<String, PerformanceAnalyzer.PerformanceMetrics> metricsByRuntime =
          performanceResults.getMetricsByRuntime();
      final PerformanceAnalyzer.PerformanceMetrics metrics = metricsByRuntime.get(runtime.name());

      if (metrics != null) {
        if (metrics.getSuccessRate() < 0.95) {
          observations.add(String.format("Runtime has low success rate: %.1f%%", metrics.getSuccessRate() * 100));
          recommendations.add("Investigate and fix test failures");
          insightType = RuntimeInsightType.RELIABILITY_ISSUES;
          severity = InsightSeverity.HIGH;
        }

        if (!metrics.isStatisticallyReliable()) {
          observations.add("Performance metrics are not statistically reliable");
          recommendations.add("Increase test sample size for reliable metrics");
          severity = InsightSeverity.MEDIUM;
        }
      }

      // Analyze coverage characteristics
      final Map<RuntimeType, Set<String>> runtimeFeatureCoverage = coverageResults.getRuntimeFeatureCoverage();
      final Set<String> runtimeFeatures = runtimeFeatureCoverage.getOrDefault(runtime, Collections.emptySet());
      final int totalFeatures = coverageResults.getDetectedFeatures().size();

      if (totalFeatures > 0) {
        final double coveragePercentage = (double) runtimeFeatures.size() / totalFeatures * 100;
        if (coveragePercentage < 80.0) {
          observations.add(String.format("Runtime has low feature coverage: %.1f%%", coveragePercentage));
          recommendations.add("Implement missing WebAssembly features");
          insightType = RuntimeInsightType.FEATURE_GAPS;
          severity = InsightSeverity.HIGH;
        }
      }

      // Generate runtime-specific recommendations
      switch (runtime) {
        case JNI:
          if (metrics != null && metricsByRuntime.containsKey("PANAMA")) {
            final PerformanceAnalyzer.PerformanceMetrics panamaMetrics = metricsByRuntime.get("PANAMA");
            if (metrics.getMeanExecutionTimeMs() > panamaMetrics.getMeanExecutionTimeMs() * 1.2) {
              observations.add("JNI shows overhead compared to Panama implementation");
              recommendations.add("Optimize JNI call patterns and data marshalling");
              recommendations.add("Consider batching JNI operations");
              insightType = RuntimeInsightType.PERFORMANCE_OPTIMIZATION;
            }
          }
          break;

        case PANAMA:
          if (metrics != null && metrics.getMeanMemoryUsage() > 0) {
            observations.add("Panama Foreign Function API in use");
            recommendations.add("Leverage arena-based memory management");
            recommendations.add("Optimize native method signatures");
          }
          break;
      }

      if (!observations.isEmpty()) {
        insights.put(runtime, new RuntimeInsight(
            runtime,
            insightType,
            observations,
            recommendations,
            severity,
            calculateRuntimeConfidenceScore(runtime, observations.size())
        ));
      }
    }

    return insights;
  }

  private List<CrossCuttingInsight> generateCrossCuttingInsights(
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults) {

    final List<CrossCuttingInsight> insights = new ArrayList<>();

    // Analyze correlation between behavioral and performance issues
    if (behavioralResults.getConsistencyScore() < 0.8 && performanceResults.hasRegressions()) {
      insights.add(new CrossCuttingInsight(
          CrossCuttingInsightType.BEHAVIORAL_PERFORMANCE_CORRELATION,
          "Behavioral inconsistencies correlate with performance regressions",
          List.of(
              "Low behavioral consistency (score: " + String.format("%.2f", behavioralResults.getConsistencyScore()) + ")",
              "Performance regressions detected: " + performanceResults.getRegressionWarnings().size()
          ),
          List.of(
              "Investigate if optimization changes affected behavioral consistency",
              "Ensure behavioral tests are included in performance optimization cycles",
              "Implement integrated behavioral-performance validation"
          ),
          InsightSeverity.HIGH,
          0.8
      ));
    }

    // Analyze coverage vs. performance correlation
    if (coverageResults.getCoverageMetrics().getOverallCoveragePercentage() < 70.0 &&
        performanceResults.hasSignificantDifferences()) {
      insights.add(new CrossCuttingInsight(
          CrossCuttingInsightType.COVERAGE_PERFORMANCE_RELATIONSHIP,
          "Low test coverage may be masking performance issues",
          List.of(
              "Overall coverage: " + String.format("%.1f%%", coverageResults.getCoverageMetrics().getOverallCoveragePercentage()),
              "Significant performance differences: " + performanceResults.getSignificantDifferences().size()
          ),
          List.of(
              "Increase test coverage to validate performance across more scenarios",
              "Focus on testing performance-critical WebAssembly features",
              "Add performance tests for uncovered features"
          ),
          InsightSeverity.MEDIUM,
          0.6
      ));
    }

    // Analyze feature interaction complexity
    final FeatureInteractionAnalysis interactionAnalysis = coverageResults.getFeatureInteractionAnalysis();
    if (interactionAnalysis.getInteractionComplexity() > 5.0 &&
        !interactionAnalysis.getProblematicInteractions().isEmpty()) {
      insights.add(new CrossCuttingInsight(
          CrossCuttingInsightType.FEATURE_INTERACTION_COMPLEXITY,
          "Complex feature interactions may impact system reliability",
          List.of(
              "Interaction complexity: " + String.format("%.1f", interactionAnalysis.getInteractionComplexity()),
              "Problematic interactions: " + interactionAnalysis.getProblematicInteractions().size()
          ),
          List.of(
              "Simplify feature interaction patterns where possible",
              "Add comprehensive integration tests for complex interactions",
              "Implement feature isolation mechanisms"
          ),
          InsightSeverity.MEDIUM,
          0.7
      ));
    }

    return insights;
  }

  private List<StrategicInsight> generateStrategicInsights(
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults,
      final RecommendationResult recommendationResults) {

    final List<StrategicInsight> insights = new ArrayList<>();

    // Analyze development focus areas
    final long highPriorityRecommendations = recommendationResults.getHighPriorityRecommendations().size();
    if (highPriorityRecommendations > 3) {
      insights.add(new StrategicInsight(
          StrategicInsightType.DEVELOPMENT_FOCUS,
          "Multiple high-priority issues require focused development effort",
          List.of(
              "High-priority recommendations: " + highPriorityRecommendations,
              "Categories affected: " + recommendationResults.getSummary().getCategoryBreakdown().size()
          ),
          List.of(
              "Prioritize high-impact issues for immediate resolution",
              "Allocate dedicated resources to critical compatibility issues",
              "Implement systematic approach to issue resolution"
          ),
          StrategicImportance.HIGH,
          0.9
      ));
    }

    // Analyze runtime maturity
    final String fastestRuntime = performanceResults.getFastestRuntime();
    final String mostMemoryEfficient = performanceResults.getMostMemoryEfficientRuntime();

    if (!fastestRuntime.equals(mostMemoryEfficient)) {
      insights.add(new StrategicInsight(
          StrategicInsightType.RUNTIME_OPTIMIZATION,
          "Different runtimes excel in different metrics, suggesting optimization opportunities",
          List.of(
              "Fastest runtime: " + fastestRuntime,
              "Most memory efficient: " + mostMemoryEfficient
          ),
          List.of(
              "Learn from best-performing runtime characteristics",
              "Consider hybrid approaches for different use cases",
              "Optimize runtimes based on primary use case requirements"
          ),
          StrategicImportance.MEDIUM,
          0.7
      ));
    }

    // Analyze coverage strategy
    final double overallCoverage = coverageResults.getCoverageMetrics().getOverallCoveragePercentage();
    if (overallCoverage < 80.0) {
      insights.add(new StrategicInsight(
          StrategicInsightType.TESTING_STRATEGY,
          "Test coverage strategy needs enhancement for comprehensive validation",
          List.of(
              "Overall coverage: " + String.format("%.1f%%", overallCoverage),
              "High-severity gaps: " + coverageResults.getHighSeverityGapCount()
          ),
          List.of(
              "Develop comprehensive WebAssembly feature test suite",
              "Implement automated coverage tracking and reporting",
              "Prioritize testing of critical WebAssembly features"
          ),
          StrategicImportance.HIGH,
          0.8
      ));
    }

    return insights;
  }

  private List<PatternInsight> generatePatternInsights(
      final String testName,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults) {

    final List<PatternInsight> insights = new ArrayList<>();

    // Update performance patterns
    updatePerformancePatterns(testName, performanceResults);

    // Generate insights based on patterns
    for (final PerformancePattern pattern : performancePatterns.values()) {
      if (pattern.getOccurrenceCount() >= MIN_SAMPLE_SIZE_FOR_INSIGHTS &&
          pattern.getConfidenceScore() >= PATTERN_CONFIDENCE_THRESHOLD) {

        insights.add(new PatternInsight(
            pattern.getPatternType(),
            pattern.getDescription(),
            pattern.getOccurrenceCount(),
            pattern.getConfidenceScore(),
            pattern.getRecommendations()
        ));
      }
    }

    return insights;
  }

  private List<TrendInsight> generateTrendInsights(final String testName) {
    final List<TrendInsight> insights = new ArrayList<>();

    // Generate trend insights based on historical data
    final Map<String, TrendData> trends = trendTracker.getTrendsForTest(testName);

    for (final Map.Entry<String, TrendData> entry : trends.entrySet()) {
      final String metric = entry.getKey();
      final TrendData trendData = entry.getValue();

      if (trendData.hasSignificantTrend()) {
        insights.add(new TrendInsight(
            metric,
            trendData.getTrendDirection(),
            trendData.getTrendStrength(),
            trendData.getDescription(),
            trendData.getRecommendations()
        ));
      }
    }

    return insights;
  }

  private void updateTrendTracking(
      final String testName,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final BehavioralAnalysisResult behavioralResults) {

    // Track performance trends
    for (final Map.Entry<String, PerformanceAnalyzer.PerformanceMetrics> entry :
         performanceResults.getMetricsByRuntime().entrySet()) {
      final String runtime = entry.getKey();
      final PerformanceAnalyzer.PerformanceMetrics metrics = entry.getValue();

      trendTracker.recordDataPoint(testName, runtime + "_execution_time", metrics.getMeanExecutionTimeMs());
      trendTracker.recordDataPoint(testName, runtime + "_memory_usage", metrics.getMeanMemoryUsage());
      trendTracker.recordDataPoint(testName, runtime + "_success_rate", metrics.getSuccessRate());
    }

    // Track behavioral trends
    trendTracker.recordDataPoint(testName, "consistency_score", behavioralResults.getConsistencyScore());
    trendTracker.recordDataPoint(testName, "discrepancy_count", behavioralResults.getDiscrepancies().size());
  }

  private void updatePerformancePatterns(
      final String testName,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults) {

    // Identify patterns in performance results
    final Map<String, PerformanceAnalyzer.PerformanceMetrics> metricsByRuntime =
        performanceResults.getMetricsByRuntime();

    // Pattern: Consistent high variability
    for (final Map.Entry<String, PerformanceAnalyzer.PerformanceMetrics> entry : metricsByRuntime.entrySet()) {
      final String runtime = entry.getKey();
      final PerformanceAnalyzer.PerformanceMetrics metrics = entry.getValue();

      if (metrics.getCoefficientOfVariation() > 20.0) {
        final String patternKey = runtime + "_high_variability";
        performancePatterns.computeIfAbsent(patternKey, k -> new PerformancePattern(
            k,
            PatternType.HIGH_VARIABILITY,
            String.format("Runtime %s consistently shows high performance variability", runtime)
        )).addOccurrence(testName, metrics.getCoefficientOfVariation());
      }
    }

    // Pattern: Memory usage spikes
    for (final Map.Entry<String, PerformanceAnalyzer.PerformanceMetrics> entry : metricsByRuntime.entrySet()) {
      final String runtime = entry.getKey();
      final PerformanceAnalyzer.PerformanceMetrics metrics = entry.getValue();

      final double memorySpike = metrics.getPeakMemoryUsage() > 0 ?
          (double) metrics.getPeakMemoryUsage() / metrics.getMeanMemoryUsage() : 1.0;

      if (memorySpike > 2.0) {
        final String patternKey = runtime + "_memory_spikes";
        performancePatterns.computeIfAbsent(patternKey, k -> new PerformancePattern(
            k,
            PatternType.MEMORY_SPIKES,
            String.format("Runtime %s consistently shows memory usage spikes", runtime)
        )).addOccurrence(testName, memorySpike);
      }
    }
  }

  private InsightConfidenceMetrics calculateConfidenceMetrics(
      final List<PerformanceInsight> performanceInsights,
      final Map<RuntimeType, RuntimeInsight> runtimeInsights,
      final List<CrossCuttingInsight> crossCuttingInsights,
      final List<StrategicInsight> strategicInsights) {

    final double averagePerformanceConfidence = performanceInsights.stream()
        .mapToDouble(PerformanceInsight::getConfidenceScore)
        .average()
        .orElse(0.0);

    final double averageRuntimeConfidence = runtimeInsights.values().stream()
        .mapToDouble(RuntimeInsight::getConfidenceScore)
        .average()
        .orElse(0.0);

    final double averageCrossCuttingConfidence = crossCuttingInsights.stream()
        .mapToDouble(CrossCuttingInsight::getConfidenceScore)
        .average()
        .orElse(0.0);

    final double averageStrategicConfidence = strategicInsights.stream()
        .mapToDouble(StrategicInsight::getConfidenceScore)
        .average()
        .orElse(0.0);

    final double overallConfidence = (averagePerformanceConfidence + averageRuntimeConfidence +
        averageCrossCuttingConfidence + averageStrategicConfidence) / 4.0;

    return new InsightConfidenceMetrics(
        overallConfidence,
        averagePerformanceConfidence,
        averageRuntimeConfidence,
        averageCrossCuttingConfidence,
        averageStrategicConfidence
    );
  }

  private List<GlobalPattern> identifyGlobalPerformancePatterns(
      final Map<String, InsightAnalysisResult> testInsights) {

    final List<GlobalPattern> patterns = new ArrayList<>();
    final Map<String, Integer> performancePatternCounts = new HashMap<>();

    // Count performance patterns across tests
    for (final InsightAnalysisResult result : testInsights.values()) {
      for (final PerformanceInsight insight : result.getPerformanceInsights()) {
        final String patternKey = insight.getType().name() + "_" + insight.getRuntime();
        performancePatternCounts.merge(patternKey, 1, Integer::sum);
      }
    }

    // Identify significant global patterns
    final int totalTests = testInsights.size();
    for (final Map.Entry<String, Integer> entry : performancePatternCounts.entrySet()) {
      if (entry.getValue() >= Math.max(2, totalTests / 3)) { // Appears in at least 1/3 of tests
        patterns.add(new GlobalPattern(
            GlobalPatternType.PERFORMANCE,
            entry.getKey(),
            String.format("Performance pattern '%s' appears in %d of %d tests",
                entry.getKey(), entry.getValue(), totalTests),
            entry.getValue(),
            (double) entry.getValue() / totalTests
        ));
      }
    }

    return patterns;
  }

  private List<GlobalPattern> identifyGlobalBehavioralPatterns(
      final Map<String, CompleteTestAnalysis> testResults) {

    final List<GlobalPattern> patterns = new ArrayList<>();
    final Map<DiscrepancySeverity, Integer> severityCounts = new HashMap<>();

    // Analyze behavioral patterns
    for (final CompleteTestAnalysis analysis : testResults.values()) {
      for (final BehavioralDiscrepancy discrepancy : analysis.getBehavioralResults().getDiscrepancies()) {
        severityCounts.merge(discrepancy.getSeverity(), 1, Integer::sum);
      }
    }

    // Identify concerning trends
    final int totalTests = testResults.size();
    for (final Map.Entry<DiscrepancySeverity, Integer> entry : severityCounts.entrySet()) {
      if (entry.getKey() == DiscrepancySeverity.CRITICAL && entry.getValue() > 0) {
        patterns.add(new GlobalPattern(
            GlobalPatternType.BEHAVIORAL,
            "critical_discrepancies",
            String.format("Critical behavioral discrepancies found in %d tests", entry.getValue()),
            entry.getValue(),
            (double) entry.getValue() / totalTests
        ));
      }
    }

    return patterns;
  }

  private List<GlobalPattern> identifyGlobalCoveragePatterns(
      final Map<String, CompleteTestAnalysis> testResults) {

    final List<GlobalPattern> patterns = new ArrayList<>();
    final Map<String, Integer> gapCounts = new HashMap<>();

    // Analyze coverage patterns
    for (final CompleteTestAnalysis analysis : testResults.values()) {
      for (final CoverageGap gap : analysis.getCoverageResults().getCoverageGaps()) {
        if (gap.getSeverity() == GapSeverity.HIGH) {
          gapCounts.merge(gap.getType().name(), 1, Integer::sum);
        }
      }
    }

    // Identify systematic coverage issues
    final int totalTests = testResults.size();
    for (final Map.Entry<String, Integer> entry : gapCounts.entrySet()) {
      if (entry.getValue() >= Math.max(2, totalTests / 4)) { // Appears in at least 1/4 of tests
        patterns.add(new GlobalPattern(
            GlobalPatternType.COVERAGE,
            entry.getKey(),
            String.format("High-severity coverage gaps of type '%s' found in %d tests",
                entry.getKey(), entry.getValue()),
            entry.getValue(),
            (double) entry.getValue() / totalTests
        ));
      }
    }

    return patterns;
  }

  private RuntimeHealthScore calculateRuntimeHealthScore(
      final RuntimeType runtime,
      final Map<String, CompleteTestAnalysis> testResults) {

    double totalPerformanceScore = 0.0;
    double totalBehavioralScore = 0.0;
    double totalCoverageScore = 0.0;
    int validTests = 0;

    for (final CompleteTestAnalysis analysis : testResults.values()) {
      // Performance score
      final Map<String, PerformanceAnalyzer.PerformanceMetrics> metricsByRuntime =
          analysis.getPerformanceResults().getMetricsByRuntime();
      final PerformanceAnalyzer.PerformanceMetrics metrics = metricsByRuntime.get(runtime.name());

      if (metrics != null) {
        totalPerformanceScore += metrics.getSuccessRate();
        validTests++;
      }

      // Behavioral score
      final long behavioralIssues = analysis.getBehavioralResults().getDiscrepancies().stream()
          .filter(d -> d.getAffectedRuntimes().contains(runtime))
          .count();
      totalBehavioralScore += Math.max(0.0, 1.0 - (behavioralIssues * 0.1));

      // Coverage score
      final Map<RuntimeType, Set<String>> runtimeFeatureCoverage =
          analysis.getCoverageResults().getRuntimeFeatureCoverage();
      final Set<String> runtimeFeatures = runtimeFeatureCoverage.getOrDefault(runtime, Collections.emptySet());
      final int totalFeatures = analysis.getCoverageResults().getDetectedFeatures().size();

      if (totalFeatures > 0) {
        totalCoverageScore += (double) runtimeFeatures.size() / totalFeatures;
      }
    }

    final int testCount = testResults.size();
    final double performanceScore = validTests > 0 ? totalPerformanceScore / validTests : 0.0;
    final double behavioralScore = testCount > 0 ? totalBehavioralScore / testCount : 0.0;
    final double coverageScore = testCount > 0 ? totalCoverageScore / testCount : 0.0;

    final double overallScore = (performanceScore + behavioralScore + coverageScore) / 3.0;

    return new RuntimeHealthScore(
        runtime,
        overallScore,
        performanceScore,
        behavioralScore,
        coverageScore,
        determineHealthStatus(overallScore)
    );
  }

  private List<ExecutiveInsight> generateExecutiveInsights(
      final List<GlobalPattern> globalPatterns,
      final Map<RuntimeType, RuntimeHealthScore> runtimeHealthScores,
      final int totalTests) {

    final List<ExecutiveInsight> insights = new ArrayList<>();

    // Overall system health insight
    final double averageHealthScore = runtimeHealthScores.values().stream()
        .mapToDouble(RuntimeHealthScore::getOverallScore)
        .average()
        .orElse(0.0);

    insights.add(new ExecutiveInsight(
        ExecutiveInsightType.SYSTEM_HEALTH,
        String.format("Overall system health score: %.1f%% across %d runtime(s)", averageHealthScore * 100, runtimeHealthScores.size()),
        determineHealthStatus(averageHealthScore),
        List.of(
            String.format("Analyzed %d tests across %d runtime implementations", totalTests, runtimeHealthScores.size()),
            String.format("Identified %d global patterns requiring attention", globalPatterns.size())
        )
    ));

    // Critical pattern insight
    final long criticalPatterns = globalPatterns.stream()
        .filter(pattern -> pattern.getFrequency() >= 0.5) // Appears in 50%+ of tests
        .count();

    if (criticalPatterns > 0) {
      insights.add(new ExecutiveInsight(
          ExecutiveInsightType.CRITICAL_PATTERNS,
          String.format("%d critical patterns identified affecting majority of tests", criticalPatterns),
          HealthStatus.POOR,
          List.of(
              "These patterns indicate systematic issues requiring immediate attention",
              "Recommend prioritizing fixes for these widespread issues"
          )
      ));
    }

    // Runtime comparison insight
    final RuntimeType bestRuntime = runtimeHealthScores.entrySet().stream()
        .max(Map.Entry.comparingByValue((score1, score2) ->
            Double.compare(score1.getOverallScore(), score2.getOverallScore())))
        .map(Map.Entry::getKey)
        .orElse(null);

    if (bestRuntime != null && runtimeHealthScores.size() > 1) {
      insights.add(new ExecutiveInsight(
          ExecutiveInsightType.RUNTIME_COMPARISON,
          String.format("Runtime %s shows best overall performance and compatibility", bestRuntime),
          HealthStatus.GOOD,
          List.of(
              String.format("%s can serve as reference for optimizing other runtimes", bestRuntime),
              "Consider analyzing best practices from top-performing runtime"
          )
      ));
    }

    return insights;
  }

  private OptimizationRecommendation createOptimizationRecommendation(final PerformancePattern pattern) {
    return new OptimizationRecommendation(
        pattern.getPatternType(),
        pattern.getDescription(),
        pattern.getRecommendations(),
        OptimizationPriority.HIGH,
        pattern.getConfidenceScore()
    );
  }

  private List<OptimizationRecommendation> createRuntimeOptimizationRecommendations(
      final RuntimeType runtime,
      final RuntimeCharacteristics characteristics) {

    final List<OptimizationRecommendation> recommendations = new ArrayList<>();

    for (final String opportunity : characteristics.getOptimizationOpportunities()) {
      recommendations.add(new OptimizationRecommendation(
          PatternType.RUNTIME_SPECIFIC,
          String.format("Optimization opportunity for %s: %s", runtime, opportunity),
          List.of(
              "Analyze runtime-specific performance characteristics",
              "Implement targeted optimizations",
              "Validate optimization effectiveness"
          ),
          OptimizationPriority.MEDIUM,
          0.7
      ));
    }

    return recommendations;
  }

  private double calculateRuntimeConfidenceScore(final RuntimeType runtime, final int observationCount) {
    // Base confidence on number of observations and runtime maturity
    double baseScore = Math.min(1.0, observationCount * 0.2);

    // Adjust based on runtime characteristics
    final RuntimeCharacteristics characteristics = runtimeCharacteristics.get(runtime);
    if (characteristics != null && characteristics.isWellTested()) {
      baseScore += 0.1;
    }

    return Math.min(1.0, baseScore);
  }

  private double calculateBatchConfidenceScore(final Iterable<InsightAnalysisResult> results) {
    double totalConfidence = 0.0;
    int count = 0;

    for (final InsightAnalysisResult result : results) {
      totalConfidence += result.getConfidenceMetrics().getOverallConfidence();
      count++;
    }

    return count > 0 ? totalConfidence / count : 0.0;
  }

  private HealthStatus determineHealthStatus(final double score) {
    if (score >= 0.8) {
      return HealthStatus.EXCELLENT;
    } else if (score >= 0.6) {
      return HealthStatus.GOOD;
    } else if (score >= 0.4) {
      return HealthStatus.FAIR;
    } else {
      return HealthStatus.POOR;
    }
  }

  /** Tracks trends over time for various metrics. */
  private static final class TrendTracker {
    private final Map<String, Map<String, List<Double>>> trends = new ConcurrentHashMap<>();

    public void recordDataPoint(final String testName, final String metric, final double value) {
      trends.computeIfAbsent(testName, k -> new ConcurrentHashMap<>())
          .computeIfAbsent(metric, k -> new ArrayList<>())
          .add(value);
    }

    public Map<String, TrendData> getTrendsForTest(final String testName) {
      final Map<String, List<Double>> testTrends = trends.getOrDefault(testName, Collections.emptyMap());
      final Map<String, TrendData> trendData = new HashMap<>();

      for (final Map.Entry<String, List<Double>> entry : testTrends.entrySet()) {
        final String metric = entry.getKey();
        final List<Double> values = entry.getValue();

        if (values.size() >= 3) { // Need at least 3 points for trend analysis
          trendData.put(metric, analyzeTrend(metric, values));
        }
      }

      return trendData;
    }

    public void clear() {
      trends.clear();
    }

    private TrendData analyzeTrend(final String metric, final List<Double> values) {
      // Simple linear trend analysis
      final int n = values.size();
      double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

      for (int i = 0; i < n; i++) {
        final double x = i;
        final double y = values.get(i);
        sumX += x;
        sumY += y;
        sumXY += x * y;
        sumX2 += x * x;
      }

      final double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
      final TrendDirection direction = slope > 0.05 ? TrendDirection.IMPROVING :
          slope < -0.05 ? TrendDirection.DECLINING : TrendDirection.STABLE;

      final double strength = Math.abs(slope) * 100; // Convert to percentage
      final String description = String.format("Trend for %s: %s (strength: %.2f%%)",
          metric, direction, strength);

      final List<String> recommendations = new ArrayList<>();
      if (direction == TrendDirection.DECLINING && strength > 10) {
        recommendations.add("Investigate cause of declining trend");
        recommendations.add("Implement corrective measures");
      } else if (direction == TrendDirection.IMPROVING) {
        recommendations.add("Continue current approach");
        recommendations.add("Document successful practices");
      }

      return new TrendData(direction, strength, description, recommendations);
    }
  }

  /** Represents runtime-specific characteristics and optimization opportunities. */
  private static final class RuntimeCharacteristics {
    private final RuntimeType runtime;
    private final Set<String> optimizationOpportunities = new HashSet<>();
    private boolean wellTested = false;

    public RuntimeCharacteristics(final RuntimeType runtime) {
      this.runtime = runtime;
      initializeCharacteristics();
    }

    public boolean hasOptimizationOpportunities() {
      return !optimizationOpportunities.isEmpty();
    }

    public Set<String> getOptimizationOpportunities() {
      return new HashSet<>(optimizationOpportunities);
    }

    public boolean isWellTested() {
      return wellTested;
    }

    private void initializeCharacteristics() {
      switch (runtime) {
        case JNI:
          optimizationOpportunities.add("JNI call overhead reduction");
          optimizationOpportunities.add("Data marshalling optimization");
          wellTested = true;
          break;
        case PANAMA:
          optimizationOpportunities.add("Arena-based memory management");
          optimizationOpportunities.add("Native method signature optimization");
          wellTested = false; // Panama is newer
          break;
      }
    }
  }

  /** Represents a performance pattern observed across multiple tests. */
  private static final class PerformancePattern {
    private final String patternId;
    private final PatternType patternType;
    private final String description;
    private final List<String> occurrences = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();

    public PerformancePattern(final String patternId, final PatternType patternType, final String description) {
      this.patternId = patternId;
      this.patternType = patternType;
      this.description = description;
    }

    public void addOccurrence(final String testName, final double value) {
      occurrences.add(testName);
      values.add(value);
    }

    public String getPatternId() {
      return patternId;
    }

    public PatternType getPatternType() {
      return patternType;
    }

    public String getDescription() {
      return description;
    }

    public int getOccurrenceCount() {
      return occurrences.size();
    }

    public double getConfidenceScore() {
      if (values.isEmpty()) {
        return 0.0;
      }

      // Calculate confidence based on consistency of values
      final double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
      final double variance = values.stream()
          .mapToDouble(v -> Math.pow(v - mean, 2))
          .average()
          .orElse(0.0);

      final double cv = mean > 0 ? Math.sqrt(variance) / mean : 1.0;
      return Math.max(0.0, 1.0 - cv); // Higher confidence for lower variation
    }

    public List<String> getRecommendations() {
      final List<String> recommendations = new ArrayList<>();

      switch (patternType) {
        case HIGH_VARIABILITY:
          recommendations.add("Investigate sources of performance variability");
          recommendations.add("Implement performance stabilization measures");
          break;
        case MEMORY_SPIKES:
          recommendations.add("Profile memory allocation patterns");
          recommendations.add("Optimize memory usage and implement pooling");
          break;
        case RUNTIME_SPECIFIC:
          recommendations.add("Analyze runtime-specific optimization opportunities");
          recommendations.add("Implement targeted runtime optimizations");
          break;
      }

      return recommendations;
    }
  }
}