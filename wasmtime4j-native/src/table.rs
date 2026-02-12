//! WebAssembly Table management with bounds checking and reference type support
//!
//! This module provides safe wrapper around wasmtime::Table for managing tables
//! in WebAssembly modules. Tables are used to store references to functions,
//! external objects, or other reference types with proper bounds checking
//! and type validation.

use crate::element_segment::ElementItem;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::global::ReferenceType;
use crate::instance::Instance;
use crate::store::Store;
use std::collections::HashMap;
use std::mem::ManuallyDrop;
use std::sync::{Arc, Mutex};
use wasmtime::{
    Extern, Func, HeapType, Ref, RefType, Table as WasmtimeTable, TableType, Val, ValType,
};

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
    pub initial_size: u64,
    /// Maximum size limit (None means unlimited)
    pub maximum_size: Option<u64>,
    /// Optional name for debugging purposes
    pub name: Option<String>,
    /// Whether this table uses 64-bit addressing (Memory64 proposal)
    pub is_64: bool,
}

/// Global reference registry for managing references across table operations
/// Wrapped in ManuallyDrop to prevent automatic cleanup during process exit.
use once_cell::sync::Lazy;
static REFERENCE_REGISTRY: Lazy<ManuallyDrop<Arc<Mutex<ReferenceRegistry>>>> =
    Lazy::new(|| ManuallyDrop::new(Arc::new(Mutex::new(ReferenceRegistry::new()))));

