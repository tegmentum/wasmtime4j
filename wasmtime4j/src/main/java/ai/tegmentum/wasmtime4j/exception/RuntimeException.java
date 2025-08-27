package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown during WebAssembly execution.
 * 
 * <p>This exception is thrown when errors occur during WebAssembly function
 * execution, including traps, out-of-fuel conditions, and other runtime errors.
 * 
 * @since 1.0.0
 */
public class RuntimeException extends WasmException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new runtime exception with the specified message.
     * 
     * @param message the error message describing the runtime failure
     */
    public RuntimeException(final String message) {
        super(message);
    }
    
    /**
     * Creates a new runtime exception with the specified message and cause.
     * 
     * @param message the error message describing the runtime failure
     * @param cause the underlying cause
     */
    public RuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}