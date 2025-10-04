package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive test suite for MetricsCollector functionality. Tests data collection, thread
 * safety, and statistical analysis.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("MetricsCollector Tests")
class MetricsCollectorTest {

  private MetricsCollector metricsCollector;

  @BeforeEach
  void setUp() {
    metricsCollector = new MetricsCollector();
  }

  @Test
  @DisplayName("Should collect basic performance data points")
  void testBasicDataCollection() {
    final MetricsCollector.PerformanceDataPoint dataPoint =
        MetricsCollector.PerformanceDataPoint.builder("test_basic", "JNI")
            .executionTime(Duration.ofMillis(100))
            .memoryUsed(1024 * 1024)
            .peakMemoryUsage(2 * 1024 * 1024)
            .successful(true)
            .build();

    metricsCollector.recordDataPoint(dataPoint);

    final List<MetricsCollector.PerformanceDataPoint> dataPoints = metricsCollector.getDataPoints();
    assertEquals(1, dataPoints.size());
    assertEquals("test_basic", dataPoints.get(0).getTestName());
    assertEquals("JNI", dataPoints.get(0).getRuntimeType());
    assertEquals(100.0, dataPoints.get(0).getExecutionTimeMs(), 0.1);
    assertEquals(1.0, dataPoints.get(0).getMemoryUsageMb(), 0.1);
  }

  @Test
  @DisplayName("Should handle measurement sessions correctly")
  void testMeasurementSession() throws InterruptedException {
    final MetricsCollector.MeasurementSession session =
        metricsCollector.startMeasurement("test_session", "JNI");

    assertNotNull(session);

    // Simulate some work
    Thread.sleep(50);
    session.recordCustomMetric("iterations", 100);

    session.recordSuccess();

    final List<MetricsCollector.PerformanceDataPoint> dataPoints = metricsCollector.getDataPoints();
    assertEquals(1, dataPoints.size());

    final MetricsCollector.PerformanceDataPoint dataPoint = dataPoints.get(0);
    assertEquals("test_session", dataPoint.getTestName());
    assertEquals("JNI", dataPoint.getRuntimeType());
    assertTrue(dataPoint.isSuccessful());
    assertTrue(dataPoint.getExecutionTime().toMillis() >= 50);

    assertEquals(1, metricsCollector.getSuccessfulOperations());
    assertEquals(0, metricsCollector.getFailedOperations());
    assertEquals(1, metricsCollector.getTotalOperations());
  }

  @Test
  @DisplayName("Should handle failed measurement sessions")
  void testFailedMeasurementSession() {
    final MetricsCollector.MeasurementSession session =
        metricsCollector.startMeasurement("test_failed", "PANAMA");

    session.recordFailure("Test execution failed");

    final List<MetricsCollector.PerformanceDataPoint> dataPoints = metricsCollector.getDataPoints();
    assertEquals(1, dataPoints.size());

    final MetricsCollector.PerformanceDataPoint dataPoint = dataPoints.get(0);
    assertEquals("test_failed", dataPoint.getTestName());
    assertEquals("PANAMA", dataPoint.getRuntimeType());
    assertFalse(dataPoint.isSuccessful());
    assertEquals("Test execution failed", dataPoint.getErrorMessage());

    assertEquals(0, metricsCollector.getSuccessfulOperations());
    assertEquals(1, metricsCollector.getFailedOperations());
    assertEquals(1, metricsCollector.getTotalOperations());
  }

  @Test
  @DisplayName("Should collect and retrieve custom metrics")
  void testCustomMetricsCollection() {
    metricsCollector.recordCustomMetric("throughput", 1000.0);
    metricsCollector.recordCustomMetric("throughput", 1050.0);
    metricsCollector.recordCustomMetric("latency", 25.5);

    final Map<String, List<Object>> customMetrics = metricsCollector.getCustomMetrics();

    assertTrue(customMetrics.containsKey("throughput"));
    assertTrue(customMetrics.containsKey("latency"));

    final List<Object> throughputValues = customMetrics.get("throughput");
    assertEquals(2, throughputValues.size());
    assertEquals(1000.0, throughputValues.get(0));
    assertEquals(1050.0, throughputValues.get(1));

    final List<Object> latencyValues = customMetrics.get("latency");
    assertEquals(1, latencyValues.size());
    assertEquals(25.5, latencyValues.get(0));
  }

