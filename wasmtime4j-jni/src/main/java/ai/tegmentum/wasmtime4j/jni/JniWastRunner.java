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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.wast.WastExecutionResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * JNI wrapper for executing WAST (WebAssembly Test) files using Wasmtime's native WAST parser.
 *
 * <p>This class provides methods to execute WAST files directly using Wasmtime's built-in test
 * execution engine, ensuring 100% compatibility with Wasmtime's own test behavior.
 *
 * <p>WAST files are the standard test format used by the WebAssembly specification and contain
 * module definitions along with test assertions (e.g., assert_return, assert_trap, assert_invalid).
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. Multiple threads can execute WAST files
 * concurrently.
 *
 * <h2>Example Usage:</h2>
 *
 * <pre>{@code
 * // Execute a WAST file
 * WastExecutionResult result = JniWastRunner.executeWastFile("tests/test.wast");
 * if (result.allPassed()) {
 *     System.out.println("All tests passed!");
 * } else {
 *     System.out.println("Failed: " + result.getFailedDirectives() + " directives");
 * }
 *
 * // Execute WAST content from a string
 * String wastContent = "(module (func (export \"test\") (result i32) i32.const 42))";
 * WastExecutionResult result = JniWastRunner.executeWastString("inline.wast", wastContent);
 * }</pre>
 */
public final class JniWastRunner {

  /**
   * Executes a WAST file from disk.
   *
   * <p>This method loads and executes a WAST file using Wasmtime's native WAST parser. The file
   * should be in standard WAST format with module definitions and test assertions.
   *
   * @param filePath the path to the WAST file to execute (must exist and be readable)
   * @return the execution results containing pass/fail information
   * @throws IllegalArgumentException if filePath is null or empty
   * @throws RuntimeException if the native execution fails catastrophically
   */
  public static WastExecutionResult executeWastFile(final String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      throw new IllegalArgumentException("File path cannot be null or empty");
    }

    return nativeExecuteWastFile(filePath);
  }

  /**
   * Executes a WAST file from a Path object.
   *
   * @param path the path to the WAST file
   * @return the execution results
   * @throws IllegalArgumentException if path is null
   * @throws RuntimeException if the native execution fails
   */
  public static WastExecutionResult executeWastFile(final Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }

    return executeWastFile(path.toAbsolutePath().toString());
  }

  /**
   * Executes WAST content from a string.
   *
   * <p>This method executes WAST content provided as a string, which is useful for inline test
   * definitions or dynamically generated tests.
   *
   * @param filename the filename to use for error reporting (not a real file)
   * @param wastContent the WAST content to execute (in standard WAST text format)
   * @return the execution results
   * @throws IllegalArgumentException if filename or wastContent is null or empty
   * @throws RuntimeException if the native execution fails
   */
  public static WastExecutionResult executeWastString(
      final String filename, final String wastContent) {
    if (filename == null || filename.isEmpty()) {
      throw new IllegalArgumentException("Filename cannot be null or empty");
    }
    if (wastContent == null || wastContent.isEmpty()) {
      throw new IllegalArgumentException("WAST content cannot be null or empty");
    }

    final byte[] contentBytes = wastContent.getBytes(StandardCharsets.UTF_8);
    return nativeExecuteWastBuffer(filename, contentBytes);
  }

  /**
   * Executes WAST content from a byte array.
   *
   * <p>This is the most efficient method for executing WAST content that is already in byte form
   * (e.g., read from a file or network).
   *
   * @param filename the filename to use for error reporting
   * @param wastContent the WAST content as UTF-8 encoded bytes
   * @return the execution results
   * @throws IllegalArgumentException if filename or wastContent is null
   * @throws RuntimeException if the native execution fails
   */
  public static WastExecutionResult executeWastBytes(
      final String filename, final byte[] wastContent) {
    if (filename == null || filename.isEmpty()) {
      throw new IllegalArgumentException("Filename cannot be null or empty");
    }
    if (wastContent == null) {
      throw new IllegalArgumentException("WAST content cannot be null");
    }

    return nativeExecuteWastBuffer(filename, wastContent);
  }

  /**
   * Convenience method to execute a WAST file and throw an exception if it fails.
   *
   * @param filePath the path to the WAST file
   * @throws ai.tegmentum.wasmtime4j.wast.WastRunner.WastExecutionException if any directives fail
   *     or execution errors occur
   */
  public static void executeWastFileOrThrow(final String filePath) {
    final WastExecutionResult result = executeWastFile(filePath);
    if (!result.allPassed()) {
      throw new ai.tegmentum.wasmtime4j.wast.WastRunner.WastExecutionException(result);
    }
  }

  /**
   * Convenience method to execute WAST content and throw an exception if it fails.
   *
   * @param filename the filename for error reporting
   * @param wastContent the WAST content
   * @throws ai.tegmentum.wasmtime4j.wast.WastRunner.WastExecutionException if any directives fail
   *     or execution errors occur
   */
  public static void executeWastStringOrThrow(final String filename, final String wastContent) {
    final WastExecutionResult result = executeWastString(filename, wastContent);
    if (!result.allPassed()) {
      throw new ai.tegmentum.wasmtime4j.wast.WastRunner.WastExecutionException(result);
    }
  }

  // Native methods - implemented in Rust via JNI

  /**
   * Native method to execute a WAST file from disk.
   *
   * @param filePath the path to the WAST file
   * @return the execution results
   */
  private static native WastExecutionResult nativeExecuteWastFile(String filePath);

  /**
   * Native method to execute WAST content from a byte buffer.
   *
   * @param filename the filename for error reporting
   * @param content the WAST content as bytes
   * @return the execution results
   */
  private static native WastExecutionResult nativeExecuteWastBuffer(
      String filename, byte[] content);

  // Private constructor to prevent instantiation
  private JniWastRunner() {
    throw new AssertionError("JniWastRunner cannot be instantiated");
  }
}
