package ai.tegmentum.wasmtime4j;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * High-performance bulk memory operations for WebAssembly linear memory.
 *
 * <p>This interface provides optimized bulk operations for memory manipulation, designed for
 * enterprise-grade applications that require efficient memory management. All operations include
 * comprehensive bounds checking and defensive programming to prevent JVM crashes.
 *
 * @since 1.0.0
 */
public interface BulkMemoryOperations {

  /**
   * Performs a high-performance bulk copy between two memory regions.
   *
   * <p>This operation is optimized for large data transfers and uses native implementations for
   * maximum performance while maintaining safety through defensive programming.
   *
   * @param dest the destination memory
   * @param destOffset the offset in the destination memory
   * @param source the source memory
   * @param sourceOffset the offset in the source memory
   * @param length the number of bytes to copy
   * @throws IllegalArgumentException if any memory is null
   * @throws IndexOutOfBoundsException if any offset or length is invalid
   * @throws RuntimeException if the copy operation fails
   */
  void bulkCopy(
      final WasmMemory dest,
      final int destOffset,
      final WasmMemory source,
      final int sourceOffset,
      final int length);

  /**
   * Fills a memory region with a specified byte value using optimized bulk operations.
   *
   * <p>This operation provides high-performance memory initialization and clearing capabilities
   * with comprehensive bounds validation.
   *
   * @param memory the target memory
   * @param offset the starting offset in memory
   * @param length the number of bytes to fill
   * @param value the byte value to fill with
   * @throws IllegalArgumentException if memory is null
   * @throws IndexOutOfBoundsException if offset or length is invalid
   * @throws RuntimeException if the fill operation fails
   */
  void bulkFill(final WasmMemory memory, final int offset, final int length, final byte value);

  /**
   * Compares two memory regions for equality using optimized bulk comparison.
   *
   * <p>This operation provides high-performance memory comparison with early termination and
   * comprehensive bounds checking.
   *
   * @param memory1 the first memory to compare
   * @param offset1 the offset in the first memory
   * @param memory2 the second memory to compare
   * @param offset2 the offset in the second memory
   * @param length the number of bytes to compare
   * @return 0 if equal, negative if memory1 &lt; memory2, positive if memory1 &gt; memory2
   * @throws IllegalArgumentException if any memory is null
   * @throws IndexOutOfBoundsException if any offset or length is invalid
   * @throws RuntimeException if the comparison fails
   */
  int bulkCompare(
      final WasmMemory memory1,
      final int offset1,
      final WasmMemory memory2,
      final int offset2,
      final int length);

  /**
   * Performs batched write operations to memory using a single native call for optimal performance.
   *
   * <p>This operation reduces JNI/Panama call overhead by batching multiple write operations into a
   * single native invocation while maintaining full validation and error handling.
   *
   * @param memory the target memory
   * @param writes a map of offset to ByteBuffer pairs representing the data to write
   * @throws IllegalArgumentException if memory or writes map is null, or if any ByteBuffer is null
   * @throws IndexOutOfBoundsException if any write would exceed memory bounds
   * @throws RuntimeException if any write operation fails
   */
  void batchWrite(final WasmMemory memory, final Map<Integer, ByteBuffer> writes);

  /**
   * Performs batched read operations from memory using a single native call for optimal
   * performance.
   *
   * <p>This operation reduces JNI/Panama call overhead by batching multiple read operations into a
   * single native invocation while maintaining full validation and error handling.
   *
   * @param memory the source memory
   * @param reads a map of offset to length pairs representing the data to read
   * @return a map of offset to ByteBuffer pairs containing the read data
   * @throws IllegalArgumentException if memory or reads map is null
   * @throws IndexOutOfBoundsException if any read would exceed memory bounds
   * @throws RuntimeException if any read operation fails
   */
  Map<Integer, ByteBuffer> batchRead(final WasmMemory memory, final Map<Integer, Integer> reads);

  /**
   * Performs an optimized memory search operation for a specific byte pattern.
   *
   * <p>This operation provides high-performance pattern matching within memory regions using native
   * optimization techniques.
   *
   * @param memory the memory to search
   * @param offset the starting offset for the search
   * @param length the length of the region to search
   * @param pattern the byte pattern to search for
   * @return the offset of the first match, or -1 if not found
   * @throws IllegalArgumentException if memory or pattern is null, or pattern is empty
   * @throws IndexOutOfBoundsException if the search region is invalid
   * @throws RuntimeException if the search operation fails
   */
  int bulkSearch(final WasmMemory memory, final int offset, final int length, final byte[] pattern);

  /**
   * Performs an optimized memory move operation that handles overlapping regions correctly.
   *
   * <p>This operation provides safe memory movement even when source and destination regions
   * overlap, using appropriate copy direction to prevent data corruption.
   *
   * @param memory the target memory (both source and destination)
   * @param destOffset the destination offset
   * @param sourceOffset the source offset
   * @param length the number of bytes to move
   * @throws IllegalArgumentException if memory is null
   * @throws IndexOutOfBoundsException if any offset or length is invalid
   * @throws RuntimeException if the move operation fails
   */
  void bulkMove(
      final WasmMemory memory, final int destOffset, final int sourceOffset, final int length);
}
