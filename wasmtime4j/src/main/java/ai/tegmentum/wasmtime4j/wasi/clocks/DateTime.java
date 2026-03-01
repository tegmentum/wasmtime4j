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
package ai.tegmentum.wasmtime4j.wasi.clocks;

/**
 * Represents a point in time as seconds and nanoseconds since the Unix epoch
 * (1970-01-01T00:00:00Z).
 *
 * <p>This is used by the wall clock interface to report the current date and time. The wall clock
 * is not monotonic and may be reset, so successive calls to {@code now()} may return non-increasing
 * values.
 *
 * <p>WASI Preview 2 specification: wasi:clocks/wall-clock@0.2.8
 */
public final class DateTime {

  private final long seconds;
  private final int nanoseconds;

  /**
   * Creates a new DateTime.
   *
   * @param seconds seconds since Unix epoch (1970-01-01T00:00:00Z)
   * @param nanoseconds nanoseconds component (must be less than 1,000,000,000)
   * @throws IllegalArgumentException if nanoseconds is &gt;= 1,000,000,000
   */
  public DateTime(final long seconds, final int nanoseconds) {
    if (nanoseconds < 0 || nanoseconds >= 1_000_000_000) {
      throw new IllegalArgumentException(
          "nanoseconds must be in range [0, 1000000000), got: " + nanoseconds);
    }
    this.seconds = seconds;
    this.nanoseconds = nanoseconds;
  }

  /**
   * Gets the seconds component since Unix epoch.
   *
   * @return seconds since 1970-01-01T00:00:00Z
   */
  public long getSeconds() {
    return seconds;
  }

  /**
   * Gets the nanoseconds component.
   *
   * @return nanoseconds (0 to 999,999,999)
   */
  public int getNanoseconds() {
    return nanoseconds;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DateTime)) {
      return false;
    }
    final DateTime other = (DateTime) obj;
    return seconds == other.seconds && nanoseconds == other.nanoseconds;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(seconds) * 31 + Integer.hashCode(nanoseconds);
  }

  @Override
  public String toString() {
    return "DateTime{seconds=" + seconds + ", nanoseconds=" + nanoseconds + '}';
  }
}
