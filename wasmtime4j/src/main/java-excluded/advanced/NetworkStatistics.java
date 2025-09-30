package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Statistics for network streaming operations.
 *
 * <p>NetworkStatistics provides real-time information about network performance during streaming
 * WebAssembly compilation, including bandwidth, latency, error rates, and CDN performance.
 *
 * @since 1.0.0
 */
public final class NetworkStatistics {

  private final long totalBytesDownloaded;
  private final long totalBytesRequested;
  private final double downloadThroughputBytesPerSecond;
  private final Duration averageLatency;
  private final int activeConnections;
  private final int maxConcurrentConnections;
  private final int successfulRequests;
  private final int failedRequests;
  private final int retriedRequests;
  private final Optional<String> activeCdnUrl;
  private final int segmentSize;
  private final Instant networkingStartTime;
  private final Duration totalNetworkingTime;
  private final double networkEfficiency;

  private NetworkStatistics(final Builder builder) {
    this.totalBytesDownloaded = builder.totalBytesDownloaded;
    this.totalBytesRequested = builder.totalBytesRequested;
    this.downloadThroughputBytesPerSecond = builder.downloadThroughputBytesPerSecond;
    this.averageLatency = builder.averageLatency;
    this.activeConnections = builder.activeConnections;
    this.maxConcurrentConnections = builder.maxConcurrentConnections;
    this.successfulRequests = builder.successfulRequests;
    this.failedRequests = builder.failedRequests;
    this.retriedRequests = builder.retriedRequests;
    this.activeCdnUrl = builder.activeCdnUrl;
    this.segmentSize = builder.segmentSize;
    this.networkingStartTime = builder.networkingStartTime;
    this.totalNetworkingTime = builder.totalNetworkingTime;
    this.networkEfficiency = builder.networkEfficiency;
  }

  /**
   * Gets the total number of bytes downloaded.
   *
   * @return total bytes downloaded
   */
  public long getTotalBytesDownloaded() {
    return totalBytesDownloaded;
  }

  /**
   * Gets the total number of bytes requested.
   *
   * @return total bytes requested
   */
  public long getTotalBytesRequested() {
    return totalBytesRequested;
  }

  /**
   * Gets the current download throughput in bytes per second.
   *
   * @return download throughput
   */
  public double getDownloadThroughputBytesPerSecond() {
    return downloadThroughputBytesPerSecond;
  }

  /**
   * Gets the average latency for network requests.
   *
   * @return average latency
   */
  public Duration getAverageLatency() {
    return averageLatency;
  }

  /**
   * Gets the current number of active connections.
   *
   * @return number of active connections
   */
  public int getActiveConnections() {
    return activeConnections;
  }

  /**
   * Gets the maximum number of concurrent connections allowed.
   *
   * @return maximum concurrent connections
   */
  public int getMaxConcurrentConnections() {
    return maxConcurrentConnections;
  }

  /**
   * Gets the number of successful requests.
   *
   * @return number of successful requests
   */
  public int getSuccessfulRequests() {
    return successfulRequests;
  }

  /**
   * Gets the number of failed requests.
   *
   * @return number of failed requests
   */
  public int getFailedRequests() {
    return failedRequests;
  }

  /**
   * Gets the number of retried requests.
   *
   * @return number of retried requests
   */
  public int getRetriedRequests() {
    return retriedRequests;
  }

  /**
   * Gets the currently active CDN URL (if any).
   *
   * @return active CDN URL, or empty if not using CDN
   */
  public Optional<String> getActiveCdnUrl() {
    return activeCdnUrl;
  }

  /**
   * Gets the current segment size being used for requests.
   *
   * @return segment size in bytes
   */
  public int getSegmentSize() {
    return segmentSize;
  }

  /**
   * Gets the time when network operations started.
   *
   * @return networking start time
   */
  public Instant getNetworkingStartTime() {
    return networkingStartTime;
  }

  /**
   * Gets the total time spent on network operations.
   *
   * @return total networking time
   */
  public Duration getTotalNetworkingTime() {
    return totalNetworkingTime;
  }

  /**
   * Gets the network efficiency ratio (0.0 to 1.0).
   *
   * <p>Network efficiency represents how effectively the available bandwidth is being utilized.
   *
   * @return network efficiency ratio
   */
  public double getNetworkEfficiency() {
    return networkEfficiency;
  }

  /**
   * Calculates the download completion percentage.
   *
   * @return completion percentage (0.0 to 100.0)
   */
  public double getDownloadCompletionPercentage() {
    return totalBytesRequested > 0
        ? (double) totalBytesDownloaded / totalBytesRequested * 100.0
        : 0.0;
  }

  /**
   * Calculates the request success rate.
   *
   * @return success rate (0.0 to 1.0)
   */
  public double getRequestSuccessRate() {
    final int totalRequests = successfulRequests + failedRequests;
    return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
  }

  /**
   * Calculates the retry rate.
   *
   * @return retry rate (0.0 to 1.0)
   */
  public double getRetryRate() {
    final int totalRequests = successfulRequests + failedRequests;
    return totalRequests > 0 ? (double) retriedRequests / totalRequests : 0.0;
  }

  /**
   * Calculates the connection utilization.
   *
   * @return connection utilization (0.0 to 1.0)
   */
  public double getConnectionUtilization() {
    return maxConcurrentConnections > 0
        ? (double) activeConnections / maxConcurrentConnections
        : 0.0;
  }

