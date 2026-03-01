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
 * Exception thrown when a WebAssembly type error occurs.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>Type mismatch between expected and actual value types
 *   <li>Invalid type conversion attempts
 *   <li>Incompatible function signature calls
 *   <li>Table or memory type mismatches
 *   <li>Global type constraint violations
 * </ul>
 *
 * @since 1.0.0
 */
public class WasmTypeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new WebAssembly type exception with the specified message.
   *
   * @param message the error message
   */
  public WasmTypeException(final String message) {
    super(message);
  }

  /**
   * Creates a new WebAssembly type exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasmTypeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
