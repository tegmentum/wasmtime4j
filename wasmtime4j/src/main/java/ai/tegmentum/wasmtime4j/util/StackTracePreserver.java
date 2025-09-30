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

package ai.tegmentum.wasmtime4j.util;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Utility for preserving and enhancing stack trace information across WebAssembly boundaries.
 *
 * <p>This class provides mechanisms to capture and preserve stack trace information when errors
 * occur during WebAssembly execution, bridging the gap between native WebAssembly stack traces and
 * Java stack traces to provide comprehensive debugging information.
 *
 * <p>The stack trace preserver supports parsing native stack trace information, mapping WebAssembly
 * function names and source locations, and combining this information with Java stack traces to
 * create comprehensive error reports.
 *
 * @since 1.0.0
 */
public final class StackTracePreserver {
  private static final Logger LOGGER = Logger.getLogger(StackTracePreserver.class.getName());

  /** Prefix used to identify WebAssembly stack frames in combined stack traces. */
  private static final String WASM_FRAME_PREFIX = "[WASM]";

  /** Prefix used to identify native runtime stack frames. */
  private static final String NATIVE_FRAME_PREFIX = "[NATIVE]";

  /** Private constructor to prevent instantiation of utility class. */
  private StackTracePreserver() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /** Represents a WebAssembly stack frame with enhanced debugging information. */
  public static final class WasmStackFrame {
    private final String functionName;
    private final String moduleName;
    private final Optional<String> sourceFile;
    private final Optional<Integer> lineNumber;
    private final Optional<Integer> columnNumber;
    private final long instructionOffset;

    /**
     * Creates a new WebAssembly stack frame.
     *
     * @param functionName the WebAssembly function name
     * @param moduleName the module name (may be null)
     * @param sourceFile the source file name (may be null)
     * @param lineNumber the source line number (may be null)
     * @param columnNumber the source column number (may be null)
     * @param instructionOffset the instruction offset within the function
     */
    public WasmStackFrame(
        final String functionName,
        final String moduleName,
        final String sourceFile,
        final Integer lineNumber,
        final Integer columnNumber,
        final long instructionOffset) {
      this.functionName = functionName != null ? functionName : "<unknown>";
      this.moduleName = moduleName;
      this.sourceFile = Optional.ofNullable(sourceFile);
      this.lineNumber = Optional.ofNullable(lineNumber);
      this.columnNumber = Optional.ofNullable(columnNumber);
      this.instructionOffset = instructionOffset;
    }

    /**
     * Creates a minimal WebAssembly stack frame with just function name.
     *
     * @param functionName the function name
     * @param instructionOffset the instruction offset
     * @return new WebAssembly stack frame
     */
    public static WasmStackFrame simple(final String functionName, final long instructionOffset) {
      return new WasmStackFrame(functionName, null, null, null, null, instructionOffset);
    }

    public String getFunctionName() {
      return functionName;
    }

    public Optional<String> getModuleName() {
      return Optional.ofNullable(moduleName);
    }

    public Optional<String> getSourceFile() {
      return sourceFile;
    }

    public Optional<Integer> getLineNumber() {
      return lineNumber;
    }

    public Optional<Integer> getColumnNumber() {
      return columnNumber;
    }

    public long getInstructionOffset() {
      return instructionOffset;
    }

    /**
     * Converts this WebAssembly stack frame to a Java StackTraceElement.
     *
     * @return equivalent Java stack trace element
     */
    public StackTraceElement toStackTraceElement() {
      final String className = WASM_FRAME_PREFIX + (moduleName != null ? " " + moduleName : "");
      final String fileName = sourceFile.orElse(null);
      final int lineNum = lineNumber.orElse(-1);

      return new StackTraceElement(className, functionName, fileName, lineNum);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(WASM_FRAME_PREFIX).append(" ");

      if (moduleName != null) {
        sb.append(moduleName).append("::");
      }

      sb.append(functionName);

      if (sourceFile.isPresent()) {
        sb.append(" (").append(sourceFile.get());
        if (lineNumber.isPresent()) {
          sb.append(":").append(lineNumber.get());
          if (columnNumber.isPresent()) {
            sb.append(":").append(columnNumber.get());
          }
        }
        sb.append(")");
      } else {
        sb.append(" (offset ").append(instructionOffset).append(")");
      }

      return sb.toString();
    }
  }

  /** Enhanced exception that preserves WebAssembly stack trace information. */
  public static final class EnhancedWasmException extends WasmException {
    private static final long serialVersionUID = 1L;

    private final transient List<WasmStackFrame> wasmStackFrames;
    private final transient Optional<String> wasmTrapMessage;

    /**
     * Creates an enhanced WebAssembly exception with stack trace information.
     *
     * @param message the exception message
     * @param wasmStackFrames the WebAssembly stack frames
     * @param wasmTrapMessage the original WebAssembly trap message
     * @param cause the underlying cause
     */
    public EnhancedWasmException(
        final String message,
        final List<WasmStackFrame> wasmStackFrames,
        final String wasmTrapMessage,
        final Throwable cause) {
      super(message, cause);
      this.wasmStackFrames = Collections.unmodifiableList(new ArrayList<>(wasmStackFrames));
      this.wasmTrapMessage = Optional.ofNullable(wasmTrapMessage);

      // Enhance the Java stack trace with WebAssembly frames
      enhanceStackTrace();
    }

