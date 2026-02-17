//! Panama FFI bindings for Linker operations
//!
//! This module provides C-compatible functions for linking WebAssembly modules
//! with host functions, globals, tables, and memories.

use crate::error::ffi_utils;
use crate::hostfunc::{HostFunction, HostFunctionCallback};
use crate::instance::{FfiWasmValue, WasmValue};
use crate::linker::core as linker_core;
use std::ffi::CStr;
use std::os::raw::{c_char, c_int, c_uint, c_void};
use wasmtime::{FuncType, RefType, ValType};

/// Type for Panama callback function pointer
///
/// This function pointer is called from Rust back into Java when a host function is invoked.
///
/// Parameters:
/// - callback_id: i64 - The unique identifier for this callback
/// - params_ptr: *const c_void - Pointer to array of FfiWasmValue parameters
/// - params_len: c_uint - Number of parameters
/// - results_ptr: *mut c_void - Pointer to buffer for FfiWasmValue results
/// - results_len: c_uint - Expected number of results
/// - error_message_ptr: *mut c_char - Buffer for error message (on failure)
/// - error_message_len: c_uint - Size of error message buffer
///
/// Returns: c_int - 0 on success, non-zero on error
type PanamaHostFunctionCallback = extern "C" fn(
    callback_id: i64,
    params_ptr: *const c_void,
    params_len: c_uint,
    results_ptr: *mut c_void,
    results_len: c_uint,
    error_message_ptr: *mut c_char,
    error_message_len: c_uint,
) -> c_int;

/// Panama-specific host function callback implementation
struct PanamaHostFunctionCallbackImpl {
    callback_fn: PanamaHostFunctionCallback,
    callback_id: i64,
    result_count: usize,
}

impl HostFunctionCallback for PanamaHostFunctionCallbackImpl {
    fn execute(&self, params: &[WasmValue]) -> crate::WasmtimeResult<Vec<WasmValue>> {
        // Convert internal WasmValue to FFI-safe format
        let ffi_params: Vec<FfiWasmValue> =
            params.iter().map(FfiWasmValue::from_wasm_value).collect();

        // Allocate result buffer in FFI-safe format
        let expected_results = self.result_count;
        let mut ffi_results = vec![
            FfiWasmValue {
                tag: 0,
                value: [0u8; 16]
            };
            expected_results
        ];

        // Allocate error message buffer (1024 bytes should be sufficient)
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
            let error_message = String::from_utf8_lossy(&error_message_buffer[..len]).to_string();

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

        // Convert FFI results back to internal WasmValue
        let results: Vec<WasmValue> = ffi_results
            .iter()
            .map(FfiWasmValue::to_wasm_value)
            .collect();

        Ok(results)
    }

    fn clone_callback(&self) -> Box<dyn HostFunctionCallback> {
        Box::new(PanamaHostFunctionCallbackImpl {
            callback_fn: self.callback_fn,
            callback_id: self.callback_id,
            result_count: self.result_count,
        })
    }
}

unsafe impl Send for PanamaHostFunctionCallbackImpl {}
unsafe impl Sync for PanamaHostFunctionCallbackImpl {}

/// Create a new Wasmtime linker (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_create(engine_ptr: *mut c_void) -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };
        linker_core::create_linker(engine)
    })
}

