package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import java.nio.ByteBuffer;

/**
 * Represents WebAssembly linear memory.
 *
 * <p>Linear memory provides a contiguous, mutable array of bytes that can be accessed by
 * WebAssembly code. Memory can be grown dynamically up to its maximum size limit.
 *
 * @since 1.0.0
 */
public interface WasmMemory {

  /**
   * Gets the current size of the memory in pages (64KB each).
   *
   * @return the current size in pages
   */
  int getSize();

  /**
   * Gets the current size of the memory in pages (64KB each). Alias for getSize() for
   * compatibility.
   *
   * @return the current size in pages
   */
  default int size() {
    return getSize();
  }

  /**
   * Grows the memory by the specified number of pages.
   *
   * @param pages the number of pages to grow by
   * @return the previous size in pages, or -1 if growth failed
   */
  int grow(final int pages);

  /**
   * Gets the maximum size of the memory in pages.
   *
   * @return the maximum size in pages, or -1 if unlimited
   */
  int getMaxSize();

  /**
   * Gets the complete type information for this memory.
   *
   * <p>The MemoryType provides full type information including minimum and maximum page counts,
   * 64-bit addressing support, and shared memory status. This provides comprehensive metadata about
   * the memory's configuration and limits.
   *
   * @return the complete memory type information
   * @since 1.0.0
   */
  MemoryType getMemoryType();

  /**
   * Gets a read-only view of the memory as a ByteBuffer.
   *
   * <p>The returned buffer provides direct access to the WebAssembly memory. Care must be taken
   * when using this buffer as it may become invalid if the memory is grown.
   *
   * @return a ByteBuffer view of the memory
   */
  ByteBuffer getBuffer();

  /**
   * Reads a byte from the memory at the given offset.
   *
   * @param offset the byte offset
   * @return the byte value
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  byte readByte(final int offset);

  /**
   * Writes a byte to the memory at the given offset.
   *
   * @param offset the byte offset
   * @param value the byte value to write
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  void writeByte(final int offset, final byte value);

  /**
   * Reads bytes from the memory into the given array.
   *
   * @param offset the starting offset in memory
   * @param dest the destination array
   * @param destOffset the starting offset in the destination array
   * @param length the number of bytes to read
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   */
  void readBytes(final int offset, final byte[] dest, final int destOffset, final int length);

  /**
   * Writes bytes from the given array to memory.
   *
   * @param offset the starting offset in memory
   * @param src the source array
   * @param srcOffset the starting offset in the source array
   * @param length the number of bytes to write
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   */
  void writeBytes(final int offset, final byte[] src, final int srcOffset, final int length);

  // Bulk Memory Operations

  /**
   * Copies memory from one region to another within the same memory instance.
   *
   * <p>This operation supports overlapping memory regions and will handle them correctly. It is
   * equivalent to the WebAssembly memory.copy instruction.
   *
   * @param destOffset the destination offset in memory
   * @param srcOffset the source offset in memory
   * @param length the number of bytes to copy
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   */
  void copy(final int destOffset, final int srcOffset, final int length);

  /**
   * Copies memory from one region to another with long addressing.
   *
   * @param destOffset the destination offset in memory
   * @param srcOffset the source offset in memory
   * @param length the number of bytes to copy
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   * @since 1.1.0
   */
  default void copy(final long destOffset, final long srcOffset, final long length) {
    // Handle overlapping regions correctly
    if (destOffset == srcOffset || length == 0) {
      return; // Nothing to copy
    }

    final int chunkSize = 1024 * 1024; // 1MB chunks
    final byte[] copyBuffer = new byte[chunkSize];

    if (destOffset < srcOffset) {
      // Copy forward
      long remaining = length;
      long currentSrc = srcOffset;
      long currentDest = destOffset;

      while (remaining > 0) {
        final int currentChunkSize = (int) Math.min(remaining, chunkSize);
        readBytes64(currentSrc, copyBuffer, 0, currentChunkSize);
        writeBytes64(currentDest, copyBuffer, 0, currentChunkSize);
        remaining -= currentChunkSize;
        currentSrc += currentChunkSize;
        currentDest += currentChunkSize;
      }
    } else {
      // Copy backward to handle overlap
      long remaining = length;
      long currentSrc = srcOffset + length;
      long currentDest = destOffset + length;

      while (remaining > 0) {
        final int currentChunkSize = (int) Math.min(remaining, chunkSize);
        currentSrc -= currentChunkSize;
        currentDest -= currentChunkSize;
        readBytes64(currentSrc, copyBuffer, 0, currentChunkSize);
        writeBytes64(currentDest, copyBuffer, 0, currentChunkSize);
        remaining -= currentChunkSize;
      }
    }
  }

