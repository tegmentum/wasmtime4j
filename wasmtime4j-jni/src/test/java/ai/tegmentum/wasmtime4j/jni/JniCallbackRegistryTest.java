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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniCallbackRegistry}.
 *
 * <p>Tests validation logic, initial state, close lifecycle, and operations-after-close behavior.
 * All validation tests exercise pre-native-call code paths that run entirely in Java.
 *
 * <p>Callback registration tests that require native resources (JniFunctionReference) are covered
 * by integration tests. These unit tests focus on input validation, lifecycle management, and
 * metrics tracking that can be verified without native bindings.
 */
@DisplayName("JniCallbackRegistry Tests")
class JniCallbackRegistryTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniStore testStore;
  private JniCallbackRegistry registry;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
    registry = new JniCallbackRegistry(testStore);
  }

  @AfterEach
  void tearDown() {
    if (registry != null) {
      try {
        registry.close();
      } catch (WasmException e) {
        // Expected - close may fail since there are no real native resources
      }
    }
    if (testStore != null) {
      testStore.markClosedForTesting();
    }
    if (testEngine != null) {
      testEngine.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorValidation {

    @Test
    @DisplayName("Null store should throw NullPointerException")
    void nullStoreShouldThrow() {
      NullPointerException e =
          assertThrows(NullPointerException.class, () -> new JniCallbackRegistry(null));
      assertTrue(
          e.getMessage().contains("Store cannot be null"),
          "Expected message to contain: Store cannot be null");
    }

    @Test
    @DisplayName("Valid store should create registry successfully")
    void validStoreShouldCreateRegistry() {
      assertNotNull(registry);
      assertEquals(0, registry.getCallbackCount());
    }
  }

  @Nested
  @DisplayName("Initial State")
  class InitialState {

    @Test
    @DisplayName("New registry should have zero callback count")
    void newRegistryShouldHaveZeroCallbackCount() {
      assertEquals(0, registry.getCallbackCount(), "New registry should have no callbacks");
    }

    @Test
    @DisplayName("New registry should have no callback for any name")
    void newRegistryShouldHaveNoCallbacks() {
      assertFalse(
          registry.hasCallback("nonexistent"),
          "New registry should not have any callbacks");
    }

    @Test
    @DisplayName("New registry metrics should be zero")
    void newRegistryMetricsShouldBeZero() {
      final CallbackRegistry.CallbackMetrics metrics = registry.getMetrics();
      assertEquals(0, metrics.getTotalInvocations());
      assertEquals(0, metrics.getFailureCount());
      assertEquals(0, metrics.getTimeoutCount());
      assertEquals(0, metrics.getTotalExecutionTimeNanos());
      assertEquals(0.0, metrics.getAverageExecutionTimeNanos());
    }
  }

  @Nested
  @DisplayName("Callback Registration Validation")
  class CallbackRegistrationValidation {

    @Test
    @DisplayName("registerCallback with null name should throw WasmException")
    void registerWithNullNameShouldThrow() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      WasmException e =
          assertThrows(
              WasmException.class,
              () -> registry.registerCallback(null, params -> params, funcType));
      assertTrue(
          e.getMessage().contains("Callback name cannot be null"),
          "Expected message to contain: Callback name cannot be null");
    }

    @Test
    @DisplayName("registerCallback with null callback should throw WasmException")
    void registerWithNullCallbackShouldThrow() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      WasmException e =
          assertThrows(
              WasmException.class,
              () -> registry.registerCallback("test", null, funcType));
      assertTrue(
          e.getMessage().contains("Callback cannot be null"),
          "Expected message to contain: Callback cannot be null");
    }

    @Test
    @DisplayName("registerCallback with null function type should throw WasmException")
    void registerWithNullFunctionTypeShouldThrow() {
      WasmException e =
          assertThrows(
              WasmException.class,
              () -> registry.registerCallback("test", params -> params, null));
      assertTrue(
          e.getMessage().contains("Function type cannot be null"),
          "Expected message to contain: Function type cannot be null");
    }
  }

  @Nested
  @DisplayName("Async Callback Registration Validation")
  class AsyncCallbackRegistrationValidation {

    @Test
    @DisplayName("registerAsyncCallback with null name should throw WasmException")
    void registerAsyncWithNullNameShouldThrow() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      WasmException e =
          assertThrows(
              WasmException.class,
              () ->
                  registry.registerAsyncCallback(
                      null,
                      params -> java.util.concurrent.CompletableFuture.completedFuture(params),
                      funcType));
      assertTrue(
          e.getMessage().contains("Callback name cannot be null"),
          "Expected message to contain: Callback name cannot be null");
    }

    @Test
    @DisplayName("registerAsyncCallback with null callback should throw WasmException")
    void registerAsyncWithNullCallbackShouldThrow() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      WasmException e =
          assertThrows(
              WasmException.class,
              () -> registry.registerAsyncCallback("test", null, funcType));
      assertTrue(
          e.getMessage().contains("Callback cannot be null"),
          "Expected message to contain: Callback cannot be null");
    }

    @Test
    @DisplayName("registerAsyncCallback with null function type should throw WasmException")
    void registerAsyncWithNullFunctionTypeShouldThrow() {
      WasmException e =
          assertThrows(
              WasmException.class,
              () ->
                  registry.registerAsyncCallback(
                      "test",
                      params -> java.util.concurrent.CompletableFuture.completedFuture(params),
                      null));
      assertTrue(
          e.getMessage().contains("Function type cannot be null"),
          "Expected message to contain: Function type cannot be null");
    }
  }

  @Nested
  @DisplayName("Unregistration Validation")
  class UnregistrationValidation {

    @Test
    @DisplayName("unregisterCallback with null handle should throw NullPointerException")
    void unregisterNullHandleShouldThrow() {
      NullPointerException e =
          assertThrows(NullPointerException.class, () -> registry.unregisterCallback(null));
      assertTrue(
          e.getMessage().contains("Callback handle cannot be null"),
          "Expected message to contain: Callback handle cannot be null");
    }
  }

  @Nested
  @DisplayName("Invocation Validation")
  class InvocationValidation {

    @Test
    @DisplayName("invokeCallback with null handle should throw NullPointerException")
    void invokeNullHandleShouldThrow() {
      NullPointerException e =
          assertThrows(NullPointerException.class, () -> registry.invokeCallback(null));
      assertTrue(
          e.getMessage().contains("Callback handle cannot be null"),
          "Expected message to contain: Callback handle cannot be null");
    }
  }

  @Nested
  @DisplayName("HasCallback Validation")
  class HasCallbackValidation {

    @Test
    @DisplayName("hasCallback with null name should throw NullPointerException")
    void hasCallbackNullNameShouldThrow() {
      NullPointerException e =
          assertThrows(NullPointerException.class, () -> registry.hasCallback(null));
      assertTrue(
          e.getMessage().contains("Callback name cannot be null"),
          "Expected message to contain: Callback name cannot be null");
    }
  }

  @Nested
  @DisplayName("Close Lifecycle")
  class CloseLifecycle {

    @Test
    @DisplayName("Double close should not throw")
    void doubleCloseShouldNotThrow() throws WasmException {
      registry.close();
      assertDoesNotThrow(() -> registry.close(), "Second close should be a no-op");
    }

    @Test
    @DisplayName("registerCallback after close should throw IllegalStateException")
    void registerAfterCloseShouldThrow() throws WasmException {
      registry.close();
      IllegalStateException e =
          assertThrows(
              IllegalStateException.class,
              () ->
                  registry.registerCallback(
                      "test", params -> params, createSimpleFunctionType()));
      assertTrue(
          e.getMessage().contains("closed"),
          "Expected message to contain: closed");
    }

    @Test
    @DisplayName("registerAsyncCallback after close should throw IllegalStateException")
    void registerAsyncAfterCloseShouldThrow() throws WasmException {
      registry.close();
      IllegalStateException e =
          assertThrows(
              IllegalStateException.class,
              () ->
                  registry.registerAsyncCallback(
                      "test",
                      params -> java.util.concurrent.CompletableFuture.completedFuture(params),
                      createSimpleFunctionType()));
      assertTrue(
          e.getMessage().contains("closed"),
          "Expected message to contain: closed");
    }

    @Test
    @DisplayName("hasCallback after close should return false (no ensureNotClosed guard)")
    void hasCallbackAfterCloseShouldReturnFalse() throws WasmException {
      registry.close();
      // hasCallback and getCallbackCount do not call ensureNotClosed
      assertFalse(
          registry.hasCallback("test"),
          "hasCallback should return false on closed empty registry");
    }

    @Test
    @DisplayName("getCallbackCount after close should still work")
    void getCallbackCountAfterCloseShouldWork() throws WasmException {
      registry.close();
      // getCallbackCount doesn't call ensureNotClosed
      assertEquals(0, registry.getCallbackCount());
    }

    @Test
    @DisplayName("getMetrics after close should still work")
    void getMetricsAfterCloseShouldWork() throws WasmException {
      registry.close();
      // getMetrics doesn't call ensureNotClosed
      assertNotNull(registry.getMetrics());
    }
  }

  @Nested
  @DisplayName("Implements CallbackRegistry")
  class ImplementsCallbackRegistry {

    @Test
    @DisplayName("JniCallbackRegistry should implement CallbackRegistry interface")
    void shouldImplementCallbackRegistry() {
      assertInstanceOf(
          CallbackRegistry.class,
          registry,
          "JniCallbackRegistry should implement CallbackRegistry");
    }
  }

  private FunctionType createSimpleFunctionType() {
    return new FunctionType(
        new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
  }
}
