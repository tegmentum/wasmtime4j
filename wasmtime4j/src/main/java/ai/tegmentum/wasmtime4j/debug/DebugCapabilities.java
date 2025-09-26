package ai.tegmentum.wasmtime4j.debug;

import java.util.Collections;
import java.util.Set;
import java.util.Objects;

/**
 * Represents debugging capabilities supported by a WebAssembly runtime.
 *
 * <p>This class provides information about what debugging features are available
 * in the current runtime implementation.
 *
 * @since 1.0.0
 */
public final class DebugCapabilities {

    private final boolean breakpointSupport;
    private final boolean stepExecutionSupport;
    private final boolean variableInspectionSupport;
    private final boolean memoryInspectionSupport;
    private final boolean callStackSupport;
    private final boolean sourceMapSupport;
    private final boolean dwarfSupport;
    private final boolean profilingSupport;
    private final boolean tracingSupport;
    private final boolean multiTargetSupport;
    private final boolean conditionalBreakpointSupport;
    private final boolean watchExpressionSupport;
    private final boolean memoryModificationSupport;
    private final boolean hotReloadSupport;
    private final Set<String> supportedFeatures;
    private final String version;

    private DebugCapabilities(final Builder builder) {
        this.breakpointSupport = builder.breakpointSupport;
        this.stepExecutionSupport = builder.stepExecutionSupport;
        this.variableInspectionSupport = builder.variableInspectionSupport;
        this.memoryInspectionSupport = builder.memoryInspectionSupport;
        this.callStackSupport = builder.callStackSupport;
        this.sourceMapSupport = builder.sourceMapSupport;
        this.dwarfSupport = builder.dwarfSupport;
        this.profilingSupport = builder.profilingSupport;
        this.tracingSupport = builder.tracingSupport;
        this.multiTargetSupport = builder.multiTargetSupport;
        this.conditionalBreakpointSupport = builder.conditionalBreakpointSupport;
        this.watchExpressionSupport = builder.watchExpressionSupport;
        this.memoryModificationSupport = builder.memoryModificationSupport;
        this.hotReloadSupport = builder.hotReloadSupport;
        this.supportedFeatures = Collections.unmodifiableSet(builder.supportedFeatures);
        this.version = builder.version;
    }

    /**
     * Creates a capabilities builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates capabilities with all features enabled.
     *
     * @param version the version string
     * @return full capabilities
     */
    public static DebugCapabilities fullCapabilities(final String version) {
        return builder()
                .breakpointSupport(true)
                .stepExecutionSupport(true)
                .variableInspectionSupport(true)
                .memoryInspectionSupport(true)
                .callStackSupport(true)
                .sourceMapSupport(true)
                .dwarfSupport(true)
                .profilingSupport(true)
                .tracingSupport(true)
                .multiTargetSupport(true)
                .conditionalBreakpointSupport(true)
                .watchExpressionSupport(true)
                .memoryModificationSupport(true)
                .hotReloadSupport(true)
                .version(version)
                .build();
    }

    /**
     * Creates capabilities with minimal features.
     *
     * @param version the version string
     * @return minimal capabilities
     */
    public static DebugCapabilities minimalCapabilities(final String version) {
        return builder()
                .breakpointSupport(true)
                .stepExecutionSupport(true)
                .callStackSupport(true)
                .version(version)
                .build();
    }

    /**
     * Checks if breakpoint support is available.
     *
     * @return true if breakpoints are supported
     */
    public boolean hasBreakpointSupport() {
        return breakpointSupport;
    }

    /**
     * Checks if step execution support is available.
     *
     * @return true if step execution is supported
     */
    public boolean hasStepExecutionSupport() {
        return stepExecutionSupport;
    }

    /**
     * Checks if variable inspection support is available.
     *
     * @return true if variable inspection is supported
     */
    public boolean hasVariableInspectionSupport() {
        return variableInspectionSupport;
    }

    /**
     * Checks if memory inspection support is available.
     *
     * @return true if memory inspection is supported
     */
    public boolean hasMemoryInspectionSupport() {
        return memoryInspectionSupport;
    }

