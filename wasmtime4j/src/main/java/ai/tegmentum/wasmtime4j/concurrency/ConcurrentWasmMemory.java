package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Thread-safe WebAssembly memory interface for concurrent access.
 *
 * <p>A ConcurrentWasmMemory extends the standard WasmMemory interface with explicit thread safety
 * guarantees for all memory operations. Multiple threads can safely read and write memory
 * simultaneously with proper synchronization.
 *
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe memory read and write operations
 *   <li>Atomic memory operations for concurrent access
 *   <li>Asynchronous bulk memory operations
 *   <li>Memory barriers and synchronization primitives
 *   <li>Concurrent memory growth with proper coordination
 * </ul>
 *
 * @since 1.0.0
 */
public interface ConcurrentWasmMemory extends WasmMemory {

  /**
   * Thread-safe atomic read of a single byte from memory.
   *
   * @param offset the memory offset to read from
   * @return the byte value at the specified offset
   * @throws WasmException if the offset is out of bounds or read fails
   */
  byte readByteAtomic(final int offset) throws WasmException;

  /**
   * Thread-safe atomic write of a single byte to memory.
   *
   * @param offset the memory offset to write to
   * @param value the byte value to write
   * @throws WasmException if the offset is out of bounds or write fails
   */
  void writeByteAtomic(final int offset, final byte value) throws WasmException;

  /**
   * Thread-safe atomic read of bytes from memory.
   *
   * @param offset the memory offset to read from
   * @param length the number of bytes to read
   * @return a byte array containing the read data
   * @throws WasmException if the range is out of bounds or read fails
   */
  byte[] readBytesAtomic(final int offset, final int length) throws WasmException;

  /**
   * Thread-safe atomic write of bytes to memory.
   *
   * @param offset the memory offset to write to
   * @param data the byte array to write
   * @throws WasmException if the range is out of bounds or write fails
   */
  void writeBytesAtomic(final int offset, final byte[] data) throws WasmException;

  /**
   * Asynchronously reads bytes from memory.
   *
   * @param offset the memory offset to read from
   * @param length the number of bytes to read
   * @return a CompletableFuture that completes with the read data
   */
  CompletableFuture<byte[]> readBytesAsync(final int offset, final int length);

  /**
   * Asynchronously writes bytes to memory.
   *
   * @param offset the memory offset to write to
   * @param data the byte array to write
   * @return a CompletableFuture that completes when the write is done
   */
  CompletableFuture<Void> writeBytesAsync(final int offset, final byte[] data);

  /**
   * Performs an atomic compare-and-swap operation on a byte.
   *
   * @param offset the memory offset
   * @param expectedValue the expected current value
   * @param newValue the new value to set
   * @return true if the swap was successful, false otherwise
   * @throws WasmException if the offset is out of bounds
   */
  boolean compareAndSwapByte(final int offset, final byte expectedValue, final byte newValue)
      throws WasmException;

  /**
   * Performs an atomic compare-and-swap operation on an integer.
   *
   * @param offset the memory offset (must be 4-byte aligned)
   * @param expectedValue the expected current value
   * @param newValue the new value to set
   * @return true if the swap was successful, false otherwise
   * @throws WasmException if the offset is out of bounds or not aligned
   */
  boolean compareAndSwapInt(final int offset, final int expectedValue, final int newValue)
      throws WasmException;

  /**
   * Performs an atomic compare-and-swap operation on a long.
   *
   * @param offset the memory offset (must be 8-byte aligned)
   * @param expectedValue the expected current value
   * @param newValue the new value to set
   * @return true if the swap was successful, false otherwise
   * @throws WasmException if the offset is out of bounds or not aligned
   */
  boolean compareAndSwapLong(final int offset, final long expectedValue, final long newValue)
      throws WasmException;

  /**
   * Atomically increments a byte value and returns the new value.
   *
   * @param offset the memory offset
   * @param delta the value to add
   * @return the new value after increment
   * @throws WasmException if the offset is out of bounds
   */
  byte getAndAddByte(final int offset, final byte delta) throws WasmException;

  /**
   * Atomically increments an integer value and returns the new value.
   *
   * @param offset the memory offset (must be 4-byte aligned)
   * @param delta the value to add
   * @return the new value after increment
   * @throws WasmException if the offset is out of bounds or not aligned
   */
  int getAndAddInt(final int offset, final int delta) throws WasmException;

  /**
   * Atomically increments a long value and returns the new value.
   *
   * @param offset the memory offset (must be 8-byte aligned)
   * @param delta the value to add
   * @return the new value after increment
   * @throws WasmException if the offset is out of bounds or not aligned
   */
  long getAndAddLong(final int offset, final long delta) throws WasmException;

  /**
   * Executes a memory operation with a read lock held.
   *
   * @param <T> the return type of the operation
   * @param operation the operation to execute under read lock
   * @return the result of the operation
   * @throws WasmException if the operation fails
   */
  <T> T executeWithReadLock(final java.util.function.Supplier<T> operation) throws WasmException;

  /**
   * Executes a memory operation with a write lock held.
   *
   * @param <T> the return type of the operation
   * @param operation the operation to execute under write lock
   * @return the result of the operation
   * @throws WasmException if the operation fails
   */
  <T> T executeWithWriteLock(final java.util.function.Supplier<T> operation) throws WasmException;

  /**
   * Gets the read-write lock used for memory synchronization.
   *
   * @return the ReadWriteLock used for synchronization
   */
  ReadWriteLock getSynchronizationLock();

  /**
   * Creates a thread-safe view of a memory region.
   *
   * @param offset the starting offset of the region
   * @param length the length of the region
   * @return a thread-safe MemoryRegion view
   * @throws WasmException if the region is out of bounds
   */
  MemoryRegion createThreadSafeRegion(final int offset, final int length) throws WasmException;

  /**
   * Performs a bulk memory copy operation atomically.
   *
   * @param sourceOffset the source offset
   * @param destOffset the destination offset
   * @param length the number of bytes to copy
   * @return a CompletableFuture that completes when the copy is done
   */
  CompletableFuture<Void> copyAsync(final int sourceOffset, final int destOffset, final int length);

  /**
   * Performs a bulk memory fill operation atomically.
   *
   * @param offset the starting offset
   * @param value the byte value to fill with
   * @param length the number of bytes to fill
   * @return a CompletableFuture that completes when the fill is done
   */
  CompletableFuture<Void> fillAsync(final int offset, final byte value, final int length);

  /**
   * Gets statistics about concurrent access to this memory.
   *
   * @return detailed concurrency statistics for this memory
   */
  MemoryConcurrencyStatistics getConcurrencyStatistics();

  /**
   * Gets the number of threads currently accessing this memory.
   *
   * @return the number of threads currently reading or writing memory
   */
  int getCurrentAccessorCount();

  /**
   * Validates that the memory is properly configured for concurrent access.
   *
   * @return true if the memory is thread-safe and properly configured
   */
  boolean validateConcurrencyConfiguration();
}