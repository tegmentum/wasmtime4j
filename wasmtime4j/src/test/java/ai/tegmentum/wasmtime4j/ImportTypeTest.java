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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ImportType} class.
 *
 * <p>ImportType represents the type information of a WebAssembly import.
 */
@DisplayName("ImportType Tests")
class ImportTypeTest {

  /** Simple test implementation of WasmType for testing purposes. */
  private static class TestWasmType implements WasmType {
    private final WasmTypeKind kind;

    TestWasmType(final WasmTypeKind kind) {
      this.kind = kind;
    }

    @Override
    public WasmTypeKind getKind() {
      return kind;
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "print", testType);

      assertNotNull(importType, "ImportType instance should not be null");
      assertEquals("env", importType.getModuleName(), "Module name should match");
      assertEquals("print", importType.getName(), "Name should match");
      assertEquals(testType, importType.getType(), "Type should match");
    }

    @Test
    @DisplayName("should allow null module name")
    void shouldAllowNullModuleName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType(null, "func", testType);

      assertNull(importType.getModuleName(), "Null module name should be preserved");
      assertEquals("func", importType.getName(), "Name should match");
    }

    @Test
    @DisplayName("should allow null name")
    void shouldAllowNullName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", null, testType);

      assertEquals("env", importType.getModuleName(), "Module name should match");
      assertNull(importType.getName(), "Null name should be preserved");
    }

    @Test
    @DisplayName("should allow null type")
    void shouldAllowNullType() {
      final ImportType importType = new ImportType("env", "test", null);

      assertEquals("env", importType.getModuleName(), "Module name should match");
      assertEquals("test", importType.getName(), "Name should match");
      assertNull(importType.getType(), "Null type should be preserved");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("getModuleName should return the module name")
    void getModuleNameShouldReturnModuleName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("wasi_snapshot_preview1", "fd_write", testType);

      assertEquals(
          "wasi_snapshot_preview1",
          importType.getModuleName(),
          "getModuleName should return the module name");
    }

    @Test
    @DisplayName("getName should return the import name")
    void getNameShouldReturnImportName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ImportType importType = new ImportType("env", "memory", testType);

      assertEquals("memory", importType.getName(), "getName should return the import name");
    }

    @Test
    @DisplayName("getType should return the import type")
    void getTypeShouldReturnImportType() {
      final WasmType testType = new TestWasmType(WasmTypeKind.TABLE);
      final ImportType importType = new ImportType("env", "table", testType);

      assertEquals(testType, importType.getType(), "getType should return the import type");
      assertEquals(WasmTypeKind.TABLE, importType.getType().getKind(), "Type kind should match");
    }

    @Test
    @DisplayName("should handle all WasmTypeKind values")
    void shouldHandleAllWasmTypeKindValues() {
      for (final WasmTypeKind kind : WasmTypeKind.values()) {
        final WasmType testType = new TestWasmType(kind);
        final ImportType importType = new ImportType("module", "test_" + kind.name(), testType);

        assertEquals(
            kind, importType.getType().getKind(), "Should handle WasmTypeKind." + kind.name());
      }
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string representation")
    void toStringShouldReturnFormattedString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "add", testType);

      final String result = importType.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("ImportType"), "toString should contain class name");
      assertTrue(result.contains("env"), "toString should contain module name");
      assertTrue(result.contains("add"), "toString should contain import name");
    }

    @Test
    @DisplayName("toString should include module, name, and type information")
    void toStringShouldIncludeAllInformation() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("wasi", "clock_time_get", testType);

      final String result = importType.toString();

      assertTrue(result.contains("wasi"), "toString should include module name");
      assertTrue(result.contains("clock_time_get"), "toString should include import name");
      assertTrue(result.contains("type="), "toString should include type information");
    }

    @Test
    @DisplayName("toString should handle null values")
    void toStringShouldHandleNullValues() {
      final ImportType importType = new ImportType(null, null, null);

      final String result = importType.toString();

      assertNotNull(result, "toString should not throw on null values");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty module name string")
    void shouldHandleEmptyModuleNameString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("", "func", testType);

      assertEquals("", importType.getModuleName(), "Empty module name should be preserved");
    }

    @Test
    @DisplayName("should handle empty name string")
    void shouldHandleEmptyNameString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "", testType);

      assertEquals("", importType.getName(), "Empty name should be preserved");
    }

    @Test
    @DisplayName("should handle special characters in module name")
    void shouldHandleSpecialCharactersInModuleName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType =
          new ImportType("module$with_special-chars.123", "func", testType);

      assertEquals(
          "module$with_special-chars.123",
          importType.getModuleName(),
          "Special characters in module name should be preserved");
    }

    @Test
    @DisplayName("should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func$with_special-chars.123", testType);

      assertEquals(
          "func$with_special-chars.123",
          importType.getName(),
          "Special characters in name should be preserved");
    }

    @Test
    @DisplayName("should handle unicode characters in module name")
    void shouldHandleUnicodeCharactersInModuleName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("モジュール", "関数", testType);

      assertEquals("モジュール", importType.getModuleName(), "Unicode module name should be preserved");
    }

    @Test
    @DisplayName("should handle unicode characters in name")
    void shouldHandleUnicodeCharactersInName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "インポート関数", testType);

      assertEquals("インポート関数", importType.getName(), "Unicode name should be preserved");
    }

    @Test
    @DisplayName("should handle very long module names")
    void shouldHandleVeryLongModuleNames() {
      final String longName = "m".repeat(10000);
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType(longName, "func", testType);

      assertEquals(longName, importType.getModuleName(), "Long module names should be preserved");
    }

    @Test
    @DisplayName("should handle very long names")
    void shouldHandleVeryLongNames() {
      final String longName = "f".repeat(10000);
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", longName, testType);

      assertEquals(longName, importType.getName(), "Long names should be preserved");
    }

    @Test
    @DisplayName("should handle names with whitespace")
    void shouldHandleNamesWithWhitespace() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env module", "import func", testType);

      assertEquals(
          "env module", importType.getModuleName(), "Module names with spaces should be preserved");
      assertEquals("import func", importType.getName(), "Names with spaces should be preserved");
    }
  }

  @Nested
  @DisplayName("WasmTypeKind Integration Tests")
  class WasmTypeKindIntegrationTests {

    @Test
    @DisplayName("should work with FUNCTION type kind")
    void shouldWorkWithFunctionTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func_import", testType);

      assertEquals(
          WasmTypeKind.FUNCTION, importType.getType().getKind(), "Should work with FUNCTION");
    }

    @Test
    @DisplayName("should work with GLOBAL type kind")
    void shouldWorkWithGlobalTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.GLOBAL);
      final ImportType importType = new ImportType("env", "global_import", testType);

      assertEquals(WasmTypeKind.GLOBAL, importType.getType().getKind(), "Should work with GLOBAL");
    }

    @Test
    @DisplayName("should work with MEMORY type kind")
    void shouldWorkWithMemoryTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ImportType importType = new ImportType("env", "memory", testType);

      assertEquals(WasmTypeKind.MEMORY, importType.getType().getKind(), "Should work with MEMORY");
    }

    @Test
    @DisplayName("should work with TABLE type kind")
    void shouldWorkWithTableTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.TABLE);
      final ImportType importType = new ImportType("env", "table", testType);

      assertEquals(WasmTypeKind.TABLE, importType.getType().getKind(), "Should work with TABLE");
    }
  }

  @Nested
  @DisplayName("Common WASI Import Pattern Tests")
  class WasiImportPatternTests {

    @Test
    @DisplayName("should work with wasi_snapshot_preview1 fd_write")
    void shouldWorkWithFdWrite() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("wasi_snapshot_preview1", "fd_write", testType);

      assertEquals(
          "wasi_snapshot_preview1", importType.getModuleName(), "Module name should match");
      assertEquals("fd_write", importType.getName(), "Name should match");
    }

    @Test
    @DisplayName("should work with wasi_snapshot_preview1 fd_read")
    void shouldWorkWithFdRead() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("wasi_snapshot_preview1", "fd_read", testType);

      assertEquals(
          "wasi_snapshot_preview1", importType.getModuleName(), "Module name should match");
      assertEquals("fd_read", importType.getName(), "Name should match");
    }

    @Test
    @DisplayName("should work with wasi_snapshot_preview1 clock_time_get")
    void shouldWorkWithClockTimeGet() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType =
          new ImportType("wasi_snapshot_preview1", "clock_time_get", testType);

      assertEquals(
          "wasi_snapshot_preview1", importType.getModuleName(), "Module name should match");
      assertEquals("clock_time_get", importType.getName(), "Name should match");
    }

    @Test
    @DisplayName("should work with env memory import")
    void shouldWorkWithEnvMemory() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ImportType importType = new ImportType("env", "memory", testType);

      assertEquals("env", importType.getModuleName(), "Module name should match");
      assertEquals("memory", importType.getName(), "Name should match");
      assertEquals(WasmTypeKind.MEMORY, importType.getType().getKind(), "Type should be MEMORY");
    }

    @Test
    @DisplayName("should work with env table import")
    void shouldWorkWithEnvTable() {
      final WasmType testType = new TestWasmType(WasmTypeKind.TABLE);
      final ImportType importType = new ImportType("env", "__indirect_function_table", testType);

      assertEquals("env", importType.getModuleName(), "Module name should match");
      assertEquals("__indirect_function_table", importType.getName(), "Name should match");
      assertEquals(WasmTypeKind.TABLE, importType.getType().getKind(), "Type should be TABLE");
    }
  }
}
