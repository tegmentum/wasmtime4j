package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.gc.ExnType;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.memory.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * JNI implementation of the {@link ExnRef} interface.
 *
 * <p>This class wraps a native Wasmtime exception reference handle for the WebAssembly exception
 * handling proposal. Exception references are rooted GC references that can be thrown and caught.
 *
 * @since 1.0.0
 */
public final class JniExnRef extends JniResource implements ExnRef {

  private static final Logger LOGGER = Logger.getLogger(JniExnRef.class.getName());

  private final long storeHandle;

  /**
   * Creates a new JniExnRef wrapping a native exception reference handle.
   *
   * @param nativeHandle the native exception reference handle
   * @param storeHandle the store handle this exception belongs to
   */
  JniExnRef(final long nativeHandle, final long storeHandle) {
    super(nativeHandle);
    this.storeHandle = storeHandle;
  }

  @Override
  public Tag getTag(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }
    final long currentStoreHandle = jniStore.getNativeHandle();

    final long tagHandle = nativeGetTag(getNativeHandle(), currentStoreHandle);

    if (tagHandle == 0) {
      throw new WasmException("Failed to get tag from exception reference");
    }

    return new JniTag(tagHandle, currentStoreHandle);
  }

  @Override
  public WasmValue field(final Store store, final int index) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (index < 0) {
      throw new IllegalArgumentException("index must be non-negative");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }

    final Object[] result = nativeGetField(getNativeHandle(), jniStore.getNativeHandle(), index);

    if (result == null || result.length == 0) {
      throw new WasmException("Failed to get field " + index + " from exception reference");
    }

    return (WasmValue) result[0];
  }

  @Override
  public List<WasmValue> fields(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }

    final Object[] result = nativeGetFields(getNativeHandle(), jniStore.getNativeHandle());

    if (result == null) {
      throw new WasmException("Failed to get fields from exception reference");
    }

    final List<WasmValue> fieldValues = new ArrayList<>(result.length);
    for (final Object val : result) {
      fieldValues.add((WasmValue) val);
    }
    return Collections.unmodifiableList(fieldValues);
  }

  @Override
  public ExnType ty(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    final Tag tag = getTag(store);
    return new ExnType(tag.getType(store));
  }

  @Override
  public boolean isValid() {
    if (isClosed()) {
      return false;
    }
    return nativeIsValid(getNativeHandle(), storeHandle);
  }

  /**
   * Gets the store handle this exception belongs to.
   *
   * @return the store handle
   */
  long getStoreHandle() {
    return storeHandle;
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, ExnRefs are owned by the Store. Destroying an ExnRef while the Store
   * still exists can corrupt the Store's internal slab state. We mark the ExnRef as closed but
   * don't destroy it - the Store will handle cleanup.
   */
  @Override
  protected void doClose() throws Exception {
    // Note: Do NOT call nativeDestroy here. ExnRefs are Store-owned resources.
    // The Store will clean up all its ExnRefs when it is destroyed.
    LOGGER.fine(
        "ExnRef marked as closed (handle: 0x"
            + Long.toHexString(nativeHandle)
            + "). Native resources freed with Store.");
  }

  @Override
  protected String getResourceType() {
    return "ExnRef";
  }

  // Native method declarations
  private static native long nativeGetTag(long exnRefHandle, long storeHandle);

  private static native boolean nativeIsValid(long exnRefHandle, long storeHandle);

  private static native Object[] nativeGetField(long exnRefHandle, long storeHandle, int index);

  private static native Object[] nativeGetFields(long exnRefHandle, long storeHandle);
}
