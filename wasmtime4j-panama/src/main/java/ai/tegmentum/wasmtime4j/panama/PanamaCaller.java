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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Export;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.adapter.WasmGlobalToGlobalAdapter;
import ai.tegmentum.wasmtime4j.adapter.WasmMemoryToMemoryAdapter;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.func.Function;
import ai.tegmentum.wasmtime4j.memory.Global;
import ai.tegmentum.wasmtime4j.memory.Memory;
import ai.tegmentum.wasmtime4j.memory.Table;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the Caller interface for accessing WebAssembly instance context.
 *
 * <p>This class provides access to the calling WebAssembly instance's exports, memory, globals, and
 * execution state through Panama Foreign Function API bindings to the native Wasmtime caller
 * context.
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
final class PanamaCaller<T> implements Caller<T> {
  private static final Logger LOGGER = Logger.getLogger(PanamaCaller.class.getName());

  private final long callerHandle;
  private final MemorySegment callerPtr;
  private final PanamaStore store;
  private final NativeInstanceBindings bindings;

  /**
   * Creates a Panama caller context wrapper.
   *
   * @param callerHandle the native caller handle from Wasmtime
   * @param store the store this caller is associated with
   */
  PanamaCaller(final long callerHandle, final PanamaStore store) {
    if (callerHandle == 0) {
      throw new IllegalArgumentException("Caller handle cannot be 0");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }

    this.callerHandle = callerHandle;
    this.callerPtr = MemorySegment.ofAddress(callerHandle);
    this.store = store;
    this.bindings = NativeInstanceBindings.getInstance();

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created PanamaCaller with handle: 0x" + Long.toHexString(callerHandle));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public T data() {
    // Get user data from the store
    final Object storeData = store.getData();
    return storeData != null ? (T) storeData : null;
  }

