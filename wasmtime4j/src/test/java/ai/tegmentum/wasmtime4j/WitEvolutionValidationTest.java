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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitEvolutionValidation;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEvolutionValidation} class.
 *
 * <p>WitEvolutionValidation provides comprehensive validation information for interface evolution
 * operations, including constraint violations, compatibility issues, and evolution recommendations.
 */
@DisplayName("WitEvolutionValidation Tests")
class WitEvolutionValidationTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WitEvolutionValidation.class.getModifiers()),
          "WitEvolutionValidation should be public");
      assertTrue(
          Modifier.isFinal(WitEvolutionValidation.class.getModifiers()),
          "WitEvolutionValidation should be final");
    }

    @Test
    @DisplayName("ConstraintViolation should be public static final class")
    void constraintViolationShouldBePublicStaticFinalClass() {
      assertTrue(
          Modifier.isPublic(WitEvolutionValidation.ConstraintViolation.class.getModifiers()),
          "ConstraintViolation should be public");
      assertTrue(
          Modifier.isStatic(WitEvolutionValidation.ConstraintViolation.class.getModifiers()),
          "ConstraintViolation should be static");
      assertTrue(
          Modifier.isFinal(WitEvolutionValidation.ConstraintViolation.class.getModifiers()),
          "ConstraintViolation should be final");
    }

    @Test
    @DisplayName("CompatibilityIssue should be public static final class")
    void compatibilityIssueShouldBePublicStaticFinalClass() {
      assertTrue(
          Modifier.isPublic(WitEvolutionValidation.CompatibilityIssue.class.getModifiers()),
          "CompatibilityIssue should be public");
      assertTrue(
          Modifier.isStatic(WitEvolutionValidation.CompatibilityIssue.class.getModifiers()),
          "CompatibilityIssue should be static");
      assertTrue(
          Modifier.isFinal(WitEvolutionValidation.CompatibilityIssue.class.getModifiers()),
          "CompatibilityIssue should be final");
    }

    @Test
    @DisplayName("EvolutionWarning should be public static final class")
    void evolutionWarningShouldBePublicStaticFinalClass() {
      assertTrue(
          Modifier.isPublic(WitEvolutionValidation.EvolutionWarning.class.getModifiers()),
          "EvolutionWarning should be public");
      assertTrue(
          Modifier.isStatic(WitEvolutionValidation.EvolutionWarning.class.getModifiers()),
          "EvolutionWarning should be static");
      assertTrue(
          Modifier.isFinal(WitEvolutionValidation.EvolutionWarning.class.getModifiers()),
          "EvolutionWarning should be final");
    }
  }

  @Nested
  @DisplayName("Enum Tests")
  class EnumTests {

    @Test
    @DisplayName("ViolationType should have expected values")
    void violationTypeShouldHaveExpectedValues() {
      final var values = WitEvolutionValidation.ViolationType.values();
      assertEquals(5, values.length);

      assertNotNull(WitEvolutionValidation.ViolationType.SEMANTIC_VERSION_VIOLATION);
      assertNotNull(WitEvolutionValidation.ViolationType.BACKWARD_COMPATIBILITY_VIOLATION);
      assertNotNull(WitEvolutionValidation.ViolationType.TYPE_SAFETY_VIOLATION);
      assertNotNull(WitEvolutionValidation.ViolationType.INTERFACE_CONTRACT_VIOLATION);
      assertNotNull(WitEvolutionValidation.ViolationType.DEPENDENCY_VIOLATION);
    }

    @Test
    @DisplayName("ViolationSeverity should have expected values")
    void violationSeverityShouldHaveExpectedValues() {
      final var values = WitEvolutionValidation.ViolationSeverity.values();
      assertEquals(4, values.length);

      assertNotNull(WitEvolutionValidation.ViolationSeverity.LOW);
      assertNotNull(WitEvolutionValidation.ViolationSeverity.MEDIUM);
      assertNotNull(WitEvolutionValidation.ViolationSeverity.HIGH);
      assertNotNull(WitEvolutionValidation.ViolationSeverity.CRITICAL);
    }

    @Test
    @DisplayName("IssueType should have expected values")
    void issueTypeShouldHaveExpectedValues() {
      final var values = WitEvolutionValidation.IssueType.values();
      assertEquals(5, values.length);

      assertNotNull(WitEvolutionValidation.IssueType.TYPE_MISMATCH);
      assertNotNull(WitEvolutionValidation.IssueType.MISSING_FUNCTION);
      assertNotNull(WitEvolutionValidation.IssueType.INCOMPATIBLE_SIGNATURE);
      assertNotNull(WitEvolutionValidation.IssueType.VERSION_CONFLICT);
      assertNotNull(WitEvolutionValidation.IssueType.DEPENDENCY_ISSUE);
    }

    @Test
    @DisplayName("IssueSeverity should have expected values")
    void issueSeverityShouldHaveExpectedValues() {
      final var values = WitEvolutionValidation.IssueSeverity.values();
      assertEquals(4, values.length);

      assertNotNull(WitEvolutionValidation.IssueSeverity.INFO);
      assertNotNull(WitEvolutionValidation.IssueSeverity.WARNING);
      assertNotNull(WitEvolutionValidation.IssueSeverity.ERROR);
      assertNotNull(WitEvolutionValidation.IssueSeverity.CRITICAL);
    }

    @Test
    @DisplayName("WarningType should have expected values")
    void warningTypeShouldHaveExpectedValues() {
      final var values = WitEvolutionValidation.WarningType.values();
      assertEquals(5, values.length);

      assertNotNull(WitEvolutionValidation.WarningType.PERFORMANCE_WARNING);
      assertNotNull(WitEvolutionValidation.WarningType.DEPRECATION_WARNING);
      assertNotNull(WitEvolutionValidation.WarningType.COMPATIBILITY_WARNING);
      assertNotNull(WitEvolutionValidation.WarningType.BEST_PRACTICE_WARNING);
      assertNotNull(WitEvolutionValidation.WarningType.SECURITY_WARNING);
    }

    @Test
    @DisplayName("EvolutionRisk should have expected values")
    void evolutionRiskShouldHaveExpectedValues() {
      final var values = WitEvolutionValidation.EvolutionRisk.values();
      assertEquals(4, values.length);

      assertNotNull(WitEvolutionValidation.EvolutionRisk.LOW);
      assertNotNull(WitEvolutionValidation.EvolutionRisk.MEDIUM);
      assertNotNull(WitEvolutionValidation.EvolutionRisk.HIGH);
      assertNotNull(WitEvolutionValidation.EvolutionRisk.CRITICAL);
    }

    @Test
    @DisplayName("CompatibilityLevel should have expected values")
    void compatibilityLevelShouldHaveExpectedValues() {
      final var values = WitEvolutionValidation.CompatibilityLevel.values();
      assertEquals(4, values.length);

      assertNotNull(WitEvolutionValidation.CompatibilityLevel.FULL);
      assertNotNull(WitEvolutionValidation.CompatibilityLevel.PARTIAL);
      assertNotNull(WitEvolutionValidation.CompatibilityLevel.LIMITED);
      assertNotNull(WitEvolutionValidation.CompatibilityLevel.NONE);
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("success should create valid result with LOW risk")
    void successShouldCreateValidResultWithLowRisk() {
      final List<String> recommendations = List.of("rec1", "rec2");
      final var metrics = createMockMetrics();

      final var validation = WitEvolutionValidation.success(recommendations, metrics);

      assertTrue(validation.isValid());
      assertFalse(validation.hasViolations());
      assertFalse(validation.hasIssues());
      assertFalse(validation.hasWarnings());
      assertEquals(WitEvolutionValidation.EvolutionRisk.LOW, validation.getRiskAssessment());
      assertEquals(2, validation.getRecommendations().size());
      assertNotNull(validation.getValidationTime());
      assertEquals(metrics, validation.getMetrics());
    }

    @Test
    @DisplayName("failure should create invalid result with HIGH risk")
    void failureShouldCreateInvalidResultWithHighRisk() {
      final var violation =
          new WitEvolutionValidation.ConstraintViolation(
              WitEvolutionValidation.ViolationType.TYPE_SAFETY_VIOLATION,
              "Type mismatch",
              "interface.types.user",
              WitEvolutionValidation.ViolationSeverity.HIGH,
              Optional.of("Use compatible type"));

      final var issue =
          new WitEvolutionValidation.CompatibilityIssue(
              WitEvolutionValidation.IssueType.TYPE_MISMATCH,
              "Type mismatch detected",
              "interface.types",
              WitEvolutionValidation.IssueSeverity.ERROR,
              List.of("Change type", "Update consumer"),
              WitEvolutionValidation.CompatibilityLevel.LIMITED);

      final var metrics = createMockMetrics();

      final var validation =
          WitEvolutionValidation.failure(List.of(violation), List.of(issue), metrics);

      assertFalse(validation.isValid());
      assertTrue(validation.hasViolations());
      assertTrue(validation.hasIssues());
      assertFalse(validation.hasWarnings());
      assertEquals(WitEvolutionValidation.EvolutionRisk.HIGH, validation.getRiskAssessment());
      assertEquals(1, validation.getViolations().size());
      assertEquals(1, validation.getIssues().size());
      assertTrue(validation.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("withWarnings should create valid result with warnings")
    void withWarningsShouldCreateValidResultWithWarnings() {
      final var warning =
          new WitEvolutionValidation.EvolutionWarning(
              WitEvolutionValidation.WarningType.PERFORMANCE_WARNING,
              "May impact performance",
              "interface.functions.process",
              Optional.of("Consider caching"));

      final List<String> recommendations = List.of("Review changes");
      final var metrics = createMockMetrics();

      final var validation =
          WitEvolutionValidation.withWarnings(
              List.of(warning),
              WitEvolutionValidation.EvolutionRisk.MEDIUM,
              recommendations,
              metrics);

      assertTrue(validation.isValid());
      assertFalse(validation.hasViolations());
      assertFalse(validation.hasIssues());
      assertTrue(validation.hasWarnings());
      assertEquals(WitEvolutionValidation.EvolutionRisk.MEDIUM, validation.getRiskAssessment());
      assertEquals(1, validation.getWarnings().size());
      assertEquals(1, validation.getRecommendations().size());
    }
  }

  @Nested
  @DisplayName("ConstraintViolation Tests")
  class ConstraintViolationTests {

    @Test
    @DisplayName("should create violation with all properties")
    void shouldCreateViolationWithAllProperties() {
      final var violation =
          new WitEvolutionValidation.ConstraintViolation(
              WitEvolutionValidation.ViolationType.BACKWARD_COMPATIBILITY_VIOLATION,
              "Breaking change detected",
              "interface.functions.greet",
              WitEvolutionValidation.ViolationSeverity.CRITICAL,
              Optional.of("Keep old function"));

      assertEquals(
          WitEvolutionValidation.ViolationType.BACKWARD_COMPATIBILITY_VIOLATION,
          violation.getType());
      assertEquals("Breaking change detected", violation.getDescription());
      assertEquals("interface.functions.greet", violation.getLocation());
      assertEquals(WitEvolutionValidation.ViolationSeverity.CRITICAL, violation.getSeverity());
      assertTrue(violation.getSuggestion().isPresent());
      assertEquals("Keep old function", violation.getSuggestion().get());
    }

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var violation =
          new WitEvolutionValidation.ConstraintViolation(
              WitEvolutionValidation.ViolationType.TYPE_SAFETY_VIOLATION,
              "Type error",
              "loc",
              WitEvolutionValidation.ViolationSeverity.HIGH,
              Optional.empty());

      final String str = violation.toString();

      assertNotNull(str);
      assertTrue(str.contains("ConstraintViolation"));
      assertTrue(str.contains("TYPE_SAFETY_VIOLATION"));
      assertTrue(str.contains("Type error"));
      assertTrue(str.contains("HIGH"));
    }
  }

  @Nested
  @DisplayName("CompatibilityIssue Tests")
  class CompatibilityIssueTests {

    @Test
    @DisplayName("should create issue with all properties")
    void shouldCreateIssueWithAllProperties() {
      final var issue =
          new WitEvolutionValidation.CompatibilityIssue(
              WitEvolutionValidation.IssueType.MISSING_FUNCTION,
              "Function not found",
              "interface.functions.process",
              WitEvolutionValidation.IssueSeverity.ERROR,
              List.of("Add function", "Use alternative"),
              WitEvolutionValidation.CompatibilityLevel.LIMITED);

      assertEquals(WitEvolutionValidation.IssueType.MISSING_FUNCTION, issue.getType());
      assertEquals("Function not found", issue.getDescription());
      assertEquals("interface.functions.process", issue.getLocation());
      assertEquals(WitEvolutionValidation.IssueSeverity.ERROR, issue.getSeverity());
      assertEquals(2, issue.getResolutions().size());
      assertEquals(WitEvolutionValidation.CompatibilityLevel.LIMITED, issue.getImpactLevel());
    }

    @Test
    @DisplayName("getResolutions should return unmodifiable list")
    void getResolutionsShouldReturnUnmodifiableList() {
      final var issue =
          new WitEvolutionValidation.CompatibilityIssue(
              WitEvolutionValidation.IssueType.TYPE_MISMATCH,
              "desc",
              "loc",
              WitEvolutionValidation.IssueSeverity.WARNING,
              List.of("resolution"),
              WitEvolutionValidation.CompatibilityLevel.PARTIAL);

      final List<String> resolutions = issue.getResolutions();

      try {
        resolutions.add("new resolution");
        assertTrue(false, "List should be unmodifiable");
      } catch (UnsupportedOperationException e) {
        assertTrue(true, "List is unmodifiable as expected");
      }
    }

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var issue =
          new WitEvolutionValidation.CompatibilityIssue(
              WitEvolutionValidation.IssueType.VERSION_CONFLICT,
              "Version mismatch",
              "interface",
              WitEvolutionValidation.IssueSeverity.CRITICAL,
              List.of(),
              WitEvolutionValidation.CompatibilityLevel.NONE);

      final String str = issue.toString();

      assertNotNull(str);
      assertTrue(str.contains("CompatibilityIssue"));
      assertTrue(str.contains("VERSION_CONFLICT"));
      assertTrue(str.contains("Version mismatch"));
      assertTrue(str.contains("NONE"));
    }
  }

  @Nested
  @DisplayName("EvolutionWarning Tests")
  class EvolutionWarningTests {

    @Test
    @DisplayName("should create warning with all properties")
    void shouldCreateWarningWithAllProperties() {
      final var warning =
          new WitEvolutionValidation.EvolutionWarning(
              WitEvolutionValidation.WarningType.SECURITY_WARNING,
              "Potential security concern",
              "interface.functions.auth",
              Optional.of("Add validation"));

      assertEquals(WitEvolutionValidation.WarningType.SECURITY_WARNING, warning.getType());
      assertEquals("Potential security concern", warning.getMessage());
      assertEquals("interface.functions.auth", warning.getLocation());
      assertTrue(warning.getRecommendation().isPresent());
      assertEquals("Add validation", warning.getRecommendation().get());
    }

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var warning =
          new WitEvolutionValidation.EvolutionWarning(
              WitEvolutionValidation.WarningType.DEPRECATION_WARNING,
              "Feature deprecated",
              "interface.types.old",
              Optional.empty());

      final String str = warning.toString();

      assertNotNull(str);
      assertTrue(str.contains("EvolutionWarning"));
      assertTrue(str.contains("DEPRECATION_WARNING"));
      assertTrue(str.contains("Feature deprecated"));
    }
  }

  @Nested
  @DisplayName("Filtering Tests")
  class FilteringTests {

    @Test
    @DisplayName("getViolationsByType should filter violations")
    void getViolationsByTypeShouldFilterViolations() {
      final var violation1 =
          new WitEvolutionValidation.ConstraintViolation(
              WitEvolutionValidation.ViolationType.TYPE_SAFETY_VIOLATION,
              "desc1",
              "loc1",
              WitEvolutionValidation.ViolationSeverity.HIGH,
              Optional.empty());

      final var violation2 =
          new WitEvolutionValidation.ConstraintViolation(
              WitEvolutionValidation.ViolationType.DEPENDENCY_VIOLATION,
              "desc2",
              "loc2",
              WitEvolutionValidation.ViolationSeverity.MEDIUM,
              Optional.empty());

      final var violation3 =
          new WitEvolutionValidation.ConstraintViolation(
              WitEvolutionValidation.ViolationType.TYPE_SAFETY_VIOLATION,
              "desc3",
              "loc3",
              WitEvolutionValidation.ViolationSeverity.LOW,
              Optional.empty());

      final var metrics = createMockMetrics();

      final var validation =
          WitEvolutionValidation.failure(
              List.of(violation1, violation2, violation3), List.of(), metrics);

      final var typeSafetyViolations =
          validation.getViolationsByType(
              WitEvolutionValidation.ViolationType.TYPE_SAFETY_VIOLATION);

      assertEquals(2, typeSafetyViolations.size());
    }

    @Test
    @DisplayName("getIssuesBySeverity should filter issues")
    void getIssuesBySeverityShouldFilterIssues() {
      final var issue1 =
          new WitEvolutionValidation.CompatibilityIssue(
              WitEvolutionValidation.IssueType.TYPE_MISMATCH,
              "desc1",
              "loc1",
              WitEvolutionValidation.IssueSeverity.ERROR,
              List.of(),
              WitEvolutionValidation.CompatibilityLevel.LIMITED);

      final var issue2 =
          new WitEvolutionValidation.CompatibilityIssue(
              WitEvolutionValidation.IssueType.MISSING_FUNCTION,
              "desc2",
              "loc2",
              WitEvolutionValidation.IssueSeverity.WARNING,
              List.of(),
              WitEvolutionValidation.CompatibilityLevel.PARTIAL);

      final var issue3 =
          new WitEvolutionValidation.CompatibilityIssue(
              WitEvolutionValidation.IssueType.VERSION_CONFLICT,
              "desc3",
              "loc3",
              WitEvolutionValidation.IssueSeverity.ERROR,
              List.of(),
              WitEvolutionValidation.CompatibilityLevel.NONE);

      final var metrics = createMockMetrics();

      final var validation =
          WitEvolutionValidation.failure(List.of(), List.of(issue1, issue2, issue3), metrics);

      final var errorIssues =
          validation.getIssuesBySeverity(WitEvolutionValidation.IssueSeverity.ERROR);

      assertEquals(2, errorIssues.size());
    }
  }

  @Nested
  @DisplayName("List Immutability Tests")
  class ListImmutabilityTests {

    @Test
    @DisplayName("getViolations should return unmodifiable list")
    void getViolationsShouldReturnUnmodifiableList() {
      final var validation = WitEvolutionValidation.success(List.of(), createMockMetrics());

      final List<WitEvolutionValidation.ConstraintViolation> violations =
          validation.getViolations();

      try {
        violations.add(
            new WitEvolutionValidation.ConstraintViolation(
                WitEvolutionValidation.ViolationType.TYPE_SAFETY_VIOLATION,
                "desc",
                "loc",
                WitEvolutionValidation.ViolationSeverity.LOW,
                Optional.empty()));
        assertTrue(false, "List should be unmodifiable");
      } catch (UnsupportedOperationException e) {
        assertTrue(true, "List is unmodifiable as expected");
      }
    }

    @Test
    @DisplayName("getIssues should return unmodifiable list")
    void getIssuesShouldReturnUnmodifiableList() {
      final var validation = WitEvolutionValidation.success(List.of(), createMockMetrics());

      final List<WitEvolutionValidation.CompatibilityIssue> issues = validation.getIssues();

      try {
        issues.add(
            new WitEvolutionValidation.CompatibilityIssue(
                WitEvolutionValidation.IssueType.TYPE_MISMATCH,
                "desc",
                "loc",
                WitEvolutionValidation.IssueSeverity.WARNING,
                List.of(),
                WitEvolutionValidation.CompatibilityLevel.PARTIAL));
        assertTrue(false, "List should be unmodifiable");
      } catch (UnsupportedOperationException e) {
        assertTrue(true, "List is unmodifiable as expected");
      }
    }

    @Test
    @DisplayName("getWarnings should return unmodifiable list")
    void getWarningsShouldReturnUnmodifiableList() {
      final var validation = WitEvolutionValidation.success(List.of(), createMockMetrics());

      final List<WitEvolutionValidation.EvolutionWarning> warnings = validation.getWarnings();

      try {
        warnings.add(
            new WitEvolutionValidation.EvolutionWarning(
                WitEvolutionValidation.WarningType.PERFORMANCE_WARNING,
                "msg",
                "loc",
                Optional.empty()));
        assertTrue(false, "List should be unmodifiable");
      } catch (UnsupportedOperationException e) {
        assertTrue(true, "List is unmodifiable as expected");
      }
    }

    @Test
    @DisplayName("getRecommendations should return unmodifiable list")
    void getRecommendationsShouldReturnUnmodifiableList() {
      final var validation = WitEvolutionValidation.success(List.of("rec1"), createMockMetrics());

      final List<String> recommendations = validation.getRecommendations();

      try {
        recommendations.add("new rec");
        assertTrue(false, "List should be unmodifiable");
      } catch (UnsupportedOperationException e) {
        assertTrue(true, "List is unmodifiable as expected");
      }
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var validation = WitEvolutionValidation.success(List.of("rec"), createMockMetrics());

      final String str = validation.toString();

      assertNotNull(str);
      assertTrue(str.contains("WitEvolutionValidation"));
      assertTrue(str.contains("valid=true"));
      assertTrue(str.contains("violations=0"));
      assertTrue(str.contains("issues=0"));
      assertTrue(str.contains("warnings=0"));
      assertTrue(str.contains("LOW"));
    }
  }

  /**
   * Creates a mock ValidationMetrics for testing.
   *
   * @return mock validation metrics
   */
  private WitEvolutionValidation.ValidationMetrics createMockMetrics() {
    return new WitEvolutionValidation.ValidationMetrics() {
      @Override
      public Duration getValidationDuration() {
        return Duration.ofMillis(100);
      }

      @Override
      public int getConstraintsChecked() {
        return 10;
      }

      @Override
      public int getTypesAnalyzed() {
        return 5;
      }

      @Override
      public int getFunctionsAnalyzed() {
        return 8;
      }

      @Override
      public double getValidationScore() {
        return 0.95;
      }

      @Override
      public Map<String, Object> getDetailedMetrics() {
        return Map.of();
      }
    };
  }
}
