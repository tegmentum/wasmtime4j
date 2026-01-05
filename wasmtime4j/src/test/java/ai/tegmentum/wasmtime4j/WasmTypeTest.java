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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmType interface.
 *
 * <p>WasmType is the base interface for WebAssembly types including function types, memory types,
 * global types, and table types. This test verifies the interface structure and method signatures.
 */
@DisplayName("WasmType Interface Tests")
class WasmTypeTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmType.class.isInterface(), "WasmType should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasmType.class.getModifiers()), "WasmType should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(WasmType.class.getModifiers()),
          "WasmType should not be final (interfaces cannot be final)");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getKind method")
    void shouldHaveGetKindMethod() throws NoSuchMethodException {
      final Method method = WasmType.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertEquals(
          WasmTypeKind.class, method.getReturnType(), "getKind should return WasmTypeKind");
    }

    @Test
    @DisplayName("getKind should be abstract (not default)")
    void getKindShouldBeAbstract() throws NoSuchMethodException {
      final Method method = WasmType.class.getMethod("getKind");
      assertFalse(method.isDefault(), "getKind should not be a default method");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "getKind should be abstract");
    }

    @Test
    @DisplayName("getKind should have no parameters")
    void getKindShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasmType.class.getMethod("getKind");
      assertEquals(0, method.getParameterCount(), "getKind should have no parameters");
    }

    @Test
    @DisplayName("getKind should be public")
    void getKindShouldBePublic() throws NoSuchMethodException {
      final Method method = WasmType.class.getMethod("getKind");
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "getKind should be public (inherited from interface)");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly 1 declared method")
    void shouldHaveExactly1DeclaredMethod() {
      int declaredMethods = WasmType.class.getDeclaredMethods().length;
      assertEquals(1, declaredMethods, "WasmType should have exactly 1 declared method");
    }

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(WasmType.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertEquals(0, defaultMethods, "WasmType should have no default methods");
    }

    @Test
    @DisplayName("should have exactly 1 abstract method")
    void shouldHaveExactly1AbstractMethod() {
      long abstractMethods =
          Arrays.stream(WasmType.class.getDeclaredMethods())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(1, abstractMethods, "WasmType should have exactly 1 abstract method");
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
          0, WasmType.class.getInterfaces().length, "WasmType should not extend any interface");
    }

    @Test
    @DisplayName("FuncType should implement WasmType")
    void funcTypeShouldImplementWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(FuncType.class), "FuncType should implement WasmType");
    }

    @Test
    @DisplayName("GlobalType should implement WasmType")
    void globalTypeShouldImplementWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(GlobalType.class),
          "GlobalType should implement WasmType");
    }

    @Test
    @DisplayName("MemoryType should implement WasmType")
    void memoryTypeShouldImplementWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(MemoryType.class),
          "MemoryType should implement WasmType");
    }

    @Test
    @DisplayName("TableType should implement WasmType")
    void tableTypeShouldImplementWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(TableType.class), "TableType should implement WasmType");
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
          Arrays.stream(WasmType.class.getDeclaredMethods())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "WasmType should have no static methods");
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
          0, WasmType.class.getDeclaredClasses().length, "WasmType should have no nested classes");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0, WasmType.class.getDeclaredFields().length, "WasmType should have no declared fields");
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("getKind return type should be WasmTypeKind enum")
    void getKindReturnTypeShouldBeWasmTypeKindEnum() throws NoSuchMethodException {
      Method method = WasmType.class.getMethod("getKind");
      Class<?> returnType = method.getReturnType();
      assertTrue(returnType.isEnum(), "getKind return type should be an enum");
      assertEquals(WasmTypeKind.class, returnType, "getKind should return WasmTypeKind");
    }
  }

  // ========================================================================
  // Design Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Design Pattern Tests")
  class DesignPatternTests {

    @Test
    @DisplayName("should be a marker/tagging interface with single method")
    void shouldBeMarkerInterfaceWithSingleMethod() {
      // WasmType is a simple categorization interface with one method
      assertEquals(
          1,
          WasmType.class.getDeclaredMethods().length,
          "WasmType should have only one method for categorization");
    }

    @Test
    @DisplayName("getKind provides type discrimination")
    void getKindProvidesTypeDiscrimination() throws NoSuchMethodException {
      Method method = WasmType.class.getMethod("getKind");
      // Return type is an enum which provides discrete categories
      assertTrue(
          method.getReturnType().isEnum(), "getKind should return an enum for discrimination");
    }
  }

  // ========================================================================
  // Consistency with ExportDescriptor and ImportDescriptor Tests
  // ========================================================================

  @Nested
  @DisplayName("Consistency with Descriptor Interfaces Tests")
  class ConsistencyWithDescriptorInterfacesTests {

    @Test
    @DisplayName("ExportDescriptor.getType should return WasmType")
    void exportDescriptorGetTypeShouldReturnWasmType() throws NoSuchMethodException {
      Method method = ExportDescriptor.class.getMethod("getType");
      assertEquals(
          WasmType.class,
          method.getReturnType(),
          "ExportDescriptor.getType should return WasmType");
    }

    @Test
    @DisplayName("ImportDescriptor.getType should return WasmType")
    void importDescriptorGetTypeShouldReturnWasmType() throws NoSuchMethodException {
      Method method = ImportDescriptor.class.getMethod("getType");
      assertEquals(
          WasmType.class,
          method.getReturnType(),
          "ImportDescriptor.getType should return WasmType");
    }
  }
}
