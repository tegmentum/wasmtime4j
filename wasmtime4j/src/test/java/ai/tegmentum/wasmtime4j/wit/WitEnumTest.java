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
package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEnum} class.
 *
 * <p>WitEnum represents a WIT enum value (discriminated choice without payload).
 */
@DisplayName("WitEnum Tests")
class WitEnumTest {

  @Nested
  @DisplayName("Factory Method Null Validation Tests")
  class FactoryMethodNullTests {

    @Test
    @DisplayName("of with null discriminant should throw IllegalArgumentException")
    void ofWithNullDiscriminantShouldThrow() {
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(enumType, null),
          "of with null discriminant should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("of with empty discriminant should throw IllegalArgumentException")
    void ofWithEmptyDiscriminantShouldThrow() {
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(enumType, ""),
          "of with empty discriminant should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Valid Creation Tests")
  class ValidCreationTests {

    @Test
    @DisplayName("of with valid discriminant should create enum")
    void ofWithValidDiscriminantShouldCreateEnum() {
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      final WitEnum value = WitEnum.of(enumType, "red");

      assertNotNull(value, "Enum should not be null");
      assertEquals("red", value.getDiscriminant(), "Discriminant should be 'red'");
    }

    @Test
    @DisplayName("toJava should return discriminant string")
    void toJavaShouldReturnDiscriminant() {
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      final WitEnum value = WitEnum.of(enumType, "green");

      assertEquals("green", value.toJava(), "toJava should return the discriminant");
    }

    @Test
    @DisplayName("getType should return the enum type")
    void getTypeShouldReturnEnumType() {
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      final WitEnum value = WitEnum.of(enumType, "blue");

      assertNotNull(value.getType(), "Type should not be null");
      assertEquals("color", value.getType().getName(), "Type name should be 'color'");
    }
  }

  @Nested
  @DisplayName("Surviving Mutant Killer Tests")
  class SurvivingMutantKillerTests {

    @Test
    @DisplayName("constructor must check null discriminant - line 52")
    void constructorMustCheckNullDiscriminant() {
      // Targets line 52: discriminant == null check
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitEnum.of(enumType, null),
              "Null discriminant must throw");
      assertTrue(
          ex.getMessage().contains("null") || ex.getMessage().contains("empty"),
          "Error should mention null or empty: " + ex.getMessage());
    }

    @Test
    @DisplayName("constructor must call validate - line 56")
    void constructorMustCallValidate() {
      // Targets line 56: validate() call removal mutation
      // If validate() is removed, an invalid discriminant would be accepted
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(enumType, "yellow"),
          "Invalid discriminant must be rejected by validate()");
    }

    @Test
    @DisplayName(
        "extractDiscriminants must verify kind is not null and category is ENUM - line 106")
    void extractDiscriminantsMustVerifyTypeIsEnum() {
      // Targets line 106: enumType.getKind() == null || getCategory() != WitTypeCategory.ENUM
      final WitType primitiveType = WitType.createS32();
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(primitiveType, "test"),
          "Should reject non-enum type");

      final WitType listType = WitType.list(WitType.createS32());
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(listType, "test"),
          "Should reject list type as enum type");
    }

    @Test
    @DisplayName("validate must check discriminant against valid list - line 91")
    void validateMustCheckDiscriminantAgainstValidList() {
      // Targets line 91: !validDiscriminants.contains(discriminant)
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));

      // Valid discriminant should work
      final WitEnum valid = WitEnum.of(enumType, "red");
      assertEquals("red", valid.getDiscriminant(), "Valid discriminant should be accepted");

      // Invalid discriminant should be rejected
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitEnum.of(enumType, "purple"),
              "Invalid discriminant must be rejected");
      assertTrue(
          ex.getMessage().contains("purple"), "Error should contain the invalid discriminant name");
      assertTrue(ex.getMessage().contains("not found"), "Error should say 'not found'");
    }

    @Test
    @DisplayName("equals and hashCode work correctly")
    void equalsAndHashCodeWorkCorrectly() {
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      final WitEnum red1 = WitEnum.of(enumType, "red");
      final WitEnum red2 = WitEnum.of(enumType, "red");
      final WitEnum green = WitEnum.of(enumType, "green");

      assertEquals(red1, red2, "Same discriminants should be equal");
      assertNotEquals(red1, green, "Different discriminants should not be equal");
      assertEquals(red1.hashCode(), red2.hashCode(), "Equal enums should have same hashCode");
      assertTrue(red1.equals(red1), "Reflexive equals must return true");
      assertFalse(red1.equals(null), "equals(null) must return false");
      assertFalse(red1.equals("red"), "equals(String) must return false");
    }

    @Test
    @DisplayName("toString should contain discriminant")
    void toStringShouldContainDiscriminant() {
      final WitType enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      final WitEnum value = WitEnum.of(enumType, "green");
      assertTrue(value.toString().contains("green"), "toString should contain discriminant");
      assertTrue(value.toString().contains("WitEnum"), "toString should contain WitEnum");
    }
  }
}
