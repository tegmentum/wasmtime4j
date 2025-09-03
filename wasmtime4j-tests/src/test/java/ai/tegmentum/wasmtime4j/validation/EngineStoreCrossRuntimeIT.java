package ai.tegmentum.wasmtime4j.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive cross-runtime validation tests for Engine and Store APIs. These tests ensure that
 * JNI and Panama implementations produce identical results for all Engine and Store operations.
 */
@DisplayName("Engine & Store Cross-Runtime Validation Tests")
final class EngineStoreCrossRuntimeIT extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled(TestCategories.CROSS_RUNTIME);
    skipIfPanamaNotAvailable();
  }

  @Nested
  @DisplayName("Engine Creation Cross-Runtime Validation")
  final class EngineCreationValidationTests {

    @Test
    @DisplayName("Should produce identical default engine creation results")
    void shouldProduceIdenticalDefaultEngineCreationResults() {
      LOGGER.info("Validating default engine creation across runtimes");

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final boolean isValid = engine.isValid();
                final EngineConfig config = engine.getConfig();
                engine.close();

                return EngineValidationResult.of(isValid, config);
              });

      assertThat(result.isValid()).as("Cross-runtime engine creation validation failed").isTrue();
      assertThat(result.areResultsIdentical())
          .as("Engine creation results differ between runtimes")
          .isTrue();
      LOGGER.info("Engine creation validation: " + result.getDifferenceDescription());
      // addTestMetric("Validated default engine creation cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical custom config engine creation results")
    void shouldProduceIdenticalCustomConfigEngineCreationResults() {
      LOGGER.info("Validating custom config engine creation across runtimes");

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final EngineConfig config =
                    new EngineConfig()
                        .debugInfo(true)
                        .consumeFuel(true)
                        .optimizationLevel(OptimizationLevel.SIZE)
                        .parallelCompilation(false);

                final Engine engine = runtime.createEngine(config);
                final boolean isValid = engine.isValid();
                final EngineConfig retrievedConfig = engine.getConfig();
                engine.close();

                return EngineValidationResult.of(isValid, retrievedConfig);
              });

      assertThat(result.isValid())
          .as("Cross-runtime custom engine creation validation failed")
          .isTrue();
      assertThat(result.areResultsIdentical())
          .as("Custom engine creation results differ between runtimes")
          .isTrue();
      LOGGER.info("Custom config engine creation validation: " + result.getDifferenceDescription());
      // addTestMetric("Validated custom config engine creation cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical optimization level handling")
    void shouldProduceIdenticalOptimizationLevelHandling() {
      LOGGER.info("Validating optimization level handling across runtimes");

      final OptimizationLevel[] levels = OptimizationLevel.values();

      for (final OptimizationLevel level : levels) {
        final CrossRuntimeValidator.ComparisonResult result =
            CrossRuntimeValidator.validateCrossRuntime(
                runtime -> {
                  final EngineConfig config = new EngineConfig().optimizationLevel(level);
                  final Engine engine = runtime.createEngine(config);
                  final OptimizationLevel retrievedLevel =
                      engine.getConfig().getOptimizationLevel();
                  engine.close();

                  return retrievedLevel;
                });

        assertThat(result.isValid())
            .as("Cross-runtime optimization level validation failed for " + level)
            .isTrue();
        assertThat(result.areResultsIdentical())
            .as("Optimization level results differ between runtimes for " + level)
            .isTrue();
        LOGGER.fine(
            "Optimization level " + level + " validation: " + result.getDifferenceDescription());
      }

      // addTestMetric("Validated " + levels.length + " optimization levels cross-runtime");
    }
  }

  @Nested
  @DisplayName("Engine Module Compilation Cross-Runtime Validation")
  final class EngineModuleCompilationValidationTests {

    @Test
    @DisplayName("Should produce identical module compilation results")
    void shouldProduceIdenticalModuleCompilationResults() {
      LOGGER.info("Validating module compilation across runtimes");

      final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Module module = engine.compileModule(wasmBytes);
                final boolean isValid = module.isValid();
                module.close();
                engine.close();

                return isValid;
              });

      assertThat(result.isValid())
          .as("Cross-runtime module compilation validation failed")
          .isTrue();
      assertThat(result.areResultsIdentical())
          .as("Module compilation results differ between runtimes")
          .isTrue();
      LOGGER.info("Module compilation validation: " + result.getDifferenceDescription());
      // addTestMetric("Validated module compilation cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical compilation error handling")
    void shouldProduceIdenticalCompilationErrorHandling() {
      LOGGER.info("Validating compilation error handling across runtimes");

      final byte[] invalidWasm = "invalid wasm".getBytes();
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                try {
                  engine.compileModule(invalidWasm);
                  engine.close();
                  return "SUCCESS"; // Should not reach here
                } catch (final Exception e) {
                  engine.close();
                  return e.getClass().getSimpleName(); // Return exception type for comparison
                }
              });

      // Both runtimes should throw exceptions of the same type
      assertThat(result.areExceptionsIdentical() || result.areResultsIdentical())
          .as("Error handling differs between runtimes")
          .isTrue();
      LOGGER.info("Compilation error handling validation: " + result.getDifferenceDescription());
      // addTestMetric("Validated compilation error handling cross-runtime");
    }
  }

  @Nested
  @DisplayName("Store Creation Cross-Runtime Validation")
  final class StoreCreationValidationTests {

    @Test
    @DisplayName("Should produce identical store creation results")
    void shouldProduceIdenticalStoreCreationResults() {
      LOGGER.info("Validating store creation across runtimes");

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final boolean isValid = store.isValid();
                final Object data = store.getData(); // Should be null
                store.close();
                engine.close();

                return StoreValidationResult.of(isValid, data);
              });

      assertThat(result.isValid()).as("Cross-runtime store creation validation failed").isTrue();
      assertThat(result.areResultsIdentical())
          .as("Store creation results differ between runtimes")
          .isTrue();
      LOGGER.info("Store creation validation: " + result.getDifferenceDescription());
      // addTestMetric("Validated store creation cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical store creation with custom data")
    void shouldProduceIdenticalStoreCreationWithCustomData() {
      LOGGER.info("Validating store creation with custom data across runtimes");

      final String testData = "cross-runtime-test-data";
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Store store = engine.createStore(testData);
                final boolean isValid = store.isValid();
                final Object data = store.getData();
                store.close();
                engine.close();

                return StoreValidationResult.of(isValid, data);
              });

      assertThat(result.isValid())
          .as("Cross-runtime store creation with data validation failed")
          .isTrue();
      assertThat(result.areResultsIdentical())
          .as("Store creation with data results differ between runtimes")
          .isTrue();
      LOGGER.info("Store creation with data validation: " + result.getDifferenceDescription());
      // addTestMetric("Validated store creation with data cross-runtime");
    }
  }

  @Nested
  @DisplayName("Store Data Management Cross-Runtime Validation")
  final class StoreDataManagementValidationTests {

    @Test
    @DisplayName("Should produce identical data set/get operations")
    void shouldProduceIdenticalDataSetGetOperations() {
      LOGGER.info("Validating store data operations across runtimes");

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();

                // Test various data types
                store.setData("string-data");
                final Object data1 = store.getData();

                store.setData(42);
                final Object data2 = store.getData();

                store.setData(List.of("item1", "item2"));
                final Object data3 = store.getData();

                store.setData(null);
                final Object data4 = store.getData();

                store.close();
                engine.close();

                return List.of(data1, data2, data3, data4);
              });

      assertThat(result.isValid()).as("Cross-runtime data operations validation failed").isTrue();
      assertThat(result.areResultsIdentical())
          .as("Data operations results differ between runtimes")
          .isTrue();
      LOGGER.info("Data operations validation: " + result.getDifferenceDescription());
      // addTestMetric("Validated store data operations cross-runtime");
    }
  }

  @Nested
  @DisplayName("Store Fuel Management Cross-Runtime Validation")
  final class StoreFuelManagementValidationTests {

    @Test
    @DisplayName("Should produce identical fuel operations")
    void shouldProduceIdenticalFuelOperations() {
      LOGGER.info("Validating store fuel operations across runtimes");

      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final EngineConfig config = new EngineConfig().consumeFuel(true);
                final Engine engine = runtime.createEngine(config);
                final Store store = engine.createStore();

                // Test fuel operations
                store.setFuel(1000L);
                final long fuel1 = store.getFuel();

                store.addFuel(500L);
                final long fuel2 = store.getFuel();

                store.setFuel(0L);
                final long fuel3 = store.getFuel();

                store.close();
                engine.close();

                return List.of(fuel1, fuel2, fuel3);
              });

      assertThat(result.isValid()).as("Cross-runtime fuel operations validation failed").isTrue();
      assertThat(result.areResultsIdentical())
          .as("Fuel operations results differ between runtimes")
          .isTrue();
      LOGGER.info("Fuel operations validation: " + result.getDifferenceDescription());
      // addTestMetric("Validated store fuel operations cross-runtime");
      //     }
      //
      //     @Test
      //     @DisplayName("Should produce identical fuel operations when consumption disabled")
      //     void shouldProduceIdenticalFuelOperationsWhenConsumptionDisabled() {
      //     LOGGER.info("Validating store fuel operations with consumption disabled across
      // runtimes");

      final CrossRuntimeValidator.ComparisonResult disabledResult =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final EngineConfig config = new EngineConfig().consumeFuel(false);
                final Engine engine = runtime.createEngine(config);
                final Store store = engine.createStore();

                try {
                  store.setFuel(1000L);
                  final long fuel = store.getFuel();
                  store.close();
                  engine.close();
                  return "SUCCESS:" + fuel;
                } catch (final Exception e) {
                  store.close();
                  engine.close();
                  return "ERROR:" + e.getClass().getSimpleName();
                }
              });

      // Both should either succeed or fail with same exception type
      assertThat(disabledResult.areExceptionsIdentical() || disabledResult.areResultsIdentical())
          .as("Fuel operations with consumption disabled differ between runtimes")
          .isTrue();
      LOGGER.info(
          "Fuel operations (consumption disabled) validation: "
              + disabledResult.getDifferenceDescription());
      // addTestMetric("Validated fuel operations with consumption disabled cross-runtime");
    }
  }

  /*
   * Additional test classes are commented out due to incomplete implementation.
   * These would include:
   * - Store Epoch Management Cross-Runtime Validation Tests
   * - Lifecycle Cross-Runtime Validation Tests
   * - Complex Workflow Cross-Runtime Validation Tests
   * - Additional validation result helper classes
   */
}
