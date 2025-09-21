package ai.tegmentum.wasmtime4j.resource;

import java.time.Duration;

/**
 * Configuration settings for resource caches.
 *
 * <p>CacheConfiguration defines the operational parameters for resource caches
 * including size limits, eviction policies, and performance settings.
 *
 * @since 1.0.0
 */
public final class CacheConfiguration {

    /**
     * Default maximum number of cache entries.
     */
    public static final int DEFAULT_MAX_ENTRIES = 1000;

    /**
     * Default maximum memory usage in bytes (64MB).
     */
    public static final long DEFAULT_MAX_MEMORY_USAGE = 64L * 1024 * 1024;

    /**
     * Default time-to-live for cache entries.
     */
    public static final Duration DEFAULT_TTL = Duration.ofHours(1);

    /**
     * Default cleanup interval.
     */
    public static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofMinutes(5);

    private final int maxEntries;
    private final long maxMemoryUsage;
    private final Duration defaultTtl;
    private final EvictionPolicy evictionPolicy;
    private final Duration cleanupInterval;
    private final boolean statisticsEnabled;
    private final boolean persistenceEnabled;
    private final String persistenceLocation;
    private final int concurrencyLevel;
    private final boolean weakKeys;
    private final boolean weakValues;
    private final double evictionThreshold;
    private final Duration refreshInterval;

    private CacheConfiguration(final Builder builder) {
        this.maxEntries = builder.maxEntries;
        this.maxMemoryUsage = builder.maxMemoryUsage;
        this.defaultTtl = builder.defaultTtl;
        this.evictionPolicy = builder.evictionPolicy;
        this.cleanupInterval = builder.cleanupInterval;
        this.statisticsEnabled = builder.statisticsEnabled;
        this.persistenceEnabled = builder.persistenceEnabled;
        this.persistenceLocation = builder.persistenceLocation;
        this.concurrencyLevel = builder.concurrencyLevel;
        this.weakKeys = builder.weakKeys;
        this.weakValues = builder.weakValues;
        this.evictionThreshold = builder.evictionThreshold;
        this.refreshInterval = builder.refreshInterval;
    }

    /**
     * Creates a new configuration builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a configuration with default settings.
     *
     * @return default cache configuration
     */
    public static CacheConfiguration defaultConfiguration() {
        return builder().build();
    }

    /**
     * Gets the maximum number of entries allowed in the cache.
     *
     * @return the maximum entry count
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * Gets the maximum memory usage allowed for the cache in bytes.
     *
     * @return the maximum memory usage
     */
    public long getMaxMemoryUsage() {
        return maxMemoryUsage;
    }

    /**
     * Gets the default time-to-live for cache entries.
     *
     * @return the default TTL
     */
    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    /**
     * Gets the eviction policy for the cache.
     *
     * @return the eviction policy
     */
    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    /**
     * Gets the cleanup interval for expired entries.
     *
     * @return the cleanup interval
     */
    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    /**
     * Checks if statistics collection is enabled.
     *
     * @return true if statistics are enabled
     */
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    /**
     * Checks if persistence is enabled.
     *
     * @return true if persistence is enabled
     */
    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    /**
     * Gets the persistence location for cache data.
     *
     * @return the persistence location, or null if not configured
     */
    public String getPersistenceLocation() {
        return persistenceLocation;
    }

