//! Panama FFI bindings for WebAssembly module operations.
//!
//! This module provides C-compatible functions for compiling, validating,
//! and managing WebAssembly modules through the Panama Foreign Function Interface.

use std::os::raw::{c_char, c_int, c_void};

use crate::error::ffi_utils;
use crate::shared_ffi::module::{ByteArrayConverter, StringConverter};

/// Panama-specific byte array converter implementation
pub struct PanamaByteArrayConverter {
    data: *const u8,
    len: usize,
}

impl PanamaByteArrayConverter {
    /// Creates a new PanamaByteArrayConverter
    pub unsafe fn new(data: *const u8, len: usize) -> Self {
        Self { data, len }
    }
}

impl ByteArrayConverter for PanamaByteArrayConverter {
    unsafe fn get_bytes(&self) -> crate::error::WasmtimeResult<&[u8]> {
        if self.data.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Byte data pointer cannot be null".to_string(),
            });
        }

        if self.len == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Byte data length cannot be zero".to_string(),
            });
        }

        Ok(std::slice::from_raw_parts(self.data, self.len))
    }

    fn len(&self) -> usize {
        self.len
    }
}

/// Panama-specific string converter implementation
pub struct PanamaStringConverter {
    string_ptr: *const c_char,
}

impl PanamaStringConverter {
    /// Creates a new PanamaStringConverter
    pub unsafe fn new(string_ptr: *const c_char) -> Self {
        Self { string_ptr }
    }
}

impl StringConverter for PanamaStringConverter {
    unsafe fn get_string(&self) -> crate::error::WasmtimeResult<String> {
        if self.string_ptr.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "String pointer cannot be null".to_string(),
            });
        }

        let c_str = std::ffi::CStr::from_ptr(self.string_ptr);
        let string = c_str.to_string_lossy().into_owned();
        Ok(string)
    }

    fn is_empty(&self) -> bool {
        if self.string_ptr.is_null() {
            true
        } else {
            unsafe {
                let c_str = std::ffi::CStr::from_ptr(self.string_ptr);
                c_str.to_bytes().is_empty()
            }
        }
    }
}

/// Compile a WebAssembly module (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_compile(
    engine_ptr: *mut c_void,
    wasm_bytes: *const u8,
    wasm_size: usize,
    module_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };
        let byte_converter = unsafe { PanamaByteArrayConverter::new(wasm_bytes, wasm_size) };

        let module = crate::shared_ffi::module::compile_module_shared(engine, byte_converter)?;

        unsafe {
            *module_ptr = Box::into_raw(module) as *mut c_void;
        }

        Ok(())
    })
}

/// Compile a WebAssembly module from WAT (WebAssembly Text format)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_compile_wat(
    engine_ptr: *mut c_void,
    wat_text: *const c_char,
    module_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };
        let string_converter = unsafe { PanamaStringConverter::new(wat_text) };

        let module =
            crate::shared_ffi::module::compile_module_wat_shared(engine, string_converter)?;

        unsafe {
            *module_ptr = Box::into_raw(module) as *mut c_void;
        }
        Ok(())
    })
}

/// Validate WebAssembly bytecode without compiling
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_validate(
    wasm_bytes: *const u8,
    wasm_size: usize,
) -> c_int {
    let byte_converter = unsafe { PanamaByteArrayConverter::new(wasm_bytes, wasm_size) };
    crate::shared_ffi::module::validation_result_to_ffi_code(
        crate::shared_ffi::module::validate_module_shared(byte_converter),
    )
}

/// Get module size in bytes
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_size(module_ptr: *mut c_void) -> usize {
    let result = crate::shared_ffi::module::get_module_size_shared(module_ptr);
    let (_, size) = crate::shared_ffi::module::size_result_to_ffi_result(result);
    size
}

/// Get module name (if available)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_name(module_ptr: *mut c_void) -> *mut c_char {
    match crate::shared_ffi::module::get_module_name_shared(module_ptr) {
        Ok(Some(name)) => match std::ffi::CString::new(name) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        _ => std::ptr::null_mut(),
    }
}

