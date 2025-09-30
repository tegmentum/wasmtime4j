package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution context configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionContextConfig {

  /**
   * Gets the configuration name.
   *
   * @return configuration name
   */
  String getName();

  /**
   * Gets the execution timeout in milliseconds.
   *
   * @return execution timeout
   */
  long getTimeout();

  /**
   * Sets the execution timeout.
   *
   * @param timeoutMs timeout in milliseconds
   */
  void setTimeout(long timeoutMs);

  /**
   * Gets the memory limit in bytes.
   *
   * @return memory limit
   */
  long getMemoryLimit();

  /**
   * Sets the memory limit.
   *
   * @param limitBytes memory limit in bytes
   */
  void setMemoryLimit(long limitBytes);

  /**
   * Checks if debugging is enabled.
   *
   * @return true if debugging enabled
   */
  boolean isDebuggingEnabled();

  /**
   * Sets debugging enabled state.
   *
   * @param enabled debugging enabled
   */
  void setDebuggingEnabled(boolean enabled);

  /**
   * Checks if profiling is enabled.
   *
   * @return true if profiling enabled
   */
  boolean isProfilingEnabled();

  /**
   * Sets profiling enabled state.
   *
   * @param enabled profiling enabled
   */
  void setProfilingEnabled(boolean enabled);

  /**
   * Gets the thread pool size.
   *
   * @return thread pool size
   */
  int getThreadPoolSize();

  /**
   * Sets the thread pool size.
   *
   * @param size thread pool size
   */
  void setThreadPoolSize(int size);

  /**
   * Gets security configuration.
   *
   * @return security config
   */
  SecurityConfig getSecurityConfig();

  /**
   * Sets security configuration.
   *
   * @param config security config
   */
  void setSecurityConfig(SecurityConfig config);

  /** Security configuration interface. */
  interface SecurityConfig {
    /**
     * Checks if sandboxing is enabled.
     *
     * @return true if sandboxing enabled
     */
    boolean isSandboxingEnabled();

    /**
     * Gets allowed system calls.
     *
     * @return set of allowed system calls
     */
    java.util.Set<String> getAllowedSystemCalls();

    /**
     * Gets security level.
     *
     * @return security level
     */
    SecurityLevel getSecurityLevel();

    /**
     * Checks if network access is allowed.
     *
     * @return true if network access allowed
     */
    boolean isNetworkAccessAllowed();

    /**
     * Checks if file system access is allowed.
     *
     * @return true if file system access allowed
     */
    boolean isFileSystemAccessAllowed();
  }

  /** Security level enumeration. */
  enum SecurityLevel {
    /** Minimal security. */
    LOW,
    /** Standard security. */
    MEDIUM,
    /** High security. */
    HIGH,
    /** Maximum security. */
    STRICT
  }
}
