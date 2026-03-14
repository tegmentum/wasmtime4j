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
package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.AbstractWasiTimeOperations;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WASI time and clock operations.
 *
 * <p>Provides access to WASI time operations using Panama Foreign Function Interface. Constants,
 * convenience methods, and static utilities are inherited from {@link AbstractWasiTimeOperations}.
 *
 * @since 1.0.0
 */
public final class WasiTimeOperations extends AbstractWasiTimeOperations {

  private static final Logger LOGGER = Logger.getLogger(WasiTimeOperations.class.getName());

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
    Validation.requireNonNull(wasiContext, "wasiContext");
    Validation.requireNonNull(symbolLookup, "symbolLookup");

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

  @Override
  public long getClockResolution(final int clockId) throws WasiException {
    validateClockId(clockId);

    try (final Arena arena = Arena.ofConfined()) {
      LOGGER.fine(() -> String.format("Getting clock resolution for clock ID %d", clockId));

      // Allocate memory for the resolution output
      final MemorySegment resolutionOut = arena.allocate(ValueLayout.JAVA_LONG);

      // Call wasi_clock_res_get
      final int result = (int) clockResGetHandle.invokeExact(clockId, resolutionOut);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(
            result, "Failed to get clock resolution for clock " + clockId);
      }

      final long resolution = resolutionOut.get(ValueLayout.JAVA_LONG, 0);
      LOGGER.fine(() -> String.format("Clock %d resolution: %d nanoseconds", clockId, resolution));
      return resolution;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Error getting clock resolution for clock ID " + clockId, e);
      throw e;
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Error getting clock resolution for clock ID " + clockId, e);
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new WasiException("Failed to get clock resolution: " + e.getMessage(), e);
      }
    }
  }

  @Override
  public long getCurrentTime(final int clockId, final long precision) throws WasiException {
    validateClockId(clockId);
    Validation.requireNonNegative(precision, "precision");

    try (final Arena arena = Arena.ofConfined()) {
      LOGGER.fine(
          () ->
              String.format(
                  "Getting current time for clock ID %d with precision %d", clockId, precision));

      // Allocate memory for the timestamp output
      final MemorySegment timestampOut = arena.allocate(ValueLayout.JAVA_LONG);

      // Call wasi_clock_time_get
      final int result = (int) clockTimeGetHandle.invokeExact(clockId, precision, timestampOut);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(
            result, "Failed to get current time for clock " + clockId);
      }

      final long timestamp = timestampOut.get(ValueLayout.JAVA_LONG, 0);
      LOGGER.fine(() -> String.format("Clock %d current time: %d nanoseconds", clockId, timestamp));
      return timestamp;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Error getting current time for clock ID " + clockId, e);
      throw e;
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Error getting current time for clock ID " + clockId, e);
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new WasiException("Failed to get current time: " + e.getMessage(), e);
      }
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
