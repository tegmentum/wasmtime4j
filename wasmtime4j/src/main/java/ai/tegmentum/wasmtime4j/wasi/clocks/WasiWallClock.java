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
