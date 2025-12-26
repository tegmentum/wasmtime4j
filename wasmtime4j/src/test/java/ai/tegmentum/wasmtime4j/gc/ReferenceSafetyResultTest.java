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
 * Tests for {@link ReferenceSafetyResult} interface.
 *
 * <p>ReferenceSafetyResult provides comprehensive analysis of reference safety, including type
 * safety violations, dangling references, null pointer access patterns, and other safety concerns.
 */
@DisplayName("ReferenceSafetyResult Tests")
class ReferenceSafetyResultTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ReferenceSafetyResult.class.isInterface(),
          "ReferenceSafetyResult should be an interface");
    }
  }

  @Nested
  @DisplayName("Core Method Tests")
  class CoreMethodTests {

    @Test
    @DisplayName("should have isAllSafe method")
    void shouldHaveIsAllSafeMethod() throws NoSuchMethodException {
      final Method method = ReferenceSafetyResult.class.getMethod("isAllSafe");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getTotalReferencesValidated method")
    void shouldHaveGetTotalReferencesValidatedMethod() throws NoSuchMethodException {
      final Method method = ReferenceSafetyResult.class.getMethod("getTotalReferencesValidated");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getViolationCount method")
    void shouldHaveGetViolationCountMethod() throws NoSuchMethodException {
      final Method method = ReferenceSafetyResult.class.getMethod("getViolationCount");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getSafetyViolations method")
    void shouldHaveGetSafetyViolationsMethod() throws NoSuchMethodException {
      final Method method = ReferenceSafetyResult.class.getMethod("getSafetyViolations");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getSafetyScore method")
    void shouldHaveGetSafetyScoreMethod() throws NoSuchMethodException {
      final Method method = ReferenceSafetyResult.class.getMethod("getSafetyScore");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getViolationStatistics method")
    void shouldHaveGetViolationStatisticsMethod() throws NoSuchMethodException {
      final Method method = ReferenceSafetyResult.class.getMethod("getViolationStatistics");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getRecommendations method")
    void shouldHaveGetRecommendationsMethod() throws NoSuchMethodException {
      final Method method = ReferenceSafetyResult.class.getMethod("getRecommendations");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getDangerousPatterns method")
    void shouldHaveGetDangerousPatternsMethod() throws NoSuchMethodException {
      final Method method = ReferenceSafetyResult.class.getMethod("getDangerousPatterns");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("SafetyViolation Interface Tests")
  class SafetyViolationTests {

    @Test
    @DisplayName("should have all safety violation methods")
    void shouldHaveAllSafetyViolationMethods() {
      final String[] expectedMethods = {
        "getViolationId",
        "getViolationType",
        "getSeverity",
        "getObjectId",
        "getReferencePath",
        "getDescription",
        "getExpectedType",
        "getActualType",
        "getSuggestedFixes"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ReferenceSafetyResult.SafetyViolation.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ViolationType Enum Tests")
  class ViolationTypeEnumTests {

    @Test
    @DisplayName("should have all violation types")
    void shouldHaveAllViolationTypes() {
      final ReferenceSafetyResult.ViolationType[] values =
          ReferenceSafetyResult.ViolationType.values();
      assertEquals(9, values.length, "Should have 9 violation types");

      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("TYPE_CAST_VIOLATION"));
      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("NULL_REFERENCE_ACCESS"));
      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("DANGLING_REFERENCE"));
      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("TYPE_ASSIGNMENT_MISMATCH"));
      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("INVALID_FIELD_ACCESS"));
      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("INVALID_ARRAY_ACCESS"));
      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("FINALIZED_OBJECT_REFERENCE"));
      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("CIRCULAR_DEPENDENCY"));
      assertNotNull(ReferenceSafetyResult.ViolationType.valueOf("CROSS_MODULE_VIOLATION"));
    }
  }

  @Nested
  @DisplayName("ViolationSeverity Enum Tests")
  class ViolationSeverityEnumTests {

    @Test
    @DisplayName("should have all violation severity levels")
    void shouldHaveAllViolationSeverityLevels() {
      final ReferenceSafetyResult.ViolationSeverity[] values =
          ReferenceSafetyResult.ViolationSeverity.values();
      assertEquals(4, values.length, "Should have 4 violation severity levels");

      assertNotNull(ReferenceSafetyResult.ViolationSeverity.valueOf("INFO"));
      assertNotNull(ReferenceSafetyResult.ViolationSeverity.valueOf("WARNING"));
      assertNotNull(ReferenceSafetyResult.ViolationSeverity.valueOf("ERROR"));
      assertNotNull(ReferenceSafetyResult.ViolationSeverity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("RiskLevel Enum Tests")
  class RiskLevelEnumTests {

    @Test
    @DisplayName("should have all risk levels")
    void shouldHaveAllRiskLevels() {
      final ReferenceSafetyResult.RiskLevel[] values = ReferenceSafetyResult.RiskLevel.values();
      assertEquals(4, values.length, "Should have 4 risk levels");

      assertNotNull(ReferenceSafetyResult.RiskLevel.valueOf("LOW"));
      assertNotNull(ReferenceSafetyResult.RiskLevel.valueOf("MEDIUM"));
      assertNotNull(ReferenceSafetyResult.RiskLevel.valueOf("HIGH"));
      assertNotNull(ReferenceSafetyResult.RiskLevel.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("RecommendationType Enum Tests")
  class RecommendationTypeEnumTests {

    @Test
    @DisplayName("should have all recommendation types")
    void shouldHaveAllRecommendationTypes() {
      final ReferenceSafetyResult.RecommendationType[] values =
          ReferenceSafetyResult.RecommendationType.values();
      assertEquals(7, values.length, "Should have 7 recommendation types");

      assertNotNull(ReferenceSafetyResult.RecommendationType.valueOf("ADD_TYPE_VALIDATION"));
      assertNotNull(ReferenceSafetyResult.RecommendationType.valueOf("ADD_NULL_CHECKS"));
      assertNotNull(ReferenceSafetyResult.RecommendationType.valueOf("FIX_TYPE_CASTING"));
      assertNotNull(ReferenceSafetyResult.RecommendationType.valueOf("IMPROVE_LIFECYCLE"));
      assertNotNull(ReferenceSafetyResult.RecommendationType.valueOf("ADD_BOUNDS_CHECKING"));
      assertNotNull(ReferenceSafetyResult.RecommendationType.valueOf("IMPLEMENT_CLEANUP"));
      assertNotNull(ReferenceSafetyResult.RecommendationType.valueOf("USE_SAFER_PATTERNS"));
    }
  }

  @Nested
  @DisplayName("ImplementationDifficulty Enum Tests")
  class ImplementationDifficultyEnumTests {

    @Test
    @DisplayName("should have all implementation difficulty levels")
    void shouldHaveAllImplementationDifficultyLevels() {
      final ReferenceSafetyResult.ImplementationDifficulty[] values =
          ReferenceSafetyResult.ImplementationDifficulty.values();
      assertEquals(4, values.length, "Should have 4 implementation difficulty levels");

      assertNotNull(ReferenceSafetyResult.ImplementationDifficulty.valueOf("EASY"));
      assertNotNull(ReferenceSafetyResult.ImplementationDifficulty.valueOf("MODERATE"));
      assertNotNull(ReferenceSafetyResult.ImplementationDifficulty.valueOf("DIFFICULT"));
      assertNotNull(ReferenceSafetyResult.ImplementationDifficulty.valueOf("VERY_DIFFICULT"));
    }
  }

  @Nested
  @DisplayName("RecommendationPriority Enum Tests")
  class RecommendationPriorityEnumTests {

    @Test
    @DisplayName("should have all recommendation priority levels")
    void shouldHaveAllRecommendationPriorityLevels() {
      final ReferenceSafetyResult.RecommendationPriority[] values =
          ReferenceSafetyResult.RecommendationPriority.values();
      assertEquals(4, values.length, "Should have 4 recommendation priority levels");

      assertNotNull(ReferenceSafetyResult.RecommendationPriority.valueOf("CRITICAL"));
      assertNotNull(ReferenceSafetyResult.RecommendationPriority.valueOf("HIGH"));
      assertNotNull(ReferenceSafetyResult.RecommendationPriority.valueOf("MEDIUM"));
      assertNotNull(ReferenceSafetyResult.RecommendationPriority.valueOf("LOW"));
    }
  }

  @Nested
  @DisplayName("DangerousReferencePattern Interface Tests")
  class DangerousReferencePatternTests {

    @Test
    @DisplayName("should have all dangerous reference pattern methods")
    void shouldHaveAllDangerousReferencePatternMethods() {
      final String[] expectedMethods = {
        "getPatternName",
        "getDescription",
        "getInvolvedObjects",
        "getRiskLevel",
        "getProblemLikelihood",
        "getMitigationStrategies"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ReferenceSafetyResult.DangerousReferencePattern.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("SafetyRecommendation Interface Tests")
  class SafetyRecommendationTests {

    @Test
    @DisplayName("should have all safety recommendation methods")
    void shouldHaveAllSafetyRecommendationMethods() {
      final String[] expectedMethods = {
        "getType",
        "getDescription",
        "getAffectedObjects",
        "getExpectedImpact",
        "getDifficulty",
        "getPriority"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ReferenceSafetyResult.SafetyRecommendation.class, methodName),
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
    @DisplayName("should support safety validation pattern")
    void shouldSupportSafetyValidationPattern() {
      // Documents usage:
      // if (!result.isAllSafe()) {
      //   List<SafetyViolation> violations = result.getSafetyViolations();
      //   for (SafetyViolation violation : violations) { ... }
      // }
      assertTrue(hasMethod(ReferenceSafetyResult.class, "isAllSafe"), "Need isAllSafe method");
      assertTrue(
          hasMethod(ReferenceSafetyResult.class, "getSafetyViolations"),
          "Need getSafetyViolations method");
    }

    @Test
    @DisplayName("should support safety score monitoring")
    void shouldSupportSafetyScoreMonitoring() {
      // Documents usage:
      // double score = result.getSafetyScore();
      // if (score < 0.9) { ... }
      assertTrue(
          hasMethod(ReferenceSafetyResult.class, "getSafetyScore"), "Need getSafetyScore method");
    }

    @Test
    @DisplayName("should support violation statistics analysis")
    void shouldSupportViolationStatisticsAnalysis() {
      // Documents usage:
      // Map<ViolationType, Integer> stats = result.getViolationStatistics();
      // for (Entry<ViolationType, Integer> entry : stats.entrySet()) { ... }
      assertTrue(
          hasMethod(ReferenceSafetyResult.class, "getViolationStatistics"),
          "Need getViolationStatistics method");
    }

    @Test
    @DisplayName("should support dangerous pattern detection")
    void shouldSupportDangerousPatternDetection() {
      // Documents usage:
      // List<DangerousReferencePattern> patterns = result.getDangerousPatterns();
      // for (DangerousReferencePattern pattern : patterns) { ... }
      assertTrue(
          hasMethod(ReferenceSafetyResult.class, "getDangerousPatterns"),
          "Need getDangerousPatterns method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("WASM GC Specification Compliance Tests")
  class WasmGcSpecificationComplianceTests {

    @Test
    @DisplayName("should have all required reference safety result methods")
    void shouldHaveAllRequiredReferenceSafetyResultMethods() {
      final String[] expectedMethods = {
        "isAllSafe",
        "getTotalReferencesValidated",
        "getViolationCount",
        "getSafetyViolations",
        "getSafetyScore",
        "getViolationStatistics",
        "getRecommendations",
        "getDangerousPatterns"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(ReferenceSafetyResult.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
