package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Configuration options for streaming WebAssembly operations.
 *
 * <p>StreamingOptions provides fine-grained control over streaming compilation, validation, and
 * memory operations. This interface allows customization of buffer sizes, timeouts, progress
 * tracking, and resource limits for streaming operations.
 *
 * <p>Streaming options are particularly important for optimizing performance in different
 * environments and use cases, from memory-constrained systems to high-throughput scenarios.
 *
 * @since 1.0.0
 */
public interface StreamingOptions {

  /**
   * Gets the buffer size for streaming operations.
   *
   * <p>This determines how much data is read and processed at once during streaming operations.
   * Larger buffers may improve throughput but consume more memory.
   *
   * @return buffer size in bytes
   */
  int getBufferSize();

  /**
   * Checks if progress tracking is enabled for streaming operations.
   *
   * <p>When enabled, streaming operations will emit progress events that can be monitored for user
   * interface updates or operational monitoring.
   *
   * @return true if progress tracking is enabled
   */
  boolean isEnableProgressTracking();

  /**
   * Gets the timeout for streaming operations.
   *
   * <p>This timeout applies to the entire streaming operation. Individual buffer operations may
   * have their own shorter timeouts.
   *
   * @return timeout duration, or null for no timeout
   */
  Duration getTimeout();

  /**
   * Gets the custom executor for streaming operations.
   *
   * <p>When specified, streaming operations will use this executor instead of the default async
   * executor. This allows for custom threading behavior and resource management.
   *
   * @return custom executor, or null to use default
   */
  Executor getExecutor();

  /**
   * Gets the maximum memory usage allowed during streaming operations.
   *
   * <p>This limit helps prevent memory exhaustion during processing of large modules or
   * high-throughput scenarios.
   *
   * @return maximum memory in bytes, or -1 for unlimited
   */
  long getMaxMemoryUsage();

  /**
   * Checks if validation should be skipped during streaming compilation.
   *
   * <p>Skipping validation can improve performance but reduces safety. Only use this when the
   * source is trusted and performance is critical.
   *
   * @return true if validation should be skipped
   */
  boolean isSkipValidation();

  /**
   * Gets the priority level for streaming operations.
   *
   * <p>Higher priority operations may be scheduled before lower priority ones when multiple
   * streaming operations are queued.
   *
   * @return priority level (higher values indicate higher priority)
   */
  int getPriority();

  /**
   * Gets the chunk size for progressive processing.
   *
   * <p>This determines how the stream is divided for progressive processing. Smaller chunks provide
   * more granular progress updates but may reduce efficiency.
   *
   * @return chunk size in bytes
   */
  int getChunkSize();

  /**
   * Checks if parallel processing is enabled for streaming operations.
   *
   * <p>When enabled, different parts of the streaming operation may be processed concurrently to
   * improve performance on multi-core systems.
   *
   * @return true if parallel processing is enabled
   */
  boolean isParallelProcessingEnabled();

  /**
   * Gets the maximum number of concurrent processing threads.
   *
   * <p>This limits the concurrency level for parallel streaming operations to prevent resource
   * exhaustion.
   *
   * @return maximum concurrent threads, or -1 for unlimited
   */
  int getMaxConcurrentThreads();

  /**
   * Checks if compression is enabled for streaming data.
   *
   * <p>When enabled, streaming data may be compressed to reduce I/O overhead, particularly useful
   * for network-based streams.
   *
   * @return true if compression is enabled
   */
  boolean isCompressionEnabled();

  /**
   * Gets the compression level for streaming operations.
   *
   * <p>Higher compression levels reduce data size but increase CPU usage. Only relevant when
   * compression is enabled.
   *
   * @return compression level (0-9), where 0 is no compression and 9 is maximum
   */
  int getCompressionLevel();

  /**
   * Checks if error recovery is enabled for streaming operations.
   *
   * <p>When enabled, streaming operations may attempt to recover from certain types of errors
   * rather than failing immediately.
   *
   * @return true if error recovery is enabled
   */
  boolean isErrorRecoveryEnabled();

  /**
   * Gets the maximum number of recovery attempts for failed operations.
   *
   * <p>This limits the number of times error recovery will be attempted before giving up and
   * reporting failure.
   *
   * @return maximum recovery attempts
   */
  int getMaxRecoveryAttempts();

