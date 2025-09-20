package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Statistics and performance metrics for a WASI HTTP client.
 *
 * <p>This class provides detailed information about HTTP client usage,
 * performance characteristics, and request/response patterns. All values
 * are cumulative since the client was created.
 *
 * <p>Instances of this class are immutable and represent a snapshot
 * of client statistics at the time of creation.
 *
 * @since 1.0.0
 */
public final class WasiHttpClientStats {

  private final long totalRequests;
  private final long successfulRequests;
  private final long failedRequests;
  private final long timeoutRequests;
  private final long redirectsFollowed;
  private final long totalBytesUploaded;
  private final long totalBytesDownloaded;
  private final long totalConnectionTime;
  private final long totalRequestTime;
  private final long totalResponseTime;
  private final int activeRequests;
  private final int connectionPoolSize;
  private final int maxConnectionPoolSize;
  private final long connectionsCreated;
  private final long connectionsReused;
  private final double averageRequestTime;
  private final double averageResponseTime;
  private final long startTime;

  /**
   * Creates a new HTTP client statistics snapshot.
   *
   * @param totalRequests total number of requests sent
   * @param successfulRequests number of successful requests (2xx status)
   * @param failedRequests number of failed requests (4xx/5xx status)
   * @param timeoutRequests number of requests that timed out
   * @param redirectsFollowed total number of redirects followed
   * @param totalBytesUploaded total bytes uploaded in request bodies
   * @param totalBytesDownloaded total bytes downloaded in response bodies
   * @param totalConnectionTime cumulative time spent establishing connections (ms)
   * @param totalRequestTime cumulative time spent sending requests (ms)
   * @param totalResponseTime cumulative time spent receiving responses (ms)
   * @param activeRequests current number of active/pending requests
   * @param connectionPoolSize current connection pool size
   * @param maxConnectionPoolSize maximum connection pool size
   * @param connectionsCreated total number of new connections created
   * @param connectionsReused total number of times connections were reused
   * @param averageRequestTime average time per request (ms)
   * @param averageResponseTime average time per response (ms)
   * @param startTime timestamp when client was created
   */
  public WasiHttpClientStats(final long totalRequests, final long successfulRequests,
                           final long failedRequests, final long timeoutRequests,
                           final long redirectsFollowed, final long totalBytesUploaded,
                           final long totalBytesDownloaded, final long totalConnectionTime,
                           final long totalRequestTime, final long totalResponseTime,
                           final int activeRequests, final int connectionPoolSize,
                           final int maxConnectionPoolSize, final long connectionsCreated,
                           final long connectionsReused, final double averageRequestTime,
                           final double averageResponseTime, final long startTime) {
    this.totalRequests = totalRequests;
    this.successfulRequests = successfulRequests;
    this.failedRequests = failedRequests;
    this.timeoutRequests = timeoutRequests;
    this.redirectsFollowed = redirectsFollowed;
    this.totalBytesUploaded = totalBytesUploaded;
    this.totalBytesDownloaded = totalBytesDownloaded;
    this.totalConnectionTime = totalConnectionTime;
    this.totalRequestTime = totalRequestTime;
    this.totalResponseTime = totalResponseTime;
    this.activeRequests = activeRequests;
    this.connectionPoolSize = connectionPoolSize;
    this.maxConnectionPoolSize = maxConnectionPoolSize;
    this.connectionsCreated = connectionsCreated;
    this.connectionsReused = connectionsReused;
    this.averageRequestTime = averageRequestTime;
    this.averageResponseTime = averageResponseTime;
    this.startTime = startTime;
  }

  /**
   * Gets the total number of HTTP requests sent.
   *
   * @return total requests
   */
  public long getTotalRequests() {
    return totalRequests;
  }

  /**
   * Gets the number of successful requests (2xx status codes).
   *
   * @return successful requests
   */
  public long getSuccessfulRequests() {
    return successfulRequests;
  }

  /**
   * Gets the number of failed requests (4xx/5xx status codes).
   *
   * @return failed requests
   */
  public long getFailedRequests() {
    return failedRequests;
  }

  /**
   * Gets the number of requests that timed out.
   *
   * @return timeout requests
   */
  public long getTimeoutRequests() {
    return timeoutRequests;
  }

  /**
   * Gets the total number of HTTP redirects followed.
   *
   * @return redirects followed
   */
  public long getRedirectsFollowed() {
    return redirectsFollowed;
  }

  /**
   * Gets the total number of bytes uploaded in request bodies.
   *
   * @return total bytes uploaded
   */
  public long getTotalBytesUploaded() {
    return totalBytesUploaded;
  }

  /**
   * Gets the total number of bytes downloaded in response bodies.
   *
   * @return total bytes downloaded
   */
  public long getTotalBytesDownloaded() {
    return totalBytesDownloaded;
  }

