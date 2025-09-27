package ai.tegmentum.wasmtime4j.panama.config;

import ai.tegmentum.wasmtime4j.config.AdvancedConfiguration;
import ai.tegmentum.wasmtime4j.config.CompilationStrategy;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import ai.tegmentum.wasmtime4j.config.ResourceLimits;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaExceptionHandler;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.foreign.ValueLayout.*;

/**
 * Panama FFI implementation of advanced configuration capabilities for wasmtime4j.
 *
 * <p>This class provides comprehensive configuration options for WebAssembly execution,
 * including compilation strategies, optimization levels, resource limits, and
 * performance tuning parameters specifically optimized for Panama FFI-based operations.
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
public final class PanamaAdvancedConfiguration implements AdvancedConfiguration {

    private static final Logger LOGGER = Logger.getLogger(PanamaAdvancedConfiguration.class.getName());

    // Native function handles loaded via Panama FFI
    private static final MethodHandle CREATE_CONFIGURATION;
    private static final MethodHandle DESTROY_CONFIGURATION;
    private static final MethodHandle APPLY_CONFIGURATION;
    private static final MethodHandle RESET_CONFIGURATION;
    private static final MethodHandle SET_COMPILATION_STRATEGY;
    private static final MethodHandle SET_OPTIMIZATION_LEVEL;
    private static final MethodHandle SET_RESOURCE_LIMITS;
    private static final MethodHandle ENABLE_FEATURE;
    private static final MethodHandle DISABLE_FEATURE;
    private static final MethodHandle SET_PARAMETER;
    private static final MethodHandle SET_MEMORY_CONFIGURATION;
    private static final MethodHandle SET_THREAD_CONFIGURATION;
    private static final MethodHandle SET_DEBUG_CONFIGURATION;

    static {
        try {
            System.loadLibrary("wasmtime4j");
            final SymbolLookup nativeLib = SymbolLookup.loaderLookup();
            final Linker linker = Linker.nativeLinker();

            CREATE_CONFIGURATION = nativeLib.find("wasmtime4j_config_create")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_create"));

            DESTROY_CONFIGURATION = nativeLib.find("wasmtime4j_config_destroy")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_destroy"));

            APPLY_CONFIGURATION = nativeLib.find("wasmtime4j_config_apply")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_apply"));

            RESET_CONFIGURATION = nativeLib.find("wasmtime4j_config_reset")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_reset"));

            SET_COMPILATION_STRATEGY = nativeLib.find("wasmtime4j_config_set_compilation_strategy")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_set_compilation_strategy"));

            SET_OPTIMIZATION_LEVEL = nativeLib.find("wasmtime4j_config_set_optimization_level")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_set_optimization_level"));

            SET_RESOURCE_LIMITS = nativeLib.find("wasmtime4j_config_set_resource_limits")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_set_resource_limits"));

            ENABLE_FEATURE = nativeLib.find("wasmtime4j_config_enable_feature")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_enable_feature"));

            DISABLE_FEATURE = nativeLib.find("wasmtime4j_config_disable_feature")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_disable_feature"));

            SET_PARAMETER = nativeLib.find("wasmtime4j_config_set_parameter")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_set_parameter"));

            SET_MEMORY_CONFIGURATION = nativeLib.find("wasmtime4j_config_set_memory_config")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_BOOLEAN, JAVA_LONG)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_set_memory_config"));

            SET_THREAD_CONFIGURATION = nativeLib.find("wasmtime4j_config_set_thread_config")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_LONG, JAVA_BOOLEAN, JAVA_INT, JAVA_LONG)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_set_thread_config"));

            SET_DEBUG_CONFIGURATION = nativeLib.find("wasmtime4j_config_set_debug_config")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_BOOLEAN, JAVA_BOOLEAN, JAVA_LONG, JAVA_BOOLEAN, JAVA_INT)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_config_set_debug_config"));

            LOGGER.log(Level.FINE, "Loaded native configuration functions via Panama FFI");
        } catch (final Throwable e) {
            LOGGER.log(Level.SEVERE, "Failed to load native configuration library", e);
            throw new RuntimeException("Failed to load native configuration library", e);
        }
    }

    private final MemorySegment nativeConfigHandle;
    private final Arena arena;
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
     * Creates a new Panama advanced configuration with default settings.
     */
    public PanamaAdvancedConfiguration() {
        this.arena = Arena.ofConfined();
        this.configurationMap = new ConcurrentHashMap<>();
        this.runtimeParameters = new ConcurrentHashMap<>();

        try {
            this.nativeConfigHandle = (MemorySegment) CREATE_CONFIGURATION.invokeExact();
            if (nativeConfigHandle.address() == 0L) {
                throw new WasmException("Failed to create native configuration");
            }

            // Initialize with default values
            initializeDefaults();

            LOGGER.log(Level.FINE, "Created Panama advanced configuration with handle: {0}", nativeConfigHandle.address());
        } catch (final Throwable e) {
            arena.close();
            throw PanamaExceptionHandler.wrapException(e, "Failed to create Panama advanced configuration");
        }
    }

    @Override
    public AdvancedConfiguration setCompilationStrategy(final CompilationStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy cannot be null");
        validateNotApplied();

        try {
            SET_COMPILATION_STRATEGY.invokeExact(nativeConfigHandle, strategy.ordinal());
            this.compilationStrategy = strategy;
            configurationMap.put("compilation_strategy", strategy);

            LOGGER.log(Level.FINE, "Set compilation strategy: {0}", strategy);
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to set compilation strategy");
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
            SET_OPTIMIZATION_LEVEL.invokeExact(nativeConfigHandle, level.ordinal());
            this.optimizationLevel = level;
            configurationMap.put("optimization_level", level);

            LOGGER.log(Level.FINE, "Set optimization level: {0}", level);
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to set optimization level");
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
            SET_RESOURCE_LIMITS.invokeExact(nativeConfigHandle,
                limits.getMaxMemoryBytes(),
                limits.getMaxTableElements(),
                limits.getMaxInstances(),
                limits.getMaxTables(),
                limits.getMaxMemories());

            this.resourceLimits = limits;
            configurationMap.put("resource_limits", limits);

            LOGGER.log(Level.FINE, "Set resource limits: {0}", limits);
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to set resource limits");
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
            final MemorySegment featureStr = arena.allocateUtf8String(feature);
            final boolean success = (boolean) ENABLE_FEATURE.invokeExact(nativeConfigHandle, featureStr);

            if (success) {
                enabledFeatures.add(feature);
                configurationMap.put("enabled_features", enabledFeatures);

                LOGGER.log(Level.FINE, "Enabled feature: {0}", feature);
            } else {
                LOGGER.log(Level.WARNING, "Failed to enable feature: {0}", feature);
            }
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to enable feature");
        }
    }

    @Override
    public AdvancedConfiguration disableFeature(final String feature) {
        Objects.requireNonNull(feature, "feature cannot be null");
        validateNotApplied();

        try {
            final MemorySegment featureStr = arena.allocateUtf8String(feature);
            final boolean success = (boolean) DISABLE_FEATURE.invokeExact(nativeConfigHandle, featureStr);

            if (success) {
                enabledFeatures.remove(feature);
                configurationMap.put("enabled_features", enabledFeatures);

                LOGGER.log(Level.FINE, "Disabled feature: {0}", feature);
            } else {
                LOGGER.log(Level.WARNING, "Failed to disable feature: {0}", feature);
            }
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to disable feature");
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
            final MemorySegment keyStr = arena.allocateUtf8String(key);
            final MemorySegment valueStr = arena.allocateUtf8String(stringValue);

            final boolean success = (boolean) SET_PARAMETER.invokeExact(nativeConfigHandle, keyStr, valueStr);

            if (success) {
                runtimeParameters.put(key, value);

                LOGGER.log(Level.FINE, "Set parameter: {0} = {1}", new Object[]{key, value});
            } else {
                LOGGER.log(Level.WARNING, "Failed to set parameter: {0}", key);
            }
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to set parameter");
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
            final boolean success = (boolean) APPLY_CONFIGURATION.invokeExact(nativeConfigHandle);
            if (!success) {
                throw new WasmException("Failed to apply native configuration");
            }

            applied = true;
            LOGGER.log(Level.FINE, "Applied Panama advanced configuration");
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to apply configuration");
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
            RESET_CONFIGURATION.invokeExact(nativeConfigHandle);
            applied = false;

            // Reset to defaults
            initializeDefaults();

            LOGGER.log(Level.FINE, "Reset Panama advanced configuration");
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to reset configuration");
        }
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> result = new HashMap<>(configurationMap);
        result.putAll(runtimeParameters);
        result.put("applied", applied);
        result.put("runtime_type", "Panama");
        return result;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            if (nativeConfigHandle.address() != 0L) {
                DESTROY_CONFIGURATION.invokeExact(nativeConfigHandle);
            }

            arena.close();
            configurationMap.clear();
            runtimeParameters.clear();
            closed = true;

            LOGGER.log(Level.FINE, "Closed Panama advanced configuration");
        } catch (final Throwable e) {
            LOGGER.log(Level.WARNING, "Error closing Panama configuration", e);
            closed = true; // Mark as closed even on error
        }
    }

    /**
     * Gets the native configuration handle.
     *
     * @return native handle
     */
    public MemorySegment getNativeHandle() {
        validateNotClosed();
        return nativeConfigHandle;
    }

    /**
     * Gets the memory arena for this configuration.
     *
     * @return memory arena
     */
    public Arena getArena() {
        validateNotClosed();
        return arena;
    }

    /**
     * Sets memory configuration options.
     *
     * @param config memory configuration
     * @return this configuration instance
     */
    public PanamaAdvancedConfiguration setMemoryConfiguration(final MemoryConfiguration config) {
        Objects.requireNonNull(config, "config cannot be null");
        validateNotApplied();

        try {
            SET_MEMORY_CONFIGURATION.invokeExact(nativeConfigHandle,
                config.getInitialMemorySize(),
                config.getMaximumMemorySize(),
                config.getMemoryGrowthIncrement(),
                config.isMemoryGuardEnabled(),
                config.getMemoryGuardSize());

            this.memoryConfig = config;
            configurationMap.put("memory_config", config);

            LOGGER.log(Level.FINE, "Set memory configuration: {0}", config);
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to set memory configuration");
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
    public PanamaAdvancedConfiguration setThreadConfiguration(final ThreadConfiguration config) {
        Objects.requireNonNull(config, "config cannot be null");
        validateNotApplied();

        try {
            SET_THREAD_CONFIGURATION.invokeExact(nativeConfigHandle,
                config.getMaxThreads(),
                config.getThreadStackSize(),
                config.isThreadPoolEnabled(),
                config.getThreadPoolSize(),
                config.getThreadAffinityMask());

            this.threadConfig = config;
            configurationMap.put("thread_config", config);

            LOGGER.log(Level.FINE, "Set thread configuration: {0}", config);
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to set thread configuration");
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
    public PanamaAdvancedConfiguration setDebugConfiguration(final DebugConfiguration config) {
        Objects.requireNonNull(config, "config cannot be null");
        validateNotApplied();

        try {
            SET_DEBUG_CONFIGURATION.invokeExact(nativeConfigHandle,
                config.isDebugInfoEnabled(),
                config.isProfilingEnabled(),
                config.getProfilingSamplingInterval().toMillis(),
                config.isStackTraceEnabled(),
                config.getMaxStackTraceDepth());

            this.debugConfig = config;
            configurationMap.put("debug_config", config);

            LOGGER.log(Level.FINE, "Set debug configuration: {0}", config);
            return this;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to set debug configuration");
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
        runtimeParameters.put("panama.optimization.enabled", true);
        runtimeParameters.put("panama.arena.size", 1024 * 1024); // 1MB
        runtimeParameters.put("panama.cache.enabled", true);
        runtimeParameters.put("panama.parallel.compilation", true);
    }

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
}