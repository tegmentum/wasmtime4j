//! Panama Foreign Function Interface bindings for Java 23+

use std::os::raw::{c_char, c_int, c_long, c_uchar, c_uint, c_ulong, c_void};
use std::sync::Arc;
use crate::ffi_common::error_handling;
use crate::WasmtimeResult;
use crate::error::WasmtimeError;

/// Panama FFI bindings module
/// 
/// This module provides C-compatible functions for use by the wasmtime4j-panama module.
/// All functions use C calling conventions and handle memory management appropriately.

pub mod engine {
    use super::*;
    use crate::engine::core;
    use crate::error::ffi_utils;
    use crate::ffi_common::parameter_conversion;
    
    /// Create a new Wasmtime engine with default configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_create() -> *mut c_void {
        ffi_utils::ffi_try_ptr(|| core::create_engine())
    }

    /// Create a new Wasmtime engine with custom configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_create_with_config(
        strategy: c_int,
        opt_level: c_int,
        debug_info: c_int,
        wasm_threads: c_int,
        wasm_simd: c_int,
        wasm_reference_types: c_int,
        wasm_bulk_memory: c_int,
        wasm_multi_value: c_int,
        fuel_enabled: c_int,
        max_memory_pages: c_int,
        max_stack_size: c_int,
        epoch_interruption: c_int,
        max_instances: c_int,
    ) -> *mut c_void {
        
        
        ffi_utils::ffi_try_ptr(|| {
            let strategy_opt = parameter_conversion::convert_strategy(strategy);
            let opt_level_opt = parameter_conversion::convert_opt_level(opt_level);
            let max_memory_pages_opt = parameter_conversion::convert_int_to_optional_u32(max_memory_pages);
            let max_stack_size_opt = parameter_conversion::convert_int_to_optional_usize(max_stack_size);
            let max_instances_opt = parameter_conversion::convert_int_to_optional_u32(max_instances);
            
            core::create_engine_with_config(
                strategy_opt,
                opt_level_opt,
                parameter_conversion::convert_int_to_bool(debug_info),
                parameter_conversion::convert_int_to_bool(wasm_threads),
                parameter_conversion::convert_int_to_bool(wasm_simd),
                parameter_conversion::convert_int_to_bool(wasm_reference_types),
                parameter_conversion::convert_int_to_bool(wasm_bulk_memory),
                parameter_conversion::convert_int_to_bool(wasm_multi_value),
                parameter_conversion::convert_int_to_bool(fuel_enabled),
                max_memory_pages_opt,
                max_stack_size_opt,
                parameter_conversion::convert_int_to_bool(epoch_interruption),
                max_instances_opt,
                false,  // async_support - TODO: add Panama parameter
            )
        })
    }
    
    /// Destroy a Wasmtime engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_destroy(engine_ptr: *mut c_void) {
        unsafe {
            core::destroy_engine(engine_ptr);
        }
    }

    /// Check if fuel consumption is enabled (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_is_fuel_enabled(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if engine.fuel_enabled() { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Check if epoch interruption is enabled (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_is_epoch_interruption_enabled(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if engine.epoch_interruption_enabled() { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Check if coredump generation on trap is enabled (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_is_coredump_on_trap_enabled(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if engine.coredump_on_trap() { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Get memory limit in pages (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_get_memory_limit(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => engine.memory_limit_pages().map(|limit| limit as c_int).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Get stack size limit in bytes (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_get_stack_limit(engine_ptr: *mut c_void) -> c_long {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => engine.stack_size_limit().map(|limit| limit as c_long).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Get maximum instances limit (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_get_max_instances(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => core::get_max_instances(engine).map(|limit| limit as c_int).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Validate engine functionality (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_validate(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if core::validate_engine(engine).is_ok() { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Check if engine supports WebAssembly feature by name (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_supports_feature(
        engine_ptr: *mut c_void,
        feature_name: *const c_char,
    ) -> c_int {
        use crate::engine::WasmFeature;

        // Convert C string to Rust string
        let feature_str = match unsafe { std::ffi::CStr::from_ptr(feature_name) }.to_str() {
            Ok(s) => s,
            Err(_) => return -1,
        };

        // Parse feature name to WasmFeature enum
        let feature = match feature_str {
            "THREADS" => WasmFeature::Threads,
            "REFERENCE_TYPES" => WasmFeature::ReferenceTypes,
            "SIMD" => WasmFeature::Simd,
            "BULK_MEMORY" => WasmFeature::BulkMemory,
            "MULTI_VALUE" => WasmFeature::MultiValue,
            "TAIL_CALL" => WasmFeature::TailCall,
            "MULTI_MEMORY" => WasmFeature::MultiMemory,
            "MEMORY64" => WasmFeature::Memory64,
            "EXCEPTIONS" => WasmFeature::Exceptions,
            "RELAXED_SIMD" => WasmFeature::RelaxedSimd,
            "EXTENDED_CONST" => WasmFeature::ExtendedConst,
            "COMPONENT_MODEL" => WasmFeature::ComponentModel,
            "FUNCTION_REFERENCES" => WasmFeature::FunctionReferences,
            "GC" => WasmFeature::Gc,
            "CUSTOM_PAGE_SIZES" => WasmFeature::CustomPageSizes,
            "WIDE_ARITHMETIC" => WasmFeature::WideArithmetic,
            "STACK_SWITCHING" => WasmFeature::StackSwitching,
            "SHARED_EVERYTHING_THREADS" => WasmFeature::SharedEverythingThreads,
            "COMPONENT_MODEL_ASYNC" => WasmFeature::ComponentModelAsync,
            "COMPONENT_MODEL_ASYNC_BUILTINS" => WasmFeature::ComponentModelAsyncBuiltins,
            "COMPONENT_MODEL_ASYNC_STACKFUL" => WasmFeature::ComponentModelAsyncStackful,
            "COMPONENT_MODEL_ERROR_CONTEXT" => WasmFeature::ComponentModelErrorContext,
            "COMPONENT_MODEL_GC" => WasmFeature::ComponentModelGc,
            _ => return -1,
        };

        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if engine.supports_feature(feature) { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Get engine reference count for debugging (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_get_reference_count(engine_ptr: *mut c_void) -> c_int {
        // Demonstrate shared error handling utilities
        match error_handling::validate_void_pointer_as::<crate::engine::Engine>(engine_ptr, "engine") {
            Ok(_) => {
                // Now we can safely use the engine
                match unsafe { core::get_engine_ref(engine_ptr) } {
                    Ok(engine) => core::get_reference_count(engine) as c_int,
                    Err(e) => {
                        let error_info = error_handling::convert_internal_error(&e);
                        log::error!("Engine access failed: {}", error_info.message);
                        -1
                    }
                }
            },
            Err(validation_error) => {
                let error_info = error_handling::validation_error_to_info(validation_error);
                log::error!("Parameter validation failed: {}", error_info.message);
                -1
            }
        }
    }

    /// Increment the epoch counter (Panama FFI version)
    ///
    /// This function is signal-safe and performs only an atomic increment.
    /// The epoch counter is used for epoch-based interruption of WebAssembly execution.
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_increment_epoch(engine_ptr: *mut c_void) {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => {
                engine.increment_epoch();
            },
            Err(e) => {
                log::error!("Failed to increment epoch: {:?}", e);
            }
        }
    }

    /// Precompile a WebAssembly module for AOT (ahead-of-time) usage (Panama FFI version)
    ///
    /// Takes raw WebAssembly bytes and compiles them to a serialized form that can
    /// be loaded later via Module::deserialize without needing to recompile.
    ///
    /// Returns 0 on success, non-zero on failure.
    /// The caller is responsible for freeing the output data with wasmtime4j_panama_free_bytes.
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_engine_precompile_module(
        engine_ptr: *mut c_void,
        wasm_bytes: *const u8,
        wasm_len: usize,
        out_data: *mut *mut u8,
        out_len: *mut usize,
    ) -> c_int {
        if engine_ptr.is_null() || wasm_bytes.is_null() || out_data.is_null() || out_len.is_null() {
            return -1;
        }

        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => {
                let bytes = unsafe { std::slice::from_raw_parts(wasm_bytes, wasm_len) };
                match core::precompile_module(engine, bytes) {
                    Ok(precompiled) => {
                        let len = precompiled.len();
                        let data = Box::into_raw(precompiled.into_boxed_slice()) as *mut u8;
                        unsafe {
                            *out_data = data;
                            *out_len = len;
                        }
                        0  // Success
                    }
                    Err(e) => {
                        log::error!("Failed to precompile module: {:?}", e);
                        -1
                    }
                }
            }
            Err(e) => {
                log::error!("Invalid engine pointer: {:?}", e);
                -1
            }
        }
    }
}

