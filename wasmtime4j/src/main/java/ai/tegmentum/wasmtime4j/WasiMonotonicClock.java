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

/**
 * WASI Preview 2 Monotonic Clock interface.
 *
 * <p>Provides access to a high-resolution monotonic clock suitable for measuring elapsed time. The
 * clock is guaranteed to be non-decreasing and is not affected by system time changes.
 *
 * <p>This interface maps to the wasi:clocks/monotonic-clock@0.2.0 WIT interface.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiMonotonicClock clock = runtime.getMonotonicClock();
 * long start = clock.now();
 * // ... perform operation ...
 * long elapsed = clock.now() - start;
 * System.out.println("Elapsed nanoseconds: " + elapsed);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiMonotonicClock {

  /**
   * Returns the current time in nanoseconds.
   *
   * <p>This is a monotonically increasing value that is suitable for measuring elapsed time. The
   * value has no defined relationship to wall clock time.
   *
   * @return the current monotonic time in nanoseconds
   */
  long now();

  /**
   * Returns the clock resolution in nanoseconds.
   *
   * <p>This indicates the smallest time interval that the clock can measure.
   *
   * @return the clock resolution in nanoseconds
   */
  long resolution();

  /**
   * Subscribes to receive a pollable handle that resolves at the specified instant.
   *
   * <p>The pollable will be ready when the clock reaches or exceeds the specified instant.
   *
   * @param when the instant to wait for, in nanoseconds from the clock's epoch
   * @return a pollable handle that can be used with the wasi:io/poll interface
   */
  long subscribeInstant(long when);

  /**
   * Subscribes to receive a pollable handle that resolves after the specified duration.
   *
   * <p>The pollable will be ready after the specified number of nanoseconds have elapsed.
   *
   * @param duration the duration to wait, in nanoseconds
   * @return a pollable handle that can be used with the wasi:io/poll interface
   */
  long subscribeDuration(long duration);
}
