package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.debug.GuestProfiler;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallHook;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link GuestProfiler}.
 *
 * <p>Wraps a native Wasmtime GuestProfiler handle. The native profiler is behind a Mutex for
 * thread-safe access. After {@link #finish()} is called, the native profiler is consumed and the
 * handle remains valid but calls to {@link #sample(Store, Duration)} and {@link #callHook(Store,
 * CallHook)} will throw.
 *
 * @since 1.0.0
 */
final class JniGuestProfiler implements GuestProfiler {

  private static final Logger LOGGER = Logger.getLogger(JniGuestProfiler.class.getName());

  private long nativeHandle;
  private boolean finished;

  /**
   * Creates a new JNI guest profiler.
   *
   * @param engineHandle the native engine handle
   * @param moduleName the profile label
   * @param intervalNanos sampling interval hint in nanoseconds
   * @param modules map of module names to modules
   * @throws WasmException if profiler creation fails
   */
  JniGuestProfiler(
      final long engineHandle,
      final String moduleName,
      final long intervalNanos,
      final Map<String, Module> modules)
      throws WasmException {
    if (engineHandle == 0) {
      throw new IllegalArgumentException("engineHandle cannot be 0");
    }
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("moduleName cannot be null or empty");
    }

    // Extract native module handles and names
    final long[] moduleHandles = new long[modules.size()];
    final String[] moduleNames = new String[modules.size()];
    int idx = 0;
    for (final Map.Entry<String, Module> entry : modules.entrySet()) {
      moduleNames[idx] = entry.getKey();
      if (!(entry.getValue() instanceof JniModule)) {
        throw new WasmException("Module '" + entry.getKey() + "' must be a JNI module");
      }
      moduleHandles[idx] = ((JniModule) entry.getValue()).getNativeHandle();
      if (moduleHandles[idx] == 0) {
        throw new WasmException("Module '" + entry.getKey() + "' has invalid native handle");
      }
      idx++;
    }

