//! WASI keyvalue helper functions for both JNI and Panama FFI bindings
//!
//! This module provides shared helper functions used by both the JNI and Panama FFI
//! implementations for WASI keyvalue operations. It integrates with the wasmtime-wasi-keyvalue
//! crate to provide key-value store functionality.
//!
//! The wasi-keyvalue interface provides a simple key-value store abstraction that
//! WebAssembly components can use to persist data.

use std::collections::HashMap;
use std::sync::{Arc, RwLock};

use crate::error::{WasmtimeError, WasmtimeResult};

/// Context for WASI keyvalue operations
///
/// This context wraps the wasmtime-wasi-keyvalue functionality and provides
/// a simple interface for key-value storage operations.
#[derive(Debug)]
pub struct WasiKeyValueContext {
    /// Context identifier
    context_id: u64,
    /// In-memory storage (default backend)
    storage: Arc<RwLock<HashMap<String, Vec<u8>>>>,
    /// Whether the context is valid
    valid: bool,
}

impl WasiKeyValueContext {
    /// Creates a new WASI keyvalue context
    pub fn new() -> WasmtimeResult<Self> {
        static CONTEXT_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

        Ok(Self {
            context_id: CONTEXT_ID.fetch_add(1, std::sync::atomic::Ordering::SeqCst),
            storage: Arc::new(RwLock::new(HashMap::new())),
            valid: true,
        })
    }

    /// Creates a new WASI keyvalue context with initial data
    pub fn with_data(data: HashMap<String, Vec<u8>>) -> WasmtimeResult<Self> {
        static CONTEXT_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

        Ok(Self {
            context_id: CONTEXT_ID.fetch_add(1, std::sync::atomic::Ordering::SeqCst),
            storage: Arc::new(RwLock::new(data)),
            valid: true,
        })
    }

    /// Gets the context ID
    pub fn id(&self) -> u64 {
        self.context_id
    }

    /// Checks if the context is valid
    pub fn is_valid(&self) -> bool {
        self.valid
    }

    /// Invalidates the context
    pub fn invalidate(&mut self) {
        self.valid = false;
    }

    /// Gets a value by key
    pub fn get(&self, key: &str) -> WasmtimeResult<Option<Vec<u8>>> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let storage = self.storage.read().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire read lock: {}", e),
        })?;

        Ok(storage.get(key).cloned())
    }

    /// Sets a value for a key
    pub fn set(&self, key: &str, value: Vec<u8>) -> WasmtimeResult<()> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let mut storage = self.storage.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire write lock: {}", e),
        })?;

        storage.insert(key.to_string(), value);
        Ok(())
    }

    /// Deletes a key
    pub fn delete(&self, key: &str) -> WasmtimeResult<bool> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let mut storage = self.storage.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire write lock: {}", e),
        })?;

        Ok(storage.remove(key).is_some())
    }

    /// Checks if a key exists
    pub fn exists(&self, key: &str) -> WasmtimeResult<bool> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let storage = self.storage.read().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire read lock: {}", e),
        })?;

        Ok(storage.contains_key(key))
    }

    /// Gets all keys
    pub fn keys(&self) -> WasmtimeResult<Vec<String>> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let storage = self.storage.read().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire read lock: {}", e),
        })?;

        Ok(storage.keys().cloned().collect())
    }

    /// Gets multiple values by keys
    pub fn get_many(&self, keys: &[String]) -> WasmtimeResult<HashMap<String, Vec<u8>>> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let storage = self.storage.read().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire read lock: {}", e),
        })?;

        let mut result = HashMap::new();
        for key in keys {
            if let Some(value) = storage.get(key) {
                result.insert(key.clone(), value.clone());
            }
        }
        Ok(result)
    }

    /// Sets multiple key-value pairs
    pub fn set_many(&self, entries: HashMap<String, Vec<u8>>) -> WasmtimeResult<()> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let mut storage = self.storage.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire write lock: {}", e),
        })?;

        for (key, value) in entries {
            storage.insert(key, value);
        }
        Ok(())
    }

    /// Deletes multiple keys
    pub fn delete_many(&self, keys: &[String]) -> WasmtimeResult<Vec<String>> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let mut storage = self.storage.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire write lock: {}", e),
        })?;

        let mut deleted = Vec::new();
        for key in keys {
            if storage.remove(key).is_some() {
                deleted.push(key.clone());
            }
        }
        Ok(deleted)
    }

    /// Atomically increments a numeric value
    pub fn increment(&self, key: &str, delta: i64) -> WasmtimeResult<i64> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let mut storage = self.storage.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire write lock: {}", e),
        })?;

        let current = storage.get(key).and_then(|v| {
            if v.len() == 8 {
                Some(i64::from_le_bytes(v.as_slice().try_into().unwrap_or([0; 8])))
            } else {
                None
            }
        }).unwrap_or(0);

        let new_value = current.saturating_add(delta);
        storage.insert(key.to_string(), new_value.to_le_bytes().to_vec());
        Ok(new_value)
    }

    /// Gets the number of entries in the store
    pub fn size(&self) -> WasmtimeResult<usize> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let storage = self.storage.read().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire read lock: {}", e),
        })?;

        Ok(storage.len())
    }

    /// Clears all entries from the store
    pub fn clear(&self) -> WasmtimeResult<()> {
        if !self.valid {
            return Err(WasmtimeError::Wasi {
                message: "Keyvalue context is no longer valid".to_string(),
            });
        }

        let mut storage = self.storage.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire write lock: {}", e),
        })?;

        storage.clear();
        Ok(())
    }
}

