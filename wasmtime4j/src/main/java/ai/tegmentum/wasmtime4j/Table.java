package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a WebAssembly table instance.
 *
 * <p>WebAssembly tables are arrays of opaque values of a particular table element type. Tables are
 * used to store references that cannot be directly manipulated by WebAssembly code, such as
 * function references.
 *
 * @since 1.0.0
 */
public interface Table {

  /**
   * Gets the current size of this table.
   *
   * @return the current table size (number of elements)
   */
  long getSize();

  /**
   * Grows this table by the specified number of elements.
   *
   * @param deltaElements the number of elements to grow by
   * @param initValue the initial value for new elements
   * @return the previous size of the table, or -1 if the grow operation failed
   * @throws WasmException if the grow operation fails
   */
  long grow(final long deltaElements, final Object initValue) throws WasmException;

  /**
   * Gets an element from the table at the specified index.
   *
   * @param index the index of the element to retrieve
   * @return the element at the specified index
   * @throws WasmException if the index is out of bounds or the operation fails
   */
  Object get(final long index) throws WasmException;

  /**
   * Sets an element in the table at the specified index.
   *
   * @param index the index of the element to set
   * @param value the value to set at the specified index
   * @throws WasmException if the index is out of bounds or the operation fails
   */
  void set(final long index, final Object value) throws WasmException;

  /**
   * Gets the element type of this table.
   *
   * @return the table element type
   */
  TableElementType getElementType();

  /**
   * Gets the maximum number of elements this table can grow to.
   *
   * @return the maximum table size, or -1 if unlimited
   */
  long getMaxSize();

  /**
   * Checks if this table instance is still valid.
   *
   * @return true if the table is valid and can be used, false otherwise
   */
  boolean isValid();

  // ===== Bulk Table Operations =====

  /**
   * Fills a region of this table with the specified value.
   *
   * <p>This method efficiently sets multiple consecutive elements to the same value, which is
   * useful for initializing or resetting table regions.
   *
   * @param dstIndex the starting index in this table
   * @param value the value to fill with
   * @param length the number of elements to fill
   * @throws WasmException if the operation fails or indices are out of bounds
   * @throws IllegalArgumentException if length is negative
   * @since 1.0.0
   */
  void fill(long dstIndex, Object value, long length) throws WasmException;

  /**
   * Copies elements from another table to this table.
   *
   * <p>This method efficiently copies a region of elements from a source table to a destination
   * region in this table. The source and destination regions may overlap if the tables are the
   * same.
   *
   * @param dstIndex the starting index in this table
   * @param srcTable the source table to copy from
   * @param srcIndex the starting index in the source table
   * @param length the number of elements to copy
   * @throws WasmException if the operation fails or indices are out of bounds
   * @throws IllegalArgumentException if srcTable is null or length is negative
   * @since 1.0.0
   */
  void copy(long dstIndex, Table srcTable, long srcIndex, long length) throws WasmException;

  /**
   * Gets the table type (full type information including limits).
   *
   * <p>This returns complete type information about the table, including the element type, minimum
   * size, and optional maximum size.
   *
   * @return the complete TableType
   * @since 1.0.0
   */
  TableType getTableType();

  // ===== Async Table Operations =====

  /**
   * Grows this table asynchronously.
   *
   * <p>This method performs table growth in an async context, allowing the operation to yield if
   * the store is configured with async resource limiting.
   *
   * <p><b>Note:</b> The async feature must be enabled in the engine configuration.
   *
   * @param deltaElements the number of elements to grow by
   * @param initValue the initial value for new elements
   * @return a future that completes with the previous size, or -1 if growth failed
   * @since 1.0.0
   */
  java.util.concurrent.CompletableFuture<Long> growAsync(long deltaElements, Object initValue);

  /** Enumeration of supported table element types. */
  enum TableElementType {
    /** Function reference type. */
    FUNCREF,
    /** External reference type. */
    EXTERNREF
  }
}
