package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Configuration for WebAssembly debugging sessions.
 *
 * <p>This class provides configuration options for creating and customizing
 * debug sessions, including profiling, tracing, and performance monitoring settings.
 *
 * @since 1.0.0
 */
public final class DebugConfig {

    private final boolean profilingEnabled;
    private final boolean tracingEnabled;
    private final boolean memoryInspectionEnabled;
    private final boolean sourceMapRequired;
    private final boolean dwarfInfoRequired;
    private final int maxStackDepth;
    private final int maxVariables;
    private final long timeoutMs;
    private final boolean pauseOnStart;
    private final boolean pauseOnException;
    private final ProfilingLevel profilingLevel;

    private DebugConfig(final Builder builder) {
        this.profilingEnabled = builder.profilingEnabled;
        this.tracingEnabled = builder.tracingEnabled;
        this.memoryInspectionEnabled = builder.memoryInspectionEnabled;
        this.sourceMapRequired = builder.sourceMapRequired;
        this.dwarfInfoRequired = builder.dwarfInfoRequired;
        this.maxStackDepth = builder.maxStackDepth;
        this.maxVariables = builder.maxVariables;
        this.timeoutMs = builder.timeoutMs;
        this.pauseOnStart = builder.pauseOnStart;
        this.pauseOnException = builder.pauseOnException;
        this.profilingLevel = builder.profilingLevel;
    }

    /**
     * Creates a default debug configuration.
     *
     * @return a new debug configuration with default settings
     */
    public static DebugConfig defaultConfig() {
        return builder().build();
    }

    /**
     * Creates a debug configuration builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if profiling is enabled.
     *
     * @return true if profiling is enabled
     */
    public boolean isProfilingEnabled() {
        return profilingEnabled;
    }

    /**
     * Checks if tracing is enabled.
     *
     * @return true if tracing is enabled
     */
    public boolean isTracingEnabled() {
        return tracingEnabled;
    }

    /**
     * Checks if memory inspection is enabled.
     *
     * @return true if memory inspection is enabled
     */
    public boolean isMemoryInspectionEnabled() {
        return memoryInspectionEnabled;
    }

    /**
     * Checks if source map is required.
     *
     * @return true if source map is required
     */
    public boolean isSourceMapRequired() {
        return sourceMapRequired;
    }

    /**
     * Checks if DWARF debug info is required.
     *
     * @return true if DWARF info is required
     */
    public boolean isDwarfInfoRequired() {
        return dwarfInfoRequired;
    }

    /**
     * Gets the maximum stack depth to capture.
     *
     * @return maximum stack depth
     */
    public int getMaxStackDepth() {
        return maxStackDepth;
    }

    /**
     * Gets the maximum number of variables to capture.
     *
     * @return maximum variables
     */
    public int getMaxVariables() {
        return maxVariables;
    }

    /**
     * Gets the debug operation timeout in milliseconds.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Checks if execution should pause on start.
     *
     * @return true if should pause on start
     */
    public boolean isPauseOnStart() {
        return pauseOnStart;
    }

    /**
     * Checks if execution should pause on exceptions.
     *
     * @return true if should pause on exceptions
     */
    public boolean isPauseOnException() {
        return pauseOnException;
    }

    /**
     * Gets the profiling level.
     *
     * @return profiling level
     */
    public ProfilingLevel getProfilingLevel() {
        return profilingLevel;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DebugConfig other = (DebugConfig) obj;
        return profilingEnabled == other.profilingEnabled &&
                tracingEnabled == other.tracingEnabled &&
                memoryInspectionEnabled == other.memoryInspectionEnabled &&
                sourceMapRequired == other.sourceMapRequired &&
                dwarfInfoRequired == other.dwarfInfoRequired &&
                maxStackDepth == other.maxStackDepth &&
                maxVariables == other.maxVariables &&
                timeoutMs == other.timeoutMs &&
                pauseOnStart == other.pauseOnStart &&
                pauseOnException == other.pauseOnException &&
                profilingLevel == other.profilingLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(profilingEnabled, tracingEnabled, memoryInspectionEnabled,
                sourceMapRequired, dwarfInfoRequired, maxStackDepth, maxVariables,
                timeoutMs, pauseOnStart, pauseOnException, profilingLevel);
    }

