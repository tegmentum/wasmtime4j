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

package ai.tegmentum.wasmtime4j.wasi.io;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * WASI Preview 2 stream error.
 *
 * <p>Represents errors that occur during stream operations. Stream errors can indicate operation
 * failures or stream closure.
 *
 * <p>This corresponds to the wasi:io/streams.stream-error variant from the WASI Preview 2
 * specification.
 *
 * @since 1.0.0
 */
public final class WasiStreamError extends WasmException {

  private static final long serialVersionUID = 1L;

  private final ErrorType errorType;
  private final transient Optional<Object> errorDetails;

  /**
   * Type of stream error.
   *
   * @since 1.0.0
   */
  public enum ErrorType {
    /** Operation failed before completion with an error. */
    LAST_OPERATION_FAILED,

    /** Stream is closed and no more operations are possible. */
    CLOSED
  }

  /**
   * Creates a stream error indicating operation failure.
   *
   * @param message error message
   * @param errorDetails optional error details from the underlying operation
   */
  private WasiStreamError(
      final String message, final ErrorType errorType, final Object errorDetails) {
    super(message);
    this.errorType = errorType;
    this.errorDetails = Optional.ofNullable(errorDetails);
  }

  /**
   * Creates a stream error indicating the last operation failed.
   *
   * @param message error message
   * @param errorDetails optional error details
   * @return stream error
   */
  public static WasiStreamError lastOperationFailed(
      final String message, final Object errorDetails) {
    return new WasiStreamError(message, ErrorType.LAST_OPERATION_FAILED, errorDetails);
  }

  /**
   * Creates a stream error indicating the last operation failed.
   *
   * @param message error message
   * @return stream error
   */
  public static WasiStreamError lastOperationFailed(final String message) {
    return new WasiStreamError(message, ErrorType.LAST_OPERATION_FAILED, null);
  }

  /**
   * Creates a stream error indicating the stream is closed.
   *
   * @param message error message
   * @return stream error
   */
  public static WasiStreamError closed(final String message) {
    return new WasiStreamError(message, ErrorType.CLOSED, null);
  }

  /**
   * Creates a stream error indicating the stream is closed.
   *
   * @return stream error with default message
   */
  public static WasiStreamError closed() {
    return new WasiStreamError("Stream is closed", ErrorType.CLOSED, null);
  }

  /**
   * Gets the type of stream error.
   *
   * @return error type
   */
  public ErrorType getErrorType() {
    return errorType;
  }

  /**
   * Gets error details if available.
   *
   * @return optional error details from the underlying operation
   */
  public Optional<Object> getErrorDetails() {
    return errorDetails;
  }

  /**
   * Checks if this error indicates a closed stream.
   *
   * @return true if the stream is closed
   */
  public boolean isClosed() {
    return errorType == ErrorType.CLOSED;
  }

  /**
   * Checks if this error indicates an operation failure.
   *
   * @return true if the last operation failed
   */
  public boolean isOperationFailed() {
    return errorType == ErrorType.LAST_OPERATION_FAILED;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("WasiStreamError{");
    sb.append("errorType=").append(errorType);
    sb.append(", message='").append(getMessage()).append('\'');
    errorDetails.ifPresent(details -> sb.append(", errorDetails=").append(details));
    sb.append('}');
    return sb.toString();
  }
}
