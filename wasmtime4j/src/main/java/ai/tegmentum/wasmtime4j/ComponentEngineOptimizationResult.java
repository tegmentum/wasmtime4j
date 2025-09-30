package ai.tegmentum.wasmtime4j;

/**
 * Result of a WebAssembly component engine optimization.
 *
 * @since 1.0.0
 */
public interface ComponentEngineOptimizationResult {

  /**
   * Gets the optimization result status.
   *
   * @return the status (SUCCESS, FAILED, PARTIAL)
   */
  String getStatus();

  /**
   * Gets the optimization result message.
   *
   * @return descriptive message about the optimization
   */
  String getMessage();

  /**
   * Gets the optimization metrics.
   *
   * @return optimization metrics as a string
   */
  String getMetrics();

  /**
   * Gets the optimization duration.
   *
   * @return duration in milliseconds
   */
  long getDuration();
}
