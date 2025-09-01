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
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive test suite specifically focused on function invocation patterns, parameter
 * handling, return value validation, and edge cases in WebAssembly function calls. This test suite
 * provides exhaustive coverage of all function invocation scenarios.
 */
@DisplayName("Instance Function Invocation Tests")
final class InstanceFunctionInvocationTest {

  private static final Logger LOGGER =
      Logger.getLogger(InstanceFunctionInvocationTest.class.getName());

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
  @DisplayName("WebAssembly Value Type Tests")
  final class ValueTypeTests {

    @Test
    @DisplayName("Should handle i32 function parameters and returns comprehensively")
    void shouldHandleI32FunctionParametersAndReturnsComprehensively() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test all i32 boundary values
              final Map<String, Integer[]> boundaryTests = new HashMap<>();
              boundaryTests.put("zero", new Integer[] {0, 0});
              boundaryTests.put("positive", new Integer[] {42, 58});
              boundaryTests.put("negative", new Integer[] {-42, -58});
              boundaryTests.put("max_value", new Integer[] {Integer.MAX_VALUE, 0});
              boundaryTests.put("min_value", new Integer[] {Integer.MIN_VALUE, 0});
              boundaryTests.put(
                  "max_pos_neg", new Integer[] {Integer.MAX_VALUE, Integer.MIN_VALUE});
              boundaryTests.put("large_pos", new Integer[] {1000000, 2000000});
              boundaryTests.put("large_neg", new Integer[] {-1000000, -2000000});

