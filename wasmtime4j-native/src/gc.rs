//! # WebAssembly GC Implementation
//!
//! This module provides a complete implementation of the WebAssembly GC proposal,
//! including all reference types, operations, and integration with Wasmtime's
//! garbage collection system.
//!
//! ## Features
//!
//! - Complete GC type system (structref, arrayref, i31ref, eqref, anyref)
//! - Struct operations (new, get, set, subtyping)
//! - Array operations (new, get, set, len)
//! - Reference type conversions (cast, test, eq, null checks)
//! - Heap management with garbage collection integration
//! - Java integration through JNI and Panama FFI
//!
//! ## Safety and Performance
//!
//! All operations are designed for safety and performance with comprehensive
//! validation and defensive programming patterns.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::gc_heap::*;
use crate::gc_operations::*;
use crate::gc_types::*;
use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use wasmtime::*;

/// Metadata for a WebAssembly GC reference managed by Wasmtime's native GC system.
///
/// The actual GC reference is held as an `OwnedRooted<T>` in `WasmtimeGcOperations::gc_objects`
/// which correctly manages the rooting lifetime. This struct tracks only the type information
/// and object identity needed for validation in `WasmGcRuntime`.
#[derive(Clone)]
pub struct WasmtimeGcRef {
    /// Type information for the reference
    pub ref_type: GcReferenceType,
    /// Object ID for tracking
    pub object_id: ObjectId,
}

/// Main WebAssembly GC implementation using Wasmtime's native GC system
pub struct WasmGcRuntime {
    /// Type registry for managing GC types
    type_registry: Arc<GcTypeRegistry>,
    /// Heap manager for GC objects
    heap: Arc<GcHeap>,
    /// Real Wasmtime GC operations manager
    gc_operations: Mutex<WasmtimeGcOperations>,
    /// Object ID counter for unique tracking
    next_object_id: Mutex<ObjectId>,
    /// Real GC object mapping from ObjectId to Wasmtime GC references
    gc_objects: RwLock<HashMap<ObjectId, WasmtimeGcRef>>,
    /// Direct stats tracking for Wasmtime GC allocations
    allocation_count: AtomicU64,
    /// Direct stats tracking for GC collections
    collection_count: AtomicU64,
}

/// GC operation result for struct operations
#[derive(Debug, Clone)]
pub struct StructOperationResult {
    /// Operation success status
    pub success: bool,
    /// Resulting object ID (for allocations)
    pub object_id: Option<ObjectId>,
    /// Resulting value (for field gets)
    pub value: Option<GcValue>,
    /// Error message if operation failed
    pub error: Option<String>,
}

/// GC operation result for array operations
#[derive(Debug, Clone)]
pub struct ArrayOperationResult {
    /// Operation success status
    pub success: bool,
    /// Resulting object ID (for allocations)
    pub object_id: Option<ObjectId>,
    /// Resulting value (for element gets)
    pub value: Option<GcValue>,
    /// Array length (for length queries)
    pub length: Option<u32>,
    /// Error message if operation failed
    pub error: Option<String>,
}

/// Reference type operation result
#[derive(Debug, Clone)]
pub struct RefOperationResult {
    /// Operation success status
    pub success: bool,
    /// Cast result (for ref.cast operations)
    pub cast_result: Option<ObjectId>,
    /// Test result (for ref.test operations)
    pub test_result: Option<bool>,
    /// Equality result (for ref.eq operations)
    pub eq_result: Option<bool>,
    /// Null check result (for ref.is_null operations)
    pub is_null: Option<bool>,
    /// Retrieved value (for operations like i31.get)
    pub value: Option<GcValue>,
    /// Error message if operation failed
    pub error: Option<String>,
}

impl WasmGcRuntime {
    /// Create a new WebAssembly GC runtime with real Wasmtime GC integration
    pub fn new(engine: Engine) -> WasmtimeResult<Self> {
        let type_registry = Arc::new(GcTypeRegistry::new());
        let heap_config = GcHeapConfig::default();
        let heap = Arc::new(GcHeap::new(heap_config, type_registry.clone()));

        // Create Wasmtime store with GC features enabled
        let store = Store::new(&engine, ());

        // Initialize real GC operations with Wasmtime integration
        let gc_operations = WasmtimeGcOperations::new(store).map_err(|e| {
            WasmtimeError::from_string(&format!("Failed to initialize GC operations: {}", e))
        })?;

        Ok(Self {
            type_registry,
            heap,
            gc_operations: Mutex::new(gc_operations),
            next_object_id: Mutex::new(1),
            gc_objects: RwLock::new(HashMap::new()),
            allocation_count: AtomicU64::new(0),
            collection_count: AtomicU64::new(0),
        })
    }

    // === Struct Operations ===

    /// Create a new struct instance using real Wasmtime GC (struct.new)
    pub fn struct_new(
        &self,
        type_def: StructTypeDefinition,
        field_values: Vec<GcValue>,
    ) -> StructOperationResult {
        // Generate unique object ID
        let object_id = {
            let mut next_id = match self
                .next_object_id
                .lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock"))
            {
                Ok(guard) => guard,
                Err(_) => {
                    return StructOperationResult {
                        success: false,
                        object_id: None,
                        value: None,
                        error: Some("Failed to acquire object ID lock".to_string()),
                    };
                }
            };
            let id = *next_id;
            *next_id += 1;
            id
        };

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return StructOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Register struct type with Wasmtime if not already registered
        if let Err(e) = gc_ops.register_struct_type(&type_def) {
            return StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: Some(format!("Failed to register struct type: {}", e)),
            };
        }

        // Create struct using real Wasmtime GC APIs
        let result = gc_ops.struct_new(&type_def, &field_values, object_id);

