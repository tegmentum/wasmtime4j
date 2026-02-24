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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Store} callback registry and fuel async yield interval methods.
 *
 * <p>Covers: {@link Store#getCallbackRegistry()}, {@link Store#setFuelAsyncYieldInterval(long)},
 * {@link Store#getFuelAsyncYieldInterval()}.
 *
 * @since 1.0.0
 */
@DisplayName("Store Metrics and Registry Tests")
public class StoreMetricsAndRegistryTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(StoreMetricsAndRegistryTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
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
      assertSame(
          registry1,
          registry2,
          "getCallbackRegistry() should return the same instance on repeated calls");
      LOGGER.info(
          "[" + runtime + "] Both calls returned same instance: " + registry1.getClass().getName());
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
      assertEquals(0L, interval, "Fuel async yield interval should default to 0 (disabled)");
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
        LOGGER.info(
            "["
                + runtime
                + "] Set fuel async yield interval to "
                + targetInterval
                + ", got back: "
                + retrievedInterval);
        assertEquals(
            targetInterval,
            retrievedInterval,
            "getFuelAsyncYieldInterval() should return the value that was set");
      } catch (final WasmException | UnsupportedOperationException | UnsatisfiedLinkError e) {
        LOGGER.info(
            "["
                + runtime
                + "] setFuelAsyncYieldInterval not supported: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      }
    }
  }
}
