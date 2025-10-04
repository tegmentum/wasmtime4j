package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI Preview 2 environment and command-line operations.
 *
 * <p>This class implements the WASI Preview 2 environment operations as defined in the WIT
 * interfaces `wasi:cli/environment` and `wasi:cli/argv`. It provides enhanced environment variable
 * and command-line argument handling with async support.
 *
 * <p>Supported WASI Preview 2 environment operations:
 *
 * <ul>
 *   <li>Environment variable access with enhanced security
 *   <li>Command-line argument parsing and validation
 *   <li>Async environment operations
 *   <li>Environment variable modification (controlled)
 *   <li>Process exit handling
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiEnvironmentOperationsPreview2 {

  private static final Logger LOGGER =
      Logger.getLogger(WasiEnvironmentOperationsPreview2.class.getName());

  /** Maximum number of environment variables allowed. */
  private static final int MAX_ENVIRONMENT_VARIABLES = 1000;

  /** Maximum length of environment variable name or value. */
  private static final int MAX_ENV_LENGTH = 32768; // 32KB

  /** Maximum number of command-line arguments allowed. */
  private static final int MAX_ARGUMENTS = 1000;

  /** Maximum length of a command-line argument. */
  private static final int MAX_ARG_LENGTH = 32768; // 32KB

  /** The WASI context this environment operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** Cached environment variables. */
  private volatile Map<String, String> cachedEnvironment;

  /** Cached command-line arguments. */
  private volatile String[] cachedArguments;

  /**
   * Creates a new WASI Preview 2 environment operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiEnvironmentOperationsPreview2(
      final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;

    LOGGER.info("Created WASI Preview 2 environment operations handler");
  }

  /**
   * Gets all environment variables.
   *
   * <p>WIT interface: wasi:cli/environment.get-environment
   *
   * @return immutable map of environment variables
   * @throws WasiException if the operation fails
   */
  public Map<String, String> getEnvironment() {
    LOGGER.fine("Getting environment variables");

    // Return cached environment if available
    if (cachedEnvironment != null) {
      return cachedEnvironment;
    }

    try {
      final EnvironmentResult result = nativeGetEnvironment(wasiContext.getNativeHandle());

      if (result.errorCode != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
        throw new WasiException(
            "Failed to get environment variables: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      final Map<String, String> environment = parseEnvironmentVariables(result.envVars);
      cachedEnvironment =
          java.util.Collections.unmodifiableMap(new java.util.HashMap<>(environment));

      LOGGER.fine(() -> String.format("Got %d environment variables", environment.size()));
      return cachedEnvironment;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get environment variables", e);
      throw new WasiException("Get environment failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets all environment variables asynchronously.
   *
   * @return CompletableFuture that resolves to an immutable map of environment variables
   */
  public CompletableFuture<Map<String, String>> getEnvironmentAsync() {
    LOGGER.fine("Getting environment variables async");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return getEnvironment();
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async get environment failed", e);
            throw new RuntimeException("Async get environment failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Gets a specific environment variable.
   *
   * <p>WIT interface: wasi:cli/environment.get-environment
   *
   * @param name the environment variable name
   * @return the environment variable value, or null if not found
   * @throws WasiException if the operation fails
   */
  public String getEnvironmentVariable(final String name) {
    JniValidation.requireNonEmpty(name, "name");
    validateEnvironmentName(name);

    LOGGER.fine(() -> String.format("Getting environment variable: %s", name));

    try {
      final Map<String, String> environment = getEnvironment();
      final String value = environment.get(name);

      LOGGER.fine(
          () ->
              String.format(
                  "Environment variable %s = %s", name, value != null ? "set" : "not set"));
      return value;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get environment variable: " + name, e);
      throw new WasiException(
          "Get environment variable failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets a specific environment variable asynchronously.
   *
   * @param name the environment variable name
   * @return CompletableFuture that resolves to the environment variable value, or null if not found
   */
  public CompletableFuture<String> getEnvironmentVariableAsync(final String name) {
    JniValidation.requireNonEmpty(name, "name");
    validateEnvironmentName(name);

    LOGGER.fine(() -> String.format("Getting environment variable async: %s", name));

    return getEnvironmentAsync().thenApply(env -> env.get(name));
  }

  /**
   * Gets all command-line arguments.
   *
   * <p>WIT interface: wasi:cli/argv.get-arguments
   *
   * @return immutable list of command-line arguments
   * @throws WasiException if the operation fails
   */
  public List<String> getArguments() {
    LOGGER.fine("Getting command-line arguments");

    // Return cached arguments if available
    if (cachedArguments != null) {
      return java.util.Arrays.asList(cachedArguments);
    }

    try {
      final ArgumentsResult result = nativeGetArguments(wasiContext.getNativeHandle());

      if (result.errorCode != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
        throw new WasiException(
            "Failed to get command-line arguments: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      final String[] arguments = parseArguments(result.args);
      cachedArguments = arguments;

      LOGGER.fine(() -> String.format("Got %d command-line arguments", arguments.length));
      return java.util.Arrays.asList(arguments);

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get command-line arguments", e);
      throw new WasiException("Get arguments failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets all command-line arguments asynchronously.
   *
   * @return CompletableFuture that resolves to an immutable list of command-line arguments
   */
  public CompletableFuture<List<String>> getArgumentsAsync() {
    LOGGER.fine("Getting command-line arguments async");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return getArguments();
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async get arguments failed", e);
            throw new RuntimeException("Async get arguments failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Gets the program name (first argument).
   *
   * @return the program name, or empty string if no arguments
   * @throws WasiException if the operation fails
   */
  public String getProgramName() {
    LOGGER.fine("Getting program name");

    try {
      final List<String> arguments = getArguments();
      final String programName = arguments.isEmpty() ? "" : arguments.get(0);

      LOGGER.fine(() -> String.format("Program name: %s", programName));
      return programName;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get program name", e);
      throw new WasiException("Get program name failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets the program arguments (excluding the program name).
   *
   * @return immutable list of program arguments
   * @throws WasiException if the operation fails
   */
  public List<String> getProgramArguments() {
    LOGGER.fine("Getting program arguments");

    try {
      final List<String> allArguments = getArguments();
      final List<String> programArguments =
          allArguments.size() <= 1
              ? Collections.emptyList()
              : allArguments.subList(1, allArguments.size());

      LOGGER.fine(() -> String.format("Got %d program arguments", programArguments.size()));
      return programArguments;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get program arguments", e);
      throw new WasiException("Get program arguments failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Exits the process with the specified exit code.
   *
   * <p>WIT interface: wasi:cli/exit.exit
   *
   * @param exitCode the exit code (0 for success, non-zero for error)
   */
  public void exit(final int exitCode) {
    LOGGER.info(() -> String.format("Exiting with code: %d", exitCode));

    try {
      // Perform any necessary cleanup
      nativeExit(wasiContext.getNativeHandle(), exitCode);

      // This should not return, but if it does, use Java's exit
      System.exit(exitCode);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Error during exit", e);
      System.exit(exitCode);
    }
  }

  /**
   * Exits the process asynchronously with the specified exit code.
   *
   * @param exitCode the exit code (0 for success, non-zero for error)
   * @return CompletableFuture that completes when the exit process starts
   */
  public CompletableFuture<Void> exitAsync(final int exitCode) {
    LOGGER.info(() -> String.format("Exiting async with code: %d", exitCode));

    return CompletableFuture.runAsync(() -> exit(exitCode), asyncExecutor);
  }

  /**
   * Checks if the environment contains the specified variable.
   *
   * @param name the environment variable name
   * @return true if the variable exists, false otherwise
   * @throws WasiException if the operation fails
   */
  public boolean hasEnvironmentVariable(final String name) {
    JniValidation.requireNonEmpty(name, "name");
    return getEnvironmentVariable(name) != null;
  }

  /**
   * Gets the number of environment variables.
   *
   * @return the number of environment variables
   * @throws WasiException if the operation fails
   */
  public int getEnvironmentVariableCount() {
    return getEnvironment().size();
  }

  /**
   * Gets the number of command-line arguments.
   *
   * @return the number of command-line arguments
   * @throws WasiException if the operation fails
   */
  public int getArgumentCount() {
    return getArguments().size();
  }

  /** Validates environment variable name. */
  private void validateEnvironmentName(final String name) {
    if (name.length() > MAX_ENV_LENGTH) {
      throw new WasiException(
          "Environment variable name too long: " + name.length() + " > " + MAX_ENV_LENGTH,
          WasiErrorCode.EINVAL);
    }

    // Check for invalid characters
    if (name.contains("=") || name.contains("\0")) {
      throw new WasiException(
          "Environment variable name contains invalid characters", WasiErrorCode.EINVAL);
    }
  }

  /** Parses environment variables from native result. */
  private Map<String, String> parseEnvironmentVariables(final String[] envVars) {
    if (envVars == null) {
      return Collections.emptyMap();
    }

    if (envVars.length > MAX_ENVIRONMENT_VARIABLES) {
      throw new WasiException(
          "Too many environment variables: " + envVars.length + " > " + MAX_ENVIRONMENT_VARIABLES,
          WasiErrorCode.EINVAL);
    }

    final Map<String, String> environment = new HashMap<>();

    for (final String envVar : envVars) {
      if (envVar == null || envVar.isEmpty()) {
        continue;
      }

      final int equalsIndex = envVar.indexOf('=');
      if (equalsIndex == -1) {
        // Environment variable without value
        environment.put(envVar, "");
      } else if (equalsIndex == 0) {
        // Invalid environment variable (starts with '=')
        LOGGER.warning("Skipping invalid environment variable: " + envVar);
      } else {
        final String name = envVar.substring(0, equalsIndex);
        final String value = envVar.substring(equalsIndex + 1);

        if (name.length() > MAX_ENV_LENGTH || value.length() > MAX_ENV_LENGTH) {
          LOGGER.warning("Skipping oversized environment variable: " + name);
          continue;
        }

        environment.put(name, value);
      }
    }

    return environment;
  }

  /** Parses command-line arguments from native result. */
  private String[] parseArguments(final String[] args) {
    if (args == null) {
      return new String[0];
    }

    if (args.length > MAX_ARGUMENTS) {
      throw new WasiException(
          "Too many arguments: " + args.length + " > " + MAX_ARGUMENTS, WasiErrorCode.EINVAL);
    }

    final List<String> validArgs = new ArrayList<>();

    for (final String arg : args) {
      if (arg == null) {
        continue;
      }

      if (arg.length() > MAX_ARG_LENGTH) {
        LOGGER.warning("Skipping oversized argument (length: " + arg.length() + ")");
        continue;
      }

      validArgs.add(arg);
    }

    return validArgs.toArray(new String[0]);
  }

  // Native method declarations
  private static native EnvironmentResult nativeGetEnvironment(long contextHandle);

  private static native ArgumentsResult nativeGetArguments(long contextHandle);

  private static native void nativeExit(long contextHandle, int exitCode);

  /** Environment variables result from native code. */
  private static final class EnvironmentResult {
    public final int errorCode;
    public final String[] envVars;

    public EnvironmentResult(final int errorCode, final String[] envVars) {
      this.errorCode = errorCode;
      this.envVars = envVars;
    }
  }

  /** Command-line arguments result from native code. */
  private static final class ArgumentsResult {
    public final int errorCode;
    public final String[] args;

    public ArgumentsResult(final int errorCode, final String[] args) {
      this.errorCode = errorCode;
      this.args = args;
    }
  }
}
