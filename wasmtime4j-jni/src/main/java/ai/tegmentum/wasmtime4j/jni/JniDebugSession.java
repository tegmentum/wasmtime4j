package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.debug.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WebAssembly debug session.
 *
 * <p>This class provides JNI bindings for individual debugging sessions, including breakpoint
 * management, execution control, and variable inspection.
 *
 * <p>All methods implement defensive programming patterns to prevent JVM crashes and ensure robust
 * operation in production environments.
 *
 * @since 1.0.0
 */
public final class JniDebugSession implements DebugSession {

  private static final Logger LOGGER = Logger.getLogger(JniDebugSession.class.getName());
  private static final AtomicLong SESSION_ID_COUNTER = new AtomicLong(1);

  private final String sessionId;
  private final long nativeHandle;
  private final JniDebugger debugger;
  private final List<Instance> instances;
  private final List<DebugEventListener> eventListeners;
  private volatile boolean closed;

  /** Creates a debug session for a single instance. */
  JniDebugSession(final JniDebugger debugger, final long debuggerHandle, final long instanceHandle)
      throws WasmException {
    this(debugger, debuggerHandle, instanceHandle, DebugConfig.defaultConfig());
  }

  /** Creates a debug session for multiple instances. */
  JniDebugSession(
      final JniDebugger debugger, final long debuggerHandle, final long[] instanceHandles)
      throws WasmException {
    this.sessionId = "debug-session-" + SESSION_ID_COUNTER.getAndIncrement();
    this.debugger = Objects.requireNonNull(debugger, "debugger cannot be null");
    this.eventListeners = new CopyOnWriteArrayList<>();
    this.closed = false;

    try {
      this.nativeHandle =
          nativeCreateMultiSession(debuggerHandle, instanceHandles, DebugConfig.defaultConfig());
      if (this.nativeHandle == 0) {
        throw new WasmException("Failed to create native debug session");
      }

      // We would need to resolve instance handles back to Instance objects
      // For now, create empty list - in real implementation this would be properly handled
      this.instances = Collections.emptyList();

      LOGGER.log(
          Level.FINE,
          "Created JNI debug session {0} for {1} instances",
          new Object[] {sessionId, instanceHandles.length});
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to create debug session");
    }
  }

  /** Creates a debug session with configuration. */
  JniDebugSession(
      final JniDebugger debugger,
      final long debuggerHandle,
      final long instanceHandle,
      final DebugConfig config)
      throws WasmException {
    this.sessionId = "debug-session-" + SESSION_ID_COUNTER.getAndIncrement();
    this.debugger = Objects.requireNonNull(debugger, "debugger cannot be null");
    this.eventListeners = new CopyOnWriteArrayList<>();
    this.closed = false;

    try {
      this.nativeHandle = nativeCreateSession(debuggerHandle, instanceHandle, config);
      if (this.nativeHandle == 0) {
        throw new WasmException("Failed to create native debug session");
      }

      // We would need to resolve instance handle back to Instance object
      // For now, create empty list - in real implementation this would be properly handled
      this.instances = Collections.emptyList();

      LOGGER.log(Level.FINE, "Created JNI debug session {0} with config", sessionId);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to create debug session");
    }
  }

