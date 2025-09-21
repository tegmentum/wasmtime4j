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

package ai.tegmentum.wasmtime4j.panama.exception;

/**
 * Exception thrown when there are issues with native resource management in the Panama FFI implementation.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>Arena resource allocation fails
 *   <li>MemorySegment operations fail
 *   <li>Native resource is accessed after Arena is closed
 *   <li>Arena resource limits are exceeded
 *   <li>Memory management operations fail in Panama FFI
 *   <li>Resource cleanup fails during Arena closure
 * </ul>
 *
 * <p>Panama-specific failure scenarios include:
 * <ul>
 *   <li>Arena.ofConfined() or Arena.ofShared() allocation failures
 *   <li>MemorySegment reinterpretation failures
 *   <li>Invalid memory access through MemorySegment
 *   <li>Arena scope violations
 *   <li>Native pointer invalidation
 *   <li>Memory layout alignment issues
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaResourceException extends PanamaException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new Panama resource exception with the specified message.
   *
   * @param message the error message
   */
  public PanamaResourceException(final String message) {
    super(message);
  }

  /**
   * Creates a new Panama resource exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public PanamaResourceException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new Panama resource exception with the specified message and native error code.
   *
   * @param message the error message
   * @param nativeErrorCode the native error code
   */
  public PanamaResourceException(final String message, final int nativeErrorCode) {
    super(message, nativeErrorCode);
  }

  /**
   * Creates a new Panama resource exception with all parameters.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param nativeErrorCode the native error code
   */
  public PanamaResourceException(
      final String message, final Throwable cause, final int nativeErrorCode) {
    super(message, cause, nativeErrorCode);
  }
}