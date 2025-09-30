package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.nio.file.Path;
import java.util.Map;

/**
 * Configuration context for WASI (WebAssembly System Interface) functionality.
 *
 * <p>WasiContext allows configuring the WASI environment including command-line arguments,
 * environment variables, file system access, and standard I/O redirection.
 *
 * <p>WASI provides a standardized interface for WebAssembly modules to interact with the host
 * system in a secure and portable way.
 *
 * @since 1.0.0
 */
public interface WasiContext {

  /**
   * Sets the command-line arguments for the WASI module.
   *
   * <p>These arguments will be available to the WebAssembly module through the standard WASI
   * args_get and args_sizes_get functions.
   *
   * @param argv the command-line arguments
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if argv is null
   * @since 1.0.0
   */
  WasiContext setArgv(String[] argv);

  /**
   * Sets a single environment variable for the WASI module.
   *
   * <p>Environment variables are available to the WebAssembly module through the WASI environ_get
   * and environ_sizes_get functions.
   *
   * @param key the environment variable name
   * @param value the environment variable value
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if key or value is null
   * @since 1.0.0
   */
  WasiContext setEnv(String key, String value);

  /**
   * Sets multiple environment variables at once.
   *
   * @param env a map of environment variable names to values
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if env is null or contains null keys/values
   * @since 1.0.0
   */
  WasiContext setEnv(Map<String, String> env);

  /**
   * Inherits the current process's environment variables.
   *
   * <p>This makes all environment variables from the host process available to the WebAssembly
   * module.
   *
   * @return this WasiContext for method chaining
   * @since 1.0.0
   */
  WasiContext inheritEnv();

  /**
   * Configures the WASI module to inherit the host's standard I/O streams.
   *
   * <p>This connects the WebAssembly module's stdin, stdout, and stderr to the host process's
   * corresponding streams.
   *
   * @return this WasiContext for method chaining
   * @since 1.0.0
   */
  WasiContext inheritStdio();

  /**
   * Redirects stdin to read from the specified file.
   *
   * @param path the file to read stdin from
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if path is null
   * @since 1.0.0
   */
  WasiContext setStdin(Path path);

  /**
   * Redirects stdout to write to the specified file.
   *
   * @param path the file to write stdout to
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if path is null
   * @since 1.0.0
   */
  WasiContext setStdout(Path path);

  /**
   * Redirects stderr to write to the specified file.
   *
   * @param path the file to write stderr to
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if path is null
   * @since 1.0.0
   */
  WasiContext setStderr(Path path);

  /**
   * Grants the WASI module access to a directory on the host file system.
   *
   * <p>The host directory will be mapped to the specified guest path, allowing the WebAssembly
   * module to access files within that directory through WASI file system functions.
   *
   * @param hostPath the path on the host file system to grant access to
   * @param guestPath the path that the WebAssembly module will use to access the directory
   * @return this WasiContext for method chaining
   * @throws WasmException if the directory cannot be accessed or mapped
   * @throws IllegalArgumentException if hostPath or guestPath is null
   * @since 1.0.0
   */
  WasiContext preopenedDir(Path hostPath, String guestPath) throws WasmException;

  /**
   * Grants read-only access to a directory on the host file system.
   *
   * @param hostPath the path on the host file system to grant read access to
   * @param guestPath the path that the WebAssembly module will use to access the directory
   * @return this WasiContext for method chaining
   * @throws WasmException if the directory cannot be accessed or mapped
   * @throws IllegalArgumentException if hostPath or guestPath is null
   * @since 1.0.0
   */
  WasiContext preopenedDirReadOnly(Path hostPath, String guestPath) throws WasmException;

  /**
   * Sets the working directory for the WASI module.
   *
   * <p>This affects relative path resolution within the WebAssembly module.
   *
   * @param workingDir the working directory path
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if workingDir is null
   * @since 1.0.0
   */
  WasiContext setWorkingDirectory(String workingDir);

