//! Engine builder for creating configured engines
//!
//! This module provides the EngineBuilder and EngineConfigSummary types
//! for creating and configuring Wasmtime engines with various options.

use std::borrow::Cow;
use std::sync::atomic::AtomicBool;
use std::sync::{Arc, RwLock};

use wasmtime::{Config, Engine as WasmtimeEngine, OptLevel, RegallocAlgorithm, Strategy};

use super::{safe_wasmtime_config, Engine};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Type alias for the C function pointer that implements CacheStore.get()
///
/// Parameters: callback_id, key_ptr, key_len, out_data_ptr, out_data_len
/// Returns: 0 = cache miss (out_data_ptr not set), 1 = cache hit (out_data_ptr/len set)
///
/// When returning 1 (cache hit), the callback must allocate the output data using
/// `malloc` or equivalent, and set *out_data_ptr and *out_data_len. The caller
/// will free this memory after use.
pub type CacheGetFn = unsafe extern "C" fn(
    callback_id: i64,
    key_ptr: *const u8,
    key_len: usize,
    out_data_ptr: *mut *mut u8,
    out_data_len: *mut usize,
) -> i32;

/// Type alias for the C function pointer that implements CacheStore.insert()
///
/// Parameters: callback_id, key_ptr, key_len, value_ptr, value_len
/// Returns: 1 = success, 0 = failure
pub type CacheInsertFn = unsafe extern "C" fn(
    callback_id: i64,
    key_ptr: *const u8,
    key_len: usize,
    value_ptr: *const u8,
    value_len: usize,
) -> i32;

/// Type alias for the function that frees memory allocated by the cache get callback
pub type CacheFreeFn = unsafe extern "C" fn(ptr: *mut u8, len: usize);

/// A CacheStore implementation that delegates to C function pointers (JNI or Panama callbacks)
pub struct CallbackCacheStore {
    callback_id: i64,
    get_fn: CacheGetFn,
    insert_fn: CacheInsertFn,
    free_fn: CacheFreeFn,
}

// SAFETY: The callback functions are expected to be thread-safe (Java CacheStore is thread-safe)
unsafe impl Send for CallbackCacheStore {}
unsafe impl Sync for CallbackCacheStore {}

impl std::fmt::Debug for CallbackCacheStore {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("CallbackCacheStore")
            .field("callback_id", &self.callback_id)
            .finish()
    }
}

impl CallbackCacheStore {
    /// Create a new callback-based cache store
    pub fn new(
        callback_id: i64,
        get_fn: CacheGetFn,
        insert_fn: CacheInsertFn,
        free_fn: CacheFreeFn,
    ) -> Self {
        Self {
            callback_id,
            get_fn,
            insert_fn,
            free_fn,
        }
    }
}

impl wasmtime::CacheStore for CallbackCacheStore {
    fn get(&self, key: &[u8]) -> Option<Cow<'_, [u8]>> {
        let mut out_ptr: *mut u8 = std::ptr::null_mut();
        let mut out_len: usize = 0;

        let result = unsafe {
            (self.get_fn)(
                self.callback_id,
                key.as_ptr(),
                key.len(),
                &mut out_ptr,
                &mut out_len,
            )
        };

        if result == 1 && !out_ptr.is_null() && out_len > 0 {
            // Copy the data into an owned Vec and free the callback-allocated memory
            let data = unsafe { std::slice::from_raw_parts(out_ptr, out_len) }.to_vec();
            unsafe { (self.free_fn)(out_ptr, out_len) };
            Some(Cow::Owned(data))
        } else {
            None
        }
    }

    fn insert(&self, key: &[u8], value: Vec<u8>) -> bool {
        let result = unsafe {
            (self.insert_fn)(
                self.callback_id,
                key.as_ptr(),
                key.len(),
                value.as_ptr(),
                value.len(),
            )
        };
        result == 1
    }
}

// ==================== MemoryCreator Extension Trait ====================

/// C function pointer types for MemoryCreator callbacks (Panama FFI path)

/// new_memory callback: creates a new linear memory.
/// Returns: callback_id for the new LinearMemory, or 0 on failure.
/// Parameters: creator_id, min_bytes, max_bytes (-1 = none), reserved_bytes (-1 = none), guard_bytes
pub type MemCreatorNewMemoryFn = unsafe extern "C" fn(
    creator_id: i64,
    min_bytes: u64,
    max_bytes: i64,
    reserved_bytes: i64,
    guard_bytes: u64,
) -> i64;

/// LinearMemory.byte_size callback
pub type LinMemByteSizeFn = unsafe extern "C" fn(memory_id: i64) -> u64;

/// LinearMemory.byte_capacity callback
pub type LinMemByteCapacityFn = unsafe extern "C" fn(memory_id: i64) -> u64;

/// LinearMemory.grow_to callback. Returns 0 on success, -1 on failure.
pub type LinMemGrowToFn = unsafe extern "C" fn(memory_id: i64, new_size: u64) -> i32;

/// LinearMemory.as_ptr callback
pub type LinMemAsPtrFn = unsafe extern "C" fn(memory_id: i64) -> *mut u8;

/// LinearMemory.drop callback (called when Wasmtime is done with the memory)
pub type LinMemDropFn = unsafe extern "C" fn(memory_id: i64);

/// A MemoryCreator that delegates to C function pointers (Panama callbacks)
pub struct CallbackMemoryCreator {
    creator_id: i64,
    new_memory_fn: MemCreatorNewMemoryFn,
    byte_size_fn: LinMemByteSizeFn,
    byte_capacity_fn: LinMemByteCapacityFn,
    grow_to_fn: LinMemGrowToFn,
    as_ptr_fn: LinMemAsPtrFn,
    drop_fn: LinMemDropFn,
}

unsafe impl Send for CallbackMemoryCreator {}
unsafe impl Sync for CallbackMemoryCreator {}

impl CallbackMemoryCreator {
    /// Create a new callback-based memory creator
    pub fn new(
        creator_id: i64,
        new_memory_fn: MemCreatorNewMemoryFn,
        byte_size_fn: LinMemByteSizeFn,
        byte_capacity_fn: LinMemByteCapacityFn,
        grow_to_fn: LinMemGrowToFn,
        as_ptr_fn: LinMemAsPtrFn,
        drop_fn: LinMemDropFn,
    ) -> Self {
        Self {
            creator_id,
            new_memory_fn,
            byte_size_fn,
            byte_capacity_fn,
            grow_to_fn,
            as_ptr_fn,
            drop_fn,
        }
    }
}

/// A LinearMemory backed by callback function pointers
struct CallbackLinearMemory {
    memory_id: i64,
    byte_size_fn: LinMemByteSizeFn,
    byte_capacity_fn: LinMemByteCapacityFn,
    grow_to_fn: LinMemGrowToFn,
    as_ptr_fn: LinMemAsPtrFn,
    drop_fn: LinMemDropFn,
}

unsafe impl Send for CallbackLinearMemory {}
unsafe impl Sync for CallbackLinearMemory {}

unsafe impl wasmtime::LinearMemory for CallbackLinearMemory {
    fn byte_size(&self) -> usize {
        unsafe { (self.byte_size_fn)(self.memory_id) as usize }
    }

    fn byte_capacity(&self) -> usize {
        unsafe { (self.byte_capacity_fn)(self.memory_id) as usize }
    }

    fn grow_to(&mut self, new_size: usize) -> wasmtime::Result<()> {
        let result = unsafe { (self.grow_to_fn)(self.memory_id, new_size as u64) };
        if result == 0 {
            Ok(())
        } else {
            Err(wasmtime::Error::msg("LinearMemory.growTo failed"))
        }
    }

    fn as_ptr(&self) -> *mut u8 {
        unsafe { (self.as_ptr_fn)(self.memory_id) }
    }
}

impl Drop for CallbackLinearMemory {
    fn drop(&mut self) {
        unsafe { (self.drop_fn)(self.memory_id) };
    }
}

unsafe impl wasmtime::MemoryCreator for CallbackMemoryCreator {
    fn new_memory(
        &self,
        _ty: wasmtime::MemoryType,
        minimum: usize,
        maximum: Option<usize>,
        reserved_size_in_bytes: Option<usize>,
        guard_size_in_bytes: usize,
    ) -> Result<Box<dyn wasmtime::LinearMemory>, String> {
        let max_val = maximum.map(|m| m as i64).unwrap_or(-1);
        let reserved_val = reserved_size_in_bytes.map(|r| r as i64).unwrap_or(-1);

        let memory_id = unsafe {
            (self.new_memory_fn)(
                self.creator_id,
                minimum as u64,
                max_val,
                reserved_val,
                guard_size_in_bytes as u64,
            )
        };

        if memory_id == 0 {
            return Err("MemoryCreator.newMemory returned null".to_string());
        }

        Ok(Box::new(CallbackLinearMemory {
            memory_id,
            byte_size_fn: self.byte_size_fn,
            byte_capacity_fn: self.byte_capacity_fn,
            grow_to_fn: self.grow_to_fn,
            as_ptr_fn: self.as_ptr_fn,
            drop_fn: self.drop_fn,
        }))
    }
}

