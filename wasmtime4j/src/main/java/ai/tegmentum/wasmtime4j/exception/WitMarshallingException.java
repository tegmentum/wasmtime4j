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

/**
 * Exception thrown when marshalling between Java and WebAssembly Interface Types fails.
 *
 * <p>This exception is used for errors during bidirectional conversion between Java objects and WIT
 * values, including serialization to native memory and deserialization from native memory.
 *
 * @since 1.0.0
 */
public class WitMarshallingException extends WitValueException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new marshalling exception with the specified message.
   *
   * @param message the error message
   */
  public WitMarshallingException(final String message) {
    super(message, ErrorCode.MARSHALLING_ERROR);
  }

  /**
   * Creates a new marshalling exception with message and cause.
   *
   * @param message the error message
   * @param cause the cause of this exception
   */
  public WitMarshallingException(final String message, final Throwable cause) {
    super(message, ErrorCode.MARSHALLING_ERROR, cause);
  }

  /**
   * Creates a new marshalling exception with type information.
   *
   * @param message the error message
   * @param expectedType the expected WIT type
   * @param actualValue the actual Java value that failed to marshal
   */
  public WitMarshallingException(
      final String message, final WitType expectedType, final Object actualValue) {
    super(message, ErrorCode.MARSHALLING_ERROR, expectedType, actualValue);
  }

  /**
   * Creates a marshalling exception for null values where non-null is required.
   *
   * @param expectedType the expected WIT type
   * @return a new marshalling exception
   */
  public static WitMarshallingException nullValue(final WitType expectedType) {
    return new WitMarshallingException(
        "Null value provided for non-optional type", expectedType, null);
  }

  /**
   * Creates a marshalling exception for type mismatches.
   *
   * @param expectedType the expected WIT type
   * @param actualValue the actual Java value
   * @return a new marshalling exception
   */
  public static WitMarshallingException typeMismatch(
      final WitType expectedType, final Object actualValue) {
    final String actualTypeName =
        actualValue == null ? "null" : actualValue.getClass().getSimpleName();
    return new WitMarshallingException(
        String.format("Cannot marshal Java type %s to WIT type %s", actualTypeName, expectedType),
        expectedType,
        actualValue);
  }

  /**
   * Creates a marshalling exception for native memory allocation failures.
   *
   * @param type the WIT type being marshalled
   * @param cause the underlying cause
   * @return a new marshalling exception
   */
  public static WitMarshallingException allocationFailure(
      final WitType type, final Throwable cause) {
    return new WitMarshallingException(
        String.format("Failed to allocate native memory for WIT type %s", type), cause);
  }

  /**
   * Creates a marshalling exception for native memory read failures.
   *
   * @param type the WIT type being unmarshalled
   * @param cause the underlying cause
   * @return a new marshalling exception
   */
  public static WitMarshallingException readFailure(final WitType type, final Throwable cause) {
    return new WitMarshallingException(
        String.format("Failed to read native memory for WIT type %s", type), cause);
  }
}