        if result.success {
            // Store metadata for type validation (actual GC ref is OwnedRooted in gc_operations)
            let wasmtime_ref = WasmtimeGcRef {
                ref_type: GcReferenceType::StructRef(type_def.clone()),
                object_id,
            };

            if let Ok(mut gc_objects) = self.gc_objects.write() {
                gc_objects.insert(object_id, wasmtime_ref);
            }

            // Increment allocation count
            self.allocation_count.fetch_add(1, Ordering::Relaxed);

            StructOperationResult {
                success: true,
                object_id: Some(object_id),
                value: None,
                error: None,
            }
        } else {
            StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: result.error,
            }
        }
    }

    /// Create a new struct instance using async resource limiter
    ///
    /// This is the async variant of [`struct_new`]. Uses `StructRef::new_async` which goes
    /// through the async resource limiter.
    #[cfg(feature = "async")]
    pub fn struct_new_async(
        &self,
        type_def: StructTypeDefinition,
        field_values: Vec<GcValue>,
    ) -> StructOperationResult {
        let object_id = {
            let mut next_id = match self
                .next_object_id
                .lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock"))
            {
                Ok(guard) => guard,
                Err(_) => {
                    return StructOperationResult {
                        success: false,
                        object_id: None,
                        value: None,
                        error: Some("Failed to acquire object ID lock".to_string()),
                    };
                }
            };
            let id = *next_id;
            *next_id += 1;
            id
        };

        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return StructOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        if let Err(e) = gc_ops.register_struct_type(&type_def) {
            return StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: Some(format!("Failed to register struct type: {}", e)),
            };
        }

        let result = gc_ops.struct_new_async(&type_def, &field_values, object_id);

        if result.success {
            let wasmtime_ref = WasmtimeGcRef {
                ref_type: GcReferenceType::StructRef(type_def.clone()),
                object_id,
            };

            if let Ok(mut gc_objects) = self.gc_objects.write() {
                gc_objects.insert(object_id, wasmtime_ref);
            }

            self.allocation_count.fetch_add(1, Ordering::Relaxed);

            StructOperationResult {
                success: true,
                object_id: Some(object_id),
                value: None,
                error: None,
            }
        } else {
            StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: result.error,
            }
        }
    }

    /// Create a new struct instance with default values (struct.new_default)
    pub fn struct_new_default(&self, type_def: StructTypeDefinition) -> StructOperationResult {
        let default_values: Vec<GcValue> = type_def
            .fields
            .iter()
            .map(|field| match &field.field_type {
                crate::gc_types::FieldType::I32 => GcValue::I32(0),
                crate::gc_types::FieldType::I64 => GcValue::I64(0),
                crate::gc_types::FieldType::F32 => GcValue::F32(0.0),
                crate::gc_types::FieldType::F64 => GcValue::F64(0.0),
                crate::gc_types::FieldType::V128 => GcValue::V128([0; 16]),
                crate::gc_types::FieldType::PackedI8 | crate::gc_types::FieldType::PackedI16 => {
                    GcValue::I32(0)
                }
                crate::gc_types::FieldType::Reference(_) => GcValue::Null,
            })
            .collect();

        self.struct_new(type_def, default_values)
    }

    /// Get a struct field value using real Wasmtime GC (struct.get)
    pub fn struct_get(&self, object_id: ObjectId, field_index: u32) -> StructOperationResult {
        // Validate object exists
        let gc_objects = match self.gc_objects.read() {
            Ok(objects) => objects,
            Err(_) => {
                return StructOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    error: Some("Failed to acquire GC objects lock".to_string()),
                };
            }
        };

        let wasmtime_ref = match gc_objects.get(&object_id) {
            Some(r) => r,
            None => {
                return StructOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    error: Some(format!("Object {} not found", object_id)),
                };
            }
        };

        // Validate it's a struct reference
        if !matches!(wasmtime_ref.ref_type, GcReferenceType::StructRef(_)) {
            return StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: Some("Object is not a struct".to_string()),
            };
        }

        drop(gc_objects); // Release lock before acquiring operations lock

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return StructOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Get field value using real Wasmtime GC APIs
        let result = gc_ops.struct_get(object_id, field_index);

        StructOperationResult {
            success: result.success,
            object_id: result.object_id,
            value: result.value,
            error: result.error,
        }
    }

    /// Set a struct field value using real Wasmtime GC (struct.set)
    pub fn struct_set(
        &self,
        object_id: ObjectId,
        field_index: u32,
        value: GcValue,
    ) -> StructOperationResult {
        // Validate object exists
        let gc_objects = match self.gc_objects.read() {
            Ok(objects) => objects,
            Err(_) => {
                return StructOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    error: Some("Failed to acquire GC objects lock".to_string()),
                };
            }
        };

        let wasmtime_ref = match gc_objects.get(&object_id) {
            Some(r) => r,
            None => {
                return StructOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    error: Some(format!("Object {} not found", object_id)),
                };
            }
        };

        // Validate it's a struct reference
        if !matches!(wasmtime_ref.ref_type, GcReferenceType::StructRef(_)) {
            return StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: Some("Object is not a struct".to_string()),
            };
        }

        drop(gc_objects); // Release lock before acquiring operations lock

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return StructOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Set field value using real Wasmtime GC APIs
        let result = gc_ops.struct_set(object_id, field_index, &value);

        StructOperationResult {
            success: result.success,
            object_id: None,
            value: None,
            error: result.error,
        }
    }

    // === Array Operations ===

    /// Create a new array instance using real Wasmtime GC (array.new)
    pub fn array_new(
        &self,
        type_def: ArrayTypeDefinition,
        elements: Vec<GcValue>,
    ) -> ArrayOperationResult {
        // Generate unique object ID
        let object_id = {
            let mut next_id = match self
                .next_object_id
                .lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock"))
            {
                Ok(guard) => guard,
                Err(_) => {
                    return ArrayOperationResult {
                        success: false,
                        object_id: None,
                        value: None,
                        length: None,
                        error: Some("Failed to acquire object ID lock".to_string()),
                    };
                }
            };
            let id = *next_id;
            *next_id += 1;
            id
        };

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Register array type with Wasmtime if not already registered
        if let Err(e) = gc_ops.register_array_type(&type_def) {
            return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some(format!("Failed to register array type: {}", e)),
            };
        }

        // Create array using real Wasmtime GC APIs
        let result = gc_ops.array_new(&type_def, &elements, object_id);

        if result.success {
            let wasmtime_ref = WasmtimeGcRef {
                ref_type: GcReferenceType::ArrayRef(Box::new(type_def.clone())),
                object_id,
            };

            if let Ok(mut gc_objects) = self.gc_objects.write() {
                gc_objects.insert(object_id, wasmtime_ref);
            }

            // Increment allocation count
            self.allocation_count.fetch_add(1, Ordering::Relaxed);

            ArrayOperationResult {
                success: true,
                object_id: Some(object_id),
                value: None,
                length: Some(elements.len() as u32),
                error: None,
            }
        } else {
            ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: result.error,
            }
        }
    }

    /// Create a new array instance using async resource limiter
    ///
    /// This is the async variant of [`array_new`]. Uses `ArrayRef::new_async` and
    /// `ArrayRef::new_fixed_async` which go through the async resource limiter.
    #[cfg(feature = "async")]
    pub fn array_new_async(
        &self,
        type_def: ArrayTypeDefinition,
        elements: Vec<GcValue>,
    ) -> ArrayOperationResult {
        let object_id = {
            let mut next_id = match self
                .next_object_id
                .lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock"))
            {
                Ok(guard) => guard,
                Err(_) => {
                    return ArrayOperationResult {
                        success: false,
                        object_id: None,
                        value: None,
                        length: None,
                        error: Some("Failed to acquire object ID lock".to_string()),
                    };
                }
            };
            let id = *next_id;
            *next_id += 1;
            id
        };

        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        if let Err(e) = gc_ops.register_array_type(&type_def) {
            return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some(format!("Failed to register array type: {}", e)),
            };
        }

        let result = gc_ops.array_new_async(&type_def, &elements, object_id);

        if result.success {
            let wasmtime_ref = WasmtimeGcRef {
                ref_type: GcReferenceType::ArrayRef(Box::new(type_def.clone())),
                object_id,
            };

            if let Ok(mut gc_objects) = self.gc_objects.write() {
                gc_objects.insert(object_id, wasmtime_ref);
            }

            self.allocation_count.fetch_add(1, Ordering::Relaxed);

            ArrayOperationResult {
                success: true,
                object_id: Some(object_id),
                value: None,
                length: Some(elements.len() as u32),
                error: None,
            }
        } else {
            ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: result.error,
            }
        }
    }

    /// Create a new array instance with default values (array.new_default) including advanced SIMD
    pub fn array_new_default(
        &self,
        type_def: ArrayTypeDefinition,
        length: u32,
    ) -> ArrayOperationResult {
        let default_value = match &type_def.element_type {
            crate::gc_types::FieldType::I32 => GcValue::I32(0),
            crate::gc_types::FieldType::I64 => GcValue::I64(0),
            crate::gc_types::FieldType::F32 => GcValue::F32(0.0),
            crate::gc_types::FieldType::F64 => GcValue::F64(0.0),
            crate::gc_types::FieldType::V128 => GcValue::V128([0; 16]),
            crate::gc_types::FieldType::PackedI8 | crate::gc_types::FieldType::PackedI16 => {
                GcValue::I32(0)
            }
            crate::gc_types::FieldType::Reference(_) => GcValue::Null,
        };

        let elements = vec![default_value; length as usize];
        self.array_new(type_def, elements)
    }

    /// Get an array element value using real Wasmtime GC (array.get)
    pub fn array_get(&self, object_id: ObjectId, element_index: u32) -> ArrayOperationResult {
        // Validate object exists
        let gc_objects = match self.gc_objects.read() {
            Ok(objects) => objects,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC objects lock".to_string()),
                };
            }
        };

        let wasmtime_ref = match gc_objects.get(&object_id) {
            Some(r) => r,
            None => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some(format!("Object {} not found", object_id)),
                };
            }
        };

        // Validate it's an array reference
        if !matches!(wasmtime_ref.ref_type, GcReferenceType::ArrayRef(_)) {
            return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Object is not an array".to_string()),
            };
        }

        drop(gc_objects); // Release lock before acquiring operations lock

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Get element value using real Wasmtime GC APIs
        let result = gc_ops.array_get(object_id, element_index);

        ArrayOperationResult {
            success: result.success,
            object_id: None,
            value: result.value,
            length: None,
            error: result.error,
        }
    }

    /// Set an array element value (array.set)
    pub fn array_set(
        &self,
        object_id: ObjectId,
        element_index: u32,
        value: GcValue,
    ) -> ArrayOperationResult {
        // Validate object exists
        let gc_objects = match self.gc_objects.read() {
            Ok(objects) => objects,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC objects lock".to_string()),
                };
            }
        };

        let wasmtime_ref = match gc_objects.get(&object_id) {
            Some(r) => r,
            None => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some(format!("Object {} not found", object_id)),
                };
            }
        };

        // Validate it's an array reference
        if !matches!(wasmtime_ref.ref_type, GcReferenceType::ArrayRef(_)) {
            return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Object is not an array".to_string()),
            };
        }

        drop(gc_objects); // Release lock before acquiring operations lock

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Set element value using real Wasmtime GC APIs
        let result = gc_ops.array_set(object_id, element_index, &value);

        ArrayOperationResult {
            success: result.success,
            object_id: None,
            value: None,
            length: None,
            error: result.error,
        }
    }

    /// Get array length using real Wasmtime GC (array.len)
    pub fn array_len(&self, object_id: ObjectId) -> ArrayOperationResult {
        // Validate object exists
        let gc_objects = match self.gc_objects.read() {
            Ok(objects) => objects,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC objects lock".to_string()),
                };
            }
        };

        let wasmtime_ref = match gc_objects.get(&object_id) {
            Some(r) => r,
            None => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some(format!("Object {} not found", object_id)),
                };
            }
        };

        // Validate it's an array reference
        if !matches!(wasmtime_ref.ref_type, GcReferenceType::ArrayRef(_)) {
            return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Object is not an array".to_string()),
            };
        }

        drop(gc_objects); // Release lock before acquiring operations lock

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Get array length using real Wasmtime GC APIs
        let result = gc_ops.array_len(object_id);

        ArrayOperationResult {
            success: result.success,
            object_id: None,
            value: None,
            length: result.length,
            error: result.error,
        }
    }

    /// Copy elements from one array to another using real Wasmtime GC (array.copy)
    pub fn array_copy(
        &self,
        dest_object_id: ObjectId,
        dest_index: u32,
        src_object_id: ObjectId,
        src_index: u32,
        length: u32,
    ) -> ArrayOperationResult {
        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Perform array copy using real Wasmtime GC APIs
        let result =
            gc_ops.array_copy(dest_object_id, dest_index, src_object_id, src_index, length);

        ArrayOperationResult {
            success: result.success,
            object_id: None,
            value: None,
            length: result.length,
            error: result.error,
        }
    }

    /// Fill array elements with a value using real Wasmtime GC (array.fill)
    pub fn array_fill(
        &self,
        object_id: ObjectId,
        start_index: u32,
        value: GcValue,
        length: u32,
    ) -> ArrayOperationResult {
        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return ArrayOperationResult {
                    success: false,
                    object_id: None,
                    value: None,
                    length: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Perform array fill using real Wasmtime GC APIs
        let result = gc_ops.array_fill(object_id, start_index, &value, length);

        ArrayOperationResult {
            success: result.success,
            object_id: None,
            value: None,
            length: result.length,
            error: result.error,
        }
    }

    // === Reference Type Operations ===

    /// Cast a reference to a specific type using real Wasmtime GC (ref.cast)
    pub fn ref_cast(
        &self,
        object_id: ObjectId,
        target_type: GcReferenceType,
    ) -> RefOperationResult {
        // Validate object exists
        let gc_objects = match self.gc_objects.read() {
            Ok(objects) => objects,
            Err(_) => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some("Failed to acquire GC objects lock".to_string()),
                };
            }
        };

        let _wasmtime_ref = match gc_objects.get(&object_id) {
            Some(r) => r,
            None => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some(format!("Object {} not found", object_id)),
                };
            }
        };

        drop(gc_objects); // Release lock before acquiring operations lock

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Perform cast using real Wasmtime GC APIs
        let result = gc_ops.ref_cast(object_id, &target_type);

        if result.success {
            RefOperationResult {
                success: true,
                cast_result: result.cast_object_id,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: None,
            }
        } else {
            RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: result.error,
            }
        }
    }

    /// Test if a reference is of a specific type using real Wasmtime GC (ref.test)
    pub fn ref_test(
        &self,
        object_id: ObjectId,
        target_type: GcReferenceType,
    ) -> RefOperationResult {
        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Perform type test using real Wasmtime GC APIs
        let result = gc_ops.ref_test(object_id, &target_type);

        RefOperationResult {
            success: result.success,
            cast_result: None,
            test_result: result.test_result,
            eq_result: None,
            is_null: None,
            value: None,
            error: result.error,
        }
    }

    /// Test if a reference is null using real Wasmtime GC (ref.is_null)
    pub fn ref_is_null(&self, object_id: Option<ObjectId>) -> RefOperationResult {
        let is_null = match object_id {
            None => true,
            Some(id) => {
                // Use real Wasmtime GC operations for null checking
                let gc_ops = match self.gc_operations.lock() {
                    Ok(ops) => ops,
                    Err(_) => {
                        return RefOperationResult {
                            success: false,
                            cast_result: None,
                            test_result: None,
                            eq_result: None,
                            is_null: None,
                            value: None,
                            error: Some("Failed to acquire GC operations lock".to_string()),
                        }
                    }
                };

                let result = gc_ops.ref_is_null(id);
                result.is_null.unwrap_or(true)
            }
        };

        RefOperationResult {
            success: true,
            cast_result: None,
            test_result: None,
            eq_result: None,
            is_null: Some(is_null),
            value: None,
            error: None,
        }
    }

    /// Compare two references for equality using real Wasmtime GC (ref.eq)
    pub fn ref_eq(
        &self,
        object_id1: Option<ObjectId>,
        object_id2: Option<ObjectId>,
    ) -> RefOperationResult {
        let eq_result = match (object_id1, object_id2) {
            (None, None) => true, // Both null
            (Some(id1), Some(id2)) => {
                // Use real Wasmtime GC operations for reference comparison
                let mut gc_ops = match self.gc_operations.lock() {
                    Ok(ops) => ops,
                    Err(_) => {
                        return RefOperationResult {
                            success: false,
                            cast_result: None,
                            test_result: None,
                            eq_result: None,
                            is_null: None,
                            value: None,
                            error: Some("Failed to acquire GC operations lock".to_string()),
                        }
                    }
                };

                let result = gc_ops.ref_eq(id1, id2);
                result.eq_result.unwrap_or(false)
            }
            _ => false, // One null, one not null
        };

        RefOperationResult {
            success: true,
            cast_result: None,
            test_result: None,
            eq_result: Some(eq_result),
            is_null: None,
            value: None,
            error: None,
        }
    }

    // === I31 Operations ===

    /// Create an I31 reference using real Wasmtime GC (i31.new)
    pub fn i31_new(&self, value: i32) -> RefOperationResult {
        // Generate unique object ID
        let object_id = {
            let mut next_id = match self
                .next_object_id
                .lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock"))
            {
                Ok(guard) => guard,
                Err(_) => {
                    return RefOperationResult {
                        success: false,
                        cast_result: None,
                        test_result: None,
                        eq_result: None,
                        is_null: None,
                        value: None,
                        error: Some("Failed to acquire object ID lock".to_string()),
                    };
                }
            };
            let id = *next_id;
            *next_id += 1;
            id
        };

        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Create I31 using real Wasmtime GC APIs
        let result = gc_ops.i31_new(value, object_id);

        if result.success {
            let wasmtime_ref = WasmtimeGcRef {
                ref_type: GcReferenceType::I31Ref,
                object_id,
            };

            if let Ok(mut gc_objects) = self.gc_objects.write() {
                gc_objects.insert(object_id, wasmtime_ref);
            }

            // Increment allocation count
            self.allocation_count.fetch_add(1, Ordering::Relaxed);

            RefOperationResult {
                success: true,
                cast_result: Some(object_id),
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: None,
            }
        } else {
            RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: result.error,
            }
        }
    }

    /// Create I31 from unsigned u32 (checked, returns error if > 2^31-1)
    pub fn i31_new_unsigned(&self, value: u32) -> RefOperationResult {
        let object_id = {
            let mut next_id = match self
                .next_object_id
                .lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock"))
            {
                Ok(guard) => guard,
                Err(_) => {
                    return RefOperationResult {
                        success: false,
                        cast_result: None,
                        test_result: None,
                        eq_result: None,
                        is_null: None,
                        value: None,
                        error: Some("Failed to acquire object ID lock".to_string()),
                    };
                }
            };
            let id = *next_id;
            *next_id += 1;
            id
        };

        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        let result = gc_ops.i31_new_unsigned(value, object_id);

        if result.success {
            let wasmtime_ref = WasmtimeGcRef {
                ref_type: GcReferenceType::I31Ref,
                object_id,
            };
            if let Ok(mut gc_objects) = self.gc_objects.write() {
                gc_objects.insert(object_id, wasmtime_ref);
            }
            self.allocation_count.fetch_add(1, Ordering::Relaxed);
            RefOperationResult {
                success: true,
                cast_result: Some(object_id),
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: None,
            }
        } else {
            RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: result.error,
            }
        }
    }

    /// Create I31 from signed i32 (wrapping, truncates to 31 bits)
    pub fn i31_wrapping_signed(&self, value: i32) -> RefOperationResult {
        let object_id = {
            let mut next_id = match self
                .next_object_id
                .lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock"))
            {
                Ok(guard) => guard,
                Err(_) => {
                    return RefOperationResult {
                        success: false,
                        cast_result: None,
                        test_result: None,
                        eq_result: None,
                        is_null: None,
                        value: None,
                        error: Some("Failed to acquire object ID lock".to_string()),
                    };
                }
            };
            let id = *next_id;
            *next_id += 1;
            id
        };

        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        let result = gc_ops.i31_wrapping_signed(value, object_id);

        if result.success {
            let wasmtime_ref = WasmtimeGcRef {
                ref_type: GcReferenceType::I31Ref,
                object_id,
            };
            if let Ok(mut gc_objects) = self.gc_objects.write() {
                gc_objects.insert(object_id, wasmtime_ref);
            }
            self.allocation_count.fetch_add(1, Ordering::Relaxed);
            RefOperationResult {
                success: true,
                cast_result: Some(object_id),
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: None,
            }
        } else {
            RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: result.error,
            }
        }
    }

    /// Create I31 from unsigned u32 (wrapping, truncates to 31 bits)
    pub fn i31_wrapping_unsigned(&self, value: u32) -> RefOperationResult {
        let object_id = {
            let mut next_id = match self
                .next_object_id
                .lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock"))
            {
                Ok(guard) => guard,
                Err(_) => {
                    return RefOperationResult {
                        success: false,
                        cast_result: None,
                        test_result: None,
                        eq_result: None,
                        is_null: None,
                        value: None,
                        error: Some("Failed to acquire object ID lock".to_string()),
                    };
                }
            };
            let id = *next_id;
            *next_id += 1;
            id
        };

        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        let result = gc_ops.i31_wrapping_unsigned(value, object_id);

        if result.success {
            let wasmtime_ref = WasmtimeGcRef {
                ref_type: GcReferenceType::I31Ref,
                object_id,
            };
            if let Ok(mut gc_objects) = self.gc_objects.write() {
                gc_objects.insert(object_id, wasmtime_ref);
            }
            self.allocation_count.fetch_add(1, Ordering::Relaxed);
            RefOperationResult {
                success: true,
                cast_result: Some(object_id),
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: None,
            }
        } else {
            RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: result.error,
            }
        }
    }

    /// Get value from I31 reference using real Wasmtime GC (i31.get_s/i31.get_u)
    pub fn i31_get(&self, object_id: ObjectId, signed: bool) -> RefOperationResult {
        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => {
                return RefOperationResult {
                    success: false,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: None,
                    value: None,
                    error: Some("Failed to acquire GC operations lock".to_string()),
                }
            }
        };

        // Get I31 value using real Wasmtime GC APIs
        let result = gc_ops.i31_get(object_id, signed);

        if result.success {
            RefOperationResult {
                success: true,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: result.value,
                error: None,
            }
        } else {
            RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: result.error,
            }
        }
    }

    // === Type System Operations ===

    /// Register a struct type
    pub fn register_struct_type(&self, definition: StructTypeDefinition) -> WasmtimeResult<u32> {
        self.type_registry.register_struct_type(definition)
    }

    /// Register an array type
    pub fn register_array_type(&self, definition: ArrayTypeDefinition) -> WasmtimeResult<u32> {
        self.type_registry.register_array_type(definition)
    }

    /// Get struct type definition
    pub fn get_struct_type(&self, type_id: u32) -> WasmtimeResult<StructTypeDefinition> {
        self.type_registry.get_struct_type(type_id)
    }

    /// Get array type definition
    pub fn get_array_type(&self, type_id: u32) -> WasmtimeResult<ArrayTypeDefinition> {
        self.type_registry.get_array_type(type_id)
    }

    /// Check subtype relationship
    pub fn is_subtype(&self, subtype_id: u32, supertype_id: u32) -> WasmtimeResult<bool> {
        self.type_registry.is_subtype(subtype_id, supertype_id)
    }

    /// Get the runtime type category of a GC object.
    /// Returns: 0=AnyRef, 1=EqRef, 2=I31Ref, 3=StructRef, 4=ArrayRef
    pub fn get_runtime_type(&self, object_id: ObjectId) -> WasmtimeResult<i32> {
        let gc_objects = self
            .gc_objects
            .read()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC objects lock"))?;

        let wasmtime_ref = gc_objects
            .get(&object_id)
            .ok_or_else(|| {
                WasmtimeError::from_string(&format!("Object {} not found", object_id))
            })?;

        match &wasmtime_ref.ref_type {
            GcReferenceType::AnyRef => Ok(0),
            GcReferenceType::EqRef => Ok(1),
            GcReferenceType::I31Ref => Ok(2),
            GcReferenceType::StructRef(_) => Ok(3),
            GcReferenceType::ArrayRef(_) => Ok(4),
            _ => Err(WasmtimeError::from_string(&format!(
                "Unsupported reference type for object {}",
                object_id
            ))),
        }
    }

    // === Object Lifecycle ===

    /// Release a GC object by ID, removing it from both the metadata map and the
    /// operations store. This allows the Wasmtime GC to collect the underlying
    /// reference, preventing native memory leaks in long-running processes.
    ///
    /// Returns `true` if the object existed and was released.
    pub fn release_object(&self, object_id: ObjectId) -> bool {
        // Remove from metadata map
        let removed_meta = if let Ok(mut gc_objects) = self.gc_objects.write() {
            gc_objects.remove(&object_id).is_some()
        } else {
            false
        };

        // Remove from operations store (the actual OwnedRooted reference)
        let removed_ref = if let Ok(mut gc_ops) = self.gc_operations.lock() {
            gc_ops.remove_object(object_id)
        } else {
            false
        };

        removed_meta || removed_ref
    }

    // === Heap Management ===

    /// Get heap statistics
    pub fn get_heap_stats(&self) -> WasmtimeResult<GcHeapStats> {
        // Return stats based on our direct allocation tracking
        let total_allocated = self.allocation_count.load(Ordering::Relaxed);

        // Also get object count from gc_objects
        let current_objects = self
            .gc_objects
            .read()
            .map(|objects| objects.len() as u64)
            .unwrap_or(0);

        let collection_count = self.collection_count.load(Ordering::Relaxed);

        let mut stats = GcHeapStats::default();
        stats.total_allocated = total_allocated;
        stats.current_heap_size = (current_objects * 32) as usize; // Rough estimate
        stats.major_collections = collection_count;
        Ok(stats)
    }

    /// Trigger garbage collection
    pub fn collect_garbage(&self) -> WasmtimeResult<GcCollectionResult> {
        let result = self.heap.collect_garbage(CollectionTrigger::Explicit)?;
        self.collection_count.fetch_add(1, Ordering::Relaxed);
        Ok(result)
    }

    /// Create weak reference
    pub fn create_weak_reference(&self, object_id: ObjectId) -> WasmtimeResult<GcWeakReference> {
        // Check that the object exists in gc_objects (where struct_new stores objects)
        let gc_objects = self
            .gc_objects
            .read()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire gc_objects lock"))?;

        if !gc_objects.contains_key(&object_id) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Object with ID {} not found in gc_objects", object_id),
            });
        }
        drop(gc_objects);

        // Create weak reference using the heap
        self.heap.create_weak_reference(object_id)
    }

    // === AnyRef Raw Conversion Operations ===

    /// Converts an AnyRef (identified by object_id) to its raw u32 representation.
    pub fn anyref_to_raw(&self, object_id: ObjectId) -> WasmtimeResult<u32> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.anyref_to_raw(object_id)
    }

    /// Creates an AnyRef from a raw u32 representation.
    pub fn anyref_from_raw(&self, raw: u32) -> WasmtimeResult<Option<ObjectId>> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.anyref_from_raw(raw)
    }

    /// Checks if an AnyRef matches a given HeapType.
    pub fn anyref_matches_ty(
        &self,
        object_id: ObjectId,
        heap_type: &wasmtime::HeapType,
    ) -> WasmtimeResult<bool> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.anyref_matches_ty(object_id, heap_type)
    }

    /// Converts an AnyRef to an ExternRef (extern.convert_any).
    pub fn externref_convert_any(&self, object_id: ObjectId) -> WasmtimeResult<Option<i64>> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.externref_convert_any(object_id)
    }

    /// Converts an ExternRef to an AnyRef (any.convert_extern).
    pub fn anyref_convert_extern(&self, externref_data: i64) -> WasmtimeResult<ObjectId> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.anyref_convert_extern(externref_data)
    }

    /// Gets the HeapType code for an EqRef.
    pub fn eqref_ty(&self, object_id: ObjectId) -> WasmtimeResult<i32> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.eqref_ty(object_id)
    }

    /// Checks if an EqRef matches a given HeapType.
    pub fn eqref_matches_ty(
        &self,
        object_id: ObjectId,
        heap_type: &wasmtime::HeapType,
    ) -> WasmtimeResult<bool> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.eqref_matches_ty(object_id, heap_type)
    }

    /// Checks if a StructRef matches a given HeapType.
    pub fn structref_matches_ty(
        &self,
        object_id: ObjectId,
        heap_type: &wasmtime::HeapType,
    ) -> WasmtimeResult<bool> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.structref_matches_ty(object_id, heap_type)
    }

    /// Checks if an ArrayRef matches a given HeapType.
    pub fn arrayref_matches_ty(
        &self,
        object_id: ObjectId,
        heap_type: &wasmtime::HeapType,
    ) -> WasmtimeResult<bool> {
        let mut gc_ops = self
            .gc_operations
            .lock()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire GC operations lock"))?;
        gc_ops.arrayref_matches_ty(object_id, heap_type)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::gc_types::{FieldDefinition, FieldType};

    // Use shared GC wasmtime engine to reduce wasmtime GLOBAL_CODE registry accumulation
    fn create_test_runtime() -> WasmGcRuntime {
        let engine = crate::engine::get_shared_gc_wasmtime_engine();
        WasmGcRuntime::new(engine).unwrap()
    }

    #[test]
    fn test_runtime_creation() {
        let runtime = create_test_runtime();
        let stats = runtime.get_heap_stats().unwrap();
        assert_eq!(stats.total_allocated, 0);
    }

    #[test]
    fn test_struct_operations() {
        let runtime = create_test_runtime();

        // Register struct type
        let struct_def = StructTypeDefinition {
            type_id: 0,
            fields: vec![FieldDefinition {
                name: Some("x".to_string()),
                field_type: crate::gc_types::FieldType::I32,
                mutable: true,
                index: 0,
            }],
            name: Some("TestStruct".to_string()),
            supertype: None,
        };

        let type_id = runtime.register_struct_type(struct_def.clone()).unwrap();
        let struct_def = runtime.get_struct_type(type_id).unwrap();

        // Create struct
        let values = vec![GcValue::I32(42)];
        let result = runtime.struct_new(struct_def, values);
        assert!(result.success);

        let object_id = result.object_id.unwrap();

        // Get field
        let get_result = runtime.struct_get(object_id, 0);
        assert!(get_result.success);

        if let Some(GcValue::I32(val)) = get_result.value {
            assert_eq!(val, 42);
        } else {
            panic!("Expected I32 value");
        }
    }

    #[test]
    fn test_array_operations() {
        let runtime = create_test_runtime();

        // Register array type
        let array_def = ArrayTypeDefinition {
            type_id: 0,
            element_type: crate::gc_types::FieldType::I32,
            mutable: true,
            name: Some("IntArray".to_string()),
        };

        let type_id = runtime.register_array_type(array_def.clone()).unwrap();
        let array_def = runtime.get_array_type(type_id).unwrap();

        // Create array
        let elements = vec![GcValue::I32(1), GcValue::I32(2), GcValue::I32(3)];
        let result = runtime.array_new(array_def, elements);
        assert!(result.success);

        let object_id = result.object_id.unwrap();

        // Get length
        let len_result = runtime.array_len(object_id);
        assert!(len_result.success);
        assert_eq!(len_result.length.unwrap(), 3);

        // Get element
        let get_result = runtime.array_get(object_id, 1);
        assert!(get_result.success);

        if let Some(GcValue::I32(val)) = get_result.value {
            assert_eq!(val, 2);
        } else {
            panic!("Expected I32 value");
        }
    }

    #[test]
    fn test_i31_operations() {
        let runtime = create_test_runtime();

        // Create I31
        let result = runtime.i31_new(42);
        assert!(result.success);

        let object_id = result.cast_result.unwrap();

        // Get value
        let get_result = runtime.i31_get(object_id, true);
        assert!(get_result.success);
    }

    #[test]
    fn test_reference_operations() {
        let runtime = create_test_runtime();

        // Create I31 for testing
        let i31_result = runtime.i31_new(42);
        let object_id = i31_result.cast_result.unwrap();

        // Test ref.test with i31ref
        let test_result = runtime.ref_test(object_id, GcReferenceType::I31Ref);
        assert!(test_result.success);
        assert_eq!(test_result.test_result.unwrap(), true);

        // Test ref.test with anyref (should be true)
        let test_result = runtime.ref_test(object_id, GcReferenceType::AnyRef);
        assert!(test_result.success);
        assert_eq!(test_result.test_result.unwrap(), true);

        // Test ref.is_null
        let null_result = runtime.ref_is_null(Some(object_id));
        assert!(null_result.success);
        assert_eq!(null_result.is_null.unwrap(), false);

        let null_result = runtime.ref_is_null(None);
        assert!(null_result.success);
        assert_eq!(null_result.is_null.unwrap(), true);
    }

    #[test]
    fn test_reference_equality() {
        let runtime = create_test_runtime();

        // Create two I31 values
        let result1 = runtime.i31_new(42);
        let result2 = runtime.i31_new(42);

        let id1 = result1.cast_result.unwrap();
        let id2 = result2.cast_result.unwrap();

        // Test ref.eq - different objects should not be equal (by identity)
        let eq_result = runtime.ref_eq(Some(id1), Some(id2));
        assert!(eq_result.success);
        assert_eq!(eq_result.eq_result.unwrap(), false);

        // Test ref.eq - same object should be equal
        let eq_result = runtime.ref_eq(Some(id1), Some(id1));
        assert!(eq_result.success);
        assert_eq!(eq_result.eq_result.unwrap(), true);

        // Test ref.eq - both null should be equal
        let eq_result = runtime.ref_eq(None, None);
        assert!(eq_result.success);
        assert_eq!(eq_result.eq_result.unwrap(), true);
    }

    #[test]
    fn test_array_copy_operation() {
        let runtime = create_test_runtime();

        // Register array type
        let array_def = ArrayTypeDefinition {
            type_id: 1,
            element_type: crate::gc_types::FieldType::I32,
            mutable: true,
            name: Some("IntArray".to_string()),
        };

        let type_id = runtime.register_array_type(array_def.clone()).unwrap();
        let array_def = runtime.get_array_type(type_id).unwrap();

        // Create source array
        let src_elements = vec![
            GcValue::I32(1),
            GcValue::I32(2),
            GcValue::I32(3),
            GcValue::I32(4),
        ];
        let src_result = runtime.array_new(array_def.clone(), src_elements);
        assert!(src_result.success);
        let src_object_id = src_result.object_id.unwrap();

        // Create destination array
        let dest_elements = vec![
            GcValue::I32(0),
            GcValue::I32(0),
            GcValue::I32(0),
            GcValue::I32(0),
        ];
        let dest_result = runtime.array_new(array_def, dest_elements);
        assert!(dest_result.success);
        let dest_object_id = dest_result.object_id.unwrap();

        // Test array copy
        let copy_result = runtime.array_copy(dest_object_id, 1, src_object_id, 0, 2);
        assert!(copy_result.success);

        // Verify copied elements
        let get_result1 = runtime.array_get(dest_object_id, 1);
        assert!(get_result1.success);
        if let Some(GcValue::I32(val)) = get_result1.value {
            assert_eq!(val, 1);
        } else {
            panic!("Expected I32 value");
        }

        let get_result2 = runtime.array_get(dest_object_id, 2);
        assert!(get_result2.success);
        if let Some(GcValue::I32(val)) = get_result2.value {
            assert_eq!(val, 2);
        } else {
            panic!("Expected I32 value");
        }
    }

    #[test]
    fn test_array_fill_operation() {
        let runtime = create_test_runtime();

        // Register array type
        let array_def = ArrayTypeDefinition {
            type_id: 1,
            element_type: crate::gc_types::FieldType::I32,
            mutable: true,
            name: Some("IntArray".to_string()),
        };

        let type_id = runtime.register_array_type(array_def.clone()).unwrap();
        let array_def = runtime.get_array_type(type_id).unwrap();

        // Create array
        let elements = vec![
            GcValue::I32(0),
            GcValue::I32(0),
            GcValue::I32(0),
            GcValue::I32(0),
        ];
        let result = runtime.array_new(array_def, elements);
        assert!(result.success);
        let object_id = result.object_id.unwrap();

        // Test array fill
        let fill_result = runtime.array_fill(object_id, 1, GcValue::I32(99), 2);
        assert!(fill_result.success);

        // Verify filled elements
        let get_result0 = runtime.array_get(object_id, 0);
        assert!(get_result0.success);
        if let Some(GcValue::I32(val)) = get_result0.value {
            assert_eq!(val, 0); // Should be unchanged
        } else {
            panic!("Expected I32 value");
        }

        let get_result1 = runtime.array_get(object_id, 1);
        assert!(get_result1.success);
        if let Some(GcValue::I32(val)) = get_result1.value {
            assert_eq!(val, 99); // Should be filled
        } else {
            panic!("Expected I32 value");
        }

        let get_result2 = runtime.array_get(object_id, 2);
        assert!(get_result2.success);
        if let Some(GcValue::I32(val)) = get_result2.value {
            assert_eq!(val, 99); // Should be filled
        } else {
            panic!("Expected I32 value");
        }

        let get_result3 = runtime.array_get(object_id, 3);
        assert!(get_result3.success);
        if let Some(GcValue::I32(val)) = get_result3.value {
            assert_eq!(val, 0); // Should be unchanged
        } else {
            panic!("Expected I32 value");
        }
    }

    #[test]
    fn test_i31_operations_with_value_return() {
        let runtime = create_test_runtime();

        // Create I31 reference
        let result = runtime.i31_new(42);
        assert!(result.success);
        let object_id = result.cast_result.unwrap();

        // Test signed get (i31.get_s)
        let get_result_signed = runtime.i31_get(object_id, true);
        assert!(get_result_signed.success);
        if let Some(GcValue::I32(val)) = get_result_signed.value {
            assert_eq!(val, 42);
        } else {
            panic!("Expected I32 value from i31.get_s");
        }

        // Test unsigned get (i31.get_u)
        let get_result_unsigned = runtime.i31_get(object_id, false);
        assert!(get_result_unsigned.success);
        if let Some(GcValue::I32(val)) = get_result_unsigned.value {
            assert_eq!(val, 42);
        } else {
            panic!("Expected I32 value from i31.get_u");
        }

        // Test with negative value
        let neg_result = runtime.i31_new(-100);
        assert!(neg_result.success);
        let neg_object_id = neg_result.cast_result.unwrap();

        // Test signed get with negative value
        let neg_get_signed = runtime.i31_get(neg_object_id, true);
        assert!(neg_get_signed.success);
        if let Some(GcValue::I32(val)) = neg_get_signed.value {
            assert_eq!(val, -100);
        } else {
            panic!("Expected I32 value from i31.get_s with negative");
        }

        // Test unsigned get with negative value (should mask to positive)
        let neg_get_unsigned = runtime.i31_get(neg_object_id, false);
        assert!(neg_get_unsigned.success);
        if let Some(GcValue::I32(val)) = neg_get_unsigned.value {
            // For unsigned, negative values should be masked
            assert_eq!(val, (-100i32) & 0x7FFFFFFF);
        } else {
            panic!("Expected I32 value from i31.get_u with negative");
        }
    }

    #[test]
    fn test_struct_new_default_operation() {
        let runtime = create_test_runtime();

        // Register struct type with multiple fields
        let struct_def = StructTypeDefinition {
            type_id: 1,
            fields: vec![
                FieldDefinition {
                    name: Some("x".to_string()),
                    field_type: crate::gc_types::FieldType::I32,
                    mutable: true,
                    index: 0,
                },
                FieldDefinition {
                    name: Some("y".to_string()),
                    field_type: crate::gc_types::FieldType::F64,
                    mutable: true,
                    index: 1,
                },
            ],
            name: Some("TestStruct".to_string()),
            supertype: None,
        };

        let type_id = runtime.register_struct_type(struct_def.clone()).unwrap();
        let struct_def = runtime.get_struct_type(type_id).unwrap();

        // Create struct with default values
        let result = runtime.struct_new_default(struct_def);
        assert!(result.success);
        let object_id = result.object_id.unwrap();

        // Verify default values
        let get_result_x = runtime.struct_get(object_id, 0);
        assert!(get_result_x.success);
        if let Some(GcValue::I32(val)) = get_result_x.value {
            assert_eq!(val, 0); // Default for I32
        } else {
            panic!("Expected I32 default value");
        }

        let get_result_y = runtime.struct_get(object_id, 1);
        assert!(get_result_y.success);
        if let Some(GcValue::F64(val)) = get_result_y.value {
            assert_eq!(val, 0.0); // Default for F64
        } else {
            panic!("Expected F64 default value");
        }
    }
}
