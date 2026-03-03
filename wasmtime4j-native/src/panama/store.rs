//! Panama FFI bindings for WebAssembly store operations
//!
//! This module provides C-compatible functions for creating, managing,
//! and interacting with WebAssembly stores. Stores hold the runtime state
//! for WebAssembly instances including fuel, epoch deadlines, and garbage collection.

use crate::error::ffi_utils;
use crate::store::core;
use crate::WasmtimeResult;
use std::os::raw::{c_char, c_int, c_uchar, c_uint, c_ulong, c_void};
use wasmtime::{FuncType, ValType};

/// Create a new WebAssembly store with default configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_create(
    engine_ptr: *mut c_void,
    store_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };

        let store = core::create_store(engine)?;
        let raw_ptr = Box::into_raw(store);
        crate::memory::core::register_store_handle(raw_ptr as *const c_void)?;

        unsafe {
            *store_ptr = raw_ptr as *mut c_void;
        }

        Ok(())
    })
}

/// Create a new WebAssembly store with custom configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_create_with_config(
    engine_ptr: *mut c_void,
    fuel_limit: c_ulong,             // 0 = no limit
    memory_limit_bytes: c_ulong,     // 0 = no limit
    execution_timeout_secs: c_ulong, // 0 = no timeout
    max_instances: c_uint,           // 0 = no limit
    max_table_elements: c_uint,      // 0 = no limit
    max_tables: c_uint,              // 0 = no limit
    max_memories: c_uint,            // 0 = no limit
    trap_on_grow_failure: c_int,     // 0 = false, non-zero = true
    store_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };

        use crate::ffi_common::parameter_conversion::{
            zero_to_none_u32, zero_to_none_u64, zero_to_none_usize,
        };

        let store = core::create_store_with_config(
            engine,
            zero_to_none_u64(fuel_limit as i64),
            zero_to_none_usize(memory_limit_bytes as i64),
            zero_to_none_u64(execution_timeout_secs as i64),
            zero_to_none_usize(max_instances as i64),
            zero_to_none_u32(max_table_elements as i32),
            zero_to_none_usize(max_tables as i64),
            zero_to_none_usize(max_memories as i64),
            trap_on_grow_failure != 0,
        )?;
        let raw_ptr = Box::into_raw(store);
        crate::memory::core::register_store_handle(raw_ptr as *const c_void)?;

        unsafe {
            *store_ptr = raw_ptr as *mut c_void;
        }

        Ok(())
    })
}

/// Add fuel to the store for execution limiting (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_add_fuel(store_ptr: *mut c_void, fuel: c_ulong) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::add_fuel(store, fuel as u64)?;
        Ok(())
    })
}

/// Get remaining fuel in the store (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_get_fuel_remaining(
    store_ptr: *mut c_void,
    fuel_ptr: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let fuel = core::get_fuel_remaining(store)?;
        unsafe {
            *fuel_ptr = fuel as c_ulong;
        }
        Ok(())
    })
}

/// Consume fuel from the store (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_consume_fuel(
    store_ptr: *mut c_void,
    fuel_to_consume: c_ulong,
    fuel_consumed_ptr: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let actual_consumed = core::consume_fuel(store, fuel_to_consume as u64)?;
        unsafe {
            *fuel_consumed_ptr = actual_consumed as c_ulong;
        }
        Ok(())
    })
}

/// Set epoch deadline for interruption (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_epoch_deadline(
    store_ptr: *mut c_void,
    ticks: c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::set_epoch_deadline(store, ticks as u64);
        Ok(())
    })
}

/// Configure epoch deadline trap behavior (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_epoch_deadline_trap(store_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::epoch_deadline_trap(store)?;
        Ok(())
    })
}

/// Set epoch deadline callback (Panama FFI version)
///
/// Note: This configures trap behavior as the default callback.
/// Use `wasmtime4j_panama_store_set_epoch_deadline_callback_fn` for custom callbacks.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_epoch_deadline_callback(
    store_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::epoch_deadline_trap(store)?;
        Ok(())
    })
}

/// Clear epoch deadline callback (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_clear_epoch_deadline_callback(
    store_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        // Clear by setting trap behavior (default)
        core::epoch_deadline_trap(store)?;
        Ok(())
    })
}

