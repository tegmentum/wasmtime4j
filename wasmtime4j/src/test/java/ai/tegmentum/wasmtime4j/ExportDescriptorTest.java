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
 * Comprehensive test suite for the ExportDescriptor interface.
 *
 * <p>ExportDescriptor describes a WebAssembly export with detailed type information including the
 * export name and type. This test verifies the interface structure and method signatures.
 */
@DisplayName("ExportDescriptor Interface Tests")
class ExportDescriptorTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExportDescriptor.class.isInterface(), "ExportDescriptor should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExportDescriptor.class.getModifiers()),
          "ExportDescriptor should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(ExportDescriptor.class.getModifiers()),
          "ExportDescriptor should not be final (interfaces cannot be final)");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getName abstract method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
      assertFalse(method.isDefault(), "getName should be abstract (not default)");
    }

    @Test
    @DisplayName("should have getType abstract method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(WasmType.class, method.getReturnType(), "getType should return WasmType");
      assertFalse(method.isDefault(), "getType should be abstract (not default)");
    }

    @Test
    @DisplayName("should have exactly 2 abstract methods")
    void shouldHaveExactly2AbstractMethods() {
      long abstractMethods =
          Arrays.stream(ExportDescriptor.class.getDeclaredMethods())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(2, abstractMethods, "ExportDescriptor should have exactly 2 abstract methods");
    }
  }

  // ========================================================================
  // Default Method Tests - getKind
  // ========================================================================

  @Nested
  @DisplayName("getKind Default Method Tests")
  class GetKindDefaultMethodTests {

    @Test
    @DisplayName("should have getKind default method")
    void shouldHaveGetKindMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertTrue(method.isDefault(), "getKind should be a default method");
      assertEquals(
          WasmTypeKind.class, method.getReturnType(), "getKind should return WasmTypeKind");
    }

    @Test
    @DisplayName("getKind should have no parameters")
    void getKindShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("getKind");
      assertEquals(0, method.getParameterCount(), "getKind should have no parameters");
    }
  }

  // ========================================================================
  // Default Method Tests - Type Checking Methods
  // ========================================================================

  @Nested
  @DisplayName("Type Checking Default Method Tests")
  class TypeCheckingDefaultMethodTests {

    @Test
    @DisplayName("should have isFunction default method")
    void shouldHaveIsFunctionMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("isFunction");
      assertNotNull(method, "isFunction method should exist");
      assertTrue(method.isDefault(), "isFunction should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "isFunction should return boolean");
    }

    @Test
    @DisplayName("should have isGlobal default method")
    void shouldHaveIsGlobalMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("isGlobal");
      assertNotNull(method, "isGlobal method should exist");
      assertTrue(method.isDefault(), "isGlobal should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "isGlobal should return boolean");
    }

    @Test
    @DisplayName("should have isMemory default method")
    void shouldHaveIsMemoryMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("isMemory");
      assertNotNull(method, "isMemory method should exist");
      assertTrue(method.isDefault(), "isMemory should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "isMemory should return boolean");
    }

    @Test
    @DisplayName("should have isTable default method")
    void shouldHaveIsTableMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("isTable");
      assertNotNull(method, "isTable method should exist");
      assertTrue(method.isDefault(), "isTable should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "isTable should return boolean");
    }

    @Test
    @DisplayName("all type checking methods should have no parameters")
    void allTypeCheckingMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      String[] methodNames = {"isFunction", "isGlobal", "isMemory", "isTable"};
      for (String name : methodNames) {
        Method method = ExportDescriptor.class.getMethod(name);
        assertEquals(0, method.getParameterCount(), name + " should have no parameters");
      }
    }
  }

  // ========================================================================
  // Default Method Tests - Type Casting Methods
  // ========================================================================

  @Nested
  @DisplayName("Type Casting Default Method Tests")
  class TypeCastingDefaultMethodTests {

    @Test
    @DisplayName("should have asFunctionType default method")
    void shouldHaveAsFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("asFunctionType");
      assertNotNull(method, "asFunctionType method should exist");
      assertTrue(method.isDefault(), "asFunctionType should be a default method");
      assertEquals(FuncType.class, method.getReturnType(), "asFunctionType should return FuncType");
    }

    @Test
    @DisplayName("should have asGlobalType default method")
    void shouldHaveAsGlobalTypeMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("asGlobalType");
      assertNotNull(method, "asGlobalType method should exist");
      assertTrue(method.isDefault(), "asGlobalType should be a default method");
      assertEquals(
          GlobalType.class, method.getReturnType(), "asGlobalType should return GlobalType");
    }

    @Test
    @DisplayName("should have asMemoryType default method")
    void shouldHaveAsMemoryTypeMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("asMemoryType");
      assertNotNull(method, "asMemoryType method should exist");
      assertTrue(method.isDefault(), "asMemoryType should be a default method");
      assertEquals(
          MemoryType.class, method.getReturnType(), "asMemoryType should return MemoryType");
    }

    @Test
    @DisplayName("should have asTableType default method")
    void shouldHaveAsTableTypeMethod() throws NoSuchMethodException {
      final Method method = ExportDescriptor.class.getMethod("asTableType");
      assertNotNull(method, "asTableType method should exist");
      assertTrue(method.isDefault(), "asTableType should be a default method");
      assertEquals(TableType.class, method.getReturnType(), "asTableType should return TableType");
    }

    @Test
    @DisplayName("all type casting methods should have no parameters")
    void allTypeCastingMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      String[] methodNames = {"asFunctionType", "asGlobalType", "asMemoryType", "asTableType"};
      for (String name : methodNames) {
        Method method = ExportDescriptor.class.getMethod(name);
        assertEquals(0, method.getParameterCount(), name + " should have no parameters");
      }
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
              "getType",
              "getKind",
              "isFunction",
              "isGlobal",
              "isMemory",
              "isTable",
              "asFunctionType",
              "asGlobalType",
              "asMemoryType",
              "asTableType");

      Set<String> actualMethods =
          Arrays.stream(ExportDescriptor.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "ExportDescriptor should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 11 declared methods")
    void shouldHaveExactly11DeclaredMethods() {
      long declaredMethods =
          Arrays.stream(ExportDescriptor.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(11, declaredMethods, "ExportDescriptor should have exactly 11 declared methods");
    }

    @Test
    @DisplayName("should have exactly 9 default methods")
    void shouldHaveExactly9DefaultMethods() {
      long defaultMethods =
          Arrays.stream(ExportDescriptor.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(9, defaultMethods, "ExportDescriptor should have exactly 9 default methods");
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
          0,
          ExportDescriptor.class.getInterfaces().length,
          "ExportDescriptor should not extend any interface");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethods =
          Arrays.stream(ExportDescriptor.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "ExportDescriptor should have no static methods");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          ExportDescriptor.class.getDeclaredClasses().length,
          "ExportDescriptor should have no nested classes");
    }
  }

  // ========================================================================
  // Method Return Type Consistency Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Consistency Tests")
  class ReturnTypeConsistencyTests {

    @Test
    @DisplayName("type checking methods should return primitive boolean")
    void typeCheckingMethodsShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      String[] methodNames = {"isFunction", "isGlobal", "isMemory", "isTable"};
      for (String name : methodNames) {
        Method method = ExportDescriptor.class.getMethod(name);
        assertEquals(
            boolean.class, method.getReturnType(), name + " should return primitive boolean");
        assertFalse(
            method.getReturnType().equals(Boolean.class),
            name + " should not return Boolean wrapper");
      }
    }

    @Test
    @DisplayName("type casting methods should return WasmType subinterfaces")
    void typeCastingMethodsShouldReturnWasmTypeSubinterfaces() throws NoSuchMethodException {
      assertTrue(
          WasmType.class.isAssignableFrom(
              ExportDescriptor.class.getMethod("asFunctionType").getReturnType()),
          "asFunctionType return type should be assignable to WasmType");
      assertTrue(
          WasmType.class.isAssignableFrom(
              ExportDescriptor.class.getMethod("asGlobalType").getReturnType()),
          "asGlobalType return type should be assignable to WasmType");
      assertTrue(
          WasmType.class.isAssignableFrom(
              ExportDescriptor.class.getMethod("asMemoryType").getReturnType()),
          "asMemoryType return type should be assignable to WasmType");
      assertTrue(
          WasmType.class.isAssignableFrom(
              ExportDescriptor.class.getMethod("asTableType").getReturnType()),
          "asTableType return type should be assignable to WasmType");
    }
  }

  // ========================================================================
  // Method Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Semantic Tests")
  class MethodSemanticTests {

    @Test
    @DisplayName("getName should be public")
    void getNameShouldBePublic() throws NoSuchMethodException {
      Method method = ExportDescriptor.class.getMethod("getName");
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "getName should be public (inherited from interface)");
    }

    @Test
    @DisplayName("getType should be public")
    void getTypeShouldBePublic() throws NoSuchMethodException {
      Method method = ExportDescriptor.class.getMethod("getType");
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "getType should be public (inherited from interface)");
    }
  }
}
