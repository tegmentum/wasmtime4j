package ai.tegmentum.wasmtime4j.wasi;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration interface for WASI component instantiation.
 *
 * <p>WasiConfig encapsulates all configuration options needed to instantiate a WASI component,
 * including environment variables, pre-opened directories, resource limits, and security policies.
 *
 * <p>Configurations are immutable once created and can be reused across multiple component
 * instantiations. Use {@link WasiConfigBuilder} to create configured instances.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiConfig config = WasiConfig.builder()
 *     .withEnvironment("HOME", "/app")
 *     .withArgument("--verbose")
 *     .withMemoryLimit(50 * 1024 * 1024)
 *     .build();
 *
 * WasiInstance instance = component.instantiate(config);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiConfig {

  /**
   * Creates a new configuration builder.
   *
   * @return a new WasiConfigBuilder instance
   */
  static WasiConfigBuilder builder() {
    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.WasiConfigBuilder");
      return (WasiConfigBuilder) builderClass.getDeclaredConstructor().newInstance();
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> builderClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.JniWasiConfigBuilder");
        return (WasiConfigBuilder) builderClass.getDeclaredConstructor().newInstance();
      } catch (final ClassNotFoundException e2) {
        throw new UnsupportedOperationException(
            "No WasiConfigBuilder implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create WASI config builder", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create WASI config builder", e);
    }
  }

  /**
   * Creates a default configuration with minimal settings.
   *
   * @return a default WasiConfig instance
   */
  static WasiConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Gets the environment variables for the component.
   *
   * @return immutable map of environment variable names to values
   */
  Map<String, String> getEnvironment();

  /**
   * Gets the command line arguments for the component.
   *
   * @return immutable list of command line arguments
   */
  List<String> getArguments();

  /**
   * Gets the pre-opened directories for the component.
   *
   * @return immutable map of guest paths to host paths
   */
  Map<String, Path> getPreopenDirectories();

  /**
   * Gets the working directory for the component.
   *
   * @return the working directory path, or empty if not specified
   */
  Optional<String> getWorkingDirectory();

  /**
   * Gets the memory limit for the component.
   *
   * @return the memory limit in bytes, or empty if not specified
   */
  Optional<Long> getMemoryLimit();

  /**
   * Gets the execution timeout for component operations.
   *
   * @return the execution timeout, or empty if not specified
   */
  Optional<Duration> getExecutionTimeout();

  /**
   * Gets the resource limits for the component.
   *
   * @return the resource limits, or empty if not specified
   */
  Optional<WasiResourceLimits> getResourceLimits();

  /**
   * Gets the security policy for the component.
   *
   * @return the security policy, or empty if not specified
   */
  Optional<WasiSecurityPolicy> getSecurityPolicy();

  /**
   * Gets the import resolvers for component dependencies.
   *
   * @return immutable map of interface names to their resolvers
   */
  Map<String, WasiImportResolver> getImportResolvers();

  /**
   * Gets whether validation is enabled.
   *
   * @return true if validation is enabled, false otherwise
   */
  boolean isValidationEnabled();

  /**
   * Gets whether strict mode is enabled.
   *
   * @return true if strict mode is enabled, false otherwise
   */
  boolean isStrictModeEnabled();

  /**
   * Creates a new configuration builder based on this configuration.
   *
   * <p>This allows creating modified versions of existing configurations while preserving the
   * original.
   *
   * @return a new WasiConfigBuilder initialized with this configuration's values
   */
  WasiConfigBuilder toBuilder();

  /**
   * Validates this configuration for consistency and completeness.
   *
   * <p>Performs comprehensive validation including path existence checks, resource limit
   * validation, and security policy consistency.
   *
   * @throws IllegalArgumentException if the configuration is invalid
   */
  void validate();
}
