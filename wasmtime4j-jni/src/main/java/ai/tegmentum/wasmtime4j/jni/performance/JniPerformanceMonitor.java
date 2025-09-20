package ai.tegmentum.wasmtime4j.jni.performance;

import ai.tegmentum.wasmtime4j.performance.ExecutionMetrics;
import ai.tegmentum.wasmtime4j.performance.MonitoringOverhead;
import ai.tegmentum.wasmtime4j.performance.PerformanceEvent;
import ai.tegmentum.wasmtime4j.performance.PerformanceMonitor;
import ai.tegmentum.wasmtime4j.performance.PerformanceReport;
import ai.tegmentum.wasmtime4j.performance.PerformanceThresholdListener;
import ai.tegmentum.wasmtime4j.performance.PerformanceThresholds;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * JNI implementation of the PerformanceMonitor interface.
 *
 * <p>This implementation provides comprehensive performance monitoring capabilities
 * for WebAssembly execution using JNI calls to the native performance monitoring
 * system.
 *
 * @since 1.0.0
 */
public final class JniPerformanceMonitor extends JniResource implements PerformanceMonitor {

  private static final Logger LOGGER = Logger.getLogger(JniPerformanceMonitor.class.getName());

  /** Native handle for the performance monitor. */
  private final long nativeHandle;

  /** Performance threshold listeners. */
  private final List<PerformanceThresholdListener> thresholdListeners = new CopyOnWriteArrayList<>();

  /** Current monitoring interval. */
  private volatile Duration monitoringInterval = Duration.ofMillis(100);

  /** Current performance thresholds. */
  private volatile PerformanceThresholds performanceThresholds;

  /**
   * Creates a new JNI performance monitor.
   *
   * @param engineHandle the engine handle to monitor
   * @throws IllegalArgumentException if the engine handle is invalid
   */
  public JniPerformanceMonitor(final long engineHandle) {
    if (engineHandle == 0) {
      throw new IllegalArgumentException("Engine handle cannot be zero");
    }

    this.nativeHandle = nativeCreatePerformanceMonitor(engineHandle);
    if (this.nativeHandle == 0) {
      throw new RuntimeException("Failed to create native performance monitor");
    }

    LOGGER.info("Created JNI performance monitor with handle: " + this.nativeHandle);
  }

  @Override
  public void startMonitoring() {
    validateNotClosed();

    final int result = nativeStartMonitoring(nativeHandle);
    if (result != 0) {
      throw new RuntimeException("Failed to start performance monitoring: error code " + result);
    }

    LOGGER.info("Performance monitoring started");
  }

  @Override
  public void stopMonitoring() {
    validateNotClosed();

    final int result = nativeStopMonitoring(nativeHandle);
    if (result != 0) {
      throw new RuntimeException("Failed to stop performance monitoring: error code " + result);
    }

    LOGGER.info("Performance monitoring stopped");
  }

  @Override
  public boolean isMonitoring() {
    validateNotClosed();
    return nativeIsMonitoring(nativeHandle);
  }

  @Override
  public PerformanceReport generateReport() {
    validateNotClosed();

    if (!isMonitoring()) {
      throw new IllegalStateException("Performance monitoring is not active");
    }

    final long reportHandle = nativeGenerateReport(nativeHandle);
    if (reportHandle == 0) {
      throw new RuntimeException("Failed to generate performance report");
    }

    return new JniPerformanceReport(reportHandle);
  }

  @Override
  public ExecutionMetrics getCurrentMetrics() {
    validateNotClosed();

    if (!isMonitoring()) {
      throw new IllegalStateException("Performance monitoring is not active");
    }

    final long metricsHandle = nativeGetCurrentMetrics(nativeHandle);
    if (metricsHandle == 0) {
      throw new RuntimeException("Failed to get current execution metrics");
    }

    return new JniExecutionMetrics(metricsHandle);
  }

  @Override
  public List<PerformanceEvent> getEvents(final Duration timeWindow) {
    if (timeWindow == null) {
      throw new IllegalArgumentException("Time window cannot be null");
    }
    if (timeWindow.isNegative()) {
      throw new IllegalArgumentException("Time window cannot be negative");
    }

    validateNotClosed();

    final long timeWindowMicros = timeWindow.toNanos() / 1000;
    final long eventsHandle = nativeGetEvents(nativeHandle, timeWindowMicros);
    if (eventsHandle == 0) {
      throw new RuntimeException("Failed to get performance events");
    }

    return JniPerformanceEventList.fromNativeHandle(eventsHandle);
  }

