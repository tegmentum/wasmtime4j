package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for network-aware streaming WebAssembly compilation.
 *
 * <p>NetworkStreamingConfig extends StreamingConfig with network-specific settings including HTTP
 * range request parameters, CDN optimization, and adaptive streaming based on network conditions.
 *
 * @since 1.0.0
 */
public final class NetworkStreamingConfig {

  /** Default segment size for HTTP range requests (256KB). */
  public static final int DEFAULT_SEGMENT_SIZE = 256 * 1024;

  /** Default connection timeout (10 seconds). */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

  /** Default read timeout (30 seconds). */
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

  /** Default maximum concurrent connections (4). */
  public static final int DEFAULT_MAX_CONNECTIONS = 4;

  private final StreamingConfig baseConfig;
  private final int segmentSize;
  private final Duration connectTimeout;
  private final Duration readTimeout;
  private final int maxConcurrentConnections;
  private final boolean enableRangeRequests;
  private final boolean enableAdaptiveStreaming;
  private final boolean enableNetworkProbing;
  private final long bandwidthThreshold;
  private final Duration latencyThreshold;
  private final RetryPolicy retryPolicy;
  private final Optional<String> userAgent;
  private final Map<String, String> customHeaders;
  private final boolean enableCdnOptimization;
  private final CdnStrategy cdnStrategy;
  private final Optional<String> cdnRegionHint;

  private NetworkStreamingConfig(final Builder builder) {
    this.baseConfig = builder.baseConfig;
    this.segmentSize = builder.segmentSize;
    this.connectTimeout = builder.connectTimeout;
    this.readTimeout = builder.readTimeout;
    this.maxConcurrentConnections = builder.maxConcurrentConnections;
    this.enableRangeRequests = builder.enableRangeRequests;
    this.enableAdaptiveStreaming = builder.enableAdaptiveStreaming;
    this.enableNetworkProbing = builder.enableNetworkProbing;
    this.bandwidthThreshold = builder.bandwidthThreshold;
    this.latencyThreshold = builder.latencyThreshold;
    this.retryPolicy = builder.retryPolicy;
    this.userAgent = builder.userAgent;
    this.customHeaders = Map.copyOf(builder.customHeaders);
    this.enableCdnOptimization = builder.enableCdnOptimization;
    this.cdnStrategy = builder.cdnStrategy;
    this.cdnRegionHint = builder.cdnRegionHint;
  }

  /**
   * Gets the base streaming configuration.
   *
   * @return base streaming configuration
   */
  public StreamingConfig getBaseConfig() {
    return baseConfig;
  }

  /**
   * Gets the segment size for HTTP range requests.
   *
   * @return segment size in bytes
   */
  public int getSegmentSize() {
    return segmentSize;
  }

  /**
   * Gets the connection timeout for HTTP requests.
   *
   * @return connection timeout
   */
  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Gets the read timeout for HTTP requests.
   *
   * @return read timeout
   */
  public Duration getReadTimeout() {
    return readTimeout;
  }

  /**
   * Gets the maximum number of concurrent connections.
   *
   * @return maximum concurrent connections
   */
  public int getMaxConcurrentConnections() {
    return maxConcurrentConnections;
  }

  /**
   * Checks if HTTP range requests are enabled.
   *
   * @return true if range requests are enabled
   */
  public boolean isRangeRequestsEnabled() {
    return enableRangeRequests;
  }

  /**
   * Checks if adaptive streaming is enabled.
   *
   * <p>Adaptive streaming adjusts segment size and request patterns based on network conditions.
   *
   * @return true if adaptive streaming is enabled
   */
  public boolean isAdaptiveStreamingEnabled() {
    return enableAdaptiveStreaming;
  }

  /**
   * Checks if network probing is enabled.
   *
   * <p>Network probing tests network conditions to optimize streaming parameters.
   *
   * @return true if network probing is enabled
   */
  public boolean isNetworkProbingEnabled() {
    return enableNetworkProbing;
  }

