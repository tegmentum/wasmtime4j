package ai.tegmentum.wasmtime4j.panama;

import static java.lang.foreign.ValueLayout.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.debug.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaExceptionHandler;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.*;
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
 * <p>This class provides Panama Foreign Function Interface bindings for WebAssembly debugging
 * functionality, including session management, breakpoint control, and execution monitoring.
 *
 * <p>All methods implement defensive programming patterns to prevent JVM crashes and ensure robust
 * operation in production environments.
 *
 * @since 1.0.0
 */
public final class PanamaDebugger implements Debugger {

  private static final Logger LOGGER = Logger.getLogger(PanamaDebugger.class.getName());

  // Native function handles loaded via Panama FFI
  private static final MethodHandle CREATE_DEBUGGER;
  private static final MethodHandle CLOSE_DEBUGGER;
  private static final MethodHandle IS_VALID_DEBUGGER;
  private static final MethodHandle GET_CAPABILITIES;
  private static final MethodHandle ATTACH_TO_INSTANCE;
  private static final MethodHandle DETACH_FROM_INSTANCE;
  private static final MethodHandle GET_DEBUG_INFO;
  private static final MethodHandle SET_DEBUG_MODE_ENABLED;
  private static final MethodHandle IS_DEBUG_MODE_ENABLED;
  private static final MethodHandle SET_DEBUG_OPTIONS;
  private static final MethodHandle GET_DEBUG_OPTIONS;

  // Advanced debugging function handles
  private static final MethodHandle GET_DWARF_INFO;
  private static final MethodHandle CREATE_SOURCE_MAP_INTEGRATION;
  private static final MethodHandle CREATE_EXECUTION_TRACER;
  private static final MethodHandle START_PROFILING;
  private static final MethodHandle STOP_PROFILING;
  private static final MethodHandle GET_PROFILING_DATA;
  private static final MethodHandle SET_BREAKPOINT_AT_ADDRESS;
  private static final MethodHandle SET_BREAKPOINT_AT_FUNCTION;
  private static final MethodHandle SET_BREAKPOINT_AT_LINE;
  private static final MethodHandle REMOVE_BREAKPOINT;
  private static final MethodHandle GET_CALL_STACK;
  private static final MethodHandle GET_LOCAL_VARIABLES;
  private static final MethodHandle EVALUATE_EXPRESSION;
  private static final MethodHandle INSPECT_MEMORY;
  private static final MethodHandle STEP_INTO;
  private static final MethodHandle STEP_OVER;
  private static final MethodHandle STEP_OUT;
  private static final MethodHandle CONTINUE_EXECUTION;
  private static final MethodHandle ENABLE_DWARF;
  private static final MethodHandle IS_DWARF_ENABLED;

  private final MemorySegment nativeHandle;
  private final Engine engine;
  private final List<DebugSession> activeSessions;
  private final Map<String, DwarfDebugInfo> dwarfInfoCache;
  private final Map<Long, SourceMapIntegration> sourceMapCache;
  private final Map<Long, ExecutionTracer> executionTracers;
  private final Arena arena;
  private volatile boolean closed;
  private volatile boolean dwarfEnabled;
  private volatile boolean profilingEnabled;

