package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly value types.
 * 
 * <p>These represent the fundamental value types in WebAssembly that can
 * be used as parameters and return values for functions.
 * 
 * @since 1.0.0
 */
public enum WasmValueType {
    /**
     * 32-bit integer type.
     */
    I32,
    
    /**
     * 64-bit integer type.
     */
    I64,
    
    /**
     * 32-bit floating-point type.
     */
    F32,
    
    /**
     * 64-bit floating-point type.
     */
    F64,
    
    /**
     * 128-bit vector type (SIMD).
     */
    V128,
    
    /**
     * Reference to a function.
     */
    FUNCREF,
    
    /**
     * Reference to external data.
     */
    EXTERNREF
}