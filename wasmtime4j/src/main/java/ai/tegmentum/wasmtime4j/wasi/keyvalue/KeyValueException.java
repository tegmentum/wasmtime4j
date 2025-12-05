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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Exception thrown when key-value operations fail in WASI-keyvalue.
 *
 * @since 1.0.0
 */
public class KeyValueException extends WasmException {

  private static final long serialVersionUID = 1L;

  private final KeyValueErrorCode errorCode;

  /**
   * Creates a new key-value exception with the specified message.
   *
   * @param message the error message
   */
  public KeyValueException(final String message) {
    super(message);
    this.errorCode = KeyValueErrorCode.UNKNOWN;
  }

  /**
   * Creates a new key-value exception with the specified message and error code.
   *
   * @param message the error message
   * @param errorCode the error code
   */
  public KeyValueException(final String message, final KeyValueErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Creates a new key-value exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public KeyValueException(final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = KeyValueErrorCode.UNKNOWN;
  }

  /**
   * Creates a new key-value exception with the specified message, error code, and cause.
   *
   * @param message the error message
   * @param errorCode the error code
   * @param cause the underlying cause
   */
  public KeyValueException(
      final String message, final KeyValueErrorCode errorCode, final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * Gets the key-value error code.
   *
   * @return the error code
   */
  public KeyValueErrorCode getErrorCode() {
    return errorCode;
  }
}
