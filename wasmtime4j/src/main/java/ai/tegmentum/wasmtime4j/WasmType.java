package ai.tegmentum.wasmtime4j;

/**
 * Base interface for WebAssembly types.
 * 
 * <p>This interface represents the various types in WebAssembly including
 * function types, memory types, global types, and table types.
 * 
 * @since 1.0.0
 */
public interface WasmType {
    
    /**
     * Gets the kind of this WebAssembly type.
     * 
     * @return the type kind
     */
    WasmTypeKind getKind();
}