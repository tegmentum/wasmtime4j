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
 * WASI timezone interface for retrieving timezone information.
 *
 * <p>Provides access to timezone data including UTC offsets, timezone names, and daylight saving
 * time status for a given point in time.
 *
 * <p>If timezone determination fails for a datetime, implementations return UTC with zero offset
 * and no daylight saving time active.
 *
 * <p>WASI Preview 2 specification: wasi:clocks/timezone@0.2.8
 *
 * @unstable This interface is marked as unstable in WASI (feature = clocks-timezone)
 */
public interface WasiTimezone {

  /**
   * Gets comprehensive timezone information for a specific datetime.
   *
   * <p>Returns timezone display information including UTC offset, timezone name abbreviation, and
   * daylight saving time status.
   *
   * <p>If timezone determination fails, returns UTC with zero offset and no DST.
   *
   * @param when datetime to get timezone information for
   * @return timezone display information
   * @throws WasmException if retrieving timezone information fails
   * @throws IllegalArgumentException if when is null
   */
  TimezoneDisplay display(DateTime when);

  /**
   * Gets the UTC offset in seconds for a specific datetime.
   *
   * <p>Returns the number of seconds to add to UTC to get local time. Positive values are east of
   * UTC, negative values are west.
   *
   * @param when datetime to get UTC offset for
   * @return UTC offset in seconds (less than 86,400)
   * @throws WasmException if retrieving the offset fails
   * @throws IllegalArgumentException if when is null
   */
  int utcOffset(DateTime when);
}
