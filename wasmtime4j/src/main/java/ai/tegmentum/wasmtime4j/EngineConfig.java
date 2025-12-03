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
  private boolean guestDebug = false;
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
  private boolean asyncSupport = false;
  private boolean generateDebugInfo = false;
  private boolean epochInterruption = false;
  private boolean coredumpOnTrap = false;

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

  // Instance allocation strategy
  private InstanceAllocationStrategy allocationStrategy = InstanceAllocationStrategy.ON_DEMAND;

  // Pooling allocator configuration
  private boolean poolingAllocatorEnabled = false;
  private int instancePoolSize = 1000;
  private long maxMemoryPerInstance = 1024L * 1024L * 1024L; // 1GB

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
   * Enables or disables guest-level debugging instrumentation.
   *
   * <p>When enabled, compiled WebAssembly code is instrumented to track VM state at every step,
   * enabling precise debugging with perfect fidelity. This allows inspection of Wasm locals and
   * operand stack values from hostcalls via the DebugFrameCursor API.
   *
   * <p>Note: Enabling this option adds instrumentation overhead but provides accurate VM state for
   * debugging purposes.
   *
   * @param guestDebug true to enable guest debugging instrumentation
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig guestDebug(final boolean guestDebug) {
    this.guestDebug = guestDebug;
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
   * Enables or disables asynchronous execution support.
   *
   * <p>When enabled, allows creation of async host functions and async execution of WebAssembly
   * code using Tokio runtime. This is required for non-blocking I/O operations and async WASI.
   *
   * <p>Note: Enabling async support requires the native Tokio runtime and may increase memory usage
   * due to async stack allocation.
   *
   * @param asyncSupport true to enable async execution support
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig asyncSupport(final boolean asyncSupport) {
    this.asyncSupport = asyncSupport;
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

  /**
   * Enables or disables coredump generation on trap.
   *
   * <p>When enabled, the engine will generate a WebAssembly coredump when a trap occurs. This
   * coredump contains diagnostic information including stack frames, globals, and memory state that
   * can be used for post-mortem debugging.
   *
   * @param enabled true to enable coredump generation on trap
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig setCoredumpOnTrap(final boolean enabled) {
    this.coredumpOnTrap = enabled;
    return this;
  }

  /**
   * Convenience method for enabling coredump generation on trap.
   *
   * <p>This is an alias for {@link #setCoredumpOnTrap(boolean)}, providing a more fluent API for
   * the builder pattern.
   *
   * @param enabled true to enable coredump generation on trap
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig coredumpOnTrap(final boolean enabled) {
    return setCoredumpOnTrap(enabled);
  }

  // Getters

  public boolean isDebugInfo() {
    return debugInfo;
  }

  public boolean isGuestDebug() {
    return guestDebug;
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

  public boolean isAsyncSupport() {
    return asyncSupport;
  }

  public boolean isGenerateDebugInfo() {
    return generateDebugInfo;
  }

  public boolean isEpochInterruption() {
    return epochInterruption;
  }

  /**
   * Returns whether coredump generation on trap is enabled.
   *
   * @return true if coredump generation on trap is enabled
   * @since 1.0.0
   */
  public boolean isCoredumpOnTrap() {
    return coredumpOnTrap;
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
        .guestDebug(true)
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
        .setCoredumpOnTrap(this.coredumpOnTrap)
        .setMaxWasmStack(this.maxWasmStack)
        .setAsyncStackSize(this.asyncStackSize)
        .asyncSupport(this.asyncSupport)
        .setCraneliftSettings(new java.util.HashMap<>(this.craneliftSettings))
        .setWasmFeatures(new java.util.HashSet<>(this.wasmFeatures));
  }

  // Note: Experimental features methods moved to advanced package

  /**
   * Builds an Engine with this configuration.
   *
   * <p>This is a convenience method that creates an Engine using this EngineConfig. It is
   * equivalent to calling {@link Engine#create(EngineConfig)} with this configuration.
   *
   * @return a new Engine instance with this configuration
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if engine creation fails
   * @since 1.0.0
   */
  public Engine build() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    return Engine.create(this);
  }

  /**
   * Convenience method for enabling fuel consumption.
   *
   * <p>This is an alias for {@link #consumeFuel(boolean)} with {@code true} parameter, providing a
   * more fluent API for the builder pattern.
   *
   * @param enabled true to enable fuel consumption
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig withFuelEnabled(final boolean enabled) {
    return consumeFuel(enabled);
  }

  /**
   * Convenience method for enabling epoch interruption.
   *
   * <p>This is an alias for {@link #setEpochInterruption(boolean)}, providing a more fluent API for
   * the builder pattern.
   *
   * @param enabled true to enable epoch interruption
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig withEpochInterruption(final boolean enabled) {
    return setEpochInterruption(enabled);
  }

  // Pooling allocator configuration methods

  /**
   * Enables or disables the pooling allocator.
   *
   * <p>When enabled, the engine will use pooling allocation strategy for improved performance in
   * scenarios where many instances are created and destroyed frequently.
   *
   * @param enabled true to enable pooling allocator
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig setPoolingAllocatorEnabled(final boolean enabled) {
    this.poolingAllocatorEnabled = enabled;
    return this;
  }

  /**
   * Sets the instance pool size for the pooling allocator.
   *
   * @param size the number of instances in the pool
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if size is not positive
   * @since 1.0.0
   */
  public EngineConfig setInstancePoolSize(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Instance pool size must be positive");
    }
    this.instancePoolSize = size;
    return this;
  }

  /**
   * Sets the maximum memory per instance in bytes for the pooling allocator.
   *
   * @param bytes maximum memory per instance in bytes
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if bytes is not positive
   * @since 1.0.0
   */
  public EngineConfig setMaxMemoryPerInstance(final long bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("Maximum memory per instance must be positive");
    }
    this.maxMemoryPerInstance = bytes;
    return this;
  }

  /**
   * Returns whether the pooling allocator is enabled.
   *
   * @return true if pooling allocator is enabled
   * @since 1.0.0
   */
  public boolean isPoolingAllocatorEnabled() {
    return poolingAllocatorEnabled;
  }

  /**
   * Returns the instance pool size.
   *
   * @return the instance pool size
   * @since 1.0.0
   */
  public int getInstancePoolSize() {
    return instancePoolSize;
  }

  /**
   * Returns the maximum memory per instance in bytes.
   *
   * @return maximum memory per instance in bytes
   * @since 1.0.0
   */
  public long getMaxMemoryPerInstance() {
    return maxMemoryPerInstance;
  }

  // Instance allocation strategy methods

  /**
   * Sets the instance allocation strategy.
   *
   * <p>The allocation strategy determines how WebAssembly instances are allocated and managed.
   * Different strategies are optimized for different use cases:
   *
   * <ul>
   *   <li>{@link InstanceAllocationStrategy#ON_DEMAND} - Default, allocates instances individually
   *   <li>{@link InstanceAllocationStrategy#POOLING} - Pre-allocates a pool for faster
   *       instantiation
   * </ul>
   *
   * <p>When using {@link InstanceAllocationStrategy#POOLING}, this automatically enables the
   * pooling allocator. Configure pool settings via {@link #setInstancePoolSize(int)} and {@link
   * #setMaxMemoryPerInstance(long)}, or use the dedicated {@link
   * ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig} for advanced configuration.
   *
   * @param strategy the allocation strategy to use
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if strategy is null
   * @since 1.0.0
   */
  public EngineConfig setAllocationStrategy(final InstanceAllocationStrategy strategy) {
    if (strategy == null) {
      throw new IllegalArgumentException("Allocation strategy cannot be null");
    }
    this.allocationStrategy = strategy;
    // Automatically enable/disable pooling based on strategy
    if (strategy == InstanceAllocationStrategy.POOLING) {
      this.poolingAllocatorEnabled = true;
    }
    return this;
  }

  /**
   * Returns the instance allocation strategy.
   *
   * @return the allocation strategy
   * @since 1.0.0
   */
  public InstanceAllocationStrategy getAllocationStrategy() {
    return allocationStrategy;
  }

  /**
   * Convenience method to configure pooling allocation with default settings.
   *
   * <p>This sets the allocation strategy to {@link InstanceAllocationStrategy#POOLING} with default
   * pool settings. For custom pool configuration, use {@link
   * #setAllocationStrategy(InstanceAllocationStrategy)} followed by pool-specific methods, or use
   * the dedicated {@link ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig}.
   *
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig withPoolingAllocation() {
    return setAllocationStrategy(InstanceAllocationStrategy.POOLING);
  }

  /**
   * Convenience method to configure pooling allocation with custom settings.
   *
   * @param instancePoolSize the number of instances in the pool
   * @param maxMemoryPerInstance maximum memory per instance in bytes
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if instancePoolSize is not positive or maxMemoryPerInstance is
   *     not positive
   * @since 1.0.0
   */
  public EngineConfig withPoolingAllocation(
      final int instancePoolSize, final long maxMemoryPerInstance) {
    return setAllocationStrategy(InstanceAllocationStrategy.POOLING)
        .setInstancePoolSize(instancePoolSize)
        .setMaxMemoryPerInstance(maxMemoryPerInstance);
  }
}
