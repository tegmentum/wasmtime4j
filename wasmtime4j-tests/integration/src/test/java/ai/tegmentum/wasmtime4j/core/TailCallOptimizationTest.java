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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WebAssembly tail call optimization.
 *
 * <p>This test suite validates tail call optimization including:
 *
 * <ul>
 *   <li>return_call instruction
 *   <li>return_call_indirect instruction
 *   <li>Deep recursion without stack overflow
 *   <li>Mutual tail recursion
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("Tail Call Optimization Integration Tests")
public final class TailCallOptimizationTest {

  private static final Logger LOGGER = Logger.getLogger(TailCallOptimizationTest.class.getName());

  private static boolean tailCallSupported = false;
  private static WasmRuntime sharedRuntime;
  private static Engine sharedEngine;

  // WAT module with tail call optimization for factorial
  private static final String TAIL_CALL_FACTORIAL_WAT =
      "(module\n"
          + "  ;; Tail-recursive factorial helper\n"
          + "  (func $fact_helper (param $n i64) (param $acc i64) (result i64)\n"
          + "    (if (result i64) (i64.le_s (local.get $n) (i64.const 1))\n"
          + "      (then (local.get $acc))\n"
          + "      (else\n"
          + "        (return_call $fact_helper\n"
          + "          (i64.sub (local.get $n) (i64.const 1))\n"
          + "          (i64.mul (local.get $acc) (local.get $n))\n"
          + "        )\n"
          + "      )\n"
          + "    )\n"
          + "  )\n"
          + "  \n"
          + "  ;; Public factorial function\n"
          + "  (func (export \"factorial\") (param $n i64) (result i64)\n"
          + "    (call $fact_helper (local.get $n) (i64.const 1))\n"
          + "  )\n"
          + ")";

  // WAT module with deep tail recursion for countdown
  private static final String DEEP_TAIL_RECURSION_WAT =
      "(module\n"
          + "  ;; Countdown using tail recursion - should handle very large values\n"
          + "  (func $countdown (param $n i64) (result i64)\n"
          + "    (if (result i64) (i64.le_s (local.get $n) (i64.const 0))\n"
          + "      (then (i64.const 0))\n"
          + "      (else\n"
          + "        (return_call $countdown\n"
          + "          (i64.sub (local.get $n) (i64.const 1))\n"
          + "        )\n"
          + "      )\n"
          + "    )\n"
          + "  )\n"
          + "  (export \"countdown\" (func $countdown))\n"
          + ")";

  // WAT module with mutual tail recursion (even/odd check)
  private static final String MUTUAL_TAIL_RECURSION_WAT =
      "(module\n"
          + "  ;; Mutually recursive even/odd predicates using tail calls\n"
          + "  (func $is_even (param $n i64) (result i32)\n"
          + "    (if (result i32) (i64.eq (local.get $n) (i64.const 0))\n"
          + "      (then (i32.const 1))  ;; 0 is even\n"
          + "      (else\n"
          + "        (return_call $is_odd (i64.sub (local.get $n) (i64.const 1)))\n"
          + "      )\n"
          + "    )\n"
          + "  )\n"
          + "  \n"
          + "  (func $is_odd (param $n i64) (result i32)\n"
          + "    (if (result i32) (i64.eq (local.get $n) (i64.const 0))\n"
          + "      (then (i32.const 0))  ;; 0 is not odd\n"
          + "      (else\n"
          + "        (return_call $is_even (i64.sub (local.get $n) (i64.const 1)))\n"
          + "      )\n"
          + "    )\n"
          + "  )\n"
          + "  \n"
          + "  (export \"is_even\" (func $is_even))\n"
          + "  (export \"is_odd\" (func $is_odd))\n"
          + ")";

  // WAT module with return_call_indirect
  private static final String RETURN_CALL_INDIRECT_WAT =
      "(module\n"
          + "  (type $unary_i64 (func (param i64) (result i64)))\n"
          + "  \n"
          + "  (table 2 funcref)\n"
          + "  (elem (i32.const 0) $double $triple)\n"
          + "  \n"
          + "  (func $double (param $n i64) (result i64)\n"
          + "    (i64.mul (local.get $n) (i64.const 2))\n"
          + "  )\n"
          + "  \n"
          + "  (func $triple (param $n i64) (result i64)\n"
          + "    (i64.mul (local.get $n) (i64.const 3))\n"
          + "  )\n"
          + "  \n"
          + "  ;; Apply function at table index using return_call_indirect\n"
          + "  (func (export \"apply\") (param $idx i32) (param $n i64) (result i64)\n"
          + "    (return_call_indirect (type $unary_i64)\n"
          + "      (local.get $n)\n"
          + "      (local.get $idx)\n"
          + "    )\n"
          + "  )\n"
          + ")";

