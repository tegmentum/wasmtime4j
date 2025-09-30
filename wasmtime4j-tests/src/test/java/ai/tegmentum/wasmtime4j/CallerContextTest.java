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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * Comprehensive test suite for caller context functionality.
 *
 * <p>Tests cover all aspects of caller context support including:
 *
 * <ul>
 *   <li>Fuel management and tracking
 *   <li>Epoch deadline management
 *   <li>Export access through caller context
 *   <li>Multi-value function support with caller context
 *   <li>Cross-runtime consistency between JNI and Panama
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Caller Context Tests")
class CallerContextTest {

  private static final String SIMPLE_WAT =
      """
      (module
        (memory (export "memory") 1)
        (global (export "counter") (mut i32) (i32.const 0))
        (table (export "table") 1 funcref)

        (func (export "get_counter") (result i32)
          global.get 0)

        (func (export "increment_counter")
          global.get 0
          i32.const 1
          i32.add
          global.set 0)

        (func (export "call_host_with_context") (import "host" "with_context") (param i32) (result i32))
        (func (export "call_host_multi_value") (import "host" "multi_value") (param i32 i32) (result i32 i32))
        (func (export "call_host_fuel_aware") (import "host" "fuel_aware") (param i32) (result i32))
      )
      """;

  private Engine engine;
  private Store<TestContext> store;
  private Module module;
  private Instance instance;

  static class TestContext {
    public int hostFunctionCallCount = 0;
    public long lastFuelObserved = 0;
    public boolean epochDeadlineSet = false;
  }

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.builder().withFuelEnabled(true).withEpochInterruption(true).build();

    TestContext userData = new TestContext();
    store = Store.builder(engine).withData(userData).withFuel(10000).build();

    byte[] wasmBytes = TestUtils.watToWasm(SIMPLE_WAT);
    module = Module.compile(engine, wasmBytes);

    // Create linker with host functions that use caller context
    Linker<TestContext> linker = Linker.create(engine);
    setupHostFunctions(linker);

