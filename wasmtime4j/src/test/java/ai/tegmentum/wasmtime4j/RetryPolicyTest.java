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

import ai.tegmentum.wasmtime4j.execution.RetryPolicy;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RetryPolicy}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("RetryPolicy Tests")
class RetryPolicyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(RetryPolicy.class.isEnum(), "RetryPolicy should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 5 values")
    void shouldHaveExactValueCount() {
      assertEquals(5, RetryPolicy.values().length, "RetryPolicy should have exactly 5 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain NO_RETRY")
    void shouldContainNoRetry() {
      assertNotNull(RetryPolicy.NO_RETRY, "NO_RETRY constant should exist");
    }

    @Test
    @DisplayName("should contain LINEAR_BACKOFF")
    void shouldContainLinearBackoff() {
      assertNotNull(RetryPolicy.LINEAR_BACKOFF, "LINEAR_BACKOFF constant should exist");
    }

    @Test
    @DisplayName("should contain EXPONENTIAL_BACKOFF")
    void shouldContainExponentialBackoff() {
      assertNotNull(RetryPolicy.EXPONENTIAL_BACKOFF, "EXPONENTIAL_BACKOFF constant should exist");
    }

    @Test
    @DisplayName("should contain EXPONENTIAL_BACKOFF_WITH_JITTER")
    void shouldContainExponentialBackoffWithJitter() {
      assertNotNull(
          RetryPolicy.EXPONENTIAL_BACKOFF_WITH_JITTER,
          "EXPONENTIAL_BACKOFF_WITH_JITTER constant should exist");
    }

    @Test
    @DisplayName("should contain CUSTOM")
    void shouldContainCustom() {
      assertNotNull(RetryPolicy.CUSTOM, "CUSTOM constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final RetryPolicy value : RetryPolicy.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(RetryPolicy.values().length, ordinals.size(), "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final RetryPolicy[] values = RetryPolicy.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final RetryPolicy value : RetryPolicy.values()) {
        assertEquals(
            value, RetryPolicy.valueOf(value.name()), "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RetryPolicy.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final RetryPolicy[] first = RetryPolicy.values();
      final RetryPolicy[] second = RetryPolicy.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final RetryPolicy value : RetryPolicy.values()) {
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
      for (final RetryPolicy policy : RetryPolicy.values()) {
        final String result;
        switch (policy) {
          case NO_RETRY:
          case LINEAR_BACKOFF:
          case EXPONENTIAL_BACKOFF:
          case EXPONENTIAL_BACKOFF_WITH_JITTER:
          case CUSTOM:
            result = policy.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(policy.name(), result, "Switch should handle " + policy.name());
      }
    }
  }
}
