package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestResult;
import ai.tegmentum.wasmtime4j.webassembly.RuntimeTestExecution;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Performance regression detection framework for WebAssembly runtime testing. Tracks performance
 * baselines and detects regressions across test runs.
 */
public final class PerformanceRegressionDetector {
  private static final Logger LOGGER =
      Logger.getLogger(PerformanceRegressionDetector.class.getName());

  // Configuration
  private static final double DEFAULT_REGRESSION_THRESHOLD =
      1.2; // 20% slower considered regression
  private static final String BASELINE_FILE = "performance-baselines.json";
  private static final Path BASELINE_PATH = Paths.get("target", "test-reports", BASELINE_FILE);

  // Performance tracking
  private static final ConcurrentMap<String, PerformanceBaseline> baselines =
      new ConcurrentHashMap<>();
  private static final ConcurrentMap<String, List<PerformanceMeasurement>> measurements =
      new ConcurrentHashMap<>();

  private PerformanceRegressionDetector() {
    // Utility class - prevent instantiation
  }

  /**
   * Records a performance measurement from a cross-runtime test result.
   *
   * @param testName the test name
   * @param result the cross-runtime test result
   */
  public static void recordMeasurement(final String testName, final CrossRuntimeTestResult result) {
    final Instant measurementTime = Instant.now();

    // Record JNI measurement
    recordRuntimeMeasurement(testName, RuntimeType.JNI, result.getJniResult(), measurementTime);

    // Record Panama measurement if available
    if (result.hasPanamaResult()) {
      recordRuntimeMeasurement(
          testName, RuntimeType.PANAMA, result.getPanamaResult(), measurementTime);
    }

    LOGGER.fine("Recorded performance measurement for test: " + testName);
  }

  /**
   * Records a performance measurement for a specific runtime.
   *
   * @param testName the test name
   * @param runtimeType the runtime type
   * @param execution the runtime execution result
   * @param measurementTime the measurement time
   */
  private static void recordRuntimeMeasurement(
      final String testName,
      final RuntimeType runtimeType,
      final RuntimeTestExecution execution,
      final Instant measurementTime) {
    if (!execution.isSuccessful()) {
      return; // Don't record measurements for failed executions
    }

    final String measurementKey = testName + ":" + runtimeType.name();
    final PerformanceMeasurement measurement =
        new PerformanceMeasurement(
            testName,
            runtimeType,
            execution.getDuration(),
            measurementTime,
            execution.isSuccessful());

    measurements.computeIfAbsent(measurementKey, k -> new ArrayList<>()).add(measurement);

    // Update or create baseline
    updateBaseline(measurementKey, measurement);
  }

  /**
   * Updates the performance baseline for a measurement.
   *
   * @param measurementKey the measurement key
   * @param measurement the new measurement
   */
  private static void updateBaseline(
      final String measurementKey, final PerformanceMeasurement measurement) {
    final PerformanceBaseline existingBaseline = baselines.get(measurementKey);

    if (existingBaseline == null) {
      // Create new baseline
      final PerformanceBaseline newBaseline =
          new PerformanceBaseline(
              measurement.getTestName(),
              measurement.getRuntimeType(),
              measurement.getDuration(),
              measurement.getMeasurementTime(),
              1 // measurement count
              );
      baselines.put(measurementKey, newBaseline);
      LOGGER.info(
          "Created new performance baseline for "
              + measurementKey
              + ": "
              + measurement.getDuration().toMillis()
              + "ms");
    } else {
      // Update existing baseline with moving average
      final PerformanceBaseline updatedBaseline = existingBaseline.withNewMeasurement(measurement);
      baselines.put(measurementKey, updatedBaseline);
    }
  }

  /**
   * Detects performance regressions for a specific test.
   *
   * @param testName the test name
   * @param currentResult the current test result
   * @return performance regression analysis
   */
  public static PerformanceRegressionAnalysis detectRegressions(
      final String testName, final CrossRuntimeTestResult currentResult) {
    return detectRegressions(testName, currentResult, DEFAULT_REGRESSION_THRESHOLD);
  }

  /**
   * Detects performance regressions with a custom threshold.
   *
   * @param testName the test name
   * @param currentResult the current test result
   * @param regressionThreshold the regression threshold (e.g., 1.2 = 20% slower)
   * @return performance regression analysis
   */
  public static PerformanceRegressionAnalysis detectRegressions(
      final String testName,
      final CrossRuntimeTestResult currentResult,
      final double regressionThreshold) {
    final PerformanceRegressionAnalysis.Builder analysisBuilder =
        new PerformanceRegressionAnalysis.Builder(testName);

    // Analyze JNI performance
    analyzeRuntimeRegression(
        testName,
        RuntimeType.JNI,
        currentResult.getJniResult(),
        regressionThreshold,
        analysisBuilder);

    // Analyze Panama performance if available
    if (currentResult.hasPanamaResult()) {
      analyzeRuntimeRegression(
          testName,
          RuntimeType.PANAMA,
          currentResult.getPanamaResult(),
          regressionThreshold,
          analysisBuilder);
    }

    return analysisBuilder.build();
  }

