package ai.tegmentum.wasmtime4j.exception;

public class WasmException extends Exception {
    public WasmException(String message) {
        super(message);
    }
    
    public WasmException(String message, Throwable cause) {
        super(message, cause);
    }
}