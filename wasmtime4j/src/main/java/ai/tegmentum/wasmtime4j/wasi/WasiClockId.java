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

import java.util.concurrent.TimeUnit;

/**
 * WASI clock identifiers for time operations.
 *
 * <p>These identifiers specify which clock to use for time-related WASI operations. Also provides
 * shared utility methods for clock validation and time conversion used by both JNI and Panama
 * implementations.
 *
 * @since 1.0.0
 */
public enum WasiClockId {
  /** Realtime clock, measuring real (i.e., wall-clock) time. */
  REALTIME(0),

  /** Monotonic clock, measuring elapsed time. */
  MONOTONIC(1),

  /** Process CPU-time clock. */
  PROCESS_CPUTIME_ID(2),

  /** Thread CPU-time clock. */
  THREAD_CPUTIME_ID(3);

  /** Maximum valid clock ID value. */
  public static final int MAX_CLOCK_ID = 3;

  private final int value;

  WasiClockId(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this clock ID.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets a WasiClockId by its numeric value.
   *
   * @param value the numeric value
   * @return the corresponding WasiClockId
   * @throws IllegalArgumentException if the value is invalid
   */
  public static WasiClockId fromValue(final int value) {
    for (final WasiClockId clockId : values()) {
      if (clockId.value == value) {
        return clockId;
      }
    }
    throw new IllegalArgumentException("Invalid clock ID: " + value);
  }

  /**
   * Checks if the specified clock ID value is supported.
   *
   * @param clockId the clock identifier value to check
   * @return true if the clock ID is supported, false otherwise
   */
  public static boolean isClockSupported(final int clockId) {
    return clockId >= 0 && clockId <= MAX_CLOCK_ID;
  }

  /**
   * Gets a human-readable name for the specified clock ID value.
   *
   * @param clockId the clock identifier value
   * @return the human-readable clock name
   */
  public static String getClockName(final int clockId) {
    switch (clockId) {
      case 0:
        return "REALTIME";
      case 1:
        return "MONOTONIC";
      case 2:
        return "PROCESS_CPUTIME";
      case 3:
        return "THREAD_CPUTIME";
      default:
        return "UNKNOWN(" + clockId + ")";
    }
  }

  /**
   * Converts nanoseconds to the specified time unit.
   *
   * @param nanoseconds the time in nanoseconds
   * @param unit the target time unit
   * @return the time converted to the specified unit
   * @throws IllegalArgumentException if unit is null
   */
  public static long convertTime(final long nanoseconds, final TimeUnit unit) {
    if (unit == null) {
      throw new IllegalArgumentException("unit must not be null");
    }
    return unit.convert(nanoseconds, TimeUnit.NANOSECONDS);
  }
}
