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

import ai.tegmentum.wasmtime4j.validation.ModuleValidationResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleValidationResult} class.
 *
 * <p>ModuleValidationResult encapsulates the result of validating WebAssembly bytecode, including
 * validity status and errors. Lists are wrapped as unmodifiable.
 */
@DisplayName("ModuleValidationResult Tests")
class ModuleValidationResultTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create result with valid status and empty lists")
    void shouldCreateResultWithValidStatusAndEmptyLists() {
      final ModuleValidationResult result =
          new ModuleValidationResult(true, Collections.emptyList());

      assertTrue(result.isValid(), "Result should be valid");
      assertTrue(result.getErrors().isEmpty(), "Errors list should be empty");
    }

    @Test
    @DisplayName("should create result with errors")
    void shouldCreateResultWithErrors() {
      final List<String> errors = List.of("Error 1", "Error 2");

      final ModuleValidationResult result = new ModuleValidationResult(false, errors);

      assertFalse(result.isValid(), "Result should not be valid");
      assertEquals(2, result.getErrors().size(), "Should have 2 errors");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null errors list")
    void shouldThrowIaeForNullErrors() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new ModuleValidationResult(false, null),
              "Null errors list should throw IllegalArgumentException");
      assertTrue(exception.getMessage().contains("null"), "Exception message should mention null");
    }

    @Test
    @DisplayName("should wrap errors list as unmodifiable")
    void shouldWrapErrorsListAsUnmodifiable() {
      final List<String> errors = new ArrayList<>();
      errors.add("Error 1");
      final ModuleValidationResult result = new ModuleValidationResult(false, errors);

      assertThrows(
          UnsupportedOperationException.class,
          () -> result.getErrors().add("New error"),
          "Errors list should be unmodifiable");
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
    }

    @Test
    @DisplayName("failure should create invalid result with errors")
    void failureShouldCreateInvalidResultWithErrors() {
      final List<String> errors = List.of("Invalid magic number", "Unsupported version");

      final ModuleValidationResult result = ModuleValidationResult.failure(errors);

      assertFalse(result.isValid(), "Failure result should not be valid");
      assertEquals(2, result.getErrors().size(), "Failure result should have 2 errors");
      assertEquals("Invalid magic number", result.getErrors().get(0), "First error should match");
      assertEquals("Unsupported version", result.getErrors().get(1), "Second error should match");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("hasErrors should return true when errors exist")
    void hasErrorsShouldReturnTrueWhenErrorsExist() {
      final ModuleValidationResult result = ModuleValidationResult.failure(List.of("Some error"));

      assertTrue(result.hasErrors(), "hasErrors should return true when errors exist");
    }

    @Test
    @DisplayName("hasErrors should return false when no errors")
    void hasErrorsShouldReturnFalseWhenNoErrors() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertFalse(result.hasErrors(), "hasErrors should return false when no errors");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical results")
    void equalsShouldReturnTrueForIdenticalResults() {
      final ModuleValidationResult result1 = new ModuleValidationResult(true, List.of("err"));
      final ModuleValidationResult result2 = new ModuleValidationResult(true, List.of("err"));

      assertEquals(result1, result2, "Results with identical values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different validity")
    void equalsShouldReturnFalseForDifferentValidity() {
      final ModuleValidationResult result1 =
          new ModuleValidationResult(true, Collections.emptyList());
      final ModuleValidationResult result2 =
          new ModuleValidationResult(false, Collections.emptyList());

      assertNotEquals(result1, result2, "Results with different validity should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different errors")
    void equalsShouldReturnFalseForDifferentErrors() {
      final ModuleValidationResult result1 = new ModuleValidationResult(false, List.of("Error A"));
      final ModuleValidationResult result2 = new ModuleValidationResult(false, List.of("Error B"));

      assertNotEquals(result1, result2, "Results with different errors should not be equal");
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

      assertEquals(
          result1.hashCode(), result2.hashCode(), "Equal objects should have equal hash codes");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain valid status")
    void toStringShouldContainValidStatus() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertTrue(
          result.toString().contains("valid=true"),
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
    @DisplayName("toString should start with ModuleValidationResult")
    void toStringShouldStartWithPrefix() {
      final ModuleValidationResult result = ModuleValidationResult.success();

      assertTrue(
          result.toString().startsWith("ModuleValidationResult{"),
          "toString should start with 'ModuleValidationResult{'");
    }
  }
}
