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
 * Exception thrown for WebAssembly runtime errors.
 *
 * <p>This exception is thrown for general runtime failures including memory operations, function
 * calls, type mismatches, component errors, interface errors, engine configuration errors, store
 * errors, concurrency errors, and internal errors.
 *
 * @since 1.0.0
 */
public class WasmRuntimeException extends WasmException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new runtime exception with the specified message.
   *
   * @param message the error message describing the runtime failure
   */
  public WasmRuntimeException(final String message) {
    super(message);
  }

  /**
   * Creates a new runtime exception with the specified message and cause.
   *
   * @param message the error message describing the runtime failure
   * @param cause the underlying cause
   */
  public WasmRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
