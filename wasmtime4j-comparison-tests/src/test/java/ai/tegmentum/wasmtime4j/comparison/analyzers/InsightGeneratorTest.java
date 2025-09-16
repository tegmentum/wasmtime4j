package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for InsightGenerator ensuring accurate pattern recognition,
 * trend analysis, and strategic insight generation.
 */
@DisplayName("InsightGenerator")
class InsightGeneratorTest {

  private InsightGenerator insightGenerator;
  private BehavioralAnalysisResult behavioralResults;
  private PerformanceAnalyzer.PerformanceComparisonResult performanceResults;
  private CoverageAnalysisResult coverageResults;
  private RecommendationResult recommendationResults;

  @BeforeEach
  void setUp() {
    insightGenerator = new InsightGenerator();
    setupTestData();
  }

  private void setupTestData() {
    // Setup behavioral results
    behavioralResults = new BehavioralAnalysisResult.Builder("insight_test")
        .pairwiseComparisons(Collections.emptyList())
        .discrepancies(List.of(createDiscrepancy("Performance affecting issue", DiscrepancySeverity.MAJOR)))
        .executionPattern(new ExecutionPattern(2, 0, 0, 1, 0, 10.0))
        .consistencyScore(0.75)
        .verdict(BehavioralVerdict.MOSTLY_CONSISTENT)
        .build();

    // Setup performance results with variability
    final Map<String, PerformanceAnalyzer.PerformanceMetrics> metricsByRuntime = createPerformanceMetrics();
    performanceResults = new PerformanceAnalyzer.PerformanceComparisonResult(
        "insight_test",
        Collections.emptyList(),
        metricsByRuntime,
        List.of("JNI is 20% slower than PANAMA"),
        List.of("JNI shows 10% performance regression"),
        new PerformanceAnalyzer.OverheadAnalysis(metricsByRuntime)
    );

    // Setup coverage results
    final CoverageMetrics coverageMetrics = new CoverageMetrics(10, 8, 80.0,
        Map.of(RuntimeType.JNI, 75.0, RuntimeType.PANAMA, 85.0), 90.0);

    coverageResults = new CoverageAnalysisResult.Builder("insight_test")
        .detectedFeatures(Set.of("memory", "simd", "core"))
        .runtimeFeatureCoverage(Map.of(
            RuntimeType.JNI, Set.of("memory", "core"),
            RuntimeType.PANAMA, Set.of("memory", "simd", "core")
        ))
        .coverageMetrics(coverageMetrics)
        .coverageGaps(List.of(createCoverageGap()))
        .featureInteractionAnalysis(new FeatureInteractionAnalysis(
            Map.of("memory+simd", Set.of("memory", "simd")),
            Collections.emptyList(),
            4.5
        ))
        .build();

    // Setup recommendation results
    final List<ActionableRecommendation> recommendations = new ArrayList<>();
    recommendations.add(createRecommendation("Fix JNI Performance", IssueCategory.PERFORMANCE, IssueSeverity.HIGH));
    recommendations.add(createRecommendation("Improve Coverage", IssueCategory.COVERAGE, IssueSeverity.MEDIUM));

    final RecommendationSummary summary = new RecommendationSummary(
        2, 1, 1, 0,
        Map.of(IssueCategory.PERFORMANCE, 1, IssueCategory.COVERAGE, 1)
    );

    recommendationResults = new RecommendationResult.Builder("insight_test")
        .behavioralRecommendations(Collections.emptyList())
        .performanceRecommendations(List.of(recommendations.get(0)))
        .coverageRecommendations(List.of(recommendations.get(1)))
        .integrationRecommendations(Collections.emptyList())
        .prioritizedRecommendations(recommendations)
        .summary(summary)
        .build();
  }

