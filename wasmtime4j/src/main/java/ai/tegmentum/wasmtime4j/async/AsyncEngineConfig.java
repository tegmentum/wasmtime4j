package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Configuration options for asynchronous WebAssembly engine operations.
 *
 * <p>This class provides settings for controlling async compilation behavior, thread pool
 * configuration, and resource limits for parallel operations.
 *
 * @since 1.0.0
 */
public final class AsyncEngineConfig {

  private int maxParallelThreads = Runtime.getRuntime().availableProcessors();
  private Duration compilationTimeout = Duration.ofMinutes(5);
  private boolean enableProgressTracking = true;
  private Duration progressUpdateInterval = Duration.ofMillis(100);
  private Executor defaultExecutor = ForkJoinPool.commonPool();
  private int maxQueuedCompilations = 100;
  private boolean enableCompilationCaching = true;
  private long maxCacheSize = 100L * 1024 * 1024; // 100 MB
  private boolean enableBatchCompilation = true;
  private int batchSize = 4;
  private boolean enableResourceMonitoring = true;
  private long maxMemoryUsage = 512L * 1024 * 1024; // 512 MB
  private boolean enableAdaptiveThreading = true;
  private double threadScalingFactor = 1.5;
  private boolean enableCompilationStatistics = true;
  private boolean enableCancellation = true;

  /** Creates a new async engine configuration with default settings. */
  public AsyncEngineConfig() {
    // Default configuration
  }

  /**
   * Sets the maximum number of parallel compilation threads.
   *
   * @param threads maximum parallel threads (0 for unlimited)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if threads is negative
   */
  public AsyncEngineConfig setMaxParallelThreads(final int threads) {
    if (threads < 0) {
      throw new IllegalArgumentException("Maximum parallel threads cannot be negative");
    }
    this.maxParallelThreads = threads == 0 ? Integer.MAX_VALUE : threads;
    return this;
  }

  /**
   * Sets the compilation timeout.
   *
   * @param timeout maximum time to wait for compilation
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  public AsyncEngineConfig setCompilationTimeout(final Duration timeout) {
    if (timeout == null || timeout.isNegative()) {
      throw new IllegalArgumentException("Compilation timeout must be positive");
    }
    this.compilationTimeout = timeout;
    return this;
  }

  /**
   * Enables or disables progress tracking for async operations.
   *
   * @param enabled true to enable progress tracking
   * @return this configuration for method chaining
   */
  public AsyncEngineConfig setProgressTracking(final boolean enabled) {
    this.enableProgressTracking = enabled;
    return this;
  }

  /**
   * Sets the progress update interval.
   *
   * @param interval how often to update progress
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if interval is null or negative
   */
  public AsyncEngineConfig setProgressUpdateInterval(final Duration interval) {
    if (interval == null || interval.isNegative()) {
      throw new IllegalArgumentException("Progress update interval must be positive");
    }
    this.progressUpdateInterval = interval;
    return this;
  }

  /**
   * Sets the default executor for async operations.
   *
   * @param executor the executor to use
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if executor is null
   */
  public AsyncEngineConfig setDefaultExecutor(final Executor executor) {
    if (executor == null) {
      throw new IllegalArgumentException("Default executor cannot be null");
    }
    this.defaultExecutor = executor;
    return this;
  }

  /**
   * Sets the maximum number of queued compilations.
   *
   * @param maxQueued maximum queued compilations
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if maxQueued is negative
   */
  public AsyncEngineConfig setMaxQueuedCompilations(final int maxQueued) {
    if (maxQueued < 0) {
      throw new IllegalArgumentException("Maximum queued compilations cannot be negative");
    }
    this.maxQueuedCompilations = maxQueued;
    return this;
  }

  /**
   * Enables or disables compilation result caching.
   *
   * @param enabled true to enable caching
   * @return this configuration for method chaining
   */
  public AsyncEngineConfig setCompilationCaching(final boolean enabled) {
    this.enableCompilationCaching = enabled;
    return this;
  }

  /**
   * Sets the maximum cache size in bytes.
   *
   * @param sizeBytes maximum cache size
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if sizeBytes is negative
   */
  public AsyncEngineConfig setMaxCacheSize(final long sizeBytes) {
    if (sizeBytes < 0) {
      throw new IllegalArgumentException("Maximum cache size cannot be negative");
    }
    this.maxCacheSize = sizeBytes;
    return this;
  }

  /**
   * Enables or disables batch compilation for multiple modules.
   *
   * @param enabled true to enable batch compilation
   * @return this configuration for method chaining
   */
  public AsyncEngineConfig setBatchCompilation(final boolean enabled) {
    this.enableBatchCompilation = enabled;
    return this;
  }

