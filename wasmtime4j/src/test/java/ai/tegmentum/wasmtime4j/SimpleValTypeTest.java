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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the SimpleValType class.
 *
 * <p>SimpleValType is a package-private implementation of ValType that wraps a WasmValueType. This
 * test verifies the class structure and method signatures using reflection.
 */
@DisplayName("SimpleValType Class Tests")
class SimpleValTypeTest {

  // Get the package-private class via reflection
  private Class<?> getSimpleValTypeClass() {
    try {
      return Class.forName("ai.tegmentum.wasmtime4j.SimpleValType");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("SimpleValType class not found", e);
    }
  }

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      Class<?> clazz = getSimpleValTypeClass();
      assertTrue(!clazz.isInterface(), "SimpleValType should be a class");
      assertTrue(!clazz.isEnum(), "SimpleValType should not be an enum");
    }

    @Test
    @DisplayName("should be package-private")
    void shouldBePackagePrivate() {
      Class<?> clazz = getSimpleValTypeClass();
      int modifiers = clazz.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isProtected(modifiers)
              && !Modifier.isPrivate(modifiers),
          "SimpleValType should be package-private");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      Class<?> clazz = getSimpleValTypeClass();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "SimpleValType should be final");
    }

    @Test
    @DisplayName("should implement ValType")
    void shouldImplementValType() {
      Class<?> clazz = getSimpleValTypeClass();
      assertTrue(ValType.class.isAssignableFrom(clazz), "SimpleValType should implement ValType");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with WasmValueType parameter")
    void shouldHavePackagePrivateConstructorWithWasmValueType() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      Constructor<?> constructor = clazz.getDeclaredConstructor(WasmValueType.class);
      assertNotNull(constructor, "Constructor with WasmValueType should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isProtected(modifiers)
              && !Modifier.isPrivate(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("constructor should accept WasmValueType parameter")
    void constructorShouldAcceptWasmValueTypeParameter() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      Constructor<?> constructor = clazz.getDeclaredConstructor(WasmValueType.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(1, paramTypes.length, "Constructor should have 1 parameter");
      assertEquals(WasmValueType.class, paramTypes[0], "Parameter should be WasmValueType");
    }
  }

  // ========================================================================
  // ValType Interface Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("ValType Interface Methods Tests")
  class ValTypeInterfaceMethodsTests {

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(
          WasmValueType.class, method.getReturnType(), "getValueType should return WasmValueType");
    }

    @Test
    @DisplayName("should have isNumeric method")
    void shouldHaveIsNumericMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("isNumeric");
      assertNotNull(method, "isNumeric method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isNumeric should return boolean");
    }

    @Test
    @DisplayName("should have isInteger method")
    void shouldHaveIsIntegerMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("isInteger");
      assertNotNull(method, "isInteger method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isInteger should return boolean");
    }

    @Test
    @DisplayName("should have isFloat method")
    void shouldHaveIsFloatMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("isFloat");
      assertNotNull(method, "isFloat method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isFloat should return boolean");
    }

    @Test
    @DisplayName("should have isReference method")
    void shouldHaveIsReferenceMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("isReference");
      assertNotNull(method, "isReference method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isReference should return boolean");
    }

    @Test
    @DisplayName("should have isGcReference method")
    void shouldHaveIsGcReferenceMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("isGcReference");
      assertNotNull(method, "isGcReference method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isGcReference should return boolean");
    }

    @Test
    @DisplayName("should have isNullableReference method")
    void shouldHaveIsNullableReferenceMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("isNullableReference");
      assertNotNull(method, "isNullableReference method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isNullableReference should return boolean");
    }

    @Test
    @DisplayName("should have isVector method")
    void shouldHaveIsVectorMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("isVector");
      assertNotNull(method, "isVector method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isVector should return boolean");
    }

    @Test
    @DisplayName("should have matches method")
    void shouldHaveMatchesMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("matches", ValType.class);
      assertNotNull(method, "matches method should exist");
      assertEquals(boolean.class, method.getReturnType(), "matches should return boolean");
    }

    @Test
    @DisplayName("should have eq method")
    void shouldHaveEqMethod() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("eq", ValType.class);
      assertNotNull(method, "eq method should exist");
      assertEquals(boolean.class, method.getReturnType(), "eq should return boolean");
    }
  }

  // ========================================================================
  // Object Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Methods Tests")
  class ObjectMethodsTests {

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          clazz, method.getDeclaringClass(), "toString should be declared in SimpleValType");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(clazz, method.getDeclaringClass(), "equals should be declared in SimpleValType");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      Class<?> clazz = getSimpleValTypeClass();
      final Method method = clazz.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          clazz, method.getDeclaringClass(), "hashCode should be declared in SimpleValType");
    }
  }

  // ========================================================================
  // Factory Inner Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Factory Inner Class Tests")
  class FactoryInnerClassTests {

    private Class<?> getFactoryClass() {
      try {
        return Class.forName("ai.tegmentum.wasmtime4j.SimpleValType$Factory");
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("SimpleValType.Factory class not found", e);
      }
    }

    @Test
    @DisplayName("Factory should be a static inner class")
    void factoryShouldBeStaticInnerClass() {
      Class<?> factoryClass = getFactoryClass();
      assertTrue(factoryClass.isMemberClass(), "Factory should be a member class");
      assertTrue(Modifier.isStatic(factoryClass.getModifiers()), "Factory should be static");
    }

    @Test
    @DisplayName("Factory should be package-private")
    void factoryShouldBePackagePrivate() {
      Class<?> factoryClass = getFactoryClass();
      int modifiers = factoryClass.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isProtected(modifiers)
              && !Modifier.isPrivate(modifiers),
          "Factory should be package-private");
    }

    @Test
    @DisplayName("Factory should be final")
    void factoryShouldBeFinal() {
      Class<?> factoryClass = getFactoryClass();
      assertTrue(Modifier.isFinal(factoryClass.getModifiers()), "Factory should be final");
    }

    @Test
    @DisplayName("Factory should have static from method")
    void factoryShouldHaveStaticFromMethod() throws NoSuchMethodException {
      Class<?> factoryClass = getFactoryClass();
      final Method method = factoryClass.getDeclaredMethod("from", WasmValueType.class);
      assertNotNull(method, "from method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "from should be static");
      assertEquals(ValType.class, method.getReturnType(), "from should return ValType");
    }

    @Test
    @DisplayName("Factory should have numeric type factory methods")
    void factoryShouldHaveNumericTypeFactoryMethods() throws NoSuchMethodException {
      Class<?> factoryClass = getFactoryClass();

      Method i32Method = factoryClass.getDeclaredMethod("i32");
      assertNotNull(i32Method, "i32 method should exist");
      assertTrue(Modifier.isStatic(i32Method.getModifiers()), "i32 should be static");

      Method i64Method = factoryClass.getDeclaredMethod("i64");
      assertNotNull(i64Method, "i64 method should exist");
      assertTrue(Modifier.isStatic(i64Method.getModifiers()), "i64 should be static");

      Method f32Method = factoryClass.getDeclaredMethod("f32");
      assertNotNull(f32Method, "f32 method should exist");
      assertTrue(Modifier.isStatic(f32Method.getModifiers()), "f32 should be static");

      Method f64Method = factoryClass.getDeclaredMethod("f64");
      assertNotNull(f64Method, "f64 method should exist");
      assertTrue(Modifier.isStatic(f64Method.getModifiers()), "f64 should be static");
    }

    @Test
    @DisplayName("Factory should have vector type factory method")
    void factoryShouldHaveVectorTypeFactoryMethod() throws NoSuchMethodException {
      Class<?> factoryClass = getFactoryClass();
      Method v128Method = factoryClass.getDeclaredMethod("v128");
      assertNotNull(v128Method, "v128 method should exist");
      assertTrue(Modifier.isStatic(v128Method.getModifiers()), "v128 should be static");
    }

    @Test
    @DisplayName("Factory should have reference type factory methods")
    void factoryShouldHaveReferenceTypeFactoryMethods() throws NoSuchMethodException {
      Class<?> factoryClass = getFactoryClass();

      Method funcrefMethod = factoryClass.getDeclaredMethod("funcref");
      assertNotNull(funcrefMethod, "funcref method should exist");
      assertTrue(Modifier.isStatic(funcrefMethod.getModifiers()), "funcref should be static");

      Method externrefMethod = factoryClass.getDeclaredMethod("externref");
      assertNotNull(externrefMethod, "externref method should exist");
      assertTrue(Modifier.isStatic(externrefMethod.getModifiers()), "externref should be static");
    }

    @Test
    @DisplayName("Factory should have GC reference type factory methods")
    void factoryShouldHaveGcReferenceTypeFactoryMethods() throws NoSuchMethodException {
      Class<?> factoryClass = getFactoryClass();
      Set<String> gcMethods =
          Set.of(
              "anyref",
              "eqref",
              "i31ref",
              "structref",
              "arrayref",
              "nullref",
              "nullfuncref",
              "nullexternref");

      for (String methodName : gcMethods) {
        Method method = factoryClass.getDeclaredMethod(methodName);
        assertNotNull(method, methodName + " method should exist");
        assertTrue(Modifier.isStatic(method.getModifiers()), methodName + " should be static");
        assertEquals(ValType.class, method.getReturnType(), methodName + " should return ValType");
      }
    }

    @Test
    @DisplayName("Factory should have all expected methods")
    void factoryShouldHaveAllExpectedMethods() {
      Class<?> factoryClass = getFactoryClass();
      Set<String> expectedMethods =
          Set.of(
              "from",
              "i32",
              "i64",
              "f32",
              "f64",
              "v128",
              "funcref",
              "externref",
              "anyref",
              "eqref",
              "i31ref",
              "structref",
              "arrayref",
              "nullref",
              "nullfuncref",
              "nullexternref");

      Set<String> actualMethods =
          Arrays.stream(factoryClass.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Factory should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have private final valueType field")
    void shouldHavePrivateFinalValueTypeField() throws NoSuchFieldException {
      Class<?> clazz = getSimpleValTypeClass();
      var field = clazz.getDeclaredField("valueType");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "valueType field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "valueType field should be final");
      assertEquals(WasmValueType.class, field.getType(), "valueType field should be WasmValueType");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object directly")
    void shouldExtendObjectDirectly() {
      Class<?> clazz = getSimpleValTypeClass();
      assertEquals(Object.class, clazz.getSuperclass(), "SimpleValType should extend Object");
    }

    @Test
    @DisplayName("should implement ValType interface")
    void shouldImplementValTypeInterface() {
      Class<?> clazz = getSimpleValTypeClass();
      Class<?>[] interfaces = clazz.getInterfaces();
      assertEquals(1, interfaces.length, "SimpleValType should implement exactly 1 interface");
      assertEquals(ValType.class, interfaces[0], "SimpleValType should implement ValType");
    }
  }
}
