package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;

public interface Function extends AutoCloseable {
    Object[] invoke(Object... args) throws RuntimeException, WasmException;
    void close() throws WasmException;
}