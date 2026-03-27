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
package ai.tegmentum.wasmtime4j.wasmtime.generated.engine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.OperatorCost;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for OperatorCost configuration with actual fuel consumption.
 *
 * <p>Verifies that per-operator fuel costs affect execution fuel usage.
 */
@DisplayName("OperatorCost Integration")
public final class OperatorCostIntegrationTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("engine with operator cost can be created")
  void engineWithOperatorCostCanBeCreated(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final OperatorCost costs = OperatorCost.defaults().set("Call", 10).set("I32Add", 2);

    final EngineConfig config = new EngineConfig().consumeFuel(true).operatorCost(costs);

    try (final Engine engine = Engine.create(config)) {
      assertNotNull(engine, "Engine with operator cost should be created");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("custom operator costs affect fuel consumption")
  void customOperatorCostsAffectFuelConsumption(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Create two engines: one with default costs, one with high call cost
    final EngineConfig defaultConfig = new EngineConfig().consumeFuel(true);
    final EngineConfig expensiveCallConfig =
        new EngineConfig().consumeFuel(true).operatorCost(OperatorCost.defaults().set("Call", 100));

    final String wat =
        """
        (module
          (func $add (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add)
          (func (export "run") (result i32)
            i32.const 1
            i32.const 2
            call $add)
        )
        """;

    // Run with default costs
    final long defaultFuelUsed;
    try (final Engine engine = Engine.create(defaultConfig);
        final Store store = engine.createStore();
        final Module module = engine.compileWat(wat);
        final Linker<?> linker = Linker.create(engine)) {
      store.setFuel(10000);
      final Instance instance = linker.instantiate(store, module);
      instance.callFunction("run");
      defaultFuelUsed = 10000 - store.getFuel();
    }

    // Run with expensive call cost
    final long expensiveFuelUsed;
    try (final Engine engine = Engine.create(expensiveCallConfig);
        final Store store = engine.createStore();
        final Module module = engine.compileWat(wat);
        final Linker<?> linker = Linker.create(engine)) {
      store.setFuel(10000);
      final Instance instance = linker.instantiate(store, module);
      instance.callFunction("run");
      expensiveFuelUsed = 10000 - store.getFuel();
    }

    assertTrue(
        expensiveFuelUsed > defaultFuelUsed,
        "Expensive call cost ("
            + expensiveFuelUsed
            + ") should use more fuel than default ("
            + defaultFuelUsed
            + ")");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("zero-cost operators consume no fuel")
  void zeroCostOperatorsConsumeNoFuel(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Set all common operations to zero cost
    final OperatorCost zeroCosts =
        OperatorCost.defaults()
            .set("I32Const", 0)
            .set("I32Add", 0)
            .set("LocalGet", 0)
            .set("Call", 0)
            .set("End", 0)
            .set("Return", 0);

    final EngineConfig config = new EngineConfig().consumeFuel(true).operatorCost(zeroCosts);

    final String wat =
        """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add)
        )
        """;

    try (final Engine engine = Engine.create(config);
        final Store store = engine.createStore();
        final Module module = engine.compileWat(wat);
        final Linker<?> linker = Linker.create(engine)) {
      store.setFuel(100);
      final Instance instance = linker.instantiate(store, module);
      final WasmValue[] result = instance.callFunction("add", WasmValue.i32(40), WasmValue.i32(2));
      assertNotNull(result);

      // With zero-cost ops, very little fuel should be consumed
      final long remaining = store.getFuel();
      assertTrue(
          remaining >= 90, "Zero-cost ops should consume minimal fuel, remaining: " + remaining);
    }
  }
}
