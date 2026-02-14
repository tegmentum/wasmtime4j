/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.jni.wasi.keyvalue;

import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueEntry;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueException;
import ai.tegmentum.wasmtime4j.wasi.keyvalue.WasiKeyValue;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI keyvalue interface.
 *
 * <p>This implementation uses JNI to call native WASI keyvalue functions.
 *
 * @since 1.0.0
 */
public final class JniWasiKeyValue implements WasiKeyValue {

  private static final Logger LOGGER = Logger.getLogger(JniWasiKeyValue.class.getName());

  /** Native context handle. */
  private volatile long contextHandle;

  /** Whether the store is closed. */
  private volatile boolean closed = false;

  // Native library loading
  static {
    try {
      System.loadLibrary("wasmtime4j");
    } catch (UnsatisfiedLinkError e) {
      LOGGER.log(Level.SEVERE, "Failed to load native library", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  // Native method declarations
  private static native long nativeCreateContext();

  private static native void nativeDestroyContext(long handle);

  private static native byte[] nativeGet(long handle, String key);

  private static native boolean nativeSet(long handle, String key, byte[] value);

  private static native boolean nativeDelete(long handle, String key);

  private static native boolean nativeExists(long handle, String key);

  private static native String nativeKeys(long handle);

  private static native boolean nativeIsAvailable();

  /**
   * Creates a new JNI WASI keyvalue store.
   *
   * @throws KeyValueException if creation fails
   */
  public JniWasiKeyValue() throws KeyValueException {
    this.contextHandle = nativeCreateContext();
    if (this.contextHandle == 0) {
      throw new KeyValueException("Failed to create keyvalue context");
    }
    LOGGER.log(Level.FINE, "Created JNI WASI keyvalue context: {0}", contextHandle);
  }

  /**
   * Checks if WASI keyvalue support is available.
   *
   * @return true if available
   */
  public static boolean isAvailable() {
    try {
      return nativeIsAvailable();
    } catch (Throwable t) {
      return false;
    }
  }

  private void ensureOpen() throws KeyValueException {
    if (closed) {
      throw new KeyValueException("Keyvalue store is closed");
    }
  }

  @Override
  public Optional<byte[]> get(final String key) throws KeyValueException {
    ensureOpen();
    byte[] result = nativeGet(contextHandle, key);
    return Optional.ofNullable(result);
  }

  @Override
  public Optional<KeyValueEntry> getEntry(final String key) throws KeyValueException {
    return get(key).map(value -> KeyValueEntry.builder(key, value).build());
  }

  @Override
  public void set(final String key, final byte[] value) throws KeyValueException {
    ensureOpen();
    if (!nativeSet(contextHandle, key, value)) {
      throw new KeyValueException("Failed to set value for key: " + key);
    }
  }

  @Override
  public boolean delete(final String key) throws KeyValueException {
    ensureOpen();
    return nativeDelete(contextHandle, key);
  }

  @Override
  public boolean exists(final String key) throws KeyValueException {
    ensureOpen();
    return nativeExists(contextHandle, key);
  }

  @Override
  public Set<String> keys() throws KeyValueException {
    ensureOpen();
    String json = nativeKeys(contextHandle);
    if (json == null) {
      return new HashSet<>();
    }

    // Parse JSON array (simple implementation)
    Set<String> keySet = new HashSet<>();
    if (json.startsWith("[") && json.endsWith("]")) {
      String content = json.substring(1, json.length() - 1);
      if (!content.isEmpty()) {
        String[] parts = content.split(",");
        for (String part : parts) {
          String trimmed = part.trim();
          if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            keySet.add(trimmed.substring(1, trimmed.length() - 1));
          }
        }
      }
    }
    return keySet;
  }

  @Override
  public void close() throws KeyValueException {
    if (closed) {
      return;
    }
    closed = true;

    if (contextHandle != 0) {
      nativeDestroyContext(contextHandle);
      contextHandle = 0;
    }
  }
}
