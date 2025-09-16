package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration test demonstrating the complete Stream C analysis pipeline including
 * CoverageAnalyzer, RecommendationEngine, and InsightGenerator working together with existing
 * behavioral and performance analyzers.
 */
@DisplayName("Stream C Integration")
class StreamCIntegrationTest {

  private CoverageAnalyzer coverageAnalyzer;
  private RecommendationEngine recommendationEngine;
  private InsightGenerator insightGenerator;
  private BehavioralAnalyzer behavioralAnalyzer;
  private PerformanceAnalyzer performanceAnalyzer;

  @BeforeEach
  void setUp() {
    coverageAnalyzer = new CoverageAnalyzer();
    recommendationEngine = new RecommendationEngine();
    insightGenerator = new InsightGenerator();
    behavioralAnalyzer = new BehavioralAnalyzer();
    performanceAnalyzer = new PerformanceAnalyzer();
  }

  @Test
  @DisplayName("Should demonstrate complete analysis pipeline")
  void shouldDemonstrateCompleteAnalysisPipeline() {
    // 1. Setup test execution results for multiple runtimes
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
        createSampleExecutionResults();

    // 2. Perform behavioral analysis (Stream A)
    final BehavioralAnalysisResult behavioralResults =
        behavioralAnalyzer.analyze("memory_simd_integration_test", executionResults);

    assertNotNull(behavioralResults);
    assertTrue(behavioralResults.getConsistencyScore() > 0.0);

    // 3. Perform performance analysis (Stream B)
    final List<PerformanceAnalyzer.TestExecutionResult> performanceResultsList =
        convertToPerformanceResults(executionResults);
    final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
        performanceAnalyzer.analyze(performanceResultsList);

    assertNotNull(performanceResults);
    assertEquals("memory_simd_integration_test", performanceResults.getTestName());

    // 4. Perform coverage analysis (Stream C)
    final CoverageAnalysisResult coverageResults =
        coverageAnalyzer.analyzeCoverage(
            "memory_simd_integration_test",
            executionResults,
            behavioralResults,
            performanceResults);

    assertNotNull(coverageResults);
    assertFalse(coverageResults.getDetectedFeatures().isEmpty());
    assertTrue(coverageResults.getDetectedFeatures().contains("memory_operations"));
    assertTrue(coverageResults.getDetectedFeatures().contains("v128_operations"));

    // 5. Generate recommendations (Stream C)
    final RecommendationResult recommendationResults =
        recommendationEngine.generateRecommendations(
            "memory_simd_integration_test", behavioralResults, performanceResults, coverageResults);

    assertNotNull(recommendationResults);
    assertNotNull(recommendationResults.getSummary());

    // 6. Generate insights (Stream C)
    final InsightAnalysisResult insightResults =
        insightGenerator.generateInsights(
            "memory_simd_integration_test",
            behavioralResults,
            performanceResults,
            coverageResults,
            recommendationResults);

    assertNotNull(insightResults);
    assertTrue(insightResults.getTotalInsightCount() > 0);

    // 7. Verify integration and data flow
    verifyIntegrationConsistency(
        behavioralResults,
        performanceResults,
        coverageResults,
        recommendationResults,
        insightResults);
  }

