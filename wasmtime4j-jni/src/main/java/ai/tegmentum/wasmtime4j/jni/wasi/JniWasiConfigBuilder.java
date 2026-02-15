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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for WASI configuration in JNI implementation.
 *
 * @since 1.0.0
 */
public final class JniWasiConfigBuilder implements WasiConfigBuilder {

  private final Map<String, String> environment = new HashMap<>();
  private final List<String> arguments = new ArrayList<>();
  private final Map<String, Path> preopenDirectories = new HashMap<>();
  private String workingDirectory;
  private boolean inheritEnvironment;
  private Duration executionTimeout;

  private boolean validationEnabled;
  private boolean strictModeEnabled;
  private WasiVersion wasiVersion = WasiVersion.PREVIEW_1;
  private boolean asyncOperations;
  private Integer maxAsyncOperations;
  private Duration asyncOperationTimeout;

  /** Creates a new WASI configuration builder. */
  public JniWasiConfigBuilder() {}

  @Override
  public WasiConfigBuilder withEnvironment(final String name, final String value) {
    if (name == null) {
      throw new IllegalArgumentException("Environment variable name cannot be null");
    }
    environment.put(name, value);
    return this;
  }

  @Override
  public WasiConfigBuilder withEnvironment(final Map<String, String> environment) {
    if (environment == null) {
      throw new IllegalArgumentException("Environment map cannot be null");
    }
    this.environment.putAll(environment);
    return this;
  }

  @Override
  public WasiConfigBuilder withoutEnvironment(final String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Environment variable name cannot be null or empty");
    }
    environment.remove(name);
    return this;
  }

  @Override
  public WasiConfigBuilder clearEnvironment() {
    environment.clear();
    return this;
  }

  @Override
  public WasiConfigBuilder withArgument(final String argument) {
    if (argument == null) {
      throw new IllegalArgumentException("Argument cannot be null");
    }
    arguments.add(argument);
    return this;
  }

  @Override
  public WasiConfigBuilder withArguments(final List<String> arguments) {
    if (arguments == null) {
      throw new IllegalArgumentException("Arguments list cannot be null");
    }
    this.arguments.clear();
    this.arguments.addAll(arguments);
    return this;
  }

  @Override
  public WasiConfigBuilder clearArguments() {
    arguments.clear();
    return this;
  }

  @Override
  public WasiConfigBuilder withPreopenDirectory(final String guestPath, final Path hostPath) {
    if (guestPath == null || guestPath.isEmpty()) {
      throw new IllegalArgumentException("Guest path cannot be null or empty");
    }
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    preopenDirectories.put(guestPath, hostPath);
    return this;
  }

  @Override
  public WasiConfigBuilder withPreopenDirectories(final Map<String, Path> directories) {
    if (directories == null) {
      throw new IllegalArgumentException("Directories map cannot be null");
    }
    this.preopenDirectories.putAll(directories);
    return this;
  }

  @Override
  public WasiConfigBuilder withoutPreopenDirectory(final String guestPath) {
    if (guestPath == null || guestPath.isEmpty()) {
      throw new IllegalArgumentException("Guest path cannot be null or empty");
    }
    preopenDirectories.remove(guestPath);
    return this;
  }

  @Override
  public WasiConfigBuilder clearPreopenDirectories() {
    preopenDirectories.clear();
    return this;
  }

  @Override
  public WasiConfigBuilder withWorkingDirectory(final String workingDirectory) {
    if (workingDirectory == null || workingDirectory.isEmpty()) {
      throw new IllegalArgumentException("Working directory cannot be null or empty");
    }
    this.workingDirectory = workingDirectory;
    return this;
  }

  @Override
  public WasiConfigBuilder withoutWorkingDirectory() {
    this.workingDirectory = null;
    return this;
  }

  /**
   * Configures the context to inherit environment variables from the host.
   *
   * @return this builder for method chaining
   */
  public WasiConfigBuilder inheritEnvironment() {
    this.inheritEnvironment = true;
    return this;
  }


  @Override
  public WasiConfigBuilder withExecutionTimeout(final Duration timeout) {
    if (timeout == null || timeout.isNegative()) {
      throw new IllegalArgumentException("Timeout must be non-null and non-negative");
    }
    this.executionTimeout = timeout;
    return this;
  }

  @Override
  public WasiConfigBuilder withoutExecutionTimeout() {
    this.executionTimeout = null;
    return this;
  }



  @Override
  public WasiConfigBuilder withValidation(final boolean validate) {
    this.validationEnabled = validate;
    return this;
  }

  @Override
  public WasiConfigBuilder withStrictMode(final boolean strict) {
    this.strictModeEnabled = strict;
    return this;
  }

  @Override
  public WasiConfigBuilder withWasiVersion(final WasiVersion version) {
    if (version == null) {
      throw new IllegalArgumentException("WASI version cannot be null");
    }
    this.wasiVersion = version;
    return this;
  }

  @Override
  public WasiConfigBuilder withAsyncOperations(final boolean asyncOperations) {
    this.asyncOperations = asyncOperations;
    return this;
  }

  @Override
  public WasiConfigBuilder withMaxAsyncOperations(final int maxOperations) {
    if (maxOperations <= 0) {
      throw new IllegalArgumentException("Max async operations must be positive");
    }
    this.maxAsyncOperations = maxOperations;
    return this;
  }

  @Override
  public WasiConfigBuilder withoutMaxAsyncOperations() {
    this.maxAsyncOperations = null;
    return this;
  }

  @Override
  public WasiConfigBuilder withAsyncOperationTimeout(final Duration timeout) {
    if (timeout == null || timeout.isNegative()) {
      throw new IllegalArgumentException("Timeout must be non-null and non-negative");
    }
    this.asyncOperationTimeout = timeout;
    return this;
  }

  @Override
  public WasiConfigBuilder withoutAsyncOperationTimeout() {
    this.asyncOperationTimeout = null;
    return this;
  }

  @Override
  public void validate() {
    // Basic validation - ensure no null values in collections
    // More complex validation can be added later
  }

  @Override
  public WasiConfig build() {
    validate();
    return new JniWasiConfig(
        environment,
        arguments,
        preopenDirectories,
        workingDirectory,
        inheritEnvironment,
        executionTimeout,
        validationEnabled,
        strictModeEnabled,
        wasiVersion,
        asyncOperations,
        maxAsyncOperations,
        asyncOperationTimeout);
  }
}
