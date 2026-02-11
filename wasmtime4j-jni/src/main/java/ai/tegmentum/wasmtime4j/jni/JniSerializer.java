/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.config.Serializer;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * JNI implementation of the Serializer interface.
 *
 * <p>This serializer provides efficient serialization and deserialization of compiled WebAssembly
 * modules with caching support. It uses native Wasmtime serialization with optional compression.
 *
 * @since 1.0.0
 */
public final class JniSerializer implements Serializer {

  private final long nativeHandle;
  private volatile boolean closed;

  /**
   * Creates a new serializer with default configuration.
   *
   * @param nativeHandle the native serializer handle
   */
  public JniSerializer(final long nativeHandle) {
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Invalid native handle");
    }
    this.nativeHandle = nativeHandle;
    this.closed = false;
  }

  @Override
  public byte[] serialize(final Engine engine, final byte[] moduleBytes) throws WasmException {
    ensureNotClosed();

    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (moduleBytes == null) {
      throw new IllegalArgumentException("Module bytes cannot be null");
    }
    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine for JNI serializer");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    return nativeSerialize(nativeHandle, jniEngine.getNativeHandle(), moduleBytes);
  }

  @Override
  public Module deserialize(final Engine engine, final byte[] serializedBytes)
      throws WasmException {
    ensureNotClosed();

    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (serializedBytes == null) {
      throw new IllegalArgumentException("Serialized bytes cannot be null");
    }
    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine for JNI serializer");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    final long moduleHandle =
        nativeDeserialize(nativeHandle, jniEngine.getNativeHandle(), serializedBytes);

    if (moduleHandle == 0) {
      throw new WasmException("Failed to deserialize module");
    }

    return new JniModule(moduleHandle, engine);
  }

  @Override
  public boolean clearCache() throws WasmException {
    ensureNotClosed();
    return nativeClearCache(nativeHandle);
  }

  @Override
  public int getCacheEntryCount() {
    ensureNotClosed();
    return nativeGetCacheEntryCount(nativeHandle);
  }

  @Override
  public long getCacheTotalSize() {
    ensureNotClosed();
    return nativeGetCacheTotalSize(nativeHandle);
  }

  @Override
  public double getCacheHitRate() {
    ensureNotClosed();
    return nativeGetCacheHitRate(nativeHandle);
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      nativeDestroy(nativeHandle);
    }
  }

  /**
   * Gets the native handle for this serializer.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    ensureNotClosed();
    return nativeHandle;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Serializer has been closed");
    }
  }

  // Native method declarations
  private static native byte[] nativeSerialize(
      long serializerHandle, long engineHandle, byte[] moduleBytes) throws WasmException;

  private static native long nativeDeserialize(
      long serializerHandle, long engineHandle, byte[] serializedBytes) throws WasmException;

  private static native boolean nativeClearCache(long serializerHandle) throws WasmException;

  private static native int nativeGetCacheEntryCount(long serializerHandle);

  private static native long nativeGetCacheTotalSize(long serializerHandle);

  private static native double nativeGetCacheHitRate(long serializerHandle);

  private static native void nativeDestroy(long serializerHandle);
}
