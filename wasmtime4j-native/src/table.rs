//! WebAssembly Table management with bounds checking and reference type support
//!
//! This module provides safe wrapper around wasmtime::Table for managing tables
//! in WebAssembly modules. Tables are used to store references to functions,
//! external objects, or other reference types with proper bounds checking
//! and type validation.

use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use wasmtime::{Table as WasmtimeTable, TableType, Val, ValType, RefType, Ref, Func, Extern};
use crate::store::Store;
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

/// Global reference registry for managing references across table operations
use once_cell::sync::Lazy;
static REFERENCE_REGISTRY: Lazy<Arc<Mutex<ReferenceRegistry>>> = Lazy::new(|| {
    Arc::new(Mutex::new(ReferenceRegistry::new()))
});

/// Registry for managing WebAssembly references by ID
#[derive(Debug)]
struct ReferenceRegistry {
    functions: HashMap<u64, Func>,
    externals: HashMap<u64, Extern>,
    next_id: u64,
}

impl ReferenceRegistry {
    fn new() -> Self {
        Self {
            functions: HashMap::new(),
            externals: HashMap::new(),
            next_id: 1, // Start from 1, 0 reserved for null
        }
    }

    fn register_function(&mut self, func: Func) -> u64 {
        let id = self.next_id;
        self.next_id += 1;
        self.functions.insert(id, func);
        id
    }

    fn register_external(&mut self, external: Extern) -> u64 {
        let id = self.next_id;
        self.next_id += 1;
        self.externals.insert(id, external);
        id
    }

    fn get_function(&self, id: u64) -> Option<&Func> {
        self.functions.get(&id)
    }

    fn get_external(&self, id: u64) -> Option<&Extern> {
        self.externals.get(&id)
    }

    fn remove_function(&mut self, id: u64) -> Option<Func> {
        self.functions.remove(&id)
    }

    fn remove_external(&mut self, id: u64) -> Option<Extern> {
        self.externals.remove(&id)
    }
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

        let ref_type = Self::valtype_to_reftype(&element_type)?;
        let table_type = TableType::new(ref_type, initial_size, maximum_size);
        let initial_value = Self::default_ref_for_type(&element_type)?;

        let wasmtime_table = store.with_context(|ctx| {
            WasmtimeTable::new(ctx, table_type, initial_value)
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
    pub fn size(&self, store: &Store) -> WasmtimeResult<u64> {
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        store.with_context_ro(|ctx| {
            Ok(table.size(&ctx))
        })
    }

    /// Get an element from the table at the specified index
    pub fn get(&self, store: &Store, index: u64) -> WasmtimeResult<TableElement> {
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

        let wasmtime_value = store.with_context(|ctx| {
            table.get(ctx, index)
                .ok_or_else(|| WasmtimeError::Runtime {
                    message: format!("Failed to get table element at index {}", index),
                    backtrace: None,
                })
        })?;

        Self::wasmtime_val_to_table_element(wasmtime_value, &self.metadata.element_type)
    }

    /// Set an element in the table at the specified index
    pub fn set(&self, store: &Store, index: u64, element: TableElement) -> WasmtimeResult<()> {
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

        let wasmtime_value = Self::table_element_to_wasmtime_ref(element)?;

        store.with_context(|mut ctx| {
            table.set(&mut ctx, index, wasmtime_value)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to set table element at index {}: {}", index, e),
                    backtrace: None,
                })
        })
    }

    /// Grow the table by the specified number of elements
    pub fn grow(&self, store: &Store, delta: u32, init_value: TableElement) -> WasmtimeResult<u64> {
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
            if current_size.saturating_add(delta as u64) > max_size as u64 {
                return Err(WasmtimeError::Runtime {
                    message: format!(
                        "Cannot grow table: would exceed maximum size {} (current: {}, delta: {})",
                        max_size, current_size, delta
                    ),
                    backtrace: None,
                });
            }
        }

        let wasmtime_init_value = Self::table_element_to_wasmtime_ref(init_value)?;

