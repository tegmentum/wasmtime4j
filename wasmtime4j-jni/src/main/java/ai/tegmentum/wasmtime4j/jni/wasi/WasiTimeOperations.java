package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI time and clock operations.
 *
 * <p>This class provides access to WASI time operations including clock resolution querying and
 * time retrieval for different clock types. It implements the WASI preview1 specification for
 * time operations with proper validation and error handling.
 *
 * <p>Supported operations:
 *
 * <ul>
 *   <li>Clock resolution querying for different clock types
 *   <li>Current time retrieval for realtime, monotonic, and CPU time clocks
 *   <li>Proper validation and error handling for all time operations
 *   <li>Thread-safe access to system time sources
 * </ul>
 *
 * <p>Security considerations:
 *
 * <ul>
 *   <li>All time operations are read-only and do not modify system state
 *   <li>Clock access is controlled by the WASI permission system
 *   <li>Time precision may be limited for security reasons
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiTimeOperations {

  private static final Logger LOGGER = Logger.getLogger(WasiTimeOperations.class.getName());

  /** Clock ID for wall clock time (since Unix epoch). */
  public static final int WASI_CLOCK_REALTIME = 0;

  /** Clock ID for monotonic time (suitable for measuring time intervals). */
  public static final int WASI_CLOCK_MONOTONIC = 1;

  /** Clock ID for process CPU time. */
  public static final int WASI_CLOCK_PROCESS_CPUTIME_ID = 2;

  /** Clock ID for thread CPU time. */
  public static final int WASI_CLOCK_THREAD_CPUTIME_ID = 3;

  /** Maximum valid clock ID value. */
  private static final int MAX_CLOCK_ID = 3;

  /** The WASI context this time operations instance belongs to. */
  private final WasiContext wasiContext;

  /**
   * Creates a new WASI time operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @throws JniException if the wasiContext is null
   */
  public WasiTimeOperations(final WasiContext wasiContext) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    this.wasiContext = wasiContext;
  }

  /**
   * Gets the resolution of the specified clock.
   *
   * <p>This operation returns the resolution (precision) of the specified clock in nanoseconds.
   * The resolution represents the minimum time interval that can be measured by the clock.
   *
   * @param clockId the clock identifier (one of the WASI_CLOCK_* constants)
   * @return the clock resolution in nanoseconds
   * @throws WasiException if the clock ID is invalid or the operation fails
   * @throws JniException if a JNI error occurs
   */
  public long getClockResolution(final int clockId) {
    validateClockId(clockId);

    try {
      LOGGER.fine(() -> String.format("Getting clock resolution for clock ID %d", clockId));

      final long resolution = nativeGetClockResolution(wasiContext.getNativeHandle(), clockId);

      if (resolution < 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull((int) -resolution);
        if (errorCode != null) {
          throw new WasiException("Failed to get clock resolution: " + errorCode.getDescription(), 
                                  errorCode);
        } else {
          throw new WasiException("Failed to get clock resolution with unknown error code: " + 
                                  (-resolution));
        }
      }

      LOGGER.fine(() -> String.format("Clock %d resolution: %d nanoseconds", clockId, resolution));
      return resolution;

    } catch (final RuntimeException e) {
      LOGGER.log(Level.WARNING, "Error getting clock resolution for clock ID " + clockId, e);
      throw e;
    }
  }

  /**
   * Gets the current time for the specified clock.
   *
   * <p>This operation returns the current time for the specified clock. The time is returned as
   * nanoseconds since the clock's epoch (Unix epoch for REALTIME, system boot for MONOTONIC).
   *
   * @param clockId the clock identifier (one of the WASI_CLOCK_* constants)
   * @param precision the requested time precision in nanoseconds (0 for maximum precision)
   * @return the current time in nanoseconds since the clock's epoch
   * @throws WasiException if the clock ID is invalid or the operation fails
   * @throws JniException if a JNI error occurs
   */
  public long getCurrentTime(final int clockId, final long precision) {
    validateClockId(clockId);
    JniValidation.requireNonNegative(precision, "precision");

    try {
      LOGGER.fine(() -> String.format("Getting current time for clock ID %d with precision %d", 
                                      clockId, precision));

      final long timestamp = nativeGetCurrentTime(wasiContext.getNativeHandle(), clockId, precision);

      if (timestamp < 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull((int) -timestamp);
        if (errorCode != null) {
          throw new WasiException("Failed to get current time: " + errorCode.getDescription(), 
                                  errorCode);
        } else {
          throw new WasiException("Failed to get current time with unknown error code: " + 
                                  (-timestamp));
        }
      }

      LOGGER.fine(() -> String.format("Clock %d current time: %d nanoseconds", clockId, timestamp));
      return timestamp;

    } catch (final RuntimeException e) {
      LOGGER.log(Level.WARNING, "Error getting current time for clock ID " + clockId, e);
      throw e;
    }
  }

  /**
   * Gets the current time for the specified clock with maximum precision.
   *
   * <p>This is a convenience method equivalent to calling {@link #getCurrentTime(int, long)} with
   * precision set to 0 (maximum precision).
   *
   * @param clockId the clock identifier (one of the WASI_CLOCK_* constants)
   * @return the current time in nanoseconds since the clock's epoch
   * @throws WasiException if the clock ID is invalid or the operation fails
   * @throws JniException if a JNI error occurs
   */
  public long getCurrentTime(final int clockId) {
    return getCurrentTime(clockId, 0);
  }

  /**
   * Gets the current realtime (wall clock time) in nanoseconds since Unix epoch.
   *
   * <p>This is a convenience method for getting the current wall clock time, equivalent to calling
   * {@link #getCurrentTime(int)} with {@link #WASI_CLOCK_REALTIME}.
   *
   * @return the current realtime in nanoseconds since Unix epoch
   * @throws WasiException if the operation fails
   * @throws JniException if a JNI error occurs
   */
  public long getRealtime() {
    return getCurrentTime(WASI_CLOCK_REALTIME);
  }

  /**
   * Gets the current monotonic time in nanoseconds since system boot.
   *
   * <p>This is a convenience method for getting monotonic time suitable for measuring time
   * intervals, equivalent to calling {@link #getCurrentTime(int)} with {@link #WASI_CLOCK_MONOTONIC}.
   *
   * @return the current monotonic time in nanoseconds since system boot
   * @throws WasiException if the operation fails
   * @throws JniException if a JNI error occurs
   */
  public long getMonotonicTime() {
    return getCurrentTime(WASI_CLOCK_MONOTONIC);
  }

  /**
   * Gets the current process CPU time in nanoseconds.
   *
   * <p>This is a convenience method for getting the CPU time consumed by the current process,
   * equivalent to calling {@link #getCurrentTime(int)} with {@link #WASI_CLOCK_PROCESS_CPUTIME_ID}.
   *
   * @return the current process CPU time in nanoseconds
   * @throws WasiException if the operation fails
   * @throws JniException if a JNI error occurs
   */
  public long getProcessCpuTime() {
    return getCurrentTime(WASI_CLOCK_PROCESS_CPUTIME_ID);
  }

  /**
   * Gets the current thread CPU time in nanoseconds.
   *
   * <p>This is a convenience method for getting the CPU time consumed by the current thread,
   * equivalent to calling {@link #getCurrentTime(int)} with {@link #WASI_CLOCK_THREAD_CPUTIME_ID}.
   *
   * @return the current thread CPU time in nanoseconds
   * @throws WasiException if the operation fails
   * @throws JniException if a JNI error occurs
   */
  public long getThreadCpuTime() {
    return getCurrentTime(WASI_CLOCK_THREAD_CPUTIME_ID);
  }

  /**
   * Converts nanoseconds to the specified time unit.
   *
   * <p>This is a utility method for converting time values returned by the time operations to
   * different time units for convenience.
   *
   * @param nanoseconds the time in nanoseconds
   * @param unit the target time unit
   * @return the time converted to the specified unit
   */
  public static long convertTime(final long nanoseconds, final TimeUnit unit) {
    JniValidation.requireNonNull(unit, "unit");
    return unit.convert(nanoseconds, TimeUnit.NANOSECONDS);
  }

  /**
   * Checks if the specified clock ID is supported.
   *
   * <p>This method can be used to check if a clock ID is supported before attempting to use it
   * with the time operations.
   *
   * @param clockId the clock identifier to check
   * @return true if the clock ID is supported, false otherwise
   */
  public static boolean isClockSupported(final int clockId) {
    return clockId >= 0 && clockId <= MAX_CLOCK_ID;
  }

  /**
   * Gets a human-readable name for the specified clock ID.
   *
   * @param clockId the clock identifier
   * @return the human-readable clock name
   */
  public static String getClockName(final int clockId) {
    switch (clockId) {
      case WASI_CLOCK_REALTIME:
        return "REALTIME";
      case WASI_CLOCK_MONOTONIC:
        return "MONOTONIC";
      case WASI_CLOCK_PROCESS_CPUTIME_ID:
        return "PROCESS_CPUTIME";
      case WASI_CLOCK_THREAD_CPUTIME_ID:
        return "THREAD_CPUTIME";
      default:
        return "UNKNOWN(" + clockId + ")";
    }
  }

  /**
   * Validates that the specified clock ID is valid.
   *
   * @param clockId the clock ID to validate
   * @throws JniException if the clock ID is invalid
   */
  private void validateClockId(final int clockId) {
    if (!isClockSupported(clockId)) {
      throw new JniException("Invalid clock ID: " + clockId + 
                            " (valid range: 0-" + MAX_CLOCK_ID + ")");
    }
  }

  /**
   * Native method to get clock resolution.
   *
   * @param contextHandle the native WASI context handle
   * @param clockId the clock identifier
   * @return the clock resolution in nanoseconds, or negative error code on failure
   */
  private static native long nativeGetClockResolution(long contextHandle, int clockId);

  /**
   * Native method to get current time.
   *
   * @param contextHandle the native WASI context handle
   * @param clockId the clock identifier
   * @param precision the requested precision in nanoseconds
   * @return the current time in nanoseconds, or negative error code on failure
   */
  private static native long nativeGetCurrentTime(long contextHandle, int clockId, long precision);
}