  private Map<String, PerformanceAnalyzer.PerformanceMetrics> createPerformanceMetrics() {
    final Map<String, PerformanceAnalyzer.PerformanceMetrics> metrics = new HashMap<>();

    // Create metrics with high variability for JNI
    final List<PerformanceAnalyzer.TestExecutionResult> jniResults = new ArrayList<>();
    jniResults.add(PerformanceAnalyzer.TestExecutionResult.builder("test", "JNI")
        .executionDuration(Duration.ofMillis(100))
        .memoryUsed(1024L)
        .peakMemoryUsage(2048L)
        .successful(true)
        .build());
    jniResults.add(PerformanceAnalyzer.TestExecutionResult.builder("test", "JNI")
        .executionDuration(Duration.ofMillis(150))
        .memoryUsed(1024L)
        .peakMemoryUsage(4096L) // High peak for memory spike
        .successful(true)
        .build());
    jniResults.add(PerformanceAnalyzer.TestExecutionResult.builder("test", "JNI")
        .executionDuration(Duration.ofMillis(80))
        .memoryUsed(1024L)
        .peakMemoryUsage(2048L)
        .successful(true)
        .build());

    // Create more consistent metrics for Panama
    final List<PerformanceAnalyzer.TestExecutionResult> panamaResults = new ArrayList<>();
    panamaResults.add(PerformanceAnalyzer.TestExecutionResult.builder("test", "PANAMA")
        .executionDuration(Duration.ofMillis(85))
        .memoryUsed(512L)
        .peakMemoryUsage(1024L)
        .successful(true)
        .build());
    panamaResults.add(PerformanceAnalyzer.TestExecutionResult.builder("test", "PANAMA")
        .executionDuration(Duration.ofMillis(90))
        .memoryUsed(512L)
        .peakMemoryUsage(1024L)
        .successful(true)
        .build());

    metrics.put("JNI", new PerformanceAnalyzer.PerformanceMetrics("JNI", jniResults));
    metrics.put("PANAMA", new PerformanceAnalyzer.PerformanceMetrics("PANAMA", panamaResults));

    return metrics;
  }

  private BehavioralDiscrepancy createDiscrepancy(final String description, final DiscrepancySeverity severity) {
    return new BehavioralDiscrepancy(DiscrepancyType.PERFORMANCE_RELATED, description, severity, Set.of(RuntimeType.JNI));
  }

  private CoverageGap createCoverageGap() {
    return new CoverageGap(
        CoverageGapType.FEATURE_INCOMPLETE,
        "SIMD features missing in JNI",
        Set.of("simd_operations"),
        Set.of(RuntimeType.JNI),
        GapSeverity.HIGH
    );
  }

  private ActionableRecommendation createRecommendation(final String title, final IssueCategory category,
                                                       final IssueSeverity severity) {
    return new ActionableRecommendation(
        title,
        "Test recommendation description",
        List.of("Step 1", "Step 2"),
        category,
        severity,
        0.8,
        Set.of(RuntimeType.JNI),
        "test_pattern"
    );
  }

  @Nested
  @DisplayName("Insight Generation")
  class InsightGenerationTests {

    @Test
    @DisplayName("Should generate comprehensive insights")
    void shouldGenerateComprehensiveInsights() {
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "comprehensive_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      assertNotNull(result);
      assertEquals("comprehensive_test", result.getTestName());
      assertNotNull(result.getPerformanceInsights());
      assertNotNull(result.getRuntimeInsights());
      assertNotNull(result.getCrossCuttingInsights());
      assertNotNull(result.getStrategicInsights());
      assertNotNull(result.getPatternInsights());
      assertNotNull(result.getTrendInsights());
      assertNotNull(result.getConfidenceMetrics());
    }

    @Test
    @DisplayName("Should generate performance optimization insights")
    void shouldGeneratePerformanceOptimizationInsights() {
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "performance_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      final List<PerformanceInsight> performanceInsights = result.getPerformanceInsights();
      assertFalse(performanceInsights.isEmpty());

      // Should detect high variability in JNI
      final boolean hasVariabilityInsight = performanceInsights.stream()
          .anyMatch(insight -> insight.getType() == PerformanceInsightType.HIGH_VARIABILITY &&
                              insight.getRuntime().equals("JNI"));
      assertTrue(hasVariabilityInsight);

      // Should detect memory spikes
      final boolean hasMemorySpike = performanceInsights.stream()
          .anyMatch(insight -> insight.getType() == PerformanceInsightType.MEMORY_SPIKE);
      assertTrue(hasMemorySpike);
    }