/// Epoch deadline callback function pointer type for Panama FFI.
///
/// # Parameters
/// * `callback_id` - i64 - Identifier passed back to the callback (for Java to identify the callback)
/// * `epoch` - u64 - Current epoch value (reserved for future use)
///
/// # Returns
/// * Positive i64: Continue execution and add this many ticks to the deadline
/// * Negative i64: Trap execution
type EpochDeadlineCallbackFn = extern "C" fn(callback_id: i64, epoch: u64) -> i64;

/// Set epoch deadline callback with function pointer (Panama FFI version)
///
/// This allows setting a callback function that will be invoked when the epoch
/// deadline is reached. The callback can decide to continue execution by returning
/// a positive delta, or trap by returning a negative value.
///
/// # Arguments
/// * `store_ptr` - Pointer to the store
/// * `callback_fn` - Function pointer for the epoch deadline callback
/// * `callback_id` - Identifier passed to the callback (for Java to identify the callback)
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_epoch_deadline_callback_fn(
    store_ptr: *mut c_void,
    callback_fn: EpochDeadlineCallbackFn,
    callback_id: i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::epoch_deadline_callback_with_fn(store, callback_fn, callback_id)?;
        Ok(())
    })
}

/// Debug handler callback function type for Panama FFI
pub type DebugHandlerCallbackFn = extern "C" fn(callback_id: i64, event_code: i32);

/// Set debug handler on store (Panama FFI version)
///
/// # Arguments
/// * `store_ptr` - Pointer to the store
/// * `callback_fn` - Function pointer for the debug handler callback
/// * `callback_id` - Identifier passed to the callback
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_debug_handler(
    store_ptr: *mut c_void,
    callback_fn: DebugHandlerCallbackFn,
    callback_id: i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        store.set_debug_handler_with_fn(callback_fn, callback_id)?;
        Ok(())
    })
}

/// Clear debug handler on store (Panama FFI version)
///
/// # Arguments
/// * `store_ptr` - Pointer to the store
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_clear_debug_handler(store_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        store.clear_debug_handler()?;
        Ok(())
    })
}

/// Configure epoch deadline to yield and update (Panama FFI version)
///
/// This configures the store to yield when the epoch deadline is reached,
/// then increment the deadline by the given delta and continue execution.
///
/// # Arguments
/// * `store_ptr` - Pointer to the store
/// * `delta` - Number of ticks to add to the deadline after yielding
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_epoch_deadline_async_yield_and_update(
    store_ptr: *mut c_void,
    delta: c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::epoch_deadline_async_yield_and_update(store, delta as u64)?;
        Ok(())
    })
}

/// Force garbage collection in the store (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_garbage_collect(store_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::garbage_collect(store)?;
        Ok(())
    })
}

/// Validate store functionality (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_validate(store_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::validate_store(store)?;
        Ok(())
    })
}

/// Get store execution statistics (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_get_execution_stats(
    store_ptr: *mut c_void,
    execution_count_ptr: *mut c_ulong,
    total_execution_time_us_ptr: *mut c_ulong,
    fuel_consumed_ptr: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let stats = core::get_execution_stats(store)?;

        unsafe {
            *execution_count_ptr = stats.execution_count;
            *total_execution_time_us_ptr = stats.total_execution_time.as_micros() as c_ulong;
            *fuel_consumed_ptr = stats.fuel_consumed;
        }

        Ok(())
    })
}

/// Get store memory usage statistics (Panama FFI version).
///
/// `total_bytes_ptr` and `used_bytes_ptr` are always written as 0 —
/// Wasmtime does not expose per-store memory aggregation.
/// The parameters are preserved for ABI compatibility.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_get_memory_usage(
    store_ptr: *mut c_void,
    total_bytes_ptr: *mut c_ulong,
    used_bytes_ptr: *mut c_ulong,
    instance_count_ptr: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let usage = core::get_memory_usage(store)?;

        unsafe {
            *total_bytes_ptr = 0;
            *used_bytes_ptr = 0;
            *instance_count_ptr = usage.execution_count as c_ulong;
        }

        Ok(())
    })
}

