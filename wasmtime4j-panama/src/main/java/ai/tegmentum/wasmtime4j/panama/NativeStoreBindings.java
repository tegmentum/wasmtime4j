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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Native function bindings for Store operations.
 *
 * <p>Provides type-safe wrappers for all Wasmtime store lifecycle, fuel management, epoch
 * deadlines, garbage collection, exception handling, call hooks, and resource limiter native
 * functions. Hot-path methods use eagerly initialized volatile {@link MethodHandle} fields for
 * {@code invokeExact} optimization.
 *
 * <p>This class follows the singleton pattern with initialization-on-demand holder.
 */
public final class NativeStoreBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeStoreBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeStoreBindings INSTANCE = new NativeStoreBindings();
  }

  // Hot-path volatile MethodHandle fields for invokeExact optimization
  // Signature: (ADDRESS) -> ADDRESS
  private volatile MethodHandle mhStoreCreate;

  // Pre-cached fuel/epoch MethodHandles to avoid ConcurrentHashMap lookups on hot paths
  private volatile MethodHandle mhStoreSetFuel;
  private volatile MethodHandle mhStoreGetFuel;
  private volatile MethodHandle mhStoreAddFuel;
  private volatile MethodHandle mhStoreConsumeFuel;
  private volatile MethodHandle mhStoreSetFuelAsyncYieldInterval;
  private volatile MethodHandle mhStoreGetHostcallFuel;
  private volatile MethodHandle mhStoreSetHostcallFuel;
  private volatile MethodHandle mhStoreTryCreate;
  private volatile MethodHandle mhStoreSetEpochDeadline;
  private volatile MethodHandle mhStoreGc;

  private NativeStoreBindings() {
    super();
    initializeBindings();
    initializeHotPathHandles();
    markInitialized();
    LOGGER.fine("Initialized NativeStoreBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeStoreBindings getInstance() {
    return Holder.INSTANCE;
  }

  /** Eagerly initializes hot-path MethodHandles after all bindings are registered. */
  private void initializeHotPathHandles() {
    this.mhStoreCreate = resolveHandle("wasmtime4j_store_create");
    this.mhStoreSetFuel = resolveHandle("wasmtime4j_panama_store_set_fuel");
    this.mhStoreGetFuel = resolveHandle("wasmtime4j_panama_store_get_fuel");
    this.mhStoreAddFuel = resolveHandle("wasmtime4j_panama_store_add_fuel");
    this.mhStoreConsumeFuel = resolveHandle("wasmtime4j_panama_store_consume_fuel");
    this.mhStoreSetFuelAsyncYieldInterval =
        resolveHandle("wasmtime4j_panama_store_set_fuel_async_yield_interval");
    this.mhStoreGetHostcallFuel = resolveHandle("wasmtime4j_panama_store_get_hostcall_fuel");
    this.mhStoreSetHostcallFuel = resolveHandle("wasmtime4j_panama_store_set_hostcall_fuel");
    this.mhStoreTryCreate = resolveHandle("wasmtime4j_panama_store_try_create");
    this.mhStoreSetEpochDeadline = resolveHandle("wasmtime4j_panama_store_set_epoch_deadline");
    this.mhStoreGc = resolveHandle("wasmtime4j_panama_store_gc");
  }

  /**
   * Resolves a MethodHandle from a registered function binding.
   *
   * @param functionName the native function name
   * @return the MethodHandle, or null if the binding is not available
   */
  private MethodHandle resolveHandle(final String functionName) {
    FunctionBinding binding = getFunctionBinding(functionName);
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  // ===== Binding Registrations =====

  private void initializeBindings() {
    addFunctionBinding(
        "wasmtime4j_store_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return store*
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding("wasmtime4j_store_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_store_new_for_module",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return store*
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_LONG, // fuel_limit
            ValueLayout.JAVA_LONG, // memory_limit_bytes
            ValueLayout.JAVA_LONG, // execution_timeout_secs
            ValueLayout.JAVA_INT, // max_instances
            ValueLayout.JAVA_INT, // max_table_elements
            ValueLayout.JAVA_INT, // max_tables
            ValueLayout.JAVA_INT, // max_memories
            ValueLayout.JAVA_INT, // trap_on_grow_failure
            ValueLayout.ADDRESS)); // store_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_store_set_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_panama_store_get_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // fuel_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_add_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_panama_store_consume_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // fuel
            ValueLayout.ADDRESS)); // remaining_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_fuel_async_yield_interval",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // interval

    addFunctionBinding(
        "wasmtime4j_panama_store_try_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // store_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_store_get_hostcall_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // fuel_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_hostcall_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_panama_store_set_epoch_deadline",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // ticks

    addFunctionBinding(
        "wasmtime4j_panama_store_epoch_deadline_trap",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_clear_epoch_deadline_callback",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_epoch_deadline_callback_fn",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // callback_fn (function pointer)
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_panama_store_set_resource_limiter",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // callback_id
            ValueLayout.ADDRESS, // memory_growing_fn
            ValueLayout.ADDRESS, // table_growing_fn
            ValueLayout.ADDRESS, // memory_grow_failed_fn (nullable)
            ValueLayout.ADDRESS)); // table_grow_failed_fn (nullable)

    addFunctionBinding(
        "wasmtime4j_panama_store_set_resource_limiter_async",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // callback_id
            ValueLayout.ADDRESS, // memory_growing_fn
            ValueLayout.ADDRESS, // table_growing_fn
            ValueLayout.ADDRESS, // memory_grow_failed_fn (nullable)
            ValueLayout.ADDRESS)); // table_grow_failed_fn (nullable)

    addFunctionBinding(
        "wasmtime4j_panama_store_get_execution_stats",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // execution_count_ptr
            ValueLayout.ADDRESS, // total_execution_time_us_ptr
            ValueLayout.ADDRESS)); // fuel_consumed_ptr

    addFunctionBinding(
        "wasmtime4j_store_set_wasi_context",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // wasi_ctx_ptr

    addFunctionBinding(
        "wasmtime4j_store_has_wasi_context",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    // Debugging API
    addFunctionBinding(
        "wasmtime4j_store_is_single_step",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_breakpoint_count",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_set_single_step",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT)); // enabled

    addFunctionBinding(
        "wasmtime4j_store_add_breakpoint",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.JAVA_INT)); // pc

    addFunctionBinding(
        "wasmtime4j_store_remove_breakpoint",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.JAVA_INT)); // pc

    addFunctionBinding(
        "wasmtime4j_store_is_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_debug_exit_frames",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 0=success, -1=not enabled, negative=error
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // out_data (i32 array buffer)
            ValueLayout.ADDRESS)); // out_count (pointer to i32 for frame count)

    addFunctionBinding(
        "wasmtime4j_panama_store_epoch_deadline_async_yield_and_update",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // delta ticks

    addFunctionBinding(
        "wasmtime4j_panama_store_gc",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_gc_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_capture_backtrace",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // buffer_out_ptr
            ValueLayout.ADDRESS)); // buffer_len_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_force_capture_backtrace",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // buffer_out_ptr
            ValueLayout.ADDRESS)); // buffer_len_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_throw_exception",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // exn_ref_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_take_pending_exception",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return exn_ref_ptr or NULL
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_has_pending_exception",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_call_hook",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_clear_call_hook",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_call_hook_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_clear_call_hook_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr
  }

  // ===========================================================================================
  // Store Operations
  // ===========================================================================================

  /**
   * Creates a new Wasmtime store.
   *
   * @param enginePtr pointer to the engine
   * @return memory segment pointer to the store, or null on failure
   */
  public MemorySegment storeCreate(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");

    final MethodHandle mh = mhStoreCreate;
    if (mh != null) {
      try {
        return (MemorySegment) mh.invokeExact(enginePtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native storeCreate failed", t);
      }
    }
    return callNativeFunction("wasmtime4j_store_create", MemorySegment.class, enginePtr);
  }

  /**
   * Creates a WebAssembly store that is compatible with a specific module.
   *
   * <p>CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Engine Arc as the
   * Module's internal wasmtime::Module. This is required because wasmtime's Instance::new() uses
   * Arc::ptr_eq() to verify engine compatibility.
   *
   * @param modulePtr pointer to the module
   * @return memory segment pointer to the store, or null on failure
   */
  public MemorySegment storeCreateForModule(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction("wasmtime4j_store_new_for_module", MemorySegment.class, modulePtr);
  }

  /**
   * Destroys a WebAssembly store.
   *
   * @param storePtr pointer to the store to destroy
   */
  public void storeDestroy(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    callNativeFunction("wasmtime4j_store_destroy", Void.class, storePtr);
  }

  /**
   * Creates a WebAssembly store with custom configuration.
   *
   * @param enginePtr pointer to the engine
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxTables the maximum number of tables (0 = no limit)
   * @param maxMemories the maximum number of memories (0 = no limit)
   * @param trapOnGrowFailure whether growth failures should trap (0 = false, non-zero = true)
   * @param storePtr pointer to store the created store (output parameter)
   * @return 0 on success, negative error code on failure
   */
  public int storeCreateWithConfig(
      final MemorySegment enginePtr,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSecs,
      final int maxInstances,
      final int maxTableElements,
      final int maxTables,
      final int maxMemories,
      final int trapOnGrowFailure,
      final MemorySegment storePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(storePtr, "storePtr");

    return callNativeFunction(
        "wasmtime4j_panama_store_create_with_config",
        Integer.class,
        enginePtr,
        fuelLimit,
        memoryLimitBytes,
        executionTimeoutSecs,
        maxInstances,
        maxTableElements,
        maxTables,
        maxMemories,
        trapOnGrowFailure,
        storePtr);
  }

  /**
   * Sets WASI context on a WebAssembly store.
   *
   * <p>This function attaches a WASI context to a store, which is required before instantiating
   * modules that import WASI functions.
   *
   * @param storePtr pointer to the store
   * @param wasiCtxPtr pointer to the WASI context
   * @return 0 on success, negative error code on failure
   */
  public int storeSetWasiContext(final MemorySegment storePtr, final MemorySegment wasiCtxPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(wasiCtxPtr, "wasiCtxPtr");
    return callNativeFunction(
        "wasmtime4j_store_set_wasi_context", Integer.class, storePtr, wasiCtxPtr);
  }

  /**
   * Checks if a store has WASI context attached.
   *
   * @param storePtr pointer to the store
   * @return 1 if store has WASI context, 0 if not, negative on error
   */
  public int storeHasWasiContext(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_has_wasi_context", Integer.class, storePtr);
  }

  /**
   * Captures backtrace from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param bufferOutPtr pointer to store the backtrace buffer pointer
   * @param bufferLenOutPtr pointer to store the buffer length
   * @return 0 on success, negative error code on failure
   */
  public int storeCaptureBacktrace(
      final MemorySegment storePtr,
      final MemorySegment bufferOutPtr,
      final MemorySegment bufferLenOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferOutPtr, "bufferOutPtr");
    validatePointer(bufferLenOutPtr, "bufferLenOutPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_capture_backtrace",
        Integer.class,
        storePtr,
        bufferOutPtr,
        bufferLenOutPtr);
  }

  /**
   * Force captures backtrace from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param bufferOutPtr pointer to store the backtrace buffer pointer
   * @param bufferLenOutPtr pointer to store the buffer length
   * @return 0 on success, negative error code on failure
   */
  public int storeForceCaptureBacktrace(
      final MemorySegment storePtr,
      final MemorySegment bufferOutPtr,
      final MemorySegment bufferLenOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferOutPtr, "bufferOutPtr");
    validatePointer(bufferLenOutPtr, "bufferLenOutPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_force_capture_backtrace",
        Integer.class,
        storePtr,
        bufferOutPtr,
        bufferLenOutPtr);
  }

  /**
   * Frees a byte array buffer allocated by native backtrace capture functions.
   *
   * @param dataPtr pointer to the data
   * @param len length of the data in bytes
   */
  public void freeBuffer(final MemorySegment dataPtr, final int len) {
    if (dataPtr != null && !dataPtr.equals(MemorySegment.NULL) && len > 0) {
      callNativeFunction("wasmtime4j_free_byte_array", Void.class, dataPtr, (long) len);
    }
  }

  /**
   * Performs garbage collection on a WebAssembly store.
   *
   * <p>This triggers garbage collection to reclaim unreachable GC objects. If GC support is not
   * enabled in the engine configuration, this is a no-op.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGc(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    final MethodHandle mh = mhStoreGc;
    if (mh != null) {
      try {
        return (int) mh.invokeExact(storePtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native storeGc failed", t);
      }
    }
    return callNativeFunction("wasmtime4j_panama_store_gc", Integer.class, storePtr);
  }

  /**
   * Performs asynchronous garbage collection on a WebAssembly store.
   *
   * <p>This is the async version of {@link #storeGc(MemorySegment)} that cooperatively yields
   * during GC if the store is configured with epoch-based interruption.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGcAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_gc_async", Integer.class, storePtr);
  }

  /**
   * Configures the epoch deadline to async yield and update.
   *
   * <p>When the epoch deadline is reached, execution will yield back to the async executor and then
   * continue with a new deadline of current epoch + deltaTicks.
   *
   * @param storePtr pointer to the store
   * @param deltaTicks number of ticks to add for the new deadline
   * @return 0 on success, negative error code on failure
   */
  public int storeEpochDeadlineAsyncYieldAndUpdate(
      final MemorySegment storePtr, final long deltaTicks) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_epoch_deadline_async_yield_and_update",
        Integer.class,
        storePtr,
        deltaTicks);
  }

  /**
   * Configures the epoch deadline to trap when reached.
   *
   * <p>When the epoch deadline is reached, execution will trap immediately.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeEpochDeadlineTrap(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_epoch_deadline_trap", Integer.class, storePtr);
  }

  /**
   * Clears the epoch deadline callback from the store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeClearEpochDeadlineCallback(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_clear_epoch_deadline_callback", Integer.class, storePtr);
  }

  /**
   * Sets an epoch deadline callback with a function pointer on the store.
   *
   * <p>This allows the Java code to receive callbacks when the epoch deadline is reached and decide
   * whether to continue execution (by returning a positive delta) or trap (by returning a negative
   * value).
   *
   * @param storePtr pointer to the store
   * @param callbackFn function pointer for the epoch callback (takes callback_id, epoch; returns
   *     delta or negative to trap)
   * @param callbackId identifier passed to the callback to identify the Java callback
   * @return 0 on success, negative error code on failure
   */
  public int storeSetEpochDeadlineCallbackFn(
      final MemorySegment storePtr, final MemorySegment callbackFn, final long callbackId) {
    validatePointer(storePtr, "storePtr");
    validatePointer(callbackFn, "callbackFn");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_epoch_deadline_callback_fn",
        Integer.class,
        storePtr,
        callbackFn,
        callbackId);
  }

  /**
   * Sets a debug handler with a function pointer on the store.
   *
   * @param storePtr pointer to the store
   * @param callbackFn function pointer for the debug handler callback (takes callback_id,
   *     event_code)
   * @param callbackId identifier passed to the callback to identify the Java handler
   * @return 0 on success, negative error code on failure
   */
  public int storeSetDebugHandlerFn(
      final MemorySegment storePtr, final MemorySegment callbackFn, final long callbackId) {
    validatePointer(storePtr, "storePtr");
    validatePointer(callbackFn, "callbackFn");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_debug_handler",
        Integer.class,
        storePtr,
        callbackFn,
        callbackId);
  }

  /**
   * Clears the debug handler from the store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeClearDebugHandler(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_clear_debug_handler", Integer.class, storePtr);
  }

  /**
   * Sets a dynamic resource limiter on the store with callback function pointers.
   *
   * <p>The callbacks are invoked by the Wasmtime runtime each time a memory or table needs to grow,
   * allowing dynamic per-request resource allocation decisions.
   *
   * @param storePtr pointer to the store
   * @param callbackId identifier passed to callbacks for Java-side dispatch
   * @param memoryGrowingFn function pointer for memory grow decisions
   * @param tableGrowingFn function pointer for table grow decisions
   * @param memoryGrowFailedFn function pointer for memory grow failure notifications (nullable)
   * @param tableGrowFailedFn function pointer for table grow failure notifications (nullable)
   * @return 0 on success, negative error code on failure
   */
  public int storeSetResourceLimiter(
      final MemorySegment storePtr,
      final long callbackId,
      final MemorySegment memoryGrowingFn,
      final MemorySegment tableGrowingFn,
      final MemorySegment memoryGrowFailedFn,
      final MemorySegment tableGrowFailedFn) {
    validatePointer(storePtr, "storePtr");
    validatePointer(memoryGrowingFn, "memoryGrowingFn");
    validatePointer(tableGrowingFn, "tableGrowingFn");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_resource_limiter",
        Integer.class,
        storePtr,
        callbackId,
        memoryGrowingFn,
        tableGrowingFn,
        memoryGrowFailedFn,
        tableGrowFailedFn);
  }

  /**
   * Sets a dynamic async resource limiter on the store with callback function pointers.
   *
   * <p>This is the async counterpart to {@link #storeSetResourceLimiter}. It uses the same callback
   * signatures but registers via the async limiter path in Wasmtime. Requires the engine to be
   * configured with async support.
   *
   * @param storePtr pointer to the store
   * @param callbackId identifier passed to callbacks for Java-side dispatch
   * @param memoryGrowingFn function pointer for memory grow decisions
   * @param tableGrowingFn function pointer for table grow decisions
   * @param memoryGrowFailedFn function pointer for memory grow failure notifications (nullable)
   * @param tableGrowFailedFn function pointer for table grow failure notifications (nullable)
   * @return 0 on success, negative error code on failure
   */
  public int storeSetResourceLimiterAsync(
      final MemorySegment storePtr,
      final long callbackId,
      final MemorySegment memoryGrowingFn,
      final MemorySegment tableGrowingFn,
      final MemorySegment memoryGrowFailedFn,
      final MemorySegment tableGrowFailedFn) {
    validatePointer(storePtr, "storePtr");
    validatePointer(memoryGrowingFn, "memoryGrowingFn");
    validatePointer(tableGrowingFn, "tableGrowingFn");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_resource_limiter_async",
        Integer.class,
        storePtr,
        callbackId,
        memoryGrowingFn,
        tableGrowingFn,
        memoryGrowFailedFn,
        tableGrowFailedFn);
  }

  // ===== Store Exception Methods =====

  /**
   * Throws an exception in the store.
   *
   * @param storePtr the store pointer
   * @param exnRefPtr the exception reference pointer
   * @return 0 on success, -1 on error
   */
  public int storeThrowException(final MemorySegment storePtr, final MemorySegment exnRefPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(exnRefPtr, "exnRefPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_throw_exception", Integer.class, storePtr, exnRefPtr);
  }

  /**
   * Takes and removes the pending exception from the store.
   *
   * @param storePtr the store pointer
   * @return the exception reference pointer, or NULL if no pending exception
   */
  public MemorySegment storeTakePendingException(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_take_pending_exception", MemorySegment.class, storePtr);
  }

  /**
   * Checks if the store has a pending exception.
   *
   * @param storePtr the store pointer
   * @return 1 if pending exception exists, 0 if not, -1 on error
   */
  public int storeHasPendingException(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_has_pending_exception", Integer.class, storePtr);
  }

  // ===== Store Call Hook Methods =====

  /**
   * Sets a call hook on the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeSetCallHook(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_set_call_hook", Integer.class, storePtr);
  }

  /**
   * Clears the call hook from the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeClearCallHook(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_clear_call_hook", Integer.class, storePtr);
  }

  /**
   * Sets an async call hook on the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeSetCallHookAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_call_hook_async", Integer.class, storePtr);
  }

  /**
   * Clears the async call hook from the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeClearCallHookAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_clear_call_hook_async", Integer.class, storePtr);
  }

  // ===== Store Fuel/Epoch MethodHandle Getters =====

  /**
   * Gets the method handle for setting fuel level in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreSetFuel() {
    return mhStoreSetFuel;
  }

  /**
   * Gets the method handle for getting remaining fuel from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreGetFuel() {
    return mhStoreGetFuel;
  }

  /**
   * Gets the method handle for adding fuel to a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreAddFuel() {
    return mhStoreAddFuel;
  }

  /**
   * Gets the method handle for consuming fuel from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreConsumeFuel() {
    return mhStoreConsumeFuel;
  }

  /**
   * Gets the method handle for setting fuel async yield interval in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreSetFuelAsyncYieldInterval() {
    return mhStoreSetFuelAsyncYieldInterval;
  }

  /**
   * Gets the method handle for trying to create a store (OOM-safe).
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreTryCreate() {
    return mhStoreTryCreate;
  }

  /**
   * Gets the method handle for getting hostcall fuel from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreGetHostcallFuel() {
    return mhStoreGetHostcallFuel;
  }

  /**
   * Gets the method handle for setting hostcall fuel in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreSetHostcallFuel() {
    return mhStoreSetHostcallFuel;
  }

  /**
   * Gets the method handle for setting epoch deadline in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreSetEpochDeadline() {
    return mhStoreSetEpochDeadline;
  }

  /**
   * Gets the method handle for getting execution statistics from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreGetExecutionStats() {
    return resolveHandle("wasmtime4j_panama_store_get_execution_stats");
  }

  // ===== Debugging API =====

  /**
   * Gets the method handle for checking single-step mode.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getStoreIsSingleStep() {
    return resolveHandle("wasmtime4j_store_is_single_step");
  }

  /**
   * Gets the method handle for getting breakpoint count.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getStoreBreakpointCount() {
    return resolveHandle("wasmtime4j_store_breakpoint_count");
  }

  /**
   * Gets the method handle for setting single-step mode.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getStoreSetSingleStep() {
    return resolveHandle("wasmtime4j_store_set_single_step");
  }

  /**
   * Gets the method handle for adding a breakpoint.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getStoreAddBreakpoint() {
    return resolveHandle("wasmtime4j_store_add_breakpoint");
  }

  /**
   * Gets the method handle for removing a breakpoint.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getStoreRemoveBreakpoint() {
    return resolveHandle("wasmtime4j_store_remove_breakpoint");
  }

  /**
   * Gets the method handle for checking async support.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getStoreIsAsync() {
    return resolveHandle("wasmtime4j_store_is_async");
  }

  /**
   * Gets the method handle for retrieving debug exit frames.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getStoreDebugExitFrames() {
    return resolveHandle("wasmtime4j_store_debug_exit_frames");
  }
}
