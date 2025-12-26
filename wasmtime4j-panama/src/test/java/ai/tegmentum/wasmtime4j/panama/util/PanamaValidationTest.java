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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaValidation} utility class.
 *
 * <p>This test class verifies the defensive programming validation methods.
 */
@DisplayName("PanamaValidation Tests")
class PanamaValidationTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaValidation should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaValidation.class.getModifiers()),
          "PanamaValidation should be final");
    }

    @Test
    @DisplayName("Constructor should throw AssertionError")
    void constructorShouldThrowAssertionError() throws Exception {
      final var constructor = PanamaValidation.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      assertThrows(
          AssertionError.class,
          constructor::newInstance,
          "Constructor should throw AssertionError");
    }
  }

  @Nested
  @DisplayName("requireNonNull Tests")
  class RequireNonNullTests {

    @Test
    @DisplayName("requireNonNull should accept non-null object")
    void requireNonNullShouldAcceptNonNullObject() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireNonNull("test", "paramName"),
          "Should accept non-null object");
    }

    @Test
    @DisplayName("requireNonNull should throw for null object")
    void requireNonNullShouldThrowForNullObject() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PanamaValidation.requireNonNull(null, "paramName"),
              "Should throw for null object");
      assertTrue(ex.getMessage().contains("paramName"), "Message should contain parameter name");
      assertTrue(ex.getMessage().contains("null"), "Message should indicate null");
    }
  }

  @Nested
  @DisplayName("requireNonEmpty String Tests")
  class RequireNonEmptyStringTests {

    @Test
    @DisplayName("requireNonEmpty should accept non-empty string")
    void requireNonEmptyShouldAcceptNonEmptyString() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireNonEmpty("test", "paramName"),
          "Should accept non-empty string");
    }

    @Test
    @DisplayName("requireNonEmpty should throw for null string")
    void requireNonEmptyShouldThrowForNullString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireNonEmpty((String) null, "paramName"),
          "Should throw for null string");
    }

    @Test
    @DisplayName("requireNonEmpty should throw for empty string")
    void requireNonEmptyShouldThrowForEmptyString() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PanamaValidation.requireNonEmpty("", "paramName"),
              "Should throw for empty string");
      assertTrue(ex.getMessage().contains("empty"), "Message should indicate empty");
    }
  }

  @Nested
  @DisplayName("requireNonEmpty Array Tests")
  class RequireNonEmptyArrayTests {

    @Test
    @DisplayName("requireNonEmpty should accept non-empty array")
    void requireNonEmptyShouldAcceptNonEmptyArray() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireNonEmpty(new byte[] {1, 2, 3}, "paramName"),
          "Should accept non-empty array");
    }

    @Test
    @DisplayName("requireNonEmpty should throw for null array")
    void requireNonEmptyShouldThrowForNullArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireNonEmpty((byte[]) null, "paramName"),
          "Should throw for null array");
    }

    @Test
    @DisplayName("requireNonEmpty should throw for empty array")
    void requireNonEmptyShouldThrowForEmptyArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireNonEmpty(new byte[0], "paramName"),
          "Should throw for empty array");
    }
  }

  @Nested
  @DisplayName("requireNonBlank Tests")
  class RequireNonBlankTests {

    @Test
    @DisplayName("requireNonBlank should accept non-blank string")
    void requireNonBlankShouldAcceptNonBlankString() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireNonBlank("test", "paramName"),
          "Should accept non-blank string");
    }

    @Test
    @DisplayName("requireNonBlank should throw for null string")
    void requireNonBlankShouldThrowForNullString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireNonBlank(null, "paramName"),
          "Should throw for null string");
    }

    @Test
    @DisplayName("requireNonBlank should throw for whitespace-only string")
    void requireNonBlankShouldThrowForWhitespaceOnlyString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireNonBlank("   ", "paramName"),
          "Should throw for whitespace-only string");
    }
  }

  @Nested
  @DisplayName("requireInRange Int Tests")
  class RequireInRangeIntTests {

    @Test
    @DisplayName("requireInRange should accept value in range")
    void requireInRangeShouldAcceptValueInRange() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireInRange(5, 1, 10, "paramName"),
          "Should accept value in range");
    }

    @Test
    @DisplayName("requireInRange should accept minimum value")
    void requireInRangeShouldAcceptMinimumValue() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireInRange(1, 1, 10, "paramName"),
          "Should accept minimum value");
    }

    @Test
    @DisplayName("requireInRange should accept maximum value")
    void requireInRangeShouldAcceptMaximumValue() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireInRange(10, 1, 10, "paramName"),
          "Should accept maximum value");
    }

    @Test
    @DisplayName("requireInRange should throw for value below minimum")
    void requireInRangeShouldThrowForValueBelowMinimum() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PanamaValidation.requireInRange(0, 1, 10, "paramName"),
              "Should throw for value below minimum");
      assertTrue(ex.getMessage().contains("0"), "Message should contain actual value");
    }

    @Test
    @DisplayName("requireInRange should throw for value above maximum")
    void requireInRangeShouldThrowForValueAboveMaximum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireInRange(11, 1, 10, "paramName"),
          "Should throw for value above maximum");
    }
  }

  @Nested
  @DisplayName("requireInRange Long Tests")
  class RequireInRangeLongTests {

    @Test
    @DisplayName("requireInRange should accept long value in range")
    void requireInRangeShouldAcceptLongValueInRange() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireInRange(50L, 10L, 100L, "paramName"),
          "Should accept long value in range");
    }

    @Test
    @DisplayName("requireInRange should throw for long value out of range")
    void requireInRangeShouldThrowForLongValueOutOfRange() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireInRange(5L, 10L, 100L, "paramName"),
          "Should throw for long value out of range");
    }
  }

  @Nested
  @DisplayName("requireNonNegative Tests")
  class RequireNonNegativeTests {

    @Test
    @DisplayName("requireNonNegative should accept zero")
    void requireNonNegativeShouldAcceptZero() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireNonNegative(0L, "paramName"), "Should accept zero");
    }

    @Test
    @DisplayName("requireNonNegative should accept positive value")
    void requireNonNegativeShouldAcceptPositiveValue() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireNonNegative(100L, "paramName"),
          "Should accept positive value");
    }

    @Test
    @DisplayName("requireNonNegative should throw for negative value")
    void requireNonNegativeShouldThrowForNegativeValue() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PanamaValidation.requireNonNegative(-1L, "paramName"),
              "Should throw for negative value");
      assertTrue(ex.getMessage().contains("non-negative"), "Message should indicate non-negative");
    }
  }

  @Nested
  @DisplayName("requireValidHandle Long Tests")
  class RequireValidHandleLongTests {

    @Test
    @DisplayName("requireValidHandle should accept non-zero handle")
    void requireValidHandleShouldAcceptNonZeroHandle() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireValidHandle(12345L, "handleName"),
          "Should accept non-zero handle");
    }

    @Test
    @DisplayName("requireValidHandle should throw for zero handle")
    void requireValidHandleShouldThrowForZeroHandle() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PanamaValidation.requireValidHandle(0L, "handleName"),
              "Should throw for zero handle");
      assertTrue(ex.getMessage().contains("null pointer"), "Message should indicate null pointer");
    }
  }

  @Nested
  @DisplayName("requireValidHandle MemorySegment Tests")
  class RequireValidHandleMemorySegmentTests {

    @Test
    @DisplayName("requireValidHandle should throw for null segment")
    void requireValidHandleShouldThrowForNullSegment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidHandle((MemorySegment) null, "handleName"),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("requireValidHandle should throw for NULL segment")
    void requireValidHandleShouldThrowForNullSegmentValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidHandle(MemorySegment.NULL, "handleName"),
          "Should throw for NULL segment");
    }
  }

  @Nested
  @DisplayName("defensiveCopy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("defensiveCopy should throw for null array")
    void defensiveCopyShouldThrowForNullArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.defensiveCopy(null),
          "Should throw for null array");
    }

    @Test
    @DisplayName("defensiveCopy should return copy of array")
    void defensiveCopyShouldReturnCopyOfArray() {
      final byte[] original = new byte[] {1, 2, 3, 4, 5};
      final byte[] copy = PanamaValidation.defensiveCopy(original);

      assertEquals(original.length, copy.length, "Copy should have same length");
      for (int i = 0; i < original.length; i++) {
        assertEquals(original[i], copy[i], "Copy should have same values");
      }

      // Verify it's a different array
      original[0] = 99;
      assertEquals(1, copy[0], "Copy should be independent of original");
    }
  }

  @Nested
  @DisplayName("requirePositive Int Tests")
  class RequirePositiveIntTests {

    @Test
    @DisplayName("requirePositive should accept positive value")
    void requirePositiveShouldAcceptPositiveValue() {
      final int result = PanamaValidation.requirePositive(5, "paramName");
      assertEquals(5, result, "Should return the value");
    }

    @Test
    @DisplayName("requirePositive should throw for zero")
    void requirePositiveShouldThrowForZero() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requirePositive(0, "paramName"),
          "Should throw for zero");
    }

    @Test
    @DisplayName("requirePositive should throw for negative")
    void requirePositiveShouldThrowForNegative() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requirePositive(-5, "paramName"),
          "Should throw for negative");
    }
  }

  @Nested
  @DisplayName("requirePositive Long Tests")
  class RequirePositiveLongTests {

    @Test
    @DisplayName("requirePositive should accept positive long value")
    void requirePositiveShouldAcceptPositiveLongValue() {
      final long result = PanamaValidation.requirePositive(100L, "paramName");
      assertEquals(100L, result, "Should return the value");
    }

    @Test
    @DisplayName("requirePositive should throw for zero long")
    void requirePositiveShouldThrowForZeroLong() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requirePositive(0L, "paramName"),
          "Should throw for zero");
    }
  }

  @Nested
  @DisplayName("requireValidPort Tests")
  class RequireValidPortTests {

    @Test
    @DisplayName("requireValidPort should accept valid port")
    void requireValidPortShouldAcceptValidPort() {
      assertDoesNotThrow(() -> PanamaValidation.requireValidPort(8080), "Should accept valid port");
    }

    @Test
    @DisplayName("requireValidPort should accept minimum port")
    void requireValidPortShouldAcceptMinimumPort() {
      assertDoesNotThrow(() -> PanamaValidation.requireValidPort(1), "Should accept minimum port");
    }

    @Test
    @DisplayName("requireValidPort should accept maximum port")
    void requireValidPortShouldAcceptMaximumPort() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireValidPort(65535), "Should accept maximum port");
    }

    @Test
    @DisplayName("requireValidPort should throw for zero port")
    void requireValidPortShouldThrowForZeroPort() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidPort(0),
          "Should throw for zero port");
    }

    @Test
    @DisplayName("requireValidPort should throw for port above 65535")
    void requireValidPortShouldThrowForPortAbove65535() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidPort(65536),
          "Should throw for port above 65535");
    }

    @Test
    @DisplayName("requireValidPort should throw for negative port")
    void requireValidPortShouldThrowForNegativePort() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidPort(-1),
          "Should throw for negative port");
    }
  }

  @Nested
  @DisplayName("requireValidConnectionId Tests")
  class RequireValidConnectionIdTests {

    @Test
    @DisplayName("requireValidConnectionId should accept valid connection")
    void requireValidConnectionIdShouldAcceptValidConnection() {
      final Map<Long, Object> connections = new HashMap<>();
      connections.put(1L, "connection");

      assertDoesNotThrow(
          () -> PanamaValidation.requireValidConnectionId(1L, connections),
          "Should accept valid connection");
    }

    @Test
    @DisplayName("requireValidConnectionId should throw for null map")
    void requireValidConnectionIdShouldThrowForNullMap() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidConnectionId(1L, null),
          "Should throw for null map");
    }

    @Test
    @DisplayName("requireValidConnectionId should throw for zero id")
    void requireValidConnectionIdShouldThrowForZeroId() {
      final Map<Long, Object> connections = new HashMap<>();
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidConnectionId(0L, connections),
          "Should throw for zero id");
    }

    @Test
    @DisplayName("requireValidConnectionId should throw for negative id")
    void requireValidConnectionIdShouldThrowForNegativeId() {
      final Map<Long, Object> connections = new HashMap<>();
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidConnectionId(-1L, connections),
          "Should throw for negative id");
    }

    @Test
    @DisplayName("requireValidConnectionId should throw for non-existent connection")
    void requireValidConnectionIdShouldThrowForNonExistentConnection() {
      final Map<Long, Object> connections = new HashMap<>();
      connections.put(1L, "connection");

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidConnectionId(999L, connections),
          "Should throw for non-existent connection");
    }
  }

  @Nested
  @DisplayName("requireValidString Tests")
  class RequireValidStringTests {

    @Test
    @DisplayName("requireValidString should accept non-empty string")
    void requireValidStringShouldAcceptNonEmptyString() {
      assertDoesNotThrow(
          () -> PanamaValidation.requireValidString("test", "paramName"),
          "Should accept non-empty string");
    }

    @Test
    @DisplayName("requireValidString should throw for null string")
    void requireValidStringShouldThrowForNullString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidString(null, "paramName"),
          "Should throw for null string");
    }

    @Test
    @DisplayName("requireValidString should throw for whitespace-only string")
    void requireValidStringShouldThrowForWhitespaceOnlyString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaValidation.requireValidString("   ", "paramName"),
          "Should throw for whitespace-only string");
    }
  }
}