  /**
   * Gets the bandwidth threshold for adaptive streaming.
   *
   * @return bandwidth threshold in bytes per second
   */
  public long getBandwidthThreshold() {
    return bandwidthThreshold;
  }

  /**
   * Gets the latency threshold for adaptive streaming.
   *
   * @return latency threshold
   */
  public Duration getLatencyThreshold() {
    return latencyThreshold;
  }

  /**
   * Gets the retry policy for failed requests.
   *
   * @return retry policy
   */
  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  /**
   * Gets the user agent string for HTTP requests.
   *
   * @return user agent string, or empty to use default
   */
  public Optional<String> getUserAgent() {
    return userAgent;
  }

  /**
   * Gets custom HTTP headers to include in requests.
   *
   * @return map of custom headers
   */
  public Map<String, String> getCustomHeaders() {
    return customHeaders;
  }

  /**
   * Checks if CDN optimization is enabled.
   *
   * @return true if CDN optimization is enabled
   */
  public boolean isCdnOptimizationEnabled() {
    return enableCdnOptimization;
  }

  /**
   * Gets the CDN strategy to use.
   *
   * @return CDN strategy
   */
  public CdnStrategy getCdnStrategy() {
    return cdnStrategy;
  }

  /**
   * Gets the CDN region hint for optimization.
   *
   * @return CDN region hint, or empty if not specified
   */
  public Optional<String> getCdnRegionHint() {
    return cdnRegionHint;
  }

  /**
   * Creates a new builder for NetworkStreamingConfig.
   *
   * @param baseConfig the base streaming configuration (must not be null)
   * @return a new builder instance
   * @throws IllegalArgumentException if baseConfig is null
   */
  public static Builder builder(final StreamingConfig baseConfig) {
    if (baseConfig == null) {
      throw new IllegalArgumentException("Base config cannot be null");
    }
    return new Builder(baseConfig);
  }

  /**
   * Creates a NetworkStreamingConfig with default settings.
   *
   * @return a NetworkStreamingConfig with default values
   */
  public static NetworkStreamingConfig defaultConfig() {
    return builder(StreamingConfig.defaultConfig()).build();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final NetworkStreamingConfig that = (NetworkStreamingConfig) obj;
    return segmentSize == that.segmentSize
        && maxConcurrentConnections == that.maxConcurrentConnections
        && enableRangeRequests == that.enableRangeRequests
        && enableAdaptiveStreaming == that.enableAdaptiveStreaming
        && enableNetworkProbing == that.enableNetworkProbing
        && bandwidthThreshold == that.bandwidthThreshold
        && enableCdnOptimization == that.enableCdnOptimization
        && Objects.equals(baseConfig, that.baseConfig)
        && Objects.equals(connectTimeout, that.connectTimeout)
        && Objects.equals(readTimeout, that.readTimeout)
        && Objects.equals(latencyThreshold, that.latencyThreshold)
        && retryPolicy == that.retryPolicy
        && Objects.equals(userAgent, that.userAgent)
        && Objects.equals(customHeaders, that.customHeaders)
        && cdnStrategy == that.cdnStrategy
        && Objects.equals(cdnRegionHint, that.cdnRegionHint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        baseConfig,
        segmentSize,
        connectTimeout,
        readTimeout,
        maxConcurrentConnections,
        enableRangeRequests,
        enableAdaptiveStreaming,
        enableNetworkProbing,
        bandwidthThreshold,
        latencyThreshold,
        retryPolicy,
        userAgent,
        customHeaders,
        enableCdnOptimization,
        cdnStrategy,
        cdnRegionHint);
  }

