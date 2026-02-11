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

import ai.tegmentum.wasmtime4j.wit.WitType;
import java.util.Optional;

/**
 * Base exception for WebAssembly Interface Type (WIT) value operations.
 *
 * <p>This exception is thrown when WIT value creation, validation, or conversion fails. It provides
 * structured error information including error codes, expected types, and actual values for
 * debugging.
 *
 * @since 1.0.0
 */
public class WitValueException extends WasmException {

  private static final long serialVersionUID = 1L;

  private final ErrorCode code;
  private final transient Optional<WitType> expectedType;
  private final transient Optional<Object> actualValue;

  /**
   * Error codes for WIT value exceptions.
   *
   * @since 1.0.0
   */
  public enum ErrorCode {
    /** Type mismatch between expected and actual types. */
    TYPE_MISMATCH,

    /** Value out of valid range for the target type. */
    RANGE_ERROR,

    /** Invalid value format or encoding. */
    INVALID_FORMAT,

    /** Marshalling operation failed. */
    MARSHALLING_ERROR,

    /** Validation constraint violated. */
    VALIDATION_ERROR,

    /** Null value where non-null required. */
    NULL_VALUE,

    /** Unsupported operation for this value type. */
    UNSUPPORTED_OPERATION
  }

  /**
   * Creates a new WIT value exception with the specified message and error code.
   *
   * @param message the error message
   * @param code the error code
   */
  public WitValueException(final String message, final ErrorCode code) {
    super(message);
    this.code = code;
    this.expectedType = Optional.empty();
    this.actualValue = Optional.empty();
  }

  /**
   * Creates a new WIT value exception with message, error code, and cause.
   *
   * @param message the error message
   * @param code the error code
   * @param cause the cause of this exception
   */
  public WitValueException(final String message, final ErrorCode code, final Throwable cause) {
    super(message, cause);
    this.code = code;
    this.expectedType = Optional.empty();
    this.actualValue = Optional.empty();
  }

  /**
   * Creates a new WIT value exception with type information.
   *
   * @param message the error message
   * @param code the error code
   * @param expectedType the expected type
   * @param actualValue the actual value that caused the error
   */
  public WitValueException(
      final String message,
      final ErrorCode code,
      final WitType expectedType,
      final Object actualValue) {
    super(message);
    this.code = code;
    this.expectedType = Optional.ofNullable(expectedType);
    this.actualValue = Optional.ofNullable(actualValue);
  }

  /**
   * Gets the error code for this exception.
   *
   * @return the error code
   */
  public ErrorCode getCode() {
    return code;
  }

  /**
   * Gets the expected type, if applicable.
   *
   * @return the expected type, or empty if not applicable
   */
  public Optional<WitType> getExpectedType() {
    return expectedType;
  }

  /**
   * Gets the actual value that caused the error, if applicable.
   *
   * @return the actual value, or empty if not applicable
   */
  public Optional<Object> getActualValue() {
    return actualValue;
  }

  @Override
  public String getMessage() {
    final StringBuilder sb = new StringBuilder(super.getMessage());

    if (expectedType.isPresent()) {
      sb.append(" [Expected type: ").append(expectedType.get()).append("]");
    }

    if (actualValue.isPresent()) {
      sb.append(" [Actual value: ").append(actualValue.get()).append("]");
    }

    sb.append(" [Error code: ").append(code).append("]");

    return sb.toString();
  }
}
