package ai.tegmentum.wasmtime4j.resource;

import java.time.Duration;

/**
 * Policy configuration for individual cache entries.
 *
 * <p>CachePolicy defines the behavior and characteristics of cached resources
 * including expiration, priority, and eviction settings.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * CachePolicy policy = CachePolicy.builder()
 *     .withTtl(Duration.ofHours(6))
 *     .withPriority(CachePriority.HIGH)
 *     .withRefreshOnAccess(true)
 *     .withEvictionWeight(0.8)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class CachePolicy {

    /**
     * Default time-to-live for cache entries.
     */
    public static final Duration DEFAULT_TTL = Duration.ofHours(1);

    /**
     * Default cache priority.
     */
    public static final CachePriority DEFAULT_PRIORITY = CachePriority.NORMAL;

    /**
     * Default eviction weight.
     */
    public static final double DEFAULT_EVICTION_WEIGHT = 1.0;

    private final Duration ttl;
    private final CachePriority priority;
    private final boolean refreshOnAccess;
    private final boolean refreshOnWrite;
    private final double evictionWeight;
    private final Duration maxIdleTime;
    private final boolean persistable;
    private final int maxAccessCount;
    private final Duration refreshInterval;

    private CachePolicy(final Builder builder) {
        this.ttl = builder.ttl;
        this.priority = builder.priority;
        this.refreshOnAccess = builder.refreshOnAccess;
        this.refreshOnWrite = builder.refreshOnWrite;
        this.evictionWeight = builder.evictionWeight;
        this.maxIdleTime = builder.maxIdleTime;
        this.persistable = builder.persistable;
        this.maxAccessCount = builder.maxAccessCount;
        this.refreshInterval = builder.refreshInterval;
    }

    /**
     * Creates a new cache policy builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a cache policy with default settings.
     *
     * @return default cache policy
     */
    public static CachePolicy defaultPolicy() {
        return builder().build();
    }

    /**
     * Creates a cache policy for high-priority resources.
     *
     * @return high-priority cache policy
     */
    public static CachePolicy highPriority() {
        return builder()
                .withPriority(CachePriority.HIGH)
                .withTtl(Duration.ofHours(24))
                .withEvictionWeight(0.1) // Low weight = harder to evict
                .build();
    }

    /**
     * Creates a cache policy for low-priority resources.
     *
     * @return low-priority cache policy
     */
    public static CachePolicy lowPriority() {
        return builder()
                .withPriority(CachePriority.LOW)
                .withTtl(Duration.ofMinutes(15))
                .withEvictionWeight(2.0) // High weight = easier to evict
                .build();
    }

    /**
     * Creates a cache policy for ephemeral resources.
     *
     * @return ephemeral cache policy
     */
    public static CachePolicy ephemeral() {
        return builder()
                .withTtl(Duration.ofMinutes(5))
                .withPriority(CachePriority.LOW)
                .withMaxIdleTime(Duration.ofMinutes(2))
                .withEvictionWeight(3.0)
                .build();
    }

    /**
     * Creates a cache policy for persistent resources.
     *
     * @return persistent cache policy
     */
    public static CachePolicy persistent() {
        return builder()
                .withTtl(Duration.ofDays(30))
                .withPriority(CachePriority.HIGH)
                .withPersistable(true)
                .withEvictionWeight(0.2)
                .build();
    }

    /**
     * Gets the time-to-live for cache entries.
     *
     * @return the TTL duration
     */
    public Duration getTtl() {
        return ttl;
    }

    /**
     * Gets the cache priority.
     *
     * @return the cache priority
     */
    public CachePriority getPriority() {
        return priority;
    }

    /**
     * Checks if entries should be refreshed on access.
     *
     * @return true if refresh on access is enabled
     */
    public boolean isRefreshOnAccess() {
        return refreshOnAccess;
    }

    /**
     * Checks if entries should be refreshed on write.
     *
     * @return true if refresh on write is enabled
     */
    public boolean isRefreshOnWrite() {
        return refreshOnWrite;
    }

    /**
     * Gets the eviction weight for this entry.
     *
     * <p>Higher weights make entries more likely to be evicted.
     * Lower weights make entries less likely to be evicted.
     *
     * @return the eviction weight
     */
    public double getEvictionWeight() {
        return evictionWeight;
    }

    /**
     * Gets the maximum idle time before eviction.
     *
     * @return the maximum idle time, or null if no limit
     */
    public Duration getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Checks if this entry can be persisted to disk.
     *
     * @return true if the entry is persistable
     */
    public boolean isPersistable() {
        return persistable;
    }

    /**
     * Gets the maximum access count before eviction.
     *
     * @return the maximum access count, or -1 if no limit
     */
    public int getMaxAccessCount() {
        return maxAccessCount;
    }

    /**
     * Gets the refresh interval for background refresh.
     *
     * @return the refresh interval, or null if no background refresh
     */
    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * Creates a copy of this policy with modified TTL.
     *
     * @param newTtl the new TTL
     * @return a new cache policy with the modified TTL
     * @throws IllegalArgumentException if newTtl is null or negative
     */
    public CachePolicy withTtl(final Duration newTtl) {
        return toBuilder().withTtl(newTtl).build();
    }

    /**
     * Creates a copy of this policy with modified priority.
     *
     * @param newPriority the new priority
     * @return a new cache policy with the modified priority
     * @throws IllegalArgumentException if newPriority is null
     */
    public CachePolicy withPriority(final CachePriority newPriority) {
        return toBuilder().withPriority(newPriority).build();
    }

    /**
     * Creates a builder initialized with this policy's settings.
     *
     * @return a builder with this policy's settings
     */
    public Builder toBuilder() {
        return new Builder()
                .withTtl(ttl)
                .withPriority(priority)
                .withRefreshOnAccess(refreshOnAccess)
                .withRefreshOnWrite(refreshOnWrite)
                .withEvictionWeight(evictionWeight)
                .withMaxIdleTime(maxIdleTime)
                .withPersistable(persistable)
                .withMaxAccessCount(maxAccessCount)
                .withRefreshInterval(refreshInterval);
    }

    /**
     * Builder for creating cache policies.
     */
    public static final class Builder {
        private Duration ttl = DEFAULT_TTL;
        private CachePriority priority = DEFAULT_PRIORITY;
        private boolean refreshOnAccess = false;
        private boolean refreshOnWrite = false;
        private double evictionWeight = DEFAULT_EVICTION_WEIGHT;
        private Duration maxIdleTime;
        private boolean persistable = false;
        private int maxAccessCount = -1;
        private Duration refreshInterval;

        private Builder() {}

        /**
         * Sets the time-to-live for cache entries.
         *
         * @param ttl the TTL duration
         * @return this builder
         * @throws IllegalArgumentException if ttl is null or negative
         */
        public Builder withTtl(final Duration ttl) {
            if (ttl == null || ttl.isNegative()) {
                throw new IllegalArgumentException("TTL must be positive");
            }
            this.ttl = ttl;
            return this;
        }

        /**
         * Sets the cache priority.
         *
         * @param priority the cache priority
         * @return this builder
         * @throws IllegalArgumentException if priority is null
         */
        public Builder withPriority(final CachePriority priority) {
            if (priority == null) {
                throw new IllegalArgumentException("Priority cannot be null");
            }
            this.priority = priority;
            return this;
        }

        /**
         * Sets whether entries should be refreshed on access.
         *
         * @param refreshOnAccess true to enable refresh on access
         * @return this builder
         */
        public Builder withRefreshOnAccess(final boolean refreshOnAccess) {
            this.refreshOnAccess = refreshOnAccess;
            return this;
        }

        /**
         * Sets whether entries should be refreshed on write.
         *
         * @param refreshOnWrite true to enable refresh on write
         * @return this builder
         */
        public Builder withRefreshOnWrite(final boolean refreshOnWrite) {
            this.refreshOnWrite = refreshOnWrite;
            return this;
        }

        /**
         * Sets the eviction weight.
         *
         * @param evictionWeight the eviction weight (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if evictionWeight is not positive
         */
        public Builder withEvictionWeight(final double evictionWeight) {
            if (evictionWeight <= 0) {
                throw new IllegalArgumentException("Eviction weight must be positive");
            }
            this.evictionWeight = evictionWeight;
            return this;
        }

        /**
         * Sets the maximum idle time.
         *
         * @param maxIdleTime the maximum idle time
         * @return this builder
         * @throws IllegalArgumentException if maxIdleTime is negative
         */
        public Builder withMaxIdleTime(final Duration maxIdleTime) {
            if (maxIdleTime != null && maxIdleTime.isNegative()) {
                throw new IllegalArgumentException("Max idle time cannot be negative");
            }
            this.maxIdleTime = maxIdleTime;
            return this;
        }

        /**
         * Sets whether entries can be persisted.
         *
         * @param persistable true if entries can be persisted
         * @return this builder
         */
        public Builder withPersistable(final boolean persistable) {
            this.persistable = persistable;
            return this;
        }

        /**
         * Sets the maximum access count.
         *
         * @param maxAccessCount the maximum access count, or -1 for no limit
         * @return this builder
         * @throws IllegalArgumentException if maxAccessCount is less than -1
         */
        public Builder withMaxAccessCount(final int maxAccessCount) {
            if (maxAccessCount < -1) {
                throw new IllegalArgumentException("Max access count cannot be less than -1");
            }
            this.maxAccessCount = maxAccessCount;
            return this;
        }

        /**
         * Sets the refresh interval for background refresh.
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
         * Builds the cache policy.
         *
         * @return the cache policy
         */
        public CachePolicy build() {
            return new CachePolicy(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "CachePolicy{ttl=%s, priority=%s, refreshOnAccess=%s, refreshOnWrite=%s, " +
                "evictionWeight=%.2f, maxIdleTime=%s, persistable=%s, maxAccessCount=%d, " +
                "refreshInterval=%s}",
                ttl, priority, refreshOnAccess, refreshOnWrite, evictionWeight,
                maxIdleTime, persistable, maxAccessCount, refreshInterval);
    }
}