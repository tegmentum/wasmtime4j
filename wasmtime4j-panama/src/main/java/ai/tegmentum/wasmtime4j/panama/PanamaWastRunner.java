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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.wast.WastDirectiveResult;
import ai.tegmentum.wasmtime4j.wast.WastExecutionResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

  private static final Gson GSON = new Gson();

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
            "Native WAST execution failed with error code: " + result,
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
   * @throws WastExecutionException if any directives fail or execution errors occur
   */
  public static void executeWastFileOrThrow(final String filePath) {
    final WastExecutionResult result = executeWastFile(filePath);
    if (!result.allPassed()) {
      throw new WastExecutionException(result);
    }
  }

  /**
   * Convenience method to execute WAST content and throw an exception if it fails.
   *
   * @param filename the filename for error reporting
   * @param wastContent the WAST content
   * @throws WastExecutionException if any directives fail or execution errors occur
   */
  public static void executeWastStringOrThrow(final String filename, final String wastContent) {
    final WastExecutionResult result = executeWastString(filename, wastContent);
    if (!result.allPassed()) {
      throw new WastExecutionException(result);
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
            "Native WAST execution failed with error code: " + result,
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
  private static WastExecutionResult parseJsonResult(final String json) {
    final JsonObject root = GSON.fromJson(json, JsonObject.class);

    final String filePath = root.has("file_path") ? root.get("file_path").getAsString() : "";
    final int totalDirectives =
        root.has("total_directives") ? root.get("total_directives").getAsInt() : 0;
    final int passedDirectives =
        root.has("passed_directives") ? root.get("passed_directives").getAsInt() : 0;
    final int failedDirectives =
        root.has("failed_directives") ? root.get("failed_directives").getAsInt() : 0;
    final String executionError =
        root.has("execution_error") && !root.get("execution_error").isJsonNull()
            ? root.get("execution_error").getAsString()
            : null;

    WastDirectiveResult[] directiveResults = new WastDirectiveResult[0];
    if (root.has("directive_results")) {
      final JsonArray directivesArray = root.getAsJsonArray("directive_results");
      directiveResults = new WastDirectiveResult[directivesArray.size()];
      for (int i = 0; i < directivesArray.size(); i++) {
        final JsonElement element = directivesArray.get(i);
        final JsonObject directive = element.getAsJsonObject();
        final int lineNumber =
            directive.has("line_number") ? directive.get("line_number").getAsInt() : 0;
        final boolean passed = directive.has("passed") && directive.get("passed").getAsBoolean();
        final String errorMessage =
            directive.has("error_message") && !directive.get("error_message").isJsonNull()
                ? directive.get("error_message").getAsString()
                : null;
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

  /** Exception thrown when WAST execution fails. */
  public static final class WastExecutionException extends RuntimeException {
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
            "WAST execution failed for %s: %s", result.getFilePath(), result.getExecutionError());
      } else {
        return String.format(
            "WAST execution failed for %s: %d of %d directives failed",
            result.getFilePath(), result.getFailedDirectives(), result.getTotalDirectives());
      }
    }
  }

  // Private constructor to prevent instantiation
  private PanamaWastRunner() {
    throw new AssertionError("PanamaWastRunner cannot be instantiated");
  }
}
