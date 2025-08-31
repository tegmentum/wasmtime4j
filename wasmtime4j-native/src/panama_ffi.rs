//! Panama Foreign Function Interface bindings for Java 23+

use std::os::raw::{c_char, c_int, c_void};

/// Panama FFI bindings module
/// 
/// This module provides C-compatible functions for use by the wasmtime4j-panama module.
/// All functions use C calling conventions and handle memory management appropriately.

pub mod engine {
    use super::*;
    
    /// Create a new Wasmtime engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_create() -> *mut c_void {
        use crate::engine::Engine;
        
        match Engine::new() {
            Ok(engine) => Box::into_raw(Box::new(engine)) as *mut c_void,
            Err(e) => {
                log::error!("Failed to create engine: {:?}", e);
                crate::error::ffi_utils::set_last_error(e);
                std::ptr::null_mut()
            }
        }
    }
    
    /// Destroy a Wasmtime engine (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_destroy(engine_ptr: *mut c_void) {
        use crate::engine::Engine;
        
        if !engine_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(engine_ptr as *mut Engine);
                log::debug!("Engine destroyed successfully");
            }
        }
    }
    
    /// Configure engine with options (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_engine_configure(
        engine_ptr: *mut c_void,
        option_name: *const c_char,
        _option_value: *const c_char,
    ) -> c_int {
        // Placeholder implementation
        // Returns 0 for success, negative for error
        if engine_ptr.is_null() || option_name.is_null() {
            return -1;
        }
        0
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
        use crate::engine::Engine;
        use crate::module::Module;
        use crate::error::{ffi_utils, ErrorCode};
        
        if engine_ptr.is_null() || wasm_bytes.is_null() || module_ptr.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let engine = unsafe { &*(engine_ptr as *const Engine) };
        let wasm_data = unsafe { std::slice::from_raw_parts(wasm_bytes, wasm_size) };
        
        match Module::compile(engine, wasm_data) {
            Ok(module) => {
                unsafe {
                    *module_ptr = Box::into_raw(Box::new(module)) as *mut c_void;
                }
                ffi_utils::clear_last_error();
                0
            }
            Err(e) => {
                let error_code = e.to_error_code();
                ffi_utils::set_last_error(e);
                error_code as c_int
            }
        }
    }
    
    /// Destroy a WebAssembly module (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_module_destroy(module_ptr: *mut c_void) {
        use crate::module::Module;
        
        if !module_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(module_ptr as *mut Module);
                log::debug!("Module destroyed successfully");
            }
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
        use crate::store::Store;
        use crate::module::Module;
        use crate::instance::Instance;
        use crate::error::{ffi_utils, ErrorCode};
        
        if store_ptr.is_null() || module_ptr.is_null() || instance_ptr.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let store = unsafe { &mut *(store_ptr as *mut Store) };
        let module = unsafe { &*(module_ptr as *const Module) };
        
        match Instance::new_without_imports(store, module) {
            Ok(instance) => {
                unsafe {
                    *instance_ptr = Box::into_raw(Box::new(instance)) as *mut c_void;
                }
                ffi_utils::clear_last_error();
                0
            }
            Err(e) => {
                let error_code = e.to_error_code();
                ffi_utils::set_last_error(e);
                error_code as c_int
            }
        }
    }
    
    /// Destroy a WebAssembly instance (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_instance_destroy(instance_ptr: *mut c_void) {
        use crate::instance::Instance;
        
        if !instance_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(instance_ptr as *mut Instance);
                log::debug!("Instance destroyed successfully");
            }
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
        use crate::engine::Engine;
        use crate::store::Store;
        use crate::error::{ffi_utils, ErrorCode};
        
        if engine_ptr.is_null() || store_ptr.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let engine = unsafe { &*(engine_ptr as *const Engine) };
        
        match Store::new(engine) {
            Ok(store) => {
                unsafe {
                    *store_ptr = Box::into_raw(Box::new(store)) as *mut c_void;
                }
                ffi_utils::clear_last_error();
                0
            }
            Err(e) => {
                let error_code = e.to_error_code();
                ffi_utils::set_last_error(e);
                error_code as c_int
            }
        }
    }
    
    /// Destroy a WebAssembly store (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_store_destroy(store_ptr: *mut c_void) {
        use crate::store::Store;
        
        if !store_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(store_ptr as *mut Store);
                log::debug!("Store destroyed successfully");
            }
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
        use crate::component::ComponentEngine;
        use crate::error::ffi_utils;
        
        match ComponentEngine::new() {
            Ok(engine) => {
                ffi_utils::clear_last_error();
                Box::into_raw(Box::new(engine)) as *mut c_void
            }
            Err(e) => {
                log::error!("Failed to create component engine: {:?}", e);
                ffi_utils::set_last_error(e);
                std::ptr::null_mut()
            }
        }
    }
    
    /// Load a component from WebAssembly bytes (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_component_load_from_bytes(
        engine_ptr: *mut c_void,
        wasm_bytes: *const u8,
        wasm_size: usize,
        component_ptr: *mut *mut c_void,
    ) -> c_int {
        use crate::component::ComponentEngine;
        use crate::error::{ffi_utils, ErrorCode};
        
        if engine_ptr.is_null() || wasm_bytes.is_null() || component_ptr.is_null() {
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        if wasm_size == 0 {
            log::error!("Component bytes cannot be empty");
            return ErrorCode::InvalidParameterError as c_int;
        }
        
        let engine = unsafe { &*(engine_ptr as *const ComponentEngine) };
        let wasm_data = unsafe { std::slice::from_raw_parts(wasm_bytes, wasm_size) };
        
        match engine.load_component_from_bytes(wasm_data) {
            Ok(component) => {
                unsafe {
                    *component_ptr = Box::into_raw(Box::new(component)) as *mut c_void;
                }
                ffi_utils::clear_last_error();
                0
            }
            Err(e) => {
                let error_code = e.to_error_code();
                log::error!("Failed to load component: {:?}", e);
                ffi_utils::set_last_error(e);
                error_code as c_int
            }
        }
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
        use crate::component::ComponentEngine;
        
        if !engine_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(engine_ptr as *mut ComponentEngine);
                log::debug!("Component engine destroyed successfully");
            }
        }
    }
    
    /// Destroy a component (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_component_destroy(component_ptr: *mut c_void) {
        use crate::component::Component;
        
        if !component_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(component_ptr as *mut Component);
                log::debug!("Component destroyed successfully");
            }
        }
    }
    
    /// Destroy a component instance (Panama FFI version)
    #[no_mangle]
    pub extern "C" fn wasmtime4j_component_instance_destroy(instance_ptr: *mut c_void) {
        use wasmtime::component::Instance as ComponentInstance;
        use std::sync::Arc;
        
        if !instance_ptr.is_null() {
            unsafe {
                let _ = Box::from_raw(instance_ptr as *mut Arc<ComponentInstance>);
                log::debug!("Component instance destroyed successfully");
            }
        }
    }
}