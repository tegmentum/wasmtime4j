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

package ai.tegmentum.wasmtime4j.panama.debug;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.debug.DebugCapabilities;
import ai.tegmentum.wasmtime4j.debug.DebugConfig;
import ai.tegmentum.wasmtime4j.debug.DebugEventListener;
import ai.tegmentum.wasmtime4j.debug.DebugInfo;
import ai.tegmentum.wasmtime4j.debug.DebugOptions;
import ai.tegmentum.wasmtime4j.debug.DebugSession;
import ai.tegmentum.wasmtime4j.debug.Debugger;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaEngine;
import ai.tegmentum.wasmtime4j.panama.PanamaInstance;
import ai.tegmentum.wasmtime4j.panama.PanamaModule;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly debugger.
 *
 * <p>This class provides Panama FFI bindings for WebAssembly debugging functionality, including
 * session management, breakpoint control, and execution monitoring.
 *
 * <p>All methods implement defensive programming patterns to prevent JVM crashes and ensure robust
 * operation in production environments.
 *
 * @since 1.0.0
 */
public final class PanamaDebugger implements Debugger {

  private static final Logger LOGGER = Logger.getLogger(PanamaDebugger.class.getName());

  private final MemorySegment nativeHandle;
  private final Engine engine;
  private final Arena arena;
  private final List<PanamaDebugSession> activeSessions;
  private final Map<String, DwarfDebugInfo> dwarfInfoCache;
  private final Map<Long, SourceMapIntegration> sourceMapCache;
  private final Map<Long, PanamaExecutionTracer> executionTracers;
  private final Map<String, Instance> instanceRegistry;
  private volatile boolean closed;
  private volatile boolean dwarfEnabled;
  private volatile boolean profilingEnabled;
  private volatile DebugEventListener eventListener;

  /**
   * Creates a Panama debugger instance.
   *
   * @param engine the engine to debug
   * @throws WasmException if debugger creation fails
   * @throws IllegalArgumentException if engine is null
   */
  public PanamaDebugger(final Engine engine) throws WasmException {
    this(engine, Arena.ofAuto());
  }

  /**
   * Creates a Panama debugger instance with a specific arena.
   *
   * @param engine the engine to debug
   * @param arena the arena for memory management
   * @throws WasmException if debugger creation fails
   * @throws IllegalArgumentException if engine is null
   */
  public PanamaDebugger(final Engine engine, final Arena arena) throws WasmException {
    this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    this.arena = Objects.requireNonNull(arena, "arena cannot be null");
    this.activeSessions = new CopyOnWriteArrayList<>();
    this.instanceRegistry = new ConcurrentHashMap<>();
    this.dwarfInfoCache = new ConcurrentHashMap<>();
    this.sourceMapCache = new ConcurrentHashMap<>();
    this.executionTracers = new ConcurrentHashMap<>();
    this.closed = false;
    this.dwarfEnabled = true;
    this.profilingEnabled = false;

    try {
      final MemorySegment engineHandle = extractEngineHandle(engine);
      this.nativeHandle = nativeCreateDebugger(engineHandle);

      if (this.nativeHandle == null || this.nativeHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create native debugger");
      }

      LOGGER.log(Level.FINE, "Created Panama debugger with handle: {0}", this.nativeHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to create debugger", e);
    }
  }

  /**
   * Gets the engine associated with this debugger.
   *
   * @return the engine
   */
  public Engine getEngine() {
    validateNotClosed();
    return engine;
  }

  @Override
  public DebugSession createSession(final DebugConfig config) {
    Objects.requireNonNull(config, "config cannot be null");
    validateNotClosed();

    final PanamaDebugSession session =
        new PanamaDebugSession(nativeHandle, MemorySegment.NULL, config, arena);
    activeSessions.add(session);

    LOGGER.log(Level.FINE, "Created config-based debug session: {0}", session.getSessionId());
    return session;
  }

  /**
   * Creates a debug session for an instance.
   *
   * @param instance the instance to debug
   * @return the debug session
   * @throws WasmException if session creation fails
   */
  public PanamaDebugSession createSession(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final PanamaDebugSession session =
          new PanamaDebugSession(nativeHandle, instanceHandle, arena);
      activeSessions.add(session);

      LOGGER.log(Level.FINE, "Created debug session: {0}", session.getSessionId());
      return session;
    } catch (final Exception e) {
      throw new WasmException("Failed to create debug session", e);
    }
  }

