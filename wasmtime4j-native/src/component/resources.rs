//! Resource management for WebAssembly Component Model
//!
//! This module provides resource tracking, cleanup, and lifecycle management
//! for component instances.

use crate::error::WasmtimeResult;
use std::collections::HashMap;
use std::sync::{Arc, Weak};
use wasmtime::component::Instance as ComponentInstance;

/// Resource manager for automatic component cleanup
///
/// Tracks component instances and ensures proper cleanup when components
/// are no longer referenced. Prevents resource leaks and dangling references.
pub struct ResourceManager {
    /// Weak references to active instances
    instances: HashMap<u64, Weak<ComponentInstance>>,
    /// Cleanup callbacks for each instance
    cleanup_callbacks: HashMap<u64, Box<dyn FnOnce() + Send>>,
}

impl ResourceManager {
    /// Create a new resource manager
    ///
    /// # Returns
    ///
    /// Returns a new `ResourceManager` ready to track component instances.
    pub fn new() -> Self {
        ResourceManager {
            instances: HashMap::new(),
            cleanup_callbacks: HashMap::new(),
        }
    }

    /// Register a component instance for tracking
    ///
    /// # Arguments
    ///
    /// * `instance_id` - Unique identifier for the instance
    /// * `instance` - Weak reference to the instance
    /// * `cleanup_callback` - Optional cleanup callback to execute when instance is dropped
    ///
    /// # Returns
    ///
    /// Returns `Ok(())` if the instance was successfully registered.
    pub fn register_instance<F>(
        &mut self,
        instance_id: u64,
        instance: Weak<ComponentInstance>,
        cleanup_callback: Option<F>,
    ) -> WasmtimeResult<()>
    where
        F: FnOnce() + Send + 'static,
    {
        self.instances.insert(instance_id, instance);

        if let Some(callback) = cleanup_callback {
            self.cleanup_callbacks
                .insert(instance_id, Box::new(callback));
        }

        Ok(())
    }

    /// Cleanup all inactive instances
    ///
    /// # Returns
    ///
    /// Returns the number of instances that were cleaned up.
    pub fn cleanup_inactive(&mut self) -> usize {
        let mut cleaned_up = 0;
        let mut to_remove = Vec::new();

        for (id, weak_ref) in &self.instances {
            if weak_ref.strong_count() == 0 {
                to_remove.push(*id);
            }
        }

        for id in to_remove {
            self.instances.remove(&id);

            if let Some(callback) = self.cleanup_callbacks.remove(&id) {
                callback();
            }

            cleaned_up += 1;
        }

        cleaned_up
    }

    /// Get count of active instances
    ///
    /// # Returns
    ///
    /// Returns the number of currently active component instances.
    pub fn active_count(&self) -> usize {
        self.instances
            .iter()
            .filter(|(_, weak_ref)| weak_ref.strong_count() > 0)
            .count()
    }
}

impl Default for ResourceManager {
    fn default() -> Self {
        Self::new()
    }
}

/// Host interface implementation placeholder
///
/// This struct will be expanded to support actual host interface implementations
/// as the Wasmtime component model API develops.
pub struct HostInterface {
    /// Interface name
    pub name: String,
    /// Interface implementation (placeholder)
    pub implementation: Box<dyn std::any::Any + Send + Sync>,
}

/// Information about an active component instance
#[derive(Debug, Clone)]
pub struct InstanceInfo {
    /// Unique instance identifier
    pub instance_id: u64,
    /// Number of strong references to this instance
    pub strong_references: usize,
}

/// Component instance wrapper for FFI operations
///
/// Wraps a Wasmtime component instance with additional metadata for safe FFI operations.
pub struct ComponentInstanceWrapper {
    /// The actual component instance
    pub instance: Arc<ComponentInstance>,
    /// Instance metadata
    pub metadata: ComponentInstanceMetadata,
}

/// Metadata for component instances
#[derive(Debug, Clone)]
pub struct ComponentInstanceMetadata {
    /// Instance ID
    pub instance_id: u64,
    /// Creation timestamp
    pub created_at: std::time::SystemTime,
    /// Instance state
    pub state: ComponentInstanceState,
}

/// Component instance state
#[derive(Debug, Clone, PartialEq)]
pub enum ComponentInstanceState {
    /// Instance is being created
    Creating,
    /// Instance is active and ready for use
    Active,
    /// Instance is being disposed
    Disposing,
    /// Instance has been disposed
    Disposed,
}
