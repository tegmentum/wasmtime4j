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
package ai.tegmentum.wasmtime4j.error;

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
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
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
 * Integration tests for resource exhaustion handling.
 *
 * <p>These tests verify that the runtime properly handles and reports resource exhaustion scenarios
 * including memory limits, table limits, and stack overflow.
 */
@DisplayName("Resource Exhaustion Tests")
@Tag("integration")
class ResourceExhaustionTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ResourceExhaustionTest.class.getName());

  private Engine engine;
  private Store store;

  // WASM module with memory that can grow
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
        0x60, // func type
        0x01,
        0x7F, // 1 param: i32 (pages to grow)
        0x01,
        0x7F, // 1 result: i32 (old size or -1)

        // Function section (id=3)
        0x03,
        0x02, // section size
        0x01, // number of functions
        0x00, // type index 0

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

  // WASM with recursive function for stack overflow
  private static final byte[] STACK_OVERFLOW_WASM =
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
        0x05, // section size
        0x01, // number of types
        0x60, // func type
        0x00, // no params
        0x01,
        0x7F, // 1 i32 result

        // Function section (id=3)
        0x03,
        0x02, // section size
        0x01, // number of functions
        0x00, // type index 0

        // Export section (id=7)
        0x07,
        0x0D, // section size
        0x01, // number of exports
        0x09, // name length
        'r',
        'e',
        'c',
        'u',
        'r',
        's',
        'i',
        'v',
        'e',
        0x00, // export kind: function
        0x00, // function index

        // Code section (id=10)
        0x0A,
        0x08, // section size
        0x01, // number of functions
        0x06, // body size
        0x00, // locals count
        0x10,
        0x00, // call 0 (recursive)
        0x41,
        0x01, // i32.const 1
        0x6A, // i32.add
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
  @DisplayName("Memory Exhaustion Tests")
  class MemoryExhaustionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return -1 when memory growth exceeds limit")
    void shouldReturnNegativeOneWhenMemoryGrowthExceedsLimit(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_GROW_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction growFunc = instance.getFunction("grow").orElse(null);

        assertNotNull(growFunc);

        // Memory has max 10 pages, min 1. Try to grow by 20 pages - should fail
        final WasmValue[] result = growFunc.call(WasmValue.i32(20));

        LOGGER.info("Memory grow result for 20 pages: " + result[0].asInt());

        // Result should be -1 indicating failure
        assertEquals(-1, result[0].asInt());
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should successfully grow memory within limits")
    void shouldSuccessfullyGrowMemoryWithinLimits(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_GROW_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction growFunc = instance.getFunction("grow").orElse(null);

        assertNotNull(growFunc);

        // Grow by 5 pages - should succeed (current 1 + 5 = 6, max is 10)
        final WasmValue[] result = growFunc.call(WasmValue.i32(5));

        LOGGER.info("Memory grow result for 5 pages: " + result[0].asInt());

        // Result should be old size (1) indicating success
        assertEquals(1, result[0].asInt());
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Table Growth Limit Tests")
  class TableGrowthLimitTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return -1 when table growth exceeds limit")
    void shouldReturnNegativeOneWhenTableGrowthExceedsLimit(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      // Create a table with limited size
      final WasmTable table = store.createTable(WasmValueType.FUNCREF, 5, 10);

      assertNotNull(table);
      assertEquals(5, table.getSize());

      // Try to grow beyond the max
      final long result = table.grow(20, null);

      LOGGER.info("Table grow result for 20 elements: " + result);

      // Result should be -1 indicating failure
      assertEquals(-1, result);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should successfully grow table within limits")
    void shouldSuccessfullyGrowTableWithinLimits(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final WasmTable table = store.createTable(WasmValueType.FUNCREF, 5, 10);

      assertNotNull(table);

      // Grow by 3 elements - should succeed (5 + 3 = 8, max is 10)
      final long oldSize = table.grow(3, null);

      LOGGER.info("Table grow result for 3 elements: " + oldSize);
      LOGGER.info("New table size: " + table.getSize());

      // Result should be old size (5) indicating success
      assertEquals(5, oldSize);
      assertEquals(8, table.getSize());
    }
  }

  @Nested
  @DisplayName("Stack Overflow Tests")
  class StackOverflowTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should trap on stack overflow from infinite recursion")
    void shouldTrapOnStackOverflow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(STACK_OVERFLOW_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction recursiveFunc = instance.getFunction("recursive").orElse(null);

        assertNotNull(recursiveFunc);

        final TrapException trap = assertThrows(TrapException.class, () -> recursiveFunc.call());
        LOGGER.info("Stack overflow trap: " + trap.getMessage());
        LOGGER.info("Trap type: " + trap.getTrapType());
        // Should be a stack overflow trap
        final String msg = trap.getMessage().toLowerCase();
        assertTrue(
            msg.contains("stack") || msg.contains("overflow") || msg.contains("call"),
            "Expected trap message to contain 'stack', 'overflow', or 'call', got: " + msg);
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Memory Access Out of Bounds Tests")
  class MemoryAccessOutOfBoundsTests {

    // WASM that reads beyond memory bounds
    private static final byte[] OOB_READ_WASM =
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
          0x05, // section size
          0x01, // number of types
          0x60, // func type
          0x00, // no params
          0x01,
          0x7F, // 1 i32 result

          // Function section (id=3)
          0x03,
          0x02, // section size
          0x01, // number of functions
          0x00, // type index 0

          // Memory section (id=5) - 1 page (64KB)
          0x05,
          0x03, // section size
          0x01, // number of memories
          0x00, // limits: no max
          0x01, // min: 1 page

          // Export section (id=7)
          0x07,
          0x0B, // section size
          0x01, // number of exports
          0x07, // name length
          'o',
          'o',
          'b',
          'R',
          'e',
          'a',
          'd',
          0x00, // export kind: function
          0x00, // function index

          // Code section (id=10)
          0x0A,
          0x0B, // section size
          0x01, // number of functions
          0x09, // body size
          0x00, // locals count
          0x41,
          (byte) 0x80,
          (byte) 0x80,
          (byte) 0x84,
          0x01, // i32.const 0x10100 (beyond 64KB)
          0x28,
          0x02,
          0x00, // i32.load align=4 offset=0
          0x0B // end
        };

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should trap on out of bounds memory read")
    void shouldTrapOnOutOfBoundsMemoryRead(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(OOB_READ_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction oobReadFunc = instance.getFunction("oobRead").orElse(null);

        assertNotNull(oobReadFunc);

        final TrapException trap = assertThrows(TrapException.class, () -> oobReadFunc.call());
        LOGGER.info("OOB read trap: " + trap.getMessage());
        LOGGER.info("Trap type: " + trap.getTrapType());
        final String msg = trap.getMessage().toLowerCase();
        assertTrue(
            msg.contains("memory")
                || msg.contains("bound")
                || msg.contains("access")
                || msg.contains("out"),
            "Expected trap message to contain 'memory', 'bound', 'access', or 'out', got: " + msg);
      } finally {
        module.close();
      }
    }
  }
}
