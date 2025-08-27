package ai.tegmentum.wasmtime4j;

/**
 * Kinds of WebAssembly types.
 * 
 * <p>This enum identifies the different categories of WebAssembly types
 * that can be imported or exported.
 * 
 * @since 1.0.0
 */
public enum WasmTypeKind {
    /**
     * A WebAssembly function type.
     */
    FUNCTION,
    
    /**
     * A WebAssembly global type.
     */
    GLOBAL,
    
    /**
     * A WebAssembly memory type.
     */
    MEMORY,
    
    /**
     * A WebAssembly table type.
     */
    TABLE
}