  /**
   * Gets the delay between recovery attempts.
   *
   * <p>This provides a back-off period between recovery attempts to avoid overwhelming the system
   * or external resources.
   *
   * @return recovery delay duration
   */
  Duration getRecoveryDelay();

  /**
   * Checks if detailed metrics collection is enabled.
   *
   * <p>When enabled, streaming operations collect detailed performance metrics that can be used for
   * monitoring and optimization.
   *
   * @return true if metrics collection is enabled
   */
  boolean isMetricsCollectionEnabled();

  /**
   * Gets the interval for metrics sampling.
   *
   * <p>This determines how frequently metrics are collected during streaming operations. More
   * frequent sampling provides better resolution but uses more resources.
   *
   * @return metrics sampling interval
   */
  Duration getMetricsSamplingInterval();

  /**
   * Creates a new StreamingOptions builder with default values.
   *
   * @return a new StreamingOptions builder
   */
  static Builder builder() {
    return new DefaultStreamingOptionsBuilder();
  }

  /**
   * Creates StreamingOptions with default configuration.
   *
   * @return default streaming options
   */
  static StreamingOptions defaults() {
    return builder().build();
  }

  /** Builder for creating StreamingOptions instances. */
  interface Builder {
    /**
     * Sets the buffer size for streaming operations.
     *
     * @param bufferSize buffer size in bytes
     * @return this builder
     */
    Builder bufferSize(int bufferSize);

    /**
     * Enables or disables progress tracking.
     *
     * @param enableProgressTracking true to enable progress tracking
     * @return this builder
     */
    Builder enableProgressTracking(boolean enableProgressTracking);

    /**
     * Sets the timeout for streaming operations.
     *
     * @param timeout timeout duration
     * @return this builder
     */
    Builder timeout(Duration timeout);

    /**
     * Sets the custom executor for streaming operations.
     *
     * @param executor custom executor
     * @return this builder
     */
    Builder executor(Executor executor);

    /**
     * Sets the maximum memory usage for streaming operations.
     *
     * @param maxMemoryUsage maximum memory in bytes
     * @return this builder
     */
    Builder maxMemoryUsage(long maxMemoryUsage);

    /**
     * Enables or disables validation skipping.
     *
     * @param skipValidation true to skip validation
     * @return this builder
     */
    Builder skipValidation(boolean skipValidation);

    /**
     * Sets the priority level for streaming operations.
     *
     * @param priority priority level
     * @return this builder
     */
    Builder priority(int priority);

    /**
     * Sets the chunk size for progressive processing.
     *
     * @param chunkSize chunk size in bytes
     * @return this builder
     */
    Builder chunkSize(int chunkSize);

    /**
     * Enables or disables parallel processing.
     *
     * @param parallelProcessingEnabled true to enable parallel processing
     * @return this builder
     */
    Builder parallelProcessingEnabled(boolean parallelProcessingEnabled);

    /**
     * Sets the maximum number of concurrent threads.
     *
     * @param maxConcurrentThreads maximum concurrent threads
     * @return this builder
     */
    Builder maxConcurrentThreads(int maxConcurrentThreads);

    /**
     * Enables or disables compression.
     *
     * @param compressionEnabled true to enable compression
     * @return this builder
     */
    Builder compressionEnabled(boolean compressionEnabled);

    /**
     * Sets the compression level.
     *
     * @param compressionLevel compression level (0-9)
     * @return this builder
     */
    Builder compressionLevel(int compressionLevel);

    /**
     * Enables or disables error recovery.
     *
     * @param errorRecoveryEnabled true to enable error recovery
     * @return this builder
     */
    Builder errorRecoveryEnabled(boolean errorRecoveryEnabled);

    /**
     * Sets the maximum number of recovery attempts.
     *
     * @param maxRecoveryAttempts maximum recovery attempts
     * @return this builder
     */
    Builder maxRecoveryAttempts(int maxRecoveryAttempts);

    /**
     * Sets the delay between recovery attempts.
     *
     * @param recoveryDelay recovery delay duration
     * @return this builder
     */
    Builder recoveryDelay(Duration recoveryDelay);

