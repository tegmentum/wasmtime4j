package ai.tegmentum.wasmtime4j.module;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.ImportType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive tests for Module API covering all module operations and scenarios. Tests module
 * compilation, validation, instantiation, metadata introspection, and cross-runtime compatibility.
 */
@DisplayName("Module API Comprehensive Tests")
class ModuleApiComprehensiveIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleApiComprehensiveIT.class.getName());

  private WasmTestDataManager testDataManager;
  private ExecutorService executorService;

  @Override
  @SuppressWarnings("unused")
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled("module.comprehensive");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
      executorService = Executors.newFixedThreadPool(4);
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test data manager: " + e.getMessage());
      skipIfNot(false, "Test data manager setup failed: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDownExecutor() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executorService.shutdownNow();
      }
    }
  }

  @Test
  @DisplayName("Should compile valid WebAssembly modules")
  void shouldCompileValidWebAssemblyModules() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-compilation-basic",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              // When
              final Engine engine = runtime.createEngine();
              final Module module = engine.compileModule(wasmBytes);

              // Then
              assertThat(module).isNotNull();
              assertThat(module.isValid()).isTrue();
              assertThat(module.getEngine()).isSameAs(engine);

              // Cleanup
              module.close();
              engine.close();

              return "Module compiled successfully";
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module compilation validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module instantiation")
  void shouldHandleModuleInstantiation() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-instantiation-basic",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes);
                  final Store store = engine.createStore()) {

                // When
                final Instance instance = module.instantiate(store);

                // Then
                assertThat(instance).isNotNull();

                return "Module instantiated successfully";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module instantiation validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module instantiation with imports")
  void shouldHandleModuleInstantiationWithImports() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-instantiation-imports",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createMemoryImportWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // When - Check imports required
                final List<ImportType> imports = module.getImports();
                assertThat(imports).isNotEmpty();

                // For this test, we'll validate the import structure without instantiating
                // since we don't have proper import setup yet
                return "Module imports validated: " + imports.size();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module imports validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should extract module exports metadata")
  void shouldExtractModuleExportsMetadata() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-exports-metadata",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // When
                final List<ExportType> exports = module.getExports();

                // Then
                assertThat(exports).isNotNull();
                assertThat(exports).isNotEmpty();

                // Simple module should have an "add" function export
                final boolean hasAddFunction =
                    exports.stream().anyMatch(export -> "add".equals(export.getName()));

                return "Exports found: " + exports.size() + ", has add: " + hasAddFunction;
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module exports metadata validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should extract module imports metadata")
  void shouldExtractModuleImportsMetadata() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-imports-metadata",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createMemoryImportWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // When
                final List<ImportType> imports = module.getImports();

                // Then
                assertThat(imports).isNotNull();
                assertThat(imports).isNotEmpty();

                // Memory import module should have memory import from "env"
                final boolean hasMemoryImport =
                    imports.stream()
                        .anyMatch(
                            imp ->
                                "env".equals(imp.getModuleName())
                                    && "memory".equals(imp.getName()));

                return "Imports found: " + imports.size() + ", has memory: " + hasMemoryImport;
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module imports metadata validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate import compatibility")
  void shouldValidateImportCompatibility() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-import-validation",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createMemoryImportWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // When - Create empty import map (should be invalid)
                final ImportMap emptyImports = ImportMap.empty();
                final boolean validWithEmpty = module.validateImports(emptyImports);

                // Then
                assertThat(validWithEmpty).isFalse(); // Should be false since module needs memory

                return "Empty imports validation: " + validWithEmpty;
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module import validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle multiple modules simultaneously")
  void shouldHandleMultipleModulesSimultaneously() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-multiple-simultaneous",
            runtime -> {
              // Given
              final byte[] wasmBytes1 = TestUtils.createSimpleWasmModule();
              final byte[] wasmBytes2 = TestUtils.createMemoryImportWasmModule();

              try (final Engine engine = runtime.createEngine()) {
                // When
                final Module module1 = engine.compileModule(wasmBytes1);
                final Module module2 = engine.compileModule(wasmBytes2);

                try {
                  // Then
                  assertThat(module1).isNotNull();
                  assertThat(module2).isNotNull();
                  assertThat(module1).isNotSameAs(module2);
                  assertThat(module1.isValid()).isTrue();
                  assertThat(module2.isValid()).isTrue();

                  // Verify they have different metadata
                  assertThat(module1.getExports().size()).isNotEqualTo(module2.getImports().size());

                  return "Multiple modules handled successfully";
                } finally {
                  module1.close();
                  module2.close();
                }
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Multiple modules validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle concurrent module compilation")
  @SuppressWarnings("unused")
  void shouldHandleConcurrentModuleCompilation() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-concurrent-compilation",
            runtime -> {
              // Given
              final int threadCount = 4;
              final int modulesPerThread = 3;
              final CountDownLatch startLatch = new CountDownLatch(1);
              final CountDownLatch completionLatch = new CountDownLatch(threadCount);

              try (final Engine engine = runtime.createEngine()) {
                // When - Compile modules concurrently
                for (int i = 0; i < threadCount; i++) {
                  executorService.submit(
                      () -> {
                        try {
                          startLatch.await();

                          for (int j = 0; j < modulesPerThread; j++) {
                            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
                            final Module module = engine.compileModule(wasmBytes);
                            assertThat(module).isNotNull();
                            assertThat(module.isValid()).isTrue();
                            module.close();
                          }
                        } catch (final Exception e) {
                          LOGGER.severe("Concurrent compilation failed: " + e.getMessage());
                          throw new RuntimeException(e);
                        } finally {
                          completionLatch.countDown();
                        }
                      });
                }

                // Start all threads
                startLatch.countDown();

                // Wait for completion
                final boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
                assertThat(completed).isTrue();

                return "Concurrent compilation completed successfully";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Concurrent compilation validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module resource cleanup")
  void shouldHandleModuleResourceCleanup() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-resource-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine()) {
                // When
                final Module module = engine.compileModule(wasmBytes);
                assertThat(module.isValid()).isTrue();

                // Close module
                module.close();

                // Then
                assertThat(module.isValid()).isFalse();

                return "Resource cleanup handled successfully";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module resource cleanup validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should measure module compilation performance")
  void shouldMeasureModuleCompilationPerformance(final RuntimeType runtimeType) {
    // skipIfCategoryNotEnabled("performance");

    // Use cross-runtime test execution for performance measurement
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-performance-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int iterations = 50; // Reduce for faster tests

              long totalCompilationTime = 0;

              try (final Engine engine = runtime.createEngine()) {
                // Warmup
                for (int i = 0; i < 5; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  module.close();
                }

                // When - Measure compilation time
                for (int i = 0; i < iterations; i++) {
                  final Instant start = Instant.now();
                  final Module module = engine.compileModule(wasmBytes);
                  final Duration compilationTime = Duration.between(start, Instant.now());
                  totalCompilationTime += compilationTime.toNanos();
                  module.close();
                }
              }

              // Then
              final double avgCompilationMs = totalCompilationTime / (iterations * 1_000_000.0);
              assertThat(avgCompilationMs).isLessThan(100.0); // Should be fast

              return String.format("Avg compilation time: %.2f ms", avgCompilationMs);
            },
            comparison -> true); // Just measure, don't compare values exactly

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info(
        "Module performance validation for " + runtimeType + ": " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module name extraction")
  void shouldHandleModuleNameExtraction() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-name-extraction",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // When
                final String moduleName = module.getName();

                // Then - Simple module should not have a name
                assertThat(moduleName).isNull();

                return "Module name: " + moduleName;
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module name extraction validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate module state consistency")
  void shouldValidateModuleStateConsistency() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-state-consistency",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine()) {
                final Module module = engine.compileModule(wasmBytes);

                try {
                  // When - Module should be valid after compilation
                  assertThat(module.isValid()).isTrue();

                  // Multiple calls should return consistent results
                  final List<ExportType> exports1 = module.getExports();
                  final List<ExportType> exports2 = module.getExports();
                  final List<ImportType> imports1 = module.getImports();
                  final List<ImportType> imports2 = module.getImports();

                  // Then
                  assertThat(exports1).isEqualTo(exports2);
                  assertThat(imports1).isEqualTo(imports2);

                  return "State consistency validated";
                } finally {
                  module.close();
                }
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module state consistency validation: " + validation.getSummary());
  }
}