/// Define a host function in the linker (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_define_host_function(
    linker_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    param_types: *const c_int,
    param_count: c_uint,
    return_types: *const c_int,
    return_count: c_uint,
    callback_fn: PanamaHostFunctionCallback,
    callback_id: i64,
) -> c_int {
    if linker_ptr.is_null() || module_name.is_null() || name.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        // Convert C strings
        let module_name_str = unsafe { CStr::from_ptr(module_name) }
            .to_str()
            .map_err(|e| crate::error::WasmtimeError::Utf8Error {
                message: e.to_string(),
            })?;
        let name_str = unsafe { CStr::from_ptr(name) }.to_str().map_err(|e| {
            crate::error::WasmtimeError::Utf8Error {
                message: e.to_string(),
            }
        })?;

        // Convert parameter types
        let param_slice = unsafe { std::slice::from_raw_parts(param_types, param_count as usize) };
        let param_val_types: Vec<ValType> = param_slice
            .iter()
            .map(|&t| int_to_valtype(t))
            .collect::<Result<Vec<_>, _>>()?;

        // Convert return types
        let return_slice =
            unsafe { std::slice::from_raw_parts(return_types, return_count as usize) };
        let return_val_types: Vec<ValType> = return_slice
            .iter()
            .map(|&t| int_to_valtype(t))
            .collect::<Result<Vec<_>, _>>()?;

        // Store result count before moving return_val_types
        let result_count = return_val_types.len();

        // Get linker
        let linker = unsafe { linker_core::get_linker_mut(linker_ptr)? };

        // Get engine from linker
        let linker_lock = linker.inner()?;
        let engine = linker_lock.engine();

        // Create function type
        let func_type = FuncType::new(engine, param_val_types, return_val_types);

        // Drop lock before creating host function
        drop(linker_lock);

        // Create Panama callback with result count from function type
        let callback = PanamaHostFunctionCallbackImpl {
            callback_fn,
            callback_id,
            result_count,
        };

        // Create host function
        let host_func = HostFunction::new(
            format!("{}::{}", module_name_str, name_str),
            func_type,
            Box::new(callback),
        )?;

        // Register host function
        let host_func_clone = (*host_func).clone();
        linker.define_host_function(
            module_name_str,
            name_str,
            host_func.func_type().clone(),
            host_func_clone,
        )?;

        Ok(())
    })
}

/// Create an alias for an export (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_alias(
    linker_ptr: *mut c_void,
    from_module: *const c_char,
    from_name: *const c_char,
    to_module: *const c_char,
    to_name: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Validate pointers
            if linker_ptr.is_null()
                || from_module.is_null()
                || from_name.is_null()
                || to_module.is_null()
                || to_name.is_null()
            {
                return Err(crate::error::WasmtimeError::Linker {
                    message: "Null pointer in linker alias parameters".to_string(),
                });
            }

            // Convert C strings to Rust strings
            let from_module_str = CStr::from_ptr(from_module).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;
            let from_name_str = CStr::from_ptr(from_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;
            let to_module_str = CStr::from_ptr(to_module).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;
            let to_name_str = CStr::from_ptr(to_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;

            // Get linker reference
            let linker = linker_core::get_linker_ref(linker_ptr)?;

            // Lock linker
            let mut linker_lock = linker.inner()?;

            // Use Wasmtime's alias method
            linker_lock
                .alias(from_module_str, from_name_str, to_module_str, to_name_str)
                .map_err(|e| crate::error::WasmtimeError::Linker {
                    message: format!(
                        "Failed to create alias from {}::{} to {}::{}: {}",
                        from_module_str, from_name_str, to_module_str, to_name_str, e
                    ),
                })?;

            Ok(())
        }
    })
}

/// Define unknown imports as traps (Panama FFI version)
///
/// Implements any function imports of the module that are not already defined
/// with functions that trap when called.
///
/// # Arguments
/// * `linker_ptr` - Pointer to the linker
/// * `store_ptr` - Pointer to the store (used to flush pending host functions)
/// * `module_ptr` - Pointer to the module
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_define_unknown_imports_as_traps(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    module_ptr: *const c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Get mutable linker reference
            let linker = linker_core::get_linker_mut(linker_ptr)?;

            // Flush pending host functions to wasmtime linker before defining traps
            let store = crate::store::core::get_store_mut(store_ptr)?;
            {
                let mut store_lock = store.try_lock_store()?;
                linker.instantiate_host_functions(&mut *store_lock)?;
            }

            // Get module reference and clone the inner wasmtime module
            let module = crate::module::core::get_module_ref(module_ptr)?;
            let wasmtime_module = module.inner().clone();

            // Call the method with the cloned wasmtime module
            linker.define_unknown_imports_as_traps_wasmtime(&wasmtime_module)?;

            Ok(())
        }
    })
}

