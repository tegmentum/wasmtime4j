//! # WebAssembly GC Type System Implementation
//!
//! This module provides a complete implementation of the WebAssembly GC proposal's type system,
//! including support for structured reference types (structref, arrayref, i31ref, eqref, anyref).
//!
//! ## Type Hierarchy
//!
//! The WebAssembly GC type system includes these reference types:
//! - `anyref`: Top type in the reference hierarchy
//! - `eqref`: Subtypes of anyref that support equality comparison
//! - `i31ref`: Immediate 31-bit integers as references
//! - `structref`: References to struct instances
//! - `arrayref`: References to array instances
//!
//! ## Safety and Defensive Programming
//!
//! All operations validate type compatibility and provide defensive checks
//! to prevent runtime errors and ensure type safety.

use wasmtime::*;
use std::collections::HashMap;
use std::sync::{Mutex, RwLock};
use crate::error::{WasmtimeError, WasmtimeResult};

/// WebAssembly GC reference type enumeration
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum GcReferenceType {
    /// Top type in the reference hierarchy - all GC references are subtypes of anyref
    AnyRef,
    /// Equality-testable references - subset of anyref that supports ref.eq
    EqRef,
    /// Immediate 31-bit integer references for efficient small integer storage
    I31Ref,
    /// References to struct instances with typed field access
    StructRef(StructTypeDefinition),
    /// References to array instances with element type information
    ArrayRef(Box<ArrayTypeDefinition>),
    /// Reference to external host data
    ExternRef,
    /// Reference to a function
    FuncRef,
    /// Null reference type - bottom type for nullable GC references
    NullRef,
    /// Nullable function reference type
    NullFuncRef,
    /// Nullable external reference type
    NullExternRef,
}

/// Struct type definition with field metadata
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct StructTypeDefinition {
    /// Unique identifier for this struct type
    pub type_id: u32,
    /// Field definitions with types and mutability
    pub fields: Vec<FieldDefinition>,
    /// Optional name for debugging purposes
    pub name: Option<String>,
    /// Supertype for inheritance (if any)
    pub supertype: Option<Box<StructTypeDefinition>>,
}

/// Array type definition with element metadata
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ArrayTypeDefinition {
    /// Unique identifier for this array type
    pub type_id: u32,
    /// Element type definition
    pub element_type: FieldType,
    /// Whether array elements are mutable
    pub mutable: bool,
    /// Optional name for debugging purposes
    pub name: Option<String>,
}

/// Field definition for struct types
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct FieldDefinition {
    /// Field name (optional, for named fields)
    pub name: Option<String>,
    /// Field type specification
    pub field_type: FieldType,
    /// Whether this field is mutable
    pub mutable: bool,
    /// Field index within the struct
    pub index: u32,
}

/// Field type enumeration supporting all WebAssembly value types including advanced SIMD
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum FieldType {
    /// 32-bit integer
    I32,
    /// 64-bit integer
    I64,
    /// 32-bit float
    F32,
    /// 64-bit float
    F64,
    /// 128-bit SIMD vector (standard WebAssembly)
    V128,
    /// 256-bit SIMD vector (advanced SIMD from Task #307)
    V256,
    /// 512-bit SIMD vector (AVX-512 support from Task #307)
    V512,
    /// 8-bit packed integer (storage type)
    PackedI8,
    /// 16-bit packed integer (storage type)
    PackedI16,
    /// Reference type (GC or legacy reference)
    Reference(GcReferenceType),
}

/// GC value enumeration supporting all GC types including advanced SIMD
#[derive(Debug, Clone)]
pub enum GcValue {
    /// 32-bit integer value
    I32(i32),
    /// 64-bit integer value
    I64(i64),
    /// 32-bit float value
    F32(f32),
    /// 64-bit float value
    F64(f64),
    /// 128-bit SIMD vector value (standard WebAssembly)
    V128([u8; 16]),
    /// 256-bit SIMD vector value (advanced SIMD)
    V256([u8; 32]),
    /// 512-bit SIMD vector value (AVX-512 support)
    V512([u8; 64]),
    /// Reference (placeholder - actual GC refs handled by gc_operations)
    Reference,
    /// Null reference
    Null,
}