  @BeforeAll
  static void checkTailCallSupport() {
    LOGGER.info("Checking tail call support...");
    try {
      sharedRuntime = WasmRuntimeFactory.create();

      // Create engine with tail call enabled
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.TAIL_CALL);
      sharedEngine = sharedRuntime.createEngine(config);

      // Try to compile a module with tail calls
      final Module testModule = sharedEngine.compileWat(TAIL_CALL_FACTORIAL_WAT);
      if (testModule != null) {
        tailCallSupported = true;
        testModule.close();
        LOGGER.info("Tail call optimization is supported");
      }
    } catch (final Exception e) {
      LOGGER.warning("Tail call not supported: " + e.getMessage());
      tailCallSupported = false;
    }
  }

  @AfterAll
  static void cleanup() {
    if (sharedEngine != null) {
      try {
        sharedEngine.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared engine: " + e.getMessage());
      }
    }
    if (sharedRuntime != null) {
      try {
        sharedRuntime.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared runtime: " + e.getMessage());
      }
    }
  }

  private static void assumeTailCallSupported() {
    assumeTrue(tailCallSupported, "Tail call optimization not supported - skipping");
  }

  private Store store;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (tailCallSupported && sharedRuntime != null && sharedEngine != null) {
      store = sharedRuntime.createStore(sharedEngine);
      resources.add(store);
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    store = null;
  }

  @Nested
  @DisplayName("Return Call Tests")
  class ReturnCallTests {

    @Test
    @DisplayName("should compile module with return_call")
    void shouldCompileModuleWithReturnCall(final TestInfo testInfo) throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(TAIL_CALL_FACTORIAL_WAT);
      resources.add(module);

      assertNotNull(module, "Module with return_call should compile");
      LOGGER.info("Module with return_call compiled successfully");
    }

    @Test
    @DisplayName("should compute factorial correctly with tail recursion")
    void shouldComputeFactorialCorrectlyWithTailRecursion(final TestInfo testInfo)
        throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(TAIL_CALL_FACTORIAL_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction factorial = instance.getFunction("factorial").orElseThrow();

      // Test small factorials
      assertEquals(1L, factorial.call(WasmValue.i64(0))[0].asLong(), "0! = 1");
      assertEquals(1L, factorial.call(WasmValue.i64(1))[0].asLong(), "1! = 1");
      assertEquals(2L, factorial.call(WasmValue.i64(2))[0].asLong(), "2! = 2");
      assertEquals(6L, factorial.call(WasmValue.i64(3))[0].asLong(), "3! = 6");
      assertEquals(24L, factorial.call(WasmValue.i64(4))[0].asLong(), "4! = 24");
      assertEquals(120L, factorial.call(WasmValue.i64(5))[0].asLong(), "5! = 120");
      assertEquals(3628800L, factorial.call(WasmValue.i64(10))[0].asLong(), "10! = 3628800");

      // Larger factorial to stress tail recursion
      final long fact20 = factorial.call(WasmValue.i64(20))[0].asLong();
      assertEquals(2432902008176640000L, fact20, "20! = 2432902008176640000");

      LOGGER.info("Factorial computation with tail recursion verified");
    }
  }

  @Nested
  @DisplayName("Deep Recursion Tests")
  class DeepRecursionTests {

    @Test
    @DisplayName("should handle deep recursion without stack overflow")
    void shouldHandleDeepRecursionWithoutStackOverflow(final TestInfo testInfo) throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(DEEP_TAIL_RECURSION_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction countdown = instance.getFunction("countdown").orElseThrow();

      // Test with moderate depth
      assertEquals(0L, countdown.call(WasmValue.i64(1000))[0].asLong());

      // Test with large depth - this would overflow without tail call optimization
      assertTimeoutPreemptively(
          Duration.ofSeconds(10),
          () -> {
            final long result = countdown.call(WasmValue.i64(100000))[0].asLong();
            assertEquals(0L, result, "Deep countdown should return 0");
          },
          "Deep recursion should complete without timeout");

      LOGGER.info("Deep tail recursion handled without stack overflow");
    }

    @Test
    @DisplayName("should handle very deep recursion")
    void shouldHandleVeryDeepRecursion(final TestInfo testInfo) throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(DEEP_TAIL_RECURSION_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction countdown = instance.getFunction("countdown").orElseThrow();

      // Very deep recursion - 1 million iterations
      assertTimeoutPreemptively(
          Duration.ofSeconds(30),
          () -> {
            final long result = countdown.call(WasmValue.i64(1_000_000))[0].asLong();
            assertEquals(0L, result, "Very deep countdown should return 0");
          },
          "Very deep recursion should complete");

      LOGGER.info("Very deep tail recursion verified");
    }
  }

  @Nested
  @DisplayName("Mutual Tail Recursion Tests")
  class MutualTailRecursionTests {

    @Test
    @DisplayName("should compile module with mutual tail recursion")
    void shouldCompileModuleWithMutualTailRecursion(final TestInfo testInfo) throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(MUTUAL_TAIL_RECURSION_WAT);
      resources.add(module);

      assertNotNull(module, "Module with mutual tail recursion should compile");
      LOGGER.info("Module with mutual tail recursion compiled successfully");
    }

    @Test
    @DisplayName("should determine even/odd correctly with mutual tail recursion")
    void shouldDetermineEvenOddCorrectlyWithMutualTailRecursion(final TestInfo testInfo)
        throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(MUTUAL_TAIL_RECURSION_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction isEven = instance.getFunction("is_even").orElseThrow();
      final WasmFunction isOdd = instance.getFunction("is_odd").orElseThrow();

      // Test even numbers
      assertEquals(1, isEven.call(WasmValue.i64(0))[0].asInt(), "0 is even");
      assertEquals(1, isEven.call(WasmValue.i64(2))[0].asInt(), "2 is even");
      assertEquals(1, isEven.call(WasmValue.i64(100))[0].asInt(), "100 is even");

      // Test odd numbers
      assertEquals(1, isOdd.call(WasmValue.i64(1))[0].asInt(), "1 is odd");
      assertEquals(1, isOdd.call(WasmValue.i64(3))[0].asInt(), "3 is odd");
      assertEquals(1, isOdd.call(WasmValue.i64(99))[0].asInt(), "99 is odd");

      // Test false cases
      assertEquals(0, isEven.call(WasmValue.i64(1))[0].asInt(), "1 is not even");
      assertEquals(0, isOdd.call(WasmValue.i64(2))[0].asInt(), "2 is not odd");

      LOGGER.info("Even/odd with mutual tail recursion verified");
    }

    @Test
    @DisplayName("should handle deep mutual tail recursion")
    void shouldHandleDeepMutualTailRecursion(final TestInfo testInfo) throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(MUTUAL_TAIL_RECURSION_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction isEven = instance.getFunction("is_even").orElseThrow();

      // Large number would cause stack overflow without tail call optimization
      assertTimeoutPreemptively(
          Duration.ofSeconds(10),
          () -> {
            final int result = isEven.call(WasmValue.i64(100000))[0].asInt();
            assertEquals(1, result, "100000 is even");
          },
          "Deep mutual recursion should complete");

      LOGGER.info("Deep mutual tail recursion verified");
    }
  }

  @Nested
  @DisplayName("Return Call Indirect Tests")
  class ReturnCallIndirectTests {

    @Test
    @DisplayName("should compile module with return_call_indirect")
    void shouldCompileModuleWithReturnCallIndirect(final TestInfo testInfo) throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(RETURN_CALL_INDIRECT_WAT);
      resources.add(module);

      assertNotNull(module, "Module with return_call_indirect should compile");
      LOGGER.info("Module with return_call_indirect compiled successfully");
    }

    @Test
    @DisplayName("should call function indirectly via table using return_call_indirect")
    void shouldCallFunctionIndirectlyViaTable(final TestInfo testInfo) throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Module module = sharedEngine.compileWat(RETURN_CALL_INDIRECT_WAT);
      resources.add(module);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final WasmFunction apply = instance.getFunction("apply").orElseThrow();

      // Call double (index 0)
      final long doubled = apply.call(WasmValue.i32(0), WasmValue.i64(5))[0].asLong();
      assertEquals(10L, doubled, "double(5) = 10");

      // Call triple (index 1)
      final long tripled = apply.call(WasmValue.i32(1), WasmValue.i64(5))[0].asLong();
      assertEquals(15L, tripled, "triple(5) = 15");

      LOGGER.info("return_call_indirect via table verified");
    }
  }

  @Nested
  @DisplayName("Configuration Tests")
  class ConfigurationTests {

    @Test
    @DisplayName("should enable tail call via engine config")
    void shouldEnableTailCallViaEngineConfig(final TestInfo testInfo) throws Exception {
      assumeTailCallSupported();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.TAIL_CALL);

      try (Engine engine = sharedRuntime.createEngine(config)) {
        assertNotNull(engine, "Engine with tail call should be created");

        // Verify module with tail calls can be compiled
        final Module module = engine.compileWat(TAIL_CALL_FACTORIAL_WAT);
        assertNotNull(module, "Module with tail calls should compile");
        module.close();
      }

      LOGGER.info("Tail call engine configuration verified");
    }
  }
}
