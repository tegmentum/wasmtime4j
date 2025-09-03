//! WebAssembly Table management with bounds checking and reference type support
//!
//! This module provides safe wrapper around wasmtime::Table for managing tables
//! in WebAssembly modules. Tables are used to store references to functions,
//! external objects, or other reference types with proper bounds checking
//! and type validation.

use std::sync::{Arc, Mutex};
use wasmtime::{Table as WasmtimeTable, TableType, Val, ValType, Func, Extern};
use crate::store::{Store, StoreData};
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::global::ReferenceType;

/// Thread-safe wrapper around Wasmtime table with bounds checking
pub struct Table {
    inner: Arc<Mutex<WasmtimeTable>>,
    metadata: TableMetadata,
}

/// Table metadata and configuration information
#[derive(Debug, Clone)]
pub struct TableMetadata {
    /// Element type stored in this table (funcref, externref, etc.)
    pub element_type: ValType,
    /// Initial size of the table
    pub initial_size: u32,
    /// Maximum size limit (None means unlimited)
    pub maximum_size: Option<u32>,
    /// Optional name for debugging purposes
    pub name: Option<String>,
}

/// Type-safe reference container for table elements
#[derive(Debug, Clone)]
pub enum TableElement {
    /// Function reference with function ID
    FuncRef(Option<u64>), // Function ID, None for null reference
    /// External reference with object ID
    ExternRef(Option<u64>), // External object ID, None for null reference
    /// Any reference type with generic ID
    AnyRef(Option<u64>), // Generic reference ID, None for null reference
}

