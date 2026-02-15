package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.config.ResourceLimiter;
import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.debug.WasmBacktrace;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallHook;
import ai.tegmentum.wasmtime4j.func.CallHookHandler;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
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
   * Gets the remaining fuel amount.
   *
   * @return the remaining fuel, or -1 if fuel consumption is disabled
   */
  long getRemainingFuel() throws WasmException;

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

  // ===== Async Creation Methods =====

  /**
   * Creates a new WebAssembly table asynchronously with the specified element type and size.
   *
   * <p>This method performs table creation in an async context, allowing the operation to yield if
   * the store is configured with async resource limiting. This is useful when table creation needs
   * to check external quotas or resources asynchronously.
   *
   * <p><b>Note:</b> The async feature must be enabled in the engine configuration.
   *
   * @param elementType the type of elements this table will store (FUNCREF or EXTERNREF)
   * @param initialSize the initial number of elements
   * @param maxSize the maximum number of elements, or -1 for unlimited
   * @return a future that completes with a new WasmTable
   * @throws IllegalArgumentException if any parameter is invalid
   * @since 1.0.0
   */
  CompletableFuture<WasmTable> createTableAsync(
      WasmValueType elementType, int initialSize, int maxSize);

  /**
   * Creates a new WebAssembly linear memory asynchronously with the specified size.
   *
   * <p>This method performs memory creation in an async context, allowing the operation to yield if
   * the store is configured with async resource limiting. This is useful when memory allocation
   * needs to check external quotas or resources asynchronously.
   *
   * <p><b>Note:</b> The async feature must be enabled in the engine configuration.
   *
   * @param initialPages the initial number of 64KB pages
   * @param maxPages the maximum number of pages, or -1 for unlimited
   * @return a future that completes with a new WasmMemory
   * @throws IllegalArgumentException if initialPages or maxPages is invalid
   * @since 1.0.0
   */
  CompletableFuture<WasmMemory> createMemoryAsync(int initialPages, int maxPages);

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
   * Checks if the store is still valid and usable.
   *
   * @return true if the store is valid, false otherwise
   */
  boolean isValid();

  /**
   * Gets the total amount of fuel consumed by this store since creation.
   *
   * <p>This method provides historical fuel consumption data for monitoring and profiling
   * WebAssembly execution patterns.
   *
   * @return the total fuel consumed since store creation
   * @throws WasmException if fuel tracking is not enabled
   * @since 1.0.0
   */
  long getTotalFuelConsumed() throws WasmException;

  /**
   * Gets the number of function executions performed in this store.
   *
   * <p>This counter includes all function calls made through this store, both host functions and
   * WebAssembly functions.
   *
   * @return the total number of function executions
   * @since 1.0.0
   */
  long getExecutionCount();

  /**
   * Gets the total execution time in microseconds for this store.
   *
   * <p>This includes time spent in both WebAssembly execution and host function calls made through
   * this store.
   *
   * @return the total execution time in microseconds
   * @since 1.0.0
   */
  long getTotalExecutionTimeMicros();

  /**
   * Captures a WebAssembly backtrace from this store.
   *
   * <p>This method captures the current call stack if backtrace capture is enabled in the engine
   * configuration. If backtrace capture is disabled, this may return an empty backtrace.
   *
   * @return a WasmBacktrace containing the current call stack
   * @since 1.0.0
   */
  WasmBacktrace captureBacktrace();

  /**
   * Forces capture of a WebAssembly backtrace from this store.
   *
   * <p>This method always captures the call stack, even if backtrace capture is disabled in the
   * engine configuration. Force-captured backtraces may have performance implications.
   *
   * @return a WasmBacktrace containing the current call stack
   * @since 1.0.0
   */
  WasmBacktrace forceCaptureBacktrace();

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
   * Performs asynchronous garbage collection of externrefs in this store.
   *
   * <p>This method is similar to {@link #gc()} but designed for use in async contexts. It performs
   * cooperative, async-yielding during GC if configured with epoch-based interruption.
   *
   * <p><b>Note:</b> Requires both the GC feature and async feature to be enabled. The store must
   * also be configured with async support via epoch interruption.
   *
   * @return a CompletableFuture that completes when GC is finished
   * @throws WasmException if the async GC operation fails
   * @since 1.0.0
   */
  java.util.concurrent.CompletableFuture<Void> gcAsync() throws WasmException;

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
    private final long deltaTicks;

    private EpochDeadlineAction(final boolean shouldContinue, final long deltaTicks) {
      this.shouldContinue = shouldContinue;
      this.deltaTicks = deltaTicks;
    }

    /**
     * Creates an action to continue execution with a new deadline.
     *
     * @param deltaTicks the ticks to add for the new deadline
     * @return the continue action
     */
    public static EpochDeadlineAction continueWith(final long deltaTicks) {
      return new EpochDeadlineAction(true, deltaTicks);
    }

    /**
     * Creates an action to trap execution.
     *
     * @return the trap action
     */
    public static EpochDeadlineAction trap() {
      return new EpochDeadlineAction(false, 0);
    }

    /**
     * Returns whether execution should continue.
     *
     * @return true to continue, false to trap
     */
    public boolean shouldContinue() {
      return shouldContinue;
    }

    /**
     * Gets the delta ticks for the new deadline.
     *
     * @return the delta ticks (only valid if shouldContinue is true)
     */
    public long getDeltaTicks() {
      return deltaTicks;
    }
  }

  // ===== Exception Handling Methods =====

  /**
   * Throws an exception reference within the WebAssembly context.
   *
   * <p>This method sets the exception as pending and propagates it through host calls. The method
   * never returns normally - it always throws a WasmException wrapping the thrown exception.
   *
   * <p><b>Note:</b> Exception handling must be enabled in the engine configuration for this method
   * to work.
   *
   * @param <R> the declared return type (never actually returned)
   * @param exceptionRef the exception reference to throw
   * @return never returns - always throws
   * @throws WasmException with the thrown exception information
   * @throws IllegalArgumentException if exceptionRef is null
   * @since 1.0.0
   */
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
    return engine.getRuntime()
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
}
