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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Precompiled}.
 *
 * <p>Verifies enum structure, constants, getValue/fromValue, and round-trip conversion. Note:
 * fromValue returns null for unknown values rather than throwing.
 */
@DisplayName("Precompiled Tests")
class PrecompiledTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(Precompiled.class.isEnum(), "Precompiled should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactValueCount() {
      assertEquals(2, Precompiled.values().length, "Precompiled should have exactly 2 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain MODULE")
    void shouldContainModule() {
      assertNotNull(Precompiled.MODULE, "MODULE constant should exist");
      assertEquals("MODULE", Precompiled.MODULE.name(), "MODULE name should match");
    }

    @Test
    @DisplayName("should contain COMPONENT")
    void shouldContainComponent() {
      assertNotNull(Precompiled.COMPONENT, "COMPONENT constant should exist");
      assertEquals("COMPONENT", Precompiled.COMPONENT.name(), "COMPONENT name should match");
    }
  }

  @Nested
  @DisplayName("GetValue Tests")
  class GetValueTests {

    @Test
    @DisplayName("MODULE should have value 0")
    void moduleShouldHaveValue0() {
      assertEquals(0, Precompiled.MODULE.getValue(), "MODULE should have value 0");
    }

    @Test
    @DisplayName("COMPONENT should have value 1")
    void componentShouldHaveValue1() {
      assertEquals(1, Precompiled.COMPONENT.getValue(), "COMPONENT should have value 1");
    }

    @Test
    @DisplayName("should have unique values across all constants")
    void shouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (final Precompiled value : Precompiled.values()) {
        values.add(value.getValue());
      }
      assertEquals(Precompiled.values().length, values.size(), "All values should be unique");
    }
  }

  @Nested
  @DisplayName("FromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("should return MODULE for value 0")
    void shouldReturnModuleForValue0() {
      assertEquals(
          Precompiled.MODULE, Precompiled.fromValue(0), "fromValue(0) should return MODULE");
    }

    @Test
    @DisplayName("should return COMPONENT for value 1")
    void shouldReturnComponentForValue1() {
      assertEquals(
          Precompiled.COMPONENT, Precompiled.fromValue(1), "fromValue(1) should return COMPONENT");
    }

    @Test
    @DisplayName("should return null for negative value")
    void shouldReturnNullForNegativeValue() {
      assertNull(Precompiled.fromValue(-1), "fromValue(-1) should return null");
    }

    @Test
    @DisplayName("should return null for out-of-range value")
    void shouldReturnNullForOutOfRangeValue() {
      assertNull(Precompiled.fromValue(99), "fromValue(99) should return null");
    }

    @Test
    @DisplayName("should round-trip getValue and fromValue for all constants")
    void shouldRoundTripGetValueAndFromValue() {
      for (final Precompiled value : Precompiled.values()) {
        assertEquals(
            value,
            Precompiled.fromValue(value.getValue()),
            "Round-trip should return original for " + value.name());
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final Precompiled value : Precompiled.values()) {
        assertEquals(
            value, Precompiled.valueOf(value.name()), "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Precompiled.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support switch statement over all values")
    void shouldSupportSwitchStatement() {
      for (final Precompiled precompiled : Precompiled.values()) {
        final String result;
        switch (precompiled) {
          case MODULE:
          case COMPONENT:
            result = precompiled.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(precompiled.name(), result, "Switch should handle " + precompiled.name());
      }
    }
  }
}
