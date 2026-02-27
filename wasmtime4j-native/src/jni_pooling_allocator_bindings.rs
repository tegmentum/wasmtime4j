//! JNI bindings for JniPoolingAllocator
//!
//! This module provides JNI bindings that wrap wasmtime's PoolingAllocationConfig
//! and InstanceAllocationStrategy with custom instance tracking for statistics.

use jni::objects::JClass;
use jni::sys::{jboolean, jfloat, jint, jlong, jlongArray, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::RwLock;
use std::time::Instant;
use wasmtime::{InstanceAllocationStrategy, PoolingAllocationConfig};

/// Instance state for tracking
#[derive(Clone, Copy, PartialEq, Eq)]
enum InstanceState {
    /// Instance is currently in use
    Active,
    /// Instance has been released and is available for reuse
    Released,
}

/// Wrapper around wasmtime's pooling allocator configuration with instance tracking
pub struct JniPoolingAllocatorWrapper {
    /// The wasmtime pooling allocation config
    config: PoolingAllocationConfig,
    /// Whether the allocator is closed
    closed: AtomicBool,
    /// Custom instance tracking - maps instance ID to (allocation time, state)
    instances: RwLock<HashMap<u64, (Instant, InstanceState)>>,
    /// Next instance ID
    next_instance_id: AtomicU64,
    /// Statistics
    stats: RwLock<PoolingStatistics>,
    /// Configuration parameters for reference
    instance_pool_size: u32,
    max_memory_per_instance: usize,
    pool_warming_enabled: bool,
    pool_warming_percentage: f32,
}

/// Statistics for the pooling allocator
#[derive(Default, Clone)]
struct PoolingStatistics {
    instances_allocated: u64,
    instances_reused: u64,
    instances_created: u64,
    memory_pools_allocated: u64,
    memory_pools_reused: u64,
    stack_pools_allocated: u64,
    stack_pools_reused: u64,
    table_pools_allocated: u64,
    table_pools_reused: u64,
    peak_memory_usage: u64,
    current_memory_usage: u64,
    allocation_failures: u64,
    pool_warming_time_nanos: u64,
    total_allocation_time_nanos: u64,
    allocation_count: u64,
}

impl PoolingStatistics {
    fn average_allocation_time_nanos(&self) -> u64 {
        if self.allocation_count == 0 {
            0
        } else {
            self.total_allocation_time_nanos / self.allocation_count
        }
    }
}

impl JniPoolingAllocatorWrapper {
    /// Create a new pooling allocator wrapper with the given configuration
    pub fn new(
        instance_pool_size: u32,
        max_memory_per_instance: usize,
        _stack_size: usize,
        max_stacks: u32,
        max_tables_per_instance: u32,
        max_tables_per_component: u32,
        max_tables: u32,
        memory_decommit_enabled: bool,
        pool_warming_enabled: bool,
        pool_warming_percentage: f32,
    ) -> Result<Self, String> {
        // Create wasmtime's pooling allocation config
        let mut config = PoolingAllocationConfig::new();

        // Configure the pool using wasmtime's API
        config.total_core_instances(instance_pool_size);
        config.max_memory_size(max_memory_per_instance);
        config.total_stacks(max_stacks);
        config.total_tables(max_tables);
        config.max_tables_per_module(max_tables_per_instance);
        config.max_tables_per_component(max_tables_per_component);

        // Configure memory behavior
        if memory_decommit_enabled {
            // Keep less memory resident when decommit is enabled
            config.table_keep_resident(0);
        }

        // Configure pool warming via max_unused_warm_slots
        if pool_warming_enabled {
            let warm_slots = ((instance_pool_size as f32) * pool_warming_percentage) as u32;
            config.max_unused_warm_slots(warm_slots.max(1));
        }

        Ok(Self {
            config,
            closed: AtomicBool::new(false),
            instances: RwLock::new(HashMap::new()),
            next_instance_id: AtomicU64::new(1),
            stats: RwLock::new(PoolingStatistics::default()),
            instance_pool_size,
            max_memory_per_instance,
            pool_warming_enabled,
            pool_warming_percentage,
        })
    }

    /// Get the allocation strategy for engine configuration
    pub fn get_allocation_strategy(&self) -> InstanceAllocationStrategy {
        InstanceAllocationStrategy::Pooling(self.config.clone())
    }

    /// Allocate a new instance slot from the pool
    pub fn allocate_instance(&self) -> Result<u64, String> {
        if self.closed.load(Ordering::Acquire) {
            return Err("Allocator is closed".to_string());
        }

        let start = Instant::now();
        let instance_id = self.next_instance_id.fetch_add(1, Ordering::Relaxed);

        // Track the instance as active
        {
            let mut instances = self.instances.write().map_err(|e| e.to_string())?;
            instances.insert(instance_id, (Instant::now(), InstanceState::Active));
        }

        // Update statistics
        {
            let mut stats = self.stats.write().map_err(|e| e.to_string())?;
            stats.instances_allocated += 1;
            stats.instances_created += 1;
            stats.memory_pools_allocated += 1;
            stats.stack_pools_allocated += 1;
            stats.table_pools_allocated += 1;
            stats.current_memory_usage += self.max_memory_per_instance as u64;
            if stats.current_memory_usage > stats.peak_memory_usage {
                stats.peak_memory_usage = stats.current_memory_usage;
            }
            stats.total_allocation_time_nanos += start.elapsed().as_nanos() as u64;
            stats.allocation_count += 1;
        }

        Ok(instance_id)
    }

    /// Reuse an existing instance slot that was previously released
    pub fn reuse_instance(&self, instance_id: u64) -> Result<bool, String> {
        if self.closed.load(Ordering::Acquire) {
            return Err("Allocator is closed".to_string());
        }

        // Check if instance exists and is in Released state
        {
            let mut instances = self.instances.write().map_err(|e| e.to_string())?;
            match instances.get_mut(&instance_id) {
                Some((_, state)) if *state == InstanceState::Released => {
                    // Mark as active again
                    *state = InstanceState::Active;
                }
                _ => return Ok(false),
            }
        }

        // Update statistics for reuse
        {
            let mut stats = self.stats.write().map_err(|e| e.to_string())?;
            stats.instances_reused += 1;
            stats.memory_pools_reused += 1;
            stats.stack_pools_reused += 1;
            stats.table_pools_reused += 1;
            stats.current_memory_usage += self.max_memory_per_instance as u64;
            if stats.current_memory_usage > stats.peak_memory_usage {
                stats.peak_memory_usage = stats.current_memory_usage;
            }
        }

        Ok(true)
    }

    /// Release an instance slot back to the pool
    pub fn release_instance(&self, instance_id: u64) -> Result<bool, String> {
        if self.closed.load(Ordering::Acquire) {
            return Err("Allocator is closed".to_string());
        }

        // Check if instance exists and is Active, then mark as Released
        {
            let mut instances = self.instances.write().map_err(|e| e.to_string())?;
            match instances.get_mut(&instance_id) {
                Some((_, state)) if *state == InstanceState::Active => {
                    *state = InstanceState::Released;
                }
                _ => return Ok(false),
            }
        }

        // Update statistics
        {
            let mut stats = self.stats.write().map_err(|e| e.to_string())?;
            if stats.current_memory_usage >= self.max_memory_per_instance as u64 {
                stats.current_memory_usage -= self.max_memory_per_instance as u64;
            }
        }

        Ok(true)
    }

    /// Get current statistics as an array
    pub fn get_statistics(&self) -> Result<Vec<i64>, String> {
        let stats = self.stats.read().map_err(|e| e.to_string())?;
        Ok(vec![
            stats.instances_allocated as i64,
            stats.instances_reused as i64,
            stats.instances_created as i64,
            stats.memory_pools_allocated as i64,
            stats.memory_pools_reused as i64,
            stats.stack_pools_allocated as i64,
            stats.stack_pools_reused as i64,
            stats.table_pools_allocated as i64,
            stats.table_pools_reused as i64,
            stats.peak_memory_usage as i64,
            stats.current_memory_usage as i64,
            stats.allocation_failures as i64,
            stats.pool_warming_time_nanos as i64,
            stats.average_allocation_time_nanos() as i64,
        ])
    }

    /// Reset statistics
    pub fn reset_statistics(&self) -> Result<(), String> {
        let mut stats = self.stats.write().map_err(|e| e.to_string())?;
        *stats = PoolingStatistics::default();
        Ok(())
    }

    /// Warm the pools by pre-allocating resources
    pub fn warm_pools(&self) -> Result<(), String> {
        if self.closed.load(Ordering::Acquire) {
            return Err("Allocator is closed".to_string());
        }

        if !self.pool_warming_enabled {
            return Ok(());
        }

        let start = Instant::now();
        let warm_count = ((self.instance_pool_size as f32) * self.pool_warming_percentage) as u32;

        // Pre-allocate and immediately release instances to warm the pool
        for _ in 0..warm_count {
            let id = self.allocate_instance()?;
            self.release_instance(id)?;
        }

        // Update warming statistics
        {
            let mut stats = self.stats.write().map_err(|e| e.to_string())?;
            stats.pool_warming_time_nanos = start.elapsed().as_nanos() as u64;
        }

        Ok(())
    }

    /// Perform maintenance on the pool
    pub fn perform_maintenance(&self) -> Result<(), String> {
        if self.closed.load(Ordering::Acquire) {
            return Err("Allocator is closed".to_string());
        }

        // Count active instances for memory usage
        let instances = self.instances.read().map_err(|e| e.to_string())?;
        let active_count = instances
            .values()
            .filter(|(_, state)| *state == InstanceState::Active)
            .count();
        drop(instances);

        // Update memory usage estimate based on active instances
        {
            let mut stats = self.stats.write().map_err(|e| e.to_string())?;
            stats.current_memory_usage =
                (active_count as u64) * (self.max_memory_per_instance as u64);
        }

        Ok(())
    }

    /// Check if the allocator is closed
    pub fn is_closed(&self) -> bool {
        self.closed.load(Ordering::Acquire)
    }

    /// Close the allocator
    pub fn close(&self) {
        self.closed.store(true, Ordering::Release);
    }
}

// =============================================================================
// JNI Bindings
// =============================================================================

/// Create a new pooling allocator with configuration
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeCreateWithConfig(
    _env: JNIEnv,
    _class: JClass,
    instance_pool_size: jint,
    max_memory_per_instance: jlong,
    stack_size: jint,
    max_stacks: jint,
    max_tables_per_instance: jint,
    max_tables_per_component: jint,
    max_tables: jint,
    memory_decommit_enabled: jboolean,
    pool_warming_enabled: jboolean,
    pool_warming_percentage: jfloat,
) -> jlong {
    let wrapper = match JniPoolingAllocatorWrapper::new(
        instance_pool_size as u32,
        max_memory_per_instance as usize,
        stack_size as usize,
        max_stacks as u32,
        max_tables_per_instance as u32,
        max_tables_per_component as u32,
        max_tables as u32,
        memory_decommit_enabled != JNI_FALSE,
        pool_warming_enabled != JNI_FALSE,
        pool_warming_percentage,
    ) {
        Ok(w) => w,
        Err(_) => return 0,
    };

    Box::into_raw(Box::new(wrapper)) as jlong
}

/// Allocate an instance from the pool
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeAllocateInstance(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jlong {
    if allocator_handle == 0 {
        return -1;
    }

    let wrapper = unsafe { &*(allocator_handle as *const JniPoolingAllocatorWrapper) };

    match wrapper.allocate_instance() {
        Ok(id) => id as jlong,
        Err(_) => -1,
    }
}

/// Reuse an existing instance
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeReuseInstance(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
    instance_id: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let wrapper = unsafe { &*(allocator_handle as *const JniPoolingAllocatorWrapper) };

    match wrapper.reuse_instance(instance_id as u64) {
        Ok(true) => JNI_TRUE,
        _ => JNI_FALSE,
    }
}

/// Release an instance back to the pool
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeReleaseInstance(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
    instance_id: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let wrapper = unsafe { &*(allocator_handle as *const JniPoolingAllocatorWrapper) };

    match wrapper.release_instance(instance_id as u64) {
        Ok(true) => JNI_TRUE,
        _ => JNI_FALSE,
    }
}

/// Get statistics from the allocator
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeGetStatistics<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    allocator_handle: jlong,
) -> jlongArray {
    if allocator_handle == 0 {
        return std::ptr::null_mut();
    }

    let wrapper = unsafe { &*(allocator_handle as *const JniPoolingAllocatorWrapper) };

    let stats = match wrapper.get_statistics() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    let result = match env.new_long_array(stats.len() as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    if env.set_long_array_region(&result, 0, &stats).is_err() {
        return std::ptr::null_mut();
    }

    result.into_raw()
}

/// Reset statistics
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeResetStatistics(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let wrapper = unsafe { &*(allocator_handle as *const JniPoolingAllocatorWrapper) };

    match wrapper.reset_statistics() {
        Ok(()) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

/// Warm the pools
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeWarmPools(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let wrapper = unsafe { &*(allocator_handle as *const JniPoolingAllocatorWrapper) };

    match wrapper.warm_pools() {
        Ok(()) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

/// Perform maintenance
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativePerformMaintenance(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let wrapper = unsafe { &*(allocator_handle as *const JniPoolingAllocatorWrapper) };

    match wrapper.perform_maintenance() {
        Ok(()) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

/// Destroy the allocator
#[unsafe(no_mangle)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) {
    if allocator_handle == 0 {
        return;
    }

    let wrapper = unsafe { Box::from_raw(allocator_handle as *mut JniPoolingAllocatorWrapper) };
    wrapper.close();
    drop(wrapper);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_create_pooling_allocator() {
        let wrapper = JniPoolingAllocatorWrapper::new(
            100,     // instance_pool_size
            1 << 20, // max_memory_per_instance (1MB)
            1 << 16, // stack_size (64KB)
            100,     // max_stacks
            10,      // max_tables_per_instance
            10,      // max_tables_per_component
            1000,    // max_tables
            true,    // memory_decommit_enabled
            true,    // pool_warming_enabled
            0.1,     // pool_warming_percentage
        );

        assert!(wrapper.is_ok());
        let wrapper = wrapper.unwrap();
        assert!(!wrapper.is_closed());
    }

    #[test]
    fn test_allocate_and_release_instance() {
        let wrapper = JniPoolingAllocatorWrapper::new(
            100,
            1 << 20,
            1 << 16,
            100,
            10,
            10,
            1000,
            true,
            false,
            0.0,
        )
        .unwrap();

        // Allocate an instance
        let id = wrapper.allocate_instance().unwrap();
        assert!(id > 0);

        // Check statistics
        let stats = wrapper.get_statistics().unwrap();
        assert_eq!(stats[0], 1); // instances_allocated
        assert_eq!(stats[2], 1); // instances_created

        // Release the instance
        let released = wrapper.release_instance(id).unwrap();
        assert!(released);

        // Try to release again - should fail
        let released_again = wrapper.release_instance(id).unwrap();
        assert!(!released_again);
    }

    #[test]
    fn test_reuse_instance() {
        let wrapper = JniPoolingAllocatorWrapper::new(
            100,
            1 << 20,
            1 << 16,
            100,
            10,
            10,
            1000,
            true,
            false,
            0.0,
        )
        .unwrap();

        // Allocate an instance
        let id = wrapper.allocate_instance().unwrap();

        // Cannot reuse an active instance - must release first
        let reused_active = wrapper.reuse_instance(id).unwrap();
        assert!(!reused_active);

        // Release the instance
        let released = wrapper.release_instance(id).unwrap();
        assert!(released);

        // Now reuse the released instance
        let reused = wrapper.reuse_instance(id).unwrap();
        assert!(reused);

        // Check statistics
        let stats = wrapper.get_statistics().unwrap();
        assert_eq!(stats[1], 1); // instances_reused

        // Try to reuse non-existent instance
        let reused_invalid = wrapper.reuse_instance(99999).unwrap();
        assert!(!reused_invalid);
    }

    #[test]
    fn test_warm_pools() {
        let wrapper = JniPoolingAllocatorWrapper::new(
            100,
            1 << 20,
            1 << 16,
            100,
            10,
            10,
            1000,
            true,
            true,
            0.1,
        )
        .unwrap();

        // Warm pools
        let result = wrapper.warm_pools();
        assert!(result.is_ok());

        // Check that warming time was recorded
        let stats = wrapper.get_statistics().unwrap();
        assert!(stats[12] > 0); // pool_warming_time_nanos
    }

    #[test]
    fn test_reset_statistics() {
        let wrapper = JniPoolingAllocatorWrapper::new(
            100,
            1 << 20,
            1 << 16,
            100,
            10,
            10,
            1000,
            true,
            false,
            0.0,
        )
        .unwrap();

        // Allocate some instances
        wrapper.allocate_instance().unwrap();
        wrapper.allocate_instance().unwrap();

        // Verify statistics
        let stats = wrapper.get_statistics().unwrap();
        assert_eq!(stats[0], 2);

        // Reset statistics
        wrapper.reset_statistics().unwrap();

        // Verify reset
        let stats = wrapper.get_statistics().unwrap();
        assert_eq!(stats[0], 0);
    }

    #[test]
    fn test_closed_allocator_rejects_operations() {
        let wrapper = JniPoolingAllocatorWrapper::new(
            100,
            1 << 20,
            1 << 16,
            100,
            10,
            10,
            1000,
            true,
            false,
            0.0,
        )
        .unwrap();

        // Close the allocator
        wrapper.close();
        assert!(wrapper.is_closed());

        // Try to allocate - should fail
        let result = wrapper.allocate_instance();
        assert!(result.is_err());
    }

    #[test]
    fn test_get_allocation_strategy() {
        let wrapper = JniPoolingAllocatorWrapper::new(
            100,
            1 << 20,
            1 << 16,
            100,
            10,
            10,
            1000,
            true,
            false,
            0.0,
        )
        .unwrap();

        let strategy = wrapper.get_allocation_strategy();
        match strategy {
            InstanceAllocationStrategy::Pooling(_) => (),
            _ => panic!("Expected pooling strategy"),
        }
    }
}
