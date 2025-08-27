package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

public interface Table extends AutoCloseable {
    long size() throws WasmException;
    Object get(long index) throws WasmException;
    void set(long index, Object value) throws WasmException;
    void close() throws WasmException;
}