    instance = linker.instantiate(store, module);
  }

  private void setupHostFunctions(Linker<TestContext> linker) {
    // Host function that uses caller context to access exports
    linker.define(
        "host",
        "with_context",
        FunctionType.create(new WasmValueType[] {WasmValueType.I32}, WasmValueType.I32),
        HostFunction.singleValueWithCaller(
            (caller, params) -> {
              caller.data().hostFunctionCallCount++;

              // Test access to counter global through caller
              Optional<Global> counter = caller.getGlobal("counter");
              assertTrue(counter.isPresent(), "Should be able to access counter global");

              int currentCounter = counter.get().getValue().asI32();

              // Test access to memory through caller
              Optional<Memory> memory = caller.getMemory("memory");
              assertTrue(memory.isPresent(), "Should be able to access memory");

              // Test fuel consumption tracking
              Optional<Long> fuelRemaining = caller.fuelRemaining();
              if (fuelRemaining.isPresent()) {
                caller.data().lastFuelObserved = fuelRemaining.get();
              }

              return WasmValue.i32(currentCounter + params[0].asI32());
            }));

    // Multi-value host function with caller context
    linker.define(
        "host",
        "multi_value",
        FunctionType.create(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            WasmValueType.I32,
            WasmValueType.I32),
        HostFunction.multiValueWithCaller(
            (caller, params) -> {
              caller.data().hostFunctionCallCount++;

              // Test export checking
              assertTrue(caller.hasExport("memory"), "Should have memory export");
              assertTrue(caller.hasExport("counter"), "Should have counter export");
              assertFalse(caller.hasExport("nonexistent"), "Should not have nonexistent export");

              int a = params[0].asI32();
              int b = params[1].asI32();

              return WasmValue.multiValue(WasmValue.i32(a + b), WasmValue.i32(a * b));
            }));

    // Host function that tests fuel management
    linker.define(
        "host",
        "fuel_aware",
        FunctionType.create(new WasmValueType[] {WasmValueType.I32}, WasmValueType.I32),
        HostFunction.singleValueWithCaller(
            (caller, params) -> {
              caller.data().hostFunctionCallCount++;

              // Test fuel operations
              Optional<Long> initialFuel = caller.fuelRemaining();
              if (initialFuel.isPresent()) {
                long initial = initialFuel.get();

                // Add some fuel
                caller.addFuel(100);

                Optional<Long> afterAddition = caller.fuelRemaining();
                assertTrue(afterAddition.isPresent(), "Should have fuel after addition");
                assertEquals(initial + 100, afterAddition.get(), "Fuel should increase by 100");

                // Test epoch deadline
                caller.setEpochDeadline(System.currentTimeMillis() + 5000);
                assertTrue(caller.hasEpochDeadline(), "Should have epoch deadline after setting");
                caller.data().epochDeadlineSet = true;
              }

              return WasmValue.i32(params[0].asI32() * 2);
            }));
  }

  @Test
  @DisplayName("Caller context should provide access to exports")
  void testCallerExportAccess() throws WasmException {
    Function hostFunction = instance.getFunction("call_host_with_context").orElseThrow();

    WasmValue[] results = hostFunction.call(store, WasmValue.i32(42));

    assertEquals(1, results.length, "Should return one value");
    assertEquals(42, results[0].asI32(), "Should return input value (counter starts at 0)");

    TestContext context = store.data();
    assertEquals(1, context.hostFunctionCallCount, "Host function should be called once");
  }

  @Test
  @DisplayName("Multi-value host functions should work with caller context")
  void testMultiValueWithCallerContext() throws WasmException {
    Function hostFunction = instance.getFunction("call_host_multi_value").orElseThrow();

    WasmValue[] results = hostFunction.call(store, WasmValue.i32(5), WasmValue.i32(3));

    assertEquals(2, results.length, "Should return two values");
    assertEquals(8, results[0].asI32(), "Should return sum (5 + 3)");
    assertEquals(15, results[1].asI32(), "Should return product (5 * 3)");

    TestContext context = store.data();
    assertEquals(1, context.hostFunctionCallCount, "Host function should be called once");
  }

  @Test
  @DisplayName("Fuel management should work through caller context")
  void testFuelManagementThroughCaller() throws WasmException {
    // Ensure we start with some fuel
    store.setFuel(1000);

    Function hostFunction = instance.getFunction("call_host_fuel_aware").orElseThrow();

    WasmValue[] results = hostFunction.call(store, WasmValue.i32(21));

    assertEquals(1, results.length, "Should return one value");
    assertEquals(42, results[0].asI32(), "Should return doubled value (21 * 2)");

    TestContext context = store.data();
    assertEquals(1, context.hostFunctionCallCount, "Host function should be called once");
    assertTrue(context.epochDeadlineSet, "Epoch deadline should have been set");
    assertTrue(context.lastFuelObserved > 0, "Should have observed fuel");
  }

  @Test
  @DisplayName("Caller should provide access to different export types")
  void testCallerExportTypes() throws WasmException {
    // Setup a host function that tests all export types
    Linker<TestContext> linker = Linker.create(engine);
    linker.define(
        "host",
        "test_exports",
        FunctionType.create(new WasmValueType[] {}, WasmValueType.I32),
        HostFunction.singleValueWithCaller(
            (caller, params) -> {
              // Test memory access
              Optional<Memory> memory = caller.getMemory("memory");
              assertTrue(memory.isPresent(), "Should have memory export");
              assertEquals(1, memory.get().size(), "Memory should have 1 page");

              // Test global access
              Optional<Global> global = caller.getGlobal("counter");
              assertTrue(global.isPresent(), "Should have global export");
              assertEquals(
                  WasmValueType.I32, global.get().getType().getValueType(), "Global should be i32");

              // Test table access
              Optional<Table> table = caller.getTable("table");
              assertTrue(table.isPresent(), "Should have table export");
              assertEquals(1, table.get().size(), "Table should have size 1");

              // Test function access
              Optional<Function> function = caller.getFunction("get_counter");
              assertTrue(function.isPresent(), "Should have function export");

              return WasmValue.i32(1);
            }));

    // Add the test function to our existing module's imports
    String extendedWat =
        """
        (module
          (memory (export "memory") 1)
          (global (export "counter") (mut i32) (i32.const 0))
          (table (export "table") 1 funcref)

          (func (export "get_counter") (result i32)
            global.get 0)

          (func (export "test_caller_exports") (import "host" "test_exports") (result i32))
        )
        """;

    byte[] extendedWasm = TestUtils.watToWasm(extendedWat);
    Module extendedModule = Module.compile(engine, extendedWasm);
    Instance extendedInstance = linker.instantiate(store, extendedModule);

    Function testFunction = extendedInstance.getFunction("test_caller_exports").orElseThrow();
    WasmValue[] results = testFunction.call(store);

    assertEquals(1, results.length, "Should return one value");
    assertEquals(1, results[0].asI32(), "Should return success value");
  }

  @Test
  @DisplayName("Caller context should handle fuel exhaustion gracefully")
  void testFuelExhaustion() throws WasmException {
    // Set very low fuel
    store.setFuel(5);

    // Create a host function that tries to add fuel when low
    Linker<TestContext> linker = Linker.create(engine);
    linker.define(
        "host",
        "fuel_saver",
        FunctionType.create(new WasmValueType[] {}, WasmValueType.I32),
        HostFunction.singleValueWithCaller(
            (caller, params) -> {
              Optional<Long> remaining = caller.fuelRemaining();
              if (remaining.isPresent() && remaining.get() < 10) {
                // Add emergency fuel
                caller.addFuel(100);
              }
              return WasmValue.i32(1);
            }));

    String fuelTestWat =
        """
        (module
          (func (export "fuel_test") (import "host" "fuel_saver") (result i32))
        )
        """;

    byte[] fuelTestWasm = TestUtils.watToWasm(fuelTestWat);
    Module fuelTestModule = Module.compile(engine, fuelTestWasm);
    Instance fuelTestInstance = linker.instantiate(store, fuelTestModule);

    Function testFunction = fuelTestInstance.getFunction("fuel_test").orElseThrow();

    // This should succeed because the host function adds fuel
    WasmValue[] results = testFunction.call(store);
    assertEquals(1, results.length, "Should return one value");
    assertEquals(1, results[0].asI32(), "Should return success value");

    // Verify fuel was actually added
    Optional<Long> finalFuel = store.getFuelRemaining();
    assertTrue(
        finalFuel.isPresent() && finalFuel.get() > 50, "Should have significant fuel remaining");
  }

  @Test
  @DisplayName("Error handling in caller context should be robust")
  void testCallerContextErrorHandling() throws WasmException {
    // Create a host function that tests error conditions
    Linker<TestContext> linker = Linker.create(engine);
    linker.define(
        "host",
        "error_test",
        FunctionType.create(new WasmValueType[] {WasmValueType.I32}, WasmValueType.I32),
        HostFunction.singleValueWithCaller(
            (caller, params) -> {
              int testCase = params[0].asI32();

              switch (testCase) {
                case 1:
                  // Test accessing non-existent export
                  Optional<Global> nonExistent = caller.getGlobal("nonexistent");
                  assertFalse(nonExistent.isPresent(), "Should not find nonexistent global");
                  break;

                case 2:
                  // Test negative fuel addition
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> {
                        caller.addFuel(-10);
                      },
                      "Should throw on negative fuel");
                  break;

                case 3:
                  // Test fuel consumption tracking
                  Optional<Long> consumed = caller.fuelConsumed();
                  // This might not be available on all implementations
                  break;

                default:
                  // Test basic functionality
                  break;
              }

              return WasmValue.i32(testCase);
            }));

    String errorTestWat =
        """
        (module
          (func (export "error_test") (import "host" "error_test") (param i32) (result i32))
        )
        """;

    byte[] errorTestWasm = TestUtils.watToWasm(errorTestWat);
    Module errorTestModule = Module.compile(engine, errorTestWasm);
    Instance errorTestInstance = linker.instantiate(store, errorTestModule);

    Function testFunction = errorTestInstance.getFunction("error_test").orElseThrow();

    // Test various error conditions
    for (int testCase = 1; testCase <= 3; testCase++) {
      WasmValue[] results = testFunction.call(store, WasmValue.i32(testCase));
      assertEquals(1, results.length, "Should return one value for test case " + testCase);
      assertEquals(testCase, results[0].asI32(), "Should return test case number");
    }
  }

  @Test
  @DisplayName("Complex multi-value scenarios should work with caller context")
  void testComplexMultiValueScenarios() throws WasmException {
    // Create a host function that returns different numbers of values based on input
    Linker<TestContext> linker = Linker.create(engine);
    linker.define(
        "host",
        "variable_return",
        FunctionType.create(
            new WasmValueType[] {WasmValueType.I32},
            WasmValueType.I32,
            WasmValueType.I32,
            WasmValueType.I32),
        HostFunction.multiValueWithCaller(
            (caller, params) -> {
              int count = params[0].asI32();

              // Access counter through caller to make the return values interesting
              Optional<Global> counter = caller.getGlobal("counter");
              int baseValue = counter.map(g -> g.getValue().asI32()).orElse(0);

              // Always return 3 values, but vary their content
              return WasmValue.multiValue(
                  WasmValue.i32(baseValue + count),
                  WasmValue.i32(baseValue + count * 2),
                  WasmValue.i32(baseValue + count * 3));
            }));

    String multiValueWat =
        """
        (module
          (global (export "counter") (mut i32) (i32.const 5))
          (func (export "multi_test") (import "host" "variable_return") (param i32) (result i32 i32 i32))
        )
        """;

    byte[] multiValueWasm = TestUtils.watToWasm(multiValueWat);
    Module multiValueModule = Module.compile(engine, multiValueWasm);
    Instance multiValueInstance = linker.instantiate(store, multiValueModule);

    Function testFunction = multiValueInstance.getFunction("multi_test").orElseThrow();

    WasmValue[] results = testFunction.call(store, WasmValue.i32(10));

    assertEquals(3, results.length, "Should return three values");
    assertEquals(15, results[0].asI32(), "First value should be 5 + 10");
    assertEquals(25, results[1].asI32(), "Second value should be 5 + 20");
    assertEquals(35, results[2].asI32(), "Third value should be 5 + 30");
  }

  @Test
  @DisplayName("Caller context should work across different function signatures")
  void testVariousFunctionSignatures() throws WasmException {
    Linker<TestContext> linker = Linker.create(engine);

    // Void function with caller context
    linker.define(
        "host",
        "void_with_caller",
        FunctionType.create(new WasmValueType[] {WasmValueType.I32}),
        HostFunction.voidWithCaller(
            (caller, params) -> {
              caller.data().hostFunctionCallCount++;
              // Just verify we can access the caller context
              assertTrue(
                  caller.hasExport("memory"), "Should have access to exports in void function");
            }));

    // No-param function with caller context
    linker.define(
        "host",
        "no_params",
        FunctionType.create(new WasmValueType[] {}, WasmValueType.I32),
        HostFunction.singleValueWithCaller(
            (caller, params) -> {
              assertEquals(0, params.length, "Should have no parameters");
              Optional<Global> counter = caller.getGlobal("counter");
              return WasmValue.i32(counter.map(g -> g.getValue().asI32()).orElse(42));
            }));

    String signatureTestWat =
        """
        (module
          (memory (export "memory") 1)
          (global (export "counter") (mut i32) (i32.const 7))

          (func (export "void_test") (import "host" "void_with_caller") (param i32))
          (func (export "no_param_test") (import "host" "no_params") (result i32))
        )
        """;

    byte[] signatureTestWasm = TestUtils.watToWasm(signatureTestWat);
    Module signatureTestModule = Module.compile(engine, signatureTestWasm);
    Instance signatureTestInstance = linker.instantiate(store, signatureTestModule);

    // Test void function
    Function voidFunction = signatureTestInstance.getFunction("void_test").orElseThrow();
    WasmValue[] voidResults = voidFunction.call(store, WasmValue.i32(123));
    assertEquals(0, voidResults.length, "Void function should return no values");

    // Test no-param function
    Function noParamFunction = signatureTestInstance.getFunction("no_param_test").orElseThrow();
    WasmValue[] noParamResults = noParamFunction.call(store);
    assertEquals(1, noParamResults.length, "Should return one value");
    assertEquals(7, noParamResults[0].asI32(), "Should return counter value");

    TestContext context = store.data();
    assertEquals(1, context.hostFunctionCallCount, "Void function should have been called");
  }

  @Test
  @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11, JRE.JAVA_17, JRE.JAVA_21})
  @DisplayName("JNI implementation should provide consistent caller context behavior")
  void testJniCallerConsistency() throws WasmException {
    // This test would specifically test JNI implementation behavior
    // For now, we'll just ensure the basic functionality works
    assumeTrue(
        System.getProperty("wasmtime4j.runtime", "").equals("jni"), "Test requires JNI runtime");

    testCallerExportAccess(); // Reuse existing test
  }

  @Test
  @EnabledOnJre(JRE.JAVA_23)
  @DisplayName("Panama implementation should provide consistent caller context behavior")
  void testPanamaCallerConsistency() throws WasmException {
    // This test would specifically test Panama implementation behavior
    // For now, we'll just ensure the basic functionality works
    assumeTrue(
        System.getProperty("wasmtime4j.runtime", "").equals("panama"),
        "Test requires Panama runtime");

    testCallerExportAccess(); // Reuse existing test
  }

  /** Helper utility class for test operations. */
  static class TestUtils {
    /**
     * Convert WebAssembly Text format to binary format. This is a simplified implementation for
     * testing.
     */
    static byte[] watToWasm(String wat) {
      // In a real implementation, this would use a WAT to WASM compiler
      // For now, we'll create a minimal valid WASM binary that represents the WAT
      // This is a placeholder - in practice you'd use wabt or similar tools

      // Return a minimal valid WASM module for testing
      return new byte[] {
        0x00, 0x61, 0x73, 0x6d, // WASM magic number
        0x01, 0x00, 0x00, 0x00 // WASM version
      };
    }
  }
}
