package ai.tegmentum.wasmtime4j;

/**
 * Configuration options for WebAssembly engine creation.
 * 
 * <p>This class provides options to customize the behavior of WebAssembly engines,
 * including compilation settings, optimization levels, and runtime features.
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
    private boolean wasmSIMD = true;
    private boolean wasmRelaxedSIMD = false;
    private boolean wasmMultiValue = true;
    private boolean wasmBulkMemory = true;
    private boolean wasmThreads = false;
    private boolean wasmTailCall = false;
    private boolean wasmMultiMemory = false;
    private boolean wasmMemory64 = false;
    
    /**
     * Creates a new engine configuration with default settings.
     */
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
    
    public boolean isWasmSIMD() {
        return wasmSIMD;
    }
    
    public boolean isWasmRelaxedSIMD() {
        return wasmRelaxedSIMD;
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
     * Creates a new configuration with default settings optimized for speed.
     * 
     * @return a new configuration optimized for speed
     */
    public static EngineConfig forSpeed() {
        return new EngineConfig()
                .optimizationLevel(OptimizationLevel.SPEED)
                .parallelCompilation(true);
    }
    
    /**
     * Creates a new configuration with default settings optimized for size.
     * 
     * @return a new configuration optimized for size
     */
    public static EngineConfig forSize() {
        return new EngineConfig()
                .optimizationLevel(OptimizationLevel.SIZE)
                .parallelCompilation(true);
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