  @Override
  public String toString() {
    return "NetworkStreamingConfig{"
        + "baseConfig="
        + baseConfig
        + ", segmentSize="
        + segmentSize
        + ", connectTimeout="
        + connectTimeout
        + ", readTimeout="
        + readTimeout
        + ", maxConcurrentConnections="
        + maxConcurrentConnections
        + ", enableRangeRequests="
        + enableRangeRequests
        + ", enableAdaptiveStreaming="
        + enableAdaptiveStreaming
        + ", enableNetworkProbing="
        + enableNetworkProbing
        + ", bandwidthThreshold="
        + bandwidthThreshold
        + ", latencyThreshold="
        + latencyThreshold
        + ", retryPolicy="
        + retryPolicy
        + ", userAgent="
        + userAgent
        + ", customHeaders="
        + customHeaders
        + ", enableCdnOptimization="
        + enableCdnOptimization
        + ", cdnStrategy="
        + cdnStrategy
        + ", cdnRegionHint="
        + cdnRegionHint
        + '}';
  }

  /** Builder for NetworkStreamingConfig. */
  public static final class Builder {
    private final StreamingConfig baseConfig;
    private int segmentSize = DEFAULT_SEGMENT_SIZE;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
    private int maxConcurrentConnections = DEFAULT_MAX_CONNECTIONS;
    private boolean enableRangeRequests = true;
    private boolean enableAdaptiveStreaming = true;
    private boolean enableNetworkProbing = false;
    private long bandwidthThreshold = 1024 * 1024; // 1 MB/s
    private Duration latencyThreshold = Duration.ofMillis(100);
    private RetryPolicy retryPolicy = RetryPolicy.EXPONENTIAL_BACKOFF;
    private Optional<String> userAgent = Optional.empty();
    private Map<String, String> customHeaders = Map.of();
    private boolean enableCdnOptimization = false;
    private CdnStrategy cdnStrategy = CdnStrategy.FASTEST_FIRST;
    private Optional<String> cdnRegionHint = Optional.empty();

    private Builder(final StreamingConfig baseConfig) {
      this.baseConfig = baseConfig;
    }

    /**
     * Sets the segment size for HTTP range requests.
     *
     * @param segmentSize segment size in bytes (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if segmentSize is not positive
     */
    public Builder segmentSize(final int segmentSize) {
      if (segmentSize <= 0) {
        throw new IllegalArgumentException("Segment size must be positive");
      }
      this.segmentSize = segmentSize;
      return this;
    }

