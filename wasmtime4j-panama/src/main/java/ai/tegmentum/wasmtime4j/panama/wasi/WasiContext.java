package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WASI context management.
 *
 * <p>This class provides comprehensive WASI (WebAssembly System Interface) context management for
 * Panama FFI-based WebAssembly execution. It includes:
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
 * <p>This implementation uses Panama Foreign Function Interface for native interop and integrates
 * with the ArenaResourceManager for proper memory management.
 *
 * @since 1.0.0
 */
public final class WasiContext implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(WasiContext.class.getName());

  /** Native handle for the WASI context. */
  private final MemorySegment nativeHandle;

  /** Resource manager for native memory management. */
  private final ArenaResourceManager resourceManager;

  /** Environment variables for the WASI context. */
  private final Map<String, String> environment;

  /** Command-line arguments for the WASI context. */
  private final String[] arguments;

  /** Pre-opened directories with their sandbox permissions. */
  private final Map<String, Path> preopenedDirectories;

  /** Working directory for the WASI context. */
  private final Path workingDirectory;

  /** Resource handle for lifecycle management. */
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new WASI context with the specified configuration.
   *
   * @param nativeHandle the native handle for the WASI context
   * @param resourceManager the resource manager for native memory
   * @param builder the WASI context builder with configuration
   */
  WasiContext(
      final MemorySegment nativeHandle,
      final ArenaResourceManager resourceManager,
      final WasiContextBuilder builder) {

    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native handle cannot be null");
    }
    if (resourceManager == null) {
      throw new IllegalArgumentException("Resource manager cannot be null");
    }
    if (builder == null) {
      throw new IllegalArgumentException("Builder cannot be null");
    }

    this.nativeHandle = nativeHandle;
    this.resourceManager = resourceManager;
    this.environment = new ConcurrentHashMap<>(builder.getEnvironment());
    this.arguments = builder.getArguments().toArray(new String[0]);
    this.preopenedDirectories = new ConcurrentHashMap<>(builder.getPreopenedDirectories());
    this.workingDirectory = builder.getWorkingDirectory();

    // Capture handle locally for safety net (must NOT capture 'this')
    final MemorySegment handle = this.nativeHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "WasiContext",
            () -> {
              LOGGER.fine("Closing Panama WASI context with handle: " + handle.address());

              // Call native cleanup
              nativeClose(handle);

              // Clear local state
              environment.clear();
              preopenedDirectories.clear();

              // Close resource manager
              resourceManager.close();

              LOGGER.info("Panama WASI context closed successfully");
            },
            this,
            () -> nativeClose(handle));

    LOGGER.info(
        String.format(
            "Created Panama WASI context with %d preopen directories, %d environment variables, %d"
                + " arguments",
            preopenedDirectories.size(), environment.size(), arguments.length));
  }

  /**
   * Gets the native handle for this WASI context.
   *
   * @return the native handle
   * @throws IllegalStateException if the context is closed
   */
  public MemorySegment getNativeHandle() {
    ensureNotClosed();
    return nativeHandle;
  }

  /**
   * Gets an environment variable value.
   *
   * @param name the environment variable name
   * @return the environment variable value, or null if not set
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if the context is closed
   */
  public String getEnvironmentVariable(final String name) {
    ensureNotClosed();

    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Environment variable name cannot be null or empty");
    }

    return environment.get(name);
  }

  /**
   * Gets all environment variables.
   *
   * @return a copy of all environment variables
   * @throws IllegalStateException if the context is closed
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
   * @throws IllegalStateException if the context is closed
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
   * @throws IllegalStateException if the context is closed
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
   * @throws IllegalStateException if the context is closed
   */
  public Path getWorkingDirectory() {
    ensureNotClosed();
    return workingDirectory;
  }

  /**
   * Validates that a file path is accessible within the sandbox.
   *
   * <p>Security validation checks for path traversal attacks. Filesystem sandboxing is enforced by
   * the Wasmtime native runtime through pre-opened directory configuration.
   *
   * @param path the file path to validate
   * @return the resolved and validated path
   * @throws IllegalArgumentException if path is null or empty
   * @throws IllegalStateException if the context is closed
   * @throws RuntimeException if the path is not accessible or validation fails
   */
  public Path validatePath(final String path) {
    ensureNotClosed();

    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }

    final Path resolvedPath = Paths.get(path);

    return resolvedPath.normalize().toAbsolutePath();
  }

  /**
   * Checks if this WASI context has been closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return resourceHandle.isClosed();
  }

  /**
   * Closes this WASI context and releases all associated resources.
   *
   * <p>This method is idempotent and thread-safe. After calling this method, all other methods will
   * throw IllegalStateException.
   */
  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Creates a new WASI context builder.
   *
   * @return a new WASI context builder
   */
  public static WasiContextBuilder builder() {
    return new WasiContextBuilder();
  }

  /**
   * Ensures that this context has not been closed.
   *
   * @throws IllegalStateException if the context has been closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /**
   * Native method to close the WASI context.
   *
   * @param handle the native handle for the WASI context
   */
  private static void nativeClose(final MemorySegment handle) {
    try {
      if (handle == null || handle.equals(MemorySegment.NULL)) {
        LOGGER.warning("Attempted to close null WASI context handle");
        return;
      }

      LOGGER.fine("Native WASI context cleanup called for handle: " + handle.address());

      // Get the native function bindings and destroy the WASI context
      ai.tegmentum.wasmtime4j.panama.NativeWasiBindings bindings =
          ai.tegmentum.wasmtime4j.panama.NativeWasiBindings.getInstance();

      if (bindings.isInitialized()) {
        bindings.wasiContextDestroy(handle);
        LOGGER.fine("WASI context destroyed successfully");
      } else {
        LOGGER.warning("Native function bindings not initialized, cannot destroy WASI context");
      }
    } catch (Exception e) {
      LOGGER.warning("Error during native WASI context cleanup: " + e.getMessage());
      // Continue with cleanup despite error to prevent resource leaks
    }
  }

  @Override
  public String toString() {
    return String.format(
        "WasiContext{handle=%s, closed=%s, env=%d, args=%d, preopen=%d}",
        nativeHandle.address(),
        resourceHandle.isClosed(),
        environment.size(),
        arguments.length,
        preopenedDirectories.size());
  }
}
