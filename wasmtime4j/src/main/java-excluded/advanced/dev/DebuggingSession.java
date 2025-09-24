package ai.tegmentum.wasmtime4j.dev;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Interactive debugging session for WebAssembly execution with breakpoints, stepping, and memory
 * inspection capabilities.
 */
public final class DebuggingSession implements AutoCloseable {

  private final Instance instance;
  private final Module module;
  private final Map<Integer, Breakpoint> breakpoints;
  private final Map<String, WatchExpression> watchExpressions;
  private final List<DebugEventListener> listeners;
  private final long debugHandle;
  private volatile DebugState state;
  private volatile ExecutionContext currentContext;

  /**
   * Creates a debugging session for the given instance.
   *
   * @param instance The WebAssembly instance to debug
   * @throws IllegalArgumentException if instance is null
   */
  public DebuggingSession(final Instance instance) {
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    this.instance = instance;
    this.module = getModuleFromInstance(instance);
    this.breakpoints = new ConcurrentHashMap<>();
    this.watchExpressions = new ConcurrentHashMap<>();
    this.listeners = new ArrayList<>();
    this.debugHandle = initializeDebugSession(instance);
    this.state = DebugState.READY;
    this.currentContext = null;
  }

  /**
   * Sets a breakpoint at the specified instruction offset.
   *
   * @param functionIndex The function index
   * @param instructionOffset The instruction offset within the function
   * @return The created breakpoint
   */
  public Breakpoint setBreakpoint(final int functionIndex, final int instructionOffset) {
    return setBreakpoint(functionIndex, instructionOffset, null);
  }

  /**
   * Sets a conditional breakpoint at the specified instruction offset.
   *
   * @param functionIndex The function index
   * @param instructionOffset The instruction offset within the function
   * @param condition The breakpoint condition (null for unconditional)
   * @return The created breakpoint
   */
  public Breakpoint setBreakpoint(
      final int functionIndex, final int instructionOffset, final String condition) {
    final int breakpointId = generateBreakpointId();
    final Breakpoint breakpoint =
        new Breakpoint(breakpointId, functionIndex, instructionOffset, condition, true);

    breakpoints.put(breakpointId, breakpoint);
    setNativeBreakpoint(debugHandle, breakpointId, functionIndex, instructionOffset, condition);

    notifyListeners(listener -> listener.onBreakpointSet(breakpoint));
    return breakpoint;
  }

  /**
   * Removes a breakpoint.
   *
   * @param breakpointId The breakpoint ID to remove
   * @return true if the breakpoint was removed, false if not found
   */
  public boolean removeBreakpoint(final int breakpointId) {
    final Breakpoint removed = breakpoints.remove(breakpointId);
    if (removed != null) {
      removeNativeBreakpoint(debugHandle, breakpointId);
      notifyListeners(listener -> listener.onBreakpointRemoved(removed));
      return true;
    }
    return false;
  }

  /**
   * Enables or disables a breakpoint.
   *
   * @param breakpointId The breakpoint ID
   * @param enabled true to enable, false to disable
   */
  public void setBreakpointEnabled(final int breakpointId, final boolean enabled) {
    final Breakpoint breakpoint = breakpoints.get(breakpointId);
    if (breakpoint != null) {
      final Breakpoint updated = breakpoint.withEnabled(enabled);
      breakpoints.put(breakpointId, updated);
      setNativeBreakpointEnabled(debugHandle, breakpointId, enabled);
      notifyListeners(listener -> listener.onBreakpointChanged(updated));
    }
  }

  /**
   * Adds a watch expression to monitor variable values.
   *
   * @param name The name of the watch expression
   * @param expression The expression to evaluate
   * @return The created watch expression
   */
  public WatchExpression addWatchExpression(final String name, final String expression) {
    final WatchExpression watch = new WatchExpression(name, expression, true);
    watchExpressions.put(name, watch);
    addNativeWatchExpression(debugHandle, name, expression);
    notifyListeners(listener -> listener.onWatchExpressionAdded(watch));
    return watch;
  }

