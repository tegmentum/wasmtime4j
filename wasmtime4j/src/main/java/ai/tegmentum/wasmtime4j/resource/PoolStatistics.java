package ai.tegmentum.wasmtime4j.resource;

import java.time.Instant;

/**
 * Statistics and metrics for resource pool operations.
 *
 * <p>PoolStatistics provides comprehensive information about pool performance,
 * resource utilization, and operational metrics to support monitoring and tuning.
 *
 * @since 1.0.0
 */
public final class PoolStatistics {

    private final int totalSize;
    private final int activeCount;
    private final int idleCount;
    private final int maxSize;
    private final int minSize;
    private final long totalAcquisitions;
    private final long totalReturns;
    private final long totalCreations;
    private final long totalDestructions;
    private final long totalValidations;
    private final long validationFailures;
    private final long totalTimeouts;
    private final long totalWaitTime;
    private final long averageWaitTime;
    private final long maxWaitTime;
    private final long totalActiveTime;
    private final long averageActiveTime;
    private final long maxActiveTime;
    private final int currentWaitingThreads;
    private final int maxWaitingThreads;
    private final Instant lastAcquisition;
    private final Instant lastReturn;
    private final Instant lastCreation;
    private final Instant lastValidation;
    private final Instant statisticsTimestamp;

    private PoolStatistics(final Builder builder) {
        this.totalSize = builder.totalSize;
        this.activeCount = builder.activeCount;
        this.idleCount = builder.idleCount;
        this.maxSize = builder.maxSize;
        this.minSize = builder.minSize;
        this.totalAcquisitions = builder.totalAcquisitions;
        this.totalReturns = builder.totalReturns;
        this.totalCreations = builder.totalCreations;
        this.totalDestructions = builder.totalDestructions;
        this.totalValidations = builder.totalValidations;
        this.validationFailures = builder.validationFailures;
        this.totalTimeouts = builder.totalTimeouts;
        this.totalWaitTime = builder.totalWaitTime;
        this.averageWaitTime = builder.averageWaitTime;
        this.maxWaitTime = builder.maxWaitTime;
        this.totalActiveTime = builder.totalActiveTime;
        this.averageActiveTime = builder.averageActiveTime;
        this.maxActiveTime = builder.maxActiveTime;
        this.currentWaitingThreads = builder.currentWaitingThreads;
        this.maxWaitingThreads = builder.maxWaitingThreads;
        this.lastAcquisition = builder.lastAcquisition;
        this.lastReturn = builder.lastReturn;
        this.lastCreation = builder.lastCreation;
        this.lastValidation = builder.lastValidation;
        this.statisticsTimestamp = builder.statisticsTimestamp;
    }

    /**
     * Creates a new statistics builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the total number of resources in the pool.
     *
     * @return the total pool size
     */
    public int getTotalSize() {
        return totalSize;
    }

    /**
     * Gets the number of active (checked out) resources.
     *
     * @return the active resource count
     */
    public int getActiveCount() {
        return activeCount;
    }

    /**
     * Gets the number of idle (available) resources.
     *
     * @return the idle resource count
     */
    public int getIdleCount() {
        return idleCount;
    }

    /**
     * Gets the maximum configured pool size.
     *
     * @return the maximum pool size
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Gets the minimum configured pool size.
     *
     * @return the minimum pool size
     */
    public int getMinSize() {
        return minSize;
    }

    /**
     * Gets the total number of resource acquisitions.
     *
     * @return the total acquisition count
     */
    public long getTotalAcquisitions() {
        return totalAcquisitions;
    }

    /**
     * Gets the total number of resource returns.
     *
     * @return the total return count
     */
    public long getTotalReturns() {
        return totalReturns;
    }

    /**
     * Gets the total number of resources created.
     *
     * @return the total creation count
     */
    public long getTotalCreations() {
        return totalCreations;
    }

    /**
     * Gets the total number of resources destroyed.
     *
     * @return the total destruction count
     */
    public long getTotalDestructions() {
        return totalDestructions;
    }

    /**
     * Gets the total number of validations performed.
     *
     * @return the total validation count
     */
    public long getTotalValidations() {
        return totalValidations;
    }

