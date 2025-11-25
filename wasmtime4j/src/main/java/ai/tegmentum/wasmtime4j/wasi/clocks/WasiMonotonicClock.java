package ai.tegmentum.wasmtime4j.wasi.clocks;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;

/**
 * WASI monotonic clock interface for measuring elapsed time.
 *
 * <p>This clock is monotonic, meaning successive calls to {@link #now()} will always return
 * non-decreasing values. The clock measures time in nanoseconds relative to an unspecified
 * initial value.
 *
 * <p>This interface should be used for measuring elapsed time, performance timing, and
 * implementing timeouts. For reporting the current date and time to humans, use
 * {@link WasiWallClock} instead.
 *
 * <p>WASI Preview 2 specification: wasi:clocks/monotonic-clock@0.2.8
 */
public interface WasiMonotonicClock {

    /**
     * Reads the current value of the monotonic clock.
     *
     * <p>The returned value is in nanoseconds relative to an unspecified initial value.
     * Successive calls to this method will return non-decreasing values.
     *
     * @return current instant in nanoseconds
     * @throws WasmException if reading the clock fails
     */
    long now();

    /**
     * Gets the resolution of the monotonic clock.
     *
     * <p>The resolution represents the duration of a single clock tick in nanoseconds.
     * This indicates the precision of the clock.
     *
     * @return clock resolution in nanoseconds
     * @throws WasmException if reading the resolution fails
     */
    long resolution();

    /**
     * Creates a pollable that resolves when the specified instant occurs.
     *
     * <p>The pollable will become ready when the monotonic clock reaches or exceeds
     * the specified instant. This can be used to implement timeouts and scheduled tasks.
     *
     * @param when instant in nanoseconds (as returned by {@link #now()})
     * @return pollable that resolves at the specified instant
     * @throws WasmException if creating the pollable fails
     */
    WasiPollable subscribeInstant(long when);

    /**
     * Creates a pollable that resolves after the specified duration elapses.
     *
     * <p>The pollable will become ready after the given duration has passed from the time
     * this method is called. This is useful for implementing delays and timeouts.
     *
     * @param duration duration in nanoseconds to wait
     * @return pollable that resolves after the duration
     * @throws WasmException if creating the pollable fails
     */
    WasiPollable subscribeDuration(long duration);
}