/// Get store metadata (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_get_metadata(
    store_ptr: *mut c_void,
    fuel_limit_ptr: *mut c_ulong,
    memory_limit_bytes_ptr: *mut c_ulong,
    execution_timeout_secs_ptr: *mut c_ulong,
    instance_count_ptr: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let metadata = core::get_store_metadata(store);

        unsafe {
            *fuel_limit_ptr = metadata.fuel_limit.unwrap_or(0) as c_ulong;
            *memory_limit_bytes_ptr = metadata.memory_limit_bytes.unwrap_or(0) as c_ulong;
            *execution_timeout_secs_ptr =
                metadata.execution_timeout.map(|d| d.as_secs()).unwrap_or(0) as c_ulong;
            *instance_count_ptr = metadata.instance_count as c_ulong;
        }

        Ok(())
    })
}

/// Callback type for Panama host functions used in store-level creation
type StoreHostFunctionCallback = extern "C" fn(
    callback_id: i64,
    params_ptr: *const c_void,
    params_len: c_uint,
    results_ptr: *mut c_void,
    results_len: c_uint,
    error_message_ptr: *mut c_char,
    error_message_len: c_uint,
) -> c_int;

use crate::ffi_common::valtype_conversion;

/// Create a host function and register it for table operations (Panama FFI version)
///
/// This creates a Wasmtime Func that wraps a Panama callback and registers it
/// in the reference registry so it can be used with table.set() operations.
///
/// # Parameters
/// - store_ptr: Pointer to the Store
/// - callback_fn: The Panama callback function pointer
/// - callback_id: Unique ID for the callback
/// - param_types: Array of parameter type codes
/// - param_count: Number of parameters
/// - return_types: Array of return type codes
/// - return_count: Number of return values
/// - func_ref_id_out: Output pointer for the function reference ID
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_create_host_function(
    store_ptr: *mut c_void,
    callback_fn: StoreHostFunctionCallback,
    callback_id: i64,
    param_types: *const c_int,
    param_count: c_uint,
    return_types: *const c_int,
    return_count: c_uint,
    func_ref_id_out: *mut c_ulong,
) -> c_int {
    use crate::hostfunc::HostFunctionCallback;
    use crate::instance::WasmValue;

    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_mut(store_ptr)? };

        // Convert parameter types
        let param_slice = unsafe { std::slice::from_raw_parts(param_types, param_count as usize) };
        let param_val_types: Vec<ValType> = param_slice
            .iter()
            .map(|&t| valtype_conversion::int_to_valtype(t))
            .collect::<Result<Vec<_>, _>>()?;

        // Convert return types
        let return_slice =
            unsafe { std::slice::from_raw_parts(return_types, return_count as usize) };
        let return_val_types: Vec<ValType> = return_slice
            .iter()
            .map(|&t| valtype_conversion::int_to_valtype(t))
            .collect::<Result<Vec<_>, _>>()?;

        // Create FuncType using the Store's engine
        let func_type = store.with_context(|ctx| {
            let engine = ctx.engine();
            Ok(FuncType::new(
                engine,
                param_val_types.clone(),
                return_val_types.clone(),
            ))
        })?;

        // Create callback implementation that calls the Panama callback
        // This mirrors the linker module's PanamaHostFunctionCallbackImpl
        struct StoreHostFunctionCallbackImpl {
            callback_fn: StoreHostFunctionCallback,
            callback_id: i64,
            result_count: usize,
        }

        impl HostFunctionCallback for StoreHostFunctionCallbackImpl {
            fn execute(&self, params: &[WasmValue]) -> crate::WasmtimeResult<Vec<WasmValue>> {
                log::debug!("[STORE_CB] StoreHostFunctionCallbackImpl.execute, callback_id={}, params.len={}, expected_results={}",
                    self.callback_id, params.len(), self.result_count);

                // Convert internal WasmValue to FFI-safe format
                let ffi_params: Vec<crate::instance::FfiWasmValue> = params
                    .iter()
                    .map(crate::instance::FfiWasmValue::from_wasm_value)
                    .collect();

                // Allocate result buffer in FFI-safe format
                let expected_results = self.result_count;
                let mut ffi_results = vec![
                    crate::instance::FfiWasmValue {
                        tag: 0,
                        value: [0u8; 16]
                    };
                    expected_results
                ];

                // Allocate error message buffer
                const ERROR_BUFFER_SIZE: usize = 1024;
                let mut error_message_buffer = vec![0u8; ERROR_BUFFER_SIZE];

                // Call the Panama function pointer with FFI-safe structs
                let result_code = (self.callback_fn)(
                    self.callback_id,
                    ffi_params.as_ptr() as *const c_void,
                    ffi_params.len() as c_uint,
                    ffi_results.as_mut_ptr() as *mut c_void,
                    expected_results as c_uint,
                    error_message_buffer.as_mut_ptr() as *mut c_char,
                    ERROR_BUFFER_SIZE as c_uint,
                );

                if result_code != 0 {
                    // Extract error message from buffer (safe operations on the stack buffer)
                    let len = error_message_buffer
                        .iter()
                        .position(|&b| b == 0)
                        .unwrap_or(ERROR_BUFFER_SIZE);
                    let error_message =
                        String::from_utf8_lossy(&error_message_buffer[..len]).to_string();

                    let final_message = if error_message.is_empty() {
                        format!(
                            "Panama host function callback failed with code: {}",
                            result_code
                        )
                    } else {
                        error_message
                    };

                    log::error!("[STORE_CB] Error: {}", final_message);
                    return Err(crate::error::WasmtimeError::Function {
                        message: final_message,
                    });
                }

                // Convert FFI results back to internal WasmValue
                let results: Vec<WasmValue> = ffi_results
                    .iter()
                    .map(crate::instance::FfiWasmValue::to_wasm_value)
                    .collect::<crate::WasmtimeResult<Vec<WasmValue>>>()?;
                log::debug!(
                    "[STORE_CB] Converted {} results, execute complete",
                    results.len()
                );

                Ok(results)
            }

            fn clone_callback(&self) -> Box<dyn HostFunctionCallback> {
                Box::new(StoreHostFunctionCallbackImpl {
                    callback_fn: self.callback_fn,
                    callback_id: self.callback_id,
                    result_count: self.result_count,
                })
            }
        }

        unsafe impl Send for StoreHostFunctionCallbackImpl {}
        unsafe impl Sync for StoreHostFunctionCallbackImpl {}

        let callback = Box::new(StoreHostFunctionCallbackImpl {
            callback_fn,
            callback_id,
            result_count: return_count as usize,
        });

        // Create the host function in the store
        let (_, wasmtime_func) = store.create_host_function(
            format!("panama_host_func_{}", callback_id),
            func_type,
            callback,
        )?;

        // Register the Wasmtime Func in the reference registry
        let func_ref_id =
            crate::table::core::register_function_reference(wasmtime_func, store.id())?;

        unsafe {
            *func_ref_id_out = func_ref_id;
        }

        log::debug!(
            "Created Panama host function with callback_id={}, func_ref_id={}",
            callback_id,
            func_ref_id
        );

        Ok(())
    })
}

