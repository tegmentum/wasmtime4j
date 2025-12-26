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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DateTime} class.
 *
 * <p>DateTime represents a point in time as seconds and nanoseconds since the Unix epoch.
 */
@DisplayName("DateTime Tests")
class DateTimeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(DateTime.class.getModifiers()), "DateTime should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(DateTime.class.getModifiers()), "DateTime should be public");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create DateTime with valid values")
    void shouldCreateDateTimeWithValidValues() {
      final DateTime dateTime = new DateTime(1704067200, 500_000_000);

      assertEquals(1704067200, dateTime.getSeconds(), "Seconds should match");
      assertEquals(500_000_000, dateTime.getNanoseconds(), "Nanoseconds should match");
    }

    @Test
    @DisplayName("should create DateTime with zero values")
    void shouldCreateDateTimeWithZeroValues() {
      final DateTime dateTime = new DateTime(0, 0);

      assertEquals(0, dateTime.getSeconds(), "Seconds should be 0");
      assertEquals(0, dateTime.getNanoseconds(), "Nanoseconds should be 0");
    }

    @Test
    @DisplayName("should create DateTime with negative seconds")
    void shouldCreateDateTimeWithNegativeSeconds() {
      final DateTime dateTime = new DateTime(-1, 500_000_000);

      assertEquals(-1, dateTime.getSeconds(), "Negative seconds should be allowed");
      assertEquals(500_000_000, dateTime.getNanoseconds(), "Nanoseconds should match");
    }

    @Test
    @DisplayName("should create DateTime with max nanoseconds")
    void shouldCreateDateTimeWithMaxNanoseconds() {
      final DateTime dateTime = new DateTime(0, 999_999_999);

      assertEquals(999_999_999, dateTime.getNanoseconds(), "Max nanoseconds should be allowed");
    }

    @Test
    @DisplayName("should throw exception for nanoseconds at boundary")
    void shouldThrowExceptionForNanosecondsAtBoundary() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DateTime(0, 1_000_000_000),
          "Should throw for nanoseconds >= 1,000,000,000");
    }

    @Test
    @DisplayName("should throw exception for negative nanoseconds")
    void shouldThrowExceptionForNegativeNanoseconds() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DateTime(0, -1),
          "Should throw for negative nanoseconds");
    }

    @Test
    @DisplayName("should throw exception for nanoseconds exceeding max")
    void shouldThrowExceptionForNanosecondsExceedingMax() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DateTime(0, 2_000_000_000),
          "Should throw for nanoseconds > 1,000,000,000");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getSeconds should return seconds")
    void getSecondsShouldReturnSeconds() {
      final DateTime dateTime = new DateTime(1704067200, 0);

      assertEquals(1704067200, dateTime.getSeconds(), "getSeconds should return seconds");
    }

    @Test
    @DisplayName("getNanoseconds should return nanoseconds")
    void getNanosecondsShouldReturnNanoseconds() {
      final DateTime dateTime = new DateTime(0, 123_456_789);

      assertEquals(
          123_456_789, dateTime.getNanoseconds(), "getNanoseconds should return nanoseconds");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for same values")
    void equalsShouldReturnTrueForSameValues() {
      final DateTime dt1 = new DateTime(1000, 500);
      final DateTime dt2 = new DateTime(1000, 500);

      assertEquals(dt1, dt2, "DateTimes with same values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different seconds")
    void equalsShouldReturnFalseForDifferentSeconds() {
      final DateTime dt1 = new DateTime(1000, 500);
      final DateTime dt2 = new DateTime(2000, 500);

      assertNotEquals(dt1, dt2, "DateTimes with different seconds should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different nanoseconds")
    void equalsShouldReturnFalseForDifferentNanoseconds() {
      final DateTime dt1 = new DateTime(1000, 500);
      final DateTime dt2 = new DateTime(1000, 600);

      assertNotEquals(dt1, dt2, "DateTimes with different nanoseconds should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final DateTime dt = new DateTime(1000, 500);

      assertEquals(dt, dt, "DateTime should equal itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final DateTime dt = new DateTime(1000, 500);

      assertFalse(dt.equals(null), "DateTime should not equal null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final DateTime dt = new DateTime(1000, 500);

      assertFalse(dt.equals("not a DateTime"), "DateTime should not equal different type");
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final DateTime dt1 = new DateTime(1000, 500);
      final DateTime dt2 = new DateTime(1000, 500);

      assertEquals(dt1.hashCode(), dt2.hashCode(), "Equal DateTimes should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be different for different values")
    void hashCodeShouldBeDifferentForDifferentValues() {
      final DateTime dt1 = new DateTime(1000, 500);
      final DateTime dt2 = new DateTime(2000, 600);

      assertNotEquals(
          dt1.hashCode(), dt2.hashCode(), "Different DateTimes should have different hashCode");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain seconds")
    void toStringShouldContainSeconds() {
      final DateTime dt = new DateTime(1704067200, 0);

      assertTrue(dt.toString().contains("1704067200"), "toString should contain seconds");
    }

    @Test
    @DisplayName("toString should contain nanoseconds")
    void toStringShouldContainNanoseconds() {
      final DateTime dt = new DateTime(0, 123_456_789);

      assertTrue(dt.toString().contains("123456789"), "toString should contain nanoseconds");
    }

    @Test
    @DisplayName("toString should have expected format")
    void toStringShouldHaveExpectedFormat() {
      final DateTime dt = new DateTime(100, 200);

      assertEquals(
          "DateTime{seconds=100, nanoseconds=200}",
          dt.toString(),
          "toString should have expected format");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle large positive seconds")
    void shouldHandleLargePositiveSeconds() {
      final DateTime dt = new DateTime(Long.MAX_VALUE, 0);

      assertEquals(Long.MAX_VALUE, dt.getSeconds(), "Should handle max long for seconds");
    }

    @Test
    @DisplayName("should handle large negative seconds")
    void shouldHandleLargeNegativeSeconds() {
      final DateTime dt = new DateTime(Long.MIN_VALUE, 0);

      assertEquals(Long.MIN_VALUE, dt.getSeconds(), "Should handle min long for seconds");
    }
  }
}