// ==================== StackCreator Extension Trait ====================

/// StackCreator.new_stack callback.
/// Returns: callback_id for the new StackMemory, or 0 on failure.
pub type StkCreatorNewStackFn =
    unsafe extern "C" fn(creator_id: i64, size: u64, zeroed: i32) -> i64;

/// StackMemory.top callback
pub type StkMemTopFn = unsafe extern "C" fn(stack_id: i64) -> *mut u8;

/// StackMemory.range callback: writes range_start and range_end
pub type StkMemRangeFn =
    unsafe extern "C" fn(stack_id: i64, out_start: *mut u64, out_end: *mut u64);

/// StackMemory.guard_range callback: writes guard_start and guard_end pointers
pub type StkMemGuardRangeFn =
    unsafe extern "C" fn(stack_id: i64, out_start: *mut *mut u8, out_end: *mut *mut u8);

/// StackMemory.drop callback
pub type StkMemDropFn = unsafe extern "C" fn(stack_id: i64);

/// A StackCreator that delegates to C function pointers
pub struct CallbackStackCreator {
    creator_id: i64,
    new_stack_fn: StkCreatorNewStackFn,
    top_fn: StkMemTopFn,
    range_fn: StkMemRangeFn,
    guard_range_fn: StkMemGuardRangeFn,
    drop_fn: StkMemDropFn,
}

unsafe impl Send for CallbackStackCreator {}
unsafe impl Sync for CallbackStackCreator {}

impl CallbackStackCreator {
    /// Create a new callback-based stack creator
    pub fn new(
        creator_id: i64,
        new_stack_fn: StkCreatorNewStackFn,
        top_fn: StkMemTopFn,
        range_fn: StkMemRangeFn,
        guard_range_fn: StkMemGuardRangeFn,
        drop_fn: StkMemDropFn,
    ) -> Self {
        Self {
            creator_id,
            new_stack_fn,
            top_fn,
            range_fn,
            guard_range_fn,
            drop_fn,
        }
    }
}

/// A StackMemory backed by callback function pointers
struct CallbackStackMemory {
    stack_id: i64,
    top_fn: StkMemTopFn,
    range_fn: StkMemRangeFn,
    guard_range_fn: StkMemGuardRangeFn,
    drop_fn: StkMemDropFn,
}

unsafe impl Send for CallbackStackMemory {}
unsafe impl Sync for CallbackStackMemory {}

unsafe impl wasmtime::StackMemory for CallbackStackMemory {
    fn top(&self) -> *mut u8 {
        unsafe { (self.top_fn)(self.stack_id) }
    }

    fn range(&self) -> std::ops::Range<usize> {
        let mut start: u64 = 0;
        let mut end: u64 = 0;
        unsafe { (self.range_fn)(self.stack_id, &mut start, &mut end) };
        (start as usize)..(end as usize)
    }

    fn guard_range(&self) -> std::ops::Range<*mut u8> {
        let mut start: *mut u8 = std::ptr::null_mut();
        let mut end: *mut u8 = std::ptr::null_mut();
        unsafe { (self.guard_range_fn)(self.stack_id, &mut start, &mut end) };
        start..end
    }
}

impl Drop for CallbackStackMemory {
    fn drop(&mut self) {
        unsafe { (self.drop_fn)(self.stack_id) };
    }
}

unsafe impl wasmtime::StackCreator for CallbackStackCreator {
    fn new_stack(
        &self,
        size: usize,
        zeroed: bool,
    ) -> Result<Box<dyn wasmtime::StackMemory>, wasmtime::Error> {
        let stack_id = unsafe {
            (self.new_stack_fn)(self.creator_id, size as u64, if zeroed { 1 } else { 0 })
        };

        if stack_id == 0 {
            return Err(wasmtime::Error::msg("StackCreator.newStack returned null"));
        }

        Ok(Box::new(CallbackStackMemory {
            stack_id,
            top_fn: self.top_fn,
            range_fn: self.range_fn,
            guard_range_fn: self.guard_range_fn,
            drop_fn: self.drop_fn,
        }))
    }
}

// ==================== CustomCodeMemory Extension Trait ====================

/// CustomCodeMemory.required_alignment callback
pub type CodeMemAlignmentFn = unsafe extern "C" fn(code_mem_id: i64) -> u64;

/// CustomCodeMemory.publish_executable callback. Returns 0 on success, -1 on failure.
pub type CodeMemPublishFn = unsafe extern "C" fn(code_mem_id: i64, ptr: *const u8, len: u64) -> i32;

/// CustomCodeMemory.unpublish_executable callback. Returns 0 on success, -1 on failure.
pub type CodeMemUnpublishFn =
    unsafe extern "C" fn(code_mem_id: i64, ptr: *const u8, len: u64) -> i32;

/// A CustomCodeMemory that delegates to C function pointers
pub struct CallbackCustomCodeMemory {
    code_mem_id: i64,
    alignment_fn: CodeMemAlignmentFn,
    publish_fn: CodeMemPublishFn,
    unpublish_fn: CodeMemUnpublishFn,
}

unsafe impl Send for CallbackCustomCodeMemory {}
unsafe impl Sync for CallbackCustomCodeMemory {}

impl CallbackCustomCodeMemory {
    /// Create a new callback-based custom code memory
    pub fn new(
        code_mem_id: i64,
        alignment_fn: CodeMemAlignmentFn,
        publish_fn: CodeMemPublishFn,
        unpublish_fn: CodeMemUnpublishFn,
    ) -> Self {
        Self {
            code_mem_id,
            alignment_fn,
            publish_fn,
            unpublish_fn,
        }
    }
}

impl wasmtime::CustomCodeMemory for CallbackCustomCodeMemory {
    fn required_alignment(&self) -> usize {
        unsafe { (self.alignment_fn)(self.code_mem_id) as usize }
    }

    fn publish_executable(&self, ptr: *const u8, len: usize) -> wasmtime::Result<()> {
        let result = unsafe { (self.publish_fn)(self.code_mem_id, ptr, len as u64) };
        if result == 0 {
            Ok(())
        } else {
            Err(wasmtime::Error::msg(
                "CustomCodeMemory.publishExecutable failed",
            ))
        }
    }

    fn unpublish_executable(&self, ptr: *const u8, len: usize) -> wasmtime::Result<()> {
        let result = unsafe { (self.unpublish_fn)(self.code_mem_id, ptr, len as u64) };
        if result == 0 {
            Ok(())
        } else {
            Err(wasmtime::Error::msg(
                "CustomCodeMemory.unpublishExecutable failed",
            ))
        }
    }
}

/// Summary of engine configuration for runtime introspection and FFI queries.
///
/// Only contains fields that are read after engine construction:
/// - Runtime-essential fields used by store/instance creation
/// - Feature flags exposed to Java via `supports_feature()` and FFI getters
///
/// Construction-only settings (strategy, opt_level, debug_info, etc.) are
/// applied to `wasmtime::Config` during `EngineBuilder::build()` and not stored.
#[derive(Debug, Clone)]
pub struct EngineConfigSummary {
    // ===== Wasm feature flags (exposed via supports_feature()) =====
    pub wasm_threads: bool,
    pub wasm_reference_types: bool,
    pub wasm_simd: bool,
    pub wasm_bulk_memory: bool,
    pub wasm_multi_value: bool,
    pub wasm_multi_memory: bool,
    pub wasm_tail_call: bool,
    pub wasm_relaxed_simd: bool,
    pub wasm_function_references: bool,
    pub wasm_gc: bool,
    pub wasm_exceptions: bool,
    pub wasm_memory64: bool,
    pub wasm_extended_const: bool,
    pub wasm_component_model: bool,
    pub wasm_custom_page_sizes: bool,
    pub wasm_wide_arithmetic: bool,
    pub wasm_stack_switching: bool,
    pub wasm_shared_everything_threads: bool,
    pub wasm_component_model_async: bool,
    pub wasm_component_model_async_builtins: bool,
    pub wasm_component_model_async_stackful: bool,
    pub wasm_component_model_error_context: bool,
    pub wasm_component_model_gc: bool,
    pub wasm_component_model_threading: bool,
    pub wasm_component_model_fixed_length_lists: bool,

    // ===== WasmFeatures-only flags (no individual Config method) =====
    pub wasm_mutable_global: bool,
    pub wasm_saturating_float_to_int: bool,
    pub wasm_sign_extension: bool,
    pub wasm_floats: bool,
    pub wasm_memory_control: bool,
    pub wasm_legacy_exceptions: bool,
    pub wasm_gc_types: bool,
    pub wasm_component_model_values: bool,
    pub wasm_component_model_nested_names: bool,
    pub wasm_component_model_map: bool,
    pub wasm_call_indirect_overlong: bool,
    pub wasm_bulk_memory_opt: bool,
    pub wasm_custom_descriptors: bool,
    pub wasm_compact_imports: bool,

