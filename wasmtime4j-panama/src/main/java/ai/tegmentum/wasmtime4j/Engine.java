package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.nio.ByteBuffer;

public interface Engine extends AutoCloseable {
    Module compileModule(byte[] wasmBytes) throws CompilationException, WasmException;
    Module compileModule(ByteBuffer wasmBuffer) throws CompilationException, WasmException;
    void close() throws WasmException;
}