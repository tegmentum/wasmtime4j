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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncHostFunction} interface.
 *
 * <p>AsyncHostFunction allows host functions to perform asynchronous operations without blocking
 * the WebAssembly execution thread.
 */
@DisplayName("AsyncHostFunction Tests")
class AsyncHostFunctionTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(AsyncHostFunction.class.getModifiers()),
          "AsyncHostFunction should be public");
      assertTrue(AsyncHostFunction.class.isInterface(), "AsyncHostFunction should be an interface");
    }

    @Test
    @DisplayName("should be annotated with FunctionalInterface")
    void shouldBeAnnotatedWithFunctionalInterface() {
      assertTrue(
          AsyncHostFunction.class.isAnnotationPresent(FunctionalInterface.class),
          "AsyncHostFunction should be annotated with @FunctionalInterface");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have BlockingOperation nested interface")
    void shouldHaveBlockingOperationNestedInterface() {
      final var nestedClasses = AsyncHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("BlockingOperation")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "BlockingOperation should be an interface");
          assertTrue(
              nestedClass.isAnnotationPresent(FunctionalInterface.class),
              "BlockingOperation should be a functional interface");
          break;
        }
      }
      assertTrue(found, "Should have BlockingOperation nested interface");
    }

    @Test
    @DisplayName("should have AsyncOperation nested interface")
    void shouldHaveAsyncOperationNestedInterface() {
      final var nestedClasses = AsyncHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("AsyncOperation")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "AsyncOperation should be an interface");
          assertTrue(
              nestedClass.isAnnotationPresent(FunctionalInterface.class),
              "AsyncOperation should be a functional interface");
          break;
        }
      }
      assertTrue(found, "Should have AsyncOperation nested interface");
    }

    @Test
    @DisplayName("should have SimpleAsyncHostFunction nested class")
    void shouldHaveSimpleAsyncHostFunctionNestedClass() {
      final var nestedClasses = AsyncHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("SimpleAsyncHostFunction")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "SimpleAsyncHostFunction should be a class");
          assertTrue(
              AsyncHostFunction.class.isAssignableFrom(nestedClass),
              "SimpleAsyncHostFunction should implement AsyncHostFunction");
          break;
        }
      }
      assertTrue(found, "Should have SimpleAsyncHostFunction nested class");
    }

    @Test
    @DisplayName("should have BlockingAsyncHostFunction nested class")
    void shouldHaveBlockingAsyncHostFunctionNestedClass() {
      final var nestedClasses = AsyncHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("BlockingAsyncHostFunction")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "BlockingAsyncHostFunction should be a class");
          assertTrue(
              AsyncHostFunction.class.isAssignableFrom(nestedClass),
              "BlockingAsyncHostFunction should implement AsyncHostFunction");
          break;
        }
      }
      assertTrue(found, "Should have BlockingAsyncHostFunction nested class");
    }

    @Test
    @DisplayName("should have ExecutorAsyncHostFunction nested class")
    void shouldHaveExecutorAsyncHostFunctionNestedClass() {
      final var nestedClasses = AsyncHostFunction.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ExecutorAsyncHostFunction")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "ExecutorAsyncHostFunction should be a class");
          assertTrue(
              AsyncHostFunction.class.isAssignableFrom(nestedClass),
              "ExecutorAsyncHostFunction should implement AsyncHostFunction");
          break;
        }
      }
      assertTrue(found, "Should have ExecutorAsyncHostFunction nested class");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have call method")
    void shouldHaveCallMethod() throws NoSuchMethodException {
      final Method method =
          AsyncHostFunction.class.getMethod(
              "call", ai.tegmentum.wasmtime4j.Function.class, WasmValue[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have getName default method")
    void shouldHaveGetNameDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertTrue(method.isDefault(), "getName should be a default method");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getModuleName default method")
    void shouldHaveGetModuleNameDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName method should exist");
      assertTrue(method.isDefault(), "getModuleName should be a default method");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getParameterTypes default method")
    void shouldHaveGetParameterTypesDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("getParameterTypes");
      assertNotNull(method, "getParameterTypes method should exist");
      assertTrue(method.isDefault(), "getParameterTypes should be a default method");
      assertEquals(Class[].class, method.getReturnType(), "Should return Class[]");
    }

    @Test
    @DisplayName("should have getReturnTypes default method")
    void shouldHaveGetReturnTypesDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("getReturnTypes");
      assertNotNull(method, "getReturnTypes method should exist");
      assertTrue(method.isDefault(), "getReturnTypes should be a default method");
      assertEquals(Class[].class, method.getReturnType(), "Should return Class[]");
    }

    @Test
    @DisplayName("should have getMaxExecutionTime default method")
    void shouldHaveGetMaxExecutionTimeDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("getMaxExecutionTime");
      assertNotNull(method, "getMaxExecutionTime method should exist");
      assertTrue(method.isDefault(), "getMaxExecutionTime should be a default method");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have supportsCancellation default method")
    void shouldHaveSupportsCancellationDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("supportsCancellation");
      assertNotNull(method, "supportsCancellation method should exist");
      assertTrue(method.isDefault(), "supportsCancellation should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getPreferredExecutor default method")
    void shouldHaveGetPreferredExecutorDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("getPreferredExecutor");
      assertNotNull(method, "getPreferredExecutor method should exist");
      assertTrue(method.isDefault(), "getPreferredExecutor should be a default method");
      assertEquals(Executor.class, method.getReturnType(), "Should return Executor");
    }

    @Test
    @DisplayName("should have isConcurrencySafe default method")
    void shouldHaveIsConcurrencySafeDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("isConcurrencySafe");
      assertNotNull(method, "isConcurrencySafe method should exist");
      assertTrue(method.isDefault(), "isConcurrencySafe should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getPriority default method")
    void shouldHaveGetPriorityDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("getPriority");
      assertNotNull(method, "getPriority method should exist");
      assertTrue(method.isDefault(), "getPriority should be a default method");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have onRegister default method")
    void shouldHaveOnRegisterDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("onRegister", Object.class);
      assertNotNull(method, "onRegister method should exist");
      assertTrue(method.isDefault(), "onRegister should be a default method");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have onUnregister default method")
    void shouldHaveOnUnregisterDefaultMethod() throws NoSuchMethodException {
      final Method method = AsyncHostFunction.class.getMethod("onUnregister", Object.class);
      assertNotNull(method, "onUnregister method should exist");
      assertTrue(method.isDefault(), "onUnregister should be a default method");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have simple static factory method")
    void shouldHaveSimpleStaticFactoryMethod() throws NoSuchMethodException {
      final Method method =
          AsyncHostFunction.class.getMethod("simple", String.class, WasmValue[].class);
      assertNotNull(method, "simple method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "simple should be static");
      assertEquals(
          AsyncHostFunction.class, method.getReturnType(), "Should return AsyncHostFunction");
    }

    @Test
    @DisplayName("should have blocking static factory method")
    void shouldHaveBlockingStaticFactoryMethod() throws NoSuchMethodException {
      final Method method =
          AsyncHostFunction.class.getMethod(
              "blocking", String.class, AsyncHostFunction.BlockingOperation.class);
      assertNotNull(method, "blocking method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "blocking should be static");
      assertEquals(
          AsyncHostFunction.class, method.getReturnType(), "Should return AsyncHostFunction");
    }

    @Test
    @DisplayName("should have withExecutor static factory method")
    void shouldHaveWithExecutorStaticFactoryMethod() throws NoSuchMethodException {
      final Method method =
          AsyncHostFunction.class.getMethod(
              "withExecutor", String.class, Executor.class, AsyncHostFunction.AsyncOperation.class);
      assertNotNull(method, "withExecutor method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "withExecutor should be static");
      assertEquals(
          AsyncHostFunction.class, method.getReturnType(), "Should return AsyncHostFunction");
    }
  }

  @Nested
  @DisplayName("SimpleAsyncHostFunction Behavior Tests")
  class SimpleAsyncHostFunctionBehaviorTests {

    @Test
    @DisplayName("simple should create function that returns fixed result")
    void simpleShouldCreateFunctionThatReturnsFixedResult() throws Exception {
      final WasmValue[] result = new WasmValue[] {WasmValue.i32(42)};
      final AsyncHostFunction func = AsyncHostFunction.simple("test-func", result);

      assertEquals("test-func", func.getName(), "Name should match");

      final CompletableFuture<WasmValue[]> future = func.call(null, null);
      final WasmValue[] actual = future.get(1, TimeUnit.SECONDS);

      assertNotNull(actual, "Result should not be null");
      assertEquals(1, actual.length, "Result should have 1 element");
    }

    @Test
    @DisplayName("simple should return defensive copy of result")
    void simpleShouldReturnDefensiveCopyOfResult() throws Exception {
      final WasmValue[] result = new WasmValue[] {WasmValue.i32(1)};
      final AsyncHostFunction func = AsyncHostFunction.simple("func", result);

      // Modify original
      result[0] = WasmValue.i32(999);

      final WasmValue[] actual = func.call(null, null).get(1, TimeUnit.SECONDS);
      // Should not be affected by modification
      assertNotNull(actual, "Result should not be null");
    }
  }

  @Nested
  @DisplayName("Default Method Value Tests")
  class DefaultMethodValueTests {

    @Test
    @DisplayName("default getModuleName should return host")
    void defaultGetModuleNameShouldReturnHost() {
      final AsyncHostFunction func = (caller, args) -> CompletableFuture.completedFuture(null);
      assertEquals("host", func.getModuleName(), "Default module name should be 'host'");
    }

    @Test
    @DisplayName("default getParameterTypes should return empty array")
    void defaultGetParameterTypesShouldReturnEmptyArray() {
      final AsyncHostFunction func = (caller, args) -> CompletableFuture.completedFuture(null);
      final Class<?>[] types = func.getParameterTypes();
      assertNotNull(types, "Parameter types should not be null");
      assertEquals(0, types.length, "Parameter types should be empty");
    }

    @Test
    @DisplayName("default getReturnTypes should return empty array")
    void defaultGetReturnTypesShouldReturnEmptyArray() {
      final AsyncHostFunction func = (caller, args) -> CompletableFuture.completedFuture(null);
      final Class<?>[] types = func.getReturnTypes();
      assertNotNull(types, "Return types should not be null");
      assertEquals(0, types.length, "Return types should be empty");
    }

    @Test
    @DisplayName("default getMaxExecutionTime should return 30 seconds")
    void defaultGetMaxExecutionTimeShouldReturn30Seconds() {
      final AsyncHostFunction func = (caller, args) -> CompletableFuture.completedFuture(null);
      assertEquals(
          Duration.ofSeconds(30),
          func.getMaxExecutionTime(),
          "Default timeout should be 30 seconds");
    }

    @Test
    @DisplayName("default supportsCancellation should return true")
    void defaultSupportsCancellationShouldReturnTrue() {
      final AsyncHostFunction func = (caller, args) -> CompletableFuture.completedFuture(null);
      assertTrue(func.supportsCancellation(), "Default should support cancellation");
    }

    @Test
    @DisplayName("default getPreferredExecutor should return null")
    void defaultGetPreferredExecutorShouldReturnNull() {
      final AsyncHostFunction func = (caller, args) -> CompletableFuture.completedFuture(null);
      assertNull(func.getPreferredExecutor(), "Default executor should be null");
    }

    @Test
    @DisplayName("default isConcurrencySafe should return true")
    void defaultIsConcurrencySafeShouldReturnTrue() {
      final AsyncHostFunction func = (caller, args) -> CompletableFuture.completedFuture(null);
      assertTrue(func.isConcurrencySafe(), "Default should be concurrency safe");
    }

    @Test
    @DisplayName("default getPriority should return 0")
    void defaultGetPriorityShouldReturn0() {
      final AsyncHostFunction func = (caller, args) -> CompletableFuture.completedFuture(null);
      assertEquals(0, func.getPriority(), "Default priority should be 0");
    }
  }

  @Nested
  @DisplayName("ExecutorAsyncHostFunction Tests")
  class ExecutorAsyncHostFunctionTests {

    @Test
    @DisplayName("withExecutor should use specified executor")
    void withExecutorShouldUseSpecifiedExecutor() {
      final Executor executor = Executors.newSingleThreadExecutor();
      final AsyncHostFunction func =
          AsyncHostFunction.withExecutor(
              "executor-func",
              executor,
              (caller, args) ->
                  CompletableFuture.completedFuture(new WasmValue[] {WasmValue.i32(99)}));

      assertEquals("executor-func", func.getName(), "Name should match");
      assertEquals(executor, func.getPreferredExecutor(), "Should return specified executor");
    }
  }
}
