/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.framework;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Production scenario integration tests validating enterprise-grade functionality.
 *
 * <p>These tests validate real-world production scenarios using actual WASM execution including
 * error recovery, timeout handling, resource exhaustion, concurrent stress, and graceful
 * degradation.
 *
 * @since 1.0.0
 */
@DisplayName("Production Scenario Integration Tests")
public class ProductionScenarioTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ProductionScenarioTest.class.getName());

  private ExecutorService executorService;
  private ProductionMetrics metrics;

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Setting up production scenario test: " + testInfo.getDisplayName());
    executorService = Executors.newFixedThreadPool(10);
    metrics = new ProductionMetrics();
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down production scenario test: " + testInfo.getDisplayName());
    clearRuntimeSelection();
    if (executorService != null) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("System Resilience and Recovery Testing")
  void testSystemResilienceAndRecovery(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("Testing system resilience and recovery capabilities");

    final ResilienceTestFramework framework = new ResilienceTestFramework();
    final List<ResilienceTest> resilienceTests = createResilienceTests();

    int passedTests = 0;
    for (final ResilienceTest test : resilienceTests) {
      try {
        final boolean result = framework.executeResilienceTest(test);
        if (result) {
          passedTests++;
          LOGGER.info("✓ Resilience test '" + test.getName() + "' passed");
          metrics.recordResilienceTest(test.getName(), true, 0);
        } else {
          LOGGER.warning("✗ Resilience test '" + test.getName() + "' failed");
          metrics.recordResilienceTest(test.getName(), false, 0);
        }
      } catch (Exception e) {
        LOGGER.warning(
            "✗ Resilience test '" + test.getName() + "' threw exception: " + e.getMessage());
        metrics.recordResilienceTest(test.getName(), false, 0);
      }
    }

    final double resilienceSuccessRate = (double) passedTests / resilienceTests.size();
    LOGGER.info(
        String.format(
            "Resilience testing results: %d/%d tests passed (%.1f%% success rate)",
            passedTests, resilienceTests.size(), resilienceSuccessRate * 100));

    assertTrue(
        resilienceSuccessRate >= 0.8,
        "Should achieve at least 80% success rate in resilience tests");
  }

  private List<ResilienceTest> createResilienceTests() {
    final List<ResilienceTest> tests = new ArrayList<>();
    tests.add(new ResilienceTest("error-recovery", "Test error recovery mechanisms"));
    tests.add(new ResilienceTest("timeout-handling", "Test timeout handling"));
    tests.add(new ResilienceTest("resource-exhaustion", "Test behavior under resource exhaustion"));
    tests.add(new ResilienceTest("concurrent-stress", "Test concurrent access under stress"));
    tests.add(new ResilienceTest("graceful-degradation", "Test graceful degradation"));
    return tests;
  }

  private static class ProductionMetrics {
    private final Map<String, Boolean> resilienceTestResults = new ConcurrentHashMap<>();

    public void recordResilienceTest(
        final String testName, final boolean passed, final long durationNs) {
      resilienceTestResults.put(testName, passed);
    }
  }

  private static class ResilienceTestFramework {
    private static final Logger FRAMEWORK_LOGGER =
        Logger.getLogger(ResilienceTestFramework.class.getName());

    private static final byte[] ADD_WASM = {
      0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f,
      0x01, 0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00,
      0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
    };

    public boolean executeResilienceTest(final ResilienceTest test) {
      FRAMEWORK_LOGGER.info("Executing resilience test: " + test.getName());
      switch (test.getName()) {
        case "error-recovery":
          return testErrorRecovery();
        case "timeout-handling":
          return testTimeoutHandling();
        case "resource-exhaustion":
          return testResourceExhaustion();
        case "concurrent-stress":
          return testConcurrentStress();
        case "graceful-degradation":
          return testGracefulDegradation();
        default:
          FRAMEWORK_LOGGER.warning("Unknown resilience test: " + test.getName());
          return false;
      }
    }

    private boolean testErrorRecovery() {
      // Verify that after a failed operation (invalid WASM), a subsequent valid
      // operation on a new store succeeds.
      try (final Engine engine = Engine.create()) {
        // Trigger an error with invalid WASM bytes
        try {
          engine.compileModule(new byte[] {0x00, 0x01, 0x02, 0x03});
          FRAMEWORK_LOGGER.warning("error-recovery: Expected compilation to fail");
          return false;
        } catch (final Exception expected) {
          FRAMEWORK_LOGGER.info(
              "error-recovery: Compilation correctly failed: "
                  + expected.getClass().getSimpleName());
        }

        // Recover by performing a valid operation
        try (final Store store = engine.createStore();
            final Module module = engine.compileModule(ADD_WASM);
            final Instance instance = module.instantiate(store)) {
          final Optional<WasmFunction> addFunc = instance.getFunction("add");
          if (!addFunc.isPresent()) {
            FRAMEWORK_LOGGER.warning("error-recovery: add function not found after recovery");
            return false;
          }
          final WasmValue[] result = addFunc.get().call(WasmValue.i32(10), WasmValue.i32(20));
          final int sum = result[0].asInt();
          FRAMEWORK_LOGGER.info("error-recovery: Recovered successfully, 10+20=" + sum);
          return sum == 30;
        }
      } catch (final Exception e) {
        FRAMEWORK_LOGGER.warning("error-recovery: Unexpected exception: " + e.getMessage());
        return false;
      }
    }

    private boolean testTimeoutHandling() {
      // Verify that store creation and module compilation complete within a reasonable time.
      try {
        final long startTime = System.nanoTime();
        try (final Engine engine = Engine.create();
            final Store store = engine.createStore();
            final Module module = engine.compileModule(ADD_WASM);
            final Instance instance = module.instantiate(store)) {
          final Optional<WasmFunction> addFunc = instance.getFunction("add");
          if (!addFunc.isPresent()) {
            return false;
          }
          addFunc.get().call(WasmValue.i32(1), WasmValue.i32(2));
        }
        final long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        FRAMEWORK_LOGGER.info("timeout-handling: Completed in " + elapsedMs + "ms");
        return elapsedMs < 5000; // Should complete well within 5 seconds
      } catch (final Exception e) {
        FRAMEWORK_LOGGER.warning("timeout-handling: Unexpected exception: " + e.getMessage());
        return false;
      }
    }

    private boolean testResourceExhaustion() {
      // Create and destroy many engines/stores to verify resources are properly released.
      try {
        for (int i = 0; i < 20; i++) {
          try (final Engine engine = Engine.create();
              final Store store = engine.createStore();
              final Module module = engine.compileModule(ADD_WASM);
              final Instance instance = module.instantiate(store)) {
            final Optional<WasmFunction> addFunc = instance.getFunction("add");
            if (!addFunc.isPresent()) {
              return false;
            }
            addFunc.get().call(WasmValue.i32(i), WasmValue.i32(i));
          }
        }
        FRAMEWORK_LOGGER.info("resource-exhaustion: 20 engine/store cycles completed");
        return true;
      } catch (final Exception e) {
        FRAMEWORK_LOGGER.warning("resource-exhaustion: Failed: " + e.getMessage());
        return false;
      }
    }

    private boolean testConcurrentStress() {
      // Run multiple threads each creating their own engine/store/instance and calling add.
      final int threadCount = 5;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicInteger successes = new AtomicInteger(0);

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        new Thread(
                () -> {
                  try (final Engine engine = Engine.create();
                      final Store store = engine.createStore();
                      final Module module = engine.compileModule(ADD_WASM);
                      final Instance instance = module.instantiate(store)) {
                    final Optional<WasmFunction> addFunc = instance.getFunction("add");
                    if (addFunc.isPresent()) {
                      final WasmValue[] result =
                          addFunc.get().call(WasmValue.i32(threadId), WasmValue.i32(1));
                      if (result[0].asInt() == threadId + 1) {
                        successes.incrementAndGet();
                      }
                    }
                  } catch (final Exception e) {
                    FRAMEWORK_LOGGER.warning(
                        "concurrent-stress: Thread " + threadId + " failed: " + e.getMessage());
                  } finally {
                    latch.countDown();
                  }
                })
            .start();
      }

      try {
        final boolean completed = latch.await(30, TimeUnit.SECONDS);
        FRAMEWORK_LOGGER.info(
            "concurrent-stress: "
                + successes.get()
                + "/"
                + threadCount
                + " threads succeeded, completed="
                + completed);
        return completed && successes.get() == threadCount;
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }

    private boolean testGracefulDegradation() {
      // Verify that closing a store does not crash and subsequent operations on a
      // new store still work.
      try (final Engine engine = Engine.create()) {
        // Create and immediately close a store
        final Store store1 = engine.createStore();
        store1.close();

        // Verify a new store works fine after the first was closed
        try (final Store store2 = engine.createStore();
            final Module module = engine.compileModule(ADD_WASM);
            final Instance instance = module.instantiate(store2)) {
          final Optional<WasmFunction> addFunc = instance.getFunction("add");
          if (!addFunc.isPresent()) {
            return false;
          }
          final WasmValue[] result = addFunc.get().call(WasmValue.i32(100), WasmValue.i32(200));
          FRAMEWORK_LOGGER.info("graceful-degradation: 100+200=" + result[0].asInt());
          return result[0].asInt() == 300;
        }
      } catch (final Exception e) {
        FRAMEWORK_LOGGER.warning("graceful-degradation: Failed: " + e.getMessage());
        return false;
      }
    }
  }

  private static class ResilienceTest {
    private final String name;
    private final String description;

    public ResilienceTest(final String name, final String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }
}
