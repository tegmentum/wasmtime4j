package ai.tegmentum.wasmtime4j.resource;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;

/**
 * Builder for creating ResourceCache instances with custom configuration.
 *
 * <p>ResourceCacheBuilder provides a fluent API for configuring and creating resource caches with
 * specific requirements and policies.
 *
 * @since 1.0.0
 */
public final class ResourceCacheBuilder {

  private CacheConfiguration configuration = CacheConfiguration.defaultConfiguration();
  private String cacheName = "DefaultResourceCache";
  private CacheLoader defaultLoader;
  private boolean jmxExportEnabled = false;
  private Duration shutdownTimeout = Duration.ofSeconds(30);

  /** Package-private constructor - use ResourceCache.builder() to create instances. */
  ResourceCacheBuilder() {}

  /**
   * Sets the cache configuration.
   *
   * @param configuration the cache configuration
   * @return this builder
   * @throws IllegalArgumentException if configuration is null
   */
  public ResourceCacheBuilder withConfiguration(final CacheConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    this.configuration = configuration;
    return this;
  }

  /**
   * Sets the maximum number of entries.
   *
   * @param maxEntries the maximum entry count
   * @return this builder
   * @throws IllegalArgumentException if maxEntries is less than 1
   */
  public ResourceCacheBuilder withMaxEntries(final int maxEntries) {
    this.configuration = configuration.toBuilder().withMaxEntries(maxEntries).build();
    return this;
  }

  /**
   * Sets the maximum memory usage.
   *
   * @param maxMemoryUsage the maximum memory usage in bytes
   * @return this builder
   * @throws IllegalArgumentException if maxMemoryUsage is less than 1
   */
  public ResourceCacheBuilder withMaxMemoryUsage(final long maxMemoryUsage) {
    this.configuration = configuration.toBuilder().withMaxMemoryUsage(maxMemoryUsage).build();
    return this;
  }

  /**
   * Sets the default time-to-live.
   *
   * @param defaultTtl the default TTL
   * @return this builder
   * @throws IllegalArgumentException if defaultTtl is null or negative
   */
  public ResourceCacheBuilder withDefaultTtl(final Duration defaultTtl) {
    this.configuration = configuration.toBuilder().withDefaultTtl(defaultTtl).build();
    return this;
  }

  /**
   * Sets the eviction policy.
   *
   * @param evictionPolicy the eviction policy
   * @return this builder
   * @throws IllegalArgumentException if evictionPolicy is null
   */
  public ResourceCacheBuilder withEvictionPolicy(final EvictionPolicy evictionPolicy) {
    this.configuration = configuration.toBuilder().withEvictionPolicy(evictionPolicy).build();
    return this;
  }

  /**
   * Sets the cleanup interval.
   *
   * @param cleanupInterval the cleanup interval
   * @return this builder
   * @throws IllegalArgumentException if cleanupInterval is null or negative
   */
  public ResourceCacheBuilder withCleanupInterval(final Duration cleanupInterval) {
    this.configuration = configuration.toBuilder().withCleanupInterval(cleanupInterval).build();
    return this;
  }

  /**
   * Enables or disables statistics collection.
   *
   * @param statisticsEnabled true to enable statistics
   * @return this builder
   */
  public ResourceCacheBuilder withStatisticsEnabled(final boolean statisticsEnabled) {
    this.configuration = configuration.toBuilder().withStatisticsEnabled(statisticsEnabled).build();
    return this;
  }

  /**
   * Enables or disables persistence.
   *
   * @param persistenceEnabled true to enable persistence
   * @return this builder
   */
  public ResourceCacheBuilder withPersistenceEnabled(final boolean persistenceEnabled) {
    this.configuration =
        configuration.toBuilder().withPersistenceEnabled(persistenceEnabled).build();
    return this;
  }

  /**
   * Sets the persistence location.
   *
   * @param persistenceLocation the persistence location
   * @return this builder
   */
  public ResourceCacheBuilder withPersistenceLocation(final String persistenceLocation) {
    this.configuration =
        configuration.toBuilder().withPersistenceLocation(persistenceLocation).build();
    return this;
  }

  /**
   * Sets the concurrency level.
   *
   * @param concurrencyLevel the concurrency level
   * @return this builder
   * @throws IllegalArgumentException if concurrencyLevel is less than 1
   */
  public ResourceCacheBuilder withConcurrencyLevel(final int concurrencyLevel) {
    this.configuration = configuration.toBuilder().withConcurrencyLevel(concurrencyLevel).build();
    return this;
  }

