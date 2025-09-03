//! Panama Foreign Function Interface bindings for Java 23+

use std::os::raw::{c_char, c_int, c_void};

/// Panama FFI bindings module
/// 
/// This module provides C-compatible functions for use by the wasmtime4j-panama module.
/// All functions use C calling conventions and handle memory management appropriately.

pub mod engine {
    use super::*;
    use crate::engine::core;
    use crate::error::ffi_utils;
    
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
            let strategy_opt = match strategy {
                0 => Some(Strategy::Cranelift),
                _ => None,
            };
            
            let opt_level_opt = match opt_level {
                0 => Some(OptLevel::None),
                1 => Some(OptLevel::Speed),
                2 => Some(OptLevel::SpeedAndSize),
                _ => None,
            };
            
            let max_memory_pages_opt = if max_memory_pages < 0 {
                None
            } else {
                Some(max_memory_pages as u32)
            };
            
            let max_stack_size_opt = if max_stack_size < 0 {
                None
            } else {
                Some(max_stack_size as usize)
            };
            
            let max_instances_opt = if max_instances < 0 {
                None
            } else {
                Some(max_instances as u32)
            };
            
            core::create_engine_with_config(
                strategy_opt,
                opt_level_opt,
                debug_info != 0,
                wasm_threads != 0,
                wasm_simd != 0,
                wasm_reference_types != 0,
                wasm_bulk_memory != 0,
                wasm_multi_value != 0,
                fuel_enabled != 0,
                max_memory_pages_opt,
                max_stack_size_opt,
                epoch_interruption != 0,
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
        match unsafe { core::get_engine_ref(engine_ptr) } {
            Ok(engine) => core::get_reference_count(engine) as c_int,
            Err(_) => -1,
        }
    }
}

/// Panama FFI bindings for WebAssembly module operations
/// 
/// This module provides C-compatible functions for compiling, validating,
/// and managing WebAssembly modules through the Panama Foreign Function Interface.
pub mod module {
    use super::*;
    
    /// Compile a WebAssembly module (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_compile(
        engine_ptr: *mut c_void,
        wasm_bytes: *const u8,
        wasm_size: usize,
        module_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::error::{ffi_utils, ErrorCode};
        
        ffi_utils::ffi_try_code(|| {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr)? };
            let wasm_data = unsafe { ffi_utils::slice_from_raw_parts(wasm_bytes, wasm_size, "wasm_bytes")? };
            
            let module = crate::module::core::compile_module(engine, wasm_data)?;
            
            unsafe {
                *module_ptr = Box::into_raw(module) as *mut c_void;
            }
            
            Ok(())
        })
    }
    
    /// Destroy a WebAssembly module (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_destroy(module_ptr: *mut c_void) {
        unsafe {
            crate::module::core::destroy_module(module_ptr);
        }
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
                *instance_ptr = Box::into_raw(instance) as *mut c_void;
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