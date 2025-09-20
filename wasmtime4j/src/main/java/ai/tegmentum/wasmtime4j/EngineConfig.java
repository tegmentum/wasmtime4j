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
  private long fuelAmount = 0;
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
  private boolean wasiEnabled = false;
  private boolean epochInterruption = false;
  private boolean memoryLimitEnabled = false;
  private long memoryLimit = 0;

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
   * Sets the fuel amount for execution limits when fuel consumption is enabled.
   *
   * @param fuelAmount the amount of fuel to allocate (0 = unlimited)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if fuelAmount is negative
   */
  public EngineConfig fuelAmount(final long fuelAmount) {
    if (fuelAmount < 0) {
      throw new IllegalArgumentException("Fuel amount cannot be negative");
    }
    this.fuelAmount = fuelAmount;
    return this;
  }

  /**
   * Enables or disables WASI (WebAssembly System Interface) support.
   *
   * @param wasiEnabled true to enable WASI support
   * @return this configuration for method chaining
   */
  public EngineConfig wasiEnabled(final boolean wasiEnabled) {
    this.wasiEnabled = wasiEnabled;
    return this;
  }

  /**
   * Enables or disables epoch-based interruption support.
   *
   * @param epochInterruption true to enable epoch interruption
   * @return this configuration for method chaining
   */
  public EngineConfig epochInterruption(final boolean epochInterruption) {
    this.epochInterruption = epochInterruption;
    return this;
  }

  /**
   * Enables or disables memory limit enforcement.
   *
   * @param memoryLimitEnabled true to enable memory limits
   * @return this configuration for method chaining
   */
  public EngineConfig memoryLimitEnabled(final boolean memoryLimitEnabled) {
    this.memoryLimitEnabled = memoryLimitEnabled;
    return this;
  }

  /**
   * Sets the memory limit in bytes when memory limits are enabled.
   *
   * @param memoryLimit the memory limit in bytes (0 = unlimited)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if memoryLimit is negative
   */
  public EngineConfig memoryLimit(final long memoryLimit) {
    if (memoryLimit < 0) {
      throw new IllegalArgumentException("Memory limit cannot be negative");
    }
    this.memoryLimit = memoryLimit;
    return this;
  }

  /**
   * Enables or disables WebAssembly backtrace details.
   *
   * @param wasmBacktraceDetails true to enable backtrace details
   * @return this configuration for method chaining
   */
  public EngineConfig wasmBacktraceDetails(final boolean wasmBacktraceDetails) {
    this.wasmBacktraceDetails = wasmBacktraceDetails;
    return this;
  }

  /**
   * Enables or disables WebAssembly reference types proposal.
   *
   * @param wasmReferenceTypes true to enable reference types
   * @return this configuration for method chaining
   */
  public EngineConfig wasmReferenceTypes(final boolean wasmReferenceTypes) {
    this.wasmReferenceTypes = wasmReferenceTypes;
    return this;
  }

  /**
   * Enables or disables WebAssembly SIMD proposal.
   *
   * @param wasmSimd true to enable SIMD support
   * @return this configuration for method chaining
   */
  public EngineConfig wasmSimd(final boolean wasmSimd) {
    this.wasmSimd = wasmSimd;
    return this;
  }

  /**
   * Enables or disables WebAssembly relaxed SIMD proposal.
   *
   * @param wasmRelaxedSimd true to enable relaxed SIMD support
   * @return this configuration for method chaining
   */
  public EngineConfig wasmRelaxedSimd(final boolean wasmRelaxedSimd) {
    this.wasmRelaxedSimd = wasmRelaxedSimd;
    return this;
  }

  /**
   * Enables or disables WebAssembly multi-value proposal.
   *
   * @param wasmMultiValue true to enable multi-value support
   * @return this configuration for method chaining
   */
  public EngineConfig wasmMultiValue(final boolean wasmMultiValue) {
    this.wasmMultiValue = wasmMultiValue;
    return this;
  }

  /**
   * Enables or disables WebAssembly bulk memory proposal.
   *
   * @param wasmBulkMemory true to enable bulk memory support
   * @return this configuration for method chaining
   */
  public EngineConfig wasmBulkMemory(final boolean wasmBulkMemory) {
    this.wasmBulkMemory = wasmBulkMemory;
    return this;
  }

  /**
   * Enables or disables WebAssembly threads proposal.
   *
   * @param wasmThreads true to enable threads support
   * @return this configuration for method chaining
   */
  public EngineConfig wasmThreads(final boolean wasmThreads) {
    this.wasmThreads = wasmThreads;
    return this;
  }

  /**
   * Enables or disables WebAssembly tail call proposal.
   *
   * @param wasmTailCall true to enable tail call support
   * @return this configuration for method chaining
   */
  public EngineConfig wasmTailCall(final boolean wasmTailCall) {
    this.wasmTailCall = wasmTailCall;
    return this;
  }

  /**
   * Enables or disables WebAssembly multi-memory proposal.
   *
   * @param wasmMultiMemory true to enable multi-memory support
   * @return this configuration for method chaining
   */
  public EngineConfig wasmMultiMemory(final boolean wasmMultiMemory) {
    this.wasmMultiMemory = wasmMultiMemory;
    return this;
  }

  /**
   * Enables or disables WebAssembly 64-bit memory proposal.
   *
   * @param wasmMemory64 true to enable 64-bit memory support
   * @return this configuration for method chaining
   */
  public EngineConfig wasmMemory64(final boolean wasmMemory64) {
    this.wasmMemory64 = wasmMemory64;
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

  /**
   * Gets the fuel amount for execution limits.
   *
   * @return the fuel amount (0 = unlimited)
   */
  public long getFuelAmount() {
    return fuelAmount;
  }

  /**
   * Checks if WASI (WebAssembly System Interface) support is enabled.
   *
   * @return true if WASI is enabled
   */
  public boolean isWasiEnabled() {
    return wasiEnabled;
  }

  /**
   * Checks if epoch-based interruption is enabled.
   *
   * @return true if epoch interruption is enabled
   */
  public boolean isEpochInterruptionEnabled() {
    return epochInterruption;
  }

  /**
   * Checks if memory limit enforcement is enabled.
   *
   * @return true if memory limits are enabled
   */
  public boolean isMemoryLimitEnabled() {
    return memoryLimitEnabled;
  }

  /**
   * Gets the memory limit in bytes.
   *
   * @return the memory limit (0 = unlimited)
   */
  public long getMemoryLimit() {
    return memoryLimit;
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
   * Creates a new configuration optimized for production environments.
   *
   * @return a new configuration optimized for production
   */
  public static EngineConfig forProduction() {
    return new EngineConfig()
        .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
        .parallelCompilation(true)
        .wasmBacktraceDetails(false)
        .debugInfo(false)
        .craneliftDebugVerifier(false);
  }

  /**
   * Creates a new configuration with strict resource limits for security-sensitive environments.
   *
   * @return a new configuration with strict limits
   */
  public static EngineConfig forSecurity() {
    return new EngineConfig()
        .optimizationLevel(OptimizationLevel.SPEED)
        .memoryLimitEnabled(true)
        .memoryLimit(256 * 1024 * 1024) // 256MB limit
        .consumeFuel(true)
        .fuelAmount(1000000) // Limit execution fuel
        .epochInterruption(true)
        .wasmThreads(false) // Disable threads for security
        .wasmTailCall(false) // Disable potentially risky features
        .wasmMultiMemory(false)
        .wasmMemory64(false);
  }

  /**
   * Creates a new configuration optimized for maximum compatibility.
   *
   * @return a new configuration with maximum WebAssembly feature support
   */
  public static EngineConfig forCompatibility() {
    return new EngineConfig()
        .optimizationLevel(OptimizationLevel.SPEED)
        .wasmReferenceTypes(true)
        .wasmSimd(true)
        .wasmRelaxedSimd(true)
        .wasmMultiValue(true)
        .wasmBulkMemory(true)
        .wasmThreads(true)
        .wasmTailCall(true)
        .wasmMultiMemory(true)
        .wasmMemory64(true);
  }

  /**
   * Creates a new configuration with low resource usage for embedded environments.
   *
   * @return a new configuration optimized for low resource usage
   */
  public static EngineConfig forEmbedded() {
    return new EngineConfig()
        .optimizationLevel(OptimizationLevel.SIZE)
        .parallelCompilation(false)
        .debugInfo(false)
        .wasmBacktraceDetails(false)
        .wasmThreads(false)
        .wasmSimd(false)
        .wasmRelaxedSimd(false)
        .wasmTailCall(false)
        .wasmMultiMemory(false)
        .wasmMemory64(false)
        .memoryLimitEnabled(true)
        .memoryLimit(32 * 1024 * 1024); // 32MB limit
  }

  /**
   * Validates this configuration for consistency and compatibility.
   *
   * @throws IllegalStateException if the configuration is invalid
   */
  public void validate() {
    validateOptimizationSettings();
    validateMemorySettings();
    validateFuelSettings();
    validateWasmFeatureCompatibility();
  }

  /** Validates optimization-related settings. */
  private void validateOptimizationSettings() {
    if (optimizationLevel == null) {
      throw new IllegalStateException("Optimization level cannot be null");
    }

    // Debug verifier should only be enabled with debug info for performance
    if (craneliftDebugVerifier && !debugInfo) {
      throw new IllegalStateException("Cranelift debug verifier requires debug info to be enabled");
    }

    // Parallel compilation with debug verifier may cause issues
    if (parallelCompilation && craneliftDebugVerifier) {
      throw new IllegalStateException(
          "Parallel compilation should be disabled when using Cranelift debug verifier");
    }
  }

  /** Validates memory-related settings. */
  private void validateMemorySettings() {
    if (memoryLimitEnabled && memoryLimit <= 0) {
      throw new IllegalStateException(
          "Memory limit must be positive when memory limiting is enabled");
    }

    if (!memoryLimitEnabled && memoryLimit > 0) {
      throw new IllegalStateException("Memory limit is set but memory limiting is not enabled");
    }

    // Memory limit should be reasonable (at least 1MB, at most 16GB)
    if (memoryLimitEnabled) {
      final long minMemory = 1024 * 1024; // 1MB
      final long maxMemory = 16L * 1024 * 1024 * 1024; // 16GB
      if (memoryLimit < minMemory || memoryLimit > maxMemory) {
        throw new IllegalStateException(
            "Memory limit must be between " + minMemory + " and " + maxMemory + " bytes");
      }
    }
  }

  /** Validates fuel-related settings. */
  private void validateFuelSettings() {
    if (consumeFuel && fuelAmount <= 0) {
      throw new IllegalStateException(
          "Fuel amount must be positive when fuel consumption is enabled");
    }

    if (!consumeFuel && fuelAmount > 0) {
      throw new IllegalStateException("Fuel amount is set but fuel consumption is not enabled");
    }
  }

  /** Validates WebAssembly feature compatibility. */
  private void validateWasmFeatureCompatibility() {
    // Relaxed SIMD requires regular SIMD
    if (wasmRelaxedSimd && !wasmSimd) {
      throw new IllegalStateException("Relaxed SIMD requires SIMD to be enabled");
    }

    // Threads require reference types for proper implementation
    if (wasmThreads && !wasmReferenceTypes) {
      throw new IllegalStateException("WebAssembly threads require reference types to be enabled");
    }

    // Memory64 and multi-memory are experimental features that may conflict
    if (wasmMemory64 && wasmMultiMemory) {
      throw new IllegalStateException(
          "Memory64 and multi-memory features may conflict and should not be enabled together");
    }
  }

  /**
   * Checks if this configuration is compatible with another configuration.
   *
   * @param other the other configuration to check compatibility with
   * @return true if configurations are compatible, false otherwise
   */
  public boolean isCompatibleWith(final EngineConfig other) {
    if (other == null) {
      return false;
    }

    // Check WebAssembly feature compatibility
    return isWasmFeaturesCompatible(other) && isRuntimeSettingsCompatible(other);
  }

  /** Checks if WebAssembly features are compatible between configurations. */
  private boolean isWasmFeaturesCompatible(final EngineConfig other) {
    // All features that are enabled in this config must also be enabled in the other
    // This ensures modules compiled with this config can run in engines with the other config
    return (!wasmReferenceTypes || other.wasmReferenceTypes)
        && (!wasmSimd || other.wasmSimd)
        && (!wasmRelaxedSimd || other.wasmRelaxedSimd)
        && (!wasmMultiValue || other.wasmMultiValue)
        && (!wasmBulkMemory || other.wasmBulkMemory)
        && (!wasmThreads || other.wasmThreads)
        && (!wasmTailCall || other.wasmTailCall)
        && (!wasmMultiMemory || other.wasmMultiMemory)
        && (!wasmMemory64 || other.wasmMemory64);
  }

  /** Checks if runtime settings are compatible between configurations. */
  private boolean isRuntimeSettingsCompatible(final EngineConfig other) {
    // Fuel consumption must match for proper execution
    if (consumeFuel != other.consumeFuel) {
      return false;
    }

    // Memory limits must be compatible
    if (memoryLimitEnabled && other.memoryLimitEnabled) {
      return memoryLimit <= other.memoryLimit;
    }

    return true;
  }

  /**
   * Creates a copy of this configuration.
   *
   * @return a new EngineConfig with the same settings
   */
  public EngineConfig copy() {
    final EngineConfig copy = new EngineConfig();
    copy.debugInfo = this.debugInfo;
    copy.consumeFuel = this.consumeFuel;
    copy.fuelAmount = this.fuelAmount;
    copy.optimizationLevel = this.optimizationLevel;
    copy.parallelCompilation = this.parallelCompilation;
    copy.craneliftDebugVerifier = this.craneliftDebugVerifier;
    copy.wasmBacktraceDetails = this.wasmBacktraceDetails;
    copy.wasmReferenceTypes = this.wasmReferenceTypes;
    copy.wasmSimd = this.wasmSimd;
    copy.wasmRelaxedSimd = this.wasmRelaxedSimd;
    copy.wasmMultiValue = this.wasmMultiValue;
    copy.wasmBulkMemory = this.wasmBulkMemory;
    copy.wasmThreads = this.wasmThreads;
    copy.wasmTailCall = this.wasmTailCall;
    copy.wasmMultiMemory = this.wasmMultiMemory;
    copy.wasmMemory64 = this.wasmMemory64;
    copy.wasiEnabled = this.wasiEnabled;
    copy.epochInterruption = this.epochInterruption;
    copy.memoryLimitEnabled = this.memoryLimitEnabled;
    copy.memoryLimit = this.memoryLimit;
    return copy;
  }

  /**
   * Gets a human-readable summary of this configuration.
   *
   * @return configuration summary string
   */
  public String getSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("EngineConfig{");
    sb.append("optimization=").append(optimizationLevel);
    sb.append(", debug=").append(debugInfo);
    sb.append(", parallel=").append(parallelCompilation);

    if (consumeFuel) {
      sb.append(", fuel=").append(fuelAmount);
    }

    if (memoryLimitEnabled) {
      sb.append(", memoryLimit=").append(memoryLimit / (1024 * 1024)).append("MB");
    }

    if (epochInterruption) {
      sb.append(", epochInterruption=true");
    }

    sb.append(", wasmFeatures=[");
    if (wasmReferenceTypes) sb.append("ref-types,");
    if (wasmSimd) sb.append("simd,");
    if (wasmRelaxedSimd) sb.append("relaxed-simd,");
    if (wasmMultiValue) sb.append("multi-value,");
    if (wasmBulkMemory) sb.append("bulk-memory,");
    if (wasmThreads) sb.append("threads,");
    if (wasmTailCall) sb.append("tail-call,");
    if (wasmMultiMemory) sb.append("multi-memory,");
    if (wasmMemory64) sb.append("memory64,");
    sb.append("]}");

    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final EngineConfig that = (EngineConfig) obj;
    return debugInfo == that.debugInfo
        && consumeFuel == that.consumeFuel
        && fuelAmount == that.fuelAmount
        && optimizationLevel == that.optimizationLevel
        && parallelCompilation == that.parallelCompilation
        && craneliftDebugVerifier == that.craneliftDebugVerifier
        && wasmBacktraceDetails == that.wasmBacktraceDetails
        && wasmReferenceTypes == that.wasmReferenceTypes
        && wasmSimd == that.wasmSimd
        && wasmRelaxedSimd == that.wasmRelaxedSimd
        && wasmMultiValue == that.wasmMultiValue
        && wasmBulkMemory == that.wasmBulkMemory
        && wasmThreads == that.wasmThreads
        && wasmTailCall == that.wasmTailCall
        && wasmMultiMemory == that.wasmMultiMemory
        && wasmMemory64 == that.wasmMemory64
        && wasiEnabled == that.wasiEnabled
        && epochInterruption == that.epochInterruption
        && memoryLimitEnabled == that.memoryLimitEnabled
        && memoryLimit == that.memoryLimit;
  }

  @Override
  public int hashCode() {
    int result = Boolean.hashCode(debugInfo);
    result = 31 * result + Boolean.hashCode(consumeFuel);
    result = 31 * result + Long.hashCode(fuelAmount);
    result = 31 * result + optimizationLevel.hashCode();
    result = 31 * result + Boolean.hashCode(parallelCompilation);
    result = 31 * result + Boolean.hashCode(craneliftDebugVerifier);
    result = 31 * result + Boolean.hashCode(wasmBacktraceDetails);
    result = 31 * result + Boolean.hashCode(wasmReferenceTypes);
    result = 31 * result + Boolean.hashCode(wasmSimd);
    result = 31 * result + Boolean.hashCode(wasmRelaxedSimd);
    result = 31 * result + Boolean.hashCode(wasmMultiValue);
    result = 31 * result + Boolean.hashCode(wasmBulkMemory);
    result = 31 * result + Boolean.hashCode(wasmThreads);
    result = 31 * result + Boolean.hashCode(wasmTailCall);
    result = 31 * result + Boolean.hashCode(wasmMultiMemory);
    result = 31 * result + Boolean.hashCode(wasmMemory64);
    result = 31 * result + Boolean.hashCode(wasiEnabled);
    result = 31 * result + Boolean.hashCode(epochInterruption);
    result = 31 * result + Boolean.hashCode(memoryLimitEnabled);
    result = 31 * result + Long.hashCode(memoryLimit);
    return result;
  }

  @Override
  public String toString() {
    return getSummary();
  }
}
