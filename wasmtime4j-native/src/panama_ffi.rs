//! Panama Foreign Function Interface bindings for Java 23+

use std::os::raw::{c_char, c_int, c_uint, c_ulong, c_void};
use std::sync::Arc;
use crate::error::ffi_utils;
use crate::ffi_common::error_handling;

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
    pub extern "C" fn wasmtime4j_engine_create() -> *mut c_void {
        ffi_utils::ffi_try_ptr(|| core::create_engine())
    }

    /// Create a new Wasmtime engine with custom configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_create_with_config(
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
        use wasmtime::{Strategy, OptLevel};
        
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
            )
        })
    }
    
    /// Destroy a Wasmtime engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_destroy(engine_ptr: *mut c_void) {
        unsafe {
            core::destroy_engine(engine_ptr);
        }
    }

    /// Check if fuel consumption is enabled (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_is_fuel_enabled(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if core::is_fuel_enabled(engine) { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Check if epoch interruption is enabled (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_is_epoch_interruption_enabled(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if core::is_epoch_interruption_enabled(engine) { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Get memory limit in pages (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_get_memory_limit(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => core::get_memory_limit(engine).map(|limit| limit as c_int).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Get stack size limit in bytes (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_get_stack_limit(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => core::get_stack_limit(engine).map(|limit| limit as c_int).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Get maximum instances limit (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_get_max_instances(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => core::get_max_instances(engine).map(|limit| limit as c_int).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Validate engine functionality (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_validate(engine_ptr: *mut c_void) -> c_int {
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if core::validate_engine(engine).is_ok() { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Check if engine supports WebAssembly feature (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_supports_feature(
        engine_ptr: *mut c_void,
        feature_id: c_int,
    ) -> c_int {
        use crate::engine::WasmFeature;
        
        let feature = match feature_id {
            0 => WasmFeature::Threads,
            1 => WasmFeature::ReferenceTypes,
            2 => WasmFeature::Simd,
            3 => WasmFeature::BulkMemory,
            4 => WasmFeature::MultiValue,
            _ => return -1,
        };

        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => if core::check_feature_support(engine, feature) { 1 } else { 0 },
            Err(_) => -1,
        }
    }

    /// Get engine reference count for debugging (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_get_reference_count(engine_ptr: *mut c_void) -> c_int {
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
}

/// Panama FFI bindings for WebAssembly module operations
/// 
/// This module provides C-compatible functions for compiling, validating,
/// and managing WebAssembly modules through the Panama Foreign Function Interface.
pub mod module {
    use super::*;
    use crate::module::core;
    use crate::error::ffi_utils;
    use crate::shared_ffi::module::{ByteArrayConverter, StringConverter};

    /// Panama-specific byte array converter implementation
    pub struct PanamaByteArrayConverter {
        data: *const u8,
        len: usize,
    }

    impl PanamaByteArrayConverter {
        pub unsafe fn new(data: *const u8, len: usize) -> Self {
            Self { data, len }
        }
    }

    impl ByteArrayConverter for PanamaByteArrayConverter {
        unsafe fn get_bytes(&self) -> crate::error::WasmtimeResult<&[u8]> {
            if self.data.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter(
                    "Byte data pointer cannot be null".to_string()
                ));
            }
            
