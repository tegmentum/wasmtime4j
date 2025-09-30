package ai.tegmentum.wasmtime4j;

/**
 * Result of a WebAssembly component engine health check.
 *
 * @since 1.0.0
 */
public interface ComponentEngineHealthCheckResult {

  /**
   * Gets the health check result status.
   *
   * @return the status (HEALTHY, UNHEALTHY, UNKNOWN)
   */
  String getStatus();

  /**
   * Gets the health check message.
   *
   * @return descriptive message about the health status
   */
  String getMessage();

  /**
   * Gets the timestamp of the health check.
   *
   * @return timestamp in milliseconds
   */
  long getTimestamp();

  /**
   * Gets the duration of the health check.
   *
   * @return duration in milliseconds
   */
  long getDuration();
}
