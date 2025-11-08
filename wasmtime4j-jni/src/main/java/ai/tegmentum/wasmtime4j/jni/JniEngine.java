package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * JNI implementation of the Engine interface.
 *
 * @since 1.0.0
 */
public class JniEngine implements Engine {
  private final long nativeHandle;
  private volatile boolean closed = false;

  /**
   * Creates a new JNI engine with the given native handle.
   *
   * @param nativeHandle the native handle
   */
  public JniEngine(final long nativeHandle) {
    this.nativeHandle = nativeHandle;
  }

  /**
   * Gets the native handle.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  @Override
  public Store createStore() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

    final long storeHandle = nativeCreateStore(nativeHandle);
    if (storeHandle == 0) {
      throw new WasmException("Failed to create store");
    }
    return new JniStore(storeHandle, this);
  }

  @Override
  public Store createStore(final Object data) throws WasmException {
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

    // TODO: Call native method to create store with user data
    // long storeHandle = nativeCreateStoreWithData(nativeHandle, data);
    // return new JniStore(storeHandle, this, data);
    throw new UnsupportedOperationException(
        "Store creation with data not yet implemented - native library required");
  }

  @Override
  public boolean isEpochInterruptionEnabled() {
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeIsEpochInterruptionEnabled(nativeHandle);
  }

  @Override
  public boolean isFuelEnabled() {
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeIsFuelEnabled(nativeHandle);
  }

  @Override
  public long getStackSizeLimit() {
    if (closed || nativeHandle == 0) {
      return 0;
    }
    return nativeGetStackSizeLimit(nativeHandle);
  }

  @Override
  public int getMemoryLimitPages() {
    if (closed || nativeHandle == 0) {
      return 0;
    }
    return nativeGetMemoryLimitPages(nativeHandle);
  }

  @Override
  public boolean supportsFeature(final ai.tegmentum.wasmtime4j.WasmFeature feature) {
    if (feature == null) {
      return false;
    }
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeSupportsFeature(nativeHandle, feature.name());
  }

  @Override
  public boolean isValid() {
    return !closed && nativeHandle != 0;
  }

  @Override
  public void incrementEpoch() {
    // No-op for JNI engine - epoch interruption handled at store level
  }

  @Override
  public Module compileModule(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be empty");
    }
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

    final long moduleHandle = nativeCompileModule(nativeHandle, wasmBytes);
    if (moduleHandle == 0) {
      throw new WasmException("Failed to compile module from bytes");
    }
    return new JniModule(moduleHandle, this);
  }

  @Override
  public Module compileWat(final String wat) throws WasmException {
    if (wat == null) {
      throw new IllegalArgumentException("wat cannot be null");
    }
    if (wat.isEmpty()) {
      throw new IllegalArgumentException("wat cannot be empty");
    }
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

    final long moduleHandle = nativeCompileWat(nativeHandle, wat);
    if (moduleHandle == 0) {
      throw new WasmException("Failed to compile WAT");
    }
    return new JniModule(moduleHandle, this);
  }

  private native long nativeCompileModule(long engineHandle, byte[] wasmBytes);

  private native long nativeCompileWat(long engineHandle, String wat);

  @Override
  public ai.tegmentum.wasmtime4j.StreamingCompiler createStreamingCompiler() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

    // TODO: Call native method to create streaming compiler
    // long compilerHandle = nativeCreateStreamingCompiler(nativeHandle);
    // if (compilerHandle == 0) {
    //   throw new WasmException("Failed to create streaming compiler");
    // }
    // return new JniStreamingCompiler(compilerHandle, this);
    throw new UnsupportedOperationException(
        "Streaming compiler creation not yet implemented - " + "native library required");
  }

  @Override
  public EngineConfig getConfig() {
    // TODO: Implement config retrieval
    return null;
  }

  @Override
  public long getReferenceCount() {
    // TODO: Implement reference counting
    return 1;
  }

  @Override
  public int getMaxInstances() {
    // TODO: Implement max instances tracking
    return Integer.MAX_VALUE;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      nativeDestroyEngine(nativeHandle);
    }
  }

  private native long nativeCreateStore(long engineHandle);

  private native void nativeDestroyEngine(long handle);

  private native boolean nativeIsEpochInterruptionEnabled(long engineHandle);

  private native boolean nativeIsFuelEnabled(long engineHandle);

  private native long nativeGetStackSizeLimit(long engineHandle);

  private native int nativeGetMemoryLimitPages(long engineHandle);

  private native boolean nativeSupportsFeature(long engineHandle, String featureName);
}
