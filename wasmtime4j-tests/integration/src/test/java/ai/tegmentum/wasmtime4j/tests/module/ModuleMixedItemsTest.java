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
package ai.tegmentum.wasmtime4j.tests.module;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests mixed import and export scenarios in WebAssembly modules.
 *
 * <p>This test validates that wasmtime4j correctly handles modules that both import and export
 * items (functions, globals, memory, tables), matching Wasmtime's behavior.
 *
 * <p>Expected Wasmtime behavior:
 *
 * <ul>
 *   <li>A module can both import items from host and export items to host
 *   <li>Imported and exported items can interact within the module
 *   <li>Host can access exported items that depend on imported items
 *   <li>Complex dependency chains between imports and exports work correctly
 * </ul>
 *
 * <p>Reference: <a
 * href="https://docs.wasmtime.dev/api/wasmtime/struct.Instance.html">https://docs.wasmtime.dev/api/wasmtime/struct.Instance.html</a>
 *
 * <p>This test runs on both JNI and Panama runtimes to ensure both correctly implement Wasmtime's
 * behavior.
 */
public class ModuleMixedItemsTest extends DualRuntimeTest {
  private Engine engine;
  private Store store;

  @AfterEach
  void cleanupRuntime() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    clearRuntimeSelection();
  }

  private void setupRuntime() throws Exception {
    engine = Engine.create();
    store = engine.createStore();
  }

  /**
   * Tests importing and exporting mixed items in a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Import and export mixed items")
  public void testImportAndExportMixedItems(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final Linker<Void> linker = Linker.create(engine);
    final WasmGlobal importedGlobal =
        store.createMutableGlobal(WasmValueType.I32, WasmValue.i32(10));
    linker.defineGlobal(store, "env", "base", importedGlobal);

    final HostFunction mulFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * params[1].asInt()));
    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});
    linker.defineHostFunction("env", "multiply", funcType, mulFunc);

    final String wat =
        """
        (module
          (import "env" "base" (global $base (mut i32)))
          (import "env" "multiply" (func $multiply (param i32 i32) (result i32)))
          (memory (export "mem") 1)
          (global $result (export "result") (mut i32) (i32.const 0))
          (func (export "compute") (param i32) (result i32)
            global.get $base
            local.get 0
            call $multiply
            global.set $result
            global.get $result
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    assertTrue(instance.getMemory("mem").isPresent());
    assertTrue(instance.getGlobal("result").isPresent());

    final WasmValue[] results = instance.callFunction("compute", WasmValue.i32(4));
    assertEquals(40, results[0].asInt());

    final WasmGlobal resultGlobal = instance.getGlobal("result").orElseThrow();
    assertEquals(40, resultGlobal.get().asInt());

    instance.close();
    linker.close();
  }

  /**
   * Tests re-exporting an imported function from a WebAssembly module.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the test fails
   */
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Re-export imported function")
  public void testReExportImportedFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    setupRuntime();

    final Linker<Void> linker = Linker.create(engine);
    final HostFunction doubleFunc =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * 2));
    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
    linker.defineHostFunction("env", "original", funcType, doubleFunc);

    final String wat =
        """
        (module
          (import "env" "original" (func $original (param i32) (result i32)))
          (export "reexported" (func $original))
          (func (export "wrapped") (param i32) (result i32)
            local.get 0
            call $original
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = linker.instantiate(store, module);

    WasmValue[] results = instance.callFunction("reexported", WasmValue.i32(21));
    assertEquals(42, results[0].asInt());

    results = instance.callFunction("wrapped", WasmValue.i32(21));
    assertEquals(42, results[0].asInt());

    instance.close();
    linker.close();
  }
}