    // ===== Runtime-essential fields =====
    /// Whether fuel consumption is enabled (drives store fuel initialization)
    pub fuel_enabled: bool,
    /// Whether epoch-based interruption is enabled (drives store epoch setup)
    pub epoch_interruption: bool,

    // ===== Introspection mirrors (Engine accessor methods) =====
    pub max_stack_size: Option<usize>,
    pub async_support: bool,
    pub concurrency_support: bool,
    pub coredump_on_trap: bool,

    // ===== FFI getter fields =====
    pub memory_reservation: Option<u64>,
    pub memory_guard_size: Option<u64>,
    pub memory_reservation_for_growth: Option<u64>,
    pub wmemcheck_enabled: bool,
    pub table_lazy_init: bool,
}

impl Default for EngineConfigSummary {
    fn default() -> Self {
        Self {
            wasm_threads: false,
            wasm_reference_types: false,
            wasm_simd: false,
            wasm_bulk_memory: false,
            wasm_multi_value: false,
            wasm_multi_memory: false,
            wasm_tail_call: false,
            wasm_relaxed_simd: false,
            wasm_function_references: false,
            wasm_gc: false,
            wasm_exceptions: false,
            wasm_memory64: false,
            wasm_extended_const: false,
            wasm_component_model: false,
            wasm_custom_page_sizes: false,
            wasm_wide_arithmetic: false,
            wasm_stack_switching: false,
            wasm_shared_everything_threads: false,
            wasm_component_model_async: false,
            wasm_component_model_async_builtins: false,
            wasm_component_model_async_stackful: false,
            wasm_component_model_error_context: false,
            wasm_component_model_gc: false,
            wasm_component_model_threading: false,
            wasm_component_model_fixed_length_lists: false,
            wasm_mutable_global: true,
            wasm_saturating_float_to_int: true,
            wasm_sign_extension: true,
            wasm_floats: true,
            wasm_memory_control: false,
            wasm_legacy_exceptions: false,
            wasm_gc_types: false,
            wasm_component_model_values: false,
            wasm_component_model_nested_names: false,
            wasm_component_model_map: false,
            wasm_call_indirect_overlong: false,
            wasm_bulk_memory_opt: false,
            wasm_custom_descriptors: false,
            wasm_compact_imports: false,
            fuel_enabled: false,
            epoch_interruption: false,
            max_stack_size: None,
            async_support: false,
            concurrency_support: false,
            coredump_on_trap: false,
            memory_reservation: None,
            memory_guard_size: None,
            memory_reservation_for_growth: None,
            wmemcheck_enabled: false,
            table_lazy_init: true,
        }
    }
}

impl EngineConfigSummary {
    /// Returns best-guess Wasmtime defaults for when Engine is created with
    /// a raw `wasmtime::Config` (no EngineBuilder). Wasmtime's Config doesn't
    /// expose its settings for introspection, so these are hardcoded.
    pub(crate) fn default_assumptions() -> Self {
        EngineConfigSummary {
            wasm_threads: true,
            wasm_reference_types: true,
            wasm_simd: true,
            wasm_bulk_memory: true,
            wasm_multi_value: true,
            wasm_multi_memory: false,
            wasm_tail_call: false,
            wasm_relaxed_simd: false,
            wasm_function_references: false,
            wasm_gc: false,
            wasm_exceptions: false,
            wasm_memory64: true,
            wasm_extended_const: true,
            wasm_component_model: true,
            wasm_custom_page_sizes: false,
            wasm_wide_arithmetic: false,
            wasm_stack_switching: false,
            wasm_shared_everything_threads: false,
            wasm_component_model_async: false,
            wasm_component_model_async_builtins: false,
            wasm_component_model_async_stackful: false,
            wasm_component_model_error_context: false,
            wasm_component_model_gc: false,
            wasm_component_model_threading: false,
            wasm_component_model_fixed_length_lists: false,
            wasm_mutable_global: true,
            wasm_saturating_float_to_int: true,
            wasm_sign_extension: true,
            wasm_floats: true,
            wasm_memory_control: false,
            wasm_legacy_exceptions: false,
            wasm_gc_types: false,
            wasm_component_model_values: false,
            wasm_component_model_nested_names: false,
            wasm_component_model_map: false,
            wasm_call_indirect_overlong: false,
            wasm_bulk_memory_opt: false,
            wasm_custom_descriptors: false,
            wasm_compact_imports: false,
            fuel_enabled: false, // Wasmtime default: consume_fuel is off
            epoch_interruption: false,
            max_stack_size: None,
            async_support: false,
            concurrency_support: false,
            coredump_on_trap: false,
            memory_reservation: None,
            memory_guard_size: None,
            memory_reservation_for_growth: None,
            wmemcheck_enabled: false,
            table_lazy_init: true,
        }
    }

    pub(crate) fn from_builder(builder: &EngineBuilder) -> Self {
        EngineConfigSummary {
            wasm_threads: builder.wasm_threads,
            wasm_reference_types: builder.wasm_reference_types,
            wasm_simd: builder.wasm_simd,
            wasm_bulk_memory: builder.wasm_bulk_memory,
            wasm_multi_value: builder.wasm_multi_value,
            wasm_multi_memory: builder.wasm_multi_memory,
            wasm_tail_call: builder.wasm_tail_call,
            wasm_relaxed_simd: builder.wasm_relaxed_simd,
            wasm_function_references: builder.wasm_function_references,
            wasm_gc: builder.wasm_gc,
            wasm_exceptions: builder.wasm_exceptions,
            wasm_memory64: builder.wasm_memory64,
            wasm_extended_const: builder.wasm_extended_const,
            wasm_component_model: builder.wasm_component_model,
            wasm_custom_page_sizes: builder.wasm_custom_page_sizes,
            wasm_wide_arithmetic: builder.wasm_wide_arithmetic,
            wasm_stack_switching: builder.wasm_stack_switching,
            wasm_shared_everything_threads: builder.wasm_shared_everything_threads,
            wasm_component_model_async: builder.wasm_component_model_async,
            wasm_component_model_async_builtins: builder.wasm_component_model_async_builtins,
            wasm_component_model_async_stackful: builder.wasm_component_model_async_stackful,
            wasm_component_model_error_context: builder.wasm_component_model_error_context,
            wasm_component_model_gc: builder.wasm_component_model_gc,
            wasm_component_model_threading: builder.wasm_component_model_threading,
            wasm_component_model_fixed_length_lists: builder
                .wasm_component_model_fixed_length_lists,
            wasm_mutable_global: builder.wasm_mutable_global,
            wasm_saturating_float_to_int: builder.wasm_saturating_float_to_int,
            wasm_sign_extension: builder.wasm_sign_extension,
            wasm_floats: builder.wasm_floats,
            wasm_memory_control: builder.wasm_memory_control,
            wasm_legacy_exceptions: builder.wasm_legacy_exceptions,
            wasm_gc_types: builder.wasm_gc_types,
            wasm_component_model_values: builder.wasm_component_model_values,
            wasm_component_model_nested_names: builder.wasm_component_model_nested_names,
            wasm_component_model_map: builder.wasm_component_model_map,
            wasm_call_indirect_overlong: builder.wasm_call_indirect_overlong,
            wasm_bulk_memory_opt: builder.wasm_bulk_memory_opt,
            wasm_custom_descriptors: builder.wasm_custom_descriptors,
            wasm_compact_imports: builder.wasm_compact_imports,
            fuel_enabled: builder.fuel_enabled,
            epoch_interruption: builder.epoch_interruption,
            max_stack_size: builder.max_stack_size,
            async_support: builder.async_support,
            concurrency_support: builder.concurrency_support,
            coredump_on_trap: builder.coredump_on_trap,
            memory_reservation: builder.memory_reservation,
            memory_guard_size: builder.memory_guard_size,
            memory_reservation_for_growth: builder.memory_reservation_for_growth,
            #[cfg(feature = "wmemcheck")]
            wmemcheck_enabled: builder.wmemcheck_enabled,
            #[cfg(not(feature = "wmemcheck"))]
            wmemcheck_enabled: false,
            table_lazy_init: builder.table_lazy_init,
        }
    }
}

