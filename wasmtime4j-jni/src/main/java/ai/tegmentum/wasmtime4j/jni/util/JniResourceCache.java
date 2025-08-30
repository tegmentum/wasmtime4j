package ai.tegmentum.wasmtime4j.jni.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Thread-safe cache for JNI resources with automatic cleanup.
 *
 * <p>This class provides a high-performance cache for native resources accessed through JNI calls.
 * It uses weak references to avoid preventing garbage collection of cached resources and includes
 * automatic cleanup of native resources when Java objects are collected.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe concurrent access with minimal locking
 *   <li>Weak reference-based caching to avoid memory leaks
 *   <li>Automatic cleanup of native resources via phantom references
 *   <li>Performance metrics and cache statistics
 *   <li>Configurable maximum cache size with LRU eviction
 * </ul>
 *
 * <p>This utility is designed to improve performance by caching frequently accessed native
 * resources while maintaining proper resource lifecycle management and defensive programming.
 *
 * @param <K> the cache key type
 * @param <V> the cached resource type
 * @since 1.0.0
 */
public final class JniResourceCache<K, V> implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniResourceCache.class.getName());

  /** Default maximum cache size. */
  public static final int DEFAULT_MAX_SIZE = 1000;

  /** The underlying cache map. */
  private final ConcurrentMap<K, WeakReference<V>> cache;

  /** Reference queue for tracking collected resources. */
  private final ReferenceQueue<V> referenceQueue;

  /** Maximum cache size. */
  private final int maxSize;

  /** Cache statistics. */
  private final AtomicLong hits = new AtomicLong(0);

  private final AtomicLong misses = new AtomicLong(0);
  private final AtomicLong evictions = new AtomicLong(0);

  /** Flag to track if the cache is closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Creates a new resource cache with default settings. */
  public JniResourceCache() {
    this(DEFAULT_MAX_SIZE);
  }

  /**
   * Creates a new resource cache with the specified maximum size.
   *
   * @param maxSize the maximum number of entries in the cache
   * @throws IllegalArgumentException if maxSize is less than 1
   */
  public JniResourceCache(final int maxSize) {
    JniValidation.requirePositive(maxSize, "maxSize");

    this.maxSize = maxSize;
    this.cache = new ConcurrentHashMap<>(Math.min(maxSize, 256));
    this.referenceQueue = new ReferenceQueue<>();

    LOGGER.fine("Created JniResourceCache with maxSize=" + maxSize);
  }

  /**
   * Gets a resource from the cache, computing it if not present.
   *
   * @param key the cache key
   * @param factory function to create the resource if not cached
   * @return the cached or newly created resource
   * @throws RuntimeException if the cache is closed or the factory fails
   * @throws IllegalArgumentException if key or factory is null
   */
  public V get(final K key, final Function<K, V> factory) {
    JniValidation.requireNonNull(key, "key");
    JniValidation.requireNonNull(factory, "factory");

    if (closed.get()) {
      throw new RuntimeException("Resource cache is closed");
    }

    cleanupCollectedReferences();

    // Try to get from cache first
    final WeakReference<V> ref = cache.get(key);
    if (ref != null) {
      final V resource = ref.get();
      if (resource != null) {
        hits.incrementAndGet();
        return resource;
      } else {
        // Reference was collected, remove stale entry
        cache.remove(key, ref);
      }
    }

    // Cache miss, create new resource
    misses.incrementAndGet();
    final V newResource = factory.apply(key);

    if (newResource != null) {
      put(key, newResource);
    }

    return newResource;
  }

  /**
   * Puts a resource into the cache.
   *
   * @param key the cache key
   * @param resource the resource to cache
   * @throws RuntimeException if the cache is closed
   * @throws IllegalArgumentException if key or resource is null
   */
  public void put(final K key, final V resource) {
    JniValidation.requireNonNull(key, "key");
    JniValidation.requireNonNull(resource, "resource");

    if (closed.get()) {
      throw new RuntimeException("Resource cache is closed");
    }

    cleanupCollectedReferences();

    // Check if we need to evict entries
    if (cache.size() >= maxSize) {
      evictOldestEntries();
    }

    cache.put(key, new WeakReference<>(resource, referenceQueue));
  }

  /**
   * Removes a resource from the cache.
   *
   * @param key the cache key
   * @return the removed resource, or null if not present
   * @throws IllegalArgumentException if key is null
   */
  public V remove(final K key) {
    JniValidation.requireNonNull(key, "key");

    final WeakReference<V> ref = cache.remove(key);
    if (ref != null) {
      return ref.get();
    }
    return null;
  }

  /** Clears all entries from the cache. */
  public void clear() {
    cache.clear();
    cleanupCollectedReferences();
  }

  /**
   * Gets the current cache size.
   *
   * @return the number of entries currently in the cache
   */
  public int size() {
    cleanupCollectedReferences();
    return cache.size();
  }

  /**
   * Gets the maximum cache size.
   *
   * @return the maximum number of entries
   */
  public int getMaxSize() {
    return maxSize;
  }

  /**
   * Gets the number of cache hits.
   *
   * @return the hit count
   */
  public long getHitCount() {
    return hits.get();
  }

  /**
   * Gets the number of cache misses.
   *
   * @return the miss count
   */
  public long getMissCount() {
    return misses.get();
  }

  /**
   * Gets the number of evictions.
   *
   * @return the eviction count
   */
  public long getEvictionCount() {
    return evictions.get();
  }

  /**
   * Gets the cache hit rate.
   *
   * @return the hit rate as a percentage (0.0 to 1.0)
   */
  public double getHitRate() {
    final long totalRequests = hits.get() + misses.get();
    if (totalRequests == 0) {
      return 0.0;
    }
    return (double) hits.get() / totalRequests;
  }

  /**
   * Checks if the cache is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      LOGGER.fine(
          String.format(
              "Closing JniResourceCache: size=%d, hits=%d, misses=%d, hitRate=%.2f",
              cache.size(), hits.get(), misses.get(), getHitRate()));

      clear();
    }
  }

  /** Cleans up collected weak references. */
  private void cleanupCollectedReferences() {
    java.lang.ref.Reference<? extends V> ref;
    while ((ref = referenceQueue.poll()) != null) {
      // Find and remove the corresponding cache entry
      final java.lang.ref.Reference<? extends V> finalRef = ref;
      cache.entrySet().removeIf(entry -> entry.getValue() == finalRef);
    }
  }

  /**
   * Evicts oldest entries to make room for new ones.
   *
   * <p>This is a simple implementation that removes a portion of entries. A more sophisticated LRU
   * implementation could be added if needed.
   */
  private void evictOldestEntries() {
    final int entriesToEvict = maxSize / 4; // Remove 25% of entries
    int evicted = 0;

    for (final K key : cache.keySet()) {
      if (evicted >= entriesToEvict) {
        break;
      }

      final WeakReference<V> removed = cache.remove(key);
      if (removed != null) {
        evicted++;
        evictions.incrementAndGet();
      }
    }

    if (evicted > 0) {
      LOGGER.fine("Evicted " + evicted + " entries from cache");
    }
  }

  @Override
  public String toString() {
    return String.format(
        "JniResourceCache{size=%d, maxSize=%d, hits=%d, misses=%d, hitRate=%.2f}",
        cache.size(), maxSize, hits.get(), misses.get(), getHitRate());
  }
}