  /**
   * Fills a memory region with the specified byte value.
   *
   * <p>This is equivalent to the WebAssembly memory.fill instruction.
   *
   * @param offset the starting offset in memory
   * @param value the byte value to fill with
   * @param length the number of bytes to fill
   * @throws IndexOutOfBoundsException if offset or length is out of bounds
   */
  void fill(final int offset, final byte value, final int length);

  /**
   * Fills a memory region with the specified byte value using long addressing.
   *
   * @param offset the starting offset in memory
   * @param value the byte value to fill with
   * @param length the number of bytes to fill
   * @throws IndexOutOfBoundsException if offset or length is out of bounds
   * @since 1.1.0
   */
  default void fill(final long offset, final byte value, final long length) {
    // Use chunked filling for efficiency with large regions
    final int chunkSize = 1024 * 1024; // 1MB chunks
    final byte[] fillBuffer = new byte[chunkSize];
    java.util.Arrays.fill(fillBuffer, value);

    long remaining = length;
    long currentOffset = offset;

    while (remaining > 0) {
      final int currentChunkSize = (int) Math.min(remaining, chunkSize);
      writeBytes64(currentOffset, fillBuffer, 0, currentChunkSize);
      remaining -= currentChunkSize;
      currentOffset += currentChunkSize;
    }
  }

  /**
   * Initializes memory from a data segment.
   *
   * <p>This is equivalent to the WebAssembly memory.init instruction. The data segment must be
   * available and not dropped.
   *
   * @param destOffset the destination offset in memory
   * @param dataSegmentIndex the index of the data segment
   * @param srcOffset the source offset within the data segment
   * @param length the number of bytes to copy
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   * @throws IllegalStateException if the data segment has been dropped
   */
  void init(
      final int destOffset, final int dataSegmentIndex, final int srcOffset, final int length);

  /**
   * Drops a data segment, making it unavailable for memory.init operations.
   *
   * <p>This is equivalent to the WebAssembly data.drop instruction. After dropping, the segment
   * cannot be used for memory.init operations.
   *
   * @param dataSegmentIndex the index of the data segment to drop
   * @throws IllegalArgumentException if the segment index is invalid
   * @throws IllegalStateException if the segment has already been dropped
   */
  void dropDataSegment(final int dataSegmentIndex);

  // Shared Memory Operations

  /**
   * Checks if this memory is shared between threads.
   *
   * @return true if this memory is shared, false if private
   */
  boolean isShared();

