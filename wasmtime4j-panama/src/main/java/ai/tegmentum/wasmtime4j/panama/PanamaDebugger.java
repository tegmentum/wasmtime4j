package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.debug.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaExceptionHandler;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;

import java.lang.foreign.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.foreign.ValueLayout.*;

/**
 * Panama FFI implementation of WebAssembly debugger.
 *
 * <p>This class provides Panama Foreign Function Interface bindings for WebAssembly
 * debugging functionality, including session management, breakpoint control, and
 * execution monitoring.
 *
 * <p>All methods implement defensive programming patterns to prevent JVM crashes
 * and ensure robust operation in production environments.
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

    private final MemorySegment nativeHandle;
    private final Engine engine;
    private final List<DebugSession> activeSessions;
    private final Arena arena;
    private volatile boolean closed;

    static {
        try {
            final SymbolLookup nativeLib = NativeFunctionBindings.getNativeLibrary();

            CREATE_DEBUGGER = nativeLib.find("wasmtime4j_debug_create_debugger")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.of(ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_create_debugger"));

            CLOSE_DEBUGGER = nativeLib.find("wasmtime4j_debug_close_debugger")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.ofVoid(ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_close_debugger"));

            IS_VALID_DEBUGGER = nativeLib.find("wasmtime4j_debug_is_valid_debugger")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_is_valid_debugger"));

            GET_CAPABILITIES = nativeLib.find("wasmtime4j_debug_get_capabilities")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.of(ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_capabilities"));

            ATTACH_TO_INSTANCE = nativeLib.find("wasmtime4j_debug_attach_to_instance")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_attach_to_instance"));

            DETACH_FROM_INSTANCE = nativeLib.find("wasmtime4j_debug_detach_from_instance")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_detach_from_instance"));

            GET_DEBUG_INFO = nativeLib.find("wasmtime4j_debug_get_debug_info")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.of(ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_debug_info"));

            SET_DEBUG_MODE_ENABLED = nativeLib.find("wasmtime4j_debug_set_debug_mode_enabled")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.ofVoid(ADDRESS, JAVA_BOOLEAN)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_set_debug_mode_enabled"));

            IS_DEBUG_MODE_ENABLED = nativeLib.find("wasmtime4j_debug_is_debug_mode_enabled")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_is_debug_mode_enabled"));

            SET_DEBUG_OPTIONS = nativeLib.find("wasmtime4j_debug_set_debug_options")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_set_debug_options"));

            GET_DEBUG_OPTIONS = nativeLib.find("wasmtime4j_debug_get_debug_options")
                    .map(addr -> Linker.nativeLinker().downcallHandle(addr,
                            FunctionDescriptor.of(ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_debug_get_debug_options"));

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
        this.arena = Arena.ofConfined();
        this.closed = false;

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

            final PanamaDebugSession session = new PanamaDebugSession(this, nativeHandle, instanceHandlesArray);
            activeSessions.add(session);

            LOGGER.log(Level.FINE, "Created multi-instance debug session: {0}", session.getSessionId());
            return session;
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to create multi-instance debug session");
        }
    }

    @Override
    public DebugSession createSession(final Instance instance, final DebugConfig config) throws WasmException {
        Objects.requireNonNull(instance, "instance cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        validateNotClosed();

        try {
            final MemorySegment instanceHandle = extractInstanceHandle(instance);
            final PanamaDebugSession session = new PanamaDebugSession(this, nativeHandle, instanceHandle, config);
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
            final MemorySegment capabilitiesPtr = (MemorySegment) GET_CAPABILITIES.invokeExact(nativeHandle);
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
            final MemorySegment sessionHandle = (MemorySegment) ATTACH_TO_INSTANCE.invokeExact(nativeHandle, instanceHandle);

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
     * Removes a session from the active sessions list.
     * Called by PanamaDebugSession when it's closed.
     */
    void removeSession(final DebugSession session) {
        activeSessions.remove(session);
    }

    /**
     * Gets the native debugger handle.
     */
    MemorySegment getNativeHandle() {
        return nativeHandle;
    }

    /**
     * Gets the memory arena for this debugger.
     */
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
}