package ai.tegmentum.wasmtime4j.threading;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmThread;
import ai.tegmentum.wasmtime4j.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.WasmThreadState;
import ai.tegmentum.wasmtime4j.WasmThreadStatistics;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive test suite for WebAssembly threading functionality.
 *
 * <p>This test class validates all aspects of WebAssembly threading support including:
 *
 * <ul>
 *   <li>Thread creation and lifecycle management
 *   <li>Thread-local storage operations
 *   <li>Shared memory access and atomic operations
 *   <li>Thread synchronization and coordination
 *   <li>Performance characteristics and resource management
 *   <li>Error handling and edge cases
 * </ul>
 *
 * <p>Tests are designed to be comprehensive, robust, and reveal flaws in the implementation. Each
 * test includes detailed validation and produces verbose output for debugging purposes.
 *
 * @since 1.0.0
 */
@Execution(ExecutionMode.CONCURRENT)
public class ComprehensiveThreadingTest {

  /** WebAssembly runtime for testing. */
  private WasmRuntime runtime;

  /** WebAssembly engine for testing. */
  private Engine engine;

  /** WebAssembly store for testing. */
  private Store store;

  /** Simple WebAssembly module for testing. */
  private Module module;

  /** WebAssembly instance for testing. */
  private Instance instance;

  /** Shared memory for threading tests. */
  private WasmMemory sharedMemory;

  /** Thread pool for concurrent testing. */
  private ExecutorService executorService;

