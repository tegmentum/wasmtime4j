//! WebAssembly linear memory management with comprehensive bounds checking
//!
//! This module provides safe access to WebAssembly linear memory with defensive
//! programming patterns to prevent buffer overflows, segfaults, and data corruption.
//! All memory operations include comprehensive bounds checking and validation.

use std::sync::{Arc, Mutex, RwLock};
use std::time::Instant;
use std::collections::HashMap;
use wasmtime::{Memory as WasmtimeMemory, MemoryType};
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::store::Store;

/// Thread-safe WebAssembly memory wrapper with comprehensive bounds checking
#[derive(Debug)]
pub struct Memory {
    /// Wasmtime memory instance
    inner: WasmtimeMemory,
    /// Memory metadata and statistics
    metadata: Arc<RwLock<MemoryMetadata>>,
    /// Memory configuration and limits
    config: MemoryConfig,
}

/// Memory metadata and usage statistics
#[derive(Debug, Clone)]
pub struct MemoryMetadata {
    /// Timestamp when this memory was created
    pub created_at: Instant,
    /// Current size in pages (64KB each)
    pub current_pages: u64,
    /// Maximum allowed pages
    pub maximum_pages: Option<u64>,
    /// Total number of read operations performed
    pub read_operations: u64,
    /// Total number of write operations performed  
    pub write_operations: u64,
    /// Total bytes read from memory
    pub bytes_read: u64,
    /// Total bytes written to memory
    pub bytes_written: u64,
    /// Number of growth operations performed
    pub growth_operations: u64,
    /// Peak memory usage in pages
    pub peak_pages: u64,
    /// Last access timestamp
    pub last_access: Option<Instant>,
    /// Number of bounds check violations prevented
    pub bounds_violations_prevented: u64,
}

/// Memory configuration and limits
#[derive(Debug, Clone)]
pub struct MemoryConfig {
    /// Initial size in pages
    pub initial_pages: u64,
    /// Maximum allowed pages (None for unlimited)
    pub maximum_pages: Option<u64>,
    /// Whether memory is shared between threads
    pub is_shared: bool,
    /// Memory index within the module (for multi-memory support)
    pub memory_index: u32,
    /// Name of this memory (for debugging)
    pub name: Option<String>,
}

/// Builder for creating configured memory instances
#[derive(Debug)]
pub struct MemoryBuilder {
    initial_pages: u64,
    maximum_pages: Option<u64>,
    is_shared: bool,
    memory_index: u32,
    name: Option<String>,
}

/// Memory usage statistics and monitoring information
#[derive(Debug, Clone)]
pub struct MemoryUsage {
    /// Current memory size in bytes
    pub current_bytes: usize,
    /// Current memory size in pages
    pub current_pages: u64,
    /// Maximum possible memory size in bytes
    pub maximum_bytes: Option<usize>,
    /// Peak memory usage in bytes
    pub peak_bytes: usize,
    /// Total read operations
    pub read_count: u64,
    /// Total write operations
    pub write_count: u64,
    /// Total bytes transferred
    pub bytes_transferred: u64,
    /// Memory utilization percentage (0-100)
    pub utilization_percent: f64,
}

/// Memory data types for type-safe read/write operations
#[derive(Debug, Clone, Copy)]
pub enum MemoryDataType {
    /// 8-bit unsigned integer
    U8,
    /// 8-bit signed integer
    I8,
    /// 16-bit unsigned integer (little endian)
    U16Le,
    /// 16-bit signed integer (little endian)
    I16Le,
    /// 32-bit unsigned integer (little endian)
    U32Le,
    /// 32-bit signed integer (little endian)
    I32Le,
    /// 64-bit unsigned integer (little endian)
    U64Le,
    /// 64-bit signed integer (little endian)
    I64Le,
    /// 32-bit IEEE 754 float (little endian)
    F32Le,
    /// 64-bit IEEE 754 float (little endian)
    F64Le,
}

/// Memory access result with comprehensive error information
pub type MemoryResult<T> = Result<T, MemoryError>;

