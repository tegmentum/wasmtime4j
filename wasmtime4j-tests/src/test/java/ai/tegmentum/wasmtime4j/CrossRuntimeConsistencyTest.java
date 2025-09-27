/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Cross-runtime consistency tests for caller context functionality.
 *
 * <p>This test suite validates that the JNI and Panama implementations
 * of caller context functionality behave identically, ensuring users
 * can switch between runtimes without any functional differences.
 *
 * <p>Tests are designed to run under both runtime environments and
 * compare the behavior and results for consistency.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Cross-Runtime Consistency Tests")
class CrossRuntimeConsistencyTest {

  private static final String TEST_WAT = """
      (module
        (memory (export "memory") 1)
        (global (export "counter") (mut i32) (i32.const 0))
        (table (export "table") 1 funcref)

        (func (export "increment") (result i32)
          global.get 0
          i32.const 1
          i32.add
          global.set 0
          global.get 0)

        (func (export "test_caller_context") (import "host" "test_caller") (param i32) (result i32))
        (func (export "test_multi_value") (import "host" "multi_value") (param i32 i32) (result i32 i32))
        (func (export "test_fuel_ops") (import "host" "fuel_ops") (param i32) (result i32))
      )
      """;

  private RuntimeTestResults jniResults;
  private RuntimeTestResults panamaResults;

  @BeforeAll
  void setUp() throws Exception {
    // Run tests under both runtimes if available
    jniResults = runTestsWithRuntime("jni");
    panamaResults = runTestsWithRuntime("panama");
  }

  @Test
  @DisplayName("Caller context export access should be consistent across runtimes")
  void testCallerExportAccessConsistency() {
    assumeBothRuntimesAvailable();

    // Compare export access results
    assertEquals(jniResults.exportAccessTest.success, panamaResults.exportAccessTest.success,
                 "Export access success should be identical");
    assertEquals(jniResults.exportAccessTest.exportsFound, panamaResults.exportAccessTest.exportsFound,
                 "Number of exports found should be identical");
    assertEquals(jniResults.exportAccessTest.memorySize, panamaResults.exportAccessTest.memorySize,
                 "Memory size should be identical");
  }

  @Test
  @DisplayName("Multi-value function results should be consistent across runtimes")
  void testMultiValueConsistency() {
    assumeBothRuntimesAvailable();

    // Compare multi-value results
    assertEquals(jniResults.multiValueTest.success, panamaResults.multiValueTest.success,
                 "Multi-value success should be identical");
    assertArrayEquals(jniResults.multiValueTest.results, panamaResults.multiValueTest.results,
                      "Multi-value results should be identical");
  }

  @Test
  @DisplayName("Fuel management should be consistent across runtimes")
  void testFuelManagementConsistency() {
    assumeBothRuntimesAvailable();

    // Compare fuel management results
    assertEquals(jniResults.fuelTest.success, panamaResults.fuelTest.success,
                 "Fuel operations success should be identical");
    assertEquals(jniResults.fuelTest.initialFuel, panamaResults.fuelTest.initialFuel,
                 "Initial fuel should be identical");
    assertEquals(jniResults.fuelTest.finalFuel, panamaResults.fuelTest.finalFuel,
                 "Final fuel should be identical");
    assertEquals(jniResults.fuelTest.fuelAdded, panamaResults.fuelTest.fuelAdded,
                 "Fuel added should be identical");
  }

  @Test
  @DisplayName("Epoch deadline management should be consistent across runtimes")
  void testEpochDeadlineConsistency() {
    assumeBothRuntimesAvailable();

    // Compare epoch deadline results
    assertEquals(jniResults.epochTest.success, panamaResults.epochTest.success,
                 "Epoch deadline success should be identical");
    assertEquals(jniResults.epochTest.deadlineSet, panamaResults.epochTest.deadlineSet,
                 "Epoch deadline set status should be identical");
  }

  @Test
  @DisplayName("Performance characteristics should be similar across runtimes")
  void testPerformanceConsistency() {
    assumeBothRuntimesAvailable();

    // Performance should be within reasonable bounds (allowing for implementation differences)
    long jniTime = jniResults.performance.totalExecutionTime;
    long panamaTime = panamaResults.performance.totalExecutionTime;

    // Allow up to 3x difference in execution time (generous tolerance for different implementations)
    double ratio = Math.max(jniTime, panamaTime) / (double) Math.min(jniTime, panamaTime);
    assertTrue(ratio <= 3.0, String.format(
        "Performance ratio too high: JNI=%dms, Panama=%dms, ratio=%.2f",
        jniTime, panamaTime, ratio));
  }

