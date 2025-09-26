package ai.tegmentum.wasmtime4j.debug;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for source map integration caching.
 *
 * <p>This class provides configuration options for controlling the behavior
 * of various caches used by the source map integration system, including
 * source map cache, symbol cache, and file content cache.
 *
 * @since 1.0.0
 */
public final class CacheConfiguration {

    private final int maxSourceMaps;
    private final int maxSymbols;
    private final int maxFiles;
    private final Duration timeToLive;
    private final boolean enableWeakReferences;
    private final CacheEvictionPolicy evictionPolicy;

    /**
     * Cache eviction policies.
     */
    public enum CacheEvictionPolicy {
        /** Least Recently Used */
        LRU,
        /** Least Frequently Used */
        LFU,
        /** First In, First Out */
        FIFO,
        /** Random eviction */
        RANDOM
    }

    /**
     * Creates a new cache configuration.
     *
     * @param maxSourceMaps maximum number of cached source maps
     * @param maxSymbols maximum number of cached symbols
     * @param maxFiles maximum number of cached files
     * @param timeToLive cache entry time to live
     * @param enableWeakReferences whether to use weak references
     * @param evictionPolicy the cache eviction policy
     */
    public CacheConfiguration(
            final int maxSourceMaps,
            final int maxSymbols,
            final int maxFiles,
            final Duration timeToLive,
            final boolean enableWeakReferences,
            final CacheEvictionPolicy evictionPolicy) {
        if (maxSourceMaps < 0) {
            throw new IllegalArgumentException("Max source maps cannot be negative: " + maxSourceMaps);
        }
        if (maxSymbols < 0) {
            throw new IllegalArgumentException("Max symbols cannot be negative: " + maxSymbols);
        }
        if (maxFiles < 0) {
            throw new IllegalArgumentException("Max files cannot be negative: " + maxFiles);
        }
        if (timeToLive != null && timeToLive.isNegative()) {
            throw new IllegalArgumentException("Time to live cannot be negative: " + timeToLive);
        }

        this.maxSourceMaps = maxSourceMaps;
        this.maxSymbols = maxSymbols;
        this.maxFiles = maxFiles;
        this.timeToLive = timeToLive;
        this.enableWeakReferences = enableWeakReferences;
        this.evictionPolicy = evictionPolicy != null ? evictionPolicy : CacheEvictionPolicy.LRU;
    }

    /**
     * Gets the maximum number of cached source maps.
     *
     * @return the maximum source maps
     */
    public int getMaxSourceMaps() {
        return maxSourceMaps;
    }

    /**
     * Gets the maximum number of cached symbols.
     *
     * @return the maximum symbols
     */
    public int getMaxSymbols() {
        return maxSymbols;
    }

    /**
     * Gets the maximum number of cached files.
     *
     * @return the maximum files
     */
    public int getMaxFiles() {
        return maxFiles;
    }

    /**
     * Gets the cache entry time to live.
     *
     * @return the time to live, or null if unlimited
     */
    public Duration getTimeToLive() {
        return timeToLive;
    }

    /**
     * Checks if weak references are enabled.
     *
     * @return true if weak references are enabled
     */
    public boolean isWeakReferencesEnabled() {
        return enableWeakReferences;
    }

    /**
     * Gets the cache eviction policy.
     *
     * @return the eviction policy
     */
    public CacheEvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    /**
     * Creates a copy with updated maximum source maps.
     *
     * @param maxSourceMaps the new maximum source maps
     * @return a new configuration with updated value
     */
    public CacheConfiguration withMaxSourceMaps(final int maxSourceMaps) {
        return new CacheConfiguration(
                maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy);
    }

    /**
     * Creates a copy with updated maximum symbols.
     *
     * @param maxSymbols the new maximum symbols
     * @return a new configuration with updated value
     */
    public CacheConfiguration withMaxSymbols(final int maxSymbols) {
        return new CacheConfiguration(
                maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy);
    }

    /**
     * Creates a copy with updated maximum files.
     *
     * @param maxFiles the new maximum files
     * @return a new configuration with updated value
     */
    public CacheConfiguration withMaxFiles(final int maxFiles) {
        return new CacheConfiguration(
                maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy);
    }

    /**
     * Creates a copy with updated time to live.
     *
     * @param timeToLive the new time to live
     * @return a new configuration with updated value
     */
    public CacheConfiguration withTimeToLive(final Duration timeToLive) {
        return new CacheConfiguration(
                maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy);
    }

    /**
     * Creates a copy with updated weak references setting.
     *
     * @param enableWeakReferences the new weak references setting
     * @return a new configuration with updated value
     */
    public CacheConfiguration withWeakReferences(final boolean enableWeakReferences) {
        return new CacheConfiguration(
                maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy);
    }

