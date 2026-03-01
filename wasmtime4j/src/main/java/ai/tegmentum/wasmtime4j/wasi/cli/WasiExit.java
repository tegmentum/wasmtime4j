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
package ai.tegmentum.wasmtime4j.wasi.cli;

/**
 * WASI Preview 2 exit interface.
 *
 * <p>Provides program termination functionality with exit status codes. This interface corresponds
 * to the wasi:cli/exit from the WASI Preview 2 specification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiExit exit = getExit();
 *
 * // Exit with success
 * exit.exit(0);
 *
 * // Exit with error
 * exit.exit(1);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiExit {

  /** Exit status indicating successful completion. */
  int EXIT_SUCCESS = 0;

  /** Exit status indicating failure. */
  int EXIT_FAILURE = 1;

  /**
   * Terminates the program with the specified exit status code.
   *
   * <p>This function does not return. The exit status code is typically 0 for success or non-zero
   * for failure, following POSIX conventions.
   *
   * @param statusCode the exit status code (0 for success, non-zero for failure)
   */
  void exit(final int statusCode);
}
