/*
 * Copyright 2024 Tegmentum AI Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.testing;

import java.time.Duration;

/**
 * Statistical information about test execution performance and behavior.
 *
 * <p>This class provides detailed statistics about test execution including timing,
 * memory usage, and performance metrics for analysis and optimization.
 */
public final class TestStatistics {

    private final Duration minExecutionTime;
    private final Duration maxExecutionTime;
    private final Duration averageExecutionTime;
    private final Duration medianExecutionTime;
    private final long memoryUsedBytes;
    private final long peakMemoryUsageBytes;
    private final int testMethodCount;
    private final int testClassCount;

    private TestStatistics(final Builder builder) {
        this.minExecutionTime = builder.minExecutionTime;
        this.maxExecutionTime = builder.maxExecutionTime;
        this.averageExecutionTime = builder.averageExecutionTime;
        this.medianExecutionTime = builder.medianExecutionTime;
        this.memoryUsedBytes = builder.memoryUsedBytes;
        this.peakMemoryUsageBytes = builder.peakMemoryUsageBytes;
        this.testMethodCount = builder.testMethodCount;
        this.testClassCount = builder.testClassCount;
    }

    /**
     * Creates a new test statistics builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the minimum execution time across all tests.
     *
     * @return minimum execution time
     */
    public Duration getMinExecutionTime() {
        return minExecutionTime;
    }

    /**
     * Gets the maximum execution time across all tests.
     *
     * @return maximum execution time
     */
    public Duration getMaxExecutionTime() {
        return maxExecutionTime;
    }

    /**
     * Gets the average execution time across all tests.
     *
     * @return average execution time
     */
    public Duration getAverageExecutionTime() {
        return averageExecutionTime;
    }

    /**
     * Gets the median execution time across all tests.
     *
     * @return median execution time
     */
    public Duration getMedianExecutionTime() {
        return medianExecutionTime;
    }

    /**
     * Gets the total memory used during test execution.
     *
     * @return memory used in bytes
     */
    public long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    /**
     * Gets the peak memory usage during test execution.
     *
     * @return peak memory usage in bytes
     */
    public long getPeakMemoryUsageBytes() {
        return peakMemoryUsageBytes;
    }

    /**
     * Gets the total number of test methods executed.
     *
     * @return test method count
     */
    public int getTestMethodCount() {
        return testMethodCount;
    }

    /**
     * Gets the total number of test classes executed.
     *
     * @return test class count
     */
    public int getTestClassCount() {
        return testClassCount;
    }

    /**
     * Gets memory usage in megabytes.
     *
     * @return memory used in MB
     */
    public double getMemoryUsedMB() {
        return memoryUsedBytes / (1024.0 * 1024.0);
    }

    /**
     * Gets peak memory usage in megabytes.
     *
     * @return peak memory usage in MB
     */
    public double getPeakMemoryUsageMB() {
        return peakMemoryUsageBytes / (1024.0 * 1024.0);
    }

    @Override
    public String toString() {
        return String.format(
            "TestStatistics{" +
            "testMethods=%d, testClasses=%d, " +
            "minTime=%s, maxTime=%s, avgTime=%s, " +
            "memoryUsed=%.2fMB, peakMemory=%.2fMB}",
            testMethodCount, testClassCount,
            minExecutionTime, maxExecutionTime, averageExecutionTime,
            getMemoryUsedMB(), getPeakMemoryUsageMB()
        );
    }

    /**
     * Builder for creating TestStatistics instances.
     */
    public static final class Builder {

        private Duration minExecutionTime = Duration.ZERO;
        private Duration maxExecutionTime = Duration.ZERO;
        private Duration averageExecutionTime = Duration.ZERO;
        private Duration medianExecutionTime = Duration.ZERO;
        private long memoryUsedBytes = 0;
        private long peakMemoryUsageBytes = 0;
        private int testMethodCount = 0;
        private int testClassCount = 0;

        private Builder() {
        }

        /**
         * Sets the minimum execution time.
         *
         * @param minExecutionTime minimum execution time
         * @return this builder
         */
        public Builder withMinExecutionTime(final Duration minExecutionTime) {
            this.minExecutionTime = minExecutionTime;
            return this;
        }

        /**
         * Sets the maximum execution time.
         *
         * @param maxExecutionTime maximum execution time
         * @return this builder
         */
        public Builder withMaxExecutionTime(final Duration maxExecutionTime) {
            this.maxExecutionTime = maxExecutionTime;
            return this;
        }

        /**
         * Sets the average execution time.
         *
         * @param averageExecutionTime average execution time
         * @return this builder
         */
        public Builder withAverageExecutionTime(final Duration averageExecutionTime) {
            this.averageExecutionTime = averageExecutionTime;
            return this;
        }

        /**
         * Sets the median execution time.
         *
         * @param medianExecutionTime median execution time
         * @return this builder
         */
        public Builder withMedianExecutionTime(final Duration medianExecutionTime) {
            this.medianExecutionTime = medianExecutionTime;
            return this;
        }

        /**
         * Sets the memory used during test execution.
         *
         * @param memoryUsedBytes memory used in bytes
         * @return this builder
         */
        public Builder withMemoryUsedBytes(final long memoryUsedBytes) {
            this.memoryUsedBytes = memoryUsedBytes;
            return this;
        }

        /**
         * Sets the peak memory usage during test execution.
         *
         * @param peakMemoryUsageBytes peak memory usage in bytes
         * @return this builder
         */
        public Builder withPeakMemoryUsageBytes(final long peakMemoryUsageBytes) {
            this.peakMemoryUsageBytes = peakMemoryUsageBytes;
            return this;
        }

        /**
         * Sets the test method count.
         *
         * @param testMethodCount number of test methods
         * @return this builder
         */
        public Builder withTestMethodCount(final int testMethodCount) {
            this.testMethodCount = testMethodCount;
            return this;
        }

        /**
         * Sets the test class count.
         *
         * @param testClassCount number of test classes
         * @return this builder
         */
        public Builder withTestClassCount(final int testClassCount) {
            this.testClassCount = testClassCount;
            return this;
        }

        /**
         * Builds the TestStatistics instance.
         *
         * @return the test statistics
         */
        public TestStatistics build() {
            return new TestStatistics(this);
        }
    }
}