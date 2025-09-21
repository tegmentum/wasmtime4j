package ai.tegmentum.wasmtime4j;

import java.time.Duration;

/**
 * Threads and atomic operations support for WebAssembly shared memory scenarios.
 *
 * <p>This interface provides access to WebAssembly threads and atomic operations that enable shared
 * memory programming patterns. These features are essential for multi-threaded WebAssembly
 * applications that require synchronization primitives and atomic memory operations.
 *
 * <p>All operations include comprehensive validation and defensive programming to ensure thread
 * safety and prevent race conditions or memory corruption.
 *
 * @since 1.0.0
 */
public interface ThreadsAndAtomics {

  /**
   * Checks if thread support is available in the current runtime.
   *
   * @return true if threads are supported, false otherwise
   */
  boolean areThreadsSupported();

  /**
   * Checks if atomic operations are supported in the current runtime.
   *
   * @return true if atomic operations are supported, false otherwise
   */
  boolean areAtomicsSupported();

  /**
   * Creates a shared memory instance that can be accessed by multiple threads.
   *
   * @param type the memory type specification for the shared memory
   * @return a new shared memory instance
   * @throws IllegalArgumentException if type is null
   * @throws RuntimeException if shared memory creation fails or is not supported
   */
  WasmMemory createSharedMemory(final MemoryType type);

  /**
   * Checks if the specified memory instance is shared memory.
   *
   * @param memory the memory instance to check
   * @return true if the memory is shared, false otherwise
   * @throws IllegalArgumentException if memory is null
   */
  boolean isSharedMemory(final WasmMemory memory);

  /**
   * Performs an atomic load operation from shared memory.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param type the atomic operation type
   * @return the loaded value
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic load fails
   */
  long atomicLoad(final WasmMemory memory, final int offset, final AtomicType type);

  /**
   * Performs an atomic store operation to shared memory.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param value the value to store
   * @param type the atomic operation type
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic store fails
   */
  void atomicStore(
      final WasmMemory memory, final int offset, final long value, final AtomicType type);

  /**
   * Performs an atomic read-modify-write add operation.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param value the value to add
   * @param type the atomic operation type
   * @return the previous value before the addition
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic operation fails
   */
  long atomicRmwAdd(
      final WasmMemory memory, final int offset, final long value, final AtomicType type);

  /**
   * Performs an atomic read-modify-write subtract operation.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param value the value to subtract
   * @param type the atomic operation type
   * @return the previous value before the subtraction
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic operation fails
   */
  long atomicRmwSub(
      final WasmMemory memory, final int offset, final long value, final AtomicType type);

  /**
   * Performs an atomic read-modify-write bitwise AND operation.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param value the value to AND with
   * @param type the atomic operation type
   * @return the previous value before the AND operation
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic operation fails
   */
  long atomicRmwAnd(
      final WasmMemory memory, final int offset, final long value, final AtomicType type);

  /**
   * Performs an atomic read-modify-write bitwise OR operation.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param value the value to OR with
   * @param type the atomic operation type
   * @return the previous value before the OR operation
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic operation fails
   */
  long atomicRmwOr(
      final WasmMemory memory, final int offset, final long value, final AtomicType type);

  /**
   * Performs an atomic read-modify-write bitwise XOR operation.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param value the value to XOR with
   * @param type the atomic operation type
   * @return the previous value before the XOR operation
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic operation fails
   */
  long atomicRmwXor(
      final WasmMemory memory, final int offset, final long value, final AtomicType type);

  /**
   * Performs an atomic read-modify-write exchange operation.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param value the value to exchange with
   * @param type the atomic operation type
   * @return the previous value before the exchange
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic operation fails
   */
  long atomicRmwExchange(
      final WasmMemory memory, final int offset, final long value, final AtomicType type);

  /**
   * Performs an atomic compare-and-exchange operation.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param expected the expected current value
   * @param replacement the value to store if the current value equals expected
   * @param type the atomic operation type
   * @return the actual value that was at the memory location
   * @throws IllegalArgumentException if memory or type is null, or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the atomic operation fails
   */
  long atomicRmwCompareExchange(
      final WasmMemory memory,
      final int offset,
      final long expected,
      final long replacement,
      final AtomicType type);

  /**
   * Waits for a 32-bit value at the specified memory location to change or timeout.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param expected the expected value to wait for a change from
   * @param timeout the maximum time to wait, or null for infinite wait
   * @return the wait result indicating why the wait ended
   * @throws IllegalArgumentException if memory is null or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the wait operation fails
   */
  WaitResult memoryAtomicWait32(
      final WasmMemory memory, final int offset, final int expected, final Duration timeout);

  /**
   * Waits for a 64-bit value at the specified memory location to change or timeout.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param expected the expected value to wait for a change from
   * @param timeout the maximum time to wait, or null for infinite wait
   * @return the wait result indicating why the wait ended
   * @throws IllegalArgumentException if memory is null or offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the wait operation fails
   */
  WaitResult memoryAtomicWait64(
      final WasmMemory memory, final int offset, final long expected, final Duration timeout);

  /**
   * Notifies waiting threads that a memory location has changed.
   *
   * @param memory the shared memory instance
   * @param offset the byte offset in memory
   * @param count the maximum number of threads to notify, or -1 for all
   * @return the actual number of threads that were notified
   * @throws IllegalArgumentException if memory is null, offset is negative, or count is invalid
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   * @throws RuntimeException if the notify operation fails
   */
  int memoryAtomicNotify(final WasmMemory memory, final int offset, final int count);

  /**
   * Gets the maximum number of threads supported by the runtime.
   *
   * @return the maximum number of threads, or -1 if unlimited
   */
  int getMaxThreads();

  /**
   * Checks if the current thread can perform atomic operations.
   *
   * @return true if atomic operations are allowed from the current thread, false otherwise
   */
  boolean canPerformAtomicOperations();

  /** Enum representing atomic operation types. */
  enum AtomicType {
    /** 32-bit integer atomic operations. */
    I32,
    /** 64-bit integer atomic operations. */
    I64,
    /** 8-bit integer atomic operations. */
    I8,
    /** 16-bit integer atomic operations. */
    I16
  }

  /** Enum representing wait operation results. */
  enum WaitResult {
    /** The wait completed successfully (value changed). */
    OK,
    /** The wait failed because the value was not equal to expected. */
    NOT_EQUAL,
    /** The wait timed out before the value changed. */
    TIMED_OUT
  }
}
