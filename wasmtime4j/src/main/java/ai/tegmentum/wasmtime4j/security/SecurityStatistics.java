package ai.tegmentum.wasmtime4j.security;

/**
 * Security statistics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SecurityStatistics {

  /**
   * Gets the total number of access requests.
   *
   * @return the request count
   */
  long getTotalRequests();

  /**
   * Gets the number of granted requests.
   *
   * @return the granted count
   */
  long getGrantedRequests();

  /**
   * Gets the number of denied requests.
   *
   * @return the denied count
   */
  long getDeniedRequests();

  /**
   * Gets the number of security violations.
   *
   * @return the violation count
   */
  long getViolationCount();

  /**
   * Gets the uptime in milliseconds.
   *
   * @return the uptime
   */
  long getUptime();
}
