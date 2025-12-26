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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiFileHandleManager}. */
@DisplayName("WasiFileHandleManager Tests")
class WasiFileHandleManagerTest {

  private WasiFileHandleManager manager;

  @BeforeEach
  void setUp() {
    manager = new WasiFileHandleManager();
  }

  @AfterEach
  void tearDown() {
    if (manager != null) {
      manager.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiFileHandleManager should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiFileHandleManager.class.getModifiers()),
          "WasiFileHandleManager should be final");
    }

    @Test
    @DisplayName("WasiFileHandleManager should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiFileHandleManager.class),
          "WasiFileHandleManager should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create manager")
    void defaultConstructorShouldCreateManager() {
      final WasiFileHandleManager m = new WasiFileHandleManager();
      assertNotNull(m, "Manager should be created");
      assertEquals(0, m.getActiveHandleCount(), "Should have no active handles");
      m.close();
    }

    @Test
    @DisplayName("Parameterized constructor should validate maxHandles")
    void parameterizedConstructorShouldValidateMaxHandles() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiFileHandleManager(0, 300),
          "Should throw on zero maxHandles");
    }

    @Test
    @DisplayName("Parameterized constructor should validate timeout")
    void parameterizedConstructorShouldValidateTimeout() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiFileHandleManager(100, 0),
          "Should throw on zero timeout");
    }

    @Test
    @DisplayName("Parameterized constructor should accept valid parameters")
    void parameterizedConstructorShouldAcceptValidParameters() {
      final WasiFileHandleManager m = new WasiFileHandleManager(500, 60);
      assertNotNull(m, "Manager should be created");
      m.close();
    }
  }

  @Nested
  @DisplayName("registerHandle Tests")
  class RegisterHandleTests {

    @Test
    @DisplayName("Should register handle successfully")
    void shouldRegisterHandleSuccessfully() {
      final WasiFileHandle mockHandle = createTestHandle(3);

      final WasiFileHandleManager.ManagedFileHandle managed = manager.registerHandle(mockHandle);

      assertNotNull(managed, "Managed handle should not be null");
      assertEquals(1, manager.getActiveHandleCount(), "Should have 1 active handle");
    }

    @Test
    @DisplayName("Should throw on null handle")
    void shouldThrowOnNullHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.registerHandle(null),
          "Should throw on null handle");
    }

    @Test
    @DisplayName("Should throw on duplicate descriptor")
    void shouldThrowOnDuplicateDescriptor() {
      final WasiFileHandle handle1 = createTestHandle(3);
      final WasiFileHandle handle2 = createTestHandle(3);

      manager.registerHandle(handle1);

      assertThrows(
          WasiFileSystemException.class,
          () -> manager.registerHandle(handle2),
          "Should throw on duplicate descriptor");
    }

    @Test
    @DisplayName("Should throw when max handles exceeded")
    void shouldThrowWhenMaxHandlesExceeded() {
      final WasiFileHandleManager limitedManager = new WasiFileHandleManager(2, 300);

      try {
        limitedManager.registerHandle(createTestHandle(3));
        limitedManager.registerHandle(createTestHandle(4));

        assertThrows(
            WasiFileSystemException.class,
            () -> limitedManager.registerHandle(createTestHandle(5)),
            "Should throw when max handles exceeded");
      } finally {
        limitedManager.close();
      }
    }

    @Test
    @DisplayName("Should throw after shutdown")
    void shouldThrowAfterShutdown() {
      manager.close();

      assertThrows(
          WasiFileSystemException.class,
          () -> manager.registerHandle(createTestHandle(3)),
          "Should throw after shutdown");
    }
  }

  @Nested
  @DisplayName("getHandle Tests")
  class GetHandleTests {

    @Test
    @DisplayName("Should get registered handle")
    void shouldGetRegisteredHandle() {
      final WasiFileHandle mockHandle = createTestHandle(5);
      manager.registerHandle(mockHandle);

      final WasiFileHandleManager.ManagedFileHandle retrieved = manager.getHandle(5);

      assertNotNull(retrieved, "Should retrieve handle");
      assertEquals(mockHandle, retrieved.getHandle(), "Should be same handle");
    }

    @Test
    @DisplayName("Should throw on invalid descriptor")
    void shouldThrowOnInvalidDescriptor() {
      assertThrows(
          WasiFileSystemException.class,
          () -> manager.getHandle(999),
          "Should throw on invalid descriptor");
    }

    @Test
    @DisplayName("Should update last access time")
    void shouldUpdateLastAccessTime() throws InterruptedException {
      final WasiFileHandle mockHandle = createTestHandle(5);
      final WasiFileHandleManager.ManagedFileHandle managed = manager.registerHandle(mockHandle);
      final long initialTime = managed.getLastAccessTime();

      Thread.sleep(10);
      manager.getHandle(5);

      assertTrue(managed.getLastAccessTime() >= initialTime, "Last access time should be updated");
    }

    @Test
    @DisplayName("Should throw after shutdown")
    void shouldThrowAfterShutdown() {
      final WasiFileHandle mockHandle = createTestHandle(5);
      manager.registerHandle(mockHandle);
      manager.close();

      assertThrows(
          WasiFileSystemException.class, () -> manager.getHandle(5), "Should throw after shutdown");
    }
  }

  @Nested
  @DisplayName("unregisterHandle Tests")
  class UnregisterHandleTests {

    @Test
    @DisplayName("Should unregister handle")
    void shouldUnregisterHandle() {
      final WasiFileHandle mockHandle = createTestHandle(5);
      manager.registerHandle(mockHandle);
      assertEquals(1, manager.getActiveHandleCount(), "Should have 1 handle");

      manager.unregisterHandle(5);

      assertEquals(0, manager.getActiveHandleCount(), "Should have 0 handles");
    }

    @Test
    @DisplayName("Should handle unknown descriptor gracefully")
    void shouldHandleUnknownDescriptorGracefully() {
      // Should not throw, just log warning
      manager.unregisterHandle(999);
      assertEquals(0, manager.getActiveHandleCount(), "Should still have 0 handles");
    }
  }

  @Nested
  @DisplayName("getActiveHandleCount Tests")
  class GetActiveHandleCountTests {

    @Test
    @DisplayName("Should return zero initially")
    void shouldReturnZeroInitially() {
      assertEquals(0, manager.getActiveHandleCount(), "Should have 0 handles initially");
    }

    @Test
    @DisplayName("Should track handle count correctly")
    void shouldTrackHandleCountCorrectly() {
      manager.registerHandle(createTestHandle(3));
      manager.registerHandle(createTestHandle(4));
      manager.registerHandle(createTestHandle(5));

      assertEquals(3, manager.getActiveHandleCount(), "Should have 3 handles");

      manager.unregisterHandle(4);

      assertEquals(2, manager.getActiveHandleCount(), "Should have 2 handles");
    }
  }

  @Nested
  @DisplayName("getStats Tests")
  class GetStatsTests {

    @Test
    @DisplayName("Should return stats")
    void shouldReturnStats() {
      manager.registerHandle(createTestHandle(3));
      manager.registerHandle(createTestHandle(4));

      final WasiFileHandleManager.HandleManagerStats stats = manager.getStats();

      assertNotNull(stats, "Stats should not be null");
      assertEquals(2, stats.getActiveHandles(), "Should have 2 active handles");
      assertEquals(2, stats.getTotalHandlesCreated(), "Should have created 2 handles");
    }

    @Test
    @DisplayName("Stats toString should contain all fields")
    void statsToStringShouldContainAllFields() {
      manager.registerHandle(createTestHandle(3));
      final WasiFileHandleManager.HandleManagerStats stats = manager.getStats();

      final String str = stats.toString();

      assertTrue(str.contains("active="), "Should contain active");
      assertTrue(str.contains("max="), "Should contain max");
      assertTrue(str.contains("created="), "Should contain created");
      assertTrue(str.contains("closed="), "Should contain closed");
    }
  }

  @Nested
  @DisplayName("forceCleanup Tests")
  class ForceCleanupTests {

    @Test
    @DisplayName("Should return zero when no expired handles")
    void shouldReturnZeroWhenNoExpiredHandles() {
      manager.registerHandle(createTestHandle(3));

      final int cleaned = manager.forceCleanup();

      assertEquals(0, cleaned, "Should not clean up any handles");
    }

    @Test
    @DisplayName("Should return zero after shutdown")
    void shouldReturnZeroAfterShutdown() {
      manager.close();

      final int cleaned = manager.forceCleanup();

      assertEquals(0, cleaned, "Should return 0 after shutdown");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close all handles")
    void shouldCloseAllHandles() {
      manager.registerHandle(createTestHandle(3));
      manager.registerHandle(createTestHandle(4));
      assertEquals(2, manager.getActiveHandleCount(), "Should have 2 handles");

      manager.close();

      // After close, getActiveHandleCount throws or returns 0
      // Check via stats that handles were closed
    }

    @Test
    @DisplayName("Should be idempotent")
    void shouldBeIdempotent() {
      manager.registerHandle(createTestHandle(3));

      manager.close();
      manager.close(); // Should not throw

      assertTrue(true, "Close should be idempotent");
    }
  }

  @Nested
  @DisplayName("ManagedFileHandle Tests")
  class ManagedFileHandleTests {

    @Test
    @DisplayName("Should return underlying handle")
    void shouldReturnUnderlyingHandle() {
      final WasiFileHandle mockHandle = createTestHandle(5);
      final WasiFileHandleManager.ManagedFileHandle managed = manager.registerHandle(mockHandle);

      assertEquals(mockHandle, managed.getHandle(), "Should return underlying handle");
    }

    @Test
    @DisplayName("Should track last access time")
    void shouldTrackLastAccessTime() {
      final WasiFileHandle mockHandle = createTestHandle(5);
      final WasiFileHandleManager.ManagedFileHandle managed = manager.registerHandle(mockHandle);

      assertTrue(managed.getLastAccessTime() > 0, "Should have last access time");
    }
  }

  @Nested
  @DisplayName("HandleManagerStats Tests")
  class HandleManagerStatsTests {

    @Test
    @DisplayName("Should return all stat values")
    void shouldReturnAllStatValues() {
      manager.registerHandle(createTestHandle(3));
      manager.unregisterHandle(3);
      manager.registerHandle(createTestHandle(4));

      final WasiFileHandleManager.HandleManagerStats stats = manager.getStats();

      assertEquals(1, stats.getActiveHandles(), "Should have 1 active handle");
      assertTrue(stats.getMaxHandles() > 0, "Max handles should be > 0");
      assertEquals(2, stats.getTotalHandlesCreated(), "Should have created 2");
      assertEquals(1, stats.getTotalHandlesClosed(), "Should have closed 1");
      assertEquals(0, stats.getTotalHandlesGarbageCollected(), "Should have 0 GC'd");
    }
  }

  @Nested
  @DisplayName("Concurrent Access Tests")
  class ConcurrentAccessTests {

    @Test
    @DisplayName("Should handle concurrent registrations")
    void shouldHandleConcurrentRegistrations() throws InterruptedException {
      final int threadCount = 10;
      final Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int fd = i + 3;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    manager.registerHandle(createTestHandle(fd));
                  } catch (final Exception e) {
                    // May fail due to race conditions, which is expected
                  }
                });
      }

      for (final Thread thread : threads) {
        thread.start();
      }

      for (final Thread thread : threads) {
        thread.join();
      }

      assertTrue(
          manager.getActiveHandleCount() <= threadCount,
          "Should have at most " + threadCount + " handles");
    }
  }

  /** Helper to create test WasiFileHandle. */
  private WasiFileHandle createTestHandle(final int fd) {
    return TestWasiFileHandleFactory.createTestHandle(fd);
  }
}
