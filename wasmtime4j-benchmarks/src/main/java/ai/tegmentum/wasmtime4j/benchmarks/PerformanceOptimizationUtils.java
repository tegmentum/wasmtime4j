/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.benchmarks;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Performance optimization utilities implementing caching strategies, object pooling, and batching
 * patterns identified through benchmark analysis.
 *
 * <p>This class provides optimized implementations for common performance bottlenecks in
 * WebAssembly operations:
 *
 * <ul>
 *   <li>Buffer pooling to reduce memory allocation pressure
 *   <li>Caching strategies for expensive operations
 *   <li>Batching utilities for reducing native call overhead
 *   <li>GC-resistant operation patterns
 *   <li>Performance monitoring and metrics collection
 * </ul>
 *
 * <p>Based on analysis of existing benchmarks, the key optimization patterns are:
 *
 * <ul>
 *   <li>Buffer reuse to minimize allocations (seen in MemoryOperationBenchmark)
 *   <li>Batch operations to reduce call overhead (identified across multiple benchmarks)
 *   <li>Caching for repeated operations (NativeLoaderComparisonBenchmark)
 *   <li>Thread pooling for concurrent operations (ConcurrencyBenchmark)
 *   <li>Memory pressure reduction (PerformanceOptimizationBenchmark)
 * </ul>
 */
public final class PerformanceOptimizationUtils {

  /** Logger for performance optimization operations. */
  private static final Logger LOGGER =
      Logger.getLogger(PerformanceOptimizationUtils.class.getName());

  /** Maximum buffer size for pooling (1MB). */
  private static final int MAX_BUFFER_SIZE = 1024 * 1024;

  /** Maximum pool size to prevent memory leaks. */
  private static final int MAX_POOL_SIZE = 100;

  /** Cache size limits for different operation types. */
  private static final int COMPILATION_CACHE_SIZE = 50;

  private static final int INSTANCE_CACHE_SIZE = 20;

  /** Performance metrics tracking. */
  private static final AtomicLong CACHE_HITS = new AtomicLong(0);

  private static final AtomicLong CACHE_MISSES = new AtomicLong(0);
  private static final AtomicLong BUFFER_POOL_HITS = new AtomicLong(0);
  private static final AtomicLong BUFFER_POOL_MISSES = new AtomicLong(0);

  private PerformanceOptimizationUtils() {
    // Utility class
  }

  /**
   * Buffer pool for reusing byte arrays to reduce GC pressure.
   *
   * <p>Implementation uses SoftReference to allow GC under memory pressure while maintaining
   * performance benefits under normal conditions.
   */
  public static final class BufferPool {
    private final ConcurrentMap<Integer, SoftReference<byte[]>> pooledBuffers =
        new ConcurrentHashMap<>();
    private final AtomicInteger poolSize = new AtomicInteger(0);

    /**
     * Gets a buffer of the specified size, reusing pooled buffers when available.
     *
     * @param size the required buffer size
     * @return a byte array of the requested size
     */
    public byte[] getBuffer(final int size) {
      if (size <= 0 || size > MAX_BUFFER_SIZE) {
        BUFFER_POOL_MISSES.incrementAndGet();
        return new byte[size];
      }

      // Try to get from pool
      final SoftReference<byte[]> ref = pooledBuffers.get(size);
      if (ref != null) {
        final byte[] buffer = ref.get();
        if (buffer != null) {
          pooledBuffers.remove(size, ref);
          poolSize.decrementAndGet();
          BUFFER_POOL_HITS.incrementAndGet();
          return buffer;
        } else {
          // Reference was cleared, remove it
          pooledBuffers.remove(size, ref);
          poolSize.decrementAndGet();
        }
      }

      BUFFER_POOL_MISSES.incrementAndGet();
      return new byte[size];
    }

    /**
     * Returns a buffer to the pool for reuse.
     *
     * @param buffer the buffer to return
     */
    public void returnBuffer(final byte[] buffer) {
      if (buffer == null || buffer.length <= 0 || buffer.length > MAX_BUFFER_SIZE) {
        return;
      }

      if (poolSize.get() < MAX_POOL_SIZE) {
        pooledBuffers.put(buffer.length, new SoftReference<>(buffer));
        poolSize.incrementAndGet();
      }
    }

    /** Clears the buffer pool. */
    public void clear() {
      pooledBuffers.clear();
      poolSize.set(0);
    }

    /**
     * Gets current pool utilization statistics.
     *
     * @return pool stats string
     */
    public String getStats() {
      return String.format(
          "BufferPool{size=%d, unique_sizes=%d}", poolSize.get(), pooledBuffers.size());
    }
  }

