//! Multi-Memory Support for WebAssembly
//!
//! This module provides native implementation for WebAssembly multi-memory features,
//! enabling modules to define and use multiple linear memory instances. All operations
//! are implemented with defensive programming principles to prevent JVM crashes.

use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::ptr;
use std::os::raw::{c_void, c_int};

use wasmtime::{Memory, MemoryType, Store, Limits};
use crate::error::{WasmtimeResult, WasmtimeError};

/// Maximum number of memory instances supported per module
const MAX_MEMORY_INSTANCES: usize = 100;

/// Multi-memory manager for tracking multiple memory instances
pub struct MultiMemoryManager {
    /// Map of memory index to memory instance
    memories: Arc<RwLock<HashMap<usize, Memory>>>,
    /// Map of memory index to memory type
    memory_types: Arc<RwLock<HashMap<usize, MemoryType>>>,
    /// Next available memory index
    next_index: Arc<Mutex<usize>>,
    /// Whether multi-memory support is enabled
    enabled: Arc<Mutex<bool>>,
}

impl Default for MultiMemoryManager {
    fn default() -> Self {
        Self::new()
    }
}

impl MultiMemoryManager {
    /// Create a new multi-memory manager
    pub fn new() -> Self {
        Self {
            memories: Arc::new(RwLock::new(HashMap::new())),
            memory_types: Arc::new(RwLock::new(HashMap::new())),
            next_index: Arc::new(Mutex::new(0)),
            enabled: Arc::new(Mutex::new(true)),
        }
    }

