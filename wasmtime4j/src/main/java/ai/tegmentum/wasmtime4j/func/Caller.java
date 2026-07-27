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
package ai.tegmentum.wasmtime4j.func;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * Provides access to the calling WebAssembly instance context within host functions.
 *
 * <p>The Caller interface allows host functions to access exports from the calling WebAssembly
 * instance, including memory, tables, globals, and functions. This enables host functions to
 * interact with the WebAssembly module's state and resources.
 *
 * <p>Caller instances are passed to host functions that are defined with caller context support and
 * provide safe access to the execution environment.
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
public interface Caller<T> {

  /**
   * Gets the user data associated with the store.
   *
   * <p>This is the same data that was provided when creating the store and can be used to maintain
   * state across host function calls.
   *
   * @return the store's user data
   * @since 1.0.0
   */
  T data();

  /**
   * Gets an exported item by name from the calling instance.
   *
   * <p>This method provides access to any export (function, memory, table, or global) that the
   * calling WebAssembly instance has made available.
   *
   * @param name the name of the export to retrieve
   * @return the export if it exists, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<Extern> getExport(String name);

  /**
   * Gets an exported item using a pre-resolved {@link ModuleExport} handle for O(1) lookup.
   *
   * <p>This method provides fast export access by using a cached index handle obtained from {@link
   * Module#getModuleExport(String)}.
   *
   * @param moduleExport the pre-resolved export handle
   * @return the export if it exists, empty otherwise
   * @throws IllegalArgumentException if moduleExport is null
   * @since 1.1.0
   */
  Optional<Extern> getExport(ModuleExport moduleExport);

  /**
   * Gets an exported function by name from the calling instance.
   *
   * @param name the name of the function export
   * @return the function if it exists and is a function, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmFunction> getFunction(String name);

  /**
   * Gets an exported memory by name from the calling instance.
   *
   * @param name the name of the memory export
   * @return the memory if it exists and is a memory, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmMemory> getMemory(String name);

  /**
   * Gets the default memory export from the calling instance.
   *
   * <p>This is a convenience method that looks for a memory export named "memory", which is the
   * default export name for WebAssembly memory.
   *
   * @return the default memory if it exists, empty otherwise
   * @since 1.0.0
   */
  default Optional<WasmMemory> getMemory() {
    return getMemory("memory");
  }

  /**
   * Gets an exported table by name from the calling instance.
   *
   * @param name the name of the table export
   * @return the table if it exists and is a table, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmTable> getTable(String name);

  /**
   * Gets an exported global by name from the calling instance.
   *
   * @param name the name of the global export
   * @return the global if it exists and is a global, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmGlobal> getGlobal(String name);

  /**
   * Checks if the calling instance has an export with the given name.
   *
   * @param name the name to check for
   * @return true if an export with that name exists
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  boolean hasExport(String name);

  /**
   * Gets the fuel remaining in the caller if fuel metering is enabled.
   *
   * <p>This method returns the amount of fuel remaining for the current execution. When fuel is
   * exhausted, the WebAssembly execution will be interrupted.
   *
   * @return the fuel remaining, or empty if fuel metering is not enabled
   * @since 1.0.0
   */
  Optional<Long> fuelRemaining();

  /**
   * Adds fuel to the caller's fuel tank.
   *
   * <p>This allows extending the execution time during host function calls. The added fuel becomes
   * immediately available for continued execution.
   *
   * @param fuel the amount of fuel to add
   * @throws WasmException if fuel metering is not enabled or if adding fuel fails
   * @throws IllegalArgumentException if fuel is negative
   * @since 1.0.0
   */
  void addFuel(long fuel) throws WasmException;

  /**
   * Sets the fuel level to a specific value.
   *
   * <p>This replaces the current fuel amount rather than adding to it. The fuel value becomes
   * immediately effective for continued execution.
   *
   * @param fuel the fuel level to set
   * @throws WasmException if fuel metering is not enabled or if setting fuel fails
   * @throws IllegalArgumentException if fuel is negative
   * @since 1.0.0
   */
  void setFuel(long fuel) throws WasmException;

  /**
   * Gets the engine associated with the caller's store.
   *
   * <p>This provides access to engine configuration during host function execution, which can be
   * useful for checking enabled features or accessing shared engine state.
   *
   * @return the Engine associated with this caller
   * @since 1.0.0
   */
  Engine engine();

  /**
   * Triggers garbage collection from within a host function.
   *
   * <p>This is useful for managing memory during long-running host operations that may have
   * accumulated many unreferenced GC objects. Unlike the synchronous GC call, this method is
   * designed for use within host function contexts.
   *
   * <p><b>Note:</b> GC support must be enabled in the engine configuration for this method to have
   * any effect.
   *
   * @throws WasmException if the GC operation fails
   * @since 1.0.0
   */
  void gc() throws WasmException;