  /**
   * Creates a debug session for multiple instances.
   *
   * @param instances the instances to debug
   * @return the debug session
   * @throws WasmException if session creation fails
   */
  public PanamaDebugSession createSession(final List<Instance> instances) throws WasmException {
    Objects.requireNonNull(instances, "instances cannot be null");
    if (instances.isEmpty()) {
      throw new IllegalArgumentException("instances cannot be empty");
    }
    validateNotClosed();

    try {
      final MemorySegment[] instanceHandles =
          instances.stream().map(this::extractInstanceHandle).toArray(MemorySegment[]::new);

      final PanamaDebugSession session =
          new PanamaDebugSession(nativeHandle, instanceHandles, arena);
      activeSessions.add(session);

      LOGGER.log(Level.FINE, "Created multi-instance debug session: {0}", session.getSessionId());
      return session;
    } catch (final Exception e) {
      throw new WasmException("Failed to create multi-instance debug session", e);
    }
  }

  /**
   * Creates a debug session for an instance with configuration.
   *
   * @param instance the instance to debug
   * @param config the debug configuration
   * @return the debug session
   * @throws WasmException if session creation fails
   */
  public PanamaDebugSession createSession(final Instance instance, final DebugConfig config)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    Objects.requireNonNull(config, "config cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final PanamaDebugSession session =
          new PanamaDebugSession(nativeHandle, instanceHandle, config, arena);
      activeSessions.add(session);

      LOGGER.log(Level.FINE, "Created configured debug session: {0}", session.getSessionId());
      return session;
    } catch (final Exception e) {
      throw new WasmException("Failed to create configured debug session", e);
    }
  }

  /**
   * Gets all active debug sessions.
   *
   * @return list of active sessions
   */
  public List<PanamaDebugSession> getActiveSessions() {
    validateNotClosed();
    return new ArrayList<>(activeSessions);
  }

  @Override
  public DebugSession getCurrentSession() {
    validateNotClosed();
    if (activeSessions.isEmpty()) {
      return null;
    }
    return activeSessions.get(activeSessions.size() - 1);
  }

  @Override
  public String getDebuggerName() {
    return "PanamaDebugger";
  }

  @Override
  public boolean isEnabled() {
    return !closed;
  }

  @Override
  public void setEventListener(final DebugEventListener listener) {
    this.eventListener = listener;
  }

  @Override
  public void detach() {
    validateNotClosed();
    closeAllSessions();
  }

  /**
   * Detaches from a specific instance.
   *
   * @param instance the instance to detach from
   * @return true if detached successfully
   * @throws WasmException if detach fails
   */
  public boolean detach(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeDetachFromInstance(nativeHandle, instanceHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to detach from instance", e);
    }
  }

  /**
   * Closes a specific debug session.
   *
   * @param session the session to close
   * @return true if closed successfully
   */
  public boolean closeSession(final PanamaDebugSession session) {
    Objects.requireNonNull(session, "session cannot be null");
    validateNotClosed();

    try {
      if (activeSessions.remove(session)) {
        session.close();
        LOGGER.log(Level.FINE, "Closed debug session: {0}", session.getSessionId());
        return true;
      }
      return false;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to close debug session", e);
      return false;
    }
  }

  /**
   * Closes all active debug sessions.
   *
   * @return number of sessions closed
   */
  public int closeAllSessions() {
    validateNotClosed();

    int closedCount = 0;
    for (final PanamaDebugSession session : new ArrayList<>(activeSessions)) {
      if (closeSession(session)) {
        closedCount++;
      }
    }

    LOGGER.log(Level.FINE, "Closed {0} debug sessions", closedCount);
    return closedCount;
  }

  /**
   * Gets the debugging capabilities.
   *
   * @return the debug capabilities
   */
  public DebugCapabilities getCapabilities() {
    validateNotClosed();

    try {
      final DebugCapabilities nativeCapabilities = nativeGetCapabilities(nativeHandle);
      if (nativeCapabilities != null) {
        return nativeCapabilities;
      }
      return PanamaDebugCapabilities.getDefault();
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get debug capabilities, returning defaults", e);
      return PanamaDebugCapabilities.getDefault();
    }
  }

  @Override
  public DebugSession attach(final String instanceId) {
    Objects.requireNonNull(instanceId, "instanceId cannot be null");
    validateNotClosed();

    final Instance instance = instanceRegistry.get(instanceId);
    if (instance == null) {
      throw new IllegalArgumentException("Unknown instance ID: " + instanceId);
    }

    try {
      return attach(instance);
    } catch (final WasmException e) {
      throw new IllegalStateException("Failed to attach to instance: " + instanceId, e);
    }
  }

  /**
   * Attaches to an instance for debugging.
   *
   * @param instance the instance to attach to
   * @return the debug session
   * @throws WasmException if attachment fails
   */
  public PanamaDebugSession attach(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final MemorySegment sessionHandle = nativeAttachToInstance(nativeHandle, instanceHandle);

      if (sessionHandle == null || sessionHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to attach to instance");
      }

      final PanamaDebugSession session = new PanamaDebugSession(sessionHandle, arena);
      activeSessions.add(session);

      LOGGER.log(Level.FINE, "Attached to instance, created session: {0}", session.getSessionId());
      return session;
    } catch (final Exception e) {
      throw new WasmException("Failed to attach to instance", e);
    }
  }

  /**
   * Registers an instance for ID-based debugging attachment.
   *
   * @param instanceId the unique identifier for the instance
   * @param instance the instance to register
   * @throws IllegalArgumentException if instanceId or instance is null
   */
  public void registerInstance(final String instanceId, final Instance instance) {
    Objects.requireNonNull(instanceId, "instanceId cannot be null");
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    instanceRegistry.put(instanceId, instance);
    LOGGER.log(Level.FINE, "Registered instance for debugging: {0}", instanceId);
  }

  /**
   * Unregisters an instance from debugging.
   *
   * @param instanceId the instance ID to unregister
   * @return true if the instance was registered, false otherwise
   */
  public boolean unregisterInstance(final String instanceId) {
    Objects.requireNonNull(instanceId, "instanceId cannot be null");
    if (closed) {
      return false;
    }

    final boolean removed = instanceRegistry.remove(instanceId) != null;
    if (removed) {
      LOGGER.log(Level.FINE, "Unregistered instance from debugging: {0}", instanceId);
    }
    return removed;
  }

  /**
   * Gets the list of registered instance IDs.
   *
   * @return unmodifiable list of registered instance IDs
   */
  public List<String> getRegisteredInstanceIds() {
    validateNotClosed();
    return Collections.unmodifiableList(new ArrayList<>(instanceRegistry.keySet()));
  }

  /**
   * Gets debug information.
   *
   * @return list of debug info
   */
  public List<DebugInfo> getDebugInfo() {
    validateNotClosed();

    try {
      return nativeGetDebugInfo(nativeHandle);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get debug info", e);
      return Collections.emptyList();
    }
  }

  /**
   * Sets whether debug mode is enabled.
   *
   * @param enabled true to enable debug mode
   * @throws WasmException if operation fails
   */
  public void setDebugModeEnabled(final boolean enabled) throws WasmException {
    validateNotClosed();

    try {
      nativeSetDebugModeEnabled(nativeHandle, enabled);
      LOGGER.log(Level.FINE, "Set debug mode enabled: {0}", enabled);
    } catch (final Exception e) {
      throw new WasmException("Failed to set debug mode", e);
    }
  }

  /**
   * Checks if debug mode is enabled.
   *
   * @return true if debug mode is enabled
   */
  public boolean isDebugModeEnabled() {
    if (closed) {
      return false;
    }

    try {
      return nativeIsDebugModeEnabled(nativeHandle);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to check debug mode", e);
      return false;
    }
  }

  /**
   * Sets debug options.
   *
   * @param options the debug options
   * @throws WasmException if operation fails
   */
  public void setDebugOptions(final DebugOptions options) throws WasmException {
    Objects.requireNonNull(options, "options cannot be null");
    validateNotClosed();

    try {
      nativeSetDebugOptions(nativeHandle, options);
      LOGGER.log(Level.FINE, "Set debug options: {0}", options);
    } catch (final Exception e) {
      throw new WasmException("Failed to set debug options", e);
    }
  }

  /**
   * Gets debug options.
   *
   * @return the debug options
   */
  public DebugOptions getDebugOptions() {
    if (closed) {
      return null;
    }

    try {
      return nativeGetDebugOptions(nativeHandle);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get debug options", e);
      return null;
    }
  }

  /**
   * Checks if the debugger is valid.
   *
   * @return true if valid
   */
  public boolean isValid() {
    return !closed
        && nativeHandle != null
        && !nativeHandle.equals(MemorySegment.NULL)
        && nativeIsValidDebugger(nativeHandle);
  }

  /**
   * Gets DWARF debug information for a module.
   *
   * @param module the module to get debug info for
   * @return DWARF debug information if available
   */
  public Optional<DwarfDebugInfo> getDwarfInfo(final Module module) {
    Objects.requireNonNull(module, "module cannot be null");
    validateNotClosed();

    final String moduleKey = module.toString();
    DwarfDebugInfo cachedInfo = dwarfInfoCache.get(moduleKey);

    if (cachedInfo != null) {
      return Optional.of(cachedInfo);
    }

    try {
      final MemorySegment moduleHandle = extractModuleHandle(module);
      final DwarfDebugInfo dwarfInfo = nativeGetDwarfInfo(nativeHandle, moduleHandle);

      if (dwarfInfo != null) {
        dwarfInfoCache.put(moduleKey, dwarfInfo);
        return Optional.of(dwarfInfo);
      }

      return Optional.empty();
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get DWARF info", e);
      return Optional.empty();
    }
  }

  /**
   * Creates source map integration for the debugger.
   *
   * @param sourceMapData the source map data as JSON string
   * @return source map integration instance
   * @throws WasmException if creation fails
   */
  public SourceMapIntegration createSourceMapIntegration(final String sourceMapData)
      throws WasmException {
    Objects.requireNonNull(sourceMapData, "sourceMapData cannot be null");
    validateNotClosed();

    try {
      final SourceMapIntegration integration =
          nativeCreateSourceMapIntegration(nativeHandle, sourceMapData);

      if (integration != null) {
        sourceMapCache.put(System.currentTimeMillis(), integration);
        return integration;
      }

      throw new WasmException("Failed to create source map integration");
    } catch (final Exception e) {
      throw new WasmException("Failed to create source map integration", e);
    }
  }

  /**
   * Creates an execution tracer for the given instance.
   *
   * @param instance the instance to trace
   * @return execution tracer
   * @throws WasmException if tracer creation fails
   */
  public PanamaExecutionTracer createExecutionTracer(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final MemorySegment tracerHandle = nativeCreateExecutionTracer(nativeHandle, instanceHandle);

      if (tracerHandle == null || tracerHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create execution tracer");
      }

      final PanamaExecutionTracer tracer = new PanamaExecutionTracer(tracerHandle, instance);
      executionTracers.put(tracerHandle.address(), tracer);

      return tracer;
    } catch (final Exception e) {
      throw new WasmException("Failed to create execution tracer", e);
    }
  }

  /**
   * Starts profiling with the specified options.
   *
   * @param cpuProfiling whether to enable CPU profiling
   * @param memoryProfiling whether to enable memory profiling
   * @throws WasmException if profiling cannot be started
   */
  public void startProfiling(final boolean cpuProfiling, final boolean memoryProfiling)
      throws WasmException {
    validateNotClosed();

    try {
      nativeStartProfiling(nativeHandle, cpuProfiling, memoryProfiling);
      profilingEnabled = true;
      LOGGER.log(
          Level.FINE,
          "Started profiling (CPU: {0}, Memory: {1})",
          new Object[] {cpuProfiling, memoryProfiling});
    } catch (final Exception e) {
      throw new WasmException("Failed to start profiling", e);
    }
  }

  /**
   * Stops profiling and returns the collected data.
   *
   * @return profiling data
   * @throws WasmException if profiling cannot be stopped
   */
  public ProfilingData stopProfiling() throws WasmException {
    validateNotClosed();

    try {
      nativeStopProfiling(nativeHandle);
      final ProfilingData data = nativeGetProfilingData(nativeHandle);
      profilingEnabled = false;

      LOGGER.log(Level.FINE, "Stopped profiling");
      return data;
    } catch (final Exception e) {
      throw new WasmException("Failed to stop profiling", e);
    }
  }

  /**
   * Sets a breakpoint at the specified address.
   *
   * @param instance the instance to set breakpoint in
   * @param address the byte address
   * @return breakpoint ID
   * @throws WasmException if breakpoint cannot be set
   */
  public long setBreakpointAtAddress(final Instance instance, final int address)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeSetBreakpointAtAddress(nativeHandle, instanceHandle, address);
    } catch (final Exception e) {
      throw new WasmException("Failed to set breakpoint at address", e);
    }
  }

  /**
   * Sets a breakpoint at the specified function.
   *
   * @param instance the instance to set breakpoint in
   * @param functionName the function name
   * @return breakpoint ID
   * @throws WasmException if breakpoint cannot be set
   */
  public long setBreakpointAtFunction(final Instance instance, final String functionName)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    Objects.requireNonNull(functionName, "functionName cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeSetBreakpointAtFunction(nativeHandle, instanceHandle, functionName);
    } catch (final Exception e) {
      throw new WasmException("Failed to set breakpoint at function", e);
    }
  }

  /**
   * Sets a breakpoint at the specified source line.
   *
   * @param instance the instance to set breakpoint in
   * @param fileName the source file name
   * @param lineNumber the line number
   * @return breakpoint ID
   * @throws WasmException if breakpoint cannot be set
   */
  public long setBreakpointAtLine(
      final Instance instance, final String fileName, final int lineNumber) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    Objects.requireNonNull(fileName, "fileName cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeSetBreakpointAtLine(nativeHandle, instanceHandle, fileName, lineNumber);
    } catch (final Exception e) {
      throw new WasmException("Failed to set breakpoint at line", e);
    }
  }

  /**
   * Removes a breakpoint.
   *
   * @param breakpointId the breakpoint ID
   * @return true if breakpoint was removed
   */
  public boolean removeBreakpoint(final long breakpointId) {
    if (closed) {
      return false;
    }

    try {
      return nativeRemoveBreakpoint(nativeHandle, breakpointId);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to remove breakpoint", e);
      return false;
    }
  }

  /**
   * Gets the current call stack for an instance.
   *
   * @param instance the instance to get call stack for
   * @return list of stack frames
   * @throws WasmException if call stack cannot be retrieved
   */
  public List<PanamaStackFrame> getCallStack(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeGetCallStack(nativeHandle, instanceHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to get call stack", e);
    }
  }

  /**
   * Gets local variables for a stack frame.
   *
   * @param instance the instance
   * @param frameIndex the frame index (0 = top frame)
   * @return list of local variables
   * @throws WasmException if variables cannot be retrieved
   */
  public List<PanamaVariable> getLocalVariables(final Instance instance, final int frameIndex)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeGetLocalVariables(nativeHandle, instanceHandle, frameIndex);
    } catch (final Exception e) {
      throw new WasmException("Failed to get local variables", e);
    }
  }

  /**
   * Evaluates a debug expression.
   *
   * @param instance the instance
   * @param expression the expression to evaluate
   * @return evaluation result
   * @throws WasmException if evaluation fails
   */
  public PanamaVariableValue evaluateExpression(final Instance instance, final String expression)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    Objects.requireNonNull(expression, "expression cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeEvaluateExpression(nativeHandle, instanceHandle, expression);
    } catch (final Exception e) {
      throw new WasmException("Failed to evaluate expression", e);
    }
  }

  /**
   * Inspects memory at the specified address.
   *
   * @param instance the instance
   * @param address the memory address
   * @param length the number of bytes to inspect
   * @return memory information
   * @throws WasmException if memory cannot be inspected
   */
  public MemoryInfo inspectMemory(final Instance instance, final int address, final int length)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeInspectMemory(nativeHandle, instanceHandle, address, length);
    } catch (final Exception e) {
      throw new WasmException("Failed to inspect memory", e);
    }
  }

  /**
   * Performs a step into operation.
   *
   * @param instance the instance
   * @return execution result
   * @throws WasmException if step operation fails
   */
  public ExecutionResult stepInto(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeStepInto(nativeHandle, instanceHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to step into", e);
    }
  }

  /**
   * Performs a step over operation.
   *
   * @param instance the instance
   * @return execution result
   * @throws WasmException if step operation fails
   */
  public ExecutionResult stepOver(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeStepOver(nativeHandle, instanceHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to step over", e);
    }
  }

  /**
   * Performs a step out operation.
   *
   * @param instance the instance
   * @return execution result
   * @throws WasmException if step operation fails
   */
  public ExecutionResult stepOut(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeStepOut(nativeHandle, instanceHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to step out", e);
    }
  }

  /**
   * Continues execution.
   *
   * @param instance the instance
   * @return execution result
   * @throws WasmException if continue operation fails
   */
  public ExecutionResult continueExecution(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return nativeContinue(nativeHandle, instanceHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to continue execution", e);
    }
  }

  /**
   * Enables or disables DWARF debugging support.
   *
   * @param enabled whether to enable DWARF support
   * @throws WasmException if operation fails
   */
  public void setDwarfEnabled(final boolean enabled) throws WasmException {
    validateNotClosed();

    try {
      nativeEnableDwarf(nativeHandle, enabled);
      dwarfEnabled = enabled;
      LOGGER.log(Level.FINE, "Set DWARF enabled: {0}", enabled);
    } catch (final Exception e) {
      throw new WasmException("Failed to set DWARF enabled", e);
    }
  }

  /**
   * Checks if DWARF debugging is enabled.
   *
   * @return true if DWARF is enabled
   */
  public boolean isDwarfEnabled() {
    if (closed) {
      return false;
    }

    try {
      return nativeIsDwarfEnabled(nativeHandle);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to check DWARF enabled status", e);
      return dwarfEnabled;
    }
  }

  /**
   * Checks if profiling is currently active.
   *
   * @return true if profiling is active
   */
  public boolean isProfilingEnabled() {
    return profilingEnabled && !closed;
  }

  /**
   * Gets the native debugger handle.
   *
   * @return the native handle
   */
  MemorySegment getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Removes a session from the active sessions list. Called by PanamaDebugSession when it's closed.
   */
  void removeSession(final PanamaDebugSession session) {
    activeSessions.remove(session);
  }

  /** Closes the debugger and releases resources. */
  public void close() {
    if (closed) {
      return;
    }

    try {
      closeAllSessions();

      for (final PanamaExecutionTracer tracer : executionTracers.values()) {
        try {
          tracer.stop();
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Failed to stop execution tracer", e);
        }
      }
      executionTracers.clear();

      dwarfInfoCache.clear();
      sourceMapCache.clear();
      instanceRegistry.clear();

      if (nativeHandle != null && !nativeHandle.equals(MemorySegment.NULL)) {
        nativeCloseDebugger(nativeHandle);
      }

      closed = true;
      LOGGER.log(Level.FINE, "Closed Panama debugger");
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing debugger", e);
      closed = true;
    }
  }

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Debugger is closed");
    }
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Invalid native handle");
    }
  }

  private MemorySegment extractEngineHandle(final Engine engine) {
    Objects.requireNonNull(engine, "engine");

    if (engine instanceof PanamaEngine) {
      return ((PanamaEngine) engine).getNativeEngine();
    } else {
      throw new IllegalArgumentException("Engine must be a PanamaEngine instance");
    }
  }

  private MemorySegment extractInstanceHandle(final Instance instance) {
    Objects.requireNonNull(instance, "instance");

    if (instance instanceof PanamaInstance) {
      return ((PanamaInstance) instance).getNativeInstance();
    } else {
      throw new IllegalArgumentException("Instance must be a PanamaInstance instance");
    }
  }

  private MemorySegment extractModuleHandle(final Module module) {
    Objects.requireNonNull(module, "module");

    if (module instanceof PanamaModule) {
      return ((PanamaModule) module).getNativeModule();
    } else {
      throw new IllegalArgumentException("Module must be a PanamaModule instance");
    }
  }

  // Native method declarations - these would be implemented via Panama FFI
  // Using stub implementations for now

  private static MemorySegment nativeCreateDebugger(final MemorySegment engineHandle) {
    // TODO: Implement via Panama FFI
    return MemorySegment.NULL;
  }

  private static void nativeCloseDebugger(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
  }

  private static boolean nativeIsValidDebugger(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
    return true;
  }

  private static DebugCapabilities nativeGetCapabilities(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static MemorySegment nativeAttachToInstance(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle) {
    // TODO: Implement via Panama FFI
    return MemorySegment.NULL;
  }

  private static boolean nativeDetachFromInstance(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle) {
    // TODO: Implement via Panama FFI
    return false;
  }

  private static List<DebugInfo> nativeGetDebugInfo(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
    return Collections.emptyList();
  }

  private static void nativeSetDebugModeEnabled(
      final MemorySegment debuggerHandle, final boolean enabled) {
    // TODO: Implement via Panama FFI
  }

  private static boolean nativeIsDebugModeEnabled(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
    return false;
  }

  private static void nativeSetDebugOptions(
      final MemorySegment debuggerHandle, final DebugOptions options) {
    // TODO: Implement via Panama FFI
  }

  private static DebugOptions nativeGetDebugOptions(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static DwarfDebugInfo nativeGetDwarfInfo(
      final MemorySegment debuggerHandle, final MemorySegment moduleHandle) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static SourceMapIntegration nativeCreateSourceMapIntegration(
      final MemorySegment debuggerHandle, final String sourceMapData) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static MemorySegment nativeCreateExecutionTracer(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle) {
    // TODO: Implement via Panama FFI
    return MemorySegment.NULL;
  }

  private static void nativeStartProfiling(
      final MemorySegment debuggerHandle,
      final boolean cpuProfiling,
      final boolean memoryProfiling) {
    // TODO: Implement via Panama FFI
  }

  private static void nativeStopProfiling(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
  }

  private static ProfilingData nativeGetProfilingData(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static long nativeSetBreakpointAtAddress(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle, final int address) {
    // TODO: Implement via Panama FFI
    return -1;
  }

  private static long nativeSetBreakpointAtFunction(
      final MemorySegment debuggerHandle,
      final MemorySegment instanceHandle,
      final String functionName) {
    // TODO: Implement via Panama FFI
    return -1;
  }

  private static long nativeSetBreakpointAtLine(
      final MemorySegment debuggerHandle,
      final MemorySegment instanceHandle,
      final String fileName,
      final int lineNumber) {
    // TODO: Implement via Panama FFI
    return -1;
  }

  private static boolean nativeRemoveBreakpoint(
      final MemorySegment debuggerHandle, final long breakpointId) {
    // TODO: Implement via Panama FFI
    return false;
  }

  private static List<PanamaStackFrame> nativeGetCallStack(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle) {
    // TODO: Implement via Panama FFI
    return Collections.emptyList();
  }

  private static List<PanamaVariable> nativeGetLocalVariables(
      final MemorySegment debuggerHandle,
      final MemorySegment instanceHandle,
      final int frameIndex) {
    // TODO: Implement via Panama FFI
    return Collections.emptyList();
  }

  private static PanamaVariableValue nativeEvaluateExpression(
      final MemorySegment debuggerHandle,
      final MemorySegment instanceHandle,
      final String expression) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static MemoryInfo nativeInspectMemory(
      final MemorySegment debuggerHandle,
      final MemorySegment instanceHandle,
      final int address,
      final int length) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static ExecutionResult nativeStepInto(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static ExecutionResult nativeStepOver(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static ExecutionResult nativeStepOut(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static ExecutionResult nativeContinue(
      final MemorySegment debuggerHandle, final MemorySegment instanceHandle) {
    // TODO: Implement via Panama FFI
    return null;
  }

  private static void nativeEnableDwarf(final MemorySegment debuggerHandle, final boolean enabled) {
    // TODO: Implement via Panama FFI
  }

  private static boolean nativeIsDwarfEnabled(final MemorySegment debuggerHandle) {
    // TODO: Implement via Panama FFI
    return true;
  }

  /** DWARF debug info implementation. */
  public static final class DwarfDebugInfo {
    // TODO: Implement DWARF debug info support
  }

  /** Source map integration implementation. */
  public static final class SourceMapIntegration {
    // TODO: Implement source map integration support
  }

  /** Profiling data implementation. */
  public static final class ProfilingData {
    // TODO: Implement profiling data support
  }

  /** Memory info implementation. */
  public static final class MemoryInfo {
    // TODO: Implement memory info support
  }

  /** Execution result implementation. */
  public static final class ExecutionResult {
    // TODO: Implement execution result support
  }

  /** Execution tracer implementation. */
  public static final class PanamaExecutionTracer {
    private final MemorySegment nativeHandle;
    private final Instance instance;
    private volatile boolean started;

    /**
     * Creates an execution tracer.
     *
     * @param nativeHandle the native tracer handle
     * @param instance the instance being traced
     */
    PanamaExecutionTracer(final MemorySegment nativeHandle, final Instance instance) {
      this.nativeHandle = nativeHandle;
      this.instance = instance;
      this.started = false;
    }

    /** Starts tracing. */
    public void start() {
      started = true;
    }

    /** Stops tracing. */
    public void stop() {
      started = false;
    }

    /**
     * Checks if tracing is active.
     *
     * @return true if started
     */
    public boolean isStarted() {
      return started;
    }

    /**
     * Gets trace events.
     *
     * @return list of trace events
     */
    public List<String> getEvents() {
      // TODO: Return actual trace events
      return new ArrayList<>();
    }

    /** Clears trace events. */
    public void clearEvents() {
      // Implementation would clear native trace buffer
    }
  }
}