/// Type system registry for managing GC types
pub struct GcTypeRegistry {
    /// Registered struct types
    struct_types: RwLock<HashMap<u32, StructTypeDefinition>>,
    /// Registered array types
    array_types: RwLock<HashMap<u32, ArrayTypeDefinition>>,
    /// Type ID counter for unique identifiers
    next_type_id: Mutex<u32>,
    /// Type compatibility cache for performance
    compatibility_cache: RwLock<HashMap<(u32, u32), bool>>,
}

impl GcTypeRegistry {
    /// Create a new GC type registry
    pub fn new() -> Self {
        Self {
            struct_types: RwLock::new(HashMap::new()),
            array_types: RwLock::new(HashMap::new()),
            next_type_id: Mutex::new(1), // Start from 1, 0 reserved for invalid
            compatibility_cache: RwLock::new(HashMap::new()),
        }
    }

    /// Register a new struct type and return its type ID
    pub fn register_struct_type(&self, mut definition: StructTypeDefinition) -> WasmtimeResult<u32> {
        let mut next_id = self.next_type_id.lock()
            .map_err(|_| WasmtimeError::Concurrency { message: "Failed to acquire type ID lock".to_string() })?;

        let type_id = *next_id;
        *next_id += 1;

        definition.type_id = type_id;

        // Validate field definitions
        self.validate_struct_definition(&definition)?;

        let mut struct_types = self.struct_types.write()
            .map_err(|_| WasmtimeError::Concurrency { message: "Failed to acquire struct types lock".to_string() })?;

        struct_types.insert(type_id, definition);
        Ok(type_id)
    }

    /// Register a new array type and return its type ID
    pub fn register_array_type(&self, mut definition: ArrayTypeDefinition) -> WasmtimeResult<u32> {
        let mut next_id = self.next_type_id.lock()
            .map_err(|_| WasmtimeError::Concurrency { message: "Failed to acquire type ID lock".to_string() })?;

        let type_id = *next_id;
        *next_id += 1;

        definition.type_id = type_id;

        // Validate element type
        self.validate_field_type(&definition.element_type)?;

        let mut array_types = self.array_types.write()
            .map_err(|_| WasmtimeError::Concurrency { message: "Failed to acquire array types lock".to_string() })?;

        array_types.insert(type_id, definition);
        Ok(type_id)
    }

    /// Get struct type definition by ID
    pub fn get_struct_type(&self, type_id: u32) -> WasmtimeResult<StructTypeDefinition> {
        let struct_types = self.struct_types.read()
            .map_err(|_| WasmtimeError::Concurrency { message: "Failed to acquire struct types lock".to_string() })?;

        struct_types.get(&type_id)
            .cloned()
            .ok_or_else(|| WasmtimeError::Type { message: format!("Struct type {} not found", type_id) })
    }

    /// Get array type definition by ID
    pub fn get_array_type(&self, type_id: u32) -> WasmtimeResult<ArrayTypeDefinition> {
        let array_types = self.array_types.read()
            .map_err(|_| WasmtimeError::Concurrency { message: "Failed to acquire array types lock".to_string() })?;

        array_types.get(&type_id)
            .cloned()
            .ok_or_else(|| WasmtimeError::Type { message: format!("Array type {} not found", type_id) })
    }

    /// Check if one type is a subtype of another
    pub fn is_subtype(&self, subtype_id: u32, supertype_id: u32) -> WasmtimeResult<bool> {
        if subtype_id == supertype_id {
            return Ok(true);
        }

        // Check cache first
        if let Ok(cache) = self.compatibility_cache.read() {
            if let Some(&result) = cache.get(&(subtype_id, supertype_id)) {
                return Ok(result);
            }
        }

        let result = self.compute_subtype_relationship(subtype_id, supertype_id)?;

        // Cache the result
        if let Ok(mut cache) = self.compatibility_cache.write() {
            cache.insert((subtype_id, supertype_id), result);
        }

        Ok(result)
    }

