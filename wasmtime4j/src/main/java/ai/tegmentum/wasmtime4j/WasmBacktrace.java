package ai.tegmentum.wasmtime4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    return frames;
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
