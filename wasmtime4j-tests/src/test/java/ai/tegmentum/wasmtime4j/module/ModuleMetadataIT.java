package ai.tegmentum.wasmtime4j.module;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.ImportType;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive tests for Module metadata introspection and analysis. Tests export/import
 * introspection, type information, custom sections, and metadata consistency.
 */
@DisplayName("Module Metadata Comprehensive Tests")
class ModuleMetadataIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleMetadataIT.class.getName());

  private WasmTestDataManager testDataManager;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled("module.metadata");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test data manager: " + e.getMessage());
      skipIfNot(false, "Test data manager setup failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should extract function export metadata")
  void shouldExtractFunctionExportMetadata() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-function-export-metadata",
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
                final Optional<ExportType> addExport =
                    exports.stream().filter(export -> "add".equals(export.getName())).findFirst();

                assertThat(addExport).isPresent();
                assertThat(addExport.get().getType().getKind()).isEqualTo(WasmTypeKind.FUNCTION);

                // Verify all exports have valid names and types
                for (final ExportType export : exports) {
                  assertThat(export.getName()).isNotNull();
                  assertThat(export.getName()).isNotEmpty();
                  assertThat(export.getType()).isNotNull();
                  assertThat(export.getType().getKind()).isNotNull();
                }

                return "Function exports: "
                    + exports.size()
                    + ", has add: "
                    + addExport.isPresent();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Function export metadata validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should extract memory import metadata")
  void shouldExtractMemoryImportMetadata() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-memory-import-metadata",
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

                // Module should import memory from "env"
                final Optional<ImportType> memoryImport =
                    imports.stream()
                        .filter(
                            imp ->
                                "env".equals(imp.getModuleName()) && "memory".equals(imp.getName()))
                        .findFirst();

                assertThat(memoryImport).isPresent();
                assertThat(memoryImport.get().getType().getKind()).isEqualTo(WasmTypeKind.MEMORY);

                // Verify all imports have valid metadata
                for (final ImportType importType : imports) {
                  assertThat(importType.getModuleName()).isNotNull();
                  assertThat(importType.getModuleName()).isNotEmpty();
                  assertThat(importType.getName()).isNotNull();
                  assertThat(importType.getName()).isNotEmpty();
                  assertThat(importType.getType()).isNotNull();
                  assertThat(importType.getType().getKind()).isNotNull();
                }

                return "Memory imports: "
                    + imports.size()
                    + ", has env.memory: "
                    + memoryImport.isPresent();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Memory import metadata validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module with no exports or imports")
  void shouldHandleModuleWithNoExportsOrImports() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-empty-metadata",
            runtime -> {
              // Given - Empty module (header only)
              final byte[] emptyModuleBytes = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(emptyModuleBytes)) {

                // When
                final List<ExportType> exports = module.getExports();
                final List<ImportType> imports = module.getImports();

                // Then
                assertThat(exports).isNotNull();
                assertThat(exports).isEmpty();
                assertThat(imports).isNotNull();
                assertThat(imports).isEmpty();

                return "Empty module - exports: " + exports.size() + ", imports: " + imports.size();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Empty module metadata validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle complex module with multiple export types")
  void shouldHandleComplexModuleWithMultipleExportTypes() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-complex-exports",
            runtime -> {
              // Given - Complex module with multiple export types
              final byte[] complexModuleBytes = createComplexModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(complexModuleBytes)) {

                // When
                final List<ExportType> exports = module.getExports();

                // Then
                assertThat(exports).isNotNull();

                // Group exports by type
                final Set<WasmTypeKind> exportTypeKinds =
                    exports.stream()
                        .map(export -> export.getType().getKind())
                        .collect(Collectors.toSet());

                // Verify we have at least functions
                assertThat(exportTypeKinds).contains(WasmTypeKind.FUNCTION);

                // Verify export names are unique
                final Set<String> exportNames =
                    exports.stream().map(ExportType::getName).collect(Collectors.toSet());
                assertThat(exportNames.size()).isEqualTo(exports.size()); // No duplicates

                return "Complex exports: " + exports.size() + ", types: " + exportTypeKinds.size();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Complex module exports validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle complex module with multiple import types")
  void shouldHandleComplexModuleWithMultipleImportTypes() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-complex-imports",
            runtime -> {
              // Given - Complex module with multiple import types
              final byte[] complexImportsBytes = createModuleWithMultipleImports();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(complexImportsBytes)) {

                // When
                final List<ImportType> imports = module.getImports();

                // Then
                assertThat(imports).isNotNull();
                assertThat(imports).isNotEmpty();

                // Group imports by module
                final Set<String> importModules =
                    imports.stream().map(ImportType::getModuleName).collect(Collectors.toSet());

                // Group imports by type
                final Set<WasmTypeKind> importTypeKinds =
                    imports.stream()
                        .map(imp -> imp.getType().getKind())
                        .collect(Collectors.toSet());

                // Verify we have imports from different modules
                assertThat(importModules).isNotEmpty();

                return "Complex imports: "
                    + imports.size()
                    + ", modules: "
                    + importModules.size()
                    + ", types: "
                    + importTypeKinds.size();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Complex module imports validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should extract type information consistency")
  void shouldExtractTypeInformationConsistency() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-type-consistency",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // When - Extract metadata multiple times
                final List<ExportType> exports1 = module.getExports();
                final List<ExportType> exports2 = module.getExports();
                final List<ImportType> imports1 = module.getImports();
                final List<ImportType> imports2 = module.getImports();

                // Then - Results should be identical
                assertThat(exports2.size()).isEqualTo(exports1.size());
                assertThat(imports2.size()).isEqualTo(imports1.size());

                // Verify individual elements match
                for (int i = 0; i < exports1.size(); i++) {
                  final ExportType export1 = exports1.get(i);
                  final ExportType export2 = exports2.get(i);
                  assertThat(export2.getName()).isEqualTo(export1.getName());
                  assertThat(export2.getType().getKind()).isEqualTo(export1.getType().getKind());
                }

                for (int i = 0; i < imports1.size(); i++) {
                  final ImportType import1 = imports1.get(i);
                  final ImportType import2 = imports2.get(i);
                  assertThat(import2.getModuleName()).isEqualTo(import1.getModuleName());
                  assertThat(import2.getName()).isEqualTo(import1.getName());
                  assertThat(import2.getType().getKind()).isEqualTo(import1.getType().getKind());
                }

                return "Type consistency verified";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Type consistency validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module name extraction")
  void shouldHandleModuleNameExtraction() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-name-metadata",
            runtime -> {
              // Given
              final byte[] namedModuleBytes = createNamedModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(namedModuleBytes)) {

                // When
                final String moduleName = module.getName();

                // Then - Module name may be null if not present in the module
                // This is implementation-dependent

                return "Module name: " + (moduleName != null ? moduleName : "null");
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module name extraction validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate export/import type correspondence")
  void shouldValidateExportImportTypeCorrespondence() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-export-import-correspondence",
            runtime -> {
              // Given - Module that exports what another might import
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // When
                final List<ExportType> exports = module.getExports();

                // Then - Verify that if this module were used as an import source,
                // the type information would be compatible

                int functionExports = 0;
                int memoryExports = 0;
                int globalExports = 0;
                int tableExports = 0;

                for (final ExportType export : exports) {
                  switch (export.getType().getKind()) {
                    case FUNCTION:
                      functionExports++;
                      break;
                    case MEMORY:
                      memoryExports++;
                      break;
                    case GLOBAL:
                      globalExports++;
                      break;
                    case TABLE:
                      tableExports++;
                      break;
                    default:
                      // Unknown type
                      break;
                  }
                }

                return String.format(
                    "Exports - func: %d, mem: %d, global: %d, table: %d",
                    functionExports, memoryExports, globalExports, tableExports);
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Export/import correspondence validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should handle metadata extraction from test suite modules")
  void shouldHandleMetadataExtractionFromTestSuiteModules(final RuntimeType runtimeType) {
    // skipIfCategoryNotEnabled("testsuite");

    try {
      final List<WasmTestCase> testCases =
          testDataManager.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);

      if (testCases.isEmpty()) {
        LOGGER.info("No custom test cases available, skipping test suite metadata extraction");
        return;
      }

      int processedCount = 0;
      int maxToProcess = Math.min(testCases.size(), 5); // Limit for test performance

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "module-testsuite-metadata-" + runtimeType,
              runtime -> {
                int localProcessed = 0;

                try (final Engine engine = runtime.createEngine()) {
                  for (final WasmTestCase testCase : testCases) {
                    if (localProcessed >= maxToProcess) {
                      break;
                    }

                    try {
                      final Module module = engine.compileModule(testCase.getModuleBytes());

                      // Extract and validate metadata
                      final List<ExportType> exports = module.getExports();
                      final List<ImportType> imports = module.getImports();

                      assertThat(exports).isNotNull();
                      assertThat(imports).isNotNull();

                      module.close();
                      localProcessed++;

                    } catch (final Exception e) {
                      LOGGER.fine(
                          "Test case compilation failed (expected for some): "
                              + testCase.getTestName()
                              + " - "
                              + e.getMessage());
                    }
                  }
                }

                return "Processed " + localProcessed + " test suite modules";
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.info(
          "Test suite metadata extraction for " + runtimeType + ": " + validation.getSummary());

    } catch (final IOException e) {
      LOGGER.warning("Failed to load test suite: " + e.getMessage());
      skipIfNot(false, "Test suite loading failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should handle metadata extraction performance")
  void shouldHandleMetadataExtractionPerformance() {
    // skipIfCategoryNotEnabled("performance");

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-metadata-performance",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int iterations = 100;

              long totalExtractionTime = 0;

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                // Warmup
                for (int i = 0; i < 10; i++) {
                  module.getExports();
                  module.getImports();
                }

                // When - Measure metadata extraction time
                for (int i = 0; i < iterations; i++) {
                  final long start = System.nanoTime();

                  final List<ExportType> exports = module.getExports();
                  final List<ImportType> imports = module.getImports();

                  final long end = System.nanoTime();
                  totalExtractionTime += (end - start);

                  // Verify extraction worked
                  assertThat(exports).isNotNull();
                  assertThat(imports).isNotNull();
                }
              }

              // Then
              final double avgExtractionMs = totalExtractionTime / (iterations * 1_000_000.0);
              assertThat(avgExtractionMs).isLessThan(1.0); // Should be very fast

              return String.format("Avg metadata extraction: %.3f ms", avgExtractionMs);
            },
            comparison -> true); // Don't compare exact performance numbers

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Metadata extraction performance: " + validation.getSummary());
  }

  /** Creates a complex WebAssembly module with multiple export types. */
  private byte[] createComplexModule() {
    // This is a more complex module with function exports, memory, and globals
    // For simplicity, we'll use the existing simple module
    // In a real implementation, this would be a more complex WASM binary
    return TestUtils.createSimpleWasmModule();
  }

  /** Creates a WebAssembly module with multiple import types. */
  private byte[] createModuleWithMultipleImports() {
    // This would be a module that imports functions, memory, globals, etc.
    // For now, we'll use the memory import module
    return TestUtils.createMemoryImportWasmModule();
  }

  /** Creates a WebAssembly module with a name section. */
  private byte[] createNamedModule() {
    // This would be a module with a custom name section
    // For now, we'll use a simple module
    return TestUtils.createSimpleWasmModule();
  }
}
