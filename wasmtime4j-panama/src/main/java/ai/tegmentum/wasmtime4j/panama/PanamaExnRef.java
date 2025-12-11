package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the {@link ExnRef} interface.
 *
 * <p>This class wraps a native Wasmtime exception reference using the Panama Foreign Function API
 * for the WebAssembly exception handling proposal.
 *
 * @since 1.0.0
 */
public final class PanamaExnRef implements ExnRef {

  private static final Logger LOGGER = Logger.getLogger(PanamaExnRef.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final MemorySegment nativeHandle;
  private final MemorySegment storeHandle;
  private volatile boolean closed = false;

  /**
   * Creates a new PanamaExnRef wrapping a native exception reference.
   *
   * @param nativeHandle the native exception reference segment
   * @param storeHandle the store segment this exception belongs to
   */
  PanamaExnRef(final MemorySegment nativeHandle, final MemorySegment storeHandle) {
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("nativeHandle cannot be null");
    }
    this.nativeHandle = nativeHandle;
    this.storeHandle = storeHandle;
  }

  @Override
  public Tag getTag(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final MemorySegment tagPtr =
        NATIVE_BINDINGS.exnRefGetTag(nativeHandle, panamaStore.getNativeStore());

    if (tagPtr == null || tagPtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to get tag from exception reference");
    }

    return new PanamaTag(tagPtr, panamaStore.getNativeStore());
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

  @Override
  public boolean isValid() {
    if (closed) {
      return false;
    }
    return NATIVE_BINDINGS.exnRefIsValid(nativeHandle, storeHandle) != 0;
  }

  /** Closes this exception reference and releases native resources. */
  public void close() {
    if (!closed) {
      closed = true;
      NATIVE_BINDINGS.exnRefDestroy(nativeHandle);
      LOGGER.fine("Destroyed exception reference");
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("ExnRef has been closed");
    }
  }
}
