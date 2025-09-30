package ai.tegmentum.wasmtime4j;

/**
 * Orchestrator for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentOrchestrator {

  /**
   * Starts orchestration with the given configuration.
   *
   * @param config the orchestration configuration
   */
  void start(ComponentOrchestrationConfig config);

  /** Stops orchestration. */
  void stop();

  /**
   * Checks if orchestration is running.
   *
   * @return true if orchestration is running
   */
  boolean isRunning();

  /**
   * Gets orchestration statistics.
   *
   * @return orchestration statistics as a string
   */
  String getStatistics();
}
