package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive test suite for TrendAnalyzer functionality. Tests trend detection, statistical
 * analysis, and baseline management.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("TrendAnalyzer Tests")
class TrendAnalyzerTest {

  private TrendAnalyzer trendAnalyzer;
  private Instant baseTime;

  @BeforeEach
  void setUp() {
    trendAnalyzer = new TrendAnalyzer();
    baseTime = Instant.now();
  }

  @Test
  @DisplayName("Should record and retrieve basic metrics")
  void testBasicMetricRecording() {
    trendAnalyzer.recordMetric("test_basic", "JNI", "execution_time", 100.0);
    trendAnalyzer.recordMetric("test_basic", "JNI", "execution_time", 105.0);
    trendAnalyzer.recordMetric("test_basic", "JNI", "memory_usage", 1024.0);

    final Map<String, java.util.List<TrendAnalyzer.TrendDataPoint>> history =
        trendAnalyzer.getTrendHistory();

    assertTrue(history.containsKey("test_basic_JNI_execution_time"));
    assertTrue(history.containsKey("test_basic_JNI_memory_usage"));

    assertEquals(2, history.get("test_basic_JNI_execution_time").size());
    assertEquals(1, history.get("test_basic_JNI_memory_usage").size());

    final TrendAnalyzer.TrendDataPoint firstPoint =
        history.get("test_basic_JNI_execution_time").get(0);
    assertEquals("test_basic", firstPoint.getContext().split("_")[0]);
    assertEquals("execution_time", firstPoint.getMetric());
    assertEquals(100.0, firstPoint.getValue(), 0.01);
  }

  @Test
  @DisplayName("Should detect increasing trend correctly")
  void testIncreasingTrendDetection() {
    // Record data points with increasing trend
    final String testName = "test_increasing";
    final String runtime = "JNI";
    final String metric = "execution_time";

    for (int i = 0; i < 10; i++) {
      trendAnalyzer.recordMetric(
          testName, runtime, metric, 100.0 + (i * 5)); // Increasing by 5ms each time
    }

    final Map<String, TrendAnalyzer.TrendAnalysisResult> results = trendAnalyzer.analyzeTrends();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(results.containsKey(trendKey));

    final TrendAnalyzer.TrendAnalysisResult result = results.get(trendKey);
    assertNotNull(result);
    assertEquals(TrendAnalyzer.TrendDirection.INCREASING, result.getDirection());
    assertTrue(result.getTrendSlope() > 0);
    assertTrue(result.isStatisticallySignificant());

    final String description = result.getTrendDescription();
    assertTrue(description.contains("increasing"));
  }

  @Test
  @DisplayName("Should detect decreasing trend correctly")
  void testDecreasingTrendDetection() {
    // Record data points with decreasing trend (performance improvement)
    final String testName = "test_decreasing";
    final String runtime = "PANAMA";
    final String metric = "execution_time";

    for (int i = 0; i < 8; i++) {
      trendAnalyzer.recordMetric(
          testName, runtime, metric, 150.0 - (i * 3)); // Decreasing by 3ms each time
    }

    final Map<String, TrendAnalyzer.TrendAnalysisResult> results = trendAnalyzer.analyzeTrends();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(results.containsKey(trendKey));

    final TrendAnalyzer.TrendAnalysisResult result = results.get(trendKey);
    assertNotNull(result);
    assertEquals(TrendAnalyzer.TrendDirection.DECREASING, result.getDirection());
    assertTrue(result.getTrendSlope() < 0);
    assertTrue(result.isStatisticallySignificant());
    assertTrue(result.isImprovement());

    final String description = result.getTrendDescription();
    assertTrue(description.contains("improvement"));
  }