  /**
   * Performs garbage collection asynchronously.
   *
   * <p>This is the async variant of {@link #gc()} that cooperatively yields during collection.
   * Requires the store to have async support enabled.
   *
   * @return a future that completes when GC is finished
   * @since 1.1.0
   */
  default java.util.concurrent.CompletableFuture<Void> gcAsync() {
    return java.util.concurrent.CompletableFuture.runAsync(
        () -> {
          try {
            gc();
          } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Configures the fuel-based async yield interval for this caller's store.
   *
   * <p>When both fuel consumption and async support are enabled, this controls how frequently the
   * WebAssembly execution yields back to the async executor. A value of 0 disables automatic
   * yielding.
   *
   * @param interval the fuel interval between yields, or 0 to disable
   * @throws WasmException if the configuration fails
   * @throws IllegalArgumentException if interval is negative
   * @since 1.0.0
   */
  void setFuelAsyncYieldInterval(long interval) throws WasmException;

  /**
   * Gets the debug exit frames from the caller's execution context.
   *
   * <p>This provides a snapshot of the WebAssembly call stack, including function indices, program
   * counters, local variable counts, and operand stack depths. Requires the engine to be configured
   * with {@code guestDebug(true)}.
   *
   * @return a list of frame handles, innermost first
   * @throws UnsupportedOperationException if guest debugging is not enabled
   * @throws WasmException if frame retrieval fails
   * @since 1.1.0
   */
  default java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> debugExitFrames()
      throws WasmException {
    throw new UnsupportedOperationException(
        "debugExitFrames requires guest debugging to be enabled via Config.guestDebug(true)");
  }

  // ==========================================================================
  // r.2 scoped store-mutation methods (F-Wasmtime4j-Caller-Aware-Host-Function).
  //
  // These are the borrow-safe reentrant-mutation entrypoints. They route through
  // the callback's wasmtime Caller<'_, StoreData> handle instead of acquiring a
  // fresh Store lock, so a host function can compile modules, grow tables /
  // memories, and install funcref elements without tripping the
  // reentrant-mutation SIGSEGV witnessed by F-JIT-Loader-Java-Reference r.5.b.
  //
  // Every call is generation-checked in the JNI implementation: if the caller
  // reference escapes its host-callback frame and is used later, the method
  // throws IllegalStateException instead of dereferencing a stale native pointer.
  //
  // Default methods throw UnsupportedOperationException so alternate Caller
  // implementations (Panama, mocks) opt in explicitly.
  // ==========================================================================

  /**
   * Compile a Module using this callback's store engine.
   *
   * <p>Module compilation is engine-scoped, not store-scoped — it does not mutate the caller's
   * store — but exposing it on {@code Caller} lets a host function build modules inside a callback
   * with the guarantee that they share the caller's Engine (and therefore can be instantiated into
   * the caller's store without cross-engine errors).
   *
   * @param wasmBytes WebAssembly binary bytes (or WAT text)
   * @return the compiled Module
   * @throws WasmException on compilation failure
   * @throws IllegalStateException if the callback has returned (use-after-return)
   * @since 1.6.0
   */
  default ai.tegmentum.wasmtime4j.Module compileModule(byte[] wasmBytes) throws WasmException {
    throw new UnsupportedOperationException(
        "compileModule not implemented on this Caller backend (JNI-only in wasmtime4j 1.6.0)");
  }

  /**
   * Grow a caller-visible {@link ai.tegmentum.wasmtime4j.WasmTable} by {@code delta} slots, filling
   * new slots with {@code init}.
   *
   * <p>For {@code funcref} tables {@code init} must be a {@link
   * ai.tegmentum.wasmtime4j.WasmFunction} or {@code null}. For {@code externref} / {@code anyref}
   * tables, r.2 only supports {@code null} (non-null externref writes deferred to a follow-up).
   *
   * @param table the caller-visible table to grow
   * @param delta the number of slots to add
   * @param init initial value for new slots (may be null)
   * @return the previous table size on success, -1 on failure
   * @throws WasmException on runtime error
   * @throws IllegalStateException if the callback has returned (use-after-return)
   * @since 1.6.0
   */
  default int growTable(
      final ai.tegmentum.wasmtime4j.WasmTable table, final int delta, final Object init)
      throws WasmException {
    throw new UnsupportedOperationException(
        "growTable not implemented on this Caller backend (JNI-only in wasmtime4j 1.6.0)");
  }

  /**
   * Set an element in a caller-visible {@link ai.tegmentum.wasmtime4j.WasmTable}.
   *
   * <p>For {@code funcref} tables {@code value} must be a {@link
   * ai.tegmentum.wasmtime4j.WasmFunction} or {@code null}.
   *
   * @param table the caller-visible table
   * @param index target slot index
   * @param value value to write (may be null)
   * @throws WasmException on runtime error
   * @throws IllegalStateException if the callback has returned (use-after-return)
   * @since 1.6.0
   */
  default void setTableElement(
      final ai.tegmentum.wasmtime4j.WasmTable table, final int index, final Object value)
      throws WasmException {
    throw new UnsupportedOperationException(
        "setTableElement not implemented on this Caller backend (JNI-only in wasmtime4j 1.6.0)");
  }

  /**
   * Grow a caller-visible {@link ai.tegmentum.wasmtime4j.WasmMemory} by {@code deltaPages} pages.
   *
   * <p>Only regular (non-shared) memories are supported by this scoped path; shared memories grow
   * atomically via {@link ai.tegmentum.wasmtime4j.WasmMemory#grow(long)} and don't need a caller
   * borrow.
   *
   * @param memory the caller-visible memory to grow
   * @param deltaPages the number of 64 KiB pages to add
   * @return the previous size in pages on success, -1 on failure
   * @throws WasmException on runtime error
   * @throws IllegalStateException if the callback has returned (use-after-return)
   * @since 1.6.0
   */
  default long growMemory(final ai.tegmentum.wasmtime4j.WasmMemory memory, final long deltaPages)
      throws WasmException {
    throw new UnsupportedOperationException(
        "growMemory not implemented on this Caller backend (JNI-only in wasmtime4j 1.6.0)");
  }

  /**
   * Instantiate a pre-linked module using this callback's store, borrowed safely from the wasmtime
   * callback context.
   *
   * <p>The {@code InstancePre} must have been pre-instantiated against this callback's engine and
   * linker BEFORE the host-callback fires — inside the callback, only the final {@code
   * InstancePre::instantiate} step runs. This matches Rust wasmtime's discipline of pre-linking
   * outside the borrow scope so the reentrant step is minimal.
   *
   * <p>The JNI backend implements this via {@code InstancePreWrapper::instantiate_with_context},
   * which borrows the caller's live {@code StoreContextMut} instead of re-acquiring the {@code
   * Store} wrapper's reentrant lock — the lock-based path would deadlock from a callback frame.
   * Other backends that inherit this default still throw {@link UnsupportedOperationException}
   * pending their own scoped-instantiate implementation.
   *
   * @param pre pre-linked module to instantiate into the caller's store
   * @return the newly created Instance
   * @throws WasmException on runtime error
   * @throws IllegalStateException if the callback has returned (use-after-return)
   * @throws UnsupportedOperationException if the backend has not implemented scoped instantiate
   * @since 1.6.0
   */
  default ai.tegmentum.wasmtime4j.Instance instantiate(
      final ai.tegmentum.wasmtime4j.InstancePre pre) throws WasmException {
    throw new UnsupportedOperationException(
        "instantiate(InstancePre) not implemented on this Caller backend (JNI-only in wasmtime4j"
            + " 1.6.0)");
  }

  /**
   * Read a byte range from the caller's exported memory using the caller-scoped native path.
   *
   * <p>Safe inside a host-callback frame. Routes through wasmtime's {@code Caller::get_export(name)
   * .into_memory()} + {@code Memory::read(&mut ctx, offset, buf)} with the same generation-counter
   * guard as the other scoped methods. Preferable to {@code caller.getMemory(name).get().read(...)}
   * from inside a callback because the api-layer Memory adapter's native handle may not be
   * registered from the callback frame — see doctrine
   * {@code doctrine-wasmtime4j-callback-frame-must-route-all-ops-through-caller-scoped-entrypoints}.
   *
   * @param memoryName name of the caller's exported memory (usually "memory")
   * @param offset byte offset into memory
   * @param length number of bytes to read
   * @return the byte range
   * @throws WasmException on runtime error (memory not found, bounds violation)
   * @throws IllegalStateException if the callback has returned (use-after-return)
   * @throws IllegalArgumentException if memoryName is null, offset is negative, or length is
   *     negative
   * @since 1.7.0
   */
  default byte[] readMemory(final String memoryName, final long offset, final int length)
      throws WasmException {
    throw new UnsupportedOperationException(
        "readMemory not implemented on this Caller backend (JNI-only in wasmtime4j 1.7.0)");
  }

  /**
   * Write a byte array into the caller's exported memory using the caller-scoped native path.
   *
   * <p>Same safety guarantees as {@link #readMemory}.
   *
   * @param memoryName name of the caller's exported memory
   * @param offset byte offset into memory
   * @param bytes bytes to write
   * @throws WasmException on runtime error
   * @throws IllegalStateException if the callback has returned (use-after-return)
   * @throws IllegalArgumentException if memoryName is null, offset is negative, or bytes is null
   * @since 1.7.0
   */
  default void writeMemory(final String memoryName, final long offset, final byte[] bytes)
      throws WasmException {
    throw new UnsupportedOperationException(
        "writeMemory not implemented on this Caller backend (JNI-only in wasmtime4j 1.7.0)");
  }

  /**
   * Define a memory extern on a linker using this callback's live store context
   * (F-Wasmtime4j-Caller-Scoped-Instantiate-Extern-Imports r.1 2026-07-27).
   *
   * <p>Mirrors {@link Linker#defineMemory(ai.tegmentum.wasmtime4j.Store, String, String, WasmMemory)}
   * but uses the caller's borrowed {@code AsContextMut} instead of a {@link
   * ai.tegmentum.wasmtime4j.Store}, so a host callback can wire memory imports into a linker for a
   * nested {@code InstancePre} without acquiring the store lock (which would deadlock).
   *
   * @param linker the linker to define the memory on
   * @param moduleName import module name (e.g. "env")
   * @param name import field name (e.g. "memory")
   * @param memory the memory extern to bind
   * @throws WasmException on runtime error
   * @throws IllegalStateException if the callback has returned (use-after-return)
   * @throws UnsupportedOperationException if the backend has not implemented scoped
   *     linker-define-memory
   * @since 1.5.2
   */
  default void linkerDefineMemory(
      final Linker<?> linker, final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    throw new UnsupportedOperationException(
        "linkerDefineMemory not implemented on this Caller backend"
            + " (JNI-only in wasmtime4j 1.5.2)");
  }

  /**
   * Define a table extern on a linker using this callback's live store context.
   * See {@link #linkerDefineMemory} for scoped-context rationale.
   *
   * @since 1.5.2
   */
  default void linkerDefineTable(
      final Linker<?> linker, final String moduleName, final String name, final WasmTable table)
      throws WasmException {
    throw new UnsupportedOperationException(
        "linkerDefineTable not implemented on this Caller backend"
            + " (JNI-only in wasmtime4j 1.5.2)");
  }

  /**
   * Define a global extern on a linker using this callback's live store context.
   * See {@link #linkerDefineMemory} for scoped-context rationale.
   *
   * @since 1.5.2
   */
  default void linkerDefineGlobal(
      final Linker<?> linker, final String moduleName, final String name, final WasmGlobal global)
      throws WasmException {
    throw new UnsupportedOperationException(
        "linkerDefineGlobal not implemented on this Caller backend"
            + " (JNI-only in wasmtime4j 1.5.2)");
  }

  /**
   * Define a memory extern on a linker by looking it up on the caller by export name
   * (F-Wasmtime4j-Caller-Scoped-Instantiate-Extern-Imports r.1 addendum, 2026-07-27).
   *
   * <p>Preferable to {@link #linkerDefineMemory} when the source is the caller's own
   * export — the api-layer WasmMemory handle path fails because caller-scoped memory
   * handles from {@code Caller.getMemory} are not registered in the outer memory
   * registry. This variant uses {@code caller.get_export(callerExportName).into_memory()}
   * on the native side and bypasses the registry entirely.
   *
   * @param linker the linker to define the memory on
   * @param moduleName import module name (e.g. "env")
   * @param name import field name (e.g. "memory")
   * @param callerExportName the caller's memory export name to look up (e.g. "memory")
   * @throws WasmException on runtime error (export not found, wrong extern kind)
   * @since 1.5.2
   */
  default void linkerDefineMemoryFromExport(
      final Linker<?> linker,
      final String moduleName,
      final String name,
      final String callerExportName)
      throws WasmException {
    throw new UnsupportedOperationException(
        "linkerDefineMemoryFromExport not implemented on this Caller backend"
            + " (JNI-only in wasmtime4j 1.5.2)");
  }

  /**
   * Define a table extern on a linker by looking it up on the caller by export name.
   * See {@link #linkerDefineMemoryFromExport} for rationale.
   *
   * @since 1.5.2
   */
  default void linkerDefineTableFromExport(
      final Linker<?> linker,
      final String moduleName,
      final String name,
      final String callerExportName)
      throws WasmException {
    throw new UnsupportedOperationException(
        "linkerDefineTableFromExport not implemented on this Caller backend"
            + " (JNI-only in wasmtime4j 1.5.2)");
  }
}
