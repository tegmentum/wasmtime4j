/*
 * Copyright 2024 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.WitType;

/**
 * Exception thrown when a value is out of range for the target WIT type.
 *
 * <p>This exception is specifically for numeric range violations, overflow/underflow conditions,
 * and boundary constraint failures during WIT value creation or conversion.
 *
 * @since 1.0.0
 */
public class WitRangeException extends WitValueException {

  private static final long serialVersionUID = 1L;

  private final Number minValue;
  private final Number maxValue;

  /**
   * Creates a new range exception with the specified message.
   *
   * @param message the error message
   */
  public WitRangeException(final String message) {
    super(message, ErrorCode.RANGE_ERROR);
    this.minValue = null;
    this.maxValue = null;
  }

  /**
   * Creates a new range exception with message and type information.
   *
   * @param message the error message
   * @param expectedType the expected WIT type
   * @param actualValue the actual value that was out of range
   */
  public WitRangeException(
      final String message, final WitType expectedType, final Object actualValue) {
    super(message, ErrorCode.RANGE_ERROR, expectedType, actualValue);
    this.minValue = null;
    this.maxValue = null;
  }

  /**
   * Creates a new range exception with full range information.
   *
   * @param message the error message
   * @param expectedType the expected WIT type
   * @param actualValue the actual value that was out of range
   * @param minValue the minimum allowed value
   * @param maxValue the maximum allowed value
   */
  public WitRangeException(
      final String message,
      final WitType expectedType,
      final Object actualValue,
      final Number minValue,
      final Number maxValue) {
    super(message, ErrorCode.RANGE_ERROR, expectedType, actualValue);
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  /**
   * Gets the minimum allowed value for this range, if available.
   *
   * @return the minimum value, or null if not applicable
   */
  public Number getMinValue() {
    return minValue;
  }

  /**
   * Gets the maximum allowed value for this range, if available.
   *
   * @return the maximum value, or null if not applicable
   */
  public Number getMaxValue() {
    return maxValue;
  }

  /**
   * Creates a range exception for unsigned integer overflow.
   *
   * @param type the WIT type
   * @param value the value that overflowed
   * @param maxValue the maximum allowed value
   * @return a new range exception
   */
  public static WitRangeException unsignedOverflow(
      final WitType type, final long value, final long maxValue) {
    return new WitRangeException(
        String.format("Value %d exceeds maximum for unsigned %s: %d", value, type, maxValue),
        type,
        value,
        0L,
        maxValue);
  }

  /**
   * Creates a range exception for negative values in unsigned types.
   *
   * @param type the WIT type
   * @param value the negative value
   * @return a new range exception
   */
  public static WitRangeException negativeUnsigned(final WitType type, final long value) {
    return new WitRangeException(
        String.format("Negative value %d not allowed for unsigned %s", value, type), type, value);
  }

  /**
   * Creates a range exception for signed integer overflow.
   *
   * @param type the WIT type
   * @param value the value that overflowed
   * @param minValue the minimum allowed value
   * @param maxValue the maximum allowed value
   * @return a new range exception
   */
  public static WitRangeException signedOverflow(
      final WitType type, final long value, final long minValue, final long maxValue) {
    return new WitRangeException(
        String.format("Value %d out of range for %s [%d, %d]", value, type, minValue, maxValue),
        type,
        value,
        minValue,
        maxValue);
  }

  /**
   * Creates a range exception for floating-point special values (NaN, infinity).
   *
   * @param type the WIT type
   * @param value the special floating-point value
   * @return a new range exception
   */
  public static WitRangeException invalidFloatingPoint(final WitType type, final double value) {
    return new WitRangeException(
        String.format("Invalid floating-point value for %s: %f", type, value), type, value);
  }

  /**
   * Creates a range exception for character codepoint out of valid Unicode range.
   *
   * @param codepoint the invalid codepoint
   * @return a new range exception
   */
  public static WitRangeException invalidCodepoint(final int codepoint) {
    return new WitRangeException(
        String.format("Codepoint 0x%X is not a valid Unicode scalar value", codepoint),
        WitType.createChar(),
        codepoint,
        0,
        0x10FFFF);
  }

  @Override
  public String getMessage() {
    final StringBuilder sb = new StringBuilder(super.getMessage());

    if (minValue != null && maxValue != null) {
      sb.append(String.format(" [Valid range: %s to %s]", minValue, maxValue));
    }

    return sb.toString();
  }
}
