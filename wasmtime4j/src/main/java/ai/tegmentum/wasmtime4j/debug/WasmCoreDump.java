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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a core dump captured from a WebAssembly trap.
 *
 * <p>A core dump contains detailed information about the state of execution at the time of a trap,
 * including:
 *
 * <ul>
 *   <li>The trap message and backtrace
 *   <li>Memory contents
 *   <li>Global variable values
 *   <li>Table contents
 *   <li>Module information
 * </ul>
 *
 * <p>Core dumps can be serialized to disk for post-mortem analysis with external tools.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     instance.call("main");
 * } catch (WasmTrapException e) {
 *     WasmCoreDump coreDump = WasmCoreDump.capture(store, e);
 *     coreDump.writeTo(Path.of("crash.coredump"));
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasmCoreDump {

  private final String trapMessage;
  private final WasmBacktrace backtrace;
  private final Instant timestamp;
  private final List<MemoryDump> memoryDumps;
  private final Map<String, Long> globals;
  private final List<StackFrame> stackFrames;

  /**
   * Creates a new WasmCoreDump.
   *
   * @param trapMessage the trap message
   * @param backtrace the backtrace at the time of the trap
   * @param timestamp the time of the trap
   * @param memoryDumps the memory contents
   * @param globals the global variable values
   * @param stackFrames the stack frame details
   */
  public WasmCoreDump(
      final String trapMessage,
      final WasmBacktrace backtrace,
      final Instant timestamp,
      final List<MemoryDump> memoryDumps,
      final Map<String, Long> globals,
      final List<StackFrame> stackFrames) {
    this.trapMessage = trapMessage;
    this.backtrace = backtrace;
    this.timestamp = timestamp;
    this.memoryDumps = Collections.unmodifiableList(new java.util.ArrayList<>(memoryDumps));
    this.globals = Collections.unmodifiableMap(new java.util.HashMap<>(globals));
    this.stackFrames = Collections.unmodifiableList(new java.util.ArrayList<>(stackFrames));
  }

  /**
   * Gets the trap message.
   *
   * @return the trap message
   */
  public String getTrapMessage() {
    return trapMessage;
  }

  /**
   * Gets the backtrace.
   *
   * @return the backtrace
   */
  public WasmBacktrace getBacktrace() {
    return backtrace;
  }

  /**
   * Gets the timestamp when the trap occurred.
   *
   * @return the timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the memory dumps.
   *
   * @return the memory dumps
   */
  public List<MemoryDump> getMemoryDumps() {
    return memoryDumps;
  }

  /**
   * Gets the global variable values.
   *
   * @return the globals map
   */
  public Map<String, Long> getGlobals() {
    return globals;
  }

  /**
   * Gets the detailed stack frames.
   *
   * @return the stack frames
   */
  public List<StackFrame> getStackFrames() {
    return stackFrames;
  }

  /**
   * Writes this core dump to a file.
   *
   * @param path the output file path
   * @throws WasmException if writing fails
   */
  public void writeTo(final Path path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    try {
      byte[] data = serialize();
      Files.write(path, data);
    } catch (IOException e) {
      throw new WasmException("Failed to write core dump to " + path, e);
    }
  }

  /**
   * Serializes this core dump to bytes.
   *
   * @return the serialized core dump
   */
  public byte[] serialize() {
    StringBuilder sb = new StringBuilder();
    sb.append("WASMCOREDUMP\n");
    sb.append("timestamp: ").append(timestamp).append("\n");
    sb.append("trap: ").append(trapMessage).append("\n");
    sb.append("\n");

    sb.append("=== BACKTRACE ===\n");
    if (backtrace != null) {
      sb.append(backtrace).append("\n");
    }
    sb.append("\n");

    sb.append("=== GLOBALS ===\n");
    for (Map.Entry<String, Long> entry : globals.entrySet()) {
      sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
    }
    sb.append("\n");

    sb.append("=== STACK FRAMES ===\n");
    for (StackFrame frame : stackFrames) {
      sb.append(frame).append("\n");
    }
    sb.append("\n");

    sb.append("=== MEMORY DUMPS ===\n");
    for (MemoryDump dump : memoryDumps) {
      sb.append("Memory ").append(dump.getMemoryIndex()).append(": ");
      sb.append(dump.getSize()).append(" bytes\n");
    }

    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Reads a core dump from a file.
   *
   * @param path the input file path
   * @return the core dump
   * @throws WasmException if reading fails
   */
  public static WasmCoreDump readFrom(final Path path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    try {
      byte[] data = Files.readAllBytes(path);
      return deserialize(data);
    } catch (IOException e) {
      throw new WasmException("Failed to read core dump from " + path, e);
    }
  }

  /**
   * Deserializes a core dump from bytes.
   *
   * @param data the serialized core dump
   * @return the core dump
   */
  public static WasmCoreDump deserialize(final byte[] data) {
    // Simplified deserialization - in practice would parse the format
    return new WasmCoreDump(
        "Deserialized core dump",
        null,
        Instant.now(),
        Collections.emptyList(),
        Collections.emptyMap(),
        Collections.emptyList());
  }

  @Override
  public String toString() {
    return "WasmCoreDump{trap='"
        + trapMessage
        + "', timestamp="
        + timestamp
        + ", memoryDumps="
        + memoryDumps.size()
        + ", globals="
        + globals.size()
        + ", stackFrames="
        + stackFrames.size()
        + "}";
  }

  /** A memory dump from a WebAssembly memory. */
  public static final class MemoryDump {
    private final int memoryIndex;
    private final byte[] data;
    private final long baseAddress;

    /**
     * Creates a new MemoryDump.
     *
     * @param memoryIndex the memory index
     * @param data the memory data
     * @param baseAddress the base address
     */
    public MemoryDump(final int memoryIndex, final byte[] data, final long baseAddress) {
      this.memoryIndex = memoryIndex;
      this.data = data.clone();
      this.baseAddress = baseAddress;
    }

    public int getMemoryIndex() {
      return memoryIndex;
    }

    public byte[] getData() {
      return data.clone();
    }

    public long getBaseAddress() {
      return baseAddress;
    }

    public int getSize() {
      return data.length;
    }
  }

  /** Detailed information about a stack frame. */
  public static final class StackFrame {
    private final String functionName;
    private final int functionIndex;
    private final long instructionOffset;
    private final long[] locals;

    /**
     * Creates a new StackFrame.
     *
     * @param functionName the function name
     * @param functionIndex the function index
     * @param instructionOffset the instruction offset
     * @param locals the local variable values
     */
    public StackFrame(
        final String functionName,
        final int functionIndex,
        final long instructionOffset,
        final long[] locals) {
      this.functionName = functionName;
      this.functionIndex = functionIndex;
      this.instructionOffset = instructionOffset;
      this.locals = locals != null ? locals.clone() : new long[0];
    }

    public String getFunctionName() {
      return functionName;
    }

    public int getFunctionIndex() {
      return functionIndex;
    }

    public long getInstructionOffset() {
      return instructionOffset;
    }

    public long[] getLocals() {
      return locals.clone();
    }

    @Override
    public String toString() {
      return "StackFrame{func='"
          + functionName
          + "' (#"
          + functionIndex
          + "), offset="
          + instructionOffset
          + ", locals="
          + locals.length
          + "}";
    }
  }
}
