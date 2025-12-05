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

package ai.tegmentum.wasmtime4j.wasi.crypto;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Exception thrown when cryptographic operations fail in WASI-crypto.
 *
 * @since 1.0.0
 */
public class CryptoException extends WasmException {

  private static final long serialVersionUID = 1L;

  private final CryptoErrorCode errorCode;

  /**
   * Creates a new crypto exception with the specified message.
   *
   * @param message the error message
   */
  public CryptoException(final String message) {
    super(message);
    this.errorCode = CryptoErrorCode.UNKNOWN;
  }

  /**
   * Creates a new crypto exception with the specified message and error code.
   *
   * @param message the error message
   * @param errorCode the crypto error code
   */
  public CryptoException(final String message, final CryptoErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Creates a new crypto exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public CryptoException(final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = CryptoErrorCode.UNKNOWN;
  }

  /**
   * Creates a new crypto exception with the specified message, error code, and cause.
   *
   * @param message the error message
   * @param errorCode the crypto error code
   * @param cause the underlying cause
   */
  public CryptoException(
      final String message, final CryptoErrorCode errorCode, final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * Gets the crypto error code.
   *
   * @return the error code
   */
  public CryptoErrorCode getErrorCode() {
    return errorCode;
  }
}