  /**
   * Gets the cumulative time spent establishing connections.
   *
   * @return total connection time in milliseconds
   */
  public long getTotalConnectionTime() {
    return totalConnectionTime;
  }

  /**
   * Gets the cumulative time spent sending requests.
   *
   * @return total request time in milliseconds
   */
  public long getTotalRequestTime() {
    return totalRequestTime;
  }

  /**
   * Gets the cumulative time spent receiving responses.
   *
   * @return total response time in milliseconds
   */
  public long getTotalResponseTime() {
    return totalResponseTime;
  }

  /**
   * Gets the current number of active/pending requests.
   *
   * @return active requests
   */
  public int getActiveRequests() {
    return activeRequests;
  }

  /**
   * Gets the current connection pool size.
   *
   * @return connection pool size
   */
  public int getConnectionPoolSize() {
    return connectionPoolSize;
  }

  /**
   * Gets the maximum connection pool size.
   *
   * @return maximum connection pool size
   */
  public int getMaxConnectionPoolSize() {
    return maxConnectionPoolSize;
  }

  /**
   * Gets the total number of new connections created.
   *
   * @return connections created
   */
  public long getConnectionsCreated() {
    return connectionsCreated;
  }

  /**
   * Gets the total number of times connections were reused.
   *
   * @return connections reused
   */
  public long getConnectionsReused() {
    return connectionsReused;
  }

  /**
   * Gets the average time per request.
   *
   * @return average request time in milliseconds
   */
  public double getAverageRequestTime() {
    return averageRequestTime;
  }

  /**
   * Gets the average time per response.
   *
   * @return average response time in milliseconds
   */
  public double getAverageResponseTime() {
    return averageResponseTime;
  }

  /**
   * Gets the timestamp when the client was created.
   *
   * @return creation timestamp in milliseconds since epoch
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Calculates the client uptime in milliseconds.
   *
   * @return uptime in milliseconds
   */
  public long getUptime() {
    return System.currentTimeMillis() - startTime;
  }

  /**
   * Calculates the success rate as a percentage.
   *
   * @return success rate (0.0 - 100.0)
   */
  public double getSuccessRate() {
    if (totalRequests == 0) {
      return 100.0;
    }
    return ((double) successfulRequests / totalRequests) * 100.0;
  }

  /**
   * Calculates the error rate as a percentage.
   *
   * @return error rate (0.0 - 100.0)
   */
  public double getErrorRate() {
    if (totalRequests == 0) {
      return 0.0;
    }
    return ((double) failedRequests / totalRequests) * 100.0;
  }

  /**
   * Calculates the timeout rate as a percentage.
   *
   * @return timeout rate (0.0 - 100.0)
   */
  public double getTimeoutRate() {
    if (totalRequests == 0) {
      return 0.0;
    }
    return ((double) timeoutRequests / totalRequests) * 100.0;
  }

  /**
   * Calculates the connection reuse rate as a percentage.
   *
   * @return connection reuse rate (0.0 - 100.0)
   */
  public double getConnectionReuseRate() {
    final long totalConnections = connectionsCreated + connectionsReused;
    if (totalConnections == 0) {
      return 0.0;
    }
    return ((double) connectionsReused / totalConnections) * 100.0;
  }

  /**
   * Calculates the total throughput in bytes per second.
   *
   * @return throughput in bytes per second
   */
  public double getThroughputBytesPerSecond() {
    final long uptimeSeconds = getUptime() / 1000;
    if (uptimeSeconds == 0) {
      return 0.0;
    }
    return (double) (totalBytesUploaded + totalBytesDownloaded) / uptimeSeconds;
  }

  /**
   * Calculates the request rate (requests per second).
   *
   * @return requests per second
   */
  public double getRequestRate() {
    final long uptimeSeconds = getUptime() / 1000;
    if (uptimeSeconds == 0) {
      return 0.0;
    }
    return (double) totalRequests / uptimeSeconds;
  }

  /**
   * Calculates the average number of redirects per request.
   *
   * @return average redirects per request
   */
  public double getAverageRedirectsPerRequest() {
    if (totalRequests == 0) {
      return 0.0;
    }
    return (double) redirectsFollowed / totalRequests;
  }

  @Override
  public String toString() {
    return "WasiHttpClientStats{" +
           "totalRequests=" + totalRequests +
           ", successfulRequests=" + successfulRequests +
           ", failedRequests=" + failedRequests +
           ", timeoutRequests=" + timeoutRequests +
           ", successRate=" + String.format("%.2f%%", getSuccessRate()) +
           ", averageRequestTime=" + String.format("%.2fms", averageRequestTime) +
           ", throughput=" + String.format("%.2f B/s", getThroughputBytesPerSecond()) +
           ", connectionReuseRate=" + String.format("%.2f%%", getConnectionReuseRate()) +
           '}';
  }
}