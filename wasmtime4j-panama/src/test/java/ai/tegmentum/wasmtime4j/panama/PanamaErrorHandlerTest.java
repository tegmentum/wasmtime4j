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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaErrorHandler} class.
 *
 * <p>PanamaErrorHandler provides utility methods for mapping native errors to Java exceptions.
 */
@DisplayName("PanamaErrorHandler Tests")
class PanamaErrorHandlerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaErrorHandler.class.getModifiers()),
          "PanamaErrorHandler should be public");
      assertTrue(
          Modifier.isFinal(PanamaErrorHandler.class.getModifiers()),
          "PanamaErrorHandler should be final");
    }

    @Test
    @DisplayName("should have public constructor")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      var constructor = PanamaErrorHandler.class.getConstructor();
      assertNotNull(constructor, "Public constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Error Description Method Tests")
  class ErrorDescriptionMethodTests {

    @Test
    @DisplayName("should have getErrorDescription static method")
    void shouldHaveGetErrorDescriptionMethod() throws NoSuchMethodException {
      final Method method = PanamaErrorHandler.class.getMethod("getErrorDescription", int.class);
      assertNotNull(method, "getErrorDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Error Recovery Method Tests")
  class ErrorRecoveryMethodTests {

    @Test
    @DisplayName("should have isRecoverableError static method")
    void shouldHaveIsRecoverableErrorMethod() throws NoSuchMethodException {
      final Method method = PanamaErrorHandler.class.getMethod("isRecoverableError", int.class);
      assertNotNull(method, "isRecoverableError method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Error Check Method Tests")
  class ErrorCheckMethodTests {

    @Test
    @DisplayName("should have checkErrorCode with operation string")
    void shouldHaveCheckErrorCodeWithOperationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("checkErrorCode", int.class, String.class);
      assertNotNull(method, "checkErrorCode method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have checkErrorCode with format and args")
    void shouldHaveCheckErrorCodeWithFormatMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod(
              "checkErrorCode", int.class, String.class, Object[].class);
      assertNotNull(method, "checkErrorCode method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have checkErrorStruct method")
    void shouldHaveCheckErrorStructMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod(
              "checkErrorStruct", Object.class, String.class, String.class);
      assertNotNull(method, "checkErrorStruct method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have safeCheckError method")
    void shouldHaveSafeCheckErrorMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod(
              "safeCheckError", int.class, String.class, String.class);
      assertNotNull(method, "safeCheckError method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Error Message Creation Method Tests")
  class ErrorMessageCreationMethodTests {

    @Test
    @DisplayName("should have createDetailedErrorMessage with integer error code")
    void shouldHaveCreateDetailedErrorMessageWithIntegerMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod(
              "createDetailedErrorMessage", String.class, Integer.class, String.class);
      assertNotNull(method, "createDetailedErrorMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have createDetailedErrorMessage with string context")
    void shouldHaveCreateDetailedErrorMessageWithStringContextMethod()
        throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod(
              "createDetailedErrorMessage", String.class, String.class, String.class);
      assertNotNull(method, "createDetailedErrorMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have requireNonNegative for long")
    void shouldHaveRequireNonNegativeLongMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("requireNonNegative", long.class, String.class);
      assertNotNull(method, "requireNonNegative method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have requireNonNegative for int")
    void shouldHaveRequireNonNegativeIntMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("requireNonNegative", int.class, String.class);
      assertNotNull(method, "requireNonNegative method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have requireNotEmpty method")
    void shouldHaveRequireNotEmptyMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("requireNotEmpty", String.class, String.class);
      assertNotNull(method, "requireNotEmpty method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have requireNonEmpty method")
    void shouldHaveRequireNonEmptyMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("requireNonEmpty", String.class, String.class);
      assertNotNull(method, "requireNonEmpty method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have requireValidPointer method")
    void shouldHaveRequireValidPointerMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod(
              "requireValidPointer", MemorySegment.class, String.class);
      assertNotNull(method, "requireValidPointer method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have requirePositive for long")
    void shouldHaveRequirePositiveLongMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("requirePositive", long.class, String.class);
      assertNotNull(method, "requirePositive method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have requirePositive for int")
    void shouldHaveRequirePositiveIntMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("requirePositive", int.class, String.class);
      assertNotNull(method, "requirePositive method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have requireValidIndex method")
    void shouldHaveRequireValidIndexMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod(
              "requireValidIndex", int.class, int.class, String.class);
      assertNotNull(method, "requireValidIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have requireSuccess method")
    void shouldHaveRequireSuccessMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("requireSuccess", Object.class, String.class);
      assertNotNull(method, "requireSuccess method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return generic type");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Exception Mapping Method Tests")
  class ExceptionMappingMethodTests {

    @Test
    @DisplayName("should have mapToWasmException with error code")
    void shouldHaveMapToWasmExceptionWithErrorCodeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("mapToWasmException", int.class, String.class);
      assertNotNull(method, "mapToWasmException method should exist");
      assertEquals(WasmException.class, method.getReturnType(), "Should return WasmException");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have mapToWasmException with throwable")
    void shouldHaveMapToWasmExceptionWithThrowableMethod() throws NoSuchMethodException {
      final Method method =
          PanamaErrorHandler.class.getMethod("mapToWasmException", Throwable.class, String.class);
      assertNotNull(method, "mapToWasmException method should exist");
      assertEquals(WasmException.class, method.getReturnType(), "Should return WasmException");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }
}
