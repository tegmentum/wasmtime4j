package ai.tegmentum.wasmtime4j.jni;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Module interface.
 *
 * <p>This class represents a compiled WebAssembly module and provides access to its metadata
 * and exports through JNI calls to the native Wasmtime library. A module contains the compiled
 * WebAssembly bytecode and can be instantiated multiple times.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and
 * JVM crashes.
 */
public final class JniModule implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(JniModule.class.getName());

    /** Native module handle. */
    private volatile long nativeHandle;

    /** Flag to track if this module has been closed. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new JNI module with the given native handle.
     *
     * @param nativeHandle the native module handle
     * @throws IllegalArgumentException if nativeHandle is 0
     */
    JniModule(final long nativeHandle) {
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Native handle cannot be 0");
        }
        this.nativeHandle = nativeHandle;
        LOGGER.fine("Created JNI module with handle: " + nativeHandle);
    }

    /**
     * Gets the names of all functions exported by this module.
     *
     * @return array of exported function names
     * @throws IllegalStateException if this module is closed
     * @throws RuntimeException if the exports cannot be retrieved
     */
    public String[] getExportedFunctions() {
        validateNotClosed();
        try {
            final String[] functions = nativeGetExportedFunctions(nativeHandle);
            return functions != null ? functions : new String[0];
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting exported functions", e);
        }
    }

    /**
     * Gets the names of all memories exported by this module.
     *
     * @return array of exported memory names
     * @throws IllegalStateException if this module is closed
     * @throws RuntimeException if the exports cannot be retrieved
     */
    public String[] getExportedMemories() {
        validateNotClosed();
        try {
            final String[] memories = nativeGetExportedMemories(nativeHandle);
            return memories != null ? memories : new String[0];
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting exported memories", e);
        }
    }

    /**
     * Gets the names of all tables exported by this module.
     *
     * @return array of exported table names
     * @throws IllegalStateException if this module is closed
     * @throws RuntimeException if the exports cannot be retrieved
     */
    public String[] getExportedTables() {
        validateNotClosed();
        try {
            final String[] tables = nativeGetExportedTables(nativeHandle);
            return tables != null ? tables : new String[0];
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting exported tables", e);
        }
    }

    /**
     * Gets the names of all globals exported by this module.
     *
     * @return array of exported global names
     * @throws IllegalStateException if this module is closed
     * @throws RuntimeException if the exports cannot be retrieved
     */
    public String[] getExportedGlobals() {
        validateNotClosed();
        try {
            final String[] globals = nativeGetExportedGlobals(nativeHandle);
            return globals != null ? globals : new String[0];
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting exported globals", e);
        }
    }

    /**
     * Gets the names of all functions imported by this module.
     *
     * @return array of imported function names in "module::name" format
     * @throws IllegalStateException if this module is closed
     * @throws RuntimeException if the imports cannot be retrieved
     */
    public String[] getImportedFunctions() {
        validateNotClosed();
        try {
            final String[] functions = nativeGetImportedFunctions(nativeHandle);
            return functions != null ? functions : new String[0];
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting imported functions", e);
        }
    }

    /**
     * Validates a module's bytecode without compiling it.
     *
     * @param bytecode the WebAssembly bytecode to validate
     * @return true if the bytecode is valid
     * @throws IllegalArgumentException if bytecode is null or empty
     */
    public static boolean validate(final byte[] bytecode) {
        if (bytecode == null || bytecode.length == 0) {
            throw new IllegalArgumentException("Bytecode cannot be null or empty");
        }

        try {
            return nativeValidateModule(bytecode);
        } catch (final Exception e) {
            LOGGER.warning("Error validating module: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the size of the compiled module in bytes.
     *
     * @return the module size in bytes
     * @throws IllegalStateException if this module is closed
     * @throws RuntimeException if the size cannot be retrieved
     */
    public long getSize() {
        validateNotClosed();
        try {
            return nativeGetModuleSize(nativeHandle);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting module size", e);
        }
    }

    /**
     * Gets the native handle for internal use.
     *
     * @return the native handle
     * @throws IllegalStateException if this module is closed
     */
    long getNativeHandle() {
        validateNotClosed();
        return nativeHandle;
    }

    /**
     * Closes this module and releases all associated native resources.
     *
     * <p>After calling this method, all operations on this module will throw
     * {@link IllegalStateException}. This method is idempotent.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (nativeHandle != 0) {
                try {
                    nativeDestroyModule(nativeHandle);
                    LOGGER.fine("Destroyed JNI module with handle: " + nativeHandle);
                } catch (final Exception e) {
                    LOGGER.warning("Error destroying native module: " + e.getMessage());
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
                LOGGER.warning("JniModule was finalized without being closed");
                close();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Validates that this module is not closed.
     *
     * @throws IllegalStateException if this module is closed
     */
    private void validateNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Module is closed");
        }
    }

    // Native method declarations

    /**
     * Gets the exported functions from a module.
     *
     * @param moduleHandle the native module handle
     * @return array of function names or null on error
     */
    private static native String[] nativeGetExportedFunctions(long moduleHandle);

    /**
     * Gets the exported memories from a module.
     *
     * @param moduleHandle the native module handle
     * @return array of memory names or null on error
     */
    private static native String[] nativeGetExportedMemories(long moduleHandle);

    /**
     * Gets the exported tables from a module.
     *
     * @param moduleHandle the native module handle
     * @return array of table names or null on error
     */
    private static native String[] nativeGetExportedTables(long moduleHandle);

    /**
     * Gets the exported globals from a module.
     *
     * @param moduleHandle the native module handle
     * @return array of global names or null on error
     */
    private static native String[] nativeGetExportedGlobals(long moduleHandle);

    /**
     * Gets the imported functions from a module.
     *
     * @param moduleHandle the native module handle
     * @return array of function names in "module::name" format or null on error
     */
    private static native String[] nativeGetImportedFunctions(long moduleHandle);

    /**
     * Validates WebAssembly bytecode.
     *
     * @param bytecode the bytecode to validate
     * @return true if valid, false otherwise
     */
    private static native boolean nativeValidateModule(byte[] bytecode);

    /**
     * Gets the size of a compiled module.
     *
     * @param moduleHandle the native module handle
     * @return the module size in bytes
     */
    private static native long nativeGetModuleSize(long moduleHandle);

    /**
     * Destroys a native module.
     *
     * @param moduleHandle the native module handle
     */
    private static native void nativeDestroyModule(long moduleHandle);
}