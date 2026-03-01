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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InstanceAllocationStrategy}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("InstanceAllocationStrategy Tests")
class InstanceAllocationStrategyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(
          InstanceAllocationStrategy.class.isEnum(),
          "InstanceAllocationStrategy should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactValueCount() {
      assertEquals(
          2,
          InstanceAllocationStrategy.values().length,
          "InstanceAllocationStrategy should have exactly 2 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain ON_DEMAND")
    void shouldContainOnDemand() {
      assertNotNull(InstanceAllocationStrategy.ON_DEMAND, "ON_DEMAND constant should exist");
      assertEquals(
          "ON_DEMAND", InstanceAllocationStrategy.ON_DEMAND.name(), "ON_DEMAND name should match");
    }

    @Test
    @DisplayName("should contain POOLING")
    void shouldContainPooling() {
      assertNotNull(InstanceAllocationStrategy.POOLING, "POOLING constant should exist");
      assertEquals(
          "POOLING", InstanceAllocationStrategy.POOLING.name(), "POOLING name should match");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final InstanceAllocationStrategy value : InstanceAllocationStrategy.values()) {
        assertEquals(
            value,
            InstanceAllocationStrategy.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> InstanceAllocationStrategy.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final InstanceAllocationStrategy[] first = InstanceAllocationStrategy.values();
      final InstanceAllocationStrategy[] second = InstanceAllocationStrategy.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final InstanceAllocationStrategy value : InstanceAllocationStrategy.values()) {
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
      for (final InstanceAllocationStrategy strategy : InstanceAllocationStrategy.values()) {
        final String result;
        switch (strategy) {
          case ON_DEMAND:
          case POOLING:
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
