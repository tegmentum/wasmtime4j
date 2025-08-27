package ai.tegmentum.wasmtime4j.jni;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Function interface.
 *
 * <p>This class provides access to WebAssembly functions through JNI calls to the native
 * Wasmtime library. It supports calling functions with various parameter and return types.
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive type checking for function calls.
 */
public final class JniFunction implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(JniFunction.class.getName());

    /** Native function handle. */
    private volatile long nativeHandle;

    /** Function name for debugging. */
    private final String name;

    /** Flag to track if this function has been closed. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new JNI function with the given native handle and name.
     *
     * @param nativeHandle the native function handle
     * @param name the function name
     * @throws IllegalArgumentException if nativeHandle is 0 or name is null
     */
    JniFunction(final long nativeHandle, final String name) {
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Native handle cannot be 0");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.nativeHandle = nativeHandle;
        this.name = name;
        LOGGER.fine("Created JNI function '" + name + "' with handle: " + nativeHandle);
    }

    /**
     * Gets the name of this function.
     *
     * @return the function name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the parameter types for this function.
     *
     * @return array of parameter type names
     * @throws IllegalStateException if this function is closed
     * @throws RuntimeException if the types cannot be retrieved
     */
    public String[] getParameterTypes() {
        validateNotClosed();
        try {
            final String[] types = nativeGetParameterTypes(nativeHandle);
            return types != null ? types : new String[0];
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting parameter types", e);
        }
    }

    /**
     * Gets the return types for this function.
     *
     * @return array of return type names
     * @throws IllegalStateException if this function is closed
     * @throws RuntimeException if the types cannot be retrieved
     */
    public String[] getReturnTypes() {
        validateNotClosed();
        try {
            final String[] types = nativeGetReturnTypes(nativeHandle);
            return types != null ? types : new String[0];
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting return types", e);
        }
    }

    /**
     * Calls this function with no parameters.
     *
     * @return the return value (null if function returns void)
     * @throws IllegalStateException if this function is closed
     * @throws RuntimeException if the call fails or types don't match
     */
    public Object call() {
        return call(new Object[0]);
    }

    /**
     * Calls this function with the given parameters.
     *
     * @param parameters the function parameters
     * @return the return value (null if function returns void)
     * @throws IllegalArgumentException if parameters is null
     * @throws IllegalStateException if this function is closed
     * @throws RuntimeException if the call fails or types don't match
     */
    public Object call(final Object... parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        validateNotClosed();

        try {
            return nativeCall(nativeHandle, parameters);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
        }
    }

    /**
     * Calls this function with integer parameters (optimized path).
     *
     * @param parameters the integer parameters
     * @return the return value as an integer (0 if function returns void)
     * @throws IllegalArgumentException if parameters is null
     * @throws IllegalStateException if this function is closed
     * @throws RuntimeException if the call fails or types don't match
     */
    public int callInt(final int... parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        validateNotClosed();

        try {
            return nativeCallInt(nativeHandle, parameters);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
        }
    }

    /**
     * Calls this function with long parameters (optimized path).
     *
     * @param parameters the long parameters
     * @return the return value as a long (0 if function returns void)
     * @throws IllegalArgumentException if parameters is null
     * @throws IllegalStateException if this function is closed
     * @throws RuntimeException if the call fails or types don't match
     */
    public long callLong(final long... parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        validateNotClosed();

        try {
            return nativeCallLong(nativeHandle, parameters);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
        }
    }

    /**
     * Calls this function with float parameters (optimized path).
     *
     * @param parameters the float parameters
     * @return the return value as a float (0.0 if function returns void)
     * @throws IllegalArgumentException if parameters is null
     * @throws IllegalStateException if this function is closed
     * @throws RuntimeException if the call fails or types don't match
     */
    public float callFloat(final float... parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        validateNotClosed();

        try {
            return nativeCallFloat(nativeHandle, parameters);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
        }
    }

    /**
     * Calls this function with double parameters (optimized path).
     *
     * @param parameters the double parameters
     * @return the return value as a double (0.0 if function returns void)
     * @throws IllegalArgumentException if parameters is null
     * @throws IllegalStateException if this function is closed
     * @throws RuntimeException if the call fails or types don't match
     */
    public double callDouble(final double... parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        validateNotClosed();

        try {
            return nativeCallDouble(nativeHandle, parameters);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
        }
    }

    /**
     * Gets the native handle for internal use.
     *
     * @return the native handle
     * @throws IllegalStateException if this function is closed
     */
    long getNativeHandle() {
        validateNotClosed();
        return nativeHandle;
    }

    /**
     * Closes this function and releases all associated native resources.
     *
     * <p>After calling this method, all operations on this function will throw
     * {@link IllegalStateException}. This method is idempotent.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (nativeHandle != 0) {
                try {
                    nativeDestroyFunction(nativeHandle);
                    LOGGER.fine("Destroyed JNI function '" + name + "' with handle: " + nativeHandle);
                } catch (final Exception e) {
                    LOGGER.warning("Error destroying native function: " + e.getMessage());
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
                LOGGER.warning("JniFunction '" + name + "' was finalized without being closed");
                close();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Validates that this function is not closed.
     *
     * @throws IllegalStateException if this function is closed
     */
    private void validateNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Function '" + name + "' is closed");
        }
    }

    // Native method declarations

    /**
     * Gets the parameter types for a function.
     *
     * @param functionHandle the native function handle
     * @return array of parameter type names or null on error
     */
    private static native String[] nativeGetParameterTypes(long functionHandle);

    /**
     * Gets the return types for a function.
     *
     * @param functionHandle the native function handle
     * @return array of return type names or null on error
     */
    private static native String[] nativeGetReturnTypes(long functionHandle);

    /**
     * Calls a function with generic parameters.
     *
     * @param functionHandle the native function handle
     * @param parameters the function parameters
     * @return the return value or null
     */
    private static native Object nativeCall(long functionHandle, Object[] parameters);

    /**
     * Calls a function with integer parameters (optimized).
     *
     * @param functionHandle the native function handle
     * @param parameters the integer parameters
     * @return the return value as an integer
     */
    private static native int nativeCallInt(long functionHandle, int[] parameters);

    /**
     * Calls a function with long parameters (optimized).
     *
     * @param functionHandle the native function handle
     * @param parameters the long parameters
     * @return the return value as a long
     */
    private static native long nativeCallLong(long functionHandle, long[] parameters);

    /**
     * Calls a function with float parameters (optimized).
     *
     * @param functionHandle the native function handle
     * @param parameters the float parameters
     * @return the return value as a float
     */
    private static native float nativeCallFloat(long functionHandle, float[] parameters);

    /**
     * Calls a function with double parameters (optimized).
     *
     * @param functionHandle the native function handle
     * @param parameters the double parameters
     * @return the return value as a double
     */
    private static native double nativeCallDouble(long functionHandle, double[] parameters);

    /**
     * Destroys a native function.
     *
     * @param functionHandle the native function handle
     */
    private static native void nativeDestroyFunction(long functionHandle);
}