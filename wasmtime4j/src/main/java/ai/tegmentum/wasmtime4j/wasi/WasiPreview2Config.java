/*
 * Copyright 2024 Tegmentum AI
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for WASI Preview 2 support in the Component Model.
 *
 * <p>WASI Preview 2 provides a rich set of system interfaces for WebAssembly components, including:
 *
 * <ul>
 *   <li>wasi:cli - Command-line interface (args, env, stdio)
 *   <li>wasi:filesystem - File system access
 *   <li>wasi:sockets - Network socket operations
 *   <li>wasi:clocks - Wall clock and monotonic time
 *   <li>wasi:random - Random number generation
 *   <li>wasi:io - Streams and polling
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiPreview2Config config = WasiPreview2Config.builder()
 *     .inheritStdio()
 *     .inheritEnv()
 *     .args("my-app", "--verbose")
 *     .preopenDir("/tmp", "/sandbox/tmp")
 *     .allowNetwork(true)
 *     .build();
 *
 * linker.enableWasiPreview2(config);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiPreview2Config {

  private final List<String> args;
  private final Map<String, String> env;
  private final boolean inheritEnv;
  private final boolean inheritStdio;
  private final List<PreopenDir> preopenDirs;
  private final boolean allowNetwork;
  private final boolean allowClock;
  private final boolean allowRandom;

  private WasiPreview2Config(final Builder builder) {
    this.args = List.copyOf(builder.args);
    this.env = Map.copyOf(builder.env);
    this.inheritEnv = builder.inheritEnv;
    this.inheritStdio = builder.inheritStdio;
    this.preopenDirs = List.copyOf(builder.preopenDirs);
    this.allowNetwork = builder.allowNetwork;
    this.allowClock = builder.allowClock;
    this.allowRandom = builder.allowRandom;
  }

  /**
   * Gets the command-line arguments.
   *
   * @return the arguments
   */
  public List<String> getArgs() {
    return args;
  }

  /**
   * Gets the environment variables.
   *
   * @return the environment variables
   */
  public Map<String, String> getEnv() {
    return env;
  }

  /**
   * Checks if environment should be inherited from the host.
   *
   * @return true if inheriting environment
   */
  public boolean isInheritEnv() {
    return inheritEnv;
  }

  /**
   * Checks if stdio should be inherited from the host.
   *
   * @return true if inheriting stdio
   */
  public boolean isInheritStdio() {
    return inheritStdio;
  }

  /**
   * Gets the preopened directories.
   *
   * @return the preopened directories
   */
  public List<PreopenDir> getPreopenDirs() {
    return preopenDirs;
  }

  /**
   * Checks if network access is allowed.
   *
   * @return true if network is allowed
   */
  public boolean isAllowNetwork() {
    return allowNetwork;
  }

  /**
   * Checks if clock access is allowed.
   *
   * @return true if clock is allowed
   */
  public boolean isAllowClock() {
    return allowClock;
  }

  /**
   * Checks if random number generation is allowed.
   *
   * @return true if random is allowed
   */
  public boolean isAllowRandom() {
    return allowRandom;
  }

  /**
   * Creates a new builder.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default configuration with minimal permissions.
   *
   * @return a minimal config
   */
  public static WasiPreview2Config minimal() {
    return builder().build();
  }

  /**
   * Creates a configuration that inherits stdio and environment from the host.
   *
   * @return a config with inherited stdio and env
   */
  public static WasiPreview2Config inherited() {
    return builder().inheritStdio().inheritEnv().allowClock(true).allowRandom(true).build();
  }

  /** Represents a preopened directory mapping. */
  public static final class PreopenDir {
    private final Path hostPath;
    private final String guestPath;
    private final boolean readOnly;

    /**
     * Creates a preopened directory mapping.
     *
     * @param hostPath the path on the host filesystem
     * @param guestPath the path as seen by the guest
     * @param readOnly whether the directory is read-only
     */
    public PreopenDir(final Path hostPath, final String guestPath, final boolean readOnly) {
      this.hostPath = hostPath;
      this.guestPath = guestPath;
      this.readOnly = readOnly;
    }

    public Path getHostPath() {
      return hostPath;
    }

    public String getGuestPath() {
      return guestPath;
    }

    public boolean isReadOnly() {
      return readOnly;
    }
  }

  /** Builder for WasiPreview2Config. */
  public static final class Builder {
    private final List<String> args = new ArrayList<>();
    private final Map<String, String> env = new HashMap<>();
    private boolean inheritEnv = false;
    private boolean inheritStdio = false;
    private final List<PreopenDir> preopenDirs = new ArrayList<>();
    private boolean allowNetwork = false;
    private boolean allowClock = false;
    private boolean allowRandom = false;

    Builder() {}

    /**
     * Sets command-line arguments.
     *
     * @param args the arguments
     * @return this builder
     */
    public Builder args(final String... args) {
      this.args.clear();
      this.args.addAll(List.of(args));
      return this;
    }

    /**
     * Adds command-line arguments.
     *
     * @param args the arguments to add
     * @return this builder
     */
    public Builder addArgs(final String... args) {
      this.args.addAll(List.of(args));
      return this;
    }

    /**
     * Sets an environment variable.
     *
     * @param key the variable name
     * @param value the variable value
     * @return this builder
     */
    public Builder env(final String key, final String value) {
      this.env.put(key, value);
      return this;
    }

    /**
     * Sets multiple environment variables.
     *
     * @param env the environment variables
     * @return this builder
     */
    public Builder env(final Map<String, String> env) {
      this.env.putAll(env);
      return this;
    }

    /**
     * Inherits environment variables from the host process.
     *
     * @return this builder
     */
    public Builder inheritEnv() {
      this.inheritEnv = true;
      return this;
    }

    /**
     * Inherits stdio (stdin, stdout, stderr) from the host process.
     *
     * @return this builder
     */
    public Builder inheritStdio() {
      this.inheritStdio = true;
      return this;
    }

    /**
     * Preopens a directory with read-write access.
     *
     * @param hostPath the path on the host filesystem
     * @param guestPath the path as seen by the guest
     * @return this builder
     */
    public Builder preopenDir(final Path hostPath, final String guestPath) {
      this.preopenDirs.add(new PreopenDir(hostPath, guestPath, false));
      return this;
    }

    /**
     * Preopens a directory with read-write access using string path.
     *
     * @param hostPath the path on the host filesystem
     * @param guestPath the path as seen by the guest
     * @return this builder
     */
    public Builder preopenDir(final String hostPath, final String guestPath) {
      return preopenDir(Path.of(hostPath), guestPath);
    }

    /**
     * Preopens a directory with specified access mode.
     *
     * @param hostPath the path on the host filesystem
     * @param guestPath the path as seen by the guest
     * @param readOnly whether the directory is read-only
     * @return this builder
     */
    public Builder preopenDir(final Path hostPath, final String guestPath, final boolean readOnly) {
      this.preopenDirs.add(new PreopenDir(hostPath, guestPath, readOnly));
      return this;
    }

    /**
     * Sets whether network access is allowed.
     *
     * @param allow true to allow network
     * @return this builder
     */
    public Builder allowNetwork(final boolean allow) {
      this.allowNetwork = allow;
      return this;
    }

    /**
     * Sets whether clock access is allowed.
     *
     * @param allow true to allow clock
     * @return this builder
     */
    public Builder allowClock(final boolean allow) {
      this.allowClock = allow;
      return this;
    }

    /**
     * Sets whether random number generation is allowed.
     *
     * @param allow true to allow random
     * @return this builder
     */
    public Builder allowRandom(final boolean allow) {
      this.allowRandom = allow;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return the configuration
     */
    public WasiPreview2Config build() {
      return new WasiPreview2Config(this);
    }
  }
}
