package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Specialized linker for WASI (WebAssembly System Interface) modules with enhanced configuration.
 *
 * <p>A WasiLinker extends the standard linker functionality with WASI-specific capabilities,
 * providing fine-grained control over system interface access, filesystem permissions,
 * environment variables, and network access.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Configurable filesystem access with directory mapping and permissions
 *   <li>Environment variable control with allow/deny lists
 *   <li>Command-line argument passing to WebAssembly modules
 *   <li>Standard I/O stream configuration
 *   <li>Network access controls (where supported)
 *   <li>Security sandboxing and capability-based permissions
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * try (WasiLinker linker = WasiLinker.create(engine)) {
 *     // Configure filesystem access
 *     linker.allowDirectoryAccess("/tmp", "/sandbox",
 *         WasiPermissions.READ_WRITE);
 *
 *     // Set environment variables
 *     linker.setEnvironmentVariable("USER", "wasm-user");
 *
 *     // Set command line arguments
 *     linker.setArguments(List.of("program", "--verbose"));
 *
 *     // Instantiate WASI module
 *     Instance instance = linker.instantiate(store, module);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiLinker extends Closeable {

  /**
   * Allows access to a host directory from the WebAssembly module.
   *
   * <p>This method maps a host filesystem directory to a path visible within
   * the WebAssembly module's WASI filesystem namespace. Permissions control
   * what operations the module can perform on files within this directory.
   *
   * @param hostPath the path on the host filesystem to expose
   * @param guestPath the path where it should appear in the WASI filesystem
   * @param permissions the permissions to grant for this directory
   * @throws WasmException if directory mapping fails
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.0.0
   */
  void allowDirectoryAccess(
      final Path hostPath,
      final String guestPath,
      final WasiPermissions permissions) throws WasmException;

  /**
   * Allows access to a host directory with default permissions.
   *
   * <p>Convenience method that maps a directory with read-write permissions.
   *
   * @param hostPath the path on the host filesystem to expose
   * @param guestPath the path where it should appear in the WASI filesystem
   * @throws WasmException if directory mapping fails
   * @throws IllegalArgumentException if any parameter is null
   */
  void allowDirectoryAccess(final Path hostPath, final String guestPath) throws WasmException;

  /**
   * Sets an environment variable that will be available to the WebAssembly module.
   *
   * @param name the environment variable name
   * @param value the environment variable value
   * @throws IllegalArgumentException if name is null
   */
  void setEnvironmentVariable(final String name, final String value);

  /**
   * Sets multiple environment variables from a map.
   *
   * @param environment map of environment variable names to values
   * @throws IllegalArgumentException if environment is null
   */
  void setEnvironmentVariables(final Map<String, String> environment);

  /**
   * Inherits environment variables from the host process.
   *
   * <p>This allows the WebAssembly module to access the same environment
   * variables as the host Java process. Use with caution as this may expose
   * sensitive information.
   *
   * @throws WasmException if environment inheritance fails
   */
  void inheritEnvironment() throws WasmException;

  /**
   * Inherits only specified environment variables from the host process.
   *
   * @param variableNames list of environment variable names to inherit
   * @throws WasmException if environment inheritance fails
   * @throws IllegalArgumentException if variableNames is null
   */
  void inheritEnvironmentVariables(final List<String> variableNames) throws WasmException;

  /**
   * Sets the command-line arguments that will be passed to the WebAssembly module.
   *
   * <p>The first argument is typically the program name, followed by any
   * command-line flags or parameters.
   *
   * @param arguments list of command-line arguments
   * @throws IllegalArgumentException if arguments is null
   */
  void setArguments(final List<String> arguments);

  /**
   * Configures standard input for the WebAssembly module.
   *
   * @param config the standard input configuration
   * @throws WasmException if stdin configuration fails
   * @throws IllegalArgumentException if config is null
   */
  void configureStdin(final WasiStdioConfig config) throws WasmException;

  /**
   * Configures standard output for the WebAssembly module.
   *
   * @param config the standard output configuration
   * @throws WasmException if stdout configuration fails
   * @throws IllegalArgumentException if config is null
   */
  void configureStdout(final WasiStdioConfig config) throws WasmException;

  /**
   * Configures standard error for the WebAssembly module.
   *
   * @param config the standard error configuration
   * @throws WasmException if stderr configuration fails
   * @throws IllegalArgumentException if config is null
   */
  void configureStderr(final WasiStdioConfig config) throws WasmException;

  /**
   * Enables network access for the WebAssembly module.
   *
   * <p>Network access is disabled by default for security. Enable only when
   * the WebAssembly module requires network connectivity and is trusted.
   *
   * @throws WasmException if network access cannot be enabled
   */
  void enableNetworkAccess() throws WasmException;

  /**
   * Disables network access for the WebAssembly module.
   *
   * <p>This is the default state. Network access can be explicitly disabled
   * even after being enabled.
   */
  void disableNetworkAccess();

  /**
   * Sets the maximum file size for WASI file operations.
   *
   * <p>This limits how large files can grow through WASI operations, providing
   * protection against resource exhaustion attacks.
   *
   * @param maxSizeBytes maximum file size in bytes, or null for no limit
   */
  void setMaxFileSize(final Long maxSizeBytes);

  /**
   * Sets the maximum number of open file descriptors.
   *
   * <p>This limits how many files the WebAssembly module can have open
   * simultaneously, providing protection against resource exhaustion.
   *
   * @param maxOpenFiles maximum number of open files, or null for no limit
   */
  void setMaxOpenFiles(final Integer maxOpenFiles);

  /**
   * Instantiates a WebAssembly module with WASI support using this linker.
   *
   * <p>The linker will provide all WASI functions and configured resources
   * to satisfy the module's import requirements. The module must be a valid
   * WASI module that imports WASI functions.
   *
   * @param store the store to instantiate the module in
   * @param module the compiled WASI module to instantiate
   * @return a new Instance of the module with WASI support
   * @throws WasmException if instantiation fails or WASI setup fails
   * @throws IllegalArgumentException if store or module is null
   */
  Instance instantiate(final Store store, final Module module) throws WasmException;

  /**
   * Gets the underlying standard linker.
   *
   * <p>This provides access to the base linker functionality for defining
   * additional host functions or imports beyond WASI.
   *
   * @return the underlying Linker instance
   */
  Linker<?> getLinker();

  /**
   * Gets the engine associated with this WASI linker.
   *
   * @return the Engine that created this linker
   */
  Engine getEngine();

  /**
   * Gets the current WASI configuration.
   *
   * @return immutable view of the current WASI configuration
   */
  WasiConfig getConfig();

  /**
   * Checks if the WASI linker is still valid and usable.
   *
   * @return true if the linker is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the WASI linker and releases associated resources.
   *
   * <p>After closing, the linker becomes invalid and should not be used.
   */
  @Override
  void close();

  /**
   * Creates a new WasiLinker for the given engine.
   *
   * @param engine the engine to create the linker for
   * @return a new WasiLinker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   */
  static WasiLinker create(final Engine engine) throws WasmException {
    return WasmRuntimeFactory.create().createWasiLinker(engine);
  }

  /**
   * Creates a new WasiLinker with the specified configuration.
   *
   * @param engine the engine to create the linker for
   * @param config the WASI configuration to use
   * @return a new WasiLinker instance with the specified configuration
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine or config is null
   */
  static WasiLinker create(final Engine engine, final WasiConfig config) throws WasmException {
    return WasmRuntimeFactory.create().createWasiLinker(engine, config);
  }
}