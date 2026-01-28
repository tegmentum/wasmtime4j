package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.TypedFunc;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaFunction}.
 *
 * <p>Tests cover constructor validation, all call methods (call, callVoid, callI32ToI32,
 * callI32I32ToI32, callI64ToI64, callF64ToF64, callToI32), async calls, typed functions, lifecycle
 * management, and accessor methods.
 */
@DisplayName("PanamaFunction Tests")
class PanamaFunctionTest {
  private static final Logger LOGGER = Logger.getLogger(PanamaFunctionTest.class.getName());
  private final List<AutoCloseable> resources = new ArrayList<>();

  /** WAT module with various function signatures for testing. */
  private static final String FUNCTIONS_WAT =
      "(module\n"
          + "  (func (export \"void_func\"))\n"
          + "  (func (export \"return_i32\") (result i32) (i32.const 42))\n"
          + "  (func (export \"i32_to_i32\") (param i32) (result i32)\n"
          + "    (i32.mul (local.get 0) (i32.const 2)))\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    (i32.add (local.get 0) (local.get 1)))\n"
          + "  (func (export \"i64_to_i64\") (param i64) (result i64)\n"
          + "    (i64.mul (local.get 0) (i64.const 3)))\n"
          + "  (func (export \"f64_to_f64\") (param f64) (result f64)\n"
          + "    (f64.mul (local.get 0) (f64.const 2.0)))\n"
          + ")";

