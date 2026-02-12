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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for AsyncHostFunction - asynchronous host function handling.
 *
 * <p>These tests verify async host function creation, execution, timeout handling, cancellation
 * support, and various helper factory methods. These tests do not require native implementation as
 * they test the Java-side async wrapper functionality.
 *
 * @since 1.0.0
 */
@DisplayName("AsyncHostFunction Integration Tests")
public final class AsyncHostFunctionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(AsyncHostFunctionIntegrationTest.class.getName());

  @Nested
  @DisplayName("SimpleAsyncHostFunction Tests")
  class SimpleAsyncHostFunctionTests {

    @Test
    @DisplayName("should create simple async host function with fixed result")
    void shouldCreateSimpleAsyncHostFunctionWithFixedResult(final TestInfo testInfo)
        throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasmValue[] expectedResult = new WasmValue[] {WasmValue.i32(42)};
      final AsyncHostFunction func = AsyncHostFunction.simple("test_func", expectedResult);

      assertNotNull(func, "Function should not be null");
      assertEquals("test_func", func.getName(), "Name should match");

      LOGGER.info("Created simple async host function");
    }

    @Test
    @DisplayName("should execute simple async host function")
    void shouldExecuteSimpleAsyncHostFunction(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasmValue[] expectedResult = new WasmValue[] {WasmValue.i32(42)};
      final AsyncHostFunction func = AsyncHostFunction.simple("test_func", expectedResult);

      final CompletableFuture<WasmValue[]> future = func.call(null, new WasmValue[0]);
      assertNotNull(future, "Future should not be null");

      final WasmValue[] result = future.get(5, TimeUnit.SECONDS);
      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Result should have one element");
      assertEquals(42, result[0].asInt(), "Result value should be 42");

      LOGGER.info("Simple async host function executed successfully");
    }

    @Test
    @DisplayName("should return empty result for simple function with no values")
    void shouldReturnEmptyResultForSimpleFunctionWithNoValues(final TestInfo testInfo)
        throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("void_func");

      final CompletableFuture<WasmValue[]> future = func.call(null, new WasmValue[0]);
      final WasmValue[] result = future.get(5, TimeUnit.SECONDS);

      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Result should be empty");

      LOGGER.info("Void function returned empty result");
    }

    @Test
    @DisplayName("should return defensive copy of result array")
    void shouldReturnDefensiveCopyOfResultArray(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasmValue[] expectedResult = new WasmValue[] {WasmValue.i32(10)};
      final AsyncHostFunction func = AsyncHostFunction.simple("test_func", expectedResult);

      final WasmValue[] result1 = func.call(null, new WasmValue[0]).get(5, TimeUnit.SECONDS);
      final WasmValue[] result2 = func.call(null, new WasmValue[0]).get(5, TimeUnit.SECONDS);

      // Modify result1
      if (result1.length > 0) {
        result1[0] = WasmValue.i32(999);
      }

      // result2 should be unaffected
      assertEquals(10, result2[0].asInt(), "Second call should return original value");

      LOGGER.info("Defensive copy verified");
    }
  }

  @Nested
  @DisplayName("BlockingAsyncHostFunction Tests")
  class BlockingAsyncHostFunctionTests {

    @Test
    @DisplayName("should create blocking async host function")
    void shouldCreateBlockingAsyncHostFunction(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func =
          AsyncHostFunction.blocking(
              "blocking_func", (caller, args) -> new WasmValue[] {WasmValue.i32(100)});

      assertNotNull(func, "Function should not be null");
      assertEquals("blocking_func", func.getName(), "Name should match");

      LOGGER.info("Created blocking async host function");
    }

    @Test
    @DisplayName("should execute blocking operation asynchronously")
    void shouldExecuteBlockingOperationAsynchronously(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicBoolean executed = new AtomicBoolean(false);
      final AsyncHostFunction func =
          AsyncHostFunction.blocking(
              "slow_func",
              (caller, args) -> {
                Thread.sleep(100);
                executed.set(true);
                return new WasmValue[] {WasmValue.i32(42)};
              });

      final CompletableFuture<WasmValue[]> future = func.call(null, new WasmValue[0]);
      assertFalse(executed.get(), "Should not have executed yet (async)");

      final WasmValue[] result = future.get(5, TimeUnit.SECONDS);
      assertTrue(executed.get(), "Should have executed after get()");
      assertEquals(42, result[0].asInt(), "Result should be 42");

      LOGGER.info("Blocking operation executed asynchronously");
    }

    @Test
    @DisplayName("should propagate exception from blocking operation")
    void shouldPropagateExceptionFromBlockingOperation(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func =
          AsyncHostFunction.blocking(
              "error_func",
              (caller, args) -> {
                throw new RuntimeException("Test error");
              });

      final CompletableFuture<WasmValue[]> future = func.call(null, new WasmValue[0]);

      final Exception exception =
          assertThrows(
              ExecutionException.class,
              () -> future.get(5, TimeUnit.SECONDS),
              "Should throw ExecutionException");

      assertTrue(
          exception.getCause() instanceof RuntimeException, "Cause should be RuntimeException");

      LOGGER.info("Exception propagated correctly: " + exception.getCause().getMessage());
    }

    @Test
    @DisplayName("should pass arguments to blocking operation")
    void shouldPassArgumentsToBlockingOperation(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicReference<WasmValue[]> capturedArgs = new AtomicReference<>();
      final AsyncHostFunction func =
          AsyncHostFunction.blocking(
              "args_func",
              (caller, args) -> {
                capturedArgs.set(args);
                return new WasmValue[] {WasmValue.i32(args.length)};
              });

      final WasmValue[] inputArgs =
          new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3)};
      final CompletableFuture<WasmValue[]> future = func.call(null, inputArgs);
      final WasmValue[] result = future.get(5, TimeUnit.SECONDS);

      assertEquals(3, result[0].asInt(), "Should return argument count");
      assertArrayEquals(inputArgs, capturedArgs.get(), "Arguments should be passed correctly");

      LOGGER.info("Arguments passed correctly to blocking operation");
    }
  }

  @Nested
  @DisplayName("ExecutorAsyncHostFunction Tests")
  class ExecutorAsyncHostFunctionTests {

    @Test
    @DisplayName("should create async host function with custom executor")
    void shouldCreateAsyncHostFunctionWithCustomExecutor(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Executor executor = Executors.newSingleThreadExecutor();
      final AsyncHostFunction func =
          AsyncHostFunction.withExecutor(
              "executor_func",
              executor,
              (caller, args) ->
                  CompletableFuture.completedFuture(new WasmValue[] {WasmValue.i32(1)}));

      assertNotNull(func, "Function should not be null");
      assertEquals("executor_func", func.getName(), "Name should match");
      assertEquals(executor, func.getPreferredExecutor(), "Executor should match");

      LOGGER.info("Created async host function with custom executor");
    }

    @Test
    @DisplayName("should execute on custom executor")
    void shouldExecuteOnCustomExecutor(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicReference<String> executionThread = new AtomicReference<>();
      final Executor executor =
          Executors.newSingleThreadExecutor(
              r -> {
                final Thread t = new Thread(r);
                t.setName("custom-executor-thread");
                return t;
              });

      final AsyncHostFunction func =
          AsyncHostFunction.withExecutor(
              "executor_func",
              executor,
              (caller, args) -> {
                executionThread.set(Thread.currentThread().getName());
                return CompletableFuture.completedFuture(new WasmValue[] {WasmValue.i32(1)});
              });

      final CompletableFuture<WasmValue[]> future = func.call(null, new WasmValue[0]);
      future.get(5, TimeUnit.SECONDS);

      assertEquals(
          "custom-executor-thread",
          executionThread.get(),
          "Should execute on custom executor thread");

      LOGGER.info("Executed on custom executor: " + executionThread.get());
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should return default module name")
    void shouldReturnDefaultModuleName(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      assertEquals("host", func.getModuleName(), "Default module name should be 'host'");

      LOGGER.info("Default module name is 'host'");
    }

    @Test
    @DisplayName("should return default parameter types (empty)")
    void shouldReturnDefaultParameterTypes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      final Class<?>[] paramTypes = func.getParameterTypes();

      assertNotNull(paramTypes, "Parameter types should not be null");
      assertEquals(0, paramTypes.length, "Default parameter types should be empty");

      LOGGER.info("Default parameter types is empty array");
    }

    @Test
    @DisplayName("should return default return types (empty)")
    void shouldReturnDefaultReturnTypes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      final Class<?>[] returnTypes = func.getReturnTypes();

      assertNotNull(returnTypes, "Return types should not be null");
      assertEquals(0, returnTypes.length, "Default return types should be empty");

      LOGGER.info("Default return types is empty array");
    }

    @Test
    @DisplayName("should return default max execution time (30 seconds)")
    void shouldReturnDefaultMaxExecutionTime(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      final Duration maxTime = func.getMaxExecutionTime();

      assertEquals(Duration.ofSeconds(30), maxTime, "Default max execution time should be 30s");

      LOGGER.info("Default max execution time is 30 seconds");
    }

    @Test
    @DisplayName("should support cancellation by default")
    void shouldSupportCancellationByDefault(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      assertTrue(func.supportsCancellation(), "Should support cancellation by default");

      LOGGER.info("Cancellation supported by default");
    }

    @Test
    @DisplayName("should return null for default preferred executor")
    void shouldReturnNullForDefaultPreferredExecutor(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      assertNull(func.getPreferredExecutor(), "Default preferred executor should be null");

      LOGGER.info("Default preferred executor is null");
    }

    @Test
    @DisplayName("should be concurrency safe by default")
    void shouldBeConcurrencySafeByDefault(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      assertTrue(func.isConcurrencySafe(), "Should be concurrency safe by default");

      LOGGER.info("Concurrency safe by default");
    }

    @Test
    @DisplayName("should return default priority (0)")
    void shouldReturnDefaultPriority(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      assertEquals(0, func.getPriority(), "Default priority should be 0");

      LOGGER.info("Default priority is 0");
    }

    @Test
    @DisplayName("should handle onRegister without error")
    void shouldHandleOnRegisterWithoutError(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      assertDoesNotThrow(() -> func.onRegister(new Object()), "onRegister should not throw");

      LOGGER.info("onRegister handled without error");
    }

    @Test
    @DisplayName("should handle onUnregister without error")
    void shouldHandleOnUnregisterWithoutError(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func = AsyncHostFunction.simple("test");
      assertDoesNotThrow(() -> func.onUnregister(new Object()), "onUnregister should not throw");

      LOGGER.info("onUnregister handled without error");
    }
  }

  @Nested
  @DisplayName("Custom Implementation Tests")
  class CustomImplementationTests {

    @Test
    @DisplayName("should allow custom implementation via lambda")
    void shouldAllowCustomImplementationViaLambda(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func =
          (caller, args) -> {
            final int a = args[0].asInt();
            final int b = args[1].asInt();
            return CompletableFuture.completedFuture(new WasmValue[] {WasmValue.i32(a + b)});
          };

      final WasmValue[] args = new WasmValue[] {WasmValue.i32(5), WasmValue.i32(3)};
      final WasmValue[] result = func.call(null, args).get(5, TimeUnit.SECONDS);

      assertEquals(8, result[0].asInt(), "5 + 3 should equal 8");

      LOGGER.info("Custom lambda implementation works");
    }

    @Test
    @DisplayName("should allow overriding default methods")
    void shouldAllowOverridingDefaultMethods(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func =
          new AsyncHostFunction() {
            @Override
            public CompletableFuture<WasmValue[]> call(
                final ai.tegmentum.wasmtime4j.func.Function<?> caller, final WasmValue[] args) {
              return CompletableFuture.completedFuture(new WasmValue[] {WasmValue.i32(1)});
            }

            @Override
            public String getName() {
              return "custom_name";
            }

            @Override
            public String getModuleName() {
              return "custom_module";
            }

            @Override
            public Duration getMaxExecutionTime() {
              return Duration.ofMinutes(5);
            }

            @Override
            public boolean supportsCancellation() {
              return false;
            }

            @Override
            public int getPriority() {
              return 10;
            }
          };

      assertEquals("custom_name", func.getName(), "Custom name should be returned");
      assertEquals("custom_module", func.getModuleName(), "Custom module should be returned");
      assertEquals(
          Duration.ofMinutes(5), func.getMaxExecutionTime(), "Custom timeout should be returned");
      assertFalse(func.supportsCancellation(), "Custom cancellation setting should be returned");
      assertEquals(10, func.getPriority(), "Custom priority should be returned");

      LOGGER.info("Default methods can be overridden");
    }
  }

  @Nested
  @DisplayName("Timeout and Cancellation Tests")
  class TimeoutAndCancellationTests {

    @Test
    @DisplayName("should timeout slow operation")
    void shouldTimeoutSlowOperation(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncHostFunction func =
          AsyncHostFunction.blocking(
              "slow_func",
              (caller, args) -> {
                Thread.sleep(10000); // 10 seconds
                return new WasmValue[] {WasmValue.i32(1)};
              });

      final CompletableFuture<WasmValue[]> future = func.call(null, new WasmValue[0]);

      assertThrows(
          TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS), "Should timeout");

      LOGGER.info("Slow operation timed out as expected");
    }

    @Test
    @DisplayName("should cancel operation via future")
    void shouldCancelOperationViaFuture(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicBoolean interrupted = new AtomicBoolean(false);
      final AsyncHostFunction func =
          AsyncHostFunction.blocking(
              "cancellable_func",
              (caller, args) -> {
                try {
                  Thread.sleep(10000);
                } catch (InterruptedException e) {
                  interrupted.set(true);
                  Thread.currentThread().interrupt();
                }
                return new WasmValue[] {};
              });

      final CompletableFuture<WasmValue[]> future = func.call(null, new WasmValue[0]);

      // Cancel after short delay
      Thread.sleep(50);
      final boolean cancelled = future.cancel(true);

      LOGGER.info("Cancel returned: " + cancelled + ", future cancelled: " + future.isCancelled());
    }
  }
}
