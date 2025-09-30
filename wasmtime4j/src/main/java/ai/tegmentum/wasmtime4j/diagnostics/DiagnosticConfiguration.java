package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Configuration for WebAssembly diagnostic and logging behavior.
 *
 * <p>This class provides centralized control over diagnostic features including error logging,
 * performance monitoring, and debug output. Configuration can be modified at runtime and affects
 * all error loggers and diagnostic tools.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();
 * config.setGlobalLogLevel(Level.FINE);
 * config.setPerformanceMonitoringEnabled(true);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class DiagnosticConfiguration {

  private static final DiagnosticConfiguration INSTANCE = new DiagnosticConfiguration();

  // Global configuration flags
  private final AtomicBoolean performanceMonitoringEnabled = new AtomicBoolean(false);
  private final AtomicBoolean detailedStackTracesEnabled = new AtomicBoolean(true);
  private final AtomicBoolean errorRecoveryLoggingEnabled = new AtomicBoolean(true);
  private final AtomicBoolean securityViolationLoggingEnabled = new AtomicBoolean(true);
  private final AtomicBoolean resourceTrackingEnabled = new AtomicBoolean(false);

  // Logging configuration
  private final AtomicReference<Level> globalLogLevel = new AtomicReference<>(Level.INFO);
  private final AtomicReference<Level> errorLogLevel = new AtomicReference<>(Level.SEVERE);
  private final AtomicReference<Level> performanceLogLevel = new AtomicReference<>(Level.INFO);

  // Performance thresholds
  private final AtomicReference<Long> slowCompilationThreshold = new AtomicReference<>(1000L);
  private final AtomicReference<Long> slowRuntimeThreshold = new AtomicReference<>(100L);
  private final AtomicReference<Long> largeModuleThreshold = new AtomicReference<>(1024L * 1024L);

  private DiagnosticConfiguration() {
    // Singleton pattern - private constructor
  }

  /**
   * Gets the singleton instance of the diagnostic configuration.
   *
   * @return the diagnostic configuration instance
   */
  public static DiagnosticConfiguration getInstance() {
    return INSTANCE;
  }

  /**
   * Checks if performance monitoring is enabled.
   *
   * @return true if performance monitoring is enabled
   */
  public boolean isPerformanceMonitoringEnabled() {
    return performanceMonitoringEnabled.get();
  }

  /**
   * Sets whether performance monitoring is enabled.
   *
   * @param enabled true to enable performance monitoring
   */
  public void setPerformanceMonitoringEnabled(final boolean enabled) {
    performanceMonitoringEnabled.set(enabled);
  }

  /**
   * Checks if detailed stack traces are enabled for error logging.
   *
   * @return true if detailed stack traces are enabled
   */
  public boolean isDetailedStackTracesEnabled() {
    return detailedStackTracesEnabled.get();
  }

  /**
   * Sets whether detailed stack traces are enabled for error logging.
   *
   * @param enabled true to enable detailed stack traces
   */
  public void setDetailedStackTracesEnabled(final boolean enabled) {
    detailedStackTracesEnabled.set(enabled);
  }

  /**
   * Checks if error recovery logging is enabled.
   *
   * @return true if error recovery logging is enabled
   */
  public boolean isErrorRecoveryLoggingEnabled() {
    return errorRecoveryLoggingEnabled.get();
  }

  /**
   * Sets whether error recovery logging is enabled.
   *
   * @param enabled true to enable error recovery logging
   */
  public void setErrorRecoveryLoggingEnabled(final boolean enabled) {
    errorRecoveryLoggingEnabled.set(enabled);
  }

  /**
   * Checks if security violation logging is enabled.
   *
   * @return true if security violation logging is enabled
   */
  public boolean isSecurityViolationLoggingEnabled() {
    return securityViolationLoggingEnabled.get();
  }

  /**
   * Sets whether security violation logging is enabled.
   *
   * @param enabled true to enable security violation logging
   */
  public void setSecurityViolationLoggingEnabled(final boolean enabled) {
    securityViolationLoggingEnabled.set(enabled);
  }

  /**
   * Checks if resource tracking is enabled.
   *
   * @return true if resource tracking is enabled
   */
  public boolean isResourceTrackingEnabled() {
    return resourceTrackingEnabled.get();
  }

  /**
   * Sets whether resource tracking is enabled.
   *
   * @param enabled true to enable resource tracking
   */
  public void setResourceTrackingEnabled(final boolean enabled) {
    resourceTrackingEnabled.set(enabled);
  }

  /**
   * Gets the global logging level.
   *
   * @return the global logging level
   */
  public Level getGlobalLogLevel() {
    return globalLogLevel.get();
  }

  /**
   * Sets the global logging level.
   *
   * @param level the global logging level
   * @throws IllegalArgumentException if level is null
   */
  public void setGlobalLogLevel(final Level level) {
    if (level == null) {
      throw new IllegalArgumentException("Log level cannot be null");
    }
    globalLogLevel.set(level);
  }

  /**
   * Gets the error logging level.
   *
   * @return the error logging level
   */
  public Level getErrorLogLevel() {
    return errorLogLevel.get();
  }

  /**
   * Sets the error logging level.
   *
   * @param level the error logging level
   * @throws IllegalArgumentException if level is null
   */
  public void setErrorLogLevel(final Level level) {
    if (level == null) {
      throw new IllegalArgumentException("Error log level cannot be null");
    }
    errorLogLevel.set(level);
  }

  /**
   * Gets the performance logging level.
   *
   * @return the performance logging level
   */
  public Level getPerformanceLogLevel() {
    return performanceLogLevel.get();
  }

  /**
   * Sets the performance logging level.
   *
   * @param level the performance logging level
   * @throws IllegalArgumentException if level is null
   */
  public void setPerformanceLogLevel(final Level level) {
    if (level == null) {
      throw new IllegalArgumentException("Performance log level cannot be null");
    }
    performanceLogLevel.set(level);
  }

  /**
   * Gets the threshold for considering a compilation slow (in milliseconds).
   *
   * @return the slow compilation threshold
   */
  public long getSlowCompilationThreshold() {
    return slowCompilationThreshold.get();
  }

  /**
   * Sets the threshold for considering a compilation slow (in milliseconds).
   *
   * @param threshold the slow compilation threshold
   * @throws IllegalArgumentException if threshold is negative
   */
  public void setSlowCompilationThreshold(final long threshold) {
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold cannot be negative");
    }
    slowCompilationThreshold.set(threshold);
  }

  /**
   * Gets the threshold for considering a runtime operation slow (in milliseconds).
   *
   * @return the slow runtime threshold
   */
  public long getSlowRuntimeThreshold() {
    return slowRuntimeThreshold.get();
  }

  /**
   * Sets the threshold for considering a runtime operation slow (in milliseconds).
   *
   * @param threshold the slow runtime threshold
   * @throws IllegalArgumentException if threshold is negative
   */
  public void setSlowRuntimeThreshold(final long threshold) {
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold cannot be negative");
    }
    slowRuntimeThreshold.set(threshold);
  }

  /**
   * Gets the threshold for considering a module large (in bytes).
   *
   * @return the large module threshold
   */
  public long getLargeModuleThreshold() {
    return largeModuleThreshold.get();
  }

  /**
   * Sets the threshold for considering a module large (in bytes).
   *
   * @param threshold the large module threshold
   * @throws IllegalArgumentException if threshold is negative
   */
  public void setLargeModuleThreshold(final long threshold) {
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold cannot be negative");
    }
    largeModuleThreshold.set(threshold);
  }

  /**
   * Checks if a compilation duration is considered slow.
   *
   * @param duration the compilation duration in milliseconds
   * @return true if the duration exceeds the slow compilation threshold
   */
  public boolean isSlowCompilation(final long duration) {
    return duration > slowCompilationThreshold.get();
  }

  /**
   * Checks if a runtime operation duration is considered slow.
   *
   * @param duration the runtime duration in milliseconds
   * @return true if the duration exceeds the slow runtime threshold
   */
  public boolean isSlowRuntime(final long duration) {
    return duration > slowRuntimeThreshold.get();
  }

  /**
   * Checks if a module size is considered large.
   *
   * @param size the module size in bytes
   * @return true if the size exceeds the large module threshold
   */
  public boolean isLargeModule(final long size) {
    return size > largeModuleThreshold.get();
  }

  /** Resets all configuration to default values. */
  public void resetToDefaults() {
    performanceMonitoringEnabled.set(false);
    detailedStackTracesEnabled.set(true);
    errorRecoveryLoggingEnabled.set(true);
    securityViolationLoggingEnabled.set(true);
    resourceTrackingEnabled.set(false);

    globalLogLevel.set(Level.INFO);
    errorLogLevel.set(Level.SEVERE);
    performanceLogLevel.set(Level.INFO);

    slowCompilationThreshold.set(1000L);
    slowRuntimeThreshold.set(100L);
    largeModuleThreshold.set(1024L * 1024L);
  }

  /**
   * Returns a string representation of the current configuration.
   *
   * @return a string representation of the configuration
   */
  @Override
  public String toString() {
    return String.format(
        "DiagnosticConfiguration{performance=%s, stackTraces=%s, errorRecovery=%s, "
            + "security=%s, resourceTracking=%s, globalLevel=%s, errorLevel=%s, "
            + "performanceLevel=%s, slowCompilation=%dms, slowRuntime=%dms, largeModule=%d bytes}",
        performanceMonitoringEnabled.get(),
        detailedStackTracesEnabled.get(),
        errorRecoveryLoggingEnabled.get(),
        securityViolationLoggingEnabled.get(),
        resourceTrackingEnabled.get(),
        globalLogLevel.get(),
        errorLogLevel.get(),
        performanceLogLevel.get(),
        slowCompilationThreshold.get(),
        slowRuntimeThreshold.get(),
        largeModuleThreshold.get());
  }
}
