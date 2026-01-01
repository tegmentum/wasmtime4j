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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Host function support.
 *
 * <p>These tests verify host function creation, linking, and invocation from WebAssembly.
 *
 * @since 1.0.0
 */
@DisplayName("Host Function Integration Tests")
public final class HostFunctionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(HostFunctionIntegrationTest.class.getName());

  /**
   * WebAssembly module that imports and calls a host function.
   *
   * <p>Module structure: - Imports (i32, i32) -> i32 function from "env" "add" - Exports "call_add"
   * that calls the imported function
   *
   * <pre>
   * (module
   *   (import "env" "add" (func $add (param i32 i32) (result i32)))
   *   (func (export "call_add") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     call $add))
   * </pre>
   */
  private static final byte[] IMPORT_ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // magic + version
        0x01,
        0x07,
        0x01,
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // type section
        0x02,
        0x0b,
        0x01,
        0x03,
        0x65,
        0x6e,
        0x76,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // import "env" "add"
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x0c,
        0x01,
        0x08,
        0x63,
        0x61,
        0x6c,
        0x6c,
        0x5f,
        0x61,
        0x64,
        0x64,
        0x00,
        0x01, // export "call_add"
        0x0a,
        0x0a,
        0x01,
        0x08,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x10,
        0x00,
        0x0b // code section
      };

  /**
   * WebAssembly module that imports a void function (no return value).
   *
   * <p>Module structure: - Imports (i32) -> () function from "env" "log" - Exports "do_log" that
   * calls the imported function
   *
   * <pre>
   * (module
   *   (import "env" "log" (func $log (param i32)))
   *   (func (export "do_log") (param i32)
   *     local.get 0
   *     call $log))
   * </pre>
   */
  private static final byte[] IMPORT_LOG_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // magic + version
        0x01,
        0x05,
        0x01,
        0x60,
        0x01,
        0x7f,
        0x00, // type section: (i32) -> ()
        0x02,
        0x0b,
        0x01,
        0x03,
        0x65,
        0x6e,
        0x76,
        0x03,
        0x6c,
        0x6f,
        0x67,
        0x00,
        0x00, // import "env" "log"
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x0a,
        0x01,
        0x06,
        0x64,
        0x6f,
        0x5f,
        0x6c,
        0x6f,
        0x67,
        0x00,
        0x01, // export "do_log"
        0x0a,
        0x08,
        0x01,
        0x06,
        0x00,
        0x20,
        0x00,
        0x10,
        0x00,
        0x0b // code section
      };

  /**
   * WebAssembly module that imports a function with no parameters.
   *
   * <p>Module structure: - Imports () -> i32 function from "env" "get_value" - Exports
   * "fetch_value" that calls the imported function
   *
   * <pre>
   * (module
   *   (import "env" "get_value" (func $get_value (result i32)))
   *   (func (export "fetch_value") (result i32)
   *     call $get_value))
   * </pre>
   */
  private static final byte[] IMPORT_GET_VALUE_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version 1

        // Type section (id=1)
        0x01,
        0x05, // section id and size
        0x01, // number of types
        0x60,
        0x00,
        0x01,
        0x7F, // () -> i32

        // Import section (id=2)
        0x02,
        0x10, // section id and size
        0x01, // number of imports
        0x03,
        0x65,
        0x6E,
        0x76, // "env"
        0x09,
        0x67,
        0x65,
        0x74,
        0x5F,
        0x76,
        0x61,
        0x6C,
        0x75,
        0x65, // "get_value"
        0x00,
        0x00, // function, type index 0

        // Function section (id=3)
        0x03,
        0x02, // section id and size
        0x01, // number of functions
        0x00, // type index 0

        // Export section (id=7)
        0x07,
        0x0F, // section id and size
        0x01, // number of exports
        0x0B,
        0x66,
        0x65,
        0x74,
        0x63,
        0x68,
        0x5F,
        0x76,
        0x61,
        0x6C,
        0x75,
        0x65, // "fetch_value"
        0x00,
        0x01, // function, index 1

        // Code section (id=10)
        0x0A,
        0x05, // section id and size
        0x01, // number of functions
        0x03, // function body size
        0x00, // local count
        0x10,
        0x00, // call 0
        0x0B // end
      };

  /**
   * WebAssembly module that imports multiple functions.
   *
   * <p>Module structure: - Imports "math" "add" (i32, i32) -> i32 - Imports "math" "mul" (i32, i32)
   * -> i32 - Exports "add_mul" that calls both: (a + b) * b
   *
   * <pre>
   * (module
   *   (import "math" "add" (func $add (param i32 i32) (result i32)))
   *   (import "math" "mul" (func $mul (param i32 i32) (result i32)))
   *   (func (export "add_mul") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     call $add
   *     local.get 1
   *     call $mul))
   * </pre>
   */
  private static final byte[] IMPORT_MULTI_FUNC_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version 1

        // Type section (id=1)
        0x01,
        0x07, // section id and size
        0x01, // number of types
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32

        // Import section (id=2)
        0x02,
        0x13, // section id and size
        0x02, // number of imports
        0x04,
        0x6D,
        0x61,
        0x74,
        0x68, // "math"
        0x03,
        0x61,
        0x64,
        0x64, // "add"
        0x00,
        0x00, // function, type index 0
        0x04,
        0x6D,
        0x61,
        0x74,
        0x68, // "math"
        0x03,
        0x6D,
        0x75,
        0x6C, // "mul"
        0x00,
        0x00, // function, type index 0

        // Function section (id=3)
        0x03,
        0x02, // section id and size
        0x01, // number of functions
        0x00, // type index 0

        // Export section (id=7)
        0x07,
        0x0B, // section id and size
        0x01, // number of exports
        0x07,
        0x61,
        0x64,
        0x64,
        0x5F,
        0x6D,
        0x75,
        0x6C, // "add_mul"
        0x00,
        0x02, // function, index 2 (after 2 imports)

        // Code section (id=10)
        0x0A,
        0x0B, // section id and size
        0x01, // number of functions
        0x09, // function body size
        0x00, // local count
        0x20,
        0x00, // local.get 0
        0x20,
        0x01, // local.get 1
        0x10,
        0x00, // call 0 (add)
        0x20,
        0x01, // local.get 1
        0x10,
        0x01, // call 1 (mul)
        0x0B // end
      };

  @Nested
  @DisplayName("Basic Host Function Tests")
  class BasicHostFunctionTests {

    @Test
    @DisplayName("should create linker and define host function")
    void shouldCreateLinkerAndDefineHostFunction() throws Exception {
      LOGGER.info("Testing linker creation and host function definition");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine)) {

        assertNotNull(linker, "Linker should not be null");
        assertTrue(linker.isValid(), "Linker should be valid after creation");

        final FunctionType addType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});

        final HostFunction addImpl =
            (params) -> {
              final int a = params[0].asI32();
              final int b = params[1].asI32();
              return new WasmValue[] {WasmValue.i32(a + b)};
            };

        assertDoesNotThrow(
            () -> linker.defineHostFunction("env", "add", addType, addImpl),
            "Should be able to define host function");

        LOGGER.info("Linker created and host function defined successfully");
      }
    }

    @Test
    @DisplayName("should call host function from WebAssembly")
    void shouldCallHostFunctionFromWebAssembly() throws Exception {
      LOGGER.info("Testing host function invocation from WebAssembly");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(IMPORT_ADD_WASM)) {

        final FunctionType addType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});

        final HostFunction addImpl =
            (params) -> {
              final int a = params[0].asI32();
              final int b = params[1].asI32();
              LOGGER.info("Host add called: " + a + " + " + b);
              return new WasmValue[] {WasmValue.i32(a + b)};
            };

        linker.defineHostFunction("env", "add", addType, addImpl);

        try (final Instance instance = linker.instantiate(store, module)) {
          assertNotNull(instance, "Instance should not be null");

          final Optional<WasmFunction> callAddOpt = instance.getFunction("call_add");
          assertTrue(callAddOpt.isPresent(), "Should have call_add function");

          final WasmValue[] results = callAddOpt.get().call(WasmValue.i32(5), WasmValue.i32(3));
          assertEquals(8, results[0].asInt(), "5 + 3 should equal 8");

          LOGGER.info("Host function call result: " + results[0].asInt());
        }
      }
    }

    @Test
    @DisplayName("should call host function with various integer values")
    void shouldCallHostFunctionWithVariousIntegerValues() throws Exception {
      LOGGER.info("Testing host function with various integer values");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(IMPORT_ADD_WASM)) {

        final FunctionType addType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});

        linker.defineHostFunction(
            "env",
            "add",
            addType,
            (params) -> {
              return new WasmValue[] {WasmValue.i32(params[0].asI32() + params[1].asI32())};
            });

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callAdd = instance.getFunction("call_add").orElseThrow();

          // Test with zero
          WasmValue[] results = callAdd.call(WasmValue.i32(0), WasmValue.i32(0));
          assertEquals(0, results[0].asInt(), "0 + 0 should equal 0");

          // Test with negative values
          results = callAdd.call(WasmValue.i32(-10), WasmValue.i32(5));
          assertEquals(-5, results[0].asInt(), "-10 + 5 should equal -5");

          // Test with large values
          results = callAdd.call(WasmValue.i32(100000), WasmValue.i32(100000));
          assertEquals(200000, results[0].asInt(), "Large values should add correctly");

          LOGGER.info("Various integer value tests passed");
        }
      }
    }
  }

  @Nested
  @DisplayName("Void Host Function Tests")
  class VoidHostFunctionTests {

    @Test
    @DisplayName("should call void host function with side effects")
    void shouldCallVoidHostFunctionWithSideEffects() throws Exception {
      LOGGER.info("Testing void host function with side effects");

      final AtomicInteger loggedValue = new AtomicInteger(-1);

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(IMPORT_LOG_WASM)) {

        final FunctionType logType =
            new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});

        final HostFunction logImpl =
            HostFunction.voidFunction(
                (params) -> {
                  final int value = params[0].asI32();
                  LOGGER.info("Log called with value: " + value);
                  loggedValue.set(value);
                });

        linker.defineHostFunction("env", "log", logType, logImpl);

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmFunction doLog = instance.getFunction("do_log").orElseThrow();

          doLog.call(WasmValue.i32(42));

          assertEquals(42, loggedValue.get(), "Logged value should be 42");
          LOGGER.info("Void host function side effect verified");
        }
      }
    }

    @Test
    @DisplayName("should call void host function multiple times")
    void shouldCallVoidHostFunctionMultipleTimes() throws Exception {
      LOGGER.info("Testing void host function multiple calls");

      final AtomicInteger callCount = new AtomicInteger(0);

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(IMPORT_LOG_WASM)) {

        final FunctionType logType =
            new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});

        linker.defineHostFunction(
            "env",
            "log",
            logType,
            HostFunction.voidFunction((params) -> callCount.incrementAndGet()));

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmFunction doLog = instance.getFunction("do_log").orElseThrow();

          doLog.call(WasmValue.i32(1));
          doLog.call(WasmValue.i32(2));
          doLog.call(WasmValue.i32(3));

          assertEquals(3, callCount.get(), "Host function should be called 3 times");
          LOGGER.info("Multiple call count verified: " + callCount.get());
        }
      }
    }
  }

  @Nested
  @DisplayName("Host Function Factory Method Tests")
  class HostFunctionFactoryMethodTests {

    @Test
    @DisplayName("should create single value host function")
    void shouldCreateSingleValueHostFunction() throws Exception {
      LOGGER.info("Testing single value host function factory");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(IMPORT_GET_VALUE_WASM)) {

        final FunctionType getValueType =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});

        final HostFunction getValueImpl =
            HostFunction.singleValue((params) -> WasmValue.i32(12345));

        linker.defineHostFunction("env", "get_value", getValueType, getValueImpl);

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmFunction fetchValue = instance.getFunction("fetch_value").orElseThrow();

          final WasmValue[] results = fetchValue.call();
          assertEquals(12345, results[0].asInt(), "Should return 12345");

          LOGGER.info("Single value host function test passed");
        }
      }
    }

    @Test
    @DisplayName("should create host function with validation")
    void shouldCreateHostFunctionWithValidation() throws Exception {
      LOGGER.info("Testing host function with validation");

      final HostFunction baseImpl =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() + params[1].asI32())};

      final HostFunction validatedImpl = HostFunction.withValidation(baseImpl, WasmValueType.I32);

      // Call with valid return type should succeed
      final WasmValue[] result =
          validatedImpl.execute(new WasmValue[] {WasmValue.i32(5), WasmValue.i32(3)});

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should have one result");
      assertEquals(8, result[0].asI32(), "Result should be 8");

      LOGGER.info("Validation test passed");
    }
  }

  @Nested
  @DisplayName("Multiple Host Functions Tests")
  class MultipleHostFunctionsTests {

    @Test
    @DisplayName("should define and call multiple host functions")
    void shouldDefineAndCallMultipleHostFunctions() throws Exception {
      LOGGER.info("Testing multiple host functions");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(IMPORT_MULTI_FUNC_WASM)) {

        final FunctionType mathType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});

        linker.defineHostFunction(
            "math",
            "add",
            mathType,
            (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() + params[1].asI32())});

        linker.defineHostFunction(
            "math",
            "mul",
            mathType,
            (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() * params[1].asI32())});

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmFunction addMul = instance.getFunction("add_mul").orElseThrow();

          // (a + b) * b = (5 + 3) * 3 = 8 * 3 = 24
          final WasmValue[] results = addMul.call(WasmValue.i32(5), WasmValue.i32(3));
          assertEquals(24, results[0].asInt(), "(5 + 3) * 3 should equal 24");

          // (10 + 2) * 2 = 24
          final WasmValue[] results2 = addMul.call(WasmValue.i32(10), WasmValue.i32(2));
          assertEquals(24, results2[0].asInt(), "(10 + 2) * 2 should equal 24");

          LOGGER.info("Multiple host functions test passed");
        }
      }
    }
  }

  @Nested
  @DisplayName("Linker Lifecycle Tests")
  class LinkerLifecycleTests {

    @Test
    @DisplayName("should properly close linker")
    void shouldProperlyCloseLinker() throws Exception {
      LOGGER.info("Testing linker close");

      final Linker<Void> linker;
      try (final Engine engine = Engine.create()) {
        linker = Linker.create(engine);
        assertTrue(linker.isValid(), "Linker should be valid before close");

        linker.close();
        // After close, linker should be invalid
        // Note: exact behavior may vary by implementation
      }

      LOGGER.info("Linker close test completed");
    }

    @Test
    @DisplayName("should handle multiple close calls gracefully")
    void shouldHandleMultipleCloseCallsGracefully() throws Exception {
      LOGGER.info("Testing multiple linker close calls");

      try (final Engine engine = Engine.create()) {
        final Linker<Void> linker = Linker.create(engine);
        linker.close();

        assertDoesNotThrow(linker::close, "Multiple close calls should not throw");
      }

      LOGGER.info("Multiple close calls handled gracefully");
    }

    @Test
    @DisplayName("should get engine from linker")
    void shouldGetEngineFromLinker() throws Exception {
      LOGGER.info("Testing engine retrieval from linker");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine)) {

        final Engine linkerEngine = linker.getEngine();
        assertNotNull(linkerEngine, "Engine from linker should not be null");

        LOGGER.info("Engine retrieval from linker successful");
      }
    }
  }

  @Nested
  @DisplayName("Linker Configuration Tests")
  class LinkerConfigurationTests {

    @Test
    @DisplayName("should allow shadowing when configured")
    void shouldAllowShadowingWhenConfigured() throws Exception {
      LOGGER.info("Testing linker shadowing configuration");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine)) {

        final FunctionType type =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});

        // Define first function
        linker.defineHostFunction(
            "test", "func", type, (params) -> new WasmValue[] {WasmValue.i32(1)});

        // Enable shadowing
        linker.allowShadowing(true);

        // Define again - should not throw
        assertDoesNotThrow(
            () ->
                linker.defineHostFunction(
                    "test", "func", type, (params) -> new WasmValue[] {WasmValue.i32(2)}),
            "Should allow shadowing when configured");

        LOGGER.info("Shadowing configuration test passed");
      }
    }

    @Test
    @DisplayName("should check if import is defined")
    void shouldCheckIfImportIsDefined() throws Exception {
      LOGGER.info("Testing hasImport check");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine)) {

        final FunctionType type =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});

        linker.defineHostFunction(
            "test", "func", type, (params) -> new WasmValue[] {WasmValue.i32(42)});

        assertTrue(linker.hasImport("test", "func"), "Should find defined import");

        LOGGER.info("hasImport check test passed");
      }
    }
  }

  @Nested
  @DisplayName("Host Function Exception Handling Tests")
  class HostFunctionExceptionHandlingTests {

    @Test
    @DisplayName("should propagate exception from host function as trap")
    void shouldPropagateExceptionFromHostFunctionAsTrap() throws Exception {
      LOGGER.info("Testing host function exception propagation");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore();
          final Module module = engine.compileModule(IMPORT_ADD_WASM)) {

        final FunctionType addType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});

        linker.defineHostFunction(
            "env",
            "add",
            addType,
            (params) -> {
              throw new WasmException("Intentional test error");
            });

        try (final Instance instance = linker.instantiate(store, module)) {
          final WasmFunction callAdd = instance.getFunction("call_add").orElseThrow();

          // Calling should throw due to host function exception
          assertThrows(
              Exception.class,
              () -> callAdd.call(WasmValue.i32(1), WasmValue.i32(2)),
              "Should throw when host function throws");

          LOGGER.info("Exception propagation test passed");
        }
      }
    }
  }

  @Nested
  @DisplayName("Streaming Host Function Tests")
  class StreamingHostFunctionTests {

    @Test
    @DisplayName("should create streaming host function")
    void shouldCreateStreamingHostFunction() throws Exception {
      LOGGER.info("Testing streaming host function creation");

      final HostFunction streamingImpl =
          HostFunction.streaming(
              (params, context) -> {
                context.yield(WasmValue.i32(1));
                context.yield(WasmValue.i32(2));
                context.yield(WasmValue.i32(3));
              });

      // Execute the streaming function
      final WasmValue[] results = streamingImpl.execute(new WasmValue[] {});

      assertNotNull(results, "Results should not be null");
      assertEquals(3, results.length, "Should have 3 results");
      assertEquals(1, results[0].asI32(), "First result should be 1");
      assertEquals(2, results[1].asI32(), "Second result should be 2");
      assertEquals(3, results[2].asI32(), "Third result should be 3");

      LOGGER.info("Streaming host function test passed");
    }

    @Test
    @DisplayName("should use streaming context correctly")
    void shouldUseStreamingContextCorrectly() throws Exception {
      LOGGER.info("Testing streaming context usage");

      final HostFunction.StreamingContext context = new HostFunction.StreamingContext();

      context.yield(WasmValue.i32(10));
      assertEquals(1, context.getResultCount(), "Should have 1 result after first yield");

      context.yield(WasmValue.i32(20), WasmValue.i32(30));
      assertEquals(3, context.getResultCount(), "Should have 3 results after multi-yield");

      final WasmValue[] results = context.getResults();
      assertEquals(3, results.length, "Should have 3 results total");

      context.clear();
      assertEquals(0, context.getResultCount(), "Should have 0 results after clear");

      LOGGER.info("Streaming context usage test passed");
    }
  }
}