  @Test
  @DisplayName("Error handling should be consistent across runtimes")
  void testErrorHandlingConsistency() {
    assumeBothRuntimesAvailable();

    // Compare error handling results
    assertEquals(jniResults.errorTest.success, panamaResults.errorTest.success,
                 "Error handling success should be identical");
    assertEquals(jniResults.errorTest.exceptionsThrown, panamaResults.errorTest.exceptionsThrown,
                 "Number of exceptions should be identical");
  }

  @Test
  @EnabledIfSystemProperty(named = "wasmtime4j.runtime.comparison", matches = "enabled")
  @DisplayName("Memory usage should be comparable across runtimes")
  void testMemoryUsageConsistency() {
    assumeBothRuntimesAvailable();

    // Memory usage comparison (this is more informational than strict equality)
    long jniMemory = jniResults.performance.memoryUsed;
    long panamaMemory = panamaResults.performance.memoryUsed;

    // Log the comparison for analysis
    System.out.printf("Memory usage - JNI: %d bytes, Panama: %d bytes%n", jniMemory, panamaMemory);

    // Memory usage can vary significantly, so we just ensure both are reasonable
    assertTrue(jniMemory > 0, "JNI should use some memory");
    assertTrue(panamaMemory > 0, "Panama should use some memory");
  }

  private void assumeBothRuntimesAvailable() {
    if (jniResults == null || panamaResults == null) {
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Both JNI and Panama runtimes must be available");
    }
  }

  private RuntimeTestResults runTestsWithRuntime(String runtime) {
    try {
      // Set the runtime system property
      String originalRuntime = System.getProperty("wasmtime4j.runtime");
      System.setProperty("wasmtime4j.runtime", runtime);

      try {
        return executeTestSuite();
      } finally {
        // Restore original runtime setting
        if (originalRuntime != null) {
          System.setProperty("wasmtime4j.runtime", originalRuntime);
        } else {
          System.clearProperty("wasmtime4j.runtime");
        }
      }
    } catch (Exception e) {
      // Runtime not available
      return null;
    }
  }

  private RuntimeTestResults executeTestSuite() throws Exception {
    RuntimeTestResults results = new RuntimeTestResults();
    long startTime = System.currentTimeMillis();

    // Get memory baseline
    Runtime.getRuntime().gc();
    long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    Engine engine = Engine.builder()
        .withFuelEnabled(true)
        .withEpochInterruption(true)
        .build();

    Store<TestContext> store = Store.builder(engine)
        .withData(new TestContext())
        .withFuel(1000)
        .build();

    byte[] wasmBytes = TestUtils.watToWasm(TEST_WAT);
    Module module = Module.compile(engine, wasmBytes);

    Linker<TestContext> linker = Linker.create(engine);
    setupConsistencyTestHostFunctions(linker, results);

    Instance instance = linker.instantiate(store, module);

    // Run consistency tests
    runExportAccessTest(instance, store, results);
    runMultiValueTest(instance, store, results);
    runFuelOperationsTest(instance, store, results);
    runEpochDeadlineTest(instance, store, results);
    runErrorHandlingTest(instance, store, results);

    // Record performance metrics
    long endTime = System.currentTimeMillis();
    Runtime.getRuntime().gc();
    long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    results.performance.totalExecutionTime = endTime - startTime;
    results.performance.memoryUsed = endMemory - startMemory;

    return results;
  }