  /** Creates a debug session from existing native handle (for attach scenarios). */
  JniDebugSession(final JniDebugger debugger, final long nativeHandle) {
    this.sessionId = "debug-session-" + SESSION_ID_COUNTER.getAndIncrement();
    this.debugger = Objects.requireNonNull(debugger, "debugger cannot be null");
    this.nativeHandle = nativeHandle;
    this.instances = Collections.emptyList();
    this.eventListeners = new CopyOnWriteArrayList<>();
    this.closed = false;

    LOGGER.log(Level.FINE, "Created JNI debug session {0} from existing handle", sessionId);
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public List<Instance> getInstances() {
    validateNotClosed();
    return List.copyOf(instances);
  }

  @Override
  public Breakpoint setBreakpoint(final String functionName, final int line) throws WasmException {
    Objects.requireNonNull(functionName, "functionName cannot be null");
    JniValidation.validateNonNegative(line, "line");
    validateNotClosed();

    try {
      return nativeSetBreakpointAtLine(nativeHandle, functionName, line);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(
          e, "Failed to set breakpoint at " + functionName + ":" + line);
    }
  }

  @Override
  public Breakpoint setBreakpoint(final String functionName, final long instructionOffset)
      throws WasmException {
    Objects.requireNonNull(functionName, "functionName cannot be null");
    JniValidation.validateNonNegative(instructionOffset, "instructionOffset");
    validateNotClosed();

    try {
      return nativeSetBreakpointAtOffset(nativeHandle, functionName, instructionOffset);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(
          e, "Failed to set breakpoint at " + functionName + ":" + instructionOffset);
    }
  }

  @Override
  public boolean removeBreakpoint(final Breakpoint breakpoint) throws WasmException {
    Objects.requireNonNull(breakpoint, "breakpoint cannot be null");
    validateNotClosed();

    try {
      return nativeRemoveBreakpoint(nativeHandle, breakpoint.getId());
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to remove breakpoint");
    }
  }

  @Override
  public int removeBreakpoints(final String functionName) throws WasmException {
    Objects.requireNonNull(functionName, "functionName cannot be null");
    validateNotClosed();

    try {
      return nativeRemoveBreakpointsInFunction(nativeHandle, functionName);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(
          e, "Failed to remove breakpoints in function " + functionName);
    }
  }

  @Override
  public List<Breakpoint> getBreakpoints() {
    if (closed) {
      return Collections.emptyList();
    }

    try {
      return nativeGetBreakpoints(nativeHandle);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get breakpoints", e);
      return Collections.emptyList();
    }
  }

  @Override
  public CompletableFuture<DebugEvent> continueExecution() throws WasmException {
    validateNotClosed();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return nativeContinueExecution(nativeHandle);
          } catch (final Exception e) {
            throw new RuntimeException(
                JniExceptionHandler.wrapException(e, "Failed to continue execution"));
          }
        });
  }

  @Override
  public CompletableFuture<DebugEvent> stepInto() throws WasmException {
    validateNotClosed();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return nativeStepInto(nativeHandle);
          } catch (final Exception e) {
            throw new RuntimeException(JniExceptionHandler.wrapException(e, "Failed to step into"));
          }
        });
  }

  @Override
  public CompletableFuture<DebugEvent> stepOver() throws WasmException {
    validateNotClosed();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return nativeStepOver(nativeHandle);
          } catch (final Exception e) {
            throw new RuntimeException(JniExceptionHandler.wrapException(e, "Failed to step over"));
          }
        });
  }

  @Override
  public CompletableFuture<DebugEvent> stepOut() throws WasmException {
    validateNotClosed();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return nativeStepOut(nativeHandle);
          } catch (final Exception e) {
            throw new RuntimeException(JniExceptionHandler.wrapException(e, "Failed to step out"));
          }
        });
  }

  @Override
  public CompletableFuture<DebugEvent> pause() throws WasmException {
    validateNotClosed();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return nativePauseExecution(nativeHandle);
          } catch (final Exception e) {
            throw new RuntimeException(
                JniExceptionHandler.wrapException(e, "Failed to pause execution"));
          }
        });
  }

  @Override
  public List<StackFrame> getStackTrace() throws WasmException {
    validateNotClosed();

    try {
      return nativeGetStackTrace(nativeHandle);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to get stack trace");
    }
  }

  @Override
  public List<Variable> getCurrentVariables() throws WasmException {
    validateNotClosed();

    try {
      return nativeGetCurrentVariables(nativeHandle);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to get current variables");
    }
  }

  @Override
  public List<Variable> getVariables(final int frameIndex) throws WasmException {
    JniValidation.validateNonNegative(frameIndex, "frameIndex");
    validateNotClosed();

    try {
      return nativeGetVariables(nativeHandle, frameIndex);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to get variables for frame " + frameIndex);
    }
  }

  @Override
  public VariableValue getVariableValue(final String variableName) throws WasmException {
    Objects.requireNonNull(variableName, "variableName cannot be null");
    validateNotClosed();

    try {
      return nativeGetVariableValue(nativeHandle, variableName);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(
          e, "Failed to get variable value for " + variableName);
    }
  }

  @Override
  public void setVariableValue(final String variableName, final VariableValue value)
      throws WasmException {
    Objects.requireNonNull(variableName, "variableName cannot be null");
    Objects.requireNonNull(value, "value cannot be null");
    validateNotClosed();

    try {
      nativeSetVariableValue(nativeHandle, variableName, value);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(
          e, "Failed to set variable value for " + variableName);
    }
  }

  @Override
  public byte[] readMemory(final long address, final int length) throws WasmException {
    JniValidation.validateNonNegative(address, "address");
    JniValidation.validatePositive(length, "length");
    validateNotClosed();

    try {
      return nativeReadMemory(nativeHandle, address, length);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to read memory at address " + address);
    }
  }

  @Override
  public void writeMemory(final long address, final byte[] data) throws WasmException {
    JniValidation.validateNonNegative(address, "address");
    Objects.requireNonNull(data, "data cannot be null");
    validateNotClosed();

    try {
      nativeWriteMemory(nativeHandle, address, data);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to write memory at address " + address);
    }
  }

  @Override
  public MemoryInfo getMemoryInfo() throws WasmException {
    validateNotClosed();

    try {
      return nativeGetMemoryInfo(nativeHandle);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to get memory info");
    }
  }

  @Override
  public List<Long> searchMemory(
      final byte[] pattern, final long startAddress, final long endAddress) throws WasmException {
    Objects.requireNonNull(pattern, "pattern cannot be null");
    JniValidation.validateNonNegative(startAddress, "startAddress");
    JniValidation.validateNonNegative(endAddress, "endAddress");
    if (startAddress >= endAddress) {
      throw new IllegalArgumentException("startAddress must be less than endAddress");
    }
    validateNotClosed();

    try {
      return nativeSearchMemory(nativeHandle, pattern, startAddress, endAddress);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to search memory");
    }
  }

  @Override
  public EvaluationResult evaluateExpression(final String expression) throws WasmException {
    Objects.requireNonNull(expression, "expression cannot be null");
    validateNotClosed();

    try {
      return nativeEvaluateExpression(nativeHandle, expression);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to evaluate expression: " + expression);
    }
  }

  @Override
  public void addDebugEventListener(final DebugEventListener listener) {
    Objects.requireNonNull(listener, "listener cannot be null");
    eventListeners.add(listener);
  }

  @Override
  public boolean removeDebugEventListener(final DebugEventListener listener) {
    Objects.requireNonNull(listener, "listener cannot be null");
    return eventListeners.remove(listener);
  }

  @Override
  public DebugEvent waitForEvent() throws InterruptedException {
    validateNotClosed();

    try {
      return nativeWaitForEvent(nativeHandle, -1); // -1 for infinite timeout
    } catch (final Exception e) {
      if (e instanceof InterruptedException) {
        throw (InterruptedException) e;
      }
      throw new RuntimeException(JniExceptionHandler.wrapException(e, "Failed to wait for event"));
    }
  }

  @Override
  public DebugEvent waitForEvent(final long timeoutMs) throws InterruptedException {
    JniValidation.validateNonNegative(timeoutMs, "timeoutMs");
    validateNotClosed();

    try {
      return nativeWaitForEvent(nativeHandle, timeoutMs);
    } catch (final Exception e) {
      if (e instanceof InterruptedException) {
        throw (InterruptedException) e;
      }
      throw new RuntimeException(JniExceptionHandler.wrapException(e, "Failed to wait for event"));
    }
  }

  @Override
  public ExecutionState getExecutionState() {
    if (closed) {
      return ExecutionState.error("Session closed");
    }

    try {
      return nativeGetExecutionState(nativeHandle);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get execution state", e);
      return ExecutionState.error("Failed to get execution state: " + e.getMessage());
    }
  }

  @Override
  public ProfilingData getProfilingData() {
    if (closed) {
      return ProfilingData.empty();
    }

    try {
      return nativeGetProfilingData(nativeHandle);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get profiling data", e);
      return ProfilingData.empty();
    }
  }

  @Override
  public void setProfilingEnabled(final boolean enabled) throws WasmException {
    validateNotClosed();

    try {
      nativeSetProfilingEnabled(nativeHandle, enabled);
    } catch (final Exception e) {
      throw JniExceptionHandler.wrapException(e, "Failed to set profiling enabled");
    }
  }

  @Override
  public boolean isActive() {
    return !closed && nativeHandle != 0 && nativeIsSessionActive(nativeHandle);
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      // Clear event listeners
      eventListeners.clear();

      // Close native session
      if (nativeHandle != 0) {
        nativeCloseSession(nativeHandle);
      }

      // Remove from debugger's active sessions
      debugger.removeSession(this);

      closed = true;
      LOGGER.log(Level.FINE, "Closed JNI debug session {0}", sessionId);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing debug session", e);
      closed = true; // Mark as closed even on error
    }
  }

  // Package-private helper methods

  /** Gets the native session handle. */
  long getNativeHandle() {
    return nativeHandle;
  }

  /** Notifies event listeners of a debug event. Called by native code via JNI callback. */
  void notifyEventListeners(final DebugEvent event) {
    for (final DebugEventListener listener : eventListeners) {
      try {
        listener.onDebugEvent(event);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Debug event listener failed", e);
      }
    }
  }

  // Private helper methods

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Debug session is closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Invalid native handle");
    }
  }

  // Native method declarations
  // These would be implemented in the native Rust code

  private static native long nativeCreateSession(
      long debuggerHandle, long instanceHandle, DebugConfig config);

  private static native long nativeCreateMultiSession(
      long debuggerHandle, long[] instanceHandles, DebugConfig config);

  private static native void nativeCloseSession(long sessionHandle);

  private static native boolean nativeIsSessionActive(long sessionHandle);

  private static native Breakpoint nativeSetBreakpointAtLine(
      long sessionHandle, String functionName, int line);

  private static native Breakpoint nativeSetBreakpointAtOffset(
      long sessionHandle, String functionName, long offset);

  private static native boolean nativeRemoveBreakpoint(long sessionHandle, String breakpointId);

  private static native int nativeRemoveBreakpointsInFunction(
      long sessionHandle, String functionName);

  private static native List<Breakpoint> nativeGetBreakpoints(long sessionHandle);

  private static native DebugEvent nativeContinueExecution(long sessionHandle);

  private static native DebugEvent nativeStepInto(long sessionHandle);

  private static native DebugEvent nativeStepOver(long sessionHandle);

  private static native DebugEvent nativeStepOut(long sessionHandle);

  private static native DebugEvent nativePauseExecution(long sessionHandle);

  private static native List<StackFrame> nativeGetStackTrace(long sessionHandle);

  private static native List<Variable> nativeGetCurrentVariables(long sessionHandle);

  private static native List<Variable> nativeGetVariables(long sessionHandle, int frameIndex);

  private static native VariableValue nativeGetVariableValue(
      long sessionHandle, String variableName);

  private static native void nativeSetVariableValue(
      long sessionHandle, String variableName, VariableValue value);

  private static native byte[] nativeReadMemory(long sessionHandle, long address, int length);

  private static native void nativeWriteMemory(long sessionHandle, long address, byte[] data);

  private static native MemoryInfo nativeGetMemoryInfo(long sessionHandle);

  private static native List<Long> nativeSearchMemory(
      long sessionHandle, byte[] pattern, long startAddress, long endAddress);

  private static native EvaluationResult nativeEvaluateExpression(
      long sessionHandle, String expression);

  private static native DebugEvent nativeWaitForEvent(long sessionHandle, long timeoutMs)
      throws InterruptedException;

  private static native ExecutionState nativeGetExecutionState(long sessionHandle);

  private static native ProfilingData nativeGetProfilingData(long sessionHandle);

  private static native void nativeSetProfilingEnabled(long sessionHandle, boolean enabled);
}
