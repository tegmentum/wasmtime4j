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

package ai.tegmentum.wasmtime4j;

import java.time.Instant;

/**
 * WASI Preview 2 Wall Clock interface.
 *
 * <p>Provides access to the system wall clock, which represents time as seen by an external
 * observer. Unlike the monotonic clock, the wall clock can be adjusted by the system and may go
 * backwards.
 *
 * <p>This interface maps to the wasi:clocks/wall-clock@0.2.0 WIT interface.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiWallClock clock = runtime.getWallClock();
 * WasiWallClock.Datetime datetime = clock.now();
 * System.out.println("Current time: " + datetime.toInstant());
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiWallClock {

  /**
   * Returns the current wall clock time.
   *
   * @return the current wall clock time as a datetime structure
   */
  Datetime now();

  /**
   * Returns the clock resolution.
   *
   * <p>This indicates the smallest time interval that the clock can measure.
   *
   * @return the clock resolution as a datetime structure
   */
  Datetime resolution();

  /**
   * Represents a wall clock datetime value.
   *
   * <p>The datetime is split into seconds and nanoseconds to provide high precision while
   * supporting a wide range of values.
   */
  final class Datetime {

    private final long seconds;
    private final int nanoseconds;

    /**
     * Creates a new datetime value.
     *
     * @param seconds the number of seconds since the Unix epoch (1970-01-01T00:00:00Z)
     * @param nanoseconds the additional nanoseconds (0-999999999)
     */
    public Datetime(final long seconds, final int nanoseconds) {
      if (nanoseconds < 0 || nanoseconds > 999_999_999) {
        throw new IllegalArgumentException(
            "Nanoseconds must be between 0 and 999999999, got: " + nanoseconds);
      }
      this.seconds = seconds;
      this.nanoseconds = nanoseconds;
    }

    /**
     * Gets the number of seconds since the Unix epoch.
     *
     * @return the seconds component
     */
    public long getSeconds() {
      return seconds;
    }

    /**
     * Gets the additional nanoseconds.
     *
     * @return the nanoseconds component (0-999999999)
     */
    public int getNanoseconds() {
      return nanoseconds;
    }

    /**
     * Converts this datetime to a Java Instant.
     *
     * @return an Instant representing this datetime
     */
    public Instant toInstant() {
      return Instant.ofEpochSecond(seconds, nanoseconds);
    }

    /**
     * Creates a Datetime from a Java Instant.
     *
     * @param instant the instant to convert
     * @return a new Datetime representing the same point in time
     */
    public static Datetime fromInstant(final Instant instant) {
      return new Datetime(instant.getEpochSecond(), instant.getNano());
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Datetime datetime = (Datetime) obj;
      return seconds == datetime.seconds && nanoseconds == datetime.nanoseconds;
    }

    @Override
    public int hashCode() {
      return 31 * Long.hashCode(seconds) + nanoseconds;
    }

    @Override
    public String toString() {
      return "Datetime{seconds=" + seconds + ", nanoseconds=" + nanoseconds + "}";
    }
  }
}