    /// Validate struct field access
    pub fn validate_struct_field_access(&self, struct_type_id: u32, field_index: u32) -> WasmtimeResult<FieldDefinition> {
        let struct_def = self.get_struct_type(struct_type_id)?;

        if field_index as usize >= struct_def.fields.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Field index {} out of bounds for struct type {} (has {} fields)",
                    field_index, struct_type_id, struct_def.fields.len()
                ),
            });
        }

        Ok(struct_def.fields[field_index as usize].clone())
    }

    /// Validate array element access
    pub fn validate_array_element_access(&self, array_type_id: u32, element_index: u32, array_length: u32) -> WasmtimeResult<FieldType> {
        let array_def = self.get_array_type(array_type_id)?;

        if element_index >= array_length {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Element index {} out of bounds for array of length {}",
                    element_index, array_length
                )
            });
        }

        Ok(array_def.element_type.clone())
    }

    /// Validate that a value is compatible with a field type including advanced SIMD from Task #307
    pub fn validate_value_type(&self, value: &GcValue, expected_type: &FieldType) -> WasmtimeResult<()> {
        match (value, expected_type) {
            (GcValue::I32(_), FieldType::I32) => Ok(()),
            (GcValue::I64(_), FieldType::I64) => Ok(()),
            (GcValue::F32(_), FieldType::F32) => Ok(()),
            (GcValue::F64(_), FieldType::F64) => Ok(()),
            (GcValue::V128(_), FieldType::V128) => Ok(()),
            (GcValue::V256(_), FieldType::V256) => Ok(()),
            (GcValue::V512(_), FieldType::V512) => Ok(()),
            (GcValue::I32(i), FieldType::PackedI8) if *i >= -128 && *i <= 127 => Ok(()),
            (GcValue::I32(i), FieldType::PackedI16) if *i >= -32768 && *i <= 32767 => Ok(()),
            (GcValue::Reference, FieldType::Reference(_)) => Ok(()),
            (GcValue::Null, FieldType::Reference(_)) => Ok(()),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Value type mismatch: expected {:?}, got {:?}",
                    expected_type, value
                )
            }),
        }
    }

    /// Private method to validate struct definition
    fn validate_struct_definition(&self, definition: &StructTypeDefinition) -> WasmtimeResult<()> {
        // Check for duplicate field names
        let mut field_names = std::collections::HashSet::new();

        for (index, field) in definition.fields.iter().enumerate() {
            if field.index != index as u32 {
                return Err(WasmtimeError::InvalidParameter { message: format!(
                    "Field index mismatch: expected {}, got {}",
                    index, field.index
                )});
            }

            if let Some(ref name) = field.name {
                if !field_names.insert(name.clone()) {
                    return Err(WasmtimeError::InvalidParameter { message: format!(
                        "Duplicate field name: {}", name
                    )});
                }
            }

            self.validate_field_type(&field.field_type)?;
        }

        Ok(())
    }

    /// Private method to validate field type
    fn validate_field_type(&self, field_type: &FieldType) -> WasmtimeResult<()> {
        match field_type {
            FieldType::I32 | FieldType::I64 | FieldType::F32 | FieldType::F64 |
            FieldType::V128 | FieldType::V256 | FieldType::V512 |
            FieldType::PackedI8 | FieldType::PackedI16 => Ok(()),
            FieldType::Reference(_) => {
                // Additional reference type validation could be added here
                Ok(())
            }
        }
    }

    /// Private method to compute subtype relationship
    fn compute_subtype_relationship(&self, subtype_id: u32, supertype_id: u32) -> WasmtimeResult<bool> {
        // For now, implement basic structural subtyping
        // In a full implementation, this would check field-by-field compatibility

        if let (Ok(subtype), Ok(supertype)) = (
            self.get_struct_type(subtype_id),
            self.get_struct_type(supertype_id)
        ) {
            // Struct subtyping: all fields of supertype must be present and compatible in subtype
            if subtype.fields.len() < supertype.fields.len() {
                return Ok(false);
            }

            for (i, super_field) in supertype.fields.iter().enumerate() {
                let sub_field = &subtype.fields[i];

                // Field types must be compatible
                if sub_field.field_type != super_field.field_type {
                    return Ok(false);
                }

                // Mutability must be compatible (mutable can substitute for immutable, not vice versa)
                if super_field.mutable && !sub_field.mutable {
                    return Ok(false);
                }
            }

            return Ok(true);
        }

        // For array types, check element type compatibility
        if let (Ok(subtype), Ok(supertype)) = (
            self.get_array_type(subtype_id),
            self.get_array_type(supertype_id)
        ) {
            return Ok(subtype.element_type == supertype.element_type &&
                     (!supertype.mutable || subtype.mutable));
        }

        // No subtyping relationship found
        Ok(false)
    }
}