/// Create an unchecked host function that skips per-call type validation (Panama FFI version)
///
/// Identical to wasmtime4j_panama_store_create_host_function but uses Func::new_unchecked
/// internally for better performance. The caller is responsible for type correctness.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_create_host_function_unchecked(
    store_ptr: *mut c_void,
    callback_fn: StoreHostFunctionCallback,
    callback_id: i64,
    param_types: *const c_int,
    param_count: c_uint,
    return_types: *const c_int,
    return_count: c_uint,
    func_ref_id_out: *mut c_ulong,
) -> c_int {
    use crate::hostfunc::HostFunctionCallback;
    use crate::instance::WasmValue;

    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_mut(store_ptr)? };

        let param_slice = unsafe { std::slice::from_raw_parts(param_types, param_count as usize) };
        let param_val_types: Vec<ValType> = param_slice
            .iter()
            .map(|&t| valtype_conversion::int_to_valtype(t))
            .collect::<Result<Vec<_>, _>>()?;

        let return_slice =
            unsafe { std::slice::from_raw_parts(return_types, return_count as usize) };
        let return_val_types: Vec<ValType> = return_slice
            .iter()
            .map(|&t| valtype_conversion::int_to_valtype(t))
            .collect::<Result<Vec<_>, _>>()?;

        let func_type = store.with_context(|ctx| {
            let engine = ctx.engine();
            Ok(FuncType::new(
                engine,
                param_val_types.clone(),
                return_val_types.clone(),
            ))
        })?;

        struct StoreHostFunctionCallbackImpl {
            callback_fn: StoreHostFunctionCallback,
            callback_id: i64,
            result_count: usize,
        }

        impl HostFunctionCallback for StoreHostFunctionCallbackImpl {
            fn execute(&self, params: &[WasmValue]) -> crate::WasmtimeResult<Vec<WasmValue>> {
                let ffi_params: Vec<crate::instance::FfiWasmValue> = params
                    .iter()
                    .map(crate::instance::FfiWasmValue::from_wasm_value)
                    .collect();

                let expected_results = self.result_count;
                let mut ffi_results = vec![
                    crate::instance::FfiWasmValue {
                        tag: 0,
                        value: [0u8; 16]
                    };
                    expected_results
                ];

                const ERROR_BUFFER_SIZE: usize = 1024;
                let mut error_message_buffer = vec![0u8; ERROR_BUFFER_SIZE];

                let result_code = (self.callback_fn)(
                    self.callback_id,
                    ffi_params.as_ptr() as *const c_void,
                    ffi_params.len() as c_uint,
                    ffi_results.as_mut_ptr() as *mut c_void,
                    expected_results as c_uint,
                    error_message_buffer.as_mut_ptr() as *mut c_char,
                    ERROR_BUFFER_SIZE as c_uint,
                );

                if result_code != 0 {
                    let len = error_message_buffer
                        .iter()
                        .position(|&b| b == 0)
                        .unwrap_or(ERROR_BUFFER_SIZE);
                    let error_message =
                        String::from_utf8_lossy(&error_message_buffer[..len]).to_string();

                    let final_message = if error_message.is_empty() {
                        format!(
                            "Panama host function callback failed with code: {}",
                            result_code
                        )
                    } else {
                        error_message
                    };

                    return Err(crate::error::WasmtimeError::Function {
                        message: final_message,
                    });
                }

                let results: Vec<WasmValue> = ffi_results
                    .iter()
                    .map(crate::instance::FfiWasmValue::to_wasm_value)
                    .collect::<crate::WasmtimeResult<Vec<WasmValue>>>()?;

                Ok(results)
            }

            fn clone_callback(&self) -> Box<dyn HostFunctionCallback> {
                Box::new(StoreHostFunctionCallbackImpl {
                    callback_fn: self.callback_fn,
                    callback_id: self.callback_id,
                    result_count: self.result_count,
                })
            }
        }

        unsafe impl Send for StoreHostFunctionCallbackImpl {}
        unsafe impl Sync for StoreHostFunctionCallbackImpl {}

        let callback = Box::new(StoreHostFunctionCallbackImpl {
            callback_fn,
            callback_id,
            result_count: return_count as usize,
        });

        let (_, wasmtime_func) = store.create_host_function_unchecked(
            format!("panama_host_func_unchecked_{}", callback_id),
            func_type,
            callback,
        )?;

        let func_ref_id =
            crate::table::core::register_function_reference(wasmtime_func, store.id())?;

        unsafe {
            *func_ref_id_out = func_ref_id;
        }

        Ok(())
    })
}

