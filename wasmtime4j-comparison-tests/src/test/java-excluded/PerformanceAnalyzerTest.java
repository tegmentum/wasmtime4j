package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive test suite for PerformanceAnalyzer functionality. Tests statistical analysis,
 * comparison logic, and regression detection.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("PerformanceAnalyzer Tests")
class PerformanceAnalyzerTest {

  private PerformanceAnalyzer analyzer;
  private Instant baseTime;

  @BeforeEach
  void setUp() {
    analyzer = new PerformanceAnalyzer();
    baseTime = Instant.now();
  }

  @Test
  @DisplayName("Should handle empty results gracefully")
  void testEmptyResultsHandling() {
    final PerformanceAnalyzer.PerformanceComparisonResult result =
        analyzer.analyze(Collections.emptyList());

    assertNotNull(result);
    assertEquals("EMPTY_ANALYSIS", result.getTestName());
    assertTrue(result.getResults().isEmpty());
    assertTrue(result.getMetricsByRuntime().isEmpty());
    assertFalse(result.hasSignificantDifferences());
    assertFalse(result.hasRegressions());
  }

  @Test
  @DisplayName("Should analyze single runtime performance correctly")
  void testSingleRuntimeAnalysis() {
    final List<PerformanceAnalyzer.TestExecutionResult> results =
        createTestResults("test_single", "JNI", 5, 100.0, 10.0);

    final PerformanceAnalyzer.PerformanceComparisonResult result = analyzer.analyze(results);

    assertNotNull(result);
    assertEquals("test_single", result.getTestName());
    assertEquals(5, result.getResults().size());
    assertEquals(1, result.getMetricsByRuntime().size());
    assertTrue(result.getMetricsByRuntime().containsKey("JNI"));

    final PerformanceAnalyzer.PerformanceMetrics jniMetrics =
        result.getMetricsByRuntime().get("JNI");
    assertNotNull(jniMetrics);
    assertEquals(5, jniMetrics.getSampleSize());
    assertEquals(1.0, jniMetrics.getSuccessRate(), 0.01);
    assertTrue(jniMetrics.isStatisticallyReliable());
    assertTrue(jniMetrics.getMeanExecutionTimeMs() > 90.0);
    assertTrue(jniMetrics.getMeanExecutionTimeMs() < 110.0);
  }

  @Test
  @DisplayName("Should compare multiple runtimes and detect differences")
  void testMultipleRuntimeComparison() {
    final List<PerformanceAnalyzer.TestExecutionResult> jniResults =
        createTestResults("test_comparison", "JNI", 10, 100.0, 5.0);
    final List<PerformanceAnalyzer.TestExecutionResult> panamaResults =
        createTestResults("test_comparison", "PANAMA", 10, 150.0, 10.0);

    final List<PerformanceAnalyzer.TestExecutionResult> allResults =
        Arrays.asList(jniResults, panamaResults).stream()
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.toList());

    final PerformanceAnalyzer.PerformanceComparisonResult result = analyzer.analyze(allResults);

    assertNotNull(result);
    assertEquals("test_comparison", result.getTestName());
    assertEquals(20, result.getResults().size());
    assertEquals(2, result.getMetricsByRuntime().size());

    assertTrue(result.hasSignificantDifferences());
    assertFalse(result.getSignificantDifferences().isEmpty());

    // JNI should be faster
    assertEquals("JNI", result.getFastestRuntime());

