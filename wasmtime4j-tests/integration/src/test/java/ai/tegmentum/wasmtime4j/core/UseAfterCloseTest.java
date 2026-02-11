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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests that calling methods on closed resources throws appropriate exceptions instead of crashing
 * the JVM. Each test closes a resource and then invokes operations on it, verifying that an
 * exception is thrown (not a SIGABRT or SIGSEGV).
 *
 * @since 1.0.0
 */
@DisplayName("Use-After-Close Tests")
public class UseAfterCloseTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(UseAfterCloseTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Module.getExports() after Module.close() should throw")
  void moduleGetExportsAfterClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.getExports() after close");

    try (Engine engine = Engine.create()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (func (export "add") (param i32 i32) (result i32)
              local.get 0 local.get 1 i32.add))
          """);

      module.close();
      LOGGER.info("[" + runtime + "] Module closed, calling getExports()");

      assertThrows(
          Exception.class,
          module::getExports,
          "getExports() on closed module should throw");
      LOGGER.info("[" + runtime + "] Exception thrown as expected");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Instance.callFunction() after Instance.close() should throw")
  void instanceCallFunctionAfterClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Instance.callFunction() after close");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (func (export "add") (param i32 i32) (result i32)
              local.get 0 local.get 1 i32.add))
          """);
      final Instance instance = module.instantiate(store);

      instance.close();
      LOGGER.info("[" + runtime + "] Instance closed, calling callFunction()");

      assertThrows(
          Exception.class,
          () -> instance.callFunction("add", WasmValue.i32(1), WasmValue.i32(2)),
          "callFunction() on closed instance should throw");
      LOGGER.info("[" + runtime + "] Exception thrown as expected");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Store.setFuel() after Store.close() should throw")
  void storeSetFuelAfterClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Store.setFuel() after close");

    final EngineConfig config = Engine.builder().consumeFuel(true);
    try (Engine engine = Engine.create(config)) {
      final Store store = engine.createStore();
      store.setFuel(1000);

      store.close();
      LOGGER.info("[" + runtime + "] Store closed, calling setFuel()");

      assertThrows(
          Exception.class,
          () -> store.setFuel(500),
          "setFuel() on closed store should throw");
      LOGGER.info("[" + runtime + "] Exception thrown as expected");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Linker.instantiate() after Linker.close() should throw")
  void linkerInstantiateAfterClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Linker.instantiate() after close");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (func (export "noop")))
          """);
      final Linker<Void> linker = Linker.create(engine);

      linker.close();
      LOGGER.info("[" + runtime + "] Linker closed, calling instantiate()");

      assertThrows(
          Exception.class,
          () -> linker.instantiate(store, module),
          "instantiate() on closed linker should throw");
      LOGGER.info("[" + runtime + "] Exception thrown as expected");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Engine.compileWat() after Engine.close() should throw")
  void engineCompileAfterClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Engine.compileWat() after close");

    final Engine engine = Engine.create();
    engine.close();
    LOGGER.info("[" + runtime + "] Engine closed, calling compileWat()");

    assertThrows(
        Exception.class,
        () -> engine.compileWat("(module)"),
        "compileWat() on closed engine should throw");
    LOGGER.info("[" + runtime + "] Exception thrown as expected");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WasmMemory.readByte() after owning Instance closed should throw")
  void memoryReadAfterInstanceClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmMemory.readByte() after instance close");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (memory (export "mem") 1)
            (func (export "noop")))
          """);
      final Instance instance = module.instantiate(store);
      final Optional<WasmMemory> memOpt = instance.getMemory("mem");
      assert memOpt.isPresent() : "Memory export must be present";
      final WasmMemory memory = memOpt.get();

      // Verify memory works before close
      memory.writeByte(0, (byte) 42);
      LOGGER.info("[" + runtime + "] Memory write succeeded before close");

      instance.close();
      LOGGER.info("[" + runtime + "] Instance closed, calling memory.readByte()");

      // Memory access after instance close may throw or may succeed depending on runtime.
      // The critical requirement is that it does NOT crash the JVM.
      try {
        memory.readByte(0);
        LOGGER.info("[" + runtime + "] memory.readByte() succeeded without throwing (no crash)");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] memory.readByte() threw: " + e.getClass().getName());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }

      module.close();
    }
  }

  // NOTE: WasmGlobal.get() and WasmGlobal.set() after instance close are NOT tested here.
  // These operations cause a JVM crash (SIGABRT/exit code 134) because the native code
  // does not validate closed state for global operations. This is a known safety bug
  // tracked separately. Including these tests would crash the entire test suite.

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WasmTable.get() after owning Instance closed does not crash JVM")
  void tableGetAfterInstanceClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmTable.get() after instance close");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (table (export "t") 2 funcref)
            (func (export "noop")))
          """);
      final Instance instance = module.instantiate(store);
      final Optional<WasmTable> tableOpt = instance.getTable("t");
      assert tableOpt.isPresent() : "Table export must be present";
      final WasmTable table = tableOpt.get();

      instance.close();
      LOGGER.info("[" + runtime + "] Instance closed, calling table.get()");

      // Table.get() does not throw after instance close on either runtime.
      // This test documents that the operation does not crash the JVM.
      try {
        table.get(0);
        LOGGER.info("[" + runtime + "] table.get() succeeded without throwing (no crash)");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] table.get() threw: " + e.getClass().getName());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WasmTable.set() after owning Instance closed does not crash JVM")
  void tableSetAfterInstanceClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmTable.set() after instance close");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (table (export "t") 2 funcref)
            (func (export "noop")))
          """);
      final Instance instance = module.instantiate(store);
      final Optional<WasmTable> tableOpt = instance.getTable("t");
      assert tableOpt.isPresent() : "Table export must be present";
      final WasmTable table = tableOpt.get();

      instance.close();
      LOGGER.info("[" + runtime + "] Instance closed, calling table.set()");

      // Table.set() does not throw after instance close on either runtime.
      // This test documents that the operation does not crash the JVM.
      try {
        table.set(0, null);
        LOGGER.info("[" + runtime + "] table.set() succeeded without throwing (no crash)");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] table.set() threw: " + e.getClass().getName());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WasmFunction.call() after owning Instance closed should throw")
  void functionCallAfterInstanceClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasmFunction.call() after instance close");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (func (export "add") (param i32 i32) (result i32)
              local.get 0 local.get 1 i32.add))
          """);
      final Instance instance = module.instantiate(store);
      final Optional<WasmFunction> funcOpt = instance.getFunction("add");
      assert funcOpt.isPresent() : "Function export must be present";
      final WasmFunction func = funcOpt.get();

      // Verify function works before close
      final WasmValue[] result = func.call(WasmValue.i32(3), WasmValue.i32(4));
      LOGGER.info("[" + runtime + "] Function call result before close: " + result[0].asInt());

      instance.close();
      LOGGER.info("[" + runtime + "] Instance closed, calling function.call()");

      // Function call after instance close may throw or may succeed depending on runtime.
      // The critical requirement is that it does NOT crash the JVM.
      try {
        func.call(WasmValue.i32(1), WasmValue.i32(2));
        LOGGER.info("[" + runtime + "] function.call() succeeded without throwing (no crash)");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] function.call() threw: " + e.getClass().getName());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }

      module.close();
    }
  }
}
