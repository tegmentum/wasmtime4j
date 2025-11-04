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
 * Exception thrown when WIT value validation fails.
 *
 * <p>This exception is used for constraint violations, format errors, and other validation failures
 * that occur during WIT value creation or manipulation.
 *
 * @since 1.0.0
 */
public class WitValidationException extends WitValueException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new validation exception with the specified message.
   *
   * @param message the error message
   */
  public WitValidationException(final String message) {
    super(message, ErrorCode.VALIDATION_ERROR);
  }

  /**
   * Creates a new validation exception with message and cause.
   *
   * @param message the error message
   * @param cause the cause of this exception
   */
  public WitValidationException(final String message, final Throwable cause) {
    super(message, ErrorCode.VALIDATION_ERROR, cause);
  }

  /**
   * Creates a new validation exception with type information.
   *
   * @param message the error message
   * @param expectedType the expected WIT type
   * @param actualValue the actual value that failed validation
   */
  public WitValidationException(
      final String message, final WitType expectedType, final Object actualValue) {
    super(message, ErrorCode.VALIDATION_ERROR, expectedType, actualValue);
  }

  /**
   * Creates a validation exception for invalid string encoding.
   *
   * @param value the invalid string value
   * @return a new validation exception
   */
  public static WitValidationException invalidEncoding(final String value) {
    return new WitValidationException(
        String.format("String contains invalid UTF-8 encoding: %s", value));
  }

  /**
   * Creates a validation exception for invalid character values.
   *
   * @param codepoint the invalid Unicode codepoint
   * @return a new validation exception
   */
  public static WitValidationException invalidChar(final int codepoint) {
    return new WitValidationException(
        String.format("Invalid Unicode codepoint for char type: 0x%X", codepoint));
  }

  /**
   * Creates a validation exception for constraint violations.
   *
   * @param constraint the constraint that was violated
   * @param value the value that violated the constraint
   * @return a new validation exception
   */
  public static WitValidationException constraintViolation(
      final String constraint, final Object value) {
    return new WitValidationException(
        String.format("Value violates constraint '%s': %s", constraint, value));
  }

  /**
   * Creates a validation exception for invalid format.
   *
   * @param expectedFormat the expected format
   * @param actualValue the value with invalid format
   * @return a new validation exception
   */
  public static WitValidationException invalidFormat(
      final String expectedFormat, final Object actualValue) {
    return new WitValidationException(
        String.format("Invalid format, expected %s but got: %s", expectedFormat, actualValue));
  }
}
