//! Platform-specific memory management with huge pages, NUMA awareness, and WebAssembly linear memory
//!
//! This module provides comprehensive memory management optimizations for wasmtime4j including:
//! - WebAssembly linear memory management with comprehensive bounds checking
//! - Huge pages support for Linux, macOS, and Windows
//! - NUMA-aware memory allocation and thread binding
//! - Custom memory allocators with platform-specific optimization
//! - Memory prefetching and cache optimization strategies
//! - Memory compression and deduplication for WebAssembly heaps
//! - Comprehensive memory usage monitoring and leak detection
//! - Memory pool management with size-based allocation strategies
//! - Platform-specific virtual memory management and protection

mod ffi;
// platform module removed (Phase 12: dead PlatformMemory infrastructure)
mod types;

// Re-export all public types for backward compatibility
pub use types::{
    MemoryBuilder, MemoryConfig, MemoryDataType, MemoryError, MemoryMetadata, MemoryResult,
    MemoryUsage, MemoryVariant,
};

// Re-export FFI functions
pub use ffi::*;

use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::time::Instant;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::store::Store;
use wasmtime::{Memory as WasmtimeMemory, MemoryType, SharedMemory as WasmtimeSharedMemory};

/// Thread-safe WebAssembly memory wrapper with comprehensive bounds checking
///
/// This struct supports both regular and shared WebAssembly memory, providing
/// a unified interface for memory operations. The internal variant determines
/// how operations are performed (with or without store context).
#[derive(Debug)]
pub struct Memory {
    /// Wasmtime memory instance (can be regular or shared)
    pub(crate) inner: MemoryVariant,
    /// Memory metadata and statistics
    metadata: Arc<RwLock<MemoryMetadata>>,
    /// Memory configuration and limits
    config: MemoryConfig,
    /// Cached memory type (stored on creation to avoid needing Store for type queries)
    pub memory_type: MemoryType,
}

impl Memory {
    /// Create a new memory with default configuration
    pub fn new(store: &mut Store, initial_pages: u64) -> WasmtimeResult<Self> {
        let config = MemoryConfig {
            initial_pages,
            maximum_pages: None,
            is_shared: false,
            is_64: false,
            memory_index: 0,
            name: None,
        };
        Self::new_with_config(store, config)
    }

