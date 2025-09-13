package ai.tegmentum.wasmtime4j.instance;

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
 * Test suite for WebAssembly function invocation functionality within instances across both JNI and
 * Panama runtime implementations.
 *
 * <p>This test class validates:
 *
 * <ul>
 *   <li>Function discovery and enumeration
 *   <li>Function signature validation
 *   <li>Parameter marshaling and validation
 *   <li>Function execution and result handling
 *   <li>Exception handling during invocation
 *   <li>Performance characteristics of function calls
 *   <li>Memory management during invocation
 *   <li>Cross-runtime behavior consistency
 * </ul>
 */
@DisplayName("Instance Function Invocation Tests")
public final class InstanceFunctionInvocationIT extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(InstanceFunctionInvocationIT.class.getName());

  /** Tests basic function discovery and invocation within instances. */
  @Test
  @DisplayName("Basic function discovery and invocation")
  void testBasicFunctionDiscoveryAndInvocation() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing function discovery and invocation with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module = engine.compileModule(WasmTestModules.getModule("basic_add"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            // Test function discovery
            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be discoverable");

            // Test function invocation
            final WasmFunction add = addFunction.get();
            final WasmValue[] result = add.call(WasmValue.i32(15), WasmValue.i32(25));

            assertEquals(1, result.length, "Should return one value");
            assertEquals(40, result[0].asI32(), "15 + 25 should equal 40");

            // Test non-existent function discovery
            final Optional<WasmFunction> nonExistentFunction = instance.getFunction("nonexistent");
            assertFalse(
                nonExistentFunction.isPresent(), "Non-existent function should not be found");

            LOGGER.info("Function discovery and invocation test completed for " + runtimeType);
          }
        });
  }

  /** Tests function signature validation and parameter type checking. */
  @Test
  @DisplayName("Function signature validation")
  void testFunctionSignatureValidation() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing function signature validation with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("arithmetic_int"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            // Test multiple function signatures
            final String[] functionNames = {"add", "sub", "mul"};

            for (final String functionName : functionNames) {
              final Optional<WasmFunction> function = instance.getFunction(functionName);
              assertTrue(function.isPresent(), functionName + " function should be discoverable");

              final WasmFunction fn = function.get();
              final FunctionType functionType = fn.getFunctionType();

              // Validate signature
              assertEquals(
                  2,
                  functionType.getParamTypes().length,
                  functionName + " should have 2 parameters");
              assertEquals(
                  1,
                  functionType.getReturnTypes().length,
                  functionName + " should have 1 return value");
              assertEquals(WasmValueType.I32, functionType.getParamTypes()[0]);
              assertEquals(WasmValueType.I32, functionType.getParamTypes()[1]);
              assertEquals(WasmValueType.I32, functionType.getReturnTypes()[0]);

              // Test function name
              assertEquals(functionName, fn.getName(), "Function name should match");
            }

            LOGGER.info("Function signature validation test completed for " + runtimeType);
          }
        });
  }

  /** Tests various parameter types and marshaling scenarios. */
  @Test
  @DisplayName("Parameter marshaling validation")
  void testParameterMarshalingValidation() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing parameter marshaling validation with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("arithmetic_float"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            // Test F32 parameter marshaling
            final Optional<WasmFunction> faddFunction = instance.getFunction("fadd_f32");
            assertTrue(faddFunction.isPresent(), "F32 add function should be discoverable");

            final WasmFunction fadd = faddFunction.get();

            // Test various F32 values
            final WasmValue[] result1 = fadd.call(WasmValue.f32(1.5f), WasmValue.f32(2.5f));
            assertEquals(4.0f, result1[0].asF32(), 0.001f, "1.5 + 2.5 should equal 4.0");

            final WasmValue[] result2 = fadd.call(WasmValue.f32(-1.0f), WasmValue.f32(3.0f));
            assertEquals(2.0f, result2[0].asF32(), 0.001f, "-1.0 + 3.0 should equal 2.0");

            // Test edge cases
            final WasmValue[] result3 = fadd.call(WasmValue.f32(0.0f), WasmValue.f32(0.0f));
            assertEquals(0.0f, result3[0].asF32(), 0.001f, "0.0 + 0.0 should equal 0.0");

            LOGGER.info("Parameter marshaling validation test completed for " + runtimeType);
          }
        });
  }

  /** Tests function execution performance within instance context. */
  @Test
  @DisplayName("Function execution performance within instances")
  void testFunctionExecutionPerformanceWithinInstances() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing function execution performance with " + runtimeType + " runtime");

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
            assertTrue(doubleFunction.isPresent(), "Double function should be discoverable");

            final WasmFunction doubleFn = doubleFunction.get();

            // Warm up the function
            for (int i = 0; i < 50; i++) {
              doubleFn.call(WasmValue.i32(i));
            }

            // Measure execution performance
            measureExecutionTime(
                "Function invocations (1000 iterations) within instance with " + runtimeType,
                () -> {
                  try {
                    for (int i = 0; i < 1000; i++) {
                      final WasmValue[] result = doubleFn.call(WasmValue.i32(i));
                      assertEquals(i * 2, result[0].asI32());
                    }
                  } catch (final WasmException e) {
                    throw new RuntimeException("Function invocation failed", e);
                  }
                });

            LOGGER.info("Function execution performance test completed for " + runtimeType);
          }
        });
  }

  /** Tests exception handling during function invocation within instances. */
  @Test
  @DisplayName("Exception handling during function invocation")
  void testExceptionHandlingDuringFunctionInvocation() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info(
              "Testing exception handling during invocation with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module = engine.compileModule(WasmTestModules.getModule("basic_add"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be discoverable");

            final WasmFunction add = addFunction.get();

            // Test correct invocation first
            final WasmValue[] correctResult = add.call(WasmValue.i32(10), WasmValue.i32(20));
            assertEquals(30, correctResult[0].asI32());

            // Test parameter count mismatch
            assertThrows(
                WasmException.class,
                () -> add.call(WasmValue.i32(10)),
                "Wrong parameter count should throw exception");

            // Test parameter type mismatch
            assertThrows(
                WasmException.class,
                () -> add.call(WasmValue.f32(10.0f), WasmValue.i32(20)),
                "Wrong parameter type should throw exception");

            // Verify instance is still usable after exceptions
            final WasmValue[] recoveryResult = add.call(WasmValue.i32(5), WasmValue.i32(7));
            assertEquals(
                12, recoveryResult[0].asI32(), "Instance should remain usable after exceptions");

            LOGGER.info("Exception handling test completed for " + runtimeType);
          }
        });
  }

  /** Tests memory management during intensive function invocations. */
  @Test
  @DisplayName("Memory management during function invocations")
  void testMemoryManagementDuringFunctionInvocations() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info(
              "Testing memory management during invocations with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine)) {

            registerForCleanup(engine);
            registerForCleanup(store);

            // Create and destroy multiple instances with function calls
            for (int iteration = 0; iteration < 10; iteration++) {
              try (final Module module =
                  engine.compileModule(WasmTestModules.getModule("basic_add"))) {
                final Instance instance = store.createInstance(module);

                final Optional<WasmFunction> addFunction = instance.getFunction("add");
                assertTrue(addFunction.isPresent(), "Add function should be discoverable");

                final WasmFunction add = addFunction.get();

                // Perform multiple function calls
                for (int i = 0; i < 100; i++) {
                  final WasmValue[] result = add.call(WasmValue.i32(i), WasmValue.i32(iteration));
                  assertEquals(i + iteration, result[0].asI32());
                }

                instance.close(); // Explicit cleanup
              }
            }

            LOGGER.info("Memory management test completed for " + runtimeType);
          }
        });
  }

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up instance function invocation test: " + testInfo.getDisplayName());
  }
}
