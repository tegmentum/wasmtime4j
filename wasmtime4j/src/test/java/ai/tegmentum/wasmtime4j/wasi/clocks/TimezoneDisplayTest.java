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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link TimezoneDisplay} class.
 *
 * <p>Verifies construction, validation of offset and name, getters, equals/hashCode, and toString.
 */
@DisplayName("TimezoneDisplay Tests")
class TimezoneDisplayTest {

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create UTC timezone display")
    void shouldCreateUtcTimezoneDisplay() {
      final TimezoneDisplay tz = new TimezoneDisplay(0, "UTC", false);
      assertEquals(0, tz.getUtcOffsetSeconds(), "UTC offset should be 0");
      assertEquals("UTC", tz.getName(), "Name should be UTC");
      assertFalse(tz.isInDaylightSavingTime(), "DST should be false for UTC");
    }

    @Test
    @DisplayName("should create positive offset timezone (east of UTC)")
    void shouldCreatePositiveOffset() {
      final TimezoneDisplay tz = new TimezoneDisplay(32400, "JST", false);
      assertEquals(32400, tz.getUtcOffsetSeconds(), "JST offset should be 32400 (9 hours)");
      assertEquals("JST", tz.getName(), "Name should be JST");
    }

    @Test
    @DisplayName("should create negative offset timezone (west of UTC)")
    void shouldCreateNegativeOffset() {
      final TimezoneDisplay tz = new TimezoneDisplay(-28800, "PST", false);
      assertEquals(-28800, tz.getUtcOffsetSeconds(), "PST offset should be -28800 (-8 hours)");
      assertEquals("PST", tz.getName(), "Name should be PST");
    }

    @Test
    @DisplayName("should create timezone with DST active")
    void shouldCreateWithDstActive() {
      final TimezoneDisplay tz = new TimezoneDisplay(-25200, "PDT", true);
      assertEquals(-25200, tz.getUtcOffsetSeconds(), "PDT offset should be -25200 (-7 hours)");
      assertTrue(tz.isInDaylightSavingTime(), "DST should be true for PDT");
    }

    @Test
    @DisplayName("should accept maximum valid positive offset (86399)")
    void shouldAcceptMaxPositiveOffset() {
      final TimezoneDisplay tz = new TimezoneDisplay(86399, "MAX", false);
      assertEquals(86399, tz.getUtcOffsetSeconds(), "Maximum positive offset should be accepted");
    }

    @Test
    @DisplayName("should accept maximum valid negative offset (-86399)")
    void shouldAcceptMaxNegativeOffset() {
      final TimezoneDisplay tz = new TimezoneDisplay(-86399, "MIN", false);
      assertEquals(-86399, tz.getUtcOffsetSeconds(), "Maximum negative offset should be accepted");
    }

    @Test
    @DisplayName("should accept empty name string")
    void shouldAcceptEmptyName() {
      final TimezoneDisplay tz = new TimezoneDisplay(0, "", false);
      assertEquals("", tz.getName(), "Empty name should be accepted");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should throw for offset equal to 86400")
    void shouldThrowForOffset86400() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(86400, "BAD", false),
          "Should throw for offset = 86400");
    }

    @Test
    @DisplayName("should throw for offset equal to -86400")
    void shouldThrowForNegativeOffset86400() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(-86400, "BAD", false),
          "Should throw for offset = -86400");
    }

    @Test
    @DisplayName("should throw for excessively large offset")
    void shouldThrowForExcessiveOffset() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(100000, "BAD", false),
          "Should throw for offset = 100000");
    }

    @Test
    @DisplayName("should throw for null name")
    void shouldThrowForNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(0, null, false),
          "Should throw for null name");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal timezones should be equal")
    void equalTimezonesShouldBeEqual() {
      final TimezoneDisplay tz1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay tz2 = new TimezoneDisplay(-28800, "PST", false);
      assertEquals(tz1, tz2, "TimezoneDisplays with same values should be equal");
    }

    @Test
    @DisplayName("equal timezones should have same hashCode")
    void equalTimezonesShouldHaveSameHashCode() {
      final TimezoneDisplay tz1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay tz2 = new TimezoneDisplay(-28800, "PST", false);
      assertEquals(
          tz1.hashCode(),
          tz2.hashCode(),
          "TimezoneDisplays with same values should have same hashCode");
    }

    @Test
    @DisplayName("different offsets should not be equal")
    void differentOffsetsShouldNotBeEqual() {
      final TimezoneDisplay tz1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay tz2 = new TimezoneDisplay(-25200, "PST", false);
      assertNotEquals(tz1, tz2, "TimezoneDisplays with different offsets should not be equal");
    }

    @Test
    @DisplayName("different names should not be equal")
    void differentNamesShouldNotBeEqual() {
      final TimezoneDisplay tz1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay tz2 = new TimezoneDisplay(-28800, "PDT", false);
      assertNotEquals(tz1, tz2, "TimezoneDisplays with different names should not be equal");
    }

    @Test
    @DisplayName("different DST should not be equal")
    void differentDstShouldNotBeEqual() {
      final TimezoneDisplay tz1 = new TimezoneDisplay(3600, "CET", false);
      final TimezoneDisplay tz2 = new TimezoneDisplay(3600, "CET", true);
      assertNotEquals(tz1, tz2, "TimezoneDisplays with different DST should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final TimezoneDisplay tz = new TimezoneDisplay(0, "UTC", false);
      assertNotEquals(null, tz, "TimezoneDisplay should not equal null");
    }

    @Test
    @DisplayName("should equal itself")
    void shouldEqualItself() {
      final TimezoneDisplay tz = new TimezoneDisplay(0, "UTC", false);
      assertEquals(tz, tz, "TimezoneDisplay should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain offset, name, and DST status")
    void toStringShouldContainAllFields() {
      final TimezoneDisplay tz = new TimezoneDisplay(-28800, "PST", false);
      final String result = tz.toString();
      assertTrue(result.contains("-28800"), "toString should contain offset: " + result);
      assertTrue(result.contains("PST"), "toString should contain name: " + result);
      assertTrue(result.contains("false"), "toString should contain DST status: " + result);
    }

    @Test
    @DisplayName("toString should contain class name")
    void toStringShouldContainClassName() {
      final TimezoneDisplay tz = new TimezoneDisplay(0, "UTC", false);
      final String result = tz.toString();
      assertTrue(
          result.contains("TimezoneDisplay"), "toString should contain class name: " + result);
    }
  }
}
