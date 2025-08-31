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