package ai.tegmentum.wasmtime4j.panama.util;

import ai.tegmentum.wasmtime4j.debug.FrameInfo;
import ai.tegmentum.wasmtime4j.debug.FrameSymbol;
import ai.tegmentum.wasmtime4j.debug.WasmBacktrace;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for deserializing backtrace data from native FFI.
 *
 * <p>Deserializes binary backtrace format produced by Panama FFI backtrace capture functions. The
 * format matches the serialization in panama_ffi.rs:
 *
 * <pre>
 * Backtrace: [frame_count: u32][force_capture: u8][frames...]
 * Frame: [func_index: u32][has_func_name: u8][func_name_len: u32][func_name: bytes]
 *        [has_module_offset: u8][module_offset: u32][has_func_offset: u8][func_offset: u32]
 *        [symbol_count: u32][symbols...]
 * Symbol: [has_name: u8][name_len: u32][name: bytes][has_file: u8][file_len: u32][file: bytes]
 *         [has_line: u8][line: u32][has_column: u8][column: u32]
 * </pre>
 *
 * @since 1.0.0
 */
public final class BacktraceDeserializer {

  private BacktraceDeserializer() {
    // Utility class
  }

  /**
   * Deserializes a backtrace from binary format.
   *
   * @param data the binary backtrace data
   * @return the deserialized WasmBacktrace
   * @throws IllegalArgumentException if the data is invalid
   */
  public static WasmBacktrace deserialize(final byte[] data) {
    if (data == null || data.length < 5) {
      throw new IllegalArgumentException("Invalid backtrace data: too short");
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

    // Read header
    final int frameCount = buffer.getInt();
    final boolean forceCapture = buffer.get() != 0;

    // Read frames
    final List<FrameInfo> frames = new ArrayList<>(frameCount);
    for (int i = 0; i < frameCount; i++) {
      frames.add(deserializeFrame(buffer));
    }

    return new WasmBacktrace(frames, forceCapture);
  }

  /**
   * Deserializes a single frame from the buffer.
   *
   * @param buffer the byte buffer
   * @return the deserialized FrameInfo
   */
  private static FrameInfo deserializeFrame(final ByteBuffer buffer) {
    // Function index
    final int funcIndex = buffer.getInt();

    // Function name (optional)
    final String funcName;
    if (buffer.get() != 0) {
      final int nameLen = buffer.getInt();
      final byte[] nameBytes = new byte[nameLen];
      buffer.get(nameBytes);
      funcName = new String(nameBytes, StandardCharsets.UTF_8);
    } else {
      funcName = null;
    }

    // Module offset (optional)
    final Integer moduleOffset;
    if (buffer.get() != 0) {
      moduleOffset = buffer.getInt();
    } else {
      moduleOffset = null;
    }

    // Function offset (optional)
    final Integer funcOffset;
    if (buffer.get() != 0) {
      funcOffset = buffer.getInt();
    } else {
      funcOffset = null;
    }

    // Symbols
    final int symbolCount = buffer.getInt();
    final List<FrameSymbol> symbols = new ArrayList<>(symbolCount);
    for (int i = 0; i < symbolCount; i++) {
      symbols.add(deserializeSymbol(buffer));
    }

    // Module is not serialized (would require module handle), pass null
    return new FrameInfo(funcIndex, null, funcName, moduleOffset, funcOffset, symbols);
  }

  /**
   * Deserializes a single symbol from the buffer.
   *
   * @param buffer the byte buffer
   * @return the deserialized FrameSymbol
   */
  private static FrameSymbol deserializeSymbol(final ByteBuffer buffer) {
    // Symbol name (optional)
    final String name;
    if (buffer.get() != 0) {
      final int nameLen = buffer.getInt();
      final byte[] nameBytes = new byte[nameLen];
      buffer.get(nameBytes);
      name = new String(nameBytes, StandardCharsets.UTF_8);
    } else {
      name = null;
    }

    // Source file (optional)
    final String file;
    if (buffer.get() != 0) {
      final int fileLen = buffer.getInt();
      final byte[] fileBytes = new byte[fileLen];
      buffer.get(fileBytes);
      file = new String(fileBytes, StandardCharsets.UTF_8);
    } else {
      file = null;
    }

    // Line number (optional)
    final Integer line;
    if (buffer.get() != 0) {
      line = buffer.getInt();
    } else {
      line = null;
    }

    // Column number (optional)
    final Integer column;
    if (buffer.get() != 0) {
      column = buffer.getInt();
    } else {
      column = null;
    }

    return new FrameSymbol(name, file, line, column);
  }
}
