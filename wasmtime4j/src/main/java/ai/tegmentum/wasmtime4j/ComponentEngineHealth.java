package ai.tegmentum.wasmtime4j;

/**
 * Health information for WebAssembly component engines.
 *
 * @since 1.0.0
 */
public interface ComponentEngineHealth {

  /**
   * Gets the health status.
   *
   * @return the health status
   */
  String getHealthStatus();

  /**
   * Checks if the engine is healthy.
   *
   * @return true if the engine is healthy
   */
  boolean isHealthy();

  /**
   * Gets the last health check timestamp.
   *
   * @return the timestamp in milliseconds
   */
  long getLastHealthCheckTime();
}
