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
package ai.tegmentum.wasmtime4j.tests.linker;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/** Comprehensive tests for WebAssembly module linking. */
@SuppressWarnings("deprecation")
public class LinkerTest extends DualRuntimeTest {

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Link function between modules")
  public void testLinkFunctionBetweenModules(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      // Module 1: Exports a function
      final String wat1 =
          """
          (module
            (func (export "add") (param i32 i32) (result i32)
              local.get 0
              local.get 1
              i32.add
            )
          )
          """;

      // Module 2: Imports and uses the function
      final String wat2 =
          """
          (module
            (import "mod1" "add" (func $add (param i32 i32) (result i32)))
            (func (export "compute") (result i32)
              i32.const 10
              i32.const 32
              call $add
            )
          )
          """;

      final Module module1 = engine.compileWat(wat1);
      final Instance instance1 = module1.instantiate(store);

      final Linker<Void> linker = Linker.create(engine);
      linker.defineInstance(store, "mod1", instance1);

      final Module module2 = engine.compileWat(wat2);
      final Instance instance2 = linker.instantiate(store, module2);

      final WasmValue[] results = instance2.callFunction("compute");
      assertEquals(42, results[0].asInt());

      instance2.close();
      instance1.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Link memory between modules")
  public void testLinkMemoryBetweenModules(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      // Module 1: Exports memory
      final String wat1 =
          """
          (module
            (memory (export "shared_mem") 1)
            (func (export "write_value") (param i32 i32)
              local.get 0
              local.get 1
              i32.store
            )
          )
          """;

      // Module 2: Imports memory
      final String wat2 =
          """
          (module
            (import "mod1" "shared_mem" (memory 1))
            (func (export "read_value") (param i32) (result i32)
              local.get 0
              i32.load
            )
          )
          """;

      final Module module1 = engine.compileWat(wat1);
      final Instance instance1 = module1.instantiate(store);

      // Write to memory via module 1
      instance1.callFunction("write_value", WasmValue.i32(0), WasmValue.i32(42));

      final Linker<Void> linker = Linker.create(engine);
      final WasmMemory sharedMemory = instance1.getMemory("shared_mem").orElseThrow();
      linker.defineMemory(store, "mod1", "shared_mem", sharedMemory);

      final Module module2 = engine.compileWat(wat2);
      final Instance instance2 = linker.instantiate(store, module2);

      // Read from memory via module 2
      final WasmValue[] results = instance2.callFunction("read_value", WasmValue.i32(0));
      assertEquals(42, results[0].asInt());

      instance2.close();
      instance1.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Link global between modules")
  public void testLinkGlobalBetweenModules(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      // Module 1: Exports global
      final String wat1 =
          """
          (module
            (global (export "counter") (mut i32) (i32.const 0))
            (func (export "increment")
              global.get 0
              i32.const 1
              i32.add
              global.set 0
            )
          )
          """;

      // Module 2: Imports global
      final String wat2 =
          """
          (module
            (import "mod1" "counter" (global $counter (mut i32)))
            (func (export "get_counter") (result i32)
              global.get $counter
            )
            (func (export "double_counter")
              global.get $counter
              i32.const 2
              i32.mul
              global.set $counter
            )
          )
          """;

      final Module module1 = engine.compileWat(wat1);
      final Instance instance1 = module1.instantiate(store);

      // Increment via module 1
      instance1.callFunction("increment");

      final Linker<Void> linker = Linker.create(engine);
      final WasmGlobal sharedGlobal = instance1.getGlobal("counter").orElseThrow();
      linker.defineGlobal(store, "mod1", "counter", sharedGlobal);

      final Module module2 = engine.compileWat(wat2);
      final Instance instance2 = linker.instantiate(store, module2);

      // Check value via module 2
      WasmValue[] results = instance2.callFunction("get_counter");
      assertEquals(1, results[0].asInt());

      // Double via module 2
      instance2.callFunction("double_counter");

      // Verify via module 1
      instance1.callFunction("increment");
      assertEquals(3, sharedGlobal.get().asInt()); // Was 2, incremented to 3

      instance2.close();
      instance1.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Link table between modules")
  public void testLinkTableBetweenModules(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      // Module 1: Exports table with functions
      final String wat1 =
          """
          (module
            (table (export "funcs") 5 funcref)
            (func $f1 (result i32) i32.const 10)
            (func $f2 (result i32) i32.const 20)

            (elem declare func $f1 $f2)

            (func (export "init_table")
              i32.const 0
              ref.func $f1
              table.set 0

              i32.const 1
              ref.func $f2
              table.set 0
            )
          )
          """;

      // Module 2: Imports table
      final String wat2 =
          """
          (module
            (import "mod1" "funcs" (table 5 funcref))

            (func (export "call_from_table") (param i32) (result i32)
              local.get 0
              call_indirect (result i32)
            )
          )
          """;

      final Module module1 = engine.compileWat(wat1);
      final Instance instance1 = module1.instantiate(store);

      // Initialize table
      instance1.callFunction("init_table");

      final Linker<Void> linker = Linker.create(engine);
      final WasmTable sharedTable = instance1.getTable("funcs").orElseThrow();
      linker.defineTable(store, "mod1", "funcs", sharedTable);

      final Module module2 = engine.compileWat(wat2);
      final Instance instance2 = linker.instantiate(store, module2);

      // Call functions from shared table via module 2
      WasmValue[] results = instance2.callFunction("call_from_table", WasmValue.i32(0));
      assertEquals(10, results[0].asInt());

      results = instance2.callFunction("call_from_table", WasmValue.i32(1));
      assertEquals(20, results[0].asInt());

      instance2.close();
      instance1.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Chain linking - three modules")
  public void testChainLinking(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      // Module A: Base functionality
      final String watA =
          """
          (module
            (func (export "multiply") (param i32 i32) (result i32)
              local.get 0
              local.get 1
              i32.mul
            )
          )
          """;

      // Module B: Uses A
      final String watB =
          """
          (module
            (import "modA" "multiply" (func $multiply (param i32 i32) (result i32)))
            (func (export "square") (param i32) (result i32)
              local.get 0
              local.get 0
              call $multiply
            )
          )
          """;

      // Module C: Uses B
      final String watC =
          """
          (module
            (import "modB" "square" (func $square (param i32) (result i32)))
            (func (export "sum_of_squares") (param i32 i32) (result i32)
              local.get 0
              call $square
              local.get 1
              call $square
              i32.add
            )
          )
          """;

      final Module moduleA = engine.compileWat(watA);
      final Instance instanceA = moduleA.instantiate(store);

      final Linker<Void> linker = Linker.create(engine);
      linker.defineInstance(store, "modA", instanceA);

      final Module moduleB = engine.compileWat(watB);
      final Instance instanceB = linker.instantiate(store, moduleB);

      linker.defineInstance(store, "modB", instanceB);

      final Module moduleC = engine.compileWat(watC);
      final Instance instanceC = linker.instantiate(store, moduleC);

      // 3^2 + 4^2 = 9 + 16 = 25
      final WasmValue[] results =
          instanceC.callFunction("sum_of_squares", WasmValue.i32(3), WasmValue.i32(4));
      assertEquals(25, results[0].asInt());

      instanceC.close();
      instanceB.close();
      instanceA.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Mix host and module exports")
  public void testMixHostAndModuleExports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      // Host function
      final HostFunction logFunction =
          HostFunction.singleValue(
              params -> {
                // Just return the value for testing
                return WasmValue.i32(params[0].asInt() * 2);
              });

      final FunctionType logType =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      // WASM module that exports a function
      final String wat1 =
          """
          (module
            (func (export "add") (param i32 i32) (result i32)
              local.get 0
              local.get 1
              i32.add
            )
          )
          """;

      // Module using both host function and WASM function
      final String wat2 =
          """
          (module
            (import "env" "log" (func $log (param i32) (result i32)))
            (import "mod1" "add" (func $add (param i32 i32) (result i32)))

            (func (export "compute") (result i32)
              i32.const 10
              i32.const 5
              call $add
              call $log
            )
          )
          """;

      final Module module1 = engine.compileWat(wat1);
      final Instance instance1 = module1.instantiate(store);

      final Linker<Void> linker = Linker.create(engine);
      linker.defineHostFunction("env", "log", logType, logFunction);
      linker.defineInstance(store, "mod1", instance1);

      final Module module2 = engine.compileWat(wat2);
      final Instance instance2 = linker.instantiate(store, module2);

      // (10 + 5) * 2 = 30
      final WasmValue[] results = instance2.callFunction("compute");
      assertEquals(30, results[0].asInt());

      instance2.close();
      instance1.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Linker alias functionality")
  public void testLinkerAlias(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat1 =
          """
          (module
            (func (export "original") (result i32)
              i32.const 42
            )
          )
          """;

      final String wat2 =
          """
          (module
            (import "mod1" "aliased" (func $f (result i32)))
            (func (export "test") (result i32)
              call $f
            )
          )
          """;

      final Module module1 = engine.compileWat(wat1);
      final Instance instance1 = module1.instantiate(store);

      final Linker<Void> linker = Linker.create(engine);
      linker.defineInstance(store, "mod1", instance1);

      // Create alias: mod1.original -> mod1.aliased
      linker.alias("mod1", "original", "mod1", "aliased");

      final Module module2 = engine.compileWat(wat2);
      final Instance instance2 = linker.instantiate(store, module2);

      final WasmValue[] results = instance2.callFunction("test");
      assertEquals(42, results[0].asInt());

      instance2.close();
      instance1.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Check if import is defined")
  public void testHasImport(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final Linker<Void> linker = Linker.create(engine);

      // Initially no imports defined
      assertFalse(linker.hasImport("env", "test_func"));

      // Define a host function
      final HostFunction testFunc = HostFunction.singleValue(params -> WasmValue.i32(42));
      final FunctionType funcType =
          FunctionType.of(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction("env", "test_func", funcType, testFunc);

      // Now it should be defined
      assertTrue(linker.hasImport("env", "test_func"));

      // Different name should not be defined
      assertFalse(linker.hasImport("env", "other_func"));

      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple instances from same module")
  public void testMultipleInstancesFromSameModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (import "env" "value" (global $val (mut i32)))
            (func (export "get") (result i32)
              global.get $val
            )
            (func (export "increment")
              global.get $val
              i32.const 1
              i32.add
              global.set $val
            )
          )
          """;

      final Module module = engine.compileWat(wat);

      // Create two different globals
      final WasmGlobal global1 = store.createMutableGlobal(WasmValueType.I32, WasmValue.i32(10));
      final WasmGlobal global2 = store.createMutableGlobal(WasmValueType.I32, WasmValue.i32(20));

      // Create two instances with different globals
      final Linker<Void> linker1 = Linker.create(engine);
      linker1.defineGlobal(store, "env", "value", global1);
      final Instance instance1 = linker1.instantiate(store, module);

      final Linker<Void> linker2 = Linker.create(engine);
      linker2.defineGlobal(store, "env", "value", global2);
      final Instance instance2 = linker2.instantiate(store, module);

      // Instances should have independent state
      WasmValue[] results = instance1.callFunction("get");
      assertEquals(10, results[0].asInt());

      results = instance2.callFunction("get");
      assertEquals(20, results[0].asInt());

      // Increment instance1
      instance1.callFunction("increment");

      results = instance1.callFunction("get");
      assertEquals(11, results[0].asInt());

      // Instance2 should be unchanged
      results = instance2.callFunction("get");
      assertEquals(20, results[0].asInt());

      instance2.close();
      instance1.close();
      linker2.close();
      linker1.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Linker reuse across instantiations")
  public void testLinkerReuse(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final HostFunction addFunc =
          HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() + params[1].asInt()));

      final FunctionType funcType =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final Linker<Void> linker = Linker.create(engine);
      linker.defineHostFunction("env", "add", funcType, addFunc);

      final String wat =
          """
          (module
            (import "env" "add" (func $add (param i32 i32) (result i32)))
            (func (export "test") (param i32 i32) (result i32)
              local.get 0
              local.get 1
              call $add
            )
          )
          """;

      final Module module = engine.compileWat(wat);

      // Use same linker for multiple instantiations
      final Instance instance1 = linker.instantiate(store, module);
      final Instance instance2 = linker.instantiate(store, module);

      WasmValue[] results = instance1.callFunction("test", WasmValue.i32(10), WasmValue.i32(5));
      assertEquals(15, results[0].asInt());

      results = instance2.callFunction("test", WasmValue.i32(20), WasmValue.i32(3));
      assertEquals(23, results[0].asInt());

      instance2.close();
      instance1.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Missing import fails instantiation")
  public void testMissingImportFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (import "env" "missing_func" (func $f (result i32)))
            (func (export "test") (result i32)
              call $f
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Linker<Void> linker = Linker.create(engine);

      // Should fail - missing import
      assertThrows(
          Exception.class,
          () -> {
            linker.instantiate(store, module);
          });

      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Named module instantiation")
  public void testNamedModuleInstantiation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat1 =
          """
          (module
            (func (export "get_value") (result i32)
              i32.const 42
            )
          )
          """;

      final String wat2 =
          """
          (module
            (import "module_a" "get_value" (func $get (result i32)))
            (func (export "test") (result i32)
              call $get
            )
          )
          """;

      final Module module1 = engine.compileWat(wat1);
      final Module module2 = engine.compileWat(wat2);

      final Linker<Void> linker = Linker.create(engine);

      // Instantiate with name
      final Instance instance1 = linker.instantiate(store, "module_a", module1);

      // Second module can now import from "module_a"
      final Instance instance2 = linker.instantiate(store, module2);

      final WasmValue[] results = instance2.callFunction("test");
      assertEquals(42, results[0].asInt());

      instance2.close();
      instance1.close();
      linker.close();
    } finally {
      store.close();
      engine.close();
    }
  }
}
