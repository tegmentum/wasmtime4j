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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.Caller;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the Caller interface for accessing WebAssembly instance context.
 *
 * <p>This class provides access to the calling WebAssembly instance's exports, memory, globals, and
 * execution state through JNI bindings to the native Wasmtime caller context.
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
final class JniCaller<T> implements Caller<T> {
  private static final Logger LOGGER = Logger.getLogger(JniCaller.class.getName());

  // Load native library
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniCaller: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  private final long callerHandle;
  private final JniStore store;

  /**
   * Generation counter value captured at construction. Every scoped method call checks {@code
   * store.getCallerGeneration() == capturedGeneration}; a mismatch indicates the caller was
   * retained past its host-callback scope and the underlying wasmtime {@code Caller} borrow is no
   * longer valid.
   */
  private final long capturedGeneration;

  /**
   * Creates a JNI caller context wrapper.
   *
   * <p>Legacy constructor that binds the caller to generation 0. Retained for source-compatibility
   * with the pre-generation-counter callsites; prefer {@link #JniCaller(long, JniStore, long)} in
   * new code so use-after-return is caught.
   *
   * @param callerHandle the native caller handle from Wasmtime
   * @param store the store this caller is associated with
   */
  JniCaller(final long callerHandle, final JniStore store) {
    this(callerHandle, store, store == null ? 0L : store.getCallerGeneration());
  }

