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

package ai.tegmentum.wasmtime4j.jni.wasi.clocks;

import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiWallClock;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiWallClock interface.
 *
 * <p>This class provides access to WASI Preview 2 wall clock operations through JNI calls to the
 * native Wasmtime library. The wall clock reports real-world time as seconds and nanoseconds since
 * the Unix epoch (1970-01-01T00:00:00Z).
 *
 * <p>Use cases:
 *
 * <ul>
 *   <li>Timestamping log entries and events
 *   <li>Displaying current date and time to users
 *   <li>Calendar and scheduling operations
 *   <li>Audit trails and transaction records
 * </ul>
 *
 * <p><b>Note:</b> This clock is not monotonic and may be affected by system time adjustments. For
 * measuring elapsed time, use {@link JniWasiMonotonicClock} instead.
 *
 * @since 1.0.0
 */
public final class JniWasiWallClock implements WasiWallClock {

  private static final Logger LOGGER = Logger.getLogger(JniWasiWallClock.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiWallClock: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI wall clock with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is 0
   */
  public JniWasiWallClock(final long contextHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI wall clock with context handle: " + contextHandle);
  }

  @Override
  public DateTime now() {
    final long[] result = nativeNow(contextHandle);
    if (result == null || result.length != 2) {
      throw new RuntimeException("Failed to get wall clock time");
    }
    return new DateTime(result[0], (int) result[1]);
  }

  @Override
  public DateTime resolution() {
    final long[] result = nativeResolution(contextHandle);
    if (result == null || result.length != 2) {
      throw new RuntimeException("Failed to get wall clock resolution");
    }
    return new DateTime(result[0], (int) result[1]);
  }

  /**
   * Native method to get the current wall clock time.
   *
   * @param contextHandle the native context handle
   * @return array of [seconds, nanoseconds] since Unix epoch
   */
  private static native long[] nativeNow(long contextHandle);

  /**
   * Native method to get the wall clock resolution.
   *
   * @param contextHandle the native context handle
   * @return array of [seconds, nanoseconds] representing clock resolution
   */
  private static native long[] nativeResolution(long contextHandle);
}
