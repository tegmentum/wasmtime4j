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

package ai.tegmentum.wasmtime4j.jni.debug;

import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import ai.tegmentum.wasmtime4j.debug.DebugConfig;
import ai.tegmentum.wasmtime4j.debug.DebugSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of a WebAssembly debug session.
 *
 * <p>This class manages debugging state for a WebAssembly execution, including breakpoints,
 * stepping, and execution control.
 *
 * @since 1.0.0
 */
public final class JniDebugSession implements DebugSession {

  private static final Logger LOGGER = Logger.getLogger(JniDebugSession.class.getName());

  private final String sessionId;
  private final long nativeHandle;
  private final long debuggerHandle;
  private final long[] instanceHandles;
  private final DebugConfig config;
  private final AtomicBoolean active;
  private final AtomicBoolean paused;
  private final AtomicBoolean closed;
  private final List<JniBreakpoint> breakpoints;
  private final List<JniStackFrame> callStack;
  private volatile StepType pendingStep;

  /**
   * Creates a new debug session with a native handle.
   *
   * @param nativeHandle the native session handle
   */
  public JniDebugSession(final long nativeHandle) {
    this.sessionId = "session-" + UUID.randomUUID().toString();
    this.nativeHandle = nativeHandle;
    this.debuggerHandle = 0L;
    this.instanceHandles = new long[0];
    this.config = null;
    this.active = new AtomicBoolean(false);
    this.paused = new AtomicBoolean(false);
    this.closed = new AtomicBoolean(false);
    this.breakpoints = new CopyOnWriteArrayList<>();
    this.callStack = new CopyOnWriteArrayList<>();
    this.pendingStep = null;
  }

  /**
   * Creates a new debug session with debugger and instance handles.
   *
   * @param debuggerHandle the debugger handle
   * @param instanceHandle the instance handle
   */
  public JniDebugSession(final long debuggerHandle, final long instanceHandle) {
    this.sessionId = "session-" + UUID.randomUUID().toString();
    this.nativeHandle = debuggerHandle;
    this.debuggerHandle = debuggerHandle;
    this.instanceHandles = new long[] {instanceHandle};
    this.config = null;
    this.active = new AtomicBoolean(true);
    this.paused = new AtomicBoolean(false);
    this.closed = new AtomicBoolean(false);
    this.breakpoints = new CopyOnWriteArrayList<>();
    this.callStack = new CopyOnWriteArrayList<>();
    this.pendingStep = null;
  }

  /**
   * Creates a new debug session with multiple instance handles.
   *
   * @param debuggerHandle the debugger handle
   * @param instanceHandles the instance handles
   */
  public JniDebugSession(final long debuggerHandle, final long[] instanceHandles) {
    this.sessionId = "session-" + UUID.randomUUID().toString();
    this.nativeHandle = debuggerHandle;
    this.debuggerHandle = debuggerHandle;
    this.instanceHandles = instanceHandles != null ? instanceHandles.clone() : new long[0];
    this.config = null;
    this.active = new AtomicBoolean(true);
    this.paused = new AtomicBoolean(false);
    this.closed = new AtomicBoolean(false);
    this.breakpoints = new CopyOnWriteArrayList<>();
    this.callStack = new CopyOnWriteArrayList<>();
    this.pendingStep = null;
  }

  /**
   * Creates a new debug session with config.
   *
   * @param debuggerHandle the debugger handle
   * @param instanceHandle the instance handle
   * @param config the debug configuration
   */
  public JniDebugSession(
      final long debuggerHandle, final long instanceHandle, final DebugConfig config) {
    this.sessionId = "session-" + UUID.randomUUID().toString();
    this.nativeHandle = debuggerHandle;
    this.debuggerHandle = debuggerHandle;
    this.instanceHandles = new long[] {instanceHandle};
    this.config = config;
    this.active = new AtomicBoolean(true);
    this.paused = new AtomicBoolean(false);
    this.closed = new AtomicBoolean(false);
    this.breakpoints = new CopyOnWriteArrayList<>();
    this.callStack = new CopyOnWriteArrayList<>();
    this.pendingStep = null;
  }

  /**
   * Creates a new debug session without a native handle (for testing).
   *
   * @return a new debug session
   */
  public static JniDebugSession createLocal() {
    return new JniDebugSession(0L);
  }

  @Override
  public void start() {
    if (active.compareAndSet(false, true)) {
      LOGGER.log(Level.FINE, "Starting debug session: {0}", sessionId);
      if (nativeHandle != 0) {
        nativeStart(nativeHandle);
      }
    } else {
      LOGGER.log(Level.WARNING, "Debug session already active: {0}", sessionId);
    }
  }

  @Override
  public void stop() {
    if (active.compareAndSet(true, false)) {
      LOGGER.log(Level.FINE, "Stopping debug session: {0}", sessionId);
      paused.set(false);
      pendingStep = null;
      if (nativeHandle != 0) {
        nativeStop(nativeHandle);
      }
    }
  }

  @Override
  public void step(final StepType stepType) {
    Objects.requireNonNull(stepType, "stepType cannot be null");
    if (!active.get()) {
      throw new IllegalStateException("Debug session is not active");
    }
    LOGGER.log(Level.FINE, "Stepping: {0} in session {1}", new Object[] {stepType, sessionId});
    pendingStep = stepType;
    if (nativeHandle != 0) {
      nativeStep(nativeHandle, stepType.ordinal());
    }
    paused.set(false);
  }

