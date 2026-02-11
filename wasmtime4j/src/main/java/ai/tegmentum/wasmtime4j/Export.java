package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.func.Function;

import ai.tegmentum.wasmtime4j.type.WasmTypeKind;

import ai.tegmentum.wasmtime4j.type.ExportType;

/**
 * Represents a WebAssembly export item.
 *
 * <p>An export can be one of several types: function, memory, table, or global. This interface
 * provides a unified way to handle any export type while allowing safe casting to specific export
 * types.
 *
 * @since 1.0.0
 */
public interface Export {

  /**
   * Gets the name of this export.
   *
   * @return the export name
   */
  String getName();

  /**
   * Gets the type of this export.
   *
   * @return the export type
   */
  ExportType getExportType();

  /**
   * Gets the kind of this export.
   *
   * @return the export kind
   */
  default WasmTypeKind getKind() {
    return getExportType().getType().getKind();
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
   * Checks if this export is a global.
   *
   * @return true if this is a global export
   */
  default boolean isGlobal() {
    return getKind() == WasmTypeKind.GLOBAL;
  }

  /**
   * Casts this export to a function.
   *
   * @param <T> the type of user data associated with the store
   * @return this export as a function
   * @throws IllegalStateException if this export is not a function
   */
  @SuppressWarnings("unchecked")
  default <T> Function<T> asFunction() {
    if (!isFunction()) {
      throw new IllegalStateException("Export '" + getName() + "' is not a function");
    }
    return (Function<T>) this;
  }

  /**
   * Casts this export to a memory.
   *
   * @return this export as a memory
   * @throws IllegalStateException if this export is not a memory
   */
  default Memory asMemory() {
    if (!isMemory()) {
      throw new IllegalStateException("Export '" + getName() + "' is not a memory");
    }
    return (Memory) this;
  }

  /**
   * Casts this export to a table.
   *
   * @return this export as a table
   * @throws IllegalStateException if this export is not a table
   */
  default Table asTable() {
    if (!isTable()) {
      throw new IllegalStateException("Export '" + getName() + "' is not a table");
    }
    return (Table) this;
  }

  /**
   * Casts this export to a global.
   *
   * @return this export as a global
   * @throws IllegalStateException if this export is not a global
   */
  default Global asGlobal() {
    if (!isGlobal()) {
      throw new IllegalStateException("Export '" + getName() + "' is not a global");
    }
    return (Global) this;
  }
}
