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
    // Use the same runtime selection pattern as WasmRuntimeFactory
    try {
      // First try Panama implementation
      final Class<?> panamaClass = Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaImportMap");
      return (ImportMap) panamaClass.getDeclaredConstructor().newInstance();
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> jniClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniImportMap");
        return (ImportMap) jniClass.getDeclaredConstructor().newInstance();
      } catch (final ClassNotFoundException e2) {
        // No specific implementation found, use Panama as fallback since it's more universal
        throw new RuntimeException(
            "No ImportMap implementation available. Ensure wasmtime4j-panama or wasmtime4j-jni is"
                + " on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create JNI ImportMap instance", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create Panama ImportMap instance", e);
    }
  }
}
