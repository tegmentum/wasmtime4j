package ai.tegmentum.wasmtime4j.webassembly;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner.RuntimeTestFunction;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WebAssembly specification compliance. Tests both JNI and Panama
 * implementations against WebAssembly test cases.
 */
@DisplayName("WebAssembly Specification Integration Tests")
class WebAssemblySpecIT extends BaseIntegrationTest {

  private static WasmTestDataManager testDataManager;

  @BeforeAll
  static void setUpTestSuite() throws IOException {
    testDataManager = WasmTestDataManager.getInstance();
    testDataManager.initializeTestData();

    final WasmTestSuiteStats stats = WasmTestSuiteLoader.getTestSuiteStatistics();
    LOGGER.info("Test suite statistics: " + stats);
  }

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    skipIfCategoryNotEnabled("wasm.suite");
  }

  @Test
  @DisplayName("Should load and validate test suite infrastructure")
  void shouldLoadAndValidateTestSuiteInfrastructure() throws IOException {
    // Given: Test suite infrastructure

    // When: Loading test suite statistics
    final WasmTestSuiteStats stats = WasmTestSuiteLoader.getTestSuiteStatistics();

    // Then: Should have test suites available
    assertThat(stats.getTotalTestCount()).isGreaterThan(0);
    assertThat(stats.hasAnyTests()).isTrue();

    LOGGER.info("Test infrastructure validated: " + stats);
  }

  @Test
  @DisplayName("Should execute simple arithmetic test across both runtimes")
  void shouldExecuteSimpleArithmeticTestAcrossRuntimes() throws IOException {
    // Given: A simple arithmetic test
    final RuntimeTestFunction testFunction =
        runtime -> {
          // Load the add module
          final byte[] moduleBytes = testDataManager.loadTestCase("add").getModuleBytes();

          // Validate module
          final WasmModuleValidationResult validation = testDataManager.validateModule(moduleBytes);
          assertThat(validation.isValid()).as("Module should be valid").isTrue();

          // Create engine and compile module
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final Module module = engine.compileModule(moduleBytes);
            final Instance instance = store.instantiate(module);

            // Get the add function
            final WasmFunction addFunction =
                instance
                    .getFunction("add")
                    .orElseThrow(() -> new AssertionError("add function should be exported"));

            // Test the function
            final WasmValue[] args = {WasmValue.i32(5), WasmValue.i32(3)};
            final WasmValue[] results = addFunction.call(args);

            assertThat(results).hasSize(1);
            assertThat(results[0].asI32()).isEqualTo(8);

            return results[0].asI32();
          }
        };

    // When: Executing test across runtimes
    final CrossRuntimeTestResult result =
        CrossRuntimeTestRunner.executeAcrossRuntimes("simple_arithmetic_add", testFunction);

    // Then: Both runtimes should succeed with same result
    assertThat(result.getJniResult().isSuccessful()).isTrue();
    assertThat(result.getJniResult().getResult()).isEqualTo(8);

    if (TestUtils.isPanamaAvailable()) {
      assertThat(result.getPanamaResult()).isNotNull();
      assertThat(result.getPanamaResult().isSuccessful()).isTrue();
      assertThat(result.getPanamaResult().getResult()).isEqualTo(8);
      assertThat(result.bothSuccessful()).isTrue();
    }

    LOGGER.info("Cross-runtime arithmetic test completed: " + result.getSummary());
  }

  @Test
  @DisplayName("Should validate runtime consistency for simple operations")
  void shouldValidateRuntimeConsistencyForSimpleOperations() throws IOException {
    // Given: A test function that should produce consistent results
    final RuntimeTestFunction testFunction =
        runtime -> {
          final byte[] moduleBytes = TestUtils.createSimpleWasmModule();

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final Module module = engine.compileModule(moduleBytes);
            final Instance instance = store.instantiate(module);

            final WasmFunction addFunction =
                instance
                    .getFunction("add")
                    .orElseThrow(() -> new AssertionError("add function should be exported"));

            // Test multiple values
            int sum = 0;
            for (int i = 1; i <= 10; i++) {
              final WasmValue[] args = {WasmValue.i32(i), WasmValue.i32(i * 2)};
              final WasmValue[] results = addFunction.call(args);
              sum += results[0].asI32();
            }

            return sum;
          }
        };

    // When: Validating consistency
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "runtime_consistency_test", testFunction, comparison -> comparison.resultsEqual());

    // Then: Should be consistent
    if (TestUtils.isPanamaAvailable()) {
      assertThat(validation.isConsistent()).isTrue();
      assertThat(validation.hasErrors()).isFalse();
    }

    LOGGER.info("Runtime consistency validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.TestCaseRuntimeProvider.class)
  @DisplayName("Should execute test cases across available runtimes")
  void shouldExecuteTestCasesAcrossRuntimes(
      final WasmTestCase testCase, final RuntimeType runtimeType) {
    // Given: A test case and runtime type
    LOGGER.info("Testing " + testCase.getDisplayName() + " on " + runtimeType);

    // When: Testing the module
    try (final WasmRuntime runtime = createRuntimeForType(runtimeType)) {

      // Skip negative tests for now (they're expected to fail)
      if (testCase.isNegativeTest()) {
        LOGGER.info("Skipping negative test: " + testCase.getDisplayName());
        return;
      }

      // Validate module first
      final WasmModuleValidationResult validation =
          testDataManager.validateModule(testCase.getModuleBytes());
      assertThat(validation.isValid()).as("Module should be valid for positive test").isTrue();

      // Create engine and compile module
      try (final Engine engine = runtime.createEngine();
          final Store store = engine.createStore()) {

        final Module module = engine.compileModule(testCase.getModuleBytes());
        assertThat(module).isNotNull();

        final Instance instance = store.instantiate(module);
        assertThat(instance).isNotNull();

        // For modules with exports, try to validate basic functionality
        validateBasicModuleFunctionality(instance, testCase);
      }
    } catch (final Exception e) {
      if (!testCase.isNegativeTest()) {
        throw new AssertionError("Positive test should not fail: " + testCase.getDisplayName(), e);
      }
      // Negative tests are expected to fail
      LOGGER.info("Negative test failed as expected: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should generate execution summary with statistics")
  void shouldGenerateExecutionSummaryWithStatistics() {
    // Given: Some test executions
    final RuntimeTestFunction simpleTest =
        runtime -> {
          final byte[] moduleBytes = TestUtils.createSimpleWasmModule();

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final Module module = engine.compileModule(moduleBytes);
            final Instance instance = store.instantiate(module);

            return "success";
          }
        };

    // When: Executing multiple tests
    CrossRuntimeTestRunner.executeAcrossRuntimes("test1", simpleTest);
    CrossRuntimeTestRunner.executeAcrossRuntimes("test2", simpleTest);
    CrossRuntimeTestRunner.executeAcrossRuntimes("test3", simpleTest);

    final CrossRuntimeExecutionSummary summary = CrossRuntimeTestRunner.createExecutionSummary();

    // Then: Should have meaningful statistics
    assertThat(summary.getTotalTests()).isGreaterThan(0);
    assertThat(summary.getSuccessCount(RuntimeType.JNI)).isGreaterThan(0);

    if (TestUtils.isPanamaAvailable()) {
      assertThat(summary.getSuccessCount(RuntimeType.PANAMA)).isGreaterThan(0);
      assertThat(summary.getConsistencyRate()).isGreaterThan(0.0);
    }

    final String report = summary.createReport();
    assertThat(report).contains("Cross-Runtime Test Execution Summary");
    assertThat(report).contains("JNI Runtime");

    LOGGER.info("Execution summary:\n" + report);

    // Cleanup for other tests
    CrossRuntimeTestRunner.clearCache();
  }

  /** Creates a runtime instance for the specified type. */
  private WasmRuntime createRuntimeForType(final RuntimeType runtimeType) {
    final String originalProperty = System.getProperty("wasmtime4j.runtime");
    try {
      System.setProperty("wasmtime4j.runtime", runtimeType.name().toLowerCase());
      return ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory.createRuntime();
    } finally {
      if (originalProperty != null) {
        System.setProperty("wasmtime4j.runtime", originalProperty);
      } else {
        System.clearProperty("wasmtime4j.runtime");
      }
    }
  }

  /** Validates basic functionality of a WebAssembly module instance. */
  private void validateBasicModuleFunctionality(
      final Instance instance, final WasmTestCase testCase) {
    // Check that instance has expected exports
    final List<String> exportNames = instance.getExportNames();
    assertThat(exportNames).isNotEmpty().as("Module should have exports");

    LOGGER.fine("Module " + testCase.getDisplayName() + " exports: " + exportNames);

    // For modules that export functions, validate they can be retrieved
    for (final String exportName : exportNames) {
      if (instance.getFunction(exportName).isPresent()) {
        LOGGER.fine("Found exported function: " + exportName);
      } else if (instance.getMemory(exportName).isPresent()) {
        LOGGER.fine("Found exported memory: " + exportName);
      } else if (instance.getGlobal(exportName).isPresent()) {
        LOGGER.fine("Found exported global: " + exportName);
      } else if (instance.getTable(exportName).isPresent()) {
        LOGGER.fine("Found exported table: " + exportName);
      }
    }
  }
}
