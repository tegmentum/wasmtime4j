package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.permission.WasiPermissionManager;
import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Builder for creating WASI contexts with comprehensive configuration options.
 *
 * <p>This builder provides a fluent API for configuring WASI contexts with security, permissions,
 * and resource management. It includes:
 *
 * <ul>
 *   <li>Environment variable configuration
 *   <li>Command-line argument setup
 *   <li>Pre-opened directory management with sandbox permissions
 *   <li>Security validation and path traversal protection
 *   <li>Resource limiting and quota enforcement
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiContext context = WasiContext.builder()
 *     .withEnvironment("HOME", "/home/user")
 *     .withArgument("--verbose")
 *     .withPreopenDirectory("/tmp", "/host/tmp")
 *     .withWorkingDirectory("/app")
 *     .withPermissionManager(permissionManager)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiContextBuilder {

  private static final Logger LOGGER = Logger.getLogger(WasiContextBuilder.class.getName());

  /** Default working directory if none specified. */
  private static final String DEFAULT_WORKING_DIR = "/";

  /** Environment variables for the WASI context. */
  private final Map<String, String> environment = new HashMap<>();

  /** Command-line arguments for the WASI context. */
  private final List<String> arguments = new ArrayList<>();

  /** Pre-opened directories with their host mappings. */
  private final Map<String, Path> preopenedDirectories = new HashMap<>();

  /** Working directory for the WASI context. */
  private Path workingDirectory = Paths.get(DEFAULT_WORKING_DIR);

  /** Permission manager for controlling WASI capabilities. */
  private WasiPermissionManager permissionManager = WasiPermissionManager.defaultManager();

  /** Security validator for preventing unauthorized access. */
  private WasiSecurityValidator securityValidator = WasiSecurityValidator.defaultValidator();

  /** Package-private constructor - use WasiContext.builder() to create. */
  WasiContextBuilder() {
    LOGGER.fine("Created new WASI context builder");
  }

  /**
   * Sets an environment variable for the WASI context.
   *
   * @param name the environment variable name
   * @param value the environment variable value
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name or value is null/empty
   */
  public WasiContextBuilder withEnvironment(final String name, final String value) {
    JniValidation.requireNonEmpty(name, "name");
    JniValidation.requireNonNull(value, "value");

    environment.put(name, value);
    LOGGER.fine(String.format("Added environment variable: %s", name));

    return this;
  }

  /**
   * Sets multiple environment variables for the WASI context.
   *
   * @param environmentVars the environment variables to set
   * @return this builder for method chaining
   * @throws IllegalArgumentException if environmentVars is null
   */
  public WasiContextBuilder withEnvironment(final Map<String, String> environmentVars) {
    JniValidation.requireNonNull(environmentVars, "environmentVars");

    for (final Map.Entry<String, String> entry : environmentVars.entrySet()) {
      withEnvironment(entry.getKey(), entry.getValue());
    }

    return this;
  }

  /**
   * Inherits environment variables from the current process.
   *
   * @return this builder for method chaining
   */
  public WasiContextBuilder withInheritedEnvironment() {
    withEnvironment(System.getenv());
    LOGGER.fine("Inherited environment variables from host process");

    return this;
  }

  /**
   * Adds a command-line argument to the WASI context.
   *
   * @param argument the command-line argument to add
   * @return this builder for method chaining
   * @throws IllegalArgumentException if argument is null
   */
  public WasiContextBuilder withArgument(final String argument) {
    JniValidation.requireNonNull(argument, "argument");

    arguments.add(argument);
    LOGGER.fine(String.format("Added argument: %s", argument));

    return this;
  }

  /**
   * Adds multiple command-line arguments to the WASI context.
   *
   * @param args the command-line arguments to add
   * @return this builder for method chaining
   * @throws IllegalArgumentException if args is null
   */
  public WasiContextBuilder withArguments(final String... args) {
    JniValidation.requireNonNull(args, "args");

    for (final String arg : args) {
      withArgument(arg);
    }

    return this;
  }

  /**
   * Pre-opens a directory for WASI file system access.
   *
   * <p>The guest directory is the path as seen by the WebAssembly module, while the host directory
   * is the actual path on the host file system.
   *
   * @param guestDir the directory path as seen by the WASI module
   * @param hostDir the actual directory path on the host system
   * @return this builder for method chaining
   * @throws IllegalArgumentException if either directory path is invalid
   */
  public WasiContextBuilder withPreopenDirectory(final String guestDir, final String hostDir) {
    JniValidation.requireNonEmpty(guestDir, "guestDir");
    JniValidation.requireNonEmpty(hostDir, "hostDir");

    final Path hostPath = Paths.get(hostDir).toAbsolutePath().normalize();

    // Validate that host directory exists and is accessible
    if (!Files.exists(hostPath)) {
      throw new IllegalArgumentException(
          String.format("Host directory does not exist: %s", hostPath));
    }

    if (!Files.isDirectory(hostPath)) {
      throw new IllegalArgumentException(
          String.format("Host path is not a directory: %s", hostPath));
    }

    preopenedDirectories.put(guestDir, hostPath);
    LOGGER.fine(String.format("Pre-opened directory: %s -> %s", guestDir, hostPath));

    return this;
  }

  /**
   * Pre-opens a directory using the same path for both guest and host.
   *
   * @param directory the directory path to pre-open
   * @return this builder for method chaining
   * @throws IllegalArgumentException if directory path is invalid
   */
  public WasiContextBuilder withPreopenDirectory(final String directory) {
    return withPreopenDirectory(directory, directory);
  }

  /**
   * Sets the working directory for the WASI context.
   *
   * @param workingDir the working directory path
   * @return this builder for method chaining
   * @throws IllegalArgumentException if workingDir is null/empty
   */
  public WasiContextBuilder withWorkingDirectory(final String workingDir) {
    JniValidation.requireNonEmpty(workingDir, "workingDir");

    this.workingDirectory = Paths.get(workingDir).normalize();
    LOGGER.fine(String.format("Set working directory: %s", this.workingDirectory));

    return this;
  }

  /**
   * Sets the permission manager for controlling WASI capabilities.
   *
   * @param manager the permission manager to use
   * @return this builder for method chaining
   * @throws IllegalArgumentException if manager is null
   */
  public WasiContextBuilder withPermissionManager(final WasiPermissionManager manager) {
    JniValidation.requireNonNull(manager, "manager");

    this.permissionManager = manager;
    LOGGER.fine("Set custom permission manager");

    return this;
  }

  /**
   * Sets the security validator for preventing unauthorized access.
   *
   * @param validator the security validator to use
   * @return this builder for method chaining
   * @throws IllegalArgumentException if validator is null
   */
  public WasiContextBuilder withSecurityValidator(final WasiSecurityValidator validator) {
    JniValidation.requireNonNull(validator, "validator");

    this.securityValidator = validator;
    LOGGER.fine("Set custom security validator");

    return this;
  }

  /**
   * Creates a WASI context with the configured settings.
   *
   * @return the configured WASI context
   * @throws JniException if the WASI context cannot be created
   */
  public WasiContext build() throws JniException {
    LOGGER.info(
        String.format(
            "Building WASI context with %d environment variables, %d arguments, %d preopen"
                + " directories",
            environment.size(), arguments.size(), preopenedDirectories.size()));

    // Validate configuration before creating native context
    validateConfiguration();

    // Convert configuration to native format
    final String[] envArray = convertEnvironmentToArray();
    final String[] argArray = arguments.toArray(new String[0]);
    final String[] preopenArray = convertPreopenDirectoriesToArray();
    final String workingDirStr = workingDirectory.toString();

    try {
      // Create native WASI context
      final long nativeHandle =
          WasiContext.nativeCreate(envArray, argArray, preopenArray, workingDirStr);

      // Create and return Java wrapper
      return new WasiContext(nativeHandle, this);

    } catch (final Exception e) {
      throw new JniException("Failed to create WASI context: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the permission manager.
   *
   * @return the permission manager
   */
  WasiPermissionManager getPermissionManager() {
    return permissionManager;
  }

  /**
   * Gets the security validator.
   *
   * @return the security validator
   */
  WasiSecurityValidator getSecurityValidator() {
    return securityValidator;
  }

  /**
   * Gets the environment variables.
   *
   * @return the environment variables
   */
  Map<String, String> getEnvironment() {
    return new HashMap<>(environment);
  }

  /**
   * Gets the command-line arguments.
   *
   * @return the command-line arguments
   */
  List<String> getArguments() {
    return new ArrayList<>(arguments);
  }

  /**
   * Gets the pre-opened directories.
   *
   * @return the pre-opened directories
   */
  Map<String, Path> getPreopenedDirectories() {
    return new HashMap<>(preopenedDirectories);
  }

  /**
   * Gets the working directory.
   *
   * @return the working directory
   */
  Path getWorkingDirectory() {
    return workingDirectory;
  }

  /** Validates the configuration before creating the WASI context. */
  private void validateConfiguration() {
    // Validate pre-opened directories
    for (final Map.Entry<String, Path> entry : preopenedDirectories.entrySet()) {
      final String guestDir = entry.getKey();
      final Path hostPath = entry.getValue();

      // Validate guest directory path
      securityValidator.validatePath(Paths.get(guestDir));

      // Validate host directory accessibility
      permissionManager.validateFileSystemAccess(hostPath);
    }

    // Validate working directory
    securityValidator.validatePath(workingDirectory);

    LOGGER.fine("WASI context configuration validation completed");
  }

  /** Converts environment variables to array format for native calls. */
  private String[] convertEnvironmentToArray() {
    final String[] envArray = new String[environment.size()];
    int index = 0;

    for (final Map.Entry<String, String> entry : environment.entrySet()) {
      envArray[index++] = entry.getKey() + "=" + entry.getValue();
    }

    return envArray;
  }

  /** Converts pre-opened directories to array format for native calls. */
  private String[] convertPreopenDirectoriesToArray() {
    final String[] preopenArray = new String[preopenedDirectories.size() * 2];
    int index = 0;

    for (final Map.Entry<String, Path> entry : preopenedDirectories.entrySet()) {
      preopenArray[index++] = entry.getKey(); // Guest directory
      preopenArray[index++] = entry.getValue().toString(); // Host directory
    }

    return preopenArray;
  }

  /**
   * Creates a new builder instance.
   *
   * @return a new WasiContextBuilder
   */
  public static WasiContextBuilder builder() {
    return new WasiContextBuilder();
  }

  /**
   * Adds a preopen directory (alias for withPreopenDirectory).
   *
   * @param hostPath the host directory path
   * @param guestPath the guest directory path
   * @return this builder for method chaining
   * @throws IllegalArgumentException if hostPath is null
   */
  public WasiContextBuilder addPreopenedDirectory(final Path hostPath, final String guestPath) {
    JniValidation.requireNonNull(hostPath, "hostPath");
    return withPreopenDirectory(guestPath, hostPath.toString());
  }

  /**
   * Adds an environment variable (alias for withEnvironment).
   *
   * @param name the environment variable name
   * @param value the environment variable value
   * @return this builder for method chaining
   */
  public WasiContextBuilder addEnvironmentVariable(final String name, final String value) {
    return withEnvironment(name, value);
  }
}
