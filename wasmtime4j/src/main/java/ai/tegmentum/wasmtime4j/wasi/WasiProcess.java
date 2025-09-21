package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;

/**
 * WASI Preview 1 process and environment interface providing process management and environment
 * access.
 *
 * <p>This interface provides access to process-related operations within the WASI sandbox,
 * including environment variable access, command-line arguments, process spawning, and exit
 * handling.
 *
 * <p>Process operations include:
 *
 * <ul>
 *   <li>Environment variable access and manipulation
 *   <li>Command-line argument access
 *   <li>Process spawning and management
 *   <li>Working directory operations
 *   <li>Process exit and termination
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiContext context = WasiFactory.createContext();
 * WasiProcess process = context.getProcess();
 *
 * // Access environment variables
 * Map<String, String> env = process.getEnvironment();
 * String path = process.getEnvironmentVariable("PATH");
 *
 * // Get command line arguments
 * List<String> args = process.getCommandLineArguments();
 *
 * // Set environment variable
 * process.setEnvironmentVariable("MY_VAR", "value");
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiProcess {

  /**
   * Spawns a new process with the specified configuration.
   *
   * <p>This method creates and starts a new process using the provided configuration. The exact
   * capabilities depend on the WASI host implementation and security policy.
   *
   * @param config the process configuration including executable path, arguments, and environment
   * @return a process ID that can be used to interact with the spawned process
   * @throws WasmException if process spawning fails or permission is denied
   * @throws IllegalArgumentException if config is null
   */
  WasiProcessId spawn(final WasiProcessConfig config) throws WasmException;

  /**
   * Waits for a process to complete and returns its exit code.
   *
   * <p>This method blocks until the specified process completes execution and returns the exit
   * code. The exit code is typically 0 for success and non-zero for errors.
   *
   * @param pid the process ID to wait for
   * @return the exit code of the process
   * @throws WasmException if waiting fails or the process ID is invalid
   * @throws IllegalArgumentException if pid is null
   */
  int waitFor(final WasiProcessId pid) throws WasmException;

  /**
   * Sends a signal to terminate a process.
   *
   * <p>This method sends a termination signal to the specified process. The exact signal and
   * behavior depend on the host system and WASI implementation.
   *
   * @param pid the process ID to terminate
   * @param signal the signal to send
   * @throws WasmException if signaling fails or permission is denied
   * @throws IllegalArgumentException if pid or signal is null
   */
  void kill(final WasiProcessId pid, final WasiSignal signal) throws WasmException;

  /**
   * Gets all environment variables available to the current process.
   *
   * <p>This method returns a map of all environment variables that are accessible within the WASI
   * sandbox. The available variables depend on the security policy and configuration.
   *
   * @return a map of environment variable names to values
   * @throws WasmException if environment access fails or permission is denied
   */
  Map<String, String> getEnvironment() throws WasmException;

  /**
   * Gets the value of a specific environment variable.
   *
   * <p>This method retrieves the value of a single environment variable by name. Returns null if
   * the variable is not set or not accessible.
   *
   * @param key the name of the environment variable
   * @return the value of the environment variable, or null if not found
   * @throws WasmException if environment access fails or permission is denied
   * @throws IllegalArgumentException if key is null
   */
  String getEnvironmentVariable(final String key) throws WasmException;

  /**
   * Sets the value of an environment variable.
   *
   * <p>This method sets an environment variable to the specified value. The variable will be
   * available to subsequently spawned processes if the security policy allows it.
   *
   * @param key the name of the environment variable
   * @param value the value to set
   * @throws WasmException if setting the variable fails or permission is denied
   * @throws IllegalArgumentException if key or value is null
   */
  void setEnvironmentVariable(final String key, final String value) throws WasmException;

  /**
   * Gets the process ID of the current process.
   *
   * <p>This method returns an identifier for the current WASI process. The exact meaning and format
   * of the process ID depends on the host system.
   *
   * @return the current process ID
   * @throws WasmException if getting the process ID fails
   */
  WasiProcessId getCurrentProcessId() throws WasmException;

  /**
   * Gets the current working directory path.
   *
   * <p>This method returns the current working directory as seen by the WASI process. The path is
   * relative to the WASI filesystem namespace.
   *
   * @return the current working directory path
   * @throws WasmException if getting the working directory fails or permission is denied
   */
  String getCurrentWorkingDirectory() throws WasmException;

  /**
   * Sets the current working directory.
   *
   * <p>This method changes the current working directory for the WASI process. Relative paths in
   * filesystem operations will be resolved relative to this directory.
   *
   * @param path the new working directory path
   * @throws WasmException if changing the directory fails or permission is denied
   * @throws IllegalArgumentException if path is null
   */
  void setCurrentWorkingDirectory(final String path) throws WasmException;

  /**
   * Gets the command-line arguments passed to the current process.
   *
   * <p>This method returns the command-line arguments that were passed when the WASI process was
   * started. The first argument is typically the program name.
   *
   * @return a list of command-line arguments
   * @throws WasmException if getting arguments fails
   */
  List<String> getCommandLineArguments() throws WasmException;

  /**
   * Terminates the current process with the specified exit code.
   *
   * <p>This method immediately terminates the current WASI process with the given exit code. The
   * exit code is typically 0 for success and non-zero for errors. This method does not return.
   *
   * @param exitCode the exit code to return to the parent process
   */
  void exit(final int exitCode);

  /**
   * Aborts the current process abnormally.
   *
   * <p>This method immediately terminates the current WASI process in an abnormal way, typically
   * with a non-zero exit code. This method does not return and should only be used in error
   * conditions.
   */
  void abort();

  /**
   * Checks if process spawning is supported and enabled.
   *
   * <p>This method can be used to determine if the current WASI environment supports spawning child
   * processes. Support depends on the host implementation and security policy.
   *
   * @return true if process spawning is available, false otherwise
   */
  boolean isSpawnSupported();

  /**
   * Gets process usage statistics for the current process.
   *
   * <p>This method returns information about resource usage (CPU time, memory, etc.) for the
   * current process. The available statistics depend on the host system.
   *
   * @return process usage statistics
   * @throws WasmException if getting statistics fails or permission is denied
   */
  WasiProcessStats getProcessStats() throws WasmException;
}
