package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the Table interface.
 *
 * <p>This class provides access to WebAssembly tables through JNI calls to the native Wasmtime
 * library. Tables store references to functions or other objects that can be called indirectly.
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive bounds checking for table access using JniValidation and the JniResource base class.
 */
public final class JniTable extends JniResource {

  private static final Logger LOGGER = Logger.getLogger(JniTable.class.getName());

  /**
   * Creates a new JNI table with the given native handle.
   *
   * @param nativeHandle the native table handle
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniTable(final long nativeHandle) {
    super(nativeHandle);
    LOGGER.fine("Created JNI table with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Gets the current size of the table.
   *
   * @return the number of elements in the table
   * @throws JniResourceException if this table is closed
   * @throws RuntimeException if the size cannot be retrieved
   */
  public int getSize() {
    try {
      return nativeGetSize(getNativeHandle());
    } catch (final JniResourceException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting table size", e);
    }
  }

  /**
   * Gets the maximum size of the table.
   *
   * @return the maximum number of elements, or -1 if unlimited
   * @throws JniResourceException if this table is closed
   * @throws RuntimeException if the max size cannot be retrieved
   */
  public int getMaxSize() {
    try {
      return nativeGetMaxSize(getNativeHandle());
    } catch (final JniResourceException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting table max size", e);
    }
  }

  /**
   * Gets the element type of this table.
   *
   * @return the element type name (e.g., "funcref", "externref")
   * @throws JniResourceException if this table is closed
   * @throws RuntimeException if the type cannot be retrieved
   */
  public String getElementType() {
    try {
      final String type = nativeGetElementType(getNativeHandle());
      return type != null ? type : "unknown";
    } catch (final JniResourceException e) {
      throw e;
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
   * @throws JniResourceException if this table is closed
   * @throws IndexOutOfBoundsException if index is beyond table bounds
   * @throws RuntimeException if the element cannot be retrieved
   */
  public Object get(final int index) {
    JniValidation.requireNonNegative(index, "index");
    
    final long handle = getNativeHandle(); // This validates not closed
    validateIndex(index);

    try {
      return nativeGet(handle, index);
    } catch (final JniResourceException | IllegalArgumentException | IndexOutOfBoundsException e) {
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
   * @throws JniResourceException if this table is closed
   * @throws IndexOutOfBoundsException if index is beyond table bounds
   * @throws RuntimeException if the element cannot be set
   */
  public void set(final int index, final Object value) {
    JniValidation.requireNonNegative(index, "index");
    
    final long handle = getNativeHandle(); // This validates not closed
    validateIndex(index);

    try {
      final boolean success = nativeSet(handle, index, value);
      if (!success) {
        throw new RuntimeException("Failed to set table element");
      }
    } catch (final JniResourceException | IllegalArgumentException | IndexOutOfBoundsException e) {
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
   * @throws JniResourceException if this table is closed
   * @throws RuntimeException if the growth operation fails
   */
  public int grow(final int delta, final Object init) {
    JniValidation.requireNonNegative(delta, "delta");

    try {
      return nativeGrow(getNativeHandle(), delta, init);
    } catch (final JniResourceException | IllegalArgumentException e) {
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
   * @throws JniResourceException if this table is closed
   * @throws IndexOutOfBoundsException if the range exceeds table bounds
   * @throws RuntimeException if the fill operation fails
   */
  public void fill(final int start, final int count, final Object value) {
    JniValidation.requireNonNegative(start, "start");
    JniValidation.requireNonNegative(count, "count");
    
    final long handle = getNativeHandle(); // This validates not closed
    validateRange(start, count);

    try {
      final boolean success = nativeFill(handle, start, count, value);
      if (!success) {
        throw new RuntimeException("Failed to fill table range");
      }
    } catch (final JniResourceException | IllegalArgumentException | IndexOutOfBoundsException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error filling table", e);
    }
  }


  /**
   * Validates that an index is within table bounds.
   *
   * @param index the index to validate
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  private void validateIndex(final int index) {
    final int tableSize = getSize();
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
    final int tableSize = getSize();
    if (start + count > tableSize) {
      throw new IndexOutOfBoundsException(
          "Range [" + start + ", " + (start + count) + ") exceeds table size " + tableSize);
    }
  }

  @Override
  protected void doClose() throws Exception {
    nativeDestroyTable(nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "Table";
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
   * Gets the maximum size of a table.
   *
   * @param tableHandle the native table handle
   * @return the maximum number of elements, or -1 if unlimited
   */
  private static native int nativeGetMaxSize(long tableHandle);

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
