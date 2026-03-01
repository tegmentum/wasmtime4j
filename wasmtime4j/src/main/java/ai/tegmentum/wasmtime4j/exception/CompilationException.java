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
 * Exception thrown when WebAssembly compilation fails.
 *
 * <p>This exception is thrown when WebAssembly bytecode cannot be compiled due to validation
 * errors, unsupported features, or other compilation issues.
 *
 * @since 1.0.0
 */
public class CompilationException extends WasmException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new compilation exception with the specified message.
   *
   * @param message the error message describing the compilation failure
   */
  public CompilationException(final String message) {
    super(message);
  }

  /**
   * Creates a new compilation exception with the specified message and cause.
   *
   * @param message the error message describing the compilation failure
   * @param cause the underlying cause
   */
  public CompilationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
