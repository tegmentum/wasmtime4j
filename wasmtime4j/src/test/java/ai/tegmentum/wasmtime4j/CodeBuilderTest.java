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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CodeBuilder class.
 *
 * <p>CodeBuilder provides a fluent API for building WebAssembly modules programmatically. This test
 * verifies the class structure, builder methods, and nested enums.
 */
@DisplayName("CodeBuilder Class Tests")
class CodeBuilderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(CodeBuilder.class.getModifiers()), "CodeBuilder should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CodeBuilder.class.getModifiers()), "CodeBuilder should be public");
    }

    @Test
    @DisplayName("should not be abstract")
    void shouldNotBeAbstract() {
      assertTrue(
          !Modifier.isAbstract(CodeBuilder.class.getModifiers()),
          "CodeBuilder should not be abstract");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have no-arg constructor")
    void shouldHaveNoArgConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = CodeBuilder.class.getConstructor();
      assertNotNull(constructor, "No-arg constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "No-arg constructor should be public");
    }
  }

  // ========================================================================
  // Type Definition Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Builder Methods Tests")
  class TypeDefinitionBuilderMethodsTests {

    @Test
    @DisplayName("should have addType method")
    void shouldHaveAddTypeMethod() throws NoSuchMethodException {
      final Method method = CodeBuilder.class.getMethod("addType", FuncType.class);
      assertNotNull(method, "addType method should exist");
      assertEquals(CodeBuilder.class, method.getReturnType(), "addType should return CodeBuilder");
    }
  }

  // ========================================================================
  // Import Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Import Builder Methods Tests")
  class ImportBuilderMethodsTests {

    @Test
    @DisplayName("should have addFunctionImport method")
    void shouldHaveAddFunctionImportMethod() throws NoSuchMethodException {
      final Method method =
          CodeBuilder.class.getMethod("addFunctionImport", String.class, String.class, int.class);
      assertNotNull(method, "addFunctionImport method should exist");
      assertEquals(
          CodeBuilder.class, method.getReturnType(), "addFunctionImport should return CodeBuilder");
    }

    @Test
    @DisplayName("should have addMemoryImport method")
    void shouldHaveAddMemoryImportMethod() throws NoSuchMethodException {
      final Method method =
          CodeBuilder.class.getMethod(
              "addMemoryImport", String.class, String.class, int.class, int.class);
      assertNotNull(method, "addMemoryImport method should exist");
      assertEquals(
          CodeBuilder.class, method.getReturnType(), "addMemoryImport should return CodeBuilder");
    }
  }

  // ========================================================================
  // Function Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Function Builder Methods Tests")
  class FunctionBuilderMethodsTests {

    @Test
    @DisplayName("should have addFunction method")
    void shouldHaveAddFunctionMethod() throws NoSuchMethodException {
      final Method method =
          CodeBuilder.class.getMethod("addFunction", int.class, List.class, byte[].class);
      assertNotNull(method, "addFunction method should exist");
      assertEquals(
          CodeBuilder.class, method.getReturnType(), "addFunction should return CodeBuilder");
    }
  }

  // ========================================================================
  // Memory Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Memory Builder Methods Tests")
  class MemoryBuilderMethodsTests {

    @Test
    @DisplayName("should have addMemory method")
    void shouldHaveAddMemoryMethod() throws NoSuchMethodException {
      final Method method = CodeBuilder.class.getMethod("addMemory", int.class, int.class);
      assertNotNull(method, "addMemory method should exist");
      assertEquals(
          CodeBuilder.class, method.getReturnType(), "addMemory should return CodeBuilder");
    }
  }

  // ========================================================================
  // Table Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Table Builder Methods Tests")
  class TableBuilderMethodsTests {

    @Test
    @DisplayName("should have addTable method")
    void shouldHaveAddTableMethod() throws NoSuchMethodException {
      final Method method =
          CodeBuilder.class.getMethod("addTable", WasmValueType.class, int.class, int.class);
      assertNotNull(method, "addTable method should exist");
      assertEquals(CodeBuilder.class, method.getReturnType(), "addTable should return CodeBuilder");
    }
  }

  // ========================================================================
  // Global Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Global Builder Methods Tests")
  class GlobalBuilderMethodsTests {

    @Test
    @DisplayName("should have addGlobal method")
    void shouldHaveAddGlobalMethod() throws NoSuchMethodException {
      final Method method =
          CodeBuilder.class.getMethod("addGlobal", WasmValueType.class, boolean.class, long.class);
      assertNotNull(method, "addGlobal method should exist");
      assertEquals(
          CodeBuilder.class, method.getReturnType(), "addGlobal should return CodeBuilder");
    }
  }

  // ========================================================================
  // Export Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Export Builder Methods Tests")
  class ExportBuilderMethodsTests {

    @Test
    @DisplayName("should have addExport method")
    void shouldHaveAddExportMethod() throws NoSuchMethodException, ClassNotFoundException {
      Class<?> exportKindClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ExportKind");
      final Method method =
          CodeBuilder.class.getMethod("addExport", String.class, exportKindClass, int.class);
      assertNotNull(method, "addExport method should exist");
      assertEquals(
          CodeBuilder.class, method.getReturnType(), "addExport should return CodeBuilder");
    }
  }

  // ========================================================================
  // Build Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Build Methods Tests")
  class BuildMethodsTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = CodeBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(byte[].class, method.getReturnType(), "build should return byte[]");
    }

    @Test
    @DisplayName("should have writeTo method with OutputStream")
    void shouldHaveWriteToMethodWithOutputStream() throws NoSuchMethodException {
      final Method method = CodeBuilder.class.getMethod("writeTo", OutputStream.class);
      assertNotNull(method, "writeTo method with OutputStream should exist");
      assertEquals(void.class, method.getReturnType(), "writeTo should return void");
    }

    @Test
    @DisplayName("should have writeTo method with Path")
    void shouldHaveWriteToMethodWithPath() throws NoSuchMethodException {
      final Method method = CodeBuilder.class.getMethod("writeTo", Path.class);
      assertNotNull(method, "writeTo method with Path should exist");
      assertEquals(void.class, method.getReturnType(), "writeTo should return void");
    }
  }

  // ========================================================================
  // ImportKind Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ImportKind Enum Tests")
  class ImportKindEnumTests {

    @Test
    @DisplayName("should have ImportKind nested enum")
    void shouldHaveImportKindNestedEnum() {
      Class<?>[] nestedClasses = CodeBuilder.class.getDeclaredClasses();
      boolean hasImportKind =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("ImportKind"));
      assertTrue(hasImportKind, "CodeBuilder should have ImportKind nested enum");
    }

    @Test
    @DisplayName("ImportKind should be an enum")
    void importKindShouldBeAnEnum() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ImportKind");
      assertTrue(enumClass.isEnum(), "ImportKind should be an enum");
    }

    @Test
    @DisplayName("ImportKind should be public")
    void importKindShouldBePublic() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ImportKind");
      assertTrue(Modifier.isPublic(enumClass.getModifiers()), "ImportKind should be public");
    }

    @Test
    @DisplayName("ImportKind should have FUNCTION value")
    void importKindShouldHaveFunctionValue() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ImportKind");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasFunction = Arrays.stream(constants).anyMatch(c -> c.toString().equals("FUNCTION"));
      assertTrue(hasFunction, "ImportKind should have FUNCTION value");
    }

    @Test
    @DisplayName("ImportKind should have TABLE value")
    void importKindShouldHaveTableValue() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ImportKind");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasTable = Arrays.stream(constants).anyMatch(c -> c.toString().equals("TABLE"));
      assertTrue(hasTable, "ImportKind should have TABLE value");
    }

    @Test
    @DisplayName("ImportKind should have MEMORY value")
    void importKindShouldHaveMemoryValue() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ImportKind");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasMemory = Arrays.stream(constants).anyMatch(c -> c.toString().equals("MEMORY"));
      assertTrue(hasMemory, "ImportKind should have MEMORY value");
    }

    @Test
    @DisplayName("ImportKind should have GLOBAL value")
    void importKindShouldHaveGlobalValue() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ImportKind");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasGlobal = Arrays.stream(constants).anyMatch(c -> c.toString().equals("GLOBAL"));
      assertTrue(hasGlobal, "ImportKind should have GLOBAL value");
    }

    @Test
    @DisplayName("ImportKind should have exactly 4 values")
    void importKindShouldHaveExactly4Values() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ImportKind");
      Object[] constants = enumClass.getEnumConstants();
      assertEquals(4, constants.length, "ImportKind should have exactly 4 values");
    }
  }

  // ========================================================================
  // ExportKind Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExportKind Enum Tests")
  class ExportKindEnumTests {

    @Test
    @DisplayName("should have ExportKind nested enum")
    void shouldHaveExportKindNestedEnum() {
      Class<?>[] nestedClasses = CodeBuilder.class.getDeclaredClasses();
      boolean hasExportKind =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("ExportKind"));
      assertTrue(hasExportKind, "CodeBuilder should have ExportKind nested enum");
    }

    @Test
    @DisplayName("ExportKind should be an enum")
    void exportKindShouldBeAnEnum() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ExportKind");
      assertTrue(enumClass.isEnum(), "ExportKind should be an enum");
    }

    @Test
    @DisplayName("ExportKind should be public")
    void exportKindShouldBePublic() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ExportKind");
      assertTrue(Modifier.isPublic(enumClass.getModifiers()), "ExportKind should be public");
    }

    @Test
    @DisplayName("ExportKind should have FUNCTION value")
    void exportKindShouldHaveFunctionValue() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ExportKind");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasFunction = Arrays.stream(constants).anyMatch(c -> c.toString().equals("FUNCTION"));
      assertTrue(hasFunction, "ExportKind should have FUNCTION value");
    }

    @Test
    @DisplayName("ExportKind should have TABLE value")
    void exportKindShouldHaveTableValue() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ExportKind");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasTable = Arrays.stream(constants).anyMatch(c -> c.toString().equals("TABLE"));
      assertTrue(hasTable, "ExportKind should have TABLE value");
    }

    @Test
    @DisplayName("ExportKind should have MEMORY value")
    void exportKindShouldHaveMemoryValue() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ExportKind");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasMemory = Arrays.stream(constants).anyMatch(c -> c.toString().equals("MEMORY"));
      assertTrue(hasMemory, "ExportKind should have MEMORY value");
    }

    @Test
    @DisplayName("ExportKind should have GLOBAL value")
    void exportKindShouldHaveGlobalValue() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ExportKind");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasGlobal = Arrays.stream(constants).anyMatch(c -> c.toString().equals("GLOBAL"));
      assertTrue(hasGlobal, "ExportKind should have GLOBAL value");
    }

    @Test
    @DisplayName("ExportKind should have exactly 4 values")
    void exportKindShouldHaveExactly4Values() throws ClassNotFoundException {
      Class<?> enumClass = Class.forName("ai.tegmentum.wasmtime4j.CodeBuilder$ExportKind");
      Object[] constants = enumClass.getEnumConstants();
      assertEquals(4, constants.length, "ExportKind should have exactly 4 values");
    }
  }

  // ========================================================================
  // Nested Types Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Types Count Tests")
  class NestedTypesCountTests {

    @Test
    @DisplayName("should have exactly 2 public nested enums")
    void shouldHaveExactly2PublicNestedEnums() {
      Class<?>[] nestedClasses = CodeBuilder.class.getDeclaredClasses();
      long publicEnumCount =
          Arrays.stream(nestedClasses)
              .filter(c -> Modifier.isPublic(c.getModifiers()) && c.isEnum())
              .count();
      assertEquals(2, publicEnumCount, "Should have exactly 2 public nested enums");
    }

    @Test
    @DisplayName("should have ImportKind and ExportKind nested enums")
    void shouldHaveCorrectNestedEnums() {
      Class<?>[] nestedClasses = CodeBuilder.class.getDeclaredClasses();
      Set<String> enumNames =
          Arrays.stream(nestedClasses)
              .filter(c -> Modifier.isPublic(c.getModifiers()) && c.isEnum())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(enumNames.contains("ImportKind"), "Should have ImportKind nested enum");
      assertTrue(enumNames.contains("ExportKind"), "Should have ExportKind nested enum");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected public methods")
    void shouldHaveAllExpectedPublicMethods() {
      Set<String> expectedMethods =
          Set.of(
              "addType",
              "addFunctionImport",
              "addMemoryImport",
              "addFunction",
              "addMemory",
              "addTable",
              "addGlobal",
              "addExport",
              "build",
              "writeTo");

      Set<String> actualMethods =
          Arrays.stream(CodeBuilder.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "CodeBuilder should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Fluent API Tests
  // ========================================================================

  @Nested
  @DisplayName("Fluent API Tests")
  class FluentApiTests {

    @Test
    @DisplayName("all builder methods should return CodeBuilder for fluent chaining")
    void allBuilderMethodsShouldReturnCodeBuilderForFluentChaining() {
      Set<String> builderMethodNames =
          Set.of(
              "addType",
              "addFunctionImport",
              "addMemoryImport",
              "addFunction",
              "addMemory",
              "addTable",
              "addGlobal",
              "addExport");

      for (Method method : CodeBuilder.class.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())
            && builderMethodNames.contains(method.getName())) {
          assertEquals(
              CodeBuilder.class,
              method.getReturnType(),
              method.getName() + " should return CodeBuilder for fluent chaining");
        }
      }
    }
  }
}
