//! ResourceDynamic support for component model
//!
//! This module provides support for wasmtime::component::ResourceDynamic, which enables
//! host resources with runtime-determined type information. Unlike the statically-typed
//! Resource<T>, ResourceDynamic allows more dynamic scenarios where resource types
//! must be determined at runtime.
//!
//! ## Key Features
//!
//! - Create owned and borrowed dynamic resources
//! - Query resource representation and type
//! - Convert to/from ResourceAny

use std::os::raw::{c_int, c_void};
use wasmtime::component::ResourceDynamic;

use crate::error::WasmtimeResult;
use crate::shared_ffi::{FFI_SUCCESS, FFI_ERROR};

/// Wrapper around ResourceDynamic for FFI
#[derive(Debug)]
pub struct ResourceDynamicWrapper {
    inner: ResourceDynamic,
}

impl ResourceDynamicWrapper {
    /// Create a new owned resource dynamic
    pub fn new_own(rep: u32, ty: u32) -> Self {
        Self {
            inner: ResourceDynamic::new_own(rep, ty),
        }
    }

    /// Create a new borrowed resource dynamic
    pub fn new_borrow(rep: u32, ty: u32) -> Self {
        Self {
            inner: ResourceDynamic::new_borrow(rep, ty),
        }
    }

    /// Get the representation value
    pub fn rep(&self) -> u32 {
        self.inner.rep()
    }

    /// Get the type value
    pub fn ty(&self) -> u32 {
        self.inner.ty()
    }

    /// Check if this is an owned resource
    pub fn owned(&self) -> bool {
        self.inner.owned()
    }

    /// Get reference to inner ResourceDynamic
    pub fn inner(&self) -> &ResourceDynamic {
        &self.inner
    }

    /// Get mutable reference to inner ResourceDynamic
    pub fn inner_mut(&mut self) -> &mut ResourceDynamic {
        &mut self.inner
    }

    /// Consume and return inner ResourceDynamic
    pub fn into_inner(self) -> ResourceDynamic {
        self.inner
    }
}

// =============================================================================
// FFI Functions
// =============================================================================

/// Create a new owned ResourceDynamic
///
/// # Arguments
/// * `rep` - The 32-bit representation value
/// * `ty` - The 32-bit type identifier
///
/// # Returns
/// Pointer to the ResourceDynamic wrapper, or null on error
#[no_mangle]
pub extern "C" fn wasmtime4j_resource_dynamic_new_own(rep: u32, ty: u32) -> *mut c_void {
    let wrapper = Box::new(ResourceDynamicWrapper::new_own(rep, ty));
    Box::into_raw(wrapper) as *mut c_void
}

/// Create a new borrowed ResourceDynamic
///
/// # Arguments
/// * `rep` - The 32-bit representation value
/// * `ty` - The 32-bit type identifier
///
/// # Returns
/// Pointer to the ResourceDynamic wrapper, or null on error
#[no_mangle]
pub extern "C" fn wasmtime4j_resource_dynamic_new_borrow(rep: u32, ty: u32) -> *mut c_void {
    let wrapper = Box::new(ResourceDynamicWrapper::new_borrow(rep, ty));
    Box::into_raw(wrapper) as *mut c_void
}

/// Get the representation value from a ResourceDynamic
///
/// # Safety
/// ptr must be a valid pointer to ResourceDynamicWrapper
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_resource_dynamic_rep(ptr: *const c_void) -> u32 {
    if ptr.is_null() {
        return 0;
    }
    let wrapper = &*(ptr as *const ResourceDynamicWrapper);
    wrapper.rep()
}

/// Get the type value from a ResourceDynamic
///
/// # Safety
/// ptr must be a valid pointer to ResourceDynamicWrapper
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_resource_dynamic_ty(ptr: *const c_void) -> u32 {
    if ptr.is_null() {
        return 0;
    }
    let wrapper = &*(ptr as *const ResourceDynamicWrapper);
    wrapper.ty()
}

/// Check if this is an owned ResourceDynamic
///
/// # Safety
/// ptr must be a valid pointer to ResourceDynamicWrapper
///
/// # Returns
/// 1 if owned, 0 if borrowed or invalid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_resource_dynamic_owned(ptr: *const c_void) -> c_int {
    if ptr.is_null() {
        return 0;
    }
    let wrapper = &*(ptr as *const ResourceDynamicWrapper);
    if wrapper.owned() { 1 } else { 0 }
}

/// Destroy a ResourceDynamic
///
/// # Safety
/// ptr must be a valid pointer to ResourceDynamicWrapper that was created by
/// one of the wasmtime4j_resource_dynamic_new_* functions
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_resource_dynamic_destroy(ptr: *mut c_void) -> c_int {
    if ptr.is_null() {
        return FFI_SUCCESS;
    }
    let _wrapper = Box::from_raw(ptr as *mut ResourceDynamicWrapper);
    FFI_SUCCESS
}

/// Get both rep and ty values in a single call
///
/// # Safety
/// ptr must be a valid pointer to ResourceDynamicWrapper
/// rep_out and ty_out must be valid pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_resource_dynamic_get_values(
    ptr: *const c_void,
    rep_out: *mut u32,
    ty_out: *mut u32,
    owned_out: *mut c_int,
) -> c_int {
    if ptr.is_null() || rep_out.is_null() || ty_out.is_null() || owned_out.is_null() {
        return FFI_ERROR;
    }

    let wrapper = &*(ptr as *const ResourceDynamicWrapper);
    *rep_out = wrapper.rep();
    *ty_out = wrapper.ty();
    *owned_out = if wrapper.owned() { 1 } else { 0 };

    FFI_SUCCESS
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_resource_dynamic_owned() {
        let res = ResourceDynamicWrapper::new_own(42, 100);
        assert_eq!(res.rep(), 42);
        assert_eq!(res.ty(), 100);
        assert!(res.owned());
    }

    #[test]
    fn test_resource_dynamic_borrowed() {
        let res = ResourceDynamicWrapper::new_borrow(123, 200);
        assert_eq!(res.rep(), 123);
        assert_eq!(res.ty(), 200);
        assert!(!res.owned());
    }

    #[test]
    fn test_ffi_create_destroy() {
        unsafe {
            let ptr = wasmtime4j_resource_dynamic_new_own(10, 20);
            assert!(!ptr.is_null());

            assert_eq!(wasmtime4j_resource_dynamic_rep(ptr), 10);
            assert_eq!(wasmtime4j_resource_dynamic_ty(ptr), 20);
            assert_eq!(wasmtime4j_resource_dynamic_owned(ptr), 1);

            assert_eq!(wasmtime4j_resource_dynamic_destroy(ptr), FFI_SUCCESS);
        }
    }

    #[test]
    fn test_ffi_get_values() {
        unsafe {
            let ptr = wasmtime4j_resource_dynamic_new_borrow(55, 66);

            let mut rep: u32 = 0;
            let mut ty: u32 = 0;
            let mut owned: c_int = -1;

            let result = wasmtime4j_resource_dynamic_get_values(ptr, &mut rep, &mut ty, &mut owned);
            assert_eq!(result, FFI_SUCCESS);
            assert_eq!(rep, 55);
            assert_eq!(ty, 66);
            assert_eq!(owned, 0);

            wasmtime4j_resource_dynamic_destroy(ptr);
        }
    }
}
