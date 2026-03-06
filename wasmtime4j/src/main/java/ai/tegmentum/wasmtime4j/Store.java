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
package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.config.ResourceLimiter;
import ai.tegmentum.wasmtime4j.config.ResourceLimiterAsync;
import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallHook;
import ai.tegmentum.wasmtime4j.func.CallHookHandler;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a WebAssembly store.
 *
 * <p>A Store is an execution context that holds the runtime state for WebAssembly instances. Each
 * store maintains isolated linear memory, globals, and execution state. Objects like instances,
 * functions, and memories are tied to a specific store.
 *
 * <p>Stores are not thread-safe and should not be shared between threads without external
 * synchronization.
 *
 * @since 1.0.0
 */
public interface Store extends Closeable {

  /**
   * Gets the engine associated with this store.
   *
   * @return the Engine that created this store
   */
  Engine getEngine();

  /**
   * Gets custom data associated with this store, if any.
   *
   * @return the custom data, or null if none was set
   */
  Object getData();

  /**
   * Sets custom data to be associated with this store.
   *
   * @param data the custom data to associate
   */
  void setData(final Object data);

  /**
   * Sets the fuel amount available for WebAssembly execution.
   *
   * <p>Fuel is consumed during execution and can be used to limit the amount of computation
   * performed by WebAssembly code.
   *
   * @param fuel the amount of fuel to set
   * @throws IllegalArgumentException if fuel is negative
   */
  void setFuel(final long fuel) throws WasmException;

  /**
   * Gets the remaining fuel amount.
   *
   * @return the remaining fuel, or -1 if fuel consumption is disabled
   */
  long getFuel() throws WasmException;

  /**
   * Adds fuel to the store.
   *
   * @param fuel the amount of fuel to add
   * @throws IllegalArgumentException if fuel is negative
   */
  void addFuel(final long fuel) throws WasmException;

  /**
   * Sets the epoch deadline for WebAssembly execution.
   *
   * <p>Epochs provide a way to interrupt long-running WebAssembly code at regular intervals.
   *
   * @param ticks the number of epoch ticks before interruption
   */
  void setEpochDeadline(final long ticks) throws WasmException;

  /**
   * Consumes a specific amount of fuel from the store.
   *
   * <p>This method subtracts the specified amount of fuel from the store's fuel counter. If the
   * store doesn't have enough fuel, a WasmException is thrown.
   *
   * @param fuel the amount of fuel to consume
   * @return the remaining fuel after consumption
   * @throws WasmException if fuel consumption fails or insufficient fuel available
   * @throws IllegalArgumentException if fuel is negative
   */
  long consumeFuel(final long fuel) throws WasmException;

  /**
   * Gets the hostcall fuel limit for this store.
   *
   * <p>Hostcall fuel limits the amount of data a guest component may transfer to the host in a
   * single call, serving as a denial-of-service mitigation mechanism. The default is 128 MiB. This
   * fuel is reset for each host call and does not limit host-to-guest data transfers.
   *
   * @return the current hostcall fuel limit
   * @throws WasmException if retrieval fails
   * @since 1.1.0
   */
  long hostcallFuel() throws WasmException;

  /**
   * Sets the hostcall fuel limit for this store.
   *
   * <p>Configures the fuel limit for data transfers during guest-to-host component calls. The fuel
   * value roughly corresponds to the maximum number of bytes a guest may transfer to the host in a
   * single call. The default is 128 MiB. This fuel is reset for each host call and does not limit
   * host-to-guest data transfers.
   *
   * @param fuel the hostcall fuel limit to set
   * @throws WasmException if setting the fuel fails
   * @throws IllegalArgumentException if fuel is negative
   * @since 1.1.0
   */
  void setHostcallFuel(final long fuel) throws WasmException;

