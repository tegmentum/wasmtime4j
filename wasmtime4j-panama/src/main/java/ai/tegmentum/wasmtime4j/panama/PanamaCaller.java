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

  // ===========================================================================
  // F-Wasmtime4j-Panama-Caller-Scoped-Mutation-Java-Bindings r.3 slice 2
  // (2026-07-28). Overrides the 9 Caller<T> mutation methods that inherit UOE
  // defaults from the interface. Delegates to NativeInstanceBindings wrappers
  // added by slice 1.
  //
  // Concrete-type discipline: mutation args must be Panama-tier types
  // (PanamaTable, PanamaMemory, PanamaGlobal, PanamaLinker, PanamaInstancePre).
  // Passing a JNI-tier or api-level wrapper throws IllegalArgumentException
  // — mixing binding tiers is an operator error.
  //
  // Charter-scope exclusions preserved: {@code init} on growTable and
  // {@code value} on setTableElement must be null (registry-id 0). Non-null
  // funcref requires the FuncToRegistryId FFI which is out-of-scope for
  // Panama's cleaner call surface (see charter §Out-of-scope).
  // ===========================================================================

  @Override
  public int growTable(
      final ai.tegmentum.wasmtime4j.WasmTable table, final int delta, final Object init)
      throws WasmException {
    if (table == null) {
      throw new IllegalArgumentException("table cannot be null");
    }
    if (delta < 0) {
      throw new IllegalArgumentException("delta cannot be negative");
    }
    final long initRefId = resolveRefIdForMutation(init, "growTable");
    final MemorySegment tablePtr = extractPanamaTable(table).getNativeTable();
    final long prev = bindings.callerGrowTable(callerPtr, tablePtr, delta, initRefId);
    if (prev < 0) {
      throw new WasmException(
          "PanamaCaller.growTable failed (native returned " + prev + "); check last error");
    }
    return (int) prev;
  }

  @Override
  public void setTableElement(
      final ai.tegmentum.wasmtime4j.WasmTable table, final int index, final Object value)
      throws WasmException {
    if (table == null) {
      throw new IllegalArgumentException("table cannot be null");
    }
    if (index < 0) {
      throw new IllegalArgumentException("index cannot be negative");
    }
    final long valueRefId = resolveRefIdForMutation(value, "setTableElement");
    final MemorySegment tablePtr = extractPanamaTable(table).getNativeTable();
    final int rc = bindings.callerSetTableElement(callerPtr, tablePtr, index, valueRefId);
    if (rc != 0) {
      throw new WasmException(
          "PanamaCaller.setTableElement failed (native returned " + rc + ")");
    }
  }

  @Override
  public long growMemory(
      final ai.tegmentum.wasmtime4j.WasmMemory memory, final long deltaPages)
      throws WasmException {
    if (memory == null) {
      throw new IllegalArgumentException("memory cannot be null");
    }
    if (deltaPages < 0) {
      throw new IllegalArgumentException("deltaPages cannot be negative");
    }
    final MemorySegment memoryPtr = extractPanamaMemory(memory).getNativeMemory();
    final long prev = bindings.callerGrowMemory(callerPtr, memoryPtr, deltaPages);
    if (prev < 0) {
      throw new WasmException(
          "PanamaCaller.growMemory failed (native returned " + prev + ")");
    }
    return prev;
  }

  @Override
  public byte[] readMemory(final String memoryName, final long offset, final int length)
      throws WasmException {
    if (memoryName == null) {
      throw new IllegalArgumentException("memoryName cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset cannot be negative");
    }
    if (length < 0) {
      throw new IllegalArgumentException("length cannot be negative");
    }
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(memoryName);
      final MemorySegment outBuf = arena.allocate(length);
      final int rc = bindings.callerReadMemory(callerPtr, nameSegment, offset, length, outBuf);
      if (rc != 0) {
        throw new WasmException(
            "PanamaCaller.readMemory('" + memoryName + "') failed (native returned " + rc + ")");
      }
      final byte[] result = new byte[length];
      MemorySegment.copy(outBuf, ValueLayout.JAVA_BYTE, 0, result, 0, length);
      return result;
    }
  }

  @Override
  public void writeMemory(final String memoryName, final long offset, final byte[] bytes)
      throws WasmException {
    if (memoryName == null) {
      throw new IllegalArgumentException("memoryName cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset cannot be negative");
    }
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(memoryName);
      final MemorySegment bytesSegment = arena.allocate(bytes.length);
      MemorySegment.copy(bytes, 0, bytesSegment, ValueLayout.JAVA_BYTE, 0, bytes.length);
      final int rc =
          bindings.callerWriteMemory(callerPtr, nameSegment, offset, bytesSegment, bytes.length);
      if (rc != 0) {
        throw new WasmException(
            "PanamaCaller.writeMemory('" + memoryName + "') failed (native returned " + rc + ")");
      }
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Instance instantiate(final ai.tegmentum.wasmtime4j.InstancePre pre)
      throws WasmException {
    if (pre == null) {
      throw new IllegalArgumentException("pre cannot be null");
    }
    if (!(pre instanceof PanamaInstancePre panamaPre)) {
      throw new IllegalArgumentException(
          "PanamaCaller.instantiate requires a PanamaInstancePre; got "
              + pre.getClass().getName());
    }
    final ai.tegmentum.wasmtime4j.Module module = panamaPre.getModule();
    if (!(module instanceof PanamaModule panamaModule)) {
      throw new IllegalArgumentException(
          "PanamaCaller.instantiate: InstancePre's module is not a PanamaModule");
    }
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment prePtr = panamaPre.getNativeInstancePre();
      final MemorySegment instanceOut = arena.allocate(ValueLayout.ADDRESS);
      final int rc = bindings.callerInstantiate(callerPtr, prePtr, instanceOut);
      if (rc != 0) {
        throw new WasmException(
            "PanamaCaller.instantiate failed (native returned " + rc + ")");
      }
      final MemorySegment instanceHandle = instanceOut.get(ValueLayout.ADDRESS, 0);
      if (instanceHandle == null || instanceHandle.equals(MemorySegment.NULL)) {
        throw new WasmException(
            "PanamaCaller.instantiate returned success but instance handle is null");
      }
      return new PanamaInstance(instanceHandle, panamaModule, store);
    }
  }

  @Override
  public void linkerDefineMemory(
      final ai.tegmentum.wasmtime4j.Linker<?> linker,
      final String moduleName,
      final String name,
      final ai.tegmentum.wasmtime4j.WasmMemory memory)
      throws WasmException {
    validateLinkerDefineArgs(linker, moduleName, name, memory, "linkerDefineMemory");
    final MemorySegment linkerPtr = extractPanamaLinker(linker).getNativeLinker();
    final MemorySegment memoryPtr = extractPanamaMemory(memory).getNativeMemory();
    try (final Arena arena = Arena.ofConfined()) {
      final int rc =
          bindings.callerLinkerDefineMemory(
              callerPtr,
              linkerPtr,
              arena.allocateFrom(moduleName),
              arena.allocateFrom(name),
              memoryPtr);
      if (rc != 0) {
        throw new WasmException(
            "PanamaCaller.linkerDefineMemory '"
                + moduleName
                + "::"
                + name
                + "' failed (native returned "
                + rc
                + ")");
      }
    }
  }

  @Override
  public void linkerDefineTable(
      final ai.tegmentum.wasmtime4j.Linker<?> linker,
      final String moduleName,
      final String name,
      final ai.tegmentum.wasmtime4j.WasmTable table)
      throws WasmException {
    validateLinkerDefineArgs(linker, moduleName, name, table, "linkerDefineTable");
    final MemorySegment linkerPtr = extractPanamaLinker(linker).getNativeLinker();
    final MemorySegment tablePtr = extractPanamaTable(table).getNativeTable();
    try (final Arena arena = Arena.ofConfined()) {
      final int rc =
          bindings.callerLinkerDefineTable(
              callerPtr,
              linkerPtr,
              arena.allocateFrom(moduleName),
              arena.allocateFrom(name),
              tablePtr);
      if (rc != 0) {
        throw new WasmException(
            "PanamaCaller.linkerDefineTable '"
                + moduleName
                + "::"
                + name
                + "' failed (native returned "
                + rc
                + ")");
      }
    }
  }

  @Override
  public void linkerDefineGlobal(
      final ai.tegmentum.wasmtime4j.Linker<?> linker,
      final String moduleName,
      final String name,
      final ai.tegmentum.wasmtime4j.WasmGlobal global)
      throws WasmException {
    validateLinkerDefineArgs(linker, moduleName, name, global, "linkerDefineGlobal");
    final MemorySegment linkerPtr = extractPanamaLinker(linker).getNativeLinker();
    final MemorySegment globalPtr = extractPanamaGlobal(global).getNativeGlobal();
    try (final Arena arena = Arena.ofConfined()) {
      final int rc =
          bindings.callerLinkerDefineGlobal(
              callerPtr,
              linkerPtr,
              arena.allocateFrom(moduleName),
              arena.allocateFrom(name),
              globalPtr);
      if (rc != 0) {
        throw new WasmException(
            "PanamaCaller.linkerDefineGlobal '"
                + moduleName
                + "::"
                + name
                + "' failed (native returned "
                + rc
                + ")");
      }
    }
  }

  // ===========================================================================
  // F-Wasmtime4j-Panama-Consumer-Gated-Followups r.3 + r.4 (2026-07-28).
  //
  // Overrides the remaining 3 Caller<T> mutation methods that inherited UOE
  // defaults after the predecessor mutation-java-bindings charter:
  //   - linkerDefineMemoryFromExport (r.3)
  //   - linkerDefineTableFromExport  (r.3)
  //   - compileModule                (r.4)
  //
  // Behaviour is parity with JniCaller (JNI-tier reference at
  // wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/JniCaller.java:469,
  // :833, :873).
  // ===========================================================================

  @Override
  public ai.tegmentum.wasmtime4j.Module compileModule(final byte[] wasmBytes)
      throws WasmException {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    // Engine-scoped compilation. Panama already has PanamaEngine.compileModule
    // at PanamaEngine.java:215; delegate through the caller's store's engine
    // so the resulting Module is definitely-compatible with the caller's store.
    // Parity with JniCaller.compileModule (JniCaller.java:469-478).
    final Engine engine = store.getEngine();
    return engine.compileModule(wasmBytes);
  }

  @Override
  public void linkerDefineMemoryFromExport(
      final ai.tegmentum.wasmtime4j.Linker<?> linker,
      final String moduleName,
      final String name,
      final String callerExportName)
      throws WasmException {
    validateLinkerDefineArgs(
        linker, moduleName, name, callerExportName, "linkerDefineMemoryFromExport");
    final MemorySegment linkerPtr = extractPanamaLinker(linker).getNativeLinker();
    try (final Arena arena = Arena.ofConfined()) {
      final int rc =
          bindings.callerLinkerDefineMemoryFromExport(
              callerPtr,
              linkerPtr,
              arena.allocateFrom(moduleName),
              arena.allocateFrom(name),
              arena.allocateFrom(callerExportName));
      if (rc != 0) {
        throw new WasmException(
            "PanamaCaller.linkerDefineMemoryFromExport '"
                + moduleName
                + "::"
                + name
                + "' (from caller export '"
                + callerExportName
                + "') failed (native returned "
                + rc
                + ")");
      }
    }
  }

  @Override
  public void linkerDefineTableFromExport(
      final ai.tegmentum.wasmtime4j.Linker<?> linker,
      final String moduleName,
      final String name,
      final String callerExportName)
      throws WasmException {
    validateLinkerDefineArgs(
        linker, moduleName, name, callerExportName, "linkerDefineTableFromExport");
    final MemorySegment linkerPtr = extractPanamaLinker(linker).getNativeLinker();
    try (final Arena arena = Arena.ofConfined()) {
      final int rc =
          bindings.callerLinkerDefineTableFromExport(
              callerPtr,
              linkerPtr,
              arena.allocateFrom(moduleName),
              arena.allocateFrom(name),
              arena.allocateFrom(callerExportName));
      if (rc != 0) {
        throw new WasmException(
            "PanamaCaller.linkerDefineTableFromExport '"
                + moduleName
                + "::"
                + name
                + "' (from caller export '"
                + callerExportName
                + "') failed (native returned "
                + rc
                + ")");
      }
    }
  }

  // --- helpers ---

  private static PanamaTable extractPanamaTable(final ai.tegmentum.wasmtime4j.WasmTable table) {
    if (table instanceof PanamaTable pt) {
      return pt;
    }
    throw new IllegalArgumentException(
        "PanamaCaller mutation ops require PanamaTable; got " + table.getClass().getName());
  }

  private static PanamaMemory extractPanamaMemory(final ai.tegmentum.wasmtime4j.WasmMemory memory) {
    if (memory instanceof PanamaMemory pm) {
      return pm;
    }
    throw new IllegalArgumentException(
        "PanamaCaller mutation ops require PanamaMemory; got " + memory.getClass().getName());
  }

  private static PanamaGlobal extractPanamaGlobal(final ai.tegmentum.wasmtime4j.WasmGlobal global) {
    if (global instanceof PanamaGlobal pg) {
      return pg;
    }
    throw new IllegalArgumentException(
        "PanamaCaller mutation ops require PanamaGlobal; got " + global.getClass().getName());
  }

  private static PanamaLinker<?> extractPanamaLinker(
      final ai.tegmentum.wasmtime4j.Linker<?> linker) {
    if (linker instanceof PanamaLinker<?> pl) {
      return pl;
    }
    throw new IllegalArgumentException(
        "PanamaCaller.linkerDefine* requires PanamaLinker; got " + linker.getClass().getName());
  }

  private void validateLinkerDefineArgs(
      final ai.tegmentum.wasmtime4j.Linker<?> linker,
      final String moduleName,
      final String name,
      final Object extern,
      final String opName) {
    if (linker == null) {
      throw new IllegalArgumentException(opName + ": linker cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException(opName + ": moduleName cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException(opName + ": name cannot be null");
    }
    if (extern == null) {
      throw new IllegalArgumentException(opName + ": extern cannot be null");
    }
  }

  /**
   * Resolve a mutation-op {@code init}/{@code value} argument to a funcref
   * registry id.
   *
   * <p>Supported values (F-Wasmtime4j-Panama-Consumer-Gated-Followups r.2,
   * 2026-07-28):
   * <ul>
   *   <li>{@code null} → registry id 0 (null funcref sentinel).
   *   <li>{@link PanamaCallerFunction} — extracts its {@code funcHandle}
   *       {@link MemorySegment} and registers via
   *       {@link NativeInstanceBindings#callerFuncToRegistryId}.
   *   <li>{@link PanamaHostFunction} — extracts its
   *       {@link PanamaHostFunction#getFunctionHandle} and registers via
   *       the same path.
   * </ul>
   *
   * <p>Other {@link WasmFunction} implementations (including {@link PanamaFunction}
   * which does not hold a direct native handle — it dispatches by name through
   * an instance) still throw {@link IllegalArgumentException}. Consumers wanting
   * to pass a name-dispatch {@link PanamaFunction} should first materialize it
   * through a {@link PanamaHostFunction} export lookup, mirroring how JNI
   * consumers pass {@code JniFunction}.
   */
  private long resolveRefIdForMutation(final Object value, final String opName)
      throws WasmException {
    if (value == null) {
      return 0L;
    }
    final MemorySegment funcPtr;
    if (value instanceof PanamaCallerFunction pcf) {
      funcPtr = pcf.getFuncHandle();
    } else if (value instanceof PanamaHostFunction phf) {
      funcPtr = phf.getFunctionHandle();
    } else {
      throw new IllegalArgumentException(
          "PanamaCaller."
              + opName
              + ": non-null init/value must be a PanamaCallerFunction or "
              + "PanamaHostFunction. PanamaFunction (name-dispatch) has no "
              + "direct native handle — materialize it via a caller export lookup "
              + "first. Passed value type: "
              + value.getClass().getName());
    }
    if (funcPtr == null || funcPtr.equals(MemorySegment.NULL) || funcPtr.address() == 0) {
      throw new WasmException(
          "PanamaCaller." + opName + ": function handle is null or zero");
    }
    final long id = bindings.callerFuncToRegistryId(callerPtr, funcPtr);
    if (id == 0L) {
      throw new WasmException(
          "PanamaCaller." + opName + ": could not register function into REFERENCE_REGISTRY");
    }
    return id;
  }

  @Override
  public String toString() {
    return String.format("PanamaCaller{handle=0x%x}", callerHandle);
  }
}
