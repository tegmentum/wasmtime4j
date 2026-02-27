//! Exception Handling FFI module for Panama
//!
//! This module provides Panama FFI bindings for WebAssembly exception handling,
//! including Tag creation, ExnRef management, and store exception operations.

use std::os::raw::{c_int, c_void};

// ============================================================================
// Tag Functions
// ============================================================================

/// Creates a new WebAssembly tag for exception handling.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
/// - param_types and return_types must be valid pointers to int arrays
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_create(
    store_ptr: *mut c_void,
    param_types: *const c_int,
    param_count: c_int,
    _return_types: *const c_int,
    _return_count: c_int,
) -> *mut c_void {
    use wasmtime::{FuncType, Tag, TagType, ValType};

    if store_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let store = unsafe { &*(store_ptr as *const crate::store::Store) };

    // Convert param type codes to ValTypes
    let params: Vec<ValType> = if param_count > 0 && !param_types.is_null() {
        let param_slice = unsafe { std::slice::from_raw_parts(param_types, param_count as usize) };
        param_slice
            .iter()
            .filter_map(|&code| match code {
                0 => Some(ValType::I32),
                1 => Some(ValType::I64),
                2 => Some(ValType::F32),
                3 => Some(ValType::F64),
                4 => Some(ValType::V128),
                5 => Some(ValType::FUNCREF),
                6 => Some(ValType::EXTERNREF),
                _ => None,
            })
            .collect()
    } else {
        Vec::new()
    };

    // Create FuncType for the tag (tags use params only, empty results)
    let func_type = FuncType::new(store.engine().inner(), params.iter().cloned(), []);

    // Create TagType from FuncType
    let tag_type = TagType::new(func_type);

    // Lock the store and create the Tag
    let mut store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => return std::ptr::null_mut(),
    };
    match Tag::new(&mut *store_guard, &tag_type) {
        Ok(tag) => Box::into_raw(Box::new(tag)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Gets the parameter types of a tag.
///
/// # Safety
/// - tag_ptr must be a valid pointer to a Tag
/// - store_ptr must be a valid pointer to a Store
/// - out_count must be a valid pointer
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_get_param_types(
    tag_ptr: *const c_void,
    store_ptr: *mut c_void,
    out_count: *mut c_int,
) -> *mut c_int {
    use wasmtime::Tag;

    if tag_ptr.is_null() || store_ptr.is_null() || out_count.is_null() {
        if !out_count.is_null() {
            unsafe {
                *out_count = 0;
            }
        }
        return std::ptr::null_mut();
    }

    let tag = unsafe { &*(tag_ptr as *const Tag) };
    let store = unsafe { &*(store_ptr as *const crate::store::Store) };
    let store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => {
            unsafe {
                *out_count = 0;
            }
            return std::ptr::null_mut();
        }
    };

    let tag_type = tag.ty(&*store_guard);
    let func_type = tag_type.ty();
    let params: Vec<c_int> = func_type
        .params()
        .map(|vt| crate::ffi_common::valtype_conversion::valtype_to_int(&vt))
        .collect();

    let count = params.len();
    unsafe {
        *out_count = count as c_int;
    }

    if count == 0 {
        return std::ptr::null_mut();
    }

    let boxed = params.into_boxed_slice();
    Box::into_raw(boxed) as *mut c_int
}

/// Gets the return types of a tag.
///
/// # Safety
/// - tag_ptr must be a valid pointer to a Tag
/// - store_ptr must be a valid pointer to a Store
/// - out_count must be a valid pointer
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_get_return_types(
    tag_ptr: *const c_void,
    store_ptr: *mut c_void,
    out_count: *mut c_int,
) -> *mut c_int {
    use wasmtime::Tag;

    if tag_ptr.is_null() || store_ptr.is_null() || out_count.is_null() {
        if !out_count.is_null() {
            unsafe {
                *out_count = 0;
            }
        }
        return std::ptr::null_mut();
    }

    let tag = unsafe { &*(tag_ptr as *const Tag) };
    let store = unsafe { &*(store_ptr as *const crate::store::Store) };
    let store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => {
            unsafe {
                *out_count = 0;
            }
            return std::ptr::null_mut();
        }
    };

    let tag_type = tag.ty(&*store_guard);
    let func_type = tag_type.ty();
    let results: Vec<c_int> = func_type
        .results()
        .map(|vt| crate::ffi_common::valtype_conversion::valtype_to_int(&vt))
        .collect();

    let count = results.len();
    unsafe {
        *out_count = count as c_int;
    }

    if count == 0 {
        return std::ptr::null_mut();
    }

    let boxed = results.into_boxed_slice();
    Box::into_raw(boxed) as *mut c_int
}

