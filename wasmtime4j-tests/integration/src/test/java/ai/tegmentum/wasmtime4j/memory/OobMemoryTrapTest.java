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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for out-of-bounds memory access traps. Verifies that i32.load/store beyond memory size
 * traps, boundary access succeeds, and memory.copy with OOB source/dest traps.
 *
 * @since 1.0.0
 */
@DisplayName("OOB Memory Trap Tests")
public class OobMemoryTrapTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(OobMemoryTrapTest.class.getName());

  /** Module with 1 page (65536 bytes) of memory and load/store functions. */
  private static final String MEMORY_OPS_WAT =
      """
      (module
        (memory (export "mem") 1 1)

        (func (export "load_i32") (param i32) (result i32)
          local.get 0
          i32.load)

        (func (export "store_i32") (param i32 i32)
          local.get 0
          local.get 1
          i32.store)

        (func (export "load_byte") (param i32) (result i32)
          local.get 0
          i32.load8_u)

        (func (export "store_byte") (param i32 i32)
          local.get 0
          local.get 1
          i32.store8)

        (func (export "copy") (param i32 i32 i32)
          local.get 0
          local.get 1
          local.get 2
          memory.copy)
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("i32.load at offset beyond memory size traps")
  void loadBeyondMemoryTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing i32.load beyond memory");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // 1 page = 65536 bytes, i32.load reads 4 bytes
      // Offset 65536 is beyond memory
      assertThrows(
          Exception.class,
          () -> instance.callFunction("load_i32", WasmValue.i32(65536)),
          "i32.load at 65536 (beyond 1 page) should trap");
      LOGGER.info("[" + runtime + "] i32.load beyond memory trapped as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("i32.store at offset beyond memory size traps")
  void storeBeyondMemoryTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing i32.store beyond memory");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      assertThrows(
          Exception.class,
          () -> instance.callFunction("store_i32", WasmValue.i32(65536), WasmValue.i32(42)),
          "i32.store at 65536 (beyond 1 page) should trap");
      LOGGER.info("[" + runtime + "] i32.store beyond memory trapped as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Load at exact boundary (last valid byte) succeeds")
  void loadAtExactBoundarySucceeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing load at exact boundary");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // Last valid i32.load: offset 65532 (reads bytes 65532-65535)
      assertDoesNotThrow(
          () -> instance.callFunction("load_i32", WasmValue.i32(65532)),
          "i32.load at 65532 (last valid 4-byte boundary) should succeed");
      LOGGER.info("[" + runtime + "] Load at last valid boundary succeeded");

      // Last valid byte load: offset 65535
      assertDoesNotThrow(
          () -> instance.callFunction("load_byte", WasmValue.i32(65535)),
          "i32.load8_u at 65535 (last valid byte) should succeed");
      LOGGER.info("[" + runtime + "] Byte load at last valid offset succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Load one byte past boundary traps")
  void loadOneBytePastBoundaryTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing load one byte past boundary");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // i32.load at 65533 reads bytes 65533-65536 (one byte past end)
      assertThrows(
          Exception.class,
          () -> instance.callFunction("load_i32", WasmValue.i32(65533)),
          "i32.load at 65533 (extends past boundary) should trap");
      LOGGER.info("[" + runtime + "] Load past boundary trapped as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Store one byte past boundary traps")
  void storeOneBytePastBoundaryTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing store one byte past boundary");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // i32.store at 65533 writes bytes 65533-65536 (one byte past end)
      assertThrows(
          Exception.class,
          () -> instance.callFunction("store_i32", WasmValue.i32(65533), WasmValue.i32(42)),
          "i32.store at 65533 (extends past boundary) should trap");
      LOGGER.info("[" + runtime + "] Store past boundary trapped as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.copy with out-of-bounds destination traps")
  void memoryCopyOobDestTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.copy with OOB destination");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // Copy 100 bytes from offset 0 to offset 65500 (extends to 65600, past 65536)
      assertThrows(
          Exception.class,
          () ->
              instance.callFunction(
                  "copy", WasmValue.i32(65500), WasmValue.i32(0), WasmValue.i32(100)),
          "memory.copy with OOB dest should trap");
      LOGGER.info("[" + runtime + "] memory.copy OOB dest trapped as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory.copy with out-of-bounds source traps")
  void memoryCopyOobSrcTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing memory.copy with OOB source");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(MEMORY_OPS_WAT);
      final Instance instance = module.instantiate(store);

      // Copy 100 bytes from offset 65500 (extends to 65600) to offset 0
      assertThrows(
          Exception.class,
          () ->
              instance.callFunction(
                  "copy", WasmValue.i32(0), WasmValue.i32(65500), WasmValue.i32(100)),
          "memory.copy with OOB source should trap");
      LOGGER.info("[" + runtime + "] memory.copy OOB source trapped as expected");

      instance.close();
      module.close();
    }
  }
}