  /**
   * Enables or disables network access for the WASI module.
   *
   * <p>When enabled, the WebAssembly module can use WASI networking functions to create sockets and
   * connect to remote hosts.
   *
   * @param enabled true to enable network access, false to disable
   * @return this WasiContext for method chaining
   * @since 1.0.0
   */
  WasiContext setNetworkEnabled(boolean enabled);

  /**
   * Sets the maximum number of open file descriptors allowed.
   *
   * <p>This provides resource limiting to prevent WebAssembly modules from exhausting system file
   * descriptors.
   *
   * @param maxFds the maximum number of file descriptors, or -1 for unlimited
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if maxFds is less than -1
   * @since 1.0.0
   */
  WasiContext setMaxOpenFiles(int maxFds);

  /**
   * Enables or disables async I/O operations for WASI Preview 2.
   *
   * <p>When enabled, I/O operations can be performed asynchronously with proper cancellation and
   * timeout support. This is a Preview 2 specific feature.
   *
   * @param enabled true to enable async I/O, false to disable
   * @return this WasiContext for method chaining
   * @since 1.0.0
   */
  WasiContext setAsyncIoEnabled(boolean enabled);

  /**
   * Sets the maximum number of concurrent async operations allowed.
   *
   * <p>This provides resource limiting for async operations in WASI Preview 2.
   *
   * @param maxOps the maximum number of concurrent async operations, or -1 for unlimited
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if maxOps is less than -1
   * @since 1.0.0
   */
  WasiContext setMaxAsyncOperations(int maxOps);

  /**
   * Sets the default timeout for async operations in milliseconds.
   *
   * <p>This timeout applies to async I/O operations in WASI Preview 2 when no specific timeout is
   * provided.
   *
   * @param timeoutMs the timeout in milliseconds, or -1 for no timeout
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if timeoutMs is less than -1
   * @since 1.0.0
   */
  WasiContext setAsyncTimeout(long timeoutMs);

  /**
   * Enables or disables Component Model support for WASI Preview 2.
   *
   * <p>When enabled, the WASI context can work with WebAssembly components and provide enhanced
   * resource management through the Component Model.
   *
   * @param enabled true to enable Component Model support, false to disable
   * @return this WasiContext for method chaining
   * @since 1.0.0
   */
  WasiContext setComponentModelEnabled(boolean enabled);

  /**
   * Enables or disables process operations for WASI.
   *
   * <p>When enabled, the WebAssembly module can spawn processes, get process information, and
   * perform other process-related operations.
   *
   * @param enabled true to enable process operations, false to disable
   * @return this WasiContext for method chaining
   * @since 1.0.0
   */
  WasiContext setProcessEnabled(boolean enabled);

  /**
   * Sets the working directory for WASI filesystem operations.
   *
   * <p>This affects relative path resolution and is used as the base directory for filesystem
   * operations within the WebAssembly module.
   *
   * @param workingDir the working directory path
   * @return this WasiContext for method chaining
   * @throws IllegalArgumentException if workingDir is null
   * @since 1.0.0
   */
  WasiContext setFilesystemWorkingDir(Path workingDir);

  /**
   * Grants the WASI module access to a directory with specific permissions.
   *
   * <p>This is an enhanced version of preopenedDir that allows fine-grained permission control for
   * WASI Preview 2.
   *
   * @param hostPath the path on the host file system to grant access to
   * @param guestPath the path that the WebAssembly module will use to access the directory
   * @param permissions the permissions to grant (read, write, create, etc.)
   * @return this WasiContext for method chaining
   * @throws WasmException if the directory cannot be accessed or mapped
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.0.0
   */
  WasiContext preopenedDirWithPermissions(
      Path hostPath, String guestPath, WasiDirectoryPermissions permissions) throws WasmException;

  /**
   * Creates a new WasiContext with default settings.
   *
   * <p>The default context has no environment variables, no command-line arguments, inherits stdio,
   * and has no file system access.
   *
   * @return a new WasiContext with default settings
   * @since 1.0.0
   */
  static WasiContext create() throws WasmException {
    return WasmRuntimeFactory.create().createWasiContext();
  }
}
