/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiConfigBuilder;
import ai.tegmentum.wasmtime4j.wasi.WasiVersion;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JNI implementation of WASI configuration.
 *
 * <p>This class holds configuration options for WASI context creation. Instances are immutable once
 * created.
 *
 * @since 1.0.0
 */
public final class JniWasiConfig implements WasiConfig {

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
  JniWasiConfig(
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
    final JniWasiConfigBuilder builder = new JniWasiConfigBuilder();
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
    // Basic validation - ensure no null values
    // More complex validation can be added later
  }
}
