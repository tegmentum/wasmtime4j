package ai.tegmentum.wasmtime4j.toolchain;

import ai.tegmentum.wasmtime4j.debug.StepType;
import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Active debugging session for a WebAssembly module.
 *
 * <p>Provides control over execution flow, inspection of variables and memory,
 * and management of breakpoints during debugging.
 *
 * @since 1.0.0
 */
public interface DebuggingSession extends AutoCloseable {

  /**
   * Gets the session identifier.
   *
   * @return unique session ID
   */
  String getSessionId();

  /**
   * Gets when this session was created.
   *
   * @return session creation time
   */
  Instant getCreatedAt();

  /**
   * Checks if the session is currently active.
   *
   * @return true if active
   */
  boolean isActive();

  /**
   * Checks if execution is currently paused.
   *
   * @return true if paused at a breakpoint or step
   */
  boolean isPaused();

  /**
   * Gets the current execution state.
   *
   * @return current execution state
   */
  ExecutionState getExecutionState();

  /**
   * Starts or resumes execution.
   *
   * @throws WasmException if execution cannot be started
   */
  void start() throws WasmException;

  /**
   * Pauses execution at the next instruction.
   *
   * @throws WasmException if execution cannot be paused
   */
  void pause() throws WasmException;

  /**
   * Stops the debugging session.
   *
   * @throws WasmException if session cannot be stopped
   */
  void stop() throws WasmException;

  /**
   * Performs a step operation.
   *
   * @param stepType the type of step to perform
   * @throws WasmException if step operation fails
   */
  void step(final StepType stepType) throws WasmException;

  /**
   * Continues execution until the next breakpoint.
   *
   * @throws WasmException if continue operation fails
   */
  void continueExecution() throws WasmException;

  /**
   * Adds a breakpoint.
   *
   * @param breakpoint the breakpoint to add
   * @return breakpoint ID for future reference
   * @throws WasmException if breakpoint cannot be set
   */
  String addBreakpoint(final DebuggingRequest.Breakpoint breakpoint) throws WasmException;

  /**
   * Removes a breakpoint.
   *
   * @param breakpointId the breakpoint ID to remove
   * @throws WasmException if breakpoint cannot be removed
   */
  void removeBreakpoint(final String breakpointId) throws WasmException;

  /**
   * Lists all breakpoints.
   *
   * @return list of active breakpoints
   */
  List<DebuggingRequest.Breakpoint> listBreakpoints();

  /**
   * Gets the current call stack.
   *
   * @return call stack frames
   * @throws WasmException if call stack cannot be retrieved
   */
  List<StackFrame> getCallStack() throws WasmException;

  /**
   * Gets variables in the current scope.
   *
   * @return map of variable names to values
   * @throws WasmException if variables cannot be retrieved
   */
  Map<String, Object> getVariables() throws WasmException;

  /**
   * Evaluates a watch expression.
   *
   * @param expression the expression to evaluate
   * @return evaluation result
   * @throws WasmException if expression cannot be evaluated
   */
  Object evaluateExpression(final String expression) throws WasmException;

  /**
   * Reads memory at a specific address.
   *
   * @param address the memory address
   * @param length the number of bytes to read
   * @return memory contents
   * @throws WasmException if memory cannot be read
   */
  byte[] readMemory(final long address, final int length) throws WasmException;

  /**
   * Writes memory at a specific address.
   *
   * @param address the memory address
   * @param data the data to write
   * @throws WasmException if memory cannot be written
   */
  void writeMemory(final long address, final byte[] data) throws WasmException;

  /**
   * Gets the current execution location.
   *
   * @return current execution location
   */
  Optional<ExecutionLocation> getCurrentLocation();

  /**
   * Adds a session event listener.
   *
   * @param listener the event listener
   */
  void addEventListener(final SessionEventListener listener);

  /**
   * Removes a session event listener.
   *
   * @param listener the event listener to remove
   */
  void removeEventListener(final SessionEventListener listener);

  /**
   * Execution states for debugging sessions.
   */
  enum ExecutionState {
    /** Session is not started */
    NOT_STARTED,

    /** Execution is running */
    RUNNING,

    /** Execution is paused at a breakpoint */
    PAUSED_AT_BREAKPOINT,

    /** Execution is paused due to a step operation */
    PAUSED_AT_STEP,

    /** Execution is paused due to an exception */
    PAUSED_AT_EXCEPTION,

    /** Execution has completed */
    COMPLETED,

    /** Execution was terminated */
    TERMINATED,

    /** An error occurred */
    ERROR
  }

  /**
   * Stack frame information.
   */
  final class StackFrame {
    private final String functionName;
    private final Optional<String> sourceFile;
    private final int lineNumber;
    private final Map<String, Object> localVariables;

    public StackFrame(final String functionName,
                      final String sourceFile,
                      final int lineNumber,
                      final Map<String, Object> localVariables) {
      this.functionName = Objects.requireNonNull(functionName);
      this.sourceFile = Optional.ofNullable(sourceFile);
      this.lineNumber = lineNumber;
      this.localVariables = Map.copyOf(localVariables);
    }

    public String getFunctionName() { return functionName; }
    public Optional<String> getSourceFile() { return sourceFile; }
    public int getLineNumber() { return lineNumber; }
    public Map<String, Object> getLocalVariables() { return localVariables; }

    @Override
    public String toString() {
      return String.format("StackFrame{function='%s', file=%s, line=%d}",
          functionName, sourceFile.orElse("unknown"), lineNumber);
    }
  }

  /**
   * Execution location information.
   */
  final class ExecutionLocation {
    private final String functionName;
    private final Optional<String> sourceFile;
    private final int lineNumber;
    private final long instructionAddress;

    public ExecutionLocation(final String functionName,
                             final String sourceFile,
                             final int lineNumber,
                             final long instructionAddress) {
      this.functionName = Objects.requireNonNull(functionName);
      this.sourceFile = Optional.ofNullable(sourceFile);
      this.lineNumber = lineNumber;
      this.instructionAddress = instructionAddress;
    }

    public String getFunctionName() { return functionName; }
    public Optional<String> getSourceFile() { return sourceFile; }
    public int getLineNumber() { return lineNumber; }
    public long getInstructionAddress() { return instructionAddress; }

    @Override
    public String toString() {
      return String.format("ExecutionLocation{function='%s', file=%s, line=%d, address=0x%x}",
          functionName, sourceFile.orElse("unknown"), lineNumber, instructionAddress);
    }
  }

  /**
   * Event listener for debugging session events.
   */
  interface SessionEventListener {
    /**
     * Called when execution state changes.
     *
     * @param oldState the previous state
     * @param newState the new state
     */
    void onStateChanged(final ExecutionState oldState, final ExecutionState newState);

    /**
     * Called when a breakpoint is hit.
     *
     * @param breakpoint the breakpoint that was hit
     * @param location the execution location
     */
    void onBreakpointHit(final DebuggingRequest.Breakpoint breakpoint, final ExecutionLocation location);

    /**
     * Called when an exception occurs.
     *
     * @param exception the exception
     * @param location the execution location
     */
    void onException(final Exception exception, final ExecutionLocation location);

    /**
     * Called when execution completes.
     *
     * @param result the execution result
     */
    void onExecutionComplete(final Object result);
  }
}