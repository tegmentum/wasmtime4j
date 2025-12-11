package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
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
    final long currentStoreHandle = jniStore.getNativeHandle();

    final long tagHandle = nativeGetTag(getNativeHandle(), currentStoreHandle);

    if (tagHandle == 0) {
      throw new WasmException("Failed to get tag from exception reference");
    }

    return new JniTag(tagHandle, currentStoreHandle);
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

  @Override
  protected void doClose() throws Exception {
    nativeDestroy(getNativeHandle());
    LOGGER.fine(
        "Destroyed exception reference with handle: 0x" + Long.toHexString(getNativeHandle()));
  }

  @Override
  protected String getResourceType() {
    return "ExnRef";
  }

  // Native method declarations
  private static native long nativeGetTag(long exnRefHandle, long storeHandle);

  private static native boolean nativeIsValid(long exnRefHandle, long storeHandle);

  private static native void nativeDestroy(long exnRefHandle);
}
