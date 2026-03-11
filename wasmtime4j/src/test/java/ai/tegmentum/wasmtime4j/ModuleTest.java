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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.Mutability;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.validation.ModuleValidationResult;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Module} default and static methods.
 *
 * <p>These tests use anonymous implementations to exercise default method behavior without
 * requiring a native runtime.
 */
@DisplayName("Module Default Method Tests")
class ModuleTest {

  /** Creates a stub module with the given exports and imports. */
  private Module createStubModule(final List<ExportType> exports, final List<ImportType> imports) {
    return new Module() {
      @Override
      public Instance instantiate(Store store) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Instance instantiate(Store store, ai.tegmentum.wasmtime4j.validation.ImportMap imp) {
        throw new UnsupportedOperationException();
      }

      @Override
      public List<ExportType> getExports() {
        return Collections.unmodifiableList(exports);
      }

      @Override
      public List<ImportType> getImports() {
        return Collections.unmodifiableList(imports);
      }

      @Override
      public boolean hasExport(String name) {
        return exports.stream().anyMatch(e -> e.getName().equals(name));
      }

      @Override
      public boolean hasImport(String moduleName, String fieldName) {
        return imports.stream()
            .anyMatch(i -> i.getModuleName().equals(moduleName) && i.getName().equals(fieldName));
      }

      @Override
      public Engine getEngine() {
        return null;
      }

      @Override
      public boolean validateImports(ai.tegmentum.wasmtime4j.validation.ImportMap imp) {
        return true;
      }

      @Override
      public ai.tegmentum.wasmtime4j.validation.ImportValidation validateImportsDetailed(
          ai.tegmentum.wasmtime4j.validation.ImportMap imp) {
        return null;
      }

      @Override
      public String getName() {
        return "test-module";
      }

      @Override
      public byte[] text() {
        return new byte[0];
      }

      @Override
      public List<AddressMapping> addressMap() {
        return Collections.emptyList();
      }

      @Override
      public ImageRange imageRange() {
        return null;
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public void close() {}

      @Override
      public boolean same(Module other) {
        return this == other;
      }

      @Override
      public int getExportIndex(String name) {
        for (int i = 0; i < exports.size(); i++) {
          if (exports.get(i).getName().equals(name)) {
            return i;
          }
        }
        return -1;
      }

      @Override
      public Optional<ModuleExport> getModuleExport(String name) {
        return Optional.empty();
      }

      @Override
      public byte[] serialize() {
        return new byte[0];
      }
    };
  }

  private FunctionType funcType(WasmValueType[] params, WasmValueType[] results) {
    return new FunctionType(params, results);
  }

  private MemoryType memoryType(long min, long max) {
    return new MemoryType() {
      @Override
      public long getMinimum() {
        return min;
      }

      @Override
      public Optional<Long> getMaximum() {
        return max < 0 ? Optional.empty() : Optional.of(max);
      }

      @Override
      public boolean is64Bit() {
        return false;
      }

      @Override
      public boolean isShared() {
        return false;
      }

      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.MEMORY;
      }
    };
  }