/// Destroy a host function and unregister it from the reference registry (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_destroy_host_function(func_ref_id: c_ulong) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if func_ref_id != 0 {
            match crate::table::core::remove_function_reference(func_ref_id) {
                Ok(Some(_)) => {
                    log::debug!(
                        "Removed function reference {} from table registry",
                        func_ref_id
                    );
                }
                Ok(None) => {
                    log::debug!(
                        "Function reference {} was not in table registry",
                        func_ref_id
                    );
                }
                Err(e) => {
                    log::warn!(
                        "Failed to remove function reference {} from table registry: {}",
                        func_ref_id,
                        e
                    );
                }
            }
        }
        Ok(())
    })
}

/// Destroy a WebAssembly store (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_destroy(store_ptr: *mut c_void) {
    if !store_ptr.is_null() {
        // Unregister the store handle from memory module before destroying
        let _ = crate::memory::core::unregister_store_handle(store_ptr as *const c_void);
    }
    unsafe {
        core::destroy_store(store_ptr);
    }
}

/// Capture backtrace from a WebAssembly store (Panama FFI version)
///
/// Returns a serialized backtrace as a byte array, or null on error.
/// The caller must free the returned buffer using wasmtime4j_panama_free_buffer.
///
/// Format: [frame_count: u32][force_capture: u8][frames...]
/// Each frame: [func_index: u32][has_func_name: u8][func_name_len: u32][func_name: bytes]
///             [has_module_offset: u8][module_offset: u32][has_func_offset: u8][func_offset: u32]
///             [symbol_count: u32][symbols...]
/// Each symbol: [has_name: u8][name_len: u32][name: bytes][has_file: u8][file_len: u32][file: bytes]
///              [has_line: u8][line: u32][has_column: u8][column: u32]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_capture_backtrace(
    store_ptr: *mut c_void,
    buffer_out: *mut *mut c_uchar,
    buffer_len_out: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let store_lock = store.inner.lock();
        let backtrace = wasmtime::WasmBacktrace::capture(&*store_lock);

        let serialized = serialize_backtrace(&backtrace, false)?;

        unsafe {
            let buffer = Box::into_raw(serialized.into_boxed_slice());
            *buffer_out = (*buffer).as_mut_ptr();
            *buffer_len_out = (&(*buffer)).len() as c_uint;
        }

        Ok(())
    })
}