/// Specialized memory errors with detailed context
#[derive(Debug, thiserror::Error)]
pub enum MemoryError {
    /// Memory bounds check violation
    #[error("Memory bounds violation: attempted to access offset {offset} with length {length}, but memory size is {memory_size}")]
    BoundsViolation {
        offset: usize,
        length: usize,
        memory_size: usize,
    },
    /// Memory growth failure
    #[error("Memory growth failed: attempted to grow from {current_pages} to {requested_pages} pages, but maximum is {maximum_pages:?}")]
    GrowthFailure {
        current_pages: u64,
        requested_pages: u64,
        maximum_pages: Option<u64>,
    },
    /// Memory alignment error
    #[error("Memory alignment error: offset {offset} is not aligned for {data_type:?} (requires {required_alignment} byte alignment)")]
    AlignmentError {
        offset: usize,
        data_type: MemoryDataType,
        required_alignment: usize,
    },
    /// Memory sharing violation
    #[error("Memory sharing violation: {message}")]
    SharingViolation { message: String },
    /// Memory initialization error
    #[error("Memory initialization failed: {message}")]
    InitializationError { message: String },
}

impl From<MemoryError> for WasmtimeError {
    fn from(error: MemoryError) -> Self {
        WasmtimeError::Memory {
            message: error.to_string(),
        }
    }
}

impl MemoryBuilder {
    /// Create a new memory builder with default configuration
    pub fn new(initial_pages: u64) -> Self {
        Self {
            initial_pages,
            maximum_pages: None,
            is_shared: false,
            memory_index: 0,
            name: None,
        }
    }

    /// Set maximum pages limit
    pub fn maximum_pages(mut self, max_pages: u64) -> Self {
        self.maximum_pages = Some(max_pages);
        self
    }

    /// Enable shared memory
    pub fn shared(mut self) -> Self {
        self.is_shared = true;
        self
    }

    /// Set memory index for multi-memory support
    pub fn memory_index(mut self, index: u32) -> Self {
        self.memory_index = index;
        self
    }

    /// Set debug name for this memory
    pub fn name<S: Into<String>>(mut self, name: S) -> Self {
        self.name = Some(name.into());
        self
    }

    /// Build the memory instance
    pub fn build(self, store: &mut Store) -> WasmtimeResult<Memory> {
        Memory::new_with_config(store, self.into())
    }
}

impl From<MemoryBuilder> for MemoryConfig {
    fn from(builder: MemoryBuilder) -> Self {
        Self {
            initial_pages: builder.initial_pages,
            maximum_pages: builder.maximum_pages,
            is_shared: builder.is_shared,
            memory_index: builder.memory_index,
            name: builder.name,
        }
    }
}

impl Memory {
    /// Create a new memory with default configuration
    pub fn new(store: &mut Store, initial_pages: u64) -> WasmtimeResult<Self> {
        let config = MemoryConfig {
            initial_pages,
            maximum_pages: None,
            is_shared: false,
            memory_index: 0,
            name: None,
        };
        Self::new_with_config(store, config)
    }

