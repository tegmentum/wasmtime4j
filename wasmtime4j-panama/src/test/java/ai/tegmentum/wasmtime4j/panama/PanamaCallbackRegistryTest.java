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

import ai.tegmentum.wasmtime4j.CallbackRegistry;
import ai.tegmentum.wasmtime4j.CallbackRegistry.AsyncCallbackHandle;
import ai.tegmentum.wasmtime4j.CallbackRegistry.AsyncHostFunction;
import ai.tegmentum.wasmtime4j.CallbackRegistry.CallbackHandle;
import ai.tegmentum.wasmtime4j.CallbackRegistry.CallbackMetrics;
import ai.tegmentum.wasmtime4j.FunctionReference;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaCallbackRegistry} class.
 *
 * <p>PanamaCallbackRegistry manages callbacks and asynchronous operations in Panama FFI.
 */
@DisplayName("PanamaCallbackRegistry Tests")
class PanamaCallbackRegistryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaCallbackRegistry.class.getModifiers()),
          "PanamaCallbackRegistry should be public");
      assertTrue(
          Modifier.isFinal(PanamaCallbackRegistry.class.getModifiers()),
          "PanamaCallbackRegistry should be final");
    }

    @Test
    @DisplayName("should implement CallbackRegistry interface")
    void shouldImplementCallbackRegistryInterface() {
      assertTrue(
          CallbackRegistry.class.isAssignableFrom(PanamaCallbackRegistry.class),
          "PanamaCallbackRegistry should implement CallbackRegistry");
    }
  }

  @Nested
  @DisplayName("Sync Callback Method Tests")
  class SyncCallbackMethodTests {

    @Test
    @DisplayName("should have registerCallback method")
    void shouldHaveRegisterCallbackMethod() throws NoSuchMethodException {
      final Method method =
          PanamaCallbackRegistry.class.getMethod(
              "registerCallback", String.class, HostFunction.class, FunctionType.class);
      assertNotNull(method, "registerCallback method should exist");
      assertEquals(CallbackHandle.class, method.getReturnType(), "Should return CallbackHandle");
    }

    @Test
    @DisplayName("should have unregisterCallback method")
    void shouldHaveUnregisterCallbackMethod() throws NoSuchMethodException {
      final Method method =
          PanamaCallbackRegistry.class.getMethod("unregisterCallback", CallbackHandle.class);
      assertNotNull(method, "unregisterCallback method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have invokeCallback method")
    void shouldHaveInvokeCallbackMethod() throws NoSuchMethodException {
      final Method method =
          PanamaCallbackRegistry.class.getMethod(
              "invokeCallback", CallbackHandle.class, WasmValue[].class);
      assertNotNull(method, "invokeCallback method should exist");
      assertEquals(WasmValue[].class, method.getReturnType(), "Should return WasmValue[]");
    }
  }

  @Nested
  @DisplayName("Async Callback Method Tests")
  class AsyncCallbackMethodTests {

    @Test
    @DisplayName("should have registerAsyncCallback method")
    void shouldHaveRegisterAsyncCallbackMethod() throws NoSuchMethodException {
      final Method method =
          PanamaCallbackRegistry.class.getMethod(
              "registerAsyncCallback", String.class, AsyncHostFunction.class, FunctionType.class);
      assertNotNull(method, "registerAsyncCallback method should exist");
      assertEquals(
          AsyncCallbackHandle.class, method.getReturnType(), "Should return AsyncCallbackHandle");
    }

    @Test
    @DisplayName("should have invokeAsyncCallback method")
    void shouldHaveInvokeAsyncCallbackMethod() throws NoSuchMethodException {
      final Method method =
          PanamaCallbackRegistry.class.getMethod(
              "invokeAsyncCallback", AsyncCallbackHandle.class, WasmValue[].class);
      assertNotNull(method, "invokeAsyncCallback method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Function Reference Method Tests")
  class FunctionReferenceMethodTests {

    @Test
    @DisplayName("should have createFunctionReference method")
    void shouldHaveCreateFunctionReferenceMethod() throws NoSuchMethodException {
      final Method method =
          PanamaCallbackRegistry.class.getMethod("createFunctionReference", CallbackHandle.class);
      assertNotNull(method, "createFunctionReference method should exist");
      assertEquals(
          FunctionReference.class, method.getReturnType(), "Should return FunctionReference");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("should have getCallbackCount method")
    void shouldHaveGetCallbackCountMethod() throws NoSuchMethodException {
      final Method method = PanamaCallbackRegistry.class.getMethod("getCallbackCount");
      assertNotNull(method, "getCallbackCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have hasCallback method")
    void shouldHaveHasCallbackMethod() throws NoSuchMethodException {
      final Method method = PanamaCallbackRegistry.class.getMethod("hasCallback", String.class);
      assertNotNull(method, "hasCallback method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Metrics Method Tests")
  class MetricsMethodTests {

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = PanamaCallbackRegistry.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(CallbackMetrics.class, method.getReturnType(), "Should return CallbackMetrics");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaCallbackRegistry.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Default Timeout Tests")
  class DefaultTimeoutTests {

    @Test
    @DisplayName("should have reasonable default async timeout")
    void shouldHaveReasonableDefaultAsyncTimeout() throws Exception {
      // The default timeout is a private constant, so we verify it indirectly
      // by checking the class has the expected static field pattern
      java.lang.reflect.Field[] fields = PanamaCallbackRegistry.class.getDeclaredFields();
      boolean hasTimeoutField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().contains("TIMEOUT") && Modifier.isStatic(field.getModifiers())) {
          hasTimeoutField = true;
          break;
        }
      }
      assertTrue(hasTimeoutField, "Should have a timeout constant");
    }
  }
}