  @Override
  public void continueExecution() {
    if (!active.get()) {
      throw new IllegalStateException("Debug session is not active");
    }
    LOGGER.log(Level.FINE, "Continuing execution in session: {0}", sessionId);
    pendingStep = null;
    if (nativeHandle != 0) {
      nativeContinue(nativeHandle);
    }
    paused.set(false);
  }

  @Override
  public void addBreakpoint(final Breakpoint breakpoint) {
    Objects.requireNonNull(breakpoint, "breakpoint cannot be null");
    if (breakpoint instanceof JniBreakpoint) {
      final JniBreakpoint jniBreakpoint = (JniBreakpoint) breakpoint;
      breakpoints.add(jniBreakpoint);
      LOGGER.log(
          Level.FINE,
          "Added breakpoint {0} to session {1}",
          new Object[] {breakpoint.getBreakpointId(), sessionId});
      if (nativeHandle != 0 && active.get()) {
        nativeAddBreakpoint(
            nativeHandle,
            jniBreakpoint.getFunctionName(),
            jniBreakpoint.getLineNumber(),
            jniBreakpoint.getColumnNumber(),
            jniBreakpoint.getInstructionOffset());
      }
    } else {
      throw new IllegalArgumentException("Breakpoint must be a JniBreakpoint");
    }
  }

  @Override
  public void removeBreakpoint(final Breakpoint breakpoint) {
    Objects.requireNonNull(breakpoint, "breakpoint cannot be null");
    if (breakpoints.removeIf(bp -> bp.getBreakpointId().equals(breakpoint.getBreakpointId()))) {
      LOGGER.log(
          Level.FINE,
          "Removed breakpoint {0} from session {1}",
          new Object[] {breakpoint.getBreakpointId(), sessionId});
      if (nativeHandle != 0 && active.get()) {
        nativeRemoveBreakpoint(nativeHandle, breakpoint.getBreakpointId());
      }
    }
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public boolean isActive() {
    return active.get();
  }

  /**
   * Checks if the session is currently paused at a breakpoint or step.
   *
   * @return true if paused
   */
  public boolean isPaused() {
    return paused.get();
  }

  /**
   * Gets the native session handle.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Gets all breakpoints in this session.
   *
   * @return unmodifiable list of breakpoints
   */
  public List<JniBreakpoint> getBreakpoints() {
    return Collections.unmodifiableList(new ArrayList<>(breakpoints));
  }

  /**
   * Gets the current call stack.
   *
   * @return unmodifiable list of stack frames
   */
  public List<JniStackFrame> getCallStack() {
    return Collections.unmodifiableList(new ArrayList<>(callStack));
  }

  /**
   * Gets the pending step type.
   *
   * @return the pending step type, or null if not stepping
   */
  public StepType getPendingStep() {
    return pendingStep;
  }

  /**
   * Pauses execution at the current location. Called from native code when a breakpoint is hit.
   *
   * @param breakpointId the breakpoint ID that was hit
   */
  public void onBreakpointHit(final String breakpointId) {
    paused.set(true);
    for (final JniBreakpoint bp : breakpoints) {
      if (bp.getBreakpointId().equals(breakpointId)) {
        bp.incrementHitCount();
        break;
      }
    }
    LOGGER.log(
        Level.FINE, "Breakpoint {0} hit in session {1}", new Object[] {breakpointId, sessionId});
  }

  /**
   * Updates the call stack. Called from native code during debugging.
   *
   * @param frames the new stack frames
   */
  public void updateCallStack(final List<JniStackFrame> frames) {
    callStack.clear();
    if (frames != null) {
      callStack.addAll(frames);
    }
  }

  /**
   * Pauses execution after a step completes.
   *
   * @param stepType the step type that completed
   */
  public void onStepComplete(final StepType stepType) {
    paused.set(true);
    pendingStep = null;
    LOGGER.log(Level.FINE, "Step {0} complete in session {1}", new Object[] {stepType, sessionId});
  }

  /**
   * Gets the debug configuration.
   *
   * @return the configuration, or null if not set
   */
  public DebugConfig getConfig() {
    return config;
  }

  /**
   * Gets the debugger handle.
   *
   * @return the debugger handle
   */
  public long getDebuggerHandle() {
    return debuggerHandle;
  }

  /**
   * Gets the instance handles.
   *
   * @return copy of the instance handles array
   */
  public long[] getInstanceHandles() {
    return instanceHandles.clone();
  }

  /**
   * Checks if the session is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed.get();
  }

  /** Closes the debug session and releases resources. */
  public void close() {
    if (closed.compareAndSet(false, true)) {
      active.set(false);
      paused.set(false);
      breakpoints.clear();
      callStack.clear();
      pendingStep = null;

      if (nativeHandle != 0) {
        nativeStop(nativeHandle);
      }

      LOGGER.log(Level.FINE, "Closed debug session: {0}", sessionId);
    }
  }

  @Override
  public String toString() {
    return "JniDebugSession{"
        + "sessionId='"
        + sessionId
        + '\''
        + ", active="
        + active.get()
        + ", paused="
        + paused.get()
        + ", breakpoints="
        + breakpoints.size()
        + ", stackDepth="
        + callStack.size()
        + '}';
  }

  // Native methods
  private static native void nativeStart(long handle);

  private static native void nativeStop(long handle);

  private static native void nativeStep(long handle, int stepType);

  private static native void nativeContinue(long handle);

  private static native void nativeAddBreakpoint(
      long handle, String functionName, int lineNumber, int columnNumber, long instructionOffset);

  private static native void nativeRemoveBreakpoint(long handle, String breakpointId);
}
