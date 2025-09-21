package ai.tegmentum.wasmtime4j;

/**
 * Reference types support for advanced WebAssembly reference type operations.
 *
 * <p>This interface provides access to WebAssembly reference types including function references
 * (funcref), external references (externref), and any references (anyref). Reference types enable
 * more sophisticated memory management and interoperability patterns in WebAssembly modules.
 *
 * <p>All operations include comprehensive validation and defensive programming to ensure type
 * safety and prevent JVM crashes when working with reference values.
 *
 * @since 1.0.0
 */
public interface ReferenceTypes {

  /**
   * Creates a function reference type for the specified function type.
   *
   * @param type the function type specification
   * @return a new function reference type
   * @throws IllegalArgumentException if type is null
   * @throws RuntimeException if reference type creation fails
   */
  RefType createFunctionReference(final FuncType type);

  /**
   * Creates an external reference type for host objects.
   *
   * @return a new external reference type
   * @throws RuntimeException if reference type creation fails
   */
  RefType createExternalReference();

  /**
   * Creates an any reference type that can hold any reference.
   *
   * @return a new any reference type
   * @throws RuntimeException if reference type creation fails
   */
  RefType createAnyReference();

  /**
   * Creates a null reference value for the specified reference type.
   *
   * @param type the reference type for the null value
   * @return a null reference value of the specified type
   * @throws IllegalArgumentException if type is null
   * @throws RuntimeException if null reference creation fails
   */
  WasmValue createNullReference(final RefType type);

  /**
   * Checks if the specified value is a null reference.
   *
   * @param ref the reference value to check
   * @return true if the value is a null reference, false otherwise
   * @throws IllegalArgumentException if ref is null
   */
  boolean isNullReference(final WasmValue ref);

  /**
   * Gets the reference type of the specified reference value.
   *
   * @param ref the reference value to examine
   * @return the reference type of the value
   * @throws IllegalArgumentException if ref is null or not a reference type
   * @throws RuntimeException if unable to determine the reference type
   */
  RefType getReferenceType(final WasmValue ref);

  /**
   * Sets a table element to the specified reference value.
   *
   * @param table the table to modify
   * @param index the element index in the table
   * @param ref the reference value to set
   * @throws IllegalArgumentException if table or ref is null, or index is negative
   * @throws IndexOutOfBoundsException if index exceeds table bounds
   * @throws RuntimeException if the set operation fails
   */
  void setTableElement(final WasmTable table, final int index, final WasmValue ref);

  /**
   * Gets a table element as a reference value.
   *
   * @param table the table to read from
   * @param index the element index in the table
   * @return the reference value at the specified index
   * @throws IllegalArgumentException if table is null or index is negative
   * @throws IndexOutOfBoundsException if index exceeds table bounds
   * @throws RuntimeException if the get operation fails
   */
  WasmValue getTableElement(final WasmTable table, final int index);

  /**
   * Copies table elements between tables.
   *
   * @param source the source table
   * @param sourceIndex the starting index in the source table
   * @param dest the destination table
   * @param destIndex the starting index in the destination table
   * @param length the number of elements to copy
   * @throws IllegalArgumentException if any table is null or any index is negative
   * @throws IndexOutOfBoundsException if any index or length exceeds table bounds
   * @throws RuntimeException if the copy operation fails
   */
  void copyTableElements(
      final WasmTable source,
      final int sourceIndex,
      final WasmTable dest,
      final int destIndex,
      final int length);

  /**
   * Fills table elements with the specified reference value.
   *
   * @param table the table to fill
   * @param index the starting index in the table
   * @param length the number of elements to fill
   * @param ref the reference value to fill with
   * @throws IllegalArgumentException if table or ref is null, or index is negative
   * @throws IndexOutOfBoundsException if index or length exceeds table bounds
   * @throws RuntimeException if the fill operation fails
   */
  void fillTable(final WasmTable table, final int index, final int length, final WasmValue ref);

  /**
   * Checks if reference types are supported by the current runtime.
   *
   * @return true if reference types are supported, false otherwise
   */
  boolean areReferenceTypesSupported();

  /**
   * Creates a function reference from an existing WebAssembly function.
   *
   * @param function the WebAssembly function to create a reference for
   * @return a function reference value
   * @throws IllegalArgumentException if function is null
   * @throws RuntimeException if function reference creation fails
   */
  WasmValue createFunctionReferenceValue(final WasmFunction function);

  /**
   * Creates an external reference from a host object.
   *
   * @param hostObject the host object to wrap in an external reference
   * @return an external reference value
   * @throws IllegalArgumentException if hostObject is null
   * @throws RuntimeException if external reference creation fails
   */
  WasmValue createExternalReferenceValue(final Object hostObject);

  /**
   * Extracts the host object from an external reference.
   *
   * @param externRef the external reference to unwrap
   * @return the wrapped host object
   * @throws IllegalArgumentException if externRef is null or not an external reference
   * @throws RuntimeException if unable to extract the host object
   */
  Object extractHostObject(final WasmValue externRef);
}
