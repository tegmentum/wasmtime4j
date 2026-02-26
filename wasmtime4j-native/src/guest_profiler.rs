// Copyright 2025 Tegmentum AI
//
// Licensed under the Apache License, Version 2.0

//! Guest profiler wrapper for sampling-based WebAssembly performance profiling.
//!
//! Wraps `wasmtime::GuestProfiler` to provide FFI-safe profiling operations.
//! The profiler collects stack samples during execution and outputs profiles
//! in the Firefox Processed Profile Format (JSON), viewable at
//! <https://profiler.firefox.com/>.

use std::ffi::{c_char, c_int, c_long, c_ulong, c_void, CStr};
use std::sync::Mutex;
use std::time::Duration;

use wasmtime::{CallHook, GuestProfiler};

/// Opaque boxed profiler behind a Mutex for safe mutable access from FFI.
/// The Mutex is needed because `sample()` and `call_hook()` take `&mut self`.
struct ProfilerBox {
    /// `None` after `finish()` consumes the profiler.
    profiler: Option<GuestProfiler>,
}

/// Creates a new guest profiler for core wasm modules.
///
/// # Arguments
/// * `engine_ptr` - pointer to a `wasmtime::Engine`
/// * `module_name` - C string label for the profile
/// * `interval_nanos` - sampling interval hint in nanoseconds
/// * `module_ptrs` - array of pointers to `wasmtime::Module`
/// * `module_names` - array of C string names corresponding to modules
/// * `module_count` - number of modules in the arrays
///
/// # Returns
/// Opaque pointer to a `Mutex<ProfilerBox>`, or null on error.
///
/// # Safety
/// All pointer arguments must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_new(
    engine_ptr: *const c_void,
    module_name: *const c_char,
    interval_nanos: c_long,
    module_ptrs: *const *const c_void,
    module_names: *const *const c_char,
    module_count: c_int,
) -> *mut c_void {
    if engine_ptr.is_null() || module_name.is_null() {
        return std::ptr::null_mut();
    }

    let engine = &*(engine_ptr as *const crate::engine::Engine);
    let wasmtime_engine = engine.inner();
    let name = match CStr::from_ptr(module_name).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    let interval = Duration::from_nanos(interval_nanos as u64);

    // Build the modules list
    let mut modules: Vec<(String, wasmtime::Module)> = Vec::new();
    for i in 0..module_count as usize {
        let mptr = *module_ptrs.add(i);
        let nptr = *module_names.add(i);
        if mptr.is_null() || nptr.is_null() {
            continue;
        }
        let module_wrapper = &*(mptr as *const crate::module::Module);
        let module = module_wrapper.inner().clone();
        let mname = match CStr::from_ptr(nptr).to_str() {
            Ok(s) => s.to_string(),
            Err(_) => continue,
        };
        modules.push((mname, module));
    }

    match GuestProfiler::new(wasmtime_engine, name, interval, modules) {
        Ok(profiler) => {
            let boxed = Box::new(Mutex::new(ProfilerBox {
                profiler: Some(profiler),
            }));
            Box::into_raw(boxed) as *mut c_void
        }
        Err(e) => {
            log::error!("Failed to create GuestProfiler: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Creates a new guest profiler for a single module (convenience).
///
/// # Arguments
/// * `engine_ptr` - pointer to a `wasmtime::Engine`
/// * `module_name` - C string label for the profile (also used as module name)
/// * `interval_nanos` - sampling interval hint in nanoseconds
/// * `module_ptr` - pointer to a `wasmtime::Module` to profile
///
/// # Returns
/// Opaque pointer to a `Mutex<ProfilerBox>`, or null on error.
///
/// # Safety
/// All pointer arguments must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_new_single(
    engine_ptr: *const c_void,
    module_name: *const c_char,
    interval_nanos: c_long,
    module_ptr: *const c_void,
) -> *mut c_void {
    if engine_ptr.is_null() || module_name.is_null() || module_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let engine = &*(engine_ptr as *const crate::engine::Engine);
    let wasmtime_engine = engine.inner();
    let name = match CStr::from_ptr(module_name).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    let interval = Duration::from_nanos(interval_nanos as u64);
    let module_wrapper = &*(module_ptr as *const crate::module::Module);
    let module = module_wrapper.inner().clone();

    match GuestProfiler::new(wasmtime_engine, name, interval, vec![(name.to_string(), module)]) {
        Ok(profiler) => {
            let boxed = Box::new(Mutex::new(ProfilerBox {
                profiler: Some(profiler),
            }));
            Box::into_raw(boxed) as *mut c_void
        }
        Err(e) => {
            log::error!("Failed to create GuestProfiler: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Creates a new guest profiler for a component.
///
/// # Arguments
/// * `engine_ptr` - pointer to a `wasmtime::Engine`
/// * `component_name` - C string label for the profile
/// * `interval_nanos` - sampling interval hint in nanoseconds
/// * `component_ptr` - pointer to a `crate::component::Component`
/// * `extra_module_ptrs` - array of pointers to extra `wasmtime::Module` (may be null if count=0)
/// * `extra_module_names` - array of C string names for extra modules (may be null if count=0)
/// * `extra_module_count` - number of extra modules
///
/// # Returns
/// Opaque pointer to a `Mutex<ProfilerBox>`, or null on error.
///
/// # Safety
/// All pointer arguments must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_new_component(
    engine_ptr: *const c_void,
    component_name: *const c_char,
    interval_nanos: c_long,
    component_ptr: *const c_void,
    extra_module_ptrs: *const *const c_void,
    extra_module_names: *const *const c_char,
    extra_module_count: c_int,
) -> *mut c_void {
    if engine_ptr.is_null() || component_name.is_null() || component_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let engine = &*(engine_ptr as *const crate::engine::Engine);
    let wasmtime_engine = engine.inner();
    let name = match CStr::from_ptr(component_name).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    let interval = Duration::from_nanos(interval_nanos as u64);
    let component = &*(component_ptr as *const crate::component::Component);
    let wasmtime_component = component.wasmtime_component().clone();

    // Build extra modules list
    let mut extra_modules: Vec<(String, wasmtime::Module)> = Vec::new();
    for i in 0..extra_module_count as usize {
        let mptr = *extra_module_ptrs.add(i);
        let nptr = *extra_module_names.add(i);
        if mptr.is_null() || nptr.is_null() {
            continue;
        }
        let module_wrapper = &*(mptr as *const crate::module::Module);
        let module = module_wrapper.inner().clone();
        let mname = match CStr::from_ptr(nptr).to_str() {
            Ok(s) => s.to_string(),
            Err(_) => continue,
        };
        extra_modules.push((mname, module));
    }

    match GuestProfiler::new_component(
        wasmtime_engine,
        name,
        interval,
        wasmtime_component,
        extra_modules,
    ) {
        Ok(profiler) => {
            let boxed = Box::new(Mutex::new(ProfilerBox {
                profiler: Some(profiler),
            }));
            Box::into_raw(boxed) as *mut c_void
        }
        Err(e) => {
            log::error!("Failed to create component GuestProfiler: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Collects a stack sample from the current execution.
///
/// # Arguments
/// * `profiler_ptr` - opaque pointer from `wasmtime4j_guest_profiler_new*`
/// * `store_ptr` - pointer to a `crate::store::Store`
/// * `delta_nanos` - CPU time since previous sample in nanoseconds
///
/// # Returns
/// 0 on success, -1 on error, -2 if profiler already finished.
///
/// # Safety
/// All pointer arguments must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_sample(
    profiler_ptr: *mut c_void,
    store_ptr: *mut c_void,
    delta_nanos: c_long,
) -> c_int {
    if profiler_ptr.is_null() || store_ptr.is_null() {
        return -1;
    }

    let mutex = &*(profiler_ptr as *const Mutex<ProfilerBox>);
    let store = &*(store_ptr as *const crate::store::Store);
    let delta = Duration::from_nanos(delta_nanos as u64);

    let store_guard = match store.try_lock_store() {
        Ok(g) => g,
        Err(_) => return -1,
    };

    match mutex.lock() {
        Ok(mut guard) => {
            if let Some(ref mut profiler) = guard.profiler {
                profiler.sample(&*store_guard, delta);
                0
            } else {
                -2 // already finished
            }
        }
        Err(_) => -1,
    }
}

/// Records a call hook transition marker.
///
/// # Arguments
/// * `profiler_ptr` - opaque pointer from `wasmtime4j_guest_profiler_new*`
/// * `store_ptr` - pointer to a `crate::store::Store`
/// * `hook_kind` - 0=CallingWasm, 1=ReturningFromWasm, 2=CallingHost, 3=ReturningFromHost
///
/// # Returns
/// 0 on success, -1 on error, -2 if profiler already finished.
///
/// # Safety
/// All pointer arguments must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_call_hook(
    profiler_ptr: *mut c_void,
    store_ptr: *mut c_void,
    hook_kind: c_int,
) -> c_int {
    if profiler_ptr.is_null() || store_ptr.is_null() {
        return -1;
    }

    let hook = match hook_kind {
        0 => CallHook::CallingWasm,
        1 => CallHook::ReturningFromWasm,
        2 => CallHook::CallingHost,
        3 => CallHook::ReturningFromHost,
        _ => return -1,
    };

    let mutex = &*(profiler_ptr as *const Mutex<ProfilerBox>);
    let store = &*(store_ptr as *const crate::store::Store);

    let store_guard = match store.try_lock_store() {
        Ok(g) => g,
        Err(_) => return -1,
    };

    match mutex.lock() {
        Ok(mut guard) => {
            if let Some(ref mut profiler) = guard.profiler {
                profiler.call_hook(&*store_guard, hook);
                0
            } else {
                -2 // already finished
            }
        }
        Err(_) => -1,
    }
}

/// Finishes profiling and writes the profile as JSON.
///
/// Consumes the profiler. After this call, `sample()` and `call_hook()` will
/// return -2.
///
/// # Arguments
/// * `profiler_ptr` - opaque pointer from `wasmtime4j_guest_profiler_new*`
/// * `data_out` - output pointer for the JSON data (allocated by Rust, caller must free)
/// * `len_out` - output pointer for the data length
///
/// # Returns
/// 0 on success, -1 on error, -2 if profiler already finished.
///
/// # Safety
/// All pointer arguments must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_finish(
    profiler_ptr: *mut c_void,
    data_out: *mut *mut u8,
    len_out: *mut c_ulong,
) -> c_int {
    if profiler_ptr.is_null() || data_out.is_null() || len_out.is_null() {
        return -1;
    }

    let mutex = &*(profiler_ptr as *const Mutex<ProfilerBox>);

    let profiler = match mutex.lock() {
        Ok(mut guard) => {
            match guard.profiler.take() {
                Some(p) => p,
                None => return -2, // already finished
            }
        }
        Err(_) => return -1,
    };

    let mut buffer = Vec::new();
    match profiler.finish(&mut buffer) {
        Ok(()) => {
            let len = buffer.len();
            let ptr = buffer.as_mut_ptr();
            std::mem::forget(buffer); // Caller owns the memory now
            *data_out = ptr;
            *len_out = len as c_ulong;
            0
        }
        Err(e) => {
            log::error!("Failed to finish GuestProfiler: {}", e);
            -1
        }
    }
}

/// Frees the JSON data buffer returned by `wasmtime4j_guest_profiler_finish`.
///
/// # Safety
/// `data` must be a pointer previously returned by `finish`, and `len` must
/// match the length returned.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_free_data(data: *mut u8, len: c_ulong) {
    if !data.is_null() && len > 0 {
        let _ = Vec::from_raw_parts(data, len as usize, len as usize);
    }
}

/// Destroys the profiler and frees all associated resources.
///
/// # Safety
/// `profiler_ptr` must be a pointer previously returned by `new*`, or null.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_destroy(profiler_ptr: *mut c_void) {
    if !profiler_ptr.is_null() {
        let _ = Box::from_raw(profiler_ptr as *mut Mutex<ProfilerBox>);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_destroy_null_is_safe() {
        unsafe {
            wasmtime4j_guest_profiler_destroy(std::ptr::null_mut());
        }
    }

    #[test]
    fn test_new_null_engine_returns_null() {
        unsafe {
            let result = wasmtime4j_guest_profiler_new_single(
                std::ptr::null(),
                b"test\0".as_ptr() as *const c_char,
                1_000_000,
                std::ptr::null(),
            );
            assert!(result.is_null());
        }
    }

    #[test]
    fn test_sample_null_returns_error() {
        unsafe {
            let result = wasmtime4j_guest_profiler_sample(
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                0,
            );
            assert_eq!(result, -1);
        }
    }

    #[test]
    fn test_finish_null_returns_error() {
        unsafe {
            let result = wasmtime4j_guest_profiler_finish(
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                std::ptr::null_mut(),
            );
            assert_eq!(result, -1);
        }
    }
}
