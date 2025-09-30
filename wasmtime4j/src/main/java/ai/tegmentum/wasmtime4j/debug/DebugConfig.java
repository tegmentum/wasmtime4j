package ai.tegmentum.wasmtime4j.debug;

/**
 * Debug configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DebugConfig {

  /**
   * Gets the debug port.
   *
   * @return debug port number
   */
  int getDebugPort();

  /**
   * Gets the host address.
   *
   * @return host address
   */
  String getHostAddress();

  /**
   * Checks if remote debugging is enabled.
   *
   * @return true if remote debugging is enabled
   */
  boolean isRemoteDebuggingEnabled();

  /**
   * Gets the session timeout in milliseconds.
   *
   * @return session timeout
   */
  long getSessionTimeout();

  /**
   * Checks if breakpoints are enabled.
   *
   * @return true if breakpoints are enabled
   */
  boolean isBreakpointsEnabled();

  /**
   * Gets the maximum breakpoint count.
   *
   * @return maximum breakpoint count
   */
  int getMaxBreakpoints();

  /**
   * Checks if step debugging is enabled.
   *
   * @return true if step debugging is enabled
   */
  boolean isStepDebuggingEnabled();

  /**
   * Gets the debug log level.
   *
   * @return debug log level
   */
  String getLogLevel();
}
