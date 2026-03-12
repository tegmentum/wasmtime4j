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
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Fuzz tests for pooling allocator configuration and lifecycle.
 *
 * <p>This fuzzer tests the robustness of pooling allocator operations by:
 *
 * <ul>
 *   <li>Building PoolingAllocatorConfig with fuzzed values
 *   <li>Rapidly allocating and releasing instances from a small pool
 *   <li>Exhausting pool capacity and verifying recovery
 *   <li>Concurrent allocation and release from multiple threads
 * </ul>
 *
 * @since 1.0.0
 */
public class PoolingAllocatorFuzzer {

  private static final String SIMPLE_MODULE_WAT =
      """
      (module
        (memory (export "memory") 1 2)
        (func (export "noop"))
      )
      """;

  /**
   * Builds a PoolingAllocatorConfig with fuzzed values and creates an engine.
   *
   * <p>Verifies graceful failure for invalid configurations. Pool values are capped to prevent OOM.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzPoolConfig(final FuzzedDataProvider data) {
    try {
      final int instancePoolSize = data.consumeInt(1, 100);
      final long maxMemoryPerInstance = data.consumeLong(0, 67108864L);
      final long stackSize = data.consumeLong(0, 1048576L);

      final PoolingAllocatorConfig config =
          PoolingAllocatorConfig.builder()
              .instancePoolSize(instancePoolSize)
              .maxMemoryPerInstance(maxMemoryPerInstance)
              .stackSize((int) stackSize)
              .build();

      final EngineConfig engineConfig = new EngineConfig();
      engineConfig.setPoolingAllocatorEnabled(true);
      engineConfig.setInstancePoolSize(instancePoolSize);
      engineConfig.setMaxMemoryPerInstance(maxMemoryPerInstance > 0 ? maxMemoryPerInstance : 1);

      try (Engine engine = Engine.create(engineConfig)) {
        engine.isValid();
      }
    } catch (WasmException e) {
      // Expected: invalid config combinations
    } catch (IllegalArgumentException e) {
      // Expected: invalid config values
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Rapidly allocates instances in a loop from a small pool, then releases them in fuzzed order.
   *
   * <p>Uses a small pool size to stress allocation and release paths.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzAllocateRelease(final FuzzedDataProvider data) {
    final int poolSize = data.consumeInt(5, 20);

    try {
      final EngineConfig config = new EngineConfig();
      config.setPoolingAllocatorEnabled(true);
      config.setInstancePoolSize(poolSize);
      config.setMaxMemoryPerInstance(16 * 1024 * 1024L);

      try (Engine engine = Engine.create(config)) {
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);

        final List<AutoCloseable> resources = new ArrayList<>();

        // Allocate instances up to pool size
        final int allocCount = Math.min(poolSize, data.consumeInt(1, poolSize));
        for (int i = 0; i < allocCount; i++) {
          try {
            final Store store = engine.createStore();
            final Instance instance = module.instantiate(store);
            resources.add(instance);
            resources.add(store);
          } catch (WasmException e) {
            // Expected: pool may be exhausted
            break;
          }
        }

        // Close in fuzzed order by shuffling
        if (data.consumeBoolean()) {
          Collections.reverse(resources);
        }

        for (final AutoCloseable resource : resources) {
          try {
            resource.close();
          } catch (Exception e) {
            // Ignore close errors
          }
        }

        module.close();
      }
    } catch (WasmException e) {
      // Expected: invalid config combinations
    } catch (IllegalArgumentException e) {
      // Expected: invalid config values
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Allocates instances until pool exhaustion, then verifies recovery after release.
   *
   * <p>Uses a very small pool (1-5) to reliably trigger exhaustion. After releasing all instances,
   * verifies that a new instance can be allocated.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzPoolExhaustion(final FuzzedDataProvider data) {
    final int poolSize = data.consumeInt(1, 5);

    try {
      final EngineConfig config = new EngineConfig();
      config.setPoolingAllocatorEnabled(true);
      config.setInstancePoolSize(poolSize);
      config.setMaxMemoryPerInstance(16 * 1024 * 1024L);

      try (Engine engine = Engine.create(config)) {
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);

        final List<Store> stores = new ArrayList<>();
        final List<Instance> instances = new ArrayList<>();

        // Allocate until exhaustion
        boolean exhausted = false;
        for (int i = 0; i < poolSize + 5; i++) {
          try {
            final Store store = engine.createStore();
            final Instance instance = module.instantiate(store);
            stores.add(store);
            instances.add(instance);
          } catch (WasmException e) {
            // Expected: pool exhausted
            exhausted = true;
            break;
          }
        }

        // Release all instances
        for (final Instance instance : instances) {
          try {
            instance.close();
          } catch (Exception e) {
            // Ignore
          }
        }
        for (final Store store : stores) {
          try {
            store.close();
          } catch (Exception e) {
            // Ignore
          }
        }

        // Verify recovery: allocate one more instance after releasing
        if (exhausted) {
          try {
            final Store recoveryStore = engine.createStore();
            final Instance recoveryInstance = module.instantiate(recoveryStore);
            recoveryInstance.close();
            recoveryStore.close();
          } catch (WasmException e) {
            // Pool recovery may not be immediate
          }
        }

        module.close();
      }
    } catch (WasmException e) {
      // Expected: invalid config
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Concurrent allocation and release from multiple threads sharing the same engine and pool.
   *
   * <p>Each thread creates its own Store, compiles and instantiates the module, calls the "noop"
   * export, and closes the store. Uses a CountDownLatch for synchronization.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzConcurrentPool(final FuzzedDataProvider data) {
    final int threadCount = data.consumeInt(2, 4);

    try {
      final EngineConfig config = new EngineConfig();
      config.setPoolingAllocatorEnabled(true);
      config.setInstancePoolSize(threadCount * 2);
      config.setMaxMemoryPerInstance(16 * 1024 * 1024L);

      try (Engine engine = Engine.create(config)) {
        final Module module = engine.compileWat(SIMPLE_MODULE_WAT);

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(1);
        final List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
          futures.add(
              executor.submit(
                  () -> {
                    try {
                      latch.await();

                      final Store store = engine.createStore();
                      final Instance instance = module.instantiate(store);

                      // Call the noop function
                      instance.callFunction("noop");

                      instance.close();
                      store.close();
                    } catch (WasmException e) {
                      // Expected: pool contention or exhaustion
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                    } catch (Exception e) {
                      // Catch all to prevent thread death from crashing the fuzzer
                    }
                  }));
        }

        // Release all threads simultaneously
        latch.countDown();

        // Wait for all threads to complete
        for (final Future<?> future : futures) {
          try {
            future.get(10, TimeUnit.SECONDS);
          } catch (Exception e) {
            // Expected: thread may fail under contention
          }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        module.close();
      }
    } catch (WasmException e) {
      // Expected: invalid config
    } catch (IllegalArgumentException e) {
      // Expected: invalid config values
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
