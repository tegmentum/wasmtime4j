package ai.tegmentum.wasmtime4j.wasi.extensions;

/**
 * Statistics and performance metrics for WASI networking operations.
 *
 * <p>This class provides comprehensive information about networking usage, performance
 * characteristics, and resource utilization across all networking operations in a WASI context.
 *
 * <p>Instances of this class are immutable and represent a snapshot of networking statistics at the
 * time of creation.
 *
 * @since 1.0.0
 */
public final class WasiNetworkingStats {

  private final long totalSocketsCreated;
  private final int activeSocketsCount;
  private final long totalConnectionsEstablished;
  private final int activeConnectionsCount;
  private final long totalBytesTransferred;
  private final long totalBytesReceived;
  private final long totalBytesSent;
  private final long totalPacketsReceived;
  private final long totalPacketsSent;
  private final long httpRequestsSent;
  private final long httpResponsesReceived;
  private final int httpClientsActive;
  private final int httpServersActive;
  private final long connectionErrors;
  private final long timeoutErrors;
  private final double averageConnectionTime;
  private final long peakActiveConnections;
  private final long startTime;

  /**
   * Creates a new networking statistics snapshot.
   *
   * @param totalSocketsCreated total number of sockets created
   * @param activeSocketsCount current number of active sockets
   * @param totalConnectionsEstablished total connections established
   * @param activeConnectionsCount current number of active connections
   * @param totalBytesTransferred total bytes transferred (sent + received)
   * @param totalBytesReceived total bytes received
   * @param totalBytesSent total bytes sent
   * @param totalPacketsReceived total packets received
   * @param totalPacketsSent total packets sent
   * @param httpRequestsSent total HTTP requests sent
   * @param httpResponsesReceived total HTTP responses received
   * @param httpClientsActive current number of active HTTP clients
   * @param httpServersActive current number of active HTTP servers
   * @param connectionErrors total connection errors
   * @param timeoutErrors total timeout errors
   * @param averageConnectionTime average time to establish connections (ms)
   * @param peakActiveConnections peak number of simultaneous connections
   * @param startTime timestamp when networking was initialized
   */
  public WasiNetworkingStats(
      final long totalSocketsCreated,
      final int activeSocketsCount,
      final long totalConnectionsEstablished,
      final int activeConnectionsCount,
      final long totalBytesTransferred,
      final long totalBytesReceived,
      final long totalBytesSent,
      final long totalPacketsReceived,
      final long totalPacketsSent,
      final long httpRequestsSent,
      final long httpResponsesReceived,
      final int httpClientsActive,
      final int httpServersActive,
      final long connectionErrors,
      final long timeoutErrors,
      final double averageConnectionTime,
      final long peakActiveConnections,
      final long startTime) {
    this.totalSocketsCreated = totalSocketsCreated;
    this.activeSocketsCount = activeSocketsCount;
    this.totalConnectionsEstablished = totalConnectionsEstablished;
    this.activeConnectionsCount = activeConnectionsCount;
    this.totalBytesTransferred = totalBytesTransferred;
    this.totalBytesReceived = totalBytesReceived;
    this.totalBytesSent = totalBytesSent;
    this.totalPacketsReceived = totalPacketsReceived;
    this.totalPacketsSent = totalPacketsSent;
    this.httpRequestsSent = httpRequestsSent;
    this.httpResponsesReceived = httpResponsesReceived;
    this.httpClientsActive = httpClientsActive;
    this.httpServersActive = httpServersActive;
    this.connectionErrors = connectionErrors;
    this.timeoutErrors = timeoutErrors;
    this.averageConnectionTime = averageConnectionTime;
    this.peakActiveConnections = peakActiveConnections;
    this.startTime = startTime;
  }

  /**
   * Gets the total number of sockets created since networking was initialized.
   *
   * @return total sockets created
   */
  public long getTotalSocketsCreated() {
    return totalSocketsCreated;
  }

  /**
   * Gets the current number of active sockets.
   *
   * @return active sockets count
   */
  public int getActiveSocketsCount() {
    return activeSocketsCount;
  }

  /**
   * Gets the total number of connections established.
   *
   * @return total connections established
   */
  public long getTotalConnectionsEstablished() {
    return totalConnectionsEstablished;
  }

  /**
   * Gets the current number of active connections.
   *
   * @return active connections count
   */
  public int getActiveConnectionsCount() {
    return activeConnectionsCount;
  }

  /**
   * Gets the total number of bytes transferred (sent + received).
   *
   * @return total bytes transferred
   */
  public long getTotalBytesTransferred() {
    return totalBytesTransferred;
  }