  /** Analyzes regression for a specific runtime. */
  private static void analyzeRuntimeRegression(
      final String testName,
      final RuntimeType runtimeType,
      final RuntimeTestExecution execution,
      final double threshold,
      final PerformanceRegressionAnalysis.Builder builder) {
    if (!execution.isSuccessful()) {
      return;
    }

    final String measurementKey = testName + ":" + runtimeType.name();
    final PerformanceBaseline baseline = baselines.get(measurementKey);

    if (baseline == null) {
      builder.addInfo("No baseline available for " + runtimeType + " - creating new baseline");
      return;
    }

    final Duration currentDuration = execution.getDuration();
    final Duration baselineDuration = baseline.getBaselineDuration();

    if (baselineDuration.isZero()) {
      builder.addWarning("Baseline duration is zero for " + runtimeType);
      return;
    }

    final double performanceRatio =
        (double) currentDuration.toMillis() / baselineDuration.toMillis();

    if (performanceRatio > threshold) {
      // Performance regression detected
      final long regressionMs = currentDuration.toMillis() - baselineDuration.toMillis();
      final double regressionPercent = (performanceRatio - 1.0) * 100.0;

      builder.addRegression(
          new PerformanceRegression(
              runtimeType, baselineDuration, currentDuration, regressionMs, regressionPercent));

      LOGGER.warning(
          "Performance regression detected for "
              + testName
              + " on "
              + runtimeType
              + ": "
              + String.format("%.1f%%", regressionPercent)
              + " slower ("
              + regressionMs
              + "ms increase)");
    } else if (performanceRatio < 0.8) {
      // Significant performance improvement
      final long improvementMs = baselineDuration.toMillis() - currentDuration.toMillis();
      final double improvementPercent = (1.0 - performanceRatio) * 100.0;

      builder.addInfo(
          "Performance improvement detected for "
              + runtimeType
              + ": "
              + String.format("%.1f%%", improvementPercent)
              + " faster ("
              + improvementMs
              + "ms decrease)");
    } else {
      builder.addInfo(
          "Performance within acceptable range for "
              + runtimeType
              + " (ratio: "
              + String.format("%.2f", performanceRatio)
              + ")");
    }
  }

  /**
   * Gets all current performance baselines.
   *
   * @return map of measurement keys to baselines
   */
  public static Map<String, PerformanceBaseline> getAllBaselines() {
    return new HashMap<>(baselines);
  }

  /**
   * Gets all performance measurements for a test.
   *
   * @param testName the test name
   * @return map of runtime types to measurements
   */
  public static Map<RuntimeType, List<PerformanceMeasurement>> getMeasurements(
      final String testName) {
    final Map<RuntimeType, List<PerformanceMeasurement>> result = new HashMap<>();

    for (final RuntimeType runtimeType : RuntimeType.values()) {
      final String key = testName + ":" + runtimeType.name();
      final List<PerformanceMeasurement> measurementList = measurements.get(key);
      if (measurementList != null) {
        result.put(runtimeType, new ArrayList<>(measurementList));
      }
    }

    return result;
  }

  /**
   * Saves performance baselines to disk.
   *
   * @throws IOException if saving fails
   */
  public static void saveBaselines() throws IOException {
    Files.createDirectories(BASELINE_PATH.getParent());

    final StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"baselines\": {\n");

    boolean first = true;
    for (final Map.Entry<String, PerformanceBaseline> entry : baselines.entrySet()) {
      if (!first) {
        json.append(",\n");
      }
      first = false;

      final PerformanceBaseline baseline = entry.getValue();
      json.append("    \"").append(entry.getKey()).append("\": {\n");
      json.append("      \"testName\": \"").append(baseline.getTestName()).append("\",\n");
      json.append("      \"runtimeType\": \"").append(baseline.getRuntimeType()).append("\",\n");
      json.append("      \"baselineDurationMs\": ")
          .append(baseline.getBaselineDuration().toMillis())
          .append(",\n");
      json.append("      \"lastUpdated\": \"").append(baseline.getLastUpdated()).append("\",\n");
      json.append("      \"measurementCount\": ")
          .append(baseline.getMeasurementCount())
          .append("\n");
      json.append("    }");
    }

    json.append("\n  },\n");
    json.append("  \"savedAt\": \"").append(Instant.now()).append("\"\n");
    json.append("}\n");

    Files.writeString(
        BASELINE_PATH,
        json.toString(),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);

    LOGGER.info("Saved " + baselines.size() + " performance baselines to " + BASELINE_PATH);
  }

  /** Clears all performance data. */
  public static void clearPerformanceData() {
    baselines.clear();
    measurements.clear();
    LOGGER.info("Cleared all performance data");
  }

  /**
   * Creates a performance summary report.
   *
   * @return the performance summary
   */
  public static String createPerformanceSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Performance Baseline Summary\n");
    sb.append("===========================\n\n");

    sb.append("Total Baselines: ").append(baselines.size()).append('\n');
    sb.append("Total Measurements: ")
        .append(measurements.values().stream().mapToInt(List::size).sum())
        .append('\n');
    sb.append('\n');

    // Group by test name
    final Map<String, Map<RuntimeType, PerformanceBaseline>> byTest = new HashMap<>();
    for (final PerformanceBaseline baseline : baselines.values()) {
      byTest
          .computeIfAbsent(baseline.getTestName(), k -> new HashMap<>())
          .put(baseline.getRuntimeType(), baseline);
    }

    for (final Map.Entry<String, Map<RuntimeType, PerformanceBaseline>> testEntry :
        byTest.entrySet()) {
      sb.append("Test: ").append(testEntry.getKey()).append('\n');

      for (final Map.Entry<RuntimeType, PerformanceBaseline> runtimeEntry :
          testEntry.getValue().entrySet()) {
        final PerformanceBaseline baseline = runtimeEntry.getValue();
        sb.append("  ")
            .append(runtimeEntry.getKey())
            .append(": ")
            .append(baseline.getBaselineDuration().toMillis())
            .append("ms")
            .append(" (")
            .append(baseline.getMeasurementCount())
            .append(" measurements)")
            .append('\n');
      }
      sb.append('\n');
    }

    return sb.toString();
  }
}
