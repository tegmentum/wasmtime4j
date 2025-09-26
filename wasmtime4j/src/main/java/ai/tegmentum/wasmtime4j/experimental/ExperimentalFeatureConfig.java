package ai.tegmentum.wasmtime4j.experimental;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

/**
 * Configuration for experimental WebAssembly features and cutting-edge Wasmtime capabilities.
 *
 * <p><b>WARNING:</b> Experimental features are unstable and subject to change.
 * They should only be used for testing, development, and research purposes.
 * Use in production environments is strongly discouraged.
 *
 * <p>This class provides comprehensive configuration for:
 * <ul>
 *   <li>Committee-stage WebAssembly proposals</li>
 *   <li>Unreleased Wasmtime optimization features</li>
 *   <li>Beta compilation and execution enhancements</li>
 *   <li>Experimental security and sandboxing features</li>
 *   <li>Cutting-edge performance optimizations</li>
 *   <li>Experimental WASI extensions</li>
 *   <li>Advanced debugging and profiling capabilities</li>
 * </ul>
 *
 * @since 1.0.0
 * @see ai.tegmentum.wasmtime4j.EngineConfig
 */
public final class ExperimentalFeatureConfig {

    private final Map<ExperimentalFeature, Boolean> enabledFeatures = new HashMap<>();
    private final Map<String, Object> featureParameters = new HashMap<>();

    // Stack switching configuration
    private long stackSwitchingStackSize = 1024 * 1024; // 1MB default
    private int stackSwitchingMaxConcurrentStacks = 100;
    private StackSwitchingStrategy stackSwitchingStrategy = StackSwitchingStrategy.COOPERATIVE;
    private boolean enableCoroutineSupport = false;
    private boolean enableFiberSupport = false;

    // Call/CC configuration
    private int callCcMaxContinuations = 1000;
    private ContinuationStorageStrategy callCcStorageStrategy = ContinuationStorageStrategy.HYBRID;
    private boolean callCcCompressionEnabled = false;
    private boolean callCcExceptionIntegration = false;

    // Memory64 extended configuration
    private boolean memory64LargeAddressOptimizations = false;
    private boolean memory64CrossMemoryAddressing = false;
    private boolean memory64VirtualMapping = false;
    private boolean memory64LazyAllocation = true;
    private boolean memory64HugePages = false;

    // Custom page sizes configuration
    private final Set<Integer> customPageSizesSupported = new HashSet<>();
    private PageSizeStrategy customPageSizeStrategy = PageSizeStrategy.SYSTEM;
    private int minAlignment = 1;
    private int preferredAlignment = 8;
    private boolean strictAlignment = false;

    // Shared-everything threads configuration
    private int sharedThreadsMinThreads = 1;
    private int sharedThreadsMaxThreads = Runtime.getRuntime().availableProcessors();
    private boolean sharedThreadsAffinityEnabled = false;
    private boolean sharedThreadsGlobalStateSharing = false;
    private boolean sharedThreadsMemorySharing = false;
    private boolean sharedThreadsTableSharing = false;

    // Type imports configuration
    private TypeValidationStrategy typeImportsValidationStrategy = TypeValidationStrategy.STRICT;
    private ImportResolutionMechanism typeImportsResolutionMechanism = ImportResolutionMechanism.STATIC;
    private boolean typeImportsStructuralCompatibility = true;
    private boolean typeImportsVersionCompatibility = true;

    // String imports configuration
    private StringEncodingFormat stringImportsEncodingFormat = StringEncodingFormat.UTF8;
    private boolean stringImportsInterning = true;
    private boolean stringImportsLazyDecoding = true;
    private boolean stringImportsCompression = false;
    private boolean stringImportsJsInterop = false;

    // Resource types configuration
    private boolean resourceTypesAutomaticCleanup = true;
    private boolean resourceTypesReferenceCounting = false;
    private boolean resourceTypesWeakReferences = false;
    private ResourceCleanupStrategy resourceTypesCleanupStrategy = ResourceCleanupStrategy.AUTOMATIC;

    // Interface types configuration
    private InterfaceValidationLevel interfaceTypesValidationLevel = InterfaceValidationLevel.STANDARD;
    private boolean interfaceTypesAutomaticAdapterGeneration = true;
    private int interfaceTypesOptimizationLevel = 2;
    private boolean interfaceTypesCInterop = false;
    private boolean interfaceTypesRustInterop = false;
    private boolean interfaceTypesJavascriptInterop = false;

    // Flexible vectors configuration
    private boolean flexibleVectorsDynamicSizing = false;
    private boolean flexibleVectorsAutoVectorization = true;
    private boolean flexibleVectorsLoopUnrolling = true;
    private boolean flexibleVectorsTargetOptimization = true;
    private boolean flexibleVectorsSimdFallback = true;
    private boolean flexibleVectorsPlatformSpecificSimd = true;

