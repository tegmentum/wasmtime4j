package ai.tegmentum.wasmtime4j.wasi;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Builder for creating WASI component configuration.
 *
 * <p>This builder provides a fluent API for configuring WASI component instantiation settings
 * including environment variables, pre-opened directories, resource limits, and security policies.
 *
 * <p>All methods return the builder instance for method chaining. The builder can be used multiple
 * times to create different configurations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiConfig config = WasiConfig.builder()
 *     .withEnvironment("HOME", "/app")
 *     .withEnvironment("PATH", "/usr/bin:/bin")
 *     .withArgument("--config")
 *     .withArgument("/etc/app.conf")
 *     .withPreopenDirectory("/tmp", Paths.get("/host/tmp"))
 *     .withWorkingDirectory("/app")
 *     .withMemoryLimit(100 * 1024 * 1024)
 *     .withExecutionTimeout(Duration.ofSeconds(30))
 *     .withValidation(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiConfigBuilder {

  /**
   * Adds an environment variable for the component.
   *
   * <p>Environment variables are made available to the component during execution. Existing
   * variables with the same name are replaced.
   *
   * @param name the environment variable name
   * @param value the environment variable value
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name is null or empty, or value is null
   */
  WasiConfigBuilder withEnvironment(final String name, final String value);

  /**
   * Adds multiple environment variables for the component.
   *
   * <p>All provided environment variables are added to the configuration. Existing variables with
   * the same names are replaced.
   *
   * @param environment map of environment variable names to values
   * @return this builder for method chaining
   * @throws IllegalArgumentException if environment is null or contains null keys/values
   */
  WasiConfigBuilder withEnvironment(final Map<String, String> environment);

  /**
   * Removes an environment variable from the configuration.
   *
   * @param name the environment variable name to remove
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name is null or empty
   */
  WasiConfigBuilder withoutEnvironment(final String name);

  /**
   * Clears all environment variables from the configuration.
   *
   * @return this builder for method chaining
   */
  WasiConfigBuilder clearEnvironment();

  /**
   * Adds a command line argument for the component.
   *
   * <p>Arguments are passed to the component in the order they are added.
   *
   * @param argument the command line argument
   * @return this builder for method chaining
   * @throws IllegalArgumentException if argument is null
   */
  WasiConfigBuilder withArgument(final String argument);

  /**
   * Sets the command line arguments for the component.
   *
   * <p>This replaces any previously configured arguments with the provided list.
   *
   * @param arguments list of command line arguments
   * @return this builder for method chaining
   * @throws IllegalArgumentException if arguments is null or contains null elements
   */
  WasiConfigBuilder withArguments(final List<String> arguments);

  /**
   * Removes all command line arguments from the configuration.
   *
   * @return this builder for method chaining
   */
  WasiConfigBuilder clearArguments();

  /**
   * Pre-opens a directory for the component.
   *
   * <p>This makes a host directory available to the component at the specified guest path. The
   * directory must exist and be accessible. Security policies may restrict which directories can be
   * pre-opened.
   *
   * @param guestPath the path where the directory appears inside the component
   * @param hostPath the actual host directory path to expose
   * @return this builder for method chaining
   * @throws IllegalArgumentException if guestPath is null or empty, or hostPath is null
   */
  WasiConfigBuilder withPreopenDirectory(final String guestPath, final Path hostPath);

  /**
   * Adds multiple pre-opened directories for the component.
   *
   * @param directories map of guest paths to host paths
   * @return this builder for method chaining
   * @throws IllegalArgumentException if directories is null or contains null keys/values
   */
  WasiConfigBuilder withPreopenDirectories(final Map<String, Path> directories);

  /**
   * Removes a pre-opened directory from the configuration.
   *
   * @param guestPath the guest path to remove
   * @return this builder for method chaining
   * @throws IllegalArgumentException if guestPath is null or empty
   */
  WasiConfigBuilder withoutPreopenDirectory(final String guestPath);

  /**
   * Clears all pre-opened directories from the configuration.
   *
   * @return this builder for method chaining
   */
  WasiConfigBuilder clearPreopenDirectories();

  /**
   * Sets the working directory for the component.
   *
   * <p>The working directory must be accessible within the component's filesystem view. If not
   * specified, a default working directory is used.
   *
   * @param workingDirectory the working directory path inside the component
   * @return this builder for method chaining
   * @throws IllegalArgumentException if workingDirectory is null or empty
   */
  WasiConfigBuilder withWorkingDirectory(final String workingDirectory);

  /**
   * Removes the working directory setting, using default instead.
   *
   * @return this builder for method chaining
   */
  WasiConfigBuilder withoutWorkingDirectory();


  /**
   * Sets the execution timeout for component operations.
   *
   * <p>Component operations that exceed this timeout will be terminated. This applies to individual
   * function calls and overall component execution.
   *
   * @param timeout maximum execution time for component operations
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiConfigBuilder withExecutionTimeout(final Duration timeout);

  /**
   * Removes the execution timeout, allowing unlimited execution time.
   *
   * @return this builder for method chaining
   */
  WasiConfigBuilder withoutExecutionTimeout();



  /**
   * Adds an import resolver for component dependencies.
   *
   * <p>Import resolvers provide implementations for component imports. Each interface can have at
   * most one resolver.
   *
   * @param interfaceName the interface name to resolve
   * @param resolver the resolver for this interface
   * @return this builder for method chaining
   * @throws IllegalArgumentException if interfaceName is null or empty, or resolver is null
   */
  WasiConfigBuilder withImportResolver(
      final String interfaceName, final WasiImportResolver resolver);

  /**
   * Adds multiple import resolvers for component dependencies.
   *
   * @param resolvers map of interface names to their resolvers
   * @return this builder for method chaining
   * @throws IllegalArgumentException if resolvers is null or contains null keys/values
   */
  WasiConfigBuilder withImportResolvers(final Map<String, WasiImportResolver> resolvers);

  /**
   * Removes an import resolver from the configuration.
   *
   * @param interfaceName the interface name to remove resolver for
   * @return this builder for method chaining
   * @throws IllegalArgumentException if interfaceName is null or empty
   */
  WasiConfigBuilder withoutImportResolver(final String interfaceName);

  /**
   * Clears all import resolvers from the configuration.
   *
   * @return this builder for method chaining
   */
  WasiConfigBuilder clearImportResolvers();

  /**
   * Enables or disables component validation during instantiation.
   *
   * <p>When enabled, the component and configuration are validated for security, compatibility, and
   * resource constraints during instantiation. This is recommended for production environments.
   *
   * @param validate true to enable validation, false to disable
   * @return this builder for method chaining
   */
  WasiConfigBuilder withValidation(final boolean validate);

  /**
   * Enables or disables strict mode for enhanced security and validation.
   *
   * <p>Strict mode enables additional security checks, resource validation, and compatibility
   * constraints. This may reduce performance but increases security guarantees.
   *
   * @param strict true to enable strict mode, false for normal mode
   * @return this builder for method chaining
   */
  WasiConfigBuilder withStrictMode(final boolean strict);

  /**
   * Sets the WASI version to use for the component.
   *
   * <p>This determines which WASI interface version is used for component execution. Preview 1
   * provides traditional POSIX-like operations, while Preview 2 offers component model features and
   * async operations.
   *
   * @param version the WASI version to use
   * @return this builder for method chaining
   * @throws IllegalArgumentException if version is null
   */
  WasiConfigBuilder withWasiVersion(final WasiVersion version);

  /**
   * Enables or disables async operations (WASI Preview 2 only).
   *
   * <p>When enabled, async operations use non-blocking I/O and return CompletableFuture instances.
   * This is only available with WASI Preview 2.
   *
   * @param asyncOperations true to enable async operations, false for synchronous mode
   * @return this builder for method chaining
   */
  WasiConfigBuilder withAsyncOperations(final boolean asyncOperations);

  /**
   * Sets the maximum number of concurrent async operations (WASI Preview 2 only).
   *
   * <p>This limits the number of async operations that can be running concurrently to prevent
   * resource exhaustion. Only applies when async operations are enabled.
   *
   * @param maxOperations maximum concurrent async operations
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxOperations is negative or zero
   */
  WasiConfigBuilder withMaxAsyncOperations(final int maxOperations);

  /**
   * Removes the limit on concurrent async operations.
   *
   * @return this builder for method chaining
   */
  WasiConfigBuilder withoutMaxAsyncOperations();

  /**
   * Sets the default timeout for async operations (WASI Preview 2 only).
   *
   * <p>Async operations that exceed this timeout will be cancelled. This provides protection
   * against hanging operations.
   *
   * @param timeout default timeout for async operations
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiConfigBuilder withAsyncOperationTimeout(final Duration timeout);

  /**
   * Removes the timeout for async operations, allowing unlimited execution time.
   *
   * @return this builder for method chaining
   */
  WasiConfigBuilder withoutAsyncOperationTimeout();

  /**
   * Creates the configured WASI configuration.
   *
   * <p>This method creates an immutable WasiConfig instance with all the configured settings. The
   * builder can continue to be used to create additional configurations.
   *
   * @return a configured WasiConfig instance
   * @throws IllegalArgumentException if the configuration is invalid or incomplete
   */
  WasiConfig build();

  /**
   * Validates the current configuration without building it.
   *
   * <p>This method performs validation of all configuration settings without creating the actual
   * configuration instance. Useful for early error detection.
   *
   * @throws IllegalArgumentException if validation fails with details about the specific issues
   */
  void validate();
}
