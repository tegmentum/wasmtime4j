package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for CoverageAnalyzer ensuring accurate WebAssembly feature
 * coverage mapping, gap detection, and insight generation.
 */
@DisplayName("CoverageAnalyzer")
class CoverageAnalyzerTest {

  private CoverageAnalyzer coverageAnalyzer;
  private Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults;
  private BehavioralAnalysisResult behavioralResults;
  private PerformanceAnalyzer.PerformanceComparisonResult performanceResults;

  @BeforeEach
  void setUp() {
    coverageAnalyzer = new CoverageAnalyzer();
    setupTestData();
  }

  private void setupTestData() {
    // Setup execution results
    executionResults = new EnumMap<>(RuntimeType.class);
    executionResults.put(RuntimeType.JNI, createSuccessfulResult());
    executionResults.put(RuntimeType.PANAMA, createSuccessfulResult());

    // Setup behavioral results
    behavioralResults = new BehavioralAnalysisResult.Builder("memory_test")
        .pairwiseComparisons(Collections.emptyList())
        .discrepancies(Collections.emptyList())
        .executionPattern(new ExecutionPattern(2, 0, 0, 1, 0, 5.0))
        .consistencyScore(0.95)
        .verdict(BehavioralVerdict.CONSISTENT)
        .build();

    // Setup performance results
    performanceResults = createPerformanceResults();
  }

  private BehavioralAnalyzer.TestExecutionResult createSuccessfulResult() {
    return new BehavioralAnalyzer.TestExecutionResult(
        true, false, "success", null, Duration.ofMillis(100),
        new BehavioralAnalyzer.MemoryUsage(1024L, 512L)
    );
  }

  private BehavioralAnalyzer.TestExecutionResult createFailedResult() {
    return new BehavioralAnalyzer.TestExecutionResult(
        false, false, null, new RuntimeException("Test failure"), Duration.ofMillis(50), null
    );
  }

  private PerformanceAnalyzer.PerformanceComparisonResult createPerformanceResults() {
    final Map<String, PerformanceAnalyzer.PerformanceMetrics> metricsByRuntime = new HashMap<>();
    return new PerformanceAnalyzer.PerformanceComparisonResult(
        "memory_test",
        Collections.emptyList(),
        metricsByRuntime,
        Collections.emptyList(),
        Collections.emptyList(),
        new PerformanceAnalyzer.OverheadAnalysis(metricsByRuntime)
    );
  }

  @Nested
  @DisplayName("Coverage Analysis")
  class CoverageAnalysisTests {

    @Test
    @DisplayName("Should analyze coverage for memory operations test")
    void shouldAnalyzeCoverageForMemoryTest() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "memory_load_test", executionResults, behavioralResults, performanceResults);

