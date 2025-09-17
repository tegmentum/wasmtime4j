package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Builder for creating and configuring WASI components.
 *
 * <p>This builder provides a fluent API for configuring WASI components with comprehensive options
 * including environment variables, pre-opened directories, resource limits, and security
 * constraints.
 *
 * <p>The builder supports both loading components from bytecode and configuring their runtime
 * environment. Components can be validated before instantiation to ensure compatibility and
 * security requirements.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiComponent component = WasiComponentBuilder.create()
 *     .fromBytes(wasmComponentBytes)
 *     .withName("my-component")
 *     .withEnvironment("HOME", "/app")
 *     .withPreopenDirectory("/tmp", "/host/tmp")
 *     .withMemoryLimit(100 * 1024 * 1024) // 100MB
 *     .withExecutionTimeout(Duration.ofSeconds(30))
 *     .withSecurityPolicy(policy)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiComponentBuilder {

  /**
   * Creates a new builder instance.
   *
   * @return a new WasiComponentBuilder
   */
  static WasiComponentBuilder create() {
    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> builderClass = Class.forName(
          "ai.tegmentum.wasmtime4j.panama.wasi.WasiComponentBuilder");
      return (WasiComponentBuilder) builderClass.getDeclaredConstructor().newInstance();
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> builderClass = Class.forName(
            "ai.tegmentum.wasmtime4j.jni.wasi.JniWasiComponentBuilder");
        return (WasiComponentBuilder) builderClass.getDeclaredConstructor().newInstance();
      } catch (final ClassNotFoundException e2) {
        throw new UnsupportedOperationException(
            "No WasiComponentBuilder implementation available. "
            + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create WASI component builder", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create WASI component builder", e);
    }
  }

  /**
   * Sets the WebAssembly component bytecode to load.
   *
   * @param wasmBytes the WebAssembly component bytecode
   * @return this builder for method chaining
   * @throws IllegalArgumentException if wasmBytes is null or empty
   */
  WasiComponentBuilder fromBytes(final byte[] wasmBytes);

  /**
   * Loads the WebAssembly component from a file.
   *
   * @param filePath path to the WebAssembly component file
   * @return this builder for method chaining
   * @throws IllegalArgumentException if filePath is null
   * @throws WasmException if the file cannot be read or contains invalid bytecode
   */
  WasiComponentBuilder fromFile(final Path filePath) throws WasmException;

  /**
   * Sets the component name for identification and logging.
   *
   * @param name the component name
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name is null or empty
   */
  WasiComponentBuilder withName(final String name);

  /**
   * Adds an environment variable for the component.
   *
   * @param name the environment variable name
   * @param value the environment variable value
   * @return this builder for method chaining
   * @throws IllegalArgumentException if name is null or empty
   */
  WasiComponentBuilder withEnvironment(final String name, final String value);

  /**
   * Adds multiple environment variables for the component.
   *
   * @param environment map of environment variable names to values
   * @return this builder for method chaining
   * @throws IllegalArgumentException if environment is null
   */
  WasiComponentBuilder withEnvironment(final Map<String, String> environment);

  /**
   * Adds a command line argument for the component.
   *
   * @param argument the command line argument
   * @return this builder for method chaining
   * @throws IllegalArgumentException if argument is null
   */
  WasiComponentBuilder withArgument(final String argument);

  /**
   * Sets the command line arguments for the component.
   *
   * @param arguments list of command line arguments
   * @return this builder for method chaining
   * @throws IllegalArgumentException if arguments is null
   */
  WasiComponentBuilder withArguments(final List<String> arguments);

  /**
   * Pre-opens a directory for the component.
   *
   * <p>This makes a host directory available to the component at the specified guest path. The
   * directory is subject to security and permission constraints.
   *
   * @param guestPath the path where the directory appears inside the component
   * @param hostPath the actual host directory path to expose
   * @return this builder for method chaining
   * @throws IllegalArgumentException if guestPath or hostPath is null
   */
  WasiComponentBuilder withPreopenDirectory(final String guestPath, final Path hostPath);

  /**
   * Sets the working directory for the component.
   *
   * @param workingDirectory the working directory path inside the component
   * @return this builder for method chaining
   * @throws IllegalArgumentException if workingDirectory is null
   */
  WasiComponentBuilder withWorkingDirectory(final String workingDirectory);

  /**
   * Sets the maximum memory limit for the component.
   *
   * @param bytes maximum memory in bytes
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative
   */
  WasiComponentBuilder withMemoryLimit(final long bytes);

  /**
   * Sets the execution timeout for component operations.
   *
   * @param timeout maximum execution time for component operations
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiComponentBuilder withExecutionTimeout(final Duration timeout);

  /**
   * Sets resource limits for the component.
   *
   * @param limits resource limits to apply
   * @return this builder for method chaining
   * @throws IllegalArgumentException if limits is null
   */
  WasiComponentBuilder withResourceLimits(final WasiResourceLimits limits);

  /**
   * Sets the security policy for the component.
   *
   * @param policy security policy to enforce
   * @return this builder for method chaining
   * @throws IllegalArgumentException if policy is null
   */
  WasiComponentBuilder withSecurityPolicy(final WasiSecurityPolicy policy);

  /**
   * Adds an import resolver for component dependencies.
   *
   * @param interfaceName the interface name to resolve
   * @param resolver the resolver for this interface
   * @return this builder for method chaining
   * @throws IllegalArgumentException if interfaceName or resolver is null
   */
  WasiComponentBuilder withImportResolver(
      final String interfaceName, final WasiImportResolver resolver);

  /**
   * Sets import resolvers for component dependencies.
   *
   * @param resolvers map of interface names to their resolvers
   * @return this builder for method chaining
   * @throws IllegalArgumentException if resolvers is null
   */
  WasiComponentBuilder withImportResolvers(final Map<String, WasiImportResolver> resolvers);

  /**
   * Enables or disables component validation during build.
   *
   * <p>When enabled, the component is validated for security, compatibility, and resource
   * constraints during the build process. This is recommended for production environments.
   *
   * @param validate true to enable validation, false to disable
   * @return this builder for method chaining
   */
  WasiComponentBuilder withValidation(final boolean validate);

  /**
   * Enables strict mode for enhanced security and validation.
   *
   * <p>Strict mode enables additional security checks, resource validation, and compatibility
   * constraints. This may reduce performance but increases security guarantees.
   *
   * @param strict true to enable strict mode, false for normal mode
   * @return this builder for method chaining
   */
  WasiComponentBuilder withStrictMode(final boolean strict);

  /**
   * Creates the configured WASI component.
   *
   * <p>This method processes the provided bytecode, applies all configuration settings, and creates
   * a ready-to-use WasiComponent instance. The component can then be instantiated and executed.
   *
   * @return a configured WasiComponent instance
   * @throws WasmException if component creation fails due to invalid bytecode, configuration
   *     errors, or security violations
   * @throws IllegalStateException if required configuration is missing (e.g., no bytecode
   *     specified)
   */
  WasiComponent build() throws WasmException;

  /**
   * Validates the current configuration without building the component.
   *
   * <p>This method performs validation of all configuration settings and checks bytecode validity
   * without creating the actual component instance. Useful for early error detection.
   *
   * @throws WasmException if validation fails with details about the specific issues
   * @throws IllegalStateException if required configuration is missing
   */
  void validate() throws WasmException;
}
