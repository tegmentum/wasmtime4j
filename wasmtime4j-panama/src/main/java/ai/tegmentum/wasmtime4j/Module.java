package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.util.List;

public interface Module extends AutoCloseable {
    Instance instantiate() throws WasmException;
    Instance instantiate(List<Object> imports) throws WasmException;
    List<String> getImports() throws WasmException;
    List<String> getExports() throws WasmException;
    byte[] serialize() throws WasmException;
    void close() throws WasmException;
}