/// Define unknown imports as default values (Panama FFI version)
///
/// Implements any function imports of the module that are not already defined
/// with functions that return default values (zero for numbers, null for references).
///
/// # Arguments
/// * `linker_ptr` - Pointer to the linker
/// * `store_ptr` - Pointer to the store
/// * `module_ptr` - Pointer to the module
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_define_unknown_imports_as_default_values(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    module_ptr: *const c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Get mutable linker reference
            let linker = linker_core::get_linker_mut(linker_ptr)?;

            // Get store reference
            let store = crate::store::core::get_store_mut(store_ptr)?;

            // Flush pending host functions to wasmtime linker before defining defaults
            {
                let mut store_lock = store.try_lock_store()?;
                linker.instantiate_host_functions(&mut *store_lock)?;
            }

            // Get module reference
            let module = crate::module::core::get_module_ref(module_ptr)?;

            // Lock store and call the method
            let mut store_lock = store.try_lock_store()?;
            linker.define_unknown_imports_as_default_values(&mut *store_lock, module)?;

            Ok(())
        }
    })
}

/// Destroy a linker (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_destroy(linker_ptr: *mut c_void) {
    if linker_ptr.is_null() {
        return;
    }

    unsafe {
        linker_core::destroy_linker(linker_ptr);
    }
}

/// Set allow shadowing on a linker (Panama FFI version)
///
/// When enabled, allows later definitions to shadow earlier ones.
///
/// # Arguments
/// * `linker_ptr` - Pointer to the linker
/// * `allow` - 1 to allow shadowing, 0 to disallow
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_allow_shadowing(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| unsafe {
        let linker = linker_core::get_linker_mut(linker_ptr)?;
        linker.set_allow_shadowing(allow != 0)?;
        Ok(())
    })
}

/// Set allow unknown exports on a linker (Panama FFI version)
///
/// When enabled, allows modules to have exports that are not defined in the linker.
///
/// # Arguments
/// * `linker_ptr` - Pointer to the linker
/// * `allow` - 1 to allow unknown exports, 0 to disallow
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_allow_unknown_exports(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| unsafe {
        let linker = linker_core::get_linker_mut(linker_ptr)?;
        linker.set_allow_unknown_exports(allow != 0)?;
        Ok(())
    })
}

/// Define a global in the linker (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_define_global(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    global_ptr: *mut c_void,
) -> c_int {
    use wasmtime::AsContextMut;

    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Convert C strings to Rust strings
            let module_name_str = CStr::from_ptr(module_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;
            let name_str = CStr::from_ptr(name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;

            // Get linker reference
            let linker = linker_core::get_linker_ref(linker_ptr)?;

            // Get store reference
            let store = crate::store::core::get_store_mut(store_ptr)?;

            // Get global reference
            let global = crate::global::core::get_global_ref(global_ptr)?;

            // Lock linker and global
            let mut linker_lock = linker.inner()?;
            let wasmtime_global_arc = global.wasmtime_global();
            let wasmtime_global_lock = wasmtime_global_arc.lock().map_err(|e| {
                crate::error::WasmtimeError::Concurrency {
                    message: format!("Failed to lock global: {}", e),
                }
            })?;

            // Lock store and define global
            let mut store_lock = store.try_lock_store()?;
            linker_lock
                .define(
                    &mut (*store_lock).as_context_mut(),
                    module_name_str,
                    name_str,
                    wasmtime::Extern::Global(*wasmtime_global_lock),
                )
                .map_err(|e| crate::error::WasmtimeError::Linker {
                    message: format!(
                        "Failed to define global '{}::{}': {}",
                        module_name_str, name_str, e
                    ),
                })?;

            Ok(())
        }
    })
}

