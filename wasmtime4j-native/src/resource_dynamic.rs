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

use wasmtime::component::ResourceDynamic;

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

}