  /** Simple WebAssembly module bytecode for testing. */
  private static final byte[] SIMPLE_WASM_MODULE = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic
    0x01,
    0x00,
    0x00,
    0x00, // Version
    // Type section
    0x01,
    0x07,
    0x01,
    0x60,
    0x02,
    0x7f,
    0x7f,
    0x01,
    0x7f, // Function type (i32, i32) -> i32
    // Function section
    0x03,
    0x02,
    0x01,
    0x00, // One function of type 0
    // Memory section
    0x05,
    0x04,
    0x01,
    0x01,
    0x01,
    0x02, // Shared memory, min=1, max=2 pages
    // Export section
    0x07,
    0x0b,
    0x02,
    0x03,
    0x61,
    0x64,
    0x64,
    0x00,
    0x00, // Export "add" function
    0x06,
    0x6d,
    0x65,
    0x6d,
    0x6f,
    0x72,
    0x79,
    0x02,
    0x00, // Export "memory"
    // Code section
    0x0a,
    0x09,
    0x01,
    0x07,
    0x00,
    0x20,
    0x00,
    0x20,
    0x01,
    0x6a,
    0x0b // add function: local.get 0, local.get 1, i32.add
  };

  @BeforeEach
  void setUp(final TestInfo testInfo) throws WasmException {
    System.out.printf("Setting up test: %s%n", testInfo.getDisplayName());

    runtime = WasmRuntimeFactory.createRuntime();
    engine = runtime.createEngine();
    store = runtime.createStore(engine);
    module = runtime.compileModule(engine, SIMPLE_WASM_MODULE);
    instance = runtime.instantiateModule(store, module);

    // Get shared memory for threading tests
    sharedMemory =
        instance
            .getMemory("memory")
            .orElseThrow(() -> new WasmException("Memory export not found"));

    assertTrue(sharedMemory.isShared(), "Memory should be shared for threading tests");

    executorService = Executors.newFixedThreadPool(8);

    System.out.printf("Setup completed for test: %s%n", testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    System.out.printf("Tearing down test: %s%n", testInfo.getDisplayName());

    if (executorService != null) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    try {
      if (instance != null) {
        instance.close();
      }
      if (module != null) {
        module.close();
      }
      if (store != null) {
        store.close();
      }
      if (engine != null) {
        engine.close();
      }
      if (runtime != null) {
        runtime.close();
      }
    } catch (final Exception e) {
      System.err.println("Error during cleanup: " + e.getMessage());
    }

    System.out.printf("Teardown completed for test: %s%n", testInfo.getDisplayName());
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testThreadCreationAndBasicLifecycle() throws Exception {
    System.out.println("Testing thread creation and basic lifecycle...");

    // Create a WebAssembly thread
    final WasmThread thread = runtime.createThread(sharedMemory);
    assertNotNull(thread, "Thread should not be null");

    System.out.printf("Created thread with ID: %d%n", thread.getThreadId());
    assertTrue(thread.getThreadId() > 0, "Thread ID should be positive");

    // Check initial state
    assertEquals(WasmThreadState.NEW, thread.getState(), "Initial thread state should be NEW");
    assertTrue(thread.isAlive(), "Thread should be alive after creation");
    assertFalse(thread.isTerminationRequested(), "Termination should not be requested initially");

    // Test statistics
    final WasmThreadStatistics initialStats = thread.getStatistics();
    assertNotNull(initialStats, "Initial statistics should not be null");
    assertEquals(
        0, initialStats.getFunctionsExecuted(), "No functions should be executed initially");

    // Test shared memory access
    final WasmMemory threadSharedMemory = thread.getSharedMemory();
    assertNotNull(threadSharedMemory, "Thread should have shared memory");
    assertSame(sharedMemory, threadSharedMemory, "Thread should reference the same shared memory");

    // Test thread-local storage
    final WasmThreadLocalStorage localStorage = thread.getThreadLocalStorage();
    assertNotNull(localStorage, "Thread should have local storage");

    // Test thread termination
    System.out.println("Requesting thread termination...");
    final CompletableFuture<Void> termination = thread.terminate();
    assertNotNull(termination, "Termination future should not be null");

    termination.get(10, TimeUnit.SECONDS);
    System.out.println("Thread termination completed");

    // Verify final state
    assertFalse(thread.isAlive(), "Thread should not be alive after termination");

    // Clean up
    thread.close();
    System.out.println("Thread creation and lifecycle test completed successfully");
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testThreadLocalStorageOperations() throws Exception {
    System.out.println("Testing thread-local storage operations...");

    final WasmThread thread = runtime.createThread(sharedMemory);
    final WasmThreadLocalStorage localStorage = thread.getThreadLocalStorage();

    // Test integer storage
    System.out.println("Testing integer storage...");
    localStorage.putInt("test_int", 42);
    assertEquals(42, localStorage.getInt("test_int"), "Integer value should match");
    assertTrue(localStorage.contains("test_int"), "Key should exist");

    // Test long storage
    System.out.println("Testing long storage...");
    localStorage.putLong("test_long", 12345678901234L);
    assertEquals(12345678901234L, localStorage.getLong("test_long"), "Long value should match");

    // Test float storage
    System.out.println("Testing float storage...");
    localStorage.putFloat("test_float", 3.14159f);
    assertEquals(
        3.14159f, localStorage.getFloat("test_float"), 0.00001f, "Float value should match");

    // Test double storage
    System.out.println("Testing double storage...");
    localStorage.putDouble("test_double", 2.718281828459045);
    assertEquals(
        2.718281828459045,
        localStorage.getDouble("test_double"),
        0.000000000000001,
        "Double value should match");

    // Test byte array storage
    System.out.println("Testing byte array storage...");
    final byte[] testBytes = {1, 2, 3, 4, 5};
    localStorage.putBytes("test_bytes", testBytes);
    final byte[] retrievedBytes = localStorage.getBytes("test_bytes");
    assertArrayEquals(testBytes, retrievedBytes, "Byte arrays should match");

    // Test string storage
    System.out.println("Testing string storage...");
    final String testString = "Hello, WebAssembly Threading!";
    localStorage.putString("test_string", testString);
    assertEquals(testString, localStorage.getString("test_string"), "String values should match");

    // Test storage size and memory usage
    System.out.printf("Storage size: %d entries%n", localStorage.size());
    assertTrue(localStorage.size() >= 6, "Storage should contain at least 6 entries");

    final long memoryUsage = localStorage.getMemoryUsage();
    System.out.printf("Memory usage: %d bytes%n", memoryUsage);
    assertTrue(memoryUsage > 0, "Memory usage should be positive");

    // Test key removal
    System.out.println("Testing key removal...");
    assertTrue(localStorage.remove("test_int"), "Removal should succeed for existing key");
    assertFalse(localStorage.contains("test_int"), "Key should no longer exist");
    assertEquals(0, localStorage.getInt("test_int"), "Removed key should return default value");

    // Test storage clearing
    System.out.println("Testing storage clearing...");
    localStorage.clear();
    assertEquals(0, localStorage.size(), "Storage should be empty after clearing");
    assertFalse(localStorage.contains("test_string"), "No keys should exist after clearing");

    // Clean up
    thread.close();
    System.out.println("Thread-local storage operations test completed successfully");
  }

  @Test
  @Timeout(value = 45, unit = TimeUnit.SECONDS)
  void testConcurrentThreadExecution() throws Exception {
    System.out.println("Testing concurrent thread execution...");

    final int threadCount = 8;
    final int operationsPerThread = 100;
    final List<WasmThread> threads = new ArrayList<>();
    final List<Future<Integer>> futures = new ArrayList<>();
    final AtomicInteger totalOperations = new AtomicInteger(0);

    // Get the add function for testing
    final WasmFunction addFunction =
        instance.getFunction("add").orElseThrow(() -> new WasmException("Add function not found"));

    try {
      // Create and start multiple threads
      System.out.printf("Creating %d threads for concurrent execution...%n", threadCount);
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);

        final int threadIndex = i;
        final Future<Integer> future =
            thread
                .executeFunction(
                    addFunction, WasmValue.i32(threadIndex), WasmValue.i32(operationsPerThread))
                .thenApply(
                    values -> {
                      totalOperations.incrementAndGet();
                      final int result = values[0].asInt();
                      System.out.printf(
                          "Thread %d completed with result: %d%n", threadIndex, result);
                      return result;
                    });

        futures.add(future);
      }

      // Wait for all threads to complete
      System.out.println("Waiting for all threads to complete...");
      int totalResult = 0;
      for (int i = 0; i < futures.size(); i++) {
        final int result = futures.get(i).get(30, TimeUnit.SECONDS);
        totalResult += result;
        System.out.printf("Thread %d result: %d%n", i, result);
      }

      System.out.printf("Total operations completed: %d%n", totalOperations.get());
      System.out.printf("Sum of all results: %d%n", totalResult);

      // Verify all operations completed
      assertEquals(threadCount, totalOperations.get(), "All threads should have completed");

      // Verify results are as expected (each thread adds its index to operationsPerThread)
      int expectedTotal = 0;
      for (int i = 0; i < threadCount; i++) {
        expectedTotal += i + operationsPerThread;
      }
      assertEquals(expectedTotal, totalResult, "Total result should match expected sum");

      // Check thread statistics
      System.out.println("Checking thread statistics...");
      for (int i = 0; i < threads.size(); i++) {
        final WasmThread thread = threads.get(i);
        final WasmThreadStatistics stats = thread.getStatistics();
        System.out.printf("Thread %d stats: %s%n", i, stats);

        assertTrue(stats.getFunctionsExecuted() > 0, "Thread should have executed functions");
        assertTrue(stats.getTotalExecutionTime() > 0, "Thread should have execution time");
      }

    } finally {
      // Clean up all threads
      System.out.println("Cleaning up threads...");
      for (final WasmThread thread : threads) {
        try {
          thread.close();
        } catch (final Exception e) {
          System.err.printf("Error closing thread: %s%n", e.getMessage());
        }
      }
    }

    System.out.println("Concurrent thread execution test completed successfully");
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testSharedMemoryAccess() throws Exception {
    System.out.println("Testing shared memory access across threads...");

    final int threadCount = 4;
    final List<WasmThread> threads = new ArrayList<>();
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completionLatch = new CountDownLatch(threadCount);

    try {
      // Create threads that will access shared memory
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);

        final int threadIndex = i;
        final int writeOffset = threadIndex * 1024; // Each thread writes to different offset

        thread.executeOperation(
            () -> {
              try {
                // Wait for all threads to be ready
                startLatch.await();

                final WasmMemory memory = thread.getSharedMemory();
                System.out.printf(
                    "Thread %d accessing shared memory at offset %d%n", threadIndex, writeOffset);

                // Write thread-specific data to memory
                final byte[] data = new byte[256];
                for (int j = 0; j < data.length; j++) {
                  data[j] = (byte) ((threadIndex * 10) + (j % 10));
                }

                memory.writeBytes(writeOffset, data, 0, data.length);
                System.out.printf(
                    "Thread %d wrote %d bytes to shared memory%n", threadIndex, data.length);

                // Read back and verify
                final byte[] readData = new byte[256];
                memory.readBytes(writeOffset, readData, 0, readData.length);

                boolean dataMatches = true;
                for (int j = 0; j < data.length; j++) {
                  if (data[j] != readData[j]) {
                    dataMatches = false;
                    break;
                  }
                }

                if (dataMatches) {
                  System.out.printf(
                      "Thread %d successfully verified shared memory data%n", threadIndex);
                } else {
                  System.err.printf("Thread %d data verification failed!%n", threadIndex);
                  throw new RuntimeException("Shared memory data verification failed");
                }

                return true;

              } catch (final Exception e) {
                System.err.printf("Thread %d error: %s%n", threadIndex, e.getMessage());
                throw new RuntimeException(e);
              } finally {
                completionLatch.countDown();
              }
            });
      }

      // Start all threads simultaneously
      System.out.println("Starting all threads for shared memory access...");
      startLatch.countDown();

      // Wait for all threads to complete
      assertTrue(
          completionLatch.await(20, TimeUnit.SECONDS),
          "All threads should complete within timeout");

      // Verify that different threads wrote to different parts of memory
      System.out.println("Verifying shared memory contents...");
      for (int i = 0; i < threadCount; i++) {
        final int readOffset = i * 1024;
        final byte[] verifyData = new byte[256];
        sharedMemory.readBytes(readOffset, verifyData, 0, verifyData.length);

        // Check that data matches what thread i should have written
        boolean dataCorrect = true;
        for (int j = 0; j < verifyData.length; j++) {
          final byte expectedByte = (byte) ((i * 10) + (j % 10));
          if (verifyData[j] != expectedByte) {
            dataCorrect = false;
            System.err.printf(
                "Data mismatch at offset %d: expected %d, got %d%n",
                readOffset + j, expectedByte, verifyData[j]);
            break;
          }
        }

        assertTrue(
            dataCorrect, String.format("Thread %d data should be correct in shared memory", i));
        System.out.printf("Thread %d data verified correctly in shared memory%n", i);
      }

    } finally {
      // Clean up all threads
      for (final WasmThread thread : threads) {
        try {
          thread.close();
        } catch (final Exception e) {
          System.err.printf("Error closing thread: %s%n", e.getMessage());
        }
      }
    }

    System.out.println("Shared memory access test completed successfully");
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testThreadTerminationAndJoin() throws Exception {
    System.out.println("Testing thread termination and join operations...");

    // Test graceful termination
    System.out.println("Testing graceful termination...");
    final WasmThread gracefulThread = runtime.createThread(sharedMemory);

    final CompletableFuture<Void> termination = gracefulThread.terminate();
    assertNotNull(termination, "Termination future should not be null");

    termination.get(10, TimeUnit.SECONDS);
    assertFalse(gracefulThread.isAlive(), "Thread should not be alive after graceful termination");

    gracefulThread.close();

    // Test join with timeout
    System.out.println("Testing join with timeout...");
    final WasmThread timedThread = runtime.createThread(sharedMemory);

    // Start a long-running operation
    final AtomicLong operationCounter = new AtomicLong(0);
    timedThread.executeOperation(
        () -> {
          try {
            for (int i = 0; i < 1000 && !timedThread.isTerminationRequested(); i++) {
              Thread.sleep(1);
              operationCounter.incrementAndGet();
            }
            return operationCounter.get();
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return operationCounter.get();
          }
        });

    // Wait briefly to let operation start
    Thread.sleep(100);

    // Request termination and join with timeout
    timedThread.terminate();

    final boolean joinedSuccessfully = timedThread.join(5000); // 5 second timeout
    assertTrue(joinedSuccessfully, "Thread should join successfully within timeout");
    assertFalse(timedThread.isAlive(), "Thread should not be alive after join");

    System.out.printf(
        "Operation completed %d iterations before termination%n", operationCounter.get());
    assertTrue(operationCounter.get() > 0, "Operation should have completed some iterations");

    timedThread.close();

    // Test force termination
    System.out.println("Testing force termination...");
    final WasmThread forceThread = runtime.createThread(sharedMemory);

    // Start an operation that might not respond to termination requests
    forceThread.executeOperation(
        () -> {
          try {
            Thread.sleep(30000); // Long sleep
            return "completed";
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted";
          }
        });

    // Wait briefly to let operation start
    Thread.sleep(100);

    // Force terminate
    forceThread.forceTerminate();
    assertEquals(
        WasmThreadState.KILLED,
        forceThread.getState(),
        "Thread state should be KILLED after force termination");

    forceThread.close();

    System.out.println("Thread termination and join test completed successfully");
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testThreadStatistics() throws Exception {
    System.out.println("Testing thread statistics collection...");

    final WasmThread thread = runtime.createThread(sharedMemory);
    final WasmFunction addFunction =
        instance.getFunction("add").orElseThrow(() -> new WasmException("Add function not found"));

    try {
      // Get initial statistics
      WasmThreadStatistics stats = thread.getStatistics();
      assertEquals(0, stats.getFunctionsExecuted(), "No functions should be executed initially");
      assertEquals(0, stats.getTotalExecutionTime(), "No execution time initially");

      // Execute some functions
      final int functionCallCount = 10;
      for (int i = 0; i < functionCallCount; i++) {
        final Future<WasmValue[]> result =
            thread.executeFunction(addFunction, WasmValue.i32(i), WasmValue.i32(i * 2));

        final WasmValue[] values = result.get(5, TimeUnit.SECONDS);
        assertEquals(i + (i * 2), values[0].asInt(), "Function result should be correct");

        System.out.printf("Function call %d completed with result: %d%n", i, values[0].asInt());
      }

      // Check updated statistics
      stats = thread.getStatistics();
      System.out.printf("Final statistics: %s%n", stats);

      assertTrue(
          stats.getFunctionsExecuted() >= functionCallCount,
          "Function execution count should be at least " + functionCallCount);
      assertTrue(stats.getTotalExecutionTime() > 0, "Total execution time should be positive");
      assertTrue(stats.getAverageExecutionTime() > 0, "Average execution time should be positive");

      // Test statistics methods
      assertTrue(
          stats.getTotalExecutionTimeMillis() >= 0,
          "Execution time in millis should be non-negative");
      assertTrue(stats.getOperationsPerSecond() > 0, "Operations per second should be positive");

      System.out.printf(
          "Average execution time per function: %d nanoseconds%n", stats.getAverageExecutionTime());
      System.out.printf("Operations per second: %.2f%n", stats.getOperationsPerSecond());

    } finally {
      thread.close();
    }

    System.out.println("Thread statistics test completed successfully");
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testThreadErrorHandling() throws Exception {
    System.out.println("Testing thread error handling...");

    final WasmThread thread = runtime.createThread(sharedMemory);
    final WasmThreadLocalStorage localStorage = thread.getThreadLocalStorage();

    try {
      // Test null key validation
      System.out.println("Testing null key validation...");
      assertThrows(
          IllegalArgumentException.class,
          () -> localStorage.putInt(null, 42),
          "Null key should throw IllegalArgumentException");

      assertThrows(
          IllegalArgumentException.class,
          () -> localStorage.putString("", "test"),
          "Empty key should throw IllegalArgumentException");

      // Test null value validation
      System.out.println("Testing null value validation...");
      assertThrows(
          IllegalArgumentException.class,
          () -> localStorage.putBytes("test", null),
          "Null byte array should throw IllegalArgumentException");

      assertThrows(
          IllegalArgumentException.class,
          () -> localStorage.putString("test", null),
          "Null string should throw IllegalArgumentException");

      // Test accessing non-existent keys
      System.out.println("Testing non-existent key access...");
      assertEquals(0, localStorage.getInt("non_existent"), "Non-existent int should return 0");
      assertEquals(0L, localStorage.getLong("non_existent"), "Non-existent long should return 0L");
      assertEquals(
          0.0f, localStorage.getFloat("non_existent"), "Non-existent float should return 0.0f");
      assertEquals(
          0.0, localStorage.getDouble("non_existent"), "Non-existent double should return 0.0");
      assertNull(localStorage.getBytes("non_existent"), "Non-existent bytes should return null");
      assertNull(localStorage.getString("non_existent"), "Non-existent string should return null");

      // Test invalid function execution
      System.out.println("Testing invalid function execution...");
      assertThrows(
          IllegalArgumentException.class,
          () -> thread.executeFunction(null, WasmValue.i32(1)),
          "Null function should throw IllegalArgumentException");

      assertThrows(
          IllegalArgumentException.class,
          () -> thread.executeOperation(null),
          "Null operation should throw IllegalArgumentException");

      // Test operations on closed thread
      System.out.println("Testing operations on closed thread...");
      thread.close();

      assertThrows(
          IllegalStateException.class,
          () -> thread.getState(),
          "Getting state of closed thread should throw IllegalStateException");

      assertThrows(
          IllegalStateException.class,
          () -> localStorage.putInt("test", 42),
          "Accessing closed thread local storage should throw IllegalStateException");

    } catch (final Exception e) {
      // Clean up in case test thread wasn't closed
      try {
        thread.close();
      } catch (final Exception ignored) {
        // Ignore cleanup errors
      }
      throw e;
    }

    System.out.println("Thread error handling test completed successfully");
  }

  @Test
  @Timeout(value = 45, unit = TimeUnit.SECONDS)
  void testThreadResourceManagement() throws Exception {
    System.out.println("Testing thread resource management...");

    final int threadCount = 20;
    final List<WasmThread> threads = new ArrayList<>();

    try {
      // Create many threads to test resource management
      System.out.printf("Creating %d threads to test resource management...%n", threadCount);
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);

        // Add some data to thread-local storage
        final WasmThreadLocalStorage localStorage = thread.getThreadLocalStorage();
        localStorage.putInt("thread_id", i);
        localStorage.putString("thread_name", "test_thread_" + i);
        localStorage.putBytes("thread_data", new byte[1024]); // 1KB of data per thread

        System.out.printf(
            "Created thread %d with %d bytes of local storage%n", i, localStorage.getMemoryUsage());
      }

      // Check that all threads are alive and have data
      int aliveThreads = 0;
      long totalMemoryUsage = 0;
      for (int i = 0; i < threads.size(); i++) {
        final WasmThread thread = threads.get(i);
        if (thread.isAlive()) {
          aliveThreads++;
        }

        final WasmThreadLocalStorage localStorage = thread.getThreadLocalStorage();
        assertEquals(
            i, localStorage.getInt("thread_id"), "Thread ID should match in local storage");

        totalMemoryUsage += localStorage.getMemoryUsage();
      }

      System.out.printf(
          "Alive threads: %d, Total memory usage: %d bytes%n", aliveThreads, totalMemoryUsage);
      assertEquals(threadCount, aliveThreads, "All threads should be alive");
      assertTrue(totalMemoryUsage > 0, "Total memory usage should be positive");

      // Test gradual cleanup
      System.out.println("Testing gradual thread cleanup...");
      for (int i = 0; i < threads.size(); i += 2) { // Close every other thread
        final WasmThread thread = threads.get(i);
        thread.close();
        System.out.printf("Closed thread %d%n", i);

        // Verify thread is no longer alive
        assertFalse(thread.isAlive(), "Thread should not be alive after close");
      }

      // Check remaining threads are still functional
      System.out.println("Verifying remaining threads are still functional...");
      for (int i = 1; i < threads.size(); i += 2) { // Check remaining threads
        final WasmThread thread = threads.get(i);
        assertTrue(thread.isAlive(), "Remaining thread should still be alive");

        final WasmThreadLocalStorage localStorage = thread.getThreadLocalStorage();
        assertEquals(
            i, localStorage.getInt("thread_id"), "Thread local storage should still be accessible");
      }

    } finally {
      // Clean up all remaining threads
      System.out.println("Cleaning up all remaining threads...");
      for (int i = 0; i < threads.size(); i++) {
        final WasmThread thread = threads.get(i);
        try {
          thread.close();
        } catch (final Exception e) {
          System.err.printf("Error closing thread %d: %s%n", i, e.getMessage());
        }
      }
    }

    // Force garbage collection and verify cleanup
    System.gc();
    Thread.sleep(100);

    System.out.println("Thread resource management test completed successfully");
  }
}
