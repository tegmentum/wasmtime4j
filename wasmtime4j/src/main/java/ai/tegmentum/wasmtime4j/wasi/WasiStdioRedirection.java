package ai.tegmentum.wasmtime4j.wasi;

/**
 * Configuration for redirecting standard I/O streams (stdin, stdout, stderr) in WASI processes.
 *
 * <p>This class defines how standard I/O streams should be handled when spawning a new process,
 * including inheritance from parent, redirection to files, or capture to buffers.
 *
 * @since 1.0.0
 */
public final class WasiStdioRedirection {

  /** Type of stdio redirection. */
  public enum Type {
    /** Inherit the stream from the parent process. */
    INHERIT,
    /** Redirect to a file. */
    FILE,
    /** Capture to a buffer. */
    BUFFER,
    /** Discard all data (null device). */
    NULL
  }

  private final Type type;
  private final String filePath;
  private final byte[] bufferData;

  private WasiStdioRedirection(final Type type, final String filePath, final byte[] bufferData) {
    this.type = type;
    this.filePath = filePath;
    this.bufferData = bufferData;
  }

  /**
   * Gets the type of redirection.
   *
   * @return the redirection type
   */
  public Type getType() {
    return type;
  }

  /**
   * Gets the file path for FILE redirection.
   *
   * @return the file path, or null if not applicable
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Gets the buffer data for BUFFER redirection (input only).
   *
   * @return the buffer data, or null if not applicable
   */
  public byte[] getBufferData() {
    return bufferData;
  }

  /**
   * Creates a redirection that inherits from the parent process.
   *
   * @return a new inherit redirection
   */
  public static WasiStdioRedirection inherit() {
    return new WasiStdioRedirection(Type.INHERIT, null, null);
  }

  /**
   * Creates a redirection to a file.
   *
   * @param filePath the path to the file
   * @return a new file redirection
   */
  public static WasiStdioRedirection file(final String filePath) {
    return new WasiStdioRedirection(Type.FILE, filePath, null);
  }

  /**
   * Creates a redirection to capture output to a buffer.
   *
   * <p>This is only valid for stdout and stderr.
   *
   * @return a new buffer redirection
   */
  public static WasiStdioRedirection buffer() {
    return new WasiStdioRedirection(Type.BUFFER, null, null);
  }

  /**
   * Creates a redirection from a buffer (for stdin).
   *
   * <p>This is only valid for stdin.
   *
   * @param data the input data
   * @return a new buffer redirection
   */
  public static WasiStdioRedirection buffer(final byte[] data) {
    return new WasiStdioRedirection(Type.BUFFER, null, data.clone());
  }

  /**
   * Creates a redirection that discards all data.
   *
   * @return a new null redirection
   */
  public static WasiStdioRedirection nullDevice() {
    return new WasiStdioRedirection(Type.NULL, null, null);
  }

  @Override
  public String toString() {
    switch (type) {
      case INHERIT:
        return "inherit";
      case FILE:
        return "file(" + filePath + ")";
      case BUFFER:
        if (bufferData != null) {
          return "buffer(input, " + bufferData.length + " bytes)";
        } else {
          return "buffer(capture)";
        }
      case NULL:
        return "null";
      default:
        return "unknown";
    }
  }
}
