package ai.tegmentum.wasmtime4j.wasi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configuration for spawning a new WASI process.
 *
 * <p>This class contains all the configuration options needed to spawn a new process in the WASI
 * environment, including the executable path, command-line arguments, environment variables, and
 * working directory.
 *
 * @since 1.0.0
 */
public final class WasiProcessConfig {

  private final String executable;
  private final List<String> arguments;
  private final Map<String, String> environment;
  private final String workingDirectory;
  private final WasiStdioRedirection stdin;
  private final WasiStdioRedirection stdout;
  private final WasiStdioRedirection stderr;

  /**
   * Creates a new process configuration.
   *
   * @param executable the path to the executable to run
   * @param arguments the command-line arguments (not including the executable name)
   * @param environment the environment variables for the new process
   * @param workingDirectory the working directory for the new process
   * @param stdin the standard input redirection
   * @param stdout the standard output redirection
   * @param stderr the standard error redirection
   */
  public WasiProcessConfig(
      final String executable,
      final List<String> arguments,
      final Map<String, String> environment,
      final String workingDirectory,
      final WasiStdioRedirection stdin,
      final WasiStdioRedirection stdout,
      final WasiStdioRedirection stderr) {
    this.executable = executable;
    this.arguments = Collections.unmodifiableList(arguments);
    this.environment = Collections.unmodifiableMap(environment);
    this.workingDirectory = workingDirectory;
    this.stdin = stdin;
    this.stdout = stdout;
    this.stderr = stderr;
  }

  /**
   * Gets the path to the executable to run.
   *
   * @return the executable path
   */
  public String getExecutable() {
    return executable;
  }

  /**
   * Gets the command-line arguments for the process.
   *
   * <p>This list does not include the executable name itself (argv[0]), only the arguments
   * that follow it.
   *
   * @return an immutable list of command-line arguments
   */
  public List<String> getArguments() {
    return arguments;
  }

  /**
   * Gets the environment variables for the new process.
   *
   * @return an immutable map of environment variable names to values
   */
  public Map<String, String> getEnvironment() {
    return environment;
  }

  /**
   * Gets the working directory for the new process.
   *
   * <p>This is the directory that will be set as the current working directory when the process
   * starts. May be null to inherit the current working directory.
   *
   * @return the working directory path, or null to inherit
   */
  public String getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * Gets the standard input redirection configuration.
   *
   * @return the stdin redirection
   */
  public WasiStdioRedirection getStdin() {
    return stdin;
  }

  /**
   * Gets the standard output redirection configuration.
   *
   * @return the stdout redirection
   */
  public WasiStdioRedirection getStdout() {
    return stdout;
  }

  /**
   * Gets the standard error redirection configuration.
   *
   * @return the stderr redirection
   */
  public WasiStdioRedirection getStderr() {
    return stderr;
  }

  /**
   * Creates a builder for constructing WasiProcessConfig instances.
   *
   * @param executable the path to the executable to run
   * @return a new builder instance
   */
  public static Builder builder(final String executable) {
    return new Builder(executable);
  }

  @Override
  public String toString() {
    return String.format(
        "WasiProcessConfig{executable='%s', arguments=%s, environment=%s, workingDirectory='%s', "
            + "stdin=%s, stdout=%s, stderr=%s}",
        executable, arguments, environment, workingDirectory, stdin, stdout, stderr);
  }

  /**
   * Builder for creating WasiProcessConfig instances.
   */
  public static final class Builder {
    private final String executable;
    private List<String> arguments = Collections.emptyList();
    private Map<String, String> environment = Collections.emptyMap();
    private String workingDirectory = null;
    private WasiStdioRedirection stdin = WasiStdioRedirection.inherit();
    private WasiStdioRedirection stdout = WasiStdioRedirection.inherit();
    private WasiStdioRedirection stderr = WasiStdioRedirection.inherit();

    private Builder(final String executable) {
      this.executable = executable;
    }

    /**
     * Sets the command-line arguments for the process.
     *
     * @param arguments the command-line arguments
     * @return this builder
     */
    public Builder arguments(final List<String> arguments) {
      this.arguments = arguments;
      return this;
    }

    /**
     * Sets the command-line arguments for the process.
     *
     * @param arguments the command-line arguments
     * @return this builder
     */
    public Builder arguments(final String... arguments) {
      this.arguments = List.of(arguments);
      return this;
    }

    /**
     * Sets the environment variables for the process.
     *
     * @param environment the environment variables
     * @return this builder
     */
    public Builder environment(final Map<String, String> environment) {
      this.environment = environment;
      return this;
    }

    /**
     * Sets the working directory for the process.
     *
     * @param workingDirectory the working directory path
     * @return this builder
     */
    public Builder workingDirectory(final String workingDirectory) {
      this.workingDirectory = workingDirectory;
      return this;
    }

    /**
     * Sets the standard input redirection.
     *
     * @param stdin the stdin redirection
     * @return this builder
     */
    public Builder stdin(final WasiStdioRedirection stdin) {
      this.stdin = stdin;
      return this;
    }

    /**
     * Sets the standard output redirection.
     *
     * @param stdout the stdout redirection
     * @return this builder
     */
    public Builder stdout(final WasiStdioRedirection stdout) {
      this.stdout = stdout;
      return this;
    }

    /**
     * Sets the standard error redirection.
     *
     * @param stderr the stderr redirection
     * @return this builder
     */
    public Builder stderr(final WasiStdioRedirection stderr) {
      this.stderr = stderr;
      return this;
    }

    /**
     * Builds the WasiProcessConfig.
     *
     * @return a new WasiProcessConfig instance
     */
    public WasiProcessConfig build() {
      return new WasiProcessConfig(
          executable, arguments, environment, workingDirectory, stdin, stdout, stderr);
    }
  }
}