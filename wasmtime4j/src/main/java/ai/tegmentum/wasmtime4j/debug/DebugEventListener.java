package ai.tegmentum.wasmtime4j.debug;

/**
 * Debug event listener interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DebugEventListener {

  /**
   * Called when a breakpoint is hit.
   *
   * @param event the debug event
   */
  void onBreakpoint(DebugEvent event);

  /**
   * Called when execution is paused.
   *
   * @param event the debug event
   */
  void onPaused(DebugEvent event);

  /**
   * Called when execution is resumed.
   *
   * @param event the debug event
   */
  void onResumed(DebugEvent event);

  /**
   * Called when an exception occurs.
   *
   * @param event the debug event
   */
  void onException(DebugEvent event);

  /**
   * Called when the session ends.
   *
   * @param event the debug event
   */
  void onSessionEnd(DebugEvent event);

  /** Debug event interface. */
  interface DebugEvent {
    /**
     * Gets the event type.
     *
     * @return event type
     */
    String getEventType();

    /**
     * Gets the event timestamp.
     *
     * @return timestamp in milliseconds
     */
    long getTimestamp();

    /**
     * Gets the event data.
     *
     * @return event data
     */
    String getEventData();

    /**
     * Gets the session ID.
     *
     * @return session ID
     */
    String getSessionId();
  }
}
