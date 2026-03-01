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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitTypeCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitTypeCategory}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("WitTypeCategory Tests")
class WitTypeCategoryTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(WitTypeCategory.class.isEnum(), "WitTypeCategory should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 10 values")
    void shouldHaveExactValueCount() {
      assertEquals(
          10, WitTypeCategory.values().length, "WitTypeCategory should have exactly 10 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain all expected constants")
    void shouldContainAllExpectedConstants() {
      assertNotNull(WitTypeCategory.PRIMITIVE, "PRIMITIVE constant should exist");
      assertNotNull(WitTypeCategory.RECORD, "RECORD constant should exist");
      assertNotNull(WitTypeCategory.VARIANT, "VARIANT constant should exist");
      assertNotNull(WitTypeCategory.ENUM, "ENUM constant should exist");
      assertNotNull(WitTypeCategory.FLAGS, "FLAGS constant should exist");
      assertNotNull(WitTypeCategory.LIST, "LIST constant should exist");
      assertNotNull(WitTypeCategory.OPTION, "OPTION constant should exist");
      assertNotNull(WitTypeCategory.RESULT, "RESULT constant should exist");
      assertNotNull(WitTypeCategory.TUPLE, "TUPLE constant should exist");
      assertNotNull(WitTypeCategory.RESOURCE, "RESOURCE constant should exist");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final WitTypeCategory value : WitTypeCategory.values()) {
        assertEquals(
            value, WitTypeCategory.valueOf(value.name()), "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTypeCategory.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final WitTypeCategory[] first = WitTypeCategory.values();
      final WitTypeCategory[] second = WitTypeCategory.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final WitTypeCategory value : WitTypeCategory.values()) {
        assertEquals(
            value.name(),
            value.toString(),
            "toString should return the enum name for " + value.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support switch statement over all values")
    void shouldSupportSwitchStatement() {
      for (final WitTypeCategory category : WitTypeCategory.values()) {
        final String result;
        switch (category) {
          case PRIMITIVE:
          case RECORD:
          case VARIANT:
          case ENUM:
          case FLAGS:
          case LIST:
          case OPTION:
          case RESULT:
          case TUPLE:
          case RESOURCE:
            result = category.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(category.name(), result, "Switch should handle " + category.name());
      }
    }
  }
}