  @Override
  public void setMonitoringInterval(final Duration interval) {
    if (interval == null) {
      throw new IllegalArgumentException("Monitoring interval cannot be null");
    }
    if (interval.isZero() || interval.isNegative()) {
      throw new IllegalArgumentException("Monitoring interval must be positive");
    }

    validateNotClosed();

    final long intervalMicros = interval.toNanos() / 1000;
    final int result = nativeSetMonitoringInterval(nativeHandle, intervalMicros);
    if (result != 0) {
      throw new RuntimeException("Failed to set monitoring interval: error code " + result);
    }

    this.monitoringInterval = interval;
    LOGGER.info("Monitoring interval set to: " + interval);
  }

  @Override
  public Duration getMonitoringInterval() {
    return monitoringInterval;
  }

  @Override
  public void addPerformanceThresholdListener(final PerformanceThresholdListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }

    thresholdListeners.add(listener);
    LOGGER.fine("Added performance threshold listener: " + listener.getClass().getSimpleName());
  }

  @Override
  public boolean removePerformanceThresholdListener(final PerformanceThresholdListener listener) {
    final boolean removed = thresholdListeners.remove(listener);
    if (removed) {
      LOGGER.fine("Removed performance threshold listener: " + listener.getClass().getSimpleName());
    }
    return removed;
  }

  @Override
  public void setPerformanceThresholds(final PerformanceThresholds thresholds) {
    if (thresholds == null) {
      throw new IllegalArgumentException("Performance thresholds cannot be null");
    }

    validateNotClosed();

    final int result = nativeSetPerformanceThresholds(nativeHandle,
        thresholds.getMaxFunctionExecutionTime().toNanos() / 1000,
        thresholds.getMaxAllocationSize(),
        thresholds.getMaxTotalMemoryUsage(),
        (int) thresholds.getMaxCpuUsage(),
        (long) thresholds.getMinInstructionsPerSecond(),
        thresholds.getMaxGcEventsPerMinute(),
        thresholds.getMaxJitCompilationTime().toNanos() / 1000,
        (long) thresholds.getMaxHostFunctionCallsPerSecond(),
        thresholds.getMaxErrorsPerMinute(),
        (int) thresholds.getMaxMonitoringOverhead());

    if (result != 0) {
      throw new RuntimeException("Failed to set performance thresholds: error code " + result);
    }

    this.performanceThresholds = thresholds;
    LOGGER.info("Performance thresholds updated");
  }

  @Override
  public PerformanceThresholds getPerformanceThresholds() {
    return performanceThresholds;
  }

  @Override
  public void reset() {
    validateNotClosed();

    final int result = nativeReset(nativeHandle);
    if (result != 0) {
      throw new RuntimeException("Failed to reset performance monitor: error code " + result);
    }

    LOGGER.info("Performance monitor reset");
  }

  @Override
  public MonitoringOverhead getMonitoringOverhead() {
    validateNotClosed();

    final long overheadHandle = nativeGetMonitoringOverhead(nativeHandle);
    if (overheadHandle == 0) {
      throw new RuntimeException("Failed to get monitoring overhead");
    }

    return new JniMonitoringOverhead(overheadHandle);
  }

  @Override
  protected void disposeInternal() {
    if (nativeHandle != 0) {
      nativeDispose(nativeHandle);
      LOGGER.info("Disposed JNI performance monitor");
    }
  }

  /**
   * Notifies threshold listeners of a violation.
   *
   * @param violationHandle native handle to the threshold violation info
   */
  private void notifyThresholdViolation(final long violationHandle) {
    if (violationHandle == 0 || thresholdListeners.isEmpty()) {
      return;
    }

    try {
      final JniThresholdViolationInfo violation = new JniThresholdViolationInfo(violationHandle);
      for (final PerformanceThresholdListener listener : thresholdListeners) {
        try {
          listener.onThresholdViolation(violation);
        } catch (final Exception e) {
          LOGGER.warning("Exception in threshold listener: " + e.getMessage());
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to create threshold violation info: " + e.getMessage());
    }
  }

  // Native method declarations

  /**
   * Creates a native performance monitor.
   *
   * @param engineHandle the engine handle to monitor
   * @return native performance monitor handle
   */
  private static native long nativeCreatePerformanceMonitor(final long engineHandle);

  /**
   * Starts performance monitoring.
   *
   * @param handle the performance monitor handle
   * @return 0 on success, non-zero on error
   */
  private static native int nativeStartMonitoring(final long handle);

  /**
   * Stops performance monitoring.
   *
   * @param handle the performance monitor handle
   * @return 0 on success, non-zero on error
   */
  private static native int nativeStopMonitoring(final long handle);

  /**
   * Checks if monitoring is active.
   *
   * @param handle the performance monitor handle
   * @return true if monitoring is active
   */
  private static native boolean nativeIsMonitoring(final long handle);

  /**
   * Generates a performance report.
   *
   * @param handle the performance monitor handle
   * @return native performance report handle
   */
  private static native long nativeGenerateReport(final long handle);

  /**
   * Gets current execution metrics.
   *
   * @param handle the performance monitor handle
   * @return native execution metrics handle
   */
  private static native long nativeGetCurrentMetrics(final long handle);

  /**
   * Gets performance events within a time window.
   *
   * @param handle the performance monitor handle
   * @param timeWindowMicros time window in microseconds
   * @return native events list handle
   */
  private static native long nativeGetEvents(final long handle, final long timeWindowMicros);

  /**
   * Sets the monitoring interval.
   *
   * @param handle the performance monitor handle
   * @param intervalMicros monitoring interval in microseconds
   * @return 0 on success, non-zero on error
   */
  private static native int nativeSetMonitoringInterval(final long handle, final long intervalMicros);

  /**
   * Sets performance thresholds.
   *
   * @param handle the performance monitor handle
   * @param maxFunctionExecutionTimeMicros maximum function execution time in microseconds
   * @param maxAllocationSize maximum allocation size in bytes
   * @param maxTotalMemoryUsage maximum total memory usage in bytes
   * @param maxCpuUsagePercent maximum CPU usage percentage
   * @param minInstructionsPerSecond minimum instructions per second
   * @param maxGcEventsPerMinute maximum GC events per minute
   * @param maxJitCompilationTimeMicros maximum JIT compilation time in microseconds
   * @param maxHostFunctionCallsPerSecond maximum host function calls per second
   * @param maxErrorsPerMinute maximum errors per minute
   * @param maxMonitoringOverheadPercent maximum monitoring overhead percentage
   * @return 0 on success, non-zero on error
   */
  private static native int nativeSetPerformanceThresholds(
      final long handle,
      final long maxFunctionExecutionTimeMicros,
      final long maxAllocationSize,
      final long maxTotalMemoryUsage,
      final int maxCpuUsagePercent,
      final long minInstructionsPerSecond,
      final int maxGcEventsPerMinute,
      final long maxJitCompilationTimeMicros,
      final long maxHostFunctionCallsPerSecond,
      final int maxErrorsPerMinute,
      final int maxMonitoringOverheadPercent);

  /**
   * Resets the performance monitor.
   *
   * @param handle the performance monitor handle
   * @return 0 on success, non-zero on error
   */
  private static native int nativeReset(final long handle);

  /**
   * Gets monitoring overhead metrics.
   *
   * @param handle the performance monitor handle
   * @return native monitoring overhead handle
   */
  private static native long nativeGetMonitoringOverhead(final long handle);

  /**
   * Disposes the native performance monitor.
   *
   * @param handle the performance monitor handle
   */
  private static native void nativeDispose(final long handle);

  /**
   * Callback method called from native code when thresholds are violated.
   *
   * @param monitorHandle the performance monitor handle
   * @param violationHandle the threshold violation info handle
   */
  @SuppressWarnings("unused") // Called from native code
  private static void onThresholdViolation(final long monitorHandle, final long violationHandle) {
    // This would be called from native code - implementation would need
    // a way to map monitor handles to Java instances
    // For now, this is a placeholder for the callback mechanism
  }
}