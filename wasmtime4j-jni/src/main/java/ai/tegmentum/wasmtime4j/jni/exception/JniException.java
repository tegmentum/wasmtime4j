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
package ai.tegmentum.wasmtime4j.jni.exception;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Base class for all JNI-specific WebAssembly exceptions.
 *
 * <p>This exception class extends the general {@link WasmException} and serves as the base for all
 * JNI implementation-specific errors. It provides additional context and functionality specific to
 * JNI operations.
 *
 * <p>JNI exceptions typically occur during:
 *
 * <ul>
 *   <li>Native library loading failures
 *   <li>JNI method invocation errors
 *   <li>Native resource allocation failures
 *   <li>Parameter validation errors before native calls
 *   <li>Native-to-Java error translation
 * </ul>
 *
 * @since 1.0.0
 */
public class JniException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** The native error code if available. */
  private final Integer nativeErrorCode;

  /**
   * Creates a new JNI exception with the specified message.
   *
   * @param message the error message
   */
  public JniException(final String message) {
    super(message);
    this.nativeErrorCode = null;
  }

  /**
   * Creates a new JNI exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public JniException(final String message, final Throwable cause) {
    super(message, cause);
    this.nativeErrorCode = null;
  }

  /**
   * Creates a new JNI exception with the specified message and native error code.
   *
   * @param message the error message
   * @param nativeErrorCode the native error code from the JNI layer
   */
  public JniException(final String message, final int nativeErrorCode) {
    super(message);
    this.nativeErrorCode = nativeErrorCode;
  }

  /**
   * Creates a new JNI exception with the specified message, cause, and native error code.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param nativeErrorCode the native error code from the JNI layer
   */
  public JniException(final String message, final Throwable cause, final int nativeErrorCode) {
    super(message, cause);
    this.nativeErrorCode = nativeErrorCode;
  }

  /**
   * Gets the native error code associated with this exception.
   *
   * @return the native error code, or null if not available
   */
  public Integer getNativeErrorCode() {
    return nativeErrorCode;
  }

  /**
   * Checks if this exception has a native error code.
   *
   * @return true if a native error code is available, false otherwise
   */
  public boolean hasNativeErrorCode() {
    return nativeErrorCode != null;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(super.toString());
    if (hasNativeErrorCode()) {
      sb.append(" (native error code: ").append(nativeErrorCode).append(")");
    }
    return sb.toString();
  }
}