    /**
     * Creates an enhanced exception from a regular WebAssembly exception.
     *
     * @param original the original exception
     * @param wasmStackFrames the WebAssembly stack frames
     * @param wasmTrapMessage the WebAssembly trap message
     */
    public EnhancedWasmException(
        final WasmException original,
        final List<WasmStackFrame> wasmStackFrames,
        final String wasmTrapMessage) {
      this(original.getMessage(), wasmStackFrames, wasmTrapMessage, original.getCause());
    }

    public List<WasmStackFrame> getWasmStackFrames() {
      return wasmStackFrames;
    }

    public Optional<String> getWasmTrapMessage() {
      return wasmTrapMessage;
    }

    /** Enhances the Java stack trace by inserting WebAssembly frames. */
    private void enhanceStackTrace() {
      final StackTraceElement[] originalStackTrace = getStackTrace();
      final List<StackTraceElement> enhancedStackTrace = new ArrayList<>();

      // Add WebAssembly frames first
      for (final WasmStackFrame wasmFrame : wasmStackFrames) {
        enhancedStackTrace.add(wasmFrame.toStackTraceElement());
      }

      // Add a separator frame
      if (!wasmStackFrames.isEmpty() && originalStackTrace.length > 0) {
        enhancedStackTrace.add(
            new StackTraceElement("[BOUNDARY]", "nativeToJavaTransition", null, -1));
      }

      // Add original Java frames
      Collections.addAll(enhancedStackTrace, originalStackTrace);

      setStackTrace(enhancedStackTrace.toArray(new StackTraceElement[0]));
    }

    @Override
    public String getMessage() {
      final StringBuilder message = new StringBuilder(super.getMessage());

      if (wasmTrapMessage.isPresent()) {
        message.append("\nWebAssembly trap: ").append(wasmTrapMessage.get());
      }

      if (!wasmStackFrames.isEmpty()) {
        message.append("\nWebAssembly stack trace:");
        for (final WasmStackFrame frame : wasmStackFrames) {
          message.append("\n  at ").append(frame.toString());
        }
      }

      return message.toString();
    }
  }

  /**
   * Parses a native WebAssembly stack trace string into structured stack frames.
   *
   * <p>This method attempts to parse various formats of WebAssembly stack trace output from
   * different sources (Wasmtime, browser engines, etc.) and convert them into structured
   * WasmStackFrame objects.
   *
   * @param nativeStackTrace the native stack trace string
   * @return list of parsed WebAssembly stack frames
   */
  public static List<WasmStackFrame> parseNativeStackTrace(final String nativeStackTrace) {
    final List<WasmStackFrame> frames = new ArrayList<>();

    if (nativeStackTrace == null || nativeStackTrace.trim().isEmpty()) {
      return frames;
    }

    final String[] lines = nativeStackTrace.split("\n");

    for (final String line : lines) {
      final String trimmedLine = line.trim();
      if (trimmedLine.isEmpty()) {
        continue;
      }

      final Optional<WasmStackFrame> frame = parseStackTraceLine(trimmedLine);
      frame.ifPresent(frames::add);
    }

    LOGGER.fine("Parsed " + frames.size() + " WebAssembly stack frames");
    return frames;
  }

  /**
   * Parses a single line of native stack trace output.
   *
   * @param line the stack trace line
   * @return parsed WebAssembly stack frame, if successful
   */
  private static Optional<WasmStackFrame> parseStackTraceLine(final String line) {
    // Try to parse various common WebAssembly stack trace formats

    // Format 1: "function_name (module.wasm:line:column)"
    if (line.contains("(") && line.contains(")")) {
      return parseParenthesesFormat(line);
    }

    // Format 2: "at function_name (module.wasm:line:column)"
    if (line.startsWith("at ")) {
      return parseParenthesesFormat(line.substring(3));
    }

    // Format 3: "function_name@module.wasm:line:column"
    if (line.contains("@")) {
      return parseAtSignFormat(line);
    }

    // Format 4: Simple function name only
    if (!line.contains(" ") && isValidFunctionName(line)) {
      return Optional.of(WasmStackFrame.simple(line, 0));
    }

    LOGGER.fine("Unable to parse stack trace line: " + line);
    return Optional.empty();
  }

  /** Parses stack trace format: "function_name (module.wasm:line:column)". */
  private static Optional<WasmStackFrame> parseParenthesesFormat(final String line) {
    final int parenIndex = line.indexOf('(');
    final int closeParenIndex = line.lastIndexOf(')');

    if (parenIndex == -1 || closeParenIndex == -1 || closeParenIndex <= parenIndex) {
      return Optional.empty();
    }

    final String functionName = line.substring(0, parenIndex).trim();
    final String locationInfo = line.substring(parenIndex + 1, closeParenIndex).trim();

    return parseLocationInfo(functionName, locationInfo);
  }

