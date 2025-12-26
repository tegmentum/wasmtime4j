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

package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JniMemoryManager}.
 */
@DisplayName("JniMemoryManager Tests")
class JniMemoryManagerTest {

  @BeforeEach
  void setUp() {
    // Perform emergency cleanup to start with a clean slate
    JniMemoryManager.emergencyCleanup();
  }

  @AfterEach
  void tearDown() {
    // Clean up after each test
    JniMemoryManager.emergencyCleanup();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniMemoryManager should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniMemoryManager.class.getModifiers()),
          "JniMemoryManager should be final");
    }

    @Test
    @DisplayName("JniMemoryManager should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = JniMemoryManager.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should throw AssertionError")
    void privateConstructorShouldThrowAssertionError() throws Exception {
      final Constructor<?> constructor = JniMemoryManager.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      assertThrows(InvocationTargetException.class, constructor::newInstance,
          "Constructor should throw when invoked");
    }
  }

  @Nested
  @DisplayName("registerHandle Tests")
  class RegisterHandleTests {

    @Test
    @DisplayName("Should register valid handle with resource type")
    void shouldRegisterValidHandleWithResourceType() {
      final long initialCount = JniMemoryManager.getTotalAllocatedCount();

      JniMemoryManager.registerHandle(12345L, "Engine");

      assertTrue(JniMemoryManager.isHandleRegistered(12345L), "Handle should be registered");
      assertEquals("Engine", JniMemoryManager.getResourceType(12345L),
          "Resource type should match");
      assertEquals(initialCount + 1, JniMemoryManager.getTotalAllocatedCount(),
          "Allocated count should increase");
    }

    @Test
    @DisplayName("Should register handle with creation location")
    void shouldRegisterHandleWithCreationLocation() {
      JniMemoryManager.registerHandle(67890L, "Module", "TestClass.java:42");

      assertTrue(JniMemoryManager.isHandleRegistered(67890L), "Handle should be registered");
      assertEquals("Module", JniMemoryManager.getResourceType(67890L),
          "Resource type should match");
    }

    @Test
    @DisplayName("Should not register zero handle")
    void shouldNotRegisterZeroHandle() {
      final int initialActiveCount = JniMemoryManager.getActiveHandleCount();

      JniMemoryManager.registerHandle(0L, "Engine");

      assertFalse(JniMemoryManager.isHandleRegistered(0L), "Zero handle should not be registered");
      assertEquals(initialActiveCount, JniMemoryManager.getActiveHandleCount(),
          "Active count should not change");
    }

    @Test
    @DisplayName("Should handle re-registration of existing handle")
    void shouldHandleReRegistrationOfExistingHandle() {
      JniMemoryManager.registerHandle(11111L, "Engine");
      JniMemoryManager.registerHandle(11111L, "Module");

      // The handle should now be associated with "Module"
      assertEquals("Module", JniMemoryManager.getResourceType(11111L),
          "Resource type should be updated");
    }
  }

  @Nested
  @DisplayName("unregisterHandle Tests")
  class UnregisterHandleTests {

    @Test
    @DisplayName("Should unregister existing handle")
    void shouldUnregisterExistingHandle() {
      JniMemoryManager.registerHandle(22222L, "Store");
      final long deallocBefore = JniMemoryManager.getTotalDeallocatedCount();

      final boolean result = JniMemoryManager.unregisterHandle(22222L);

      assertTrue(result, "Unregister should return true");
      assertFalse(JniMemoryManager.isHandleRegistered(22222L),
          "Handle should no longer be registered");
      assertEquals(deallocBefore + 1, JniMemoryManager.getTotalDeallocatedCount(),
          "Deallocated count should increase");
    }

    @Test
    @DisplayName("Should return false for unknown handle")
    void shouldReturnFalseForUnknownHandle() {
      final boolean result = JniMemoryManager.unregisterHandle(99999L);

      assertFalse(result, "Unregister should return false for unknown handle");
    }

    @Test
    @DisplayName("Should return false for zero handle")
    void shouldReturnFalseForZeroHandle() {
      final boolean result = JniMemoryManager.unregisterHandle(0L);

      assertFalse(result, "Unregister should return false for zero handle");
    }
  }

  @Nested
  @DisplayName("isHandleRegistered Tests")
  class IsHandleRegisteredTests {

    @Test
    @DisplayName("Should return true for registered handle")
    void shouldReturnTrueForRegisteredHandle() {
      JniMemoryManager.registerHandle(33333L, "Instance");

      assertTrue(JniMemoryManager.isHandleRegistered(33333L), "Should return true");
    }

    @Test
    @DisplayName("Should return false for unregistered handle")
    void shouldReturnFalseForUnregisteredHandle() {
      assertFalse(JniMemoryManager.isHandleRegistered(44444L), "Should return false");
    }

    @Test
    @DisplayName("Should return false for zero handle")
    void shouldReturnFalseForZeroHandleCheck() {
      assertFalse(JniMemoryManager.isHandleRegistered(0L), "Should return false for zero handle");
    }
  }

  @Nested
  @DisplayName("getResourceType Tests")
  class GetResourceTypeTests {

    @Test
    @DisplayName("Should return resource type for registered handle")
    void shouldReturnResourceTypeForRegisteredHandle() {
      JniMemoryManager.registerHandle(55555L, "Memory");

      assertEquals("Memory", JniMemoryManager.getResourceType(55555L), "Should return type");
    }

    @Test
    @DisplayName("Should return null for unregistered handle")
    void shouldReturnNullForUnregisteredHandle() {
      assertNull(JniMemoryManager.getResourceType(66666L), "Should return null");
    }
  }

  @Nested
  @DisplayName("getActiveHandleCount Tests")
  class GetActiveHandleCountTests {

    @Test
    @DisplayName("Should return zero initially after cleanup")
    void shouldReturnZeroInitiallyAfterCleanup() {
      assertEquals(0, JniMemoryManager.getActiveHandleCount(), "Should be zero after cleanup");
    }

    @Test
    @DisplayName("Should track active handles correctly")
    void shouldTrackActiveHandlesCorrectly() {
      JniMemoryManager.registerHandle(77771L, "Engine");
      JniMemoryManager.registerHandle(77772L, "Store");
      JniMemoryManager.registerHandle(77773L, "Module");

      assertEquals(3, JniMemoryManager.getActiveHandleCount(), "Should have 3 active handles");

      JniMemoryManager.unregisterHandle(77772L);

      assertEquals(2, JniMemoryManager.getActiveHandleCount(), "Should have 2 active handles");
    }
  }

  @Nested
  @DisplayName("getTotalAllocatedCount Tests")
  class GetTotalAllocatedCountTests {

    @Test
    @DisplayName("Should track total allocations")
    void shouldTrackTotalAllocations() {
      final long before = JniMemoryManager.getTotalAllocatedCount();

      JniMemoryManager.registerHandle(88881L, "Engine");
      JniMemoryManager.registerHandle(88882L, "Store");

      assertEquals(before + 2, JniMemoryManager.getTotalAllocatedCount(),
          "Should track allocations");
    }
  }

  @Nested
  @DisplayName("getTotalDeallocatedCount Tests")
  class GetTotalDeallocatedCountTests {

    @Test
    @DisplayName("Should track total deallocations")
    void shouldTrackTotalDeallocations() {
      JniMemoryManager.registerHandle(99991L, "Engine");
      JniMemoryManager.registerHandle(99992L, "Store");

      final long before = JniMemoryManager.getTotalDeallocatedCount();

      JniMemoryManager.unregisterHandle(99991L);
      JniMemoryManager.unregisterHandle(99992L);

      assertEquals(before + 2, JniMemoryManager.getTotalDeallocatedCount(),
          "Should track deallocations");
    }
  }

  @Nested
  @DisplayName("getMemoryStats Tests")
  class GetMemoryStatsTests {

    @Test
    @DisplayName("Should return stats string")
    void shouldReturnStatsString() {
      JniMemoryManager.registerHandle(10001L, "Engine");
      JniMemoryManager.registerHandle(10002L, "Module");

      final String stats = JniMemoryManager.getMemoryStats();

      assertNotNull(stats, "Stats should not be null");
      assertTrue(stats.contains("JNI Memory Management Statistics"),
          "Should contain header");
      assertTrue(stats.contains("Active handles:"), "Should contain active handles");
      assertTrue(stats.contains("Total allocated:"), "Should contain total allocated");
      assertTrue(stats.contains("Total deallocated:"), "Should contain total deallocated");
      assertTrue(stats.contains("Potential leaks:"), "Should contain potential leaks");
    }

    @Test
    @DisplayName("Should include resource type breakdown when handles exist")
    void shouldIncludeResourceTypeBreakdownWhenHandlesExist() {
      JniMemoryManager.registerHandle(10003L, "Engine");
      JniMemoryManager.registerHandle(10004L, "Engine");
      JniMemoryManager.registerHandle(10005L, "Module");

      final String stats = JniMemoryManager.getMemoryStats();

      assertTrue(stats.contains("Active handle types:"), "Should contain type breakdown header");
      assertTrue(stats.contains("Engine:"), "Should contain Engine type");
      assertTrue(stats.contains("Module:"), "Should contain Module type");
    }
  }

  @Nested
  @DisplayName("checkForLeaks Tests")
  class CheckForLeaksTests {

    @Test
    @DisplayName("Should not throw when no leaks")
    void shouldNotThrowWhenNoLeaks() {
      // Register and unregister to ensure no leaks
      JniMemoryManager.registerHandle(20001L, "Engine");
      JniMemoryManager.unregisterHandle(20001L);

      // This should complete without exception
      JniMemoryManager.checkForLeaks();
    }

    @Test
    @DisplayName("Should detect active handles as potential leaks")
    void shouldDetectActiveHandlesAsPotentialLeaks() {
      JniMemoryManager.registerHandle(20002L, "LeakTest");

      // This should log a warning but not throw
      JniMemoryManager.checkForLeaks();

      // Verify the handle is still active
      assertTrue(JniMemoryManager.isHandleRegistered(20002L),
          "Handle should still be registered");
    }
  }

  @Nested
  @DisplayName("emergencyCleanup Tests")
  class EmergencyCleanupTests {

    @Test
    @DisplayName("Should cleanup all registered handles")
    void shouldCleanupAllRegisteredHandles() {
      JniMemoryManager.registerHandle(30001L, "Engine");
      JniMemoryManager.registerHandle(30002L, "Store");
      JniMemoryManager.registerHandle(30003L, "Module");

      JniMemoryManager.emergencyCleanup();

      assertEquals(0, JniMemoryManager.getActiveHandleCount(),
          "All handles should be cleaned up");
    }

    @Test
    @DisplayName("Should do nothing when no handles registered")
    void shouldDoNothingWhenNoHandlesRegistered() {
      // Start with no handles after setUp cleanup

      // This should complete without exception
      JniMemoryManager.emergencyCleanup();

      assertEquals(0, JniMemoryManager.getActiveHandleCount(),
          "Should still have zero handles");
    }
  }

  @Nested
  @DisplayName("Concurrent Access Tests")
  class ConcurrentAccessTests {

    @Test
    @DisplayName("Should handle concurrent registrations")
    void shouldHandleConcurrentRegistrations() throws InterruptedException {
      final int threadCount = 10;
      final int handlesPerThread = 100;
      final Thread[] threads = new Thread[threadCount];

      for (int t = 0; t < threadCount; t++) {
        final int threadIndex = t;
        threads[t] = new Thread(() -> {
          for (int i = 0; i < handlesPerThread; i++) {
            final long handle = (threadIndex * 100000L) + i + 1;
            JniMemoryManager.registerHandle(handle, "Thread" + threadIndex);
          }
        });
      }

      // Start all threads
      for (final Thread thread : threads) {
        thread.start();
      }

      // Wait for all threads to complete
      for (final Thread thread : threads) {
        thread.join();
      }

      assertEquals(threadCount * handlesPerThread, JniMemoryManager.getActiveHandleCount(),
          "All handles should be registered");
    }
  }
}
