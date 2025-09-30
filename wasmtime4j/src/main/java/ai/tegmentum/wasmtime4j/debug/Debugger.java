package ai.tegmentum.wasmtime4j.debug;

/**
 * Debugger interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface Debugger {

  /**
   * Creates a new debug session.
   *
   * @param config debug configuration
   * @return debug session
   */
  DebugSession createSession(DebugConfig config);

  /**
   * Attaches to an existing instance.
   *
   * @param instanceId instance ID to attach to
   * @return debug session
   */
  DebugSession attach(String instanceId);

  /** Detaches from the current session. */
  void detach();

  /**
   * Gets the debugger name.
   *
   * @return debugger name
   */
  String getDebuggerName();

  /**
   * Checks if debugging is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets the event listener.
   *
   * @param listener the event listener
   */
  void setEventListener(DebugEventListener listener);

  /**
   * Gets the current debug session.
   *
   * @return current session or null
   */
  DebugSession getCurrentSession();
}