  private TableType tableType(long min, long max) {
    return new TableType() {
      @Override
      public WasmValueType getElementType() {
        return WasmValueType.FUNCREF;
      }

      @Override
      public long getMinimum() {
        return min;
      }

      @Override
      public Optional<Long> getMaximum() {
        return max < 0 ? Optional.empty() : Optional.of(max);
      }

      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.TABLE;
      }
    };
  }

  private GlobalType globalType(WasmValueType valType) {
    return GlobalType.of(valType, Mutability.CONST);
  }

  @Nested
  @DisplayName("getExport() Default Method")
  class GetExportTests {

    @Test
    @DisplayName("should find export by name")
    void shouldFindExportByName() {
      FunctionType ft = funcType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
      List<ExportType> exports = List.of(new ExportType("myFunc", ft));
      Module mod = createStubModule(exports, Collections.emptyList());

      Optional<ExportType> result = mod.getExport("myFunc");
      assertTrue(result.isPresent());
      assertEquals("myFunc", result.get().getName());
    }

    @Test
    @DisplayName("should return empty for unknown export")
    void shouldReturnEmptyForUnknownExport() {
      Module mod = createStubModule(Collections.emptyList(), Collections.emptyList());
      assertFalse(mod.getExport("missing").isPresent());
    }

    @Test
    @DisplayName("should throw for null name")
    void shouldThrowForNullName() {
      Module mod = createStubModule(Collections.emptyList(), Collections.emptyList());
      assertThrows(IllegalArgumentException.class, () -> mod.getExport(null));
    }
  }

  @Nested
  @DisplayName("getFunctionType() Default Method")
  class GetFunctionTypeTests {

    @Test
    @DisplayName("should return function type for function export")
    void shouldReturnFunctionType() {
      FunctionType ft =
          funcType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      List<ExportType> exports = List.of(new ExportType("add", ft));
      Module mod = createStubModule(exports, Collections.emptyList());

      Optional<FuncType> result = mod.getFunctionType("add");
      assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("should return empty for non-function export")
    void shouldReturnEmptyForNonFunction() {
      List<ExportType> exports = List.of(new ExportType("mem", memoryType(1, 10)));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertFalse(mod.getFunctionType("mem").isPresent());
    }

    @Test
    @DisplayName("should return empty for null name")
    void shouldReturnEmptyForNullName() {
      Module mod = createStubModule(Collections.emptyList(), Collections.emptyList());
      assertFalse(mod.getFunctionType(null).isPresent());
    }

    @Test
    @DisplayName("should return empty for missing export")
    void shouldReturnEmptyForMissing() {
      Module mod = createStubModule(Collections.emptyList(), Collections.emptyList());
      assertFalse(mod.getFunctionType("missing").isPresent());
    }
  }

  @Nested
  @DisplayName("getGlobalType() Default Method")
  class GetGlobalTypeTests {

    @Test
    @DisplayName("should return global type for global export")
    void shouldReturnGlobalType() {
      GlobalType gt = globalType(WasmValueType.I32);
      List<ExportType> exports = List.of(new ExportType("g1", gt));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertTrue(mod.getGlobalType("g1").isPresent());
    }

    @Test
    @DisplayName("should return empty for null name")
    void shouldReturnEmptyForNullName() {
      Module mod = createStubModule(Collections.emptyList(), Collections.emptyList());
      assertFalse(mod.getGlobalType(null).isPresent());
    }
  }

  @Nested
  @DisplayName("getMemoryType() Default Method")
  class GetMemoryTypeTests {

    @Test
    @DisplayName("should return memory type for memory export")
    void shouldReturnMemoryType() {
      List<ExportType> exports = List.of(new ExportType("memory", memoryType(1, 10)));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertTrue(mod.getMemoryType("memory").isPresent());
    }

    @Test
    @DisplayName("should return empty for null name")
    void shouldReturnEmptyForNullName() {
      Module mod = createStubModule(Collections.emptyList(), Collections.emptyList());
      assertFalse(mod.getMemoryType(null).isPresent());
    }
  }

  @Nested
  @DisplayName("getTableType() Default Method")
  class GetTableTypeTests {

    @Test
    @DisplayName("should return table type for table export")
    void shouldReturnTableType() {
      List<ExportType> exports = List.of(new ExportType("tbl", tableType(10, 100)));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertTrue(mod.getTableType("tbl").isPresent());
    }

    @Test
    @DisplayName("should return empty for null name")
    void shouldReturnEmptyForNullName() {
      Module mod = createStubModule(Collections.emptyList(), Collections.emptyList());
      assertFalse(mod.getTableType(null).isPresent());
    }
  }

  @Nested
  @DisplayName("getXxxTypes() Filtering Default Methods")
  class TypeFilteringTests {

    @Test
    @DisplayName("getFunctionTypes() should filter function exports")
    void getFunctionTypesShouldFilterFunctions() {
      FunctionType ft = funcType(new WasmValueType[0], new WasmValueType[0]);
      List<ExportType> exports =
          List.of(
              new ExportType("f1", ft),
              new ExportType("mem", memoryType(1, 10)),
              new ExportType("f2", ft));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertEquals(2, mod.getFunctionTypes().size());
    }

    @Test
    @DisplayName("getMemoryTypes() should filter memory exports")
    void getMemoryTypesShouldFilterMemories() {
      FunctionType ft = funcType(new WasmValueType[0], new WasmValueType[0]);
      List<ExportType> exports =
          List.of(new ExportType("f1", ft), new ExportType("mem", memoryType(1, 10)));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertEquals(1, mod.getMemoryTypes().size());
    }

    @Test
    @DisplayName("getTableTypes() should filter table exports")
    void getTableTypesShouldFilterTables() {
      List<ExportType> exports = List.of(new ExportType("tbl", tableType(5, 50)));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertEquals(1, mod.getTableTypes().size());
    }

    @Test
    @DisplayName("getGlobalTypes() should filter global exports")
    void getGlobalTypesShouldFilterGlobals() {
      GlobalType gt = globalType(WasmValueType.I64);
      List<ExportType> exports = List.of(new ExportType("g1", gt), new ExportType("g2", gt));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertEquals(2, mod.getGlobalTypes().size());
    }

    @Test
    @DisplayName("type lists should be immutable")
    void typeListsShouldBeImmutable() {
      FunctionType ft = funcType(new WasmValueType[0], new WasmValueType[0]);
      List<ExportType> exports = List.of(new ExportType("f1", ft));
      Module mod = createStubModule(exports, Collections.emptyList());

      assertThrows(UnsupportedOperationException.class, () -> mod.getFunctionTypes().add(null));
      assertThrows(UnsupportedOperationException.class, () -> mod.getMemoryTypes().add(null));
      assertThrows(UnsupportedOperationException.class, () -> mod.getTableTypes().add(null));
      assertThrows(UnsupportedOperationException.class, () -> mod.getGlobalTypes().add(null));
    }
  }

  @Nested
  @DisplayName("functions() Default Method")
  class FunctionsTests {

    @Test
    @DisplayName("should include imported functions")
    void shouldIncludeImportedFunctions() {
      FunctionType ft = funcType(new WasmValueType[0], new WasmValueType[0]);
      List<ImportType> imports = List.of(new ImportType("env", "log", ft));
      Module mod = createStubModule(Collections.emptyList(), imports);

      int count = 0;
      for (ai.tegmentum.wasmtime4j.func.FunctionInfo fi : mod.functions()) {
        count++;
        assertEquals("log", fi.getName());
        assertTrue(fi.isImport());
      }
      assertEquals(1, count);
    }

    @Test
    @DisplayName("should include exported functions")
    void shouldIncludeExportedFunctions() {
      FunctionType ft =
          funcType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      List<ExportType> exports = List.of(new ExportType("compute", ft));
      Module mod = createStubModule(exports, Collections.emptyList());

      int count = 0;
      for (ai.tegmentum.wasmtime4j.func.FunctionInfo fi : mod.functions()) {
        count++;
        assertEquals("compute", fi.getName());
        assertFalse(fi.isImport());
      }
      assertEquals(1, count);
    }

    @Test
    @DisplayName("should skip non-function imports and exports")
    void shouldSkipNonFunctions() {
      List<ImportType> imports = List.of(new ImportType("env", "mem", memoryType(1, 10)));
      List<ExportType> exports = List.of(new ExportType("tbl", tableType(5, 50)));
      Module mod = createStubModule(exports, imports);

      int count = 0;
      for (ai.tegmentum.wasmtime4j.func.FunctionInfo ignored : mod.functions()) {
        count++;
      }
      assertEquals(0, count);
    }
  }

  @Nested
  @DisplayName("validate() Static Method")
  class ValidateTests {

    @Test
    @DisplayName("should throw for null engine")
    void shouldThrowForNullEngine() {
      assertThrows(IllegalArgumentException.class, () -> Module.validate(null, new byte[8]));
    }

    @Test
    @DisplayName("should throw for null bytes")
    void shouldThrowForNullBytes() {
      assertThrows(IllegalArgumentException.class, () -> Module.validate(createMockEngine(), null));
    }

    @Test
    @DisplayName("should fail for empty bytes")
    void shouldFailForEmptyBytes() {
      ModuleValidationResult result = Module.validate(createMockEngine(), new byte[0]);
      assertFalse(result.isValid());
      assertTrue(result.getErrors().get(0).contains("empty"));
    }

    @Test
    @DisplayName("should fail for bytes shorter than 8")
    void shouldFailForShortBytes() {
      ModuleValidationResult result = Module.validate(createMockEngine(), new byte[4]);
      assertFalse(result.isValid());
      assertTrue(result.getErrors().get(0).contains("too short"));
    }

    @Test
    @DisplayName("should fail for invalid magic number")
    void shouldFailForInvalidMagic() {
      byte[] bytes = new byte[] {0x01, 0x02, 0x03, 0x04, 0x01, 0x00, 0x00, 0x00};
      ModuleValidationResult result = Module.validate(createMockEngine(), bytes);
      assertFalse(result.isValid());
      assertTrue(result.getErrors().get(0).contains("magic number"));
    }

    @Test
    @DisplayName("should fail for invalid version")
    void shouldFailForInvalidVersion() {
      byte[] bytes = new byte[] {0x00, 0x61, 0x73, 0x6D, 0x02, 0x00, 0x00, 0x00};
      ModuleValidationResult result = Module.validate(createMockEngine(), bytes);
      assertFalse(result.isValid());
      assertTrue(result.getErrors().get(0).contains("version"));
    }

    /**
     * Creates a minimal Engine mock. The validate static method will call
     * engine.getRuntime().validateModule() only when magic+version pass, so we need an engine that
     * does not throw for the null-check but would throw at runtime level (which we do not reach in
     * our header-failing tests).
     */
    private Engine createMockEngine() {
      return org.mockito.Mockito.mock(Engine.class);
    }
  }

  @Nested
  @DisplayName("fromBinary() Static Method")
  class FromBinaryTests {

    @Test
    @DisplayName("should throw for null engine")
    void shouldThrowForNullEngine() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.fromBinary(null, new byte[] {0x00, 0x61, 0x73, 0x6D}));
    }

    @Test
    @DisplayName("should throw for null bytes")
    void shouldThrowForNullBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.fromBinary(org.mockito.Mockito.mock(Engine.class), null));
    }

    @Test
    @DisplayName("should throw for bytes shorter than 4")
    void shouldThrowForShortBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.fromBinary(org.mockito.Mockito.mock(Engine.class), new byte[3]));
    }

    @Test
    @DisplayName("should throw for invalid magic number")
    void shouldThrowForInvalidMagic() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              Module.fromBinary(
                  org.mockito.Mockito.mock(Engine.class), new byte[] {0x01, 0x02, 0x03, 0x04}));
    }
  }

  @Nested
  @DisplayName("compile() with DWARF Static Method")
  class CompileWithDwarfTests {

    @Test
    @DisplayName("should throw for null engine")
    void shouldThrowForNullEngine() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.compile(null, new byte[] {1}, new byte[] {2}));
    }

    @Test
    @DisplayName("should throw for null wasmBytes")
    void shouldThrowForNullWasmBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.compile(org.mockito.Mockito.mock(Engine.class), null, new byte[] {2}));
    }

    @Test
    @DisplayName("should throw for empty wasmBytes")
    void shouldThrowForEmptyWasmBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              Module.compile(org.mockito.Mockito.mock(Engine.class), new byte[0], new byte[] {2}));
    }

    @Test
    @DisplayName("should throw for null dwarfPackage")
    void shouldThrowForNullDwarfPackage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.compile(org.mockito.Mockito.mock(Engine.class), new byte[] {1}, null));
    }

    @Test
    @DisplayName("should throw for empty dwarfPackage")
    void shouldThrowForEmptyDwarfPackage() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              Module.compile(org.mockito.Mockito.mock(Engine.class), new byte[] {1}, new byte[0]));
    }
  }

  @Nested
  @DisplayName("fromFile() Static Method")
  class FromFileTests {

    @Test
    @DisplayName("should throw for null engine")
    void shouldThrowForNullEngine() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.fromFile(null, java.nio.file.Path.of("test.wasm")));
    }

    @Test
    @DisplayName("should throw for null path")
    void shouldThrowForNullPath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.fromFile(org.mockito.Mockito.mock(Engine.class), null));
    }
  }

  @Nested
  @DisplayName("initializeCopyOnWriteImage() Default Method")
  class InitCowTests {

    @Test
    @DisplayName("should be no-op by default")
    void shouldBeNoOpByDefault() throws Exception {
      Module mod = createStubModule(Collections.emptyList(), Collections.emptyList());
      mod.initializeCopyOnWriteImage(); // should not throw
    }
  }

  @Nested
  @DisplayName("AddressMapping Inner Class")
  class AddressMappingTests {

    @Test
    @DisplayName("should store code offset and wasm offset")
    void shouldStoreOffsets() {
      Module.AddressMapping am = new Module.AddressMapping(100L, java.util.OptionalInt.of(50));
      assertEquals(100L, am.getCodeOffset());
      assertEquals(50, am.getWasmOffset().getAsInt());
    }

    @Test
    @DisplayName("should support empty wasm offset")
    void shouldSupportEmptyWasmOffset() {
      Module.AddressMapping am = new Module.AddressMapping(200L, java.util.OptionalInt.empty());
      assertEquals(200L, am.getCodeOffset());
      assertFalse(am.getWasmOffset().isPresent());
    }

    @Test
    @DisplayName("equals and hashCode should work correctly")
    void equalsAndHashCode() {
      Module.AddressMapping a = new Module.AddressMapping(10L, java.util.OptionalInt.of(5));
      Module.AddressMapping b = new Module.AddressMapping(10L, java.util.OptionalInt.of(5));
      Module.AddressMapping c = new Module.AddressMapping(20L, java.util.OptionalInt.of(5));

      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertFalse(a.equals(c));
    }

    @Test
    @DisplayName("toString should contain offsets")
    void toStringShouldContainOffsets() {
      Module.AddressMapping am = new Module.AddressMapping(10L, java.util.OptionalInt.of(5));
      assertTrue(am.toString().contains("10"));
      assertTrue(am.toString().contains("5"));
    }
  }
}
