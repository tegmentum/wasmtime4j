package ai.tegmentum.wasmtime4j.tests.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for thread-safety and concurrent execution. */
public class ConcurrencyTest {

  @Test
  @DisplayName("Parallel instance creation")
  public void testParallelInstanceCreation() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final int threadCount = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              () -> {
                try {
                  latch.countDown();
                  latch.await(); // Wait for all threads to be ready

                  final Instance instance = module.instantiate(store);
                  final WasmValue[] results = instance.callFunction("test");
                  instance.close();
                  return results[0].asInt();
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              }));
    }

    // Verify all returned 42
    for (Future<Integer> future : futures) {
      assertEquals(42, future.get());
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Parallel function calls on different instances")
  public void testParallelFunctionCalls() throws Exception {
    final String wat =
        """
        (module
          (func (export "compute") (param i32) (result i32)
            local.get 0
            local.get 0
            i32.mul
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final int instanceCount = 5;
    final Instance[] instances = new Instance[instanceCount];

    for (int i = 0; i < instanceCount; i++) {
      instances[i] = module.instantiate(store);
    }

    final int threadCount = 20;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      final int value = i + 1;
      final Instance instance = instances[i % instanceCount];

      executor.submit(
          () -> {
            try {
              latch.countDown();
              latch.await();

              final WasmValue[] results = instance.callFunction("compute", WasmValue.i32(value));
              if (results[0].asInt() == value * value) {
                successCount.incrementAndGet();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    assertEquals(threadCount, successCount.get());

    for (Instance instance : instances) {
      instance.close();
    }
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Sequential calls maintain state")
  public void testSequentialStateConsistency() throws Exception {
    final String wat =
        """
        (module
          (global $counter (mut i32) (i32.const 0))
          (func (export "increment") (result i32)
            global.get $counter
            i32.const 1
            i32.add
            global.set $counter
            global.get $counter
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final int iterations = 100;
    for (int i = 1; i <= iterations; i++) {
      final WasmValue[] results = instance.callFunction("increment");
      assertEquals(i, results[0].asInt());
    }

    instance.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Multiple engines in parallel")
  public void testMultipleEngines() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    final int engineCount = 5;
    final ExecutorService executor = Executors.newFixedThreadPool(engineCount);
    final List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < engineCount; i++) {
      futures.add(
          executor.submit(
              () -> {
                final Engine engine = Engine.create();
                final Store store = engine.createStore();
                final Module module = engine.compileWat(wat);
                final Instance instance = module.instantiate(store);

                final WasmValue[] results = instance.callFunction("test");
                final int result = results[0].asInt();

                instance.close();
                store.close();
                engine.close();

                return result;
              }));
    }

    for (Future<Integer> future : futures) {
      assertEquals(42, future.get());
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
  }

  @Test
  @DisplayName("Stress test with many short-lived instances")
  public void testStressShortLivedInstances() throws Exception {
    final String wat =
        """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final int threadCount = 10;
    final int iterationsPerThread = 50;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final CountDownLatch startLatch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
      executor.submit(
          () -> {
            try {
              startLatch.countDown();
              startLatch.await();

              for (int i = 0; i < iterationsPerThread; i++) {
                final Instance instance = module.instantiate(store);
                final WasmValue[] results =
                    instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(32));
                if (results[0].asInt() == 42) {
                  successCount.incrementAndGet();
                }
                instance.close();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

    assertEquals(threadCount * iterationsPerThread, successCount.get());

    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Module compilation in parallel")
  public void testParallelModuleCompilation() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();

    final int threadCount = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final List<Future<Boolean>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              () -> {
                try {
                  final Module module = engine.compileWat(wat);
                  return module != null;
                } catch (Exception e) {
                  e.printStackTrace();
                  return false;
                }
              }));
    }

    for (Future<Boolean> future : futures) {
      assertTrue(future.get());
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    engine.close();
  }

  @Test
  @DisplayName("Independent instance state")
  public void testIndependentInstanceState() throws Exception {
    final String wat =
        """
        (module
          (global $value (mut i32) (i32.const 0))
          (func (export "set") (param i32)
            local.get 0
            global.set $value
          )
          (func (export "get") (result i32)
            global.get $value
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final int instanceCount = 10;
    final Instance[] instances = new Instance[instanceCount];

    // Create instances
    for (int i = 0; i < instanceCount; i++) {
      instances[i] = module.instantiate(store);
    }

    final ExecutorService executor = Executors.newFixedThreadPool(instanceCount);
    final List<Future<Integer>> futures = new ArrayList<>();

    // Each instance sets its own value
    for (int i = 0; i < instanceCount; i++) {
      final int value = i * 10;
      final Instance instance = instances[i];

      futures.add(
          executor.submit(
              () -> {
                instance.callFunction("set", WasmValue.i32(value));
                Thread.sleep(10); // Small delay
                final WasmValue[] results = instance.callFunction("get");
                return results[0].asInt();
              }));
    }

    // Verify each instance maintained its own value
    for (int i = 0; i < instanceCount; i++) {
      assertEquals(i * 10, futures.get(i).get());
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    for (Instance instance : instances) {
      instance.close();
    }
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Race condition prevention")
  public void testRaceConditionPrevention() throws Exception {
    final String wat =
        """
        (module
          (memory 1)
          (func (export "atomic_inc") (param i32) (result i32)
            ;; Simulated atomic increment
            local.get 0
            local.get 0
            i32.load
            i32.const 1
            i32.add
            i32.store

            local.get 0
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final int threadCount = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final List<Future<Integer>> futures = new ArrayList<>();
    final CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              () -> {
                try {
                  latch.countDown();
                  latch.await();

                  final WasmValue[] results = instance.callFunction("atomic_inc", WasmValue.i32(0));
                  return results[0].asInt();
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              }));
    }

    // All threads should complete successfully (ReentrantLock should handle this)
    for (Future<Integer> future : futures) {
      assertNotNull(future.get()); // Should not throw
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    instance.close();
    store.close();
    engine.close();
  }
}