    /// Create a new memory with specific configuration
    pub fn new_with_config(store: &mut Store, config: MemoryConfig) -> WasmtimeResult<Self> {
        // Validate configuration parameters
        if config.initial_pages == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Initial pages cannot be zero".to_string(),
            });
        }

        if let Some(max_pages) = config.maximum_pages {
            if config.initial_pages > max_pages {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Initial pages ({}) cannot exceed maximum pages ({})",
                        config.initial_pages, max_pages
                    ),
                });
            }
        }

        // Check WebAssembly memory limits (4GB max)
        const MAX_WASM_PAGES: u64 = 65536; // 4GB / 64KB
        if config.initial_pages > MAX_WASM_PAGES {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Initial pages ({}) exceeds WebAssembly limit ({})",
                    config.initial_pages, MAX_WASM_PAGES
                ),
            });
        }

        if let Some(max_pages) = config.maximum_pages {
            if max_pages > MAX_WASM_PAGES {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Maximum pages ({}) exceeds WebAssembly limit ({})",
                        max_pages, MAX_WASM_PAGES
                    ),
                });
            }
        }

        // Create Wasmtime memory type
        let memory_type = MemoryType::new64(
            config.initial_pages,
            config.maximum_pages,
        );

        // Create memory instance
        let inner = store.with_context(|ctx| {
            WasmtimeMemory::new(ctx, memory_type)
                .map_err(|e| WasmtimeError::Memory {
                    message: format!("Failed to create memory: {}", e),
                })
        })?;

        // Initialize metadata
        let metadata = MemoryMetadata {
            created_at: Instant::now(),
            current_pages: config.initial_pages,
            maximum_pages: config.maximum_pages,
            read_operations: 0,
            write_operations: 0,
            bytes_read: 0,
            bytes_written: 0,
            growth_operations: 0,
            peak_pages: config.initial_pages,
            last_access: None,
            bounds_violations_prevented: 0,
        };

        Ok(Self {
            inner,
            metadata: Arc::new(RwLock::new(metadata)),
            config,
        })
    }

    /// Get current memory size in pages
    pub fn size_pages(&self, store: &Store) -> WasmtimeResult<u64> {
        store.with_context_ro(|ctx| {
            Ok(self.inner.size(ctx))
        })
    }

    /// Get current memory size in bytes
    pub fn size_bytes(&self, store: &Store) -> WasmtimeResult<usize> {
        Ok((self.size_pages(store)? * 65536) as usize)
    }

    /// Get memory data size for bounds checking
    fn get_data_size(&self, store: &Store) -> WasmtimeResult<usize> {
        store.with_context_ro(|ctx| {
            Ok(self.inner.data(ctx).len())
        })
    }

    /// Grow memory by the specified number of pages with validation
    pub fn grow(&self, store: &mut Store, additional_pages: u64) -> WasmtimeResult<u64> {
        // Get current size
        let current_pages = self.size_pages(store)?;
        let requested_pages = current_pages + additional_pages;

        // Validate growth against limits
        if let Some(max_pages) = self.config.maximum_pages {
            if requested_pages > max_pages {
                return Err(MemoryError::GrowthFailure {
                    current_pages,
                    requested_pages,
                    maximum_pages: Some(max_pages),
                }.into());
            }
        }

        // Check WebAssembly memory limits
        const MAX_WASM_PAGES: u64 = 65536;
        if requested_pages > MAX_WASM_PAGES {
            return Err(MemoryError::GrowthFailure {
                current_pages,
                requested_pages,
                maximum_pages: Some(MAX_WASM_PAGES),
            }.into());
        }

        // Perform the growth operation
        let previous_pages = store.with_context(|ctx| {
            self.inner.grow(ctx, additional_pages)
                .map_err(|e| WasmtimeError::Memory {
                    message: format!("Memory growth failed: {}", e),
                })
        })?;

        // Update metadata
        if let Ok(mut metadata) = self.metadata.write() {
            metadata.current_pages = requested_pages;
            metadata.growth_operations += 1;
            if requested_pages > metadata.peak_pages {
                metadata.peak_pages = requested_pages;
            }
        }

        log::debug!(
            "Memory grown from {} to {} pages ({} bytes)",
            previous_pages,
            requested_pages,
            requested_pages * 65536
        );

        Ok(previous_pages)
    }

    /// Read data with comprehensive bounds checking
    pub fn read_bytes(&self, store: &Store, offset: usize, length: usize) -> WasmtimeResult<Vec<u8>> {
        // Bounds checking
        let memory_size = self.size_bytes(store)?;
        if offset.saturating_add(length) > memory_size {
            let mut metadata = self.metadata.write().map_err(|_| {
                WasmtimeError::Concurrency {
                    message: "Failed to acquire metadata lock".to_string(),
                }
            })?;
            metadata.bounds_violations_prevented += 1;
            
            return Err(MemoryError::BoundsViolation {
                offset,
                length,
                memory_size,
            }.into());
        }

        // Read memory data within the store lock
        let result = store.with_context_ro(|ctx| {
            let data = self.inner.data(ctx);
            Ok(data[offset..offset + length].to_vec())
        })?;

        // Update statistics
        if let Ok(mut metadata) = self.metadata.write() {
            metadata.read_operations += 1;
            metadata.bytes_read += length as u64;
            metadata.last_access = Some(Instant::now());
        }

        Ok(result)
    }

    /// Write data with comprehensive bounds checking
    pub fn write_bytes(&self, store: &mut Store, offset: usize, data: &[u8]) -> WasmtimeResult<()> {
        let length = data.len();
        
        // Bounds checking
        let memory_size = self.size_bytes(store)?;
        if offset.saturating_add(length) > memory_size {
            let mut metadata = self.metadata.write().map_err(|_| {
                WasmtimeError::Concurrency {
                    message: "Failed to acquire metadata lock".to_string(),
                }
            })?;
            metadata.bounds_violations_prevented += 1;
            
            return Err(MemoryError::BoundsViolation {
                offset,
                length,
                memory_size,
            }.into());
        }

        // Write memory data within the store lock
        store.with_context(|ctx| {
            let memory_data = self.inner.data_mut(ctx);
            memory_data[offset..offset + length].copy_from_slice(data);
            Ok(())
        })?;

        // Update statistics
        if let Ok(mut metadata) = self.metadata.write() {
            metadata.write_operations += 1;
            metadata.bytes_written += length as u64;
            metadata.last_access = Some(Instant::now());
        }

        Ok(())
    }

    /// Read typed value with alignment checking
    pub fn read_typed<T>(&self, store: &Store, offset: usize, data_type: MemoryDataType) -> WasmtimeResult<T>
    where
        T: Default + Copy,
    {
        // Check alignment
        let alignment = Self::get_type_alignment(data_type);
        if offset % alignment != 0 {
            return Err(MemoryError::AlignmentError {
                offset,
                data_type,
                required_alignment: alignment,
            }.into());
        }

        let type_size = Self::get_type_size(data_type);
        let bytes = self.read_bytes(store, offset, type_size)?;
        
        // Convert bytes to typed value
        Self::bytes_to_typed_value(&bytes, data_type)
    }

    /// Write typed value with alignment checking  
    pub fn write_typed<T>(&self, store: &mut Store, offset: usize, value: T, data_type: MemoryDataType) -> WasmtimeResult<()>
    where
        T: Copy,
    {
        // Check alignment
        let alignment = Self::get_type_alignment(data_type);
        if offset % alignment != 0 {
            return Err(MemoryError::AlignmentError {
                offset,
                data_type,
                required_alignment: alignment,
            }.into());
        }

        let bytes = Self::typed_value_to_bytes(value, data_type)?;
        self.write_bytes(store, offset, &bytes)
    }

    /// Get memory usage statistics
    pub fn get_usage(&self, store: &Store) -> WasmtimeResult<MemoryUsage> {
        let current_pages = self.size_pages(store)?;
        let current_bytes = (current_pages * 65536) as usize;
        let maximum_bytes = self.config.maximum_pages.map(|p| (p * 65536) as usize);
        
        let metadata = self.metadata.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire metadata lock".to_string(),
            }
        })?;

        let peak_bytes = (metadata.peak_pages * 65536) as usize;
        let bytes_transferred = metadata.bytes_read + metadata.bytes_written;
        
        let utilization_percent = if let Some(max_bytes) = maximum_bytes {
            (current_bytes as f64 / max_bytes as f64) * 100.0
        } else {
            0.0 // Unlimited memory
        };

        Ok(MemoryUsage {
            current_bytes,
            current_pages,
            maximum_bytes,
            peak_bytes,
            read_count: metadata.read_operations,
            write_count: metadata.write_operations,
            bytes_transferred,
            utilization_percent,
        })
    }

    /// Get memory metadata (read-only)
    pub fn get_metadata(&self) -> WasmtimeResult<MemoryMetadata> {
        let metadata = self.metadata.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire metadata lock".to_string(),
            }
        })?;
        Ok(metadata.clone())
    }

    /// Get memory configuration
    pub fn get_config(&self) -> &MemoryConfig {
        &self.config
    }

    /// Get Wasmtime memory handle for advanced operations
    pub fn as_wasmtime_memory(&self) -> &WasmtimeMemory {
        &self.inner
    }

    // Helper functions for type conversion
    
    fn get_type_size(data_type: MemoryDataType) -> usize {
        match data_type {
            MemoryDataType::U8 | MemoryDataType::I8 => 1,
            MemoryDataType::U16Le | MemoryDataType::I16Le => 2,
            MemoryDataType::U32Le | MemoryDataType::I32Le | MemoryDataType::F32Le => 4,
            MemoryDataType::U64Le | MemoryDataType::I64Le | MemoryDataType::F64Le => 8,
        }
    }

    fn get_type_alignment(data_type: MemoryDataType) -> usize {
        match data_type {
            MemoryDataType::U8 | MemoryDataType::I8 => 1,
            MemoryDataType::U16Le | MemoryDataType::I16Le => 2,
            MemoryDataType::U32Le | MemoryDataType::I32Le | MemoryDataType::F32Le => 4,
            MemoryDataType::U64Le | MemoryDataType::I64Le | MemoryDataType::F64Le => 8,
        }
    }

    fn bytes_to_typed_value<T>(bytes: &[u8], data_type: MemoryDataType) -> WasmtimeResult<T>
    where
        T: Default + Copy,
    {
        if bytes.len() != Self::get_type_size(data_type) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Byte array length {} does not match expected size {} for type {:?}",
                    bytes.len(),
                    Self::get_type_size(data_type),
                    data_type
                ),
            });
        }

        // Use unsafe casting to convert bytes to the target type
        // This is safe because we've validated the size above
        unsafe {
            match data_type {
                MemoryDataType::U8 => {
                    let value = bytes[0] as u8;
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I8 => {
                    let value = bytes[0] as i8;
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::U16Le => {
                    let value = u16::from_le_bytes([bytes[0], bytes[1]]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I16Le => {
                    let value = i16::from_le_bytes([bytes[0], bytes[1]]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::U32Le => {
                    let value = u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I32Le => {
                    let value = i32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::U64Le => {
                    let value = u64::from_le_bytes([
                        bytes[0], bytes[1], bytes[2], bytes[3],
                        bytes[4], bytes[5], bytes[6], bytes[7]
                    ]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I64Le => {
                    let value = i64::from_le_bytes([
                        bytes[0], bytes[1], bytes[2], bytes[3],
                        bytes[4], bytes[5], bytes[6], bytes[7]
                    ]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::F32Le => {
                    let bits = u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
                    let value = f32::from_bits(bits);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::F64Le => {
                    let bits = u64::from_le_bytes([
                        bytes[0], bytes[1], bytes[2], bytes[3],
                        bytes[4], bytes[5], bytes[6], bytes[7]
                    ]);
                    let value = f64::from_bits(bits);
                    Ok(std::mem::transmute_copy(&value))
                }
            }
        }
    }

    fn typed_value_to_bytes<T>(value: T, data_type: MemoryDataType) -> WasmtimeResult<Vec<u8>>
    where
        T: Copy,
    {
        // Use unsafe transmutation to convert typed values to bytes
        // This is safe for primitive numeric types
        unsafe {
            match data_type {
                MemoryDataType::U8 => {
                    let val: u8 = std::mem::transmute_copy(&value);
                    Ok(vec![val])
                }
                MemoryDataType::I8 => {
                    let val: i8 = std::mem::transmute_copy(&value);
                    Ok(vec![val as u8])
                }
                MemoryDataType::U16Le => {
                    let val: u16 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::I16Le => {
                    let val: i16 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::U32Le => {
                    let val: u32 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::I32Le => {
                    let val: i32 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::U64Le => {
                    let val: u64 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::I64Le => {
                    let val: i64 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::F32Le => {
                    let val: f32 = std::mem::transmute_copy(&value);
                    Ok(val.to_bits().to_le_bytes().to_vec())
                }
                MemoryDataType::F64Le => {
                    let val: f64 = std::mem::transmute_copy(&value);
                    Ok(val.to_bits().to_le_bytes().to_vec())
                }
            }
        }
    }
}

/// Thread-safe memory registry for managing multiple memory instances
pub struct MemoryRegistry {
    memories: Arc<Mutex<HashMap<u32, Arc<Memory>>>>,
}

impl MemoryRegistry {
    /// Create a new memory registry
    pub fn new() -> Self {
        Self {
            memories: Arc::new(Mutex::new(HashMap::new())),
        }
    }

    /// Register a memory instance
    pub fn register(&self, memory: Memory) -> WasmtimeResult<u32> {
        let mut memories = self.memories.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            }
        })?;
        
        let memory_id = memory.config.memory_index;
        memories.insert(memory_id, Arc::new(memory));
        
        log::debug!("Registered memory with ID {}", memory_id);
        Ok(memory_id)
    }

    /// Get memory instance by ID
    pub fn get(&self, memory_id: u32) -> WasmtimeResult<Arc<Memory>> {
        let memories = self.memories.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            }
        })?;
        
        memories.get(&memory_id).cloned().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Memory with ID {} not found", memory_id),
            }
        })
    }

    /// Remove memory instance
    pub fn unregister(&self, memory_id: u32) -> WasmtimeResult<()> {
        let mut memories = self.memories.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            }
        })?;
        
        memories.remove(&memory_id).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Memory with ID {} not found", memory_id),
            }
        })?;
        
        log::debug!("Unregistered memory with ID {}", memory_id);
        Ok(())
    }

    /// Get all registered memory IDs
    pub fn list_memories(&self) -> WasmtimeResult<Vec<u32>> {
        let memories = self.memories.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            }
        })?;
        
        Ok(memories.keys().cloned().collect())
    }
}

