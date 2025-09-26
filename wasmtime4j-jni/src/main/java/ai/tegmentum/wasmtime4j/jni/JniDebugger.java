package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.debug.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WebAssembly debugger.
 *
 * <p>This class provides JNI bindings for WebAssembly debugging functionality,
 * including session management, breakpoint control, and execution monitoring.
 *
 * <p>All methods implement defensive programming patterns to prevent JVM crashes
 * and ensure robust operation in production environments.
 *
 * @since 1.0.0
 */
public final class JniDebugger implements Debugger {

    private static final Logger LOGGER = Logger.getLogger(JniDebugger.class.getName());

    private final long nativeHandle;
    private final Engine engine;
    private final List<DebugSession> activeSessions;
    private volatile boolean closed;

    /**
     * Creates a JNI debugger instance.
     *
     * @param engine the engine to debug
     * @throws WasmException if debugger creation fails
     * @throws IllegalArgumentException if engine is null
     */
    public JniDebugger(final Engine engine) throws WasmException {
        this.engine = Objects.requireNonNull(engine, "engine cannot be null");
        this.activeSessions = new CopyOnWriteArrayList<>();
        this.closed = false;

        try {
            // Extract native engine handle - assuming JniEngine has a getNativeHandle method
            final long engineHandle = extractEngineHandle(engine);
            this.nativeHandle = nativeCreateDebugger(engineHandle);

            if (this.nativeHandle == 0) {
                throw new WasmException("Failed to create native debugger");
            }

            LOGGER.log(Level.FINE, "Created JNI debugger with handle: {0}", this.nativeHandle);
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to create debugger");
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
            final long instanceHandle = extractInstanceHandle(instance);
            final JniDebugSession session = new JniDebugSession(this, nativeHandle, instanceHandle);
            activeSessions.add(session);

            LOGGER.log(Level.FINE, "Created debug session: {0}", session.getSessionId());
            return session;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to create debug session");
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
            final long[] instanceHandles = instances.stream()
                    .mapToLong(this::extractInstanceHandle)
                    .toArray();

            final JniDebugSession session = new JniDebugSession(this, nativeHandle, instanceHandles);
            activeSessions.add(session);

            LOGGER.log(Level.FINE, "Created multi-instance debug session: {0}", session.getSessionId());
            return session;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to create multi-instance debug session");
        }
    }

    @Override
    public DebugSession createSession(final Instance instance, final DebugConfig config) throws WasmException {
        Objects.requireNonNull(instance, "instance cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        validateNotClosed();

        try {
            final long instanceHandle = extractInstanceHandle(instance);
            final JniDebugSession session = new JniDebugSession(this, nativeHandle, instanceHandle, config);
            activeSessions.add(session);

            LOGGER.log(Level.FINE, "Created configured debug session: {0}", session.getSessionId());
            return session;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to create configured debug session");
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
            return nativeGetCapabilities(nativeHandle);
        } catch (final Exception e) {
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
            final long instanceHandle = extractInstanceHandle(instance);
            final long sessionHandle = nativeAttachToInstance(nativeHandle, instanceHandle);

            if (sessionHandle == 0) {
                throw new WasmException("Failed to attach to instance");
            }

            final JniDebugSession session = new JniDebugSession(this, sessionHandle);
            activeSessions.add(session);

            LOGGER.log(Level.FINE, "Attached to instance, created session: {0}", session.getSessionId());
            return session;
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to attach to instance");
        }
    }

    @Override
    public boolean detach(final Instance instance) throws WasmException {
        Objects.requireNonNull(instance, "instance cannot be null");
        validateNotClosed();

        try {
            final long instanceHandle = extractInstanceHandle(instance);
            return nativeDetachFromInstance(nativeHandle, instanceHandle);
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to detach from instance");
        }
    }

    @Override
    public List<DebugInfo> getDebugInfo() {
        validateNotClosed();

        try {
            return nativeGetDebugInfo(nativeHandle);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get debug info", e);
            return List.of();
        }
    }

    @Override
    public void setDebugModeEnabled(final boolean enabled) throws WasmException {
        validateNotClosed();

        try {
            nativeSetDebugModeEnabled(nativeHandle, enabled);
            LOGGER.log(Level.FINE, "Set debug mode enabled: {0}", enabled);
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set debug mode");
        }
    }

    @Override
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

    @Override
    public void setDebugOptions(final DebugOptions options) throws WasmException {
        Objects.requireNonNull(options, "options cannot be null");
        validateNotClosed();

        try {
            nativeSetDebugOptions(nativeHandle, options);
            LOGGER.log(Level.FINE, "Set debug options: {0}", options);
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to set debug options");
        }
    }

    @Override
    public DebugOptions getDebugOptions() {
        if (closed) {
            return DebugOptions.defaultOptions();
        }

        try {
            return nativeGetDebugOptions(nativeHandle);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get debug options", e);
            return DebugOptions.defaultOptions();
        }
    }

    @Override
    public boolean isValid() {
        return !closed && nativeHandle != 0 && nativeIsValidDebugger(nativeHandle);
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
            if (nativeHandle != 0) {
                nativeCloseDebugger(nativeHandle);
            }

            closed = true;
            LOGGER.log(Level.FINE, "Closed JNI debugger");
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error closing debugger", e);
            closed = true; // Mark as closed even on error
        }
    }

