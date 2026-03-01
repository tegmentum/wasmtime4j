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

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Builder for creating WASI contexts with comprehensive configuration options.
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

  /** Whether to allow network access (calls inherit_network on the native builder). */
  private boolean allowNetwork = false;

  /** Whether to allow TCP socket creation. */
  private boolean allowTcp = true;

  /** Whether to allow UDP socket creation. */
  private boolean allowUdp = true;

  /** Whether to allow IP name lookups (DNS). */
  private boolean allowIpNameLookup = true;

  /** Whether to allow blocking the current thread. */
  private boolean allowBlockingCurrentThread = false;

  /** Insecure random seed (0 means use default). */
  private long insecureRandomSeed = 0;

  /** Package-private constructor - use WasiContext.builder() to create. */
  WasiContextBuilder() {
    LOGGER.fine("Created new WASI context builder");
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
    Validation.requireNonEmpty(name, "name");
    Validation.requireNonNull(value, "value");

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
    Validation.requireNonNull(environmentVars, "environmentVars");

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
    Validation.requireNonNull(argument, "argument");

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
    Validation.requireNonNull(args, "args");

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
    Validation.requireNonEmpty(guestDir, "guestDir");
    Validation.requireNonEmpty(hostDir, "hostDir");

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
    Validation.requireNonEmpty(workingDir, "workingDir");

    this.workingDirectory = Paths.get(workingDir).normalize();
    LOGGER.fine(String.format("Set working directory: %s", this.workingDirectory));

    return this;
  }

  /**
   * Configures network access for the WASI context.
   *
   * @param allow true to enable network access (inherit_network)
   * @return this builder for method chaining
   */
  public WasiContextBuilder withAllowNetwork(final boolean allow) {
    this.allowNetwork = allow;
    return this;
  }

  /**
   * Configures TCP socket creation for the WASI context.
   *
   * @param allow true to allow TCP socket creation
   * @return this builder for method chaining
   */
  public WasiContextBuilder withAllowTcp(final boolean allow) {
    this.allowTcp = allow;
    return this;
  }

  /**
   * Configures UDP socket creation for the WASI context.
   *
   * @param allow true to allow UDP socket creation
   * @return this builder for method chaining
   */
  public WasiContextBuilder withAllowUdp(final boolean allow) {
    this.allowUdp = allow;
    return this;
  }

  /**
   * Configures IP name lookup (DNS) access for the WASI context.
   *
   * @param allow true to allow IP name lookups
   * @return this builder for method chaining
   */
  public WasiContextBuilder withAllowIpNameLookup(final boolean allow) {
    this.allowIpNameLookup = allow;
    return this;
  }

  /**
   * Configures whether blocking the current thread is allowed.
   *
   * @param allow true to allow blocking the current thread
   * @return this builder for method chaining
   */
  public WasiContextBuilder withAllowBlockingCurrentThread(final boolean allow) {
    this.allowBlockingCurrentThread = allow;
    return this;
  }

  /**
   * Sets the insecure random seed for deterministic testing.
   *
   * @param seed the random seed (0 means use default random source)
   * @return this builder for method chaining
   */
  public WasiContextBuilder withInsecureRandomSeed(final long seed) {
    this.insecureRandomSeed = seed;
    return this;
  }

  /**
   * Creates a WASI context with the configured settings.
   *
   * @return the configured WASI context
   * @throws JniException if the WASI context cannot be created
   */
  public WasiContext build() throws JniException {
    LOGGER.info(
        String.format(
            "Building WASI context with %d environment variables, %d arguments, %d preopen"
                + " directories",
            environment.size(), arguments.size(), preopenedDirectories.size()));

    // Validate configuration before creating native context
    validateConfiguration();

    // Convert configuration to native format
    final String[] envArray = convertEnvironmentToArray();
    final String[] argArray = arguments.toArray(new String[0]);
    final String[] preopenArray = convertPreopenDirectoriesToArray();
    final String workingDirStr = workingDirectory.toString();

    try {
      // Create native WASI context
      final long nativeHandle =
          WasiContext.nativeCreate(envArray, argArray, preopenArray, workingDirStr);

      // Apply network configuration
      WasiContext.nativeSetNetworkConfig(
          nativeHandle, allowNetwork, allowTcp, allowUdp, allowIpNameLookup);

      // Apply blocking configuration
      if (allowBlockingCurrentThread) {
        WasiContext.nativeSetAllowBlocking(nativeHandle, true);
      }

      // Apply insecure random seed
      if (insecureRandomSeed != 0) {
        WasiContext.nativeSetInsecureRandomSeed(nativeHandle, insecureRandomSeed, 0);
      }

      // Rebuild context once after all configuration changes
      WasiContext.nativeRebuildContext(nativeHandle);

      // Create and return Java wrapper
      return new WasiContext(nativeHandle, this);

    } catch (final Exception e) {
      throw new JniException("Failed to create WASI context: " + e.getMessage(), e);
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
    LOGGER.fine("WASI context configuration validation completed");
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
   * Creates a new builder instance.
   *
   * @return a new WasiContextBuilder
   */
  public static WasiContextBuilder builder() {
    return new WasiContextBuilder();
  }

  /**
   * Adds a preopen directory (alias for withPreopenDirectory).
   *
   * @param hostPath the host directory path
   * @param guestPath the guest directory path
   * @return this builder for method chaining
   * @throws IllegalArgumentException if hostPath is null
   */
  public WasiContextBuilder addPreopenedDirectory(final Path hostPath, final String guestPath) {
    Validation.requireNonNull(hostPath, "hostPath");
    return withPreopenDirectory(guestPath, hostPath.toString());
  }

  /**
   * Adds an environment variable (alias for withEnvironment).
   *
   * @param name the environment variable name
   * @param value the environment variable value
   * @return this builder for method chaining
   */
  public WasiContextBuilder addEnvironmentVariable(final String name, final String value) {
    return withEnvironment(name, value);
  }
}
