package ai.tegmentum.wasmtime4j.nativefunctions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.memory.MemoryLeakDetector;
import ai.tegmentum.wasmtime4j.nativefunctions.NativeFunctionTestUtils.TestModule;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive native function tests for Instance operations. Tests all native functions related
 * to WebAssembly instance creation, function calls, memory access, table operations, and global
 * variable management for both JNI and Panama FFI implementations.
 *
 * <p>This test class validates:
 *
 * <ul>
 *   <li>Instance creation and destruction native functions
 *   <li>Function invocation native functions
 *   <li>Memory access native functions
 *   <li>Table operation native functions
 *   <li>Global variable access native functions
 *   <li>Export/import resolution native functions
 *   <li>Memory leak detection for all instance operations
 *   <li>Thread safety of concurrent instance operations
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Instance Native Function Tests")
@SuppressWarnings("try")
public class InstanceNativeFunctionTest extends BaseNativeFunctionTest {

  @Test
  @Order(1)
  @DisplayName("Should create and destroy instance without memory leaks")
  void shouldCreateAndDestroyInstanceWithoutMemoryLeaks() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getComplexModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "instance_create_destroy",
              r -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    try (final var instance = r.instantiate(module)) {
                      assertThat(instance).isNotNull();

                      // Exercise instance functionality
                      exerciseInstanceOperations(instance);
                    }
                  }
                }
              });

      assertNoMemoryLeaks(result, "instance_create_destroy");
      LOGGER.info("Instance creation/destruction passed memory leak test");
    }
  }

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @Order(2)
  @DisplayName("Should create instances across all runtime types")
  void shouldCreateInstancesAcrossAllRuntimeTypes(final RuntimeType runtimeType)
      throws WasmException {
    if (runtimeType == RuntimeType.PANAMA && !TestUtils.isPanamaAvailable()) {
      LOGGER.info("Skipping Panama test - not available on this platform");
      return;
    }

    try (final WasmRuntime runtime = createRuntime(runtimeType)) {
      final List<TestModule> testModules = testUtils.getAllTestModules();

      for (final TestModule testModule : testModules.subList(0, Math.min(3, testModules.size()))) {
        final MemoryLeakDetector.LeakAnalysisResult result =
            testWithFastMemoryLeakDetection(
                "instance_create_" + runtimeType + "_" + testModule.getName(),
                r -> {
                  try (final var engine = r.createEngine()) {
                    try {
                      try (final var module = engine.compileModule(testModule.getModuleBytes())) {
                        try (final var instance = r.instantiate(module)) {
                          assertThat(instance).isNotNull();
                          exerciseInstanceOperations(instance);
                        }
                      }
                    } catch (final Exception e) {
                      // Some test modules may not be instantiable
                      LOGGER.fine("Expected instantiation failure for " + testModule.getName());
                    }
                  }
                });

        assertNoMemoryLeaks(result, "instance_create_" + runtimeType + "_" + testModule.getName());
      }
    }
  }

  @Test
  @Order(3)
  @DisplayName("Should handle function invocation with memory leak detection")
  void shouldHandleFunctionInvocationWithMemoryLeakDetection() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "function_invocation",
              r -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    try (final var instance = r.instantiate(module)) {
                      // Test function invocation repeatedly
                      final var addFunction = instance.getFunction("add");
                      if (addFunction.isPresent()) {
                        for (int i = 0; i < 20; i++) {
                          final var args =
                              new ai.tegmentum.wasmtime4j.WasmValue[] {
                                ai.tegmentum.wasmtime4j.WasmValue.i32(i),
                                ai.tegmentum.wasmtime4j.WasmValue.i32(i * 2)
                              };
                          final var callResult = addFunction.get().call(args);
                          assertThat(callResult[0].asI32()).isEqualTo(i * 3);
                        }
                      }
                    }
                  }
                }
              });

      assertNoMemoryLeaks(result, "function_invocation");
    }
  }

  @Test
  @Order(4)
  @DisplayName("Should handle memory access operations")
  void shouldHandleMemoryAccessOperations() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getMemoryModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "memory_access_operations",
              r -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    try (final var instance = r.instantiate(module)) {
                      // Test memory operations
                      exerciseMemoryOperations(instance);

                      // Test repeated memory access
                      for (int i = 0; i < 10; i++) {
                        exerciseMemoryOperations(instance);
                      }
                    }
                  }
                }
              });

      assertNoMemoryLeaks(result, "memory_access_operations");
    }
  }

  @Test
  @Order(5)
  @DisplayName("Should handle table operations")
  void shouldHandleTableOperations() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getTableModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "table_operations",
              r -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    try (final var instance = r.instantiate(module)) {
                      // Test table operations
                      exerciseTableOperations(instance);

                      // Test repeated table access
                      for (int i = 0; i < 5; i++) {
                        exerciseTableOperations(instance);
                      }
                    }
                  }
                }
              });

      assertNoMemoryLeaks(result, "table_operations");
    }
  }

  @Test
  @Order(6)
  @DisplayName("Should handle global variable operations")
  void shouldHandleGlobalVariableOperations() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getGlobalModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "global_variable_operations",
              r -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    try (final var instance = r.instantiate(module)) {
                      // Test global operations
                      exerciseGlobalOperations(instance);

                      // Test state consistency
                      for (int i = 0; i < 15; i++) {
                        exerciseGlobalOperations(instance);
                      }
                    }
                  }
                }
              });

      assertNoMemoryLeaks(result, "global_variable_operations");
    }
  }

  @Test
  @Order(7)
  @DisplayName("Should test thread safety of instance operations")
  void shouldTestThreadSafetyOfInstanceOperations() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getComplexModule();

      final ThreadSafetyTestResult result =
          testThreadSafety(
              "concurrent_instance_operations",
              (r, threadId, operationId) -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    try (final var instance = r.instantiate(module)) {
                      // Exercise various instance operations concurrently
                      exerciseInstanceOperations(instance);

                      // Test function calls with thread-specific data
                      if (instance.getFunction("set_state").isPresent()) {
                        final var setStateFunc = instance.getFunction("set_state");
                        if (setStateFunc.isPresent()) {
                          final int stateValue = threadId * 1000 + operationId;
                          final var args =
                              new ai.tegmentum.wasmtime4j.WasmValue[] {
                                ai.tegmentum.wasmtime4j.WasmValue.i32(stateValue)
                              };
                          setStateFunc.get().call(args);

                          // Verify state was set
                          final var getStateFunc = instance.getFunction("get_state");
                          if (getStateFunc.isPresent()) {
                            final var stateResult = getStateFunc.get().call();
                            if (stateResult.length > 0) {
                              assertThat(stateResult[0].asI32()).isEqualTo(stateValue);
                            }
                          }
                        }
                      }
                    }
                  }
                }
              },
              4, // 4 threads
              10); // 10 operations per thread

      assertThreadSafety(result, "concurrent_instance_operations");
      LOGGER.info(
          String.format(
              "Instance thread safety test completed: %.2f ops/sec",
              result.getOperationsPerSecond()));
    }
  }

  @Test
  @Order(8)
  @DisplayName("Should test concurrent function invocation")
  void shouldTestConcurrentFunctionInvocation() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();

      final ThreadSafetyTestResult result =
          testThreadSafety(
              "concurrent_function_invocation",
              (r, threadId, operationId) -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    try (final var instance = r.instantiate(module)) {
                      final var addFunction = instance.getFunction("add");
                      if (addFunction.isPresent()) {
                        // Multiple function calls per operation
                        for (int i = 0; i < 5; i++) {
                          final int a = threadId + i;
                          final int b = operationId + i;
                          final var args =
                              new ai.tegmentum.wasmtime4j.WasmValue[] {
                                ai.tegmentum.wasmtime4j.WasmValue.i32(a),
                                ai.tegmentum.wasmtime4j.WasmValue.i32(b)
                              };
                          final var callResult = addFunction.get().call(args);
                          assertThat(callResult[0].asI32()).isEqualTo(a + b);
                        }
                      }
                    }
                  }
                }
              },
              8, // 8 threads
              6); // 6 operations per thread

      assertThreadSafety(result, "concurrent_function_invocation");
      LOGGER.info(
          String.format(
              "Concurrent function invocation completed: %.2f ops/sec",
              result.getOperationsPerSecond()));
    }
  }

  @Test
  @Order(9)
  @DisplayName("Should test cross-runtime instance compatibility")
  void shouldTestCrossRuntimeInstanceCompatibility() {
    final byte[] moduleBytes = testUtils.getComplexModule();

    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> results =
        testCrossRuntimeCompatibility(
            "cross_runtime_instance_test",
            runtime -> {
              try (final var engine = runtime.createEngine()) {
                try (final var module = engine.compileModule(moduleBytes)) {
                  try (final var instance = runtime.instantiate(module)) {
                    // Test instance functionality across runtimes
                    exerciseInstanceOperations(instance);

                    // Test specific function calls
                    if (instance.getFunction("fibonacci").isPresent()) {
                      final var fibFunction = instance.getFunction("fibonacci");
                      if (fibFunction.isPresent()) {
                        final var args =
                            new ai.tegmentum.wasmtime4j.WasmValue[] {
                              ai.tegmentum.wasmtime4j.WasmValue.i32(10)
                            };
                        final var result = fibFunction.get().call(args);
                        // Fibonacci(10) = 55
                        assertThat(result[0].asI32()).isEqualTo(55);
                      }
                    }

                    // Test memory operations
                    if (instance.getFunction("store_load").isPresent()) {
                      final var storeLoadFunc = instance.getFunction("store_load");
                      if (storeLoadFunc.isPresent()) {
                        final var args =
                            new ai.tegmentum.wasmtime4j.WasmValue[] {
                              ai.tegmentum.wasmtime4j.WasmValue.i32(0), // address
                              ai.tegmentum.wasmtime4j.WasmValue.i32(123) // value
                            };
                        final var result = storeLoadFunc.get().call(args);
                        assertThat(result[0].asI32()).isEqualTo(123);
                      }
                    }
                  }
                }
              }
            });

    results.forEach(
        (runtimeType, result) -> {
          assertNoMemoryLeaks(result, "cross_runtime_instance_test_" + runtimeType);
        });

    LOGGER.info(
        "Cross-runtime instance compatibility test completed for " + results.size() + " runtimes");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 15, 30})
  @Order(10)
  @DisplayName("Should handle multiple instance lifecycle cycles")
  void shouldHandleMultipleInstanceLifecycleCycles(final int cycleCount) throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithThoroughMemoryLeakDetection(
              "instance_lifecycle_cycles_" + cycleCount,
              r -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    final AtomicInteger successCount = new AtomicInteger(0);

                    for (int i = 0; i < cycleCount; i++) {
                      try (final var instance = r.instantiate(module)) {
                        assertThat(instance).isNotNull();
                        exerciseInstanceOperations(instance);
                        successCount.incrementAndGet();
                      }
                    }

                    assertThat(successCount.get()).isEqualTo(cycleCount);
                  }
                }
              });

      assertNoMemoryLeaks(result, "instance_lifecycle_cycles_" + cycleCount);
      LOGGER.info("Instance lifecycle cycles (" + cycleCount + ") completed without leaks");
    }
  }

  @Test
  @Order(11)
  @DisplayName("Should handle instance resource lifecycle patterns")
  void shouldHandleInstanceResourceLifecyclePatterns() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();
      final var lifecycleData = testUtils.createResourceLifecycleTest();

      try (final var engine = runtime.createEngine();
          final var module = engine.compileModule(moduleBytes)) {

        for (final var testCase : lifecycleData.getTestCases()) {
          LOGGER.info("Testing instance lifecycle pattern: " + testCase.getName());

          switch (testCase.getPattern()) {
            case NORMAL:
              // Normal lifecycle
              assertDoesNotThrow(
                  () -> {
                    try (final var instance = runtime.instantiate(module)) {
                      exerciseInstanceOperations(instance);
                    }
                  });
              break;

            case DOUBLE_CLOSE:
              // Double close
              assertDoesNotThrow(
                  () -> {
                    final var instance = runtime.instantiate(module);
                    instance.close();
                    instance.close(); // Should not throw
                  });
              break;

            case USE_AFTER_CLOSE:
              // Use after close
              try {
                final var closedInstance = runtime.instantiate(module);
                closedInstance.close();

                assertThrows(
                    Exception.class,
                    () -> exerciseInstanceOperations(closedInstance),
                    "Should throw exception when using closed instance");
              } catch (final Exception e) {
                LOGGER.fine("Expected exception during USE_AFTER_CLOSE test: " + e.getMessage());
              }
              break;

            case RAPID_CYCLES:
              // Rapid cycles
              final AtomicInteger cycleCount = new AtomicInteger(0);
              assertDoesNotThrow(
                  () -> {
                    for (int i = 0; i < 25; i++) {
                      try (final var instance = runtime.instantiate(module)) {
                        exerciseInstanceOperations(instance);
                        cycleCount.incrementAndGet();
                      }
                    }
                  });
              assertThat(cycleCount.get()).isEqualTo(25);
              break;

            case NO_CLOSE:
              // No close - test for leaks
              final MemoryLeakDetector.LeakAnalysisResult result =
                  testWithThoroughMemoryLeakDetection(
                      "instance_no_close_lifecycle",
                      r -> {
                        try (final var eng = r.createEngine()) {
                          try (final var mod = eng.compileModule(moduleBytes)) {
                            for (int i = 0; i < 3; i++) {
                              final var instance = r.instantiate(mod);
                              exerciseInstanceOperations(instance);
                              // Intentionally not closing
                            }
                            System.gc();
                            System.gc();
                          }
                        }
                      });

              LOGGER.info("Instance no-close test result: " + result.isLeakDetected());
              break;
            default:
              throw new IllegalArgumentException(
                  "Unsupported lifecycle pattern: " + testCase.getPattern());
          }
        }
      } catch (final Exception e) {
        LOGGER.warning(
            "Exception during instance resource lifecycle patterns test: " + e.getMessage());
        throw new RuntimeException("Instance resource lifecycle patterns test failed", e);
      }
    }
  }

  /**
   * Exercises basic instance operations to test native functions.
   *
   * @param instance the instance to exercise
   */
  private void exerciseInstanceOperations(final Object instance) {
    assertDoesNotThrow(
        () -> {
          if (instance instanceof ai.tegmentum.wasmtime4j.Instance) {
            final var wasmInstance = (ai.tegmentum.wasmtime4j.Instance) instance;

            // Test export checking
            final boolean hasAdd = wasmInstance.getFunction("add").isPresent();
            LOGGER.fine("Instance has 'add' export: " + hasAdd);

            // Test function access
            final var addFunction = wasmInstance.getFunction("add");
            if (addFunction.isPresent()) {
              final var args =
                  new ai.tegmentum.wasmtime4j.WasmValue[] {
                    ai.tegmentum.wasmtime4j.WasmValue.i32(5),
                    ai.tegmentum.wasmtime4j.WasmValue.i32(3)
                  };
              final var result = addFunction.get().call(args);
              assertThat(result[0].asI32()).isEqualTo(8);
            }
          }
        });
  }

  /**
   * Exercises memory-related operations.
   *
   * @param instance the instance to test
   */
  private void exerciseMemoryOperations(final Object instance) {
    assertDoesNotThrow(
        () -> {
          if (instance instanceof ai.tegmentum.wasmtime4j.Instance) {
            final var wasmInstance = (ai.tegmentum.wasmtime4j.Instance) instance;

            // Test memory access if available
            final var memory = wasmInstance.getMemory("memory");
            if (memory.isPresent()) {
              LOGGER.fine("Instance has memory export");
            }

            // Test memory functions
            final var getSizeFunc = wasmInstance.getFunction("get_memory_size");
            if (getSizeFunc.isPresent()) {
              final var result = getSizeFunc.get().call();
              LOGGER.fine("Memory size: " + (result.length > 0 ? result[0].asI32() : "unknown"));
            }
          }
        });
  }

  /**
   * Exercises table-related operations.
   *
   * @param instance the instance to test
   */
  private void exerciseTableOperations(final Object instance) {
    assertDoesNotThrow(
        () -> {
          if (instance instanceof ai.tegmentum.wasmtime4j.Instance) {
            final var wasmInstance = (ai.tegmentum.wasmtime4j.Instance) instance;

            // Test table access if available
            final var table = wasmInstance.getTable("table");
            if (table.isPresent()) {
              LOGGER.fine("Instance has table export");
            }

            // Test table functions
            final var getTableSizeFunc = wasmInstance.getFunction("get_table_size");
            if (getTableSizeFunc.isPresent()) {
              final var result = getTableSizeFunc.get().call();
              LOGGER.fine("Table size: " + (result.length > 0 ? result[0].asI32() : "unknown"));
            }
          }
        });
  }

  /**
   * Exercises global variable operations.
   *
   * @param instance the instance to test
   */
  private void exerciseGlobalOperations(final Object instance) {
    assertDoesNotThrow(
        () -> {
          if (instance instanceof ai.tegmentum.wasmtime4j.Instance) {
            final var wasmInstance = (ai.tegmentum.wasmtime4j.Instance) instance;

            // Test global access if available
            final var global = wasmInstance.getGlobal("counter");
            if (global.isPresent()) {
              LOGGER.fine("Instance has counter global");
            }

            // Test global functions
            final var getCounterFunc = wasmInstance.getFunction("get_counter");
            if (getCounterFunc.isPresent()) {
              final var result = getCounterFunc.get().call();
              final int counterValue = result.length > 0 ? result[0].asI32() : 0;
              LOGGER.fine("Counter value: " + counterValue);

              // Test increment
              final var incrementFunc = wasmInstance.getFunction("increment_counter");
              if (incrementFunc.isPresent()) {
                incrementFunc.get().call();

                // Verify increment
                final var newResult = getCounterFunc.get().call();
                final int newCounterValue = newResult.length > 0 ? newResult[0].asI32() : 0;
                assertThat(newCounterValue).isEqualTo(counterValue + 1);
              }
            }
          }
        });
  }
}