    // Advanced compilation features
    private boolean enableAdvancedJitOptimizations = false;
    private boolean enableMachineCodeCaching = false;
    private boolean enableCrossModuleOptimizations = false;
    private boolean enableSpeculativeOptimizations = false;
    private TierUpStrategy tierUpStrategy = TierUpStrategy.ADAPTIVE;

    // Security and sandboxing features
    private boolean enableAdvancedSandboxing = false;
    private boolean enableResourceLimiting = false;
    private boolean enableCapabilityBasedSecurity = false;
    private boolean enableCryptographicValidation = false;
    private SecurityLevel securityLevel = SecurityLevel.STANDARD;

    // Debugging and profiling features
    private boolean enableAdvancedProfiling = false;
    private boolean enableRuntimeInstrumentation = false;
    private boolean enableHardwarePerformanceCounters = false;
    private boolean enableMemoryProfiling = false;
    private boolean enableExecutionTracing = false;
    private ProfilingGranularity profilingGranularity = ProfilingGranularity.FUNCTION;

    // WASI extensions
    private boolean enableWasiPreview2 = false;
    private boolean enableWasiNetworking = false;
    private boolean enableWasiFilesystem = false;
    private boolean enableWasiSockets = false;
    private boolean enableWasiHttp = false;
    private boolean enableWasiRandomize = false;

    /**
     * Creates a new experimental feature configuration with default settings.
     * All experimental features are disabled by default.
     */
    public ExperimentalFeatureConfig() {
        // Initialize with default supported page sizes
        customPageSizesSupported.add(4096);
        customPageSizesSupported.add(8192);
        customPageSizesSupported.add(16384);
        customPageSizesSupported.add(65536);
    }

    /**
     * Enables or disables an experimental feature.
     *
     * @param feature the experimental feature to configure
     * @param enabled true to enable the feature, false to disable
     * @return this configuration for method chaining
     * @throws IllegalArgumentException if feature is null
     */
    public ExperimentalFeatureConfig setFeature(final ExperimentalFeature feature, final boolean enabled) {
        Objects.requireNonNull(feature, "Experimental feature cannot be null");
        enabledFeatures.put(feature, enabled);
        return this;
    }

    /**
     * Checks if an experimental feature is enabled.
     *
     * @param feature the experimental feature to check
     * @return true if the feature is enabled, false otherwise
     * @throws IllegalArgumentException if feature is null
     */
    public boolean isFeatureEnabled(final ExperimentalFeature feature) {
        Objects.requireNonNull(feature, "Experimental feature cannot be null");
        return enabledFeatures.getOrDefault(feature, false);
    }

    /**
     * Sets a parameter for an experimental feature.
     *
     * @param key the parameter key
     * @param value the parameter value
     * @return this configuration for method chaining
     * @throws IllegalArgumentException if key is null
     */
    public ExperimentalFeatureConfig setParameter(final String key, final Object value) {
        Objects.requireNonNull(key, "Parameter key cannot be null");
        if (value == null) {
            featureParameters.remove(key);
        } else {
            featureParameters.put(key, value);
        }
        return this;
    }

    /**
     * Gets a parameter for an experimental feature.
     *
     * @param key the parameter key
     * @param defaultValue the default value if parameter is not set
     * @param <T> the parameter type
     * @return the parameter value or default value
     * @throws IllegalArgumentException if key is null
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(final String key, final T defaultValue) {
        Objects.requireNonNull(key, "Parameter key cannot be null");
        return (T) featureParameters.getOrDefault(key, defaultValue);
    }

    // Stack switching configuration methods

    /**
     * Configures stack switching parameters.
     *
     * @param stackSize stack size in bytes
     * @param maxStacks maximum concurrent stacks
     * @param strategy switching strategy
     * @return this configuration for method chaining
     */
    public ExperimentalFeatureConfig configureStackSwitching(
            final long stackSize,
            final int maxStacks,
            final StackSwitchingStrategy strategy) {
        if (stackSize < 4096) {
            throw new IllegalArgumentException("Stack size must be at least 4KB");
        }
        if (maxStacks <= 0) {
            throw new IllegalArgumentException("Maximum stacks must be positive");
        }
        Objects.requireNonNull(strategy, "Stack switching strategy cannot be null");

        this.stackSwitchingStackSize = stackSize;
        this.stackSwitchingMaxConcurrentStacks = maxStacks;
        this.stackSwitchingStrategy = strategy;
        return this;
    }

    /**
     * Enables coroutine support for stack switching.
     *
     * @param enabled true to enable coroutine support
     * @return this configuration for method chaining
     */
    public ExperimentalFeatureConfig setCoroutineSupport(final boolean enabled) {
        this.enableCoroutineSupport = enabled;
        return this;
    }

