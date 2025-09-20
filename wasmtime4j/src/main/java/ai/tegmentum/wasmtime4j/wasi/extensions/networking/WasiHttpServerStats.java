package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Statistics and performance metrics for a WASI HTTP server.
 *
 * <p>This class provides detailed information about HTTP server usage, performance characteristics,
 * and request handling patterns. All values are cumulative since the server was started.
 *
 * <p>Instances of this class are immutable and represent a snapshot of server statistics at the
 * time of creation.
 *
 * @since 1.0.0
 */
public final class WasiHttpServerStats {

  private final long totalRequestsReceived;
  private final long successfulResponses;
  private final long clientErrorResponses;
  private final long serverErrorResponses;
  private final long totalBytesReceived;
  private final long totalBytesSent;
  private final long totalRequestProcessingTime;
  private final int currentActiveConnections;
  private final int maxConcurrentConnections;
  private final long totalConnectionsAccepted;
  private final long totalConnectionsClosed;
  private final double averageRequestProcessingTime;
  private final double averageResponseSize;
  private final long routeNotFoundCount;
  private final long middlewareExecutionTime;
  private final long startTime;
  private final boolean isRunning;

  /**
   * Creates a new HTTP server statistics snapshot.
   *
   * @param totalRequestsReceived total number of requests received
   * @param successfulResponses number of 2xx responses sent
   * @param clientErrorResponses number of 4xx responses sent
   * @param serverErrorResponses number of 5xx responses sent
   * @param totalBytesReceived total bytes received in request bodies
   * @param totalBytesSent total bytes sent in response bodies
   * @param totalRequestProcessingTime cumulative request processing time (ms)
   * @param currentActiveConnections current number of active connections
   * @param maxConcurrentConnections peak number of concurrent connections
   * @param totalConnectionsAccepted total connections accepted
   * @param totalConnectionsClosed total connections closed
   * @param averageRequestProcessingTime average processing time per request (ms)
   * @param averageResponseSize average response size in bytes
   * @param routeNotFoundCount number of 404 responses (route not found)
   * @param middlewareExecutionTime cumulative middleware execution time (ms)
   * @param startTime timestamp when server was started
   * @param isRunning whether the server is currently running
   */
  public WasiHttpServerStats(
      final long totalRequestsReceived,
      final long successfulResponses,
      final long clientErrorResponses,
      final long serverErrorResponses,
      final long totalBytesReceived,
      final long totalBytesSent,
      final long totalRequestProcessingTime,
      final int currentActiveConnections,
      final int maxConcurrentConnections,
      final long totalConnectionsAccepted,
      final long totalConnectionsClosed,
      final double averageRequestProcessingTime,
      final double averageResponseSize,
      final long routeNotFoundCount,
      final long middlewareExecutionTime,
      final long startTime,
      final boolean isRunning) {
    this.totalRequestsReceived = totalRequestsReceived;
    this.successfulResponses = successfulResponses;
    this.clientErrorResponses = clientErrorResponses;
    this.serverErrorResponses = serverErrorResponses;
    this.totalBytesReceived = totalBytesReceived;
    this.totalBytesSent = totalBytesSent;
    this.totalRequestProcessingTime = totalRequestProcessingTime;
    this.currentActiveConnections = currentActiveConnections;
    this.maxConcurrentConnections = maxConcurrentConnections;
    this.totalConnectionsAccepted = totalConnectionsAccepted;
    this.totalConnectionsClosed = totalConnectionsClosed;
    this.averageRequestProcessingTime = averageRequestProcessingTime;
    this.averageResponseSize = averageResponseSize;
    this.routeNotFoundCount = routeNotFoundCount;
    this.middlewareExecutionTime = middlewareExecutionTime;
    this.startTime = startTime;
    this.isRunning = isRunning;
  }

  /**
   * Gets the total number of HTTP requests received.
   *
   * @return total requests received
   */
  public long getTotalRequestsReceived() {
    return totalRequestsReceived;
  }

  /**
   * Gets the number of successful responses (2xx status codes).
   *
   * @return successful responses
   */
  public long getSuccessfulResponses() {
    return successfulResponses;
  }

  /**
   * Gets the number of client error responses (4xx status codes).
   *
   * @return client error responses
   */
  public long getClientErrorResponses() {
    return clientErrorResponses;
  }

  /**
   * Gets the number of server error responses (5xx status codes).
   *
   * @return server error responses
   */
  public long getServerErrorResponses() {
    return serverErrorResponses;
  }

  /**
   * Gets the total number of bytes received in request bodies.
   *
   * @return total bytes received
   */
  public long getTotalBytesReceived() {
    return totalBytesReceived;
  }

  /**
   * Gets the total number of bytes sent in response bodies.
   *
   * @return total bytes sent
   */
  public long getTotalBytesSent() {
    return totalBytesSent;
  }