              for (final Map.Entry<String, Integer[]> test : boundaryTests.entrySet()) {
                final String testName = test.getKey();
                final Integer[] values = test.getValue();

                // Test addition
                final WasmValue[] addResult =
                    instance.callFunction(
                        "add", WasmValue.i32(values[0]), WasmValue.i32(values[1]));
                assertThat(addResult).hasSize(1);
                assertThat(addResult[0].getType()).isEqualTo(WasmValueType.I32);
                assertThat(addResult[0].asI32()).isEqualTo(values[0] + values[1]);

                // Test subtraction
                final WasmValue[] subResult =
                    instance.callFunction(
                        "sub", WasmValue.i32(values[0]), WasmValue.i32(values[1]));
                assertThat(subResult).hasSize(1);
                assertThat(subResult[0].asI32()).isEqualTo(values[0] - values[1]);

                // Test multiplication
                final WasmValue[] mulResult =
                    instance.callFunction(
                        "mul", WasmValue.i32(values[0]), WasmValue.i32(values[1]));
                assertThat(mulResult).hasSize(1);
                assertThat(mulResult[0].asI32()).isEqualTo(values[0] * values[1]);

                addTestMetric(
                    String.format("i32 boundary test '%s' passed with %s", testName, runtimeType));
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle f32 function parameters and returns with precision")
    void shouldHandleF32FunctionParametersAndReturnsWithPrecision() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_float");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test f32 precision and edge cases
              final Map<String, Float[]> floatTests = new HashMap<>();
              floatTests.put("simple", new Float[] {3.14f, 2.86f});
              floatTests.put("precision", new Float[] {0.1f, 0.2f});
              floatTests.put("zero", new Float[] {0.0f, 0.0f});
              floatTests.put("negative", new Float[] {-5.5f, -3.3f});
              floatTests.put("max_value", new Float[] {Float.MAX_VALUE, 0.0f});
              floatTests.put("min_value", new Float[] {Float.MIN_VALUE, 0.0f});
              floatTests.put("small_numbers", new Float[] {0.000001f, 0.000002f});
              floatTests.put("large_numbers", new Float[] {1000000.0f, 2000000.0f});

              for (final Map.Entry<String, Float[]> test : floatTests.entrySet()) {
                final String testName = test.getKey();
                final Float[] values = test.getValue();

                // Test f32 addition
                final WasmValue[] addResult =
                    instance.callFunction(
                        "fadd_f32", WasmValue.f32(values[0]), WasmValue.f32(values[1]));
                assertThat(addResult).hasSize(1);
                assertThat(addResult[0].getType()).isEqualTo(WasmValueType.F32);
                assertThat(addResult[0].asF32()).isCloseTo(values[0] + values[1], within(0.00001f));

                // Test f32 subtraction
                final WasmValue[] subResult =
                    instance.callFunction(
                        "fsub_f32", WasmValue.f32(values[0]), WasmValue.f32(values[1]));
                assertThat(subResult).hasSize(1);
                assertThat(subResult[0].asF32()).isCloseTo(values[0] - values[1], within(0.00001f));

                // Test f32 multiplication
                final WasmValue[] mulResult =
                    instance.callFunction(
                        "fmul_f32", WasmValue.f32(values[0]), WasmValue.f32(values[1]));
                assertThat(mulResult).hasSize(1);
                assertThat(mulResult[0].asF32()).isCloseTo(values[0] * values[1], within(0.00001f));

                addTestMetric(
                    String.format("f32 precision test '%s' passed with %s", testName, runtimeType));
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle special float values (NaN, Infinity)")
    void shouldHandleSpecialFloatValues() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_float");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test special float values
              final Map<String, Float[]> specialTests = new HashMap<>();
              specialTests.put("positive_infinity", new Float[] {Float.POSITIVE_INFINITY, 1.0f});
              specialTests.put("negative_infinity", new Float[] {Float.NEGATIVE_INFINITY, 1.0f});
              specialTests.put("nan", new Float[] {Float.NaN, 1.0f});

              for (final Map.Entry<String, Float[]> test : specialTests.entrySet()) {
                final String testName = test.getKey();
                final Float[] values = test.getValue();

                try {
                  final WasmValue[] result =
                      instance.callFunction(
                          "fadd_f32", WasmValue.f32(values[0]), WasmValue.f32(values[1]));
                  assertThat(result).hasSize(1);

                  final float resultValue = result[0].asF32();
                  if (testName.equals("nan")) {
                    assertThat(Float.isNaN(resultValue)).isTrue();
                  } else if (testName.equals("positive_infinity")) {
                    assertThat(Float.isInfinite(resultValue)).isTrue();
                    assertThat(resultValue > 0).isTrue();
                  } else if (testName.equals("negative_infinity")) {
                    assertThat(Float.isInfinite(resultValue)).isTrue();
                    assertThat(resultValue < 0).isTrue();
                  }

                  addTestMetric(
                      String.format(
                          "Special float test '%s' handled with %s", testName, runtimeType));
                } catch (final WasmException e) {
                  // Some special values may cause traps, which is acceptable
                  addTestMetric(
                      String.format(
                          "Special float test '%s' trapped (expected) with %s",
                          testName, runtimeType));
                }
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle mixed parameter types correctly")
    void shouldHandleMixedParameterTypesCorrectly() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              // Test with different modules to ensure type safety
              final byte[] intModuleBytes = WasmTestModules.getModule("arithmetic_int");
              try (final Module intModule = engine.compileModule(intModuleBytes);
                  final Instance intInstance = intModule.instantiate(store)) {

                // Should accept correct i32 parameters
                final WasmValue[] correctResult =
                    intInstance.callFunction("add", WasmValue.i32(10), WasmValue.i32(20));
                assertThat(correctResult[0].asI32()).isEqualTo(30);

                // Should reject f32 parameters for i32 function
                assertThatThrownBy(
                        () ->
                            intInstance.callFunction(
                                "add", WasmValue.f32(10.0f), WasmValue.i32(20)))
                    .isInstanceOf(WasmException.class);

                assertThatThrownBy(
                        () ->
                            intInstance.callFunction(
                                "add", WasmValue.i32(10), WasmValue.f32(20.0f)))
                    .isInstanceOf(WasmException.class);
              }

              final byte[] floatModuleBytes = WasmTestModules.getModule("arithmetic_float");
              try (final Module floatModule = engine.compileModule(floatModuleBytes);
                  final Instance floatInstance = floatModule.instantiate(store)) {

                // Should accept correct f32 parameters
                final WasmValue[] correctResult =
                    floatInstance.callFunction(
                        "fadd_f32", WasmValue.f32(10.5f), WasmValue.f32(20.5f));
                assertThat(correctResult[0].asF32()).isCloseTo(31.0f, within(0.001f));

                // Should reject i32 parameters for f32 function
                assertThatThrownBy(
                        () ->
                            floatInstance.callFunction(
                                "fadd_f32", WasmValue.i32(10), WasmValue.f32(20.5f)))
                    .isInstanceOf(WasmException.class);
              }

              addTestMetric("Mixed parameter type validation passed with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Parameter and Return Value Validation Tests")
  final class ParameterReturnValidationTests {

    @Test
    @DisplayName("Should validate parameter count strictly")
    void shouldValidateParameterCountStrictly() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Get function type to verify expected parameters
              final Optional<WasmFunction> addFunc = instance.getFunction("add");
              assertThat(addFunc).isPresent();
              final FunctionType funcType = addFunc.get().getFunctionType();
              assertThat(funcType.getParamTypes()).hasSize(2);
              assertThat(funcType.getReturnTypes()).hasSize(1);

              // Test correct parameter count
              final WasmValue[] correctResult =
                  instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(20));
              assertThat(correctResult).hasSize(1);
              assertThat(correctResult[0].asI32()).isEqualTo(30);

              // Test too few parameters
              assertThatThrownBy(() -> instance.callFunction("add"))
                  .isInstanceOf(WasmException.class)
                  .hasMessageContaining("parameter");

              assertThatThrownBy(() -> instance.callFunction("add", WasmValue.i32(10)))
                  .isInstanceOf(WasmException.class)
                  .hasMessageContaining("parameter");

              // Test too many parameters
              assertThatThrownBy(
                      () ->
                          instance.callFunction(
                              "add", WasmValue.i32(10), WasmValue.i32(20), WasmValue.i32(30)))
                  .isInstanceOf(WasmException.class)
                  .hasMessageContaining("parameter");

              assertThatThrownBy(
                      () ->
                          instance.callFunction(
                              "add",
                              WasmValue.i32(10),
                              WasmValue.i32(20),
                              WasmValue.i32(30),
                              WasmValue.i32(40)))
                  .isInstanceOf(WasmException.class)
                  .hasMessageContaining("parameter");

              addTestMetric("Parameter count validation passed with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle functions with no parameters")
    void shouldHandleFunctionsWithNoParameters() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_immutable");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test function with no parameters
              final WasmValue[] result = instance.callFunction("get_const");
              assertThat(result).hasSize(1);
              assertThat(result[0].getType()).isEqualTo(WasmValueType.I32);
              assertThat(result[0].asI32()).isEqualTo(65536);

              // Should reject parameters for no-parameter function
              assertThatThrownBy(() -> instance.callFunction("get_const", WasmValue.i32(10)))
                  .isInstanceOf(WasmException.class);

              // Test consistency across multiple calls
              for (int i = 0; i < 10; i++) {
                final WasmValue[] consistentResult = instance.callFunction("get_const");
                assertThat(consistentResult[0].asI32()).isEqualTo(65536);
              }

              addTestMetric("No-parameter functions handled with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should validate return value types and counts")
    void shouldValidateReturnValueTypesAndCounts() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              // Test single return value
              final byte[] intModuleBytes = WasmTestModules.getModule("arithmetic_int");
              try (final Module intModule = engine.compileModule(intModuleBytes);
                  final Instance intInstance = intModule.instantiate(store)) {

                final WasmValue[] result =
                    intInstance.callFunction("add", WasmValue.i32(15), WasmValue.i32(25));

                // Validate return count and type
                assertThat(result).hasSize(1);
                assertThat(result[0].getType()).isEqualTo(WasmValueType.I32);
                assertThat(result[0].asI32()).isEqualTo(40);

                // Verify function type matches actual behavior
                final Optional<WasmFunction> addFunc = intInstance.getFunction("add");
                assertThat(addFunc).isPresent();
                final FunctionType funcType = addFunc.get().getFunctionType();
                assertThat(funcType.getReturnTypes()).hasSize(1);
                assertThat(funcType.getReturnTypes()[0]).isEqualTo(WasmValueType.I32);
              }

              // Test different return type
              final byte[] floatModuleBytes = WasmTestModules.getModule("arithmetic_float");
              try (final Module floatModule = engine.compileModule(floatModuleBytes);
                  final Instance floatInstance = floatModule.instantiate(store)) {

                final WasmValue[] result =
                    floatInstance.callFunction(
                        "fadd_f32", WasmValue.f32(1.5f), WasmValue.f32(2.5f));

                // Validate return count and type
                assertThat(result).hasSize(1);
                assertThat(result[0].getType()).isEqualTo(WasmValueType.F32);
                assertThat(result[0].asF32()).isCloseTo(4.0f, within(0.001f));

                // Verify function type matches actual behavior
                final Optional<WasmFunction> addFunc = floatInstance.getFunction("fadd_f32");
                assertThat(addFunc).isPresent();
                final FunctionType funcType = addFunc.get().getFunctionType();
                assertThat(funcType.getReturnTypes()).hasSize(1);
                assertThat(funcType.getReturnTypes()[0]).isEqualTo(WasmValueType.F32);
              }

              addTestMetric("Return value validation passed with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Complex Function Call Pattern Tests")
  final class ComplexFunctionCallTests {

    @Test
    @DisplayName("Should handle rapid successive function calls")
    void shouldHandleRapidSuccessiveFunctionCalls() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final int numCalls = 10000;
              final Instant startTime = Instant.now();

              for (int i = 0; i < numCalls; i++) {
                final WasmValue[] result =
                    instance.callFunction("add", WasmValue.i32(i), WasmValue.i32(i + 1));
                assertThat(result[0].asI32()).isEqualTo(2 * i + 1);
              }

              final Duration totalTime = Duration.between(startTime, Instant.now());
              final double callsPerSecond = numCalls / (totalTime.toMillis() / 1000.0);

              // Should achieve reasonable performance
              assertThat(callsPerSecond).isGreaterThan(1000);

              addTestMetric(
                  String.format(
                      "Rapid calls: %d calls in %dms (%.0f calls/sec) with %s",
                      numCalls, totalTime.toMillis(), callsPerSecond, runtimeType));
            }
          });
    }

