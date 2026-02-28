package ai.tegmentum.wasmtime4j.tests.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for thread-safety and concurrent execution.
 *
 * <p>IMPORTANT: Wasmtime's Store is NOT thread-safe. Each thread must have its own Store. Engine
 * and Module compilation ARE thread-safe and can be shared across threads.
 */
public class ConcurrencyTest {

  private static final Logger LOGGER = Logger.getLogger(ConcurrencyTest.class.getName());

  @Test
  @DisplayName("Parallel instance creation with per-thread stores")
  public void testParallelInstanceCreation() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    // Engine is thread-safe and can be shared
    final Engine engine = Engine.create();
    final Module module = engine.compileWat(wat);

    final int threadCount = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              () -> {
                latch.countDown();
                latch.await(); // Wait for all threads to be ready

                // Each thread gets its own Store (Store is NOT thread-safe)
                final Store store = engine.createStore();
                final Instance instance = module.instantiate(store);
                final WasmValue[] results = instance.callFunction("test");
                final int result = results[0].asInt();

                instance.close();
                store.close();
                return result;
              }));
    }

    // Verify all returned 42
    for (Future<Integer> future : futures) {
      assertEquals(42, future.get(), "Each thread should independently produce 42");
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    engine.close();
  }

  @Test
  @DisplayName("Parallel function calls with per-thread stores")
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

    // Engine and Module are thread-safe
    final Engine engine = Engine.create();
    final Module module = engine.compileWat(wat);

    final int threadCount = 20;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicReference<Throwable> firstError = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      final int value = i + 1;

      executor.submit(
          () -> {
            try {
              latch.countDown();
              latch.await();

              // Each thread creates its own Store and Instance
              final Store store = engine.createStore();
              final Instance instance = module.instantiate(store);
              final WasmValue[] results = instance.callFunction("compute", WasmValue.i32(value));
              final int actual = results[0].asInt();
              final int expected = value * value;

              if (actual == expected) {
                successCount.incrementAndGet();
              } else {
                firstError.compareAndSet(
                    null,
                    new AssertionError(
                        "Expected " + expected + " but got " + actual + " for value " + value));
              }

              instance.close();
              store.close();
            } catch (Exception e) {
              firstError.compareAndSet(null, e);
            }
          });
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    final Throwable error = firstError.get();
    if (error != null) {
      fail("Thread failed with: " + error.getMessage(), error);
    }
    assertEquals(
        threadCount,
        successCount.get(),
        "All " + threadCount + " threads should compute correct results");

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
      assertEquals(i, results[0].asInt(), "Counter should be " + i + " after " + i + " increments");
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
                // Each thread creates its own Engine, Store, Module, and Instance
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
      assertEquals(42, future.get(), "Each engine should independently produce 42");
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
  }

  @Test
  @DisplayName("Stress test with many short-lived instances using per-thread stores")
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

    // Engine and Module are thread-safe
    final Engine engine = Engine.create();
    final Module module = engine.compileWat(wat);

    final int threadCount = 10;
    final int iterationsPerThread = 50;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicReference<Throwable> firstError = new AtomicReference<>();
    final CountDownLatch startLatch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
      executor.submit(
          () -> {
            try {
              startLatch.countDown();
              startLatch.await();

              // Each thread creates its own Store
              final Store store = engine.createStore();

              for (int i = 0; i < iterationsPerThread; i++) {
                final Instance instance = module.instantiate(store);
                final WasmValue[] results =
                    instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(32));
                if (results[0].asInt() == 42) {
                  successCount.incrementAndGet();
                } else {
                  firstError.compareAndSet(
                      null,
                      new AssertionError(
                          "Expected 42 but got " + results[0].asInt() + " at iteration " + i));
                }
                instance.close();
              }

              store.close();
            } catch (Exception e) {
              firstError.compareAndSet(null, e);
            }
          });
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

    final Throwable error = firstError.get();
    if (error != null) {
      fail("Thread failed with: " + error.getMessage(), error);
    }
    assertEquals(
        threadCount * iterationsPerThread,
        successCount.get(),
        "All iterations across all threads should succeed");

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

    // Engine compilation IS thread-safe
    final Engine engine = Engine.create();

    final int threadCount = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              () -> {
                // Compile module on this thread
                final Module module = engine.compileWat(wat);
                // Verify the compiled module works
                final Store store = engine.createStore();
                final Instance instance = module.instantiate(store);
                final WasmValue[] results = instance.callFunction("test");
                final int result = results[0].asInt();
                instance.close();
                store.close();
                return result;
              }));
    }

    for (Future<Integer> future : futures) {
      assertEquals(42, future.get(), "Parallel-compiled module should produce correct result");
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    engine.close();
  }

  @Test
  @DisplayName("Independent instance state with per-thread stores")
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

    // Engine and Module are thread-safe
    final Engine engine = Engine.create();
    final Module module = engine.compileWat(wat);

    final int instanceCount = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(instanceCount);
    final List<Future<Integer>> futures = new ArrayList<>();

    // Each thread gets its own Store + Instance
    for (int i = 0; i < instanceCount; i++) {
      final int value = i * 10;

      futures.add(
          executor.submit(
              () -> {
                final Store store = engine.createStore();
                final Instance instance = module.instantiate(store);

                instance.callFunction("set", WasmValue.i32(value));
                Thread.sleep(10); // Small delay to allow interleaving
                final WasmValue[] results = instance.callFunction("get");
                final int result = results[0].asInt();

                instance.close();
                store.close();
                return result;
              }));
    }

    // Verify each instance maintained its own value
    for (int i = 0; i < instanceCount; i++) {
      assertEquals(
          i * 10, futures.get(i).get(), "Instance " + i + " should have value " + (i * 10));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    engine.close();
  }

  @Test
  @DisplayName("Concurrent host function callbacks via per-thread stores")
  public void testConcurrentHostFunctionCallbacks() throws Exception {
    final String wat =
        """
        (module
          (import "env" "callback" (func $callback (param i32) (result i32)))
          (func (export "call_host") (param i32) (result i32)
            local.get 0
            call $callback
          )
        )
        """;

    final Engine engine = Engine.create();
    final Module module = engine.compileWat(wat);
    final AtomicInteger totalCallbackInvocations = new AtomicInteger(0);
    final AtomicReference<Throwable> firstError = new AtomicReference<>();

    final int threadCount = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int threadValue = i + 1;
      futures.add(
          executor.submit(
              () -> {
                try {
                  latch.countDown();
                  latch.await();

                  // Each thread creates its own Linker, Store, and Instance
                  final Linker<Void> linker = Linker.create(engine);

                  linker.defineHostFunction(
                      "env",
                      "callback",
                      FunctionType.of(
                          new WasmValueType[] {WasmValueType.I32},
                          new WasmValueType[] {WasmValueType.I32}),
                      (params) -> {
                        totalCallbackInvocations.incrementAndGet();
                        return new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)};
                      });

                  final Store store = engine.createStore();
                  final Instance instance = linker.instantiate(store, module);

                  final WasmValue[] results =
                      instance.callFunction("call_host", WasmValue.i32(threadValue));
                  final int result = results[0].asInt();

                  instance.close();
                  store.close();
                  linker.close();

                  return result;
                } catch (Exception e) {
                  firstError.compareAndSet(null, e);
                  return -1;
                }
              }));
    }

    for (int i = 0; i < threadCount; i++) {
      final int expected = (i + 1) * 2;
      assertEquals(
          expected,
          futures.get(i).get(),
          "Thread " + i + " should get " + expected + " from host callback");
    }

    final Throwable error = firstError.get();
    if (error != null) {
      fail("Thread failed with: " + error.getMessage(), error);
    }

    assertEquals(
        threadCount,
        totalCallbackInvocations.get(),
        "Host function should be invoked once per thread");

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    engine.close();
  }
}