/// Frees a tag types array.
///
/// # Safety
/// - types_ptr must be a valid pointer allocated by tag_get_param/return_types
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_types_free(types_ptr: *mut c_int, count: c_int) {
    if !types_ptr.is_null() && count > 0 {
        unsafe {
            let _ = Vec::from_raw_parts(types_ptr, count as usize, count as usize);
        }
    }
}

/// Checks if two tags are equal.
///
/// # Safety
/// - tag1_ptr and tag2_ptr must be valid pointers to Tags
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_equals(
    tag1_ptr: *const c_void,
    tag2_ptr: *const c_void,
    store_ptr: *mut c_void,
) -> c_int {
    use wasmtime::Tag;

    if tag1_ptr.is_null() || tag2_ptr.is_null() || store_ptr.is_null() {
        return 0;
    }

    let tag1 = unsafe { &*(tag1_ptr as *const Tag) };
    let tag2 = unsafe { &*(tag2_ptr as *const Tag) };
    let store = unsafe { &*(store_ptr as *const crate::store::Store) };
    let store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => return 0, // Return false if store is closed
    };

    // Compare tags by checking if they have the same type
    let ty1 = tag1.ty(&*store_guard);
    let ty2 = tag2.ty(&*store_guard);

    // Tags are equal if their func types match
    let ft1 = ty1.ty();
    let ft2 = ty2.ty();

    // Compare params and results count and types using matches()
    let params1: Vec<_> = ft1.params().collect();
    let params2: Vec<_> = ft2.params().collect();
    let results1: Vec<_> = ft1.results().collect();
    let results2: Vec<_> = ft2.results().collect();

    if params1.len() != params2.len() || results1.len() != results2.len() {
        return 0;
    }

    // Check each param type matches
    for (p1, p2) in params1.iter().zip(params2.iter()) {
        if !p1.matches(p2) {
            return 0;
        }
    }

    // Check each result type matches
    for (r1, r2) in results1.iter().zip(results2.iter()) {
        if !r1.matches(r2) {
            return 0;
        }
    }

    1
}

/// Destroys a tag and frees its native resources.
///
/// # Safety
/// - tag_ptr must be a valid pointer to a Tag
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_destroy(tag_ptr: *mut c_void) {
    use wasmtime::Tag;

    if !tag_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(tag_ptr as *mut Tag);
        }
    }
}

// ============================================================================
// ExnRef Functions
// ============================================================================

/// Gets the tag from an exception reference.
///
/// # Safety
/// - exnref_ptr must be a valid pointer to an ExnRef
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_exnref_get_tag(
    exnref_ptr: *const c_void,
    store_ptr: *mut c_void,
) -> *mut c_void {
    use wasmtime::{ExnRef, OwnedRooted, RootScope};

    if exnref_ptr.is_null() || store_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let owned_exnref = unsafe { &*(exnref_ptr as *const OwnedRooted<ExnRef>) };
    let store = unsafe { &*(store_ptr as *const crate::store::Store) };
    let mut store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => return std::ptr::null_mut(),
    };

    // Create a root scope and get the tag
    let mut scope = RootScope::new(&mut *store_guard);
    let exnref = owned_exnref.to_rooted(&mut scope);
    match exnref.tag(&mut scope) {
        Ok(tag) => Box::into_raw(Box::new(tag)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Checks if an exception reference is valid.
///
/// # Safety
/// - exnref_ptr must be a valid pointer to an ExnRef
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_exnref_is_valid(
    _exnref_ptr: *const c_void,
    _store_ptr: *mut c_void,
) -> c_int {
    // ExnRef.isValid is not yet implemented
    0
}

/// Destroys an exception reference and frees its native resources.
///
/// # Safety
/// - exnref_ptr must be a valid pointer to an OwnedRooted<ExnRef>
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_exnref_destroy(exnref_ptr: *mut c_void) -> c_int {
    if exnref_ptr.is_null() {
        return 0; // FFI_SUCCESS
    }
    unsafe {
        let _boxed = Box::from_raw(exnref_ptr as *mut wasmtime::OwnedRooted<wasmtime::ExnRef>);
        // Dropping the Box cleans up the OwnedRooted
    }
    0 // FFI_SUCCESS
}

/// Gets a single field value from an exception reference by index.
///
/// # Safety
/// - exnref_ptr must be a valid pointer to an OwnedRooted<ExnRef>
/// - store_ptr must be a valid pointer to a Store
/// - out_type, out_value_i64, out_value_f64 must be valid pointers
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_exnref_get_field(
    exnref_ptr: *mut c_void,
    store_ptr: *mut c_void,
    index: u32,
    out_type: *mut i32,
    out_value_i64: *mut i64,
    out_value_f64: *mut f64,
) -> c_int {
    use wasmtime::{ExnRef, OwnedRooted, RootScope};

    if exnref_ptr.is_null() || store_ptr.is_null() || out_type.is_null() {
        return -1;
    }

    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<(), crate::error::WasmtimeError> {
        let owned_exnref = unsafe { &*(exnref_ptr as *const OwnedRooted<ExnRef>) };
        let store = unsafe { &*(store_ptr as *const crate::store::Store) };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let exnref = owned_exnref.to_rooted(&mut scope);

        let val = exnref.field(&mut scope, index as usize).map_err(|e| {
            crate::error::WasmtimeError::Internal {
                message: format!("Failed to get field {} from ExnRef: {}", index, e),
            }
        })?;

        // Encode the value as type code + raw bits
        unsafe { encode_val_to_ffi(&val, out_type, out_value_i64, out_value_f64) };
        Ok(())
    }));

    match result {
        Ok(Ok(())) => 0,
        _ => -1,
    }
}

