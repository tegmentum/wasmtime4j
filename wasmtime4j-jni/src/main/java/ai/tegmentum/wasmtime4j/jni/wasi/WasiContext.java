package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.WasiContextData;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI context management.
 *
 * <p>This class provides comprehensive WASI (WebAssembly System Interface) context management for
 * JNI-based WebAssembly execution. It includes:
 *
 * <ul>
 *   <li>Configurable permission system with fine-grained controls
 *   <li>Security validation and path traversal protection
 *   <li>Resource limiting and quota enforcement
 *   <li>Thread-safe context management
 *   <li>Comprehensive error mapping for system operation failures
 * </ul>
 *
 * <p>Security is the highest priority - all file system operations are validated against sandbox
 * permissions and checked for path traversal attacks before execution.
 *
 * @since 1.0.0
 */
public final class WasiContext extends JniResource {

  private static final Logger LOGGER = Logger.getLogger(WasiContext.class.getName());

  /** Shared WASI context data (environment, arguments, preopen dirs, working dir). */
  private final WasiContextData contextData;

  /**
   * Creates a new WASI context with the specified configuration.
   *
   * @param nativeHandle the native handle for the WASI context
   * @param builder the WASI context builder with configuration
   * @throws JniException if the WASI context cannot be created
   */
  WasiContext(final long nativeHandle, final WasiContextBuilder builder) {
    super(nativeHandle);

    Validation.requireNonNull(builder, "builder");

    this.contextData =
        new WasiContextData(
            builder.getEnvironment(),
            builder.getArguments(),
            builder.getPreopenedDirectories(),
            builder.getWorkingDirectory());

    LOGGER.info(
        String.format(
            "Created WASI context with %d preopen directories, %d environment variables, %d"
                + " arguments",
            contextData.getPreopenedDirectoryCount(),
            contextData.getEnvironmentCount(),
            contextData.getArgumentCount()));
  }

  /**
   * Gets an environment variable value.
   *
   * @param name the environment variable name
   * @return the environment variable value, or null if not set
   * @throws JniException if the context is closed or name is invalid
   */
  public String getEnvironmentVariable(final String name) {
    ensureNotClosed();
    Validation.requireNonEmpty(name, "name");
    return contextData.getEnvironmentVariable(name);
  }

  /**
   * Gets all environment variables.
   *
   * @return a copy of all environment variables
   * @throws JniException if the context is closed
   */
  public Map<String, String> getEnvironment() {
    ensureNotClosed();
    return contextData.getEnvironment();
  }

  /**
   * Gets the command-line arguments.
   *
   * @return a copy of the command-line arguments
   * @throws JniException if the context is closed
   */
  public String[] getArguments() {
    ensureNotClosed();
    return contextData.getArguments();
  }

  /**
   * Gets all pre-opened directories.
   *
   * @return a copy of all pre-opened directories
   * @throws JniException if the context is closed
   */
  public Map<String, Path> getPreopenedDirectories() {
    ensureNotClosed();
    return contextData.getPreopenedDirectories();
  }

  /**
   * Gets the working directory.
   *
   * @return the working directory
   * @throws JniException if the context is closed
   */
  public Path getWorkingDirectory() {
    ensureNotClosed();
    return contextData.getWorkingDirectory();
  }

  /**
   * Validates that a file path is accessible within the sandbox.
   *
   * <p>Security validation checks for path traversal attacks. Filesystem sandboxing is enforced by
   * the Wasmtime native runtime through pre-opened directory configuration.
   *
   * @param path the file path to validate
   * @return the resolved and validated path
   * @throws JniException if the path is not accessible or validation fails
   */
  public Path validatePath(final String path) {
    ensureNotClosed();
    Validation.requireNonEmpty(path, "path");
    return contextData.validatePath(path);
  }

  /**
   * Checks if this WASI context is valid and usable.
   *
   * <p>A context is valid if it has a non-zero native handle and has not been closed.
   *
   * @return true if the context is valid, false otherwise
   */
  public boolean isValid() {
    return nativeHandle != 0 && !isClosed();
  }

  /**
   * Creates a new WASI context builder.
   *
   * @return a new WASI context builder
   */
  public static WasiContextBuilder builder() {
    return new WasiContextBuilder();
  }

  @Override
  protected void doClose() throws Exception {
    LOGGER.fine("Closing WASI context with handle: 0x" + Long.toHexString(nativeHandle));

    // Call native cleanup
    nativeClose(nativeHandle);

    // Clear local state
    contextData.clearState();

    LOGGER.info("WASI context closed successfully");
  }

  @Override
  protected String getResourceType() {
    return "WasiContext";
  }

  /**
   * Native method to close the WASI context.
   *
   * @param handle the native handle for the WASI context
   */
  private static native void nativeClose(long handle);

  /**
   * Native method to create a new WASI context.
   *
   * @param environment environment variables as key=value pairs
   * @param arguments command-line arguments
   * @param preopenDirs pre-opened directory mappings
   * @param workingDir working directory path
   * @return the native handle for the created WASI context
   * @throws JniException if the context cannot be created
   */
  static native long nativeCreate(
      String[] environment, String[] arguments, String[] preopenDirs, String workingDir)
      throws JniException;

  /**
   * Native method to add a directory mapping to the WASI context.
   *
   * @param handle the native handle for the WASI context
   * @param hostPath the host filesystem path
   * @param guestPath the guest filesystem path
   * @param canRead whether reading is allowed
   * @param canWrite whether writing is allowed
   * @param canCreate whether file creation is allowed
   * @return true if successful, false otherwise
   * @throws JniException if the operation fails
   */
  static native boolean nativeAddDirectory(
      long handle,
      String hostPath,
      String guestPath,
      boolean canRead,
      boolean canWrite,
      boolean canCreate)
      throws JniException;

  /**
   * Native method to set an environment variable in the WASI context.
   *
   * @param handle the native handle for the WASI context
   * @param key the environment variable key
   * @param value the environment variable value
   * @return true if successful, false otherwise
   * @throws JniException if the operation fails
   */
  static native boolean nativeSetEnvironmentVariable(long handle, String key, String value)
      throws JniException;

  /**
   * Native method to check if a path is allowed by the WASI context.
   *
   * @param handle the native handle for the WASI context
   * @param path the path to check
   * @return true if allowed, false otherwise
   * @throws JniException if the operation fails
   */
  static native boolean nativeIsPathAllowed(long handle, String path) throws JniException;

  /**
   * Native method to get the number of environment variables.
   *
   * @param handle the native handle for the WASI context
   * @return the number of environment variables
   * @throws JniException if the operation fails
   */
  static native int nativeGetEnvironmentCount(long handle) throws JniException;

  /**
   * Native method to get the number of command line arguments.
   *
   * @param handle the native handle for the WASI context
   * @return the number of arguments
   * @throws JniException if the operation fails
   */
  static native int nativeGetArgumentCount(long handle) throws JniException;

  /**
   * Native method to get the number of directory mappings.
   *
   * @param handle the native handle for the WASI context
   * @return the number of directory mappings
   * @throws JniException if the operation fails
   */
  static native int nativeGetDirectoryCount(long handle) throws JniException;
}
