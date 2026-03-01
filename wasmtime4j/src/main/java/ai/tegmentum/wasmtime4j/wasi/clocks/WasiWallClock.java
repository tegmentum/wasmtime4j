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

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * WASI wall clock interface for reporting the current date and time.
 *
 * <p>This clock provides real-world time as seconds and nanoseconds since the Unix epoch
 * (1970-01-01T00:00:00Z). Unlike the monotonic clock, this clock is not guaranteed to be monotonic
 * and may be reset by system time adjustments.
 *
 * <p>This interface should be used for displaying the current date and time to users, timestamping
 * events, and other calendar/clock applications. For measuring elapsed time and implementing
 * timeouts, use {@link WasiMonotonicClock} instead.
 *
 * <p>WASI Preview 2 specification: wasi:clocks/wall-clock@0.2.8
 */
public interface WasiWallClock {

  /**
   * Gets the current wall clock time.
   *
   * <p>Returns the current real-world time as a {@link DateTime} representing seconds and
   * nanoseconds since the Unix epoch (1970-01-01T00:00:00Z).
   *
   * <p>This clock is not monotonic, so successive calls may return non-increasing values if the
   * system time is adjusted.
   *
   * @return current date and time
   * @throws WasmException if reading the clock fails
   */
  DateTime now();

  /**
   * Gets the resolution of the wall clock.
   *
   * <p>The resolution represents the duration of a single clock tick, indicating the precision of
   * the clock.
   *
   * @return clock resolution as a {@link DateTime}
   * @throws WasmException if reading the resolution fails
   */
  DateTime resolution();
}
