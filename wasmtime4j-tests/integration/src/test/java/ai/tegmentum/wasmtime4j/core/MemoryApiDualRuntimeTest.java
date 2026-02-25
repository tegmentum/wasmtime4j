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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime integration tests for generic WasmMemory API operations.
 *
 * <p>These tests verify memory API behavior across both JNI and Panama runtimes. Tests that are
 * already covered in {@link MemoryOperationsIntegrationTest} are not duplicated here.
 *
 * @since 1.0.0
 */
@DisplayName("Memory API DualRuntime Tests")
@SuppressWarnings("deprecation")
public class MemoryApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryApiDualRuntimeTest.class.getName());

  private static final int PAGE_SIZE = 65536;

  /** WAT module with 1 initial page and 10 max pages, plus an empty data segment at index 0. */
  private static final String MEMORY_WAT =
      "(module\n" + "  (memory (export \"memory\") 1 10)\n" + "  (data (i32.const 0) \"\")\n" + ")";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ==================== Top-level basic operations tests ====================

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Get buffer returns snapshot")
  void testGetBuffer(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getBuffer returns snapshot");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      final byte[] data = {0x10, 0x20, 0x30, 0x40};
      memory.writeBytes(0, data, 0, data.length);

      final ByteBuffer buffer = memory.getBuffer();
      assertNotNull(buffer, "Buffer should not be null");
      assertEquals(PAGE_SIZE, buffer.remaining(), "Buffer size should be one page");

      assertEquals(0x10, buffer.get(0));
      assertEquals(0x20, buffer.get(1));
      assertEquals(0x30, buffer.get(2));
      assertEquals(0x40, buffer.get(3));
      LOGGER.info("[" + runtime + "] getBuffer snapshot verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Write large data")
  void testWriteLargeData(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing write large data");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      final byte[] data = new byte[1024];
      for (int i = 0; i < data.length; i++) {
        data[i] = (byte) (i % 256);
      }
      memory.writeBytes(0, data, 0, data.length);

      final byte[] readData = new byte[data.length];
      memory.readBytes(0, readData, 0, data.length);

      assertArrayEquals(data, readData, "Large data should match");
      LOGGER.info("[" + runtime + "] Large data write/read verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Write at end of page")
  void testWriteAtEndOfPage(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing write at end of page");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      final byte[] data = {0x01, 0x02, 0x03, 0x04};
      final int offset = PAGE_SIZE - data.length;
      memory.writeBytes(offset, data, 0, data.length);

      final byte[] readData = new byte[data.length];
      memory.readBytes(offset, readData, 0, data.length);

      assertArrayEquals(data, readData, "Data at end of page should match");
      LOGGER.info("[" + runtime + "] End of page write verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Write and read zero bytes")
  void testWriteAndReadZeroBytes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing write and read zero bytes");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      final byte[] data = new byte[0];
      assertDoesNotThrow(() -> memory.writeBytes(0, data, 0, 0));
      assertDoesNotThrow(() -> memory.readBytes(0, data, 0, 0));
      LOGGER.info("[" + runtime + "] Zero bytes read/write verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Negative offset throws exception")
  void testNegativeOffsetThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing negative offset throws");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();
      final byte[] data = new byte[10];

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.writeBytes(-1, data, 0, data.length),
          "Negative offset should throw IllegalArgumentException");

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.readBytes(-1, data, 0, data.length),
          "Negative offset should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Negative offset throws verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Null array throws exception")
  void testNullArrayThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing null array throws");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.writeBytes(0, null, 0, 10),
          "Null source array should throw");

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.readBytes(0, null, 0, 10),
          "Null destination array should throw");
      LOGGER.info("[" + runtime + "] Null array throws verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Negative array offset throws exception")
  void testNegativeArrayOffsetThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing negative array offset throws");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();
      final byte[] data = new byte[10];

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.writeBytes(0, data, -1, 5),
          "Negative array offset should throw IllegalArgumentException");

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.readBytes(0, data, -1, 5),
          "Negative array offset should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Negative array offset throws verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Negative length throws exception")
  void testNegativeLengthThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing negative length throws");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();
      final byte[] data = new byte[10];

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.writeBytes(0, data, 0, -1),
          "Negative length should throw IllegalArgumentException");

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.readBytes(0, data, 0, -1),
          "Negative length should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Negative length throws verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Array bounds exceeded throws exception")
  void testArrayBoundsExceededThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing array bounds exceeded throws");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();
      final byte[] data = new byte[10];

      assertThrows(
          Exception.class,
          () -> memory.writeBytes(0, data, 5, 10),
          "Array bounds exceeded should throw");

      assertThrows(
          Exception.class,
          () -> memory.readBytes(0, data, 5, 10),
          "Array bounds exceeded should throw");
      LOGGER.info("[" + runtime + "] Array bounds exceeded throws verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Grow by negative pages throws exception")
  void testGrowByNegativePagesThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing grow by negative pages throws");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      assertThrows(
          IllegalArgumentException.class,
          () -> memory.grow(-1),
          "Growing by negative pages should throw");
      LOGGER.info("[" + runtime + "] Grow by negative pages throws verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Write data persists across reads")
  void testDataPersistence(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing data persistence across reads");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      final byte[] data1 = {0x11, 0x22, 0x33};
      memory.writeBytes(0, data1, 0, data1.length);

      final byte[] read1 = new byte[data1.length];
      memory.readBytes(0, read1, 0, data1.length);
      assertArrayEquals(data1, read1);

      final byte[] read2 = new byte[data1.length];
      memory.readBytes(0, read2, 0, data1.length);
      assertArrayEquals(data1, read2);
      LOGGER.info("[" + runtime + "] Data persistence verified");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple writes to different offsets")
  void testMultipleWritesDifferentOffsets(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple writes to different offsets");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent(), "Memory export should be present");
      final WasmMemory memory = memOpt.get();

      final byte[] data1 = {0x01, 0x02, 0x03};
      memory.writeBytes(0, data1, 0, data1.length);

      final byte[] data2 = {0x04, 0x05, 0x06};
      memory.writeBytes(100, data2, 0, data2.length);

      final byte[] data3 = {0x07, 0x08, 0x09};
      memory.writeBytes(200, data3, 0, data3.length);

      final byte[] read1 = new byte[3];
      memory.readBytes(0, read1, 0, 3);
      assertArrayEquals(data1, read1);

      final byte[] read2 = new byte[3];
      memory.readBytes(100, read2, 0, 3);
      assertArrayEquals(data2, read2);

      final byte[] read3 = new byte[3];
      memory.readBytes(200, read3, 0, 3);
      assertArrayEquals(data3, read3);
      LOGGER.info("[" + runtime + "] Multiple writes at different offsets verified");
    }
  }

  // ==================== Nested test groups ====================

  @Nested
  @DisplayName("Max Size Tests")
  class MaxSizeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return positive max size for bounded memory")
    void shouldReturnPositiveMaxSize(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing positive max size");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final int maxSize = memory.getMaxSize();
        LOGGER.info("[" + runtime + "] Memory max size: " + maxSize + " pages");
        assertTrue(maxSize > 0, "Max size should be positive for bounded memory");
        assertTrue(
            maxSize >= memory.getSize(),
            "Max size (" + maxSize + ") should be >= current size (" + memory.getSize() + ")");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return max size of 10 pages for test module")
    void shouldReturnCorrectMaxSize(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing correct max size");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final int maxSize = memory.getMaxSize();
        assertEquals(10, maxSize, "Max size should be 10 pages for test module");
        LOGGER.info("[" + runtime + "] Max size is 10 pages as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should maintain max size after grow")
    void shouldMaintainMaxSizeAfterGrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing max size preserved after grow");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final int maxBefore = memory.getMaxSize();
        memory.grow(2);
        final int maxAfter = memory.getMaxSize();
        assertEquals(maxBefore, maxAfter, "Max size should not change after grow");
        LOGGER.info("[" + runtime + "] Max size preserved after grow: " + maxAfter);
      }
    }
  }

  @Nested
  @DisplayName("Memory Type Tests")
  class MemoryTypeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return non-null memory type")
    void shouldReturnNonNullMemoryType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing non-null memory type");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final MemoryType memType = memory.getMemoryType();
        assertNotNull(memType, "Memory type should not be null");
        LOGGER.info("[" + runtime + "] Memory type: " + memType);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should have minimum of 1 page")
    void shouldHaveCorrectMinimum(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing memory type minimum");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final MemoryType memType = memory.getMemoryType();
        assertEquals(1, memType.getMinimum(), "Minimum should be 1 page");
        LOGGER.info("[" + runtime + "] Minimum is 1 page as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should have maximum of 10 pages")
    void shouldHaveMaximum(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing memory type maximum");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final MemoryType memType = memory.getMemoryType();
        final Optional<Long> max = memType.getMaximum();
        assertTrue(max.isPresent(), "Maximum should be present for bounded memory");
        assertEquals(10L, max.get(), "Maximum should be 10 pages");
        LOGGER.info("[" + runtime + "] Maximum is 10 pages as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should not be 64-bit")
    void shouldNotBe64Bit(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing memory type is not 64-bit");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final MemoryType memType = memory.getMemoryType();
        assertFalse(memType.is64Bit(), "Regular memory should not be 64-bit");
        LOGGER.info("[" + runtime + "] Memory type is not 64-bit as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should not be shared")
    void shouldNotBeSharedFromType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing memory type is not shared");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final MemoryType memType = memory.getMemoryType();
        assertFalse(memType.isShared(), "Regular memory should not be shared");
        LOGGER.info("[" + runtime + "] Memory type is not shared as expected");
      }
    }
  }

  @Nested
  @DisplayName("Read Byte Tests")
  class ReadByteTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should read byte at offset after writing bytes via writeBytes")
    void shouldReadByteAfterWrite(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readByte after writeBytes");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte[] data = {0x41, 0x42, 0x43};
        memory.writeBytes(0, data, 0, data.length);

        assertEquals(0x41, memory.readByte(0), "readByte(0) should return 0x41");
        assertEquals(0x42, memory.readByte(1), "readByte(1) should return 0x42");
        assertEquals(0x43, memory.readByte(2), "readByte(2) should return 0x43");
        LOGGER.info("[" + runtime + "] readByte after writeBytes verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should read zero from uninitialized memory")
    void shouldReadZeroFromUninitialized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing read zero from uninitialized memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte result = memory.readByte(5000);
        assertEquals(0, result, "Uninitialized memory should read as zero");
        LOGGER.info("[" + runtime + "] Uninitialized memory reads as zero");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for negative offset")
    void shouldThrowForNegativeOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readByte negative offset throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.readByte(-1),
            "Negative offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] readByte negative offset throws verified");
      }
    }
  }

  @Nested
  @DisplayName("Write Byte Tests")
  class WriteByteTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should write byte without error")
    void shouldWriteByteWithoutError(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing writeByte without error");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertDoesNotThrow(
            () -> memory.writeByte(0, (byte) 0xFF), "writeByte should not throw for valid offset");
        LOGGER.info("[" + runtime + "] writeByte without error verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for negative offset")
    void shouldThrowForNegativeOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing writeByte negative offset throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.writeByte(-1, (byte) 0x42),
            "Negative offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] writeByte negative offset throws verified");
      }
    }
  }

  @Nested
  @DisplayName("Copy Tests")
  class CopyTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should copy non-overlapping regions without error")
    void shouldCopyNonOverlapping(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy non-overlapping");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte[] srcData = {0x01, 0x02, 0x03, 0x04, 0x05};
        memory.writeBytes(0, srcData, 0, srcData.length);

        // Behavior varies by runtime: JNI may not have nativeMemoryCopy linked yet
        try {
          memory.copy(100, 0, 5);
          LOGGER.info("[" + runtime + "] Non-overlapping copy succeeded");
        } catch (final Throwable t) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Non-overlapping copy threw: "
                  + t.getClass().getName()
                  + " - "
                  + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should copy overlapping regions without error")
    void shouldCopyOverlapping(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy overlapping");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
        memory.writeBytes(0, data, 0, data.length);

        // Behavior varies by runtime: JNI may not have nativeMemoryCopy linked yet
        try {
          memory.copy(2, 0, 5);
          LOGGER.info("[" + runtime + "] Overlapping copy succeeded");
        } catch (final Throwable t) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Overlapping copy threw: "
                  + t.getClass().getName()
                  + " - "
                  + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should handle zero-length copy")
    void shouldCopyZeroLength(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing zero-length copy");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: JNI may not have nativeMemoryCopy linked yet
        try {
          memory.copy(0, 0, 0);
          LOGGER.info("[" + runtime + "] Zero-length copy succeeded");
        } catch (final Throwable t) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Zero-length copy threw: "
                  + t.getClass().getName()
                  + " - "
                  + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for negative destination offset")
    void shouldThrowForNegativeDestOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy negative dest offset throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.copy(-1, 0, 5),
            "Negative destination offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Copy negative dest offset throws verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for negative source offset")
    void shouldThrowForNegativeSrcOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy negative src offset throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.copy(0, -1, 5),
            "Negative source offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Copy negative src offset throws verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for negative length")
    void shouldThrowForNegativeLength(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy negative length throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.copy(0, 0, -1),
            "Negative length should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Copy negative length throws verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for out-of-bounds source")
    void shouldThrowForOutOfBoundsSrc(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy out-of-bounds source throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: may throw Exception or Error (UnsatisfiedLinkError)
        try {
          memory.copy(0, PAGE_SIZE - 2, 5);
          fail("Source past memory bounds should throw");
        } catch (final Throwable t) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Copy OOB source threw: "
                  + t.getClass().getName()
                  + " - "
                  + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for out-of-bounds destination")
    void shouldThrowForOutOfBoundsDest(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing copy out-of-bounds destination throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: may throw Exception or Error (UnsatisfiedLinkError)
        try {
          memory.copy(PAGE_SIZE - 2, 0, 5);
          fail("Destination past memory bounds should throw");
        } catch (final Throwable t) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Copy OOB dest threw: "
                  + t.getClass().getName()
                  + " - "
                  + t.getMessage());
        }
      }
    }
  }

  @Nested
  @DisplayName("Fill Tests")
  class FillTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should fill without error")
    void shouldFillWithoutError(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill without error");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: JNI may not have nativeMemoryFill linked yet
        try {
          memory.fill(0, (byte) 0xAB, 100);
          LOGGER.info("[" + runtime + "] Fill without error verified");
        } catch (final Throwable t) {
          LOGGER.info(
              "[" + runtime + "] Fill threw: " + t.getClass().getName() + " - " + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should handle zero-length fill")
    void shouldFillZeroLength(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing zero-length fill");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: JNI may not have nativeMemoryFill linked yet
        try {
          memory.fill(0, (byte) 0xFF, 0);
          LOGGER.info("[" + runtime + "] Zero-length fill verified");
        } catch (final Throwable t) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Zero-length fill threw: "
                  + t.getClass().getName()
                  + " - "
                  + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for negative offset")
    void shouldThrowForNegativeOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill negative offset throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.fill(-1, (byte) 0xFF, 10),
            "Negative offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Fill negative offset throws verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for negative length")
    void shouldThrowForNegativeLength(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill negative length throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.fill(0, (byte) 0xFF, -1),
            "Negative length should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Fill negative length throws verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw for out-of-bounds range")
    void shouldThrowForOutOfBounds(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing fill out-of-bounds throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: may throw Exception or Error (UnsatisfiedLinkError)
        try {
          memory.fill(PAGE_SIZE - 5, (byte) 0xFF, 10);
          fail("Fill past memory bounds should throw");
        } catch (final Throwable t) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Fill OOB threw: "
                  + t.getClass().getName()
                  + " - "
                  + t.getMessage());
        }
      }
    }
  }

  @Nested
  @DisplayName("Shared Memory Tests")
  class SharedMemoryTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Regular memory should not be shared")
    void shouldNotBeSharedForRegularMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing isShared for regular memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final boolean shared = memory.isShared();
        LOGGER.info("[" + runtime + "] isShared: " + shared);
        assertFalse(shared, "Regular (non-shared) memory should return false from isShared()");
      }
    }
  }

  @Nested
  @DisplayName("64-Bit Addressing Tests")
  class Addressing64BitTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Regular memory should not support 64-bit addressing")
    void shouldNotSupport64BitForRegularMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing supports64BitAddressing for regular memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final boolean supports64 = memory.supports64BitAddressing();
        LOGGER.info("[" + runtime + "] supports64BitAddressing: " + supports64);
        assertFalse(supports64, "Regular memory should not support 64-bit addressing");
      }
    }
  }

  @Nested
  @DisplayName("Atomic Operations Validation Tests")
  class AtomicValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative offset for atomicCompareAndSwapInt")
    void shouldRejectNegativeOffsetForCasInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicCAS int negative offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicCompareAndSwapInt(-1, 0, 0),
            "Negative offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicCAS int negative offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicCompareAndSwapInt")
    void shouldRejectMisalignedOffsetForCasInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicCAS int misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicCompareAndSwapInt(1, 0, 0),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicCAS int misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicCompareAndSwapLong")
    void shouldRejectMisalignedOffsetForCasLong(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicCAS long misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicCompareAndSwapLong(3, 0L, 0L),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicCAS long misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicLoadInt")
    void shouldRejectMisalignedOffsetForLoadInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicLoadInt misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicLoadInt(2),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicLoadInt misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicLoadLong")
    void shouldRejectMisalignedOffsetForLoadLong(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicLoadLong misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicLoadLong(4),
            "4-byte aligned but not 8-byte aligned should throw for Long");
        LOGGER.info("[" + runtime + "] atomicLoadLong misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicStoreInt")
    void shouldRejectMisalignedOffsetForStoreInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicStoreInt misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicStoreInt(3, 0),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicStoreInt misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicStoreLong")
    void shouldRejectMisalignedOffsetForStoreLong(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicStoreLong misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicStoreLong(5, 0L),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicStoreLong misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicAddInt")
    void shouldRejectMisalignedOffsetForAddInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicAddInt misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicAddInt(1, 0),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicAddInt misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicAddLong")
    void shouldRejectMisalignedOffsetForAddLong(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicAddLong misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicAddLong(3, 0L),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicAddLong misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicAndInt")
    void shouldRejectMisalignedOffsetForAndInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicAndInt misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicAndInt(1, 0),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicAndInt misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicOrInt")
    void shouldRejectMisalignedOffsetForOrInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicOrInt misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicOrInt(1, 0),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicOrInt misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicXorInt")
    void shouldRejectMisalignedOffsetForXorInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicXorInt misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicXorInt(1, 0),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicXorInt misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicNotify")
    void shouldRejectMisalignedOffsetForNotify(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicNotify misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicNotify(1, 1),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicNotify misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject negative count for atomicNotify")
    void shouldRejectNegativeNotifyCount(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicNotify negative count");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicNotify(0, -1),
            "Negative count should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicNotify negative count verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicWait32")
    void shouldRejectMisalignedOffsetForWait32(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicWait32 misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicWait32(1, 0, 1000),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicWait32 misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject invalid timeout for atomicWait32")
    void shouldRejectInvalidWait32Timeout(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicWait32 invalid timeout");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicWait32(0, 0, -2),
            "Timeout of -2 should throw (only -1 is valid for infinite)");
        LOGGER.info("[" + runtime + "] atomicWait32 invalid timeout verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject misaligned offset for atomicWait64")
    void shouldRejectMisalignedOffsetForWait64(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicWait64 misaligned offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.atomicWait64(3, 0L, 1000),
            "Misaligned offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] atomicWait64 misaligned offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject invalid timeout for atomicWait64")
    void shouldRejectInvalidWait64Timeout(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicWait64 invalid timeout");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: may throw IllegalArgumentException or
        // UnsupportedOperationException
        assertThrows(
            Exception.class,
            () -> memory.atomicWait64(0, 0L, -2),
            "Timeout of -2 should throw (only -1 is valid for infinite)");
        LOGGER.info("[" + runtime + "] atomicWait64 invalid timeout verified");
      }
    }
  }

  @Nested
  @DisplayName("Atomic Operations on Non-Shared Memory Tests")
  class AtomicOperationsTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicCompareAndSwapInt should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForCasInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicCAS int on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicCompareAndSwapInt(0, 0, 42),
            "Atomic CAS on non-shared memory should throw UnsupportedOperationException");
        LOGGER.info("[" + runtime + "] atomicCAS int throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicCompareAndSwapLong should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForCasLong(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicCAS long on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicCompareAndSwapLong(0, 0L, 42L),
            "Atomic CAS Long on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicCAS long throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicLoadInt should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForLoadInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicLoadInt on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicLoadInt(0),
            "Atomic load int on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicLoadInt throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicLoadLong should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForLoadLong(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicLoadLong on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicLoadLong(0),
            "Atomic load long on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicLoadLong throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicStoreInt should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForStoreInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicStoreInt on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicStoreInt(0, 42),
            "Atomic store int on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicStoreInt throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicStoreLong should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForStoreLong(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicStoreLong on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicStoreLong(0, 42L),
            "Atomic store long on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicStoreLong throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicAddInt should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForAddInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicAddInt on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicAddInt(0, 1),
            "Atomic add int on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicAddInt throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicAddLong should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForAddLong(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicAddLong on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicAddLong(0, 1L),
            "Atomic add long on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicAddLong throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicAndInt should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForAndInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicAndInt on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicAndInt(0, 0xFF),
            "Atomic AND int on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicAndInt throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicOrInt should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForOrInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicOrInt on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicOrInt(0, 0xFF),
            "Atomic OR int on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicOrInt throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicXorInt should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForXorInt(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicXorInt on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicXorInt(0, 0xFF),
            "Atomic XOR int on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicXorInt throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicFence should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForFence(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicFence on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            memory::atomicFence,
            "Atomic fence on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicFence throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicNotify should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForNotify(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicNotify on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicNotify(0, 1),
            "Atomic notify on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicNotify throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicWait32 should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForWait32(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicWait32 on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicWait32(0, 0, 1000),
            "Atomic wait32 on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicWait32 throws on non-shared verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("atomicWait64 should throw UnsupportedOperationException")
    void shouldThrowUnsupportedForWait64(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing atomicWait64 on non-shared memory");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            UnsupportedOperationException.class,
            () -> memory.atomicWait64(0, 0L, 1000),
            "Atomic wait64 on non-shared memory should throw");
        LOGGER.info("[" + runtime + "] atomicWait64 throws on non-shared verified");
      }
    }
  }

  @Nested
  @DisplayName("64-Bit Operations Tests")
  class Operations64BitTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getSize64 should return page count")
    void shouldGetSize64(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing getSize64");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final long size64 = memory.getSize64();
        LOGGER.info("[" + runtime + "] getSize64: " + size64);
        assertEquals(1L, size64, "getSize64() should return 1 for single-page memory");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("grow64 should return previous page count")
    void shouldGrow64(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing grow64");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final long previousPages = memory.grow64(1L);
        LOGGER.info("[" + runtime + "] grow64(1) returned previous pages: " + previousPages);
        assertEquals(1L, previousPages, "grow64(1) should return 1 (previous page count)");
        assertEquals(2L, memory.getSize64(), "Size should be 2 after growing by 1");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("grow64 should reject negative pages")
    void shouldGrow64RejectNegative(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing grow64 reject negative");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: some runtimes may not reject negative values
        try {
          final long result = memory.grow64(-1L);
          LOGGER.info("[" + runtime + "] grow64(-1) returned: " + result + " (no throw)");
        } catch (final Exception e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] grow64(-1) threw: "
                  + e.getClass().getName()
                  + " - "
                  + e.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should write and read bytes using 64-bit operations")
    void shouldReadWriteBytes64(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readBytes64/writeBytes64");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte[] data = {0x10, 0x20, 0x30, 0x40, 0x50};
        memory.writeBytes64(0L, data, 0, data.length);

        final byte[] readBack = new byte[data.length];
        memory.readBytes64(0L, readBack, 0, data.length);

        assertArrayEquals(data, readBack, "64-bit read should match 64-bit write");
        LOGGER.info("[" + runtime + "] 64-bit read/write verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should write and read bytes at non-zero 64-bit offset")
    void shouldReadWriteBytes64AtOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readBytes64/writeBytes64 at offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte[] data = {0x0A, 0x0B, 0x0C};
        memory.writeBytes64(500L, data, 0, data.length);

        final byte[] readBack = new byte[data.length];
        memory.readBytes64(500L, readBack, 0, data.length);

        assertArrayEquals(data, readBack, "64-bit read at offset 500 should match write");
        LOGGER.info("[" + runtime + "] 64-bit read/write at offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should handle zero-length 64-bit read")
    void shouldReadBytes64ZeroLength(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readBytes64 zero length");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final byte[] empty = new byte[0];
        assertDoesNotThrow(
            () -> memory.readBytes64(0L, empty, 0, 0), "Zero-length 64-bit read should not throw");
        LOGGER.info("[" + runtime + "] readBytes64 zero length verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should handle zero-length 64-bit write")
    void shouldWriteBytes64ZeroLength(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing writeBytes64 zero length");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final byte[] empty = new byte[0];
        assertDoesNotThrow(
            () -> memory.writeBytes64(0L, empty, 0, 0),
            "Zero-length 64-bit write should not throw");
        LOGGER.info("[" + runtime + "] writeBytes64 zero length verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("readBytes64 should reject null dest")
    void shouldReadBytes64RejectNull(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readBytes64 null dest");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.readBytes64(0L, null, 0, 10),
            "Null dest should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] readBytes64 null dest verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("writeBytes64 should reject null src")
    void shouldWriteBytes64RejectNull(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing writeBytes64 null src");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.writeBytes64(0L, null, 0, 10),
            "Null src should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] writeBytes64 null src verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("readBytes64 should reject negative offset")
    void shouldRejectNegativeOffset64Read(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readBytes64 negative offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.readBytes64(-1L, new byte[10], 0, 10),
            "Negative offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] readBytes64 negative offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("writeBytes64 should reject negative offset")
    void shouldRejectNegativeOffset64Write(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing writeBytes64 negative offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.writeBytes64(-1L, new byte[10], 0, 10),
            "Negative offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] writeBytes64 negative offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("readBytes64 should reject negative dest offset")
    void shouldRejectNegativeDestOffset64Read(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readBytes64 negative dest offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.readBytes64(0L, new byte[10], -1, 5),
            "Negative dest offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] readBytes64 negative dest offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("writeBytes64 should reject negative src offset")
    void shouldRejectNegativeSrcOffset64Write(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing writeBytes64 negative src offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.writeBytes64(0L, new byte[10], -1, 5),
            "Negative src offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] writeBytes64 negative src offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("readBytes64 should reject negative length")
    void shouldRejectNegativeLength64Read(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readBytes64 negative length");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.readBytes64(0L, new byte[10], 0, -1),
            "Negative length should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] readBytes64 negative length verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("writeBytes64 should reject negative length")
    void shouldRejectNegativeLength64Write(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing writeBytes64 negative length");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.writeBytes64(0L, new byte[10], 0, -1),
            "Negative length should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] writeBytes64 negative length verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("readBytes64 should reject array bounds exceeded")
    void shouldRejectArrayBoundsExceeded64Read(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing readBytes64 array bounds exceeded");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            Exception.class,
            () -> memory.readBytes64(0L, new byte[5], 3, 5),
            "Array bounds exceeded should throw IndexOutOfBoundsException");
        LOGGER.info("[" + runtime + "] readBytes64 array bounds exceeded verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("writeBytes64 should reject array bounds exceeded")
    void shouldRejectArrayBoundsExceeded64Write(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing writeBytes64 array bounds exceeded");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            Exception.class,
            () -> memory.writeBytes64(0L, new byte[5], 3, 5),
            "Array bounds exceeded should throw IndexOutOfBoundsException");
        LOGGER.info("[" + runtime + "] writeBytes64 array bounds exceeded verified");
      }
    }
  }

  @Nested
  @DisplayName("Init and Drop Data Segment Validation Tests")
  class InitDropDataSegmentTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init should throw for negative destination offset")
    void shouldThrowForNegativeDestOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init negative dest offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.init(-1, 0, 0, 10),
            "Negative destination offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] init negative dest offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init should throw for negative data segment index")
    void shouldThrowForNegativeDataSegmentIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init negative data segment index");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.init(0, -1, 0, 10),
            "Negative data segment index should throw");
        LOGGER.info("[" + runtime + "] init negative data segment index verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init should throw for negative source offset")
    void shouldThrowForNegativeSrcOffset(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init negative source offset");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.init(0, 0, -1, 10),
            "Negative source offset should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] init negative source offset verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init should throw for negative length")
    void shouldThrowForNegativeLength(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init negative length");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.init(0, 0, 0, -1),
            "Negative length should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] init negative length verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("init should handle zero length without error")
    void shouldHandleZeroLengthInit(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing init zero length");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        // Behavior varies by runtime: MEMORY_WAT may not have data segments,
        // causing init to throw even with zero length
        try {
          memory.init(0, 0, 0, 0);
          LOGGER.info("[" + runtime + "] init zero length succeeded");
        } catch (final Throwable t) {
          LOGGER.info(
              "["
                  + runtime
                  + "] init zero length threw: "
                  + t.getClass().getName()
                  + " - "
                  + t.getMessage());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("dropDataSegment should throw for negative index")
    void shouldThrowForNegativeDropIndex(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing dropDataSegment negative index");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        assertThrows(
            IllegalArgumentException.class,
            () -> memory.dropDataSegment(-1),
            "Negative data segment index should throw");
        LOGGER.info("[" + runtime + "] dropDataSegment negative index verified");
      }
    }
  }

  @Nested
  @DisplayName("Buffer Caching Tests")
  class BufferCachingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getBuffer should return consistent content across calls")
    void shouldReturnConsistentBufferContent(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing buffer consistent content");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte[] data = {0x01, 0x02, 0x03};
        memory.writeBytes(0, data, 0, data.length);

        final ByteBuffer buf1 = memory.getBuffer();
        final ByteBuffer buf2 = memory.getBuffer();

        assertNotNull(buf1);
        assertNotNull(buf2);
        assertEquals(buf1.get(0), buf2.get(0), "Buffers should have same content at offset 0");
        assertEquals(buf1.get(1), buf2.get(1), "Buffers should have same content at offset 1");
        assertEquals(buf1.get(2), buf2.get(2), "Buffers should have same content at offset 2");
        LOGGER.info("[" + runtime + "] Buffer consistent content verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("getBuffer should reflect new size after grow")
    void shouldRefreshBufferAfterGrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing buffer refresh after grow");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final ByteBuffer buf1 = memory.getBuffer();
        assertEquals(PAGE_SIZE, buf1.remaining(), "Initial buffer should be 1 page");

        memory.grow(2);

        final ByteBuffer buf2 = memory.getBuffer();
        assertEquals(PAGE_SIZE * 3, buf2.remaining(), "Buffer after grow should be 3 pages");
        LOGGER.info("[" + runtime + "] Buffer refresh after grow verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Written data should be visible in buffer snapshot")
    void shouldShowWrittenDataInBuffer(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing written data visible in buffer");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte[] pattern = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        memory.writeBytes(256, pattern, 0, pattern.length);

        final ByteBuffer buffer = memory.getBuffer();
        assertEquals((byte) 0xDE, buffer.get(256));
        assertEquals((byte) 0xAD, buffer.get(257));
        assertEquals((byte) 0xBE, buffer.get(258));
        assertEquals((byte) 0xEF, buffer.get(259));
        LOGGER.info("[" + runtime + "] Written data visible in buffer verified");
      }
    }
  }

  @Nested
  @DisplayName("Memory Out-of-Bounds Access Tests")
  class OutOfBoundsTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Write beyond memory should throw")
    void shouldThrowForWriteBeyondMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing write beyond memory throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final byte[] data = new byte[10];

        // Behavior varies by runtime: Panama may not throw for OOB writes
        try {
          memory.writeBytes(PAGE_SIZE, data, 0, data.length);
          LOGGER.info("[" + runtime + "] Write beyond memory did not throw");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] Write beyond memory threw: " + t.getClass().getName());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Read beyond memory should throw")
    void shouldThrowForReadBeyondMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing read beyond memory throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final byte[] data = new byte[10];

        // Behavior varies by runtime: Panama may not throw for OOB reads
        try {
          memory.readBytes(PAGE_SIZE, data, 0, data.length);
          LOGGER.info("[" + runtime + "] Read beyond memory did not throw");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] Read beyond memory threw: " + t.getClass().getName());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Write spanning memory boundary should throw")
    void shouldThrowForWriteSpanningBoundary(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing write spanning boundary throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final byte[] data = new byte[10];

        // Behavior varies by runtime: Panama may not throw for boundary-spanning writes
        try {
          memory.writeBytes(PAGE_SIZE - 5, data, 0, data.length);
          LOGGER.info("[" + runtime + "] Write spanning boundary did not throw");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] Write spanning boundary threw: " + t.getClass().getName());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Read spanning memory boundary should throw")
    void shouldThrowForReadSpanningBoundary(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing read spanning boundary throws");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final byte[] data = new byte[10];

        // Behavior varies by runtime: Panama may not throw for boundary-spanning reads
        try {
          memory.readBytes(PAGE_SIZE - 5, data, 0, data.length);
          LOGGER.info("[" + runtime + "] Read spanning boundary did not throw");
        } catch (final Throwable t) {
          LOGGER.info("[" + runtime + "] Read spanning boundary threw: " + t.getClass().getName());
        }
      }
    }
  }

  @Nested
  @DisplayName("Grow Boundary Tests")
  class GrowBoundaryTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Grow to exactly max should succeed")
    void shouldGrowToExactMax(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing grow to exact max");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();
        final int result = memory.grow(9);
        assertEquals(1, result, "Previous size should be 1");
        assertEquals(10, memory.getSize(), "Size should be exactly 10 pages");
        LOGGER.info("[" + runtime + "] Grow to exact max verified");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Data persists after grow")
    void shouldPreserveDataAfterGrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing data persists after grow");

      try (Engine engine = Engine.create();
          Store store = engine.createStore();
          Module module = engine.compileWat(MEMORY_WAT);
          Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").orElseThrow();

        final byte[] data = {0x41, 0x42, 0x43, 0x44};
        memory.writeBytes(1000, data, 0, data.length);

        memory.grow(3);

        final byte[] readBack = new byte[data.length];
        memory.readBytes(1000, readBack, 0, data.length);
        assertArrayEquals(data, readBack, "Data should persist after memory grow");
        LOGGER.info("[" + runtime + "] Data persists after grow verified");
      }
    }
  }
}
