package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

public interface Global extends AutoCloseable {
    Object getValue() throws WasmException;
    void setValue(Object value) throws WasmException;
    boolean isMutable() throws WasmException;
    void close() throws WasmException;
}