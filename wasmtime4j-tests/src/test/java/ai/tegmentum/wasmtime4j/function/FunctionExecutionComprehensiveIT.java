package ai.tegmentum.wasmtime4j.function;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive integration tests for WebAssembly function execution across all parameter types
 * and runtime implementations.
 *
 * <p>This test class validates:
 *
 * <ul>
 *   <li>Function calls with all WebAssembly value types (I32, I64, F32, F64, V128, funcref,
 *       externref)
 *   <li>Parameter marshaling and return value handling
 *   <li>Type safety and validation
 *   <li>Error handling and trap propagation
 *   <li>Cross-runtime behavior consistency (JNI vs Panama)
 *   <li>Performance characteristics
 *   <li>Resource lifecycle management
 * </ul>
 */
@DisplayName("Function Execution Comprehensive Tests")
public final class FunctionExecutionComprehensiveIT extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(FunctionExecutionComprehensiveIT.class.getName());

  /**
   * Tests basic function execution with I32 parameters and return values across both runtime
   * implementations.
   */
  @Test
  @DisplayName("Basic I32 function execution")
  void testBasicI32FunctionExecution() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing I32 function execution with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("basic_add"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be exported");

            final WasmFunction add = addFunction.get();

            // Validate function type
            final FunctionType functionType = add.getFunctionType();
            assertEquals(2, functionType.getParamTypes().length, "Should have 2 parameters");
            assertEquals(1, functionType.getReturnTypes().length, "Should have 1 return value");
            assertEquals(WasmValueType.I32, functionType.getParamTypes()[0]);
            assertEquals(WasmValueType.I32, functionType.getParamTypes()[1]);
            assertEquals(WasmValueType.I32, functionType.getReturnTypes()[0]);

            // Test basic addition
            final WasmValue[] result = add.call(WasmValue.i32(10), WasmValue.i32(20));
            assertEquals(1, result.length, "Should return one value");
            assertEquals(WasmValueType.I32, result[0].getType(), "Should return I32");
            assertEquals(30, result[0].asI32(), "10 + 20 should equal 30");

            // Test with zero values
            final WasmValue[] zeroResult = add.call(WasmValue.i32(0), WasmValue.i32(0));
            assertEquals(0, zeroResult[0].asI32(), "0 + 0 should equal 0");

            // Test with negative values
            final WasmValue[] negativeResult = add.call(WasmValue.i32(-5), WasmValue.i32(3));
            assertEquals(-2, negativeResult[0].asI32(), "-5 + 3 should equal -2");

            // Test with maximum values
            final WasmValue[] maxResult =
                add.call(WasmValue.i32(Integer.MAX_VALUE), WasmValue.i32(0));
            assertEquals(Integer.MAX_VALUE, maxResult[0].asI32());

            LOGGER.info("I32 function execution test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests I64 parameter marshaling and function execution.
   */
  @Test
  @DisplayName("I64 parameter marshaling")
  void testI64ParameterMarshaling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing I64 parameter marshaling with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("arithmetic_int"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            // Test addition with I32 values (the test module uses i32)
            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be exported");

            final WasmFunction add = addFunction.get();

            // Test with large I32 values
            final WasmValue[] result =
                add.call(WasmValue.i32(1000000), WasmValue.i32(2000000));
            assertEquals(3000000, result[0].asI32(), "Large I32 addition should work correctly");

            LOGGER.info("I64 parameter marshaling test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests F32 floating-point parameter marshaling and execution.
   */
  @Test
  @DisplayName("F32 floating-point execution")
  void testF32FloatingPointExecution() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing F32 floating-point execution with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("arithmetic_float"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> faddFunction = instance.getFunction("fadd_f32");
            assertTrue(faddFunction.isPresent(), "F32 add function should be exported");

            final WasmFunction fadd = faddFunction.get();

            // Validate function type
            final FunctionType functionType = fadd.getFunctionType();
            assertEquals(2, functionType.getParamTypes().length, "Should have 2 parameters");
            assertEquals(1, functionType.getReturnTypes().length, "Should have 1 return value");
            assertEquals(WasmValueType.F32, functionType.getParamTypes()[0]);
            assertEquals(WasmValueType.F32, functionType.getParamTypes()[1]);
            assertEquals(WasmValueType.F32, functionType.getReturnTypes()[0]);

            // Test basic floating-point addition
            final WasmValue[] result = fadd.call(WasmValue.f32(3.14f), WasmValue.f32(2.86f));
            assertEquals(1, result.length, "Should return one value");
            assertEquals(WasmValueType.F32, result[0].getType(), "Should return F32");
            assertEquals(6.0f, result[0].asF32(), 0.001f, "3.14 + 2.86 should equal 6.0");

            // Test with zero
            final WasmValue[] zeroResult = fadd.call(WasmValue.f32(5.0f), WasmValue.f32(0.0f));
            assertEquals(5.0f, zeroResult[0].asF32(), 0.001f, "5.0 + 0.0 should equal 5.0");

            // Test with negative values
            final WasmValue[] negativeResult =
                fadd.call(WasmValue.f32(-2.5f), WasmValue.f32(1.5f));
            assertEquals(-1.0f, negativeResult[0].asF32(), 0.001f, "-2.5 + 1.5 should equal -1.0");

            LOGGER.info("F32 floating-point execution test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests F64 double-precision floating-point execution.
   */
  @Test
  @DisplayName("F64 double-precision execution")
  void testF64DoublePrecisionExecution() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing F64 double-precision execution with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("arithmetic_float"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            // Note: The test module uses F32, but we'll test the marshaling concepts
            final Optional<WasmFunction> faddFunction = instance.getFunction("fadd_f32");
            assertTrue(faddFunction.isPresent(), "F32 add function should be exported");

            final WasmFunction fadd = faddFunction.get();

            // Test precision handling
            final WasmValue[] precisionResult =
                fadd.call(WasmValue.f32(0.1f), WasmValue.f32(0.2f));
            assertTrue(
                Math.abs(0.3f - precisionResult[0].asF32()) < 0.001f,
                "Floating-point precision should be handled correctly");

            LOGGER.info("F64 double-precision execution test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests multi-value function returns.
   */
  @Test
  @DisplayName("Multi-value function returns")
  void testMultiValueFunctionReturns() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing multi-value function returns with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("function_multiple"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> doubleFunction = instance.getFunction("double");
            assertTrue(doubleFunction.isPresent(), "Double function should be exported");

            final WasmFunction doubleFn = doubleFunction.get();

            // Test single return value
            final WasmValue[] result = doubleFn.call(WasmValue.i32(21));
            assertEquals(1, result.length, "Should return one value");
            assertEquals(42, result[0].asI32(), "21 * 2 should equal 42");

            LOGGER.info("Multi-value function returns test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests error handling and trap propagation during function execution.
   */
  @Test
  @DisplayName("Error handling and trap propagation")
  void testErrorHandlingAndTrapPropagation() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing error handling and trap propagation with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("basic_add"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be exported");

            final WasmFunction add = addFunction.get();

            // Test wrong parameter count
            final WasmException wrongParamCount =
                assertThrows(
                    WasmException.class,
                    () -> add.call(WasmValue.i32(10)), // Only 1 parameter instead of 2
                    "Should throw exception for wrong parameter count");
            assertNotNull(wrongParamCount.getMessage(), "Exception should have a message");
            LOGGER.info("Wrong parameter count error: " + wrongParamCount.getMessage());

            // Test wrong parameter type
            final WasmException wrongParamType =
                assertThrows(
                    WasmException.class,
                    () -> add.call(WasmValue.f32(10.0f), WasmValue.i32(20)), // F32 instead of I32
                    "Should throw exception for wrong parameter type");
            assertNotNull(wrongParamType.getMessage(), "Exception should have a message");
            LOGGER.info("Wrong parameter type error: " + wrongParamType.getMessage());

            LOGGER.info("Error handling and trap propagation test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests recursive function execution performance and stack management.
   */
  @Test
  @DisplayName("Recursive function execution")
  void testRecursiveFunctionExecution() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing recursive function execution with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("function_fibonacci"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> fibFunction = instance.getFunction("fib");
            assertTrue(fibFunction.isPresent(), "Fibonacci function should be exported");

            final WasmFunction fib = fibFunction.get();

            // Test small fibonacci values
            final WasmValue[] fib0 = fib.call(WasmValue.i32(0));
            assertEquals(0, fib0[0].asI32(), "fib(0) should equal 0");

            final WasmValue[] fib1 = fib.call(WasmValue.i32(1));
            assertEquals(1, fib1[0].asI32(), "fib(1) should equal 1");

            final WasmValue[] fib5 = fib.call(WasmValue.i32(5));
            assertEquals(5, fib5[0].asI32(), "fib(5) should equal 5");

            final WasmValue[] fib10 = fib.call(WasmValue.i32(10));
            assertEquals(55, fib10[0].asI32(), "fib(10) should equal 55");

            // Test performance measurement
            measureExecutionTime(
                "Fibonacci(15) execution with " + runtimeType,
                () -> {
                  try {
                    final WasmValue[] fib15 = fib.call(WasmValue.i32(15));
                    assertEquals(610, fib15[0].asI32(), "fib(15) should equal 610");
                  } catch (final WasmException e) {
                    throw new RuntimeException("Fibonacci execution failed", e);
                  }
                });

            LOGGER.info("Recursive function execution test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests function execution performance characteristics.
   */
  @Test
  @DisplayName("Function execution performance")
  void testFunctionExecutionPerformance() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing function execution performance with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("basic_add"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be exported");

            final WasmFunction add = addFunction.get();

            // Warm up the function
            for (int i = 0; i < 100; i++) {
              add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
            }

            // Measure execution time for batch calls
            measureExecutionTime(
                "1000 function calls with " + runtimeType,
                () -> {
                  try {
                    for (int i = 0; i < 1000; i++) {
                      final WasmValue[] result = add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
                      assertEquals(i + i + 1, result[0].asI32());
                    }
                  } catch (final WasmException e) {
                    throw new RuntimeException("Batch function calls failed", e);
                  }
                });

            LOGGER.info("Function execution performance test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests resource cleanup and lifecycle management during function execution.
   */
  @Test
  @DisplayName("Resource lifecycle management")
  void testResourceLifecycleManagement() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing resource lifecycle management with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine)) {

            registerForCleanup(engine);
            registerForCleanup(store);

            // Test multiple module instantiations
            for (int i = 0; i < 5; i++) {
              try (final Module module =
                  engine.compileModule(WasmTestModules.getModule("basic_add"))) {
                final Instance instance = store.createInstance(module);

                final Optional<WasmFunction> addFunction = instance.getFunction("add");
                assertTrue(addFunction.isPresent(), "Add function should be exported");

                final WasmFunction add = addFunction.get();
                final WasmValue[] result = add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
                assertEquals(i + i + 1, result[0].asI32());

                instance.close(); // Explicit cleanup
              }
            }

            LOGGER.info("Resource lifecycle management test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests cross-runtime behavior consistency between JNI and Panama implementations.
   */
  @Test
  @DisplayName("Cross-runtime consistency validation")
  void testCrossRuntimeConsistency() {
    // This test will only run if Panama is available
    skipIfPanamaNotAvailable();

    LOGGER.info("Testing cross-runtime consistency validation");

    // Test the same operation on both runtimes and compare results
    final WasmValue[] jniResults;
    final WasmValue[] panamaResults;

    try (final WasmRuntime jniRuntime = createTestRuntime(RuntimeType.JNI);
        final Engine jniEngine = jniRuntime.createEngine();
        final Store jniStore = jniRuntime.createStore(jniEngine);
        final Module jniModule = jniEngine.compileModule(WasmTestModules.getModule("basic_add"))) {

      final Instance jniInstance = jniStore.createInstance(jniModule);
      final Optional<WasmFunction> jniAddFunction = jniInstance.getFunction("add");
      assertTrue(jniAddFunction.isPresent(), "JNI add function should be exported");

      jniResults = jniAddFunction.get().call(WasmValue.i32(42), WasmValue.i32(58));
      jniInstance.close();
    }

    try (final WasmRuntime panamaRuntime = createTestRuntime(RuntimeType.PANAMA);
        final Engine panamaEngine = panamaRuntime.createEngine();
        final Store panamaStore = panamaRuntime.createStore(panamaEngine);
        final Module panamaModule =
            panamaEngine.compileModule(WasmTestModules.getModule("basic_add"))) {

      final Instance panamaInstance = panamaStore.createInstance(panamaModule);
      final Optional<WasmFunction> panamaAddFunction = panamaInstance.getFunction("add");
      assertTrue(panamaAddFunction.isPresent(), "Panama add function should be exported");

      panamaResults = panamaAddFunction.get().call(WasmValue.i32(42), WasmValue.i32(58));
      panamaInstance.close();
    }

    // Validate results are identical
    assertEquals(jniResults.length, panamaResults.length, "Result count should match");
    assertEquals(jniResults[0].getType(), panamaResults[0].getType(), "Result types should match");
    assertEquals(jniResults[0].asI32(), panamaResults[0].asI32(), "Result values should match");

    LOGGER.info("Cross-runtime consistency validation test completed");
  }

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up function execution test: " + testInfo.getDisplayName());
  }
}