  /**
   * Creates a JNI caller context wrapper with an explicit captured generation.
   *
   * @param callerHandle the native caller handle from Wasmtime
   * @param store the store this caller is associated with
   * @param capturedGeneration the store's caller generation at the time this caller was minted;
   *     every subsequent scoped call checks the store's current generation against this value
   * @since 1.6.0
   */
  JniCaller(final long callerHandle, final JniStore store, final long capturedGeneration) {
    if (callerHandle == 0) {
      throw new IllegalArgumentException("Caller handle cannot be 0");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }

    this.callerHandle = callerHandle;
    this.store = store;
    this.capturedGeneration = capturedGeneration;

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "Created JniCaller with handle: 0x"
              + Long.toHexString(callerHandle)
              + " gen="
              + capturedGeneration);
    }
  }

  /**
   * Verify the underlying wasmtime caller borrow is still live.
   *
   * @throws IllegalStateException if the callback has returned
   */
  private void checkStillValid() {
    final long current = store.getCallerGeneration();
    if (current != capturedGeneration) {
      throw new IllegalStateException(
          "Caller used after callback returned (captured generation "
              + capturedGeneration
              + ", store now at "
              + current
              + ")");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public T data() {
    checkStillValid();
    // Get user data from the store
    final Object storeData = store.getData();
    return storeData != null ? (T) storeData : null;
  }

  @Override
  public Optional<Extern> getExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }
    checkStillValid();

    try {
      // Try function
      final long funcHandle = nativeGetFunction(callerHandle, name);
      if (funcHandle != 0) {
        return Optional.of(new JniExternFunc(funcHandle, store));
      }

      // Try memory
      final long memHandle = nativeGetMemory(callerHandle, name);
      if (memHandle != 0) {
        return Optional.of(new JniExternMemory(memHandle, store));
      }

      // Try table
      final long tableHandle = nativeGetTable(callerHandle, name);
      if (tableHandle != 0) {
        return Optional.of(new JniExternTable(tableHandle, store));
      }

      // Try global
      final long globalHandle = nativeGetGlobal(callerHandle, name);
      if (globalHandle != 0) {
        return Optional.of(new JniExternGlobal(globalHandle, store));
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
    checkStillValid();

    try {
      final long funcHandle = nativeGetFunction(callerHandle, name);
      if (funcHandle == 0) {
        return Optional.empty();
      }
      // Module handle is 0 since we're getting the function from a caller context
      return Optional.of(new JniFunction(funcHandle, name, 0L, store));
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
    checkStillValid();

    try {
      final long memHandle = nativeGetMemory(callerHandle, name);
      if (memHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniMemory(memHandle, store));
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
    checkStillValid();

    try {
      final long tableHandle = nativeGetTable(callerHandle, name);
      if (tableHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniTable(tableHandle, store));
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
    checkStillValid();

    try {
      final long globalHandle = nativeGetGlobal(callerHandle, name);
      if (globalHandle == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniGlobal(globalHandle, store));
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
    checkStillValid();

    try {
      return nativeHasExport(callerHandle, name);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to check export: " + name, e);
      return false;
    }
  }

  @Override
  public Optional<Long> fuelRemaining() {
    checkStillValid();
    try {
      final long fuel = nativeGetFuelRemaining(callerHandle);
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
    checkStillValid();

    try {
      nativeAddFuel(callerHandle, fuel);
    } catch (Exception e) {
      throw new WasmException("Failed to add fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public void setFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel amount cannot be negative");
    }
    checkStillValid();

    try {
      nativeSetFuel(callerHandle, fuel);
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
    checkStillValid();
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
    checkStillValid();
    nativeSetFuelAsyncYieldInterval(callerHandle, interval);
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
  JniStore getStore() {
    return store;
  }

  @Override
  public String toString() {
    return String.format("JniCaller{handle=0x%x}", callerHandle);
  }

  // Native method declarations

  /**
   * Gets a memory export by name.
   *
   * @param callerHandle the native caller handle
   * @param name the memory name
   * @return native memory handle, or 0 if not found
   */
  private static native long nativeGetMemory(long callerHandle, String name);

  /**
   * Gets a global export by name.
   *
   * @param callerHandle the native caller handle
   * @param name the global name
   * @return native global handle, or 0 if not found
   */
  private static native long nativeGetGlobal(long callerHandle, String name);

  /**
   * Gets a function export by name.
   *
   * @param callerHandle the native caller handle
   * @param name the function name
   * @return native function handle, or 0 if not found
   */
  private static native long nativeGetFunction(long callerHandle, String name);

  /**
   * Gets a table export by name.
   *
   * @param callerHandle the native caller handle
   * @param name the table name
   * @return native table handle, or 0 if not found
   */
  private static native long nativeGetTable(long callerHandle, String name);

  /**
   * Checks if an export exists.
   *
   * @param callerHandle the native caller handle
   * @param name the export name
   * @return true if the export exists
   */
  private static native boolean nativeHasExport(long callerHandle, String name);

  /**
   * Gets the fuel remaining in the caller.
   *
   * @param callerHandle the native caller handle
   * @return fuel remaining, or -1 if not available
   */
  private static native long nativeGetFuelRemaining(long callerHandle);

  /**
   * Adds fuel to the caller.
   *
   * @param callerHandle the native caller handle
   * @param fuel the amount of fuel to add
   */
  private static native void nativeAddFuel(long callerHandle, long fuel);

  private static native void nativeSetFuel(long callerHandle, long fuel);

  /**
   * Sets the fuel async yield interval for the caller's store.
   *
   * @param callerHandle the native caller handle
   * @param interval the yield interval, or 0 to disable
   */
  private static native void nativeSetFuelAsyncYieldInterval(long callerHandle, long interval);

  @Override
  public java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> debugExitFrames()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    checkStillValid();
    final int[] frameData = nativeDebugExitFrames(callerHandle);
    if (frameData == null) {
      return java.util.Collections.emptyList();
    }
    final int frameCount = frameData.length / 4;
    final java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> frames =
        new java.util.ArrayList<>(frameCount);
    for (int i = 0; i < frameCount; i++) {
      final int base = i * 4;
      frames.add(
          new ai.tegmentum.wasmtime4j.debug.FrameHandle(
              0L, // no native ptr for snapshot approach
              frameData[base], // functionIndex
              frameData[base + 1], // pc
              frameData[base + 2], // numLocals
              frameData[base + 3], // numStack
              null, // instance (not available in snapshot)
              null)); // module (not available in snapshot)
    }
    return frames;
  }

  private static native int[] nativeDebugExitFrames(long callerHandle);

  // ==========================================================================
  // r.2 scoped store-mutation methods.
  // ==========================================================================

  @Override
  public ai.tegmentum.wasmtime4j.Module compileModule(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    checkStillValid();
    // Compilation is engine-scoped; delegate to the caller's store's engine so
    // the resulting module is definitely-compatible with the caller's store.
    final Engine callerEngine = store.getEngine();
    return callerEngine.compileModule(wasmBytes);
  }

  @Override
  public int growTable(final WasmTable table, final int delta, final Object init)
      throws WasmException {
    if (table == null) {
      throw new IllegalArgumentException("table cannot be null");
    }
    if (delta < 0) {
      throw new IllegalArgumentException("delta must be non-negative");
    }
    checkStillValid();
    final long tableHandle = extractHandle(table, "table");
    final long initRefId = objectToRefId(init);
    return (int) nativeCallerGrowTable(callerHandle, tableHandle, delta, initRefId);
  }

  @Override
  public void setTableElement(final WasmTable table, final int index, final Object value)
      throws WasmException {
    if (table == null) {
      throw new IllegalArgumentException("table cannot be null");
    }
    if (index < 0) {
      throw new IllegalArgumentException("index must be non-negative");
    }
    checkStillValid();
    final long tableHandle = extractHandle(table, "table");
    final long valueRefId = objectToRefId(value);
    final boolean ok = nativeCallerSetTableElement(callerHandle, tableHandle, index, valueRefId);
    if (!ok) {
      throw new WasmException("Caller-scoped table set failed");
    }
  }

  @Override
  public long growMemory(final WasmMemory memory, final long deltaPages) throws WasmException {
    if (memory == null) {
      throw new IllegalArgumentException("memory cannot be null");
    }
    if (deltaPages < 0) {
      throw new IllegalArgumentException("deltaPages must be non-negative");
    }
    checkStillValid();
    final long memoryHandle = extractHandle(memory, "memory");
    return nativeCallerGrowMemory(callerHandle, memoryHandle, deltaPages);
  }

  @Override
  public ai.tegmentum.wasmtime4j.Instance instantiate(final ai.tegmentum.wasmtime4j.InstancePre pre)
      throws WasmException {
    if (pre == null) {
      throw new IllegalArgumentException("pre cannot be null");
    }
    checkStillValid();
    if (!(pre instanceof JniInstancePre)) {
      throw new IllegalArgumentException(
          "Caller.instantiate requires a JniInstancePre; got " + pre.getClass().getName());
    }
    final JniInstancePre jniPre = (JniInstancePre) pre;
    if (!jniPre.isValid()) {
      throw new WasmException("InstancePre has been closed");
    }
    final long preHandle = jniPre.getNativeHandle();
    final long instanceHandle = nativeCallerInstantiate(callerHandle, preHandle);
    if (instanceHandle == 0L) {
      throw new WasmException("Caller-scoped instantiate returned null instance handle");
    }
    return new JniInstance(instanceHandle, jniPre.getModule(), store);
  }

  /**
   * Extract a native handle from a {@link JniResource}-backed wasmtime4j object. Callers
   * responsible for providing an object owned by this JNI backend — mixing objects across backends
   * throws {@link IllegalArgumentException}.
   */
  private static long extractHandle(final Object obj, final String argName) {
    if (obj instanceof ai.tegmentum.wasmtime4j.jni.util.JniResource) {
      return ((ai.tegmentum.wasmtime4j.jni.util.JniResource) obj).getNativeHandle();
    }
    throw new IllegalArgumentException(
        argName
            + " must be a JNI-backed wasmtime4j object, got "
            + (obj == null ? "null" : obj.getClass().getName()));
  }

  /**
   * Resolve a Java-side reference value (WasmFunction / null / etc) to the shared function
   * reference registry id used by the caller-scoped natives.
   *
   * <p>{@code null} maps to id 0 (null reference). {@code WasmFunction} instances backed by {@link
   * JniFunction} resolve to their reference id via {@link JniFunction#nativeFuncToRaw(long, long)}.
   * Other object types are rejected in r.2 — extending to externref / anyref writes is deferred.
   */
  private long objectToRefId(final Object value) throws WasmException {
    if (value == null) {
      return 0L;
    }
    if (value instanceof JniFunction) {
      final JniFunction jniFunc = (JniFunction) value;
      final long id =
          JniFunction.nativeFuncToRaw(jniFunc.getNativeHandle(), store.getNativeHandle());
      if (id == 0L) {
        throw new WasmException("Could not resolve JniFunction to a funcref id");
      }
      return id;
    }
    if (value instanceof WasmFunction) {
      throw new WasmException(
          "growTable / setTableElement with a non-JniFunction WasmFunction is not supported in"
              + " r.2");
    }
    throw new WasmException(
        "growTable / setTableElement with element of type "
            + value.getClass().getName()
            + " not supported in r.2 (funcref-only)");
  }

  /**
   * Grow a caller-visible Table (native side uses caller.as_context_mut()).
   *
   * @param callerHandle the native caller handle
   * @param tableHandle the target table's native handle
   * @param delta number of slots to add
   * @param initRefId funcref registry id or 0 for null
   * @return previous table size, or -1 on failure
   */
  private static native long nativeCallerGrowTable(
      long callerHandle, long tableHandle, int delta, long initRefId);

  /**
   * Set a caller-visible Table element (native side uses caller.as_context_mut()).
   *
   * @param callerHandle the native caller handle
   * @param tableHandle the target table's native handle
   * @param index target slot index
   * @param valueRefId funcref registry id or 0 for null
   * @return true on success
   */
  private static native boolean nativeCallerSetTableElement(
      long callerHandle, long tableHandle, int index, long valueRefId);

  /**
   * Grow a caller-visible Memory (native side uses caller.as_context_mut()).
   *
   * @param callerHandle the native caller handle
   * @param memoryHandle the target memory's native handle
   * @param deltaPages number of pages to add
   * @return previous size in pages, or -1 on failure
   */
  private static native long nativeCallerGrowMemory(
      long callerHandle, long memoryHandle, long deltaPages);

  /**
   * Instantiate an {@link ai.tegmentum.wasmtime4j.InstancePre} against the caller's live wasmtime
   * context (native side uses {@code caller.as_context_mut()} + {@code
   * InstancePreWrapper::instantiate_with_context}). Skips the Store's ReentrantLock so it is
   * reentrant-safe from a host-callback frame.
   *
   * @param callerHandle the native caller handle
   * @param instancePreHandle the target InstancePre's native handle
   * @return handle to the newly constructed Instance (pointer to {@code Box<Instance>}), or 0 on
   *     failure
   */
  private static native long nativeCallerInstantiate(long callerHandle, long instancePreHandle);

  @Override
  public byte[] readMemory(final String memoryName, final long offset, final int length)
      throws WasmException {
    if (memoryName == null) {
      throw new IllegalArgumentException("memoryName cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be non-negative");
    }
    if (length < 0) {
      throw new IllegalArgumentException("length must be non-negative");
    }
    checkStillValid();
    return nativeCallerReadMemory(callerHandle, memoryName, offset, length);
  }

  @Override
  public void writeMemory(final String memoryName, final long offset, final byte[] bytes)
      throws WasmException {
    if (memoryName == null) {
      throw new IllegalArgumentException("memoryName cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be non-negative");
    }
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    checkStillValid();
    nativeCallerWriteMemory(callerHandle, memoryName, offset, bytes);
  }

  /**
   * Read a byte range from a caller-exported memory by name, via the caller-scoped native path.
   *
   * @param callerHandle the native caller handle
   * @param memoryName name of the exported memory to look up
   * @param offset byte offset into the memory
   * @param length number of bytes to read
   * @return the byte array read
   */
  private static native byte[] nativeCallerReadMemory(
      long callerHandle, String memoryName, long offset, int length);

  /**
   * Write a byte range into a caller-exported memory by name, via the caller-scoped native path.
   *
   * @param callerHandle the native caller handle
   * @param memoryName name of the exported memory to look up
   * @param offset byte offset into the memory
   * @param bytes bytes to write
   */
  private static native void nativeCallerWriteMemory(
      long callerHandle, String memoryName, long offset, byte[] bytes);

  /**
   * Define a memory extern on a linker using this caller's live store context
   * (F-Wasmtime4j-Caller-Scoped-Instantiate-Extern-Imports r.1 2026-07-27).
   *
   * <p>Package-private — the intended consumer is the webassembly4j
   * WasmtimeCallerAdapter, which uses this to wire {@code MemoryImport}
   * defs into a transient linker built from a callback frame.
   *
   * @param linker the JniLinker to define the memory on (its native handle
   *     will be extracted internally)
   * @param moduleName import module name (e.g. "env")
   * @param name import field name (e.g. "memory")
   * @param memory the WasmMemory extern to bind
   * @throws WasmException on failure
   * @since 1.5.2
   */
  @Override
  public void linkerDefineMemory(
      final Linker<?> linker,
      final String moduleName,
      final String name,
      final WasmMemory memory)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("linker cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("moduleName cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (memory == null) {
      throw new IllegalArgumentException("memory cannot be null");
    }
    if (!(linker instanceof JniLinker)) {
      throw new IllegalArgumentException(
          "Caller.linkerDefineMemory requires a JniLinker; got " + linker.getClass().getName());
    }
    checkStillValid();
    final long memoryHandle = extractHandle(memory, "memory");
    final long linkerHandle = ((JniLinker<?>) linker).getNativeHandle();
    final boolean ok =
        nativeCallerLinkerDefineMemory(callerHandle, linkerHandle, moduleName, name, memoryHandle);
    if (!ok) {
      throw new WasmException(
          "Caller-scoped Linker.defineMemory '" + moduleName + "::" + name + "' returned false");
    }
  }

  @Override
  public void linkerDefineTable(
      final Linker<?> linker,
      final String moduleName,
      final String name,
      final WasmTable table)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("linker cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("moduleName cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (table == null) {
      throw new IllegalArgumentException("table cannot be null");
    }
    if (!(linker instanceof JniLinker)) {
      throw new IllegalArgumentException(
          "Caller.linkerDefineTable requires a JniLinker; got " + linker.getClass().getName());
    }
    checkStillValid();
    final long tableHandle = extractHandle(table, "table");
    final long linkerHandle = ((JniLinker<?>) linker).getNativeHandle();
    final boolean ok =
        nativeCallerLinkerDefineTable(callerHandle, linkerHandle, moduleName, name, tableHandle);
    if (!ok) {
      throw new WasmException(
          "Caller-scoped Linker.defineTable '" + moduleName + "::" + name + "' returned false");
    }
  }

  @Override
  public void linkerDefineGlobal(
      final Linker<?> linker,
      final String moduleName,
      final String name,
      final WasmGlobal global)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("linker cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("moduleName cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (global == null) {
      throw new IllegalArgumentException("global cannot be null");
    }
    if (!(linker instanceof JniLinker)) {
      throw new IllegalArgumentException(
          "Caller.linkerDefineGlobal requires a JniLinker; got " + linker.getClass().getName());
    }
    checkStillValid();
    final long globalHandle = extractHandle(global, "global");
    final long linkerHandle = ((JniLinker<?>) linker).getNativeHandle();
    final boolean ok =
        nativeCallerLinkerDefineGlobal(callerHandle, linkerHandle, moduleName, name, globalHandle);
    if (!ok) {
      throw new WasmException(
          "Caller-scoped Linker.defineGlobal '" + moduleName + "::" + name + "' returned false");
    }
  }

  private static native boolean nativeCallerLinkerDefineMemory(
      long callerHandle, long linkerHandle, String moduleName, String name, long memoryHandle);

  private static native boolean nativeCallerLinkerDefineTable(
      long callerHandle, long linkerHandle, String moduleName, String name, long tableHandle);

  private static native boolean nativeCallerLinkerDefineGlobal(
      long callerHandle, long linkerHandle, String moduleName, String name, long globalHandle);

  @Override
  public void linkerDefineMemoryFromExport(
      final Linker<?> linker,
      final String moduleName,
      final String name,
      final String callerExportName)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("linker cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("moduleName cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (callerExportName == null) {
      throw new IllegalArgumentException("callerExportName cannot be null");
    }
    if (!(linker instanceof JniLinker)) {
      throw new IllegalArgumentException(
          "Caller.linkerDefineMemoryFromExport requires a JniLinker; got "
              + linker.getClass().getName());
    }
    checkStillValid();
    final long linkerHandle = ((JniLinker<?>) linker).getNativeHandle();
    final boolean ok =
        nativeCallerLinkerDefineMemoryFromExport(
            callerHandle, linkerHandle, moduleName, name, callerExportName);
    if (!ok) {
      throw new WasmException(
          "Caller-scoped Linker.defineMemoryFromExport '"
              + moduleName
              + "::"
              + name
              + "' (from caller export '"
              + callerExportName
              + "') returned false");
    }
  }

  @Override
  public void linkerDefineTableFromExport(
      final Linker<?> linker,
      final String moduleName,
      final String name,
      final String callerExportName)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("linker cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("moduleName cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (callerExportName == null) {
      throw new IllegalArgumentException("callerExportName cannot be null");
    }
    if (!(linker instanceof JniLinker)) {
      throw new IllegalArgumentException(
          "Caller.linkerDefineTableFromExport requires a JniLinker; got "
              + linker.getClass().getName());
    }
    checkStillValid();
    final long linkerHandle = ((JniLinker<?>) linker).getNativeHandle();
    final boolean ok =
        nativeCallerLinkerDefineTableFromExport(
            callerHandle, linkerHandle, moduleName, name, callerExportName);
    if (!ok) {
      throw new WasmException(
          "Caller-scoped Linker.defineTableFromExport '"
              + moduleName
              + "::"
              + name
              + "' (from caller export '"
              + callerExportName
              + "') returned false");
    }
  }

  private static native boolean nativeCallerLinkerDefineMemoryFromExport(
      long callerHandle,
      long linkerHandle,
      String moduleName,
      String name,
      String callerExportName);

  private static native boolean nativeCallerLinkerDefineTableFromExport(
      long callerHandle,
      long linkerHandle,
      String moduleName,
      String name,
      String callerExportName);
}