/// Force capture backtrace from a WebAssembly store (Panama FFI version)
///
/// Same as capture_backtrace but forces capture even if backtrace capture is disabled.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_force_capture_backtrace(
    store_ptr: *mut c_void,
    buffer_out: *mut *mut c_uchar,
    buffer_len_out: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let store_lock = store.inner.lock();
        let backtrace = wasmtime::WasmBacktrace::force_capture(&*store_lock);

        let serialized = serialize_backtrace(&backtrace, true)?;

        unsafe {
            let buffer = Box::into_raw(serialized.into_boxed_slice());
            *buffer_out = (*buffer).as_mut_ptr();
            *buffer_len_out = (&(*buffer)).len() as c_uint;
        }

        Ok(())
    })
}

/// Trigger garbage collection on a WebAssembly store (Panama FFI version)
///
/// This explicitly triggers GC to reclaim unreachable GC objects. If GC support
/// is not enabled in the engine configuration, this is a no-op.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_gc(store_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let mut store_lock = store.inner.lock();
        // Wasmtime's gc() method takes Option<&GcHeapOutOfMemory<()>>
        // Passing None means we're not in an OOM recovery scenario
        let _ = store_lock.gc(None);
        Ok(())
    })
}

/// Set fuel for a WebAssembly store (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_fuel(store_ptr: *mut c_void, fuel: c_ulong) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };
        crate::store::core::set_fuel(store, fuel as u64)?;
        Ok(())
    })
}

/// Set fuel async yield interval for a WebAssembly store (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_fuel_async_yield_interval(
    store_ptr: *mut c_void,
    interval: c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };
        crate::store::core::set_fuel_async_yield_interval(store, interval as u64)?;
        Ok(())
    })
}

/// Try to create a store, returning an error on allocation failure (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_try_create(
    engine_ptr: *mut c_void,
    store_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };

        let store = core::try_create_store(engine)?;
        let raw_ptr = Box::into_raw(store);
        crate::memory::core::register_store_handle(raw_ptr as *const c_void)?;

        unsafe {
            *store_ptr = raw_ptr as *mut c_void;
        }

        Ok(())
    })
}

/// Get hostcall fuel limit (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_get_hostcall_fuel(
    store_ptr: *mut c_void,
    fuel_ptr: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        let fuel = core::get_hostcall_fuel(store)?;
        unsafe {
            *fuel_ptr = fuel as c_ulong;
        }
        Ok(())
    })
}

/// Set hostcall fuel limit (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_hostcall_fuel(
    store_ptr: *mut c_void,
    fuel: c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::set_hostcall_fuel(store, fuel as usize)?;
        Ok(())
    })
}

