package ai.tegmentum.wasmtime4j.cache;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration settings for module caches.
 *
 * <p>CacheConfiguration defines the behavior and limits of module caches including size limits,
 * expiration policies, and maintenance settings.
 *
 * @since 1.0.0
 */
public interface CacheConfiguration {

  /**
   * Gets the maximum number of entries the cache can hold.
   *
   * @return the maximum cache size, or -1 if unlimited
   */
  long getMaxSize();

  /**
   * Gets the maximum memory usage allowed for the cache.
   *
   * @return the maximum memory usage in bytes, or -1 if unlimited
   */
  long getMaxMemoryUsage();

  /**
   * Gets the duration after which cache entries expire.
   *
   * @return the expiration duration, or null if entries don't expire
   */
  Duration getExpirationDuration();

  /**
   * Gets the eviction policy used when the cache reaches capacity.
   *
   * @return the eviction policy
   */
  EvictionPolicy getEvictionPolicy();

  /**
   * Checks if integrity checking is enabled for cached modules.
   *
   * @return true if integrity checking is enabled, false otherwise
   */
  boolean isIntegrityCheckingEnabled();

  /**
   * Checks if statistics collection is enabled.
   *
   * @return true if statistics are collected, false otherwise
   */
  boolean isStatisticsEnabled();

  /**
   * Gets the interval for automatic maintenance operations.
   *
   * @return the maintenance interval, or null if automatic maintenance is disabled
   */
  Duration getMaintenanceInterval();

  /**
   * Checks if the cache should persist data across restarts.
   *
   * @return true if persistence is enabled, false otherwise
   */
  boolean isPersistenceEnabled();

  /**
   * Gets the path where cache data should be persisted.
   *
   * @return the persistence path, or null if persistence is disabled
   */
  String getPersistencePath();

  /**
   * Gets the number of threads used for cache operations.
   *
   * @return the thread pool size, or -1 for unlimited
   */
  int getConcurrencyLevel();

  /**
   * Checks if compression should be used for stored cache entries.
   *
   * @return true if compression is enabled, false otherwise
   */
  boolean isCompressionEnabled();

  /**
   * Creates a builder for constructing CacheConfiguration instances.
   *
   * @return a new configuration builder
   */
  static CacheConfigurationBuilder builder() {
    return new CacheConfigurationBuilder();
  }

  /**
   * Creates a CacheConfiguration with default settings.
   *
   * @return a configuration with default settings
   */
  static CacheConfiguration defaults() {
    return builder().build();
  }

  /**
   * Eviction policies for cache entries.
   */
  enum EvictionPolicy {
    /** Least Recently Used - evict the least recently accessed entry. */
    LRU,

    /** Least Frequently Used - evict the least frequently accessed entry. */
    LFU,

    /** First In, First Out - evict the oldest entry. */
    FIFO,

    /** Random eviction - evict a random entry. */
    RANDOM
  }

  /**
   * Builder for CacheConfiguration.
   */
  final class CacheConfigurationBuilder {
    private long maxSize = 1000;
    private long maxMemoryUsage = -1;
    private Duration expirationDuration = Duration.ofHours(24);
    private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
    private boolean integrityCheckingEnabled = true;
    private boolean statisticsEnabled = true;
    private Duration maintenanceInterval = Duration.ofMinutes(10);
    private boolean persistenceEnabled = false;
    private String persistencePath = null;
    private int concurrencyLevel = Runtime.getRuntime().availableProcessors();
    private boolean compressionEnabled = true;

    /**
     * Sets the maximum number of entries the cache can hold.
     *
     * @param maxSize the maximum cache size
     * @return this builder
     */
    public CacheConfigurationBuilder maxSize(final long maxSize) {
      this.maxSize = maxSize;
      return this;
    }

    public CacheConfigurationBuilder maxMemoryUsage(final long maxMemoryUsage) {
      this.maxMemoryUsage = maxMemoryUsage;
      return this;
    }

    public CacheConfigurationBuilder expirationDuration(final Duration expirationDuration) {
      this.expirationDuration = expirationDuration;
      return this;
    }

    public CacheConfigurationBuilder expireAfter(final long duration, final TimeUnit unit) {
      this.expirationDuration = Duration.of(duration, unit.toChronoUnit());
      return this;
    }

