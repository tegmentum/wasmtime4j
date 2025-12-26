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
 * Tests for the {@link RetryPolicy} enum.
 *
 * <p>This test class verifies the RetryPolicy enum which defines different strategies for retrying
 * failed HTTP requests during network streaming operations.
 */
@DisplayName("RetryPolicy Tests")
class RetryPolicyTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("Should have NO_RETRY value")
    void shouldHaveNoRetryValue() {
      assertNotNull(RetryPolicy.valueOf("NO_RETRY"), "Should have NO_RETRY value");
    }

    @Test
    @DisplayName("Should have LINEAR_BACKOFF value")
    void shouldHaveLinearBackoffValue() {
      assertNotNull(RetryPolicy.valueOf("LINEAR_BACKOFF"), "Should have LINEAR_BACKOFF value");
    }

    @Test
    @DisplayName("Should have EXPONENTIAL_BACKOFF value")
    void shouldHaveExponentialBackoffValue() {
      assertNotNull(
          RetryPolicy.valueOf("EXPONENTIAL_BACKOFF"), "Should have EXPONENTIAL_BACKOFF value");
    }

    @Test
    @DisplayName("Should have EXPONENTIAL_BACKOFF_WITH_JITTER value")
    void shouldHaveExponentialBackoffWithJitterValue() {
      assertNotNull(
          RetryPolicy.valueOf("EXPONENTIAL_BACKOFF_WITH_JITTER"),
          "Should have EXPONENTIAL_BACKOFF_WITH_JITTER value");
    }

    @Test
    @DisplayName("Should have CUSTOM value")
    void shouldHaveCustomValue() {
      assertNotNull(RetryPolicy.valueOf("CUSTOM"), "Should have CUSTOM value");
    }

    @Test
    @DisplayName("Should have exactly 5 values")
    void shouldHaveExactly5Values() {
      assertEquals(5, RetryPolicy.values().length, "Should have exactly 5 retry policies");
    }

    @Test
    @DisplayName("NO_RETRY should be at ordinal 0")
    void noRetryShouldBeAtOrdinal0() {
      assertEquals(0, RetryPolicy.NO_RETRY.ordinal(), "NO_RETRY should be at ordinal 0");
    }

    @Test
    @DisplayName("LINEAR_BACKOFF should be at ordinal 1")
    void linearBackoffShouldBeAtOrdinal1() {
      assertEquals(
          1, RetryPolicy.LINEAR_BACKOFF.ordinal(), "LINEAR_BACKOFF should be at ordinal 1");
    }

    @Test
    @DisplayName("EXPONENTIAL_BACKOFF should be at ordinal 2")
    void exponentialBackoffShouldBeAtOrdinal2() {
      assertEquals(
          2,
          RetryPolicy.EXPONENTIAL_BACKOFF.ordinal(),
          "EXPONENTIAL_BACKOFF should be at ordinal 2");
    }

    @Test
    @DisplayName("EXPONENTIAL_BACKOFF_WITH_JITTER should be at ordinal 3")
    void exponentialBackoffWithJitterShouldBeAtOrdinal3() {
      assertEquals(
          3,
          RetryPolicy.EXPONENTIAL_BACKOFF_WITH_JITTER.ordinal(),
          "EXPONENTIAL_BACKOFF_WITH_JITTER should be at ordinal 3");
    }

    @Test
    @DisplayName("CUSTOM should be at ordinal 4")
    void customShouldBeAtOrdinal4() {
      assertEquals(4, RetryPolicy.CUSTOM.ordinal(), "CUSTOM should be at ordinal 4");
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return NO_RETRY for 'NO_RETRY'")
    void valueOfShouldReturnNoRetryForNoRetry() {
      assertEquals(
          RetryPolicy.NO_RETRY,
          RetryPolicy.valueOf("NO_RETRY"),
          "valueOf('NO_RETRY') should return NO_RETRY");
    }

    @Test
    @DisplayName("valueOf should return LINEAR_BACKOFF for 'LINEAR_BACKOFF'")
    void valueOfShouldReturnLinearBackoffForLinearBackoff() {
      assertEquals(
          RetryPolicy.LINEAR_BACKOFF,
          RetryPolicy.valueOf("LINEAR_BACKOFF"),
          "valueOf('LINEAR_BACKOFF') should return LINEAR_BACKOFF");
    }

    @Test
    @DisplayName("valueOf should return EXPONENTIAL_BACKOFF for 'EXPONENTIAL_BACKOFF'")
    void valueOfShouldReturnExponentialBackoffForExponentialBackoff() {
      assertEquals(
          RetryPolicy.EXPONENTIAL_BACKOFF,
          RetryPolicy.valueOf("EXPONENTIAL_BACKOFF"),
          "valueOf('EXPONENTIAL_BACKOFF') should return EXPONENTIAL_BACKOFF");
    }

    @Test
    @DisplayName("valueOf should return EXPONENTIAL_BACKOFF_WITH_JITTER")
    void valueOfShouldReturnExponentialBackoffWithJitter() {
      assertEquals(
          RetryPolicy.EXPONENTIAL_BACKOFF_WITH_JITTER,
          RetryPolicy.valueOf("EXPONENTIAL_BACKOFF_WITH_JITTER"),
          "valueOf('EXPONENTIAL_BACKOFF_WITH_JITTER') should return the correct value");
    }

    @Test
    @DisplayName("valueOf should return CUSTOM for 'CUSTOM'")
    void valueOfShouldReturnCustomForCustom() {
      assertEquals(
          RetryPolicy.CUSTOM,
          RetryPolicy.valueOf("CUSTOM"),
          "valueOf('CUSTOM') should return CUSTOM");
    }

    @Test
    @DisplayName("valueOf should throw for invalid value")
    void valueOfShouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RetryPolicy.valueOf("INVALID"),
          "valueOf should throw for invalid value");
    }

    @Test
    @DisplayName("valueOf should throw for lowercase value")
    void valueOfShouldThrowForLowercaseValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RetryPolicy.valueOf("no_retry"),
          "valueOf should throw for lowercase value");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null")
    void valueOfShouldThrowNpeForNull() {
      assertThrows(
          NullPointerException.class,
          () -> RetryPolicy.valueOf(null),
          "valueOf should throw NPE for null");
    }
  }

  @Nested
  @DisplayName("name Tests")
  class NameTests {

    @Test
    @DisplayName("NO_RETRY name should be 'NO_RETRY'")
    void noRetryNameShouldBeNoRetry() {
      assertEquals("NO_RETRY", RetryPolicy.NO_RETRY.name(), "NO_RETRY name should be 'NO_RETRY'");
    }

    @Test
    @DisplayName("LINEAR_BACKOFF name should be 'LINEAR_BACKOFF'")
    void linearBackoffNameShouldBeLinearBackoff() {
      assertEquals(
          "LINEAR_BACKOFF",
          RetryPolicy.LINEAR_BACKOFF.name(),
          "LINEAR_BACKOFF name should be 'LINEAR_BACKOFF'");
    }

    @Test
    @DisplayName("EXPONENTIAL_BACKOFF name should be 'EXPONENTIAL_BACKOFF'")
    void exponentialBackoffNameShouldBeExponentialBackoff() {
      assertEquals(
          "EXPONENTIAL_BACKOFF",
          RetryPolicy.EXPONENTIAL_BACKOFF.name(),
          "EXPONENTIAL_BACKOFF name should be 'EXPONENTIAL_BACKOFF'");
    }

    @Test
    @DisplayName("EXPONENTIAL_BACKOFF_WITH_JITTER name should match")
    void exponentialBackoffWithJitterNameShouldMatch() {
      assertEquals(
          "EXPONENTIAL_BACKOFF_WITH_JITTER",
          RetryPolicy.EXPONENTIAL_BACKOFF_WITH_JITTER.name(),
          "EXPONENTIAL_BACKOFF_WITH_JITTER name should match");
    }

    @Test
    @DisplayName("CUSTOM name should be 'CUSTOM'")
    void customNameShouldBeCustom() {
      assertEquals("CUSTOM", RetryPolicy.CUSTOM.name(), "CUSTOM name should be 'CUSTOM'");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("NO_RETRY toString should return 'NO_RETRY'")
    void noRetryToStringShouldReturnNoRetry() {
      assertEquals(
          "NO_RETRY",
          RetryPolicy.NO_RETRY.toString(),
          "NO_RETRY toString should return 'NO_RETRY'");
    }

    @Test
    @DisplayName("EXPONENTIAL_BACKOFF toString should return 'EXPONENTIAL_BACKOFF'")
    void exponentialBackoffToStringShouldReturnExponentialBackoff() {
      assertEquals(
          "EXPONENTIAL_BACKOFF",
          RetryPolicy.EXPONENTIAL_BACKOFF.toString(),
          "EXPONENTIAL_BACKOFF toString should return 'EXPONENTIAL_BACKOFF'");
    }

    @Test
    @DisplayName("CUSTOM toString should return 'CUSTOM'")
    void customToStringShouldReturnCustom() {
      assertEquals(
          "CUSTOM", RetryPolicy.CUSTOM.toString(), "CUSTOM toString should return 'CUSTOM'");
    }
  }

  @Nested
  @DisplayName("values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array with all policies")
    void valuesShouldReturnArrayWithAllPolicies() {
      final RetryPolicy[] values = RetryPolicy.values();

      assertEquals(5, values.length, "Should have 5 values");
      assertEquals(RetryPolicy.NO_RETRY, values[0], "First value should be NO_RETRY");
      assertEquals(RetryPolicy.LINEAR_BACKOFF, values[1], "Second value should be LINEAR_BACKOFF");
      assertEquals(
          RetryPolicy.EXPONENTIAL_BACKOFF, values[2], "Third value should be EXPONENTIAL_BACKOFF");
      assertEquals(
          RetryPolicy.EXPONENTIAL_BACKOFF_WITH_JITTER,
          values[3],
          "Fourth value should be EXPONENTIAL_BACKOFF_WITH_JITTER");
      assertEquals(RetryPolicy.CUSTOM, values[4], "Fifth value should be CUSTOM");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final RetryPolicy[] values1 = RetryPolicy.values();
      final RetryPolicy[] values2 = RetryPolicy.values();

      assertTrue(values1 != values2, "values() should return new array each call");
      assertEquals(values1.length, values2.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final RetryPolicy policy = RetryPolicy.EXPONENTIAL_BACKOFF;
      String result;

      switch (policy) {
        case NO_RETRY:
          result = "No retry";
          break;
        case LINEAR_BACKOFF:
          result = "Linear backoff";
          break;
        case EXPONENTIAL_BACKOFF:
          result = "Exponential backoff";
          break;
        case EXPONENTIAL_BACKOFF_WITH_JITTER:
          result = "Exponential with jitter";
          break;
        case CUSTOM:
          result = "Custom policy";
          break;
        default:
          result = "Unknown";
      }

      assertEquals("Exponential backoff", result, "Switch should work with EXPONENTIAL_BACKOFF");
    }

    @Test
    @DisplayName("Should be comparable using equals")
    void shouldBeComparableUsingEquals() {
      final RetryPolicy policy1 = RetryPolicy.LINEAR_BACKOFF;
      final RetryPolicy policy2 = RetryPolicy.LINEAR_BACKOFF;
      final RetryPolicy policy3 = RetryPolicy.CUSTOM;

      assertEquals(policy1, policy2, "Same enum values should be equal");
      assertTrue(!policy1.equals(policy3), "Different enum values should not be equal");
    }

    @Test
    @DisplayName("Should be usable with compareTo")
    void shouldBeUsableWithCompareTo() {
      assertTrue(
          RetryPolicy.NO_RETRY.compareTo(RetryPolicy.CUSTOM) < 0,
          "NO_RETRY should be less than CUSTOM by ordinal");
      assertTrue(
          RetryPolicy.CUSTOM.compareTo(RetryPolicy.NO_RETRY) > 0,
          "CUSTOM should be greater than NO_RETRY by ordinal");
      assertEquals(
          0,
          RetryPolicy.LINEAR_BACKOFF.compareTo(RetryPolicy.LINEAR_BACKOFF),
          "Same value should compare equal");
    }

    @Test
    @DisplayName("Should work for network retry configuration scenarios")
    void shouldWorkForNetworkRetryConfigurationScenarios() {
      // Simulating configuration based on application type
      final RetryPolicy timeCritical = RetryPolicy.NO_RETRY;
      final RetryPolicy simple = RetryPolicy.LINEAR_BACKOFF;
      final RetryPolicy recommended = RetryPolicy.EXPONENTIAL_BACKOFF;
      final RetryPolicy distributed = RetryPolicy.EXPONENTIAL_BACKOFF_WITH_JITTER;
      final RetryPolicy custom = RetryPolicy.CUSTOM;

      assertEquals(RetryPolicy.NO_RETRY, timeCritical, "Time-critical apps should use NO_RETRY");
      assertEquals(RetryPolicy.LINEAR_BACKOFF, simple, "Simple apps can use LINEAR_BACKOFF");
      assertEquals(
          RetryPolicy.EXPONENTIAL_BACKOFF,
          recommended,
          "Recommended policy is EXPONENTIAL_BACKOFF");
      assertEquals(
          RetryPolicy.EXPONENTIAL_BACKOFF_WITH_JITTER,
          distributed,
          "Distributed systems should use jitter to avoid thundering herd");
      assertEquals(RetryPolicy.CUSTOM, custom, "Custom allows user-defined retry behavior");
    }
  }
}