  /**
   * Creates a new builder for NetworkStatistics.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final NetworkStatistics that = (NetworkStatistics) obj;
    return totalBytesDownloaded == that.totalBytesDownloaded
        && totalBytesRequested == that.totalBytesRequested
        && Double.compare(that.downloadThroughputBytesPerSecond, downloadThroughputBytesPerSecond)
            == 0
        && activeConnections == that.activeConnections
        && maxConcurrentConnections == that.maxConcurrentConnections
        && successfulRequests == that.successfulRequests
        && failedRequests == that.failedRequests
        && retriedRequests == that.retriedRequests
        && segmentSize == that.segmentSize
        && Double.compare(that.networkEfficiency, networkEfficiency) == 0
        && Objects.equals(averageLatency, that.averageLatency)
        && Objects.equals(activeCdnUrl, that.activeCdnUrl)
        && Objects.equals(networkingStartTime, that.networkingStartTime)
        && Objects.equals(totalNetworkingTime, that.totalNetworkingTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalBytesDownloaded,
        totalBytesRequested,
        downloadThroughputBytesPerSecond,
        averageLatency,
        activeConnections,
        maxConcurrentConnections,
        successfulRequests,
        failedRequests,
        retriedRequests,
        activeCdnUrl,
        segmentSize,
        networkingStartTime,
        totalNetworkingTime,
        networkEfficiency);
  }

  @Override
  public String toString() {
    return "NetworkStatistics{"
        + "totalBytesDownloaded="
        + totalBytesDownloaded
        + ", totalBytesRequested="
        + totalBytesRequested
        + ", downloadThroughputBytesPerSecond="
        + downloadThroughputBytesPerSecond
        + ", averageLatency="
        + averageLatency
        + ", activeConnections="
        + activeConnections
        + ", maxConcurrentConnections="
        + maxConcurrentConnections
        + ", successfulRequests="
        + successfulRequests
        + ", failedRequests="
        + failedRequests
        + ", retriedRequests="
        + retriedRequests
        + ", activeCdnUrl="
        + activeCdnUrl
        + ", segmentSize="
        + segmentSize
        + ", networkingStartTime="
        + networkingStartTime
        + ", totalNetworkingTime="
        + totalNetworkingTime
        + ", networkEfficiency="
        + networkEfficiency
        + '}';
  }

  /** Builder for NetworkStatistics. */
  public static final class Builder {
    private long totalBytesDownloaded = 0;
    private long totalBytesRequested = 0;
    private double downloadThroughputBytesPerSecond = 0.0;
    private Duration averageLatency = Duration.ZERO;
    private int activeConnections = 0;
    private int maxConcurrentConnections = 0;
    private int successfulRequests = 0;
    private int failedRequests = 0;
    private int retriedRequests = 0;
    private Optional<String> activeCdnUrl = Optional.empty();
    private int segmentSize = 0;
    private Instant networkingStartTime = Instant.now();
    private Duration totalNetworkingTime = Duration.ZERO;
    private double networkEfficiency = 0.0;

    private Builder() {}

    public Builder totalBytesDownloaded(final long totalBytesDownloaded) {
      this.totalBytesDownloaded = totalBytesDownloaded;
      return this;
    }

    public Builder totalBytesRequested(final long totalBytesRequested) {
      this.totalBytesRequested = totalBytesRequested;
      return this;
    }

    public Builder downloadThroughputBytesPerSecond(final double downloadThroughputBytesPerSecond) {
      this.downloadThroughputBytesPerSecond = downloadThroughputBytesPerSecond;
      return this;
    }

    public Builder averageLatency(final Duration averageLatency) {
      this.averageLatency = Objects.requireNonNull(averageLatency, "Average latency cannot be null");
      return this;
    }

    public Builder activeConnections(final int activeConnections) {
      this.activeConnections = activeConnections;
      return this;
    }

    public Builder maxConcurrentConnections(final int maxConcurrentConnections) {
      this.maxConcurrentConnections = maxConcurrentConnections;
      return this;
    }

    public Builder successfulRequests(final int successfulRequests) {
      this.successfulRequests = successfulRequests;
      return this;
    }

    public Builder failedRequests(final int failedRequests) {
      this.failedRequests = failedRequests;
      return this;
    }

    public Builder retriedRequests(final int retriedRequests) {
      this.retriedRequests = retriedRequests;
      return this;
    }

    public Builder activeCdnUrl(final String activeCdnUrl) {
      this.activeCdnUrl = Optional.ofNullable(activeCdnUrl);
      return this;
    }

    public Builder segmentSize(final int segmentSize) {
      this.segmentSize = segmentSize;
      return this;
    }

    public Builder networkingStartTime(final Instant networkingStartTime) {
      this.networkingStartTime =
          Objects.requireNonNull(networkingStartTime, "Networking start time cannot be null");
      return this;
    }

    public Builder totalNetworkingTime(final Duration totalNetworkingTime) {
      this.totalNetworkingTime =
          Objects.requireNonNull(totalNetworkingTime, "Total networking time cannot be null");
      return this;
    }

    public Builder networkEfficiency(final double networkEfficiency) {
      this.networkEfficiency = networkEfficiency;
      return this;
    }

    public NetworkStatistics build() {
      return new NetworkStatistics(this);
    }
  }
}