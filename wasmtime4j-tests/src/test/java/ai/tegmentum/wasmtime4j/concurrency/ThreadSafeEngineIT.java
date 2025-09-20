package ai.tegmentum.wasmtime4j.concurrency;

import ai.tegmentum.wasmtime4j.concurrency.ThreadSafeEngine;
import ai.tegmentum.wasmtime4j.concurrency.ConcurrentModule;
import ai.tegmentum.wasmtime4j.concurrency.ThreadSafeStore;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ThreadSafeEngine concurrent operations.
 *
 * <p>This test suite validates the thread safety and concurrent execution
 * capabilities of ThreadSafeEngine implementations across both JNI and Panama
 * runtime implementations.
 */
@DisplayName("ThreadSafeEngine Concurrency Integration Tests")
@Timeout(30) // Global timeout for all tests
public final class ThreadSafeEngineIT extends BaseIntegrationTest {

  private ThreadSafeEngine threadSafeEngine;
  private ExecutorService testExecutor;

  @BeforeEach
  void setUp() throws WasmException {
    // This would be created from a factory that returns ThreadSafeEngine
    // For now, this is a placeholder that needs actual implementation
    // threadSafeEngine = WasmRuntimeFactory.createThreadSafeEngine();
    testExecutor = Executors.newFixedThreadPool(8);
  }

  @AfterEach
  void tearDown() {
    if (threadSafeEngine != null) {
      threadSafeEngine.close();
    }
    if (testExecutor != null) {
      testExecutor.shutdown();
    }
  }