    /// Check if multi-memory support is enabled
    pub fn is_multi_memory_supported(&self) -> WasmtimeResult<bool> {
        let enabled = self.enabled.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire multi-memory enabled lock".to_string(),
            }
        })?;
        Ok(*enabled)
    }

    /// Add a memory instance to the manager
    pub fn add_memory(&self, store: &mut Store<()>, memory_type: MemoryType) -> WasmtimeResult<usize> {
        if !self.is_multi_memory_supported()? {
            return Err(WasmtimeError::FeatureNotSupported {
                message: "Multi-memory support is disabled".to_string(),
            });
        }

        let memories = self.memories.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memories read lock".to_string(),
            }
        })?;

        if memories.len() >= MAX_MEMORY_INSTANCES {
            return Err(WasmtimeError::Memory {
                message: format!("Maximum number of memory instances ({}) exceeded", MAX_MEMORY_INSTANCES),
            });
        }
        drop(memories);

        // Create the memory instance
        let memory = Memory::new(store, memory_type.clone()).map_err(|e| {
            WasmtimeError::Memory {
                message: format!("Failed to create memory instance: {}", e),
            }
        })?;

        // Get next available index
        let mut next_index = self.next_index.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next index lock".to_string(),
            }
        })?;
        let index = *next_index;
        *next_index += 1;
        drop(next_index);

        // Store the memory and its type
        let mut memories = self.memories.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memories write lock".to_string(),
            }
        })?;
        let mut memory_types = self.memory_types.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory types write lock".to_string(),
            }
        })?;

        memories.insert(index, memory);
        memory_types.insert(index, memory_type);

        Ok(index)
    }

    /// Get a memory instance by index
    pub fn get_memory(&self, index: usize) -> WasmtimeResult<Memory> {
        let memories = self.memories.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memories read lock".to_string(),
            }
        })?;

        memories.get(&index).cloned().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Memory instance at index {} not found", index),
            }
        })
    }

    /// Get all memory instances
    pub fn get_all_memories(&self) -> WasmtimeResult<Vec<(usize, Memory)>> {
        let memories = self.memories.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memories read lock".to_string(),
            }
        })?;

        Ok(memories.iter().map(|(&index, memory)| (index, memory.clone())).collect())
    }

    /// Get the number of memory instances
    pub fn get_memory_count(&self) -> WasmtimeResult<usize> {
        let memories = self.memories.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memories read lock".to_string(),
            }
        })?;
        Ok(memories.len())
    }

    /// Get the maximum number of memory instances supported
    pub fn get_max_memory_instances(&self) -> i32 {
        MAX_MEMORY_INSTANCES as i32
    }

    /// Copy data between two memory instances
    pub fn copy_between_memories(
        &self,
        store: &mut Store<()>,
        source_index: usize,
        source_offset: usize,
        dest_index: usize,
        dest_offset: usize,
        length: usize,
    ) -> WasmtimeResult<()> {
        if length == 0 {
            return Ok(());
        }

        // Get memory instances
        let source_memory = self.get_memory(source_index)?;
        let dest_memory = self.get_memory(dest_index)?;

        // Validate source bounds
        let source_size = source_memory.data_size(store);
        if source_offset.saturating_add(length) > source_size {
            return Err(WasmtimeError::MemoryOutOfBounds {
                message: format!(
                    "Source memory access out of bounds: offset={}, length={}, memory_size={}",
                    source_offset, length, source_size
                ),
            });
        }

        // Validate destination bounds
        let dest_size = dest_memory.data_size(store);
        if dest_offset.saturating_add(length) > dest_size {
            return Err(WasmtimeError::MemoryOutOfBounds {
                message: format!(
                    "Destination memory access out of bounds: offset={}, length={}, memory_size={}",
                    dest_offset, length, dest_size
                ),
            });
        }

        // Perform the copy operation
        unsafe {
            let source_data = source_memory.data_ptr(store);
            let dest_data = dest_memory.data_ptr(store);

            if source_data.is_null() || dest_data.is_null() {
                return Err(WasmtimeError::Memory {
                    message: "Invalid memory data pointer".to_string(),
                });
            }

            // Use memmove for safe overlapping copy (even though different memories)
            ptr::copy(
                source_data.add(source_offset),
                dest_data.add(dest_offset),
                length,
            );
        }

        Ok(())
    }

    /// Fill a memory instance with a specified byte value
    pub fn fill_memory(
        &self,
        store: &mut Store<()>,
        index: usize,
        offset: usize,
        length: usize,
        value: u8,
    ) -> WasmtimeResult<()> {
        if length == 0 {
            return Ok(());
        }

        let memory = self.get_memory(index)?;

        // Validate bounds
        let memory_size = memory.data_size(store);
        if offset.saturating_add(length) > memory_size {
            return Err(WasmtimeError::MemoryOutOfBounds {
                message: format!(
                    "Memory fill out of bounds: offset={}, length={}, memory_size={}",
                    offset, length, memory_size
                ),
            });
        }

        // Perform the fill operation
        unsafe {
            let data_ptr = memory.data_ptr(store);
            if data_ptr.is_null() {
                return Err(WasmtimeError::Memory {
                    message: "Invalid memory data pointer".to_string(),
                });
            }

            ptr::write_bytes(data_ptr.add(offset), value, length);
        }

        Ok(())
    }

    /// Grow a memory instance by the specified number of pages
    pub fn grow_memory(
        &self,
        store: &mut Store<()>,
        index: usize,
        delta_pages: usize,
    ) -> WasmtimeResult<bool> {
        let memory = self.get_memory(index)?;

        match memory.grow(store, delta_pages as u64) {
            Ok(_) => Ok(true),
            Err(_) => Ok(false), // Growth failed but not an error
        }
    }

    /// Get the current page count for a memory instance
    pub fn get_memory_page_count(&self, store: &Store<()>, index: usize) -> WasmtimeResult<u32> {
        let memory = self.get_memory(index)?;
        Ok(memory.size(store) as u32)
    }

    /// Get the current byte size for a memory instance
    pub fn get_memory_byte_size(&self, store: &Store<()>, index: usize) -> WasmtimeResult<u64> {
        let memory = self.get_memory(index)?;
        Ok(memory.data_size(store) as u64)
    }

    /// Remove a memory instance from the manager
    pub fn remove_memory(&self, index: usize) -> WasmtimeResult<()> {
        let mut memories = self.memories.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memories write lock".to_string(),
            }
        })?;
        let mut memory_types = self.memory_types.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory types write lock".to_string(),
            }
        })?;

        memories.remove(&index);
        memory_types.remove(&index);

        Ok(())
    }

    /// Clear all memory instances
    pub fn clear_all_memories(&self) -> WasmtimeResult<()> {
        let mut memories = self.memories.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memories write lock".to_string(),
            }
        })?;
        let mut memory_types = self.memory_types.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory types write lock".to_string(),
            }
        })?;

        memories.clear();
        memory_types.clear();

        // Reset next index
        let mut next_index = self.next_index.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire next index lock".to_string(),
            }
        })?;
        *next_index = 0;

        Ok(())
    }

    /// Enable multi-memory support
    pub fn enable_multi_memory(&self) -> WasmtimeResult<()> {
        let mut enabled = self.enabled.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire multi-memory enabled lock".to_string(),
            }
        })?;
        *enabled = true;
        Ok(())
    }

    /// Disable multi-memory support
    pub fn disable_multi_memory(&self) -> WasmtimeResult<()> {
        let mut enabled = self.enabled.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire multi-memory enabled lock".to_string(),
            }
        })?;
        *enabled = false;
        Ok(())
    }
}

