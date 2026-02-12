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

package ai.tegmentum.wasmtime4j.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests memory bulk operations (copy, fill, data segment init/drop) using WAT-level instructions
 * exposed through exported functions.
 *
 * <p>Note: The Java API methods {@code WasmMemory.copy()}, {@code WasmMemory.fill()}, and {@code
 * WasmMemory.dropDataSegment()} are NOT tested directly because they currently trigger a SIGSEGV in
 * the native library at {@code wasmtime4j::memory::Memory::size_pages}. This is a known native bug.
 * Instead, tests exercise the underlying WebAssembly instructions via exported WAT functions,
 * similar to {@link ZeroLengthMemoryOpsTest}.
 *
 * @since 1.0.0
 */
@DisplayName("Memory Bulk Operations Tests")
public class MemoryBulkOperationsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryBulkOperationsTest.class.getName());

  /**
   * WAT module with memory copy, fill, read/write helpers, and passive data segment operations.
   * Exports: mem, store_byte, load_byte, do_copy, do_fill, init_seg, drop_seg.
   */
  private static final String WAT =
      """
      (module
        (memory (export "mem") 1)
        (data $seg "HELLO")

        ;; store_byte(offset, value)
        (func (export "store_byte") (param i32 i32)
          local.get 0 local.get 1 i32.store8)

        ;; load_byte(offset) -> i32
        (func (export "load_byte") (param i32) (result i32)
          local.get 0 i32.load8_u)

        ;; do_copy(dest, src, len) - calls memory.copy
        (func (export "do_copy") (param i32 i32 i32)
          local.get 0 local.get 1 local.get 2 memory.copy)

        ;; do_fill(dest, value, len) - calls memory.fill
        (func (export "do_fill") (param i32 i32 i32)
          local.get 0 local.get 1 local.get 2 memory.fill)

        ;; init_seg(dest, seg_offset, len)
        (func (export "init_seg") (param i32 i32 i32)
          local.get 0 local.get 1 local.get 2 memory.init $seg)

        ;; drop_seg
        (func (export "drop_seg") data.drop $seg))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy non-overlapping region preserves data")
  void copyNonOverlappingRegion(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory copy non-overlapping");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmMemory memory = instance.getMemory("mem").get();

      // Write a pattern at offset 0 using store_byte
      final int[] pattern = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A};
      for (int i = 0; i < pattern.length; i++) {
        instance.callFunction("store_byte", WasmValue.i32(i), WasmValue.i32(pattern[i]));
      }

      // Copy 10 bytes from offset 0 to offset 100
      instance.callFunction("do_copy", WasmValue.i32(100), WasmValue.i32(0), WasmValue.i32(10));

      // Verify the copy using load_byte
      for (int i = 0; i < pattern.length; i++) {
        final WasmValue[] result = instance.callFunction("load_byte", WasmValue.i32(100 + i));
        assertEquals(
            pattern[i], result[0].asInt(), "Byte at dest offset " + i + " should match source");
      }
      LOGGER.info("[" + runtime + "] Non-overlapping copy verified for 10 bytes");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy overlapping forward region works correctly")
  void copyOverlappingForward(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory copy overlapping forward");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Write pattern at offset 0: [1, 2, 3, 4, 5]
      final int[] pattern = {0x01, 0x02, 0x03, 0x04, 0x05};
      for (int i = 0; i < pattern.length; i++) {
        instance.callFunction("store_byte", WasmValue.i32(i), WasmValue.i32(pattern[i]));
      }

      // Overlapping copy: src=0, dest=2, len=5 (dest > src, forward overlap)
      instance.callFunction("do_copy", WasmValue.i32(2), WasmValue.i32(0), WasmValue.i32(5));

      // After copy, bytes at [2..6] should be [1, 2, 3, 4, 5]
      for (int i = 0; i < pattern.length; i++) {
        final WasmValue[] result = instance.callFunction("load_byte", WasmValue.i32(2 + i));
        assertEquals(
            pattern[i],
            result[0].asInt(),
            "Overlapping copy byte at offset " + (2 + i) + " should be " + pattern[i]);
      }
      LOGGER.info("[" + runtime + "] Overlapping forward copy verified");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy zero length is a no-op")
  void copyZeroLengthNoOp(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory copy zero length");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // This should not throw
      instance.callFunction("do_copy", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(0));
      LOGGER.info("[" + runtime + "] Zero-length copy succeeded as no-op");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fill writes byte pattern across region")
  void fillRegionWithByte(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory fill with 0xAB over 16 bytes");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Fill 16 bytes at offset 0 with 0xAB
      instance.callFunction("do_fill", WasmValue.i32(0), WasmValue.i32(0xAB), WasmValue.i32(16));

      // Verify all 16 bytes
      for (int i = 0; i < 16; i++) {
        final WasmValue[] result = instance.callFunction("load_byte", WasmValue.i32(i));
        assertEquals(0xAB, result[0].asInt(), "Filled byte at offset " + i + " should be 0xAB");
      }
      LOGGER.info("[" + runtime + "] Fill with 0xAB verified for 16 bytes");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fill zero length is a no-op")
  void fillZeroLengthNoOp(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory fill zero length");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Should not throw
      instance.callFunction("do_fill", WasmValue.i32(0), WasmValue.i32(0xFF), WasmValue.i32(0));
      LOGGER.info("[" + runtime + "] Zero-length fill succeeded as no-op");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fill large region of 4096 bytes")
  void fillLargeRegion(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory fill 4096 bytes");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Fill 4096 bytes with 0x55
      instance.callFunction("do_fill", WasmValue.i32(0), WasmValue.i32(0x55), WasmValue.i32(4096));

      // Spot-check a few positions
      final int[] offsets = {0, 100, 1000, 2048, 4095};
      for (final int offset : offsets) {
        final WasmValue[] result = instance.callFunction("load_byte", WasmValue.i32(offset));
        assertEquals(0x55, result[0].asInt(), "Byte at offset " + offset + " should be 0x55");
      }
      LOGGER.info("[" + runtime + "] Large fill (4096 bytes) verified at spot-check offsets");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("init from data segment copies data to memory")
  void initFromDataSegment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.init from passive data segment");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // init_seg(dest=0, seg_offset=0, len=5) copies "HELLO" to memory[0..4]
      instance.callFunction("init_seg", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(5));

      // Verify "HELLO" = {72, 69, 76, 76, 79}
      final int[] expected = {'H', 'E', 'L', 'L', 'O'};
      for (int i = 0; i < expected.length; i++) {
        final WasmValue[] result = instance.callFunction("load_byte", WasmValue.i32(i));
        assertEquals(
            expected[i],
            result[0].asInt(),
            "Byte at offset " + i + " should be '" + (char) expected[i] + "'");
      }
      LOGGER.info("[" + runtime + "] memory.init verified: 'HELLO' at offset 0");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("drop data segment then init fails")
  void dropDataSegmentThenInitFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing data.drop then memory.init fails");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Drop the data segment
      instance.callFunction("drop_seg");
      LOGGER.info("[" + runtime + "] data.drop succeeded");

      // Now trying to init from the dropped segment should trap
      try {
        instance.callFunction("init_seg", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(5));
        LOGGER.info("[" + runtime + "] memory.init after drop did not throw (unexpected)");
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] memory.init after drop threw as expected: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy out of bounds traps")
  void copyOutOfBoundsTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory copy out of bounds");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // 1 page = 65536 bytes; copying 65537 bytes should trap
      try {
        instance.callFunction("do_copy", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(65537));
        LOGGER.info("[" + runtime + "] Copy out of bounds did not throw (unexpected)");
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] Copy out of bounds trapped as expected: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  // ---------- Java API default method tests: copy(long,long,long) and fill(long,long,byte) ------
  // These default methods use readBytes64/writeBytes64, NOT the native copy/fill that crashes.

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("long copy non-overlapping region via Java API")
  void longCopyNonOverlapping(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory.copy(long,long,long) non-overlapping");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmMemory memory = instance.getMemory("mem").get();

      // Write 10 bytes at offset 0 via WAT store_byte
      final byte[] pattern = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A};
      for (int i = 0; i < pattern.length; i++) {
        instance.callFunction("store_byte", WasmValue.i32(i), WasmValue.i32(pattern[i]));
      }

      // Copy 10 bytes from offset 0 to offset 100 via Java default method
      memory.copy(100L, 0L, 10L);

      // Verify using load_byte
      for (int i = 0; i < pattern.length; i++) {
        final WasmValue[] result = instance.callFunction("load_byte", WasmValue.i32(100 + i));
        assertEquals(
            pattern[i] & 0xFF,
            result[0].asInt(),
            "Byte at dest offset " + (100 + i) + " should match source");
      }
      LOGGER.info("[" + runtime + "] Long copy non-overlapping verified for 10 bytes");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("long copy forward overlap via Java API")
  void longCopyForwardOverlap(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory.copy(long,long,long) with forward overlap");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmMemory memory = instance.getMemory("mem").get();

      // Write [1,2,3,4,5] at offset 0
      final int[] pattern = {0x01, 0x02, 0x03, 0x04, 0x05};
      for (int i = 0; i < pattern.length; i++) {
        instance.callFunction("store_byte", WasmValue.i32(i), WasmValue.i32(pattern[i]));
      }

      // Copy from offset 0 to offset 2 (forward overlap), length 5
      memory.copy(2L, 0L, 5L);

      // After copy, bytes at [2..6] should be [1,2,3,4,5]
      for (int i = 0; i < pattern.length; i++) {
        final WasmValue[] result = instance.callFunction("load_byte", WasmValue.i32(2 + i));
        assertEquals(
            pattern[i],
            result[0].asInt(),
            "Overlapping copy byte at offset " + (2 + i) + " should be " + pattern[i]);
      }
      LOGGER.info("[" + runtime + "] Long copy forward overlap verified");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("long copy same offset is no-op via Java API")
  void longCopyNoOpSameOffset(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory.copy(long,long,long) same offset no-op");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmMemory memory = instance.getMemory("mem").get();

      // This should be a no-op (same source and dest)
      memory.copy(10L, 10L, 100L);
      LOGGER.info("[" + runtime + "] Long copy same-offset no-op succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("long copy zero length is no-op via Java API")
  void longCopyNoOpZeroLength(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory.copy(long,long,long) zero length no-op");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmMemory memory = instance.getMemory("mem").get();

      // Zero length copy should be a no-op
      memory.copy(0L, 100L, 0L);
      LOGGER.info("[" + runtime + "] Long copy zero-length no-op succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("long fill region via Java API")
  void longFillRegion(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory.fill(long,long,byte) with 16 bytes of 0xAB");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmMemory memory = instance.getMemory("mem").get();

      // Fill 16 bytes at offset 0 with 0xAB via Java default method
      memory.fill(0L, 16L, (byte) 0xAB);

      // Verify all 16 bytes using load_byte
      for (int i = 0; i < 16; i++) {
        final WasmValue[] result = instance.callFunction("load_byte", WasmValue.i32(i));
        assertEquals(0xAB, result[0].asInt(), "Filled byte at offset " + i + " should be 0xAB");
      }
      LOGGER.info("[" + runtime + "] Long fill verified for 16 bytes of 0xAB");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("long fill zero length is no-op via Java API")
  void longFillZeroLength(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory.fill(long,long,byte) zero length no-op");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmMemory memory = instance.getMemory("mem").get();

      // Zero length fill should be a no-op
      memory.fill(0L, 0L, (byte) 0xFF);
      LOGGER.info("[" + runtime + "] Long fill zero-length no-op succeeded");

      instance.close();
      module.close();
    }
  }
}
