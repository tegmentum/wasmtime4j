package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main interface for WebAssembly debugging sessions.
 *
 * <p>A DebugSession provides comprehensive debugging capabilities including breakpoint management,
 * step execution, variable inspection, and memory analysis. Each session manages the debugging
 * state for one or more WebAssembly instances.
 *
 * <p>Example usage:
 * <pre>{@code
 * try (DebugSession session = debugger.createSession(instance)) {
 *     session.setBreakpoint("main", 42);
 *     session.continueExecution();
 *
 *     DebugEvent event = session.waitForEvent();
 *     if (event.getType() == DebugEventType.BREAKPOINT_HIT) {
 *         List<StackFrame> stack = session.getStackTrace();
 *         // Inspect variables and memory...
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface DebugSession extends Closeable {

    /**
     * Gets the unique session identifier.
     *
     * @return the session ID
     */
    String getSessionId();

    /**
     * Gets the instances being debugged in this session.
     *
     * @return list of debugged instances
     */
    List<Instance> getInstances();

    /**
     * Sets a breakpoint at the specified function and line.
     *
     * @param functionName the function name
     * @param line the line number
     * @return the created breakpoint
     * @throws WasmException if breakpoint cannot be set
     * @throws IllegalArgumentException if functionName is null or line is negative
     */
    Breakpoint setBreakpoint(final String functionName, final int line) throws WasmException;

    /**
     * Sets a breakpoint at the specified WebAssembly instruction offset.
     *
     * @param functionName the function name
     * @param instructionOffset the instruction offset
     * @return the created breakpoint
     * @throws WasmException if breakpoint cannot be set
     * @throws IllegalArgumentException if functionName is null or instructionOffset is negative
     */
    Breakpoint setBreakpoint(final String functionName, final long instructionOffset) throws WasmException;

    /**
     * Removes a breakpoint.
     *
     * @param breakpoint the breakpoint to remove
     * @return true if the breakpoint was successfully removed
     * @throws WasmException if removal fails
     * @throws IllegalArgumentException if breakpoint is null
     */
    boolean removeBreakpoint(final Breakpoint breakpoint) throws WasmException;

    /**
     * Removes all breakpoints in the specified function.
     *
     * @param functionName the function name
     * @return number of breakpoints removed
     * @throws WasmException if removal fails
     * @throws IllegalArgumentException if functionName is null
     */
    int removeBreakpoints(final String functionName) throws WasmException;

    /**
     * Gets all active breakpoints.
     *
     * @return list of active breakpoints
     */
    List<Breakpoint> getBreakpoints();

    /**
     * Continues execution until the next breakpoint or completion.
     *
     * @return future that completes when execution stops
     * @throws WasmException if execution fails
     */
    CompletableFuture<DebugEvent> continueExecution() throws WasmException;

    /**
     * Steps into the next instruction, entering function calls.
     *
     * @return future that completes when step is complete
     * @throws WasmException if step fails
     */
    CompletableFuture<DebugEvent> stepInto() throws WasmException;

    /**
     * Steps over the next instruction, not entering function calls.
     *
     * @return future that completes when step is complete
     * @throws WasmException if step fails
     */
    CompletableFuture<DebugEvent> stepOver() throws WasmException;

    /**
     * Steps out of the current function.
     *
     * @return future that completes when step is complete
     * @throws WasmException if step fails
     */
    CompletableFuture<DebugEvent> stepOut() throws WasmException;

    /**
     * Pauses execution at the current instruction.
     *
     * @return future that completes when execution is paused
     * @throws WasmException if pause fails
     */
    CompletableFuture<DebugEvent> pause() throws WasmException;

    /**
     * Gets the current call stack.
     *
     * @return list of stack frames
     * @throws WasmException if stack trace cannot be retrieved
     */
    List<StackFrame> getStackTrace() throws WasmException;

    /**
     * Gets variables in the current scope.
     *
     * @return list of variables
     * @throws WasmException if variables cannot be retrieved
     */
    List<Variable> getCurrentVariables() throws WasmException;

    /**
     * Gets variables in the specified stack frame.
     *
     * @param frameIndex the stack frame index (0 = current frame)
     * @return list of variables
     * @throws WasmException if variables cannot be retrieved
     * @throws IndexOutOfBoundsException if frameIndex is invalid
     */
    List<Variable> getVariables(final int frameIndex) throws WasmException;

    /**
     * Gets the value of a specific variable.
     *
     * @param variableName the variable name
     * @return the variable value
     * @throws WasmException if variable cannot be retrieved
     * @throws IllegalArgumentException if variableName is null
     */
    VariableValue getVariableValue(final String variableName) throws WasmException;

    /**
     * Sets the value of a specific variable.
     *
     * @param variableName the variable name
     * @param value the new value
     * @throws WasmException if variable cannot be set
     * @throws IllegalArgumentException if variableName or value is null
     */
    void setVariableValue(final String variableName, final VariableValue value) throws WasmException;

    /**
     * Reads memory at the specified address.
     *
     * @param address the memory address
     * @param length the number of bytes to read
     * @return the memory contents
     * @throws WasmException if memory cannot be read
     * @throws IllegalArgumentException if address is negative or length is non-positive
     */
    byte[] readMemory(final long address, final int length) throws WasmException;

    /**
     * Writes memory at the specified address.
     *
     * @param address the memory address
     * @param data the data to write
     * @throws WasmException if memory cannot be written
     * @throws IllegalArgumentException if address is negative or data is null
     */
    void writeMemory(final long address, final byte[] data) throws WasmException;

    /**
     * Gets memory information for the current instance.
     *
     * @return memory information
     * @throws WasmException if memory info cannot be retrieved
     */
    MemoryInfo getMemoryInfo() throws WasmException;

    /**
     * Searches memory for the specified pattern.
     *
     * @param pattern the byte pattern to search for
     * @param startAddress the starting address
     * @param endAddress the ending address
     * @return list of addresses where pattern was found
     * @throws WasmException if search fails
     * @throws IllegalArgumentException if pattern is null or addresses are invalid
     */
    List<Long> searchMemory(final byte[] pattern, final long startAddress, final long endAddress) throws WasmException;

    /**
     * Evaluates a WebAssembly expression in the current context.
     *
     * @param expression the expression to evaluate
     * @return the evaluation result
     * @throws WasmException if evaluation fails
     * @throws IllegalArgumentException if expression is null
     */
    EvaluationResult evaluateExpression(final String expression) throws WasmException;

    /**
     * Registers a debug event listener.
     *
     * @param listener the event listener
     * @throws IllegalArgumentException if listener is null
     */
    void addDebugEventListener(final DebugEventListener listener);

    /**
     * Unregisters a debug event listener.
     *
     * @param listener the event listener
     * @return true if the listener was removed
     * @throws IllegalArgumentException if listener is null
     */
    boolean removeDebugEventListener(final DebugEventListener listener);

    /**
     * Waits for the next debug event.
     *
     * @return the next debug event
     * @throws InterruptedException if the wait is interrupted
     */
    DebugEvent waitForEvent() throws InterruptedException;

    /**
     * Waits for the next debug event with a timeout.
     *
     * @param timeoutMs the timeout in milliseconds
     * @return the next debug event, or null if timeout occurs
     * @throws InterruptedException if the wait is interrupted
     */
    DebugEvent waitForEvent(final long timeoutMs) throws InterruptedException;

    /**
     * Gets the current execution state.
     *
     * @return the execution state
     */
    ExecutionState getExecutionState();

    /**
     * Gets performance profiling data if available.
     *
     * @return profiling data or null if not available
     */
    ProfilingData getProfilingData();

    /**
     * Enables or disables performance profiling.
     *
     * @param enabled true to enable profiling
     * @throws WasmException if profiling state cannot be changed
     */
    void setProfilingEnabled(final boolean enabled) throws WasmException;

    /**
     * Checks if the session is still active.
     *
     * @return true if the session is active
     */
    boolean isActive();

    /**
     * Closes the debug session and releases resources.
     */
    @Override
    void close();
}