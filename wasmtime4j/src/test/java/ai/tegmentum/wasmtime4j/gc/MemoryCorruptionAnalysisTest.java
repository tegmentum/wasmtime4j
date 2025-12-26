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
 * Tests for {@link MemoryCorruptionAnalysis} interface.
 *
 * <p>MemoryCorruptionAnalysis provides detailed information about potential memory corruption
 * issues including buffer overflows, use-after-free conditions, and other memory safety violations.
 */
@DisplayName("MemoryCorruptionAnalysis Tests")
class MemoryCorruptionAnalysisTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          MemoryCorruptionAnalysis.class.isInterface(),
          "MemoryCorruptionAnalysis should be an interface");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have getAnalysisTime method")
    void shouldHaveGetAnalysisTimeMethod() throws NoSuchMethodException {
      final Method method = MemoryCorruptionAnalysis.class.getMethod("getAnalysisTime");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have isCorruptionDetected method")
    void shouldHaveIsCorruptionDetectedMethod() throws NoSuchMethodException {
      final Method method = MemoryCorruptionAnalysis.class.getMethod("isCorruptionDetected");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getCorruptionSeverity method")
    void shouldHaveGetCorruptionSeverityMethod() throws NoSuchMethodException {
      final Method method = MemoryCorruptionAnalysis.class.getMethod("getCorruptionSeverity");
      assertEquals(
          MemoryCorruptionAnalysis.CorruptionSeverity.class,
          method.getReturnType(),
          "Should return CorruptionSeverity");
    }

    @Test
    @DisplayName("should have getCorruptionIssues method")
    void shouldHaveGetCorruptionIssuesMethod() throws NoSuchMethodException {
      final Method method = MemoryCorruptionAnalysis.class.getMethod("getCorruptionIssues");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Check Result Method Tests")
  class CheckResultMethodTests {

    @Test
    @DisplayName("should have getIntegrityResult method")
    void shouldHaveGetIntegrityResultMethod() throws NoSuchMethodException {
      final Method method = MemoryCorruptionAnalysis.class.getMethod("getIntegrityResult");
      assertEquals(
          MemoryCorruptionAnalysis.MemoryIntegrityResult.class,
          method.getReturnType(),
          "Should return MemoryIntegrityResult");
    }

    @Test
    @DisplayName("should have getConsistencyResult method")
    void shouldHaveGetConsistencyResultMethod() throws NoSuchMethodException {
      final Method method = MemoryCorruptionAnalysis.class.getMethod("getConsistencyResult");
      assertEquals(
          MemoryCorruptionAnalysis.HeapConsistencyResult.class,
          method.getReturnType(),
          "Should return HeapConsistencyResult");
    }

    @Test
    @DisplayName("should have getLifecycleViolationResult method")
    void shouldHaveGetLifecycleViolationResultMethod() throws NoSuchMethodException {
      final Method method = MemoryCorruptionAnalysis.class.getMethod("getLifecycleViolationResult");
      assertEquals(
          MemoryCorruptionAnalysis.LifecycleViolationResult.class,
          method.getReturnType(),
          "Should return LifecycleViolationResult");
    }

    @Test
    @DisplayName("should have getRecommendations method")
    void shouldHaveGetRecommendationsMethod() throws NoSuchMethodException {
      final Method method = MemoryCorruptionAnalysis.class.getMethod("getRecommendations");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("CorruptionType Enum Tests")
  class CorruptionTypeEnumTests {

    @Test
    @DisplayName("should have all corruption types")
    void shouldHaveAllCorruptionTypes() {
      final MemoryCorruptionAnalysis.CorruptionType[] values =
          MemoryCorruptionAnalysis.CorruptionType.values();
      assertEquals(9, values.length, "Should have 9 corruption types");

      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("BUFFER_OVERFLOW"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("USE_AFTER_FREE"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("DOUBLE_FREE"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("INVALID_POINTER"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("HEAP_METADATA_CORRUPTION"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("TYPE_INFO_CORRUPTION"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("REFERENCE_COUNT_CORRUPTION"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("OBJECT_HEADER_CORRUPTION"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionType.valueOf("LAYOUT_CORRUPTION"));
    }
  }

  @Nested
  @DisplayName("CorruptionSeverity Enum Tests")
  class CorruptionSeverityEnumTests {

    @Test
    @DisplayName("should have all corruption severity levels")
    void shouldHaveAllCorruptionSeverityLevels() {
      final MemoryCorruptionAnalysis.CorruptionSeverity[] values =
          MemoryCorruptionAnalysis.CorruptionSeverity.values();
      assertEquals(5, values.length, "Should have 5 corruption severity levels");

      assertNotNull(MemoryCorruptionAnalysis.CorruptionSeverity.valueOf("POTENTIAL"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionSeverity.valueOf("MINOR"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionSeverity.valueOf("MODERATE"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionSeverity.valueOf("SERIOUS"));
      assertNotNull(MemoryCorruptionAnalysis.CorruptionSeverity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("LifecycleViolationType Enum Tests")
  class LifecycleViolationTypeEnumTests {

    @Test
    @DisplayName("should have all lifecycle violation types")
    void shouldHaveAllLifecycleViolationTypes() {
      final MemoryCorruptionAnalysis.LifecycleViolationType[] values =
          MemoryCorruptionAnalysis.LifecycleViolationType.values();
      assertEquals(5, values.length, "Should have 5 lifecycle violation types");

      assertNotNull(
          MemoryCorruptionAnalysis.LifecycleViolationType.valueOf("USE_AFTER_FINALIZATION"));
      assertNotNull(
          MemoryCorruptionAnalysis.LifecycleViolationType.valueOf("ACCESS_DURING_FINALIZATION"));
      assertNotNull(
          MemoryCorruptionAnalysis.LifecycleViolationType.valueOf("INVALID_STATE_TRANSITION"));
      assertNotNull(MemoryCorruptionAnalysis.LifecycleViolationType.valueOf("RESOURCE_LEAK"));
      assertNotNull(MemoryCorruptionAnalysis.LifecycleViolationType.valueOf("PREMATURE_CLEANUP"));
    }
  }

  @Nested
  @DisplayName("MemoryPermission Enum Tests")
  class MemoryPermissionEnumTests {

    @Test
    @DisplayName("should have all memory permissions")
    void shouldHaveAllMemoryPermissions() {
      final MemoryCorruptionAnalysis.MemoryPermission[] values =
          MemoryCorruptionAnalysis.MemoryPermission.values();
      assertEquals(3, values.length, "Should have 3 memory permissions");

      assertNotNull(MemoryCorruptionAnalysis.MemoryPermission.valueOf("READ"));
      assertNotNull(MemoryCorruptionAnalysis.MemoryPermission.valueOf("WRITE"));
      assertNotNull(MemoryCorruptionAnalysis.MemoryPermission.valueOf("EXECUTE"));
    }
  }

  @Nested
  @DisplayName("RecommendationType Enum Tests")
  class RecommendationTypeEnumTests {

    @Test
    @DisplayName("should have all recommendation types")
    void shouldHaveAllRecommendationTypes() {
      final MemoryCorruptionAnalysis.RecommendationType[] values =
          MemoryCorruptionAnalysis.RecommendationType.values();
      assertEquals(6, values.length, "Should have 6 recommendation types");

      assertNotNull(MemoryCorruptionAnalysis.RecommendationType.valueOf("ADD_BOUNDS_CHECKING"));
      assertNotNull(
          MemoryCorruptionAnalysis.RecommendationType.valueOf("IMPROVE_MEMORY_MANAGEMENT"));
      assertNotNull(
          MemoryCorruptionAnalysis.RecommendationType.valueOf("ADD_CORRUPTION_DETECTION"));
      assertNotNull(
          MemoryCorruptionAnalysis.RecommendationType.valueOf("FIX_LIFECYCLE_MANAGEMENT"));
      assertNotNull(
          MemoryCorruptionAnalysis.RecommendationType.valueOf("IMPLEMENT_MEMORY_BARRIERS"));
      assertNotNull(MemoryCorruptionAnalysis.RecommendationType.valueOf("ADD_INTEGRITY_CHECKING"));
    }
  }

  @Nested
  @DisplayName("CorruptionIssue Interface Tests")
  class CorruptionIssueTests {

    @Test
    @DisplayName("should have all corruption issue methods")
    void shouldHaveAllCorruptionIssueMethods() {
      final String[] expectedMethods = {
        "getIssueId",
        "getCorruptionType",
        "getSeverity",
        "getAffectedObjectId",
        "getMemoryAddress",
        "getDescription",
        "getDetectionMethod",
        "getConfidenceLevel",
        "getPotentialCauses",
        "getEvidence"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryCorruptionAnalysis.CorruptionIssue.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("MemoryIntegrityResult Interface Tests")
  class MemoryIntegrityResultTests {

    @Test
    @DisplayName("should have all memory integrity result methods")
    void shouldHaveAllMemoryIntegrityResultMethods() {
      final String[] expectedMethods = {
        "isIntegrityIntact",
        "getViolationCount",
        "getViolations",
        "getIntegrityScore",
        "getChecksumResults"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryCorruptionAnalysis.MemoryIntegrityResult.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("HeapConsistencyResult Interface Tests")
  class HeapConsistencyResultTests {

    @Test
    @DisplayName("should have all heap consistency result methods")
    void shouldHaveAllHeapConsistencyResultMethods() {
      final String[] expectedMethods = {
        "isConsistent",
        "getErrorCount",
        "getErrors",
        "getFreeListValidation",
        "getObjectGraphValidation"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(MemoryCorruptionAnalysis.HeapConsistencyResult.class, methodName),
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
    @DisplayName("should support corruption detection pattern")
    void shouldSupportCorruptionDetectionPattern() {
      // Documents usage:
      // if (analysis.isCorruptionDetected()) {
      //   List<CorruptionIssue> issues = analysis.getCorruptionIssues();
      //   for (CorruptionIssue issue : issues) { ... }
      // }
      assertTrue(
          hasMethod(MemoryCorruptionAnalysis.class, "isCorruptionDetected"),
          "Need isCorruptionDetected method");
      assertTrue(
          hasMethod(MemoryCorruptionAnalysis.class, "getCorruptionIssues"),
          "Need getCorruptionIssues method");
    }

    @Test
    @DisplayName("should support severity assessment pattern")
    void shouldSupportSeverityAssessmentPattern() {
      // Documents usage:
      // CorruptionSeverity severity = analysis.getCorruptionSeverity();
      // if (severity == CorruptionSeverity.CRITICAL) { ... }
      assertTrue(
          hasMethod(MemoryCorruptionAnalysis.class, "getCorruptionSeverity"),
          "Need getCorruptionSeverity method");
    }

    @Test
    @DisplayName("should support comprehensive analysis pattern")
    void shouldSupportComprehensiveAnalysisPattern() {
      // Documents usage:
      // MemoryIntegrityResult integrity = analysis.getIntegrityResult();
      // HeapConsistencyResult consistency = analysis.getConsistencyResult();
      // LifecycleViolationResult lifecycle = analysis.getLifecycleViolationResult();
      assertTrue(
          hasMethod(MemoryCorruptionAnalysis.class, "getIntegrityResult"),
          "Need getIntegrityResult method");
      assertTrue(
          hasMethod(MemoryCorruptionAnalysis.class, "getConsistencyResult"),
          "Need getConsistencyResult method");
      assertTrue(
          hasMethod(MemoryCorruptionAnalysis.class, "getLifecycleViolationResult"),
          "Need getLifecycleViolationResult method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Priority Enum Tests")
  class PriorityEnumTests {

    @Test
    @DisplayName("should have all priority levels")
    void shouldHaveAllPriorityLevels() {
      final MemoryCorruptionAnalysis.Priority[] values = MemoryCorruptionAnalysis.Priority.values();
      assertEquals(4, values.length, "Should have 4 priority levels");

      assertNotNull(MemoryCorruptionAnalysis.Priority.valueOf("LOW"));
      assertNotNull(MemoryCorruptionAnalysis.Priority.valueOf("MEDIUM"));
      assertNotNull(MemoryCorruptionAnalysis.Priority.valueOf("HIGH"));
      assertNotNull(MemoryCorruptionAnalysis.Priority.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("Severity Enum Tests")
  class SeverityEnumTests {

    @Test
    @DisplayName("should have ViolationSeverity enum")
    void shouldHaveViolationSeverityEnum() {
      final MemoryCorruptionAnalysis.ViolationSeverity[] values =
          MemoryCorruptionAnalysis.ViolationSeverity.values();
      assertEquals(4, values.length, "Should have 4 violation severity levels");
    }

    @Test
    @DisplayName("should have ErrorSeverity enum")
    void shouldHaveErrorSeverityEnum() {
      final MemoryCorruptionAnalysis.ErrorSeverity[] values =
          MemoryCorruptionAnalysis.ErrorSeverity.values();
      assertEquals(3, values.length, "Should have 3 error severity levels");
    }
  }
}
