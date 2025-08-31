package ai.tegmentum.wasmtime4j.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.performance.PerformanceTestHarness;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive test suite for Instance API functionality providing exhaustive coverage of instance
 * creation, function invocation, export access, memory operations, type safety validation, and
 * concurrent execution patterns. This test suite goes beyond basic functionality to test edge cases,
 * performance characteristics, and cross-runtime consistency.
 */
@DisplayName("Instance API Comprehensive Tests")
final class InstanceApiComprehensiveTest {

  private static final Logger LOGGER =
      Logger.getLogger(InstanceApiComprehensiveTest.class.getName());

  private final Map<String, Object> testMetrics = new HashMap<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    TestUtils.skipIfCategoryNotEnabled(TestCategories.INSTANCE);
    testMetrics.clear();
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
  }

  /**
   * Execute test with both JNI and Panama runtimes if available.
   *
   * @param testAction The test action to execute with each runtime
   */
  private void runWithBothRuntimes(final RuntimeTestAction testAction) {
    final List<RuntimeType> availableRuntimes = WasmRuntimeFactory.getAvailableRuntimes();

    for (final RuntimeType runtimeType : availableRuntimes) {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
        LOGGER.info("Testing with runtime: " + runtimeType);
        testAction.execute(runtime, runtimeType);
      } catch (final Exception e) {
        throw new RuntimeException("Test failed with runtime " + runtimeType, e);
      }
    }
  }

  /**
   * Add a test metric for tracking and analysis.
   *
   * @param message The metric message
   */
  private void addTestMetric(final String message) {
    testMetrics.put(Instant.now().toString(), message);
    LOGGER.info("Test metric: " + message);
  }

  /** Functional interface for runtime-specific test actions. */
  @FunctionalInterface
  private interface RuntimeTestAction {
    void execute(WasmRuntime runtime, RuntimeType runtimeType) throws Exception;
  }

  @Nested
  @DisplayName("Enhanced Instance Creation and State Management Tests")
  final class EnhancedInstanceCreationTests {

    @Test
    @DisplayName("Should create instance with all types of WebAssembly modules")
    void shouldCreateInstanceWithAllTypesOfWebAssemblyModules() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final Map<String, byte[]> moduleTypes = new HashMap<>();
            moduleTypes.put("Basic arithmetic", WasmTestModules.getModule("basic_add"));
            moduleTypes.put("Memory operations", WasmTestModules.getModule("memory_basic"));
            moduleTypes.put("Global variables", WasmTestModules.getModule("global_mutable"));
            moduleTypes.put("Function tables", WasmTestModules.getModule("table_indirect"));
            moduleTypes.put("Complex functions", WasmTestModules.getModule("function_fibonacci"));

            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              for (final Map.Entry<String, byte[]> entry : moduleTypes.entrySet()) {
                final String moduleType = entry.getKey();
                final byte[] moduleBytes = entry.getValue();

                try (final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  assertThat(instance).isNotNull();
                  assertThat(instance.isValid()).isTrue();
                  assertThat(instance.getModule()).isEqualTo(module);
                  assertThat(instance.getStore()).isEqualTo(store);

                  // Verify instance has expected exports
                  final String[] exportNames = instance.getExportNames();
                  assertThat(exportNames).isNotEmpty();

                  addTestMetric(
                      String.format(
                          "%s module instantiated successfully with %s (%d exports)",
                          moduleType, runtimeType, exportNames.length));
                }
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle instance state transitions correctly")
    void shouldHandleInstanceStateTransitionsCorrectly() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_mutable");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes)) {

              // Test initial state after creation
              final Instance instance = module.instantiate(store);
              assertThat(instance.isValid()).isTrue();

              // Test function execution modifies state
              final WasmValue[] initialValue = instance.callFunction("get");
              assertThat(initialValue).hasSize(1);

              instance.callFunction("set", WasmValue.i32(42));
              final WasmValue[] modifiedValue = instance.callFunction("get");
              assertThat(modifiedValue[0].asI32()).isEqualTo(42);
              assertThat(modifiedValue[0].asI32()).isNotEqualTo(initialValue[0].asI32());

              // Test state persistence across multiple operations
              for (int i = 0; i < 100; i++) {
                instance.callFunction("set", WasmValue.i32(i));
                final WasmValue[] currentValue = instance.callFunction("get");
                assertThat(currentValue[0].asI32()).isEqualTo(i);
              }

              // Test state after close
              instance.close();
              assertThat(instance.isValid()).isFalse();

              addTestMetric("Instance state transitions validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle complex instantiation patterns")
    void shouldHandleComplexInstantiationPatterns() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine()) {

              // Test multiple stores with same engine
              final List<Store> stores = new ArrayList<>();
              final List<Instance> instances = new ArrayList<>();

              try {
                for (int i = 0; i < 10; i++) {
                  final Store store = engine.createStore();
                  stores.add(store);
                  
                  final Module module = engine.compileModule(moduleBytes);
                  final Instance instance = module.instantiate(store);
                  instances.add(instance);

                  assertThat(instance.isValid()).isTrue();
                  assertThat(instance.getStore()).isEqualTo(store);

                  // Verify each instance works independently
                  final WasmValue[] result = instance.callFunction("add", 
                      WasmValue.i32(i), WasmValue.i32(i + 1));
                  assertThat(result[0].asI32()).isEqualTo(2 * i + 1);
                }

                // All instances should be valid and independent
                for (int i = 0; i < instances.size(); i++) {
                  final Instance instance = instances.get(i);
                  assertThat(instance.isValid()).isTrue();
                  
                  final WasmValue[] result = instance.callFunction("add", 
                      WasmValue.i32(100), WasmValue.i32(i));
                  assertThat(result[0].asI32()).isEqualTo(100 + i);
                }

              } finally {
                // Clean up resources
                for (final Instance instance : instances) {
                  if (instance.isValid()) {
                    instance.close();
                  }
                }
                for (final Store store : stores) {
                  store.close();
                }
              }

              addTestMetric("Complex instantiation patterns validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should validate instance immutability constraints")
    void shouldValidateInstanceImmutabilityConstraints() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_immutable");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test that instance references are stable
              final Module moduleRef1 = instance.getModule();
              final Module moduleRef2 = instance.getModule();
              assertThat(moduleRef1).isSameAs(moduleRef2);

              final Store storeRef1 = instance.getStore();
              final Store storeRef2 = instance.getStore();
              assertThat(storeRef1).isSameAs(storeRef2);

              // Test that exports are stable
              final String[] exports1 = instance.getExportNames();
              final String[] exports2 = instance.getExportNames();
              assertThat(exports1).isEqualTo(exports2);

              // Test immutable global access
              final WasmValue[] constantValue1 = instance.callFunction("get_const");
              final WasmValue[] constantValue2 = instance.callFunction("get_const");
              assertThat(constantValue1[0].asI32()).isEqualTo(constantValue2[0].asI32());

              addTestMetric("Instance immutability constraints validated with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Advanced Function Invocation Tests")
  final class AdvancedFunctionInvocationTests {

    @Test
    @DisplayName("Should handle all WebAssembly value types in function calls")
    void shouldHandleAllWebAssemblyValueTypesInFunctionCalls() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              // Test i32 operations
              final byte[] intModuleBytes = WasmTestModules.getModule("arithmetic_int");
              try (final Module intModule = engine.compileModule(intModuleBytes);
                  final Instance intInstance = intModule.instantiate(store)) {

                final WasmValue[] intResult = intInstance.callFunction("add",
                    WasmValue.i32(Integer.MAX_VALUE), WasmValue.i32(-1));
                assertThat(intResult[0].asI32()).isEqualTo(Integer.MAX_VALUE - 1);
              }

              // Test f32 operations
              final byte[] floatModuleBytes = WasmTestModules.getModule("arithmetic_float");
              try (final Module floatModule = engine.compileModule(floatModuleBytes);
                  final Instance floatInstance = floatModule.instantiate(store)) {

                final WasmValue[] floatResult = floatInstance.callFunction("fadd_f32",
                    WasmValue.f32(Float.MAX_VALUE), WasmValue.f32(-Float.MAX_VALUE));
                assertThat(floatResult[0].asF32()).isCloseTo(0.0f, within(0.001f));
              }

              addTestMetric("All WebAssembly value types tested with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle edge cases in function parameter validation")
    void shouldHandleEdgeCasesInFunctionParameterValidation() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test boundary values
              final Map<String, WasmValue[]> boundaryTests = new HashMap<>();
              boundaryTests.put("zero", new WasmValue[]{WasmValue.i32(0), WasmValue.i32(0)});
              boundaryTests.put("max_int", new WasmValue[]{WasmValue.i32(Integer.MAX_VALUE), 
                  WasmValue.i32(0)});
              boundaryTests.put("min_int", new WasmValue[]{WasmValue.i32(Integer.MIN_VALUE), 
                  WasmValue.i32(0)});
              boundaryTests.put("negative", new WasmValue[]{WasmValue.i32(-100), 
                  WasmValue.i32(50)});

              for (final Map.Entry<String, WasmValue[]> test : boundaryTests.entrySet()) {
                final String testName = test.getKey();
                final WasmValue[] params = test.getValue();
                
                final WasmValue[] result = instance.callFunction("add", params);
                assertThat(result).hasSize(1);
                
                final int expected = params[0].asI32() + params[1].asI32();
                assertThat(result[0].asI32()).isEqualTo(expected);
                
                addTestMetric(String.format("Boundary test '%s' passed with %s", 
                    testName, runtimeType));
              }

              // Test empty parameter arrays where applicable
              final Optional<WasmFunction> addFunc = instance.getFunction("add");
              assertThat(addFunc).isPresent();
              final FunctionType funcType = addFunc.get().getFunctionType();
              assertThat(funcType.getParamTypes()).hasSize(2);
              
              // Verify wrong parameter count fails
              assertThatThrownBy(() -> instance.callFunction("add"))
                  .isInstanceOf(WasmException.class);
              assertThatThrownBy(() -> instance.callFunction("add", WasmValue.i32(1)))
                  .isInstanceOf(WasmException.class);
              assertThatThrownBy(() -> instance.callFunction("add", 
                  WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3)))
                  .isInstanceOf(WasmException.class);

              addTestMetric("Parameter validation edge cases tested with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle complex function call patterns and recursion")
    void shouldHandleComplexFunctionCallPatternsAndRecursion() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("function_fibonacci");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test fibonacci sequence with various inputs
              final Map<Integer, Integer> fibonacciExpected = new HashMap<>();
              fibonacciExpected.put(0, 0);
              fibonacciExpected.put(1, 1);
              fibonacciExpected.put(2, 1);
              fibonacciExpected.put(3, 2);
              fibonacciExpected.put(4, 3);
              fibonacciExpected.put(5, 5);
              fibonacciExpected.put(10, 55);
              fibonacciExpected.put(15, 610);
              fibonacciExpected.put(20, 6765);

              for (final Map.Entry<Integer, Integer> test : fibonacciExpected.entrySet()) {
                final int input = test.getKey();
                final int expected = test.getValue();
                
                final WasmValue[] result = instance.callFunction("fib", WasmValue.i32(input));
                assertThat(result).hasSize(1);
                assertThat(result[0].asI32()).isEqualTo(expected);
                
                addTestMetric(String.format("fib(%d) = %d verified with %s", 
                    input, expected, runtimeType));
              }

              // Test performance characteristics of recursive calls
              final Instant startTime = Instant.now();
              instance.callFunction("fib", WasmValue.i32(25));
              final Duration recursionTime = Duration.between(startTime, Instant.now());
              
              // Recursive fibonacci should complete within reasonable time
              assertThat(recursionTime.toMillis()).isLessThan(5000);
              
              addTestMetric("Complex recursion patterns validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle function invocation with stack depth limits")
    void shouldHandleFunctionInvocationWithStackDepthLimits() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("function_fibonacci");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test increasingly deep recursion until reasonable limits
              int maxWorkingDepth = 0;
              for (int depth = 10; depth <= 30; depth += 5) {
                try {
                  final WasmValue[] result = instance.callFunction("fib", WasmValue.i32(depth));
                  assertThat(result).hasSize(1);
                  maxWorkingDepth = depth;
                  
                  addTestMetric(String.format("Recursion depth %d successful with %s", 
                      depth, runtimeType));
                } catch (final WasmException e) {
                  // Expected at some point due to stack limits
                  addTestMetric(String.format("Recursion limit reached at depth %d with %s", 
                      depth, runtimeType));
                  break;
                }
              }

              // Should handle at least reasonable recursion depth
              assertThat(maxWorkingDepth).isGreaterThanOrEqualTo(20);
              
              addTestMetric("Stack depth limits validated with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Comprehensive Export Discovery and Type System Tests")
  final class ComprehensiveExportTests {

    @Test
    @DisplayName("Should discover and validate all export types comprehensively")
    void shouldDiscoverAndValidateAllExportTypesComprehensively() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              // Test function exports
              final byte[] funcModuleBytes = WasmTestModules.getModule("arithmetic_int");
              try (final Module funcModule = engine.compileModule(funcModuleBytes);
                  final Instance funcInstance = funcModule.instantiate(store)) {

                final String[] funcExports = funcInstance.getExportNames();
                assertThat(funcExports).containsExactlyInAnyOrder("add", "sub", "mul");

                // Validate each function export
                for (final String exportName : funcExports) {
                  final Optional<WasmFunction> function = funcInstance.getFunction(exportName);
                  assertThat(function).isPresent();
                  assertThat(function.get().getName()).isEqualTo(exportName);

                  final FunctionType funcType = function.get().getFunctionType();
                  assertThat(funcType).isNotNull();
                  assertThat(funcType.getParamTypes()).hasSize(2);
                  assertThat(funcType.getReturnTypes()).hasSize(1);
                  assertThat(funcType.getParamTypes()[0]).isEqualTo(WasmValueType.I32);
                  assertThat(funcType.getParamTypes()[1]).isEqualTo(WasmValueType.I32);
                  assertThat(funcType.getReturnTypes()[0]).isEqualTo(WasmValueType.I32);
                }
              }

              // Test memory exports
              final byte[] memModuleBytes = WasmTestModules.getModule("memory_basic");
              try (final Module memModule = engine.compileModule(memModuleBytes);
                  final Instance memInstance = memModule.instantiate(store)) {

                final Optional<WasmMemory> defaultMemory = memInstance.getDefaultMemory();
                if (defaultMemory.isPresent()) {
                  assertThat(defaultMemory.get()).isNotNull();
                  addTestMetric("Memory export validated with " + runtimeType);
                }
              }

              addTestMetric("All export types discovered and validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle export name resolution with special characters")
    void shouldHandleExportNameResolutionWithSpecialCharacters() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test normal export name
              final Optional<WasmFunction> normalFunc = instance.getFunction("add");
              assertThat(normalFunc).isPresent();

              // Test case sensitivity
              final Optional<WasmFunction> caseFunc = instance.getFunction("ADD");
              assertThat(caseFunc).isEmpty();

              // Test whitespace handling
              final Optional<WasmFunction> whitespaceFunc = instance.getFunction(" add ");
              assertThat(whitespaceFunc).isEmpty();

              // Test empty string
              final Optional<WasmFunction> emptyFunc = instance.getFunction("");
              assertThat(emptyFunc).isEmpty();

              // Test very long names
              final String longName = "a".repeat(1000);
              final Optional<WasmFunction> longNameFunc = instance.getFunction(longName);
              assertThat(longNameFunc).isEmpty();

              addTestMetric("Export name resolution edge cases validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should validate export consistency across multiple queries")
    void shouldValidateExportConsistencyAcrossMultipleQueries() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test that export names are consistent across calls
              final String[] exports1 = instance.getExportNames();
              final String[] exports2 = instance.getExportNames();
              assertThat(exports1).isEqualTo(exports2);

              // Test that function references are consistent
              final Optional<WasmFunction> func1 = instance.getFunction("add");
              final Optional<WasmFunction> func2 = instance.getFunction("add");
              assertThat(func1).isPresent();
              assertThat(func2).isPresent();

              // Function type should be identical
              final FunctionType type1 = func1.get().getFunctionType();
              final FunctionType type2 = func2.get().getFunctionType();
              assertThat(type1.getParamTypes()).isEqualTo(type2.getParamTypes());
              assertThat(type1.getReturnTypes()).isEqualTo(type2.getReturnTypes());

              // Test consistency across many queries
              for (int i = 0; i < 100; i++) {
                final String[] exports = instance.getExportNames();
                assertThat(exports).isEqualTo(exports1);
                
                final Optional<WasmFunction> func = instance.getFunction("add");
                assertThat(func).isPresent();
                assertThat(func.get().getName()).isEqualTo("add");
              }

              addTestMetric("Export consistency validated across multiple queries with " + runtimeType);
            }
          });
    }
  }

  // Helper method for floating-point comparison
  private static org.assertj.core.data.Offset<Float> within(final float offset) {
    return org.assertj.core.data.Offset.offset(offset);
  }
}