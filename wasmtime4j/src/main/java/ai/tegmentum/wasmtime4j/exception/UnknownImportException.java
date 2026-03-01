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
 * Exception thrown when a WebAssembly module references an import that cannot be resolved.
 *
 * <p>This exception is thrown during instantiation when a module's import declaration does not
 * match any available export from linked modules or host functions.
 *
 * @since 1.1.0
 */
public class UnknownImportException extends LinkingException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new unknown import exception with the specified message.
   *
   * @param message the error message describing the unknown import
   */
  public UnknownImportException(final String message) {
    super(LinkingErrorType.IMPORT_NOT_FOUND, message);
  }

  /**
   * Creates a new unknown import exception with the specified message and cause.
   *
   * @param message the error message describing the unknown import
   * @param cause the underlying cause
   */
  public UnknownImportException(final String message, final Throwable cause) {
    super(LinkingErrorType.IMPORT_NOT_FOUND, message, cause);
  }
}
