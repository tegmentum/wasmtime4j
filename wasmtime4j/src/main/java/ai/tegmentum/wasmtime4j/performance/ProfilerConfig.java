package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for performance profiler behavior and monitoring settings.
 *
 * <p>This class allows fine-tuning of profiler behavior including sampling intervals,
 * enabled metrics, storage limits, and event thresholds.
 *
 * @since 1.0.0
 */
public final class ProfilerConfig {
  private final Duration samplingInterval;
  private final Set<ProfileMetric> enabledMetrics;
  private final int maxSnapshots;
  private final boolean realTimeEnabled;
  private final Map<String, Object> customOptions;
  private final Duration snapshotRetention;
  private final double eventThreshold;

  private ProfilerConfig(final ProfilerConfigBuilder builder) {
    this.samplingInterval = builder.samplingInterval;
    this.enabledMetrics = Set.copyOf(builder.enabledMetrics);
    this.maxSnapshots = builder.maxSnapshots;
    this.realTimeEnabled = builder.realTimeEnabled;
    this.customOptions = Map.copyOf(builder.customOptions);
    this.snapshotRetention = builder.snapshotRetention;
    this.eventThreshold = builder.eventThreshold;
  }

  /**
   * Creates a new profiler configuration builder.
   *
   * @return new configuration builder
   */
  public static ProfilerConfigBuilder builder() {
    return new ProfilerConfigBuilder();
  }

  /**
   * Creates a default profiler configuration.
   *
   * @return default configuration
   */
  public static ProfilerConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Creates a low-overhead profiler configuration.
   *
   * @return low-overhead configuration
   */
  public static ProfilerConfig lowOverhead() {
    return builder()
        .samplingInterval(Duration.ofSeconds(1))
        .enableMetric(ProfileMetric.CPU_USAGE)
        .enableMetric(ProfileMetric.MEMORY_USAGE)
        .maxSnapshots(10)
        .eventThreshold(0.8)
        .build();
  }

  /**
   * Creates a comprehensive profiler configuration.
   *
   * @return comprehensive configuration
   */
  public static ProfilerConfig comprehensive() {
    return builder()
        .samplingInterval(Duration.ofMillis(50))
        .enableAllMetrics()
        .maxSnapshots(1000)
        .enableRealTime(true)
        .eventThreshold(0.5)
        .build();
  }

  /**
   * Gets the sampling interval for metric collection.
   *
   * @return sampling interval
   */
  public Duration getSamplingInterval() {
    return samplingInterval;
  }

  /**
   * Gets the set of enabled metrics.
   *
   * @return enabled metrics
   */
  public Set<ProfileMetric> getEnabledMetrics() {
    return enabledMetrics;
  }

  /**
   * Gets the maximum number of snapshots to retain.
   *
   * @return maximum snapshots
   */
  public int getMaxSnapshots() {
    return maxSnapshots;
  }

  /**
   * Checks if real-time monitoring is enabled.
   *
   * @return true if real-time monitoring is enabled
   */
  public boolean isRealTimeEnabled() {
    return realTimeEnabled;
  }

  /**
   * Gets custom configuration options.
   *
   * @return map of custom options
   */
  public Map<String, Object> getCustomOptions() {
    return customOptions;
  }

  /**
   * Gets the snapshot retention duration.
   *
   * @return snapshot retention duration
   */
  public Duration getSnapshotRetention() {
    return snapshotRetention;
  }

  /**
   * Gets the event detection threshold.
   *
   * @return event threshold (0.0 to 1.0)
   */
  public double getEventThreshold() {
    return eventThreshold;
  }

  /**
   * Checks if a specific metric is enabled.
   *
   * @param metric the metric to check
   * @return true if the metric is enabled
   */
  public boolean isMetricEnabled(final ProfileMetric metric) {
    return enabledMetrics.contains(metric);
  }

  /**
   * Gets a custom option value.
   *
   * @param key the option key
   * @return option value, or null if not set
   */
  public Object getCustomOption(final String key) {
    return customOptions.get(key);
  }

  /**
   * Gets the estimated profiler overhead based on configuration.
   *
   * @return estimated overhead percentage (0.0 to 100.0)
   */
  public double getEstimatedOverhead() {
    double overhead = 0.0;

    // Base overhead from sampling interval
    final long intervalMs = samplingInterval.toMillis();
    if (intervalMs < 50) {
      overhead += 5.0; // High frequency sampling
    } else if (intervalMs < 200) {
      overhead += 2.0; // Medium frequency sampling
    } else {
      overhead += 0.5; // Low frequency sampling
    }

    // Overhead from enabled metrics
    overhead += enabledMetrics.size() * 0.2;

    // Overhead from real-time monitoring
    if (realTimeEnabled) {
      overhead += 1.0;
    }

    return Math.min(overhead, 10.0); // Cap at 10%
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ProfilerConfig that = (ProfilerConfig) obj;
    return maxSnapshots == that.maxSnapshots &&
        realTimeEnabled == that.realTimeEnabled &&
        Double.compare(that.eventThreshold, eventThreshold) == 0 &&
        Objects.equals(samplingInterval, that.samplingInterval) &&
        Objects.equals(enabledMetrics, that.enabledMetrics) &&
        Objects.equals(customOptions, that.customOptions) &&
        Objects.equals(snapshotRetention, that.snapshotRetention);
  }