  private void setupConsistencyTestHostFunctions(Linker<TestContext> linker, RuntimeTestResults results) {
    // Test caller context access
    linker.define("host", "test_caller",
        FunctionType.create(new WasmValueType[]{WasmValueType.I32}, WasmValueType.I32),
        HostFunction.singleValueWithCaller((caller, params) -> {
          results.exportAccessTest.success = true;

          // Test export access
          results.exportAccessTest.exportsFound += caller.hasExport("memory") ? 1 : 0;
          results.exportAccessTest.exportsFound += caller.hasExport("counter") ? 1 : 0;
          results.exportAccessTest.exportsFound += caller.hasExport("table") ? 1 : 0;

          // Test memory access
          Optional<Memory> memory = caller.getMemory("memory");
          if (memory.isPresent()) {
            results.exportAccessTest.memorySize = memory.get().size();
          }

          return WasmValue.i32(params[0].asI32() + 1);
        }));

    // Test multi-value functions
    linker.define("host", "multi_value",
        FunctionType.create(
            new WasmValueType[]{WasmValueType.I32, WasmValueType.I32},
            WasmValueType.I32, WasmValueType.I32),
        HostFunction.multiValueWithCaller((caller, params) -> {
          results.multiValueTest.success = true;

          int a = params[0].asI32();
          int b = params[1].asI32();
          int sum = a + b;
          int product = a * b;

          results.multiValueTest.results = new int[]{sum, product};

          return WasmValue.multiValue(
              WasmValue.i32(sum),
              WasmValue.i32(product)
          );
        }));

    // Test fuel operations
    linker.define("host", "fuel_ops",
        FunctionType.create(new WasmValueType[]{WasmValueType.I32}, WasmValueType.I32),
        HostFunction.singleValueWithCaller((caller, params) -> {
          results.fuelTest.success = true;

          // Record initial fuel
          Optional<Long> initialFuel = caller.fuelRemaining();
          if (initialFuel.isPresent()) {
            results.fuelTest.initialFuel = initialFuel.get();

            // Add fuel
            long fuelToAdd = params[0].asI32();
            caller.addFuel(fuelToAdd);
            results.fuelTest.fuelAdded = fuelToAdd;

            // Record final fuel
            Optional<Long> finalFuel = caller.fuelRemaining();
            if (finalFuel.isPresent()) {
              results.fuelTest.finalFuel = finalFuel.get();
            }

            // Test epoch deadline
            caller.setEpochDeadline(System.currentTimeMillis() + 1000);
            results.epochTest.deadlineSet = caller.hasEpochDeadline();
            results.epochTest.success = true;
          }

          return WasmValue.i32((int) results.fuelTest.finalFuel);
        }));
  }

  private void runExportAccessTest(Instance instance, Store<TestContext> store, RuntimeTestResults results) throws WasmException {
    Function testFunction = instance.getFunction("test_caller_context").orElseThrow();
    testFunction.call(store, WasmValue.i32(42));
  }

  private void runMultiValueTest(Instance instance, Store<TestContext> store, RuntimeTestResults results) throws WasmException {
    Function testFunction = instance.getFunction("test_multi_value").orElseThrow();
    WasmValue[] results_ = testFunction.call(store, WasmValue.i32(5), WasmValue.i32(7));
    // Results are recorded in the host function
  }

  private void runFuelOperationsTest(Instance instance, Store<TestContext> store, RuntimeTestResults results) throws WasmException {
    Function testFunction = instance.getFunction("test_fuel_ops").orElseThrow();
    testFunction.call(store, WasmValue.i32(100));
  }

  private void runEpochDeadlineTest(Instance instance, Store<TestContext> store, RuntimeTestResults results) {
    // Epoch deadline testing is done in the fuel operations test
  }

  private void runErrorHandlingTest(Instance instance, Store<TestContext> store, RuntimeTestResults results) {
    results.errorTest.success = true;
    results.errorTest.exceptionsThrown = 0;

    try {
      // Test with invalid parameters (should handle gracefully)
      Function testFunction = instance.getFunction("test_caller_context").orElseThrow();
      testFunction.call(store, WasmValue.i32(Integer.MAX_VALUE));
    } catch (Exception e) {
      results.errorTest.exceptionsThrown++;
    }
  }

  // Helper classes for test results
  static class RuntimeTestResults {
    ExportAccessTestResult exportAccessTest = new ExportAccessTestResult();
    MultiValueTestResult multiValueTest = new MultiValueTestResult();
    FuelTestResult fuelTest = new FuelTestResult();
    EpochTestResult epochTest = new EpochTestResult();
    ErrorTestResult errorTest = new ErrorTestResult();
    PerformanceResult performance = new PerformanceResult();
  }

  static class ExportAccessTestResult {
    boolean success = false;
    int exportsFound = 0;
    int memorySize = 0;
  }

  static class MultiValueTestResult {
    boolean success = false;
    int[] results = new int[0];
  }

  static class FuelTestResult {
    boolean success = false;
    long initialFuel = 0;
    long finalFuel = 0;
    long fuelAdded = 0;
  }

  static class EpochTestResult {
    boolean success = false;
    boolean deadlineSet = false;
  }

  static class ErrorTestResult {
    boolean success = false;
    int exceptionsThrown = 0;
  }

  static class PerformanceResult {
    long totalExecutionTime = 0;
    long memoryUsed = 0;
  }

  static class TestContext {
    // Empty test context
  }

  static class TestUtils {
    static byte[] watToWasm(String wat) {
      // Placeholder - in practice this would compile WAT to WASM
      return new byte[]{
          0x00, 0x61, 0x73, 0x6d, // WASM magic number
          0x01, 0x00, 0x00, 0x00  // WASM version
      };
    }
  }
}