package ai.tegmentum.wasmtime4j.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
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
    skipIfCategoryNotEnabled(TestCategories.CROSS_RUNTIME);
    skipIfPanamaNotAvailable();
  }

  @Nested
  @DisplayName("Engine Creation Cross-Runtime Validation")
  final class EngineCreationValidationTests {

    @Test
    @DisplayName("Should produce identical default engine creation results")
    void shouldProduceIdenticalDefaultEngineCreationResults() {
      LOGGER.info("Validating default engine creation across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            final Engine engine = runtime.createEngine();
            final boolean isValid = engine.isValid();
            final EngineConfig config = engine.getConfig();
            engine.close();
            
            return EngineValidationResult.of(isValid, config);
          });

      assertThat(result.isValid()).as("Cross-runtime engine creation validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Engine creation results differ between runtimes").isTrue();
      LOGGER.info("Engine creation validation: " + result.getDifferenceDescription());
      addTestMetric("Validated default engine creation cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical custom config engine creation results")
    void shouldProduceIdenticalCustomConfigEngineCreationResults() {
      LOGGER.info("Validating custom config engine creation across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            final EngineConfig config = new EngineConfig()
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

      assertThat(result.isValid()).as("Cross-runtime custom engine creation validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Custom engine creation results differ between runtimes").isTrue();
      LOGGER.info("Custom config engine creation validation: " + result.getDifferenceDescription());
      addTestMetric("Validated custom config engine creation cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical optimization level handling")
    void shouldProduceIdenticalOptimizationLevelHandling() {
      LOGGER.info("Validating optimization level handling across runtimes");
      
      final OptimizationLevel[] levels = OptimizationLevel.values();
      
      for (final OptimizationLevel level : levels) {
        final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
            runtime -> {
              final EngineConfig config = new EngineConfig().optimizationLevel(level);
              final Engine engine = runtime.createEngine(config);
              final OptimizationLevel retrievedLevel = engine.getConfig().getOptimizationLevel();
              engine.close();
              
              return retrievedLevel;
            });

        assertThat(result.isValid()).as("Cross-runtime optimization level validation failed for " + level).isTrue();
        assertThat(result.areResultsIdentical()).as("Optimization level results differ between runtimes for " + level).isTrue();
        LOGGER.fine("Optimization level " + level + " validation: " + result.getDifferenceDescription());
      }
      
      addTestMetric("Validated " + levels.length + " optimization levels cross-runtime");
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
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            final Engine engine = runtime.createEngine();
            final Module module = engine.compileModule(wasmBytes);
            final boolean isValid = module.isValid();
            module.close();
            engine.close();
            
            return isValid;
          });

      assertThat(result.isValid()).as("Cross-runtime module compilation validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Module compilation results differ between runtimes").isTrue();
      LOGGER.info("Module compilation validation: " + result.getDifferenceDescription());
      addTestMetric("Validated module compilation cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical compilation error handling")
    void shouldProduceIdenticalCompilationErrorHandling() {
      LOGGER.info("Validating compilation error handling across runtimes");
      
      final byte[] invalidWasm = "invalid wasm".getBytes();
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
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
          .as("Error handling differs between runtimes").isTrue();
      LOGGER.info("Compilation error handling validation: " + result.getDifferenceDescription());
      addTestMetric("Validated compilation error handling cross-runtime");
    }
  }

  @Nested
  @DisplayName("Store Creation Cross-Runtime Validation")
  final class StoreCreationValidationTests {

    @Test
    @DisplayName("Should produce identical store creation results")
    void shouldProduceIdenticalStoreCreationResults() {
      LOGGER.info("Validating store creation across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
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
      assertThat(result.areResultsIdentical()).as("Store creation results differ between runtimes").isTrue();
      LOGGER.info("Store creation validation: " + result.getDifferenceDescription());
      addTestMetric("Validated store creation cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical store creation with custom data")
    void shouldProduceIdenticalStoreCreationWithCustomData() {
      LOGGER.info("Validating store creation with custom data across runtimes");
      
      final String testData = "cross-runtime-test-data";
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            final Engine engine = runtime.createEngine();
            final Store store = engine.createStore(testData);
            final boolean isValid = store.isValid();
            final Object data = store.getData();
            store.close();
            engine.close();
            
            return StoreValidationResult.of(isValid, data);
          });

      assertThat(result.isValid()).as("Cross-runtime store creation with data validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Store creation with data results differ between runtimes").isTrue();
      LOGGER.info("Store creation with data validation: " + result.getDifferenceDescription());
      addTestMetric("Validated store creation with data cross-runtime");
    }
  }

  @Nested
  @DisplayName("Store Data Management Cross-Runtime Validation")
  final class StoreDataManagementValidationTests {

    @Test
    @DisplayName("Should produce identical data set/get operations")
    void shouldProduceIdenticalDataSetGetOperations() {
      LOGGER.info("Validating store data operations across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
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
      assertThat(result.areResultsIdentical()).as("Data operations results differ between runtimes").isTrue();
      LOGGER.info("Data operations validation: " + result.getDifferenceDescription());
      addTestMetric("Validated store data operations cross-runtime");
    }
  }

  @Nested
  @DisplayName("Store Fuel Management Cross-Runtime Validation")
  final class StoreFuelManagementValidationTests {

    @Test
    @DisplayName("Should produce identical fuel operations")
    void shouldProduceIdenticalFuelOperations() {
      LOGGER.info("Validating store fuel operations across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
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
      assertThat(result.areResultsIdentical()).as("Fuel operations results differ between runtimes").isTrue();
      LOGGER.info("Fuel operations validation: " + result.getDifferenceDescription());
      addTestMetric("Validated store fuel operations cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical fuel operations when consumption disabled")
    void shouldProduceIdenticalFuelOperationsWhenConsumptionDisabled() {
      LOGGER.info("Validating store fuel operations with consumption disabled across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
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
      assertThat(result.areExceptionsIdentical() || result.areResultsIdentical())
          .as("Fuel operations with consumption disabled differ between runtimes").isTrue();
      LOGGER.info("Fuel operations (consumption disabled) validation: " + result.getDifferenceDescription());
      addTestMetric("Validated fuel operations with consumption disabled cross-runtime");
    }
  }

  @Nested
  @DisplayName("Store Epoch Management Cross-Runtime Validation")
  final class StoreEpochManagementValidationTests {

    @Test
    @DisplayName("Should produce identical epoch deadline operations")
    void shouldProduceIdenticalEpochDeadlineOperations() {
      LOGGER.info("Validating store epoch deadline operations across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            final Engine engine = runtime.createEngine();
            final Store store = engine.createStore();
            
            // Test various epoch deadlines
            final long[] deadlines = {0L, 1L, 100L, 1000L, Long.MAX_VALUE};
            for (final long deadline : deadlines) {
              store.setEpochDeadline(deadline);
            }
            
            store.close();
            engine.close();
            
            return deadlines.length; // Return count of successful operations
          });

      assertThat(result.isValid()).as("Cross-runtime epoch operations validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Epoch operations results differ between runtimes").isTrue();
      LOGGER.info("Epoch operations validation: " + result.getDifferenceDescription());
      addTestMetric("Validated store epoch operations cross-runtime");
    }
  }

  @Nested
  @DisplayName("Lifecycle Cross-Runtime Validation")
  final class LifecycleValidationTests {

    @Test
    @DisplayName("Should produce identical engine closure behavior")
    void shouldProduceIdenticalEngineClosureBehavior() {
      LOGGER.info("Validating engine closure behavior across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            final Engine engine = runtime.createEngine();
            final boolean validBefore = engine.isValid();
            
            engine.close();
            final boolean validAfter = engine.isValid();
            
            // Multiple closes should be safe
            engine.close();
            final boolean validAfterSecondClose = engine.isValid();
            
            return List.of(validBefore, validAfter, validAfterSecondClose);
          });

      assertThat(result.isValid()).as("Cross-runtime engine closure validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Engine closure behavior differs between runtimes").isTrue();
      LOGGER.info("Engine closure validation: " + result.getDifferenceDescription());
      addTestMetric("Validated engine closure cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical store closure behavior")
    void shouldProduceIdenticalStoreClosure Behavior() {
      LOGGER.info("Validating store closure behavior across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            final Engine engine = runtime.createEngine();
            final Store store = engine.createStore();
            final boolean validBefore = store.isValid();
            
            store.close();
            final boolean validAfter = store.isValid();
            
            // Multiple closes should be safe
            store.close();
            final boolean validAfterSecondClose = store.isValid();
            
            engine.close();
            
            return List.of(validBefore, validAfter, validAfterSecondClose);
          });

      assertThat(result.isValid()).as("Cross-runtime store closure validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Store closure behavior differs between runtimes").isTrue();
      LOGGER.info("Store closure validation: " + result.getDifferenceDescription());
      addTestMetric("Validated store closure cross-runtime");
    }
  }

  @Nested
  @DisplayName("Complex Workflow Cross-Runtime Validation")
  final class ComplexWorkflowValidationTests {

    @Test
    @DisplayName("Should produce identical results for complete engine-store-module workflow")
    void shouldProduceIdenticalResultsForCompleteWorkflow() {
      LOGGER.info("Validating complete engine-store-module workflow across runtimes");
      
      final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            // Create engine with custom config
            final EngineConfig config = new EngineConfig()
                .debugInfo(true)
                .consumeFuel(true)
                .optimizationLevel(OptimizationLevel.SPEED);
                
            final Engine engine = runtime.createEngine(config);
            
            // Compile module
            final Module module = engine.compileModule(wasmBytes);
            final boolean moduleValid = module.isValid();
            
            // Create stores
            final Store store1 = engine.createStore();
            final Store store2 = engine.createStore("test-data");
            
            // Configure stores
            store1.setFuel(1000L);
            store1.setData("store1-data");
            store1.setEpochDeadline(100L);
            
            store2.setFuel(2000L);
            store2.setEpochDeadline(200L);
            
            // Get final states
            final long fuel1 = store1.getFuel();
            final long fuel2 = store2.getFuel();
            final Object data1 = store1.getData();
            final Object data2 = store2.getData();
            
            // Cleanup
            module.close();
            store1.close();
            store2.close();
            engine.close();
            
            return WorkflowValidationResult.of(moduleValid, fuel1, fuel2, data1, data2);
          }, Duration.ofMinutes(2));

      assertThat(result.isValid()).as("Cross-runtime complete workflow validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Complete workflow results differ between runtimes").isTrue();
      LOGGER.info("Complete workflow validation: " + result.getDifferenceDescription());
      addTestMetric("Validated complete engine-store-module workflow cross-runtime");
    }

    @Test
    @DisplayName("Should produce identical results for concurrent operations workflow")
    void shouldProduceIdenticalResultsForConcurrentOperationsWorkflow() throws InterruptedException, ExecutionException {
      skipIfCategoryNotEnabled(TestCategories.CONCURRENCY);
      
      LOGGER.info("Validating concurrent operations workflow across runtimes");
      
      final CrossRuntimeValidator.ComparisonResult result = CrossRuntimeValidator.validateCrossRuntime(
          runtime -> {
            final Engine engine = runtime.createEngine();
            final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
            
            // Perform concurrent operations
            final List<CompletableFuture<Boolean>> futures = List.of(
                CompletableFuture.supplyAsync(() -> {
                  try {
                    final Module module = engine.compileModule(wasmBytes);
                    final boolean valid = module.isValid();
                    module.close();
                    return valid;
                  } catch (final Exception e) {
                    return false;
                  }
                }),
                CompletableFuture.supplyAsync(() -> {
                  try {
                    final Store store = engine.createStore("concurrent-data");
                    final boolean valid = store.isValid();
                    store.close();
                    return valid;
                  } catch (final Exception e) {
                    return false;
                  }
                }),
                CompletableFuture.supplyAsync(() -> {
                  try {
                    final Store store = engine.createStore();
                    store.setData("concurrent-test");
                    final Object data = store.getData();
                    store.close();
                    return "concurrent-test".equals(data);
                  } catch (final Exception e) {
                    return false;
                  }
                })
            );
            
            // Wait for all operations and collect results
            final List<Boolean> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            engine.close();
            return results;
          }, Duration.ofMinutes(2));

      assertThat(result.isValid()).as("Cross-runtime concurrent workflow validation failed").isTrue();
      assertThat(result.areResultsIdentical()).as("Concurrent workflow results differ between runtimes").isTrue();
      LOGGER.info("Concurrent workflow validation: " + result.getDifferenceDescription());
      addTestMetric("Validated concurrent operations workflow cross-runtime");
    }
  }

  // Helper classes for structured validation results
  private static final class EngineValidationResult {
    private final boolean valid;
    private final boolean debugInfo;
    private final boolean consumeFuel;
    private final OptimizationLevel optimizationLevel;
    private final boolean parallelCompilation;

    private EngineValidationResult(final boolean valid, final EngineConfig config) {
      this.valid = valid;
      this.debugInfo = config.isDebugInfo();
      this.consumeFuel = config.isConsumeFuel();
      this.optimizationLevel = config.getOptimizationLevel();
      this.parallelCompilation = config.isParallelCompilation();
    }

    public static EngineValidationResult of(final boolean valid, final EngineConfig config) {
      return new EngineValidationResult(valid, config);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final EngineValidationResult other = (EngineValidationResult) obj;
      return valid == other.valid
          && debugInfo == other.debugInfo
          && consumeFuel == other.consumeFuel
          && optimizationLevel == other.optimizationLevel
          && parallelCompilation == other.parallelCompilation;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(valid, debugInfo, consumeFuel, optimizationLevel, parallelCompilation);
    }

    @Override
    public String toString() {
      return String.format("EngineValidationResult[valid=%s, debugInfo=%s, consumeFuel=%s, " +
                          "optimizationLevel=%s, parallelCompilation=%s]",
                          valid, debugInfo, consumeFuel, optimizationLevel, parallelCompilation);
    }
  }

  private static final class StoreValidationResult {
    private final boolean valid;
    private final Object data;

    private StoreValidationResult(final boolean valid, final Object data) {
      this.valid = valid;
      this.data = data;
    }

    public static StoreValidationResult of(final boolean valid, final Object data) {
      return new StoreValidationResult(valid, data);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final StoreValidationResult other = (StoreValidationResult) obj;
      return valid == other.valid && java.util.Objects.equals(data, other.data);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(valid, data);
    }

    @Override
    public String toString() {
      return String.format("StoreValidationResult[valid=%s, data=%s]", valid, data);
    }
  }

  private static final class WorkflowValidationResult {
    private final boolean moduleValid;
    private final long fuel1;
    private final long fuel2;
    private final Object data1;
    private final Object data2;

    private WorkflowValidationResult(final boolean moduleValid, final long fuel1, final long fuel2,
                                    final Object data1, final Object data2) {
      this.moduleValid = moduleValid;
      this.fuel1 = fuel1;
      this.fuel2 = fuel2;
      this.data1 = data1;
      this.data2 = data2;
    }

    public static WorkflowValidationResult of(final boolean moduleValid, final long fuel1, final long fuel2,
                                             final Object data1, final Object data2) {
      return new WorkflowValidationResult(moduleValid, fuel1, fuel2, data1, data2);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final WorkflowValidationResult other = (WorkflowValidationResult) obj;
      return moduleValid == other.moduleValid
          && fuel1 == other.fuel1
          && fuel2 == other.fuel2
          && java.util.Objects.equals(data1, other.data1)
          && java.util.Objects.equals(data2, other.data2);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(moduleValid, fuel1, fuel2, data1, data2);
    }

    @Override
    public String toString() {
      return String.format("WorkflowValidationResult[moduleValid=%s, fuel1=%d, fuel2=%d, data1=%s, data2=%s]",
                          moduleValid, fuel1, fuel2, data1, data2);
    }
  }
}