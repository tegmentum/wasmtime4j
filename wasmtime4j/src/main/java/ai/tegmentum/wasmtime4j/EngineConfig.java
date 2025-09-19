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
}
