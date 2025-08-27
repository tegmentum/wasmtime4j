package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WebAssembly validation fails.
 * 
 * <p>This exception is thrown when WebAssembly modules fail validation,
 * such as type mismatches, invalid bytecode, or structural issues.
 * 
 * @since 1.0.0
 */
public class ValidationException extends WasmException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new validation exception with the specified message.
     * 
     * @param message the error message describing the validation failure
     */
    public ValidationException(final String message) {
        super(message);
    }
    
    /**
     * Creates a new validation exception with the specified message and cause.
     * 
     * @param message the error message describing the validation failure
     * @param cause the underlying cause
     */
    public ValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}