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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

  @Nested
  @DisplayName("getMessage Format Mutation Tests")
  class GetMessageFormatMutationTests {

    @Test
    @DisplayName("getMessage should append valid range info when both min and max are set")
    void getMessageShouldAppendValidRangeInfoWhenBothMinAndMaxAreSet() {
      final WitRangeException exception =
          new WitRangeException("Base message", WitType.createU8(), 300, 0, 255);

      final String message = exception.getMessage();

      // Verify the format includes "Valid range:" and "to"
      assertTrue(message.contains("Valid range:"), "Should contain 'Valid range:'");
      assertTrue(message.contains(" to "), "Should contain ' to ' between min and max");
      assertTrue(message.contains("0"), "Should contain min value 0");
      assertTrue(message.contains("255"), "Should contain max value 255");
    }

    @Test
    @DisplayName("getMessage should not append range info when min is null")
    void getMessageShouldNotAppendRangeInfoWhenMinIsNull() {
      // Using constructor that doesn't set min/max
      final WitRangeException exception =
          new WitRangeException("Base message", WitType.createU8(), 300);

      final String message = exception.getMessage();

      // Verify the format does NOT include "Valid range:"
      assertFalse(message.contains("Valid range:"), "Should not contain 'Valid range:'");
      assertFalse(message.contains(" to "), "Should not contain ' to '");
    }

    @Test
    @DisplayName("getMessage should not append range info when max is null")
    void getMessageShouldNotAppendRangeInfoWhenMaxIsNull() {
      // Using simple constructor
      final WitRangeException exception = new WitRangeException("Base message only");

      final String message = exception.getMessage();

      assertFalse(message.contains("Valid range:"), "Should not contain 'Valid range:'");
    }

    @Test
    @DisplayName("getMessage format should use correct separators")
    void getMessageFormatShouldUseCorrectSeparators() {
      final WitRangeException exception =
          new WitRangeException("Test", WitType.createS16(), 50000L, -32768L, 32767L);

      final String message = exception.getMessage();

      // Verify the bracket format [Valid range: X to Y]
      assertTrue(message.contains("[Valid range:"), "Should start range with '['");
      assertTrue(message.contains("]"), "Should end range with ']'");
    }
  }

  @Nested
  @DisplayName("Factory Method Format Mutation Tests")
  class FactoryMethodFormatMutationTests {

    @Test
    @DisplayName("unsignedOverflow message should contain 'exceeds maximum'")
    void unsignedOverflowMessageShouldContainExceedsMaximum() {
      final WitRangeException exception =
          WitRangeException.unsignedOverflow(WitType.createU8(), 300, 255);

      final String message = exception.getMessage();

      assertTrue(message.contains("exceeds maximum"), "Should contain 'exceeds maximum'");
      assertTrue(message.contains("unsigned"), "Should contain 'unsigned'");
    }

    @Test
    @DisplayName("unsignedOverflow message should include all three numeric values")
    void unsignedOverflowMessageShouldIncludeAllThreeNumericValues() {
      final WitRangeException exception =
          WitRangeException.unsignedOverflow(WitType.createU16(), 70000, 65535);

      final String message = exception.getMessage();

      assertTrue(message.contains("70000"), "Should contain the actual value");
      assertTrue(message.contains("65535"), "Should contain the max value");
      // The "0" is the min value set in the constructor (0L)
      assertEquals(0L, exception.getMinValue(), "Min value should be 0");
    }

    @Test
    @DisplayName("negativeUnsigned message should contain 'Negative value' and 'not allowed'")
    void negativeUnsignedMessageShouldContainNegativeValueAndNotAllowed() {
      final WitRangeException exception =
          WitRangeException.negativeUnsigned(WitType.createU32(), -100);

      final String message = exception.getMessage();

      assertTrue(message.contains("Negative value"), "Should contain 'Negative value'");
      assertTrue(message.contains("not allowed"), "Should contain 'not allowed'");
      assertTrue(message.contains("unsigned"), "Should contain 'unsigned'");
      assertTrue(message.contains("-100"), "Should contain the negative value");
    }

    @Test
    @DisplayName("signedOverflow message should contain 'out of range' and brackets")
    void signedOverflowMessageShouldContainOutOfRangeAndBrackets() {
      final WitRangeException exception =
          WitRangeException.signedOverflow(WitType.createS8(), 200, -128, 127);

      final String message = exception.getMessage();

      assertTrue(message.contains("out of range"), "Should contain 'out of range'");
      assertTrue(message.contains("["), "Should contain opening bracket");
      assertTrue(message.contains("]"), "Should contain closing bracket");
      assertTrue(message.contains("-128"), "Should contain min value");
      assertTrue(message.contains("127"), "Should contain max value");
    }

    @Test
    @DisplayName("signedOverflow message should format with comma-separated bounds")
    void signedOverflowMessageShouldFormatWithCommaSeparatedBounds() {
      final WitRangeException exception =
          WitRangeException.signedOverflow(WitType.createS32(), 3000000000L, -2147483648L, 2147483647L);

      final String message = exception.getMessage();

      // The format is: "Value %d out of range for %s [%d, %d]"
      assertTrue(message.contains(", "), "Should contain comma separator in bounds");
    }

    @Test
    @DisplayName("invalidFloatingPoint message should contain 'Invalid floating-point value'")
    void invalidFloatingPointMessageShouldContainInvalidFloatingPointValue() {
      final WitRangeException exception =
          WitRangeException.invalidFloatingPoint(WitType.createFloat64(), Double.NaN);

      final String message = exception.getMessage();

      assertTrue(
          message.contains("Invalid floating-point value"),
          "Should contain 'Invalid floating-point value'");
    }

    @Test
    @DisplayName("invalidFloatingPoint message should include type and value")
    void invalidFloatingPointMessageShouldIncludeTypeAndValue() {
      final WitRangeException exception =
          WitRangeException.invalidFloatingPoint(WitType.createFloat32(), Double.POSITIVE_INFINITY);

      final String message = exception.getMessage();

      // The format includes type name and value
      assertTrue(message.contains("for"), "Should contain 'for' before type");
      assertTrue(message.contains(":"), "Should contain ':' before value");
    }

    @Test
    @DisplayName("invalidFloatingPoint should work with NaN")
    void invalidFloatingPointShouldWorkWithNaN() {
      final WitRangeException exception =
          WitRangeException.invalidFloatingPoint(WitType.createFloat64(), Double.NaN);

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(exception.getMessage().length() > 0, "Message should not be empty");
    }

    @Test
    @DisplayName("invalidFloatingPoint should work with Infinity")
    void invalidFloatingPointShouldWorkWithInfinity() {
      final WitRangeException exception =
          WitRangeException.invalidFloatingPoint(WitType.createFloat64(), Double.NEGATIVE_INFINITY);

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(
          exception.getMessage().contains("Infinity") || exception.getMessage().contains("-Infinity"),
          "Message should contain infinity representation");
    }
  }

  @Nested
  @DisplayName("getMessage AND Condition Tests")
  class GetMessageAndConditionTests {

    @Test
    @DisplayName("Valid range appended only when BOTH min AND max are non-null")
    void validRangeAppendedOnlyWhenBothMinAndMaxAreNonNull() {
      // Both set - should show range
      final WitRangeException bothSet =
          new WitRangeException("Test", WitType.createU8(), 300, 0, 255);
      assertTrue(
          bothSet.getMessage().contains("Valid range"),
          "Should show range when both min and max are set");

      // Neither set - should NOT show range
      final WitRangeException neitherSet = new WitRangeException("Test");
      assertFalse(
          neitherSet.getMessage().contains("Valid range"),
          "Should not show range when neither is set");
    }

    @Test
    @DisplayName("getMessage condition requires both minValue != null AND maxValue != null")
    void getMessageConditionRequiresBothMinValueAndMaxValueNotNull() {
      // Test with full constructor (both values set)
      final WitRangeException exception =
          new WitRangeException("Base", WitType.createS8(), 200, -128, 127);

      // The condition is: if (minValue != null && maxValue != null)
      // If either is null, the condition fails and range is not appended
      assertNotNull(exception.getMinValue(), "Min value should be set");
      assertNotNull(exception.getMaxValue(), "Max value should be set");
      assertTrue(
          exception.getMessage().contains("Valid range"),
          "Should append range when both values are non-null");
    }
  }

  @Nested
  @DisplayName("InlineConstant Mutation Killing Tests")
  class InlineConstantMutationKillingTests {

    @Test
    @DisplayName("unsignedOverflow format should include 'Value' at start")
    void unsignedOverflowFormatShouldIncludeValueAtStart() {
      final WitRangeException exception =
          WitRangeException.unsignedOverflow(WitType.createU8(), 300, 255);

      // Verify exact format: "Value %d exceeds maximum for unsigned %s: %d"
      assertTrue(exception.getMessage().startsWith("Value "), "Message should start with 'Value '");
    }

    @Test
    @DisplayName("unsignedOverflow format should contain exact keywords in order")
    void unsignedOverflowFormatShouldContainExactKeywordsInOrder() {
      final WitRangeException exception =
          WitRangeException.unsignedOverflow(WitType.createU8(), 300, 255);

      final String message = exception.getMessage();
      final int valueIndex = message.indexOf("Value");
      final int exceedsIndex = message.indexOf("exceeds");
      final int maximumIndex = message.indexOf("maximum");
      final int forIndex = message.indexOf("for");
      final int unsignedIndex = message.indexOf("unsigned");

      assertTrue(valueIndex >= 0, "Should contain 'Value'");
      assertTrue(exceedsIndex > valueIndex, "'exceeds' should come after 'Value'");
      assertTrue(maximumIndex > exceedsIndex, "'maximum' should come after 'exceeds'");
      assertTrue(forIndex > maximumIndex, "'for' should come after 'maximum'");
      assertTrue(unsignedIndex > forIndex, "'unsigned' should come after 'for'");
    }

    @Test
    @DisplayName("negativeUnsigned format should start with 'Negative value'")
    void negativeUnsignedFormatShouldStartWithNegativeValue() {
      final WitRangeException exception =
          WitRangeException.negativeUnsigned(WitType.createU8(), -5);

      assertTrue(
          exception.getMessage().startsWith("Negative value"),
          "Message should start with 'Negative value'");
    }

    @Test
    @DisplayName("negativeUnsigned format should contain 'not allowed for unsigned'")
    void negativeUnsignedFormatShouldContainNotAllowedForUnsigned() {
      final WitRangeException exception =
          WitRangeException.negativeUnsigned(WitType.createU8(), -5);

      assertTrue(
          exception.getMessage().contains("not allowed for unsigned"),
          "Message should contain 'not allowed for unsigned'");
    }

    @Test
    @DisplayName("signedOverflow format should start with 'Value'")
    void signedOverflowFormatShouldStartWithValue() {
      final WitRangeException exception =
          WitRangeException.signedOverflow(WitType.createS8(), 200, -128, 127);

      assertTrue(exception.getMessage().startsWith("Value "), "Message should start with 'Value '");
    }

    @Test
    @DisplayName("signedOverflow format should contain 'out of range for'")
    void signedOverflowFormatShouldContainOutOfRangeFor() {
      final WitRangeException exception =
          WitRangeException.signedOverflow(WitType.createS8(), 200, -128, 127);

      assertTrue(
          exception.getMessage().contains("out of range for"),
          "Message should contain 'out of range for'");
    }

    @Test
    @DisplayName("signedOverflow format should have bounds in brackets with comma")
    void signedOverflowFormatShouldHaveBoundsInBracketsWithComma() {
      final WitRangeException exception =
          WitRangeException.signedOverflow(WitType.createS8(), 200, -128, 127);

      final String message = exception.getMessage();

      // Format: "Value %d out of range for %s [%d, %d]"
      // The bounds are in format [min, max]
      assertTrue(message.contains("[-128, 127]"), "Should contain bounds in [min, max] format");
    }

    @Test
    @DisplayName("invalidFloatingPoint format should start with 'Invalid'")
    void invalidFloatingPointFormatShouldStartWithInvalid() {
      final WitRangeException exception =
          WitRangeException.invalidFloatingPoint(WitType.createFloat64(), Double.NaN);

      assertTrue(
          exception.getMessage().startsWith("Invalid"),
          "Message should start with 'Invalid'");
    }

    @Test
    @DisplayName("invalidFloatingPoint format should contain 'floating-point value for'")
    void invalidFloatingPointFormatShouldContainFloatingPointValueFor() {
      final WitRangeException exception =
          WitRangeException.invalidFloatingPoint(WitType.createFloat64(), Double.NaN);

      assertTrue(
          exception.getMessage().contains("floating-point value for"),
          "Message should contain 'floating-point value for'");
    }

    @Test
    @DisplayName("getMessage format should append '[Valid range:' followed by ' to '")
    void getMessageFormatShouldAppendValidRangeFollowedByTo() {
      final WitRangeException exception =
          new WitRangeException("Test", WitType.createU8(), 300, 0, 255);

      final String message = exception.getMessage();

      // Format: " [Valid range: %s to %s]"
      assertTrue(message.contains("[Valid range:"), "Should contain '[Valid range:'");
      final int validRangeIndex = message.indexOf("[Valid range:");
      final int toIndex = message.indexOf(" to ", validRangeIndex);
      assertTrue(toIndex > validRangeIndex, "' to ' should come after '[Valid range:'");
    }

    @Test
    @DisplayName("getMessage format should end with closing bracket")
    void getMessageFormatShouldEndWithClosingBracket() {
      final WitRangeException exception =
          new WitRangeException("Base", WitType.createU8(), 300, 0, 255);

      final String message = exception.getMessage();

      // The range part should end with ]
      assertTrue(message.endsWith("]"), "Message with range should end with ']'");
    }
  }

  @Nested
  @DisplayName("RemoveConditional EQUAL_IF Mutation Killing Tests")
  class RemoveConditionalEqualIfMutationKillingTests {

    @Test
    @DisplayName("getMessage should NOT show range when only min is null")
    void getMessageShouldNotShowRangeWhenOnlyMinIsNull() {
      // Constructor that sets only message (min and max are null)
      final WitRangeException exception = new WitRangeException("Error");

      assertNull(exception.getMinValue(), "Min should be null");
      assertNull(exception.getMaxValue(), "Max should be null");

      // If condition was replaced with true, range would be shown even with nulls
      // The format would try to format null values
      assertFalse(
          exception.getMessage().contains("Valid range"),
          "Should NOT show range when min is null");
      assertFalse(
          exception.getMessage().contains(" to "),
          "Should NOT contain ' to ' when range not shown");
    }

    @Test
    @DisplayName("getMessage should NOT show range when only max is null")
    void getMessageShouldNotShowRangeWhenOnlyMaxIsNull() {
      // Constructor that sets type and value but not range
      final WitRangeException exception =
          new WitRangeException("Error", WitType.createU8(), 300);

      assertNull(exception.getMinValue(), "Min should be null");
      assertNull(exception.getMaxValue(), "Max should be null");

      // If condition was replaced with true, range would be shown even with nulls
      assertFalse(
          exception.getMessage().contains("Valid range"),
          "Should NOT show range when max is null");
    }

    @Test
    @DisplayName("getMessage should SHOW range when both min and max are non-null")
    void getMessageShouldShowRangeWhenBothMinAndMaxAreNonNull() {
      // Full constructor with range
      final WitRangeException exception =
          new WitRangeException("Error", WitType.createU8(), 300, 0, 255);

      assertNotNull(exception.getMinValue(), "Min should be non-null");
      assertNotNull(exception.getMaxValue(), "Max should be non-null");

      // If condition was replaced with false, range would NOT be shown
      assertTrue(
          exception.getMessage().contains("Valid range"),
          "SHOULD show range when both are non-null");
      assertTrue(
          exception.getMessage().contains(" to "),
          "SHOULD contain ' to ' when range is shown");
    }

    @Test
    @DisplayName("Verify different behavior between null and non-null bounds")
    void verifyDifferentBehaviorBetweenNullAndNonNullBounds() {
      // Without bounds
      final WitRangeException withoutBounds = new WitRangeException("Base message");

      // With bounds
      final WitRangeException withBounds =
          new WitRangeException("Base message", WitType.createU8(), 300, 0, 255);

      // The messages should be different
      assertFalse(
          withoutBounds.getMessage().equals(withBounds.getMessage()),
          "Messages should differ based on whether bounds are set");

      // More specifically, one should contain range info and the other shouldn't
      assertFalse(
          withoutBounds.getMessage().contains("Valid range"),
          "Without bounds should not have range");
      assertTrue(
          withBounds.getMessage().contains("Valid range"),
          "With bounds should have range");
    }
  }

  @Nested
  @DisplayName("String Format Argument Order Mutation Tests")
  class StringFormatArgumentOrderMutationTests {

    @Test
    @DisplayName("unsignedOverflow format arg 0 should be the value")
    void unsignedOverflowFormatArg0ShouldBeTheValue() {
      // Format: "Value %d exceeds maximum for unsigned %s: %d"
      // Args: value (arg 0), type (arg 1), maxValue (arg 2)
      final WitRangeException exception =
          WitRangeException.unsignedOverflow(WitType.createU8(), 300, 255);

      final String message = exception.getMessage();

      // Verify "Value 300" appears (value is first arg)
      assertTrue(message.startsWith("Value 300 "),
          "Message should start with 'Value 300 ' - value is first format arg: " + message);
    }

    @Test
    @DisplayName("unsignedOverflow format arg 2 should be the maxValue")
    void unsignedOverflowFormatArg2ShouldBeTheMaxValue() {
      // Format: "Value %d exceeds maximum for unsigned %s: %d"
      // The maxValue should appear after the colon
      final WitRangeException exception =
          WitRangeException.unsignedOverflow(WitType.createU8(), 300, 255);

      final String message = exception.getMessage();

      // The base format ends with ": 255" before parent class appends additional info
      // Look for ": 255 " (with space after, before [Expected type...])
      assertTrue(message.contains(": 255 ") || message.contains(": 255["),
          "Message should contain ': 255' followed by space or bracket - maxValue is third format arg: " + message);
    }

    @Test
    @DisplayName("negativeUnsigned format arg 0 should be the value")
    void negativeUnsignedFormatArg0ShouldBeTheValue() {
      // Format: "Negative value %d not allowed for unsigned %s"
      // Args: value (arg 0), type (arg 1)
      final WitRangeException exception =
          WitRangeException.negativeUnsigned(WitType.createU32(), -42);

      final String message = exception.getMessage();

      // Verify "Negative value -42 " appears (value is first arg after "Negative value ")
      assertTrue(message.startsWith("Negative value -42 "),
          "Message should start with 'Negative value -42 ' - value is first format arg: " + message);
    }

    @Test
    @DisplayName("negativeUnsigned format arg 1 should be the type")
    void negativeUnsignedFormatArg1ShouldBeTheType() {
      // Format: "Negative value %d not allowed for unsigned %s"
      // Type should appear after "unsigned" in the message
      final WitType type = WitType.createU64();
      final WitRangeException exception =
          WitRangeException.negativeUnsigned(type, -100);

      final String message = exception.getMessage();

      // Type.toString() should appear after "unsigned " and before " [Expected type"
      final int unsignedIndex = message.indexOf("unsigned ");
      assertTrue(unsignedIndex >= 0, "Message should contain 'unsigned '");

      final int typeEndIndex = message.indexOf(" [Expected type");
      assertTrue(typeEndIndex > unsignedIndex, "Should have '[Expected type' after unsigned");

      final String typeInMessage = message.substring(unsignedIndex + "unsigned ".length(), typeEndIndex);
      assertEquals(type.toString(), typeInMessage,
          "Type should appear between 'unsigned ' and ' [Expected type': " + message);
    }

    @Test
    @DisplayName("signedOverflow format arg 0 should be the value")
    void signedOverflowFormatArg0ShouldBeTheValue() {
      // Format: "Value %d out of range for %s [%d, %d]"
      // Args: value (arg 0), type (arg 1), min (arg 2), max (arg 3)
      final WitRangeException exception =
          WitRangeException.signedOverflow(WitType.createS8(), 200, -128, 127);

      final String message = exception.getMessage();

      // Verify "Value 200 " appears at start
      assertTrue(message.startsWith("Value 200 "),
          "Message should start with 'Value 200 ' - value is first format arg: " + message);
    }

    @Test
    @DisplayName("signedOverflow format args 2 and 3 should be min and max in brackets")
    void signedOverflowFormatArgs2And3ShouldBeMinAndMaxInBrackets() {
      // Format: "Value %d out of range for %s [%d, %d]"
      // The [min, max] should appear at the end with min before max
      final WitRangeException exception =
          WitRangeException.signedOverflow(WitType.createS16(), 50000, -32768, 32767);

      final String message = exception.getMessage();

      // The base message (before [Valid range]) should contain [-32768, 32767]
      // Note: The format puts min before max
      assertTrue(message.contains("[-32768, 32767]"),
          "Message should contain '[-32768, 32767]' with min before max: " + message);
    }

    @Test
    @DisplayName("invalidFloatingPoint format arg 0 should be the type")
    void invalidFloatingPointFormatArg0ShouldBeTheType() {
      // Format: "Invalid floating-point value for %s: %f"
      // Args: type (arg 0), value (arg 1)
      final WitType type = WitType.createFloat32();
      final WitRangeException exception =
          WitRangeException.invalidFloatingPoint(type, Double.NaN);

      final String message = exception.getMessage();

      // Type should appear after "for " and before ":"
      final int forIndex = message.indexOf(" for ");
      final int colonIndex = message.indexOf(":", forIndex);
      final String extractedType = message.substring(forIndex + 5, colonIndex);

      assertEquals(type.toString(), extractedType,
          "Type should appear between 'for ' and ':' - type is first format arg: " + message);
    }

    @Test
    @DisplayName("invalidFloatingPoint format arg 1 should be the value")
    void invalidFloatingPointFormatArg1ShouldBeTheValue() {
      // Format: "Invalid floating-point value for %s: %f"
      // Value should appear after the colon
      final WitRangeException exception =
          WitRangeException.invalidFloatingPoint(WitType.createFloat64(), Double.POSITIVE_INFINITY);

      final String message = exception.getMessage();

      // Value representation should appear after the colon
      final int colonIndex = message.indexOf(":");
      final String afterColon = message.substring(colonIndex + 1).trim();

      // Infinity formats as "Infinity"
      assertTrue(afterColon.contains("Infinity"),
          "Value should appear after ':' - value is second format arg: " + message);
    }

    @Test
    @DisplayName("getMessage format min should appear before max in valid range")
    void getMessageFormatMinShouldAppearBeforeMaxInValidRange() {
      // Format: " [Valid range: %s to %s]"
      // Args: minValue (arg 0), maxValue (arg 1)
      final WitRangeException exception =
          new WitRangeException("Test", WitType.createU8(), 300, 10, 250);

      final String message = exception.getMessage();

      // Find the valid range part
      final int rangeStart = message.indexOf("Valid range:");
      assertTrue(rangeStart >= 0, "Should contain 'Valid range:'");

      // Extract the range part
      final String rangeSubstring = message.substring(rangeStart);

      // Min (10) should appear before "to" and max (250) after
      final int minIndex = rangeSubstring.indexOf("10");
      final int toIndex = rangeSubstring.indexOf(" to ");
      final int maxIndex = rangeSubstring.indexOf("250");

      assertTrue(minIndex >= 0, "Should contain min value 10");
      assertTrue(maxIndex >= 0, "Should contain max value 250");
      assertTrue(minIndex < toIndex, "Min should appear before 'to': " + rangeSubstring);
      assertTrue(maxIndex > toIndex, "Max should appear after 'to': " + rangeSubstring);
    }

    @Test
    @DisplayName("unsignedOverflow with unique values verifies exact positions")
    void unsignedOverflowWithUniqueValuesVerifiesExactPositions() {
      // Use unique values to ensure we can verify positions
      final WitRangeException exception =
          WitRangeException.unsignedOverflow(WitType.createU16(), 12345, 65535);

      final String message = exception.getMessage();

      // Format: "Value %d exceeds maximum for unsigned %s: %d"
      // Expected: "Value 12345 exceeds maximum for unsigned u16: 65535"
      final int value12345Index = message.indexOf("12345");
      final int value65535Index = message.indexOf("65535");

      assertTrue(value12345Index >= 0, "Message should contain 12345");
      assertTrue(value65535Index >= 0, "Message should contain 65535");
      assertTrue(value12345Index < value65535Index,
          "Value 12345 should appear before maxValue 65535: " + message);
    }

    @Test
    @DisplayName("signedOverflow with unique values verifies exact positions")
    void signedOverflowWithUniqueValuesVerifiesExactPositions() {
      // Use unique values: value=99999, min=-50000, max=50000
      final WitRangeException exception =
          WitRangeException.signedOverflow(WitType.createS32(), 99999, -50000, 50000);

      final String message = exception.getMessage();

      // Format: "Value %d out of range for %s [%d, %d]"
      final int value99999Index = message.indexOf("99999");
      final int minus50000Index = message.indexOf("-50000");
      final int plus50000Index = message.indexOf("50000");

      assertTrue(value99999Index >= 0, "Message should contain 99999");
      assertTrue(minus50000Index >= 0, "Message should contain -50000");

      // Value should come first
      assertTrue(value99999Index < minus50000Index,
          "Value 99999 should appear before min -50000: " + message);

      // In the bracket part, min should come before max
      // [-50000, 50000]
      assertTrue(message.contains("[-50000, 50000]"),
          "Bounds should be in order [-50000, 50000]: " + message);
    }
  }
}
