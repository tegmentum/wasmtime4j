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
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.ExportType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for Module compilation, Instance creation, and Function invocation.
 *
 * <p>These tests verify the core WebAssembly workflow: compile a module, create an instance, and
 * invoke exported functions.
 *
 * @since 1.0.0
 */
@DisplayName("Module and Instance Integration Tests")
public final class ModuleInstanceTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleInstanceTest.class.getName());

  /** Simple WebAssembly module that exports an add function: (i32, i32) -> i32. */
  private static final byte[] ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07, // type section (1 type, 7 bytes)
        0x01, // 1 function type
        0x60, // func
        0x02,
        0x7F,
        0x7F, // 2 params: i32, i32
        0x01,
        0x7F, // 1 result: i32
        0x03,
        0x02, // function section
        0x01,
        0x00, // 1 function, type index 0
        0x07,
        0x07, // export section (1 export, 7 bytes)
        0x01, // 1 export
        0x03,
        0x61,
        0x64,
        0x64, // "add"
        0x00,
        0x00, // function export, index 0
        0x0A,
        0x09, // code section
        0x01, // 1 function body
        0x07, // body size
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0
        0x20,
        0x01, // local.get 1
        0x6A, // i32.add
        0x0B // end
      };

  /** WebAssembly module that exports a constant function: () -> i32 returning 42. */
  private static final byte[] CONST_42_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x05, // type section
        0x01, // 1 function type
        0x60, // func
        0x00, // 0 params
        0x01,
        0x7F, // 1 result: i32
        0x03,
        0x02, // function section
        0x01,
        0x00, // 1 function, type index 0
        0x07,
        0x0A, // export section
        0x01, // 1 export
        0x06,
        0x67,
        0x65,
        0x74,
        0x5F,
        0x34,
        0x32, // "get_42"
        0x00,
        0x00, // function export, index 0
        0x0A,
        0x06, // code section
        0x01, // 1 function body
        0x04, // body size
        0x00, // 0 locals
        0x41,
        0x2A, // i32.const 42
        0x0B // end
      };

  /** WebAssembly module with one page of memory exported. */
  private static final byte[] MEMORY_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x05,
        0x03, // memory section
        0x01, // 1 memory
        0x00,
        0x01, // min 1 page, no max
        0x07,
        0x0A, // export section
        0x01, // 1 export
        0x06,
        0x6D,
        0x65,
        0x6D,
        0x6F,
        0x72,
        0x79, // "memory"
        0x02,
        0x00 // memory export, index 0
      };

  /** WebAssembly module that exports multiple functions. */
  private static final byte[] MULTI_FUNC_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x0C, // type section (12 bytes)
        0x02, // 2 function types
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // (i32) -> i32
        0x03,
        0x03, // function section
        0x02,
        0x00,
        0x01, // 2 functions, types 0 and 1
        0x07,
        0x0E, // export section (14 bytes)
        0x02, // 2 exports
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // "add", func 0
        0x04,
        0x69,
        0x6E,
        0x63,
        0x72,
        0x00,
        0x01, // "incr", func 1
        0x0A,
        0x11, // code section (17 bytes)
        0x02, // 2 function bodies
        0x07, // body 1 size (7 bytes)
        0x00, // 0 locals
        0x20,
        0x00,
        0x20,
        0x01,
        0x6A,
        0x0B, // local.get 0, local.get 1, i32.add, end
        0x07, // body 2 size (7 bytes)
        0x00, // 0 locals
        0x20,
        0x00,
        0x41,
        0x01,
        0x6A,
        0x0B // local.get 0, i32.const 1, i32.add, end
      };

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Module Compilation Tests")
  class ModuleCompilationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should compile valid WebAssembly bytecode")
    void shouldCompileValidWasmBytecode(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing compilation of valid WASM bytecode [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(ADD_WASM)) {

        assertNotNull(module, "Compiled module should not be null");
        assertTrue(module.isValid(), "Compiled module should be valid");
        LOGGER.info("Module compiled successfully, valid=" + module.isValid());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for invalid bytecode")
    void shouldThrowExceptionForInvalidBytecode(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing compilation of invalid bytecode [" + runtime + "]");

      try (final Engine engine = Engine.create()) {
        final byte[] invalidWasm = new byte[] {0x00, 0x01, 0x02, 0x03};
        assertThrows(
            WasmException.class,
            () -> engine.compileModule(invalidWasm),
            "Should throw WasmException for invalid bytecode");
      }

      LOGGER.info("Correctly threw exception for invalid bytecode");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should throw exception for empty bytecode")
    void shouldThrowExceptionForEmptyBytecode(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing compilation of empty bytecode [" + runtime + "]");

      try (final Engine engine = Engine.create()) {
        final byte[] emptyWasm = new byte[0];
        assertThrows(
            Exception.class,
            () -> engine.compileModule(emptyWasm),
            "Should throw exception for empty bytecode");
      }

      LOGGER.info("Correctly threw exception for empty bytecode");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should list exports from compiled module")
    void shouldListExportsFromCompiledModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing export listing from compiled module [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(ADD_WASM)) {

        final List<ExportType> exports = module.getExports();
        assertNotNull(exports, "Exports list should not be null");
        assertFalse(exports.isEmpty(), "Exports list should not be empty");
        LOGGER.info("Found " + exports.size() + " exports");

        boolean foundAdd = false;
        for (final ExportType export : exports) {
          LOGGER.info(
              "Export: " + export.getName() + " (kind: " + export.getType().getKind() + ")");
          if ("add".equals(export.getName())) {
            foundAdd = true;
          }
        }
        assertTrue(foundAdd, "Should find 'add' export");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should list exports from module with memory")
    void shouldListExportsFromModuleWithMemory(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing export listing from module with memory [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(MEMORY_WASM)) {

        final List<ExportType> exports = module.getExports();
        assertNotNull(exports, "Exports list should not be null");
        LOGGER.info("Found " + exports.size() + " exports from memory module");

        boolean foundMemory = false;
        for (final ExportType export : exports) {
          LOGGER.info(
              "Export: " + export.getName() + " (kind: " + export.getType().getKind() + ")");
          if ("memory".equals(export.getName())) {
            foundMemory = true;
          }
        }
        assertTrue(foundMemory, "Should find 'memory' export");
      }
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create instance from compiled module")
    void shouldCreateInstanceFromCompiledModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing instance creation from compiled module [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        assertNotNull(instance, "Instance should not be null");
        assertTrue(instance.isValid(), "Instance should be valid");
        LOGGER.info("Instance created successfully, valid=" + instance.isValid());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should list function names from instance")
    void shouldListFunctionNamesFromInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing function name listing from instance [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(MULTI_FUNC_WASM);
          final Instance instance = module.instantiate(store)) {

        final String[] exportNames = instance.getExportNames();
        assertNotNull(exportNames, "Export names list should not be null");
        LOGGER.info("Found exports: " + java.util.Arrays.toString(exportNames));

        boolean foundAdd = false;
        boolean foundIncr = false;
        for (final String name : exportNames) {
          if ("add".equals(name)) {
            foundAdd = true;
          }
          if ("incr".equals(name)) {
            foundIncr = true;
          }
        }
        assertTrue(foundAdd, "Should contain 'add' function");
        assertTrue(foundIncr, "Should contain 'incr' function");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get exports map from instance")
    void shouldGetExportsMapFromInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing exports map from instance [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(MULTI_FUNC_WASM);
          final Instance instance = module.instantiate(store)) {

        final List<Map.Entry<String, ai.tegmentum.wasmtime4j.Extern>> exports =
            instance.getExports();
        assertNotNull(exports, "Exports list should not be null");
        assertFalse(exports.isEmpty(), "Exports list should not be empty");

        LOGGER.info("Exports list has " + exports.size() + " entries");
        for (final Map.Entry<String, ai.tegmentum.wasmtime4j.Extern> entry : exports) {
          LOGGER.info(
              "Export: " + entry.getKey() + " -> " + entry.getValue().getClass().getSimpleName());
        }
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create multiple instances from same module")
    void shouldCreateMultipleInstancesFromSameModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing multiple instance creation from same module [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance1 = module.instantiate(store);
          final Instance instance2 = module.instantiate(store)) {

        assertNotNull(instance1, "First instance should not be null");
        assertNotNull(instance2, "Second instance should not be null");
        assertTrue(instance1.isValid(), "First instance should be valid");
        assertTrue(instance2.isValid(), "Second instance should be valid");

        LOGGER.info("Both instances created successfully");
      }
    }
  }

  @Nested
  @DisplayName("Function Invocation Tests")
  class FunctionInvocationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should invoke add function with two i32 arguments")
    void shouldInvokeAddFunctionWithTwoI32Arguments(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing add function invocation [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "Should find 'add' function");

        final WasmFunction add = addFunc.get();
        LOGGER.info("Found add function");

        final WasmValue[] results = add.call(WasmValue.i32(5), WasmValue.i32(3));
        assertNotNull(results, "Results should not be null");
        assertTrue(results.length > 0, "Should have at least one result");
        LOGGER.info("add(5, 3) = " + results[0].asInt());

        assertEquals(8, results[0].asInt(), "5 + 3 should equal 8");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should invoke constant function with no arguments")
    void shouldInvokeConstantFunctionWithNoArguments(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing constant function invocation [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(CONST_42_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> getFunc = instance.getFunction("get_42");
        assertTrue(getFunc.isPresent(), "Should find 'get_42' function");

        final WasmFunction get42 = getFunc.get();
        assertEquals(
            0, get42.getFunctionType().getParamTypes().length, "get_42 should have 0 parameters");

        final WasmValue[] results = get42.call();
        assertNotNull(results, "Results should not be null");
        assertTrue(results.length > 0, "Should have at least one result");
        LOGGER.info("get_42() = " + results[0].asInt());

        assertEquals(42, results[0].asInt(), "get_42() should return 42");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should invoke multiple functions from same instance")
    void shouldInvokeMultipleFunctionsFromSameInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing multiple function invocations [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(MULTI_FUNC_WASM);
          final Instance instance = module.instantiate(store)) {

        // Test add function
        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "Should find 'add' function");
        final WasmValue[] addResults = addFunc.get().call(WasmValue.i32(10), WasmValue.i32(20));
        assertEquals(30, addResults[0].asInt(), "10 + 20 should equal 30");
        LOGGER.info("add(10, 20) = " + addResults[0].asInt());

        // Test incr function
        final Optional<WasmFunction> incrFunc = instance.getFunction("incr");
        assertTrue(incrFunc.isPresent(), "Should find 'incr' function");
        final WasmValue[] incrResults = incrFunc.get().call(WasmValue.i32(99));
        assertEquals(100, incrResults[0].asInt(), "incr(99) should equal 100");
        LOGGER.info("incr(99) = " + incrResults[0].asInt());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return empty optional for non-existent function")
    void shouldReturnEmptyOptionalForNonExistentFunction(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing non-existent function lookup [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> nonExistent = instance.getFunction("does_not_exist");
        assertFalse(nonExistent.isPresent(), "Non-existent function should return empty Optional");

        LOGGER.info("Correctly returned empty Optional for non-existent function");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle negative i32 arguments")
    void shouldHandleNegativeI32Arguments(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing add function with negative arguments [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "Should find 'add' function");

        final WasmValue[] results = addFunc.get().call(WasmValue.i32(-10), WasmValue.i32(5));
        LOGGER.info("add(-10, 5) = " + results[0].asInt());
        assertEquals(-5, results[0].asInt(), "-10 + 5 should equal -5");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle i32 overflow correctly")
    void shouldHandleI32OverflowCorrectly(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing add function with overflow [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "Should find 'add' function");

        // Integer.MAX_VALUE + 1 should overflow
        final WasmValue[] results =
            addFunc.get().call(WasmValue.i32(Integer.MAX_VALUE), WasmValue.i32(1));
        LOGGER.info("add(MAX_VALUE, 1) = " + results[0].asInt());
        assertEquals(
            Integer.MIN_VALUE, results[0].asInt(), "MAX_VALUE + 1 should overflow to MIN_VALUE");
      }
    }
  }

  @Nested
  @DisplayName("Memory Export Tests")
  class MemoryExportTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get exported memory from instance")
    void shouldGetExportedMemoryFromInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing memory export retrieval [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(MEMORY_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmMemory> memory = instance.getMemory("memory");
        assertTrue(memory.isPresent(), "Should find 'memory' export");

        final WasmMemory mem = memory.get();
        assertNotNull(mem, "Memory should not be null");
        LOGGER.info("Memory size: " + mem.getSize() + " pages (" + mem.dataSize() + " bytes)");

        // Should have at least 1 page (64KB)
        assertTrue(mem.getSize() >= 1, "Memory should have at least 1 page");
        assertTrue(mem.dataSize() >= 65536, "Memory should have at least 64KB");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should list memory names from instance")
    void shouldListMemoryNamesFromInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing memory name listing [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(MEMORY_WASM);
          final Instance instance = module.instantiate(store)) {

        final String[] exportNames = instance.getExportNames();
        assertNotNull(exportNames, "Export names should not be null");
        boolean foundMemory = false;
        for (final String name : exportNames) {
          if ("memory".equals(name)) {
            foundMemory = true;
            break;
          }
        }
        assertTrue(foundMemory, "Should contain 'memory' export");
        LOGGER.info("Export names: " + java.util.Arrays.toString(exportNames));
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should get default memory from instance")
    void shouldGetDefaultMemoryFromInstance(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing default memory retrieval [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(MEMORY_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmMemory> defaultMemory = instance.getDefaultMemory();
        assertTrue(defaultMemory.isPresent(), "Should have default memory");
        LOGGER.info("Default memory found with size: " + defaultMemory.get().getSize() + " pages");
      }
    }
  }

  @Nested
  @DisplayName("Resource Lifecycle Tests")
  class ResourceLifecycleTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should close instance before module")
    void shouldCloseInstanceBeforeModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing resource closure order [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(ADD_WASM);
        final Instance instance = module.instantiate(store);

        assertTrue(instance.isValid(), "Instance should be valid before close");
        assertTrue(module.isValid(), "Module should be valid before close");

        instance.close();
        LOGGER.info("Instance closed");

        module.close();
        LOGGER.info("Module closed");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should invalidate instance after close")
    void shouldInvalidateInstanceAfterClose(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing instance invalidation after close [" + runtime + "]");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM)) {

        final Instance instance = module.instantiate(store);
        assertTrue(instance.isValid(), "Instance should be valid before close");

        instance.close();
        assertFalse(instance.isValid(), "Instance should be invalid after close");
        LOGGER.info("Instance correctly invalidated after close");
      }
    }
  }
}