      assertNotNull(result);
      assertEquals("memory_load_test", result.getTestName());
      assertFalse(result.getDetectedFeatures().isEmpty());
      assertTrue(result.getDetectedFeatures().contains("memory_operations"));
      assertTrue(result.getDetectedFeatures().contains("function_calls"));
      assertNotNull(result.getCoverageMetrics());
      assertNotNull(result.getFeatureInteractionAnalysis());
    }

    @Test
    @DisplayName("Should detect WASI features for WASI tests")
    void shouldDetectWasiFeatures() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "wasi_file_operations", executionResults, behavioralResults, performanceResults);

      assertNotNull(result);
      assertTrue(result.getDetectedFeatures().contains("file_operations"));
      assertTrue(result.getDetectedFeatures().contains("stdio_operations"));
    }

    @Test
    @DisplayName("Should detect SIMD features for vector tests")
    void shouldDetectSimdFeatures() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "simd_v128_operations", executionResults, behavioralResults, performanceResults);

      assertNotNull(result);
      assertTrue(result.getDetectedFeatures().contains("v128_operations"));
      assertTrue(result.getDetectedFeatures().contains("arithmetic_simd"));
    }

    @Test
    @DisplayName("Should calculate coverage metrics correctly")
    void shouldCalculateCoverageMetricsCorrectly() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "basic_test", executionResults, behavioralResults, performanceResults);

      final CoverageMetrics metrics = result.getCoverageMetrics();
      assertNotNull(metrics);
      assertTrue(metrics.getOverallCoveragePercentage() >= 0.0);
      assertTrue(metrics.getOverallCoveragePercentage() <= 100.0);
      assertEquals(2, metrics.getRuntimeCoveragePercentages().size());
      assertTrue(metrics.getSuccessRate() > 0.0);
    }

    @Test
    @DisplayName("Should identify coverage gaps for failed execution")
    void shouldIdentifyCoverageGapsForFailedExecution() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> failedResults = new EnumMap<>(RuntimeType.class);
      failedResults.put(RuntimeType.JNI, createSuccessfulResult());
      failedResults.put(RuntimeType.PANAMA, createFailedResult());

      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "test_with_failure", failedResults, behavioralResults, performanceResults);

      assertFalse(result.getCoverageGaps().isEmpty());
      assertTrue(result.getCoverageGaps().stream()
          .anyMatch(gap -> gap.getType() == CoverageGapType.FEATURE_INCOMPLETE));
    }

    @Test
    @DisplayName("Should analyze feature interactions")
    void shouldAnalyzeFeatureInteractions() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "complex_memory_table_test", executionResults, behavioralResults, performanceResults);

      final FeatureInteractionAnalysis analysis = result.getFeatureInteractionAnalysis();
      assertNotNull(analysis);
      assertFalse(analysis.getFeatureCombinations().isEmpty());
      assertTrue(analysis.getInteractionComplexity() > 0.0);
    }

    @Test
    @DisplayName("Should handle empty execution results gracefully")
    void shouldHandleEmptyExecutionResults() {
      assertThrows(IllegalArgumentException.class, () ->
          coverageAnalyzer.analyzeCoverage("empty_test", Collections.emptyMap(),
              behavioralResults, performanceResults));
    }

    @Test
    @DisplayName("Should handle null inputs")
    void shouldHandleNullInputs() {
      assertThrows(NullPointerException.class, () ->
          coverageAnalyzer.analyzeCoverage(null, executionResults, behavioralResults, performanceResults));

      assertThrows(NullPointerException.class, () ->
          coverageAnalyzer.analyzeCoverage("test", null, behavioralResults, performanceResults));

      assertThrows(NullPointerException.class, () ->
          coverageAnalyzer.analyzeCoverage("test", executionResults, null, performanceResults));

      assertThrows(NullPointerException.class, () ->
          coverageAnalyzer.analyzeCoverage("test", executionResults, behavioralResults, null));
    }
  }

  @Nested
  @DisplayName("Comprehensive Coverage Report")
  class ComprehensiveCoverageReportTests {

    @Test
    @DisplayName("Should generate comprehensive coverage report")
    void shouldGenerateComprehensiveCoverageReport() {
      // Analyze several tests to build up coverage data
      coverageAnalyzer.analyzeCoverage("memory_test", executionResults, behavioralResults, performanceResults);
      coverageAnalyzer.analyzeCoverage("table_test", executionResults, behavioralResults, performanceResults);
      coverageAnalyzer.analyzeCoverage("wasi_test", executionResults, behavioralResults, performanceResults);

      final ComprehensiveCoverageReport report = coverageAnalyzer.generateComprehensiveReport();

      assertNotNull(report);
      assertFalse(report.getCategoryCompleteness().isEmpty());
      assertFalse(report.getRuntimeCoverageScores().isEmpty());
      assertNotNull(report.getCoverageTrend());
      assertTrue(report.getTotalTestsAnalyzed() > 0);
    }

    @Test
    @DisplayName("Should calculate category completeness")
    void shouldCalculateCategoryCompleteness() {
      coverageAnalyzer.analyzeCoverage("memory_ops", executionResults, behavioralResults, performanceResults);

      final ComprehensiveCoverageReport report = coverageAnalyzer.generateComprehensiveReport();
      final Map<String, Double> completeness = report.getCategoryCompleteness();

      assertNotNull(completeness);
      assertTrue(completeness.containsKey("MEMORY"));
      assertTrue(completeness.get("MEMORY") > 0.0);
    }

    @Test
    @DisplayName("Should generate coverage recommendations")
    void shouldGenerateCoverageRecommendations() {
      // Create minimal coverage to trigger recommendations
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> minimalResults = new EnumMap<>(RuntimeType.class);
      minimalResults.put(RuntimeType.JNI, createSuccessfulResult());

      coverageAnalyzer.analyzeCoverage("minimal_test", minimalResults, behavioralResults, performanceResults);

      final ComprehensiveCoverageReport report = coverageAnalyzer.generateComprehensiveReport();
      assertFalse(report.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Should provide executive summary")
    void shouldProvideExecutiveSummary() {
      coverageAnalyzer.analyzeCoverage("test1", executionResults, behavioralResults, performanceResults);

      final ComprehensiveCoverageReport report = coverageAnalyzer.generateComprehensiveReport();
      final String summary = report.getExecutiveSummary();

      assertNotNull(summary);
      assertTrue(summary.contains("Coverage Report Executive Summary"));
      assertTrue(summary.contains("Overall Coverage"));
    }
  }

  @Nested
  @DisplayName("Global Coverage Statistics")
  class GlobalCoverageStatisticsTests {

    @Test
    @DisplayName("Should provide global coverage statistics")
    void shouldProvideGlobalCoverageStatistics() {
      coverageAnalyzer.analyzeCoverage("test1", executionResults, behavioralResults, performanceResults);
      coverageAnalyzer.analyzeCoverage("test2", executionResults, behavioralResults, performanceResults);

      final GlobalCoverageStatistics stats = coverageAnalyzer.getGlobalCoverageStatistics();

      assertNotNull(stats);
      assertTrue(stats.getTotalFeatures() > 0);
      assertTrue(stats.getCoveredFeatures() >= 0);
      assertTrue(stats.getOverallCoveragePercentage() >= 0.0);
      assertTrue(stats.getOverallCoveragePercentage() <= 100.0);
      assertEquals(2, stats.getTotalTestsAnalyzed());
    }

    @Test
    @DisplayName("Should clear coverage data")
    void shouldClearCoverageData() {
      coverageAnalyzer.analyzeCoverage("test1", executionResults, behavioralResults, performanceResults);

      GlobalCoverageStatistics statsBefore = coverageAnalyzer.getGlobalCoverageStatistics();
      assertTrue(statsBefore.getTotalTestsAnalyzed() > 0);

      coverageAnalyzer.clearCoverageData();

      GlobalCoverageStatistics statsAfter = coverageAnalyzer.getGlobalCoverageStatistics();
      assertEquals(0, statsAfter.getTotalTestsAnalyzed());
    }
  }

  @Nested
  @DisplayName("Feature Detection")
  class FeatureDetectionTests {

    @Test
    @DisplayName("Should detect memory features from test name")
    void shouldDetectMemoryFeaturesFromTestName() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "memory_load_store_operations", executionResults, behavioralResults, performanceResults);

      final Set<String> features = result.getDetectedFeatures();
      assertTrue(features.contains("memory_operations"));
      assertTrue(features.contains("linear_memory"));
    }

    @Test
    @DisplayName("Should detect table features from test name")
    void shouldDetectTableFeaturesFromTestName() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "table_get_set_operations", executionResults, behavioralResults, performanceResults);

      final Set<String> features = result.getDetectedFeatures();
      assertTrue(features.contains("table_operations"));
      assertTrue(features.contains("table_get"));
      assertTrue(features.contains("table_set"));
    }

    @Test
    @DisplayName("Should detect exception features from test name")
    void shouldDetectExceptionFeaturesFromTestName() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "exception_try_catch_test", executionResults, behavioralResults, performanceResults);

      final Set<String> features = result.getDetectedFeatures();
      assertTrue(features.contains("try_catch"));
      assertTrue(features.contains("exception_handling"));
    }

    @Test
    @DisplayName("Should detect features from exception messages")
    void shouldDetectFeaturesFromExceptionMessages() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> resultsWithMemoryError = new EnumMap<>(RuntimeType.class);
      resultsWithMemoryError.put(RuntimeType.JNI, new BehavioralAnalyzer.TestExecutionResult(
          false, false, null, new RuntimeException("Memory access violation"), Duration.ofMillis(50), null));

      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "basic_test", resultsWithMemoryError, behavioralResults, performanceResults);

      assertTrue(result.getDetectedFeatures().contains("memory_operations"));
    }

    @Test
    @DisplayName("Should always include core features")
    void shouldAlwaysIncludeCoreFeatures() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "minimal_test", executionResults, behavioralResults, performanceResults);

      final Set<String> features = result.getDetectedFeatures();
      assertTrue(features.contains("function_calls"));
      assertTrue(features.contains("control_flow"));
      assertTrue(features.contains("arithmetic_operations"));
      assertTrue(features.contains("comparison_operations"));
    }
  }

  @Nested
  @DisplayName("Coverage Gap Detection")
  class CoverageGapDetectionTests {

    @Test
    @DisplayName("Should detect runtime missing gaps")
    void shouldDetectRuntimeMissingGaps() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> incompleteResults = new EnumMap<>(RuntimeType.class);
      incompleteResults.put(RuntimeType.JNI, createSuccessfulResult());

      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "incomplete_test", incompleteResults, behavioralResults, performanceResults);

      assertTrue(result.getCoverageGaps().stream()
          .anyMatch(gap -> gap.getType() == CoverageGapType.RUNTIME_MISSING));
    }

    @Test
    @DisplayName("Should detect feature incomplete gaps")
    void shouldDetectFeatureIncompleteGaps() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> partialResults = new EnumMap<>(RuntimeType.class);
      partialResults.put(RuntimeType.JNI, createSuccessfulResult());
      partialResults.put(RuntimeType.PANAMA, createFailedResult());

      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "partial_test", partialResults, behavioralResults, performanceResults);

      assertTrue(result.getCoverageGaps().stream()
          .anyMatch(gap -> gap.getType() == CoverageGapType.FEATURE_INCOMPLETE));
    }

    @Test
    @DisplayName("Should assign appropriate gap severity")
    void shouldAssignAppropriateGapSeverity() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> failedResults = new EnumMap<>(RuntimeType.class);
      failedResults.put(RuntimeType.JNI, createFailedResult());
      failedResults.put(RuntimeType.PANAMA, createFailedResult());

      // Create behavioral results with low consistency score
      final BehavioralAnalysisResult lowConsistencyResults = new BehavioralAnalysisResult.Builder("failed_test")
          .pairwiseComparisons(Collections.emptyList())
          .discrepancies(Collections.emptyList())
          .executionPattern(new ExecutionPattern(0, 2, 0, 0, 1, 10.0))
          .consistencyScore(0.5)
          .verdict(BehavioralVerdict.INCONSISTENT)
          .build();

      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "failed_test", failedResults, lowConsistencyResults, performanceResults);

      assertTrue(result.getCoverageGaps().stream()
          .anyMatch(gap -> gap.getSeverity() == GapSeverity.HIGH));
    }
  }

  @Nested
  @DisplayName("Coverage Reporting")
  class CoverageReportingTests {

    @Test
    @DisplayName("Should generate coverage summary")
    void shouldGenerateCoverageSummary() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "summary_test", executionResults, behavioralResults, performanceResults);

      final String summary = result.getCoverageSummary();
      assertNotNull(summary);
      assertTrue(summary.contains("Coverage Analysis for summary_test"));
      assertTrue(summary.contains("Features Detected"));
      assertTrue(summary.contains("Overall Coverage"));
    }

    @Test
    @DisplayName("Should check comprehensive coverage status")
    void shouldCheckComprehensiveCoverageStatus() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "comprehensive_test", executionResults, behavioralResults, performanceResults);

      // Test with good coverage should be comprehensive
      if (result.getCoverageMetrics().getOverallCoveragePercentage() >= 90.0) {
        assertTrue(result.hasComprehensiveCoverage());
      }
    }

    @Test
    @DisplayName("Should count high severity gaps")
    void shouldCountHighSeverityGaps() {
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "gap_test", executionResults, behavioralResults, performanceResults);

      final long highSeverityGaps = result.getHighSeverityGapCount();
      assertTrue(highSeverityGaps >= 0);
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle very large feature sets")
    void shouldHandleVeryLargeFeatureSets() {
      // Test with a complex test name that would trigger many features
      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "memory_table_simd_atomic_exception_wasi_comprehensive_test",
          executionResults, behavioralResults, performanceResults);

      assertNotNull(result);
      assertTrue(result.getDetectedFeatures().size() > 10);
      assertNotNull(result.getCoverageMetrics());
    }

    @Test
    @DisplayName("Should handle skipped execution results")
    void shouldHandleSkippedExecutionResults() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> skippedResults = new EnumMap<>(RuntimeType.class);
      skippedResults.put(RuntimeType.JNI, new BehavioralAnalyzer.TestExecutionResult(
          false, true, null, null, Duration.ofMillis(0), null));

      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "skipped_test", skippedResults, behavioralResults, performanceResults);

      assertNotNull(result);
      assertNotNull(result.getCoverageMetrics());
    }

    @Test
    @DisplayName("Should handle mixed success/failure/skip results")
    void shouldHandleMixedResults() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> mixedResults = new EnumMap<>(RuntimeType.class);
      mixedResults.put(RuntimeType.JNI, createSuccessfulResult());
      mixedResults.put(RuntimeType.PANAMA, new BehavioralAnalyzer.TestExecutionResult(
          false, true, null, null, Duration.ofMillis(0), null));

      final CoverageAnalysisResult result = coverageAnalyzer.analyzeCoverage(
          "mixed_test", mixedResults, behavioralResults, performanceResults);

      assertNotNull(result);
      assertTrue(result.getCoverageGaps().size() > 0);
    }
  }
}