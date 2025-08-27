package ai.tegmentum.wasmtime4j.exception;

public class CompilationException extends WasmException {
    public CompilationException(String message) {
        super(message);
    }
    
    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}