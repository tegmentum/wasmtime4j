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
import ai.tegmentum.wasmtime4j.wasi.clocks.TimezoneDisplay;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiTimezone;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiTimezone interface.
 *
 * <p>This class provides access to WASI Preview 2 timezone operations through JNI calls to the
 * native Wasmtime library. It provides timezone information including UTC offsets, timezone names,
 * and daylight saving time status.
 *
 * <p>Use cases:
 *
 * <ul>
 *   <li>Converting between UTC and local time
 *   <li>Displaying timezone information to users
 *   <li>Handling daylight saving time transitions
 *   <li>Calendar and scheduling applications
 * </ul>
 *
 * <p><b>Note:</b> This interface is marked as unstable in WASI Preview 2.
 *
 * @since 1.0.0
 */
public final class JniWasiTimezone implements WasiTimezone {

  private static final Logger LOGGER = Logger.getLogger(JniWasiTimezone.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiTimezone: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI timezone with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is 0
   */
  public JniWasiTimezone(final long contextHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI timezone with context handle: " + contextHandle);
  }

  @Override
  public TimezoneDisplay display(final DateTime when) {
    if (when == null) {
      throw new IllegalArgumentException("DateTime cannot be null");
    }
    final TimezoneDisplay result =
        nativeDisplay(contextHandle, when.getSeconds(), when.getNanoseconds());
    if (result == null) {
      throw new RuntimeException("Failed to get timezone display");
    }
    return result;
  }

  @Override
  public int utcOffset(final DateTime when) {
    if (when == null) {
      throw new IllegalArgumentException("DateTime cannot be null");
    }
    return nativeUtcOffset(contextHandle, when.getSeconds(), when.getNanoseconds());
  }

  /**
   * Native method to get timezone display information.
   *
   * @param contextHandle the native context handle
   * @param seconds seconds since Unix epoch
   * @param nanoseconds nanoseconds component
   * @return TimezoneDisplay object
   */
  private static native TimezoneDisplay nativeDisplay(
      long contextHandle, long seconds, int nanoseconds);

  /**
   * Native method to get UTC offset in seconds.
   *
   * @param contextHandle the native context handle
   * @param seconds seconds since Unix epoch
   * @param nanoseconds nanoseconds component
   * @return UTC offset in seconds
   */
  private static native int nativeUtcOffset(long contextHandle, long seconds, int nanoseconds);
}