/// Define a table in the linker (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_define_table(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    table_ptr: *mut c_void,
) -> c_int {
    use wasmtime::AsContextMut;

    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Convert C strings to Rust strings
            let module_name_str = CStr::from_ptr(module_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;
            let name_str = CStr::from_ptr(name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;

            // Get linker reference
            let linker = linker_core::get_linker_ref(linker_ptr)?;

            // Get store reference
            let store = crate::store::core::get_store_mut(store_ptr)?;

            // Get table reference
            let table = crate::table::core::get_table_ref(table_ptr)?;

            // Lock linker and table
            let mut linker_lock = linker.inner()?;
            let wasmtime_table_arc = table.wasmtime_table();
            let wasmtime_table_lock = wasmtime_table_arc.lock().map_err(|e| {
                crate::error::WasmtimeError::Concurrency {
                    message: format!("Failed to lock table: {}", e),
                }
            })?;

            // Lock store and define table
            let mut store_lock = store.try_lock_store()?;
            linker_lock
                .define(
                    &mut (*store_lock).as_context_mut(),
                    module_name_str,
                    name_str,
                    wasmtime::Extern::Table(*wasmtime_table_lock),
                )
                .map_err(|e| crate::error::WasmtimeError::Linker {
                    message: format!(
                        "Failed to define table '{}::{}': {}",
                        module_name_str, name_str, e
                    ),
                })?;

            Ok(())
        }
    })
}

/// Define a memory in the linker (Panama FFI version)
///
/// This function defines a memory in the linker so it can be used by other modules
/// as an import.
///
/// # Arguments
/// * `linker_ptr` - Pointer to the linker
/// * `store_ptr` - Pointer to the store
/// * `module_name` - Module name (C string)
/// * `name` - Memory name (C string)
/// * `memory_ptr` - Pointer to a raw wasmtime::Memory (as returned by instanceGetMemoryByName)
///
/// # Returns
/// 0 on success, negative error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_define_memory(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    memory_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Validate pointers
            if linker_ptr.is_null() || store_ptr.is_null() || memory_ptr.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Null pointer passed to linker_define_memory".to_string(),
                });
            }

            // Convert C strings to Rust strings
            let module_name_str = CStr::from_ptr(module_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;
            let name_str = CStr::from_ptr(name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;

            // Get linker reference
            let linker = linker_core::get_linker_ref(linker_ptr)?;

            // Get store reference
            let store = crate::store::core::get_store_mut(store_ptr)?;

            // The memory_ptr is a pointer to our Memory wrapper (from instanceGetMemoryByName)
            let memory =
                crate::memory::core::get_memory_ref(memory_ptr as *const std::ffi::c_void)?;

            // Lock linker
            let mut linker_lock = linker.inner()?;

            // Handle both regular and shared memory - exact same pattern as JNI
            store.with_context(|ctx| {
                let extern_memory = if let Some(wasmtime_memory) = memory.inner() {
                    wasmtime::Extern::Memory(*wasmtime_memory)
                } else if let Some(wasmtime_shared_memory) = memory.inner_shared() {
                    wasmtime::Extern::SharedMemory(wasmtime_shared_memory.clone())
                } else {
                    return Err(crate::error::WasmtimeError::Linker {
                        message: format!(
                            "Memory '{}::{}' has invalid variant",
                            module_name_str, name_str
                        ),
                    });
                };

                linker_lock
                    .define(ctx, module_name_str, name_str, extern_memory)
                    .map_err(|e| crate::error::WasmtimeError::Linker {
                        message: format!(
                            "Failed to define memory '{}::{}': {}",
                            module_name_str, name_str, e
                        ),
                    })
            })?;

            Ok(())
        }
    })
}