/// Panama FFI bindings for WebAssembly module operations
/// 
/// This module provides C-compatible functions for compiling, validating,
/// and managing WebAssembly modules through the Panama Foreign Function Interface.
pub mod module {
    use super::*;
    
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
                    message: "Byte data pointer cannot be null".to_string()
                });
            }
            
            if self.len == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Byte data length cannot be zero".to_string()
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
                    message: "String pointer cannot be null".to_string()
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
            
            let module = crate::shared_ffi::module::compile_module_wat_shared(engine, string_converter)?;
            
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
            crate::shared_ffi::module::validate_module_shared(byte_converter)
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
            Ok(Some(name)) => {
                match std::ffi::CString::new(name) {
                    Ok(c_str) => c_str.into_raw(),
                    Err(_) => std::ptr::null_mut(),
                }
            }
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
        if has_export { 1 } else { 0 }
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
    pub extern "C" fn wasmtime4j_panama_module_validate_functionality(module_ptr: *mut c_void) -> c_int {
        crate::shared_ffi::module::validation_result_to_ffi_code(
            crate::shared_ffi::module::validate_module_functionality_shared(module_ptr)
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
    pub extern "C" fn wasmtime4j_panama_module_get_exports_json(module_ptr: *mut c_void) -> *mut c_char {
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
            Ok(json) => {
                match std::ffi::CString::new(json) {
                    Ok(c_str) => c_str.into_raw(),
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Get module imports as JSON string (Panama FFI version)
    /// Returns NULL on error or a JSON string containing import metadata
    /// Caller must free the returned string with wasmtime4j_panama_module_free_string
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_module_get_imports_json(module_ptr: *mut c_void) -> *mut c_char {
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
            Ok(json) => {
                match std::ffi::CString::new(json) {
                    Ok(c_str) => c_str.into_raw(),
                    Err(_) => std::ptr::null_mut(),
                }
            }
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

    /// Get custom sections from a WebAssembly module (Panama FFI version)
    /// Returns a JSON string mapping section names to Base64-encoded data
    /// Caller must free the returned string with wasmtime4j_panama_module_free_string
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_module_get_custom_sections(module_ptr: *mut c_void) -> *mut c_char {
        use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};

        if module_ptr.is_null() {
            return std::ptr::null_mut();
        }

        match unsafe { crate::module::core::get_module_ref(module_ptr) } {
            Ok(module) => {
                let metadata = module.metadata();

                // Build JSON object with custom sections
                let mut json_parts: Vec<String> = Vec::new();
                for (name, data) in &metadata.custom_sections {
                    let encoded = BASE64.encode(data);
                    // Escape JSON string values
                    let escaped_name = name.replace('\\', "\\\\").replace('"', "\\\"");
                    json_parts.push(format!(r#""{}":{}"#, escaped_name, serde_json::json!(encoded)));
                }

                let json = format!("{{{}}}", json_parts.join(","));

                match std::ffi::CString::new(json) {
                    Ok(c_str) => c_str.into_raw(),
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Destroy a WebAssembly module (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_module_destroy(module_ptr: *mut c_void) {
        crate::shared_ffi::module::destroy_module_shared(module_ptr);
    }
}

/// Panama FFI bindings for WebAssembly instance operations
/// 
/// This module provides C-compatible functions for instantiating WebAssembly
/// modules, invoking exported functions, and managing runtime execution state.
pub mod instance {
    use super::*;
    
    /// Instantiate a WebAssembly module (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_instance_create(
        store_ptr: *mut c_void,
        module_ptr: *mut c_void,
        instance_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::error::ffi_utils;
        
        ffi_utils::ffi_try_code(|| {
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };
            let module = unsafe { crate::module::core::get_module_ref(module_ptr)? };
            
            let instance = crate::instance::core::create_instance(store, module)?;
            
            unsafe {
                // SAFETY IMPROVEMENT: Using safe Box management utilities
                *instance_ptr = crate::ffi_common::memory_utils::box_into_raw_safe(instance) as *mut c_void;
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
}

/// Panama FFI bindings for WebAssembly store operations
/// 
/// This module provides C-compatible functions for creating, configuring,
/// and managing WebAssembly stores through the Panama Foreign Function Interface.
pub mod store {
    use super::*;
    use crate::error::ffi_utils;
    use crate::store::core;
    
    /// Create a new WebAssembly store with default configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_store_create(
        engine_ptr: *mut c_void,
        store_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };
            
            let store = core::create_store(engine)?;
            
            unsafe {
                *store_ptr = Box::into_raw(store) as *mut c_void;
            }
            
            Ok(())
        })
    }
    
    /// Create a new WebAssembly store with custom configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_store_create_with_config(
        engine_ptr: *mut c_void,
        fuel_limit: c_ulong,        // 0 = no limit
        memory_limit_bytes: c_ulong, // 0 = no limit
        execution_timeout_secs: c_ulong, // 0 = no timeout
        max_instances: c_uint,      // 0 = no limit
        max_table_elements: c_uint, // 0 = no limit
        max_functions: c_uint,      // 0 = no limit
        store_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };
            
            let fuel_limit_opt = if fuel_limit == 0 { None } else { Some(fuel_limit as u64) };
            let memory_limit_opt = if memory_limit_bytes == 0 { None } else { Some(memory_limit_bytes as usize) };
            let timeout_opt = if execution_timeout_secs == 0 { None } else { Some(execution_timeout_secs) };
            let max_instances_opt = if max_instances == 0 { None } else { Some(max_instances as usize) };
            let max_table_elements_opt = if max_table_elements == 0 { None } else { Some(max_table_elements) };
            let max_functions_opt = if max_functions == 0 { None } else { Some(max_functions as usize) };
            
            let store = core::create_store_with_config(
                engine,
                fuel_limit_opt,
                memory_limit_opt,
                timeout_opt,
                max_instances_opt,
                max_table_elements_opt,
                max_functions_opt,
            )?;
            
            unsafe {
                *store_ptr = Box::into_raw(store) as *mut c_void;
            }
            
            Ok(())
        })
    }
    
    /// Add fuel to the store for execution limiting (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_store_add_fuel(
        store_ptr: *mut c_void,
        fuel: c_ulong,
    ) -> c_int {
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
        total_execution_time_ms_ptr: *mut c_ulong,
        fuel_consumed_ptr: *mut c_ulong,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let store = unsafe { core::get_store_ref(store_ptr)? };
            let stats = core::get_execution_stats(store)?;
            
            unsafe {
                *execution_count_ptr = stats.execution_count;
                *total_execution_time_ms_ptr = stats.total_execution_time.as_millis() as c_ulong;
                *fuel_consumed_ptr = stats.fuel_consumed;
            }
            
            Ok(())
        })
    }
    
    /// Get store memory usage statistics (Panama FFI version)
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
                *total_bytes_ptr = usage.total_bytes as c_ulong;
                *used_bytes_ptr = usage.used_bytes as c_ulong;
                *instance_count_ptr = usage.instance_count as c_ulong;
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
                *execution_timeout_secs_ptr = metadata.execution_timeout
                    .map(|d| d.as_secs())
                    .unwrap_or(0) as c_ulong;
                *instance_count_ptr = metadata.instance_count as c_ulong;
            }
            
            Ok(())
        })
    }
    
    /// Destroy a WebAssembly store (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_store_destroy(store_ptr: *mut c_void) {
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

    /// Set fuel for a WebAssembly store (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_store_set_fuel(
        store_ptr: *mut c_void,
        fuel: c_ulong,
    ) -> c_int {
        use crate::error::ffi_utils;

        ffi_utils::ffi_try_code(|| {
            let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };
            crate::store::core::set_fuel_level(store, fuel as u64)?;
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
        use crate::error::ffi_utils;
        
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
    
}

/// Panama FFI bindings for WebAssembly Component Model operations (WASI Preview 2)
///
/// Component Model FFI functions are provided directly by the component.rs module.
/// No additional Panama-specific wrappers are needed as the existing FFI exports
/// are compatible with both JNI and Panama Foreign Function API.

/// Active Component Model FFI functions for Panama
pub mod component {
    use super::*;
    use crate::error::ffi_utils;
    use crate::wit_value_marshal;
    use wasmtime::component::{Instance, Val};

    /// Structure for passing WIT values through FFI
    /// Format: type_discriminator (4 bytes) + data_length (4 bytes) + data
    #[repr(C)]
    struct WitValueFFI {
        type_discriminator: c_int,
        data_length: c_int,
        data_ptr: *const u8,
    }

    /// Get list of exported function names from a component instance
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_get_exported_functions(
        instance_ptr: *mut c_void,
        functions_out: *mut *mut *mut c_char,
        count_out: *mut c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let _instance = unsafe { ffi_utils::deref_ptr::<Instance>(instance_ptr, "instance")? };

            // TODO: Extract function names from component metadata
            // For now, return empty list as we need WIT metadata parsing
            let function_names: Vec<String> = Vec::new();

            // Allocate array of C strings
            let count = function_names.len();
            let mut c_strings: Vec<*mut c_char> = Vec::with_capacity(count);

            for name in function_names {
                let c_string = std::ffi::CString::new(name)
                    .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid function name: {}", e),
                    })?;
                c_strings.push(c_string.into_raw());
            }

            // Box the array and return pointer
            unsafe {
                *functions_out = Box::into_raw(c_strings.into_boxed_slice()) as *mut *mut c_char;
                *count_out = count as c_int;
            }

            Ok(())
        })
    }

    /// Invoke a component function with parameters
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_invoke(
        instance_ptr: *mut c_void,
        function_name: *const c_char,
        params_ptr: *const WitValueFFI,
        params_count: c_int,
        results_out: *mut *mut WitValueFFI,
        results_count_out: *mut c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let _instance = unsafe { ffi_utils::deref_ptr::<Instance>(instance_ptr, "instance")? };

            let _func_name = unsafe {
                std::ffi::CStr::from_ptr(function_name)
                    .to_str()
                    .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid function name: {}", e),
                    })?
            };

            // Parse parameters from FFI format to Vec<Val>
            let mut params: Vec<Val> = Vec::with_capacity(params_count as usize);
            if !params_ptr.is_null() && params_count > 0 {
                unsafe {
                    let params_slice = std::slice::from_raw_parts(params_ptr, params_count as usize);
                    for param_ffi in params_slice {
                        let data = std::slice::from_raw_parts(
                            param_ffi.data_ptr,
                            param_ffi.data_length as usize,
                        );
                        let val = wit_value_marshal::deserialize_to_val(
                            param_ffi.type_discriminator,
                            data,
                        )?;
                        params.push(val);
                    }
                }
            }

            // TODO: Actual function invocation requires architectural changes
            //
            // Problem: Wasmtime component functions require both Instance AND Store to invoke:
            //   let func = instance.get_func(&mut store, function_name)?;
            //   func.call(&mut store, params, &mut results)?;
            //
            // Current architecture passes only Instance pointer, but Store is needed for:
            // 1. Looking up functions: instance.get_func(&mut store, name)
            // 2. Calling functions: func.call(&mut store, params, results)
            // 3. Post-call cleanup: func.post_return(&mut store)
            //
            // Solutions:
            // Option 1: Pass both Store and Instance pointers through FFI
            // Option 2: Use EnhancedComponentEngine with instance IDs (already implemented)
            // Option 3: Store both in a wrapper struct and pass wrapper pointer
            //
            // For now, return empty results to demonstrate marshalling infrastructure works
            let _results: Vec<Val> = Vec::new();

            // Serialize results to FFI format
            // For now, return empty results
            unsafe {
                *results_out = std::ptr::null_mut();
                *results_count_out = 0;
            }

            Ok(())
        })
    }

    /// Free the array of C strings returned by get_exported_functions
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_free_string_array(
        strings: *mut *mut c_char,
        count: c_int,
    ) {
        if strings.is_null() {
            return;
        }

        unsafe {
            // Reconstruct the boxed slice
            let slice = std::slice::from_raw_parts_mut(strings, count as usize);

            // Free each C string
            for i in 0..count as usize {
                if !slice[i].is_null() {
                    let _ = std::ffi::CString::from_raw(slice[i]);
                }
            }

            // Free the array itself
            let _ = Box::from_raw(slice);
        }
    }

    /// Free WIT value array returned by invoke
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_free_wit_values(
        values_ptr: *mut WitValueFFI,
        count: c_int,
    ) {
        if values_ptr.is_null() {
            return;
        }

        unsafe {
            let values_slice = std::slice::from_raw_parts_mut(values_ptr, count as usize);

            // Free each value's data
            for i in 0..count as usize {
                if !values_slice[i].data_ptr.is_null() {
                    let data_vec = Vec::from_raw_parts(
                        values_slice[i].data_ptr as *mut u8,
                        values_slice[i].data_length as usize,
                        values_slice[i].data_length as usize,
                    );
                    drop(data_vec);
                }
            }

            // Free the array itself
            let _ = Box::from_raw(values_slice);
        }
    }
}

/// Enhanced Component Model FFI functions using EnhancedComponentEngine
/// This provides proper Store/Instance lifecycle management via instance IDs
pub mod component_enhanced {
    use super::*;
    use crate::component_core::EnhancedComponentEngine;
    use crate::error::ffi_utils;
    use crate::wit_value_marshal;
    use crate::component::Component;
    use wasmtime::component::Val;

    /// WitValueFFI structure (same as component module)
    #[repr(C)]
    struct WitValueFFI {
        type_discriminator: c_int,
        data_length: c_int,
        data_ptr: *const u8,
    }

    /// Create an enhanced component engine
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_enhanced_component_engine_create() -> *mut c_void {
        match EnhancedComponentEngine::new() {
            Ok(engine) => Box::into_raw(Box::new(engine)) as *mut c_void,
            Err(e) => {
                log::error!("Failed to create enhanced component engine: {:?}", e);
                std::ptr::null_mut()
            }
        }
    }

    /// Load component from bytes using enhanced engine
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_enhanced_component_load_from_bytes(
        engine_ptr: *mut c_void,
        wasm_bytes: *const u8,
        wasm_size: usize,
        component_out: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let engine = unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };
            let wasm_data = unsafe {
                if wasm_bytes.is_null() || wasm_size == 0 {
                    return Err(crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid WASM bytes or size".to_string(),
                    });
                }
                std::slice::from_raw_parts(wasm_bytes, wasm_size)
            };

            let component = engine.load_component_from_bytes(wasm_data)?;

            unsafe {
                *component_out = Box::into_raw(Box::new(component)) as *mut c_void;
            }

            Ok(())
        })
    }

    /// Instantiate a component and return instance ID
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_enhanced_component_instantiate(
        engine_ptr: *mut c_void,
        component_ptr: *mut c_void,
        instance_id_out: *mut c_ulong,
    ) -> c_int {
        use std::fs::OpenOptions;
        use std::io::Write;

        let _ = OpenOptions::new()
            .create(true)
            .append(true)
            .open("/tmp/wasmtime4j_debug.log")
            .and_then(|mut log| {
                writeln!(log, "\n===== RUST FFI ENTRY POINT =====")?;
                writeln!(log, "RUST: Function called with engine_ptr={:?}", engine_ptr)?;
                writeln!(log, "RUST: component_ptr={:?}", component_ptr)
            });

        let result = ffi_utils::ffi_try_code(|| {
            let engine = unsafe {
                ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")?
            };

            let component = unsafe {
                ffi_utils::deref_ptr::<Component>(component_ptr, "component")?
            };

            let instance_id = engine.instantiate_component(component)?;

            unsafe {
                *instance_id_out = instance_id;
            }

            Ok(())
        });

        let _ = OpenOptions::new()
            .create(true)
            .append(true)
            .open("/tmp/wasmtime4j_debug.log")
            .and_then(|mut log| {
                writeln!(log, "RUST: Result = {:?}", result)
            });

        result
    }

    /// Invoke a component function using instance ID
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_enhanced_component_invoke(
        engine_ptr: *mut c_void,
        instance_id: c_ulong,
        function_name: *const c_char,
        params_ptr: *const WitValueFFI,
        params_count: c_int,
        results_out: *mut *mut WitValueFFI,
        results_count_out: *mut c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            eprintln!("PANAMA FFI: enhanced_component_invoke called");
            let engine = unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };

            let func_name = unsafe {
                std::ffi::CStr::from_ptr(function_name)
                    .to_str()
                    .map_err(|e| {
                        eprintln!("ERROR: Invalid function name: {}", e);
                        crate::error::WasmtimeError::InvalidParameter {
                            message: format!("Invalid function name: {}", e),
                        }
                    })?
            };

            eprintln!("PANAMA FFI: Function name: {}, params_count: {}", func_name, params_count);

            // Parse parameters from FFI format to Vec<Val>
            let mut params: Vec<Val> = Vec::with_capacity(params_count as usize);
            if !params_ptr.is_null() && params_count > 0 {
                unsafe {
                    let params_slice = std::slice::from_raw_parts(params_ptr, params_count as usize);
                    for (i, param_ffi) in params_slice.iter().enumerate() {
                        eprintln!("PANAMA FFI: Deserializing param {}: type={}, length={}",
                                 i, param_ffi.type_discriminator, param_ffi.data_length);
                        let data = std::slice::from_raw_parts(
                            param_ffi.data_ptr,
                            param_ffi.data_length as usize,
                        );
                        let val = wit_value_marshal::deserialize_to_val(
                            param_ffi.type_discriminator,
                            data,
                        ).map_err(|e| {
                            eprintln!("ERROR: Failed to deserialize param {}: {:?}", i, e);
                            e
                        })?;
                        eprintln!("PANAMA FFI: Param {} deserialized successfully", i);
                        params.push(val);
                    }
                }
            }

            eprintln!("PANAMA FFI: About to invoke component function");
            // Invoke the function using EnhancedComponentEngine
            let results = engine.invoke_component_function(instance_id, func_name, &params).map_err(|e| {
                eprintln!("ERROR: invoke_component_function failed: {:?}", e);
                e
            })?;
            eprintln!("PANAMA FFI: Function invoked successfully, results.len()={}", results.len());

            // Serialize results to FFI format
            // First, flatten any tuples into their constituent elements
            let mut flattened_results: Vec<&Val> = Vec::new();
            for val in &results {
                match val {
                    Val::Tuple(elements) => {
                        // Unwrap tuple into individual elements
                        flattened_results.extend(elements.iter());
                    }
                    _ => {
                        flattened_results.push(val);
                    }
                }
            }

            if !flattened_results.is_empty() {
                let mut results_ffi: Vec<WitValueFFI> = Vec::with_capacity(flattened_results.len());

                for val in flattened_results {
                    let (type_discriminator, data) = wit_value_marshal::serialize_from_val(val)?;

                    // Allocate data on heap
                    let data_vec = data.into_boxed_slice();
                    let data_len = data_vec.len();
                    let data_ptr = Box::into_raw(data_vec) as *const u8;

                    results_ffi.push(WitValueFFI {
                        type_discriminator,
                        data_length: data_len as c_int,
                        data_ptr,
                    });
                }

                let results_count = results_ffi.len();
                unsafe {
                    *results_out = Box::into_raw(results_ffi.into_boxed_slice()) as *mut WitValueFFI;
                    *results_count_out = results_count as c_int;
                }
            } else {
                unsafe {
                    *results_out = std::ptr::null_mut();
                    *results_count_out = 0;
                }
            }

            Ok(())
        })
    }

    /// Get exported function names using instance ID
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_enhanced_component_get_exports(
        engine_ptr: *mut c_void,
        instance_id: c_ulong,
        functions_out: *mut *mut *mut c_char,
        count_out: *mut c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let engine = unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };

            let function_names = engine.get_exported_function_names(instance_id)?;

            // Allocate array of C strings
            let count = function_names.len();
            let mut c_strings: Vec<*mut c_char> = Vec::with_capacity(count);

            for name in function_names {
                let c_string = std::ffi::CString::new(name)
                    .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid function name: {}", e),
                    })?;
                c_strings.push(c_string.into_raw());
            }

            // Box the array and return pointer
            unsafe {
                *functions_out = Box::into_raw(c_strings.into_boxed_slice()) as *mut *mut c_char;
                *count_out = count as c_int;
            }

            Ok(())
        })
    }

    /// Destroy an enhanced component engine
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_enhanced_component_engine_destroy(engine_ptr: *mut c_void) {
        if !engine_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(engine_ptr as *mut EnhancedComponentEngine);
            }
        }
    }
}