  /**
   * Sets the batch size for batch compilation.
   *
   * @param size number of modules per batch
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if size is less than 1
   */
  public AsyncEngineConfig setBatchSize(final int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Batch size must be at least 1");
    }
    this.batchSize = size;
    return this;
  }

  /**
   * Enables or disables resource monitoring during compilation.
   *
   * @param enabled true to enable resource monitoring
   * @return this configuration for method chaining
   */
  public AsyncEngineConfig setResourceMonitoring(final boolean enabled) {
    this.enableResourceMonitoring = enabled;
    return this;
  }

  /**
   * Sets the maximum memory usage for async operations.
   *
   * @param bytes maximum memory usage in bytes
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if bytes is negative
   */
  public AsyncEngineConfig setMaxMemoryUsage(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Maximum memory usage cannot be negative");
    }
    this.maxMemoryUsage = bytes;
    return this;
  }

  /**
   * Enables or disables adaptive threading based on system load.
   *
   * @param enabled true to enable adaptive threading
   * @return this configuration for method chaining
   */
  public AsyncEngineConfig setAdaptiveThreading(final boolean enabled) {
    this.enableAdaptiveThreading = enabled;
    return this;
  }

  /**
   * Sets the thread scaling factor for adaptive threading.
   *
   * @param factor scaling factor (1.0 = no scaling)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if factor is not positive
   */
  public AsyncEngineConfig setThreadScalingFactor(final double factor) {
    if (factor <= 0.0) {
      throw new IllegalArgumentException("Thread scaling factor must be positive");
    }
    this.threadScalingFactor = factor;
    return this;
  }

  /**
   * Enables or disables compilation statistics collection.
   *
   * @param enabled true to enable statistics
   * @return this configuration for method chaining
   */
  public AsyncEngineConfig setCompilationStatistics(final boolean enabled) {
    this.enableCompilationStatistics = enabled;
    return this;
  }

  /**
   * Enables or disables cancellation support.
   *
   * @param enabled true to enable cancellation
   * @return this configuration for method chaining
   */
  public AsyncEngineConfig setCancellation(final boolean enabled) {
    this.enableCancellation = enabled;
    return this;
  }

  // Getters

  public int getMaxParallelThreads() {
    return maxParallelThreads;
  }

  public Duration getCompilationTimeout() {
    return compilationTimeout;
  }

  public boolean isProgressTrackingEnabled() {
    return enableProgressTracking;
  }

  public Duration getProgressUpdateInterval() {
    return progressUpdateInterval;
  }

  public Executor getDefaultExecutor() {
    return defaultExecutor;
  }

  public int getMaxQueuedCompilations() {
    return maxQueuedCompilations;
  }

  public boolean isCompilationCachingEnabled() {
    return enableCompilationCaching;
  }

  public long getMaxCacheSize() {
    return maxCacheSize;
  }

  public boolean isBatchCompilationEnabled() {
    return enableBatchCompilation;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public boolean isResourceMonitoringEnabled() {
    return enableResourceMonitoring;
  }

  public long getMaxMemoryUsage() {
    return maxMemoryUsage;
  }

  public boolean isAdaptiveThreadingEnabled() {
    return enableAdaptiveThreading;
  }

  public double getThreadScalingFactor() {
    return threadScalingFactor;
  }

  public boolean isCompilationStatisticsEnabled() {
    return enableCompilationStatistics;
  }

  public boolean isCancellationEnabled() {
    return enableCancellation;
  }

  /**
   * Creates a default async engine configuration.
   *
   * @return default configuration
   */
  public static AsyncEngineConfig defaultConfig() {
    return new AsyncEngineConfig();
  }

  /**
   * Creates a high-performance async configuration.
   *
   * @return high-performance configuration
   */
  public static AsyncEngineConfig highPerformance() {
    return new AsyncEngineConfig()
        .setMaxParallelThreads(Runtime.getRuntime().availableProcessors() * 2)
        .setBatchCompilation(true)
        .setBatchSize(8)
        .setAdaptiveThreading(true)
        .setCompilationCaching(true)
        .setMaxCacheSize(256L * 1024 * 1024); // 256 MB
  }

  /**
   * Creates a low-resource async configuration.
   *
   * @return low-resource configuration
   */
  public static AsyncEngineConfig lowResource() {
    return new AsyncEngineConfig()
        .setMaxParallelThreads(2)
        .setBatchCompilation(false)
        .setProgressTracking(false)
        .setResourceMonitoring(false)
        .setCompilationCaching(false)
        .setMaxMemoryUsage(64L * 1024 * 1024); // 64 MB
  }

  @Override
  public String toString() {
    return String.format(
        "AsyncEngineConfig{threads=%d, timeout=%s, caching=%s, batching=%s}",
        maxParallelThreads, compilationTimeout, enableCompilationCaching, enableBatchCompilation);
  }
}
