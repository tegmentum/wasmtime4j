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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.metadata.CustomSectionValidationResult;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionValidationResult.ValidationIssue;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionValidationResult.ValidationIssueType;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CustomSectionValidationResult} class.
 *
 * <p>CustomSectionValidationResult contains results of validating WebAssembly custom sections.
 * Validity is derived from errors being empty. Uses Builder pattern and static factories.
 */
@DisplayName("CustomSectionValidationResult Tests")
class CustomSectionValidationResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(CustomSectionValidationResult.class.getModifiers()),
          "CustomSectionValidationResult should be public");
      assertTrue(
          Modifier.isFinal(CustomSectionValidationResult.class.getModifiers()),
          "CustomSectionValidationResult should be final");
    }
  }

  @Nested
  @DisplayName("Success Factory Tests")
  class SuccessFactoryTests {

    @Test
    @DisplayName("success should create valid result with no issues")
    void successShouldCreateValidResultWithNoIssues() {
      final CustomSectionValidationResult result = CustomSectionValidationResult.success();

      assertNotNull(result, "Success result should not be null");
      assertTrue(result.isValid(), "Success result should be valid");
      assertTrue(result.getErrors().isEmpty(), "Should have no errors");
      assertTrue(result.getWarnings().isEmpty(), "Should have no warnings");
      assertFalse(result.hasIssues(), "Should have no issues");
      assertEquals(0, result.getIssueCount(), "Issue count should be 0");
    }

    @Test
    @DisplayName("success should return default summary for valid result")
    void successShouldReturnDefaultSummary() {
      final CustomSectionValidationResult result = CustomSectionValidationResult.success();

      assertEquals(
          "All custom sections are valid",
          result.getSummary(),
          "Default summary for success should indicate all valid");
    }
  }

  @Nested
  @DisplayName("SuccessWithWarnings Factory Tests")
  class SuccessWithWarningsFactoryTests {

    @Test
    @DisplayName("successWithWarnings should create valid result with warnings")
    void successWithWarningsShouldCreateValidResultWithWarnings() {
      final List<ValidationIssue> warnings =
          List.of(ValidationIssue.warning("name", "Section name is very long"));

      final CustomSectionValidationResult result =
          CustomSectionValidationResult.successWithWarnings(warnings);

      assertTrue(result.isValid(), "Result should still be valid with only warnings");
      assertTrue(result.getErrors().isEmpty(), "Should have no errors");
      assertEquals(1, result.getWarnings().size(), "Should have 1 warning");
      assertTrue(result.hasIssues(), "Should report having issues (warnings)");
    }

    @Test
    @DisplayName("successWithWarnings should throw IllegalArgumentException for null")
    void successWithWarningsShouldThrowIaeForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionValidationResult.successWithWarnings(null),
          "Null warnings should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Failure Factory Tests")
  class FailureFactoryTests {

    @Test
    @DisplayName("failure with errors only should create invalid result")
    void failureWithErrorsOnlyShouldCreateInvalidResult() {
      final List<ValidationIssue> errors =
          List.of(ValidationIssue.error("debug", "Invalid debug section format"));

      final CustomSectionValidationResult result = CustomSectionValidationResult.failure(errors);

      assertFalse(result.isValid(), "Failure result should not be valid");
      assertEquals(1, result.getErrors().size(), "Should have 1 error");
      assertTrue(result.getWarnings().isEmpty(), "Should have no warnings");
    }

    @Test
    @DisplayName("failure with errors and warnings should create invalid result")
    void failureWithErrorsAndWarningsShouldCreateInvalidResult() {
      final List<ValidationIssue> errors =
          List.of(ValidationIssue.error("producers", "Malformed producers section"));
      final List<ValidationIssue> warnings =
          List.of(ValidationIssue.warning("name", "Empty name section"));

      final CustomSectionValidationResult result =
          CustomSectionValidationResult.failure(errors, warnings);

      assertFalse(result.isValid(), "Result should not be valid");
      assertEquals(1, result.getErrors().size(), "Should have 1 error");
      assertEquals(1, result.getWarnings().size(), "Should have 1 warning");
      assertEquals(2, result.getIssueCount(), "Issue count should be 2");
    }

    @Test
    @DisplayName("failure should throw IllegalArgumentException for null errors")
    void failureShouldThrowIaeForNullErrors() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionValidationResult.failure(null),
          "Null errors should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("failure should throw IllegalArgumentException for empty errors")
    void failureShouldThrowIaeForEmptyErrors() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionValidationResult.failure(List.of()),
          "Empty errors should throw IllegalArgumentException for failure");
    }

    @Test
    @DisplayName("failure with two lists should throw IAE for null errors")
    void failureWithTwoListsShouldThrowIaeForNullErrors() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionValidationResult.failure(null, List.of()),
          "Null errors should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("failure with two lists should throw IAE for empty errors")
    void failureWithTwoListsShouldThrowIaeForEmptyErrors() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionValidationResult.failure(List.of(), List.of()),
          "Empty errors should throw IllegalArgumentException for failure");
    }

    @Test
    @DisplayName("failure with two lists should throw IAE for null warnings")
    void failureWithTwoListsShouldThrowIaeForNullWarnings() {
      final List<ValidationIssue> errors = List.of(ValidationIssue.error("test", "test error"));

      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionValidationResult.failure(errors, null),
          "Null warnings should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create valid result when no errors added")
    void builderShouldCreateValidResultWhenNoErrors() {
      final CustomSectionValidationResult result = CustomSectionValidationResult.builder().build();

      assertTrue(result.isValid(), "Builder with no errors should produce valid result");
    }

    @Test
    @DisplayName("builder should create invalid result when errors added")
    void builderShouldCreateInvalidResultWhenErrorsAdded() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder()
              .addError(ValidationIssue.error("test", "Test error"))
              .build();

      assertFalse(result.isValid(), "Builder with errors should produce invalid result");
    }

    @Test
    @DisplayName("builder addError with section and message should work")
    void builderAddErrorWithSectionAndMessageShouldWork() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addError("debug", "Invalid format").build();

      assertFalse(result.isValid(), "Result should not be valid");
      assertEquals(1, result.getErrors().size(), "Should have 1 error");
      assertEquals(
          "debug",
          result.getErrors().get(0).getSectionName(),
          "Error section name should be 'debug'");
      assertEquals(
          "Invalid format", result.getErrors().get(0).getMessage(), "Error message should match");
    }

    @Test
    @DisplayName("builder addWarning with section and message should work")
    void builderAddWarningWithSectionAndMessageShouldWork() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addWarning("name", "Section is large").build();

      assertTrue(result.isValid(), "Result should still be valid with only warnings");
      assertEquals(1, result.getWarnings().size(), "Should have 1 warning");
      assertEquals(
          "name",
          result.getWarnings().get(0).getSectionName(),
          "Warning section name should be 'name'");
    }

    @Test
    @DisplayName("builder addError should throw IAE for null error")
    void builderAddErrorShouldThrowIaeForNullError() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionValidationResult.builder().addError((ValidationIssue) null),
          "Adding null error should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("builder addWarning should throw IAE for null warning")
    void builderAddWarningShouldThrowIaeForNullWarning() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionValidationResult.builder().addWarning((ValidationIssue) null),
          "Adding null warning should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("builder setSummary should set custom summary")
    void builderSetSummaryShouldSetCustomSummary() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().setSummary("Custom validation summary").build();

      assertEquals(
          "Custom validation summary",
          result.getSummary(),
          "Summary should match the custom value");
    }

    @Test
    @DisplayName("builder setErrors should replace errors list")
    void builderSetErrorsShouldReplaceErrorsList() {
      final List<ValidationIssue> errors =
          List.of(
              ValidationIssue.error("sec1", "Error 1"), ValidationIssue.error("sec2", "Error 2"));

      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().setErrors(errors).build();

      assertEquals(2, result.getErrors().size(), "Should have 2 errors");
    }

    @Test
    @DisplayName("builder setWarnings should replace warnings list")
    void builderSetWarningsShouldReplaceWarningsList() {
      final List<ValidationIssue> warnings =
          List.of(
              ValidationIssue.warning("sec1", "Warn 1"),
              ValidationIssue.warning("sec2", "Warn 2"),
              ValidationIssue.warning("sec3", "Warn 3"));

      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().setWarnings(warnings).build();

      assertEquals(3, result.getWarnings().size(), "Should have 3 warnings");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("hasIssues should return false when no errors or warnings")
    void hasIssuesShouldReturnFalseWhenNoIssues() {
      final CustomSectionValidationResult result = CustomSectionValidationResult.success();

      assertFalse(result.hasIssues(), "Should not have issues when empty");
    }

    @Test
    @DisplayName("hasIssues should return true when only warnings present")
    void hasIssuesShouldReturnTrueWhenOnlyWarnings() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addWarning("test", "Warning msg").build();

      assertTrue(result.hasIssues(), "Should have issues when warnings present");
    }

    @Test
    @DisplayName("getIssueCount should return sum of errors and warnings")
    void getIssueCountShouldReturnSum() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder()
              .addError("sec1", "Error 1")
              .addError("sec2", "Error 2")
              .addWarning("sec3", "Warn 1")
              .build();

      assertEquals(3, result.getIssueCount(), "Issue count should be 3 (2 errors + 1 warning)");
    }

    @Test
    @DisplayName("getAllIssues should combine errors and warnings")
    void getAllIssuesShouldCombineErrorsAndWarnings() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder()
              .addError("sec1", "Error 1")
              .addWarning("sec2", "Warn 1")
              .build();

      final List<ValidationIssue> allIssues = result.getAllIssues();

      assertEquals(2, allIssues.size(), "Should have 2 total issues");
    }

    @Test
    @DisplayName("getAllIssues should return immutable list")
    void getAllIssuesShouldReturnImmutableList() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addError("sec1", "Error 1").build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> result.getAllIssues().add(ValidationIssue.error("new", "new error")),
          "getAllIssues should return immutable list");
    }

    @Test
    @DisplayName("getIssuesForSection should filter by section name")
    void getIssuesForSectionShouldFilterBySectionName() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder()
              .addError("debug", "Debug error 1")
              .addError("debug", "Debug error 2")
              .addError("name", "Name error")
              .addWarning("debug", "Debug warning")
              .build();

      final List<ValidationIssue> debugIssues = result.getIssuesForSection("debug");

      assertEquals(
          3, debugIssues.size(), "Should have 3 issues for 'debug' section (2 errors + 1 warning)");
    }

    @Test
    @DisplayName("getIssuesForSection should return empty list for unknown section")
    void getIssuesForSectionShouldReturnEmptyForUnknownSection() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addError("debug", "Error").build();

      final List<ValidationIssue> issues = result.getIssuesForSection("nonexistent");

      assertTrue(issues.isEmpty(), "Should return empty list for unknown section");
    }

    @Test
    @DisplayName("getIssuesForSection should throw IAE for null section name")
    void getIssuesForSectionShouldThrowIaeForNullSectionName() {
      final CustomSectionValidationResult result = CustomSectionValidationResult.success();

      assertThrows(
          IllegalArgumentException.class,
          () -> result.getIssuesForSection(null),
          "Null section name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Summary Tests")
  class SummaryTests {

    @Test
    @DisplayName("getSummary should generate default for valid with warnings")
    void getSummaryShouldGenerateDefaultForValidWithWarnings() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addWarning("test", "Some warning").build();

      final String summary = result.getSummary();

      assertTrue(summary.contains("valid"), "Summary should mention 'valid'");
      assertTrue(summary.contains("1"), "Summary should contain warning count");
    }

    @Test
    @DisplayName("getSummary should generate default for invalid result")
    void getSummaryShouldGenerateDefaultForInvalidResult() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addError("test", "Some error").build();

      final String summary = result.getSummary();

      assertTrue(summary.contains("failed"), "Summary should mention 'failed'");
    }

    @Test
    @DisplayName("getSummary should return custom summary when set")
    void getSummaryShouldReturnCustomSummaryWhenSet() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().setSummary("My custom summary").build();

      assertEquals(
          "My custom summary", result.getSummary(), "Should return custom summary when set");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("getErrors should return immutable list")
    void getErrorsShouldReturnImmutableList() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addError("sec", "Error").build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> result.getErrors().add(ValidationIssue.error("new", "new")),
          "Errors list should be immutable");
    }

    @Test
    @DisplayName("getWarnings should return immutable list")
    void getWarningsShouldReturnImmutableList() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder().addWarning("sec", "Warning").build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> result.getWarnings().add(ValidationIssue.warning("new", "new")),
          "Warnings list should be immutable");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain CustomSectionValidationResult prefix")
    void toStringShouldContainPrefix() {
      final CustomSectionValidationResult result = CustomSectionValidationResult.success();

      assertTrue(
          result.toString().startsWith("CustomSectionValidationResult{"),
          "toString should start with 'CustomSectionValidationResult{'");
    }

    @Test
    @DisplayName("toString should contain valid status")
    void toStringShouldContainValidStatus() {
      final CustomSectionValidationResult result = CustomSectionValidationResult.success();

      assertTrue(
          result.toString().contains("valid=true"),
          "toString should contain valid=true for success");
    }

    @Test
    @DisplayName("toString should contain error and warning counts")
    void toStringShouldContainCounts() {
      final CustomSectionValidationResult result =
          CustomSectionValidationResult.builder()
              .addError("sec", "Error")
              .addWarning("sec", "Warn")
              .build();

      final String str = result.toString();

      assertTrue(str.contains("errors=1"), "toString should contain error count");
      assertTrue(str.contains("warnings=1"), "toString should contain warning count");
    }
  }

  @Nested
  @DisplayName("ValidationIssue Tests")
  class ValidationIssueTests {

    @Test
    @DisplayName("should create error issue with all fields")
    void shouldCreateErrorIssueWithAllFields() {
      final ValidationIssue issue =
          new ValidationIssue(
              ValidationIssueType.ERROR, "debug", "Invalid format", "Expected version 1");

      assertEquals(ValidationIssueType.ERROR, issue.getType(), "Type should be ERROR");
      assertEquals("debug", issue.getSectionName(), "Section name should be 'debug'");
      assertEquals("Invalid format", issue.getMessage(), "Message should match");
      assertEquals("Expected version 1", issue.getDetails(), "Details should match");
      assertTrue(issue.isError(), "isError should return true for ERROR type");
      assertFalse(issue.isWarning(), "isWarning should return false for ERROR type");
    }

    @Test
    @DisplayName("should create warning issue with all fields")
    void shouldCreateWarningIssueWithAllFields() {
      final ValidationIssue issue =
          new ValidationIssue(ValidationIssueType.WARNING, "name", "Large section", null);

      assertEquals(ValidationIssueType.WARNING, issue.getType(), "Type should be WARNING");
      assertEquals("name", issue.getSectionName(), "Section name should be 'name'");
      assertEquals("Large section", issue.getMessage(), "Message should match");
      assertNull(issue.getDetails(), "Details should be null");
      assertFalse(issue.isError(), "isError should return false for WARNING type");
      assertTrue(issue.isWarning(), "isWarning should return true for WARNING type");
    }

    @Test
    @DisplayName("should create issue without details using 3-param constructor")
    void shouldCreateIssueWithoutDetails() {
      final ValidationIssue issue =
          new ValidationIssue(ValidationIssueType.ERROR, "producers", "Missing field");

      assertEquals(ValidationIssueType.ERROR, issue.getType(), "Type should be ERROR");
      assertEquals("producers", issue.getSectionName(), "Section name should match");
      assertEquals("Missing field", issue.getMessage(), "Message should match");
      assertNull(issue.getDetails(), "Details should be null from 3-param constructor");
    }

    @Test
    @DisplayName("error factory should create ERROR type issue")
    void errorFactoryShouldCreateErrorTypeIssue() {
      final ValidationIssue issue = ValidationIssue.error("test", "test error");

      assertEquals(ValidationIssueType.ERROR, issue.getType(), "Factory should create ERROR type");
      assertEquals("test", issue.getSectionName(), "Section name should match");
      assertEquals("test error", issue.getMessage(), "Message should match");
    }

    @Test
    @DisplayName("warning factory should create WARNING type issue")
    void warningFactoryShouldCreateWarningTypeIssue() {
      final ValidationIssue issue = ValidationIssue.warning("test", "test warning");

      assertEquals(
          ValidationIssueType.WARNING, issue.getType(), "Factory should create WARNING type");
      assertEquals("test", issue.getSectionName(), "Section name should match");
      assertEquals("test warning", issue.getMessage(), "Message should match");
    }

    @Test
    @DisplayName("constructor should throw IAE for null type")
    void constructorShouldThrowIaeForNullType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ValidationIssue(null, "section", "message"),
          "Null type should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("constructor should throw IAE for null sectionName")
    void constructorShouldThrowIaeForNullSectionName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ValidationIssue(ValidationIssueType.ERROR, null, "message"),
          "Null sectionName should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("constructor should throw IAE for null message")
    void constructorShouldThrowIaeForNullMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ValidationIssue(ValidationIssueType.ERROR, "section", null),
          "Null message should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("toString should contain type section and message")
    void toStringShouldContainTypeAndSectionAndMessage() {
      final ValidationIssue issue =
          new ValidationIssue(ValidationIssueType.ERROR, "debug", "Bad format", "Details here");

      final String str = issue.toString();

      assertTrue(str.contains("ERROR"), "toString should contain type");
      assertTrue(str.contains("debug"), "toString should contain section name");
      assertTrue(str.contains("Bad format"), "toString should contain message");
      assertTrue(str.contains("Details here"), "toString should contain details");
    }

    @Test
    @DisplayName("toString should not contain details parentheses when details is null")
    void toStringShouldNotContainDetailsWhenNull() {
      final ValidationIssue issue =
          new ValidationIssue(ValidationIssueType.WARNING, "name", "Warning msg");

      final String str = issue.toString();

      assertFalse(str.contains("("), "toString should not contain parentheses when no details");
    }

    @Test
    @DisplayName("equals should return true for identical issues")
    void equalsShouldReturnTrueForIdenticalIssues() {
      final ValidationIssue issue1 =
          new ValidationIssue(ValidationIssueType.ERROR, "debug", "Bad format", "details");
      final ValidationIssue issue2 =
          new ValidationIssue(ValidationIssueType.ERROR, "debug", "Bad format", "details");

      assertEquals(issue1, issue2, "Identical issues should be equal");
    }

    @Test
    @DisplayName("hashCode should be equal for equal issues")
    void hashCodeShouldBeEqualForEqualIssues() {
      final ValidationIssue issue1 =
          new ValidationIssue(ValidationIssueType.ERROR, "debug", "Bad format", null);
      final ValidationIssue issue2 =
          new ValidationIssue(ValidationIssueType.ERROR, "debug", "Bad format", null);

      assertEquals(
          issue1.hashCode(), issue2.hashCode(), "Equal issues should have equal hash codes");
    }
  }

  @Nested
  @DisplayName("ValidationIssueType Enum Tests")
  class ValidationIssueTypeEnumTests {

    @Test
    @DisplayName("should have ERROR and WARNING values")
    void shouldHaveErrorAndWarningValues() {
      final ValidationIssueType[] values = ValidationIssueType.values();

      assertEquals(2, values.length, "Should have exactly 2 enum values");
    }

    @Test
    @DisplayName("should have ERROR value")
    void shouldHaveErrorValue() {
      assertEquals(
          ValidationIssueType.ERROR,
          ValidationIssueType.valueOf("ERROR"),
          "Should have ERROR value");
    }

    @Test
    @DisplayName("should have WARNING value")
    void shouldHaveWarningValue() {
      assertEquals(
          ValidationIssueType.WARNING,
          ValidationIssueType.valueOf("WARNING"),
          "Should have WARNING value");
    }
  }
}
