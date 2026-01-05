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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncRuntime} interface.
 *
 * <p>AsyncRuntime provides infrastructure for executing WebAssembly functions asynchronously.
 */
@DisplayName("AsyncRuntime Tests")
class AsyncRuntimeTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(AsyncRuntime.class.getModifiers()), "AsyncRuntime should be public");
      assertTrue(AsyncRuntime.class.isInterface(), "AsyncRuntime should be an interface");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(AsyncRuntime.class),
          "AsyncRuntime should extend Closeable");
    }
  }

  @Nested
  @DisplayName("OperationStatus Enum Tests")
  class OperationStatusEnumTests {

    @Test
    @DisplayName("should have OperationStatus nested enum")
    void shouldHaveOperationStatusNestedEnum() {
      final var nestedClasses = AsyncRuntime.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("OperationStatus")) {
          found = true;
          assertTrue(nestedClass.isEnum(), "OperationStatus should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have OperationStatus nested enum");
    }

    @Test
    @DisplayName("OperationStatus should have all expected values")
    void operationStatusShouldHaveAllExpectedValues() {
      final AsyncRuntime.OperationStatus[] values = AsyncRuntime.OperationStatus.values();
      assertEquals(6, values.length, "OperationStatus should have 6 values");

      // Check specific values exist
      assertNotNull(AsyncRuntime.OperationStatus.valueOf("PENDING"), "PENDING should exist");
      assertNotNull(AsyncRuntime.OperationStatus.valueOf("RUNNING"), "RUNNING should exist");
      assertNotNull(AsyncRuntime.OperationStatus.valueOf("COMPLETED"), "COMPLETED should exist");
      assertNotNull(AsyncRuntime.OperationStatus.valueOf("FAILED"), "FAILED should exist");
      assertNotNull(AsyncRuntime.OperationStatus.valueOf("CANCELLED"), "CANCELLED should exist");
      assertNotNull(AsyncRuntime.OperationStatus.valueOf("TIMED_OUT"), "TIMED_OUT should exist");
    }

    @Test
    @DisplayName("OperationStatus values should be in expected order")
    void operationStatusValuesShouldBeInExpectedOrder() {
      final AsyncRuntime.OperationStatus[] values = AsyncRuntime.OperationStatus.values();
      assertEquals(
          AsyncRuntime.OperationStatus.PENDING, values[0], "First value should be PENDING");
      assertEquals(
          AsyncRuntime.OperationStatus.RUNNING, values[1], "Second value should be RUNNING");
      assertEquals(
          AsyncRuntime.OperationStatus.COMPLETED, values[2], "Third value should be COMPLETED");
      assertEquals(AsyncRuntime.OperationStatus.FAILED, values[3], "Fourth value should be FAILED");
      assertEquals(
          AsyncRuntime.OperationStatus.CANCELLED, values[4], "Fifth value should be CANCELLED");
      assertEquals(
          AsyncRuntime.OperationStatus.TIMED_OUT, values[5], "Sixth value should be TIMED_OUT");
    }
  }

  @Nested
  @DisplayName("AsyncResult Nested Class Tests")
  class AsyncResultNestedClassTests {

    @Test
    @DisplayName("should have AsyncResult nested class")
    void shouldHaveAsyncResultNestedClass() {
      final var nestedClasses = AsyncRuntime.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("AsyncResult")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "AsyncResult should be a class");
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "AsyncResult should be final");
          break;
        }
      }
      assertTrue(found, "Should have AsyncResult nested class");
    }

    @Test
    @DisplayName("AsyncResult should have all required methods")
    void asyncResultShouldHaveAllRequiredMethods() throws NoSuchMethodException {
      final Class<?> asyncResultClass = AsyncRuntime.AsyncResult.class;

      assertNotNull(asyncResultClass.getMethod("getStatus"), "getStatus method should exist");
      assertNotNull(
          asyncResultClass.getMethod("getStatusCode"), "getStatusCode method should exist");
      assertNotNull(asyncResultClass.getMethod("getMessage"), "getMessage method should exist");
      assertNotNull(asyncResultClass.getMethod("getResult"), "getResult method should exist");
      assertNotNull(asyncResultClass.getMethod("isSuccess"), "isSuccess method should exist");
    }

    @Test
    @DisplayName("AsyncResult constructor should accept all parameters")
    void asyncResultConstructorShouldAcceptAllParameters() {
      final AsyncRuntime.AsyncResult result =
          new AsyncRuntime.AsyncResult(
              AsyncRuntime.OperationStatus.COMPLETED, 0, "Success", "result-value");

      assertEquals(
          AsyncRuntime.OperationStatus.COMPLETED, result.getStatus(), "Status should match");
      assertEquals(0, result.getStatusCode(), "Status code should match");
      assertEquals("Success", result.getMessage(), "Message should match");
      assertEquals("result-value", result.getResult(), "Result should match");
    }

    @Test
    @DisplayName("AsyncResult isSuccess should return true for COMPLETED with code 0")
    void asyncResultIsSuccessShouldReturnTrueForCompletedWithCode0() {
      final AsyncRuntime.AsyncResult result =
          new AsyncRuntime.AsyncResult(AsyncRuntime.OperationStatus.COMPLETED, 0, "Success", null);

      assertTrue(result.isSuccess(), "isSuccess should return true");
    }

    @Test
    @DisplayName("AsyncResult isSuccess should return false for FAILED")
    void asyncResultIsSuccessShouldReturnFalseForFailed() {
      final AsyncRuntime.AsyncResult result =
          new AsyncRuntime.AsyncResult(AsyncRuntime.OperationStatus.FAILED, 1, "Error", null);

      assertFalse(result.isSuccess(), "isSuccess should return false for FAILED");
    }

    @Test
    @DisplayName("AsyncResult isSuccess should return false for non-zero code")
    void asyncResultIsSuccessShouldReturnFalseForNonZeroCode() {
      final AsyncRuntime.AsyncResult result =
          new AsyncRuntime.AsyncResult(
              AsyncRuntime.OperationStatus.COMPLETED, 1, "Completed with error", null);

      assertFalse(result.isSuccess(), "isSuccess should return false for non-zero code");
    }

    @Test
    @DisplayName("AsyncResult toString should include key fields")
    void asyncResultToStringShouldIncludeKeyFields() {
      final AsyncRuntime.AsyncResult result =
          new AsyncRuntime.AsyncResult(
              AsyncRuntime.OperationStatus.RUNNING, 42, "In progress", null);

      final String str = result.toString();
      assertTrue(str.contains("RUNNING"), "Should contain status");
      assertTrue(str.contains("42"), "Should contain status code");
      assertTrue(str.contains("In progress"), "Should contain message");
    }
  }

  @Nested
  @DisplayName("Interface Method Tests")
  class InterfaceMethodTests {

    @Test
    @DisplayName("should have initialize method")
    void shouldHaveInitializeMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntime.class.getMethod("initialize");
      assertNotNull(method, "initialize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");

      // Check it throws WasmException
      final Class<?>[] exceptions = method.getExceptionTypes();
      boolean throwsWasmException = false;
      for (Class<?> ex : exceptions) {
        if (ex.equals(WasmException.class)) {
          throwsWasmException = true;
          break;
        }
      }
      assertTrue(throwsWasmException, "initialize should throw WasmException");
    }

    @Test
    @DisplayName("should have isInitialized method")
    void shouldHaveIsInitializedMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntime.class.getMethod("isInitialized");
      assertNotNull(method, "isInitialized method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRuntimeInfo method")
    void shouldHaveGetRuntimeInfoMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntime.class.getMethod("getRuntimeInfo");
      assertNotNull(method, "getRuntimeInfo method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have executeAsync method")
    void shouldHaveExecuteAsyncMethod() throws NoSuchMethodException {
      final Method method =
          AsyncRuntime.class.getMethod(
              "executeAsync", long.class, String.class, Object[].class, long.class, Consumer.class);
      assertNotNull(method, "executeAsync method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have compileAsync method")
    void shouldHaveCompileAsyncMethod() throws NoSuchMethodException {
      final Method method =
          AsyncRuntime.class.getMethod(
              "compileAsync", byte[].class, long.class, Consumer.class, Consumer.class);
      assertNotNull(method, "compileAsync method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have cancelOperation method")
    void shouldHaveCancelOperationMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntime.class.getMethod("cancelOperation", long.class);
      assertNotNull(method, "cancelOperation method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getOperationStatus method")
    void shouldHaveGetOperationStatusMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntime.class.getMethod("getOperationStatus", long.class);
      assertNotNull(method, "getOperationStatus method should exist");
      assertEquals(
          AsyncRuntime.OperationStatus.class,
          method.getReturnType(),
          "Should return OperationStatus");
    }

    @Test
    @DisplayName("should have waitForOperation method")
    void shouldHaveWaitForOperationMethod() throws NoSuchMethodException {
      final Method method =
          AsyncRuntime.class.getMethod("waitForOperation", long.class, long.class);
      assertNotNull(method, "waitForOperation method should exist");
      assertEquals(
          AsyncRuntime.OperationStatus.class,
          method.getReturnType(),
          "Should return OperationStatus");
    }

    @Test
    @DisplayName("should have getActiveOperationCount method")
    void shouldHaveGetActiveOperationCountMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntime.class.getMethod("getActiveOperationCount");
      assertNotNull(method, "getActiveOperationCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntime.class.getMethod("shutdown");
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = AsyncRuntime.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
