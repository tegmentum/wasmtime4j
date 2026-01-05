package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.TagType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.util.logging.Logger;

/**
 * JNI implementation of the {@link Tag} interface.
 *
 * <p>This class wraps a native Wasmtime tag handle and provides exception tag functionality for the
 * WebAssembly exception handling proposal.
 *
 * @since 1.0.0
 */
public final class JniTag extends JniResource implements Tag {

  private static final Logger LOGGER = Logger.getLogger(JniTag.class.getName());

  private final long storeHandle;

  /**
   * Creates a new JniTag wrapping a native tag handle.
   *
   * @param nativeHandle the native tag handle
   * @param storeHandle the store handle this tag belongs to
   */
  JniTag(final long nativeHandle, final long storeHandle) {
    super(nativeHandle);
    this.storeHandle = storeHandle;
  }

  @Override
  public TagType getType(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    final long currentStoreHandle = jniStore.getNativeHandle();

    // Validate store matches
    if (currentStoreHandle != storeHandle) {
      LOGGER.warning("Tag accessed with different store than it was created with");
    }

    final int[] paramTypes = nativeGetParamTypes(getNativeHandle(), currentStoreHandle);
    final int[] returnTypes = nativeGetReturnTypes(getNativeHandle(), currentStoreHandle);

    // Convert native type codes to Java types
    final ai.tegmentum.wasmtime4j.WasmValueType[] params =
        new ai.tegmentum.wasmtime4j.WasmValueType[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      params[i] = ai.tegmentum.wasmtime4j.WasmValueType.fromNativeTypeCode(paramTypes[i]);
    }

    final ai.tegmentum.wasmtime4j.WasmValueType[] returns =
        new ai.tegmentum.wasmtime4j.WasmValueType[returnTypes.length];
    for (int i = 0; i < returnTypes.length; i++) {
      returns[i] = ai.tegmentum.wasmtime4j.WasmValueType.fromNativeTypeCode(returnTypes[i]);
    }

    final ai.tegmentum.wasmtime4j.FunctionType funcType =
        new ai.tegmentum.wasmtime4j.FunctionType(params, returns);
    return TagType.create(funcType);
  }

  @Override
  public boolean equals(final Tag other, final Store store) throws WasmException {
    if (other == null) {
      return false;
    }
    if (!(other instanceof JniTag)) {
      return false;
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    return nativeEquals(
        getNativeHandle(), ((JniTag) other).getNativeHandle(), jniStore.getNativeHandle());
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, Tags are owned by the Store. Destroying a Tag while the Store still
   * exists can corrupt the Store's internal slab state. We mark the Tag as closed but don't destroy
   * it - the Store will handle cleanup.
   */
  @Override
  protected void doClose() throws Exception {
    // Note: Do NOT call nativeDestroy here. Tags are Store-owned resources.
    // The Store will clean up all its Tags when it is destroyed.
    LOGGER.fine(
        "Tag marked as closed (handle: 0x"
            + Long.toHexString(nativeHandle)
            + "). Native resources freed with Store.");
  }

  @Override
  protected String getResourceType() {
    return "Tag";
  }

  // Native method declarations
  private static native int[] nativeGetParamTypes(long tagHandle, long storeHandle);

  private static native int[] nativeGetReturnTypes(long tagHandle, long storeHandle);

  private static native boolean nativeEquals(long tag1Handle, long tag2Handle, long storeHandle);

  private static native void nativeDestroy(long tagHandle);
}