// TODO: Re-enable when component modules with enhanced features are available
/*
#[cfg(feature = "component-model")]
pub mod component_enhanced {
    use super::*;
    use crate::component_core::{EnhancedComponentEngine, ComponentInstanceHandle};
    use crate::wit_interfaces::WitInterfaceManager;
    use crate::component_orchestration::ComponentOrchestrator;
    use crate::component_resources::ComponentResourceManager;
    // TODO: Re-enable when distributed_components module is implemented
    // use crate::distributed_components::DistributedComponentManager;
    
    /// Create a new component engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_engine_create() -> *mut c_void {
        use crate::error::ffi_utils;
        
        ffi_utils::ffi_try_ptr(|| crate::component::core::create_component_engine())
    }
    
    /// Load a component from WebAssembly bytes (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_load_from_bytes(
        engine_ptr: *mut c_void,
        wasm_bytes: *const u8,
        wasm_size: usize,
        component_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::error::ffi_utils;
        
        ffi_utils::ffi_try_code(|| {
            let engine = unsafe { crate::component::core::get_component_engine_ref(engine_ptr)? };
            let wasm_data = unsafe { ffi_utils::slice_from_raw_parts(wasm_bytes, wasm_size, "component_bytes")? };
            
            let component = crate::component::core::load_component_from_bytes(engine, wasm_data)?;
            
            unsafe {
                *component_ptr = Box::into_raw(component) as *mut c_void;
            }
            
            Ok(())
        })
    }
    
    /// Instantiate a component (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_instantiate(
        engine_ptr: *mut c_void,
        component_ptr: *mut c_void,
        instance_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::component::{ComponentEngine, Component};
        use crate::error::{ffi_utils, ErrorCode};
        
        if engine_ptr.is_null() || component_ptr.is_null() || instance_ptr.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let engine = unsafe { &*(engine_ptr as *const ComponentEngine) };
        let component = unsafe { &*(component_ptr as *const Component) };
        
        match engine.instantiate_component(component) {
            Ok(instance) => {
                unsafe {
                    *instance_ptr = Box::into_raw(Box::new(instance)) as *mut c_void;
                }
                ffi_utils::clear_last_error();
                0
            }
            Err(e) => {
                let error_code = e.to_error_code();
                log::error!("Failed to instantiate component: {:?}", e);
                ffi_utils::set_last_error(e);
                error_code as c_int
            }
        }
    }
    
    /// Get component size in bytes (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_get_size(
        component_ptr: *mut c_void,
        size_out: *mut usize,
    ) -> c_int {
        use crate::component::Component;
        use crate::error::{ffi_utils, ErrorCode};
        
        if component_ptr.is_null() || size_out.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let component = unsafe { &*(component_ptr as *const Component) };
        
        unsafe {
            *size_out = component.size_bytes();
        }
        
        ffi_utils::clear_last_error();
        0
    }
    
    /// Check if component exports an interface (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_exports_interface(
        component_ptr: *mut c_void,
        interface_name: *const c_char,
        result_out: *mut c_int,
    ) -> c_int {
        use crate::component::Component;
        use crate::error::{ffi_utils, ErrorCode};
        use std::ffi::CStr;
        
        if component_ptr.is_null() || interface_name.is_null() || result_out.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let component = unsafe { &*(component_ptr as *const Component) };
        
        let interface_str = unsafe {
            match CStr::from_ptr(interface_name).to_str() {
                Ok(s) => s,
                Err(e) => {
                    log::error!("Failed to convert interface name: {:?}", e);
                    return ErrorCode::InvalidParameterError as c_int;
                }
            }
        };
        
        unsafe {
            *result_out = if component.exports_interface(interface_str) { 1 } else { 0 };
        }
        
        ffi_utils::clear_last_error();
        0
    }
    
    /// Check if component imports an interface (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_imports_interface(
        component_ptr: *mut c_void,
        interface_name: *const c_char,
        result_out: *mut c_int,
    ) -> c_int {
        use crate::component::Component;
        use crate::error::{ffi_utils, ErrorCode};
        use std::ffi::CStr;
        
        if component_ptr.is_null() || interface_name.is_null() || result_out.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let component = unsafe { &*(component_ptr as *const Component) };
        
        let interface_str = unsafe {
            match CStr::from_ptr(interface_name).to_str() {
                Ok(s) => s,
                Err(e) => {
                    log::error!("Failed to convert interface name: {:?}", e);
                    return ErrorCode::InvalidParameterError as c_int;
                }
            }
        };
        
        unsafe {
            *result_out = if component.imports_interface(interface_str) { 1 } else { 0 };
        }
        
        ffi_utils::clear_last_error();
        0
    }
    
    /// Get active component instances count (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_get_active_instances_count(
        engine_ptr: *mut c_void,
        count_out: *mut c_int,
    ) -> c_int {
        use crate::component::ComponentEngine;
        use crate::error::{ffi_utils, ErrorCode};
        
        if engine_ptr.is_null() || count_out.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let engine = unsafe { &*(engine_ptr as *const ComponentEngine) };
        
        match engine.get_active_instances() {
            Ok(instances) => {
                unsafe {
                    *count_out = instances.len() as c_int;
                }
                ffi_utils::clear_last_error();
                0
            }
            Err(e) => {
                let error_code = e.to_error_code();
                log::error!("Failed to get active instances: {:?}", e);
                ffi_utils::set_last_error(e);
                error_code as c_int
            }
        }
    }
    
    /// Cleanup inactive component instances (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_cleanup_instances(
        engine_ptr: *mut c_void,
        cleaned_count_out: *mut c_int,
    ) -> c_int {
        use crate::component::ComponentEngine;
        use crate::error::{ffi_utils, ErrorCode};
        
        if engine_ptr.is_null() || cleaned_count_out.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let engine = unsafe { &*(engine_ptr as *const ComponentEngine) };
        
        match engine.cleanup_instances() {
            Ok(cleaned_count) => {
                unsafe {
                    *cleaned_count_out = cleaned_count as c_int;
                }
                ffi_utils::clear_last_error();
                0
            }
            Err(e) => {
                let error_code = e.to_error_code();
                log::error!("Failed to cleanup instances: {:?}", e);
                ffi_utils::set_last_error(e);
                error_code as c_int
            }
        }
    }

    /// Create a WIT interface manager (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_wit_interface_manager_create() -> *mut c_void {
        use crate::error::ffi_utils;
        ffi_utils::ffi_try_ptr(|| Ok(Box::new(WitInterfaceManager::new())))
    }

    /// Create a component orchestrator (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_orchestrator_create(
        enhanced_engine_ptr: *mut c_void,
    ) -> *mut c_void {
        use crate::error::ffi_utils;

        ffi_utils::ffi_try_ptr(|| {
            let engine = unsafe {
                crate::component_core::core::get_enhanced_component_engine_ref(enhanced_engine_ptr as *const c_void)?
            };
            // Simplified Arc creation - would need proper Arc management in production
            let engine_arc = std::sync::Arc::new(engine.clone());
            ComponentOrchestrator::new(engine_arc).map(Box::new)
        })
    }

    /// Create a component resource manager (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_resource_manager_create() -> *mut c_void {
        use crate::error::ffi_utils;
        ffi_utils::ffi_try_ptr(|| Ok(Box::new(ComponentResourceManager::new())))
    }

    /// Create a distributed component manager (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_distributed_component_manager_create(
        node_id: *const c_char,
        node_name: *const c_char,
    ) -> *mut c_void {
        use crate::error::ffi_utils;
        use std::ffi::CStr;

        ffi_utils::ffi_try_ptr(|| {
            let node_id_str = unsafe {
                CStr::from_ptr(node_id).to_str()
                    .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid node ID: {}", e),
                    })?
                    .to_string()
            };

            let node_name_str = unsafe {
                CStr::from_ptr(node_name).to_str()
                    .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid node name: {}", e),
                    })?
                    .to_string()
            };

            // TODO: Re-enable when distributed_components module is implemented
            /*
            let node_info = crate::distributed_components::NodeInfo {
                id: node_id_str,
                name: node_name_str,
                addresses: Vec::new(),
                capabilities: crate::distributed_components::NodeCapabilities {
                    supported_types: std::collections::HashSet::new(),
                    available_resources: crate::distributed_components::ResourceCapabilities {
                        cpu_cores: 4,
                        memory_bytes: 8 * 1024 * 1024 * 1024,
                        storage_bytes: 100 * 1024 * 1024 * 1024,
                        network_bandwidth: 1000 * 1024 * 1024,
                        hardware_features: std::collections::HashSet::new(),
                    },
                    security_features: crate::distributed_components::SecurityCapabilities {
                        encryption_algorithms: std::collections::HashSet::new(),
                        auth_methods: std::collections::HashSet::new(),
                        trusted_cas: std::collections::HashSet::new(),
                        security_level: crate::distributed_components::SecurityLevel::Standard,
                    },
                    performance: crate::distributed_components::PerformanceCapabilities {
                        cpu_score: 1000.0,
                        memory_bandwidth: 1000 * 1024 * 1024,
                        storage_iops: 1000,
                        network_latency: 1.0,
                        reliability_score: 0.99,
                    },
                },
                status: crate::distributed_components::NodeStatus::Active,
                last_seen: std::time::Instant::now(),
                metadata: std::collections::HashMap::new(),
            };

            DistributedComponentManager::new(node_info).map(Box::new)
            */
            // Placeholder implementation until distributed_components is available
            Ok(Box::new(()))
        })
    }

    /// Get component metrics from enhanced engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_enhanced_component_get_metrics(
        enhanced_engine_ptr: *mut c_void,
        metrics_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::error::ffi_utils;

        ffi_utils::ffi_try_code(|| {
            let engine = unsafe {
                crate::component_core::core::get_enhanced_component_engine_ref(enhanced_engine_ptr as *const c_void)?
            };

            let metrics = crate::component_core::core::get_component_metrics(engine)?;

            unsafe {
                *metrics_ptr = Box::into_raw(Box::new(metrics)) as *mut c_void;
            }

            Ok(())
        })
    }

    /// Get components loaded count from metrics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_metrics_get_components_loaded(
        metrics_ptr: *const c_void,
    ) -> u64 {
        if metrics_ptr.is_null() {
            return 0;
        }
        let metrics = unsafe { &*(metrics_ptr as *const crate::component_core::ComponentMetrics) };
        metrics.components_loaded
    }

    /// Get instances created count from metrics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_metrics_get_instances_created(
        metrics_ptr: *const c_void,
    ) -> u64 {
        if metrics_ptr.is_null() {
            return 0;
        }
        let metrics = unsafe { &*(metrics_ptr as *const crate::component_core::ComponentMetrics) };
        metrics.instances_created
    }

    /// Get instances destroyed count from metrics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_metrics_get_instances_destroyed(
        metrics_ptr: *const c_void,
    ) -> u64 {
        if metrics_ptr.is_null() {
            return 0;
        }
        let metrics = unsafe { &*(metrics_ptr as *const crate::component_core::ComponentMetrics) };
        metrics.instances_destroyed
    }

    /// Get average instantiation time in nanoseconds from metrics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_metrics_get_avg_instantiation_time_nanos(
        metrics_ptr: *const c_void,
    ) -> u64 {
        if metrics_ptr.is_null() {
            return 0;
        }
        let metrics = unsafe { &*(metrics_ptr as *const crate::component_core::ComponentMetrics) };
        metrics.avg_instantiation_time.as_nanos() as u64
    }

    /// Get peak memory usage from metrics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_metrics_get_peak_memory_usage(
        metrics_ptr: *const c_void,
    ) -> u64 {
        if metrics_ptr.is_null() {
            return 0;
        }
        let metrics = unsafe { &*(metrics_ptr as *const crate::component_core::ComponentMetrics) };
        metrics.peak_memory_usage as u64
    }

    /// Get function calls count from metrics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_metrics_get_function_calls(
        metrics_ptr: *const c_void,
    ) -> u64 {
        if metrics_ptr.is_null() {
            return 0;
        }
        let metrics = unsafe { &*(metrics_ptr as *const crate::component_core::ComponentMetrics) };
        metrics.function_calls
    }

    /// Get error count from metrics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_metrics_get_error_count(
        metrics_ptr: *const c_void,
    ) -> u64 {
        if metrics_ptr.is_null() {
            return 0;
        }
        let metrics = unsafe { &*(metrics_ptr as *const crate::component_core::ComponentMetrics) };
        metrics.error_count
    }

    /// Destroy component metrics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_metrics_destroy(
        metrics_ptr: *mut c_void,
    ) {
        if !metrics_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(metrics_ptr as *mut crate::component_core::ComponentMetrics);
            }
        }
    }

    /// Register interface with WIT interface manager (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_wit_interface_manager_register(
        manager_ptr: *mut c_void,
        interface_name: *const c_char,
    ) -> c_int {
        use crate::error::ffi_utils;
        use std::ffi::CStr;

        ffi_utils::ffi_try_code(|| {
            let manager = unsafe {
                &*(manager_ptr as *const WitInterfaceManager)
            };

            let interface_str = unsafe {
                CStr::from_ptr(interface_name).to_str()
                    .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid interface name: {}", e),
                    })?
                    .to_string()
            };

            // Create minimal interface definition for testing
            let interface_def = crate::component::InterfaceDefinition {
                name: interface_str,
                namespace: None,
                version: None,
                functions: Vec::new(),
                types: Vec::new(),
                resources: Vec::new(),
            };

            manager.register_interface(interface_def)
        })
    }

    /// Create resource with resource manager (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_resource_manager_create_resource(
        manager_ptr: *mut c_void,
        resource_type: *const c_char,
        owner: *const c_char,
        resource_handle_out: *mut u64,
    ) -> c_int {
        use crate::error::ffi_utils;
        use std::ffi::CStr;

        ffi_utils::ffi_try_code(|| {
            let manager = unsafe {
                &*(manager_ptr as *const ComponentResourceManager)
            };

            let resource_type_str = unsafe {
                CStr::from_ptr(resource_type).to_str()
                    .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid resource type: {}", e),
                    })?
                    .to_string()
            };

            let owner_str = unsafe {
                CStr::from_ptr(owner).to_str()
                    .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid owner: {}", e),
                    })?
                    .to_string()
            };

            // Create minimal resource for testing (simplified implementation)
            let wasmtime_resource = wasmtime::component::ResourceAny::new(); // This would need proper resource creation
            let metadata = crate::component_resources::ResourceMetadata {
                size_bytes: Some(1024),
                description: Some("Test resource".to_string()),
                tags: std::collections::HashMap::new(),
                version: None,
                properties: std::collections::HashMap::new(),
            };

            let handle = manager.create_resource(resource_type_str, owner_str, wasmtime_resource, metadata)?;

            unsafe {
                *resource_handle_out = handle;
            }

            Ok(())
        })
    }

    /// Destroy a component engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_engine_destroy(engine_ptr: *mut c_void) {
        unsafe {
            crate::component::core::destroy_component_engine(engine_ptr);
        }
    }
    
    /// Destroy a component (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_destroy(component_ptr: *mut c_void) {
        unsafe {
            crate::component::core::destroy_component(component_ptr);
        }
    }
    
    /// Destroy a component instance (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_component_instance_destroy(instance_ptr: *mut c_void) {
        unsafe {
            crate::component::core::destroy_component_instance(instance_ptr);
        }
    }
}
*/

/// Panama FFI bindings for WebAssembly linear memory operations
/// 
/// This module provides C-compatible functions for creating, managing,
/// and accessing WebAssembly linear memory with comprehensive bounds checking.
pub mod memory {
    use super::*;
    use crate::memory::{Memory, MemoryBuilder, MemoryDataType, MemoryRegistry};
    use crate::store::Store;
    use crate::error::ffi_utils;

    /// Create a new WebAssembly memory with default configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_create(
        store_ptr: *mut c_void,
        initial_pages: c_uint,
        memory_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
            
            let memory = Memory::new(store, initial_pages as u64)?;
            
            unsafe {
                *memory_ptr = Box::into_raw(Box::new(memory)) as *mut c_void;
            }
            