  /**
   * Performs an atomic compare-and-swap operation on a 32-bit value.
   *
   * <p>This operation atomically compares the value at the specified offset with the expected
   * value, and if they match, replaces it with the new value. This is only supported for shared
   * memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @param expected the expected current value
   * @param newValue the new value to set if expected matches current
   * @return the original value at the offset before the operation
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicCompareAndSwapInt(final int offset, final int expected, final int newValue);

  /**
   * Performs an atomic compare-and-swap operation on a 64-bit value.
   *
   * <p>This operation atomically compares the value at the specified offset with the expected
   * value, and if they match, replaces it with the new value. This is only supported for shared
   * memory.
   *
   * @param offset the byte offset (must be 8-byte aligned)
   * @param expected the expected current value
   * @param newValue the new value to set if expected matches current
   * @return the original value at the offset before the operation
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 8-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  long atomicCompareAndSwapLong(final int offset, final long expected, final long newValue);

  /**
   * Performs an atomic load operation on a 32-bit value.
   *
   * <p>This operation atomically loads a value from shared memory with acquire semantics. This is
   * only supported for shared memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @return the loaded value
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicLoadInt(final int offset);

  /**
   * Performs an atomic load operation on a 64-bit value.
   *
   * <p>This operation atomically loads a value from shared memory with acquire semantics. This is
   * only supported for shared memory.
   *
   * @param offset the byte offset (must be 8-byte aligned)
   * @return the loaded value
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 8-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  long atomicLoadLong(final int offset);

  /**
   * Performs an atomic store operation on a 32-bit value.
   *
   * <p>This operation atomically stores a value to shared memory with release semantics. This is
   * only supported for shared memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @param value the value to store
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  void atomicStoreInt(final int offset, final int value);

  /**
   * Performs an atomic store operation on a 64-bit value.
   *
   * <p>This operation atomically stores a value to shared memory with release semantics. This is
   * only supported for shared memory.
   *
   * @param offset the byte offset (must be 8-byte aligned)
   * @param value the value to store
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 8-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  void atomicStoreLong(final int offset, final long value);

  /**
   * Performs an atomic add operation on a 32-bit value.
   *
   * <p>This operation atomically adds the specified value to the value at the given offset and
   * returns the original value. This is only supported for shared memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @param value the value to add
   * @return the original value before the addition
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicAddInt(final int offset, final int value);

  /**
   * Performs an atomic add operation on a 64-bit value.
   *
   * <p>This operation atomically adds the specified value to the value at the given offset and
   * returns the original value. This is only supported for shared memory.
   *
   * @param offset the byte offset (must be 8-byte aligned)
   * @param value the value to add
   * @return the original value before the addition
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 8-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  long atomicAddLong(final int offset, final long value);

  /**
   * Performs an atomic bitwise AND operation on a 32-bit value.
   *
   * <p>This operation atomically performs a bitwise AND operation between the value at the offset
   * and the specified value, storing the result and returning the original value. This is only
   * supported for shared memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @param value the value to AND with
   * @return the original value before the operation
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicAndInt(final int offset, final int value);

  /**
   * Performs an atomic bitwise OR operation on a 32-bit value.
   *
   * <p>This operation atomically performs a bitwise OR operation between the value at the offset
   * and the specified value, storing the result and returning the original value. This is only
   * supported for shared memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @param value the value to OR with
   * @return the original value before the operation
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicOrInt(final int offset, final int value);

  /**
   * Performs an atomic bitwise XOR operation on a 32-bit value.
   *
   * <p>This operation atomically performs a bitwise XOR operation between the value at the offset
   * and the specified value, storing the result and returning the original value. This is only
   * supported for shared memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @param value the value to XOR with
   * @return the original value before the operation
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicXorInt(final int offset, final int value);

  /**
   * Performs an atomic memory fence operation.
   *
   * <p>This operation ensures that all memory operations before the fence are visible to other
   * threads before any memory operations after the fence. This is only supported for shared memory
   * and is equivalent to the WebAssembly memory.atomic.fence instruction.
   *
   * @throws IllegalStateException if this memory is not shared
   */
  void atomicFence();

  /**
   * Notifies threads waiting on the specified memory location.
   *
   * <p>This operation is part of the WebAssembly threads proposal and wakes up threads that are
   * waiting on the memory location using atomicWait. This is only supported for shared memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @param count the number of threads to wake up (0 means wake all)
   * @return the number of threads actually woken up
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicNotify(final int offset, final int count);

  /**
   * Waits for a notification on the specified memory location.
   *
   * <p>This operation is part of the WebAssembly threads proposal and causes the current thread to
   * wait until either the value at the memory location changes from the expected value or a timeout
   * occurs. This is only supported for shared memory.
   *
   * @param offset the byte offset (must be 4-byte aligned)
   * @param expected the expected value to wait for a change from
   * @param timeoutNanos the timeout in nanoseconds (-1 for infinite)
   * @return 0 if woken by notify, 1 if value mismatch, 2 if timeout
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 4-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicWait32(final int offset, final int expected, final long timeoutNanos);

  /**
   * Waits for a notification on the specified memory location (64-bit version).
   *
   * <p>This operation is part of the WebAssembly threads proposal and causes the current thread to
   * wait until either the value at the memory location changes from the expected value or a timeout
   * occurs. This is only supported for shared memory.
   *
   * @param offset the byte offset (must be 8-byte aligned)
   * @param expected the expected value to wait for a change from
   * @param timeoutNanos the timeout in nanoseconds (-1 for infinite)
   * @return 0 if woken by notify, 1 if value mismatch, 2 if timeout
   * @throws IllegalStateException if this memory is not shared
   * @throws IllegalArgumentException if offset is not 8-byte aligned
   * @throws IndexOutOfBoundsException if offset is out of bounds
   */
  int atomicWait64(final int offset, final long expected, final long timeoutNanos);

  // 64-bit Memory Operations for Large Memory Support (>4GB)

  /**
   * Gets the current size of the memory in pages (64KB each) using 64-bit addressing.
   *
   * <p>This method enables access to memories larger than 4GB by using long-based page counts
   * instead of the 32-bit limited getSize() method.
   *
   * @return the current size in pages as a long value
   * @since 1.1.0
   */
  default long getSize64() {
    if (supports64BitAddressing()) {
      // Implementation should override this for true 64-bit support
      return getSize();
    }
    return getSize();
  }

