package ai.tegmentum.wasmtime4j.debug;

/**
 * Debug session interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DebugSession {

  /** Starts the debug session. */
  void start();

  /** Stops the debug session. */
  void stop();

  /**
   * Steps through execution.
   *
   * @param stepType the step type
   */
  void step(StepType stepType);

  /** Continues execution. */
  void continueExecution();

  /**
   * Adds a breakpoint.
   *
   * @param breakpoint the breakpoint to add
   */
  void addBreakpoint(Breakpoint breakpoint);

  /**
   * Removes a breakpoint.
   *
   * @param breakpoint the breakpoint to remove
   */
  void removeBreakpoint(Breakpoint breakpoint);

  /**
   * Gets the session ID.
   *
   * @return session ID
   */
  String getSessionId();

  /**
   * Checks if the session is active.
   *
   * @return true if active
   */
  boolean isActive();

  /** Step type enumeration. */
  enum StepType {
    /** Step into function calls. */
    STEP_INTO,
    /** Step over function calls. */
    STEP_OVER,
    /** Step out of current function. */
    STEP_OUT
  }
}
