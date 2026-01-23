package ai.tegmentum.wasmtime4j.tests.module;

import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple ImportMap implementation for testing purposes.
 *
 * <p>This class provides a minimal implementation of ImportMap to support testing without requiring
 * full JniImportMap/PanamaImportMap implementations.
 */
final class TestImportMap implements ImportMap {
  private final Map<String, Map<String, Object>> imports = new HashMap<>();

  @Override
  public ImportMap addFunction(
      final String moduleName, final String name, final WasmFunction function) {
    imports.computeIfAbsent(moduleName, k -> new HashMap<>()).put(name, function);
    return this;
  }

  @Override
  public ImportMap addMemory(final String moduleName, final String name, final WasmMemory memory) {
    imports.computeIfAbsent(moduleName, k -> new HashMap<>()).put(name, memory);
    return this;
  }

  @Override
  public ImportMap addGlobal(final String moduleName, final String name, final WasmGlobal global) {
    imports.computeIfAbsent(moduleName, k -> new HashMap<>()).put(name, global);
    return this;
  }

  @Override
  public ImportMap addTable(final String moduleName, final String name, final WasmTable table) {
    imports.computeIfAbsent(moduleName, k -> new HashMap<>()).put(name, table);
    return this;
  }

  @Override
  public Optional<WasmFunction> getFunction(final String moduleName, final String name) {
    final Map<String, Object> moduleImports = imports.get(moduleName);
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
  public Map<String, Map<String, Object>> getImports() {
    return imports;
  }

  @Override
  public boolean contains(final String moduleName, final String name) {
    final Map<String, Object> moduleImports = imports.get(moduleName);
    return moduleImports != null && moduleImports.containsKey(name);
  }
}
