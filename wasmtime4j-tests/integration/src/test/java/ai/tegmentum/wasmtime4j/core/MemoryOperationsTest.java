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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WebAssembly linear memory operations.
 *
 * <p>These tests verify memory creation, reading, writing, growing, and bounds checking.
 *
 * @since 1.0.0
 */
@DisplayName("Memory Operations Integration Tests")
public class MemoryOperationsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryOperationsTest.class.getName());

  /** Page size in bytes (64KB). */
  private static final int PAGE_SIZE = 65536;

  /** WebAssembly module with one page of memory exported. */
  private static final byte[] MEMORY_1_PAGE_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x05,
        0x03, // memory section
        0x01, // 1 memory
        0x00,
        0x01, // min 1 page, no max
        0x07,
        0x0A, // export section
        0x01, // 1 export
        0x06,
        0x6D,
        0x65,
        0x6D,
        0x6F,
        0x72,
        0x79, // "memory"
        0x02,
        0x00 // memory export, index 0
      };

  /** WebAssembly module with 2 pages initial, 4 pages max. */
  private static final byte[] MEMORY_2_4_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x05,
        0x04, // memory section
        0x01, // 1 memory
        0x01,
        0x02,
        0x04, // min 2 pages, max 4 pages
        0x07,
        0x0A, // export section
        0x01, // 1 export
        0x06,
        0x6D,
        0x65,
        0x6D,
        0x6F,
        0x72,
        0x79, // "memory"
        0x02,
        0x00 // memory export, index 0
      };

  /** WebAssembly module with memory and store/load functions. */
  private static final byte[] MEMORY_FUNCS_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x0B, // type section
        0x02, // 2 function types
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x00, // (i32, i32) -> void (store)
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // (i32) -> i32 (load)
        0x03,
        0x03, // function section
        0x02,
        0x00,
        0x01, // 2 functions
        0x05,
        0x03, // memory section
        0x01, // 1 memory
        0x00,
        0x01, // min 1 page
        0x07,
        0x19, // export section (25 bytes: count + 3 exports)
        0x03, // 3 exports
        0x06,
        0x6D,
        0x65,
        0x6D,
        0x6F,
        0x72,
        0x79,
        0x02,
        0x00, // "memory", mem 0
        0x05,
        0x73,
        0x74,
        0x6F,
        0x72,
        0x65,
        0x00,
        0x00, // "store", func 0
        0x04,
        0x6C,
        0x6F,
        0x61,
        0x64,
        0x00,
        0x01, // "load", func 1
        0x0A,
        0x13, // code section (19 bytes: count + 2 bodies)
        0x02, // 2 function bodies
        0x09, // body 1 size (9 bytes: store function)
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0 (address)
        0x20,
        0x01, // local.get 1 (value)
        0x36,
        0x02,
        0x00, // i32.store align=2 offset=0
        0x0B, // end
        0x07, // body 2 size (load)
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0 (address)
        0x28,
        0x02,
        0x00, // i32.load align=2 offset=0
        0x0B // end
      };

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Memory Size Tests")
  class MemorySizeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should report correct initial memory size in pages")
    void shouldReportCorrectInitialMemorySizeInPages(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing initial memory size in pages");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();
        assertEquals(1, memory.getSize(), "Memory should have 1 page initially");
        LOGGER.info("Memory size: " + memory.getSize() + " pages");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should report correct initial memory size in bytes")
    void shouldReportCorrectInitialMemorySizeInBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing initial memory size in bytes");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();
        assertEquals(PAGE_SIZE, memory.dataSize(), "Memory should have 64KB (1 page)");
        LOGGER.info("Memory size: " + memory.dataSize() + " bytes");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should report correct size for multi-page memory")
    void shouldReportCorrectSizeForMultiPageMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing multi-page memory size");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_2_4_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();
        assertEquals(2, memory.getSize(), "Memory should have 2 pages initially");
        assertEquals(2 * PAGE_SIZE, memory.dataSize(), "Memory should have 128KB (2 pages)");
        LOGGER.info("Memory size: " + memory.getSize() + " pages, " + memory.dataSize() + " bytes");
      }
    }
  }

  @Nested
  @DisplayName("Memory Byte Read/Write Tests")
  class MemoryByteReadWriteTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should write and read single byte")
    void shouldWriteAndReadSingleByte(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing single byte write and read");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Write byte at offset 0
        memory.writeByte(0, (byte) 0x42);
        LOGGER.info("Wrote byte 0x42 at offset 0");

        // Read byte back
        final byte value = memory.readByte(0);
        assertEquals((byte) 0x42, value, "Should read back 0x42");
        LOGGER.info("Read byte: 0x" + Integer.toHexString(value & 0xFF));
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should write and read bytes at various offsets")
    void shouldWriteAndReadBytesAtVariousOffsets(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing byte writes at various offsets");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Write bytes at different offsets
        memory.writeByte(0, (byte) 0x01);
        memory.writeByte(100, (byte) 0x02);
        memory.writeByte(1000, (byte) 0x03);
        memory.writeByte(PAGE_SIZE - 1, (byte) 0xFF); // Last byte of first page

        // Verify all values
        assertEquals((byte) 0x01, memory.readByte(0), "Byte at offset 0 should be 0x01");
        assertEquals((byte) 0x02, memory.readByte(100), "Byte at offset 100 should be 0x02");
        assertEquals((byte) 0x03, memory.readByte(1000), "Byte at offset 1000 should be 0x03");
        assertEquals((byte) 0xFF, memory.readByte(PAGE_SIZE - 1), "Last byte should be 0xFF");

        LOGGER.info("All byte reads verified correctly");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle signed byte values correctly")
    void shouldHandleSignedByteValuesCorrectly(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing signed byte values");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Write negative byte value
        memory.writeByte(0, (byte) -128); // 0x80
        final byte value = memory.readByte(0);
        assertEquals((byte) -128, value, "Should correctly handle signed byte -128");

        memory.writeByte(1, (byte) -1); // 0xFF
        assertEquals((byte) -1, memory.readByte(1), "Should correctly handle signed byte -1");

        LOGGER.info("Signed byte handling verified");
      }
    }
  }

  @Nested
  @DisplayName("Memory Byte Array Read/Write Tests")
  class MemoryByteArrayReadWriteTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should write and read byte array")
    void shouldWriteAndReadByteArray(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing byte array write and read");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Prepare test data
        final byte[] testData = {0x01, 0x02, 0x03, 0x04, 0x05};

        // Write to memory
        memory.writeBytes(0, testData, 0, testData.length);
        LOGGER.info("Wrote " + testData.length + " bytes to memory");

        // Read back
        final byte[] readBuffer = new byte[5];
        memory.readBytes(0, readBuffer, 0, readBuffer.length);
        LOGGER.info("Read " + readBuffer.length + " bytes from memory");

        // Verify data
        for (int i = 0; i < testData.length; i++) {
          assertEquals(testData[i], readBuffer[i], "Byte " + i + " should match");
        }

        LOGGER.info("Byte array read/write verified successfully");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should write byte array at non-zero offset")
    void shouldWriteByteArrayAtNonZeroOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing byte array write at offset");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Write at offset 100
        final byte[] testData = {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC};
        memory.writeBytes(100, testData, 0, testData.length);

        // Verify bytes at offset 100
        assertEquals((byte) 0xAA, memory.readByte(100), "Byte at 100 should be 0xAA");
        assertEquals((byte) 0xBB, memory.readByte(101), "Byte at 101 should be 0xBB");
        assertEquals((byte) 0xCC, memory.readByte(102), "Byte at 102 should be 0xCC");

        // Verify bytes before offset are still zero
        assertEquals((byte) 0x00, memory.readByte(99), "Byte at 99 should be 0x00");

        LOGGER.info("Byte array write at offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle partial array writes")
    void shouldHandlePartialArrayWrites(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing partial array write");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Write only middle portion of array
        final byte[] sourceData = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A};
        // Write bytes 3-7 (indices 2-6, 5 bytes) to memory at offset 0
        memory.writeBytes(0, sourceData, 2, 5);

        // Verify
        assertEquals((byte) 0x03, memory.readByte(0), "First byte should be 0x03");
        assertEquals((byte) 0x04, memory.readByte(1), "Second byte should be 0x04");
        assertEquals((byte) 0x05, memory.readByte(2), "Third byte should be 0x05");
        assertEquals((byte) 0x06, memory.readByte(3), "Fourth byte should be 0x06");
        assertEquals((byte) 0x07, memory.readByte(4), "Fifth byte should be 0x07");

        LOGGER.info("Partial array write verified");
      }
    }
  }

  @Nested
  @DisplayName("Memory Integer Read/Write Tests")
  class MemoryIntegerReadWriteTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should write and read i32 value")
    void shouldWriteAndReadI32Value(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing i32 write and read");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Write i32 at offset 0
        memory.writeInt32(0, 0x12345678);
        LOGGER.info("Wrote i32 0x12345678 at offset 0");

        // Read back
        final int value = memory.readInt32(0);
        assertEquals(0x12345678, value, "Should read back 0x12345678");
        LOGGER.info("Read i32: 0x" + Integer.toHexString(value));
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle negative i32 values")
    void shouldHandleNegativeI32Values(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing negative i32 values");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        memory.writeInt32(0, -1);
        assertEquals(-1, memory.readInt32(0), "Should handle -1 correctly");

        memory.writeInt32(4, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, memory.readInt32(4), "Should handle MIN_VALUE correctly");

        memory.writeInt32(8, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, memory.readInt32(8), "Should handle MAX_VALUE correctly");

        LOGGER.info("Negative i32 handling verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should write and read i64 value")
    void shouldWriteAndReadI64Value(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing i64 write and read");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Write i64 at offset 0
        memory.writeInt64(0, 0x123456789ABCDEF0L);
        LOGGER.info("Wrote i64 0x123456789ABCDEF0 at offset 0");

        // Read back
        final long value = memory.readInt64(0);
        assertEquals(0x123456789ABCDEF0L, value, "Should read back 0x123456789ABCDEF0");
        LOGGER.info("Read i64: 0x" + Long.toHexString(value));
      }
    }
  }

  @Nested
  @DisplayName("Memory Grow Tests")
  class MemoryGrowTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should grow memory by one page")
    void shouldGrowMemoryByOnePage(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing memory grow by 1 page");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        final long initialSize = memory.getSize();
        assertEquals(1, initialSize, "Initial size should be 1 page");

        // Grow by 1 page
        final long previousSize = memory.grow(1);
        assertEquals(1, previousSize, "Previous size should be 1 page");

        final long newSize = memory.getSize();
        assertEquals(2, newSize, "New size should be 2 pages");
        LOGGER.info("Memory grew from " + previousSize + " to " + newSize + " pages");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should grow memory multiple times")
    void shouldGrowMemoryMultipleTimes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing multiple memory grows");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Grow multiple times
        memory.grow(1); // 1 -> 2 pages
        assertEquals(2, memory.getSize(), "Should have 2 pages after first grow");

        memory.grow(2); // 2 -> 4 pages
        assertEquals(4, memory.getSize(), "Should have 4 pages after second grow");

        LOGGER.info("Final memory size: " + memory.getSize() + " pages");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle grow by zero pages")
    void shouldHandleGrowByZeroPages(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing memory grow by 0 pages");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        final long initialSize = memory.getSize();
        assertEquals(1, initialSize, "Initial size should be 1 page");

        // Grow by 0 should succeed and return current size
        final long previousSize = memory.grow(0);
        assertEquals(1, previousSize, "Previous size should be 1 page");

        final long newSize = memory.getSize();
        assertEquals(1, newSize, "Size should remain unchanged after grow(0)");
        LOGGER.info("Memory grow(0) returned " + previousSize + ", size unchanged at " + newSize);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should respect memory maximum limit")
    void shouldRespectMemoryMaximumLimit(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing memory maximum limit");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_2_4_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        assertEquals(2, memory.getSize(), "Initial size should be 2 pages");

        // Grow to maximum (4 pages)
        memory.grow(2);
        assertEquals(4, memory.getSize(), "Should have 4 pages (max)");

        // Try to grow beyond maximum - should fail
        final long result = memory.grow(1);
        assertEquals(-1, result, "Growing beyond max should return -1");
        assertEquals(4, memory.getSize(), "Size should still be 4 pages");

        LOGGER.info("Memory maximum limit enforced correctly");
      }
    }
  }

  @Nested
  @DisplayName("Memory Bounds Checking Tests")
  class MemoryBoundsCheckingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for out-of-bounds byte read")
    void shouldThrowExceptionForOutOfBoundsByteRead(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing out-of-bounds byte read");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Try to read beyond memory bounds
        // May throw WasmException or IndexOutOfBoundsException depending on where bounds check
        // happens
        final Exception readException =
            assertThrows(
                Exception.class,
                () -> memory.readByte(PAGE_SIZE), // Just beyond 1 page
                "Should throw exception for out-of-bounds read");
        assertTrue(
            readException instanceof WasmException
                || readException instanceof IndexOutOfBoundsException,
            "Should throw WasmException or IndexOutOfBoundsException, got: "
                + readException.getClass().getName());

        LOGGER.info("Out-of-bounds read check passed");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for out-of-bounds byte write")
    void shouldThrowExceptionForOutOfBoundsByteWrite(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing out-of-bounds byte write");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Try to write beyond memory bounds
        // May throw WasmException or IndexOutOfBoundsException depending on where bounds check
        // happens
        final Exception writeException =
            assertThrows(
                Exception.class,
                () -> memory.writeByte(PAGE_SIZE, (byte) 0x42),
                "Should throw exception for out-of-bounds write");
        assertTrue(
            writeException instanceof WasmException
                || writeException instanceof IndexOutOfBoundsException,
            "Should throw WasmException or IndexOutOfBoundsException, got: "
                + writeException.getClass().getName());

        LOGGER.info("Out-of-bounds write check passed");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should allow access after memory grow")
    void shouldAllowAccessAfterMemoryGrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing access after memory grow");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");

        final WasmMemory memory = memOpt.get();

        // Initially can't access offset 65536 (page 2)
        // May throw WasmException or IndexOutOfBoundsException depending on where bounds check
        // happens
        final Exception beforeGrowException =
            assertThrows(
                Exception.class, () -> memory.readByte(PAGE_SIZE), "Should throw before grow");
        assertTrue(
            beforeGrowException instanceof WasmException
                || beforeGrowException instanceof IndexOutOfBoundsException,
            "Should throw WasmException or IndexOutOfBoundsException, got: "
                + beforeGrowException.getClass().getName());

        // Grow memory
        memory.grow(1);

        // Now access should succeed
        memory.writeByte(PAGE_SIZE, (byte) 0xAB);
        assertEquals((byte) 0xAB, memory.readByte(PAGE_SIZE), "Should be able to access page 2");

        LOGGER.info("Access after grow verified");
      }
    }
  }

  @Nested
  @DisplayName("Memory and Function Integration Tests")
  class MemoryAndFunctionIntegrationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should use WASM store and load functions")
    void shouldUseWasmStoreAndLoadFunctions(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing WASM store/load functions");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_FUNCS_WASM);

        final Instance instance = module.instantiate(store);

        // Get functions
        final Optional<WasmFunction> storeFunc = instance.getFunction("store");
        final Optional<WasmFunction> loadFunc = instance.getFunction("load");

        assertTrue(storeFunc.isPresent(), "store function should be present");
        assertTrue(loadFunc.isPresent(), "load function should be present");

        // Store value using WASM function
        storeFunc.get().call(WasmValue.i32(0), WasmValue.i32(42)); // store at address 0, value 42
        LOGGER.info("Stored value 42 at address 0 using WASM function");

        // Load value using WASM function
        final WasmValue[] results = loadFunc.get().call(WasmValue.i32(0));
        assertNotNull(results, "Load result should not be null");
        assertEquals(1, results.length, "Should have one result");
        assertEquals(42, results[0].asInt(), "Should load back 42");
        LOGGER.info("Loaded value: " + results[0].asInt());

        // Verify we can also read via Memory interface
        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");
        assertEquals(42, memOpt.get().readInt32(0), "Direct memory read should also give 42");

        LOGGER.info("WASM store/load integration verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should share memory between Java and WASM")
    void shouldShareMemoryBetweenJavaAndWasm(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing memory sharing between Java and WASM");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_FUNCS_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");
        final WasmMemory memory = memOpt.get();

        final Optional<WasmFunction> loadFunc = instance.getFunction("load");
        assertTrue(loadFunc.isPresent(), "load function should be present");

        // Write directly to memory from Java
        memory.writeInt32(100, 0x12345678);
        LOGGER.info("Wrote 0x12345678 at address 100 from Java");

        // Read using WASM function
        final WasmValue[] wasmResults = loadFunc.get().call(WasmValue.i32(100));
        assertEquals(0x12345678, wasmResults[0].asInt(), "WASM should see Java's write");
        LOGGER.info("WASM read: 0x" + Integer.toHexString(wasmResults[0].asInt()));

        LOGGER.info("Memory sharing verified");
      }
    }
  }

  /** Tests for float read/write memory operations. */
  @Nested
  @DisplayName("Memory Float Read/Write Tests")
  class MemoryFloatReadWriteTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should write and read float32")
    void shouldWriteAndReadFloat32(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing float32 write and read");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");
        final WasmMemory memory = memOpt.get();

        memory.writeFloat32(0, 3.14f);
        final float result = memory.readFloat32(0);
        assertEquals(3.14f, result, 0.0001f, "Should read back the same float32 value");
        LOGGER.info("Float32 write/read verified: " + result);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should write and read float64")
    void shouldWriteAndReadFloat64(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing float64 write and read");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");
        final WasmMemory memory = memOpt.get();

        memory.writeFloat64(0, 3.141592653589793);
        final double result = memory.readFloat64(0);
        assertEquals(
            3.141592653589793, result, 0.000000000001, "Should read back the same float64 value");
        LOGGER.info("Float64 write/read verified: " + result);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle float32 special values")
    void shouldHandleFloat32SpecialValues(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing float32 special values");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");
        final WasmMemory memory = memOpt.get();

        // NaN
        memory.writeFloat32(0, Float.NaN);
        assertTrue(Float.isNaN(memory.readFloat32(0)), "Should preserve NaN");
        LOGGER.info("Float32 NaN preserved");

        // Positive infinity
        memory.writeFloat32(4, Float.POSITIVE_INFINITY);
        assertEquals(
            Float.POSITIVE_INFINITY, memory.readFloat32(4), "Should preserve positive infinity");
        LOGGER.info("Float32 +Infinity preserved");

        // Negative infinity
        memory.writeFloat32(8, Float.NEGATIVE_INFINITY);
        assertEquals(
            Float.NEGATIVE_INFINITY, memory.readFloat32(8), "Should preserve negative infinity");
        LOGGER.info("Float32 -Infinity preserved");

        // Positive zero
        memory.writeFloat32(12, 0.0f);
        assertEquals(0.0f, memory.readFloat32(12), "Should preserve positive zero");
        assertEquals(
            Float.floatToRawIntBits(0.0f),
            Float.floatToRawIntBits(memory.readFloat32(12)),
            "Should preserve positive zero bit pattern");
        LOGGER.info("Float32 +0.0 preserved");

        // Negative zero
        memory.writeFloat32(16, -0.0f);
        assertEquals(
            Float.floatToRawIntBits(-0.0f),
            Float.floatToRawIntBits(memory.readFloat32(16)),
            "Should preserve negative zero bit pattern");
        LOGGER.info("Float32 -0.0 preserved");

        // Minimum positive normal
        memory.writeFloat32(20, Float.MIN_NORMAL);
        assertEquals(Float.MIN_NORMAL, memory.readFloat32(20), "Should preserve MIN_NORMAL");
        LOGGER.info("Float32 MIN_NORMAL preserved");

        // Minimum positive denormalized
        memory.writeFloat32(24, Float.MIN_VALUE);
        assertEquals(Float.MIN_VALUE, memory.readFloat32(24), "Should preserve MIN_VALUE");
        LOGGER.info("Float32 MIN_VALUE (denormalized) preserved");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle float64 special values")
    void shouldHandleFloat64SpecialValues(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing float64 special values");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(MEMORY_1_PAGE_WASM);

        final Instance instance = module.instantiate(store);

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be present");
        final WasmMemory memory = memOpt.get();

        // NaN
        memory.writeFloat64(0, Double.NaN);
        assertTrue(Double.isNaN(memory.readFloat64(0)), "Should preserve NaN");
        LOGGER.info("Float64 NaN preserved");

        // Positive infinity
        memory.writeFloat64(8, Double.POSITIVE_INFINITY);
        assertEquals(
            Double.POSITIVE_INFINITY, memory.readFloat64(8), "Should preserve positive infinity");
        LOGGER.info("Float64 +Infinity preserved");

        // Negative infinity
        memory.writeFloat64(16, Double.NEGATIVE_INFINITY);
        assertEquals(
            Double.NEGATIVE_INFINITY, memory.readFloat64(16), "Should preserve negative infinity");
        LOGGER.info("Float64 -Infinity preserved");

        // Positive zero
        memory.writeFloat64(24, 0.0);
        assertEquals(0.0, memory.readFloat64(24), "Should preserve positive zero");
        assertEquals(
            Double.doubleToRawLongBits(0.0),
            Double.doubleToRawLongBits(memory.readFloat64(24)),
            "Should preserve positive zero bit pattern");
        LOGGER.info("Float64 +0.0 preserved");

        // Negative zero
        memory.writeFloat64(32, -0.0);
        assertEquals(
            Double.doubleToRawLongBits(-0.0),
            Double.doubleToRawLongBits(memory.readFloat64(32)),
            "Should preserve negative zero bit pattern");
        LOGGER.info("Float64 -0.0 preserved");

        // Minimum positive normal
        memory.writeFloat64(40, Double.MIN_NORMAL);
        assertEquals(Double.MIN_NORMAL, memory.readFloat64(40), "Should preserve MIN_NORMAL");
        LOGGER.info("Float64 MIN_NORMAL preserved");

        // Minimum positive denormalized
        memory.writeFloat64(48, Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, memory.readFloat64(48), "Should preserve MIN_VALUE");
        LOGGER.info("Float64 MIN_VALUE (denormalized) preserved");
      }
    }
  }
}
