package ai.tegmentum.wasmtime4j;

import java.nio.ByteBuffer;

/**
 * Represents WebAssembly linear memory with advanced management capabilities.
 *
 * <p>Linear memory provides a contiguous, mutable array of bytes that can be accessed by
 * WebAssembly code. Memory can be grown dynamically up to its maximum size limit.
 *
 * <p>This interface provides comprehensive memory management features including bulk operations,
 * introspection, and security protection for enterprise-grade applications.
 *
 * @since 1.0.0
 */
public interface WasmMemory extends BulkMemoryOperations, MemoryIntrospection, MemoryProtection {

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

  /**
   * Gets the current size of the memory in bytes.
   *
   * <p>This provides the exact byte size of the memory, calculated as pages * 64KB.
   *
   * @return the current size in bytes
   */
  default long getSizeInBytes() {
    return (long) getSize() * 65536L;
  }

  /**
   * Checks if the memory can be grown by the specified number of pages.
   *
   * <p>This validates whether a grow operation would succeed without actually
   * performing the growth, allowing applications to make informed decisions.
   *
   * @param pages the number of pages to check for growth capacity
   * @return true if growth would succeed, false otherwise
   */
  default boolean canGrow(final int pages) {
    final int maxSize = getMaxSize();
    if (maxSize == -1) {
      return true; // Unlimited growth
    }
    return getSize() + pages <= maxSize;
  }

  /**
   * Gets the remaining growth capacity in pages.
   *
   * <p>This calculates how many more pages can be allocated before reaching
   * the maximum size limit.
   *
   * @return the remaining pages that can be allocated, or -1 if unlimited
   */
  default int getRemainingGrowthCapacity() {
    final int maxSize = getMaxSize();
    if (maxSize == -1) {
      return -1; // Unlimited
    }
    return Math.max(0, maxSize - getSize());
  }

  /**
   * Calculates the memory utilization ratio.
   *
   * <p>This provides insight into how much of the maximum available memory
   * is currently allocated.
   *
   * @return the utilization ratio between 0.0 and 1.0, or -1.0 if unlimited
   */
  default double getUtilizationRatio() {
    final int maxSize = getMaxSize();
    if (maxSize == -1) {
      return -1.0; // Unlimited
    }
    return maxSize == 0 ? 0.0 : (double) getSize() / maxSize;
  }
}
