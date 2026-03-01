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
package ai.tegmentum.wasmtime4j.wasi.nn;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Exception thrown when a WASI-NN operation fails.
 *
 * <p>This exception wraps errors from the WASI-NN implementation, providing error codes and
 * detailed messages about the failure.
 *
 * @since 1.0.0
 */
public class NnException extends WasmException {

  private static final long serialVersionUID = 1L;

  private final NnErrorCode errorCode;

  /**
   * Creates a new NnException with the given message.
   *
   * @param message the error message
   */
  public NnException(final String message) {
    super(message);
    this.errorCode = NnErrorCode.UNKNOWN;
  }

  /**
   * Creates a new NnException with the given message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public NnException(final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = NnErrorCode.UNKNOWN;
  }

  /**
   * Creates a new NnException with the given error code and message.
   *
   * @param errorCode the WASI-NN error code
   * @param message the error message
   */
  public NnException(final NnErrorCode errorCode, final String message) {
    super(formatMessage(errorCode, message));
    this.errorCode = errorCode != null ? errorCode : NnErrorCode.UNKNOWN;
  }

  /**
   * Creates a new NnException with the given error code, message, and cause.
   *
   * @param errorCode the WASI-NN error code
   * @param message the error message
   * @param cause the underlying cause
   */
  public NnException(final NnErrorCode errorCode, final String message, final Throwable cause) {
    super(formatMessage(errorCode, message), cause);
    this.errorCode = errorCode != null ? errorCode : NnErrorCode.UNKNOWN;
  }

  /**
   * Gets the WASI-NN error code for this exception.
   *
   * @return the error code
   */
  public NnErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Checks if this exception represents an invalid argument error.
   *
   * @return true if the error code is INVALID_ARGUMENT
   */
  public boolean isInvalidArgument() {
    return errorCode == NnErrorCode.INVALID_ARGUMENT;
  }

  /**
   * Checks if this exception represents an invalid encoding error.
   *
   * @return true if the error code is INVALID_ENCODING
   */
  public boolean isInvalidEncoding() {
    return errorCode == NnErrorCode.INVALID_ENCODING;
  }

  /**
   * Checks if this exception represents a timeout error.
   *
   * @return true if the error code is TIMEOUT
   */
  public boolean isTimeout() {
    return errorCode == NnErrorCode.TIMEOUT;
  }

  /**
   * Checks if this exception represents a runtime error.
   *
   * @return true if the error code is RUNTIME_ERROR
   */
  public boolean isRuntimeError() {
    return errorCode == NnErrorCode.RUNTIME_ERROR;
  }

  /**
   * Checks if this exception represents an unsupported operation.
   *
   * @return true if the error code is UNSUPPORTED_OPERATION
   */
  public boolean isUnsupportedOperation() {
    return errorCode == NnErrorCode.UNSUPPORTED_OPERATION;
  }

  /**
   * Checks if this exception represents a resource too large error.
   *
   * @return true if the error code is TOO_LARGE
   */
  public boolean isTooLarge() {
    return errorCode == NnErrorCode.TOO_LARGE;
  }

  /**
   * Checks if this exception represents a not found error.
   *
   * @return true if the error code is NOT_FOUND
   */
  public boolean isNotFound() {
    return errorCode == NnErrorCode.NOT_FOUND;
  }

  /**
   * Checks if this exception represents a security error.
   *
   * @return true if the error code is SECURITY
   */
  public boolean isSecurity() {
    return errorCode == NnErrorCode.SECURITY;
  }

  /**
   * Creates an NnException for an invalid argument.
   *
   * @param message the error message
   * @return a new NnException
   */
  public static NnException invalidArgument(final String message) {
    return new NnException(NnErrorCode.INVALID_ARGUMENT, message);
  }

  /**
   * Creates an NnException for an invalid encoding.
   *
   * @param message the error message
   * @return a new NnException
   */
  public static NnException invalidEncoding(final String message) {
    return new NnException(NnErrorCode.INVALID_ENCODING, message);
  }

  /**
   * Creates an NnException for a timeout.
   *
   * @param message the error message
   * @return a new NnException
   */
  public static NnException timeout(final String message) {
    return new NnException(NnErrorCode.TIMEOUT, message);
  }

  /**
   * Creates an NnException for a runtime error.
   *
   * @param message the error message
   * @return a new NnException
   */
  public static NnException runtimeError(final String message) {
    return new NnException(NnErrorCode.RUNTIME_ERROR, message);
  }

  /**
   * Creates an NnException for a runtime error with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @return a new NnException
   */
  public static NnException runtimeError(final String message, final Throwable cause) {
    return new NnException(NnErrorCode.RUNTIME_ERROR, message, cause);
  }

  /**
   * Creates an NnException for an unsupported operation.
   *
   * @param message the error message
   * @return a new NnException
   */
  public static NnException unsupportedOperation(final String message) {
    return new NnException(NnErrorCode.UNSUPPORTED_OPERATION, message);
  }

  /**
   * Creates an NnException for a resource too large error.
   *
   * @param message the error message
   * @return a new NnException
   */
  public static NnException tooLarge(final String message) {
    return new NnException(NnErrorCode.TOO_LARGE, message);
  }

  /**
   * Creates an NnException for a not found error.
   *
   * @param message the error message
   * @return a new NnException
   */
  public static NnException notFound(final String message) {
    return new NnException(NnErrorCode.NOT_FOUND, message);
  }

  /**
   * Creates an NnException for a security error.
   *
   * @param message the error message
   * @return a new NnException
   */
  public static NnException security(final String message) {
    return new NnException(NnErrorCode.SECURITY, message);
  }

  /**
   * Creates an NnException from a native error code and message.
   *
   * @param nativeCode the native error code
   * @param message the error message
   * @return a new NnException
   */
  public static NnException fromNativeError(final int nativeCode, final String message) {
    return new NnException(NnErrorCode.fromNativeCode(nativeCode), message);
  }

  private static String formatMessage(final NnErrorCode code, final String message) {
    if (code == null || code == NnErrorCode.UNKNOWN) {
      return message;
    }
    return "[" + code.getWasiName() + "] " + message;
  }
}
