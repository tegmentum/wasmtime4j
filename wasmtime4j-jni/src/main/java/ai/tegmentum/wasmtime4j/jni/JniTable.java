package ai.tegmentum.wasmtime4j.jni;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Table interface.
 *
 * <p>This class provides access to WebAssembly tables through JNI calls to the native Wasmtime
 * library. Tables store references to functions or other objects that can be called indirectly.
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive bounds checking for table access.
 */
public final class JniTable implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(JniTable.class.getName());

    /** Native table handle. */
    private volatile long nativeHandle;

    /** Flag to track if this table has been closed. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new JNI table with the given native handle.
     *
     * @param nativeHandle the native table handle
     * @throws IllegalArgumentException if nativeHandle is 0
     */
    JniTable(final long nativeHandle) {
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Native handle cannot be 0");
        }
        this.nativeHandle = nativeHandle;
        LOGGER.fine("Created JNI table with handle: " + nativeHandle);
    }

    /**
     * Gets the current size of the table.
     *
     * @return the number of elements in the table
     * @throws IllegalStateException if this table is closed
     * @throws RuntimeException if the size cannot be retrieved
     */
    public int size() {
        validateNotClosed();
        try {
            return nativeGetSize(nativeHandle);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting table size", e);
        }
    }

    /**
     * Gets the element type of this table.
     *
     * @return the element type name (e.g., "funcref", "externref")
     * @throws IllegalStateException if this table is closed
     * @throws RuntimeException if the type cannot be retrieved
     */
    public String getElementType() {
        validateNotClosed();
        try {
            final String type = nativeGetElementType(nativeHandle);
            return type != null ? type : "unknown";
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting table element type", e);
        }
    }

    /**
     * Gets an element from the table at the specified index.
     *
     * @param index the table index
     * @return the element at the index (may be null for uninitialized slots)
     * @throws IllegalArgumentException if index is negative
     * @throws IllegalStateException if this table is closed
     * @throws IndexOutOfBoundsException if index is beyond table bounds
     * @throws RuntimeException if the element cannot be retrieved
     */
    public Object get(final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative");
        }
        validateNotClosed();
        validateIndex(index);

        try {
            return nativeGet(nativeHandle, index);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error getting table element", e);
        }
    }

    /**
     * Sets an element in the table at the specified index.
     *
     * @param index the table index
     * @param value the value to set (must be compatible with element type)
     * @throws IllegalArgumentException if index is negative
     * @throws IllegalStateException if this table is closed
     * @throws IndexOutOfBoundsException if index is beyond table bounds
     * @throws RuntimeException if the element cannot be set
     */
    public void set(final int index, final Object value) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative");
        }
        validateNotClosed();
        validateIndex(index);

        try {
            final boolean success = nativeSet(nativeHandle, index, value);
            if (!success) {
                throw new RuntimeException("Failed to set table element");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error setting table element", e);
        }
    }

    /**
     * Grows the table by the specified number of elements.
     *
     * @param delta the number of elements to add
     * @param init the initial value for new elements (may be null)
     * @return the previous size of the table, or -1 if growth failed
     * @throws IllegalArgumentException if delta is negative
     * @throws IllegalStateException if this table is closed
     * @throws RuntimeException if the growth operation fails
     */
    public int grow(final int delta, final Object init) {
        if (delta < 0) {
            throw new IllegalArgumentException("Delta must be non-negative");
        }
        validateNotClosed();

        try {
            return nativeGrow(nativeHandle, delta, init);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error growing table", e);
        }
    }

    /**
     * Fills a range of the table with the specified value.
     *
     * @param start the starting index
     * @param count the number of elements to fill
     * @param value the value to fill with
     * @throws IllegalArgumentException if start or count is negative
     * @throws IllegalStateException if this table is closed
     * @throws IndexOutOfBoundsException if the range exceeds table bounds
     * @throws RuntimeException if the fill operation fails
     */
    public void fill(final int start, final int count, final Object value) {
        if (start < 0) {
            throw new IllegalArgumentException("Start must be non-negative");
        }
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative");
        }
        validateNotClosed();
        validateRange(start, count);

        try {
            final boolean success = nativeFill(nativeHandle, start, count, value);
            if (!success) {
                throw new RuntimeException("Failed to fill table range");
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error filling table", e);
        }
    }

    /**
     * Gets the native handle for internal use.
     *
     * @return the native handle
     * @throws IllegalStateException if this table is closed
     */
    long getNativeHandle() {
        validateNotClosed();
        return nativeHandle;
    }

    /**
     * Validates that an index is within table bounds.
     *
     * @param index the index to validate
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    private void validateIndex(final int index) {
        final int tableSize = size();
        if (index >= tableSize) {
            throw new IndexOutOfBoundsException("Index " + index + " exceeds table size " + tableSize);
        }
    }

    /**
     * Validates that a range is within table bounds.
     *
     * @param start the starting index
     * @param count the number of elements
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    private void validateRange(final int start, final int count) {
        final int tableSize = size();
        if (start + count > tableSize) {
            throw new IndexOutOfBoundsException(
                    "Range [" + start + ", " + (start + count) + ") exceeds table size " + tableSize);
        }
    }

    /**
     * Closes this table and releases all associated native resources.
     *
     * <p>After calling this method, all operations on this table will throw
     * {@link IllegalStateException}. This method is idempotent.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (nativeHandle != 0) {
                try {
                    nativeDestroyTable(nativeHandle);
                    LOGGER.fine("Destroyed JNI table with handle: " + nativeHandle);
                } catch (final Exception e) {
                    LOGGER.warning("Error destroying native table: " + e.getMessage());
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
                LOGGER.warning("JniTable was finalized without being closed");
                close();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Validates that this table is not closed.
     *
     * @throws IllegalStateException if this table is closed
     */
    private void validateNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Table is closed");
        }
    }

    // Native method declarations

    /**
     * Gets the size of a table.
     *
     * @param tableHandle the native table handle
     * @return the number of elements in the table
     */
    private static native int nativeGetSize(long tableHandle);

    /**
     * Gets the element type of a table.
     *
     * @param tableHandle the native table handle
     * @return the element type name or null on error
     */
    private static native String nativeGetElementType(long tableHandle);

    /**
     * Gets an element from a table.
     *
     * @param tableHandle the native table handle
     * @param index the table index
     * @return the element at the index or null
     */
    private static native Object nativeGet(long tableHandle, int index);

    /**
     * Sets an element in a table.
     *
     * @param tableHandle the native table handle
     * @param index the table index
     * @param value the value to set
     * @return true on success, false on failure
     */
    private static native boolean nativeSet(long tableHandle, int index, Object value);

    /**
     * Grows a table by the specified number of elements.
     *
     * @param tableHandle the native table handle
     * @param delta the number of elements to add
     * @param init the initial value for new elements
     * @return the previous size or -1 on failure
     */
    private static native int nativeGrow(long tableHandle, int delta, Object init);

    /**
     * Fills a range of a table with the specified value.
     *
     * @param tableHandle the native table handle
     * @param start the starting index
     * @param count the number of elements to fill
     * @param value the value to fill with
     * @return true on success, false on failure
     */
    private static native boolean nativeFill(long tableHandle, int start, int count, Object value);

    /**
     * Destroys a native table.
     *
     * @param tableHandle the native table handle
     */
    private static native void nativeDestroyTable(long tableHandle);
}