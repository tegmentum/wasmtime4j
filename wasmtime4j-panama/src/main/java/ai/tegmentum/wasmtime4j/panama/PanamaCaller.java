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
import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
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
  public Optional<Extern> getExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(name);

      // Try function
      final MemorySegment funcOut = arena.allocate(ValueLayout.ADDRESS);
      if (bindings.callerGetFunction(callerPtr, nameSegment, funcOut) == 0) {
        final MemorySegment handle = funcOut.get(ValueLayout.ADDRESS, 0);
        if (!handle.equals(MemorySegment.NULL) && handle.address() != 0) {
          return Optional.of(new PanamaExternFunc(handle, store));
        }
      }

      // Try memory
      final MemorySegment memOut = arena.allocate(ValueLayout.ADDRESS);
      if (bindings.callerGetMemory(callerPtr, nameSegment, memOut) == 0) {
        final MemorySegment handle = memOut.get(ValueLayout.ADDRESS, 0);
        if (!handle.equals(MemorySegment.NULL) && handle.address() != 0) {
          return Optional.of(new PanamaExternMemory(handle, store));
        }
      }

      // Try table
      final MemorySegment tableOut = arena.allocate(ValueLayout.ADDRESS);
      if (bindings.callerGetTable(callerPtr, nameSegment, tableOut) == 0) {
        final MemorySegment handle = tableOut.get(ValueLayout.ADDRESS, 0);
        if (!handle.equals(MemorySegment.NULL) && handle.address() != 0) {
          return Optional.of(new PanamaExternTable(handle, store));
        }
      }

      // Try global
      final MemorySegment globalOut = arena.allocate(ValueLayout.ADDRESS);
      if (bindings.callerGetGlobal(callerPtr, nameSegment, globalOut) == 0) {
        final MemorySegment handle = globalOut.get(ValueLayout.ADDRESS, 0);
        if (!handle.equals(MemorySegment.NULL) && handle.address() != 0) {
          return Optional.of(new PanamaExternGlobal(handle, store));
        }
      }

      return Optional.empty();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get export: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Extern> getExport(final ModuleExport moduleExport) {
    if (moduleExport == null) {
      throw new IllegalArgumentException("ModuleExport cannot be null");
    }
    // Delegate to name-based lookup using the ModuleExport's name
    return getExport(moduleExport.name());
  }

  @Override
  public Optional<WasmFunction> getFunction(final String name) {
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
      return Optional.of(new PanamaCallerFunction(funcHandle, store, name));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get function: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmMemory> getMemory(final String name) {
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
      return Optional.of(new PanamaMemory(memoryHandle, store));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get memory: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmTable> getTable(final String name) {
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
      return Optional.of(new PanamaTable(tableHandle, store));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get table: " + name, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final String name) {
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
      return Optional.of(new PanamaGlobal(globalHandle, store));
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
        throw PanamaErrorMapper.mapNativeError(result, "Failed to add fuel");
      }
    } catch (WasmException e) {
      throw e;
    } catch (Exception e) {
      throw new WasmException("Failed to add fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public void setFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel amount cannot be negative");
    }

    try {
      final int result = bindings.callerSetFuel(callerPtr, fuel);
      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to set fuel");
      }
    } catch (WasmException e) {
      throw e;
    } catch (Exception e) {
      throw new WasmException("Failed to set fuel: " + e.getMessage(), e);
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
  public void setFuelAsyncYieldInterval(final long interval) throws WasmException {
    if (interval < 0) {
      throw new IllegalArgumentException("interval cannot be negative");
    }
    final int result = bindings.callerSetFuelAsyncYieldInterval(callerPtr, interval);
    if (result != 0) {
      throw new WasmException("Failed to set fuel async yield interval");
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
  public java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> debugExitFrames()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    try (final java.lang.foreign.Arena localArena = java.lang.foreign.Arena.ofConfined()) {
      // Phase 1: get frame count
      final java.lang.foreign.MemorySegment countSegment =
          localArena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);
      final int countResult =
          bindings.callerDebugExitFrames(
              callerPtr, countSegment, java.lang.foreign.MemorySegment.NULL);
      if (countResult == -1) {
        return java.util.Collections.emptyList(); // debugging not enabled
      }
      if (countResult < 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to get debug exit frames: error code " + countResult);
      }
      final int frameCount = countSegment.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);
      if (frameCount <= 0) {
        return java.util.Collections.emptyList();
      }

      // Phase 2: allocate buffer and get frame data
      final java.lang.foreign.MemorySegment dataSegment =
          localArena.allocate(java.lang.foreign.ValueLayout.JAVA_INT, (long) frameCount * 4);
      final int dataResult = bindings.callerDebugExitFrames(callerPtr, countSegment, dataSegment);
      if (dataResult < 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to get debug exit frame data: error code " + dataResult);
      }

      // Parse frame data: 4 ints per frame [func_index, pc, num_locals, num_stacks]
      final java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> frames =
          new java.util.ArrayList<>(frameCount);
      for (int i = 0; i < frameCount; i++) {
        final long base = (long) i * 4 * java.lang.foreign.ValueLayout.JAVA_INT.byteSize();
        frames.add(
            new ai.tegmentum.wasmtime4j.debug.FrameHandle(
                0L, // no native ptr for snapshot approach
                dataSegment.get(java.lang.foreign.ValueLayout.JAVA_INT, base),
                dataSegment.get(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    base + java.lang.foreign.ValueLayout.JAVA_INT.byteSize()),
                dataSegment.get(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    base + 2 * java.lang.foreign.ValueLayout.JAVA_INT.byteSize()),
                dataSegment.get(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    base + 3 * java.lang.foreign.ValueLayout.JAVA_INT.byteSize()),
                null, // instance (not available in snapshot)
                null)); // module (not available in snapshot)
      }
      return frames;
    }
  }

  @Override
  public String toString() {
    return String.format("PanamaCaller{handle=0x%x}", callerHandle);
  }
}
