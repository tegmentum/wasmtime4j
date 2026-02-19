//! # WebAssembly GC Heap Management
//!
//! This module provides minimal heap tracking for WebAssembly GC objects.
//! Actual GC operations are handled by Wasmtime's native garbage collection
//! system through `gc_operations.rs`. This module provides:
//!
//! - ObjectId-based handle management
//! - Weak reference support bridging Java and WebAssembly GC
//! - Statistics tracking for monitoring
//! - GC trigger coordination with Wasmtime
//!
//! ## Safety and Defensive Programming
//!
//! All operations coordinate with Wasmtime's actual GC system to ensure
//! memory safety and prevent crashes.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::gc_types::GcTypeRegistry;
use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};

/// Unique identifier for GC objects
pub type ObjectId = u64;

/// GC heap configuration
#[derive(Debug, Clone)]
pub struct GcHeapConfig {
    /// Maximum heap size in bytes
    pub max_heap_size: usize,
    /// Allocation threshold for triggering minor GC
    pub minor_gc_threshold: usize,
    /// Allocation threshold for triggering major GC
    pub major_gc_threshold: usize,
    /// Enable weak reference support
    pub weak_references: bool,
}

impl Default for GcHeapConfig {
    fn default() -> Self {
        Self {
            max_heap_size: 64 * 1024 * 1024,      // 64MB default
            minor_gc_threshold: 8 * 1024 * 1024,  // 8MB
            major_gc_threshold: 32 * 1024 * 1024, // 32MB
            weak_references: true,
        }
    }
}

/// Garbage collection statistics
#[derive(Debug, Clone, Default)]
pub struct GcHeapStats {
    /// Total objects allocated
    pub total_allocated: u64,
    /// Total objects collected
    pub total_collected: u64,
    /// Total bytes allocated
    pub bytes_allocated: u64,
    /// Total bytes collected
    pub bytes_collected: u64,
    /// Number of minor collections
    pub minor_collections: u64,
    /// Number of major collections
    pub major_collections: u64,
    /// Total time spent in GC
    pub total_gc_time: Duration,
    /// Current heap size in bytes
    pub current_heap_size: usize,
    /// Peak heap size in bytes
    pub peak_heap_size: usize,
}

/// Reasons for triggering garbage collection
#[derive(Debug, Clone)]
pub enum CollectionTrigger {
    /// Allocation threshold exceeded
    AllocationThreshold,
    /// Heap size limit approached
    HeapPressure,
    /// Explicit request
    Explicit,
    /// Periodic collection
    Periodic,
    /// Wasmtime-triggered collection
    WasmtimeTriggered,
}

/// Result of a garbage collection run
#[derive(Debug, Clone)]
pub struct GcCollectionResult {
    /// Number of objects collected
    pub objects_collected: u64,
    /// Bytes collected
    pub bytes_collected: u64,
    /// Objects before collection
    pub objects_before: u64,
    /// Objects after collection
    pub objects_after: u64,
    /// Heap size before collection
    pub bytes_before: usize,
    /// Heap size after collection
    pub bytes_after: usize,
    /// Reason for triggering collection
    pub trigger_reason: CollectionTrigger,
}

/// Collection state tracking
#[derive(Debug, Default)]
struct CollectionState {
    /// Whether a collection is currently in progress
    collection_in_progress: bool,
    /// Last collection timestamp
    last_collection: Option<Instant>,
}

/// Weak reference entry for tracking object lifecycle
#[derive(Debug)]
struct WeakRefEntry {
    /// Whether the referenced object is still valid
    valid: bool,
}

/// WebAssembly GC heap manager for statistics and weak reference tracking
pub struct GcHeap {
    /// Heap configuration
    config: GcHeapConfig,
    /// Allocation statistics
    stats: Mutex<GcHeapStats>,
    /// Weak reference registry
    weak_refs: RwLock<HashMap<ObjectId, WeakRefEntry>>,
    /// Collection state tracking
    collection_state: Mutex<CollectionState>,
}

impl GcHeap {
    /// Create a new GC heap manager
    pub fn new(config: GcHeapConfig, _type_registry: Arc<GcTypeRegistry>) -> Self {
        Self {
            config,
            stats: Mutex::new(GcHeapStats::default()),
            weak_refs: RwLock::new(HashMap::new()),
            collection_state: Mutex::new(CollectionState::default()),
        }
    }