    /**
     * Gets the number of validation failures.
     *
     * @return the validation failure count
     */
    public long getValidationFailures() {
        return validationFailures;
    }

    /**
     * Gets the total number of acquisition timeouts.
     *
     * @return the timeout count
     */
    public long getTotalTimeouts() {
        return totalTimeouts;
    }

    /**
     * Gets the total time spent waiting for resources (in milliseconds).
     *
     * @return the total wait time
     */
    public long getTotalWaitTime() {
        return totalWaitTime;
    }

    /**
     * Gets the average time spent waiting for resources (in milliseconds).
     *
     * @return the average wait time
     */
    public long getAverageWaitTime() {
        return averageWaitTime;
    }

    /**
     * Gets the maximum time spent waiting for a resource (in milliseconds).
     *
     * @return the maximum wait time
     */
    public long getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * Gets the total time resources have been active (in milliseconds).
     *
     * @return the total active time
     */
    public long getTotalActiveTime() {
        return totalActiveTime;
    }

    /**
     * Gets the average time resources are active (in milliseconds).
     *
     * @return the average active time
     */
    public long getAverageActiveTime() {
        return averageActiveTime;
    }

    /**
     * Gets the maximum time a resource was active (in milliseconds).
     *
     * @return the maximum active time
     */
    public long getMaxActiveTime() {
        return maxActiveTime;
    }

    /**
     * Gets the current number of threads waiting for resources.
     *
     * @return the current waiting thread count
     */
    public int getCurrentWaitingThreads() {
        return currentWaitingThreads;
    }

    /**
     * Gets the maximum number of threads that have waited concurrently.
     *
     * @return the maximum waiting thread count
     */
    public int getMaxWaitingThreads() {
        return maxWaitingThreads;
    }

    /**
     * Gets the timestamp of the last resource acquisition.
     *
     * @return the last acquisition time, or null if none
     */
    public Instant getLastAcquisition() {
        return lastAcquisition;
    }

    /**
     * Gets the timestamp of the last resource return.
     *
     * @return the last return time, or null if none
     */
    public Instant getLastReturn() {
        return lastReturn;
    }

    /**
     * Gets the timestamp of the last resource creation.
     *
     * @return the last creation time, or null if none
     */
    public Instant getLastCreation() {
        return lastCreation;
    }

    /**
     * Gets the timestamp of the last validation.
     *
     * @return the last validation time, or null if none
     */
    public Instant getLastValidation() {
        return lastValidation;
    }

    /**
     * Gets the timestamp when these statistics were collected.
     *
     * @return the statistics timestamp
     */
    public Instant getStatisticsTimestamp() {
        return statisticsTimestamp;
    }

    /**
     * Calculates the pool utilization as a percentage.
     *
     * @return the pool utilization (0.0 to 1.0)
     */
    public double getUtilization() {
        if (maxSize == 0) {
            return 0.0;
        }
        return (double) activeCount / maxSize;
    }

    /**
     * Calculates the validation success rate as a percentage.
     *
     * @return the validation success rate (0.0 to 1.0)
     */
    public double getValidationSuccessRate() {
        if (totalValidations == 0) {
            return 1.0; // No validations means 100% success
        }
        return (double) (totalValidations - validationFailures) / totalValidations;
    }

    /**
     * Calculates the average acquisition rate (acquisitions per second).
     *
     * @param timeWindowSeconds the time window in seconds
     * @return the acquisition rate
     */
    public double getAcquisitionRate(final long timeWindowSeconds) {
        if (timeWindowSeconds <= 0) {
            return 0.0;
        }
        return (double) totalAcquisitions / timeWindowSeconds;
    }

    /**
     * Builder for creating pool statistics.
     */
    public static final class Builder {
        private int totalSize;
        private int activeCount;
        private int idleCount;
        private int maxSize;
        private int minSize;
        private long totalAcquisitions;
        private long totalReturns;
        private long totalCreations;
        private long totalDestructions;
        private long totalValidations;
        private long validationFailures;
        private long totalTimeouts;
        private long totalWaitTime;
        private long averageWaitTime;
        private long maxWaitTime;
        private long totalActiveTime;
        private long averageActiveTime;
        private long maxActiveTime;
        private int currentWaitingThreads;
        private int maxWaitingThreads;
        private Instant lastAcquisition;
        private Instant lastReturn;
        private Instant lastCreation;
        private Instant lastValidation;
        private Instant statisticsTimestamp = Instant.now();

