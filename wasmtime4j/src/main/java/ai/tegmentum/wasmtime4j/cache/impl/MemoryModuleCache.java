package ai.tegmentum.wasmtime4j.cache.impl;

import ai.tegmentum.wasmtime4j.cache.CacheConfiguration;
import ai.tegmentum.wasmtime4j.cache.CacheStatistics;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheKey;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * In-memory implementation of ModuleCache with LRU eviction policy.
 *
 * <p>This implementation provides fast access to cached modules by keeping them in memory with
 * automatic eviction based on Least Recently Used (LRU) strategy when the cache size limit is
 * reached.
 *
 * <p>This cache is thread-safe and supports concurrent access from multiple threads.
 *
 * @since 1.0.0
 */
public final class MemoryModuleCache implements ModuleCache {

  private static final Logger LOGGER = Logger.getLogger(MemoryModuleCache.class.getName());

  private final long maxSize;
  private final CacheConfiguration configuration;
  private final CacheStatisticsImpl statistics;
  private final Map<ModuleCacheKey, SerializedModule> cache;
  private final ReadWriteLock lock;
  private volatile boolean closed;

  /**
   * Creates a new MemoryModuleCache with the specified maximum size.
   *
   * @param maxSize the maximum number of modules to cache
   * @throws IllegalArgumentException if maxSize is not positive
   */
  public MemoryModuleCache(final long maxSize) {
    this(maxSize, CacheConfiguration.defaults());
  }

  /**
   * Creates a new MemoryModuleCache with the specified configuration.
   *
   * @param maxSize the maximum number of modules to cache
   * @param configuration the cache configuration
   * @throws IllegalArgumentException if maxSize is not positive or configuration is null
   */
  public MemoryModuleCache(final long maxSize, final CacheConfiguration configuration) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Max size must be positive: " + maxSize);
    }
    this.maxSize = maxSize;
    this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    this.statistics = new CacheStatisticsImpl();
    this.lock = new ReentrantReadWriteLock();
    this.closed = false;

    // Create thread-safe LRU cache
    this.cache =
        new LinkedHashMap<ModuleCacheKey, SerializedModule>(
            (int) Math.min(maxSize + 1, Integer.MAX_VALUE), 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(
              final Map.Entry<ModuleCacheKey, SerializedModule> eldest) {
            final boolean shouldRemove = size() > maxSize;
            if (shouldRemove) {
              statistics.recordEviction();
              LOGGER.fine("Evicting oldest cache entry: " + eldest.getKey());
            }
            return shouldRemove;
          }
        };
  }

  @Override
  public Optional<SerializedModule> get(final ModuleCacheKey key) {
    Objects.requireNonNull(key, "Cache key cannot be null");
    ensureNotClosed();

    lock.readLock().lock();
    try {
      final SerializedModule module = cache.get(key);
      if (module != null) {
        statistics.recordHit();
        LOGGER.fine("Cache hit for key: " + key);
        return Optional.of(module);
      } else {
        statistics.recordMiss();
        LOGGER.fine("Cache miss for key: " + key);
        return Optional.empty();
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void put(final ModuleCacheKey key, final SerializedModule module) {
    Objects.requireNonNull(key, "Cache key cannot be null");
    Objects.requireNonNull(module, "Serialized module cannot be null");
    ensureNotClosed();

    lock.writeLock().lock();
    try {
      final boolean wasPresent = cache.containsKey(key);
      cache.put(key, module);

      if (!wasPresent) {
        LOGGER.fine("Added new module to cache: " + key + ", size=" + module.getSize() + " bytes");
      } else {
        LOGGER.fine("Updated existing module in cache: " + key);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void invalidate(final ModuleCacheKey key) {
    Objects.requireNonNull(key, "Cache key cannot be null");
    ensureNotClosed();

    lock.writeLock().lock();
    try {
      final SerializedModule removed = cache.remove(key);
      if (removed != null) {
        LOGGER.fine("Invalidated cache entry: " + key);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void clear() {
    ensureNotClosed();

    lock.writeLock().lock();
    try {
      final int oldSize = cache.size();
      cache.clear();
      statistics.reset();
      LOGGER.info("Cleared cache, removed " + oldSize + " entries");
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean containsKey(final ModuleCacheKey key) {
    Objects.requireNonNull(key, "Cache key cannot be null");
    ensureNotClosed();

    lock.readLock().lock();
    try {
      return cache.containsKey(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public long size() {
    ensureNotClosed();

    lock.readLock().lock();
    try {
      return cache.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    ensureNotClosed();

    lock.readLock().lock();
    try {
      return cache.isEmpty();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Set<ModuleCacheKey> keySet() {
    ensureNotClosed();

    lock.readLock().lock();
    try {
      return Set.copyOf(cache.keySet());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public CacheStatistics getStatistics() {
    ensureNotClosed();
    return statistics.snapshot();
  }

  @Override
  public void performMaintenance() {
    ensureNotClosed();

    lock.writeLock().lock();
    try {
      // For memory cache, no special maintenance is needed
      // The LRU eviction happens automatically
      LOGGER.fine("Performed cache maintenance, current size: " + cache.size());
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public CacheConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public long estimateMemoryUsage() {
    ensureNotClosed();

    lock.readLock().lock();
    try {
      long totalSize = 0;
      for (final SerializedModule module : cache.values()) {
        totalSize += module.getSize();
      }
      // Add estimated overhead per entry (key + references)
      totalSize += cache.size() * 100; // rough estimate
      return totalSize;
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    lock.writeLock().lock();
    try {
      if (!closed) {
        final int oldSize = cache.size();
        cache.clear();
        closed = true;
        LOGGER.info("Closed memory cache, released " + oldSize + " entries");
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Checks if this cache is closed.
   *
   * @return true if the cache is closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Gets the maximum size of this cache.
   *
   * @return the maximum number of modules that can be cached
   */
  public long getMaxSize() {
    return maxSize;
  }

  /**
   * Ensures that this cache is not closed.
   *
   * @throws IllegalStateException if the cache is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Cache has been closed");
    }
  }

  @Override
  public String toString() {
    lock.readLock().lock();
    try {
      return "MemoryModuleCache{"
          + "size="
          + cache.size()
          + ", maxSize="
          + maxSize
          + ", hitRate="
          + statistics.getHitRate()
          + ", closed="
          + closed
          + '}';
    } finally {
      lock.readLock().unlock();
    }
  }
}
