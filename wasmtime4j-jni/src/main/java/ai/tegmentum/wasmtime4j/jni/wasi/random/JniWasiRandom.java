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

package ai.tegmentum.wasmtime4j.jni.wasi.random;

import ai.tegmentum.wasmtime4j.wasi.random.WasiRandom;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiRandom interface.
 *
 * <p>This class provides access to WASI Preview 2 random data generation through JNI calls to the
 * native Wasmtime library. It generates cryptographically-secure random data suitable for
 * security-sensitive applications.
 *
 * <p>All random data returned is:
 *
 * <ul>
 *   <li>Cryptographically-secure (equivalent to properly seeded CSPRNG)
 *   <li>Unpredictable and fresh
 *   <li>Non-blocking under all circumstances
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniWasiRandom implements WasiRandom {

  private static final Logger LOGGER = Logger.getLogger(JniWasiRandom.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiRandom: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI random generator with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is 0
   */
  public JniWasiRandom(final long contextHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI random with context handle: " + contextHandle);
  }

  @Override
  public byte[] getRandomBytes(final long len) {
    if (len < 0) {
      throw new IllegalArgumentException("Length cannot be negative: " + len);
    }
    if (len == 0) {
      return new byte[0];
    }
    return nativeGetRandomBytes(contextHandle, len);
  }

  @Override
  public long getRandomU64() {
    return nativeGetRandomU64(contextHandle);
  }

  /**
   * Native method to get cryptographically-secure random bytes.
   *
   * @param contextHandle the native context handle
   * @param len number of random bytes to generate
   * @return byte array containing random data
   */
  private static native byte[] nativeGetRandomBytes(long contextHandle, long len);

  /**
   * Native method to get a cryptographically-secure random unsigned 64-bit integer.
   *
   * @param contextHandle the native context handle
   * @return random unsigned 64-bit integer
   */
  private static native long nativeGetRandomU64(long contextHandle);
}
