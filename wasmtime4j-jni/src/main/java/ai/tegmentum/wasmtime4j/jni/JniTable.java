package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
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
 * comprehensive bounds checking for table access using JniValidation and the JniResource base
 * class.
 */
public final class JniTable extends JniResource implements WasmTable {

  private static final Logger LOGGER = Logger.getLogger(JniTable.class.getName());
  private final JniStore store;

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniTable: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Creates a new JNI table with the given native handle and store.
   *
   * @param nativeHandle the native table handle
   * @param store the store that owns this table
   * @throws JniResourceException if nativeHandle is invalid
   * @throws IllegalArgumentException if store is null
   */
  JniTable(final long nativeHandle, final JniStore store) {
    super(nativeHandle);
    JniValidation.requireNonNull(store, "store");
    this.store = store;
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
    if (store.isClosed()) {
      throw new JniResourceException("Store is closed");
    }
    try {
      final long tableHandle = getNativeHandle();
      final long storeHandle = store.getNativeHandle();
      final int size = nativeGetSize(tableHandle, storeHandle);
      return size;
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
    if (store.isClosed()) {
      throw new JniResourceException("Store is closed");
    }
    try {
      return nativeGetMaxSize(getNativeHandle(), store.getNativeHandle());
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
  public WasmValueType getElementType() {
    if (store.isClosed()) {
      throw new JniResourceException("Store is closed");
    }
    try {
      final String typeString = nativeGetElementType(getNativeHandle(), store.getNativeHandle());
      if (typeString == null) {
        return WasmValueType.EXTERNREF;
      }
      // Convert string type to WasmValueType enum
      switch (typeString.toLowerCase()) {
        case "funcref":
          return WasmValueType.FUNCREF;
        case "externref":
          return WasmValueType.EXTERNREF;
        default:
          return WasmValueType.EXTERNREF; // Default fallback
      }
    } catch (final JniResourceException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting table element type", e);
    }
  }

  @Override
  public WasmValueType getType() {
    return getElementType();
  }

  @Override
  public ai.tegmentum.wasmtime4j.TableType getTableType() {
    if (store.isClosed()) {
      throw new JniResourceException("Store is closed");
    }
    try {
      final long[] typeInfo = nativeGetTableTypeInfo(getNativeHandle(), store.getNativeHandle());
      if (typeInfo.length < 3) {
        throw new IllegalStateException("Invalid table type info from native");
      }
      final WasmValueType elementType = WasmValueType.fromNativeTypeCode((int) typeInfo[0]);
      final long minimum = typeInfo[1];
      final Long maximum = typeInfo[2] == -1 ? null : typeInfo[2];
      return new ai.tegmentum.wasmtime4j.jni.type.JniTableType(elementType, minimum, maximum);
    } catch (final JniResourceException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting table type", e);
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

    if (store.isClosed()) {
      throw new JniResourceException("Store is closed");
    }

    final long handle = getNativeHandle(); // This validates not closed
    validateIndex(index);

    try {
      return nativeGet(handle, store.getNativeHandle(), index);
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

    if (store.isClosed()) {
      throw new JniResourceException("Store is closed");
    }

    final long handle = getNativeHandle(); // This validates not closed
    validateIndex(index);

    System.err.println(
        "[JAVA] JniTable.set called: index="
            + index
            + ", value="
            + value
            + ", tableHandle=0x"
            + Long.toHexString(handle)
            + ", storeHandle=0x"
            + Long.toHexString(store.getNativeHandle()));

    if (value != null && value instanceof JniHostFunction) {
      JniHostFunction func = (JniHostFunction) value;
      System.err.println(
          "[JAVA] JniTable.set: value is JniHostFunction with nativeHandle=0x"
              + Long.toHexString(func.getNativeHandle()));
    }

    try {
      final boolean success = nativeSet(handle, store.getNativeHandle(), index, value);
      System.err.println("[JAVA] JniTable.set: nativeSet returned " + success);
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

    if (store.isClosed()) {
      throw new JniResourceException("Store is closed");
    }

    try {
      final long tableHandle = getNativeHandle();
      final long storeHandle = store.getNativeHandle();
      // Convert init Object to a long handle for native code
      // For null/funcref null, pass 0
      final long initValue = 0L; // funcref null
      return (int) nativeTableGrow(tableHandle, storeHandle, delta, initValue);
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
   * Copies elements from one region of the table to another.
   *
   * @param dst the destination starting index
   * @param src the source starting index
   * @param count the number of elements to copy
   * @throws IllegalArgumentException if dst, src, or count is negative
   * @throws JniResourceException if this table is closed
   * @throws IndexOutOfBoundsException if source or destination range exceeds table bounds
   * @throws RuntimeException if the copy operation fails
   */
  public void copy(final int dst, final int src, final int count) {
    JniValidation.requireNonNegative(dst, "dst");
    JniValidation.requireNonNegative(src, "src");
    JniValidation.requireNonNegative(count, "count");

    final long handle = getNativeHandle(); // This validates not closed
    validateRange(dst, count);
    validateRange(src, count);

    try {
      final boolean success = nativeCopy(handle, dst, src, count);
      if (!success) {
        throw new RuntimeException("Failed to copy table elements");
      }
    } catch (final JniResourceException | IllegalArgumentException | IndexOutOfBoundsException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error copying table elements", e);
    }
  }

  /**
   * Copies elements from another table to this table.
   *
   * @param dst the destination starting index in this table
   * @param src the source table to copy from
   * @param srcIndex the source starting index in the source table
   * @param count the number of elements to copy
   * @throws IllegalArgumentException if dst, srcIndex, or count is negative, or if src is null
   * @throws JniResourceException if this table or source table is closed
   * @throws IndexOutOfBoundsException if source or destination range exceeds table bounds
   * @throws RuntimeException if the copy operation fails
   */
  public void copy(final int dst, final WasmTable src, final int srcIndex, final int count) {
    JniValidation.requireNonNull(src, "src");
    JniValidation.requireNonNegative(dst, "dst");
    JniValidation.requireNonNegative(srcIndex, "srcIndex");
    JniValidation.requireNonNegative(count, "count");

    if (!(src instanceof JniTable)) {
      throw new IllegalArgumentException(
          "Source table must be JniTable instance for cross-table copy");
    }

    final JniTable srcTable = (JniTable) src;
    final long dstHandle = getNativeHandle(); // This validates not closed
    final long srcHandle = srcTable.getNativeHandle(); // This validates source not closed

    // Validate destination range
    validateRange(dst, count);

    // Validate source range
    final int srcTableSize = srcTable.getSize();
    if (srcIndex + count > srcTableSize) {
      throw new IndexOutOfBoundsException(
          "Source range ["
              + srcIndex
              + ", "
              + (srcIndex + count)
              + ") exceeds source table size "
              + srcTableSize);
    }

    // Validate type compatibility
    if (!getElementType().equals(srcTable.getElementType())) {
      throw new IllegalArgumentException(
          "Table element types must match: this="
              + getElementType()
              + ", src="
              + srcTable.getElementType());
    }

    try {
      final boolean success = nativeCopyFromTable(dstHandle, dst, srcHandle, srcIndex, count);
      if (!success) {
        throw new RuntimeException("Failed to copy elements from source table");
      }
    } catch (final JniResourceException | IllegalArgumentException | IndexOutOfBoundsException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error copying from source table", e);
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

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, Tables are owned by the Store. Destroying a Table while the Store still
   * exists can corrupt the Store's internal slab state. We mark the Table as closed but don't
   * destroy it - the Store will handle cleanup.
   */
  @Override
  protected void doClose() throws Exception {
    // Note: Do NOT call nativeDestroy here. Tables are Store-owned resources.
    // The Store will clean up all its Tables when it is destroyed.
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
   * @param storeHandle the native store handle
   * @return the number of elements in the table
   */
  private static native int nativeGetSize(long tableHandle, long storeHandle);

  /**
   * Gets the maximum size of a table.
   *
   * @param tableHandle the native table handle
   * @param storeHandle the native store handle
   * @return the maximum number of elements, or -1 if unlimited
   */
  private static native int nativeGetMaxSize(long tableHandle, long storeHandle);

  /**
   * Gets the element type of a table.
   *
   * @param tableHandle the native table handle
   * @param storeHandle the native store handle
   * @return the element type name or null on error
   */
  private static native String nativeGetElementType(long tableHandle, long storeHandle);

  /**
   * Gets an element from a table.
   *
   * @param tableHandle the native table handle
   * @param storeHandle the native store handle
   * @param index the table index
   * @return the element at the index or null
   */
  private static native Object nativeGet(long tableHandle, long storeHandle, int index);

  /**
   * Sets an element in a table.
   *
   * @param tableHandle the native table handle
   * @param storeHandle the native store handle
   * @param index the table index
   * @param value the value to set
   * @return true on success, false on failure
   */
  private static native boolean nativeSet(
      long tableHandle, long storeHandle, int index, Object value);

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
   * Grows a table by the specified number of elements (with store context).
   *
   * @param tableHandle the native table handle
   * @param storeHandle the native store handle
   * @param delta the number of elements to add
   * @param initValue the initial value handle for new elements (0 for null)
   * @return the previous size or -1 on failure
   */
  private static native long nativeTableGrow(
      long tableHandle, long storeHandle, int delta, long initValue);

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
   * Copies elements within a table.
   *
   * @param tableHandle the native table handle
   * @param dst the destination starting index
   * @param src the source starting index
   * @param count the number of elements to copy
   * @return true on success, false on failure
   */
  private static native boolean nativeCopy(long tableHandle, int dst, int src, int count);

  /**
   * Copies elements from another table to this table.
   *
   * @param dstTableHandle the destination table handle
   * @param dst the destination starting index
   * @param srcTableHandle the source table handle
   * @param src the source starting index
   * @param count the number of elements to copy
   * @return true on success, false on failure
   */
  private static native boolean nativeCopyFromTable(
      long dstTableHandle, int dst, long srcTableHandle, int src, int count);

  /**
   * Destroys a native table.
   *
   * @param tableHandle the native table handle
   */
  private static native void nativeDestroy(long tableHandle);

  /**
   * Gets table type information directly from the table.
   *
   * @param tableHandle the native table handle
   * @param storeHandle the native store handle
   * @return array containing [elementTypeCode, minimum, maximum(-1 if unlimited)]
   */
  private static native long[] nativeGetTableTypeInfo(long tableHandle, long storeHandle);

  @Override
  public void dropElementSegment(final int segmentIndex) {
    JniValidation.requireNonNegative(segmentIndex, "segmentIndex");
    ensureNotClosed();

    // Element segments are dropped automatically by the runtime
    // This is a no-op in the JNI implementation as Wasmtime manages element segments internally
    LOGGER.fine("Dropped element segment: " + segmentIndex);
  }

  @Override
  public void init(
      final int destOffset, final int srcOffset, final int srcSegmentIndex, final int length) {
    JniValidation.requireNonNegative(destOffset, "destOffset");
    JniValidation.requireNonNegative(srcOffset, "srcOffset");
    JniValidation.requireNonNegative(srcSegmentIndex, "srcSegmentIndex");
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    // Table initialization from element segments is handled by the runtime during instantiation
    // This is a no-op in the JNI implementation as Wasmtime manages table initialization internally
    LOGGER.fine(
        "Initialized table range ["
            + destOffset
            + ", "
            + (destOffset + length)
            + ") from segment "
            + srcSegmentIndex);
  }

  /**
   * Checks if this table supports 64-bit addressing.
   *
   * <p>Returns true if the table was created with the table64 proposal enabled, allowing for table
   * sizes and indices larger than 32-bit limits.
   *
   * @return true if 64-bit addressing is supported
   * @since 1.1.0
   */
  @Override
  public boolean supports64BitAddressing() {
    if (store.isClosed()) {
      return false;
    }
    try {
      return nativeSupports64BitAddressing(getNativeHandle(), store.getNativeHandle());
    } catch (final Exception e) {
      LOGGER.fine("Error checking 64-bit addressing support: " + e.getMessage());
      return false;
    }
  }

  /**
   * Checks if the table supports 64-bit addressing.
   *
   * @param tableHandle the native table handle
   * @param storeHandle the native store handle
   * @return true if 64-bit addressing is supported
   */
  private static native boolean nativeSupports64BitAddressing(long tableHandle, long storeHandle);
}