  /**
   * Grows the memory by the specified number of pages using 64-bit addressing.
   *
   * <p>This method enables growth operations for memories larger than 4GB by supporting page counts
   * that exceed the 32-bit integer limit.
   *
   * @param pages the number of pages to grow by
   * @return the previous size in pages, or -1 if growth failed
   * @since 1.1.0
   */
  default long grow64(final long pages) {
    if (!supports64BitAddressing()) {
      if (pages > Integer.MAX_VALUE) {
        return -1; // Cannot grow by more than Integer.MAX_VALUE pages with 32-bit interface
      }
      return grow((int) pages);
    }
    // Implementation should override this for true 64-bit support
    if (pages > Integer.MAX_VALUE) {
      return -1; // Fallback for compatibility
    }
    return grow((int) pages);
  }

  /**
   * Gets the maximum size of the memory in pages using 64-bit addressing.
   *
   * <p>This method enables access to maximum size limits larger than 4GB by using long-based page
   * counts instead of the 32-bit limited getMaxSize() method.
   *
   * @return the maximum size in pages, or -1 if unlimited
   * @since 1.1.0
   */
  default long getMaxSize64() {
    return getMaxSize();
  }

  /**
   * Reads a byte from the memory at the given offset using 64-bit addressing.
   *
   * <p>This method enables reading from memory addresses beyond the 32-bit limit, supporting
   * memories larger than 4GB.
   *
   * @param offset the byte offset (64-bit)
   * @return the byte value
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default byte readByte64(final long offset) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }
    if (offset > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("Offset exceeds 32-bit memory addressing: " + offset);
    }
    return readByte((int) offset);
  }

  /**
   * Writes a byte to the memory at the given offset using 64-bit addressing.
   *
   * <p>This method enables writing to memory addresses beyond the 32-bit limit, supporting memories
   * larger than 4GB.
   *
   * @param offset the byte offset (64-bit)
   * @param value the byte value to write
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default void writeByte64(final long offset, final byte value) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }
    if (offset > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("Offset exceeds 32-bit memory addressing: " + offset);
    }
    writeByte((int) offset, value);
  }

  /**
   * Reads bytes from the memory into the given array using 64-bit addressing.
   *
   * <p>This method enables reading from memory addresses beyond the 32-bit limit, supporting
   * memories larger than 4GB.
   *
   * @param offset the starting offset in memory (64-bit)
   * @param dest the destination array
   * @param destOffset the starting offset in the destination array
   * @param length the number of bytes to read
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   * @since 1.1.0
   */
  default void readBytes64(
      final long offset, final byte[] dest, final int destOffset, final int length) {
    if (offset > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("Offset exceeds 32-bit memory addressing: " + offset);
    }
    readBytes((int) offset, dest, destOffset, length);
  }

  /**
   * Writes bytes from the given array to memory using 64-bit addressing.
   *
   * <p>This method enables writing to memory addresses beyond the 32-bit limit, supporting memories
   * larger than 4GB.
   *
   * @param offset the starting offset in memory (64-bit)
   * @param src the source array
   * @param srcOffset the starting offset in the source array
   * @param length the number of bytes to write
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   * @since 1.1.0
   */
  default void writeBytes64(
      final long offset, final byte[] src, final int srcOffset, final int length) {
    if (offset > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("Offset exceeds 32-bit memory addressing: " + offset);
    }
    writeBytes((int) offset, src, srcOffset, length);
  }

  /**
   * Copies memory from one region to another within the same memory instance using 64-bit
   * addressing.
   *
   * <p>This operation supports overlapping memory regions and will handle them correctly. It is
   * equivalent to the WebAssembly memory.copy instruction with 64-bit offsets for large memory
   * support.
   *
   * @param destOffset the destination offset in memory (64-bit)
   * @param srcOffset the source offset in memory (64-bit)
   * @param length the number of bytes to copy
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   * @since 1.1.0
   */
  default void copy64(final long destOffset, final long srcOffset, final long length) {
    if (destOffset > Integer.MAX_VALUE
        || srcOffset > Integer.MAX_VALUE
        || length > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException(
          "64-bit memory operations not supported by this implementation");
    }
    copy((int) destOffset, (int) srcOffset, (int) length);
  }

