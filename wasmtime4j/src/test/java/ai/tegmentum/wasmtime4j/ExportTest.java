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
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Export interface.
 *
 * <p>Export represents a WebAssembly export item that can be one of several types: function,
 * memory, table, or global. This test verifies the interface structure and method signatures.
 */
@DisplayName("Export Interface Tests")
class ExportTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Export.class.isInterface(), "Export should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Export.class.getModifiers()), "Export should be public");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have getExportType method")
    void shouldHaveGetExportTypeMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("getExportType");
      assertNotNull(method, "getExportType method should exist");
      assertEquals(
          ExportType.class, method.getReturnType(), "getExportType should return ExportType");
    }

    @Test
    @DisplayName("getName should be abstract")
    void getNameShouldBeAbstract() throws NoSuchMethodException {
      final Method method = Export.class.getDeclaredMethod("getName");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "getName should be abstract");
    }

    @Test
    @DisplayName("getExportType should be abstract")
    void getExportTypeShouldBeAbstract() throws NoSuchMethodException {
      final Method method = Export.class.getDeclaredMethod("getExportType");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "getExportType should be abstract");
    }
  }

  // ========================================================================
  // Default Type Check Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Type Check Methods Tests")
  class DefaultTypeCheckMethodsTests {

    @Test
    @DisplayName("should have getKind default method")
    void shouldHaveGetKindMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertEquals(
          WasmTypeKind.class, method.getReturnType(), "getKind should return WasmTypeKind");
      assertTrue(method.isDefault(), "getKind should be a default method");
    }

    @Test
    @DisplayName("should have isFunction default method")
    void shouldHaveIsFunctionMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("isFunction");
      assertNotNull(method, "isFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isFunction should return boolean");
      assertTrue(method.isDefault(), "isFunction should be a default method");
    }

    @Test
    @DisplayName("should have isMemory default method")
    void shouldHaveIsMemoryMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("isMemory");
      assertNotNull(method, "isMemory method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isMemory should return boolean");
      assertTrue(method.isDefault(), "isMemory should be a default method");
    }

    @Test
    @DisplayName("should have isTable default method")
    void shouldHaveIsTableMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("isTable");
      assertNotNull(method, "isTable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isTable should return boolean");
      assertTrue(method.isDefault(), "isTable should be a default method");
    }

    @Test
    @DisplayName("should have isGlobal default method")
    void shouldHaveIsGlobalMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("isGlobal");
      assertNotNull(method, "isGlobal method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isGlobal should return boolean");
      assertTrue(method.isDefault(), "isGlobal should be a default method");
    }
  }

  // ========================================================================
  // Default Cast Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Cast Methods Tests")
  class DefaultCastMethodsTests {

    @Test
    @DisplayName("should have asFunction default method")
    void shouldHaveAsFunctionMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("asFunction");
      assertNotNull(method, "asFunction method should exist");
      assertEquals(Function.class, method.getReturnType(), "asFunction should return Function");
      assertTrue(method.isDefault(), "asFunction should be a default method");
    }

    @Test
    @DisplayName("should have asMemory default method")
    void shouldHaveAsMemoryMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("asMemory");
      assertNotNull(method, "asMemory method should exist");
      assertEquals(Memory.class, method.getReturnType(), "asMemory should return Memory");
      assertTrue(method.isDefault(), "asMemory should be a default method");
    }

    @Test
    @DisplayName("should have asTable default method")
    void shouldHaveAsTableMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("asTable");
      assertNotNull(method, "asTable method should exist");
      assertEquals(Table.class, method.getReturnType(), "asTable should return Table");
      assertTrue(method.isDefault(), "asTable should be a default method");
    }

    @Test
    @DisplayName("should have asGlobal default method")
    void shouldHaveAsGlobalMethod() throws NoSuchMethodException {
      final Method method = Export.class.getMethod("asGlobal");
      assertNotNull(method, "asGlobal method should exist");
      assertEquals(Global.class, method.getReturnType(), "asGlobal should return Global");
      assertTrue(method.isDefault(), "asGlobal should be a default method");
    }
  }

  // ========================================================================
  // Method Signature Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("all type check methods should have no parameters")
    void allTypeCheckMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0, Export.class.getMethod("getKind").getParameterCount(), "getKind should have 0 params");
      assertEquals(
          0,
          Export.class.getMethod("isFunction").getParameterCount(),
          "isFunction should have 0 params");
      assertEquals(
          0,
          Export.class.getMethod("isMemory").getParameterCount(),
          "isMemory should have 0 params");
      assertEquals(
          0, Export.class.getMethod("isTable").getParameterCount(), "isTable should have 0 params");
      assertEquals(
          0,
          Export.class.getMethod("isGlobal").getParameterCount(),
          "isGlobal should have 0 params");
    }

    @Test
    @DisplayName("all cast methods should have no parameters")
    void allCastMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          Export.class.getMethod("asFunction").getParameterCount(),
          "asFunction should have 0 params");
      assertEquals(
          0,
          Export.class.getMethod("asMemory").getParameterCount(),
          "asMemory should have 0 params");
      assertEquals(
          0, Export.class.getMethod("asTable").getParameterCount(), "asTable should have 0 params");
      assertEquals(
          0,
          Export.class.getMethod("asGlobal").getParameterCount(),
          "asGlobal should have 0 params");
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
              "getName",
              "getExportType",
              "getKind",
              "isFunction",
              "isMemory",
              "isTable",
              "isGlobal",
              "asFunction",
              "asMemory",
              "asTable",
              "asGlobal");

      Set<String> actualMethods =
          Arrays.stream(Export.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Export should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 12 declared methods")
    void shouldHave12DeclaredMethods() {
      // 2 abstract + 9 default + 1 synthetic bridge method for generic asFunction<T>()
      assertEquals(
          12, Export.class.getDeclaredMethods().length, "Export should have exactly 12 methods");
    }

    @Test
    @DisplayName("should have 9 default methods")
    void shouldHave9DefaultMethods() {
      long defaultMethodCount =
          Arrays.stream(Export.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertEquals(9, defaultMethodCount, "Export should have 9 default methods");
    }

    @Test
    @DisplayName("should have 2 abstract methods")
    void shouldHave2AbstractMethods() {
      long abstractMethodCount =
          Arrays.stream(Export.class.getDeclaredMethods())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(2, abstractMethodCount, "Export should have 2 abstract methods");
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
          0, Export.class.getInterfaces().length, "Export should not extend any interface");
    }
  }
}