    @Override
    public String toString() {
        return "DebugConfig{" +
                "profilingEnabled=" + profilingEnabled +
                ", tracingEnabled=" + tracingEnabled +
                ", memoryInspectionEnabled=" + memoryInspectionEnabled +
                ", sourceMapRequired=" + sourceMapRequired +
                ", dwarfInfoRequired=" + dwarfInfoRequired +
                ", maxStackDepth=" + maxStackDepth +
                ", maxVariables=" + maxVariables +
                ", timeoutMs=" + timeoutMs +
                ", pauseOnStart=" + pauseOnStart +
                ", pauseOnException=" + pauseOnException +
                ", profilingLevel=" + profilingLevel +
                '}';
    }

    /**
     * Profiling levels for debugging.
     */
    public enum ProfilingLevel {
        /** No profiling */
        NONE,
        /** Basic profiling (function calls) */
        BASIC,
        /** Detailed profiling (instructions) */
        DETAILED,
        /** Full profiling (all events) */
        FULL
    }

    /**
     * Builder for creating debug configurations.
     */
    public static final class Builder {
        private boolean profilingEnabled = false;
        private boolean tracingEnabled = false;
        private boolean memoryInspectionEnabled = true;
        private boolean sourceMapRequired = false;
        private boolean dwarfInfoRequired = false;
        private int maxStackDepth = 100;
        private int maxVariables = 1000;
        private long timeoutMs = 30000L; // 30 seconds
        private boolean pauseOnStart = false;
        private boolean pauseOnException = false;
        private ProfilingLevel profilingLevel = ProfilingLevel.NONE;

        /**
         * Enables or disables profiling.
         *
         * @param enabled true to enable profiling
         * @return this builder
         */
        public Builder profilingEnabled(final boolean enabled) {
            this.profilingEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables tracing.
         *
         * @param enabled true to enable tracing
         * @return this builder
         */
        public Builder tracingEnabled(final boolean enabled) {
            this.tracingEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables memory inspection.
         *
         * @param enabled true to enable memory inspection
         * @return this builder
         */
        public Builder memoryInspectionEnabled(final boolean enabled) {
            this.memoryInspectionEnabled = enabled;
            return this;
        }

        /**
         * Sets whether source map is required.
         *
         * @param required true if source map is required
         * @return this builder
         */
        public Builder sourceMapRequired(final boolean required) {
            this.sourceMapRequired = required;
            return this;
        }

        /**
         * Sets whether DWARF debug info is required.
         *
         * @param required true if DWARF info is required
         * @return this builder
         */
        public Builder dwarfInfoRequired(final boolean required) {
            this.dwarfInfoRequired = required;
            return this;
        }

        /**
         * Sets the maximum stack depth.
         *
         * @param maxDepth maximum stack depth
         * @return this builder
         * @throws IllegalArgumentException if maxDepth is negative
         */
        public Builder maxStackDepth(final int maxDepth) {
            if (maxDepth < 0) {
                throw new IllegalArgumentException("maxStackDepth cannot be negative");
            }
            this.maxStackDepth = maxDepth;
            return this;
        }

        /**
         * Sets the maximum number of variables.
         *
         * @param maxVars maximum variables
         * @return this builder
         * @throws IllegalArgumentException if maxVars is negative
         */
        public Builder maxVariables(final int maxVars) {
            if (maxVars < 0) {
                throw new IllegalArgumentException("maxVariables cannot be negative");
            }
            this.maxVariables = maxVars;
            return this;
        }

        /**
         * Sets the debug operation timeout.
         *
         * @param timeoutMs timeout in milliseconds
         * @return this builder
         * @throws IllegalArgumentException if timeoutMs is negative
         */
        public Builder timeout(final long timeoutMs) {
            if (timeoutMs < 0) {
                throw new IllegalArgumentException("timeoutMs cannot be negative");
            }
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets whether to pause on start.
         *
         * @param pauseOnStart true to pause on start
         * @return this builder
         */
        public Builder pauseOnStart(final boolean pauseOnStart) {
            this.pauseOnStart = pauseOnStart;
            return this;
        }

        /**
         * Sets whether to pause on exceptions.
         *
         * @param pauseOnException true to pause on exceptions
         * @return this builder
         */
        public Builder pauseOnException(final boolean pauseOnException) {
            this.pauseOnException = pauseOnException;
            return this;
        }

        /**
         * Sets the profiling level.
         *
         * @param level profiling level
         * @return this builder
         * @throws IllegalArgumentException if level is null
         */
        public Builder profilingLevel(final ProfilingLevel level) {
            this.profilingLevel = Objects.requireNonNull(level, "profilingLevel cannot be null");
            return this;
        }

        /**
         * Builds the debug configuration.
         *
         * @return a new debug configuration
         */
        public DebugConfig build() {
            return new DebugConfig(this);
        }
    }
}