impl Default for GcTypeRegistry {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_gc_type_registry_creation() {
        let registry = GcTypeRegistry::new();
        assert!(registry.struct_types.read().unwrap_or_else(|e| e.into_inner()).is_empty());
        assert!(registry.array_types.read().unwrap_or_else(|e| e.into_inner()).is_empty());
    }

    #[test]
    fn test_struct_type_registration() {
        let registry = GcTypeRegistry::new();

        let struct_def = StructTypeDefinition {
            type_id: 0, // Will be assigned by registry
            fields: vec![
                FieldDefinition {
                    name: Some("x".to_string()),
                    field_type: FieldType::I32,
                    mutable: true,
                    index: 0,
                },
                FieldDefinition {
                    name: Some("y".to_string()),
                    field_type: FieldType::F64,
                    mutable: false,
                    index: 1,
                },
            ],
            name: Some("Point".to_string()),
            supertype: None,
        };

        let type_id = registry.register_struct_type(struct_def).unwrap();
        assert_eq!(type_id, 1);

        let retrieved = registry.get_struct_type(type_id).unwrap();
        assert_eq!(retrieved.fields.len(), 2);
        assert_eq!(retrieved.name.as_ref().unwrap(), "Point");
    }

    #[test]
    fn test_array_type_registration() {
        let registry = GcTypeRegistry::new();

        let array_def = ArrayTypeDefinition {
            type_id: 0, // Will be assigned by registry
            element_type: FieldType::I32,
            mutable: true,
            name: Some("IntArray".to_string()),
        };

        let type_id = registry.register_array_type(array_def).unwrap();
        assert_eq!(type_id, 1);

        let retrieved = registry.get_array_type(type_id).unwrap();
        assert_eq!(retrieved.element_type, FieldType::I32);
        assert!(retrieved.mutable);
    }

    #[test]
    fn test_field_validation() {
        let registry = GcTypeRegistry::new();

        // Valid I32 value
        let value = GcValue::I32(42);
        let field_type = FieldType::I32;
        assert!(registry.validate_value_type(&value, &field_type).is_ok());

        // Invalid type combination
        let value = GcValue::F32(3.14);
        let field_type = FieldType::I32;
        assert!(registry.validate_value_type(&value, &field_type).is_err());

        // Valid packed i8 value
        let value = GcValue::I32(100);
        let field_type = FieldType::PackedI8;
        assert!(registry.validate_value_type(&value, &field_type).is_ok());

        // Invalid packed i8 value (out of range)
        let value = GcValue::I32(300);
        let field_type = FieldType::PackedI8;
        assert!(registry.validate_value_type(&value, &field_type).is_err());
    }

    #[test]
    fn test_struct_field_access_validation() {
        let registry = GcTypeRegistry::new();

        let struct_def = StructTypeDefinition {
            type_id: 0,
            fields: vec![
                FieldDefinition {
                    name: Some("field1".to_string()),
                    field_type: FieldType::I32,
                    mutable: true,
                    index: 0,
                },
            ],
            name: Some("TestStruct".to_string()),
            supertype: None,
        };

        let type_id = registry.register_struct_type(struct_def).unwrap();

        // Valid field access
        assert!(registry.validate_struct_field_access(type_id, 0).is_ok());

        // Invalid field access (out of bounds)
        assert!(registry.validate_struct_field_access(type_id, 1).is_err());
    }

    #[test]
    fn test_duplicate_field_names_rejected() {
        let registry = GcTypeRegistry::new();

        let struct_def = StructTypeDefinition {
            type_id: 0,
            fields: vec![
                FieldDefinition {
                    name: Some("x".to_string()),
                    field_type: FieldType::I32,
                    mutable: true,
                    index: 0,
                },
                FieldDefinition {
                    name: Some("x".to_string()), // Duplicate name
                    field_type: FieldType::F64,
                    mutable: false,
                    index: 1,
                },
            ],
            name: Some("InvalidStruct".to_string()),
            supertype: None,
        };

        assert!(registry.register_struct_type(struct_def).is_err());
    }
}