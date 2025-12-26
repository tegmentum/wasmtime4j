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
 * Tests for {@link ModuleImport} class.
 *
 * <p>ModuleImport represents a WebAssembly module import with complete type information.
 */
@DisplayName("ModuleImport Tests")
class ModuleImportTest {

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
      assertTrue(
          Modifier.isPublic(ModuleImport.class.getModifiers()), "ModuleImport should be public");
      assertTrue(
          Modifier.isFinal(ModuleImport.class.getModifiers()), "ModuleImport should be final");
    }

    @Test
    @DisplayName("should have three-parameter constructor")
    void shouldHaveThreeParameterConstructor() throws NoSuchMethodException {
      final var constructor =
          ModuleImport.class.getConstructor(String.class, String.class, ImportType.class);
      assertNotNull(constructor, "Three-parameter constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName method should exist");
      assertEquals(String.class, method.getReturnType(), "getModuleName should return String");
    }

    @Test
    @DisplayName("should have getFieldName method")
    void shouldHaveGetFieldNameMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("getFieldName");
      assertNotNull(method, "getFieldName method should exist");
      assertEquals(String.class, method.getReturnType(), "getFieldName should return String");
    }

    @Test
    @DisplayName("should have getImportType method")
    void shouldHaveGetImportTypeMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("getImportType");
      assertNotNull(method, "getImportType method should exist");
      assertEquals(
          ImportType.class, method.getReturnType(), "getImportType should return ImportType");
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should throw IllegalArgumentException when moduleName is null")
    void shouldThrowWhenModuleNameIsNull() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ModuleImport(null, "field", importType),
              "Should throw IllegalArgumentException for null moduleName");

      assertEquals(
          "Module name cannot be null", exception.getMessage(), "Exception message should match");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when fieldName is null")
    void shouldThrowWhenFieldNameIsNull() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ModuleImport("env", null, importType),
              "Should throw IllegalArgumentException for null fieldName");

      assertEquals(
          "Field name cannot be null", exception.getMessage(), "Exception message should match");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when importType is null")
    void shouldThrowWhenImportTypeIsNull() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ModuleImport("env", "field", null),
              "Should throw IllegalArgumentException for null importType");

      assertEquals(
          "Import type cannot be null", exception.getMessage(), "Exception message should match");
    }

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "myFunc", importType);

      assertNotNull(moduleImport, "ModuleImport instance should not be null");
      assertEquals("env", moduleImport.getModuleName(), "Module name should match");
      assertEquals("myFunc", moduleImport.getFieldName(), "Field name should match");
      assertEquals(importType, moduleImport.getImportType(), "Import type should match");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("getModuleName should return the module name")
    void getModuleNameShouldReturnModuleName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("wasi", "fd_write", testType);
      final ModuleImport moduleImport =
          new ModuleImport("wasi_snapshot_preview1", "fd_write", importType);

      assertEquals(
          "wasi_snapshot_preview1",
          moduleImport.getModuleName(),
          "getModuleName should return the module name");
    }

    @Test
    @DisplayName("getFieldName should return the field name")
    void getFieldNameShouldReturnFieldName() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ImportType importType = new ImportType("env", "memory", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "my_memory", importType);

      assertEquals(
          "my_memory", moduleImport.getFieldName(), "getFieldName should return the field name");
    }

    @Test
    @DisplayName("getImportType should return the import type")
    void getImportTypeShouldReturnImportType() {
      final WasmType testType = new TestWasmType(WasmTypeKind.TABLE);
      final ImportType importType = new ImportType("env", "table", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "my_table", importType);

      assertEquals(
          importType, moduleImport.getImportType(), "getImportType should return the import type");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal for same values")
    void shouldBeEqualForSameValues() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType1 = new ImportType("env", "func", testType);
      final ImportType importType2 = new ImportType("env", "func", testType);
      final ModuleImport import1 = new ModuleImport("env", "myFunc", importType1);
      final ModuleImport import2 = new ModuleImport("env", "myFunc", importType2);

      assertEquals(import1, import2, "ModuleImports with same values should be equal");
      assertEquals(import1.hashCode(), import2.hashCode(), "Hash codes should be equal");
    }

    @Test
    @DisplayName("should not be equal for different module names")
    void shouldNotBeEqualForDifferentModuleNames() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport import1 = new ModuleImport("env", "myFunc", importType);
      final ModuleImport import2 = new ModuleImport("wasi", "myFunc", importType);

      assertNotEquals(
          import1, import2, "ModuleImports with different module names should not be equal");
    }

    @Test
    @DisplayName("should not be equal for different field names")
    void shouldNotBeEqualForDifferentFieldNames() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport import1 = new ModuleImport("env", "func1", importType);
      final ModuleImport import2 = new ModuleImport("env", "func2", importType);

      assertNotEquals(
          import1, import2, "ModuleImports with different field names should not be equal");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "myFunc", importType);

      assertEquals(moduleImport, moduleImport, "ModuleImport should be equal to itself");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "myFunc", importType);

      assertFalse(moduleImport.equals(null), "ModuleImport should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "myFunc", importType);

      assertFalse(moduleImport.equals("string"), "ModuleImport should not be equal to a String");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string representation")
    void toStringShouldReturnFormattedString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "myFunc", importType);

      final String result = moduleImport.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("ModuleImport"), "toString should contain class name");
      assertTrue(result.contains("env"), "toString should contain module name");
      assertTrue(result.contains("myFunc"), "toString should contain field name");
    }

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("wasi", "clock_get", testType);
      final ModuleImport moduleImport =
          new ModuleImport("wasi_snapshot_preview1", "clock_time_get", importType);

      final String result = moduleImport.toString();

      assertTrue(result.contains("wasi_snapshot_preview1"), "toString should include module name");
      assertTrue(result.contains("clock_time_get"), "toString should include field name");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty module name string")
    void shouldHandleEmptyModuleNameString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("", "myFunc", importType);

      assertEquals("", moduleImport.getModuleName(), "Empty module name should be preserved");
    }

    @Test
    @DisplayName("should handle empty field name string")
    void shouldHandleEmptyFieldNameString() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "", importType);

      assertEquals("", moduleImport.getFieldName(), "Empty field name should be preserved");
    }

    @Test
    @DisplayName("should handle special characters in names")
    void shouldHandleSpecialCharactersInNames() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport =
          new ModuleImport("module$name", "field_name-123", importType);

      assertEquals(
          "module$name",
          moduleImport.getModuleName(),
          "Special chars in module name should be preserved");
      assertEquals(
          "field_name-123",
          moduleImport.getFieldName(),
          "Special chars in field name should be preserved");
    }

    @Test
    @DisplayName("should handle unicode characters")
    void shouldHandleUnicodeCharacters() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("モジュール", "関数", importType);

      assertEquals(
          "モジュール", moduleImport.getModuleName(), "Unicode module name should be preserved");
      assertEquals("関数", moduleImport.getFieldName(), "Unicode field name should be preserved");
    }

    @Test
    @DisplayName("should handle very long names")
    void shouldHandleVeryLongNames() {
      final String longModuleName = "m".repeat(10000);
      final String longFieldName = "f".repeat(10000);
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport(longModuleName, longFieldName, importType);

      assertEquals(
          longModuleName, moduleImport.getModuleName(), "Long module name should be preserved");
      assertEquals(
          longFieldName, moduleImport.getFieldName(), "Long field name should be preserved");
    }
  }

  @Nested
  @DisplayName("WasmTypeKind Integration Tests")
  class WasmTypeKindIntegrationTests {

    @Test
    @DisplayName("should work with FUNCTION type kind")
    void shouldWorkWithFunctionTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.FUNCTION);
      final ImportType importType = new ImportType("env", "func", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "myFunc", importType);

      assertEquals(
          WasmTypeKind.FUNCTION,
          moduleImport.getImportType().getType().getKind(),
          "Should work with FUNCTION");
    }

    @Test
    @DisplayName("should work with MEMORY type kind")
    void shouldWorkWithMemoryTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.MEMORY);
      final ImportType importType = new ImportType("env", "memory", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "memory", importType);

      assertEquals(
          WasmTypeKind.MEMORY,
          moduleImport.getImportType().getType().getKind(),
          "Should work with MEMORY");
    }

    @Test
    @DisplayName("should work with TABLE type kind")
    void shouldWorkWithTableTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.TABLE);
      final ImportType importType = new ImportType("env", "table", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "func_table", importType);

      assertEquals(
          WasmTypeKind.TABLE,
          moduleImport.getImportType().getType().getKind(),
          "Should work with TABLE");
    }

    @Test
    @DisplayName("should work with GLOBAL type kind")
    void shouldWorkWithGlobalTypeKind() {
      final WasmType testType = new TestWasmType(WasmTypeKind.GLOBAL);
      final ImportType importType = new ImportType("env", "global", testType);
      final ModuleImport moduleImport = new ModuleImport("env", "my_global", importType);

      assertEquals(
          WasmTypeKind.GLOBAL,
          moduleImport.getImportType().getType().getKind(),
          "Should work with GLOBAL");
    }
  }
}
