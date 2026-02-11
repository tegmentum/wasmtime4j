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

import ai.tegmentum.wasmtime4j.type.ImportType;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ImportValidation} class.
 *
 * <p>ImportValidation provides detailed information about import compatibility, missing imports,
 * type mismatches, and other validation issues.
 */
@DisplayName("ImportValidation Tests")
class ImportValidationTest {

  private ImportIssue createIssue(final ImportIssue.Severity severity) {
    return new ImportIssue(
        severity, ImportIssue.Type.MISSING_IMPORT, "mod", "fn", "Issue message");
  }

  private ImportInfo createImportInfo(final String moduleName, final String importName) {
    return new ImportInfo(
        moduleName,
        importName,
        ImportInfo.ImportType.FUNCTION,
        Optional.empty(),
        Instant.EPOCH,
        false,
        Optional.empty());
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create valid instance with all parameters")
    void shouldCreateValidInstanceWithAllParameters() {
      final List<ImportIssue> issues = List.of(createIssue(ImportIssue.Severity.WARNING));
      final List<ImportInfo> imports = List.of(createImportInfo("env", "log"));
      final Duration duration = Duration.ofMillis(150);

      final ImportValidation validation =
          new ImportValidation(true, issues, imports, 5, 4, duration);

      assertTrue(validation.isValid(), "isValid should be true");
      assertEquals(1, validation.getIssues().size(), "should have 1 issue");
      assertEquals(1, validation.getValidatedImports().size(), "should have 1 validated import");
      assertEquals(5, validation.getTotalImports(), "totalImports should be 5");
      assertEquals(4, validation.getValidImports(), "validImports should be 4");
      assertEquals(duration, validation.getValidationTime(), "validationTime should match");
    }

    @Test
    @DisplayName("should create instance with empty lists")
    void shouldCreateInstanceWithEmptyLists() {
      final ImportValidation validation =
          new ImportValidation(
              true,
              Collections.emptyList(),
              Collections.emptyList(),
              0,
              0,
              Duration.ZERO);

      assertTrue(validation.isValid(), "isValid should be true");
      assertTrue(validation.getIssues().isEmpty(), "issues should be empty");
      assertTrue(validation.getValidatedImports().isEmpty(), "validatedImports should be empty");
    }

    @Test
    @DisplayName("should throw NullPointerException for null issues list")
    void shouldThrowForNullIssuesList() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportValidation(
                  true, null, Collections.emptyList(), 0, 0, Duration.ZERO),
          "null issues should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null validatedImports list")
    void shouldThrowForNullValidatedImportsList() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportValidation(
                  true, Collections.emptyList(), null, 0, 0, Duration.ZERO),
          "null validatedImports should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null validationTime")
    void shouldThrowForNullValidationTime() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportValidation(
                  true, Collections.emptyList(), Collections.emptyList(), 0, 0, null),
          "null validationTime should throw NullPointerException");
    }

    @Test
    @DisplayName("issues list should be unmodifiable")
    void issuesListShouldBeUnmodifiable() {
      final List<ImportIssue> issues = new ArrayList<>();
      issues.add(createIssue(ImportIssue.Severity.ERROR));
      final ImportValidation validation =
          new ImportValidation(
              false, issues, Collections.emptyList(), 1, 0, Duration.ofMillis(10));

      assertThrows(
          UnsupportedOperationException.class,
          () -> validation.getIssues().add(createIssue(ImportIssue.Severity.INFO)),
          "Issues list should be unmodifiable");
    }

    @Test
    @DisplayName("validatedImports list should be unmodifiable")
    void validatedImportsListShouldBeUnmodifiable() {
      final List<ImportInfo> imports = new ArrayList<>();
      imports.add(createImportInfo("env", "log"));
      final ImportValidation validation =
          new ImportValidation(
              true, Collections.emptyList(), imports, 1, 1, Duration.ofMillis(5));

      assertThrows(
          UnsupportedOperationException.class,
          () -> validation.getValidatedImports().add(createImportInfo("env", "mem")),
          "ValidatedImports list should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("Validation Rate Tests")
  class ValidationRateTests {

    @Test
    @DisplayName("should return 100.0 when totalImports is 0")
    void shouldReturn100WhenTotalImportsIsZero() {
      final ImportValidation validation =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 0, 0, Duration.ZERO);

      assertEquals(
          100.0,
          validation.getValidationRate(),
          0.001,
          "Validation rate should be 100.0 when totalImports is 0");
    }

    @Test
    @DisplayName("should return 100.0 when all imports are valid")
    void shouldReturn100WhenAllValid() {
      final ImportValidation validation =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 10, 10, Duration.ZERO);

      assertEquals(
          100.0,
          validation.getValidationRate(),
          0.001,
          "Validation rate should be 100.0 when all valid");
    }

    @Test
    @DisplayName("should return 50.0 when half imports are valid")
    void shouldReturn50WhenHalfValid() {
      final ImportValidation validation =
          new ImportValidation(
              false, Collections.emptyList(), Collections.emptyList(), 10, 5, Duration.ZERO);

      assertEquals(
          50.0,
          validation.getValidationRate(),
          0.001,
          "Validation rate should be 50.0 when half valid");
    }

    @Test
    @DisplayName("should return 0.0 when no imports are valid")
    void shouldReturn0WhenNoneValid() {
      final ImportValidation validation =
          new ImportValidation(
              false, Collections.emptyList(), Collections.emptyList(), 5, 0, Duration.ZERO);

      assertEquals(
          0.0,
          validation.getValidationRate(),
          0.001,
          "Validation rate should be 0.0 when none valid");
    }

    @Test
    @DisplayName("should calculate fractional rate correctly")
    void shouldCalculateFractionalRateCorrectly() {
      final ImportValidation validation =
          new ImportValidation(
              false, Collections.emptyList(), Collections.emptyList(), 3, 1, Duration.ZERO);

      assertEquals(
          33.333,
          validation.getValidationRate(),
          0.01,
          "Validation rate should be approximately 33.33 for 1/3");
    }
  }

  @Nested
  @DisplayName("Severity Filtering Tests")
  class SeverityFilteringTests {

    @Test
    @DisplayName("getIssuesBySeverity should filter by specified severity")
    void getIssuesBySeverityShouldFilter() {
      final List<ImportIssue> issues =
          List.of(
              createIssue(ImportIssue.Severity.ERROR),
              createIssue(ImportIssue.Severity.WARNING),
              createIssue(ImportIssue.Severity.ERROR),
              createIssue(ImportIssue.Severity.INFO));

      final ImportValidation validation =
          new ImportValidation(
              false, issues, Collections.emptyList(), 4, 1, Duration.ofMillis(20));

      final List<ImportIssue> errors =
          validation.getIssuesBySeverity(ImportIssue.Severity.ERROR);

      assertEquals(2, errors.size(), "Should have 2 ERROR issues");
    }

    @Test
    @DisplayName("getIssuesBySeverity should return empty list when no matches")
    void getIssuesBySeverityShouldReturnEmptyWhenNoMatches() {
      final List<ImportIssue> issues = List.of(createIssue(ImportIssue.Severity.WARNING));

      final ImportValidation validation =
          new ImportValidation(
              false, issues, Collections.emptyList(), 1, 0, Duration.ZERO);

      final List<ImportIssue> criticals =
          validation.getIssuesBySeverity(ImportIssue.Severity.CRITICAL);

      assertTrue(criticals.isEmpty(), "Should have no CRITICAL issues");
    }

    @Test
    @DisplayName("getIssuesBySeverity should throw NullPointerException for null severity")
    void getIssuesBySeverityShouldThrowForNullSeverity() {
      final ImportValidation validation =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 0, 0, Duration.ZERO);

      assertThrows(
          NullPointerException.class,
          () -> validation.getIssuesBySeverity(null),
          "null severity should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("hasCriticalIssues Tests")
  class HasCriticalIssuesTests {

    @Test
    @DisplayName("should return true when critical issues exist")
    void shouldReturnTrueWhenCriticalIssuesExist() {
      final List<ImportIssue> issues =
          List.of(
              createIssue(ImportIssue.Severity.WARNING),
              createIssue(ImportIssue.Severity.CRITICAL));

      final ImportValidation validation =
          new ImportValidation(
              false, issues, Collections.emptyList(), 2, 0, Duration.ZERO);

      assertTrue(validation.hasCriticalIssues(), "hasCriticalIssues should return true");
    }

    @Test
    @DisplayName("should return false when no critical issues exist")
    void shouldReturnFalseWhenNoCriticalIssues() {
      final List<ImportIssue> issues =
          List.of(
              createIssue(ImportIssue.Severity.WARNING),
              createIssue(ImportIssue.Severity.ERROR));

      final ImportValidation validation =
          new ImportValidation(
              false, issues, Collections.emptyList(), 2, 0, Duration.ZERO);

      assertFalse(validation.hasCriticalIssues(), "hasCriticalIssues should return false");
    }

    @Test
    @DisplayName("should return false when issues list is empty")
    void shouldReturnFalseWhenIssuesEmpty() {
      final ImportValidation validation =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 0, 0, Duration.ZERO);

      assertFalse(
          validation.hasCriticalIssues(),
          "hasCriticalIssues should return false for empty issues");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical values")
    void equalsShouldReturnTrueForIdenticalValues() {
      final Duration duration = Duration.ofMillis(100);
      final ImportValidation v1 =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 5, 5, duration);
      final ImportValidation v2 =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 5, 5, duration);

      assertEquals(v1, v2, "ImportValidation with same values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different valid flag")
    void equalsShouldReturnFalseForDifferentValidFlag() {
      final Duration duration = Duration.ofMillis(100);
      final ImportValidation v1 =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 5, 5, duration);
      final ImportValidation v2 =
          new ImportValidation(
              false, Collections.emptyList(), Collections.emptyList(), 5, 5, duration);

      assertNotEquals(v1, v2, "Different valid flag should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different totalImports")
    void equalsShouldReturnFalseForDifferentTotalImports() {
      final Duration duration = Duration.ofMillis(100);
      final ImportValidation v1 =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 5, 5, duration);
      final ImportValidation v2 =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 10, 5, duration);

      assertNotEquals(v1, v2, "Different totalImports should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final ImportValidation v =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 0, 0, Duration.ZERO);

      assertEquals(v, v, "Same object should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final ImportValidation v =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 0, 0, Duration.ZERO);

      assertNotEquals(null, v, "ImportValidation should not be equal to null");
    }

    @Test
    @DisplayName("hashCode should be consistent for equal instances")
    void hashCodeShouldBeConsistentForEqualInstances() {
      final Duration duration = Duration.ofMillis(100);
      final ImportValidation v1 =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 5, 5, duration);
      final ImportValidation v2 =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 5, 5, duration);

      assertEquals(
          v1.hashCode(),
          v2.hashCode(),
          "Equal ImportValidation instances should have same hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain ImportValidation prefix")
    void toStringShouldContainPrefix() {
      final ImportValidation v =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 5, 5, Duration.ofMillis(50));

      final String result = v.toString();

      assertTrue(
          result.contains("ImportValidation{"), "toString should contain 'ImportValidation{'");
    }

    @Test
    @DisplayName("toString should contain valid flag")
    void toStringShouldContainValidFlag() {
      final ImportValidation v =
          new ImportValidation(
              false, Collections.emptyList(), Collections.emptyList(), 5, 3, Duration.ZERO);

      final String result = v.toString();

      assertTrue(result.contains("valid=false"), "toString should contain valid=false");
    }

    @Test
    @DisplayName("toString should contain import counts")
    void toStringShouldContainImportCounts() {
      final ImportValidation v =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 10, 8, Duration.ZERO);

      final String result = v.toString();

      assertTrue(result.contains("imports=10"), "toString should contain imports=10");
      assertTrue(result.contains("validImports=8"), "toString should contain validImports=8");
    }

    @Test
    @DisplayName("toString should contain validation rate percentage")
    void toStringShouldContainValidationRate() {
      final ImportValidation v =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 10, 10, Duration.ZERO);

      final String result = v.toString();

      assertTrue(result.contains("100.0%"), "toString should contain validation rate percentage");
    }

    @Test
    @DisplayName("toString should not return null")
    void toStringShouldNotReturnNull() {
      final ImportValidation v =
          new ImportValidation(
              true, Collections.emptyList(), Collections.emptyList(), 0, 0, Duration.ZERO);

      assertNotNull(v.toString(), "toString should not return null");
    }
  }
}
