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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CoreDumpGlobal interface.
 *
 * <p>CoreDumpGlobal represents a WebAssembly global variable captured in a core dump, providing
 * access to the global's value, type, and mutability information.
 */
@DisplayName("CoreDumpGlobal Interface Tests")
class CoreDumpGlobalTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CoreDumpGlobal.class.isInterface(), "CoreDumpGlobal should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CoreDumpGlobal.class.getModifiers()),
          "CoreDumpGlobal should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(CoreDumpGlobal.class.getModifiers()),
          "CoreDumpGlobal should not be final (interfaces cannot be final)");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          CoreDumpGlobal.class.getInterfaces().length,
          "CoreDumpGlobal should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getInstanceIndex method")
    void shouldHaveGetInstanceIndexMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getInstanceIndex");
      assertNotNull(method, "getInstanceIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getInstanceIndex should be abstract");
    }

    @Test
    @DisplayName("should have getGlobalIndex method")
    void shouldHaveGetGlobalIndexMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getGlobalIndex");
      assertNotNull(method, "getGlobalIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getGlobalIndex should be abstract");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertFalse(method.isDefault(), "getName should be abstract");
    }

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(WasmValueType.class, method.getReturnType(), "Should return WasmValueType");
      assertFalse(method.isDefault(), "getValueType should be abstract");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isMutable should be abstract");
    }

    @Test
    @DisplayName("should have getRawValue method")
    void shouldHaveGetRawValueMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getRawValue");
      assertNotNull(method, "getRawValue method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
      assertFalse(method.isDefault(), "getRawValue should be abstract");
    }

    @Test
    @DisplayName("should have getI32Value method")
    void shouldHaveGetI32ValueMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getI32Value");
      assertNotNull(method, "getI32Value method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getI32Value should be abstract");
    }

    @Test
    @DisplayName("should have getI64Value method")
    void shouldHaveGetI64ValueMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getI64Value");
      assertNotNull(method, "getI64Value method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getI64Value should be abstract");
    }

    @Test
    @DisplayName("should have getF32Value method")
    void shouldHaveGetF32ValueMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getF32Value");
      assertNotNull(method, "getF32Value method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
      assertFalse(method.isDefault(), "getF32Value should be abstract");
    }

    @Test
    @DisplayName("should have getF64Value method")
    void shouldHaveGetF64ValueMethod() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getF64Value");
      assertNotNull(method, "getF64Value method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertFalse(method.isDefault(), "getF64Value should be abstract");
    }

    @Test
    @DisplayName("should have exactly 10 abstract methods")
    void shouldHaveExactly10AbstractMethods() {
      long abstractMethods =
          Arrays.stream(CoreDumpGlobal.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(10, abstractMethods, "CoreDumpGlobal should have exactly 10 abstract methods");
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
              "getInstanceIndex",
              "getGlobalIndex",
              "getName",
              "getValueType",
              "isMutable",
              "getRawValue",
              "getI32Value",
              "getI64Value",
              "getF32Value",
              "getF64Value");

      Set<String> actualMethods =
          Arrays.stream(CoreDumpGlobal.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "CoreDumpGlobal should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 10 declared methods")
    void shouldHaveExactly10DeclaredMethods() {
      long methodCount =
          Arrays.stream(CoreDumpGlobal.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(10, methodCount, "CoreDumpGlobal should have exactly 10 declared methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(CoreDumpGlobal.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "CoreDumpGlobal should have no default methods");
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
          Arrays.stream(CoreDumpGlobal.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "CoreDumpGlobal should have no static methods");
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
          0,
          CoreDumpGlobal.class.getDeclaredFields().length,
          "CoreDumpGlobal should have no declared fields");
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
          CoreDumpGlobal.class.getDeclaredClasses().length,
          "CoreDumpGlobal should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getInstanceIndex should have no parameters")
    void getInstanceIndexShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getInstanceIndex");
      assertEquals(0, method.getParameterCount(), "getInstanceIndex should have no parameters");
    }

    @Test
    @DisplayName("getGlobalIndex should have no parameters")
    void getGlobalIndexShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getGlobalIndex");
      assertEquals(0, method.getParameterCount(), "getGlobalIndex should have no parameters");
    }

    @Test
    @DisplayName("getName should have no parameters")
    void getNameShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getName");
      assertEquals(0, method.getParameterCount(), "getName should have no parameters");
    }

    @Test
    @DisplayName("getValueType should have no parameters")
    void getValueTypeShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getValueType");
      assertEquals(0, method.getParameterCount(), "getValueType should have no parameters");
    }

    @Test
    @DisplayName("isMutable should have no parameters")
    void isMutableShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("isMutable");
      assertEquals(0, method.getParameterCount(), "isMutable should have no parameters");
    }

    @Test
    @DisplayName("getRawValue should have no parameters")
    void getRawValueShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getRawValue");
      assertEquals(0, method.getParameterCount(), "getRawValue should have no parameters");
    }

    @Test
    @DisplayName("getI32Value should have no parameters")
    void getI32ValueShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getI32Value");
      assertEquals(0, method.getParameterCount(), "getI32Value should have no parameters");
    }

    @Test
    @DisplayName("getI64Value should have no parameters")
    void getI64ValueShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getI64Value");
      assertEquals(0, method.getParameterCount(), "getI64Value should have no parameters");
    }

    @Test
    @DisplayName("getF32Value should have no parameters")
    void getF32ValueShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getF32Value");
      assertEquals(0, method.getParameterCount(), "getF32Value should have no parameters");
    }

    @Test
    @DisplayName("getF64Value should have no parameters")
    void getF64ValueShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getF64Value");
      assertEquals(0, method.getParameterCount(), "getF64Value should have no parameters");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(CoreDumpGlobal.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getName should return Optional<String>")
    void getNameShouldReturnOptionalString() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getName");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      assertEquals(
          String.class, paramType.getActualTypeArguments()[0], "Type argument should be String");
    }
  }

  // ========================================================================
  // Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("getInstanceIndex should return primitive int")
    void getInstanceIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getInstanceIndex");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getInstanceIndex should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getGlobalIndex should return primitive int")
    void getGlobalIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getGlobalIndex");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getGlobalIndex should return primitive int, not Integer");
    }

    @Test
    @DisplayName("isMutable should return primitive boolean")
    void isMutableShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("isMutable");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isMutable should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("getI32Value should return primitive int")
    void getI32ValueShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getI32Value");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getI32Value should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getI64Value should return primitive long")
    void getI64ValueShouldReturnPrimitiveLong() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getI64Value");
      assertEquals(
          long.class, method.getReturnType(), "getI64Value should return primitive long, not Long");
    }

    @Test
    @DisplayName("getF32Value should return primitive float")
    void getF32ValueShouldReturnPrimitiveFloat() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getF32Value");
      assertEquals(
          float.class,
          method.getReturnType(),
          "getF32Value should return primitive float, not Float");
    }

    @Test
    @DisplayName("getF64Value should return primitive double")
    void getF64ValueShouldReturnPrimitiveDouble() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getF64Value");
      assertEquals(
          double.class,
          method.getReturnType(),
          "getF64Value should return primitive double, not Double");
    }

    @Test
    @DisplayName("getValueType should return WasmValueType enum")
    void getValueTypeShouldReturnWasmValueTypeEnum() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getValueType");
      assertEquals(
          WasmValueType.class, method.getReturnType(), "getValueType should return WasmValueType");
    }

    @Test
    @DisplayName("getRawValue should return byte array")
    void getRawValueShouldReturnByteArray() throws NoSuchMethodException {
      Method method = CoreDumpGlobal.class.getMethod("getRawValue");
      assertEquals(byte[].class, method.getReturnType(), "getRawValue should return byte[]");
    }
  }
}
