package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.List;

/**
 * Main interface for WebAssembly debugging operations.
 *
 * <p>The Debugger provides the primary entry point for creating debug sessions and managing
 * debugging operations across multiple WebAssembly instances. It supports both single-target
 * and multi-target debugging scenarios.
 *
 * <p>Example usage:
 * <pre>{@code
 * try (Debugger debugger = runtime.createDebugger(engine)) {
 *     DebugSession session = debugger.createSession(instance);
 *     session.setBreakpoint("main", 0);
 *     // Debug operations...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface Debugger extends Closeable {

    /**
     * Gets the engine associated with this debugger.
     *
     * @return the engine
     */
    Engine getEngine();

    /**
     * Creates a new debug session for a single instance.
     *
     * @param instance the instance to debug
     * @return a new debug session
     * @throws WasmException if session cannot be created
     * @throws IllegalArgumentException if instance is null
     */
    DebugSession createSession(final Instance instance) throws WasmException;

    /**
     * Creates a new debug session for multiple instances.
     *
     * @param instances the instances to debug
     * @return a new debug session
     * @throws WasmException if session cannot be created
     * @throws IllegalArgumentException if instances is null or empty
     */
    DebugSession createSession(final List<Instance> instances) throws WasmException;

    /**
     * Creates a new debug session with custom configuration.
     *
     * @param instance the instance to debug
     * @param config the debug configuration
     * @return a new debug session
     * @throws WasmException if session cannot be created
     * @throws IllegalArgumentException if instance or config is null
     */
    DebugSession createSession(final Instance instance, final DebugConfig config) throws WasmException;

    /**
     * Gets all active debug sessions.
     *
     * @return list of active sessions
     */
    List<DebugSession> getActiveSessions();

    /**
     * Closes a specific debug session.
     *
     * @param session the session to close
     * @return true if the session was closed
     * @throws IllegalArgumentException if session is null
     */
    boolean closeSession(final DebugSession session);

    /**
     * Closes all active debug sessions.
     *
     * @return number of sessions closed
     */
    int closeAllSessions();

    /**
     * Gets debugging capabilities supported by this debugger.
     *
     * @return debugging capabilities
     */
    DebugCapabilities getCapabilities();

    /**
     * Attaches to a running WebAssembly instance for debugging.
     *
     * @param instance the instance to attach to
     * @return a debug session for the attached instance
     * @throws WasmException if attachment fails
     * @throws IllegalArgumentException if instance is null
     */
    DebugSession attach(final Instance instance) throws WasmException;

    /**
     * Detaches from a WebAssembly instance.
     *
     * @param instance the instance to detach from
     * @return true if detachment was successful
     * @throws WasmException if detachment fails
     * @throws IllegalArgumentException if instance is null
     */
    boolean detach(final Instance instance) throws WasmException;

    /**
     * Gets debug information for all instances managed by this debugger.
     *
     * @return list of debug information
     */
    List<DebugInfo> getDebugInfo();

    /**
     * Enables or disables global debug mode.
     *
     * @param enabled true to enable debug mode
     * @throws WasmException if debug mode cannot be changed
     */
    void setDebugModeEnabled(final boolean enabled) throws WasmException;

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is enabled
     */
    boolean isDebugModeEnabled();

    /**
     * Sets global debugging options.
     *
     * @param options the debug options
     * @throws WasmException if options cannot be set
     * @throws IllegalArgumentException if options is null
     */
    void setDebugOptions(final DebugOptions options) throws WasmException;

    /**
     * Gets current debugging options.
     *
     * @return current debug options
     */
    DebugOptions getDebugOptions();

    /**
     * Checks if the debugger is still valid.
     *
     * @return true if the debugger is valid
     */
    boolean isValid();

    /**
     * Closes the debugger and all associated sessions.
     */
    @Override
    void close();
}