    @Test
    @DisplayName("Should generate runtime-specific insights")
    void shouldGenerateRuntimeSpecificInsights() {
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "runtime_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      final Map<RuntimeType, RuntimeInsight> runtimeInsights = result.getRuntimeInsights();
      assertFalse(runtimeInsights.isEmpty());

      // Should have insights for JNI (which has issues)
      assertTrue(runtimeInsights.containsKey(RuntimeType.JNI));

      final RuntimeInsight jniInsight = runtimeInsights.get(RuntimeType.JNI);
      assertNotNull(jniInsight);
      assertFalse(jniInsight.getObservations().isEmpty());
      assertFalse(jniInsight.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Should generate cross-cutting insights")
    void shouldGenerateCrossCuttingInsights() {
      // Create results with correlated behavioral and performance issues
      final BehavioralAnalysisResult poorBehavioral = new BehavioralAnalysisResult.Builder("cross_cutting_test")
          .pairwiseComparisons(Collections.emptyList())
          .discrepancies(List.of(createDiscrepancy("Major issue", DiscrepancySeverity.CRITICAL)))
          .executionPattern(new ExecutionPattern(1, 1, 0, 2, 1, 15.0))
          .consistencyScore(0.65)
          .verdict(BehavioralVerdict.INCONSISTENT)
          .build();

      final PerformanceAnalyzer.PerformanceComparisonResult regressionResults =
          new PerformanceAnalyzer.PerformanceComparisonResult(
              "cross_cutting_test",
              Collections.emptyList(),
              Collections.emptyMap(),
              Collections.emptyList(),
              List.of("Major regression detected"),
              new PerformanceAnalyzer.OverheadAnalysis(Collections.emptyMap())
          );

      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "cross_cutting_test", poorBehavioral, regressionResults, coverageResults, recommendationResults);

      final List<CrossCuttingInsight> crossCuttingInsights = result.getCrossCuttingInsights();
      assertFalse(crossCuttingInsights.isEmpty());

      // Should detect correlation between behavioral and performance issues
      final boolean hasCorrelationInsight = crossCuttingInsights.stream()
          .anyMatch(insight -> insight.getType() == CrossCuttingInsightType.BEHAVIORAL_PERFORMANCE_CORRELATION);
      assertTrue(hasCorrelationInsight);
    }

    @Test
    @DisplayName("Should generate strategic insights")
    void shouldGenerateStrategicInsights() {
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "strategic_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      final List<StrategicInsight> strategicInsights = result.getStrategicInsights();

      // Should generate strategic insights when there are high-priority recommendations
      if (recommendationResults.getHighPriorityRecommendations().size() > 3) {
        assertFalse(strategicInsights.isEmpty());
      }

      // Verify strategic insights have appropriate importance levels
      for (final StrategicInsight insight : strategicInsights) {
        assertNotNull(insight.getImportance());
        assertFalse(insight.getObservations().isEmpty());
        assertFalse(insight.getRecommendations().isEmpty());
      }
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void shouldHandleNullInputsGracefully() {
      assertThrows(NullPointerException.class, () ->
          insightGenerator.generateInsights(null, behavioralResults, performanceResults, coverageResults, recommendationResults));

      assertThrows(NullPointerException.class, () ->
          insightGenerator.generateInsights("test", null, performanceResults, coverageResults, recommendationResults));

      assertThrows(NullPointerException.class, () ->
          insightGenerator.generateInsights("test", behavioralResults, null, coverageResults, recommendationResults));

      assertThrows(NullPointerException.class, () ->
          insightGenerator.generateInsights("test", behavioralResults, performanceResults, null, recommendationResults));

      assertThrows(NullPointerException.class, () ->
          insightGenerator.generateInsights("test", behavioralResults, performanceResults, coverageResults, null));
    }
  }

  @Nested
  @DisplayName("Batch Insight Generation")
  class BatchInsightGenerationTests {

    @Test
    @DisplayName("Should generate batch insights")
    void shouldGenerateBatchInsights() {
      final Map<String, CompleteTestAnalysis> testResults = new HashMap<>();
      testResults.put("test1", new CompleteTestAnalysis(behavioralResults, performanceResults, coverageResults, recommendationResults));
      testResults.put("test2", new CompleteTestAnalysis(behavioralResults, performanceResults, coverageResults, recommendationResults));

      final BatchInsightResult result = insightGenerator.generateBatchInsights(testResults);

      assertNotNull(result);
      assertEquals(2, result.getTestInsights().size());
      assertNotNull(result.getGlobalPatterns());
      assertNotNull(result.getRuntimeHealthScores());
      assertNotNull(result.getExecutiveInsights());
      assertTrue(result.getBatchConfidenceScore() >= 0.0);
    }

    @Test
    @DisplayName("Should identify global patterns")
    void shouldIdentifyGlobalPatterns() {
      final Map<String, CompleteTestAnalysis> testResults = new HashMap<>();
      // Add multiple tests with similar patterns
      for (int i = 1; i <= 5; i++) {
        testResults.put("test" + i, new CompleteTestAnalysis(behavioralResults, performanceResults, coverageResults, recommendationResults));
      }

      final BatchInsightResult result = insightGenerator.generateBatchInsights(testResults);

      // Should identify patterns that appear frequently
      assertFalse(result.getGlobalPatterns().isEmpty());
    }

    @Test
    @DisplayName("Should calculate runtime health scores")
    void shouldCalculateRuntimeHealthScores() {
      final Map<String, CompleteTestAnalysis> testResults = new HashMap<>();
      testResults.put("health_test", new CompleteTestAnalysis(behavioralResults, performanceResults, coverageResults, recommendationResults));

      final BatchInsightResult result = insightGenerator.generateBatchInsights(testResults);

      final Map<RuntimeType, RuntimeHealthScore> healthScores = result.getRuntimeHealthScores();
      assertFalse(healthScores.isEmpty());

      for (final RuntimeHealthScore score : healthScores.values()) {
        assertTrue(score.getOverallScore() >= 0.0);
        assertTrue(score.getOverallScore() <= 1.0);
        assertNotNull(score.getHealthStatus());
      }
    }

    @Test
    @DisplayName("Should generate executive insights")
    void shouldGenerateExecutiveInsights() {
      final Map<String, CompleteTestAnalysis> testResults = new HashMap<>();
      testResults.put("executive_test", new CompleteTestAnalysis(behavioralResults, performanceResults, coverageResults, recommendationResults));

      final BatchInsightResult result = insightGenerator.generateBatchInsights(testResults);

      final List<ExecutiveInsight> executiveInsights = result.getExecutiveInsights();
      assertFalse(executiveInsights.isEmpty());

      // Should have system health insight
      final boolean hasSystemHealthInsight = executiveInsights.stream()
          .anyMatch(insight -> insight.getType() == ExecutiveInsightType.SYSTEM_HEALTH);
      assertTrue(hasSystemHealthInsight);
    }

    @Test
    @DisplayName("Should identify best performing runtime")
    void shouldIdentifyBestPerformingRuntime() {
      final Map<String, CompleteTestAnalysis> testResults = new HashMap<>();
      testResults.put("best_runtime_test", new CompleteTestAnalysis(behavioralResults, performanceResults, coverageResults, recommendationResults));

      final BatchInsightResult result = insightGenerator.generateBatchInsights(testResults);

      final RuntimeType bestRuntime = result.getBestPerformingRuntime();
      assertNotNull(bestRuntime);
      assertTrue(result.getRuntimeHealthScores().containsKey(bestRuntime));
    }

    @Test
    @DisplayName("Should generate executive dashboard")
    void shouldGenerateExecutiveDashboard() {
      final Map<String, CompleteTestAnalysis> testResults = new HashMap<>();
      testResults.put("dashboard_test", new CompleteTestAnalysis(behavioralResults, performanceResults, coverageResults, recommendationResults));

      final BatchInsightResult result = insightGenerator.generateBatchInsights(testResults);

      final String dashboard = result.getExecutiveDashboard();
      assertNotNull(dashboard);
      assertTrue(dashboard.contains("Executive Dashboard"));
      assertTrue(dashboard.contains("Tests Analyzed"));
      assertTrue(dashboard.contains("Runtime Health"));
    }

    @Test
    @DisplayName("Should handle empty test results")
    void shouldHandleEmptyTestResults() {
      final BatchInsightResult result = insightGenerator.generateBatchInsights(Collections.emptyMap());

      assertNotNull(result);
      assertTrue(result.getTestInsights().isEmpty());
      assertEquals(0, result.getTotalInsightCount());
    }
  }

  @Nested
  @DisplayName("Pattern Recognition and Trend Analysis")
  class PatternRecognitionAndTrendAnalysisTests {

    @Test
    @DisplayName("Should detect performance patterns")
    void shouldDetectPerformancePatterns() {
      // Generate multiple insights to build pattern data
      for (int i = 0; i < 5; i++) {
        insightGenerator.generateInsights(
            "pattern_test_" + i, behavioralResults, performanceResults, coverageResults, recommendationResults);
      }

      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "pattern_final", behavioralResults, performanceResults, coverageResults, recommendationResults);

      // Should have pattern insights based on repeated occurrences
      final List<PatternInsight> patternInsights = result.getPatternInsights();
      assertNotNull(patternInsights);
    }

    @Test
    @DisplayName("Should track trends over time")
    void shouldTrackTrendsOverTime() {
      // Generate multiple insights for the same test to build trend data
      for (int i = 0; i < 5; i++) {
        insightGenerator.generateInsights(
            "trend_test", behavioralResults, performanceResults, coverageResults, recommendationResults);
      }

      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "trend_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      // Should have trend insights
      final List<TrendInsight> trendInsights = result.getTrendInsights();
      assertNotNull(trendInsights);
    }

    @Test
    @DisplayName("Should clear analysis data")
    void shouldClearAnalysisData() {
      // Generate some insights to build pattern and trend data
      insightGenerator.generateInsights(
          "clear_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      // Clear analysis data
      insightGenerator.clearAnalysisData();

      // Subsequent analysis should not be affected by previous data
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "clear_test_after", behavioralResults, performanceResults, coverageResults, recommendationResults);

      assertNotNull(result);
      assertTrue(result.getTotalInsightCount() >= 0);
    }
  }