    /**
     * Sets the connection timeout for HTTP requests.
     *
     * @param connectTimeout connection timeout (must not be null or negative)
     * @return this builder
     * @throws IllegalArgumentException if connectTimeout is null or negative
     */
    public Builder connectTimeout(final Duration connectTimeout) {
      if (connectTimeout == null) {
        throw new IllegalArgumentException("Connect timeout cannot be null");
      }
      if (connectTimeout.isNegative()) {
        throw new IllegalArgumentException("Connect timeout cannot be negative");
      }
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * Sets the read timeout for HTTP requests.
     *
     * @param readTimeout read timeout (must not be null or negative)
     * @return this builder
     * @throws IllegalArgumentException if readTimeout is null or negative
     */
    public Builder readTimeout(final Duration readTimeout) {
      if (readTimeout == null) {
        throw new IllegalArgumentException("Read timeout cannot be null");
      }
      if (readTimeout.isNegative()) {
        throw new IllegalArgumentException("Read timeout cannot be negative");
      }
      this.readTimeout = readTimeout;
      return this;
    }

    /**
     * Sets the maximum number of concurrent connections.
     *
     * @param maxConcurrentConnections maximum concurrent connections (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if maxConcurrentConnections is not positive
     */
    public Builder maxConcurrentConnections(final int maxConcurrentConnections) {
      if (maxConcurrentConnections <= 0) {
        throw new IllegalArgumentException("Max concurrent connections must be positive");
      }
      this.maxConcurrentConnections = maxConcurrentConnections;
      return this;
    }

    /**
     * Enables or disables HTTP range requests.
     *
     * @param enable whether to enable range requests
     * @return this builder
     */
    public Builder rangeRequests(final boolean enable) {
      this.enableRangeRequests = enable;
      return this;
    }

    /**
     * Enables or disables adaptive streaming.
     *
     * @param enable whether to enable adaptive streaming
     * @return this builder
     */
    public Builder adaptiveStreaming(final boolean enable) {
      this.enableAdaptiveStreaming = enable;
      return this;
    }

    /**
     * Enables or disables network probing.
     *
     * @param enable whether to enable network probing
     * @return this builder
     */
    public Builder networkProbing(final boolean enable) {
      this.enableNetworkProbing = enable;
      return this;
    }

    /**
     * Sets the bandwidth threshold for adaptive streaming.
     *
     * @param bandwidthThreshold bandwidth threshold in bytes per second (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if bandwidthThreshold is negative
     */
    public Builder bandwidthThreshold(final long bandwidthThreshold) {
      if (bandwidthThreshold < 0) {
        throw new IllegalArgumentException("Bandwidth threshold cannot be negative");
      }
      this.bandwidthThreshold = bandwidthThreshold;
      return this;
    }

    /**
     * Sets the latency threshold for adaptive streaming.
     *
     * @param latencyThreshold latency threshold (must not be null or negative)
     * @return this builder
     * @throws IllegalArgumentException if latencyThreshold is null or negative
     */
    public Builder latencyThreshold(final Duration latencyThreshold) {
      if (latencyThreshold == null) {
        throw new IllegalArgumentException("Latency threshold cannot be null");
      }
      if (latencyThreshold.isNegative()) {
        throw new IllegalArgumentException("Latency threshold cannot be negative");
      }
      this.latencyThreshold = latencyThreshold;
      return this;
    }

    /**
     * Sets the retry policy for failed requests.
     *
     * @param retryPolicy retry policy (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if retryPolicy is null
     */
    public Builder retryPolicy(final RetryPolicy retryPolicy) {
      if (retryPolicy == null) {
        throw new IllegalArgumentException("Retry policy cannot be null");
      }
      this.retryPolicy = retryPolicy;
      return this;
    }

    /**
     * Sets the user agent string for HTTP requests.
     *
     * @param userAgent user agent string (can be null)
     * @return this builder
     */
    public Builder userAgent(final String userAgent) {
      this.userAgent = Optional.ofNullable(userAgent);
      return this;
    }

    /**
     * Sets custom HTTP headers to include in requests.
     *
     * @param customHeaders map of custom headers (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if customHeaders is null
     */
    public Builder customHeaders(final Map<String, String> customHeaders) {
      if (customHeaders == null) {
        throw new IllegalArgumentException("Custom headers cannot be null");
      }
      this.customHeaders = customHeaders;
      return this;
    }

    /**
     * Enables or disables CDN optimization.
     *
     * @param enable whether to enable CDN optimization
     * @return this builder
     */
    public Builder cdnOptimization(final boolean enable) {
      this.enableCdnOptimization = enable;
      return this;
    }

    /**
     * Sets the CDN strategy to use.
     *
     * @param cdnStrategy CDN strategy (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if cdnStrategy is null
     */
    public Builder cdnStrategy(final CdnStrategy cdnStrategy) {
      if (cdnStrategy == null) {
        throw new IllegalArgumentException("CDN strategy cannot be null");
      }
      this.cdnStrategy = cdnStrategy;
      return this;
    }

    /**
     * Sets a CDN region hint for optimization.
     *
     * @param cdnRegionHint CDN region hint (can be null)
     * @return this builder
     */
    public Builder cdnRegionHint(final String cdnRegionHint) {
      this.cdnRegionHint = Optional.ofNullable(cdnRegionHint);
      return this;
    }

    /**
     * Builds the NetworkStreamingConfig instance.
     *
     * @return a new NetworkStreamingConfig
     */
    public NetworkStreamingConfig build() {
      return new NetworkStreamingConfig(this);
    }
  }
}