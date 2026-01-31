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
 * Tests for {@link CdnStrategy}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("CdnStrategy Tests")
class CdnStrategyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(CdnStrategy.class.isEnum(), "CdnStrategy should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 6 values")
    void shouldHaveExactValueCount() {
      assertEquals(6, CdnStrategy.values().length,
          "CdnStrategy should have exactly 6 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain FASTEST_FIRST")
    void shouldContainFastestFirst() {
      assertNotNull(CdnStrategy.FASTEST_FIRST, "FASTEST_FIRST constant should exist");
      assertEquals("FASTEST_FIRST", CdnStrategy.FASTEST_FIRST.name(),
          "FASTEST_FIRST name should match");
    }

    @Test
    @DisplayName("should contain SEQUENTIAL")
    void shouldContainSequential() {
      assertNotNull(CdnStrategy.SEQUENTIAL, "SEQUENTIAL constant should exist");
    }

    @Test
    @DisplayName("should contain GEOGRAPHIC")
    void shouldContainGeographic() {
      assertNotNull(CdnStrategy.GEOGRAPHIC, "GEOGRAPHIC constant should exist");
    }

    @Test
    @DisplayName("should contain LOAD_BALANCED")
    void shouldContainLoadBalanced() {
      assertNotNull(CdnStrategy.LOAD_BALANCED, "LOAD_BALANCED constant should exist");
    }

    @Test
    @DisplayName("should contain HIGHEST_BANDWIDTH")
    void shouldContainHighestBandwidth() {
      assertNotNull(CdnStrategy.HIGHEST_BANDWIDTH,
          "HIGHEST_BANDWIDTH constant should exist");
    }

    @Test
    @DisplayName("should contain ADAPTIVE")
    void shouldContainAdaptive() {
      assertNotNull(CdnStrategy.ADAPTIVE, "ADAPTIVE constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final CdnStrategy value : CdnStrategy.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(CdnStrategy.values().length, ordinals.size(),
          "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final CdnStrategy[] values = CdnStrategy.values();
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
      for (final CdnStrategy value : CdnStrategy.values()) {
        assertEquals(value, CdnStrategy.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> CdnStrategy.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final CdnStrategy[] first = CdnStrategy.values();
      final CdnStrategy[] second = CdnStrategy.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final CdnStrategy value : CdnStrategy.values()) {
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
      for (final CdnStrategy strategy : CdnStrategy.values()) {
        final String result;
        switch (strategy) {
          case FASTEST_FIRST:
          case SEQUENTIAL:
          case GEOGRAPHIC:
          case LOAD_BALANCED:
          case HIGHEST_BANDWIDTH:
          case ADAPTIVE:
            result = strategy.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(strategy.name(), result, "Switch should handle " + strategy.name());
      }
    }
  }
}
