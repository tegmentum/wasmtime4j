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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ObjectLifecycleTracker} interface.
 *
 * <p>ObjectLifecycleTracker tracks the lifecycle of WebAssembly GC objects for debugging and
 * profiling, including creation, access patterns, reference changes, and garbage collection events.
 */
@DisplayName("ObjectLifecycleTracker Tests")
class ObjectLifecycleTrackerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ObjectLifecycleTracker.class.isInterface(),
          "ObjectLifecycleTracker should be an interface");
    }
  }

  @Nested
  @DisplayName("Tracking Method Tests")
  class TrackingMethodTests {

    @Test
    @DisplayName("should have getTrackedObjects method")
    void shouldHaveGetTrackedObjectsMethod() throws NoSuchMethodException {
      final Method method = ObjectLifecycleTracker.class.getMethod("getTrackedObjects");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getLifecycleEvents method")
    void shouldHaveGetLifecycleEventsMethod() throws NoSuchMethodException {
      final Method method =
          ObjectLifecycleTracker.class.getMethod("getLifecycleEvents", long.class);
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getObjectStatuses method")
    void shouldHaveGetObjectStatusesMethod() throws NoSuchMethodException {
      final Method method = ObjectLifecycleTracker.class.getMethod("getObjectStatuses");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getAccessStatistics method")
    void shouldHaveGetAccessStatisticsMethod() throws NoSuchMethodException {
      final Method method = ObjectLifecycleTracker.class.getMethod("getAccessStatistics");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getReferenceHistory method")
    void shouldHaveGetReferenceHistoryMethod() throws NoSuchMethodException {
      final Method method = ObjectLifecycleTracker.class.getMethod("getReferenceHistory");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Control Method Tests")
  class ControlMethodTests {

    @Test
    @DisplayName("should have stopTracking method")
    void shouldHaveStopTrackingMethod() throws NoSuchMethodException {
      final Method method = ObjectLifecycleTracker.class.getMethod("stopTracking");
      assertEquals(
          ObjectLifecycleTracker.LifecycleTrackingSummary.class,
          method.getReturnType(),
          "Should return LifecycleTrackingSummary");
    }

    @Test
    @DisplayName("should have trackAdditionalObjects method")
    void shouldHaveTrackAdditionalObjectsMethod() throws NoSuchMethodException {
      final Method method =
          ObjectLifecycleTracker.class.getMethod("trackAdditionalObjects", List.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stopTrackingObjects method")
    void shouldHaveStopTrackingObjectsMethod() throws NoSuchMethodException {
      final Method method =
          ObjectLifecycleTracker.class.getMethod("stopTrackingObjects", List.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("LifecycleEvent Interface Tests")
  class LifecycleEventTests {

    @Test
    @DisplayName("should have all lifecycle event methods")
    void shouldHaveAllLifecycleEventMethods() {
      final String[] expectedMethods = {
        "getTimestamp", "getEventType", "getObjectId", "getDetails", "getThreadId"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ObjectLifecycleTracker.LifecycleEvent.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("LifecycleEventType Enum Tests")
  class LifecycleEventTypeEnumTests {

    @Test
    @DisplayName("should have all lifecycle event types")
    void shouldHaveAllLifecycleEventTypes() {
      final ObjectLifecycleTracker.LifecycleEventType[] values =
          ObjectLifecycleTracker.LifecycleEventType.values();
      assertEquals(7, values.length, "Should have 7 lifecycle event types");

      assertNotNull(ObjectLifecycleTracker.LifecycleEventType.valueOf("CREATED"));
      assertNotNull(ObjectLifecycleTracker.LifecycleEventType.valueOf("ACCESSED"));
      assertNotNull(ObjectLifecycleTracker.LifecycleEventType.valueOf("MODIFIED"));
      assertNotNull(ObjectLifecycleTracker.LifecycleEventType.valueOf("REFERENCE_ADDED"));
      assertNotNull(ObjectLifecycleTracker.LifecycleEventType.valueOf("REFERENCE_REMOVED"));
      assertNotNull(ObjectLifecycleTracker.LifecycleEventType.valueOf("UNREACHABLE"));
      assertNotNull(ObjectLifecycleTracker.LifecycleEventType.valueOf("COLLECTED"));
    }
  }

  @Nested
  @DisplayName("ObjectStatus Interface Tests")
  class ObjectStatusTests {

    @Test
    @DisplayName("should have all object status methods")
    void shouldHaveAllObjectStatusMethods() {
      final String[] expectedMethods = {
        "getObjectId",
        "isAlive",
        "getReferenceCount",
        "getLastAccessed",
        "getCreationTime",
        "getObjectType"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ObjectLifecycleTracker.ObjectStatus.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("AccessStatistics Interface Tests")
  class AccessStatisticsTests {

    @Test
    @DisplayName("should have all access statistics methods")
    void shouldHaveAllAccessStatisticsMethods() {
      final String[] expectedMethods = {
        "getObjectId",
        "getReadCount",
        "getWriteCount",
        "getAccessingThreadCount",
        "getFirstAccess",
        "getLastAccess",
        "getAverageAccessInterval"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ObjectLifecycleTracker.AccessStatistics.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ReferenceChange Interface Tests")
  class ReferenceChangeTests {

    @Test
    @DisplayName("should have all reference change methods")
    void shouldHaveAllReferenceChangeMethods() {
      final String[] expectedMethods = {
        "getTimestamp",
        "getChangeType",
        "getSourceObjectId",
        "getTargetObjectId",
        "getIndex",
        "getDetails"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ObjectLifecycleTracker.ReferenceChange.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ReferenceChangeType Enum Tests")
  class ReferenceChangeTypeEnumTests {

    @Test
    @DisplayName("should have all reference change types")
    void shouldHaveAllReferenceChangeTypes() {
      final ObjectLifecycleTracker.ReferenceChangeType[] values =
          ObjectLifecycleTracker.ReferenceChangeType.values();
      assertEquals(3, values.length, "Should have 3 reference change types");

      assertNotNull(ObjectLifecycleTracker.ReferenceChangeType.valueOf("REFERENCE_CREATED"));
      assertNotNull(ObjectLifecycleTracker.ReferenceChangeType.valueOf("REFERENCE_REMOVED"));
      assertNotNull(ObjectLifecycleTracker.ReferenceChangeType.valueOf("REFERENCE_UPDATED"));
    }
  }

  @Nested
  @DisplayName("LifecycleTrackingSummary Interface Tests")
  class LifecycleTrackingSummaryTests {

    @Test
    @DisplayName("should have all tracking summary methods")
    void shouldHaveAllTrackingSummaryMethods() {
      final String[] expectedMethods = {
        "getTrackingDurationMillis",
        "getTrackedObjectCount",
        "getCollectedObjectCount",
        "getTotalEventCount",
        "getMostAccessedObjects",
        "getLongestLivedObjects",
        "getPotentialLeaks"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ObjectLifecycleTracker.LifecycleTrackingSummary.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support object lifecycle tracking pattern")
    void shouldSupportObjectLifecycleTrackingPattern() {
      // Documents usage:
      // List<LifecycleEvent> events = tracker.getLifecycleEvents(objectId);
      // for (LifecycleEvent event : events) { ... }
      assertTrue(
          hasMethod(ObjectLifecycleTracker.class, "getLifecycleEvents"),
          "Need getLifecycleEvents method");
      assertTrue(
          hasMethod(ObjectLifecycleTracker.class, "getTrackedObjects"),
          "Need getTrackedObjects method");
    }

    @Test
    @DisplayName("should support access pattern analysis")
    void shouldSupportAccessPatternAnalysis() {
      // Documents usage:
      // Map<Long, AccessStatistics> stats = tracker.getAccessStatistics();
      assertTrue(
          hasMethod(ObjectLifecycleTracker.class, "getAccessStatistics"),
          "Need getAccessStatistics method");
    }

    @Test
    @DisplayName("should support reference tracking pattern")
    void shouldSupportReferenceTrackingPattern() {
      // Documents usage:
      // Map<Long, List<ReferenceChange>> history = tracker.getReferenceHistory();
      assertTrue(
          hasMethod(ObjectLifecycleTracker.class, "getReferenceHistory"),
          "Need getReferenceHistory method");
    }

    @Test
    @DisplayName("should support dynamic object tracking")
    void shouldSupportDynamicObjectTracking() {
      // Documents usage:
      // tracker.trackAdditionalObjects(newObjects);
      // tracker.stopTrackingObjects(objectIds);
      assertTrue(
          hasMethod(ObjectLifecycleTracker.class, "trackAdditionalObjects"),
          "Need trackAdditionalObjects method");
      assertTrue(
          hasMethod(ObjectLifecycleTracker.class, "stopTrackingObjects"),
          "Need stopTrackingObjects method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("WASM GC Specification Compliance Tests")
  class WasmGcSpecificationComplianceTests {

    @Test
    @DisplayName("should have all required lifecycle tracker methods")
    void shouldHaveAllRequiredLifecycleTrackerMethods() {
      final String[] expectedMethods = {
        "getTrackedObjects",
        "getLifecycleEvents",
        "getObjectStatuses",
        "getAccessStatistics",
        "getReferenceHistory",
        "stopTracking",
        "trackAdditionalObjects",
        "stopTrackingObjects"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ObjectLifecycleTracker.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