  /**
   * Gets the cumulative request processing time.
   *
   * @return total processing time in milliseconds
   */
  public long getTotalRequestProcessingTime() {
    return totalRequestProcessingTime;
  }

  /**
   * Gets the current number of active connections.
   *
   * @return active connections
   */
  public int getCurrentActiveConnections() {
    return currentActiveConnections;
  }

  /**
   * Gets the peak number of concurrent connections.
   *
   * @return maximum concurrent connections
   */
  public int getMaxConcurrentConnections() {
    return maxConcurrentConnections;
  }

  /**
   * Gets the total number of connections accepted.
   *
   * @return total connections accepted
   */
  public long getTotalConnectionsAccepted() {
    return totalConnectionsAccepted;
  }

  /**
   * Gets the total number of connections closed.
   *
   * @return total connections closed
   */
  public long getTotalConnectionsClosed() {
    return totalConnectionsClosed;
  }

  /**
   * Gets the average request processing time.
   *
   * @return average processing time in milliseconds
   */
  public double getAverageRequestProcessingTime() {
    return averageRequestProcessingTime;
  }

  /**
   * Gets the average response size.
   *
   * @return average response size in bytes
   */
  public double getAverageResponseSize() {
    return averageResponseSize;
  }

  /**
   * Gets the number of 404 responses (route not found).
   *
   * @return route not found count
   */
  public long getRouteNotFoundCount() {
    return routeNotFoundCount;
  }

  /**
   * Gets the cumulative middleware execution time.
   *
   * @return middleware execution time in milliseconds
   */
  public long getMiddlewareExecutionTime() {
    return middlewareExecutionTime;
  }

  /**
   * Gets the timestamp when the server was started.
   *
   * @return start timestamp in milliseconds since epoch
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Checks if the server is currently running.
   *
   * @return true if running, false otherwise
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Calculates the server uptime in milliseconds.
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
    if (totalRequestsReceived == 0) {
      return 100.0;
    }
    return ((double) successfulResponses / totalRequestsReceived) * 100.0;
  }

  /**
   * Calculates the client error rate as a percentage.
   *
   * @return client error rate (0.0 - 100.0)
   */
  public double getClientErrorRate() {
    if (totalRequestsReceived == 0) {
      return 0.0;
    }
    return ((double) clientErrorResponses / totalRequestsReceived) * 100.0;
  }

  /**
   * Calculates the server error rate as a percentage.
   *
   * @return server error rate (0.0 - 100.0)
   */
  public double getServerErrorRate() {
    if (totalRequestsReceived == 0) {
      return 0.0;
    }
    return ((double) serverErrorResponses / totalRequestsReceived) * 100.0;
  }

  /**
   * Calculates the overall error rate as a percentage.
   *
   * @return error rate (0.0 - 100.0)
   */
  public double getErrorRate() {
    if (totalRequestsReceived == 0) {
      return 0.0;
    }
    final long totalErrors = clientErrorResponses + serverErrorResponses;
    return ((double) totalErrors / totalRequestsReceived) * 100.0;
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
    return (double) totalRequestsReceived / uptimeSeconds;
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
    return (double) (totalBytesReceived + totalBytesSent) / uptimeSeconds;
  }

  /**
   * Calculates the connection utilization rate.
   *
   * @return connection utilization (0.0 - 1.0)
   */
  public double getConnectionUtilization() {
    if (maxConcurrentConnections == 0) {
      return 0.0;
    }
    return (double) currentActiveConnections / maxConcurrentConnections;
  }

  /**
   * Calculates the middleware overhead as a percentage of total processing time.
   *
   * @return middleware overhead percentage (0.0 - 100.0)
   */
  public double getMiddlewareOverhead() {
    if (totalRequestProcessingTime == 0) {
      return 0.0;
    }
    return ((double) middlewareExecutionTime / totalRequestProcessingTime) * 100.0;
  }

  /**
   * Calculates the route not found rate as a percentage.
   *
   * @return route not found rate (0.0 - 100.0)
   */
  public double getRouteNotFoundRate() {
    if (totalRequestsReceived == 0) {
      return 0.0;
    }
    return ((double) routeNotFoundCount / totalRequestsReceived) * 100.0;
  }

  @Override
  public String toString() {
    return "WasiHttpServerStats{"
        + "totalRequests="
        + totalRequestsReceived
        + ", successfulResponses="
        + successfulResponses
        + ", errorResponses="
        + (clientErrorResponses + serverErrorResponses)
        + ", successRate="
        + String.format("%.2f%%", getSuccessRate())
        + ", averageProcessingTime="
        + String.format("%.2fms", averageRequestProcessingTime)
        + ", requestRate="
        + String.format("%.2f req/s", getRequestRate())
        + ", throughput="
        + String.format("%.2f B/s", getThroughputBytesPerSecond())
        + ", activeConnections="
        + currentActiveConnections
        + ", isRunning="
        + isRunning
        + '}';
  }
}
