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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry.AsyncHostFunction;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry.CallbackHandle;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JniCallbackRegistry defensive programming and validation logic.
 *
 * <p>Note: Most registry operations require native library since they create function references.
 * These tests verify parameter validation that occurs before native calls.
 */
class JniCallbackRegistryTest {
  private static final long VALID_HANDLE = 1L;
  private JniEngine testEngine;
  private JniStore testStore;
  private JniCallbackRegistry registry;
  private FunctionType testFunctionType;
  private HostFunction testCallback;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
    registry = new JniCallbackRegistry(testStore);
    testFunctionType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
    testCallback = params -> null;
  }

  // Constructor validation tests

  @Test
  void testConstructorWithNullStore() {
    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> new JniCallbackRegistry(null));

    assertThat(exception.getMessage()).contains("Store cannot be null");
  }

  @Test
  void testConstructorCreatesValidRegistry() {
    final JniCallbackRegistry newRegistry = new JniCallbackRegistry(testStore);

    assertThat(newRegistry).isNotNull();
    assertThat(newRegistry.getCallbackCount()).isEqualTo(0);
  }

  // registerCallback validation tests - will fail at native level

  @Test
  void testRegisterCallbackWithNullName() {
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> registry.registerCallback(null, testCallback, testFunctionType));

    assertThat(exception.getMessage()).contains("Failed to register callback");
  }

  @Test
  void testRegisterCallbackWithNullCallback() {
    final WasmException exception =
        assertThrows(
            WasmException.class, () -> registry.registerCallback("test", null, testFunctionType));

    assertThat(exception.getMessage()).contains("Failed to register callback");
  }

  @Test
  void testRegisterCallbackWithNullFunctionType() {
    final WasmException exception =
        assertThrows(
            WasmException.class, () -> registry.registerCallback("test", testCallback, null));

    assertThat(exception.getMessage()).contains("Failed to register callback");
  }

  // registerAsyncCallback validation tests - will fail at native level

  @Test
  void testRegisterAsyncCallbackWithNullName() {
    final AsyncHostFunction asyncCallback =
        params -> java.util.concurrent.CompletableFuture.completedFuture(null);

    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> registry.registerAsyncCallback(null, asyncCallback, testFunctionType));

    assertThat(exception.getMessage()).contains("Failed to register async callback");
  }

  @Test
  void testRegisterAsyncCallbackWithNullCallback() {
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> registry.registerAsyncCallback("test", null, testFunctionType));

    assertThat(exception.getMessage()).contains("Failed to register async callback");
  }

  @Test
  void testRegisterAsyncCallbackWithNullFunctionType() {
    final AsyncHostFunction asyncCallback =
        params -> java.util.concurrent.CompletableFuture.completedFuture(null);

    final WasmException exception =
        assertThrows(
            WasmException.class, () -> registry.registerAsyncCallback("test", asyncCallback, null));

    assertThat(exception.getMessage()).contains("Failed to register async callback");
  }

  // createFunctionReference validation tests

  @Test
  void testCreateFunctionReferenceWithNullHandle() {
    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> registry.createFunctionReference(null));

    assertThat(exception.getMessage()).contains("Callback handle cannot be null");
  }

  @Test
  void testCreateFunctionReferenceWithNonExistentCallback() throws WasmException {
    // Create a fake callback handle that doesn't exist in the registry
    final CallbackHandle fakeHandle = createFakeCallbackHandle(999L, "fake");

    final WasmException exception =
        assertThrows(WasmException.class, () -> registry.createFunctionReference(fakeHandle));

    assertThat(exception.getMessage()).contains("Callback not found");
  }

  // unregisterCallback validation tests

  @Test
  void testUnregisterCallbackWithNullHandle() {
    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> registry.unregisterCallback(null));

    assertThat(exception.getMessage()).contains("Callback handle cannot be null");
  }

  @Test
  void testUnregisterCallbackWithNonExistentCallback() throws WasmException {
    // Unregistering a non-existent callback should not throw, just log a warning
    final CallbackHandle fakeHandle = createFakeCallbackHandle(999L, "fake");

    // Should not throw - just logs warning
    assertDoesNotThrow(() -> registry.unregisterCallback(fakeHandle));
  }

  // invokeCallback validation tests

  @Test
  void testInvokeCallbackWithNullHandle() {
    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> registry.invokeCallback(null));

    assertThat(exception.getMessage()).contains("Callback handle cannot be null");
  }

  @Test
  void testInvokeCallbackWithNullParams() throws WasmException {
    final CallbackHandle fakeHandle = createFakeCallbackHandle(1L, "test");

    final NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> registry.invokeCallback(fakeHandle, (ai.tegmentum.wasmtime4j.WasmValue[]) null));

    assertThat(exception.getMessage()).contains("Parameters cannot be null");
  }

  @Test
  void testInvokeCallbackWithInvalidHandle() throws WasmException {
    final CallbackHandle invalidHandle = createInvalidCallbackHandle(1L, "test");

    final WasmException exception =
        assertThrows(WasmException.class, () -> registry.invokeCallback(invalidHandle));

    assertThat(exception.getMessage()).contains("Callback handle is no longer valid");
  }

  @Test
  void testInvokeCallbackWithNonExistentCallback() throws WasmException {
    final CallbackHandle fakeHandle = createFakeCallbackHandle(999L, "fake");

    final WasmException exception =
        assertThrows(WasmException.class, () -> registry.invokeCallback(fakeHandle));

    assertThat(exception.getMessage()).contains("Callback not found");
  }

  // invokeAsyncCallback validation tests

  @Test
  void testInvokeAsyncCallbackWithNullHandle() {
    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> registry.invokeAsyncCallback(null));

    assertThat(exception.getMessage()).contains("Callback handle cannot be null");
  }

  @Test
  void testInvokeAsyncCallbackWithNullParams() throws WasmException {
    final ai.tegmentum.wasmtime4j.func.CallbackRegistry.AsyncCallbackHandle fakeHandle =
        createFakeAsyncCallbackHandle(1L, "test");

    final NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () ->
                registry.invokeAsyncCallback(
                    fakeHandle, (ai.tegmentum.wasmtime4j.WasmValue[]) null));

    assertThat(exception.getMessage()).contains("Parameters cannot be null");
  }

  @Test
  void testInvokeAsyncCallbackWithInvalidHandle() throws WasmException {
    final ai.tegmentum.wasmtime4j.func.CallbackRegistry.AsyncCallbackHandle invalidHandle =
        createInvalidAsyncCallbackHandle(1L, "test");

    final WasmException exception =
        assertThrows(WasmException.class, () -> registry.invokeAsyncCallback(invalidHandle));

    assertThat(exception.getMessage()).contains("Async callback handle is no longer valid");
  }

  // Metrics tests

  @Test
  void testGetMetricsReturnsNonNull() {
    final ai.tegmentum.wasmtime4j.func.CallbackRegistry.CallbackMetrics metrics =
        registry.getMetrics();

    assertThat(metrics).isNotNull();
    assertThat(metrics.getTotalInvocations()).isEqualTo(0);
    assertThat(metrics.getFailureCount()).isEqualTo(0);
    assertThat(metrics.getTimeoutCount()).isEqualTo(0);
  }

  @Test
  void testGetCallbackCountInitiallyZero() {
    assertThat(registry.getCallbackCount()).isEqualTo(0);
  }

  @Test
  void testHasCallbackWithNullName() {
    final NullPointerException exception =
        assertThrows(NullPointerException.class, () -> registry.hasCallback(null));

    assertThat(exception).isNotNull();
  }

  @Test
  void testHasCallbackWithNonExistentName() {
    assertFalse(registry.hasCallback("nonexistent"));
  }

  @Test
  void testHasCallbackWithEmptyName() {
    assertFalse(registry.hasCallback(""));
  }

  // State validation tests

  @Test
  void testRegistryNotClosedInitially() {
    // Registry should be functional after creation
    assertThat(registry.getCallbackCount()).isEqualTo(0);
  }

  // Helper methods to create fake callback handles for testing

  private CallbackHandle createFakeCallbackHandle(final long id, final String name) {
    return new CallbackHandle() {
      @Override
      public long getId() {
        return id;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public FunctionType getFunctionType() {
        return testFunctionType;
      }

      @Override
      public boolean isValid() {
        return true;
      }
    };
  }

  private CallbackHandle createInvalidCallbackHandle(final long id, final String name) {
    return new CallbackHandle() {
      @Override
      public long getId() {
        return id;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public FunctionType getFunctionType() {
        return testFunctionType;
      }

      @Override
      public boolean isValid() {
        return false; // Invalid!
      }
    };
  }

  private ai.tegmentum.wasmtime4j.func.CallbackRegistry.AsyncCallbackHandle
      createFakeAsyncCallbackHandle(final long id, final String name) {
    return new ai.tegmentum.wasmtime4j.func.CallbackRegistry.AsyncCallbackHandle() {
      @Override
      public long getId() {
        return id;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public FunctionType getFunctionType() {
        return testFunctionType;
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public long getTimeoutMillis() {
        return 30000;
      }

      @Override
      public void setTimeoutMillis(final long timeoutMillis) {
        // No-op
      }
    };
  }

  private ai.tegmentum.wasmtime4j.func.CallbackRegistry.AsyncCallbackHandle
      createInvalidAsyncCallbackHandle(final long id, final String name) {
    return new ai.tegmentum.wasmtime4j.func.CallbackRegistry.AsyncCallbackHandle() {
      @Override
      public long getId() {
        return id;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public FunctionType getFunctionType() {
        return testFunctionType;
      }

      @Override
      public boolean isValid() {
        return false; // Invalid!
      }

      @Override
      public long getTimeoutMillis() {
        return 30000;
      }

      @Override
      public void setTimeoutMillis(final long timeoutMillis) {
        // No-op
      }
    };
  }
}