  @Test
  @DisplayName("Should detect stable trend correctly")
  void testStableTrendDetection() {
    // Record data points with minimal variation
    final String testName = "test_stable";
    final String runtime = "JNI";
    final String metric = "execution_time";

    final java.util.Random random = new java.util.Random(42); // Fixed seed
    for (int i = 0; i < 6; i++) {
      trendAnalyzer.recordMetric(
          testName, runtime, metric, 100.0 + (random.nextGaussian() * 1.0)); // Small variation
    }

    final Map<String, TrendAnalyzer.TrendAnalysisResult> results = trendAnalyzer.analyzeTrends();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(results.containsKey(trendKey));

    final TrendAnalyzer.TrendAnalysisResult result = results.get(trendKey);
    assertNotNull(result);
    assertEquals(TrendAnalyzer.TrendDirection.STABLE, result.getDirection());
    assertEquals(TrendAnalyzer.TrendType.STABLE, result.getTrendType());

    final String description = result.getTrendDescription();
    assertTrue(description.contains("stable") || description.contains("No significant trend"));
  }

  @Test
  @DisplayName("Should detect performance regression")
  void testPerformanceRegressionDetection() {
    // Record data points showing regression (performance getting worse)
    final String testName = "test_regression";
    final String runtime = "JNI";
    final String metric = "execution_time";

    for (int i = 0; i < 7; i++) {
      trendAnalyzer.recordMetric(
          testName, runtime, metric, 100.0 + (i * 8)); // Increasing execution time = regression
    }

    final Map<String, TrendAnalyzer.TrendAnalysisResult> results = trendAnalyzer.analyzeTrends();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(results.containsKey(trendKey));

    final TrendAnalyzer.TrendAnalysisResult result = results.get(trendKey);
    assertNotNull(result);
    assertTrue(result.isRegression());
    assertEquals(TrendAnalyzer.TrendType.REGRESSION, result.getTrendType());

    final String description = result.getTrendDescription();
    assertTrue(description.contains("regression"));
  }

  @Test
  @DisplayName("Should establish and use baselines correctly")
  void testBaselineEstablishment() {
    final String testName = "test_baseline";
    final String runtime = "PANAMA";
    final String metric = "execution_time";

    // Record baseline data
    for (int i = 0; i < 6; i++) {
      trendAnalyzer.recordMetric(
          testName, runtime, metric, 120.0 + (i % 2)); // Values around 120-121
    }

    // Establish baseline
    trendAnalyzer.establishBaseline(testName, runtime, metric, 0.05); // 5% tolerance

    final Map<String, TrendAnalyzer.TrendBaseline> baselines = trendAnalyzer.getBaselines();
    final String baselineKey = testName + "_" + runtime + "_" + metric;

    assertTrue(baselines.containsKey(baselineKey));

    final TrendAnalyzer.TrendBaseline baseline = baselines.get(baselineKey);
    assertNotNull(baseline);
    assertEquals(testName + "_" + runtime + "_" + metric, baseline.getKey());
    assertTrue(baseline.getBaselineValue() >= 120.0);
    assertTrue(baseline.getBaselineValue() <= 121.0);
    assertEquals(0.05, baseline.getTolerance(), 0.001);

    // Test baseline drift detection
    assertFalse(baseline.isDrift(120.5)); // Within tolerance
    assertTrue(baseline.isDrift(130.0)); // Beyond tolerance

    final double driftPercentage = baseline.getDriftPercentage(130.0);
    assertTrue(driftPercentage > 0.07); // More than 7% increase
  }

  @Test
  @DisplayName("Should detect baseline drift across all metrics")
  void testBaselineDriftDetection() {
    final String testName = "test_drift";
    final String runtime = "JNI";
    final String metric = "execution_time";

    // Establish baseline
    for (int i = 0; i < 6; i++) {
      trendAnalyzer.recordMetric(testName, runtime, metric, 100.0);
    }
    trendAnalyzer.establishBaseline(testName, runtime, metric, 0.05);

    // Add data point that drifts from baseline
    trendAnalyzer.recordMetric(testName, runtime, metric, 112.0); // 12% increase

    final Map<String, Double> driftResults = trendAnalyzer.detectBaselineDrift();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(driftResults.containsKey(trendKey));
    final double driftPercentage = driftResults.get(trendKey);
    assertTrue(driftPercentage > 0.10); // More than 10% drift
  }

