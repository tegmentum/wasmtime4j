//! Reference Types Support for WebAssembly
//!
//! This module provides native implementation for WebAssembly reference types including
//! function references (funcref), external references (externref), and any references.
//! All operations are implemented with defensive programming principles to ensure type
//! safety and prevent JVM crashes.

use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::ptr;
use std::os::raw::{c_void, c_int, c_char};
use std::ffi::{CStr, CString};

use wasmtime::{Val, RefType, HeapType, ValType, FuncType, Table, Store, Extern, Func};
use crate::error::{WasmtimeResult, WasmtimeError};

/// Maximum number of reference types to track
const MAX_REFERENCE_TYPES: usize = 1000;

/// Reference type information
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ReferenceTypeInfo {
    /// Type identifier
    pub type_id: u64,
    /// Heap type (0=func, 1=extern, 2=any, 3=none, 4=noextern, 5=nofunc)
    pub heap_type: u32,
    /// Whether this reference type is nullable
    pub nullable: bool,
    /// Human-readable description
    pub description: *mut c_char,
}

/// Reference value handle for safe passing between Java and native code
#[repr(C)]
#[derive(Debug)]
pub struct ReferenceValueHandle {
    /// Handle identifier
    pub handle_id: u64,
    /// Type identifier
    pub type_id: u64,
    /// Whether this is a null reference
    pub is_null: bool,
    /// Value type (0=funcref, 1=externref, 2=anyref)
    pub value_type: u32,
}

/// Reference types manager for tracking reference types and values
pub struct ReferenceTypesManager {
    /// Map of type ID to reference type info
    ref_types: Arc<RwLock<HashMap<u64, ReferenceTypeInfo>>>,
    /// Map of handle ID to actual values
    ref_values: Arc<RwLock<HashMap<u64, Val>>>,
    /// Map of host objects for external references
    host_objects: Arc<RwLock<HashMap<u64, Box<dyn std::any::Any + Send + Sync>>>>,
    /// Next available type ID
    next_type_id: Arc<Mutex<u64>>,
    /// Next available handle ID
    next_handle_id: Arc<Mutex<u64>>,
    /// Whether reference types are supported
    supported: Arc<Mutex<bool>>,
}

impl Default for ReferenceTypesManager {
    fn default() -> Self {
        Self::new()
    }
}

impl ReferenceTypesManager {
    /// Create a new reference types manager
    pub fn new() -> Self {
        Self {
            ref_types: Arc::new(RwLock::new(HashMap::new())),
            ref_values: Arc::new(RwLock::new(HashMap::new())),
            host_objects: Arc::new(RwLock::new(HashMap::new())),
            next_type_id: Arc::new(Mutex::new(1)),
            next_handle_id: Arc::new(Mutex::new(1)),
            supported: Arc::new(Mutex::new(true)),
        }
    }