  /** Parses stack trace format: "function_name@module.wasm:line:column". */
  private static Optional<WasmStackFrame> parseAtSignFormat(final String line) {
    final int atIndex = line.indexOf('@');
    if (atIndex == -1) {
      return Optional.empty();
    }

    final String functionName = line.substring(0, atIndex).trim();
    final String locationInfo = line.substring(atIndex + 1).trim();

    return parseLocationInfo(functionName, locationInfo);
  }

  /** Parses location information in the format "module.wasm:line:column". */
  private static Optional<WasmStackFrame> parseLocationInfo(
      final String functionName, final String locationInfo) {
    String moduleName = null;
    String sourceFile = null;
    Integer lineNumber = null;
    Integer columnNumber = null;
    long instructionOffset = 0;

    final String[] parts = locationInfo.split(":");
    if (parts.length >= 1) {
      sourceFile = parts[0];
      if (sourceFile.endsWith(".wasm")) {
        moduleName = sourceFile.substring(0, sourceFile.length() - 5);
      }
    }

    if (parts.length >= 2) {
      try {
        lineNumber = Integer.parseInt(parts[1]);
      } catch (NumberFormatException e) {
        // Line number might be instruction offset
        try {
          instructionOffset = Long.parseLong(parts[1]);
        } catch (NumberFormatException e2) {
          // Ignore parsing errors
        }
      }
    }

    if (parts.length >= 3) {
      try {
        columnNumber = Integer.parseInt(parts[2]);
      } catch (NumberFormatException e) {
        // Ignore parsing errors
      }
    }

    return Optional.of(
        new WasmStackFrame(
            functionName, moduleName, sourceFile, lineNumber, columnNumber, instructionOffset));
  }

  /** Checks if a string looks like a valid WebAssembly function name. */
  private static boolean isValidFunctionName(final String name) {
    return name != null
        && !name.isEmpty()
        && (name.startsWith("$") || Character.isLetter(name.charAt(0)) || name.charAt(0) == '_');
  }

  /**
   * Creates an enhanced exception that preserves WebAssembly stack trace information.
   *
   * @param originalException the original exception
   * @param nativeStackTrace the native WebAssembly stack trace
   * @param wasmTrapMessage the WebAssembly trap message
   * @return enhanced exception with preserved stack trace
   */
  public static EnhancedWasmException enhanceException(
      final WasmException originalException,
      final String nativeStackTrace,
      final String wasmTrapMessage) {
    final List<WasmStackFrame> wasmFrames = parseNativeStackTrace(nativeStackTrace);
    return new EnhancedWasmException(originalException, wasmFrames, wasmTrapMessage);
  }

  /**
   * Creates an enhanced exception from scratch with WebAssembly stack information.
   *
   * @param message the exception message
   * @param nativeStackTrace the native WebAssembly stack trace
   * @param wasmTrapMessage the WebAssembly trap message
   * @param cause the underlying cause
   * @return enhanced exception with preserved stack trace
   */
  public static EnhancedWasmException createEnhancedException(
      final String message,
      final String nativeStackTrace,
      final String wasmTrapMessage,
      final Throwable cause) {
    final List<WasmStackFrame> wasmFrames = parseNativeStackTrace(nativeStackTrace);
    return new EnhancedWasmException(message, wasmFrames, wasmTrapMessage, cause);
  }

  /**
   * Combines multiple stack traces into a single comprehensive trace.
   *
   * <p>This method is useful when errors occur across multiple boundaries (e.g., Java -> WASM ->
   * native -> WASM -> Java) and you want to preserve the complete call chain.
   *
   * @param primaryException the primary exception
   * @param additionalStackTraces additional stack trace information to include
   * @return enhanced exception with combined stack traces
   */
  public static EnhancedWasmException combineStackTraces(
      final WasmException primaryException, final String... additionalStackTraces) {
    final List<WasmStackFrame> allFrames = new ArrayList<>();

    for (final String stackTrace : additionalStackTraces) {
      allFrames.addAll(parseNativeStackTrace(stackTrace));
    }

    return new EnhancedWasmException(primaryException, allFrames, null);
  }

  /**
   * Extracts WebAssembly-specific debugging information from an exception chain.
   *
   * @param exception the exception to analyze
   * @return debugging information string
   */
  public static String extractDebuggingInfo(final Throwable exception) {
    final StringBuilder info = new StringBuilder();

    Throwable current = exception;
    int depth = 0;

    while (current != null && depth < 10) { // Prevent infinite loops
      info.append("Exception ")
          .append(depth)
          .append(": ")
          .append(current.getClass().getSimpleName())
          .append(": ")
          .append(current.getMessage())
          .append("\n");

      if (current instanceof EnhancedWasmException) {
        final EnhancedWasmException enhanced = (EnhancedWasmException) current;
        info.append("  WebAssembly frames: ")
            .append(enhanced.getWasmStackFrames().size())
            .append("\n");
        enhanced
            .getWasmTrapMessage()
            .ifPresent(msg -> info.append("  Trap message: ").append(msg).append("\n"));
      }

      current = current.getCause();
      depth++;
    }

    return info.toString();
  }
}
