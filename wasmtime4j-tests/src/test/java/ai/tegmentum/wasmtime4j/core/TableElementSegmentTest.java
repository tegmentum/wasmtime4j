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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Tests bulk-memory table operations: table.init and elem.drop via WASM exported functions. These
 * are part of the bulk-memory-operations proposal, enabled by default in Wasmtime 41.0.1.
 *
 * <p>The module uses a passive element segment containing function references that can be copied
 * into a funcref table via table.init and invalidated via elem.drop.
 *
 * @since 1.0.0
 */
@DisplayName("Table Element Segment (table.init / elem.drop) Tests")
public class TableElementSegmentTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(TableElementSegmentTest.class.getName());

  /**
   * WAT module with a funcref table (size 3), three functions returning 10/20/30, and a passive
   * element segment. Exports table.init, elem.drop, and call_indirect wrappers.
   */
  private static final String WAT =
      """
      (module
        (type $ret_i32 (func (result i32)))
        (table (export "t") 3 funcref)
        (func $f10 (result i32) i32.const 10)
        (func $f20 (result i32) i32.const 20)
        (func $f30 (result i32) i32.const 30)
        (elem $seg func $f10 $f20 $f30)
        (func (export "table_init") (param i32 i32 i32)
          local.get 0 local.get 1 local.get 2
          table.init $seg)
        (func (export "elem_drop")
          elem.drop $seg)
        (func (export "call_indirect") (param i32) (result i32)
          local.get 0
          call_indirect (type $ret_i32)))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.init copies all elements and call_indirect verifies")
  void initCopiesElementsToTable(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table.init copies all elements");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // table_init(dest=0, src=0, len=3) -- copy all 3 function refs
      instance.callFunction("table_init",
          WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(3));

      final WasmValue[] r0 = instance.callFunction("call_indirect", WasmValue.i32(0));
      final WasmValue[] r1 = instance.callFunction("call_indirect", WasmValue.i32(1));
      final WasmValue[] r2 = instance.callFunction("call_indirect", WasmValue.i32(2));

      assertNotNull(r0, "Result 0 should not be null");
      assertNotNull(r1, "Result 1 should not be null");
      assertNotNull(r2, "Result 2 should not be null");
      assertEquals(10, r0[0].asInt(), "Table[0] should call f10 returning 10");
      assertEquals(20, r1[0].asInt(), "Table[1] should call f20 returning 20");
      assertEquals(30, r2[0].asInt(), "Table[2] should call f30 returning 30");
      LOGGER.info("[" + runtime + "] table.init results: "
          + r0[0].asInt() + ", " + r1[0].asInt() + ", " + r2[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.init copies partial elements with offset")
  void initCopiesPartialElements(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table.init with partial elements");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // table_init(dest=1, src=1, len=2) -- copy f20,f30 into table[1],table[2]
      instance.callFunction("table_init",
          WasmValue.i32(1), WasmValue.i32(1), WasmValue.i32(2));

      final WasmValue[] r1 = instance.callFunction("call_indirect", WasmValue.i32(1));
      final WasmValue[] r2 = instance.callFunction("call_indirect", WasmValue.i32(2));

      assertEquals(20, r1[0].asInt(), "Table[1] should call f20 returning 20");
      assertEquals(30, r2[0].asInt(), "Table[2] should call f30 returning 30");
      LOGGER.info("[" + runtime + "] Partial table.init results: "
          + r1[0].asInt() + ", " + r2[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("elem.drop then table.init traps")
  void dropThenInitTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing elem.drop then table.init traps");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Drop element segment
      instance.callFunction("elem_drop");
      LOGGER.info("[" + runtime + "] Element segment dropped");

      // Attempting to init dropped element segment should trap
      assertThrows(Exception.class,
          () -> instance.callFunction("table_init",
              WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(3)),
          "table.init on dropped element segment should trap");
      LOGGER.info("[" + runtime + "] table.init on dropped segment trapped as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.init with zero length does not error")
  void initZeroLength(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table.init with zero length");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // table_init(dest=0, src=0, len=0) -- zero-length init should succeed
      assertDoesNotThrow(
          () -> instance.callFunction("table_init",
              WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(0)),
          "Zero-length table.init should not trap");
      LOGGER.info("[" + runtime + "] Zero-length table.init succeeded");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.init out of bounds traps")
  void initOutOfBoundsTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table.init out of bounds");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // table_init(dest=0, src=0, len=100) -- segment only has 3 elements
      assertThrows(Exception.class,
          () -> instance.callFunction("table_init",
              WasmValue.i32(0), WasmValue.i32(0), WasmValue.i32(100)),
          "table.init with length exceeding segment should trap");
      LOGGER.info("[" + runtime + "] Out-of-bounds table.init trapped as expected");

      instance.close();
      module.close();
    }
  }
}