impl Default for MemoryRegistry {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;

    #[test]
    fn test_memory_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        assert_eq!(memory.size_pages(&store).unwrap(), 1);
        assert_eq!(memory.size_bytes(&store).unwrap(), 65536);
    }

    #[test]
    fn test_memory_growth() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let previous_pages = memory.grow(&mut store, 1).expect("Failed to grow memory");
        
        assert_eq!(previous_pages, 1);
        assert_eq!(memory.size_pages(&store).unwrap(), 2);
    }

    #[test]
    fn test_bounds_checking() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        
        // Should succeed - within bounds
        let result = memory.read_bytes(&store, 0, 100);
        assert!(result.is_ok());
        
        // Should fail - out of bounds  
        let result = memory.read_bytes(&store, 65536, 1);
        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), WasmtimeError::Memory { .. }));
    }

    #[test]
    fn test_memory_statistics() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        
        // Perform some operations
        memory.read_bytes(&store, 0, 100).expect("Read failed");
        memory.write_bytes(&mut store, 0, &[1, 2, 3, 4]).expect("Write failed");
        
        let usage = memory.get_usage(&store).expect("Failed to get usage");
        assert_eq!(usage.current_pages, 1);
        assert_eq!(usage.read_count, 1);
        assert_eq!(usage.write_count, 1);
    }

    #[test]
    fn test_memory_registry() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        let registry = MemoryRegistry::new();
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let memory_id = registry.register(memory).expect("Failed to register memory");
        
        let retrieved = registry.get(memory_id).expect("Failed to get memory");
        assert_eq!(retrieved.size_pages(&store).unwrap(), 1);
        
        registry.unregister(memory_id).expect("Failed to unregister memory");
        assert!(registry.get(memory_id).is_err());
    }
}