  /**
   * Creates a host function that can be imported by WebAssembly modules.
   *
   * <p>The created function will be bound to this store and can be added to import maps for module
   * instantiation. The function type must accurately describe the parameter and return types.
   *
   * @param name the name of the function (for debugging/logging purposes)
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @return a WasmFunction that can be used in import maps
   * @throws WasmException if host function creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  WasmFunction createHostFunction(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException;

  /**
   * Creates an unchecked host function that skips per-call type validation.
   *
   * <p>This is identical to {@link #createHostFunction} but uses {@code Func::new_unchecked}
   * internally, which skips Wasmtime's per-call parameter and return type validation for better
   * performance. The caller is responsible for ensuring that the function is always called with
   * arguments matching the declared function type.
   *
   * <p><b>Warning:</b> Using this method with mismatched types is undefined behavior and may cause
   * crashes or data corruption. Only use this when performance is critical and type correctness is
   * guaranteed by the caller.
   *
   * @param name the name of the function (for debugging/logging purposes)
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @return a WasmFunction that can be used in import maps
   * @throws WasmException if host function creation fails
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.1.0
   */
  default WasmFunction createHostFunctionUnchecked(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException {
    return createHostFunction(name, functionType, implementation);
  }

  /**
   * Creates a new WebAssembly global variable with the specified type and initial value.
   *
   * <p>Global variables can store values that persist across function calls and can be either
   * mutable or immutable. The created global will be bound to this store.
   *
   * @param valueType the type of values this global will store
   * @param isMutable true if the global should be mutable, false for immutable
   * @param initialValue the initial value to store in the global
   * @return a new WasmGlobal that can be used in import maps or accessed directly
   * @throws WasmException if global creation fails
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  WasmGlobal createGlobal(
      final WasmValueType valueType, final boolean isMutable, final WasmValue initialValue)
      throws WasmException;

  /**
   * Creates a new immutable WebAssembly global variable with the specified type and initial value.
   *
   * <p>This is a convenience method equivalent to calling {@link #createGlobal(WasmValueType,
   * boolean, WasmValue)} with {@code isMutable} set to {@code false}.
   *
   * @param valueType the type of values this global will store
   * @param initialValue the initial value to store in the global
   * @return a new immutable WasmGlobal
   * @throws WasmException if global creation fails
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  default WasmGlobal createImmutableGlobal(
      final WasmValueType valueType, final WasmValue initialValue) throws WasmException {
    return createGlobal(valueType, false, initialValue);
  }

  /**
   * Creates a new mutable WebAssembly global variable with the specified type and initial value.
   *
   * <p>This is a convenience method equivalent to calling {@link #createGlobal(WasmValueType,
   * boolean, WasmValue)} with {@code isMutable} set to {@code true}.
   *
   * @param valueType the type of values this global will store
   * @param initialValue the initial value to store in the global
   * @return a new mutable WasmGlobal
   * @throws WasmException if global creation fails
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  default WasmGlobal createMutableGlobal(
      final WasmValueType valueType, final WasmValue initialValue) throws WasmException {
    return createGlobal(valueType, true, initialValue);
  }

  /**
   * Creates a new WebAssembly table with the specified element type and size.
   *
   * <p>Tables are resizable arrays of references (like function references) that can be accessed by
   * WebAssembly code. They provide a way to implement indirect function calls and other dynamic
   * behavior.
   *
   * @param elementType the type of elements this table will store (FUNCREF or EXTERNREF)
   * @param initialSize the initial number of elements
   * @param maxSize the maximum number of elements, or -1 for unlimited
   * @return a new WasmTable that can be used in import maps or accessed directly
   * @throws WasmException if table creation fails
   * @throws IllegalArgumentException if any parameter is invalid
   * @since 1.0.0
   */
  WasmTable createTable(WasmValueType elementType, int initialSize, int maxSize)
      throws WasmException;

  /**
   * Creates a new WebAssembly table with the specified element type, size, and initial value.
   *
   * <p>All table slots are initialized to the specified value instead of the default null
   * reference. This is useful when creating tables that should be pre-populated with a specific
   * function reference or other reference value.
   *
   * @param elementType the type of elements this table will store (FUNCREF or EXTERNREF)
   * @param initialSize the initial number of elements
   * @param maxSize the maximum number of elements, or -1 for unlimited
   * @param initValue the initial value for all table slots
   * @return a new WasmTable that can be used in import maps or accessed directly
   * @throws WasmException if table creation fails
   * @throws IllegalArgumentException if any parameter is invalid or initValue is null
   * @since 1.1.0
   */
  WasmTable createTable(
      WasmValueType elementType, int initialSize, int maxSize, WasmValue initValue)
      throws WasmException;

  /**
   * Creates a new WebAssembly table from a table type descriptor.
   *
   * <p>This method creates a table with full type information, supporting features like 64-bit
   * indices (Memory64 proposal) in a single call. The table type encodes the element type, minimum
   * and maximum sizes, and whether the table uses 64-bit indexing.
   *
   * @param tableType the table type descriptor specifying all table attributes
   * @return a new WasmTable matching the specified type
   * @throws WasmException if table creation fails
   * @throws IllegalArgumentException if tableType is null
   * @since 1.1.0
   */
  WasmTable createTable(TableType tableType) throws WasmException;

  /**
   * Creates a new WebAssembly table from a table type descriptor with an initial value.
   *
   * <p>All table slots are initialized to the specified value instead of the default null
   * reference.
   *
   * @param tableType the table type descriptor specifying all table attributes
   * @param initValue the initial value for all table slots
   * @return a new WasmTable matching the specified type
   * @throws WasmException if table creation fails
   * @throws IllegalArgumentException if tableType or initValue is null
   * @since 1.1.0
   */
  WasmTable createTable(TableType tableType, WasmValue initValue) throws WasmException;

  /**
   * Creates a new WebAssembly linear memory with the specified size.
   *
   * <p>Linear memory is a contiguous, mutable array of raw bytes that can be read and written by
   * WebAssembly code. Memory is measured in pages, where each page is 64 KiB (65,536 bytes).
   *
   * @param initialPages the initial number of 64KB pages
   * @param maxPages the maximum number of pages, or -1 for unlimited
   * @return a new WasmMemory that can be used in import maps or accessed directly
   * @throws WasmException if memory creation fails
   * @throws IllegalArgumentException if initialPages or maxPages is invalid
   * @since 1.0.0
   */
  WasmMemory createMemory(int initialPages, int maxPages) throws WasmException;

  /**
   * Creates a new WebAssembly linear memory from a memory type descriptor.
   *
   * <p>This method creates a memory with full type information, supporting features like 64-bit
   * addressing and shared memory in a single call. The memory type encodes minimum and maximum page
   * counts, whether the memory is shared, and whether it uses 64-bit addressing.
   *
   * @param memoryType the memory type descriptor specifying all memory attributes
   * @return a new WasmMemory matching the specified type
   * @throws WasmException if memory creation fails
   * @throws IllegalArgumentException if memoryType is null
   * @since 1.1.0
   */
  WasmMemory createMemory(MemoryType memoryType) throws WasmException;

  /**
   * Creates a new shared WebAssembly linear memory for thread communication.
   *
   * <p>Shared memory can be accessed atomically by multiple threads, enabling concurrent
   * WebAssembly execution. This is essential for the WebAssembly threads proposal.
   *
   * <p>Shared memory provides:
   *
   * <ul>
   *   <li>Atomic load/store operations for thread-safe access
   *   <li>Compare-and-swap operations for lock-free synchronization
   *   <li>Wait/notify primitives for thread coordination
   * </ul>
   *
   * <p>Note: The engine must be configured with threads support enabled.
   *
   * @param initialPages the initial number of 64KB pages
   * @param maxPages the maximum number of pages (required for shared memory)
   * @return a new shared WasmMemory that can be accessed atomically
   * @throws WasmException if shared memory creation fails
   * @throws IllegalArgumentException if initialPages or maxPages is invalid
   * @throws UnsupportedOperationException if threads support is not enabled
   * @since 1.0.0
   */
  WasmMemory createSharedMemory(int initialPages, int maxPages) throws WasmException;

  /**
   * Creates a function reference from a host function.
   *
   * <p>Function references enable dynamic function dispatch and callbacks in WebAssembly programs.
   * They can be passed as parameters, stored in tables, and called indirectly.
   *
   * @param implementation the Java implementation of the function
   * @param functionType the WebAssembly function type signature
   * @return a FunctionReference that can be used as a WebAssembly value
   * @throws WasmException if function reference creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  FunctionReference createFunctionReference(
      final HostFunction implementation, final FunctionType functionType) throws WasmException;

  /**
   * Creates a function reference from an existing WebAssembly function.
   *
   * @param function the WebAssembly function to create a reference for
   * @return a FunctionReference for the given function
   * @throws WasmException if function reference creation fails
   * @throws IllegalArgumentException if function is null
   */
  FunctionReference createFunctionReference(final WasmFunction function) throws WasmException;

  /**
   * Gets the callback registry for this store.
   *
   * <p>The callback registry provides centralized management of callbacks and asynchronous
   * operations between Java and WebAssembly.
   *
   * @return the callback registry
   */
  CallbackRegistry getCallbackRegistry();

  /**
   * Creates an instance of a WebAssembly module in this store.
   *
   * <p>This method instantiates a compiled module within this store's execution context, making its
   * exported functions, memory, and globals available for use.
   *
   * @param module the compiled module to instantiate
   * @return a new Instance of the module
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if module is null
   */
  Instance createInstance(final Module module) throws WasmException;

  /**
   * Creates an instance of a WebAssembly module in this store with explicit imports.
   *
   * <p>The imports must be provided in the same order as the module's import declarations. Each
   * element in the array corresponds to one import, matching the module's import list by position.
   *
   * @param module the compiled module to instantiate
   * @param imports the array of extern values to satisfy the module's imports, in order
   * @return a new Instance of the module
   * @throws WasmException if instantiation fails or imports don't match
   * @throws IllegalArgumentException if module or imports is null
   */
  Instance createInstance(final Module module, final Extern[] imports) throws WasmException;

  /**
   * Checks if the store is still valid and usable.
   *
   * @return true if the store is valid, false otherwise
   */
  boolean isValid();

  /**
   * Triggers garbage collection for this store.
   *
   * <p>Garbage collection is performed automatically according to internal heuristics, but this
   * method allows explicit triggering when needed. This is useful when:
   *
   * <ul>
   *   <li>Memory pressure is known and immediate reclamation is desired
   *   <li>Deterministic collection timing is required for testing
   *   <li>Preparing for a batch of allocations
   * </ul>
   *
   * <p><b>Note:</b> GC support must be enabled in the engine configuration for this method to have
   * any effect. If GC is disabled, this method is a no-op.
   *
   * @throws WasmException if the GC operation fails
   * @since 1.0.0
   */
  void gc() throws WasmException;

  /**
   * Performs garbage collection asynchronously.
   *
   * <p>This is the async variant of {@link #gc()} that cooperatively yields during collection if
   * async fuel yielding or epoch-based yielding is configured. This allows other async tasks to
   * make progress during potentially long GC operations.
   *
   * <p>Requires the store to have async support enabled (via {@code Config.asyncSupport(true)}).
   *
   * @return a future that completes when GC is finished
   * @throws UnsupportedOperationException if async support is not enabled
   * @since 1.1.0
   */
  default CompletableFuture<Void> gcAsync() {
    // Default implementation: delegate to sync gc on ForkJoinPool
    // Implementations should override to use real native async
    return CompletableFuture.runAsync(
        () -> {
          try {
            gc();
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Asynchronously creates an instance of a WebAssembly module in this store.
   *
   * <p>This is the async variant of {@link #createInstance(Module)}. The default implementation
   * delegates to the synchronous method on the ForkJoinPool. Implementations may override to use
   * native async instantiation.
   *
   * @param module the compiled module to instantiate
   * @return a future that completes with a new Instance
   * @throws IllegalArgumentException if module is null
   * @since 1.1.0
   */
  default CompletableFuture<Instance> createInstanceAsync(final Module module) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return createInstance(module);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Asynchronously creates a new WebAssembly linear memory from a memory type descriptor.
   *
   * <p>This is the async variant of {@link #createMemory(MemoryType)}. The default implementation
   * delegates to the synchronous method on the ForkJoinPool.
   *
   * @param memoryType the memory type descriptor specifying all memory attributes
   * @return a future that completes with a new WasmMemory
   * @throws IllegalArgumentException if memoryType is null
   * @since 1.1.0
   */
  default CompletableFuture<WasmMemory> createMemoryAsync(final MemoryType memoryType) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return createMemory(memoryType);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Asynchronously creates a new WebAssembly table from a table type descriptor.
   *
   * <p>This is the async variant of {@link #createTable(TableType)}. The default implementation
   * delegates to the synchronous method on the ForkJoinPool.
   *
   * @param tableType the table type descriptor specifying all table attributes
   * @return a future that completes with a new WasmTable
   * @throws IllegalArgumentException if tableType is null
   * @since 1.1.0
   */
  default CompletableFuture<WasmTable> createTableAsync(final TableType tableType) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return createTable(tableType);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Configures epoch-deadline expiration to yield to the async caller and update the deadline.
   *
   * <p>When epoch-interruption-instrumented code is executed on this store and the epoch deadline
   * is reached before completion, with the store configured in this way, execution will yield (the
   * future will return Pending but re-awake itself for later execution) and, upon resuming, the
   * store will be configured with an epoch deadline equal to the current epoch plus delta ticks.
   *
   * <p>This setting is intended to allow for cooperative timeslicing of multiple CPU-bound Wasm
   * guests in different stores, all executing under the control of an async executor. Stores should
   * be configured to "yield and update" automatically with this method, and some external driver (a
   * thread that wakes up periodically, or a timer signal/interrupt) should call {@link
   * Engine#incrementEpoch()}.
   *
   * <p><b>Note:</b> Epoch interruption must be enabled in the engine configuration.
   *
   * @param deltaTicks the number of ticks to add to the current epoch for the new deadline
   * @throws WasmException if configuration fails
   * @throws IllegalArgumentException if deltaTicks is negative
   * @since 1.0.0
   */
  void epochDeadlineAsyncYieldAndUpdate(long deltaTicks) throws WasmException;

  /**
   * Sets the epoch deadline to trap when reached.
   *
   * <p>When the epoch deadline is reached, execution will trap immediately rather than yielding.
   *
   * <p><b>Note:</b> Epoch interruption must be enabled in the engine configuration.
   *
   * @throws WasmException if configuration fails
   * @since 1.0.0
   */
  void epochDeadlineTrap() throws WasmException;

  /**
   * Sets a callback to be invoked when the epoch deadline is reached.
   *
   * <p>The callback receives the current epoch and returns the action to take.
   *
   * <p><b>Note:</b> Epoch interruption must be enabled in the engine configuration.
   *
   * @param callback the callback to invoke, or null to clear any existing callback
   * @throws WasmException if configuration fails
   * @since 1.0.0
   */
  void epochDeadlineCallback(EpochDeadlineCallback callback) throws WasmException;

  /**
   * Callback interface for epoch deadline handling.
   *
   * @since 1.0.0
   */
  @FunctionalInterface
  interface EpochDeadlineCallback {
    /**
     * Called when the epoch deadline is reached.
     *
     * @param currentEpoch the current epoch value
     * @return the action to take (continue with new delta, or trap)
     */
    EpochDeadlineAction onEpochDeadline(long currentEpoch);
  }

  /**
   * Action to take when epoch deadline is reached.
   *
   * @since 1.0.0
   */
  final class EpochDeadlineAction {
    private final boolean shouldContinue;
    private final boolean shouldYield;
    private final long deltaTicks;

    private EpochDeadlineAction(
        final boolean shouldContinue, final boolean shouldYield, final long deltaTicks) {
      this.shouldContinue = shouldContinue;
      this.shouldYield = shouldYield;
      this.deltaTicks = deltaTicks;
    }

    /**
     * Creates an action to continue execution with a new deadline.
     *
     * @param deltaTicks the ticks to add for the new deadline
     * @return the continue action
     */
    public static EpochDeadlineAction continueWith(final long deltaTicks) {
      return new EpochDeadlineAction(true, false, deltaTicks);
    }

    /**
     * Creates an action to trap execution.
     *
     * @return the trap action
     */
    public static EpochDeadlineAction trap() {
      return new EpochDeadlineAction(false, false, 0);
    }

    /**
     * Creates an action to yield execution and resume later with a new deadline.
     *
     * <p>This is used with async-enabled stores to cooperatively yield back to the async executor
     * when the epoch deadline is reached, then resume with the specified delta.
     *
     * @param deltaTicks the ticks to add for the new deadline after resuming
     * @return the yield action
     */
    public static EpochDeadlineAction yield(final long deltaTicks) {
      return new EpochDeadlineAction(false, true, deltaTicks);
    }

    /**
     * Returns whether execution should continue.
     *
     * @return true to continue, false to trap or yield
     */
    public boolean shouldContinue() {
      return shouldContinue;
    }

    /**
     * Returns whether execution should yield.
     *
     * @return true to yield, false to continue or trap
     */
    public boolean shouldYield() {
      return shouldYield;
    }

    /**
     * Gets the delta ticks for the new deadline.
     *
     * @return the delta ticks (only valid if shouldContinue or shouldYield is true)
     */
    public long getDeltaTicks() {
      return deltaTicks;
    }
  }

  // ===== Exception Handling Methods =====

  /**
   * Throws an exception reference within the WebAssembly context.
   *
   * <p><b>Note:</b> Wasmtime does not support host-initiated exception throwing. Exceptions can
   * only originate from WebAssembly {@code throw} or {@code throw_ref} instructions during
   * execution. Use {@link #takePendingException()} to retrieve exceptions thrown during WASM
   * execution.
   *
   * @param <R> the declared return type (never actually returned)
   * @param exceptionRef the exception reference to throw
   * @return never returns - always throws
   * @throws UnsupportedOperationException always, as host-initiated exception throwing is not
   *     supported by Wasmtime
   * @throws IllegalArgumentException if exceptionRef is null
   * @since 1.0.0
   * @deprecated Wasmtime does not support host-side exception throwing. Exceptions propagate only
   *     from WASM {@code throw}/{@code throw_ref} instructions. Use {@link #takePendingException()}
   *     instead.
   */
  @Deprecated
  <R> R throwException(ExnRef exceptionRef) throws WasmException;

  /**
   * Takes and removes the currently pending exception from the store.
   *
   * <p>If an exception was thrown during WebAssembly execution and has not been caught, this method
   * retrieves and clears it from the store.
   *
   * @return the pending exception, or null if none exists
   * @throws WasmException if retrieval fails
   * @since 1.0.0
   */
  ExnRef takePendingException() throws WasmException;

  /**
   * Tests whether a pending exception currently exists on the store.
   *
   * <p>This method allows checking for pending exceptions without consuming them.
   *
   * @return true if there is a pending exception
   * @since 1.0.0
   */
  boolean hasPendingException();

  // ===== Call Hook Methods =====

  /**
   * Sets a call hook handler that is invoked on every transition between WebAssembly and host code.
   *
   * <p>The handler receives a {@link CallHook} indicating the type of transition:
   *
   * <ul>
   *   <li>{@link CallHook#CALLING_WASM} - Host is calling into WebAssembly
   *   <li>{@link CallHook#RETURNING_FROM_WASM} - Returning from WebAssembly to host
   *   <li>{@link CallHook#CALLING_HOST} - WebAssembly is calling a host function
   *   <li>{@link CallHook#RETURNING_FROM_HOST} - Returning from host function to WebAssembly
   * </ul>
   *
   * <p><b>Performance note:</b> Call hooks incur a small overhead on all entries/exits from
   * WebAssembly. Only enable when necessary.
   *
   * <p><b>Note:</b> The call-hook feature must be enabled in the engine configuration for this to
   * work.
   *
   * @param handler the call hook handler, or null to disable call hooks
   * @throws WasmException if setting the hook fails
   * @since 1.0.0
   */
  void setCallHook(CallHookHandler handler) throws WasmException;

  /**
   * Sets an async call hook handler for use with async execution.
   *
   * <p>This is the async equivalent of {@link #setCallHook(CallHookHandler)}. The handler may
   * perform async operations and returns a CompletableFuture.
   *
   * <p><b>Note:</b> The call-hook and async features must be enabled in the engine configuration.
   *
   * @param handler the async call hook handler, or null to disable
   * @throws WasmException if setting the hook fails
   * @since 1.0.0
   */
  void setCallHookAsync(AsyncCallHookHandler handler) throws WasmException;

  /**
   * Async handler for call hook events.
   *
   * @since 1.0.0
   */
  @FunctionalInterface
  interface AsyncCallHookHandler {
    /**
     * Handles a call hook event asynchronously.
     *
     * @param hook the type of call transition
     * @return a future that completes when the hook processing is done
     */
    java.util.concurrent.CompletableFuture<Void> onCallHook(CallHook hook);
  }

  // ===== Debug Methods =====

  /**
   * Captures a WebAssembly backtrace from the current execution state of this store.
   *
   * <p>This captures the current call stack, providing function names, module offsets, and debug
   * symbols when available. Backtrace capture must be enabled in the engine configuration.
   *
   * <p>If backtrace capture is disabled, this may return an empty backtrace. Use {@link
   * #forceCaptureBacktrace()} to capture even when disabled.
   *
   * @return the captured backtrace
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if capture fails
   * @since 1.1.0
   */
  default ai.tegmentum.wasmtime4j.debug.WasmBacktrace captureBacktrace()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    throw new UnsupportedOperationException("captureBacktrace not supported by this store");
  }

  /**
   * Force-captures a WebAssembly backtrace even if backtrace capture is disabled.
   *
   * <p>This method captures the current call stack regardless of the engine's backtrace
   * configuration. This is useful for debugging when backtrace capture is normally disabled for
   * performance reasons.
   *
   * @return the force-captured backtrace
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if capture fails
   * @since 1.1.0
   */
  default ai.tegmentum.wasmtime4j.debug.WasmBacktrace forceCaptureBacktrace()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    throw new UnsupportedOperationException("forceCaptureBacktrace not supported by this store");
  }

  // ===== Fuel Async Methods =====

  /**
   * Configures the interval at which async execution should yield based on fuel consumption.
   *
   * <p>When set to a positive value, WebAssembly execution will automatically yield to the async
   * executor after consuming the specified amount of fuel. This enables cooperative timeslicing
   * without requiring epoch-based interruption.
   *
   * <p>A value of 0 (default) disables fuel-based async yielding.
   *
   * <p><b>Note:</b> Both async support and fuel consumption must be enabled for this to work.
   *
   * @param interval the fuel units to consume before yielding (0 to disable)
   * @throws WasmException if configuration fails
   * @throws IllegalArgumentException if interval is negative
   * @since 1.0.0
   */
  void setFuelAsyncYieldInterval(long interval) throws WasmException;

  /**
   * Gets the currently configured fuel async yield interval.
   *
   * @return the fuel async yield interval (0 means disabled)
   * @since 1.0.0
   */
  long getFuelAsyncYieldInterval();

  /**
   * Sets a dynamic resource limiter for this store.
   *
   * <p>Unlike {@link StoreLimits}, which sets static limits at store creation time, a {@link
   * ResourceLimiter} provides dynamic, callback-based limiting. The Wasmtime runtime calls back
   * into the limiter each time a memory or table needs to grow, allowing per-request decisions.
   *
   * <p>Only one limiter can be active at a time. Setting a new limiter replaces any previously set
   * limiter. This method can be called at any time during the store's lifetime.
   *
   * @param limiter the resource limiter to set
   * @throws WasmException if setting the limiter fails
   * @throws IllegalArgumentException if limiter is null
   * @since 1.0.0
   */
  void setResourceLimiter(ResourceLimiter limiter) throws WasmException;

  /**
   * Sets an asynchronous resource limiter for this store.
   *
   * <p>This is the async counterpart to {@link #setResourceLimiter(ResourceLimiter)}. The limiter's
   * callbacks return {@link java.util.concurrent.CompletableFuture} to allow non-blocking
   * decisions.
   *
   * <p>Requires the engine to be configured with {@code asyncSupport(true)}. Only one limiter (sync
   * or async) can be active at a time. Setting a new limiter replaces any previously set limiter.
   *
   * @param limiter the async resource limiter to set
   * @throws WasmException if setting the limiter fails
   * @throws IllegalArgumentException if limiter is null
   * @since 1.1.0
   */
  void setResourceLimiterAsync(ResourceLimiterAsync limiter) throws WasmException;

  // ===== Debugging API =====

  /**
   * Gets the current stack frames for debugging.
   *
   * <p>Returns frame handles from innermost (current function) to outermost (root caller). Frame
   * handles provide access to function identity, program counter, locals, and stack values.
   *
   * <p>Requires the engine to be configured with {@code guestDebug(true)}.
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

  /**
   * Creates a breakpoint editor for batch-modifying breakpoints.
   *
   * <p>The returned editor allows adding/removing breakpoints and toggling single-step mode.
   * Changes are applied when {@link ai.tegmentum.wasmtime4j.debug.BreakpointEditor#apply()} is
   * called.
   *
   * <p>Requires the engine to be configured with {@code guestDebug(true)}.
   *
   * @return a breakpoint editor, or empty if guest debugging is not enabled
   * @since 1.1.0
   */
  default java.util.Optional<ai.tegmentum.wasmtime4j.debug.BreakpointEditor> editBreakpoints() {
    return java.util.Optional.empty();
  }

  /**
   * Gets all currently active breakpoints.
   *
   * <p>Requires the engine to be configured with {@code guestDebug(true)}.
   *
   * <p><b>Note:</b> Wasmtime only exposes a breakpoint count, not individual breakpoint details.
   * Implementations return the count via {@link
   * ai.tegmentum.wasmtime4j.debug.BreakpointEditor#apply()}, but this method returns empty because
   * individual breakpoint introspection is not available in the wasmtime C API.
   *
   * @return a list of active breakpoints, or empty if guest debugging is not enabled
   * @since 1.1.0
   */
  default java.util.Optional<java.util.List<ai.tegmentum.wasmtime4j.debug.Breakpoint>>
      breakpoints() {
    return java.util.Optional.empty();
  }

  /**
   * Checks if single-step mode is active.
   *
   * <p>When single-step mode is enabled, the debug handler is invoked before each WebAssembly
   * instruction is executed.
   *
   * @return true if single-step mode is active
   * @since 1.1.0
   */
  default boolean isSingleStep() {
    return false;
  }

  /**
   * Sets a debug handler for this store.
   *
   * <p>The handler will be invoked when debug events occur, such as breakpoints being hit, traps
   * occurring, or exceptions being thrown.
   *
   * <p>Requires the engine to be configured with both {@code guestDebug(true)} and {@code
   * asyncSupport(true)}.
   *
   * <p><b>Not yet implemented.</b> This method requires async debug handler callback infrastructure
   * (wasmtime's {@code DebugHandlerHook} trait with async support). The other debug APIs ({@link
   * #debugExitFrames()}, {@link #editBreakpoints()}, {@link #isSingleStep()}) are fully functional
   * and can be used without a debug handler.
   *
   * @param handler the debug handler to set
   * @throws UnsupportedOperationException always, until async debug handler plumbing is implemented
   * @throws NullPointerException if handler is null
   * @since 1.1.0
   */
  default void setDebugHandler(final ai.tegmentum.wasmtime4j.debug.DebugHandler handler) {
    throw new UnsupportedOperationException(
        "setDebugHandler requires guest debugging and async support to be enabled");
  }

  /**
   * Clears the current debug handler.
   *
   * <p>After clearing, no debug events will be delivered to any handler.
   *
   * @since 1.1.0
   */
  default void clearDebugHandler() {
    // No-op by default when no handler is set
  }

  // ===== Concurrency API =====

  /**
   * Runs a concurrent task on this store, returning a future for its result.
   *
   * <p>This is a Java-level concurrency utility, not wasmtime's native cooperative scheduling. The
   * task receives the full Store reference and runs on the {@link
   * java.util.concurrent.ForkJoinPool#commonPool()}. The caller must not use this store on any
   * other thread until the returned future completes, since stores are not thread-safe.
   *
   * <p>For native concurrent component calls, use {@link
   * ai.tegmentum.wasmtime4j.component.ComponentInstance#runConcurrent(java.util.List)}.
   *
   * @param <R> the result type of the task
   * @param task the concurrent task to execute
   * @return a future that completes with the task's result
   * @throws IllegalArgumentException if task is null
   * @since 1.1.0
   */
  default <R> CompletableFuture<R> runConcurrent(final ConcurrentTask<R> task) {
    if (task == null) {
      throw new IllegalArgumentException("task cannot be null");
    }
    final Store self = this;
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return task.execute(self);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Submits a concurrent task on this store, returning a handle to join its result.
   *
   * <p>This is a Java-level concurrency utility, not wasmtime's native {@code Store::spawn()} which
   * uses cooperative scheduling. The task receives the full Store reference (unlike wasmtime's
   * scoped accessor) and runs on the ForkJoinPool.
   *
   * <p>The caller must not use this store on any other thread until the task completes or is
   * joined, since stores are not thread-safe.
   *
   * <p>For native concurrent component calls, use {@link
   * ai.tegmentum.wasmtime4j.component.ComponentInstance#runConcurrent(java.util.List)}.
   *
   * @param <R> the result type of the task
   * @param task the concurrent task to submit
   * @return a JoinHandle for the submitted task
   * @throws IllegalArgumentException if task is null
   * @since 1.1.0
   */
  default <R> JoinHandle<R> submitTask(final ConcurrentTask<R> task) {
    if (task == null) {
      throw new IllegalArgumentException("task cannot be null");
    }
    final CompletableFuture<R> future = runConcurrent(task);
    return new DefaultJoinHandle<>(future);
  }

  /**
   * Spawns a concurrent task on this store, returning a handle to join its result.
   *
   * @param <R> the result type of the task
   * @param task the concurrent task to spawn
   * @return a JoinHandle for the spawned task
   * @throws IllegalArgumentException if task is null
   * @deprecated Use {@link #submitTask(ConcurrentTask)} instead. This method is a Java-level
   *     concurrency utility and does not correspond to wasmtime's native {@code Store::spawn()}.
   * @since 1.1.0
   */
  @Deprecated
  default <R> JoinHandle<R> spawn(final ConcurrentTask<R> task) {
    return submitTask(task);
  }

  /**
   * Checks whether this store has async support enabled.
   *
   * <p>Async-enabled stores are required for Wasmtime's async operations such as {@code
   * callAsync()}, {@code instantiateAsync()}, and async host functions. The async flag is inherited
   * from the Engine's {@code asyncSupport} configuration at store creation time.
   *
   * @return true if this store supports async operations
   * @since 1.1.0
   */
  default boolean isAsync() {
    return false;
  }

  /**
   * Closes the store and releases associated resources.
   *
   * <p>After closing, the store becomes invalid and should not be used. All instances and other
   * objects associated with this store may also become invalid.
   */
  @Override
  void close();

  /**
   * Creates a new Store for the given engine.
   *
   * @param engine the engine to create the store for
   * @return a new Store instance
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if engine is null
   */
  static Store create(final Engine engine) throws WasmException {
    return engine.getRuntime().createStore(engine);
  }

  /**
   * Creates a new Store with custom configuration.
   *
   * <p>This factory method allows creating a store with specific fuel limits, memory limits, and
   * execution timeouts configured at creation time.
   *
   * @param engine the engine to create the store for
   * @param fuelLimit the initial fuel limit (0 for unlimited)
   * @param memoryLimitBytes the memory limit in bytes (0 for unlimited)
   * @param executionTimeoutSeconds the execution timeout in seconds (0 for unlimited)
   * @return a new Store instance with the specified configuration
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if engine is null or limits are negative
   * @since 1.0.0
   */
  static Store create(
      final Engine engine,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSeconds)
      throws WasmException {
    return engine
        .getRuntime()
        .createStore(engine, fuelLimit, memoryLimitBytes, executionTimeoutSeconds);
  }

  /**
   * Creates a new Store with resource limits.
   *
   * <p>Resource limits are enforced during WebAssembly execution and control memory size, table
   * elements, and instance counts. Limits are applied per-resource (each memory/table can grow to
   * the limit independently).
   *
   * @param engine the engine to create the store for
   * @param limits the resource limits to apply
   * @return a new Store instance with the specified limits
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if engine or limits is null
   * @since 1.0.0
   */
  static Store create(final Engine engine, final StoreLimits limits) throws WasmException {
    if (limits == null) {
      throw new IllegalArgumentException("StoreLimits cannot be null");
    }
    return engine.getRuntime().createStore(engine, limits);
  }

  /**
   * Tries to create a new Store for the given engine, returning empty on allocation failure.
   *
   * <p>Unlike {@link #create(Engine)}, this method returns an empty Optional instead of throwing if
   * the store allocation fails (e.g., out of memory). This is useful in memory-constrained
   * environments where OOM should be handled gracefully rather than crashing.
   *
   * @param engine the engine to create the store for
   * @return an Optional containing the new Store, or empty if allocation failed
   * @throws IllegalArgumentException if engine is null
   * @since 1.1.0
   */
  static java.util.Optional<Store> tryCreate(final Engine engine) {
    try {
      return java.util.Optional.of(engine.getRuntime().tryCreateStore(engine));
    } catch (final WasmException e) {
      return java.util.Optional.empty();
    }
  }

  /**
   * Creates a new builder for configuring a Store.
   *
   * <p>The builder provides a fluent API for configuring store options including user data, fuel
   * limits, and epoch deadlines.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Store store = Store.builder(engine)
   *     .withData(myUserData)
   *     .withFuel(10000)
   *     .build();
   * }</pre>
   *
   * @param <T> the type of user data to associate with the store
   * @param engine the engine to create the store for
   * @return a new StoreBuilder for configuring the store
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  static <T> StoreBuilder<T> builder(final Engine engine) {
    return new StoreBuilder<>(engine);
  }

  /**
   * Gets the WASI context associated with this store, if any.
   *
   * <p>The WASI context is typically set during module instantiation via a linker. This method
   * returns the context that was applied to this store, allowing runtime inspection and mutation of
   * WASI configuration.
   *
   * <p>After modifying the returned context (e.g., adding environment variables), call {@link
   * #reapplyWasiContext()} to apply the changes to the running store.
   *
   * @return an Optional containing the WASI context, or empty if no WASI context is configured
   * @since 1.1.0
   */
  default java.util.Optional<ai.tegmentum.wasmtime4j.wasi.WasiContext> getWasiContext() {
    return java.util.Optional.empty();
  }

  /**
   * Re-applies the WASI context to this store, rebuilding the internal WASI state.
   *
   * <p>This method should be called after modifying the WASI context returned by {@link
   * #getWasiContext()} to ensure that changes (such as new environment variables or arguments) take
   * effect in the running store.
   *
   * @throws WasmException if re-applying the WASI context fails or no context is set
   * @since 1.1.0
   */
  default void reapplyWasiContext() throws WasmException {
    throw new WasmException("WASI context re-application not supported by this store");
  }
}