/// Registry for managing WebAssembly references by ID
#[derive(Debug)]
struct ReferenceRegistry {
    functions: HashMap<u64, Arc<Func>>,
    externals: HashMap<u64, Arc<Extern>>,
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
        self.functions.insert(id, Arc::new(func));
        id
    }

    fn register_external(&mut self, external: Extern) -> u64 {
        let id = self.next_id;
        self.next_id += 1;
        self.externals.insert(id, Arc::new(external));
        id
    }

    fn get_function(&self, id: u64) -> Option<Arc<Func>> {
        self.functions.get(&id).cloned()
    }

    fn get_external(&self, id: u64) -> Option<Arc<Extern>> {
        self.externals.get(&id).cloned()
    }

    fn remove_function(&mut self, id: u64) -> Option<Arc<Func>> {
        self.functions.remove(&id)
    }

    fn remove_external(&mut self, id: u64) -> Option<Arc<Extern>> {
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
    /// Create a new 32-bit table with specified element type and size
    pub fn new(
        store: &Store,
        element_type: ValType,
        initial_size: u32,
        maximum_size: Option<u32>,
        name: Option<String>,
    ) -> WasmtimeResult<Self> {
        Self::new_internal(
            store,
            element_type,
            initial_size as u64,
            maximum_size.map(|s| s as u64),
            name,
            false, // 32-bit table
        )
    }

    /// Create a new 64-bit table with specified element type and size (Memory64 proposal)
    ///
    /// 64-bit tables allow for larger table sizes and are part of the WebAssembly
    /// Memory64 proposal. This creates a table that uses 64-bit indices.
    pub fn new64(
        store: &Store,
        element_type: ValType,
        initial_size: u64,
        maximum_size: Option<u64>,
        name: Option<String>,
    ) -> WasmtimeResult<Self> {
        Self::new_internal(store, element_type, initial_size, maximum_size, name, true)
    }

    /// Internal table creation method
    fn new_internal(
        store: &Store,
        element_type: ValType,
        initial_size: u64,
        maximum_size: Option<u64>,
        name: Option<String>,
        is_64: bool,
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

        // Create table type based on 32-bit or 64-bit addressing
        let table_type = if is_64 {
            TableType::new64(ref_type, initial_size, maximum_size)
        } else {
            // For 32-bit tables, we need to validate sizes fit in u32
            let init_size_32 =
                initial_size
                    .try_into()
                    .map_err(|_| WasmtimeError::InvalidParameter {
                        message: format!(
                            "Initial size {} exceeds u32 maximum for 32-bit table",
                            initial_size
                        ),
                    })?;
            let max_size_32 = maximum_size
                .map(|s| s.try_into())
                .transpose()
                .map_err(|_| WasmtimeError::InvalidParameter {
                    message: "Maximum size exceeds u32 maximum for 32-bit table".to_string(),
                })?;
            TableType::new(ref_type, init_size_32, max_size_32)
        };

        let initial_value = Self::default_ref_for_type(&element_type)?;

        let wasmtime_table = store.with_context(|ctx| {
            WasmtimeTable::new(ctx, table_type, initial_value).map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create table: {}", e),
                backtrace: None,
            })
        })?;

        let metadata = TableMetadata {
            element_type,
            initial_size,
            maximum_size,
            name,
            is_64,
        };

        Ok(Table {
            inner: Arc::new(Mutex::new(wasmtime_table)),
            metadata,
        })
    }

    /// Check if this table uses 64-bit addressing (Memory64 proposal)
    pub fn is_64(&self) -> bool {
        self.metadata.is_64
    }

    /// Get the current size of the table
    pub fn size(&self, store: &Store) -> WasmtimeResult<u64> {
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        store.with_context_ro(|ctx| Ok(table.size(&ctx)))
    }

    /// Get an element from the table at the specified index
    pub fn get(&self, store: &Store, index: u64) -> WasmtimeResult<TableElement> {
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        let table_size = store.with_context_ro(|ctx| Ok(table.size(&ctx)))?;

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
            table.get(ctx, index).ok_or_else(|| WasmtimeError::Runtime {
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

        let table_size = store.with_context_ro(|ctx| Ok(table.size(&ctx)))?;

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
            table
                .set(&mut ctx, index, wasmtime_value)
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

        let current_size = store.with_context_ro(|ctx| Ok(table.size(&ctx)))?;

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
            table
                .grow(&mut ctx, delta as u64, wasmtime_init_value)
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

        let table_size = store.with_context_ro(|ctx| Ok(table.size(&ctx)))?;

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
            table
                .fill(&mut ctx, dst as u64, wasmtime_value, len as u64)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to fill table: {}", e),
                    backtrace: None,
                })
        })
    }

    /// Copy elements within the table
    pub fn copy_within(&self, store: &Store, dst: u32, src: u32, len: u32) -> WasmtimeResult<()> {
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        let table_size = store.with_context_ro(|ctx| Ok(table.size(&ctx)))?;

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
                let src_elem = table.get(&mut ctx, (src + i) as u64).ok_or_else(|| {
                    WasmtimeError::Runtime {
                        message: format!("Failed to get element at index {}", src + i),
                        backtrace: None,
                    }
                })?;
                table
                    .set(&mut ctx, (dst + i) as u64, src_elem)
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
        if format!("{:?}", self.metadata.element_type)
            != format!("{:?}", src_table.metadata.element_type)
        {
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

        let src_table_inner = src_table
            .inner
            .lock()
            .map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to acquire source table lock: {}", e),
            })?;

        let (dst_table_size, src_table_size) =
            store.with_context_ro(|ctx| Ok((dst_table.size(&ctx), src_table_inner.size(&ctx))))?;

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
                let element = src_table_inner
                    .get(&mut ctx, src_index + i)
                    .ok_or_else(|| WasmtimeError::Runtime {
                        message: format!("Failed to get source element at index {}", src_index + i),
                        backtrace: None,
                    })?;

                dst_table
                    .set(&mut ctx, dst_index + i, element)
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!(
                            "Failed to set destination element at index {}: {}",
                            dst_index + i,
                            e
                        ),
                        backtrace: None,
                    })?;
            }

            Ok(())
        })
    }

    /// Initialize table elements from an element segment
    ///
    /// This implements the table.init instruction, copying elements from
    /// a passive element segment into the table.
    ///
    /// # Arguments
    /// * `store` - The WebAssembly store
    /// * `instance` - The instance containing the element segments
    /// * `dst` - Destination offset in the table
    /// * `src` - Source offset in the element segment
    /// * `len` - Number of elements to copy
    /// * `segment_index` - Index of the element segment to copy from
    pub fn init_from_segment(
        &self,
        store: &Store,
        instance: &Instance,
        dst: u32,
        src: u32,
        len: u32,
        segment_index: u32,
    ) -> WasmtimeResult<()> {
        // Get element segment manager from instance
        let segment_manager = instance.get_element_segment_manager();

        // Get table lock
        let table = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire table lock: {}", e),
        })?;

        // Get table size for bounds checking
        let table_size = store.with_context_ro(|ctx| Ok(table.size(&ctx)))?;

        // Bounds check for destination
        if (dst as u64).saturating_add(len as u64) > table_size {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table.init destination would exceed bounds: dst={}, len={}, table_size={}",
                    dst, len, table_size
                ),
                backtrace: None,
            });
        }

        // Get segment info for validation
        let segment = segment_manager.get_segment(segment_index).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Element segment index {} out of bounds", segment_index),
            }
        })?;

        // Check if segment is available (Some = passive, None = active/declarative)
        let segment = segment.as_ref().ok_or_else(|| WasmtimeError::Runtime {
            message: format!(
                "Element segment {} is not available (may be active or declarative)",
                segment_index
            ),
            backtrace: None,
        })?;

        // Bounds check for source
        if (src as u64).saturating_add(len as u64) > segment.len() as u64 {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Table.init source would exceed segment bounds: src={}, len={}, segment_size={}",
                    src, len, segment.len()
                ),
                backtrace: None,
            });
        }

        // Validate type compatibility
        if format!("{:?}", self.metadata.element_type) != format!("{:?}", segment.elem_type) {
            return Err(WasmtimeError::Type {
                message: format!(
                    "Table element type {:?} does not match segment element type {:?}",
                    self.metadata.element_type, segment.elem_type
                ),
            });
        }

        // Copy elements from segment to table
        store.with_context(|mut ctx| {
            for i in 0..len {
                let element_item = segment_manager.get_element(segment_index, src + i)?;

                // Convert ElementItem to Ref
                let ref_val = match element_item {
                    ElementItem::NullFunc => {
                        Ref::null(&HeapType::Func)
                    }
                    ElementItem::FuncIndex(func_idx) => {
                        // Get the function from the instance by index
                        // For now, we'll create a null reference as a placeholder
                        // Full implementation would need to resolve the function reference
                        // from the instance's exports or internal function table
                        log::warn!("table.init with function index {} - creating null ref (function resolution not yet implemented)", func_idx);
                        Ref::null(&HeapType::Func)
                    }
                    ElementItem::Expr(_) => {
                        // Expression-based elements
                        log::warn!("table.init with expression element - creating null ref (expression evaluation not yet implemented)");
                        Ref::null(&HeapType::Func)
                    }
                };

                // Set the table element
                table.set(&mut ctx, (dst + i) as u64, ref_val)
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to set table element at index {}: {}", dst + i, e),
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

        store.with_context_ro(|ctx| Ok(table.ty(&ctx)))
    }

    /// Validate that a ValType is a valid reference type for table elements
    fn validate_element_type(element_type: &ValType) -> WasmtimeResult<()> {
        match element_type {
            ValType::Ref(_) => Ok(()),
            _ => Err(WasmtimeError::Type {
                message: format!(
                    "Invalid table element type: {:?}. Only reference types are allowed.",
                    element_type
                ),
            }),
        }
    }

    /// Validate that a TableElement matches the expected ValType
    fn validate_element_matches_type(
        element: &TableElement,
        expected_type: &ValType,
    ) -> WasmtimeResult<()> {
        let matches = match (element, expected_type) {
            (TableElement::FuncRef(_), ValType::Ref(ref_type)) => {
                // Check if the reference type is specifically funcref
                matches!(ref_type.heap_type(), wasmtime::HeapType::Func)
            }
            (TableElement::ExternRef(_), ValType::Ref(ref_type)) => {
                // Check if the reference type is specifically externref
                matches!(ref_type.heap_type(), wasmtime::HeapType::Extern)
            }
            (TableElement::AnyRef(_), ValType::Ref(_)) => {
                // AnyRef should match any reference type (used for generic operations)
                true
            }
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
                message: format!(
                    "Cannot convert non-reference ValType to RefType: {:?}",
                    element_type
                ),
            }),
        }
    }

    /// Get the default Ref value for a given element type
    fn default_ref_for_type(element_type: &ValType) -> WasmtimeResult<Ref> {
        match element_type {
            ValType::Ref(ref_type) => Ok(Ref::null(ref_type.heap_type())),
            _ => Err(WasmtimeError::Type {
                message: format!("No default ref for non-reference type: {:?}", element_type),
            }),
        }
    }

    /// Convert TableElement to wasmtime::Ref for table operations
    fn table_element_to_wasmtime_ref(element: TableElement) -> WasmtimeResult<Ref> {
        let wasmtime_ref = match element {
            TableElement::FuncRef(ref_id) => {
                if let Some(id) = ref_id {
                    // Look up function reference in the registry
                    let registry =
                        REFERENCE_REGISTRY
                            .lock()
                            .map_err(|e| WasmtimeError::Concurrency {
                                message: format!("Failed to lock reference registry: {}", e),
                            })?;

                    if let Some(arc_func) = registry.get_function(id) {
                        Ref::from(Clone::clone(&*arc_func))
                    } else {
                        // Function not found - return error instead of silent null
                        return Err(WasmtimeError::Runtime {
                            message: format!("Function reference with id {} not found in registry (registry has {} functions)", id, registry.functions.len()),
                            backtrace: None,
                        });
                    }
                } else {
                    Ref::null(&RefType::FUNCREF.heap_type())
                }
            }
            TableElement::ExternRef(ref_id) => {
                if let Some(id) = ref_id {
                    // Look up external reference in the registry
                    let registry =
                        REFERENCE_REGISTRY
                            .lock()
                            .map_err(|e| WasmtimeError::Concurrency {
                                message: format!("Failed to lock reference registry: {}", e),
                            })?;

                    if let Some(arc_external) = registry.get_external(id) {
                        // Convert Extern to Ref - this might need specific handling based on Extern type
                        match &*arc_external {
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
            }
            TableElement::AnyRef(ref_id) => {
                if let Some(id) = ref_id {
                    // Try to resolve as function first, then external
                    let registry =
                        REFERENCE_REGISTRY
                            .lock()
                            .map_err(|e| WasmtimeError::Concurrency {
                                message: format!("Failed to lock reference registry: {}", e),
                            })?;

                    if let Some(arc_func) = registry.get_function(id) {
                        Ref::from(Clone::clone(&*arc_func))
                    } else if let Some(arc_external) = registry.get_external(id) {
                        match &*arc_external {
                            Extern::Func(func) => Ref::from(Clone::clone(func)),
                            _ => Ref::null(&RefType::EXTERNREF.heap_type()),
                        }
                    } else {
                        // Reference not found - return error instead of silent null
                        return Err(WasmtimeError::Runtime {
                            message: format!("AnyRef reference with id {} not found in registry (registry has {} functions)", id, registry.functions.len()),
                            backtrace: None,
                        });
                    }
                } else {
                    Ref::null(&RefType::FUNCREF.heap_type())
                }
            }
        };

        Ok(wasmtime_ref)
    }

    /// Convert wasmtime::Val to TableElement
    fn wasmtime_val_to_table_element(
        val: Ref,
        element_type: &ValType,
    ) -> WasmtimeResult<TableElement> {
        let table_element = match element_type {
            ValType::Ref(ref_type) => {
                // First, try to extract as a funcref using as_func()
                // In Wasmtime 39+, as_func() returns Option<Option<Func>>:
                // - Some(Some(func)) = non-null funcref
                // - Some(None) = null funcref
                // - None = not a funcref type
                if let Some(func_opt) = val.as_func() {
                    // This is a funcref
                    match func_opt {
                        Some(func) => {
                            // Non-null funcref - register and return
                            log::debug!(
                                "wasmtime_val_to_table_element: registering non-null funcref"
                            );
                            let mut registry = REFERENCE_REGISTRY.lock().map_err(|e| {
                                WasmtimeError::Concurrency {
                                    message: format!("Failed to lock reference registry: {}", e),
                                }
                            })?;
                            let id = registry.register_function(func.clone());
                            log::debug!("wasmtime_val_to_table_element: registered with id={}", id);
                            TableElement::FuncRef(Some(id))
                        }
                        None => {
                            // Null funcref
                            log::debug!("wasmtime_val_to_table_element: null funcref");
                            TableElement::FuncRef(None)
                        }
                    }
                } else {
                    log::debug!("wasmtime_val_to_table_element: not a funcref, checking heap type");
                    // Not a funcref, check for externref or other types based on heap type
                    match ref_type.heap_type() {
                        wasmtime::HeapType::Extern => {
                            if val.is_null() {
                                TableElement::ExternRef(None)
                            } else {
                                // For external references that aren't funcrefs
                                TableElement::ExternRef(None)
                            }
                        }
                        _ => {
                            // For other reference types, use AnyRef
                            if val.is_null() {
                                TableElement::AnyRef(None)
                            } else {
                                TableElement::AnyRef(None)
                            }
                        }
                    }
                }
            }
            _ => {
                return Err(WasmtimeError::Type {
                    message: format!(
                        "Cannot convert Ref to TableElement for non-reference type {:?}",
                        element_type
                    ),
                })
            }
        };

        log::debug!(
            "wasmtime_val_to_table_element: returning {:?}",
            table_element
        );
        Ok(table_element)
    }

    /// Create a table from an existing wasmtime::Table (for imports/exports)
    pub fn from_wasmtime_table(
        wasmtime_table: WasmtimeTable,
        store: &Store,
        name: Option<String>,
    ) -> WasmtimeResult<Self> {
        let table_type = store.with_context_ro(|ctx| Ok(wasmtime_table.ty(&ctx)))?;

        // Detect if the table uses 64-bit addressing
        let is_64 = table_type.is_64();

        let metadata = TableMetadata {
            element_type: ValType::Ref(table_type.element().clone()),
            initial_size: table_type.minimum(),
            maximum_size: table_type.maximum(),
            name,
            is_64,
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
    use crate::validate_ptr_not_null;
    use std::os::raw::c_void;

    /// Core function to create a new 32-bit table
    pub fn create_table(
        store: &Store,
        element_type: ValType,
        initial_size: u32,
        maximum_size: Option<u32>,
        name: Option<String>,
    ) -> WasmtimeResult<Box<Table>> {
        Table::new(store, element_type, initial_size, maximum_size, name).map(Box::new)
    }

    /// Core function to create a new 64-bit table (Memory64 proposal)
    pub fn create_table64(
        store: &Store,
        element_type: ValType,
        initial_size: u64,
        maximum_size: Option<u64>,
        name: Option<String>,
    ) -> WasmtimeResult<Box<Table>> {
        Table::new64(store, element_type, initial_size, maximum_size, name).map(Box::new)
    }

    /// Check if a table uses 64-bit addressing
    pub fn is_table_64(table: &Table) -> bool {
        table.is_64()
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

    /// Core function to get table maximum size (-1 if unlimited)
    pub fn get_table_max_size(table: &Table, store: &Store) -> WasmtimeResult<i32> {
        let table_type = table.table_type(store)?;
        match table_type.maximum() {
            Some(max) => Ok(max as i32),
            None => Ok(-1), // -1 indicates unlimited
        }
    }

    /// Core function to get table element
    pub fn get_table_element(
        table: &Table,
        store: &Store,
        index: u32,
    ) -> WasmtimeResult<TableElement> {
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
    ///
    /// Uses the consolidated `safe_destroy` utility from `ffi_common::resource_destruction`
    /// which provides double-free protection, fake pointer detection, and panic safety.
    pub unsafe fn destroy_table(table_ptr: *mut c_void) {
        use crate::ffi_common::resource_destruction::safe_destroy;
        let _ = safe_destroy::<Table>(table_ptr, "Table");
    }

    /// Helper function to create TableElement from raw components
    pub fn create_table_element(
        element_type: ValType,
        ref_id: Option<u64>,
    ) -> WasmtimeResult<TableElement> {
        let table_element = match element_type {
            ValType::Ref(ref_type) => {
                // Discriminate between different reference types based on heap type
                match ref_type.heap_type() {
                    HeapType::Func | HeapType::NoFunc | HeapType::ConcreteFunc(_) => {
                        TableElement::FuncRef(ref_id)
                    }
                    HeapType::Extern | HeapType::NoExtern => TableElement::ExternRef(ref_id),
                    // GC types, exception types, continuation types - all map to AnyRef
                    _ => TableElement::AnyRef(ref_id),
                }
            }
            _ => {
                return Err(WasmtimeError::Type {
                    message: format!("Invalid table element type: {:?}", element_type),
                })
            }
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
            ValType::Ref(ref_type) => match ref_type.heap_type() {
                wasmtime::HeapType::Func | wasmtime::HeapType::Extern => Ok(()),
                _ => Err(WasmtimeError::Type {
                    message: format!("Unsupported table element reference type: {:?}", ref_type),
                }),
            },
            _ => Err(WasmtimeError::Type {
                message: format!(
                    "Table elements must be reference types, got: {:?}",
                    val_type
                ),
            }),
        }
    }

    /// Create a TableElement from ValType and optional reference ID
    pub fn create_typed_table_element(
        val_type: &ValType,
        ref_id: Option<u64>,
    ) -> WasmtimeResult<TableElement> {
        match val_type {
            ValType::Ref(ref_type) => match ref_type.heap_type() {
                wasmtime::HeapType::Func => Ok(TableElement::FuncRef(ref_id)),
                wasmtime::HeapType::Extern => Ok(TableElement::ExternRef(ref_id)),
                _ => Ok(TableElement::AnyRef(ref_id)),
            },
            _ => Err(WasmtimeError::Type {
                message: format!(
                    "Cannot create TableElement from non-reference type: {:?}",
                    val_type
                ),
            }),
        }
    }

    /// Register a function reference and return its ID
    pub fn register_function_reference(func: Func) -> WasmtimeResult<u64> {
        let mut registry = REFERENCE_REGISTRY
            .lock()
            .map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to lock reference registry: {}", e),
            })?;
        Ok(registry.register_function(func))
    }

    /// Register an external reference and return its ID
    pub fn register_external_reference(external: Extern) -> WasmtimeResult<u64> {
        let mut registry = REFERENCE_REGISTRY
            .lock()
            .map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to lock reference registry: {}", e),
            })?;
        Ok(registry.register_external(external))
    }

    /// Get a function reference by ID
    pub fn get_function_reference(id: u64) -> WasmtimeResult<Option<Func>> {
        let registry = REFERENCE_REGISTRY
            .lock()
            .map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to lock reference registry: {}", e),
            })?;
        // Get Arc<Func> and dereference it to get a clone of the Func
        // This is safe because Func is Clone and the Arc keeps the original alive
        Ok(registry
            .get_function(id)
            .map(|arc_func| (*arc_func).clone()))
    }

    /// Get an external reference by ID
    pub fn get_external_reference(id: u64) -> WasmtimeResult<Option<Extern>> {
        let registry = REFERENCE_REGISTRY
            .lock()
            .map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to lock reference registry: {}", e),
            })?;
        // Get Arc<Extern> and dereference it to get a clone of the Extern
        Ok(registry
            .get_external(id)
            .map(|arc_extern| (*arc_extern).clone()))
    }

    /// Remove a function reference from the registry
    pub fn remove_function_reference(id: u64) -> WasmtimeResult<Option<Func>> {
        let mut registry = REFERENCE_REGISTRY
            .lock()
            .map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to lock reference registry: {}", e),
            })?;
        // Remove returns Arc<Func>, unwrap it to return Func
        Ok(registry
            .remove_function(id)
            .map(|arc_func| Arc::try_unwrap(arc_func).unwrap_or_else(|arc| (*arc).clone())))
    }

    /// Remove an external reference from the registry
    pub fn remove_external_reference(id: u64) -> WasmtimeResult<Option<Extern>> {
        let mut registry = REFERENCE_REGISTRY
            .lock()
            .map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to lock reference registry: {}", e),
            })?;
        // Remove returns Arc<Extern>, unwrap it to return Extern
        Ok(registry
            .remove_external(id)
            .map(|arc_extern| Arc::try_unwrap(arc_extern).unwrap_or_else(|arc| (*arc).clone())))
    }

    /// Clear all references (useful for cleanup)
    pub fn clear_references() -> WasmtimeResult<()> {
        let mut registry = REFERENCE_REGISTRY
            .lock()
            .map_err(|e| WasmtimeError::Concurrency {
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

    // Use the global shared engine to reduce wasmtime GLOBAL_CODE registry accumulation
    fn shared_engine() -> Engine {
        crate::engine::get_shared_engine()
    }

    #[test]
    fn test_table_creation() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::Ref(RefType::FUNCREF),
            10,
            Some(100),
            Some("test_table".to_string()),
        )
        .expect("Failed to create table");

        // Check if the table element type is a function reference - simplified check
        assert!(matches!(table.metadata().element_type, ValType::Ref(_)));
        assert_eq!(table.metadata().initial_size, 10);
        assert_eq!(table.metadata().maximum_size, Some(100));
        assert_eq!(table.metadata().name, Some("test_table".to_string()));
    }

    #[test]
    fn test_table_size() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(&store, ValType::Ref(RefType::FUNCREF), 10, None, None)
            .expect("Failed to create table");

        let size = table.size(&store).expect("Failed to get table size");
        assert_eq!(size, 10);
    }

    #[test]
    fn test_table_bounds_checking() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(&store, ValType::Ref(RefType::FUNCREF), 5, None, None)
            .expect("Failed to create table");

        // Test accessing out of bounds element
        let result = table.get(&store, 10);
        assert!(result.is_err());

        // Test setting out of bounds element
        let result = table.set(&store, 10, TableElement::FuncRef(None));
        assert!(result.is_err());
    }

    #[test]
    fn test_table_grow() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(&store, ValType::Ref(RefType::FUNCREF), 5, Some(20), None)
            .expect("Failed to create table");

        let initial_size = table.size(&store).expect("Failed to get initial size");
        assert_eq!(initial_size, 5);

        // Test growing table
        let old_size = table
            .grow(&store, 3, TableElement::FuncRef(None))
            .expect("Failed to grow table");
        assert_eq!(old_size, 5);

        let new_size = table.size(&store).expect("Failed to get new size");
        assert_eq!(new_size, 8);
    }

    #[test]
    fn test_table_grow_maximum_limit() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(
            &store,
            ValType::Ref(RefType::FUNCREF),
            5,
            Some(7), // Small maximum for testing
            None,
        )
        .expect("Failed to create table");

        // Test growing beyond maximum
        let result = table.grow(&store, 5, TableElement::FuncRef(None));
        assert!(result.is_err());
    }

    #[test]
    fn test_table_get_set_null_refs() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(&store, ValType::Ref(RefType::FUNCREF), 10, None, None)
            .expect("Failed to create table");

        // Test getting initial value (should be null funcref)
        let element = table.get(&store, 0).expect("Failed to get table element");
        match element {
            TableElement::FuncRef(ref_id) => {
                assert_eq!(ref_id, None, "Initial table element should be null");
            }
            _ => panic!("Expected FuncRef element"),
        }

        // Test setting null value explicitly
        table
            .set(&store, 0, TableElement::FuncRef(None))
            .expect("Failed to set table element to null");

        // Test getting the value back (should still be null)
        let element = table
            .get(&store, 0)
            .expect("Failed to get table element after set");
        match element {
            TableElement::FuncRef(ref_id) => {
                assert_eq!(
                    ref_id, None,
                    "Table element should be null after setting null"
                );
            }
            _ => panic!("Expected FuncRef element"),
        }

        // Test setting and getting at different indices
        for i in 0..5 {
            table
                .set(&store, i, TableElement::FuncRef(None))
                .expect(&format!("Failed to set table element at index {}", i));
            let element = table
                .get(&store, i)
                .expect(&format!("Failed to get table element at index {}", i));
            match element {
                TableElement::FuncRef(ref_id) => {
                    assert_eq!(ref_id, None, "Element at index {} should be null", i);
                }
                _ => panic!("Expected FuncRef element at index {}", i),
            }
        }
    }

    #[test]
    fn test_table_fill_null_refs() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(&store, ValType::Ref(RefType::FUNCREF), 10, None, None)
            .expect("Failed to create table");

        // Fill a range with null funcrefs
        table
            .fill(&store, 2, TableElement::FuncRef(None), 3)
            .expect("Failed to fill table with null funcrefs");

        // Verify the filled elements are null
        for i in 2..5 {
            let element = table.get(&store, i).expect("Failed to get element");
            match element {
                TableElement::FuncRef(ref_id) => {
                    assert_eq!(ref_id, None, "Filled element at index {} should be null", i);
                }
                _ => panic!("Expected FuncRef element at index {}", i),
            }
        }

        // Fill entire table
        table
            .fill(&store, 0, TableElement::FuncRef(None), 10)
            .expect("Failed to fill entire table");

        // Verify all elements
        for i in 0..10 {
            let element = table.get(&store, i).expect("Failed to get element");
            match element {
                TableElement::FuncRef(ref_id) => {
                    assert_eq!(ref_id, None, "Element at index {} should be null", i);
                }
                _ => panic!("Expected FuncRef element at index {}", i),
            }
        }
    }

    #[test]
    fn test_table_fill_bounds_checking() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(&store, ValType::Ref(RefType::FUNCREF), 5, None, None)
            .expect("Failed to create table");

        // Test fill that would exceed bounds
        let result = table.fill(&store, 3, TableElement::FuncRef(None), 5);
        assert!(result.is_err(), "Fill exceeding bounds should fail");

        // Test fill starting beyond bounds
        let result = table.fill(&store, 10, TableElement::FuncRef(None), 1);
        assert!(result.is_err(), "Fill starting beyond bounds should fail");

        // Valid fill should work
        let result = table.fill(&store, 0, TableElement::FuncRef(None), 5);
        assert!(result.is_ok(), "Valid fill should succeed");
    }

    #[test]
    fn test_invalid_element_type() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Test creating table with invalid element type (I32)
        let result = Table::new(&store, ValType::I32, 10, None, None);
        assert!(result.is_err());
    }

    #[test]
    fn test_type_mismatch() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let table = Table::new(&store, ValType::Ref(RefType::FUNCREF), 10, None, None)
            .expect("Failed to create table");

        // Test setting wrong element type
        let result = table.set(&store, 0, TableElement::ExternRef(None));
        assert!(result.is_err());
    }
}
