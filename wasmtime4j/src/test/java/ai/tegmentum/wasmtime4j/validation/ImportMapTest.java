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
package ai.tegmentum.wasmtime4j.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ImportMap} interface and its {@code empty()} factory implementation.
 *
 * <p>Uses dynamic proxies for WasmFunction/WasmMemory/WasmGlobal/WasmTable to avoid mocking while
 * satisfying the interface contracts. The proxies are never invoked; they serve only as typed
 * objects stored in the import map.
 */
@DisplayName("ImportMap Tests")
class ImportMapTest {

  @SuppressWarnings("unchecked")
  private static <T> T proxy(final Class<T> iface) {
    return (T)
        Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[] {iface},
            (p, method, args) -> {
              if ("toString".equals(method.getName())) {
                return "Test" + iface.getSimpleName();
              }
              throw new UnsupportedOperationException(
                  "Test proxy: " + method.getName() + " not implemented");
            });
  }

  @Nested
  @DisplayName("Empty Factory Tests")
  class EmptyFactoryTests {

    @Test
    @DisplayName("empty() should return non-null ImportMap")
    void emptyShouldReturnNonNull() {
      final ImportMap map = ImportMap.empty();

      assertNotNull(map, "empty() should not return null");
    }

    @Test
    @DisplayName("empty() should have empty imports")
    void emptyShouldHaveEmptyImports() {
      final ImportMap map = ImportMap.empty();

      assertTrue(map.getImports().isEmpty(), "Empty import map should have no imports");
    }
  }

  @Nested
  @DisplayName("Function Operations Tests")
  class FunctionOperationsTests {

    @Test
    @DisplayName("addFunction and getFunction should round-trip")
    void addFunctionShouldBeRetrievable() {
      final ImportMap map = ImportMap.empty();
      final WasmFunction func = proxy(WasmFunction.class);

      map.addFunction("math", "add", func);

      assertTrue(map.contains("math", "add"), "Should contain added function");
      final Optional<WasmFunction> retrieved = map.getFunction("math", "add");
      assertTrue(retrieved.isPresent(), "Function should be retrievable");
      assertSame(func, retrieved.get(), "Retrieved function should be same instance");
    }

    @Test
    @DisplayName("getFunction for missing module should return empty")
    void getFunctionMissingModuleShouldReturnEmpty() {
      final ImportMap map = ImportMap.empty();

      final Optional<WasmFunction> result = map.getFunction("nonexistent", "foo");

      assertFalse(result.isPresent(), "Missing module should return empty");
    }

    @Test
    @DisplayName("getFunction for non-function import should return empty")
    void getFunctionForNonFunctionShouldReturnEmpty() {
      final ImportMap map = ImportMap.empty();
      map.addMemory("env", "memory", proxy(WasmMemory.class));

      final Optional<WasmFunction> result = map.getFunction("env", "memory");

      assertFalse(result.isPresent(), "Non-function import should return empty from getFunction");
    }
  }

  @Nested
  @DisplayName("Other Import Type Tests")
  class OtherImportTypeTests {

    @Test
    @DisplayName("addMemory should be containable")
    void addMemoryShouldBeContainable() {
      final ImportMap map = ImportMap.empty();

      map.addMemory("env", "memory", proxy(WasmMemory.class));

      assertTrue(map.contains("env", "memory"), "Should contain added memory");
    }

    @Test
    @DisplayName("addGlobal should be containable")
    void addGlobalShouldBeContainable() {
      final ImportMap map = ImportMap.empty();

      map.addGlobal("env", "stack_pointer", proxy(WasmGlobal.class));

      assertTrue(map.contains("env", "stack_pointer"), "Should contain added global");
    }

    @Test
    @DisplayName("addTable should be containable")
    void addTableShouldBeContainable() {
      final ImportMap map = ImportMap.empty();

      map.addTable("env", "__indirect_function_table", proxy(WasmTable.class));

      assertTrue(map.contains("env", "__indirect_function_table"), "Should contain added table");
    }
  }

  @Nested
  @DisplayName("Contains and GetImports Tests")
  class ContainsAndGetImportsTests {

    @Test
    @DisplayName("contains should return false for non-existent import")
    void containsShouldReturnFalseForNonExistent() {
      final ImportMap map = ImportMap.empty();

      assertFalse(map.contains("env", "foo"), "Non-existent should return false");
    }

    @Test
    @DisplayName("contains should return false for non-existent module")
    void containsShouldReturnFalseForNonExistentModule() {
      final ImportMap map = ImportMap.empty();
      map.addFunction("env", "log", proxy(WasmFunction.class));

      assertFalse(map.contains("wasi", "log"), "Non-existent module should return false");
    }

    @Test
    @DisplayName("getImports should return all imports")
    void getImportsShouldReturnAll() {
      final ImportMap map = ImportMap.empty();
      map.addFunction("env", "log", proxy(WasmFunction.class));
      map.addMemory("env", "memory", proxy(WasmMemory.class));
      map.addGlobal("wasi", "clock", proxy(WasmGlobal.class));

      final Map<String, Map<String, Object>> imports = map.getImports();

      assertEquals(2, imports.size(), "Should have 2 modules (env and wasi)");
      assertTrue(imports.containsKey("env"), "Should contain 'env' module");
      assertTrue(imports.containsKey("wasi"), "Should contain 'wasi' module");
      assertEquals(2, imports.get("env").size(), "env module should have 2 imports");
      assertEquals(1, imports.get("wasi").size(), "wasi module should have 1 import");
    }

    @Test
    @DisplayName("fluent chaining should return same ImportMap instance for addFunction")
    void fluentChainingAddFunctionShouldReturnSameInstance() {
      final ImportMap map = ImportMap.empty();

      final ImportMap result = map.addFunction("m", "f", proxy(WasmFunction.class));

      assertSame(map, result, "addFunction should return same instance");
    }

    @Test
    @DisplayName("fluent chaining should return same ImportMap instance for addMemory")
    void fluentChainingAddMemoryShouldReturnSameInstance() {
      final ImportMap map = ImportMap.empty();

      final ImportMap result = map.addMemory("m", "mem", proxy(WasmMemory.class));

      assertSame(map, result, "addMemory should return same instance");
    }

    @Test
    @DisplayName("fluent chaining should return same ImportMap instance for addGlobal")
    void fluentChainingAddGlobalShouldReturnSameInstance() {
      final ImportMap map = ImportMap.empty();

      final ImportMap result = map.addGlobal("m", "g", proxy(WasmGlobal.class));

      assertSame(map, result, "addGlobal should return same instance");
    }

    @Test
    @DisplayName("fluent chaining should return same ImportMap instance for addTable")
    void fluentChainingAddTableShouldReturnSameInstance() {
      final ImportMap map = ImportMap.empty();

      final ImportMap result = map.addTable("m", "t", proxy(WasmTable.class));

      assertSame(map, result, "addTable should return same instance");
    }
  }
}
