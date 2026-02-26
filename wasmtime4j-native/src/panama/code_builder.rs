//! Panama FFI bindings for CodeBuilder operations

use std::os::raw::{c_int, c_void};

use crate::code_builder;
use crate::engine::Engine;
use crate::error::ffi_utils;

/// Create a new CodeBuilder for the given engine handle.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_create(
    engine_ptr: *const c_void,
) -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let engine = unsafe { &*(engine_ptr as *const Engine) };
        code_builder::code_builder_new(engine)
    })
}

/// Set wasm binary bytes on the builder.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_wasm_binary(
    builder_ptr: *mut c_void,
    bytes_ptr: *const u8,
    bytes_len: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder = unsafe { &mut *(builder_ptr as *mut code_builder::CodeBuilderState) };
        let bytes = unsafe { std::slice::from_raw_parts(bytes_ptr, bytes_len as usize) };
        code_builder::code_builder_wasm_binary(builder, bytes.to_vec())
    })
}

/// Set wasm binary or text bytes on the builder.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_wasm_binary_or_text(
    builder_ptr: *mut c_void,
    bytes_ptr: *const u8,
    bytes_len: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder = unsafe { &mut *(builder_ptr as *mut code_builder::CodeBuilderState) };
        let bytes = unsafe { std::slice::from_raw_parts(bytes_ptr, bytes_len as usize) };
        code_builder::code_builder_wasm_binary_or_text(builder, bytes.to_vec())
    })
}

/// Set DWARF package bytes on the builder.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_dwarf_package(
    builder_ptr: *mut c_void,
    bytes_ptr: *const u8,
    bytes_len: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder = unsafe { &mut *(builder_ptr as *mut code_builder::CodeBuilderState) };
        let bytes = unsafe { std::slice::from_raw_parts(bytes_ptr, bytes_len as usize) };
        code_builder::code_builder_dwarf_package(builder, bytes.to_vec())
    })
}

/// Set hint on the builder.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_hint(
    builder_ptr: *mut c_void,
    hint_ordinal: c_int,
) {
    let builder = unsafe { &mut *(builder_ptr as *mut code_builder::CodeBuilderState) };
    code_builder::code_builder_hint(builder, hint_ordinal);
}

/// Compile module from builder. Returns module handle.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_compile_module(
    builder_ptr: *const c_void,
) -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let builder = unsafe { &*(builder_ptr as *const code_builder::CodeBuilderState) };
        code_builder::code_builder_compile_module(builder)
    })
}

/// Compile module serialized from builder.
/// Writes serialized bytes to out_data/out_len. Returns 0 on success, -1 on failure.
/// The caller is responsible for freeing the output data with wasmtime4j_panama_free_bytes.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_compile_module_serialized(
    builder_ptr: *const c_void,
    out_data: *mut *mut u8,
    out_len: *mut usize,
) -> c_int {
    if builder_ptr.is_null() || out_data.is_null() || out_len.is_null() {
        return -1;
    }

    let builder = unsafe { &*(builder_ptr as *const code_builder::CodeBuilderState) };
    match code_builder::code_builder_compile_module_serialized(builder) {
        Ok(data) => {
            let len = data.len();
            let ptr = Box::into_raw(data.into_boxed_slice()) as *mut u8;
            unsafe {
                *out_data = ptr;
                *out_len = len;
            }
            0
        }
        Err(e) => {
            log::error!("Failed to compile module serialized: {:?}", e);
            -1
        }
    }
}

/// Compile component from builder. Returns component handle.
#[no_mangle]
#[cfg(feature = "component-model")]
pub extern "C" fn wasmtime4j_panama_code_builder_compile_component(
    builder_ptr: *const c_void,
) -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let builder = unsafe { &*(builder_ptr as *const code_builder::CodeBuilderState) };
        code_builder::code_builder_compile_component(builder)
    })
}