            if self.len == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter(
                    "Byte data length cannot be zero".to_string()
                ));
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
        pub unsafe fn new(string_ptr: *const c_char) -> Self {
            Self { string_ptr }
        }
    }

    impl StringConverter for PanamaStringConverter {
        unsafe fn get_string(&self) -> crate::error::WasmtimeResult<String> {
            if self.string_ptr.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter(
                    "String pointer cannot be null".to_string()
                ));
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
    pub extern "C" fn wasmtime4j_module_compile(
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
    pub extern "C" fn wasmtime4j_module_compile_wat(
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
    pub extern "C" fn wasmtime4j_module_validate(
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
    pub extern "C" fn wasmtime4j_module_get_size(module_ptr: *mut c_void) -> usize {
        let result = crate::shared_ffi::module::get_module_size_shared(module_ptr);
        let (_, size) = crate::shared_ffi::module::size_result_to_ffi_result(result);
        size
    }

    /// Get module name (if available)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_get_name(module_ptr: *mut c_void) -> *mut c_char {
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
    pub extern "C" fn wasmtime4j_module_get_export_count(module_ptr: *mut c_void) -> c_int {
        let result = crate::shared_ffi::module::get_export_count_shared(module_ptr);
        let (_, count) = crate::shared_ffi::module::count_result_to_ffi_result(result);
        count
    }

    /// Get number of imports
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_get_import_count(module_ptr: *mut c_void) -> c_int {
        let result = crate::shared_ffi::module::get_import_count_shared(module_ptr);
        let (_, count) = crate::shared_ffi::module::count_result_to_ffi_result(result);
        count
    }

    /// Get number of functions
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_get_function_count(module_ptr: *mut c_void) -> c_int {
        let result = crate::shared_ffi::module::get_function_count_shared(module_ptr);
        let (_, count) = crate::shared_ffi::module::count_result_to_ffi_result(result);
        count
    }

    /// Check if module has a specific export
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_has_export(
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
    pub extern "C" fn wasmtime4j_module_serialize(
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
    pub extern "C" fn wasmtime4j_module_deserialize(
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

    /// Validate module functionality (defensive check)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_validate_functionality(module_ptr: *mut c_void) -> c_int {
        crate::shared_ffi::module::validation_result_to_ffi_code(
            crate::shared_ffi::module::validate_module_functionality_shared(module_ptr)
        )
    }

    /// Free serialized data
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_free_serialized_data(data_ptr: *mut u8, len: usize) {
        if !data_ptr.is_null() && len > 0 {
            unsafe {
                drop(Box::from_raw(std::slice::from_raw_parts_mut(data_ptr, len)));
            }
        }
    }

    /// Free C string returned by module functions
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_free_string(str_ptr: *mut c_char) {
        if !str_ptr.is_null() {
            unsafe {
                drop(std::ffi::CString::from_raw(str_ptr));
            }
        }
    }
    
    /// Destroy a WebAssembly module (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_destroy(module_ptr: *mut c_void) {
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
    pub extern "C" fn wasmtime4j_instance_create(
        store_ptr: *mut c_void,
        module_ptr: *mut c_void,
        instance_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::error::{ffi_utils, ErrorCode};
        
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
    pub extern "C" fn wasmtime4j_instance_destroy(instance_ptr: *mut c_void) {
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
    
    /// Create a new WebAssembly store (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_store_create(
        engine_ptr: *mut c_void,
        store_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::error::{ffi_utils, ErrorCode};
        
        ffi_utils::ffi_try_code(|| {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };
            
            let store = crate::store::core::create_store(engine)?;
            
            unsafe {
                *store_ptr = Box::into_raw(store) as *mut c_void;
            }
            
            Ok(())
        })
    }
    
    /// Destroy a WebAssembly store (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_store_destroy(store_ptr: *mut c_void) {
        unsafe {
            crate::store::core::destroy_store(store_ptr);
        }
    }
}

/// Panama FFI bindings for WebAssembly Component Model operations (WASI Preview 2)
/// 
/// This module provides C-compatible functions for creating, instantiating,
/// and managing WebAssembly components through the Panama Foreign Function Interface.
pub mod component {
    use super::*;
    
    /// Create a new component engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_component_engine_create() -> *mut c_void {
        use crate::error::{ffi_utils, ErrorCode};
        
        ffi_utils::ffi_try_ptr(|| crate::component::core::create_component_engine())
    }
    
    /// Load a component from WebAssembly bytes (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_component_load_from_bytes(
        engine_ptr: *mut c_void,
        wasm_bytes: *const u8,
        wasm_size: usize,
        component_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::error::{ffi_utils, ErrorCode};
        
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
    pub extern "C" fn wasmtime4j_component_instantiate(
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
    pub extern "C" fn wasmtime4j_component_get_size(
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
    pub extern "C" fn wasmtime4j_component_exports_interface(
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
    pub extern "C" fn wasmtime4j_component_imports_interface(
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
    pub extern "C" fn wasmtime4j_component_get_active_instances_count(
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
    pub extern "C" fn wasmtime4j_component_cleanup_instances(
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
    
    /// Destroy a component engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_component_engine_destroy(engine_ptr: *mut c_void) {
        unsafe {
            crate::component::core::destroy_component_engine(engine_ptr);
        }
    }
    
    /// Destroy a component (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_component_destroy(component_ptr: *mut c_void) {
        unsafe {
            crate::component::core::destroy_component(component_ptr);
        }
    }
    
    /// Destroy a component instance (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_component_instance_destroy(instance_ptr: *mut c_void) {
        unsafe {
            crate::component::core::destroy_component_instance(instance_ptr);
        }
    }
}

/// Panama FFI bindings for WebAssembly linear memory operations
/// 
/// This module provides C-compatible functions for creating, managing,
/// and accessing WebAssembly linear memory with comprehensive bounds checking.
pub mod memory {
    use super::*;
    use crate::memory::{Memory, MemoryBuilder, MemoryConfig, MemoryUsage as MemUsage, MemoryDataType, MemoryRegistry};
    use crate::store::Store;
    use crate::error::{ffi_utils, ErrorCode};

    /// Create a new WebAssembly memory with default configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_create(
        store_ptr: *mut c_void,
        initial_pages: c_uint,
        memory_ptr: *mut *mut c_void,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let mut store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
            
            let memory = Memory::new(store, initial_pages as u64)?;
            
            unsafe {
                *memory_ptr = Box::into_raw(Box::new(memory)) as *mut c_void;
            }
            
            Ok(())
        })
    }

    /// Create a new WebAssembly memory with configuration (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_create_with_config(
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
            let mut store = crate::ffi_common::memory_utils::safe_deref_mut(
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
    pub extern "C" fn wasmtime4j_memory_size_pages(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        size_out: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let size = memory.size_pages(store)?;
            
            unsafe {
                *size_out = size as c_uint;
            }
            
            Ok(())
        })
    }

    /// Get memory size in bytes (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_size_bytes(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        size_out: *mut usize,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            let size = memory.size_bytes(store)?;
            
            unsafe {
                *size_out = size;
            }
            
            Ok(())
        })
    }

    /// Grow memory by additional pages (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_grow(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        additional_pages: c_uint,
        previous_pages_out: *mut c_uint,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let mut store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
            
            let previous_pages = memory.grow(store, additional_pages as u64)?;
            
            unsafe {
                *previous_pages_out = previous_pages as c_uint;
            }
            
            Ok(())
        })
    }

    /// Read bytes from memory with bounds checking (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_read_bytes(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        length: usize,
        buffer: *mut u8,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
            
            if buffer.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Buffer cannot be null".to_string(),
                });
            }
            
            let data = memory.read_bytes(store, offset, length)?;
            
            unsafe {
                std::ptr::copy_nonoverlapping(data.as_ptr(), buffer, length);
            }
            
            Ok(())
        })
    }

    /// Write bytes to memory with bounds checking (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_write_bytes(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        length: usize,
        buffer: *const u8,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let mut store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
            
            if buffer.is_null() {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Buffer cannot be null".to_string(),
                });
            }
            
            let data = unsafe { std::slice::from_raw_parts(buffer, length) };
            memory.write_bytes(store, offset, data)?;
            
            Ok(())
        })
    }

    /// Read typed value from memory with alignment checking (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_read_u32(
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
    pub extern "C" fn wasmtime4j_memory_write_u32(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: u32,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
            let mut store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
            
            memory.write_typed(store, offset, value, MemoryDataType::U32Le)?;
            
            Ok(())
        })
    }

    /// Get memory usage statistics (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_get_usage(
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
    pub extern "C" fn wasmtime4j_memory_registry_create(registry_ptr: *mut *mut c_void) -> c_int {
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
    pub extern "C" fn wasmtime4j_memory_registry_register(
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
    pub extern "C" fn wasmtime4j_memory_registry_get(
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
    pub extern "C" fn wasmtime4j_memory_destroy(memory_ptr: *mut c_void) {
        // SAFETY IMPROVEMENT: Using safe resource destruction with validation
        unsafe {
            crate::ffi_common::memory_utils::destroy_ffi_resource::<Memory>(memory_ptr, "Memory");
        }
    }

    /// Destroy a memory registry (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_memory_registry_destroy(registry_ptr: *mut c_void) {
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
    use crate::global::{Global, GlobalValue, core};
    use crate::store::Store;
    use crate::error::{ffi_utils, ErrorCode};
    use wasmtime::{ValType, Mutability};

    /// Create a new WebAssembly global variable (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_global_create(
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
                5 => ValType::FuncRef,
                6 => ValType::ExternRef,
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
                val_type, 
                i32_value, 
                i64_value as i64, 
                f32_value as f32, 
                f64_value,
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
    pub extern "C" fn wasmtime4j_global_get(
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
    pub extern "C" fn wasmtime4j_global_set(
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
                5 => ValType::FuncRef,
                6 => ValType::ExternRef,
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
                ref_id_opt,
            )?;

            core::set_global_value(global, store, value)?;
            
            Ok(())
        })
    }

    /// Get global variable metadata (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_global_metadata(
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
                        ValType::FuncRef => 5,
                        ValType::ExternRef => 6,
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
    pub extern "C" fn wasmtime4j_global_destroy(global_ptr: *mut c_void) {
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
    use crate::table::{Table, TableElement, core};
    use crate::store::Store;
    use crate::error::{ffi_utils, ErrorCode};
    use wasmtime::ValType;

    /// Create a new WebAssembly table (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_table_create(
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
                5 => ValType::FuncRef,
                6 => ValType::ExternRef,
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
    pub extern "C" fn wasmtime4j_table_size(
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
    pub extern "C" fn wasmtime4j_table_get(
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
    pub extern "C" fn wasmtime4j_table_set(
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
                5 => ValType::FuncRef,
                6 => ValType::ExternRef,
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
    pub extern "C" fn wasmtime4j_table_grow(
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
                5 => ValType::FuncRef,
                6 => ValType::ExternRef,
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
    pub extern "C" fn wasmtime4j_table_fill(
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
                5 => ValType::FuncRef,
                6 => ValType::ExternRef,
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
    #[no_mangle]
    pub extern "C" fn wasmtime4j_table_metadata(
        table_ptr: *mut c_void,
        element_type: *mut c_int,
        initial_size: *mut c_uint,
        has_maximum: *mut c_int,
        maximum_size: *mut c_uint,
        name_ptr: *mut *mut c_char,
    ) -> c_int {
        ffi_utils::ffi_try_code(|| {
            let table = unsafe { core::get_table_ref(table_ptr)? };
            let metadata = core::get_table_metadata(table);
            
            unsafe {
                if !element_type.is_null() {
                    *element_type = match metadata.element_type {
                        ValType::FuncRef => 5,
                        ValType::ExternRef => 6,
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
    pub extern "C" fn wasmtime4j_table_destroy(table_ptr: *mut c_void) {
        unsafe {
            core::destroy_table(table_ptr);
        }
    }
}