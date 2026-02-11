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

import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExportType} class.
 *
 * <p>ExportType represents the type information of a WebAssembly export.
 */
@DisplayName("ExportType Tests")
class ExportTypeTest {

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
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(Modifier.isPublic(ExportType.class.getModifiers()), "ExportType should be public");
      assertTrue(Modifier.isFinal(ExportType.class.getModifiers()), "ExportType should be final");
    }

    @Test
    @DisplayName("should have two-parameter constructor")
    void shouldHaveTwoParameterConstructor() throws NoSuchMethodException {
      final var constructor = ExportType.class.getConstructor(String.class, WasmType.class);
      assertNotNull(constructor, "Two-parameter constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ExportType.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ExportType.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(WasmType.class, method.getReturnType(), "getType should return WasmType");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("myFunc", testType);

      assertNotNull(exportType, "ExportType instance should not be null");
      assertEquals("myFunc", exportType.getName(), "Name should match");
      assertEquals(testType, exportType.getType(), "Type should match");
    }

    @Test
    @DisplayName("should allow null name")
    void shouldAllowNullName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType(null, testType);

      assertNull(exportType.getName(), "Null name should be preserved");
      assertEquals(testType, exportType.getType(), "Type should match");
    }

    @Test
    @DisplayName("should allow null type")
    void shouldAllowNullType() {
      final ExportType exportType = new ExportType("test", null);

      assertEquals("test", exportType.getName(), "Name should match");
      assertNull(exportType.getType(), "Null type should be preserved");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("getName should return the export name")
    void getNameShouldReturnExportName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("calculate", testType);

      assertEquals("calculate", exportType.getName(), "getName should return the export name");
    }

    @Test
    @DisplayName("getType should return the export type")
    void getTypeShouldReturnExportType() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ExportType exportType = new ExportType("memory", testType);

      assertEquals(testType, exportType.getType(), "getType should return the export type");
    }

    @Test
    @DisplayName("should handle all WasmTypeKind values")
    void shouldHandleAllWasmTypeKindValues() {
      for (final WasmTypeKind kind : WasmTypeKind.values()) {
        final WasmType testType = new TestWasmType(kind);
        final ExportType exportType = new ExportType("test_" + kind.name(), testType);

        assertEquals(
            kind, exportType.getType().getKind(), "Should handle WasmTypeKind." + kind.name());
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
      final ExportType exportType = new ExportType("add", testType);

      final String result = exportType.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("ExportType"), "toString should contain class name");
      assertTrue(result.contains("add"), "toString should contain export name");
    }

    @Test
    @DisplayName("toString should include name and type information")
    void toStringShouldIncludeNameAndType() {
      final WasmType testType = new TestWasmType(WasmTypeKind.GLOBAL);
      final ExportType exportType = new ExportType("global_pi", testType);

      final String result = exportType.toString();

      assertTrue(result.contains("global_pi"), "toString should include the export name");
      assertTrue(result.contains("type="), "toString should include type information");
    }

    @Test
    @DisplayName("toString should handle null values")
    void toStringShouldHandleNullValues() {
      final ExportType exportType = new ExportType(null, null);

      final String result = exportType.toString();

      assertNotNull(result, "toString should not throw on null values");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty name string")
    void shouldHandleEmptyNameString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("", testType);

      assertEquals("", exportType.getName(), "Empty name should be preserved");
    }

    @Test
    @DisplayName("should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("export$with_special-chars.123", testType);

      assertEquals(
          "export$with_special-chars.123",
          exportType.getName(),
          "Special characters should be preserved");
    }

    @Test
    @DisplayName("should handle unicode characters in name")
    void shouldHandleUnicodeCharactersInName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("エクスポート関数", testType);

      assertEquals("エクスポート関数", exportType.getName(), "Unicode characters should be preserved");
    }

    @Test
    @DisplayName("should handle very long names")
    void shouldHandleVeryLongNames() {
      final String longName = "a".repeat(10000);
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType(longName, testType);

      assertEquals(longName, exportType.getName(), "Long names should be preserved");
    }

    @Test
    @DisplayName("should handle names with whitespace")
    void shouldHandleNamesWithWhitespace() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("export with spaces", testType);

      assertEquals(
          "export with spaces", exportType.getName(), "Names with spaces should be preserved");
    }

    @Test
    @DisplayName("should handle names with newlines and tabs")
    void shouldHandleNamesWithNewlinesAndTabs() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("export\nwith\ttabs", testType);

      assertEquals(
          "export\nwith\ttabs", exportType.getName(), "Newlines and tabs should be preserved");
    }
  }

  @Nested
  @DisplayName("WasmTypeKind Integration Tests")
  class WasmTypeKindIntegrationTests {

    @Test
    @DisplayName("should work with FUNCTION type kind")
    void shouldWorkWithFunctionTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func_export", testType);

      assertEquals(
          WasmTypeKind.FUNCTION, exportType.getType().getKind(), "Should work with FUNCTION");
    }

    @Test
    @DisplayName("should work with GLOBAL type kind")
    void shouldWorkWithGlobalTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.GLOBAL);
      final ExportType exportType = new ExportType("global_var", testType);

      assertEquals(WasmTypeKind.GLOBAL, exportType.getType().getKind(), "Should work with GLOBAL");
    }

    @Test
    @DisplayName("should work with MEMORY type kind")
    void shouldWorkWithMemoryTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ExportType exportType = new ExportType("main_memory", testType);

      assertEquals(WasmTypeKind.MEMORY, exportType.getType().getKind(), "Should work with MEMORY");
    }

    @Test
    @DisplayName("should work with TABLE type kind")
    void shouldWorkWithTableTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.TABLE);
      final ExportType exportType = new ExportType("func_table", testType);

      assertEquals(WasmTypeKind.TABLE, exportType.getType().getKind(), "Should work with TABLE");
    }
  }
}