            Ok(())
        })
    }

    /// Create a new WebAssembly memory with configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_create_with_config(
        store_ptr: *mut c_void,
        initial_pages: c_uint,
        maximum_pages: c_uint,
        is_shared: c_int,
        memory_index: c_uint,
        name: *const c_char,
        memory_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // SAFETY IMPROVEMENT: Using new memory utilities with comprehensive validation
            let store = crate::ffi_common::memory_utils::safe_deref_mut(
                store_ptr as *mut Store, 
                "store"
            ).map_err(|e| e.to_wasmtime_error())?;
            
            let mut builder = MemoryBuilder::new(initial_pages as u64);
            
            if maximum_pages > 0 {
                builder = builder.maximum_pages(maximum_pages as u64);
            }
            
            if is_shared != 0 {
                builder = builder.shared();
            }
            
            builder = builder.memory_index(memory_index);
            
            if !name.is_null() {
                let name_str = unsafe { ffi_utils::c_str_to_string(name, "memory_name")? };
                builder = builder.name(name_str);
            }
            
            let memory = builder.build(store)?;
            
            unsafe {
                *memory_ptr = Box::into_raw(Box::new(memory)) as *mut c_void;
            }
            
            Ok(())
        })
    }

    /// Get memory size in pages (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_size_pages(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        size_out: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

            // Call wasmtime::Memory::size directly with store context
            let size = store.with_context_ro(|ctx| {
                Ok(memory.size(ctx))
            })?;

            unsafe {
                *size_out = size as c_uint;
            }

            Ok(())
        })
    }

    /// Get memory size in bytes (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_size_bytes(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        size_out: *mut usize,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

            // Call wasmtime::Memory::data_size directly with store context
            let size = store.with_context_ro(|ctx| {
                Ok(memory.data_size(ctx))
            })?;

            unsafe {
                *size_out = size;
            }

            Ok(())
        })
    }

    /// Grow memory by additional pages (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_grow(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        additional_pages: c_uint,
        previous_pages_out: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            // Call wasmtime::Memory::grow directly with store context
            let previous_pages = store.with_context(|mut ctx| {
                memory.grow(&mut ctx, additional_pages as u64)
                    .map_err(|e| crate::error::WasmtimeError::Runtime {
                        message: format!("Failed to grow memory: {}", e),
                        backtrace: None,
                    })
            })?;

            unsafe {
                *previous_pages_out = previous_pages as c_uint;
            }

            Ok(())
        })
    }

    /// Grow memory by additional pages using 64-bit addressing (Panama FFI version)
    /// This supports Memory64 proposal for memories larger than 4GB.
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_grow64(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        additional_pages: u64,
        previous_pages_out: *mut u64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            // Call wasmtime::Memory::grow directly with store context
            let previous_pages = store.with_context(|mut ctx| {
                memory.grow(&mut ctx, additional_pages)
                    .map_err(|e| crate::error::WasmtimeError::Runtime {
                        message: format!("Failed to grow memory: {}", e),
                        backtrace: None,
                    })
            })?;

            unsafe {
                *previous_pages_out = previous_pages;
            }

            Ok(())
        })
    }

    /// Get whether memory uses 64-bit addressing (Memory64 proposal)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_is_64bit(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        is_64bit_out: *mut c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

            // Check memory type for 64-bit support
            let memory_type = store.with_context_ro(|ctx| Ok(memory.ty(ctx)))?;

            let is_64 = memory_type.is_64();

            unsafe {
                *is_64bit_out = if is_64 { 1 } else { 0 };
            }

            Ok(())
        })
    }

    /// Check if memory is shared between threads (for threads proposal)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_is_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        is_shared_out: *mut c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

            // Check memory type for shared status
            let memory_type = store.with_context_ro(|ctx| Ok(memory.ty(ctx)))?;

            let is_shared = memory_type.is_shared();

            unsafe {
                *is_shared_out = if is_shared { 1 } else { 0 };
            }

            Ok(())
        })
    }

    /// Get memory size in pages using 64-bit return value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_size_pages64(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        size_out: *mut u64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

            // Get memory size in pages
            let pages = store.with_context_ro(|ctx| Ok(memory.size(ctx)))?;

            unsafe {
                *size_out = pages;
            }

            Ok(())
        })
    }

    /// Read bytes from memory with bounds checking (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_read_bytes(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        length: usize,
        buffer: *mut u8,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

            if buffer.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Buffer cannot be null".to_string(),
                });
            }

            // Read bytes directly from wasmtime::Memory
            store.with_context_ro(|ctx| {
                let data = memory.data(ctx);
                if offset + length > data.len() {
                    return Err(crate::error::WasmtimeError::Memory {
                        message: format!("Memory access out of bounds: offset={}, length={}, size={}", offset, length, data.len()),
                    });
                }

                unsafe {
                    std::ptr::copy_nonoverlapping(data.as_ptr().add(offset), buffer, length);
                }

                Ok(())
            })
        })
    }

    /// Write bytes to memory with bounds checking (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_write_bytes(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        length: usize,
        buffer: *const u8,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            if buffer.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Buffer cannot be null".to_string(),
                });
            }

            // Write bytes directly to wasmtime::Memory
            store.with_context(|mut ctx| {
                let data = memory.data_mut(&mut ctx);
                if offset + length > data.len() {
                    return Err(crate::error::WasmtimeError::Memory {
                        message: format!("Memory access out of bounds: offset={}, length={}, size={}", offset, length, data.len()),
                    });
                }

                unsafe {
                    std::ptr::copy_nonoverlapping(buffer, data.as_mut_ptr().add(offset), length);
                }

                Ok(())
            })
        })
    }

    /// Check if instance has a memory export with the given name
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_has_memory_export(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in memory name".to_string(),
                    })?
            };

            match crate::instance::core::get_exported_memory(instance, store, name_str)? {
                Some(_) => Ok(()),
                None => Err(crate::error::WasmtimeError::ImportExport {
                    message: format!("Memory '{}' not found", name_str),
                }),
            }
        })
    }

    /// Get memory size in pages by looking up memory fresh
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_get_memory_size_pages(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        size_out: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in memory name".to_string(),
                    })?
            };

            let memory = crate::instance::core::get_exported_memory(instance, store, name_str)?
                .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                    message: format!("Memory '{}' not found", name_str),
                })?;

            let size = store.with_context_ro(|ctx| Ok(memory.size(ctx)))?;

            unsafe {
                *size_out = size as c_uint;
            }

            Ok(())
        })
    }

    /// Get memory size in bytes by looking up memory fresh
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_get_memory_size_bytes(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        size_out: *mut usize,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in memory name".to_string(),
                    })?
            };

            let memory = crate::instance::core::get_exported_memory(instance, store, name_str)?
                .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                    message: format!("Memory '{}' not found", name_str),
                })?;

            let size = store.with_context_ro(|ctx| Ok(memory.data_size(ctx)))?;

            unsafe {
                *size_out = size;
            }

            Ok(())
        })
    }

    /// Grow memory by looking up memory fresh
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_grow_memory(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        additional_pages: c_uint,
        previous_pages_out: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in memory name".to_string(),
                    })?
            };

            let memory = crate::instance::core::get_exported_memory(instance, store, name_str)?
                .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                    message: format!("Memory '{}' not found", name_str),
                })?;

            let previous_pages = store.with_context(|mut ctx| {
                memory.grow(&mut ctx, additional_pages as u64)
                    .map_err(|e| crate::error::WasmtimeError::Runtime {
                        message: format!("Failed to grow memory: {}", e),
                        backtrace: None,
                    })
            })?;

            unsafe {
                *previous_pages_out = previous_pages as c_uint;
            }

            Ok(())
        })
    }

    /// Read bytes from memory by looking up memory fresh
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_read_memory_bytes(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        offset: usize,
        length: usize,
        buffer: *mut u8,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in memory name".to_string(),
                    })?
            };

            if buffer.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Buffer cannot be null".to_string(),
                });
            }

            let memory = crate::instance::core::get_exported_memory(instance, store, name_str)?
                .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                    message: format!("Memory '{}' not found", name_str),
                })?;

            store.with_context_ro(|ctx| {
                let data = memory.data(ctx);
                if offset + length > data.len() {
                    return Err(crate::error::WasmtimeError::Memory {
                        message: format!("Memory access out of bounds: offset={}, length={}, size={}", offset, length, data.len()),
                    });
                }

                unsafe {
                    std::ptr::copy_nonoverlapping(data.as_ptr().add(offset), buffer, length);
                }

                Ok(())
            })
        })
    }

    /// Write bytes to memory by looking up memory fresh
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_write_memory_bytes(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        offset: usize,
        length: usize,
        buffer: *const u8,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in memory name".to_string(),
                    })?
            };

            if buffer.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Buffer cannot be null".to_string(),
                });
            }

            let memory = crate::instance::core::get_exported_memory(instance, store, name_str)?
                .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                    message: format!("Memory '{}' not found", name_str),
                })?;

            store.with_context(|mut ctx| {
                let data = memory.data_mut(&mut ctx);
                if offset + length > data.len() {
                    return Err(crate::error::WasmtimeError::Memory {
                        message: format!("Memory access out of bounds: offset={}, length={}, size={}", offset, length, data.len()),
                    });
                }

                unsafe {
                    std::ptr::copy_nonoverlapping(buffer, data.as_mut_ptr().add(offset), length);
                }

                Ok(())
            })
        })
    }

    /// Get global type information
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_get_global_type(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        value_type_out: *mut i32,
        is_mutable_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in global name".to_string(),
                    })?
            };

            let global = crate::instance::core::get_exported_global(instance, store, name_str)?
                .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                    message: format!("Global '{}' not found", name_str),
                })?;

            store.with_context_ro(|ctx| {
                let global_type = global.ty(&ctx);

                // Map wasmtime value type to our type codes
                let type_code = match global_type.content() {
                    wasmtime::ValType::I32 => 0,
                    wasmtime::ValType::I64 => 1,
                    wasmtime::ValType::F32 => 2,
                    wasmtime::ValType::F64 => 3,
                    wasmtime::ValType::V128 => 4,
                    wasmtime::ValType::Ref(ref_type) => {
                        match ref_type.heap_type() {
                            wasmtime::HeapType::Func => 5, // FuncRef
                            _ => 6, // ExternRef or other
                        }
                    }
                };

                unsafe {
                    if !value_type_out.is_null() {
                        *value_type_out = type_code;
                    }
                    if !is_mutable_out.is_null() {
                        *is_mutable_out = if global_type.mutability() == wasmtime::Mutability::Var {
                            1
                        } else {
                            0
                        };
                    }
                }

                Ok(())
            })
        })
    }

    /// Check if global export exists
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_has_global_export(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in global name".to_string(),
                    })?
            };

            match crate::instance::core::get_exported_global(instance, store, name_str)? {
                Some(_) => Ok(()),
                None => Err(crate::error::WasmtimeError::ImportExport {
                    message: format!("Global '{}' not found", name_str),
                }),
            }
        })
    }

    /// Get value from global by looking up global fresh
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_get_global_value(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        i32_out: *mut i32,
        i64_out: *mut i64,
        f32_out: *mut f64,
        f64_out: *mut f64,
        ref_id_present_out: *mut i32,
        ref_id_out: *mut i64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in global name".to_string(),
                    })?
            };

            let global = crate::instance::core::get_exported_global(instance, store, name_str)?
                .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                    message: format!("Global '{}' not found", name_str),
                })?;

            store.with_context(|mut ctx| {
                let value = global.get(&mut ctx);

                // Initialize all outputs to 0
                unsafe {
                    if !i32_out.is_null() {
                        *i32_out = 0;
                    }
                    if !i64_out.is_null() {
                        *i64_out = 0;
                    }
                    if !f32_out.is_null() {
                        *f32_out = 0.0;
                    }
                    if !f64_out.is_null() {
                        *f64_out = 0.0;
                    }
                    if !ref_id_present_out.is_null() {
                        *ref_id_present_out = 0;
                    }
                    if !ref_id_out.is_null() {
                        *ref_id_out = 0;
                    }

                    // Set the appropriate output based on type
                    match value {
                        wasmtime::Val::I32(v) => {
                            if !i32_out.is_null() {
                                *i32_out = v;
                            }
                        }
                        wasmtime::Val::I64(v) => {
                            if !i64_out.is_null() {
                                *i64_out = v;
                            }
                        }
                        wasmtime::Val::F32(v) => {
                            if !f32_out.is_null() {
                                *f32_out = f32::from_bits(v) as f64;
                            }
                        }
                        wasmtime::Val::F64(v) => {
                            if !f64_out.is_null() {
                                *f64_out = f64::from_bits(v);
                            }
                        }
                        wasmtime::Val::FuncRef(maybe_func) => {
                            if !ref_id_present_out.is_null() {
                                *ref_id_present_out = if maybe_func.is_some() { 1 } else { 0 };
                            }
                        }
                        wasmtime::Val::ExternRef(maybe_ref) => {
                            if !ref_id_present_out.is_null() {
                                *ref_id_present_out = if maybe_ref.is_some() { 1 } else { 0 };
                            }
                        }
                        _ => {
                            return Err(crate::error::WasmtimeError::Type {
                                message: format!("Unsupported global value type: {:?}", value),
                            });
                        }
                    }
                }

                Ok(())
            })
        })
    }

    /// Set value for global by looking up global fresh
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_set_global_value(
        instance_ptr: *const c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        value_type_code: i32,
        i32_value: i32,
        i64_value: i64,
        f32_value: f64,
        f64_value: f64,
        ref_id_present: i32,
        _ref_id: i64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { crate::instance::ffi_core::get_instance_ref(instance_ptr)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

            let name_str = unsafe {
                std::ffi::CStr::from_ptr(name)
                    .to_str()
                    .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                        message: "Invalid UTF-8 in global name".to_string(),
                    })?
            };

            let global = crate::instance::core::get_exported_global(instance, store, name_str)?
                .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                    message: format!("Global '{}' not found", name_str),
                })?;

            let value = match value_type_code {
                0 => wasmtime::Val::I32(i32_value),
                1 => wasmtime::Val::I64(i64_value),
                2 => wasmtime::Val::F32((f32_value as f32).to_bits()),
                3 => wasmtime::Val::F64(f64_value.to_bits()),
                5 => {
                    // FuncRef
                    if ref_id_present != 0 {
                        return Err(crate::error::WasmtimeError::Type {
                            message: "Setting funcref values not yet supported".to_string(),
                        });
                    }
                    wasmtime::Val::FuncRef(None)
                }
                6 => {
                    // ExternRef
                    if ref_id_present != 0 {
                        return Err(crate::error::WasmtimeError::Type {
                            message: "Setting externref values not yet supported".to_string(),
                        });
                    }
                    wasmtime::Val::ExternRef(None)
                }
                _ => {
                    return Err(crate::error::WasmtimeError::Type {
                        message: format!("Invalid value type code: {}", value_type_code),
                    });
                }
            };

            store.with_context(|mut ctx| {
                global
                    .set(&mut ctx, value)
                    .map_err(|e| crate::error::WasmtimeError::Runtime {
                        message: format!("Failed to set global value: {}", e),
                        backtrace: None,
                    })
            })
        })
    }

    /// Read typed value from memory with alignment checking (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_read_u32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value_out: *mut u32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            if value_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Value output pointer cannot be null".to_string(),
                });
            }
            
            let value: u32 = memory.read_typed(store, offset, MemoryDataType::U32Le)?;
            
            unsafe {
                *value_out = value;
            }
            
            Ok(())
        })
    }

    /// Write typed value to memory with alignment checking (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_write_u32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: u32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
            
            memory.write_typed(store, offset, value, MemoryDataType::U32Le)?;
            
            Ok(())
        })
    }

    /// Get memory usage statistics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_get_usage(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        current_bytes_out: *mut usize,
        current_pages_out: *mut c_uint,
        peak_bytes_out: *mut usize,
        read_count_out: *mut c_ulong,
        write_count_out: *mut c_ulong,
        bytes_transferred_out: *mut c_ulong,
        utilization_percent_out: *mut f64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let usage = memory.get_usage(store)?;
            
            unsafe {
                if !current_bytes_out.is_null() {
                    *current_bytes_out = usage.current_bytes;
                }
                if !current_pages_out.is_null() {
                    *current_pages_out = usage.current_pages as c_uint;
                }
                if !peak_bytes_out.is_null() {
                    *peak_bytes_out = usage.peak_bytes;
                }
                if !read_count_out.is_null() {
                    *read_count_out = usage.read_count as c_ulong;
                }
                if !write_count_out.is_null() {
                    *write_count_out = usage.write_count as c_ulong;
                }
                if !bytes_transferred_out.is_null() {
                    *bytes_transferred_out = usage.bytes_transferred as c_ulong;
                }
                if !utilization_percent_out.is_null() {
                    *utilization_percent_out = usage.utilization_percent;
                }
            }
            
            Ok(())
        })
    }

    /// Create a memory registry (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_registry_create(registry_ptr: *mut *mut c_void) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let registry = MemoryRegistry::new();
            
            unsafe {
                *registry_ptr = Box::into_raw(Box::new(registry)) as *mut c_void;
            }
            
            Ok(())
        })
    }

    /// Register a memory in the registry (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_registry_register(
        registry_ptr: *mut c_void,
        memory_ptr: *mut c_void,
        memory_id_out: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let registry = unsafe { ffi_utils::deref_ptr::<MemoryRegistry>(registry_ptr, "registry")? };
            let memory = unsafe { Box::from_raw(memory_ptr as *mut Memory) };
            
            let memory_id = registry.register(*memory)?;
            
            unsafe {
                *memory_id_out = memory_id;
            }
            
            Ok(())
        })
    }

    /// Get memory from registry (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_registry_get(
        registry_ptr: *mut c_void,
        memory_id: c_uint,
        memory_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let registry = unsafe { ffi_utils::deref_ptr::<MemoryRegistry>(registry_ptr, "registry")? };
            
            let memory_arc = registry.get(memory_id)?;
            
            // Return a reference to the Arc-wrapped memory
            // Note: This is a simplified approach - production code would need better lifetime management
            unsafe {
                *memory_ptr = Arc::as_ptr(&memory_arc) as *mut c_void;
            }
            
            Ok(())
        })
    }

    /// Destroy a memory instance (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_destroy(memory_ptr: *mut c_void) {
        // SAFETY IMPROVEMENT: Using safe resource destruction with validation
        unsafe {
            crate::ffi_common::memory_utils::destroy_ffi_resource::<Memory>(memory_ptr, "Memory");
        }
    }

    /// Destroy a memory registry (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_registry_destroy(registry_ptr: *mut c_void) {
        if !registry_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(registry_ptr as *mut MemoryRegistry);
            }
            log::debug!("Memory registry destroyed successfully");
        }
    }
}