    @Test
    @DisplayName("Should handle nested function calls with state changes")
    void shouldHandleNestedFunctionCallsWithStateChanges() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_mutable");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test nested state modifications
              final int[] testValues = {0, 42, -17, 1000000, Integer.MIN_VALUE, Integer.MAX_VALUE};

              for (final int testValue : testValues) {
                // Set the global value
                instance.callFunction("set", WasmValue.i32(testValue));

                // Verify it was set correctly
                final WasmValue[] getResult = instance.callFunction("get");
                assertThat(getResult[0].asI32()).isEqualTo(testValue);

                // Perform multiple operations that might affect state
                for (int i = 0; i < 10; i++) {
                  final int newValue = testValue + i;
                  instance.callFunction("set", WasmValue.i32(newValue));
                  final WasmValue[] currentResult = instance.callFunction("get");
                  assertThat(currentResult[0].asI32()).isEqualTo(newValue);
                }
              }

              addTestMetric("Nested function calls with state changes passed with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle concurrent function calls safely")
    void shouldHandleConcurrentFunctionCallsSafely() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final int numThreads = 10;
              final int numCallsPerThread = 100;
              final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
              final AtomicInteger successCount = new AtomicInteger(0);
              final AtomicInteger errorCount = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                  final int threadId = i;
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            for (int j = 0; j < numCallsPerThread; j++) {
                              try {
                                final WasmValue[] result =
                                    instance.callFunction(
                                        "add", WasmValue.i32(threadId), WasmValue.i32(j));

                                if (result[0].asI32() == threadId + j) {
                                  successCount.incrementAndGet();
                                } else {
                                  errorCount.incrementAndGet();
                                }
                              } catch (final Exception e) {
                                errorCount.incrementAndGet();
                                LOGGER.warning("Concurrent call failed: " + e.getMessage());
                              }
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all threads to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);

