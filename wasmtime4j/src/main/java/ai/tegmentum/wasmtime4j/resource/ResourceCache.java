package ai.tegmentum.wasmtime4j.resource;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Enterprise-grade resource cache providing caching mechanisms and eviction policies.
 *
 * <p>ResourceCache provides intelligent caching of frequently used resources to improve
 * performance and reduce resource allocation overhead. It supports configurable eviction
 * policies, cache size limits, and comprehensive monitoring.
 *
 * <p>Key features:
 * - Multiple eviction policies (LRU, LFU, TTL-based, size-based)
 * - Configurable cache size limits and memory thresholds
 * - Thread-safe concurrent access with high performance
 * - Cache statistics and hit/miss tracking
 * - Automatic expiration and cleanup of stale entries
 * - Cache warming and preloading capabilities
 * - Integration with resource lifecycle management
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ResourceCache cache = ResourceCache.builder()
 *     .withMaxEntries(1000)
 *     .withMaxMemoryUsage(256 * 1024 * 1024) // 256MB
 *     .withDefaultTtl(Duration.ofHours(2))
 *     .withEvictionPolicy(EvictionPolicy.LRU)
 *     .build();
 *
 * // Cache a compiled module for reuse
 * cache.put("module:common", module, ResourceType.MODULE,
 *           CachePolicy.builder()
 *               .withTtl(Duration.ofHours(24))
 *               .withPriority(CachePriority.HIGH)
 *               .build());
 *
 * // Retrieve from cache
 * Optional<Module> cached = cache.get("module:common", Module.class);
 * if (cached.isPresent()) {
 *     // Use cached module
 * } else {
 *     // Compile new module and cache it
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ResourceCache extends AutoCloseable {

    /**
     * Creates a new resource cache builder with default configuration.
     *
     * @return a new builder instance
     */
    static ResourceCacheBuilder builder() {
        return new ResourceCacheBuilder();
    }

    /**
     * Creates a resource cache with default configuration.
     *
     * @return a new resource cache instance
     * @throws WasmException if cache creation fails
     */
    static ResourceCache create() throws WasmException {
        return builder().build();
    }

    /**
     * Stores a resource in the cache with the specified key and policy.
     *
     * @param <T> the resource type
     * @param key the cache key
     * @param resource the resource to cache
     * @param type the resource type classification
     * @param policy the cache policy for this entry
     * @throws WasmException if caching fails
     * @throws IllegalArgumentException if any parameter is null or key is empty
     */
    <T> void put(final String key, final T resource, final ResourceType type,
                final CachePolicy policy) throws WasmException;

    /**
     * Stores a resource in the cache with default policy.
     *
     * @param <T> the resource type
     * @param key the cache key
     * @param resource the resource to cache
     * @param type the resource type classification
     * @throws WasmException if caching fails
     * @throws IllegalArgumentException if any parameter is null or key is empty
     */
    <T> void put(final String key, final T resource, final ResourceType type) throws WasmException;

    /**
     * Stores a resource in the cache if the key is not already present.
     *
     * @param <T> the resource type
     * @param key the cache key
     * @param resource the resource to cache
     * @param type the resource type classification
     * @param policy the cache policy for this entry
     * @return true if the resource was stored, false if key already exists
     * @throws WasmException if caching fails
     * @throws IllegalArgumentException if any parameter is null or key is empty
     */
    <T> boolean putIfAbsent(final String key, final T resource, final ResourceType type,
                           final CachePolicy policy) throws WasmException;

    /**
     * Retrieves a resource from the cache.
     *
     * @param <T> the expected resource type
     * @param key the cache key
     * @param expectedType the expected resource class
     * @return the cached resource, or empty if not found or expired
     * @throws WasmException if cache access fails
     * @throws IllegalArgumentException if key is null/empty or expectedType is null
     */
    <T> Optional<T> get(final String key, final Class<T> expectedType) throws WasmException;

    /**
     * Retrieves a resource from the cache and extends its TTL.
     *
     * @param <T> the expected resource type
     * @param key the cache key
     * @param expectedType the expected resource class
     * @param ttlExtension the duration to extend the TTL
     * @return the cached resource, or empty if not found or expired
     * @throws WasmException if cache access fails
     * @throws IllegalArgumentException if any parameter is null or key is empty
     */
    <T> Optional<T> getAndExtendTtl(final String key, final Class<T> expectedType,
                                   final Duration ttlExtension) throws WasmException;

    /**
     * Retrieves a resource from the cache or computes it if not present.
     *
     * @param <T> the resource type
     * @param key the cache key
     * @param type the resource type classification
     * @param expectedType the expected resource class
     * @param supplier the supplier to compute the resource if not cached
     * @return the cached or computed resource
     * @throws WasmException if cache access or computation fails
     * @throws IllegalArgumentException if any parameter is null or key is empty
     */
    <T> T getOrCompute(final String key, final ResourceType type, final Class<T> expectedType,
                      final java.util.function.Supplier<T> supplier) throws WasmException;

    /**
     * Retrieves a resource from the cache or computes it asynchronously.
     *
     * @param <T> the resource type
     * @param key the cache key
     * @param type the resource type classification
     * @param expectedType the expected resource class
     * @param supplier the supplier to compute the resource if not cached
     * @return future containing the cached or computed resource
     * @throws IllegalArgumentException if any parameter is null or key is empty
     */
    <T> CompletableFuture<T> getOrComputeAsync(final String key, final ResourceType type,
                                              final Class<T> expectedType,
                                              final java.util.function.Supplier<T> supplier);

    /**
     * Checks if a resource is present in the cache.
     *
     * @param key the cache key
     * @return true if the key is present and not expired, false otherwise
     * @throws WasmException if cache access fails
     * @throws IllegalArgumentException if key is null or empty
     */
    boolean containsKey(final String key) throws WasmException;

    /**
     * Removes a resource from the cache.
     *
     * @param key the cache key to remove
     * @return true if the key was present and removed, false otherwise
     * @throws WasmException if cache removal fails
     * @throws IllegalArgumentException if key is null or empty
     */
    boolean remove(final String key) throws WasmException;

    /**
     * Removes resources from the cache based on a key pattern.
     *
     * @param keyPattern the pattern to match keys against (supports wildcards)
     * @return the number of entries that were removed
     * @throws WasmException if cache removal fails
     * @throws IllegalArgumentException if keyPattern is null or empty
     */
    int removeByPattern(final String keyPattern) throws WasmException;

    /**
     * Removes all resources of a specific type from the cache.
     *
     * @param type the resource type to remove
     * @return the number of entries that were removed
     * @throws WasmException if cache removal fails
     * @throws IllegalArgumentException if type is null
     */
    int removeByType(final ResourceType type) throws WasmException;

    /**
     * Clears all entries from the cache.
     *
     * @throws WasmException if cache clearing fails
     */
    void clear() throws WasmException;

    /**
     * Gets the current number of entries in the cache.
     *
     * @return the number of cache entries
     */
    int size();

    /**
     * Checks if the cache is empty.
     *
     * @return true if the cache is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Gets the estimated memory usage of the cache in bytes.
     *
     * @return the estimated memory usage
     */
    long getEstimatedMemoryUsage();

    /**
     * Gets comprehensive cache statistics.
     *
     * @return current cache statistics
     */
    CacheStatistics getStatistics();

    /**
     * Gets cache statistics for a specific resource type.
     *
     * @param type the resource type
     * @return cache statistics for the specified type
     * @throws IllegalArgumentException if type is null
     */
    CacheStatistics getStatistics(final ResourceType type);

    /**
     * Gets all cache keys currently present.
     *
     * @return list of all cache keys
     * @throws WasmException if key enumeration fails
     */
    List<String> getAllKeys() throws WasmException;

    /**
     * Gets all cache keys for a specific resource type.
     *
     * @param type the resource type
     * @return list of cache keys for the specified type
     * @throws WasmException if key enumeration fails
     * @throws IllegalArgumentException if type is null
     */
    List<String> getKeysByType(final ResourceType type) throws WasmException;

    /**
     * Gets cache entry metadata for a specific key.
     *
     * @param key the cache key
     * @return the cache entry metadata, or empty if key not found
     * @throws WasmException if metadata access fails
     * @throws IllegalArgumentException if key is null or empty
     */
    Optional<CacheEntryMetadata> getEntryMetadata(final String key) throws WasmException;

    /**
     * Gets all cache entry metadata.
     *
     * @return map of cache keys to their metadata
     * @throws WasmException if metadata access fails
     */
    Map<String, CacheEntryMetadata> getAllEntryMetadata() throws WasmException;

    /**
     * Performs manual eviction of expired entries.
     *
     * @return the number of entries that were evicted
     * @throws WasmException if eviction fails
     */
    int evictExpiredEntries() throws WasmException;

    /**
     * Performs manual eviction based on configured policies.
     *
     * @param targetMemoryUsage the target memory usage to achieve
     * @return the number of entries that were evicted
     * @throws WasmException if eviction fails
     * @throws IllegalArgumentException if targetMemoryUsage is negative
     */
    int evictToTargetMemory(final long targetMemoryUsage) throws WasmException;

    /**
     * Forces eviction of the least valuable entries.
     *
     * @param targetCount the target number of entries to maintain
     * @return the number of entries that were evicted
     * @throws WasmException if eviction fails
     * @throws IllegalArgumentException if targetCount is negative
     */
    int evictToTargetCount(final int targetCount) throws WasmException;

    /**
     * Preloads the cache with resources using the provided loader.
     *
     * @param loader the cache loader to use for preloading
     * @return future that completes when preloading is finished
     * @throws IllegalArgumentException if loader is null
     */
    CompletableFuture<Void> preload(final CacheLoader loader);

    /**
     * Refreshes a specific cache entry by reloading it.
     *
     * @param key the cache key to refresh
     * @param loader the cache loader to use for refreshing
     * @return future that completes when refresh is finished
     * @throws IllegalArgumentException if key is null/empty or loader is null
     */
    CompletableFuture<Void> refresh(final String key, final CacheLoader loader);

    /**
     * Refreshes all entries of a specific type.
     *
     * @param type the resource type to refresh
     * @param loader the cache loader to use for refreshing
     * @return future that completes when refresh is finished
     * @throws IllegalArgumentException if type or loader is null
     */
    CompletableFuture<Void> refreshByType(final ResourceType type, final CacheLoader loader);

    /**
     * Validates all cache entries and removes invalid ones.
     *
     * @return the number of invalid entries that were removed
     * @throws WasmException if validation fails
     */
    int validateCache() throws WasmException;

    /**
     * Validates cache entries of a specific type.
     *
     * @param type the resource type to validate
     * @return the number of invalid entries that were removed
     * @throws WasmException if validation fails
     * @throws IllegalArgumentException if type is null
     */
    int validateCache(final ResourceType type) throws WasmException;

    /**
     * Gets the current cache configuration.
     *
     * @return the cache configuration
     */
    CacheConfiguration getConfiguration();

    /**
     * Updates the cache configuration.
     *
     * <p>Configuration changes take effect immediately and may trigger
     * eviction operations if new limits are more restrictive.
     *
     * @param configuration the new cache configuration
     * @throws WasmException if configuration update fails
     * @throws IllegalArgumentException if configuration is null
     */
    void updateConfiguration(final CacheConfiguration configuration) throws WasmException;

    /**
     * Checks if the cache is currently operational.
     *
     * @return true if the cache is operational, false otherwise
     */
    boolean isOperational();

    /**
     * Gracefully shuts down the cache.
     *
     * <p>This method will stop background operations, persist cache state if
     * configured, and clean up resources.
     *
     * @param timeout the maximum time to wait for shutdown
     * @throws WasmException if shutdown fails or times out
     * @throws IllegalArgumentException if timeout is null or negative
     */
    void shutdown(final Duration timeout) throws WasmException;

    /**
     * Asynchronously shuts down the cache.
     *
     * @param timeout the maximum time to wait for shutdown
     * @return future that completes when shutdown is finished
     * @throws IllegalArgumentException if timeout is null or negative
     */
    CompletableFuture<Void> shutdownAsync(final Duration timeout);

    /**
     * Closes the cache and releases all resources immediately.
     */
    @Override
    void close();
}