    /**
     * Creates a copy with updated eviction policy.
     *
     * @param evictionPolicy the new eviction policy
     * @return a new configuration with updated value
     */
    public CacheConfiguration withEvictionPolicy(final CacheEvictionPolicy evictionPolicy) {
        return new CacheConfiguration(
                maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy);
    }

    /**
     * Creates a default cache configuration.
     *
     * @return a default configuration
     */
    public static CacheConfiguration defaults() {
        return new CacheConfiguration(
                1000, // maxSourceMaps
                10000, // maxSymbols
                5000, // maxFiles
                Duration.ofHours(1), // timeToLive
                false, // enableWeakReferences
                CacheEvictionPolicy.LRU // evictionPolicy
        );
    }

    /**
     * Creates a configuration for memory-constrained environments.
     *
     * @return a memory-constrained configuration
     */
    public static CacheConfiguration memoryConstrained() {
        return new CacheConfiguration(
                100, // maxSourceMaps
                1000, // maxSymbols
                500, // maxFiles
                Duration.ofMinutes(30), // timeToLive
                true, // enableWeakReferences
                CacheEvictionPolicy.LRU // evictionPolicy
        );
    }

    /**
     * Creates a configuration for high-performance environments.
     *
     * @return a high-performance configuration
     */
    public static CacheConfiguration highPerformance() {
        return new CacheConfiguration(
                5000, // maxSourceMaps
                50000, // maxSymbols
                25000, // maxFiles
                Duration.ofHours(4), // timeToLive
                false, // enableWeakReferences
                CacheEvictionPolicy.LFU // evictionPolicy
        );
    }

    /**
     * Creates a configuration with no caching.
     *
     * @return a no-cache configuration
     */
    public static CacheConfiguration noCache() {
        return new CacheConfiguration(
                0, // maxSourceMaps
                0, // maxSymbols
                0, // maxFiles
                Duration.ZERO, // timeToLive
                false, // enableWeakReferences
                CacheEvictionPolicy.LRU // evictionPolicy
        );
    }

    /**
     * Creates a builder for custom configurations.
     *
     * @return a new configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for cache configurations.
     */
    public static final class Builder {
        private int maxSourceMaps = 1000;
        private int maxSymbols = 10000;
        private int maxFiles = 5000;
        private Duration timeToLive = Duration.ofHours(1);
        private boolean enableWeakReferences = false;
        private CacheEvictionPolicy evictionPolicy = CacheEvictionPolicy.LRU;

        private Builder() {
            // Use CacheConfiguration.builder()
        }

        /**
         * Sets the maximum number of cached source maps.
         *
         * @param maxSourceMaps the maximum source maps
         * @return this builder
         */
        public Builder maxSourceMaps(final int maxSourceMaps) {
            this.maxSourceMaps = maxSourceMaps;
            return this;
        }

        /**
         * Sets the maximum number of cached symbols.
         *
         * @param maxSymbols the maximum symbols
         * @return this builder
         */
        public Builder maxSymbols(final int maxSymbols) {
            this.maxSymbols = maxSymbols;
            return this;
        }

        /**
         * Sets the maximum number of cached files.
         *
         * @param maxFiles the maximum files
         * @return this builder
         */
        public Builder maxFiles(final int maxFiles) {
            this.maxFiles = maxFiles;
            return this;
        }

        /**
         * Sets the cache entry time to live.
         *
         * @param timeToLive the time to live
         * @return this builder
         */
        public Builder timeToLive(final Duration timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        /**
         * Enables or disables weak references.
         *
         * @param enableWeakReferences true to enable weak references
         * @return this builder
         */
        public Builder weakReferences(final boolean enableWeakReferences) {
            this.enableWeakReferences = enableWeakReferences;
            return this;
        }

        /**
         * Sets the cache eviction policy.
         *
         * @param evictionPolicy the eviction policy
         * @return this builder
         */
        public Builder evictionPolicy(final CacheEvictionPolicy evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }

        /**
         * Builds the cache configuration.
         *
         * @return a new cache configuration
         */
        public CacheConfiguration build() {
            return new CacheConfiguration(
                    maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final CacheConfiguration that = (CacheConfiguration) obj;
        return maxSourceMaps == that.maxSourceMaps
                && maxSymbols == that.maxSymbols
                && maxFiles == that.maxFiles
                && enableWeakReferences == that.enableWeakReferences
                && Objects.equals(timeToLive, that.timeToLive)
                && evictionPolicy == that.evictionPolicy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy);
    }

    @Override
    public String toString() {
        return String.format(
                "CacheConfiguration{maxSourceMaps=%d, maxSymbols=%d, maxFiles=%d, timeToLive=%s, " +
                "weakReferences=%s, evictionPolicy=%s}",
                maxSourceMaps, maxSymbols, maxFiles, timeToLive, enableWeakReferences, evictionPolicy
        );
    }
}