/// Gets the number of fields in an exception reference.
///
/// # Safety
/// - exnref_ptr must be a valid pointer to an OwnedRooted<ExnRef>
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_exnref_field_count(
    exnref_ptr: *mut c_void,
    store_ptr: *mut c_void,
) -> i32 {
    use wasmtime::{ExnRef, OwnedRooted, RootScope};

    if exnref_ptr.is_null() || store_ptr.is_null() {
        return -1;
    }

    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<i32, crate::error::WasmtimeError> {
        let owned_exnref = unsafe { &*(exnref_ptr as *const OwnedRooted<ExnRef>) };
        let store = unsafe { &*(store_ptr as *const crate::store::Store) };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let exnref = owned_exnref.to_rooted(&mut scope);

        let count = exnref.fields(&mut scope).map_err(|e| {
            crate::error::WasmtimeError::Internal {
                message: format!("Failed to get fields from ExnRef: {}", e),
            }
        })?.count();

        Ok(count as i32)
    }));

    match result {
        Ok(Ok(count)) => count,
        _ => -1,
    }
}

/// Helper to encode a wasmtime Val into FFI-safe output pointers.
///
/// # Safety
/// All output pointers must be valid and aligned.
unsafe fn encode_val_to_ffi(
    val: &wasmtime::Val,
    out_type: *mut i32,
    out_value_i64: *mut i64,
    out_value_f64: *mut f64,
) {
    match val {
        wasmtime::Val::I32(v) => {
            *out_type = 0;
            *out_value_i64 = *v as i64;
        }
        wasmtime::Val::I64(v) => {
            *out_type = 1;
            *out_value_i64 = *v;
        }
        wasmtime::Val::F32(v) => {
            *out_type = 2;
            *out_value_f64 = f32::from_bits(*v) as f64;
        }
        wasmtime::Val::F64(v) => {
            *out_type = 3;
            *out_value_f64 = f64::from_bits(*v);
        }
        _ => {
            // For reference types and V128, encode as type -1 (unsupported for now)
            *out_type = -1;
            *out_value_i64 = 0;
        }
    }
}

// ============================================================================
// Store Exception Functions
// ============================================================================

/// Checks if the store has a pending exception.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_has_pending_exception(store_ptr: *mut c_void) -> c_int {
    if store_ptr.is_null() {
        return 0;
    }
    unsafe {
        match crate::store::core::get_store_ref(store_ptr) {
            Ok(store) => {
                if store.has_pending_exception() {
                    1
                } else {
                    0
                }
            }
            Err(_) => 0,
        }
    }
}

/// Takes and removes the pending exception from the store.
/// Returns a handle to the ExnRef, or null if no exception is pending.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_take_pending_exception(
    store_ptr: *mut c_void,
) -> *mut c_void {
    if store_ptr.is_null() {
        return std::ptr::null_mut();
    }
    unsafe {
        match crate::store::core::get_store_ref(store_ptr) {
            Ok(store) => {
                match store.take_pending_exception() {
                    Some(exn_ref) => {
                        // Box the OwnedRooted<ExnRef> and return as handle
                        Box::into_raw(Box::new(exn_ref)) as *mut c_void
                    }
                    None => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
}
