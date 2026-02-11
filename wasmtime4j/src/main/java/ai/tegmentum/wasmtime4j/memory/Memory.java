package ai.tegmentum.wasmtime4j.memory;

import ai.tegmentum.wasmtime4j.exception.WasmException;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.ByteBuffer;

/**
 * Represents a WebAssembly linear memory instance.
 *
 * <p>WebAssembly linear memory is a contiguous, mutable array of raw bytes that can be read and
 * written by WebAssembly code. This interface provides access to memory operations while enforcing
 * safety and bounds checking.
 *
 * @since 1.0.0
 */
public interface Memory {

  /**
   * Gets the current size of this memory in pages.
   *
   * @return the current memory size in pages (each page is 64KB)
   */
  long getSize();

  /**
   * Gets the current size of this memory in bytes.
   *
   * @return the current memory size in bytes
   */
  long getSizeInBytes();

  /**
   * Grows this memory by the specified number of pages.
   *
   * @param deltaPages the number of pages to grow by
   * @return the previous size in pages, or -1 if the grow operation failed
   * @throws WasmException if the grow operation fails
   */
  long grow(final long deltaPages) throws WasmException;

  /**
   * Reads data from memory into the provided buffer.
   *
   * @param offset the byte offset in memory to start reading from
   * @param buffer the buffer to read data into
   * @return the number of bytes read
   * @throws WasmException if the read operation fails or is out of bounds
   */
  int read(final long offset, final ByteBuffer buffer) throws WasmException;

  /**
   * Writes data from the provided buffer into memory.
   *
   * @param offset the byte offset in memory to start writing to
   * @param buffer the buffer containing data to write
   * @return the number of bytes written
   * @throws WasmException if the write operation fails or is out of bounds
   */
  int write(final long offset, final ByteBuffer buffer) throws WasmException;

  /**
   * Reads a single byte from memory.
   *
   * @param offset the byte offset in memory
   * @return the byte value at the specified offset
   * @throws WasmException if the read operation fails or is out of bounds
   */
  byte readByte(final long offset) throws WasmException;

  /**
   * Writes a single byte to memory.
   *
   * @param offset the byte offset in memory
   * @param value the byte value to write
   * @throws WasmException if the write operation fails or is out of bounds
   */
  void writeByte(final long offset, final byte value) throws WasmException;

  /**
   * Reads a 32-bit integer from memory in little-endian format.
   *
   * @param offset the byte offset in memory
   * @return the integer value at the specified offset
   * @throws WasmException if the read operation fails or is out of bounds
   */
  int readInt32(final long offset) throws WasmException;

  /**
   * Writes a 32-bit integer to memory in little-endian format.
   *
   * @param offset the byte offset in memory
   * @param value the integer value to write
   * @throws WasmException if the write operation fails or is out of bounds
   */
  void writeInt32(final long offset, final int value) throws WasmException;

  /**
   * Reads a 64-bit long from memory in little-endian format.
   *
   * @param offset the byte offset in memory
   * @return the long value at the specified offset
   * @throws WasmException if the read operation fails or is out of bounds
   */
  long readInt64(final long offset) throws WasmException;

  /**
   * Writes a 64-bit long to memory in little-endian format.
   *
   * @param offset the byte offset in memory
   * @param value the long value to write
   * @throws WasmException if the write operation fails or is out of bounds
   */
  void writeInt64(final long offset, final long value) throws WasmException;

  /**
   * Gets the maximum number of pages this memory can grow to.
   *
   * @return the maximum memory size in pages, or -1 if unlimited
   */
  long getMaxSize();

  /**
   * Checks if this memory instance is still valid.
   *
   * @return true if the memory is valid and can be used, false otherwise
   */
  boolean isValid();
}
