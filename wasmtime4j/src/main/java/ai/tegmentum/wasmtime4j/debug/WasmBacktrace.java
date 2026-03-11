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
package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A WebAssembly call stack backtrace.
 *
 * <p>WasmBacktrace captures the call stack at a specific point during WebAssembly execution,
 * typically when a trap occurs. It provides access to stack frames with function information and
 * debug symbols.
 *
 * <p>Backtraces are captured automatically when traps occur if the engine is configured with debug
 * info enabled. They can also be explicitly captured from a Store.
 *
 * @since 1.0.0
 */
public final class WasmBacktrace {

  private final List<FrameInfo> frames;
  private final boolean forceCapture;

  /**
   * Captures a backtrace from the current execution state of the store.
   *
   * <p>This delegates to {@link Store#captureBacktrace()}.
   *
   * @param store the store to capture the backtrace from
   * @return the captured backtrace
   * @throws WasmException if capture fails
   * @throws IllegalArgumentException if store is null
   * @since 1.1.0
   */
  public static WasmBacktrace capture(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    return store.captureBacktrace();
  }

  /**
   * Force-captures a backtrace even if backtrace capture is disabled in the engine configuration.
   *
   * <p>This delegates to {@link Store#forceCaptureBacktrace()}.
   *
   * @param store the store to capture the backtrace from
   * @return the force-captured backtrace
   * @throws WasmException if capture fails
   * @throws IllegalArgumentException if store is null
   * @since 1.1.0
   */
  public static WasmBacktrace forceCapture(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    return store.forceCaptureBacktrace();
  }

  /**
   * Pattern matching wasmtime backtrace frame lines.
   *
   * <p>Format: {@code N: 0xHEX - <func_name>} or {@code N: 0xHEX - func_name}
   */
  private static final Pattern FRAME_PATTERN =
      Pattern.compile("^\\s*(\\d+):\\s+0x([0-9a-fA-F]+)\\s+-\\s+(.+)$");

  /**
   * Pattern matching DWARF source location lines that follow frame lines.
   *
   * <p>Format: {@code at file:line:col} or {@code at file:line}
   */
  private static final Pattern SOURCE_PATTERN =
      Pattern.compile("^\\s+at\\s+(.+?):(\\d+)(?::(\\d+))?$");

  /**
   * Parses a {@link WasmBacktrace} from a wasmtime error message string.
   *
   * <p>Wasmtime embeds backtrace text in error messages using the format:
   *
   * <pre>
   * wasm trap: &lt;description&gt;
   * wasm backtrace:
   *     0:   0x1234 - &lt;func_name&gt;
   *                     at &lt;file&gt;:&lt;line&gt;:&lt;col&gt;
   *     1:   0x5678 - &lt;func_name&gt;
   * </pre>
   *
   * @param message the error message that may contain a wasmtime backtrace section
   * @return a parsed WasmBacktrace, or an empty backtrace if no backtrace section was found
   */
  public static WasmBacktrace fromErrorMessage(final String message) {
    if (message == null || message.isEmpty()) {
      return new WasmBacktrace(Collections.emptyList(), false);
    }

    final int btIndex = message.indexOf("wasm backtrace:");
    if (btIndex < 0) {
      return new WasmBacktrace(Collections.emptyList(), false);
    }

    final String btSection = message.substring(btIndex + "wasm backtrace:".length());
    final String[] lines = btSection.split("\n");

    final List<FrameInfo> frames = new ArrayList<>();
    int currentFuncIndex = 0;
    String currentFuncName = null;
    Integer currentOffset = null;

    for (int i = 0; i < lines.length; i++) {
      final String line = lines[i];
      final Matcher frameMatcher = FRAME_PATTERN.matcher(line);
      if (frameMatcher.matches()) {
        // Flush any previous frame that hasn't been added yet
        // (handled by checking if currentOffset is set below)

        currentFuncIndex = Integer.parseInt(frameMatcher.group(1));
        try {
          currentOffset = Integer.parseInt(frameMatcher.group(2), 16);
        } catch (NumberFormatException e) {
          currentOffset = null;
        }
        String rawName = frameMatcher.group(3).trim();
        // Strip angle brackets: <func_name> -> func_name
        if (rawName.startsWith("<") && rawName.endsWith(">")) {
          rawName = rawName.substring(1, rawName.length() - 1);
        }

        // Extract module name from "module!func" format
        String currentModuleName = null;
        final int bangIndex = rawName.indexOf('!');
        if (bangIndex > 0) {
          currentModuleName = rawName.substring(0, bangIndex);
          rawName = rawName.substring(bangIndex + 1);
        }
        currentFuncName = rawName;

        // Check if next line is a source location
        FrameSymbol symbol = null;
        if (i + 1 < lines.length) {
          final Matcher srcMatcher = SOURCE_PATTERN.matcher(lines[i + 1]);
          if (srcMatcher.matches()) {
            final String file = srcMatcher.group(1);
            final Integer srcLine = Integer.parseInt(srcMatcher.group(2));
            final Integer srcCol =
                srcMatcher.group(3) != null ? Integer.parseInt(srcMatcher.group(3)) : null;
            symbol = new FrameSymbol(currentFuncName, file, srcLine, srcCol);
            i++; // skip the source line
          }
        }

        final List<FrameSymbol> symbols =
            symbol != null ? Collections.singletonList(symbol) : Collections.emptyList();
        frames.add(
            new FrameInfo(
                currentFuncIndex,
                null,
                currentFuncName,
                currentOffset,
                null,
                symbols,
                currentModuleName));
        currentOffset = null;
        currentFuncName = null;
      }
    }

    return new WasmBacktrace(frames, false);
  }

  /**
   * Creates a new WasmBacktrace with the specified frames.
   *
   * @param frames the stack frames in order from innermost (current) to outermost (caller)
   * @param forceCapture whether this backtrace was force-captured
   */
  public WasmBacktrace(final List<FrameInfo> frames, final boolean forceCapture) {
    this.frames = frames != null ? Collections.unmodifiableList(frames) : Collections.emptyList();
    this.forceCapture = forceCapture;
  }

  /**
   * Gets the stack frames in this backtrace.
   *
   * <p>Frames are ordered from innermost (index 0 = current function) to outermost (highest index =
   * root caller).
   *
   * @return an immutable list of stack frames
   */
  public List<FrameInfo> getFrames() {
    return new java.util.ArrayList<>(frames);
  }

  /**
   * Checks if this backtrace was force-captured.
   *
   * <p>Force-captured backtraces are explicitly requested even when backtrace capture is disabled
   * in the engine configuration.
   *
   * @return true if this was a forced capture
   */
  public boolean isForceCapture() {
    return forceCapture;
  }

  /**
   * Gets the number of frames in this backtrace.
   *
   * @return the frame count
   */
  public int getFrameCount() {
    return frames.size();
  }

  /**
   * Checks if this backtrace is empty.
   *
   * @return true if there are no frames
   */
  public boolean isEmpty() {
    return frames.isEmpty();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WasmBacktrace that = (WasmBacktrace) obj;
    return forceCapture == that.forceCapture && Objects.equals(frames, that.frames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(frames, forceCapture);
  }

  @Override
  public String toString() {
    if (frames.isEmpty()) {
      return "WasmBacktrace{empty}";
    }
    final StringBuilder sb = new StringBuilder("WasmBacktrace{\n");
    for (int i = 0; i < frames.size(); i++) {
      sb.append("  ").append(i).append(": ").append(frames.get(i)).append("\n");
    }
    sb.append("}");
    return sb.toString();
  }
}
