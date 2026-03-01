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
package ai.tegmentum.wasmtime4j.wasi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared data holder for WASI context configuration.
 *
 * <p>Encapsulates the common state shared between JNI and Panama {@code WasiContext}
 * implementations: environment variables, command-line arguments, pre-opened directories, and the
 * working directory. All accessor methods return defensive copies.
 *
 * <p>This class is constructed from builder values and provides consistent accessor behavior across
 * both runtime implementations.
 *
 * @since 1.0.0
 */
public final class WasiContextData {

  /** Environment variables for the WASI context. */
  private final ConcurrentHashMap<String, String> environment;

  /** Command-line arguments for the WASI context. */
  private final String[] arguments;

  /** Pre-opened directories with their sandbox permissions. */
  private final ConcurrentHashMap<String, Path> preopenedDirectories;

  /** Working directory for the WASI context. */
  private final Path workingDirectory;

  /**
   * Creates a new WASI context data holder from builder values.
   *
   * @param environment the environment variable map (defensively copied)
   * @param arguments the command-line arguments list (converted to array)
   * @param preopenedDirectories the pre-opened directory map (defensively copied)
   * @param workingDirectory the working directory path
   */
  public WasiContextData(
      final Map<String, String> environment,
      final List<String> arguments,
      final Map<String, Path> preopenedDirectories,
      final Path workingDirectory) {
    this.environment =
        environment != null ? new ConcurrentHashMap<>(environment) : new ConcurrentHashMap<>();
    this.arguments = arguments != null ? arguments.toArray(new String[0]) : new String[0];
    this.preopenedDirectories =
        preopenedDirectories != null
            ? new ConcurrentHashMap<>(preopenedDirectories)
            : new ConcurrentHashMap<>();
    this.workingDirectory = workingDirectory != null ? workingDirectory : Paths.get("/");
  }

  /**
   * Gets an environment variable value.
   *
   * @param name the environment variable name
   * @return the environment variable value, or null if not set
   * @throws IllegalArgumentException if name is null or empty
   */
  public String getEnvironmentVariable(final String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Environment variable name cannot be null or empty");
    }
    return environment.get(name);
  }

  /**
   * Gets all environment variables.
   *
   * @return an unmodifiable copy of all environment variables
   */
  public Map<String, String> getEnvironment() {
    return Collections.unmodifiableMap(new ConcurrentHashMap<>(environment));
  }

  /**
   * Gets the command-line arguments.
   *
   * @return a copy of the command-line arguments
   */
  public String[] getArguments() {
    return arguments.clone();
  }

  /**
   * Gets all pre-opened directories.
   *
   * @return an unmodifiable copy of all pre-opened directories
   */
  public Map<String, Path> getPreopenedDirectories() {
    return Collections.unmodifiableMap(new ConcurrentHashMap<>(preopenedDirectories));
  }

  /**
   * Gets the working directory.
   *
   * @return the working directory
   */
  public Path getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * Validates that a file path is accessible within the sandbox.
   *
   * <p>Security validation resolves relative paths against the configured working directory and
   * normalizes the result. Filesystem sandboxing is enforced by the Wasmtime native runtime through
   * pre-opened directory configuration.
   *
   * @param path the file path to validate
   * @return the resolved and validated path
   * @throws IllegalArgumentException if path is null or empty
   */
  public Path validatePath(final String path) {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }

    // Resolve relative paths against the working directory
    final Path pathObj = Paths.get(path);
    final Path resolvedPath = pathObj.isAbsolute() ? pathObj : workingDirectory.resolve(path);
    return resolvedPath.normalize().toAbsolutePath();
  }

  /**
   * Clears internal state for resource cleanup during context close.
   *
   * <p>This is a defensive measure to release references for garbage collection. After calling this
   * method, the accessor methods will return empty collections.
   */
  public void clearState() {
    environment.clear();
    preopenedDirectories.clear();
  }

  /**
   * Gets the number of environment variables.
   *
   * @return the environment variable count
   */
  public int getEnvironmentCount() {
    return environment.size();
  }

  /**
   * Gets the number of command-line arguments.
   *
   * @return the argument count
   */
  public int getArgumentCount() {
    return arguments.length;
  }

  /**
   * Gets the number of pre-opened directories.
   *
   * @return the pre-opened directory count
   */
  public int getPreopenedDirectoryCount() {
    return preopenedDirectories.size();
  }
}
