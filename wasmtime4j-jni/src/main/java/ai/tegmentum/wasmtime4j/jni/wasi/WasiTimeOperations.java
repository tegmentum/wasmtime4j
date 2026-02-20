package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.AbstractWasiTimeOperations;
import ai.tegmentum.wasmtime4j.wasi.exception.WasiErrorCode;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI time and clock operations.
 *
 * <p>Provides access to WASI time operations using JNI native calls. Constants, convenience
 * methods, and static utilities are inherited from {@link AbstractWasiTimeOperations}.
 *
 * @since 1.0.0
 */
public final class WasiTimeOperations extends AbstractWasiTimeOperations {

  private static final Logger LOGGER = Logger.getLogger(WasiTimeOperations.class.getName());

  /** The WASI context this time operations instance belongs to. */
  private final WasiContext wasiContext;

  /**
   * Creates a new WASI time operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @throws IllegalArgumentException if the wasiContext is null
   */
  public WasiTimeOperations(final WasiContext wasiContext) {
    Validation.requireNonNull(wasiContext, "wasiContext");
    this.wasiContext = wasiContext;
  }

  @Override
  public long getClockResolution(final int clockId) throws WasiException {
    validateClockId(clockId);

    try {
      LOGGER.fine(() -> String.format("Getting clock resolution for clock ID %d", clockId));

      final long resolution = nativeGetClockResolution(wasiContext.getNativeHandle(), clockId);

      if (resolution < 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull((int) -resolution);
        if (errorCode != null) {
          throw new WasiException(
              "Failed to get clock resolution: " + errorCode.getDescription(),
              errorCode,
              "clock_res_get",
              String.valueOf(clockId));
        } else {
          throw new WasiException(
              "Failed to get clock resolution with unknown error code: " + (-resolution));
        }
      }

      LOGGER.fine(() -> String.format("Clock %d resolution: %d nanoseconds", clockId, resolution));
      return resolution;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Error getting clock resolution for clock ID " + clockId, e);
      throw e;
    } catch (final RuntimeException e) {
      LOGGER.log(Level.WARNING, "Error getting clock resolution for clock ID " + clockId, e);
      throw e;
    }
  }

  @Override
  public long getCurrentTime(final int clockId, final long precision) throws WasiException {
    validateClockId(clockId);
    Validation.requireNonNegative(precision, "precision");

    try {
      LOGGER.fine(
          () ->
              String.format(
                  "Getting current time for clock ID %d with precision %d", clockId, precision));

      final long timestamp =
          nativeGetCurrentTime(wasiContext.getNativeHandle(), clockId, precision);

      if (timestamp < 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull((int) -timestamp);
        if (errorCode != null) {
          throw new WasiException(
              "Failed to get current time: " + errorCode.getDescription(),
              errorCode,
              "clock_time_get",
              String.valueOf(clockId));
        } else {
          throw new WasiException(
              "Failed to get current time with unknown error code: " + (-timestamp));
        }
      }

      LOGGER.fine(() -> String.format("Clock %d current time: %d nanoseconds", clockId, timestamp));
      return timestamp;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Error getting current time for clock ID " + clockId, e);
      throw e;
    } catch (final RuntimeException e) {
      LOGGER.log(Level.WARNING, "Error getting current time for clock ID " + clockId, e);
      throw e;
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
