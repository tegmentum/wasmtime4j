package ai.tegmentum.wasmtime4j.exception;

/**
 * Base class for all WebAssembly-related exceptions.
 * 
 * <p>This is the root exception class for all errors that can occur during
 * WebAssembly operations, including compilation, instantiation, and execution.
 * 
 * @since 1.0.0
 */
public class WasmException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new WebAssembly exception with the specified message.
     * 
     * @param message the error message
     */
    public WasmException(final String message) {
        super(message);
    }
    
    /**
     * Creates a new WebAssembly exception with the specified message and cause.
     * 
     * @param message the error message
     * @param cause the underlying cause
     */
    public WasmException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new WebAssembly exception with the specified cause.
     * 
     * @param cause the underlying cause
     */
    public WasmException(final Throwable cause) {
        super(cause);
    }
}