/// Helper function to serialize a backtrace into a byte buffer
fn serialize_backtrace(
    backtrace: &wasmtime::WasmBacktrace,
    force_capture: bool,
) -> WasmtimeResult<Vec<u8>> {
    let mut buffer = Vec::new();

    // Write frame count
    buffer.extend_from_slice(&(backtrace.frames().len() as u32).to_le_bytes());

    // Write force_capture flag
    buffer.push(if force_capture { 1 } else { 0 });

    // Write each frame
    for frame in backtrace.frames() {
        // Function index
        buffer.extend_from_slice(&(frame.func_index() as u32).to_le_bytes());

        // Function name (optional)
        if let Some(func_name) = frame.func_name() {
            buffer.push(1); // has_func_name = true
            let name_bytes = func_name.as_bytes();
            buffer.extend_from_slice(&(name_bytes.len() as u32).to_le_bytes());
            buffer.extend_from_slice(name_bytes);
        } else {
            buffer.push(0); // has_func_name = false
        }

        // Module offset (optional)
        if let Some(offset) = frame.module_offset() {
            buffer.push(1); // has_module_offset = true
            buffer.extend_from_slice(&(offset as u32).to_le_bytes());
        } else {
            buffer.push(0); // has_module_offset = false
        }

        // Function offset (optional)
        if let Some(offset) = frame.func_offset() {
            buffer.push(1); // has_func_offset = true
            buffer.extend_from_slice(&(offset as u32).to_le_bytes());
        } else {
            buffer.push(0); // has_func_offset = false
        }

        // Symbols
        let symbols = frame.symbols();
        buffer.extend_from_slice(&(symbols.len() as u32).to_le_bytes());

        for symbol in symbols.iter() {
            // Symbol name (optional)
            if let Some(name) = symbol.name() {
                buffer.push(1); // has_name = true
                let name_bytes = name.as_bytes();
                buffer.extend_from_slice(&(name_bytes.len() as u32).to_le_bytes());
                buffer.extend_from_slice(name_bytes);
            } else {
                buffer.push(0); // has_name = false
            }

            // Source file (optional)
            if let Some(file) = symbol.file() {
                buffer.push(1); // has_file = true
                let file_bytes = file.as_bytes();
                buffer.extend_from_slice(&(file_bytes.len() as u32).to_le_bytes());
                buffer.extend_from_slice(file_bytes);
            } else {
                buffer.push(0); // has_file = false
            }

            // Line number (optional)
            if let Some(line) = symbol.line() {
                buffer.push(1); // has_line = true
                buffer.extend_from_slice(&(line as u32).to_le_bytes());
            } else {
                buffer.push(0); // has_line = false
            }

            // Column number (optional)
            if let Some(column) = symbol.column() {
                buffer.push(1); // has_column = true
                buffer.extend_from_slice(&(column as u32).to_le_bytes());
            } else {
                buffer.push(0); // has_column = false
            }
        }
    }

    Ok(buffer)
}

/// Get remaining fuel from a WebAssembly store (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_get_fuel(
    store_ptr: *mut c_void,
    fuel_out: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

        if fuel_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Fuel output pointer cannot be null".to_string(),
            });
        }

        let remaining_fuel = crate::store::core::get_fuel_remaining(store)?;

        unsafe {
            *fuel_out = remaining_fuel as c_ulong;
        }

        Ok(())
    })
}

/// Resource limiter callback type for memory grow decisions (Panama FFI)
type PanamaMemoryGrowingCallbackFn =
    extern "C" fn(callback_id: i64, current: u64, desired: u64, maximum: u64) -> i32;

/// Resource limiter callback type for table grow decisions (Panama FFI)
type PanamaTableGrowingCallbackFn =
    extern "C" fn(callback_id: i64, current: u32, desired: u32, maximum: u32) -> i32;

/// Resource limiter callback type for grow failure notifications (Panama FFI)
type PanamaGrowFailedCallbackFn =
    extern "C" fn(callback_id: i64, error: *const std::os::raw::c_char);

