package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;

/**
 * Describes a WebAssembly export with detailed type information.
 *
 * <p>This interface provides comprehensive metadata about an export including field name and the
 * specific type information of the export.
 *
 * @since 1.0.0
 */
public interface ExportDescriptor {

  /**
   * Gets the name of the export.
   *
   * @return the export name
   */
  String getName();

  /**
   * Gets the type of the export.
   *
   * @return the export type
   */
  WasmType getType();

  /**
   * Gets the kind of the export.
   *
   * @return the export kind
   */
  default WasmTypeKind getKind() {
    return getType().getKind();
  }

  /**
   * Checks if this export is a function.
   *
   * @return true if this is a function export
   */
  default boolean isFunction() {
    return getKind() == WasmTypeKind.FUNCTION;
  }

  /**
   * Checks if this export is a global.
   *
   * @return true if this is a global export
   */
  default boolean isGlobal() {
    return getKind() == WasmTypeKind.GLOBAL;
  }

  /**
   * Checks if this export is a memory.
   *
   * @return true if this is a memory export
   */
  default boolean isMemory() {
    return getKind() == WasmTypeKind.MEMORY;
  }

  /**
   * Checks if this export is a table.
   *
   * @return true if this is a table export
   */
  default boolean isTable() {
    return getKind() == WasmTypeKind.TABLE;
  }

  /**
   * Gets the function type if this is a function export.
   *
   * @return the function type
   * @throws IllegalStateException if this is not a function export
   */
  default FuncType asFunctionType() {
    if (!isFunction()) {
      throw new IllegalStateException("Export is not a function");
    }
    return (FuncType) getType();
  }

  /**
   * Gets the global type if this is a global export.
   *
   * @return the global type
   * @throws IllegalStateException if this is not a global export
   */
  default GlobalType asGlobalType() {
    if (!isGlobal()) {
      throw new IllegalStateException("Export is not a global");
    }
    return (GlobalType) getType();
  }

  /**
   * Gets the memory type if this is a memory export.
   *
   * @return the memory type
   * @throws IllegalStateException if this is not a memory export
   */
  default MemoryType asMemoryType() {
    if (!isMemory()) {
      throw new IllegalStateException("Export is not a memory");
    }
    return (MemoryType) getType();
  }

  /**
   * Gets the table type if this is a table export.
   *
   * @return the table type
   * @throws IllegalStateException if this is not a table export
   */
  default TableType asTableType() {
    if (!isTable()) {
      throw new IllegalStateException("Export is not a table");
    }
    return (TableType) getType();
  }
}
