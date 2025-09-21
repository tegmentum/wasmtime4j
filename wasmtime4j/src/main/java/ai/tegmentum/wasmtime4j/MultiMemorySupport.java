package ai.tegmentum.wasmtime4j;

import java.util.List;

/**
 * Multi-memory support for WebAssembly modules with multiple memory instances.
 *
 * <p>This interface provides access to advanced WebAssembly multi-memory features that allow
 * modules to define and use multiple linear memory instances. This capability is essential for
 * complex applications requiring memory isolation, specialized memory regions, or performance
 * optimization through memory partitioning.
 *
 * <p>All operations include comprehensive bounds checking and defensive programming to prevent JVM
 * crashes and ensure memory safety across multiple memory instances.
 *
 * @since 1.0.0
 */
public interface MultiMemorySupport {

  /**
   * Gets all memory instances associated with the current module.
   *
   * @return an immutable list of all memory instances, never null
   * @throws RuntimeException if unable to retrieve memory instances
   */
  List<WasmMemory> getMemories();

  /**
   * Gets a specific memory instance by its index.
   *
   * @param index the memory index (0-based)
   * @return the memory instance at the specified index
   * @throws IllegalArgumentException if index is negative
   * @throws IndexOutOfBoundsException if index exceeds available memories
   * @throws RuntimeException if unable to retrieve the memory instance
   */
  WasmMemory getMemory(final int index);

  /**
   * Creates an additional memory instance with the specified type.
   *
   * @param type the memory type specification for the new memory
   * @return the newly created memory instance
   * @throws IllegalArgumentException if type is null
   * @throws RuntimeException if memory creation fails or is not supported
   */
  WasmMemory createAdditionalMemory(final MemoryType type);

  /**
   * Copies data between two different memory instances.
   *
   * <p>This operation provides safe cross-memory copying with comprehensive bounds checking to
   * ensure both source and destination memory regions are valid.
   *
   * @param source the source memory instance
   * @param sourceOffset the offset in the source memory
   * @param dest the destination memory instance
   * @param destOffset the offset in the destination memory
   * @param length the number of bytes to copy
   * @throws IllegalArgumentException if any memory is null
   * @throws IndexOutOfBoundsException if any offset or length is invalid
   * @throws RuntimeException if the copy operation fails
   */
  void copyBetweenMemories(
      final WasmMemory source,
      final int sourceOffset,
      final WasmMemory dest,
      final int destOffset,
      final int length);

  /**
   * Fills a memory region with the specified byte value.
   *
   * @param memory the target memory instance
   * @param offset the starting offset in memory
   * @param length the number of bytes to fill
   * @param value the byte value to fill with
   * @throws IllegalArgumentException if memory is null
   * @throws IndexOutOfBoundsException if offset or length is invalid
   * @throws RuntimeException if the fill operation fails
   */
  void fillMemory(final WasmMemory memory, final int offset, final int length, final byte value);

  /**
   * Attempts to grow the specified memory by the given number of pages.
   *
   * @param memory the memory instance to grow
   * @param deltaPages the number of pages to add (each page is 65536 bytes)
   * @return true if the memory was successfully grown, false otherwise
   * @throws IllegalArgumentException if memory is null or deltaPages is negative
   * @throws RuntimeException if the growth operation fails
   */
  boolean growMemory(final WasmMemory memory, final int deltaPages);

  /**
   * Gets the current page count for the specified memory.
   *
   * @param memory the memory instance to query
   * @return the current number of pages (each page is 65536 bytes)
   * @throws IllegalArgumentException if memory is null
   * @throws RuntimeException if unable to get page count
   */
  int getMemoryPageCount(final WasmMemory memory);

  /**
   * Gets the current byte size for the specified memory.
   *
   * @param memory the memory instance to query
   * @return the current size in bytes
   * @throws IllegalArgumentException if memory is null
   * @throws RuntimeException if unable to get byte size
   */
  long getMemoryByteSize(final WasmMemory memory);

  /**
   * Checks if multi-memory support is available in the current runtime.
   *
   * @return true if multi-memory operations are supported, false otherwise
   */
  boolean isMultiMemorySupported();

  /**
   * Gets the maximum number of memory instances supported by the runtime.
   *
   * @return the maximum number of memory instances, or -1 if unlimited
   * @throws RuntimeException if unable to determine the limit
   */
  int getMaxMemoryInstances();
}
