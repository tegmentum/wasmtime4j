package ai.tegmentum.wasmtime4j.jni.performance;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.performance.ExecutionMetrics;
import ai.tegmentum.wasmtime4j.performance.PerformanceEvent;
import ai.tegmentum.wasmtime4j.performance.PerformanceEventType;
import ai.tegmentum.wasmtime4j.performance.PerformanceReport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JNI implementation of PerformanceReport interface.
 *
 * <p>Provides comprehensive performance analysis based on native monitoring data.
 *
 * @since 1.0.0
 */
public final class JniPerformanceReport extends JniResource implements PerformanceReport {

  private static final Logger LOGGER = Logger.getLogger(JniPerformanceReport.class.getName());

  /** Native handle for the performance report. */
  private final long nativeHandle;

  /**
   * Creates a new JNI performance report instance.
   *
   * @param nativeHandle the native performance report handle
   * @throws IllegalArgumentException if the native handle is invalid
   */
  public JniPerformanceReport(final long nativeHandle) {
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be zero");
    }

    this.nativeHandle = nativeHandle;
    LOGGER.fine("Created JNI performance report with handle: " + this.nativeHandle);
  }

  @Override
  public Instant getStartTime() {
    validateNotClosed();
    final long timestampMicros = nativeGetStartTime(nativeHandle);
    return Instant.ofEpochSecond(timestampMicros / 1_000_000, (timestampMicros % 1_000_000) * 1000);
  }

  @Override
  public Instant getEndTime() {
    validateNotClosed();
    final long timestampMicros = nativeGetEndTime(nativeHandle);
    return Instant.ofEpochSecond(timestampMicros / 1_000_000, (timestampMicros % 1_000_000) * 1000);
  }

  @Override
  public Duration getTotalDuration() {
    validateNotClosed();
    final long durationMicros = nativeGetTotalDuration(nativeHandle);
    return Duration.ofNanos(durationMicros * 1000);
  }

  @Override
  public ExecutionMetrics getOverallMetrics() {
    validateNotClosed();
    final long metricsHandle = nativeGetOverallMetrics(nativeHandle);
    if (metricsHandle == 0) {
      throw new RuntimeException("Failed to get overall execution metrics");
    }
    return new JniExecutionMetrics(metricsHandle);
  }

  @Override
  public ExecutionMetrics getAverageMetrics() {
    validateNotClosed();
    final long metricsHandle = nativeGetAverageMetrics(nativeHandle);
    if (metricsHandle == 0) {
      throw new RuntimeException("Failed to get average execution metrics");
    }
    return new JniExecutionMetrics(metricsHandle);
  }

  @Override
  public ExecutionMetrics getPeakMetrics() {
    validateNotClosed();
    final long metricsHandle = nativeGetPeakMetrics(nativeHandle);
    if (metricsHandle == 0) {
      throw new RuntimeException("Failed to get peak execution metrics");
    }
    return new JniExecutionMetrics(metricsHandle);
  }

  @Override
  public List<PerformanceEvent> getSignificantEvents() {
    validateNotClosed();
    final long eventsHandle = nativeGetSignificantEvents(nativeHandle);
    if (eventsHandle == 0) {
      throw new RuntimeException("Failed to get significant performance events");
    }
    return JniPerformanceEventList.fromNativeHandle(eventsHandle);
  }

  @Override
  public List<PerformanceEvent> getEventsByType(final PerformanceEventType type) {
    if (type == null) {
      throw new IllegalArgumentException("Event type cannot be null");
    }
    validateNotClosed();
    final long eventsHandle = nativeGetEventsByType(nativeHandle, type.ordinal());
    if (eventsHandle == 0) {
      throw new RuntimeException("Failed to get events by type");
    }
    return JniPerformanceEventList.fromNativeHandle(eventsHandle);
  }

  @Override
  public Map<PerformanceEventType, Integer> getEventCounts() {
    validateNotClosed();
    final long countsHandle = nativeGetEventCounts(nativeHandle);
    if (countsHandle == 0) {
      throw new RuntimeException("Failed to get event counts");
    }
    return JniEventCountsMap.fromNativeHandle(countsHandle);
  }

  @Override
  public List<String> getPerformanceInsights() {
    validateNotClosed();
    final long insightsHandle = nativeGetPerformanceInsights(nativeHandle);
    if (insightsHandle == 0) {
      throw new RuntimeException("Failed to get performance insights");
    }
    return JniStringList.fromNativeHandle(insightsHandle);
  }

  @Override
  public List<String> getOptimizationRecommendations() {
    validateNotClosed();
    final long recommendationsHandle = nativeGetOptimizationRecommendations(nativeHandle);
    if (recommendationsHandle == 0) {
      throw new RuntimeException("Failed to get optimization recommendations");
    }
    return JniStringList.fromNativeHandle(recommendationsHandle);
  }

  @Override
  public double getPerformanceScore() {
    validateNotClosed();
    return nativeGetPerformanceScore(nativeHandle);
  }

  @Override
  public Map<String, Object> getAnalysisSummary() {
    validateNotClosed();
    final long summaryHandle = nativeGetAnalysisSummary(nativeHandle);
    if (summaryHandle == 0) {
      throw new RuntimeException("Failed to get analysis summary");
    }
    return JniAnalysisSummaryMap.fromNativeHandle(summaryHandle);
  }

  @Override
  public String getDetailedReport() {
    validateNotClosed();
    final String report = nativeGetDetailedReport(nativeHandle);
    if (report == null) {
      throw new RuntimeException("Failed to get detailed performance report");
    }
    return report;
  }

  @Override
  public String getExecutiveSummary() {
    validateNotClosed();
    final String summary = nativeGetExecutiveSummary(nativeHandle);
    if (summary == null) {
      throw new RuntimeException("Failed to get executive summary");
    }
    return summary;
  }

  @Override
  public boolean hasPerformanceIssues() {
    validateNotClosed();
    return nativeHasPerformanceIssues(nativeHandle);
  }

  @Override
  public List<String> getPerformanceIssues() {
    validateNotClosed();
    final long issuesHandle = nativeGetPerformanceIssues(nativeHandle);
    if (issuesHandle == 0) {
      throw new RuntimeException("Failed to get performance issues");
    }
    return JniStringList.fromNativeHandle(issuesHandle);
  }

  @Override
  public boolean exceedsThresholds() {
    validateNotClosed();
    return nativeExceedsThresholds(nativeHandle);
  }

  @Override
  public List<String> getThresholdViolations() {
    validateNotClosed();
    final long violationsHandle = nativeGetThresholdViolations(nativeHandle);
    if (violationsHandle == 0) {
      throw new RuntimeException("Failed to get threshold violations");
    }
    return JniStringList.fromNativeHandle(violationsHandle);
  }

  @Override
  public String exportToJson() {
    validateNotClosed();
    final String json = nativeExportToJson(nativeHandle);
    if (json == null) {
      throw new RuntimeException("Failed to export performance report to JSON");
    }
    return json;
  }

  @Override
  public String exportToCsv() {
    validateNotClosed();
    final String csv = nativeExportToCsv(nativeHandle);
    if (csv == null) {
      throw new RuntimeException("Failed to export performance report to CSV");
    }
    return csv;
  }

  @Override
  protected void disposeInternal() {
    if (nativeHandle != 0) {
      nativeDispose(nativeHandle);
      LOGGER.fine("Disposed JNI performance report");
    }
  }

  // Native method declarations

  private static native long nativeGetStartTime(final long handle);

  private static native long nativeGetEndTime(final long handle);

  private static native long nativeGetTotalDuration(final long handle);

  private static native long nativeGetOverallMetrics(final long handle);

  private static native long nativeGetAverageMetrics(final long handle);

  private static native long nativeGetPeakMetrics(final long handle);

  private static native long nativeGetSignificantEvents(final long handle);

  private static native long nativeGetEventsByType(final long handle, final int eventType);

  private static native long nativeGetEventCounts(final long handle);

  private static native long nativeGetPerformanceInsights(final long handle);

  private static native long nativeGetOptimizationRecommendations(final long handle);

  private static native double nativeGetPerformanceScore(final long handle);

  private static native long nativeGetAnalysisSummary(final long handle);

  private static native String nativeGetDetailedReport(final long handle);

  private static native String nativeGetExecutiveSummary(final long handle);

  private static native boolean nativeHasPerformanceIssues(final long handle);

  private static native long nativeGetPerformanceIssues(final long handle);

  private static native boolean nativeExceedsThresholds(final long handle);

  private static native long nativeGetThresholdViolations(final long handle);

  private static native String nativeExportToJson(final long handle);

  private static native String nativeExportToCsv(final long handle);

  private static native void nativeDispose(final long handle);
}