    /**
     * Enables fiber support for stack switching.
     *
     * @param enabled true to enable fiber support
     * @return this configuration for method chaining
     */
    public ExperimentalFeatureConfig setFiberSupport(final boolean enabled) {
        this.enableFiberSupport = enabled;
        return this;
    }

    // Call/CC configuration methods

    /**
     * Configures call/cc parameters.
     *
     * @param maxContinuations maximum number of continuations
     * @param storageStrategy continuation storage strategy
     * @param compressionEnabled whether to enable compression
     * @return this configuration for method chaining
     */
    public ExperimentalFeatureConfig configureCallCc(
            final int maxContinuations,
            final ContinuationStorageStrategy storageStrategy,
            final boolean compressionEnabled) {
        if (maxContinuations <= 0) {
            throw new IllegalArgumentException("Maximum continuations must be positive");
        }
        Objects.requireNonNull(storageStrategy, "Storage strategy cannot be null");

        this.callCcMaxContinuations = maxContinuations;
        this.callCcStorageStrategy = storageStrategy;
        this.callCcCompressionEnabled = compressionEnabled;
        return this;
    }

    // Security configuration methods

    /**
     * Configures security level and features.
     *
     * @param level the security level
     * @param enableAdvancedSandboxing whether to enable advanced sandboxing
     * @param enableResourceLimiting whether to enable resource limiting
     * @return this configuration for method chaining
     */
    public ExperimentalFeatureConfig configureSecurity(
            final SecurityLevel level,
            final boolean enableAdvancedSandboxing,
            final boolean enableResourceLimiting) {
        Objects.requireNonNull(level, "Security level cannot be null");
        this.securityLevel = level;
        this.enableAdvancedSandboxing = enableAdvancedSandboxing;
        this.enableResourceLimiting = enableResourceLimiting;
        return this;
    }

    // Profiling configuration methods

    /**
     * Configures profiling and debugging features.
     *
     * @param enableAdvancedProfiling whether to enable advanced profiling
     * @param enableTracing whether to enable execution tracing
     * @param granularity profiling granularity
     * @return this configuration for method chaining
     */
    public ExperimentalFeatureConfig configureProfiling(
            final boolean enableAdvancedProfiling,
            final boolean enableTracing,
            final ProfilingGranularity granularity) {
        Objects.requireNonNull(granularity, "Profiling granularity cannot be null");
        this.enableAdvancedProfiling = enableAdvancedProfiling;
        this.enableExecutionTracing = enableTracing;
        this.profilingGranularity = granularity;
        return this;
    }

    // WASI extensions configuration

    /**
     * Configures WASI preview 2 and extensions.
     *
     * @param enablePreview2 whether to enable WASI preview 2
     * @param enableNetworking whether to enable WASI networking
     * @param enableFilesystem whether to enable WASI filesystem extensions
     * @return this configuration for method chaining
     */
    public ExperimentalFeatureConfig configureWasiExtensions(
            final boolean enablePreview2,
            final boolean enableNetworking,
            final boolean enableFilesystem) {
        this.enableWasiPreview2 = enablePreview2;
        this.enableWasiNetworking = enableNetworking;
        this.enableWasiFilesystem = enableFilesystem;
        return this;
    }

    /**
     * Creates a configuration with all experimental features enabled.
     * <p>
     * <b>WARNING:</b> This configuration is intended for testing and development only.
     * Do not use in production environments.
     *
     * @return a new configuration with all experimental features enabled
     */
    public static ExperimentalFeatureConfig allFeaturesEnabled() {
        final ExperimentalFeatureConfig config = new ExperimentalFeatureConfig();

        // Enable all experimental features
        for (final ExperimentalFeature feature : ExperimentalFeature.values()) {
            config.setFeature(feature, true);
        }

        // Configure advanced settings
        config.enableAdvancedJitOptimizations = true;
        config.enableAdvancedSandboxing = true;
        config.enableAdvancedProfiling = true;
        config.enableWasiPreview2 = true;
        config.enableWasiNetworking = true;
        config.enableWasiFilesystem = true;
        config.enableCoroutineSupport = true;
        config.enableFiberSupport = true;
        config.securityLevel = SecurityLevel.MAXIMUM;
        config.tierUpStrategy = TierUpStrategy.AGGRESSIVE;

        return config;
    }

