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

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentCompatibilityResult} class.
 *
 * <p>ComponentCompatibilityResult provides detailed information about component compatibility.
 */
@DisplayName("ComponentCompatibilityResult Tests")
class ComponentCompatibilityResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentCompatibilityResult.class.getModifiers()),
          "ComponentCompatibilityResult should be public");
      assertTrue(
          Modifier.isFinal(ComponentCompatibilityResult.class.getModifiers()),
          "ComponentCompatibilityResult should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create compatible result")
    void shouldCreateCompatibleResult() {
      final var result = new ComponentCompatibilityResult(true, "Compatible");

      assertTrue(result.isCompatible(), "Should be compatible");
      assertEquals("Compatible", result.getDetails(), "Details should match");
    }

    @Test
    @DisplayName("should create incompatible result")
    void shouldCreateIncompatibleResult() {
      final var result = new ComponentCompatibilityResult(false, "Version mismatch");

      assertFalse(result.isCompatible(), "Should not be compatible");
      assertEquals("Version mismatch", result.getDetails(), "Details should match");
    }

    @Test
    @DisplayName("should handle null details")
    void shouldHandleNullDetails() {
      final var result = new ComponentCompatibilityResult(true, null);

      assertTrue(result.isCompatible(), "Should be compatible");
      assertEquals("", result.getDetails(), "Null details should default to empty string");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("compatible factory method should create compatible result")
    void compatibleFactoryMethodShouldCreateCompatibleResult() {
      final var result = ComponentCompatibilityResult.compatible("All checks passed");

      assertTrue(result.isCompatible(), "Should be compatible");
      assertEquals("All checks passed", result.getDetails(), "Details should match");
    }

    @Test
    @DisplayName("incompatible factory method should create incompatible result")
    void incompatibleFactoryMethodShouldCreateIncompatibleResult() {
      final var result = ComponentCompatibilityResult.incompatible("Interface mismatch");

      assertFalse(result.isCompatible(), "Should not be compatible");
      assertEquals("Interface mismatch", result.getDetails(), "Details should match");
    }

    @Test
    @DisplayName("compatible factory method should handle null details")
    void compatibleFactoryMethodShouldHandleNullDetails() {
      final var result = ComponentCompatibilityResult.compatible(null);

      assertTrue(result.isCompatible(), "Should be compatible");
      assertEquals("", result.getDetails(), "Null should default to empty string");
    }

    @Test
    @DisplayName("incompatible factory method should handle null details")
    void incompatibleFactoryMethodShouldHandleNullDetails() {
      final var result = ComponentCompatibilityResult.incompatible(null);

      assertFalse(result.isCompatible(), "Should not be compatible");
      assertEquals("", result.getDetails(), "Null should default to empty string");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("isCompatible should return true when compatible")
    void isCompatibleShouldReturnTrueWhenCompatible() {
      final var result = new ComponentCompatibilityResult(true, "details");

      assertTrue(result.isCompatible(), "Should return true");
    }

    @Test
    @DisplayName("isCompatible should return false when incompatible")
    void isCompatibleShouldReturnFalseWhenIncompatible() {
      final var result = new ComponentCompatibilityResult(false, "details");

      assertFalse(result.isCompatible(), "Should return false");
    }

    @Test
    @DisplayName("getDetails should return correct value")
    void getDetailsShouldReturnCorrectValue() {
      final var result = new ComponentCompatibilityResult(true, "Specific details here");

      assertEquals("Specific details here", result.getDetails(), "Should return details");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty string details")
    void shouldHandleEmptyStringDetails() {
      final var result = new ComponentCompatibilityResult(true, "");

      assertEquals("", result.getDetails(), "Empty string should be preserved");
    }

    @Test
    @DisplayName("should handle long details string")
    void shouldHandleLongDetailsString() {
      final String longDetails = "X".repeat(10000);
      final var result = new ComponentCompatibilityResult(true, longDetails);

      assertEquals(10000, result.getDetails().length(), "Should handle long strings");
    }

    @Test
    @DisplayName("should handle special characters in details")
    void shouldHandleSpecialCharactersInDetails() {
      final String specialDetails = "Error: \"Missing interface\" at line 5\nStack: [...]";
      final var result = new ComponentCompatibilityResult(false, specialDetails);

      assertEquals(specialDetails, result.getDetails(), "Should handle special characters");
    }

    @Test
    @DisplayName("should handle unicode in details")
    void shouldHandleUnicodeInDetails() {
      final String unicodeDetails = "インターフェース不一致 🔴";
      final var result = new ComponentCompatibilityResult(false, unicodeDetails);

      assertEquals(unicodeDetails, result.getDetails(), "Should handle unicode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string for compatible result")
    void toStringShouldReturnFormattedStringForCompatibleResult() {
      final var result = ComponentCompatibilityResult.compatible("Success");

      final String str = result.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("true") || str.contains("compatible"), "Should indicate compatible");
      assertTrue(str.contains("Success"), "Should contain details");
    }

    @Test
    @DisplayName("toString should return formatted string for incompatible result")
    void toStringShouldReturnFormattedStringForIncompatibleResult() {
      final var result = ComponentCompatibilityResult.incompatible("Failure");

      final String str = result.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(
          str.contains("false") || str.contains("compatible"), "Should indicate incompatible");
      assertTrue(str.contains("Failure"), "Should contain details");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support typical compatibility check flow")
    void shouldSupportTypicalCompatibilityCheckFlow() {
      // Simulate a compatibility check
      final ComponentCompatibilityResult result = checkCompatibility();

      if (result.isCompatible()) {
        // Proceed with operation
        assertNotNull(result.getDetails(), "Details should be available");
      } else {
        // Handle incompatibility
        assertNotNull(result.getDetails(), "Error details should be available");
      }
    }

    @Test
    @DisplayName("should provide meaningful details for debugging")
    void shouldProvideMeaningfulDetailsForDebugging() {
      final var result =
          ComponentCompatibilityResult.incompatible(
              "Required interface 'wasi:http/types' not found in component");

      assertFalse(result.isCompatible(), "Should be incompatible");
      assertTrue(
          result.getDetails().contains("wasi:http/types"),
          "Details should contain specific interface name");
    }

    private ComponentCompatibilityResult checkCompatibility() {
      // Simulated check
      return ComponentCompatibilityResult.compatible("All interfaces match");
    }
  }
}
