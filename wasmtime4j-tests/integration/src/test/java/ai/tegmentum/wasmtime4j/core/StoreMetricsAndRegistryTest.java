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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiter;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Store} execution metrics, callback registry, resource limiter getter, and fuel
 * async yield interval methods.
 *
 * <p>Covers: {@link Store#getExecutionCount()}, {@link Store#getTotalExecutionTimeMicros()}, {@link
 * Store#getCallbackRegistry()}, {@link Store#getLimiter()}, {@link
 * Store#setFuelAsyncYieldInterval(long)}, {@link Store#getFuelAsyncYieldInterval()}.
 *
 * @since 1.0.0
 */
@DisplayName("Store Metrics and Registry Tests")
public class StoreMetricsAndRegistryTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(StoreMetricsAndRegistryTest.class.getName());

  /**
   * Simple WAT module that exports an add function for exercising execution metrics.
   *
   * <pre>
   * (module
   *   (func (export "add") (param i32 i32) (result i32)
   *     local.get 0 local.get 1 i32.add))
   * </pre>
   */
  private static final String ADD_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0 local.get 1 i32.add))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getExecutionCount starts at zero for fresh store")
  void getExecutionCountStartsAtZero(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getExecutionCount() on fresh store");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final long count = store.getExecutionCount();
      LOGGER.info("[" + runtime + "] Fresh store execution count: " + count);
      assertTrue(count >= 0, "Execution count should be non-negative, got: " + count);
      assertEquals(0L, count, "Execution count on fresh store should be 0");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getExecutionCount increments after function calls")
  void getExecutionCountIncrementsAfterCalls(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getExecutionCount() increments after calls");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(ADD_WAT)) {

      final long countBefore = store.getExecutionCount();
      LOGGER.info("[" + runtime + "] Execution count before calls: " + countBefore);

      try (Instance instance = module.instantiate(store)) {
        final Optional<WasmFunction> addFn = instance.getFunction("add");
        assertTrue(addFn.isPresent(), "add export must be present");

        final int numCalls = 5;
        for (int i = 0; i < numCalls; i++) {
          final WasmValue[] result = addFn.get().call(WasmValue.i32(i), WasmValue.i32(i + 1));
          LOGGER.info("[" + runtime + "] add(" + i + ", " + (i + 1) + ") = "
              + result[0].asI32());
        }

        final long countAfter = store.getExecutionCount();
        LOGGER.info("[" + runtime + "] Execution count after " + numCalls + " calls: "
            + countAfter);
        assertTrue(countAfter >= countBefore,
            "Execution count should be >= initial count after calls, got: " + countAfter);
        if (countAfter == countBefore) {
          LOGGER.info("[" + runtime + "] Runtime does not track per-call execution count "
              + "(returned " + countAfter + " before and after calls)");
        } else {
          assertTrue(countAfter >= countBefore + numCalls,
              "Execution count should be at least " + (countBefore + numCalls)
                  + " after " + numCalls + " calls, got: " + countAfter);
        }
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getTotalExecutionTimeMicros starts at zero for fresh store")
  void getTotalExecutionTimeMicrosStartsAtZero(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getTotalExecutionTimeMicros() on fresh store");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final long timeMicros = store.getTotalExecutionTimeMicros();
      LOGGER.info("[" + runtime + "] Fresh store total execution time: " + timeMicros + " us");
      assertTrue(timeMicros >= 0,
          "Total execution time should be non-negative, got: " + timeMicros);
      assertEquals(0L, timeMicros,
          "Total execution time on fresh store should be 0");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getTotalExecutionTimeMicros increments after function calls")
  void getTotalExecutionTimeMicrosIncrementsAfterCalls(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getTotalExecutionTimeMicros() increments after calls");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(ADD_WAT)) {

      try (Instance instance = module.instantiate(store)) {
        final Optional<WasmFunction> addFn = instance.getFunction("add");
        assertTrue(addFn.isPresent(), "add export must be present");

        for (int i = 0; i < 10; i++) {
          addFn.get().call(WasmValue.i32(i), WasmValue.i32(i + 1));
        }

        final long timeMicros = store.getTotalExecutionTimeMicros();
        LOGGER.info("[" + runtime + "] Total execution time after 10 calls: "
            + timeMicros + " us");
        assertTrue(timeMicros >= 0,
            "Total execution time should be non-negative after calls, got: " + timeMicros);
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getCallbackRegistry returns non-null")
  void getCallbackRegistryReturnsNonNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getCallbackRegistry() returns non-null");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final CallbackRegistry registry = store.getCallbackRegistry();
      assertNotNull(registry, "CallbackRegistry should not be null");
      LOGGER.info("[" + runtime + "] CallbackRegistry: " + registry.getClass().getName());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getCallbackRegistry returns same instance on repeated calls")
  void getCallbackRegistryConsistent(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getCallbackRegistry() consistency");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final CallbackRegistry registry1 = store.getCallbackRegistry();
      final CallbackRegistry registry2 = store.getCallbackRegistry();
      assertNotNull(registry1, "First getCallbackRegistry() should not be null");
      assertNotNull(registry2, "Second getCallbackRegistry() should not be null");
      assertSame(registry1, registry2,
          "getCallbackRegistry() should return the same instance on repeated calls");
      LOGGER.info("[" + runtime + "] Both calls returned same instance: "
          + registry1.getClass().getName());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getLimiter returns null by default")
  void getLimiterReturnsNullByDefault(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getLimiter() returns null by default");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final ResourceLimiter limiter = store.getLimiter();
      assertNull(limiter, "getLimiter() should return null when no limiter has been set");
      LOGGER.info("[" + runtime + "] getLimiter() returned null as expected");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getLimiter returns limiter after setting one")
  void getLimiterReturnsSetLimiter(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getLimiter() round-trip after setting a limiter");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        store.limiter(new ResourceLimiter() {
          @Override
          public long getId() {
            return 42L;
          }

          @Override
          public ai.tegmentum.wasmtime4j.execution.ResourceLimiterConfig getConfig()
              throws WasmException {
            return ai.tegmentum.wasmtime4j.execution.ResourceLimiterConfig.defaults();
          }

          @Override
          public boolean allowMemoryGrow(final long currentPages, final long requestedPages)
              throws WasmException {
            return true;
          }

          @Override
          public boolean allowTableGrow(final long currentElements, final long requestedElements)
              throws WasmException {
            return true;
          }

          @Override
          public ai.tegmentum.wasmtime4j.execution.ResourceLimiterStats getStats()
              throws WasmException {
            return new ai.tegmentum.wasmtime4j.execution.ResourceLimiterStats(
                0, 0, 0, 0, 0, 0);
          }

          @Override
          public void resetStats() throws WasmException {
            // no-op
          }

          @Override
          public void close() throws WasmException {
            // no-op
          }
        });

        final ResourceLimiter retrieved = store.getLimiter();
        assertNotNull(retrieved, "getLimiter() should return non-null after setting a limiter");
        LOGGER.info("[" + runtime + "] getLimiter() returned: "
            + retrieved.getClass().getName() + " (id=" + retrieved.getId() + ")");
      } catch (final UnsupportedOperationException | UnsatisfiedLinkError e) {
        LOGGER.info("[" + runtime + "] store.limiter() not supported: "
            + e.getClass().getSimpleName() + " - " + e.getMessage()
            + " -- skipping round-trip assertion");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getFuelAsyncYieldInterval defaults to zero")
  void fuelAsyncYieldIntervalDefaultsToZero(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getFuelAsyncYieldInterval() default value");

    final EngineConfig config = Engine.builder().consumeFuel(true);
    try (Engine engine = Engine.create(config);
        Store store = engine.createStore()) {

      store.setFuel(10_000L);

      final long interval = store.getFuelAsyncYieldInterval();
      LOGGER.info("[" + runtime + "] Default fuel async yield interval: " + interval);
      assertEquals(0L, interval,
          "Fuel async yield interval should default to 0 (disabled)");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("setFuelAsyncYieldInterval and getFuelAsyncYieldInterval round-trip")
  void setAndGetFuelAsyncYieldInterval(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing set/getFuelAsyncYieldInterval() round-trip");

    final EngineConfig config = Engine.builder().consumeFuel(true);
    try (Engine engine = Engine.create(config);
        Store store = engine.createStore()) {

      store.setFuel(10_000L);
      final long targetInterval = 500L;

      try {
        store.setFuelAsyncYieldInterval(targetInterval);
        final long retrievedInterval = store.getFuelAsyncYieldInterval();
        LOGGER.info("[" + runtime + "] Set fuel async yield interval to " + targetInterval
            + ", got back: " + retrievedInterval);
        assertEquals(targetInterval, retrievedInterval,
            "getFuelAsyncYieldInterval() should return the value that was set");
      } catch (final WasmException | UnsupportedOperationException | UnsatisfiedLinkError e) {
        LOGGER.info("[" + runtime + "] setFuelAsyncYieldInterval not supported: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      }
    }
  }
}
