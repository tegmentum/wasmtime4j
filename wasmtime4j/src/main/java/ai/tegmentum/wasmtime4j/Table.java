package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a WebAssembly table instance.
 *
 * <p>WebAssembly tables are arrays of opaque values of a particular table element type.
 * Tables are used to store references that cannot be directly manipulated by WebAssembly code,
 * such as function references.
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

  /**
   * Enumeration of supported table element types.
   */
  enum TableElementType {
    /** Function reference type */
    FUNCREF,
    /** External reference type */
    EXTERNREF
  }
}