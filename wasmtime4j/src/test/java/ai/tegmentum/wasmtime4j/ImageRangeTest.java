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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ImageRange} value object.
 *
 * <p>Verifies construction, getters, size calculation, equals/hashCode, and toString.
 */
@DisplayName("ImageRange Tests")
class ImageRangeTest {

  @Nested
  @DisplayName("Construction and Getter Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should store start and end addresses")
    void shouldStoreStartAndEnd() {
      final ImageRange range = new ImageRange(0x1000L, 0x2000L);

      assertEquals(0x1000L, range.getStart(), "Start address should be 0x1000");
      assertEquals(0x2000L, range.getEnd(), "End address should be 0x2000");
    }

    @Test
    @DisplayName("should handle zero addresses")
    void shouldHandleZeroAddresses() {
      final ImageRange range = new ImageRange(0L, 0L);

      assertEquals(0L, range.getStart(), "Start should be 0");
      assertEquals(0L, range.getEnd(), "End should be 0");
    }

    @Test
    @DisplayName("should handle large addresses")
    void shouldHandleLargeAddresses() {
      final ImageRange range = new ImageRange(Long.MAX_VALUE - 100, Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE - 100, range.getStart(), "Start should handle large values");
      assertEquals(Long.MAX_VALUE, range.getEnd(), "End should handle large values");
    }
  }

  @Nested
  @DisplayName("Size Calculation Tests")
  class SizeTests {

    @Test
    @DisplayName("should calculate size as end minus start")
    void shouldCalculateSize() {
      final ImageRange range = new ImageRange(0x1000L, 0x2000L);

      assertEquals(
          0x1000L, range.getSize(), "Size should be end - start = 0x1000, got: " + range.getSize());
    }

    @Test
    @DisplayName("should return zero for zero-size range")
    void shouldReturnZeroForZeroSizeRange() {
      final ImageRange range = new ImageRange(0x5000L, 0x5000L);

      assertEquals(0L, range.getSize(), "Zero-size range should have size 0");
    }

    @Test
    @DisplayName("should handle large size correctly")
    void shouldHandleLargeSize() {
      final ImageRange range = new ImageRange(0L, 0xFFFFFFFFL);

      assertEquals(
          0xFFFFFFFFL, range.getSize(), "Should handle large size, got: " + range.getSize());
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final ImageRange range = new ImageRange(100L, 200L);

      assertEquals(range, range, "Range should equal itself");
    }

    @Test
    @DisplayName("should be equal to range with same start and end")
    void shouldBeEqualToSameRange() {
      final ImageRange range1 = new ImageRange(100L, 200L);
      final ImageRange range2 = new ImageRange(100L, 200L);

      assertEquals(range1, range2, "Ranges with same start and end should be equal");
      assertEquals(range1.hashCode(), range2.hashCode(), "Equal ranges should have same hash code");
    }

    @Test
    @DisplayName("should not be equal to range with different start")
    void shouldNotBeEqualWithDifferentStart() {
      final ImageRange range1 = new ImageRange(100L, 200L);
      final ImageRange range2 = new ImageRange(101L, 200L);

      assertNotEquals(range1, range2, "Ranges with different start should not be equal");
    }

    @Test
    @DisplayName("should not be equal to range with different end")
    void shouldNotBeEqualWithDifferentEnd() {
      final ImageRange range1 = new ImageRange(100L, 200L);
      final ImageRange range2 = new ImageRange(100L, 201L);

      assertNotEquals(range1, range2, "Ranges with different end should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final ImageRange range = new ImageRange(100L, 200L);

      assertFalse(range.equals(null), "Range should not equal null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final ImageRange range = new ImageRange(100L, 200L);

      assertFalse(range.equals("not a range"), "Range should not equal a String");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should contain hex addresses and size")
    void shouldContainHexAddressesAndSize() {
      final ImageRange range = new ImageRange(0x1000L, 0x2000L);
      final String str = range.toString();

      assertTrue(str.contains("1000"), "toString should contain hex start address, got: " + str);
      assertTrue(str.contains("2000"), "toString should contain hex end address, got: " + str);
      assertTrue(str.contains("4096"), "toString should contain decimal size, got: " + str);
    }

    @Test
    @DisplayName("should follow expected format")
    void shouldFollowExpectedFormat() {
      final ImageRange range = new ImageRange(0x1000L, 0x2000L);
      final String str = range.toString();

      assertTrue(
          str.startsWith("ImageRange{"), "toString should start with ImageRange{, got: " + str);
      assertTrue(str.contains("start=0x"), "toString should contain start=0x, got: " + str);
      assertTrue(str.contains("end=0x"), "toString should contain end=0x, got: " + str);
      assertTrue(str.contains("size="), "toString should contain size=, got: " + str);
    }
  }
}