    /// Create a new memory with specific configuration
    pub fn new_with_config(store: &mut Store, config: MemoryConfig) -> WasmtimeResult<Self> {
        // Validate configuration parameters
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

        // Check WebAssembly memory limits
        // For 32-bit memory: max 65536 pages (4GB)
        // For 64-bit memory: defer to wasmtime's own limits
        if !config.is_64 {
            const MAX_WASM_PAGES_32: u64 = 65536; // 4GB / 64KB
            if config.initial_pages > MAX_WASM_PAGES_32 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Initial pages ({}) exceeds WebAssembly 32-bit limit ({})",
                        config.initial_pages, MAX_WASM_PAGES_32
                    ),
                });
            }

            if let Some(max_pages) = config.maximum_pages {
                if max_pages > MAX_WASM_PAGES_32 {
                    return Err(WasmtimeError::InvalidParameter {
                        message: format!(
                            "Maximum pages ({}) exceeds WebAssembly 32-bit limit ({})",
                            max_pages, MAX_WASM_PAGES_32
                        ),
                    });
                }
            }
        }

        // Create Wasmtime memory type based on addressing mode
        let memory_type = if config.is_64 {
            MemoryType::new64(config.initial_pages, config.maximum_pages)
        } else {
            MemoryType::new(
                config.initial_pages as u32,
                config.maximum_pages.map(|p| p as u32),
            )
        };

        // Create memory instance
        let inner = store.with_context(|ctx| {
            WasmtimeMemory::new(ctx, memory_type.clone()).map_err(|e| WasmtimeError::Memory {
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
            inner: MemoryVariant::Regular(inner),
            metadata: Arc::new(RwLock::new(metadata)),
            config,
            memory_type,
        })
    }

    /// Create a new memory with specific configuration using async resource limiter
    ///
    /// This is the async variant of [`new_with_config`]. It uses Wasmtime's
    /// `Memory::new_async` which goes through the async resource limiter.
    /// Required when the engine has `async_support(true)` and an async resource
    /// limiter is configured.
    #[cfg(feature = "async")]
    pub fn new_with_config_async(
        store: &crate::store::Store,
        config: MemoryConfig,
    ) -> WasmtimeResult<Self> {
        // Validate configuration parameters (same as sync version)
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

        if !config.is_64 {
            const MAX_WASM_PAGES_32: u64 = 65536;
            if config.initial_pages > MAX_WASM_PAGES_32 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Initial pages ({}) exceeds WebAssembly 32-bit limit ({})",
                        config.initial_pages, MAX_WASM_PAGES_32
                    ),
                });
            }
            if let Some(max_pages) = config.maximum_pages {
                if max_pages > MAX_WASM_PAGES_32 {
                    return Err(WasmtimeError::InvalidParameter {
                        message: format!(
                            "Maximum pages ({}) exceeds WebAssembly 32-bit limit ({})",
                            max_pages, MAX_WASM_PAGES_32
                        ),
                    });
                }
            }
        }

        if config.is_shared {
            return Err(WasmtimeError::InvalidParameter {
                message: "Async creation not supported for shared memory".to_string(),
            });
        }

        let memory_type = if config.is_64 {
            MemoryType::new64(config.initial_pages, config.maximum_pages)
        } else {
            MemoryType::new(
                config.initial_pages as u32,
                config.maximum_pages.map(|p| p as u32),
            )
        };

        let handle = crate::async_runtime::get_runtime_handle();
        let mut store_guard = store.try_lock_store()?;
        let inner = handle
            .block_on(async {
                WasmtimeMemory::new_async(&mut *store_guard, memory_type.clone()).await
            })
            .map_err(|e| WasmtimeError::Memory {
                message: format!("Async memory creation failed: {}", e),
            })?;

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
            inner: MemoryVariant::Regular(inner),
            metadata: Arc::new(RwLock::new(metadata)),
            config,
            memory_type,
        })
    }

    /// Get reference to inner Wasmtime memory (internal use)
    /// Returns None if this is a shared memory variant.
    pub(crate) fn inner(&self) -> Option<&WasmtimeMemory> {
        match &self.inner {
            MemoryVariant::Regular(mem) => Some(mem),
            MemoryVariant::Shared(_) => None,
        }
    }

    /// Get reference to inner Wasmtime shared memory (internal use)
    /// Returns None if this is a regular memory variant.
    pub(crate) fn inner_shared(&self) -> Option<&WasmtimeSharedMemory> {
        match &self.inner {
            MemoryVariant::Regular(_) => None,
            MemoryVariant::Shared(mem) => Some(mem),
        }
    }

    /// Get current memory size in pages
    pub fn size_pages(&self, store: &Store) -> WasmtimeResult<u64> {
        match &self.inner {
            MemoryVariant::Regular(mem) => store.with_context_ro(|ctx| Ok(mem.size(ctx))),
            MemoryVariant::Shared(mem) => Ok(mem.size()),
        }
    }

    /// Get current memory size in bytes
    pub fn size_bytes(&self, store: &Store) -> WasmtimeResult<usize> {
        Ok((self.size_pages(store)? * 65536) as usize)
    }

    /// Grow memory by the specified number of pages with validation.
    ///
    /// Returns the previous number of pages on success, or `u64::MAX` (which
    /// represents -1 as i64) on failure, consistent with the WebAssembly spec
    /// for `memory.grow`.
    pub fn grow(&self, store: &mut Store, additional_pages: u64) -> WasmtimeResult<u64> {
        // Get current size
        let current_pages = self.size_pages(store)?;
        let requested_pages = current_pages + additional_pages;

        // Validate growth against limits — return u64::MAX (-1) per WebAssembly spec
        if let Some(max_pages) = self.config.maximum_pages {
            if requested_pages > max_pages {
                log::debug!(
                    "Memory growth rejected: requested {} pages exceeds maximum {} pages",
                    requested_pages,
                    max_pages
                );
                return Ok(u64::MAX);
            }
        }

        // Check WebAssembly 32-bit memory limits (64-bit defers to wasmtime)
        if !self.config.is_64 {
            const MAX_WASM_PAGES_32: u64 = 65536;
            if requested_pages > MAX_WASM_PAGES_32 {
                log::debug!(
                    "Memory growth rejected: requested {} pages exceeds 32-bit limit of {} pages",
                    requested_pages,
                    MAX_WASM_PAGES_32
                );
                return Ok(u64::MAX);
            }
        }

        // Perform the growth operation based on memory variant
        let previous_pages = match &self.inner {
            MemoryVariant::Regular(mem) => match store.with_context(|ctx| {
                mem.grow(ctx, additional_pages)
                    .map_err(|e| WasmtimeError::Memory {
                        message: format!("Memory growth failed: {}", e),
                    })
            }) {
                Ok(pages) => pages,
                Err(e) => {
                    log::debug!("Memory growth failed: {}", e);
                    return Ok(u64::MAX);
                }
            },
            MemoryVariant::Shared(mem) => {
                log::debug!(
                    "Growing shared memory from {} to {} pages (adding {})",
                    current_pages,
                    requested_pages,
                    additional_pages
                );

                match mem.grow(additional_pages) {
                    Ok(result) => {
                        // Memory barrier to ensure growth is visible to all threads
                        std::sync::atomic::fence(std::sync::atomic::Ordering::SeqCst);
                        log::info!(
                            "Shared memory successfully grown to {} pages",
                            requested_pages
                        );
                        result
                    }
                    Err(e) => {
                        log::debug!("Shared memory growth failed: {}", e);
                        return Ok(u64::MAX);
                    }
                }
            }
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

    /// Read data with comprehensive bounds checking
    pub fn read_bytes(
        &self,
        store: &Store,
        offset: usize,
        length: usize,
    ) -> WasmtimeResult<Vec<u8>> {
        // Bounds checking
        let memory_size = self.size_bytes(store)?;
        if offset.saturating_add(length) > memory_size {
            let mut metadata = self
                .metadata
                .write()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire metadata lock".to_string(),
                })?;
            metadata.bounds_violations_prevented += 1;

            return Err(MemoryError::BoundsViolation {
                offset,
                length,
                memory_size,
            }
            .into());
        }

        // Read memory data based on variant
        let result = match &self.inner {
            MemoryVariant::Regular(mem) => store.with_context_ro(|ctx| {
                let data = mem.data(ctx);
                Ok(data[offset..offset + length].to_vec())
            })?,
            MemoryVariant::Shared(mem) => {
                let data = mem.data();
                let mut result = vec![0u8; length];
                for (i, cell) in data[offset..offset + length].iter().enumerate() {
                    result[i] = unsafe { *cell.get() };
                }
                result
            }
        };

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
            let mut metadata = self
                .metadata
                .write()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire metadata lock".to_string(),
                })?;
            metadata.bounds_violations_prevented += 1;

            return Err(MemoryError::BoundsViolation {
                offset,
                length,
                memory_size,
            }
            .into());
        }

        // Write memory data based on variant
        match &self.inner {
            MemoryVariant::Regular(mem) => {
                store.with_context(|ctx| {
                    let memory_data = mem.data_mut(ctx);
                    memory_data[offset..offset + length].copy_from_slice(data);
                    Ok(())
                })?;
            }
            MemoryVariant::Shared(mem) => {
                let mem_data = mem.data();
                for (i, byte) in data.iter().enumerate() {
                    unsafe { *mem_data[offset + i].get() = *byte };
                }
            }
        }

        // Update statistics
        if let Ok(mut metadata) = self.metadata.write() {
            metadata.write_operations += 1;
            metadata.bytes_written += length as u64;
            metadata.last_access = Some(Instant::now());
        }

        Ok(())
    }

    /// Read typed value with alignment checking
    pub fn read_typed<T>(
        &self,
        store: &Store,
        offset: usize,
        data_type: MemoryDataType,
    ) -> WasmtimeResult<T>
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
            }
            .into());
        }

        let type_size = Self::get_type_size(data_type);
        let bytes = self.read_bytes(store, offset, type_size)?;

        // Convert bytes to typed value
        Self::bytes_to_typed_value(&bytes, data_type)
    }

    /// Write typed value with alignment checking
    pub fn write_typed<T>(
        &self,
        store: &mut Store,
        offset: usize,
        value: T,
        data_type: MemoryDataType,
    ) -> WasmtimeResult<()>
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
            }
            .into());
        }

        let bytes = Self::typed_value_to_bytes(value, data_type)?;
        self.write_bytes(store, offset, &bytes)
    }

    /// Get memory usage statistics
    pub fn get_usage(&self, store: &Store) -> WasmtimeResult<MemoryUsage> {
        let current_pages = self.size_pages(store)?;
        let current_bytes = (current_pages * 65536) as usize;
        let maximum_bytes = self.config.maximum_pages.map(|p| (p * 65536) as usize);

        let metadata = self
            .metadata
            .read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire metadata lock".to_string(),
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
        let metadata = self
            .metadata
            .read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire metadata lock".to_string(),
            })?;
        Ok(metadata.clone())
    }

    /// Get memory configuration
    pub fn get_config(&self) -> &MemoryConfig {
        &self.config
    }

    /// Get Wasmtime memory handle for advanced operations
    /// Returns None if this is a shared memory variant.
    pub fn as_wasmtime_memory(&self) -> Option<&WasmtimeMemory> {
        match &self.inner {
            MemoryVariant::Regular(mem) => Some(mem),
            MemoryVariant::Shared(_) => None,
        }
    }

    /// Get memory type information from the underlying Wasmtime memory
    pub fn get_type(&self, store: &Store) -> WasmtimeResult<MemoryType> {
        match &self.inner {
            MemoryVariant::Regular(mem) => store.with_context_ro(|ctx| Ok(mem.ty(ctx))),
            MemoryVariant::Shared(mem) => Ok(mem.ty()),
        }
    }

    /// Get the memory variant for internal operations
    pub(crate) fn variant(&self) -> &MemoryVariant {
        &self.inner
    }

    /// Get memory data size
    pub fn data_size(&self, store: &Store) -> WasmtimeResult<usize> {
        match &self.inner {
            MemoryVariant::Regular(mem) => store.with_context_ro(|ctx| Ok(mem.data_size(&ctx))),
            MemoryVariant::Shared(mem) => Ok(mem.data().len()),
        }
    }

    /// Create Memory wrapper from existing Wasmtime memory (for memory exports)
    /// Note: This creates a wrapper with default metadata since we don't have store context
    pub fn from_wasmtime_memory(wasmtime_memory: WasmtimeMemory, memory_type: MemoryType) -> Self {
        // Extract configuration from memory type
        let initial_pages = memory_type.minimum();
        let maximum_pages = memory_type.maximum();
        let is_shared = memory_type.is_shared();

        // Create configuration based on memory type
        let config = MemoryConfig {
            initial_pages,
            maximum_pages,
            is_shared,
            is_64: memory_type.is_64(),
            memory_index: 0,
            name: Some("exported_memory".to_string()),
        };

        // Initialize metadata for exported memory
        let metadata = MemoryMetadata {
            created_at: Instant::now(),
            current_pages: initial_pages,
            maximum_pages,
            read_operations: 0,
            write_operations: 0,
            bytes_read: 0,
            bytes_written: 0,
            growth_operations: 0,
            peak_pages: initial_pages,
            last_access: None,
            bounds_violations_prevented: 0,
        };

        Self {
            inner: MemoryVariant::Regular(wasmtime_memory),
            metadata: Arc::new(RwLock::new(metadata)),
            config,
            memory_type,
        }
    }

    /// Create Memory wrapper from existing Wasmtime shared memory (for shared memory exports)
    pub fn from_shared_memory(shared_memory: WasmtimeSharedMemory) -> Self {
        let memory_type = shared_memory.ty();
        let initial_pages = memory_type.minimum();
        let maximum_pages = memory_type.maximum();

        let config = MemoryConfig {
            initial_pages,
            maximum_pages,
            is_shared: true,
            is_64: memory_type.is_64(),
            memory_index: 0,
            name: Some("exported_shared_memory".to_string()),
        };

        let metadata = MemoryMetadata {
            created_at: Instant::now(),
            current_pages: initial_pages,
            maximum_pages,
            read_operations: 0,
            write_operations: 0,
            bytes_read: 0,
            bytes_written: 0,
            growth_operations: 0,
            peak_pages: initial_pages,
            last_access: None,
            bounds_violations_prevented: 0,
        };

        Self {
            inner: MemoryVariant::Shared(shared_memory),
            metadata: Arc::new(RwLock::new(metadata)),
            config,
            memory_type,
        }
    }

    /// Check if this memory is a shared memory
    pub fn is_shared_memory(&self) -> bool {
        matches!(self.inner, MemoryVariant::Shared(_))
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

        unsafe {
            match data_type {
                MemoryDataType::U8 => {
                    let value = bytes[0];
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
                        bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6],
                        bytes[7],
                    ]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I64Le => {
                    let value = i64::from_le_bytes([
                        bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6],
                        bytes[7],
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
                        bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6],
                        bytes[7],
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
        let mut memories = self
            .memories
            .lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            })?;

        let memory_id = memory.config.memory_index;
        memories.insert(memory_id, Arc::new(memory));

        log::debug!("Registered memory with ID {}", memory_id);
        Ok(memory_id)
    }

    /// Get memory instance by ID
    pub fn get(&self, memory_id: u32) -> WasmtimeResult<Arc<Memory>> {
        let memories = self
            .memories
            .lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            })?;

        memories
            .get(&memory_id)
            .cloned()
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Memory with ID {} not found", memory_id),
            })
    }

    /// Remove memory instance
    pub fn unregister(&self, memory_id: u32) -> WasmtimeResult<()> {
        let mut memories = self
            .memories
            .lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            })?;

        memories
            .remove(&memory_id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Memory with ID {} not found", memory_id),
            })?;

        log::debug!("Unregistered memory with ID {}", memory_id);
        Ok(())
    }

    /// Get all registered memory IDs
    pub fn list_memories(&self) -> WasmtimeResult<Vec<u32>> {
        let memories = self
            .memories
            .lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            })?;

        Ok(memories.keys().cloned().collect())
    }
}

impl Default for MemoryRegistry {
    fn default() -> Self {
        Self::new()
    }
}

// Include the core submodule
pub mod core;

#[cfg(test)]
mod tests;