  @Test
  @DisplayName("Should handle end-to-end batch analysis")
  void shouldHandleEndToEndBatchAnalysis() {
    // Setup multiple test scenarios
    final Map<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>> multipleTests =
        createMultipleTestScenarios();

    final Map<String, CompleteTestAnalysis> completeAnalyses = new HashMap<>();

    // Analyze each test scenario
    for (final Map.Entry<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>> entry :
        multipleTests.entrySet()) {
      final String testName = entry.getKey();
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> execResults = entry.getValue();

      // Run complete analysis pipeline for each test
      final BehavioralAnalysisResult behavioralResult =
          behavioralAnalyzer.analyze(testName, execResults);

      final List<PerformanceAnalyzer.TestExecutionResult> perfResults =
          convertToPerformanceResults(execResults);
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResult =
          performanceAnalyzer.analyze(perfResults);

      final CoverageAnalysisResult coverageResult =
          coverageAnalyzer.analyzeCoverage(
              testName, execResults, behavioralResult, performanceResult);

      final RecommendationResult recommendationResult =
          recommendationEngine.generateRecommendations(
              testName, behavioralResult, performanceResult, coverageResult);

      completeAnalyses.put(
          testName,
          new CompleteTestAnalysis(
              behavioralResult, performanceResult, coverageResult, recommendationResult));
    }

    // Generate batch insights
    final BatchInsightResult batchInsights =
        insightGenerator.generateBatchInsights(completeAnalyses);
    assertNotNull(batchInsights);
    assertEquals(multipleTests.size(), batchInsights.getTestInsights().size());

    // Generate batch recommendations
    final Map<String, TestAnalysisResults> testAnalysisResults = new HashMap<>();
    for (final Map.Entry<String, CompleteTestAnalysis> entry : completeAnalyses.entrySet()) {
      final CompleteTestAnalysis analysis = entry.getValue();
      testAnalysisResults.put(
          entry.getKey(),
          new TestAnalysisResults(
              analysis.getBehavioralResults(),
              analysis.getPerformanceResults(),
              analysis.getCoverageResults()));
    }

    final BatchRecommendationResult batchRecommendations =
        recommendationEngine.generateBatchRecommendations(testAnalysisResults);
    assertNotNull(batchRecommendations);

    // Generate comprehensive coverage report
    final ComprehensiveCoverageReport coverageReport =
        coverageAnalyzer.generateComprehensiveReport();
    assertNotNull(coverageReport);
    assertTrue(coverageReport.getTotalTestsAnalyzed() > 0);

    // Verify batch-level consistency
    verifyBatchConsistency(batchInsights, batchRecommendations, coverageReport);
  }

  @Test
  @DisplayName("Should demonstrate error handling and resilience")
  void shouldDemonstrateErrorHandlingAndResilience() {
    // Create mixed success/failure scenario
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> mixedResults =
        new EnumMap<>(RuntimeType.class);
    mixedResults.put(
        RuntimeType.JNI,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            "success",
            null,
            Duration.ofMillis(100),
            new BehavioralAnalyzer.MemoryUsage(1024L, 512L)));
    mixedResults.put(
        RuntimeType.PANAMA,
        new BehavioralAnalyzer.TestExecutionResult(
            false,
            false,
            null,
            new RuntimeException("SIMD not implemented"),
            Duration.ofMillis(50),
            null));

    // Run analysis pipeline with error conditions
    final BehavioralAnalysisResult behavioralResults =
        behavioralAnalyzer.analyze("error_handling_test", mixedResults);

    final List<PerformanceAnalyzer.TestExecutionResult> performanceResultsList =
        convertToPerformanceResults(mixedResults);
    final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
        performanceAnalyzer.analyze(performanceResultsList);

    final CoverageAnalysisResult coverageResults =
        coverageAnalyzer.analyzeCoverage(
            "error_handling_test", mixedResults, behavioralResults, performanceResults);

    final RecommendationResult recommendationResults =
        recommendationEngine.generateRecommendations(
            "error_handling_test", behavioralResults, performanceResults, coverageResults);

    final InsightAnalysisResult insightResults =
        insightGenerator.generateInsights(
            "error_handling_test",
            behavioralResults,
            performanceResults,
            coverageResults,
            recommendationResults);

    // Verify that analysis pipeline handles errors gracefully
    assertNotNull(behavioralResults);
    assertNotNull(performanceResults);
    assertNotNull(coverageResults);
    assertNotNull(recommendationResults);
    assertNotNull(insightResults);

