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
 * Exception thrown when there are issues with native library loading or initialization in the Panama FFI implementation.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>Native library cannot be found or loaded through Panama FFI
 *   <li>Native library is incompatible with the current platform or Panama version
 *   <li>Native library fails to initialize properly with Panama FFI
 *   <li>Required native functions are missing from the library
 *   <li>MethodHandle creation fails for native functions
 *   <li>Arena allocation fails during library initialization
 * </ul>
 *
 * <p>Panama-specific failure scenarios include:
 * <ul>
 *   <li>SymbolLookup failures for native functions
 *   <li>FunctionDescriptor mismatches
 *   <li>Memory layout incompatibilities
 *   <li>Foreign linker initialization failures
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaLibraryException extends PanamaException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new Panama library exception with the specified message.
   *
   * @param message the error message
   */
  public PanamaLibraryException(final String message) {
    super(message);
  }

  /**
   * Creates a new Panama library exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public PanamaLibraryException(final String message, final Throwable cause) {
    super(message, cause);
  }
}