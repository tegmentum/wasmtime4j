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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.CallbackRegistry.AsyncCallbackHandle;
import ai.tegmentum.wasmtime4j.CallbackRegistry.AsyncHostFunction;
import ai.tegmentum.wasmtime4j.CallbackRegistry.CallbackHandle;
import ai.tegmentum.wasmtime4j.CallbackRegistry.CallbackMetrics;
import ai.tegmentum.wasmtime4j.FunctionReference;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaCallbackRegistry}.
 *
 * <p>Tests callback registration, invocation, lifecycle management, and metrics using real native
 * interactions through the Panama FFI layer.
 */
@DisplayName("PanamaCallbackRegistry Integration Tests")
class PanamaCallbackRegistryTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaCallbackRegistryTest.class.getName());

  private final List<AutoCloseable> resources = new ArrayList<>();
  private PanamaEngine engine;
  private PanamaStore store;
  private PanamaCallbackRegistry registry;

  @BeforeEach
  void setUp() throws Exception {
    engine = new PanamaEngine();
    resources.add(engine);
    LOGGER.info("Created PanamaEngine");

    store = new PanamaStore(engine);
    resources.add(store);
    LOGGER.info("Created PanamaStore");

    registry =
        new PanamaCallbackRegistry(
            store, store.getResourceManager(), PanamaErrorHandler.getInstance());
    LOGGER.info("Created PanamaCallbackRegistry");
  }

  @AfterEach
  void tearDown() {
    // Close the registry first (not AutoCloseable, call close() directly)
    if (registry != null) {
      try {
        registry.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close registry: " + e.getMessage());
      }
    }
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  /** Creates a simple FunctionType: (i32) -> (i32). */
  private FunctionType createI32ToI32Type() {
    return FunctionType.of(
        new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
  }

  /** Creates a void FunctionType: () -> (). */
  private FunctionType createVoidType() {
    return FunctionType.of(new WasmValueType[0], new WasmValueType[0]);
  }

  /** Creates a host function that doubles an i32 input. */
  private HostFunction createDoubleFunction() {
    return params -> {
      final int input = params[0].asInt();
      return new WasmValue[] {WasmValue.i32(input * 2)};
    };
  }

  /** Creates a host function that returns no values. */
  private HostFunction createVoidFunction() {
    return params -> new WasmValue[0];
  }

  @Nested
  @DisplayName("Constructor Null Validation Tests")
  class ConstructorNullValidationTests {

    @Test
    @DisplayName("Should throw for null store")
    void shouldThrowForNullStore() throws Exception {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaCallbackRegistry(
                  null, store.getResourceManager(), PanamaErrorHandler.getInstance()),
          "Should throw NullPointerException for null store");
      LOGGER.info("Correctly threw NullPointerException for null store");
    }

    @Test
    @DisplayName("Should throw for null ArenaResourceManager")
    void shouldThrowForNullArenaManager() throws Exception {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaCallbackRegistry(store, null, PanamaErrorHandler.getInstance()),
          "Should throw NullPointerException for null arenaManager");
      LOGGER.info("Correctly threw NullPointerException for null arenaManager");
    }

    @Test
    @DisplayName("Should throw for null PanamaErrorHandler")
    void shouldThrowForNullErrorHandler() throws Exception {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaCallbackRegistry(store, store.getResourceManager(), null),
          "Should throw NullPointerException for null errorHandler");
      LOGGER.info("Correctly threw NullPointerException for null errorHandler");
    }
  }

  @Nested
  @DisplayName("Sync Callback Registration Tests")
  class SyncCallbackRegistrationTests {

    @Test
    @DisplayName("Should register a sync callback")
    void shouldRegisterSyncCallback() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("double", createDoubleFunction(), createI32ToI32Type());
      assertNotNull(handle, "CallbackHandle should not be null");
      assertTrue(handle.getId() > 0, "Handle ID should be positive, got: " + handle.getId());
      assertEquals("double", handle.getName(), "Handle name should match");
      assertNotNull(handle.getFunctionType(), "Handle function type should not be null");
      assertTrue(handle.isValid(), "Handle should be valid after registration");
      LOGGER.info(
          "Registered sync callback with handle ID: "
              + handle.getId()
              + ", name: "
              + handle.getName());
    }

    @Test
    @DisplayName("Should register multiple callbacks with unique IDs")
    void shouldRegisterMultipleCallbacksWithUniqueIds() throws Exception {
      final CallbackHandle handle1 =
          registry.registerCallback("func1", createDoubleFunction(), createI32ToI32Type());
      final CallbackHandle handle2 =
          registry.registerCallback("func2", createVoidFunction(), createVoidType());
      final CallbackHandle handle3 =
          registry.registerCallback("func3", createDoubleFunction(), createI32ToI32Type());

      assertTrue(
          handle1.getId() != handle2.getId(),
          "Handle IDs should be unique: " + handle1.getId() + " vs " + handle2.getId());
      assertTrue(
          handle2.getId() != handle3.getId(),
          "Handle IDs should be unique: " + handle2.getId() + " vs " + handle3.getId());
      assertEquals(3, registry.getCallbackCount(), "Should have 3 registered callbacks");
      LOGGER.info(
          "Registered 3 callbacks with IDs: "
              + handle1.getId()
              + ", "
              + handle2.getId()
              + ", "
              + handle3.getId());
    }

    @Test
    @DisplayName("Should register void callback")
    void shouldRegisterVoidCallback() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("noop", createVoidFunction(), createVoidType());
      assertNotNull(handle, "Handle should not be null");
      assertEquals("noop", handle.getName(), "Handle name should match");
      LOGGER.info("Registered void callback: " + handle.getName());
    }
  }

  @Nested
  @DisplayName("Callback Query Tests")
  class CallbackQueryTests {

    @Test
    @DisplayName("Should report zero count when empty")
    void shouldReportZeroCountWhenEmpty() throws Exception {
      assertEquals(0, registry.getCallbackCount(), "Empty registry should have 0 callbacks");
      LOGGER.info("Empty registry callback count: " + registry.getCallbackCount());
    }

    @Test
    @DisplayName("Should report correct callback count")
    void shouldReportCorrectCallbackCount() throws Exception {
      registry.registerCallback("func1", createDoubleFunction(), createI32ToI32Type());
      assertEquals(1, registry.getCallbackCount(), "Should have 1 callback");

      registry.registerCallback("func2", createVoidFunction(), createVoidType());
      assertEquals(2, registry.getCallbackCount(), "Should have 2 callbacks");

      LOGGER.info("Callback count after 2 registrations: " + registry.getCallbackCount());
    }

    @Test
    @DisplayName("Should find callback by name")
    void shouldFindCallbackByName() throws Exception {
      assertFalse(registry.hasCallback("myFunc"), "Should not find unregistered callback");

      registry.registerCallback("myFunc", createDoubleFunction(), createI32ToI32Type());
      assertTrue(registry.hasCallback("myFunc"), "Should find registered callback by name");

      assertFalse(registry.hasCallback("otherFunc"), "Should not find non-existent callback");
      LOGGER.info("hasCallback('myFunc')=" + registry.hasCallback("myFunc"));
    }

    @Test
    @DisplayName("Should not find callback after unregister")
    void shouldNotFindCallbackAfterUnregister() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("tempFunc", createDoubleFunction(), createI32ToI32Type());
      assertTrue(registry.hasCallback("tempFunc"), "Should find before unregister");

      registry.unregisterCallback(handle);
      assertFalse(registry.hasCallback("tempFunc"), "Should not find after unregister");
      assertEquals(0, registry.getCallbackCount(), "Count should be 0 after unregister");
      LOGGER.info(
          "Callback removed: hasCallback='tempFunc' -> " + registry.hasCallback("tempFunc"));
    }
  }

  @Nested
  @DisplayName("Sync Callback Invocation Tests")
  class SyncCallbackInvocationTests {

    @Test
    @DisplayName("Should invoke sync callback and return result")
    void shouldInvokeSyncCallbackAndReturnResult() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("double", createDoubleFunction(), createI32ToI32Type());

      final WasmValue[] result = registry.invokeCallback(handle, WasmValue.i32(21));
      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should return one value");
      assertEquals(42, result[0].asInt(), "Should return doubled value (21*2=42)");
      LOGGER.info("Invoked 'double' callback with 21, got: " + result[0].asInt());
    }

    @Test
    @DisplayName("Should invoke void callback")
    void shouldInvokeVoidCallback() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("noop", createVoidFunction(), createVoidType());

      final WasmValue[] result = registry.invokeCallback(handle);
      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Void callback should return empty array");
      LOGGER.info("Invoked void callback, result length: " + result.length);
    }

    @Test
    @DisplayName("Should invoke callback that throws exception")
    void shouldHandleCallbackException() throws Exception {
      final HostFunction throwingFunc =
          params -> {
            throw new RuntimeException("Simulated callback error");
          };
      final CallbackHandle handle =
          registry.registerCallback("throwing", throwingFunc, createI32ToI32Type());

      assertThrows(
          Exception.class,
          () -> registry.invokeCallback(handle, WasmValue.i32(1)),
          "Should propagate exception from callback");
      LOGGER.info("Correctly propagated exception from throwing callback");
    }

    @Test
    @DisplayName("Should invoke callback multiple times")
    void shouldInvokeCallbackMultipleTimes() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("double", createDoubleFunction(), createI32ToI32Type());

      for (int i = 1; i <= 5; i++) {
        final WasmValue[] result = registry.invokeCallback(handle, WasmValue.i32(i));
        assertEquals(i * 2, result[0].asInt(), "Invocation " + i + " should return " + (i * 2));
      }
      LOGGER.info("Successfully invoked callback 5 times");
    }
  }

  @Nested
  @DisplayName("Function Reference Tests")
  class FunctionReferenceTests {

    @Test
    @DisplayName("Should create function reference from callback handle")
    void shouldCreateFunctionReferenceFromHandle() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("refFunc", createDoubleFunction(), createI32ToI32Type());

      final FunctionReference ref = registry.createFunctionReference(handle);
      assertNotNull(ref, "FunctionReference should not be null");
      LOGGER.info("Created FunctionReference from handle: " + ref);
    }

    @Test
    @DisplayName("Should create function reference with valid properties")
    void shouldCreateFunctionReferenceWithValidProperties() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("propFunc", createDoubleFunction(), createI32ToI32Type());

      final FunctionReference ref = registry.createFunctionReference(handle);
      assertNotNull(ref.getFunctionType(), "FunctionReference should have a function type");
      // isHostFunction/isWasmFunction are on PanamaFunctionReference, not the interface
      assertTrue(
          ref instanceof PanamaFunctionReference,
          "FunctionReference should be a PanamaFunctionReference");
      final PanamaFunctionReference pRef = (PanamaFunctionReference) ref;
      assertTrue(pRef.isHostFunction(), "FunctionReference should be a host function");
      assertFalse(pRef.isWasmFunction(), "FunctionReference should not be a wasm function");
      LOGGER.info(
          "FunctionReference properties: isHost="
              + pRef.isHostFunction()
              + ", isWasm="
              + pRef.isWasmFunction());
    }
  }

  @Nested
  @DisplayName("Unregister Callback Tests")
  class UnregisterCallbackTests {

    @Test
    @DisplayName("Should unregister a registered callback")
    void shouldUnregisterCallback() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("toRemove", createDoubleFunction(), createI32ToI32Type());
      assertEquals(1, registry.getCallbackCount(), "Should have 1 callback");

      registry.unregisterCallback(handle);
      assertEquals(0, registry.getCallbackCount(), "Should have 0 after unregister");
      assertFalse(handle.isValid(), "Handle should be invalid after unregister");
      LOGGER.info(
          "Unregistered callback, count="
              + registry.getCallbackCount()
              + ", handle.isValid="
              + handle.isValid());
    }

    @Test
    @DisplayName("Should not affect other callbacks when unregistering one")
    void shouldNotAffectOtherCallbacksOnUnregister() throws Exception {
      final CallbackHandle handle1 =
          registry.registerCallback("keep", createDoubleFunction(), createI32ToI32Type());
      final CallbackHandle handle2 =
          registry.registerCallback("remove", createVoidFunction(), createVoidType());
      assertEquals(2, registry.getCallbackCount(), "Should have 2 callbacks");

      registry.unregisterCallback(handle2);
      assertEquals(1, registry.getCallbackCount(), "Should have 1 after unregister");
      assertTrue(handle1.isValid(), "Remaining handle should still be valid");
      assertFalse(handle2.isValid(), "Removed handle should be invalid");
      assertTrue(registry.hasCallback("keep"), "Should still find 'keep'");
      assertFalse(registry.hasCallback("remove"), "Should not find 'remove'");
      LOGGER.info("Unregistered 'remove', 'keep' still present: " + registry.hasCallback("keep"));
    }
  }

  @Nested
  @DisplayName("Async Callback Registration Tests")
  class AsyncCallbackRegistrationTests {

    @Test
    @DisplayName("Should register an async callback")
    void shouldRegisterAsyncCallback() throws Exception {
      final AsyncHostFunction asyncFunc =
          params -> CompletableFuture.completedFuture(new WasmValue[] {WasmValue.i32(99)});

      final AsyncCallbackHandle handle =
          registry.registerAsyncCallback("asyncDouble", asyncFunc, createI32ToI32Type());
      assertNotNull(handle, "AsyncCallbackHandle should not be null");
      assertTrue(handle.getId() > 0, "Handle ID should be positive");
      assertEquals("asyncDouble", handle.getName(), "Handle name should match");
      assertTrue(handle.isValid(), "Handle should be valid");
      assertTrue(
          handle.getTimeoutMillis() > 0,
          "Timeout should be positive, got: " + handle.getTimeoutMillis());
      LOGGER.info(
          "Registered async callback: id="
              + handle.getId()
              + ", name="
              + handle.getName()
              + ", timeout="
              + handle.getTimeoutMillis()
              + "ms");
    }

    @Test
    @DisplayName("Should count async callbacks in total")
    void shouldCountAsyncCallbacksInTotal() throws Exception {
      final AsyncHostFunction asyncFunc =
          params -> CompletableFuture.completedFuture(new WasmValue[0]);

      registry.registerCallback("sync1", createVoidFunction(), createVoidType());
      registry.registerAsyncCallback("async1", asyncFunc, createVoidType());
      assertEquals(2, registry.getCallbackCount(), "Both sync and async should be counted");
      LOGGER.info("Total callbacks (1 sync + 1 async): " + registry.getCallbackCount());
    }
  }

  @Nested
  @DisplayName("Metrics Tests")
  class MetricsTests {

    @Test
    @DisplayName("Should return metrics object")
    void shouldReturnMetricsObject() throws Exception {
      final CallbackMetrics metrics = registry.getMetrics();
      assertNotNull(metrics, "Metrics should not be null");
      assertEquals(0, metrics.getTotalInvocations(), "Initial invocations should be 0");
      assertEquals(0, metrics.getFailureCount(), "Initial failures should be 0");
      LOGGER.info(
          "Initial metrics: invocations="
              + metrics.getTotalInvocations()
              + ", failures="
              + metrics.getFailureCount());
    }

    @Test
    @DisplayName("Should track invocation count")
    void shouldTrackInvocationCount() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("tracked", createDoubleFunction(), createI32ToI32Type());

      registry.invokeCallback(handle, WasmValue.i32(1));
      registry.invokeCallback(handle, WasmValue.i32(2));
      registry.invokeCallback(handle, WasmValue.i32(3));

      final CallbackMetrics metrics = registry.getMetrics();
      assertEquals(3, metrics.getTotalInvocations(), "Should have 3 invocations");
      assertEquals(0, metrics.getFailureCount(), "Should have 0 failures");
      LOGGER.info(
          "After 3 invocations: total="
              + metrics.getTotalInvocations()
              + ", failures="
              + metrics.getFailureCount());
    }

    @Test
    @DisplayName("Should track failure count")
    void shouldTrackFailureCount() throws Exception {
      final HostFunction throwingFunc =
          params -> {
            throw new RuntimeException("fail");
          };
      final CallbackHandle handle =
          registry.registerCallback("failing", throwingFunc, createI32ToI32Type());

      // Invoke and expect failure
      try {
        registry.invokeCallback(handle, WasmValue.i32(1));
      } catch (final Exception e) {
        // expected
      }

      final CallbackMetrics metrics = registry.getMetrics();
      assertTrue(
          metrics.getFailureCount() > 0,
          "Should have at least 1 failure, got: " + metrics.getFailureCount());
      LOGGER.info("After failing invocation: failures=" + metrics.getFailureCount());
    }

    @Test
    @DisplayName("Should track execution time")
    void shouldTrackExecutionTime() throws Exception {
      final CallbackHandle handle =
          registry.registerCallback("timed", createDoubleFunction(), createI32ToI32Type());

      registry.invokeCallback(handle, WasmValue.i32(1));

      final CallbackMetrics metrics = registry.getMetrics();
      assertTrue(
          metrics.getTotalExecutionTimeNanos() >= 0,
          "Execution time should be non-negative, got: " + metrics.getTotalExecutionTimeNanos());
      LOGGER.info("Execution time nanos: " + metrics.getTotalExecutionTimeNanos());
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Should close registry and reject new operations")
    void shouldCloseRegistryAndRejectNewOperations() throws Exception {
      // Create a separate registry for this test to avoid interfering with tearDown
      final PanamaCallbackRegistry separateRegistry =
          new PanamaCallbackRegistry(
              store, store.getResourceManager(), PanamaErrorHandler.getInstance());

      // Register a callback before closing
      separateRegistry.registerCallback(
          "beforeClose", createDoubleFunction(), createI32ToI32Type());
      assertEquals(1, separateRegistry.getCallbackCount(), "Should have 1 callback");

      // Close the registry
      assertDoesNotThrow(separateRegistry::close, "Close should not throw");
      LOGGER.info("Closed separate registry");

      // Operations after close should throw
      assertThrows(
          IllegalStateException.class,
          () ->
              separateRegistry.registerCallback(
                  "afterClose", createDoubleFunction(), createI32ToI32Type()),
          "Should throw IllegalStateException after close");
      LOGGER.info("Correctly rejected registration after close");
    }

    @Test
    @DisplayName("Should return zero count after close clears callbacks")
    void shouldReturnZeroCountAfterClose() throws Exception {
      final PanamaCallbackRegistry separateRegistry =
          new PanamaCallbackRegistry(
              store, store.getResourceManager(), PanamaErrorHandler.getInstance());
      separateRegistry.registerCallback("temp", createDoubleFunction(), createI32ToI32Type());
      assertEquals(1, separateRegistry.getCallbackCount(), "Should have 1 callback before close");

      separateRegistry.close();
      assertEquals(
          0, separateRegistry.getCallbackCount(), "Should have 0 callbacks after close clears map");
      LOGGER.info("getCallbackCount after close: " + separateRegistry.getCallbackCount());
    }

    @Test
    @DisplayName("Should not find callback after close clears all")
    void shouldNotFindCallbackAfterClose() throws Exception {
      final PanamaCallbackRegistry separateRegistry =
          new PanamaCallbackRegistry(
              store, store.getResourceManager(), PanamaErrorHandler.getInstance());
      separateRegistry.registerCallback("findMe", createDoubleFunction(), createI32ToI32Type());
      assertTrue(separateRegistry.hasCallback("findMe"), "Should find before close");

      separateRegistry.close();
      assertFalse(separateRegistry.hasCallback("findMe"), "Should not find after close clears map");
      LOGGER.info("hasCallback('findMe') after close: " + separateRegistry.hasCallback("findMe"));
    }

    @Test
    @DisplayName("Should still return metrics after close")
    void shouldReturnMetricsAfterClose() throws Exception {
      final PanamaCallbackRegistry separateRegistry =
          new PanamaCallbackRegistry(
              store, store.getResourceManager(), PanamaErrorHandler.getInstance());
      separateRegistry.close();

      final CallbackMetrics postCloseMetrics = separateRegistry.getMetrics();
      assertNotNull(postCloseMetrics, "Metrics should still be accessible after close");
      LOGGER.info("getMetrics after close: invocations=" + postCloseMetrics.getTotalInvocations());
    }

    @Test
    @DisplayName("Should handle double close gracefully")
    void shouldHandleDoubleCloseGracefully() throws Exception {
      final PanamaCallbackRegistry separateRegistry =
          new PanamaCallbackRegistry(
              store, store.getResourceManager(), PanamaErrorHandler.getInstance());

      assertDoesNotThrow(separateRegistry::close, "First close should not throw");
      assertDoesNotThrow(separateRegistry::close, "Second close should not throw");
      LOGGER.info("Double close handled gracefully");
    }
  }

  @Nested
  @DisplayName("End-to-End Callback Workflow Tests")
  class EndToEndCallbackWorkflowTests {

    @Test
    @DisplayName("Should complete full sync callback lifecycle")
    void shouldCompleteFullSyncCallbackLifecycle() throws Exception {
      // 1. Register
      final CallbackHandle handle =
          registry.registerCallback("lifecycle", createDoubleFunction(), createI32ToI32Type());
      assertTrue(handle.isValid(), "Handle should be valid after registration");
      assertEquals(1, registry.getCallbackCount(), "Should have 1 callback");
      assertTrue(registry.hasCallback("lifecycle"), "Should find by name");
      LOGGER.info("Step 1: Registered callback ID=" + handle.getId());

      // 2. Get function reference
      final FunctionReference ref = registry.createFunctionReference(handle);
      assertNotNull(ref, "FunctionReference should not be null");
      LOGGER.info("Step 2: Created FunctionReference");

      // 3. Invoke
      final WasmValue[] result = registry.invokeCallback(handle, WasmValue.i32(5));
      assertEquals(10, result[0].asInt(), "Should return 5*2=10");
      LOGGER.info("Step 3: Invoked callback, result=" + result[0].asInt());

      // 4. Check metrics
      final CallbackMetrics metrics = registry.getMetrics();
      assertEquals(1, metrics.getTotalInvocations(), "Should have 1 invocation");
      LOGGER.info("Step 4: Metrics - invocations=" + metrics.getTotalInvocations());

      // 5. Unregister
      registry.unregisterCallback(handle);
      assertFalse(handle.isValid(), "Handle should be invalid after unregister");
      assertEquals(0, registry.getCallbackCount(), "Should have 0 callbacks");
      assertFalse(registry.hasCallback("lifecycle"), "Should not find by name");
      LOGGER.info("Step 5: Unregistered callback, lifecycle complete");
    }

    @Test
    @DisplayName("Should handle multiple callbacks concurrently")
    void shouldHandleMultipleCallbacksConcurrently() throws Exception {
      // Register several callbacks with different behaviors
      final CallbackHandle addOne =
          registry.registerCallback(
              "addOne",
              params -> new WasmValue[] {WasmValue.i32(params[0].asInt() + 1)},
              createI32ToI32Type());

      final CallbackHandle triple =
          registry.registerCallback(
              "triple",
              params -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 3)},
              createI32ToI32Type());

      final CallbackHandle negate =
          registry.registerCallback(
              "negate",
              params -> new WasmValue[] {WasmValue.i32(-params[0].asInt())},
              createI32ToI32Type());

      assertEquals(3, registry.getCallbackCount(), "Should have 3 callbacks");

      // Invoke each
      assertEquals(11, registry.invokeCallback(addOne, WasmValue.i32(10))[0].asInt());
      assertEquals(30, registry.invokeCallback(triple, WasmValue.i32(10))[0].asInt());
      assertEquals(-10, registry.invokeCallback(negate, WasmValue.i32(10))[0].asInt());

      // Unregister one and verify others still work
      registry.unregisterCallback(triple);
      assertEquals(2, registry.getCallbackCount(), "Should have 2 callbacks");
      assertEquals(11, registry.invokeCallback(addOne, WasmValue.i32(10))[0].asInt());
      assertEquals(-10, registry.invokeCallback(negate, WasmValue.i32(10))[0].asInt());

      LOGGER.info("Multiple callbacks test completed successfully");
    }
  }
}
