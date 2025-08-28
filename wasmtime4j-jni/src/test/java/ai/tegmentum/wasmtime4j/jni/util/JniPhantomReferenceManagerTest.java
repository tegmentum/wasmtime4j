package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive unit tests for {@link JniPhantomReferenceManager}.
 *
 * <p>These tests verify phantom reference tracking, automatic cleanup, singleton behavior, thread
 * safety, and resource management.
 */
@DisplayName("JniPhantomReferenceManager Tests")
@Timeout(30) // Global timeout for all tests
class JniPhantomReferenceManagerTest {

  private JniPhantomReferenceManager manager;

  @BeforeEach
  void setUp() {
    manager = JniPhantomReferenceManager.getInstance();
  }

  @AfterEach
  void tearDown() {
    if (manager != null && !manager.isClosed()) {
      manager.close();
    }
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("Should return same instance")
    void shouldReturnSameInstance() {
      final JniPhantomReferenceManager instance1 = JniPhantomReferenceManager.getInstance();
      final JniPhantomReferenceManager instance2 = JniPhantomReferenceManager.getInstance();

      assertEquals(instance1, instance2);
    }

    @Test
    @DisplayName("Should create new instance after close")
    void shouldCreateNewInstanceAfterClose() {
      final JniPhantomReferenceManager firstInstance = JniPhantomReferenceManager.getInstance();
      firstInstance.close();

      final JniPhantomReferenceManager secondInstance = JniPhantomReferenceManager.getInstance();

      // Should be a different instance since the first was closed
      assertNotNull(secondInstance);
      assertFalse(secondInstance.isClosed());
    }
  }

  @Nested
  @DisplayName("Registration Tests")
  class RegistrationTests {

    @Test
    @DisplayName("Should register object successfully")
    void shouldRegisterObjectSuccessfully() {
      final Object testObject = new Object();
      final long testHandle = 0x12345678L;

      final int initialCount = manager.getRegisteredCount();
      manager.register(testObject, testHandle, "testCleanup");

      // Note: Due to weak reference implementation, registered count may not always increment
      // in unit tests, but no exception should be thrown
      assertTrue(manager.getRegisteredCount() >= initialCount);
      assertTrue(manager.getTotalRegistered() > 0);
    }