    final PerformanceAnalyzer.OverheadAnalysis overhead = result.getOverheadAnalysis();
    assertNotNull(overhead);
    assertEquals("JNI", overhead.getBaselineRuntime());
    assertTrue(overhead.hasSignificantOverhead());
    assertTrue(overhead.getOverheadPercentage("PANAMA") > 40.0);
  }

  @Test
  @DisplayName("Should handle failed test results appropriately")
  void testFailedResultsHandling() {
    final List<PerformanceAnalyzer.TestExecutionResult> results =
        Arrays.asList(
            createSuccessfulResult("test_mixed", "JNI", 100.0),
            createSuccessfulResult("test_mixed", "JNI", 110.0),
            createFailedResult("test_mixed", "JNI", "Test failed"),
            createSuccessfulResult("test_mixed", "JNI", 105.0));

    final PerformanceAnalyzer.PerformanceComparisonResult result = analyzer.analyze(results);

    assertNotNull(result);
    assertEquals(4, result.getResults().size());

    final PerformanceAnalyzer.PerformanceMetrics metrics = result.getMetricsByRuntime().get("JNI");
    assertNotNull(metrics);
    assertEquals(4, metrics.getSampleSize());
    assertEquals(0.75, metrics.getSuccessRate(), 0.01); // 3 out of 4 successful
    assertTrue(metrics.getMeanExecutionTimeMs() > 100.0);
    assertTrue(metrics.getMeanExecutionTimeMs() < 110.0);
  }

  @Test
  @DisplayName("Should establish and use performance baselines")
  void testBaselineEstablishment() {
    final List<PerformanceAnalyzer.TestExecutionResult> initialResults =
        createTestResults("test_baseline", "JNI", 10, 100.0, 5.0);

    final PerformanceAnalyzer.PerformanceComparisonResult initialResult =
        analyzer.analyze(initialResults);

    // Establish baseline
    analyzer.establishBaseline("test_baseline", initialResult.getMetricsByRuntime());

    // Verify baseline was established
    final Map<String, PerformanceAnalyzer.PerformanceBaseline> baselines = analyzer.getBaselines();
    assertTrue(baselines.containsKey("test_baseline_JNI"));

    final PerformanceAnalyzer.PerformanceBaseline baseline = baselines.get("test_baseline_JNI");
    assertNotNull(baseline);
    assertEquals("test_baseline", baseline.getTestName());
    assertEquals("JNI", baseline.getRuntimeType());
    assertTrue(baseline.getBaselineExecutionTimeMs() > 95.0);
    assertTrue(baseline.getBaselineExecutionTimeMs() < 105.0);

    // Test regression detection
    final List<PerformanceAnalyzer.TestExecutionResult> regressionResults =
        createTestResults("test_baseline", "JNI", 5, 150.0, 5.0); // 50% slower

    final PerformanceAnalyzer.PerformanceComparisonResult regressionResult =
        analyzer.analyze(regressionResults);
    assertTrue(regressionResult.hasRegressions());
    assertFalse(regressionResult.getRegressionWarnings().isEmpty());
  }

  @Test
  @DisplayName("Should calculate accurate performance metrics")
  void testPerformanceMetricsCalculation() {
    // Create results with known values for verification
    final List<PerformanceAnalyzer.TestExecutionResult> results =
        Arrays.asList(
            createSuccessfulResult("test_metrics", "JNI", 100.0),
            createSuccessfulResult("test_metrics", "JNI", 105.0),
            createSuccessfulResult("test_metrics", "JNI", 95.0),
            createSuccessfulResult("test_metrics", "JNI", 110.0),
            createSuccessfulResult("test_metrics", "JNI", 90.0));

    final PerformanceAnalyzer.PerformanceComparisonResult result = analyzer.analyze(results);
    final PerformanceAnalyzer.PerformanceMetrics metrics = result.getMetricsByRuntime().get("JNI");

    assertNotNull(metrics);
    assertEquals(5, metrics.getSampleSize());
    assertEquals(1.0, metrics.getSuccessRate(), 0.01);
    assertEquals(100.0, metrics.getMeanExecutionTimeMs(), 0.1); // (100+105+95+110+90)/5 = 100
    assertEquals(100.0, metrics.getMedianExecutionTimeMs(), 0.1); // Sorted: 90,95,100,105,110
    assertTrue(metrics.getStandardDeviation() > 0);
    assertTrue(metrics.getCoefficientOfVariation() > 0);
    assertEquals(110.0, metrics.getPercentile95(), 0.1);
    assertEquals(110.0, metrics.getPercentile99(), 0.1);
  }

  @Test
  @DisplayName("Should detect memory usage patterns correctly")
  void testMemoryUsageAnalysis() {
    final List<PerformanceAnalyzer.TestExecutionResult> results =
        Arrays.asList(
            createResultWithMemory("test_memory", "JNI", 100.0, 1024 * 1024, 2 * 1024 * 1024),
            createResultWithMemory("test_memory", "JNI", 105.0, 1024 * 1024, 2 * 1024 * 1024),
            createResultWithMemory(
                "test_memory", "PANAMA", 110.0, 2 * 1024 * 1024, 4 * 1024 * 1024));

    final PerformanceAnalyzer.PerformanceComparisonResult result = analyzer.analyze(results);

    assertEquals("JNI", result.getMostMemoryEfficientRuntime());

    final PerformanceAnalyzer.PerformanceMetrics jniMetrics =
        result.getMetricsByRuntime().get("JNI");
    final PerformanceAnalyzer.PerformanceMetrics panamaMetrics =
        result.getMetricsByRuntime().get("PANAMA");

    assertTrue(jniMetrics.getMeanMemoryUsage() < panamaMetrics.getMeanMemoryUsage());
    assertEquals(2 * 1024 * 1024, jniMetrics.getPeakMemoryUsage());
    assertEquals(4 * 1024 * 1024, panamaMetrics.getPeakMemoryUsage());
  }

  @Test
  @DisplayName("Should handle statistical edge cases")
  void testStatisticalEdgeCases() {
    // Test with insufficient data
    final List<PerformanceAnalyzer.TestExecutionResult> smallDataset =
        createTestResults("test_small", "JNI", 2, 100.0, 0.0);

    final PerformanceAnalyzer.PerformanceComparisonResult smallResult =
        analyzer.analyze(smallDataset);
    final PerformanceAnalyzer.PerformanceMetrics smallMetrics =
        smallResult.getMetricsByRuntime().get("JNI");

    assertFalse(smallMetrics.isStatisticallyReliable());

    // Test with high variability
    final List<PerformanceAnalyzer.TestExecutionResult> highVariabilityResults =
        Arrays.asList(
            createSuccessfulResult("test_variable", "JNI", 50.0),
            createSuccessfulResult("test_variable", "JNI", 200.0),
            createSuccessfulResult("test_variable", "JNI", 75.0),
            createSuccessfulResult("test_variable", "JNI", 180.0),
            createSuccessfulResult("test_variable", "JNI", 100.0));

    final PerformanceAnalyzer.PerformanceComparisonResult variableResult =
        analyzer.analyze(highVariabilityResults);
    final PerformanceAnalyzer.PerformanceMetrics variableMetrics =
        variableResult.getMetricsByRuntime().get("JNI");

    assertTrue(variableMetrics.getCoefficientOfVariation() > 30.0);
    assertFalse(variableMetrics.isStatisticallyReliable());
  }

  @Test
  @DisplayName("Should validate test execution result builder")
  void testTestExecutionResultBuilder() {
    final PerformanceAnalyzer.TestExecutionResult result =
        PerformanceAnalyzer.TestExecutionResult.builder("test_builder", "JNI")
            .executionTime(baseTime)
            .executionDuration(Duration.ofMillis(100))
            .memoryUsed(1024 * 1024)
            .peakMemoryUsage(2 * 1024 * 1024)
            .successful(true)
            .additionalMetric("custom_metric", "custom_value")
            .build();

    assertNotNull(result);
    assertEquals("test_builder", result.getTestName());
    assertEquals("JNI", result.getRuntimeType());
    assertEquals(baseTime, result.getExecutionTime());
    assertEquals(Duration.ofMillis(100), result.getExecutionDuration());
    assertEquals(1024 * 1024, result.getMemoryUsed());
    assertEquals(2 * 1024 * 1024, result.getPeakMemoryUsage());
    assertTrue(result.isSuccessful());
    assertNull(result.getErrorMessage());
    assertEquals("custom_value", result.getAdditionalMetrics().get("custom_metric"));
  }

  @Test
  @DisplayName("Should handle null inputs gracefully")
  void testNullInputHandling() {
    assertThrows(NullPointerException.class, () -> analyzer.analyze(null));

    assertThrows(
        NullPointerException.class,
        () -> PerformanceAnalyzer.TestExecutionResult.builder(null, "JNI"));

    assertThrows(
        NullPointerException.class,
        () -> PerformanceAnalyzer.TestExecutionResult.builder("test", null));
  }

  @Test
  @DisplayName("Should clear baselines correctly")
  void testBaselineClearance() {
    final List<PerformanceAnalyzer.TestExecutionResult> results =
        createTestResults("test_clear", "JNI", 5, 100.0, 5.0);

    final PerformanceAnalyzer.PerformanceComparisonResult result = analyzer.analyze(results);
    analyzer.establishBaseline("test_clear", result.getMetricsByRuntime());

    assertFalse(analyzer.getBaselines().isEmpty());

    analyzer.clearBaselines();
    assertTrue(analyzer.getBaselines().isEmpty());
  }

  // Helper methods for creating test data

  private List<PerformanceAnalyzer.TestExecutionResult> createTestResults(
      final String testName,
      final String runtimeType,
      final int count,
      final double meanTimeMs,
      final double stdDevMs) {
    final List<PerformanceAnalyzer.TestExecutionResult> results = new java.util.ArrayList<>();
    final java.util.Random random = new java.util.Random(42); // Fixed seed for reproducibility

    for (int i = 0; i < count; i++) {
      final double executionTime = meanTimeMs + (random.nextGaussian() * stdDevMs);
      results.add(createSuccessfulResult(testName, runtimeType, Math.max(1.0, executionTime)));
    }

    return results;
  }

  private PerformanceAnalyzer.TestExecutionResult createSuccessfulResult(
      final String testName, final String runtimeType, final double executionTimeMs) {
    return PerformanceAnalyzer.TestExecutionResult.builder(testName, runtimeType)
        .executionTime(baseTime.plusMillis((long) executionTimeMs))
        .executionDuration(Duration.ofNanos((long) (executionTimeMs * 1_000_000)))
        .memoryUsed(1024 * 1024) // 1MB default
        .peakMemoryUsage(2 * 1024 * 1024) // 2MB default
        .successful(true)
        .build();
  }

  private PerformanceAnalyzer.TestExecutionResult createFailedResult(
      final String testName, final String runtimeType, final String errorMessage) {
    return PerformanceAnalyzer.TestExecutionResult.builder(testName, runtimeType)
        .executionTime(baseTime)
        .executionDuration(Duration.ZERO)
        .successful(false)
        .errorMessage(errorMessage)
        .build();
  }

  private PerformanceAnalyzer.TestExecutionResult createResultWithMemory(
      final String testName,
      final String runtimeType,
      final double executionTimeMs,
      final long memoryUsed,
      final long peakMemory) {
    return PerformanceAnalyzer.TestExecutionResult.builder(testName, runtimeType)
        .executionTime(baseTime)
        .executionDuration(Duration.ofNanos((long) (executionTimeMs * 1_000_000)))
        .memoryUsed(memoryUsed)
        .peakMemoryUsage(peakMemory)
        .successful(true)
        .build();
  }
}