  /**
   * Fills a memory region with the specified byte value using 64-bit addressing.
   *
   * <p>This is equivalent to the WebAssembly memory.fill instruction with 64-bit offsets for large
   * memory support.
   *
   * @param offset the starting offset in memory (64-bit)
   * @param value the byte value to fill with
   * @param length the number of bytes to fill
   * @throws IndexOutOfBoundsException if offset or length is out of bounds
   * @since 1.1.0
   */
  default void fill64(final long offset, final byte value, final long length) {
    if (offset > Integer.MAX_VALUE || length > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException(
          "64-bit memory operations not supported by this implementation");
    }
    fill((int) offset, value, (int) length);
  }

  /**
   * Initializes memory from a data segment using 64-bit addressing.
   *
   * <p>This is equivalent to the WebAssembly memory.init instruction with 64-bit offsets for large
   * memory support.
   *
   * @param destOffset the destination offset in memory (64-bit)
   * @param dataSegmentIndex the index of the data segment
   * @param srcOffset the source offset within the data segment
   * @param length the number of bytes to copy
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   * @throws IllegalStateException if the data segment has been dropped
   * @since 1.1.0
   */
  default void init64(
      final long destOffset, final int dataSegmentIndex, final long srcOffset, final long length) {
    if (destOffset > Integer.MAX_VALUE
        || srcOffset > Integer.MAX_VALUE
        || length > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException(
          "64-bit memory operations not supported by this implementation");
    }
    init((int) destOffset, dataSegmentIndex, (int) srcOffset, (int) length);
  }

  /**
   * Checks if this memory supports 64-bit addressing.
   *
   * <p>Returns true if this memory implementation can handle addresses and operations beyond the
   * 32-bit integer limit (>4GB memory spaces).
   *
   * @return true if 64-bit addressing is supported, false if limited to 32-bit
   * @since 1.1.0
   */
  default boolean supports64BitAddressing() {
    return false;
  }

  /**
   * Gets the current size of the memory in bytes using 64-bit addressing.
   *
   * <p>This method provides the memory size in bytes rather than pages, enabling precise size
   * calculations for memories larger than 4GB.
   *
   * @return the current size in bytes as a long value
   * @since 1.1.0
   */
  default long getSizeInBytes64() {
    return getSize64() * 65536L; // 64KB per page
  }

  /**
   * Gets the maximum size of the memory in bytes using 64-bit addressing.
   *
   * <p>This method provides the maximum memory size in bytes rather than pages, enabling precise
   * limit calculations for memories larger than 4GB.
   *
   * @return the maximum size in bytes, or -1 if unlimited
   * @since 1.1.0
   */
  default long getMaxSizeInBytes64() {
    final long maxPages = getMaxSize64();
    return maxPages == -1 ? -1 : maxPages * 65536L; // 64KB per page
  }

  // Multi-Memory Proposal Support

