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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
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
 * Security tests for memory isolation.
 *
 * <p>These tests verify that WebAssembly memory is properly isolated, preventing modules from
 * accessing memory outside their allocated bounds.
 */
@DisplayName("Memory Isolation Tests")
@Tag("integration")
@Tag("security")
class MemoryIsolationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryIsolationTest.class.getName());

  private Engine engine;
  private Store store;

  // Module with memory that performs boundary checks
  private static final byte[] MEMORY_BOUNDS_WASM =
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
        0x09, // section size
        0x02, // number of types
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // type 0: i32 -> i32 (read)
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x00, // type 1: (i32, i32) -> () (write)

        // Function section (id=3)
        0x03,
        0x03, // section size
        0x02, // number of functions
        0x00, // read: type 0
        0x01, // write: type 1

        // Memory section (id=5) - 1 page (64KB)
        0x05,
        0x03, // section size
        0x01, // number of memories
        0x00, // limits: no max
        0x01, // min: 1 page

        // Export section (id=7)
        0x07,
        0x16, // section size
        0x03, // number of exports
        0x04, // name length
        'r',
        'e',
        'a',
        'd',
        0x00, // export kind: function
        0x00, // function index
        0x05, // name length
        'w',
        'r',
        'i',
        't',
        'e',
        0x00, // export kind: function
        0x01, // function index
        0x06, // name length
        'm',
        'e',
        'm',
        'o',
        'r',
        'y',
        0x02, // export kind: memory
        0x00, // memory index

        // Code section (id=10)
        0x0A,
        0x0F, // section size
        0x02, // number of functions

        // read function: read byte at offset
        0x07, // body size
        0x00, // locals count
        0x20,
        0x00, // local.get 0
        0x2D,
        0x00,
        0x00, // i32.load8_u align=0 offset=0
        0x0B, // end

        // write function: write byte at offset
        0x08, // body size
        0x00, // locals count
        0x20,
        0x00, // local.get 0
        0x20,
        0x01, // local.get 1
        0x3A,
        0x00,
        0x00, // i32.store8 align=0 offset=0
        0x0B // end
      };

  // Module with two separate memories
  private static final byte[] TWO_MEMORIES_WASM =
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
        0x00, // noop: type 0

        // Memory section (id=5) - 1 page each
        0x05,
        0x03, // section size
        0x01, // number of memories
        0x00, // limits: no max
        0x01, // min: 1 page

        // Export section (id=7)
        0x07,
        0x0A, // section size
        0x01, // number of exports
        0x06, // name length
        'm',
        'e',
        'm',
        'o',
        'r',
        'y',
        0x02, // export kind: memory
        0x00, // memory index

        // Code section (id=10)
        0x0A,
        0x04, // section size
        0x01, // number of functions
        0x02, // body size
        0x00, // locals count
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
  @DisplayName("Memory Bounds Enforcement Tests")
  class MemoryBoundsEnforcementTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should enforce memory bounds on read operations")
    void shouldEnforceMemoryBoundsOnRead(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_BOUNDS_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction readFunc = instance.getFunction("read").orElse(null);
        final WasmMemory memory = instance.getMemory("memory").orElse(null);

        assertNotNull(readFunc);
        assertNotNull(memory);

        // Memory is 1 page = 65536 bytes
        final long memorySize = memory.getSize() * 65536;
        LOGGER.info("Memory size: " + memorySize + " bytes");

        // Valid read at offset 0
        final WasmValue[] result = readFunc.call(WasmValue.i32(0));
        LOGGER.info("Read at offset 0: " + result[0].asInt());

        // Invalid read beyond memory bounds - should trap
        final TrapException readTrap =
            assertThrows(
                TrapException.class, () -> readFunc.call(WasmValue.i32((int) memorySize + 1000)));
        LOGGER.info("Out of bounds read trap: " + readTrap.getMessage());
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should enforce memory bounds on write operations")
    void shouldEnforceMemoryBoundsOnWrite(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_BOUNDS_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction writeFunc = instance.getFunction("write").orElse(null);
        final WasmMemory memory = instance.getMemory("memory").orElse(null);

        assertNotNull(writeFunc);
        assertNotNull(memory);

        final long memorySize = memory.getSize() * 65536;

        // Valid write at offset 0
        writeFunc.call(WasmValue.i32(0), WasmValue.i32(42));
        LOGGER.info("Write at offset 0 succeeded");

        // Invalid write beyond memory bounds - should trap
        final TrapException writeTrap =
            assertThrows(
                TrapException.class,
                () -> writeFunc.call(WasmValue.i32((int) memorySize + 1000), WasmValue.i32(42)));
        LOGGER.info("Out of bounds write trap: " + writeTrap.getMessage());
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Instance Memory Isolation Tests")
  class InstanceMemoryIsolationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should isolate memory between different instances")
    void shouldIsolateMemoryBetweenInstances(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_BOUNDS_WASM);

      try {
        // Create two instances
        final Instance instance1 = store.createInstance(module);
        final Instance instance2 = store.createInstance(module);

        final WasmMemory memory1 = instance1.getMemory("memory").orElse(null);
        final WasmMemory memory2 = instance2.getMemory("memory").orElse(null);

        assertNotNull(memory1);
        assertNotNull(memory2);

        // Write different values to each memory at same offset
        memory1.writeByte(100, (byte) 0xAA);
        memory2.writeByte(100, (byte) 0xBB);

        // Verify memories are independent
        final byte value1 = memory1.readByte(100);
        final byte value2 = memory2.readByte(100);

        assertEquals((byte) 0xAA, value1);
        assertEquals((byte) 0xBB, value2);
        assertNotEquals(value1, value2);

        LOGGER.info("Memory isolation verified: instance1=" + value1 + ", instance2=" + value2);
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should isolate memory between stores")
    void shouldIsolateMemoryBetweenStores(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_BOUNDS_WASM);

      try (Store store2 = engine.createStore()) {
        // Create instances in different stores
        final Instance instance1 = store.createInstance(module);
        final Instance instance2 = store2.createInstance(module);

        final WasmMemory memory1 = instance1.getMemory("memory").orElse(null);
        final WasmMemory memory2 = instance2.getMemory("memory").orElse(null);

        // Write to different stores
        memory1.writeByte(200, (byte) 0x11);
        memory2.writeByte(200, (byte) 0x22);

        // Verify isolation
        assertEquals((byte) 0x11, memory1.readByte(200));
        assertEquals((byte) 0x22, memory2.readByte(200));

        LOGGER.info("Cross-store memory isolation verified");
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Host Memory Protection Tests")
  class HostMemoryProtectionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should prevent WASM from accessing host memory")
    void shouldPreventWasmFromAccessingHostMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_BOUNDS_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmFunction readFunc = instance.getFunction("read").orElse(null);

        // Attempt to read using negative offset (which could wrap to host memory)
        // This should be handled safely
        try {
          readFunc.call(WasmValue.i32(-1));
          org.junit.jupiter.api.Assertions.fail("Expected exception for negative offset");
        } catch (final Exception e) {
          LOGGER.info("Negative offset handled: " + e.getClass().getName());
          LOGGER.info("Message: " + e.getMessage());
        }
      } finally {
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should validate all memory operations")
    void shouldValidateAllMemoryOperations(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(MEMORY_BOUNDS_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmMemory memory = instance.getMemory("memory").orElse(null);

        // Test various boundary conditions
        final int pageSize = 65536;
        final long memoryPages = memory.getSize();
        final int totalBytes = (int) (memoryPages * pageSize);

        LOGGER.info("Testing memory bounds: " + totalBytes + " bytes");

        // Last valid byte
        memory.writeByte(totalBytes - 1, (byte) 0xFF);
        assertEquals((byte) 0xFF, memory.readByte(totalBytes - 1));
        LOGGER.info("Last valid byte access successful");

        // First invalid byte - should throw
        try {
          memory.readByte(totalBytes);
          org.junit.jupiter.api.Assertions.fail("Expected exception for first invalid byte");
        } catch (final Exception e) {
          LOGGER.info("First invalid byte properly rejected: " + e.getMessage());
        }
      } finally {
        module.close();
      }
    }
  }

  @Nested
  @DisplayName("Memory Zeroing Tests")
  class MemoryZeroingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should initialize memory to zeros")
    void shouldInitializeMemoryToZeros(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();
      store = engine.createStore();

      final Module module = engine.compileModule(TWO_MEMORIES_WASM);

      try {
        final Instance instance = store.createInstance(module);
        final WasmMemory memory = instance.getMemory("memory").orElse(null);

        // Check several random offsets - all should be zero
        for (int i = 0; i < 100; i++) {
          final int offset = (int) (Math.random() * 1000);
          final byte value = memory.readByte(offset);
          assertEquals(
              (byte) 0, value, "Memory at offset " + offset + " should be zero-initialized");
        }

        LOGGER.info("Memory zero-initialization verified");
      } finally {
        module.close();
      }
    }
  }
}
