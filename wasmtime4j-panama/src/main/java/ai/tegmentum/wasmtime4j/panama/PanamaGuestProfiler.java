package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.debug.GuestProfiler;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallHook;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Panama implementation of {@link GuestProfiler}.
 *
 * <p>Wraps a native Wasmtime GuestProfiler handle using Panama Foreign Function API. After {@link
 * #finish()} is called, the native profiler is consumed and further calls to {@link #sample(Store,
 * Duration)} and {@link #callHook(Store, CallHook)} will throw.
 *
 * @since 1.0.0
 */
final class PanamaGuestProfiler implements GuestProfiler {

  private static final Logger LOGGER = Logger.getLogger(PanamaGuestProfiler.class.getName());
  private static final NativeEngineBindings NATIVE_BINDINGS = NativeEngineBindings.getInstance();

  private MemorySegment nativeHandle;
  private boolean finished;

  /**
   * Creates a new Panama guest profiler.
   *
   * @param enginePtr the native engine pointer
   * @param moduleName the profile label
   * @param intervalNanos sampling interval hint in nanoseconds
   * @param modules map of module names to modules
   * @throws WasmException if profiler creation fails
   */
  PanamaGuestProfiler(
      final MemorySegment enginePtr,
      final String moduleName,
      final long intervalNanos,
      final Map<String, Module> modules)
      throws WasmException {
    if (enginePtr == null || enginePtr.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("enginePtr cannot be null");
    }
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("moduleName cannot be null or empty");
    }

    try (Arena arena = Arena.ofConfined()) {
      // Allocate module name as C string
      final MemorySegment nameSegment = arena.allocateFrom(moduleName);

      // Allocate arrays for module pointers and names
      final int count = modules.size();
      final MemorySegment modulePtrs =
          arena.allocate(ValueLayout.ADDRESS, count);
      final MemorySegment moduleNamePtrs =
          arena.allocate(ValueLayout.ADDRESS, count);

      int idx = 0;
      for (final Map.Entry<String, Module> entry : modules.entrySet()) {
        if (!(entry.getValue() instanceof PanamaModule)) {
          throw new WasmException("Module '" + entry.getKey() + "' must be a Panama module");
        }
        final PanamaModule panamaModule = (PanamaModule) entry.getValue();
        final MemorySegment modulePtr = panamaModule.getNativeModule();
        if (modulePtr == null || modulePtr.equals(MemorySegment.NULL)) {
          throw new WasmException("Module '" + entry.getKey() + "' has invalid native handle");
        }
        modulePtrs.setAtIndex(ValueLayout.ADDRESS, idx, modulePtr);

        final MemorySegment entryName = arena.allocateFrom(entry.getKey());
        moduleNamePtrs.setAtIndex(ValueLayout.ADDRESS, idx, entryName);
        idx++;
      }

      this.nativeHandle =
          NATIVE_BINDINGS.guestProfilerNew(
              enginePtr, nameSegment, intervalNanos, modulePtrs, moduleNamePtrs, count);
    }

    if (this.nativeHandle == null || this.nativeHandle.equals(MemorySegment.NULL)) {
      throw new WasmException(
          "Failed to create GuestProfiler (is guest debugging enabled on the engine?)");
    }
    this.finished = false;
  }

  @Override
  public void sample(final Store store, final Duration delta) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (delta == null) {
      throw new IllegalArgumentException("delta cannot be null");
    }
    ensureActive();

    if (!(store instanceof PanamaStore)) {
      throw new WasmException("Store must be a Panama store implementation");
    }
    final MemorySegment storePtr = ((PanamaStore) store).getNativeStore();
    if (storePtr == null || storePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Store has invalid native handle");
    }

    final long deltaNanos = delta.toNanos();
    final int result = NATIVE_BINDINGS.guestProfilerSample(nativeHandle, storePtr, deltaNanos);
    if (result == -2) {
      throw new IllegalStateException("Profiler has already been finished");
    }
    if (result != 0) {
      throw new WasmException("Failed to collect stack sample");
    }
  }

  @Override
  public void callHook(final Store store, final CallHook hook) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (hook == null) {
      throw new IllegalArgumentException("hook cannot be null");
    }
    ensureActive();

    if (!(store instanceof PanamaStore)) {
      throw new WasmException("Store must be a Panama store implementation");
    }
    final MemorySegment storePtr = ((PanamaStore) store).getNativeStore();
    if (storePtr == null || storePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Store has invalid native handle");
    }

    final int result =
        NATIVE_BINDINGS.guestProfilerCallHook(nativeHandle, storePtr, hook.getValue());
    if (result == -2) {
      throw new IllegalStateException("Profiler has already been finished");
    }
    if (result != 0) {
      throw new WasmException("Failed to record call hook");
    }
  }

  @Override
  public byte[] finish() throws WasmException {
    ensureActive();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment dataOut = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment lenOut = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = NATIVE_BINDINGS.guestProfilerFinish(nativeHandle, dataOut, lenOut);
      if (result != 0) {
        throw new WasmException("Failed to finish profiler");
      }

      final MemorySegment dataPtr = dataOut.get(ValueLayout.ADDRESS, 0);
      final long dataLen = lenOut.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen == 0) {
        throw new WasmException("Failed to finish profiler - empty data");
      }

      // Copy the data into a Java byte array
      final MemorySegment dataSlice = dataPtr.reinterpret(dataLen);
      final byte[] data = dataSlice.toArray(ValueLayout.JAVA_BYTE);

      // Free the Rust-allocated data
      NATIVE_BINDINGS.guestProfilerFreeData(dataPtr, dataLen);

      finished = true;
      LOGGER.fine("Guest profiler finished, profile size: " + data.length + " bytes");
      return data;
    }
  }

  @Override
  public boolean isActive() {
    return nativeHandle != null && !nativeHandle.equals(MemorySegment.NULL) && !finished;
  }

  @Override
  public void close() {
    if (nativeHandle != null && !nativeHandle.equals(MemorySegment.NULL)) {
      NATIVE_BINDINGS.guestProfilerDestroy(nativeHandle);
      nativeHandle = MemorySegment.NULL;
      LOGGER.fine("Closed guest profiler");
    }
  }

  private void ensureActive() {
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Profiler has been closed");
    }
    if (finished) {
      throw new IllegalStateException("Profiler has already been finished");
    }
  }
}
