/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Method handle cache for optimized repeated function calls.
 *
 * <p>This class provides a thread-safe cache for MethodHandle instances to optimize repeated FFI
 * calls. It handles function lookup, caching, and invalidation while providing comprehensive
 * statistics and monitoring capabilities.
 *
 * <p>The cache is designed to minimize overhead for frequent FFI operations by eliminating repeated
 * symbol lookup and method handle creation costs.
 */
public final class MethodHandleCache {

  private static final Logger LOGGER = Logger.getLogger(MethodHandleCache.class.getName());

  // Cache key separator
  private static final String KEY_SEPARATOR = "::";

  // Default cache settings
  private static final int DEFAULT_MAX_CACHE_SIZE = 1000;
  private static final boolean DEFAULT_STATISTICS_ENABLED = true;

  // Cache storage
  private final ConcurrentHashMap<String, CachedMethodHandle> cache;
  private final int maxCacheSize;
  private final boolean statisticsEnabled;

  // Statistics tracking
  private final AtomicLong hitCount = new AtomicLong(0);
  private final AtomicLong missCount = new AtomicLong(0);
  private final AtomicLong evictionCount = new AtomicLong(0);
  private final AtomicLong loadTime = new AtomicLong(0);

  /** Creates a new method handle cache with default settings. */
  public MethodHandleCache() {
    this(DEFAULT_MAX_CACHE_SIZE, DEFAULT_STATISTICS_ENABLED);
  }

  /**
   * Creates a new method handle cache with specified settings.
   *
   * @param maxCacheSize the maximum number of cached method handles
   * @param statisticsEnabled whether to collect cache statistics
   */
  public MethodHandleCache(final int maxCacheSize, final boolean statisticsEnabled) {
    if (maxCacheSize <= 0) {
      throw new IllegalArgumentException("Max cache size must be positive: " + maxCacheSize);
    }

    this.maxCacheSize = maxCacheSize;
    this.statisticsEnabled = statisticsEnabled;
    this.cache = new ConcurrentHashMap<>();

    LOGGER.fine(
        "Initialized MethodHandleCache with max size: "
            + maxCacheSize
            + ", statistics: "
            + statisticsEnabled);
  }

  /**
   * Gets or creates a method handle for the specified function.
   *
   * @param functionName the name of the function
   * @param symbol the memory segment pointing to the function symbol
   * @param descriptor the function descriptor defining the signature
   * @return optional containing the method handle, or empty if creation failed
   */
  public Optional<MethodHandle> getOrCreate(
      final String functionName, final MemorySegment symbol, final FunctionDescriptor descriptor) {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    Objects.requireNonNull(symbol, "Symbol cannot be null");
    Objects.requireNonNull(descriptor, "Function descriptor cannot be null");

    final String cacheKey = createCacheKey(functionName, descriptor);
    final long startTime = statisticsEnabled ? System.nanoTime() : 0;

    try {
      // Try to get from cache first
      CachedMethodHandle cached = cache.get(cacheKey);
      if (cached != null && cached.isValid()) {
        if (statisticsEnabled) {
          hitCount.incrementAndGet();
        }
        LOGGER.finest("Cache hit for function: " + functionName);
        return Optional.of(cached.getMethodHandle());
      }

      // Cache miss - create new method handle
      if (statisticsEnabled) {
        missCount.incrementAndGet();
      }
      LOGGER.finest("Cache miss for function: " + functionName);

      Optional<MethodHandle> methodHandle = createMethodHandle(symbol, descriptor);
      if (methodHandle.isPresent()) {
        // Cache the new method handle
        CachedMethodHandle cachedHandle =
            new CachedMethodHandle(methodHandle.get(), symbol, descriptor);

        // Check cache size and evict if necessary
        if (cache.size() >= maxCacheSize) {
          evictLeastRecentlyUsed();
        }

        cache.put(cacheKey, cachedHandle);
        LOGGER.fine("Cached method handle for function: " + functionName);

        return methodHandle;
      } else {
        LOGGER.warning("Failed to create method handle for function: " + functionName);
        return Optional.empty();
      }
    } finally {
      if (statisticsEnabled) {
        long duration = System.nanoTime() - startTime;
        loadTime.addAndGet(duration);
      }
    }
  }

