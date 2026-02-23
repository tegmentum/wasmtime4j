package ai.tegmentum.wasmtime4j.config;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.InstanceAllocationStrategy;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.execution.ProfilingStrategy;

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
  private long fuelAsyncYieldInterval = 0; // 0 means disabled
  private boolean epochInterruption = false;
  private boolean coredumpOnTrap = false;

  // Committee-stage experimental features (disabled by default)
  private boolean wasmStackSwitching = false;
  private boolean wasmExtendedConstExpressions = false;
  private boolean wasmCustomPageSizes = false;
  private boolean wasmSharedEverythingThreads = false;

  private java.util.Map<String, String> craneliftSettings = new java.util.HashMap<>();
  private java.util.Set<WasmFeature> wasmFeatures = new java.util.HashSet<>();

  // Instance allocation strategy
  private InstanceAllocationStrategy allocationStrategy = InstanceAllocationStrategy.ON_DEMAND;

  // Pooling allocator configuration
  private boolean poolingAllocatorEnabled = false;

  // GC configuration
  private boolean wasmGc = false;
  private boolean gcSupport = true;

  // Memory configuration (0 = use platform default)
  private long memoryReservation = 0;
  private long memoryGuardSize = 0;
  private long memoryReservationForGrowth = 0;
  private boolean memoryMayMove = true;
  private boolean guardBeforeLinearMemory = true;
  private int instancePoolSize = 1000;
  private long maxMemoryPerInstance = 1024L * 1024L * 1024L; // 1GB

  // Additional wasmtime 39.0.1 feature flags
  private boolean wasmWideArithmetic = false;
  private boolean wasmFunctionReferences = false;
  private boolean relaxedSimdDeterministic = false;
  private CompilationStrategy strategy = CompilationStrategy.AUTO;
  private String target = null; // null = native target

  // Profiling configuration
  private ProfilingStrategy profilingStrategy = ProfilingStrategy.NONE;

  // Security and advanced config options (wasmtime 39.0.1)
  private boolean asyncStackZeroing = false;
  private boolean nativeUnwindInfo = true;
  private boolean craneliftNanCanonicalization = false;
  private boolean memoryInitCow = true;
  private boolean wasmExceptions = false;
  private boolean wasmComponentModelAsyncBuiltins = false;
  private boolean tableLazyInit = true;

  // Module version strategy for precompiled module compatibility
  private ai.tegmentum.wasmtime4j.config.ModuleVersionStrategy moduleVersionStrategy =
      ai.tegmentum.wasmtime4j.config.ModuleVersionStrategy.WASMTIME_VERSION;
  private String moduleVersionCustom;

  // Register allocation and backtrace configuration
  private ai.tegmentum.wasmtime4j.config.RegallocAlgorithm regallocAlgorithm =
      ai.tegmentum.wasmtime4j.config.RegallocAlgorithm.BACKTRACKING;
  private ai.tegmentum.wasmtime4j.config.WasmBacktraceDetails backtraceDetails =
      ai.tegmentum.wasmtime4j.config.WasmBacktraceDetails.ENABLE;

  // Component model extensions (wasmtime 41.0.3)
  private boolean wasmComponentModelAsync = false;
  private boolean wasmComponentModelAsyncStackful = false;
  private boolean wasmComponentModelErrorContext = false;
  private boolean wasmComponentModelGc = false;
  private boolean wasmComponentModelThreading = false;

  // Platform-specific configuration
  private boolean macosUseMachPorts = true;

  // Backtrace and debugging configuration
  private boolean wasmBacktrace = true;
  private boolean generateAddressMap = true;

  // Shared memory (independent of wasm threads)
  private boolean sharedMemory = false;

  // Cache configuration
  private boolean cacheEnabled = false;
  private String cacheConfigPath = null; // null = use default cache config

  // Cranelift proof-carrying code validation
  private boolean craneliftPcc = false;

  // GC collector: "auto", "deferred_reference_counting", "null"
  private String collector = "auto";

  // Memory guaranteed dense image size in bytes
  private long memoryGuaranteedDenseImageSize = 0;

  /** Creates a new engine configuration with default settings. */
  public EngineConfig() {
    // Default configuration
  }

  /**
   * Synchronizes an individual feature boolean with the wasmFeatures Set.
   *
   * @param feature the WasmFeature enum value
   * @param enabled true to add, false to remove from the Set
   */
  private void syncFeatureToSet(final WasmFeature feature, final boolean enabled) {
    if (enabled) {
      this.wasmFeatures.add(feature);
    } else {
      this.wasmFeatures.remove(feature);
    }
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
   * Configures the fuel async yield interval for cooperative yielding.
   *
   * <p>When async support is enabled and this interval is set to a positive value, WebAssembly
   * execution will automatically yield back to the async executor after consuming this many units
   * of fuel. This enables cooperative timeslicing of multiple Wasm guests without requiring
   * epoch-based interruption.
   *
   * <p>A value of 0 (default) disables fuel-based async yielding.
   *
   * <p><b>Note:</b> Both async support and fuel consumption must be enabled for this to take
   * effect.
   *
   * @param interval the fuel units to consume before yielding (0 to disable)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if interval is negative
   * @since 1.0.0
   */
  public EngineConfig fuelAsyncYieldInterval(final long interval) {
    if (interval < 0) {
      throw new IllegalArgumentException("Fuel async yield interval cannot be negative");
    }
    this.fuelAsyncYieldInterval = interval;
    return this;
  }

  /**
   * Gets the fuel async yield interval.
   *
   * @return the fuel async yield interval (0 means disabled)
   */
  public long getFuelAsyncYieldInterval() {
    return fuelAsyncYieldInterval;
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
    this.profilingStrategy = strategy;
    return this;
  }

  /**
   * Gets the profiling strategy.
   *
   * @return the profiling strategy
   * @since 1.0.0
   */
  public ProfilingStrategy getProfilingStrategy() {
    return profilingStrategy;
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

    // Update all individual feature flags based on the set
    this.wasmReferenceTypes = features.contains(WasmFeature.REFERENCE_TYPES);
    this.wasmSimd = features.contains(WasmFeature.SIMD);
    this.wasmRelaxedSimd = features.contains(WasmFeature.RELAXED_SIMD);
    this.wasmMultiValue = features.contains(WasmFeature.MULTI_VALUE);
    this.wasmBulkMemory = features.contains(WasmFeature.BULK_MEMORY);
    this.wasmThreads = features.contains(WasmFeature.THREADS);
    this.wasmTailCall = features.contains(WasmFeature.TAIL_CALL);
    this.wasmMultiMemory = features.contains(WasmFeature.MULTI_MEMORY);
    this.wasmMemory64 = features.contains(WasmFeature.MEMORY64);
    this.wasmGc = features.contains(WasmFeature.GC);
    this.wasmExceptions = features.contains(WasmFeature.EXCEPTIONS);
    this.wasmFunctionReferences = features.contains(WasmFeature.TYPED_FUNCTION_REFERENCES);
    this.wasmWideArithmetic = features.contains(WasmFeature.WIDE_ARITHMETIC);

    // Update experimental feature flags
    this.wasmStackSwitching = features.contains(WasmFeature.STACK_SWITCHING);
    this.wasmExtendedConstExpressions = features.contains(WasmFeature.EXTENDED_CONST_EXPRESSIONS);
    this.wasmCustomPageSizes = features.contains(WasmFeature.CUSTOM_PAGE_SIZES);
    this.wasmSharedEverythingThreads = features.contains(WasmFeature.SHARED_EVERYTHING_THREADS);

    // Update component model extension flags
    this.wasmComponentModelAsync = features.contains(WasmFeature.COMPONENT_MODEL_ASYNC);
    this.wasmComponentModelAsyncBuiltins =
        features.contains(WasmFeature.COMPONENT_MODEL_ASYNC_BUILTINS);
    this.wasmComponentModelAsyncStackful =
        features.contains(WasmFeature.COMPONENT_MODEL_ASYNC_STACKFUL);
    this.wasmComponentModelErrorContext =
        features.contains(WasmFeature.COMPONENT_MODEL_ERROR_CONTEXT);
    this.wasmComponentModelGc = features.contains(WasmFeature.COMPONENT_MODEL_GC);
    this.wasmComponentModelThreading = features.contains(WasmFeature.COMPONENT_MODEL_THREADING);

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
        this.wasmGc = true;
        break;
      case EXCEPTIONS:
        this.wasmExceptions = true;
        break;
      case TYPED_FUNCTION_REFERENCES:
        this.wasmFunctionReferences = true;
        break;
      case STACK_SWITCHING:
        this.wasmStackSwitching = true;
        break;
      case EXTENDED_CONST_EXPRESSIONS:
        this.wasmExtendedConstExpressions = true;
        break;
      case CUSTOM_PAGE_SIZES:
        this.wasmCustomPageSizes = true;
        break;
      case SHARED_EVERYTHING_THREADS:
        this.wasmSharedEverythingThreads = true;
        break;
      case WIDE_ARITHMETIC:
        this.wasmWideArithmetic = true;
        break;
      case COMPONENT_MODEL_ASYNC:
        this.wasmComponentModelAsync = true;
        break;
      case COMPONENT_MODEL_ASYNC_BUILTINS:
        this.wasmComponentModelAsyncBuiltins = true;
        break;
      case COMPONENT_MODEL_ASYNC_STACKFUL:
        this.wasmComponentModelAsyncStackful = true;
        break;
      case COMPONENT_MODEL_ERROR_CONTEXT:
        this.wasmComponentModelErrorContext = true;
        break;
      case COMPONENT_MODEL_GC:
        this.wasmComponentModelGc = true;
        break;
      case COMPONENT_MODEL_THREADING:
        this.wasmComponentModelThreading = true;
        break;
      default:
        // Unknown feature, just add to the set
        break;
    }

    return this;
  }

  /**
   * Enables or disables epoch-based interruption.
   *
   * @param enabled true to enable epoch interruption
   * @return this configuration for method chaining
   */
  public EngineConfig epochInterruption(final boolean enabled) {
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
  public EngineConfig coredumpOnTrap(final boolean enabled) {
    this.coredumpOnTrap = enabled;
    return this;
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

  public boolean isWasmExtendedConstExpressions() {
    return wasmExtendedConstExpressions;
  }

  public boolean isWasmCustomPageSizes() {
    return wasmCustomPageSizes;
  }

  public boolean isWasmSharedEverythingThreads() {
    return wasmSharedEverythingThreads;
  }

  // ===== Register Allocation and Backtrace Configuration =====

  /**
   * Sets the register allocation algorithm for the Cranelift compiler.
   *
   * @param algorithm the register allocation algorithm
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if algorithm is null
   * @since 1.1.0
   */
  public EngineConfig regallocAlgorithm(
      final ai.tegmentum.wasmtime4j.config.RegallocAlgorithm algorithm) {
    if (algorithm == null) {
      throw new IllegalArgumentException("Register allocation algorithm cannot be null");
    }
    this.regallocAlgorithm = algorithm;
    return this;
  }

  /**
   * Gets the register allocation algorithm.
   *
   * @return the register allocation algorithm
   * @since 1.1.0
   */
  public ai.tegmentum.wasmtime4j.config.RegallocAlgorithm getRegallocAlgorithm() {
    return regallocAlgorithm;
  }

  /**
   * Sets the level of detail for WebAssembly backtraces.
   *
   * @param details the backtrace detail level
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if details is null
   * @since 1.1.0
   */
  public EngineConfig backtraceDetails(
      final ai.tegmentum.wasmtime4j.config.WasmBacktraceDetails details) {
    if (details == null) {
      throw new IllegalArgumentException("Backtrace details cannot be null");
    }
    this.backtraceDetails = details;
    return this;
  }

  /**
   * Gets the backtrace detail level.
   *
   * @return the backtrace detail level
   * @since 1.1.0
   */
  public ai.tegmentum.wasmtime4j.config.WasmBacktraceDetails getBacktraceDetails() {
    return backtraceDetails;
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
        .addWasmFeature(WasmFeature.EXTENDED_CONST_EXPRESSIONS)
        .addWasmFeature(WasmFeature.CUSTOM_PAGE_SIZES);
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
        .addWasmFeature(WasmFeature.COMPONENT_MODEL);
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
    final EngineConfig c = new EngineConfig();
    // Core settings
    c.debugInfo = this.debugInfo;
    c.guestDebug = this.guestDebug;
    c.consumeFuel = this.consumeFuel;
    c.optimizationLevel = this.optimizationLevel;
    c.parallelCompilation = this.parallelCompilation;
    c.craneliftDebugVerifier = this.craneliftDebugVerifier;
    // Wasm feature flags
    c.wasmReferenceTypes = this.wasmReferenceTypes;
    c.wasmSimd = this.wasmSimd;
    c.wasmRelaxedSimd = this.wasmRelaxedSimd;
    c.wasmMultiValue = this.wasmMultiValue;
    c.wasmBulkMemory = this.wasmBulkMemory;
    c.wasmThreads = this.wasmThreads;
    c.wasmTailCall = this.wasmTailCall;
    c.wasmMultiMemory = this.wasmMultiMemory;
    c.wasmMemory64 = this.wasmMemory64;
    // Stack and async
    c.maxWasmStack = this.maxWasmStack;
    c.asyncStackSize = this.asyncStackSize;
    c.asyncSupport = this.asyncSupport;
    c.fuelAsyncYieldInterval = this.fuelAsyncYieldInterval;
    c.epochInterruption = this.epochInterruption;
    c.coredumpOnTrap = this.coredumpOnTrap;
    // Experimental features
    c.wasmStackSwitching = this.wasmStackSwitching;
    c.wasmExtendedConstExpressions = this.wasmExtendedConstExpressions;
    c.wasmCustomPageSizes = this.wasmCustomPageSizes;
    c.wasmSharedEverythingThreads = this.wasmSharedEverythingThreads;
    // Maps and sets (defensive copy)
    c.craneliftSettings = new java.util.HashMap<>(this.craneliftSettings);
    c.wasmFeatures = new java.util.HashSet<>(this.wasmFeatures);
    // Allocation strategy
    c.allocationStrategy = this.allocationStrategy;
    c.poolingAllocatorEnabled = this.poolingAllocatorEnabled;
    // GC configuration
    c.wasmGc = this.wasmGc;
    c.gcSupport = this.gcSupport;
    // Memory configuration
    c.memoryReservation = this.memoryReservation;
    c.memoryGuardSize = this.memoryGuardSize;
    c.memoryReservationForGrowth = this.memoryReservationForGrowth;
    c.memoryMayMove = this.memoryMayMove;
    c.guardBeforeLinearMemory = this.guardBeforeLinearMemory;
    c.instancePoolSize = this.instancePoolSize;
    c.maxMemoryPerInstance = this.maxMemoryPerInstance;
    // Additional feature flags
    c.wasmWideArithmetic = this.wasmWideArithmetic;
    c.wasmFunctionReferences = this.wasmFunctionReferences;
    c.relaxedSimdDeterministic = this.relaxedSimdDeterministic;
    c.strategy = this.strategy;
    c.target = this.target;
    // Profiling
    c.profilingStrategy = this.profilingStrategy;
    // Security and advanced config
    c.asyncStackZeroing = this.asyncStackZeroing;
    c.nativeUnwindInfo = this.nativeUnwindInfo;
    c.craneliftNanCanonicalization = this.craneliftNanCanonicalization;
    c.memoryInitCow = this.memoryInitCow;
    c.wasmExceptions = this.wasmExceptions;
    c.wasmComponentModelAsyncBuiltins = this.wasmComponentModelAsyncBuiltins;
    c.tableLazyInit = this.tableLazyInit;
    // Module version
    c.moduleVersionStrategy = this.moduleVersionStrategy;
    c.moduleVersionCustom = this.moduleVersionCustom;
    // Register allocation and backtrace
    c.regallocAlgorithm = this.regallocAlgorithm;
    c.backtraceDetails = this.backtraceDetails;
    // Component model extensions
    c.wasmComponentModelAsync = this.wasmComponentModelAsync;
    c.wasmComponentModelAsyncStackful = this.wasmComponentModelAsyncStackful;
    c.wasmComponentModelErrorContext = this.wasmComponentModelErrorContext;
    c.wasmComponentModelGc = this.wasmComponentModelGc;
    c.wasmComponentModelThreading = this.wasmComponentModelThreading;
    // Platform-specific
    c.macosUseMachPorts = this.macosUseMachPorts;
    // Backtrace and debugging
    c.wasmBacktrace = this.wasmBacktrace;
    c.generateAddressMap = this.generateAddressMap;
    // Shared memory
    c.sharedMemory = this.sharedMemory;
    // Cranelift PCC
    c.craneliftPcc = this.craneliftPcc;
    // GC collector
    c.collector = this.collector;
    // Memory guaranteed dense image size
    c.memoryGuaranteedDenseImageSize = this.memoryGuaranteedDenseImageSize;
    return c;
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

  // GC configuration methods

  /**
   * Enables or disables the WebAssembly Garbage Collection proposal.
   *
   * <p>When enabled, this gates support for struct and array types, as well as i31ref. The GC
   * proposal depends on the function references proposal.
   *
   * <p><b>Note:</b> Enabling this requires GC support to also be enabled via {@link
   * #gcSupport(boolean)}.
   *
   * @param enable true to enable the GC proposal
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig wasmGc(final boolean enable) {
    this.wasmGc = enable;
    if (enable) {
      this.gcSupport = true;
    }
    syncFeatureToSet(WasmFeature.GC, enable);
    return this;
  }

  /**
   * Enables or disables GC support infrastructure in Wasmtime.
   *
   * <p>This gates the entire GC infrastructure including heap allocation and collection. This must
   * be enabled for GC-dependent proposals like {@link #wasmGc(boolean)} to function.
   *
   * @param enable true to enable GC support
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig gcSupport(final boolean enable) {
    this.gcSupport = enable;
    return this;
  }

  /**
   * Returns whether the WebAssembly GC proposal is enabled.
   *
   * @return true if GC proposal is enabled
   * @since 1.0.0
   */
  public boolean isWasmGc() {
    return wasmGc;
  }

  /**
   * Returns whether GC support is enabled.
   *
   * @return true if GC support is enabled
   * @since 1.0.0
   */
  public boolean isGcSupport() {
    return gcSupport;
  }

  // Memory configuration methods

  /**
   * Sets the initial linear memory allocation capacity in bytes.
   *
   * <p>This configures the size of the initial virtual memory mapping used for linear memory. A
   * value of 0 means use the platform default (4GiB on 64-bit, 10MiB on 32-bit platforms).
   *
   * @param bytes the reservation size in bytes, or 0 for platform default
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if bytes is negative
   * @since 1.0.0
   */
  public EngineConfig memoryReservation(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Memory reservation cannot be negative");
    }
    this.memoryReservation = bytes;
    return this;
  }

  /**
   * Sets the guard region size at the end of linear memory in bytes.
   *
   * <p>The guard region is unmapped memory that traps on access, providing defense against
   * out-of-bounds accesses. A value of 0 means use the platform default (32MiB on 64-bit, 64KiB on
   * 32-bit platforms).
   *
   * @param bytes the guard size in bytes, or 0 for platform default
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if bytes is negative
   * @since 1.0.0
   */
  public EngineConfig memoryGuardSize(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Memory guard size cannot be negative");
    }
    this.memoryGuardSize = bytes;
    return this;
  }

  /**
   * Sets extra virtual memory space reserved for growth in bytes.
   *
   * <p>This reserves additional virtual address space to allow memory growth without relocating the
   * linear memory base pointer. A value of 0 means use the platform default (2GiB on 64-bit, 1MiB
   * on 32-bit platforms).
   *
   * @param bytes the reservation for growth in bytes, or 0 for platform default
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if bytes is negative
   * @since 1.0.0
   */
  public EngineConfig memoryReservationForGrowth(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Memory reservation for growth cannot be negative");
    }
    this.memoryReservationForGrowth = bytes;
    return this;
  }

  /**
   * Controls whether linear memory base pointers may relocate during growth.
   *
   * <p>When set to false, the linear memory base pointer is guaranteed to remain static, enabling
   * certain optimizations. However, this limits memory growth to the initial reservation size.
   *
   * <p>Default: true
   *
   * @param enable true to allow memory movement
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig memoryMayMove(final boolean enable) {
    this.memoryMayMove = enable;
    return this;
  }

  /**
   * Indicates whether a guard region precedes linear memory allocations.
   *
   * <p>When enabled, an additional guard region is placed before the linear memory allocation. This
   * provides defense-in-depth against potential code generator bugs that might cause negative
   * offsets.
   *
   * <p>Default: true
   *
   * @param enable true to enable guard before memory
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig guardBeforeLinearMemory(final boolean enable) {
    this.guardBeforeLinearMemory = enable;
    return this;
  }

  /**
   * Returns the configured memory reservation in bytes.
   *
   * @return the memory reservation, or 0 for platform default
   * @since 1.0.0
   */
  public long getMemoryReservation() {
    return memoryReservation;
  }

  /**
   * Returns the configured memory guard size in bytes.
   *
   * @return the memory guard size, or 0 for platform default
   * @since 1.0.0
   */
  public long getMemoryGuardSize() {
    return memoryGuardSize;
  }

  /**
   * Returns the configured memory reservation for growth in bytes.
   *
   * @return the memory reservation for growth, or 0 for platform default
   * @since 1.0.0
   */
  public long getMemoryReservationForGrowth() {
    return memoryReservationForGrowth;
  }

  /**
   * Returns whether memory may move during growth.
   *
   * @return true if memory may move
   * @since 1.0.0
   */
  public boolean isMemoryMayMove() {
    return memoryMayMove;
  }

  /**
   * Returns whether a guard region precedes linear memory.
   *
   * @return true if guard before linear memory is enabled
   * @since 1.0.0
   */
  public boolean isGuardBeforeLinearMemory() {
    return guardBeforeLinearMemory;
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

  // Wasmtime 39.0.1 additional feature configuration methods

  /**
   * Enables or disables the wide arithmetic proposal.
   *
   * <p>When enabled, this allows 128-bit integer operations for cryptographic applications.
   *
   * @param enable true to enable wide arithmetic
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig wasmWideArithmetic(final boolean enable) {
    this.wasmWideArithmetic = enable;
    syncFeatureToSet(WasmFeature.WIDE_ARITHMETIC, enable);
    return this;
  }

  /**
   * Returns whether wide arithmetic is enabled.
   *
   * @return true if wide arithmetic is enabled
   * @since 1.1.0
   */
  public boolean isWasmWideArithmetic() {
    return wasmWideArithmetic;
  }

  /**
   * Enables or disables the function references proposal.
   *
   * <p>When enabled, this allows non-nullable function references and call_ref instruction.
   *
   * @param enable true to enable function references
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig wasmFunctionReferences(final boolean enable) {
    this.wasmFunctionReferences = enable;
    syncFeatureToSet(WasmFeature.TYPED_FUNCTION_REFERENCES, enable);
    return this;
  }

  /**
   * Returns whether function references are enabled.
   *
   * @return true if function references are enabled
   * @since 1.1.0
   */
  public boolean isWasmFunctionReferences() {
    return wasmFunctionReferences;
  }

  /**
   * Forces deterministic behavior for relaxed SIMD operations.
   *
   * <p>When enabled, relaxed SIMD operations will produce deterministic results across all
   * platforms, at the cost of some performance.
   *
   * @param enable true to force deterministic relaxed SIMD
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig relaxedSimdDeterministic(final boolean enable) {
    this.relaxedSimdDeterministic = enable;
    return this;
  }

  /**
   * Returns whether relaxed SIMD deterministic mode is enabled.
   *
   * @return true if relaxed SIMD deterministic mode is enabled
   * @since 1.1.0
   */
  public boolean isRelaxedSimdDeterministic() {
    return relaxedSimdDeterministic;
  }

  /**
   * Sets the compilation strategy for the engine.
   *
   * <p>The strategy determines which compiler backend to use (Cranelift vs Winch) and how
   * aggressively to optimize.
   *
   * @param strategy the compilation strategy
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if strategy is null
   * @since 1.1.0
   */
  public EngineConfig strategy(final CompilationStrategy strategy) {
    if (strategy == null) {
      throw new IllegalArgumentException("Strategy cannot be null");
    }
    this.strategy = strategy;
    return this;
  }

  /**
   * Returns the compilation strategy.
   *
   * @return the compilation strategy
   * @since 1.1.0
   */
  public CompilationStrategy getStrategy() {
    return strategy;
  }

  /**
   * Sets the cross-compilation target triple.
   *
   * <p>This allows compiling WebAssembly for a different target architecture than the host. The
   * target triple follows LLVM conventions (e.g., "x86_64-unknown-linux-gnu").
   *
   * @param target the target triple, or null for native target
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig target(final String target) {
    this.target = target;
    return this;
  }

  /**
   * Returns the cross-compilation target.
   *
   * @return the target triple, or null for native target
   * @since 1.1.0
   */
  public String getTarget() {
    return target;
  }

  // Security and advanced configuration methods (wasmtime 39.0.1)

  /**
   * Enables or disables zeroing of async stacks when not in use.
   *
   * <p>When enabled, async stacks are zeroed when transitioning out of WebAssembly code. This
   * provides defense-in-depth against potential information leaks but has a small performance cost.
   *
   * <p>Default: false
   *
   * @param enable true to enable async stack zeroing
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig asyncStackZeroing(final boolean enable) {
    this.asyncStackZeroing = enable;
    return this;
  }

  /**
   * Returns whether async stack zeroing is enabled.
   *
   * @return true if async stack zeroing is enabled
   * @since 1.1.0
   */
  public boolean isAsyncStackZeroing() {
    return asyncStackZeroing;
  }

  /**
   * Enables or disables emission of native unwind information (.eh_frame on Linux).
   *
   * <p>When enabled, the JIT compiler emits unwind tables that allow native debuggers and profilers
   * to properly unwind WebAssembly stack frames. This is useful for integration with system-level
   * debugging tools.
   *
   * <p>Default: true
   *
   * @param enable true to emit native unwind info
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig nativeUnwindInfo(final boolean enable) {
    this.nativeUnwindInfo = enable;
    return this;
  }

  /**
   * Returns whether native unwind info emission is enabled.
   *
   * @return true if native unwind info is enabled
   * @since 1.1.0
   */
  public boolean isNativeUnwindInfo() {
    return nativeUnwindInfo;
  }

  /**
   * Enables or disables NaN canonicalization in the Cranelift compiler.
   *
   * <p>When enabled, all NaN values produced by floating-point operations are canonicalized to a
   * single representation. This provides deterministic behavior across platforms but may have a
   * small performance cost.
   *
   * <p>Default: false
   *
   * @param enable true to enable NaN canonicalization
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig craneliftNanCanonicalization(final boolean enable) {
    this.craneliftNanCanonicalization = enable;
    return this;
  }

  /**
   * Returns whether Cranelift NaN canonicalization is enabled.
   *
   * @return true if NaN canonicalization is enabled
   * @since 1.1.0
   */
  public boolean isCraneliftNanCanonicalization() {
    return craneliftNanCanonicalization;
  }

  /**
   * Enables or disables copy-on-write (CoW) initialization for linear memory.
   *
   * <p>When enabled, linear memory is initialized using copy-on-write semantics, which can
   * significantly reduce memory usage when running multiple instances of the same module with
   * similar memory contents.
   *
   * <p>Default: true
   *
   * @param enable true to enable CoW memory initialization
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig memoryInitCow(final boolean enable) {
    this.memoryInitCow = enable;
    return this;
  }

  /**
   * Returns whether copy-on-write memory initialization is enabled.
   *
   * @return true if CoW initialization is enabled
   * @since 1.1.0
   */
  public boolean isMemoryInitCow() {
    return memoryInitCow;
  }

  /**
   * Enables or disables the WebAssembly exception handling proposal.
   *
   * <p>When enabled, WebAssembly modules can use the exception handling instructions (try, catch,
   * throw, rethrow) for structured error handling.
   *
   * <p>Default: false
   *
   * @param enable true to enable exception handling
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig wasmExceptions(final boolean enable) {
    this.wasmExceptions = enable;
    syncFeatureToSet(WasmFeature.EXCEPTIONS, enable);
    return this;
  }

  /**
   * Returns whether WebAssembly exception handling is enabled.
   *
   * @return true if exception handling is enabled
   * @since 1.1.0
   */
  public boolean isWasmExceptions() {
    return wasmExceptions;
  }

  /**
   * Enables or disables async builtins for the component model.
   *
   * <p>When enabled, components can use async-compatible builtin functions for operations like
   * memory allocation and string encoding. This is required for full async component model support.
   *
   * <p>Default: false
   *
   * @param enable true to enable component model async builtins
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig wasmComponentModelAsyncBuiltins(final boolean enable) {
    this.wasmComponentModelAsyncBuiltins = enable;
    syncFeatureToSet(WasmFeature.COMPONENT_MODEL_ASYNC_BUILTINS, enable);
    return this;
  }

  /**
   * Returns whether component model async builtins are enabled.
   *
   * @return true if async builtins are enabled
   * @since 1.1.0
   */
  public boolean isWasmComponentModelAsyncBuiltins() {
    return wasmComponentModelAsyncBuiltins;
  }

  /**
   * Returns whether the WebAssembly Component Model is enabled.
   *
   * <p>This checks if the COMPONENT_MODEL feature is in the enabled features set.
   *
   * @return true if component model is enabled
   * @since 1.1.0
   */
  public boolean isWasmComponentModel() {
    return wasmFeatures.contains(WasmFeature.COMPONENT_MODEL);
  }

  /**
   * Returns whether table lazy initialization is enabled.
   *
   * <p>Table lazy initialization is enabled by default, matching the Wasmtime default. When enabled,
   * tables are initialized lazily for faster instantiation but slightly slower indirect calls.
   *
   * @return true if table lazy initialization is enabled
   * @since 1.1.0
   */
  public boolean isTableLazyInit() {
    return tableLazyInit;
  }

  /**
   * Enables or disables table lazy initialization.
   *
   * <p>When enabled (the default), tables are initialized lazily which results in faster
   * instantiation but slightly slower indirect calls. When disabled, tables are initialized eagerly
   * during instantiation.
   *
   * @param enable true to enable lazy table initialization
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig tableLazyInit(final boolean enable) {
    this.tableLazyInit = enable;
    return this;
  }

  /**
   * Returns the module version strategy used for precompiled module compatibility checks.
   *
   * @return the module version strategy
   * @since 1.1.0
   */
  public ai.tegmentum.wasmtime4j.config.ModuleVersionStrategy getModuleVersionStrategy() {
    return moduleVersionStrategy;
  }

  /**
   * Sets the module version strategy for precompiled module compatibility checks.
   *
   * <p>When set to {@link ModuleVersionStrategy#WASMTIME_VERSION} (the default), modules are
   * validated against the Wasmtime version that compiled them. When set to {@link
   * ModuleVersionStrategy#NONE}, version checks are skipped. When set to {@link
   * ModuleVersionStrategy#CUSTOM}, a custom version string is used (set via {@link
   * #moduleVersionCustom(String)}).
   *
   * @param strategy the module version strategy to use
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if strategy is null
   * @since 1.1.0
   */
  public EngineConfig moduleVersionStrategy(
      final ai.tegmentum.wasmtime4j.config.ModuleVersionStrategy strategy) {
    if (strategy == null) {
      throw new IllegalArgumentException("strategy cannot be null");
    }
    this.moduleVersionStrategy = strategy;
    return this;
  }

  /**
   * Returns the custom module version string, or null if not set.
   *
   * @return the custom module version string, or null
   * @since 1.1.0
   */
  public String getModuleVersionCustom() {
    return moduleVersionCustom;
  }

  /**
   * Sets a custom module version string for precompiled module compatibility checks.
   *
   * <p>This is only used when the module version strategy is set to {@link
   * ModuleVersionStrategy#CUSTOM}. If set with a non-null value, the strategy is automatically
   * changed to CUSTOM.
   *
   * @param version the custom version string
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig moduleVersionCustom(final String version) {
    this.moduleVersionCustom = version;
    if (version != null) {
      this.moduleVersionStrategy =
          ai.tegmentum.wasmtime4j.config.ModuleVersionStrategy.CUSTOM;
    }
    return this;
  }

  // ===== Component Model Extension Configuration (wasmtime 41.0.3) =====

  /**
   * Enables or disables the component model async proposal.
   *
   * @param enable true to enable component model async
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig wasmComponentModelAsync(final boolean enable) {
    this.wasmComponentModelAsync = enable;
    syncFeatureToSet(WasmFeature.COMPONENT_MODEL_ASYNC, enable);
    return this;
  }

  /**
   * Returns whether component model async is enabled.
   *
   * @return true if component model async is enabled
   * @since 1.0.0
   */
  public boolean isWasmComponentModelAsync() {
    return wasmComponentModelAsync;
  }

  /**
   * Enables or disables the component model async stackful proposal.
   *
   * @param enable true to enable component model async stackful
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig wasmComponentModelAsyncStackful(final boolean enable) {
    this.wasmComponentModelAsyncStackful = enable;
    syncFeatureToSet(WasmFeature.COMPONENT_MODEL_ASYNC_STACKFUL, enable);
    return this;
  }

  /**
   * Returns whether component model async stackful is enabled.
   *
   * @return true if component model async stackful is enabled
   * @since 1.0.0
   */
  public boolean isWasmComponentModelAsyncStackful() {
    return wasmComponentModelAsyncStackful;
  }

  /**
   * Enables or disables the component model error context proposal.
   *
   * @param enable true to enable component model error context
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig wasmComponentModelErrorContext(final boolean enable) {
    this.wasmComponentModelErrorContext = enable;
    syncFeatureToSet(WasmFeature.COMPONENT_MODEL_ERROR_CONTEXT, enable);
    return this;
  }

  /**
   * Returns whether component model error context is enabled.
   *
   * @return true if component model error context is enabled
   * @since 1.0.0
   */
  public boolean isWasmComponentModelErrorContext() {
    return wasmComponentModelErrorContext;
  }

  /**
   * Enables or disables the component model GC proposal.
   *
   * @param enable true to enable component model GC
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig wasmComponentModelGc(final boolean enable) {
    this.wasmComponentModelGc = enable;
    syncFeatureToSet(WasmFeature.COMPONENT_MODEL_GC, enable);
    return this;
  }

  /**
   * Returns whether component model GC is enabled.
   *
   * @return true if component model GC is enabled
   * @since 1.0.0
   */
  public boolean isWasmComponentModelGc() {
    return wasmComponentModelGc;
  }

  /**
   * Enables or disables the component model threading proposal.
   *
   * @param enable true to enable component model threading
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig wasmComponentModelThreading(final boolean enable) {
    this.wasmComponentModelThreading = enable;
    syncFeatureToSet(WasmFeature.COMPONENT_MODEL_THREADING, enable);
    return this;
  }

  /**
   * Returns whether component model threading is enabled.
   *
   * @return true if component model threading is enabled
   * @since 1.0.0
   */
  public boolean isWasmComponentModelThreading() {
    return wasmComponentModelThreading;
  }

  /**
   * Enables or disables Cranelift proof-carrying code validation.
   *
   * @param enable true to enable PCC validation
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig craneliftPcc(final boolean enable) {
    this.craneliftPcc = enable;
    return this;
  }

  /**
   * Returns whether Cranelift PCC validation is enabled.
   *
   * @return true if PCC validation is enabled
   * @since 1.0.0
   */
  public boolean isCraneliftPcc() {
    return craneliftPcc;
  }

  /**
   * Configures whether to use Mach ports for trap handling on macOS.
   *
   * <p>When enabled, Wasmtime uses Mach exception ports instead of signal handlers for catching
   * out-of-bounds memory accesses and similar traps on macOS. This is generally more reliable.
   *
   * <p>Default: {@code true}
   *
   * @param enable true to use Mach ports on macOS
   * @return this config for chaining
   * @since 1.1.0
   */
  public EngineConfig macosUseMachPorts(final boolean enable) {
    this.macosUseMachPorts = enable;
    return this;
  }

  /**
   * Returns whether Mach ports are used for trap handling on macOS.
   *
   * @return true if Mach ports are enabled
   * @since 1.1.0
   */
  public boolean isMacosUseMachPorts() {
    return macosUseMachPorts;
  }

  /**
   * Configures whether WebAssembly backtraces are collected on traps and errors.
   *
   * <p>When enabled, Wasmtime collects stack trace information when a trap occurs.
   * This is separate from {@link #backtraceDetails} which controls the level of detail.
   *
   * <p>Default: {@code true}
   *
   * @param enable true to enable backtrace collection
   * @return this config for chaining
   * @since 1.1.0
   */
  public EngineConfig wasmBacktrace(final boolean enable) {
    this.wasmBacktrace = enable;
    return this;
  }

  /**
   * Returns whether WebAssembly backtrace collection is enabled.
   *
   * @return true if backtrace collection is enabled
   * @since 1.1.0
   */
  public boolean isWasmBacktrace() {
    return wasmBacktrace;
  }

  /**
   * Configures whether to generate address map information for compiled code.
   *
   * <p>Address maps provide mapping from native code addresses back to WebAssembly bytecode
   * offsets, which is useful for debugging and profiling. Disabling this can reduce compiled
   * module size.
   *
   * <p>Default: {@code true}
   *
   * @param enable true to generate address maps
   * @return this config for chaining
   * @since 1.1.0
   */
  public EngineConfig generateAddressMap(final boolean enable) {
    this.generateAddressMap = enable;
    return this;
  }

  /**
   * Returns whether address map generation is enabled.
   *
   * @return true if address maps are generated
   * @since 1.1.0
   */
  public boolean isGenerateAddressMap() {
    return generateAddressMap;
  }

  /**
   * Configures whether shared memory is enabled.
   *
   * <p>This is independent of {@link #wasmThreads} and controls whether the {@code shared}
   * attribute is allowed on memory definitions. Shared memory enables atomic operations.
   *
   * <p>Default: {@code false}
   *
   * @param enable true to enable shared memory
   * @return this config for chaining
   * @since 1.1.0
   */
  public EngineConfig sharedMemory(final boolean enable) {
    this.sharedMemory = enable;
    return this;
  }

  /**
   * Returns whether shared memory is enabled.
   *
   * @return true if shared memory is enabled
   * @since 1.1.0
   */
  public boolean isSharedMemory() {
    return sharedMemory;
  }

  /**
   * Enables compilation caching with the default cache configuration.
   *
   * <p>When enabled, compiled modules are cached on disk to speed up subsequent compilations of
   * the same module. The default cache location is platform-specific (typically
   * {@code ~/.cache/wasmtime} on Linux/macOS).
   *
   * @return this configuration for method chaining
   * @since 1.1.0
   */
  public EngineConfig cacheConfigLoadDefault() {
    this.cacheEnabled = true;
    this.cacheConfigPath = null;
    return this;
  }

  /**
   * Enables compilation caching with a custom cache configuration file.
   *
   * <p>The configuration file specifies cache location, size limits, and other cache-related
   * settings. See Wasmtime documentation for the cache configuration file format.
   *
   * @param path the path to the cache configuration file
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if path is null
   * @since 1.1.0
   */
  public EngineConfig cacheConfigLoad(final java.nio.file.Path path) {
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    this.cacheEnabled = true;
    this.cacheConfigPath = path.toString();
    return this;
  }

  /**
   * Returns whether compilation caching is enabled.
   *
   * @return true if cache is enabled
   * @since 1.1.0
   */
  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  /**
   * Returns the cache configuration file path, or null for default.
   *
   * @return the cache config path, or null if using default
   * @since 1.1.0
   */
  public String getCacheConfigPath() {
    return cacheConfigPath;
  }

  /**
   * Sets the GC collector implementation.
   *
   * <p>Valid values are "auto", "deferred_reference_counting", "null".
   *
   * @param collector the collector name
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig collector(final String collector) {
    this.collector = collector;
    return this;
  }

  /**
   * Returns the GC collector implementation name.
   *
   * @return the collector name
   * @since 1.0.0
   */
  public String getCollector() {
    return collector;
  }

  /**
   * Sets the guaranteed dense image size for linear memory initialization.
   *
   * @param bytes the size in bytes (0 for default)
   * @return this configuration for method chaining
   * @since 1.0.0
   */
  public EngineConfig memoryGuaranteedDenseImageSize(final long bytes) {
    this.memoryGuaranteedDenseImageSize = bytes;
    return this;
  }

  /**
   * Returns the guaranteed dense image size.
   *
   * @return the size in bytes (0 for default)
   * @since 1.0.0
   */
  public long getMemoryGuaranteedDenseImageSize() {
    return memoryGuaranteedDenseImageSize;
  }

  // ===== JSON Serialization =====

  /**
   * Serializes this configuration to JSON bytes for native FFI.
   *
   * <p>The JSON format matches the Rust EngineConfigFfi struct with camelCase field names.
   * Only non-default values are included to minimize payload size.
   *
   * @return UTF-8 encoded JSON bytes
   * @since 1.0.0
   */
  public byte[] toJson() {
    StringBuilder sb = new StringBuilder(512);
    sb.append('{');
    boolean first = true;

    // Strategy
    if (strategy != CompilationStrategy.AUTO) {
      first = appendJsonField(sb, first, "strategy", strategyToString(strategy));
    }

    // Optimization level
    first = appendJsonField(sb, first, "optLevel", optLevelToString(optimizationLevel));

    // Core boolean settings
    first = appendJsonBool(sb, first, "debugInfo", debugInfo);
    first = appendJsonBool(sb, first, "fuelEnabled", consumeFuel);
    first = appendJsonBool(sb, first, "epochInterruption", epochInterruption);
    first = appendJsonBool(sb, first, "asyncSupport", asyncSupport);
    first = appendJsonBool(sb, first, "coredumpOnTrap", coredumpOnTrap);
    first = appendJsonBool(sb, first, "parallelCompilation", parallelCompilation);
    first = appendJsonBool(sb, first, "nativeUnwindInfo", nativeUnwindInfo);
    first = appendJsonBool(sb, first, "guestDebug", guestDebug);

    // Stack configuration
    if (maxWasmStack > 0) {
      first = appendJsonLong(sb, first, "maxStackSize", maxWasmStack);
    }
    if (asyncStackSize > 0) {
      first = appendJsonLong(sb, first, "asyncStackSize", asyncStackSize);
    }
    first = appendJsonBool(sb, first, "asyncStackZeroing", asyncStackZeroing);

    // Memory configuration
    if (memoryReservation > 0) {
      first = appendJsonLong(sb, first, "memoryReservation", memoryReservation);
    }
    if (memoryGuardSize > 0) {
      first = appendJsonLong(sb, first, "memoryGuardSize", memoryGuardSize);
    }
    if (memoryReservationForGrowth > 0) {
      first = appendJsonLong(sb, first, "memoryReservationForGrowth", memoryReservationForGrowth);
    }
    first = appendJsonBool(sb, first, "memoryMayMove", memoryMayMove);
    first = appendJsonBool(sb, first, "guardBeforeLinearMemory", guardBeforeLinearMemory);
    first = appendJsonBool(sb, first, "memoryInitCow", memoryInitCow);
    if (memoryGuaranteedDenseImageSize > 0) {
      first = appendJsonLong(sb, first, "memoryGuaranteedDenseImageSize",
          memoryGuaranteedDenseImageSize);
    }

    // WASM features
    first = appendJsonBool(sb, first, "wasmThreads", wasmThreads);
    first = appendJsonBool(sb, first, "wasmReferenceTypes", wasmReferenceTypes);
    first = appendJsonBool(sb, first, "wasmSimd", wasmSimd);
    first = appendJsonBool(sb, first, "wasmBulkMemory", wasmBulkMemory);
    first = appendJsonBool(sb, first, "wasmMultiValue", wasmMultiValue);
    first = appendJsonBool(sb, first, "wasmMultiMemory", wasmMultiMemory);
    first = appendJsonBool(sb, first, "wasmTailCall", wasmTailCall);
    first = appendJsonBool(sb, first, "wasmRelaxedSimd", wasmRelaxedSimd);
    first = appendJsonBool(sb, first, "wasmFunctionReferences", wasmFunctionReferences);
    first = appendJsonBool(sb, first, "wasmGc", wasmGc);
    first = appendJsonBool(sb, first, "wasmExceptions", wasmExceptions);
    first = appendJsonBool(sb, first, "wasmMemory64", wasmMemory64);
    first = appendJsonBool(sb, first, "wasmExtendedConst", wasmExtendedConstExpressions);
    first = appendJsonBool(sb, first, "wasmCustomPageSizes", wasmCustomPageSizes);
    first = appendJsonBool(sb, first, "wasmWideArithmetic", wasmWideArithmetic);
    first = appendJsonBool(sb, first, "wasmStackSwitching", wasmStackSwitching);
    first = appendJsonBool(sb, first, "wasmSharedEverythingThreads", wasmSharedEverythingThreads);

    // Component model extensions
    first = appendJsonBool(sb, first, "wasmComponentModelAsync", wasmComponentModelAsync);
    first = appendJsonBool(sb, first, "wasmComponentModelAsyncBuiltins",
        wasmComponentModelAsyncBuiltins);
    first = appendJsonBool(sb, first, "wasmComponentModelAsyncStackful",
        wasmComponentModelAsyncStackful);
    first = appendJsonBool(sb, first, "wasmComponentModelErrorContext",
        wasmComponentModelErrorContext);
    first = appendJsonBool(sb, first, "wasmComponentModelGc", wasmComponentModelGc);
    first = appendJsonBool(sb, first, "wasmComponentModelThreading", wasmComponentModelThreading);

    // Cranelift settings
    first = appendJsonBool(sb, first, "craneliftDebugVerifier", craneliftDebugVerifier);
    first = appendJsonBool(sb, first, "craneliftNanCanonicalization", craneliftNanCanonicalization);
    first = appendJsonBool(sb, first, "craneliftPcc", craneliftPcc);
    first = appendJsonBool(sb, first, "macosUseMachPorts", macosUseMachPorts);
    first = appendJsonBool(sb, first, "wasmBacktrace", wasmBacktrace);
    if (backtraceDetails != null) {
      first = appendJsonField(sb, first, "backtraceDetails",
          backtraceDetails.name().toLowerCase());
    }
    first = appendJsonBool(sb, first, "generateAddressMap", generateAddressMap);
    first = appendJsonBool(sb, first, "sharedMemory", sharedMemory);
    if (regallocAlgorithm != null) {
      first = appendJsonField(sb, first, "craneliftRegallocAlgorithm",
          regallocAlgorithm == ai.tegmentum.wasmtime4j.config.RegallocAlgorithm.SINGLE_PASS
              ? "single_pass" : "backtracking");
    }

    // Cranelift flags
    if (!craneliftSettings.isEmpty()) {
      if (!first) {
        sb.append(',');
      }
      first = false;
      sb.append("\"craneliftFlags\":[");
      boolean flagFirst = true;
      for (java.util.Map.Entry<String, String> entry : craneliftSettings.entrySet()) {
        if (!flagFirst) {
          sb.append(',');
        }
        flagFirst = false;
        sb.append("{\"name\":\"");
        appendJsonEscaped(sb, entry.getKey());
        sb.append("\",\"value\":\"");
        appendJsonEscaped(sb, entry.getValue());
        sb.append("\"}");
      }
      sb.append(']');
    }

    // GC settings
    first = appendJsonBool(sb, first, "gcSupport", gcSupport);
    if (collector != null && !"auto".equals(collector)) {
      first = appendJsonField(sb, first, "collector", collector);
    }

    // Table
    first = appendJsonBool(sb, first, "tableLazyInit", tableLazyInit);

    // SIMD determinism
    first = appendJsonBool(sb, first, "relaxedSimdDeterministic", relaxedSimdDeterministic);

    // Allocation strategy
    if (poolingAllocatorEnabled || allocationStrategy == InstanceAllocationStrategy.POOLING) {
      first = appendJsonField(sb, first, "allocationStrategy", "pooling");
    }

    // Profiling
    if (profilingStrategy != ProfilingStrategy.NONE) {
      first = appendJsonField(sb, first, "profilingStrategy",
          profilingStrategyToString(profilingStrategy));
    }

    // Module version strategy
    if (moduleVersionStrategy != ai.tegmentum.wasmtime4j.config.ModuleVersionStrategy
        .WASMTIME_VERSION) {
      first = appendJsonField(sb, first, "moduleVersionStrategy",
          moduleVersionStrategyToString(moduleVersionStrategy));
      if (moduleVersionCustom != null) {
        first = appendJsonField(sb, first, "moduleVersionCustom", moduleVersionCustom);
      }
    }

    // Target
    if (target != null) {
      first = appendJsonField(sb, first, "target", target);
    }

    // Cache configuration
    if (cacheEnabled) {
      first = appendJsonBool(sb, first, "cacheEnabled", true);
      if (cacheConfigPath != null) {
        first = appendJsonField(sb, first, "cacheConfigPath", cacheConfigPath);
      }
    }

    // Component model (from features set)
    if (wasmFeatures.contains(WasmFeature.COMPONENT_MODEL)) {
      appendJsonBool(sb, first, "wasmComponentModel", true);
    }

    sb.append('}');
    return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static boolean appendJsonBool(
      final StringBuilder sb, final boolean first, final String key, final boolean value) {
    if (!first) {
      sb.append(',');
    }
    sb.append('"').append(key).append("\":").append(value);
    return false;
  }

  private static boolean appendJsonLong(
      final StringBuilder sb, final boolean first, final String key, final long value) {
    if (!first) {
      sb.append(',');
    }
    sb.append('"').append(key).append("\":").append(value);
    return false;
  }

  private static boolean appendJsonField(
      final StringBuilder sb, final boolean first, final String key, final String value) {
    if (!first) {
      sb.append(',');
    }
    sb.append('"').append(key).append("\":\"");
    appendJsonEscaped(sb, value);
    sb.append('"');
    return false;
  }

  private static void appendJsonEscaped(final StringBuilder sb, final String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        default:
          sb.append(c);
          break;
      }
    }
  }

  @SuppressWarnings("deprecation")
  private static String strategyToString(final CompilationStrategy strategy) {
    switch (strategy) {
      case CRANELIFT:
      case PERFORMANCE:
        return "cranelift";
      case WINCH:
        return "winch";
      default:
        return "auto";
    }
  }

  private static String optLevelToString(final OptimizationLevel level) {
    switch (level) {
      case NONE:
        return "none";
      case SIZE:
        return "speed_and_size";
      case SPEED:
      default:
        return "speed";
    }
  }

  private static String profilingStrategyToString(final ProfilingStrategy strategy) {
    switch (strategy) {
      case JIT_DUMP:
        return "jitdump";
      case VTUNE:
        return "vtune";
      case PERF_MAP:
        return "perfmap";
      default:
        return "none";
    }
  }

  private static String moduleVersionStrategyToString(
      final ai.tegmentum.wasmtime4j.config.ModuleVersionStrategy strategy) {
    switch (strategy) {
      case NONE:
        return "none";
      case CUSTOM:
        return "custom";
      default:
        return "wasmtime_version";
    }
  }
}
