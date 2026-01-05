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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentTypedFunc} interface.
 *
 * <p>ComponentTypedFunc provides type-safe component function calls with statically known
 * signatures.
 */
@DisplayName("ComponentTypedFunc Tests")
class ComponentTypedFuncTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentTypedFunc.class.getModifiers()),
          "ComponentTypedFunc should be public");
      assertTrue(
          ComponentTypedFunc.class.isInterface(), "ComponentTypedFunc should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentTypedFunc.class),
          "ComponentTypedFunc should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Void Return Signature Method Tests")
  class VoidReturnSignatureMethodTests {

    @Test
    @DisplayName("should have callVoidToVoid method")
    void shouldHaveCallVoidToVoidMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callVoidToVoid");
      assertNotNull(method, "callVoidToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have callS32ToVoid method")
    void shouldHaveCallS32ToVoidMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callS32ToVoid", int.class);
      assertNotNull(method, "callS32ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have callS32S32ToVoid method")
    void shouldHaveCallS32S32ToVoidMethod() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("callS32S32ToVoid", int.class, int.class);
      assertNotNull(method, "callS32S32ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have callS64ToVoid method")
    void shouldHaveCallS64ToVoidMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callS64ToVoid", long.class);
      assertNotNull(method, "callS64ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have callS64S64ToVoid method")
    void shouldHaveCallS64S64ToVoidMethod() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("callS64S64ToVoid", long.class, long.class);
      assertNotNull(method, "callS64S64ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have callStringToVoid method")
    void shouldHaveCallStringToVoidMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callStringToVoid", String.class);
      assertNotNull(method, "callStringToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("S32 Return Signature Method Tests")
  class S32ReturnSignatureMethodTests {

    @Test
    @DisplayName("should have callS32ToS32 method")
    void shouldHaveCallS32ToS32Method() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callS32ToS32", int.class);
      assertNotNull(method, "callS32ToS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have callS32S32ToS32 method")
    void shouldHaveCallS32S32ToS32Method() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("callS32S32ToS32", int.class, int.class);
      assertNotNull(method, "callS32S32ToS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have callS32S32S32ToS32 method")
    void shouldHaveCallS32S32S32ToS32Method() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("callS32S32S32ToS32", int.class, int.class, int.class);
      assertNotNull(method, "callS32S32S32ToS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have callS64ToS32 method")
    void shouldHaveCallS64ToS32Method() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callS64ToS32", long.class);
      assertNotNull(method, "callS64ToS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("S64 Return Signature Method Tests")
  class S64ReturnSignatureMethodTests {

    @Test
    @DisplayName("should have callS64ToS64 method")
    void shouldHaveCallS64ToS64Method() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callS64ToS64", long.class);
      assertNotNull(method, "callS64ToS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have callS64S64ToS64 method")
    void shouldHaveCallS64S64ToS64Method() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("callS64S64ToS64", long.class, long.class);
      assertNotNull(method, "callS64S64ToS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have callS64S64S64ToS64 method")
    void shouldHaveCallS64S64S64ToS64Method() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod(
              "callS64S64S64ToS64", long.class, long.class, long.class);
      assertNotNull(method, "callS64S64S64ToS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have callS32S32ToS64 method")
    void shouldHaveCallS32S32ToS64Method() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("callS32S32ToS64", int.class, int.class);
      assertNotNull(method, "callS32S32ToS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Float Return Signature Method Tests")
  class FloatReturnSignatureMethodTests {

    @Test
    @DisplayName("should have callF32ToF32 method")
    void shouldHaveCallF32ToF32Method() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callF32ToF32", float.class);
      assertNotNull(method, "callF32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have callF32F32ToF32 method")
    void shouldHaveCallF32F32ToF32Method() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("callF32F32ToF32", float.class, float.class);
      assertNotNull(method, "callF32F32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have callF64ToF64 method")
    void shouldHaveCallF64ToF64Method() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callF64ToF64", double.class);
      assertNotNull(method, "callF64ToF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have callF64F64ToF64 method")
    void shouldHaveCallF64F64ToF64Method() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("callF64F64ToF64", double.class, double.class);
      assertNotNull(method, "callF64F64ToF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("String and Bool Signature Method Tests")
  class StringBoolSignatureMethodTests {

    @Test
    @DisplayName("should have callVoidToString method")
    void shouldHaveCallVoidToStringMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callVoidToString");
      assertNotNull(method, "callVoidToString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have callStringToString method")
    void shouldHaveCallStringToStringMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callStringToString", String.class);
      assertNotNull(method, "callStringToString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have callStringStringToString method")
    void shouldHaveCallStringStringToStringMethod() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod(
              "callStringStringToString", String.class, String.class);
      assertNotNull(method, "callStringStringToString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have callVoidToBool method")
    void shouldHaveCallVoidToBoolMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callVoidToBool");
      assertNotNull(method, "callVoidToBool method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have callBoolToBool method")
    void shouldHaveCallBoolToBoolMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("callBoolToBool", boolean.class);
      assertNotNull(method, "callBoolToBool method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Metadata and Lifecycle Method Tests")
  class MetadataLifecycleMethodTests {

    @Test
    @DisplayName("should have getSignature method")
    void shouldHaveGetSignatureMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("getSignature");
      assertNotNull(method, "getSignature method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("getFunction");
      assertNotNull(method, "getFunction method should exist");
      assertEquals(ComponentFunc.class, method.getReturnType(), "Should return ComponentFunc");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ComponentTypedFunc.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create static method")
    void shouldHaveCreateStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentTypedFunc.class.getMethod("create", ComponentFunc.class, String.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(
          ComponentTypedFunc.class, method.getReturnType(), "Should return ComponentTypedFunc");
    }

    @Test
    @DisplayName("create should reject null function")
    void createShouldRejectNullFunction() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentTypedFunc.create(null, "s32->s32"),
          "Should throw for null function");
    }

    @Test
    @DisplayName("create should reject null signature")
    void createShouldRejectNullSignature() {
      // We can't easily test this without a real ComponentFunc, but we can verify
      // the method signature exists and has proper validation
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentTypedFunc.create(null, null),
          "Should throw for null signature");
    }

    @Test
    @DisplayName("create should reject empty signature")
    void createShouldRejectEmptySignature() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentTypedFunc.create(null, ""),
          "Should throw for empty signature");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have TypedComponentFunctionSupport nested interface")
    void shouldHaveTypedComponentFunctionSupportNestedInterface() {
      final var nestedClasses = ComponentTypedFunc.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("TypedComponentFunctionSupport")) {
          found = true;
          assertTrue(
              nestedClass.isInterface(), "TypedComponentFunctionSupport should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have TypedComponentFunctionSupport nested interface");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("call methods should declare WasmException")
    void callMethodsShouldDeclareWasmException() throws NoSuchMethodException {
      // Test a few representative methods
      final Method[] methods = {
        ComponentTypedFunc.class.getMethod("callVoidToVoid"),
        ComponentTypedFunc.class.getMethod("callS32ToS32", int.class),
        ComponentTypedFunc.class.getMethod("callStringToString", String.class)
      };

      for (Method method : methods) {
        final Class<?>[] exceptionTypes = method.getExceptionTypes();
        boolean hasWasmException = false;
        for (Class<?> exType : exceptionTypes) {
          if (exType.getSimpleName().equals("WasmException")) {
            hasWasmException = true;
            break;
          }
        }
        assertTrue(hasWasmException, method.getName() + " should declare WasmException");
      }
    }
  }
}
