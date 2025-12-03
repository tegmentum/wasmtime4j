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

package ai.tegmentum.wasmtime4j.panama.debug;

import ai.tegmentum.wasmtime4j.debug.DebugConfig;
import java.util.Objects;

/**
 * Panama implementation of debug configuration.
 *
 * <p>This class provides configuration options for WebAssembly debugging sessions using Panama FFI.
 *
 * @since 1.0.0
 */
public final class PanamaDebugConfig implements DebugConfig {

  /** Default debug port. */
  public static final int DEFAULT_DEBUG_PORT = 9229;

  /** Default host address. */
  public static final String DEFAULT_HOST_ADDRESS = "127.0.0.1";

  /** Default session timeout in milliseconds (5 minutes). */
  public static final long DEFAULT_SESSION_TIMEOUT = 300_000L;

  /** Default maximum breakpoints. */
  public static final int DEFAULT_MAX_BREAKPOINTS = 1024;

  /** Default log level. */
  public static final String DEFAULT_LOG_LEVEL = "INFO";

  private final int debugPort;
  private final String hostAddress;
  private final boolean remoteDebuggingEnabled;
  private final long sessionTimeout;
  private final boolean breakpointsEnabled;
  private final int maxBreakpoints;
  private final boolean stepDebuggingEnabled;
  private final String logLevel;

  private PanamaDebugConfig(final Builder builder) {
    this.debugPort = builder.debugPort;
    this.hostAddress = builder.hostAddress;
    this.remoteDebuggingEnabled = builder.remoteDebuggingEnabled;
    this.sessionTimeout = builder.sessionTimeout;
    this.breakpointsEnabled = builder.breakpointsEnabled;
    this.maxBreakpoints = builder.maxBreakpoints;
    this.stepDebuggingEnabled = builder.stepDebuggingEnabled;
    this.logLevel = builder.logLevel;
  }

  /**
   * Creates a default debug configuration.
   *
   * @return default configuration
   */
  public static PanamaDebugConfig getDefault() {
    return builder().build();
  }

  /**
   * Creates a builder for debug configuration.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public int getDebugPort() {
    return debugPort;
  }

  @Override
  public String getHostAddress() {
    return hostAddress;
  }

  @Override
  public boolean isRemoteDebuggingEnabled() {
    return remoteDebuggingEnabled;
  }

  @Override
  public long getSessionTimeout() {
    return sessionTimeout;
  }

  @Override
  public boolean isBreakpointsEnabled() {
    return breakpointsEnabled;
  }

  @Override
  public int getMaxBreakpoints() {
    return maxBreakpoints;
  }

  @Override
  public boolean isStepDebuggingEnabled() {
    return stepDebuggingEnabled;
  }

  @Override
  public String getLogLevel() {
    return logLevel;
  }

  @Override
  public String toString() {
    return "PanamaDebugConfig{"
        + "debugPort="
        + debugPort
        + ", hostAddress='"
        + hostAddress
        + '\''
        + ", remoteDebugging="
        + remoteDebuggingEnabled
        + ", sessionTimeout="
        + sessionTimeout
        + ", breakpoints="
        + breakpointsEnabled
        + ", maxBreakpoints="
        + maxBreakpoints
        + ", stepDebugging="
        + stepDebuggingEnabled
        + ", logLevel='"
        + logLevel
        + '\''
        + '}';
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PanamaDebugConfig)) {
      return false;
    }
    final PanamaDebugConfig other = (PanamaDebugConfig) obj;
    return debugPort == other.debugPort
        && remoteDebuggingEnabled == other.remoteDebuggingEnabled
        && sessionTimeout == other.sessionTimeout
        && breakpointsEnabled == other.breakpointsEnabled
        && maxBreakpoints == other.maxBreakpoints
        && stepDebuggingEnabled == other.stepDebuggingEnabled
        && Objects.equals(hostAddress, other.hostAddress)
        && Objects.equals(logLevel, other.logLevel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        debugPort,
        hostAddress,
        remoteDebuggingEnabled,
        sessionTimeout,
        breakpointsEnabled,
        maxBreakpoints,
        stepDebuggingEnabled,
        logLevel);
  }

  /** Builder for debug configuration. */
  public static final class Builder {
    private int debugPort = DEFAULT_DEBUG_PORT;
    private String hostAddress = DEFAULT_HOST_ADDRESS;
    private boolean remoteDebuggingEnabled = false;
    private long sessionTimeout = DEFAULT_SESSION_TIMEOUT;
    private boolean breakpointsEnabled = true;
    private int maxBreakpoints = DEFAULT_MAX_BREAKPOINTS;
    private boolean stepDebuggingEnabled = true;
    private String logLevel = DEFAULT_LOG_LEVEL;

    private Builder() {}

    /**
     * Sets the debug port.
     *
     * @param port the port number
     * @return this builder
     */
    public Builder debugPort(final int port) {
      if (port < 0 || port > 65535) {
        throw new IllegalArgumentException("Port must be between 0 and 65535");
      }
      this.debugPort = port;
      return this;
    }

    /**
     * Sets the host address.
     *
     * @param address the host address
     * @return this builder
     */
    public Builder hostAddress(final String address) {
      this.hostAddress = Objects.requireNonNull(address, "address cannot be null");
      return this;
    }

    /**
     * Enables or disables remote debugging.
     *
     * @param enabled true to enable remote debugging
     * @return this builder
     */
    public Builder remoteDebuggingEnabled(final boolean enabled) {
      this.remoteDebuggingEnabled = enabled;
      return this;
    }

    /**
     * Sets the session timeout.
     *
     * @param timeout the timeout in milliseconds
     * @return this builder
     */
    public Builder sessionTimeout(final long timeout) {
      if (timeout < 0) {
        throw new IllegalArgumentException("Timeout cannot be negative");
      }
      this.sessionTimeout = timeout;
      return this;
    }

    /**
     * Enables or disables breakpoints.
     *
     * @param enabled true to enable breakpoints
     * @return this builder
     */
    public Builder breakpointsEnabled(final boolean enabled) {
      this.breakpointsEnabled = enabled;
      return this;
    }

    /**
     * Sets the maximum number of breakpoints.
     *
     * @param max the maximum breakpoint count
     * @return this builder
     */
    public Builder maxBreakpoints(final int max) {
      if (max < 0) {
        throw new IllegalArgumentException("Max breakpoints cannot be negative");
      }
      this.maxBreakpoints = max;
      return this;
    }

    /**
     * Enables or disables step debugging.
     *
     * @param enabled true to enable step debugging
     * @return this builder
     */
    public Builder stepDebuggingEnabled(final boolean enabled) {
      this.stepDebuggingEnabled = enabled;
      return this;
    }

    /**
     * Sets the log level.
     *
     * @param level the log level
     * @return this builder
     */
    public Builder logLevel(final String level) {
      this.logLevel = Objects.requireNonNull(level, "level cannot be null");
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return the built configuration
     */
    public PanamaDebugConfig build() {
      return new PanamaDebugConfig(this);
    }
  }
}
