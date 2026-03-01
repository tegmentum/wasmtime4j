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
package ai.tegmentum.wasmtime4j.exception;

/**
 * Base class for all WebAssembly-related exceptions.
 *
 * <p>This is the root exception class for all errors that can occur during WebAssembly operations,
 * including compilation, instantiation, and execution.
 *
 * @since 1.0.0
 */
public class WasmException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new WebAssembly exception with the specified message.
   *
   * @param message the error message
   */
  public WasmException(final String message) {
    super(message);
  }

  /**
   * Creates a new WebAssembly exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasmException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new WebAssembly exception with the specified cause.
   *
   * @param cause the underlying cause
   */
  public WasmException(final Throwable cause) {
    super(cause);
  }
}
