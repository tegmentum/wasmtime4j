package ai.tegmentum.wasmtime4j.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Stress tests for thread safety of the runtime under concurrent workloads.
 *
 * <p>Tests concurrent engine creation, function calls, and module compilation to verify that the
 * runtime handles multi-threaded access correctly without crashes or data corruption.
 */
public class ConcurrentAccessStressTest {

  private static final Logger LOGGER =
      Logger.getLogger(ConcurrentAccessStressTest.class.getName());

  private static final String SIMPLE_WAT =
      "(module\n"
          + "  (func (export \"get42\") (result i32) i32.const 42)\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
          + ")";

  private static final String COUNTER_WAT =
      "(module\n"
          + "  (global $counter (mut i32) (i32.const 0))\n"
          + "  (func (export \"increment\") (result i32)\n"
          + "    global.get $counter\n"
          + "    i32.const 1\n"
          + "    i32.add\n"
          + "    global.set $counter\n"
          + "    global.get $counter\n"
          + "  )\n"
          + "  (func (export \"get\") (result i32)\n"
          + "    global.get $counter\n"
          + "  )\n"
          + ")";

  private WasmRuntime runtime;

  @BeforeEach
  void setUp() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    runtime = WasmRuntimeFactory.create();
    LOGGER.info("Runtime created: " + runtime.getClass().getSimpleName());
  }

  @AfterEach
  void tearDown() throws IOException {
    if (runtime != null) {
      runtime.close();
    }
  }

  @Test
  void concurrentEngineCreation() throws Exception {
    final int threadCount = 8;
    final int cyclesPerThread = 100;
    LOGGER.info(
        "Starting concurrentEngineCreation: "
            + threadCount
            + " threads x "
            + cyclesPerThread
            + " cycles");

    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CyclicBarrier barrier = new CyclicBarrier(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger errorCount = new AtomicInteger(0);

    final List<Future<?>> futures = new ArrayList<>();

    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      futures.add(
          executor.submit(
              () -> {
                try {
                  barrier.await(30, TimeUnit.SECONDS);
                  for (int i = 0; i < cyclesPerThread; i++) {
                    try (Engine engine = runtime.createEngine();
                        Store store = engine.createStore();
                        Module module = engine.compileWat(SIMPLE_WAT);
                        Instance instance = module.instantiate(store)) {
                      assertNotNull(
                          instance,
                          "Instance null at thread " + threadId + " cycle " + i);
                      assertTrue(
                          instance.getFunction("get42").isPresent(),
                          "Missing get42 at thread " + threadId + " cycle " + i);
                      successCount.incrementAndGet();
                    }
                  }
                } catch (final Exception e) {
                  LOGGER.severe(
                      "Thread " + threadId + " failed: " + e.getMessage());
                  errorCount.incrementAndGet();
                  throw new RuntimeException(e);
                }
              }));
    }

    for (final Future<?> future : futures) {
      future.get(120, TimeUnit.SECONDS);
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

    LOGGER.info(
        "concurrentEngineCreation completed: "
            + successCount.get()
            + " successes, "
            + errorCount.get()
            + " errors");

    assertEquals(0, errorCount.get(), "No errors should occur during concurrent engine creation");
    assertEquals(
        threadCount * cyclesPerThread,
        successCount.get(),
        "All cycles should succeed");
  }

  @Test
  void concurrentFunctionCalls() throws Exception {
    final int threadCount = 8;
    final int callsPerThread = 500;
    LOGGER.info(
        "Starting concurrentFunctionCalls: "
            + threadCount
            + " threads x "
            + callsPerThread
            + " calls");

    // Each thread gets its own store+instance because Store is not thread-safe.
    // The Engine and Module are shared (they are thread-safe).
    try (Engine engine = runtime.createEngine();
        Module module = engine.compileWat(SIMPLE_WAT)) {

      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CyclicBarrier barrier = new CyclicBarrier(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);

      final List<Future<?>> futures = new ArrayList<>();

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        futures.add(
            executor.submit(
                () -> {
                  try {
                    barrier.await(30, TimeUnit.SECONDS);
                    // Each thread has its own store and instance
                    try (Store store = engine.createStore();
                        Instance instance = module.instantiate(store)) {
                      final WasmFunction addFunc =
                          instance
                              .getFunction("add")
                              .orElseThrow(
                                  () ->
                                      new AssertionError(
                                          "Missing add function on thread " + threadId));

                      for (int i = 0; i < callsPerThread; i++) {
                        final WasmValue[] result =
                            addFunc.call(WasmValue.i32(i), WasmValue.i32(threadId));
                        assertNotNull(
                            result,
                            "Result null at thread " + threadId + " call " + i);
                        assertEquals(
                            1,
                            result.length,
                            "Expected 1 result at thread " + threadId + " call " + i);
                        assertEquals(
                            i + threadId,
                            result[0].asI32(),
                            "Wrong result at thread " + threadId + " call " + i);
                        successCount.incrementAndGet();
                      }
                    }
                  } catch (final Exception e) {
                    LOGGER.severe(
                        "Thread " + threadId + " failed: " + e.getMessage());
                    errorCount.incrementAndGet();
                    throw new RuntimeException(e);
                  }
                }));
      }

      for (final Future<?> future : futures) {
        future.get(120, TimeUnit.SECONDS);
      }

      executor.shutdown();
      assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

      LOGGER.info(
          "concurrentFunctionCalls completed: "
              + successCount.get()
              + " successes, "
              + errorCount.get()
              + " errors");

      assertEquals(0, errorCount.get(), "No errors should occur during concurrent function calls");
      assertEquals(
          threadCount * callsPerThread,
          successCount.get(),
          "All function calls should succeed");
    }
  }

  @Test
  void concurrentModuleCompilation() throws Exception {
    final int threadCount = 4;
    final int compilationsPerThread = 100;
    LOGGER.info(
        "Starting concurrentModuleCompilation: "
            + threadCount
            + " threads x "
            + compilationsPerThread
            + " compilations");

    // Different WAT sources for each thread to ensure compilation is truly concurrent
    final String[] watSources = {
      "(module (func (export \"f0\") (result i32) i32.const 0))",
      "(module (func (export \"f1\") (param i32) (result i32) local.get 0 i32.const 1 i32.add))",
      "(module (func (export \"f2\") (param i32 i32) (result i32) local.get 0 local.get 1"
          + " i32.mul))",
      "(module (func (export \"f3\") (param i64) (result i64) local.get 0 i64.const 2 i64.div_s))",
    };

    try (Engine engine = runtime.createEngine()) {
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CyclicBarrier barrier = new CyclicBarrier(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);

      final List<Future<?>> futures = new ArrayList<>();

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        final String wat = watSources[t % watSources.length];
        futures.add(
            executor.submit(
                () -> {
                  try {
                    barrier.await(30, TimeUnit.SECONDS);
                    for (int i = 0; i < compilationsPerThread; i++) {
                      try (Module module = engine.compileWat(wat)) {
                        assertNotNull(
                            module,
                            "Module null at thread " + threadId + " compilation " + i);
                        assertTrue(
                            !module.getExports().isEmpty(),
                            "Module should have exports at thread "
                                + threadId
                                + " compilation "
                                + i);
                        successCount.incrementAndGet();
                      }
                    }
                  } catch (final Exception e) {
                    LOGGER.severe(
                        "Thread " + threadId + " failed: " + e.getMessage());
                    errorCount.incrementAndGet();
                    throw new RuntimeException(e);
                  }
                }));
      }

      for (final Future<?> future : futures) {
        future.get(120, TimeUnit.SECONDS);
      }

      executor.shutdown();
      assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

      LOGGER.info(
          "concurrentModuleCompilation completed: "
              + successCount.get()
              + " successes, "
              + errorCount.get()
              + " errors");

      assertEquals(
          0, errorCount.get(), "No errors should occur during concurrent module compilation");
      assertEquals(
          threadCount * compilationsPerThread,
          successCount.get(),
          "All compilations should succeed");
    }
  }

  /**
   * Inner helper class to use as AssertionError since WasmFunction.call uses checked exceptions.
   */
  private static class AssertionError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    AssertionError(final String message) {
      super(message);
    }
  }
}
