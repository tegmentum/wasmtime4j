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

package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * Fuzz tests for concurrent access patterns.
 *
 * <p>This fuzzer tests thread safety by:
 *
 * <ul>
 *   <li>Concurrent module compilation from a shared Engine
 *   <li>Concurrent Store creation and closure
 *   <li>Concurrent function calls with per-thread Store+Instance
 *   <li>Engine close races during compilation
 *   <li>Store close races during function execution
 * </ul>
 *
 * <p>Engine is thread-safe. Store is NOT thread-safe. Module is immutable and shareable. Instance
 * is Store-bound. Each test validates that no JVM crash occurs under concurrent access.
 *
 * @since 1.0.0
 */
public class ConcurrentAccessFuzzer {

  private static final String ADD_MODULE_WAT =
      """
      (module
        (func $add (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add)
        (export "add" (func $add))
      )
      """;

  /**
   * Multiple threads compile modules from one Engine concurrently.
   *
   * <p>Engine is thread-safe, so concurrent compilations should all succeed or throw WasmException.
   * No JVM crash should occur.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzConcurrentModuleCompilation(final FuzzedDataProvider data) {
    final int threadCount = data.consumeInt(2, 8);

    try (Engine engine = Engine.create()) {
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final List<Future<?>> futures = new ArrayList<>();

      for (int i = 0; i < threadCount; i++) {
        futures.add(
            executor.submit(
                () -> {
                  try {
                    startLatch.await();
                    try (Module module = engine.compileWat(ADD_MODULE_WAT)) {
                      // Compilation succeeded; verify module is valid
                      module.getExports();
                    }
                  } catch (WasmException e) {
                    // Expected: compilation may fail under contention
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }));
      }

      startLatch.countDown();

      for (final Future<?> future : futures) {
        try {
          future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
          // Timeout or execution exception; test passes if no JVM crash
        }
      }

      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (WasmException e) {
      // Expected: engine creation may fail
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Multiple threads create and close Stores from one Engine.
   *
   * <p>Each thread creates its own Store, optionally creates an Instance, then closes the Store. No
   * JVM crash should occur.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzConcurrentStoreCreation(final FuzzedDataProvider data) {
    final int threadCount = data.consumeInt(2, 8);
    final boolean[] shouldInstantiate = new boolean[threadCount];
    for (int i = 0; i < threadCount; i++) {
      shouldInstantiate[i] = data.consumeBoolean();
    }

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(ADD_MODULE_WAT)) {
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final List<Future<?>> futures = new ArrayList<>();

      for (int i = 0; i < threadCount; i++) {
        final boolean instantiate = shouldInstantiate[i];
        futures.add(
            executor.submit(
                () -> {
                  try {
                    startLatch.await();
                    try (Store store = engine.createStore()) {
                      if (instantiate) {
                        try (Instance instance = module.instantiate(store)) {
                          instance.getFunction("add");
                        }
                      }
                    }
                  } catch (WasmException e) {
                    // Expected
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }));
      }

      startLatch.countDown();

      for (final Future<?> future : futures) {
        try {
          future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
          // Timeout or execution exception; test passes if no JVM crash
        }
      }

      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (WasmException e) {
      // Expected
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Each thread gets its own Store+Instance from a shared Engine+Module and calls exported
   * functions with fuzzed arguments concurrently.
   *
   * <p>Results should be correct or throw WasmException. No JVM crash should occur.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzConcurrentFunctionCalls(final FuzzedDataProvider data) {
    final int threadCount = data.consumeInt(2, 8);
    final int[] argA = new int[threadCount];
    final int[] argB = new int[threadCount];
    for (int i = 0; i < threadCount; i++) {
      argA[i] = data.consumeInt();
      argB[i] = data.consumeInt();
    }

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(ADD_MODULE_WAT)) {
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final List<Future<?>> futures = new ArrayList<>();

      for (int i = 0; i < threadCount; i++) {
        final int a = argA[i];
        final int b = argB[i];
        futures.add(
            executor.submit(
                () -> {
                  try {
                    startLatch.await();
                    try (Store store = engine.createStore();
                        Instance instance = module.instantiate(store)) {
                      final WasmValue[] results =
                          instance.callFunction("add", WasmValue.i32(a), WasmValue.i32(b));
                      if (results != null && results.length > 0) {
                        results[0].asInt();
                      }
                    }
                  } catch (WasmException e) {
                    // Expected
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }));
      }

      startLatch.countDown();

      for (final Future<?> future : futures) {
        try {
          future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
          // Timeout or execution exception; test passes if no JVM crash
        }
      }

      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (WasmException e) {
      // Expected
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * One thread closes the Engine while others attempt compilation.
   *
   * <p>Operations should throw WasmException or IllegalStateException, but never crash the JVM.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzEngineCloseRace(final FuzzedDataProvider data) {
    final int threadCount = data.consumeInt(2, 6);
    final int closeDelay = data.consumeInt(0, 5);

    try {
      final Engine engine = Engine.create();
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount + 1);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final List<Future<?>> futures = new ArrayList<>();

      // Threads that attempt compilation
      for (int i = 0; i < threadCount; i++) {
        futures.add(
            executor.submit(
                () -> {
                  try {
                    startLatch.await();
                    for (int j = 0; j < 3; j++) {
                      try (Module module = engine.compileWat(ADD_MODULE_WAT)) {
                        module.getExports();
                      } catch (WasmException e) {
                        // Expected after engine close
                      } catch (IllegalStateException e) {
                        // Expected if engine already closed
                      }
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }));
      }

      // Thread that closes the engine
      futures.add(
          executor.submit(
              () -> {
                try {
                  startLatch.await();
                  if (closeDelay > 0) {
                    Thread.sleep(closeDelay);
                  }
                  engine.close();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                } catch (Exception e) {
                  // Expected: close may race with operations
                }
              }));

      startLatch.countDown();

      for (final Future<?> future : futures) {
        try {
          future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
          // Test passes if no JVM crash
        }
      }

      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (WasmException e) {
      // Expected
    } catch (IllegalStateException e) {
      // Expected
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * One thread closes a Store while another calls a function on that Store's Instance.
   *
   * <p>Operations should throw cleanly, never crash the JVM.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzStoreCloseRace(final FuzzedDataProvider data) {
    final int callCount = data.consumeInt(1, 10);
    final int closeDelay = data.consumeInt(0, 3);
    final int argA = data.consumeInt();
    final int argB = data.consumeInt();

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(ADD_MODULE_WAT)) {
      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);

      final ExecutorService executor = Executors.newFixedThreadPool(2);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final List<Future<?>> futures = new ArrayList<>();

      // Thread that calls the function repeatedly
      futures.add(
          executor.submit(
              () -> {
                try {
                  startLatch.await();
                  for (int i = 0; i < callCount; i++) {
                    try {
                      instance.callFunction("add", WasmValue.i32(argA), WasmValue.i32(argB));
                    } catch (WasmException e) {
                      // Expected after store close
                    } catch (IllegalStateException e) {
                      // Expected if store already closed
                    }
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }));

      // Thread that closes the store
      futures.add(
          executor.submit(
              () -> {
                try {
                  startLatch.await();
                  if (closeDelay > 0) {
                    Thread.sleep(closeDelay);
                  }
                  store.close();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                } catch (Exception e) {
                  // Expected: close may race
                }
              }));

      startLatch.countDown();

      for (final Future<?> future : futures) {
        try {
          future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
          // Test passes if no JVM crash
        }
      }

      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      // Clean up instance (store may already be closed)
      try {
        instance.close();
      } catch (Exception e) {
        // Expected
      }
    } catch (WasmException e) {
      // Expected
    } catch (IllegalStateException e) {
      // Expected
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
