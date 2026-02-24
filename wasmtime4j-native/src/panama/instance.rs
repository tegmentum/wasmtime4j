//! Panama FFI bindings for WebAssembly instance operations.
//!
//! This module provides C-compatible functions for instantiating WebAssembly
//! modules, invoking exported functions, and managing runtime execution state.

use std::os::raw::{c_char, c_int, c_void};

use crate::error::ffi_utils;

/// Instantiate a WebAssembly module (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_create(
    store_ptr: *mut c_void,
    module_ptr: *mut c_void,
    instance_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };
        let module = unsafe { crate::module::core::get_module_ref(module_ptr)? };

        let instance = crate::instance::core::create_instance(store, module)?;

        unsafe {
            // SAFETY IMPROVEMENT: Using safe Box management utilities
            *instance_ptr =
                crate::ffi_common::memory_utils::box_into_raw_safe(instance) as *mut c_void;
        }

        Ok(())
    })
}

/// Create a WebAssembly instance with explicit imports (Panama FFI version)
///
/// extern_ptrs: array of void* pointers to extern handles
/// extern_types: array of i32 type discriminators (0=Func, 1=Global, 2=Table, 3=Memory, 4=SharedMem)
/// count: number of imports
/// instance_ptr: output pointer for the created instance
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_create_with_imports(
    store_ptr: *mut c_void,
    module_ptr: *mut c_void,
    extern_ptrs: *const *const c_void,
    extern_types: *const c_int,
    count: c_int,
    instance_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };
        let module = unsafe { crate::module::core::get_module_ref(module_ptr)? };

        let count = count as usize;

        if count == 0 {
            let instance = crate::instance::core::create_instance(store, module)?;
            unsafe {
                *instance_ptr =
                    crate::ffi_common::memory_utils::box_into_raw_safe(instance) as *mut c_void;
            }
            return Ok(());
        }

        if extern_ptrs.is_null() || extern_types.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "extern_ptrs and extern_types must not be null when count > 0".to_string(),
            });
        }

        let ptrs = unsafe { std::slice::from_raw_parts(extern_ptrs, count) };
        let types = unsafe { std::slice::from_raw_parts(extern_types, count) };

        let instance = unsafe {
            crate::instance::core::create_instance_from_extern_handles(store, module, ptrs, types)?
        };

        unsafe {
            *instance_ptr =
                crate::ffi_common::memory_utils::box_into_raw_safe(instance) as *mut c_void;
        }

        Ok(())
    })
}

/// Get export by ModuleExport handle (Panama FFI version)
///
/// Returns the extern handle via out_handle, and the extern type via out_type.
/// Returns 0 on success, negative on error. If not found, out_handle is null and out_type is -1.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_module_export(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    module_export_ptr: *const c_void,
    out_handle: *mut *mut c_void,
    out_type: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let (handle, typ) = unsafe {
            crate::instance::core::get_export_by_module_export(instance, store, module_export_ptr)?
        };

        unsafe {
            *out_handle = handle;
            *out_type = typ;
        }

        Ok(())
    })
}

/// Destroy a WebAssembly instance (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_destroy(instance_ptr: *mut c_void) {
    unsafe {
        crate::instance::core::destroy_instance(instance_ptr);
    }
}

/// Get exported memory by name (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_memory_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }
    unsafe {
        crate::instance::wasmtime4j_instance_get_memory_by_name(instance_ptr, store_ptr, name)
    }
}

/// Get exported shared memory by name (Panama FFI)
///
/// Unlike `wasmtime4j_panama_instance_get_memory_by_name` which returns regular or shared memory,
/// this ONLY returns shared memory exports.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_shared_memory_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }

    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    let result = (|| -> Result<*mut c_void, crate::error::WasmtimeError> {
        let instance =
            unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store =
            unsafe { crate::store::core::get_store_mut(store_ptr)? };

        match instance.get_shared_memory(store, name_str)? {
            Some(shared_memory) => {
                let memory_wrapper = crate::memory::Memory::from_shared_memory(shared_memory);
                let validated_ptr = crate::memory::core::create_validated_memory(memory_wrapper)?;
                Ok(validated_ptr as *mut c_void)
            }
            None => Ok(std::ptr::null_mut()),
        }
    })();

    match result {
        Ok(ptr) => ptr,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get exported table by name (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_table_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }
    unsafe { crate::instance::wasmtime4j_instance_get_table_by_name(instance_ptr, store_ptr, name) }
}

/// Get exported global by name (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_global_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }
    unsafe {
        crate::instance::wasmtime4j_instance_get_global_by_name(instance_ptr, store_ptr, name)
    }
}

/// Get exported global by name, properly wrapped for linker use (Panama FFI)
///
/// Unlike `wasmtime4j_panama_instance_get_global_by_name` which returns a raw wasmtime::Global,
/// this function wraps the global in the proper Global struct that can be used with
/// `wasmtime4j_panama_linker_define_global`.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_global_wrapped(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    use crate::global::Global;

    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }

    ffi_utils::ffi_try_ptr(|| {
        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name).to_str().map_err(|e| {
                crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                }
            })?
        };

        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        // Get the raw wasmtime global from the instance
        let wasmtime_global = crate::instance::core::get_exported_global(
            instance, store, name_str,
        )?
        .ok_or_else(|| crate::error::WasmtimeError::ExportNotFound {
            name: name_str.to_string(),
        })?;

        // Wrap it in our Global struct so it can be used with the linker
        let wrapped_global =
            Global::from_wasmtime_global(wasmtime_global, store, Some(name_str.to_string()))?;

        Ok(Box::new(wrapped_global))
    })
}
