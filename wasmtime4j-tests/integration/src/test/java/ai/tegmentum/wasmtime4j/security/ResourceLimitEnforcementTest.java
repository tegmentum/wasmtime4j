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
package ai.tegmentum.wasmtime4j.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.TrapException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Security tests for resource limit enforcement.
 *
 * <p>These tests verify that the runtime properly enforces resource limits including memory limits,
 * table limits, execution timeouts (via epochs), and fuel consumption.
 */
@DisplayName("Resource Limit Enforcement Tests")
@Tag("integration")
@Tag("security")
@SuppressWarnings("deprecation")
class ResourceLimitEnforcementTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ResourceLimitEnforcementTest.class.getName());

  private Engine engine;
  private Store store;

  // WASM with memory that can grow
  private static final byte[] MEMORY_GROW_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version

        // Type section (id=1)
        0x01,
        0x06, // section size
        0x01, // number of types
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // type 0: i32 -> i32

        // Function section (id=3)
        0x03,
        0x02, // section size
        0x01, // number of functions
        0x00, // type 0

        // Memory section (id=5) - 1 page min, 10 pages max
        0x05,
        0x04, // section size
        0x01, // number of memories
        0x01, // limits: has max
        0x01, // min: 1 page
        0x0A, // max: 10 pages

        // Export section (id=7)
        0x07,
        0x0E, // section size
        0x02, // number of exports
        0x04, // name length
        'g',
        'r',
        'o',
        'w',
        0x00, // export kind: function
        0x00, // function index
        0x03, // name length
        'm',
        'e',
        'm',
        0x02, // export kind: memory
        0x00, // memory index

        // Code section (id=10)
        0x0A,
        0x07, // section size
        0x01, // number of functions
        0x05, // body size
        0x00, // locals count
        0x20,
        0x00, // local.get 0
        0x40,
        0x00, // memory.grow 0
        0x0B // end
      };

  // WASM with infinite loop for timeout testing
  private static final byte[] INFINITE_LOOP_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version

        // Type section (id=1)
        0x01,
        0x04, // section size
        0x01, // number of types
        0x60,
        0x00,
        0x00, // type 0: () -> ()

        // Function section (id=3)
        0x03,
        0x02, // section size
        0x01, // number of functions
        0x00, // type 0

        // Export section (id=7)
        0x07,
        0x08, // section size
        0x01, // number of exports
        0x04, // name length
        'l',
        'o',
        'o',
        'p',
        0x00, // export kind: function
        0x00, // function index

        // Code section (id=10)
        0x0A,
        0x06, // section size
        0x01, // number of functions
        0x04, // body size
        0x00, // locals count
        0x03,
        0x40, // loop
        0x0C,
        0x00, // br 0 (infinite loop)
        0x0B // end
      };

  // WASM that consumes fuel
  private static final byte[] FUEL_CONSUMER_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version

        // Type section (id=1)
        0x01,
        0x06, // section size
        0x01, // number of types
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // type 0: i32 -> i32

        // Function section (id=3)
        0x03,
        0x02, // section size
        0x01, // number of functions
        0x00, // type 0

        // Export section (id=7)
        0x07,
        0x0C, // section size
        0x01, // number of exports
        0x08, // name length
        'c',
        'o',
        'n',
        's',
        'u',
        'm',
        'e',
        'r',
        0x00, // export kind: function
        0x00, // function index

        // Code section (id=10)
        0x0A,
        0x15, // section size
        0x01, // number of functions
        0x13, // body size
        0x01,
        0x01,
        0x7F, // 1 local: i32
        0x41,
        0x00, // i32.const 0 (counter = 0)
        0x21,
        0x01, // local.set 1
        0x03,
        0x40, // loop
        0x20,
        0x01, // local.get 1 (counter)
        0x20,
        0x00, // local.get 0 (iterations)
        0x49, // i32.lt_u
        0x04,
        0x40, // if
        0x20,
        0x01, // local.get 1
        0x41,
        0x01, // i32.const 1
        0x6A, // i32.add
        0x21,
        0x01, // local.set 1
        0x0C,
        0x01, // br 1 (continue loop)
        0x0B, // end if
        0x0B, // end loop
        0x20,
        0x01, // local.get 1 (return counter)
        0x0B // end
      };

  @AfterEach
  void tearDown() {
    if (store != null) {
      try {
        store.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing store: " + e.getMessage());
      }
    }
    if (engine != null) {
      try {
        engine.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing engine: " + e.getMessage());
      }
    }
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Memory Limit Enforcement Tests")
  class MemoryLimitEnforcementTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should enforce memory maximum limit")
    void shouldEnforceMemoryMaximumLimit(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_GROW_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction growFunc = instance.getFunction("grow").orElse(null);
        final WasmMemory memory = instance.getMemory("mem").orElse(null);

        assertNotNull(growFunc);
        assertNotNull(memory);

        // Memory has max 10 pages (1 initial), current = 1
        final long initialSize = memory.getSize();
        LOGGER.info("Initial memory size: " + initialSize + " pages");

        // Try to grow by 5 pages - should succeed
        final WasmValue[] result1 = growFunc.call(WasmValue.i32(5));
        final int growth1 = result1[0].asInt();
        assertEquals(1, growth1); // Returns old size
        LOGGER.info("Grew by 5 pages, old size: " + growth1);

        // Try to grow by 10 more pages - should fail (would exceed max)
        final WasmValue[] result2 = growFunc.call(WasmValue.i32(10));
        final int growth2 = result2[0].asInt();
        assertEquals(-1, growth2); // -1 indicates failure
        LOGGER.info("Attempted to grow by 10 more pages, result: " + growth2);

        // Verify final size is at the max or less
        assertTrue(memory.getSize() <= 10);
        LOGGER.info("Final memory size: " + memory.getSize() + " pages");
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should enforce memory initial minimum")
    void shouldEnforceMemoryInitialMinimum(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_GROW_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmMemory memory = instance.getMemory("mem").orElse(null);

        assertNotNull(memory);

        // Memory should have at least the minimum (1 page)
        assertTrue(memory.getSize() >= 1);
        LOGGER.info("Memory enforces minimum of " + memory.getSize() + " pages");
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Table Limit Enforcement Tests")
  class TableLimitEnforcementTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should enforce table maximum size")
    void shouldEnforceTableMaximumSize(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final int initialSize = 5;
      final int maxSize = 10;

      final WasmTable table = store.createTable(WasmValueType.FUNCREF, initialSize, maxSize);

      assertNotNull(table);
      assertEquals(initialSize, table.getSize());

      // Grow to max should succeed
      final long growResult1 = table.grow(maxSize - initialSize, null);
      assertEquals(initialSize, growResult1); // Returns old size
      assertEquals(maxSize, table.getSize());
      LOGGER.info("Table grew to max size: " + table.getSize());

      // Grow beyond max should fail
      final long growResult2 = table.grow(1, null);
      assertEquals(-1, growResult2); // -1 indicates failure
      assertEquals(maxSize, table.getSize()); // Size unchanged
      LOGGER.info("Table growth beyond max prevented");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should reject table creation with invalid limits")
    void shouldRejectTableCreationWithInvalidLimits(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      // Max < initial should be rejected
      try {
        store.createTable(WasmValueType.FUNCREF, 10, 5);
        org.junit.jupiter.api.Assertions.fail("Expected exception for invalid table limits");
      } catch (final Exception e) {
        LOGGER.info("Invalid table limits rejected: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Fuel Consumption Tests")
  class FuelConsumptionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should limit execution via fuel")
    void shouldLimitExecutionViaFuel(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);

      // Create engine with fuel consumption enabled
      final EngineConfig fuelConfig = new EngineConfig();
      fuelConfig.consumeFuel(true);

      try (Engine fuelEngine = Engine.create(fuelConfig);
          Store fuelStore = fuelEngine.createStore()) {

        final Module module = fuelEngine.compileModule(FUEL_CONSUMER_WASM);

        try {
          final Instance instance = fuelStore.createInstance(module);
          final WasmFunction consumerFunc = instance.getFunction("consumer").orElse(null);

          assertNotNull(consumerFunc);

          // Set a limited amount of fuel
          fuelStore.setFuel(1000);
          LOGGER.info("Set initial fuel to 1000");

          // Run with small number of iterations - should succeed
          final WasmValue[] result1 = consumerFunc.call(WasmValue.i32(10));
          LOGGER.info("Small iteration result: " + result1[0].asInt());
          LOGGER.info("Remaining fuel: " + fuelStore.getFuel());

          // Set fuel again for larger test
          fuelStore.setFuel(100);

          // Run with large number of iterations - should run out of fuel
          final TrapException fuelTrap =
              assertThrows(TrapException.class, () -> consumerFunc.call(WasmValue.i32(10000)));
          LOGGER.info("Fuel exhausted trap: " + fuelTrap.getMessage());
        } finally {
          module.close();
        }
      }
    }
  }

  @Nested
  @DisplayName("Epoch Interruption Tests")
  class EpochInterruptionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should interrupt long-running execution via epochs")
    void shouldInterruptViaEpochs(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);

      // Create engine with epoch interruption
      final EngineConfig epochConfig = new EngineConfig();
      epochConfig.epochInterruption(true);

      try (Engine epochEngine = Engine.create(epochConfig);
          Store epochStore = epochEngine.createStore()) {

        final Module module = epochEngine.compileModule(INFINITE_LOOP_WASM);

        try {
          final Instance instance = epochStore.createInstance(module);
          final WasmFunction loopFunc = instance.getFunction("loop").orElse(null);

          assertNotNull(loopFunc);

          // Set deadline 1 tick in the future
          epochStore.setEpochDeadline(1);
          LOGGER.info("Set epoch deadline to 1 tick");

          // Increment the engine epoch to trigger interruption
          // Start the infinite loop in a thread and interrupt it
          final Thread interrupterThread =
              new Thread(
                  () -> {
                    try {
                      Thread.sleep(100); // Give the loop time to start
                      epochEngine.incrementEpoch();
                      LOGGER.info("Incremented engine epoch");
                    } catch (final InterruptedException e) {
                      Thread.currentThread().interrupt();
                    }
                  });

          interrupterThread.start();

          // This should be interrupted
          final TrapException epochTrap = assertThrows(TrapException.class, () -> loopFunc.call());
          LOGGER.info("Epoch interruption trap: " + epochTrap.getMessage());

          interrupterThread.join(5000);
        } finally {
          module.close();
        }
      }
    }
  }

  @Nested
  @DisplayName("Combined Resource Limit Tests")
  class CombinedResourceLimitTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should enforce multiple limits simultaneously")
    void shouldEnforceMultipleLimitsSimultaneously(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_GROW_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmMemory memory = instance.getMemory("mem").orElse(null);
        final WasmFunction growFunc = instance.getFunction("grow").orElse(null);

        assertNotNull(memory);
        assertNotNull(growFunc);

        // Create table with limits
        final WasmTable table = store.createTable(WasmValueType.FUNCREF, 5, 20);

        // Both should respect their limits independently
        assertTrue(memory.getSize() <= 10);
        assertTrue(table.getSize() <= 20);

        // Try to exceed both limits
        final WasmValue[] memoryGrowResultArr = growFunc.call(WasmValue.i32(100));
        final long memoryGrowResult = memoryGrowResultArr[0].asInt();
        final long tableGrowResult = table.grow(100, null);

        assertEquals(-1, memoryGrowResult);
        assertEquals(-1, tableGrowResult);

        LOGGER.info("Multiple resource limits enforced simultaneously");
      } finally {
        module.close();
      }
    }
  }
}