    /// Check if reference types are supported
    pub fn are_reference_types_supported(&self) -> WasmtimeResult<bool> {
        let supported = self.supported.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types supported lock".to_string(),
            }
        })?;
        Ok(*supported)
    }

    /// Create a function reference type
    pub fn create_function_reference(&self, func_type: FuncType) -> WasmtimeResult<u64> {
        if !self.are_reference_types_supported()? {
            return Err(WasmtimeError::FeatureNotSupported {
                message: "Reference types are not supported".to_string(),
            });
        }

        let mut type_id_guard = self.next_type_id.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next type ID lock".to_string(),
            }
        })?;
        let type_id = *type_id_guard;
        *type_id_guard += 1;
        drop(type_id_guard);

        let description = CString::new(format!("funcref({})", func_type.params().len())).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid function type description".to_string(),
            }
        })?;

        let ref_type_info = ReferenceTypeInfo {
            type_id,
            heap_type: 0, // func
            nullable: true,
            description: description.into_raw(),
        };

        let mut ref_types = self.ref_types.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types write lock".to_string(),
            }
        })?;

        if ref_types.len() >= MAX_REFERENCE_TYPES {
            return Err(WasmtimeError::Memory {
                message: format!("Maximum number of reference types ({}) exceeded", MAX_REFERENCE_TYPES),
            });
        }

        ref_types.insert(type_id, ref_type_info);
        Ok(type_id)
    }

    /// Create an external reference type
    pub fn create_external_reference(&self) -> WasmtimeResult<u64> {
        if !self.are_reference_types_supported()? {
            return Err(WasmtimeError::FeatureNotSupported {
                message: "Reference types are not supported".to_string(),
            });
        }

        let mut type_id_guard = self.next_type_id.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next type ID lock".to_string(),
            }
        })?;
        let type_id = *type_id_guard;
        *type_id_guard += 1;
        drop(type_id_guard);

        let description = CString::new("externref").map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid external reference description".to_string(),
            }
        })?;

        let ref_type_info = ReferenceTypeInfo {
            type_id,
            heap_type: 1, // extern
            nullable: true,
            description: description.into_raw(),
        };

        let mut ref_types = self.ref_types.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types write lock".to_string(),
            }
        })?;

        ref_types.insert(type_id, ref_type_info);
        Ok(type_id)
    }

    /// Create an any reference type
    pub fn create_any_reference(&self) -> WasmtimeResult<u64> {
        if !self.are_reference_types_supported()? {
            return Err(WasmtimeError::FeatureNotSupported {
                message: "Reference types are not supported".to_string(),
            });
        }

        let mut type_id_guard = self.next_type_id.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next type ID lock".to_string(),
            }
        })?;
        let type_id = *type_id_guard;
        *type_id_guard += 1;
        drop(type_id_guard);

        let description = CString::new("anyref").map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid any reference description".to_string(),
            }
        })?;

        let ref_type_info = ReferenceTypeInfo {
            type_id,
            heap_type: 2, // any
            nullable: true,
            description: description.into_raw(),
        };

        let mut ref_types = self.ref_types.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types write lock".to_string(),
            }
        })?;

        ref_types.insert(type_id, ref_type_info);
        Ok(type_id)
    }

    /// Create a null reference value
    pub fn create_null_reference(&self, type_id: u64) -> WasmtimeResult<u64> {
        let ref_types = self.ref_types.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types read lock".to_string(),
            }
        })?;

        let ref_type_info = ref_types.get(&type_id).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Reference type with ID {} not found", type_id),
            }
        })?;

        if !ref_type_info.nullable {
            return Err(WasmtimeError::InvalidParameter {
                message: "Cannot create null reference for non-nullable type".to_string(),
            });
        }
        drop(ref_types);

        let mut handle_id_guard = self.next_handle_id.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next handle ID lock".to_string(),
            }
        })?;
        let handle_id = *handle_id_guard;
        *handle_id_guard += 1;
        drop(handle_id_guard);

        // Create null value based on heap type
        let null_val = match ref_type_info.heap_type {
            0 => Val::null_func_ref(), // func
            1 => Val::null_extern_ref(), // extern
            2 => Val::null_extern_ref(), // any (treat as extern for now)
            _ => return Err(WasmtimeError::InvalidParameter {
                message: "Unsupported heap type for null reference".to_string(),
            }),
        };

        let mut ref_values = self.ref_values.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values write lock".to_string(),
            }
        })?;

        ref_values.insert(handle_id, null_val);
        Ok(handle_id)
    }

    /// Check if a reference value is null
    pub fn is_null_reference(&self, handle_id: u64) -> WasmtimeResult<bool> {
        let ref_values = self.ref_values.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values read lock".to_string(),
            }
        })?;

        let val = ref_values.get(&handle_id).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Reference value with handle {} not found", handle_id),
            }
        })?;

        let is_null = match val {
            Val::FuncRef(None) | Val::ExternRef(None) => true,
            _ => false,
        };

        Ok(is_null)
    }

    /// Get the reference type of a value
    pub fn get_reference_type(&self, handle_id: u64) -> WasmtimeResult<u64> {
        let ref_values = self.ref_values.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values read lock".to_string(),
            }
        })?;

        let val = ref_values.get(&handle_id).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Reference value with handle {} not found", handle_id),
            }
        })?;

        // Determine type based on the value
        let heap_type = match val {
            Val::FuncRef(_) => 0, // func
            Val::ExternRef(_) => 1, // extern
            _ => return Err(WasmtimeError::InvalidParameter {
                message: "Value is not a reference type".to_string(),
            }),
        };
        drop(ref_values);

        // Find matching type ID
        let ref_types = self.ref_types.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types read lock".to_string(),
            }
        })?;

        for (type_id, ref_type_info) in ref_types.iter() {
            if ref_type_info.heap_type == heap_type {
                return Ok(*type_id);
            }
        }

        Err(WasmtimeError::InvalidParameter {
            message: "No matching reference type found".to_string(),
        })
    }

    /// Create a function reference value from a function
    pub fn create_function_reference_value(&self, func: Func) -> WasmtimeResult<u64> {
        let mut handle_id_guard = self.next_handle_id.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next handle ID lock".to_string(),
            }
        })?;
        let handle_id = *handle_id_guard;
        *handle_id_guard += 1;
        drop(handle_id_guard);

        let func_ref_val = Val::FuncRef(Some(func));

        let mut ref_values = self.ref_values.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values write lock".to_string(),
            }
        })?;

        ref_values.insert(handle_id, func_ref_val);
        Ok(handle_id)
    }

    /// Create an external reference value from a host object
    pub fn create_external_reference_value(&self, host_object: Box<dyn std::any::Any + Send + Sync>) -> WasmtimeResult<u64> {
        let mut handle_id_guard = self.next_handle_id.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next handle ID lock".to_string(),
            }
        })?;
        let handle_id = *handle_id_guard;
        *handle_id_guard += 1;
        drop(handle_id_guard);

        // Store the host object
        let mut host_objects = self.host_objects.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire host objects write lock".to_string(),
            }
        })?;
        host_objects.insert(handle_id, host_object);
        drop(host_objects);

        // Create the external reference value
        // For now, we'll create a null extern ref - in a real implementation,
        // this would properly wrap the host object
        let extern_ref_val = Val::null_extern_ref();

        let mut ref_values = self.ref_values.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values write lock".to_string(),
            }
        })?;

        ref_values.insert(handle_id, extern_ref_val);
        Ok(handle_id)
    }

    /// Extract host object from an external reference
    pub fn extract_host_object(&self, handle_id: u64) -> WasmtimeResult<&Box<dyn std::any::Any + Send + Sync>> {
        let host_objects = self.host_objects.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire host objects read lock".to_string(),
            }
        })?;

        host_objects.get(&handle_id).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Host object with handle {} not found", handle_id),
            }
        })
    }

    /// Set table element to a reference value
    pub fn set_table_element(
        &self,
        table: &Table,
        store: &mut Store<()>,
        index: usize,
        handle_id: u64,
    ) -> WasmtimeResult<()> {
        let ref_values = self.ref_values.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values read lock".to_string(),
            }
        })?;

        let val = ref_values.get(&handle_id).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Reference value with handle {} not found", handle_id),
            }
        })?;

        table.set(store, index as u32, val.clone()).map_err(|e| {
            WasmtimeError::TableOperation {
                message: format!("Failed to set table element: {}", e),
            }
        })?;

        Ok(())
    }

    /// Get table element as a reference value
    pub fn get_table_element(
        &self,
        table: &Table,
        store: &Store<()>,
        index: usize,
    ) -> WasmtimeResult<u64> {
        let val = table.get(store, index as u32).ok_or_else(|| {
            WasmtimeError::TableOutOfBounds {
                message: format!("Table index {} out of bounds", index),
            }
        })?;

        let mut handle_id_guard = self.next_handle_id.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next handle ID lock".to_string(),
            }
        })?;
        let handle_id = *handle_id_guard;
        *handle_id_guard += 1;
        drop(handle_id_guard);

        let mut ref_values = self.ref_values.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values write lock".to_string(),
            }
        })?;

        ref_values.insert(handle_id, val);
        Ok(handle_id)
    }

    /// Copy table elements between tables
    pub fn copy_table_elements(
        &self,
        source_table: &Table,
        source_store: &Store<()>,
        source_index: usize,
        dest_table: &Table,
        dest_store: &mut Store<()>,
        dest_index: usize,
        length: usize,
    ) -> WasmtimeResult<()> {
        for i in 0..length {
            let source_val = source_table.get(source_store, (source_index + i) as u32).ok_or_else(|| {
                WasmtimeError::TableOutOfBounds {
                    message: format!("Source table index {} out of bounds", source_index + i),
                }
            })?;

            dest_table.set(dest_store, (dest_index + i) as u32, source_val).map_err(|e| {
                WasmtimeError::TableOperation {
                    message: format!("Failed to set destination table element: {}", e),
                }
            })?;
        }

        Ok(())
    }

    /// Fill table elements with a reference value
    pub fn fill_table(
        &self,
        table: &Table,
        store: &mut Store<()>,
        index: usize,
        length: usize,
        handle_id: u64,
    ) -> WasmtimeResult<()> {
        let ref_values = self.ref_values.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values read lock".to_string(),
            }
        })?;

        let val = ref_values.get(&handle_id).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Reference value with handle {} not found", handle_id),
            }
        })?;

        for i in 0..length {
            table.set(store, (index + i) as u32, val.clone()).map_err(|e| {
                WasmtimeError::TableOperation {
                    message: format!("Failed to set table element: {}", e),
                }
            })?;
        }

        Ok(())
    }

    /// Get reference type information
    pub fn get_reference_type_info(&self, type_id: u64) -> WasmtimeResult<ReferenceTypeInfo> {
        let ref_types = self.ref_types.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types read lock".to_string(),
            }
        })?;

        ref_types.get(&type_id).cloned().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Reference type with ID {} not found", type_id),
            }
        })
    }

    /// Remove a reference value
    pub fn remove_reference_value(&self, handle_id: u64) -> WasmtimeResult<()> {
        let mut ref_values = self.ref_values.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference values write lock".to_string(),
            }
        })?;
        let mut host_objects = self.host_objects.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire host objects write lock".to_string(),
            }
        })?;

        ref_values.remove(&handle_id);
        host_objects.remove(&handle_id);

        Ok(())
    }

    /// Enable reference types support
    pub fn enable_reference_types(&self) -> WasmtimeResult<()> {
        let mut supported = self.supported.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types supported lock".to_string(),
            }
        })?;
        *supported = true;
        Ok(())
    }

    /// Disable reference types support
    pub fn disable_reference_types(&self) -> WasmtimeResult<()> {
        let mut supported = self.supported.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire reference types supported lock".to_string(),
            }
        })?;
        *supported = false;
        Ok(())
    }
}