/// Panama FFI bindings for WebAssembly global variables
///
/// This module provides C-compatible functions for creating, managing,
/// and accessing WebAssembly global variables with type safety and mutability enforcement.
pub mod global {
    use super::*;
    use crate::global::core;
    use crate::store::Store;
    use crate::error::ffi_utils;
    use wasmtime::{ValType, Mutability, RefType};

    /// Create a new WebAssembly global variable (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_global_create(
        store_ptr: *mut c_void,
        value_type: c_int,
        mutability: c_int,
        i32_value: c_int,
        i64_value: c_ulong,
        f32_value: f64, // Use f64 for F32 to avoid precision loss in FFI
        f64_value: f64,
        ref_id_present: c_int,
        ref_id: c_ulong,
        name_ptr: *const c_char,
        global_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let val_type = match value_type {
                0 => ValType::I32,
                1 => ValType::I64,
                2 => ValType::F32,
                3 => ValType::F64,
                4 => ValType::V128,
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid value type: {}", value_type),
                }),
            };

            let mutability_enum = match mutability {
                0 => Mutability::Const,
                1 => Mutability::Var,
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid mutability: {}", mutability),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id) } else { None };

            let initial_value = core::create_global_value(
                val_type.clone(),
                i32_value,
                i64_value as i64,
                f32_value as f32,
                f64_value,
                None, // v128_bytes - not supported in this call path
                ref_id_opt,
            )?;

            let name = if name_ptr.is_null() {
                None
            } else {
                Some(unsafe { ffi_utils::c_char_to_string(name_ptr)? })
            };

            let global = core::create_global(store, val_type, mutability_enum, initial_value, name)?;
            
            unsafe {
                *global_ptr = Box::into_raw(global) as *mut c_void;
            }
            
            Ok(())
        })
    }

    /// Get global variable value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_global_get(
        global_ptr: *mut c_void,
        store_ptr: *mut c_void,
        i32_value: *mut c_int,
        i64_value: *mut c_ulong,
        f32_value: *mut f64,
        f64_value: *mut f64,
        ref_id_present: *mut c_int,
        ref_id: *mut c_ulong,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let global = unsafe { core::get_global_ref(global_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let value = core::get_global_value(global, store)?;
            let (i32_val, i64_val, f32_val, f64_val, ref_id_opt) = core::extract_global_value(&value);
            
            unsafe {
                if !i32_value.is_null() { *i32_value = i32_val; }
                if !i64_value.is_null() { *i64_value = i64_val as c_ulong; }
                if !f32_value.is_null() { *f32_value = f32_val as f64; }
                if !f64_value.is_null() { *f64_value = f64_val; }
                if !ref_id_present.is_null() { *ref_id_present = if ref_id_opt.is_some() { 1 } else { 0 }; }
                if !ref_id.is_null() { *ref_id = ref_id_opt.unwrap_or(0); }
            }
            
            Ok(())
        })
    }

    /// Set global variable value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_global_set(
        global_ptr: *mut c_void,
        store_ptr: *mut c_void,
        value_type: c_int,
        i32_value: c_int,
        i64_value: c_ulong,
        f32_value: f64,
        f64_value: f64,
        ref_id_present: c_int,
        ref_id: c_ulong,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let global = unsafe { core::get_global_ref(global_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let val_type = match value_type {
                0 => ValType::I32,
                1 => ValType::I64,
                2 => ValType::F32,
                3 => ValType::F64,
                4 => ValType::V128,
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid value type: {}", value_type),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id) } else { None };

            let value = core::create_global_value(
                val_type,
                i32_value,
                i64_value as i64,
                f32_value as f32,
                f64_value,
                None, // v128_bytes - not supported in this call path
                ref_id_opt,
            )?;

            core::set_global_value(global, store, value)?;
            
            Ok(())
        })
    }

    /// Get global variable metadata (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_global_metadata(
        global_ptr: *mut c_void,
        value_type: *mut c_int,
        mutability: *mut c_int,
        name_ptr: *mut *mut c_char,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let global = unsafe { core::get_global_ref(global_ptr)? };
            let metadata = core::get_global_metadata(global);
            
            unsafe {
                if !value_type.is_null() {
                    *value_type = match metadata.value_type {
                        ValType::I32 => 0,
                        ValType::I64 => 1,
                        ValType::F32 => 2,
                        ValType::F64 => 3,
                        ValType::V128 => 4,
                        ValType::Ref(_) => {
                            // Since RefType doesn't implement PartialEq, we need to work around this
                            // by using the type's Display trait or checking if it's the default types
                            // For now, assume funcref=5, externref=6 based on wasmtime convention
                            let type_string = format!("{:?}", metadata.value_type);
                            if type_string.contains("funcref") {
                                5
                            } else {
                                6
                            }
                        },
                    };
                }
                if !mutability.is_null() {
                    *mutability = match metadata.mutability {
                        Mutability::Const => 0,
                        Mutability::Var => 1,
                    };
                }
                if !name_ptr.is_null() {
                    *name_ptr = if let Some(ref name) = metadata.name {
                        ffi_utils::string_to_c_char(name.clone())?
                    } else {
                        std::ptr::null_mut()
                    };
                }
            }
            
            Ok(())
        })
    }

    /// Destroy a global variable (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_global_destroy(global_ptr: *mut c_void) {
        unsafe {
            core::destroy_global(global_ptr);
        }
    }
}

/// Panama FFI bindings for WebAssembly tables
///
/// This module provides C-compatible functions for creating, managing,
/// and accessing WebAssembly tables with bounds checking and reference type support.
pub mod table {
    use super::*;
    use crate::table::core;
    use crate::store::Store;
    use crate::error::ffi_utils;
    use wasmtime::{ValType, RefType};

    /// Create a new WebAssembly table (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_create(
        store_ptr: *mut c_void,
        element_type: c_int,
        initial_size: c_uint,
        has_maximum: c_int,
        maximum_size: c_uint,
        name_ptr: *const c_char,
        table_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let max_size = if has_maximum != 0 { Some(maximum_size) } else { None };

            let name = if name_ptr.is_null() {
                None
            } else {
                Some(unsafe { ffi_utils::c_char_to_string(name_ptr)? })
            };

            let table = core::create_table(store, val_type, initial_size, max_size, name)?;
            
            unsafe {
                *table_ptr = Box::into_raw(table) as *mut c_void;
            }
            
