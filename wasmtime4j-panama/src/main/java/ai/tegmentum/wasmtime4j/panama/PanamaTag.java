package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.TagType;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the {@link Tag} interface.
 *
 * <p>This class wraps a native Wasmtime tag using the Panama Foreign Function API for the
 * WebAssembly exception handling proposal.
 *
 * @since 1.0.0
 */
public final class PanamaTag implements Tag {

  private static final Logger LOGGER = Logger.getLogger(PanamaTag.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final MemorySegment nativeHandle;
  private final MemorySegment storeHandle;
  private volatile boolean closed = false;

  /**
   * Creates a new PanamaTag wrapping a native tag.
   *
   * @param nativeHandle the native tag segment
   * @param storeHandle the store segment this tag belongs to
   */
  PanamaTag(final MemorySegment nativeHandle, final MemorySegment storeHandle) {
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("nativeHandle cannot be null");
    }
    this.nativeHandle = nativeHandle;
    this.storeHandle = storeHandle;
  }

  @Override
  public TagType getType(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final MemorySegment currentStoreHandle = panamaStore.getNativeStore();

    // Validate store matches
    if (!currentStoreHandle.equals(storeHandle)) {
      LOGGER.warning("Tag accessed with different store than it was created with");
    }

    final int[] paramTypeCodes = NATIVE_BINDINGS.tagGetParamTypes(nativeHandle, currentStoreHandle);
    final int[] returnTypeCodes =
        NATIVE_BINDINGS.tagGetReturnTypes(nativeHandle, currentStoreHandle);

    // Convert native type codes to Java types
    final WasmValueType[] params = new WasmValueType[paramTypeCodes.length];
    for (int i = 0; i < paramTypeCodes.length; i++) {
      params[i] = WasmValueType.fromNativeTypeCode(paramTypeCodes[i]);
    }

    final WasmValueType[] returns = new WasmValueType[returnTypeCodes.length];
    for (int i = 0; i < returnTypeCodes.length; i++) {
      returns[i] = WasmValueType.fromNativeTypeCode(returnTypeCodes[i]);
    }

    final FunctionType funcType = new FunctionType(params, returns);
    return TagType.create(funcType);
  }

  @Override
  public boolean equals(final Tag other, final Store store) throws WasmException {
    if (other == null) {
      return false;
    }
    if (!(other instanceof PanamaTag)) {
      return false;
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    return NATIVE_BINDINGS.tagEquals(
            nativeHandle, ((PanamaTag) other).nativeHandle, panamaStore.getNativeStore())
        != 0;
  }

  @Override
  public long getNativeHandle() {
    return nativeHandle.address();
  }

  /**
   * Gets the native memory segment handle.
   *
   * @return the native memory segment
   */
  MemorySegment getNativeSegment() {
    return nativeHandle;
  }

  /** Closes this tag and releases native resources. */
  public void close() {
    if (!closed) {
      closed = true;
      NATIVE_BINDINGS.tagDestroy(nativeHandle);
      LOGGER.fine("Destroyed tag");
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Tag has been closed");
    }
  }
}
