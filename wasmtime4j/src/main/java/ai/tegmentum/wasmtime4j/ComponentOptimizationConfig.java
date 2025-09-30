package ai.tegmentum.wasmtime4j;

/**
 * Component optimization configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentOptimizationConfig {

  /**
   * Checks if optimization is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Gets optimization level.
   *
   * @return optimization level
   */
  OptimizationLevel getLevel();

  /**
   * Gets optimization strategies.
   *
   * @return set of strategies
   */
  java.util.Set<OptimizationStrategy> getStrategies();

  /**
   * Gets compilation optimization settings.
   *
   * @return compilation settings
   */
  CompilationOptimization getCompilationOptimization();

  /**
   * Gets runtime optimization settings.
   *
   * @return runtime settings
   */
  RuntimeOptimization getRuntimeOptimization();

  /**
   * Gets memory optimization settings.
   *
   * @return memory settings
   */
  MemoryOptimization getMemoryOptimization();

  /**
   * Gets performance tuning settings.
   *
   * @return performance settings
   */
  PerformanceTuning getPerformanceTuning();

  /**
   * Gets optimization metrics collection settings.
   *
   * @return metrics settings
   */
  OptimizationMetrics getMetrics();

  /** Compilation optimization interface. */
  interface CompilationOptimization {
    /**
     * Checks if dead code elimination is enabled.
     *
     * @return true if enabled
     */
    boolean isDeadCodeEliminationEnabled();

    /**
     * Checks if function inlining is enabled.
     *
     * @return true if enabled
     */
    boolean isInliningEnabled();

    /**
     * Gets inlining threshold.
     *
     * @return inlining threshold
     */
    int getInliningThreshold();

    /**
     * Checks if loop optimization is enabled.
     *
     * @return true if enabled
     */
    boolean isLoopOptimizationEnabled();

    /**
     * Checks if vectorization is enabled.
     *
     * @return true if enabled
     */
    boolean isVectorizationEnabled();

    /**
     * Gets optimization passes.
     *
     * @return optimization passes
     */
    java.util.List<OptimizationPass> getPasses();
  }

  /** Runtime optimization interface. */
  interface RuntimeOptimization {
    /**
     * Checks if JIT compilation is enabled.
     *
     * @return true if enabled
     */
    boolean isJitEnabled();

    /**
     * Gets JIT threshold.
     *
     * @return JIT threshold
     */
    int getJitThreshold();

    /**
     * Checks if profile-guided optimization is enabled.
     *
     * @return true if enabled
     */
    boolean isPgoEnabled();

    /**
     * Gets PGO collection window.
     *
     * @return collection window in milliseconds
     */
    long getPgoWindow();

    /**
     * Checks if adaptive optimization is enabled.
     *
     * @return true if enabled
     */
    boolean isAdaptiveOptimizationEnabled();

    /**
     * Gets optimization frequency.
     *
     * @return optimization frequency in milliseconds
     */
    long getOptimizationFrequency();
  }

  /** Memory optimization interface. */
  interface MemoryOptimization {
    /**
     * Checks if memory pooling is enabled.
     *
     * @return true if enabled
     */
    boolean isPoolingEnabled();

    /**
     * Gets pool sizes.
     *
     * @return pool sizes
     */
    java.util.Map<String, Integer> getPoolSizes();

    /**
     * Checks if memory compression is enabled.
     *
     * @return true if enabled
     */
    boolean isCompressionEnabled();

    /**
     * Gets compression algorithm.
     *
     * @return compression algorithm
     */
    String getCompressionAlgorithm();

    /**
     * Checks if garbage collection tuning is enabled.
     *
     * @return true if enabled
     */
    boolean isGcTuningEnabled();

    /**
     * Gets GC parameters.
     *
     * @return GC parameters
     */
    java.util.Map<String, Object> getGcParameters();
  }

  /** Performance tuning interface. */
  interface PerformanceTuning {
    /**
     * Gets thread pool size.
     *
     * @return thread pool size
     */
    int getThreadPoolSize();

    /**
     * Gets queue capacity.
     *
     * @return queue capacity
     */
    int getQueueCapacity();

    /**
     * Gets timeout values.
     *
     * @return timeout values
     */
    TimeoutConfig getTimeouts();

    /**
     * Gets caching configuration.
     *
     * @return caching configuration
     */
    CacheConfig getCaching();

    /**
     * Gets prefetching settings.
     *
     * @return prefetching settings
     */
    PrefetchConfig getPrefetching();
  }

  /** Timeout configuration interface. */
  interface TimeoutConfig {
    /**
     * Gets execution timeout.
     *
     * @return timeout in milliseconds
     */
    long getExecutionTimeout();

    /**
     * Gets compilation timeout.
     *
     * @return timeout in milliseconds
     */
    long getCompilationTimeout();

    /**
     * Gets initialization timeout.
     *
     * @return timeout in milliseconds
     */
    long getInitializationTimeout();
  }

  /** Cache configuration interface. */
  interface CacheConfig {
    /**
     * Gets cache size.
     *
     * @return cache size
     */
    int getSize();

    /**
     * Gets cache TTL.
     *
     * @return TTL in milliseconds
     */
    long getTtl();

    /**
     * Gets eviction policy.
     *
     * @return eviction policy
     */
    EvictionPolicy getEvictionPolicy();

    /**
     * Checks if cache is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();
  }

  /** Prefetch configuration interface. */
  interface PrefetchConfig {
    /**
     * Checks if prefetching is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets prefetch depth.
     *
     * @return prefetch depth
     */
    int getDepth();

    /**
     * Gets prefetch strategies.
     *
     * @return prefetch strategies
     */
    java.util.Set<PrefetchStrategy> getStrategies();
  }

  /** Optimization metrics interface. */
  interface OptimizationMetrics {
    /**
     * Checks if metrics collection is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets collection interval.
     *
     * @return interval in milliseconds
     */
    long getInterval();

    /**
     * Gets metrics to collect.
     *
     * @return metrics set
     */
    java.util.Set<MetricType> getMetrics();

    /**
     * Gets retention period.
     *
     * @return retention in milliseconds
     */
    long getRetention();
  }

  /** Optimization pass interface. */
  interface OptimizationPass {
    /**
     * Gets pass name.
     *
     * @return pass name
     */
    String getName();

    /**
     * Gets pass order.
     *
     * @return execution order
     */
    int getOrder();

    /**
     * Checks if pass is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets pass parameters.
     *
     * @return parameters
     */
    java.util.Map<String, Object> getParameters();
  }

  /** Optimization level enumeration. */
  enum OptimizationLevel {
    /** No optimization. */
    NONE,
    /** Basic optimization. */
    BASIC,
    /** Standard optimization. */
    STANDARD,
    /** Aggressive optimization. */
    AGGRESSIVE,
    /** Maximum optimization. */
    MAXIMUM
  }

  /** Optimization strategy enumeration. */
  enum OptimizationStrategy {
    /** Speed optimization. */
    SPEED,
    /** Size optimization. */
    SIZE,
    /** Memory optimization. */
    MEMORY,
    /** Power optimization. */
    POWER,
    /** Balanced optimization. */
    BALANCED
  }

  /** Eviction policy enumeration. */
  enum EvictionPolicy {
    /** Least Recently Used. */
    LRU,
    /** Least Frequently Used. */
    LFU,
    /** First In First Out. */
    FIFO,
    /** Random. */
    RANDOM
  }

  /** Prefetch strategy enumeration. */
  enum PrefetchStrategy {
    /** Sequential prefetch. */
    SEQUENTIAL,
    /** Predictive prefetch. */
    PREDICTIVE,
    /** Adaptive prefetch. */
    ADAPTIVE,
    /** Historical prefetch. */
    HISTORICAL
  }

  /** Metric type enumeration. */
  enum MetricType {
    /** Execution time. */
    EXECUTION_TIME,
    /** Memory usage. */
    MEMORY_USAGE,
    /** Cache hit rate. */
    CACHE_HIT_RATE,
    /** Optimization effectiveness. */
    OPTIMIZATION_EFFECTIVENESS,
    /** Throughput. */
    THROUGHPUT
  }
}
