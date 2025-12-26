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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TimezoneDisplay} class.
 *
 * <p>TimezoneDisplay contains timezone information for a specific point in time, including UTC
 * offset, timezone name abbreviation, and daylight saving time status.
 */
@DisplayName("TimezoneDisplay Tests")
class TimezoneDisplayTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(TimezoneDisplay.class.getModifiers()),
          "TimezoneDisplay should be final");
    }

    @Test
    @DisplayName("should not be an interface")
    void shouldNotBeAnInterface() {
      assertFalse(
          TimezoneDisplay.class.isInterface(),
          "TimezoneDisplay should be a class, not an interface");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final TimezoneDisplay display = new TimezoneDisplay(-28800, "PST", false);
      assertNotNull(display, "Should create instance");
      assertEquals(-28800, display.getUtcOffsetSeconds(), "Should have correct offset");
      assertEquals("PST", display.getName(), "Should have correct name");
      assertFalse(display.isInDaylightSavingTime(), "Should not be in DST");
    }

    @Test
    @DisplayName("should create instance with UTC")
    void shouldCreateInstanceWithUtc() {
      final TimezoneDisplay display = new TimezoneDisplay(0, "UTC", false);
      assertEquals(0, display.getUtcOffsetSeconds(), "UTC has zero offset");
      assertEquals("UTC", display.getName(), "Should be UTC");
      assertFalse(display.isInDaylightSavingTime(), "UTC has no DST");
    }

    @Test
    @DisplayName("should create instance with daylight saving time active")
    void shouldCreateInstanceWithDaylightSavingTimeActive() {
      final TimezoneDisplay display = new TimezoneDisplay(-25200, "PDT", true);
      assertEquals(-25200, display.getUtcOffsetSeconds(), "Should have PDT offset");
      assertEquals("PDT", display.getName(), "Should be PDT");
      assertTrue(display.isInDaylightSavingTime(), "Should be in DST");
    }

    @Test
    @DisplayName("should create instance with positive offset")
    void shouldCreateInstanceWithPositiveOffset() {
      // Tokyo is UTC+9 = 32400 seconds
      final TimezoneDisplay display = new TimezoneDisplay(32400, "JST", false);
      assertEquals(32400, display.getUtcOffsetSeconds(), "Should have positive offset");
      assertEquals("JST", display.getName(), "Should be JST");
    }

    @Test
    @DisplayName("should throw for null name")
    void shouldThrowForNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(0, null, false),
          "Should throw for null name");
    }

    @Test
    @DisplayName("should throw for offset >= 86400")
    void shouldThrowForOffsetGreaterThanOrEqualTo86400() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(86400, "UTC", false),
          "Should throw for offset >= 86400");
    }

    @Test
    @DisplayName("should throw for offset <= -86400")
    void shouldThrowForOffsetLessThanOrEqualToNegative86400() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(-86400, "UTC", false),
          "Should throw for offset <= -86400");
    }

    @Test
    @DisplayName("should accept offset just below limit")
    void shouldAcceptOffsetJustBelowLimit() {
      final TimezoneDisplay positive = new TimezoneDisplay(86399, "MAX+", false);
      assertEquals(86399, positive.getUtcOffsetSeconds(), "Should accept 86399");

      final TimezoneDisplay negative = new TimezoneDisplay(-86399, "MAX-", false);
      assertEquals(-86399, negative.getUtcOffsetSeconds(), "Should accept -86399");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getUtcOffsetSeconds should return offset")
    void getUtcOffsetSecondsShouldReturnOffset() {
      final TimezoneDisplay display = new TimezoneDisplay(-18000, "EST", false);
      assertEquals(-18000, display.getUtcOffsetSeconds(), "Should return correct offset");
    }

    @Test
    @DisplayName("getName should return timezone abbreviation")
    void getNameShouldReturnTimezoneAbbreviation() {
      final TimezoneDisplay display = new TimezoneDisplay(0, "UTC", false);
      assertEquals("UTC", display.getName(), "Should return correct name");
    }

    @Test
    @DisplayName("isInDaylightSavingTime should return DST status")
    void isInDaylightSavingTimeShouldReturnDstStatus() {
      final TimezoneDisplay noDst = new TimezoneDisplay(0, "UTC", false);
      assertFalse(noDst.isInDaylightSavingTime(), "Should be false when not in DST");

      final TimezoneDisplay withDst = new TimezoneDisplay(-25200, "PDT", true);
      assertTrue(withDst.isInDaylightSavingTime(), "Should be true when in DST");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final TimezoneDisplay display = new TimezoneDisplay(0, "UTC", false);
      assertEquals(display, display, "Should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to equivalent instance")
    void shouldBeEqualToEquivalentInstance() {
      final TimezoneDisplay display1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay display2 = new TimezoneDisplay(-28800, "PST", false);
      assertEquals(display1, display2, "Should be equal to equivalent instance");
    }

    @Test
    @DisplayName("should not be equal with different offset")
    void shouldNotBeEqualWithDifferentOffset() {
      final TimezoneDisplay display1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay display2 = new TimezoneDisplay(-25200, "PST", false);
      assertNotEquals(display1, display2, "Should not be equal with different offset");
    }

    @Test
    @DisplayName("should not be equal with different name")
    void shouldNotBeEqualWithDifferentName() {
      final TimezoneDisplay display1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay display2 = new TimezoneDisplay(-28800, "CST", false);
      assertNotEquals(display1, display2, "Should not be equal with different name");
    }

    @Test
    @DisplayName("should not be equal with different DST status")
    void shouldNotBeEqualWithDifferentDstStatus() {
      final TimezoneDisplay display1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay display2 = new TimezoneDisplay(-28800, "PST", true);
      assertNotEquals(display1, display2, "Should not be equal with different DST status");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final TimezoneDisplay display = new TimezoneDisplay(0, "UTC", false);
      assertNotEquals(null, display, "Should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final TimezoneDisplay display = new TimezoneDisplay(0, "UTC", false);
      assertNotEquals("UTC", display, "Should not be equal to String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should have consistent hashCode")
    void shouldHaveConsistentHashCode() {
      final TimezoneDisplay display = new TimezoneDisplay(0, "UTC", false);
      final int hash1 = display.hashCode();
      final int hash2 = display.hashCode();
      assertEquals(hash1, hash2, "HashCode should be consistent");
    }

    @Test
    @DisplayName("equal instances should have same hashCode")
    void equalInstancesShouldHaveSameHashCode() {
      final TimezoneDisplay display1 = new TimezoneDisplay(-28800, "PST", false);
      final TimezoneDisplay display2 = new TimezoneDisplay(-28800, "PST", false);
      assertEquals(
          display1.hashCode(), display2.hashCode(), "Equal instances should have same hashCode");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain offset")
    void toStringShouldContainOffset() {
      final TimezoneDisplay display = new TimezoneDisplay(-28800, "PST", false);
      assertTrue(
          display.toString().contains("-28800"),
          "toString should contain offset: " + display.toString());
    }

    @Test
    @DisplayName("toString should contain name")
    void toStringShouldContainName() {
      final TimezoneDisplay display = new TimezoneDisplay(-28800, "PST", false);
      assertTrue(
          display.toString().contains("PST"),
          "toString should contain name: " + display.toString());
    }

    @Test
    @DisplayName("toString should contain DST status")
    void toStringShouldContainDstStatus() {
      final TimezoneDisplay display = new TimezoneDisplay(-28800, "PST", false);
      assertTrue(
          display.toString().contains("false"),
          "toString should contain DST status: " + display.toString());
    }
  }

  @Nested
  @DisplayName("Common Timezone Tests")
  class CommonTimezoneTests {

    @Test
    @DisplayName("should represent UTC correctly")
    void shouldRepresentUtcCorrectly() {
      final TimezoneDisplay utc = new TimezoneDisplay(0, "UTC", false);
      assertEquals(0, utc.getUtcOffsetSeconds(), "UTC offset is 0");
      assertEquals("UTC", utc.getName(), "Name is UTC");
      assertFalse(utc.isInDaylightSavingTime(), "UTC has no DST");
    }

    @Test
    @DisplayName("should represent PST correctly")
    void shouldRepresentPstCorrectly() {
      // PST is UTC-8 = -28800 seconds
      final TimezoneDisplay pst = new TimezoneDisplay(-28800, "PST", false);
      assertEquals(-28800, pst.getUtcOffsetSeconds(), "PST is UTC-8");
      assertEquals("PST", pst.getName(), "Name is PST");
      assertFalse(pst.isInDaylightSavingTime(), "PST is standard time");
    }

    @Test
    @DisplayName("should represent PDT correctly")
    void shouldRepresentPdtCorrectly() {
      // PDT is UTC-7 = -25200 seconds
      final TimezoneDisplay pdt = new TimezoneDisplay(-25200, "PDT", true);
      assertEquals(-25200, pdt.getUtcOffsetSeconds(), "PDT is UTC-7");
      assertEquals("PDT", pdt.getName(), "Name is PDT");
      assertTrue(pdt.isInDaylightSavingTime(), "PDT is daylight saving time");
    }

    @Test
    @DisplayName("should represent JST correctly")
    void shouldRepresentJstCorrectly() {
      // JST is UTC+9 = 32400 seconds
      final TimezoneDisplay jst = new TimezoneDisplay(32400, "JST", false);
      assertEquals(32400, jst.getUtcOffsetSeconds(), "JST is UTC+9");
      assertEquals("JST", jst.getName(), "Name is JST");
      assertFalse(jst.isInDaylightSavingTime(), "Japan does not observe DST");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should match WASI timezone-display record")
    void shouldMatchWasiTimezoneDisplayRecord() {
      // WASI Preview 2 wasi:clocks/timezone@0.2.8 specifies:
      // record timezone-display {
      //   utc-offset: s32,
      //   name: string,
      //   in-daylight-saving-time: bool
      // }
      final TimezoneDisplay display = new TimezoneDisplay(0, "UTC", false);
      assertNotNull(display.getUtcOffsetSeconds(), "Should have utc-offset (s32)");
      assertNotNull(display.getName(), "Should have name (string)");
      // isInDaylightSavingTime returns primitive boolean, always non-null
    }

    @Test
    @DisplayName("offset should be less than 86400")
    void offsetShouldBeLessThan86400() {
      // WASI specifies: utc-offset must be less than 86,400 (seconds in a day)
      assertThrows(
          IllegalArgumentException.class,
          () -> new TimezoneDisplay(86400, "TEST", false),
          "Should reject offset >= 86400");
    }
  }
}