    // Should detect coverage gaps and generate appropriate recommendations
    assertFalse(coverageResults.getCoverageGaps().isEmpty());
    assertTrue(recommendationResults.getSummary().getTotalRecommendations() > 0);
    assertTrue(insightResults.getTotalInsightCount() > 0);
  }

  @Test
  @DisplayName("Should demonstrate real-world usage patterns")
  void shouldDemonstrateRealWorldUsagePatterns() {
    // Simulate a real-world testing scenario with various WebAssembly features
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> realWorldResults =
        createRealWorldScenario();

    // Step 1: Initial analysis
    final BehavioralAnalysisResult initialBehavioral =
        behavioralAnalyzer.analyze("real_world_wasm_test", realWorldResults);

    final List<PerformanceAnalyzer.TestExecutionResult> initialPerformance =
        convertToPerformanceResults(realWorldResults);
    final PerformanceAnalyzer.PerformanceComparisonResult initialPerfResults =
        performanceAnalyzer.analyze(initialPerformance);

    // Step 2: Coverage analysis with feature detection
    final CoverageAnalysisResult initialCoverage =
        coverageAnalyzer.analyzeCoverage(
            "real_world_wasm_test", realWorldResults, initialBehavioral, initialPerfResults);

    // Verify real-world feature detection
    assertTrue(initialCoverage.getDetectedFeatures().contains("memory_operations"));
    assertTrue(initialCoverage.getDetectedFeatures().contains("function_calls"));
    assertTrue(initialCoverage.getDetectedFeatures().contains("arithmetic_operations"));

    // Step 3: Generate actionable recommendations
    final RecommendationResult recommendations =
        recommendationEngine.generateRecommendations(
            "real_world_wasm_test", initialBehavioral, initialPerfResults, initialCoverage);

    // Should have practical recommendations
    assertFalse(recommendations.getPrioritizedRecommendations().isEmpty());

    // Step 4: Generate insights for development planning
    final InsightAnalysisResult insights =
        insightGenerator.generateInsights(
            "real_world_wasm_test",
            initialBehavioral,
            initialPerfResults,
            initialCoverage,
            recommendations);

    // Should provide strategic insights
    assertTrue(insights.getConfidenceMetrics().getOverallConfidence() > 0.0);

    // Step 5: Generate executive summary for stakeholders
    final String executiveSummary = insights.getExecutiveSummary();
    assertNotNull(executiveSummary);
    assertTrue(executiveSummary.contains("Insight Analysis Summary"));

    final String recommendationSummary = recommendations.getExecutiveSummary();
    assertNotNull(recommendationSummary);
    assertTrue(recommendationSummary.contains("Recommendation Summary"));

    final String coverageSummary = initialCoverage.getCoverageSummary();
    assertNotNull(coverageSummary);
    assertTrue(coverageSummary.contains("Coverage Analysis"));
  }

  private Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> createSampleExecutionResults() {
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> results =
        new EnumMap<>(RuntimeType.class);

    results.put(
        RuntimeType.JNI,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            "success",
            null,
            Duration.ofMillis(120),
            new BehavioralAnalyzer.MemoryUsage(2048L, 1024L)));

    results.put(
        RuntimeType.PANAMA,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            "success",
            null,
            Duration.ofMillis(95),
            new BehavioralAnalyzer.MemoryUsage(1536L, 768L)));

    return results;
  }

  private Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> createRealWorldScenario() {
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> results =
        new EnumMap<>(RuntimeType.class);

    // JNI with slightly higher latency but stable
    results.put(
        RuntimeType.JNI,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            42,
            null,
            Duration.ofMillis(85),
            new BehavioralAnalyzer.MemoryUsage(1024L, 512L)));

    // Panama with better performance but using more memory
    results.put(
        RuntimeType.PANAMA,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            42,
            null,
            Duration.ofMillis(65),
            new BehavioralAnalyzer.MemoryUsage(1536L, 768L)));

    return results;
  }

  private Map<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>>
      createMultipleTestScenarios() {
    final Map<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>> scenarios =
        new HashMap<>();

    // Memory-focused test
    scenarios.put("memory_operations_test", createMemoryTestResults());

    // SIMD-focused test
    scenarios.put("simd_v128_test", createSimdTestResults());

    // Table operations test
    scenarios.put("table_operations_test", createTableTestResults());

    return scenarios;
  }

  private Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> createMemoryTestResults() {
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> results =
        new EnumMap<>(RuntimeType.class);

    results.put(
        RuntimeType.JNI,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            "memory_success",
            null,
            Duration.ofMillis(75),
            new BehavioralAnalyzer.MemoryUsage(4096L, 2048L)));

    results.put(
        RuntimeType.PANAMA,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            "memory_success",
            null,
            Duration.ofMillis(60),
            new BehavioralAnalyzer.MemoryUsage(3072L, 1536L)));

    return results;
  }

  private Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> createSimdTestResults() {
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> results =
        new EnumMap<>(RuntimeType.class);

    results.put(
        RuntimeType.JNI,
        new BehavioralAnalyzer.TestExecutionResult(
            false,
            false,
            null,
            new RuntimeException("SIMD not fully implemented"),
            Duration.ofMillis(25),
            null));

    results.put(
        RuntimeType.PANAMA,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            "simd_result",
            null,
            Duration.ofMillis(45),
            new BehavioralAnalyzer.MemoryUsage(1024L, 512L)));

    return results;
  }

  private Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> createTableTestResults() {
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> results =
        new EnumMap<>(RuntimeType.class);

    results.put(
        RuntimeType.JNI,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            "table_result",
            null,
            Duration.ofMillis(110),
            new BehavioralAnalyzer.MemoryUsage(2048L, 1024L)));

    results.put(
        RuntimeType.PANAMA,
        new BehavioralAnalyzer.TestExecutionResult(
            true,
            false,
            "table_result",
            null,
            Duration.ofMillis(90),
            new BehavioralAnalyzer.MemoryUsage(1536L, 768L)));

    return results;
  }

  private List<PerformanceAnalyzer.TestExecutionResult> convertToPerformanceResults(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {

    final List<PerformanceAnalyzer.TestExecutionResult> results = new java.util.ArrayList<>();

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry :
        executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult execResult = entry.getValue();

      final PerformanceAnalyzer.TestExecutionResult perfResult =
          PerformanceAnalyzer.TestExecutionResult.builder("test", runtime.name())
              .executionDuration(execResult.getExecutionTime())
              .memoryUsed(execResult.getMemoryUsage().map(m -> m.getHeapUsed()).orElse(0L))
              .peakMemoryUsage(execResult.getMemoryUsage().map(m -> m.getNonHeapUsed()).orElse(0L))
              .successful(execResult.isSuccessful())
              .errorMessage(
                  execResult.getException() != null ? execResult.getException().getMessage() : null)
              .build();

      results.add(perfResult);
    }

    return results;
  }

  private void verifyIntegrationConsistency(
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults,
      final RecommendationResult recommendationResults,
      final InsightAnalysisResult insightResults) {

    // Verify test name consistency
    assertEquals(behavioralResults.getTestName(), performanceResults.getTestName());
    assertEquals(performanceResults.getTestName(), coverageResults.getTestName());
    assertEquals(coverageResults.getTestName(), recommendationResults.getTestName());
    assertEquals(recommendationResults.getTestName(), insightResults.getTestName());

    // Verify data flow consistency - recommendations should reference analysis results
    if (behavioralResults.getConsistencyScore() < 0.8) {
      assertTrue(
          recommendationResults.getBehavioralRecommendations().size() > 0
              || recommendationResults.getIntegrationRecommendations().size() > 0);
    }

    if (performanceResults.hasRegressions()) {
      assertTrue(recommendationResults.getPerformanceRecommendations().size() > 0);
    }

    if (!coverageResults.getCoverageGaps().isEmpty()) {
      assertTrue(recommendationResults.getCoverageRecommendations().size() > 0);
    }

    // Verify insights reference recommendation and analysis data
    assertTrue(insightResults.getConfidenceMetrics().getOverallConfidence() > 0.0);

    // If there are high-priority recommendations, there should be insights
    if (recommendationResults.getHighPriorityRecommendations().size() > 0) {
      assertTrue(insightResults.getTotalInsightCount() > 0);
    }
  }

  private void verifyBatchConsistency(
      final BatchInsightResult batchInsights,
      final BatchRecommendationResult batchRecommendations,
      final ComprehensiveCoverageReport coverageReport) {

    // Verify batch processing consistency
    assertEquals(
        batchInsights.getTestInsights().size(),
        batchRecommendations.getTestRecommendations().size());

    // Coverage report should reflect analyzed tests
    assertTrue(coverageReport.getTotalTestsAnalyzed() > 0);

    // Batch confidence should be reasonable
    assertTrue(batchInsights.getBatchConfidenceScore() >= 0.0);
    assertTrue(batchInsights.getBatchConfidenceScore() <= 1.0);

    // Should have runtime health scores for all tested runtimes
    assertFalse(batchInsights.getRuntimeHealthScores().isEmpty());

    // Should have executive insights
    assertFalse(batchInsights.getExecutiveInsights().isEmpty());
  }
}