/// Define a memory from an instance in the linker (Panama FFI version)
///
/// This variant takes the instance pointer and memory name, extracting the memory
/// and defining it in the linker all within the same store context to avoid
/// store mismatch issues.
///
/// # Parameters
/// * `linker_ptr` - Pointer to Linker wrapper
/// * `store_ptr` - Pointer to Store wrapper
/// * `module_name` - Module name for the memory definition
/// * `memory_name` - Name for the memory definition
/// * `instance_ptr` - Pointer to Instance wrapper containing the memory
/// * `export_name` - Name of the exported memory in the instance
///
/// # Returns
/// 0 on success, negative error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_define_memory_from_instance(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    module_name: *const c_char,
    memory_name: *const c_char,
    instance_ptr: *mut c_void,
    export_name: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Validate pointers
            if linker_ptr.is_null() || store_ptr.is_null() || instance_ptr.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Null pointer passed to linker_define_memory_from_instance"
                        .to_string(),
                });
            }

            // Convert C strings to Rust strings
            let module_name_str = CStr::from_ptr(module_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;
            let memory_name_str = CStr::from_ptr(memory_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;
            let export_name_str = CStr::from_ptr(export_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;

            // Get linker reference
            let linker = linker_core::get_linker_ref(linker_ptr)?;

            // Get store reference
            let store = crate::store::core::get_store_mut(store_ptr)?;

            // Get instance reference
            let instance = crate::instance::core::get_instance_ref(instance_ptr)?;

            // Get the linker lock
            let mut linker_lock = linker.inner()?;

            // Try to get shared memory first (for modules with threads proposal).
            // SharedMemory is store-independent, so once obtained it can be defined
            // in any linker. The caller must pass the store that owns the instance
            // to successfully extract the SharedMemory from the instance's exports.
            if let Some(shared_memory) = instance.get_shared_memory(store, export_name_str)? {
                // SharedMemory is store-independent; define it in the linker
                store.with_context(|ctx| {
                    linker_lock
                        .define(
                            ctx,
                            module_name_str,
                            memory_name_str,
                            wasmtime::Extern::SharedMemory(shared_memory),
                        )
                        .map_err(|e| crate::error::WasmtimeError::Linker {
                            message: format!(
                                "Failed to define shared memory '{}::{}': {}",
                                module_name_str, memory_name_str, e
                            ),
                        })
                })?;
            } else if let Some(memory) = instance.get_memory(store, export_name_str)? {
                // Regular memory - use the standard approach
                store.with_context(|ctx| {
                    linker_lock
                        .define(
                            ctx,
                            module_name_str,
                            memory_name_str,
                            wasmtime::Extern::Memory(memory),
                        )
                        .map_err(|e| crate::error::WasmtimeError::Linker {
                            message: format!(
                                "Failed to define memory '{}::{}': {}",
                                module_name_str, memory_name_str, e
                            ),
                        })
                })?;
            } else {
                return Err(crate::error::WasmtimeError::Linker {
                    message: format!(
                        "Memory '{}' not found in instance (neither shared nor regular)",
                        export_name_str
                    ),
                });
            }

            Ok(())
        }
    })
}

/// Define an instance in the linker (register all its exports) (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_linker_define_instance(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    module_name: *const c_char,
    instance_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Convert C string to Rust string
            let module_name_str = CStr::from_ptr(module_name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?;

            // Get linker reference
            let linker = linker_core::get_linker_ref(linker_ptr)?;

            // Get store reference
            let store = crate::store::core::get_store_mut(store_ptr)?;

            // Get instance reference
            let instance = crate::instance::core::get_instance_ref(instance_ptr)?;

            // Get the wasmtime instance from our wrapper
            let wasmtime_instance = {
                let wasmtime_instance_guard = instance.inner().lock();
                *wasmtime_instance_guard
            };

            // Get the linker lock
            let mut linker_lock = linker.inner()?;

            // Use with_context to let the store manage its own locking
            store.with_context(|ctx| {
                linker_lock
                    .instance(ctx, module_name_str, wasmtime_instance)
                    .map_err(|e| crate::error::WasmtimeError::Linker {
                        message: format!("Failed to define instance '{}': {}", module_name_str, e),
                    })
            })?;

            Ok(())
        }
    })
}