  @Test
  @DisplayName("Should analyze trends for specific test and runtime")
  void testSpecificTrendAnalysis() {
    // Record data for multiple tests and runtimes
    recordTrendData("test1", "JNI", "execution_time", new double[] {100, 105, 110, 115, 120});
    recordTrendData("test1", "PANAMA", "execution_time", new double[] {120, 125, 130, 135, 140});
    recordTrendData("test2", "JNI", "execution_time", new double[] {80, 82, 84, 86, 88});

    // Analyze trends for specific test and runtime
    final Map<String, TrendAnalyzer.TrendAnalysisResult> test1JniResults =
        trendAnalyzer.analyzeTrends("test1", "JNI");

    assertEquals(1, test1JniResults.size());
    assertTrue(test1JniResults.containsKey("test1_JNI_execution_time"));

    final TrendAnalyzer.TrendAnalysisResult result =
        test1JniResults.get("test1_JNI_execution_time");
    assertEquals(TrendAnalyzer.TrendDirection.INCREASING, result.getDirection());
    assertTrue(result.isStatisticallySignificant());
  }

  @Test
  @DisplayName("Should handle insufficient data gracefully")
  void testInsufficientDataHandling() {
    // Record too few data points
    trendAnalyzer.recordMetric("test_insufficient", "JNI", "execution_time", 100.0);
    trendAnalyzer.recordMetric("test_insufficient", "JNI", "execution_time", 105.0);

    final Map<String, TrendAnalyzer.TrendAnalysisResult> results = trendAnalyzer.analyzeTrends();

    // Should not analyze trends with insufficient data
    assertFalse(results.containsKey("test_insufficient_JNI_execution_time"));

    // Should warn about insufficient data for baseline
    trendAnalyzer.establishBaseline("test_insufficient", "JNI", "execution_time", 0.05);
    assertTrue(trendAnalyzer.getBaselines().isEmpty());
  }

  @Test
  @DisplayName("Should detect anomalies in trend data")
  void testAnomalyDetection() {
    final String testName = "test_anomaly";
    final String runtime = "JNI";
    final String metric = "execution_time";

    // Record normal trend with one anomaly
    recordTrendData(
        testName, runtime, metric, new double[] {100, 105, 110, 500, 115, 120}); // 500 is anomaly

    final Map<String, TrendAnalyzer.TrendAnalysisResult> results = trendAnalyzer.analyzeTrends();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(results.containsKey(trendKey));

    final TrendAnalyzer.TrendAnalysisResult result = results.get(trendKey);
    assertNotNull(result);
    assertFalse(result.getAnomalies().isEmpty());

    final String anomaly = result.getAnomalies().get(0);
    assertTrue(anomaly.contains("500"));
  }

  @Test
  @DisplayName("Should clear history and baselines correctly")
  void testDataClearance() {
    // Add some data
    recordTrendData("test_clear", "JNI", "execution_time", new double[] {100, 105, 110, 115, 120});
    trendAnalyzer.establishBaseline("test_clear", "JNI", "execution_time", 0.05);

    assertFalse(trendAnalyzer.getTrendHistory().isEmpty());
    assertFalse(trendAnalyzer.getBaselines().isEmpty());

    // Clear all data
    trendAnalyzer.clearHistory();

    assertTrue(trendAnalyzer.getTrendHistory().isEmpty());
    assertTrue(trendAnalyzer.getBaselines().isEmpty());
  }

  @Test
  @DisplayName("Should calculate correlation coefficient correctly")
  void testCorrelationCalculation() {
    final String testName = "test_correlation";
    final String runtime = "JNI";
    final String metric = "execution_time";

    // Perfect positive correlation
    recordTrendData(testName, runtime, metric, new double[] {100, 110, 120, 130, 140, 150});

    final Map<String, TrendAnalyzer.TrendAnalysisResult> results = trendAnalyzer.analyzeTrends();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(results.containsKey(trendKey));

    final TrendAnalyzer.TrendAnalysisResult result = results.get(trendKey);
    assertNotNull(result);

    // Should have strong positive correlation
    assertTrue(result.getCorrelationCoefficient() > 0.8);
    assertTrue(result.isStatisticallySignificant());
  }

