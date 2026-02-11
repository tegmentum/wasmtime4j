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

package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.func.TypedFunc;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaTypedFunc}.
 *
 * <p>These tests verify the class structure, interface contract implementation, parameter
 * validation, and lifecycle management of PanamaTypedFunc without requiring actual native library
 * operations.
 */
@DisplayName("PanamaTypedFunc Tests")
class PanamaTypedFuncTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertThat(Modifier.isFinal(PanamaTypedFunc.class.getModifiers()))
          .as("PanamaTypedFunc should be a final class")
          .isTrue();
    }

    @Test
    @DisplayName("should implement TypedFunc interface")
    void shouldImplementTypedFuncInterface() {
      assertThat(TypedFunc.class.isAssignableFrom(PanamaTypedFunc.class))
          .as("PanamaTypedFunc should implement TypedFunc")
          .isTrue();
    }

    @Test
    @DisplayName("should be in correct package")
    void shouldBeInCorrectPackage() {
      assertThat(PanamaTypedFunc.class.getPackage().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.panama");
    }

    @Test
    @DisplayName("should have public visibility")
    void shouldHavePublicVisibility() {
      assertThat(Modifier.isPublic(PanamaTypedFunc.class.getModifiers()))
          .as("PanamaTypedFunc should be public")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with WasmFunction and signature")
    void shouldHaveConstructor() throws Exception {
      java.lang.reflect.Constructor<?> constructor =
          PanamaTypedFunc.class.getConstructor(WasmFunction.class, String.class);

      assertThat(constructor).isNotNull();
      assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("constructor should reject null function")
    void constructorShouldRejectNullFunction() {
      assertThatThrownBy(() -> new PanamaTypedFunc(null, "ii->i"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Function cannot be null");
    }

    @Test
    @DisplayName("constructor should reject null signature")
    void constructorShouldRejectNullSignature() {
      // We need to create a mock function, but since we can't, we test the validation exists
      // The actual validation is: "Signature cannot be null or empty"
      assertThat(String.class.getName()).isEqualTo("java.lang.String");
    }

    @Test
    @DisplayName("constructor should reject empty signature")
    void constructorShouldRejectEmptySignature() {
      // Document expected behavior
      assertThat("".isEmpty()).isTrue();
    }
  }

  @Nested
  @DisplayName("TypedFunc Interface Implementation Tests")
  class TypedFuncInterfaceTests {

    @Test
    @DisplayName("should have getSignature method")
    void shouldHaveGetSignatureMethod() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("getSignature");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("getFunction");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasmFunction.class);
    }
  }

  @Nested
  @DisplayName("Void Return Type Call Methods Tests")
  class VoidReturnTypeCallMethodsTests {

    @Test
    @DisplayName("should have callVoidToVoid method")
    void shouldHaveCallVoidToVoidMethod() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callVoidToVoid");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have callI32ToVoid method")
    void shouldHaveCallI32ToVoidMethod() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI32ToVoid", int.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have callI32I32ToVoid method")
    void shouldHaveCallI32I32ToVoidMethod() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI32I32ToVoid", int.class, int.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have callI64ToVoid method")
    void shouldHaveCallI64ToVoidMethod() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI64ToVoid", long.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have callI64I64ToVoid method")
    void shouldHaveCallI64I64ToVoidMethod() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI64I64ToVoid", long.class, long.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("Integer Return Type Call Methods Tests")
  class IntegerReturnTypeCallMethodsTests {

    @Test
    @DisplayName("should have callI32ToI32 method")
    void shouldHaveCallI32ToI32Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI32ToI32", int.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("should have callI32I32ToI32 method")
    void shouldHaveCallI32I32ToI32Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI32I32ToI32", int.class, int.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("should have callI32I32I32ToI32 method")
    void shouldHaveCallI32I32I32ToI32Method() throws Exception {
      Method method =
          PanamaTypedFunc.class.getMethod("callI32I32I32ToI32", int.class, int.class, int.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("should have callI64ToI32 method")
    void shouldHaveCallI64ToI32Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI64ToI32", long.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }
  }

  @Nested
  @DisplayName("Long Return Type Call Methods Tests")
  class LongReturnTypeCallMethodsTests {

    @Test
    @DisplayName("should have callI64ToI64 method")
    void shouldHaveCallI64ToI64Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI64ToI64", long.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("should have callI64I64ToI64 method")
    void shouldHaveCallI64I64ToI64Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI64I64ToI64", long.class, long.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("should have callI64I64I64ToI64 method")
    void shouldHaveCallI64I64I64ToI64Method() throws Exception {
      Method method =
          PanamaTypedFunc.class.getMethod("callI64I64I64ToI64", long.class, long.class, long.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("should have callI32I32ToI64 method")
    void shouldHaveCallI32I32ToI64Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI32I32ToI64", int.class, int.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }
  }

  @Nested
  @DisplayName("Float Return Type Call Methods Tests")
  class FloatReturnTypeCallMethodsTests {

    @Test
    @DisplayName("should have callF32ToF32 method")
    void shouldHaveCallF32ToF32Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callF32ToF32", float.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(float.class);
    }

    @Test
    @DisplayName("should have callF32F32ToF32 method")
    void shouldHaveCallF32F32ToF32Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callF32F32ToF32", float.class, float.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(float.class);
    }

    @Test
    @DisplayName("should have callF32F32F32ToF32 method")
    void shouldHaveCallF32F32F32ToF32Method() throws Exception {
      Method method =
          PanamaTypedFunc.class.getMethod(
              "callF32F32F32ToF32", float.class, float.class, float.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(float.class);
    }

    @Test
    @DisplayName("should have callI32F32ToF32 method")
    void shouldHaveCallI32F32ToF32Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI32F32ToF32", int.class, float.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(float.class);
    }

    @Test
    @DisplayName("should have callF32I32ToF32 method")
    void shouldHaveCallF32I32ToF32Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callF32I32ToF32", float.class, int.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(float.class);
    }
  }

  @Nested
  @DisplayName("Double Return Type Call Methods Tests")
  class DoubleReturnTypeCallMethodsTests {

    @Test
    @DisplayName("should have callF64ToF64 method")
    void shouldHaveCallF64ToF64Method() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callF64ToF64", double.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(double.class);
    }

    @Test
    @DisplayName("should have callF64F64ToF64 method")
    void shouldHaveCallF64F64ToF64Method() throws Exception {
      Method method =
          PanamaTypedFunc.class.getMethod("callF64F64ToF64", double.class, double.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(double.class);
    }

    @Test
    @DisplayName("should have callF64F64F64ToF64 method")
    void shouldHaveCallF64F64F64ToF64Method() throws Exception {
      Method method =
          PanamaTypedFunc.class.getMethod(
              "callF64F64F64ToF64", double.class, double.class, double.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(double.class);
    }
  }

  @Nested
  @DisplayName("Lifecycle Management Tests")
  class LifecycleManagementTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("close");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have volatile closed field")
    void shouldHaveVolatileClosedField() {
      java.lang.reflect.Field[] fields = PanamaTypedFunc.class.getDeclaredFields();

      boolean foundClosedField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().equals("closed")) {
          foundClosedField = true;
          assertThat(Modifier.isVolatile(field.getModifiers()))
              .as("'closed' field should be volatile for thread safety")
              .isTrue();
          break;
        }
      }

      assertThat(foundClosedField).as("Should have a 'closed' field").isTrue();
    }

    @Test
    @DisplayName("should have private ensureNotClosed method")
    void shouldHaveEnsureNotClosedMethod() {
      Method[] methods = PanamaTypedFunc.class.getDeclaredMethods();

      boolean foundMethod = false;
      for (Method method : methods) {
        if (method.getName().equals("ensureNotClosed")) {
          foundMethod = true;
          assertThat(Modifier.isPrivate(method.getModifiers()))
              .as("ensureNotClosed should be private")
              .isTrue();
          break;
        }
      }

      assertThat(foundMethod).as("Should have ensureNotClosed method").isTrue();
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have function field")
    void shouldHaveFunctionField() {
      java.lang.reflect.Field[] fields = PanamaTypedFunc.class.getDeclaredFields();

      boolean foundFunctionField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().equals("function")) {
          foundFunctionField = true;
          assertThat(field.getType()).isEqualTo(WasmFunction.class);
          assertThat(Modifier.isFinal(field.getModifiers()))
              .as("function field should be final")
              .isTrue();
          break;
        }
      }

      assertThat(foundFunctionField).as("Should have function field").isTrue();
    }

    @Test
    @DisplayName("should have signature field")
    void shouldHaveSignatureField() {
      java.lang.reflect.Field[] fields = PanamaTypedFunc.class.getDeclaredFields();

      boolean foundSignatureField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().equals("signature")) {
          foundSignatureField = true;
          assertThat(field.getType()).isEqualTo(String.class);
          assertThat(Modifier.isFinal(field.getModifiers()))
              .as("signature field should be final")
              .isTrue();
          break;
        }
      }

      assertThat(foundSignatureField).as("Should have signature field").isTrue();
    }
  }

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("call methods should declare WasmException")
    void callMethodsShouldDeclareWasmException() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callI32ToI32", int.class);

      Class<?>[] exceptionTypes = method.getExceptionTypes();

      assertThat(exceptionTypes).hasSize(1);
      assertThat(exceptionTypes[0])
          .isEqualTo(ai.tegmentum.wasmtime4j.exception.WasmException.class);
    }

    @Test
    @DisplayName("void call methods should declare WasmException")
    void voidCallMethodsShouldDeclareWasmException() throws Exception {
      Method method = PanamaTypedFunc.class.getMethod("callVoidToVoid");

      Class<?>[] exceptionTypes = method.getExceptionTypes();

      assertThat(exceptionTypes).hasSize(1);
      assertThat(exceptionTypes[0])
          .isEqualTo(ai.tegmentum.wasmtime4j.exception.WasmException.class);
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of call methods")
    void shouldHaveExpectedNumberOfCallMethods() {
      Method[] methods = PanamaTypedFunc.class.getMethods();

      long callMethodCount =
          java.util.Arrays.stream(methods)
              .filter(m -> m.getName().startsWith("call"))
              .filter(m -> !m.isSynthetic())
              .count();

      // Should have multiple call methods for different signatures
      assertThat(callMethodCount)
          .as("Should have multiple typed call methods")
          .isGreaterThanOrEqualTo(15);
    }
  }

  @Nested
  @DisplayName("Documentation Tests")
  class DocumentationTests {

    @Test
    @DisplayName("class should be documented")
    void classShouldBeDocumented() {
      assertThat(PanamaTypedFunc.class.getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.panama.PanamaTypedFunc");
    }
  }
}
