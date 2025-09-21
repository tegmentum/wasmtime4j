package ai.tegmentum.wasmtime4j.wasi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for spawning WASI processes.
 *
 * <p>This class encapsulates all the configuration parameters needed to spawn a new WASI process,
 * including command-line arguments, environment variables, working directory, and resource limits.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiProcessConfig config = WasiProcessConfig.builder()
 *     .setProgram("/bin/echo")
 *     .addArgument("Hello, World!")
 *     .setEnvironmentVariable("PATH", "/usr/bin")
 *     .setWorkingDirectory("/tmp")
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiProcessConfig {
  private final String program;
  private final List<String> arguments;
  private final Map<String, String> environment;
  private final String workingDirectory;
  private final WasiResourceLimits resourceLimits;

  private WasiProcessConfig(final Builder builder) {
    this.program = builder.program;
    this.arguments = Collections.unmodifiableList(new ArrayList<>(builder.arguments));
    this.environment = Collections.unmodifiableMap(new HashMap<>(builder.environment));
    this.workingDirectory = builder.workingDirectory;
    this.resourceLimits = builder.resourceLimits;
  }

  /**
   * Gets the program path to execute.
   *
   * @return the program path
   */
  public String getProgram() {
    return program;
  }

  /**
   * Gets the command-line arguments.
   *
   * @return an immutable list of arguments
   */
  public List<String> getArguments() {
    return arguments;
  }

  /**
   * Gets the environment variables.
   *
   * @return an immutable map of environment variables
   */
  public Map<String, String> getEnvironment() {
    return environment;
  }

  /**
   * Gets the working directory.
   *
   * @return the working directory path, or null if not set
   */
  public String getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * Gets the resource limits.
   *
   * @return the resource limits, or null if not set
   */
  public WasiResourceLimits getResourceLimits() {
    return resourceLimits;
  }

  /**
   * Creates a new builder for process configuration.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new builder initialized with this configuration.
   *
   * @return a new builder instance
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Builder for WasiProcessConfig.
   */
  public static final class Builder {
    private String program;
    private final List<String> arguments = new ArrayList<>();
    private final Map<String, String> environment = new HashMap<>();
    private String workingDirectory;
    private WasiResourceLimits resourceLimits;

    private Builder() {}

    private Builder(final WasiProcessConfig config) {
      this.program = config.program;
      this.arguments.addAll(config.arguments);
      this.environment.putAll(config.environment);
      this.workingDirectory = config.workingDirectory;
      this.resourceLimits = config.resourceLimits;
    }

    /**
     * Sets the program to execute.
     *
     * @param program the program path
     * @return this builder
     * @throws IllegalArgumentException if program is null or empty
     */
    public Builder setProgram(final String program) {
      if (program == null || program.trim().isEmpty()) {
        throw new IllegalArgumentException("Program cannot be null or empty");
      }
      this.program = program;
      return this;
    }

    /**
     * Adds a command-line argument.
     *
     * @param argument the argument to add
     * @return this builder
     * @throws IllegalArgumentException if argument is null
     */
    public Builder addArgument(final String argument) {
      if (argument == null) {
        throw new IllegalArgumentException("Argument cannot be null");
      }
      this.arguments.add(argument);
      return this;
    }

    /**
     * Sets all command-line arguments.
     *
     * @param arguments the arguments to set
     * @return this builder
     * @throws IllegalArgumentException if arguments is null or contains null elements
     */
    public Builder setArguments(final List<String> arguments) {
      if (arguments == null) {
        throw new IllegalArgumentException("Arguments cannot be null");
      }
      for (final String arg : arguments) {
        if (arg == null) {
          throw new IllegalArgumentException("Arguments cannot contain null elements");
        }
      }
      this.arguments.clear();
      this.arguments.addAll(arguments);
      return this;
    }

    /**
     * Sets an environment variable.
     *
     * @param key the variable name
     * @param value the variable value
     * @return this builder
     * @throws IllegalArgumentException if key or value is null
     */
    public Builder setEnvironmentVariable(final String key, final String value) {
      if (key == null) {
        throw new IllegalArgumentException("Environment variable key cannot be null");
      }
      if (value == null) {
        throw new IllegalArgumentException("Environment variable value cannot be null");
      }
      this.environment.put(key, value);
      return this;
    }

    /**
     * Sets all environment variables.
     *
     * @param environment the environment variables to set
     * @return this builder
     * @throws IllegalArgumentException if environment is null or contains null keys/values
     */
    public Builder setEnvironment(final Map<String, String> environment) {
      if (environment == null) {
        throw new IllegalArgumentException("Environment cannot be null");
      }
      for (final Map.Entry<String, String> entry : environment.entrySet()) {
        if (entry.getKey() == null) {
          throw new IllegalArgumentException("Environment variable keys cannot be null");
        }
        if (entry.getValue() == null) {
          throw new IllegalArgumentException("Environment variable values cannot be null");
        }
      }
      this.environment.clear();
      this.environment.putAll(environment);
      return this;
    }

    /**
     * Sets the working directory.
     *
     * @param workingDirectory the working directory path
     * @return this builder
     */
    public Builder setWorkingDirectory(final String workingDirectory) {
      this.workingDirectory = workingDirectory;
      return this;
    }

    /**
     * Sets the resource limits.
     *
     * @param resourceLimits the resource limits
     * @return this builder
     */
    public Builder setResourceLimits(final WasiResourceLimits resourceLimits) {
      this.resourceLimits = resourceLimits;
      return this;
    }

    /**
     * Builds the process configuration.
     *
     * @return a new WasiProcessConfig instance
     * @throws IllegalStateException if program is not set
     */
    public WasiProcessConfig build() {
      if (program == null) {
        throw new IllegalStateException("Program must be set");
      }
      return new WasiProcessConfig(this);
    }
  }
}