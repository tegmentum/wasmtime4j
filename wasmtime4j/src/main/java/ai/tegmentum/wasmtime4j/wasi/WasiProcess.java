package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;

/**
 * WASI Preview 1 process and environment operations interface.
 *
 * <p>Provides comprehensive process management capabilities within the WASI sandbox, including
 * process spawning, environment variable access, working directory management, and process control.
 * All operations are subject to the capability-based security model configured in the WASI
 * context.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiContext context = WasiFactory.createContext()) {
 *     WasiProcess process = context.getProcess();
 *
 *     // Spawn a new process
 *     WasiProcessConfig config = WasiProcessConfig.builder()
 *         .setProgram("/bin/echo")
 *         .addArgument("Hello, WASI!")
 *         .build();
 *     WasiProcessId pid = process.spawn(config);
 *
 *     // Wait for completion
 *     int exitCode = process.waitFor(pid);
 *     System.out.println("Process exited with code: " + exitCode);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiProcess {

  /**
   * Spawns a new WASI process with the specified configuration.
   *
   * <p>The spawned process runs in its own WASI context with the environment and resource limits
   * specified in the configuration. The process inherits the capability-based security model from
   * the parent context.
   *
   * @param config the process configuration specifying program, arguments, environment, etc.
   * @return a process ID for the spawned process
   * @throws WasmException if the process cannot be spawned
   * @throws IllegalArgumentException if config is null
   */
  WasiProcessId spawn(final WasiProcessConfig config) throws WasmException;

  /**
   * Waits for a process to complete and returns its exit code.
   *
   * <p>This method blocks until the specified process terminates. If the process has already
   * terminated, this method returns immediately with the exit code.
   *
   * @param pid the process ID to wait for
   * @return the exit code of the process
   * @throws WasmException if the wait operation fails or the process ID is invalid
   * @throws IllegalArgumentException if pid is null
   */
  int waitFor(final WasiProcessId pid) throws WasmException;

  /**
   * Sends a signal to a process.
   *
   * <p>Signals are used for inter-process communication and process control. Not all signals may
   * be supported depending on the underlying platform and WASI implementation.
   *
   * @param pid the process ID to signal
   * @param signal the signal to send
   * @throws WasmException if the signal cannot be sent or the process ID is invalid
   * @throws IllegalArgumentException if any parameter is null
   */
  void kill(final WasiProcessId pid, final WasiSignal signal) throws WasmException;

  /**
   * Gets all environment variables for the current process.
   *
   * <p>Returns a snapshot of the current environment variables. Changes to the returned map do not
   * affect the actual environment.
   *
   * @return a map containing all environment variables
   * @throws WasmException if environment variables cannot be accessed
   */
  Map<String, String> getEnvironment() throws WasmException;

  /**
   * Gets the value of a specific environment variable.
   *
   * @param key the name of the environment variable
   * @return the value of the environment variable, or null if not set
   * @throws WasmException if the environment variable cannot be accessed
   * @throws IllegalArgumentException if key is null
   */
  String getEnvironmentVariable(final String key) throws WasmException;

  /**
   * Sets an environment variable for the current process.
   *
   * <p>This affects the environment for subsequently spawned child processes, but does not change
   * the environment of already running processes.
   *
   * @param key the name of the environment variable
   * @param value the value to set, or null to unset the variable
   * @throws WasmException if the environment variable cannot be set
   * @throws IllegalArgumentException if key is null
   */
  void setEnvironmentVariable(final String key, final String value) throws WasmException;

  /**
   * Gets the process ID of the current process.
   *
   * @return the current process ID
   * @throws WasmException if the process ID cannot be determined
   */
  WasiProcessId getCurrentProcessId() throws WasmException;

  /**
   * Gets the current working directory.
   *
   * @return the current working directory path
   * @throws WasmException if the working directory cannot be determined
   */
  String getCurrentWorkingDirectory() throws WasmException;

  /**
   * Changes the current working directory.
   *
   * <p>This affects path resolution for relative paths in filesystem operations. The new working
   * directory must be within the allowed filesystem capabilities.
   *
   * @param path the new working directory path
   * @throws WasmException if the working directory cannot be changed
   * @throws IllegalArgumentException if path is null
   */
  void setCurrentWorkingDirectory(final String path) throws WasmException;

  /**
   * Gets the command-line arguments for the current process.
   *
   * <p>Returns the arguments that were used to start the current process, including the program
   * name as the first argument.
   *
   * @return a list of command-line arguments
   * @throws WasmException if the arguments cannot be accessed
   */
  List<String> getCommandLineArguments() throws WasmException;

  /**
   * Exits the current process with the specified exit code.
   *
   * <p>This method does not return. It immediately terminates the current process and returns the
   * specified exit code to the parent process.
   *
   * @param exitCode the exit code to return (typically 0 for success, non-zero for error)
   */
  void exit(final int exitCode);

  /**
   * Aborts the current process immediately.
   *
   * <p>This method does not return. It immediately terminates the current process in an abnormal
   * way, typically generating a core dump if supported by the platform.
   */
  void abort();

  /**
   * Checks if the specified process is still running.
   *
   * @param pid the process ID to check
   * @return true if the process is running, false if it has terminated
   * @throws WasmException if the process status cannot be determined
   * @throws IllegalArgumentException if pid is null
   */
  boolean isProcessRunning(final WasiProcessId pid) throws WasmException;

  /**
   * Gets the exit code of a terminated process.
   *
   * <p>This method returns the exit code if the process has already terminated. If the process is
   * still running, this method throws an exception.
   *
   * @param pid the process ID to get the exit code for
   * @return the exit code of the terminated process
   * @throws WasmException if the process is still running or the exit code cannot be determined
   * @throws IllegalArgumentException if pid is null
   */
  int getProcessExitCode(final WasiProcessId pid) throws WasmException;
}