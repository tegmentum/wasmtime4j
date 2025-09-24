package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.permission.WasiPermissionManager;
import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityValidator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

  /** Permission manager for controlling WASI capabilities. */
  private final WasiPermissionManager permissionManager;

  /** Security validator for preventing unauthorized access. */
  private final WasiSecurityValidator securityValidator;

  /** Environment variables for the WASI context. */
  private final Map<String, String> environment;

  /** Command-line arguments for the WASI context. */
  private final String[] arguments;

  /** Pre-opened directories with their sandbox permissions. */
  private final Map<String, Path> preopenedDirectories;

  /** Working directory for the WASI context. */
  private final Path workingDirectory;

  /**
   * Creates a new WASI context with the specified configuration.
   *
   * @param nativeHandle the native handle for the WASI context
   * @param builder the WASI context builder with configuration
   * @throws JniException if the WASI context cannot be created
   */
  WasiContext(final long nativeHandle, final WasiContextBuilder builder) {
    super(nativeHandle);

    JniValidation.requireNonNull(builder, "builder");

    this.permissionManager = builder.getPermissionManager();
    this.securityValidator = builder.getSecurityValidator();
    this.environment = new ConcurrentHashMap<>(builder.getEnvironment());
    this.arguments = builder.getArguments().toArray(new String[0]);
    this.preopenedDirectories = new ConcurrentHashMap<>(builder.getPreopenedDirectories());
    this.workingDirectory = builder.getWorkingDirectory();

    LOGGER.info(
        String.format(
            "Created WASI context with %d preopen directories, %d environment variables, %d"
                + " arguments",
            preopenedDirectories.size(), environment.size(), arguments.length));
  }

  /**
   * Gets the permission manager for this WASI context.
   *
   * @return the permission manager
   */
  public WasiPermissionManager getPermissionManager() {
    ensureNotClosed();
    return permissionManager;
  }

  /**
   * Gets the security validator for this WASI context.
   *
   * @return the security validator
   */
  public WasiSecurityValidator getSecurityValidator() {
    ensureNotClosed();
    return securityValidator;
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
    JniValidation.requireNonEmpty(name, "name");

    // Validate access to environment variable
    securityValidator.validateEnvironmentAccess(name);

    return environment.get(name);
  }

  /**
   * Gets all environment variables.
   *
   * @return a copy of all environment variables
   * @throws JniException if the context is closed
   */
  public Map<String, String> getEnvironment() {
    ensureNotClosed();

    // Return defensive copy
    return new ConcurrentHashMap<>(environment);
  }

  /**
   * Gets the command-line arguments.
   *
   * @return a copy of the command-line arguments
   * @throws JniException if the context is closed
   */
  public String[] getArguments() {
    ensureNotClosed();

    // Return defensive copy
    return arguments.clone();
  }

  /**
   * Gets all pre-opened directories.
   *
   * @return a copy of all pre-opened directories
   * @throws JniException if the context is closed
   */
  public Map<String, Path> getPreopenedDirectories() {
    ensureNotClosed();

    // Return defensive copy
    return new ConcurrentHashMap<>(preopenedDirectories);
  }

  /**
   * Gets the working directory.
   *
   * @return the working directory
   * @throws JniException if the context is closed
   */
  public Path getWorkingDirectory() {
    ensureNotClosed();
    return workingDirectory;
  }

  /**
   * Validates that a file path is accessible within the sandbox.
   *
   * @param path the file path to validate
   * @return the resolved and validated path
   * @throws JniException if the path is not accessible or validation fails
   */
  public Path validatePath(final String path) {
    ensureNotClosed();
    JniValidation.requireNonEmpty(path, "path");

    final Path resolvedPath = Paths.get(path);

    // Security validation - check for path traversal attacks
    securityValidator.validatePath(resolvedPath);

    // Permission validation - check sandbox access
    permissionManager.validateFileSystemAccess(resolvedPath);

    return resolvedPath.normalize().toAbsolutePath();
  }

  /**
   * Validates that a file path is accessible for the specified operation.
   *
   * @param path the file path to validate
   * @param operation the file operation type
   * @return the resolved and validated path
   * @throws JniException if the path is not accessible or validation fails
   */
  public Path validatePath(final String path, final WasiFileOperation operation) {
    ensureNotClosed();
    JniValidation.requireNonEmpty(path, "path");
    JniValidation.requireNonNull(operation, "operation");

    final Path resolvedPath = validatePath(path);

    // Additional permission validation for specific operation
    permissionManager.validateFileSystemAccess(resolvedPath, operation);

    return resolvedPath;
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
    environment.clear();
    preopenedDirectories.clear();

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