/// Get number of exports
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_export_count(module_ptr: *mut c_void) -> c_int {
    let result = crate::shared_ffi::module::get_export_count_shared(module_ptr);
    let (_, count) = crate::shared_ffi::module::count_result_to_ffi_result(result);
    count
}

/// Get number of imports
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_import_count(module_ptr: *mut c_void) -> c_int {
    let result = crate::shared_ffi::module::get_import_count_shared(module_ptr);
    let (_, count) = crate::shared_ffi::module::count_result_to_ffi_result(result);
    count
}

/// Get number of functions
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_function_count(module_ptr: *mut c_void) -> c_int {
    let result = crate::shared_ffi::module::get_function_count_shared(module_ptr);
    let (_, count) = crate::shared_ffi::module::count_result_to_ffi_result(result);
    count
}

/// Check if module has a specific export
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_has_export(
    module_ptr: *mut c_void,
    name: *const c_char,
) -> c_int {
    let string_converter = unsafe { PanamaStringConverter::new(name) };
    let result = crate::shared_ffi::module::has_export_shared(module_ptr, string_converter);
    let (_, has_export) = crate::shared_ffi::module::bool_result_to_ffi_result(result);
    if has_export {
        1
    } else {
        0
    }
}

/// Serialize a module for caching
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_serialize(
    module_ptr: *mut c_void,
    data_ptr: *mut *mut u8,
    len_ptr: *mut usize,
) -> c_int {
    match crate::shared_ffi::module::serialize_module_shared(module_ptr) {
        Ok(serialized) => {
            let len = serialized.len();
            let data = Box::into_raw(serialized.into_boxed_slice()) as *mut u8;

            unsafe {
                *data_ptr = data;
                *len_ptr = len;
            }

            crate::shared_ffi::FFI_SUCCESS
        }
        Err(_) => crate::shared_ffi::FFI_ERROR,
    }
}

/// Deserialize a module from cache
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_deserialize(
    engine_ptr: *mut c_void,
    data_ptr: *const u8,
    len: usize,
    module_ptr: *mut *mut c_void,
) -> c_int {
    match unsafe { crate::engine::core::get_engine_ref(engine_ptr) } {
        Ok(engine) => {
            let byte_converter = unsafe { PanamaByteArrayConverter::new(data_ptr, len) };
            match crate::shared_ffi::module::deserialize_module_shared(engine, byte_converter) {
                Ok(module) => {
                    unsafe {
                        *module_ptr = Box::into_raw(module) as *mut c_void;
                    }
                    crate::shared_ffi::FFI_SUCCESS
                }
                Err(_) => crate::shared_ffi::FFI_ERROR,
            }
        }
        Err(_) => crate::shared_ffi::FFI_ERROR,
    }
}

/// Deserialize a module from a file using memory-mapped I/O
///
/// This is more efficient than reading the file first for large modules.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_deserialize_file(
    engine_ptr: *mut c_void,
    path_ptr: *const c_char,
    module_ptr: *mut *mut c_void,
) -> c_int {
    if path_ptr.is_null() || module_ptr.is_null() {
        return crate::shared_ffi::FFI_ERROR;
    }

    // Convert C string to Rust string
    let path_cstr = unsafe { std::ffi::CStr::from_ptr(path_ptr) };
    let path_str = match path_cstr.to_str() {
        Ok(s) => s,
        Err(_) => return crate::shared_ffi::FFI_ERROR,
    };

    match unsafe { crate::engine::core::get_engine_ref(engine_ptr) } {
        Ok(engine) => {
            match crate::shared_ffi::module::deserialize_module_file_shared(engine, path_str) {
                Ok(module) => {
                    unsafe {
                        *module_ptr = Box::into_raw(module) as *mut c_void;
                    }
                    crate::shared_ffi::FFI_SUCCESS
                }
                Err(_) => crate::shared_ffi::FFI_ERROR,
            }
        }
        Err(_) => crate::shared_ffi::FFI_ERROR,
    }
}

