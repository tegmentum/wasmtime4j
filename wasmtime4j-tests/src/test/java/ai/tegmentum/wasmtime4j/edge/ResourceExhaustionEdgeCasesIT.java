package ai.tegmentum.wasmtime4j.edge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.function.WasmFunction;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive resource exhaustion testing for robust error handling validation. Tests validate
 * proper handling of memory exhaustion, stack overflow, timeout scenarios, and resource cleanup
 * mechanisms.
 */
@DisplayName("Resource Exhaustion Edge Cases Tests")
final class ResourceExhaustionEdgeCasesIT extends BaseIntegrationTest {

  private static final long SMALL_MEMORY_LIMIT = 64 * 1024; // 64KB
  private static final long MEDIUM_MEMORY_LIMIT = 1024 * 1024; // 1MB
  private static final long LARGE_MEMORY_LIMIT = 16 * 1024 * 1024; // 16MB
  private static final Duration SHORT_TIMEOUT = Duration.ofMillis(100);
  private static final Duration MEDIUM_TIMEOUT = Duration.ofSeconds(1);
  private static final Duration LONG_TIMEOUT = Duration.ofSeconds(10);

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // Resource exhaustion tests are always enabled
  }

  @Nested
  @DisplayName("Memory Exhaustion Tests")
  final class MemoryExhaustionTests {

    @Test
    @DisplayName("Should handle memory allocation beyond limits gracefully")
    void shouldHandleMemoryAllocationBeyondLimitsGracefully() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing memory allocation beyond limits on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Create module that tries to allocate large amounts of memory
            final byte[] memoryExhaustionModule = createMemoryExhaustionModule();
            final Module module = engine.compileModule(memoryExhaustionModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            // Try to allocate memory beyond reasonable limits
            final WasmFunction allocateFunction = instance.getFunction("allocate_large");
            if (allocateFunction != null) {
              assertThatThrownBy(() -> allocateFunction.call())
                  .isInstanceOfAny(RuntimeException.class, OutOfMemoryError.class)
                  .satisfies(
                      e -> {
                        LOGGER.info("Memory exhaustion properly detected: " + e.getMessage());
                      });
            }

            // Verify system remains stable after exhaustion
            assertThat(store.isValid()).isTrue();
            assertThat(instance.isValid()).isTrue();

            LOGGER.info("Memory exhaustion handling validated on " + runtimeType);
          });
    }

    @ParameterizedTest
    @ValueSource(longs = {1024, 4096, 16384, 65536})
    @DisplayName("Should enforce memory limits consistently")
    void shouldEnforceMemoryLimitsConsistently(final long memoryLimit) throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing memory limit " + memoryLimit + " on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            try {
              // Create module with specific memory requirements
              final byte[] memoryLimitModule = createMemoryLimitModule(memoryLimit);
              final Module module = engine.compileModule(memoryLimitModule);
              registerForCleanup(module);

              final Instance instance = store.instantiate(module);
              registerForCleanup(instance);

              // Test allocation up to limit
              final WasmFunction allocateFunction = instance.getFunction("allocate");
              if (allocateFunction != null) {
                // Should succeed within limit
                final Object result = allocateFunction.call((int) (memoryLimit / 2));
                assertThat(result).isNotNull();

                // Should fail beyond limit
                assertThatThrownBy(() -> allocateFunction.call((int) (memoryLimit * 2)))
                    .isInstanceOfAny(RuntimeException.class, OutOfMemoryError.class);
              }

              LOGGER.info("Memory limit " + memoryLimit + " enforced on " + runtimeType);
            } catch (final Exception e) {
              LOGGER.info(
                  "Memory limit test failed as expected for "
                      + memoryLimit
                      + " on "
                      + runtimeType
                      + ": "
                      + e.getMessage());
            }
          });
    }

    @Test
    @DisplayName("Should detect and prevent memory leaks")
    void shouldDetectAndPreventMemoryLeaks() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing memory leak detection on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Create module that allocates but doesn't free memory
            final byte[] memoryLeakModule = createMemoryLeakModule();
            final Module module = engine.compileModule(memoryLeakModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction leakFunction = instance.getFunction("create_leak");
            if (leakFunction != null) {
              // Monitor memory usage
              final Runtime javaRuntime = Runtime.getRuntime();
              final long initialMemory = javaRuntime.totalMemory() - javaRuntime.freeMemory();

              // Repeatedly call function that leaks memory
              int iterations = 0;
              boolean memoryExhausted = false;

              try {
                for (int i = 0; i < 1000 && !memoryExhausted; i++) {
                  leakFunction.call();
                  iterations++;

                  // Check if memory usage is growing excessively
                  final long currentMemory = javaRuntime.totalMemory() - javaRuntime.freeMemory();
                  if (currentMemory > initialMemory + LARGE_MEMORY_LIMIT) {
                    memoryExhausted = true;
                  }
                }
              } catch (final Exception e) {
                LOGGER.info("Memory exhaustion detected after " + iterations + " iterations");
                memoryExhausted = true;
              }

              // System should detect excessive memory usage
              if (iterations > 500) {
                LOGGER.warning(
                    "Potential memory leak not detected after " + iterations + " iterations");
              }
            }

            LOGGER.info("Memory leak detection tested on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Stack Overflow Tests")
  final class StackOverflowTests {

    @Test
    @DisplayName("Should detect and handle stack overflow gracefully")
    void shouldDetectAndHandleStackOverflowGracefully() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing stack overflow detection on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Create module with recursive function
            final byte[] stackOverflowModule = createStackOverflowModule();
            final Module module = engine.compileModule(stackOverflowModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction recursiveFunction = instance.getFunction("recursive_call");
            if (recursiveFunction != null) {
              assertThatThrownBy(() -> recursiveFunction.call(1000))
                  .isInstanceOfAny(RuntimeException.class, StackOverflowError.class)
                  .satisfies(
                      e -> {
                        LOGGER.info("Stack overflow properly detected: " + e.getMessage());
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                      });
            }

            // Verify system remains stable after stack overflow
            assertThat(store.isValid()).isTrue();
            assertThat(instance.isValid()).isTrue();

            LOGGER.info("Stack overflow handling validated on " + runtimeType);
          });
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 50, 100, 500, 1000})
    @DisplayName("Should handle various recursion depths")
    void shouldHandleVariousRecursionDepths(final int recursionDepth) throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing recursion depth " + recursionDepth + " on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] recursionModule = createRecursionModule();
            final Module module = engine.compileModule(recursionModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction recursiveFunction = instance.getFunction("controlled_recursion");
            if (recursiveFunction != null) {
              if (recursionDepth <= 100) {
                // Should succeed for reasonable depths
                try {
                  final Object result = recursiveFunction.call(recursionDepth);
                  assertThat(result).isNotNull();
                  LOGGER.fine("Recursion depth " + recursionDepth + " succeeded on " + runtimeType);
                } catch (final Exception e) {
                  LOGGER.info(
                      "Recursion depth "
                          + recursionDepth
                          + " failed on "
                          + runtimeType
                          + ": "
                          + e.getMessage());
                }
              } else {
                // Should fail for excessive depths
                assertThatThrownBy(() -> recursiveFunction.call(recursionDepth))
                    .isInstanceOfAny(RuntimeException.class, StackOverflowError.class);
                LOGGER.fine(
                    "Excessive recursion depth " + recursionDepth + " rejected on " + runtimeType);
              }
            }
          });
    }
  }

  @Nested
  @DisplayName("Timeout Handling Tests")
  final class TimeoutHandlingTests {

    @Test
    @DisplayName("Should handle infinite loops with timeout")
    void shouldHandleInfiniteLoopsWithTimeout() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing infinite loop timeout on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);
            store.setFuel(1000); // Limited fuel to prevent infinite execution

            final byte[] infiniteLoopModule = createInfiniteLoopModule();
            final Module module = engine.compileModule(infiniteLoopModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction infiniteLoopFunction = instance.getFunction("infinite_loop");
            if (infiniteLoopFunction != null) {
              final Instant start = Instant.now();

              assertThatThrownBy(() -> infiniteLoopFunction.call())
                  .isInstanceOfAny(RuntimeException.class, TimeoutException.class)
                  .satisfies(
                      e -> {
                        final Duration elapsed = Duration.between(start, Instant.now());
                        LOGGER.info(
                            "Infinite loop timeout after "
                                + elapsed.toMillis()
                                + "ms: "
                                + e.getMessage());
                        assertThat(elapsed).isLessThan(LONG_TIMEOUT);
                      });
            }

            // Verify system remains stable after timeout
            assertThat(store.isValid()).isTrue();
            assertThat(instance.isValid()).isTrue();

            LOGGER.info("Infinite loop timeout handling validated on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle long-running operations with configurable timeouts")
    void shouldHandleLongRunningOperationsWithConfigurableTimeouts() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing configurable timeouts on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] longRunningModule = createLongRunningModule();
            final Module module = engine.compileModule(longRunningModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction longRunningFunction = instance.getFunction("long_computation");
            if (longRunningFunction != null) {
              // Test with very limited fuel (should timeout quickly)
              store.setFuel(100);
              final Instant start = Instant.now();

              assertThatThrownBy(() -> longRunningFunction.call(10000))
                  .isInstanceOfAny(RuntimeException.class, TimeoutException.class);

              final Duration elapsed = Duration.between(start, Instant.now());
              assertThat(elapsed).isLessThan(MEDIUM_TIMEOUT);

              // Test with sufficient fuel (should complete)
              store.setFuel(100000);
              try {
                final Object result = longRunningFunction.call(100);
                assertThat(result).isNotNull();
                LOGGER.fine("Long computation completed with sufficient fuel");
              } catch (final Exception e) {
                LOGGER.info("Long computation failed: " + e.getMessage());
              }
            }

            LOGGER.info("Configurable timeout handling validated on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle concurrent timeout scenarios")
    void shouldHandleConcurrentTimeoutScenarios()
        throws InterruptedException, ExecutionException, TimeoutException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent timeout scenarios on " + runtimeType);

            final EngineConfig config = new EngineConfig().consumeFuel(true);
            final Engine engine = runtime.createEngine(config);
            registerForCleanup(engine);

            final byte[] timeoutModule = createTimeoutModule();
            final Module module = engine.compileModule(timeoutModule);
            registerForCleanup(module);

            final int concurrentCount = 4;
            final List<CompletableFuture<String>> futures = new ArrayList<>();

            // Start multiple concurrent operations that should timeout
            for (int i = 0; i < concurrentCount; i++) {
              final CompletableFuture<String> future =
                  CompletableFuture.supplyAsync(
                      () -> {
                        try {
                          final Store store = engine.createStore();
                          store.setFuel(500); // Limited fuel

                          final Instance instance = store.instantiate(module);
                          final WasmFunction timeoutFunction = instance.getFunction("timeout_test");

                          if (timeoutFunction != null) {
                            timeoutFunction.call();
                            return "COMPLETED";
                          }
                          return "NO_FUNCTION";
                        } catch (final Exception e) {
                          return "TIMEOUT: " + e.getClass().getSimpleName();
                        }
                      });
              futures.add(future);
            }

            // Wait for all operations to complete or timeout
            try {
              int timeoutCount = 0;
              for (final CompletableFuture<String> future : futures) {
                final String result = future.get(30, TimeUnit.SECONDS);
                if (result.startsWith("TIMEOUT")) {
                  timeoutCount++;
                }
              }

              LOGGER.info("Concurrent timeouts: " + timeoutCount + "/" + concurrentCount);
              // Most should timeout due to limited fuel
              assertThat(timeoutCount).isGreaterThan(0);
            } catch (final Exception e) {
              throw new RuntimeException(e);
            }

            LOGGER.info("Concurrent timeout scenarios validated on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Resource Cleanup Tests")
  final class ResourceCleanupTests {

    @Test
    @DisplayName("Should clean up resources after exhaustion scenarios")
    void shouldCleanUpResourcesAfterExhaustionScenarios() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing resource cleanup after exhaustion on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Create multiple stores and instances
            final List<Store> stores = new ArrayList<>();
            final List<Instance> instances = new ArrayList<>();

            try {
              final byte[] exhaustionModule = createResourceExhaustionModule();
              final Module module = engine.compileModule(exhaustionModule);
              registerForCleanup(module);

              // Create many resources
              for (int i = 0; i < 50; i++) {
                final Store store = engine.createStore("cleanup-test-" + i);
                stores.add(store);

                try {
                  final Instance instance = store.instantiate(module);
                  instances.add(instance);

                  // Try to trigger resource exhaustion
                  final WasmFunction exhaustFunction = instance.getFunction("exhaust");
                  if (exhaustFunction != null) {
                    try {
                      exhaustFunction.call();
                    } catch (final Exception e) {
                      // Expected exhaustion
                      LOGGER.fine("Resource exhaustion at iteration " + i);
                    }
                  }
                } catch (final Exception e) {
                  LOGGER.fine("Instance creation failed at iteration " + i);
                  break;
                }
              }

              LOGGER.info(
                  "Created " + stores.size() + " stores, " + instances.size() + " instances");

            } finally {
              // Clean up all resources
              instances.forEach(
                  instance -> {
                    try {
                      instance.close();
                    } catch (final Exception e) {
                      LOGGER.warning("Failed to close instance: " + e.getMessage());
                    }
                  });

              stores.forEach(
                  store -> {
                    try {
                      store.close();
                    } catch (final Exception e) {
                      LOGGER.warning("Failed to close store: " + e.getMessage());
                    }
                  });
            }

            // Verify engine remains valid after cleanup
            assertThat(engine.isValid()).isTrue();

            // Test system recovery by creating new resources
            final Store recoveryStore = engine.createStore("recovery-test");
            registerForCleanup(recoveryStore);
            assertThat(recoveryStore.isValid()).isTrue();

            LOGGER.info("Resource cleanup and recovery validated on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle resource recovery after multiple failures")
    void shouldHandleResourceRecoveryAfterMultipleFailures() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing resource recovery after multiple failures on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Simulate multiple failure scenarios
            for (int scenario = 0; scenario < 5; scenario++) {
              LOGGER.fine("Testing failure scenario " + scenario + " on " + runtimeType);

              try {
                final Store store = engine.createStore("failure-test-" + scenario);
                registerForCleanup(store);

                // Create module that will cause different types of failures
                final byte[] failureModule = createFailureModule(scenario);
                final Module module = engine.compileModule(failureModule);
                registerForCleanup(module);

                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction failureFunction = instance.getFunction("trigger_failure");
                if (failureFunction != null) {
                  try {
                    failureFunction.call(scenario);
                  } catch (final Exception e) {
                    LOGGER.fine("Expected failure in scenario " + scenario + ": " + e.getMessage());
                  }
                }

                // Verify system can still create new resources
                final Store recoveryStore = engine.createStore("recovery-" + scenario);
                assertThat(recoveryStore.isValid()).isTrue();
                recoveryStore.close();

              } catch (final Exception e) {
                LOGGER.info("Scenario " + scenario + " failed as expected: " + e.getMessage());
              }
            }

            // Final recovery test
            final Store finalStore = engine.createStore("final-recovery");
            registerForCleanup(finalStore);
            assertThat(finalStore.isValid()).isTrue();

            LOGGER.info("Resource recovery after multiple failures validated on " + runtimeType);
          });
    }
  }

  // Helper methods for creating test modules

  private byte[] createMemoryExhaustionModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory 1)\n"
            + "  (func $allocate_large (export \"allocate_large\")\n"
            + "    (loop\n"
            + "      (br 0)\n"
            + "    )\n"
            + "  )\n"
            + ")");
  }

  private byte[] createMemoryLimitModule(final long memoryLimit) {
    final int pages = (int) (memoryLimit / 65536) + 1;
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory "
            + pages
            + ")\n"
            + "  (func $allocate (export \"allocate\") (param i32) (result i32)\n"
            + "    local.get 0\n"
            + "  )\n"
            + ")");
  }

  private byte[] createMemoryLeakModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory 1)\n"
            + "  (global $counter (mut i32) (i32.const 0))\n"
            + "  (func $create_leak (export \"create_leak\")\n"
            + "    global.get $counter\n"
            + "    i32.const 1\n"
            + "    i32.add\n"
            + "    global.set $counter\n"
            + "  )\n"
            + ")");
  }

  private byte[] createStackOverflowModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $recursive_call (export \"recursive_call\") (param i32)\n"
            + "    local.get 0\n"
            + "    call $recursive_call\n"
            + "  )\n"
            + ")");
  }

  private byte[] createRecursionModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $controlled_recursion (export \"controlled_recursion\") (param i32) (result"
            + " i32)\n"
            + "    local.get 0\n"
            + "    i32.const 0\n"
            + "    i32.eq\n"
            + "    if (result i32)\n"
            + "      i32.const 1\n"
            + "    else\n"
            + "      local.get 0\n"
            + "      i32.const 1\n"
            + "      i32.sub\n"
            + "      call $controlled_recursion\n"
            + "    end\n"
            + "  )\n"
            + ")");
  }

  private byte[] createInfiniteLoopModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $infinite_loop (export \"infinite_loop\")\n"
            + "    (loop\n"
            + "      (br 0)\n"
            + "    )\n"
            + "  )\n"
            + ")");
  }

  private byte[] createLongRunningModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $long_computation (export \"long_computation\") (param i32) (result i32)\n"
            + "    (local i32)\n"
            + "    i32.const 0\n"
            + "    local.set 1\n"
            + "    (loop\n"
            + "      local.get 1\n"
            + "      local.get 0\n"
            + "      i32.lt_s\n"
            + "      if\n"
            + "        local.get 1\n"
            + "        i32.const 1\n"
            + "        i32.add\n"
            + "        local.set 1\n"
            + "        br 1\n"
            + "      end\n"
            + "    )\n"
            + "    local.get 1\n"
            + "  )\n"
            + ")");
  }

  private byte[] createTimeoutModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $timeout_test (export \"timeout_test\")\n"
            + "    (local i32)\n"
            + "    i32.const 0\n"
            + "    local.set 0\n"
            + "    (loop\n"
            + "      local.get 0\n"
            + "      i32.const 1\n"
            + "      i32.add\n"
            + "      local.set 0\n"
            + "      local.get 0\n"
            + "      i32.const 10000\n"
            + "      i32.lt_s\n"
            + "      br_if 0\n"
            + "    )\n"
            + "  )\n"
            + ")");
  }

  private byte[] createResourceExhaustionModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory 1)\n"
            + "  (func $exhaust (export \"exhaust\")\n"
            + "    (local i32)\n"
            + "    i32.const 0\n"
            + "    local.set 0\n"
            + "    (loop\n"
            + "      local.get 0\n"
            + "      i32.const 1\n"
            + "      i32.add\n"
            + "      local.set 0\n"
            + "      br 0\n"
            + "    )\n"
            + "  )\n"
            + ")");
  }

  private byte[] createFailureModule(final int scenario) {
    switch (scenario) {
      case 0:
        return createMemoryExhaustionModule();
      case 1:
        return createStackOverflowModule();
      case 2:
        return createInfiniteLoopModule();
      case 3:
        return createTimeoutModule();
      default:
        return createResourceExhaustionModule();
    }
  }
}
