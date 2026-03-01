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
package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasiException;
import java.util.concurrent.TimeUnit;

/**
 * Shared base class for WASI time and clock operations.
 *
 * <p>Provides constants, static utility methods, convenience methods, and validation shared between
 * JNI and Panama implementations. Subclasses implement the native-specific core operations.
 *
 * @since 1.0.0
 */
public abstract class AbstractWasiTimeOperations {

  /** Clock ID for wall clock time (since Unix epoch). */
  public static final int WASI_CLOCK_REALTIME = WasiClockId.REALTIME.getValue();

  /** Clock ID for monotonic time (suitable for measuring time intervals). */
  public static final int WASI_CLOCK_MONOTONIC = WasiClockId.MONOTONIC.getValue();

  /** Clock ID for process CPU time. */
  public static final int WASI_CLOCK_PROCESS_CPUTIME_ID = WasiClockId.PROCESS_CPUTIME_ID.getValue();

  /** Clock ID for thread CPU time. */
  public static final int WASI_CLOCK_THREAD_CPUTIME_ID = WasiClockId.THREAD_CPUTIME_ID.getValue();

  /** Maximum valid clock ID value. */
  protected static final int MAX_CLOCK_ID = WasiClockId.MAX_CLOCK_ID;

  /** Protected constructor for subclasses. */
  protected AbstractWasiTimeOperations() {}

  /**
   * Gets the resolution of the specified clock.
   *
   * @param clockId the clock identifier (one of the WASI_CLOCK_* constants)
   * @return the clock resolution in nanoseconds
   * @throws WasiException if the clock ID is invalid or the operation fails
   */
  public abstract long getClockResolution(int clockId) throws WasiException;

  /**
   * Gets the current time for the specified clock.
   *
   * @param clockId the clock identifier (one of the WASI_CLOCK_* constants)
   * @param precision the requested time precision in nanoseconds (0 for maximum precision)
   * @return the current time in nanoseconds since the clock's epoch
   * @throws WasiException if the clock ID is invalid or the operation fails
   */
  public abstract long getCurrentTime(int clockId, long precision) throws WasiException;

  /**
   * Gets the current time for the specified clock with maximum precision.
   *
   * <p>Convenience method equivalent to calling {@link #getCurrentTime(int, long)} with precision
   * 0.
   *
   * @param clockId the clock identifier (one of the WASI_CLOCK_* constants)
   * @return the current time in nanoseconds since the clock's epoch
   * @throws WasiException if the clock ID is invalid or the operation fails
   */
  public long getCurrentTime(final int clockId) throws WasiException {
    return getCurrentTime(clockId, 0);
  }

  /**
   * Gets the current realtime (wall clock time) in nanoseconds since Unix epoch.
   *
   * @return the current realtime in nanoseconds since Unix epoch
   * @throws WasiException if the operation fails
   */
  public long getRealtime() throws WasiException {
    return getCurrentTime(WASI_CLOCK_REALTIME);
  }

  /**
   * Gets the current monotonic time in nanoseconds since system boot.
   *
   * @return the current monotonic time in nanoseconds since system boot
   * @throws WasiException if the operation fails
   */
  public long getMonotonicTime() throws WasiException {
    return getCurrentTime(WASI_CLOCK_MONOTONIC);
  }

  /**
   * Gets the current process CPU time in nanoseconds.
   *
   * @return the current process CPU time in nanoseconds
   * @throws WasiException if the operation fails
   */
  public long getProcessCpuTime() throws WasiException {
    return getCurrentTime(WASI_CLOCK_PROCESS_CPUTIME_ID);
  }

  /**
   * Gets the current thread CPU time in nanoseconds.
   *
   * @return the current thread CPU time in nanoseconds
   * @throws WasiException if the operation fails
   */
  public long getThreadCpuTime() throws WasiException {
    return getCurrentTime(WASI_CLOCK_THREAD_CPUTIME_ID);
  }

  /**
   * Converts nanoseconds to the specified time unit.
   *
   * @param nanoseconds the time in nanoseconds
   * @param unit the target time unit
   * @return the time converted to the specified unit
   */
  public static long convertTime(final long nanoseconds, final TimeUnit unit) {
    return WasiClockId.convertTime(nanoseconds, unit);
  }

  /**
   * Checks if the specified clock ID is supported.
   *
   * @param clockId the clock identifier to check
   * @return true if the clock ID is supported, false otherwise
   */
  public static boolean isClockSupported(final int clockId) {
    return WasiClockId.isClockSupported(clockId);
  }

  /**
   * Gets a human-readable name for the specified clock ID.
   *
   * @param clockId the clock identifier
   * @return the human-readable clock name
   */
  public static String getClockName(final int clockId) {
    return WasiClockId.getClockName(clockId);
  }

  /**
   * Validates that the specified clock ID is valid.
   *
   * @param clockId the clock ID to validate
   * @throws IllegalArgumentException if the clock ID is invalid
   */
  protected static void validateClockId(final int clockId) {
    if (!isClockSupported(clockId)) {
      throw new IllegalArgumentException(
          "Invalid clock ID: " + clockId + " (valid range: 0-" + MAX_CLOCK_ID + ")");
    }
  }
}
