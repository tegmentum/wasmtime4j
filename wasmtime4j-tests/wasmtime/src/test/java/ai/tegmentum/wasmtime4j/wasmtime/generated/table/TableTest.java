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
package ai.tegmentum.wasmtime4j.wasmtime.generated.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime table.rs tests.
 *
 * <p>Original source: table.rs - Tests table operations.
 *
 * <p>This test validates that wasmtime4j table operations produce the same behavior as the upstream
 * Wasmtime implementation.
 */
public final class TableTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::basic_table_operations")
  public void testBasicTableOperations(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "table") 10 funcref)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Table should be exported");

        final WasmTable table = tableOpt.get();
        assertEquals(10, table.getSize(), "Table should have 10 elements");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_get_set")
  public void testTableGetSet(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "table") 10 funcref)
          (func $f1 (result i32) i32.const 42)
          (func $f2 (result i32) i32.const 13)
          (elem (i32.const 0) $f1)
          (elem (i32.const 1) $f2)
          (type $i32_func (func (result i32)))
          (func (export "call_at") (param i32) (result i32)
            local.get 0
            call_indirect (type $i32_func)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Call function at index 0
        WasmValue[] results = instance.callFunction("call_at", WasmValue.i32(0));
        assertEquals(42, results[0].asInt(), "Function at 0 should return 42");

        // Call function at index 1
        results = instance.callFunction("call_at", WasmValue.i32(1));
        assertEquals(13, results[0].asInt(), "Function at 1 should return 13");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_grow")
  public void testTableGrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "table") 5 10 funcref)
          (func (export "size") (result i32)
            table.size
          )
          (func (export "grow") (param i32) (result i32)
            ref.null func
            local.get 0
            table.grow
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Check initial size
        WasmValue[] results = instance.callFunction("size");
        assertEquals(5, results[0].asInt(), "Initial size should be 5");

        // Grow by 3
        results = instance.callFunction("grow", WasmValue.i32(3));
        assertEquals(5, results[0].asInt(), "Previous size should be 5");

        // Check new size
        results = instance.callFunction("size");
        assertEquals(8, results[0].asInt(), "New size should be 8");

        // Try to grow past max
        results = instance.callFunction("grow", WasmValue.i32(5));
        assertEquals(-1, results[0].asInt(), "Growing past max should return -1");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_fill")
  public void testTableFill(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "table") 10 funcref)
          (func $f (result i32) i32.const 99)
          (elem declare func $f)  ;; Required for ref.func with function-references proposal
          (func (export "fill")
            i32.const 2        ;; start index
            ref.func $f        ;; value
            i32.const 5        ;; count
            table.fill
          )
          (type $i32_func (func (result i32)))
          (func (export "call_at") (param i32) (result i32)
            local.get 0
            call_indirect (type $i32_func)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Fill table
        instance.callFunction("fill");

        // Call functions in filled range
        for (int i = 2; i < 7; i++) {
          final WasmValue[] results = instance.callFunction("call_at", WasmValue.i32(i));
          assertEquals(99, results[0].asInt(), "Function at " + i + " should return 99");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_copy")
  public void testTableCopy(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "table") 10 funcref)
          (func $f1 (result i32) i32.const 1)
          (func $f2 (result i32) i32.const 2)
          (func $f3 (result i32) i32.const 3)
          (elem (i32.const 0) $f1 $f2 $f3)
          (func (export "copy")
            i32.const 5        ;; dest
            i32.const 0        ;; src
            i32.const 3        ;; count
            table.copy
          )
          (type $i32_func (func (result i32)))
          (func (export "call_at") (param i32) (result i32)
            local.get 0
            call_indirect (type $i32_func)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Copy elements
        instance.callFunction("copy");

        // Verify original elements
        WasmValue[] results = instance.callFunction("call_at", WasmValue.i32(0));
        assertEquals(1, results[0].asInt(), "Original at 0 should return 1");

        results = instance.callFunction("call_at", WasmValue.i32(1));
        assertEquals(2, results[0].asInt(), "Original at 1 should return 2");

        // Verify copied elements
        results = instance.callFunction("call_at", WasmValue.i32(5));
        assertEquals(1, results[0].asInt(), "Copied at 5 should return 1");

        results = instance.callFunction("call_at", WasmValue.i32(6));
        assertEquals(2, results[0].asInt(), "Copied at 6 should return 2");

        results = instance.callFunction("call_at", WasmValue.i32(7));
        assertEquals(3, results[0].asInt(), "Copied at 7 should return 3");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_out_of_bounds")
  public void testTableOutOfBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table 5 funcref)
          (func $f (result i32) i32.const 42)
          (elem (i32.const 0) $f)
          (type $i32_func (func (result i32)))
          (func (export "call_at") (param i32) (result i32)
            local.get 0
            call_indirect (type $i32_func)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Valid access
        WasmValue[] results = instance.callFunction("call_at", WasmValue.i32(0));
        assertEquals(42, results[0].asInt(), "Valid access should work");

        // Out of bounds access
        try {
          instance.callFunction("call_at", WasmValue.i32(10));
          fail("Expected trap for out-of-bounds table access");
        } catch (final Exception e) {
          // Expected - out of bounds
          assertNotNull(e.getMessage(), "Out-of-bounds trap should have a message");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_null_funcref_trap")
  public void testTableNullFuncrefTrap(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table 5 funcref)
          (type $void_func (func))
          (func (export "call_null")
            i32.const 0   ;; Table index with null ref
            call_indirect (type $void_func)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Calling null funcref should trap
        try {
          instance.callFunction("call_null");
          fail("Expected trap for null funcref call");
        } catch (final Exception e) {
          // Expected - null funcref
          assertNotNull(e.getMessage(), "Null funcref trap should have a message");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_signature_mismatch")
  public void testTableSignatureMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table 5 funcref)
          (func $returns_void)
          (elem (i32.const 0) $returns_void)
          (type $returns_i32 (func (result i32)))
          (func (export "call_mismatch") (result i32)
            i32.const 0
            call_indirect (type $returns_i32)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Calling with wrong signature should trap
        try {
          instance.callFunction("call_mismatch");
          fail("Expected trap for signature mismatch");
        } catch (final Exception e) {
          // Expected - indirect call type mismatch
          assertNotNull(e.getMessage(), "Signature mismatch trap should have a message");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_with_externref")
  public void testTableWithExternref(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "table") 5 externref)
          (func (export "size") (result i32)
            table.size
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmValue[] results = instance.callFunction("size");
        assertEquals(5, results[0].asInt(), "Externref table should have 5 elements");

        final Optional<WasmTable> tableOpt = instance.getTable("table");
        assertTrue(tableOpt.isPresent(), "Externref table should be exported");
        final WasmValueType elementType = tableOpt.get().getElementType();
        assertEquals(
            WasmValueType.EXTERNREF,
            elementType,
            "Externref table element type should be EXTERNREF");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_element_segment")
  public void testTableElementSegment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table 10 funcref)
          (func $a (result i32) i32.const 10)
          (func $b (result i32) i32.const 20)
          (func $c (result i32) i32.const 30)
          ;; Active element segment at offset 3
          (elem (i32.const 3) $a $b $c)
          (type $i32_func (func (result i32)))
          (func (export "call_at") (param i32) (result i32)
            local.get 0
            call_indirect (type $i32_func)
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Functions should be at indices 3, 4, 5
        WasmValue[] results = instance.callFunction("call_at", WasmValue.i32(3));
        assertEquals(10, results[0].asInt(), "Function at 3 should return 10");

        results = instance.callFunction("call_at", WasmValue.i32(4));
        assertEquals(20, results[0].asInt(), "Function at 4 should return 20");

        results = instance.callFunction("call_at", WasmValue.i32(5));
        assertEquals(30, results[0].asInt(), "Function at 5 should return 30");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table::table_max_size")
  public void testTableMaxSize(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (table (export "table") 5 100 funcref)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmTable table = instance.getTable("table").get();
        assertEquals(5, table.getSize(), "Initial size should be 5");
        assertEquals(100, table.getMaxSize(), "Max size should be 100");
      }
      module.close();
    }
  }
}
