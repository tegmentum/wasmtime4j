package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
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

/**
 * Comprehensive test suite for RecommendationEngine ensuring accurate issue detection, priority
 * scoring, and actionable recommendation generation.
 */
@DisplayName("RecommendationEngine")
class RecommendationEngineTest {

  private RecommendationEngine recommendationEngine;
  private BehavioralAnalysisResult behavioralResults;
  private PerformanceAnalyzer.PerformanceComparisonResult performanceResults;
  private CoverageAnalysisResult coverageResults;

  @BeforeEach
  void setUp() {
    recommendationEngine = new RecommendationEngine();
    setupTestData();
  }

  private void setupTestData() {
    // Setup behavioral results with discrepancies
    final List<BehavioralDiscrepancy> discrepancies = new ArrayList<>();
    discrepancies.add(
        createDiscrepancy(
            "Exception type mismatch", DiscrepancySeverity.MAJOR, Set.of(RuntimeType.JNI)));

    behavioralResults =
        new BehavioralAnalysisResult.Builder("test_with_issues")
            .pairwiseComparisons(Collections.emptyList())
            .discrepancies(discrepancies)
            .executionPattern(new ExecutionPattern(1, 1, 0, 2, 1, 8.0))
            .consistencyScore(0.75)
            .verdict(BehavioralVerdict.MOSTLY_CONSISTENT)
            .build();

    // Setup performance results with regressions
    final Map<String, PerformanceAnalyzer.PerformanceMetrics> metricsByRuntime = new HashMap<>();
    performanceResults =
        new PerformanceAnalyzer.PerformanceComparisonResult(
            "test_with_issues",
            Collections.emptyList(),
            metricsByRuntime,
            List.of("JNI is 25% slower than PANAMA"),
            List.of("JNI shows 15% performance regression"),
            new PerformanceAnalyzer.OverheadAnalysis(metricsByRuntime));

    // Setup coverage results with gaps
    final List<CoverageGap> coverageGaps = new ArrayList<>();
    coverageGaps.add(
        new CoverageGap(
            CoverageGapType.FEATURE_INCOMPLETE,
            "Missing SIMD features in JNI",
            Set.of("v128_operations", "simd_arithmetic"),
            Set.of(RuntimeType.JNI),
            GapSeverity.HIGH));

    final CoverageMetrics coverageMetrics =
        new CoverageMetrics(
            10, 7, 70.0, Map.of(RuntimeType.JNI, 60.0, RuntimeType.PANAMA, 80.0), 85.0);

    final FeatureInteractionAnalysis interactionAnalysis =
        new FeatureInteractionAnalysis(
            Map.of("memory+simd", Set.of("memory", "simd")), List.of("memory+simd"), 6.5);

    coverageResults =
        new CoverageAnalysisResult.Builder("test_with_issues")
            .detectedFeatures(Set.of("memory", "simd", "core"))
            .runtimeFeatureCoverage(
                Map.of(
                    RuntimeType.JNI, Set.of("memory", "core"),
                    RuntimeType.PANAMA, Set.of("memory", "simd", "core")))
            .coverageMetrics(coverageMetrics)
            .coverageGaps(coverageGaps)
            .featureInteractionAnalysis(interactionAnalysis)
            .build();
  }

  private BehavioralDiscrepancy createDiscrepancy(
      final String description,
      final DiscrepancySeverity severity,
      final Set<RuntimeType> affectedRuntimes) {
    return new BehavioralDiscrepancy(
        DiscrepancyType.EXCEPTION_TYPE, description, severity, affectedRuntimes);
  }

  @Nested
  @DisplayName("Recommendation Generation")
  class RecommendationGenerationTests {

    @Test
    @DisplayName("Should generate recommendations for all issue types")
    void shouldGenerateRecommendationsForAllIssueTypes() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "test_with_issues", behavioralResults, performanceResults, coverageResults);