  /**
   * Generic cache implementation for expensive operations.
   *
   * @param <K> key type
   * @param <V> value type
   */
  public static final class OperationCache<K, V> {
    private final ConcurrentMap<K, SoftReference<V>> cache = new ConcurrentHashMap<>();
    private final int maxSize;
    private final AtomicInteger currentSize = new AtomicInteger(0);

    /**
     * Creates an operation cache with the specified maximum size.
     *
     * @param maxSize maximum number of cached entries
     */
    public OperationCache(final int maxSize) {
      this.maxSize = maxSize;
    }

    /**
     * Gets a cached value or computes it using the provided supplier.
     *
     * @param key the cache key
     * @param supplier the value supplier for cache misses
     * @return the cached or computed value
     */
    public V get(final K key, final Supplier<V> supplier) {
      // Try cache first
      final SoftReference<V> ref = cache.get(key);
      if (ref != null) {
        final V value = ref.get();
        if (value != null) {
          CACHE_HITS.incrementAndGet();
          return value;
        } else {
          // Reference was cleared, remove it
          cache.remove(key, ref);
          currentSize.decrementAndGet();
        }
      }

      // Cache miss - compute value
      CACHE_MISSES.incrementAndGet();
      final V value = supplier.get();

      // Add to cache if there's space
      if (currentSize.get() < maxSize) {
        cache.put(key, new SoftReference<>(value));
        currentSize.incrementAndGet();
      } else {
        // Evict a random entry to make space
        evictRandomEntry();
        cache.put(key, new SoftReference<>(value));
        currentSize.incrementAndGet();
      }

      return value;
    }

    /** Clears the cache. */
    public void clear() {
      cache.clear();
      currentSize.set(0);
    }

    private void evictRandomEntry() {
      if (cache.isEmpty()) {
        return;
      }

      final Object[] keys = cache.keySet().toArray();
      if (keys.length > 0) {
        final int randomIndex = ThreadLocalRandom.current().nextInt(keys.length);
        @SuppressWarnings("unchecked")
        final K keyToEvict = (K) keys[randomIndex];
        if (cache.remove(keyToEvict) != null) {
          currentSize.decrementAndGet();
        }
      }
    }
  }

  /** Batch operation utilities for reducing native call overhead. */
  public static final class BatchOperations {

    /** Optimal batch size based on analysis of benchmark patterns. */
    public static final int OPTIMAL_BATCH_SIZE = 10;

    /**
     * Executes operations in batches to reduce overhead.
     *
     * @param operations the operations to execute
     * @param batchProcessor the batch processor function
     * @param <T> operation type
     * @param <R> result type
     * @return combined results
     */
    public static <T, R> R executeBatched(
        final T[] operations, final java.util.function.Function<T[], R> batchProcessor) {
      if (operations == null || operations.length == 0) {
        return null;
      }

      if (operations.length <= OPTIMAL_BATCH_SIZE) {
        return batchProcessor.apply(operations);
      }

      // Process in optimal batch sizes
      R lastResult = null;
      for (int i = 0; i < operations.length; i += OPTIMAL_BATCH_SIZE) {
        final int endIndex = Math.min(i + OPTIMAL_BATCH_SIZE, operations.length);
        final T[] batch = java.util.Arrays.copyOfRange(operations, i, endIndex);
        lastResult = batchProcessor.apply(batch);
      }

      return lastResult;
    }
  }

  /** GC-resistant operation patterns. */
  public static final class GcResistantOperations {

    /**
     * Performs memory operations with minimal allocation.
     *
     * @param bufferPool the buffer pool to use
     * @param operations the number of operations to perform
     * @param operationProcessor the operation processor
     */
    public static void performMemoryOperations(
        final BufferPool bufferPool,
        final int operations,
        final java.util.function.Consumer<byte[]> operationProcessor) {

      // Strategy 1: Reuse single buffer for multiple operations
      final byte[] reusableBuffer = bufferPool.getBuffer(1024);
      try {
        for (int i = 0; i < operations; i++) {
          // Modify buffer in place
          for (int j = 0; j < reusableBuffer.length; j++) {
            reusableBuffer[j] = (byte) ((i + j) % 256);
          }

          operationProcessor.accept(reusableBuffer);
        }
      } finally {
        bufferPool.returnBuffer(reusableBuffer);
      }
    }
  }

  // Global instances for benchmarks
  private static final BufferPool GLOBAL_BUFFER_POOL = new BufferPool();
  private static final OperationCache<String, Object> COMPILATION_CACHE =
      new OperationCache<>(COMPILATION_CACHE_SIZE);
  private static final OperationCache<String, Object> INSTANCE_CACHE =
      new OperationCache<>(INSTANCE_CACHE_SIZE);