/// Validate module functionality (defensive check)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_validate_functionality(
    module_ptr: *mut c_void,
) -> c_int {
    crate::shared_ffi::module::validation_result_to_ffi_code(
        crate::shared_ffi::module::validate_module_functionality_shared(module_ptr),
    )
}

/// Free serialized data
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_free_serialized_data(data_ptr: *mut u8, len: usize) {
    if !data_ptr.is_null() && len > 0 {
        unsafe {
            drop(Box::from_raw(std::slice::from_raw_parts_mut(data_ptr, len)));
        }
    }
}

/// Get module exports as JSON string (Panama FFI version)
/// Returns NULL on error or a JSON string containing export metadata
/// Caller must free the returned string with wasmtime4j_panama_module_free_string
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_exports_json(
    module_ptr: *mut c_void,
) -> *mut c_char {
    if module_ptr.is_null() {
        return std::ptr::null_mut();
    }

    // Get module and metadata
    let module = match unsafe { crate::module::core::get_module_ref(module_ptr) } {
        Ok(m) => m,
        Err(_) => return std::ptr::null_mut(),
    };
    let metadata = crate::module::core::get_metadata(module);

    // Serialize exports to JSON
    match serde_json::to_string(&metadata.exports) {
        Ok(json) => match std::ffi::CString::new(json) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get module imports as JSON string (Panama FFI version)
/// Returns NULL on error or a JSON string containing import metadata
/// Caller must free the returned string with wasmtime4j_panama_module_free_string
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_imports_json(
    module_ptr: *mut c_void,
) -> *mut c_char {
    if module_ptr.is_null() {
        return std::ptr::null_mut();
    }

    // Get module and metadata
    let module = match unsafe { crate::module::core::get_module_ref(module_ptr) } {
        Ok(m) => m,
        Err(_) => return std::ptr::null_mut(),
    };
    let metadata = crate::module::core::get_metadata(module);

    // Serialize imports to JSON
    match serde_json::to_string(&metadata.imports) {
        Ok(json) => match std::ffi::CString::new(json) {
            Ok(c_str) => c_str.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Free C string returned by module functions
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_free_string(str_ptr: *mut c_char) {
    if !str_ptr.is_null() {
        unsafe {
            drop(std::ffi::CString::from_raw(str_ptr));
        }
    }
}

/// Compile a WebAssembly module from a file path (Panama FFI version)
///
/// The file can contain either binary WebAssembly (.wasm) or WebAssembly Text (.wat).
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_compile_from_file(
    engine_ptr: *mut c_void,
    path_ptr: *const c_char,
    module_ptr: *mut *mut c_void,
) -> c_int {
    if path_ptr.is_null() || module_ptr.is_null() {
        return crate::shared_ffi::FFI_ERROR;
    }

    let path_cstr = unsafe { std::ffi::CStr::from_ptr(path_ptr) };
    let path_str = match path_cstr.to_str() {
        Ok(s) => s,
        Err(_) => return crate::shared_ffi::FFI_ERROR,
    };

    ffi_utils::ffi_try_code(|| {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };
        let module =
            crate::module::core::compile_module_from_file(engine, std::path::Path::new(path_str))?;
        unsafe {
            *module_ptr = Box::into_raw(module) as *mut c_void;
        }
        Ok(())
    })
}

/// Check if two modules are the same underlying compiled module (Panama FFI version)
///
/// Returns 1 if the modules are the same, 0 if not, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_same(
    module_ptr1: *mut c_void,
    module_ptr2: *mut c_void,
) -> c_int {
    if module_ptr1.is_null() || module_ptr2.is_null() {
        return 0;
    }
    match unsafe { crate::module::core::modules_same(module_ptr1, module_ptr2) } {
        Ok(same) => {
            if same {
                1
            } else {
                0
            }
        }
        Err(_) => 0,
    }
}

/// Get the index of an export by name (Panama FFI version)
///
/// Returns the zero-based index of the export, or -1 if not found.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_export_index(
    module_ptr: *mut c_void,
    name: *const c_char,
) -> c_int {
    if module_ptr.is_null() || name.is_null() {
        return -1;
    }

    let name_cstr = unsafe { std::ffi::CStr::from_ptr(name) };
    let name_str = match name_cstr.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    match unsafe { crate::module::core::get_export_index(module_ptr, name_str) } {
        Ok(idx) => idx,
        Err(_) => -1,
    }
}

/// Get a ModuleExport handle for O(1) export lookups (Panama FFI version)
///
/// Returns a pointer to a boxed ModuleExport via out_ptr. Returns 0 on success.
/// If the export is not found, out_ptr is set to null and 0 is still returned (not an error).
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_get_module_export(
    module_ptr: *mut c_void,
    name: *const c_char,
    out_ptr: *mut *mut c_void,
) -> c_int {
    if module_ptr.is_null() || name.is_null() || out_ptr.is_null() {
        return -1;
    }

    let name_cstr = unsafe { std::ffi::CStr::from_ptr(name) };
    let name_str = match name_cstr.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    match unsafe { crate::module::core::get_wasmtime_module_export(module_ptr, name_str) } {
        Ok(ptr) => {
            unsafe {
                *out_ptr = ptr;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Destroy a ModuleExport handle (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_export_destroy(module_export_ptr: *mut c_void) {
    unsafe {
        crate::module::core::destroy_module_export(module_export_ptr);
    }
}

/// Initialize copy-on-write image for faster instantiation (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_initialize_cow_image(
    module_ptr: *mut c_void,
) -> c_int {
    match crate::shared_ffi::module::initialize_copy_on_write_image_shared(module_ptr) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Get compiled machine code text from module (Panama FFI version)
///
/// Returns 0 on success, data_ptr and len_ptr are set.
/// Caller must free data_ptr with wasmtime4j_free_byte_array.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_text(
    module_ptr: *mut c_void,
    data_ptr: *mut *mut u8,
    len_ptr: *mut usize,
) -> c_int {
    if module_ptr.is_null() || data_ptr.is_null() || len_ptr.is_null() {
        return -1;
    }

    match unsafe { crate::module::core::get_module_ref(module_ptr) } {
        Ok(module) => {
            let text = crate::module::core::get_module_text(module);
            let len = text.len();
            let data = Box::into_raw(text.into_boxed_slice()) as *mut u8;
            unsafe {
                *data_ptr = data;
                *len_ptr = len;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get address map from module (Panama FFI version)
///
/// Returns 0 on success, 1 if address map not available, -1 on error.
/// code_offsets and wasm_offsets arrays must be freed with wasmtime4j_free_address_map.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_address_map(
    module_ptr: *mut c_void,
    code_offsets_out: *mut *mut u64,
    wasm_offsets_out: *mut *mut i64,
    count_out: *mut usize,
) -> c_int {
    if module_ptr.is_null()
        || code_offsets_out.is_null()
        || wasm_offsets_out.is_null()
        || count_out.is_null()
    {
        return -1;
    }

    match unsafe { crate::module::core::get_module_ref(module_ptr) } {
        Ok(module) => match crate::module::core::get_module_address_map(module) {
            Some(entries) => {
                let count = entries.len();
                let mut code_offsets = Vec::with_capacity(count);
                let mut wasm_offsets = Vec::with_capacity(count);

                for (code_offset, wasm_offset) in entries {
                    code_offsets.push(code_offset as u64);
                    wasm_offsets.push(wasm_offset.map(|o| o as i64).unwrap_or(-1));
                }

                unsafe {
                    *code_offsets_out =
                        Box::into_raw(code_offsets.into_boxed_slice()) as *mut u64;
                    *wasm_offsets_out =
                        Box::into_raw(wasm_offsets.into_boxed_slice()) as *mut i64;
                    *count_out = count;
                }
                0
            }
            None => {
                unsafe {
                    *code_offsets_out = std::ptr::null_mut();
                    *wasm_offsets_out = std::ptr::null_mut();
                    *count_out = 0;
                }
                1
            }
        },
        Err(_) => -1,
    }
}

/// Destroy a WebAssembly module (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_destroy(module_ptr: *mut c_void) {
    crate::shared_ffi::module::destroy_module_shared(module_ptr);
}