  static {
    try {
      final SymbolLookup nativeLib = NativeFunctionBindings.getNativeLibrary();

      CREATE_DEBUGGER =
          nativeLib
              .find("wasmtime4j_debug_create_debugger")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_create_debugger"));

      CLOSE_DEBUGGER =
          nativeLib
              .find("wasmtime4j_debug_close_debugger")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_close_debugger"));

      IS_VALID_DEBUGGER =
          nativeLib
              .find("wasmtime4j_debug_is_valid_debugger")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_is_valid_debugger"));

      GET_CAPABILITIES =
          nativeLib
              .find("wasmtime4j_debug_get_capabilities")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_capabilities"));

      ATTACH_TO_INSTANCE =
          nativeLib
              .find("wasmtime4j_debug_attach_to_instance")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_attach_to_instance"));

      DETACH_FROM_INSTANCE =
          nativeLib
              .find("wasmtime4j_debug_detach_from_instance")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_detach_from_instance"));

      GET_DEBUG_INFO =
          nativeLib
              .find("wasmtime4j_debug_get_debug_info")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_debug_info"));

      SET_DEBUG_MODE_ENABLED =
          nativeLib
              .find("wasmtime4j_debug_set_debug_mode_enabled")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_BOOLEAN)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_debug_set_debug_mode_enabled"));

      IS_DEBUG_MODE_ENABLED =
          nativeLib
              .find("wasmtime4j_debug_is_debug_mode_enabled")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_debug_is_debug_mode_enabled"));

      SET_DEBUG_OPTIONS =
          nativeLib
              .find("wasmtime4j_debug_set_debug_options")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_set_debug_options"));

      GET_DEBUG_OPTIONS =
          nativeLib
              .find("wasmtime4j_debug_get_debug_options")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_debug_options"));

      // Advanced debugging function handles
      GET_DWARF_INFO =
          nativeLib
              .find("wasmtime4j_debug_get_dwarf_info")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_dwarf_info"));

      CREATE_SOURCE_MAP_INTEGRATION =
          nativeLib
              .find("wasmtime4j_debug_create_source_map_integration")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_debug_create_source_map_integration"));

      CREATE_EXECUTION_TRACER =
          nativeLib
              .find("wasmtime4j_debug_create_execution_tracer")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_debug_create_execution_tracer"));

      START_PROFILING =
          nativeLib
              .find("wasmtime4j_debug_start_profiling")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_BOOLEAN, JAVA_BOOLEAN)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_start_profiling"));

      STOP_PROFILING =
          nativeLib
              .find("wasmtime4j_debug_stop_profiling")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_stop_profiling"));

      GET_PROFILING_DATA =
          nativeLib
              .find("wasmtime4j_debug_get_profiling_data")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_profiling_data"));

