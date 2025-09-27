package ai.tegmentum.wasmtime4j.jni.config;

import ai.tegmentum.wasmtime4j.config.AdvancedConfiguration;
import ai.tegmentum.wasmtime4j.config.CompilationStrategy;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import ai.tegmentum.wasmtime4j.config.ResourceLimits;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of advanced configuration capabilities for wasmtime4j.
 *
 * <p>This class provides comprehensive configuration options for WebAssembly execution,
 * including compilation strategies, optimization levels, resource limits, and
 * performance tuning parameters specifically optimized for JNI-based operations.
 *
 * <p>Features include:
 * <ul>
 *   <li>Compilation strategy configuration (eager, lazy, streaming)</li>
 *   <li>Optimization level control (none, speed, size)</li>
 *   <li>Resource limits and quotas</li>
 *   <li>Memory management configuration</li>
 *   <li>Thread pool and concurrency settings</li>
 *   <li>Debug and profiling options</li>
 *   <li>WASM feature enablement</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniAdvancedConfiguration implements AdvancedConfiguration {

    private static final Logger LOGGER = Logger.getLogger(JniAdvancedConfiguration.class.getName());

    private final long nativeConfigHandle;
    private final Map<String, Object> configurationMap;
    private final Map<String, Object> runtimeParameters;
    private volatile boolean applied = false;
    private volatile boolean closed = false;

    // Configuration categories
    private CompilationStrategy compilationStrategy = CompilationStrategy.LAZY;
    private OptimizationLevel optimizationLevel = OptimizationLevel.SPEED;
    private ResourceLimits resourceLimits = ResourceLimits.defaultLimits();
    private MemoryConfiguration memoryConfig = new MemoryConfiguration();
    private ThreadConfiguration threadConfig = new ThreadConfiguration();
    private DebugConfiguration debugConfig = new DebugConfiguration();
    private Set<String> enabledFeatures = EnumSet.noneOf(WasmFeature.class).stream()
            .map(Enum::name)
            .collect(java.util.stream.Collectors.toSet());

    /**
     * Creates a new JNI advanced configuration with default settings.
     */
    public JniAdvancedConfiguration() {
        this.configurationMap = new ConcurrentHashMap<>();
        this.runtimeParameters = new ConcurrentHashMap<>();

        try {
            this.nativeConfigHandle = nativeCreateConfiguration();
            if (nativeConfigHandle == 0) {
                throw new WasmException("Failed to create native configuration");
            }

            // Initialize with default values
            initializeDefaults();

            LOGGER.log(Level.FINE, "Created JNI advanced configuration with handle: {0}", nativeConfigHandle);
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to create JNI advanced configuration");
        }
    }

    @Override
    public AdvancedConfiguration setCompilationStrategy(final CompilationStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy cannot be null");
        validateNotApplied();

        try {
            nativeSetCompilationStrategy(nativeConfigHandle, strategy.ordinal());
            this.compilationStrategy = strategy;
            configurationMap.put("compilation_strategy", strategy);

            LOGGER.log(Level.FINE, "Set compilation strategy: {0}", strategy);
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set compilation strategy");
        }
    }

    @Override
    public CompilationStrategy getCompilationStrategy() {
        return compilationStrategy;
    }

    @Override
    public AdvancedConfiguration setOptimizationLevel(final OptimizationLevel level) {
        Objects.requireNonNull(level, "level cannot be null");
        validateNotApplied();

        try {
            nativeSetOptimizationLevel(nativeConfigHandle, level.ordinal());
            this.optimizationLevel = level;
            configurationMap.put("optimization_level", level);

            LOGGER.log(Level.FINE, "Set optimization level: {0}", level);
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set optimization level");
        }
    }

    @Override
    public OptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }

    @Override
    public AdvancedConfiguration setResourceLimits(final ResourceLimits limits) {
        Objects.requireNonNull(limits, "limits cannot be null");
        validateNotApplied();

        try {
            nativeSetResourceLimits(nativeConfigHandle,
                limits.getMaxMemoryBytes(),
                limits.getMaxTableElements(),
                limits.getMaxInstances(),
                limits.getMaxTables(),
                limits.getMaxMemories());

            this.resourceLimits = limits;
            configurationMap.put("resource_limits", limits);

            LOGGER.log(Level.FINE, "Set resource limits: {0}", limits);
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set resource limits");
        }
    }

    @Override
    public ResourceLimits getResourceLimits() {
        return resourceLimits;
    }

    @Override
    public AdvancedConfiguration enableFeature(final String feature) {
        Objects.requireNonNull(feature, "feature cannot be null");
        validateNotApplied();

        try {
            if (nativeEnableFeature(nativeConfigHandle, feature)) {
                enabledFeatures.add(feature);
                configurationMap.put("enabled_features", enabledFeatures);

                LOGGER.log(Level.FINE, "Enabled feature: {0}", feature);
            } else {
                LOGGER.log(Level.WARNING, "Failed to enable feature: {0}", feature);
            }
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to enable feature");
        }
    }

    @Override
    public AdvancedConfiguration disableFeature(final String feature) {
        Objects.requireNonNull(feature, "feature cannot be null");
        validateNotApplied();

        try {
            if (nativeDisableFeature(nativeConfigHandle, feature)) {
                enabledFeatures.remove(feature);
                configurationMap.put("enabled_features", enabledFeatures);

                LOGGER.log(Level.FINE, "Disabled feature: {0}", feature);
            } else {
                LOGGER.log(Level.WARNING, "Failed to disable feature: {0}", feature);
            }
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to disable feature");
        }
    }

    @Override
    public boolean isFeatureEnabled(final String feature) {
        Objects.requireNonNull(feature, "feature cannot be null");
        return enabledFeatures.contains(feature);
    }

    @Override
    public Set<String> getEnabledFeatures() {
        return Set.copyOf(enabledFeatures);
    }

    @Override
    public AdvancedConfiguration setParameter(final String key, final Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        validateNotApplied();

        try {
            // Convert value to string for native layer
            final String stringValue = value.toString();
            if (nativeSetParameter(nativeConfigHandle, key, stringValue)) {
                runtimeParameters.put(key, value);

                LOGGER.log(Level.FINE, "Set parameter: {0} = {1}", new Object[]{key, value});
            } else {
                LOGGER.log(Level.WARNING, "Failed to set parameter: {0}", key);
            }
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set parameter");
        }
    }

    @Override
    public <T> T getParameter(final String key, final Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        final Object value = runtimeParameters.get(key);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        // Attempt conversion for common types
        if (type == String.class) {
            return type.cast(value.toString());
        } else if (type == Integer.class && value instanceof Number) {
            return type.cast(((Number) value).intValue());
        } else if (type == Long.class && value instanceof Number) {
            return type.cast(((Number) value).longValue());
        } else if (type == Double.class && value instanceof Number) {
            return type.cast(((Number) value).doubleValue());
        } else if (type == Boolean.class) {
            return type.cast(Boolean.parseBoolean(value.toString()));
        }

        throw new IllegalArgumentException("Cannot convert parameter " + key + " to type " + type.getName());
    }

    @Override
    public Map<String, Object> getAllParameters() {
        return Map.copyOf(runtimeParameters);
    }

    @Override
    public AdvancedConfiguration apply() throws WasmException {
        validateNotClosed();

        if (applied) {
            throw new IllegalStateException("Configuration has already been applied");
        }

        try {
            if (!nativeApplyConfiguration(nativeConfigHandle)) {
                throw new WasmException("Failed to apply native configuration");
            }

            applied = true;
            LOGGER.log(Level.FINE, "Applied JNI advanced configuration");
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to apply configuration");
        }
    }

    @Override
    public boolean isApplied() {
        return applied;
    }

    @Override
    public AdvancedConfiguration reset() {
        validateNotClosed();

        try {
            nativeResetConfiguration(nativeConfigHandle);
            applied = false;

            // Reset to defaults
            initializeDefaults();

            LOGGER.log(Level.FINE, "Reset JNI advanced configuration");
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to reset configuration");
        }
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> result = new HashMap<>(configurationMap);
        result.putAll(runtimeParameters);
        result.put("applied", applied);
        result.put("runtime_type", "JNI");
        return result;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            if (nativeConfigHandle != 0) {
                nativeDestroyConfiguration(nativeConfigHandle);
            }

            configurationMap.clear();
            runtimeParameters.clear();
            closed = true;

            LOGGER.log(Level.FINE, "Closed JNI advanced configuration");
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error closing JNI configuration", e);
            closed = true; // Mark as closed even on error
        }
    }

    /**
     * Gets the native configuration handle.
     *
     * @return native handle
     */
    public long getNativeHandle() {
        validateNotClosed();
        return nativeConfigHandle;
    }

    /**
     * Sets memory configuration options.
     *
     * @param config memory configuration
     * @return this configuration instance
     */
    public JniAdvancedConfiguration setMemoryConfiguration(final MemoryConfiguration config) {
        Objects.requireNonNull(config, "config cannot be null");
        validateNotApplied();

        try {
            nativeSetMemoryConfiguration(nativeConfigHandle,
                config.getInitialMemorySize(),
                config.getMaximumMemorySize(),
                config.getMemoryGrowthIncrement(),
                config.isMemoryGuardEnabled(),
                config.getMemoryGuardSize());

            this.memoryConfig = config;
            configurationMap.put("memory_config", config);

            LOGGER.log(Level.FINE, "Set memory configuration: {0}", config);
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set memory configuration");
        }
    }

    /**
     * Gets memory configuration.
     *
     * @return memory configuration
     */
    public MemoryConfiguration getMemoryConfiguration() {
        return memoryConfig;
    }

    /**
     * Sets thread configuration options.
     *
     * @param config thread configuration
     * @return this configuration instance
     */
    public JniAdvancedConfiguration setThreadConfiguration(final ThreadConfiguration config) {
        Objects.requireNonNull(config, "config cannot be null");
        validateNotApplied();

        try {
            nativeSetThreadConfiguration(nativeConfigHandle,
                config.getMaxThreads(),
                config.getThreadStackSize(),
                config.isThreadPoolEnabled(),
                config.getThreadPoolSize(),
                config.getThreadAffinityMask());

            this.threadConfig = config;
            configurationMap.put("thread_config", config);

            LOGGER.log(Level.FINE, "Set thread configuration: {0}", config);
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set thread configuration");
        }
    }

    /**
     * Gets thread configuration.
     *
     * @return thread configuration
     */
    public ThreadConfiguration getThreadConfiguration() {
        return threadConfig;
    }

    /**
     * Sets debug configuration options.
     *
     * @param config debug configuration
     * @return this configuration instance
     */
    public JniAdvancedConfiguration setDebugConfiguration(final DebugConfiguration config) {
        Objects.requireNonNull(config, "config cannot be null");
        validateNotApplied();

        try {
            nativeSetDebugConfiguration(nativeConfigHandle,
                config.isDebugInfoEnabled(),
                config.isProfilingEnabled(),
                config.getProfilingSamplingInterval().toMillis(),
                config.isStackTraceEnabled(),
                config.getMaxStackTraceDepth());

            this.debugConfig = config;
            configurationMap.put("debug_config", config);

            LOGGER.log(Level.FINE, "Set debug configuration: {0}", config);
            return this;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set debug configuration");
        }
    }

    /**
     * Gets debug configuration.
     *
     * @return debug configuration
     */
    public DebugConfiguration getDebugConfiguration() {
        return debugConfig;
    }

    // Private helper methods

    private void validateNotApplied() {
        if (applied) {
            throw new IllegalStateException("Configuration has already been applied and cannot be modified");
        }
    }

    private void validateNotClosed() {
        if (closed) {
            throw new IllegalStateException("Configuration is closed");
        }
    }

    private void initializeDefaults() {
        configurationMap.clear();
        runtimeParameters.clear();

        // Set default configuration values
        configurationMap.put("compilation_strategy", compilationStrategy);
        configurationMap.put("optimization_level", optimizationLevel);
        configurationMap.put("resource_limits", resourceLimits);
        configurationMap.put("memory_config", memoryConfig);
        configurationMap.put("thread_config", threadConfig);
        configurationMap.put("debug_config", debugConfig);
        configurationMap.put("enabled_features", enabledFeatures);

        // Set default runtime parameters
        runtimeParameters.put("jni.optimization.enabled", true);
        runtimeParameters.put("jni.batch.size", 100);
        runtimeParameters.put("jni.cache.enabled", true);
        runtimeParameters.put("jni.parallel.compilation", false);
    }

    // Native method declarations

    private static native long nativeCreateConfiguration();
    private static native void nativeDestroyConfiguration(long configHandle);
    private static native boolean nativeApplyConfiguration(long configHandle);
    private static native void nativeResetConfiguration(long configHandle);

    private static native void nativeSetCompilationStrategy(long configHandle, int strategy);
    private static native void nativeSetOptimizationLevel(long configHandle, int level);
    private static native void nativeSetResourceLimits(long configHandle, long maxMemory, int maxTableElements,
                                                       int maxInstances, int maxTables, int maxMemories);

    private static native boolean nativeEnableFeature(long configHandle, String feature);
    private static native boolean nativeDisableFeature(long configHandle, String feature);
    private static native boolean nativeSetParameter(long configHandle, String key, String value);

    private static native void nativeSetMemoryConfiguration(long configHandle, long initialSize, long maxSize,
                                                           long growthIncrement, boolean guardEnabled, long guardSize);
    private static native void nativeSetThreadConfiguration(long configHandle, int maxThreads, long stackSize,
                                                           boolean poolEnabled, int poolSize, long affinityMask);
    private static native void nativeSetDebugConfiguration(long configHandle, boolean debugInfo, boolean profiling,
                                                          long samplingInterval, boolean stackTrace, int maxStackDepth);

    /**
     * Memory configuration options.
     */
    public static final class MemoryConfiguration {
        private long initialMemorySize = 64 * 1024 * 1024; // 64MB
        private long maximumMemorySize = 1024 * 1024 * 1024; // 1GB
        private long memoryGrowthIncrement = 16 * 1024 * 1024; // 16MB
        private boolean memoryGuardEnabled = true;
        private long memoryGuardSize = 4096; // 4KB

        public long getInitialMemorySize() { return initialMemorySize; }
        public MemoryConfiguration setInitialMemorySize(final long size) { this.initialMemorySize = size; return this; }

        public long getMaximumMemorySize() { return maximumMemorySize; }
        public MemoryConfiguration setMaximumMemorySize(final long size) { this.maximumMemorySize = size; return this; }

        public long getMemoryGrowthIncrement() { return memoryGrowthIncrement; }
        public MemoryConfiguration setMemoryGrowthIncrement(final long increment) { this.memoryGrowthIncrement = increment; return this; }

        public boolean isMemoryGuardEnabled() { return memoryGuardEnabled; }
        public MemoryConfiguration setMemoryGuardEnabled(final boolean enabled) { this.memoryGuardEnabled = enabled; return this; }

        public long getMemoryGuardSize() { return memoryGuardSize; }
        public MemoryConfiguration setMemoryGuardSize(final long size) { this.memoryGuardSize = size; return this; }

        @Override
        public String toString() {
            return String.format("MemoryConfiguration{initial=%d, max=%d, increment=%d, guard=%s, guardSize=%d}",
                initialMemorySize, maximumMemorySize, memoryGrowthIncrement, memoryGuardEnabled, memoryGuardSize);
        }
    }

    /**
     * Thread configuration options.
     */
    public static final class ThreadConfiguration {
        private int maxThreads = Runtime.getRuntime().availableProcessors();
        private long threadStackSize = 2 * 1024 * 1024; // 2MB
        private boolean threadPoolEnabled = true;
        private int threadPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        private long threadAffinityMask = 0; // No affinity by default

        public int getMaxThreads() { return maxThreads; }
        public ThreadConfiguration setMaxThreads(final int threads) { this.maxThreads = threads; return this; }

        public long getThreadStackSize() { return threadStackSize; }
        public ThreadConfiguration setThreadStackSize(final long size) { this.threadStackSize = size; return this; }

        public boolean isThreadPoolEnabled() { return threadPoolEnabled; }
        public ThreadConfiguration setThreadPoolEnabled(final boolean enabled) { this.threadPoolEnabled = enabled; return this; }

        public int getThreadPoolSize() { return threadPoolSize; }
        public ThreadConfiguration setThreadPoolSize(final int size) { this.threadPoolSize = size; return this; }

        public long getThreadAffinityMask() { return threadAffinityMask; }
        public ThreadConfiguration setThreadAffinityMask(final long mask) { this.threadAffinityMask = mask; return this; }

        @Override
        public String toString() {
            return String.format("ThreadConfiguration{maxThreads=%d, stackSize=%d, poolEnabled=%s, poolSize=%d, affinity=0x%x}",
                maxThreads, threadStackSize, threadPoolEnabled, threadPoolSize, threadAffinityMask);
        }
    }

    /**
     * Debug configuration options.
     */
    public static final class DebugConfiguration {
        private boolean debugInfoEnabled = false;
        private boolean profilingEnabled = false;
        private Duration profilingSamplingInterval = Duration.ofMilliseconds(10);
        private boolean stackTraceEnabled = true;
        private int maxStackTraceDepth = 100;

        public boolean isDebugInfoEnabled() { return debugInfoEnabled; }
        public DebugConfiguration setDebugInfoEnabled(final boolean enabled) { this.debugInfoEnabled = enabled; return this; }

        public boolean isProfilingEnabled() { return profilingEnabled; }
        public DebugConfiguration setProfilingEnabled(final boolean enabled) { this.profilingEnabled = enabled; return this; }

        public Duration getProfilingSamplingInterval() { return profilingSamplingInterval; }
        public DebugConfiguration setProfilingSamplingInterval(final Duration interval) { this.profilingSamplingInterval = interval; return this; }

        public boolean isStackTraceEnabled() { return stackTraceEnabled; }
        public DebugConfiguration setStackTraceEnabled(final boolean enabled) { this.stackTraceEnabled = enabled; return this; }

        public int getMaxStackTraceDepth() { return maxStackTraceDepth; }
        public DebugConfiguration setMaxStackTraceDepth(final int depth) { this.maxStackTraceDepth = depth; return this; }

        @Override
        public String toString() {
            return String.format("DebugConfiguration{debugInfo=%s, profiling=%s, samplingInterval=%s, stackTrace=%s, maxDepth=%d}",
                debugInfoEnabled, profilingEnabled, profilingSamplingInterval, stackTraceEnabled, maxStackTraceDepth);
        }
    }

    /**
     * WebAssembly feature enumeration.
     */
    public enum WasmFeature {
        BULK_MEMORY,
        MULTI_VALUE,
        REFERENCE_TYPES,
        SIMD,
        TAIL_CALL,
        THREADS,
        MULTI_MEMORY,
        EXCEPTION_HANDLING,
        FUNCTION_REFERENCES,
        GC,
        RELAXED_SIMD,
        EXTENDED_CONST,
        COMPONENT_MODEL
    }

    // Static initialization
    static {
        try {
            System.loadLibrary("wasmtime4j");
            LOGGER.log(Level.FINE, "Loaded native library for JNI advanced configuration");
        } catch (final UnsatisfiedLinkError e) {
            LOGGER.log(Level.SEVERE, "Failed to load native library", e);
            throw new RuntimeException("Failed to load native configuration library", e);
        }
    }
}