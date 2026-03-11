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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.func.HostFunctionAsync;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Linker} default methods.
 *
 * <p>These tests use anonymous implementations to exercise default method behavior without
 * requiring a native runtime.
 */
@DisplayName("Linker Default Method Tests")
class LinkerTest {

  /**
   * Creates a stub Linker that records defineHostFunction calls and can track async wrapping
   * behavior.
   */
  private TestLinker createTestLinker() {
    return new TestLinker();
  }

  /** Minimal Linker implementation for testing default methods. */
  static class TestLinker implements Linker<Object> {
    final AtomicReference<HostFunction> lastDefinedFunction = new AtomicReference<>();
    String lastModuleName;
    String lastName;

    @Override
    public void defineHostFunction(
        String moduleName, String name, FunctionType functionType, HostFunction implementation)
        throws WasmException {
      lastModuleName = moduleName;
      lastName = name;
      lastDefinedFunction.set(implementation);
    }

    @Override
    public void defineMemory(Store store, String moduleName, String name, WasmMemory memory) {}

    @Override
    public void defineTable(Store store, String moduleName, String name, WasmTable table) {}

    @Override
    public void defineGlobal(Store store, String moduleName, String name, WasmGlobal global) {}

    @Override
    public void defineInstance(Store store, String moduleName, Instance instance) {}

    @Override
    public void define(Store store, String moduleName, String name, Extern extern) {}

    @Override
    public void module(Store store, String moduleName, Module module) {}

    @Override
    public void alias(String fromModule, String fromName, String toModule, String toName) {}

    @Override
    public void aliasModule(String module, String asModule) {}

    @Override
    public Instance instantiate(Store store, Module module) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Instance instantiate(Store store, String moduleName, Module module) {
      throw new UnsupportedOperationException();
    }

    @Override
    public InstancePre instantiatePre(Module module) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void enableWasi() {}

    @Override
    public Engine getEngine() {
      return null;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean hasImport(String moduleName, String name) {
      return false;
    }

    @Override
    public ImportValidation validateImports(Module... modules) {
      return null;
    }

    @Override
    public List<ImportInfo> getImportRegistry() {
      return Collections.emptyList();
    }

    @Override
    public Linker<Object> allowShadowing(boolean allow) {
      return this;
    }

    @Override
    public Linker<Object> allowUnknownExports(boolean allow) {
      return this;
    }

    @Override
    public void defineUnknownImportsAsTraps(Store store, Module module) {}

    @Override
    public void defineUnknownImportsAsDefaultValues(Store store, Module module) {}

    @Override
    public void funcNewUnchecked(
        Store store,
        String moduleName,
        String name,
        FunctionType functionType,
        HostFunction implementation) {}

    @Override
    public Iterable<Linker.LinkerDefinition> iter() {
      return Collections.emptyList();
    }

    @Override
    public Iterable<Linker.LinkerDefinition> iter(Store store) {
      return Collections.emptyList();
    }

    @Override
    public Extern getByImport(Store store, String moduleName, String name) {
      return null;
    }

    @Override
    public WasmFunction getDefault(Store store, String moduleName) {
      return null;
    }

    @Override
    public void defineName(Store store, String name, Extern extern) {}

    @Override
    public void close() {}
  }

  @Nested
  @DisplayName("defineHostFunctionAsync() Default Method")
  class DefineHostFunctionAsyncTests {

    @Test
    @DisplayName("should wrap async function as synchronous call")
    void shouldWrapAsyncAsSynchronous() throws WasmException {
      TestLinker linker = createTestLinker();
      FunctionType ft =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      HostFunctionAsync asyncImpl =
          (caller, params) ->
              CompletableFuture.completedFuture(new WasmValue[] {WasmValue.i32(42)});

      linker.defineHostFunctionAsync("env", "getAnswer", ft, asyncImpl);

      assertNotNull(linker.lastDefinedFunction.get());
      WasmValue[] result = linker.lastDefinedFunction.get().execute(new WasmValue[0]);
      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("defineName() Convenience Default Method")
  class DefineNameTests {

    @Test
    @DisplayName("should delegate to defineHostFunction with empty module name")
    void shouldDelegateWithEmptyModuleName() throws WasmException {
      TestLinker linker = createTestLinker();
      FunctionType ft = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      HostFunction impl = params -> new WasmValue[0];

      linker.defineName("myFunc", ft, impl);

      // The default implementation passes "" as module name
      assertTrue("".equals(linker.lastModuleName));
      assertTrue("myFunc".equals(linker.lastName));
    }
  }

  @Nested
  @DisplayName("getByImport(Store, ImportType) Default Method")
  class GetByImportTypeTests {

    @Test
    @DisplayName("should throw for null importType")
    void shouldThrowForNullImportType() {
      TestLinker linker = createTestLinker();
      assertThrows(
          IllegalArgumentException.class, () -> linker.getByImport(null, (ImportType) null));
    }

    @Test
    @DisplayName("should return empty for missing definition")
    void shouldReturnEmptyForMissing() {
      TestLinker linker = createTestLinker();
      FunctionType ft = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      ImportType importType = new ImportType("env", "log", ft);
      Optional<Extern> result = linker.getByImport(null, importType);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getOneByName() Default Method")
  class GetOneByNameTests {

    @Test
    @DisplayName("should return empty for missing definition")
    void shouldReturnEmptyForMissing() {
      TestLinker linker = createTestLinker();
      Optional<Extern> result = linker.getOneByName(null, "env", "missing");
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("LinkerDefinition Inner Class")
  class LinkerDefinitionTests {

    @Test
    @DisplayName("should store module name, name, and type")
    void shouldStoreFields() {
      Linker.LinkerDefinition def = new Linker.LinkerDefinition("env", "log", null);
      assertTrue("env".equals(def.getModuleName()));
      assertTrue("log".equals(def.getName()));
    }
  }
}
