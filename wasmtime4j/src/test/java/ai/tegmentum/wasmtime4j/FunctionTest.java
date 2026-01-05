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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Function interface.
 *
 * <p>Function represents a WebAssembly function instance that can be called from Java. This test
 * verifies the interface structure, method signatures, and inner types.
 */
@DisplayName("Function Interface Tests")
class FunctionTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Function.class.isInterface(), "Function should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Function.class.getModifiers()), "Function should be public");
    }

    @Test
    @DisplayName("should have one type parameter")
    void shouldHaveOneTypeParameter() {
      TypeVariable<?>[] typeParams = Function.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Function should have exactly one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }
  }

  // ========================================================================
  // Call Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Call Methods Tests")
  class CallMethodsTests {

    @Test
    @DisplayName("should have call method with varargs")
    void shouldHaveCallMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("call", Object[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(Object[].class, method.getReturnType(), "call should return Object[]");
      assertTrue(method.isVarArgs(), "call should accept varargs");
    }

    @Test
    @DisplayName("should have callSingle method with varargs")
    void shouldHaveCallSingleMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("callSingle", Object[].class);
      assertNotNull(method, "callSingle method should exist");
      assertEquals(Object.class, method.getReturnType(), "callSingle should return Object");
      assertTrue(method.isVarArgs(), "callSingle should accept varargs");
    }

    @Test
    @DisplayName("call method should declare WasmException")
    void callMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("call", Object[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "call should declare one exception");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.WasmException",
          exceptionTypes[0].getName(),
          "call should declare WasmException");
    }

    @Test
    @DisplayName("callSingle method should declare WasmException")
    void callSingleMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("callSingle", Object[].class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "callSingle should declare one exception");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.WasmException",
          exceptionTypes[0].getName(),
          "callSingle should declare WasmException");
    }
  }

  // ========================================================================
  // Async Call Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Async Call Methods Tests")
  class AsyncCallMethodsTests {

    @Test
    @DisplayName("should have callAsync method with varargs")
    void shouldHaveCallAsyncMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("callAsync", Object[].class);
      assertNotNull(method, "callAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "callAsync should return CompletableFuture");
      assertTrue(method.isVarArgs(), "callAsync should accept varargs");
    }

    @Test
    @DisplayName("should have callAsync method with timeout")
    void shouldHaveCallAsyncWithTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          Function.class.getMethod("callAsync", long.class, TimeUnit.class, Object[].class);
      assertNotNull(method, "callAsync with timeout method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "callAsync with timeout should return CompletableFuture");
    }

    @Test
    @DisplayName("should have callSingleAsync method with varargs")
    void shouldHaveCallSingleAsyncMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("callSingleAsync", Object[].class);
      assertNotNull(method, "callSingleAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "callSingleAsync should return CompletableFuture");
      assertTrue(method.isVarArgs(), "callSingleAsync should accept varargs");
    }

    @Test
    @DisplayName("should have callSingleAsync method with timeout")
    void shouldHaveCallSingleAsyncWithTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          Function.class.getMethod("callSingleAsync", long.class, TimeUnit.class, Object[].class);
      assertNotNull(method, "callSingleAsync with timeout method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "callSingleAsync with timeout should return CompletableFuture");
    }

    @Test
    @DisplayName("callAsync timeout method should have correct parameter types")
    void callAsyncTimeoutMethodShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      final Method method =
          Function.class.getMethod("callAsync", long.class, TimeUnit.class, Object[].class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(3, paramTypes.length, "callAsync with timeout should have 3 parameters");
      assertEquals(long.class, paramTypes[0], "First param should be long (timeout)");
      assertEquals(TimeUnit.class, paramTypes[1], "Second param should be TimeUnit");
      assertEquals(Object[].class, paramTypes[2], "Third param should be Object[] (varargs)");
    }
  }

  // ========================================================================
  // Signature Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Signature Methods Tests")
  class SignatureMethodsTests {

    @Test
    @DisplayName("should have getSignature method")
    void shouldHaveGetSignatureMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("getSignature");
      assertNotNull(method, "getSignature method should exist");
      assertEquals(
          Function.FunctionSignature.class,
          method.getReturnType(),
          "getSignature should return FunctionSignature");
    }

    @Test
    @DisplayName("should have getParameterTypes method")
    void shouldHaveGetParameterTypesMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("getParameterTypes");
      assertNotNull(method, "getParameterTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "getParameterTypes should return List");
    }

    @Test
    @DisplayName("should have getReturnTypes method")
    void shouldHaveGetReturnTypesMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("getReturnTypes");
      assertNotNull(method, "getReturnTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "getReturnTypes should return List");
    }

    @Test
    @DisplayName("getParameterTypes should return List of ValueType")
    void getParameterTypesShouldReturnListOfValueType() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("getParameterTypes");
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType.getTypeName().contains("ValueType"),
          "getParameterTypes should return List<ValueType>");
    }

    @Test
    @DisplayName("getReturnTypes should return List of ValueType")
    void getReturnTypesShouldReturnListOfValueType() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("getReturnTypes");
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType.getTypeName().contains("ValueType"),
          "getReturnTypes should return List<ValueType>");
    }
  }

  // ========================================================================
  // Metadata Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Metadata Methods Tests")
  class MetadataMethodsTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getParameterCount method")
    void shouldHaveGetParameterCountMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("getParameterCount");
      assertNotNull(method, "getParameterCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getParameterCount should return int");
    }

    @Test
    @DisplayName("should have getReturnCount method")
    void shouldHaveGetReturnCountMethod() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("getReturnCount");
      assertNotNull(method, "getReturnCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getReturnCount should return int");
    }
  }

  // ========================================================================
  // FunctionSignature Inner Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("FunctionSignature Inner Interface Tests")
  class FunctionSignatureTests {

    @Test
    @DisplayName("FunctionSignature should be an interface")
    void functionSignatureShouldBeAnInterface() {
      assertTrue(
          Function.FunctionSignature.class.isInterface(),
          "FunctionSignature should be an interface");
    }

    @Test
    @DisplayName("FunctionSignature should be public")
    void functionSignatureShouldBePublic() {
      assertTrue(
          Modifier.isPublic(Function.FunctionSignature.class.getModifiers()),
          "FunctionSignature should be public");
    }

    @Test
    @DisplayName("FunctionSignature should be static")
    void functionSignatureShouldBeStatic() {
      assertTrue(
          Modifier.isStatic(Function.FunctionSignature.class.getModifiers()),
          "FunctionSignature should be static");
    }

    @Test
    @DisplayName("FunctionSignature should have getParameterTypes method")
    void functionSignatureShouldHaveGetParameterTypes() throws NoSuchMethodException {
      final Method method = Function.FunctionSignature.class.getMethod("getParameterTypes");
      assertNotNull(method, "getParameterTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "getParameterTypes should return List");
    }

    @Test
    @DisplayName("FunctionSignature should have getReturnTypes method")
    void functionSignatureShouldHaveGetReturnTypes() throws NoSuchMethodException {
      final Method method = Function.FunctionSignature.class.getMethod("getReturnTypes");
      assertNotNull(method, "getReturnTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "getReturnTypes should return List");
    }

    @Test
    @DisplayName("FunctionSignature should have matches method")
    void functionSignatureShouldHaveMatchesMethod() throws NoSuchMethodException {
      final Method method =
          Function.FunctionSignature.class.getMethod("matches", Function.FunctionSignature.class);
      assertNotNull(method, "matches method should exist");
      assertEquals(boolean.class, method.getReturnType(), "matches should return boolean");
    }
  }

  // ========================================================================
  // ValueType Inner Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ValueType Inner Enum Tests")
  class ValueTypeEnumTests {

    @Test
    @DisplayName("ValueType should be an enum")
    void valueTypeShouldBeAnEnum() {
      assertTrue(Function.ValueType.class.isEnum(), "ValueType should be an enum");
    }

    @Test
    @DisplayName("ValueType should be public")
    void valueTypeShouldBePublic() {
      assertTrue(
          Modifier.isPublic(Function.ValueType.class.getModifiers()), "ValueType should be public");
    }

    @Test
    @DisplayName("ValueType should be static")
    void valueTypeShouldBeStatic() {
      assertTrue(
          Modifier.isStatic(Function.ValueType.class.getModifiers()), "ValueType should be static");
    }

    @Test
    @DisplayName("ValueType should have exactly 7 values")
    void valueTypeShouldHaveSevenValues() {
      assertEquals(7, Function.ValueType.values().length, "ValueType should have exactly 7 values");
    }

    @Test
    @DisplayName("ValueType should have all expected values")
    void valueTypeShouldHaveAllExpectedValues() {
      Set<String> expectedValues =
          Set.of("I32", "I64", "F32", "F64", "V128", "FUNCREF", "EXTERNREF");
      Set<String> actualValues =
          Arrays.stream(Function.ValueType.values()).map(Enum::name).collect(Collectors.toSet());
      assertEquals(expectedValues, actualValues, "ValueType should have all expected values");
    }

    @Test
    @DisplayName("I32 should exist")
    void i32ShouldExist() {
      Function.ValueType value = Function.ValueType.I32;
      assertNotNull(value, "I32 should exist");
      assertEquals("I32", value.name(), "I32 should have correct name");
    }

    @Test
    @DisplayName("I64 should exist")
    void i64ShouldExist() {
      Function.ValueType value = Function.ValueType.I64;
      assertNotNull(value, "I64 should exist");
      assertEquals("I64", value.name(), "I64 should have correct name");
    }

    @Test
    @DisplayName("F32 should exist")
    void f32ShouldExist() {
      Function.ValueType value = Function.ValueType.F32;
      assertNotNull(value, "F32 should exist");
      assertEquals("F32", value.name(), "F32 should have correct name");
    }

    @Test
    @DisplayName("F64 should exist")
    void f64ShouldExist() {
      Function.ValueType value = Function.ValueType.F64;
      assertNotNull(value, "F64 should exist");
      assertEquals("F64", value.name(), "F64 should have correct name");
    }

    @Test
    @DisplayName("V128 should exist")
    void v128ShouldExist() {
      Function.ValueType value = Function.ValueType.V128;
      assertNotNull(value, "V128 should exist");
      assertEquals("V128", value.name(), "V128 should have correct name");
    }

    @Test
    @DisplayName("FUNCREF should exist")
    void funcrefShouldExist() {
      Function.ValueType value = Function.ValueType.FUNCREF;
      assertNotNull(value, "FUNCREF should exist");
      assertEquals("FUNCREF", value.name(), "FUNCREF should have correct name");
    }

    @Test
    @DisplayName("EXTERNREF should exist")
    void externrefShouldExist() {
      Function.ValueType value = Function.ValueType.EXTERNREF;
      assertNotNull(value, "EXTERNREF should exist");
      assertEquals("EXTERNREF", value.name(), "EXTERNREF should have correct name");
    }

    @Test
    @DisplayName("ValueType ordinals should be sequential")
    void valueTypeOrdinalsShouldBeSequential() {
      Function.ValueType[] values = Function.ValueType.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal for " + values[i].name() + " should be " + i);
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
              "call",
              "callSingle",
              "callAsync",
              "callSingleAsync",
              "getSignature",
              "getParameterTypes",
              "getReturnTypes",
              "getName",
              "isValid",
              "getParameterCount",
              "getReturnCount");

      Set<String> actualMethods =
          Arrays.stream(Function.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Function should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have correct number of declared methods")
    void shouldHaveCorrectNumberOfDeclaredMethods() {
      long methodCount = Function.class.getDeclaredMethods().length;
      // call, callSingle, callAsync(2), callSingleAsync(2), getSignature,
      // getParameterTypes, getReturnTypes, getName, isValid, getParameterCount, getReturnCount = 13
      assertTrue(
          methodCount >= 13,
          "Function should have at least 13 declared methods, found: " + methodCount);
    }
  }

  // ========================================================================
  // Inner Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Inner Type Tests")
  class InnerTypeTests {

    @Test
    @DisplayName("should have exactly 2 inner types")
    void shouldHaveExactlyTwoInnerTypes() {
      Class<?>[] declaredClasses = Function.class.getDeclaredClasses();
      assertEquals(2, declaredClasses.length, "Function should have exactly 2 inner types");
    }

    @Test
    @DisplayName("inner types should include FunctionSignature and ValueType")
    void innerTypesShouldIncludeFunctionSignatureAndValueType() {
      Set<String> innerTypeNames =
          Arrays.stream(Function.class.getDeclaredClasses())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());
      assertTrue(
          innerTypeNames.contains("FunctionSignature"),
          "Inner types should include FunctionSignature");
      assertTrue(innerTypeNames.contains("ValueType"), "Inner types should include ValueType");
    }
  }

  // ========================================================================
  // Generic Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Type Tests")
  class GenericTypeTests {

    @Test
    @DisplayName("callAsync should return CompletableFuture of Object array")
    void callAsyncShouldReturnCompletableFutureOfObjectArray() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("callAsync", Object[].class);
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType,
          "callAsync return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) genericReturnType;
      assertEquals(
          CompletableFuture.class, paramType.getRawType(), "Raw type should be CompletableFuture");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have one type argument");
      assertEquals(Object[].class, typeArgs[0], "Type argument should be Object[]");
    }

    @Test
    @DisplayName("callSingleAsync should return CompletableFuture of Object")
    void callSingleAsyncShouldReturnCompletableFutureOfObject() throws NoSuchMethodException {
      final Method method = Function.class.getMethod("callSingleAsync", Object[].class);
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType,
          "callSingleAsync return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) genericReturnType;
      assertEquals(
          CompletableFuture.class, paramType.getRawType(), "Raw type should be CompletableFuture");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have one type argument");
      assertEquals(Object.class, typeArgs[0], "Type argument should be Object");
    }
  }
}
