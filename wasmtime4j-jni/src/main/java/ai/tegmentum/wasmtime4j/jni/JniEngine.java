package ai.tegmentum.wasmtime4j.jni;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Engine interface.
 *
 * <p>This class manages the configuration and lifecycle of a WebAssembly execution engine through
 * JNI calls to the native Wasmtime library. The engine controls compilation settings, optimization
 * levels, and resource management for WebAssembly modules.
 *
 * <p>This implementation provides defensive programming to prevent native resource leaks and JVM
 * crashes.
 */
public final class JniEngine implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(JniEngine.class.getName());

    /** Native engine handle. */
    private volatile long nativeHandle;

    /** Flag to track if this engine has been closed. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new JNI engine with the given native handle.
     *
     * @param nativeHandle the native engine handle
     * @throws IllegalArgumentException if nativeHandle is 0
     */
    JniEngine(final long nativeHandle) {
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Native handle cannot be 0");
        }
        this.nativeHandle = nativeHandle;
        LOGGER.fine("Created JNI engine with handle: " + nativeHandle);
    }

    /**
     * Creates a new instance of a compiled module using this engine.
     *
     * @param module the compiled module to instantiate
     * @return a new module instance
     * @throws IllegalArgumentException if module is null
     * @throws IllegalStateException if this engine is closed
     * @throws RuntimeException if instantiation fails
     */
    public JniInstance instantiate(final JniModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        validateNotClosed();

        try {
            final long instanceHandle = nativeInstantiate(nativeHandle, module.getNativeHandle());
            if (instanceHandle == 0) {
                throw new RuntimeException("Failed to instantiate module");
            }
            return new JniInstance(instanceHandle);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error instantiating module", e);
        }
    }

    /**
     * Sets the optimization level for this engine.
     *
     * @param level the optimization level (0 = none, 1 = speed, 2 = speed + size)
     * @throws IllegalArgumentException if level is not valid (0-2)
     * @throws IllegalStateException if this engine is closed
     * @throws RuntimeException if the configuration cannot be changed
     */
    public void setOptimizationLevel(final int level) {
        if (level < 0 || level > 2) {
            throw new IllegalArgumentException("Optimization level must be 0, 1, or 2");
        }
        validateNotClosed();

        try {
            final boolean success = nativeSetOptimizationLevel(nativeHandle, level);
            if (!success) {
                throw new RuntimeException("Failed to set optimization level");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error setting optimization level", e);
        }
    }

    /**
     * Gets the current optimization level for this engine.
     *
     * @return the optimization level (0-2)
     * @throws IllegalStateException if this engine is closed
     * @throws RuntimeException if the configuration cannot be retrieved
     */
    public int getOptimizationLevel() {
        validateNotClosed();
        try {
            return nativeGetOptimizationLevel(nativeHandle);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting optimization level", e);
        }
    }

    /**
     * Enables or disables debug information generation.
     *
     * @param enabled true to enable debug information
     * @throws IllegalStateException if this engine is closed
     * @throws RuntimeException if the configuration cannot be changed
     */
    public void setDebugInfo(final boolean enabled) {
        validateNotClosed();
        try {
            final boolean success = nativeSetDebugInfo(nativeHandle, enabled);
            if (!success) {
                throw new RuntimeException("Failed to set debug info configuration");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error setting debug info", e);
        }
    }

    /**
     * Checks if debug information generation is enabled.
     *
     * @return true if debug information is enabled
     * @throws IllegalStateException if this engine is closed
     * @throws RuntimeException if the configuration cannot be retrieved
     */
    public boolean isDebugInfo() {
        validateNotClosed();
        try {
            return nativeIsDebugInfo(nativeHandle);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting debug info configuration", e);
        }
    }

    /**
     * Gets the native handle for internal use.
     *
     * @return the native handle
     * @throws IllegalStateException if this engine is closed
     */
    long getNativeHandle() {
        validateNotClosed();
        return nativeHandle;
    }

    /**
     * Closes this engine and releases all associated native resources.
     *
     * <p>After calling this method, all operations on this engine will throw
     * {@link IllegalStateException}. This method is idempotent.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (nativeHandle != 0) {
                try {
                    nativeDestroyEngine(nativeHandle);
                    LOGGER.fine("Destroyed JNI engine with handle: " + nativeHandle);
                } catch (final Exception e) {
                    LOGGER.warning("Error destroying native engine: " + e.getMessage());
                } finally {
                    nativeHandle = 0;
                }
            }
        }
    }

    /**
     * Finalizer to ensure native resources are released if close() wasn't called.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!closed.get()) {
                LOGGER.warning("JniEngine was finalized without being closed");
                close();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Validates that this engine is not closed.
     *
     * @throws IllegalStateException if this engine is closed
     */
    private void validateNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Engine is closed");
        }
    }

    // Native method declarations

    /**
     * Instantiates a module using this engine.
     *
     * @param engineHandle the native engine handle
     * @param moduleHandle the native module handle
     * @return native instance handle or 0 on failure
     */
    private static native long nativeInstantiate(long engineHandle, long moduleHandle);

    /**
     * Sets the optimization level for an engine.
     *
     * @param engineHandle the native engine handle
     * @param level the optimization level (0-2)
     * @return true on success, false on failure
     */
    private static native boolean nativeSetOptimizationLevel(long engineHandle, int level);

    /**
     * Gets the optimization level for an engine.
     *
     * @param engineHandle the native engine handle
     * @return the optimization level (0-2)
     */
    private static native int nativeGetOptimizationLevel(long engineHandle);

    /**
     * Enables or disables debug information generation.
     *
     * @param engineHandle the native engine handle
     * @param enabled true to enable debug information
     * @return true on success, false on failure
     */
    private static native boolean nativeSetDebugInfo(long engineHandle, boolean enabled);

    /**
     * Checks if debug information generation is enabled.
     *
     * @param engineHandle the native engine handle
     * @return true if debug information is enabled
     */
    private static native boolean nativeIsDebugInfo(long engineHandle);

    /**
     * Destroys a native engine.
     *
     * @param engineHandle the native engine handle
     */
    private static native void nativeDestroyEngine(long engineHandle);
}