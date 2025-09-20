package ai.tegmentum.wasmtime4j.performance;

/**
 * Listener interface for performance threshold violations.
 *
 * <p>Implementations of this interface receive notifications when performance metrics exceed
 * configured thresholds, allowing for real-time response to performance issues.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface PerformanceThresholdListener {

  /**
   * Called when a performance threshold is violated.
   *
   * <p>This method is invoked asynchronously when any configured performance threshold is exceeded.
   * Implementations should be thread-safe and avoid blocking operations.
   *
   * @param violation details about the threshold violation
   */
  void onThresholdViolation(final ThresholdViolationInfo violation);
}