// ============================================================================
// Function Reference FFI (Panama)
// ============================================================================

/// Create a function reference from a host function (Panama FFI version)
///
/// This creates a new function reference that can be passed as a funcref value
/// to WebAssembly functions or stored in tables/globals.
///
/// # Parameters
/// - store_ptr: Pointer to the Store
/// - param_types: Array of parameter type codes (0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FuncRef, 6=ExternRef)
/// - param_count: Number of parameters
/// - return_types: Array of return type codes
/// - return_count: Number of return values
/// - callback_fn: Panama callback function pointer for host function invocation
/// - callback_id: Unique identifier for the callback (used by Java side)
/// - result_out: Output pointer for the function reference registry ID
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_function_reference_create(
    store_ptr: *mut c_void,
    param_types: *const c_int,
    param_count: c_uint,
    return_types: *const c_int,
    return_count: c_uint,
    callback_fn: PanamaHostFunctionCallback,
    callback_id: i64,
    result_out: *mut u64,
) -> c_int {
    if store_ptr.is_null() || result_out.is_null() {
        return -1;
    }

    ffi_utils::ffi_try_code(|| {
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

        // Build function type from parameter and return type arrays
        let param_val_types: Vec<ValType> = if param_count > 0 && !param_types.is_null() {
            let param_slice =
                unsafe { std::slice::from_raw_parts(param_types, param_count as usize) };
            param_slice
                .iter()
                .map(|&t| int_to_valtype(t))
                .collect::<Result<Vec<_>, _>>()?
        } else {
            Vec::new()
        };

        let return_val_types: Vec<ValType> = if return_count > 0 && !return_types.is_null() {
            let return_slice =
                unsafe { std::slice::from_raw_parts(return_types, return_count as usize) };
            return_slice
                .iter()
                .map(|&t| int_to_valtype(t))
                .collect::<Result<Vec<_>, _>>()?
        } else {
            Vec::new()
        };

        let result_count = return_val_types.len();

        // Get wasmtime engine from store to create FuncType
        let wasmtime_engine = store.engine().inner();
        let func_type = FuncType::new(wasmtime_engine, param_val_types, return_val_types);

        // Create Panama callback wrapper
        let callback = PanamaHostFunctionCallbackImpl {
            callback_fn,
            callback_id,
            result_count,
        };

        // Create function reference using store
        let name = format!("funcref_{}", callback_id);
        let registry_id = store.create_function_reference(name, func_type, Box::new(callback))?;

        unsafe {
            *result_out = registry_id;
        }

        Ok(())
    })
}

/// Destroy a function reference (Panama FFI version)
///
/// This releases the resources associated with a function reference.
/// After calling this, the registry ID is no longer valid.
///
/// # Parameters
/// - registry_id: The function reference registry ID returned from create
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_function_reference_destroy(registry_id: u64) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Remove the function reference from the table registry
        crate::table::core::remove_function_reference(registry_id)?;
        Ok(())
    })
}

/// Check if a function reference is valid (Panama FFI version)
///
/// # Parameters
/// - registry_id: The function reference registry ID
///
/// # Returns
/// 1 if valid, 0 if invalid
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_function_reference_is_valid(registry_id: u64) -> c_int {
    match crate::table::core::get_function_reference(registry_id) {
        Ok(Some(_)) => 1,
        _ => 0,
    }
}

/// Helper: Convert int to ValType
fn int_to_valtype(val: c_int) -> crate::WasmtimeResult<ValType> {
    match val {
        0 => Ok(ValType::I32),
        1 => Ok(ValType::I64),
        2 => Ok(ValType::F32),
        3 => Ok(ValType::F64),
        4 => Ok(ValType::V128),
        5 => Ok(ValType::Ref(RefType::FUNCREF)),
        6 => Ok(ValType::Ref(RefType::EXTERNREF)),
        _ => Err(crate::error::WasmtimeError::Type {
            message: format!("Unknown value type: {}", val),
        }),
    }
}
