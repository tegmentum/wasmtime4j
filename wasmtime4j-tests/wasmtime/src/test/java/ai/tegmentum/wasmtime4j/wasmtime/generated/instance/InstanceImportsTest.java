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
package ai.tegmentum.wasmtime4j.wasmtime.generated.instance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime instance.rs tests.
 *
 * <p>Original source: instance.rs - Tests instance creation and import handling.
 *
 * <p>This test validates that wasmtime4j instance operations produce the same behavior as the
 * upstream Wasmtime implementation.
 */
public final class InstanceImportsTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instance::basic_instantiation")
  public void testBasicInstantiation(final RuntimeType runtime) throws Exception {
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
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        assertNotNull(instance, "Instance should be created");

        // Call the exported function
        final WasmValue[] results =
            instance.callFunction("add", WasmValue.i32(5), WasmValue.i32(3));
        assertEquals(1, results.length, "Should have one result");
        assertEquals(8, results[0].asInt(), "5 + 3 should equal 8");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instance::initializes_linear_memory")
  public void testInitializesLinearMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Module with data segment that initializes memory
    final String wat =
        """
        (module
          (memory (export "memory") 2)
          (data (i32.const 0) "Hello World!")
          (func (export "read") (param i32) (result i32)
            local.get 0
            i32.load8_u
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Read "Hello World!" from memory one byte at a time
        final byte[] expected = "Hello World!".getBytes();
        final byte[] actual = new byte[expected.length];

        for (int i = 0; i < expected.length; i++) {
          final WasmValue[] results = instance.callFunction("read", WasmValue.i32(i));
          actual[i] = (byte) results[0].asInt();
        }

        assertArrayEquals(expected, actual, "Memory should be initialized with 'Hello World!'");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instance::memory_grow_and_size")
  public void testMemoryGrowAndSize(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1 10)
          (func (export "size") (result i32)
            memory.size
          )
          (func (export "grow") (param i32) (result i32)
            local.get 0
            memory.grow
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Initial size should be 1 page
        WasmValue[] results = instance.callFunction("size");
        assertEquals(1, results[0].asInt(), "Initial memory size should be 1 page");

        // Grow by 2 pages, should succeed and return old size
        results = instance.callFunction("grow", WasmValue.i32(2));
        assertEquals(1, results[0].asInt(), "Grow should return old size (1)");

        // New size should be 3 pages
        results = instance.callFunction("size");
        assertEquals(3, results[0].asInt(), "Memory size should now be 3 pages");

        // Grow by 7 pages to reach limit
        results = instance.callFunction("grow", WasmValue.i32(7));
        assertEquals(3, results[0].asInt(), "Grow should return old size (3)");

        // Size should be 10 (the maximum)
        results = instance.callFunction("size");
        assertEquals(10, results[0].asInt(), "Memory size should be at maximum (10)");

        // Try to grow beyond limit, should fail (-1)
        results = instance.callFunction("grow", WasmValue.i32(1));
        assertEquals(-1, results[0].asInt(), "Growing beyond limit should return -1");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instance::multiple_instances_same_module")
  public void testMultipleInstancesSameModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (global (mut i32) (i32.const 0))
          (func (export "get") (result i32)
            global.get 0
          )
          (func (export "set") (param i32)
            local.get 0
            global.set 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore()) {

        // Create two instances from the same module
        try (final Instance instance1 = module.instantiate(store);
            final Instance instance2 = module.instantiate(store)) {

          // Set different values in each instance
          instance1.callFunction("set", WasmValue.i32(100));
          instance2.callFunction("set", WasmValue.i32(200));

          // Each instance should have its own global
          WasmValue[] results1 = instance1.callFunction("get");
          WasmValue[] results2 = instance2.callFunction("get");

          assertEquals(100, results1[0].asInt(), "Instance 1 should have value 100");
          assertEquals(200, results2[0].asInt(), "Instance 2 should have value 200");
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instance::missing_import_should_fail")
  public void testMissingImportShouldFail(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Module that requires an import
    final String wat =
        """
        (module
          (import "env" "log" (func (param i32)))
          (func (export "call")
            i32.const 42
            call 0
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore()) {

        // Instantiation without providing the import should fail
        assertThrows(
            Exception.class,
            () -> module.instantiate(store),
            "Instantiation without required import should fail");
      }
      module.close();
    }
  }
}
