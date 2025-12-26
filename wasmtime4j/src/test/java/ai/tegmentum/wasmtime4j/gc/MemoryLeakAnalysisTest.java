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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemoryLeakAnalysis} interface.
 *
 * <p>MemoryLeakAnalysis provides detailed information about potential memory leaks, including
 * objects that should have been garbage collected, circular references, and memory leak patterns.
 */
@DisplayName("MemoryLeakAnalysis Tests")
class MemoryLeakAnalysisTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          MemoryLeakAnalysis.class.isInterface(), "MemoryLeakAnalysis should be an interface");
    }
  }

  @Nested
  @DisplayName("Basic Analysis Method Tests")
  class BasicAnalysisMethodTests {

    @Test
    @DisplayName("should have getAnalysisTime method")
    void shouldHaveGetAnalysisTimeMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getAnalysisTime");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getTotalObjectCount method")
    void shouldHaveGetTotalObjectCountMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getTotalObjectCount");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPotentialLeakCount method")
    void shouldHaveGetPotentialLeakCountMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getPotentialLeakCount");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPotentialLeaks method")
    void shouldHaveGetPotentialLeaksMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getPotentialLeaks");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Detailed Analysis Method Tests")
  class DetailedAnalysisMethodTests {

    @Test
    @DisplayName("should have getCircularReferences method")
    void shouldHaveGetCircularReferencesMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getCircularReferences");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getLongLivedObjects method")
    void shouldHaveGetLongLivedObjectsMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getLongLivedObjects");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getHighlyReferencedObjects method")
    void shouldHaveGetHighlyReferencedObjectsMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getHighlyReferencedObjects");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getMemoryUsageTrend method")
    void shouldHaveGetMemoryUsageTrendMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getMemoryUsageTrend");
      assertEquals(
          MemoryLeakAnalysis.MemoryUsageTrend.class,
          method.getReturnType(),
          "Should return MemoryUsageTrend");
    }

    @Test
    @DisplayName("should have getLeakSeverity method")
    void shouldHaveGetLeakSeverityMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getLeakSeverity");
      assertEquals(
          MemoryLeakAnalysis.LeakSeverity.class,
          method.getReturnType(),
          "Should return LeakSeverity");
    }

    @Test
    @DisplayName("should have getRecommendations method")
    void shouldHaveGetRecommendationsMethod() throws NoSuchMethodException {
      final Method method = MemoryLeakAnalysis.class.getMethod("getRecommendations");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("PotentialLeak Interface Tests")
  class PotentialLeakTests {

    @Test
    @DisplayName("should have all potential leak methods")
    void shouldHaveAllPotentialLeakMethods() {
      final String[] expectedMethods = {
        "getObjectId",
        "getConfidence",
        "getLeakType",
        "getObjectType",
        "getCreationTime",
        "getTimeSinceLastAccess",
        "getReferenceCount",
        "getReason",
        "getReferencePath"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryLeakAnalysis.PotentialLeak.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("CircularReference Interface Tests")
  class CircularReferenceTests {

    @Test
    @DisplayName("should have all circular reference methods")
    void shouldHaveAllCircularReferenceMethods() {
      final String[] expectedMethods = {
        "getCycleObjects", "getCycleLength", "isBlockingGc", "getReferencePath"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryLeakAnalysis.CircularReference.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("LongLivedObject Interface Tests")
  class LongLivedObjectTests {

    @Test
    @DisplayName("should have all long-lived object methods")
    void shouldHaveAllLongLivedObjectMethods() {
      final String[] expectedMethods = {
        "getObjectId",
        "getAgeMillis",
        "getObjectType",
        "getAccessCount",
        "getLastAccess",
        "isLegitimate"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryLeakAnalysis.LongLivedObject.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("HighlyReferencedObject Interface Tests")
  class HighlyReferencedObjectTests {

    @Test
    @DisplayName("should have all highly referenced object methods")
    void shouldHaveAllHighlyReferencedObjectMethods() {
      final String[] expectedMethods = {
        "getObjectId",
        "getReferenceCount",
        "getObjectType",
        "getAverageReferenceCount",
        "isSuspicious"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryLeakAnalysis.HighlyReferencedObject.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("MemoryUsageTrend Interface Tests")
  class MemoryUsageTrendTests {

    @Test
    @DisplayName("should have all memory usage trend methods")
    void shouldHaveAllMemoryUsageTrendMethods() {
      final String[] expectedMethods = {
        "isIncreasing",
        "getGrowthRate",
        "getCorrelation",
        "getTimeToExhaustionMillis",
        "isLeakPattern"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryLeakAnalysis.MemoryUsageTrend.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ReferencePathElement Interface Tests")
  class ReferencePathElementTests {

    @Test
    @DisplayName("should have all reference path element methods")
    void shouldHaveAllReferencePathElementMethods() {
      final String[] expectedMethods = {
        "getSourceObjectId", "getTargetObjectId", "getReferenceType", "getIndex"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryLeakAnalysis.ReferencePathElement.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("LeakType Enum Tests")
  class LeakTypeEnumTests {

    @Test
    @DisplayName("should have all leak types")
    void shouldHaveAllLeakTypes() {
      final MemoryLeakAnalysis.LeakType[] values = MemoryLeakAnalysis.LeakType.values();
      assertEquals(6, values.length, "Should have 6 leak types");

      assertNotNull(MemoryLeakAnalysis.LeakType.valueOf("UNREACHABLE_OBJECT"));
      assertNotNull(MemoryLeakAnalysis.LeakType.valueOf("CIRCULAR_REFERENCE"));
      assertNotNull(MemoryLeakAnalysis.LeakType.valueOf("WEAK_REFERENCE_LEAK"));
      assertNotNull(MemoryLeakAnalysis.LeakType.valueOf("LONG_LIVED_TEMPORARY"));
      assertNotNull(MemoryLeakAnalysis.LeakType.valueOf("UNBOUNDED_GROWTH"));
      assertNotNull(MemoryLeakAnalysis.LeakType.valueOf("RESOURCE_LEAK"));
    }
  }

  @Nested
  @DisplayName("LeakSeverity Enum Tests")
  class LeakSeverityEnumTests {

    @Test
    @DisplayName("should have all leak severity levels")
    void shouldHaveAllLeakSeverityLevels() {
      final MemoryLeakAnalysis.LeakSeverity[] values = MemoryLeakAnalysis.LeakSeverity.values();
      assertEquals(4, values.length, "Should have 4 leak severity levels");

      assertNotNull(MemoryLeakAnalysis.LeakSeverity.valueOf("LOW"));
      assertNotNull(MemoryLeakAnalysis.LeakSeverity.valueOf("MODERATE"));
      assertNotNull(MemoryLeakAnalysis.LeakSeverity.valueOf("HIGH"));
      assertNotNull(MemoryLeakAnalysis.LeakSeverity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("RecommendationType Enum Tests")
  class RecommendationTypeEnumTests {

    @Test
    @DisplayName("should have all recommendation types")
    void shouldHaveAllRecommendationTypes() {
      final MemoryLeakAnalysis.RecommendationType[] values =
          MemoryLeakAnalysis.RecommendationType.values();
      assertEquals(6, values.length, "Should have 6 recommendation types");

      assertNotNull(MemoryLeakAnalysis.RecommendationType.valueOf("BREAK_CYCLE"));
      assertNotNull(MemoryLeakAnalysis.RecommendationType.valueOf("CLEAR_WEAK_REFERENCES"));
      assertNotNull(MemoryLeakAnalysis.RecommendationType.valueOf("IMPLEMENT_CLEANUP"));
      assertNotNull(MemoryLeakAnalysis.RecommendationType.valueOf("ADD_BOUNDS"));
      assertNotNull(MemoryLeakAnalysis.RecommendationType.valueOf("REVIEW_LIFECYCLE"));
      assertNotNull(MemoryLeakAnalysis.RecommendationType.valueOf("TUNE_GC"));
    }
  }

  @Nested
  @DisplayName("RecommendationPriority Enum Tests")
  class RecommendationPriorityEnumTests {

    @Test
    @DisplayName("should have all recommendation priority levels")
    void shouldHaveAllRecommendationPriorityLevels() {
      final MemoryLeakAnalysis.RecommendationPriority[] values =
          MemoryLeakAnalysis.RecommendationPriority.values();
      assertEquals(4, values.length, "Should have 4 recommendation priority levels");

      assertNotNull(MemoryLeakAnalysis.RecommendationPriority.valueOf("URGENT"));
      assertNotNull(MemoryLeakAnalysis.RecommendationPriority.valueOf("HIGH"));
      assertNotNull(MemoryLeakAnalysis.RecommendationPriority.valueOf("MEDIUM"));
      assertNotNull(MemoryLeakAnalysis.RecommendationPriority.valueOf("LOW"));
    }
  }

  @Nested
  @DisplayName("LeakRecommendation Interface Tests")
  class LeakRecommendationTests {

    @Test
    @DisplayName("should have all leak recommendation methods")
    void shouldHaveAllLeakRecommendationMethods() {
      final String[] expectedMethods = {
        "getType", "getDescription", "getAffectedObjects", "getExpectedImpact", "getPriority"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryLeakAnalysis.LeakRecommendation.class, methodName),
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
    @DisplayName("should support leak detection pattern")
    void shouldSupportLeakDetectionPattern() {
      // Documents usage:
      // if (analysis.getPotentialLeakCount() > 0) {
      //   List<PotentialLeak> leaks = analysis.getPotentialLeaks();
      //   for (PotentialLeak leak : leaks) { ... }
      // }
      assertTrue(
          hasMethod(MemoryLeakAnalysis.class, "getPotentialLeakCount"),
          "Need getPotentialLeakCount method");
      assertTrue(
          hasMethod(MemoryLeakAnalysis.class, "getPotentialLeaks"),
          "Need getPotentialLeaks method");
    }

    @Test
    @DisplayName("should support circular reference detection pattern")
    void shouldSupportCircularReferenceDetectionPattern() {
      // Documents usage:
      // List<CircularReference> cycles = analysis.getCircularReferences();
      // for (CircularReference cycle : cycles) {
      //   if (cycle.isBlockingGc()) { ... }
      // }
      assertTrue(
          hasMethod(MemoryLeakAnalysis.class, "getCircularReferences"),
          "Need getCircularReferences method");
    }

    @Test
    @DisplayName("should support memory trend analysis pattern")
    void shouldSupportMemoryTrendAnalysisPattern() {
      // Documents usage:
      // MemoryUsageTrend trend = analysis.getMemoryUsageTrend();
      // if (trend.isLeakPattern()) { ... }
      assertTrue(
          hasMethod(MemoryLeakAnalysis.class, "getMemoryUsageTrend"),
          "Need getMemoryUsageTrend method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
