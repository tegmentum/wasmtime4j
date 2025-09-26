package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Global debugging options for WebAssembly runtime.
 *
 * <p>Controls global debugging behavior that affects all debug sessions
 * created by a debugger.
 *
 * @since 1.0.0
 */
public final class DebugOptions {

    private final boolean globalPauseOnException;
    private final boolean globalPauseOnError;
    private final boolean enableInstructionTracing;
    private final boolean enableFunctionTracing;
    private final boolean enableMemoryTracing;
    private final int maxTraceEntries;
    private final long traceBufferSize;
    private final boolean enablePerformanceCounters;
    private final boolean enableStatisticsCollection;
    private final int maxConcurrentSessions;
    private final long sessionTimeoutMs;
    private final boolean enableDebugLogging;

    private DebugOptions(final Builder builder) {
        this.globalPauseOnException = builder.globalPauseOnException;
        this.globalPauseOnError = builder.globalPauseOnError;
        this.enableInstructionTracing = builder.enableInstructionTracing;
        this.enableFunctionTracing = builder.enableFunctionTracing;
        this.enableMemoryTracing = builder.enableMemoryTracing;
        this.maxTraceEntries = builder.maxTraceEntries;
        this.traceBufferSize = builder.traceBufferSize;
        this.enablePerformanceCounters = builder.enablePerformanceCounters;
        this.enableStatisticsCollection = builder.enableStatisticsCollection;
        this.maxConcurrentSessions = builder.maxConcurrentSessions;
        this.sessionTimeoutMs = builder.sessionTimeoutMs;
        this.enableDebugLogging = builder.enableDebugLogging;
    }

    /**
     * Creates default debug options.
     *
     * @return default options
     */
    public static DebugOptions defaultOptions() {
        return builder().build();
    }

    /**
     * Creates a debug options builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if global pause on exception is enabled.
     *
     * @return true if enabled
     */
    public boolean isGlobalPauseOnException() {
        return globalPauseOnException;
    }

    /**
     * Checks if global pause on error is enabled.
     *
     * @return true if enabled
     */
    public boolean isGlobalPauseOnError() {
        return globalPauseOnError;
    }

    /**
     * Checks if instruction tracing is enabled.
     *
     * @return true if enabled
     */
    public boolean isInstructionTracingEnabled() {
        return enableInstructionTracing;
    }

    /**
     * Checks if function tracing is enabled.
     *
     * @return true if enabled
     */
    public boolean isFunctionTracingEnabled() {
        return enableFunctionTracing;
    }

    /**
     * Checks if memory tracing is enabled.
     *
     * @return true if enabled
     */
    public boolean isMemoryTracingEnabled() {
        return enableMemoryTracing;
    }

    /**
     * Gets the maximum number of trace entries.
     *
     * @return maximum trace entries
     */
    public int getMaxTraceEntries() {
        return maxTraceEntries;
    }

    /**
     * Gets the trace buffer size in bytes.
     *
     * @return trace buffer size
     */
    public long getTraceBufferSize() {
        return traceBufferSize;
    }

    /**
     * Checks if performance counters are enabled.
     *
     * @return true if enabled
     */
    public boolean isPerformanceCountersEnabled() {
        return enablePerformanceCounters;
    }

    /**
     * Checks if statistics collection is enabled.
     *
     * @return true if enabled
     */
    public boolean isStatisticsCollectionEnabled() {
        return enableStatisticsCollection;
    }

