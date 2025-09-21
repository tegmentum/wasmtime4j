package ai.tegmentum.wasmtime4j.resource;

import java.time.Duration;

/**
 * Configuration settings for resource pools.
 *
 * <p>PoolConfiguration defines the operational parameters for resource pools
 * including size limits, timeout values, and cleanup policies.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PoolConfiguration config = PoolConfiguration.builder()
 *     .withMaxPoolSize(20)
 *     .withMinPoolSize(5)
 *     .withAcquisitionTimeout(Duration.ofSeconds(30))
 *     .withMaxIdleTime(Duration.ofMinutes(10))
 *     .withValidationInterval(Duration.ofMinutes(5))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PoolConfiguration {

    /**
     * Default maximum pool size.
     */
    public static final int DEFAULT_MAX_POOL_SIZE = 10;

    /**
     * Default minimum pool size.
     */
    public static final int DEFAULT_MIN_POOL_SIZE = 2;

    /**
     * Default acquisition timeout.
     */
    public static final Duration DEFAULT_ACQUISITION_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Default maximum idle time.
     */
    public static final Duration DEFAULT_MAX_IDLE_TIME = Duration.ofMinutes(15);

    /**
     * Default validation interval.
     */
    public static final Duration DEFAULT_VALIDATION_INTERVAL = Duration.ofMinutes(5);

    /**
     * Default cleanup interval.
     */
    public static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofMinutes(10);

    private final int maxPoolSize;
    private final int minPoolSize;
    private final Duration acquisitionTimeout;
    private final Duration maxIdleTime;
    private final Duration validationInterval;
    private final Duration cleanupInterval;
    private final boolean testOnAcquire;
    private final boolean testOnReturn;
    private final boolean testWhileIdle;
    private final boolean blockWhenExhausted;
    private final boolean fairness;
    private final int maxWaitingThreads;

    private PoolConfiguration(final Builder builder) {
        this.maxPoolSize = builder.maxPoolSize;
        this.minPoolSize = builder.minPoolSize;
        this.acquisitionTimeout = builder.acquisitionTimeout;
        this.maxIdleTime = builder.maxIdleTime;
        this.validationInterval = builder.validationInterval;
        this.cleanupInterval = builder.cleanupInterval;
        this.testOnAcquire = builder.testOnAcquire;
        this.testOnReturn = builder.testOnReturn;
        this.testWhileIdle = builder.testWhileIdle;
        this.blockWhenExhausted = builder.blockWhenExhausted;
        this.fairness = builder.fairness;
        this.maxWaitingThreads = builder.maxWaitingThreads;
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
     * @return default pool configuration
     */
    public static PoolConfiguration defaultConfiguration() {
        return builder().build();
    }

    /**
     * Gets the maximum number of resources that can be allocated from the pool.
     *
     * @return the maximum pool size
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Gets the minimum number of resources to maintain in the pool.
     *
     * @return the minimum pool size
     */
    public int getMinPoolSize() {
        return minPoolSize;
    }

    /**
     * Gets the maximum time to wait when acquiring a resource from the pool.
     *
     * @return the acquisition timeout
     */
    public Duration getAcquisitionTimeout() {
        return acquisitionTimeout;
    }

    /**
     * Gets the maximum time a resource can remain idle in the pool.
     *
     * @return the maximum idle time
     */
    public Duration getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Gets the interval between validation checks of idle resources.
     *
     * @return the validation interval
     */
    public Duration getValidationInterval() {
        return validationInterval;
    }

    /**
     * Gets the interval between cleanup operations.
     *
     * @return the cleanup interval
     */
    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    /**
     * Checks if resources should be validated when acquired from the pool.
     *
     * @return true if validation on acquire is enabled
     */
    public boolean isTestOnAcquire() {
        return testOnAcquire;
    }

    /**
     * Checks if resources should be validated when returned to the pool.
     *
     * @return true if validation on return is enabled
     */
    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    /**
     * Checks if idle resources should be validated periodically.
     *
     * @return true if validation of idle resources is enabled
     */
    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    /**
     * Checks if acquire requests should block when the pool is exhausted.
     *
     * @return true if blocking is enabled when pool is exhausted
     */
    public boolean isBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    /**
     * Checks if fair queueing is enabled for waiting threads.
     *
     * @return true if fair queueing is enabled
     */
    public boolean isFairness() {
        return fairness;
    }

    /**
     * Gets the maximum number of threads that can wait for resources.
     *
     * @return the maximum number of waiting threads
     */
    public int getMaxWaitingThreads() {
        return maxWaitingThreads;
    }

    /**
     * Creates a copy of this configuration with modified settings.
     *
     * @return a builder initialized with this configuration's settings
     */
    public Builder toBuilder() {
        return new Builder()
                .withMaxPoolSize(maxPoolSize)
                .withMinPoolSize(minPoolSize)
                .withAcquisitionTimeout(acquisitionTimeout)
                .withMaxIdleTime(maxIdleTime)
                .withValidationInterval(validationInterval)
                .withCleanupInterval(cleanupInterval)
                .withTestOnAcquire(testOnAcquire)
                .withTestOnReturn(testOnReturn)
                .withTestWhileIdle(testWhileIdle)
                .withBlockWhenExhausted(blockWhenExhausted)
                .withFairness(fairness)
                .withMaxWaitingThreads(maxWaitingThreads);
    }

    /**
     * Builder for creating pool configurations.
     */
    public static final class Builder {
        private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        private int minPoolSize = DEFAULT_MIN_POOL_SIZE;
        private Duration acquisitionTimeout = DEFAULT_ACQUISITION_TIMEOUT;
        private Duration maxIdleTime = DEFAULT_MAX_IDLE_TIME;
        private Duration validationInterval = DEFAULT_VALIDATION_INTERVAL;
        private Duration cleanupInterval = DEFAULT_CLEANUP_INTERVAL;
        private boolean testOnAcquire = true;
        private boolean testOnReturn = false;
        private boolean testWhileIdle = true;
        private boolean blockWhenExhausted = true;
        private boolean fairness = false;
        private int maxWaitingThreads = Integer.MAX_VALUE;

        private Builder() {}

        /**
         * Sets the maximum pool size.
         *
         * @param maxPoolSize the maximum pool size
         * @return this builder
         * @throws IllegalArgumentException if maxPoolSize is less than 1
         */
        public Builder withMaxPoolSize(final int maxPoolSize) {
            if (maxPoolSize < 1) {
                throw new IllegalArgumentException("Max pool size must be at least 1");
            }
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        /**
         * Sets the minimum pool size.
         *
         * @param minPoolSize the minimum pool size
         * @return this builder
         * @throws IllegalArgumentException if minPoolSize is negative
         */
        public Builder withMinPoolSize(final int minPoolSize) {
            if (minPoolSize < 0) {
                throw new IllegalArgumentException("Min pool size cannot be negative");
            }
            this.minPoolSize = minPoolSize;
            return this;
        }

        /**
         * Sets the acquisition timeout.
         *
         * @param acquisitionTimeout the acquisition timeout
         * @return this builder
         * @throws IllegalArgumentException if acquisitionTimeout is null or negative
         */
        public Builder withAcquisitionTimeout(final Duration acquisitionTimeout) {
            if (acquisitionTimeout == null || acquisitionTimeout.isNegative()) {
                throw new IllegalArgumentException("Acquisition timeout must be positive");
            }
            this.acquisitionTimeout = acquisitionTimeout;
            return this;
        }

        /**
         * Sets the maximum idle time.
         *
         * @param maxIdleTime the maximum idle time
         * @return this builder
         * @throws IllegalArgumentException if maxIdleTime is null or negative
         */
        public Builder withMaxIdleTime(final Duration maxIdleTime) {
            if (maxIdleTime == null || maxIdleTime.isNegative()) {
                throw new IllegalArgumentException("Max idle time must be positive");
            }
            this.maxIdleTime = maxIdleTime;
            return this;
        }

        /**
         * Sets the validation interval.
         *
         * @param validationInterval the validation interval
         * @return this builder
         * @throws IllegalArgumentException if validationInterval is null or negative
         */
        public Builder withValidationInterval(final Duration validationInterval) {
            if (validationInterval == null || validationInterval.isNegative()) {
                throw new IllegalArgumentException("Validation interval must be positive");
            }
            this.validationInterval = validationInterval;
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
         * Sets whether to test resources on acquire.
         *
         * @param testOnAcquire true to enable testing on acquire
         * @return this builder
         */
        public Builder withTestOnAcquire(final boolean testOnAcquire) {
            this.testOnAcquire = testOnAcquire;
            return this;
        }

        /**
         * Sets whether to test resources on return.
         *
         * @param testOnReturn true to enable testing on return
         * @return this builder
         */
        public Builder withTestOnReturn(final boolean testOnReturn) {
            this.testOnReturn = testOnReturn;
            return this;
        }

        /**
         * Sets whether to test idle resources.
         *
         * @param testWhileIdle true to enable testing while idle
         * @return this builder
         */
        public Builder withTestWhileIdle(final boolean testWhileIdle) {
            this.testWhileIdle = testWhileIdle;
            return this;
        }

        /**
         * Sets whether to block when the pool is exhausted.
         *
         * @param blockWhenExhausted true to enable blocking when exhausted
         * @return this builder
         */
        public Builder withBlockWhenExhausted(final boolean blockWhenExhausted) {
            this.blockWhenExhausted = blockWhenExhausted;
            return this;
        }

        /**
         * Sets whether to use fair queueing for waiting threads.
         *
         * @param fairness true to enable fair queueing
         * @return this builder
         */
        public Builder withFairness(final boolean fairness) {
            this.fairness = fairness;
            return this;
        }

        /**
         * Sets the maximum number of waiting threads.
         *
         * @param maxWaitingThreads the maximum number of waiting threads
         * @return this builder
         * @throws IllegalArgumentException if maxWaitingThreads is less than 1
         */
        public Builder withMaxWaitingThreads(final int maxWaitingThreads) {
            if (maxWaitingThreads < 1) {
                throw new IllegalArgumentException("Max waiting threads must be at least 1");
            }
            this.maxWaitingThreads = maxWaitingThreads;
            return this;
        }

        /**
         * Builds the pool configuration.
         *
         * @return the pool configuration
         * @throws IllegalArgumentException if the configuration is invalid
         */
        public PoolConfiguration build() {
            if (minPoolSize > maxPoolSize) {
                throw new IllegalArgumentException("Min pool size cannot be greater than max pool size");
            }
            return new PoolConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "PoolConfiguration{maxPoolSize=%d, minPoolSize=%d, acquisitionTimeout=%s, " +
                "maxIdleTime=%s, validationInterval=%s, cleanupInterval=%s, testOnAcquire=%s, " +
                "testOnReturn=%s, testWhileIdle=%s, blockWhenExhausted=%s, fairness=%s, " +
                "maxWaitingThreads=%d}",
                maxPoolSize, minPoolSize, acquisitionTimeout, maxIdleTime, validationInterval,
                cleanupInterval, testOnAcquire, testOnReturn, testWhileIdle, blockWhenExhausted,
                fairness, maxWaitingThreads);
    }
}