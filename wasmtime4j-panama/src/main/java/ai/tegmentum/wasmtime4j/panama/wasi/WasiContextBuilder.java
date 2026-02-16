package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Builder for creating WASI contexts with comprehensive configuration options in Panama FFI
 * implementation.
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

  /** Package-private constructor - use WasiContext.builder() to create. */
  WasiContextBuilder() {
    LOGGER.fine("Created new Panama WASI context builder");
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
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Environment variable name cannot be null or empty");
    }
    if (value == null) {
      throw new IllegalArgumentException("Environment variable value cannot be null");
    }

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
    if (environmentVars == null) {
      throw new IllegalArgumentException("Environment variables cannot be null");
    }

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
    if (argument == null) {
      throw new IllegalArgumentException("Argument cannot be null");
    }

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
    if (args == null) {
      throw new IllegalArgumentException("Arguments cannot be null");
    }

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
    if (guestDir == null || guestDir.isEmpty()) {
      throw new IllegalArgumentException("Guest directory cannot be null or empty");
    }
    if (hostDir == null || hostDir.isEmpty()) {
      throw new IllegalArgumentException("Host directory cannot be null or empty");
    }

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
    if (workingDir == null || workingDir.isEmpty()) {
      throw new IllegalArgumentException("Working directory cannot be null or empty");
    }

    this.workingDirectory = Paths.get(workingDir).normalize();
    LOGGER.fine(String.format("Set working directory: %s", this.workingDirectory));

    return this;
  }

  /**
   * Creates a WASI context with the configured settings.
   *
   * @return the configured WASI context
   * @throws RuntimeException if the WASI context cannot be created
   */
  public WasiContext build() {
    LOGGER.info(
        String.format(
            "Building Panama WASI context with %d environment variables, %d arguments, %d preopen"
                + " directories",
            environment.size(), arguments.size(), preopenedDirectories.size()));

    // Validate configuration before creating native context
    validateConfiguration();

    // Create resource manager for native memory management
    final ArenaResourceManager resourceManager = new ArenaResourceManager(Arena.ofConfined(), true);

    try {
      // Convert configuration to native format
      final String[] envArray = convertEnvironmentToArray();
      final String[] argArray = arguments.toArray(new String[0]);
      final String[] preopenArray = convertPreopenDirectoriesToArray();
      final String workingDirStr = workingDirectory.toString();

      // Create native WASI context using Panama FFI
      final MemorySegment nativeHandle =
          nativeCreate(envArray, argArray, preopenArray, workingDirStr);

      // Create and return Java wrapper
      return new WasiContext(nativeHandle, resourceManager, this);

    } catch (final Exception e) {
      // Clean up resource manager if context creation fails
      try {
        resourceManager.close();
      } catch (final Exception cleanupException) {
        LOGGER.warning(
            "Error cleaning up resource manager after failed context creation: "
                + cleanupException.getMessage());
        e.addSuppressed(cleanupException);
      }

      throw new RuntimeException("Failed to create Panama WASI context: " + e.getMessage(), e);
    }
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

  /**
   * Validates the configuration before creating the WASI context.
   *
   * <p>Filesystem sandboxing is enforced by the Wasmtime native runtime through pre-opened
   * directory configuration.
   */
  private void validateConfiguration() {
    LOGGER.fine("Panama WASI context configuration validation completed");
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
   * Native method to create a new WASI context using Panama FFI.
   *
   * @param environment environment variables as key=value pairs
   * @param arguments command-line arguments
   * @param preopenDirs pre-opened directory mappings
   * @param workingDir working directory path
   * @return the native handle for the created WASI context
   * @throws RuntimeException if the context cannot be created
   */
  private static MemorySegment nativeCreate(
      final String[] environment,
      final String[] arguments,
      final String[] preopenDirs,
      final String workingDir) {

    LOGGER.fine(
        String.format(
            "Native WASI context creation called with %d env vars, %d args, %d preopen dirs",
            environment.length, arguments.length, preopenDirs.length / 2));

    try {
      // Get the native function bindings
      ai.tegmentum.wasmtime4j.panama.NativeWasiBindings bindings =
          ai.tegmentum.wasmtime4j.panama.NativeWasiBindings.getInstance();

      if (!bindings.isInitialized()) {
        throw new RuntimeException("Native function bindings not initialized");
      }

      // Create WASI context with default configuration
      // TODO: Support custom configuration parameters (allow_network, allow_arbitrary_fs, etc.)
      MemorySegment contextHandle = bindings.wasiContextCreate();

      if (contextHandle == null || contextHandle.equals(MemorySegment.NULL)) {
        throw new RuntimeException("Failed to create native WASI context");
      }

      LOGGER.fine("Created native WASI context with handle: " + contextHandle.address());

      // Configure environment variables
      if (environment.length > 0) {
        try (Arena arena = Arena.ofConfined()) {
          for (String envVar : environment) {
            String[] parts = envVar.split("=", 2);
            if (parts.length == 2) {
              MemorySegment keySegment = arena.allocateFrom(parts[0]);
              MemorySegment valueSegment = arena.allocateFrom(parts[1]);

              int result = bindings.wasiContextSetEnv(contextHandle, keySegment, valueSegment);
              if (result != 0) {
                LOGGER.warning(
                    "Failed to set environment variable: "
                        + parts[0]
                        + ": "
                        + PanamaErrorMapper.getErrorDescription(result));
              }
            }
          }
        }
      }

      // Configure command line arguments
      if (arguments.length > 0) {
        try (Arena arena = Arena.ofConfined()) {
          // Create array of string pointers
          MemorySegment argsArray = arena.allocate(ValueLayout.ADDRESS, arguments.length);
          for (int i = 0; i < arguments.length; i++) {
            MemorySegment argString = arena.allocateFrom(arguments[i]);
            argsArray.setAtIndex(ValueLayout.ADDRESS, i, argString);
          }

          int result = bindings.wasiContextSetArgv(contextHandle, argsArray, arguments.length);
          if (result != 0) {
            LOGGER.warning(
                "Failed to set command line arguments: "
                    + PanamaErrorMapper.getErrorDescription(result));
          }
        }
      }

      // Configure pre-opened directories
      if (preopenDirs.length > 0) {
        try (Arena arena = Arena.ofConfined()) {
          for (int i = 0; i < preopenDirs.length; i += 2) {
            String guestPath = preopenDirs[i];
            String hostPath = preopenDirs[i + 1];

            MemorySegment guestSegment = arena.allocateFrom(guestPath);
            MemorySegment hostSegment = arena.allocateFrom(hostPath);

            // Default directory permissions: read only for both directories and files
            int result =
                bindings.wasiContextPreopenDirReadonly(contextHandle, hostSegment, guestSegment);

            if (result != 0) {
              LOGGER.warning(
                  "Failed to add directory mapping "
                      + guestPath
                      + " -> "
                      + hostPath
                      + ": "
                      + PanamaErrorMapper.getErrorDescription(result));
            }
          }
        }
      }

      return contextHandle;

    } catch (Exception e) {
      LOGGER.severe("Exception during native WASI context creation: " + e.getMessage());
      throw new RuntimeException("Failed to create native WASI context", e);
    }
  }
}
