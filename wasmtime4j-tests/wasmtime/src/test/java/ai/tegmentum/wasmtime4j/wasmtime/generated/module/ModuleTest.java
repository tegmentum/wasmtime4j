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
package ai.tegmentum.wasmtime4j.wasmtime.generated.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime module.rs tests.
 *
 * <p>Original source: module.rs - Tests module operations.
 *
 * <p>This test validates that wasmtime4j module operations produce the same behavior as the
 * upstream Wasmtime implementation.
 */
public final class ModuleTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::basic_compilation")
  public void testBasicCompilation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module should be compiled");
      assertTrue(module.isValid(), "Module should be valid");
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_exports")
  public void testModuleExports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "func1"))
          (func (export "func2") (result i32) i32.const 0)
          (memory (export "mem") 1)
          (global (export "glob") i32 (i32.const 42))
          (table (export "tab") 10 funcref)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);

      final List<ExportType> exports = module.getExports();
      assertEquals(5, exports.size(), "Should have 5 exports");

      // Check export names
      assertTrue(module.hasExport("func1"), "Should have func1 export");
      assertTrue(module.hasExport("func2"), "Should have func2 export");
      assertTrue(module.hasExport("mem"), "Should have mem export");
      assertTrue(module.hasExport("glob"), "Should have glob export");
      assertTrue(module.hasExport("tab"), "Should have tab export");
      assertFalse(module.hasExport("nonexistent"), "Should not have nonexistent export");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_imports")
  public void testModuleImports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (import "env" "func" (func))
          (import "env" "mem" (memory 1))
          (import "env" "glob" (global i32))
          (import "env" "tab" (table 10 funcref))
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);

      final List<ImportType> imports = module.getImports();
      assertEquals(4, imports.size(), "Should have 4 imports");

      // Check import presence
      assertTrue(module.hasImport("env", "func"), "Should have env.func import");
      assertTrue(module.hasImport("env", "mem"), "Should have env.mem import");
      assertTrue(module.hasImport("env", "glob"), "Should have env.glob import");
      assertTrue(module.hasImport("env", "tab"), "Should have env.tab import");
      assertFalse(module.hasImport("env", "nonexistent"), "Should not have nonexistent import");
      assertFalse(module.hasImport("other", "func"), "Should not have other.func import");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::invalid_wat_fails")
  public void testInvalidWatFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String invalidWat = "(module (func (invalid syntax";

    try (final Engine engine = Engine.create()) {
      assertThrows(
          WasmException.class,
          () -> engine.compileWat(invalidWat),
          "Invalid WAT should fail compilation");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::instantiate_simple_module")
  public void testInstantiateSimpleModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "answer") (result i32)
            i32.const 42
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        assertNotNull(instance, "Instance should be created");
        assertTrue(instance.isValid(), "Instance should be valid");

        final WasmValue[] results = instance.callFunction("answer");
        assertEquals(42, results[0].asInt(), "Function should return 42");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_multiple_instantiation")
  public void testModuleMultipleInstantiation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "g") (mut i32) (i32.const 0))
          (func (export "inc")
            global.get 0
            i32.const 1
            i32.add
            global.set 0
          )
          (func (export "get") (result i32)
            global.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore()) {

        // Create two instances from the same module
        try (final Instance instance1 = module.instantiate(store);
            final Instance instance2 = module.instantiate(store)) {

          // Modify instance1
          instance1.callFunction("inc");
          instance1.callFunction("inc");

          // Modify instance2
          instance2.callFunction("inc");

          // Check that they have independent state
          final WasmValue[] results1 = instance1.callFunction("get");
          final WasmValue[] results2 = instance2.callFunction("get");

          assertEquals(2, results1[0].asInt(), "Instance1 should have 2");
          assertEquals(1, results2[0].asInt(), "Instance2 should have 1");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_serialization")
  public void testModuleSerialization(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "double") (param i32) (result i32)
            local.get 0
            i32.const 2
            i32.mul
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module originalModule = engine.compileWat(wat);

      // Serialize
      final byte[] serialized = originalModule.serialize();
      assertNotNull(serialized, "Serialized bytes should not be null");
      assertTrue(serialized.length > 0, "Serialized bytes should not be empty");

      // Deserialize
      final Module deserializedModule = Module.deserialize(engine, serialized);
      assertNotNull(deserializedModule, "Deserialized module should not be null");

      // Test the deserialized module works
      try (final Store store = engine.createStore();
          final Instance instance = deserializedModule.instantiate(store)) {

        final WasmValue[] results = instance.callFunction("double", WasmValue.i32(21));
        assertEquals(42, results[0].asInt(), "Deserialized module should work correctly");
      }

      deserializedModule.close();
      originalModule.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_function_types")
  public void testModuleFunctionTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "no_params_no_results"))
          (func (export "one_param") (param i32))
          (func (export "one_result") (result i32) i32.const 0)
          (func (export "both") (param i32 i64) (result f32) f32.const 0)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);

      // Check function types
      var funcType = module.getFunctionType("no_params_no_results");
      assertTrue(funcType.isPresent(), "Should find function type");
      assertEquals(0, funcType.get().getParams().size(), "Should have no params");
      assertEquals(0, funcType.get().getResults().size(), "Should have no results");

      funcType = module.getFunctionType("one_param");
      assertTrue(funcType.isPresent(), "Should find function type");
      assertEquals(1, funcType.get().getParams().size(), "Should have 1 param");
      assertEquals(0, funcType.get().getResults().size(), "Should have no results");

      funcType = module.getFunctionType("one_result");
      assertTrue(funcType.isPresent(), "Should find function type");
      assertEquals(0, funcType.get().getParams().size(), "Should have no params");
      assertEquals(1, funcType.get().getResults().size(), "Should have 1 result");

      funcType = module.getFunctionType("both");
      assertTrue(funcType.isPresent(), "Should find function type");
      assertEquals(2, funcType.get().getParams().size(), "Should have 2 params");
      assertEquals(1, funcType.get().getResults().size(), "Should have 1 result");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_memory_types")
  public void testModuleMemoryTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "mem") 1 10)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);

      final var memType = module.getMemoryType("mem");
      assertTrue(memType.isPresent(), "Should find memory type");
      assertEquals(1, memType.get().getMinimum(), "Minimum should be 1");
      assertTrue(memType.get().getMaximum().isPresent(), "Should have maximum");
      assertEquals(10, memType.get().getMaximum().get(), "Maximum should be 10");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_table_types")
  public void testModuleTableTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "tab") 5 100 funcref)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);

      final var tableType = module.getTableType("tab");
      assertTrue(tableType.isPresent(), "Should find table type");
      assertEquals(5, tableType.get().getMinimum(), "Minimum should be 5");
      assertTrue(tableType.get().getMaximum().isPresent(), "Should have maximum");
      assertEquals(100, tableType.get().getMaximum().get(), "Maximum should be 100");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_global_types")
  public void testModuleGlobalTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "immut") i32 (i32.const 42))
          (global (export "mut") (mut i64) (i64.const 0))
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);

      var globalType = module.getGlobalType("immut");
      assertTrue(globalType.isPresent(), "Should find global type");
      assertFalse(globalType.get().isMutable(), "Should be immutable");

      globalType = module.getGlobalType("mut");
      assertTrue(globalType.isPresent(), "Should find global type");
      assertTrue(globalType.get().isMutable(), "Should be mutable");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_with_start_function")
  public void testModuleWithStartFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (export "initialized") (mut i32) (i32.const 0))
          (func $start
            i32.const 42
            global.set 0
          )
          (start $start)
          (func (export "get") (result i32)
            global.get 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Start function should have run during instantiation
        final WasmValue[] results = instance.callFunction("get");
        assertEquals(42, results[0].asInt(), "Start function should have initialized global");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_data_segments")
  public void testModuleDataSegments(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "mem") 1)
          (data (i32.const 0) "Hello")
          (data (i32.const 100) "World")
          (func (export "load") (param i32) (result i32)
            local.get 0
            i32.load8_u
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Check first segment
        WasmValue[] results = instance.callFunction("load", WasmValue.i32(0));
        assertEquals('H', results[0].asInt(), "First byte should be 'H'");

        // Check second segment
        results = instance.callFunction("load", WasmValue.i32(100));
        assertEquals('W', results[0].asInt(), "Byte at 100 should be 'W'");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_element_segments")
  public void testModuleElementSegments(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "tab") 10 funcref)
          (func $f1 (result i32) i32.const 1)
          (func $f2 (result i32) i32.const 2)
          (func $f3 (result i32) i32.const 3)
          (elem (i32.const 0) $f1 $f2 $f3)
          (type $i32_func (func (result i32)))
          (func (export "call") (param i32) (result i32)
            local.get 0
            call_indirect (type $i32_func)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        WasmValue[] results = instance.callFunction("call", WasmValue.i32(0));
        assertEquals(1, results[0].asInt(), "f1 should return 1");

        results = instance.callFunction("call", WasmValue.i32(1));
        assertEquals(2, results[0].asInt(), "f2 should return 2");

        results = instance.callFunction("call", WasmValue.i32(2));
        assertEquals(3, results[0].asInt(), "f3 should return 3");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::module_validity_after_close")
  public void testModuleValidityAfterClose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (func (export "test") (result i32) i32.const 42)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      assertTrue(module.isValid(), "Module should be valid before close");

      module.close();
      assertFalse(module.isValid(), "Module should be invalid after close");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module::compile_large_module")
  public void testCompileLargeModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Generate a module with many functions
    final StringBuilder wat = new StringBuilder("(module\n");
    for (int i = 0; i < 100; i++) {
      wat.append("  (func (export \"f")
          .append(i)
          .append("\") (result i32) i32.const ")
          .append(i)
          .append(")\n");
    }
    wat.append(")");

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat.toString());
      assertTrue(module.isValid(), "Large module should compile");

      final List<ExportType> exports = module.getExports();
      assertEquals(100, exports.size(), "Should have 100 exports");

      // Test a few functions
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        WasmValue[] results = instance.callFunction("f0");
        assertEquals(0, results[0].asInt());

        results = instance.callFunction("f50");
        assertEquals(50, results[0].asInt());

        results = instance.callFunction("f99");
        assertEquals(99, results[0].asInt());
      }

      module.close();
    }
  }
}