  /**
   * Removes a watch expression.
   *
   * @param name The name of the watch expression to remove
   * @return true if removed, false if not found
   */
  public boolean removeWatchExpression(final String name) {
    final WatchExpression removed = watchExpressions.remove(name);
    if (removed != null) {
      removeNativeWatchExpression(debugHandle, name);
      notifyListeners(listener -> listener.onWatchExpressionRemoved(removed));
      return true;
    }
    return false;
  }

  /**
   * Starts debugging execution of the specified function.
   *
   * @param functionName The function name to debug
   * @param arguments The function arguments
   * @return Completion future for the debug execution
   */
  public CompletableFuture<DebugResult> debugFunction(
      final String functionName, final WasmValue... arguments) {
    if (state != DebugState.READY) {
      return CompletableFuture.completedFuture(
          DebugResult.failed("Debug session not ready: " + state));
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            state = DebugState.RUNNING;
            notifyListeners(listener -> listener.onExecutionStarted(functionName));

            final DebugResult result = executeWithDebugging(functionName, arguments);

            state = DebugState.READY;
            notifyListeners(listener -> listener.onExecutionFinished(result));

            return result;
          } catch (final Exception e) {
            state = DebugState.ERROR;
            final DebugResult result = DebugResult.failed("Execution failed: " + e.getMessage());
            notifyListeners(listener -> listener.onExecutionFinished(result));
            return result;
          }
        });
  }

  /**
   * Steps to the next instruction.
   *
   * @return The execution context after stepping
   */
  public ExecutionContext stepNext() {
    if (state != DebugState.PAUSED) {
      throw new IllegalStateException("Cannot step when not paused");
    }

    currentContext = stepNextNative(debugHandle);
    notifyListeners(listener -> listener.onStep(currentContext));
    return currentContext;
  }

  /**
   * Steps into a function call.
   *
   * @return The execution context after stepping
   */
  public ExecutionContext stepInto() {
    if (state != DebugState.PAUSED) {
      throw new IllegalStateException("Cannot step when not paused");
    }

    currentContext = stepIntoNative(debugHandle);
    notifyListeners(listener -> listener.onStep(currentContext));
    return currentContext;
  }

  /**
   * Steps out of the current function.
   *
   * @return The execution context after stepping
   */
  public ExecutionContext stepOut() {
    if (state != DebugState.PAUSED) {
      throw new IllegalStateException("Cannot step when not paused");
    }

    currentContext = stepOutNative(debugHandle);
    notifyListeners(listener -> listener.onStep(currentContext));
    return currentContext;
  }

  /** Continues execution from a paused state. */
  public void continueExecution() {
    if (state != DebugState.PAUSED) {
      throw new IllegalStateException("Cannot continue when not paused");
    }

    state = DebugState.RUNNING;
    continueNative(debugHandle);
    notifyListeners(listener -> listener.onContinue());
  }

  /** Pauses execution if currently running. */
  public void pauseExecution() {
    if (state != DebugState.RUNNING) {
      throw new IllegalStateException("Cannot pause when not running");
    }

    state = DebugState.PAUSED;
    pauseNative(debugHandle);
    currentContext = getCurrentExecutionContext();
    notifyListeners(listener -> listener.onPause(currentContext));
  }

  /**
   * Gets the current execution context.
   *
   * @return The current execution context, or null if not debugging
   */
  public ExecutionContext getCurrentContext() {
    return currentContext;
  }

  /**
   * Gets the current call stack.
   *
   * @return The call stack frames
   */
  public List<StackFrame> getCallStack() {
    if (state != DebugState.PAUSED) {
      return Collections.emptyList();
    }
    return getCallStackNative(debugHandle);
  }

  /**
   * Inspects memory at the specified address.
   *
   * @param address The memory address
   * @param length The number of bytes to read
   * @return Memory inspection result
   */
  public MemoryInspection inspectMemory(final long address, final int length) {
    final byte[] data = readMemoryNative(debugHandle, address, length);
    return new MemoryInspection(address, data);
  }

  /**
   * Gets the current variable values in scope.
   *
   * @return Map of variable names to values
   */
  public Map<String, WasmValue> getVariables() {
    if (state != DebugState.PAUSED) {
      return Collections.emptyMap();
    }
    return getVariablesNative(debugHandle);
  }

  /**
   * Evaluates a watch expression in the current context.
   *
   * @param expression The expression to evaluate
   * @return The evaluation result
   */
  public WatchEvaluationResult evaluateWatch(final String expression) {
    if (state != DebugState.PAUSED) {
      return WatchEvaluationResult.failed("Not paused");
    }
    return evaluateWatchNative(debugHandle, expression);
  }

  /**
   * Gets all active breakpoints.
   *
   * @return List of active breakpoints
   */
  public List<Breakpoint> getBreakpoints() {
    return new ArrayList<>(breakpoints.values());
  }

  /**
   * Gets all watch expressions.
   *
   * @return List of watch expressions
   */
  public List<WatchExpression> getWatchExpressions() {
    return new ArrayList<>(watchExpressions.values());
  }

  /**
   * Gets the current debug state.
   *
   * @return The current debug state
   */
  public DebugState getState() {
    return state;
  }

  /**
   * Adds a debug event listener.
   *
   * @param listener The listener to add
   */
  public void addDebugEventListener(final DebugEventListener listener) {
    if (listener != null) {
      synchronized (listeners) {
        listeners.add(listener);
      }
    }
  }

  /**
   * Removes a debug event listener.
   *
   * @param listener The listener to remove
   */
  public void removeDebugEventListener(final DebugEventListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  @Override
  public void close() {
    if (state == DebugState.RUNNING || state == DebugState.PAUSED) {
      try {
        stopDebugging();
      } catch (final Exception e) {
        // Log but continue cleanup
      }
    }

    breakpoints.clear();
    watchExpressions.clear();
    cleanupDebugSession(debugHandle);
  }

  /** Stops the current debugging session. */
  public void stopDebugging() {
    if (state == DebugState.RUNNING || state == DebugState.PAUSED) {
      stopNative(debugHandle);
      state = DebugState.READY;
      currentContext = null;
      notifyListeners(listener -> listener.onExecutionStopped());
    }
  }

  private void notifyListeners(final Consumer<DebugEventListener> action) {
    final List<DebugEventListener> currentListeners;
    synchronized (listeners) {
      currentListeners = new ArrayList<>(listeners);
    }

    for (final DebugEventListener listener : currentListeners) {
      try {
        action.accept(listener);
      } catch (final Exception e) {
        // Log but continue with other listeners
      }
    }
  }

  private int generateBreakpointId() {
    return breakpoints.size() + 1; // Simple ID generation
  }

  private native long initializeDebugSession(Instance instance);

  private native Module getModuleFromInstance(Instance instance);

  private native void setNativeBreakpoint(
      long handle, int id, int functionIndex, int instructionOffset, String condition);

  private native void removeNativeBreakpoint(long handle, int id);

  private native void setNativeBreakpointEnabled(long handle, int id, boolean enabled);

  private native void addNativeWatchExpression(long handle, String name, String expression);

  private native void removeNativeWatchExpression(long handle, String name);

  private native DebugResult executeWithDebugging(String functionName, WasmValue[] arguments);

  private native ExecutionContext stepNextNative(long handle);

  private native ExecutionContext stepIntoNative(long handle);

  private native ExecutionContext stepOutNative(long handle);

  private native void continueNative(long handle);

  private native void pauseNative(long handle);

  private native void stopNative(long handle);

  private native ExecutionContext getCurrentExecutionContext();

  private native List<StackFrame> getCallStackNative(long handle);

  private native byte[] readMemoryNative(long handle, long address, int length);

  private native Map<String, WasmValue> getVariablesNative(long handle);

  private native WatchEvaluationResult evaluateWatchNative(long handle, String expression);

  private native void cleanupDebugSession(long handle);

  /** Breakpoint information. */
  public static final class Breakpoint {
    private final int id;
    private final int functionIndex;
    private final int instructionOffset;
    private final String condition;
    private final boolean enabled;

    public Breakpoint(
        final int id,
        final int functionIndex,
        final int instructionOffset,
        final String condition,
        final boolean enabled) {
      this.id = id;
      this.functionIndex = functionIndex;
      this.instructionOffset = instructionOffset;
      this.condition = condition;
      this.enabled = enabled;
    }

    public Breakpoint withEnabled(final boolean enabled) {
      return new Breakpoint(id, functionIndex, instructionOffset, condition, enabled);
    }

    public int getId() {
      return id;
    }

    public int getFunctionIndex() {
      return functionIndex;
    }

    public int getInstructionOffset() {
      return instructionOffset;
    }

    public String getCondition() {
      return condition;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public boolean isConditional() {
      return condition != null && !condition.trim().isEmpty();
    }
  }

  /** Watch expression information. */
  public static final class WatchExpression {
    private final String name;
    private final String expression;
    private final boolean enabled;

    public WatchExpression(final String name, final String expression, final boolean enabled) {
      this.name = name;
      this.expression = expression;
      this.enabled = enabled;
    }

    public String getName() {
      return name;
    }

    public String getExpression() {
      return expression;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

  /** Execution context information. */
  public static final class ExecutionContext {
    private final int functionIndex;
    private final int instructionOffset;
    private final String functionName;
    private final Map<String, WasmValue> locals;
    private final List<WasmValue> stack;

    public ExecutionContext(
        final int functionIndex,
        final int instructionOffset,
        final String functionName,
        final Map<String, WasmValue> locals,
        final List<WasmValue> stack) {
      this.functionIndex = functionIndex;
      this.instructionOffset = instructionOffset;
      this.functionName = functionName;
      this.locals = Collections.unmodifiableMap(new HashMap<>(locals));
      this.stack = Collections.unmodifiableList(new ArrayList<>(stack));
    }

    public int getFunctionIndex() {
      return functionIndex;
    }

    public int getInstructionOffset() {
      return instructionOffset;
    }

    public String getFunctionName() {
      return functionName;
    }

    public Map<String, WasmValue> getLocals() {
      return locals;
    }

    public List<WasmValue> getStack() {
      return stack;
    }
  }

  /** Stack frame information. */
  public static final class StackFrame {
    private final int functionIndex;
    private final String functionName;
    private final int instructionOffset;
    private final Map<String, WasmValue> variables;

    public StackFrame(
        final int functionIndex,
        final String functionName,
        final int instructionOffset,
        final Map<String, WasmValue> variables) {
      this.functionIndex = functionIndex;
      this.functionName = functionName;
      this.instructionOffset = instructionOffset;
      this.variables = Collections.unmodifiableMap(new HashMap<>(variables));
    }

    public int getFunctionIndex() {
      return functionIndex;
    }

    public String getFunctionName() {
      return functionName;
    }

    public int getInstructionOffset() {
      return instructionOffset;
    }

    public Map<String, WasmValue> getVariables() {
      return variables;
    }
  }

  /** Memory inspection result. */
  public static final class MemoryInspection {
    private final long address;
    private final byte[] data;

    public MemoryInspection(final long address, final byte[] data) {
      this.address = address;
      this.data = data.clone();
    }

    public long getAddress() {
      return address;
    }

    public byte[] getData() {
      return data.clone();
    }

    public int getLength() {
      return data.length;
    }

    /**
     * Gets data as hexadecimal string.
     *
     * @return Hex representation of the data
     */
    public String getHexString() {
      final StringBuilder sb = new StringBuilder();
      for (final byte b : data) {
        sb.append(String.format("%02X ", b & 0xFF));
      }
      return sb.toString().trim();
    }
  }

  /** Watch evaluation result. */
  public static final class WatchEvaluationResult {
    private final boolean successful;
    private final WasmValue value;
    private final String error;

    private WatchEvaluationResult(
        final boolean successful, final WasmValue value, final String error) {
      this.successful = successful;
      this.value = value;
      this.error = error;
    }

    public static WatchEvaluationResult successful(final WasmValue value) {
      return new WatchEvaluationResult(true, value, null);
    }

    public static WatchEvaluationResult failed(final String error) {
      return new WatchEvaluationResult(false, null, error);
    }

    public boolean isSuccessful() {
      return successful;
    }

    public WasmValue getValue() {
      return value;
    }

    public String getError() {
      return error;
    }
  }

  /** Debug execution result. */
  public static final class DebugResult {
    private final boolean successful;
    private final WasmValue[] results;
    private final String error;
    private final long executionTimeNs;

    private DebugResult(
        final boolean successful,
        final WasmValue[] results,
        final String error,
        final long executionTimeNs) {
      this.successful = successful;
      this.results = results;
      this.error = error;
      this.executionTimeNs = executionTimeNs;
    }

    public static DebugResult successful(final WasmValue[] results, final long executionTimeNs) {
      return new DebugResult(true, results, null, executionTimeNs);
    }

    public static DebugResult failed(final String error) {
      return new DebugResult(false, null, error, 0);
    }

    public boolean isSuccessful() {
      return successful;
    }

    public WasmValue[] getResults() {
      return results;
    }

    public String getError() {
      return error;
    }

    public long getExecutionTimeNs() {
      return executionTimeNs;
    }
  }

  /** Debug state enumeration. */
  public enum DebugState {
    READY, // Ready to start debugging
    RUNNING, // Currently executing
    PAUSED, // Paused at breakpoint or step
    ERROR // Error state
  }

  /** Debug event listener interface. */
  public interface DebugEventListener {
    /**
     * Called when execution starts.
     *
     * @param functionName The function being executed
     */
    default void onExecutionStarted(final String functionName) {}

    /**
     * Called when execution finishes.
     *
     * @param result The execution result
     */
    default void onExecutionFinished(final DebugResult result) {}

    /** Called when execution is stopped. */
    default void onExecutionStopped() {}

    /**
     * Called when execution is paused.
     *
     * @param context The execution context
     */
    default void onPause(final ExecutionContext context) {}

    /** Called when execution continues. */
    default void onContinue() {}

    /**
     * Called when a step is performed.
     *
     * @param context The execution context after stepping
     */
    default void onStep(final ExecutionContext context) {}

    /**
     * Called when a breakpoint is set.
     *
     * @param breakpoint The breakpoint that was set
     */
    default void onBreakpointSet(final Breakpoint breakpoint) {}

    /**
     * Called when a breakpoint is removed.
     *
     * @param breakpoint The breakpoint that was removed
     */
    default void onBreakpointRemoved(final Breakpoint breakpoint) {}

    /**
     * Called when a breakpoint is changed.
     *
     * @param breakpoint The breakpoint that was changed
     */
    default void onBreakpointChanged(final Breakpoint breakpoint) {}

    /**
     * Called when a breakpoint is hit.
     *
     * @param breakpoint The breakpoint that was hit
     * @param context The execution context
     */
    default void onBreakpointHit(final Breakpoint breakpoint, final ExecutionContext context) {}

    /**
     * Called when a watch expression is added.
     *
     * @param watchExpression The watch expression that was added
     */
    default void onWatchExpressionAdded(final WatchExpression watchExpression) {}

    /**
     * Called when a watch expression is removed.
     *
     * @param watchExpression The watch expression that was removed
     */
    default void onWatchExpressionRemoved(final WatchExpression watchExpression) {}
  }
}
