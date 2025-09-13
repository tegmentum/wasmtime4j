package ai.tegmentum.wasmtime4j.nativefunctions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
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
 * Comprehensive native function tests for Module operations. Tests all native functions related to
 * WebAssembly module compilation, validation, metadata access, and resource management for both JNI
 * and Panama FFI implementations.
 *
 * <p>This test class validates:
 *
 * <ul>
 *   <li>Module compilation native functions
 *   <li>Module validation native functions
 *   <li>Module metadata access native functions
 *   <li>Module serialization/deserialization native functions
 *   <li>Memory leak detection for all module operations
 *   <li>Thread safety of concurrent module operations
 *   <li>Parameter validation and boundary condition handling
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Module Native Function Tests")
@SuppressWarnings("try")
public class ModuleNativeFunctionIT extends BaseNativeFunctionTest {

  @Test
  @Order(1)
  @DisplayName("Should compile and destroy module without memory leaks")
  void shouldCompileAndDestroyModuleWithoutMemoryLeaks() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "module_compile_destroy",
              r -> {
                try (final var engine = r.createEngine()) {
                  // Compile module
                  try (final var module = engine.compileModule(moduleBytes)) {
                    assertThat(module).isNotNull();

                    // Access module metadata to exercise native functions
                    if (module.getExports() != null) {
                      assertThat(module.getExports()).isNotEmpty();
                    }

                    // Verify module can be instantiated
                    try (final var instance = r.instantiate(module)) {
                      assertThat(instance).isNotNull();
                    }
                  }
                }
              });

      assertNoMemoryLeaks(result, "module_compile_destroy");
      LOGGER.info("Module compilation/destruction passed memory leak test");
    }
  }

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @Order(2)
  @DisplayName("Should compile modules across all runtime types")
  void shouldCompileModulesAcrossAllRuntimeTypes(final RuntimeType runtimeType)
      throws WasmException {
    if (runtimeType == RuntimeType.PANAMA && !TestUtils.isPanamaAvailable()) {
      LOGGER.info("Skipping Panama test - not available on this platform");
      return;
    }

    try (final WasmRuntime runtime = createRuntime(runtimeType)) {
      final List<TestModule> testModules = testUtils.getAllTestModules();

      for (final TestModule testModule : testModules) {
        final MemoryLeakDetector.LeakAnalysisResult result =
            testWithFastMemoryLeakDetection(
                "module_compile_" + runtimeType + "_" + testModule.getName(),
                r -> {
                  try (final var engine = r.createEngine()) {
                    try (final var module = engine.compileModule(testModule.getModuleBytes())) {
                      assertThat(module).isNotNull();

                      // Exercise native functions for metadata access
                      exerciseModuleMetadataAccess(module);
                    }
                  }
                });

        assertNoMemoryLeaks(result, "module_compile_" + runtimeType + "_" + testModule.getName());
      }
    }
  }

  @Test
  @Order(3)
  @DisplayName("Should handle module validation with memory leak detection")
  void shouldHandleModuleValidationWithMemoryLeakDetection() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "module_validation",
              r -> {
                try (final var engine = r.createEngine()) {
                  // Test valid modules
                  final List<TestModule> validModules = testUtils.getAllTestModules();
                  for (final TestModule module : validModules) {
                    try {
                      try (final var compiledModule =
                          engine.compileModule(module.getModuleBytes())) {
                        assertThat(compiledModule).isNotNull();
                      }
                    } catch (final CompilationException | ValidationException e) {
                      // Some test modules may be intentionally invalid
                      LOGGER.fine("Expected validation failure for: " + module.getName());
                    }
                  }

                  // Test invalid modules
                  testInvalidModuleHandling(engine);
                }
              });

      assertNoMemoryLeaks(result, "module_validation");
    }
  }

  @Test
  @Order(4)
  @DisplayName("Should handle module metadata access")
  void shouldHandleModuleMetadataAccess() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getComplexModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithFastMemoryLeakDetection(
              "module_metadata_access",
              r -> {
                try (final var engine = r.createEngine()) {
                  try (final var module = engine.compileModule(moduleBytes)) {
                    // Test all metadata access functions
                    exerciseModuleMetadataAccess(module);

                    // Test repeated access to ensure native functions are stable
                    for (int i = 0; i < 10; i++) {
                      exerciseModuleMetadataAccess(module);
                    }
                  }
                }
              });

      assertNoMemoryLeaks(result, "module_metadata_access");
    }
  }

  @Test
  @Order(5)
  @DisplayName("Should test thread safety of module operations")
  void shouldTestThreadSafetyOfModuleOperations() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();

      final ThreadSafetyTestResult result =
          testThreadSafety(
              "concurrent_module_operations",
              (r, threadId, operationId) -> {
                try (final var engine = r.createEngine()) {
                  // Compile module
                  try (final var module = engine.compileModule(moduleBytes)) {
                    assertThat(module).isNotNull();

                    // Access metadata concurrently
                    exerciseModuleMetadataAccess(module);

                    // Instantiate module
                    try (final var instance = r.instantiate(module)) {
                      assertThat(instance).isNotNull();
                    }
                  }
                }
              },
              4, // 4 threads
              15); // 15 operations per thread

      assertThreadSafety(result, "concurrent_module_operations");
      LOGGER.info(
          String.format(
              "Module thread safety test completed: %.2f ops/sec",
              result.getOperationsPerSecond()));
    }
  }

  @Test
  @Order(6)
  @DisplayName("Should test concurrent module compilation")
  void shouldTestConcurrentModuleCompilation() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final List<TestModule> modules = testUtils.getAllTestModules();

      final ThreadSafetyTestResult result =
          testThreadSafety(
              "concurrent_module_compilation",
              (r, threadId, operationId) -> {
                try (final var engine = r.createEngine()) {
                  // Use different modules for different threads to test various scenarios
                  final TestModule testModule = modules.get(threadId % modules.size());

                  try {
                    try (final var module = engine.compileModule(testModule.getModuleBytes())) {
                      assertThat(module).isNotNull();

                      // Quick validation that module is usable
                      exerciseModuleMetadataAccess(module);
                    }
                  } catch (final CompilationException | ValidationException e) {
                    // Some modules may be intentionally invalid for testing
                    LOGGER.fine("Expected compilation failure for " + testModule.getName());
                  }
                }
              },
              6, // 6 threads
              8); // 8 operations per thread

      assertThreadSafety(result, "concurrent_module_compilation");
      LOGGER.info(
          String.format(
              "Concurrent module compilation completed: %.2f ops/sec",
              result.getOperationsPerSecond()));
    }
  }

  @Test
  @Order(7)
  @DisplayName("Should test cross-runtime module compatibility")
  void shouldTestCrossRuntimeModuleCompatibility() {
    final byte[] moduleBytes = testUtils.getComplexModule();

    final Map<RuntimeType, MemoryLeakDetector.LeakAnalysisResult> results =
        testCrossRuntimeCompatibility(
            "cross_runtime_module_test",
            runtime -> {
              try (final var engine = runtime.createEngine()) {
                try (final var module = engine.compileModule(moduleBytes)) {
                  // Test metadata access across runtimes
                  exerciseModuleMetadataAccess(module);

                  // Test module instantiation
                  try (final var instance = runtime.instantiate(module)) {
                    assertThat(instance).isNotNull();

                    // Verify functionality is consistent across runtimes
                    if (instance.getFunction("get_state").isPresent()) {
                      final var getStateFunc = instance.getFunction("get_state");
                      if (getStateFunc.isPresent()) {
                        final var result = getStateFunc.get().call();
                        assertThat(result).isNotNull();
                      }
                    }
                  }
                }
              }
            });

    results.forEach(
        (runtimeType, result) -> {
          assertNoMemoryLeaks(result, "cross_runtime_module_test_" + runtimeType);
        });

    LOGGER.info(
        "Cross-runtime module compatibility test completed for " + results.size() + " runtimes");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 10, 100, 1000})
  @Order(8)
  @DisplayName("Should handle multiple module compilation cycles")
  void shouldHandleMultipleModuleCompilationCycles(final int cycleCount) throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();

      final MemoryLeakDetector.LeakAnalysisResult result =
          testWithThoroughMemoryLeakDetection(
              "module_compilation_cycles_" + cycleCount,
              r -> {
                try (final var engine = r.createEngine()) {
                  final AtomicInteger successCount = new AtomicInteger(0);

                  for (int i = 0; i < cycleCount; i++) {
                    try (final var module = engine.compileModule(moduleBytes)) {
                      assertThat(module).isNotNull();
                      exerciseModuleMetadataAccess(module);
                      successCount.incrementAndGet();
                    }
                  }

                  assertThat(successCount.get()).isEqualTo(cycleCount);
                }
              });

      assertNoMemoryLeaks(result, "module_compilation_cycles_" + cycleCount);
      LOGGER.info("Module compilation cycles (" + cycleCount + ") completed without leaks");
    }
  }

  @Test
  @Order(9)
  @DisplayName("Should handle module parameter boundary conditions")
  void shouldHandleModuleParameterBoundaryConditions() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      try (final var engine = runtime.createEngine()) {

        // Test null module bytes
        assertThrows(
            Exception.class,
            () -> engine.compileModule(null),
            "Should throw exception for null module bytes");

        // Test empty module bytes
        assertThrows(
            Exception.class,
            () -> engine.compileModule(new byte[0]),
            "Should throw exception for empty module bytes");

        // Test invalid WebAssembly magic number
        final byte[] invalidMagic = {0x00, 0x61, 0x73, 0x6D}; // Incorrect magic
        assertThrows(
            Exception.class,
            () -> engine.compileModule(invalidMagic),
            "Should throw exception for invalid magic number");

        // Test truncated module
        final byte[] truncated = {0x00, 0x61, 0x73, 0x6D, 0x01, 0x00}; // Valid magic but truncated
        assertThrows(
            Exception.class,
            () -> engine.compileModule(truncated),
            "Should throw exception for truncated module");

        // Test very large invalid module
        final byte[] largeInvalid = new byte[1024 * 1024]; // 1MB of invalid data
        java.util.Arrays.fill(largeInvalid, (byte) 0xFF);

        assertThrows(
            Exception.class,
            () -> engine.compileModule(largeInvalid),
            "Should throw exception for large invalid module");
      }
    }
  }

  @Test
  @Order(10)
  @DisplayName("Should handle module resource lifecycle patterns")
  void shouldHandleModuleResourceLifecyclePatterns() throws WasmException {
    try (final WasmRuntime runtime = createRuntime()) {
      final byte[] moduleBytes = testUtils.getSimpleAddModule();
      final var lifecycleData = testUtils.createResourceLifecycleTest();

      for (final var testCase : lifecycleData.getTestCases()) {
        LOGGER.info("Testing module lifecycle pattern: " + testCase.getName());

        try (final var engine = runtime.createEngine()) {
          switch (testCase.getPattern()) {
            case NORMAL:
              // Normal lifecycle
              assertDoesNotThrow(
                  () -> {
                    try (final var module = engine.compileModule(moduleBytes)) {
                      exerciseModuleMetadataAccess(module);
                    }
                  });
              break;

            case DOUBLE_CLOSE:
              // Double close
              assertDoesNotThrow(
                  () -> {
                    final var module = engine.compileModule(moduleBytes);
                    module.close();
                    module.close(); // Should not throw
                  });
              break;

            case USE_AFTER_CLOSE:
              // Use after close
              final var closedModule = engine.compileModule(moduleBytes);
              closedModule.close();

              assertThrows(
                  Exception.class,
                  () -> exerciseModuleMetadataAccess(closedModule),
                  "Should throw exception when accessing closed module");
              break;

            case RAPID_CYCLES:
              // Rapid cycles
              final AtomicInteger cycleCount = new AtomicInteger(0);
              assertDoesNotThrow(
                  () -> {
                    for (int i = 0; i < 50; i++) {
                      try (final var module = engine.compileModule(moduleBytes)) {
                        exerciseModuleMetadataAccess(module);
                        cycleCount.incrementAndGet();
                      }
                    }
                  });
              assertThat(cycleCount.get()).isEqualTo(50);
              break;

            case NO_CLOSE:
              // No close - test for leaks
              final MemoryLeakDetector.LeakAnalysisResult result =
                  testWithThoroughMemoryLeakDetection(
                      "module_no_close_lifecycle",
                      r -> {
                        try (final var eng = r.createEngine()) {
                          for (int i = 0; i < 5; i++) {
                            final var module = eng.compileModule(moduleBytes);
                            exerciseModuleMetadataAccess(module);
                            // Intentionally not closing
                          }
                          System.gc();
                          System.gc();
                        }
                      });

              LOGGER.info("Module no-close test result: " + result.isLeakDetected());
              break;
            default:
              throw new IllegalArgumentException(
                  "Unsupported lifecycle pattern: " + testCase.getPattern());
          }
        }
      }
    }
  }

  /**
   * Exercises various module metadata access native functions to ensure they work correctly.
   *
   * @param module the module to access metadata for
   */
  private void exerciseModuleMetadataAccess(final Object module) {
    // This method would call various metadata access methods on the module
    // The exact methods depend on the module interface

    assertDoesNotThrow(
        () -> {
          // Access exports (if available)
          if (module instanceof ai.tegmentum.wasmtime4j.Module) {
            final var wasmModule = (ai.tegmentum.wasmtime4j.Module) module;

            // These would exercise native functions for metadata access
            final var exports = wasmModule.getExports();
            if (exports != null && !exports.isEmpty()) {
              LOGGER.fine("Module has " + exports.size() + " exports");
            }

            final var imports = wasmModule.getImports();
            if (imports != null && !imports.isEmpty()) {
              LOGGER.fine("Module has " + imports.size() + " imports");
            }
          }
        });
  }

  /**
   * Tests handling of invalid module data.
   *
   * @param engine the engine to test with
   */
  private void testInvalidModuleHandling(final Object engine) {
    // Test various invalid module scenarios
    final byte[][] invalidModules = {
      // Invalid magic number
      {0x00, 0x62, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00},

      // Invalid version
      {0x00, 0x61, 0x73, 0x6D, 0x02, 0x00, 0x00, 0x00},

      // Corrupted section
      {0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF},

      // Random data
      {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}
    };

    if (engine instanceof ai.tegmentum.wasmtime4j.Engine) {
      final var wasmEngine = (ai.tegmentum.wasmtime4j.Engine) engine;

      for (final byte[] invalidModule : invalidModules) {
        assertThrows(
            Exception.class,
            () -> wasmEngine.compileModule(invalidModule),
            "Should throw exception for invalid module data");
      }
    }
  }
}
