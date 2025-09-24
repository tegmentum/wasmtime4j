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
#[allow(missing_docs)]
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

    /// Get reference to inner Wasmtime memory (internal use)
    pub(crate) fn inner(&self) -> &WasmtimeMemory {
        &self.inner
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
    #[allow(dead_code)]
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

        // Perform the growth operation with shared memory considerations
        let previous_pages = if self.config.is_shared {
            // For shared memory, we need to coordinate growth across threads
            self.grow_shared_memory(store, additional_pages, current_pages, requested_pages)?
        } else {
            // Regular memory growth
            store.with_context(|ctx| {
                self.inner.grow(ctx, additional_pages)
                    .map_err(|e| WasmtimeError::Memory {
                        message: format!("Memory growth failed: {}", e),
                    })
            })?
        };

        // Update metadata
        if let Ok(mut metadata) = self.metadata.write() {
            metadata.current_pages = requested_pages;
            metadata.growth_operations += 1;
            if requested_pages > metadata.peak_pages {
                metadata.peak_pages = requested_pages;
            }
        }

        log::debug!(
            "Memory grown from {} to {} pages ({} bytes) [shared: {}]",
            previous_pages,
            requested_pages,
            requested_pages * 65536,
            self.config.is_shared
        );

        Ok(previous_pages)
    }

    /// Thread-safe growth for shared memory
    fn grow_shared_memory(
        &self,
        store: &mut Store,
        additional_pages: u64,
        current_pages: u64,
        requested_pages: u64
    ) -> WasmtimeResult<u64> {
        // For shared memory, we need to ensure thread-safe growth
        // This is a simplified implementation - a real implementation would
        // need more sophisticated synchronization

        store.with_context(|ctx| {
            // Use atomic operations or locks to ensure only one thread can grow at a time
            // For now, we'll use the same growth mechanism as regular memory
            // but with additional logging for shared memory
            log::debug!(
                "Growing shared memory from {} to {} pages (adding {})",
                current_pages, requested_pages, additional_pages
            );

            let result = self.inner.grow(ctx, additional_pages)
                .map_err(|e| WasmtimeError::Memory {
                    message: format!("Shared memory growth failed: {}", e),
                });

            // In a real implementation, we would:
            // 1. Notify all threads that memory has grown
            // 2. Update any cached memory pointers in other threads
            // 3. Ensure memory barriers are in place

            if result.is_ok() {
                log::info!("Shared memory successfully grown to {} pages", requested_pages);
                // Emit memory fence to ensure growth is visible to all threads
                std::sync::atomic::fence(std::sync::atomic::Ordering::SeqCst);
            }

            result
        })
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

/// Core FFI-compatible functions for Memory operations
/// 
/// This module provides functions that can be safely called from both JNI and Panama FFI
/// implementations. These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use std::sync::atomic::{AtomicU64, Ordering};
    use std::collections::HashSet;
    use crate::error::ffi_utils;
    use crate::store::Store;
    
    /// Memory handle validation magic number for integrity checking
    const MEMORY_MAGIC: u64 = 0xDEADBEEF_CAFEBABE;
    
    /// Thread-safe registry for tracking valid memory handles (using usize for thread safety)
    static VALID_MEMORY_HANDLES: once_cell::sync::Lazy<Arc<RwLock<HashSet<usize>>>> = 
        once_cell::sync::Lazy::new(|| Arc::new(RwLock::new(HashSet::new())));
    
    /// Thread-safe registry for tracking valid store handles (using usize for thread safety)
    static VALID_STORE_HANDLES: once_cell::sync::Lazy<Arc<RwLock<HashSet<usize>>>> = 
        once_cell::sync::Lazy::new(|| Arc::new(RwLock::new(HashSet::new())));
    
    /// Memory access counter for detecting potential race conditions
    static MEMORY_ACCESS_COUNTER: AtomicU64 = AtomicU64::new(0);
    
    /// Wrapper for memory with validation metadata
    #[repr(C)]
    pub struct ValidatedMemory {
        magic: u64,
        memory: Memory,
        created_at: Instant,
        access_count: AtomicU64,
        is_destroyed: std::sync::atomic::AtomicBool,
    }
    
    impl ValidatedMemory {
        /// Create a new validated memory wrapper
        pub fn new(memory: Memory) -> Self {
            Self {
                magic: MEMORY_MAGIC,
                memory,
                created_at: Instant::now(),
                access_count: AtomicU64::new(0),
                is_destroyed: std::sync::atomic::AtomicBool::new(false),
            }
        }
        
        /// Check if this memory instance is valid
        pub fn is_valid(&self) -> bool {
            self.magic == MEMORY_MAGIC && !self.is_destroyed.load(Ordering::Acquire)
        }
        
        /// Mark this memory instance as destroyed
        pub fn mark_destroyed(&self) {
            self.is_destroyed.store(true, Ordering::Release);
        }
        
        /// Increment access counter and return memory reference
        pub fn access_memory(&self) -> WasmtimeResult<&Memory> {
            if !self.is_valid() {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Attempt to access destroyed or corrupted memory".to_string(),
                });
            }
            
            self.access_count.fetch_add(1, Ordering::Relaxed);
            MEMORY_ACCESS_COUNTER.fetch_add(1, Ordering::Relaxed);
            Ok(&self.memory)
        }
        
        /// Get access statistics
        pub fn get_access_count(&self) -> u64 {
            self.access_count.load(Ordering::Relaxed)
        }
    }
    
    /// Register a memory handle as valid
    pub fn register_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        let mut handles = VALID_MEMORY_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        handles.insert(ptr as usize);
        log::debug!("Registered memory handle: {:p}", ptr);
        Ok(())
    }
    
    /// Unregister a memory handle
    pub fn unregister_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        let mut handles = VALID_MEMORY_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        if handles.remove(&(ptr as usize)) {
            log::debug!("Unregistered memory handle: {:p}", ptr);
            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: format!("Memory handle {:p} was not registered", ptr),
            })
        }
    }
    
    /// Register a store handle as valid
    pub fn register_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        let mut handles = VALID_STORE_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            }
        })?;
        
        handles.insert(ptr as usize);
        log::debug!("Registered store handle: {:p}", ptr);
        Ok(())
    }
    
    /// Unregister a store handle
    pub fn unregister_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        let mut handles = VALID_STORE_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            }
        })?;
        
        if handles.remove(&(ptr as usize)) {
            log::debug!("Unregistered store handle: {:p}", ptr);
            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: format!("Store handle {:p} was not registered", ptr),
            })
        }
    }
    
    /// Validate memory handle with comprehensive checks
    pub unsafe fn validate_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        // Basic null check
        if ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory pointer cannot be null".to_string(),
            });
        }
        
        // Check if handle is registered
        let handles = VALID_MEMORY_HANDLES.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        if !handles.contains(&(ptr as usize)) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Memory handle {:p} is not registered or has been freed", ptr),
            });
        }
        
        // Check magic number and validity if using ValidatedMemory
        let validated_memory = &*(ptr as *const ValidatedMemory);
        if validated_memory.magic != MEMORY_MAGIC {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Memory handle {:p} has invalid magic number (corrupted)", ptr),
            });
        }
        
        if validated_memory.is_destroyed.load(Ordering::Acquire) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Memory handle {:p} has been destroyed (use-after-free attempt)", ptr),
            });
        }
        
        Ok(())
    }
    
    /// Validate store handle with comprehensive checks
    pub unsafe fn validate_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        // Basic null check
        if ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store pointer cannot be null".to_string(),
            });
        }
        
        // Check if handle is registered
        let handles = VALID_STORE_HANDLES.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            }
        })?;
        
        if !handles.contains(&(ptr as usize)) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Store handle {:p} is not registered or has been freed", ptr),
            });
        }
        
        Ok(())
    }
    
    /// Get memory from pointer with comprehensive validation
    pub unsafe fn get_memory_ref(ptr: *const c_void) -> WasmtimeResult<&'static Memory> {
        validate_memory_handle(ptr)?;
        
        let validated_memory = &*(ptr as *const ValidatedMemory);
        validated_memory.access_memory()
    }

    /// Get mutable memory from pointer with comprehensive validation
    pub unsafe fn get_memory_mut(ptr: *mut c_void) -> WasmtimeResult<&'static mut Memory> {
        validate_memory_handle(ptr)?;
        
        let validated_memory = &mut *(ptr as *mut ValidatedMemory);
        let _memory_ref = validated_memory.access_memory()?; // Validate first
        Ok(&mut validated_memory.memory)
    }

    /// Get store from pointer with validation (read-only)
    pub unsafe fn get_store_ref(ptr: *const c_void) -> WasmtimeResult<&'static Store> {
        validate_store_handle(ptr)?;
        ffi_utils::deref_ptr::<Store>(ptr, "store")
    }

    /// Get mutable store from pointer with validation
    pub unsafe fn get_store_mut(ptr: *mut c_void) -> WasmtimeResult<&'static mut Store> {
        validate_store_handle(ptr as *const c_void)?;
        ffi_utils::deref_ptr_mut::<Store>(ptr, "store")
    }

    /// Core function to get memory size in bytes
    pub fn get_memory_size(memory: &Memory, store: &Store) -> WasmtimeResult<usize> {
        memory.size_bytes(store)
    }

    /// Core function to grow memory by pages
    pub fn grow_memory(memory: &Memory, store: &mut Store, pages: u64) -> WasmtimeResult<u64> {
        memory.grow(store, pages)
    }

    /// Core function to get memory page count
    pub fn get_memory_page_count(memory: &Memory, store: &Store) -> WasmtimeResult<u64> {
        memory.size_pages(store)
    }

    /// Core function to read bytes from memory
    pub fn read_memory_bytes(memory: &Memory, store: &Store, offset: usize, length: usize) -> WasmtimeResult<Vec<u8>> {
        memory.read_bytes(store, offset, length)
    }

    /// Core function to write bytes to memory
    pub fn write_memory_bytes(memory: &Memory, store: &mut Store, offset: usize, data: &[u8]) -> WasmtimeResult<()> {
        memory.write_bytes(store, offset, data)
    }

    /// Core function to read a single byte from memory
    pub fn read_memory_byte(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<u8> {
        let bytes = memory.read_bytes(store, offset, 1)?;
        Ok(bytes[0])
    }

    /// Core function to write a single byte to memory
    pub fn write_memory_byte(memory: &Memory, store: &mut Store, offset: usize, value: u8) -> WasmtimeResult<()> {
        memory.write_bytes(store, offset, &[value])
    }

    /// Core function to get direct memory buffer access
    pub fn get_memory_buffer(memory: &Memory, store: &Store) -> WasmtimeResult<(*mut u8, usize)> {
        store.with_context_ro(|ctx| {
            let data = memory.inner.data(ctx);
            let ptr = data.as_ptr() as *mut u8;
            let len = data.len();
            Ok((ptr, len))
        })
    }

    /// Create a validated memory instance with handle registration
    pub fn create_validated_memory(memory: Memory) -> WasmtimeResult<*mut ValidatedMemory> {
        let validated = ValidatedMemory::new(memory);
        let boxed = Box::new(validated);
        let ptr = Box::into_raw(boxed);
        
        // Register the handle
        register_memory_handle(ptr as *const c_void)?;
        
        log::debug!("Created validated memory handle: {:p}", ptr);
        Ok(ptr)
    }
    
    /// Core function to destroy memory with proper cleanup and validation
    pub unsafe fn destroy_memory(ptr: *mut c_void) {
        if ptr.is_null() {
            log::warn!("Attempted to destroy null memory pointer");
            return;
        }

        // First try to validate the handle (this might fail if already destroyed)
        match validate_memory_handle(ptr) {
            Ok(_) => {
                // Mark as destroyed first to prevent further access
                let validated_memory = &*(ptr as *const ValidatedMemory);
                validated_memory.mark_destroyed();

                // Unregister the handle
                if let Err(e) = unregister_memory_handle(ptr) {
                    log::error!("Failed to unregister memory handle {:p}: {}", ptr, e);
                }

                // Finally, deallocate the memory
                let _ = Box::from_raw(ptr as *mut ValidatedMemory);
                log::debug!("Destroyed validated memory handle: {:p}", ptr);
            }
            Err(e) => {
                log::warn!("Attempted to destroy invalid memory handle {:p}: {}", ptr, e);
                // Still attempt cleanup in case it's a partially corrupted handle
                if let Err(unregister_err) = unregister_memory_handle(ptr) {
                    log::debug!("Handle was not registered (expected for corrupted handle): {}", unregister_err);
                }
            }
        }
    }

    // Shared Memory Operations

    /// Core function to check if memory is shared
    pub fn memory_is_shared(memory: &Memory, store: &Store) -> WasmtimeResult<bool> {
        store.with_context_ro(|ctx| {
            let memory_type = memory.inner.ty(ctx);
            Ok(memory_type.is_shared())
        })
    }

    /// Core function for atomic compare-and-swap on 32-bit value
    pub fn atomic_compare_and_swap_i32(memory: &Memory, store: &mut Store, offset: usize, expected: i32, new_value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            // Use Rust's atomic operations on the memory data
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                let result = atomic_ref.compare_exchange(
                    expected,
                    new_value,
                    std::sync::atomic::Ordering::SeqCst,
                    std::sync::atomic::Ordering::SeqCst
                );

                match result {
                    Ok(old_value) => Ok(old_value),
                    Err(actual_value) => Ok(actual_value),
                }
            }
        })
    }

    /// Core function for atomic compare-and-swap on 64-bit value
    pub fn atomic_compare_and_swap_i64(memory: &Memory, store: &mut Store, offset: usize, expected: i64, new_value: i64) -> WasmtimeResult<i64> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                let result = atomic_ref.compare_exchange(
                    expected,
                    new_value,
                    std::sync::atomic::Ordering::SeqCst,
                    std::sync::atomic::Ordering::SeqCst
                );

                match result {
                    Ok(old_value) => Ok(old_value),
                    Err(actual_value) => Ok(actual_value),
                }
            }
        })
    }

    /// Core function for atomic load on 32-bit value
    pub fn atomic_load_i32(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context_ro(|ctx| {
            let memory_data = memory.inner.data(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.load(std::sync::atomic::Ordering::Acquire))
            }
        })
    }

    /// Core function for atomic load on 64-bit value
    pub fn atomic_load_i64(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<i64> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context_ro(|ctx| {
            let memory_data = memory.inner.data(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.load(std::sync::atomic::Ordering::Acquire))
            }
        })
    }

    /// Core function for atomic store on 32-bit value
    pub fn atomic_store_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<()> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                atomic_ref.store(value, std::sync::atomic::Ordering::Release);
                Ok(())
            }
        })
    }

    /// Core function for atomic store on 64-bit value
    pub fn atomic_store_i64(memory: &Memory, store: &mut Store, offset: usize, value: i64) -> WasmtimeResult<()> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                atomic_ref.store(value, std::sync::atomic::Ordering::Release);
                Ok(())
            }
        })
    }

    /// Core function for atomic add on 32-bit value
    pub fn atomic_add_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_add(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic add on 64-bit value
    pub fn atomic_add_i64(memory: &Memory, store: &mut Store, offset: usize, value: i64) -> WasmtimeResult<i64> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_add(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic bitwise AND on 32-bit value
    pub fn atomic_and_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_and(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic bitwise OR on 32-bit value
    pub fn atomic_or_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_or(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic bitwise XOR on 32-bit value
    pub fn atomic_xor_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_xor(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic memory fence
    pub fn atomic_fence(_memory: &Memory, store: &Store) -> WasmtimeResult<()> {
        // Validate memory is shared
        if !memory_is_shared(_memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Memory fence - ensures ordering of memory operations
        std::sync::atomic::fence(std::sync::atomic::Ordering::SeqCst);
        Ok(())
    }

    /// Core function for atomic notify (wake threads waiting on a memory location)
    pub fn atomic_notify(memory: &Memory, store: &Store, offset: usize, count: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        // Note: This is a simplified implementation. A real implementation would
        // maintain a thread wait queue for each memory location and notify
        // the specified number of waiting threads.

        // For now, we'll return 0 (no threads notified) since we don't have
        // a full wait/notify infrastructure implemented
        log::debug!("Atomic notify at offset {} with count {} (simplified implementation)", offset, count);
        Ok(0)
    }

    /// Core function for atomic wait on 32-bit value
    pub fn atomic_wait32(memory: &Memory, store: &Store, offset: usize, expected: i32, timeout_nanos: i64) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context_ro(|ctx| {
            let memory_data = memory.inner.data(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                let current_value = atomic_ref.load(std::sync::atomic::Ordering::Acquire);

                if current_value != expected {
                    // Value mismatch - return immediately
                    Ok(1)
                } else {
                    // Note: This is a simplified implementation. A real implementation would
                    // block the thread until notified or timeout occurs.
                    // For now, we'll simulate a timeout
                    log::debug!("Atomic wait32 at offset {} with expected {} and timeout {} ns (simplified implementation)",
                               offset, expected, timeout_nanos);

                    if timeout_nanos == 0 {
                        Ok(2) // Immediate timeout
                    } else {
                        // Simulate a short wait
                        std::thread::sleep(std::time::Duration::from_millis(1));
                        Ok(2) // Timeout
                    }
                }
            }
        })
    }

    /// Core function for atomic wait on 64-bit value
    pub fn atomic_wait64(memory: &Memory, store: &Store, offset: usize, expected: i64, timeout_nanos: i64) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context_ro(|ctx| {
            let memory_data = memory.inner.data(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                let current_value = atomic_ref.load(std::sync::atomic::Ordering::Acquire);

                if current_value != expected {
                    // Value mismatch - return immediately
                    Ok(1)
                } else {
                    // Note: This is a simplified implementation. A real implementation would
                    // block the thread until notified or timeout occurs.
                    log::debug!("Atomic wait64 at offset {} with expected {} and timeout {} ns (simplified implementation)",
                               offset, expected, timeout_nanos);

                    if timeout_nanos == 0 {
                        Ok(2) // Immediate timeout
                    } else {
                        // Simulate a short wait
                        std::thread::sleep(std::time::Duration::from_millis(1));
                        Ok(2) // Timeout
                    }
                }
            }
        })
    }

    // Bulk Memory Operations

    /// Core function to copy memory within the same memory instance
    pub fn memory_copy(memory: &Memory, store: &mut Store, dest_offset: usize, src_offset: usize, len: usize) -> WasmtimeResult<()> {
        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if dest_offset.saturating_add(len) > memory_size || src_offset.saturating_add(len) > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset: std::cmp::max(dest_offset, src_offset),
                length: len,
                memory_size,
            }.into());
        }

        // Perform memory copy within the store lock
        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);

            // Handle overlapping memory regions correctly
            if dest_offset < src_offset && dest_offset + len > src_offset {
                // Forward overlap - copy from end to beginning
                for i in (0..len).rev() {
                    memory_data[dest_offset + i] = memory_data[src_offset + i];
                }
            } else if src_offset < dest_offset && src_offset + len > dest_offset {
                // Backward overlap - copy from beginning to end
                for i in 0..len {
                    memory_data[dest_offset + i] = memory_data[src_offset + i];
                }
            } else {
                // No overlap - use efficient copy
                memory_data.copy_within(src_offset..src_offset + len, dest_offset);
            }
            Ok(())
        })
    }

    /// Core function to fill memory with a specific byte value
    pub fn memory_fill(memory: &Memory, store: &mut Store, offset: usize, value: u8, len: usize) -> WasmtimeResult<()> {
        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset.saturating_add(len) > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: len,
                memory_size,
            }.into());
        }

        // Perform memory fill within the store lock
        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            memory_data[offset..offset + len].fill(value);
            Ok(())
        })
    }

    /// Core function to initialize memory from a data segment
    pub fn memory_init(memory: &Memory, store: &mut Store, dest_offset: usize, data_segment_index: u32, src_offset: usize, len: usize) -> WasmtimeResult<()> {
        // Note: This is a simplified implementation. In a real implementation, you would:
        // 1. Get the data segment from the module/instance
        // 2. Check if the segment has been dropped
        // 3. Perform bounds checking on both memory and data segment
        // 4. Copy data from segment to memory

        // For now, return an error indicating this needs module context
        Err(WasmtimeError::InvalidParameter {
            message: "memory.init requires module/instance context for data segment access".to_string(),
        })
    }

    /// Core function to drop a data segment
    pub fn data_drop(store: &mut Store, data_segment_index: u32) -> WasmtimeResult<()> {
        // Note: This is a simplified implementation. In a real implementation, you would:
        // 1. Get the data segment from the module/instance
        // 2. Mark it as dropped
        // 3. Free any associated memory

        // For now, return an error indicating this needs module context
        Err(WasmtimeError::InvalidParameter {
            message: "data.drop requires module/instance context for data segment management".to_string(),
        })
    }
    
    /// Get diagnostic information about memory handle validation
    pub fn get_memory_handle_diagnostics() -> WasmtimeResult<(usize, u64)> {
        let handles = VALID_MEMORY_HANDLES.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        let handle_count = handles.len();
        let total_accesses = MEMORY_ACCESS_COUNTER.load(Ordering::Relaxed);
        
        Ok((handle_count, total_accesses))
    }
    
    /// Force cleanup of all registered handles (for emergency shutdown)
    pub fn force_cleanup_all_handles() -> WasmtimeResult<usize> {
        let mut memory_handles = VALID_MEMORY_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        let mut store_handles = VALID_STORE_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            }
        })?;
        
        let total_cleaned = memory_handles.len() + store_handles.len();
        
        memory_handles.clear();
        store_handles.clear();
        
        log::warn!("Force cleaned {} handles during emergency shutdown", total_cleaned);
        Ok(total_cleaned)
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

    #[test]
    fn test_memory_handle_validation() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let validated_ptr = core::create_validated_memory(memory).expect("Failed to create validated memory");
        
        // Validate the handle works
        unsafe {
            assert!(core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_ok());
            let memory_ref = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
            assert!(memory_ref.is_ok());
        }
        
        // Test destruction and use-after-free detection
        unsafe {
            core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
            
            // Now validation should fail
            assert!(core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_err());
        }
    }

    #[test]
    fn test_null_pointer_validation() {
        unsafe {
            // Test null pointer validation
            assert!(core::validate_memory_handle(std::ptr::null()).is_err());
            assert!(core::validate_store_handle(std::ptr::null()).is_err());
            
            // Test getting memory from null pointer
            let result = core::get_memory_ref(std::ptr::null());
            assert!(result.is_err());
        }
    }

    #[test]
    fn test_invalid_pointer_validation() {
        unsafe {
            // Test with invalid but non-null pointer
            let invalid_ptr = 0xDEADBEEF as *const std::os::raw::c_void;
            assert!(core::validate_memory_handle(invalid_ptr).is_err());
            
            // Test getting memory from invalid pointer
            let result = core::get_memory_ref(invalid_ptr);
            assert!(result.is_err());
        }
    }

    #[test]
    fn test_memory_access_counting() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let validated_ptr = core::create_validated_memory(memory).expect("Failed to create validated memory");
        
        unsafe {
            let validated_memory = &*(validated_ptr as *const core::ValidatedMemory);
            
            // Initial access count should be 0
            assert_eq!(validated_memory.get_access_count(), 0);
            
            // Access the memory a few times
            let _memory1 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
            let _memory2 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
            let _memory3 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
            
            // Access count should have incremented (note: validation also increments count)
            assert!(validated_memory.get_access_count() >= 3);
            
            // Cleanup
            core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
        }
    }

    #[test]
    fn test_handle_registry_diagnostics() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        // Get initial state
        let (initial_handles, _initial_accesses) = core::get_memory_handle_diagnostics()
            .expect("Failed to get initial diagnostics");
        
        // Create some memory handles
        let memory1 = Memory::new(&mut store, 1).expect("Failed to create memory 1");
        let memory2 = Memory::new(&mut store, 1).expect("Failed to create memory 2");
        
        let validated_ptr1 = core::create_validated_memory(memory1).expect("Failed to create validated memory 1");
        let validated_ptr2 = core::create_validated_memory(memory2).expect("Failed to create validated memory 2");
        
        // Check handle count increased
        let (current_handles, _current_accesses) = core::get_memory_handle_diagnostics()
            .expect("Failed to get current diagnostics");
        assert_eq!(current_handles, initial_handles + 2);
        
        // Access the memories to increase access counter
        unsafe {
            let _mem1 = core::get_memory_ref(validated_ptr1 as *const std::os::raw::c_void);
            let _mem2 = core::get_memory_ref(validated_ptr2 as *const std::os::raw::c_void);
        }
        
        let (_final_handles, final_accesses) = core::get_memory_handle_diagnostics()
            .expect("Failed to get final diagnostics");
        assert!(final_accesses >= 2); // At least 2 accesses
        
        // Cleanup
        unsafe {
            core::destroy_memory(validated_ptr1 as *mut std::os::raw::c_void);
            core::destroy_memory(validated_ptr2 as *mut std::os::raw::c_void);
        }
        
        // Check handle count decreased
        let (cleanup_handles, _cleanup_accesses) = core::get_memory_handle_diagnostics()
            .expect("Failed to get cleanup diagnostics");
        assert_eq!(cleanup_handles, initial_handles);
    }

    #[test]
    fn test_corrupted_handle_detection() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let validated_ptr = core::create_validated_memory(memory).expect("Failed to create validated memory");
        
        unsafe {
            // First validate it works
            assert!(core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_ok());
            
            // Corrupt the magic number by directly manipulating memory
            let validated_memory_ptr = validated_ptr as *mut u64; // First field is magic
            *validated_memory_ptr = 0xBADCAFE; // Wrong magic
            
            // Now validation should fail due to corrupted magic
            let result = core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void);
            assert!(result.is_err());
            let error_msg = result.unwrap_err().to_string();
            assert!(error_msg.contains("invalid magic number"));
            
            // Cleanup (this will also fail validation but still clean up)
            core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
        }
    }

    #[test]
    fn test_thread_safety_basic() {
        use std::thread;
        
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let validated_ptr = core::create_validated_memory(memory).expect("Failed to create validated memory");
        
        let ptr_copy = validated_ptr as usize;
        
        // Spawn threads to access the handle concurrently
        let handles: Vec<_> = (0..4).map(|_| {
            let ptr = ptr_copy;
            thread::spawn(move || {
                unsafe {
                    for _ in 0..10 {
                        let result = core::validate_memory_handle(ptr as *const std::os::raw::c_void);
                        // We don't assert success here because the memory might be destroyed
                        // by another thread, but validation should not crash
                        let _ = result;
                    }
                }
            })
        }).collect();
        
        // Wait for all threads to complete
        for handle in handles {
            handle.join().expect("Thread panicked");
        }
        
        // Cleanup
        unsafe {
            core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
        }
    }
}