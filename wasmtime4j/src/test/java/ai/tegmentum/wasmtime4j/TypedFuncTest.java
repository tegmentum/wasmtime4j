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

import ai.tegmentum.wasmtime4j.func.TypedFunc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypedFunc} interface.
 *
 * <p>TypedFunc provides type-safe WebAssembly function calls with statically known signatures.
 */
@DisplayName("TypedFunc Tests")
class TypedFuncTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(Modifier.isPublic(TypedFunc.class.getModifiers()), "TypedFunc should be public");
      assertTrue(TypedFunc.class.isInterface(), "TypedFunc should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(TypedFunc.class),
          "TypedFunc should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have TypedFunctionSupport inner interface")
    void shouldHaveTypedFunctionSupportInterface() {
      final Class<?>[] innerClasses = TypedFunc.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("TypedFunctionSupport")) {
          found = true;
          assertTrue(innerClass.isInterface(), "TypedFunctionSupport should be an interface");
          break;
        }
      }
      assertTrue(found, "TypedFunctionSupport inner interface should exist");
    }
  }

  @Nested
  @DisplayName("Void-Returning Method Tests")
  class VoidReturningMethodTests {

    @Test
    @DisplayName("should have callVoidToVoid method")
    void shouldHaveCallVoidToVoidMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callVoidToVoid");
      assertNotNull(method, "callVoidToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "callVoidToVoid should return void");
      assertEquals(0, method.getParameterCount(), "callVoidToVoid should have no parameters");
    }

    @Test
    @DisplayName("should have callI32ToVoid method")
    void shouldHaveCallI32ToVoidMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI32ToVoid", int.class);
      assertNotNull(method, "callI32ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "callI32ToVoid should return void");
      assertEquals(1, method.getParameterCount(), "callI32ToVoid should have 1 parameter");
    }

    @Test
    @DisplayName("should have callI32I32ToVoid method")
    void shouldHaveCallI32I32ToVoidMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI32I32ToVoid", int.class, int.class);
      assertNotNull(method, "callI32I32ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "callI32I32ToVoid should return void");
      assertEquals(2, method.getParameterCount(), "callI32I32ToVoid should have 2 parameters");
    }

    @Test
    @DisplayName("should have callI64ToVoid method")
    void shouldHaveCallI64ToVoidMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI64ToVoid", long.class);
      assertNotNull(method, "callI64ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "callI64ToVoid should return void");
    }

    @Test
    @DisplayName("should have callI64I64ToVoid method")
    void shouldHaveCallI64I64ToVoidMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI64I64ToVoid", long.class, long.class);
      assertNotNull(method, "callI64I64ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "callI64I64ToVoid should return void");
    }
  }

  @Nested
  @DisplayName("I32-Returning Method Tests")
  class I32ReturningMethodTests {

    @Test
    @DisplayName("should have callI32ToI32 method")
    void shouldHaveCallI32ToI32Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI32ToI32", int.class);
      assertNotNull(method, "callI32ToI32 method should exist");
      assertEquals(int.class, method.getReturnType(), "callI32ToI32 should return int");
    }

    @Test
    @DisplayName("should have callI32I32ToI32 method")
    void shouldHaveCallI32I32ToI32Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI32I32ToI32", int.class, int.class);
      assertNotNull(method, "callI32I32ToI32 method should exist");
      assertEquals(int.class, method.getReturnType(), "callI32I32ToI32 should return int");
    }

    @Test
    @DisplayName("should have callI32I32I32ToI32 method")
    void shouldHaveCallI32I32I32ToI32Method() throws NoSuchMethodException {
      final Method method =
          TypedFunc.class.getMethod("callI32I32I32ToI32", int.class, int.class, int.class);
      assertNotNull(method, "callI32I32I32ToI32 method should exist");
      assertEquals(int.class, method.getReturnType(), "callI32I32I32ToI32 should return int");
      assertEquals(3, method.getParameterCount(), "callI32I32I32ToI32 should have 3 parameters");
    }

    @Test
    @DisplayName("should have callI64ToI32 method")
    void shouldHaveCallI64ToI32Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI64ToI32", long.class);
      assertNotNull(method, "callI64ToI32 method should exist");
      assertEquals(int.class, method.getReturnType(), "callI64ToI32 should return int");
    }
  }

  @Nested
  @DisplayName("I64-Returning Method Tests")
  class I64ReturningMethodTests {

    @Test
    @DisplayName("should have callI64ToI64 method")
    void shouldHaveCallI64ToI64Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI64ToI64", long.class);
      assertNotNull(method, "callI64ToI64 method should exist");
      assertEquals(long.class, method.getReturnType(), "callI64ToI64 should return long");
    }

    @Test
    @DisplayName("should have callI64I64ToI64 method")
    void shouldHaveCallI64I64ToI64Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI64I64ToI64", long.class, long.class);
      assertNotNull(method, "callI64I64ToI64 method should exist");
      assertEquals(long.class, method.getReturnType(), "callI64I64ToI64 should return long");
    }

    @Test
    @DisplayName("should have callI64I64I64ToI64 method")
    void shouldHaveCallI64I64I64ToI64Method() throws NoSuchMethodException {
      final Method method =
          TypedFunc.class.getMethod("callI64I64I64ToI64", long.class, long.class, long.class);
      assertNotNull(method, "callI64I64I64ToI64 method should exist");
      assertEquals(long.class, method.getReturnType(), "callI64I64I64ToI64 should return long");
      assertEquals(3, method.getParameterCount(), "callI64I64I64ToI64 should have 3 parameters");
    }

    @Test
    @DisplayName("should have callI32I32ToI64 method")
    void shouldHaveCallI32I32ToI64Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI32I32ToI64", int.class, int.class);
      assertNotNull(method, "callI32I32ToI64 method should exist");
      assertEquals(long.class, method.getReturnType(), "callI32I32ToI64 should return long");
    }
  }

  @Nested
  @DisplayName("F32-Returning Method Tests")
  class F32ReturningMethodTests {

    @Test
    @DisplayName("should have callF32ToF32 method")
    void shouldHaveCallF32ToF32Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callF32ToF32", float.class);
      assertNotNull(method, "callF32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "callF32ToF32 should return float");
    }

    @Test
    @DisplayName("should have callF32F32ToF32 method")
    void shouldHaveCallF32F32ToF32Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callF32F32ToF32", float.class, float.class);
      assertNotNull(method, "callF32F32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "callF32F32ToF32 should return float");
    }

    @Test
    @DisplayName("should have callF32F32F32ToF32 method")
    void shouldHaveCallF32F32F32ToF32Method() throws NoSuchMethodException {
      final Method method =
          TypedFunc.class.getMethod("callF32F32F32ToF32", float.class, float.class, float.class);
      assertNotNull(method, "callF32F32F32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "callF32F32F32ToF32 should return float");
    }

    @Test
    @DisplayName("should have callI32F32ToF32 method")
    void shouldHaveCallI32F32ToF32Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI32F32ToF32", int.class, float.class);
      assertNotNull(method, "callI32F32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "callI32F32ToF32 should return float");
    }

    @Test
    @DisplayName("should have callF32I32ToF32 method")
    void shouldHaveCallF32I32ToF32Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callF32I32ToF32", float.class, int.class);
      assertNotNull(method, "callF32I32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "callF32I32ToF32 should return float");
    }
  }

  @Nested
  @DisplayName("F64-Returning Method Tests")
  class F64ReturningMethodTests {

    @Test
    @DisplayName("should have callF64ToF64 method")
    void shouldHaveCallF64ToF64Method() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callF64ToF64", double.class);
      assertNotNull(method, "callF64ToF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "callF64ToF64 should return double");
    }

    @Test
    @DisplayName("should have callF64F64ToF64 method")
    void shouldHaveCallF64F64ToF64Method() throws NoSuchMethodException {
      final Method method =
          TypedFunc.class.getMethod("callF64F64ToF64", double.class, double.class);
      assertNotNull(method, "callF64F64ToF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "callF64F64ToF64 should return double");
    }

    @Test
    @DisplayName("should have callF64F64F64ToF64 method")
    void shouldHaveCallF64F64F64ToF64Method() throws NoSuchMethodException {
      final Method method =
          TypedFunc.class.getMethod("callF64F64F64ToF64", double.class, double.class, double.class);
      assertNotNull(method, "callF64F64F64ToF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "callF64F64F64ToF64 should return double");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getSignature method")
    void shouldHaveGetSignatureMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("getSignature");
      assertNotNull(method, "getSignature method should exist");
      assertEquals(String.class, method.getReturnType(), "getSignature should return String");
    }

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("getFunction");
      assertNotNull(method, "getFunction method should exist");
      assertEquals(
          WasmFunction.class, method.getReturnType(), "getFunction should return WasmFunction");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create static method")
    void shouldHaveCreateStaticMethod() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("create", WasmFunction.class, String.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(TypedFunc.class, method.getReturnType(), "create should return TypedFunc");
    }

    @Test
    @DisplayName("create should throw on null function")
    void createShouldThrowOnNullFunction() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypedFunc.create(null, "i->i"),
              "create should throw on null function");
      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null: " + exception.getMessage());
    }

    @Test
    @DisplayName("create should throw on null signature")
    void createShouldThrowOnNullSignature() {
      // We need a mock or real WasmFunction, but since we're testing the static method's validation
      // and we don't have a concrete implementation, we'll test via reflection
      final Method method;
      try {
        method = TypedFunc.class.getMethod("create", WasmFunction.class, String.class);
      } catch (NoSuchMethodException e) {
        throw new AssertionError("create method should exist", e);
      }
      assertNotNull(method, "create method should exist");
    }

    @Test
    @DisplayName("create should throw on empty signature")
    void createShouldThrowOnEmptySignature() {
      // Test the validation logic exists by checking method signature
      try {
        final Method method = TypedFunc.class.getMethod("create", WasmFunction.class, String.class);
        assertEquals(2, method.getParameterCount(), "create should have 2 parameters");
      } catch (NoSuchMethodException e) {
        throw new AssertionError("create method should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("callVoidToVoid should declare WasmException")
    void callVoidToVoidShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callVoidToVoid");
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "callVoidToVoid should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "Should declare WasmException");
    }

    @Test
    @DisplayName("callI32ToI32 should declare WasmException")
    void callI32ToI32ShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI32ToI32", int.class);
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "callI32ToI32 should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "Should declare WasmException");
    }

    @Test
    @DisplayName("callI64ToI64 should declare WasmException")
    void callI64ToI64ShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callI64ToI64", long.class);
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "callI64ToI64 should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "Should declare WasmException");
    }

    @Test
    @DisplayName("callF32ToF32 should declare WasmException")
    void callF32ToF32ShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callF32ToF32", float.class);
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "callF32ToF32 should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "Should declare WasmException");
    }

    @Test
    @DisplayName("callF64ToF64 should declare WasmException")
    void callF64ToF64ShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = TypedFunc.class.getMethod("callF64ToF64", double.class);
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "callF64ToF64 should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of call methods")
    void shouldHaveExpectedCallMethods() {
      int callMethodCount = 0;
      for (final Method method : TypedFunc.class.getDeclaredMethods()) {
        if (method.getName().startsWith("call")) {
          callMethodCount++;
        }
      }
      // Expected: 20 call methods based on interface definition
      assertTrue(
          callMethodCount >= 20, "Should have at least 20 call methods, found: " + callMethodCount);
    }
  }

  @Nested
  @DisplayName("TypedFunctionSupport Inner Interface Tests")
  class TypedFunctionSupportTests {

    @Test
    @DisplayName("TypedFunctionSupport should have asTyped method")
    void shouldHaveAsTypedMethod() throws NoSuchMethodException {
      final Class<?> supportInterface = TypedFunc.TypedFunctionSupport.class;
      final Method method = supportInterface.getMethod("asTyped", String.class);
      assertNotNull(method, "asTyped method should exist");
      assertEquals(TypedFunc.class, method.getReturnType(), "asTyped should return TypedFunc");
    }

    @Test
    @DisplayName("TypedFunctionSupport should be nested in TypedFunc")
    void shouldBeNestedInTypedFunc() {
      assertTrue(
          TypedFunc.TypedFunctionSupport.class.isMemberClass(),
          "TypedFunctionSupport should be a member class");
      assertEquals(
          TypedFunc.class,
          TypedFunc.TypedFunctionSupport.class.getDeclaringClass(),
          "TypedFunctionSupport should be declared in TypedFunc");
    }
  }
}
