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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WebAssembly function type variations.
 *
 * <p>Tests various function signatures including different parameter counts, types, and return
 * value configurations to ensure comprehensive coverage of function calling.
 *
 * @since 1.0.0
 */
@DisplayName("Function Type Variations Integration Tests")
public final class FunctionTypeVariationsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(FunctionTypeVariationsTest.class.getName());

  private final List<AutoCloseable> resources = new ArrayList<>();

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("No Parameter Functions")
  class NoParameterFunctions {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call function returning i32 constant")
    void shouldCallFunctionReturningI32Constant(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // WAT: (module (func (export "get42") (result i32) (i32.const 42)))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x05, // type section, 5 bytes
        0x01, // 1 type
        0x60,
        0x00,
        0x01,
        0x7F, // () -> i32
        0x03,
        0x02, // function section
        0x01,
        0x00, // 1 function, type 0
        0x07,
        0x09, // export section
        0x01,
        0x05,
        0x67,
        0x65,
        0x74,
        0x34,
        0x32,
        0x00,
        0x00, // "get42" func 0
        0x0A,
        0x06, // code section
        0x01,
        0x04, // 1 function, 4 bytes
        0x00, // 0 locals
        0x41,
        0x2A, // i32.const 42
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmFunction> funcOpt = instance.getFunction("get42");
      assertTrue(funcOpt.isPresent(), "Function 'get42' should be exported");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] results = func.call();

      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should return 1 value");
      assertEquals(42, results[0].asInt(), "Result should be 42");

      LOGGER.info("Function returned: " + results[0].asInt());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call function returning i64 constant")
    void shouldCallFunctionReturningI64Constant(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // WAT: (module (func (export "get_big") (result i64) (i64.const 9223372036854775807)))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x05, // type section
        0x01,
        0x60,
        0x00,
        0x01,
        0x7E, // () -> i64
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x0B, // export section
        0x01,
        0x07,
        0x67,
        0x65,
        0x74,
        0x5F,
        0x62,
        0x69,
        0x67,
        0x00,
        0x00, // "get_big"
        0x0A,
        0x0F, // code section, 15 bytes
        0x01,
        0x0D, // 1 function, 13 bytes
        0x00, // 0 locals
        0x42, // i64.const
        (byte) 0xFF,
        (byte) 0xFF,
        (byte) 0xFF,
        (byte) 0xFF,
        (byte) 0xFF,
        (byte) 0xFF,
        (byte) 0xFF,
        (byte) 0xFF,
        (byte) 0xFF,
        0x00, // Long.MAX_VALUE in signed LEB128 (extra byte for positive sign)
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmFunction> funcOpt = instance.getFunction("get_big");
      assertTrue(funcOpt.isPresent(), "Function 'get_big' should be exported");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] results = func.call();

      assertEquals(1, results.length, "Should return 1 value");
      assertEquals(Long.MAX_VALUE, results[0].asLong(), "Result should be Long.MAX_VALUE");

      LOGGER.info("Function returned: " + results[0].asLong());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call void function")
    void shouldCallVoidFunction(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // WAT: (module (func (export "noop")))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x04, // type section
        0x01,
        0x60,
        0x00,
        0x00, // () -> ()
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x08, // export section
        0x01,
        0x04,
        0x6E,
        0x6F,
        0x6F,
        0x70,
        0x00,
        0x00, // "noop"
        0x0A,
        0x04, // code section
        0x01,
        0x02, // 1 function, 2 bytes
        0x00, // 0 locals
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmFunction> funcOpt = instance.getFunction("noop");
      assertTrue(funcOpt.isPresent(), "Function 'noop' should be exported");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] results = func.call();

      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.length, "Void function should return 0 values");

      LOGGER.info("Void function called successfully");
    }
  }

  @Nested
  @DisplayName("Single Parameter Functions")
  class SingleParameterFunctions {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call function with i32 parameter")
    void shouldCallFunctionWithI32Parameter(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // WAT: (module (func (export "double") (param i32) (result i32)
      //              (i32.mul (local.get 0) (i32.const 2))))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x06, // type section
        0x01,
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // (i32) -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x0A, // export section
        0x01,
        0x06,
        0x64,
        0x6F,
        0x75,
        0x62,
        0x6C,
        0x65,
        0x00,
        0x00, // "double"
        0x0A,
        0x09, // code section
        0x01,
        0x07, // 1 function, 7 bytes
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0
        0x41,
        0x02, // i32.const 2
        0x6C, // i32.mul
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmFunction> funcOpt = instance.getFunction("double");
      assertTrue(funcOpt.isPresent(), "Function 'double' should be exported");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] results = func.call(WasmValue.i32(21));

      assertEquals(1, results.length, "Should return 1 value");
      assertEquals(42, results[0].asInt(), "21 * 2 should equal 42");

      LOGGER.info("double(21) = " + results[0].asInt());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call function with f64 parameter")
    void shouldCallFunctionWithF64Parameter(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // WAT: (module (func (export "negate") (param f64) (result f64)
      //              (f64.neg (local.get 0))))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x06, // type section
        0x01,
        0x60,
        0x01,
        0x7C,
        0x01,
        0x7C, // (f64) -> f64
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x0A, // export section
        0x01,
        0x06,
        0x6E,
        0x65,
        0x67,
        0x61,
        0x74,
        0x65,
        0x00,
        0x00, // "negate"
        0x0A,
        0x07, // code section
        0x01,
        0x05, // 1 function, 5 bytes
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0
        (byte) 0x9A, // f64.neg
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmFunction> funcOpt = instance.getFunction("negate");
      assertTrue(funcOpt.isPresent(), "Function 'negate' should be exported");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] results = func.call(WasmValue.f64(3.14));

      assertEquals(1, results.length, "Should return 1 value");
      assertEquals(-3.14, results[0].asDouble(), 0.0001, "negate(3.14) should equal -3.14");

      LOGGER.info("negate(3.14) = " + results[0].asDouble());
    }
  }

  @Nested
  @DisplayName("Multiple Parameter Functions")
  class MultipleParameterFunctions {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call function with two i32 parameters")
    void shouldCallFunctionWithTwoI32Parameters(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // WAT: (module (func (export "add") (param i32 i32) (result i32)
      //              (i32.add (local.get 0) (local.get 1))))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07, // type section
        0x01,
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07, // export section
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // "add"
        0x0A,
        0x09, // code section
        0x01,
        0x07, // 1 function, 7 bytes
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0
        0x20,
        0x01, // local.get 1
        0x6A, // i32.add
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmFunction> funcOpt = instance.getFunction("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be exported");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] results = func.call(WasmValue.i32(100), WasmValue.i32(23));

      assertEquals(1, results.length, "Should return 1 value");
      assertEquals(123, results[0].asInt(), "100 + 23 should equal 123");

      LOGGER.info("add(100, 23) = " + results[0].asInt());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call function with mixed parameter types")
    void shouldCallFunctionWithMixedParameterTypes(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // WAT: (module (func (export "mix") (param i32 i64) (result i64)
      //              (i64.add (i64.extend_i32_u (local.get 0)) (local.get 1))))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07, // type section
        0x01,
        0x60,
        0x02,
        0x7F,
        0x7E,
        0x01,
        0x7E, // (i32, i64) -> i64
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07, // export section
        0x01,
        0x03,
        0x6D,
        0x69,
        0x78,
        0x00,
        0x00, // "mix"
        0x0A,
        0x0A, // code section
        0x01,
        0x08, // 1 function, 8 bytes
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0
        (byte) 0xAD, // i64.extend_i32_u
        0x20,
        0x01, // local.get 1
        0x7C, // i64.add
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmFunction> funcOpt = instance.getFunction("mix");
      assertTrue(funcOpt.isPresent(), "Function 'mix' should be exported");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] results = func.call(WasmValue.i32(10), WasmValue.i64(1000L));

      assertEquals(1, results.length, "Should return 1 value");
      assertEquals(1010L, results[0].asLong(), "10 + 1000 should equal 1010");

      LOGGER.info("mix(10, 1000) = " + results[0].asLong());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should call function with four parameters")
    void shouldCallFunctionWithFourParameters(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // WAT: (module (func (export "sum4") (param i32 i32 i32 i32) (result i32)
      //              (i32.add (i32.add (i32.add (local.get 0) (local.get 1)) (local.get 2))
      //              (local.get 3))))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x09, // type section
        0x01,
        0x60,
        0x04,
        0x7F,
        0x7F,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32, i32, i32) -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x08, // export section
        0x01,
        0x04,
        0x73,
        0x75,
        0x6D,
        0x34,
        0x00,
        0x00, // "sum4"
        0x0A,
        0x0F, // code section
        0x01,
        0x0D, // 1 function, 13 bytes
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0
        0x20,
        0x01, // local.get 1
        0x6A, // i32.add
        0x20,
        0x02, // local.get 2
        0x6A, // i32.add
        0x20,
        0x03, // local.get 3
        0x6A, // i32.add
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmFunction> funcOpt = instance.getFunction("sum4");
      assertTrue(funcOpt.isPresent(), "Function 'sum4' should be exported");

      final WasmFunction func = funcOpt.get();
      final WasmValue[] results =
          func.call(WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3), WasmValue.i32(4));

      assertEquals(1, results.length, "Should return 1 value");
      assertEquals(10, results[0].asInt(), "1 + 2 + 3 + 4 should equal 10");

      LOGGER.info("sum4(1, 2, 3, 4) = " + results[0].asInt());
    }
  }

  @Nested
  @DisplayName("Edge Case Values")
  class EdgeCaseValues {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle i32 min and max values")
    void shouldHandleI32MinAndMaxValues(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // Simple identity function: (param i32) (result i32) - returns what it receives
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x06, // type section
        0x01,
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // (i32) -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x06, // export section
        0x01,
        0x02,
        0x69,
        0x64,
        0x00,
        0x00, // "id"
        0x0A,
        0x06, // code section
        0x01,
        0x04, // 1 function, 4 bytes
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction func = instance.getFunction("id").orElseThrow();

      // Test min value
      WasmValue[] results = func.call(WasmValue.i32(Integer.MIN_VALUE));
      assertEquals(Integer.MIN_VALUE, results[0].asInt(), "Should handle Integer.MIN_VALUE");

      // Test max value
      results = func.call(WasmValue.i32(Integer.MAX_VALUE));
      assertEquals(Integer.MAX_VALUE, results[0].asInt(), "Should handle Integer.MAX_VALUE");

      // Test zero
      results = func.call(WasmValue.i32(0));
      assertEquals(0, results[0].asInt(), "Should handle zero");

      // Test negative one
      results = func.call(WasmValue.i32(-1));
      assertEquals(-1, results[0].asInt(), "Should handle -1");

      LOGGER.info("Edge case i32 values handled correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle f32 special values")
    void shouldHandleF32SpecialValues(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(0, engine);
      final Store store = engine.createStore();
      resources.add(0, store);

      // Simple identity function: (param f32) (result f32)
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x06, // type section
        0x01,
        0x60,
        0x01,
        0x7D,
        0x01,
        0x7D, // (f32) -> f32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07, // export section
        0x01,
        0x03,
        0x69,
        0x64,
        0x66,
        0x00,
        0x00, // "idf"
        0x0A,
        0x06, // code section
        0x01,
        0x04, // 1 function, 4 bytes
        0x00, // 0 locals
        0x20,
        0x00, // local.get 0
        0x0B // end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final WasmFunction func = instance.getFunction("idf").orElseThrow();

      // Test positive infinity
      WasmValue[] results = func.call(WasmValue.f32(Float.POSITIVE_INFINITY));
      assertTrue(
          Float.isInfinite(results[0].asFloat()) && results[0].asFloat() > 0,
          "Should handle positive infinity");

      // Test negative infinity
      results = func.call(WasmValue.f32(Float.NEGATIVE_INFINITY));
      assertTrue(
          Float.isInfinite(results[0].asFloat()) && results[0].asFloat() < 0,
          "Should handle negative infinity");

      // Test NaN
      results = func.call(WasmValue.f32(Float.NaN));
      assertTrue(Float.isNaN(results[0].asFloat()), "Should handle NaN");

      // Test zero
      results = func.call(WasmValue.f32(0.0f));
      assertEquals(0.0f, results[0].asFloat(), 0.0001f, "Should handle zero");

      LOGGER.info("Edge case f32 values handled correctly");
    }
  }
}