/// Global instance of multi-memory manager
static MULTI_MEMORY_MANAGER: std::sync::LazyLock<MultiMemoryManager> =
    std::sync::LazyLock::new(MultiMemoryManager::new);

// C API exports for multi-memory support

/// Check if multi-memory support is available
///
/// # Safety
///
/// The result parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_is_supported(result: *mut bool) -> c_int {
    if result.is_null() {
        return -1;
    }

    let manager = &*MULTI_MEMORY_MANAGER;
    match manager.is_multi_memory_supported() {
        Ok(supported) => {
            ptr::write(result, supported);
            0
        }
        Err(_) => -1,
    }
}

/// Create a new memory instance
///
/// # Safety
///
/// All parameters must be valid pointers.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_create(
    store_handle: *mut c_void,
    min_pages: u32,
    max_pages: i32, // -1 for unlimited
    memory_index: *mut usize,
) -> c_int {
    if store_handle.is_null() || memory_index.is_null() {
        return -1;
    }

    // Validate parameters
    if min_pages > u32::MAX / 65536 {
        return -2; // Invalid minimum pages
    }

    let manager = &*MULTI_MEMORY_MANAGER;

    // Create memory type
    let limits = if max_pages < 0 {
        Limits::new(min_pages as u64, None)
    } else {
        if max_pages < min_pages as i32 {
            return -3; // Invalid maximum pages
        }
        Limits::new(min_pages as u64, Some(max_pages as u64))
    };

    let memory_type = MemoryType::new(limits);

    // For now, we'll use a mock store - in real implementation this would be the actual store
    // let store = match crate::store::get_store_ref_mut(store_handle) {
    //     Ok(store) => store,
    //     Err(_) => return -4,
    // };

    // Create a mock store for now
    let engine = wasmtime::Engine::default();
    let mut store = Store::new(&engine, ());

    match manager.add_memory(&mut store, memory_type) {
        Ok(index) => {
            ptr::write(memory_index, index);
            0
        }
        Err(_) => -5,
    }
}

/// Get the number of memory instances
///
/// # Safety
///
/// The count parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_get_count(count: *mut usize) -> c_int {
    if count.is_null() {
        return -1;
    }

    let manager = &*MULTI_MEMORY_MANAGER;
    match manager.get_memory_count() {
        Ok(memory_count) => {
            ptr::write(count, memory_count);
            0
        }
        Err(_) => -1,
    }
}

/// Get the maximum number of memory instances supported
///
/// # Safety
///
/// The max_count parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_get_max_instances(max_count: *mut i32) -> c_int {
    if max_count.is_null() {
        return -1;
    }

    let manager = &*MULTI_MEMORY_MANAGER;
    ptr::write(max_count, manager.get_max_memory_instances());
    0
}

