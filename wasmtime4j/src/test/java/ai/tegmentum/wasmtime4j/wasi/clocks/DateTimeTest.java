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
package ai.tegmentum.wasmtime4j.wasi.clocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link DateTime} class.
 *
 * <p>Verifies construction, validation of nanoseconds, getters, equals/hashCode, and toString.
 */
@DisplayName("DateTime Tests")
class DateTimeTest {

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create with zero seconds and zero nanoseconds")
    void shouldCreateWithZeroValues() {
      final DateTime dt = new DateTime(0, 0);
      assertEquals(0L, dt.getSeconds(), "Seconds should be 0");
      assertEquals(0, dt.getNanoseconds(), "Nanoseconds should be 0");
    }

    @Test
    @DisplayName("should create with positive seconds and nanoseconds")
    void shouldCreateWithPositiveValues() {
      final DateTime dt = new DateTime(1706745600L, 500_000_000);
      assertEquals(1706745600L, dt.getSeconds(), "Seconds should match");
      assertEquals(500_000_000, dt.getNanoseconds(), "Nanoseconds should match");
    }

    @Test
    @DisplayName("should create with negative seconds (pre-epoch)")
    void shouldCreateWithNegativeSeconds() {
      final DateTime dt = new DateTime(-100L, 0);
      assertEquals(-100L, dt.getSeconds(), "Negative seconds should be accepted");
    }

    @Test
    @DisplayName("should accept maximum valid nanoseconds (999_999_999)")
    void shouldAcceptMaximumNanoseconds() {
      final DateTime dt = new DateTime(0, 999_999_999);
      assertEquals(999_999_999, dt.getNanoseconds(), "Maximum nanoseconds should be accepted");
    }

    @Test
    @DisplayName("should accept Long.MAX_VALUE seconds")
    void shouldAcceptMaxSeconds() {
      final DateTime dt = new DateTime(Long.MAX_VALUE, 0);
      assertEquals(Long.MAX_VALUE, dt.getSeconds(), "Long.MAX_VALUE seconds should be accepted");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should throw for negative nanoseconds")
    void shouldThrowForNegativeNanoseconds() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new DateTime(0, -1),
              "Should throw for negative nanoseconds");
      assertTrue(
          exception.getMessage().contains("-1"),
          "Message should contain the invalid value: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw for nanoseconds equal to 1 billion")
    void shouldThrowForOneBillionNanoseconds() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DateTime(0, 1_000_000_000),
          "Should throw for nanoseconds = 1,000,000,000");
    }

    @Test
    @DisplayName("should throw for nanoseconds exceeding 1 billion")
    void shouldThrowForExcessiveNanoseconds() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DateTime(0, Integer.MAX_VALUE),
          "Should throw for nanoseconds = Integer.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal values should be equal")
    void equalValuesShouldBeEqual() {
      final DateTime dt1 = new DateTime(100L, 500);
      final DateTime dt2 = new DateTime(100L, 500);
      assertEquals(dt1, dt2, "DateTimes with same values should be equal");
    }

    @Test
    @DisplayName("equal values should have same hashCode")
    void equalValuesShouldHaveSameHashCode() {
      final DateTime dt1 = new DateTime(100L, 500);
      final DateTime dt2 = new DateTime(100L, 500);
      assertEquals(
          dt1.hashCode(), dt2.hashCode(), "DateTimes with same values should have same hashCode");
    }

    @Test
    @DisplayName("different seconds should not be equal")
    void differentSecondsShouldNotBeEqual() {
      final DateTime dt1 = new DateTime(100L, 500);
      final DateTime dt2 = new DateTime(101L, 500);
      assertNotEquals(dt1, dt2, "DateTimes with different seconds should not be equal");
    }

    @Test
    @DisplayName("different nanoseconds should not be equal")
    void differentNanosecondsShouldNotBeEqual() {
      final DateTime dt1 = new DateTime(100L, 500);
      final DateTime dt2 = new DateTime(100L, 501);
      assertNotEquals(dt1, dt2, "DateTimes with different nanoseconds should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final DateTime dt = new DateTime(100L, 500);
      assertNotEquals(null, dt, "DateTime should not equal null");
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      final DateTime dt = new DateTime(100L, 500);
      assertNotEquals("not a datetime", dt, "DateTime should not equal a String");
    }

    @Test
    @DisplayName("should equal itself")
    void shouldEqualItself() {
      final DateTime dt = new DateTime(42L, 123);
      assertEquals(dt, dt, "DateTime should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain seconds and nanoseconds")
    void toStringShouldContainValues() {
      final DateTime dt = new DateTime(1706745600L, 500_000_000);
      final String result = dt.toString();
      assertTrue(result.contains("1706745600"), "toString should contain seconds: " + result);
      assertTrue(result.contains("500000000"), "toString should contain nanoseconds: " + result);
    }

    @Test
    @DisplayName("toString should match expected format")
    void toStringShouldMatchExpectedFormat() {
      final DateTime dt = new DateTime(10L, 20);
      assertEquals(
          "DateTime{seconds=10, nanoseconds=20}",
          dt.toString(),
          "toString should match expected format");
    }
  }
}
