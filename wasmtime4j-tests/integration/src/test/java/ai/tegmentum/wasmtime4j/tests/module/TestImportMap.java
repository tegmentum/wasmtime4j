/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.tests.module;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
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