/// Copy data between two memory instances
///
/// # Safety
///
/// All parameters must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_copy_between(
    store_handle: *mut c_void,
    source_index: usize,
    source_offset: usize,
    dest_index: usize,
    dest_offset: usize,
    length: usize,
) -> c_int {
    if store_handle.is_null() {
        return -1;
    }

    let manager = &*MULTI_MEMORY_MANAGER;

    // For now, we'll use a mock store - in real implementation this would be the actual store
    let engine = wasmtime::Engine::default();
    let mut store = Store::new(&engine, ());

    match manager.copy_between_memories(
        &mut store,
        source_index,
        source_offset,
        dest_index,
        dest_offset,
        length,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Fill a memory instance with a byte value
///
/// # Safety
///
/// All parameters must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_fill(
    store_handle: *mut c_void,
    memory_index: usize,
    offset: usize,
    length: usize,
    value: u8,
) -> c_int {
    if store_handle.is_null() {
        return -1;
    }

    let manager = &*MULTI_MEMORY_MANAGER;

    // For now, we'll use a mock store - in real implementation this would be the actual store
    let engine = wasmtime::Engine::default();
    let mut store = Store::new(&engine, ());

    match manager.fill_memory(&mut store, memory_index, offset, length, value) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Grow a memory instance by the specified number of pages
///
/// # Safety
///
/// All parameters must be valid pointers.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_grow(
    store_handle: *mut c_void,
    memory_index: usize,
    delta_pages: usize,
    result: *mut bool,
) -> c_int {
    if store_handle.is_null() || result.is_null() {
        return -1;
    }

    let manager = &*MULTI_MEMORY_MANAGER;

    // For now, we'll use a mock store - in real implementation this would be the actual store
    let engine = wasmtime::Engine::default();
    let mut store = Store::new(&engine, ());

    match manager.grow_memory(&mut store, memory_index, delta_pages) {
        Ok(success) => {
            ptr::write(result, success);
            0
        }
        Err(_) => -1,
    }
}

/// Get the current page count for a memory instance
///
/// # Safety
///
/// All parameters must be valid pointers.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_get_page_count(
    store_handle: *mut c_void,
    memory_index: usize,
    page_count: *mut u32,
) -> c_int {
    if store_handle.is_null() || page_count.is_null() {
        return -1;
    }

    let manager = &*MULTI_MEMORY_MANAGER;

    // For now, we'll use a mock store - in real implementation this would be the actual store
    let engine = wasmtime::Engine::default();
    let store = Store::new(&engine, ());

    match manager.get_memory_page_count(&store, memory_index) {
        Ok(pages) => {
            ptr::write(page_count, pages);
            0
        }
        Err(_) => -1,
    }
}

/// Get the current byte size for a memory instance
///
/// # Safety
///
/// All parameters must be valid pointers.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_get_byte_size(
    store_handle: *mut c_void,
    memory_index: usize,
    byte_size: *mut u64,
) -> c_int {
    if store_handle.is_null() || byte_size.is_null() {
        return -1;
    }

    let manager = &*MULTI_MEMORY_MANAGER;

    // For now, we'll use a mock store - in real implementation this would be the actual store
    let engine = wasmtime::Engine::default();
    let store = Store::new(&engine, ());

    match manager.get_memory_byte_size(&store, memory_index) {
        Ok(size) => {
            ptr::write(byte_size, size);
            0
        }
        Err(_) => -1,
    }
}

/// Remove a memory instance
///
/// # Safety
///
/// All parameters must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_remove(memory_index: usize) -> c_int {
    let manager = &*MULTI_MEMORY_MANAGER;
    match manager.remove_memory(memory_index) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Clear all memory instances
///
/// # Safety
///
/// This function is safe to call at any time.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_clear_all() -> c_int {
    let manager = &*MULTI_MEMORY_MANAGER;
    match manager.clear_all_memories() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Enable multi-memory support
///
/// # Safety
///
/// This function is safe to call at any time.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_enable() -> c_int {
    let manager = &*MULTI_MEMORY_MANAGER;
    match manager.enable_multi_memory() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Disable multi-memory support