  /**
   * Reads a 32-bit integer from memory at the given offset.
   *
   * @param offset the byte offset
   * @return the integer value
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default int readInt32(final long offset) {
    final byte[] bytes = new byte[4];
    readBytes64(offset, bytes, 0, 4);
    return (bytes[0] & 0xFF)
        | ((bytes[1] & 0xFF) << 8)
        | ((bytes[2] & 0xFF) << 16)
        | ((bytes[3] & 0xFF) << 24);
  }

  /**
   * Reads a 64-bit integer from memory at the given offset.
   *
   * @param offset the byte offset
   * @return the long value
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default long readInt64(final long offset) {
    final byte[] bytes = new byte[8];
    readBytes64(offset, bytes, 0, 8);
    return (bytes[0] & 0xFFL)
        | ((bytes[1] & 0xFFL) << 8)
        | ((bytes[2] & 0xFFL) << 16)
        | ((bytes[3] & 0xFFL) << 24)
        | ((bytes[4] & 0xFFL) << 32)
        | ((bytes[5] & 0xFFL) << 40)
        | ((bytes[6] & 0xFFL) << 48)
        | ((bytes[7] & 0xFFL) << 56);
  }

  /**
   * Reads a 32-bit float from memory at the given offset.
   *
   * @param offset the byte offset
   * @return the float value
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default float readFloat32(final long offset) {
    return Float.intBitsToFloat(readInt32(offset));
  }

  /**
   * Reads a 64-bit double from memory at the given offset.
   *
   * @param offset the byte offset
   * @return the double value
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default double readFloat64(final long offset) {
    return Double.longBitsToDouble(readInt64(offset));
  }

  /**
   * Writes a 32-bit integer to memory at the given offset.
   *
   * @param offset the byte offset
   * @param value the integer value to write
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default void writeInt32(final long offset, final int value) {
    final byte[] bytes = new byte[4];
    bytes[0] = (byte) (value & 0xFF);
    bytes[1] = (byte) ((value >> 8) & 0xFF);
    bytes[2] = (byte) ((value >> 16) & 0xFF);
    bytes[3] = (byte) ((value >> 24) & 0xFF);
    writeBytes64(offset, bytes, 0, 4);
  }

  /**
   * Writes a 64-bit integer to memory at the given offset.
   *
   * @param offset the byte offset
   * @param value the long value to write
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default void writeInt64(final long offset, final long value) {
    final byte[] bytes = new byte[8];
    bytes[0] = (byte) (value & 0xFF);
    bytes[1] = (byte) ((value >> 8) & 0xFF);
    bytes[2] = (byte) ((value >> 16) & 0xFF);
    bytes[3] = (byte) ((value >> 24) & 0xFF);
    bytes[4] = (byte) ((value >> 32) & 0xFF);
    bytes[5] = (byte) ((value >> 40) & 0xFF);
    bytes[6] = (byte) ((value >> 48) & 0xFF);
    bytes[7] = (byte) ((value >> 56) & 0xFF);
    writeBytes64(offset, bytes, 0, 8);
  }

  /**
   * Writes a 32-bit float to memory at the given offset.
   *
   * @param offset the byte offset
   * @param value the float value to write
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default void writeFloat32(final long offset, final float value) {
    writeInt32(offset, Float.floatToIntBits(value));
  }

  /**
   * Writes a 64-bit double to memory at the given offset.
   *
   * @param offset the byte offset
   * @param value the double value to write
   * @throws IndexOutOfBoundsException if offset is out of bounds
   * @since 1.1.0
   */
  default void writeFloat64(final long offset, final double value) {
    writeInt64(offset, Double.doubleToLongBits(value));
  }

  // Direct memory access methods (wasmtime 39.0.1 API)

  /**
   * Gets the base pointer address for direct memory access.
   *
   * <p>This returns the native memory address of the linear memory's data buffer. This pointer can
   * be used for direct memory access via unsafe operations or JNI/Panama FFI.
   *
   * <p><b>Warning:</b> The returned pointer may be invalidated by memory growth operations. Always
   * re-fetch the pointer after any operation that may grow memory.
   *
   * @return the native memory address, or 0 if not available
   * @since 1.1.0
   */
  default long dataPtr() {
    return 0;
  }

  /**
   * Gets the current size of the memory in bytes.
   *
   * <p>This is equivalent to {@code getSize() * 65536L} but may be more efficient as it can be
   * calculated directly from the native memory state.
   *
   * @return the current size in bytes
   * @since 1.1.0
   */
  default long dataSize() {
    return (long) getSize() * 65536L;
  }

  /**
   * Gets the page size for this memory in bytes.
   *
   * <p>Standard WebAssembly page size is 64KB (65536 bytes). Custom page sizes are available via
   * the custom-page-sizes proposal.
   *
   * @return the page size in bytes (typically 65536)
   * @since 1.1.0
   */
  default int pageSize() {
    return 65536;
  }

  /**
   * Gets the log2 of the page size for this memory.
   *
   * <p>For standard 64KB pages, this returns 16. This is useful for efficient page calculations
   * using bit shifts instead of division.
   *
   * @return the log2 of page size (typically 16)
   * @since 1.1.0
   */
  default int pageSizeLog2() {
    return 16;
  }

  /**
   * Gets the minimum number of pages for this memory.
   *
   * <p>This is the minimum size that the memory was created with and cannot shrink below.
   *
   * @return the minimum number of pages
   * @since 1.1.0
   */
  default int getMinSize() {
    MemoryType memType = getMemoryType();
    return memType != null ? (int) memType.getMinimum() : 0;
  }

  /**
   * Grows memory asynchronously by the specified number of pages.
   *
   * <p>This method requires the engine to be configured with {@code asyncSupport(true)}.
   * It goes through the async resource limiter if one is set on the store, allowing
   * the limiter to asynchronously decide whether to permit the growth.
   *
   * @param pages the number of additional pages to allocate
   * @return the previous number of pages before growth
   * @throws WasmException if growth fails or async support is not enabled
   * @since 1.1.0
   */
  long growAsync(long pages) throws WasmException;
}