  /**
   * Gets the global buffer pool instance.
   *
   * @return the global buffer pool
   */
  public static BufferPool getGlobalBufferPool() {
    return GLOBAL_BUFFER_POOL;
  }

  /**
   * Gets the global compilation cache.
   *
   * @return the compilation cache
   */
  public static OperationCache<String, Object> getCompilationCache() {
    return COMPILATION_CACHE;
  }

  /**
   * Gets the global instance cache.
   *
   * @return the instance cache
   */
  public static OperationCache<String, Object> getInstanceCache() {
    return INSTANCE_CACHE;
  }

  /** Clears all global caches and pools. */
  public static void clearAllCaches() {
    GLOBAL_BUFFER_POOL.clear();
    COMPILATION_CACHE.clear();
    INSTANCE_CACHE.clear();
    CACHE_HITS.set(0);
    CACHE_MISSES.set(0);
    BUFFER_POOL_HITS.set(0);
    BUFFER_POOL_MISSES.set(0);
    LOGGER.info("All performance optimization caches cleared");
  }

  /**
   * Gets comprehensive performance statistics.
   *
   * @return performance statistics string
   */
  public static String getPerformanceStatistics() {
    final long totalCacheOps = CACHE_HITS.get() + CACHE_MISSES.get();
    final long totalBufferOps = BUFFER_POOL_HITS.get() + BUFFER_POOL_MISSES.get();

    final double cacheHitRate = totalCacheOps > 0 ? (double) CACHE_HITS.get() / totalCacheOps : 0.0;
    final double bufferHitRate =
        totalBufferOps > 0 ? (double) BUFFER_POOL_HITS.get() / totalBufferOps : 0.0;

    return String.format(
        "Performance Statistics:\n"
            + "  Cache Operations: %d (%.1f%% hit rate)\n"
            + "  Buffer Pool Operations: %d (%.1f%% hit rate)\n"
            + "  %s\n"
            + "  Compilation Cache: %d/%d entries\n"
            + "  Instance Cache: %d/%d entries",
        totalCacheOps,
        cacheHitRate * 100,
        totalBufferOps,
        bufferHitRate * 100,
        GLOBAL_BUFFER_POOL.getStats(),
        COMPILATION_CACHE.currentSize.get(),
        COMPILATION_CACHE.maxSize,
        INSTANCE_CACHE.currentSize.get(),
        INSTANCE_CACHE.maxSize);
  }

  /**
   * Performance optimization recommendations based on current statistics.
   *
   * @return optimization recommendations
   */
  public static String getOptimizationRecommendations() {
    final long totalCacheOps = CACHE_HITS.get() + CACHE_MISSES.get();
    final long totalBufferOps = BUFFER_POOL_HITS.get() + BUFFER_POOL_MISSES.get();

    final double cacheHitRate = totalCacheOps > 0 ? (double) CACHE_HITS.get() / totalCacheOps : 0.0;
    final double bufferHitRate =
        totalBufferOps > 0 ? (double) BUFFER_POOL_HITS.get() / totalBufferOps : 0.0;

    final StringBuilder recommendations = new StringBuilder();
    recommendations.append("Performance Optimization Recommendations:\n");

    if (cacheHitRate < 0.5) {
      recommendations
          .append("  ⚠️  Low cache hit rate (")
          .append(String.format("%.1f%%", cacheHitRate * 100))
          .append(") - Consider increasing cache sizes or improving cache key patterns\n");
    } else if (cacheHitRate > 0.8) {
      recommendations
          .append("  ✅ Excellent cache hit rate (")
          .append(String.format("%.1f%%", cacheHitRate * 100))
          .append(")\n");
    }

    if (bufferHitRate < 0.3) {
      recommendations
          .append("  ⚠️  Low buffer pool hit rate (")
          .append(String.format("%.1f%%", bufferHitRate * 100))
          .append(") - Consider using more consistent buffer sizes\n");
    } else if (bufferHitRate > 0.6) {
      recommendations
          .append("  ✅ Good buffer pool hit rate (")
          .append(String.format("%.1f%%", bufferHitRate * 100))
          .append(")\n");
    }

    if (COMPILATION_CACHE.currentSize.get() == COMPILATION_CACHE.maxSize) {
      recommendations.append("  ⚠️  Compilation cache is full - consider increasing size\n");
    }

    if (INSTANCE_CACHE.currentSize.get() == INSTANCE_CACHE.maxSize) {
      recommendations.append("  ⚠️  Instance cache is full - consider increasing size\n");
    }

    if (recommendations.length() == "Performance Optimization Recommendations:\n".length()) {
      recommendations.append("  ✅ All performance metrics are within optimal ranges\n");
    }

    return recommendations.toString();
  }
}