    // Package-private helper methods

    /**
     * Removes a session from the active sessions list.
     * Called by JniDebugSession when it's closed.
     */
    void removeSession(final DebugSession session) {
        activeSessions.remove(session);
    }

    /**
     * Gets the native debugger handle.
     */
    long getNativeHandle() {
        return nativeHandle;
    }

    // Private helper methods

    private void validateNotClosed() {
        if (closed) {
            throw new IllegalStateException("Debugger is closed");
        }
        if (nativeHandle == 0) {
            throw new IllegalStateException("Invalid native handle");
        }
    }

    private long extractEngineHandle(final Engine engine) {
        JniValidation.validateNotNull(engine, "engine");

        if (engine instanceof JniEngine) {
            return ((JniEngine) engine).getNativeHandle();
        } else {
            throw new IllegalArgumentException("Engine must be a JniEngine instance");
        }
    }

    private long extractInstanceHandle(final Instance instance) {
        JniValidation.validateNotNull(instance, "instance");

        if (instance instanceof JniInstance) {
            return ((JniInstance) instance).getNativeHandle();
        } else {
            throw new IllegalArgumentException("Instance must be a JniInstance instance");
        }
    }

    // Native method declarations
    // These would be implemented in the native Rust code

    private static native long nativeCreateDebugger(long engineHandle);

    private static native void nativeCloseDebugger(long debuggerHandle);

    private static native boolean nativeIsValidDebugger(long debuggerHandle);

    private static native DebugCapabilities nativeGetCapabilities(long debuggerHandle);

    private static native long nativeAttachToInstance(long debuggerHandle, long instanceHandle);

    private static native boolean nativeDetachFromInstance(long debuggerHandle, long instanceHandle);

    private static native List<DebugInfo> nativeGetDebugInfo(long debuggerHandle);

    private static native void nativeSetDebugModeEnabled(long debuggerHandle, boolean enabled);

    private static native boolean nativeIsDebugModeEnabled(long debuggerHandle);

    private static native void nativeSetDebugOptions(long debuggerHandle, DebugOptions options);

    private static native DebugOptions nativeGetDebugOptions(long debuggerHandle);

    // Static initialization
    static {
        try {
            // Ensure native library is loaded
            System.loadLibrary("wasmtime4j");
            LOGGER.log(Level.FINE, "Loaded native library for JNI debugger");
        } catch (final UnsatisfiedLinkError e) {
            LOGGER.log(Level.SEVERE, "Failed to load native library", e);
            throw new RuntimeException("Failed to load native debugging library", e);
        }
    }
}