    public CacheConfigurationBuilder evictionPolicy(final EvictionPolicy evictionPolicy) {
      this.evictionPolicy = evictionPolicy;
      return this;
    }

    public CacheConfigurationBuilder integrityChecking(final boolean enabled) {
      this.integrityCheckingEnabled = enabled;
      return this;
    }

    public CacheConfigurationBuilder statistics(final boolean enabled) {
      this.statisticsEnabled = enabled;
      return this;
    }

    public CacheConfigurationBuilder maintenanceInterval(final Duration maintenanceInterval) {
      this.maintenanceInterval = maintenanceInterval;
      return this;
    }

    public CacheConfigurationBuilder persistence(final boolean enabled, final String path) {
      this.persistenceEnabled = enabled;
      this.persistencePath = path;
      return this;
    }

    public CacheConfigurationBuilder concurrencyLevel(final int concurrencyLevel) {
      this.concurrencyLevel = concurrencyLevel;
      return this;
    }

    public CacheConfigurationBuilder compression(final boolean enabled) {
      this.compressionEnabled = enabled;
      return this;
    }

    /**
     * Builds the CacheConfiguration instance.
     *
     * @return a new CacheConfiguration with the configured settings
     */
    public CacheConfiguration build() {
      return new CacheConfigurationImpl(
          maxSize,
          maxMemoryUsage,
          expirationDuration,
          evictionPolicy,
          integrityCheckingEnabled,
          statisticsEnabled,
          maintenanceInterval,
          persistenceEnabled,
          persistencePath,
          concurrencyLevel,
          compressionEnabled);
    }
  }

  /**
   * Implementation of CacheConfiguration.
   */
  final class CacheConfigurationImpl implements CacheConfiguration {
    private final long maxSize;
    private final long maxMemoryUsage;
    private final Duration expirationDuration;
    private final EvictionPolicy evictionPolicy;
    private final boolean integrityCheckingEnabled;
    private final boolean statisticsEnabled;
    private final Duration maintenanceInterval;
    private final boolean persistenceEnabled;
    private final String persistencePath;
    private final int concurrencyLevel;
    private final boolean compressionEnabled;

    CacheConfigurationImpl(
        final long maxSize,
        final long maxMemoryUsage,
        final Duration expirationDuration,
        final EvictionPolicy evictionPolicy,
        final boolean integrityCheckingEnabled,
        final boolean statisticsEnabled,
        final Duration maintenanceInterval,
        final boolean persistenceEnabled,
        final String persistencePath,
        final int concurrencyLevel,
        final boolean compressionEnabled) {
      this.maxSize = maxSize;
      this.maxMemoryUsage = maxMemoryUsage;
      this.expirationDuration = expirationDuration;
      this.evictionPolicy = evictionPolicy;
      this.integrityCheckingEnabled = integrityCheckingEnabled;
      this.statisticsEnabled = statisticsEnabled;
      this.maintenanceInterval = maintenanceInterval;
      this.persistenceEnabled = persistenceEnabled;
      this.persistencePath = persistencePath;
      this.concurrencyLevel = concurrencyLevel;
      this.compressionEnabled = compressionEnabled;
    }

    @Override
    public long getMaxSize() {
      return maxSize;
    }

    @Override
    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }

    @Override
    public Duration getExpirationDuration() {
      return expirationDuration;
    }

    @Override
    public EvictionPolicy getEvictionPolicy() {
      return evictionPolicy;
    }

    @Override
    public boolean isIntegrityCheckingEnabled() {
      return integrityCheckingEnabled;
    }

    @Override
    public boolean isStatisticsEnabled() {
      return statisticsEnabled;
    }

    @Override
    public Duration getMaintenanceInterval() {
      return maintenanceInterval;
    }

    @Override
    public boolean isPersistenceEnabled() {
      return persistenceEnabled;
    }

    @Override
    public String getPersistencePath() {
      return persistencePath;
    }

    @Override
    public int getConcurrencyLevel() {
      return concurrencyLevel;
    }

    @Override
    public boolean isCompressionEnabled() {
      return compressionEnabled;
    }
  }
}