impl Default for WasiKeyValueContext {
    fn default() -> Self {
        Self::new().expect("Failed to create default WasiKeyValueContext")
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_create_context() {
        let ctx = WasiKeyValueContext::new().unwrap();
        assert!(ctx.is_valid());
        assert!(ctx.id() > 0);
    }

    #[test]
    fn test_get_set() {
        let ctx = WasiKeyValueContext::new().unwrap();

        // Initially empty
        assert!(ctx.get("key1").unwrap().is_none());

        // Set a value
        ctx.set("key1", b"value1".to_vec()).unwrap();

        // Get the value
        let value = ctx.get("key1").unwrap();
        assert_eq!(value, Some(b"value1".to_vec()));
    }

    #[test]
    fn test_delete() {
        let ctx = WasiKeyValueContext::new().unwrap();

        ctx.set("key1", b"value1".to_vec()).unwrap();
        assert!(ctx.exists("key1").unwrap());

        let deleted = ctx.delete("key1").unwrap();
        assert!(deleted);
        assert!(!ctx.exists("key1").unwrap());

        // Delete non-existent key
        let deleted = ctx.delete("nonexistent").unwrap();
        assert!(!deleted);
    }

    #[test]
    fn test_keys() {
        let ctx = WasiKeyValueContext::new().unwrap();

        ctx.set("key1", b"value1".to_vec()).unwrap();
        ctx.set("key2", b"value2".to_vec()).unwrap();
        ctx.set("key3", b"value3".to_vec()).unwrap();

        let mut keys = ctx.keys().unwrap();
        keys.sort();
        assert_eq!(keys, vec!["key1", "key2", "key3"]);
    }

    #[test]
    fn test_increment() {
        let ctx = WasiKeyValueContext::new().unwrap();

        // Increment non-existent key (starts at 0)
        let value = ctx.increment("counter", 5).unwrap();
        assert_eq!(value, 5);

        // Increment existing key
        let value = ctx.increment("counter", 3).unwrap();
        assert_eq!(value, 8);

        // Decrement
        let value = ctx.increment("counter", -2).unwrap();
        assert_eq!(value, 6);
    }

    #[test]
    fn test_batch_operations() {
        let ctx = WasiKeyValueContext::new().unwrap();

        // Set multiple
        let mut entries = HashMap::new();
        entries.insert("key1".to_string(), b"value1".to_vec());
        entries.insert("key2".to_string(), b"value2".to_vec());
        ctx.set_many(entries).unwrap();

        // Get multiple
        let result = ctx.get_many(&["key1".to_string(), "key2".to_string(), "key3".to_string()]).unwrap();
        assert_eq!(result.len(), 2);
        assert_eq!(result.get("key1"), Some(&b"value1".to_vec()));
        assert_eq!(result.get("key2"), Some(&b"value2".to_vec()));

        // Delete multiple
        let deleted = ctx.delete_many(&["key1".to_string(), "key3".to_string()]).unwrap();
        assert_eq!(deleted, vec!["key1"]);
    }

    #[test]
    fn test_size_and_clear() {
        let ctx = WasiKeyValueContext::new().unwrap();

        assert_eq!(ctx.size().unwrap(), 0);

        ctx.set("key1", b"value1".to_vec()).unwrap();
        ctx.set("key2", b"value2".to_vec()).unwrap();
        assert_eq!(ctx.size().unwrap(), 2);

        ctx.clear().unwrap();
        assert_eq!(ctx.size().unwrap(), 0);
    }

    #[test]
    fn test_with_initial_data() {
        let mut data = HashMap::new();
        data.insert("preset_key".to_string(), b"preset_value".to_vec());

        let ctx = WasiKeyValueContext::with_data(data).unwrap();

        let value = ctx.get("preset_key").unwrap();
        assert_eq!(value, Some(b"preset_value".to_vec()));
    }

    #[test]
    fn test_invalidated_context() {
        let mut ctx = WasiKeyValueContext::new().unwrap();
        ctx.invalidate();

        assert!(!ctx.is_valid());
        assert!(ctx.get("key").is_err());
        assert!(ctx.set("key", vec![]).is_err());
    }
}