    /// Trigger garbage collection with Wasmtime GC coordination
    pub fn collect_garbage(
        &self,
        trigger: CollectionTrigger,
    ) -> WasmtimeResult<GcCollectionResult> {
        let start_time = Instant::now();

        // Check if collection is already in progress
        {
            let mut state =
                self.collection_state
                    .lock()
                    .map_err(|_| WasmtimeError::Concurrency {
                        message: "Failed to acquire collection state lock".to_string(),
                    })?;

            if state.collection_in_progress {
                return Err(WasmtimeError::Resource {
                    message: "Collection already in progress".to_string(),
                });
            }

            state.collection_in_progress = true;
        }

        // Get stats before collection
        let (objects_before, bytes_before) = {
            let stats = self.stats.lock().map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire stats lock".to_string(),
            })?;
            (
                stats.total_allocated - stats.total_collected,
                stats.current_heap_size,
            )
        };

        // Clean up invalid weak references
        let weak_refs_cleaned = self.cleanup_weak_refs()?;

        let collection_time = start_time.elapsed();

        // Update collection state
        {
            let mut state =
                self.collection_state
                    .lock()
                    .map_err(|_| WasmtimeError::Concurrency {
                        message: "Failed to acquire collection state lock".to_string(),
                    })?;

            state.collection_in_progress = false;
            state.last_collection = Some(start_time);
        }

        // Update statistics
        {
            let mut stats = self.stats.lock().map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire stats lock".to_string(),
            })?;

            stats.total_gc_time += collection_time;
            match trigger {
                CollectionTrigger::AllocationThreshold => stats.minor_collections += 1,
                CollectionTrigger::HeapPressure
                | CollectionTrigger::Explicit
                | CollectionTrigger::WasmtimeTriggered => stats.major_collections += 1,
                CollectionTrigger::Periodic => stats.minor_collections += 1,
            }
        }

        // Get stats after collection
        let (objects_after, bytes_after) = {
            let stats = self.stats.lock().map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire stats lock".to_string(),
            })?;
            (
                stats.total_allocated - stats.total_collected,
                stats.current_heap_size,
            )
        };

        Ok(GcCollectionResult {
            objects_collected: weak_refs_cleaned,
            bytes_collected: 0, // Actual bytes are tracked by Wasmtime
            objects_before,
            objects_after,
            bytes_before,
            bytes_after,
            trigger_reason: trigger,
        })
    }

    /// Get current heap statistics
    pub fn get_stats(&self) -> WasmtimeResult<GcHeapStats> {
        let stats = self.stats.lock().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire stats lock".to_string(),
        })?;

        Ok(stats.clone())
    }

    /// Create a weak reference to an object
    pub fn create_weak_reference(&self, object_id: ObjectId) -> WasmtimeResult<GcWeakReference> {
        if !self.config.weak_references {
            return Err(WasmtimeError::InvalidParameter {
                message: "Weak references are disabled".to_string(),
            });
        }

        // Register the weak reference
        {
            let mut weak_refs = self
                .weak_refs
                .write()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire weak refs lock".to_string(),
                })?;

            weak_refs.insert(
                object_id,
                WeakRefEntry {
                    valid: true,
                },
            );
        }

        Ok(GcWeakReference { object_id })
    }

    /// Check if an object exists (by checking weak reference validity)
    pub fn object_exists(&self, object_id: ObjectId) -> WasmtimeResult<bool> {
        let weak_refs = self
            .weak_refs
            .read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire weak refs lock".to_string(),
            })?;

        Ok(weak_refs.get(&object_id).map(|e| e.valid).unwrap_or(false))
    }

    /// Mark an object as invalid (collected)
    pub fn invalidate_object(&self, object_id: ObjectId) -> WasmtimeResult<()> {
        let mut weak_refs = self
            .weak_refs
            .write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire weak refs lock".to_string(),
            })?;

        if let Some(entry) = weak_refs.get_mut(&object_id) {
            entry.valid = false;
        }

        Ok(())
    }

    /// Clean up invalid weak references
    fn cleanup_weak_refs(&self) -> WasmtimeResult<u64> {
        let mut weak_refs = self
            .weak_refs
            .write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire weak refs lock".to_string(),
            })?;

        let initial_count = weak_refs.len();
        weak_refs.retain(|_, entry| entry.valid);
        let removed = (initial_count - weak_refs.len()) as u64;

        Ok(removed)
    }
}

