package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.TableType;

/**
 * Represents a WebAssembly table.
 *
 * <p>Tables are resizable arrays of references (like function references) that can be accessed by
 * WebAssembly code. They provide a way to implement indirect function calls and other dynamic
 * behavior.
 *
 * @since 1.0.0
 */
public interface WasmTable {

  /**
   * Gets the current size of the table.
   *
   * @return the current number of elements in the table
   */
  int getSize();

  /**
   * Gets the current size of the table. Alias for getSize() for compatibility.
   *
   * @return the current number of elements in the table
   */
  default int size() {
    return getSize();
  }

  /**
   * Gets the type of this table.
   *
   * @return the table type
   */
  WasmValueType getType();

  /**
   * Gets the complete type information for this table.
   *
   * <p>The TableType provides full type information including element type and size constraints
   * (minimum and maximum). This is more comprehensive than {@link #getType()} which only returns
   * the element value type.
   *
   * @return the complete table type information
   * @since 1.0.0
   */
  TableType getTableType();

  /**
   * Grows the table by the specified number of elements.
   *
   * @param elements the number of elements to add
   * @param initValue the initial value for new elements
   * @return the previous size, or -1 if growth failed
   */
  int grow(final int elements, final Object initValue);

  /**
   * Gets the maximum size of the table.
   *
   * @return the maximum number of elements, or -1 if unlimited
   */
  int getMaxSize();

  /**
   * Gets an element from the table at the given index.
   *
   * @param index the index of the element
   * @return the element at the index
   * @throws IndexOutOfBoundsException if index is out of bounds
   */
  Object get(final int index);

  /**
   * Sets an element in the table at the given index.
   *
   * @param index the index of the element
   * @param value the value to set
   * @throws IndexOutOfBoundsException if index is out of bounds
   */
  void set(final int index, final Object value);

  /**
   * Gets the element type of this table.
   *
   * @return the element type
   */
  WasmValueType getElementType();

  /**
   * Fills a range of the table with the specified value.
   *
   * @param start the starting index
   * @param count the number of elements to fill
   * @param value the value to fill with
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException if start or count is negative
   */
  void fill(final int start, final int count, final Object value);

  /**
   * Copies elements from one region of the table to another.
   *
   * @param dst the destination starting index
   * @param src the source starting index
   * @param count the number of elements to copy
   * @throws IndexOutOfBoundsException if source or destination range is out of bounds
   * @throws IllegalArgumentException if dst, src, or count is negative
   */
  void copy(final int dst, final int src, final int count);

  /**
   * Copies elements from another table to this table.
   *
   * @param dst the destination starting index in this table
   * @param src the source table to copy from
   * @param srcIndex the source starting index in the source table
   * @param count the number of elements to copy
   * @throws IndexOutOfBoundsException if source or destination range is out of bounds
   * @throws IllegalArgumentException if dst, srcIndex, or count is negative
   * @throws IllegalArgumentException if src is null or incompatible type
   */
  void copy(final int dst, final WasmTable src, final int srcIndex, final int count);

  // Element Segment Operations

  /**
   * Initializes table elements from an element segment.
   *
   * <p>This is equivalent to the WebAssembly table.init instruction. The element segment must be
   * available and not dropped.
   *
   * @param destOffset the destination offset in the table
   * @param elementSegmentIndex the index of the element segment
   * @param srcOffset the source offset within the element segment
   * @param length the number of elements to copy
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   * @throws IllegalStateException if the element segment has been dropped
   */
  void init(
      final int destOffset, final int elementSegmentIndex, final int srcOffset, final int length);

  /**
   * Drops an element segment, making it unavailable for table.init operations.
   *
   * <p>This is equivalent to the WebAssembly elem.drop instruction. After dropping, the segment
   * cannot be used for table.init operations.
   *
   * @param elementSegmentIndex the index of the element segment to drop
   * @throws IllegalArgumentException if the segment index is invalid
   * @throws IllegalStateException if the segment has already been dropped
   */
  void dropElementSegment(final int elementSegmentIndex);

  // Memory64 Table Operations (64-bit addressing support)

  /**
   * Checks if this table supports 64-bit addressing.
   *
   * @return true if 64-bit addressing is supported, false otherwise
   * @since 1.1.0
   */
  default boolean supports64BitAddressing() {
    return false;
  }

  /**
   * Gets the current size of the table using 64-bit addressing.
   *
   * @return the current number of elements in the table (64-bit)
   * @throws UnsupportedOperationException if 64-bit addressing is not supported
   * @since 1.1.0
   */
  default long getSize64() {
    if (!supports64BitAddressing()) {
      throw new UnsupportedOperationException("Table does not support 64-bit addressing");
    }
    return getSize();
  }

