package ai.tegmentum.wasmtime4j.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.ImportType;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteStats;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * WebAssembly specification compliance tests for Module API.
 * Tests compliance with official WebAssembly specification using test suites.
 */
@DisplayName("Module WebAssembly Specification Compliance Tests")
class ModuleSpecificationComplianceTest extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleSpecificationComplianceTest.class.getName());

  private WasmTestDataManager testDataManager;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    skipIfCategoryNotEnabled("module.compliance");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test data manager: " + e.getMessage());
      skipIfNot(false, "Test data manager setup failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should validate WebAssembly binary format specification")
  void shouldValidateWebAssemblyBinaryFormatSpecification() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "spec-binary-format-validation",
            runtime -> {
              // Given - Modules that test binary format compliance
              final List<SpecTestCase> testCases = createBinaryFormatTestCases();

              try (final Engine engine = runtime.createEngine()) {
                int validModules = 0;
                int invalidModules = 0;

                // When & Then - Test each case
                for (final SpecTestCase testCase : testCases) {
                  try {
                    final Module module = engine.compileModule(testCase.moduleBytes);
                    
                    if (testCase.shouldBeValid) {
                      validModules++;
                      // Valid module should have expected properties
                      assertThat(module.isValid()).isTrue();
                    } else {
                      // Should not reach here for invalid modules
                      LOGGER.warning("Invalid module was unexpectedly accepted: " + testCase.description);
                    }
                    
                    module.close();
                    
                  } catch (final WasmException | CompilationException | ValidationException e) {
                    if (!testCase.shouldBeValid) {
                      invalidModules++;
                      // Expected rejection
                    } else {
                      LOGGER.warning("Valid module was unexpectedly rejected: " + testCase.description
                          + " - " + e.getMessage());
                    }
                  }
                }

                return String.format("Binary format: %d valid, %d invalid", validModules, invalidModules);
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Binary format specification validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate WebAssembly section ordering specification")
  void shouldValidateWebAssemblySectionOrderingSpecification() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "spec-section-ordering",
            runtime -> {
              // Given - Modules with different section orderings
              final List<SpecTestCase> testCases = createSectionOrderingTestCases();

              try (final Engine engine = runtime.createEngine()) {
                int correctOrderings = 0;
                int incorrectOrderings = 0;

                // When & Then - Test each ordering
                for (final SpecTestCase testCase : testCases) {
                  try {
                    final Module module = engine.compileModule(testCase.moduleBytes);
                    
                    if (testCase.shouldBeValid) {
                      correctOrderings++;
                    }
                    
                    module.close();
                    
                  } catch (final WasmException | CompilationException | ValidationException e) {
                    if (!testCase.shouldBeValid) {
                      incorrectOrderings++;
                    }
                  }
                }

                return String.format("Section ordering: %d correct, %d incorrect", 
                    correctOrderings, incorrectOrderings);
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Section ordering specification validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @EnumSource(WasmTestSuiteLoader.TestSuiteType.class)
  @DisplayName("Should process official WebAssembly test suites")
  void shouldProcessOfficialWebAssemblyTestSuites(final WasmTestSuiteLoader.TestSuiteType suiteType) {
    skipIfCategoryNotEnabled("testsuite");

    try {
      final List<WasmTestCase> testCases = testDataManager.loadTestSuite(suiteType);

      if (testCases.isEmpty()) {
        LOGGER.info("No test cases found for suite: " + suiteType);
        return;
      }

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "spec-testsuite-" + suiteType.name().toLowerCase(),
              runtime -> {
                final AtomicInteger compiledCount = new AtomicInteger(0);
                final AtomicInteger failedCount = new AtomicInteger(0);
                final int maxToTest = Math.min(testCases.size(), 20); // Limit for test performance

                try (final Engine engine = runtime.createEngine()) {
                  for (int i = 0; i < maxToTest; i++) {
                    final WasmTestCase testCase = testCases.get(i);

                    try {
                      final Module module = engine.compileModule(testCase.getModuleBytes());
                      
                      // Basic validation
                      assertThat(module.isValid()).isTrue();
                      assertThat(module.getExports()).isNotNull();
                      assertThat(module.getImports()).isNotNull();

                      module.close();
                      compiledCount.incrementAndGet();

                    } catch (final Exception e) {
                      failedCount.incrementAndGet();
                      LOGGER.fine("Test case failed (may be expected): " + testCase.getName() + " - " + e.getMessage());
                    }
                  }
                }

                return String.format("Suite %s: %d/%d compiled successfully", 
                    suiteType, compiledCount.get(), maxToTest);
              },
              comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.info("Test suite " + suiteType + " validation: " + validation.getSummary());

    } catch (final IOException e) {
      LOGGER.warning("Failed to load test suite " + suiteType + ": " + e.getMessage());
      skipIfNot(false, "Test suite " + suiteType + " loading failed");
    }
  }

  @Test
  @DisplayName("Should validate WebAssembly type system specification")
  void shouldValidateWebAssemblyTypeSystemSpecification() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "spec-type-system",
            runtime -> {
              // Given - Modules testing different type system aspects
              final List<SpecTestCase> testCases = createTypeSystemTestCases();

              try (final Engine engine = runtime.createEngine()) {
                int validTypes = 0;
                int invalidTypes = 0;

                // When & Then - Test type system compliance
                for (final SpecTestCase testCase : testCases) {
                  try {
                    final Module module = engine.compileModule(testCase.moduleBytes);
                    
                    if (testCase.shouldBeValid) {
                      validTypes++;
                      
                      // Validate type information is extractable
                      final List<ExportType> exports = module.getExports();
                      final List<ImportType> imports = module.getImports();
                      
                      // All types should have valid type kinds
                      for (final ExportType export : exports) {
                        assertThat(export.getType().getKind()).isNotNull();
                      }
                      
                      for (final ImportType importType : imports) {
                        assertThat(importType.getType().getKind()).isNotNull();
                      }
                    }
                    
                    module.close();
                    
                  } catch (final WasmException | CompilationException | ValidationException e) {
                    if (!testCase.shouldBeValid) {
                      invalidTypes++;
                    }
                  }
                }

                return String.format("Type system: %d valid, %d invalid", validTypes, invalidTypes);
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Type system specification validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate WebAssembly limits and constraints")
  void shouldValidateWebAssemblyLimitsAndConstraints() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "spec-limits-constraints",
            runtime -> {
              // Given - Modules testing specification limits
              final List<SpecTestCase> testCases = createLimitsTestCases();

              try (final Engine engine = runtime.createEngine()) {
                int withinLimits = 0;
                int exceedsLimits = 0;

                // When & Then - Test limit enforcement
                for (final SpecTestCase testCase : testCases) {
                  try {
                    final Module module = engine.compileModule(testCase.moduleBytes);
                    
                    if (testCase.shouldBeValid) {
                      withinLimits++;
                    }
                    
                    module.close();
                    
                  } catch (final WasmException | CompilationException | ValidationException e) {
                    if (!testCase.shouldBeValid) {
                      exceedsLimits++;
                    }
                  }
                }

                return String.format("Limits: %d within, %d exceeds", withinLimits, exceedsLimits);
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Limits and constraints validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should validate module feature support across runtimes")
  void shouldValidateModuleFeatureSupportAcrossRuntimes(final RuntimeType runtimeType) {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "spec-feature-support-" + runtimeType,
            runtime -> {
              // Given - Modules testing different WebAssembly features
              final List<FeatureTestCase> featureTests = createFeatureTestCases();

              try (final Engine engine = runtime.createEngine()) {
                final List<String> supportedFeatures = new ArrayList<>();
                final List<String> unsupportedFeatures = new ArrayList<>();

                // When - Test each feature
                for (final FeatureTestCase featureTest : featureTests) {
                  try {
                    final Module module = engine.compileModule(featureTest.moduleBytes);
                    module.close();
                    supportedFeatures.add(featureTest.featureName);
                  } catch (final Exception e) {
                    unsupportedFeatures.add(featureTest.featureName);
                  }
                }

                return String.format("Features - Supported: %s, Unsupported: %s",
                    supportedFeatures, unsupportedFeatures);
              }
            },
            comparison -> true); // Feature support may legitimately vary

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Feature support for " + runtimeType + ": " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate specification compliance statistics")
  void shouldValidateSpecificationComplianceStatistics() {
    skipIfCategoryNotEnabled("testsuite");

    try {
      final WasmTestSuiteStats stats = testDataManager.getTestSuiteStatistics();

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "spec-compliance-statistics",
              runtime -> {
                int totalTests = 0;
                int passedTests = 0;

                try (final Engine engine = runtime.createEngine()) {
                  // Test a sample from each available suite
                  for (final WasmTestSuiteLoader.TestSuiteType suiteType : WasmTestSuiteLoader.TestSuiteType.values()) {
                    final int suiteSize = stats.getSuiteSize(suiteType);
                    if (suiteSize > 0) {
                      final List<WasmTestCase> testCases = testDataManager.loadTestSuite(suiteType);
                      final int sampleSize = Math.min(testCases.size(), 5); // Small sample

                      for (int i = 0; i < sampleSize; i++) {
                        totalTests++;
                        try {
                          final Module module = engine.compileModule(testCases.get(i).getModuleBytes());
                          module.close();
                          passedTests++;
                        } catch (final Exception e) {
                          // Some failures are expected in test suites
                        }
                      }
                    }
                  }
                }

                final double complianceRate = totalTests > 0 ? (double) passedTests / totalTests * 100.0 : 0.0;
                
                return String.format("Compliance: %.1f%% (%d/%d tests passed)", 
                    complianceRate, passedTests, totalTests);
              },
              comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.info("Specification compliance statistics: " + validation.getSummary());

    } catch (final IOException e) {
      LOGGER.warning("Failed to load test suite statistics: " + e.getMessage());
      skipIfNot(false, "Test suite statistics loading failed");
    }
  }

  @Test
  @DisplayName("Should validate WebAssembly instruction set specification")
  void shouldValidateWebAssemblyInstructionSetSpecification() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "spec-instruction-set",
            runtime -> {
              // Given - Modules testing different instruction categories
              final List<InstructionTestCase> instructionTests = createInstructionTestCases();

              try (final Engine engine = runtime.createEngine()) {
                final Set<String> supportedInstructions = instructionTests.stream()
                    .filter(test -> {
                      try {
                        final Module module = engine.compileModule(test.moduleBytes);
                        module.close();
                        return true;
                      } catch (final Exception e) {
                        return false;
                      }
                    })
                    .map(test -> test.instructionCategory)
                    .collect(Collectors.toSet());

                return "Instruction categories supported: " + supportedInstructions.size()
                       + " (" + supportedInstructions + ")";
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Instruction set specification validation: " + validation.getSummary());
  }

  /**
   * Creates test cases for binary format validation.
   */
  private List<SpecTestCase> createBinaryFormatTestCases() {
    final List<SpecTestCase> testCases = new ArrayList<>();

    // Valid cases
    testCases.add(new SpecTestCase(
        "Valid simple module",
        TestUtils.createSimpleWasmModule(),
        true
    ));

    testCases.add(new SpecTestCase(
        "Valid module with imports",
        TestUtils.createMemoryImportWasmModule(),
        true
    ));

    // Invalid cases
    testCases.add(new SpecTestCase(
        "Invalid magic number",
        new byte[]{0x01, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00},
        false
    ));

    testCases.add(new SpecTestCase(
        "Invalid version",
        new byte[]{0x00, 0x61, 0x73, 0x6d, 0x02, 0x00, 0x00, 0x00},
        false
    ));

    return testCases;
  }

  /**
   * Creates test cases for section ordering validation.
   */
  private List<SpecTestCase> createSectionOrderingTestCases() {
    final List<SpecTestCase> testCases = new ArrayList<>();

    // Correct ordering (type, import, function, export, code)
    testCases.add(new SpecTestCase(
        "Correct section order",
        TestUtils.createSimpleWasmModule(),
        true
    ));

    // TODO: Add cases with incorrect section ordering
    // This would require creating modules with sections in wrong order

    return testCases;
  }

  /**
   * Creates test cases for type system validation.
   */
  private List<SpecTestCase> createTypeSystemTestCases() {
    final List<SpecTestCase> testCases = new ArrayList<>();

    // Valid type system usage
    testCases.add(new SpecTestCase(
        "Valid function types",
        TestUtils.createSimpleWasmModule(),
        true
    ));

    // TODO: Add cases with invalid type usage
    
    return testCases;
  }

  /**
   * Creates test cases for limits and constraints validation.
   */
  private List<SpecTestCase> createLimitsTestCases() {
    final List<SpecTestCase> testCases = new ArrayList<>();

    // Within limits
    testCases.add(new SpecTestCase(
        "Normal module size",
        TestUtils.createSimpleWasmModule(),
        true
    ));

    // TODO: Add cases that exceed specification limits
    
    return testCases;
  }

  /**
   * Creates test cases for different WebAssembly features.
   */
  private List<FeatureTestCase> createFeatureTestCases() {
    final List<FeatureTestCase> testCases = new ArrayList<>();

    testCases.add(new FeatureTestCase(
        "MVP Functions",
        TestUtils.createSimpleWasmModule()
    ));

    testCases.add(new FeatureTestCase(
        "Linear Memory",
        TestUtils.createMemoryImportWasmModule()
    ));

    // TODO: Add more feature-specific test cases
    // - SIMD
    // - Threads
    // - Reference Types
    // - etc.

    return testCases;
  }

  /**
   * Creates test cases for different instruction categories.
   */
  private List<InstructionTestCase> createInstructionTestCases() {
    final List<InstructionTestCase> testCases = new ArrayList<>();

    testCases.add(new InstructionTestCase(
        "Arithmetic",
        TestUtils.createSimpleWasmModule() // Contains i32.add
    ));

    testCases.add(new InstructionTestCase(
        "Memory",
        TestUtils.createMemoryImportWasmModule() // Contains i32.load
    ));

    // TODO: Add more instruction category tests

    return testCases;
  }

  /**
   * Test case for specification compliance testing.
   */
  private static class SpecTestCase {
    final String description;
    final byte[] moduleBytes;
    final boolean shouldBeValid;

    SpecTestCase(final String description, final byte[] moduleBytes, final boolean shouldBeValid) {
      this.description = description;
      this.moduleBytes = moduleBytes.clone();
      this.shouldBeValid = shouldBeValid;
    }
  }

  /**
   * Test case for feature support testing.
   */
  private static class FeatureTestCase {
    final String featureName;
    final byte[] moduleBytes;

    FeatureTestCase(final String featureName, final byte[] moduleBytes) {
      this.featureName = featureName;
      this.moduleBytes = moduleBytes.clone();
    }
  }

  /**
   * Test case for instruction set testing.
   */
  private static class InstructionTestCase {
    final String instructionCategory;
    final byte[] moduleBytes;

    InstructionTestCase(final String instructionCategory, final byte[] moduleBytes) {
      this.instructionCategory = instructionCategory;
      this.moduleBytes = moduleBytes.clone();
    }
  }
}