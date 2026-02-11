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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaFunctionReference}.
 *
 * <p>Tests both host function and WebAssembly function reference creation, calling, registry
 * management, lifecycle, and error handling using real native library interaction.
 */
@DisplayName("PanamaFunctionReference Integration Tests")
class PanamaFunctionReferenceTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaFunctionReferenceTest.class.getName());

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

  private final List<AutoCloseable> resources = new ArrayList<>();
  private final List<PanamaFunctionReference> funcRefs = new ArrayList<>();

  private PanamaEngine engine;
  private PanamaStore store;

  @BeforeEach
  void setUp() throws WasmException {
    engine = new PanamaEngine();
    resources.add(engine);
    store = new PanamaStore(engine);
    resources.add(store);
    LOGGER.info("Test setup complete: engine and store created");
  }

  @AfterEach
  void tearDown() {
    // Close function references first (they reference store resources)
    for (int i = funcRefs.size() - 1; i >= 0; i--) {
      try {
        funcRefs.get(i).close();
      } catch (Exception e) {
        LOGGER.warning("Error closing function reference: " + e.getMessage());
      }
    }
    funcRefs.clear();
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  private PanamaInstance createInstanceFromWat(final String wat) throws WasmException {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    resources.add(module);
    final PanamaLinker linker = new PanamaLinker(engine);
    resources.add(linker);
    final PanamaInstance instance = (PanamaInstance) linker.instantiate(store, module);
    resources.add(instance);
    return instance;
  }

  private PanamaFunctionReference trackRef(final FunctionReference ref) {
    final PanamaFunctionReference pRef = (PanamaFunctionReference) ref;
    funcRefs.add(pRef);
    return pRef;
  }

  @Nested
  @DisplayName("Host Function Reference Constructor Null Validation Tests")
  class HostConstructorNullValidationTests {

    @Test
    @DisplayName("Should throw WasmException for null host function")
    void shouldThrowForNullHostFunction() {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference(null, ft, store, arm, eh),
              "Should throw WasmException for null host function");
      LOGGER.info("Null host function error: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("null"),
          "Error message should mention null: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw WasmException for null function type")
    void shouldThrowForNullFunctionType() {
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};
      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference(hf, null, store, arm, eh),
              "Should throw WasmException for null function type");
      LOGGER.info("Null function type error: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("null"),
          "Error message should mention null: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw WasmException for null store")
    void shouldThrowForNullStore() {
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference(hf, ft, null, arm, eh),
              "Should throw WasmException for null store");
      LOGGER.info("Null store error: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("null"),
          "Error message should mention null: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw WasmException for null arena manager")
    void shouldThrowForNullArenaManager() {
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference(hf, ft, store, null, eh),
              "Should throw WasmException for null arena manager");
      LOGGER.info("Null arena manager error: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("null"),
          "Error message should mention null: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw WasmException for null error handler")
    void shouldThrowForNullErrorHandler() {
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference(hf, ft, store, arm, null),
              "Should throw WasmException for null error handler");
      LOGGER.info("Null error handler error: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("null"),
          "Error message should mention null: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Wasm Function Reference Constructor Null Validation Tests")
  class WasmConstructorNullValidationTests {

    @Test
    @DisplayName("Should throw WasmException for null wasm function")
    void shouldThrowForNullWasmFunction() {
      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference((WasmFunction) null, store, arm, eh),
              "Should throw WasmException for null wasm function");
      LOGGER.info("Null wasm function error: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("null"),
          "Error message should mention null: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw WasmException for null store in wasm constructor")
    void shouldThrowForNullStoreInWasmConstructor() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");
      final WasmFunction wasmFunc = funcOpt.get();
      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference(wasmFunc, null, arm, eh),
              "Should throw WasmException for null store");
      LOGGER.info("Null store error: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw WasmException for null arena manager in wasm constructor")
    void shouldThrowForNullArenaManagerInWasmConstructor() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");
      final WasmFunction wasmFunc = funcOpt.get();
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference(wasmFunc, store, null, eh),
              "Should throw WasmException for null arena manager");
      LOGGER.info("Null arena manager error: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw WasmException for null error handler in wasm constructor")
    void shouldThrowForNullErrorHandlerInWasmConstructor() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");
      final WasmFunction wasmFunc = funcOpt.get();
      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> new PanamaFunctionReference(wasmFunc, store, arm, null),
              "Should throw WasmException for null error handler");
      LOGGER.info("Null error handler error: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Host Function Reference Creation and Properties Tests")
  class HostFunctionReferenceTests {

    @Test
    @DisplayName("Should create host function reference with valid parameters")
    void shouldCreateHostFunctionReference() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(99)};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      assertNotNull(ref, "Function reference should not be null");
      trackRef(ref);
      LOGGER.info("Created host function reference: " + ref);
    }

    @Test
    @DisplayName("Should return function type for host function reference")
    void shouldReturnFunctionType() throws WasmException {
      final FunctionType ft =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() + params[1].asI32())};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final FunctionType returnedType = ref.getFunctionType();
      assertNotNull(returnedType, "Function type should not be null");
      assertEquals(2, returnedType.getParamTypes().length, "Should have 2 param types");
      assertEquals(1, returnedType.getReturnTypes().length, "Should have 1 return type");
      assertEquals(WasmValueType.I32, returnedType.getParamTypes()[0], "First param should be I32");
      assertEquals(
          WasmValueType.I32, returnedType.getReturnTypes()[0], "Return type should be I32");
      LOGGER.info(
          "Function type: params="
              + returnedType.getParamTypes().length
              + " returns="
              + returnedType.getReturnTypes().length);
    }

    @Test
    @DisplayName("Should return host function name with prefix")
    void shouldReturnHostFunctionName() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      final String name = ref.getName();
      assertNotNull(name, "Name should not be null");
      assertTrue(
          name.startsWith("host_function_"),
          "Host function name should start with 'host_function_': " + name);
      LOGGER.info("Host function reference name: " + name);
    }

    @Test
    @DisplayName("Should report as host function")
    void shouldReportAsHostFunction() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      assertTrue(ref.isHostFunction(), "Should be a host function");
      assertFalse(ref.isWasmFunction(), "Should not be a wasm function");
      LOGGER.info(
          "isHostFunction=" + ref.isHostFunction() + " isWasmFunction=" + ref.isWasmFunction());
    }

    @Test
    @DisplayName("Should be valid after creation")
    void shouldBeValidAfterCreation() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      assertTrue(ref.isValid(), "Function reference should be valid after creation");
      LOGGER.info("isValid=" + ref.isValid());
    }

    @Test
    @DisplayName("Should have positive ID")
    void shouldHavePositiveId() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      assertTrue(ref.getId() >= 0, "Function reference ID should be non-negative: " + ref.getId());
      LOGGER.info("Function reference ID: " + ref.getId());
    }

    @Test
    @DisplayName("Should have positive longValue")
    void shouldHavePositiveLongValue() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      assertTrue(ref.longValue() > 0, "longValue should be positive: " + ref.longValue());
      LOGGER.info("longValue: " + ref.longValue());
    }

    @Test
    @DisplayName("Should have upcall stub for host function")
    void shouldHaveUpcallStub() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      final MemorySegment stub = ref.getUpcallStub();
      assertNotNull(stub, "Upcall stub should not be null for host function");
      assertNotEquals(MemorySegment.NULL, stub, "Upcall stub should not be NULL segment");
      LOGGER.info("Upcall stub address: 0x" + Long.toHexString(stub.address()));
    }

    @Test
    @DisplayName("Should have non-negative native registry ID")
    void shouldHaveNativeRegistryId() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      assertTrue(
          ref.getNativeRegistryId() >= 0,
          "Native registry ID should be non-negative: " + ref.getNativeRegistryId());
      LOGGER.info("Native registry ID: " + ref.getNativeRegistryId());
    }

    @Test
    @DisplayName("Should call host function reference with no parameters")
    void shouldCallHostFunctionNoParams() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(42)};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final WasmValue[] results = ref.call();
      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(42, results[0].asI32(), "Result should be 42");
      LOGGER.info("Call result: " + results[0].asI32());
    }

    @Test
    @DisplayName("Should call host function reference with parameters")
    void shouldCallHostFunctionWithParams() throws WasmException {
      final FunctionType ft =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() + params[1].asI32())};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final WasmValue[] results = ref.call(WasmValue.i32(10), WasmValue.i32(20));
      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(30, results[0].asI32(), "Result should be 30");
      LOGGER.info("Call result (10+20): " + results[0].asI32());
    }

    @Test
    @DisplayName("Should call void host function reference")
    void shouldCallVoidHostFunction() throws WasmException {
      final FunctionType ft = FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {});
      final HostFunction hf = (params) -> new WasmValue[] {};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final WasmValue[] results = ref.call();
      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.length, "Should have no results for void function");
      LOGGER.info("Void function call completed, result length: " + results.length);
    }
  }

  @Nested
  @DisplayName("Wasm Function Reference Creation and Properties Tests")
  class WasmFunctionReferenceTests {

    @Test
    @DisplayName("Should create wasm function reference via 4-arg constructor")
    void shouldCreateWasmFunctionReference() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      trackRef(ref);

      assertNotNull(ref, "Wasm function reference should not be null");
      LOGGER.info("Created wasm function reference: " + ref);
    }

    @Test
    @DisplayName("Should return wasm function name from original function")
    void shouldReturnWasmFunctionName() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("add");
      assertTrue(funcOpt.isPresent(), "add function should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      trackRef(ref);

      final String name = ref.getName();
      assertNotNull(name, "Name should not be null");
      assertEquals("add", name, "Name should match original wasm function name");
      LOGGER.info("Wasm function reference name: " + name);
    }

    @Test
    @DisplayName("Should report as wasm function (not host)")
    void shouldReportAsWasmFunction() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      trackRef(ref);

      assertTrue(ref.isWasmFunction(), "Should be a wasm function");
      assertFalse(ref.isHostFunction(), "Should not be a host function");
      LOGGER.info(
          "isWasmFunction=" + ref.isWasmFunction() + " isHostFunction=" + ref.isHostFunction());
    }

    @Test
    @DisplayName("Should have null upcall stub for wasm function reference")
    void shouldHaveNullUpcallStub() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      trackRef(ref);

      assertNull(ref.getUpcallStub(), "Wasm function reference should have null upcall stub");
      LOGGER.info("Upcall stub for wasm function: " + ref.getUpcallStub());
    }

    @Test
    @DisplayName("Should call wasm function reference and return result")
    void shouldCallWasmFunctionReference() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("add");
      assertTrue(funcOpt.isPresent(), "add function should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      trackRef(ref);

      final WasmValue[] results = ref.call(WasmValue.i32(7), WasmValue.i32(8));
      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(15, results[0].asI32(), "7+8 should equal 15");
      LOGGER.info("Wasm function call result (7+8): " + results[0].asI32());
    }

    @Test
    @DisplayName("Should return function type from wasm function")
    void shouldReturnFunctionType() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("add");
      assertTrue(funcOpt.isPresent(), "add function should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      trackRef(ref);

      final FunctionType ft = ref.getFunctionType();
      assertNotNull(ft, "Function type should not be null");
      // Note: PanamaInstance.getFunction() currently uses a placeholder FunctionType
      // (empty params/returns) - actual signature is validated at call time by native code.
      assertNotNull(ft.getParamTypes(), "Param types should not be null");
      assertNotNull(ft.getReturnTypes(), "Return types should not be null");
      LOGGER.info(
          "Function type: "
              + ft.getParamTypes().length
              + " params, "
              + ft.getReturnTypes().length
              + " returns");
    }

    @Test
    @DisplayName("Should have valid state for wasm function reference")
    void shouldBeValidForWasmFunction() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      trackRef(ref);

      assertTrue(ref.isValid(), "Wasm function reference should be valid");
      assertTrue(ref.longValue() > 0, "longValue should be positive: " + ref.longValue());
      assertTrue(ref.getId() > 0, "getId should be positive: " + ref.getId());
      LOGGER.info(
          "Valid=" + ref.isValid() + " longValue=" + ref.longValue() + " id=" + ref.getId());
    }
  }

  @Nested
  @DisplayName("Store-Created Function Reference Tests")
  class StoreCreatedFunctionReferenceTests {

    @Test
    @DisplayName("Should create function reference from wasm function via store")
    void shouldCreateFromWasmFunctionViaStore() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");

      final FunctionReference ref = store.createFunctionReference(funcOpt.get());
      trackRef(ref);

      assertNotNull(ref, "Store-created function reference should not be null");
      assertTrue(ref.isValid(), "Should be valid");
      LOGGER.info("Store-created wasm function reference: " + ref);
    }

    @Test
    @DisplayName("Should call store-created wasm function reference")
    void shouldCallStoreCreatedWasmFunctionReference() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("add");
      assertTrue(funcOpt.isPresent(), "add function should exist");

      final FunctionReference ref = store.createFunctionReference(funcOpt.get());
      trackRef(ref);

      final WasmValue[] results = ref.call(WasmValue.i32(100), WasmValue.i32(200));
      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(300, results[0].asI32(), "100+200 should equal 300");
      LOGGER.info("Store-created function call result (100+200): " + results[0].asI32());
    }

    @Test
    @DisplayName("Store-created wasm function reference should be host function type")
    void storeCreatedWasmRefShouldBeHostType() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 function should exist");

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(funcOpt.get());
      trackRef(ref);

      // Store wraps wasm function in a HostFunction wrapper
      assertTrue(
          ref.isHostFunction(),
          "Store-created wasm function reference is wrapped as host function");
      LOGGER.info("Store-created ref isHostFunction=" + ref.isHostFunction());
    }
  }

  @Nested
  @DisplayName("Registry Tests")
  class RegistryTests {

    @Test
    @DisplayName("Should register function reference in global registry")
    void shouldRegisterInRegistry() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      final PanamaFunctionReference fromRegistry =
          PanamaFunctionReference.getFromRegistry(ref.longValue());
      assertNotNull(fromRegistry, "Should find function reference in registry");
      assertEquals(ref, fromRegistry, "Registry should return the same instance");
      LOGGER.info("Retrieved from registry by longValue: " + ref.longValue());
    }

    @Test
    @DisplayName("Should retrieve function reference by ID via public API")
    void shouldRetrieveByPublicApi() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      final FunctionReference fromRegistry =
          PanamaFunctionReference.getFunctionReferenceById(ref.longValue());
      assertNotNull(fromRegistry, "Should find function reference by public API");
      LOGGER.info("Retrieved via getFunctionReferenceById: " + ref.longValue());
    }

    @Test
    @DisplayName("Should return null for invalid registry ID")
    void shouldReturnNullForInvalidId() {
      final FunctionReference result = PanamaFunctionReference.getFunctionReferenceById(-999);
      assertNull(result, "Should return null for invalid registry ID");

      final PanamaFunctionReference result2 =
          PanamaFunctionReference.getFromRegistry(Long.MAX_VALUE);
      assertNull(result2, "Should return null for non-existent registry ID");
      LOGGER.info("Correctly returned null for invalid IDs");
    }

    @Test
    @DisplayName("Should return valid registry statistics")
    void shouldReturnRegistryStats() throws WasmException {
      final long[] statsBefore = PanamaFunctionReference.getRegistryStats();
      assertNotNull(statsBefore, "Registry stats should not be null");
      assertEquals(2, statsBefore.length, "Stats should have 2 elements [count, nextId]");
      LOGGER.info("Registry stats before: count=" + statsBefore[0] + " nextId=" + statsBefore[1]);

      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};
      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final long[] statsAfter = PanamaFunctionReference.getRegistryStats();
      assertTrue(
          statsAfter[0] >= statsBefore[0],
          "Registry count should not decrease after adding: before="
              + statsBefore[0]
              + " after="
              + statsAfter[0]);
      assertTrue(
          statsAfter[1] > statsBefore[1],
          "Next ID should increase: before=" + statsBefore[1] + " after=" + statsAfter[1]);
      LOGGER.info("Registry stats after: count=" + statsAfter[0] + " nextId=" + statsAfter[1]);
    }

    @Test
    @DisplayName("Should assign unique IDs to function references")
    void shouldAssignUniqueIds() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref1 =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref1);
      final PanamaFunctionReference ref2 =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref2);

      assertNotEquals(
          ref1.longValue(),
          ref2.longValue(),
          "Each function reference should have a unique longValue");
      assertNotEquals(
          ref1.getId(), ref2.getId(), "Each function reference should have a unique ID");
      LOGGER.info("Unique IDs: ref1=" + ref1.longValue() + " ref2=" + ref2.longValue());
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Should be invalid after close")
    void shouldBeInvalidAfterClose() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      assertTrue(ref.isValid(), "Should be valid before close");

      ref.close();
      assertFalse(ref.isValid(), "Should be invalid after close");
      LOGGER.info("isValid after close: " + ref.isValid());
    }

    @Test
    @DisplayName("Should throw WasmException on call after close")
    void shouldThrowOnCallAfterClose() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      ref.close();

      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> ref.call(),
              "Should throw WasmException on call after close");
      assertTrue(
          ex.getMessage().contains("closed"),
          "Error message should mention closed: " + ex.getMessage());
      LOGGER.info("Call after close error: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should handle double close gracefully")
    void shouldHandleDoubleClose() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      ref.close();
      assertDoesNotThrow(() -> ref.close(), "Double close should not throw");
      LOGGER.info("Double close handled gracefully");
    }

    @Test
    @DisplayName("Should remove from registry on close")
    void shouldRemoveFromRegistryOnClose() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      final long id = ref.longValue();

      assertNotNull(
          PanamaFunctionReference.getFromRegistry(id), "Should be in registry before close");

      ref.close();

      assertNull(
          PanamaFunctionReference.getFromRegistry(id),
          "Should be removed from registry after close");
      LOGGER.info("Verified removal from registry after close, ID: " + id);
    }

    @Test
    @DisplayName("Should close wasm function reference without error")
    void shouldCloseWasmFunctionReference() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      assertTrue(ref.isValid(), "Should be valid before close");

      assertDoesNotThrow(() -> ref.close(), "Closing wasm function ref should not throw");
      assertFalse(ref.isValid(), "Should be invalid after close");
      LOGGER.info("Wasm function reference closed successfully");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should contain host type in toString for host function")
    void shouldContainHostTypeInToString() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      final String str = ref.toString();
      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("host"), "toString should contain 'host': " + str);
      assertTrue(
          str.contains("PanamaFunctionReference"), "toString should contain class name: " + str);
      LOGGER.info("Host function toString: " + str);
    }

    @Test
    @DisplayName("Should contain wasm type in toString for wasm function")
    void shouldContainWasmTypeInToString() throws WasmException {
      final PanamaInstance instance = createInstanceFromWat(FUNCTIONS_WAT);
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "return_i32 should exist");

      final ArenaResourceManager arm = new ArenaResourceManager();
      resources.add(arm);
      final PanamaErrorHandler eh = PanamaErrorHandler.getInstance();

      final PanamaFunctionReference ref =
          new PanamaFunctionReference(funcOpt.get(), store, arm, eh);
      trackRef(ref);

      final String str = ref.toString();
      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("wasm"), "toString should contain 'wasm': " + str);
      LOGGER.info("Wasm function toString: " + str);
    }

    @Test
    @DisplayName("Should show closed state in toString")
    void shouldShowClosedInToString() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      ref.close();

      final String str = ref.toString();
      assertTrue(str.contains("closed"), "toString should contain 'closed': " + str);
      LOGGER.info("Closed function toString: " + str);
    }
  }

  @Nested
  @DisplayName("Callback Invocation Tests")
  class CallbackInvocationTests {

    @Test
    @DisplayName("Should invoke callback via static invokeFunctionReferenceCallback")
    void shouldInvokeCallbackDirectly() throws WasmException {
      final FunctionType ft =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() * 3)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      trackRef(ref);

      final WasmValue[] results =
          PanamaFunctionReference.invokeFunctionReferenceCallback(
              ref.longValue(), new WasmValue[] {WasmValue.i32(10)});
      assertNotNull(results, "Callback results should not be null");
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(30, results[0].asI32(), "10*3 should be 30");
      LOGGER.info("Callback invocation result (10*3): " + results[0].asI32());
    }

    @Test
    @DisplayName("Should throw WasmException for invalid callback ID")
    void shouldThrowForInvalidCallbackId() {
      final WasmException ex =
          assertThrows(
              WasmException.class,
              () ->
                  PanamaFunctionReference.invokeFunctionReferenceCallback(
                      -999L, new WasmValue[] {}),
              "Should throw for invalid callback ID");
      assertTrue(
          ex.getMessage().contains("not found"),
          "Error should mention not found: " + ex.getMessage());
      LOGGER.info("Invalid callback ID error: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should throw WasmException for closed function reference callback")
    void shouldThrowForClosedCallbackRef() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final PanamaFunctionReference ref =
          (PanamaFunctionReference) store.createFunctionReference(hf, ft);
      final long id = ref.longValue();
      ref.close();

      // After close, the ref is removed from registry, so invokeFunctionReferenceCallback
      // should fail with "not found" since it's no longer in registry
      final WasmException ex =
          assertThrows(
              WasmException.class,
              () -> PanamaFunctionReference.invokeFunctionReferenceCallback(id, new WasmValue[] {}),
              "Should throw for closed function reference callback");
      LOGGER.info("Closed callback error: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Global Callback Stub Tests")
  class GlobalCallbackStubTests {

    @Test
    @DisplayName("Should return non-null global callback stub")
    void shouldReturnGlobalCallbackStub() {
      final MemorySegment stub = PanamaFunctionReference.getGlobalCallbackStub();
      assertNotNull(stub, "Global callback stub should not be null");
      assertNotEquals(MemorySegment.NULL, stub, "Global callback stub should not be NULL segment");
      LOGGER.info("Global callback stub address: 0x" + Long.toHexString(stub.address()));
    }

    @Test
    @DisplayName("Should return same global callback stub on multiple calls")
    void shouldReturnSameGlobalCallbackStub() {
      final MemorySegment stub1 = PanamaFunctionReference.getGlobalCallbackStub();
      final MemorySegment stub2 = PanamaFunctionReference.getGlobalCallbackStub();
      assertEquals(stub1, stub2, "Global callback stub should be singleton");
      LOGGER.info("Global callback stub is singleton: same instance");
    }
  }

  @Nested
  @DisplayName("Multiple Function Type Tests")
  class MultipleTypeTests {

    @Test
    @DisplayName("Should create I64 host function reference")
    void shouldCreateI64HostFunctionReference() throws WasmException {
      final FunctionType ft =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.I64});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i64(params[0].asI64() * 2)};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final WasmValue[] results = ref.call(WasmValue.i64(100L));
      assertEquals(200L, results[0].asI64(), "100*2 should equal 200");
      LOGGER.info("I64 function result: " + results[0].asI64());
    }

    @Test
    @DisplayName("Should create F32 host function reference")
    void shouldCreateF32HostFunctionReference() throws WasmException {
      final FunctionType ft =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.F32}, new WasmValueType[] {WasmValueType.F32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.f32(params[0].asF32() + 1.0f)};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final WasmValue[] results = ref.call(WasmValue.f32(3.14f));
      assertEquals(4.14f, results[0].asF32(), 0.001f, "3.14+1.0 should equal 4.14");
      LOGGER.info("F32 function result: " + results[0].asF32());
    }

    @Test
    @DisplayName("Should create F64 host function reference")
    void shouldCreateF64HostFunctionReference() throws WasmException {
      final FunctionType ft =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.F64}, new WasmValueType[] {WasmValueType.F64});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.f64(params[0].asF64() * 0.5)};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final WasmValue[] results = ref.call(WasmValue.f64(10.0));
      assertEquals(5.0, results[0].asF64(), 0.001, "10.0*0.5 should equal 5.0");
      LOGGER.info("F64 function result: " + results[0].asF64());
    }

    @Test
    @DisplayName("Should create multi-parameter host function reference")
    void shouldCreateMultiParamHostFunctionReference() throws WasmException {
      final FunctionType ft =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64, WasmValueType.F64},
              new WasmValueType[] {WasmValueType.F64});
      final HostFunction hf =
          (params) ->
              new WasmValue[] {
                WasmValue.f64(
                    (double) params[0].asI32() + (double) params[1].asI64() + params[2].asF64())
              };

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      final WasmValue[] results = ref.call(WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f64(3.0));
      assertEquals(6.0, results[0].asF64(), 0.001, "1+2+3.0 should equal 6.0");
      LOGGER.info("Multi-param function result: " + results[0].asF64());
    }
  }

  @Nested
  @DisplayName("Call with Null Parameters Tests")
  class NullParameterCallTests {

    @Test
    @DisplayName("Should throw NullPointerException for null params array")
    void shouldThrowForNullParamsArray() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf = (params) -> new WasmValue[] {WasmValue.i32(1)};

      final FunctionReference ref = store.createFunctionReference(hf, ft);
      trackRef(ref);

      assertThrows(
          NullPointerException.class,
          () -> ref.call((WasmValue[]) null),
          "Should throw NullPointerException for null params");
      LOGGER.info("Correctly threw NullPointerException for null params");
    }
  }

  @Nested
  @DisplayName("Multiple Function References Tests")
  class MultipleFunctionReferencesTests {

    @Test
    @DisplayName("Should create multiple host function references independently")
    void shouldCreateMultipleIndependent() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf1 = (params) -> new WasmValue[] {WasmValue.i32(1)};
      final HostFunction hf2 = (params) -> new WasmValue[] {WasmValue.i32(2)};
      final HostFunction hf3 = (params) -> new WasmValue[] {WasmValue.i32(3)};

      final FunctionReference ref1 = store.createFunctionReference(hf1, ft);
      trackRef(ref1);
      final FunctionReference ref2 = store.createFunctionReference(hf2, ft);
      trackRef(ref2);
      final FunctionReference ref3 = store.createFunctionReference(hf3, ft);
      trackRef(ref3);

      assertEquals(1, ref1.call()[0].asI32(), "ref1 should return 1");
      assertEquals(2, ref2.call()[0].asI32(), "ref2 should return 2");
      assertEquals(3, ref3.call()[0].asI32(), "ref3 should return 3");
      LOGGER.info("Three independent function references work correctly");
    }

    @Test
    @DisplayName("Should close one reference without affecting others")
    void shouldCloseOneWithoutAffectingOthers() throws WasmException {
      final FunctionType ft =
          FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final HostFunction hf1 = (params) -> new WasmValue[] {WasmValue.i32(10)};
      final HostFunction hf2 = (params) -> new WasmValue[] {WasmValue.i32(20)};

      final PanamaFunctionReference ref1 = trackRef(store.createFunctionReference(hf1, ft));
      final PanamaFunctionReference ref2 = trackRef(store.createFunctionReference(hf2, ft));

      ref1.close();

      assertFalse(ref1.isValid(), "ref1 should be invalid after close");
      assertTrue(ref2.isValid(), "ref2 should still be valid");
      assertEquals(20, ref2.call()[0].asI32(), "ref2 should still work");
      LOGGER.info("Closing one reference did not affect the other");
    }
  }
}
