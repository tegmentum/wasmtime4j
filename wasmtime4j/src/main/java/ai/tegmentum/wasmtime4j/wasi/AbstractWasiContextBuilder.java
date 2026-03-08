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
 * Abstract base class for WASI context builders.
 *
 * <p>This class provides all shared configuration logic for building WASI contexts, including
 * environment variables, command-line arguments, pre-opened directories, and network/security
 * settings. Subclasses implement {@link #doBuild()} to perform the runtime-specific native context
 * creation.
 *
 * @param <T> the concrete builder type for fluent method chaining
 * @since 1.0.0
 */
@SuppressWarnings("unchecked")
public abstract class AbstractWasiContextBuilder<T extends AbstractWasiContextBuilder<T>> {

  private static final Logger LOGGER = Logger.getLogger(AbstractWasiContextBuilder.class.getName());

  /** Default working directory if none specified. */
  private static final String DEFAULT_WORKING_DIR = "/";

  private final Map<String, String> environment = new HashMap<>();
  private final List<String> arguments = new ArrayList<>();
  private final Map<String, Path> preopenedDirectories = new HashMap<>();
  private Path workingDirectory = Paths.get(DEFAULT_WORKING_DIR);
  private boolean allowNetwork = false;
  private boolean allowTcp = true;
  private boolean allowUdp = true;
  private boolean allowIpNameLookup = true;
  private boolean allowBlockingCurrentThread = false;
  private long insecureRandomSeed = 0;

  /** Creates a new WASI context builder. */
  protected AbstractWasiContextBuilder() {
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
  public T withEnvironment(final String name, final String value) {
    Validation.requireNonEmpty(name, "name");
    Validation.requireNonNull(value, "value");

    environment.put(name, value);
    LOGGER.fine(String.format("Added environment variable: %s", name));

    return (T) this;
  }

  /**
   * Sets multiple environment variables for the WASI context.
   *
   * @param environmentVars the environment variables to set
   * @return this builder for method chaining
   * @throws IllegalArgumentException if environmentVars is null
   */
  public T withEnvironment(final Map<String, String> environmentVars) {
    Validation.requireNonNull(environmentVars, "environmentVars");

    for (final Map.Entry<String, String> entry : environmentVars.entrySet()) {
      withEnvironment(entry.getKey(), entry.getValue());
    }

    return (T) this;
  }

  /**
   * Inherits environment variables from the current process.
   *
   * @return this builder for method chaining
   */
  public T withInheritedEnvironment() {
    withEnvironment(System.getenv());
    LOGGER.fine("Inherited environment variables from host process");

    return (T) this;
  }

  /**
   * Adds a command-line argument to the WASI context.
   *
   * @param argument the command-line argument to add
   * @return this builder for method chaining
   * @throws IllegalArgumentException if argument is null
   */
  public T withArgument(final String argument) {
    Validation.requireNonNull(argument, "argument");

    arguments.add(argument);
    LOGGER.fine(String.format("Added argument: %s", argument));

    return (T) this;
  }

  /**
   * Adds multiple command-line arguments to the WASI context.
   *
   * @param args the command-line arguments to add
   * @return this builder for method chaining
   * @throws IllegalArgumentException if args is null
   */
  public T withArguments(final String... args) {
    Validation.requireNonNull(args, "args");

    for (final String arg : args) {
      withArgument(arg);
    }

    return (T) this;
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
  public T withPreopenDirectory(final String guestDir, final String hostDir) {
    Validation.requireNonEmpty(guestDir, "guestDir");
    Validation.requireNonEmpty(hostDir, "hostDir");

    final Path hostPath = Paths.get(hostDir).toAbsolutePath().normalize();

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

    return (T) this;
  }

  /**
   * Pre-opens a directory using the same path for both guest and host.
   *
   * @param directory the directory path to pre-open
   * @return this builder for method chaining
   * @throws IllegalArgumentException if directory path is invalid
   */
  public T withPreopenDirectory(final String directory) {
    return withPreopenDirectory(directory, directory);
  }

  /**
   * Sets the working directory for the WASI context.
   *
   * @param workingDir the working directory path
   * @return this builder for method chaining
   * @throws IllegalArgumentException if workingDir is null/empty
   */
  public T withWorkingDirectory(final String workingDir) {
    Validation.requireNonEmpty(workingDir, "workingDir");

    this.workingDirectory = Paths.get(workingDir).normalize();
    LOGGER.fine(String.format("Set working directory: %s", this.workingDirectory));

    return (T) this;
  }

  /**
   * Configures network access for the WASI context.
   *
   * @param allow true to enable network access (inherit_network)
   * @return this builder for method chaining
   */
  public T withAllowNetwork(final boolean allow) {
    this.allowNetwork = allow;
    return (T) this;
  }

  /**
   * Configures TCP socket creation for the WASI context.
   *
   * @param allow true to allow TCP socket creation
   * @return this builder for method chaining
   */
  public T withAllowTcp(final boolean allow) {
    this.allowTcp = allow;
    return (T) this;
  }

  /**
   * Configures UDP socket creation for the WASI context.
   *
   * @param allow true to allow UDP socket creation
   * @return this builder for method chaining
   */
  public T withAllowUdp(final boolean allow) {
    this.allowUdp = allow;
    return (T) this;
  }

  /**
   * Configures IP name lookup (DNS) access for the WASI context.
   *
   * @param allow true to allow IP name lookups
   * @return this builder for method chaining
   */
  public T withAllowIpNameLookup(final boolean allow) {
    this.allowIpNameLookup = allow;
    return (T) this;
  }

  /**
   * Configures whether blocking the current thread is allowed.
   *
   * @param allow true to allow blocking the current thread
   * @return this builder for method chaining
   */
  public T withAllowBlockingCurrentThread(final boolean allow) {
    this.allowBlockingCurrentThread = allow;
    return (T) this;
  }

  /**
   * Sets the insecure random seed for deterministic testing.
   *
   * @param seed the random seed (0 means use default random source)
   * @return this builder for method chaining
   */
  public T withInsecureRandomSeed(final long seed) {
    this.insecureRandomSeed = seed;
    return (T) this;
  }

  /**
   * Gets the configured environment variables.
   *
   * @return a copy of the environment variables map
   */
  public Map<String, String> getEnvironment() {
    return new HashMap<>(environment);
  }

  /**
   * Gets the configured arguments.
   *
   * @return a copy of the arguments list
   */
  public List<String> getArguments() {
    return new ArrayList<>(arguments);
  }

  /**
   * Gets the configured pre-opened directories.
   *
   * @return a copy of the pre-opened directories map
   */
  public Map<String, Path> getPreopenedDirectories() {
    return new HashMap<>(preopenedDirectories);
  }

  /**
   * Gets the configured working directory.
   *
   * @return the working directory path
   */
  public Path getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * Gets whether network access is allowed.
   *
   * @return true if network access is allowed
   */
  protected boolean isAllowNetwork() {
    return allowNetwork;
  }

  /**
   * Gets whether TCP socket creation is allowed.
   *
   * @return true if TCP is allowed
   */
  protected boolean isAllowTcp() {
    return allowTcp;
  }

  /**
   * Gets whether UDP socket creation is allowed.
   *
   * @return true if UDP is allowed
   */
  protected boolean isAllowUdp() {
    return allowUdp;
  }

  /**
   * Gets whether IP name lookups are allowed.
   *
   * @return true if IP name lookups are allowed
   */
  protected boolean isAllowIpNameLookup() {
    return allowIpNameLookup;
  }

  /**
   * Gets whether blocking the current thread is allowed.
   *
   * @return true if blocking is allowed
   */
  protected boolean isAllowBlockingCurrentThread() {
    return allowBlockingCurrentThread;
  }

  /**
   * Gets the insecure random seed.
   *
   * @return the random seed (0 means default)
   */
  protected long getInsecureRandomSeed() {
    return insecureRandomSeed;
  }

  /**
   * Validates the current configuration. Subclasses may override.
   *
   * <p>Currently a no-op that logs the validation step.
   */
  protected void validateConfiguration() {
    LOGGER.fine("Validating WASI context configuration");
  }

  /**
   * Converts the environment map to a flat string array of "key=value" entries.
   *
   * @return the environment as a string array
   */
  protected String[] convertEnvironmentToArray() {
    final String[] envArray = new String[environment.size()];
    int i = 0;
    for (final Map.Entry<String, String> entry : environment.entrySet()) {
      envArray[i++] = entry.getKey() + "=" + entry.getValue();
    }
    return envArray;
  }

  /**
   * Converts the pre-opened directories to a flat string array of alternating guest/host paths.
   *
   * @return the pre-opened directories as a string array
   */
  protected String[] convertPreopenDirectoriesToArray() {
    final String[] preopenArray = new String[preopenedDirectories.size() * 2];
    int i = 0;
    for (final Map.Entry<String, Path> entry : preopenedDirectories.entrySet()) {
      preopenArray[i++] = entry.getKey();
      preopenArray[i++] = entry.getValue().toString();
    }
    return preopenArray;
  }
}
