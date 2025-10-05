package ai.tegmentum.wasmtime4j;

/**
 * Configuration options for WebAssembly engine creation.
 *
 * <p>This class provides options to customize the behavior of WebAssembly engines, including
 * compilation settings, optimization levels, and runtime features.
 *
 * @since 1.0.0
 */
public final class EngineConfig {

  private boolean debugInfo = false;
  private boolean consumeFuel = false;
  private OptimizationLevel optimizationLevel = OptimizationLevel.SPEED;
  private boolean parallelCompilation = true;
  private boolean craneliftDebugVerifier = false;
  private boolean wasmBacktraceDetails = true;
  private boolean wasmReferenceTypes = true;
  private boolean wasmSimd = true;
  private boolean wasmRelaxedSimd = false;
  private boolean wasmMultiValue = true;
  private boolean wasmBulkMemory = true;
  private boolean wasmThreads = false;
  private boolean wasmTailCall = false;
  private boolean wasmMultiMemory = false;
  private boolean wasmMemory64 = false;
  private long maxWasmStack = 0; // 0 means unlimited
  private long asyncStackSize = 0; // 0 means unlimited
  private boolean generateDebugInfo = false;
  private boolean epochInterruption = false;

  // Committee-stage experimental features (disabled by default)
  private boolean wasmStackSwitching = false;
  private boolean wasmCallCc = false;
  private boolean wasmExtendedConstExpressions = false;
  private boolean wasmMemory64Extended = false;
  private boolean wasmCustomPageSizes = false;
  private boolean wasmSharedEverythingThreads = false;
  private boolean wasmTypeImports = false;
  private boolean wasmStringImports = false;
  private boolean wasmResourceTypes = false;
  private boolean wasmInterfaceTypes = false;
  private boolean wasmFlexibleVectors = false;

  private java.util.Map<String, String> craneliftSettings = new java.util.HashMap<>();
  private java.util.Set<WasmFeature> wasmFeatures = new java.util.HashSet<>();

  // Experimental features configuration
  // Note: ExperimentalFeatureConfig moved to advanced package

  /** Creates a new engine configuration with default settings. */
  public EngineConfig() {
    // Default configuration
  }

  /**
   * Enables or disables debug information generation.
   *
   * @param debugInfo true to enable debug information
   * @return this configuration for method chaining
   */
  public EngineConfig debugInfo(final boolean debugInfo) {
    this.debugInfo = debugInfo;
    return this;
  }

  /**
   * Enables or disables fuel consumption for execution limits.
   *
   * @param consumeFuel true to enable fuel consumption
   * @return this configuration for method chaining
   */
  public EngineConfig consumeFuel(final boolean consumeFuel) {
    this.consumeFuel = consumeFuel;
    return this;
  }

  /**
   * Sets the optimization level for compilation.
   *
   * @param level the optimization level
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if level is null
   */
  public EngineConfig optimizationLevel(final OptimizationLevel level) {
    if (level == null) {
      throw new IllegalArgumentException("Optimization level cannot be null");
    }
    this.optimizationLevel = level;
    return this;
  }

  /**
   * Enables or disables parallel compilation.
   *
   * @param parallelCompilation true to enable parallel compilation
   * @return this configuration for method chaining
   */
  public EngineConfig parallelCompilation(final boolean parallelCompilation) {
    this.parallelCompilation = parallelCompilation;
    return this;
  }

  /**
   * Enables or disables the Cranelift debug verifier.
   *
   * @param craneliftDebugVerifier true to enable the debug verifier
   * @return this configuration for method chaining
   */
  public EngineConfig craneliftDebugVerifier(final boolean craneliftDebugVerifier) {
    this.craneliftDebugVerifier = craneliftDebugVerifier;
    return this;
  }

  /**
   * Sets the optimization level for compilation.
   *
   * @param level the optimization level
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if level is null
   */
  public EngineConfig setOptimizationLevel(final OptimizationLevel level) {
    return optimizationLevel(level);
  }

  /**
   * Sets Cranelift-specific settings.
   *
   * @param settings map of Cranelift setting names to values
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if settings is null
   */
  public EngineConfig setCraneliftSettings(final java.util.Map<String, String> settings) {
    if (settings == null) {
      throw new IllegalArgumentException("Cranelift settings cannot be null");
    }
    this.craneliftSettings.clear();
    this.craneliftSettings.putAll(settings);
    return this;
  }