/// Global instance of reference types manager
static REFERENCE_TYPES_MANAGER: std::sync::LazyLock<ReferenceTypesManager> =
    std::sync::LazyLock::new(ReferenceTypesManager::new);

// C API exports for reference types support

/// Check if reference types are supported
///
/// # Safety
///
/// The result parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_are_supported(result: *mut bool) -> c_int {
    if result.is_null() {
        return -1;
    }

    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.are_reference_types_supported() {
        Ok(supported) => {
            ptr::write(result, supported);
            0
        }
        Err(_) => -1,
    }
}

/// Create a function reference type
///
/// # Safety
///
/// All parameters must be valid pointers.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_create_func_ref(
    param_count: u32,
    result_count: u32,
    type_id: *mut u64,
) -> c_int {
    if type_id.is_null() {
        return -1;
    }

    let manager = &*REFERENCE_TYPES_MANAGER;

    // Create a mock function type (in real implementation, would use actual parameter types)
    let params: Vec<ValType> = (0..param_count).map(|_| ValType::I32).collect();
    let results: Vec<ValType> = (0..result_count).map(|_| ValType::I32).collect();
    let func_type = FuncType::new(params, results);

    match manager.create_function_reference(func_type) {
        Ok(id) => {
            ptr::write(type_id, id);
            0
        }
        Err(_) => -1,
    }
}

