package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI Preview 2 clock and time operations.
 *
 * <p>This class implements the WASI Preview 2 clock operations as defined in the WIT interface
 * `wasi:clocks/wall-clock` and `wasi:clocks/monotonic-clock`. It provides enhanced time
 * functionality with async support and improved precision.
 *
 * <p>Supported WASI Preview 2 clock operations:
 *
 * <ul>
 *   <li>Wall clock operations with timezone support
 *   <li>Monotonic clock operations for duration measurement
 *   <li>High-precision time queries
 *   <li>Async time operations
 *   <li>Sleep and timer operations
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiTimeOperationsPreview2 {

  private static final Logger LOGGER = Logger.getLogger(WasiTimeOperationsPreview2.class.getName());

  /** Clock ID for realtime clock (wall clock). */
  public static final int CLOCK_REALTIME = 0;

  /** Clock ID for monotonic clock. */
  public static final int CLOCK_MONOTONIC = 1;

  /** Clock ID for process CPU time. */
  public static final int CLOCK_PROCESS_CPUTIME = 2;

  /** Clock ID for thread CPU time. */
  public static final int CLOCK_THREAD_CPUTIME = 3;

  /** Nanoseconds per second. */
  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  /** The WASI context this time operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /**
   * Creates a new WASI Preview 2 time operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiTimeOperationsPreview2(
      final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;

    LOGGER.info("Created WASI Preview 2 time operations handler");
  }

  /**
   * Gets the current time for the specified clock.
   *
   * <p>WIT interface: wasi:clocks/wall-clock.now or wasi:clocks/monotonic-clock.now
   *
   * @param clockId the clock identifier
   * @param precision the requested precision in nanoseconds
   * @return the current time in nanoseconds since epoch (wall clock) or since start (monotonic)
   * @throws WasiException if the operation fails
   */
  public long getCurrentTime(final int clockId, final long precision) {
    validateClockId(clockId);
    JniValidation.requireNonNegative(precision, "precision");

    LOGGER.fine(
        () -> String.format("Getting current time: clockId=%d, precision=%d", clockId, precision));

    try {
      final long time = nativeGetCurrentTime(wasiContext.getNativeHandle(), clockId, precision);

      if (time < 0) {
        throw new WasiException(
            "Failed to get current time for clock " + clockId, WasiErrorCode.EIO);
      }

      LOGGER.fine(() -> String.format("Got current time: %d nanoseconds", time));
      return time;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get current time for clock " + clockId, e);
      throw new WasiException("Get current time failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets the current time asynchronously.
   *
   * @param clockId the clock identifier
   * @param precision the requested precision in nanoseconds
   * @return CompletableFuture that resolves to the current time in nanoseconds
   */
  public CompletableFuture<Long> getCurrentTimeAsync(final int clockId, final long precision) {
    validateClockId(clockId);
    JniValidation.requireNonNegative(precision, "precision");

    LOGGER.fine(
        () ->
            String.format(
                "Getting current time async: clockId=%d, precision=%d", clockId, precision));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return getCurrentTime(clockId, precision);
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async get current time failed", e);
            throw new RuntimeException("Async get current time failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Gets the resolution of the specified clock.
   *
   * <p>WIT interface: wasi:clocks/wall-clock.resolution or wasi:clocks/monotonic-clock.resolution
   *
   * @param clockId the clock identifier
   * @return the clock resolution in nanoseconds
   * @throws WasiException if the operation fails
   */
  public long getClockResolution(final int clockId) {
    validateClockId(clockId);

    LOGGER.fine(() -> String.format("Getting clock resolution: clockId=%d", clockId));

    try {
      final long resolution = nativeGetClockResolution(wasiContext.getNativeHandle(), clockId);

      if (resolution <= 0) {
        throw new WasiException(
            "Failed to get clock resolution for clock " + clockId, WasiErrorCode.EIO);
      }

      LOGGER.fine(() -> String.format("Got clock resolution: %d nanoseconds", resolution));
      return resolution;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get clock resolution for clock " + clockId, e);
      throw new WasiException("Get clock resolution failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets the current wall clock time as a ZonedDateTime.
   *
   * <p>This method provides a more convenient way to get the current wall clock time with timezone
   * information.
   *
   * @return the current wall clock time with UTC timezone
   * @throws WasiException if the operation fails
   */
  public ZonedDateTime getCurrentWallClockTime() {
    LOGGER.fine("Getting current wall clock time");

    try {
      final long nanosSinceEpoch = getCurrentTime(CLOCK_REALTIME, 1);
      final long seconds = nanosSinceEpoch / NANOS_PER_SECOND;
      final long nanos = nanosSinceEpoch % NANOS_PER_SECOND;

      final Instant instant = Instant.ofEpochSecond(seconds, nanos);
      final ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);

      LOGGER.fine(() -> String.format("Got wall clock time: %s", dateTime));
      return dateTime;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get wall clock time", e);
      throw new WasiException("Get wall clock time failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets the current wall clock time asynchronously.
   *
   * @return CompletableFuture that resolves to the current wall clock time
   */
  public CompletableFuture<ZonedDateTime> getCurrentWallClockTimeAsync() {
    LOGGER.fine("Getting current wall clock time async");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return getCurrentWallClockTime();
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async get wall clock time failed", e);
            throw new RuntimeException("Async get wall clock time failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Sleeps for the specified duration.
   *
   * <p>WIT interface: wasi:clocks/monotonic-clock.sleep
   *
   * @param duration the duration to sleep
   * @throws WasiException if the operation fails
   */
  public void sleep(final Duration duration) {
    JniValidation.requireNonNull(duration, "duration");
    if (duration.isNegative()) {
      throw new WasiException("Sleep duration cannot be negative", WasiErrorCode.EINVAL);
    }

    final long nanos = duration.toNanos();
    LOGGER.fine(() -> String.format("Sleeping for %d nanoseconds", nanos));

    try {
      final int result = nativeSleep(wasiContext.getNativeHandle(), nanos);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Sleep operation failed: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      LOGGER.fine("Sleep completed successfully");

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Sleep operation failed", e);
      throw new WasiException("Sleep failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Sleeps for the specified duration asynchronously.
   *
   * @param duration the duration to sleep
   * @return CompletableFuture that completes when the sleep is done
   */
  public CompletableFuture<Void> sleepAsync(final Duration duration) {
    JniValidation.requireNonNull(duration, "duration");
    if (duration.isNegative()) {
      return CompletableFuture.failedFuture(
          new WasiException("Sleep duration cannot be negative", WasiErrorCode.EINVAL));
    }

    final long nanos = duration.toNanos();
    LOGGER.fine(() -> String.format("Sleeping async for %d nanoseconds", nanos));

    return CompletableFuture.runAsync(
        () -> {
          try {
            sleep(duration);
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async sleep failed", e);
            throw new RuntimeException("Async sleep failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates a timer that can be polled for completion.
   *
   * <p>WIT interface: wasi:clocks/monotonic-clock.subscribe-duration
   *
   * @param duration the timer duration
   * @return the timer handle that can be used with poll operations
   * @throws WasiException if timer creation fails
   */
  public long createTimer(final Duration duration) {
    JniValidation.requireNonNull(duration, "duration");
    if (duration.isNegative()) {
      throw new WasiException("Timer duration cannot be negative", WasiErrorCode.EINVAL);
    }

    final long nanos = duration.toNanos();
    LOGGER.fine(() -> String.format("Creating timer for %d nanoseconds", nanos));

    try {
      final TimerResult result = nativeCreateTimer(wasiContext.getNativeHandle(), nanos);

      if (result.errorCode != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
        throw new WasiException(
            "Timer creation failed: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      LOGGER.fine(() -> String.format("Created timer: handle=%d", result.timerHandle));
      return result.timerHandle;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Timer creation failed", e);
      throw new WasiException("Timer creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Cancels a timer.
   *
   * @param timerHandle the timer handle to cancel
   * @throws WasiException if timer cancellation fails
   */
  public void cancelTimer(final long timerHandle) {
    LOGGER.fine(() -> String.format("Cancelling timer: handle=%d", timerHandle));

    try {
      final int result = nativeCancelTimer(wasiContext.getNativeHandle(), timerHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Timer cancellation failed: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      LOGGER.fine(() -> String.format("Cancelled timer: handle=%d", timerHandle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Timer cancellation failed: " + timerHandle, e);
      throw new WasiException("Timer cancellation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /** Validates clock ID. */
  private void validateClockId(final int clockId) {
    if (clockId < 0 || clockId > CLOCK_THREAD_CPUTIME) {
      throw new WasiException("Invalid clock ID: " + clockId, WasiErrorCode.EINVAL);
    }
  }

  // Native method declarations
  private static native long nativeGetCurrentTime(long contextHandle, int clockId, long precision);

  private static native long nativeGetClockResolution(long contextHandle, int clockId);

  private static native int nativeSleep(long contextHandle, long durationNanos);

  private static native TimerResult nativeCreateTimer(long contextHandle, long durationNanos);

  private static native int nativeCancelTimer(long contextHandle, long timerHandle);

  /** Timer creation result from native code. */
  private static final class TimerResult {
    public final int errorCode;
    public final long timerHandle;

    public TimerResult(final int errorCode, final long timerHandle) {
      this.errorCode = errorCode;
      this.timerHandle = timerHandle;
    }
  }
}