  /**
   * Enables weak references for keys.
   *
   * @return this builder
   */
  public ResourceCacheBuilder withWeakKeys() {
    this.configuration = configuration.toBuilder().withWeakKeys(true).build();
    return this;
  }

  /**
   * Enables weak references for values.
   *
   * @return this builder
   */
  public ResourceCacheBuilder withWeakValues() {
    this.configuration = configuration.toBuilder().withWeakValues(true).build();
    return this;
  }

  /**
   * Sets the eviction threshold.
   *
   * @param evictionThreshold the eviction threshold (0.0 to 1.0)
   * @return this builder
   * @throws IllegalArgumentException if evictionThreshold is not between 0.0 and 1.0
   */
  public ResourceCacheBuilder withEvictionThreshold(final double evictionThreshold) {
    this.configuration = configuration.toBuilder().withEvictionThreshold(evictionThreshold).build();
    return this;
  }

  /**
   * Sets the refresh interval.
   *
   * @param refreshInterval the refresh interval
   * @return this builder
   * @throws IllegalArgumentException if refreshInterval is negative
   */
  public ResourceCacheBuilder withRefreshInterval(final Duration refreshInterval) {
    this.configuration = configuration.toBuilder().withRefreshInterval(refreshInterval).build();
    return this;
  }

  /**
   * Sets the cache name for identification and monitoring.
   *
   * @param cacheName the cache name
   * @return this builder
   * @throws IllegalArgumentException if cacheName is null or empty
   */
  public ResourceCacheBuilder withCacheName(final String cacheName) {
    if (cacheName == null || cacheName.trim().isEmpty()) {
      throw new IllegalArgumentException("Cache name cannot be null or empty");
    }
    this.cacheName = cacheName.trim();
    return this;
  }

  /**
   * Sets the default cache loader.
   *
   * @param defaultLoader the default cache loader
   * @return this builder
   */
  public ResourceCacheBuilder withDefaultLoader(final CacheLoader defaultLoader) {
    this.defaultLoader = defaultLoader;
    return this;
  }

  /**
   * Enables or disables JMX export for cache metrics.
   *
   * @param jmxExportEnabled true to enable JMX export
   * @return this builder
   */
  public ResourceCacheBuilder withJmxExport(final boolean jmxExportEnabled) {
    this.jmxExportEnabled = jmxExportEnabled;
    return this;
  }

  /**
   * Sets the shutdown timeout for graceful cache shutdown.
   *
   * @param shutdownTimeout the shutdown timeout
   * @return this builder
   * @throws IllegalArgumentException if shutdownTimeout is null or negative
   */
  public ResourceCacheBuilder withShutdownTimeout(final Duration shutdownTimeout) {
    if (shutdownTimeout == null || shutdownTimeout.isNegative()) {
      throw new IllegalArgumentException("Shutdown timeout must be positive");
    }
    this.shutdownTimeout = shutdownTimeout;
    return this;
  }

  /**
   * Builds the resource cache with the configured settings.
   *
   * @return a new resource cache instance
   * @throws WasmException if cache creation fails
   */
  public ResourceCache build() throws WasmException {
    try {
      // This would be implemented to create the actual cache implementation
      // For now, return a placeholder implementation
      throw new WasmException(
          "ResourceCache implementation not yet available - " + "this is a placeholder interface");
    } catch (Exception e) {
      throw new WasmException("Failed to create resource cache: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the current configuration being built.
   *
   * @return the current cache configuration
   */
  public CacheConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Gets the cache name.
   *
   * @return the cache name
   */
  public String getCacheName() {
    return cacheName;
  }

  /**
   * Gets the default loader.
   *
   * @return the default loader, or null if not set
   */
  public CacheLoader getDefaultLoader() {
    return defaultLoader;
  }

  /**
   * Checks if JMX export is enabled.
   *
   * @return true if JMX export is enabled
   */
  public boolean isJmxExportEnabled() {
    return jmxExportEnabled;
  }

  /**
   * Gets the shutdown timeout.
   *
   * @return the shutdown timeout
   */
  public Duration getShutdownTimeout() {
    return shutdownTimeout;
  }
}
