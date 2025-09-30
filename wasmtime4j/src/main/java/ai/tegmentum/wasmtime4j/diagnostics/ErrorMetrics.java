package ai.tegmentum.wasmtime4j.diagnostics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects and provides metrics about WebAssembly error handling performance.
 *
 * <p>This class tracks various error-related metrics including counts, durations, and performance
 * characteristics. All operations are thread-safe and designed for high-concurrency environments.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ErrorMetrics metrics = errorLogger.getMetrics();
 * System.out.println("Total errors: " + metrics.getTotalErrorCount());
 * System.out.println("Average compilation time: " + metrics.getAverageCompilationErrorDuration());
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ErrorMetrics {

  // Error count metrics
  private final LongAdder totalErrorCount = new LongAdder();
  private final LongAdder compilationErrorCount = new LongAdder();
  private final LongAdder runtimeErrorCount = new LongAdder();
  private final LongAdder validationErrorCount = new LongAdder();
  private final LongAdder resourceErrorCount = new LongAdder();
  private final LongAdder securityErrorCount = new LongAdder();

  // Performance metrics for compilation errors
  private final LongAdder compilationErrorDurationSum = new LongAdder();
  private final LongAdder compilationModuleSizeSum = new LongAdder();
  private final AtomicLong maxCompilationErrorDuration = new AtomicLong(0);
  private final AtomicLong minCompilationErrorDuration = new AtomicLong(Long.MAX_VALUE);

  // Timing metrics
  private final AtomicLong firstErrorTime = new AtomicLong(0);
  private final AtomicLong lastErrorTime = new AtomicLong(0);

  /**
   * Records a compilation error with performance metrics.
   *
   * @param duration the compilation duration in milliseconds
   * @param moduleSize the module size in bytes
   */
  public void recordCompilationError(final long duration, final long moduleSize) {
    totalErrorCount.increment();
    compilationErrorCount.increment();
    compilationErrorDurationSum.add(duration);
    compilationModuleSizeSum.add(moduleSize);

    updateDurationBounds(duration);
    updateErrorTimestamp();
  }

  /** Records a runtime error. */
  public void recordRuntimeError() {
    totalErrorCount.increment();
    runtimeErrorCount.increment();
    updateErrorTimestamp();
  }

  /** Records a validation error. */
  public void recordValidationError() {
    totalErrorCount.increment();
    validationErrorCount.increment();
    updateErrorTimestamp();
  }

  /** Records a resource management error. */
  public void recordResourceError() {
    totalErrorCount.increment();
    resourceErrorCount.increment();
    updateErrorTimestamp();
  }

  /** Records a security violation error. */
  public void recordSecurityError() {
    totalErrorCount.increment();
    securityErrorCount.increment();
    updateErrorTimestamp();
  }

  /**
   * Gets the total number of errors recorded.
   *
   * @return the total error count
   */
  public long getTotalErrorCount() {
    return totalErrorCount.sum();
  }

  /**
   * Gets the number of compilation errors recorded.
   *
   * @return the compilation error count
   */
  public long getCompilationErrorCount() {
    return compilationErrorCount.sum();
  }

  /**
   * Gets the number of runtime errors recorded.
   *
   * @return the runtime error count
   */
  public long getRuntimeErrorCount() {
    return runtimeErrorCount.sum();
  }

  /**
   * Gets the number of validation errors recorded.
   *
   * @return the validation error count
   */
  public long getValidationErrorCount() {
    return validationErrorCount.sum();
  }

  /**
   * Gets the number of resource errors recorded.
   *
   * @return the resource error count
   */
  public long getResourceErrorCount() {
    return resourceErrorCount.sum();
  }

  /**
   * Gets the number of security errors recorded.
   *
   * @return the security error count
   */
  public long getSecurityErrorCount() {
    return securityErrorCount.sum();
  }

  /**
   * Gets the average compilation error duration in milliseconds.
   *
   * @return the average duration, or 0 if no compilation errors recorded
   */
  public double getAverageCompilationErrorDuration() {
    final long count = compilationErrorCount.sum();
    if (count == 0) {
      return 0.0;
    }
    return (double) compilationErrorDurationSum.sum() / count;
  }

  /**
   * Gets the average module size for compilation errors in bytes.
   *
   * @return the average module size, or 0 if no compilation errors recorded
   */
  public double getAverageCompilationModuleSize() {
    final long count = compilationErrorCount.sum();
    if (count == 0) {
      return 0.0;
    }
    return (double) compilationModuleSizeSum.sum() / count;
  }

  /**
   * Gets the maximum compilation error duration in milliseconds.
   *
   * @return the maximum duration, or 0 if no compilation errors recorded
   */
  public long getMaxCompilationErrorDuration() {
    final long max = maxCompilationErrorDuration.get();
    return (max == 0) ? 0 : max;
  }

  /**
   * Gets the minimum compilation error duration in milliseconds.
   *
   * @return the minimum duration, or 0 if no compilation errors recorded
   */
  public long getMinCompilationErrorDuration() {
    final long min = minCompilationErrorDuration.get();
    return (min == Long.MAX_VALUE) ? 0 : min;
  }

  /**
   * Gets the timestamp of the first error recorded.
   *
   * @return the first error timestamp as milliseconds since epoch, or 0 if no errors recorded
   */
  public long getFirstErrorTime() {
    return firstErrorTime.get();
  }

  /**
   * Gets the timestamp of the most recent error recorded.
   *
   * @return the last error timestamp as milliseconds since epoch, or 0 if no errors recorded
   */
  public long getLastErrorTime() {
    return lastErrorTime.get();
  }

  /**
   * Gets the error rate in errors per second since the first error.
   *
   * @return the error rate, or 0 if less than two errors or timespan is too short
   */
  public double getErrorRate() {
    final long first = firstErrorTime.get();
    final long last = lastErrorTime.get();
    final long count = totalErrorCount.sum();

    if (first == 0 || last == 0 || count < 2) {
      return 0.0;
    }

    final long timespan = last - first;
    if (timespan <= 0) {
      return 0.0;
    }

    return (double) count / (timespan / 1000.0);
  }

  /** Resets all metrics to their initial state. */
  public void reset() {
    totalErrorCount.reset();
    compilationErrorCount.reset();
    runtimeErrorCount.reset();
    validationErrorCount.reset();
    resourceErrorCount.reset();
    securityErrorCount.reset();

    compilationErrorDurationSum.reset();
    compilationModuleSizeSum.reset();
    maxCompilationErrorDuration.set(0);
    minCompilationErrorDuration.set(Long.MAX_VALUE);

    firstErrorTime.set(0);
    lastErrorTime.set(0);
  }

  /**
   * Returns a summary of all metrics as a formatted string.
   *
   * @return a string representation of the metrics
   */
  @Override
  public String toString() {
    return String.format(
        "ErrorMetrics{total=%d, compilation=%d(avg_duration=%.1fms), runtime=%d, "
            + "validation=%d, resource=%d, security=%d, error_rate=%.2f/sec}",
        getTotalErrorCount(),
        getCompilationErrorCount(),
        getAverageCompilationErrorDuration(),
        getRuntimeErrorCount(),
        getValidationErrorCount(),
        getResourceErrorCount(),
        getSecurityErrorCount(),
        getErrorRate());
  }

  private void updateDurationBounds(final long duration) {
    maxCompilationErrorDuration.updateAndGet(current -> Math.max(current, duration));
    minCompilationErrorDuration.updateAndGet(current -> Math.min(current, duration));
  }

  private void updateErrorTimestamp() {
    final long now = Instant.now().toEpochMilli();
    lastErrorTime.set(now);
    firstErrorTime.compareAndSet(0, now);
  }
}
