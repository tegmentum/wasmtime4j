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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleExport} class.
 *
 * <p>ModuleExport represents a WebAssembly module export with complete type information.
 */
@DisplayName("ModuleExport Tests")
class ModuleExportTest {

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

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof TestWasmType)) {
        return false;
      }
      final TestWasmType other = (TestWasmType) obj;
      return kind == other.kind;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(kind);
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(ModuleExport.class.getModifiers()), "ModuleExport should be public");
      assertTrue(
          Modifier.isFinal(ModuleExport.class.getModifiers()), "ModuleExport should be final");
    }

    @Test
    @DisplayName("should have two-parameter constructor")
    void shouldHaveTwoParameterConstructor() throws NoSuchMethodException {
      final var constructor = ModuleExport.class.getConstructor(String.class, ExportType.class);
      assertNotNull(constructor, "Two-parameter constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ModuleExport.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have getExportType method")
    void shouldHaveGetExportTypeMethod() throws NoSuchMethodException {
      final Method method = ModuleExport.class.getMethod("getExportType");
      assertNotNull(method, "getExportType method should exist");
      assertEquals(
          ExportType.class, method.getReturnType(), "getExportType should return ExportType");
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should throw IllegalArgumentException when name is null")
    void shouldThrowWhenNameIsNull() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("test", testType);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ModuleExport(null, exportType),
              "Should throw IllegalArgumentException for null name");

      assertEquals(
          "Export name cannot be null", exception.getMessage(), "Exception message should match");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when exportType is null")
    void shouldThrowWhenExportTypeIsNull() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ModuleExport("testExport", null),
              "Should throw IllegalArgumentException for null exportType");

      assertEquals(
          "Export type cannot be null", exception.getMessage(), "Exception message should match");
    }

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("testFunc", testType);
      final ModuleExport export = new ModuleExport("myExport", exportType);

      assertNotNull(export, "ModuleExport instance should not be null");
      assertEquals("myExport", export.getName(), "Name should match");
      assertEquals(exportType, export.getExportType(), "ExportType should match");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("getName should return the export name")
    void getNameShouldReturnExportName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("add", exportType);

      assertEquals("add", export.getName(), "getName should return the export name");
    }

    @Test
    @DisplayName("getExportType should return the export type")
    void getExportTypeShouldReturnExportType() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ExportType exportType = new ExportType("memory", testType);
      final ModuleExport export = new ModuleExport("main_memory", exportType);

      assertEquals(exportType, export.getExportType(), "getExportType should return the type");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string representation")
    void toStringShouldReturnFormattedString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("calculate", exportType);

      final String result = export.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("ModuleExport"), "toString should contain class name");
      assertTrue(result.contains("calculate"), "toString should contain export name");
    }

    @Test
    @DisplayName("toString should include name and type information")
    void toStringShouldIncludeNameAndType() {
      final WasmType testType = new TestWasmType(WasmTypeKind.GLOBAL);
      final ExportType exportType = new ExportType("global", testType);
      final ModuleExport export = new ModuleExport("pi_value", exportType);

      final String result = export.toString();

      assertTrue(result.contains("pi_value"), "toString should include the export name");
      assertTrue(result.contains("type="), "toString should include type information");
    }
  }

  @Nested
  @DisplayName("equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("test", exportType);

      assertTrue(export.equals(export), "Same instance should be equal");
    }

    @Test
    @DisplayName("equals should return true for equal objects")
    void equalsShouldReturnTrueForEqualObjects() {
      final WasmType testType1 = new TestWasmType(WasmTypeKind.FUNCTION);
      final WasmType testType2 = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType1 = new ExportType("func", testType1);
      final ExportType exportType2 = new ExportType("func", testType2);
      final ModuleExport export1 = new ModuleExport("test", exportType1);
      final ModuleExport export2 = new ModuleExport("test", exportType2);

      assertEquals(export1, export2, "Equal objects should be equal");
      assertEquals(export2, export1, "Equality should be symmetric");
    }

    @Test
    @DisplayName("equals should return false for different names")
    void equalsShouldReturnFalseForDifferentNames() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export1 = new ModuleExport("test1", exportType);
      final ModuleExport export2 = new ModuleExport("test2", exportType);

      assertNotEquals(export1, export2, "Different names should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different export types")
    void equalsShouldReturnFalseForDifferentExportTypes() {
      final WasmType testType1 = new TestWasmType(WasmTypeKind.FUNCTION);
      final WasmType testType2 = new TestWasmType(WasmTypeKind.MEMORY);
      final ExportType exportType1 = new ExportType("func1", testType1);
      final ExportType exportType2 = new ExportType("func2", testType2);
      final ModuleExport export1 = new ModuleExport("test", exportType1);
      final ModuleExport export2 = new ModuleExport("test", exportType2);

      assertNotEquals(export1, export2, "Different export types should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("test", exportType);

      assertFalse(export.equals(null), "Comparing with null should return false");
    }

    @Test
    @DisplayName("equals should return false for different class")
    void equalsShouldReturnFalseForDifferentClass() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("test", exportType);

      assertFalse(export.equals("test"), "Comparing with different class should return false");
    }
  }

  @Nested
  @DisplayName("hashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("test", exportType);

      final int hash1 = export.hashCode();
      final int hash2 = export.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent across calls");
    }

    @Test
    @DisplayName("hashCode should be equal for equal objects")
    void hashCodeShouldBeEqualForEqualObjects() {
      final WasmType testType1 = new TestWasmType(WasmTypeKind.FUNCTION);
      final WasmType testType2 = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType1 = new ExportType("func", testType1);
      final ExportType exportType2 = new ExportType("func", testType2);
      final ModuleExport export1 = new ModuleExport("test", exportType1);
      final ModuleExport export2 = new ModuleExport("test", exportType2);

      assertEquals(
          export1.hashCode(), export2.hashCode(), "Equal objects should have equal hash codes");
    }

    @Test
    @DisplayName("hashCode should differ for different names")
    void hashCodeShouldDifferForDifferentNames() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export1 = new ModuleExport("name1", exportType);
      final ModuleExport export2 = new ModuleExport("name2", exportType);

      assertNotEquals(
          export1.hashCode(),
          export2.hashCode(),
          "Different names should likely have different hash codes");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty name string")
    void shouldHandleEmptyNameString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("", exportType);

      assertEquals("", export.getName(), "Empty name should be preserved");
    }

    @Test
    @DisplayName("should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("export$with_special-chars.123", exportType);

      assertEquals(
          "export$with_special-chars.123",
          export.getName(),
          "Special characters should be preserved");
    }

    @Test
    @DisplayName("should handle unicode characters in name")
    void shouldHandleUnicodeCharactersInName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("エクスポート", exportType);

      assertEquals("エクスポート", export.getName(), "Unicode characters should be preserved");
    }

    @Test
    @DisplayName("should handle very long names")
    void shouldHandleVeryLongNames() {
      final String longName = "a".repeat(10000);
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport(longName, exportType);

      assertEquals(longName, export.getName(), "Long names should be preserved");
    }
  }

  @Nested
  @DisplayName("WasmTypeKind Integration Tests")
  class WasmTypeKindIntegrationTests {

    @Test
    @DisplayName("should work with FUNCTION type kind")
    void shouldWorkWithFunctionTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ExportType exportType = new ExportType("func", testType);
      final ModuleExport export = new ModuleExport("my_function", exportType);

      assertEquals(
          WasmTypeKind.FUNCTION,
          export.getExportType().getType().getKind(),
          "Should work with FUNCTION");
    }

    @Test
    @DisplayName("should work with MEMORY type kind")
    void shouldWorkWithMemoryTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ExportType exportType = new ExportType("memory", testType);
      final ModuleExport export = new ModuleExport("my_memory", exportType);

      assertEquals(
          WasmTypeKind.MEMORY,
          export.getExportType().getType().getKind(),
          "Should work with MEMORY");
    }

    @Test
    @DisplayName("should work with TABLE type kind")
    void shouldWorkWithTableTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.TABLE);
      final ExportType exportType = new ExportType("table", testType);
      final ModuleExport export = new ModuleExport("my_table", exportType);

      assertEquals(
          WasmTypeKind.TABLE, export.getExportType().getType().getKind(), "Should work with TABLE");
    }

    @Test
    @DisplayName("should work with GLOBAL type kind")
    void shouldWorkWithGlobalTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.GLOBAL);
      final ExportType exportType = new ExportType("global", testType);
      final ModuleExport export = new ModuleExport("my_global", exportType);

      assertEquals(
          WasmTypeKind.GLOBAL,
          export.getExportType().getType().getKind(),
          "Should work with GLOBAL");
    }
  }
}