  /**
   * Gets a cached method handle if it exists.
   *
   * @param functionName the name of the function
   * @param descriptor the function descriptor defining the signature
   * @return optional containing the method handle, or empty if not cached
   */
  public Optional<MethodHandle> get(
      final String functionName, final FunctionDescriptor descriptor) {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    Objects.requireNonNull(descriptor, "Function descriptor cannot be null");

    final String cacheKey = createCacheKey(functionName, descriptor);
    CachedMethodHandle cached = cache.get(cacheKey);

    if (cached != null && cached.isValid()) {
      if (statisticsEnabled) {
        hitCount.incrementAndGet();
      }
      cached.updateLastAccess();
      return Optional.of(cached.getMethodHandle());
    } else {
      if (statisticsEnabled) {
        missCount.incrementAndGet();
      }
      return Optional.empty();
    }
  }

  /**
   * Invalidates a cached method handle.
   *
   * @param functionName the name of the function to invalidate
   * @param descriptor the function descriptor
   * @return true if the method handle was cached and removed, false otherwise
   */
  public boolean invalidate(final String functionName, final FunctionDescriptor descriptor) {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    Objects.requireNonNull(descriptor, "Function descriptor cannot be null");

    final String cacheKey = createCacheKey(functionName, descriptor);
    CachedMethodHandle removed = cache.remove(cacheKey);

    if (removed != null) {
      LOGGER.fine("Invalidated cached method handle for function: " + functionName);
      return true;
    } else {
      return false;
    }
  }

  /** Clears all cached method handles. */
  public void clear() {
    int sizeBefore = cache.size();
    cache.clear();

    if (statisticsEnabled) {
      evictionCount.addAndGet(sizeBefore);
    }

    LOGGER.fine("Cleared method handle cache, removed " + sizeBefore + " entries");
  }

  /**
   * Gets the current cache size.
   *
   * @return the number of cached method handles
   */
  public int size() {
    return cache.size();
  }

  /**
   * Gets the maximum cache size.
   *
   * @return the maximum number of cached method handles
   */
  public int getMaxSize() {
    return maxCacheSize;
  }

  /**
   * Checks if the cache is empty.
   *
   * @return true if the cache is empty, false otherwise
   */
  public boolean isEmpty() {
    return cache.isEmpty();
  }

  /**
   * Gets cache statistics if enabled.
   *
   * @return cache statistics, or empty if statistics are disabled
   */
  public Optional<CacheStatistics> getStatistics() {
    if (!statisticsEnabled) {
      return Optional.empty();
    }

    return Optional.of(
        new CacheStatistics(
            hitCount.get(), missCount.get(), evictionCount.get(), loadTime.get(), cache.size()));
  }

  /** Resets cache statistics. */
  public void resetStatistics() {
    if (statisticsEnabled) {
      hitCount.set(0);
      missCount.set(0);
      evictionCount.set(0);
      loadTime.set(0);
      LOGGER.fine("Reset method handle cache statistics");
    }
  }

  /**
   * Creates a cache key for the function name and descriptor.
   *
   * @param functionName the function name
   * @param descriptor the function descriptor
   * @return the cache key
   */
  private String createCacheKey(final String functionName, final FunctionDescriptor descriptor) {
    return functionName + KEY_SEPARATOR + descriptor.hashCode();
  }

  /**
   * Creates a method handle from a symbol and descriptor.
   *
   * @param symbol the memory segment pointing to the function symbol
   * @param descriptor the function descriptor
   * @return optional containing the method handle, or empty if creation failed
   */
  private Optional<MethodHandle> createMethodHandle(
      final MemorySegment symbol, final FunctionDescriptor descriptor) {
    try {
      Linker linker = Linker.nativeLinker();
      MethodHandle handle = linker.downcallHandle(symbol, descriptor);
      return Optional.of(handle);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to create method handle", e);
      return Optional.empty();
    }
  }

