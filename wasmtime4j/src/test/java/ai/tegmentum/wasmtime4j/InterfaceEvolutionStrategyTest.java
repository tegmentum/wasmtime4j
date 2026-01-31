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

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InterfaceEvolutionStrategy}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("InterfaceEvolutionStrategy Tests")
class InterfaceEvolutionStrategyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(InterfaceEvolutionStrategy.class.isEnum(),
          "InterfaceEvolutionStrategy should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactValueCount() {
      assertEquals(3, InterfaceEvolutionStrategy.values().length,
          "InterfaceEvolutionStrategy should have exactly 3 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain BACKWARD_COMPATIBLE")
    void shouldContainBackwardCompatible() {
      assertNotNull(InterfaceEvolutionStrategy.BACKWARD_COMPATIBLE,
          "BACKWARD_COMPATIBLE constant should exist");
    }

    @Test
    @DisplayName("should contain BREAKING_CHANGE")
    void shouldContainBreakingChange() {
      assertNotNull(InterfaceEvolutionStrategy.BREAKING_CHANGE,
          "BREAKING_CHANGE constant should exist");
    }

    @Test
    @DisplayName("should contain GRADUAL_MIGRATION")
    void shouldContainGradualMigration() {
      assertNotNull(InterfaceEvolutionStrategy.GRADUAL_MIGRATION,
          "GRADUAL_MIGRATION constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final InterfaceEvolutionStrategy value :
          InterfaceEvolutionStrategy.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(InterfaceEvolutionStrategy.values().length, ordinals.size(),
          "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final InterfaceEvolutionStrategy[] values = InterfaceEvolutionStrategy.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final InterfaceEvolutionStrategy value :
          InterfaceEvolutionStrategy.values()) {
        assertEquals(value, InterfaceEvolutionStrategy.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> InterfaceEvolutionStrategy.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final InterfaceEvolutionStrategy[] first = InterfaceEvolutionStrategy.values();
      final InterfaceEvolutionStrategy[] second = InterfaceEvolutionStrategy.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final InterfaceEvolutionStrategy value :
          InterfaceEvolutionStrategy.values()) {
        assertEquals(value.name(), value.toString(),
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
      for (final InterfaceEvolutionStrategy strategy :
          InterfaceEvolutionStrategy.values()) {
        final String result;
        switch (strategy) {
          case BACKWARD_COMPATIBLE:
          case BREAKING_CHANGE:
          case GRADUAL_MIGRATION:
            result = strategy.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(strategy.name(), result,
            "Switch should handle " + strategy.name());
      }
    }
  }
}