      assertNotNull(result);
      assertEquals("test_with_issues", result.getTestName());
      assertFalse(result.getBehavioralRecommendations().isEmpty());
      assertFalse(result.getPerformanceRecommendations().isEmpty());
      assertFalse(result.getCoverageRecommendations().isEmpty());
      assertNotNull(result.getSummary());
    }

    @Test
    @DisplayName("Should prioritize recommendations correctly")
    void shouldPrioritizeRecommendationsCorrectly() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "priority_test", behavioralResults, performanceResults, coverageResults);

      final List<ActionableRecommendation> prioritized = result.getPrioritizedRecommendations();
      assertFalse(prioritized.isEmpty());

      // Verify recommendations are sorted by priority score
      for (int i = 0; i < prioritized.size() - 1; i++) {
        assertTrue(
            prioritized.get(i).getPriorityScore() >= prioritized.get(i + 1).getPriorityScore());
      }
    }

    @Test
    @DisplayName("Should generate behavioral recommendations for discrepancies")
    void shouldGenerateBehavioralRecommendationsForDiscrepancies() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "behavioral_test", behavioralResults, performanceResults, coverageResults);

      final List<ActionableRecommendation> behavioralRecs = result.getBehavioralRecommendations();
      assertFalse(behavioralRecs.isEmpty());

      final boolean hasExceptionRecommendation =
          behavioralRecs.stream()
              .anyMatch(
                  rec ->
                      rec.getCategory() == IssueCategory.BEHAVIORAL
                          && rec.getDescription().toLowerCase().contains("exception"));
      assertTrue(hasExceptionRecommendation);
    }

    @Test
    @DisplayName("Should generate performance recommendations for regressions")
    void shouldGeneratePerformanceRecommendationsForRegressions() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "performance_test", behavioralResults, performanceResults, coverageResults);

      final List<ActionableRecommendation> performanceRecs = result.getPerformanceRecommendations();
      assertFalse(performanceRecs.isEmpty());

      final boolean hasRegressionRecommendation =
          performanceRecs.stream()
              .anyMatch(
                  rec ->
                      rec.getCategory() == IssueCategory.PERFORMANCE
                          && rec.getSeverity() == IssueSeverity.HIGH);
      assertTrue(hasRegressionRecommendation);
    }

    @Test
    @DisplayName("Should generate coverage recommendations for gaps")
    void shouldGenerateCoverageRecommendationsForGaps() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "coverage_test", behavioralResults, performanceResults, coverageResults);

      final List<ActionableRecommendation> coverageRecs = result.getCoverageRecommendations();
      assertFalse(coverageRecs.isEmpty());

      final boolean hasGapRecommendation =
          coverageRecs.stream().anyMatch(rec -> rec.getCategory() == IssueCategory.COVERAGE);
      assertTrue(hasGapRecommendation);
    }

    @Test
    @DisplayName("Should generate integration recommendations for correlated issues")
    void shouldGenerateIntegrationRecommendationsForCorrelatedIssues() {
      // Create results with both behavioral and performance issues
      final BehavioralAnalysisResult poorBehavioralResults =
          new BehavioralAnalysisResult.Builder("integration_test")
              .pairwiseComparisons(Collections.emptyList())
              .discrepancies(
                  List.of(
                      createDiscrepancy(
                          "Major issue", DiscrepancySeverity.MAJOR, Set.of(RuntimeType.JNI))))
              .executionPattern(new ExecutionPattern(1, 1, 0, 2, 1, 15.0))
              .consistencyScore(0.65)
              .verdict(BehavioralVerdict.INCONSISTENT)
              .build();

      final PerformanceAnalyzer.PerformanceComparisonResult regressionResults =
          new PerformanceAnalyzer.PerformanceComparisonResult(
              "integration_test",
              Collections.emptyList(),
              Collections.emptyMap(),
              Collections.emptyList(),
              List.of("Significant regression detected"),
              new PerformanceAnalyzer.OverheadAnalysis(Collections.emptyMap()));

      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "integration_test", poorBehavioralResults, regressionResults, coverageResults);

      assertFalse(result.getIntegrationRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void shouldHandleNullInputsGracefully() {
      assertThrows(
          NullPointerException.class,
          () ->
              recommendationEngine.generateRecommendations(
                  null, behavioralResults, performanceResults, coverageResults));

      assertThrows(
          NullPointerException.class,
          () ->
              recommendationEngine.generateRecommendations(
                  "test", null, performanceResults, coverageResults));

      assertThrows(
          NullPointerException.class,
          () ->
              recommendationEngine.generateRecommendations(
                  "test", behavioralResults, null, coverageResults));

      assertThrows(
          NullPointerException.class,
          () ->
              recommendationEngine.generateRecommendations(
                  "test", behavioralResults, performanceResults, null));
    }
  }

  @Nested
  @DisplayName("Batch Recommendation Generation")
  class BatchRecommendationGenerationTests {

    @Test
    @DisplayName("Should generate batch recommendations")
    void shouldGenerateBatchRecommendations() {
      final Map<String, TestAnalysisResults> testResults = new HashMap<>();
      testResults.put(
          "test1", new TestAnalysisResults(behavioralResults, performanceResults, coverageResults));
      testResults.put(
          "test2", new TestAnalysisResults(behavioralResults, performanceResults, coverageResults));

      final BatchRecommendationResult result =
          recommendationEngine.generateBatchRecommendations(testResults);

      assertNotNull(result);
      assertEquals(2, result.getTestRecommendations().size());
      assertNotNull(result.getSummary());
      assertNotNull(result.getIssueCategoryCounts());
    }

    @Test
    @DisplayName("Should identify common issues across tests")
    void shouldIdentifyCommonIssuesAcrossTests() {
      final Map<String, TestAnalysisResults> testResults = new HashMap<>();
      // Add multiple tests with similar issues
      for (int i = 1; i <= 5; i++) {
        testResults.put(
            "test" + i,
            new TestAnalysisResults(behavioralResults, performanceResults, coverageResults));
      }

      final BatchRecommendationResult result =
          recommendationEngine.generateBatchRecommendations(testResults);

      // Should identify common patterns
      assertFalse(result.getCommonIssues().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty test results")
    void shouldHandleEmptyTestResults() {
      final BatchRecommendationResult result =
          recommendationEngine.generateBatchRecommendations(Collections.emptyMap());

      assertNotNull(result);
      assertTrue(result.getTestRecommendations().isEmpty());
      assertTrue(result.getCommonIssues().isEmpty());
    }
  }

  @Nested
  @DisplayName("Recommendation Content and Quality")
  class RecommendationContentAndQualityTests {

    @Test
    @DisplayName("Should provide actionable implementation steps")
    void shouldProvideActionableImplementationSteps() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "actionable_test", behavioralResults, performanceResults, coverageResults);

      for (final ActionableRecommendation rec : result.getPrioritizedRecommendations()) {
        assertFalse(rec.getImplementationSteps().isEmpty());
        assertTrue(rec.getImplementationSteps().size() >= 1);

        // Check that steps are descriptive and actionable
        for (final String step : rec.getImplementationSteps()) {
          assertNotNull(step);
          assertFalse(step.trim().isEmpty());
          assertTrue(step.length() > 10); // Should be descriptive
        }
      }
    }

    @Test
    @DisplayName("Should assign appropriate severity levels")
    void shouldAssignAppropriateSeverityLevels() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "severity_test", behavioralResults, performanceResults, coverageResults);

      final List<ActionableRecommendation> recommendations = result.getPrioritizedRecommendations();
      assertFalse(recommendations.isEmpty());

      // Should have mix of severity levels
      final boolean hasHighSeverity =
          recommendations.stream().anyMatch(rec -> rec.getSeverity() == IssueSeverity.HIGH);
      assertTrue(hasHighSeverity);

      // High severity recommendations should have high priority scores
      for (final ActionableRecommendation rec : recommendations) {
        if (rec.getSeverity() == IssueSeverity.HIGH) {
          assertTrue(rec.getPriorityScore() > 0.6);
        }
      }
    }

    @Test
    @DisplayName("Should categorize issues correctly")
    void shouldCategorizeIssuesCorrectly() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "category_test", behavioralResults, performanceResults, coverageResults);

      final Map<IssueCategory, Integer> categoryBreakdown =
          result.getSummary().getCategoryBreakdown();

      // Should have recommendations in multiple categories
      assertTrue(categoryBreakdown.size() > 1);
      assertTrue(
          categoryBreakdown.containsKey(IssueCategory.BEHAVIORAL)
              || categoryBreakdown.containsKey(IssueCategory.PERFORMANCE)
              || categoryBreakdown.containsKey(IssueCategory.COVERAGE));
    }

    @Test
    @DisplayName("Should identify affected runtimes correctly")
    void shouldIdentifyAffectedRuntimesCorrectly() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "runtime_test", behavioralResults, performanceResults, coverageResults);

      for (final ActionableRecommendation rec : result.getPrioritizedRecommendations()) {
        assertFalse(rec.getAffectedRuntimes().isEmpty());
        assertTrue(rec.getAffectedRuntimes().size() <= RuntimeType.values().length);
      }
    }

    @Test
    @DisplayName("Should generate action plans")
    void shouldGenerateActionPlans() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "action_plan_test", behavioralResults, performanceResults, coverageResults);

      for (final ActionableRecommendation rec : result.getPrioritizedRecommendations()) {
        final String actionPlan = rec.getActionPlan();
        assertNotNull(actionPlan);
        assertTrue(actionPlan.contains("Recommendation:"));
        assertTrue(actionPlan.contains("Priority:"));
        assertTrue(actionPlan.contains("Implementation Steps:"));
      }
    }
  }

  @Nested
  @DisplayName("Category-Specific Recommendations")
  class CategorySpecificRecommendationsTests {

    @Test
    @DisplayName("Should provide behavioral category recommendations")
    void shouldProvideBehavioralCategoryRecommendations() {
      final List<ActionableRecommendation> recommendations =
          recommendationEngine.getRecommendationsForCategory(IssueCategory.BEHAVIORAL);

      assertFalse(recommendations.isEmpty());
      assertTrue(
          recommendations.stream().allMatch(rec -> rec.getCategory() == IssueCategory.BEHAVIORAL));
    }

    @Test
    @DisplayName("Should provide performance category recommendations")
    void shouldProvidePerformanceCategoryRecommendations() {
      final List<ActionableRecommendation> recommendations =
          recommendationEngine.getRecommendationsForCategory(IssueCategory.PERFORMANCE);

      assertFalse(recommendations.isEmpty());
      assertTrue(
          recommendations.stream().allMatch(rec -> rec.getCategory() == IssueCategory.PERFORMANCE));
    }

    @Test
    @DisplayName("Should provide coverage category recommendations")
    void shouldProvideCoverageCategoryRecommendations() {
      final List<ActionableRecommendation> recommendations =
          recommendationEngine.getRecommendationsForCategory(IssueCategory.COVERAGE);

      assertFalse(recommendations.isEmpty());
      assertTrue(
          recommendations.stream().allMatch(rec -> rec.getCategory() == IssueCategory.COVERAGE));
    }

    @Test
    @DisplayName("Should provide integration category recommendations")
    void shouldProvideIntegrationCategoryRecommendations() {
      final List<ActionableRecommendation> recommendations =
          recommendationEngine.getRecommendationsForCategory(IssueCategory.INTEGRATION);

      assertFalse(recommendations.isEmpty());
      assertTrue(
          recommendations.stream().allMatch(rec -> rec.getCategory() == IssueCategory.INTEGRATION));
    }
  }

  @Nested
  @DisplayName("Frequency Tracking and Pattern Recognition")
  class FrequencyTrackingAndPatternRecognitionTests {

    @Test
    @DisplayName("Should track issue frequency across multiple analyses")
    void shouldTrackIssueFrequencyAcrossMultipleAnalyses() {
      // Generate recommendations multiple times to build frequency data
      for (int i = 0; i < 5; i++) {
        recommendationEngine.generateRecommendations(
            "frequency_test_" + i, behavioralResults, performanceResults, coverageResults);
      }

      // The same patterns should appear frequently, affecting priority scores
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "frequency_test_final", behavioralResults, performanceResults, coverageResults);

      // Recommendations for frequently occurring patterns should have higher priority scores
      assertTrue(
          result.getPrioritizedRecommendations().stream()
              .anyMatch(rec -> rec.getPriorityScore() > 0.5));
    }

    @Test
    @DisplayName("Should clear frequency data")
    void shouldClearFrequencyData() {
      // Generate some recommendations to build frequency data
      recommendationEngine.generateRecommendations(
          "clear_test", behavioralResults, performanceResults, coverageResults);

      // Clear frequency data
      recommendationEngine.clearFrequencyData();

      // Subsequent analysis should not be affected by previous frequency data
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "clear_test_after", behavioralResults, performanceResults, coverageResults);

      assertNotNull(result);
      assertFalse(result.getPrioritizedRecommendations().isEmpty());
    }
  }

  @Nested
  @DisplayName("Result Analysis and Filtering")
  class ResultAnalysisAndFilteringTests {

    @Test
    @DisplayName("Should filter high priority recommendations")
    void shouldFilterHighPriorityRecommendations() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "high_priority_test", behavioralResults, performanceResults, coverageResults);

      final List<ActionableRecommendation> highPriority = result.getHighPriorityRecommendations();

      for (final ActionableRecommendation rec : highPriority) {
        assertEquals(IssueSeverity.HIGH, rec.getSeverity());
      }
    }

    @Test
    @DisplayName("Should filter recommendations by runtime")
    void shouldFilterRecommendationsByRuntime() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "runtime_filter_test", behavioralResults, performanceResults, coverageResults);

      final List<ActionableRecommendation> jniRecommendations =
          result.getRecommendationsForRuntime(RuntimeType.JNI);
      final List<ActionableRecommendation> panamaRecommendations =
          result.getRecommendationsForRuntime(RuntimeType.PANAMA);

      // All JNI recommendations should affect JNI runtime
      for (final ActionableRecommendation rec : jniRecommendations) {
        assertTrue(rec.getAffectedRuntimes().contains(RuntimeType.JNI));
      }

      // All Panama recommendations should affect Panama runtime
      for (final ActionableRecommendation rec : panamaRecommendations) {
        assertTrue(rec.getAffectedRuntimes().contains(RuntimeType.PANAMA));
      }
    }

    @Test
    @DisplayName("Should generate executive summary")
    void shouldGenerateExecutiveSummary() {
      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "executive_test", behavioralResults, performanceResults, coverageResults);

      final String summary = result.getExecutiveSummary();
      assertNotNull(summary);
      assertTrue(summary.contains("Recommendation Summary"));
      assertTrue(summary.contains("Total Recommendations"));
      assertTrue(summary.contains("High Priority"));
      assertTrue(summary.contains("By Category"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Robustness")
  class EdgeCasesAndRobustnessTests {

    @Test
    @DisplayName("Should handle perfect results with no issues")
    void shouldHandlePerfectResultsWithNoIssues() {
      // Create perfect results with no issues
      final BehavioralAnalysisResult perfectBehavioral =
          new BehavioralAnalysisResult.Builder("perfect_test")
              .pairwiseComparisons(Collections.emptyList())
              .discrepancies(Collections.emptyList())
              .executionPattern(new ExecutionPattern(2, 0, 0, 1, 0, 1.0))
              .consistencyScore(1.0)
              .verdict(BehavioralVerdict.CONSISTENT)
              .build();

      final PerformanceAnalyzer.PerformanceComparisonResult perfectPerformance =
          new PerformanceAnalyzer.PerformanceComparisonResult(
              "perfect_test",
              Collections.emptyList(),
              Collections.emptyMap(),
              Collections.emptyList(),
              Collections.emptyList(),
              new PerformanceAnalyzer.OverheadAnalysis(Collections.emptyMap()));

      final CoverageAnalysisResult perfectCoverage =
          new CoverageAnalysisResult.Builder("perfect_test")
              .detectedFeatures(Set.of("core"))
              .runtimeFeatureCoverage(
                  Map.of(RuntimeType.JNI, Set.of("core"), RuntimeType.PANAMA, Set.of("core")))
              .coverageMetrics(
                  new CoverageMetrics(
                      1,
                      1,
                      100.0,
                      Map.of(RuntimeType.JNI, 100.0, RuntimeType.PANAMA, 100.0),
                      100.0))
              .coverageGaps(Collections.emptyList())
              .featureInteractionAnalysis(
                  new FeatureInteractionAnalysis(
                      Collections.emptyMap(), Collections.emptyList(), 1.0))
              .build();

      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "perfect_test", perfectBehavioral, perfectPerformance, perfectCoverage);

      assertNotNull(result);
      // Should still generate some recommendations (general best practices, etc.)
      assertNotNull(result.getSummary());
    }

    @Test
    @DisplayName("Should handle extreme failure scenarios")
    void shouldHandleExtremeFailureScenarios() {
      // Create results with many severe issues
      final List<BehavioralDiscrepancy> severeDiscrepancies = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        severeDiscrepancies.add(
            createDiscrepancy(
                "Critical issue " + i,
                DiscrepancySeverity.CRITICAL,
                Set.of(RuntimeType.JNI, RuntimeType.PANAMA)));
      }

      final BehavioralAnalysisResult severeResults =
          new BehavioralAnalysisResult.Builder("severe_test")
              .pairwiseComparisons(Collections.emptyList())
              .discrepancies(severeDiscrepancies)
              .executionPattern(new ExecutionPattern(0, 2, 0, 5, 3, 50.0))
              .consistencyScore(0.1)
              .verdict(BehavioralVerdict.INCOMPATIBLE)
              .build();

      final RecommendationResult result =
          recommendationEngine.generateRecommendations(
              "severe_test", severeResults, performanceResults, coverageResults);

      assertNotNull(result);
      assertTrue(result.getSummary().getHighPriorityCount() > 0);
    }
  }
}
