package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WebAssembly compilation fails.
 * 
 * <p>This exception is thrown when WebAssembly bytecode cannot be compiled
 * due to validation errors, unsupported features, or other compilation issues.
 * 
 * @since 1.0.0
 */
public class CompilationException extends WasmException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new compilation exception with the specified message.
     * 
     * @param message the error message describing the compilation failure
     */
    public CompilationException(final String message) {
        super(message);
    }
    
    /**
     * Creates a new compilation exception with the specified message and cause.
     * 
     * @param message the error message describing the compilation failure
     * @param cause the underlying cause
     */
    public CompilationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}