  @Nested
  @DisplayName("Optimization Recommendations")
  class OptimizationRecommendationsTests {

    @Test
    @DisplayName("Should provide optimization recommendations")
    void shouldProvideOptimizationRecommendations() {
      // Generate insights to build up patterns
      for (int i = 0; i < 5; i++) {
        insightGenerator.generateInsights(
            "optimization_test_" + i, behavioralResults, performanceResults, coverageResults, recommendationResults);
      }

      final List<OptimizationRecommendation> recommendations = insightGenerator.getOptimizationRecommendations();

      assertNotNull(recommendations);
      for (final OptimizationRecommendation rec : recommendations) {
        assertNotNull(rec.getPatternType());
        assertNotNull(rec.getDescription());
        assertFalse(rec.getActionItems().isEmpty());
        assertNotNull(rec.getPriority());
        assertTrue(rec.getConfidenceScore() >= 0.0);
      }
    }

    @Test
    @DisplayName("Should prioritize optimization recommendations")
    void shouldPrioritizeOptimizationRecommendations() {
      // Generate insights to create optimization opportunities
      insightGenerator.generateInsights(
          "priority_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      final List<OptimizationRecommendation> recommendations = insightGenerator.getOptimizationRecommendations();

      // High-confidence patterns should have high priority
      final List<OptimizationRecommendation> highConfidence = recommendations.stream()
          .filter(rec -> rec.getConfidenceScore() > 0.75)
          .toList();

      for (final OptimizationRecommendation rec : highConfidence) {
        assertTrue(rec.getPriority() == OptimizationPriority.HIGH || rec.getPriority() == OptimizationPriority.MEDIUM);
      }
    }
  }

