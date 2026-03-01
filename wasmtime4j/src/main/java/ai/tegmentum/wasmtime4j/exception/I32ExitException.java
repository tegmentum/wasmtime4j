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
 * Exception thrown when a WASI module calls {@code proc_exit(code)}.
 *
 * <p>This exception corresponds to Wasmtime's {@code wasmtime_wasi::I32Exit} error. When a
 * WebAssembly component or module invokes the WASI {@code proc_exit} function, Wasmtime raises an
 * {@code I32Exit} error containing the exit code. This exception surfaces that exit code to Java
 * callers.
 *
 * <p>Unlike other exceptions, an {@code I32ExitException} with exit code 0 typically indicates
 * successful program completion rather than an error condition. Callers should check {@link
 * #getExitCode()} to distinguish between success and failure:
 *
 * <pre>{@code
 * try {
 *     instance.callMain();
 * } catch (I32ExitException e) {
 *     if (e.getExitCode() == 0) {
 *         System.out.println("Program exited successfully");
 *     } else {
 *         System.err.println("Program exited with code: " + e.getExitCode());
 *     }
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public class I32ExitException extends WasiException {

  private static final long serialVersionUID = 1L;

  /** The WASI exit code from {@code proc_exit}. */
  private final int exitCode;

  /**
   * Creates a new I32ExitException with the specified exit code.
   *
   * @param exitCode the exit code passed to {@code proc_exit}
   */
  public I32ExitException(final int exitCode) {
    super(
        "WASI proc_exit called with code " + exitCode,
        "proc_exit",
        null,
        false,
        ErrorCategory.SYSTEM);
    this.exitCode = exitCode;
  }

  /**
   * Creates a new I32ExitException with the specified exit code and cause.
   *
   * @param exitCode the exit code passed to {@code proc_exit}
   * @param cause the underlying cause
   */
  public I32ExitException(final int exitCode, final Throwable cause) {
    super(
        "WASI proc_exit called with code " + exitCode,
        "proc_exit",
        null,
        false,
        ErrorCategory.SYSTEM,
        cause);
    this.exitCode = exitCode;
  }

  /**
   * Gets the exit code that was passed to WASI {@code proc_exit}.
   *
   * <p>An exit code of 0 conventionally indicates success, while non-zero values indicate failure.
   *
   * @return the exit code
   */
  public int getExitCode() {
    return exitCode;
  }

  /**
   * Checks if the exit code indicates success (exit code 0).
   *
   * @return true if the exit code is 0
   */
  public boolean isSuccess() {
    return exitCode == 0;
  }
}
