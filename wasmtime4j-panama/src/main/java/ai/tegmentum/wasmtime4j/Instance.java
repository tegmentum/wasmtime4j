package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;

public interface Instance extends AutoCloseable {
    Function getFunction(String name) throws WasmException;
    Memory getMemory(String name) throws WasmException;
    Global getGlobal(String name) throws WasmException;
    Table getTable(String name) throws WasmException;
    Object[] invokeFunction(String name, Object... args) throws RuntimeException, WasmException;
    void close() throws WasmException;
}