            Ok(())
        })
    }

    /// Get table size (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_size(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        size: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { core::get_table_ref(table_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let table_size = core::get_table_size(table, store)?;
            
            unsafe {
                if !size.is_null() {
                    *size = table_size;
                }
            }
            
            Ok(())
        })
    }

    /// Get table element (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_get(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        index: c_uint,
        ref_id_present: *mut c_int,
        ref_id: *mut c_ulong,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { core::get_table_ref(table_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let element = core::get_table_element(table, store, index)?;
            let ref_id_opt = core::extract_table_element_ref_id(&element);
            
            unsafe {
                if !ref_id_present.is_null() {
                    *ref_id_present = if ref_id_opt.is_some() { 1 } else { 0 };
                }
                if !ref_id.is_null() {
                    *ref_id = ref_id_opt.unwrap_or(0);
                }
            }
            
            Ok(())
        })
    }

    /// Set table element (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_set(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        index: c_uint,
        element_type: c_int,
        ref_id_present: c_int,
        ref_id: c_ulong,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { core::get_table_ref(table_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id) } else { None };
            let element = core::create_table_element(val_type, ref_id_opt)?;

            core::set_table_element(table, store, index, element)?;
            
            Ok(())
        })
    }

    /// Grow table (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_grow(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        delta: c_uint,
        element_type: c_int,
        ref_id_present: c_int,
        ref_id: c_ulong,
        old_size: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { core::get_table_ref(table_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id) } else { None };
            let init_value = core::create_table_element(val_type, ref_id_opt)?;

            let previous_size = core::grow_table(table, store, delta, init_value)?;
            
            unsafe {
                if !old_size.is_null() {
                    *old_size = previous_size;
                }
            }
            
            Ok(())
        })
    }

    /// Fill table range (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_fill(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        dst: c_uint,
        len: c_uint,
        element_type: c_int,
        ref_id_present: c_int,
        ref_id: c_ulong,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { core::get_table_ref(table_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id) } else { None };
            let value = core::create_table_element(val_type, ref_id_opt)?;

            core::fill_table(table, store, dst, value, len)?;
            
            Ok(())
        })
    }

    /// Get table metadata (Panama FFI version)
    /// Updated for Table64 support - sizes are now 64-bit
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_metadata(
        table_ptr: *mut c_void,
        element_type: *mut c_int,
        initial_size: *mut c_ulong,
        has_maximum: *mut c_int,
        maximum_size: *mut c_ulong,
        is_64: *mut c_int,
        name_ptr: *mut *mut c_char,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { core::get_table_ref(table_ptr)? };
            let metadata = core::get_table_metadata(table);

            unsafe {
                if !element_type.is_null() {
                    *element_type = match metadata.element_type {
                        ValType::Ref(_) => {
                            // Since RefType doesn't implement PartialEq, we need to work around this
                            // by using the type's Display trait or checking if it's the default types
                            // For now, assume funcref=5, externref=6 based on wasmtime convention
                            let type_string = format!("{:?}", metadata.element_type);
                            if type_string.contains("funcref") {
                                5
                            } else {
                                6
                            }
                        },
                        _ => -1, // Invalid
                    };
                }
                if !initial_size.is_null() {
                    *initial_size = metadata.initial_size;
                }
                if !has_maximum.is_null() {
                    *has_maximum = if metadata.maximum_size.is_some() { 1 } else { 0 };
                }
                if !maximum_size.is_null() {
                    *maximum_size = metadata.maximum_size.unwrap_or(0);
                }
                if !is_64.is_null() {
                    *is_64 = if metadata.is_64 { 1 } else { 0 };
                }
                if !name_ptr.is_null() {
                    *name_ptr = if let Some(ref name) = metadata.name {
                        ffi_utils::string_to_c_char(name.clone())?
                    } else {
                        std::ptr::null_mut()
                    };
                }
            }

            Ok(())
        })
    }

    /// Destroy a table (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_destroy(table_ptr: *mut c_void) {
        unsafe {
            core::destroy_table(table_ptr);
        }
    }

    /// Create a new 64-bit WebAssembly table (Panama FFI version)
    /// Memory64 proposal: tables with 64-bit indices
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_create64(
        store_ptr: *mut c_void,
        element_type: c_int,
        initial_size: c_ulong,
        has_maximum: c_int,
        maximum_size: c_ulong,
        name_ptr: *const c_char,
        table_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let max_size = if has_maximum != 0 { Some(maximum_size) } else { None };

            let name = if name_ptr.is_null() {
                None
            } else {
                Some(unsafe { ffi_utils::c_char_to_string(name_ptr)? })
            };

            let table = core::create_table64(store, val_type, initial_size, max_size, name)?;

            unsafe {
                *table_ptr = Box::into_raw(table) as *mut c_void;
            }

            Ok(())
        })
    }

    /// Check if a table uses 64-bit addressing (Memory64 proposal)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_is_64(table_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_table_ref(table_ptr) } {
            Ok(table) => {
                if core::is_table_64(table) { 1 } else { 0 }
            }
            Err(_) => -1,
        }
    }

    /// Initialize table from element segment (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_init(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        instance_ptr: *mut c_void,
        dst: c_uint,
        src: c_uint,
        len: c_uint,
        segment_index: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { core::get_table_ref(table_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };
            let instance = unsafe { ffi_utils::deref_ptr::<crate::instance::Instance>(instance_ptr, "instance")? };

            table.init_from_segment(store, instance, dst, src, len, segment_index)?;
            Ok(())
        })
    }

    /// Drop an element segment (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_elem_drop(
        instance_ptr: *mut c_void,
        segment_index: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { ffi_utils::deref_ptr::<crate::instance::Instance>(instance_ptr, "instance")? };

            let segment_manager = instance.get_element_segment_manager();
            segment_manager.drop_segment(segment_index)?;
            Ok(())
        })
    }

    /// Initialize memory from data segment (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_init(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        instance_ptr: *mut c_void,
        dest_offset: c_uint,
        data_segment_index: c_uint,
        src_offset: c_uint,
        len: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            // Get raw wasmtime::Memory (not wrapped)
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };
            let instance = unsafe { ffi_utils::deref_ptr::<crate::instance::Instance>(instance_ptr, "instance")? };

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            // Create wrapped Memory from wasmtime::Memory
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            crate::memory::core::memory_init(&memory, store, instance, dest_offset, data_segment_index, src_offset, len)?;
            Ok(())
        })
    }

    /// Drop a data segment (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_data_drop(
        instance_ptr: *mut c_void,
        data_segment_index: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { ffi_utils::deref_ptr::<crate::instance::Instance>(instance_ptr, "instance")? };

            crate::memory::core::data_drop(instance, data_segment_index)?;
            Ok(())
        })
    }

    /// Copy memory within the same memory instance (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_copy(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        dest_offset: c_uint,
        src_offset: c_uint,
        len: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            crate::memory::core::memory_copy(&memory, store, dest_offset as usize, src_offset as usize, len as usize)?;
            Ok(())
        })
    }

    /// Fill memory with a byte value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_fill(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: c_uint,
        value: c_uchar,
        len: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            crate::memory::core::memory_fill(&memory, store, offset as usize, value, len as usize)?;
            Ok(())
        })
    }

    //==========================================================================================
    // Atomic Memory Operations (Panama FFI versions)
    //==========================================================================================

    /// Atomic compare-and-swap on 32-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_compare_and_swap_i32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        expected: i32,
        new_value: i32,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_compare_and_swap_i32(&memory, store, offset, expected, new_value)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic compare-and-swap on 64-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_compare_and_swap_i64(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        expected: i64,
        new_value: i64,
        result_out: *mut i64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_compare_and_swap_i64(&memory, store, offset, expected, new_value)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic load of 32-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_load_i32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_load_i32(&memory, store, offset)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic load of 64-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_load_i64(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        result_out: *mut i64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_load_i64(&memory, store, offset)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic store of 32-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_store_i32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            crate::memory::core::atomic_store_i32(&memory, store, offset, value)?;
            Ok(())
        })
    }

    /// Atomic store of 64-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_store_i64(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: i64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            crate::memory::core::atomic_store_i64(&memory, store, offset, value)?;
            Ok(())
        })
    }

    /// Atomic add on 32-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_add_i32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: i32,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_add_i32(&memory, store, offset, value)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic add on 64-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_add_i64(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: i64,
        result_out: *mut i64,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_add_i64(&memory, store, offset, value)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic AND on 32-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_and_i32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: i32,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_and_i32(&memory, store, offset, value)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic OR on 32-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_or_i32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: i32,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_or_i32(&memory, store, offset, value)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic XOR on 32-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_xor_i32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: i32,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_xor_i32(&memory, store, offset, value)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic memory fence (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_fence(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            crate::memory::core::atomic_fence(&memory, store)?;
            Ok(())
        })
    }

    /// Atomic notify/wake (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_notify(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        count: i32,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_notify(&memory, store, offset, count)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic wait on 32-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_wait32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        expected: i32,
        timeout_nanos: i64,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_wait32(&memory, store, offset, expected, timeout_nanos)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Atomic wait on 64-bit value (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_memory_atomic_wait64(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        expected: i64,
        timeout_nanos: i64,
        result_out: *mut i32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let wasmtime_memory = unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

            if result_out.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Result output pointer cannot be null".to_string(),
                });
            }

            // Get memory type information from the store
            let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
            let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

            let result = crate::memory::core::atomic_wait64(&memory, store, offset, expected, timeout_nanos)?;

            unsafe {
                *result_out = result;
            }

            Ok(())
        })
    }

    /// Copy elements within a table (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_copy(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        dst: c_uint,
        src: c_uint,
        len: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

            table.copy_within(store, dst, src, len)?;
            Ok(())
        })
    }

    /// Copy elements from another table (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_table_copy_from(
        dst_table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        dst: c_uint,
        src_table_ptr: *mut c_void,
        src: c_uint,
        len: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let dst_table = unsafe { crate::table::core::get_table_ref(dst_table_ptr as *const c_void)? };
            let src_table = unsafe { crate::table::core::get_table_ref(src_table_ptr as *const c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

            dst_table.copy_from(store, dst, src_table, src, len)?;
            Ok(())
        })
    }

    /// Get the last error message as a C string
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_get_last_error_message() -> *mut c_char {
        crate::error::ffi_utils::get_last_error_message()
    }

    /// Free an error message returned by wasmtime4j_get_last_error_message
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_free_error_message(message: *mut c_char) {
        if !message.is_null() {
            use std::ffi::CString;
            unsafe {
                let _ = CString::from_raw(message);
            }
        }
    }

    /// Clear any stored error state in the native library
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_clear_error_state() {
        crate::error::ffi_utils::clear_last_error();
    }
}

/// Panama FFI bindings for WebAssembly function operations
///
/// This module provides C-compatible functions for creating, inspecting,
/// and calling WebAssembly functions through the Panama Foreign Function Interface.
pub mod function {
    use super::*;
    use crate::instance::core;
    use crate::store::Store;
    use crate::error::ffi_utils;

    /// Get function from instance export (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_func_get(
        instance_ptr: *mut c_void,
        store_ptr: *mut c_void,
        name: *const c_char,
        func_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let instance = unsafe { core::get_instance_ref(instance_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
            let name_str = unsafe { ffi_utils::c_char_to_string(name)? };
            
            let func_opt = core::get_function_export(instance, store, &name_str)?;
            
            match func_opt {
                Some(func) => {
                    unsafe {
                        *func_ptr = Box::into_raw(Box::new(func)) as *mut c_void;
                    }
                    Ok(())
                }
                None => Err(crate::error::WasmtimeError::ExportNotFound {
                    name: name_str,
                }),
            }
        })
    }

    /// Get function parameter types (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_func_get_param_types(
        func_ptr: *mut c_void,
        store_ptr: *mut c_void,
        types_ptr: *mut *mut c_int,
        count_ptr: *mut usize,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let func = unsafe { core::get_function_ref(func_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let param_types = core::get_function_param_types(func, store)?;
            let count = param_types.len();
            
            if count > 0 {
                let types_array = param_types.into_boxed_slice();
                unsafe {
                    *types_ptr = Box::into_raw(types_array) as *mut c_int;
                    *count_ptr = count;
                }
            } else {
                unsafe {
                    *types_ptr = std::ptr::null_mut();
                    *count_ptr = 0;
                }
            }
            
            Ok(())
        })
    }

    /// Get function result types (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_func_get_result_types(
        func_ptr: *mut c_void,
        store_ptr: *mut c_void,
        types_ptr: *mut *mut c_int,
        count_ptr: *mut usize,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let func = unsafe { core::get_function_ref(func_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let result_types = core::get_function_result_types(func, store)?;
            let count = result_types.len();
            
            if count > 0 {
                let types_array = result_types.into_boxed_slice();
                unsafe {
                    *types_ptr = Box::into_raw(types_array) as *mut c_int;
                    *count_ptr = count;
                }
            } else {
                unsafe {
                    *types_ptr = std::ptr::null_mut();
                    *count_ptr = 0;
                }
            }
            
            Ok(())
        })
    }

    /// Call WebAssembly function (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_func_call(
        func_ptr: *mut c_void,
        store_ptr: *mut c_void,
        params_ptr: *const c_void,
        param_count: usize,
        results_ptr: *mut c_void,
        result_count: usize,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let func = unsafe { core::get_function_ref(func_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
            
            // Convert parameters from C representation to Rust WasmValue
            let params = if param_count > 0 {
                unsafe { 
                    core::convert_params_from_ffi(params_ptr, param_count)?
                }
            } else {
                Vec::new()
            };
            
            // Call the function
            let results = core::call_function(func, store, &params)?;
            
            // Convert results back to C representation
            if result_count > 0 && !results.is_empty() {
                unsafe {
                    core::convert_results_to_ffi(&results, results_ptr, result_count)?;
                }
            }
            
            Ok(())
        })
    }

    /// Get function type (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_func_type(
        func_ptr: *mut c_void,
        store_ptr: *mut c_void,
    ) -> *mut c_void {
        match ffi_utils::ffi_try_ptr_result(|| {
            let func = unsafe { core::get_function_ref(func_ptr)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let func_type = core::get_function_type(func, store)?;
            Ok(Box::new(func_type))
        }) {
            Ok(boxed_type) => Box::into_raw(boxed_type) as *mut c_void,
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Free function type (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_func_type_destroy(func_type_ptr: *mut c_void) {
        if !func_type_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(func_type_ptr as *mut wasmtime::FuncType);
            }
        }
    }

    /// Free parameter/result types array (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_func_free_types_array(types_ptr: *mut c_int, count: usize) {
        if !types_ptr.is_null() && count > 0 {
            unsafe {
                let _ = Box::from_raw(std::slice::from_raw_parts_mut(types_ptr, count));
            }
        }
    }

    /// Destroy a function handle (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_func_destroy(func_ptr: *mut c_void) {
        if !func_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(func_ptr as *mut wasmtime::Func);
            }
        }
    }
}

/// Panama FFI bindings for experimental WebAssembly features
pub mod experimental_features {
    use super::*;
    use crate::experimental_features::core;
    use crate::error::ffi_utils;

    /// Create experimental features configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_create_config() -> *mut c_void {
        ffi_utils::ffi_try_ptr(|| core::create_experimental_features_config())
    }

    /// Create experimental features configuration with all features enabled (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_create_all_config() -> *mut c_void {
        ffi_utils::ffi_try_ptr(|| core::create_all_experimental_config())
    }

    /// Enable stack switching in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_stack_switching(
        config_ptr: *mut c_void,
        stack_size: c_ulong,
        max_stacks: c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe { core::enable_stack_switching(config_ptr, stack_size as u64, max_stacks as u32) }
        })
    }

    /// Enable call/cc in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_call_cc(
        config_ptr: *mut c_void,
        max_continuations: c_uint,
        storage_strategy: c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                core::enable_call_cc(
                    config_ptr,
                    max_continuations as u32,
                    storage_strategy
                )
            }
        })
    }

    /// Enable extended constant expressions in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_extended_const_expressions(
        config_ptr: *mut c_void,
        import_based: c_int,
        global_deps: c_int,
        folding_level: c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                core::enable_extended_const_expressions(
                    config_ptr,
                    import_based,
                    global_deps,
                    folding_level
                )
            }
        })
    }

    /// Apply experimental features to Wasmtime config (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_apply_features(
        experimental_config_ptr: *const c_void,
        wasmtime_config_ptr: *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                // Get mutable reference to Wasmtime Config
                let wasmtime_config = &mut *(wasmtime_config_ptr as *mut wasmtime::Config);
                core::apply_experimental_features(
                    experimental_config_ptr,
                    wasmtime_config
                )
            }
        })
    }

    /// Enable flexible vectors in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_flexible_vectors(
        config_ptr: *mut c_void,
        dynamic_sizing: c_int,
        auto_vectorization: c_int,
        simd_integration: c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                core::enable_flexible_vectors(
                    config_ptr,
                    dynamic_sizing,
                    auto_vectorization,
                    simd_integration
                )
            }
        })
    }

    /// Enable string imports in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_string_imports(
        config_ptr: *mut c_void,
        encoding_format: c_int,
        string_interning: c_int,
        lazy_decoding: c_int,
        js_interop: c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                core::enable_string_imports(
                    config_ptr,
                    encoding_format,
                    string_interning,
                    lazy_decoding,
                    js_interop
                )
            }
        })
    }

    /// Enable resource types in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_resource_types(
        config_ptr: *mut c_void,
        automatic_cleanup: c_int,
        reference_counting: c_int,
        cleanup_strategy: c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                core::enable_resource_types(
                    config_ptr,
                    automatic_cleanup,
                    reference_counting,
                    cleanup_strategy
                )
            }
        })
    }

    /// Enable type imports in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_type_imports(
        config_ptr: *mut c_void,
        validation_strategy: c_int,
        resolution_mechanism: c_int,
        structural_compatibility: c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                core::enable_type_imports(
                    config_ptr,
                    validation_strategy,
                    resolution_mechanism,
                    structural_compatibility
                )
            }
        })
    }

    /// Enable shared-everything threads in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_shared_everything_threads(
        config_ptr: *mut c_void,
        min_threads: c_uint,
        max_threads: c_uint,
        global_state_sharing: c_int,
        atomic_operations: c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                core::enable_shared_everything_threads(
                    config_ptr,
                    min_threads as u32,
                    max_threads as u32,
                    global_state_sharing,
                    atomic_operations
                )
            }
        })
    }

    /// Enable custom page sizes in experimental configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_enable_custom_page_sizes(
        config_ptr: *mut c_void,
        page_size: c_uint,
        strategy: c_int,
        strict_alignment: c_int,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            unsafe {
                core::enable_custom_page_sizes(
                    config_ptr,
                    page_size as u32,
                    strategy,
                    strict_alignment
                )
            }
        })
    }

    /// Get feature detection capabilities (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_get_feature_support(feature_id: c_int) -> c_int {
        // Feature detection based on Wasmtime capabilities
        match feature_id {
            0 => 1,  // Stack switching (experimental support)
            1 => 0,  // Call/CC (not yet supported)
            2 => 1,  // Extended const expressions (partial support)
            3 => 1,  // Memory64 extended (partial support)
            4 => 0,  // Custom page sizes (not yet supported)
            5 => 0,  // Shared-everything threads (not yet supported)
            6 => 0,  // Type imports (not yet supported)
            7 => 0,  // String imports (not yet supported)
            8 => 0,  // Resource types (not yet supported)
            9 => 0,  // Interface types (not yet supported)
            10 => 0, // Flexible vectors (not yet supported)
            _ => 0
        }
    }

    /// Destroy experimental features configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_experimental_destroy_config(config_ptr: *mut c_void) {
        if !config_ptr.is_null() {
            unsafe { core::destroy_experimental_features_config(config_ptr) }
        }
    }
}

/// Panama FFI bindings for source map and debugging operations
pub mod sourcemap {
    use super::*;
    use crate::sourcemap::*;
    use crate::error::ffi_utils;
    use std::ffi::{CStr, CString};

