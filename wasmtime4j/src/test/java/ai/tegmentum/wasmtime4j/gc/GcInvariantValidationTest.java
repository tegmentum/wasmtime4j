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
 * Tests for {@link GcInvariantValidation} interface.
 *
 * <p>GcInvariantValidation validates critical invariants that must hold for the garbage collector
 * to function correctly, including type safety, reference consistency, heap integrity, and other
 * fundamental assumptions.
 */
@DisplayName("GcInvariantValidation Tests")
class GcInvariantValidationTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          GcInvariantValidation.class.isInterface(),
          "GcInvariantValidation should be an interface");
    }
  }

  @Nested
  @DisplayName("Core Method Tests")
  class CoreMethodTests {

    @Test
    @DisplayName("should have areAllInvariantsSatisfied method")
    void shouldHaveAreAllInvariantsSatisfiedMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("areAllInvariantsSatisfied");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getTotalInvariantCount method")
    void shouldHaveGetTotalInvariantCountMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("getTotalInvariantCount");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getViolationCount method")
    void shouldHaveGetViolationCountMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("getViolationCount");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getViolations method")
    void shouldHaveGetViolationsMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("getViolations");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getSatisfactionScore method")
    void shouldHaveGetSatisfactionScoreMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("getSatisfactionScore");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getCategoryResults method")
    void shouldHaveGetCategoryResultsMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("getCategoryResults");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getCriticalInvariants method")
    void shouldHaveGetCriticalInvariantsMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("getCriticalInvariants");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getPerformanceImpact method")
    void shouldHaveGetPerformanceImpactMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("getPerformanceImpact");
      assertEquals(
          GcInvariantValidation.ValidationPerformanceImpact.class,
          method.getReturnType(),
          "Should return ValidationPerformanceImpact");
    }

    @Test
    @DisplayName("should have getSpecificValidators method")
    void shouldHaveGetSpecificValidatorsMethod() throws NoSuchMethodException {
      final Method method = GcInvariantValidation.class.getMethod("getSpecificValidators");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("InvariantCategory Enum Tests")
  class InvariantCategoryEnumTests {

    @Test
    @DisplayName("should have all invariant categories")
    void shouldHaveAllInvariantCategories() {
      final GcInvariantValidation.InvariantCategory[] values =
          GcInvariantValidation.InvariantCategory.values();
      assertEquals(8, values.length, "Should have 8 invariant categories");

      assertNotNull(GcInvariantValidation.InvariantCategory.valueOf("TYPE_SAFETY"));
      assertNotNull(GcInvariantValidation.InvariantCategory.valueOf("REFERENCE_CONSISTENCY"));
      assertNotNull(GcInvariantValidation.InvariantCategory.valueOf("MEMORY_LAYOUT"));
      assertNotNull(GcInvariantValidation.InvariantCategory.valueOf("OBJECT_LIFECYCLE"));
      assertNotNull(GcInvariantValidation.InvariantCategory.valueOf("HEAP_STRUCTURE"));
      assertNotNull(GcInvariantValidation.InvariantCategory.valueOf("COLLECTION_INVARIANTS"));
      assertNotNull(GcInvariantValidation.InvariantCategory.valueOf("THREAD_SAFETY"));
      assertNotNull(GcInvariantValidation.InvariantCategory.valueOf("PERFORMANCE"));
    }
  }

  @Nested
  @DisplayName("ViolationSeverity Enum Tests")
  class ViolationSeverityEnumTests {

    @Test
    @DisplayName("should have all violation severity levels")
    void shouldHaveAllViolationSeverityLevels() {
      final GcInvariantValidation.ViolationSeverity[] values =
          GcInvariantValidation.ViolationSeverity.values();
      assertEquals(4, values.length, "Should have 4 violation severity levels");

      assertNotNull(GcInvariantValidation.ViolationSeverity.valueOf("INFO"));
      assertNotNull(GcInvariantValidation.ViolationSeverity.valueOf("WARNING"));
      assertNotNull(GcInvariantValidation.ViolationSeverity.valueOf("ERROR"));
      assertNotNull(GcInvariantValidation.ViolationSeverity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("CheckFrequency Enum Tests")
  class CheckFrequencyEnumTests {

    @Test
    @DisplayName("should have all check frequency values")
    void shouldHaveAllCheckFrequencyValues() {
      final GcInvariantValidation.CheckFrequency[] values =
          GcInvariantValidation.CheckFrequency.values();
      assertEquals(5, values.length, "Should have 5 check frequency values");

      assertNotNull(GcInvariantValidation.CheckFrequency.valueOf("ALWAYS"));
      assertNotNull(GcInvariantValidation.CheckFrequency.valueOf("PERIODIC"));
      assertNotNull(GcInvariantValidation.CheckFrequency.valueOf("GC_ONLY"));
      assertNotNull(GcInvariantValidation.CheckFrequency.valueOf("ON_DEMAND"));
      assertNotNull(GcInvariantValidation.CheckFrequency.valueOf("DEBUG_ONLY"));
    }
  }

  @Nested
  @DisplayName("CriticalityLevel Enum Tests")
  class CriticalityLevelEnumTests {

    @Test
    @DisplayName("should have all criticality levels")
    void shouldHaveAllCriticalityLevels() {
      final GcInvariantValidation.CriticalityLevel[] values =
          GcInvariantValidation.CriticalityLevel.values();
      assertEquals(4, values.length, "Should have 4 criticality levels");

      assertNotNull(GcInvariantValidation.CriticalityLevel.valueOf("ABSOLUTELY_CRITICAL"));
      assertNotNull(GcInvariantValidation.CriticalityLevel.valueOf("HIGHLY_CRITICAL"));
      assertNotNull(GcInvariantValidation.CriticalityLevel.valueOf("MODERATELY_CRITICAL"));
      assertNotNull(GcInvariantValidation.CriticalityLevel.valueOf("LOW_CRITICAL"));
    }
  }

  @Nested
  @DisplayName("InvariantViolation Interface Tests")
  class InvariantViolationTests {

    @Test
    @DisplayName("should have all invariant violation methods")
    void shouldHaveAllInvariantViolationMethods() {
      final String[] expectedMethods = {
        "getViolationId",
        "getInvariantName",
        "getCategory",
        "getSeverity",
        "getDescription",
        "getAffectedObjects",
        "getContext",
        "getExpectedCondition",
        "getActualCondition",
        "getRemediationActions"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcInvariantValidation.InvariantViolation.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ViolationContext Interface Tests")
  class ViolationContextTests {

    @Test
    @DisplayName("should have all violation context methods")
    void shouldHaveAllViolationContextMethods() {
      final String[] expectedMethods = {
        "getTriggeringOperation", "getThreadId", "getCallStack", "getGcPhase", "getContextData"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcInvariantValidation.ViolationContext.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("CategoryValidation Interface Tests")
  class CategoryValidationTests {

    @Test
    @DisplayName("should have all category validation methods")
    void shouldHaveAllCategoryValidationMethods() {
      final String[] expectedMethods = {
        "getCategory",
        "isAllSatisfied",
        "getInvariantCount",
        "getViolationCount",
        "getSatisfactionScore",
        "getInvariantResults"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcInvariantValidation.CategoryValidation.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("CriticalInvariantResult Interface Tests")
  class CriticalInvariantResultTests {

    @Test
    @DisplayName("should have all critical invariant result methods")
    void shouldHaveAllCriticalInvariantResultMethods() {
      final String[] expectedMethods = {
        "getInvariantName",
        "holds",
        "getCriticalityLevel",
        "getFailureImpact",
        "getCheckImplementation",
        "getLastSuccessfulCheck"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcInvariantValidation.CriticalInvariantResult.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ValidationPerformanceImpact Interface Tests")
  class ValidationPerformanceImpactTests {

    @Test
    @DisplayName("should have all validation performance impact methods")
    void shouldHaveAllValidationPerformanceImpactMethods() {
      final String[] expectedMethods = {
        "getTotalValidationTime",
        "getValidationOverheadPercentage",
        "getTimeByCategory",
        "getMostExpensiveInvariants",
        "getOptimizationRecommendations"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcInvariantValidation.ValidationPerformanceImpact.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Specific Validator Interface Tests")
  class SpecificValidatorInterfaceTests {

    @Test
    @DisplayName("should have TypeSafetyInvariants interface")
    void shouldHaveTypeSafetyInvariantsInterface() {
      assertTrue(
          GcInvariantValidation.TypeSafetyInvariants.class.isInterface(),
          "TypeSafetyInvariants should be an interface");
    }

    @Test
    @DisplayName("should have ReferenceConsistencyInvariants interface")
    void shouldHaveReferenceConsistencyInvariantsInterface() {
      assertTrue(
          GcInvariantValidation.ReferenceConsistencyInvariants.class.isInterface(),
          "ReferenceConsistencyInvariants should be an interface");
    }

    @Test
    @DisplayName("should have MemoryLayoutInvariants interface")
    void shouldHaveMemoryLayoutInvariantsInterface() {
      assertTrue(
          GcInvariantValidation.MemoryLayoutInvariants.class.isInterface(),
          "MemoryLayoutInvariants should be an interface");
    }

    @Test
    @DisplayName("should have ObjectLifecycleInvariants interface")
    void shouldHaveObjectLifecycleInvariantsInterface() {
      assertTrue(
          GcInvariantValidation.ObjectLifecycleInvariants.class.isInterface(),
          "ObjectLifecycleInvariants should be an interface");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support invariant validation pattern")
    void shouldSupportInvariantValidationPattern() {
      // Documents usage:
      // if (!validation.areAllInvariantsSatisfied()) {
      //   List<InvariantViolation> violations = validation.getViolations();
      //   for (InvariantViolation violation : violations) { ... }
      // }
      assertTrue(
          hasMethod(GcInvariantValidation.class, "areAllInvariantsSatisfied"),
          "Need areAllInvariantsSatisfied method");
      assertTrue(
          hasMethod(GcInvariantValidation.class, "getViolations"), "Need getViolations method");
    }

    @Test
    @DisplayName("should support category-based validation pattern")
    void shouldSupportCategoryBasedValidationPattern() {
      // Documents usage:
      // Map<InvariantCategory, CategoryValidation> results = validation.getCategoryResults();
      // CategoryValidation typeResults = results.get(InvariantCategory.TYPE_SAFETY);
      assertTrue(
          hasMethod(GcInvariantValidation.class, "getCategoryResults"),
          "Need getCategoryResults method");
    }

    @Test
    @DisplayName("should support critical invariant monitoring pattern")
    void shouldSupportCriticalInvariantMonitoringPattern() {
      // Documents usage:
      // List<CriticalInvariantResult> critical = validation.getCriticalInvariants();
      // for (CriticalInvariantResult result : critical) {
      //   if (!result.holds()) { ... }
      // }
      assertTrue(
          hasMethod(GcInvariantValidation.class, "getCriticalInvariants"),
          "Need getCriticalInvariants method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