impl Table {
    /// Create a new table with specified element type and size
    pub fn new(
        store: &Store,
        element_type: ValType,
        initial_size: u32,
        maximum_size: Option<u32>,
        name: Option<String>,
    ) -> WasmtimeResult<Self> {
        // Validate that element_type is a valid reference type
        Self::validate_element_type(&element_type)?;

        // Validate size constraints
        if let Some(max_size) = maximum_size {
            if initial_size > max_size {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Initial size {} exceeds maximum size {}",
                        initial_size, max_size
                    ),
                });
            }
        }

        let table_type = TableType::new(element_type.clone(), initial_size, maximum_size);
        let initial_value = Self::default_value_for_type(&element_type)?;

        let wasmtime_table = store.with_context(|mut ctx| {
            WasmtimeTable::new(&mut ctx, table_type, initial_value)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to create table: {}", e),
                    backtrace: None,
                })
        })?;

        let metadata = TableMetadata {
            element_type,
            initial_size,
            maximum_size,
            name,
        };

        Ok(Table {
            inner: Arc::new(Mutex::new(wasmtime_table)),
            metadata,
        })
    }

    /// Get the current size of the table
    pub fn size(&self, store: &Store) -> WasmtimeResult<u32> {
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        store.with_context_ro(|ctx| {
            Ok(table.size(&ctx))
        })
    }

    /// Get an element from the table at the specified index
    pub fn get(&self, store: &Store, index: u32) -> WasmtimeResult<TableElement> {
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        let table_size = store.with_context_ro(|ctx| {
            Ok(table.size(&ctx))
        })?;

        // Bounds check
        if index >= table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table index {} out of bounds (table size: {})",
                    index, table_size
                ),
                backtrace: None,
            });
        }

        let wasmtime_value = store.with_context_ro(|ctx| {
            table.get(&ctx, index)
                .ok_or_else(|| WasmtimeError::Runtime {
                    message: format!("Failed to get table element at index {}", index),
                    backtrace: None,
                })
        })?;

        Self::wasmtime_val_to_table_element(wasmtime_value, &self.metadata.element_type)
    }

    /// Set an element in the table at the specified index
    pub fn set(&self, store: &Store, index: u32, element: TableElement) -> WasmtimeResult<()> {
        // Validate that the element type matches the table's element type
        Self::validate_element_matches_type(&element, &self.metadata.element_type)?;

        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        let table_size = store.with_context_ro(|ctx| {
            Ok(table.size(&ctx))
        })?;

        // Bounds check
        if index >= table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table index {} out of bounds (table size: {})",
                    index, table_size
                ),
                backtrace: None,
            });
        }

        let wasmtime_value = Self::table_element_to_wasmtime_val(element)?;

        store.with_context(|mut ctx| {
            table.set(&mut ctx, index, wasmtime_value)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to set table element at index {}: {}", index, e),
                    backtrace: None,
                })
        })
    }

    /// Grow the table by the specified number of elements
    pub fn grow(&self, store: &Store, delta: u32, init_value: TableElement) -> WasmtimeResult<u32> {
        // Validate that the init_value type matches the table's element type
        Self::validate_element_matches_type(&init_value, &self.metadata.element_type)?;

        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        let current_size = store.with_context_ro(|ctx| {
            Ok(table.size(&ctx))
        })?;

        // Check against maximum size if specified
        if let Some(max_size) = self.metadata.maximum_size {
            if current_size.saturating_add(delta) > max_size {
                return Err(WasmtimeError::Runtime {
                    message: format!(
                        "Cannot grow table: would exceed maximum size {} (current: {}, delta: {})",
                        max_size, current_size, delta
                    ),
                    backtrace: None,
                });
            }
        }

        let wasmtime_init_value = Self::table_element_to_wasmtime_val(init_value)?;

        store.with_context(|mut ctx| {
            table.grow(&mut ctx, delta, wasmtime_init_value)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to grow table: {}", e),
                    backtrace: None,
                })
        })
    }

    /// Fill a range of table elements with the specified value
    pub fn fill(
        &self,
        store: &Store,
        dst: u32,
        value: TableElement,
        len: u32,
    ) -> WasmtimeResult<()> {
        // Validate that the value type matches the table's element type
        Self::validate_element_matches_type(&value, &self.metadata.element_type)?;

        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        let table_size = store.with_context_ro(|ctx| {
            Ok(table.size(&ctx))
        })?;

        // Bounds check
        if dst.saturating_add(len) > table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table fill would exceed bounds: dst={}, len={}, table_size={}",
                    dst, len, table_size
                ),
                backtrace: None,
            });
        }

        let wasmtime_value = Self::table_element_to_wasmtime_val(value)?;

        store.with_context(|mut ctx| {
            table.fill(&mut ctx, dst, wasmtime_value, len)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to fill table: {}", e),
                    backtrace: None,
                })
        })
    }

    /// Get table metadata
    pub fn metadata(&self) -> &TableMetadata {
        &self.metadata
    }

    /// Get table type information
    pub fn table_type(&self, store: &Store) -> WasmtimeResult<TableType> {
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        store.with_context_ro(|ctx| {
            Ok(table.ty(&ctx))
        })
    }

    /// Validate that a ValType is a valid reference type for table elements
    fn validate_element_type(element_type: &ValType) -> WasmtimeResult<()> {
        match element_type {
            ValType::FuncRef | ValType::ExternRef => Ok(()),
            _ => Err(WasmtimeError::Type {
                message: format!("Invalid table element type: {:?}. Only reference types are allowed.", element_type),
            }),
        }
    }

    /// Validate that a TableElement matches the expected ValType
    fn validate_element_matches_type(element: &TableElement, expected_type: &ValType) -> WasmtimeResult<()> {
        let matches = match (element, expected_type) {
            (TableElement::FuncRef(_), ValType::FuncRef) => true,
            (TableElement::ExternRef(_), ValType::ExternRef) => true,
            (TableElement::AnyRef(_), _) => true, // AnyRef should match any reference type
            _ => false,
        };

        if !matches {
            return Err(WasmtimeError::Type {
                message: format!(
                    "Table element type {:?} does not match expected type {:?}",
                    element, expected_type
                ),
            });
        }

        Ok(())
    }

    /// Get the default value for a given element type
    fn default_value_for_type(element_type: &ValType) -> WasmtimeResult<Val> {
        let default_val = match element_type {
            ValType::FuncRef => Val::FuncRef(None),
            ValType::ExternRef => Val::ExternRef(None),
            _ => return Err(WasmtimeError::Type {
                message: format!("No default value for non-reference type: {:?}", element_type),
            }),
        };

        Ok(default_val)
    }

    /// Convert TableElement to wasmtime::Val
    fn table_element_to_wasmtime_val(element: TableElement) -> WasmtimeResult<Val> {
        let wasmtime_val = match element {
            TableElement::FuncRef(_) => {
                // For now, we only support null function references
                // TODO: Implement proper function reference handling
                Val::FuncRef(None)
            },
            TableElement::ExternRef(_) => {
                // For now, we only support null external references
                // TODO: Implement proper external reference handling
                Val::ExternRef(None)
            },
            TableElement::AnyRef(_) => {
                // AnyRef is not directly supported by wasmtime::Val
                // Default to null function reference for now
                Val::FuncRef(None)
            },
        };

        Ok(wasmtime_val)
    }

    /// Convert wasmtime::Val to TableElement
    fn wasmtime_val_to_table_element(val: Val, element_type: &ValType) -> WasmtimeResult<TableElement> {
        let table_element = match (val, element_type) {
            (Val::FuncRef(func_ref), ValType::FuncRef) => {
                // TODO: Implement proper function reference ID mapping
                TableElement::FuncRef(func_ref.map(|_| 0))
            },
            (Val::ExternRef(extern_ref), ValType::ExternRef) => {
                // TODO: Implement proper external reference ID mapping
                TableElement::ExternRef(extern_ref.map(|_| 0))
            },
            _ => return Err(WasmtimeError::Type {
                message: format!("Cannot convert Val {:?} to TableElement for type {:?}", val, element_type),
            }),
        };

        Ok(table_element)
    }

    /// Create a table from an existing wasmtime::Table (for imports/exports)
    pub fn from_wasmtime_table(
        wasmtime_table: WasmtimeTable,
        store: &Store,
        name: Option<String>,
    ) -> WasmtimeResult<Self> {
        let table_type = store.with_context_ro(|ctx| {
            Ok(wasmtime_table.ty(&ctx))
        })?;

        let metadata = TableMetadata {
            element_type: table_type.element().clone(),
            initial_size: table_type.minimum(),
            maximum_size: table_type.maximum(),
            name,
        };

        Ok(Table {
            inner: Arc::new(Mutex::new(wasmtime_table)),
            metadata,
        })
    }

    /// Get the underlying wasmtime::Table (for internal use)
    pub fn wasmtime_table(&self) -> Arc<Mutex<WasmtimeTable>> {
        Arc::clone(&self.inner)
    }
}