    this.nativeHandle =
        nativeGuestProfilerCreate(
            engineHandle, moduleName, intervalNanos, moduleHandles, moduleNames);
    if (this.nativeHandle == 0) {
      throw new WasmException(
          "Failed to create GuestProfiler (is guest debugging enabled on the engine?)");
    }
    this.finished = false;
  }

  /**
   * Creates a new JNI guest profiler for a component.
   *
   * @param engineHandle the native engine handle
   * @param componentName the profile label
   * @param intervalNanos sampling interval hint in nanoseconds
   * @param componentHandle the native component handle
   * @param extraModules map of extra module names to modules
   * @throws WasmException if profiler creation fails
   */
  JniGuestProfiler(
      final long engineHandle,
      final String componentName,
      final long intervalNanos,
      final long componentHandle,
      final Map<String, Module> extraModules)
      throws WasmException {
    if (engineHandle == 0) {
      throw new IllegalArgumentException("engineHandle cannot be 0");
    }
    if (componentName == null || componentName.isEmpty()) {
      throw new IllegalArgumentException("componentName cannot be null or empty");
    }
    if (componentHandle == 0) {
      throw new IllegalArgumentException("componentHandle cannot be 0");
    }

    // Extract extra native module handles and names
    final int extraCount = extraModules != null ? extraModules.size() : 0;
    final long[] moduleHandles = new long[extraCount];
    final String[] moduleNames = new String[extraCount];
    if (extraModules != null) {
      int idx = 0;
      for (final Map.Entry<String, Module> entry : extraModules.entrySet()) {
        moduleNames[idx] = entry.getKey();
        if (!(entry.getValue() instanceof JniModule)) {
          throw new WasmException("Module '" + entry.getKey() + "' must be a JNI module");
        }
        moduleHandles[idx] = ((JniModule) entry.getValue()).getNativeHandle();
        if (moduleHandles[idx] == 0) {
          throw new WasmException("Module '" + entry.getKey() + "' has invalid native handle");
        }
        idx++;
      }
    }

    this.nativeHandle =
        nativeGuestProfilerCreateComponent(
            engineHandle,
            componentName,
            intervalNanos,
            componentHandle,
            moduleHandles,
            moduleNames);
    if (this.nativeHandle == 0) {
      throw new WasmException(
          "Failed to create component GuestProfiler (is guest debugging enabled on the engine?)");
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

    if (!(store instanceof JniStore)) {
      throw new WasmException("Store must be a JNI store implementation");
    }
    final long storeHandle = ((JniStore) store).getNativeHandle();
    if (storeHandle == 0) {
      throw new WasmException("Store has invalid native handle");
    }

    final long deltaNanos = delta.toNanos();
    final int result = nativeGuestProfilerSample(nativeHandle, storeHandle, deltaNanos);
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

    if (!(store instanceof JniStore)) {
      throw new WasmException("Store must be a JNI store implementation");
    }
    final long storeHandle = ((JniStore) store).getNativeHandle();
    if (storeHandle == 0) {
      throw new WasmException("Store has invalid native handle");
    }

    final int result = nativeGuestProfilerCallHook(nativeHandle, storeHandle, hook.getValue());
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

    final byte[] data = nativeGuestProfilerFinish(nativeHandle);
    if (data == null) {
      throw new WasmException("Failed to finish profiler");
    }
    finished = true;
    LOGGER.fine("Guest profiler finished, profile size: " + data.length + " bytes");
    return data;
  }

  @Override
  public boolean isActive() {
    return nativeHandle != 0 && !finished;
  }

  @Override
  public void close() {
    if (nativeHandle != 0) {
      nativeGuestProfilerDestroy(nativeHandle);
      nativeHandle = 0;
      LOGGER.fine("Closed guest profiler");
    }
  }

  private void ensureActive() {
    if (nativeHandle == 0) {
      throw new IllegalStateException("Profiler has been closed");
    }
    if (finished) {
      throw new IllegalStateException("Profiler has already been finished");
    }
  }

  // ===== Native methods =====

  /**
   * Creates a new guest profiler.
   *
   * @param engineHandle the native engine handle
   * @param moduleName the profile label
   * @param intervalNanos sampling interval in nanoseconds
   * @param moduleHandles array of native module handles
   * @param moduleNames array of module names
   * @return the native profiler handle, or 0 on failure
   */
  private static native long nativeGuestProfilerCreate(
      long engineHandle,
      String moduleName,
      long intervalNanos,
      long[] moduleHandles,
      String[] moduleNames);

  /**
   * Creates a new guest profiler for a component.
   *
   * @param engineHandle the native engine handle
   * @param componentName the profile label
   * @param intervalNanos sampling interval in nanoseconds
   * @param componentHandle the native component handle
   * @param extraModuleHandles array of extra native module handles
   * @param extraModuleNames array of extra module names
   * @return the native profiler handle, or 0 on failure
   */
  private static native long nativeGuestProfilerCreateComponent(
      long engineHandle,
      String componentName,
      long intervalNanos,
      long componentHandle,
      long[] extraModuleHandles,
      String[] extraModuleNames);

  /**
   * Collects a stack sample.
   *
   * @param profilerHandle the native profiler handle
   * @param storeHandle the native store handle
   * @param deltaNanos CPU time since previous sample in nanoseconds
   * @return 0 on success, -1 on error, -2 if already finished
   */
  private static native int nativeGuestProfilerSample(
      long profilerHandle, long storeHandle, long deltaNanos);

  /**
   * Records a call hook transition.
   *
   * @param profilerHandle the native profiler handle
   * @param storeHandle the native store handle
   * @param hookKind 0-3 corresponding to CallHook variants
   * @return 0 on success, -1 on error, -2 if already finished
   */
  private static native int nativeGuestProfilerCallHook(
      long profilerHandle, long storeHandle, int hookKind);

  /**
   * Finishes profiling and returns the profile data.
   *
   * @param profilerHandle the native profiler handle
   * @return the profile JSON data as bytes, or null on error
   */
  private static native byte[] nativeGuestProfilerFinish(long profilerHandle);

  /**
   * Destroys the native profiler.
   *
   * @param profilerHandle the native profiler handle
   */
  private static native void nativeGuestProfilerDestroy(long profilerHandle);
}
