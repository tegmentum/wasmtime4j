package ai.tegmentum.wasmtime4j.wasi;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Configuration for WASI standard I/O streams (stdin, stdout, stderr).
 *
 * <p>This class provides various options for configuring how WebAssembly modules access standard
 * input, output, and error streams. Streams can be:
 *
 * <ul>
 *   <li>Inherited from the host process
 *   <li>Connected to Java streams
 *   <li>Redirected to files
 *   <li>Nulled (discarded for output, empty for input)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Inherit stdout and stderr from host, null stdin
 * WasiStdioConfig stdout = WasiStdioConfig.inherit();
 * WasiStdioConfig stderr = WasiStdioConfig.inherit();
 * WasiStdioConfig stdin = WasiStdioConfig.nulled();
 *
 * linker.configureStdout(stdout);
 * linker.configureStderr(stderr);
 * linker.configureStdin(stdin);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiStdioConfig {
  private final Type type;
  private final Object target;

  private WasiStdioConfig(final Type type, final Object target) {
    this.type = type;
    this.target = target;
  }

  /**
   * Creates a configuration that inherits the stream from the host process.
   *
   * <p>This is typically used for stdout and stderr to display output in the host console, or for
   * stdin to read from the host console.
   *
   * @return a new configuration for inherited stream
   */
  public static WasiStdioConfig inherit() {
    return new WasiStdioConfig(Type.INHERIT, null);
  }

  /**
   * Creates a configuration that connects to a Java InputStream.
   *
   * <p>This is typically used for stdin to provide input from a Java stream.
   *
   * @param inputStream the Java InputStream to read from
   * @return a new configuration for the input stream
   * @throws IllegalArgumentException if inputStream is null
   */
  public static WasiStdioConfig fromInputStream(final InputStream inputStream) {
    if (inputStream == null) {
      throw new IllegalArgumentException("InputStream cannot be null");
    }
    return new WasiStdioConfig(Type.INPUT_STREAM, inputStream);
  }

  /**
   * Creates a configuration that connects to a Java OutputStream.
   *
   * <p>This is typically used for stdout or stderr to capture output in a Java stream.
   *
   * @param outputStream the Java OutputStream to write to
   * @return a new configuration for the output stream
   * @throws IllegalArgumentException if outputStream is null
   */
  public static WasiStdioConfig fromOutputStream(final OutputStream outputStream) {
    if (outputStream == null) {
      throw new IllegalArgumentException("OutputStream cannot be null");
    }
    return new WasiStdioConfig(Type.OUTPUT_STREAM, outputStream);
  }

  /**
   * Creates a configuration that redirects to a file.
   *
   * <p>For input streams, the file will be opened for reading. For output streams, the file will be
   * created or truncated for writing.
   *
   * @param filePath the path to the file to use
   * @return a new configuration for file redirection
   * @throws IllegalArgumentException if filePath is null
   */
  public static WasiStdioConfig fromFile(final Path filePath) {
    if (filePath == null) {
      throw new IllegalArgumentException("File path cannot be null");
    }
    return new WasiStdioConfig(Type.FILE, filePath);
  }

  /**
   * Creates a configuration that redirects to a file, appending if it exists.
   *
   * <p>This is only valid for output streams. The file will be opened in append mode, preserving
   * existing content.
   *
   * @param filePath the path to the file to append to
   * @return a new configuration for file appending
   * @throws IllegalArgumentException if filePath is null
   */
  public static WasiStdioConfig appendToFile(final Path filePath) {
    if (filePath == null) {
      throw new IllegalArgumentException("File path cannot be null");
    }
    return new WasiStdioConfig(Type.FILE_APPEND, filePath);
  }

  /**
   * Creates a configuration that nulls the stream.
   *
   * <p>For input streams, this provides an empty stream (immediate EOF). For output streams, this
   * discards all output.
   *
   * @return a new configuration for nulled stream
   */
  public static WasiStdioConfig nulled() {
    return new WasiStdioConfig(Type.NULL, null);
  }

  /**
   * Gets the type of this stdio configuration.
   *
   * @return the configuration type
   */
  public Type getType() {
    return type;
  }

  /**
   * Gets the target object for this configuration.
   *
   * <p>The type of object depends on the configuration type:
   *
   * <ul>
   *   <li>INHERIT, NULL: null
   *   <li>INPUT_STREAM: InputStream
   *   <li>OUTPUT_STREAM: OutputStream
   *   <li>FILE, FILE_APPEND: Path
   * </ul>
   *
   * @return the target object, or null for inherit/null types
   */
  public Object getTarget() {
    return target;
  }

  /**
   * Gets the target as an InputStream (only valid for INPUT_STREAM type).
   *
   * @return the InputStream target
   * @throws IllegalStateException if this is not an INPUT_STREAM configuration
   */
  public InputStream getInputStream() {
    if (type != Type.INPUT_STREAM) {
      throw new IllegalStateException("Not an input stream configuration");
    }
    return (InputStream) target;
  }

  /**
   * Gets the target as an OutputStream (only valid for OUTPUT_STREAM type).
   *
   * @return the OutputStream target
   * @throws IllegalStateException if this is not an OUTPUT_STREAM configuration
   */
  public OutputStream getOutputStream() {
    if (type != Type.OUTPUT_STREAM) {
      throw new IllegalStateException("Not an output stream configuration");
    }
    return (OutputStream) target;
  }

  /**
   * Gets the target as a file Path (only valid for FILE or FILE_APPEND types).
   *
   * @return the file Path target
   * @throws IllegalStateException if this is not a file configuration
   */
  public Path getFilePath() {
    if (type != Type.FILE && type != Type.FILE_APPEND) {
      throw new IllegalStateException("Not a file configuration");
    }
    return (Path) target;
  }

  @Override
  public String toString() {
    return "WasiStdioConfig{type=" + type + ", target=" + target + "}";
  }

  /** Types of WASI stdio configurations. */
  public enum Type {
    /** Inherit the stream from the host process. */
    INHERIT,
    /** Connect to a Java InputStream. */
    INPUT_STREAM,
    /** Connect to a Java OutputStream. */
    OUTPUT_STREAM,
    /** Redirect to a file (create/truncate). */
    FILE,
    /** Redirect to a file (append). */
    FILE_APPEND,
    /** Null the stream (empty input, discard output). */
    NULL
  }
}