/// Set a dynamic resource limiter on a store (Panama FFI version)
///
/// This allows setting callback functions that will be invoked when memory or table
/// grows are requested. The callbacks can dynamically decide whether to allow or deny
/// the growth.
///
/// # Arguments
/// * `store_ptr` - Pointer to the store
/// * `callback_id` - Identifier passed to callbacks for Java-side dispatch
/// * `memory_growing_fn` - Function pointer for memory grow decisions
/// * `table_growing_fn` - Function pointer for table grow decisions
/// * `memory_grow_failed_fn` - Optional function pointer for memory grow failure notifications
/// * `table_grow_failed_fn` - Optional function pointer for table grow failure notifications
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_resource_limiter(
    store_ptr: *mut c_void,
    callback_id: i64,
    memory_growing_fn: PanamaMemoryGrowingCallbackFn,
    table_growing_fn: PanamaTableGrowingCallbackFn,
    memory_grow_failed_fn: Option<PanamaGrowFailedCallbackFn>,
    table_grow_failed_fn: Option<PanamaGrowFailedCallbackFn>,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::set_resource_limiter(
            store,
            callback_id,
            memory_growing_fn,
            table_growing_fn,
            memory_grow_failed_fn,
            table_grow_failed_fn,
        )?;
        Ok(())
    })
}

/// Set an async resource limiter on a store (Panama FFI version).
///
/// Same signature as `set_resource_limiter` but registers with the async limiter path.
/// Requires the engine to be configured with `async_support(true)`.
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_resource_limiter_async(
    store_ptr: *mut c_void,
    callback_id: i64,
    memory_growing_fn: PanamaMemoryGrowingCallbackFn,
    table_growing_fn: PanamaTableGrowingCallbackFn,
    memory_grow_failed_fn: Option<PanamaGrowFailedCallbackFn>,
    table_grow_failed_fn: Option<PanamaGrowFailedCallbackFn>,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::set_resource_limiter_async(
            store,
            callback_id,
            memory_growing_fn,
            table_growing_fn,
            memory_grow_failed_fn,
            table_grow_failed_fn,
        )?;
        Ok(())
    })
}

/// C-compatible function pointer type for call hook callbacks.
///
/// # Arguments
/// * `callback_id` - Identifier for the Java callback
/// * `hook_type` - The type of call transition:
///   * 0 = CallingWasm
///   * 1 = ReturningFromWasm
///   * 2 = CallingHost
///   * 3 = ReturningFromHost
///
/// # Returns
/// * 0 = OK (continue execution)
/// * Non-zero = trap execution
type CallHookCallbackFn = extern "C" fn(callback_id: i64, hook_type: i32) -> i32;

/// Set a call hook on the store (Panama FFI version)
///
/// Installs a no-op call hook on the wasmtime Store, enabling the call hook
/// machinery. The actual callback dispatch happens on the Java side.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_call_hook(store_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::set_call_hook(store)?;
        Ok(())
    })
}

/// Set a call hook with a function pointer callback (Panama FFI version)
///
/// Installs a call hook that invokes the provided callback function on every
/// transition between host and WebAssembly code.
///
/// # Arguments
/// * `store_ptr` - Pointer to the store
/// * `callback_fn` - Function pointer for the call hook callback
/// * `callback_id` - Identifier passed to the callback (for Java dispatch)
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_call_hook_fn(
    store_ptr: *mut c_void,
    callback_fn: CallHookCallbackFn,
    callback_id: i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::set_call_hook_with_fn(store, callback_fn, callback_id)?;
        Ok(())
    })
}

/// Clear the call hook on the store (Panama FFI version)
///
/// Replaces the active call hook with a no-op, effectively disabling it.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_clear_call_hook(store_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { core::get_store_ref(store_ptr)? };
        core::clear_call_hook(store)?;
        Ok(())
    })
}

/// Set an async call hook on the store (Panama FFI version)
///
/// Delegates to the sync version since Java handles async dispatch.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_set_call_hook_async(store_ptr: *mut c_void) -> c_int {
    wasmtime4j_panama_store_set_call_hook(store_ptr)
}

/// Clear the async call hook on the store (Panama FFI version)
///
/// Delegates to the sync version since Java handles async dispatch.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_clear_call_hook_async(store_ptr: *mut c_void) -> c_int {
    wasmtime4j_panama_store_clear_call_hook(store_ptr)
}