  @Test
  @DisplayName("Should filter data points by test and runtime")
  void testDataPointFiltering() {
    // Create data points for different tests and runtimes
    final MetricsCollector.PerformanceDataPoint jniPoint1 =
        createDataPoint("test1", "JNI", 100.0, true);
    final MetricsCollector.PerformanceDataPoint jniPoint2 =
        createDataPoint("test1", "JNI", 105.0, true);
    final MetricsCollector.PerformanceDataPoint panamaPoint =
        createDataPoint("test1", "PANAMA", 110.0, true);
    final MetricsCollector.PerformanceDataPoint otherTestPoint =
        createDataPoint("test2", "JNI", 95.0, true);

    metricsCollector.recordDataPoint(jniPoint1);
    metricsCollector.recordDataPoint(jniPoint2);
    metricsCollector.recordDataPoint(panamaPoint);
    metricsCollector.recordDataPoint(otherTestPoint);

    // Test filtering
    final List<MetricsCollector.PerformanceDataPoint> test1JniPoints =
        metricsCollector.getDataPoints("test1", "JNI");
    assertEquals(2, test1JniPoints.size());

    final List<MetricsCollector.PerformanceDataPoint> test1PanamaPoints =
        metricsCollector.getDataPoints("test1", "PANAMA");
    assertEquals(1, test1PanamaPoints.size());

    final List<MetricsCollector.PerformanceDataPoint> test2Points =
        metricsCollector.getDataPoints("test2", "JNI");
    assertEquals(1, test2Points.size());

    final List<MetricsCollector.PerformanceDataPoint> nonExistentPoints =
        metricsCollector.getDataPoints("nonexistent", "JNI");
    assertTrue(nonExistentPoints.isEmpty());
  }

  @Test
  @DisplayName("Should generate accurate metrics summary")
  void testMetricsSummaryGeneration() {
    // Add known data points for testing
    recordMultipleDataPoints(
        "test_summary",
        "JNI",
        new double[] {100.0, 105.0, 95.0, 110.0, 90.0}, // Mean: 100, StdDev: ~7.07
        new boolean[] {true, true, true, true, true});

    recordMultipleDataPoints(
        "test_summary",
        "JNI",
        new double[] {0.0}, // One failed test
        new boolean[] {false});

    final MetricsCollector.MetricsSummary summary = metricsCollector.generateSummary();

    assertNotNull(summary);
    assertEquals(6, summary.getTotalDataPoints());
    assertEquals(5, summary.getSuccessfulOperations());
    assertEquals(1, summary.getFailedOperations());
    assertEquals(5.0 / 6.0, summary.getSuccessRate(), 0.01);

    // Verify statistical calculations
    assertEquals(100.0, summary.getMeanExecutionTimeMs(), 0.1);
    assertEquals(100.0, summary.getMedianExecutionTimeMs(), 0.1); // Median of [90,95,100,105,110]
    assertTrue(summary.getStandardDeviation() > 5.0);
    assertTrue(summary.getStandardDeviation() < 10.0);
    assertEquals(90.0, summary.getMinExecutionTimeMs(), 0.1);
    assertEquals(110.0, summary.getMaxExecutionTimeMs(), 0.1);

    assertTrue(summary.isStatisticallyReliable());
  }

  @Test
  @DisplayName("Should detect outliers when enabled")
  void testOutlierDetection() {
    final MetricsCollector outlierDetectingCollector = new MetricsCollector(true);

    // Add normal data points
    for (int i = 0; i < 15; i++) {
      final MetricsCollector.PerformanceDataPoint normalPoint =
          createDataPoint("test_outlier", "JNI", 100.0 + (i % 5), true);
      outlierDetectingCollector.recordDataPoint(normalPoint);
    }

    // Add an outlier (should be filtered out)
    final MetricsCollector.PerformanceDataPoint outlierPoint =
        createDataPoint("test_outlier", "JNI", 1000.0, true); // 10x normal value
    outlierDetectingCollector.recordDataPoint(outlierPoint);

    final List<MetricsCollector.PerformanceDataPoint> dataPoints =
        outlierDetectingCollector.getDataPoints();

    // Outlier should be filtered out, so we should have only 15 points
    assertEquals(15, dataPoints.size());

    // Verify no extreme values are present
    final double maxExecutionTime =
        dataPoints.stream()
            .mapToDouble(MetricsCollector.PerformanceDataPoint::getExecutionTimeMs)
            .max()
            .orElse(0.0);

    assertTrue(maxExecutionTime < 200.0); // Should not include the 1000ms outlier
  }

