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

import ai.tegmentum.wasmtime4j.validation.LinkingValidationResult;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LinkingValidationResult} class.
 *
 * <p>LinkingValidationResult provides information about component linking configuration validation.
 * Has a public constructor, valid() and invalid() static factories, and toString. No
 * equals/hashCode.
 */
@DisplayName("LinkingValidationResult Tests")
class LinkingValidationResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(LinkingValidationResult.class.getModifiers()),
          "LinkingValidationResult should be public");
      assertTrue(
          Modifier.isFinal(LinkingValidationResult.class.getModifiers()),
          "LinkingValidationResult should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create valid result with details")
    void shouldCreateValidResultWithDetails() {
      final LinkingValidationResult result =
          new LinkingValidationResult(true, "All imports satisfied");

      assertTrue(result.isValid(), "Result should be valid");
      assertEquals(
          "All imports satisfied", result.getDetails(), "Details should match constructor value");
    }

    @Test
    @DisplayName("should create invalid result with details")
    void shouldCreateInvalidResultWithDetails() {
      final LinkingValidationResult result =
          new LinkingValidationResult(false, "Missing import: env.memory");

      assertFalse(result.isValid(), "Result should not be valid");
      assertEquals(
          "Missing import: env.memory",
          result.getDetails(),
          "Details should match constructor value");
    }

    @Test
    @DisplayName("should coalesce null details to empty string")
    void shouldCoalesceNullDetailsToEmptyString() {
      final LinkingValidationResult result = new LinkingValidationResult(true, null);

      assertEquals("", result.getDetails(), "Null details should be coalesced to empty string");
    }

    @Test
    @DisplayName("should preserve empty string details")
    void shouldPreserveEmptyStringDetails() {
      final LinkingValidationResult result = new LinkingValidationResult(true, "");

      assertEquals("", result.getDetails(), "Empty string details should be preserved");
    }
  }

  @Nested
  @DisplayName("Valid Factory Tests")
  class ValidFactoryTests {

    @Test
    @DisplayName("valid should create valid result")
    void validShouldCreateValidResult() {
      final LinkingValidationResult result =
          LinkingValidationResult.valid("Linking configuration is correct");

      assertNotNull(result, "Valid result should not be null");
      assertTrue(result.isValid(), "Result from valid() should be valid");
      assertEquals("Linking configuration is correct", result.getDetails(), "Details should match");
    }

    @Test
    @DisplayName("valid should handle null details")
    void validShouldHandleNullDetails() {
      final LinkingValidationResult result = LinkingValidationResult.valid(null);

      assertTrue(result.isValid(), "Result should be valid");
      assertEquals("", result.getDetails(), "Null details should be coalesced to empty string");
    }
  }

  @Nested
  @DisplayName("Invalid Factory Tests")
  class InvalidFactoryTests {

    @Test
    @DisplayName("invalid should create invalid result")
    void invalidShouldCreateInvalidResult() {
      final LinkingValidationResult result =
          LinkingValidationResult.invalid("Missing function: start");

      assertNotNull(result, "Invalid result should not be null");
      assertFalse(result.isValid(), "Result from invalid() should not be valid");
      assertEquals("Missing function: start", result.getDetails(), "Details should match");
    }

    @Test
    @DisplayName("invalid should handle null details")
    void invalidShouldHandleNullDetails() {
      final LinkingValidationResult result = LinkingValidationResult.invalid(null);

      assertFalse(result.isValid(), "Result should not be valid");
      assertEquals("", result.getDetails(), "Null details should be coalesced to empty string");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("isValid should return true for valid constructor")
    void isValidShouldReturnTrueForValidConstructor() {
      final LinkingValidationResult result = new LinkingValidationResult(true, "details");

      assertTrue(result.isValid(), "isValid should return true");
    }

    @Test
    @DisplayName("isValid should return false for invalid constructor")
    void isValidShouldReturnFalseForInvalidConstructor() {
      final LinkingValidationResult result = new LinkingValidationResult(false, "details");

      assertFalse(result.isValid(), "isValid should return false");
    }

    @Test
    @DisplayName("getDetails should return the details string")
    void getDetailsShouldReturnDetailsString() {
      final LinkingValidationResult result =
          new LinkingValidationResult(true, "All 5 imports resolved successfully");

      assertEquals(
          "All 5 imports resolved successfully",
          result.getDetails(),
          "Details should match constructor value");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain LinkingValidationResult prefix")
    void toStringShouldContainPrefix() {
      final LinkingValidationResult result = new LinkingValidationResult(true, "OK");

      assertTrue(
          result.toString().startsWith("LinkingValidationResult{"),
          "toString should start with 'LinkingValidationResult{'");
    }

    @Test
    @DisplayName("toString should contain valid status")
    void toStringShouldContainValidStatus() {
      final LinkingValidationResult result = new LinkingValidationResult(true, "OK");

      assertTrue(result.toString().contains("valid=true"), "toString should contain valid=true");
    }

    @Test
    @DisplayName("toString should contain details")
    void toStringShouldContainDetails() {
      final LinkingValidationResult result =
          new LinkingValidationResult(false, "Missing memory import");

      assertTrue(
          result.toString().contains("Missing memory import"),
          "toString should contain the details string");
    }

    @Test
    @DisplayName("toString should show false for invalid result")
    void toStringShouldShowFalseForInvalidResult() {
      final LinkingValidationResult result = LinkingValidationResult.invalid("Error");

      assertTrue(
          result.toString().contains("valid=false"),
          "toString should contain valid=false for invalid result");
    }
  }
}
