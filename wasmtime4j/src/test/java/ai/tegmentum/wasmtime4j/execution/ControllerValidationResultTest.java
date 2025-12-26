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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ControllerValidationResult interface.
 *
 * <p>This test class verifies the interface structure, methods, nested types, and enums for
 * ControllerValidationResult using reflection-based testing.
 */
@DisplayName("ControllerValidationResult Tests")
class ControllerValidationResultTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ControllerValidationResult should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ControllerValidationResult.class.isInterface(),
          "ControllerValidationResult should be an interface");
    }

    @Test
    @DisplayName("ControllerValidationResult should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ControllerValidationResult.class.getModifiers()),
          "ControllerValidationResult should be public");
    }

    @Test
    @DisplayName("ControllerValidationResult should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ControllerValidationResult.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "ControllerValidationResult should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getErrors method")
    void shouldHaveGetErrorsMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getErrors");
      assertNotNull(method, "getErrors method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getWarnings method")
    void shouldHaveGetWarningsMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getValidatorVersion method")
    void shouldHaveGetValidatorVersionMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getValidatorVersion");
      assertNotNull(method, "getValidatorVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getContext method")
    void shouldHaveGetContextMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getContext");
      assertNotNull(method, "getContext method should exist");
      assertEquals(
          ControllerValidationResult.ValidationContext.class,
          method.getReturnType(),
          "Return type should be ValidationContext");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ControllerValidationResult.ValidationStatistics.class,
          method.getReturnType(),
          "Return type should be ValidationStatistics");
    }

    @Test
    @DisplayName("should have getSummary method")
    void shouldHaveGetSummaryMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getSummary");
      assertNotNull(method, "getSummary method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getDetailedReport method")
    void shouldHaveGetDetailedReportMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getDetailedReport");
      assertNotNull(method, "getDetailedReport method should exist");
      assertEquals(
          ControllerValidationResult.ValidationReport.class,
          method.getReturnType(),
          "Return type should be ValidationReport");
    }

    @Test
    @DisplayName("should have getRecommendations method")
    void shouldHaveGetRecommendationsMethod() throws NoSuchMethodException {
      Method method = ControllerValidationResult.class.getMethod("getRecommendations");
      assertNotNull(method, "getRecommendations method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }
  }

  // ========================================================================
  // ErrorSeverity Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ErrorSeverity Enum Tests")
  class ErrorSeverityTests {

    @Test
    @DisplayName("ErrorSeverity should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.ErrorSeverity.class.isEnum(),
          "ErrorSeverity should be an enum");
      assertTrue(
          ControllerValidationResult.ErrorSeverity.class.isMemberClass(),
          "ErrorSeverity should be a member class");
    }

    @Test
    @DisplayName("ErrorSeverity should have 4 values")
    void shouldHaveFourValues() {
      ControllerValidationResult.ErrorSeverity[] values =
          ControllerValidationResult.ErrorSeverity.values();
      assertEquals(4, values.length, "ErrorSeverity should have 4 values");
    }

    @Test
    @DisplayName("ErrorSeverity should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.ErrorSeverity severity :
          ControllerValidationResult.ErrorSeverity.values()) {
        actualNames.add(severity.name());
      }
      assertEquals(expectedNames, actualNames, "ErrorSeverity should have expected values");
    }
  }

  // ========================================================================
  // ErrorCategory Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ErrorCategory Enum Tests")
  class ErrorCategoryTests {

    @Test
    @DisplayName("ErrorCategory should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.ErrorCategory.class.isEnum(),
          "ErrorCategory should be an enum");
    }

    @Test
    @DisplayName("ErrorCategory should have 6 values")
    void shouldHaveSixValues() {
      ControllerValidationResult.ErrorCategory[] values =
          ControllerValidationResult.ErrorCategory.values();
      assertEquals(6, values.length, "ErrorCategory should have 6 values");
    }

    @Test
    @DisplayName("ErrorCategory should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of("CONFIGURATION", "RESOURCE", "SECURITY", "PERFORMANCE", "COMPATIBILITY", "LOGIC");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.ErrorCategory category :
          ControllerValidationResult.ErrorCategory.values()) {
        actualNames.add(category.name());
      }
      assertEquals(expectedNames, actualNames, "ErrorCategory should have expected values");
    }
  }

  // ========================================================================
  // WarningCategory Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WarningCategory Enum Tests")
  class WarningCategoryTests {

    @Test
    @DisplayName("WarningCategory should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.WarningCategory.class.isEnum(),
          "WarningCategory should be an enum");
    }

    @Test
    @DisplayName("WarningCategory should have 5 values")
    void shouldHaveFiveValues() {
      ControllerValidationResult.WarningCategory[] values =
          ControllerValidationResult.WarningCategory.values();
      assertEquals(5, values.length, "WarningCategory should have 5 values");
    }

    @Test
    @DisplayName("WarningCategory should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of("PERFORMANCE", "SECURITY", "COMPATIBILITY", "BEST_PRACTICE", "DEPRECATION");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.WarningCategory category :
          ControllerValidationResult.WarningCategory.values()) {
        actualNames.add(category.name());
      }
      assertEquals(expectedNames, actualNames, "WarningCategory should have expected values");
    }
  }

  // ========================================================================
  // ValidationType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ValidationType Enum Tests")
  class ValidationTypeTests {

    @Test
    @DisplayName("ValidationType should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.ValidationType.class.isEnum(),
          "ValidationType should be an enum");
    }

    @Test
    @DisplayName("ValidationType should have 5 values")
    void shouldHaveFiveValues() {
      ControllerValidationResult.ValidationType[] values =
          ControllerValidationResult.ValidationType.values();
      assertEquals(5, values.length, "ValidationType should have 5 values");
    }

    @Test
    @DisplayName("ValidationType should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of("FULL", "QUICK", "SECURITY", "PERFORMANCE", "CONFIGURATION");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.ValidationType type :
          ControllerValidationResult.ValidationType.values()) {
        actualNames.add(type.name());
      }
      assertEquals(expectedNames, actualNames, "ValidationType should have expected values");
    }
  }

  // ========================================================================
  // ValidationScope Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ValidationScope Enum Tests")
  class ValidationScopeTests {

    @Test
    @DisplayName("ValidationScope should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.ValidationScope.class.isEnum(),
          "ValidationScope should be an enum");
    }

    @Test
    @DisplayName("ValidationScope should have 4 values")
    void shouldHaveFourValues() {
      ControllerValidationResult.ValidationScope[] values =
          ControllerValidationResult.ValidationScope.values();
      assertEquals(4, values.length, "ValidationScope should have 4 values");
    }

    @Test
    @DisplayName("ValidationScope should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("CONTROLLER", "CONFIGURATION", "RESOURCES", "ALL");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.ValidationScope scope :
          ControllerValidationResult.ValidationScope.values()) {
        actualNames.add(scope.name());
      }
      assertEquals(expectedNames, actualNames, "ValidationScope should have expected values");
    }
  }

  // ========================================================================
  // ReportFormat Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ReportFormat Enum Tests")
  class ReportFormatTests {

    @Test
    @DisplayName("ReportFormat should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.ReportFormat.class.isEnum(), "ReportFormat should be an enum");
    }

    @Test
    @DisplayName("ReportFormat should have 4 values")
    void shouldHaveFourValues() {
      ControllerValidationResult.ReportFormat[] values =
          ControllerValidationResult.ReportFormat.values();
      assertEquals(4, values.length, "ReportFormat should have 4 values");
    }

    @Test
    @DisplayName("ReportFormat should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("TEXT", "HTML", "JSON", "XML");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.ReportFormat format :
          ControllerValidationResult.ReportFormat.values()) {
        actualNames.add(format.name());
      }
      assertEquals(expectedNames, actualNames, "ReportFormat should have expected values");
    }
  }

  // ========================================================================
  // ExportFormat Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExportFormat Enum Tests")
  class ExportFormatTests {

    @Test
    @DisplayName("ExportFormat should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.ExportFormat.class.isEnum(), "ExportFormat should be an enum");
    }

    @Test
    @DisplayName("ExportFormat should have 4 values")
    void shouldHaveFourValues() {
      ControllerValidationResult.ExportFormat[] values =
          ControllerValidationResult.ExportFormat.values();
      assertEquals(4, values.length, "ExportFormat should have 4 values");
    }

    @Test
    @DisplayName("ExportFormat should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("PDF", "HTML", "JSON", "CSV");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.ExportFormat format :
          ControllerValidationResult.ExportFormat.values()) {
        actualNames.add(format.name());
      }
      assertEquals(expectedNames, actualNames, "ExportFormat should have expected values");
    }
  }

  // ========================================================================
  // RecommendationPriority Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("RecommendationPriority Enum Tests")
  class RecommendationPriorityTests {

    @Test
    @DisplayName("RecommendationPriority should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.RecommendationPriority.class.isEnum(),
          "RecommendationPriority should be an enum");
    }

    @Test
    @DisplayName("RecommendationPriority should have 4 values")
    void shouldHaveFourValues() {
      ControllerValidationResult.RecommendationPriority[] values =
          ControllerValidationResult.RecommendationPriority.values();
      assertEquals(4, values.length, "RecommendationPriority should have 4 values");
    }

    @Test
    @DisplayName("RecommendationPriority should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.RecommendationPriority priority :
          ControllerValidationResult.RecommendationPriority.values()) {
        actualNames.add(priority.name());
      }
      assertEquals(
          expectedNames, actualNames, "RecommendationPriority should have expected values");
    }
  }

  // ========================================================================
  // ImpactLevel Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ImpactLevel Enum Tests")
  class ImpactLevelTests {

    @Test
    @DisplayName("ImpactLevel should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ControllerValidationResult.ImpactLevel.class.isEnum(), "ImpactLevel should be an enum");
    }

    @Test
    @DisplayName("ImpactLevel should have 5 values")
    void shouldHaveFiveValues() {
      ControllerValidationResult.ImpactLevel[] values =
          ControllerValidationResult.ImpactLevel.values();
      assertEquals(5, values.length, "ImpactLevel should have 5 values");
    }

    @Test
    @DisplayName("ImpactLevel should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("MINIMAL", "LOW", "MEDIUM", "HIGH", "SIGNIFICANT");
      Set<String> actualNames = new HashSet<>();
      for (ControllerValidationResult.ImpactLevel level :
          ControllerValidationResult.ImpactLevel.values()) {
        actualNames.add(level.name());
      }
      assertEquals(expectedNames, actualNames, "ImpactLevel should have expected values");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("ValidationError should be a nested interface")
    void validationErrorShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.ValidationError.class.isInterface(),
          "ValidationError should be an interface");
      assertTrue(
          ControllerValidationResult.ValidationError.class.isMemberClass(),
          "ValidationError should be a member class");
    }

    @Test
    @DisplayName("ValidationWarning should be a nested interface")
    void validationWarningShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.ValidationWarning.class.isInterface(),
          "ValidationWarning should be an interface");
    }

    @Test
    @DisplayName("ValidationContext should be a nested interface")
    void validationContextShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.ValidationContext.class.isInterface(),
          "ValidationContext should be an interface");
    }

    @Test
    @DisplayName("ValidationStatistics should be a nested interface")
    void validationStatisticsShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.ValidationStatistics.class.isInterface(),
          "ValidationStatistics should be an interface");
    }

    @Test
    @DisplayName("ValidationReport should be a nested interface")
    void validationReportShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.ValidationReport.class.isInterface(),
          "ValidationReport should be an interface");
    }

    @Test
    @DisplayName("ValidationRecommendation should be a nested interface")
    void validationRecommendationShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.ValidationRecommendation.class.isInterface(),
          "ValidationRecommendation should be an interface");
    }

    @Test
    @DisplayName("EnvironmentInfo should be a nested interface")
    void environmentInfoShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.EnvironmentInfo.class.isInterface(),
          "EnvironmentInfo should be an interface");
    }

    @Test
    @DisplayName("ValidationPerformanceMetrics should be a nested interface")
    void validationPerformanceMetricsShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.ValidationPerformanceMetrics.class.isInterface(),
          "ValidationPerformanceMetrics should be an interface");
    }

    @Test
    @DisplayName("ReportSection should be a nested interface")
    void reportSectionShouldBeNestedInterface() {
      assertTrue(
          ControllerValidationResult.ReportSection.class.isInterface(),
          "ReportSection should be an interface");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ControllerValidationResult should have 9 nested enums")
    void shouldHaveNineNestedEnums() {
      Class<?>[] nestedClasses = ControllerValidationResult.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(9, enumCount, "ControllerValidationResult should have 9 nested enums");
    }

    @Test
    @DisplayName("ControllerValidationResult should have 9 nested interfaces")
    void shouldHaveNineNestedInterfaces() {
      Class<?>[] nestedClasses = ControllerValidationResult.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(9, interfaceCount, "ControllerValidationResult should have 9 nested interfaces");
    }
  }
}