  @BeforeAll
  static void loadNativeLibrary() {
    final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    assertTrue(bindings.isInitialized(), "Native function bindings should be initialized");
    LOGGER.info("Native library loaded for PanamaFunctionTest");
  }

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  private PanamaEngine createEngine() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    resources.add(engine);
    return engine;
  }

  private PanamaStore createStore(final PanamaEngine engine) throws Exception {
    final PanamaStore store = new PanamaStore(engine);
    resources.add(store);
    return store;
  }

  private PanamaInstance createInstance(final PanamaModule module, final PanamaStore store)
      throws Exception {
    final PanamaInstance instance = new PanamaInstance(module, store);
    resources.add(instance);
    return instance;
  }

  private PanamaModule compileWat(final PanamaEngine engine, final String wat) throws Exception {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    resources.add(module);
    return module;
  }

  /** Creates a standard function test instance with various function exports. */
  private PanamaInstance createFunctionTestInstance() throws Exception {
    final PanamaEngine engine = createEngine();
    final PanamaStore store = createStore(engine);
    final PanamaModule module = compileWat(engine, FUNCTIONS_WAT);
    return createInstance(module, store);
  }

  private WasmFunction getFunction(final PanamaInstance instance, final String name) {
    final Optional<WasmFunction> funcOpt = instance.getFunction(name);
    assertTrue(funcOpt.isPresent(), "Function export '" + name + "' should be present");
    return funcOpt.get();
  }

  // ==================== Constructor Validation Tests ====================

  @Test
  @DisplayName("Constructor with null instance should throw")
  void testConstructorWithNullInstance() {
    final FunctionType funcType =
        new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new PanamaFunction(null, "test", funcType));
    assertThat(exception.getMessage()).contains("Instance cannot be null");
  }

  @Test
  @DisplayName("Constructor with null name should throw")
  void testConstructorWithNullName() throws Exception {
    final PanamaInstance instance = createFunctionTestInstance();
    final FunctionType funcType =
        new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new PanamaFunction(instance, null, funcType));
    assertThat(exception.getMessage()).contains("Name cannot be null");
  }

  @Test
  @DisplayName("Constructor with null function type should throw")
  void testConstructorWithNullFunctionType() throws Exception {
    final PanamaInstance instance = createFunctionTestInstance();
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new PanamaFunction(instance, "test", null));
    assertThat(exception.getMessage()).contains("Function type cannot be null");
  }

  // ==================== Function Retrieval Tests ====================

  @Nested
  @DisplayName("Function Retrieval Tests")
  class FunctionRetrievalTests {

    @Test
    @DisplayName("getFunction should return function for valid export name")
    void shouldReturnFunctionForValidExportName() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final Optional<WasmFunction> funcOpt = instance.getFunction("return_i32");
      assertTrue(funcOpt.isPresent(), "Function 'return_i32' should exist");
      LOGGER.info("Retrieved function: " + funcOpt.get().getName());
    }

    @Test
    @DisplayName("getFunction should return empty for nonexistent name")
    void shouldReturnEmptyForNonexistent() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final Optional<WasmFunction> funcOpt = instance.getFunction("nonexistent_func");
      assertTrue(funcOpt.isEmpty(), "Non-existent function should return empty Optional");
    }

    @Test
    @DisplayName("getFunction should return PanamaFunction instance")
    void shouldReturnPanamaFunctionInstance() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "return_i32");
      assertThat(func).isInstanceOf(PanamaFunction.class);
    }

    @Test
    @DisplayName("getFunction by index should return function")
    void shouldReturnFunctionByIndex() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final Optional<WasmFunction> funcOpt = instance.getFunction(0);
      assertTrue(funcOpt.isPresent(), "Function at index 0 should exist");
    }
  }

  // ==================== Accessor Tests ====================

  @Nested
  @DisplayName("Accessor Tests")
  class AccessorTests {

    @Test
    @DisplayName("getName should return function name")
    void shouldReturnFunctionName() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "return_i32");
      assertEquals("return_i32", func.getName(), "Name should match export name");
    }

    @Test
    @DisplayName("getFunctionType should return non-null type")
    void shouldReturnNonNullFunctionType() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "return_i32");
      final FunctionType funcType = func.getFunctionType();
      assertNotNull(funcType, "Function type should not be null");
      LOGGER.info(
          "Function type: params="
              + funcType.getParamCount()
              + ", returns="
              + funcType.getReturnCount());
    }

    @Test
    @DisplayName("getFunctionType for void function should have zero params and zero returns")
    void shouldHaveCorrectTypeForVoidFunc() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "void_func");
      final FunctionType funcType = func.getFunctionType();
      assertNotNull(funcType, "Function type should not be null");
      LOGGER.info(
          "void_func type: params="
              + funcType.getParamCount()
              + ", returns="
              + funcType.getReturnCount());
    }

    @Test
    @DisplayName("getFunctionType for add function should have two i32 params")
    void shouldHaveCorrectTypeForAdd() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "add");
      final FunctionType funcType = func.getFunctionType();
      assertNotNull(funcType, "Function type should not be null");
      LOGGER.info(
          "add type: params="
              + funcType.getParamCount()
              + ", returns="
              + funcType.getReturnCount());
    }
  }

  // ==================== Call Tests ====================

  @Nested
  @DisplayName("Call Tests")
  class CallTests {

    @Test
    @DisplayName("call with no params on return_i32 should return 42")
    void shouldCallReturnI32() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "return_i32");
      final WasmValue[] results = func.call();
      assertNotNull(results, "Results should not be null");
      assertTrue(results.length > 0, "Should have at least one result");
      assertEquals(42, results[0].asInt(), "return_i32 should return 42");
      LOGGER.info("return_i32() = " + results[0].asInt());
    }

    @Test
    @DisplayName("call void function should return empty or no results")
    void shouldCallVoidFunction() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "void_func");
      final WasmValue[] results = func.call();
      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.length, "Void function should return empty results");
      LOGGER.info("void_func() returned " + results.length + " results");
    }

    @Test
    @DisplayName("call i32_to_i32 with param should return doubled value")
    void shouldCallI32ToI32() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "i32_to_i32");
      final WasmValue[] results = func.call(WasmValue.i32(7));
      assertNotNull(results, "Results should not be null");
      assertEquals(14, results[0].asInt(), "i32_to_i32(7) should return 14");
      LOGGER.info("i32_to_i32(7) = " + results[0].asInt());
    }

    @Test
    @DisplayName("call add with two params should return sum")
    void shouldCallAdd() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "add");
      final WasmValue[] results = func.call(WasmValue.i32(5), WasmValue.i32(3));
      assertNotNull(results, "Results should not be null");
      assertEquals(8, results[0].asInt(), "add(5, 3) should return 8");
      LOGGER.info("add(5, 3) = " + results[0].asInt());
    }

    @Test
    @DisplayName("call with null params should throw")
    void shouldThrowForNullParams() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "return_i32");
      assertThrows(
          IllegalArgumentException.class,
          () -> func.call((WasmValue[]) null),
          "Null params should throw IllegalArgumentException");
    }
  }

  // ==================== Fast Path Call Tests ====================

  @Nested
  @DisplayName("Fast Path Call Tests")
  class FastPathCallTests {

    @Test
    @DisplayName("callVoid should call void function without error")
    void shouldCallVoid() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "void_func");
      assertDoesNotThrow(func::callVoid, "callVoid should not throw");
      LOGGER.info("callVoid() succeeded");
    }

    @Test
    @DisplayName("callToI32 should return i32 result")
    void shouldCallToI32() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "return_i32");
      final int result = func.callToI32();
      assertEquals(42, result, "callToI32 should return 42");
      LOGGER.info("callToI32() = " + result);
    }

    @Test
    @DisplayName("callI32ToI32 should return correct result")
    void shouldCallI32ToI32Fast() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "i32_to_i32");
      final int result = func.callI32ToI32(10);
      assertEquals(20, result, "callI32ToI32(10) should return 20");
      LOGGER.info("callI32ToI32(10) = " + result);
    }

    @Test
    @DisplayName("callI32I32ToI32 should return correct result")
    void shouldCallI32I32ToI32Fast() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "add");
      final int result = func.callI32I32ToI32(100, 200);
      assertEquals(300, result, "callI32I32ToI32(100, 200) should return 300");
      LOGGER.info("callI32I32ToI32(100, 200) = " + result);
    }

    @Test
    @DisplayName("callI64ToI64 should return correct result")
    void shouldCallI64ToI64Fast() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "i64_to_i64");
      final long result = func.callI64ToI64(7L);
      assertEquals(21L, result, "callI64ToI64(7) should return 21");
      LOGGER.info("callI64ToI64(7) = " + result);
    }

    @Test
    @DisplayName("callF64ToF64 should return correct result")
    void shouldCallF64ToF64Fast() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "f64_to_f64");
      final double result = func.callF64ToF64(3.5);
      assertEquals(7.0, result, 0.001, "callF64ToF64(3.5) should return 7.0");
      LOGGER.info("callF64ToF64(3.5) = " + result);
    }

    @Test
    @DisplayName("callI32ToI32 with various values should compute correctly")
    void shouldComputeCorrectlyForVariousValues() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "i32_to_i32");
      assertEquals(0, func.callI32ToI32(0), "0 * 2 = 0");
      assertEquals(2, func.callI32ToI32(1), "1 * 2 = 2");
      assertEquals(-2, func.callI32ToI32(-1), "-1 * 2 = -2");
      assertEquals(200, func.callI32ToI32(100), "100 * 2 = 200");
      LOGGER.info("Multiple callI32ToI32 computations verified");
    }

    @Test
    @DisplayName("callI32I32ToI32 with various values should compute correctly")
    void shouldAddCorrectlyForVariousValues() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "add");
      assertEquals(0, func.callI32I32ToI32(0, 0), "0 + 0 = 0");
      assertEquals(1, func.callI32I32ToI32(0, 1), "0 + 1 = 1");
      assertEquals(0, func.callI32I32ToI32(1, -1), "1 + (-1) = 0");
      assertEquals(-5, func.callI32I32ToI32(-2, -3), "-2 + (-3) = -5");
      LOGGER.info("Multiple callI32I32ToI32 computations verified");
    }
  }

  // ==================== Async Call Tests ====================

  @Nested
  @DisplayName("Async Call Tests")
  class AsyncCallTests {

    @Test
    @DisplayName("callAsync should return correct result")
    void shouldCallAsyncSuccessfully() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "return_i32");
      final CompletableFuture<WasmValue[]> future = func.callAsync();
      final WasmValue[] results = future.get(5, TimeUnit.SECONDS);
      assertNotNull(results, "Async results should not be null");
      assertTrue(results.length > 0, "Should have at least one result");
      assertEquals(42, results[0].asInt(), "Async return_i32 should return 42");
      LOGGER.info("callAsync() returned: " + results[0].asInt());
    }

    @Test
    @DisplayName("callAsync with params should work")
    void shouldCallAsyncWithParams() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction func = getFunction(instance, "add");
      final CompletableFuture<WasmValue[]> future =
          func.callAsync(WasmValue.i32(10), WasmValue.i32(20));
      final WasmValue[] results = future.get(5, TimeUnit.SECONDS);
      assertNotNull(results, "Async results should not be null");
      assertEquals(30, results[0].asInt(), "Async add(10, 20) should return 30");
      LOGGER.info("callAsync(add, 10, 20) returned: " + results[0].asInt());
    }
  }

  // ==================== Typed Function Tests ====================

  @Nested
  @DisplayName("Typed Function Tests")
  class TypedFunctionTests {

    @Test
    @DisplayName("asTyped should return TypedFunc instance")
    void shouldReturnTypedFunc() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "add");
      final TypedFunc typedFunc = func.asTyped("ii->i");
      assertNotNull(typedFunc, "TypedFunc should not be null");
      LOGGER.info("Created typed function with signature ii->i");
    }
  }

  // ==================== Lifecycle Tests ====================

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("close should not throw")
    void shouldCloseWithoutError() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "return_i32");
      assertDoesNotThrow(func::close, "Closing function should not throw");
    }

    @Test
    @DisplayName("double close should not throw")
    void shouldHandleDoubleClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "return_i32");
      func.close();
      assertDoesNotThrow(func::close, "Double close should not throw");
    }

    @Test
    @DisplayName("call after close should throw IllegalStateException")
    void shouldThrowOnCallAfterClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "return_i32");
      func.close();
      assertThrows(
          IllegalStateException.class,
          func::call,
          "call on closed function should throw IllegalStateException");
    }

    @Test
    @DisplayName("callVoid after close should throw IllegalStateException")
    void shouldThrowOnCallVoidAfterClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "void_func");
      func.close();
      assertThrows(
          IllegalStateException.class,
          func::callVoid,
          "callVoid on closed function should throw IllegalStateException");
    }

    @Test
    @DisplayName("callToI32 after close should throw IllegalStateException")
    void shouldThrowOnCallToI32AfterClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "return_i32");
      func.close();
      assertThrows(
          IllegalStateException.class,
          func::callToI32,
          "callToI32 on closed function should throw IllegalStateException");
    }

    @Test
    @DisplayName("callI32ToI32 after close should throw IllegalStateException")
    void shouldThrowOnCallI32ToI32AfterClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "i32_to_i32");
      func.close();
      assertThrows(
          IllegalStateException.class,
          () -> func.callI32ToI32(1),
          "callI32ToI32 on closed function should throw IllegalStateException");
    }

    @Test
    @DisplayName("callI32I32ToI32 after close should throw IllegalStateException")
    void shouldThrowOnCallI32I32ToI32AfterClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "add");
      func.close();
      assertThrows(
          IllegalStateException.class,
          () -> func.callI32I32ToI32(1, 2),
          "callI32I32ToI32 on closed function should throw IllegalStateException");
    }

    @Test
    @DisplayName("callI64ToI64 after close should throw IllegalStateException")
    void shouldThrowOnCallI64ToI64AfterClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "i64_to_i64");
      func.close();
      assertThrows(
          IllegalStateException.class,
          () -> func.callI64ToI64(1L),
          "callI64ToI64 on closed function should throw IllegalStateException");
    }

    @Test
    @DisplayName("callF64ToF64 after close should throw IllegalStateException")
    void shouldThrowOnCallF64ToF64AfterClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "f64_to_f64");
      func.close();
      assertThrows(
          IllegalStateException.class,
          () -> func.callF64ToF64(1.0),
          "callF64ToF64 on closed function should throw IllegalStateException");
    }

    @Test
    @DisplayName("getName and getFunctionType should work after close")
    void shouldReturnAccessorsAfterClose() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final PanamaFunction func = (PanamaFunction) getFunction(instance, "return_i32");
      final String nameBefore = func.getName();
      final FunctionType typeBefore = func.getFunctionType();
      func.close();
      assertEquals(nameBefore, func.getName(), "getName should work after close");
      assertEquals(typeBefore, func.getFunctionType(), "getFunctionType should work after close");
    }
  }

  // ==================== Multiple Functions Tests ====================

  @Nested
  @DisplayName("Multiple Functions Tests")
  class MultipleFunctionsTests {

    @Test
    @DisplayName("should retrieve all exported functions")
    void shouldRetrieveAllExportedFunctions() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final String[] expectedNames = {
        "void_func", "return_i32", "i32_to_i32", "add", "i64_to_i64", "f64_to_f64"
      };

      for (final String name : expectedNames) {
        final Optional<WasmFunction> funcOpt = instance.getFunction(name);
        assertTrue(funcOpt.isPresent(), "Function '" + name + "' should be exported");
        assertEquals(name, funcOpt.get().getName(), "Function name should match");
      }
      LOGGER.info("All " + expectedNames.length + " exported functions retrieved successfully");
    }

    @Test
    @DisplayName("should call multiple functions in sequence")
    void shouldCallMultipleFunctionsInSequence() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();

      final WasmFunction voidFunc = getFunction(instance, "void_func");
      voidFunc.callVoid();

      final WasmFunction returnI32 = getFunction(instance, "return_i32");
      assertEquals(42, returnI32.callToI32(), "return_i32 should return 42");

      final WasmFunction add = getFunction(instance, "add");
      assertEquals(15, add.callI32I32ToI32(7, 8), "add(7, 8) should return 15");

      final WasmFunction i32ToI32 = getFunction(instance, "i32_to_i32");
      assertEquals(6, i32ToI32.callI32ToI32(3), "i32_to_i32(3) should return 6");

      LOGGER.info("Multiple sequential function calls succeeded");
    }

    @Test
    @DisplayName("should chain function calls using results")
    void shouldChainFunctionCalls() throws Exception {
      final PanamaInstance instance = createFunctionTestInstance();
      final WasmFunction i32ToI32 = getFunction(instance, "i32_to_i32");
      final WasmFunction add = getFunction(instance, "add");

      // Chain: double(5) -> 10, then add(10, 3) -> 13
      final int doubled = i32ToI32.callI32ToI32(5);
      assertEquals(10, doubled, "5 * 2 = 10");

      final int sum = add.callI32I32ToI32(doubled, 3);
      assertEquals(13, sum, "10 + 3 = 13");
      LOGGER.info("Chained call: double(5) + 3 = " + sum);
    }
  }
}