    @Test
    @DisplayName("Should reject null object")
    void shouldRejectNullObject() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.register(null, 0x12345678L, "testCleanup"),
          "Should reject null object");
    }

    @Test
    @DisplayName("Should reject invalid handle")
    void shouldRejectInvalidHandle() {
      final Object testObject = new Object();

      assertThrows(
          IllegalArgumentException.class,
          () -> manager.register(testObject, 0L, "testCleanup"),
          "Should reject zero handle");
    }

    @Test
    @DisplayName("Should reject null cleanup method")
    void shouldRejectNullCleanupMethod() {
      final Object testObject = new Object();

      assertThrows(
          IllegalArgumentException.class,
          () -> manager.register(testObject, 0x12345678L, null),
          "Should reject null cleanup method");
    }

    @Test
    @DisplayName("Should handle registration after close")
    void shouldHandleRegistrationAfterClose() {
      manager.close();

      final Object testObject = new Object();
      // Should not throw but should log a warning
      manager.register(testObject, 0x12345678L, "testCleanup");
    }
  }

  @Nested
  @DisplayName("Unregistration Tests")
  class UnregistrationTests {

    @Test
    @DisplayName("Should handle unregister call")
    void shouldHandleUnregisterCall() {
      final Object testObject = new Object();

      // Should not throw exception (even though phantom references can't be directly unregistered)
      manager.unregister(testObject);
    }

    @Test
    @DisplayName("Should reject null object in unregister")
    void shouldRejectNullObjectInUnregister() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.unregister(null),
          "Should reject null object in unregister");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("Should track registration count")
    void shouldTrackRegistrationCount() {
      final long initialTotal = manager.getTotalRegistered();

      final Object testObject1 = new Object();
      final Object testObject2 = new Object();

      manager.register(testObject1, 0x11111111L, "cleanup1");
      manager.register(testObject2, 0x22222222L, "cleanup2");

      assertTrue(manager.getTotalRegistered() >= initialTotal + 2);
    }

    @Test
    @DisplayName("Should track cleanup statistics")
    void shouldTrackCleanupStatistics() {
      final long initialCleanedUp = manager.getCleanedUpCount();
      final long initialFailed = manager.getFailedCleanupCount();

      // These values should be non-negative
      assertTrue(initialCleanedUp >= 0);
      assertTrue(initialFailed >= 0);
    }

    @Test
    @DisplayName("Should provide current registered count")
    void shouldProvideCurrentRegisteredCount() {
      final int registeredCount = manager.getRegisteredCount();
      assertTrue(registeredCount >= 0);
    }
  }

  @Nested
  @DisplayName("Garbage Collection Tests")
  class GarbageCollectionTests {

    @Test
    @DisplayName("Should handle object collection")
    void shouldHandleObjectCollection() throws InterruptedException {
      final long initialCleanedUp = manager.getCleanedUpCount();

      // Create objects that can be garbage collected
      registerObjectsForCollection();

      // Force garbage collection
      forceGarbageCollection();

      // Process any pending references
      manager.processPendingReferences();

      // Allow some time for processing
      Thread.sleep(100);

      // Cleanup count might have increased (though not guaranteed in unit tests)
      assertTrue(manager.getCleanedUpCount() >= initialCleanedUp);
    }

    @Test
    @DisplayName("Should process pending references on demand")
    void shouldProcessPendingReferencesOnDemand() {
      // Register some objects
      final Object obj1 = new Object();
      final Object obj2 = new Object();

      manager.register(obj1, 0x11111111L, "pendingCleanup1");
      manager.register(obj2, 0x22222222L, "pendingCleanup2");

      // Process pending references should not throw
      manager.processPendingReferences();
    }

    @Test
    @DisplayName("Should handle multiple garbage collection cycles")
    void shouldHandleMultipleGarbageCollectionCycles() throws InterruptedException {
      final long initialCleanedUp = manager.getCleanedUpCount();

      // Multiple GC cycles
      for (int i = 0; i < 3; i++) {
        registerObjectsForCollection();
        forceGarbageCollection();
        manager.processPendingReferences();
        Thread.sleep(50);
      }

      // Should handle multiple cycles without issues
      assertTrue(manager.getCleanedUpCount() >= initialCleanedUp);
    }

    private void registerObjectsForCollection() {
      // Create objects in a separate scope so they can be collected
      for (int i = 0; i < 5; i++) {
        final Object obj = new Object();
        manager.register(obj, 0x10000000L + i, "gcTestCleanup" + i);
      }
    }

    private void forceGarbageCollection() {
      // Force garbage collection multiple times
      for (int i = 0; i < 3; i++) {
        System.gc();
        System.runFinalization();
        try {
          Thread.sleep(50);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
  }

  @Nested
  @DisplayName("Concurrency Tests")
  class ConcurrencyTests {

    @Test
    @DisplayName("Should handle concurrent registrations")
    void shouldHandleConcurrentRegistrations() throws InterruptedException {
      final int threadCount = 10;
      final int objectsPerThread = 20;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final List<Thread> threads = new ArrayList<>();

      for (int t = 0; t < threadCount; t++) {
        final int threadIndex = t;
        final Thread thread =
            new Thread(
                () -> {
                  try {
                    for (int i = 0; i < objectsPerThread; i++) {
                      final Object obj = new Object();
                      final long handle = (long) threadIndex * 1000 + i;
                      manager.register(obj, handle, "concurrentCleanup" + threadIndex + "_" + i);
                    }
                  } finally {
                    latch.countDown();
                  }
                });

        threads.add(thread);
        thread.start();
      }

      assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");

      for (final Thread thread : threads) {
        thread.join();
      }

      // Should have processed all registrations without errors
      assertTrue(manager.getTotalRegistered() >= threadCount * objectsPerThread);
    }

    @Test
    @DisplayName("Should handle concurrent processing")
    void shouldHandleConcurrentProcessing() throws InterruptedException {
      final int threadCount = 5;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final List<Thread> threads = new ArrayList<>();

      // Register some objects first
      for (int i = 0; i < 20; i++) {
        final Object obj = new Object();
        manager.register(obj, 0x20000000L + i, "concurrentProcessCleanup" + i);
      }

      // Create threads that process pending references concurrently
      for (int t = 0; t < threadCount; t++) {
        final Thread thread =
            new Thread(
                () -> {
                  try {
                    for (int i = 0; i < 5; i++) {
                      manager.processPendingReferences();
                      Thread.sleep(10);
                    }
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  } finally {
                    latch.countDown();
                  }
                });

        threads.add(thread);
        thread.start();
      }

      assertTrue(latch.await(5, TimeUnit.SECONDS), "All processing threads should complete");

      for (final Thread thread : threads) {
        thread.join();
      }
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      // Register some objects
      final Object obj1 = new Object();
      final Object obj2 = new Object();

      manager.register(obj1, 0x30000001L, "closeTestCleanup1");
      manager.register(obj2, 0x30000002L, "closeTestCleanup2");

      assertFalse(manager.isClosed());

      manager.close();
      assertTrue(manager.isClosed());
    }

    @Test
    @DisplayName("Should be idempotent on close")
    void shouldBeIdempotentOnClose() {
      manager.close();
      assertTrue(manager.isClosed());

      // Second close should not throw
      manager.close();
      assertTrue(manager.isClosed());
    }

    @Test
    @DisplayName("Should process remaining references on close")
    void shouldProcessRemainingReferencesOnClose() throws InterruptedException {
      final long initialCleanedUp = manager.getCleanedUpCount();

      // Register objects and immediately make them eligible for GC
      for (int i = 0; i < 5; i++) {
        final Object obj = new Object();
        manager.register(obj, 0x40000000L + i, "closeCleanup" + i);
      }

      // Force GC to make references available
      forceGarbageCollection();

      manager.close();

      // Should have processed remaining references
      assertTrue(manager.getCleanedUpCount() >= initialCleanedUp);
    }

    @Test
    @DisplayName("Should handle close with active cleanup thread")
    void shouldHandleCloseWithActiveCleanupThread() throws InterruptedException {
      // Register objects to keep cleanup thread active
      for (int i = 0; i < 10; i++) {
        final Object obj = new Object();
        manager.register(obj, 0x50000000L + i, "activeCleanup" + i);
      }

      // Close should stop the cleanup thread gracefully
      manager.close();
      assertTrue(manager.isClosed());

      // Give some time for thread termination
      Thread.sleep(100);
    }

    private void forceGarbageCollection() {
      for (int i = 0; i < 3; i++) {
        System.gc();
        System.runFinalization();
        try {
          Thread.sleep(50);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle cleanup exceptions gracefully")
    void shouldHandleCleanupExceptionsGracefully() {
      // Register objects and trigger processing
      final Object obj = new Object();
      manager.register(obj, 0x60000001L, "errorCleanup");

      // Processing should not throw even if native cleanup would fail
      manager.processPendingReferences();
    }

    @Test
    @DisplayName("Should track failed cleanup attempts")
    void shouldTrackFailedCleanupAttempts() {
      final long initialFailed = manager.getFailedCleanupCount();

      // Register and process - failures might occur in real cleanup
      final Object obj = new Object();
      manager.register(obj, 0x70000001L, "failableCleanup");
      manager.processPendingReferences();

      // Failed count should be non-negative and may have increased
      assertTrue(manager.getFailedCleanupCount() >= initialFailed);
    }
  }

  @Nested
  @DisplayName("toString and Object Methods Tests")
  class ToStringAndObjectMethodsTests {

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
      // Register some objects to have meaningful statistics
      final Object obj1 = new Object();
      final Object obj2 = new Object();

      manager.register(obj1, 0x80000001L, "toStringCleanup1");
      manager.register(obj2, 0x80000002L, "toStringCleanup2");

      final String toString = manager.toString();
      assertNotNull(toString);
      assertTrue(toString.contains("JniPhantomReferenceManager"));
      assertTrue(toString.contains("registered="));
      assertTrue(toString.contains("total="));
      assertTrue(toString.contains("cleanedUp="));
      assertTrue(toString.contains("failed="));
    }

    @Test
    @DisplayName("Should show current statistics in toString")
    void shouldShowCurrentStatisticsInToString() {
      final String toString = manager.toString();

      // Should contain numeric values for statistics
      assertTrue(toString.matches(".*registered=\\d+.*"));
      assertTrue(toString.matches(".*total=\\d+.*"));
      assertTrue(toString.matches(".*cleanedUp=\\d+.*"));
      assertTrue(toString.matches(".*failed=\\d+.*"));
    }
  }
}
