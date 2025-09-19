package ai.tegmentum.wasmtime4j;

/**
 * Runtime statistics for a WebAssembly engine.
 *
 * <p>This interface provides access to various metrics about the engine's runtime behavior,
 * including compilation statistics, cache performance, and memory usage. These statistics can be
 * useful for performance monitoring and optimization.
 *
 * @since 1.0.0
 */
public interface EngineStatistics {

  /**
   * Gets the number of WebAssembly modules compiled by this engine.
   *
   * <p>This count includes both successful and failed compilation attempts. It reflects the total
   * workload processed by the engine since creation.
   *
   * @return the number of compiled modules
   */
  long getCompiledModuleCount();

  /**
   * Gets the number of compilation cache hits.
   *
   * <p>Cache hits occur when a previously compiled module can be reused without recompilation,
   * improving performance. A higher hit rate indicates better cache utilization.
   *
   * @return the number of cache hits
   */
  long getCacheHits();

  /**
   * Gets the number of compilation cache misses.
   *
   * <p>Cache misses occur when a module must be compiled from scratch because no cached version
   * is available. This includes first-time compilations and cache evictions.
   *
   * @return the number of cache misses
   */
  long getCacheMisses();

  /**
   * Gets the current memory usage of the engine in bytes.
   *
   * <p>This includes memory used for compiled code, metadata, and internal data structures. The
   * value may fluctuate as modules are compiled and garbage collected.
   *
   * @return the current memory usage in bytes
   */
  long getMemoryUsage();

  /**
   * Gets the peak memory usage of the engine in bytes.
   *
   * <p>This represents the highest memory usage recorded since the engine was created. It can be
   * useful for capacity planning and memory optimization.
   *
   * @return the peak memory usage in bytes
   */
  long getPeakMemoryUsage();

  /**
   * Gets the total compilation time in milliseconds.
   *
   * <p>This represents the cumulative time spent compiling WebAssembly modules, including both
   * successful and failed attempts. It can be used to measure compilation performance.
   *
   * @return the total compilation time in milliseconds
   */
  long getTotalCompilationTimeMs();

  /**
   * Gets the cache hit rate as a percentage.
   *
   * <p>This is calculated as (cache hits / (cache hits + cache misses)) * 100. A higher
   * percentage indicates better cache performance.
   *
   * @return the cache hit rate as a percentage (0.0 to 100.0)
   */
  default double getCacheHitRate() {
    long hits = getCacheHits();
    long misses = getCacheMisses();
    long total = hits + misses;

    if (total == 0) {
      return 0.0;
    }

    return (double) hits / total * 100.0;
  }

  /**
   * Gets the average compilation time per module in milliseconds.
   *
   * <p>This is calculated as total compilation time divided by the number of compiled modules.
   * It provides insight into compilation performance characteristics.
   *
   * @return the average compilation time per module in milliseconds
   */
  default double getAverageCompilationTimeMs() {
    long count = getCompiledModuleCount();
    if (count == 0) {
      return 0.0;
    }

    return (double) getTotalCompilationTimeMs() / count;
  }
}