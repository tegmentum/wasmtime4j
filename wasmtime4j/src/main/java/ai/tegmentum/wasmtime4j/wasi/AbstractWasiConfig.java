package ai.tegmentum.wasmtime4j.wasi;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared base class for WASI configuration implementations.
 *
 * <p>This class holds all configuration state and provides all getter implementations. Subclasses
 * only need to implement {@link #createBuilder()} to return the runtime-specific builder for {@link
 * #toBuilder()}.
 *
 * <p>Instances are immutable once created.
 *
 * @since 1.0.0
 */
public abstract class AbstractWasiConfig implements WasiConfig {

  private final Map<String, String> environment;
  private final List<String> arguments;
  private final Map<String, Path> preopenDirectories;
  private final String workingDirectory;
  private final boolean inheritEnvironment;
  private final Duration executionTimeout;

  private final boolean validationEnabled;
  private final boolean strictModeEnabled;
  private final WasiVersion wasiVersion;
  private final boolean asyncOperations;
  private final Integer maxAsyncOperations;
  private final Duration asyncOperationTimeout;

  /**
   * Creates a new WASI configuration.
   *
   * @param environment environment variables
   * @param arguments command line arguments
   * @param preopenDirectories pre-opened directories
   * @param workingDirectory working directory
   * @param inheritEnvironment whether to inherit host environment
   * @param executionTimeout execution timeout
   * @param validationEnabled whether validation is enabled
   * @param strictModeEnabled whether strict mode is enabled
   * @param wasiVersion WASI version
   * @param asyncOperations whether async operations are enabled
   * @param maxAsyncOperations maximum concurrent async operations
   * @param asyncOperationTimeout timeout for async operations
   */
  protected AbstractWasiConfig(
      final Map<String, String> environment,
      final List<String> arguments,
      final Map<String, Path> preopenDirectories,
      final String workingDirectory,
      final boolean inheritEnvironment,
      final Duration executionTimeout,
      final boolean validationEnabled,
      final boolean strictModeEnabled,
      final WasiVersion wasiVersion,
      final boolean asyncOperations,
      final Integer maxAsyncOperations,
      final Duration asyncOperationTimeout) {
    this.environment = Collections.unmodifiableMap(new HashMap<>(environment));
    this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
    this.preopenDirectories = Collections.unmodifiableMap(new HashMap<>(preopenDirectories));
    this.workingDirectory = workingDirectory;
    this.inheritEnvironment = inheritEnvironment;
    this.executionTimeout = executionTimeout;
    this.validationEnabled = validationEnabled;
    this.strictModeEnabled = strictModeEnabled;
    this.wasiVersion = wasiVersion != null ? wasiVersion : WasiVersion.PREVIEW_1;
    this.asyncOperations = asyncOperations;
    this.maxAsyncOperations = maxAsyncOperations;
    this.asyncOperationTimeout = asyncOperationTimeout;
  }

  /**
   * Creates a runtime-specific builder for this configuration.
   *
   * @return a new empty builder of the appropriate runtime type
   */
  protected abstract AbstractWasiConfigBuilder createBuilder();

  @Override
  public Map<String, String> getEnvironment() {
    return environment;
  }

  @Override
  public List<String> getArguments() {
    return arguments;
  }

  @Override
  public Map<String, Path> getPreopenDirectories() {
    return preopenDirectories;
  }

  @Override
  public Optional<String> getWorkingDirectory() {
    return Optional.ofNullable(workingDirectory);
  }

  @Override
  public Optional<Duration> getExecutionTimeout() {
    return Optional.ofNullable(executionTimeout);
  }

  @Override
  public boolean isValidationEnabled() {
    return validationEnabled;
  }

  @Override
  public boolean isStrictModeEnabled() {
    return strictModeEnabled;
  }

  @Override
  public WasiConfigBuilder toBuilder() {
    final AbstractWasiConfigBuilder builder = createBuilder();
    builder.withEnvironment(environment);
    builder.withArguments(arguments);
    builder.withPreopenDirectories(preopenDirectories);
    if (workingDirectory != null) {
      builder.withWorkingDirectory(workingDirectory);
    }
    if (inheritEnvironment) {
      builder.inheritEnvironment();
    }
    if (executionTimeout != null) {
      builder.withExecutionTimeout(executionTimeout);
    }
    builder.withValidation(validationEnabled);
    builder.withStrictMode(strictModeEnabled);
    builder.withWasiVersion(wasiVersion);
    builder.withAsyncOperations(asyncOperations);
    if (maxAsyncOperations != null) {
      builder.withMaxAsyncOperations(maxAsyncOperations);
    }
    if (asyncOperationTimeout != null) {
      builder.withAsyncOperationTimeout(asyncOperationTimeout);
    }
    return builder;
  }

  @Override
  public WasiVersion getWasiVersion() {
    return wasiVersion;
  }

  /**
   * Returns whether async operations are enabled.
   *
   * @return true if async operations are enabled
   */
  public boolean isAsyncOperationsEnabled() {
    return asyncOperations;
  }

  /**
   * Returns the maximum number of concurrent async operations.
   *
   * @return the max async operations, or empty if not set
   */
  public Optional<Integer> getMaxAsyncOperations() {
    return Optional.ofNullable(maxAsyncOperations);
  }

  /**
   * Returns the timeout for async operations.
   *
   * @return the async operation timeout, or empty if not set
   */
  public Optional<Duration> getAsyncOperationTimeout() {
    return Optional.ofNullable(asyncOperationTimeout);
  }

  /**
   * Returns whether to inherit environment variables from host.
   *
   * @return true if host environment should be inherited
   */
  public boolean isInheritEnvironment() {
    return inheritEnvironment;
  }

  @Override
  public void validate() {
    throw new UnsupportedOperationException("validate not yet implemented");
  }
}
