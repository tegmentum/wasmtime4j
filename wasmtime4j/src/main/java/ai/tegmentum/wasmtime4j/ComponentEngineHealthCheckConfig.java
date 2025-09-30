package ai.tegmentum.wasmtime4j;

/**
 * Configuration for WebAssembly component engine health checks.
 *
 * @since 1.0.0
 */
public interface ComponentEngineHealthCheckConfig {

  /**
   * Gets the health check interval in milliseconds.
   *
   * @return the interval in milliseconds
   */
  long getHealthCheckInterval();

  /**
   * Gets the health check timeout in milliseconds.
   *
   * @return the timeout in milliseconds
   */
  long getHealthCheckTimeout();

  /**
   * Checks if health checks are enabled.
   *
   * @return true if health checks are enabled
   */
  boolean isHealthCheckEnabled();
}
