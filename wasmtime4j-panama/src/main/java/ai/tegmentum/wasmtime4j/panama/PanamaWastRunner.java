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
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.wast.WastDirectiveResult;
import ai.tegmentum.wasmtime4j.wast.WastExecutionResult;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI wrapper for executing WAST (WebAssembly Test) files using Wasmtime's native WAST
 * parser.
 *
 * <p>This class provides methods to execute WAST files directly using Wasmtime's built-in test
 * execution engine via the Panama Foreign Function Interface, ensuring 100% compatibility with
 * Wasmtime's own test behavior.
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
 * WastExecutionResult result = PanamaWastRunner.executeWastFile("tests/test.wast");
 * if (result.allPassed()) {
 *     System.out.println("All tests passed!");
 * } else {
 *     System.out.println("Failed: " + result.getFailedDirectives() + " directives");
 * }
 *
 * // Execute WAST content from a string
 * String wastContent = "(module (func (export \"test\") (result i32) i32.const 42))";
 * WastExecutionResult result = PanamaWastRunner.executeWastString("inline.wast", wastContent);
 * }</pre>
 */
public final class PanamaWastRunner {

  private static final Logger LOGGER = Logger.getLogger(PanamaWastRunner.class.getName());

  // Native function descriptors
  // wasmtime4j_panama_wast_execute_file(file_path: *const c_char, result_json: *mut *mut c_char)
  // -> i32
  private static final FunctionDescriptor EXECUTE_FILE_DESC =
      FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);

  // wasmtime4j_panama_wast_execute_buffer(filename: *const c_char, content: *const u8,
  //   content_len: usize, result_json: *mut *mut c_char) -> i32
  private static final FunctionDescriptor EXECUTE_BUFFER_DESC =
      FunctionDescriptor.of(
          ValueLayout.JAVA_INT,
          ValueLayout.ADDRESS,
          ValueLayout.ADDRESS,
          ValueLayout.JAVA_LONG,
          ValueLayout.ADDRESS);

  // wasmtime4j_panama_wast_free_result(result_json: *mut c_char)
  private static final FunctionDescriptor FREE_RESULT_DESC =
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);

  // Lazily initialized method handles
  private static volatile MethodHandle executeFileHandle;
  private static volatile MethodHandle executeBufferHandle;
  private static volatile MethodHandle freeResultHandle;

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

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment filePathSegment = arena.allocateFrom(filePath);
      final MemorySegment resultPtrPtr = arena.allocate(ValueLayout.ADDRESS);

      final MethodHandle mh = getExecuteFileHandle();
      final int result = (int) mh.invokeExact(filePathSegment, resultPtrPtr);

      if (result != 0) {
        return new WastExecutionResult(
            filePath,
            0,
            0,
            0,
            "Native WAST execution failed: " + PanamaErrorMapper.getErrorDescription(result),
            new WastDirectiveResult[0]);
      }

      final MemorySegment resultPtr = resultPtrPtr.get(ValueLayout.ADDRESS, 0);
      if (resultPtr.equals(MemorySegment.NULL)) {
        return new WastExecutionResult(
            filePath,
            0,
            0,
            0,
            "Native WAST execution returned null result",
            new WastDirectiveResult[0]);
      }

      try {
        final String json = resultPtr.reinterpret(Long.MAX_VALUE).getString(0);
        return parseJsonResult(json);
      } finally {
        freeResult(resultPtr);
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to execute WAST file: " + filePath, e);
      return new WastExecutionResult(
          filePath, 0, 0, 0, "Panama FFI error: " + e.getMessage(), new WastDirectiveResult[0]);
    }
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
    return executeWastBuffer(filename, contentBytes);
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

    return executeWastBuffer(filename, wastContent);
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

  /**
   * Executes WAST content from a byte buffer via the native FFI.
   *
   * @param filename the filename for error reporting
   * @param content the WAST content bytes
   * @return the execution results
   */
  private static WastExecutionResult executeWastBuffer(
      final String filename, final byte[] content) {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment filenameSegment = arena.allocateFrom(filename);
      final MemorySegment contentSegment = arena.allocate(content.length);
      contentSegment.copyFrom(MemorySegment.ofArray(content));
      final MemorySegment resultPtrPtr = arena.allocate(ValueLayout.ADDRESS);

      final MethodHandle mh = getExecuteBufferHandle();
      final int result =
          (int)
              mh.invokeExact(filenameSegment, contentSegment, (long) content.length, resultPtrPtr);

      if (result != 0) {
        return new WastExecutionResult(
            filename,
            0,
            0,
            0,
            "Native WAST execution failed: " + PanamaErrorMapper.getErrorDescription(result),
            new WastDirectiveResult[0]);
      }

      final MemorySegment resultPtr = resultPtrPtr.get(ValueLayout.ADDRESS, 0);
      if (resultPtr.equals(MemorySegment.NULL)) {
        return new WastExecutionResult(
            filename,
            0,
            0,
            0,
            "Native WAST execution returned null result",
            new WastDirectiveResult[0]);
      }

      try {
        final String json = resultPtr.reinterpret(Long.MAX_VALUE).getString(0);
        return parseJsonResult(json);
      } finally {
        freeResult(resultPtr);
      }
    } catch (Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to execute WAST buffer: " + filename, e);
      return new WastExecutionResult(
          filename, 0, 0, 0, "Panama FFI error: " + e.getMessage(), new WastDirectiveResult[0]);
    }
  }

  /**
   * Parses a JSON string into a WastExecutionResult.
   *
   * @param json the JSON string from the native WAST execution
   * @return the parsed WastExecutionResult
   */
  @SuppressWarnings("checkstyle:CyclomaticComplexity")
  private static WastExecutionResult parseJsonResult(final String json) {
    final String filePath = extractStringField(json, "file_path", "");
    final int totalDirectives = extractIntField(json, "total_directives", 0);
    final int passedDirectives = extractIntField(json, "passed_directives", 0);
    final int failedDirectives = extractIntField(json, "failed_directives", 0);
    final String executionError = extractNullableStringField(json, "execution_error");

    WastDirectiveResult[] directiveResults = new WastDirectiveResult[0];
    final String directivesArrayStr = extractArrayField(json, "directive_results");
    if (directivesArrayStr != null && !directivesArrayStr.equals("[]")) {
      final java.util.List<String> directives = splitJsonObjects(directivesArrayStr);
      directiveResults = new WastDirectiveResult[directives.size()];
      for (int i = 0; i < directives.size(); i++) {
        final String directive = directives.get(i);
        final int lineNumber = extractIntField(directive, "line_number", 0);
        final boolean passed = extractBooleanField(directive, "passed");
        final String errorMessage = extractNullableStringField(directive, "error_message");
        directiveResults[i] = new WastDirectiveResult(lineNumber, passed, errorMessage);
      }
    }

    return new WastExecutionResult(
        filePath,
        totalDirectives,
        passedDirectives,
        failedDirectives,
        executionError,
        directiveResults);
  }

  private static String extractStringField(
      final String json, final String key, final String defaultValue) {
    final String result = extractNullableStringField(json, key);
    return result != null ? result : defaultValue;
  }

  private static String extractNullableStringField(final String json, final String key) {
    final String search = "\"" + key + "\":";
    final int idx = json.indexOf(search);
    if (idx < 0) {
      return null;
    }
    int pos = idx + search.length();
    while (pos < json.length() && json.charAt(pos) == ' ') {
      pos++;
    }
    if (pos >= json.length()) {
      return null;
    }
    // Check for null
    if (json.startsWith("null", pos)) {
      return null;
    }
    if (json.charAt(pos) != '"') {
      return null;
    }
    pos++; // skip opening quote
    final StringBuilder sb = new StringBuilder();
    while (pos < json.length() && json.charAt(pos) != '"') {
      if (json.charAt(pos) == '\\' && pos + 1 < json.length()) {
        pos++;
        sb.append(json.charAt(pos));
      } else {
        sb.append(json.charAt(pos));
      }
      pos++;
    }
    return sb.toString();
  }

  private static int extractIntField(final String json, final String key, final int defaultValue) {
    final String search = "\"" + key + "\":";
    final int idx = json.indexOf(search);
    if (idx < 0) {
      return defaultValue;
    }
    int pos = idx + search.length();
    while (pos < json.length() && json.charAt(pos) == ' ') {
      pos++;
    }
    final int start = pos;
    while (pos < json.length() && (Character.isDigit(json.charAt(pos)) || json.charAt(pos) == '-')) {
      pos++;
    }
    if (start == pos) {
      return defaultValue;
    }
    return Integer.parseInt(json.substring(start, pos));
  }

  private static boolean extractBooleanField(final String json, final String key) {
    final String search = "\"" + key + "\":";
    final int idx = json.indexOf(search);
    if (idx < 0) {
      return false;
    }
    int pos = idx + search.length();
    while (pos < json.length() && json.charAt(pos) == ' ') {
      pos++;
    }
    return json.startsWith("true", pos);
  }

  private static String extractArrayField(final String json, final String key) {
    final String search = "\"" + key + "\":";
    final int idx = json.indexOf(search);
    if (idx < 0) {
      return null;
    }
    final int bracketStart = json.indexOf('[', idx + search.length());
    if (bracketStart < 0) {
      return null;
    }
    int depth = 0;
    for (int i = bracketStart; i < json.length(); i++) {
      if (json.charAt(i) == '[') {
        depth++;
      } else if (json.charAt(i) == ']') {
        depth--;
        if (depth == 0) {
          return json.substring(bracketStart, i + 1);
        }
      }
    }
    return null;
  }

  /** Splits a JSON array into its top-level object elements. */
  private static java.util.List<String> splitJsonObjects(final String arrayStr) {
    final java.util.List<String> result = new java.util.ArrayList<>();
    int depth = 0;
    int start = -1;
    for (int i = 0; i < arrayStr.length(); i++) {
      final char ch = arrayStr.charAt(i);
      if (ch == '{') {
        if (depth == 0) {
          start = i;
        }
        depth++;
      } else if (ch == '}') {
        depth--;
        if (depth == 0 && start >= 0) {
          result.add(arrayStr.substring(start, i + 1));
          start = -1;
        }
      }
    }
    return result;
  }

  /**
   * Frees a native result string.
   *
   * @param resultPtr the pointer to free
   */
  private static void freeResult(final MemorySegment resultPtr) {
    try {
      final MethodHandle mh = getFreeResultHandle();
      mh.invokeExact(resultPtr);
    } catch (Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to free native WAST result", e);
    }
  }

  private static MethodHandle getExecuteFileHandle() {
    MethodHandle mh = executeFileHandle;
    if (mh == null) {
      synchronized (PanamaWastRunner.class) {
        mh = executeFileHandle;
        if (mh == null) {
          final Optional<MethodHandle> handle =
              NativeLibraryLoader.getInstance()
                  .lookupFunction("wasmtime4j_panama_wast_execute_file", EXECUTE_FILE_DESC);
          mh =
              handle.orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Failed to find wasmtime4j_panama_wast_execute_file"));
          executeFileHandle = mh;
        }
      }
    }
    return mh;
  }

  private static MethodHandle getExecuteBufferHandle() {
    MethodHandle mh = executeBufferHandle;
    if (mh == null) {
      synchronized (PanamaWastRunner.class) {
        mh = executeBufferHandle;
        if (mh == null) {
          final Optional<MethodHandle> handle =
              NativeLibraryLoader.getInstance()
                  .lookupFunction("wasmtime4j_panama_wast_execute_buffer", EXECUTE_BUFFER_DESC);
          mh =
              handle.orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Failed to find wasmtime4j_panama_wast_execute_buffer"));
          executeBufferHandle = mh;
        }
      }
    }
    return mh;
  }

  private static MethodHandle getFreeResultHandle() {
    MethodHandle mh = freeResultHandle;
    if (mh == null) {
      synchronized (PanamaWastRunner.class) {
        mh = freeResultHandle;
        if (mh == null) {
          final Optional<MethodHandle> handle =
              NativeLibraryLoader.getInstance()
                  .lookupFunction("wasmtime4j_panama_wast_free_result", FREE_RESULT_DESC);
          mh =
              handle.orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Failed to find wasmtime4j_panama_wast_free_result"));
          freeResultHandle = mh;
        }
      }
    }
    return mh;
  }

  // Private constructor to prevent instantiation
  private PanamaWastRunner() {
    throw new AssertionError("PanamaWastRunner cannot be instantiated");
  }
}
