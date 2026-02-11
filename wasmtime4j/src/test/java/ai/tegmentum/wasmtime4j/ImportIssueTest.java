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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.validation.ImportIssue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ImportIssue} class.
 *
 * <p>ImportIssue represents an issue found during import validation including missing imports, type
 * mismatches, and other problems.
 */
@DisplayName("ImportIssue Tests")
class ImportIssueTest {

  @Nested
  @DisplayName("Full Constructor Tests")
  class FullConstructorTests {

    @Test
    @DisplayName("should create instance with all 7 parameters")
    void shouldCreateInstanceWithAllParameters() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.ERROR,
              ImportIssue.Type.TYPE_MISMATCH,
              "env",
              "memory",
              "Memory type mismatch",
              "Memory(1, 10)",
              "Memory(1, 5)");

      assertEquals(ImportIssue.Severity.ERROR, issue.getSeverity(), "severity should be ERROR");
      assertEquals(ImportIssue.Type.TYPE_MISMATCH, issue.getType(), "type should be TYPE_MISMATCH");
      assertEquals("env", issue.getModuleName(), "moduleName should be 'env'");
      assertEquals("memory", issue.getImportName(), "importName should be 'memory'");
      assertEquals("Memory type mismatch", issue.getMessage(), "message should match");
      assertEquals("Memory(1, 10)", issue.getExpectedType(), "expectedType should match");
      assertEquals("Memory(1, 5)", issue.getActualType(), "actualType should match");
    }

    @Test
    @DisplayName("should allow null expectedType and actualType")
    void shouldAllowNullExpectedAndActualType() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.WARNING,
              ImportIssue.Type.MISSING_IMPORT,
              "mod",
              "fn",
              "Import not found",
              null,
              null);

      assertNull(issue.getExpectedType(), "expectedType should be null");
      assertNull(issue.getActualType(), "actualType should be null");
    }
  }

  @Nested
  @DisplayName("Short Constructor Tests")
  class ShortConstructorTests {

    @Test
    @DisplayName("should create instance with 5 parameters")
    void shouldCreateInstanceWith5Parameters() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.CRITICAL,
              ImportIssue.Type.CIRCULAR_DEPENDENCY,
              "moduleA",
              "funcB",
              "Circular dependency detected");

      assertEquals(
          ImportIssue.Severity.CRITICAL, issue.getSeverity(), "severity should be CRITICAL");
      assertEquals(
          ImportIssue.Type.CIRCULAR_DEPENDENCY,
          issue.getType(),
          "type should be CIRCULAR_DEPENDENCY");
      assertEquals("moduleA", issue.getModuleName(), "moduleName should match");
      assertEquals("funcB", issue.getImportName(), "importName should match");
      assertEquals("Circular dependency detected", issue.getMessage(), "message should match");
      assertNull(issue.getExpectedType(), "expectedType should be null");
      assertNull(issue.getActualType(), "actualType should be null");
    }
  }

  @Nested
  @DisplayName("Null Validation Tests")
  class NullValidationTests {

    @Test
    @DisplayName("should throw NullPointerException for null severity")
    void shouldThrowForNullSeverity() {
      assertThrows(
          NullPointerException.class,
          () -> new ImportIssue(null, ImportIssue.Type.MISSING_IMPORT, "mod", "fn", "msg"),
          "null severity should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null type")
    void shouldThrowForNullType() {
      assertThrows(
          NullPointerException.class,
          () -> new ImportIssue(ImportIssue.Severity.ERROR, null, "mod", "fn", "msg"),
          "null type should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null moduleName")
    void shouldThrowForNullModuleName() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportIssue(
                  ImportIssue.Severity.ERROR, ImportIssue.Type.MISSING_IMPORT, null, "fn", "msg"),
          "null moduleName should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null importName")
    void shouldThrowForNullImportName() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportIssue(
                  ImportIssue.Severity.ERROR, ImportIssue.Type.MISSING_IMPORT, "mod", null, "msg"),
          "null importName should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null message")
    void shouldThrowForNullMessage() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportIssue(
                  ImportIssue.Severity.ERROR, ImportIssue.Type.MISSING_IMPORT, "mod", "fn", null),
          "null message should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Severity Enum Tests")
  class SeverityEnumTests {

    @Test
    @DisplayName("should have all expected severity levels")
    void shouldHaveAllExpectedSeverityLevels() {
      final ImportIssue.Severity[] values = ImportIssue.Severity.values();

      assertEquals(4, values.length, "Should have 4 Severity values");
      assertNotNull(ImportIssue.Severity.INFO, "INFO should exist");
      assertNotNull(ImportIssue.Severity.WARNING, "WARNING should exist");
      assertNotNull(ImportIssue.Severity.ERROR, "ERROR should exist");
      assertNotNull(ImportIssue.Severity.CRITICAL, "CRITICAL should exist");
    }

    @Test
    @DisplayName("valueOf should return correct enum values")
    void valueOfShouldReturnCorrectValues() {
      assertEquals(
          ImportIssue.Severity.INFO,
          ImportIssue.Severity.valueOf("INFO"),
          "valueOf('INFO') should return INFO");
      assertEquals(
          ImportIssue.Severity.CRITICAL,
          ImportIssue.Severity.valueOf("CRITICAL"),
          "valueOf('CRITICAL') should return CRITICAL");
    }
  }

  @Nested
  @DisplayName("Type Enum Tests")
  class TypeEnumTests {

    @Test
    @DisplayName("should have all expected issue types")
    void shouldHaveAllExpectedIssueTypes() {
      final ImportIssue.Type[] values = ImportIssue.Type.values();

      assertEquals(8, values.length, "Should have 8 Type values");
      assertNotNull(ImportIssue.Type.MISSING_IMPORT, "MISSING_IMPORT should exist");
      assertNotNull(ImportIssue.Type.TYPE_MISMATCH, "TYPE_MISMATCH should exist");
      assertNotNull(ImportIssue.Type.CIRCULAR_DEPENDENCY, "CIRCULAR_DEPENDENCY should exist");
      assertNotNull(ImportIssue.Type.SIGNATURE_MISMATCH, "SIGNATURE_MISMATCH should exist");
      assertNotNull(ImportIssue.Type.MODULE_NOT_FOUND, "MODULE_NOT_FOUND should exist");
      assertNotNull(ImportIssue.Type.EXPORT_NOT_FOUND, "EXPORT_NOT_FOUND should exist");
      assertNotNull(ImportIssue.Type.AMBIGUOUS_IMPORT, "AMBIGUOUS_IMPORT should exist");
      assertNotNull(ImportIssue.Type.VALIDATION_FAILED, "VALIDATION_FAILED should exist");
    }
  }

  @Nested
  @DisplayName("getImportIdentifier Tests")
  class GetImportIdentifierTests {

    @Test
    @DisplayName("should return moduleName::importName format")
    void shouldReturnCorrectFormat() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.ERROR,
              ImportIssue.Type.MISSING_IMPORT,
              "wasi",
              "fd_read",
              "Missing import");

      assertEquals(
          "wasi::fd_read",
          issue.getImportIdentifier(),
          "getImportIdentifier should return 'wasi::fd_read'");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical values")
    void equalsShouldReturnTrueForIdenticalValues() {
      final ImportIssue issue1 =
          new ImportIssue(
              ImportIssue.Severity.ERROR,
              ImportIssue.Type.TYPE_MISMATCH,
              "mod",
              "fn",
              "msg",
              "expected",
              "actual");
      final ImportIssue issue2 =
          new ImportIssue(
              ImportIssue.Severity.ERROR,
              ImportIssue.Type.TYPE_MISMATCH,
              "mod",
              "fn",
              "msg",
              "expected",
              "actual");

      assertEquals(issue1, issue2, "ImportIssue with same values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different severity")
    void equalsShouldReturnFalseForDifferentSeverity() {
      final ImportIssue issue1 =
          new ImportIssue(
              ImportIssue.Severity.ERROR, ImportIssue.Type.MISSING_IMPORT, "mod", "fn", "msg");
      final ImportIssue issue2 =
          new ImportIssue(
              ImportIssue.Severity.WARNING, ImportIssue.Type.MISSING_IMPORT, "mod", "fn", "msg");

      assertNotEquals(issue1, issue2, "Different severity should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final ImportIssue issue1 =
          new ImportIssue(
              ImportIssue.Severity.ERROR, ImportIssue.Type.MISSING_IMPORT, "mod", "fn", "msg");
      final ImportIssue issue2 =
          new ImportIssue(
              ImportIssue.Severity.ERROR, ImportIssue.Type.TYPE_MISMATCH, "mod", "fn", "msg");

      assertNotEquals(issue1, issue2, "Different type should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.INFO, ImportIssue.Type.MISSING_IMPORT, "m", "i", "msg");

      assertEquals(issue, issue, "Same object should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.INFO, ImportIssue.Type.MISSING_IMPORT, "m", "i", "msg");

      assertNotEquals(null, issue, "ImportIssue should not be equal to null");
    }

    @Test
    @DisplayName("hashCode should be consistent for equal instances")
    void hashCodeShouldBeConsistentForEqualInstances() {
      final ImportIssue issue1 =
          new ImportIssue(
              ImportIssue.Severity.ERROR,
              ImportIssue.Type.TYPE_MISMATCH,
              "mod",
              "fn",
              "msg",
              "exp",
              "act");
      final ImportIssue issue2 =
          new ImportIssue(
              ImportIssue.Severity.ERROR,
              ImportIssue.Type.TYPE_MISMATCH,
              "mod",
              "fn",
              "msg",
              "exp",
              "act");

      assertEquals(
          issue1.hashCode(),
          issue2.hashCode(),
          "Equal ImportIssue instances should have same hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain severity and type")
    void toStringShouldContainSeverityAndType() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.ERROR,
              ImportIssue.Type.MISSING_IMPORT,
              "mod",
              "fn",
              "Not found");

      final String result = issue.toString();

      assertTrue(result.contains("ERROR"), "toString should contain severity");
      assertTrue(result.contains("MISSING_IMPORT"), "toString should contain type");
    }

    @Test
    @DisplayName("toString should contain import identifier and message")
    void toStringShouldContainIdentifierAndMessage() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.WARNING,
              ImportIssue.Type.SIGNATURE_MISMATCH,
              "env",
              "log",
              "Signature does not match");

      final String result = issue.toString();

      assertTrue(result.contains("env::log"), "toString should contain import identifier");
      assertTrue(result.contains("Signature does not match"), "toString should contain message");
    }

    @Test
    @DisplayName("toString should include expected and actual when present")
    void toStringShouldIncludeExpectedAndActualWhenPresent() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.ERROR,
              ImportIssue.Type.TYPE_MISMATCH,
              "mod",
              "fn",
              "Type mismatch",
              "(i32) -> i32",
              "(i64) -> i64");

      final String result = issue.toString();

      assertTrue(result.contains("expected: (i32) -> i32"), "toString should contain expected");
      assertTrue(result.contains("actual: (i64) -> i64"), "toString should contain actual");
    }

    @Test
    @DisplayName("toString should not include expected/actual when null")
    void toStringShouldNotIncludeExpectedActualWhenNull() {
      final ImportIssue issue =
          new ImportIssue(
              ImportIssue.Severity.INFO, ImportIssue.Type.MISSING_IMPORT, "mod", "fn", "Missing");

      final String result = issue.toString();

      // When both expectedType and actualType are null, the (expected: ..., actual: ...) block
      // should not be present
      assertTrue(
          !result.contains("expected:") || !result.contains("actual:"),
          "toString should not include expected/actual when null");
    }
  }
}