    /// Create a new source map integration system (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_integration_create() -> *mut c_void {
        ffi_utils::ffi_try_ptr(|| {
            let integration = SourceMapIntegration::new();
            Ok(Box::new(integration))
        })
    }

    /// Create a source map integration system with custom cache size (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_integration_create_with_cache_size(cache_size: c_uint) -> *mut c_void {
        ffi_utils::ffi_try_ptr(|| {
            let integration = SourceMapIntegration::with_cache_size(cache_size as usize);
            Ok(Box::new(integration))
        })
    }

    /// Load and parse a source map from JSON data (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_integration_load_source_map(
        integration_ptr: *const c_void,
        json_data: *const c_char,
    ) -> *mut c_void {
        if integration_ptr.is_null() || json_data.is_null() {
            return std::ptr::null_mut();
        }

        ffi_utils::ffi_try_ptr(|| {
            let integration = unsafe { &*(integration_ptr as *const SourceMapIntegration) };
            let json_str = unsafe { CStr::from_ptr(json_data) }.to_str().map_err(|e| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid UTF-8 in JSON data: {}", e),
                }
            })?;

            let source_map = integration.load_source_map(json_str)?;
            Ok(Box::new(source_map))
        })
    }

    /// Load source file content (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_integration_load_source_file(
        integration_ptr: *const c_void,
        path: *const c_char,
    ) -> *mut c_char {
        if integration_ptr.is_null() || path.is_null() {
            return std::ptr::null_mut();
        }

        let result = (|| -> WasmtimeResult<*mut c_char> {
            let integration = unsafe { &*(integration_ptr as *const SourceMapIntegration) };
            let path_str = unsafe { CStr::from_ptr(path) }.to_str().map_err(|e| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid UTF-8 in path: {}", e),
                }
            })?;

            let content = integration.load_source_file(path_str)?;
            let content_cstr = CString::new(content.as_str())
                .map_err(|e| WasmtimeError::InvalidParameter { message: format!("Content contains null bytes: {}", e) })?;
            Ok(content_cstr.into_raw())
        })();

        match result {
            Ok(ptr) => ptr,
            Err(e) => {
                ffi_utils::set_last_error(e);
                std::ptr::null_mut()
            }
        }
    }

    /// Clear all caches (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_integration_clear_caches(integration_ptr: *const c_void) {
        if !integration_ptr.is_null() {
            let integration = unsafe { &*(integration_ptr as *const SourceMapIntegration) };
            integration.clear_caches();
        }
    }

    /// Check if source map support is available (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_integration_is_source_map_supported() -> c_int {
        1 // Always supported in our implementation
    }

    /// Check if DWARF support is available (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_integration_is_dwarf_supported() -> c_int {
        1 // Always supported in our implementation
    }

    /// Free a C string allocated by this library (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_free_string(str_ptr: *mut c_char) {
        if !str_ptr.is_null() {
            unsafe {
                let _ = CString::from_raw(str_ptr);
            }
        }
    }

    /// Destroy source map integration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_sourcemap_integration_destroy(integration_ptr: *mut c_void) {
        if !integration_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(integration_ptr as *mut SourceMapIntegration);
            }
        }
    }
}

/// Caller context FFI functions for Panama
pub mod caller {
    use super::*;
    use crate::caller::core;
    use crate::error::ffi_utils;
    use wasmtime::Caller as WasmtimeCaller;
    use crate::store::StoreData;

    /// Get fuel consumed by the caller if fuel metering is enabled (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_get_fuel(
        caller_ptr: *mut c_void,
        fuel_out: *mut c_ulong,
    ) -> c_int {
        if caller_ptr.is_null() || fuel_out.is_null() {
            return -1; // Error: null pointer
        }

        let result = (|| -> WasmtimeResult<c_int> {
            let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
            match core::caller_get_fuel(caller)? {
                Some(fuel) => {
                    unsafe { *fuel_out = fuel; }
                    Ok(0) // Success
                }
                None => Ok(-1), // Fuel metering not enabled
            }
        })();

        match result {
            Ok(code) => code,
            Err(e) => {
                ffi_utils::set_last_error(e);
                -1
            }
        }
    }

    /// Get fuel remaining in the caller if fuel metering is enabled (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_get_fuel_remaining(
        caller_ptr: *mut c_void,
        fuel_out: *mut c_ulong,
    ) -> c_int {
        if caller_ptr.is_null() || fuel_out.is_null() {
            return -1; // Error: null pointer
        }

        ffi_utils::ffi_try_code(|| {
            let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
            match core::caller_get_fuel_remaining(caller)? {
                Some(fuel) => {
                    unsafe { *fuel_out = fuel; }
                    Ok(()) // Success
                }
                None => Ok(()), // Fuel metering not enabled
            }
        })
    }

    /// Add fuel to the caller (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_add_fuel(
        caller_ptr: *mut c_void,
        fuel: c_ulong,
    ) -> c_int {
        if caller_ptr.is_null() {
            return -1; // Error: null pointer
        }

        ffi_utils::ffi_try_code(|| {
            let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
            core::caller_add_fuel(caller, fuel)?;
            Ok(()) // Success
        })
    }

    /// Set epoch deadline for the caller (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_set_epoch_deadline(
        caller_ptr: *mut c_void,
        deadline: c_ulong,
    ) -> c_int {
        if caller_ptr.is_null() {
            return -1; // Error: null pointer
        }

        ffi_utils::ffi_try_code(|| {
            let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
            core::caller_set_epoch_deadline(caller, deadline)?;
            Ok(()) // Success
        })
    }

    /// Check if the caller has an active epoch deadline (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_has_epoch_deadline(
        caller_ptr: *mut c_void,
    ) -> c_int {
        if caller_ptr.is_null() {
            return -1; // Error: null pointer
        }

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_has_epoch_deadline(caller) {
            Ok(true) => 1, // Has deadline
            Ok(false) => 0, // No deadline
            Err(_) => -1, // Error occurred
        }
    }

    /// Check if caller has an export with the given name (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_has_export(
        caller_ptr: *mut c_void,
        name: *const c_char,
    ) -> c_int {
        if caller_ptr.is_null() || name.is_null() {
            return -1; // Error: null pointer
        }

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
            Ok(s) => s,
            Err(_) => return -1, // Invalid UTF-8
        };

        match core::caller_has_export(caller, name_str) {
            Ok(true) => 1, // Has export
            Ok(false) => 0, // No export
            Err(_) => -1, // Error occurred
        }
    }

    /// Get memory export from caller by name (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_get_memory(
        caller_ptr: *mut c_void,
        name: *const c_char,
        memory_out: *mut *mut c_void,
    ) -> c_int {
        if caller_ptr.is_null() || name.is_null() || memory_out.is_null() {
            return -1; // Error: null pointer
        }

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
            Ok(s) => s,
            Err(_) => return -1, // Invalid UTF-8
        };

        match core::caller_get_memory(caller, name_str) {
            Ok(Some(memory)) => {
                unsafe { *memory_out = Box::into_raw(Box::new(memory)) as *mut c_void; }
                1 // Memory found
            }
            Ok(None) => {
                unsafe { *memory_out = std::ptr::null_mut(); }
                0 // No memory export with this name
            }
            Err(_) => -1, // Error occurred
        }
    }

    /// Get function export from caller by name (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_get_function(
        caller_ptr: *mut c_void,
        name: *const c_char,
        function_out: *mut *mut c_void,
    ) -> c_int {
        if caller_ptr.is_null() || name.is_null() || function_out.is_null() {
            return -1; // Error: null pointer
        }

        ffi_utils::ffi_try_code(|| {
            let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
            let name_str = unsafe { std::ffi::CStr::from_ptr(name) }.to_str()
                .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

            match core::caller_get_function(caller, name_str)? {
                Some(function) => {
                    unsafe { *function_out = Box::into_raw(Box::new(function)) as *mut c_void; }
                    Ok(()) // Success
                }
                None => {
                    unsafe { *function_out = std::ptr::null_mut(); }
                    Ok(()) // No function export with this name
                }
            }
        })
    }

    /// Get global export from caller by name (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_get_global(
        caller_ptr: *mut c_void,
        name: *const c_char,
        global_out: *mut *mut c_void,
    ) -> c_int {
        if caller_ptr.is_null() || name.is_null() || global_out.is_null() {
            return -1; // Error: null pointer
        }

        ffi_utils::ffi_try_code(|| {
            let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
            let name_str = unsafe { std::ffi::CStr::from_ptr(name) }.to_str()
                .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

            match core::caller_get_global(caller, name_str)? {
                Some(global) => {
                    unsafe { *global_out = Box::into_raw(Box::new(global)) as *mut c_void; }
                    Ok(()) // Success
                }
                None => {
                    unsafe { *global_out = std::ptr::null_mut(); }
                    Ok(()) // No global export with this name
                }
            }
        })
    }

    /// Get table export from caller by name (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_caller_get_table(
        caller_ptr: *mut c_void,
        name: *const c_char,
        table_out: *mut *mut c_void,
    ) -> c_int {
        if caller_ptr.is_null() || name.is_null() || table_out.is_null() {
            return -1; // Error: null pointer
        }

        ffi_utils::ffi_try_code(|| {
            let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
            let name_str = unsafe { std::ffi::CStr::from_ptr(name) }.to_str()
                .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

            match core::caller_get_table(caller, name_str)? {
                Some(table) => {
                    unsafe { *table_out = Box::into_raw(Box::new(table)) as *mut c_void; }
                    Ok(()) // Success
                }
                None => {
                    unsafe { *table_out = std::ptr::null_mut(); }
                    Ok(()) // No table export with this name
                }
            }
        })
    }
}

/// Panama FFI bindings for Linker operations
pub mod linker {
    use super::*;
    use crate::linker::ffi_core as linker_core;
    use crate::error::ffi_utils;
    use crate::hostfunc::{HostFunction, HostFunctionCallback};
    use crate::instance::WasmValue;
    use wasmtime::{ValType, FuncType, RefType};
    use std::sync::Arc;

    /// Type for Panama callback function pointer
    ///
    /// This function pointer is called from Rust back into Java when a host function is invoked.
    ///
    /// Parameters:
    /// - callback_id: i64 - The unique identifier for this callback
    /// - params_ptr: *const c_void - Pointer to array of WasmValue parameters
    /// - params_len: c_uint - Number of parameters
    /// - results_ptr: *mut c_void - Pointer to buffer for WasmValue results
    /// - results_len: c_uint - Expected number of results
    ///
    /// Returns: c_int - 0 on success, non-zero on error
    type PanamaHostFunctionCallback = extern "C" fn(
        callback_id: i64,
        params_ptr: *const c_void,
        params_len: c_uint,
        results_ptr: *mut c_void,
        results_len: c_uint,
    ) -> c_int;

    /// Panama-specific host function callback implementation
    struct PanamaHostFunctionCallbackImpl {
        callback_fn: PanamaHostFunctionCallback,
        callback_id: i64,
        result_count: usize,
    }

    impl HostFunctionCallback for PanamaHostFunctionCallbackImpl {
        fn execute(&self, params: &[WasmValue]) -> crate::WasmtimeResult<Vec<WasmValue>> {
            log::debug!("Panama host function callback - callback_id={}, params.len={}", self.callback_id, params.len());

            // Allocate result buffer based on function signature
            let expected_results = self.result_count;
            let mut results = vec![WasmValue::I32(0); expected_results];

            // Call the Panama function pointer
            let result_code = (self.callback_fn)(
                self.callback_id,
                params.as_ptr() as *const c_void,
                params.len() as c_uint,
                results.as_mut_ptr() as *mut c_void,
                expected_results as c_uint,
            );

            if result_code != 0 {
                return Err(crate::error::WasmtimeError::Function {
                    message: format!("Panama host function callback failed with code: {}", result_code),
                });
            }

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
            let module_name_str = unsafe { std::ffi::CStr::from_ptr(module_name) }
                .to_str()
                .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;
            let name_str = unsafe { std::ffi::CStr::from_ptr(name) }
                .to_str()
                .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

            // Convert parameter types
            let param_slice = unsafe { std::slice::from_raw_parts(param_types, param_count as usize) };
            let param_val_types: Vec<ValType> = param_slice.iter()
                .map(|&t| int_to_valtype(t))
                .collect::<Result<Vec<_>, _>>()?;

            // Convert return types
            let return_slice = unsafe { std::slice::from_raw_parts(return_types, return_count as usize) };
            let return_val_types: Vec<ValType> = return_slice.iter()
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
            let func_type = FuncType::new(
                engine,
                param_val_types,
                return_val_types
            );

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
                std::sync::Weak::new(), // Empty weak ref for now
                Box::new(callback),
            )?;

            // Register host function
            let host_func_clone = (*host_func).clone();
            linker.define_host_function(module_name_str, name_str, host_func.func_type().clone(), host_func_clone)?;

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
        use std::ffi::CStr;
        use crate::error::ffi_utils;

        ffi_utils::ffi_try_code(|| {
            unsafe {
                // Validate pointers
                if linker_ptr.is_null() || from_module.is_null() || from_name.is_null()
                    || to_module.is_null() || to_name.is_null() {
                    return Err(crate::error::WasmtimeError::Linker {
                        message: "Null pointer in linker alias parameters".to_string(),
                    });
                }

                // Convert C strings to Rust strings
                let from_module_str = CStr::from_ptr(from_module)
                    .to_str()
                    .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;
                let from_name_str = CStr::from_ptr(from_name)
                    .to_str()
                    .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;
                let to_module_str = CStr::from_ptr(to_module)
                    .to_str()
                    .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;
                let to_name_str = CStr::from_ptr(to_name)
                    .to_str()
                    .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

                // Get linker reference
                let linker = linker_core::get_linker_ref(linker_ptr)?;

                // Lock linker
                let mut linker_lock = linker.inner()?;

                // Use Wasmtime's alias method
                linker_lock.alias(from_module_str, from_name_str, to_module_str, to_name_str)
                    .map_err(|e| crate::error::WasmtimeError::Linker {
                        message: format!("Failed to create alias from {}::{} to {}::{}: {}",
                            from_module_str, from_name_str, to_module_str, to_name_str, e),
                    })?;

                Ok(())
            }
        })
    }

    /// Destroy a linker (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_linker_destroy(linker_ptr: *mut c_void) {
        unsafe {
            linker_core::destroy_linker(linker_ptr);
        }
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
        use std::ffi::CStr;
        use crate::error::ffi_utils;
        use wasmtime::AsContextMut;