  @Test
  @DisplayName("Should handle edge cases in statistical calculations")
  void testStatisticalEdgeCases() {
    final String testName = "test_edge";
    final String runtime = "JNI";
    final String metric = "execution_time";

    // All identical values (no variation)
    recordTrendData(testName, runtime, metric, new double[] {100, 100, 100, 100, 100});

    final Map<String, TrendAnalyzer.TrendAnalysisResult> results = trendAnalyzer.analyzeTrends();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(results.containsKey(trendKey));

    final TrendAnalyzer.TrendAnalysisResult result = results.get(trendKey);
    assertNotNull(result);
    assertEquals(TrendAnalyzer.TrendDirection.STABLE, result.getDirection());
    assertEquals(0.0, result.getTrendSlope(), 0.001);
  }

  @Test
  @DisplayName("Should limit trend history size")
  void testHistorySizeLimit() {
    final String testName = "test_limit";
    final String runtime = "JNI";
    final String metric = "execution_time";

    // Record more than 1000 data points
    for (int i = 0; i < 1200; i++) {
      trendAnalyzer.recordMetric(testName, runtime, metric, 100.0 + (i % 10));
    }

    final Map<String, java.util.List<TrendAnalyzer.TrendDataPoint>> history =
        trendAnalyzer.getTrendHistory();
    final String trendKey = testName + "_" + runtime + "_" + metric;

    assertTrue(history.containsKey(trendKey));

    // Should be limited to 1000 data points
    assertEquals(1000, history.get(trendKey).size());
  }

  @Test
  @DisplayName("Should handle null inputs gracefully")
  void testNullInputHandling() {
    assertThrows(
        NullPointerException.class, () -> trendAnalyzer.recordMetric(null, "JNI", "metric", 100.0));

    assertThrows(
        NullPointerException.class,
        () -> trendAnalyzer.recordMetric("test", null, "metric", 100.0));

    assertThrows(
        NullPointerException.class, () -> trendAnalyzer.recordMetric("test", "JNI", null, 100.0));

    assertThrows(
        NullPointerException.class,
        () -> TrendAnalyzer.TrendDataPoint.create(null, 100.0, "metric", "context"));

    assertThrows(
        NullPointerException.class,
        () -> TrendAnalyzer.TrendDataPoint.create(Instant.now(), 100.0, null, "context"));

    assertThrows(
        NullPointerException.class,
        () -> TrendAnalyzer.TrendDataPoint.create(Instant.now(), 100.0, "metric", null));
  }

  @Test
  @DisplayName("Should validate TrendDataPoint creation")
  void testTrendDataPointCreation() {
    final Instant timestamp = Instant.now();
    final Map<String, Object> metadata = Map.of("key1", "value1", "key2", 42);

    final TrendAnalyzer.TrendDataPoint dataPoint =
        TrendAnalyzer.TrendDataPoint.create(
            timestamp, 123.45, "test_metric", "test_context", metadata);

    assertNotNull(dataPoint);
    assertEquals(timestamp, dataPoint.getTimestamp());
    assertEquals(123.45, dataPoint.getValue(), 0.001);
    assertEquals("test_metric", dataPoint.getMetric());
    assertEquals("test_context", dataPoint.getContext());
    assertEquals(metadata, dataPoint.getMetadata());

    // Test simple creation without metadata
    final TrendAnalyzer.TrendDataPoint simpleDataPoint =
        TrendAnalyzer.TrendDataPoint.create(timestamp, 67.89, "simple_metric", "simple_context");

    assertNotNull(simpleDataPoint);
    assertTrue(simpleDataPoint.getMetadata().isEmpty());
  }

  // Helper methods for creating test data

  private void recordTrendData(
      final String testName, final String runtime, final String metric, final double[] values) {
    for (final double value : values) {
      trendAnalyzer.recordMetric(testName, runtime, metric, value);
    }
  }
}