    /**
     * Gets the concurrency level for the cache.
     *
     * @return the concurrency level
     */
    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }

    /**
     * Checks if weak references are used for keys.
     *
     * @return true if weak keys are enabled
     */
    public boolean isWeakKeys() {
        return weakKeys;
    }

    /**
     * Checks if weak references are used for values.
     *
     * @return true if weak values are enabled
     */
    public boolean isWeakValues() {
        return weakValues;
    }

    /**
     * Gets the eviction threshold as a percentage (0.0 to 1.0).
     *
     * <p>When cache utilization exceeds this threshold, aggressive
     * eviction is triggered.
     *
     * @return the eviction threshold
     */
    public double getEvictionThreshold() {
        return evictionThreshold;
    }

    /**
     * Gets the refresh interval for background cache operations.
     *
     * @return the refresh interval, or null if not configured
     */
    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * Creates a copy of this configuration with modified settings.
     *
     * @return a builder initialized with this configuration's settings
     */
    public Builder toBuilder() {
        return new Builder()
                .withMaxEntries(maxEntries)
                .withMaxMemoryUsage(maxMemoryUsage)
                .withDefaultTtl(defaultTtl)
                .withEvictionPolicy(evictionPolicy)
                .withCleanupInterval(cleanupInterval)
                .withStatisticsEnabled(statisticsEnabled)
                .withPersistenceEnabled(persistenceEnabled)
                .withPersistenceLocation(persistenceLocation)
                .withConcurrencyLevel(concurrencyLevel)
                .withWeakKeys(weakKeys)
                .withWeakValues(weakValues)
                .withEvictionThreshold(evictionThreshold)
                .withRefreshInterval(refreshInterval);
    }

    /**
     * Builder for creating cache configurations.
     */
    public static final class Builder {
        private int maxEntries = DEFAULT_MAX_ENTRIES;
        private long maxMemoryUsage = DEFAULT_MAX_MEMORY_USAGE;
        private Duration defaultTtl = DEFAULT_TTL;
        private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
        private Duration cleanupInterval = DEFAULT_CLEANUP_INTERVAL;
        private boolean statisticsEnabled = true;
        private boolean persistenceEnabled = false;
        private String persistenceLocation;
        private int concurrencyLevel = Runtime.getRuntime().availableProcessors();
        private boolean weakKeys = false;
        private boolean weakValues = false;
        private double evictionThreshold = 0.8;
        private Duration refreshInterval;

        private Builder() {}

        /**
         * Sets the maximum number of entries.
         *
         * @param maxEntries the maximum entry count
         * @return this builder
         * @throws IllegalArgumentException if maxEntries is less than 1
         */
        public Builder withMaxEntries(final int maxEntries) {
            if (maxEntries < 1) {
                throw new IllegalArgumentException("Max entries must be at least 1");
            }
            this.maxEntries = maxEntries;
            return this;
        }

        /**
         * Sets the maximum memory usage.
         *
         * @param maxMemoryUsage the maximum memory usage in bytes
         * @return this builder
         * @throws IllegalArgumentException if maxMemoryUsage is less than 1
         */
        public Builder withMaxMemoryUsage(final long maxMemoryUsage) {
            if (maxMemoryUsage < 1) {
                throw new IllegalArgumentException("Max memory usage must be at least 1 byte");
            }
            this.maxMemoryUsage = maxMemoryUsage;
            return this;
        }

        /**
         * Sets the default time-to-live.
         *
         * @param defaultTtl the default TTL
         * @return this builder
         * @throws IllegalArgumentException if defaultTtl is null or negative
         */
        public Builder withDefaultTtl(final Duration defaultTtl) {
            if (defaultTtl == null || defaultTtl.isNegative()) {
                throw new IllegalArgumentException("Default TTL must be positive");
            }
            this.defaultTtl = defaultTtl;
            return this;
        }

        /**
         * Sets the eviction policy.
         *
         * @param evictionPolicy the eviction policy
         * @return this builder
         * @throws IllegalArgumentException if evictionPolicy is null
         */
        public Builder withEvictionPolicy(final EvictionPolicy evictionPolicy) {
            if (evictionPolicy == null) {
                throw new IllegalArgumentException("Eviction policy cannot be null");
            }
            this.evictionPolicy = evictionPolicy;
            return this;
        }

        /**
         * Sets the cleanup interval.
         *
         * @param cleanupInterval the cleanup interval
         * @return this builder
         * @throws IllegalArgumentException if cleanupInterval is null or negative
         */
        public Builder withCleanupInterval(final Duration cleanupInterval) {
            if (cleanupInterval == null || cleanupInterval.isNegative()) {
                throw new IllegalArgumentException("Cleanup interval must be positive");
            }
            this.cleanupInterval = cleanupInterval;
            return this;
        }

        /**
         * Sets whether statistics collection is enabled.
         *
         * @param statisticsEnabled true to enable statistics
         * @return this builder
         */
        public Builder withStatisticsEnabled(final boolean statisticsEnabled) {
            this.statisticsEnabled = statisticsEnabled;
            return this;
        }

        /**
         * Sets whether persistence is enabled.
         *
         * @param persistenceEnabled true to enable persistence
         * @return this builder
         */
        public Builder withPersistenceEnabled(final boolean persistenceEnabled) {
            this.persistenceEnabled = persistenceEnabled;
            return this;
        }

        /**
         * Sets the persistence location.
         *
         * @param persistenceLocation the persistence location
         * @return this builder
         */
        public Builder withPersistenceLocation(final String persistenceLocation) {
            this.persistenceLocation = persistenceLocation;
            return this;
        }

        /**
         * Sets the concurrency level.
         *
         * @param concurrencyLevel the concurrency level
         * @return this builder
         * @throws IllegalArgumentException if concurrencyLevel is less than 1
         */
        public Builder withConcurrencyLevel(final int concurrencyLevel) {
            if (concurrencyLevel < 1) {
                throw new IllegalArgumentException("Concurrency level must be at least 1");
            }
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }

        /**
         * Sets whether to use weak references for keys.
         *
         * @param weakKeys true to use weak keys
         * @return this builder
         */
        public Builder withWeakKeys(final boolean weakKeys) {
            this.weakKeys = weakKeys;
            return this;
        }

        /**
         * Sets whether to use weak references for values.
         *
         * @param weakValues true to use weak values
         * @return this builder
         */
        public Builder withWeakValues(final boolean weakValues) {
            this.weakValues = weakValues;
            return this;
        }

        /**
         * Sets the eviction threshold.
         *
         * @param evictionThreshold the eviction threshold (0.0 to 1.0)
         * @return this builder
         * @throws IllegalArgumentException if evictionThreshold is not between 0.0 and 1.0
         */
        public Builder withEvictionThreshold(final double evictionThreshold) {
            if (evictionThreshold < 0.0 || evictionThreshold > 1.0) {
                throw new IllegalArgumentException("Eviction threshold must be between 0.0 and 1.0");
            }
            this.evictionThreshold = evictionThreshold;
            return this;
        }

        /**
         * Sets the refresh interval.
         *
         * @param refreshInterval the refresh interval
         * @return this builder
         * @throws IllegalArgumentException if refreshInterval is negative
         */
        public Builder withRefreshInterval(final Duration refreshInterval) {
            if (refreshInterval != null && refreshInterval.isNegative()) {
                throw new IllegalArgumentException("Refresh interval cannot be negative");
            }
            this.refreshInterval = refreshInterval;
            return this;
        }

        /**
         * Builds the cache configuration.
         *
         * @return the cache configuration
         */
        public CacheConfiguration build() {
            return new CacheConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "CacheConfiguration{maxEntries=%d, maxMemoryUsage=%d, defaultTtl=%s, " +
                "evictionPolicy=%s, cleanupInterval=%s, statisticsEnabled=%s, " +
                "persistenceEnabled=%s, concurrencyLevel=%d, evictionThreshold=%.2f}",
                maxEntries, maxMemoryUsage, defaultTtl, evictionPolicy, cleanupInterval,
                statisticsEnabled, persistenceEnabled, concurrencyLevel, evictionThreshold);
    }
}