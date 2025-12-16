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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.ImportType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniModule}.
 *
 * <p>Tests focus on Java wrapper logic, parameter validation, and defensive programming. Tests
 * verify constructor behavior, resource management, and basic API functionality without requiring
 * actual native library loading.
 *
 * <p>Note: Integration tests with actual WebAssembly modules are in wasmtime4j-tests.
 */
@DisplayName("JniModule Tests")
class JniModuleTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long ZERO_HANDLE = 0L;

  private JniEngine testEngine;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create module with valid handle and engine")
    void shouldCreateModuleWithValidHandleAndEngine() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      assertNotNull(module, "Module should not be null");
      assertEquals(VALID_HANDLE, module.getNativeHandle(), "Native handle should match");
      assertEquals(testEngine, module.getEngine(), "Engine should match");
    }

    @Test
    @DisplayName("should create module with null engine")
    void shouldCreateModuleWithNullEngine() {
      final JniModule module = new JniModule(VALID_HANDLE, null);

      assertNotNull(module, "Module should not be null");
      assertEquals(null, module.getEngine(), "Engine should be null");
    }

    @Test
    @DisplayName("should create module with zero handle")
    void shouldCreateModuleWithZeroHandle() {
      final JniModule module = new JniModule(ZERO_HANDLE, testEngine);

      assertNotNull(module, "Module should not be null");
      assertEquals(ZERO_HANDLE, module.getNativeHandle(), "Native handle should be zero");
    }
  }

  @Nested
  @DisplayName("GetNativeHandle Tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("should return correct native handle")
    void shouldReturnCorrectNativeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      assertEquals(VALID_HANDLE, module.getNativeHandle(), "Should return correct handle");
    }
  }

  @Nested
  @DisplayName("GetEngine Tests")
  class GetEngineTests {

    @Test
    @DisplayName("should return correct engine")
    void shouldReturnCorrectEngine() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      assertEquals(testEngine, module.getEngine(), "Should return correct engine");
    }
  }

  @Nested
  @DisplayName("GetName Tests")
  class GetNameTests {

    @Test
    @DisplayName("should return name with handle")
    void shouldReturnNameWithHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final String name = module.getName();

      assertNotNull(name, "Name should not be null");
      assertTrue(name.contains("jni-module"), "Name should contain 'jni-module'");
      assertTrue(name.contains(String.valueOf(VALID_HANDLE)), "Name should contain handle");
    }
  }

  // Note: Close tests are covered by integration tests in wasmtime4j-tests module
  // because calling close() on objects with fake handles triggers native destructor
  // which crashes the JVM when trying to free memory at an invalid address

  @Nested
  @DisplayName("Instantiate Tests")
  class InstantiateTests {

    @Test
    @DisplayName("should throw on null store")
    void shouldThrowOnNullStore() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      assertThrows(
          IllegalArgumentException.class,
          () -> module.instantiate(null),
          "Should throw on null store");
    }

    // Note: Test for closed module instantiate is covered by integration tests
    // because calling close() on fake handles crashes the JVM

    @Test
    @DisplayName("should throw on null imports map")
    void shouldThrowOnNullImportsMap() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);
      final JniStore store = new JniStore(VALID_HANDLE, testEngine);

      assertThrows(
          IllegalArgumentException.class,
          () -> module.instantiate(store, null),
          "Should throw on null imports");
    }
  }

  @Nested
  @DisplayName("HasExport Tests")
  class HasExportTests {

    @Test
    @DisplayName("should throw on null export name")
    void shouldThrowOnNullExportName() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      assertThrows(
          IllegalArgumentException.class,
          () -> module.hasExport(null),
          "Should throw on null export name");
    }

    @Test
    @DisplayName("should return false for fake handle")
    void shouldReturnFalseForFakeHandle() {
      // Using a fake/test handle that won't be a real native pointer
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      // Fake handles return false to prevent crashes
      assertFalse(module.hasExport("some_export"), "Should return false for fake handle");
    }

    // Note: Test for closed module hasExport is covered by integration tests
    // because calling close() on fake handles crashes the JVM
  }

  @Nested
  @DisplayName("HasImport Tests")
  class HasImportTests {

    @Test
    @DisplayName("should throw on null module name")
    void shouldThrowOnNullModuleName() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      assertThrows(
          IllegalArgumentException.class,
          () -> module.hasImport(null, "field"),
          "Should throw on null module name");
    }

    @Test
    @DisplayName("should throw on null field name")
    void shouldThrowOnNullFieldName() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      assertThrows(
          IllegalArgumentException.class,
          () -> module.hasImport("module", null),
          "Should throw on null field name");
    }

    @Test
    @DisplayName("should return false for fake handle")
    void shouldReturnFalseForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      // Fake handles return false to prevent crashes
      assertFalse(module.hasImport("env", "memory"), "Should return false for fake handle");
    }
  }

  @Nested
  @DisplayName("ValidateImports Tests")
  class ValidateImportsTests {

    @Test
    @DisplayName("should throw on null imports")
    void shouldThrowOnNullImports() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      assertThrows(
          IllegalArgumentException.class,
          () -> module.validateImports(null),
          "Should throw on null imports");
    }

    // Note: Testing closed module with validateImports would require a valid ImportMap,
    // which depends on runtime implementations (JniImportMap or PanamaImportMap).
    // This test is covered by integration tests in wasmtime4j-tests module.
  }

  @Nested
  @DisplayName("GetModuleImports Tests")
  class GetModuleImportsTests {

    @Test
    @DisplayName("should return empty list for fake handle")
    void shouldReturnEmptyListForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      // Fake handles return empty to prevent crashes
      final List<ai.tegmentum.wasmtime4j.ModuleImport> imports = module.getModuleImports();

      assertNotNull(imports, "Imports should not be null");
      assertTrue(imports.isEmpty(), "Should return empty list for fake handle");
    }

    // Note: Test for closed module getModuleImports is covered by integration tests
    // because calling close() on fake handles crashes the JVM
  }

  @Nested
  @DisplayName("GetModuleExports Tests")
  class GetModuleExportsTests {

    @Test
    @DisplayName("should return empty list for fake handle")
    void shouldReturnEmptyListForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      // Fake handles return empty to prevent crashes
      final List<ai.tegmentum.wasmtime4j.ModuleExport> exports = module.getModuleExports();

      assertNotNull(exports, "Exports should not be null");
      assertTrue(exports.isEmpty(), "Should return empty list for fake handle");
    }
  }

  @Nested
  @DisplayName("GetImports Tests")
  class GetImportsTests {

    @Test
    @DisplayName("should return list of import types")
    void shouldReturnListOfImportTypes() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final List<ImportType> imports = module.getImports();

      assertNotNull(imports, "Imports should not be null");
      // Empty for fake handle
      assertTrue(imports.isEmpty(), "Should be empty for fake handle");
    }
  }

  @Nested
  @DisplayName("GetExports Tests")
  class GetExportsTests {

    @Test
    @DisplayName("should return list of export types")
    void shouldReturnListOfExportTypes() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final List<ExportType> exports = module.getExports();

      assertNotNull(exports, "Exports should not be null");
      // Empty for fake handle
      assertTrue(exports.isEmpty(), "Should be empty for fake handle");
    }
  }

  @Nested
  @DisplayName("GetCustomSections Tests")
  class GetCustomSectionsTests {

    @Test
    @DisplayName("should return empty map for fake handle")
    void shouldReturnEmptyMapForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final Map<String, byte[]> sections = module.getCustomSections();

      assertNotNull(sections, "Sections should not be null");
      assertTrue(sections.isEmpty(), "Should return empty map for fake handle");
    }

    // Note: Test for closed module getCustomSections is covered by integration tests
    // because calling close() on fake handles crashes the JVM
  }

  @Nested
  @DisplayName("GetFunctionType Tests")
  class GetFunctionTypeTests {

    @Test
    @DisplayName("should return empty optional for null function name")
    void shouldReturnEmptyOptionalForNullFunctionName() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final Optional<ai.tegmentum.wasmtime4j.FuncType> funcType = module.getFunctionType(null);

      assertNotNull(funcType, "Optional should not be null");
      assertFalse(funcType.isPresent(), "Should return empty optional for null name");
    }

    @Test
    @DisplayName("should return empty optional for non-existent function")
    void shouldReturnEmptyOptionalForNonExistentFunction() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final Optional<ai.tegmentum.wasmtime4j.FuncType> funcType =
          module.getFunctionType("non_existent");

      assertNotNull(funcType, "Optional should not be null");
      assertFalse(funcType.isPresent(), "Should return empty optional for non-existent function");
    }
  }

  @Nested
  @DisplayName("GetGlobalType Tests")
  class GetGlobalTypeTests {

    @Test
    @DisplayName("should return empty optional for null global name")
    void shouldReturnEmptyOptionalForNullGlobalName() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final Optional<ai.tegmentum.wasmtime4j.GlobalType> globalType = module.getGlobalType(null);

      assertNotNull(globalType, "Optional should not be null");
      assertFalse(globalType.isPresent(), "Should return empty optional for null name");
    }
  }

  @Nested
  @DisplayName("GetMemoryType Tests")
  class GetMemoryTypeTests {

    @Test
    @DisplayName("should return empty optional for null memory name")
    void shouldReturnEmptyOptionalForNullMemoryName() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final Optional<ai.tegmentum.wasmtime4j.MemoryType> memoryType = module.getMemoryType(null);

      assertNotNull(memoryType, "Optional should not be null");
      assertFalse(memoryType.isPresent(), "Should return empty optional for null name");
    }
  }

  @Nested
  @DisplayName("GetTableType Tests")
  class GetTableTypeTests {

    @Test
    @DisplayName("should return empty optional for null table name")
    void shouldReturnEmptyOptionalForNullTableName() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final Optional<ai.tegmentum.wasmtime4j.TableType> tableType = module.getTableType(null);

      assertNotNull(tableType, "Optional should not be null");
      assertFalse(tableType.isPresent(), "Should return empty optional for null name");
    }
  }

  @Nested
  @DisplayName("Type Lists Tests")
  class TypeListsTests {

    @Test
    @DisplayName("should return empty global types for fake handle")
    void shouldReturnEmptyGlobalTypesForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final List<ai.tegmentum.wasmtime4j.GlobalType> types = module.getGlobalTypes();

      assertNotNull(types, "Types should not be null");
      assertTrue(types.isEmpty(), "Should be empty for fake handle");
    }

    @Test
    @DisplayName("should return empty table types for fake handle")
    void shouldReturnEmptyTableTypesForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final List<ai.tegmentum.wasmtime4j.TableType> types = module.getTableTypes();

      assertNotNull(types, "Types should not be null");
      assertTrue(types.isEmpty(), "Should be empty for fake handle");
    }

    @Test
    @DisplayName("should return empty memory types for fake handle")
    void shouldReturnEmptyMemoryTypesForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final List<ai.tegmentum.wasmtime4j.MemoryType> types = module.getMemoryTypes();

      assertNotNull(types, "Types should not be null");
      assertTrue(types.isEmpty(), "Should be empty for fake handle");
    }

    @Test
    @DisplayName("should return empty function types for fake handle")
    void shouldReturnEmptyFunctionTypesForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final List<ai.tegmentum.wasmtime4j.FuncType> types = module.getFunctionTypes();

      assertNotNull(types, "Types should not be null");
      assertTrue(types.isEmpty(), "Should be empty for fake handle");
    }
  }

  @Nested
  @DisplayName("Descriptor Tests")
  class DescriptorTests {

    @Test
    @DisplayName("should return empty export descriptors for fake handle")
    void shouldReturnEmptyExportDescriptorsForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final List<ai.tegmentum.wasmtime4j.ExportDescriptor> descriptors =
          module.getExportDescriptors();

      assertNotNull(descriptors, "Descriptors should not be null");
      assertTrue(descriptors.isEmpty(), "Should be empty for fake handle");
    }

    @Test
    @DisplayName("should return empty import descriptors for fake handle")
    void shouldReturnEmptyImportDescriptorsForFakeHandle() {
      final JniModule module = new JniModule(VALID_HANDLE, testEngine);

      final List<ai.tegmentum.wasmtime4j.ImportDescriptor> descriptors =
          module.getImportDescriptors();

      assertNotNull(descriptors, "Descriptors should not be null");
      assertTrue(descriptors.isEmpty(), "Should be empty for fake handle");
    }
  }
}
