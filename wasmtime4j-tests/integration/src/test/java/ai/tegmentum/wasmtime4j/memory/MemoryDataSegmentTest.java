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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * Tests bulk-memory operations: memory.init and data.drop via WASM exported functions. These are
 * part of the bulk-memory-operations proposal, enabled by default in Wasmtime 41.0.1.
 *
 * <p>The module uses passive data segments that can be copied into memory via memory.init and
 * invalidated via data.drop.
 *
 * @since 1.0.0
 */
@DisplayName("Memory Data Segment (memory.init / data.drop) Tests")
public class MemoryDataSegmentTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryDataSegmentTest.class.getName());

  /**
   * WAT module with two passive data segments and exported functions to init/drop them. Segment 0:
   * "Hello" (5 bytes). Segment 1: "World12345" (10 bytes). Memory is exported for reading results.
   */
  private static final String WAT =
      """
      (module
        (memory (export "mem") 1)
        (data $seg0 "Hello")
        (data $seg1 "World12345")
        (func (export "init_seg0") (param i32 i32 i32)
          local.get 0 local.get 1 local.get 2
          memory.init $seg0)
        (func (export "drop_seg0")
          data.drop $seg0)
        (func (export "init_seg1") (param i32 i32 i32)
          local.get 0 local.get 1 local.get 2
          memory.init $seg1)
        (func (export "drop_seg1")
          data.drop $seg1))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.init copies full data segment to memory")
  void initCopiesDataSegmentToMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.init copies full data segment");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmMemory> memOpt = instance.getMemory("mem");
      assert memOpt.isPresent() : "Memory export must be present";
      final WasmMemory memory = memOpt.get();

      // init_seg0(dest=0, src=0, len=5) -- copy all 5 bytes of "Hello" to offset 0
      instance.callFunction("init_seg0", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(5));

      final byte[] result = new byte[5];
      for (int i = 0; i < 5; i++) {
        result[i] = memory.readByte(i);
      }
      final String text = new String(result, java.nio.charset.StandardCharsets.UTF_8);
      assertEquals("Hello", text, "Memory should contain 'Hello'");
      LOGGER.info("[" + runtime + "] memory.init copied: '" + text + "'");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.init copies partial data segment with offset")
  void initCopiesPartialDataSegment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.init with partial segment");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmMemory> memOpt = instance.getMemory("mem");
      assert memOpt.isPresent() : "Memory export must be present";
      final WasmMemory memory = memOpt.get();

      // init_seg0(dest=0, src=2, len=3) -- copy "llo" (3 bytes starting at offset 2 in segment)
      instance.callFunction("init_seg0", WasmValue.i32(0), WasmValue.i32(2), WasmValue.i32(3));

      final byte[] result = new byte[3];
      for (int i = 0; i < 3; i++) {
        result[i] = memory.readByte(i);
      }
      final String text = new String(result, java.nio.charset.StandardCharsets.UTF_8);
      assertEquals("llo", text, "Memory should contain 'llo'");
      LOGGER.info("[" + runtime + "] memory.init partial copy: '" + text + "'");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.init copies second data segment")
  void initSecondDataSegment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.init with second segment");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmMemory> memOpt = instance.getMemory("mem");
      assert memOpt.isPresent() : "Memory export must be present";
      final WasmMemory memory = memOpt.get();

      // init_seg1(dest=0, src=0, len=10) -- copy all 10 bytes of "World12345"
      instance.callFunction("init_seg1", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(10));

      final byte[] result = new byte[10];
      for (int i = 0; i < 10; i++) {
        result[i] = memory.readByte(i);
      }
      final String text = new String(result, java.nio.charset.StandardCharsets.UTF_8);
      assertEquals("World12345", text, "Memory should contain 'World12345'");
      LOGGER.info("[" + runtime + "] memory.init second segment: '" + text + "'");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("data.drop then memory.init traps")
  void dropThenInitTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing data.drop then memory.init traps");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Drop segment 0
      instance.callFunction("drop_seg0");
      LOGGER.info("[" + runtime + "] Segment 0 dropped");

      // Attempting to init dropped segment should trap
      assertThrows(
          Exception.class,
          () ->
              instance.callFunction(
                  "init_seg0", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(5)),
          "memory.init on dropped segment should trap");
      LOGGER.info("[" + runtime + "] memory.init on dropped segment trapped as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.init with zero length does not error")
  void initBoundaryZeroLength(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.init with zero length");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // init_seg0(dest=0, src=0, len=0) -- zero-length init should succeed
      assertDoesNotThrow(
          () ->
              instance.callFunction(
                  "init_seg0", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(0)),
          "Zero-length memory.init should not trap");
      LOGGER.info("[" + runtime + "] Zero-length memory.init succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.init out of bounds traps")
  void initOutOfBoundsTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.init out of bounds");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // init_seg0(dest=0, src=0, len=100) -- segment is only 5 bytes, 100 exceeds bounds
      assertThrows(
          Exception.class,
          () ->
              instance.callFunction(
                  "init_seg0", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(100)),
          "memory.init with length exceeding segment should trap");
      LOGGER.info("[" + runtime + "] Out-of-bounds memory.init trapped as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WasmMemory.init() Java API if supported")
  void javaApiInitIfSupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory.init() Java API");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmMemory> memOpt = instance.getMemory("mem");
      assert memOpt.isPresent() : "Memory export must be present";
      final WasmMemory memory = memOpt.get();

      // Try the Java-level WasmMemory.init() API
      try {
        memory.init(0, 0, 0, 5);
        LOGGER.info("[" + runtime + "] WasmMemory.init() succeeded");

        final byte[] result = new byte[5];
        for (int i = 0; i < 5; i++) {
          result[i] = memory.readByte(i);
        }
        final String text = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        LOGGER.info("[" + runtime + "] WasmMemory.init() wrote: '" + text + "'");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] WasmMemory.init() not supported: " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] WasmMemory.init() threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }
}
