package ai.tegmentum.wasmtime4j.native_functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.memory.MemoryLeakDetector;
import ai.tegmentum.wasmtime4j.native_functions.NativeFunctionTestUtils.ParameterFuzzingData;
import ai.tegmentum.wasmtime4j.native_functions.NativeFunctionTestUtils.TestModule;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive native function tests for Engine operations. Tests all native functions related to
 * WebAssembly engine creation, configuration, compilation, and resource management for both JNI
 * and Panama FFI implementations.
 *
 * <p>This test class validates:
 *
 * <ul>
 *   <li>Engine creation and destruction native functions
 *   <li>Module compilation native functions
 *   <li>Store creation native functions
 *   <li>Engine configuration native functions
 *   <li>Memory leak detection for all engine operations
 *   <li>Thread safety of concurrent engine operations
 *   <li>Parameter validation and error handling
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Engine Native Function Tests")
public class EngineNativeFunctionTest extends BaseNativeFunctionTest {

  @Test
  @Order(1)
  @DisplayName("Should create and destroy engine without memory leaks")
  void shouldCreateAndDestroyEngineWithoutMemoryLeaks() {
    // Test engine creation/destruction with memory leak detection
    final MemoryLeakDetector.LeakAnalysisResult result =
        testWithFastMemoryLeakDetection(
            "engine_create_destroy",
            runtime -> {
              // Create engine
              final var engine = runtime.createEngine();
              assertThat(engine).isNotNull();

              // Use engine briefly
              final byte[] moduleBytes = testUtils.getSimpleAddModule();
              final var module = engine.compileModule(moduleBytes);
              assertThat(module).isNotNull();
              module.close();

              // Close engine
              engine.close();
            });

    // Validate no memory leaks
    assertNoMemoryLeaks(result, "engine_create_destroy");
    LOGGER.info("Engine creation/destruction passed memory leak test");
  }

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @Order(2)
  @DisplayName("Should create engine across all runtime types")
  void shouldCreateEngineAcrossAllRuntimeTypes(final RuntimeType runtimeType) {
    // Skip Panama tests if not available
    if (runtimeType == RuntimeType.PANAMA && !TestUtils.isPanamaAvailable()) {
      LOGGER.info("Skipping Panama test - not available on this platform");
      return;
    }

    try (final WasmRuntime runtime = createRuntime(runtimeType)) {
      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "engine_create_" + runtimeType,
              r -> {
                try (final var engine = r.createEngine()) {
                  assertThat(engine).isNotNull();
                  
                  // Verify engine can be used for compilation
                  final byte[] moduleBytes = testUtils.getSimpleAddModule();
                  try (final var module = engine.compileModule(moduleBytes)) {
                    assertThat(module).isNotNull();
                  }
                }
              });

      assertNoMemoryLeaks(result, "engine_create_" + runtimeType);
    }
  }

  @Test
  @Order(3)
  @DisplayName("Should handle module compilation with memory leak detection")
  void shouldHandleModuleCompilationWithMemoryLeakDetection() {
    try (final WasmRuntime runtime = createRuntime()) {
      final List<TestModule> testModules = testUtils.getAllTestModules();

      for (final TestModule testModule : testModules) {
        final MemoryLeakDetector.LeakAnalysisResult result =
            testWithFastMemoryLeakDetection(
                "compile_module_" + testModule.getName(),
                r -> {
                  try (final var engine = r.createEngine()) {
                    try (final var module = engine.compileModule(testModule.getModuleBytes())) {
                      assertThat(module).isNotNull();
                      
                      // Verify module metadata if available
                      if (module.getExports() != null) {
                        assertThat(module.getExports()).isNotEmpty();
                      }
                    }
                  }
                });

        assertNoMemoryLeaks(result, "compile_module_" + testModule.getName());
      }
    }
  }

  @Test
  @Order(4)
  @DisplayName("Should handle store creation with memory leak detection")
  void shouldHandleStoreCreationWithMemoryLeakDetection() {
    try (final WasmRuntime runtime = createRuntime()) {
      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "store_creation",
              r -> {
                try (final var engine = r.createEngine()) {
                  // Create multiple stores to test native function
                  for (int i = 0; i < 5; i++) {
                    try (final var store = engine.createStore()) {
                      assertThat(store).isNotNull();
                    }
                  }
                }
              });

      assertNoMemoryLeaks(result, "store_creation");
    }
  }

  @Test
  @Order(5)
  @DisplayName("Should test thread safety of engine operations")
  void shouldTestThreadSafetyOfEngineOperations() {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();

      // Test concurrent engine operations
      final ThreadSafetyTestResult result =
          testThreadSafety(
              "concurrent_engine_operations",
              (r, threadId, operationId) -> {
                try (final var engine = r.createEngine()) {
                  // Compile module
                  try (final var module = engine.compileModule(moduleBytes)) {
                    // Create store
                    try (final var store = engine.createStore()) {
                      // Instantiate module
                      try (final var instance = r.instantiate(module)) {
                        assertThat(instance).isNotNull();
                      }
                    }
                  }
                }
              },
              4, // 4 threads
              10); // 10 operations per thread

      assertThreadSafety(result, "concurrent_engine_operations");
      LOGGER.info(
          String.format(
              "Thread safety test completed: %.2f ops/sec", result.getOperationsPerSecond()));
    }
  }

  @Test
  @Order(6)
  @DisplayName("Should test cross-runtime engine compatibility")
  void shouldTestCrossRuntimeEngineCompatibility() {
    final byte[] moduleBytes = testUtils.getSimpleAddModule();

    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> results =
        testCrossRuntimeCompatibility(
            "cross_runtime_engine_test",
            runtime -> {
              try (final var engine = runtime.createEngine()) {
                try (final var module = engine.compileModule(moduleBytes)) {
                  try (final var store = engine.createStore()) {
                    try (final var instance = runtime.instantiate(module)) {
                      // Call a function to verify full functionality
                      final var addFunction = instance.getFunction("add");
                      if (addFunction.isPresent()) {
                        final var args = new ai.tegmentum.wasmtime4j.WasmValue[] {
                          ai.tegmentum.wasmtime4j.WasmValue.i32(10),
                          ai.tegmentum.wasmtime4j.WasmValue.i32(20)
                        };
                        final var result = addFunction.get().call(args);
                        assertThat(result[0].asI32()).isEqualTo(30);
                      }
                    }
                  }
                }
              }
            });

    // Validate all available runtimes
    results.forEach(
        (runtimeType, result) -> {
          assertNoMemoryLeaks(result, "cross_runtime_engine_test_" + runtimeType);
        });

    LOGGER.info("Cross-runtime engine compatibility test completed for " + results.size() + " runtimes");
  }

  @Test
  @Order(7)
  @DisplayName("Should handle parameter fuzzing for engine operations")
  void shouldHandleParameterFuzzingForEngineOperations() {
    try (final WasmRuntime runtime = createRuntime()) {
      final ParameterFuzzingData fuzzingData = testUtils.generateFuzzingData();

      try (final var engine = runtime.createEngine()) {
        for (final Object[] testCase : fuzzingData.getTestCases()) {
          final String caseName = (String) testCase[0];
          final Object parameter = testCase[1];
          final String description = (String) testCase[2];

          LOGGER.fine("Testing parameter fuzzing case: " + caseName + " - " + description);

          if (parameter instanceof byte[]) {
            final byte[] moduleBytes = (byte[]) parameter;

            if (moduleBytes == null || moduleBytes.length == 0) {
              // Should throw appropriate exception for invalid input
              assertThrows(
                  Exception.class,
                  () -> engine.compileModule(moduleBytes),
                  "Should throw exception for invalid module bytes: " + caseName);
            } else {
              // May succeed or fail depending on the random data
              try {
                try (final var module = engine.compileModule(moduleBytes)) {
                  LOGGER.fine("Successfully compiled random module: " + caseName);
                }
              } catch (final CompilationException | ValidationException e) {
                // Expected for random/invalid data
                LOGGER.fine("Expected compilation failure for: " + caseName);
              }
            }
          }
        }
      }
    }
  }

  @Test
  @Order(8)
  @DisplayName("Should handle resource lifecycle patterns")
  void shouldHandleResourceLifecyclePatterns() {
    try (final WasmRuntime runtime = createRuntime()) {
      final var lifecycleData = testUtils.createResourceLifecycleTest();
      final byte[] moduleBytes = testUtils.getSimpleAddModule();

      for (final var testCase : lifecycleData.getTestCases()) {
        LOGGER.info("Testing lifecycle pattern: " + testCase.getName());

        switch (testCase.getPattern()) {
          case NORMAL:
            // Normal lifecycle: create -> use -> close
            assertDoesNotThrow(() -> {
              try (final var engine = runtime.createEngine()) {
                try (final var module = engine.compileModule(moduleBytes)) {
                  assertThat(module).isNotNull();
                }
              }
            });
            break;

          case DOUBLE_CLOSE:
            // Double close: create -> close -> close
            assertDoesNotThrow(() -> {
              final var engine = runtime.createEngine();
              engine.close();
              engine.close(); // Should not throw
            });
            break;

          case USE_AFTER_CLOSE:
            // Use after close: create -> close -> use (should throw)
            final var closedEngine = runtime.createEngine();
            closedEngine.close();
            
            assertThrows(
                Exception.class,
                () -> closedEngine.compileModule(moduleBytes),
                "Should throw exception when using closed engine");
            break;

          case RAPID_CYCLES:
            // Rapid create/close cycles
            final AtomicInteger cycleCount = new AtomicInteger(0);
            assertDoesNotThrow(() -> {
              for (int i = 0; i < 100; i++) {
                try (final var engine = runtime.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    cycleCount.incrementAndGet();
                  }
                }
              }
            });
            assertThat(cycleCount.get()).isEqualTo(100);
            break;

          case NO_CLOSE:
            // No close - test for resource leaks (handled by memory leak detector)
            final MemoryLeakDetector.LeakAnalysisResult result =
                testWithThoroughMemoryLeakDetection(
                    "no_close_lifecycle",
                    r -> {
                      // Create resources without explicit close (relies on GC)
                      for (int i = 0; i < 10; i++) {
                        final var engine = r.createEngine();
                        final var module = engine.compileModule(moduleBytes);
                        // Intentionally not closing to test leak detection
                      }
                      // Force GC to see if finalizers clean up
                      System.gc();
                      System.gc();
                    });

            // This test may or may not show leaks depending on GC behavior
            LOGGER.info("No-close test result: " + result.isLeakDetected());
            break;
        }
      }
    }
  }

  @Test
  @Order(9)
  @DisplayName("Should handle invalid native handles")
  void shouldHandleInvalidNativeHandles() {
    try (final WasmRuntime runtime = createRuntime()) {
      final long[] invalidHandles = testUtils.getInvalidHandles();
      final long[] edgeCaseHandles = testUtils.getEdgeCaseHandles();

      LOGGER.info("Testing invalid native handles");

      // Test invalid handles (implementation-specific behavior)
      for (final long invalidHandle : invalidHandles) {
        LOGGER.fine("Testing invalid handle: 0x" + Long.toHexString(invalidHandle));
        
        // The exact behavior depends on implementation, but it should not crash the JVM
        assertDoesNotThrow(() -> {
          // Attempt to use invalid handle - should either throw exception or handle gracefully
          testUtils.isValidNativeHandle(invalidHandle);
        });
      }

      // Test edge case handles
      for (final long edgeCaseHandle : edgeCaseHandles) {
        LOGGER.fine("Testing edge case handle: 0x" + Long.toHexString(edgeCaseHandle));
        
        assertDoesNotThrow(() -> {
          testUtils.isValidNativeHandle(edgeCaseHandle);
        });
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 4, 8, 16})
  @Order(10)
  @DisplayName("Should handle concurrent engine creation")
  void shouldHandleConcurrentEngineCreation(final int threadCount) {
    try (final WasmRuntime runtime = createRuntime()) {
      final ThreadSafetyTestResult result =
          testThreadSafety(
              "concurrent_engine_creation_" + threadCount + "_threads",
              (r, threadId, operationId) -> {
                try (final var engine = r.createEngine()) {
                  assertThat(engine).isNotNull();
                  
                  // Verify engine functionality
                  final byte[] moduleBytes = testUtils.getSimpleAddModule();
                  try (final var module = engine.compileModule(moduleBytes)) {
                    assertThat(module).isNotNull();
                  }
                }
              },
              threadCount,
              5); // 5 operations per thread

      assertThreadSafety(result, "concurrent_engine_creation_" + threadCount + "_threads");
      LOGGER.info(
          String.format(
              "Concurrent engine creation (%d threads) completed: %.2f ops/sec",
              threadCount, result.getOperationsPerSecond()));
    }
  }

  @Test
  @Order(11)
  @EnabledIf("isPanamaAvailable")
  @DisplayName("Should test Panama-specific FFI error handling")
  void shouldTestPanamaSpecificFfiErrorHandling() {
    // This test is specific to Panama FFI implementation
    try (final WasmRuntime runtime = createRuntime(RuntimeType.PANAMA)) {
      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "panama_ffi_error_handling",
              r -> {
                try (final var engine = r.createEngine()) {
                  // Test various error conditions specific to Panama FFI
                  final byte[] invalidModule = new byte[] {0x00, 0x61, 0x73, 0x6D}; // Incomplete header
                  
                  assertThrows(
                      Exception.class,
                      () -> engine.compileModule(invalidModule),
                      "Should handle Panama FFI compilation errors");
                      
                  // Test with completely invalid data
                  final byte[] corruptedModule = new byte[1024];
                  java.util.Arrays.fill(corruptedModule, (byte) 0xFF);
                  
                  assertThrows(
                      Exception.class,
                      () -> engine.compileModule(corruptedModule),
                      "Should handle corrupted module data in Panama FFI");
                }
              });

      assertNoMemoryLeaks(result, "panama_ffi_error_handling");
    }
  }

  /**
   * Utility method to check if Panama is available for conditional tests.
   *
   * @return true if Panama FFI is available
   */
  static boolean isPanamaAvailable() {
    return TestUtils.isPanamaAvailable();
  }
}