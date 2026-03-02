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
package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Validation} utility class.
 *
 * <p>Tests all 14 public validation methods plus the private constructor guard.
 */
@DisplayName("Validation Tests")
class ValidationTest {

  @Nested
  @DisplayName("Private Constructor Tests")
  class PrivateConstructorTests {

    @Test
    @DisplayName("should prevent instantiation via reflection")
    void shouldPreventInstantiation() throws NoSuchMethodException {
      final Constructor<Validation> constructor = Validation.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      final InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              () -> constructor.newInstance(),
              "Constructor should throw when invoked via reflection");
      assertTrue(
          exception.getCause() instanceof AssertionError,
          "Cause should be AssertionError, got: " + exception.getCause().getClass().getName());
    }
  }

  @Nested
  @DisplayName("requireNonNull Tests")
  class RequireNonNullTests {

    @Test
    @DisplayName("should throw for null object")
    void shouldThrowForNull() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireNonNull(null, "testParam"),
              "Should throw for null object");
      assertTrue(
          exception.getMessage().contains("testParam"),
          "Error message should contain parameter name, got: " + exception.getMessage());
      assertTrue(
          exception.getMessage().contains("must not be null"),
          "Error message should describe null violation, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should pass for non-null object")
    void shouldPassForNonNull() {
      assertDoesNotThrow(
          () -> Validation.requireNonNull("hello", "testParam"),
          "Should not throw for non-null object");
    }
  }

  @Nested
  @DisplayName("requireNonEmpty(String) Tests")
  class RequireNonEmptyStringTests {

    @Test
    @DisplayName("should throw for null string")
    void shouldThrowForNull() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireNonEmpty((String) null, "name"),
              "Should throw for null string");
      assertTrue(
          exception.getMessage().contains("must not be null"),
          "Error message should describe null violation, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw for empty string")
    void shouldThrowForEmpty() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireNonEmpty("", "name"),
              "Should throw for empty string");
      assertTrue(
          exception.getMessage().contains("must not be empty"),
          "Error message should describe empty violation, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should pass for non-empty string")
    void shouldPassForNonEmpty() {
      assertDoesNotThrow(
          () -> Validation.requireNonEmpty("hello", "name"),
          "Should not throw for non-empty string");
    }
  }

  @Nested
  @DisplayName("requireNonEmpty(byte[]) Tests")
  class RequireNonEmptyByteArrayTests {

    @Test
    @DisplayName("should throw for null array")
    void shouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireNonEmpty((byte[]) null, "data"),
          "Should throw for null byte array");
    }

    @Test
    @DisplayName("should throw for empty array")
    void shouldThrowForEmpty() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireNonEmpty(new byte[0], "data"),
              "Should throw for empty byte array");
      assertTrue(
          exception.getMessage().contains("must not be empty"),
          "Error message should describe empty violation, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should pass for non-empty array")
    void shouldPassForNonEmpty() {
      assertDoesNotThrow(
          () -> Validation.requireNonEmpty(new byte[] {1, 2, 3}, "data"),
          "Should not throw for non-empty byte array");
    }
  }

  @Nested
  @DisplayName("requireNonBlank Tests")
  class RequireNonBlankTests {

    @Test
    @DisplayName("should throw for null string")
    void shouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireNonBlank(null, "text"),
          "Should throw for null string");
    }

    @Test
    @DisplayName("should throw for empty string")
    void shouldThrowForEmpty() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireNonBlank("", "text"),
              "Should throw for empty string");
      assertTrue(
          exception.getMessage().contains("whitespace-only"),
          "Error message should describe blank violation, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw for whitespace-only string")
    void shouldThrowForWhitespace() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireNonBlank("   \t\n  ", "text"),
          "Should throw for whitespace-only string");
    }

    @Test
    @DisplayName("should pass for non-blank string")
    void shouldPassForNonBlank() {
      assertDoesNotThrow(
          () -> Validation.requireNonBlank("hello", "text"),
          "Should not throw for non-blank string");
    }
  }

  @Nested
  @DisplayName("requireInRange(int) Tests")
  class RequireInRangeIntTests {

    @Test
    @DisplayName("should throw for value below minimum")
    void shouldThrowForBelowMin() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireInRange(-1, 0, 100, "value"),
              "Should throw for value below minimum");
      assertTrue(
          exception.getMessage().contains("[0, 100]"),
          "Error message should contain range, got: " + exception.getMessage());
      assertTrue(
          exception.getMessage().contains("-1"),
          "Error message should contain actual value, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw for value above maximum")
    void shouldThrowForAboveMax() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireInRange(101, 0, 100, "value"),
          "Should throw for value above maximum");
    }

    @Test
    @DisplayName("should pass for value at minimum boundary")
    void shouldPassForMinBoundary() {
      assertDoesNotThrow(
          () -> Validation.requireInRange(0, 0, 100, "value"),
          "Should not throw for value at minimum boundary");
    }

    @Test
    @DisplayName("should pass for value at maximum boundary")
    void shouldPassForMaxBoundary() {
      assertDoesNotThrow(
          () -> Validation.requireInRange(100, 0, 100, "value"),
          "Should not throw for value at maximum boundary");
    }

    @Test
    @DisplayName("should pass for value within range")
    void shouldPassForInRange() {
      assertDoesNotThrow(
          () -> Validation.requireInRange(50, 0, 100, "value"),
          "Should not throw for value within range");
    }
  }

  @Nested
  @DisplayName("requireInRange(long) Tests")
  class RequireInRangeLongTests {

    @Test
    @DisplayName("should throw for value below minimum")
    void shouldThrowForBelowMin() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireInRange(-1L, 0L, 100L, "value"),
          "Should throw for long value below minimum");
    }

    @Test
    @DisplayName("should throw for value above maximum")
    void shouldThrowForAboveMax() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireInRange(101L, 0L, 100L, "value"),
          "Should throw for long value above maximum");
    }

    @Test
    @DisplayName("should pass for value at boundaries")
    void shouldPassForBoundaries() {
      assertDoesNotThrow(
          () -> Validation.requireInRange(0L, 0L, 100L, "value"),
          "Should not throw for long value at min boundary");
      assertDoesNotThrow(
          () -> Validation.requireInRange(100L, 0L, 100L, "value"),
          "Should not throw for long value at max boundary");
    }

    @Test
    @DisplayName("should handle large long values")
    void shouldHandleLargeLongValues() {
      assertDoesNotThrow(
          () ->
              Validation.requireInRange(
                  Long.MAX_VALUE - 1, Long.MAX_VALUE - 10, Long.MAX_VALUE, "value"),
          "Should handle large long values correctly");
    }
  }

  @Nested
  @DisplayName("requirePositive(int) Tests")
  class RequirePositiveIntTests {

    @Test
    @DisplayName("should throw for zero")
    void shouldThrowForZero() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requirePositive(0, "count"),
              "Should throw for zero");
      assertTrue(
          exception.getMessage().contains("must be positive"),
          "Error message should describe positive violation, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw for negative value")
    void shouldThrowForNegative() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requirePositive(-5, "count"),
          "Should throw for negative value");
    }

    @Test
    @DisplayName("should pass for positive value")
    void shouldPassForPositive() {
      assertDoesNotThrow(
          () -> Validation.requirePositive(1, "count"), "Should not throw for positive value");
    }
  }

  @Nested
  @DisplayName("requirePositive(long) Tests")
  class RequirePositiveLongTests {

    @Test
    @DisplayName("should throw for zero")
    void shouldThrowForZero() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requirePositive(0L, "size"),
          "Should throw for zero long");
    }

    @Test
    @DisplayName("should throw for negative value")
    void shouldThrowForNegative() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requirePositive(-1L, "size"),
          "Should throw for negative long");
    }

    @Test
    @DisplayName("should pass for positive value")
    void shouldPassForPositive() {
      assertDoesNotThrow(
          () -> Validation.requirePositive(1L, "size"), "Should not throw for positive long");
    }
  }

  @Nested
  @DisplayName("requireNonNegative(int) Tests")
  class RequireNonNegativeIntTests {

    @Test
    @DisplayName("should throw for negative value")
    void shouldThrowForNegative() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireNonNegative(-1, "offset"),
              "Should throw for negative value");
      assertTrue(
          exception.getMessage().contains("must be non-negative"),
          "Error message should describe non-negative violation, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should pass for zero")
    void shouldPassForZero() {
      assertDoesNotThrow(
          () -> Validation.requireNonNegative(0, "offset"), "Should not throw for zero");
    }

    @Test
    @DisplayName("should pass for positive value")
    void shouldPassForPositive() {
      assertDoesNotThrow(
          () -> Validation.requireNonNegative(42, "offset"), "Should not throw for positive value");
    }
  }

  @Nested
  @DisplayName("requireNonNegative(long) Tests")
  class RequireNonNegativeLongTests {

    @Test
    @DisplayName("should throw for negative value")
    void shouldThrowForNegative() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireNonNegative(-1L, "offset"),
          "Should throw for negative long");
    }

    @Test
    @DisplayName("should pass for zero")
    void shouldPassForZero() {
      assertDoesNotThrow(
          () -> Validation.requireNonNegative(0L, "offset"), "Should not throw for zero long");
    }

    @Test
    @DisplayName("should pass for positive value")
    void shouldPassForPositive() {
      assertDoesNotThrow(
          () -> Validation.requireNonNegative(42L, "offset"), "Should not throw for positive long");
    }
  }

  @Nested
  @DisplayName("requireValidHandle Tests")
  class RequireValidHandleTests {

    @Test
    @DisplayName("should throw for zero handle (null pointer)")
    void shouldThrowForZeroHandle() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireValidHandle(0L, "enginePtr"),
              "Should throw for zero handle");
      assertTrue(
          exception.getMessage().contains("null pointer"),
          "Error message should mention null pointer, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw for negative handle")
    void shouldThrowForNegativeHandle() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireValidHandle(-1L, "enginePtr"),
              "Should throw for negative handle");
      assertTrue(
          exception.getMessage().contains("negative value"),
          "Error message should mention negative value, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should pass for valid positive handle")
    void shouldPassForValidHandle() {
      assertDoesNotThrow(
          () -> Validation.requireValidHandle(1L, "enginePtr"),
          "Should not throw for valid positive handle");
    }

    @Test
    @DisplayName("should pass for large handle value")
    void shouldPassForLargeHandle() {
      assertDoesNotThrow(
          () -> Validation.requireValidHandle(Long.MAX_VALUE, "enginePtr"),
          "Should not throw for large handle value");
    }
  }

  @Nested
  @DisplayName("requireValidBounds Tests")
  class RequireValidBoundsTests {

    @Test
    @DisplayName("should throw for null array")
    void shouldThrowForNullArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireValidBounds(null, 0, 0, "buffer"),
          "Should throw for null array");
    }

    @Test
    @DisplayName("should throw for negative offset")
    void shouldThrowForNegativeOffset() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireValidBounds(new byte[10], -1, 5, "buffer"),
          "Should throw for negative offset");
    }

    @Test
    @DisplayName("should throw for negative length")
    void shouldThrowForNegativeLength() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.requireValidBounds(new byte[10], 0, -1, "buffer"),
          "Should throw for negative length");
    }

    @Test
    @DisplayName("should throw for offset exceeding array length")
    void shouldThrowForOffsetExceedingLength() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireValidBounds(new byte[10], 11, 0, "buffer"),
              "Should throw for offset exceeding array length");
      assertTrue(
          exception.getMessage().contains("exceeds array length"),
          "Error message should describe offset overflow, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw for offset+length exceeding array length")
    void shouldThrowForOffsetPlusLengthOverflow() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.requireValidBounds(new byte[10], 5, 6, "buffer"),
              "Should throw for offset+length exceeding array length");
      assertTrue(
          exception.getMessage().contains("exceeds array length"),
          "Error message should describe bounds overflow, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should pass for valid bounds")
    void shouldPassForValidBounds() {
      assertDoesNotThrow(
          () -> Validation.requireValidBounds(new byte[10], 0, 10, "buffer"),
          "Should not throw for valid full-array bounds");
    }

    @Test
    @DisplayName("should pass for zero-length access")
    void shouldPassForZeroLengthAccess() {
      assertDoesNotThrow(
          () -> Validation.requireValidBounds(new byte[10], 5, 0, "buffer"),
          "Should not throw for zero-length access at valid offset");
    }
  }

  @Nested
  @DisplayName("require Tests")
  class RequireTests {

    @Test
    @DisplayName("should throw for false condition")
    void shouldThrowForFalseCondition() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Validation.require(false, "Custom error message"),
              "Should throw for false condition");
      assertEquals(
          "Custom error message",
          exception.getMessage(),
          "Error message should match provided message");
    }

    @Test
    @DisplayName("should pass for true condition")
    void shouldPassForTrueCondition() {
      assertDoesNotThrow(
          () -> Validation.require(true, "Should not appear"),
          "Should not throw for true condition");
    }
  }

  @Nested
  @DisplayName("defensiveCopy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertNull(Validation.defensiveCopy(null), "Defensive copy of null should return null");
    }

    @Test
    @DisplayName("should return distinct array with same contents")
    void shouldReturnDistinctCopy() {
      final byte[] original = {1, 2, 3, 4, 5};
      final byte[] copy = Validation.defensiveCopy(original);

      assertNotNull(copy, "Copy should not be null");
      assertNotSame(original, copy, "Copy should be a distinct array instance");
      assertArrayEquals(original, copy, "Copy contents should match original");
    }

    @Test
    @DisplayName("should not be affected by mutations to original")
    void shouldNotBeAffectedByOriginalMutation() {
      final byte[] original = {10, 20, 30};
      final byte[] copy = Validation.defensiveCopy(original);

      original[0] = 99;

      assertEquals(
          10,
          copy[0],
          "Copy should not be affected by mutation of original, expected 10 but got " + copy[0]);
    }

    @Test
    @DisplayName("should handle empty array")
    void shouldHandleEmptyArray() {
      final byte[] original = {};
      final byte[] copy = Validation.defensiveCopy(original);

      assertNotNull(copy, "Copy of empty array should not be null");
      assertNotSame(original, copy, "Copy should be a distinct instance even for empty array");
      assertEquals(0, copy.length, "Copy of empty array should have length 0");
    }
  }

  @Nested
  @DisplayName("toBytes Tests")
  class ToBytesTests {

    @Test
    @DisplayName("should throw for null string")
    void shouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Validation.toBytes(null, "input"),
          "Should throw for null string");
    }

    @Test
    @DisplayName("should return UTF-8 bytes for valid string")
    void shouldReturnUtf8Bytes() {
      final String input = "hello";
      final byte[] result = Validation.toBytes(input, "input");

      assertArrayEquals(
          input.getBytes(StandardCharsets.UTF_8), result, "Should return UTF-8 encoded bytes");
    }

    @Test
    @DisplayName("should handle multibyte UTF-8 characters")
    void shouldHandleMultibyteCharacters() {
      final String input = "\u00e9\u00e8\u00ea"; // é è ê
      final byte[] result = Validation.toBytes(input, "input");

      assertArrayEquals(
          input.getBytes(StandardCharsets.UTF_8),
          result,
          "Should correctly encode multibyte UTF-8 characters");
      assertTrue(
          result.length > input.length(),
          "UTF-8 encoded multibyte chars should produce more bytes than chars, "
              + "got "
              + result.length
              + " bytes for "
              + input.length()
              + " chars");
    }

    @Test
    @DisplayName("should handle empty string")
    void shouldHandleEmptyString() {
      final byte[] result = Validation.toBytes("", "input");

      assertNotNull(result, "Result should not be null for empty string");
      assertEquals(0, result.length, "Empty string should produce zero-length byte array");
    }
  }
}
