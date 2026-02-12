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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for maximum memory and table sizes. Verifies that memory and table growth beyond maximum
 * fails gracefully (returns -1 or error, no crash).
 *
 * @since 1.0.0
 */
@DisplayName("Resource Limits Tests")
public class ResourceLimitsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ResourceLimitsTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory grow to max pages succeeds")
  void memoryGrowToMaxSucceeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory grow to max");

    final String wat =
        """
        (module
          (memory (export "mem") 1 4)
          (func (export "grow") (param i32) (result i32)
            local.get 0
            memory.grow)
          (func (export "size") (result i32)
            memory.size))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      // Initial size is 1 page
      assertEquals(1, instance.callFunction("size")[0].asInt(), "Initial size should be 1 page");

      // Grow by 2 pages (to 3 of max 4)
      final int prevSize = instance.callFunction("grow", WasmValue.i32(2))[0].asInt();
      assertEquals(1, prevSize, "Previous size should be 1");
      assertEquals(3, instance.callFunction("size")[0].asInt(), "Size should now be 3 pages");
      LOGGER.info("[" + runtime + "] Grew to 3 pages successfully");

      // Grow by 1 more (to max 4)
      final int prevSize2 = instance.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(3, prevSize2, "Previous size should be 3");
      assertEquals(4, instance.callFunction("size")[0].asInt(), "Size should now be 4 pages (max)");
      LOGGER.info("[" + runtime + "] Grew to max (4 pages) successfully");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory grow beyond max fails gracefully")
  void memoryGrowBeyondMaxFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory grow beyond max");

    final String wat =
        """
        (module
          (memory (export "mem") 1 2)
          (func (export "grow") (param i32) (result i32)
            local.get 0
            memory.grow)
          (func (export "size") (result i32)
            memory.size))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      // Grow to max (2 pages)
      instance.callFunction("grow", WasmValue.i32(1));
      assertEquals(2, instance.callFunction("size")[0].asInt(), "Size should be at max (2 pages)");

      // Attempt to grow beyond max - should return -1
      final int result = instance.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(-1, result, "Growing beyond max should return -1");
      assertEquals(2, instance.callFunction("size")[0].asInt(), "Size should still be 2 pages");
      LOGGER.info("[" + runtime + "] Grow beyond max returned -1 as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Table grow to max elements succeeds")
  void tableGrowToMaxSucceeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table grow to max");

    final String wat =
        """
        (module
          (table (export "t") 2 8 funcref)
          (func (export "grow") (param i32) (result i32)
            ref.null func
            local.get 0
            table.grow 0)
          (func (export "size") (result i32)
            table.size 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      assertEquals(2, instance.callFunction("size")[0].asInt(), "Initial table size should be 2");

      // Grow by 4 (to 6 of max 8)
      final int prevSize = instance.callFunction("grow", WasmValue.i32(4))[0].asInt();
      assertEquals(2, prevSize, "Previous size should be 2");
      assertEquals(6, instance.callFunction("size")[0].asInt(), "Table size should now be 6");

      // Grow by 2 (to max 8)
      final int prevSize2 = instance.callFunction("grow", WasmValue.i32(2))[0].asInt();
      assertEquals(6, prevSize2, "Previous size should be 6");
      assertEquals(8, instance.callFunction("size")[0].asInt(), "Table size should now be 8 (max)");
      LOGGER.info("[" + runtime + "] Table grew to max (8) successfully");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Table grow beyond max fails gracefully")
  void tableGrowBeyondMaxFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table grow beyond max");

    final String wat =
        """
        (module
          (table (export "t") 2 4 funcref)
          (func (export "grow") (param i32) (result i32)
            ref.null func
            local.get 0
            table.grow 0)
          (func (export "size") (result i32)
            table.size 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      // Grow to max (4)
      instance.callFunction("grow", WasmValue.i32(2));
      assertEquals(4, instance.callFunction("size")[0].asInt(), "Table should be at max (4)");

      // Attempt to grow beyond max - should return -1
      final int result = instance.callFunction("grow", WasmValue.i32(1))[0].asInt();
      assertEquals(-1, result, "Growing table beyond max should return -1");
      assertEquals(4, instance.callFunction("size")[0].asInt(), "Table size should still be 4");
      LOGGER.info("[" + runtime + "] Table grow beyond max returned -1 as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory API getSize and grow methods")
  void memoryApiGetSizeAndGrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory API getSize and grow");

    final String wat =
        """
        (module
          (memory (export "mem") 1 3)
          (func (export "noop")))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final Optional<WasmMemory> memOpt = instance.getMemory("mem");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      assertEquals(1, memory.getSize(), "Initial size should be 1 page");
      LOGGER.info("[" + runtime + "] Initial memory size: " + memory.getSize());

      // Grow via API
      final int prevSize = memory.grow(1);
      assertEquals(1, prevSize, "Previous size should be 1");
      assertEquals(2, memory.getSize(), "Size after grow should be 2");
      LOGGER.info("[" + runtime + "] Memory size after grow: " + memory.getSize());

      instance.close();
      module.close();
    }
  }
}