    /**
     * Enables or disables metrics collection.
     *
     * @param metricsCollectionEnabled true to enable metrics collection
     * @return this builder
     */
    Builder metricsCollectionEnabled(boolean metricsCollectionEnabled);

    /**
     * Sets the metrics sampling interval.
     *
     * @param metricsSamplingInterval metrics sampling interval
     * @return this builder
     */
    Builder metricsSamplingInterval(Duration metricsSamplingInterval);

    /**
     * Builds the StreamingOptions instance.
     *
     * @return configured StreamingOptions
     */
    StreamingOptions build();
  }

  /** Default implementation of StreamingOptions.Builder. */
  final class DefaultStreamingOptionsBuilder implements Builder {
    private int bufferSize = 8192; // 8KB default
    private boolean enableProgressTracking = false;
    private Duration timeout = null;
    private Executor executor = null;
    private long maxMemoryUsage = 64 * 1024 * 1024; // 64MB default
    private boolean skipValidation = false;
    private int priority = 0;
    private int chunkSize = 4096; // 4KB default
    private boolean parallelProcessingEnabled = false;
    private int maxConcurrentThreads = Runtime.getRuntime().availableProcessors();
    private boolean compressionEnabled = false;
    private int compressionLevel = 6; // Balanced default
    private boolean errorRecoveryEnabled = true;
    private int maxRecoveryAttempts = 3;
    private Duration recoveryDelay = Duration.ofMillis(1000);
    private boolean metricsCollectionEnabled = false;
    private Duration metricsSamplingInterval = Duration.ofSeconds(1);

    @Override
    public Builder bufferSize(final int bufferSize) {
      this.bufferSize = bufferSize;
      return this;
    }

    @Override
    public Builder enableProgressTracking(final boolean enableProgressTracking) {
      this.enableProgressTracking = enableProgressTracking;
      return this;
    }

    @Override
    public Builder timeout(final Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    @Override
    public Builder executor(final Executor executor) {
      this.executor = executor;
      return this;
    }

    @Override
    public Builder maxMemoryUsage(final long maxMemoryUsage) {
      this.maxMemoryUsage = maxMemoryUsage;
      return this;
    }

    @Override
    public Builder skipValidation(final boolean skipValidation) {
      this.skipValidation = skipValidation;
      return this;
    }

    @Override
    public Builder priority(final int priority) {
      this.priority = priority;
      return this;
    }

    @Override
    public Builder chunkSize(final int chunkSize) {
      this.chunkSize = chunkSize;
      return this;
    }

    @Override
    public Builder parallelProcessingEnabled(final boolean parallelProcessingEnabled) {
      this.parallelProcessingEnabled = parallelProcessingEnabled;
      return this;
    }

    @Override
    public Builder maxConcurrentThreads(final int maxConcurrentThreads) {
      this.maxConcurrentThreads = maxConcurrentThreads;
      return this;
    }

    @Override
    public Builder compressionEnabled(final boolean compressionEnabled) {
      this.compressionEnabled = compressionEnabled;
      return this;
    }

    @Override
    public Builder compressionLevel(final int compressionLevel) {
      this.compressionLevel = compressionLevel;
      return this;
    }

    @Override
    public Builder errorRecoveryEnabled(final boolean errorRecoveryEnabled) {
      this.errorRecoveryEnabled = errorRecoveryEnabled;
      return this;
    }

    @Override
    public Builder maxRecoveryAttempts(final int maxRecoveryAttempts) {
      this.maxRecoveryAttempts = maxRecoveryAttempts;
      return this;
    }

    @Override
    public Builder recoveryDelay(final Duration recoveryDelay) {
      this.recoveryDelay = recoveryDelay;
      return this;
    }

    @Override
    public Builder metricsCollectionEnabled(final boolean metricsCollectionEnabled) {
      this.metricsCollectionEnabled = metricsCollectionEnabled;
      return this;
    }

    @Override
    public Builder metricsSamplingInterval(final Duration metricsSamplingInterval) {
      this.metricsSamplingInterval = metricsSamplingInterval;
      return this;
    }

    @Override
    public StreamingOptions build() {
      return new DefaultStreamingOptions(
          bufferSize,
          enableProgressTracking,
          timeout,
          executor,
          maxMemoryUsage,
          skipValidation,
          priority,
          chunkSize,
          parallelProcessingEnabled,
          maxConcurrentThreads,
          compressionEnabled,
          compressionLevel,
          errorRecoveryEnabled,
          maxRecoveryAttempts,
          recoveryDelay,
          metricsCollectionEnabled,
          metricsSamplingInterval);
    }
  }

