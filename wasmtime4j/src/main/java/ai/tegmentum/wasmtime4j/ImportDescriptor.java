package ai.tegmentum.wasmtime4j;

/**
 * Describes a WebAssembly import requirement with detailed type information.
 *
 * <p>This interface provides comprehensive metadata about an import including module name, field
 * name, and the specific type information required by the import.
 *
 * @since 1.0.0
 */
public interface ImportDescriptor {

  /**
   * Gets the module name of the import.
   *
   * @return the module name
   */
  String getModuleName();

  /**
   * Gets the field name of the import.
   *
   * @return the field name
   */
  String getName();

  /**
   * Gets the type of the import.
   *
   * @return the import type
   */
  WasmType getType();

  /**
   * Gets the kind of the import.
   *
   * @return the import kind
   */
  default WasmTypeKind getKind() {
    return getType().getKind();
  }

  /**
   * Checks if this import is a function.
   *
   * @return true if this is a function import
   */
  default boolean isFunction() {
    return getKind() == WasmTypeKind.FUNCTION;
  }

  /**
   * Checks if this import is a global.
   *
   * @return true if this is a global import
   */
  default boolean isGlobal() {
    return getKind() == WasmTypeKind.GLOBAL;
  }

  /**
   * Checks if this import is a memory.
   *
   * @return true if this is a memory import
   */
  default boolean isMemory() {
    return getKind() == WasmTypeKind.MEMORY;
  }

  /**
   * Checks if this import is a table.
   *
   * @return true if this is a table import
   */
  default boolean isTable() {
    return getKind() == WasmTypeKind.TABLE;
  }

  /**
   * Gets the function type if this is a function import.
   *
   * @return the function type
   * @throws IllegalStateException if this is not a function import
   */
  default FuncType asFunctionType() {
    if (!isFunction()) {
      throw new IllegalStateException("Import is not a function");
    }
    return (FuncType) getType();
  }

  /**
   * Gets the global type if this is a global import.
   *
   * @return the global type
   * @throws IllegalStateException if this is not a global import
   */
  default GlobalType asGlobalType() {
    if (!isGlobal()) {
      throw new IllegalStateException("Import is not a global");
    }
    return (GlobalType) getType();
  }

  /**
   * Gets the memory type if this is a memory import.
   *
   * @return the memory type
   * @throws IllegalStateException if this is not a memory import
   */
  default MemoryType asMemoryType() {
    if (!isMemory()) {
      throw new IllegalStateException("Import is not a memory");
    }
    return (MemoryType) getType();
  }

  /**
   * Gets the table type if this is a table import.
   *
   * @return the table type
   * @throws IllegalStateException if this is not a table import
   */
  default TableType asTableType() {
    if (!isTable()) {
      throw new IllegalStateException("Import is not a table");
    }
    return (TableType) getType();
  }
}