  @Override
  public Optional<Export> getExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    // TODO: Implement export retrieval
    return Optional.empty();
  }

  @Override
  public Optional<Function<T>> getFunction(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(name);
      final MemorySegment funcOut = arena.allocate(ValueLayout.ADDRESS);
      final int result = bindings.callerGetFunction(callerPtr, nameSegment, funcOut);
      if (result != 0) {
        return Optional.empty();
      }
      final MemorySegment funcHandle = funcOut.get(ValueLayout.ADDRESS, 0);
      if (funcHandle.equals(MemorySegment.NULL) || funcHandle.address() == 0) {
        return Optional.empty();
      }
      final PanamaCallerFunction panamaFunc = new PanamaCallerFunction(funcHandle, store, name);
      return Optional.of(
          new ai.tegmentum.wasmtime4j.panama.adapter.WasmFunctionToFunctionAdapter<>(panamaFunc));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get function: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Memory> getMemory(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Memory name cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(name);
      final MemorySegment memoryOut = arena.allocate(ValueLayout.ADDRESS);
      final int result = bindings.callerGetMemory(callerPtr, nameSegment, memoryOut);
      if (result != 0) {
        return Optional.empty();
      }
      final MemorySegment memoryHandle = memoryOut.get(ValueLayout.ADDRESS, 0);
      if (memoryHandle.equals(MemorySegment.NULL) || memoryHandle.address() == 0) {
        return Optional.empty();
      }
      final PanamaMemory panamaMemory = new PanamaMemory(memoryHandle, store);
      return Optional.of(new WasmMemoryToMemoryAdapter(panamaMemory));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get memory: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Table> getTable(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Table name cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(name);
      final MemorySegment tableOut = arena.allocate(ValueLayout.ADDRESS);
      final int result = bindings.callerGetTable(callerPtr, nameSegment, tableOut);
      if (result != 0) {
        return Optional.empty();
      }
      final MemorySegment tableHandle = tableOut.get(ValueLayout.ADDRESS, 0);
      if (tableHandle.equals(MemorySegment.NULL) || tableHandle.address() == 0) {
        return Optional.empty();
      }
      // PanamaTable requires element type which we don't have - return empty for now
      // A full implementation would need to query the table type from the native layer
      return Optional.empty();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get table: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Global> getGlobal(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Global name cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(name);
      final MemorySegment globalOut = arena.allocate(ValueLayout.ADDRESS);
      final int result = bindings.callerGetGlobal(callerPtr, nameSegment, globalOut);
      if (result != 0) {
        return Optional.empty();
      }
      final MemorySegment globalHandle = globalOut.get(ValueLayout.ADDRESS, 0);
      if (globalHandle.equals(MemorySegment.NULL) || globalHandle.address() == 0) {
        return Optional.empty();
      }
      final PanamaGlobal panamaGlobal = new PanamaGlobal(globalHandle, store);
      return Optional.of(new WasmGlobalToGlobalAdapter(panamaGlobal));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get global: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(name);
      final int result = bindings.callerHasExport(callerPtr, nameSegment);
      return result == 1;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to check export: " + name, e);
      return false;
    }
  }

  @Override
  public Optional<Long> fuelConsumed() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment fuelOut = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = bindings.callerGetFuel(callerPtr, fuelOut);
      if (result != 0) {
        return Optional.empty();
      }
      final long fuel = fuelOut.get(ValueLayout.JAVA_LONG, 0);
      return fuel >= 0 ? Optional.of(fuel) : Optional.empty();
    } catch (Exception e) {
      LOGGER.log(Level.FINE, "Fuel consumption not available", e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Long> fuelRemaining() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment fuelOut = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = bindings.callerGetFuelRemaining(callerPtr, fuelOut);
      if (result != 0) {
        return Optional.empty();
      }
      final long fuel = fuelOut.get(ValueLayout.JAVA_LONG, 0);
      return fuel >= 0 ? Optional.of(fuel) : Optional.empty();
    } catch (Exception e) {
      LOGGER.log(Level.FINE, "Fuel remaining not available", e);
      return Optional.empty();
    }
  }

  @Override
  public void addFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel amount cannot be negative");
    }

    try {
      final int result = bindings.callerAddFuel(callerPtr, fuel);
      if (result != 0) {
        throw new WasmException("Failed to add fuel (error code: " + result + ")");
      }
    } catch (WasmException e) {
      throw e;
    } catch (Exception e) {
      throw new WasmException("Failed to add fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean hasEpochDeadline() {
    try {
      final int result = bindings.callerHasEpochDeadline(callerPtr);
      return result == 1;
    } catch (Exception e) {
      LOGGER.log(Level.FINE, "Failed to check epoch deadline", e);
      return false;
    }
  }

  @Override
  public Optional<Long> epochDeadline() {
    // Note: Native bindings don't have a separate get epoch deadline function
    // The hasEpochDeadline check is available but not the actual value
    // Returning empty until the native layer supports this
    return Optional.empty();
  }

  @Override
  public void setEpochDeadline(final long deadline) throws WasmException {
    try {
      final int result = bindings.callerSetEpochDeadline(callerPtr, deadline);
      if (result != 0) {
        throw new WasmException("Failed to set epoch deadline (error code: " + result + ")");
      }
    } catch (WasmException e) {
      throw e;
    } catch (Exception e) {
      throw new WasmException("Failed to set epoch deadline: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<Export> getExportByModuleExport(final ModuleExport moduleExport) {
    if (moduleExport == null) {
      throw new IllegalArgumentException("moduleExport cannot be null");
    }

    try {
      // Use the name from the module export for lookup
      return getExport(moduleExport.getName());
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING, "Failed to get export by module export: " + moduleExport.getName(), e);
      return Optional.empty();
    }
  }

  @Override
  public Engine engine() {
    // Get the engine from the store
    return store.getEngine();
  }

  @Override
  public void gc() throws WasmException {
    try {
      store.gc();
    } catch (Exception e) {
      throw new WasmException("Failed to perform GC: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<Long> fuelAsyncYieldInterval() {
    try {
      final long interval = store.getFuelAsyncYieldInterval();
      return interval >= 0 ? Optional.of(interval) : Optional.empty();
    } catch (Exception e) {
      LOGGER.log(Level.FINE, "Fuel async yield interval not available", e);
      return Optional.empty();
    }
  }

  @Override
  public void setFuelAsyncYieldInterval(final long interval) throws WasmException {
    if (interval < 0) {
      throw new IllegalArgumentException("Interval cannot be negative");
    }

    try {
      store.setFuelAsyncYieldInterval(interval);
    } catch (Exception e) {
      throw new WasmException("Failed to set fuel async yield interval: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the native caller handle.
   *
   * @return the native handle
   */
  long getCallerHandle() {
    return callerHandle;
  }

  /**
   * Gets the associated store.
   *
   * @return the store
   */
  PanamaStore getStore() {
    return store;
  }

  @Override
  public String toString() {
    return String.format("PanamaCaller{handle=0x%x}", callerHandle);
  }
}