  /**
   * Gets an element from the table at the given 64-bit index.
   *
   * @param index the index of the element (64-bit)
   * @return the element at the index
   * @throws IndexOutOfBoundsException if index is out of bounds
   * @throws UnsupportedOperationException if 64-bit addressing is not supported
   * @since 1.1.0
   */
  default WasmValue get64(final long index) {
    if (!supports64BitAddressing()) {
      throw new UnsupportedOperationException("Table does not support 64-bit addressing");
    }
    if (index > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("Index exceeds 32-bit limit: " + index);
    }
    return WasmValue.externref(get((int) index));
  }

  /**
   * Sets an element in the table at the given 64-bit index.
   *
   * @param index the index of the element (64-bit)
   * @param value the value to set
   * @throws IndexOutOfBoundsException if index is out of bounds
   * @throws UnsupportedOperationException if 64-bit addressing is not supported
   * @since 1.1.0
   */
  default void set64(final long index, final WasmValue value) {
    if (!supports64BitAddressing()) {
      throw new UnsupportedOperationException("Table does not support 64-bit addressing");
    }
    if (index > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("Index exceeds 32-bit limit: " + index);
    }
    set((int) index, value.getValue());
  }

  /**
   * Grows the table by the specified number of elements using 64-bit addressing.
   *
   * @param elements the number of elements to add (64-bit)
   * @param initValue the initial value for new elements
   * @return the previous size, or -1 if growth failed
   * @throws UnsupportedOperationException if 64-bit addressing is not supported
   * @since 1.1.0
   */
  default long grow64(final long elements, final WasmValue initValue) {
    if (!supports64BitAddressing()) {
      throw new UnsupportedOperationException("Table does not support 64-bit addressing");
    }
    if (elements > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Element count exceeds 32-bit limit: " + elements);
    }
    return grow((int) elements, initValue.getValue());
  }

  /**
   * Fills a range of the table with the specified value using 64-bit addressing.
   *
   * @param start the starting index (64-bit)
   * @param count the number of elements to fill (64-bit)
   * @param value the value to fill with
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException if start or count is negative
   * @throws UnsupportedOperationException if 64-bit addressing is not supported
   * @since 1.1.0
   */
  default void fill64(final long start, final long count, final WasmValue value) {
    if (!supports64BitAddressing()) {
      throw new UnsupportedOperationException("Table does not support 64-bit addressing");
    }
    if (start > Integer.MAX_VALUE || count > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Parameters exceed 32-bit limits");
    }
    fill((int) start, (int) count, value.getValue());
  }

  /**
   * Copies elements from another table to this table using 64-bit addressing.
   *
   * @param dst the destination starting index in this table (64-bit)
   * @param src the source table to copy from
   * @param srcIndex the source starting index in the source table (64-bit)
   * @param count the number of elements to copy (64-bit)
   * @throws IndexOutOfBoundsException if source or destination range is out of bounds
   * @throws IllegalArgumentException if dst, srcIndex, or count is negative
   * @throws IllegalArgumentException if src is null or incompatible type
   * @throws UnsupportedOperationException if 64-bit addressing is not supported
   * @since 1.1.0
   */
  default void copy64(final long dst, final WasmTable src, final long srcIndex, final long count) {
    if (!supports64BitAddressing()) {
      throw new UnsupportedOperationException("Table does not support 64-bit addressing");
    }
    if (dst > Integer.MAX_VALUE || srcIndex > Integer.MAX_VALUE || count > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Parameters exceed 32-bit limits");
    }
    copy((int) dst, src, (int) srcIndex, (int) count);
  }

  /**
   * Initializes table elements from an element segment using 64-bit addressing.
   *
   * @param destOffset the destination offset in the table (64-bit)
   * @param elementSegmentIndex the index of the element segment
   * @param srcOffset the source offset within the element segment (64-bit)
   * @param length the number of elements to copy (64-bit)
   * @throws IndexOutOfBoundsException if any offset or length is out of bounds
   * @throws IllegalStateException if the element segment has been dropped
   * @throws UnsupportedOperationException if 64-bit addressing is not supported
   * @since 1.1.0
   */
  default void init64(
      final long destOffset,
      final int elementSegmentIndex,
      final long srcOffset,
      final long length) {
    if (!supports64BitAddressing()) {
      throw new UnsupportedOperationException("Table does not support 64-bit addressing");
    }
    if (destOffset > Integer.MAX_VALUE
        || srcOffset > Integer.MAX_VALUE
        || length > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Parameters exceed 32-bit limits");
    }
    init((int) destOffset, elementSegmentIndex, (int) srcOffset, (int) length);
  }
}