/// Create an external reference type
///
/// # Safety
///
/// The type_id parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_create_extern_ref(type_id: *mut u64) -> c_int {
    if type_id.is_null() {
        return -1;
    }

    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.create_external_reference() {
        Ok(id) => {
            ptr::write(type_id, id);
            0
        }
        Err(_) => -1,
    }
}

/// Create an any reference type
///
/// # Safety
///
/// The type_id parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_create_any_ref(type_id: *mut u64) -> c_int {
    if type_id.is_null() {
        return -1;
    }

    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.create_any_reference() {
        Ok(id) => {
            ptr::write(type_id, id);
            0
        }
        Err(_) => -1,
    }
}

/// Create a null reference value
///
/// # Safety
///
/// The handle_id parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_create_null_ref(
    type_id: u64,
    handle_id: *mut u64,
) -> c_int {
    if handle_id.is_null() {
        return -1;
    }

    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.create_null_reference(type_id) {
        Ok(id) => {
            ptr::write(handle_id, id);
            0
        }
        Err(_) => -1,
    }
}

/// Check if a reference value is null
///
/// # Safety
///
/// The result parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_is_null_ref(
    handle_id: u64,
    result: *mut bool,
) -> c_int {
    if result.is_null() {
        return -1;
    }

    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.is_null_reference(handle_id) {
        Ok(is_null) => {
            ptr::write(result, is_null);
            0
        }
        Err(_) => -1,
    }
}

