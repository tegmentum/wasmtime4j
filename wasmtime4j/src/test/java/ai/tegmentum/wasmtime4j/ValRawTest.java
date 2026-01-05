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
 * Comprehensive test suite for the ValRaw class.
 *
 * <p>ValRaw provides a type-unsafe but efficient raw representation of WebAssembly values for
 * low-level operations. This test verifies the class structure and method signatures.
 */
@DisplayName("ValRaw Class Tests")
class ValRawTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(ValRaw.class.getModifiers()), "ValRaw should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(ValRaw.class.getModifiers()), "ValRaw should be public");
    }

    @Test
    @DisplayName("should not be an interface")
    void shouldNotBeAnInterface() {
      assertTrue(!ValRaw.class.isInterface(), "ValRaw should not be an interface");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static i32 factory method")
    void shouldHaveI32FactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("i32", int.class);
      assertNotNull(method, "i32 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "i32 should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "i32 should return ValRaw");
    }

    @Test
    @DisplayName("should have static i64 factory method")
    void shouldHaveI64FactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("i64", long.class);
      assertNotNull(method, "i64 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "i64 should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "i64 should return ValRaw");
    }

    @Test
    @DisplayName("should have static f32 factory method")
    void shouldHaveF32FactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("f32", float.class);
      assertNotNull(method, "f32 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "f32 should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "f32 should return ValRaw");
    }

    @Test
    @DisplayName("should have static f64 factory method")
    void shouldHaveF64FactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("f64", double.class);
      assertNotNull(method, "f64 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "f64 should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "f64 should return ValRaw");
    }

    @Test
    @DisplayName("should have static v128 factory method")
    void shouldHaveV128FactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("v128", long.class, long.class);
      assertNotNull(method, "v128 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "v128 should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "v128 should return ValRaw");
    }

    @Test
    @DisplayName("should have static funcref factory method")
    void shouldHaveFuncrefFactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("funcref", int.class);
      assertNotNull(method, "funcref method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "funcref should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "funcref should return ValRaw");
    }

    @Test
    @DisplayName("should have static externref factory method")
    void shouldHaveExternrefFactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("externref", long.class);
      assertNotNull(method, "externref method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "externref should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "externref should return ValRaw");
    }

    @Test
    @DisplayName("should have static anyref factory method")
    void shouldHaveAnyrefFactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("anyref", long.class);
      assertNotNull(method, "anyref method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "anyref should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "anyref should return ValRaw");
    }

    @Test
    @DisplayName("should have static nullFuncref factory method")
    void shouldHaveNullFuncrefFactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("nullFuncref");
      assertNotNull(method, "nullFuncref method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullFuncref should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "nullFuncref should return ValRaw");
    }

    @Test
    @DisplayName("should have static nullExternref factory method")
    void shouldHaveNullExternrefFactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("nullExternref");
      assertNotNull(method, "nullExternref method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullExternref should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "nullExternref should return ValRaw");
    }

    @Test
    @DisplayName("should have static fromRawBits factory method")
    void shouldHaveFromRawBitsFactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("fromRawBits", long.class, long.class);
      assertNotNull(method, "fromRawBits method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromRawBits should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "fromRawBits should return ValRaw");
    }

    @Test
    @DisplayName("should have static fromWasmValue factory method")
    void shouldHaveFromWasmValueFactoryMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("fromWasmValue", WasmValue.class);
      assertNotNull(method, "fromWasmValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromWasmValue should be static");
      assertEquals(ValRaw.class, method.getReturnType(), "fromWasmValue should return ValRaw");
    }
  }

  // ========================================================================
  // Instance Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Getter Methods Tests")
  class InstanceGetterMethodsTests {

    @Test
    @DisplayName("should have getLowBits method")
    void shouldHaveGetLowBitsMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("getLowBits");
      assertNotNull(method, "getLowBits method should exist");
      assertEquals(long.class, method.getReturnType(), "getLowBits should return long");
    }

    @Test
    @DisplayName("should have getHighBits method")
    void shouldHaveGetHighBitsMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("getHighBits");
      assertNotNull(method, "getHighBits method should exist");
      assertEquals(long.class, method.getReturnType(), "getHighBits should return long");
    }
  }

  // ========================================================================
  // Type Interpretation Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Interpretation Methods Tests")
  class TypeInterpretationMethodsTests {

    @Test
    @DisplayName("should have asI32 method")
    void shouldHaveAsI32Method() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asI32");
      assertNotNull(method, "asI32 method should exist");
      assertEquals(int.class, method.getReturnType(), "asI32 should return int");
    }

    @Test
    @DisplayName("should have asU32 method")
    void shouldHaveAsU32Method() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asU32");
      assertNotNull(method, "asU32 method should exist");
      assertEquals(long.class, method.getReturnType(), "asU32 should return long");
    }

    @Test
    @DisplayName("should have asI64 method")
    void shouldHaveAsI64Method() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asI64");
      assertNotNull(method, "asI64 method should exist");
      assertEquals(long.class, method.getReturnType(), "asI64 should return long");
    }

    @Test
    @DisplayName("should have asF32 method")
    void shouldHaveAsF32Method() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asF32");
      assertNotNull(method, "asF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "asF32 should return float");
    }

    @Test
    @DisplayName("should have asF64 method")
    void shouldHaveAsF64Method() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asF64");
      assertNotNull(method, "asF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "asF64 should return double");
    }

    @Test
    @DisplayName("should have asV128Low method")
    void shouldHaveAsV128LowMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asV128Low");
      assertNotNull(method, "asV128Low method should exist");
      assertEquals(long.class, method.getReturnType(), "asV128Low should return long");
    }

    @Test
    @DisplayName("should have asV128High method")
    void shouldHaveAsV128HighMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asV128High");
      assertNotNull(method, "asV128High method should exist");
      assertEquals(long.class, method.getReturnType(), "asV128High should return long");
    }

    @Test
    @DisplayName("should have asFuncrefIndex method")
    void shouldHaveAsFuncrefIndexMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asFuncrefIndex");
      assertNotNull(method, "asFuncrefIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "asFuncrefIndex should return int");
    }

    @Test
    @DisplayName("should have asExternrefPtr method")
    void shouldHaveAsExternrefPtrMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asExternrefPtr");
      assertNotNull(method, "asExternrefPtr method should exist");
      assertEquals(long.class, method.getReturnType(), "asExternrefPtr should return long");
    }

    @Test
    @DisplayName("should have asAnyrefPtr method")
    void shouldHaveAsAnyrefPtrMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("asAnyrefPtr");
      assertNotNull(method, "asAnyrefPtr method should exist");
      assertEquals(long.class, method.getReturnType(), "asAnyrefPtr should return long");
    }
  }

  // ========================================================================
  // Conversion Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Conversion Methods Tests")
  class ConversionMethodsTests {

    @Test
    @DisplayName("should have toWasmValue method")
    void shouldHaveToWasmValueMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("toWasmValue", WasmValueType.class);
      assertNotNull(method, "toWasmValue method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "toWasmValue should return WasmValue");
    }
  }

  // ========================================================================
  // Object Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Methods Tests")
  class ObjectMethodsTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "equals should return boolean");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(int.class, method.getReturnType(), "hashCode should return int");
    }

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = ValRaw.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
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
              "i32",
              "i64",
              "f32",
              "f64",
              "v128",
              "funcref",
              "externref",
              "anyref",
              "nullFuncref",
              "nullExternref",
              "fromRawBits",
              "fromWasmValue",
              "getLowBits",
              "getHighBits",
              "asI32",
              "asU32",
              "asI64",
              "asF32",
              "asF64",
              "asV128Low",
              "asV128High",
              "asFuncrefIndex",
              "asExternrefPtr",
              "asAnyrefPtr",
              "toWasmValue",
              "equals",
              "hashCode",
              "toString");

      Set<String> actualMethods =
          Arrays.stream(ValRaw.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "ValRaw should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 27 declared methods")
    void shouldHaveAtLeast27DeclaredMethods() {
      // Note: getDeclaredMethods may include synthetic methods or bridge methods
      assertTrue(
          ValRaw.class.getDeclaredMethods().length >= 27,
          "ValRaw should have at least 27 methods (found "
              + ValRaw.class.getDeclaredMethods().length
              + ")");
    }

    @Test
    @DisplayName("should have at least 12 static methods")
    void shouldHaveAtLeast12StaticMethods() {
      long staticMethodCount =
          Arrays.stream(ValRaw.class.getDeclaredMethods())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertTrue(
          staticMethodCount >= 12,
          "ValRaw should have at least 12 static methods (found " + staticMethodCount + ")");
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
      assertEquals(Object.class, ValRaw.class.getSuperclass(), "ValRaw should extend Object");
    }

    @Test
    @DisplayName("should not implement any interface")
    void shouldNotImplementAnyInterface() {
      assertEquals(
          0, ValRaw.class.getInterfaces().length, "ValRaw should not implement any interface");
    }
  }
}