/// Builder for creating configured engines
pub struct EngineBuilder {
    pub(crate) config: Config,
    pub(crate) strategy: Option<Strategy>,
    pub(crate) opt_level: Option<OptLevel>,
    pub(crate) debug_info: bool,
    pub(crate) fuel_enabled: bool,
    pub(crate) max_stack_size: Option<usize>,
    pub(crate) epoch_interruption: bool,
    // Track wasm features separately for proper config summary
    pub(crate) wasm_threads: bool,
    pub(crate) wasm_reference_types: bool,
    pub(crate) wasm_simd: bool,
    pub(crate) wasm_bulk_memory: bool,
    pub(crate) wasm_multi_value: bool,
    pub(crate) wasm_multi_memory: bool,
    pub(crate) wasm_tail_call: bool,
    pub(crate) wasm_relaxed_simd: bool,
    pub(crate) wasm_function_references: bool,
    pub(crate) wasm_gc: bool,
    pub(crate) wasm_exceptions: bool,
    pub(crate) wasm_memory64: bool,
    pub(crate) wasm_extended_const: bool,
    pub(crate) wasm_component_model: bool,
    pub(crate) wasm_custom_page_sizes: bool,
    pub(crate) wasm_wide_arithmetic: bool,
    pub(crate) wasm_stack_switching: bool,
    pub(crate) wasm_shared_everything_threads: bool,
    pub(crate) wasm_component_model_async: bool,
    pub(crate) wasm_component_model_async_builtins: bool,
    pub(crate) wasm_component_model_async_stackful: bool,
    pub(crate) wasm_component_model_error_context: bool,
    pub(crate) wasm_component_model_gc: bool,
    pub(crate) wasm_component_model_fixed_length_lists: bool,
    // Features settable only via WasmFeatures bitflags (no individual Config method)
    pub(crate) wasm_mutable_global: bool,
    pub(crate) wasm_saturating_float_to_int: bool,
    pub(crate) wasm_sign_extension: bool,
    pub(crate) wasm_floats: bool,
    pub(crate) wasm_memory_control: bool,
    pub(crate) wasm_legacy_exceptions: bool,
    pub(crate) wasm_gc_types: bool,
    pub(crate) wasm_component_model_values: bool,
    pub(crate) wasm_component_model_nested_names: bool,
    pub(crate) wasm_component_model_map: bool,
    pub(crate) wasm_call_indirect_overlong: bool,
    pub(crate) wasm_bulk_memory_opt: bool,
    pub(crate) wasm_custom_descriptors: bool,
    pub(crate) wasm_compact_imports: bool,
    pub(crate) async_support: bool,
    pub(crate) concurrency_support: bool,
    pub(crate) coredump_on_trap: bool,
    // Memory configuration options
    pub(crate) memory_reservation: Option<u64>,
    pub(crate) memory_guard_size: Option<u64>,
    pub(crate) memory_reservation_for_growth: Option<u64>,
    // Cranelift configuration options
    pub(crate) cranelift_debug_verifier: bool,
    pub(crate) cranelift_nan_canonicalization: bool,
    pub(crate) cranelift_pcc: bool,
    pub(crate) cranelift_regalloc_algorithm: Option<RegallocAlgorithm>,
    // wmemcheck - only available when compiled with wmemcheck feature
    #[cfg(feature = "wmemcheck")]
    pub(crate) wmemcheck_enabled: bool,
    // Table lazy initialization - enabled by default for faster instantiation
    pub(crate) table_lazy_init: bool,
    // GC support infrastructure - required for GC, function references, exceptions
    pub(crate) gc_support: bool,
    // GC collector implementation choice
    pub(crate) collector: Option<wasmtime::Collector>,
    // Memory may relocate at runtime
    pub(crate) memory_may_move: bool,
    // Guard regions before linear memory
    pub(crate) guard_before_linear_memory: bool,
    // Copy-on-write memory initialization
    pub(crate) memory_init_cow: bool,
    // Component model threading support (experimental)
    pub(crate) wasm_component_model_threading: bool,
    // Deterministic relaxed SIMD behavior
    pub(crate) relaxed_simd_deterministic: bool,
    // Zero async stacks before reuse
    pub(crate) async_stack_zeroing: bool,
    // Async stack size
    pub(crate) async_stack_size: Option<usize>,
    // Multi-threaded compilation
    pub(crate) parallel_compilation: bool,
    // Use Mach ports on macOS
    pub(crate) macos_use_mach_ports: bool,
    // WebAssembly backtrace collection
    pub(crate) wasm_backtrace: bool,
    // Address map generation for debugging
    pub(crate) generate_address_map: bool,
    // Shared memory support (independent of wasm threads)
    pub(crate) shared_memory: bool,
    // Module version strategy
    pub(crate) module_version_strategy: Option<wasmtime::ModuleVersionStrategy>,
    // Instance allocation strategy
    pub(crate) allocation_strategy: Option<wasmtime::InstanceAllocationStrategy>,
    // Profiling strategy
    pub(crate) profiling_strategy: wasmtime::ProfilingStrategy,
    // Native unwind info
    pub(crate) native_unwind_info: bool,
    // Cranelift compiler inlining
    pub(crate) compiler_inlining: bool,
    // Debug adapter modules support
    pub(crate) debug_adapter_modules: bool,
    // Force memfd for memory initialization (Linux optimization)
    pub(crate) force_memory_init_memfd: bool,
    // Cranelift Wasmtime debug checks
    pub(crate) cranelift_debug_checks: bool,
    // Enable or disable the compiler (allows runtime-only engines)
    pub(crate) enable_compiler: bool,
    // Acknowledge x86 float ABI behavior
    pub(crate) x86_float_abi_ok: bool,
    // Record/replay configuration
    #[cfg(feature = "rr")]
    pub(crate) rr_config: wasmtime::RRConfig,
}

impl EngineBuilder {
    /// Create new engine builder with safe defaults
    pub(crate) fn new() -> Self {
        let mut config = safe_wasmtime_config();

        // Set production-optimized defaults
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::Speed);

        // Enable commonly used WebAssembly features
        config.wasm_reference_types(true);
        config.wasm_bulk_memory(true);
        config.wasm_multi_value(true);
        config.wasm_simd(true);
        config.wasm_function_references(true); // Tier 2 - enable for ref.func support

        // Enable Component Model support (Tier 1 feature)
        config.wasm_component_model(true);

        // Configure stack size - increase from default 512 KiB to 2 MiB for JNI safety
        config.max_wasm_stack(2 * 1024 * 1024);

        // Enable debug info for better backtraces during development
        config.debug_info(true);

        // Enable WASM backtraces for better error diagnostics
        config.wasm_backtrace(true);
        config.wasm_backtrace_details(wasmtime::WasmBacktraceDetails::Enable);

        // Note: Fuel consumption is opt-in via StoreBuilder.fuel_limit()
        // config.consume_fuel(true);

