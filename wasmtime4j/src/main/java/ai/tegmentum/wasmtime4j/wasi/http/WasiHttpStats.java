package ai.tegmentum.wasmtime4j.wasi.http;

import java.time.Duration;

/**
 * Statistics about HTTP requests made through a WASI HTTP context.
 *
 * <p>This interface provides metrics about HTTP operations including request counts, byte counts,
 * timing information, and error statistics.
 *
 * @since 1.0.0
 */
public interface WasiHttpStats {

  /**
   * Gets the total number of HTTP requests made.
   *
   * @return the total request count
   */
  long getTotalRequests();

  /**
   * Gets the number of successful HTTP requests.
   *
   * @return the successful request count
   */
  long getSuccessfulRequests();

  /**
   * Gets the number of failed HTTP requests.
   *
   * @return the failed request count
   */
  long getFailedRequests();

  /**
   * Gets the number of currently active HTTP requests.
   *
   * @return the active request count
   */
  int getActiveRequests();

  /**
   * Gets the total bytes sent in request bodies.
   *
   * @return the total bytes sent
   */
  long getTotalBytesSent();

  /**
   * Gets the total bytes received in response bodies.
   *
   * @return the total bytes received
   */
  long getTotalBytesReceived();

  /**
   * Gets the average request duration.
   *
   * @return the average request duration
   */
  Duration getAverageRequestDuration();

  /**
   * Gets the minimum request duration observed.
   *
   * @return the minimum request duration
   */
  Duration getMinRequestDuration();

  /**
   * Gets the maximum request duration observed.
   *
   * @return the maximum request duration
   */
  Duration getMaxRequestDuration();

  /**
   * Gets the number of connection timeouts.
   *
   * @return the connection timeout count
   */
  long getConnectionTimeouts();

  /**
   * Gets the number of read timeouts.
   *
   * @return the read timeout count
   */
  long getReadTimeouts();

  /**
   * Gets the number of blocked requests (due to host restrictions).
   *
   * @return the blocked request count
   */
  long getBlockedRequests();

  /**
   * Gets the number of requests blocked due to body size limits.
   *
   * @return the body size limit violation count
   */
  long getBodySizeLimitViolations();

  /**
   * Gets the number of active connections in the pool.
   *
   * @return the active connection count
   */
  int getActiveConnections();

  /**
   * Gets the number of idle connections in the pool.
   *
   * @return the idle connection count
   */
  int getIdleConnections();
}