        ffi_utils::ffi_try_code(|| {
            unsafe {
                // Convert C strings to Rust strings
                let module_name_str = CStr::from_ptr(module_name)
                    .to_str()
                    .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;
                let name_str = CStr::from_ptr(name)
                    .to_str()
                    .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

                // Get linker reference
                let linker = linker_core::get_linker_ref(linker_ptr)?;

                // Get store reference
                let store = crate::store::core::get_store_mut(store_ptr)?;

                // Get global reference
                let global = crate::global::core::get_global_ref(global_ptr)?;

                // Lock linker and global
                let mut linker_lock = linker.inner()?;
                let wasmtime_global_arc = global.wasmtime_global();
                let wasmtime_global_lock = wasmtime_global_arc.lock()
                    .map_err(|e| crate::error::WasmtimeError::Concurrency {
                        message: format!("Failed to lock global: {}", e),
                    })?;

                // Lock store and define global
                let mut store_lock = store.lock_store();
                linker_lock.define(
                    &mut (*store_lock).as_context_mut(),
                    module_name_str,
                    name_str,
                    wasmtime::Extern::Global(*wasmtime_global_lock),
                ).map_err(|e| crate::error::WasmtimeError::Linker {
                    message: format!("Failed to define global '{}::{}': {}", module_name_str, name_str, e),
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
                let param_slice = unsafe {
                    std::slice::from_raw_parts(param_types, param_count as usize)
                };
                param_slice
                    .iter()
                    .map(|&t| int_to_valtype(t))
                    .collect::<Result<Vec<_>, _>>()?
            } else {
                Vec::new()
            };

            let return_val_types: Vec<ValType> = if return_count > 0 && !return_types.is_null() {
                let return_slice = unsafe {
                    std::slice::from_raw_parts(return_types, return_count as usize)
                };
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
            let func_type = FuncType::new(
                wasmtime_engine,
                param_val_types,
                return_val_types,
            );

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
    pub extern "C" fn wasmtime4j_panama_function_reference_destroy(
        registry_id: u64,
    ) -> c_int {
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
    pub extern "C" fn wasmtime4j_panama_function_reference_is_valid(
        registry_id: u64,
    ) -> c_int {
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
}

/// Panama FFI bindings for trap introspection
///
/// This module provides C-compatible functions for extracting detailed trap information
/// from WebAssembly runtime errors, enabling better error reporting and debugging.
pub mod trap {
    use super::*;
    use std::ffi::CString;

    /// Trap code constants matching Java TrapException.TrapType enum ordinals
    pub mod trap_codes {
        /// Stack overflow trap
        pub const STACK_OVERFLOW: i32 = 0;
        /// Memory out of bounds trap
        pub const MEMORY_OUT_OF_BOUNDS: i32 = 1;
        /// Heap misaligned (atomic operation) trap
        pub const HEAP_MISALIGNED: i32 = 2;
        /// Table out of bounds trap
        pub const TABLE_OUT_OF_BOUNDS: i32 = 3;
        /// Indirect call to null table entry trap
        pub const INDIRECT_CALL_TO_NULL: i32 = 4;
        /// Bad function signature trap
        pub const BAD_SIGNATURE: i32 = 5;
        /// Integer overflow trap
        pub const INTEGER_OVERFLOW: i32 = 6;
        /// Integer division by zero trap
        pub const INTEGER_DIVISION_BY_ZERO: i32 = 7;
        /// Bad conversion to integer trap
        pub const BAD_CONVERSION_TO_INTEGER: i32 = 8;
        /// Unreachable code reached trap
        pub const UNREACHABLE_CODE_REACHED: i32 = 9;
        /// Execution interrupted trap
        pub const INTERRUPT: i32 = 10;
        /// Out of fuel trap
        pub const OUT_OF_FUEL: i32 = 11;
        /// Null reference trap (GC proposal)
        pub const NULL_REFERENCE: i32 = 12;
        /// Array out of bounds trap (GC proposal)
        pub const ARRAY_OUT_OF_BOUNDS: i32 = 13;
        /// Unknown trap type
        pub const UNKNOWN: i32 = 14;
    }

    /// Parse a trap code from an error message string
    ///
    /// This function analyzes the error message to determine the trap type.
    /// It returns the trap code constant that matches the Java TrapType enum.
    ///
    /// # Parameters
    /// - error_message: Pointer to null-terminated C string containing the error message
    ///
    /// # Returns
    /// The trap code constant, or UNKNOWN if the trap type cannot be determined
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_trap_parse_code(
        error_message: *const c_char,
    ) -> c_int {
        if error_message.is_null() {
            return trap_codes::UNKNOWN;
        }

        let msg = match unsafe { std::ffi::CStr::from_ptr(error_message) }.to_str() {
            Ok(s) => s.to_lowercase(),
            Err(_) => return trap_codes::UNKNOWN,
        };

        // Parse trap type from message content using Wasmtime's error messages
        if msg.contains("stack overflow") || msg.contains("call stack exhausted") {
            trap_codes::STACK_OVERFLOW
        } else if msg.contains("out of bounds memory") || msg.contains("memory access out of bounds") {
            trap_codes::MEMORY_OUT_OF_BOUNDS
        } else if msg.contains("misaligned") && (msg.contains("atomic") || msg.contains("heap")) {
            trap_codes::HEAP_MISALIGNED
        } else if msg.contains("out of bounds table") || msg.contains("table access out of bounds") {
            trap_codes::TABLE_OUT_OF_BOUNDS
        } else if msg.contains("indirect call") && msg.contains("null") {
            trap_codes::INDIRECT_CALL_TO_NULL
        } else if msg.contains("signature mismatch") || msg.contains("indirect call type mismatch") {
            trap_codes::BAD_SIGNATURE
        } else if msg.contains("integer overflow") {
            trap_codes::INTEGER_OVERFLOW
        } else if msg.contains("integer divide by zero") || msg.contains("division by zero") {
            trap_codes::INTEGER_DIVISION_BY_ZERO
        } else if msg.contains("invalid conversion") || msg.contains("bad conversion") {
            trap_codes::BAD_CONVERSION_TO_INTEGER
        } else if msg.contains("unreachable") {
            trap_codes::UNREACHABLE_CODE_REACHED
        } else if msg.contains("interrupt") || msg.contains("epoch") {
            trap_codes::INTERRUPT
        } else if msg.contains("fuel") && (msg.contains("out of") || msg.contains("ran out")) {
            trap_codes::OUT_OF_FUEL
        } else if msg.contains("null reference") || msg.contains("null funcref") {
            trap_codes::NULL_REFERENCE
        } else if msg.contains("array") && msg.contains("out of bounds") {
            trap_codes::ARRAY_OUT_OF_BOUNDS
        } else {
            trap_codes::UNKNOWN
        }
    }

    /// Get the trap code name as a string
    ///
    /// # Parameters
    /// - trap_code: The trap code constant
    ///
    /// # Returns
    /// Pointer to a static null-terminated string with the trap code name,
    /// or null if the code is invalid
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_trap_code_name(
        trap_code: c_int,
    ) -> *const c_char {
        static STACK_OVERFLOW: &[u8] = b"STACK_OVERFLOW\0";
        static MEMORY_OUT_OF_BOUNDS: &[u8] = b"MEMORY_OUT_OF_BOUNDS\0";
        static HEAP_MISALIGNED: &[u8] = b"HEAP_MISALIGNED\0";
        static TABLE_OUT_OF_BOUNDS: &[u8] = b"TABLE_OUT_OF_BOUNDS\0";
        static INDIRECT_CALL_TO_NULL: &[u8] = b"INDIRECT_CALL_TO_NULL\0";
        static BAD_SIGNATURE: &[u8] = b"BAD_SIGNATURE\0";
        static INTEGER_OVERFLOW: &[u8] = b"INTEGER_OVERFLOW\0";
        static INTEGER_DIVISION_BY_ZERO: &[u8] = b"INTEGER_DIVISION_BY_ZERO\0";
        static BAD_CONVERSION_TO_INTEGER: &[u8] = b"BAD_CONVERSION_TO_INTEGER\0";
        static UNREACHABLE_CODE_REACHED: &[u8] = b"UNREACHABLE_CODE_REACHED\0";
        static INTERRUPT: &[u8] = b"INTERRUPT\0";
        static OUT_OF_FUEL: &[u8] = b"OUT_OF_FUEL\0";
        static NULL_REFERENCE: &[u8] = b"NULL_REFERENCE\0";
        static ARRAY_OUT_OF_BOUNDS: &[u8] = b"ARRAY_OUT_OF_BOUNDS\0";
        static UNKNOWN: &[u8] = b"UNKNOWN\0";

        let name = match trap_code {
            trap_codes::STACK_OVERFLOW => STACK_OVERFLOW,
            trap_codes::MEMORY_OUT_OF_BOUNDS => MEMORY_OUT_OF_BOUNDS,
            trap_codes::HEAP_MISALIGNED => HEAP_MISALIGNED,
            trap_codes::TABLE_OUT_OF_BOUNDS => TABLE_OUT_OF_BOUNDS,
            trap_codes::INDIRECT_CALL_TO_NULL => INDIRECT_CALL_TO_NULL,
            trap_codes::BAD_SIGNATURE => BAD_SIGNATURE,
            trap_codes::INTEGER_OVERFLOW => INTEGER_OVERFLOW,
            trap_codes::INTEGER_DIVISION_BY_ZERO => INTEGER_DIVISION_BY_ZERO,
            trap_codes::BAD_CONVERSION_TO_INTEGER => BAD_CONVERSION_TO_INTEGER,
            trap_codes::UNREACHABLE_CODE_REACHED => UNREACHABLE_CODE_REACHED,
            trap_codes::INTERRUPT => INTERRUPT,
            trap_codes::OUT_OF_FUEL => OUT_OF_FUEL,
            trap_codes::NULL_REFERENCE => NULL_REFERENCE,
            trap_codes::ARRAY_OUT_OF_BOUNDS => ARRAY_OUT_OF_BOUNDS,
            _ => UNKNOWN,
        };

        name.as_ptr() as *const c_char
    }

    /// Check if an error message indicates a trap condition
    ///
    /// # Parameters
    /// - error_message: Pointer to null-terminated C string containing the error message
    ///
    /// # Returns
    /// 1 if the message indicates a trap, 0 otherwise
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_trap_is_trap(
        error_message: *const c_char,
    ) -> c_int {
        if error_message.is_null() {
            return 0;
        }

        let msg = match unsafe { std::ffi::CStr::from_ptr(error_message) }.to_str() {
            Ok(s) => s.to_lowercase(),
            Err(_) => return 0,
        };

        // Check for common trap indicators in Wasmtime error messages
        let is_trap = msg.contains("wasm trap")
            || msg.contains("stack overflow")
            || msg.contains("out of bounds")
            || msg.contains("unreachable")
            || msg.contains("divide by zero")
            || msg.contains("division by zero")
            || msg.contains("integer overflow")
            || msg.contains("signature mismatch")
            || msg.contains("indirect call")
            || msg.contains("out of fuel")
            || msg.contains("epoch")
            || msg.contains("null reference")
            || msg.contains("misaligned");

        if is_trap { 1 } else { 0 }
    }

    /// Extract function name from a backtrace line if present
    ///
    /// # Parameters
    /// - backtrace_line: Pointer to null-terminated C string containing a backtrace line
    /// - out_buffer: Buffer to write the function name to
    /// - buffer_size: Size of the output buffer
    ///
    /// # Returns
    /// The length of the function name written (excluding null terminator),
    /// or -1 if no function name was found or an error occurred
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_trap_extract_function_name(
        backtrace_line: *const c_char,
        out_buffer: *mut c_char,
        buffer_size: usize,
    ) -> c_int {
        if backtrace_line.is_null() || out_buffer.is_null() || buffer_size == 0 {
            return -1;
        }

        let line = match unsafe { std::ffi::CStr::from_ptr(backtrace_line) }.to_str() {
            Ok(s) => s,
            Err(_) => return -1,
        };

        // Try to extract function name from patterns like:
        // "  0: 0x123 - <function_name>" or "function_name()"
        let func_name = if let Some(idx) = line.find(" - ") {
            let after = &line[idx + 3..];
            if let Some(end) = after.find(|c: char| c == '(' || c.is_whitespace()) {
                &after[..end]
            } else {
                after.trim()
            }
        } else if let Some(idx) = line.find("!") {
            // Pattern: "module!function_name"
            let after = &line[idx + 1..];
            if let Some(end) = after.find(|c: char| c == '(' || c.is_whitespace() || c == '+') {
                &after[..end]
            } else {
                after.trim()
            }
        } else {
            return -1;
        };

        if func_name.is_empty() {
            return -1;
        }

        // Copy to output buffer
        let bytes = func_name.as_bytes();
        let copy_len = std::cmp::min(bytes.len(), buffer_size - 1);

        unsafe {
            std::ptr::copy_nonoverlapping(
                bytes.as_ptr(),
                out_buffer as *mut u8,
                copy_len,
            );
            *out_buffer.add(copy_len) = 0; // Null terminator
        }

        copy_len as c_int
    }

    /// Extract instruction offset from an error message or backtrace
    ///
    /// # Parameters
    /// - error_message: Pointer to null-terminated C string containing the error message
    ///
    /// # Returns
    /// The instruction offset if found, or -1 if not found
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_trap_extract_offset(
        error_message: *const c_char,
    ) -> c_long {
        if error_message.is_null() {
            return -1;
        }

        let msg = match unsafe { std::ffi::CStr::from_ptr(error_message) }.to_str() {
            Ok(s) => s,
            Err(_) => return -1,
        };

        // Look for patterns like "offset 0x123" or "at 0x123" or just "0x123"
        let offset_patterns = ["offset 0x", "at 0x", ": 0x"];

        for pattern in &offset_patterns {
            if let Some(idx) = msg.find(pattern) {
                let hex_start = idx + pattern.len();
                let hex_str = &msg[hex_start..];
                if let Some(end) = hex_str.find(|c: char| !c.is_ascii_hexdigit()) {
                    let hex = &hex_str[..end];
                    if let Ok(offset) = u64::from_str_radix(hex, 16) {
                        return offset as c_long;
                    }
                } else if let Ok(offset) = u64::from_str_radix(hex_str.trim(), 16) {
                    return offset as c_long;
                }
            }
        }

        -1
    }

    /// Structure to hold extracted trap information for FFI
    #[repr(C)]
    pub struct TrapInfo {
        /// The trap code constant
        pub trap_code: c_int,
        /// Instruction offset (-1 if not available)
        pub instruction_offset: c_long,
        /// Whether this is definitely a trap (1) or unknown (0)
        pub is_trap: c_int,
    }

    /// Extract comprehensive trap information from an error message
    ///
    /// # Parameters
    /// - error_message: Pointer to null-terminated C string containing the error message
    /// - out_info: Pointer to TrapInfo structure to populate
    ///
    /// # Returns
    /// 0 on success, -1 on error
    #[no_mangle]
    pub extern "C" fn wasmtime4j_panama_trap_extract_info(
        error_message: *const c_char,
        out_info: *mut TrapInfo,
    ) -> c_int {
        if error_message.is_null() || out_info.is_null() {
            return -1;
        }

        let info = TrapInfo {
            trap_code: wasmtime4j_panama_trap_parse_code(error_message),
            instruction_offset: wasmtime4j_panama_trap_extract_offset(error_message),
            is_trap: wasmtime4j_panama_trap_is_trap(error_message),
        };

        unsafe {
            *out_info = info;
        }

        0
    }
}