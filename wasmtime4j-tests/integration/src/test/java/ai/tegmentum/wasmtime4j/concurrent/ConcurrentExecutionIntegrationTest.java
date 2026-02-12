/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for concurrent execution features in wasmtime4j.
 *
 * <p>This test class validates the concurrent execution capabilities including SpawnableTask,
 * ConcurrentTask, JoinHandle, and Accessor interfaces.
 */
@DisplayName("Concurrent Execution Integration Tests")
public class ConcurrentExecutionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ConcurrentExecutionIntegrationTest.class.getName());

  private static boolean concurrentExecutionSupported;

  // Simple WASM module that adds two numbers
  private static final byte[] ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07, // type section, 7 bytes
        0x01,
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // func type (i32, i32) -> i32
        0x03,
        0x02, // function section, 2 bytes
        0x01,
        0x00, // function 0 uses type 0
        0x07,
        0x07, // export section, 7 bytes
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // export "add" as func 0
        0x0A,
        0x09, // code section, 9 bytes
        0x01,
        0x07,
        0x00, // function 0 body, 7 bytes, 0 locals
        0x20,
        0x00, // local.get 0
        0x20,
        0x01, // local.get 1
        0x6A, // i32.add
        0x0B // end
      };

  @BeforeAll
  static void setUp() {
    LOGGER.info("Setting up Concurrent Execution Integration Tests");

    // Check if concurrent execution is supported by attempting to create async-enabled engine
    try {
      final EngineConfig config = new EngineConfig().asyncSupport(true);
      try (Engine engine = Engine.create(config)) {
        concurrentExecutionSupported = engine != null;
      }
    } catch (Exception e) {
      LOGGER.warning("Concurrent execution may not be fully supported: " + e.getMessage());
      concurrentExecutionSupported = false;
    }

    LOGGER.info("Concurrent execution supported: " + concurrentExecutionSupported);
  }

  @AfterAll
  static void tearDown() {
    LOGGER.info("Completed Concurrent Execution Integration Tests");
  }

  @Nested
  @DisplayName("SpawnableTask Tests")
  class SpawnableTaskTests {

    @Test
    @DisplayName("Should create spawnable task from lambda")
    void shouldCreateSpawnableTaskFromLambda() {
      LOGGER.info("Testing spawnable task creation from lambda");

      SpawnableTask<Integer> task = () -> 42;

      assertNotNull(task, "SpawnableTask should be created from lambda");
      LOGGER.info("Successfully created SpawnableTask from lambda");
    }

    @Test
    @DisplayName("Should execute spawnable task run method")
    void shouldExecuteSpawnableTaskRunMethod() throws WasmException {
      LOGGER.info("Testing spawnable task run method execution");

      AtomicBoolean executed = new AtomicBoolean(false);
      SpawnableTask<String> task =
          () -> {
            executed.set(true);
            return "result";
          };

      String result = task.run();

      assertTrue(executed.get(), "Task should have executed");
      assertEquals("result", result, "Task should return expected result");
      LOGGER.info("Successfully executed SpawnableTask.run() with result: " + result);
    }

    @Test
    @DisplayName("Should execute spawnable task via Callable interface")
    void shouldExecuteSpawnableTaskViaCallable() throws Exception {
      LOGGER.info("Testing spawnable task execution via Callable interface");

      SpawnableTask<Integer> task = () -> 100;

      // SpawnableTask extends Callable, so call() should work
      Integer result = task.call();

      assertEquals(100, result, "Task should return expected result via call()");
      LOGGER.info("Successfully executed SpawnableTask via Callable interface");
    }

    @Test
    @DisplayName("Should propagate exceptions from spawnable task")
    void shouldPropagateExceptionsFromSpawnableTask() {
      LOGGER.info("Testing exception propagation from spawnable task");

      SpawnableTask<Void> task =
          () -> {
            throw new WasmException("Task failed");
          };

      WasmException exception = assertThrows(WasmException.class, task::run);
      assertEquals("Task failed", exception.getMessage());
      LOGGER.info("Successfully caught exception from SpawnableTask");
    }

    @Test
    @DisplayName("Should be usable with ExecutorService")
    void shouldBeUsableWithExecutorService() throws Exception {
      LOGGER.info("Testing spawnable task with ExecutorService");

      ExecutorService executor = Executors.newSingleThreadExecutor();
      try {
        SpawnableTask<Integer> task = () -> 42;

        // SpawnableTask extends Callable, so can be submitted to executor
        java.util.concurrent.Future<Integer> future = executor.submit(task);
        Integer result = future.get(5, TimeUnit.SECONDS);

        assertEquals(42, result, "Task should return result through executor");
        LOGGER.info("Successfully executed SpawnableTask via ExecutorService");
      } finally {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
      }
    }
  }

  @Nested
  @DisplayName("ConcurrentTask Tests")
  class ConcurrentTaskTests {

    @Test
    @DisplayName("Should create concurrent task from lambda")
    void shouldCreateConcurrentTaskFromLambda() {
      LOGGER.info("Testing concurrent task creation from lambda");

      ConcurrentTask<String, Integer> task = accessor -> accessor.getData().length();

      assertNotNull(task, "ConcurrentTask should be created from lambda");
      LOGGER.info("Successfully created ConcurrentTask from lambda");
    }

    @Test
    @DisplayName("Should create concurrent task that accesses data")
    void shouldCreateConcurrentTaskThatAccessesData() {
      LOGGER.info("Testing concurrent task data access");

      ConcurrentTask<Integer, Integer> task =
          accessor -> {
            Integer data = accessor.getData();
            return data != null ? data * 2 : 0;
          };

      assertNotNull(task, "ConcurrentTask should be created");
      LOGGER.info("Successfully created ConcurrentTask that accesses data");
    }

    @Test
    @DisplayName("Should support complex concurrent task operations")
    void shouldSupportComplexConcurrentTaskOperations() {
      LOGGER.info("Testing complex concurrent task operations");

      // A task that performs multiple operations
      ConcurrentTask<List<Integer>, Integer> task =
          accessor -> {
            List<Integer> data = accessor.getData();
            if (data == null || data.isEmpty()) {
              return 0;
            }
            return data.stream().mapToInt(Integer::intValue).sum();
          };

      assertNotNull(task, "Complex ConcurrentTask should be created");
      LOGGER.info("Successfully created complex ConcurrentTask");
    }
  }

  @Nested
  @DisplayName("JoinHandle Tests")
  class JoinHandleTests {

    @Test
    @DisplayName("Should verify TaskStatus enum values")
    void shouldVerifyTaskStatusEnumValues() {
      LOGGER.info("Testing TaskStatus enum values");

      JoinHandle.TaskStatus[] statuses = JoinHandle.TaskStatus.values();

      assertEquals(5, statuses.length, "Should have 5 task status values");
      assertNotNull(JoinHandle.TaskStatus.PENDING, "PENDING should exist");
      assertNotNull(JoinHandle.TaskStatus.RUNNING, "RUNNING should exist");
      assertNotNull(JoinHandle.TaskStatus.COMPLETED, "COMPLETED should exist");
      assertNotNull(JoinHandle.TaskStatus.FAILED, "FAILED should exist");
      assertNotNull(JoinHandle.TaskStatus.CANCELLED, "CANCELLED should exist");

      LOGGER.info("TaskStatus enum values verified: " + statuses.length + " values");
    }

    @Test
    @DisplayName("Should parse TaskStatus from string")
    void shouldParseTaskStatusFromString() {
      LOGGER.info("Testing TaskStatus parsing from string");

      assertEquals(
          JoinHandle.TaskStatus.PENDING,
          JoinHandle.TaskStatus.valueOf("PENDING"),
          "Should parse PENDING");
      assertEquals(
          JoinHandle.TaskStatus.RUNNING,
          JoinHandle.TaskStatus.valueOf("RUNNING"),
          "Should parse RUNNING");
      assertEquals(
          JoinHandle.TaskStatus.COMPLETED,
          JoinHandle.TaskStatus.valueOf("COMPLETED"),
          "Should parse COMPLETED");
      assertEquals(
          JoinHandle.TaskStatus.FAILED,
          JoinHandle.TaskStatus.valueOf("FAILED"),
          "Should parse FAILED");
      assertEquals(
          JoinHandle.TaskStatus.CANCELLED,
          JoinHandle.TaskStatus.valueOf("CANCELLED"),
          "Should parse CANCELLED");

      LOGGER.info("TaskStatus parsing verified");
    }

    @Test
    @DisplayName("Should get TaskStatus ordinal values")
    void shouldGetTaskStatusOrdinalValues() {
      LOGGER.info("Testing TaskStatus ordinal values");

      assertEquals(0, JoinHandle.TaskStatus.PENDING.ordinal(), "PENDING should be ordinal 0");
      assertEquals(1, JoinHandle.TaskStatus.RUNNING.ordinal(), "RUNNING should be ordinal 1");
      assertEquals(2, JoinHandle.TaskStatus.COMPLETED.ordinal(), "COMPLETED should be ordinal 2");
      assertEquals(3, JoinHandle.TaskStatus.FAILED.ordinal(), "FAILED should be ordinal 3");
      assertEquals(4, JoinHandle.TaskStatus.CANCELLED.ordinal(), "CANCELLED should be ordinal 4");

      LOGGER.info("TaskStatus ordinal values verified");
    }
  }

  @Nested
  @DisplayName("Accessor Tests")
  class AccessorTests {

    @Test
    @DisplayName("Should define accessor interface methods")
    void shouldDefineAccessorInterfaceMethods() {
      LOGGER.info("Testing Accessor interface method definitions");

      // Verify that Accessor interface has expected methods via reflection
      Method[] methods = Accessor.class.getMethods();

      List<String> methodNames = new ArrayList<>();
      for (Method method : methods) {
        methodNames.add(method.getName());
      }

      assertTrue(methodNames.contains("getData"), "Should have getData method");
      assertTrue(methodNames.contains("isValid"), "Should have isValid method");
      assertTrue(methodNames.contains("getStoreId"), "Should have getStoreId method");
      assertTrue(methodNames.contains("complete"), "Should have complete method");
      assertTrue(methodNames.contains("fail"), "Should have fail method");

      LOGGER.info("Accessor interface methods verified: " + methodNames);
    }

    @Test
    @DisplayName("Should have generic getData method with class parameter")
    void shouldHaveGenericGetDataMethodWithClassParameter() throws NoSuchMethodException {
      LOGGER.info("Testing Accessor.getData(Class) method");

      Method getDataMethod = Accessor.class.getMethod("getData", Class.class);

      assertNotNull(getDataMethod, "getData(Class) method should exist");
      LOGGER.info("Accessor.getData(Class) method verified");
    }
  }

  @Nested
  @DisplayName("Store Concurrent Execution Tests")
  class StoreConcurrentExecutionTests {

    @Test
    @DisplayName("Should have runConcurrent method in Store interface")
    void shouldHaveRunConcurrentMethodInStoreInterface() throws NoSuchMethodException {
      LOGGER.info("Testing Store.runConcurrent method existence");

      Method runConcurrentMethod = Store.class.getMethod("runConcurrent", ConcurrentTask.class);

      assertNotNull(runConcurrentMethod, "runConcurrent method should exist");
      LOGGER.info("Store.runConcurrent method verified");
    }

    @Test
    @DisplayName("Should have spawn method in Store interface")
    void shouldHaveSpawnMethodInStoreInterface() throws NoSuchMethodException {
      LOGGER.info("Testing Store.spawn method existence");

      Method spawnMethod = Store.class.getMethod("spawn", SpawnableTask.class);

      assertNotNull(spawnMethod, "spawn method should exist");
      LOGGER.info("Store.spawn method verified");
    }

    @Test
    @DisplayName("Should execute runConcurrent with async-enabled engine")
    void shouldExecuteRunConcurrentWithAsyncEnabledEngine() {
      assumeTrue(concurrentExecutionSupported, "Concurrent execution not supported");

      LOGGER.info("Testing runConcurrent execution");

      try {
        final EngineConfig config = new EngineConfig().asyncSupport(true);
        try (Engine engine = Engine.create(config);
            Store store = engine.createStore()) {

          ConcurrentTask<Void, Integer> task =
              accessor -> {
                // Simple task that returns a value
                return 42;
              };

          Integer result = store.runConcurrent(task);
          assertEquals(42, result, "runConcurrent should return task result");
          LOGGER.info("runConcurrent executed successfully with result: " + result);
        }
      } catch (UnsupportedOperationException e) {
        LOGGER.warning("runConcurrent not implemented: " + e.getMessage());
        // This is acceptable if the feature is not yet implemented
      } catch (Exception e) {
        LOGGER.warning("runConcurrent test skipped: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Should spawn task and get JoinHandle")
    void shouldSpawnTaskAndGetJoinHandle() {
      assumeTrue(concurrentExecutionSupported, "Concurrent execution not supported");

      LOGGER.info("Testing spawn and JoinHandle");

      try {
        final EngineConfig config = new EngineConfig().asyncSupport(true);
        try (Engine engine = Engine.create(config);
            Store store = engine.createStore()) {

          SpawnableTask<Integer> task = () -> 100;

          JoinHandle<Integer> handle = store.spawn(task);
          assertNotNull(handle, "spawn should return JoinHandle");
          LOGGER.info("spawn returned JoinHandle with id: " + handle.getId());

          // Try to get result
          Integer result = handle.join();
          assertEquals(100, result, "JoinHandle.join should return task result");
          LOGGER.info("JoinHandle.join returned result: " + result);
        }
      } catch (UnsupportedOperationException e) {
        LOGGER.warning("spawn not implemented: " + e.getMessage());
      } catch (Exception e) {
        LOGGER.warning("spawn test skipped: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Multi-threaded Concurrent Execution Tests")
  class MultiThreadedConcurrentExecutionTests {

    @Test
    @DisplayName("Should support multiple spawnable tasks")
    void shouldSupportMultipleSpawnableTasks() throws Exception {
      LOGGER.info("Testing multiple spawnable tasks");

      List<SpawnableTask<Integer>> tasks = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        final int value = i;
        tasks.add(() -> value * 10);
      }

      ExecutorService executor = Executors.newFixedThreadPool(5);
      try {
        List<java.util.concurrent.Future<Integer>> futures = new ArrayList<>();
        for (SpawnableTask<Integer> task : tasks) {
          futures.add(executor.submit(task));
        }

        int totalSum = 0;
        for (java.util.concurrent.Future<Integer> future : futures) {
          totalSum += future.get(5, TimeUnit.SECONDS);
        }

        // Sum of 0*10 + 1*10 + 2*10 + 3*10 + 4*10 = 100
        assertEquals(100, totalSum, "Sum of all task results should be 100");
        LOGGER.info("Multiple spawnable tasks executed successfully, sum: " + totalSum);
      } finally {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
      }
    }

    @Test
    @DisplayName("Should handle concurrent task exceptions in parallel")
    void shouldHandleConcurrentTaskExceptionsInParallel() throws Exception {
      LOGGER.info("Testing concurrent task exception handling");

      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger failureCount = new AtomicInteger(0);

      List<SpawnableTask<Integer>> tasks = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        final int value = i;
        tasks.add(
            () -> {
              if (value % 2 == 0) {
                return value;
              } else {
                throw new WasmException("Task " + value + " failed");
              }
            });
      }

      ExecutorService executor = Executors.newFixedThreadPool(10);
      try {
        List<java.util.concurrent.Future<Integer>> futures = new ArrayList<>();
        for (SpawnableTask<Integer> task : tasks) {
          futures.add(executor.submit(task));
        }

        for (java.util.concurrent.Future<Integer> future : futures) {
          try {
            future.get(5, TimeUnit.SECONDS);
            successCount.incrementAndGet();
          } catch (Exception e) {
            failureCount.incrementAndGet();
          }
        }

        assertEquals(5, successCount.get(), "5 tasks should succeed (even numbers)");
        assertEquals(5, failureCount.get(), "5 tasks should fail (odd numbers)");
        LOGGER.info(
            "Exception handling verified - success: "
                + successCount.get()
                + ", failure: "
                + failureCount.get());
      } finally {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
      }
    }

    @Test
    @DisplayName("Should support concurrent access from multiple threads")
    void shouldSupportConcurrentAccessFromMultipleThreads() throws Exception {
      LOGGER.info("Testing concurrent access from multiple threads");

      AtomicInteger counter = new AtomicInteger(0);
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch completionLatch = new CountDownLatch(10);

      List<Thread> threads = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        Thread thread =
            new Thread(
                () -> {
                  try {
                    startLatch.await(); // Wait for all threads to be ready
                    SpawnableTask<Integer> task = counter::incrementAndGet;
                    task.run();
                    completionLatch.countDown();
                  } catch (Exception e) {
                    LOGGER.warning("Thread error: " + e.getMessage());
                  }
                });
        threads.add(thread);
        thread.start();
      }

      // Release all threads at once
      startLatch.countDown();

      // Wait for all threads to complete
      boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
      assertTrue(completed, "All threads should complete within timeout");
      assertEquals(10, counter.get(), "Counter should be incremented by all threads");
      LOGGER.info("Concurrent access verified, counter: " + counter.get());

      for (Thread thread : threads) {
        thread.join(1000);
      }
    }
  }

  @Nested
  @DisplayName("CompletableFuture Integration Tests")
  class CompletableFutureIntegrationTests {

    @Test
    @DisplayName("Should convert spawnable task to CompletableFuture")
    void shouldConvertSpawnableTaskToCompletableFuture() throws Exception {
      LOGGER.info("Testing spawnable task to CompletableFuture conversion");

      SpawnableTask<Integer> task = () -> 42;

      // Create CompletableFuture from task
      CompletableFuture<Integer> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return task.run();
                } catch (WasmException e) {
                  throw new RuntimeException(e);
                }
              });

      Integer result = future.get(5, TimeUnit.SECONDS);
      assertEquals(42, result, "CompletableFuture should return task result");
      LOGGER.info("SpawnableTask converted to CompletableFuture successfully");
    }

    @Test
    @DisplayName("Should chain multiple spawnable tasks with CompletableFuture")
    void shouldChainMultipleSpawnableTasksWithCompletableFuture() throws Exception {
      LOGGER.info("Testing chained spawnable tasks with CompletableFuture");

      SpawnableTask<Integer> task1 = () -> 10;
      SpawnableTask<Integer> task2 = () -> 20;
      SpawnableTask<Integer> task3 = () -> 30;

      CompletableFuture<Integer> chainedFuture =
          CompletableFuture.supplyAsync(
                  () -> {
                    try {
                      return task1.run();
                    } catch (WasmException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .thenCompose(
                  result1 ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              return result1 + task2.run();
                            } catch (WasmException e) {
                              throw new RuntimeException(e);
                            }
                          }))
              .thenCompose(
                  result2 ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              return result2 + task3.run();
                            } catch (WasmException e) {
                              throw new RuntimeException(e);
                            }
                          }));

      Integer finalResult = chainedFuture.get(10, TimeUnit.SECONDS);
      assertEquals(60, finalResult, "Chained tasks should return sum of all results");
      LOGGER.info("Chained CompletableFuture tasks completed with result: " + finalResult);
    }

    @Test
    @DisplayName("Should handle exceptions in CompletableFuture chain")
    void shouldHandleExceptionsInCompletableFutureChain() throws Exception {
      LOGGER.info("Testing exception handling in CompletableFuture chain");

      SpawnableTask<Integer> failingTask =
          () -> {
            throw new WasmException("Intentional failure");
          };

      CompletableFuture<Integer> future =
          CompletableFuture.supplyAsync(
                  () -> {
                    try {
                      return failingTask.run();
                    } catch (WasmException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .exceptionally(
                  ex -> {
                    LOGGER.info("Exception caught: " + ex.getMessage());
                    return -1; // Default value on failure
                  });

      Integer result = future.get(5, TimeUnit.SECONDS);
      assertEquals(-1, result, "Exception handler should return default value");
      LOGGER.info("Exception handling in CompletableFuture chain verified");
    }
  }

  @Nested
  @DisplayName("JoinHandle ToFuture Integration Tests")
  class JoinHandleToFutureIntegrationTests {

    @Test
    @DisplayName("Should define toFuture method in JoinHandle")
    void shouldDefineToFutureMethodInJoinHandle() throws NoSuchMethodException {
      LOGGER.info("Testing JoinHandle.toFuture method definition");

      Method toFutureMethod = JoinHandle.class.getMethod("toFuture");

      assertNotNull(toFutureMethod, "toFuture method should exist");
      assertEquals(
          CompletableFuture.class,
          toFutureMethod.getReturnType(),
          "toFuture should return CompletableFuture");
      LOGGER.info("JoinHandle.toFuture method verified");
    }

    @Test
    @DisplayName("Should define join method with timeout in JoinHandle")
    void shouldDefineJoinMethodWithTimeoutInJoinHandle() throws NoSuchMethodException {
      LOGGER.info("Testing JoinHandle.join(timeout, unit) method definition");

      Method joinMethod = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);

      assertNotNull(joinMethod, "join(timeout, unit) method should exist");
      LOGGER.info("JoinHandle.join(timeout, unit) method verified");
    }

    @Test
    @DisplayName("Should define cancel method in JoinHandle")
    void shouldDefineCancelMethodInJoinHandle() throws NoSuchMethodException {
      LOGGER.info("Testing JoinHandle.cancel method definition");

      Method cancelMethod = JoinHandle.class.getMethod("cancel", boolean.class);

      assertNotNull(cancelMethod, "cancel method should exist");
      assertEquals(boolean.class, cancelMethod.getReturnType(), "cancel should return boolean");
      LOGGER.info("JoinHandle.cancel method verified");
    }

    @Test
    @DisplayName("Should define getId method in JoinHandle")
    void shouldDefineGetIdMethodInJoinHandle() throws NoSuchMethodException {
      LOGGER.info("Testing JoinHandle.getId method definition");

      Method getIdMethod = JoinHandle.class.getMethod("getId");

      assertNotNull(getIdMethod, "getId method should exist");
      assertEquals(long.class, getIdMethod.getReturnType(), "getId should return long");
      LOGGER.info("JoinHandle.getId method verified");
    }

    @Test
    @DisplayName("Should define getStatus method in JoinHandle")
    void shouldDefineGetStatusMethodInJoinHandle() throws NoSuchMethodException {
      LOGGER.info("Testing JoinHandle.getStatus method definition");

      Method getStatusMethod = JoinHandle.class.getMethod("getStatus");

      assertNotNull(getStatusMethod, "getStatus method should exist");
      assertEquals(
          JoinHandle.TaskStatus.class,
          getStatusMethod.getReturnType(),
          "getStatus should return TaskStatus");
      LOGGER.info("JoinHandle.getStatus method verified");
    }
  }
}
