package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Comprehensive tests for WasmtimeCoverageAnalyzer to validate enhanced coverage analysis
 * functionality with Wasmtime-specific features.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WasmtimeCoverageAnalyzerTest {

  private WasmtimeCoverageAnalyzer wasmtimeCoverageAnalyzer;
  private WasmtimeCoverageIntegrator coverageIntegrator;

  @BeforeEach
  void setUp() {
    wasmtimeCoverageAnalyzer = new WasmtimeCoverageAnalyzer();
    coverageIntegrator = new WasmtimeCoverageIntegrator();
  }

  @Test
  @DisplayName("Should detect Wasmtime-specific features from test case names")
  void shouldDetectWasmtimeFeatures() {
    // Given: A test case with memory operations
    final WasmTestCase memoryTestCase =
        createTestCase("memory_grow_test", WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
        createSuccessfulExecutionResults();
    final BehavioralAnalysisResult behavioralResults = createMockBehavioralResults();
    final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
        createMockPerformanceResults();

    // When: Analyzing coverage
    final WasmtimeCoverageAnalysisResult result =
        wasmtimeCoverageAnalyzer.analyzeWasmtimeCoverage(
            memoryTestCase, executionResults, behavioralResults, performanceResults);

    // Then: Should detect memory-related features
    assertTrue(result.getWasmtimeFeatures().contains("memory_grow"));
    assertTrue(result.getWasmtimeFeatures().contains("memory_size"));
    assertEquals("memory_grow_test", result.getTestName());
    assertEquals(WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS, result.getSuiteType());
  }

  @Test
  @DisplayName("Should calculate compatibility scores for all runtimes")
  void shouldCalculateCompatibilityScores() {
    // Given: A test case with mixed runtime results
    final WasmTestCase testCase =
        createTestCase("func_call_test", WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> mixedResults =
        Map.of(
            RuntimeType.JNI, new BehavioralAnalyzer.TestExecutionResult(true, null, 100L, 200L),
            RuntimeType.PANAMA,
                new BehavioralAnalyzer.TestExecutionResult(
                    false, new RuntimeException("Test failure"), 100L, 150L));
    final BehavioralAnalysisResult behavioralResults = createMockBehavioralResults();
    final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
        createMockPerformanceResults();

    // When: Analyzing coverage
    final WasmtimeCoverageAnalysisResult result =
        wasmtimeCoverageAnalyzer.analyzeWasmtimeCoverage(
            testCase, mixedResults, behavioralResults, performanceResults);

    // Then: Should calculate different scores for different runtimes
    final WasmtimeCompatibilityScore compatibilityScore = result.getCompatibilityScore();
    assertTrue(
        compatibilityScore.getRuntimeScore(RuntimeType.JNI)
            > compatibilityScore.getRuntimeScore(RuntimeType.PANAMA));
    assertTrue(compatibilityScore.getFailedRuntimes().contains(RuntimeType.PANAMA));
    assertFalse(compatibilityScore.getFailedRuntimes().contains(RuntimeType.JNI));
  }

  @Test
  @DisplayName("Should identify coverage gaps for missing features")
  void shouldIdentifyCoverageGaps() {
    // Given: A test case with incomplete coverage
    final WasmTestCase testCase =
        createTestCase("simple_func", WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> partialResults =
        Map.of(
            RuntimeType.JNI, new BehavioralAnalyzer.TestExecutionResult(true, null, 100L, 200L)
            // Missing PANAMA runtime
            );
    final BehavioralAnalysisResult behavioralResults = createMockBehavioralResults();
    final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
        createMockPerformanceResults();

    // When: Analyzing coverage
    final WasmtimeCoverageAnalysisResult result =
        wasmtimeCoverageAnalyzer.analyzeWasmtimeCoverage(
            testCase, partialResults, behavioralResults, performanceResults);

    // Then: Should identify missing runtime as a gap
    final boolean hasRuntimeMissingGap =
        result.getWasmtimeGaps().stream()
            .anyMatch(
                gap ->
                    gap.getGapType() == WasmtimeGapType.RUNTIME_MISSING
                        && gap.getAffectedRuntimes().contains(RuntimeType.PANAMA));
    assertTrue(hasRuntimeMissingGap);
  }

  @Test
  @DisplayName("Should perform cross-implementation analysis")
  void shouldPerformCrossImplementationAnalysis() {
    // Given: A test case with consistent results across runtimes
    final WasmTestCase testCase =
        createTestCase("consistent_test", WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> consistentResults =
        createSuccessfulExecutionResults();
    final BehavioralAnalysisResult behavioralResults = createMockBehavioralResults();
    final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
        createMockPerformanceResults();

    // When: Analyzing coverage
    final WasmtimeCoverageAnalysisResult result =
        wasmtimeCoverageAnalyzer.analyzeWasmtimeCoverage(
            testCase, consistentResults, behavioralResults, performanceResults);

    // Then: Should show consistent features across implementations
    final CrossImplementationAnalysis crossAnalysis = result.getCrossImplementationAnalysis();
    assertNotNull(crossAnalysis);
    assertTrue(crossAnalysis.getCrossImplementationScore() > 0.0);
    assertFalse(crossAnalysis.getConsistentFeatures().isEmpty());
  }

  @Test
  @DisplayName("Should generate comprehensive Wasmtime coverage report")
  void shouldGenerateComprehensiveReport() {
    // Given: Some analyzed test cases
    analyzeMultipleTestCases();

    // When: Generating comprehensive report
    final WasmtimeComprehensiveCoverageReport report =
        wasmtimeCoverageAnalyzer.generateWasmtimeReport();

    // Then: Should provide comprehensive metrics
    assertNotNull(report);
    assertNotNull(report.getWasmtimeCategoryCompleteness());
    assertNotNull(report.getUncoveredWasmtimeFeatures());
    assertNotNull(report.getWasmtimeCompatibilityScores());
    assertNotNull(report.getWasmtimeRecommendations());
    assertNotNull(report.getTestSuiteCoverage());
    assertTrue(report.getTotalAnalyzedTests() > 0);
    assertNotNull(report.getGeneratedAt());
  }

  @Test
  @DisplayName("Should provide global coverage statistics")
  void shouldProvideGlobalCoverageStatistics() {
    // Given: Some analyzed test cases
    analyzeMultipleTestCases();

    // When: Getting global statistics
    final WasmtimeGlobalCoverageStatistics stats =
        wasmtimeCoverageAnalyzer.getWasmtimeGlobalStatistics();

    // Then: Should provide comprehensive statistics
    assertNotNull(stats);
    assertTrue(stats.getTotalWasmtimeFeatures() > 0);
    assertTrue(stats.getCoveredWasmtimeFeatures() >= 0);
    assertTrue(stats.getOverallCoveragePercentage() >= 0.0);
    assertTrue(stats.getOverallCoveragePercentage() <= 100.0);
    assertTrue(stats.getTotalAnalyzedTests() > 0);
    assertTrue(stats.getTotalCategories() > 0);
    assertTrue(stats.getCompatibilityScore() >= 0.0);
    assertTrue(stats.getCompatibilityScore() <= 100.0);
  }

  @Test
  @DisplayName("Should integrate with existing coverage analysis")
  void shouldIntegrateWithExistingCoverageAnalysis() {
    // Given: A test case for integration testing
    final WasmTestCase testCase =
        createTestCase("integration_test", WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
        createSuccessfulExecutionResults();

    // When: Using the integrator
    final WasmtimeCoverageAnalysisResult result =
        coverageIntegrator.analyzeTestCase(testCase, executionResults);

    // Then: Should provide enhanced analysis results
    assertNotNull(result);
    assertNotNull(result.getBaseCoverageResult()); // Base coverage analysis included
    assertNotNull(result.getWasmtimeFeatures()); // Wasmtime-specific features
    assertNotNull(result.getCompatibilityScore()); // Compatibility analysis
    assertNotNull(result.getWasmtimeMetrics()); // Enhanced metrics
  }

  @Test
  @DisplayName("Should validate coverage targets")
  void shouldValidateCoverageTargets() {
    // Given: Some analyzed test cases
    analyzeMultipleTestCases();

    // When: Validating coverage targets
    final WasmtimeCoverageValidationResult validation =
        coverageIntegrator.validateCoverageTargets();

    // Then: Should provide validation results
    assertNotNull(validation);
    assertTrue(validation.getActualCoveragePercentage() >= 0.0);
    assertTrue(validation.getActualCoveragePercentage() <= 100.0);
    assertTrue(validation.getActualCompatibilityScore() >= 0.0);
    assertTrue(validation.getActualCompatibilityScore() <= 100.0);
    assertNotNull(validation.getRecommendations());
    assertTrue(validation.getCoverageGap() >= 0.0);
    assertTrue(validation.getCompatibilityGap() >= 0.0);
  }

  @Test
  @DisplayName("Should clear coverage data properly")
  void shouldClearCoverageData() {
    // Given: Some analyzed test cases
    analyzeMultipleTestCases();

    // Verify data exists
    final WasmtimeGlobalCoverageStatistics statsBefore =
        wasmtimeCoverageAnalyzer.getWasmtimeGlobalStatistics();
    assertTrue(statsBefore.getTotalAnalyzedTests() > 0);

    // When: Clearing coverage data
    wasmtimeCoverageAnalyzer.clearWasmtimeCoverageData();

    // Then: Data should be cleared
    final WasmtimeGlobalCoverageStatistics statsAfter =
        wasmtimeCoverageAnalyzer.getWasmtimeGlobalStatistics();
    assertEquals(0, statsAfter.getTotalAnalyzedTests());
    assertEquals(0, statsAfter.getCoveredWasmtimeFeatures());
  }

  @Test
  @DisplayName("Should handle WASI test cases correctly")
  void shouldHandleWasiTestCases() {
    // Given: A WASI test case
    final WasmTestCase wasiTestCase =
        createTestCase("wasi_file_test", WasmTestSuiteLoader.TestSuiteType.WASI_TESTS);
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
        createSuccessfulExecutionResults();
    final BehavioralAnalysisResult behavioralResults = createMockBehavioralResults();
    final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
        createMockPerformanceResults();

    // When: Analyzing coverage
    final WasmtimeCoverageAnalysisResult result =
        wasmtimeCoverageAnalyzer.analyzeWasmtimeCoverage(
            wasiTestCase, executionResults, behavioralResults, performanceResults);

    // Then: Should detect WASI-specific features
    assertTrue(
        result.getWasmtimeFeatures().stream()
            .anyMatch(feature -> feature.contains("wasi") || feature.contains("file")));
    assertEquals(WasmTestSuiteLoader.TestSuiteType.WASI_TESTS, result.getSuiteType());
  }

  // Helper methods for test setup

  private WasmTestCase createTestCase(
      final String testName, final WasmTestSuiteLoader.TestSuiteType suiteType) {
    final Path testPath = Paths.get("test", testName + ".wasm");
    final byte[] moduleBytes = createMockWasmModule();
    return new WasmTestCase(
        testName, suiteType, testPath, moduleBytes, Optional.empty(), Optional.empty());
  }

  private byte[] createMockWasmModule() {
    // Create a minimal valid WASM module with magic number and version
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // Magic number
      0x01, 0x00, 0x00, 0x00 // Version
    };
  }

  private Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>
      createSuccessfulExecutionResults() {
    return Map.of(
        RuntimeType.JNI, new BehavioralAnalyzer.TestExecutionResult(true, null, 100L, 200L),
        RuntimeType.PANAMA, new BehavioralAnalyzer.TestExecutionResult(true, null, 100L, 190L));
  }

  private BehavioralAnalysisResult createMockBehavioralResults() {
    // Create a mock behavioral analysis result with reasonable consistency score
    return new BehavioralAnalysisResult("test", 0.95, BehavioralVerdict.CONSISTENT, null, null);
  }

  private PerformanceAnalyzer.PerformanceComparisonResult createMockPerformanceResults() {
    // Create a mock performance analysis result
    return new PerformanceAnalyzer.PerformanceComparisonResult("test", Map.of(), null, null, null);
  }

  private void analyzeMultipleTestCases() {
    final String[] testNames = {"func_test", "memory_test", "table_test", "global_test"};
    final WasmTestSuiteLoader.TestSuiteType[] suiteTypes = {
      WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS,
      WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC,
      WasmTestSuiteLoader.TestSuiteType.WASI_TESTS,
      WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS
    };

    for (int i = 0; i < testNames.length; i++) {
      final WasmTestCase testCase = createTestCase(testNames[i], suiteTypes[i]);
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          createSuccessfulExecutionResults();
      final BehavioralAnalysisResult behavioralResults = createMockBehavioralResults();
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults =
          createMockPerformanceResults();

      wasmtimeCoverageAnalyzer.analyzeWasmtimeCoverage(
          testCase, executionResults, behavioralResults, performanceResults);
    }
  }
}