      SET_BREAKPOINT_AT_ADDRESS =
          nativeLib
              .find("wasmtime4j_debug_set_breakpoint_at_address")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr, FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_INT)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_debug_set_breakpoint_at_address"));

      SET_BREAKPOINT_AT_FUNCTION =
          nativeLib
              .find("wasmtime4j_debug_set_breakpoint_at_function")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr, FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_debug_set_breakpoint_at_function"));

      SET_BREAKPOINT_AT_LINE =
          nativeLib
              .find("wasmtime4j_debug_set_breakpoint_at_line")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr,
                              FunctionDescriptor.of(
                                  JAVA_LONG, ADDRESS, ADDRESS, ADDRESS, JAVA_INT)))
              .orElseThrow(
                  () -> new UnsatisfiedLinkError("wasmtime4j_debug_set_breakpoint_at_line"));

      REMOVE_BREAKPOINT =
          nativeLib
              .find("wasmtime4j_debug_remove_breakpoint")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, JAVA_LONG)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_remove_breakpoint"));

      GET_CALL_STACK =
          nativeLib
              .find("wasmtime4j_debug_get_call_stack")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_call_stack"));

      GET_LOCAL_VARIABLES =
          nativeLib
              .find("wasmtime4j_debug_get_local_variables")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_local_variables"));

      EVALUATE_EXPRESSION =
          nativeLib
              .find("wasmtime4j_debug_evaluate_expression")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_evaluate_expression"));

      INSPECT_MEMORY =
          nativeLib
              .find("wasmtime4j_debug_inspect_memory")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(
                              addr,
                              FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_inspect_memory"));

      STEP_INTO =
          nativeLib
              .find("wasmtime4j_debug_step_into")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_step_into"));

      STEP_OVER =
          nativeLib
              .find("wasmtime4j_debug_step_over")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_step_over"));

      STEP_OUT =
          nativeLib
              .find("wasmtime4j_debug_step_out")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_step_out"));

      CONTINUE_EXECUTION =
          nativeLib
              .find("wasmtime4j_debug_continue")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_continue"));

      ENABLE_DWARF =
          nativeLib
              .find("wasmtime4j_debug_enable_dwarf")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_BOOLEAN)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_enable_dwarf"));

      IS_DWARF_ENABLED =
          nativeLib
              .find("wasmtime4j_debug_is_dwarf_enabled")
              .map(
                  addr ->
                      Linker.nativeLinker()
                          .downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS)))
              .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_is_dwarf_enabled"));

      LOGGER.log(Level.FINE, "Loaded native debugging functions via Panama FFI");
    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to load native debugging library", e);
      throw new RuntimeException("Failed to load native debugging library", e);
    }
  }

  /**
   * Creates a Panama debugger instance.
   *
   * @param engine the engine to debug
   * @throws WasmException if debugger creation fails
   * @throws IllegalArgumentException if engine is null
   */
  public PanamaDebugger(final Engine engine) throws WasmException {
    this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    this.activeSessions = new CopyOnWriteArrayList<>();
    this.dwarfInfoCache = new ConcurrentHashMap<>();
    this.sourceMapCache = new ConcurrentHashMap<>();
    this.executionTracers = new ConcurrentHashMap<>();
    this.arena = Arena.ofConfined();
    this.closed = false;
    this.dwarfEnabled = true;
    this.profilingEnabled = false;

    try {
      // Extract native engine handle
      final MemorySegment engineHandle = extractEngineHandle(engine);
      this.nativeHandle = (MemorySegment) CREATE_DEBUGGER.invokeExact(engineHandle);

      if (nativeHandle.address() == 0L) {
        throw new WasmException("Failed to create native debugger");
      }

      LOGGER.log(Level.FINE, "Created Panama debugger with handle: {0}", nativeHandle.address());
    } catch (final Throwable e) {
      arena.close();
      throw PanamaExceptionHandler.wrapException(e, "Failed to create debugger");
    }
  }

  @Override
  public Engine getEngine() {
    validateNotClosed();
    return engine;
  }

  @Override
  public DebugSession createSession(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final PanamaDebugSession session = new PanamaDebugSession(this, nativeHandle, instanceHandle);
      activeSessions.add(session);

      LOGGER.log(Level.FINE, "Created debug session: {0}", session.getSessionId());
      return session;
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to create debug session");
    }
  }

  @Override
  public DebugSession createSession(final List<Instance> instances) throws WasmException {
    Objects.requireNonNull(instances, "instances cannot be null");
    if (instances.isEmpty()) {
      throw new IllegalArgumentException("instances cannot be empty");
    }
    validateNotClosed();

    try {
      final MemorySegment instanceHandlesArray = arena.allocateArray(ADDRESS, instances.size());
      for (int i = 0; i < instances.size(); i++) {
        final MemorySegment instanceHandle = extractInstanceHandle(instances.get(i));
        instanceHandlesArray.setAtIndex(ADDRESS, i, instanceHandle);
      }

      final PanamaDebugSession session =
          new PanamaDebugSession(this, nativeHandle, instanceHandlesArray);
      activeSessions.add(session);

      LOGGER.log(Level.FINE, "Created multi-instance debug session: {0}", session.getSessionId());
      return session;
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(
          e, "Failed to create multi-instance debug session");
    }
  }

  @Override
  public DebugSession createSession(final Instance instance, final DebugConfig config)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    Objects.requireNonNull(config, "config cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final PanamaDebugSession session =
          new PanamaDebugSession(this, nativeHandle, instanceHandle, config);
      activeSessions.add(session);

      LOGGER.log(Level.FINE, "Created configured debug session: {0}", session.getSessionId());
      return session;
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to create configured debug session");
    }
  }

  @Override
  public List<DebugSession> getActiveSessions() {
    validateNotClosed();
    return List.copyOf(activeSessions);
  }

  @Override
  public boolean closeSession(final DebugSession session) {
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

  @Override
  public int closeAllSessions() {
    validateNotClosed();

    int closedCount = 0;
    for (final DebugSession session : List.copyOf(activeSessions)) {
      if (closeSession(session)) {
        closedCount++;
      }
    }

    LOGGER.log(Level.FINE, "Closed {0} debug sessions", closedCount);
    return closedCount;
  }

  @Override
  public DebugCapabilities getCapabilities() {
    validateNotClosed();

    try {
      final MemorySegment capabilitiesPtr =
          (MemorySegment) GET_CAPABILITIES.invokeExact(nativeHandle);
      return deserializeDebugCapabilities(capabilitiesPtr);
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to get debug capabilities", e);
      // Return minimal capabilities on failure
      return DebugCapabilities.minimalCapabilities("unknown");
    }
  }

  @Override
  public DebugSession attach(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final MemorySegment sessionHandle =
          (MemorySegment) ATTACH_TO_INSTANCE.invokeExact(nativeHandle, instanceHandle);

      if (sessionHandle.address() == 0L) {
        throw new WasmException("Failed to attach to instance");
      }

      final PanamaDebugSession session = new PanamaDebugSession(this, sessionHandle);
      activeSessions.add(session);

      LOGGER.log(Level.FINE, "Attached to instance, created session: {0}", session.getSessionId());
      return session;
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to attach to instance");
    }
  }

  @Override
  public boolean detach(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      return (boolean) DETACH_FROM_INSTANCE.invokeExact(nativeHandle, instanceHandle);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to detach from instance");
    }
  }

  @Override
  public List<DebugInfo> getDebugInfo() {
    validateNotClosed();

    try {
      final MemorySegment debugInfoPtr = (MemorySegment) GET_DEBUG_INFO.invokeExact(nativeHandle);
      return deserializeDebugInfoList(debugInfoPtr);
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to get debug info", e);
      return List.of();
    }
  }

  @Override
  public void setDebugModeEnabled(final boolean enabled) throws WasmException {
    validateNotClosed();

    try {
      SET_DEBUG_MODE_ENABLED.invokeExact(nativeHandle, enabled);
      LOGGER.log(Level.FINE, "Set debug mode enabled: {0}", enabled);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to set debug mode");
    }
  }

  @Override
  public boolean isDebugModeEnabled() {
    if (closed) {
      return false;
    }

    try {
      return (boolean) IS_DEBUG_MODE_ENABLED.invokeExact(nativeHandle);
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to check debug mode", e);
      return false;
    }
  }

  @Override
  public void setDebugOptions(final DebugOptions options) throws WasmException {
    Objects.requireNonNull(options, "options cannot be null");
    validateNotClosed();

    try {
      final MemorySegment optionsPtr = serializeDebugOptions(options);
      SET_DEBUG_OPTIONS.invokeExact(nativeHandle, optionsPtr);
      LOGGER.log(Level.FINE, "Set debug options: {0}", options);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to set debug options");
    }
  }

  @Override
  public DebugOptions getDebugOptions() {
    if (closed) {
      return DebugOptions.defaultOptions();
    }

    try {
      final MemorySegment optionsPtr = (MemorySegment) GET_DEBUG_OPTIONS.invokeExact(nativeHandle);
      return deserializeDebugOptions(optionsPtr);
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to get debug options", e);
      return DebugOptions.defaultOptions();
    }
  }

  @Override
  public boolean isValid() {
    if (closed || nativeHandle.address() == 0L) {
      return false;
    }

    try {
      return (boolean) IS_VALID_DEBUGGER.invokeExact(nativeHandle);
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to check debugger validity", e);
      return false;
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      // Close all active sessions first
      closeAllSessions();

      // Close execution tracers
      for (final ExecutionTracer tracer : executionTracers.values()) {
        try {
          tracer.stop();
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Failed to stop execution tracer", e);
        }
      }
      executionTracers.clear();

      // Clear caches
      dwarfInfoCache.clear();
      sourceMapCache.clear();

      // Close native debugger
      if (nativeHandle.address() != 0L) {
        CLOSE_DEBUGGER.invokeExact(nativeHandle);
      }

      // Close arena
      arena.close();

      closed = true;
      LOGGER.log(Level.FINE, "Closed Panama debugger");
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Error closing debugger", e);
      closed = true; // Mark as closed even on error
    }
  }

  // Package-private helper methods

  /**
   * Removes a session from the active sessions list. Called by PanamaDebugSession when it's closed.
   */
  void removeSession(final DebugSession session) {
    activeSessions.remove(session);
  }

  /** Gets the native debugger handle. */
  MemorySegment getNativeHandle() {
    return nativeHandle;
  }

  /** Gets the memory arena for this debugger. */
  Arena getArena() {
    return arena;
  }

  // Private helper methods

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Debugger is closed");
    }
    if (nativeHandle.address() == 0L) {
      throw new IllegalStateException("Invalid native handle");
    }
  }

  private MemorySegment extractEngineHandle(final Engine engine) {
    PanamaValidation.validateNotNull(engine, "engine");

    if (engine instanceof PanamaEngine) {
      return ((PanamaEngine) engine).getNativeHandle();
    } else {
      throw new IllegalArgumentException("Engine must be a PanamaEngine instance");
    }
  }

  private MemorySegment extractInstanceHandle(final Instance instance) {
    PanamaValidation.validateNotNull(instance, "instance");

    if (instance instanceof PanamaInstance) {
      return ((PanamaInstance) instance).getNativeHandle();
    } else {
      throw new IllegalArgumentException("Instance must be a PanamaInstance instance");
    }
  }

  // Serialization/deserialization helper methods
  // In a real implementation, these would properly serialize/deserialize complex objects

  private DebugCapabilities deserializeDebugCapabilities(final MemorySegment ptr) {
    // For now, return full capabilities - in real implementation this would
    // deserialize from native structure
    return DebugCapabilities.fullCapabilities("1.0.0");
  }

  private List<DebugInfo> deserializeDebugInfoList(final MemorySegment ptr) {
    // For now, return empty list - in real implementation this would
    // deserialize from native array structure
    return List.of();
  }

  private MemorySegment serializeDebugOptions(final DebugOptions options) {
    // For now, return null pointer - in real implementation this would
    // serialize the options to native structure
    return MemorySegment.NULL;
  }

  private DebugOptions deserializeDebugOptions(final MemorySegment ptr) {
    // For now, return default options - in real implementation this would
    // deserialize from native structure
    return DebugOptions.defaultOptions();
  }

  // Advanced debugging methods implementation

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
      final MemorySegment dwarfInfoPtr =
          (MemorySegment) GET_DWARF_INFO.invokeExact(nativeHandle, moduleHandle);

      if (dwarfInfoPtr != null && dwarfInfoPtr.address() != 0L) {
        final DwarfDebugInfo dwarfInfo = deserializeDwarfInfo(dwarfInfoPtr);
        dwarfInfoCache.put(moduleKey, dwarfInfo);
        return Optional.of(dwarfInfo);
      }

      return Optional.empty();
    } catch (final Throwable e) {
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
      final MemorySegment sourceMapStr = arena.allocateUtf8String(sourceMapData);
      final MemorySegment integrationPtr =
          (MemorySegment) CREATE_SOURCE_MAP_INTEGRATION.invokeExact(nativeHandle, sourceMapStr);

      if (integrationPtr != null && integrationPtr.address() != 0L) {
        final SourceMapIntegration integration = deserializeSourceMapIntegration(integrationPtr);
        sourceMapCache.put(System.currentTimeMillis(), integration);
        return integration;
      }

      throw new WasmException("Failed to create source map integration");
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to create source map integration");
    }
  }

  /**
   * Creates an execution tracer for the given instance.
   *
   * @param instance the instance to trace
   * @return execution tracer
   * @throws WasmException if tracer creation fails
   */
  public ExecutionTracer createExecutionTracer(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final MemorySegment tracerPtr =
          (MemorySegment) CREATE_EXECUTION_TRACER.invokeExact(nativeHandle, instanceHandle);

      if (tracerPtr == null || tracerPtr.address() == 0L) {
        throw new WasmException("Failed to create execution tracer");
      }

      final ExecutionTracer tracer = new PanamaExecutionTracer(tracerPtr, instance);
      executionTracers.put(tracerPtr.address(), tracer);

      return tracer;
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to create execution tracer");
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
      START_PROFILING.invokeExact(nativeHandle, cpuProfiling, memoryProfiling);
      profilingEnabled = true;
      LOGGER.log(
          Level.FINE,
          "Started profiling (CPU: {0}, Memory: {1})",
          new Object[] {cpuProfiling, memoryProfiling});
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to start profiling");
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
      STOP_PROFILING.invokeExact(nativeHandle);
      final MemorySegment dataPtr = (MemorySegment) GET_PROFILING_DATA.invokeExact(nativeHandle);
      final ProfilingData data = deserializeProfilingData(dataPtr);
      profilingEnabled = false;

      LOGGER.log(Level.FINE, "Stopped profiling");
      return data;
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to stop profiling");
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
      return (long) SET_BREAKPOINT_AT_ADDRESS.invokeExact(nativeHandle, instanceHandle, address);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to set breakpoint at address");
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
      final MemorySegment functionNameStr = arena.allocateUtf8String(functionName);
      return (long)
          SET_BREAKPOINT_AT_FUNCTION.invokeExact(nativeHandle, instanceHandle, functionNameStr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to set breakpoint at function");
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
      final MemorySegment fileNameStr = arena.allocateUtf8String(fileName);
      return (long)
          SET_BREAKPOINT_AT_LINE.invokeExact(nativeHandle, instanceHandle, fileNameStr, lineNumber);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to set breakpoint at line");
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
      return (boolean) REMOVE_BREAKPOINT.invokeExact(nativeHandle, breakpointId);
    } catch (final Throwable e) {
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
  public List<StackFrame> getCallStack(final Instance instance) throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final MemorySegment stackPtr =
          (MemorySegment) GET_CALL_STACK.invokeExact(nativeHandle, instanceHandle);
      return deserializeStackFrames(stackPtr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to get call stack");
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
  public List<Variable> getLocalVariables(final Instance instance, final int frameIndex)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final MemorySegment variablesPtr =
          (MemorySegment) GET_LOCAL_VARIABLES.invokeExact(nativeHandle, instanceHandle, frameIndex);
      return deserializeVariables(variablesPtr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to get local variables");
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
  public VariableValue evaluateExpression(final Instance instance, final String expression)
      throws WasmException {
    Objects.requireNonNull(instance, "instance cannot be null");
    Objects.requireNonNull(expression, "expression cannot be null");
    validateNotClosed();

    try {
      final MemorySegment instanceHandle = extractInstanceHandle(instance);
      final MemorySegment expressionStr = arena.allocateUtf8String(expression);
      final MemorySegment resultPtr =
          (MemorySegment)
              EVALUATE_EXPRESSION.invokeExact(nativeHandle, instanceHandle, expressionStr);
      return deserializeVariableValue(resultPtr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to evaluate expression");
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
      final MemorySegment memoryPtr =
          (MemorySegment) INSPECT_MEMORY.invokeExact(nativeHandle, instanceHandle, address, length);
      return deserializeMemoryInfo(memoryPtr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to inspect memory");
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
      final MemorySegment resultPtr =
          (MemorySegment) STEP_INTO.invokeExact(nativeHandle, instanceHandle);
      return deserializeExecutionResult(resultPtr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to step into");
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
      final MemorySegment resultPtr =
          (MemorySegment) STEP_OVER.invokeExact(nativeHandle, instanceHandle);
      return deserializeExecutionResult(resultPtr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to step over");
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
      final MemorySegment resultPtr =
          (MemorySegment) STEP_OUT.invokeExact(nativeHandle, instanceHandle);
      return deserializeExecutionResult(resultPtr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to step out");
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
      final MemorySegment resultPtr =
          (MemorySegment) CONTINUE_EXECUTION.invokeExact(nativeHandle, instanceHandle);
      return deserializeExecutionResult(resultPtr);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to continue execution");
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
      ENABLE_DWARF.invokeExact(nativeHandle, enabled);
      dwarfEnabled = enabled;
      LOGGER.log(Level.FINE, "Set DWARF enabled: {0}", enabled);
    } catch (final Throwable e) {
      throw PanamaExceptionHandler.wrapException(e, "Failed to set DWARF enabled");
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
      return (boolean) IS_DWARF_ENABLED.invokeExact(nativeHandle);
    } catch (final Throwable e) {
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

  private MemorySegment extractModuleHandle(final Module module) {
    PanamaValidation.validateNotNull(module, "module");

    if (module instanceof PanamaModule) {
      return ((PanamaModule) module).getNativeHandle();
    } else {
      throw new IllegalArgumentException("Module must be a PanamaModule instance");
    }
  }

  // Deserialization helper methods for advanced debugging features

  private DwarfDebugInfo deserializeDwarfInfo(final MemorySegment ptr) {
    // For now, create a simple DWARF info - real implementation would deserialize from native
    // structure
    try {
      return new DwarfDebugInfo(Map.of());
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to deserialize DWARF info", e);
      return null;
    }
  }

  private SourceMapIntegration deserializeSourceMapIntegration(final MemorySegment ptr) {
    // For now, return a simple source map integration - real implementation would deserialize
    return new SourceMapIntegration() {
      @Override
      public Optional<SourcePosition> mapWasmAddress(final int address) {
        return Optional.empty();
      }

      @Override
      public Optional<Integer> mapSourcePosition(
          final String fileName, final int line, final int column) {
        return Optional.empty();
      }

      @Override
      public List<String> getSourceFiles() {
        return List.of();
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public void close() {
        // Implementation would clean up native resources
      }
    };
  }

  private ProfilingData deserializeProfilingData(final MemorySegment ptr) {
    // For now, return empty profiling data - real implementation would deserialize
    return new ProfilingData() {
      @Override
      public List<FunctionProfile> getFunctionProfiles() {
        return List.of();
      }

      @Override
      public long getTotalExecutionTime() {
        return 0;
      }

      @Override
      public long getTotalAllocatedMemory() {
        return 0;
      }

      @Override
      public Map<String, Object> getMetrics() {
        return Map.of();
      }
    };
  }

  private List<StackFrame> deserializeStackFrames(final MemorySegment ptr) {
    // For now, return empty list - real implementation would deserialize
    return List.of();
  }

  private List<Variable> deserializeVariables(final MemorySegment ptr) {
    // For now, return empty list - real implementation would deserialize
    return List.of();
  }

  private VariableValue deserializeVariableValue(final MemorySegment ptr) {
    // For now, return null value - real implementation would deserialize
    return VariableValue.nullValue();
  }

  private MemoryInfo deserializeMemoryInfo(final MemorySegment ptr) {
    // For now, return empty memory info - real implementation would deserialize
    return new MemoryInfo(0, new byte[0], Map.of());
  }

  private ExecutionResult deserializeExecutionResult(final MemorySegment ptr) {
    // For now, return simple result - real implementation would deserialize
    return ExecutionResult.success();
  }

  /** Simple execution tracer implementation for Panama. */
  private static final class PanamaExecutionTracer implements ExecutionTracer {
    private final MemorySegment nativeHandle;
    private final Instance instance;
    private volatile boolean started;

    public PanamaExecutionTracer(final MemorySegment nativeHandle, final Instance instance) {
      this.nativeHandle = nativeHandle;
      this.instance = instance;
      this.started = false;
    }

    @Override
    public void start() {
      started = true;
    }

    @Override
    public void stop() {
      started = false;
    }

    @Override
    public boolean isStarted() {
      return started;
    }

    @Override
    public List<TraceEvent> getEvents() {
      return List.of();
    }

    @Override
    public void clearEvents() {
      // Implementation would clear native trace buffer
    }
  }
}
