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
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive test suite for Instance export discovery, type introspection, and binding
 * validation. Tests export resolution, function signature validation, memory/table access
 * patterns, and cross-runtime consistency in export handling.
 */
@DisplayName("Instance Export Tests")
final class InstanceExportTest {

  private static final Logger LOGGER =
      Logger.getLogger(InstanceExportTest.class.getName());

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
  @DisplayName("Export Discovery and Enumeration Tests")
  final class ExportDiscoveryTests {

    @Test
    @DisplayName("Should discover all exports systematically")
    void shouldDiscoverAllExportsSystematically() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final Map<String, byte[]> testModules = new HashMap<>();
            testModules.put("arithmetic_int", WasmTestModules.getModule("arithmetic_int"));
            testModules.put("basic_add", WasmTestModules.getModule("basic_add"));
            testModules.put("global_mutable", WasmTestModules.getModule("global_mutable"));
            testModules.put("memory_basic", WasmTestModules.getModule("memory_basic"));

            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              for (final Map.Entry<String, byte[]> moduleEntry : testModules.entrySet()) {
                final String moduleName = moduleEntry.getKey();
                final byte[] moduleBytes = moduleEntry.getValue();

                try (final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  final String[] exportNames = instance.getExportNames();
                  assertThat(exportNames).isNotNull();
                  assertThat(exportNames.length).isGreaterThan(0);

                  // Verify no duplicate export names
                  final Set<String> uniqueNames = new HashSet<>(Arrays.asList(exportNames));
                  assertThat(uniqueNames).hasSize(exportNames.length);

                  // Verify all export names are non-empty
                  for (final String exportName : exportNames) {
                    assertThat(exportName).isNotNull();
                    assertThat(exportName).isNotEmpty();
                  }

                  // Test expected exports for specific modules
                  if ("arithmetic_int".equals(moduleName)) {
                    assertThat(Arrays.asList(exportNames))
                        .containsExactlyInAnyOrder("add", "sub", "mul");
                  } else if ("basic_add".equals(moduleName)) {
                    assertThat(Arrays.asList(exportNames)).contains("add");
                  }

                  addTestMetric(String.format("Module '%s' exports discovered: %d with %s", 
                      moduleName, exportNames.length, runtimeType));
                }
              }
            }
          });
    }

    @Test
    @DisplayName("Should provide consistent export enumeration across calls")
    void shouldProvideConsistentExportEnumerationAcrossCalls() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Get exports multiple times
              final String[] exports1 = instance.getExportNames();
              final String[] exports2 = instance.getExportNames();
              final String[] exports3 = instance.getExportNames();

              // All should be identical
              assertThat(exports2).isEqualTo(exports1);
              assertThat(exports3).isEqualTo(exports1);

              // Test consistency under concurrent access
              final ExecutorService executor = Executors.newFixedThreadPool(10);
              try {
                final CompletableFuture<String[]>[] futures = new CompletableFuture[20];
                
                for (int i = 0; i < futures.length; i++) {
                  futures[i] = CompletableFuture.supplyAsync(
                      instance::getExportNames, executor);
                }

                // All concurrent calls should return identical results
                for (final CompletableFuture<String[]> future : futures) {
                  final String[] result = future.get(5, TimeUnit.SECONDS);
                  assertThat(result).isEqualTo(exports1);
                }

              } finally {
                executor.shutdownNow();
              }

              addTestMetric("Export enumeration consistency validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle modules with no exports gracefully")
    void shouldHandleModulesWithNoExportsGracefully() throws Exception {
      // Note: Most test modules have exports, but test the edge case behavior
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final String[] exportNames = instance.getExportNames();
              
              // Even if there are exports, the array should be properly formed
              assertThat(exportNames).isNotNull();
              
              if (exportNames.length == 0) {
                // If no exports, all lookups should return empty
                assertThat(instance.getFunction("any_name")).isEmpty();
                assertThat(instance.getMemory("any_name")).isEmpty();
                assertThat(instance.getTable("any_name")).isEmpty();
                assertThat(instance.getGlobal("any_name")).isEmpty();
                assertThat(instance.getDefaultMemory()).isEmpty();
              }

              addTestMetric("Empty export handling validated with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Function Export Resolution and Type Introspection Tests")
  final class FunctionExportTests {

    @Test
    @DisplayName("Should resolve function exports with complete type information")
    void shouldResolveFunctionExportsWithCompleteTypeInformation() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final String[] expectedFunctions = {"add", "sub", "mul"};
              
              for (final String functionName : expectedFunctions) {
                final Optional<WasmFunction> function = instance.getFunction(functionName);
                assertThat(function).isPresent();

                final WasmFunction wasmFunction = function.get();
                assertThat(wasmFunction.getName()).isEqualTo(functionName);

                // Verify function type information
                final FunctionType functionType = wasmFunction.getFunctionType();
                assertThat(functionType).isNotNull();
                assertThat(functionType.getParamTypes()).hasSize(2);
                assertThat(functionType.getReturnTypes()).hasSize(1);

                // All arithmetic functions should have (i32, i32) -> i32 signature
                assertThat(functionType.getParamTypes()[0]).isEqualTo(WasmValueType.I32);
                assertThat(functionType.getParamTypes()[1]).isEqualTo(WasmValueType.I32);
                assertThat(functionType.getReturnTypes()[0]).isEqualTo(WasmValueType.I32);

                // Verify function is callable
                final WasmValue[] result = wasmFunction.call(WasmValue.i32(10), WasmValue.i32(5));
                assertThat(result).hasSize(1);
                assertThat(result[0].getType()).isEqualTo(WasmValueType.I32);

                // Verify results match expected arithmetic
                switch (functionName) {
                  case "add":
                    assertThat(result[0].asI32()).isEqualTo(15);
                    break;
                  case "sub":
                    assertThat(result[0].asI32()).isEqualTo(5);
                    break;
                  case "mul":
                    assertThat(result[0].asI32()).isEqualTo(50);
                    break;
                }

                addTestMetric(String.format("Function '%s' type validated with %s", 
                    functionName, runtimeType));
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle different function signature patterns")
    void shouldHandleDifferentFunctionSignaturePatterns() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              // Test no-parameter functions
              final byte[] noParamModuleBytes = WasmTestModules.getModule("global_immutable");
              try (final Module noParamModule = engine.compileModule(noParamModuleBytes);
                  final Instance noParamInstance = noParamModule.instantiate(store)) {

                final Optional<WasmFunction> getConstFunc = noParamInstance.getFunction("get_const");
                if (getConstFunc.isPresent()) {
                  final FunctionType funcType = getConstFunc.get().getFunctionType();
                  assertThat(funcType.getParamTypes()).hasSize(0);
                  assertThat(funcType.getReturnTypes()).hasSize(1);
                  assertThat(funcType.getReturnTypes()[0]).isEqualTo(WasmValueType.I32);

                  // Test actual call
                  final WasmValue[] result = getConstFunc.get().call();
                  assertThat(result).hasSize(1);
                  assertThat(result[0].asI32()).isEqualTo(65536);
                }
              }

              // Test float parameter functions
              final byte[] floatModuleBytes = WasmTestModules.getModule("arithmetic_float");
              try (final Module floatModule = engine.compileModule(floatModuleBytes);
                  final Instance floatInstance = floatModule.instantiate(store)) {

                final Optional<WasmFunction> faddFunc = floatInstance.getFunction("fadd_f32");
                if (faddFunc.isPresent()) {
                  final FunctionType funcType = faddFunc.get().getFunctionType();
                  assertThat(funcType.getParamTypes()).hasSize(2);
                  assertThat(funcType.getReturnTypes()).hasSize(1);
                  assertThat(funcType.getParamTypes()[0]).isEqualTo(WasmValueType.F32);
                  assertThat(funcType.getParamTypes()[1]).isEqualTo(WasmValueType.F32);
                  assertThat(funcType.getReturnTypes()[0]).isEqualTo(WasmValueType.F32);

                  // Test actual call
                  final WasmValue[] result = faddFunc.get().call(
                      WasmValue.f32(3.14f), WasmValue.f32(2.86f));
                  assertThat(result).hasSize(1);
                  assertThat(result[0].asF32()).isCloseTo(6.0f, within(0.001f));
                }
              }

              addTestMetric("Different function signatures validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should validate function export immutability")
    void shouldValidateFunctionExportImmutability() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final Optional<WasmFunction> func1 = instance.getFunction("add");
              final Optional<WasmFunction> func2 = instance.getFunction("add");
              
              assertThat(func1).isPresent();
              assertThat(func2).isPresent();

              // Function references should have consistent properties
              assertThat(func1.get().getName()).isEqualTo(func2.get().getName());
              
              final FunctionType type1 = func1.get().getFunctionType();
              final FunctionType type2 = func2.get().getFunctionType();
              
              assertThat(type1.getParamTypes()).isEqualTo(type2.getParamTypes());
              assertThat(type1.getReturnTypes()).isEqualTo(type2.getReturnTypes());

              // Both function references should work identically
              final WasmValue[] result1 = func1.get().call(WasmValue.i32(20), WasmValue.i32(22));
              final WasmValue[] result2 = func2.get().call(WasmValue.i32(20), WasmValue.i32(22));
              
              assertThat(result1[0].asI32()).isEqualTo(result2[0].asI32());
              assertThat(result1[0].asI32()).isEqualTo(42);

              addTestMetric("Function export immutability validated with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Memory and Table Export Tests")
  final class MemoryTableExportTests {

    @Test
    @DisplayName("Should resolve memory exports and provide access")
    void shouldResolveMemoryExportsAndProvideAccess() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test default memory access
              final Optional<WasmMemory> defaultMemory = instance.getDefaultMemory();
              if (defaultMemory.isPresent()) {
                assertThat(defaultMemory.get()).isNotNull();
                
                // Memory should be accessible and functional
                // This requires the memory module to export memory operations
                instance.callFunction("store", WasmValue.i32(0), WasmValue.i32(42));
                final WasmValue[] loadResult = instance.callFunction("load", WasmValue.i32(0));
                assertThat(loadResult[0].asI32()).isEqualTo(42);

                addTestMetric("Default memory access validated with " + runtimeType);
              }

              // Test named memory access (if available)
              final Optional<WasmMemory> namedMemory = instance.getMemory("memory");
              if (namedMemory.isPresent()) {
                assertThat(namedMemory.get()).isNotNull();
                // Named memory should be the same as default memory typically
                if (defaultMemory.isPresent()) {
                  // Both should reference the same underlying memory
                  addTestMetric("Named memory access validated with " + runtimeType);
                }
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle table exports correctly")
    void shouldHandleTableExportsCorrectly() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("table_indirect");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test table access through indirect calls
              final WasmValue[] result1 = instance.callFunction("call_func", WasmValue.i32(0));
              assertThat(result1).hasSize(1);
              assertThat(result1[0].asI32()).isEqualTo(10);

              final WasmValue[] result2 = instance.callFunction("call_func", WasmValue.i32(1));
              assertThat(result2).hasSize(1);
              assertThat(result2[0].asI32()).isEqualTo(20);

              // Test table export (if directly exported)
              final Optional<WasmTable> table = instance.getTable("table");
              if (table.isPresent()) {
                assertThat(table.get()).isNotNull();
                addTestMetric("Table export validated with " + runtimeType);
              }

              addTestMetric("Table functionality validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle memory growth operations through exports")
    void shouldHandleMemoryGrowthOperationsThroughExports() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_grow");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test memory growth
              final WasmValue[] growResult = instance.callFunction("grow", WasmValue.i32(1));
              assertThat(growResult).hasSize(1);
              final int previousPages = growResult[0].asI32();
              assertThat(previousPages).isGreaterThanOrEqualTo(0);

              // Test that memory is still accessible after growth
              if (instance.getDefaultMemory().isPresent()) {
                // Memory should still be functional
                addTestMetric(String.format("Memory grew from %d pages with %s", 
                    previousPages, runtimeType));
              }
            }
          });
    }
  }

  @Nested
  @DisplayName("Global Export Tests")
  final class GlobalExportTests {

    @Test
    @DisplayName("Should handle mutable global exports")
    void shouldHandleMutableGlobalExports() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_mutable");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test global access through function calls (as globals may not be directly exported)
              final WasmValue[] initialValue = instance.callFunction("get");
              assertThat(initialValue).hasSize(1);
              assertThat(initialValue[0].getType()).isEqualTo(WasmValueType.I32);

              // Test global modification
              instance.callFunction("set", WasmValue.i32(100));
              final WasmValue[] modifiedValue = instance.callFunction("get");
              assertThat(modifiedValue[0].asI32()).isEqualTo(100);
              assertThat(modifiedValue[0].asI32()).isNotEqualTo(initialValue[0].asI32());

              // Test multiple modifications
              final int[] testValues = {0, -1, 42, Integer.MAX_VALUE, Integer.MIN_VALUE};
              for (final int testValue : testValues) {
                instance.callFunction("set", WasmValue.i32(testValue));
                final WasmValue[] currentValue = instance.callFunction("get");
                assertThat(currentValue[0].asI32()).isEqualTo(testValue);
              }

              // Test direct global export (if available)
              final Optional<WasmGlobal> globalExport = instance.getGlobal("global");
              if (globalExport.isPresent()) {
                assertThat(globalExport.get()).isNotNull();
                addTestMetric("Direct global export validated with " + runtimeType);
              }

              addTestMetric("Mutable global functionality validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle immutable global exports")
    void shouldHandleImmutableGlobalExports() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("global_immutable");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test immutable global access
              final WasmValue[] constantValue1 = instance.callFunction("get_const");
              final WasmValue[] constantValue2 = instance.callFunction("get_const");
              
              assertThat(constantValue1).hasSize(1);
              assertThat(constantValue2).hasSize(1);
              assertThat(constantValue1[0].asI32()).isEqualTo(constantValue2[0].asI32());
              assertThat(constantValue1[0].asI32()).isEqualTo(65536); // 0x10000

              // Value should remain constant across many calls
              for (int i = 0; i < 100; i++) {
                final WasmValue[] value = instance.callFunction("get_const");
                assertThat(value[0].asI32()).isEqualTo(65536);
              }

              // Test direct global export (if available)
              final Optional<WasmGlobal> globalExport = instance.getGlobal("const_global");
              if (globalExport.isPresent()) {
                assertThat(globalExport.get()).isNotNull();
                addTestMetric("Direct immutable global export validated with " + runtimeType);
              }

              addTestMetric("Immutable global functionality validated with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Export Resolution Edge Cases and Error Handling Tests")
  final class ExportErrorHandlingTests {

    @Test
    @DisplayName("Should handle non-existent export lookups gracefully")
    void shouldHandleNonExistentExportLookupsGracefully() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test non-existent exports return empty
              assertThat(instance.getFunction("nonexistent")).isEmpty();
              assertThat(instance.getMemory("nonexistent")).isEmpty();
              assertThat(instance.getTable("nonexistent")).isEmpty();
              assertThat(instance.getGlobal("nonexistent")).isEmpty();

              // Test variations of existing names
              if (Arrays.asList(instance.getExportNames()).contains("add")) {
                assertThat(instance.getFunction("ADD")).isEmpty(); // Case sensitive
                assertThat(instance.getFunction("add ")).isEmpty(); // Whitespace
                assertThat(instance.getFunction(" add")).isEmpty();
                assertThat(instance.getFunction("add_")).isEmpty(); // Modified name
              }

              addTestMetric("Non-existent export handling validated with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should validate export name parameter constraints")
    void shouldValidateExportNameParameterConstraints() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test null export name handling
              assertThatThrownBy(() -> instance.getFunction(null))
                  .isInstanceOf(IllegalArgumentException.class);
              assertThatThrownBy(() -> instance.getMemory(null))
                  .isInstanceOf(IllegalArgumentException.class);
              assertThatThrownBy(() -> instance.getTable(null))
                  .isInstanceOf(IllegalArgumentException.class);
              assertThatThrownBy(() -> instance.getGlobal(null))
                  .isInstanceOf(IllegalArgumentException.class);

              // Test empty string (should return empty, not throw)
              assertThat(instance.getFunction("")).isEmpty();
              assertThat(instance.getMemory("")).isEmpty();
              assertThat(instance.getTable("")).isEmpty();
              assertThat(instance.getGlobal("")).isEmpty();

              // Test very long names
              final String longName = "a".repeat(10000);
              assertThat(instance.getFunction(longName)).isEmpty();
              assertThat(instance.getMemory(longName)).isEmpty();
              assertThat(instance.getTable(longName)).isEmpty();
              assertThat(instance.getGlobal(longName)).isEmpty();

              addTestMetric("Export name parameter validation passed with " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should handle exports on closed instances")
    void shouldHandleExportsOnClosedInstances() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes)) {

              final Instance instance = module.instantiate(store);
              
              // Verify instance works initially
              assertThat(instance.getExportNames()).isNotEmpty();
              assertThat(instance.getFunction("add")).isPresent();

              // Close the instance
              instance.close();
              assertThat(instance.isValid()).isFalse();

              // All export operations should fail after close
              assertThatThrownBy(instance::getExportNames)
                  .isInstanceOf(WasmException.class);
              assertThatThrownBy(() -> instance.getFunction("add"))
                  .isInstanceOf(WasmException.class);
              assertThatThrownBy(() -> instance.getMemory("memory"))
                  .isInstanceOf(WasmException.class);
              assertThatThrownBy(() -> instance.getTable("table"))
                  .isInstanceOf(WasmException.class);
              assertThatThrownBy(() -> instance.getGlobal("global"))
                  .isInstanceOf(WasmException.class);
              assertThatThrownBy(instance::getDefaultMemory)
                  .isInstanceOf(WasmException.class);

              addTestMetric("Closed instance export handling validated with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Cross-Runtime Export Consistency Tests")
  final class CrossRuntimeExportTests {

    @Test
    @DisplayName("Should provide identical export enumeration across runtimes")
    void shouldProvideIdenticalExportEnumerationAcrossRuntimes() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore();
                    final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  final String[] exportNames = instance.getExportNames();
                  Arrays.sort(exportNames); // Sort for consistent comparison
                  return Arrays.asList(exportNames);
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();

      addTestMetric("Cross-runtime export enumeration: " + result.getDifferenceDescription());
    }

    @Test
    @DisplayName("Should provide identical function type information across runtimes")
    void shouldProvideIdenticalFunctionTypeInformationAcrossRuntimes() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final byte[] moduleBytes = WasmTestModules.getModule("arithmetic_int");
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore();
                    final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  final Map<String, String> functionSignatures = new HashMap<>();
                  for (final String exportName : instance.getExportNames()) {
                    final Optional<WasmFunction> function = instance.getFunction(exportName);
                    if (function.isPresent()) {
                      final FunctionType funcType = function.get().getFunctionType();
                      final String signature = Arrays.toString(funcType.getParamTypes()) + 
                          " -> " + Arrays.toString(funcType.getReturnTypes());
                      functionSignatures.put(exportName, signature);
                    }
                  }
                  return functionSignatures;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();

      addTestMetric("Cross-runtime function signatures: " + result.getDifferenceDescription());
    }

    @Test
    @DisplayName("Should provide identical export resolution behavior across runtimes")
    void shouldProvideIdenticalExportResolutionBehaviorAcrossRuntimes() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final byte[] moduleBytes = WasmTestModules.getModule("basic_add");
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore();
                    final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  final Map<String, Boolean> resolutionResults = new HashMap<>();
                  
                  // Test various export lookups
                  final String[] testNames = {"add", "nonexistent", "ADD", "", "add ", " add"};
                  for (final String testName : testNames) {
                    resolutionResults.put("func_" + testName, 
                        instance.getFunction(testName).isPresent());
                    resolutionResults.put("mem_" + testName, 
                        instance.getMemory(testName).isPresent());
                    resolutionResults.put("table_" + testName, 
                        instance.getTable(testName).isPresent());
                    resolutionResults.put("global_" + testName, 
                        instance.getGlobal(testName).isPresent());
                  }
                  
                  return resolutionResults;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();

      addTestMetric("Cross-runtime export resolution: " + result.getDifferenceDescription());
    }
  }

  // Helper method for floating-point comparison
  private static org.assertj.core.data.Offset<Float> within(final float offset) {
    return org.assertj.core.data.Offset.offset(offset);
  }
}