    /**
     * Checks if call stack support is available.
     *
     * @return true if call stack is supported
     */
    public boolean hasCallStackSupport() {
        return callStackSupport;
    }

    /**
     * Checks if source map support is available.
     *
     * @return true if source maps are supported
     */
    public boolean hasSourceMapSupport() {
        return sourceMapSupport;
    }

    /**
     * Checks if DWARF debug info support is available.
     *
     * @return true if DWARF is supported
     */
    public boolean hasDwarfSupport() {
        return dwarfSupport;
    }

    /**
     * Checks if profiling support is available.
     *
     * @return true if profiling is supported
     */
    public boolean hasProfilingSupport() {
        return profilingSupport;
    }

    /**
     * Checks if tracing support is available.
     *
     * @return true if tracing is supported
     */
    public boolean hasTracingSupport() {
        return tracingSupport;
    }

    /**
     * Checks if multi-target debugging support is available.
     *
     * @return true if multi-target debugging is supported
     */
    public boolean hasMultiTargetSupport() {
        return multiTargetSupport;
    }

    /**
     * Checks if conditional breakpoint support is available.
     *
     * @return true if conditional breakpoints are supported
     */
    public boolean hasConditionalBreakpointSupport() {
        return conditionalBreakpointSupport;
    }

    /**
     * Checks if watch expression support is available.
     *
     * @return true if watch expressions are supported
     */
    public boolean hasWatchExpressionSupport() {
        return watchExpressionSupport;
    }

    /**
     * Checks if memory modification support is available.
     *
     * @return true if memory modification is supported
     */
    public boolean hasMemoryModificationSupport() {
        return memoryModificationSupport;
    }

    /**
     * Checks if hot reload support is available.
     *
     * @return true if hot reload is supported
     */
    public boolean hasHotReloadSupport() {
        return hotReloadSupport;
    }

    /**
     * Gets all supported features.
     *
     * @return set of supported feature names
     */
    public Set<String> getSupportedFeatures() {
        return supportedFeatures;
    }

