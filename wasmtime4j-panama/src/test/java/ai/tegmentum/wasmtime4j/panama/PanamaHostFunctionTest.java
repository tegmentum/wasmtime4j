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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaHostFunction}.
 *
 * <p>Tests host function creation, upcall stub generation, lifecycle management, and callback
 * registration using real Panama FFI interactions.
 */
@DisplayName("PanamaHostFunction Integration Tests")
class PanamaHostFunctionTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaHostFunctionTest.class.getName());

  private final List<AutoCloseable> resources = new ArrayList<>();
  private final List<PanamaHostFunction> hostFunctions = new ArrayList<>();
  private PanamaEngine engine;
  private PanamaStore store;
  private ArenaResourceManager arenaManager;
  private PanamaErrorHandler errorHandler;

  @BeforeEach
  void setUp() throws Exception {
    engine = new PanamaEngine();
    resources.add(engine);

    store = new PanamaStore(engine);
    resources.add(store);

    arenaManager = store.getResourceManager();
    errorHandler = PanamaErrorHandler.getInstance();
    LOGGER.info("Set up test fixtures: engine, store, arenaManager, errorHandler");
  }

  @AfterEach
  void tearDown() {
    // Close host functions first (not AutoCloseable)
    for (int i = hostFunctions.size() - 1; i >= 0; i--) {
      try {
        hostFunctions.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close host function: " + e.getMessage());
      }
    }
    hostFunctions.clear();
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  /** Creates a simple (i32) -> (i32) function type. */
  private FunctionType createI32ToI32Type() {
    return FunctionType.of(
        new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
  }

  /** Creates a void () -> () function type. */
  private FunctionType createVoidType() {
    return FunctionType.of(new WasmValueType[0], new WasmValueType[0]);
  }

  /** Creates a (i32, i32) -> (i32) function type. */
  private FunctionType createI32I32ToI32Type() {
    return FunctionType.of(
        new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
        new WasmValueType[] {WasmValueType.I32});
  }

  /** Creates a callback that doubles its first i32 parameter. */
  private PanamaHostFunction.HostFunctionCallback createDoubleCallback() {
    return params -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)};
  }

  /** Creates a void callback. */
  private PanamaHostFunction.HostFunctionCallback createVoidCallback() {
    return params -> new WasmValue[0];
  }

  /** Creates and tracks a PanamaHostFunction for proper cleanup. */
  private PanamaHostFunction createTrackedHostFunction(
      final String name,
      final FunctionType type,
      final PanamaHostFunction.HostFunctionCallback callback)
      throws Exception {
    final PanamaHostFunction func =
        new PanamaHostFunction(name, type, callback, null, store, arenaManager, errorHandler);
    hostFunctions.add(func);
    return func;
  }

  @Nested
  @DisplayName("Constructor Null Validation Tests")
  class ConstructorNullValidationTests {

    @Test
    @DisplayName("Should throw for null function name")
    void shouldThrowForNullFunctionName() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  null,
                  createI32ToI32Type(),
                  createDoubleCallback(),
                  null,
                  store,
                  arenaManager,
                  errorHandler),
          "Should throw NullPointerException for null functionName");
      LOGGER.info("Correctly threw for null function name");
    }

    @Test
    @DisplayName("Should throw for null function type")
    void shouldThrowForNullFunctionType() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  "test", null, createDoubleCallback(), null, store, arenaManager, errorHandler),
          "Should throw NullPointerException for null functionType");
      LOGGER.info("Correctly threw for null function type");
    }

    @Test
    @DisplayName("Should throw for null callback")
    void shouldThrowForNullCallback() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  "test", createI32ToI32Type(), null, null, store, arenaManager, errorHandler),
          "Should throw NullPointerException for null callback");
      LOGGER.info("Correctly threw for null callback");
    }

    @Test
    @DisplayName("Should throw for null arena manager")
    void shouldThrowForNullArenaManager() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  "test",
                  createI32ToI32Type(),
                  createDoubleCallback(),
                  null,
                  store,
                  null,
                  errorHandler),
          "Should throw NullPointerException for null arenaManager");
      LOGGER.info("Correctly threw for null arena manager");
    }

    @Test
    @DisplayName("Should throw for null error handler")
    void shouldThrowForNullErrorHandler() {
      assertThrows(
          NullPointerException.class,
          () ->
              new PanamaHostFunction(
                  "test",
                  createI32ToI32Type(),
                  createDoubleCallback(),
                  null,
                  store,
                  arenaManager,
                  null),
          "Should throw NullPointerException for null errorHandler");
      LOGGER.info("Correctly threw for null error handler");
    }
  }

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("Should create host function with store")
    void shouldCreateHostFunctionWithStore() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("addDouble", createI32ToI32Type(), createDoubleCallback());
      assertNotNull(func, "Host function should not be null");
      assertFalse(func.isClosed(), "Should not be closed after creation");
      LOGGER.info("Created host function with store: " + func.getName());
    }

    @Test
    @DisplayName("Should create host function without store")
    void shouldCreateHostFunctionWithoutStore() throws Exception {
      final PanamaHostFunction func =
          new PanamaHostFunction(
              "noStore",
              createI32ToI32Type(),
              createDoubleCallback(),
              null,
              null,
              arenaManager,
              errorHandler);
      hostFunctions.add(func);
      assertNotNull(func, "Host function should not be null");
      LOGGER.info("Created host function without store: " + func.getName());
    }

    @Test
    @DisplayName("Should create void host function")
    void shouldCreateVoidHostFunction() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("voidFunc", createVoidType(), createVoidCallback());
      assertNotNull(func, "Void host function should not be null");
      LOGGER.info("Created void host function: " + func.getName());
    }

    @Test
    @DisplayName("Should create multi-param host function")
    void shouldCreateMultiParamHostFunction() throws Exception {
      final PanamaHostFunction.HostFunctionCallback addCallback =
          params -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};
      final PanamaHostFunction func =
          createTrackedHostFunction("add", createI32I32ToI32Type(), addCallback);
      assertNotNull(func, "Multi-param host function should not be null");
      LOGGER.info("Created multi-param host function: " + func.getName());
    }
  }

  @Nested
  @DisplayName("Function Interface Tests")
  class FunctionInterfaceTests {

    @Test
    @DisplayName("Should return correct function type")
    void shouldReturnCorrectFunctionType() throws Exception {
      final FunctionType type = createI32ToI32Type();
      final PanamaHostFunction func =
          createTrackedHostFunction("typed", type, createDoubleCallback());
      assertEquals(type, func.getFunctionType(), "Function type should match constructor param");
      LOGGER.info("Function type: " + func.getFunctionType());
    }

    @Test
    @DisplayName("Should return correct name")
    void shouldReturnCorrectName() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("myHostFunc", createI32ToI32Type(), createDoubleCallback());
      assertEquals("myHostFunc", func.getName(), "Name should match constructor param");
      LOGGER.info("Function name: " + func.getName());
    }

    @Test
    @DisplayName("Should throw ValidationException on direct call")
    void shouldThrowOnDirectCall() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("noCall", createI32ToI32Type(), createDoubleCallback());

      assertThrows(
          ValidationException.class,
          () -> func.call(WasmValue.i32(42)),
          "Direct call should throw ValidationException");
      LOGGER.info("Correctly threw ValidationException on direct call");
    }

    @Test
    @DisplayName("Should return failed future on async call")
    void shouldReturnFailedFutureOnAsyncCall() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("noAsync", createI32ToI32Type(), createDoubleCallback());

      final CompletableFuture<WasmValue[]> future = func.callAsync(WasmValue.i32(1));
      assertNotNull(future, "Future should not be null");
      assertTrue(
          future.isCompletedExceptionally(), "Async call future should be completed exceptionally");
      LOGGER.info("Correctly returned failed future on async call");
    }
  }

  @Nested
  @DisplayName("Handle Access Tests")
  class HandleAccessTests {

    @Test
    @DisplayName("Should provide function handle")
    void shouldProvideFunctionHandle() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("handleFunc", createI32ToI32Type(), createDoubleCallback());

      final MemorySegment handle = func.getFunctionHandle();
      assertNotNull(handle, "Function handle should not be null");
      LOGGER.info("Function handle: " + handle);
    }

    @Test
    @DisplayName("Should provide upcall stub")
    void shouldProvideUpcallStub() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("stubFunc", createI32ToI32Type(), createDoubleCallback());

      final MemorySegment stub = func.getUpcallStub();
      assertNotNull(stub, "Upcall stub should not be null");
      LOGGER.info("Upcall stub: " + stub);
    }

    @Test
    @DisplayName("Should provide funcRef ID")
    void shouldProvideFuncRefId() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("refIdFunc", createI32ToI32Type(), createDoubleCallback());

      final long funcRefId = func.getFuncRefId();
      assertTrue(funcRefId > 0, "FuncRef ID should be positive, got: " + funcRefId);
      LOGGER.info("FuncRef ID: " + funcRefId);
    }

    @Test
    @DisplayName("Should throw for handle access after close")
    void shouldThrowForHandleAccessAfterClose() throws Exception {
      final PanamaHostFunction func =
          new PanamaHostFunction(
              "closedFunc",
              createI32ToI32Type(),
              createDoubleCallback(),
              null,
              store,
              arenaManager,
              errorHandler);
      func.close();

      assertThrows(
          IllegalStateException.class,
          func::getFunctionHandle,
          "Should throw IllegalStateException for handle after close");
      assertThrows(
          IllegalStateException.class,
          func::getUpcallStub,
          "Should throw IllegalStateException for stub after close");
      // getFuncRefId() returns the cached ID and does not check closed state
      final long closedId = func.getFuncRefId();
      assertTrue(
          closedId > 0, "FuncRefId should still return cached value after close, got: " + closedId);
      LOGGER.info("Handle/stub throw after close, funcRefId=" + closedId);
    }
  }

  @Nested
  @DisplayName("Upcall Stub Creation Tests")
  class UpcallStubCreationTests {

    @Test
    @DisplayName("Should create stub for i32 -> i32 function")
    void shouldCreateStubForI32ToI32() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("i32toi32", createI32ToI32Type(), createDoubleCallback());
      assertNotNull(func.getUpcallStub(), "i32->i32 stub should not be null");
      LOGGER.info("Created i32->i32 upcall stub");
    }

    @Test
    @DisplayName("Should create stub for void function")
    void shouldCreateStubForVoidFunction() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("void", createVoidType(), createVoidCallback());
      assertNotNull(func.getUpcallStub(), "void stub should not be null");
      LOGGER.info("Created void upcall stub");
    }

    @Test
    @DisplayName("Should create stub for i64 -> i64 function")
    void shouldCreateStubForI64ToI64() throws Exception {
      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.I64});
      final PanamaHostFunction.HostFunctionCallback callback =
          params -> new WasmValue[] {WasmValue.i64(params[0].asLong() * 2)};

      final PanamaHostFunction func = createTrackedHostFunction("i64toi64", type, callback);
      assertNotNull(func.getUpcallStub(), "i64->i64 stub should not be null");
      LOGGER.info("Created i64->i64 upcall stub");
    }

    @Test
    @DisplayName("Should create stub for f32 -> f32 function")
    void shouldCreateStubForF32ToF32() throws Exception {
      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.F32}, new WasmValueType[] {WasmValueType.F32});
      final PanamaHostFunction.HostFunctionCallback callback =
          params -> new WasmValue[] {WasmValue.f32(params[0].asFloat() * 2.0f)};

      final PanamaHostFunction func = createTrackedHostFunction("f32tof32", type, callback);
      assertNotNull(func.getUpcallStub(), "f32->f32 stub should not be null");
      LOGGER.info("Created f32->f32 upcall stub");
    }

    @Test
    @DisplayName("Should create stub for f64 -> f64 function")
    void shouldCreateStubForF64ToF64() throws Exception {
      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.F64}, new WasmValueType[] {WasmValueType.F64});
      final PanamaHostFunction.HostFunctionCallback callback =
          params -> new WasmValue[] {WasmValue.f64(params[0].asDouble() * 2.0)};

      final PanamaHostFunction func = createTrackedHostFunction("f64tof64", type, callback);
      assertNotNull(func.getUpcallStub(), "f64->f64 stub should not be null");
      LOGGER.info("Created f64->f64 upcall stub");
    }

    @Test
    @DisplayName("Should create stub for multi-param function")
    void shouldCreateStubForMultiParamFunction() throws Exception {
      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64, WasmValueType.F64},
              new WasmValueType[] {WasmValueType.F64});
      final PanamaHostFunction.HostFunctionCallback callback =
          params ->
              new WasmValue[] {
                WasmValue.f64(params[0].asInt() + params[1].asLong() + params[2].asDouble())
              };

      final PanamaHostFunction func = createTrackedHostFunction("multiParam", type, callback);
      assertNotNull(func.getUpcallStub(), "Multi-param stub should not be null");
      LOGGER.info("Created multi-param upcall stub");
    }

    @Test
    @DisplayName("Should create stub for multi-return function")
    void shouldCreateStubForMultiReturnFunction() throws Exception {
      final FunctionType type =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32});
      final PanamaHostFunction.HostFunctionCallback callback =
          params -> {
            final int val = params[0].asInt();
            return new WasmValue[] {WasmValue.i32(val), WasmValue.i32(val * 2)};
          };

      final PanamaHostFunction func = createTrackedHostFunction("multiReturn", type, callback);
      assertNotNull(func.getUpcallStub(), "Multi-return stub should not be null");
      LOGGER.info("Created multi-return upcall stub");
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should close without error")
    void shouldCloseWithoutError() throws Exception {
      final PanamaHostFunction func =
          new PanamaHostFunction(
              "closeable",
              createI32ToI32Type(),
              createDoubleCallback(),
              null,
              store,
              arenaManager,
              errorHandler);

      assertDoesNotThrow(func::close, "Close should not throw");
      assertTrue(func.isClosed(), "Should be closed after close()");
      LOGGER.info("Closed host function successfully");
    }

    @Test
    @DisplayName("Should handle double close gracefully")
    void shouldHandleDoubleClose() throws Exception {
      final PanamaHostFunction func =
          new PanamaHostFunction(
              "doubleClose",
              createI32ToI32Type(),
              createDoubleCallback(),
              null,
              store,
              arenaManager,
              errorHandler);

      assertDoesNotThrow(func::close, "First close should not throw");
      assertDoesNotThrow(func::close, "Second close should not throw");
      assertTrue(func.isClosed(), "Should remain closed");
      LOGGER.info("Double close handled gracefully");
    }

    @Test
    @DisplayName("Should report closed state correctly")
    void shouldReportClosedState() throws Exception {
      final PanamaHostFunction func =
          new PanamaHostFunction(
              "stateCheck",
              createI32ToI32Type(),
              createDoubleCallback(),
              null,
              store,
              arenaManager,
              errorHandler);

      assertFalse(func.isClosed(), "Should not be closed initially");
      func.close();
      assertTrue(func.isClosed(), "Should be closed after close()");
      LOGGER.info("Closed state reported correctly");
    }
  }

  @Nested
  @DisplayName("String Representation Tests")
  class StringRepresentationTests {

    @Test
    @DisplayName("Should include name in toString")
    void shouldIncludeNameInToString() throws Exception {
      final PanamaHostFunction func =
          createTrackedHostFunction("stringable", createI32ToI32Type(), createDoubleCallback());
      final String str = func.toString();
      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("stringable"), "toString should contain function name, got: " + str);
      LOGGER.info("toString: " + str);
    }

    @Test
    @DisplayName("Should indicate closed in toString")
    void shouldIndicateClosedInToString() throws Exception {
      final PanamaHostFunction func =
          new PanamaHostFunction(
              "closedStr",
              createI32ToI32Type(),
              createDoubleCallback(),
              null,
              store,
              arenaManager,
              errorHandler);
      func.close();
      final String str = func.toString();
      assertTrue(
          str.toLowerCase().contains("closed"),
          "toString should indicate closed state, got: " + str);
      LOGGER.info("Closed toString: " + str);
    }
  }

  @Nested
  @DisplayName("Multiple Host Functions Tests")
  class MultipleHostFunctionsTests {

    @Test
    @DisplayName("Should create multiple host functions independently")
    void shouldCreateMultipleHostFunctionsIndependently() throws Exception {
      final PanamaHostFunction func1 =
          createTrackedHostFunction("func1", createI32ToI32Type(), createDoubleCallback());
      final PanamaHostFunction func2 =
          createTrackedHostFunction("func2", createVoidType(), createVoidCallback());
      final PanamaHostFunction func3 =
          createTrackedHostFunction(
              "func3",
              createI32I32ToI32Type(),
              params -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())});

      assertEquals("func1", func1.getName());
      assertEquals("func2", func2.getName());
      assertEquals("func3", func3.getName());

      assertTrue(func1.getFuncRefId() != func2.getFuncRefId(), "FuncRef IDs should be unique");
      assertTrue(func2.getFuncRefId() != func3.getFuncRefId(), "FuncRef IDs should be unique");

      LOGGER.info(
          "Created 3 independent host functions with IDs: "
              + func1.getFuncRefId()
              + ", "
              + func2.getFuncRefId()
              + ", "
              + func3.getFuncRefId());
    }

    @Test
    @DisplayName("Should close one function without affecting others")
    void shouldCloseOneWithoutAffectingOthers() throws Exception {
      final PanamaHostFunction func1 =
          createTrackedHostFunction("keepOpen", createI32ToI32Type(), createDoubleCallback());
      final PanamaHostFunction func2 =
          new PanamaHostFunction(
              "toClose",
              createVoidType(),
              createVoidCallback(),
              null,
              store,
              arenaManager,
              errorHandler);

      func2.close();
      assertTrue(func2.isClosed(), "func2 should be closed");
      assertFalse(func1.isClosed(), "func1 should still be open");
      assertNotNull(func1.getUpcallStub(), "func1 stub should still work");
      LOGGER.info("Closed func2, func1 still operational");
    }
  }
}