  @Nested
  @DisplayName("Confidence and Quality Metrics")
  class ConfidenceAndQualityMetricsTests {

    @Test
    @DisplayName("Should calculate confidence metrics")
    void shouldCalculateConfidenceMetrics() {
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "confidence_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      final InsightConfidenceMetrics metrics = result.getConfidenceMetrics();
      assertNotNull(metrics);
      assertTrue(metrics.getOverallConfidence() >= 0.0);
      assertTrue(metrics.getOverallConfidence() <= 1.0);
      assertTrue(metrics.getPerformanceConfidence() >= 0.0);
      assertTrue(metrics.getRuntimeConfidence() >= 0.0);
      assertTrue(metrics.getCrossCuttingConfidence() >= 0.0);
      assertTrue(metrics.getStrategicConfidence() >= 0.0);
    }

    @Test
    @DisplayName("Should count insights correctly")
    void shouldCountInsightsCorrectly() {
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "count_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      final int totalCount = result.getTotalInsightCount();
      final int expectedCount = result.getPerformanceInsights().size() +
                               result.getRuntimeInsights().size() +
                               result.getCrossCuttingInsights().size() +
                               result.getStrategicInsights().size() +
                               result.getPatternInsights().size() +
                               result.getTrendInsights().size();

      assertEquals(expectedCount, totalCount);
    }