// Thread safety: Table uses Arc<Mutex<WasmtimeTable>> internally
unsafe impl Send for Table {}
unsafe impl Sync for Table {}

/// Shared core functions for table operations used by both JNI and Panama interfaces
///
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::error::ffi_utils;
    use crate::validate_ptr_not_null;

    /// Core function to create a new table
    pub fn create_table(
        store: &Store,
        element_type: ValType,
        initial_size: u32,
        maximum_size: Option<u32>,
        name: Option<String>,
    ) -> WasmtimeResult<Box<Table>> {
        Table::new(store, element_type, initial_size, maximum_size, name).map(Box::new)
    }

    /// Core function to validate table pointer and get reference
    pub unsafe fn get_table_ref(table_ptr: *const c_void) -> WasmtimeResult<&'static Table> {
        validate_ptr_not_null!(table_ptr, "table");
        Ok(&*(table_ptr as *const Table))
    }

    /// Core function to validate table pointer and get mutable reference
    pub unsafe fn get_table_mut(table_ptr: *mut c_void) -> WasmtimeResult<&'static mut Table> {
        validate_ptr_not_null!(table_ptr, "table");
        Ok(&mut *(table_ptr as *mut Table))
    }

    /// Core function to get table size
    pub fn get_table_size(table: &Table, store: &Store) -> WasmtimeResult<u32> {
        table.size(store)
    }

    /// Core function to get table element
    pub fn get_table_element(table: &Table, store: &Store, index: u32) -> WasmtimeResult<TableElement> {
        table.get(store, index)
    }

    /// Core function to set table element
    pub fn set_table_element(
        table: &Table,
        store: &Store,
        index: u32,
        element: TableElement,
    ) -> WasmtimeResult<()> {
        table.set(store, index, element)
    }

    /// Core function to grow table
    pub fn grow_table(
        table: &Table,
        store: &Store,
        delta: u32,
        init_value: TableElement,
    ) -> WasmtimeResult<u32> {
        table.grow(store, delta, init_value)
    }

    /// Core function to fill table range
    pub fn fill_table(
        table: &Table,
        store: &Store,
        dst: u32,
        value: TableElement,
        len: u32,
    ) -> WasmtimeResult<()> {
        table.fill(store, dst, value, len)
    }

    /// Core function to get table metadata
    pub fn get_table_metadata(table: &Table) -> &TableMetadata {
        table.metadata()
    }

    /// Core function to get table type
    pub fn get_table_type(table: &Table, store: &Store) -> WasmtimeResult<TableType> {
        table.table_type(store)
    }

    /// Core function to destroy a table (safe cleanup)
    pub unsafe fn destroy_table(table_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Table>(table_ptr, "Table");
    }

    /// Helper function to create TableElement from raw components
    pub fn create_table_element(
        element_type: ValType,
        ref_id: Option<u64>,
    ) -> WasmtimeResult<TableElement> {
        let table_element = match element_type {
            ValType::FuncRef => TableElement::FuncRef(ref_id),
            ValType::ExternRef => TableElement::ExternRef(ref_id),
            _ => return Err(WasmtimeError::Type {
                message: format!("Invalid table element type: {:?}", element_type),
            }),
        };

        Ok(table_element)
    }

    /// Helper function to extract reference ID from TableElement
    pub fn extract_table_element_ref_id(element: &TableElement) -> Option<u64> {
        match element {
            TableElement::FuncRef(ref_id) => *ref_id,
            TableElement::ExternRef(ref_id) => *ref_id,
            TableElement::AnyRef(ref_id) => *ref_id,
        }
    }

    /// Helper function to get TableElement type
    pub fn get_table_element_type(element: &TableElement) -> ReferenceType {
        match element {
            TableElement::FuncRef(_) => ReferenceType::FuncRef,
            TableElement::ExternRef(_) => ReferenceType::ExternRef,
            TableElement::AnyRef(_) => ReferenceType::AnyRef,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;
    use crate::store::Store;

    #[test]
    fn test_table_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::FuncRef,
            10,
            Some(100),
            Some("test_table".to_string()),
        ).expect("Failed to create table");

        assert_eq!(table.metadata().element_type, ValType::FuncRef);
        assert_eq!(table.metadata().initial_size, 10);
        assert_eq!(table.metadata().maximum_size, Some(100));
        assert_eq!(table.metadata().name, Some("test_table".to_string()));
    }

    #[test]
    fn test_table_size() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::FuncRef,
            10,
            None,
            None,
        ).expect("Failed to create table");

        let size = table.size(&store).expect("Failed to get table size");
        assert_eq!(size, 10);
    }

    #[test]
    fn test_table_get_set() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::FuncRef,
            10,
            None,
            None,
        ).expect("Failed to create table");

        // Test getting initial value (should be null)
        let element = table.get(&store, 0).expect("Failed to get table element");
        match element {
            TableElement::FuncRef(ref_id) => assert_eq!(ref_id, None),
            _ => panic!("Expected FuncRef element"),
        }

        // Test setting new value
        table.set(&store, 0, TableElement::FuncRef(Some(42)))
            .expect("Failed to set table element");

        // Test getting updated value
        let element = table.get(&store, 0).expect("Failed to get table element");
        match element {
            TableElement::FuncRef(ref_id) => assert_eq!(ref_id, Some(0)), // Mapped to 0 for now
            _ => panic!("Expected FuncRef element"),
        }
    }

    #[test]
    fn test_table_bounds_checking() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::FuncRef,
            5,
            None,
            None,
        ).expect("Failed to create table");

        // Test accessing out of bounds element
        let result = table.get(&store, 10);
        assert!(result.is_err());

        // Test setting out of bounds element
        let result = table.set(&store, 10, TableElement::FuncRef(None));
        assert!(result.is_err());
    }

    #[test]
    fn test_table_grow() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::FuncRef,
            5,
            Some(20),
            None,
        ).expect("Failed to create table");

        let initial_size = table.size(&store).expect("Failed to get initial size");
        assert_eq!(initial_size, 5);

        // Test growing table
        let old_size = table.grow(&store, 3, TableElement::FuncRef(Some(123)))
            .expect("Failed to grow table");
        assert_eq!(old_size, 5);

        let new_size = table.size(&store).expect("Failed to get new size");
        assert_eq!(new_size, 8);
    }

    #[test]
    fn test_table_grow_maximum_limit() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::FuncRef,
            5,
            Some(7), // Small maximum for testing
            None,
        ).expect("Failed to create table");

        // Test growing beyond maximum
        let result = table.grow(&store, 5, TableElement::FuncRef(None));
        assert!(result.is_err());
    }

    #[test]
    fn test_table_fill() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::ExternRef,
            10,
            None,
            None,
        ).expect("Failed to create table");

        // Test filling a range
        table.fill(&store, 2, TableElement::ExternRef(Some(456)), 3)
            .expect("Failed to fill table");

        // Verify the filled elements
        for i in 2..5 {
            let element = table.get(&store, i).expect("Failed to get element");
            match element {
                TableElement::ExternRef(ref_id) => assert_eq!(ref_id, Some(0)), // Mapped to 0 for now
                _ => panic!("Expected ExternRef element"),
            }
        }
    }

    #[test]
    fn test_invalid_element_type() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        // Test creating table with invalid element type (I32)
        let result = Table::new(
            &store,
            ValType::I32,
            10,
            None,
            None,
        );
        assert!(result.is_err());
    }

    #[test]
    fn test_type_mismatch() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::FuncRef,
            10,
            None,
            None,
        ).expect("Failed to create table");

        // Test setting wrong element type
        let result = table.set(&store, 0, TableElement::ExternRef(None));
        assert!(result.is_err());
    }
}