/// Get the reference type of a value
///
/// # Safety
///
/// The type_id parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_get_ref_type(
    handle_id: u64,
    type_id: *mut u64,
) -> c_int {
    if type_id.is_null() {
        return -1;
    }

    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.get_reference_type(handle_id) {
        Ok(id) => {
            ptr::write(type_id, id);
            0
        }
        Err(_) => -1,
    }
}

/// Remove a reference value
///
/// # Safety
///
/// This function is safe to call with any handle ID.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_remove_ref_value(handle_id: u64) -> c_int {
    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.remove_reference_value(handle_id) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Enable reference types support
///
/// # Safety
///
/// This function is safe to call at any time.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_enable() -> c_int {
    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.enable_reference_types() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Disable reference types support
///
/// # Safety
///
/// This function is safe to call at any time.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_ref_types_disable() -> c_int {
    let manager = &*REFERENCE_TYPES_MANAGER;
    match manager.disable_reference_types() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use wasmtime::{Engine, Store, ValType};

    #[test]
    fn test_reference_types_manager_creation() {
        let manager = ReferenceTypesManager::new();
        assert!(manager.are_reference_types_supported().unwrap());
    }

    #[test]
    fn test_function_reference_creation() {
        let manager = ReferenceTypesManager::new();
        let func_type = FuncType::new(vec![ValType::I32], vec![ValType::I32]);
        let type_id = manager.create_function_reference(func_type).unwrap();
        assert!(type_id > 0);
    }

    #[test]
    fn test_external_reference_creation() {
        let manager = ReferenceTypesManager::new();
        let type_id = manager.create_external_reference().unwrap();
        assert!(type_id > 0);
    }

    #[test]
    fn test_any_reference_creation() {
        let manager = ReferenceTypesManager::new();
        let type_id = manager.create_any_reference().unwrap();
        assert!(type_id > 0);
    }

    #[test]
    fn test_null_reference_creation() {
        let manager = ReferenceTypesManager::new();
        let type_id = manager.create_external_reference().unwrap();
        let handle_id = manager.create_null_reference(type_id).unwrap();
        assert!(handle_id > 0);
        assert!(manager.is_null_reference(handle_id).unwrap());
    }

    #[test]
    fn test_reference_type_info() {
        let manager = ReferenceTypesManager::new();
        let type_id = manager.create_external_reference().unwrap();
        let info = manager.get_reference_type_info(type_id).unwrap();
        assert_eq!(info.heap_type, 1); // extern
        assert!(info.nullable);
    }

    #[test]
    fn test_external_reference_value() {
        let manager = ReferenceTypesManager::new();
        let host_object = Box::new(42i32);
        let handle_id = manager.create_external_reference_value(host_object).unwrap();
        assert!(handle_id > 0);

        // The host object should be stored
        let extracted = manager.extract_host_object(handle_id);
        assert!(extracted.is_ok());
    }

    #[test]
    fn test_invalid_reference_access() {
        let manager = ReferenceTypesManager::new();

        // Try to access non-existent reference
        let result = manager.is_null_reference(999);
        assert!(result.is_err());

        let result = manager.get_reference_type(999);
        assert!(result.is_err());
    }

    #[test]
    fn test_enable_disable_reference_types() {
        let manager = ReferenceTypesManager::new();

        assert!(manager.are_reference_types_supported().unwrap());

        manager.disable_reference_types().unwrap();
        assert!(!manager.are_reference_types_supported().unwrap());

        manager.enable_reference_types().unwrap();
        assert!(manager.are_reference_types_supported().unwrap());
    }

    #[test]
    fn test_c_api_safety() {
        unsafe {
            // Test null pointer handling
            let mut result = false;
            assert_eq!(wasmtime4j_ref_types_are_supported(&mut result), 0);
            assert!(result);

            assert_eq!(wasmtime4j_ref_types_are_supported(ptr::null_mut()), -1);

            let mut type_id = 0u64;
            assert_eq!(wasmtime4j_ref_types_create_extern_ref(&mut type_id), 0);
            assert!(type_id > 0);

            let mut handle_id = 0u64;
            assert_eq!(wasmtime4j_ref_types_create_null_ref(type_id, &mut handle_id), 0);
            assert!(handle_id > 0);

            let mut is_null = false;
            assert_eq!(wasmtime4j_ref_types_is_null_ref(handle_id, &mut is_null), 0);
            assert!(is_null);

            assert_eq!(wasmtime4j_ref_types_remove_ref_value(handle_id), 0);

            assert_eq!(wasmtime4j_ref_types_enable(), 0);
            assert_eq!(wasmtime4j_ref_types_disable(), 0);
        }
    }
}