        EngineBuilder {
            config,
            strategy: Some(Strategy::Cranelift),
            opt_level: Some(OptLevel::Speed),
            debug_info: true,
            fuel_enabled: false,
            max_stack_size: None,
            epoch_interruption: false,
            wasm_threads: true,
            wasm_reference_types: true,
            wasm_simd: true,
            wasm_bulk_memory: true,
            wasm_multi_value: true,
            wasm_multi_memory: false,
            wasm_tail_call: false,
            wasm_relaxed_simd: false,
            wasm_function_references: true, // Tier 2 - enable for ref.func support
            wasm_gc: false,
            wasm_exceptions: false,
            wasm_memory64: true,                        // Tier 1 - on by default
            wasm_extended_const: true,                  // Tier 1 - on by default
            wasm_component_model: true,                 // Tier 1 - on by default
            wasm_custom_page_sizes: false,              // Tier 3 - off by default
            wasm_wide_arithmetic: false,                // Tier 3 - off by default
            wasm_stack_switching: false,                // Tier 3 - off by default
            wasm_shared_everything_threads: false,      // Tier 3 - off by default
            wasm_component_model_async: false, // Component model extension - off by default
            wasm_component_model_async_builtins: false, // Component model extension - off by default
            wasm_component_model_async_stackful: false, // Component model extension - off by default
            wasm_component_model_error_context: false, // Component model extension - off by default
            wasm_component_model_gc: false,            // Component model extension - off by default
            wasm_component_model_fixed_length_lists: false, // Component model extension - off by default
            // Features settable only via WasmFeatures bitflags
            wasm_mutable_global: true,          // MVP default - always on
            wasm_saturating_float_to_int: true, // MVP default - always on
            wasm_sign_extension: true,          // MVP default - always on
            wasm_floats: true,                  // Core feature - always on
            wasm_memory_control: false,         // Experimental - off by default
            wasm_legacy_exceptions: false,      // Deprecated - off by default
            wasm_gc_types: false,               // Structural types only - off by default
            wasm_component_model_values: false, // Component model - off by default
            wasm_component_model_nested_names: false, // Component model - off by default
            wasm_component_model_map: false,    // Component model (new in 42.0.1) - off by default
            wasm_call_indirect_overlong: false, // Legacy compatibility - off by default
            wasm_bulk_memory_opt: false,        // Core - off by default
            wasm_custom_descriptors: false,     // Core - off by default
            wasm_compact_imports: false,        // Component (new in 42.0.1) - off by default
            async_support: false,               // Async execution support - off by default
            concurrency_support: false,         // Concurrency support - off by default
            coredump_on_trap: false,            // Coredump on trap - off by default
            memory_reservation: None,           // Memory reservation - use Wasmtime default
            memory_guard_size: None,            // Memory guard size - use Wasmtime default
            memory_reservation_for_growth: None, // Memory reservation for growth - use Wasmtime default
            cranelift_debug_verifier: false,     // Cranelift debug verifier - off by default
            cranelift_nan_canonicalization: false, // Cranelift NaN canonicalization - off by default
            cranelift_pcc: false,                  // Cranelift proof-carrying code - off by default
            cranelift_regalloc_algorithm: None,    // Cranelift regalloc algorithm - use default
            #[cfg(feature = "wmemcheck")]
            wmemcheck_enabled: false, // wmemcheck - off by default
            table_lazy_init: true, // Table lazy init - on by default (wasmtime default)
            // New config options with wasmtime defaults
            gc_support: true, // GC support - on by default (enables GC infrastructure)
            collector: None,  // Collector - use wasmtime default (Auto)
            memory_may_move: true, // Memory may move - on by default
            guard_before_linear_memory: true, // Guard before memory - on by default
            memory_init_cow: true, // CoW memory init - on by default
            wasm_component_model_threading: false, // Component threading - off (experimental)
            relaxed_simd_deterministic: false, // Relaxed SIMD deterministic - off by default
            async_stack_zeroing: false, // Async stack zeroing - off by default
            async_stack_size: None, // Async stack size - use wasmtime default
            parallel_compilation: true, // Parallel compilation - on by default
            macos_use_mach_ports: true, // Mach ports on macOS - on by default
            wasm_backtrace: true, // Backtrace collection - on by default
            generate_address_map: true, // Address map generation - on by default
            shared_memory: false, // Shared memory - off by default
            module_version_strategy: None, // Module version - use wasmtime default
            allocation_strategy: None, // Allocation strategy - use wasmtime default (OnDemand)
            profiling_strategy: wasmtime::ProfilingStrategy::None, // No profiling by default
            native_unwind_info: true, // Native unwind info - on by default (wasmtime default)
            compiler_inlining: true, // Compiler inlining - on by default (wasmtime default)
            debug_adapter_modules: false, // Debug adapter modules - off by default
            force_memory_init_memfd: false, // Force memfd - off by default (Linux-specific)
            cranelift_debug_checks: false, // Cranelift debug checks - off by default
            enable_compiler: true, // Compiler enabled - on by default
            x86_float_abi_ok: false, // x86 float ABI acknowledgment - off by default
            #[cfg(feature = "rr")]
            rr_config: wasmtime::RRConfig::None, // Record/replay - disabled by default
        }
    }

    /// Set compilation strategy
    pub fn strategy(mut self, strategy: Strategy) -> Self {
        self.config.strategy(strategy.clone());
        self.strategy = Some(strategy);
        self
    }

    /// Set optimization level
    pub fn opt_level(mut self, level: OptLevel) -> Self {
        self.config.cranelift_opt_level(level.clone());
        self.opt_level = Some(level);
        self
    }

    /// Enable or disable debug information
    pub fn debug_info(mut self, enable: bool) -> Self {
        self.config.debug_info(enable);
        self.debug_info = enable;
        self
    }

    /// Configure WebAssembly threads support
    /// Also enables shared memory support when threads are enabled
    pub fn wasm_threads(mut self, enable: bool) -> Self {
        self.config.wasm_threads(enable);
        // Shared memory is required for the threads proposal to work with shared memories
        self.config.shared_memory(enable);
        self.wasm_threads = enable;
        self
    }

    /// Configure WebAssembly reference types support
    pub fn wasm_reference_types(mut self, enable: bool) -> Self {
        self.config.wasm_reference_types(enable);
        self.wasm_reference_types = enable;
        self
    }

    /// Configure WebAssembly SIMD support
    pub fn wasm_simd(mut self, enable: bool) -> Self {
        self.config.wasm_simd(enable);
        self.wasm_simd = enable;
        self
    }

    /// Configure WebAssembly bulk memory support
    pub fn wasm_bulk_memory(mut self, enable: bool) -> Self {
        self.config.wasm_bulk_memory(enable);
        self.wasm_bulk_memory = enable;
        self
    }

    /// Configure WebAssembly multi-value support
    pub fn wasm_multi_value(mut self, enable: bool) -> Self {
        self.config.wasm_multi_value(enable);
        self.wasm_multi_value = enable;
        self
    }

    /// Configure WebAssembly multi-memory support
    pub fn wasm_multi_memory(mut self, enable: bool) -> Self {
        self.config.wasm_multi_memory(enable);
        self.wasm_multi_memory = enable;
        self
    }

    /// Configure WebAssembly tail call support
    pub fn wasm_tail_call(mut self, enable: bool) -> Self {
        self.config.wasm_tail_call(enable);
        self.wasm_tail_call = enable;
        self
    }

    /// Configure WebAssembly relaxed SIMD support
    pub fn wasm_relaxed_simd(mut self, enable: bool) -> Self {
        self.config.wasm_relaxed_simd(enable);
        self.wasm_relaxed_simd = enable;
        self
    }

    /// Configure WebAssembly function references support
    pub fn wasm_function_references(mut self, enable: bool) -> Self {
        self.config.wasm_function_references(enable);
        self.wasm_function_references = enable;
        self
    }

    /// Configure WebAssembly garbage collection support
    pub fn wasm_gc(mut self, enable: bool) -> Self {
        self.config.wasm_gc(enable);
        self.wasm_gc = enable;
        self
    }

    /// Configure WebAssembly exceptions support
    pub fn wasm_exceptions(mut self, enable: bool) -> Self {
        self.config.wasm_exceptions(enable);
        self.wasm_exceptions = enable;
        self
    }

    /// Configure WebAssembly 64-bit memory support
    pub fn wasm_memory64(mut self, enable: bool) -> Self {
        self.config.wasm_memory64(enable);
        self.wasm_memory64 = enable;
        self
    }

    /// Configure WebAssembly extended constant expressions support
    pub fn wasm_extended_const(mut self, enable: bool) -> Self {
        self.config.wasm_extended_const(enable);
        self.wasm_extended_const = enable;
        self
    }

    /// Configure WebAssembly component model support
    pub fn wasm_component_model(mut self, enable: bool) -> Self {
        self.config.wasm_component_model(enable);
        self.wasm_component_model = enable;
        self
    }

    /// Configure WebAssembly custom page sizes support
    pub fn wasm_custom_page_sizes(mut self, enable: bool) -> Self {
        self.config.wasm_custom_page_sizes(enable);
        self.wasm_custom_page_sizes = enable;
        self
    }

    /// Configure WebAssembly wide arithmetic support
    pub fn wasm_wide_arithmetic(mut self, enable: bool) -> Self {
        self.config.wasm_wide_arithmetic(enable);
        self.wasm_wide_arithmetic = enable;
        self
    }

    /// Configure WebAssembly stack switching support
    pub fn wasm_stack_switching(mut self, enable: bool) -> Self {
        self.config.wasm_stack_switching(enable);
        self.wasm_stack_switching = enable;
        self
    }

    /// Configure WebAssembly shared-everything-threads support
    pub fn wasm_shared_everything_threads(mut self, enable: bool) -> Self {
        self.config.wasm_shared_everything_threads(enable);
        self.wasm_shared_everything_threads = enable;
        self
    }

    /// Configure WebAssembly component model async support
    pub fn wasm_component_model_async(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_async(enable);
        self.wasm_component_model_async = enable;
        self
    }

    /// Configure WebAssembly component model async builtins support
    pub fn wasm_component_model_async_builtins(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_async_builtins(enable);
        self.wasm_component_model_async_builtins = enable;
        self
    }

    /// Configure WebAssembly component model async stackful support
    pub fn wasm_component_model_async_stackful(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_async_stackful(enable);
        self.wasm_component_model_async_stackful = enable;
        self
    }

    /// Configure WebAssembly component model error context support
    pub fn wasm_component_model_error_context(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_error_context(enable);
        self.wasm_component_model_error_context = enable;
        self
    }

    /// Configure WebAssembly component model GC support
    pub fn wasm_component_model_gc(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_gc(enable);
        self.wasm_component_model_gc = enable;
        self
    }

    /// Configure WebAssembly component model fixed-length lists support
    pub fn wasm_component_model_fixed_length_lists(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_fixed_length_lists(enable);
        self.wasm_component_model_fixed_length_lists = enable;
        self
    }

    /// Configure WebAssembly mutable global support (MVP default, always on)
    ///
    /// Note: Only enables the feature when `enable` is true. In Wasmtime 42+,
    /// calling `wasm_features(FLAG, false)` can reset unrelated features due to
    /// how the feature configuration is tracked internally.
    pub fn wasm_mutable_global(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::MUTABLE_GLOBAL, true);
        }
        self.wasm_mutable_global = enable;
        self
    }

    /// Configure WebAssembly saturating float-to-int conversions (MVP default, always on)
    pub fn wasm_saturating_float_to_int(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::SATURATING_FLOAT_TO_INT, true);
        }
        self.wasm_saturating_float_to_int = enable;
        self
    }

    /// Configure WebAssembly sign extension operations (MVP default, always on)
    pub fn wasm_sign_extension(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::SIGN_EXTENSION, true);
        }
        self.wasm_sign_extension = enable;
        self
    }

    /// Configure WebAssembly floating point support
    pub fn wasm_floats(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::FLOATS, true);
        }
        self.wasm_floats = enable;
        self
    }

    /// Configure WebAssembly memory control support (experimental)
    pub fn wasm_memory_control(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::MEMORY_CONTROL, true);
        }
        self.wasm_memory_control = enable;
        self
    }

    /// Configure WebAssembly legacy exception handling support (deprecated)
    pub fn wasm_legacy_exceptions(mut self, enable: bool) -> Self {
        self.config.wasm_legacy_exceptions(enable);
        self.wasm_legacy_exceptions = enable;
        self
    }

    /// Configure WebAssembly GC structural types support
    pub fn wasm_gc_types(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::GC_TYPES, true);
        }
        self.wasm_gc_types = enable;
        self
    }

    /// Configure WebAssembly Component Model values support
    pub fn wasm_component_model_values(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::CM_VALUES, true);
        }
        self.wasm_component_model_values = enable;
        self
    }

    /// Configure WebAssembly Component Model nested names support
    pub fn wasm_component_model_nested_names(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::CM_NESTED_NAMES, true);
        }
        self.wasm_component_model_nested_names = enable;
        self
    }

    /// Configure WebAssembly Component Model map type support
    pub fn wasm_component_model_map(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::CM_MAP, true);
        }
        self.wasm_component_model_map = enable;
        self
    }

    /// Configure WebAssembly call_indirect overlong encoding support
    pub fn wasm_call_indirect_overlong(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::CALL_INDIRECT_OVERLONG, true);
        }
        self.wasm_call_indirect_overlong = enable;
        self
    }

    /// Configure WebAssembly bulk memory optimized operations support
    pub fn wasm_bulk_memory_opt(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::BULK_MEMORY_OPT, true);
        }
        self.wasm_bulk_memory_opt = enable;
        self
    }

    /// Configure WebAssembly custom descriptors support
    pub fn wasm_custom_descriptors(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::CUSTOM_DESCRIPTORS, true);
        }
        self.wasm_custom_descriptors = enable;
        self
    }

    /// Configure WebAssembly Component Model compact imports support
    pub fn wasm_compact_imports(mut self, enable: bool) -> Self {
        if enable {
            self.config
                .wasm_features(wasmtime::WasmFeatures::COMPACT_IMPORTS, true);
        }
        self.wasm_compact_imports = enable;
        self
    }

    /// Enable or disable fuel consumption tracking
    pub fn fuel_enabled(mut self, enable: bool) -> Self {
        self.config.consume_fuel(enable);
        self.fuel_enabled = enable;
        self
    }

    /// Set maximum stack size in bytes
    pub fn max_stack_size(mut self, size: usize) -> Self {
        self.config.max_wasm_stack(size);
        self.max_stack_size = Some(size);
        self
    }

    /// Enable or disable epoch-based interruption
    pub fn epoch_interruption(mut self, enable: bool) -> Self {
        self.config.epoch_interruption(enable);
        self.epoch_interruption = enable;
        self
    }

    /// Enable or disable async execution support
    ///
    /// Note: In Wasmtime 42.0.0+, `Config::async_support()` was removed.
    /// This method now only sets the internal tracking flag used by the
    /// engine to decide between sync and async code paths.
    pub fn async_support(mut self, enable: bool) -> Self {
        self.async_support = enable;
        self
    }

    /// Enable or disable concurrency support for `*_concurrent` APIs
    ///
    /// When enabled, the `run_concurrent` and `call_concurrent` APIs are available
    /// for concurrent component function calls. Requires the `component-model-async`
    /// crate feature.
    pub fn concurrency_support(mut self, enable: bool) -> Self {
        self.config.concurrency_support(enable);
        self.concurrency_support = enable;
        self
    }

    /// Enable or disable coredump generation on trap
    ///
    /// When enabled, traps will generate a coredump that can be used for
    /// post-mortem debugging of WebAssembly execution failures.
    pub fn coredump_on_trap(mut self, enable: bool) -> Self {
        self.config.coredump_on_trap(enable);
        self.coredump_on_trap = enable;
        self
    }

    /// Set memory reservation size in bytes
    ///
    /// This configures the size of the virtual memory reservation for linear
    /// memories. The default is 4GB when 64-bit memories are enabled, or
    /// whatever the default is in wasmtime.
    ///
    /// # Arguments
    /// * `size` - The reservation size in bytes
    pub fn memory_reservation(mut self, size: u64) -> Self {
        self.config.memory_reservation(size);
        self.memory_reservation = Some(size);
        self
    }

    /// Set memory guard size in bytes
    ///
    /// Guard pages are unmapped pages placed at the end of the memory to
    /// catch out-of-bounds accesses without explicit bounds checks.
    ///
    /// # Arguments
    /// * `size` - The guard size in bytes
    pub fn memory_guard_size(mut self, size: u64) -> Self {
        self.config.memory_guard_size(size);
        self.memory_guard_size = Some(size);
        self
    }

    /// Set memory reservation for growth in bytes
    ///
    /// This configures how much extra virtual memory is reserved after the
    /// initial memory size to allow for memory growth without needing to
    /// move the memory.
    ///
    /// # Arguments
    /// * `size` - The reservation for growth in bytes
    pub fn memory_reservation_for_growth(mut self, size: u64) -> Self {
        self.config.memory_reservation_for_growth(size);
        self.memory_reservation_for_growth = Some(size);
        self
    }

    /// Enable or disable Cranelift debug verifier
    ///
    /// When enabled, Cranelift will perform additional verification checks
    /// on the generated machine code. This is useful for debugging compiler
    /// issues but adds overhead to compilation.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable the debug verifier
    pub fn cranelift_debug_verifier(mut self, enable: bool) -> Self {
        self.config.cranelift_debug_verifier(enable);
        self.cranelift_debug_verifier = enable;
        self
    }

    /// Enable or disable Cranelift NaN canonicalization
    ///
    /// When enabled, floating-point NaN values are canonicalized to a single
    /// representation. This ensures deterministic behavior across different
    /// platforms and is important for reproducible WebAssembly execution.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable NaN canonicalization
    pub fn cranelift_nan_canonicalization(mut self, enable: bool) -> Self {
        self.config.cranelift_nan_canonicalization(enable);
        self.cranelift_nan_canonicalization = enable;
        self
    }

    /// Enable or disable Cranelift proof-carrying code validation
    ///
    /// Proof-carrying code (PCC) is an advanced feature that enables runtime
    /// verification of safety properties of the generated machine code.
    /// This can be used to eliminate some runtime checks but adds compilation overhead.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable PCC validation
    pub fn cranelift_pcc(mut self, enable: bool) -> Self {
        self.config.cranelift_pcc(enable);
        self.cranelift_pcc = enable;
        self
    }

    /// Set the Cranelift register allocation algorithm
    ///
    /// This configures which register allocation algorithm Cranelift uses
    /// during code generation. Different algorithms have different trade-offs
    /// between compilation speed and generated code quality.
    ///
    /// # Arguments
    /// * `algorithm` - The register allocation algorithm to use
    pub fn cranelift_regalloc_algorithm(mut self, algorithm: RegallocAlgorithm) -> Self {
        self.config.cranelift_regalloc_algorithm(algorithm.clone());
        self.cranelift_regalloc_algorithm = Some(algorithm);
        self
    }

    /// Set an arbitrary Cranelift compiler flag
    ///
    /// This allows setting any Cranelift flag by name and value string.
    /// For example: `cranelift_flag_set("opt_level", "speed_and_size")`.
    ///
    /// # Arguments
    /// * `name` - The flag name
    /// * `value` - The flag value
    pub fn cranelift_flag_set(mut self, name: &str, value: &str) -> Self {
        // Safety: name and value are valid string references
        unsafe {
            self.config.cranelift_flag_set(name, value);
        }
        self
    }

    /// Enable or disable wmemcheck (WebAssembly memory checker)
    ///
    /// wmemcheck is a memory debugging tool similar to Valgrind's memcheck that
    /// detects memory errors in WebAssembly programs at runtime. It can detect:
    /// - Use of uninitialized memory
    /// - Use-after-free (for tables)
    /// - Double-free errors (for tables)
    ///
    /// **Note**: This is only available when the `wmemcheck` Cargo feature is enabled.
    /// It adds significant runtime overhead and should only be used for debugging.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable wmemcheck memory checking
    #[cfg(feature = "wmemcheck")]
    pub fn wmemcheck(mut self, enable: bool) -> Self {
        self.config.wmemcheck(enable);
        self.wmemcheck_enabled = enable;
        self
    }

    /// Enable or disable table lazy initialization
    ///
    /// When enabled (the default), tables are initialized lazily which results in
    /// faster instantiation but slightly slower indirect calls. When disabled,
    /// tables are initialized eagerly during instantiation.
    ///
    /// This trade-off is typically worth it for modules that don't use all their
    /// table entries immediately after instantiation.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable lazy table initialization
    pub fn table_lazy_init(mut self, enable: bool) -> Self {
        self.config.table_lazy_init(enable);
        self.table_lazy_init = enable;
        self
    }

    /// Enable or disable GC support infrastructure
    ///
    /// GC support is required for the WebAssembly GC proposal, function references,
    /// and exception handling proposals. When disabled, these proposals cannot be used.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable GC support
    pub fn gc_support(mut self, enable: bool) -> Self {
        self.config.gc_support(enable);
        self.gc_support = enable;
        self
    }

    /// Configure which garbage collector implementation is used
    ///
    /// This selects the GC implementation for managing WebAssembly GC types.
    /// The default is `Collector::Auto` which selects an appropriate collector.
    ///
    /// # Arguments
    /// * `collector` - The collector implementation to use
    pub fn collector(mut self, collector: wasmtime::Collector) -> Self {
        self.config.collector(collector.clone());
        self.collector = Some(collector);
        self
    }

    /// Configure whether linear memories may relocate at runtime
    ///
    /// When enabled, Wasmtime may relocate the base pointer of a linear memory
    /// during memory growth operations. This allows for more efficient memory
    /// management but requires that host code never caches linear memory pointers.
    ///
    /// When disabled, the memory base pointer is guaranteed to remain stable,
    /// which is safer but may be less efficient.
    ///
    /// # Arguments
    /// * `enable` - Whether to allow memory relocation (default: true)
    pub fn memory_may_move(mut self, enable: bool) -> Self {
        self.config.memory_may_move(enable);
        self.memory_may_move = enable;
        self
    }

    /// Configure whether guard regions exist before linear memory
    ///
    /// Guard regions are unmapped pages that provide extra protection against
    /// buffer underflows. This is a defense-in-depth measure.
    ///
    /// # Arguments
    /// * `enable` - Whether to add guard regions before memory (default: true)
    pub fn guard_before_linear_memory(mut self, enable: bool) -> Self {
        self.config.guard_before_linear_memory(enable);
        self.guard_before_linear_memory = enable;
        self
    }

    /// Enable or disable copy-on-write memory initialization
    ///
    /// When enabled, Wasmtime uses memory-mapped files for data segment
    /// initialization, which can significantly speed up module instantiation
    /// for modules with large data sections.
    ///
    /// # Arguments
    /// * `enable` - Whether to use CoW memory initialization (default: true)
    pub fn memory_init_cow(mut self, enable: bool) -> Self {
        self.config.memory_init_cow(enable);
        self.memory_init_cow = enable;
        self
    }

    /// Configure WebAssembly component model threading support (experimental)
    ///
    /// This enables threading support in the component model, corresponding to
    /// the shared-everything-threads proposal. This feature is highly experimental.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable component model threading
    pub fn wasm_component_model_threading(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_threading(enable);
        self.wasm_component_model_threading = enable;
        self
    }

    /// Configure deterministic relaxed SIMD behavior
    ///
    /// When enabled, relaxed SIMD instructions produce deterministic results
    /// across all platforms. This is useful for reproducible execution but
    /// may reduce performance on some platforms.
    ///
    /// # Arguments
    /// * `enable` - Whether to force deterministic relaxed SIMD (default: false)
    pub fn relaxed_simd_deterministic(mut self, enable: bool) -> Self {
        self.config.relaxed_simd_deterministic(enable);
        self.relaxed_simd_deterministic = enable;
        self
    }

    /// Configure async stack zeroing for defense-in-depth
    ///
    /// When enabled, async stacks are zeroed before (re)use. This provides
    /// defense-in-depth against information leakage but adds overhead.
    ///
    /// # Arguments
    /// * `enable` - Whether to zero async stacks (default: false)
    pub fn async_stack_zeroing(mut self, enable: bool) -> Self {
        self.config.async_stack_zeroing(enable);
        self.async_stack_zeroing = enable;
        self
    }

    /// Configure async stack size
    ///
    /// Sets the size of the stack used for async execution. This is separate
    /// from the WebAssembly linear memory stack and is used by the async
    /// runtime for suspending and resuming execution.
    ///
    /// # Arguments
    /// * `size` - The async stack size in bytes
    pub fn async_stack_size(mut self, size: usize) -> Self {
        self.config.async_stack_size(size);
        self.async_stack_size = Some(size);
        self
    }

    /// Configure parallel compilation
    ///
    /// When enabled (the default), module compilation uses multiple threads.
    /// Disabling this can be useful for debugging or in resource-constrained
    /// environments.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable parallel compilation (default: true)
    pub fn parallel_compilation(mut self, enable: bool) -> Self {
        self.config.parallel_compilation(enable);
        self.parallel_compilation = enable;
        self
    }

    /// Configure Mach port usage on macOS
    ///
    /// When enabled (the default on macOS), Wasmtime uses Mach ports instead
    /// of Unix signals for exception handling. This is more reliable but
    /// requires macOS-specific code paths.
    ///
    /// # Arguments
    /// * `enable` - Whether to use Mach ports on macOS (default: true)
    #[cfg(target_os = "macos")]
    pub fn macos_use_mach_ports(mut self, enable: bool) -> Self {
        self.config.macos_use_mach_ports(enable);
        self.macos_use_mach_ports = enable;
        self
    }

    /// Configure WebAssembly backtrace collection on traps
    ///
    /// When enabled, Wasmtime collects stack trace information when a trap occurs.
    ///
    /// # Arguments
    /// * `enable` - Whether to collect backtraces (default: true)
    pub fn wasm_backtrace(mut self, enable: bool) -> Self {
        self.config.wasm_backtrace(enable);
        self.wasm_backtrace = enable;
        self
    }

    /// Configure the level of detail for WebAssembly backtrace information
    ///
    /// This controls whether symbolic names are included in backtraces.
    ///
    /// # Arguments
    /// * `details` - The backtrace detail level
    pub fn wasm_backtrace_details(mut self, details: wasmtime::WasmBacktraceDetails) -> Self {
        self.config.wasm_backtrace_details(details);
        self
    }

    /// Configure address map generation for compiled code
    ///
    /// Address maps provide mapping from native code addresses back to WebAssembly
    /// bytecode offsets, useful for debugging and profiling.
    ///
    /// # Arguments
    /// * `enable` - Whether to generate address maps (default: true)
    pub fn generate_address_map(mut self, enable: bool) -> Self {
        self.config.generate_address_map(enable);
        self.generate_address_map = enable;
        self
    }

    /// Configure shared memory support (independent of wasm threads)
    ///
    /// When enabled, the `shared` attribute is allowed on memory definitions,
    /// enabling atomic operations on shared memory.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable shared memory (default: false)
    pub fn shared_memory(mut self, enable: bool) -> Self {
        self.config.shared_memory(enable);
        self.shared_memory = enable;
        self
    }

    /// Configure module version strategy for serialization
    ///
    /// This controls how module version compatibility is checked when
    /// deserializing precompiled modules. The default uses Wasmtime's version.
    ///
    /// # Arguments
    /// * `strategy` - The version strategy to use
    pub fn module_version_strategy(mut self, strategy: wasmtime::ModuleVersionStrategy) -> Self {
        let _ = self.config.module_version(strategy.clone());
        self.module_version_strategy = Some(strategy);
        self
    }

    /// Configure instance allocation strategy
    ///
    /// This configures how WebAssembly instances are allocated. The default
    /// is on-demand allocation. Pooling allocation can improve instantiation
    /// performance for high-throughput scenarios.
    ///
    /// # Arguments
    /// * `strategy` - The allocation strategy to use
    pub fn allocation_strategy(mut self, strategy: wasmtime::InstanceAllocationStrategy) -> Self {
        self.config.allocation_strategy(strategy.clone());
        self.allocation_strategy = Some(strategy);
        self
    }

    /// Set the profiling strategy for the engine.
    ///
    /// This configures how profiling information is collected during
    /// WebAssembly execution.
    ///
    /// # Arguments
    /// * `strategy` - The profiling strategy to use
    pub fn profiling_strategy(mut self, strategy: wasmtime::ProfilingStrategy) -> Self {
        self.config.profiler(strategy.clone());
        self.profiling_strategy = strategy;
        self
    }

    /// Enable or disable native unwind information.
    ///
    /// This controls whether the engine generates native unwind tables,
    /// which are needed for proper stack unwinding on some platforms.
    ///
    /// # Arguments
    /// * `enable` - Whether to generate native unwind info
    pub fn native_unwind_info(mut self, enable: bool) -> Self {
        self.config.native_unwind_info(enable);
        self.native_unwind_info = enable;
        self
    }

    /// Enable or disable Cranelift function inlining
    ///
    /// When enabled (the default), Cranelift may inline functions during compilation
    /// for better runtime performance.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable compiler inlining
    pub fn compiler_inlining(mut self, enable: bool) -> Self {
        self.config.compiler_inlining(enable);
        self.compiler_inlining = enable;
        self
    }

    /// Enable or disable debug adapter modules
    ///
    /// When enabled, debug adapter modules are allowed for DAP-based debugging.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable debug adapter modules
    pub fn debug_adapter_modules(mut self, enable: bool) -> Self {
        self.config.debug_adapter_modules(enable);
        self.debug_adapter_modules = enable;
        self
    }

    /// Enable or disable forcing memfd for memory initialization (Linux)
    ///
    /// When enabled, Wasmtime will always use memfd for memory initialization,
    /// even when copy-on-write initialization might not otherwise use it.
    ///
    /// # Arguments
    /// * `enable` - Whether to force memfd memory initialization
    pub fn force_memory_init_memfd(mut self, enable: bool) -> Self {
        self.config.force_memory_init_memfd(enable);
        self.force_memory_init_memfd = enable;
        self
    }

    /// Enable or disable Cranelift Wasmtime-specific debug checks
    ///
    /// When enabled, additional runtime assertions are inserted into compiled code
    /// to verify Wasmtime invariants. Primarily for Wasmtime development.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable Cranelift debug checks
    pub fn cranelift_debug_checks(mut self, enable: bool) -> Self {
        self.config.cranelift_wasmtime_debug_checks(enable);
        self.cranelift_debug_checks = enable;
        self
    }

    /// Enable or disable the compiler
    ///
    /// When disabled, the engine can only execute pre-compiled modules and cannot
    /// compile new WebAssembly modules. This is useful for runtime-only deployments
    /// where compilation is done ahead of time.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable the compiler (default: true)
    pub fn enable_compiler(mut self, enable: bool) -> Self {
        self.config.enable_compiler(enable);
        self.enable_compiler = enable;
        self
    }

    /// Acknowledge x86 float ABI behavior
    ///
    /// This setting acknowledges that the x86 calling convention for floats
    /// may differ from what is expected. Setting this to `true` suppresses
    /// the warning that Wasmtime would otherwise emit on x86 platforms.
    ///
    /// # Arguments
    /// * `enable` - Whether to acknowledge x86 float ABI behavior
    pub fn x86_float_abi_ok(mut self, enable: bool) -> Self {
        // Safety: x86_float_abi_ok is unsafe because it acknowledges non-standard
        // float ABI behavior on x86; the caller accepts responsibility for this.
        unsafe {
            self.config.x86_float_abi_ok(enable);
        }
        self.x86_float_abi_ok = enable;
        self
    }

    /// Configure record/replay execution tracing
    ///
    /// When recording or replaying are enabled, the engine captures or replays
    /// execution traces for deterministic debugging. Requires NaN canonicalization
    /// and deterministic relaxed SIMD to be enabled.
    ///
    /// # Arguments
    /// * `rr_config_value` - 0 = None, 1 = Recording, 2 = Replaying
    #[cfg(feature = "rr")]
    pub fn rr(mut self, rr_config_value: i32) -> Self {
        let rr = match rr_config_value {
            1 => wasmtime::RRConfig::Recording,
            2 => wasmtime::RRConfig::Replaying,
            _ => wasmtime::RRConfig::None,
        };
        self.config.rr(rr.clone());
        self.rr_config = rr;
        self
    }

    /// Configure signals-based trap handling
    ///
    /// **WARNING**: This is always overridden to `false` for JVM safety.
    /// Signal-based traps conflict with the JVM's own signal handlers
    /// and would cause SIGABRT / JVM crashes. This method exists solely
    /// for API completeness.
    ///
    /// # Arguments
    /// * `_enable` - Ignored; always set to `false`
    pub fn signals_based_traps(mut self, _enable: bool) -> Self {
        // Always keep signals_based_traps disabled for JVM safety
        // The safe_wasmtime_config() already sets this to false
        self
    }

    /// Set the compilation target triple for cross-compilation
    ///
    /// This configures the target architecture for code generation, allowing
    /// cross-compilation of WebAssembly modules for different platforms.
    ///
    /// # Arguments
    /// * `triple` - The target triple string (e.g., "x86_64-unknown-linux-gnu")
    pub fn target(mut self, triple: &str) -> Self {
        let _ = self.config.target(triple);
        self
    }

    /// Set the guaranteed dense image size for linear memories
    ///
    /// This configures the size of the dense image used for memory initialization.
    /// Modules with data segments that fit within this size will use a dense
    /// memory initialization strategy, which is faster.
    ///
    /// # Arguments
    /// * `size` - The guaranteed dense image size in bytes
    pub fn memory_guaranteed_dense_image_size(mut self, size: u64) -> Self {
        self.config.memory_guaranteed_dense_image_size(size);
        self
    }

    /// Load default cache configuration
    ///
    /// Enables compilation caching using the default cache directory. This
    /// avoids recompiling modules that have already been compiled by a
    /// previous engine instance, significantly improving startup time.
    ///
    /// The default cache location is platform-specific and managed by Wasmtime.
    pub fn cache_config_load_default(mut self) -> WasmtimeResult<Self> {
        let cache = wasmtime::Cache::from_file(None).map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Failed to load default cache config: {}", e),
        })?;
        self.config.cache(Some(cache));
        Ok(self)
    }

    /// Load cache configuration from a file
    ///
    /// Enables compilation caching using the specified configuration file.
    /// The configuration file specifies the cache directory, eviction policy,
    /// and other caching parameters.
    ///
    /// # Arguments
    /// * `path` - Path to the cache configuration TOML file
    pub fn cache_config_load(mut self, path: &str) -> WasmtimeResult<Self> {
        let cache = wasmtime::Cache::from_file(Some(std::path::Path::new(path))).map_err(|e| {
            WasmtimeError::EngineConfig {
                message: format!("Failed to load cache config from '{}': {}", path, e),
            }
        })?;
        self.config.cache(Some(cache));
        Ok(self)
    }

    /// Set the directory path for Cranelift IR (CLIF) output
    ///
    /// When set, compiled Cranelift intermediate representation files will be
    /// written to the specified directory during compilation. This is useful for
    /// debugging compilation issues or inspecting generated code.
    ///
    /// # Arguments
    /// * `path` - Directory path where CLIF files will be written
    pub fn emit_clif(mut self, path: &str) -> Self {
        self.config.emit_clif(std::path::Path::new(path));
        self
    }

    /// Enable or disable guest debugging instrumentation
    ///
    /// When enabled, compiled code includes extra instrumentation to support
    /// debugging guest WebAssembly code. This enables features like breakpoints,
    /// single-stepping, and frame inspection.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable guest debugging (default: false)
    pub fn guest_debug(mut self, enable: bool) -> Self {
        self.config.guest_debug(enable);
        self
    }

    /// Enable a Cranelift boolean flag by name
    ///
    /// This is the single-flag variant that sets the flag value to "true".
    /// For example: `cranelift_flag_enable("is_pic")`.
    ///
    /// # Arguments
    /// * `name` - The flag name to enable
    pub fn cranelift_flag_enable(mut self, name: &str) -> Self {
        unsafe {
            self.config.cranelift_flag_enable(name);
        }
        self
    }

    /// Enable incremental compilation with a callback-based cache store
    ///
    /// This enables Cranelift's incremental compilation cache using the provided
    /// CacheStore implementation. The cache store is called during compilation
    /// to retrieve and store intermediate compilation artifacts.
    ///
    /// # Arguments
    /// * `cache_store` - An Arc-wrapped CacheStore implementation
    pub fn enable_incremental_compilation(
        mut self,
        cache_store: Arc<dyn wasmtime::CacheStore>,
    ) -> WasmtimeResult<Self> {
        self.config
            .enable_incremental_compilation(cache_store)
            .map_err(|e| WasmtimeError::EngineConfig {
                message: format!("Failed to enable incremental compilation: {}", e),
            })?;
        Ok(self)
    }

    /// Set a custom memory creator for linear memory allocation
    pub fn with_host_memory(mut self, memory_creator: Arc<dyn wasmtime::MemoryCreator>) -> Self {
        self.config.with_host_memory(memory_creator);
        self
    }

    /// Set a custom stack creator for async fiber stacks
    pub fn with_host_stack(mut self, stack_creator: Arc<dyn wasmtime::StackCreator>) -> Self {
        self.config.with_host_stack(stack_creator);
        self
    }

    /// Set a custom code memory manager
    pub fn with_custom_code_memory(
        mut self,
        code_memory: Arc<dyn wasmtime::CustomCodeMemory>,
    ) -> Self {
        self.config.with_custom_code_memory(Some(code_memory));
        self
    }

    /// Build engine with current configuration
    pub fn build(self) -> WasmtimeResult<Engine> {
        let summary = EngineConfigSummary::from_builder(&self);

        let engine =
            WasmtimeEngine::new(&self.config).map_err(|e| WasmtimeError::EngineConfig {
                message: format!("Failed to create Wasmtime engine: {}", e),
            })?;

        Ok(Engine {
            inner: Arc::new(engine),
            config_summary: summary,
            concurrent_ops_lock: Arc::new(RwLock::new(())),
            is_closed: Arc::new(AtomicBool::new(false)),
        })
    }
}