/// Compile component serialized from builder.
/// Writes serialized bytes to out_data/out_len. Returns 0 on success, -1 on failure.
/// The caller is responsible for freeing the output data with wasmtime4j_panama_free_bytes.
#[no_mangle]
#[cfg(feature = "component-model")]
pub extern "C" fn wasmtime4j_panama_code_builder_compile_component_serialized(
    builder_ptr: *const c_void,
    out_data: *mut *mut u8,
    out_len: *mut usize,
) -> c_int {
    if builder_ptr.is_null() || out_data.is_null() || out_len.is_null() {
        return -1;
    }

    let builder = unsafe { &*(builder_ptr as *const code_builder::CodeBuilderState) };
    match code_builder::code_builder_compile_component_serialized(builder) {
        Ok(data) => {
            let len = data.len();
            let ptr = Box::into_raw(data.into_boxed_slice()) as *mut u8;
            unsafe {
                *out_data = ptr;
                *out_len = len;
            }
            0
        }
        Err(e) => {
            log::error!("Failed to compile component serialized: {:?}", e);
            -1
        }
    }
}

/// Add compile-time builtins from binary bytes.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_compile_time_builtins_binary(
    builder_ptr: *mut c_void,
    name_ptr: *const u8,
    name_len: c_int,
    wasm_ptr: *const u8,
    wasm_len: c_int,
) {
    if builder_ptr.is_null() || name_ptr.is_null() || wasm_ptr.is_null() {
        return;
    }
    let builder = unsafe { &mut *(builder_ptr as *mut code_builder::CodeBuilderState) };
    let name_bytes = unsafe { std::slice::from_raw_parts(name_ptr, name_len as usize) };
    let name = match std::str::from_utf8(name_bytes) {
        Ok(s) => s.to_string(),
        Err(_) => return,
    };
    let wasm_bytes = unsafe { std::slice::from_raw_parts(wasm_ptr, wasm_len as usize) };
    code_builder::code_builder_compile_time_builtins_binary(builder, name, wasm_bytes.to_vec());
}

/// Add compile-time builtins from binary or text bytes.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_compile_time_builtins_binary_or_text(
    builder_ptr: *mut c_void,
    name_ptr: *const u8,
    name_len: c_int,
    wasm_ptr: *const u8,
    wasm_len: c_int,
) {
    if builder_ptr.is_null() || name_ptr.is_null() || wasm_ptr.is_null() {
        return;
    }
    let builder = unsafe { &mut *(builder_ptr as *mut code_builder::CodeBuilderState) };
    let name_bytes = unsafe { std::slice::from_raw_parts(name_ptr, name_len as usize) };
    let name = match std::str::from_utf8(name_bytes) {
        Ok(s) => s.to_string(),
        Err(_) => return,
    };
    let wasm_bytes = unsafe { std::slice::from_raw_parts(wasm_ptr, wasm_len as usize) };
    code_builder::code_builder_compile_time_builtins_binary_or_text(builder, name, wasm_bytes.to_vec());
}

/// Set expose unsafe intrinsics import name on the builder.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_expose_unsafe_intrinsics(
    builder_ptr: *mut c_void,
    name_ptr: *const u8,
    name_len: c_int,
) {
    if builder_ptr.is_null() || name_ptr.is_null() {
        return;
    }
    let builder = unsafe { &mut *(builder_ptr as *mut code_builder::CodeBuilderState) };
    let name_bytes = unsafe { std::slice::from_raw_parts(name_ptr, name_len as usize) };
    let name = match std::str::from_utf8(name_bytes) {
        Ok(s) => s.to_string(),
        Err(_) => return,
    };
    code_builder::code_builder_expose_unsafe_intrinsics(builder, name);
}

/// Destroy the code builder.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_code_builder_destroy(builder_ptr: *mut c_void) {
    if !builder_ptr.is_null() {
        let builder =
            unsafe { Box::from_raw(builder_ptr as *mut code_builder::CodeBuilderState) };
        code_builder::code_builder_destroy(builder);
    }
}
