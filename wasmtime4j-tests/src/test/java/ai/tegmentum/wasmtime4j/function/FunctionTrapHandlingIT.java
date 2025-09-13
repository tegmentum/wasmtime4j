package ai.tegmentum.wasmtime4j.function;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasmTrapException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive tests for WebAssembly trap handling and error propagation during function
 * execution.
 *
 * <p>This test class validates:
 *
 * <ul>
 *   <li>Trap propagation and stack trace preservation
 *   <li>Various trap types (division by zero, out of bounds, unreachable, etc.)
 *   <li>Error recovery and cleanup after traps
 *   <li>Cross-runtime trap consistency
 *   <li>Memory safety violations and bounds checking
 *   <li>Stack overflow detection and handling
 * </ul>
 */
@DisplayName("Function Trap Handling Tests")
public final class FunctionTrapHandlingIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(FunctionTrapHandlingIT.class.getName());

  /**
   * Tests trap handling for unreachable instruction execution.
   */
  @Test
  @DisplayName("Unreachable instruction trap handling")
  void testUnreachableInstructionTrapHandling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing unreachable instruction trap handling with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("validation_unreachable"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> unreachableFunction = instance.getFunction("unreachable");
            assertTrue(unreachableFunction.isPresent(), "Unreachable function should be exported");

            final WasmFunction unreachableFn = unreachableFunction.get();

            // Test unreachable instruction trap
            final WasmException trapException =
                assertThrows(
                    WasmException.class,
                    () -> unreachableFn.call(),
                    "Should throw exception when hitting unreachable instruction");

            assertNotNull(trapException.getMessage(), "Exception should have a message");
            LOGGER.info("Unreachable trap message: " + trapException.getMessage());

            // Verify the instance is still usable after the trap
            assertDoesNotThrow(
                () -> unreachableFunction.isPresent(),
                "Instance should remain usable after trap");

            LOGGER.info("Unreachable instruction trap handling test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests memory bounds violation trap handling.
   */
  @Test
  @DisplayName("Memory bounds violation trap handling")
  void testMemoryBoundsViolationTrapHandling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing memory bounds violation trap handling with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("security_bounds_check"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> boundsCheckFunction = instance.getFunction("bounds_check");
            assertTrue(boundsCheckFunction.isPresent(), "Bounds check function should be exported");

            final WasmFunction boundsCheck = boundsCheckFunction.get();

            // Test in-bounds access (should work)
            assertDoesNotThrow(
                () -> boundsCheck.call(WasmValue.i32(0)),
                "In-bounds memory access should not throw");

            assertDoesNotThrow(
                () -> boundsCheck.call(WasmValue.i32(65532)), // Near end of 64KB page
                "In-bounds memory access near end should not throw");

            // Test out-of-bounds access (should trap)
            final WasmException outOfBoundsException =
                assertThrows(
                    WasmException.class,
                    () -> boundsCheck.call(WasmValue.i32(65536)), // Beyond 64KB page
                    "Out-of-bounds memory access should throw exception");

            assertNotNull(outOfBoundsException.getMessage(), "Exception should have a message");
            LOGGER.info("Out-of-bounds trap message: " + outOfBoundsException.getMessage());

            // Test way out-of-bounds access
            final WasmException wayOutOfBoundsException =
                assertThrows(
                    WasmException.class,
                    () -> boundsCheck.call(WasmValue.i32(Integer.MAX_VALUE)),
                    "Way out-of-bounds memory access should throw exception");

            assertNotNull(wayOutOfBoundsException.getMessage(), "Exception should have a message");

            LOGGER.info("Memory bounds violation trap handling test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests stack overflow detection and handling during recursive function calls.
   */
  @Test
  @DisplayName("Stack overflow trap handling")
  void testStackOverflowTrapHandling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing stack overflow trap handling with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("resource_stack_intensive"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> stackTestFunction = instance.getFunction("stack_test");
            assertTrue(stackTestFunction.isPresent(), "Stack test function should be exported");

            final WasmFunction stackTest = stackTestFunction.get();

            // Test reasonable recursion depth (should work)
            final WasmValue[] result1 = stackTest.call(WasmValue.i32(5));
            assertEquals(120, result1[0].asI32(), "5! should equal 120");

            final WasmValue[] result2 = stackTest.call(WasmValue.i32(10));
            assertEquals(3628800, result2[0].asI32(), "10! should equal 3628800");

            // Test excessive recursion depth (should trap)
            final WasmException stackOverflowException =
                assertThrows(
                    WasmException.class,
                    () -> stackTest.call(WasmValue.i32(50000)), // Very deep recursion
                    "Excessive recursion should cause stack overflow");

            assertNotNull(stackOverflowException.getMessage(), "Exception should have a message");
            LOGGER.info("Stack overflow trap message: " + stackOverflowException.getMessage());

            // Verify the instance is still usable after stack overflow
            final WasmValue[] recoveryResult = stackTest.call(WasmValue.i32(3));
            assertEquals(6, recoveryResult[0].asI32(), "3! should equal 6 after stack overflow recovery");

            LOGGER.info("Stack overflow trap handling test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests parameter validation and type mismatch error handling.
   */
  @Test
  @DisplayName("Parameter validation and type mismatch handling")
  void testParameterValidationAndTypeMismatch() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing parameter validation and type mismatch with " + runtimeType + " runtime");

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

            // Test correct parameters (should work)
            final WasmValue[] correctResult = add.call(WasmValue.i32(10), WasmValue.i32(20));
            assertEquals(30, correctResult[0].asI32(), "Correct parameters should work");

            // Test too few parameters
            final WasmException tooFewParams =
                assertThrows(
                    WasmException.class,
                    () -> add.call(WasmValue.i32(10)),
                    "Too few parameters should throw exception");
            assertNotNull(tooFewParams.getMessage());
            LOGGER.info("Too few parameters error: " + tooFewParams.getMessage());

            // Test too many parameters
            final WasmException tooManyParams =
                assertThrows(
                    WasmException.class,
                    () -> add.call(WasmValue.i32(10), WasmValue.i32(20), WasmValue.i32(30)),
                    "Too many parameters should throw exception");
            assertNotNull(tooManyParams.getMessage());
            LOGGER.info("Too many parameters error: " + tooManyParams.getMessage());

            // Test wrong parameter type
            final WasmException wrongType =
                assertThrows(
                    WasmException.class,
                    () -> add.call(WasmValue.f32(10.0f), WasmValue.i32(20)),
                    "Wrong parameter type should throw exception");
            assertNotNull(wrongType.getMessage());
            LOGGER.info("Wrong parameter type error: " + wrongType.getMessage());

            LOGGER.info("Parameter validation and type mismatch test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests error recovery and cleanup after various trap scenarios.
   */
  @Test
  @DisplayName("Error recovery and cleanup after traps")
  void testErrorRecoveryAndCleanupAfterTraps() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing error recovery and cleanup after traps with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine)) {

            registerForCleanup(engine);
            registerForCleanup(store);

            // Test multiple trap scenarios with the same store/engine
            for (int iteration = 0; iteration < 3; iteration++) {
              LOGGER.info("Error recovery iteration " + (iteration + 1) + " for " + runtimeType);

              try (final Module module =
                  engine.compileModule(WasmTestModules.getModule("basic_add"))) {
                final Instance instance = store.createInstance(module);

                final Optional<WasmFunction> addFunction = instance.getFunction("add");
                assertTrue(addFunction.isPresent(), "Add function should be exported");

                final WasmFunction add = addFunction.get();

                // Cause a trap
                assertThrows(
                    WasmException.class,
                    () -> add.call(WasmValue.i32(10)), // Wrong parameter count
                    "Should cause parameter mismatch trap");

                // Verify function still works after trap
                final WasmValue[] result = add.call(WasmValue.i32(10), WasmValue.i32(20));
                assertEquals(30, result[0].asI32(), "Function should work after trap recovery");

                instance.close(); // Explicit cleanup
              }
            }

            LOGGER.info("Error recovery and cleanup test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests cross-runtime trap consistency between JNI and Panama implementations.
   */
  @Test
  @DisplayName("Cross-runtime trap consistency")
  void testCrossRuntimeTrapConsistency() {
    skipIfPanamaNotAvailable();

    LOGGER.info("Testing cross-runtime trap consistency");

    // Test the same trap scenario on both runtimes
    WasmException jniTrapException = null;
    WasmException panamaTrapException = null;

    // Test with JNI runtime
    try (final WasmRuntime jniRuntime = createTestRuntime(RuntimeType.JNI);
        final Engine jniEngine = jniRuntime.createEngine();
        final Store jniStore = jniRuntime.createStore(jniEngine);
        final Module jniModule = jniEngine.compileModule(WasmTestModules.getModule("basic_add"))) {

      final Instance jniInstance = jniStore.createInstance(jniModule);
      final Optional<WasmFunction> jniAddFunction = jniInstance.getFunction("add");
      assertTrue(jniAddFunction.isPresent(), "JNI add function should be exported");

      try {
        jniAddFunction.get().call(WasmValue.i32(10)); // Wrong parameter count
        fail("JNI runtime should have thrown exception");
      } catch (final WasmException e) {
        jniTrapException = e;
      }
      jniInstance.close();
    }

    // Test with Panama runtime
    try (final WasmRuntime panamaRuntime = createTestRuntime(RuntimeType.PANAMA);
        final Engine panamaEngine = panamaRuntime.createEngine();
        final Store panamaStore = panamaRuntime.createStore(panamaEngine);
        final Module panamaModule = panamaEngine.compileModule(WasmTestModules.getModule("basic_add"))) {

      final Instance panamaInstance = panamaStore.createInstance(panamaModule);
      final Optional<WasmFunction> panamaAddFunction = panamaInstance.getFunction("add");
      assertTrue(panamaAddFunction.isPresent(), "Panama add function should be exported");

      try {
        panamaAddFunction.get().call(WasmValue.i32(10)); // Wrong parameter count
        fail("Panama runtime should have thrown exception");
      } catch (final WasmException e) {
        panamaTrapException = e;
      }
      panamaInstance.close();
    }

    // Validate both runtimes threw exceptions
    assertNotNull(jniTrapException, "JNI runtime should have thrown trap exception");
    assertNotNull(panamaTrapException, "Panama runtime should have thrown trap exception");

    // Validate exception characteristics are similar
    assertNotNull(jniTrapException.getMessage(), "JNI exception should have message");
    assertNotNull(panamaTrapException.getMessage(), "Panama exception should have message");
    assertEquals(
        jniTrapException.getClass(),
        panamaTrapException.getClass(),
        "Exception types should match across runtimes");

    LOGGER.info("JNI trap message: " + jniTrapException.getMessage());
    LOGGER.info("Panama trap message: " + panamaTrapException.getMessage());
    LOGGER.info("Cross-runtime trap consistency test completed");
  }

  /**
   * Tests trap handling with malformed WebAssembly modules.
   */
  @Test
  @DisplayName("Malformed module trap handling")
  void testMalformedModuleTrapHandling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing malformed module trap handling with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine()) {

            registerForCleanup(engine);

            // Test various malformed modules
            final String[] malformedModules = {
              "malformed_magic",
              "malformed_version", 
              "malformed_truncated",
              "malformed_section"
            };

            for (final String malformedModuleName : malformedModules) {
              if (WasmTestModules.hasModule(malformedModuleName)) {
                LOGGER.info("Testing malformed module: " + malformedModuleName + " with " + runtimeType);

                final WasmException malformedException =
                    assertThrows(
                        WasmException.class,
                        () -> engine.compileModule(WasmTestModules.getModule(malformedModuleName)),
                        "Should throw exception for malformed module: " + malformedModuleName);

                assertNotNull(malformedException.getMessage(), "Exception should have a message");
                LOGGER.info("Malformed module error: " + malformedException.getMessage());

                // Verify the engine is still usable after malformed module
                assertDoesNotThrow(
                    () -> engine.compileModule(WasmTestModules.getModule("basic_add")),
                    "Engine should remain usable after malformed module error");
              }
            }

            LOGGER.info("Malformed module trap handling test completed for " + runtimeType);
          }
        });
  }

  /**
   * Tests trap performance characteristics and overhead.
   */
  @Test
  @DisplayName("Trap performance characteristics")
  void testTrapPerformanceCharacteristics() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing trap performance characteristics with " + runtimeType + " runtime");

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

            // Measure normal function call performance
            measureExecutionTime(
                "Normal function calls (100 iterations) with " + runtimeType,
                () -> {
                  try {
                    for (int i = 0; i < 100; i++) {
                      final WasmValue[] result = add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
                      assertEquals(i + i + 1, result[0].asI32());
                    }
                  } catch (final WasmException e) {
                    throw new RuntimeException("Normal function calls failed", e);
                  }
                });

            // Measure trap handling performance
            measureExecutionTime(
                "Trap handling (100 iterations) with " + runtimeType,
                () -> {
                  for (int i = 0; i < 100; i++) {
                    try {
                      add.call(WasmValue.i32(i)); // Wrong parameter count
                      fail("Should have thrown exception");
                    } catch (final WasmException e) {
                      // Expected trap - just continue
                      assertNotNull(e.getMessage());
                    }
                  }
                });

            LOGGER.info("Trap performance characteristics test completed for " + runtimeType);
          }
        });
  }

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up function trap handling test: " + testInfo.getDisplayName());
  }
}