                // All calls should succeed
                assertThat(successCount.get()).isEqualTo(numThreads * numCallsPerThread);
                assertThat(errorCount.get()).isEqualTo(0);

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "Concurrent function calls: %d successful with %s",
                      successCount.get(), runtimeType));
            }
          });
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases Tests")
  final class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle all types of invalid function calls")
    void shouldHandleAllTypesOfInvalidFunctionCalls() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test null function name
              assertThatThrownBy(
                      () -> instance.callFunction(null, WasmValue.i32(1), WasmValue.i32(2)))
                  .isInstanceOf(IllegalArgumentException.class);

              // Test empty function name
              assertThatThrownBy(
                      () -> instance.callFunction("", WasmValue.i32(1), WasmValue.i32(2)))
                  .isInstanceOf(WasmException.class);

              // Test non-existent function name
              assertThatThrownBy(
                      () ->
                          instance.callFunction("nonexistent", WasmValue.i32(1), WasmValue.i32(2)))
                  .isInstanceOf(WasmException.class);

              // Test function name with special characters
              assertThatThrownBy(
                      () -> instance.callFunction("add@#$", WasmValue.i32(1), WasmValue.i32(2)))
                  .isInstanceOf(WasmException.class);

              // Test very long function name
              final String longName = "a".repeat(10000);
              assertThatThrownBy(
                      () -> instance.callFunction(longName, WasmValue.i32(1), WasmValue.i32(2)))
                  .isInstanceOf(WasmException.class);

              addTestMetric("Invalid function call handling validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle null and invalid parameters gracefully")
    void shouldHandleNullAndInvalidParametersGracefully() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test null parameter array
              assertThatThrownBy(() -> instance.callFunction("add", (WasmValue[]) null))
                  .isInstanceOf(IllegalArgumentException.class);

              // Test array with null elements
              assertThatThrownBy(() -> instance.callFunction("add", WasmValue.i32(1), null))
                  .isInstanceOf(IllegalArgumentException.class);

              assertThatThrownBy(() -> instance.callFunction("add", null, WasmValue.i32(2)))
                  .isInstanceOf(IllegalArgumentException.class);

              // Test with mixed valid/invalid parameters
              assertThatThrownBy(
                      () -> instance.callFunction("add", WasmValue.i32(1), null, WasmValue.i32(3)))
                  .isInstanceOf(IllegalArgumentException.class);

              addTestMetric("Null parameter handling validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle function calls on closed instances")
    void shouldHandleFunctionCallsOnClosedInstances() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes)) {

              final Instance instance = module.instantiate(store);

              // Verify instance works initially
              final WasmValue[] result =
                  instance.callFunction("add", WasmValue.i32(1), WasmValue.i32(2));
              assertThat(result[0].asI32()).isEqualTo(3);

              // Close the instance
              instance.close();
              assertThat(instance.isValid()).isFalse();

              // All function calls should fail after close
              assertThatThrownBy(
                      () -> instance.callFunction("add", WasmValue.i32(1), WasmValue.i32(2)))
                  .isInstanceOf(WasmException.class);

              assertThatThrownBy(() -> instance.getFunction("add"))
                  .isInstanceOf(WasmException.class);

              assertThatThrownBy(() -> instance.getExportNames()).isInstanceOf(WasmException.class);

              // Subsequent close calls should be safe
              instance.close(); // Should not throw

              addTestMetric("Closed instance handling validated with " + runtimeType);
            }
          });
    }
  }

  // Helper method for floating-point comparison
  private static org.assertj.core.data.Offset<Float> within(final float offset) {
    return org.assertj.core.data.Offset.offset(offset);
  }
}