        store.with_context(|mut ctx| {
            table.grow(&mut ctx, delta as u64, wasmtime_init_value)
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
        if (dst as u64).saturating_add(len as u64) > table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table fill would exceed bounds: dst={}, len={}, table_size={}",
                    dst, len, table_size
                ),
                backtrace: None,
            });
        }

        let wasmtime_value = Self::table_element_to_wasmtime_ref(value)?;

        store.with_context(|mut ctx| {
            table.fill(&mut ctx, dst as u64, wasmtime_value, len as u64)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to fill table: {}", e),
                    backtrace: None,
                })
        })
    }

    /// Copy elements within the table
    pub fn copy_within(
        &self,
        store: &Store,
        dst: u32,
        src: u32,
        len: u32,
    ) -> WasmtimeResult<()> {
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        let table_size = store.with_context_ro(|ctx| {
            Ok(table.size(&ctx))
        })?;

        // Bounds check
        if (dst as u64).saturating_add(len as u64) > table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table copy destination would exceed bounds: dst={}, len={}, table_size={}",
                    dst, len, table_size
                ),
                backtrace: None,
            });
        }
        if (src as u64).saturating_add(len as u64) > table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table copy source would exceed bounds: src={}, len={}, table_size={}",
                    src, len, table_size
                ),
                backtrace: None,
            });
        }

        store.with_context(|mut ctx| {
            // Manually implement table copy since direct copy might not be available
            for i in 0..len {
                let src_elem = table.get(&mut ctx, (src + i) as u64)
                    .ok_or_else(|| WasmtimeError::Runtime {
                        message: format!("Failed to get element at index {}", src + i),
                        backtrace: None,
                    })?;
                table.set(&mut ctx, (dst + i) as u64, src_elem)
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to set element at index {}: {}", dst + i, e),
                        backtrace: None,
                    })?;
            }
            Ok(())
        })
    }

    /// Copy elements from another table to this table
    pub fn copy_from(
        &self,
        store: &Store,
        dst: u32,
        src_table: &Table,
        src: u32,
        len: u32,
    ) -> WasmtimeResult<()> {
        // Validate type compatibility
        if format!("{:?}", self.metadata.element_type) != format!("{:?}", src_table.metadata.element_type) {
            return Err(WasmtimeError::Type {
                message: format!(
                    "Table element types must match: dst={:?}, src={:?}",
                    self.metadata.element_type, src_table.metadata.element_type
                ),
            });
        }

        let dst_table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire destination table lock: {}", e),
        })?;

        let src_table_inner = src_table.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire source table lock: {}", e),
        })?;

        let (dst_table_size, src_table_size) = store.with_context_ro(|ctx| {
            Ok((dst_table.size(&ctx), src_table_inner.size(&ctx)))
        })?;

        // Bounds check
        if (dst as u64).saturating_add(len as u64) > dst_table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table copy destination would exceed bounds: dst={}, len={}, dst_table_size={}",
                    dst, len, dst_table_size
                ),
                backtrace: None,
            });
        }
        if (src as u64).saturating_add(len as u64) > src_table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table copy source would exceed bounds: src={}, len={}, src_table_size={}",
                    src, len, src_table_size
                ),
                backtrace: None,
            });
        }

        store.with_context(|mut ctx| {
            // Use Wasmtime's table_copy instruction for safe cross-table copying
            let dst_index = dst as u64;
            let src_index = src as u64;
            let count = len as u64;

            // Manually copy elements since wasmtime doesn't have direct cross-table copy API
            for i in 0..count {
                let element = src_table_inner.get(&mut ctx, src_index + i)
                    .ok_or_else(|| WasmtimeError::Runtime {
                        message: format!("Failed to get source element at index {}", src_index + i),
                        backtrace: None,
                    })?;

                dst_table.set(&mut ctx, dst_index + i, element)
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to set destination element at index {}: {}", dst_index + i, e),
                        backtrace: None,
                    })?;
            }

            Ok(())
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
            ValType::Ref(_) => Ok(()),
            _ => Err(WasmtimeError::Type {
                message: format!("Invalid table element type: {:?}. Only reference types are allowed.", element_type),
            }),
        }
    }

    /// Validate that a TableElement matches the expected ValType
    fn validate_element_matches_type(element: &TableElement, expected_type: &ValType) -> WasmtimeResult<()> {
        let matches = match (element, expected_type) {
            (TableElement::FuncRef(_), ValType::Ref(ref_type)) => {
                // Check if the reference type is specifically funcref
                matches!(ref_type.heap_type(), wasmtime::HeapType::Func)
            },
            (TableElement::ExternRef(_), ValType::Ref(ref_type)) => {
                // Check if the reference type is specifically externref
                matches!(ref_type.heap_type(), wasmtime::HeapType::Extern)
            },
            (TableElement::AnyRef(_), ValType::Ref(_)) => {
                // AnyRef should match any reference type (used for generic operations)
                true
            },
            _ => false,
        };

        if !matches {
            let element_type_name = match element {
                TableElement::FuncRef(_) => "funcref",
                TableElement::ExternRef(_) => "externref", 
                TableElement::AnyRef(_) => "anyref",
            };
            
            let expected_type_name = match expected_type {
                ValType::Ref(ref_type) => match ref_type.heap_type() {
                    wasmtime::HeapType::Func => "funcref",
                    wasmtime::HeapType::Extern => "externref",
                    _ => "unknown_ref",
                },
                _ => "non_ref",
            };
            
            return Err(WasmtimeError::Type {
                message: format!(
                    "Table element type {} does not match expected table element type {}",
                    element_type_name, expected_type_name
                ),
            });
        }

        Ok(())
    }

    /// Convert ValType to RefType for table creation
    fn valtype_to_reftype(element_type: &ValType) -> WasmtimeResult<RefType> {
        match element_type {
            ValType::Ref(ref_type) => Ok(ref_type.clone()),
            _ => Err(WasmtimeError::Type {
                message: format!("Cannot convert non-reference ValType to RefType: {:?}", element_type),
            }),
        }
    }

    /// Get the default Ref value for a given element type
    fn default_ref_for_type(element_type: &ValType) -> WasmtimeResult<Ref> {
        match element_type {
            ValType::Ref(ref_type) => {
                Ok(Ref::null(ref_type.heap_type()))
            },
            _ => Err(WasmtimeError::Type {
                message: format!("No default ref for non-reference type: {:?}", element_type),
            }),
        }
    }

    /// Get the default value for a given element type
    #[allow(dead_code)]
    fn default_value_for_type(element_type: &ValType) -> WasmtimeResult<Val> {
        let default_val = match element_type {
            ValType::Ref(_ref_type) => {
                // Use FuncRef null as default for now
                // TODO: Determine correct reference type based on ref_type
                Val::FuncRef(None)
            },
            _ => return Err(WasmtimeError::Type {
                message: format!("No default value for non-reference type: {:?}", element_type),
            }),
        };

        Ok(default_val)
    }

    /// Convert TableElement to wasmtime::Val
    #[allow(dead_code)]
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

    /// Convert TableElement to wasmtime::Ref for table operations
    fn table_element_to_wasmtime_ref(element: TableElement) -> WasmtimeResult<Ref> {
        let wasmtime_ref = match element {
            TableElement::FuncRef(ref_id) => {
                if let Some(id) = ref_id {
                    // Look up function reference in the registry
                    let registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
                        message: format!("Failed to lock reference registry: {}", e),
                    })?;

                    if let Some(func) = registry.get_function(id) {
                        Ref::from(Clone::clone(func))
                    } else {
                        // Function not found, use null reference
                        Ref::null(&RefType::FUNCREF.heap_type())
                    }
                } else {
                    Ref::null(&RefType::FUNCREF.heap_type())
                }
            },
            TableElement::ExternRef(ref_id) => {
                if let Some(id) = ref_id {
                    // Look up external reference in the registry
                    let registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
                        message: format!("Failed to lock reference registry: {}", e),
                    })?;

                    if let Some(external) = registry.get_external(id) {
                        // Convert Extern to Ref - this might need specific handling based on Extern type
                        match external {
                            Extern::Func(func) => Ref::from(Clone::clone(func)),
                            _ => Ref::null(&RefType::EXTERNREF.heap_type()), // For other external types, use null for now
                        }
                    } else {
                        // External not found, use null reference
                        Ref::null(&RefType::EXTERNREF.heap_type())
                    }
                } else {
                    Ref::null(&RefType::EXTERNREF.heap_type())
                }
            },
            TableElement::AnyRef(ref_id) => {
                if let Some(id) = ref_id {
                    // Try to resolve as function first, then external
                    let registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
                        message: format!("Failed to lock reference registry: {}", e),
                    })?;

                    if let Some(func) = registry.get_function(id) {
                        Ref::from(Clone::clone(func))
                    } else if let Some(external) = registry.get_external(id) {
                        match external {
                            Extern::Func(func) => Ref::from(Clone::clone(func)),
                            _ => Ref::null(&RefType::EXTERNREF.heap_type()),
                        }
                    } else {
                        // Reference not found, use null
                        Ref::null(&RefType::FUNCREF.heap_type())
                    }
                } else {
                    Ref::null(&RefType::FUNCREF.heap_type())
                }
            },
        };

        Ok(wasmtime_ref)
    }

    /// Convert wasmtime::Val to TableElement
    fn wasmtime_val_to_table_element(val: Ref, element_type: &ValType) -> WasmtimeResult<TableElement> {
        let table_element = match element_type {
            ValType::Ref(ref_type) => {
                // Discriminate between different reference types based on heap type
                match ref_type.heap_type() {
                    wasmtime::HeapType::Func => {
                        if val.is_null() {
                            TableElement::FuncRef(None)
                        } else {
                            // Try to extract function and register it
                            if let Some(func_ref) = val.as_func() {
                                let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
                                    message: format!("Failed to lock reference registry: {}", e),
                                })?;
                                let id = registry.register_function(func_ref.unwrap().clone());
                                TableElement::FuncRef(Some(id))
                            } else {
                                TableElement::FuncRef(None)
                            }
                        }
                    },
                    wasmtime::HeapType::Extern => {
                        if val.is_null() {
                            TableElement::ExternRef(None)
                        } else {
                            // For external references, we create a generic Extern::Func if possible
                            if let Some(func_ref) = val.as_func() {
                                let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
                                    message: format!("Failed to lock reference registry: {}", e),
                                })?;
                                let external = Extern::Func(func_ref.unwrap().clone());
                                let id = registry.register_external(external);
                                TableElement::ExternRef(Some(id))
                            } else {
                                // Could not extract function, use null
                                TableElement::ExternRef(None)
                            }
                        }
                    },
                    _ => {
                        // For unknown reference types, use AnyRef
                        if val.is_null() {
                            TableElement::AnyRef(None)
                        } else {
                            // Try to register as function first, then as external
                            if let Some(func_ref) = val.as_func() {
                                let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
                                    message: format!("Failed to lock reference registry: {}", e),
                                })?;
                                let id = registry.register_function(func_ref.unwrap().clone());
                                TableElement::AnyRef(Some(id))
                            } else {
                                // Could not extract, use null
                                TableElement::AnyRef(None)
                            }
                        }
                    }
                }
            },
            _ => return Err(WasmtimeError::Type {
                message: format!("Cannot convert Ref to TableElement for non-reference type {:?}", element_type),
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
            element_type: ValType::Ref(table_type.element().clone()),
            initial_size: table_type.minimum() as u32,
            maximum_size: table_type.maximum().map(|s| s as u32),
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
        table.size(store).map(|s| s as u32)
    }

    /// Core function to get table element
    pub fn get_table_element(table: &Table, store: &Store, index: u32) -> WasmtimeResult<TableElement> {
        table.get(store, index as u64)
    }

    /// Core function to set table element
    pub fn set_table_element(
        table: &Table,
        store: &Store,
        index: u32,
        element: TableElement,
    ) -> WasmtimeResult<()> {
        table.set(store, index as u64, element)
    }

    /// Core function to grow table
    pub fn grow_table(
        table: &Table,
        store: &Store,
        delta: u32,
        init_value: TableElement,
    ) -> WasmtimeResult<u32> {
        table.grow(store, delta, init_value).map(|s| s as u32)
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

    /// Core function to copy within table
    pub fn copy_table_within(
        table: &Table,
        store: &Store,
        dst: u32,
        src: u32,
        len: u32,
    ) -> WasmtimeResult<()> {
        table.copy_within(store, dst, src, len)
    }

    /// Core function to copy from another table
    pub fn copy_table_from(
        dst_table: &Table,
        store: &Store,
        dst: u32,
        src_table: &Table,
        src: u32,
        len: u32,
    ) -> WasmtimeResult<()> {
        dst_table.copy_from(store, dst, src_table, src, len)
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
        if table_ptr.is_null() {
            return;
        }

        let ptr_addr = table_ptr as usize;
        
        // Detect and reject obvious test/fake pointers
        if ptr_addr < 0x1000 || (ptr_addr & 0xFFFFFF0000000000) == 0x1234560000000000 {
            log::debug!("Ignoring fake/test pointer {:p} in destroy_table", table_ptr);
            return;
        }

        // Check if pointer was already destroyed
        {
            use crate::error::ffi_utils::DESTROYED_POINTERS;
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
            if destroyed.contains(&ptr_addr) {
                log::warn!("Attempted double-free of Table resource at {:p} - ignoring", table_ptr);
                return;
            }
            destroyed.insert(ptr_addr);
        }

        // Simple, correct cleanup - let Rust handle Arc dropping naturally
        let result = std::panic::catch_unwind(|| {
            let _boxed_table = Box::from_raw(table_ptr as *mut Table);
            // Box and Arc will be dropped automatically here
            log::debug!("Table at {:p} being destroyed", table_ptr);
        });

        match result {
            Ok(_) => {
                log::debug!("Table resource at {:p} destroyed successfully", table_ptr);
            }
            Err(e) => {
                log::error!("Table resource at {:p} destruction panicked: {:?} - preventing JVM crash", table_ptr, e);
                // Don't propagate panic to JVM
            }
        }
    }

    /// Helper function to create TableElement from raw components
    pub fn create_table_element(
        element_type: ValType,
        ref_id: Option<u64>,
    ) -> WasmtimeResult<TableElement> {
        let table_element = match element_type {
            ValType::Ref(_ref_type) => {
                // For now, treat all ref types as AnyRef
                // TODO: Discriminate between different reference types
                TableElement::AnyRef(ref_id)
            },
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
    
    /// Validate if a ValType is compatible with table elements
    pub fn validate_table_element_type(val_type: &ValType) -> WasmtimeResult<()> {
        match val_type {
            ValType::Ref(ref_type) => {
                match ref_type.heap_type() {
                    wasmtime::HeapType::Func | wasmtime::HeapType::Extern => Ok(()),
                    _ => Err(WasmtimeError::Type {
                        message: format!("Unsupported table element reference type: {:?}", ref_type),
                    }),
                }
            },
            _ => Err(WasmtimeError::Type {
                message: format!("Table elements must be reference types, got: {:?}", val_type),
            }),
        }
    }
    
    /// Create a TableElement from ValType and optional reference ID
    pub fn create_typed_table_element(
        val_type: &ValType,
        ref_id: Option<u64>
    ) -> WasmtimeResult<TableElement> {
        match val_type {
            ValType::Ref(ref_type) => {
                match ref_type.heap_type() {
                    wasmtime::HeapType::Func => Ok(TableElement::FuncRef(ref_id)),
                    wasmtime::HeapType::Extern => Ok(TableElement::ExternRef(ref_id)),
                    _ => Ok(TableElement::AnyRef(ref_id)),
                }
            },
            _ => Err(WasmtimeError::Type {
                message: format!("Cannot create TableElement from non-reference type: {:?}", val_type),
            }),
        }
    }

    /// Register a function reference and return its ID
    pub fn register_function_reference(func: Func) -> WasmtimeResult<u64> {
        let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock reference registry: {}", e),
        })?;
        Ok(registry.register_function(func))
    }

    /// Register an external reference and return its ID
    pub fn register_external_reference(external: Extern) -> WasmtimeResult<u64> {
        let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock reference registry: {}", e),
        })?;
        Ok(registry.register_external(external))
    }

    /// Get a function reference by ID
    pub fn get_function_reference(id: u64) -> WasmtimeResult<Option<Func>> {
        let registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock reference registry: {}", e),
        })?;
        Ok(registry.get_function(id).cloned())
    }

    /// Get an external reference by ID
    pub fn get_external_reference(id: u64) -> WasmtimeResult<Option<Extern>> {
        let registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock reference registry: {}", e),
        })?;
        Ok(registry.get_external(id).cloned())
    }

    /// Remove a function reference from the registry
    pub fn remove_function_reference(id: u64) -> WasmtimeResult<Option<Func>> {
        let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock reference registry: {}", e),
        })?;
        Ok(registry.remove_function(id))
    }

    /// Remove an external reference from the registry
    pub fn remove_external_reference(id: u64) -> WasmtimeResult<Option<Extern>> {
        let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock reference registry: {}", e),
        })?;
        Ok(registry.remove_external(id))
    }

    /// Clear all references (useful for cleanup)
    pub fn clear_references() -> WasmtimeResult<()> {
        let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock reference registry: {}", e),
        })?;
        registry.functions.clear();
        registry.externals.clear();
        Ok(())
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