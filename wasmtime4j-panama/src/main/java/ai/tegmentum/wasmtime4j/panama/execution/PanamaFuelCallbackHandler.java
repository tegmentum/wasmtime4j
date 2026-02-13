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

package ai.tegmentum.wasmtime4j.panama.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.execution.FuelCallbackHandler;
import ai.tegmentum.wasmtime4j.execution.FuelCallbackStats;
import ai.tegmentum.wasmtime4j.execution.FuelExhaustionAction;
import ai.tegmentum.wasmtime4j.execution.FuelExhaustionContext;
import ai.tegmentum.wasmtime4j.execution.FuelExhaustionResult;
import ai.tegmentum.wasmtime4j.panama.NativeInstanceBindings;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the FuelCallbackHandler interface.
 *
 * <p>This class provides fuel exhaustion callback handling through Panama Foreign Function API
 * calls to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaFuelCallbackHandler implements FuelCallbackHandler {

  private static final Logger LOGGER = Logger.getLogger(PanamaFuelCallbackHandler.class.getName());

  private final long handlerId;
  private final long storeId;
  private volatile boolean closed = false;

  private static MethodHandle getHandle(final String name) throws WasmException {
    return NativeInstanceBindings.getInstance()
        .getMethodHandle(name)
        .orElseThrow(() -> new WasmException("Native function not found: " + name));
  }

  /**
   * Creates a new Panama fuel callback handler with auto-refill behavior.
   *
   * @param storeId the store ID this handler is associated with
   * @param refillAmount the amount of fuel to add when exhausted
   * @param maxRefills the maximum number of refills (-1 for unlimited)
   * @return a new fuel callback handler
   * @throws WasmException if handler creation fails
   */
  public static PanamaFuelCallbackHandler createAutoRefill(
      final long storeId, final long refillAmount, final int maxRefills) throws WasmException {
    final MethodHandle createHandle = getHandle("wasmtime4j_fuel_callback_create_auto_refill");

    try {
      final long handlerId = (long) createHandle.invoke(storeId, refillAmount, maxRefills);
      if (handlerId == 0) {
        throw new WasmException("Failed to create auto-refill fuel callback handler");
      }
      LOGGER.fine(
          "Created auto-refill fuel callback handler: "
              + handlerId
              + " for store: "
              + storeId
              + " with refill amount: "
              + refillAmount);
      return new PanamaFuelCallbackHandler(handlerId, storeId);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating fuel callback handler: " + e.getMessage(), e);
    }
  }

  private PanamaFuelCallbackHandler(final long handlerId, final long storeId) {
    this.handlerId = handlerId;
    this.storeId = storeId;
  }

  @Override
  public long getId() {
    return handlerId;
  }

  @Override
  public long getStoreId() {
    return storeId;
  }

  @Override
  public FuelExhaustionResult handleExhaustion(final FuelExhaustionContext context)
      throws WasmException {
    if (closed) {
      throw new IllegalStateException("Handler has been closed");
    }

    final MethodHandle handleHandle = getHandle("wasmtime4j_fuel_callback_handle_exhaustion");

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment actionOut = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment fuelOut = arena.allocate(ValueLayout.JAVA_LONG);

      final int status =
          (int)
              handleHandle.invoke(
                  context.getStoreId(),
                  context.getFuelConsumed(),
                  context.getInitialFuel(),
                  context.getExhaustionCount(),
                  actionOut,
                  fuelOut);

      if (status != 0) {
        LOGGER.warning("Fuel exhaustion handling returned error status: " + status);
        return FuelExhaustionResult.trap();
      }

      final int actionCode = actionOut.get(ValueLayout.JAVA_INT, 0);
      final long additionalFuel = fuelOut.get(ValueLayout.JAVA_LONG, 0);
      final FuelExhaustionAction action = FuelExhaustionAction.fromCode(actionCode);

      if (action == FuelExhaustionAction.CONTINUE) {
        return FuelExhaustionResult.continueWith(additionalFuel);
      } else if (action == FuelExhaustionAction.PAUSE) {
        return FuelExhaustionResult.pause();
      } else {
        return FuelExhaustionResult.trap();
      }
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error handling fuel exhaustion: " + e.getMessage(), e);
    }
  }

  @Override
  public FuelCallbackStats getStats() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Handler has been closed");
    }

    final MethodHandle statsHandle = getHandle("wasmtime4j_fuel_callback_get_stats");

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment exhaustionEventsOut = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment totalFuelAddedOut = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment continuedCountOut = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment trappedCountOut = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment pausedCountOut = arena.allocate(ValueLayout.JAVA_LONG);

      final int status =
          (int)
              statsHandle.invoke(
                  handlerId,
                  exhaustionEventsOut,
                  totalFuelAddedOut,
                  continuedCountOut,
                  trappedCountOut,
                  pausedCountOut);

      if (status != 0) {
        throw new WasmException("Failed to get fuel callback stats for handler: " + handlerId);
      }

      return new FuelCallbackStats(
          exhaustionEventsOut.get(ValueLayout.JAVA_LONG, 0),
          totalFuelAddedOut.get(ValueLayout.JAVA_LONG, 0),
          continuedCountOut.get(ValueLayout.JAVA_LONG, 0),
          trappedCountOut.get(ValueLayout.JAVA_LONG, 0),
          pausedCountOut.get(ValueLayout.JAVA_LONG, 0));
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error getting fuel callback stats: " + e.getMessage(), e);
    }
  }

  @Override
  public void resetStats() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Handler has been closed");
    }

    final MethodHandle resetHandle = getHandle("wasmtime4j_fuel_callback_reset_stats");

    try {
      final int status = (int) resetHandle.invoke(handlerId);
      if (status != 0) {
        throw new WasmException("Failed to reset fuel callback stats for handler: " + handlerId);
      }
      LOGGER.fine("Reset stats for fuel callback handler: " + handlerId);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error resetting fuel callback stats: " + e.getMessage(), e);
    }
  }

  @Override
  public void close() throws WasmException {
    if (closed) {
      return;
    }
    closed = true;

    final MethodHandle destroyHandle = getHandle("wasmtime4j_fuel_callback_destroy");

    try {
      final int status = (int) destroyHandle.invoke(handlerId);
      if (status != 0) {
        LOGGER.warning("Failed to destroy fuel callback handler: " + handlerId);
      } else {
        LOGGER.fine("Closed fuel callback handler: " + handlerId);
      }
    } catch (final Throwable e) {
      throw new WasmException("Error closing fuel callback handler: " + e.getMessage(), e);
    }
  }
}
