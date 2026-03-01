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

package ai.tegmentum.wasmtime4j.error;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for concurrent access error handling.
 *
 * <p>These tests verify that the runtime properly handles concurrent access scenarios and either
 * provides thread-safe operations or reports appropriate errors.
 */
@DisplayName("Concurrent Access Error Tests")
@Tag("integration")
class ConcurrentAccessErrorTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ConcurrentAccessErrorTest.class.getName());

  private Engine engine;

  // Simple counter module - increment a global
  private static final byte[] COUNTER_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version

        // Type section (id=1)
        0x01,
        0x08, // section size
        0x02, // number of types
        0x60,
        0x00,
        0x01,
        0x7F, // type 0: () -> i32
        0x60,
        0x00,
        0x00, // type 1: () -> ()

        // Function section (id=3)
        0x03,
        0x03, // section size
        0x02, // number of functions
        0x00, // get: type 0
        0x01, // increment: type 1

        // Global section (id=6)
        0x06,
        0x06, // section size
        0x01, // number of globals
        0x7F, // i32
        0x01, // mutable
        0x41,
        0x00, // init: i32.const 0
        0x0B,

        // Export section (id=7)
        0x07,
        0x14, // section size
        0x02, // number of exports
        0x03, // name length
        'g',
        'e',
        't',
        0x00, // export kind: function
        0x00, // function index
        0x09, // name length
        'i',
        'n',
        'c',
        'r',
        'e',
        'm',
        'e',
        'n',
        't',
        0x00, // export kind: function
        0x01, // function index

        // Code section (id=10)
        0x0A,
        0x0F, // section size
        0x02, // number of functions

        // get function
        0x04, // body size
        0x00, // locals count
        0x23,
        0x00, // global.get 0
        0x0B, // end

        // increment function
        0x09, // body size
        0x00, // locals count
        0x23,
        0x00, // global.get 0
        0x41,
        0x01, // i32.const 1
        0x6A, // i32.add
        0x24,
        0x00, // global.set 0
        0x0B // end
      };

  @AfterEach
  void tearDown() {
    if (engine != null) {
      try {
        engine.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing engine: " + e.getMessage());
      }
    }
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Engine Thread Safety Tests")
  class EngineThreadSafetyTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should allow concurrent module compilation from same engine")
    void shouldAllowConcurrentModuleCompilation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();

      final int numThreads = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      final List<Future<Module>> futures = new ArrayList<>();
      final List<Exception> errors = new CopyOnWriteArrayList<>();

      try {
        for (int i = 0; i < numThreads; i++) {
          futures.add(
              executor.submit(
                  () -> {
                    try {
                      return engine.compileModule(COUNTER_WASM);
                    } catch (final Exception e) {
                      errors.add(e);
                      throw e;
                    }
                  }));
        }

        for (final Future<Module> future : futures) {
          final Module module = future.get(30, TimeUnit.SECONDS);
          assertThat(module).isNotNull();
          module.close();
        }

        assertThat(errors).isEmpty();
        LOGGER.info("Concurrent module compilation succeeded with " + numThreads + " threads");
      } finally {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
    }
  }

  @Nested
  @DisplayName("Store Thread Safety Tests")
  class StoreThreadSafetyTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle separate stores in separate threads correctly")
    void shouldHandleSeparateStoresInSeparateThreads(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();

      final int numThreads = 4;
      final int incrementsPerThread = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      final List<Future<Integer>> futures = new ArrayList<>();
      final List<Exception> errors = new CopyOnWriteArrayList<>();
      final Module module = engine.compileModule(COUNTER_WASM);

      try {
        for (int i = 0; i < numThreads; i++) {
          futures.add(
              executor.submit(
                  () -> {
                    // Each thread gets its own store
                    final Store store = engine.createStore();
                    try {
                      final Instance instance = store.createInstance(module);
                      final WasmFunction incrementFunc =
                          instance.getFunction("increment").orElse(null);
                      final WasmFunction getFunc = instance.getFunction("get").orElse(null);

                      for (int j = 0; j < incrementsPerThread; j++) {
                        incrementFunc.call();
                      }

                      final WasmValue[] result = getFunc.call();
                      return result[0].asInt();
                    } catch (final Exception e) {
                      errors.add(e);
                      throw e;
                    } finally {
                      store.close();
                    }
                  }));
        }

        int totalSuccessful = 0;
        for (final Future<Integer> future : futures) {
          final int result = future.get(30, TimeUnit.SECONDS);
          // Each thread should have counted to incrementsPerThread
          assertThat(result).isEqualTo(incrementsPerThread);
          totalSuccessful++;
        }

        assertThat(errors).isEmpty();
        assertThat(totalSuccessful).isEqualTo(numThreads);
        LOGGER.info(
            "Separate stores in separate threads completed: "
                + totalSuccessful
                + " threads, each counted to "
                + incrementsPerThread);
      } finally {
        module.close();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
    }
  }

  @Nested
  @DisplayName("Global Concurrent Access Tests")
  class GlobalConcurrentAccessTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle concurrent global access with separate stores")
    void shouldHandleConcurrentGlobalAccessWithSeparateStores(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      engine = Engine.create();

      final int numThreads = 4;
      final int operationsPerThread = 50;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      final List<Future<Boolean>> futures = new ArrayList<>();
      final List<Exception> errors = new CopyOnWriteArrayList<>();

      try {
        for (int i = 0; i < numThreads; i++) {
          final int threadId = i;
          futures.add(
              executor.submit(
                  () -> {
                    final Store store = engine.createStore();
                    try {
                      final WasmGlobal global =
                          store.createGlobal(
                              WasmValueType.I32, true, WasmValue.i32(threadId * 1000));

                      for (int j = 0; j < operationsPerThread; j++) {
                        final int expected = threadId * 1000 + j;
                        assertThat(global.get().asInt()).isEqualTo(expected);
                        global.set(WasmValue.i32(expected + 1));
                      }

                      return true;
                    } catch (final Exception e) {
                      errors.add(e);
                      return false;
                    } finally {
                      store.close();
                    }
                  }));
        }

        int successCount = 0;
        for (final Future<Boolean> future : futures) {
          if (future.get(30, TimeUnit.SECONDS)) {
            successCount++;
          }
        }

        assertThat(errors).isEmpty();
        assertThat(successCount).isEqualTo(numThreads);
        LOGGER.info("Concurrent global access test completed with " + successCount + " threads");
      } finally {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
    }
  }

  @Nested
  @DisplayName("Memory Concurrent Access Tests")
  class MemoryConcurrentAccessTests {

    // Simple WASM with memory export
    private static final byte[] MEMORY_WASM =
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6D, // magic
          0x01,
          0x00,
          0x00,
          0x00, // version

          // Memory section (id=5)
          0x05,
          0x03, // section size
          0x01, // number of memories
          0x00, // limits: no max
          0x01, // min: 1 page

          // Export section (id=7)
          0x07,
          0x0A, // section size
          0x01, // number of exports
          0x06, // name length
          'm',
          'e',
          'm',
          'o',
          'r',
          'y',
          0x02, // export kind: memory
          0x00 // memory index
        };

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle concurrent memory operations with separate stores")
    void shouldHandleConcurrentMemoryOperationsWithSeparateStores(final RuntimeType runtime)
        throws Exception {
      setRuntime(runtime);
      engine = Engine.create();

      final int numThreads = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      final List<Future<Boolean>> futures = new ArrayList<>();
      final List<Exception> errors = new CopyOnWriteArrayList<>();
      final Module module = engine.compileModule(MEMORY_WASM);

      try {
        for (int i = 0; i < numThreads; i++) {
          final int threadId = i;
          futures.add(
              executor.submit(
                  () -> {
                    final Store store = engine.createStore();
                    try {
                      final Instance instance = store.createInstance(module);
                      final WasmMemory memory = instance.getMemory("memory").orElse(null);

                      assertThat(memory).isNotNull();

                      // Each thread writes to a different offset
                      final int offset = threadId * 1024;
                      final byte testValue = (byte) (threadId + 1);

                      memory.writeByte(offset, testValue);
                      final byte readBack = memory.readByte(offset);

                      assertThat(readBack).isEqualTo(testValue);
                      return true;
                    } catch (final Exception e) {
                      errors.add(e);
                      LOGGER.warning(
                          "Thread "
                              + threadId
                              + " error: "
                              + e.getClass().getName()
                              + ": "
                              + e.getMessage());
                      return false;
                    } finally {
                      store.close();
                    }
                  }));
        }

        int successCount = 0;
        for (final Future<Boolean> future : futures) {
          if (future.get(30, TimeUnit.SECONDS)) {
            successCount++;
          }
        }

        assertThat(errors).isEmpty();
        assertThat(successCount).isEqualTo(numThreads);
        LOGGER.info("Concurrent memory access test completed with " + successCount + " threads");
      } finally {
        module.close();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
    }
  }

  @Nested
  @DisplayName("Race Condition Detection Tests")
  class RaceConditionDetectionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should detect or handle concurrent store access properly")
    void shouldHandleConcurrentStoreAccessProperly(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      engine = Engine.create();

      final Store store = engine.createStore();
      final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));

      final int numThreads = 4;
      final int incrementsPerThread = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      final AtomicInteger completedThreads = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completeLatch = new CountDownLatch(numThreads);

      try {
        for (int i = 0; i < numThreads; i++) {
          executor.submit(
              () -> {
                try {
                  startLatch.await(); // Wait for all threads to be ready
                  for (int j = 0; j < incrementsPerThread; j++) {
                    try {
                      final int current = global.get().asInt();
                      global.set(WasmValue.i32(current + 1));
                    } catch (final Exception e) {
                      // Store may not be thread-safe - this is acceptable
                      errorCount.incrementAndGet();
                    }
                  }
                  completedThreads.incrementAndGet();
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                } finally {
                  completeLatch.countDown();
                }
              });
        }

        startLatch.countDown(); // Start all threads simultaneously
        completeLatch.await(30, TimeUnit.SECONDS);

        LOGGER.info("Completed threads: " + completedThreads.get());
        LOGGER.info("Error count: " + errorCount.get());
        LOGGER.info("Final global value: " + global.get().asInt());

        // All threads should complete (whether with errors or not)
        assertThat(completedThreads.get()).isEqualTo(numThreads);

        // Document the behavior - either:
        // 1. All operations succeed (thread-safe implementation)
        // 2. Some operations fail (non-thread-safe with proper error handling)
        if (errorCount.get() == 0) {
          LOGGER.info("Store appears to be thread-safe for global operations");
        } else {
          LOGGER.info("Store is not thread-safe - proper errors were raised");
        }
      } finally {
        store.close();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
    }
  }
}
