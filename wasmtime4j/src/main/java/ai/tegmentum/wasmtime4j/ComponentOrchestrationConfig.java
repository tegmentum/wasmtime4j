package ai.tegmentum.wasmtime4j;

/**
 * Configuration for WebAssembly component orchestration.
 *
 * @since 1.0.0
 */
public interface ComponentOrchestrationConfig {

  /**
   * Gets the orchestration mode.
   *
   * @return the orchestration mode
   */
  String getOrchestrationMode();

  /**
   * Gets the coordination timeout in milliseconds.
   *
   * @return the timeout in milliseconds
   */
  long getCoordinationTimeout();

  /**
   * Checks if orchestration is enabled.
   *
   * @return true if orchestration is enabled
   */
  boolean isOrchestrationEnabled();

  /**
   * Gets the maximum concurrent components.
   *
   * @return the maximum concurrent components
   */
  int getMaxConcurrentComponents();
}
