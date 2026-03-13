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

import ai.tegmentum.wasmtime4j.wasi.clocks.WasiMonotonicClock;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiWallClock;
import ai.tegmentum.wasmtime4j.wasi.random.WasiRandomSource;
import ai.tegmentum.wasmtime4j.wasi.sockets.SocketAddrCheck;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
  private final boolean inheritArgs;
  private final Map<String, String> env;
  private final boolean inheritEnv;
  private final boolean inheritStdio;
  private final boolean inheritStdin;
  private final boolean inheritStdout;
  private final boolean inheritStderr;
  private final WasiStdioConfig stdinConfig;
  private final WasiStdioConfig stdoutConfig;
  private final WasiStdioConfig stderrConfig;
  private final List<PreopenDir> preopenDirs;
  private final boolean allowNetwork;
  private final boolean allowTcp;
  private final boolean allowUdp;
  private final boolean allowIpNameLookup;
  private final boolean allowBlockingCurrentThread;
  private final long insecureRandomSeed;
  private final boolean hasInsecureRandomSeed;
  private final long maxRandomSize;
  private final boolean hasMaxRandomSize;
  private final WasiWallClock wallClock;
  private final WasiMonotonicClock monotonicClock;
  private final WasiRandomSource secureRandom;
  private final WasiRandomSource insecureRandom;
  private final SocketAddrCheck socketAddrCheck;

  private WasiPreview2Config(final Builder builder) {
    this.args = Collections.unmodifiableList(new ArrayList<>(builder.args));
    this.inheritArgs = builder.inheritArgs;
    this.env = Collections.unmodifiableMap(new HashMap<>(builder.env));
    this.inheritEnv = builder.inheritEnv;
    this.inheritStdio = builder.inheritStdio;
    this.inheritStdin = builder.inheritStdin;
    this.inheritStdout = builder.inheritStdout;
    this.inheritStderr = builder.inheritStderr;
    this.stdinConfig = builder.stdinConfig;
    this.stdoutConfig = builder.stdoutConfig;
    this.stderrConfig = builder.stderrConfig;
    this.preopenDirs = Collections.unmodifiableList(new ArrayList<>(builder.preopenDirs));
    this.allowNetwork = builder.allowNetwork;
    this.allowTcp = builder.allowTcp;
    this.allowUdp = builder.allowUdp;
    this.allowIpNameLookup = builder.allowIpNameLookup;
    this.allowBlockingCurrentThread = builder.allowBlockingCurrentThread;
    this.insecureRandomSeed = builder.insecureRandomSeed;
    this.hasInsecureRandomSeed = builder.hasInsecureRandomSeed;
    this.maxRandomSize = builder.maxRandomSize;
    this.hasMaxRandomSize = builder.hasMaxRandomSize;
    this.wallClock = builder.wallClock;
    this.monotonicClock = builder.monotonicClock;
    this.secureRandom = builder.secureRandom;
    this.insecureRandom = builder.insecureRandom;
    this.socketAddrCheck = builder.socketAddrCheck;
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
   * Checks if arguments should be inherited from the host process.
   *
   * @return true if inheriting arguments
   * @since 1.1.0
   */
  public boolean isInheritArgs() {
    return inheritArgs;
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
   * Checks if stdin should be inherited from the host individually.
   *
   * @return true if inheriting stdin
   */
  public boolean isInheritStdin() {
    return inheritStdin;
  }

  /**
   * Checks if stdout should be inherited from the host individually.
   *
   * @return true if inheriting stdout
   */
  public boolean isInheritStdout() {
    return inheritStdout;
  }

  /**
   * Checks if stderr should be inherited from the host individually.
   *
   * @return true if inheriting stderr
   */
  public boolean isInheritStderr() {
    return inheritStderr;
  }

  /**
   * Gets the custom stdin configuration, if set.
   *
   * <p>When non-null, this provides fine-grained control over stdin behavior. Supported modes
   * include {@link WasiStdioConfig.Type#INHERIT INHERIT}, {@link WasiStdioConfig.Type#INPUT_STREAM
   * INPUT_STREAM}, and {@link WasiStdioConfig.Type#NULL NULL}. When null, the behavior is
   * determined by {@link #isInheritStdin()} and {@link #isInheritStdio()}.
   *
   * @return the stdin configuration, or null if not explicitly configured
   * @since 1.1.0
   */
  public WasiStdioConfig getStdinConfig() {
    return stdinConfig;
  }

  /**
   * Gets the custom stdout configuration, if set.
   *
   * <p>When non-null, this provides fine-grained control over stdout behavior. Supported modes
   * include {@link WasiStdioConfig.Type#INHERIT INHERIT} and {@link WasiStdioConfig.Type#NULL
   * NULL}. When null, the behavior is determined by {@link #isInheritStdout()} and {@link
   * #isInheritStdio()}.
   *
   * @return the stdout configuration, or null if not explicitly configured
   * @since 1.1.0
   */
  public WasiStdioConfig getStdoutConfig() {
    return stdoutConfig;
  }

  /**
   * Gets the custom stderr configuration, if set.
   *
   * <p>When non-null, this provides fine-grained control over stderr behavior. Supported modes
   * include {@link WasiStdioConfig.Type#INHERIT INHERIT} and {@link WasiStdioConfig.Type#NULL
   * NULL}. When null, the behavior is determined by {@link #isInheritStderr()} and {@link
   * #isInheritStdio()}.
   *
   * @return the stderr configuration, or null if not explicitly configured
   * @since 1.1.0
   */
  public WasiStdioConfig getStderrConfig() {
    return stderrConfig;
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
   * Checks if TCP socket access is allowed.
   *
   * @return true if TCP is allowed
   */
  public boolean isAllowTcp() {
    return allowTcp;
  }

  /**
   * Checks if UDP socket access is allowed.
   *
   * @return true if UDP is allowed
   */
  public boolean isAllowUdp() {
    return allowUdp;
  }

  /**
   * Checks if IP name lookup (DNS) is allowed.
   *
   * @return true if IP name lookup is allowed
   */
  public boolean isAllowIpNameLookup() {
    return allowIpNameLookup;
  }

  /**
   * Checks if blocking the current thread is allowed.
   *
   * <p>When true, WASI operations may block the current thread for I/O.
   *
   * @return true if blocking the current thread is allowed
   */
  public boolean isAllowBlockingCurrentThread() {
    return allowBlockingCurrentThread;
  }

  /**
   * Gets the insecure random seed value.
   *
   * <p>This provides a deterministic seed for insecure random number generation, useful for testing
   * and reproducibility.
   *
   * @return the insecure random seed
   */
  public long getInsecureRandomSeed() {
    return insecureRandomSeed;
  }

  /**
   * Checks if an insecure random seed has been explicitly set.
   *
   * @return true if an insecure random seed has been set
   */
  public boolean hasInsecureRandomSeed() {
    return hasInsecureRandomSeed;
  }

  /**
   * Gets the maximum size in bytes for random byte generation via {@code
   * wasi:random/random.get-random-bytes} and {@code
   * wasi:random/insecure.get-insecure-random-bytes}.
   *
   * <p>Calls requesting more than this many bytes will trap. The default in Wasmtime is 64 MiB.
   *
   * @return the maximum random size in bytes
   */
  public long getMaxRandomSize() {
    return maxRandomSize;
  }

  /**
   * Checks if a maximum random size has been explicitly set.
   *
   * @return true if a maximum random size has been set
   */
  public boolean hasMaxRandomSize() {
    return hasMaxRandomSize;
  }

  /**
   * Gets the custom wall clock, if set.
   *
   * @return the custom wall clock, or null if using the default
   */
  public WasiWallClock getWallClock() {
    return wallClock;
  }

  /**
   * Gets the custom monotonic clock, if set.
   *
   * @return the custom monotonic clock, or null if using the default
   */
  public WasiMonotonicClock getMonotonicClock() {
    return monotonicClock;
  }

  /**
   * Gets the custom secure random source, if set.
   *
   * @return the custom secure random source, or null if using the default
   */
  public WasiRandomSource getSecureRandom() {
    return secureRandom;
  }

  /**
   * Gets the custom insecure random source, if set.
   *
   * @return the custom insecure random source, or null if using the default
   */
  public WasiRandomSource getInsecureRandom() {
    return insecureRandom;
  }

  /**
   * Gets the socket address check callback, if set.
   *
   * @return the socket address check callback, or null if no check is configured
   */
  public SocketAddrCheck getSocketAddrCheck() {
    return socketAddrCheck;
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
    return builder().inheritStdio().inheritArgs().inheritEnv().build();
  }

  /** Represents a preopened directory mapping. */
  public static final class PreopenDir {
    private final Path hostPath;
    private final String guestPath;
    private final boolean readOnly;
    private final DirPerms dirPerms;
    private final FilePerms filePerms;

    /**
     * Creates a preopened directory mapping with simple read-only flag.
     *
     * @param hostPath the path on the host filesystem
     * @param guestPath the path as seen by the guest
     * @param readOnly whether the directory is read-only
     */
    public PreopenDir(final Path hostPath, final String guestPath, final boolean readOnly) {
      this.hostPath = hostPath;
      this.guestPath = guestPath;
      this.readOnly = readOnly;
      this.dirPerms = readOnly ? DirPerms.readOnly() : DirPerms.all();
      this.filePerms = readOnly ? FilePerms.readOnly() : FilePerms.all();
    }

    /**
     * Creates a preopened directory mapping with granular permissions.
     *
     * @param hostPath the path on the host filesystem
     * @param guestPath the path as seen by the guest
     * @param dirPerms the directory permissions
     * @param filePerms the file permissions
     */
    public PreopenDir(
        final Path hostPath,
        final String guestPath,
        final DirPerms dirPerms,
        final FilePerms filePerms) {
      this.hostPath = hostPath;
      this.guestPath = guestPath;
      this.dirPerms = dirPerms;
      this.filePerms = filePerms;
      this.readOnly = !dirPerms.canMutate() && !filePerms.canWrite();
    }

    /**
     * Gets the host path.
     *
     * @return the host path
     */
    public Path getHostPath() {
      return hostPath;
    }

    /**
     * Gets the guest path.
     *
     * @return the guest path
     */
    public String getGuestPath() {
      return guestPath;
    }

    /**
     * Checks if the directory is read-only.
     *
     * @return true if read-only
     */
    public boolean isReadOnly() {
      return readOnly;
    }

    /**
     * Gets the directory permissions.
     *
     * @return the directory permissions
     */
    public DirPerms getDirPerms() {
      return dirPerms;
    }

    /**
     * Gets the file permissions.
     *
     * @return the file permissions
     */
    public FilePerms getFilePerms() {
      return filePerms;
    }
  }

  /** Builder for WasiPreview2Config. */
  public static final class Builder {
    private final List<String> args = new ArrayList<>();
    private boolean inheritArgs = false;
    private final Map<String, String> env = new HashMap<>();
    private boolean inheritEnv = false;
    private boolean inheritStdio = false;
    private boolean inheritStdin = false;
    private boolean inheritStdout = false;
    private boolean inheritStderr = false;
    private WasiStdioConfig stdinConfig = null;
    private WasiStdioConfig stdoutConfig = null;
    private WasiStdioConfig stderrConfig = null;
    private final List<PreopenDir> preopenDirs = new ArrayList<>();
    private boolean allowNetwork = false;
    private boolean allowTcp = true;
    private boolean allowUdp = true;
    private boolean allowIpNameLookup = true;
    private boolean allowBlockingCurrentThread = false;
    private long insecureRandomSeed = 0;
    private boolean hasInsecureRandomSeed = false;
    private long maxRandomSize = 0;
    private boolean hasMaxRandomSize = false;
    private WasiWallClock wallClock = null;
    private WasiMonotonicClock monotonicClock = null;
    private WasiRandomSource secureRandom = null;
    private WasiRandomSource insecureRandom = null;
    private SocketAddrCheck socketAddrCheck = null;

    Builder() {}

    /**
     * Sets command-line arguments.
     *
     * @param args the arguments
     * @return this builder
     */
    public Builder args(final String... args) {
      this.args.clear();
      this.args.addAll(Arrays.asList(args));
      return this;
    }

    /**
     * Adds command-line arguments.
     *
     * @param args the arguments to add
     * @return this builder
     */
    public Builder addArgs(final String... args) {
      this.args.addAll(Arrays.asList(args));
      return this;
    }

    /**
     * Inherits command-line arguments from the host process.
     *
     * <p>When enabled, the host process's command-line arguments are passed to the WASI component.
     *
     * @return this builder
     * @since 1.1.0
     */
    public Builder inheritArgs() {
      this.inheritArgs = true;
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
     * Inherits only stdin from the host process.
     *
     * @return this builder
     */
    public Builder inheritStdin() {
      this.inheritStdin = true;
      return this;
    }

    /**
     * Inherits only stdout from the host process.
     *
     * @return this builder
     */
    public Builder inheritStdout() {
      this.inheritStdout = true;
      return this;
    }

    /**
     * Inherits only stderr from the host process.
     *
     * @return this builder
     */
    public Builder inheritStderr() {
      this.inheritStderr = true;
      return this;
    }

    /**
     * Configures stdin using a {@link WasiStdioConfig}.
     *
     * <p>This provides fine-grained control over the WASI stdin stream. Supported configurations:
     *
     * <ul>
     *   <li>{@link WasiStdioConfig#inherit()} — inherit stdin from the host process
     *   <li>{@link WasiStdioConfig#fromInputStream(java.io.InputStream)} — read stdin from a Java
     *       InputStream (eagerly reads all bytes at initialization)
     *   <li>{@link WasiStdioConfig#nulled()} — provide an empty stdin (EOF immediately)
     * </ul>
     *
     * <p>When set, this takes precedence over {@link #inheritStdin()}.
     *
     * @param config the stdin configuration
     * @return this builder
     * @since 1.1.0
     */
    public Builder stdin(final WasiStdioConfig config) {
      this.stdinConfig = config;
      return this;
    }

    /**
     * Configures stdout using a {@link WasiStdioConfig}.
     *
     * <p>This provides fine-grained control over the WASI stdout stream. Supported configurations:
     *
     * <ul>
     *   <li>{@link WasiStdioConfig#inherit()} — inherit stdout from the host process
     *   <li>{@link WasiStdioConfig#nulled()} — discard all stdout output
     * </ul>
     *
     * <p>When set, this takes precedence over {@link #inheritStdout()}.
     *
     * <p>Note: {@link WasiStdioConfig#fromOutputStream(java.io.OutputStream)} is not yet supported
     * for stdout in the component model. Use the core module {@code WasiLinker} for OutputStream
     * support.
     *
     * @param config the stdout configuration
     * @return this builder
     * @since 1.1.0
     */
    public Builder stdout(final WasiStdioConfig config) {
      this.stdoutConfig = config;
      return this;
    }

    /**
     * Configures stderr using a {@link WasiStdioConfig}.
     *
     * <p>This provides fine-grained control over the WASI stderr stream. Supported configurations:
     *
     * <ul>
     *   <li>{@link WasiStdioConfig#inherit()} — inherit stderr from the host process
     *   <li>{@link WasiStdioConfig#nulled()} — discard all stderr output
     * </ul>
     *
     * <p>When set, this takes precedence over {@link #inheritStderr()}.
     *
     * <p>Note: {@link WasiStdioConfig#fromOutputStream(java.io.OutputStream)} is not yet supported
     * for stderr in the component model. Use the core module {@code WasiLinker} for OutputStream
     * support.
     *
     * @param config the stderr configuration
     * @return this builder
     * @since 1.1.0
     */
    public Builder stderr(final WasiStdioConfig config) {
      this.stderrConfig = config;
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
     * Preopens a directory with granular permissions.
     *
     * @param hostPath the path on the host filesystem
     * @param guestPath the path as seen by the guest
     * @param dirPerms the directory permissions
     * @param filePerms the file permissions
     * @return this builder
     */
    public Builder preopenDir(
        final Path hostPath,
        final String guestPath,
        final DirPerms dirPerms,
        final FilePerms filePerms) {
      this.preopenDirs.add(new PreopenDir(hostPath, guestPath, dirPerms, filePerms));
      return this;
    }

    /**
     * Preopens a directory with granular permissions using string path.
     *
     * @param hostPath the path on the host filesystem
     * @param guestPath the path as seen by the guest
     * @param dirPerms the directory permissions
     * @param filePerms the file permissions
     * @return this builder
     */
    public Builder preopenDir(
        final String hostPath,
        final String guestPath,
        final DirPerms dirPerms,
        final FilePerms filePerms) {
      return preopenDir(Path.of(hostPath), guestPath, dirPerms, filePerms);
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
     * Sets whether TCP socket access is allowed.
     *
     * <p>Defaults to true. When set to false, TCP socket operations will be denied.
     *
     * @param allow true to allow TCP
     * @return this builder
     */
    public Builder allowTcp(final boolean allow) {
      this.allowTcp = allow;
      return this;
    }

    /**
     * Sets whether UDP socket access is allowed.
     *
     * <p>Defaults to true. When set to false, UDP socket operations will be denied.
     *
     * @param allow true to allow UDP
     * @return this builder
     */
    public Builder allowUdp(final boolean allow) {
      this.allowUdp = allow;
      return this;
    }

    /**
     * Sets whether IP name lookup (DNS) is allowed.
     *
     * <p>Defaults to true. When set to false, DNS lookup operations will be denied.
     *
     * @param allow true to allow IP name lookup
     * @return this builder
     */
    public Builder allowIpNameLookup(final boolean allow) {
      this.allowIpNameLookup = allow;
      return this;
    }

    /**
     * Sets whether blocking the current thread is allowed for WASI operations.
     *
     * <p>When true, WASI I/O operations may block the calling thread. When false (default),
     * blocking operations will return an error.
     *
     * @param allow true to allow blocking
     * @return this builder
     */
    public Builder allowBlockingCurrentThread(final boolean allow) {
      this.allowBlockingCurrentThread = allow;
      return this;
    }

    /**
     * Sets a deterministic seed for insecure random number generation.
     *
     * <p>This is useful for testing and reproducibility. The WASI insecure random seed is
     * internally a 128-bit value, but Java {@code long} is 64-bit. The provided seed is used as the
     * low 64 bits of the 128-bit seed, with the upper 64 bits set to zero. This means the effective
     * entropy is limited to 64 bits rather than the full 128 bits that Rust's {@code u128} type
     * supports.
     *
     * @param seed the insecure random seed (lower 64 bits of the 128-bit WASI seed)
     * @return this builder
     */
    public Builder insecureRandomSeed(final long seed) {
      this.insecureRandomSeed = seed;
      this.hasInsecureRandomSeed = true;
      return this;
    }

    /**
     * Sets the maximum number of bytes that may be requested from random byte generation.
     *
     * <p>This limits the {@code len} parameter of {@code wasi:random/random.get-random-bytes} and
     * {@code wasi:random/insecure.get-insecure-random-bytes}. Calls exceeding this limit will trap.
     *
     * <p>The default in Wasmtime is 64 MiB (67,108,864 bytes). This can be tightened for security
     * hardening against denial-of-service, or loosened if the use case requires larger buffers.
     *
     * @param maxSize the maximum random size in bytes (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if maxSize is not positive
     */
    public Builder maxRandomSize(final long maxSize) {
      if (maxSize <= 0) {
        throw new IllegalArgumentException("maxRandomSize must be positive: " + maxSize);
      }
      this.maxRandomSize = maxSize;
      this.hasMaxRandomSize = true;
      return this;
    }

    /**
     * Sets a custom wall clock implementation.
     *
     * <p>By default, the host's wall clock is used. This allows overriding with a custom
     * implementation for testing or deterministic behavior.
     *
     * @param clock the custom wall clock implementation
     * @return this builder
     */
    public Builder wallClock(final WasiWallClock clock) {
      this.wallClock = clock;
      return this;
    }

    /**
     * Sets a custom monotonic clock implementation.
     *
     * <p>By default, the host's monotonic clock is used. This allows overriding with a custom
     * implementation for testing or deterministic behavior.
     *
     * @param clock the custom monotonic clock implementation
     * @return this builder
     */
    public Builder monotonicClock(final WasiMonotonicClock clock) {
      this.monotonicClock = clock;
      return this;
    }

    /**
     * Sets a custom secure random source.
     *
     * <p>By default, the host's secure random source is used. The provided source must generate
     * cryptographically secure random data.
     *
     * @param random the custom secure random source
     * @return this builder
     */
    public Builder secureRandom(final WasiRandomSource random) {
      this.secureRandom = random;
      return this;
    }

    /**
     * Sets a custom insecure random source.
     *
     * <p>By default, the host's insecure random source is used. This is useful for deterministic
     * testing.
     *
     * @param random the custom insecure random source
     * @return this builder
     */
    public Builder insecureRandom(final WasiRandomSource random) {
      this.insecureRandom = random;
      return this;
    }

    /**
     * Sets a socket address check callback.
     *
     * <p>This callback is invoked for each socket operation to determine whether it should be
     * permitted. Returning {@code true} allows the operation, {@code false} denies it.
     *
     * @param check the socket address check callback
     * @return this builder
     */
    public Builder socketAddrCheck(final SocketAddrCheck check) {
      this.socketAddrCheck = check;
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