        private Builder() {}

        public Builder withTotalSize(final int totalSize) {
            this.totalSize = totalSize;
            return this;
        }

        public Builder withActiveCount(final int activeCount) {
            this.activeCount = activeCount;
            return this;
        }

        public Builder withIdleCount(final int idleCount) {
            this.idleCount = idleCount;
            return this;
        }

        public Builder withMaxSize(final int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder withMinSize(final int minSize) {
            this.minSize = minSize;
            return this;
        }

        public Builder withTotalAcquisitions(final long totalAcquisitions) {
            this.totalAcquisitions = totalAcquisitions;
            return this;
        }

        public Builder withTotalReturns(final long totalReturns) {
            this.totalReturns = totalReturns;
            return this;
        }

        public Builder withTotalCreations(final long totalCreations) {
            this.totalCreations = totalCreations;
            return this;
        }

        public Builder withTotalDestructions(final long totalDestructions) {
            this.totalDestructions = totalDestructions;
            return this;
        }

        public Builder withTotalValidations(final long totalValidations) {
            this.totalValidations = totalValidations;
            return this;
        }

        public Builder withValidationFailures(final long validationFailures) {
            this.validationFailures = validationFailures;
            return this;
        }

        public Builder withTotalTimeouts(final long totalTimeouts) {
            this.totalTimeouts = totalTimeouts;
            return this;
        }

        public Builder withTotalWaitTime(final long totalWaitTime) {
            this.totalWaitTime = totalWaitTime;
            return this;
        }

        public Builder withAverageWaitTime(final long averageWaitTime) {
            this.averageWaitTime = averageWaitTime;
            return this;
        }

        public Builder withMaxWaitTime(final long maxWaitTime) {
            this.maxWaitTime = maxWaitTime;
            return this;
        }

        public Builder withTotalActiveTime(final long totalActiveTime) {
            this.totalActiveTime = totalActiveTime;
            return this;
        }

        public Builder withAverageActiveTime(final long averageActiveTime) {
            this.averageActiveTime = averageActiveTime;
            return this;
        }

        public Builder withMaxActiveTime(final long maxActiveTime) {
            this.maxActiveTime = maxActiveTime;
            return this;
        }

        public Builder withCurrentWaitingThreads(final int currentWaitingThreads) {
            this.currentWaitingThreads = currentWaitingThreads;
            return this;
        }

        public Builder withMaxWaitingThreads(final int maxWaitingThreads) {
            this.maxWaitingThreads = maxWaitingThreads;
            return this;
        }

        public Builder withLastAcquisition(final Instant lastAcquisition) {
            this.lastAcquisition = lastAcquisition;
            return this;
        }

        public Builder withLastReturn(final Instant lastReturn) {
            this.lastReturn = lastReturn;
            return this;
        }

        public Builder withLastCreation(final Instant lastCreation) {
            this.lastCreation = lastCreation;
            return this;
        }

        public Builder withLastValidation(final Instant lastValidation) {
            this.lastValidation = lastValidation;
            return this;
        }

        public Builder withStatisticsTimestamp(final Instant statisticsTimestamp) {
            this.statisticsTimestamp = statisticsTimestamp;
            return this;
        }

        public PoolStatistics build() {
            return new PoolStatistics(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "PoolStatistics{totalSize=%d, activeCount=%d, idleCount=%d, " +
                "totalAcquisitions=%d, totalReturns=%d, totalCreations=%d, " +
                "utilization=%.2f%%, validationSuccessRate=%.2f%%, " +
                "averageWaitTime=%dms, averageActiveTime=%dms, timestamp=%s}",
                totalSize, activeCount, idleCount, totalAcquisitions, totalReturns,
                totalCreations, getUtilization() * 100, getValidationSuccessRate() * 100,
                averageWaitTime, averageActiveTime, statisticsTimestamp);
    }
}