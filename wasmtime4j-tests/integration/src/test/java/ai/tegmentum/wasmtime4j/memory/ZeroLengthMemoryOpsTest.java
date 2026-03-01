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
 * Tests for zero-length memory operations. Per the WebAssembly spec, zero-length copy, fill, read,
 * and write should be no-ops and succeed even at out-of-bounds offsets.
 *
 * @since 1.0.0
 */
@DisplayName("Zero-Length Memory Operations Tests")
public class ZeroLengthMemoryOpsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ZeroLengthMemoryOpsTest.class.getName());

  /** Module with 1 page of memory and copy/fill functions. */
  private static final String MEMORY_OPS_WAT =
      """
      (module
        (memory (export "mem") 1 1)

        ;; memory.copy(dest, src, len)
        (func (export "copy") (param i32 i32 i32)
          local.get 0
          local.get 1
          local.get 2
          memory.copy)

        ;; memory.fill(dest, value, len)
        (func (export "fill") (param i32 i32 i32)
          local.get 0
          local.get 1
          local.get 2
          memory.fill)
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.copy with length 0 succeeds (no-op)")
  void memoryCopyZeroLength(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.copy with length 0");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // copy(dest=0, src=0, len=0) should be a no-op
      assertDoesNotThrow(
          () -> instance.callFunction("copy", WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(0)),
          "memory.copy with length 0 should succeed");
      LOGGER.info("[" + runtime + "] memory.copy length 0 succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.fill with length 0 succeeds (no-op)")
  void memoryFillZeroLength(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.fill with length 0");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // fill(dest=0, value=0xFF, len=0) should be a no-op
      assertDoesNotThrow(
          () ->
              instance.callFunction(
                  "fill", WasmValue.i32(0), WasmValue.i32(0xFF), WasmValue.i32(0)),
          "memory.fill with length 0 should succeed");
      LOGGER.info("[" + runtime + "] memory.fill length 0 succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Read 0 bytes from memory via API succeeds")
  void readZeroBytesViaApi(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing read 0 bytes via API");

    final String wat =
        """
        (module
          (memory (export "mem") 1)
          (func (export "noop")))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final Optional<WasmMemory> memOpt = instance.getMemory("mem");
      assert memOpt.isPresent() : "Memory export must be present";
      final WasmMemory memory = memOpt.get();

      // Read 0 bytes
      final byte[] dest = new byte[0];
      assertDoesNotThrow(() -> memory.readBytes(0, dest, 0, 0), "Reading 0 bytes should succeed");
      LOGGER.info("[" + runtime + "] Read 0 bytes via API succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Write 0 bytes to memory via API succeeds")
  void writeZeroBytesViaApi(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing write 0 bytes via API");

    final String wat =
        """
        (module
          (memory (export "mem") 1)
          (func (export "noop")))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final Optional<WasmMemory> memOpt = instance.getMemory("mem");
      assert memOpt.isPresent() : "Memory export must be present";
      final WasmMemory memory = memOpt.get();

      // Write 0 bytes
      final byte[] src = new byte[0];
      assertDoesNotThrow(() -> memory.writeBytes(0, src, 0, 0), "Writing 0 bytes should succeed");
      LOGGER.info("[" + runtime + "] Write 0 bytes via API succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Zero-length memory.copy at out-of-bounds offset succeeds per Wasm spec")
  void zeroCopyAtOobOffsetSucceeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing zero-length copy at OOB offset");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // Per Wasm spec, zero-length copies should succeed even at the memory boundary.
      // Offset 65536 = exactly at boundary (1 page = 65536 bytes).
      assertDoesNotThrow(
          () ->
              instance.callFunction(
                  "copy", WasmValue.i32(65536), WasmValue.i32(0), WasmValue.i32(0)),
          "Zero-length copy at boundary should succeed per Wasm spec");
      LOGGER.info("[" + runtime + "] Zero-length copy at boundary succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Zero-length memory.fill at out-of-bounds offset succeeds per Wasm spec")
  void zeroFillAtOobOffsetSucceeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing zero-length fill at OOB offset");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // Per Wasm spec, zero-length fills should succeed at the memory boundary.
      assertDoesNotThrow(
          () ->
              instance.callFunction(
                  "fill", WasmValue.i32(65536), WasmValue.i32(0), WasmValue.i32(0)),
          "Zero-length fill at boundary should succeed per Wasm spec");
      LOGGER.info("[" + runtime + "] Zero-length fill at boundary succeeded");

      instance.close();
      module.close();
    }
  }
}
