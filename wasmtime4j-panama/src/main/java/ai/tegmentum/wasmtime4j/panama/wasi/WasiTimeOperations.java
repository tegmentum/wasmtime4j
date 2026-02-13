package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WASI time and clock operations.
 *
 * <p>This class provides access to WASI time operations including clock resolution querying and
 * time retrieval for different clock types. It implements the WASI preview1 specification for time
 * operations using Panama Foreign Function Interface.
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

  /** Native symbol lookup for WASI functions. */
  private final SymbolLookup symbolLookup;

  /** Method handle for wasi_clock_res_get function. */
  private final MethodHandle clockResGetHandle;

  /** Method handle for wasi_clock_time_get function. */
  private final MethodHandle clockTimeGetHandle;

  /**
   * Creates a new WASI time operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @param symbolLookup the symbol lookup for native WASI functions
   * @throws PanamaException if the wasiContext or symbolLookup is null, or if native function
   *     lookup fails
   */
  public WasiTimeOperations(final WasiContext wasiContext, final SymbolLookup symbolLookup)
      throws PanamaException {
    PanamaValidation.requireNonNull(wasiContext, "wasiContext");
    PanamaValidation.requireNonNull(symbolLookup, "symbolLookup");

    this.wasiContext = wasiContext;
    this.symbolLookup = symbolLookup;

    // Initialize native function handles
    try {
      this.clockResGetHandle = initializeClockResGetHandle();
      this.clockTimeGetHandle = initializeClockTimeGetHandle();
      LOGGER.fine("Initialized WASI time operations with Panama FFI");
    } catch (final Exception e) {
      throw new PanamaException("Failed to initialize WASI time operations: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the resolution of the specified clock.
   *
   * <p>This operation returns the resolution (precision) of the specified clock in nanoseconds. The
   * resolution represents the minimum time interval that can be measured by the clock.
   *
   * @param clockId the clock identifier (one of the WASI_CLOCK_* constants)
   * @return the clock resolution in nanoseconds
   * @throws WasiException if the clock ID is invalid or the operation fails
   * @throws PanamaException if a Panama FFI error occurs
   */
  public long getClockResolution(final int clockId) throws PanamaException {
    validateClockId(clockId);

    try (final Arena arena = Arena.ofConfined()) {
      LOGGER.fine(() -> String.format("Getting clock resolution for clock ID %d", clockId));

      // Allocate memory for the resolution output
      final MemorySegment resolutionOut = arena.allocate(ValueLayout.JAVA_LONG);

      // Call wasi_clock_res_get
      final int result = (int) clockResGetHandle.invoke(clockId, resolutionOut);

      if (result != 0) {
        throw new WasiException(
            "Failed to get clock resolution for clock " + clockId + ": error code " + result);
      }

      final long resolution = resolutionOut.get(ValueLayout.JAVA_LONG, 0);
      LOGGER.fine(() -> String.format("Clock %d resolution: %d nanoseconds", clockId, resolution));
      return resolution;

    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Error getting clock resolution for clock ID " + clockId, e);
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new PanamaException("Failed to get clock resolution: " + e.getMessage(), e);
      }
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
   * @throws PanamaException if a Panama FFI error occurs
   */
  public long getCurrentTime(final int clockId, final long precision) throws PanamaException {
    validateClockId(clockId);
    PanamaValidation.requireNonNegative(precision, "precision");

    try (final Arena arena = Arena.ofConfined()) {
      LOGGER.fine(
          () ->
              String.format(
                  "Getting current time for clock ID %d with precision %d", clockId, precision));

      // Allocate memory for the timestamp output
      final MemorySegment timestampOut = arena.allocate(ValueLayout.JAVA_LONG);

      // Call wasi_clock_time_get
      final int result = (int) clockTimeGetHandle.invoke(clockId, precision, timestampOut);

      if (result != 0) {
        throw new WasiException(
            "Failed to get current time for clock " + clockId + ": error code " + result);
      }

      final long timestamp = timestampOut.get(ValueLayout.JAVA_LONG, 0);
      LOGGER.fine(() -> String.format("Clock %d current time: %d nanoseconds", clockId, timestamp));
      return timestamp;

    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Error getting current time for clock ID " + clockId, e);
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new PanamaException("Failed to get current time: " + e.getMessage(), e);
      }
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
   * @throws PanamaException if a Panama FFI error occurs
   */
  public long getCurrentTime(final int clockId) throws PanamaException {
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
   * @throws PanamaException if a Panama FFI error occurs
   */
  public long getRealtime() throws PanamaException {
    return getCurrentTime(WASI_CLOCK_REALTIME);
  }

  /**
   * Gets the current monotonic time in nanoseconds since system boot.
   *
   * <p>This is a convenience method for getting monotonic time suitable for measuring time
   * intervals, equivalent to calling {@link #getCurrentTime(int)} with {@link
   * #WASI_CLOCK_MONOTONIC}.
   *
   * @return the current monotonic time in nanoseconds since system boot
   * @throws WasiException if the operation fails
   * @throws PanamaException if a Panama FFI error occurs
   */
  public long getMonotonicTime() throws PanamaException {
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
   * @throws PanamaException if a Panama FFI error occurs
   */
  public long getProcessCpuTime() throws PanamaException {
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
   * @throws PanamaException if a Panama FFI error occurs
   */
  public long getThreadCpuTime() throws PanamaException {
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
    PanamaValidation.requireNonNull(unit, "unit");
    return unit.convert(nanoseconds, TimeUnit.NANOSECONDS);
  }

  /**
   * Checks if the specified clock ID is supported.
   *
   * <p>This method can be used to check if a clock ID is supported before attempting to use it with
   * the time operations.
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
   * @throws PanamaException if the clock ID is invalid
   */
  private void validateClockId(final int clockId) throws PanamaException {
    if (!isClockSupported(clockId)) {
      throw new PanamaException(
          "Invalid clock ID: " + clockId + " (valid range: 0-" + MAX_CLOCK_ID + ")");
    }
  }

  /**
   * Initializes the method handle for wasi_clock_res_get function.
   *
   * @return the method handle for clock resolution retrieval
   * @throws Exception if function lookup fails
   */
  private MethodHandle initializeClockResGetHandle() throws Exception {
    final MemorySegment symbol =
        symbolLookup
            .find("wasi_clock_res_get")
            .orElseThrow(() -> new PanamaException("WASI function wasi_clock_res_get not found"));

    final FunctionDescriptor descriptor =
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: wasi_errno_t
            ValueLayout.JAVA_INT, // clock_id: wasi_clockid_t
            ValueLayout.ADDRESS // resolution_out: *wasi_timestamp_t
            );

    return Linker.nativeLinker().downcallHandle(symbol, descriptor);
  }

  /**
   * Initializes the method handle for wasi_clock_time_get function.
   *
   * @return the method handle for current time retrieval
   * @throws Exception if function lookup fails
   */
  private MethodHandle initializeClockTimeGetHandle() throws Exception {
    final MemorySegment symbol =
        symbolLookup
            .find("wasi_clock_time_get")
            .orElseThrow(() -> new PanamaException("WASI function wasi_clock_time_get not found"));

    final FunctionDescriptor descriptor =
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: wasi_errno_t
            ValueLayout.JAVA_INT, // clock_id: wasi_clockid_t
            ValueLayout.JAVA_LONG, // precision: wasi_timestamp_t
            ValueLayout.ADDRESS // timestamp_out: *wasi_timestamp_t
            );

    return Linker.nativeLinker().downcallHandle(symbol, descriptor);
  }
}
