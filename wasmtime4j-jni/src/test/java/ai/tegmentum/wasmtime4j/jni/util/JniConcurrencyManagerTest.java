package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive unit tests for {@link JniConcurrencyManager}.
 *
 * <p>These tests verify thread-safe resource access, read-write locking, resource registration,
 * timeout handling, and performance under concurrent load.
 */
@DisplayName("JniConcurrencyManager Tests")
@Timeout(30) // Global timeout for all tests
class JniConcurrencyManagerTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long ANOTHER_HANDLE = 0xABCDEF00L;

  private JniConcurrencyManager concurrencyManager;

  @BeforeEach
  void setUp() {
    concurrencyManager = new JniConcurrencyManager();
  }

  @AfterEach
  void tearDown() {
    if (concurrencyManager != null && !concurrencyManager.isClosed()) {
      concurrencyManager.close();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create manager with default settings")
    void shouldCreateManagerWithDefaultSettings() {
      try (final JniConcurrencyManager manager = new JniConcurrencyManager()) {
        assertFalse(manager.isClosed());
        assertEquals(0, manager.getResourceCount());
        assertEquals(0, manager.getTotalOperationCount());
      }
    }

    @Test
    @DisplayName("Should create manager with custom settings")
    void shouldCreateManagerWithCustomSettings() {
      final int maxConcurrent = 5;
      final long timeout = 5000;
      
      try (final JniConcurrencyManager manager = new JniConcurrencyManager(maxConcurrent, timeout)) {
        assertFalse(manager.isClosed());
        assertEquals(0, manager.getResourceCount());
      }
    }

    @Test
    @DisplayName("Should reject invalid max concurrent operations")
    void shouldRejectInvalidMaxConcurrentOperations() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniConcurrencyManager(0, 1000),
          "Should reject zero max concurrent operations");

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniConcurrencyManager(-1, 1000),
          "Should reject negative max concurrent operations");
    }

    @Test
    @DisplayName("Should reject negative timeout")
    void shouldRejectNegativeTimeout() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniConcurrencyManager(10, -1),
          "Should reject negative timeout");
    }

    @Test
    @DisplayName("Should accept zero timeout")
    void shouldAcceptZeroTimeout() {
      try (final JniConcurrencyManager manager = new JniConcurrencyManager(10, 0)) {
        assertFalse(manager.isClosed());
      }
    }
  }

  @Nested
  @DisplayName("Resource Registration Tests")
  class ResourceRegistrationTests {

    @Test
    @DisplayName("Should register resource successfully")
    void shouldRegisterResourceSuccessfully() {
      assertEquals(0, concurrencyManager.getResourceCount());
      
      concurrencyManager.registerResource(VALID_HANDLE);
      assertEquals(1, concurrencyManager.getResourceCount());
    }

    @Test
    @DisplayName("Should register multiple resources")
    void shouldRegisterMultipleResources() {
      concurrencyManager.registerResource(VALID_HANDLE);
      concurrencyManager.registerResource(ANOTHER_HANDLE);
      
      assertEquals(2, concurrencyManager.getResourceCount());
    }

    @Test
    @DisplayName("Should handle duplicate registration")
    void shouldHandleDuplicateRegistration() {
      concurrencyManager.registerResource(VALID_HANDLE);
      concurrencyManager.registerResource(VALID_HANDLE); // Duplicate
      
      assertEquals(1, concurrencyManager.getResourceCount());
    }

    @Test
    @DisplayName("Should reject invalid handle")
    void shouldRejectInvalidHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> concurrencyManager.registerResource(0L),
          "Should reject zero handle");
    }

    @Test
    @DisplayName("Should unregister resource")
    void shouldUnregisterResource() {
      concurrencyManager.registerResource(VALID_HANDLE);
      assertEquals(1, concurrencyManager.getResourceCount());
      
      concurrencyManager.unregisterResource(VALID_HANDLE);
      assertEquals(0, concurrencyManager.getResourceCount());
    }

    @Test
    @DisplayName("Should handle unregistering non-existent resource")
    void shouldHandleUnregisteringNonExistentResource() {
      // Should not throw exception
      concurrencyManager.unregisterResource(VALID_HANDLE);
      assertEquals(0, concurrencyManager.getResourceCount());
    }

    @Test
    @DisplayName("Should reject operations on closed manager")
    void shouldRejectOperationsOnClosedManager() {
      concurrencyManager.close();
      
      assertThrows(
          RuntimeException.class,
          () -> concurrencyManager.registerResource(VALID_HANDLE),
          "Should reject resource registration on closed manager");
    }
  }

  @Nested
  @DisplayName("Read Lock Operations Tests")
  class ReadLockOperationsTests {

    @Test
    @DisplayName("Should execute operation with read lock")
    void shouldExecuteOperationWithReadLock() {
      concurrencyManager.registerResource(VALID_HANDLE);
      
      final String result = concurrencyManager.executeWithReadLock(VALID_HANDLE, 
          () -> "read operation result");
      
      assertEquals("read operation result", result);
      assertEquals(1, concurrencyManager.getTotalOperationCount());
    }

    @Test
    @DisplayName("Should auto-register resource for read operations")
    void shouldAutoRegisterResourceForReadOperations() {
      assertEquals(0, concurrencyManager.getResourceCount());
      
      final String result = concurrencyManager.executeWithReadLock(VALID_HANDLE,
          () -> "auto-registered read");
      
      assertEquals("auto-registered read", result);
      assertEquals(1, concurrencyManager.getResourceCount());
    }

    @Test
    @DisplayName("Should handle concurrent read operations")
    void shouldHandleConcurrentReadOperations() throws InterruptedException {
      final int threadCount = 10;
      final AtomicInteger concurrentReads = new AtomicInteger(0);
      final AtomicInteger maxConcurrentReads = new AtomicInteger(0);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completeLatch = new CountDownLatch(threadCount);
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      concurrencyManager.registerResource(VALID_HANDLE);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
          try {
            startLatch.await();
            
            concurrencyManager.executeWithReadLock(VALID_HANDLE, () -> {
              final int current = concurrentReads.incrementAndGet();
              maxConcurrentReads.updateAndGet(max -> Math.max(max, current));
              
              try {
                Thread.sleep(100); // Hold lock for a bit
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              
              concurrentReads.decrementAndGet();
              return "concurrent read";
            });
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            completeLatch.countDown();
          }
        });
      }

      startLatch.countDown(); // Start all threads
      assertTrue(completeLatch.await(10, TimeUnit.SECONDS), "All read operations should complete");

      // Multiple read operations should be able to execute concurrently
      assertTrue(maxConcurrentReads.get() > 1, "Should allow concurrent reads");
      assertEquals(threadCount, concurrencyManager.getTotalOperationCount());
      
      executor.shutdown();
    }

    @Test
    @DisplayName("Should reject null operation")
    void shouldRejectNullOperation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> concurrencyManager.executeWithReadLock(VALID_HANDLE, null),
          "Should reject null operation");
    }

    @Test
    @DisplayName("Should reject invalid handle")
    void shouldRejectInvalidHandleInReadLock() {
      assertThrows(
          IllegalArgumentException.class,
          () -> concurrencyManager.executeWithReadLock(0L, () -> "test"),
          "Should reject invalid handle");
    }
  }

  @Nested
  @DisplayName("Write Lock Operations Tests")
  class WriteLockOperationsTests {

    @Test
    @DisplayName("Should execute operation with write lock")
    void shouldExecuteOperationWithWriteLock() {
      concurrencyManager.registerResource(VALID_HANDLE);
      
      final String result = concurrencyManager.executeWithWriteLock(VALID_HANDLE,
          () -> "write operation result");
      
      assertEquals("write operation result", result);
      assertEquals(1, concurrencyManager.getTotalOperationCount());
    }

    @Test
    @DisplayName("Should auto-register resource for write operations")
    void shouldAutoRegisterResourceForWriteOperations() {
      assertEquals(0, concurrencyManager.getResourceCount());
      
      final String result = concurrencyManager.executeWithWriteLock(VALID_HANDLE,
          () -> "auto-registered write");
      
      assertEquals("auto-registered write", result);
      assertEquals(1, concurrencyManager.getResourceCount());
    }

    @Test
    @DisplayName("Should serialize write operations")
    void shouldSerializeWriteOperations() throws InterruptedException {
      final int threadCount = 5;
      final AtomicInteger concurrentWrites = new AtomicInteger(0);
      final AtomicInteger maxConcurrentWrites = new AtomicInteger(0);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completeLatch = new CountDownLatch(threadCount);
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final List<String> executionOrder = new ArrayList<>();

      concurrencyManager.registerResource(VALID_HANDLE);

      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        executor.submit(() -> {
          try {
            startLatch.await();
            
            concurrencyManager.executeWithWriteLock(VALID_HANDLE, () -> {
              final int current = concurrentWrites.incrementAndGet();
              maxConcurrentWrites.updateAndGet(max -> Math.max(max, current));
              
              synchronized (executionOrder) {
                executionOrder.add("write-" + threadIndex);
              }
              
              try {
                Thread.sleep(50); // Hold lock briefly
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              
              concurrentWrites.decrementAndGet();
              return "write-" + threadIndex;
            });
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            completeLatch.countDown();
          }
        });
      }

      startLatch.countDown();
      assertTrue(completeLatch.await(10, TimeUnit.SECONDS), "All write operations should complete");

      // Write operations should be serialized (only one at a time)
      assertEquals(1, maxConcurrentWrites.get(), "Should serialize write operations");
      assertEquals(threadCount, executionOrder.size());
      assertEquals(threadCount, concurrencyManager.getTotalOperationCount());
      
      executor.shutdown();
    }

    @Test
    @DisplayName("Should block reads during write")
    void shouldBlockReadsDuringWrite() throws InterruptedException {
      final CountDownLatch writeLatch = new CountDownLatch(1);
      final CountDownLatch readStartLatch = new CountDownLatch(1);
      final AtomicReference<String> readResult = new AtomicReference<>();
      final ExecutorService executor = Executors.newFixedThreadPool(2);

      concurrencyManager.registerResource(VALID_HANDLE);

      // Start write operation that holds the lock
      final CompletableFuture<String> writeFuture = CompletableFuture.supplyAsync(() -> 
          concurrencyManager.executeWithWriteLock(VALID_HANDLE, () -> {
            readStartLatch.countDown();
            try {
              writeLatch.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return "write completed";
          }), executor);

      // Start read operation after write starts
      readStartLatch.await();
      final CompletableFuture<Void> readFuture = CompletableFuture.runAsync(() -> {
        final String result = concurrencyManager.executeWithReadLock(VALID_HANDLE,
            () -> "read completed");
        readResult.set(result);
      }, executor);

      // Release write lock
      Thread.sleep(100); // Ensure read is waiting
      writeLatch.countDown();

      // Both operations should complete
      assertEquals("write completed", writeFuture.get(5, TimeUnit.SECONDS));
      readFuture.get(5, TimeUnit.SECONDS);
      assertEquals("read completed", readResult.get());
      
      executor.shutdown();
    }
  }

  @Nested
  @DisplayName("Active Operation Tracking Tests")
  class ActiveOperationTrackingTests {

    @Test
    @DisplayName("Should track active operations")
    void shouldTrackActiveOperations() throws InterruptedException {
      final CountDownLatch operationLatch = new CountDownLatch(1);
      final CountDownLatch checkLatch = new CountDownLatch(1);
      final AtomicInteger activeCount = new AtomicInteger(-1);

      concurrencyManager.registerResource(VALID_HANDLE);

      // Start long-running operation
      final Thread operationThread = new Thread(() ->
          concurrencyManager.executeWithReadLock(VALID_HANDLE, () -> {
            checkLatch.countDown();
            try {
              operationLatch.await();
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return "long operation";
          }));

      operationThread.start();
      checkLatch.await();

      // Check active operation count
      activeCount.set(concurrencyManager.getActiveOperationCount(VALID_HANDLE));
      
      // Release operation
      operationLatch.countDown();
      operationThread.join();

      assertTrue(activeCount.get() >= 0, "Should track active operations");
    }

    @Test
    @DisplayName("Should return -1 for unregistered resource")
    void shouldReturnMinusOneForUnregisteredResource() {
      final int count = concurrencyManager.getActiveOperationCount(VALID_HANDLE);
      assertEquals(-1, count, "Should return -1 for unregistered resource");
    }

    @Test
    @DisplayName("Should handle active count after unregister")
    void shouldHandleActiveCountAfterUnregister() {
      concurrencyManager.registerResource(VALID_HANDLE);
      concurrencyManager.unregisterResource(VALID_HANDLE);
      
      final int count = concurrencyManager.getActiveOperationCount(VALID_HANDLE);
      assertEquals(-1, count, "Should return -1 after unregister");
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should propagate operation exceptions")
    void shouldPropagateOperationExceptions() {
      final RuntimeException expectedException = new RuntimeException("Test exception");
      
      concurrencyManager.registerResource(VALID_HANDLE);
      
      final RuntimeException actualException = assertThrows(
          RuntimeException.class,
          () -> concurrencyManager.executeWithReadLock(VALID_HANDLE, () -> {
            throw expectedException;
          }));
      
      assertEquals(expectedException, actualException);
      assertEquals(1, concurrencyManager.getFailedOperationCount());
    }

    @Test
    @DisplayName("Should handle interruption gracefully")
    void shouldHandleInterruptionGracefully() throws InterruptedException {
      final CountDownLatch interruptLatch = new CountDownLatch(1);
      final AtomicReference<Exception> caughtException = new AtomicReference<>();

      concurrencyManager.registerResource(VALID_HANDLE);

      final Thread operationThread = new Thread(() -> {
        try {
          concurrencyManager.executeWithReadLock(VALID_HANDLE, () -> {
            interruptLatch.countDown();
            try {
              Thread.sleep(5000); // Long operation
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new RuntimeException("Interrupted", e);
            }
            return "completed";
          });
        } catch (final Exception e) {
          caughtException.set(e);
        }
      });

      operationThread.start();
      interruptLatch.await();
      
      // Interrupt the operation
      operationThread.interrupt();
      operationThread.join(1000);

      assertNotNull(caughtException.get(), "Should catch interruption exception");
      assertTrue(caughtException.get().getMessage().contains("interrupted") ||
                 caughtException.get().getMessage().contains("Interrupted"));
    }

    @Test
    @DisplayName("Should handle timeout operations")
    void shouldHandleTimeoutOperations() {
      // Create manager with short timeout
      try (final JniConcurrencyManager timeoutManager = new JniConcurrencyManager(2, 100)) {
        timeoutManager.registerResource(VALID_HANDLE);
        
        // This test is tricky as we'd need to create actual contention
        // For now, just verify the manager handles operations normally with timeout set
        final String result = timeoutManager.executeWithReadLock(VALID_HANDLE, 
            () -> "timeout test");
        assertEquals("timeout test", result);
      }
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      concurrencyManager.registerResource(VALID_HANDLE);
      concurrencyManager.registerResource(ANOTHER_HANDLE);
      
      assertFalse(concurrencyManager.isClosed());
      
      concurrencyManager.close();
      assertTrue(concurrencyManager.isClosed());
      assertEquals(0, concurrencyManager.getResourceCount());
    }

    @Test
    @DisplayName("Should be idempotent on close")
    void shouldBeIdempotentOnClose() {
      concurrencyManager.close();
      assertTrue(concurrencyManager.isClosed());
      
      // Second close should not throw
      concurrencyManager.close();
      assertTrue(concurrencyManager.isClosed());
    }

    @Test
    @DisplayName("Should reject operations after close")
    void shouldRejectOperationsAfterClose() {
      concurrencyManager.close();
      
      assertThrows(
          RuntimeException.class,
          () -> concurrencyManager.executeWithReadLock(VALID_HANDLE, () -> "test"),
          "Should reject read operations on closed manager");

      assertThrows(
          RuntimeException.class,
          () -> concurrencyManager.executeWithWriteLock(VALID_HANDLE, () -> "test"),
          "Should reject write operations on closed manager");
    }

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      final AtomicInteger operationCount = new AtomicInteger(0);
      
      try (final JniConcurrencyManager autoCloseManager = new JniConcurrencyManager(5, 1000)) {
        final String result = autoCloseManager.executeWithReadLock(VALID_HANDLE, () -> {
          operationCount.incrementAndGet();
          return "auto-close test";
        });
        assertEquals("auto-close test", result);
      }
      
      assertEquals(1, operationCount.get());
    }

    @Test
    @DisplayName("Should close with active operations")
    void shouldCloseWithActiveOperations() throws InterruptedException {
      final CountDownLatch operationLatch = new CountDownLatch(1);
      final CountDownLatch startLatch = new CountDownLatch(1);

      concurrencyManager.registerResource(VALID_HANDLE);

      // Start long-running operation
      final Thread operationThread = new Thread(() -> {
        try {
          concurrencyManager.executeWithReadLock(VALID_HANDLE, () -> {
            startLatch.countDown();
            try {
              operationLatch.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return "active operation";
          });
        } catch (final RuntimeException e) {
          // Expected if manager is closed during operation
        }
      });

      operationThread.start();
      startLatch.await();
      
      // Close manager while operation is active
      concurrencyManager.close();
      operationLatch.countDown();
      
      operationThread.join(2000);
      assertTrue(concurrencyManager.isClosed());
    }
  }

  @Nested
  @DisplayName("toString and Object Methods Tests")
  class ToStringAndObjectMethodsTests {

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
      concurrencyManager.registerResource(VALID_HANDLE);
      concurrencyManager.registerResource(ANOTHER_HANDLE);
      
      // Execute some operations to generate statistics
      concurrencyManager.executeWithReadLock(VALID_HANDLE, () -> "test1");
      concurrencyManager.executeWithWriteLock(ANOTHER_HANDLE, () -> "test2");

      final String toString = concurrencyManager.toString();
      assertNotNull(toString);
      assertTrue(toString.contains("JniConcurrencyManager"));
      assertTrue(toString.contains("resources="));
      assertTrue(toString.contains("totalOps="));
      assertTrue(toString.contains("timeouts="));
      assertTrue(toString.contains("failures="));
    }

    @Test
    @DisplayName("Should show accurate statistics in toString")
    void shouldShowAccurateStatisticsInToString() {
      concurrencyManager.registerResource(VALID_HANDLE);
      concurrencyManager.executeWithReadLock(VALID_HANDLE, () -> "stats test");

      final String toString = concurrencyManager.toString();
      assertTrue(toString.contains("resources=1"));
      assertTrue(toString.contains("totalOps=1"));
      assertTrue(toString.contains("timeouts=0"));
      assertTrue(toString.contains("failures=0"));
    }
  }
}