    /**
     * Gets the maximum number of concurrent debug sessions.
     *
     * @return maximum concurrent sessions
     */
    public int getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }

    /**
     * Gets the session timeout in milliseconds.
     *
     * @return session timeout
     */
    public long getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    /**
     * Checks if debug logging is enabled.
     *
     * @return true if enabled
     */
    public boolean isDebugLoggingEnabled() {
        return enableDebugLogging;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DebugOptions other = (DebugOptions) obj;
        return globalPauseOnException == other.globalPauseOnException &&
                globalPauseOnError == other.globalPauseOnError &&
                enableInstructionTracing == other.enableInstructionTracing &&
                enableFunctionTracing == other.enableFunctionTracing &&
                enableMemoryTracing == other.enableMemoryTracing &&
                maxTraceEntries == other.maxTraceEntries &&
                traceBufferSize == other.traceBufferSize &&
                enablePerformanceCounters == other.enablePerformanceCounters &&
                enableStatisticsCollection == other.enableStatisticsCollection &&
                maxConcurrentSessions == other.maxConcurrentSessions &&
                sessionTimeoutMs == other.sessionTimeoutMs &&
                enableDebugLogging == other.enableDebugLogging;
    }

    @Override
    public int hashCode() {
        return Objects.hash(globalPauseOnException, globalPauseOnError, enableInstructionTracing,
                enableFunctionTracing, enableMemoryTracing, maxTraceEntries, traceBufferSize,
                enablePerformanceCounters, enableStatisticsCollection, maxConcurrentSessions,
                sessionTimeoutMs, enableDebugLogging);
    }

    @Override
    public String toString() {
        return "DebugOptions{" +
                "globalPauseOnException=" + globalPauseOnException +
                ", globalPauseOnError=" + globalPauseOnError +
                ", instructionTracing=" + enableInstructionTracing +
                ", functionTracing=" + enableFunctionTracing +
                ", memoryTracing=" + enableMemoryTracing +
                ", maxTraceEntries=" + maxTraceEntries +
                ", traceBufferSize=" + traceBufferSize +
                ", performanceCounters=" + enablePerformanceCounters +
                ", statisticsCollection=" + enableStatisticsCollection +
                ", maxConcurrentSessions=" + maxConcurrentSessions +
                ", sessionTimeoutMs=" + sessionTimeoutMs +
                ", debugLogging=" + enableDebugLogging +
                '}';
    }

    /**
     * Builder for debug options.
     */
    public static final class Builder {
        private boolean globalPauseOnException = false;
        private boolean globalPauseOnError = false;
        private boolean enableInstructionTracing = false;
        private boolean enableFunctionTracing = true;
        private boolean enableMemoryTracing = false;
        private int maxTraceEntries = 10000;
        private long traceBufferSize = 1024 * 1024; // 1MB
        private boolean enablePerformanceCounters = false;
        private boolean enableStatisticsCollection = true;
        private int maxConcurrentSessions = 10;
        private long sessionTimeoutMs = 300000L; // 5 minutes
        private boolean enableDebugLogging = false;

        /**
         * Sets global pause on exception.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder globalPauseOnException(final boolean enabled) {
            this.globalPauseOnException = enabled;
            return this;
        }

        /**
         * Sets global pause on error.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder globalPauseOnError(final boolean enabled) {
            this.globalPauseOnError = enabled;
            return this;
        }

        /**
         * Sets instruction tracing enabled.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder instructionTracing(final boolean enabled) {
            this.enableInstructionTracing = enabled;
            return this;
        }

        /**
         * Sets function tracing enabled.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder functionTracing(final boolean enabled) {
            this.enableFunctionTracing = enabled;
            return this;
        }

        /**
         * Sets memory tracing enabled.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder memoryTracing(final boolean enabled) {
            this.enableMemoryTracing = enabled;
            return this;
        }

        /**
         * Sets maximum trace entries.
         *
         * @param maxEntries maximum entries
         * @return this builder
         * @throws IllegalArgumentException if maxEntries is negative
         */
        public Builder maxTraceEntries(final int maxEntries) {
            if (maxEntries < 0) {
                throw new IllegalArgumentException("maxTraceEntries cannot be negative");
            }
            this.maxTraceEntries = maxEntries;
            return this;
        }

        /**
         * Sets trace buffer size.
         *
         * @param bufferSize buffer size in bytes
         * @return this builder
         * @throws IllegalArgumentException if bufferSize is negative
         */
        public Builder traceBufferSize(final long bufferSize) {
            if (bufferSize < 0) {
                throw new IllegalArgumentException("traceBufferSize cannot be negative");
            }
            this.traceBufferSize = bufferSize;
            return this;
        }

        /**
         * Sets performance counters enabled.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder performanceCounters(final boolean enabled) {
            this.enablePerformanceCounters = enabled;
            return this;
        }

        /**
         * Sets statistics collection enabled.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder statisticsCollection(final boolean enabled) {
            this.enableStatisticsCollection = enabled;
            return this;
        }

        /**
         * Sets maximum concurrent sessions.
         *
         * @param maxSessions maximum sessions
         * @return this builder
         * @throws IllegalArgumentException if maxSessions is not positive
         */
        public Builder maxConcurrentSessions(final int maxSessions) {
            if (maxSessions <= 0) {
                throw new IllegalArgumentException("maxConcurrentSessions must be positive");
            }
            this.maxConcurrentSessions = maxSessions;
            return this;
        }

        /**
         * Sets session timeout.
         *
         * @param timeoutMs timeout in milliseconds
         * @return this builder
         * @throws IllegalArgumentException if timeoutMs is negative
         */
        public Builder sessionTimeout(final long timeoutMs) {
            if (timeoutMs < 0) {
                throw new IllegalArgumentException("sessionTimeoutMs cannot be negative");
            }
            this.sessionTimeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets debug logging enabled.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder debugLogging(final boolean enabled) {
            this.enableDebugLogging = enabled;
            return this;
        }

        /**
         * Builds the debug options.
         *
         * @return new debug options
         */
        public DebugOptions build() {
            return new DebugOptions(this);
        }
    }
}