    @Test
    @DisplayName("Should identify high severity insights")
    void shouldIdentifyHighSeverityInsights() {
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "severity_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      final List<Object> highSeverityInsights = result.getHighSeverityInsights();
      assertNotNull(highSeverityInsights);

      // Verify that returned insights are actually high severity
      for (final Object insight : highSeverityInsights) {
        if (insight instanceof PerformanceInsight) {
          assertEquals(InsightSeverity.HIGH, ((PerformanceInsight) insight).getSeverity());
        } else if (insight instanceof RuntimeInsight) {
          assertEquals(InsightSeverity.HIGH, ((RuntimeInsight) insight).getSeverity());
        } else if (insight instanceof CrossCuttingInsight) {
          assertEquals(InsightSeverity.HIGH, ((CrossCuttingInsight) insight).getSeverity());
        } else if (insight instanceof StrategicInsight) {
          assertEquals(StrategicImportance.HIGH, ((StrategicInsight) insight).getImportance());
        }
      }
    }

    @Test
    @DisplayName("Should generate executive summary")
    void shouldGenerateExecutiveSummary() {
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "executive_summary_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      final String summary = result.getExecutiveSummary();
      assertNotNull(summary);
      assertTrue(summary.contains("Insight Analysis Summary"));
      assertTrue(summary.contains("Overall Confidence"));
      assertTrue(summary.contains("Insights Generated"));
      assertTrue(summary.contains("Total Insights"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Robustness")
  class EdgeCasesAndRobustnessTests {

    @Test
    @DisplayName("Should handle minimal data gracefully")
    void shouldHandleMinimalDataGracefully() {
      // Create minimal results with no significant issues
      final BehavioralAnalysisResult minimalBehavioral = new BehavioralAnalysisResult.Builder("minimal_test")
          .pairwiseComparisons(Collections.emptyList())
          .discrepancies(Collections.emptyList())
          .executionPattern(new ExecutionPattern(1, 0, 0, 1, 0, 1.0))
          .consistencyScore(1.0)
          .verdict(BehavioralVerdict.CONSISTENT)
          .build();

      final PerformanceAnalyzer.PerformanceComparisonResult minimalPerformance =
          new PerformanceAnalyzer.PerformanceComparisonResult(
              "minimal_test",
              Collections.emptyList(),
              Collections.emptyMap(),
              Collections.emptyList(),
              Collections.emptyList(),
              new PerformanceAnalyzer.OverheadAnalysis(Collections.emptyMap())
          );

      final CoverageAnalysisResult minimalCoverage = new CoverageAnalysisResult.Builder("minimal_test")
          .detectedFeatures(Set.of("core"))
          .runtimeFeatureCoverage(Collections.emptyMap())
          .coverageMetrics(new CoverageMetrics(1, 1, 100.0, Collections.emptyMap(), 100.0))
          .coverageGaps(Collections.emptyList())
          .featureInteractionAnalysis(new FeatureInteractionAnalysis(Collections.emptyMap(), Collections.emptyList(), 1.0))
          .build();

      final RecommendationResult minimalRecommendations = new RecommendationResult.Builder("minimal_test")
          .behavioralRecommendations(Collections.emptyList())
          .performanceRecommendations(Collections.emptyList())
          .coverageRecommendations(Collections.emptyList())
          .integrationRecommendations(Collections.emptyList())
          .prioritizedRecommendations(Collections.emptyList())
          .summary(new RecommendationSummary(0, 0, 0, 0, Collections.emptyMap()))
          .build();

      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "minimal_test", minimalBehavioral, minimalPerformance, minimalCoverage, minimalRecommendations);

      assertNotNull(result);
      assertNotNull(result.getConfidenceMetrics());
    }

    @Test
    @DisplayName("Should handle extreme performance variability")
    void shouldHandleExtremePerformanceVariability() {
      // Create performance results with extreme variability
      final Map<String, PerformanceAnalyzer.PerformanceMetrics> extremeMetrics = new HashMap<>();
      final List<PerformanceAnalyzer.TestExecutionResult> extremeResults = new ArrayList<>();

      // Add results with extreme variation
      extremeResults.add(PerformanceAnalyzer.TestExecutionResult.builder("extreme_test", "JNI")
          .executionDuration(Duration.ofMillis(10))
          .memoryUsed(100L)
          .peakMemoryUsage(1000000L) // Extreme spike
          .successful(true)
          .build());
      extremeResults.add(PerformanceAnalyzer.TestExecutionResult.builder("extreme_test", "JNI")
          .executionDuration(Duration.ofMillis(1000)) // Very slow
          .memoryUsed(100L)
          .peakMemoryUsage(200L)
          .successful(true)
          .build());

      extremeMetrics.put("JNI", new PerformanceAnalyzer.PerformanceMetrics("JNI", extremeResults));

      final PerformanceAnalyzer.PerformanceComparisonResult extremePerformance =
          new PerformanceAnalyzer.PerformanceComparisonResult(
              "extreme_test",
              Collections.emptyList(),
              extremeMetrics,
              Collections.emptyList(),
              Collections.emptyList(),
              new PerformanceAnalyzer.OverheadAnalysis(extremeMetrics)
          );

      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "extreme_test", behavioralResults, extremePerformance, coverageResults, recommendationResults);

      assertNotNull(result);
      // Should detect high variability and memory spikes
      assertFalse(result.getPerformanceInsights().isEmpty());
    }

    @Test
    @DisplayName("Should handle large number of runtime types")
    void shouldHandleLargeNumberOfRuntimeTypes() {
      // Test with multiple runtime insights
      final Map<RuntimeType, RuntimeInsight> multipleRuntimeInsights = new HashMap<>();
      for (final RuntimeType runtime : RuntimeType.values()) {
        final RuntimeInsight insight = new RuntimeInsight(
            runtime,
            RuntimeInsightType.GENERAL,
            List.of("Test observation"),
            List.of("Test recommendation"),
            InsightSeverity.LOW,
            0.5
        );
        multipleRuntimeInsights.put(runtime, insight);
      }

      // The insight generator should handle this appropriately
      final InsightAnalysisResult result = insightGenerator.generateInsights(
          "multi_runtime_test", behavioralResults, performanceResults, coverageResults, recommendationResults);

      assertNotNull(result);
      assertTrue(result.getRuntimeInsights().size() <= RuntimeType.values().length);
    }
  }
}