  /** Evicts the least recently used entry from the cache. */
  private void evictLeastRecentlyUsed() {
    String lruKey = null;
    long oldestAccess = Long.MAX_VALUE;

    for (var entry : cache.entrySet()) {
      CachedMethodHandle cached = entry.getValue();
      if (cached.getLastAccess() < oldestAccess) {
        oldestAccess = cached.getLastAccess();
        lruKey = entry.getKey();
      }
    }

    if (lruKey != null) {
      cache.remove(lruKey);
      if (statisticsEnabled) {
        evictionCount.incrementAndGet();
      }
      LOGGER.finest("Evicted LRU cache entry: " + lruKey);
    }
  }

  /** Cached method handle with metadata. */
  private static final class CachedMethodHandle {
    private final MethodHandle methodHandle;
    private final MemorySegment symbol;
    private final FunctionDescriptor descriptor;
    private final long creationTime;
    private volatile long lastAccess;

    CachedMethodHandle(
        final MethodHandle methodHandle,
        final MemorySegment symbol,
        final FunctionDescriptor descriptor) {
      this.methodHandle = Objects.requireNonNull(methodHandle);
      this.symbol = Objects.requireNonNull(symbol);
      this.descriptor = Objects.requireNonNull(descriptor);
      this.creationTime = System.nanoTime();
      this.lastAccess = this.creationTime;
    }

    MethodHandle getMethodHandle() {
      updateLastAccess();
      return methodHandle;
    }

    MemorySegment getSymbol() {
      return symbol;
    }

    FunctionDescriptor getDescriptor() {
      return descriptor;
    }

    long getCreationTime() {
      return creationTime;
    }

    long getLastAccess() {
      return lastAccess;
    }

    void updateLastAccess() {
      this.lastAccess = System.nanoTime();
    }

    boolean isValid() {
      // Check if the symbol is still valid (not null and scope is alive)
      return symbol != null && !symbol.equals(MemorySegment.NULL);
    }
  }

  /** Cache statistics data. */
  public static final class CacheStatistics {
    private final long hitCount;
    private final long missCount;
    private final long evictionCount;
    private final long totalLoadTime;
    private final int currentSize;

    CacheStatistics(
        final long hitCount,
        final long missCount,
        final long evictionCount,
        final long totalLoadTime,
        final int currentSize) {
      this.hitCount = hitCount;
      this.missCount = missCount;
      this.evictionCount = evictionCount;
      this.totalLoadTime = totalLoadTime;
      this.currentSize = currentSize;
    }

    /**
     * Gets the number of cache hits.
     *
     * @return hit count
     */
    public long getHitCount() {
      return hitCount;
    }

    /**
     * Gets the number of cache misses.
     *
     * @return miss count
     */
    public long getMissCount() {
      return missCount;
    }

    /**
     * Gets the number of cache evictions.
     *
     * @return eviction count
     */
    public long getEvictionCount() {
      return evictionCount;
    }

    /**
     * Gets the total time spent loading method handles (in nanoseconds).
     *
     * @return total load time in nanoseconds
     */
    public long getTotalLoadTime() {
      return totalLoadTime;
    }

    /**
     * Gets the current cache size.
     *
     * @return current number of cached method handles
     */
    public int getCurrentSize() {
      return currentSize;
    }

    /**
     * Calculates the cache hit rate.
     *
     * @return hit rate as a percentage (0.0 to 100.0)
     */
    public double getHitRate() {
      long totalRequests = hitCount + missCount;
      return totalRequests == 0 ? 0.0 : (double) hitCount / totalRequests * 100.0;
    }

    /**
     * Calculates the average load time per method handle.
     *
     * @return average load time in nanoseconds
     */
    public double getAverageLoadTime() {
      long totalLoads = missCount; // Only misses result in loads
      return totalLoads == 0 ? 0.0 : (double) totalLoadTime / totalLoads;
    }

    @Override
    public String toString() {
      return String.format(
          "CacheStatistics{hitCount=%d, missCount=%d, evictionCount=%d, "
              + "totalLoadTime=%d ns, currentSize=%d, hitRate=%.2f%%, avgLoadTime=%.2f ns}",
          hitCount,
          missCount,
          evictionCount,
          totalLoadTime,
          currentSize,
          getHitRate(),
          getAverageLoadTime());
    }
  }
}
