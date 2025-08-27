package ai.tegmentum.wasmtime4j.jni;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Global interface.
 *
 * <p>This class provides access to WebAssembly global variables through JNI calls to the native
 * Wasmtime library. Globals can store various value types and may be mutable or immutable.
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive type checking for global variable access.
 */
public final class JniGlobal implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(JniGlobal.class.getName());

    /** Native global handle. */
    private volatile long nativeHandle;

    /** Flag to track if this global has been closed. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new JNI global with the given native handle.
     *
     * @param nativeHandle the native global handle
     * @throws IllegalArgumentException if nativeHandle is 0
     */
    JniGlobal(final long nativeHandle) {
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Native handle cannot be 0");
        }
        this.nativeHandle = nativeHandle;
        LOGGER.fine("Created JNI global with handle: " + nativeHandle);
    }

    /**
     * Gets the value type of this global.
     *
     * @return the value type name (e.g., "i32", "i64", "f32", "f64")
     * @throws IllegalStateException if this global is closed
     * @throws RuntimeException if the type cannot be retrieved
     */
    public String getValueType() {
        validateNotClosed();
        try {
            final String type = nativeGetValueType(nativeHandle);
            return type != null ? type : "unknown";
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting global value type", e);
        }
    }

    /**
     * Checks if this global is mutable.
     *
     * @return true if the global is mutable, false if immutable
     * @throws IllegalStateException if this global is closed
     * @throws RuntimeException if the mutability cannot be determined
     */
    public boolean isMutable() {
        validateNotClosed();
        try {
            return nativeIsMutable(nativeHandle);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error checking global mutability", e);
        }
    }

    /**
     * Gets the current value of this global as a generic Object.
     *
     * @return the global value (Integer, Long, Float, or Double)
     * @throws IllegalStateException if this global is closed
     * @throws RuntimeException if the value cannot be retrieved
     */
    public Object getValue() {
        validateNotClosed();
        try {
            return nativeGetValue(nativeHandle);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting global value", e);
        }
    }

    /**
     * Gets the current value of this global as an integer.
     *
     * @return the global value as an integer
     * @throws IllegalStateException if this global is closed
     * @throws RuntimeException if the value cannot be retrieved or is not an integer
     */
    public int getIntValue() {
        validateNotClosed();
        try {
            return nativeGetIntValue(nativeHandle);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting global int value", e);
        }
    }

    /**
     * Gets the current value of this global as a long.
     *
     * @return the global value as a long
     * @throws IllegalStateException if this global is closed
     * @throws RuntimeException if the value cannot be retrieved or is not a long
     */
    public long getLongValue() {
        validateNotClosed();
        try {
            return nativeGetLongValue(nativeHandle);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting global long value", e);
        }
    }

    /**
     * Gets the current value of this global as a float.
     *
     * @return the global value as a float
     * @throws IllegalStateException if this global is closed
     * @throws RuntimeException if the value cannot be retrieved or is not a float
     */
    public float getFloatValue() {
        validateNotClosed();
        try {
            return nativeGetFloatValue(nativeHandle);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting global float value", e);
        }
    }

    /**
     * Gets the current value of this global as a double.
     *
     * @return the global value as a double
     * @throws IllegalStateException if this global is closed
     * @throws RuntimeException if the value cannot be retrieved or is not a double
     */
    public double getDoubleValue() {
        validateNotClosed();
        try {
            return nativeGetDoubleValue(nativeHandle);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting global double value", e);
        }
    }

    /**
     * Sets the value of this global (only if mutable).
     *
     * @param value the new value (must match the global's type)
     * @throws IllegalArgumentException if value is null or wrong type
     * @throws IllegalStateException if this global is closed or immutable
     * @throws RuntimeException if the value cannot be set
     */
    public void setValue(final Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        validateNotClosed();
        validateMutable();

        try {
            final boolean success = nativeSetValue(nativeHandle, value);
            if (!success) {
                throw new RuntimeException("Failed to set global value");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error setting global value", e);
        }
    }

    /**
     * Sets the value of this global to an integer (only if mutable and compatible type).
     *
     * @param value the new integer value
     * @throws IllegalStateException if this global is closed or immutable
     * @throws RuntimeException if the value cannot be set or type is incompatible
     */
    public void setIntValue(final int value) {
        validateNotClosed();
        validateMutable();

        try {
            final boolean success = nativeSetIntValue(nativeHandle, value);
            if (!success) {
                throw new RuntimeException("Failed to set global int value");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error setting global int value", e);
        }
    }

    /**
     * Sets the value of this global to a long (only if mutable and compatible type).
     *
     * @param value the new long value
     * @throws IllegalStateException if this global is closed or immutable
     * @throws RuntimeException if the value cannot be set or type is incompatible
     */
    public void setLongValue(final long value) {
        validateNotClosed();
        validateMutable();

        try {
            final boolean success = nativeSetLongValue(nativeHandle, value);
            if (!success) {
                throw new RuntimeException("Failed to set global long value");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error setting global long value", e);
        }
    }

    /**
     * Sets the value of this global to a float (only if mutable and compatible type).
     *
     * @param value the new float value
     * @throws IllegalStateException if this global is closed or immutable
     * @throws RuntimeException if the value cannot be set or type is incompatible
     */
    public void setFloatValue(final float value) {
        validateNotClosed();
        validateMutable();

        try {
            final boolean success = nativeSetFloatValue(nativeHandle, value);
            if (!success) {
                throw new RuntimeException("Failed to set global float value");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error setting global float value", e);
        }
    }

    /**
     * Sets the value of this global to a double (only if mutable and compatible type).
     *
     * @param value the new double value
     * @throws IllegalStateException if this global is closed or immutable
     * @throws RuntimeException if the value cannot be set or type is incompatible
     */
    public void setDoubleValue(final double value) {
        validateNotClosed();
        validateMutable();

        try {
            final boolean success = nativeSetDoubleValue(nativeHandle, value);
            if (!success) {
                throw new RuntimeException("Failed to set global double value");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error setting global double value", e);
        }
    }

    /**
     * Gets the native handle for internal use.
     *
     * @return the native handle
     * @throws IllegalStateException if this global is closed
     */
    long getNativeHandle() {
        validateNotClosed();
        return nativeHandle;
    }

    /**
     * Validates that this global is mutable.
     *
     * @throws IllegalStateException if this global is immutable
     */
    private void validateMutable() {
        if (!isMutable()) {
            throw new IllegalStateException("Global is immutable");
        }
    }

    /**
     * Closes this global and releases all associated native resources.
     *
     * <p>After calling this method, all operations on this global will throw
     * {@link IllegalStateException}. This method is idempotent.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (nativeHandle != 0) {
                try {
                    nativeDestroyGlobal(nativeHandle);
                    LOGGER.fine("Destroyed JNI global with handle: " + nativeHandle);
                } catch (final Exception e) {
                    LOGGER.warning("Error destroying native global: " + e.getMessage());
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
                LOGGER.warning("JniGlobal was finalized without being closed");
                close();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Validates that this global is not closed.
     *
     * @throws IllegalStateException if this global is closed
     */
    private void validateNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Global is closed");
        }
    }

    // Native method declarations

    /**
     * Gets the value type of a global.
     *
     * @param globalHandle the native global handle
     * @return the value type name or null on error
     */
    private static native String nativeGetValueType(long globalHandle);

    /**
     * Checks if a global is mutable.
     *
     * @param globalHandle the native global handle
     * @return true if mutable, false if immutable
     */
    private static native boolean nativeIsMutable(long globalHandle);

    /**
     * Gets the value of a global as a generic Object.
     *
     * @param globalHandle the native global handle
     * @return the global value or null on error
     */
    private static native Object nativeGetValue(long globalHandle);

    /**
     * Gets the value of a global as an integer.
     *
     * @param globalHandle the native global handle
     * @return the global value as an integer
     */
    private static native int nativeGetIntValue(long globalHandle);

    /**
     * Gets the value of a global as a long.
     *
     * @param globalHandle the native global handle
     * @return the global value as a long
     */
    private static native long nativeGetLongValue(long globalHandle);

    /**
     * Gets the value of a global as a float.
     *
     * @param globalHandle the native global handle
     * @return the global value as a float
     */
    private static native float nativeGetFloatValue(long globalHandle);

    /**
     * Gets the value of a global as a double.
     *
     * @param globalHandle the native global handle
     * @return the global value as a double
     */
    private static native double nativeGetDoubleValue(long globalHandle);

    /**
     * Sets the value of a global with a generic Object.
     *
     * @param globalHandle the native global handle
     * @param value the new value
     * @return true on success, false on failure
     */
    private static native boolean nativeSetValue(long globalHandle, Object value);

    /**
     * Sets the value of a global to an integer.
     *
     * @param globalHandle the native global handle
     * @param value the new integer value
     * @return true on success, false on failure
     */
    private static native boolean nativeSetIntValue(long globalHandle, int value);

    /**
     * Sets the value of a global to a long.
     *
     * @param globalHandle the native global handle
     * @param value the new long value
     * @return true on success, false on failure
     */
    private static native boolean nativeSetLongValue(long globalHandle, long value);

    /**
     * Sets the value of a global to a float.
     *
     * @param globalHandle the native global handle
     * @param value the new float value
     * @return true on success, false on failure
     */
    private static native boolean nativeSetFloatValue(long globalHandle, float value);

    /**
     * Sets the value of a global to a double.
     *
     * @param globalHandle the native global handle
     * @param value the new double value
     * @return true on success, false on failure
     */
    private static native boolean nativeSetDoubleValue(long globalHandle, double value);

    /**
     * Destroys a native global.
     *
     * @param globalHandle the native global handle
     */
    private static native void nativeDestroyGlobal(long globalHandle);
}