  /**
   * Gets the total number of bytes received.
   *
   * @return total bytes received
   */
  public long getTotalBytesReceived() {
    return totalBytesReceived;
  }

  /**
   * Gets the total number of bytes sent.
   *
   * @return total bytes sent
   */
  public long getTotalBytesSent() {
    return totalBytesSent;
  }

  /**
   * Gets the total number of packets received.
   *
   * @return total packets received
   */
  public long getTotalPacketsReceived() {
    return totalPacketsReceived;
  }

  /**
   * Gets the total number of packets sent.
   *
   * @return total packets sent
   */
  public long getTotalPacketsSent() {
    return totalPacketsSent;
  }

  /**
   * Gets the total number of HTTP requests sent.
   *
   * @return total HTTP requests sent
   */
  public long getHttpRequestsSent() {
    return httpRequestsSent;
  }

  /**
   * Gets the total number of HTTP responses received.
   *
   * @return total HTTP responses received
   */
  public long getHttpResponsesReceived() {
    return httpResponsesReceived;
  }

  /**
   * Gets the current number of active HTTP clients.
   *
   * @return active HTTP clients count
   */
  public int getHttpClientsActive() {
    return httpClientsActive;
  }

  /**
   * Gets the current number of active HTTP servers.
   *
   * @return active HTTP servers count
   */
  public int getHttpServersActive() {
    return httpServersActive;
  }

  /**
   * Gets the total number of connection errors.
   *
   * @return total connection errors
   */
  public long getConnectionErrors() {
    return connectionErrors;
  }

  /**
   * Gets the total number of timeout errors.
   *
   * @return total timeout errors
   */
  public long getTimeoutErrors() {
    return timeoutErrors;
  }

  /**
   * Gets the average time to establish connections in milliseconds.
   *
   * @return average connection time in milliseconds
   */
  public double getAverageConnectionTime() {
    return averageConnectionTime;
  }

  /**
   * Gets the peak number of simultaneous active connections.
   *
   * @return peak active connections
   */
  public long getPeakActiveConnections() {
    return peakActiveConnections;
  }

  /**
   * Gets the timestamp when networking was initialized.
   *
   * @return initialization timestamp in milliseconds since epoch
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Calculates the networking uptime in milliseconds.
   *
   * @return uptime in milliseconds
   */
  public long getUptime() {
    return System.currentTimeMillis() - startTime;
  }

  /**
   * Calculates the overall error rate as a percentage.
   *
   * @return error rate (0.0 - 100.0)
   */
  public double getErrorRate() {
    final long totalOperations = totalConnectionsEstablished + connectionErrors + timeoutErrors;
    if (totalOperations == 0) {
      return 0.0;
    }
    return ((double) (connectionErrors + timeoutErrors) / totalOperations) * 100.0;
  }

  /**
   * Calculates the average throughput in bytes per second.
   *
   * @return throughput in bytes per second
   */
  public double getThroughputBytesPerSecond() {
    final long uptimeSeconds = getUptime() / 1000;
    return uptimeSeconds > 0 ? (double) totalBytesTransferred / uptimeSeconds : 0.0;
  }

  /**
   * Calculates the connection success rate as a percentage.
   *
   * @return success rate (0.0 - 100.0)
   */
  public double getConnectionSuccessRate() {
    final long totalAttempts = totalConnectionsEstablished + connectionErrors;
    if (totalAttempts == 0) {
      return 100.0;
    }
    return ((double) totalConnectionsEstablished / totalAttempts) * 100.0;
  }

  /**
   * Calculates the HTTP request/response success rate.
   *
   * @return HTTP success rate (0.0 - 100.0)
   */
  public double getHttpSuccessRate() {
    if (httpRequestsSent == 0) {
      return 100.0;
    }
    return ((double) httpResponsesReceived / httpRequestsSent) * 100.0;
  }

  @Override
  public String toString() {
    return "WasiNetworkingStats{"
        + "totalSocketsCreated="
        + totalSocketsCreated
        + ", activeSocketsCount="
        + activeSocketsCount
        + ", totalConnectionsEstablished="
        + totalConnectionsEstablished
        + ", activeConnectionsCount="
        + activeConnectionsCount
        + ", totalBytesTransferred="
        + totalBytesTransferred
        + ", totalBytesReceived="
        + totalBytesReceived
        + ", totalBytesSent="
        + totalBytesSent
        + ", httpRequestsSent="
        + httpRequestsSent
        + ", httpResponsesReceived="
        + httpResponsesReceived
        + ", errorRate="
        + String.format("%.2f%%", getErrorRate())
        + ", throughput="
        + String.format("%.2f B/s", getThroughputBytesPerSecond())
        + '}';
  }
}
