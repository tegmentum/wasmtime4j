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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime API tests for {@link Store}.
 *
 * <p>Verifies user data, lifecycle management, fuel validation, host function creation, global
 * creation, table creation, memory creation, instance creation, epoch handling, and fuel async
 * yield interval across both JNI and Panama runtimes via the unified API.
 *
 * @since 1.0.0
 */
@DisplayName("Store API Dual-Runtime Tests")
@SuppressWarnings("deprecation")
public class StoreApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(StoreApiDualRuntimeTest.class.getName());

  /** Resources to close after each test, in reverse order. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @AfterEach
  void cleanup() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
    clearRuntimeSelection();
  }

  // ==================== User Data Tests ====================

  @Nested
  @DisplayName("User Data Tests")
  class UserDataTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should store and retrieve user data")
    void shouldStoreAndRetrieveUserData(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing store and retrieve user data");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertNull(store.getData(), "Initial user data should be null");

        store.setData("test-data");
        assertEquals("test-data", store.getData());

        store.setData(42);
        assertEquals(42, store.getData());

        store.setData(null);
        assertNull(store.getData(), "User data should be null after setting null");
        LOGGER.info("[" + runtime + "] User data storage and retrieval works correctly");
      }
    }
  }

  // ==================== Lifecycle Tests ====================

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Store should be valid after creation")
    void storeShouldBeValidAfterCreation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing store validity after creation");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertTrue(store.isValid(), "Store should be valid");
        LOGGER.info("[" + runtime + "] Store is valid after creation");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Store should be invalid after close")
    void storeShouldBeInvalidAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing store validity after close");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Store store = Store.create(engine);
      store.close();

      assertFalse(store.isValid(), "Store should be invalid after close");
      LOGGER.info("[" + runtime + "] Store is invalid after close");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Double close should not throw")
    void doubleCloseShouldNotThrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing double close");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Store store = Store.create(engine);
      store.close();

      assertDoesNotThrow(store::close, "Double close should not throw");
      LOGGER.info("[" + runtime + "] Double close succeeded without exception");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Operations on closed store should throw")
    void operationsOnClosedStoreShouldThrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing operations on closed store");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Store store = Store.create(engine);
      store.close();

      assertThrows(Exception.class, () -> store.setFuel(100));
      assertThrows(Exception.class, store::getFuel);
      assertThrows(Exception.class, () -> store.addFuel(50));
      assertThrows(Exception.class, () -> store.consumeFuel(10));
      assertThrows(Exception.class, () -> store.setEpochDeadline(1));
      // getCallbackRegistry may return cached value in some runtimes
      try {
        store.getCallbackRegistry();
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] getCallbackRegistry threw: " + e.getClass().getName());
      }
      assertThrows(Exception.class, store::gc);
      LOGGER.info("[" + runtime + "] All operations correctly throw on closed store");
    }
  }

  // ==================== Fuel Management Tests ====================

  @Nested
  @DisplayName("Fuel Management Tests")
  class FuelManagementTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative fuel for setFuel")
    void shouldRejectNegativeFuelForSet(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing setFuel with negative value");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.setFuel(-1));
        LOGGER.info("[" + runtime + "] Correctly rejected negative fuel for setFuel");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative fuel for addFuel")
    void shouldRejectNegativeFuelForAdd(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing addFuel with negative value");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.addFuel(-1));
        LOGGER.info("[" + runtime + "] Correctly rejected negative fuel for addFuel");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative fuel for consumeFuel")
    void shouldRejectNegativeFuelForConsume(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing consumeFuel with negative value");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.consumeFuel(-1));
        LOGGER.info("[" + runtime + "] Correctly rejected negative fuel for consumeFuel");
      }
    }
  }

  // ==================== Host Function Creation Tests ====================

  @Nested
  @DisplayName("Host Function Creation Tests")
  class HostFunctionCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null name for createHostFunction")
    void shouldRejectNullName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createHostFunction with null name");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(
            Exception.class,
            () ->
                store.createHostFunction(
                    null,
                    FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}),
                    params -> new WasmValue[0]));
        LOGGER.info("[" + runtime + "] Correctly rejected null function name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null function type")
    void shouldRejectNullFunctionType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createHostFunction with null function type");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(
            Exception.class,
            () -> store.createHostFunction("test", null, params -> new WasmValue[0]));
        LOGGER.info("[" + runtime + "] Correctly rejected null function type");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null implementation")
    void shouldRejectNullImplementation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createHostFunction with null implementation");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(
            Exception.class,
            () ->
                store.createHostFunction(
                    "test", FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {}), null));
        LOGGER.info("[" + runtime + "] Correctly rejected null implementation");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create host function")
    void shouldCreateHostFunction(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createHostFunction");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final ai.tegmentum.wasmtime4j.WasmFunction hostFunc =
            store.createHostFunction(
                "test_func",
                FunctionType.of(
                    new WasmValueType[] {WasmValueType.I32},
                    new WasmValueType[] {WasmValueType.I32}),
                params -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)});

        assertNotNull(hostFunc, "Host function should not be null");
        LOGGER.info("[" + runtime + "] Created host function successfully");
      }
    }
  }

  // ==================== Global Creation Tests ====================

  @Nested
  @DisplayName("Global Creation Tests")
  class GlobalCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null value type")
    void shouldRejectNullValueType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createGlobal with null value type");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createGlobal(null, true, WasmValue.i32(0)));
        LOGGER.info("[" + runtime + "] Correctly rejected null value type");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null initial value")
    void shouldRejectNullInitialValue(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createGlobal with null initial value");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createGlobal(WasmValueType.I32, true, null));
        LOGGER.info("[" + runtime + "] Correctly rejected null initial value");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject mismatched value type")
    void shouldRejectMismatchedValueType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createGlobal with mismatched value type");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(
            Exception.class, () -> store.createGlobal(WasmValueType.I64, true, WasmValue.i32(42)));
        LOGGER.info("[" + runtime + "] Correctly rejected mismatched value type");
      }
    }
  }

  // ==================== Table Creation Tests ====================

  @Nested
  @DisplayName("Table Creation Tests")
  class TableCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null element type")
    void shouldRejectNullElementType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createTable with null element type");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createTable(null, 1, 10));
        LOGGER.info("[" + runtime + "] Correctly rejected null element type");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative initial size")
    void shouldRejectNegativeInitialSize(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createTable with negative initial size");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createTable(WasmValueType.FUNCREF, -1, 10));
        LOGGER.info("[" + runtime + "] Correctly rejected negative initial size");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject invalid max size")
    void shouldRejectInvalidMaxSize(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createTable with invalid max size");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createTable(WasmValueType.FUNCREF, 1, -2));
        LOGGER.info("[" + runtime + "] Correctly rejected invalid max size");
      }
    }
  }

  // ==================== Memory Creation Tests ====================

  @Nested
  @DisplayName("Memory Creation Tests")
  class MemoryCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative initial pages")
    void shouldRejectNegativeInitialPages(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createMemory with negative initial pages");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createMemory(-1, 10));
        LOGGER.info("[" + runtime + "] Correctly rejected negative initial pages");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject invalid max pages")
    void shouldRejectInvalidMaxPages(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createMemory with invalid max pages");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createMemory(1, -2));
        LOGGER.info("[" + runtime + "] Correctly rejected invalid max pages");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject invalid shared memory max pages")
    void shouldRejectInvalidSharedMemoryMaxPages(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createSharedMemory with invalid max pages");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createSharedMemory(1, 0));
        assertThrows(Exception.class, () -> store.createSharedMemory(1, -1));
        assertThrows(Exception.class, () -> store.createSharedMemory(-1, 10));
        LOGGER.info("[" + runtime + "] Correctly rejected invalid shared memory parameters");
      }
    }
  }

  // ==================== Instance Creation Tests ====================

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null module for createInstance")
    void shouldRejectNullModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing createInstance with null module");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.createInstance(null));
        LOGGER.info("[" + runtime + "] Correctly rejected null module for createInstance");
      }
    }
  }

  // ==================== Execution Stats Tests ====================

  @Nested
  @DisplayName("Execution Stats Tests")
  class ExecutionStatsTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Pending exception on closed store should return false")
    void pendingExceptionOnClosedStoreShouldReturnFalse(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing pending exception on closed store");

      final Engine engine = Engine.create();
      resources.add(engine);

      final Store store = Store.create(engine);
      store.close();

      assertFalse(store.hasPendingException(), "Closed store should not have pending exception");
      LOGGER.info("[" + runtime + "] Closed store correctly returns false for hasPendingException");
    }
  }

  // ==================== Epoch Handling Tests ====================

  @Nested
  @DisplayName("Epoch Handling Tests")
  class EpochHandlingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative delta ticks for async yield")
    void shouldRejectNegativeDeltaTicks(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing epochDeadlineAsyncYieldAndUpdate with negative delta");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.epochDeadlineAsyncYieldAndUpdate(-1));
        LOGGER.info("[" + runtime + "] Correctly rejected negative delta ticks");
      }
    }
  }

  // ==================== Fuel Async Yield Interval Tests ====================

  @Nested
  @DisplayName("Fuel Async Yield Interval Tests")
  class FuelAsyncYieldTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should default to zero")
    void shouldDefaultToZero(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fuel async yield interval default");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final long interval = store.getFuelAsyncYieldInterval();
        assertEquals(0L, interval, "Fuel async yield interval should default to 0 (disabled)");
        LOGGER.info("[" + runtime + "] Default fuel async yield interval is " + interval);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should set and get fuel async yield interval")
    void shouldSetAndGet(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing set and get fuel async yield interval");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        store.setFuelAsyncYieldInterval(1000);
        assertEquals(
            1000L,
            store.getFuelAsyncYieldInterval(),
            "Fuel async yield interval should be 1000 after setting");
        LOGGER.info("[" + runtime + "] setFuelAsyncYieldInterval(1000) round-trip successful");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative interval")
    void shouldRejectNegativeInterval(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing setFuelAsyncYieldInterval with negative value");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        assertThrows(Exception.class, () -> store.setFuelAsyncYieldInterval(-1));
        LOGGER.info("[" + runtime + "] Correctly rejected negative interval");
      }
    }
  }
}
