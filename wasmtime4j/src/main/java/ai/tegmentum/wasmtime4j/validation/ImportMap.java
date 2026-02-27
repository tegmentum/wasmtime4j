package ai.tegmentum.wasmtime4j.validation;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import java.util.Map;
import java.util.Optional;

/**
 * A map of imports to provide to WebAssembly modules during instantiation.
 *
 * <p>This interface provides a way to supply host functions, memories, globals, and tables that a
 * WebAssembly module requires for instantiation.
 *
 * @since 1.0.0
 */
public interface ImportMap {

  /**
   * Adds a function import to the map.
   *
   * @param moduleName the module name of the import
   * @param name the name of the import
   * @param function the function to provide
   * @return this ImportMap for method chaining
   */
  ImportMap addFunction(final String moduleName, final String name, final WasmFunction function);

  /**
   * Adds a memory import to the map.
   *
   * @param moduleName the module name of the import
   * @param name the name of the import
   * @param memory the memory to provide
   * @return this ImportMap for method chaining
   */
  ImportMap addMemory(final String moduleName, final String name, final WasmMemory memory);

  /**
   * Adds a global import to the map.
   *
   * @param moduleName the module name of the import
   * @param name the name of the import
   * @param global the global to provide
   * @return this ImportMap for method chaining
   */
  ImportMap addGlobal(final String moduleName, final String name, final WasmGlobal global);

  /**
   * Adds a table import to the map.
   *
   * @param moduleName the module name of the import
   * @param name the name of the import
   * @param table the table to provide
   * @return this ImportMap for method chaining
   */
  ImportMap addTable(final String moduleName, final String name, final WasmTable table);

  /**
   * Gets a function import by module and name.
   *
   * @param moduleName the module name
   * @param name the import name
   * @return the function, or empty if not found
   */
  Optional<WasmFunction> getFunction(final String moduleName, final String name);

  /**
   * Gets all imports as a nested map structure.
   *
   * @return a map of module name to import name to import object
   */
  Map<String, Map<String, Object>> getImports();

  /**
   * Checks if this import map contains an import for the given module and name.
   *
   * @param moduleName the module name
   * @param name the import name
   * @return true if the import exists
   */
  boolean contains(final String moduleName, final String name);

  /**
   * Creates an empty import map.
   *
   * @return a new empty ImportMap
   */
  static ImportMap empty() {
    return new ImportMap() {
      private final java.util.Map<String, java.util.Map<String, Object>> imports =
          new java.util.HashMap<>();

      @Override
      public ImportMap addFunction(
          final String moduleName, final String name, final WasmFunction function) {
        imports.computeIfAbsent(moduleName, k -> new java.util.HashMap<>()).put(name, function);
        return this;
      }

      @Override
      public ImportMap addMemory(
          final String moduleName, final String name, final WasmMemory memory) {
        imports.computeIfAbsent(moduleName, k -> new java.util.HashMap<>()).put(name, memory);
        return this;
      }

      @Override
      public ImportMap addGlobal(
          final String moduleName, final String name, final WasmGlobal global) {
        imports.computeIfAbsent(moduleName, k -> new java.util.HashMap<>()).put(name, global);
        return this;
      }

      @Override
      public ImportMap addTable(final String moduleName, final String name, final WasmTable table) {
        imports.computeIfAbsent(moduleName, k -> new java.util.HashMap<>()).put(name, table);
        return this;
      }

      @Override
      public Optional<WasmFunction> getFunction(final String moduleName, final String name) {
        final java.util.Map<String, Object> moduleImports = imports.get(moduleName);
        if (moduleImports == null) {
          return Optional.empty();
        }
        final Object obj = moduleImports.get(name);
        if (obj instanceof WasmFunction) {
          return Optional.of((WasmFunction) obj);
        }
        return Optional.empty();
      }

      @Override
      public java.util.Map<String, java.util.Map<String, Object>> getImports() {
        return imports;
      }

      @Override
      public boolean contains(final String moduleName, final String name) {
        final java.util.Map<String, Object> moduleImports = imports.get(moduleName);
        return moduleImports != null && moduleImports.containsKey(name);
      }
    };
  }
}
