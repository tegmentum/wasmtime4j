/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wast;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface for executing WAST (WebAssembly Test) files.
 *
 * <p>WAST files are the standard test format used by the WebAssembly specification and contain
 * module definitions along with test assertions (e.g., assert_return, assert_trap,
 * assert_invalid).
 *
 * <p>Implementations use Wasmtime's built-in WAST parser and test runner to ensure 100%
 * compatibility with Wasmtime's own test behavior.
 *
 * <p>Use {@link #create()} to obtain a runtime-appropriate implementation.
 *
 * <h2>Example Usage:</h2>
 *
 * <pre>{@code
 * WastRunner runner = WastRunner.create();
 *
 * // Execute a WAST file
 * WastExecutionResult result = runner.executeWastFile("tests/test.wast");
 * if (result.allPassed()) {
 *     System.out.println("All tests passed!");
 * }
 *
 * // Execute WAST content from a string
 * WastExecutionResult result = runner.executeWastString("inline.wast", wastContent);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WastRunner {

  /**
   * Executes a WAST file from disk.
   *
   * @param filePath the path to the WAST file to execute
   * @return the execution results containing pass/fail information
   * @throws WasmException if the native execution fails catastrophically
   * @throws IllegalArgumentException if filePath is null or empty
   */
  WastExecutionResult executeWastFile(String filePath) throws WasmException;

  /**
   * Executes a WAST file from a Path object.
   *
   * @param path the path to the WAST file
   * @return the execution results
   * @throws WasmException if the native execution fails
   * @throws IllegalArgumentException if path is null
   */
  default WastExecutionResult executeWastFile(final Path path) throws WasmException {
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }
    return executeWastFile(path.toAbsolutePath().toString());
  }

  /**
   * Executes WAST content from a string.
   *
   * @param filename the filename to use for error reporting (not a real file)
   * @param wastContent the WAST content to execute
   * @return the execution results
   * @throws WasmException if the native execution fails
   * @throws IllegalArgumentException if filename or wastContent is null or empty
   */
  WastExecutionResult executeWastString(String filename, String wastContent) throws WasmException;

  /**
   * Executes WAST content from a byte array.
   *
   * @param filename the filename to use for error reporting
   * @param wastContent the WAST content as UTF-8 encoded bytes
   * @return the execution results
   * @throws WasmException if the native execution fails
   * @throws IllegalArgumentException if filename or wastContent is null
   */
  WastExecutionResult executeWastBytes(String filename, byte[] wastContent) throws WasmException;

  /**
   * Executes a WAST file and throws if any directives fail.
   *
   * @param filePath the path to the WAST file
   * @throws WasmException if the native execution fails
   * @throws RuntimeException if any directives fail or execution errors occur
   */
  default void executeWastFileOrThrow(final String filePath) throws WasmException {
    final WastExecutionResult result = executeWastFile(filePath);
    if (!result.allPassed()) {
      throw new WastExecutionException(result);
    }
  }

  /**
   * Executes WAST content from a string and throws if any directives fail.
   *
   * @param filename the filename for error reporting
   * @param wastContent the WAST content
   * @throws WasmException if the native execution fails
   * @throws RuntimeException if any directives fail or execution errors occur
   */
  default void executeWastStringOrThrow(final String filename, final String wastContent)
      throws WasmException {
    final WastExecutionResult result = executeWastString(filename, wastContent);
    if (!result.allPassed()) {
      throw new WastExecutionException(result);
    }
  }

  /**
   * Creates a WastRunner using the appropriate runtime implementation.
   *
   * <p>Selects Panama on Java 23+, JNI otherwise. Can be overridden via the
   * {@code wasmtime4j.runtime} system property.
   *
   * @return a new WastRunner instance
   * @throws IllegalStateException if no WastRunner implementation is available
   */
  static WastRunner create() {
    return WastRunnerFactory.createRunner();
  }

  /** Exception thrown when WAST execution fails. */
  final class WastExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final transient WastExecutionResult result;

    /**
     * Creates a new WAST execution exception.
     *
     * @param result the execution result that failed
     */
    public WastExecutionException(final WastExecutionResult result) {
      super(createMessage(result));
      this.result = result;
    }

    /**
     * Gets the execution result.
     *
     * @return the result
     */
    public WastExecutionResult getResult() {
      return result;
    }

    private static String createMessage(final WastExecutionResult result) {
      if (result.getExecutionError() != null) {
        return String.format(
            "WAST execution failed for %s: %s",
            result.getFilePath(), result.getExecutionError());
      } else {
        return String.format(
            "WAST execution failed for %s: %d of %d directives failed",
            result.getFilePath(), result.getFailedDirectives(), result.getTotalDirectives());
      }
    }
  }
}