    /**
     * Gets the version string.
     *
     * @return version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Checks if a specific feature is supported.
     *
     * @param feature the feature name
     * @return true if the feature is supported
     */
    public boolean hasFeature(final String feature) {
        return supportedFeatures.contains(feature);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DebugCapabilities other = (DebugCapabilities) obj;
        return breakpointSupport == other.breakpointSupport &&
                stepExecutionSupport == other.stepExecutionSupport &&
                variableInspectionSupport == other.variableInspectionSupport &&
                memoryInspectionSupport == other.memoryInspectionSupport &&
                callStackSupport == other.callStackSupport &&
                sourceMapSupport == other.sourceMapSupport &&
                dwarfSupport == other.dwarfSupport &&
                profilingSupport == other.profilingSupport &&
                tracingSupport == other.tracingSupport &&
                multiTargetSupport == other.multiTargetSupport &&
                conditionalBreakpointSupport == other.conditionalBreakpointSupport &&
                watchExpressionSupport == other.watchExpressionSupport &&
                memoryModificationSupport == other.memoryModificationSupport &&
                hotReloadSupport == other.hotReloadSupport &&
                Objects.equals(supportedFeatures, other.supportedFeatures) &&
                Objects.equals(version, other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(breakpointSupport, stepExecutionSupport, variableInspectionSupport,
                memoryInspectionSupport, callStackSupport, sourceMapSupport, dwarfSupport,
                profilingSupport, tracingSupport, multiTargetSupport, conditionalBreakpointSupport,
                watchExpressionSupport, memoryModificationSupport, hotReloadSupport,
                supportedFeatures, version);
    }

    @Override
    public String toString() {
        return "DebugCapabilities{" +
                "version='" + version + '\'' +
                ", breakpoints=" + breakpointSupport +
                ", stepExecution=" + stepExecutionSupport +
                ", variableInspection=" + variableInspectionSupport +
                ", memoryInspection=" + memoryInspectionSupport +
                ", callStack=" + callStackSupport +
                ", sourceMap=" + sourceMapSupport +
                ", dwarf=" + dwarfSupport +
                ", profiling=" + profilingSupport +
                ", tracing=" + tracingSupport +
                ", multiTarget=" + multiTargetSupport +
                ", conditionalBreakpoints=" + conditionalBreakpointSupport +
                ", watchExpressions=" + watchExpressionSupport +
                ", memoryModification=" + memoryModificationSupport +
                ", hotReload=" + hotReloadSupport +
                ", features=" + supportedFeatures +
                '}';
    }

    /**
     * Builder for creating debug capabilities.
     */
    public static final class Builder {
        private boolean breakpointSupport = false;
        private boolean stepExecutionSupport = false;
        private boolean variableInspectionSupport = false;
        private boolean memoryInspectionSupport = false;
        private boolean callStackSupport = false;
        private boolean sourceMapSupport = false;
        private boolean dwarfSupport = false;
        private boolean profilingSupport = false;
        private boolean tracingSupport = false;
        private boolean multiTargetSupport = false;
        private boolean conditionalBreakpointSupport = false;
        private boolean watchExpressionSupport = false;
        private boolean memoryModificationSupport = false;
        private boolean hotReloadSupport = false;
        private Set<String> supportedFeatures = Collections.emptySet();
        private String version = "1.0.0";

        /**
         * Sets breakpoint support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder breakpointSupport(final boolean supported) {
            this.breakpointSupport = supported;
            return this;
        }

        /**
         * Sets step execution support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder stepExecutionSupport(final boolean supported) {
            this.stepExecutionSupport = supported;
            return this;
        }

        /**
         * Sets variable inspection support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder variableInspectionSupport(final boolean supported) {
            this.variableInspectionSupport = supported;
            return this;
        }

        /**
         * Sets memory inspection support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder memoryInspectionSupport(final boolean supported) {
            this.memoryInspectionSupport = supported;
            return this;
        }

        /**
         * Sets call stack support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder callStackSupport(final boolean supported) {
            this.callStackSupport = supported;
            return this;
        }

        /**
         * Sets source map support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder sourceMapSupport(final boolean supported) {
            this.sourceMapSupport = supported;
            return this;
        }

        /**
         * Sets DWARF support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder dwarfSupport(final boolean supported) {
            this.dwarfSupport = supported;
            return this;
        }

        /**
         * Sets profiling support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder profilingSupport(final boolean supported) {
            this.profilingSupport = supported;
            return this;
        }

        /**
         * Sets tracing support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder tracingSupport(final boolean supported) {
            this.tracingSupport = supported;
            return this;
        }

        /**
         * Sets multi-target support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder multiTargetSupport(final boolean supported) {
            this.multiTargetSupport = supported;
            return this;
        }

        /**
         * Sets conditional breakpoint support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder conditionalBreakpointSupport(final boolean supported) {
            this.conditionalBreakpointSupport = supported;
            return this;
        }

        /**
         * Sets watch expression support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder watchExpressionSupport(final boolean supported) {
            this.watchExpressionSupport = supported;
            return this;
        }

        /**
         * Sets memory modification support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder memoryModificationSupport(final boolean supported) {
            this.memoryModificationSupport = supported;
            return this;
        }

        /**
         * Sets hot reload support.
         *
         * @param supported true if supported
         * @return this builder
         */
        public Builder hotReloadSupport(final boolean supported) {
            this.hotReloadSupport = supported;
            return this;
        }

        /**
         * Sets supported features.
         *
         * @param features set of supported features
         * @return this builder
         */
        public Builder supportedFeatures(final Set<String> features) {
            this.supportedFeatures = Objects.requireNonNull(features, "features cannot be null");
            return this;
        }

        /**
         * Sets the version.
         *
         * @param version version string
         * @return this builder
         */
        public Builder version(final String version) {
            this.version = Objects.requireNonNull(version, "version cannot be null");
            return this;
        }

        /**
         * Builds the debug capabilities.
         *
         * @return a new debug capabilities instance
         */
        public DebugCapabilities build() {
            return new DebugCapabilities(this);
        }
    }
}