  @Test
  @DisplayName("Should handle concurrent data collection safely")
  void testConcurrentDataCollection() throws InterruptedException {
    final int threadCount = 10;
    final int operationsPerThread = 100;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      executor.submit(
          () -> {
            try {
              for (int i = 0; i < operationsPerThread; i++) {
                final MetricsCollector.MeasurementSession session =
                    metricsCollector.startMeasurement("test_concurrent", "JNI_" + threadId);

                // Simulate some work
                try {
                  Thread.sleep(1);
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return;
                }

                session.recordCustomMetric("thread_id", threadId);
                session.recordSuccess();
              }
            } finally {
              latch.countDown();
            }
          });
    }

    assertTrue(latch.await(30, TimeUnit.SECONDS));
    executor.shutdown();

    final List<MetricsCollector.PerformanceDataPoint> dataPoints = metricsCollector.getDataPoints();
    assertEquals(threadCount * operationsPerThread, dataPoints.size());
    assertEquals(threadCount * operationsPerThread, metricsCollector.getSuccessfulOperations());
    assertEquals(0, metricsCollector.getFailedOperations());

    // Verify data integrity
    final Map<String, List<Object>> customMetrics = metricsCollector.getCustomMetrics();
    assertTrue(customMetrics.containsKey("thread_id"));
    assertEquals(threadCount * operationsPerThread, customMetrics.get("thread_id").size());
  }

  @Test
  @DisplayName("Should clear all data correctly")
  void testDataClearance() {
    // Add some data
    recordMultipleDataPoints(
        "test_clear", "JNI", new double[] {100.0, 105.0}, new boolean[] {true, true});
    metricsCollector.recordCustomMetric("test_metric", 42);

    assertFalse(metricsCollector.getDataPoints().isEmpty());
    assertFalse(metricsCollector.getCustomMetrics().isEmpty());
    assertEquals(2, metricsCollector.getTotalOperations());

    // Clear all data
    metricsCollector.clear();

    assertTrue(metricsCollector.getDataPoints().isEmpty());
    assertTrue(metricsCollector.getCustomMetrics().isEmpty());
    assertEquals(0, metricsCollector.getTotalOperations());
    assertEquals(0, metricsCollector.getSuccessfulOperations());
    assertEquals(0, metricsCollector.getFailedOperations());
  }

  @Test
  @DisplayName("Should handle edge cases in summary generation")
  void testSummaryEdgeCases() {
    // Test empty summary
    final MetricsCollector.MetricsSummary emptySummary = metricsCollector.generateSummary();
    assertNotNull(emptySummary);
    assertEquals(0, emptySummary.getTotalDataPoints());
    assertFalse(emptySummary.isStatisticallyReliable());

    // Test summary with only failed operations
    recordMultipleDataPoints(
        "test_failed_only",
        "JNI",
        new double[] {0.0, 0.0, 0.0},
        new boolean[] {false, false, false});

    final MetricsCollector.MetricsSummary failedOnlySummary = metricsCollector.generateSummary();
    assertEquals(3, failedOnlySummary.getTotalDataPoints());
    assertEquals(0, failedOnlySummary.getSuccessfulOperations());
    assertEquals(3, failedOnlySummary.getFailedOperations());
    assertEquals(0.0, failedOnlySummary.getSuccessRate(), 0.01);
    assertFalse(failedOnlySummary.isStatisticallyReliable());

    // Test summary with high variability
    metricsCollector.clear();
    recordMultipleDataPoints(
        "test_high_variability",
        "JNI",
        new double[] {10.0, 100.0, 20.0, 200.0, 30.0},
        new boolean[] {true, true, true, true, true});

    final MetricsCollector.MetricsSummary highVariabilitySummary =
        metricsCollector.generateSummary();
    assertTrue(highVariabilitySummary.getCoefficientOfVariation() > 50.0);
    assertFalse(highVariabilitySummary.isStatisticallyReliable());
  }

  @Test
  @DisplayName("Should validate PerformanceDataPoint builder")
  void testPerformanceDataPointBuilder() {
    final Instant timestamp = Instant.now();
    final Duration executionTime = Duration.ofMillis(150);

    final MetricsCollector.PerformanceDataPoint dataPoint =
        MetricsCollector.PerformanceDataPoint.builder("test_builder", "PANAMA")
            .timestamp(timestamp)
            .executionTime(executionTime)
            .memoryBefore(1024)
            .memoryAfter(2048)
            .memoryUsed(1024)
            .peakMemoryUsage(3072)
            .successful(true)
            .customMetric("test_key", "test_value")
            .build();

    assertNotNull(dataPoint);
    assertEquals("test_builder", dataPoint.getTestName());
    assertEquals("PANAMA", dataPoint.getRuntimeType());
    assertEquals(timestamp, dataPoint.getTimestamp());
    assertEquals(executionTime, dataPoint.getExecutionTime());
    assertEquals(1024, dataPoint.getMemoryBefore());
    assertEquals(2048, dataPoint.getMemoryAfter());
    assertEquals(1024, dataPoint.getMemoryUsed());
    assertEquals(3072, dataPoint.getPeakMemoryUsage());
    assertTrue(dataPoint.isSuccessful());
    assertNull(dataPoint.getErrorMessage());
    assertEquals("test_value", dataPoint.getCustomMetrics().get("test_key"));
    assertEquals(150.0, dataPoint.getExecutionTimeMs(), 0.1);
    assertEquals(1024.0 / (1024 * 1024), dataPoint.getMemoryUsageMb(), 0.001);
  }

  @Test
  @DisplayName("Should handle null inputs gracefully")
  void testNullInputHandling() {
    assertThrows(NullPointerException.class, () -> metricsCollector.recordDataPoint(null));

    assertThrows(NullPointerException.class, () -> metricsCollector.startMeasurement(null, "JNI"));

    assertThrows(NullPointerException.class, () -> metricsCollector.startMeasurement("test", null));

    assertThrows(
        NullPointerException.class, () -> metricsCollector.recordCustomMetric(null, "value"));

    assertThrows(
        NullPointerException.class, () -> metricsCollector.recordCustomMetric("key", null));

    assertThrows(
        NullPointerException.class,
        () -> MetricsCollector.PerformanceDataPoint.builder(null, "JNI"));

    assertThrows(
        NullPointerException.class,
        () -> MetricsCollector.PerformanceDataPoint.builder("test", null));
  }

  // Helper methods for creating test data

  private MetricsCollector.PerformanceDataPoint createDataPoint(
      final String testName,
      final String runtimeType,
      final double executionTimeMs,
      final boolean successful) {
    final MetricsCollector.PerformanceDataPoint.Builder builder =
        MetricsCollector.PerformanceDataPoint.builder(testName, runtimeType)
            .executionTime(Duration.ofNanos((long) (executionTimeMs * 1_000_000)))
            .memoryUsed(1024 * 1024)
            .peakMemoryUsage(2 * 1024 * 1024)
            .successful(successful);

    if (!successful) {
      builder.errorMessage("Test failed");
    }

    return builder.build();
  }

  private void recordMultipleDataPoints(
      final String testName,
      final String runtimeType,
      final double[] executionTimes,
      final boolean[] successes) {
    if (executionTimes.length != successes.length) {
      throw new IllegalArgumentException("Arrays must have the same length");
    }

    for (int i = 0; i < executionTimes.length; i++) {
      final MetricsCollector.PerformanceDataPoint dataPoint =
          createDataPoint(testName, runtimeType, executionTimes[i], successes[i]);
      metricsCollector.recordDataPoint(dataPoint);
    }
  }
}
