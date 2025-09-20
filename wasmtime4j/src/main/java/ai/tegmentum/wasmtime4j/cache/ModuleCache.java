package ai.tegmentum.wasmtime4j.cache;

import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import java.io.Closeable;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for caching compiled WebAssembly modules.
 *
 * <p>ModuleCache provides efficient storage and retrieval of serialized modules to avoid
 * recompilation overhead. Different implementations may provide memory-based caching,
 * file-based persistence, or distributed caching capabilities.
 *
 * <p>Cache implementations should be thread-safe and handle concurrent access gracefully.
 *
 * @since 1.0.0
 */
public interface ModuleCache extends Closeable {

  /**
   * Retrieves a cached module by its cache key.
   *
   * <p>This method returns the cached module if it exists and is still valid. Cache implementations
   * should perform appropriate validation including expiration checks and integrity verification.
   *
   * @param key the cache key identifying the module
   * @return an Optional containing the cached module if found and valid, empty otherwise
   * @throws IllegalArgumentException if key is null
   */
  Optional<SerializedModule> get(final ModuleCacheKey key);

  /**
   * Stores a serialized module in the cache.
   *
   * <p>This method stores the module using the provided key. If a module with the same key already
   * exists, it will be replaced. Cache implementations may apply size limits and eviction policies.
   *
   * @param key the cache key to associate with the module
   * @param module the serialized module to cache
   * @throws IllegalArgumentException if key or module is null
   */
  void put(final ModuleCacheKey key, final SerializedModule module);

  /**
   * Removes a specific module from the cache.
   *
   * <p>This method invalidates and removes the cached module associated with the given key.
   * If no module exists for the key, this operation has no effect.
   *
   * @param key the cache key of the module to remove
   * @throws IllegalArgumentException if key is null
   */
  void invalidate(final ModuleCacheKey key);

  /**
   * Removes all modules from the cache.
   *
   * <p>This method clears the entire cache, removing all cached modules and resetting statistics.
   * This operation is useful for testing and memory management.
   */
  void clear();

  /**
   * Checks if the cache contains a module with the given key.
   *
   * <p>This method provides a quick way to check for cache presence without retrieving the module.
   * Note that the presence check does not guarantee that a subsequent get() will succeed, as the
   * module may be evicted or expire between calls.
   *
   * @param key the cache key to check
   * @return true if a module exists for the key, false otherwise
   * @throws IllegalArgumentException if key is null
   */
  boolean containsKey(final ModuleCacheKey key);

  /**
   * Gets the current size of the cache.
   *
   * <p>The size represents the number of cached modules, not the total memory usage.
   *
   * @return the number of modules currently in the cache
   */
  long size();

  /**
   * Checks if the cache is empty.
   *
   * @return true if the cache contains no modules, false otherwise
   */
  boolean isEmpty();

  /**
   * Gets all cache keys currently in the cache.
   *
   * <p>This method returns a snapshot of current keys. The actual cache contents may change
   * after this method returns due to concurrent operations.
   *
   * @return an immutable set of cache keys
   */
  Set<ModuleCacheKey> keySet();

  /**
   * Gets statistics about cache usage and performance.
   *
   * <p>Statistics include hit/miss ratios, eviction counts, and other metrics useful for
   * monitoring and tuning cache performance.
   *
   * @return current cache statistics
   */
  CacheStatistics getStatistics();

  /**
   * Performs cache maintenance operations.
   *
   * <p>This method triggers cleanup operations such as removing expired entries, compacting
   * storage, and updating statistics. Some implementations may perform maintenance automatically,
   * while others require explicit calls to this method.
   */
  void performMaintenance();

  /**
   * Gets the configuration used by this cache.
   *
   * @return the cache configuration
   */
  CacheConfiguration getConfiguration();

  /**
   * Estimates the memory usage of the cache in bytes.
   *
   * <p>This method provides an estimate of the total memory used by cached modules. The actual
   * memory usage may vary due to implementation details and overhead.
   *
   * @return estimated memory usage in bytes
   */
  long estimateMemoryUsage();

  /**
   * Closes the cache and releases associated resources.
   *
   * <p>After closing, the cache becomes unusable and all cached modules are discarded. File-based
   * caches may persist data to storage before closing.
   */
  @Override
  void close();
}