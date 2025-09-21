package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * WASI Preview 1 clock and time interface providing comprehensive time access and operations.
 *
 * <p>This interface provides access to various clock types and time-related operations within the
 * WASI sandbox, including realtime clocks, monotonic clocks, process CPU time, and sleep
 * operations.
 *
 * <p>Clock operations include:
 *
 * <ul>
 *   <li>Reading various clock types (realtime, monotonic, CPU time)
 *   <li>High-resolution timing and clock resolution queries
 *   <li>Sleep and delay operations
 *   <li>Event polling with timeouts
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiContext context = WasiFactory.createContext();
 * WasiClock clock = context.getClock();
 *
 * // Get current realtime
 * long realtimeNanos = clock.getRealtimeClock();
 * Instant now = clock.getClockTime(WasiClockType.REALTIME);
 *
 * // Sleep for 1 second
 * clock.sleep(Duration.ofSeconds(1));
 *
 * // Get process CPU time
 * long cpuTime = clock.getProcessCputimeClock();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiClock {

  /**
   * Gets the current realtime clock value in nanoseconds.
   *
   * <p>The realtime clock represents wall-clock time and corresponds to the system's idea of the
   * current time. This clock is affected by system clock adjustments and may go backwards.
   *
   * @return the current realtime in nanoseconds since the Unix epoch
   * @throws WasmException if reading the clock fails
   */
  long getRealtimeClock() throws WasmException;

  /**
   * Gets the current monotonic clock value in nanoseconds.
   *
   * <p>The monotonic clock represents time since some unspecified starting point and is guaranteed
   * to be monotonically increasing. This clock is not affected by system clock adjustments and is
   * ideal for measuring elapsed time.
   *
   * @return the current monotonic time in nanoseconds
   * @throws WasmException if reading the clock fails
   */
  long getMonotonicClock() throws WasmException;

  /**
   * Gets the current process CPU time in nanoseconds.
   *
   * <p>This represents the total CPU time consumed by the current process, including both user and
   * system time. This clock only advances when the process is actually executing.
   *
   * @return the process CPU time in nanoseconds
   * @throws WasmException if reading the clock fails
   */
  long getProcessCputimeClock() throws WasmException;

  /**
   * Gets the current thread CPU time in nanoseconds.
   *
   * <p>This represents the CPU time consumed by the current thread. This clock only advances when
   * the current thread is actually executing.
   *
   * @return the thread CPU time in nanoseconds
   * @throws WasmException if reading the clock fails
   */
  long getThreadCputimeClock() throws WasmException;

  /**
   * Gets the resolution (precision) of the specified clock type.
   *
   * <p>The resolution indicates the smallest time increment that the clock can measure. A smaller
   * value indicates higher precision.
   *
   * @param clockType the type of clock to query
   * @return the clock resolution as a Duration
   * @throws WasmException if getting the resolution fails
   * @throws IllegalArgumentException if clockType is null
   */
  Duration getClockResolution(final WasiClockType clockType) throws WasmException;

  /**
   * Gets the current time for the specified clock type as an Instant.
   *
   * <p>This method provides a higher-level interface for reading clocks, returning an Instant
   * object instead of raw nanoseconds. For realtime clocks, this will be an absolute timestamp. For
   * other clock types, the Instant represents time relative to the clock's epoch.
   *
   * @param clockType the type of clock to read
   * @return the current time as an Instant
   * @throws WasmException if reading the clock fails
   * @throws IllegalArgumentException if clockType is null
   */
  Instant getClockTime(final WasiClockType clockType) throws WasmException;

  /**
   * Sleeps for the specified duration.
   *
   * <p>This method suspends execution of the current thread for at least the specified duration.
   * The actual sleep time may be longer due to system scheduling and timer resolution.
   *
   * @param duration the duration to sleep
   * @throws WasmException if the sleep operation fails
   * @throws IllegalArgumentException if duration is null or negative
   * @throws InterruptedException if the sleep is interrupted
   */
  void sleep(final Duration duration) throws WasmException, InterruptedException;

  /**
   * Sleeps until the specified deadline.
   *
   * <p>This method suspends execution until the specified absolute time is reached. The deadline is
   * interpreted as a realtime timestamp.
   *
   * @param deadline the absolute time to sleep until
   * @throws WasmException if the sleep operation fails
   * @throws IllegalArgumentException if deadline is null or in the past
   * @throws InterruptedException if the sleep is interrupted
   */
  void sleepUntil(final Instant deadline) throws WasmException, InterruptedException;

  /**
   * Performs a blocking poll operation with a timeout.
   *
   * <p>This method blocks until one or more of the specified subscriptions become ready or the
   * timeout expires. This is the WASI equivalent of select()/poll() system calls.
   *
   * @param subscriptions the list of events to wait for
   * @param timeout the maximum time to wait, or null for no timeout
   * @return a list of events that became ready
   * @throws WasmException if the poll operation fails
   * @throws IllegalArgumentException if subscriptions is null or empty
   * @throws InterruptedException if the poll is interrupted
   */
  List<WasiEvent> pollOneoff(final List<WasiSubscription> subscriptions, final Duration timeout)
      throws WasmException, InterruptedException;

  /**
   * Checks if high-resolution timing is available.
   *
   * <p>This method can be used to determine if the current platform supports high-resolution timing
   * operations. If false, clock operations may have limited precision.
   *
   * @return true if high-resolution timing is available, false otherwise
   */
  boolean isHighResolutionAvailable();

  /**
   * Gets timing statistics and performance information.
   *
   * <p>This method returns information about clock performance, resolution, and usage statistics
   * for the current process.
   *
   * @return timing statistics object
   * @throws WasmException if getting statistics fails
   */
  WasiClockStats getClockStats() throws WasmException;
}