  @Test
  @DisplayName("Concurrent store creation should be thread-safe")
  void testConcurrentStoreCreation() throws Exception {
    assumeThreadSafeEngineAvailable();

    final int threadCount = 10;
    final int storesPerThread = 5;
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completeLatch = new CountDownLatch(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicReference<Exception> firstException = new AtomicReference<>();

    // Create multiple threads that create stores concurrently
    for (int i = 0; i < threadCount; i++) {
      testExecutor.submit(() -> {
        try {
          startLatch.await();
          for (int j = 0; j < storesPerThread; j++) {
            try (ThreadSafeStore store = threadSafeEngine.createStore()) {
              assertNotNull(store, "Store should not be null");
              assertTrue(store.isThreadSafe(), "Store should be thread-safe");
              successCount.incrementAndGet();
            }
          }
        } catch (Exception e) {
          firstException.compareAndSet(null, e);
        } finally {
          completeLatch.countDown();
        }
      });
    }

    // Start all threads simultaneously
    startLatch.countDown();
    assertTrue(completeLatch.await(10, TimeUnit.SECONDS), "All threads should complete");

    // Verify results
    assertNull(firstException.get(), "No exceptions should occur during concurrent store creation");
    assertEquals(threadCount * storesPerThread, successCount.get(),
        "All store creation operations should succeed");
  }

  @Test
  @DisplayName("Concurrent module compilation should be thread-safe")
  void testConcurrentModuleCompilation() throws Exception {
    assumeThreadSafeEngineAvailable();

    final int threadCount = 6;
    final byte[] wasmBytes = getSimpleWasmModule();
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completeLatch = new CountDownLatch(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicReference<Exception> firstException = new AtomicReference<>();

    // Create multiple threads that compile modules concurrently
    for (int i = 0; i < threadCount; i++) {
      testExecutor.submit(() -> {
        try {
          startLatch.await();
          try (ConcurrentModule module = threadSafeEngine.compileModule(wasmBytes)) {
            assertNotNull(module, "Module should not be null");
            assertTrue(module.supportsConcurrentInstantiation(),
                "Module should support concurrent instantiation");
            successCount.incrementAndGet();
          }
        } catch (Exception e) {
          firstException.compareAndSet(null, e);
        } finally {
          completeLatch.countDown();
        }
      });
    }

    // Start all threads simultaneously
    startLatch.countDown();
    assertTrue(completeLatch.await(15, TimeUnit.SECONDS), "All threads should complete");

    // Verify results
    assertNull(firstException.get(), "No exceptions should occur during concurrent compilation");
    assertEquals(threadCount, successCount.get(), "All compilation operations should succeed");
  }

  @Test
  @DisplayName("Async module compilation should work correctly")
  void testAsyncModuleCompilation() throws Exception {
    assumeThreadSafeEngineAvailable();

    final byte[] wasmBytes = getSimpleWasmModule();
    final int moduleCount = 5;

    // Start multiple async compilations
    CompletableFuture<ConcurrentModule>[] futures = new CompletableFuture[moduleCount];
    for (int i = 0; i < moduleCount; i++) {
      futures[i] = threadSafeEngine.compileModuleAsync(wasmBytes);
    }

    // Wait for all compilations to complete
    CompletableFuture<Void> allCompilations = CompletableFuture.allOf(futures);
    allCompilations.get(10, TimeUnit.SECONDS);

    // Verify all modules were compiled successfully
    for (CompletableFuture<ConcurrentModule> future : futures) {
      assertTrue(future.isDone(), "Compilation should be complete");
      assertFalse(future.isCompletedExceptionally(), "Compilation should not have failed");

      try (ConcurrentModule module = future.get()) {
        assertNotNull(module, "Module should not be null");
        assertTrue(module.isValid(), "Module should be valid");
      }
    }
  }

  @Test
  @DisplayName("Batch module compilation should work correctly")
  void testBatchModuleCompilation() throws Exception {
    assumeThreadSafeEngineAvailable();

    final byte[] wasmBytes = getSimpleWasmModule();
    final int moduleCount = 4;
    final byte[][] wasmBytesArray = new byte[moduleCount][];
    for (int i = 0; i < moduleCount; i++) {
      wasmBytesArray[i] = wasmBytes;
    }

    // Compile modules in batch
    CompletableFuture<ConcurrentModule[]> batchFuture =
        threadSafeEngine.compileModulesBatch(wasmBytesArray);

    ConcurrentModule[] modules = batchFuture.get(15, TimeUnit.SECONDS);

    // Verify all modules were compiled
    assertNotNull(modules, "Module array should not be null");
    assertEquals(moduleCount, modules.length, "Should have compiled all modules");

    for (int i = 0; i < modules.length; i++) {
      assertNotNull(modules[i], "Module " + i + " should not be null");
      assertTrue(modules[i].isValid(), "Module " + i + " should be valid");
      modules[i].close();
    }
  }

  @Test
  @DisplayName("Concurrency configuration should be adjustable")
  void testConcurrencyConfiguration() throws Exception {
    assumeThreadSafeEngineAvailable();

    // Test initial configuration
    int initialMax = threadSafeEngine.getMaxConcurrentCompilations();
    assertTrue(initialMax > 0, "Initial max concurrent compilations should be positive");

    // Test configuration change
    int newMax = initialMax + 2;
    threadSafeEngine.setMaxConcurrentCompilations(newMax);
    assertEquals(newMax, threadSafeEngine.getMaxConcurrentCompilations(),
        "Max concurrent compilations should be updated");

    // Test validation
    assertTrue(threadSafeEngine.validateConcurrencyConfiguration(),
        "Concurrency configuration should be valid");

    // Test statistics
    var stats = threadSafeEngine.getConcurrencyStatistics();
    assertNotNull(stats, "Concurrency statistics should not be null");
  }

  @Test
  @DisplayName("Pending operations management should work correctly")
  void testPendingOperationsManagement() throws Exception {
    assumeThreadSafeEngineAvailable();

    final byte[] wasmBytes = getSimpleWasmModule();

    // Start some async compilations
    CompletableFuture<ConcurrentModule> future1 = threadSafeEngine.compileModuleAsync(wasmBytes);
    CompletableFuture<ConcurrentModule> future2 = threadSafeEngine.compileModuleAsync(wasmBytes);

    // Test awaiting pending compilations
    var awaitFuture = threadSafeEngine.awaitPendingCompilations();
    awaitFuture.get(5, TimeUnit.SECONDS);

    // Verify compilations completed
    assertTrue(future1.isDone(), "First compilation should be done");
    assertTrue(future2.isDone(), "Second compilation should be done");

    // Clean up
    try (ConcurrentModule module1 = future1.get();
         ConcurrentModule module2 = future2.get()) {
      assertNotNull(module1, "First module should not be null");
      assertNotNull(module2, "Second module should not be null");
    }
  }

  @Test
  @DisplayName("Concurrent execution context should work correctly")
  void testConcurrentExecutionContext() throws Exception {
    assumeThreadSafeEngineAvailable();

    var context = threadSafeEngine.createConcurrentContext();
    assertNotNull(context, "Concurrent execution context should not be null");

    // Test context operations (implementation would depend on actual context API)
    // This is a placeholder for when the context is fully implemented
  }

  private void assumeThreadSafeEngineAvailable() {
    // This method would check if ThreadSafeEngine is available
    // For now, skip these tests until the implementation is complete
    org.junit.jupiter.api.Assumptions.assumeTrue(false,
        "ThreadSafeEngine implementation not yet available");
  }

  private byte[] getSimpleWasmModule() {
    // Return a simple WASM module for testing
    // This is a minimal WASM module that does nothing but is valid
    return new byte[]{
        0x00, 0x61, 0x73, 0x6d, // WASM magic
        0x01, 0x00, 0x00, 0x00  // WASM version
    };
  }
}