///
/// # Safety
///
/// This function is safe to call at any time.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_multi_memory_disable() -> c_int {
    let manager = &*MULTI_MEMORY_MANAGER;
    match manager.disable_multi_memory() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use wasmtime::{Engine, Limits};

    #[test]
    fn test_multi_memory_manager_creation() {
        let manager = MultiMemoryManager::new();
        assert!(manager.is_multi_memory_supported().unwrap());
        assert_eq!(manager.get_memory_count().unwrap(), 0);
    }

    #[test]
    fn test_memory_addition() {
        let manager = MultiMemoryManager::new();
        let engine = Engine::default();
        let mut store = Store::new(&engine, ());

        let memory_type = MemoryType::new(Limits::new(1, Some(10)));
        let index = manager.add_memory(&mut store, memory_type).unwrap();

        assert_eq!(index, 0);
        assert_eq!(manager.get_memory_count().unwrap(), 1);
    }

    #[test]
    fn test_memory_retrieval() {
        let manager = MultiMemoryManager::new();
        let engine = Engine::default();
        let mut store = Store::new(&engine, ());

        let memory_type = MemoryType::new(Limits::new(1, Some(10)));
        let index = manager.add_memory(&mut store, memory_type).unwrap();

        let memory = manager.get_memory(index).unwrap();
        assert_eq!(memory.size(&store), 1);
    }

    #[test]
    fn test_memory_copy_between() {
        let manager = MultiMemoryManager::new();
        let engine = Engine::default();
        let mut store = Store::new(&engine, ());

        // Create two memory instances
        let memory_type1 = MemoryType::new(Limits::new(1, Some(10)));
        let memory_type2 = MemoryType::new(Limits::new(1, Some(10)));
        let index1 = manager.add_memory(&mut store, memory_type1).unwrap();
        let index2 = manager.add_memory(&mut store, memory_type2).unwrap();

        // Test copy between memories (this would work with actual memory data)
        let result = manager.copy_between_memories(&mut store, index1, 0, index2, 0, 64);
        assert!(result.is_ok());
    }

    #[test]
    fn test_memory_fill() {
        let manager = MultiMemoryManager::new();
        let engine = Engine::default();
        let mut store = Store::new(&engine, ());

        let memory_type = MemoryType::new(Limits::new(1, Some(10)));
        let index = manager.add_memory(&mut store, memory_type).unwrap();

        let result = manager.fill_memory(&mut store, index, 0, 100, 0xFF);
        assert!(result.is_ok());
    }

    #[test]
    fn test_memory_growth() {
        let manager = MultiMemoryManager::new();
        let engine = Engine::default();
        let mut store = Store::new(&engine, ());

        let memory_type = MemoryType::new(Limits::new(1, Some(10)));
        let index = manager.add_memory(&mut store, memory_type).unwrap();

        let initial_pages = manager.get_memory_page_count(&store, index).unwrap();
        let growth_result = manager.grow_memory(&mut store, index, 2).unwrap();

        if growth_result {
            let new_pages = manager.get_memory_page_count(&store, index).unwrap();
            assert!(new_pages > initial_pages);
        }
    }

    #[test]
    fn test_memory_bounds_validation() {
        let manager = MultiMemoryManager::new();
        let engine = Engine::default();
        let mut store = Store::new(&engine, ());

        let memory_type = MemoryType::new(Limits::new(1, Some(1))); // Single page
        let index = manager.add_memory(&mut store, memory_type).unwrap();

        // Try to fill beyond memory bounds
        let result = manager.fill_memory(&mut store, index, 65536, 1, 0xFF);
        assert!(result.is_err());
    }

    #[test]
    fn test_invalid_memory_index() {
        let manager = MultiMemoryManager::new();

        let result = manager.get_memory(999);
        assert!(result.is_err());
    }

    #[test]
    fn test_max_memory_instances() {
        let manager = MultiMemoryManager::new();
        assert_eq!(manager.get_max_memory_instances(), MAX_MEMORY_INSTANCES as i32);
    }

    #[test]
    fn test_enable_disable_multi_memory() {
        let manager = MultiMemoryManager::new();

        assert!(manager.is_multi_memory_supported().unwrap());

        manager.disable_multi_memory().unwrap();
        assert!(!manager.is_multi_memory_supported().unwrap());

        manager.enable_multi_memory().unwrap();
        assert!(manager.is_multi_memory_supported().unwrap());
    }

    #[test]
    fn test_c_api_safety() {
        unsafe {
            // Test null pointer handling
            let mut result = false;
            assert_eq!(wasmtime4j_multi_memory_is_supported(&mut result), 0);
            assert!(result);

            assert_eq!(wasmtime4j_multi_memory_is_supported(ptr::null_mut()), -1);

            let mut count = 0;
            assert_eq!(wasmtime4j_multi_memory_get_count(&mut count), 0);

            let mut max_count = 0;
            assert_eq!(wasmtime4j_multi_memory_get_max_instances(&mut max_count), 0);
            assert_eq!(max_count, MAX_MEMORY_INSTANCES as i32);

            assert_eq!(wasmtime4j_multi_memory_enable(), 0);
            assert_eq!(wasmtime4j_multi_memory_disable(), 0);
            assert_eq!(wasmtime4j_multi_memory_clear_all(), 0);
        }
    }
}