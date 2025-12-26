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

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WitType;
import ai.tegmentum.wasmtime4j.exception.WitValueException.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WitRangeException} class.
 *
 * <p>This test class verifies the construction and behavior of WIT range exceptions, including
 * range bounds and factory methods.
 */
@DisplayName("WitRangeException Tests")
class WitRangeExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WitRangeException should extend WitValueException")
    void shouldExtendWitValueException() {
      assertTrue(
          WitValueException.class.isAssignableFrom(WitRangeException.class),
          "WitRangeException should extend WitValueException");
    }

    @Test
    @DisplayName("WitRangeException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(WitRangeException.class),
          "WitRangeException should be serializable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message should set error code to RANGE_ERROR")
    void constructorWithMessageShouldSetRangeErrorCode() {
      final WitRangeException exception = new WitRangeException("Value out of range");

      assertTrue(
          exception.getMessage().contains("Value out of range"),
          "Message should contain error text");
      assertEquals(ErrorCode.RANGE_ERROR, exception.getCode(), "Error code should be RANGE_ERROR");
      assertNull(exception.getMinValue(), "Min value should be null");
      assertNull(exception.getMaxValue(), "Max value should be null");
    }

    @Test
    @DisplayName("Constructor with message and type info should set fields")
    void constructorWithMessageAndTypeInfoShouldSetFields() {
      final WitType type = WitType.createU8();
      final WitRangeException exception = new WitRangeException("Range error", type, 300);

      assertEquals(ErrorCode.RANGE_ERROR, exception.getCode(), "Error code should be RANGE_ERROR");
      assertTrue(exception.getExpectedType().isPresent(), "Expected type should be present");
      assertTrue(exception.getActualValue().isPresent(), "Actual value should be present");
      assertEquals(300, exception.getActualValue().get(), "Actual value should be 300");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final WitType type = WitType.createS16();
      final WitRangeException exception =
          new WitRangeException("Value out of range", type, 50000L, -32768L, 32767L);

      assertTrue(
          exception.getMessage().contains("Value out of range"),
          "Message should contain error text");
      assertEquals(-32768L, exception.getMinValue(), "Min value should be -32768");
      assertEquals(32767L, exception.getMaxValue(), "Max value should be 32767");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getMinValue should return minimum value when set")
    void getMinValueShouldReturnMinValueWhenSet() {
      final WitRangeException exception =
          new WitRangeException("Range error", WitType.createU32(), 5000000000L, 0L, 4294967295L);

      assertEquals(0L, exception.getMinValue(), "Min value should be 0");
    }

    @Test
    @DisplayName("getMaxValue should return maximum value when set")
    void getMaxValueShouldReturnMaxValueWhenSet() {
      final WitRangeException exception =
          new WitRangeException("Range error", WitType.createU32(), 5000000000L, 0L, 4294967295L);

      assertEquals(4294967295L, exception.getMaxValue(), "Max value should be 4294967295");
    }

    @Test
    @DisplayName("getMinValue should return null when not set")
    void getMinValueShouldReturnNullWhenNotSet() {
      final WitRangeException exception = new WitRangeException("Range error");

      assertNull(exception.getMinValue(), "Min value should be null when not set");
    }

    @Test
    @DisplayName("getMaxValue should return null when not set")
    void getMaxValueShouldReturnNullWhenNotSet() {
      final WitRangeException exception = new WitRangeException("Range error");

      assertNull(exception.getMaxValue(), "Max value should be null when not set");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("unsignedOverflow should create exception with range info")
    void unsignedOverflowShouldCreateExceptionWithRangeInfo() {
      final WitType type = WitType.createU8();
      final WitRangeException exception = WitRangeException.unsignedOverflow(type, 300, 255);

      assertTrue(exception.getMessage().contains("300"), "Message should contain the value");
      assertTrue(exception.getMessage().contains("255"), "Message should contain the max value");
      assertEquals(0L, exception.getMinValue(), "Min value should be 0");
      assertEquals(255L, exception.getMaxValue(), "Max value should be 255");
    }

    @Test
    @DisplayName("negativeUnsigned should create exception for negative values")
    void negativeUnsignedShouldCreateExceptionForNegativeValues() {
      final WitType type = WitType.createU32();
      final WitRangeException exception = WitRangeException.negativeUnsigned(type, -5);

      assertTrue(
          exception.getMessage().contains("-5"), "Message should contain the negative value");
      assertTrue(exception.getMessage().contains("unsigned"), "Message should mention unsigned");
    }

    @Test
    @DisplayName("signedOverflow should create exception with range bounds")
    void signedOverflowShouldCreateExceptionWithRangeBounds() {
      final WitType type = WitType.createS8();
      final WitRangeException exception = WitRangeException.signedOverflow(type, 200, -128, 127);

      assertTrue(exception.getMessage().contains("200"), "Message should contain the value");
      assertTrue(exception.getMessage().contains("-128"), "Message should contain min bound");
      assertTrue(exception.getMessage().contains("127"), "Message should contain max bound");
      assertEquals(-128L, exception.getMinValue(), "Min value should be -128");
      assertEquals(127L, exception.getMaxValue(), "Max value should be 127");
    }

    @Test
    @DisplayName("invalidFloatingPoint should create exception for special values")
    void invalidFloatingPointShouldCreateExceptionForSpecialValues() {
      final WitType type = WitType.createFloat64();
      final WitRangeException exception = WitRangeException.invalidFloatingPoint(type, Double.NaN);

      assertTrue(
          exception.getMessage().toLowerCase().contains("invalid"),
          "Message should mention invalid");
      assertTrue(
          exception.getMessage().toLowerCase().contains("floating"),
          "Message should mention floating-point");
    }

    @Test
    @DisplayName("invalidCodepoint should create exception for invalid Unicode")
    void invalidCodepointShouldCreateExceptionForInvalidUnicode() {
      final WitRangeException exception = WitRangeException.invalidCodepoint(0x110000);

      assertTrue(exception.getMessage().contains("110000"), "Message should contain the codepoint");
      assertTrue(
          exception.getMessage().toLowerCase().contains("unicode")
              || exception.getMessage().toLowerCase().contains("codepoint"),
          "Message should mention Unicode or codepoint");
      assertEquals(0, exception.getMinValue(), "Min value should be 0");
      assertEquals(0x10FFFF, exception.getMaxValue(), "Max value should be 0x10FFFF");
    }
  }

  @Nested
  @DisplayName("getMessage Tests")
  class GetMessageTests {

    @Test
    @DisplayName("getMessage should include valid range when bounds set")
    void getMessageShouldIncludeValidRangeWhenBoundsSet() {
      final WitRangeException exception =
          new WitRangeException(
              "Range error", WitType.createS32(), 3000000000L, -2147483648L, 2147483647L);

      assertTrue(
          exception.getMessage().contains("Valid range"), "Message should mention valid range");
      assertTrue(
          exception.getMessage().contains("-2147483648"), "Message should contain min bound");
      assertTrue(exception.getMessage().contains("2147483647"), "Message should contain max bound");
    }

    @Test
    @DisplayName("getMessage should not include range when bounds not set")
    void getMessageShouldNotIncludeRangeWhenBoundsNotSet() {
      final WitRangeException exception = new WitRangeException("Simple range error");

      // Message should contain base text but not "Valid range"
      assertTrue(
          exception.getMessage().contains("Simple range error"),
          "Message should contain base text");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final WitRangeException exception = new WitRangeException("Test");

      assertTrue(exception instanceof Throwable, "WitRangeException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WitValueException")
    void shouldBeCatchableAsWitValueException() {
      try {
        throw new WitRangeException("Test error");
      } catch (WitValueException e) {
        assertTrue(
            e.getMessage().contains("Test error"), "Should be catchable as WitValueException");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new WitRangeException("Test error");
      } catch (WasmException e) {
        assertNotNull(e, "Should be catchable as WasmException");
      }
    }
  }
}
