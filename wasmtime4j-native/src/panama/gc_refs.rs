//! GC reference raw conversion Panama FFI bindings
//!
//! Provides C-compatible functions for ExternRef raw conversions using the
//! module execution store. AnyRef operations go through the GC runtime
//! (panama_gc_ffi.rs) since AnyRef's native data lives in the GC runtime's
//! internal store.

use std::ffi::c_void;

// ============================================================================
// ExternRef Raw Conversions
// ============================================================================

/// Converts an ExternRef (identified by its i64 data) to a raw u32 GC heap index.
///
/// Creates a `wasmtime::ExternRef::new(store, externref_data)` and then calls
/// `to_raw(store)` to get the raw representation.
///
/// Returns the raw u32 value as i64, or -1 on failure.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_externref_to_raw(
    store_ptr: *mut c_void,
    externref_data: i64,
) -> i64 {
    use wasmtime::{ExternRef, RootScope};

    if store_ptr.is_null() {
        return -1;
    }

    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let store = unsafe { &*(store_ptr as *const crate::store::Store) };
        let mut store_guard = store.try_lock_store().ok()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let externref = ExternRef::new(&mut scope, externref_data).ok()?;
        externref.to_raw(&mut scope).ok().map(|r| r as i64)
    }));

    match result {
        Ok(Some(raw)) => raw,
        _ => -1,
    }
}

/// Creates an ExternRef from a raw u32 GC heap index.
///
/// Calls `ExternRef::from_raw(store, raw)`, extracts the i64 data, and returns it.
/// Returns the i64 data, or i64::MIN as sentinel for null/failure.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_externref_from_raw(
    store_ptr: *mut c_void,
    raw: u32,
) -> i64 {
    use wasmtime::{ExternRef, RootScope};

    if store_ptr.is_null() {
        return i64::MIN;
    }

    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let store = unsafe { &*(store_ptr as *const crate::store::Store) };
        let mut store_guard = store.try_lock_store().ok()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let rooted = ExternRef::from_raw(&mut scope, raw)?;
        let data = rooted.data(&scope).ok()??;
        data.downcast_ref::<i64>().copied()
    }));

    match result {
        Ok(Some(id)) => id,
        _ => i64::MIN,
    }
}
