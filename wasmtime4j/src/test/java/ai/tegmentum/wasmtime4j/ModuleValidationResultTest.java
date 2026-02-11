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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ai.tegmentum.wasmtime4j.validation.ModuleValidationResult;

/**
 * Tests for {@link ModuleValidationResult} class.
 *
 * <p>ModuleValidationResult encapsulates the result of validating WebAssembly bytecode, including
 * validity status, errors, and warnings. Lists are wrapped as unmodifiable.
 */
@DisplayName("ModuleValidationResult Tests")
class ModuleValidationResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ModuleValidationResult.class.getModifiers()),
          "ModuleValidationResult should be public");
      assertTrue(
          Modifier.isFinal(ModuleValidationResult.class.getModifiers()),
          "ModuleValidationResult should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create result with valid status and empty lists")
    void shouldCreateResultWithValidStatusAndEmptyLists() {
      final ModuleValidationResult result =
          new ModuleValidationResult(true, Collections.emptyList(), Collections.emptyList());

      assertTrue(result.isValid(), "Result should be valid");
      assertTrue(result.getErrors().isEmpty(), "Errors list should be empty");
      assertTrue(result.getWarnings().isEmpty(), "Warnings list should be empty");
    }

    @Test
    @DisplayName("should create result with errors and warnings")
    void shouldCreateResultWithErrorsAndWarnings() {
      final List<String> errors = List.of("Error 1", "Error 2");
      final List<String> warnings = List.of("Warning 1");

      final ModuleValidationResult result = new ModuleValidationResult(false, errors, warnings);

      assertFalse(result.isValid(), "Result should not be valid");
      assertEquals(2, result.getErrors().size(), "Should have 2 errors");
      assertEquals(1, result.getWarnings().size(), "Should have 1 warning");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null errors list")
    void shouldThrowIaeForNullErrors() {
      final IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> new ModuleValidationResult(false, null, Collections.emptyList()),
          "Null errors list should throw IllegalArgumentException");
      assertTrue(exception.getMessage().contains("null"),
          "Exception message should mention null");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null warnings list")
    void shouldThrowIaeForNullWarnings() {
      final IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> new ModuleValidationResult(true, Collections.emptyList(), null),
          "Null warnings list should throw IllegalArgumentException");
      assertTrue(exception.getMessage().contains("null"),
          "Exception message should mention null");
    }

    @Test
    @DisplayName("should wrap errors list as unmodifiable")
    void shouldWrapErrorsListAsUnmodifiable() {
      final List<String> errors = new ArrayList<>();
      errors.add("Error 1");
      final ModuleValidationResult result =
          new ModuleValidationResult(false, errors, Collections.emptyList());

      assertThrows(
          UnsupportedOperationException.class,
          () -> result.getErrors().add("New error"),
          "Errors list should be unmodifiable");
    }

    @Test
    @DisplayName("should wrap warnings list as unmodifiable")
    void shouldWrapWarningsListAsUnmodifiable() {
      final List<String> warnings = new ArrayList<>();
      warnings.add("Warning 1");
      final ModuleValidationResult result =
          new ModuleValidationResult(true, Collections.emptyList(), warnings);

      assertThrows(
          UnsupportedOperationException.class,
          () -> result.getWarnings().add("New warning"),
          "Warnings list should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("success should create valid result with empty lists")
    void successShouldCreateValidResultWithEmptyLists() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertNotNull(result, "Success result should not be null");
      assertTrue(result.isValid(), "Success result should be valid");
      assertTrue(result.getErrors().isEmpty(), "Success result should have no errors");
      assertTrue(result.getWarnings().isEmpty(), "Success result should have no warnings");
    }

    @Test
    @DisplayName("failure should create invalid result with errors")
    void failureShouldCreateInvalidResultWithErrors() {
      final List<String> errors = List.of("Invalid magic number", "Unsupported version");

      final ModuleValidationResult result = ModuleValidationResult.failure(errors);

      assertFalse(result.isValid(), "Failure result should not be valid");
      assertEquals(2, result.getErrors().size(), "Failure result should have 2 errors");
      assertEquals("Invalid magic number", result.getErrors().get(0),
          "First error should match");
      assertEquals("Unsupported version", result.getErrors().get(1),
          "Second error should match");
      assertTrue(result.getWarnings().isEmpty(),
          "Failure result should have no warnings");
    }

    @Test
    @DisplayName("successWithWarnings should create valid result with warnings")
    void successWithWarningsShouldCreateValidResultWithWarnings() {
      final List<String> warnings = List.of("Deprecated feature used", "Large memory allocation");

      final ModuleValidationResult result = ModuleValidationResult.successWithWarnings(warnings);

      assertTrue(result.isValid(), "Result should be valid");
      assertTrue(result.getErrors().isEmpty(), "Should have no errors");
      assertEquals(2, result.getWarnings().size(), "Should have 2 warnings");
      assertEquals("Deprecated feature used", result.getWarnings().get(0),
          "First warning should match");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("hasErrors should return true when errors exist")
    void hasErrorsShouldReturnTrueWhenErrorsExist() {
      final ModuleValidationResult result =
          ModuleValidationResult.failure(List.of("Some error"));

      assertTrue(result.hasErrors(), "hasErrors should return true when errors exist");
    }

    @Test
    @DisplayName("hasErrors should return false when no errors")
    void hasErrorsShouldReturnFalseWhenNoErrors() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertFalse(result.hasErrors(), "hasErrors should return false when no errors");
    }

    @Test
    @DisplayName("hasWarnings should return true when warnings exist")
    void hasWarningsShouldReturnTrueWhenWarningsExist() {
      final ModuleValidationResult result =
          ModuleValidationResult.successWithWarnings(List.of("Some warning"));

      assertTrue(result.hasWarnings(), "hasWarnings should return true when warnings exist");
    }

    @Test
    @DisplayName("hasWarnings should return false when no warnings")
    void hasWarningsShouldReturnFalseWhenNoWarnings() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertFalse(result.hasWarnings(), "hasWarnings should return false when no warnings");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical results")
    void equalsShouldReturnTrueForIdenticalResults() {
      final ModuleValidationResult result1 =
          new ModuleValidationResult(true, List.of("err"), List.of("warn"));
      final ModuleValidationResult result2 =
          new ModuleValidationResult(true, List.of("err"), List.of("warn"));

      assertEquals(result1, result2, "Results with identical values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different validity")
    void equalsShouldReturnFalseForDifferentValidity() {
      final ModuleValidationResult result1 =
          new ModuleValidationResult(true, Collections.emptyList(), Collections.emptyList());
      final ModuleValidationResult result2 =
          new ModuleValidationResult(false, Collections.emptyList(), Collections.emptyList());

      assertNotEquals(result1, result2,
          "Results with different validity should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different errors")
    void equalsShouldReturnFalseForDifferentErrors() {
      final ModuleValidationResult result1 =
          new ModuleValidationResult(false, List.of("Error A"), Collections.emptyList());
      final ModuleValidationResult result2 =
          new ModuleValidationResult(false, List.of("Error B"), Collections.emptyList());

      assertNotEquals(result1, result2,
          "Results with different errors should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same reference")
    void equalsShouldReturnTrueForSameReference() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertEquals(result, result, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertNotEquals(null, result, "Result should not be equal to null");
    }

    @Test
    @DisplayName("hashCode should be equal for equal objects")
    void hashCodeShouldBeEqualForEqualObjects() {
      final ModuleValidationResult result1 = ModuleValidationResult.success();
      final ModuleValidationResult result2 = ModuleValidationResult.success();

      assertEquals(result1.hashCode(), result2.hashCode(),
          "Equal objects should have equal hash codes");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain valid status")
    void toStringShouldContainValidStatus() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertTrue(result.toString().contains("valid=true"),
          "toString should contain valid=true for success");
    }

    @Test
    @DisplayName("toString should contain error count when errors present")
    void toStringShouldContainErrorCountWhenErrorsPresent() {
      final ModuleValidationResult result =
          ModuleValidationResult.failure(List.of("Error 1", "Error 2"));

      final String str = result.toString();

      assertTrue(str.contains("errors=2"), "toString should contain error count");
    }

    @Test
    @DisplayName("toString should contain warning count when warnings present")
    void toStringShouldContainWarningCountWhenWarningsPresent() {
      final ModuleValidationResult result =
          ModuleValidationResult.successWithWarnings(List.of("Warn 1", "Warn 2", "Warn 3"));

      final String str = result.toString();

      assertTrue(str.contains("warnings=3"), "toString should contain warning count");
    }

    @Test
    @DisplayName("toString should start with ModuleValidationResult")
    void toStringShouldStartWithPrefix() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertTrue(result.toString().startsWith("ModuleValidationResult{"),
          "toString should start with 'ModuleValidationResult{'");
    }
  }
}
