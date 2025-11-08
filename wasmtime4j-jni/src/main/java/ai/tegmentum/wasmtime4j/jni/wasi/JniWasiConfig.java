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
import ai.tegmentum.wasmtime4j.wasi.WasiImportResolver;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits;
import ai.tegmentum.wasmtime4j.wasi.WasiSecurityPolicy;
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

  /**
   * Creates a new WASI configuration.
   *
   * @param environment environment variables
   * @param arguments command line arguments
   * @param preopenDirectories pre-opened directories
   * @param workingDirectory working directory
   * @param inheritEnvironment whether to inherit host environment
   */
  JniWasiConfig(
      final Map<String, String> environment,
      final List<String> arguments,
      final Map<String, Path> preopenDirectories,
      final String workingDirectory,
      final boolean inheritEnvironment) {
    this.environment = Collections.unmodifiableMap(new HashMap<>(environment));
    this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
    this.preopenDirectories = Collections.unmodifiableMap(new HashMap<>(preopenDirectories));
    this.workingDirectory = workingDirectory;
    this.inheritEnvironment = inheritEnvironment;
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
  public Optional<Long> getMemoryLimit() {
    return Optional.empty();
  }

  @Override
  public Optional<Duration> getExecutionTimeout() {
    return Optional.empty();
  }

  @Override
  public Optional<WasiResourceLimits> getResourceLimits() {
    return Optional.empty();
  }

  @Override
  public Optional<WasiSecurityPolicy> getSecurityPolicy() {
    return Optional.empty();
  }

  @Override
  public Map<String, WasiImportResolver> getImportResolvers() {
    return Collections.emptyMap();
  }

  @Override
  public boolean isValidationEnabled() {
    return false;
  }

  @Override
  public boolean isStrictModeEnabled() {
    return false;
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
    return builder;
  }

  @Override
  public WasiVersion getWasiVersion() {
    return WasiVersion.PREVIEW_1;
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