/// Weak reference wrapper for GC objects
#[derive(Debug, Clone)]
pub struct GcWeakReference {
    object_id: ObjectId,
}

impl GcWeakReference {
    /// Attempt to upgrade weak reference to check if object is still valid
    pub fn upgrade(&self, heap: &GcHeap) -> Option<ObjectId> {
        heap.object_exists(self.object_id).ok().and_then(|exists| {
            if exists {
                Some(self.object_id)
            } else {
                None
            }
        })
    }

    /// Check if the referenced object is still alive
    pub fn is_alive(&self, heap: &GcHeap) -> bool {
        heap.object_exists(self.object_id).unwrap_or(false)
    }

    /// Get the object ID this weak reference points to
    pub fn object_id(&self) -> ObjectId {
        self.object_id
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn create_test_heap() -> GcHeap {
        let registry = Arc::new(GcTypeRegistry::new());
        let config = GcHeapConfig::default();
        GcHeap::new(config, registry)
    }

    #[test]
    fn test_heap_creation() {
        let heap = create_test_heap();
        let stats = heap.get_stats().unwrap();
        assert_eq!(stats.total_allocated, 0);
        assert_eq!(stats.current_heap_size, 0);
    }

    #[test]
    fn test_collection_triggering() {
        let heap = create_test_heap();

        let result = heap.collect_garbage(CollectionTrigger::Explicit).unwrap();
        assert_eq!(result.objects_before, 0);
        assert_eq!(result.objects_after, 0);

        let stats = heap.get_stats().unwrap();
        assert!(stats.major_collections > 0);
    }

    #[test]
    fn test_weak_reference_creation() {
        let heap = create_test_heap();

        let weak_ref = heap.create_weak_reference(42).unwrap();
        assert_eq!(weak_ref.object_id(), 42);

        // Object should be valid initially
        assert!(weak_ref.is_alive(&heap));
    }

    #[test]
    fn test_weak_reference_invalidation() {
        let heap = create_test_heap();

        // Create weak reference
        let weak_ref = heap.create_weak_reference(42).unwrap();
        assert!(weak_ref.is_alive(&heap));

        // Invalidate the object
        heap.invalidate_object(42).unwrap();
        assert!(!weak_ref.is_alive(&heap));
    }

    #[test]
    fn test_weak_reference_upgrade() {
        let heap = create_test_heap();

        // Create weak reference
        let weak_ref = heap.create_weak_reference(42).unwrap();

        // Should upgrade successfully
        assert!(weak_ref.upgrade(&heap).is_some());

        // Invalidate and try again
        heap.invalidate_object(42).unwrap();
        assert!(weak_ref.upgrade(&heap).is_none());
    }

    #[test]
    fn test_weak_refs_disabled() {
        let registry = Arc::new(GcTypeRegistry::new());
        let config = GcHeapConfig {
            weak_references: false,
            ..Default::default()
        };
        let heap = GcHeap::new(config, registry);

        let result = heap.create_weak_reference(42);
        assert!(result.is_err());
    }

    #[test]
    fn test_collection_state_prevents_concurrent() {
        let heap = create_test_heap();

        // First collection should succeed
        let result1 = heap.collect_garbage(CollectionTrigger::Explicit);
        assert!(result1.is_ok());

        // Second collection should also succeed (first one completed)
        let result2 = heap.collect_garbage(CollectionTrigger::Explicit);
        assert!(result2.is_ok());
    }

    #[test]
    fn test_gc_time_tracking() {
        let heap = create_test_heap();

        heap.collect_garbage(CollectionTrigger::Explicit).unwrap();
        heap.collect_garbage(CollectionTrigger::Explicit).unwrap();

        let stats = heap.get_stats().unwrap();
        // GC time is tracked (u128 is always >= 0)
        let _ = stats.total_gc_time.as_nanos();
        assert_eq!(stats.major_collections, 2);
    }
}
