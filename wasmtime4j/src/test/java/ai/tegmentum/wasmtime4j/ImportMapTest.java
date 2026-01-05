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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ImportMap interface.
 *
 * <p>ImportMap provides a way to supply host functions, memories, globals, and tables that a
 * WebAssembly module requires for instantiation. This test verifies the interface structure and
 * method signatures.
 */
@DisplayName("ImportMap Interface Tests")
class ImportMapTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ImportMap.class.isInterface(), "ImportMap should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(ImportMap.class.getModifiers()), "ImportMap should be public");
    }
  }

  // ========================================================================
  // Add Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Add Methods Tests")
  class AddMethodsTests {

    @Test
    @DisplayName("should have addFunction method")
    void shouldHaveAddFunctionMethod() throws NoSuchMethodException {
      final Method method =
          ImportMap.class.getMethod("addFunction", String.class, String.class, WasmFunction.class);
      assertNotNull(method, "addFunction method should exist");
      assertEquals(ImportMap.class, method.getReturnType(), "addFunction should return ImportMap");
    }

    @Test
    @DisplayName("should have addMemory method")
    void shouldHaveAddMemoryMethod() throws NoSuchMethodException {
      final Method method =
          ImportMap.class.getMethod("addMemory", String.class, String.class, WasmMemory.class);
      assertNotNull(method, "addMemory method should exist");
      assertEquals(ImportMap.class, method.getReturnType(), "addMemory should return ImportMap");
    }

    @Test
    @DisplayName("should have addGlobal method")
    void shouldHaveAddGlobalMethod() throws NoSuchMethodException {
      final Method method =
          ImportMap.class.getMethod("addGlobal", String.class, String.class, WasmGlobal.class);
      assertNotNull(method, "addGlobal method should exist");
      assertEquals(ImportMap.class, method.getReturnType(), "addGlobal should return ImportMap");
    }

    @Test
    @DisplayName("should have addTable method")
    void shouldHaveAddTableMethod() throws NoSuchMethodException {
      final Method method =
          ImportMap.class.getMethod("addTable", String.class, String.class, WasmTable.class);
      assertNotNull(method, "addTable method should exist");
      assertEquals(ImportMap.class, method.getReturnType(), "addTable should return ImportMap");
    }

    @Test
    @DisplayName("all add methods should have 3 parameters")
    void allAddMethodsShouldHave3Parameters() throws NoSuchMethodException {
      assertEquals(
          3,
          ImportMap.class
              .getMethod("addFunction", String.class, String.class, WasmFunction.class)
              .getParameterCount(),
          "addFunction should have 3 params");
      assertEquals(
          3,
          ImportMap.class
              .getMethod("addMemory", String.class, String.class, WasmMemory.class)
              .getParameterCount(),
          "addMemory should have 3 params");
      assertEquals(
          3,
          ImportMap.class
              .getMethod("addGlobal", String.class, String.class, WasmGlobal.class)
              .getParameterCount(),
          "addGlobal should have 3 params");
      assertEquals(
          3,
          ImportMap.class
              .getMethod("addTable", String.class, String.class, WasmTable.class)
              .getParameterCount(),
          "addTable should have 3 params");
    }

    @Test
    @DisplayName("add methods first two parameters should be String")
    void addMethodsFirstTwoParametersShouldBeString() throws NoSuchMethodException {
      Method[] addMethods = {
        ImportMap.class.getMethod("addFunction", String.class, String.class, WasmFunction.class),
        ImportMap.class.getMethod("addMemory", String.class, String.class, WasmMemory.class),
        ImportMap.class.getMethod("addGlobal", String.class, String.class, WasmGlobal.class),
        ImportMap.class.getMethod("addTable", String.class, String.class, WasmTable.class)
      };

      for (Method method : addMethods) {
        Class<?>[] paramTypes = method.getParameterTypes();
        assertEquals(
            String.class,
            paramTypes[0],
            method.getName() + " first parameter should be String (moduleName)");
        assertEquals(
            String.class,
            paramTypes[1],
            method.getName() + " second parameter should be String (name)");
      }
    }
  }

  // ========================================================================
  // Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Methods Tests")
  class GetterMethodsTests {

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("getFunction", String.class, String.class);
      assertNotNull(method, "getFunction method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getFunction should return Optional");
    }

    @Test
    @DisplayName("should have getImports method")
    void shouldHaveGetImportsMethod() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("getImports");
      assertNotNull(method, "getImports method should exist");
      assertEquals(Map.class, method.getReturnType(), "getImports should return Map");
    }

    @Test
    @DisplayName("getFunction should have 2 parameters")
    void getFunctionShouldHave2Parameters() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("getFunction", String.class, String.class);
      assertEquals(2, method.getParameterCount(), "getFunction should have 2 parameters");
    }

    @Test
    @DisplayName("getImports should have no parameters")
    void getImportsShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("getImports");
      assertEquals(0, method.getParameterCount(), "getImports should have 0 parameters");
    }
  }

  // ========================================================================
  // Contains Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Contains Method Tests")
  class ContainsMethodTests {

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("contains", String.class, String.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "contains should return boolean");
    }

    @Test
    @DisplayName("contains should have 2 String parameters")
    void containsShouldHave2StringParameters() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("contains", String.class, String.class);
      assertEquals(2, method.getParameterCount(), "contains should have 2 parameters");
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(String.class, paramTypes[0], "First parameter should be String (moduleName)");
      assertEquals(String.class, paramTypes[1], "Second parameter should be String (name)");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static empty method")
    void shouldHaveStaticEmptyMethod() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("empty");
      assertNotNull(method, "empty method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "empty should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "empty should be public");
    }

    @Test
    @DisplayName("empty method should return ImportMap")
    void emptyMethodShouldReturnImportMap() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("empty");
      assertEquals(ImportMap.class, method.getReturnType(), "empty should return ImportMap");
    }

    @Test
    @DisplayName("empty method should have no parameters")
    void emptyMethodShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("empty");
      assertEquals(0, method.getParameterCount(), "empty should have 0 parameters");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "addFunction",
              "addMemory",
              "addGlobal",
              "addTable",
              "getFunction",
              "getImports",
              "contains",
              "empty");

      Set<String> actualMethods =
          Arrays.stream(ImportMap.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "ImportMap should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 8 declared methods")
    void shouldHaveAtLeast8DeclaredMethods() {
      assertTrue(
          ImportMap.class.getDeclaredMethods().length >= 8,
          "ImportMap should have at least 8 methods");
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("all add methods should return ImportMap for fluent chaining")
    void allAddMethodsShouldReturnImportMapForFluentChaining() throws NoSuchMethodException {
      assertEquals(
          ImportMap.class,
          ImportMap.class
              .getMethod("addFunction", String.class, String.class, WasmFunction.class)
              .getReturnType(),
          "addFunction should return ImportMap");
      assertEquals(
          ImportMap.class,
          ImportMap.class
              .getMethod("addMemory", String.class, String.class, WasmMemory.class)
              .getReturnType(),
          "addMemory should return ImportMap");
      assertEquals(
          ImportMap.class,
          ImportMap.class
              .getMethod("addGlobal", String.class, String.class, WasmGlobal.class)
              .getReturnType(),
          "addGlobal should return ImportMap");
      assertEquals(
          ImportMap.class,
          ImportMap.class
              .getMethod("addTable", String.class, String.class, WasmTable.class)
              .getReturnType(),
          "addTable should return ImportMap");
    }

    @Test
    @DisplayName("getFunction should return Optional of WasmFunction")
    void getFunctionShouldReturnOptionalOfWasmFunction() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("getFunction", String.class, String.class);
      assertEquals(Optional.class, method.getReturnType(), "getFunction should return Optional");
    }

    @Test
    @DisplayName("getImports should return Map")
    void getImportsShouldReturnMap() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("getImports");
      assertEquals(Map.class, method.getReturnType(), "getImports should return Map");
    }

    @Test
    @DisplayName("contains should return boolean")
    void containsShouldReturnBoolean() throws NoSuchMethodException {
      final Method method = ImportMap.class.getMethod("contains", String.class, String.class);
      assertEquals(boolean.class, method.getReturnType(), "contains should return boolean");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0, ImportMap.class.getInterfaces().length, "ImportMap should not extend any interface");
    }
  }
}