    /**
     * Creates a configuration optimized for performance research.
     *
     * @return a new configuration optimized for performance research
     */
    public static ExperimentalFeatureConfig forPerformanceResearch() {
        final ExperimentalFeatureConfig config = new ExperimentalFeatureConfig();

        config.setFeature(ExperimentalFeature.ADVANCED_JIT_OPTIMIZATIONS, true);
        config.setFeature(ExperimentalFeature.MACHINE_CODE_CACHING, true);
        config.setFeature(ExperimentalFeature.CROSS_MODULE_OPTIMIZATIONS, true);
        config.setFeature(ExperimentalFeature.SPECULATIVE_OPTIMIZATIONS, true);
        config.setFeature(ExperimentalFeature.HARDWARE_PERFORMANCE_COUNTERS, true);
        config.setFeature(ExperimentalFeature.MEMORY_PROFILING, true);

        config.enableAdvancedJitOptimizations = true;
        config.enableMachineCodeCaching = true;
        config.enableCrossModuleOptimizations = true;
        config.enableSpeculativeOptimizations = true;
        config.enableHardwarePerformanceCounters = true;
        config.enableMemoryProfiling = true;
        config.tierUpStrategy = TierUpStrategy.AGGRESSIVE;
        config.profilingGranularity = ProfilingGranularity.INSTRUCTION;

        return config;
    }

    /**
     * Creates a configuration for WebAssembly proposal research.
     *
     * @return a new configuration for WebAssembly proposal research
     */
    public static ExperimentalFeatureConfig forProposalResearch() {
        final ExperimentalFeatureConfig config = new ExperimentalFeatureConfig();

        config.setFeature(ExperimentalFeature.STACK_SWITCHING, true);
        config.setFeature(ExperimentalFeature.CALL_CC, true);
        config.setFeature(ExperimentalFeature.EXTENDED_CONST_EXPRESSIONS, true);
        config.setFeature(ExperimentalFeature.MEMORY64_EXTENDED, true);
        config.setFeature(ExperimentalFeature.CUSTOM_PAGE_SIZES, true);
        config.setFeature(ExperimentalFeature.SHARED_EVERYTHING_THREADS, true);
        config.setFeature(ExperimentalFeature.TYPE_IMPORTS, true);
        config.setFeature(ExperimentalFeature.STRING_IMPORTS, true);
        config.setFeature(ExperimentalFeature.RESOURCE_TYPES, true);
        config.setFeature(ExperimentalFeature.INTERFACE_TYPES, true);
        config.setFeature(ExperimentalFeature.FLEXIBLE_VECTORS, true);

        config.enableCoroutineSupport = true;
        config.enableFiberSupport = true;

        return config;
    }

    // Getters for all configuration properties

    public Map<ExperimentalFeature, Boolean> getEnabledFeatures() {
        return new HashMap<>(enabledFeatures);
    }

    public long getStackSwitchingStackSize() {
        return stackSwitchingStackSize;
    }

    public int getStackSwitchingMaxConcurrentStacks() {
        return stackSwitchingMaxConcurrentStacks;
    }

    public StackSwitchingStrategy getStackSwitchingStrategy() {
        return stackSwitchingStrategy;
    }

    public boolean isCoroutineSupportEnabled() {
        return enableCoroutineSupport;
    }

    public boolean isFiberSupportEnabled() {
        return enableFiberSupport;
    }

    public int getCallCcMaxContinuations() {
        return callCcMaxContinuations;
    }

    public ContinuationStorageStrategy getCallCcStorageStrategy() {
        return callCcStorageStrategy;
    }

    public boolean isCallCcCompressionEnabled() {
        return callCcCompressionEnabled;
    }

    public boolean isCallCcExceptionIntegration() {
        return callCcExceptionIntegration;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public boolean isAdvancedSandboxingEnabled() {
        return enableAdvancedSandboxing;
    }

    public boolean isResourceLimitingEnabled() {
        return enableResourceLimiting;
    }

    public boolean isAdvancedProfilingEnabled() {
        return enableAdvancedProfiling;
    }

    public boolean isExecutionTracingEnabled() {
        return enableExecutionTracing;
    }

    public ProfilingGranularity getProfilingGranularity() {
        return profilingGranularity;
    }

    public boolean isWasiPreview2Enabled() {
        return enableWasiPreview2;
    }

    public boolean isWasiNetworkingEnabled() {
        return enableWasiNetworking;
    }

    public boolean isWasiFilesystemEnabled() {
        return enableWasiFilesystem;
    }

    public TierUpStrategy getTierUpStrategy() {
        return tierUpStrategy;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ExperimentalFeatureConfig that = (ExperimentalFeatureConfig) obj;
        return Objects.equals(enabledFeatures, that.enabledFeatures) &&
                Objects.equals(featureParameters, that.featureParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabledFeatures, featureParameters);
    }

    @Override
    public String toString() {
        return "ExperimentalFeatureConfig{" +
                "enabledFeatures=" + enabledFeatures +
                ", securityLevel=" + securityLevel +
                ", tierUpStrategy=" + tierUpStrategy +
                '}';
    }
}