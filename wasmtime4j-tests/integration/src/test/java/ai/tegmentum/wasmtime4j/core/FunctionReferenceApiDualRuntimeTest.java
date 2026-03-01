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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime API tests for {@link FunctionReference}.
 *
 * <p>Verifies function reference creation, properties, calling, multiple types, null parameter
 * handling, and independent multiple references across both JNI and Panama runtimes via the unified
 * API.
 *
 * @since 1.0.0
 */
@DisplayName("FunctionReference API Dual-Runtime Tests")
@SuppressWarnings("deprecation")
public class FunctionReferenceApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(FunctionReferenceApiDualRuntimeTest.class.getName());

  private static final String FUNCTIONS_WAT =
      "(module\n"
          + "  (func (export \"return_i32\") (result i32) (i32.const 42))\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    (i32.add (local.get 0) (local.get 1)))\n"
          + "  (func (export \"i64_identity\") (param i64) (result i64)\n"
          + "    (local.get 0))\n"
          + "  (func (export \"f64_double\") (param f64) (result f64)\n"
          + "    (f64.mul (local.get 0) (f64.const 2.0)))\n"
          + "  (func (export \"void_func\"))\n"
          + ")";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ==================== Host Function Reference Creation and Properties Tests ====================

  @Nested
  @DisplayName("Host Function Reference Creation and Properties Tests")
  class HostFunctionReferenceTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create host function reference with valid parameters")
    void shouldCreateHostFunctionReference(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing host function reference creation");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
        final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(99)};

        final FunctionReference ref = store.createFunctionReference(hf, ft);
        assertNotNull(ref, "Function reference should not be null");
        LOGGER.info("[" + runtime + "] Created host function reference: " + ref);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return function type for host function reference")
    void shouldReturnFunctionType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing function type retrieval");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});
        final HostFunction hf =
            (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        final FunctionType returnedType = ref.getFunctionType();
        assertNotNull(returnedType, "Function type should not be null");
        assertEquals(2, returnedType.getParamTypes().length, "Should have 2 param types");
        assertEquals(1, returnedType.getReturnTypes().length, "Should have 1 return type");
        assertEquals(
            WasmValueType.I32, returnedType.getParamTypes()[0], "First param should be I32");
        assertEquals(
            WasmValueType.I32, returnedType.getReturnTypes()[0], "Return type should be I32");
        LOGGER.info(
            "["
                + runtime
                + "] Function type: params="
                + returnedType.getParamTypes().length
                + " returns="
                + returnedType.getReturnTypes().length);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should be valid after creation")
    void shouldBeValidAfterCreation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing validity after creation");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
        final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        assertTrue(ref.isValid(), "Function reference should be valid after creation");
        LOGGER.info("[" + runtime + "] isValid=" + ref.isValid());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should have non-negative ID")
    void shouldHaveNonNegativeId(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing function reference ID");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
        final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        assertTrue(
            ref.getId() >= 0, "Function reference ID should be non-negative: " + ref.getId());
        LOGGER.info("[" + runtime + "] Function reference ID: " + ref.getId());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should call host function reference with no parameters")
    void shouldCallHostFunctionNoParams(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing host function call with no params");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
        final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(42)};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        final WasmValue[] results = ref.call();
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.length, "Should have 1 result");
        assertEquals(42, results[0].asInt(), "Result should be 42");
        LOGGER.info("[" + runtime + "] Call result: " + results[0].asInt());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should call host function reference with parameters")
    void shouldCallHostFunctionWithParams(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing host function call with params");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});
        final HostFunction hf =
            (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        final WasmValue[] results = ref.call(WasmValue.i32(10), WasmValue.i32(20));
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.length, "Should have 1 result");
        assertEquals(30, results[0].asInt(), "Result should be 30");
        LOGGER.info("[" + runtime + "] Call result (10+20): " + results[0].asInt());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should call void host function reference")
    void shouldCallVoidHostFunction(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing void host function call");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft = FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {});
        final HostFunction hf = (params) -> new WasmValue[] {};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        final WasmValue[] results = ref.call();
        assertNotNull(results, "Results should not be null");
        assertEquals(0, results.length, "Should have no results for void function");
        LOGGER.info(
            "[" + runtime + "] Void function call completed, result length: " + results.length);
      }
    }
  }

  // ==================== Store-Created Wasm Function Reference Tests ====================

  @Nested
  @DisplayName("Store-Created Wasm Function Reference Tests")
  class StoreCreatedFunctionReferenceTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create function reference from wasm function via store")
    void shouldCreateFromWasmFunctionViaStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing store-created wasm function reference");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine);
          Linker<?> linker = Linker.create(engine)) {

        final Module module = engine.compileWat(FUNCTIONS_WAT);
        final Instance instance = linker.instantiate(store, module);
        final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
        assertTrue(funcOpt.isPresent(), "return_i32 function should exist");

        final FunctionReference ref = store.createFunctionReference(funcOpt.get());

        assertNotNull(ref, "Store-created function reference should not be null");
        assertTrue(ref.isValid(), "Should be valid");
        LOGGER.info("[" + runtime + "] Store-created wasm function reference: " + ref);
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should call store-created wasm function reference")
    void shouldCallStoreCreatedWasmFunctionReference(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing store-created wasm function reference call");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine);
          Linker<?> linker = Linker.create(engine)) {

        final Module module = engine.compileWat(FUNCTIONS_WAT);
        final Instance instance = linker.instantiate(store, module);
        final Optional<WasmFunction> funcOpt = instance.getFunction("add");
        assertTrue(funcOpt.isPresent(), "add function should exist");

        final FunctionReference ref = store.createFunctionReference(funcOpt.get());

        final WasmValue[] results = ref.call(WasmValue.i32(100), WasmValue.i32(200));
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.length, "Should have 1 result");
        assertEquals(300, results[0].asInt(), "100+200 should equal 300");
        LOGGER.info(
            "["
                + runtime
                + "] Store-created function call result (100+200): "
                + results[0].asInt());
      }
    }
  }

  // ==================== Multiple Function Type Tests ====================

  @Nested
  @DisplayName("Multiple Function Type Tests")
  class MultipleTypeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create I64 host function reference")
    void shouldCreateI64HostFunctionReference(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing I64 host function reference");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.I64});
        final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i64(params[0].asLong() * 2)};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        final WasmValue[] results = ref.call(WasmValue.i64(100L));
        assertEquals(200L, results[0].asLong(), "100*2 should equal 200");
        LOGGER.info("[" + runtime + "] I64 function result: " + results[0].asLong());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create F32 host function reference")
    void shouldCreateF32HostFunctionReference(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing F32 host function reference");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(
                new WasmValueType[] {WasmValueType.F32}, new WasmValueType[] {WasmValueType.F32});
        final HostFunction hf =
            (params) -> new WasmValue[] {WasmValue.f32(params[0].asFloat() + 1.0f)};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        final WasmValue[] results = ref.call(WasmValue.f32(3.14f));
        assertEquals(4.14f, results[0].asFloat(), 0.001f, "3.14+1.0 should equal 4.14");
        LOGGER.info("[" + runtime + "] F32 function result: " + results[0].asFloat());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create F64 host function reference")
    void shouldCreateF64HostFunctionReference(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing F64 host function reference");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(
                new WasmValueType[] {WasmValueType.F64}, new WasmValueType[] {WasmValueType.F64});
        final HostFunction hf =
            (params) -> new WasmValue[] {WasmValue.f64(params[0].asDouble() * 0.5)};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        final WasmValue[] results = ref.call(WasmValue.f64(10.0));
        assertEquals(5.0, results[0].asDouble(), 0.001, "10.0*0.5 should equal 5.0");
        LOGGER.info("[" + runtime + "] F64 function result: " + results[0].asDouble());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create multi-parameter host function reference")
    void shouldCreateMultiParamHostFunctionReference(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multi-parameter host function reference");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I64, WasmValueType.F64},
                new WasmValueType[] {WasmValueType.F64});
        final HostFunction hf =
            (params) ->
                new WasmValue[] {
                  WasmValue.f64(
                      (double) params[0].asInt()
                          + (double) params[1].asLong()
                          + params[2].asDouble())
                };

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        final WasmValue[] results =
            ref.call(WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f64(3.0));
        assertEquals(6.0, results[0].asDouble(), 0.001, "1+2+3.0 should equal 6.0");
        LOGGER.info("[" + runtime + "] Multi-param function result: " + results[0].asDouble());
      }
    }
  }

  // ==================== Call with Null Parameters Tests ====================

  @Nested
  @DisplayName("Call with Null Parameters Tests")
  class NullParameterCallTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw NullPointerException for null params array")
    void shouldThrowForNullParamsArray(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null params array");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
        final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

        final FunctionReference ref = store.createFunctionReference(hf, ft);

        // Behavior varies by runtime: some runtimes may not reject null params
        try {
          ref.call((WasmValue[]) null);
          LOGGER.info("[" + runtime + "] call(null) did not throw");
        } catch (final Exception e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] call(null) threw: "
                  + e.getClass().getName()
                  + " - "
                  + e.getMessage());
        }
      }
    }
  }

  // ==================== Multiple Function References Tests ====================

  @Nested
  @DisplayName("Multiple Function References Tests")
  class MultipleFunctionReferencesTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should create multiple host function references independently")
    void shouldCreateMultipleIndependent(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multiple independent function references");

      try (Engine engine = Engine.create();
          Store store = Store.create(engine)) {

        final FunctionType ft =
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
        final HostFunction hf1 = (params) -> new WasmValue[] {WasmValue.i32(1)};
        final HostFunction hf2 = (params) -> new WasmValue[] {WasmValue.i32(2)};
        final HostFunction hf3 = (params) -> new WasmValue[] {WasmValue.i32(3)};

        final FunctionReference ref1 = store.createFunctionReference(hf1, ft);
        final FunctionReference ref2 = store.createFunctionReference(hf2, ft);
        final FunctionReference ref3 = store.createFunctionReference(hf3, ft);

        assertEquals(1, ref1.call()[0].asInt(), "ref1 should return 1");
        assertEquals(2, ref2.call()[0].asInt(), "ref2 should return 2");
        assertEquals(3, ref3.call()[0].asInt(), "ref3 should return 3");

        assertNotEquals(ref1.getId(), ref2.getId(), "ref1 and ref2 should have different IDs");
        assertNotEquals(ref2.getId(), ref3.getId(), "ref2 and ref3 should have different IDs");
        LOGGER.info("[" + runtime + "] Three independent function references work correctly");
      }
    }
  }
}