  /**
   * Sets the maximum WebAssembly stack size in bytes.
   *
   * @param bytes maximum stack size in bytes (0 for unlimited)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if bytes is negative
   */
  public EngineConfig setMaxWasmStack(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Maximum WebAssembly stack size cannot be negative");
    }
    this.maxWasmStack = bytes;
    return this;
  }

  /**
   * Sets the async stack size in bytes.
   *
   * @param bytes async stack size in bytes (0 for unlimited)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if bytes is negative
   */
  public EngineConfig setAsyncStackSize(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Async stack size cannot be negative");
    }
    this.asyncStackSize = bytes;
    return this;
  }

  /**
   * Enables or disables generation of debug information.
   *
   * @param enabled true to enable debug information generation
   * @return this configuration for method chaining
   */
  public EngineConfig setGenerateDebugInfo(final boolean enabled) {
    this.generateDebugInfo = enabled;
    return this;
  }

  /**
   * Sets the profiling strategy for execution.
   *
   * @param strategy the profiling strategy
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if strategy is null
   */
  public EngineConfig setProfiling(final ProfilingStrategy strategy) {
    if (strategy == null) {
      throw new IllegalArgumentException("Profiling strategy cannot be null");
    }
    // Note: For now, we'll just validate the parameter.
    // Full implementation will be added with native backing.
    return this;
  }

  /**
   * Sets the WebAssembly features to enable.
   *
   * @param features set of WebAssembly features to enable
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if features is null
   */
  public EngineConfig setWasmFeatures(final java.util.Set<WasmFeature> features) {
    if (features == null) {
      throw new IllegalArgumentException("WebAssembly features cannot be null");
    }
    this.wasmFeatures.clear();
    this.wasmFeatures.addAll(features);

    // Update individual feature flags based on the set
    this.wasmReferenceTypes = features.contains(WasmFeature.REFERENCE_TYPES);
    this.wasmSimd = features.contains(WasmFeature.SIMD);
    this.wasmRelaxedSimd = features.contains(WasmFeature.RELAXED_SIMD);
    this.wasmMultiValue = features.contains(WasmFeature.MULTI_VALUE);
    this.wasmBulkMemory = features.contains(WasmFeature.BULK_MEMORY);
    this.wasmThreads = features.contains(WasmFeature.THREADS);
    this.wasmTailCall = features.contains(WasmFeature.TAIL_CALL);
    this.wasmMultiMemory = features.contains(WasmFeature.MULTI_MEMORY);
    this.wasmMemory64 = features.contains(WasmFeature.MEMORY64);

    // Update experimental feature flags
    this.wasmStackSwitching = features.contains(WasmFeature.STACK_SWITCHING);
    this.wasmCallCc = features.contains(WasmFeature.CALL_CC);
    this.wasmExtendedConstExpressions = features.contains(WasmFeature.EXTENDED_CONST_EXPRESSIONS);
    this.wasmMemory64Extended = features.contains(WasmFeature.MEMORY64_EXTENDED);
    this.wasmCustomPageSizes = features.contains(WasmFeature.CUSTOM_PAGE_SIZES);
    this.wasmSharedEverythingThreads = features.contains(WasmFeature.SHARED_EVERYTHING_THREADS);
    this.wasmTypeImports = features.contains(WasmFeature.TYPE_IMPORTS);
    this.wasmStringImports = features.contains(WasmFeature.STRING_IMPORTS);
    this.wasmResourceTypes = features.contains(WasmFeature.RESOURCE_TYPES);
    this.wasmInterfaceTypes = features.contains(WasmFeature.INTERFACE_TYPES);
    this.wasmFlexibleVectors = features.contains(WasmFeature.FLEXIBLE_VECTORS);

    return this;
  }

  /**
   * Adds a WebAssembly feature to the enabled features set.
   *
   * @param feature the WebAssembly feature to add
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if feature is null
   */
  public EngineConfig addWasmFeature(final WasmFeature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("WebAssembly feature cannot be null");
    }
    this.wasmFeatures.add(feature);

    // Update individual feature flags based on the feature
    switch (feature) {
      case REFERENCE_TYPES:
        this.wasmReferenceTypes = true;
        break;
      case SIMD:
        this.wasmSimd = true;
        break;
      case RELAXED_SIMD:
        this.wasmRelaxedSimd = true;
        break;
      case MULTI_VALUE:
        this.wasmMultiValue = true;
        break;
      case BULK_MEMORY:
        this.wasmBulkMemory = true;
        break;
      case THREADS:
        this.wasmThreads = true;
        break;
      case TAIL_CALL:
        this.wasmTailCall = true;
        break;
      case MULTI_MEMORY:
        this.wasmMultiMemory = true;
        break;
      case MEMORY64:
        this.wasmMemory64 = true;
        break;
      case COMPONENT_MODEL:
        // Component Model is handled at the runtime level
        break;
      case GC:
        // GC is handled at the runtime level
        break;
      case EXCEPTIONS:
        // Exceptions are handled at the runtime level
        break;
      case STACK_SWITCHING:
        this.wasmStackSwitching = true;
        break;
      case CALL_CC:
        this.wasmCallCc = true;
        break;
      case EXTENDED_CONST_EXPRESSIONS:
        this.wasmExtendedConstExpressions = true;
        break;
      case MEMORY64_EXTENDED:
        this.wasmMemory64Extended = true;
        break;
      case CUSTOM_PAGE_SIZES:
        this.wasmCustomPageSizes = true;
        break;
      case SHARED_EVERYTHING_THREADS:
        this.wasmSharedEverythingThreads = true;
        break;
      case TYPE_IMPORTS:
        this.wasmTypeImports = true;
        break;
      case STRING_IMPORTS:
        this.wasmStringImports = true;
        break;
      case RESOURCE_TYPES:
        this.wasmResourceTypes = true;
        break;
      case INTERFACE_TYPES:
        this.wasmInterfaceTypes = true;
        break;
      case FLEXIBLE_VECTORS:
        this.wasmFlexibleVectors = true;
        break;
      default:
        // Unknown feature, just add to the set
        break;
    }

    return this;
  }

  /**
   * Enables or disables fuel consumption tracking.
   *
   * @param enabled true to enable fuel consumption
   * @return this configuration for method chaining
   */
  public EngineConfig setFuelConsumption(final boolean enabled) {
    return consumeFuel(enabled);
  }

  /**
   * Enables or disables epoch-based interruption.
   *
   * @param enabled true to enable epoch interruption
   * @return this configuration for method chaining
   */
  public EngineConfig setEpochInterruption(final boolean enabled) {
    this.epochInterruption = enabled;
    return this;
  }

  // Getters

  public boolean isDebugInfo() {
    return debugInfo;
  }

  public boolean isConsumeFuel() {
    return consumeFuel;
  }

  public OptimizationLevel getOptimizationLevel() {
    return optimizationLevel;
  }

  public boolean isParallelCompilation() {
    return parallelCompilation;
  }

  public boolean isCraneliftDebugVerifier() {
    return craneliftDebugVerifier;
  }

  public boolean isWasmBacktraceDetails() {
    return wasmBacktraceDetails;
  }

  public boolean isWasmReferenceTypes() {
    return wasmReferenceTypes;
  }

  public boolean isWasmSimd() {
    return wasmSimd;
  }

  public boolean isWasmRelaxedSimd() {
    return wasmRelaxedSimd;
  }

  public boolean isWasmMultiValue() {
    return wasmMultiValue;
  }

  public boolean isWasmBulkMemory() {
    return wasmBulkMemory;
  }

  public boolean isWasmThreads() {
    return wasmThreads;
  }

  public boolean isWasmTailCall() {
    return wasmTailCall;
  }

  public boolean isWasmMultiMemory() {
    return wasmMultiMemory;
  }

  public boolean isWasmMemory64() {
    return wasmMemory64;
  }

  public long getMaxWasmStack() {
    return maxWasmStack;
  }

  public long getAsyncStackSize() {
    return asyncStackSize;
  }

  public boolean isGenerateDebugInfo() {
    return generateDebugInfo;
  }

  public boolean isEpochInterruption() {
    return epochInterruption;
  }

  public java.util.Map<String, String> getCraneliftSettings() {
    return new java.util.HashMap<>(craneliftSettings);
  }

  public java.util.Set<WasmFeature> getWasmFeatures() {
    return new java.util.HashSet<>(wasmFeatures);
  }

  // Experimental feature getters

  public boolean isWasmStackSwitching() {
    return wasmStackSwitching;
  }

  public boolean isWasmCallCc() {
    return wasmCallCc;
  }

  public boolean isWasmExtendedConstExpressions() {
    return wasmExtendedConstExpressions;
  }

  public boolean isWasmMemory64Extended() {
    return wasmMemory64Extended;
  }

  public boolean isWasmCustomPageSizes() {
    return wasmCustomPageSizes;
  }

  public boolean isWasmSharedEverythingThreads() {
    return wasmSharedEverythingThreads;
  }

  public boolean isWasmTypeImports() {
    return wasmTypeImports;
  }

  public boolean isWasmStringImports() {
    return wasmStringImports;
  }

  public boolean isWasmResourceTypes() {
    return wasmResourceTypes;
  }

  public boolean isWasmInterfaceTypes() {
    return wasmInterfaceTypes;
  }

  public boolean isWasmFlexibleVectors() {
    return wasmFlexibleVectors;
  }

  /**
   * Creates a new configuration with default settings optimized for speed.
   *
   * @return a new configuration optimized for speed
   */
  public static EngineConfig forSpeed() {
    return new EngineConfig().optimizationLevel(OptimizationLevel.SPEED).parallelCompilation(true);
  }

  /**
   * Creates a new configuration with default settings optimized for size.
   *
   * @return a new configuration optimized for size
   */
  public static EngineConfig forSize() {
    return new EngineConfig().optimizationLevel(OptimizationLevel.SIZE).parallelCompilation(true);
  }

  /**
   * Creates a new configuration suitable for debugging.
   *
   * @return a new configuration suitable for debugging
   */
  public static EngineConfig forDebug() {
    return new EngineConfig()
        .debugInfo(true)
        .optimizationLevel(OptimizationLevel.NONE)
        .craneliftDebugVerifier(true);
  }

  /**
   * Creates a new configuration with experimental WebAssembly proposals enabled.
   *
   * <p><b>WARNING:</b> Experimental features are unstable and may change or be removed in future
   * versions. Use only for testing and development.
   *
   * @return a new configuration with experimental features enabled
   */
  public static EngineConfig forExperimentalFeatures() {
    return new EngineConfig()
        .addWasmFeature(WasmFeature.STACK_SWITCHING)
        .addWasmFeature(WasmFeature.CALL_CC)
        .addWasmFeature(WasmFeature.EXTENDED_CONST_EXPRESSIONS)
        .addWasmFeature(WasmFeature.MEMORY64_EXTENDED)
        .addWasmFeature(WasmFeature.CUSTOM_PAGE_SIZES)
        .addWasmFeature(WasmFeature.FLEXIBLE_VECTORS);
  }

  /**
   * Creates a new configuration with cutting-edge thread-related proposals enabled.
   *
   * <p><b>WARNING:</b> Experimental features are unstable and may change or be removed in future
   * versions. Use only for testing and development.
   *
   * @return a new configuration with threading experimental features enabled
   */
  public static EngineConfig forExperimentalThreading() {
    return new EngineConfig()
        .addWasmFeature(WasmFeature.THREADS)
        .addWasmFeature(WasmFeature.SHARED_EVERYTHING_THREADS)
        .addWasmFeature(WasmFeature.STACK_SWITCHING);
  }

  /**
   * Creates a new configuration with component model and interface proposals enabled.
   *
   * <p><b>WARNING:</b> Experimental features are unstable and may change or be removed in future
   * versions. Use only for testing and development.
   *
   * @return a new configuration with component model experimental features enabled
   */
  public static EngineConfig forExperimentalComponents() {
    return new EngineConfig()
        .addWasmFeature(WasmFeature.COMPONENT_MODEL)
        .addWasmFeature(WasmFeature.INTERFACE_TYPES)
        .addWasmFeature(WasmFeature.RESOURCE_TYPES)
        .addWasmFeature(WasmFeature.TYPE_IMPORTS)
        .addWasmFeature(WasmFeature.STRING_IMPORTS);
  }

  /**
   * Creates a deep copy of this configuration.
   *
   * <p>This method creates a new EngineConfig with all the same settings as this configuration.
   * Changes to the copy will not affect the original configuration.
   *
   * @return a new configuration with the same settings
   */
  public EngineConfig copy() {
    return new EngineConfig()
        .optimizationLevel(this.optimizationLevel)
        .parallelCompilation(this.parallelCompilation)
        .craneliftDebugVerifier(this.craneliftDebugVerifier)
        .setGenerateDebugInfo(this.generateDebugInfo)
        .setFuelConsumption(this.consumeFuel)
        .setEpochInterruption(this.epochInterruption)
        .setMaxWasmStack(this.maxWasmStack)
        .setAsyncStackSize(this.asyncStackSize)
        .setCraneliftSettings(new java.util.HashMap<>(this.craneliftSettings))
        .setWasmFeatures(new java.util.HashSet<>(this.wasmFeatures));
  }

  // Note: Experimental features methods moved to advanced package
}
