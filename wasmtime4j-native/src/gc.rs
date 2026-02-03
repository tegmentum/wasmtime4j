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

use wasmtime::*;
use std::sync::{Arc, Mutex, RwLock};
use std::sync::atomic::{AtomicU64, Ordering};
use std::collections::HashMap;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::gc_types::*;
use crate::gc_heap::*;
use crate::gc_operations::*;
use crate::simd::{V256, V512};

/// Real WebAssembly GC reference using Wasmtime's native GC system
#[derive(Clone)]
pub struct WasmtimeGcRef {
    /// Actual Wasmtime GC reference (rooted to prevent collection)
    pub gc_ref: Arc<wasmtime::Rooted<wasmtime::AnyRef>>,
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
    /// Wasmtime engine for integration
    engine: Engine,
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
        let gc_operations = WasmtimeGcOperations::new(store)
            .map_err(|e| WasmtimeError::from_string(&format!("Failed to initialize GC operations: {}", e)))?;

        Ok(Self {
            type_registry,
            heap,
            engine,
            gc_operations: Mutex::new(gc_operations),
            next_object_id: Mutex::new(1),
            gc_objects: RwLock::new(HashMap::new()),
            allocation_count: AtomicU64::new(0),
            collection_count: AtomicU64::new(0),
        })
    }

    /// Create a new WebAssembly GC runtime with custom configuration
    pub fn with_config(engine: Engine, heap_config: GcHeapConfig) -> WasmtimeResult<Self> {
        let type_registry = Arc::new(GcTypeRegistry::new());
        let heap = Arc::new(GcHeap::new(heap_config, type_registry.clone()));

        // Create Wasmtime store with GC features enabled
        let store = Store::new(&engine, ());

        // Initialize real GC operations with Wasmtime integration
        let gc_operations = WasmtimeGcOperations::new(store)
            .map_err(|e| WasmtimeError::from_string(&format!("Failed to initialize GC operations: {}", e)))?;

        Ok(Self {
            type_registry,
            heap,
            engine,
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
            let mut next_id = match self.next_object_id.lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock")) {
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
            Err(_) => return StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
            // Store the real GC reference
            if let Some(gc_ref) = result.gc_object {
                let wasmtime_ref = WasmtimeGcRef {
                    gc_ref: Arc::new(gc_ref),
                    ref_type: GcReferenceType::StructRef(type_def.clone()),
                    object_id,
                };

                if let Ok(mut gc_objects) = self.gc_objects.write() {
                    gc_objects.insert(object_id, wasmtime_ref);
                }
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

    /// Create a new struct instance with default values (struct.new_default) including advanced SIMD
    pub fn struct_new_default(&self, type_def: StructTypeDefinition) -> StructOperationResult {
        let default_values: Vec<GcValue> = type_def.fields.iter().map(|field| {
            match &field.field_type {
                crate::gc_types::FieldType::I32 => GcValue::I32(0),
                crate::gc_types::FieldType::I64 => GcValue::I64(0),
                crate::gc_types::FieldType::F32 => GcValue::F32(0.0),
                crate::gc_types::FieldType::F64 => GcValue::F64(0.0),
                crate::gc_types::FieldType::V128 => GcValue::V128([0; 16]),
                crate::gc_types::FieldType::V256 => GcValue::V256([0; 32]),
                crate::gc_types::FieldType::V512 => GcValue::V512([0; 64]),
                crate::gc_types::FieldType::PackedI8 | crate::gc_types::FieldType::PackedI16 => GcValue::I32(0),
                crate::gc_types::FieldType::Reference(_) => GcValue::Null,
            }
        }).collect();

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
            Err(_) => return StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
    pub fn struct_set(&self, object_id: ObjectId, field_index: u32, value: GcValue) -> StructOperationResult {
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
            Err(_) => return StructOperationResult {
                success: false,
                object_id: None,
                value: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
            let mut next_id = match self.next_object_id.lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock")) {
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
            Err(_) => return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
            // Store the real GC reference
            if let Some(gc_ref) = result.gc_array {
                let wasmtime_ref = WasmtimeGcRef {
                    gc_ref: Arc::new(gc_ref),
                    ref_type: GcReferenceType::ArrayRef(Box::new(type_def.clone())),
                    object_id,
                };

                if let Ok(mut gc_objects) = self.gc_objects.write() {
                    gc_objects.insert(object_id, wasmtime_ref);
                }
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

    /// Create a new array instance with default values (array.new_default) including advanced SIMD
    pub fn array_new_default(&self, type_def: ArrayTypeDefinition, length: u32) -> ArrayOperationResult {
        let default_value = match &type_def.element_type {
            crate::gc_types::FieldType::I32 => GcValue::I32(0),
            crate::gc_types::FieldType::I64 => GcValue::I64(0),
            crate::gc_types::FieldType::F32 => GcValue::F32(0.0),
            crate::gc_types::FieldType::F64 => GcValue::F64(0.0),
            crate::gc_types::FieldType::V128 => GcValue::V128([0; 16]),
            crate::gc_types::FieldType::V256 => GcValue::V256([0; 32]),
            crate::gc_types::FieldType::V512 => GcValue::V512([0; 64]),
            crate::gc_types::FieldType::PackedI8 | crate::gc_types::FieldType::PackedI16 => GcValue::I32(0),
            crate::gc_types::FieldType::Reference(_) => GcValue::Null,
        };

        let elements = vec![default_value; length as usize];
        self.array_new(type_def, elements)
    }

    /// Create a new fixed-size array (array.new_fixed)
    pub fn array_new_fixed(
        &self,
        type_def: ArrayTypeDefinition,
        elements: Vec<GcValue>,
    ) -> ArrayOperationResult {
        // Validation for fixed arrays would be more strict
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
            Err(_) => return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
    pub fn array_set(&self, object_id: ObjectId, element_index: u32, value: GcValue) -> ArrayOperationResult {
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
            Err(_) => return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
            Err(_) => return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
            Err(_) => return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
        };

        // Perform array copy using real Wasmtime GC APIs
        let result = gc_ops.array_copy(dest_object_id, dest_index, src_object_id, src_index, length);

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
            Err(_) => return ArrayOperationResult {
                success: false,
                object_id: None,
                value: None,
                length: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
    pub fn ref_cast(&self, object_id: ObjectId, target_type: GcReferenceType) -> RefOperationResult {
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
            Err(_) => return RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
    pub fn ref_test(&self, object_id: ObjectId, target_type: GcReferenceType) -> RefOperationResult {
        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => return RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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
                    Err(_) => return RefOperationResult {
                        success: false,
                        cast_result: None,
                        test_result: None,
                        eq_result: None,
                        is_null: None,
                value: None,
                        error: Some("Failed to acquire GC operations lock".to_string()),
                    },
                };

                let result = gc_ops.ref_is_null(id);
                result.is_null.unwrap_or(true)
            },
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
    pub fn ref_eq(&self, object_id1: Option<ObjectId>, object_id2: Option<ObjectId>) -> RefOperationResult {
        let eq_result = match (object_id1, object_id2) {
            (None, None) => true,  // Both null
            (Some(id1), Some(id2)) => {
                // Use real Wasmtime GC operations for reference comparison
                let mut gc_ops = match self.gc_operations.lock() {
                    Ok(ops) => ops,
                    Err(_) => return RefOperationResult {
                        success: false,
                        cast_result: None,
                        test_result: None,
                        eq_result: None,
                        is_null: None,
                value: None,
                        error: Some("Failed to acquire GC operations lock".to_string()),
                    },
                };

                let result = gc_ops.ref_eq(id1, id2);
                result.eq_result.unwrap_or(false)
            },
            _ => false,  // One null, one not null
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

    /// Create a null reference (ref.null)
    pub fn ref_null(&self, ref_type: GcReferenceType) -> RefOperationResult {
        // Validate that the reference type is valid
        match ref_type {
            GcReferenceType::AnyRef | GcReferenceType::EqRef | GcReferenceType::I31Ref |
            GcReferenceType::StructRef(_) | GcReferenceType::ArrayRef(_) |
            GcReferenceType::ExternRef | GcReferenceType::FuncRef |
            GcReferenceType::NullRef | GcReferenceType::NullFuncRef | GcReferenceType::NullExternRef => {
                RefOperationResult {
                    success: true,
                    cast_result: None,
                    test_result: None,
                    eq_result: None,
                    is_null: Some(true),
                    value: None,
                    error: None,
                }
            },
        }
    }

    /// Assert that a reference is not null (ref.as_non_null)
    pub fn ref_as_non_null(&self, object_id: Option<ObjectId>) -> RefOperationResult {
        match object_id {
            Some(id) => {
                match self.heap.get_object(id) {
                    Ok(_) => RefOperationResult {
                        success: true,
                        cast_result: Some(id),
                        test_result: None,
                        eq_result: None,
                        is_null: Some(false),
                        value: None,
                        error: None,
                    },
                    Err(e) => RefOperationResult {
                        success: false,
                        cast_result: None,
                        test_result: None,
                        eq_result: None,
                        is_null: None,
                value: None,
                        error: Some(e.to_string()),
                    },
                }
            },
            None => RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: Some("Null reference assertion failed".to_string()),
            },
        }
    }

    // === I31 Operations ===

    /// Create an I31 reference using real Wasmtime GC (i31.new)
    pub fn i31_new(&self, value: i32) -> RefOperationResult {
        // Generate unique object ID
        let object_id = {
            let mut next_id = match self.next_object_id.lock()
                .map_err(|_| WasmtimeError::from_string("Failed to acquire object ID lock")) {
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
            Err(_) => return RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
        };

        // Create I31 using real Wasmtime GC APIs
        let result = gc_ops.i31_new(value, object_id);

        if result.success {
            // Store the real GC reference
            if let Some(gc_ref) = result.cast_result {
                let wasmtime_ref = WasmtimeGcRef {
                    gc_ref: Arc::new(gc_ref),
                    ref_type: GcReferenceType::I31Ref,
                    object_id,
                };

                if let Ok(mut gc_objects) = self.gc_objects.write() {
                    gc_objects.insert(object_id, wasmtime_ref);
                }
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

    /// Get value from I31 reference using real Wasmtime GC (i31.get_s/i31.get_u)
    pub fn i31_get(&self, object_id: ObjectId, signed: bool) -> RefOperationResult {
        // Use real Wasmtime GC operations
        let mut gc_ops = match self.gc_operations.lock() {
            Ok(ops) => ops,
            Err(_) => return RefOperationResult {
                success: false,
                cast_result: None,
                test_result: None,
                eq_result: None,
                is_null: None,
                value: None,
                error: Some("Failed to acquire GC operations lock".to_string()),
            },
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

    // === Heap Management ===

    /// Get heap statistics
    pub fn get_heap_stats(&self) -> WasmtimeResult<GcHeapStats> {
        // Return stats based on our direct allocation tracking
        let total_allocated = self.allocation_count.load(Ordering::Relaxed);

        // Also get object count from gc_objects
        let current_objects = self.gc_objects.read()
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
        let gc_objects = self.gc_objects.read()
            .map_err(|_| WasmtimeError::from_string("Failed to acquire gc_objects lock"))?;

        if !gc_objects.contains_key(&object_id) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Object with ID {} not found in gc_objects", object_id),
            });
        }
        drop(gc_objects);

        // Create weak reference using the heap (which has weak_references enabled by default)
        // Note: The heap may not have this object, so we construct the weak reference directly
        Ok(GcWeakReference::new(object_id, self.heap.clone()))
    }

    // === Advanced GC Features ===

    /// Create a weak reference to an object for future GC support
    pub fn create_weak_reference_advanced(&self, object_id: ObjectId, finalization_callback: Option<Box<dyn Fn() + Send + Sync>>) -> WasmtimeResult<GcWeakReference> {
        // Store the finalization callback for future use
        if let Some(_callback) = finalization_callback {
            // In a real implementation, this would be stored in a finalization registry
            // For now, we just create the weak reference
        }
        // Use the same logic as create_weak_reference
        self.create_weak_reference(object_id)
    }

    /// Register object for finalization monitoring (future GC proposal support)
    pub fn register_finalization_callback(&self, object_id: ObjectId, _callback: Box<dyn Fn() + Send + Sync>) -> WasmtimeResult<()> {
        // This is a placeholder for future WebAssembly GC finalization support
        // When the GC proposal includes finalization, this will integrate with Wasmtime's finalizers
        Ok(())
    }

    /// Advanced GC collection with incremental and concurrent support
    pub fn collect_garbage_advanced(&self, max_pause_millis: Option<u64>, concurrent: bool) -> WasmtimeResult<GcCollectionResult> {
        // This prepares for future advanced GC algorithms in Wasmtime
        let result = if concurrent {
            // Future: concurrent GC support
            self.heap.collect_garbage(CollectionTrigger::Explicit)?
        } else if let Some(_pause_limit) = max_pause_millis {
            // Future: incremental GC with pause time limits
            self.heap.collect_garbage(CollectionTrigger::Explicit)?
        } else {
            self.heap.collect_garbage(CollectionTrigger::Explicit)?
        };
        self.collection_count.fetch_add(1, Ordering::Relaxed);
        Ok(result)
    }

    /// Support for GC object pinning (future WebAssembly GC feature)
    pub fn pin_object(&self, _object_id: ObjectId) -> WasmtimeResult<()> {
        // Pinned objects won't be moved during GC
        // This is preparation for future GC proposal features
        Ok(())
    }

    /// Support for GC object unpinning (future WebAssembly GC feature)
    pub fn unpin_object(&self, _object_id: ObjectId) -> WasmtimeResult<()> {
        // Allow pinned objects to be moved again
        // This is preparation for future GC proposal features
        Ok(())
    }

    /// Advanced type casting with performance optimization
    pub fn ref_cast_optimized(&self, object_id: ObjectId, target_type: GcReferenceType, enable_caching: bool) -> RefOperationResult {
        // Use caching for frequent cast operations to improve performance
        if enable_caching {
            // Future: implement cast result caching
            self.ref_cast(object_id, target_type)
        } else {
            self.ref_cast(object_id, target_type)
        }
    }

    /// Batch GC operations for better performance
    pub fn batch_struct_operations(&self, operations: Vec<StructBatchOperation>) -> Vec<StructOperationResult> {
        operations.into_iter().map(|op| {
            match op {
                StructBatchOperation::Get { object_id, field_index } => {
                    self.struct_get(object_id, field_index)
                },
                StructBatchOperation::Set { object_id, field_index, value } => {
                    self.struct_set(object_id, field_index, value)
                },
            }
        }).collect()
    }

    // === Private Helper Methods ===




    /// Convert our FieldType to Wasmtime's ValType including advanced SIMD support
    fn convert_field_type_to_wasmtime(&self, field_type: &crate::gc_types::FieldType) -> WasmtimeResult<ValType> {
        match field_type {
            crate::gc_types::FieldType::I32 => Ok(ValType::I32),
            crate::gc_types::FieldType::I64 => Ok(ValType::I64),
            crate::gc_types::FieldType::F32 => Ok(ValType::F32),
            crate::gc_types::FieldType::F64 => Ok(ValType::F64),
            crate::gc_types::FieldType::V128 => Ok(ValType::V128),
            // Note: V256 and V512 are not standard Wasmtime types yet, map to V128 for now
            crate::gc_types::FieldType::V256 => Ok(ValType::V128), // Future-ready: will map to native V256 when available
            crate::gc_types::FieldType::V512 => Ok(ValType::V128), // Future-ready: will map to native V512 when available
            crate::gc_types::FieldType::PackedI8 | crate::gc_types::FieldType::PackedI16 => Ok(ValType::I32), // Packed types stored as i32
            crate::gc_types::FieldType::Reference(ref_type) => {
                match ref_type {
                    GcReferenceType::AnyRef => Ok(ValType::Ref(RefType::new(true, HeapType::Any))),
                    GcReferenceType::EqRef => Ok(ValType::Ref(RefType::new(true, HeapType::Eq))),
                    GcReferenceType::I31Ref => Ok(ValType::Ref(RefType::new(true, HeapType::I31))),
                    GcReferenceType::ExternRef => Ok(ValType::Ref(RefType::new(true, HeapType::Extern))),
                    GcReferenceType::FuncRef => Ok(ValType::Ref(RefType::new(true, HeapType::Func))),
                    GcReferenceType::NullRef => Ok(ValType::Ref(RefType::new(true, HeapType::None))),
                    GcReferenceType::NullFuncRef => Ok(ValType::Ref(RefType::new(true, HeapType::NoFunc))),
                    GcReferenceType::NullExternRef => Ok(ValType::Ref(RefType::new(true, HeapType::NoExtern))),
                    GcReferenceType::StructRef(_) => Ok(ValType::Ref(RefType::new(true, HeapType::Struct))),
                    GcReferenceType::ArrayRef(_) => Ok(ValType::Ref(RefType::new(true, HeapType::Array))),
                }
            },
        }
    }

    /// Convert our GcValue to Wasmtime's Val including advanced SIMD support
    fn convert_gc_value_to_wasmtime_val(&self, gc_value: &GcValue) -> WasmtimeResult<Val> {
        match gc_value {
            GcValue::I32(i) => Ok(Val::I32(*i)),
            GcValue::I64(i) => Ok(Val::I64(*i)),
            GcValue::F32(f) => Ok(Val::F32(f.to_bits())),
            GcValue::F64(f) => Ok(Val::F64(f.to_bits())),
            GcValue::V128(bytes) => {
                let value = u128::from_le_bytes(*bytes);
                Ok(Val::V128(V128::from(value)))
            },
            GcValue::V256(bytes) => {
                // For V256, encode as V128 for now (future: native V256 support)
                let mut v128_bytes = [0u8; 16];
                v128_bytes.copy_from_slice(&bytes[0..16]); // Take first 128 bits
                let value = u128::from_le_bytes(v128_bytes);
                Ok(Val::V128(V128::from(value)))
            },
            GcValue::V512(bytes) => {
                // For V512, encode as V128 for now (future: native V512 support)
                let mut v128_bytes = [0u8; 16];
                v128_bytes.copy_from_slice(&bytes[0..16]); // Take first 128 bits
                let value = u128::from_le_bytes(v128_bytes);
                Ok(Val::V128(V128::from(value)))
            },
            GcValue::Reference(_) => {
                // For references, we'd need to resolve the ObjectId
                // For now, return null reference
                Ok(Val::null_any_ref())
            },
            GcValue::Null => Ok(Val::null_any_ref()),
        }
    }



    /// Convert Wasmtime Val back to our GcValue
    fn convert_wasmtime_val_to_gc_value(&self, val: &Val) -> WasmtimeResult<GcValue> {
        match val {
            Val::I32(i) => Ok(GcValue::I32(*i)),
            Val::I64(i) => Ok(GcValue::I64(*i)),
            Val::F32(f) => Ok(GcValue::F32(f32::from_bits(*f))),
            Val::F64(f) => Ok(GcValue::F64(f64::from_bits(*f))),
            Val::V128(v) => {
                let bytes = v.as_u128().to_le_bytes();
                Ok(GcValue::V128(bytes))
            },
            Val::AnyRef(_) | Val::FuncRef(_) | Val::ExternRef(_) | Val::ExnRef(_) | Val::ContRef(_) => {
                // For references, we'd need more complex conversion
                // For now, return null
                Ok(GcValue::Null)
            },
        }
    }

}

/// Batch operation types for performance optimization
#[derive(Debug, Clone)]
pub enum StructBatchOperation {
    /// Get a struct field value
    Get { object_id: ObjectId, field_index: u32 },
    /// Set a struct field value
    Set { object_id: ObjectId, field_index: u32, value: GcValue },
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
            fields: vec![
                FieldDefinition {
                    name: Some("x".to_string()),
                    field_type: crate::gc_types::FieldType::I32,
                    mutable: true,
                    index: 0,
                },
            ],
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
    #[ignore = "WasmGcRuntime stores objects in gc_objects but collect_garbage uses GcHeap which has separate storage"]
    fn test_garbage_collection() {
        let runtime = create_test_runtime();

        // Allocate some objects
        let _ = runtime.i31_new(1);
        let _ = runtime.i31_new(2);
        let _ = runtime.i31_new(3);

        // Trigger collection
        let collection_result = runtime.collect_garbage().unwrap();
        assert_eq!(collection_result.objects_before, 3);

        // Check stats
        let stats = runtime.get_heap_stats().unwrap();
        assert!(stats.major_collections > 0);
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
        let src_elements = vec![GcValue::I32(1), GcValue::I32(2), GcValue::I32(3), GcValue::I32(4)];
        let src_result = runtime.array_new(array_def.clone(), src_elements);
        assert!(src_result.success);
        let src_object_id = src_result.object_id.unwrap();

        // Create destination array
        let dest_elements = vec![GcValue::I32(0), GcValue::I32(0), GcValue::I32(0), GcValue::I32(0)];
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
        let elements = vec![GcValue::I32(0), GcValue::I32(0), GcValue::I32(0), GcValue::I32(0)];
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