  /** Default implementation of StreamingOptions. */
  final class DefaultStreamingOptions implements StreamingOptions {
    private final int bufferSize;
    private final boolean enableProgressTracking;
    private final Duration timeout;
    private final Executor executor;
    private final long maxMemoryUsage;
    private final boolean skipValidation;
    private final int priority;
    private final int chunkSize;
    private final boolean parallelProcessingEnabled;
    private final int maxConcurrentThreads;
    private final boolean compressionEnabled;
    private final int compressionLevel;
    private final boolean errorRecoveryEnabled;
    private final int maxRecoveryAttempts;
    private final Duration recoveryDelay;
    private final boolean metricsCollectionEnabled;
    private final Duration metricsSamplingInterval;

    DefaultStreamingOptions(
        final int bufferSize,
        final boolean enableProgressTracking,
        final Duration timeout,
        final Executor executor,
        final long maxMemoryUsage,
        final boolean skipValidation,
        final int priority,
        final int chunkSize,
        final boolean parallelProcessingEnabled,
        final int maxConcurrentThreads,
        final boolean compressionEnabled,
        final int compressionLevel,
        final boolean errorRecoveryEnabled,
        final int maxRecoveryAttempts,
        final Duration recoveryDelay,
        final boolean metricsCollectionEnabled,
        final Duration metricsSamplingInterval) {
      this.bufferSize = bufferSize;
      this.enableProgressTracking = enableProgressTracking;
      this.timeout = timeout;
      this.executor = executor;
      this.maxMemoryUsage = maxMemoryUsage;
      this.skipValidation = skipValidation;
      this.priority = priority;
      this.chunkSize = chunkSize;
      this.parallelProcessingEnabled = parallelProcessingEnabled;
      this.maxConcurrentThreads = maxConcurrentThreads;
      this.compressionEnabled = compressionEnabled;
      this.compressionLevel = compressionLevel;
      this.errorRecoveryEnabled = errorRecoveryEnabled;
      this.maxRecoveryAttempts = maxRecoveryAttempts;
      this.recoveryDelay = recoveryDelay;
      this.metricsCollectionEnabled = metricsCollectionEnabled;
      this.metricsSamplingInterval = metricsSamplingInterval;
    }

    @Override
    public int getBufferSize() {
      return bufferSize;
    }

    @Override
    public boolean isEnableProgressTracking() {
      return enableProgressTracking;
    }

    @Override
    public Duration getTimeout() {
      return timeout;
    }

    @Override
    public Executor getExecutor() {
      return executor;
    }

    @Override
    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }

    @Override
    public boolean isSkipValidation() {
      return skipValidation;
    }

    @Override
    public int getPriority() {
      return priority;
    }

    @Override
    public int getChunkSize() {
      return chunkSize;
    }

    @Override
    public boolean isParallelProcessingEnabled() {
      return parallelProcessingEnabled;
    }

    @Override
    public int getMaxConcurrentThreads() {
      return maxConcurrentThreads;
    }

    @Override
    public boolean isCompressionEnabled() {
      return compressionEnabled;
    }

    @Override
    public int getCompressionLevel() {
      return compressionLevel;
    }

    @Override
    public boolean isErrorRecoveryEnabled() {
      return errorRecoveryEnabled;
    }

    @Override
    public int getMaxRecoveryAttempts() {
      return maxRecoveryAttempts;
    }

    @Override
    public Duration getRecoveryDelay() {
      return recoveryDelay;
    }

    @Override
    public boolean isMetricsCollectionEnabled() {
      return metricsCollectionEnabled;
    }

    @Override
    public Duration getMetricsSamplingInterval() {
      return metricsSamplingInterval;
    }
  }
}
