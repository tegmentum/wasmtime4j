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

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Base exception for Panama FFI implementation errors.
 *
 * <p>This is the root exception class for all Panama-specific errors in the WebAssembly runtime. It
 * provides a common base for exception handling across the Panama implementation, and extends
 * {@link WasmException} for consistency with the JNI implementation.
 *
 * @since 1.0.0
 */
public class PanamaException extends WasmException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new Panama exception with the specified message.
   *
   * @param message the detail message
   */
  public PanamaException(final String message) {
    super(message);
  }

  /**
   * Creates a new Panama exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  public PanamaException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new Panama exception with the specified cause.
   *
   * @param cause the underlying cause
   */
  public PanamaException(final Throwable cause) {
    super(cause);
  }
}
