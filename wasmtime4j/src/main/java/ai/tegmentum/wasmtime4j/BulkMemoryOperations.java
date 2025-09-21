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

  // WebAssembly Specification Bulk Memory Operations

  /**
   * Initializes a memory region with data from a data segment (memory.init instruction).
   *
   * <p>This operation copies data from a passive data segment into linear memory. The data segment
   * must be in a passive state and is identified by its index.
   *
   * @param memory the target memory instance
   * @param memoryIndex the memory index (for multi-memory support)
   * @param data the data segment bytes to copy
   * @param dataIndex the starting index in the data segment
   * @param destOffset the destination offset in memory
   * @param size the number of bytes to copy
   * @throws IllegalArgumentException if memory or data is null, or any index is negative
   * @throws IndexOutOfBoundsException if any offset or size exceeds bounds
   * @throws RuntimeException if the initialization fails
   */
  void memoryInit(
      final WasmMemory memory,
      final int memoryIndex,
      final byte[] data,
      final int dataIndex,
      final int destOffset,
      final int size);

  /**
   * Drops a data segment making it inaccessible (data.drop instruction).
   *
   * <p>This operation removes a data segment from the module instance, making it no longer
   * available for memory.init operations. This is used for resource cleanup.
   *
   * @param dataIndex the index of the data segment to drop
   * @throws IllegalArgumentException if dataIndex is negative
   * @throws RuntimeException if the drop operation fails
   */
  void memoryDrop(final int dataIndex);

  /**
   * Copies data within the same memory instance (memory.copy instruction).
   *
   * <p>This operation provides the standard WebAssembly memory.copy semantics with proper handling
   * of overlapping regions using memmove semantics.
   *
   * @param memory the memory instance
   * @param destOffset the destination offset
   * @param sourceOffset the source offset
   * @param size the number of bytes to copy
   * @throws IllegalArgumentException if memory is null
   * @throws IndexOutOfBoundsException if any offset or size exceeds memory bounds
   * @throws RuntimeException if the copy operation fails
   */
  void memoryCopy(
      final WasmMemory memory, final int destOffset, final int sourceOffset, final int size);

  /**
   * Fills a memory region with a byte value (memory.fill instruction).
   *
   * <p>This operation implements the standard WebAssembly memory.fill instruction semantics for
   * efficient memory initialization.
   *
   * @param memory the memory instance
   * @param offset the starting offset in memory
   * @param value the byte value to fill with
   * @param size the number of bytes to fill
   * @throws IllegalArgumentException if memory is null
   * @throws IndexOutOfBoundsException if offset or size exceeds memory bounds
   * @throws RuntimeException if the fill operation fails
   */
  void memoryFill(final WasmMemory memory, final int offset, final byte value, final int size);

  /**
   * Initializes table elements from an element segment (table.init instruction).
   *
   * <p>This operation copies elements from a passive element segment into a table. The element
   * segment must be in a passive state and is identified by its index.
   *
   * @param table the target table
   * @param tableIndex the table index (for multi-table support)
   * @param elements the element segment values to copy
   * @param elementIndex the starting index in the element segment
   * @param destOffset the destination offset in the table
   * @param size the number of elements to copy
   * @throws IllegalArgumentException if table or elements is null, or any index is negative
   * @throws IndexOutOfBoundsException if any offset or size exceeds bounds
   * @throws RuntimeException if the initialization fails
   */
  void tableInit(
      final WasmTable table,
      final int tableIndex,
      final WasmValue[] elements,
      final int elementIndex,
      final int destOffset,
      final int size);

  /**
   * Drops an element segment making it inaccessible (elem.drop instruction).
   *
   * <p>This operation removes an element segment from the module instance, making it no longer
   * available for table.init operations. This is used for resource cleanup.
   *
   * @param elementIndex the index of the element segment to drop
   * @throws IllegalArgumentException if elementIndex is negative
   * @throws RuntimeException if the drop operation fails
   */
  void tableDrop(final int elementIndex);

  /**
   * Copies elements between tables or within the same table (table.copy instruction).
   *
   * <p>This operation implements the standard WebAssembly table.copy instruction semantics with
   * proper handling of overlapping regions.
   *
   * @param destTable the destination table
   * @param destIndex the destination index in the table
   * @param sourceTable the source table
   * @param sourceIndex the source index in the table
   * @param size the number of elements to copy
   * @throws IllegalArgumentException if any table is null or any index is negative
   * @throws IndexOutOfBoundsException if any index or size exceeds table bounds
   * @throws RuntimeException if the copy operation fails
   */
  void tableCopy(
      final WasmTable destTable,
      final int destIndex,
      final WasmTable sourceTable,
      final int sourceIndex,
      final int size);

  /**
   * Fills table elements with a value (table.fill instruction).
   *
   * <p>This operation implements the standard WebAssembly table.fill instruction semantics for
   * efficient table initialization.
   *
   * @param table the table to fill
   * @param index the starting index in the table
   * @param value the value to fill with
   * @param size the number of elements to fill
   * @throws IllegalArgumentException if table or value is null, or index is negative
   * @throws IndexOutOfBoundsException if index or size exceeds table bounds
   * @throws RuntimeException if the fill operation fails
   */
  void tableFill(final WasmTable table, final int index, final WasmValue value, final int size);

  /**
   * Grows a table by a specified number of elements (table.grow instruction).
   *
   * <p>This operation implements the standard WebAssembly table.grow instruction semantics, growing
   * the table and initializing new elements with the specified value.
   *
   * @param table the table to grow
   * @param delta the number of elements to add
   * @param initValue the value to initialize new elements with
   * @return the previous size of the table, or -1 if growth failed
   * @throws IllegalArgumentException if table or initValue is null, or delta is negative
   * @throws RuntimeException if the growth operation fails
   */
  int tableGrow(final WasmTable table, final int delta, final WasmValue initValue);

  /**
   * Checks if a data segment is active (not dropped).
   *
   * @param dataIndex the index of the data segment to check
   * @return true if the data segment is active, false if dropped
   * @throws IllegalArgumentException if dataIndex is negative
   * @throws RuntimeException if unable to check the data segment status
   */
  boolean isDataSegmentActive(final int dataIndex);

  /**
   * Checks if an element segment is active (not dropped).
   *
   * @param elementIndex the index of the element segment to check
   * @return true if the element segment is active, false if dropped
   * @throws IllegalArgumentException if elementIndex is negative
   * @throws RuntimeException if unable to check the element segment status
   */
  boolean isElementSegmentActive(final int elementIndex);

  /**
   * Gets the number of active data segments.
   *
   * @return the count of active data segments
   * @throws RuntimeException if unable to get the count
   */
  int getActiveDataSegmentCount();

  /**
   * Gets the number of active element segments.
   *
   * @return the count of active element segments
   * @throws RuntimeException if unable to get the count
   */
  int getActiveElementSegmentCount();
}
