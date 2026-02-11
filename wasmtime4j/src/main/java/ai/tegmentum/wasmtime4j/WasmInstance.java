package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.Function;
import ai.tegmentum.wasmtime4j.memory.Global;
import ai.tegmentum.wasmtime4j.memory.Memory;
import ai.tegmentum.wasmtime4j.memory.Table;
import ai.tegmentum.wasmtime4j.type.ExportType;
import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an instantiated WebAssembly module.
 *
 * <p>A WasmInstance provides access to the exports of a WebAssembly module, including functions,
 * memories, tables, and globals. Instances should be properly closed when no longer needed to free
 * system resources.
 *
 * @since 1.0.0
 */
public interface WasmInstance extends Closeable {

  /**
   * Gets an exported function by name.
   *
   * @param name the name of the exported function
   * @return the function, or empty if not found
   * @throws WasmException if the export exists but is not a function
   */
  Optional<Function<?>> getFunction(final String name) throws WasmException;

  /**
   * Gets an exported memory by name.
   *
   * @param name the name of the exported memory
   * @return the memory, or empty if not found
   * @throws WasmException if the export exists but is not a memory
   */
  Optional<Memory> getMemory(final String name) throws WasmException;

  /**
   * Gets an exported table by name.
   *
   * @param name the name of the exported table
   * @return the table, or empty if not found
   * @throws WasmException if the export exists but is not a table
   */
  Optional<Table> getTable(final String name) throws WasmException;

  /**
   * Gets an exported global by name.
   *
   * @param name the name of the exported global
   * @return the global, or empty if not found
   * @throws WasmException if the export exists but is not a global
   */
  Optional<Global> getGlobal(final String name) throws WasmException;

  /**
   * Gets the default memory export (named "memory" or the first memory export).
   *
   * @return the default memory, or empty if no memory exports exist
   */
  Optional<Memory> getDefaultMemory();

  /**
   * Gets all exported function names.
   *
   * @return list of exported function names
   */
  List<String> getFunctionNames();

  /**
   * Gets all exported memory names.
   *
   * @return list of exported memory names
   */
  List<String> getMemoryNames();

  /**
   * Gets all exported table names.
   *
   * @return list of exported table names
   */
  List<String> getTableNames();

  /**
   * Gets all exported global names.
   *
   * @return list of exported global names
   */
  List<String> getGlobalNames();

  /**
   * Gets all exports as a map.
   *
   * @return map of export names to their types
   */
  Map<String, ExportType> getExports();

  /**
   * Checks if this instance is still valid.
   *
   * @return true if the instance is valid and can be used, false otherwise
   */
  boolean isValid();

  /**
   * Gets the module that this instance was created from.
   *
   * @return the source module
   */
  Module getModule();

  /**
   * Closes this instance and releases its resources.
   *
   * <p>After calling this method, the instance becomes invalid and should not be used. Any attempt
   * to use the instance after closing may result in exceptions.
   */
  @Override
  void close();

  /** Enumeration of WebAssembly export types. */
  enum ExportType {
    /** Function export. */
    FUNCTION,
    /** Memory export. */
    MEMORY,
    /** Table export. */
    TABLE,
    /** Global export. */
    GLOBAL
  }
}