  @Override
  public int hashCode() {
    return Objects.hash(samplingInterval, enabledMetrics, maxSnapshots, realTimeEnabled,
                       customOptions, snapshotRetention, eventThreshold);
  }

  @Override
  public String toString() {
    return String.format(
        "ProfilerConfig{samplingInterval=%s, enabledMetrics=%s, maxSnapshots=%d, " +
        "realTimeEnabled=%s, snapshotRetention=%s, eventThreshold=%.2f, customOptions=%s}",
        samplingInterval, enabledMetrics, maxSnapshots, realTimeEnabled,
        snapshotRetention, eventThreshold, customOptions);
  }

  /**
   * Builder for profiler configuration.
   */
  public static final class ProfilerConfigBuilder {
    private Duration samplingInterval = Duration.ofMillis(200);
    private Set<ProfileMetric> enabledMetrics = EnumSet.of(
        ProfileMetric.CPU_USAGE,
        ProfileMetric.MEMORY_USAGE,
        ProfileMetric.FUNCTION_CALLS
    );
    private int maxSnapshots = 50;
    private boolean realTimeEnabled = false;
    private Map<String, Object> customOptions = Map.of();
    private Duration snapshotRetention = Duration.ofHours(1);
    private double eventThreshold = 0.7;

    private ProfilerConfigBuilder() {}

    /**
     * Sets the sampling interval for metric collection.
     *
     * @param interval the sampling interval
     * @return this builder
     */
    public ProfilerConfigBuilder samplingInterval(final Duration interval) {
      this.samplingInterval = Objects.requireNonNull(interval, "interval cannot be null");
      if (interval.isNegative() || interval.isZero()) {
        throw new IllegalArgumentException("interval must be positive: " + interval);
      }
      return this;
    }

    /**
     * Enables a specific metric.
     *
     * @param metric the metric to enable
     * @return this builder
     */
    public ProfilerConfigBuilder enableMetric(final ProfileMetric metric) {
      this.enabledMetrics = EnumSet.copyOf(enabledMetrics);
      this.enabledMetrics.add(Objects.requireNonNull(metric, "metric cannot be null"));
      return this;
    }

    /**
     * Disables a specific metric.
     *
     * @param metric the metric to disable
     * @return this builder
     */
    public ProfilerConfigBuilder disableMetric(final ProfileMetric metric) {
      this.enabledMetrics = EnumSet.copyOf(enabledMetrics);
      this.enabledMetrics.remove(metric);
      return this;
    }

    /**
     * Enables all available metrics.
     *
     * @return this builder
     */
    public ProfilerConfigBuilder enableAllMetrics() {
      this.enabledMetrics = EnumSet.allOf(ProfileMetric.class);
      return this;
    }

    /**
     * Sets the maximum number of snapshots to retain.
     *
     * @param max the maximum snapshots
     * @return this builder
     */
    public ProfilerConfigBuilder maxSnapshots(final int max) {
      if (max <= 0) {
        throw new IllegalArgumentException("maxSnapshots must be positive: " + max);
      }
      this.maxSnapshots = max;
      return this;
    }

    /**
     * Enables or disables real-time monitoring.
     *
     * @param enable true to enable real-time monitoring
     * @return this builder
     */
    public ProfilerConfigBuilder enableRealTime(final boolean enable) {
      this.realTimeEnabled = enable;
      return this;
    }

    /**
     * Sets a custom configuration option.
     *
     * @param key the option key
     * @param value the option value
     * @return this builder
     */
    public ProfilerConfigBuilder customOption(final String key, final Object value) {
      if (customOptions.isEmpty()) {
        this.customOptions = Map.of(key, value);
      } else {
        final var newOptions = new java.util.HashMap<>(customOptions);
        newOptions.put(key, value);
        this.customOptions = Map.copyOf(newOptions);
      }
      return this;
    }

    /**
     * Sets the snapshot retention duration.
     *
     * @param retention the retention duration
     * @return this builder
     */
    public ProfilerConfigBuilder snapshotRetention(final Duration retention) {
      this.snapshotRetention = Objects.requireNonNull(retention, "retention cannot be null");
      if (retention.isNegative()) {
        throw new IllegalArgumentException("retention cannot be negative: " + retention);
      }
      return this;
    }

    /**
     * Sets the event detection threshold.
     *
     * @param threshold the threshold (0.0 to 1.0)
     * @return this builder
     */
    public ProfilerConfigBuilder eventThreshold(final double threshold) {
      if (threshold < 0.0 || threshold > 1.0) {
        throw new IllegalArgumentException("threshold must be between 0.0 and 1.0: " + threshold);
      }
      this.eventThreshold = threshold;
      return this;
    }

    /**
     * Builds the profiler configuration.
     *
     * @return profiler configuration
     */
    public ProfilerConfig build() {
      return new ProfilerConfig(this);
    }
  }
}