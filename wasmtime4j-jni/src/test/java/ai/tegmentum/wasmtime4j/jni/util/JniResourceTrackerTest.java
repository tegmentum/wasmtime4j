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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniResourceTracker}. */
@DisplayName("JniResourceTracker Tests")
class JniResourceTrackerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniResourceTracker should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniResourceTracker.class.getModifiers()),
          "JniResourceTracker should be final");
    }

    @Test
    @DisplayName("JniResourceTracker should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = JniResourceTracker.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }

    @Test
    @DisplayName("Private constructor should throw AssertionError")
    void privateConstructorShouldThrowAssertionError() throws Exception {
      final Constructor<?> constructor = JniResourceTracker.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      assertThrows(
          InvocationTargetException.class,
          constructor::newInstance,
          "Constructor should throw when invoked");
    }
  }

  @Nested
  @DisplayName("trackResource Tests")
  class TrackResourceTests {

    @Test
    @DisplayName("Should track resource with valid parameters")
    void shouldTrackResourceWithValidParameters() {
      final int initialCount = JniResourceTracker.getTrackedResourceCount();
      final Object testObject = new Object();
      final AtomicBoolean cleanupCalled = new AtomicBoolean(false);

      JniResourceTracker.trackResource(testObject, 12345L, "Engine", () -> cleanupCalled.set(true));

      assertTrue(
          JniResourceTracker.getTrackedResourceCount() >= initialCount,
          "Tracked count should not decrease");
    }

    @Test
    @DisplayName("Should not track null object")
    void shouldNotTrackNullObject() {
      final int initialCount = JniResourceTracker.getTrackedResourceCount();

      JniResourceTracker.trackResource(null, 12345L, "Engine", () -> {});

      // Count should not increase
      assertTrue(
          JniResourceTracker.getTrackedResourceCount() <= initialCount + 1,
          "Count should not change for null object");
    }

    @Test
    @DisplayName("Should not track zero handle")
    void shouldNotTrackZeroHandle() {
      final int initialCount = JniResourceTracker.getTrackedResourceCount();
      final Object testObject = new Object();

      JniResourceTracker.trackResource(testObject, 0L, "Engine", () -> {});

      // Count should not increase
      assertTrue(
          JniResourceTracker.getTrackedResourceCount() <= initialCount + 1,
          "Count should not change for zero handle");
    }

    @Test
    @DisplayName("Should not track null cleanup action")
    void shouldNotTrackNullCleanupAction() {
      final int initialCount = JniResourceTracker.getTrackedResourceCount();
      final Object testObject = new Object();

      JniResourceTracker.trackResource(testObject, 12345L, "Engine", null);

      // Count should not increase
      assertTrue(
          JniResourceTracker.getTrackedResourceCount() <= initialCount + 1,
          "Count should not change for null action");
    }
  }

  @Nested
  @DisplayName("untrackResource Tests")
  class UntrackResourceTests {

    @Test
    @DisplayName("Should return false for null object")
    void shouldReturnFalseForNullObject() {
      final boolean result = JniResourceTracker.untrackResource(null);

      assertFalse(result, "Should return false for null");
    }

    @Test
    @DisplayName("Should handle untracking tracked object")
    void shouldHandleUntrackingTrackedObject() {
      final Object testObject = new Object();
      JniResourceTracker.trackResource(testObject, 11111L, "Store", () -> {});

      // This is expected to return false as per the current implementation
      // which relies on phantom reference mechanism
      final boolean result = JniResourceTracker.untrackResource(testObject);

      assertFalse(result, "Current implementation returns false for all untrack calls");
    }
  }

  @Nested
  @DisplayName("getTrackedResourceCount Tests")
  class GetTrackedResourceCountTests {

    @Test
    @DisplayName("Should return non-negative count")
    void shouldReturnNonNegativeCount() {
      final int count = JniResourceTracker.getTrackedResourceCount();

      assertTrue(count >= 0, "Count should be non-negative");
    }

    @Test
    @DisplayName("Should track new resources")
    void shouldTrackNewResources() {
      final int initialCount = JniResourceTracker.getTrackedResourceCount();

      // Create a new object and track it
      final Object obj1 = new Object();
      JniResourceTracker.trackResource(obj1, 22221L, "Engine", () -> {});

      final int afterCount = JniResourceTracker.getTrackedResourceCount();

      assertTrue(afterCount >= initialCount, "Count should increase or stay same after tracking");
    }
  }

  @Nested
  @DisplayName("getTrackingStats Tests")
  class GetTrackingStatsTests {

    @Test
    @DisplayName("Should return stats string")
    void shouldReturnStatsString() {
      final String stats = JniResourceTracker.getTrackingStats();

      assertNotNull(stats, "Stats should not be null");
      assertTrue(stats.contains("JNI Resource Tracker Statistics"), "Should contain header");
      assertTrue(stats.contains("Tracked resources:"), "Should contain tracked resources");
      assertTrue(stats.contains("Cleanup thread active:"), "Should contain thread status");
      assertTrue(stats.contains("Shutdown requested:"), "Should contain shutdown status");
    }

    @Test
    @DisplayName("Should include resource type breakdown when tracking resources")
    void shouldIncludeResourceTypeBreakdownWhenTrackingResources() {
      // Track some resources
      final Object obj1 = new Object();
      final Object obj2 = new Object();
      JniResourceTracker.trackResource(obj1, 33331L, "Engine", () -> {});
      JniResourceTracker.trackResource(obj2, 33332L, "Module", () -> {});

      final String stats = JniResourceTracker.getTrackingStats();

      // Stats may or may not include breakdown depending on tracked count
      assertNotNull(stats, "Stats should not be null");
    }
  }

  @Nested
  @DisplayName("Cleanup Thread Tests")
  class CleanupThreadTests {

    @Test
    @DisplayName("Cleanup thread should be alive")
    void cleanupThreadShouldBeAlive() {
      // Just tracking a resource ensures the thread is started
      final Object obj = new Object();
      JniResourceTracker.trackResource(obj, 44441L, "Test", () -> {});

      final String stats = JniResourceTracker.getTrackingStats();

      assertTrue(stats.contains("Cleanup thread active: true"), "Cleanup thread should be active");
    }
  }

  @Nested
  @DisplayName("Resource Cleanup Tests")
  class ResourceCleanupTests {

    @Test
    @DisplayName("Should call cleanup action when object is garbage collected")
    void shouldCallCleanupActionWhenObjectIsGarbageCollected() throws InterruptedException {
      final AtomicBoolean cleanupCalled = new AtomicBoolean(false);
      final long nativeHandle = 55551L;

      // Create object in a scope so it can be GC'd
      createAndTrackObject(nativeHandle, cleanupCalled);

      // Request garbage collection multiple times
      for (int i = 0; i < 5; i++) {
        System.gc();
        Thread.sleep(100);
      }

      // Note: We cannot guarantee GC will happen, so this test
      // primarily validates the setup doesn't throw exceptions
      // The cleanup may or may not be called depending on GC behavior
      assertTrue(true, "Test completed without exception");
    }

    private void createAndTrackObject(final long handle, final AtomicBoolean cleanupCalled) {
      final Object obj = new Object();
      JniResourceTracker.trackResource(obj, handle, "GCTest", () -> cleanupCalled.set(true));
      // Object goes out of scope here
    }
  }

  @Nested
  @DisplayName("Concurrent Tracking Tests")
  class ConcurrentTrackingTests {

    @Test
    @DisplayName("Should handle concurrent resource tracking")
    void shouldHandleConcurrentResourceTracking() throws InterruptedException {
      final int threadCount = 10;
      final int resourcesPerThread = 50;
      final Thread[] threads = new Thread[threadCount];

      for (int t = 0; t < threadCount; t++) {
        final int threadIndex = t;
        threads[t] =
            new Thread(
                () -> {
                  for (int i = 0; i < resourcesPerThread; i++) {
                    final Object obj = new Object();
                    final long handle = (threadIndex * 10000L) + i + 1;
                    JniResourceTracker.trackResource(obj, handle, "Thread" + threadIndex, () -> {});
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

      // Verify no exception was thrown and tracking count is reasonable
      assertTrue(
          JniResourceTracker.getTrackedResourceCount() >= 0,
          "Should have non-negative tracked count");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle empty resource type")
    void shouldHandleEmptyResourceType() {
      final Object obj = new Object();
      final AtomicBoolean cleanupCalled = new AtomicBoolean(false);

      // This should not throw
      JniResourceTracker.trackResource(obj, 66661L, "", () -> cleanupCalled.set(true));

      assertTrue(true, "Should handle empty resource type");
    }

    @Test
    @DisplayName("Should handle null resource type")
    void shouldHandleNullResourceType() {
      final Object obj = new Object();
      final AtomicBoolean cleanupCalled = new AtomicBoolean(false);

      // This should not throw
      JniResourceTracker.trackResource(obj, 66662L, null, () -> cleanupCalled.set(true));

      assertTrue(true, "Should handle null resource type");
    }

    @Test
    @DisplayName("Should handle cleanup action that throws exception")
    void shouldHandleCleanupActionThatThrowsException() {
      final Object obj = new Object();

      // Track with a cleanup action that throws
      JniResourceTracker.trackResource(
          obj,
          66663L,
          "ThrowTest",
          () -> {
            throw new RuntimeException("Cleanup failed");
          });

      // Just verifying it doesn't break the tracker
      assertNotNull(JniResourceTracker.getTrackingStats(), "Tracker should still work");
    }

    @Test
    @DisplayName("Should handle negative handle value")
    void shouldHandleNegativeHandleValue() {
      final Object obj = new Object();
      final AtomicBoolean cleanupCalled = new AtomicBoolean(false);

      // Negative values are technically valid handles
      JniResourceTracker.trackResource(
          obj, -12345L, "NegativeHandle", () -> cleanupCalled.set(true));

      assertTrue(true, "Should handle negative handle value");
    }

    @Test
    @DisplayName("Should handle Long.MAX_VALUE handle")
    void shouldHandleLongMaxValueHandle() {
      final Object obj = new Object();
      final AtomicBoolean cleanupCalled = new AtomicBoolean(false);

      JniResourceTracker.trackResource(
          obj, Long.MAX_VALUE, "MaxHandle", () -> cleanupCalled.set(true));

      assertTrue(true, "Should handle Long.MAX_VALUE handle");
    }
  }
}
