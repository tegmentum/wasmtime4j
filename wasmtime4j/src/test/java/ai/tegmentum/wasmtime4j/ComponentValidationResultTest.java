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

import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult.ErrorSeverity;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult.ValidationContext;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult.ValidationError;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult.ValidationWarning;
import ai.tegmentum.wasmtime4j.component.ComponentVersion;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentValidationResult} class.
 *
 * <p>ComponentValidationResult provides detailed information about component validation.
 */
@DisplayName("ComponentValidationResult Tests")
class ComponentValidationResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentValidationResult.class.getModifiers()),
          "ComponentValidationResult should be public");
      assertTrue(
          Modifier.isFinal(ComponentValidationResult.class.getModifiers()),
          "ComponentValidationResult should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create result with all parameters")
    void shouldCreateResultWithAllParameters() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var errors =
          List.of(new ValidationError("E001", "Test error", "module.function", ErrorSeverity.HIGH));
      final var warnings = List.of(new ValidationWarning("W001", "Test warning", "module.field"));
      final var recommendations = List.of("Consider upgrading");

      final var result =
          new ComponentValidationResult(false, errors, warnings, recommendations, context);

      assertNotNull(result, "Result should be created");
      assertFalse(result.isValid(), "Should not be valid with errors");
      assertEquals(1, result.getErrors().size(), "Should have 1 error");
      assertEquals(1, result.getWarnings().size(), "Should have 1 warning");
      assertEquals(1, result.getRecommendations().size(), "Should have 1 recommendation");
      assertNotNull(result.getContext(), "Should have context");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("success should create valid result")
    void successShouldCreateValidResult() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));

      final var result = ComponentValidationResult.success(context);

      assertTrue(result.isValid(), "Success result should be valid");
      assertTrue(result.getErrors().isEmpty(), "Success result should have no errors");
      assertTrue(result.getWarnings().isEmpty(), "Success result should have no warnings");
      assertTrue(
          result.getRecommendations().isEmpty(), "Success result should have no recommendations");
    }

    @Test
    @DisplayName("failure should create invalid result")
    void failureShouldCreateInvalidResult() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var errors =
          List.of(new ValidationError("E001", "Test error", "test.location", ErrorSeverity.HIGH));

      final var result = ComponentValidationResult.failure(errors, context);

      assertFalse(result.isValid(), "Failure result should not be valid");
      assertEquals(1, result.getErrors().size(), "Failure result should have errors");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("hasErrors should return true when errors exist")
    void hasErrorsShouldReturnTrueWhenErrorsExist() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var errors =
          List.of(
              new ValidationError("E001", "Test error", "test.location", ErrorSeverity.CRITICAL));

      final var result =
          new ComponentValidationResult(false, errors, List.of(), List.of(), context);

      assertTrue(result.hasErrors(), "Should have errors");
    }

    @Test
    @DisplayName("hasErrors should return false when no errors")
    void hasErrorsShouldReturnFalseWhenNoErrors() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));

      final var result = ComponentValidationResult.success(context);

      assertFalse(result.hasErrors(), "Should not have errors");
    }

    @Test
    @DisplayName("hasWarnings should return true when warnings exist")
    void hasWarningsShouldReturnTrueWhenWarningsExist() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var warnings = List.of(new ValidationWarning("W001", "Test warning", "test.location"));

      final var result =
          new ComponentValidationResult(true, List.of(), warnings, List.of(), context);

      assertTrue(result.hasWarnings(), "Should have warnings");
    }

    @Test
    @DisplayName("hasWarnings should return false when no warnings")
    void hasWarningsShouldReturnFalseWhenNoWarnings() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));

      final var result = ComponentValidationResult.success(context);

      assertFalse(result.hasWarnings(), "Should not have warnings");
    }

    @Test
    @DisplayName("getIssueCount should return sum of errors and warnings")
    void getIssueCountShouldReturnSumOfErrorsAndWarnings() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var errors =
          List.of(
              new ValidationError("E001", "Error 1", "loc1", ErrorSeverity.HIGH),
              new ValidationError("E002", "Error 2", "loc2", ErrorSeverity.MEDIUM));
      final var warnings =
          List.of(
              new ValidationWarning("W001", "Warning 1", "loc3"),
              new ValidationWarning("W002", "Warning 2", "loc4"),
              new ValidationWarning("W003", "Warning 3", "loc5"));

      final var result = new ComponentValidationResult(false, errors, warnings, List.of(), context);

      assertEquals(5, result.getIssueCount(), "Issue count should be 5 (2 errors + 3 warnings)");
    }
  }

  @Nested
  @DisplayName("Summary Tests")
  class SummaryTests {

    @Test
    @DisplayName("getSummary should indicate passed for valid result")
    void getSummaryShouldIndicatePassedForValidResult() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var result = ComponentValidationResult.success(context);

      final var summary = result.getSummary();

      assertTrue(summary.contains("PASSED"), "Summary should indicate PASSED");
    }

    @Test
    @DisplayName("getSummary should indicate failed for invalid result")
    void getSummaryShouldIndicateFailedForInvalidResult() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var errors =
          List.of(new ValidationError("E001", "Test error", "test.loc", ErrorSeverity.HIGH));
      final var result = ComponentValidationResult.failure(errors, context);

      final var summary = result.getSummary();

      assertTrue(summary.contains("FAILED"), "Summary should indicate FAILED");
    }

    @Test
    @DisplayName("getSummary should include error count")
    void getSummaryShouldIncludeErrorCount() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var errors =
          List.of(
              new ValidationError("E001", "Error 1", "loc1", ErrorSeverity.HIGH),
              new ValidationError("E002", "Error 2", "loc2", ErrorSeverity.HIGH));

      final var result =
          new ComponentValidationResult(false, errors, List.of(), List.of(), context);
      final var summary = result.getSummary();

      assertTrue(summary.contains("Errors: 2"), "Summary should include error count");
    }

    @Test
    @DisplayName("getSummary should include warning count")
    void getSummaryShouldIncludeWarningCount() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var warnings =
          List.of(
              new ValidationWarning("W001", "Warning 1", "loc1"),
              new ValidationWarning("W002", "Warning 2", "loc2"),
              new ValidationWarning("W003", "Warning 3", "loc3"));

      final var result =
          new ComponentValidationResult(true, List.of(), warnings, List.of(), context);
      final var summary = result.getSummary();

      assertTrue(summary.contains("Warnings: 3"), "Summary should include warning count");
    }

    @Test
    @DisplayName("toString should return summary")
    void toStringShouldReturnSummary() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var result = ComponentValidationResult.success(context);

      assertEquals(result.getSummary(), result.toString(), "toString should return summary");
    }
  }

  @Nested
  @DisplayName("ValidationError Tests")
  class ValidationErrorTests {

    @Test
    @DisplayName("should create error with all fields")
    void shouldCreateErrorWithAllFields() {
      final var error =
          new ValidationError("E001", "Test error message", "module.function", ErrorSeverity.HIGH);

      assertEquals("E001", error.getCode(), "Code should match");
      assertEquals("Test error message", error.getMessage(), "Message should match");
      assertEquals("module.function", error.getLocation(), "Location should match");
      assertEquals(ErrorSeverity.HIGH, error.getSeverity(), "Severity should match");
    }

    @Test
    @DisplayName("toString should format error properly")
    void toStringShouldFormatErrorProperly() {
      final var error =
          new ValidationError("E001", "Test error message", "module.function", ErrorSeverity.HIGH);

      final var str = error.toString();

      assertTrue(str.contains("HIGH"), "Should contain severity");
      assertTrue(str.contains("E001"), "Should contain code");
      assertTrue(str.contains("module.function"), "Should contain location");
      assertTrue(str.contains("Test error message"), "Should contain message");
    }
  }

  @Nested
  @DisplayName("ValidationWarning Tests")
  class ValidationWarningTests {

    @Test
    @DisplayName("should create warning with all fields")
    void shouldCreateWarningWithAllFields() {
      final var warning = new ValidationWarning("W001", "Test warning message", "module.field");

      assertEquals("W001", warning.getCode(), "Code should match");
      assertEquals("Test warning message", warning.getMessage(), "Message should match");
      assertEquals("module.field", warning.getLocation(), "Location should match");
    }

    @Test
    @DisplayName("toString should format warning properly")
    void toStringShouldFormatWarningProperly() {
      final var warning = new ValidationWarning("W001", "Test warning message", "module.field");

      final var str = warning.toString();

      assertTrue(str.contains("WARNING"), "Should contain WARNING label");
      assertTrue(str.contains("W001"), "Should contain code");
      assertTrue(str.contains("module.field"), "Should contain location");
      assertTrue(str.contains("Test warning message"), "Should contain message");
    }
  }

  @Nested
  @DisplayName("ValidationContext Tests")
  class ValidationContextTests {

    @Test
    @DisplayName("should create context with component ID and version")
    void shouldCreateContextWithComponentIdAndVersion() {
      final var version = new ComponentVersion(1, 2, 3);
      final var context = new ValidationContext("my-component", version);

      assertEquals("my-component", context.getComponentId(), "Component ID should match");
      assertEquals(version, context.getVersion(), "Version should match");
      assertTrue(context.getTimestamp() > 0, "Timestamp should be set");
    }

    @Test
    @DisplayName("should set timestamp on creation")
    void shouldSetTimestampOnCreation() {
      final long before = System.currentTimeMillis();
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final long after = System.currentTimeMillis();

      assertTrue(
          context.getTimestamp() >= before && context.getTimestamp() <= after,
          "Timestamp should be within creation window");
    }
  }

  @Nested
  @DisplayName("ErrorSeverity Enum Tests")
  class ErrorSeverityEnumTests {

    @Test
    @DisplayName("should have all severity levels")
    void shouldHaveAllSeverityLevels() {
      final var severities = ErrorSeverity.values();
      assertEquals(4, severities.length, "Should have 4 severity levels");
    }

    @Test
    @DisplayName("should have LOW severity")
    void shouldHaveLowSeverity() {
      assertEquals(ErrorSeverity.LOW, ErrorSeverity.valueOf("LOW"));
    }

    @Test
    @DisplayName("should have MEDIUM severity")
    void shouldHaveMediumSeverity() {
      assertEquals(ErrorSeverity.MEDIUM, ErrorSeverity.valueOf("MEDIUM"));
    }

    @Test
    @DisplayName("should have HIGH severity")
    void shouldHaveHighSeverity() {
      assertEquals(ErrorSeverity.HIGH, ErrorSeverity.valueOf("HIGH"));
    }

    @Test
    @DisplayName("should have CRITICAL severity")
    void shouldHaveCriticalSeverity() {
      assertEquals(ErrorSeverity.CRITICAL, ErrorSeverity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("getErrors should return immutable list")
    void getErrorsShouldReturnImmutableList() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var errors =
          List.of(new ValidationError("E001", "Test error", "loc", ErrorSeverity.HIGH));

      final var result =
          new ComponentValidationResult(false, errors, List.of(), List.of(), context);

      try {
        result.getErrors().add(new ValidationError("E002", "New error", "loc2", ErrorSeverity.LOW));
        // Should not reach here
        assertFalse(true, "Should have thrown UnsupportedOperationException");
      } catch (final UnsupportedOperationException e) {
        // Expected behavior
        assertTrue(true, "Errors list should be immutable");
      }
    }

    @Test
    @DisplayName("getWarnings should return immutable list")
    void getWarningsShouldReturnImmutableList() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var warnings = List.of(new ValidationWarning("W001", "Test warning", "loc"));

      final var result =
          new ComponentValidationResult(true, List.of(), warnings, List.of(), context);

      try {
        result.getWarnings().add(new ValidationWarning("W002", "New warning", "loc2"));
        // Should not reach here
        assertFalse(true, "Should have thrown UnsupportedOperationException");
      } catch (final UnsupportedOperationException e) {
        // Expected behavior
        assertTrue(true, "Warnings list should be immutable");
      }
    }

    @Test
    @DisplayName("getRecommendations should return immutable list")
    void getRecommendationsShouldReturnImmutableList() {
      final var context = new ValidationContext("test-component", new ComponentVersion(1, 0, 0));
      final var recommendations = List.of("Upgrade to v2");

      final var result =
          new ComponentValidationResult(true, List.of(), List.of(), recommendations, context);

      try {
        result.getRecommendations().add("New recommendation");
        // Should not reach here
        assertFalse(true, "Should have thrown UnsupportedOperationException");
      } catch (final UnsupportedOperationException e) {
        // Expected behavior
        assertTrue(true, "Recommendations list should be immutable");
      }
    }
  }
}
