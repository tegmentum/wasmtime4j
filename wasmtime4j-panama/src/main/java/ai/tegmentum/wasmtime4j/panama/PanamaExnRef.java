package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.gc.ExnType;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
  private static final NativeInstanceBindings NATIVE_BINDINGS =
      NativeInstanceBindings.getInstance();

  private final MemorySegment nativeHandle;
  private final MemorySegment storeHandle;
  private final NativeResourceHandle resourceHandle;

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
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaExnRef",
            () -> {
              NATIVE_BINDINGS.exnRefDestroy(nativeHandle);
              LOGGER.fine("Destroyed exception reference");
            });
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
  public WasmValue field(final Store store, final int index) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (index < 0) {
      throw new IllegalArgumentException("index must be non-negative");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outType = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment outValueI64 = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outValueF64 = arena.allocate(ValueLayout.JAVA_DOUBLE);

      final int result =
          NATIVE_BINDINGS.exnRefGetField(
              nativeHandle, panamaStore.getNativeStore(), index, outType, outValueI64, outValueF64);

      if (result != 0) {
        throw new WasmException("Failed to get field " + index + " from exception reference");
      }

      return decodeFieldValue(
          outType.get(ValueLayout.JAVA_INT, 0),
          outValueI64.get(ValueLayout.JAVA_LONG, 0),
          outValueF64.get(ValueLayout.JAVA_DOUBLE, 0));
    }
  }

  @Override
  public List<WasmValue> fields(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final int count = NATIVE_BINDINGS.exnRefFieldCount(nativeHandle, panamaStore.getNativeStore());

    if (count < 0) {
      throw new WasmException("Failed to get field count from exception reference");
    }

    final List<WasmValue> fieldValues = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      fieldValues.add(field(store, i));
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
    if (resourceHandle.isClosed()) {
      return false;
    }
    return NATIVE_BINDINGS.exnRefIsValid(nativeHandle, storeHandle) != 0;
  }

  /** Closes this exception reference and releases native resources. */
  public void close() {
    resourceHandle.close();
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  private static WasmValue decodeFieldValue(
      final int typeCode, final long i64Value, final double f64Value) throws WasmException {
    switch (typeCode) {
      case 0:
        return WasmValue.i32((int) i64Value);
      case 1:
        return WasmValue.i64(i64Value);
      case 2:
        return WasmValue.f32((float) f64Value);
      case 3:
        return WasmValue.f64(f64Value);
      default:
        throw new WasmException("Unsupported field value type code: " + typeCode);
    }
  }
}
