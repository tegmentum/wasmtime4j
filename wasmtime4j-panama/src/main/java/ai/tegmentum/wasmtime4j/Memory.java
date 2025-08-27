package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.nio.ByteBuffer;

public interface Memory extends AutoCloseable {
    long size() throws WasmException;
    long pages() throws WasmException;
    boolean grow(long pages) throws WasmException;
    byte readByte(long offset) throws WasmException;
    void writeByte(long offset, byte value) throws WasmException;
    void read(long offset, byte[] buffer) throws WasmException;
    void read(long offset, byte[] buffer, int bufferOffset, int length) throws WasmException;
    void write(long offset, byte[] buffer) throws WasmException;
    void write(long offset, byte[] buffer, int bufferOffset, int length) throws WasmException;
    ByteBuffer asByteBuffer() throws WasmException;
    void close() throws WasmException;
}