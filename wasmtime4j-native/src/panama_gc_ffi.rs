//! # Panama FFI Bindings for WebAssembly GC Operations
//!
//! This module provides Panama Foreign Function Interface bindings for WebAssembly GC
//! functionality, targeting Java 23+ with the Panama FFI (Foreign Function & Memory API).
//!
//! ## Safety and Performance
//!
//! All FFI functions implement defensive programming patterns and are designed for
//! high performance with minimal overhead. Memory management is handled safely
//! through Panama's memory segments.
//!
//! All FFI functions are wrapped with catch_unwind to prevent panics from crashing the JVM.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::gc::WasmGcRuntime;
use crate::gc_heap::ObjectId;
use crate::gc_types::{
    ArrayTypeDefinition, FieldDefinition, FieldType, GcReferenceType, GcValue, StructTypeDefinition,
};
use std::panic::AssertUnwindSafe;
use crate::error::ffi_utils::ffi_try_code;
use crate::ffi_boundary_result;

/// Converts a heap type code integer to a Wasmtime HeapType.
///
/// Delegates to the canonical implementation in ffi_common::heap_type_conversion.
pub fn heap_type_from_code(code: i32) -> Option<wasmtime::HeapType> {
    crate::ffi_common::heap_type_conversion::heap_type_from_code(code)
}

/// Converts a Wasmtime HeapType to an integer code matching the Java HeapType ordinal.
///
/// Delegates to the canonical implementation in ffi_common::heap_type_conversion.
pub fn heap_type_to_code(heap_type: &wasmtime::HeapType) -> i32 {
    crate::ffi_common::heap_type_conversion::heap_type_to_code(heap_type)
}

/// Panama FFI function for creating a GC runtime
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_create_runtime(engine_handle: i64) -> i64 {
    ffi_boundary_result!(0i64, {
        let runtime = create_gc_runtime_internal(engine_handle)?;
        let runtime_ptr = Box::into_raw(Box::new(runtime));
        Ok(runtime_ptr as i64)
    })
}

/// Panama FFI function for destroying a GC runtime
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_destroy_runtime(runtime_handle: i64) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        unsafe {
            let _ = Box::from_raw(runtime_handle as *mut WasmGcRuntime);
        }

        Ok(())
    }))
}

/// Panama FFI function for releasing a single GC object by ID.
///
/// Returns 1 if the object was found and released, 0 if not found, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_release_object(runtime_handle: i64, object_id: i64) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let _released = runtime.release_object(object_id as u64);

        Ok(())
    }))
}

/// Panama FFI function for registering a struct type
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_register_struct_type(
    runtime_handle: i64,
    name_ptr: *const u8,
    name_len: i32,
    field_count: i32,
    field_names_ptr: *const *const u8,
    field_name_lens: *const i32,
    field_types: *const i32,
    field_mutabilities: *const u8,
) -> i32 {
    ffi_boundary_result!(-1i32, {
        let type_id = register_struct_type_internal(
            runtime_handle,
            name_ptr,
            name_len,
            field_count,
            field_names_ptr,
            field_name_lens,
            field_types,
            field_mutabilities,
        )?;
        Ok(type_id as i32)
    })
}

/// Panama FFI function for registering an array type
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_register_array_type(
    runtime_handle: i64,
    name_ptr: *const u8,
    name_len: i32,
    element_type: i32,
    mutable: u8,
) -> i32 {
    ffi_boundary_result!(-1i32, {
        let type_id = register_array_type_internal(
            runtime_handle,
            name_ptr,
            name_len,
            element_type,
            mutable,
        )?;
        Ok(type_id as i32)
    })
}

/// Panama FFI function for creating a struct instance
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_struct_new(
    runtime_handle: i64,
    type_id: i32,
    field_values_ptr: *const i64,
    field_count: i32,
) -> i64 {
    ffi_boundary_result!(0i64, {
        let object_id =
            struct_new_internal(runtime_handle, type_id, field_values_ptr, field_count)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI function for creating a struct instance with default values
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_struct_new_default(runtime_handle: i64, type_id: i32) -> i64 {
    ffi_boundary_result!(0i64, {
        let object_id = struct_new_default_internal(runtime_handle, type_id)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI function for getting a struct field
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_struct_get(
    runtime_handle: i64,
    object_id: i64,
    field_index: i32,
    result_value: *mut i64,
    result_type: *mut i32,
    v128_out: *mut u8,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        let (value, value_type) =
            struct_get_internal(runtime_handle, object_id, field_index, v128_out)?;
        unsafe {
            *result_value = value;
            *result_type = value_type;
        }
        Ok(())
    }))
}

/// Panama FFI function for setting a struct field
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_struct_set(
    runtime_handle: i64,
    object_id: i64,
    field_index: i32,
    value: i64,
    value_type: i32,
    v128_ptr: *const u8,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        struct_set_internal(
            runtime_handle,
            object_id,
            field_index,
            value,
            value_type,
            v128_ptr,
        )?;
        Ok(())
    }))
}

/// Panama FFI function for creating an array instance
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_array_new(
    runtime_handle: i64,
    type_id: i32,
    elements_ptr: *const i64,
    element_count: i32,
) -> i64 {
    ffi_boundary_result!(0i64, {
        let object_id = array_new_internal(runtime_handle, type_id, elements_ptr, element_count)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI function for creating an array instance with default values
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_array_new_default(
    runtime_handle: i64,
    type_id: i32,
    length: i32,
) -> i64 {
    ffi_boundary_result!(0i64, {
        let object_id = array_new_default_internal(runtime_handle, type_id, length)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI function for getting an array element
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_array_get(
    runtime_handle: i64,
    object_id: i64,
    element_index: i32,
    result_value: *mut i64,
    result_type: *mut i32,
    v128_out: *mut u8,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        let (value, value_type) =
            array_get_internal(runtime_handle, object_id, element_index, v128_out)?;
        unsafe {
            *result_value = value;
            *result_type = value_type;
        }
        Ok(())
    }))
}

/// Panama FFI function for setting an array element
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_array_set(
    runtime_handle: i64,
    object_id: i64,
    element_index: i32,
    value: i64,
    value_type: i32,
    v128_ptr: *const u8,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        array_set_internal(
            runtime_handle,
            object_id,
            element_index,
            value,
            value_type,
            v128_ptr,
        )?;
        Ok(())
    }))
}

/// Panama FFI function for getting array length
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_array_len(runtime_handle: i64, object_id: i64) -> i32 {
    ffi_boundary_result!(-1i32, {
        let length = array_len_internal(runtime_handle, object_id)?;
        Ok(length as i32)
    })
}

/// Panama FFI function for creating an I31 instance
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_i31_new(runtime_handle: i64, value: i32) -> i64 {
    ffi_boundary_result!(0i64, {
        let object_id = i31_new_internal(runtime_handle, value)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI function for creating I31 from unsigned u32 (checked).
/// Returns 0 if value > 2^31-1.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_i31_new_unsigned(runtime_handle: i64, value: u32) -> i64 {
    ffi_boundary_result!(0i64, {
        let object_id = i31_new_unsigned_internal(runtime_handle, value)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI function for creating I31 from signed i32 (wrapping, truncates to 31 bits).
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_i31_wrapping_signed(runtime_handle: i64, value: i32) -> i64 {
    ffi_boundary_result!(0i64, {
        let object_id = i31_wrapping_signed_internal(runtime_handle, value)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI function for creating I31 from unsigned u32 (wrapping, truncates to 31 bits).
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_i31_wrapping_unsigned(runtime_handle: i64, value: u32) -> i64 {
    ffi_boundary_result!(0i64, {
        let object_id = i31_wrapping_unsigned_internal(runtime_handle, value)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI function for getting I31 value
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_i31_get(runtime_handle: i64, object_id: i64, signed: u8) -> i32 {
    ffi_boundary_result!(0i32, {
        i31_get_internal(runtime_handle, object_id, signed)
    })
}

/// Panama FFI function for reference cast
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_ref_cast(
    runtime_handle: i64,
    object_id: i64,
    target_type: i32,
) -> i64 {
    ffi_boundary_result!(0i64, {
        let cast_object_id = ref_cast_internal(runtime_handle, object_id, target_type)?;
        Ok(cast_object_id as i64)
    })
}

/// Panama FFI function for reference test
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_ref_test(
    runtime_handle: i64,
    object_id: i64,
    target_type: i32,
) -> u8 {
    ffi_boundary_result!(0u8, {
        let test_result = ref_test_internal(runtime_handle, object_id, target_type)?;
        Ok(if test_result { 1 } else { 0 })
    })
}

/// Panama FFI function for reference equality
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_ref_eq(
    runtime_handle: i64,
    object_id1: i64,
    object_id2: i64,
) -> u8 {
    ffi_boundary_result!(0u8, {
        let eq_result = ref_eq_internal(runtime_handle, object_id1, object_id2)?;
        Ok(if eq_result { 1 } else { 0 })
    })
}

/// Panama FFI function for null check
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_ref_is_null(runtime_handle: i64, object_id: i64) -> u8 {
    ffi_boundary_result!(0u8, {
        let is_null = ref_is_null_internal(runtime_handle, object_id)?;
        Ok(if is_null { 1 } else { 0 })
    })
}

/// Panama FFI function for garbage collection
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_collect_garbage(
    runtime_handle: i64,
    stats_ptr: *mut GcStatsFFI,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        let stats = collect_garbage_internal(runtime_handle)?;
        if !stats_ptr.is_null() {
            unsafe {
                *stats_ptr = stats;
            }
        }
        Ok(())
    }))
}

/// Panama FFI function for getting GC stats
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_get_stats(runtime_handle: i64, stats_ptr: *mut GcStatsFFI) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        let stats = get_gc_stats_internal(runtime_handle)?;
        if !stats_ptr.is_null() {
            unsafe {
                *stats_ptr = stats;
            }
        }
        Ok(())
    }))
}

/// FFI-compatible GC statistics structure
#[repr(C)]
#[derive(Clone, Copy)]
pub struct GcStatsFFI {
    pub total_allocated: i64,
    pub total_collected: i64,
    pub bytes_allocated: i64,
    pub bytes_collected: i64,
    pub minor_collections: i64,
    pub major_collections: i64,
    pub total_gc_time_nanos: i64,
    pub current_heap_size: i32,
    pub peak_heap_size: i32,
    pub max_heap_size: i32,
}

// Internal implementation functions

fn create_gc_runtime_internal(engine_handle: i64) -> WasmtimeResult<WasmGcRuntime> {
    if engine_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid engine handle".to_string(),
        });
    }

    // Use the shared wasmtime engine which has GC enabled
    let engine = crate::engine::get_shared_gc_wasmtime_engine();
    WasmGcRuntime::new(engine)
}

fn register_struct_type_internal(
    runtime_handle: i64,
    name_ptr: *const u8,
    name_len: i32,
    field_count: i32,
    field_names_ptr: *const *const u8,
    field_name_lens: *const i32,
    field_types: *const i32,
    field_mutabilities: *const u8,
) -> WasmtimeResult<u32> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert name from raw pointer
    let name_str = if name_ptr.is_null() || name_len <= 0 {
        "Unnamed".to_string()
    } else {
        unsafe {
            let name_slice = std::slice::from_raw_parts(name_ptr, name_len as usize);
            String::from_utf8_lossy(name_slice).to_string()
        }
    };

    let mut fields = Vec::with_capacity(field_count as usize);

    for i in 0..field_count as usize {
        // Get field name
        let field_name = if !field_names_ptr.is_null() && !field_name_lens.is_null() {
            unsafe {
                let name_ptr = *field_names_ptr.add(i);
                let name_len = *field_name_lens.add(i);

                if !name_ptr.is_null() && name_len > 0 {
                    let name_slice = std::slice::from_raw_parts(name_ptr, name_len as usize);
                    Some(String::from_utf8_lossy(name_slice).to_string())
                } else {
                    None
                }
            }
        } else {
            None
        };

        // Get field type
        let field_type = if !field_types.is_null() {
            unsafe {
                match *field_types.add(i) {
                    0 => FieldType::I32,
                    1 => FieldType::I64,
                    2 => FieldType::F32,
                    3 => FieldType::F64,
                    4 => FieldType::V128,
                    5 => FieldType::PackedI8,
                    6 => FieldType::PackedI16,
                    7 => FieldType::Reference(GcReferenceType::AnyRef),
                    other => {
                        return Err(WasmtimeError::InvalidParameter {
                            message: format!("Unknown GC field type code: {}", other),
                        });
                    }
                }
            }
        } else {
            FieldType::I32
        };

        // Get field mutability
        let mutable = if !field_mutabilities.is_null() {
            unsafe { *field_mutabilities.add(i) != 0 }
        } else {
            true
        };

        fields.push(FieldDefinition {
            name: field_name,
            field_type,
            mutable,
            index: i as u32,
        });
    }

    let struct_def = StructTypeDefinition {
        type_id: 0, // Will be assigned by runtime
        fields,
        name: Some(name_str),
        supertype: None,
    };

    runtime.register_struct_type(struct_def)
}

fn register_array_type_internal(
    runtime_handle: i64,
    name_ptr: *const u8,
    name_len: i32,
    element_type: i32,
    mutable: u8,
) -> WasmtimeResult<u32> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let name_str = if name_ptr.is_null() || name_len <= 0 {
        "UnnamedArray".to_string()
    } else {
        unsafe {
            let name_slice = std::slice::from_raw_parts(name_ptr, name_len as usize);
            String::from_utf8_lossy(name_slice).to_string()
        }
    };

    let field_type = match element_type {
        0 => FieldType::I32,
        1 => FieldType::I64,
        2 => FieldType::F32,
        3 => FieldType::F64,
        4 => FieldType::V128,
        5 => FieldType::PackedI8,
        6 => FieldType::PackedI16,
        7 => FieldType::Reference(GcReferenceType::AnyRef), // Default reference type
        _ => {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid element type: {}", element_type),
            })
        }
    };

    let array_def = ArrayTypeDefinition {
        type_id: 0, // Will be assigned by runtime
        element_type: field_type,
        mutable: mutable != 0,
        name: Some(name_str),
    };

    runtime.register_array_type(array_def)
}

fn struct_new_internal(
    runtime_handle: i64,
    type_id: i32,
    field_values_ptr: *const i64,
    field_count: i32,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let struct_def = runtime.get_struct_type(type_id as u32)?;

    let mut values = Vec::with_capacity(field_count as usize);

    if !field_values_ptr.is_null() && field_count > 0 {
        unsafe {
            let values_slice = std::slice::from_raw_parts(field_values_ptr, field_count as usize);
            for (i, &raw_value) in values_slice.iter().enumerate() {
                // Convert based on the field type from the struct definition
                let field_type = if i < struct_def.fields.len() {
                    &struct_def.fields[i].field_type
                } else {
                    &FieldType::I32
                };
                let gc_value = if i < struct_def.fields.len() {
                    match field_type {
                        FieldType::I32 => GcValue::I32(raw_value as i32),
                        FieldType::I64 => GcValue::I64(raw_value),
                        FieldType::F32 => GcValue::F32(f32::from_bits(raw_value as u32)),
                        FieldType::F64 => GcValue::F64(f64::from_bits(raw_value as u64)),
                        FieldType::V128 => {
                            // raw_value is a pointer to 16 bytes of V128 data
                            if raw_value == 0 {
                                GcValue::V128([0u8; 16])
                            } else {
                                let v128_ptr = raw_value as *const u8;
                                let mut bytes = [0u8; 16];
                                std::ptr::copy_nonoverlapping(v128_ptr, bytes.as_mut_ptr(), 16);
                                GcValue::V128(bytes)
                            }
                        }
                        FieldType::PackedI8 => GcValue::I32(raw_value as i32),
                        FieldType::PackedI16 => GcValue::I32(raw_value as i32),
                        // Pass reference object IDs as ObjectRef for gc_operations lookup
                        // If raw_value is 0, treat as null reference
                        FieldType::Reference(_) => {
                            if raw_value == 0 {
                                GcValue::Null
                            } else {
                                GcValue::ObjectRef(raw_value as u64)
                            }
                        }
                    }
                } else {
                    GcValue::I32(raw_value as i32)
                };
                values.push(gc_value);
            }
        }
    } else {
        // Create default values based on field types
        for field in &struct_def.fields {
            let default_value = match &field.field_type {
                FieldType::I32 => GcValue::I32(0),
                FieldType::I64 => GcValue::I64(0),
                FieldType::F32 => GcValue::F32(0.0),
                FieldType::F64 => GcValue::F64(0.0),
                FieldType::V128 => GcValue::V128([0u8; 16]),
                FieldType::PackedI8 => GcValue::I32(0),
                FieldType::PackedI16 => GcValue::I32(0),
                FieldType::Reference(_) => GcValue::Null, // Null reference for default
            };
            values.push(default_value);
        }
    }

    let result = runtime.struct_new(struct_def, values);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn struct_new_default_internal(runtime_handle: i64, type_id: i32) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let struct_def = runtime.get_struct_type(type_id as u32)?;
    let result = runtime.struct_new_default(struct_def);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn struct_get_internal(
    runtime_handle: i64,
    object_id: i64,
    field_index: i32,
    v128_out: *mut u8,
) -> WasmtimeResult<(i64, i32)> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.struct_get(object_id as ObjectId, field_index as u32);

    if result.success {
        // Check if this is a reference field (object_id is set)
        if let Some(ref_object_id) = result.object_id {
            // Reference field - return object_id as value with type 5 (Reference)
            return Ok((ref_object_id as i64, 5));
        }

        if let Some(value) = result.value {
            match value {
                GcValue::I32(i) => Ok((i as i64, 0)),              // Type 0 = I32
                GcValue::I64(i) => Ok((i, 1)),                     // Type 1 = I64
                GcValue::F32(f) => Ok((f.to_bits() as i64, 2)),    // Type 2 = F32
                GcValue::F64(f) => Ok((f.to_bits() as i64, 3)),    // Type 3 = F64
                GcValue::V128(bytes) => {
                    // Write V128 bytes to the output buffer
                    if !v128_out.is_null() {
                        unsafe {
                            std::ptr::copy_nonoverlapping(bytes.as_ptr(), v128_out, 16);
                        }
                    }
                    Ok((0, 4)) // Type 4 = V128
                }
                GcValue::ObjectRef(id) => Ok((id as i64, 5)),      // Type 5 = Reference
                GcValue::Null => Ok((0, 6)),                       // Type 6 = Null
            }
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: "No value returned".to_string(),
            })
        }
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn struct_set_internal(
    runtime_handle: i64,
    object_id: i64,
    field_index: i32,
    value: i64,
    value_type: i32,
    v128_ptr: *const u8,
) -> WasmtimeResult<()> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let gc_value = match value_type {
        0 => GcValue::I32(value as i32),
        1 => GcValue::I64(value),
        2 => GcValue::F32(f32::from_bits(value as u32)),
        3 => GcValue::F64(f64::from_bits(value as u64)),
        4 => {
            // V128 type - read 16 bytes from v128_ptr
            if v128_ptr.is_null() {
                GcValue::V128([0u8; 16])
            } else {
                let mut bytes = [0u8; 16];
                unsafe {
                    std::ptr::copy_nonoverlapping(v128_ptr, bytes.as_mut_ptr(), 16);
                }
                GcValue::V128(bytes)
            }
        }
        5 => {
            // Reference type
            if value == 0 {
                GcValue::Null
            } else {
                GcValue::ObjectRef(value as u64)
            }
        }
        6 => GcValue::Null, // Null type
        other => {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Unknown GC value type code: {}", other),
            });
        }
    };

    let result = runtime.struct_set(object_id as ObjectId, field_index as u32, gc_value);

    if result.success {
        Ok(())
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

// Simplified implementations for other functions (following similar patterns)
fn array_new_internal(
    runtime_handle: i64,
    type_id: i32,
    elements_ptr: *const i64,
    element_count: i32,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Get array type
    let array_def = runtime.get_array_type(type_id as u32)?;

    // Convert elements
    let mut gc_elements = Vec::with_capacity(element_count as usize);

    if !elements_ptr.is_null() && element_count > 0 {
        unsafe {
            let elements_slice = std::slice::from_raw_parts(elements_ptr, element_count as usize);
            for &element in elements_slice {
                // Convert based on element type
                let gc_value = match &array_def.element_type {
                    FieldType::I32 => GcValue::I32(element as i32),
                    FieldType::I64 => GcValue::I64(element),
                    FieldType::F32 => GcValue::F32(f32::from_bits(element as u32)),
                    FieldType::F64 => GcValue::F64(f64::from_bits(element as u64)),
                    FieldType::V128 => {
                        // element is a pointer to 16 bytes of V128 data
                        if element == 0 {
                            GcValue::V128([0u8; 16])
                        } else {
                            let v128_ptr = element as *const u8;
                            let mut bytes = [0u8; 16];
                            std::ptr::copy_nonoverlapping(v128_ptr, bytes.as_mut_ptr(), 16);
                            GcValue::V128(bytes)
                        }
                    }
                    FieldType::PackedI8 | FieldType::PackedI16 => {
                        GcValue::I32(element as i32)
                    }
                    FieldType::Reference(_) => {
                        if element == 0 {
                            GcValue::Null
                        } else {
                            GcValue::ObjectRef(element as u64)
                        }
                    }
                };
                gc_elements.push(gc_value);
            }
        }
    }

    let result = runtime.array_new(array_def, gc_elements);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn array_new_default_internal(
    runtime_handle: i64,
    type_id: i32,
    length: i32,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Get array type
    let array_def = runtime.get_array_type(type_id as u32)?;

    // Create array with default values
    let result = runtime.array_new_default(array_def, length as u32);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn array_get_internal(
    runtime_handle: i64,
    object_id: i64,
    element_index: i32,
    v128_out: *mut u8,
) -> WasmtimeResult<(i64, i32)> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.array_get(object_id as ObjectId, element_index as u32);

    if result.success {
        if let Some(value) = result.value {
            match value {
                GcValue::I32(i) => Ok((i as i64, 0)),
                GcValue::I64(i) => Ok((i, 1)),
                GcValue::F32(f) => Ok((f.to_bits() as i64, 2)),
                GcValue::F64(f) => Ok((f.to_bits() as i64, 3)),
                GcValue::V128(bytes) => {
                    if !v128_out.is_null() {
                        unsafe {
                            std::ptr::copy_nonoverlapping(bytes.as_ptr(), v128_out, 16);
                        }
                    }
                    Ok((0, 4)) // Type 4 = V128
                }
                GcValue::ObjectRef(id) => Ok((id as i64, 5)),
                GcValue::Null => Ok((0, 6)),
            }
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: "No value returned".to_string(),
            })
        }
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn array_set_internal(
    runtime_handle: i64,
    object_id: i64,
    element_index: i32,
    value: i64,
    value_type: i32,
    v128_ptr: *const u8,
) -> WasmtimeResult<()> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let gc_value = match value_type {
        0 => GcValue::I32(value as i32),
        1 => GcValue::I64(value),
        2 => GcValue::F32(f32::from_bits(value as u32)),
        3 => GcValue::F64(f64::from_bits(value as u64)),
        4 => {
            // V128 type - read 16 bytes from v128_ptr
            if v128_ptr.is_null() {
                GcValue::V128([0u8; 16])
            } else {
                let mut bytes = [0u8; 16];
                unsafe {
                    std::ptr::copy_nonoverlapping(v128_ptr, bytes.as_mut_ptr(), 16);
                }
                GcValue::V128(bytes)
            }
        }
        5 => {
            // Reference type
            if value == 0 {
                GcValue::Null
            } else {
                GcValue::ObjectRef(value as u64)
            }
        }
        6 => GcValue::Null, // Null type
        other => {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Unknown GC value type code: {}", other),
            });
        }
    };

    let result = runtime.array_set(object_id as ObjectId, element_index as u32, gc_value);

    if result.success {
        Ok(())
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn array_len_internal(runtime_handle: i64, object_id: i64) -> WasmtimeResult<u32> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.array_len(object_id as ObjectId);

    if result.success {
        Ok(result.length.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn i31_new_internal(runtime_handle: i64, value: i32) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.i31_new(value);

    if result.success {
        Ok(result.cast_result.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn i31_new_unsigned_internal(runtime_handle: i64, value: u32) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.i31_new_unsigned(value);

    if result.success {
        Ok(result.cast_result.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn i31_wrapping_signed_internal(runtime_handle: i64, value: i32) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.i31_wrapping_signed(value);

    if result.success {
        Ok(result.cast_result.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn i31_wrapping_unsigned_internal(runtime_handle: i64, value: u32) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.i31_wrapping_unsigned(value);

    if result.success {
        Ok(result.cast_result.unwrap_or(0))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn i31_get_internal(runtime_handle: i64, object_id: i64, signed: u8) -> WasmtimeResult<i32> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.i31_get(object_id as ObjectId, signed != 0);

    if result.success {
        if let Some(GcValue::I32(value)) = result.value {
            Ok(value)
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: "No I32 value returned".to_string(),
            })
        }
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn ref_cast_internal(
    runtime_handle: i64,
    object_id: i64,
    target_type: i32,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert target_type to GcReferenceType
    // Type IDs must match Java's NATIVE_TYPE_* constants
    let gc_target_type = match target_type {
        0 => GcReferenceType::AnyRef, // NATIVE_TYPE_ANY
        1 => GcReferenceType::EqRef,  // NATIVE_TYPE_EQ
        2 => GcReferenceType::I31Ref, // NATIVE_TYPE_I31
        3 => GcReferenceType::StructRef(StructTypeDefinition {
            // NATIVE_TYPE_STRUCT
            type_id: 0,
            fields: vec![],
            name: None,
            supertype: None,
        }),
        4 => GcReferenceType::ArrayRef(Box::new(ArrayTypeDefinition {
            // NATIVE_TYPE_ARRAY
            type_id: 0,
            element_type: FieldType::I32,
            mutable: true,
            name: None,
        })),
        _ => {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid target type: {}", target_type),
            })
        }
    };

    let result = runtime.ref_cast(object_id as ObjectId, gc_target_type);

    if result.success {
        Ok(result.cast_result.unwrap_or(object_id as ObjectId))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn ref_test_internal(
    runtime_handle: i64,
    object_id: i64,
    target_type: i32,
) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert target_type to GcReferenceType
    // Type IDs must match Java's NATIVE_TYPE_* constants
    let gc_target_type = match target_type {
        0 => GcReferenceType::AnyRef, // NATIVE_TYPE_ANY
        1 => GcReferenceType::EqRef,  // NATIVE_TYPE_EQ
        2 => GcReferenceType::I31Ref, // NATIVE_TYPE_I31
        3 => GcReferenceType::StructRef(StructTypeDefinition {
            // NATIVE_TYPE_STRUCT
            type_id: 0,
            fields: vec![],
            name: None,
            supertype: None,
        }),
        4 => GcReferenceType::ArrayRef(Box::new(ArrayTypeDefinition {
            // NATIVE_TYPE_ARRAY
            type_id: 0,
            element_type: FieldType::I32,
            mutable: true,
            name: None,
        })),
        _ => {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid target type: {}", target_type),
            })
        }
    };

    let result = runtime.ref_test(object_id as ObjectId, gc_target_type);

    if result.success {
        Ok(result.test_result.unwrap_or(false))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn ref_eq_internal(runtime_handle: i64, object_id1: i64, object_id2: i64) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Handle null cases
    let id1 = if object_id1 == 0 {
        None
    } else {
        Some(object_id1 as ObjectId)
    };
    let id2 = if object_id2 == 0 {
        None
    } else {
        Some(object_id2 as ObjectId)
    };

    let result = runtime.ref_eq(id1, id2);

    if result.success {
        Ok(result.eq_result.unwrap_or(false))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn ref_is_null_internal(runtime_handle: i64, object_id: i64) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let id = if object_id == 0 {
        None
    } else {
        Some(object_id as ObjectId)
    };

    let result = runtime.ref_is_null(id);

    if result.success {
        Ok(result.is_null.unwrap_or(true))
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: result.error.unwrap_or_default(),
        })
    }
}

fn collect_garbage_internal(runtime_handle: i64) -> WasmtimeResult<GcStatsFFI> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.collect_garbage()?;

    // Get updated stats after collection
    let stats = runtime.get_heap_stats()?;

    Ok(GcStatsFFI {
        total_allocated: stats.total_allocated as i64,
        total_collected: result.objects_collected as i64,
        bytes_allocated: stats.bytes_allocated as i64,
        bytes_collected: result.bytes_collected as i64,
        minor_collections: stats.minor_collections as i64,
        major_collections: stats.major_collections as i64,
        total_gc_time_nanos: stats.total_gc_time.as_nanos() as i64,
        current_heap_size: stats.current_heap_size as i32,
        peak_heap_size: stats.peak_heap_size as i32,
        max_heap_size: stats.peak_heap_size as i32, // Use peak as max since GcHeapStats doesn't have max_heap_size
    })
}

/// Panama FFI function for copying array elements between arrays
///
/// # Parameters
/// * `runtime_handle` - GC runtime handle
/// * `dest_object_id` - Destination array object ID
/// * `dest_index` - Destination starting index
/// * `src_object_id` - Source array object ID
/// * `src_index` - Source starting index
/// * `length` - Number of elements to copy
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_array_copy(
    runtime_handle: i64,
    dest_object_id: i64,
    dest_index: i32,
    src_object_id: i64,
    src_index: i32,
    length: i32,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

        let result = runtime.array_copy(
            dest_object_id as ObjectId,
            dest_index as u32,
            src_object_id as ObjectId,
            src_index as u32,
            length as u32,
        );

        if result.success {
            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: result.error.unwrap_or_default(),
            })
        }
    }))
}

/// Panama FFI function for filling array elements with a value
///
/// # Parameters
/// * `runtime_handle` - GC runtime handle
/// * `object_id` - Array object ID
/// * `start_index` - Starting index
/// * `value` - Value to fill with (encoded as i64)
/// * `value_type` - Type code for the value (0=I32, 1=I64, 2=F32, 3=F64, 5=Ref, 6=Null)
/// * `length` - Number of elements to fill
///
/// # Returns
/// 0 on success, non-zero error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_array_fill(
    runtime_handle: i64,
    object_id: i64,
    start_index: i32,
    value: i64,
    value_type: i32,
    length: i32,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

        let gc_value = match value_type {
            0 => GcValue::I32(value as i32),
            1 => GcValue::I64(value),
            2 => GcValue::F32(f32::from_bits(value as u32)),
            3 => GcValue::F64(f64::from_bits(value as u64)),
            5 => {
                if value == 0 {
                    GcValue::Null
                } else {
                    GcValue::ObjectRef(value as u64)
                }
            }
            6 => GcValue::Null,
            other => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Unknown GC value type code: {}", other),
                });
            }
        };

        let result = runtime.array_fill(
            object_id as ObjectId,
            start_index as u32,
            gc_value,
            length as u32,
        );

        if result.success {
            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: result.error.unwrap_or_default(),
            })
        }
    }))
}

fn get_gc_stats_internal(runtime_handle: i64) -> WasmtimeResult<GcStatsFFI> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Invalid runtime handle".to_string(),
        });
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let stats = runtime.get_heap_stats()?;

    Ok(GcStatsFFI {
        total_allocated: stats.total_allocated as i64,
        total_collected: stats.total_collected as i64,
        bytes_allocated: stats.bytes_allocated as i64,
        bytes_collected: stats.bytes_collected as i64,
        minor_collections: stats.minor_collections as i64,
        major_collections: stats.major_collections as i64,
        total_gc_time_nanos: stats.total_gc_time.as_nanos() as i64,
        current_heap_size: stats.current_heap_size as i32,
        peak_heap_size: stats.peak_heap_size as i32,
        max_heap_size: stats.peak_heap_size as i32, // Use peak as max since GcHeapStats doesn't have max_heap_size
    })
}

// ============================================================================
// AnyRef Raw Conversion and Type Matching FFI
// ============================================================================

/// Panama FFI: Convert an AnyRef (by object_id) to its raw u32 representation.
/// Returns the raw value as i64, or -1 on failure.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_anyref_to_raw(runtime_handle: i64, object_id: u64) -> i64 {
    ffi_boundary_result!(-1i64, {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }
        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let raw = runtime.anyref_to_raw(object_id)?;
        Ok(raw as i64)
    })
}

/// Panama FFI: Create an AnyRef from a raw u32 representation.
/// Returns the new object_id, or 0 if the raw value is null/invalid.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_anyref_from_raw(runtime_handle: i64, raw: u32) -> i64 {
    ffi_boundary_result!(0i64, {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }
        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        match runtime.anyref_from_raw(raw)? {
            Some(object_id) => Ok(object_id as i64),
            None => Ok(0),
        }
    })
}

/// Panama FFI: Check if an AnyRef matches a given heap type code.
/// Returns 1 if matches, 0 if not, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_anyref_matches_ty(
    runtime_handle: i64,
    object_id: u64,
    heap_type_code: i32,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let heap_type = match heap_type_from_code(heap_type_code) {
            Some(ht) => ht,
            None => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Unknown heap type code: {}", heap_type_code),
                });
            }
        };

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let _matches = runtime.anyref_matches_ty(object_id, &heap_type)?;
        Ok(())
    }))
}

/// Panama FFI: Convert an AnyRef to an ExternRef (extern.convert_any).
/// Returns the i64 data from the resulting ExternRef, or i64::MIN on failure.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_externref_convert_any(
    runtime_handle: i64,
    object_id: u64,
) -> i64 {
    ffi_boundary_result!(i64::MIN, {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }
        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        match runtime.externref_convert_any(object_id)? {
            Some(id) => Ok(id),
            None => Ok(i64::MIN),
        }
    })
}

/// Panama FFI: Convert an ExternRef to an AnyRef (any.convert_extern).
/// Returns the new object_id for the resulting AnyRef.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_anyref_convert_extern(
    runtime_handle: i64,
    externref_data: i64,
) -> i64 {
    ffi_boundary_result!(0i64, {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }
        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let object_id = runtime.anyref_convert_extern(externref_data)?;
        Ok(object_id as i64)
    })
}

/// Panama FFI: Get the HeapType code for an EqRef.
/// Returns the heap type code (matching Java HeapType ordinal), or -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_eqref_ty(
    runtime_handle: i64,
    object_id: u64,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }
        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let _code = runtime.eqref_ty(object_id)?;
        Ok(())
    }))
}

/// Panama FFI: Check if an EqRef matches a given heap type code.
/// Returns 1 if matches, 0 if not, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_eqref_matches_ty(
    runtime_handle: i64,
    object_id: u64,
    heap_type_code: i32,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let heap_type = match heap_type_from_code(heap_type_code) {
            Some(ht) => ht,
            None => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Unknown heap type code: {}", heap_type_code),
                });
            }
        };

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let _matches = runtime.eqref_matches_ty(object_id, &heap_type)?;
        Ok(())
    }))
}

/// Panama FFI: Check if a StructRef matches a given heap type code.
/// Returns 1 if matches, 0 if not, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_structref_matches_ty(
    runtime_handle: i64,
    object_id: u64,
    heap_type_code: i32,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let heap_type = match heap_type_from_code(heap_type_code) {
            Some(ht) => ht,
            None => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Unknown heap type code: {}", heap_type_code),
                });
            }
        };

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let _matches = runtime.structref_matches_ty(object_id, &heap_type)?;
        Ok(())
    }))
}

/// Panama FFI: Check if an ArrayRef matches a given heap type code.
/// Returns 1 if matches, 0 if not, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_arrayref_matches_ty(
    runtime_handle: i64,
    object_id: u64,
    heap_type_code: i32,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let heap_type = match heap_type_from_code(heap_type_code) {
            Some(ht) => ht,
            None => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Unknown heap type code: {}", heap_type_code),
                });
            }
        };

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let _matches = runtime.arrayref_matches_ty(object_id, &heap_type)?;
        Ok(())
    }))
}

// ==================== Async GC Creation ====================

/// Panama FFI function for creating a struct instance asynchronously
#[cfg(feature = "async")]
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_struct_new_async(
    runtime_handle: i64,
    type_id: i32,
    field_values_ptr: *const i64,
    field_count: i32,
) -> i64 {
    ffi_boundary_result!(0i64, {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let struct_def = runtime.get_struct_type(type_id as u32)?;

        let mut values = Vec::with_capacity(field_count as usize);
        if !field_values_ptr.is_null() && field_count > 0 {
            unsafe {
                let values_slice =
                    std::slice::from_raw_parts(field_values_ptr, field_count as usize);
                for (i, &raw_value) in values_slice.iter().enumerate() {
                    let gc_value = if i < struct_def.fields.len() {
                        match &struct_def.fields[i].field_type {
                            FieldType::I32 => GcValue::I32(raw_value as i32),
                            FieldType::I64 => GcValue::I64(raw_value),
                            FieldType::F32 => GcValue::F32(f32::from_bits(raw_value as u32)),
                            FieldType::F64 => GcValue::F64(f64::from_bits(raw_value as u64)),
                            FieldType::V128 => {
                                // raw_value is a pointer to 16 bytes of V128 data
                                if raw_value == 0 {
                                    GcValue::V128([0u8; 16])
                                } else {
                                    let v128_ptr = raw_value as *const u8;
                                    let mut bytes = [0u8; 16];
                                    std::ptr::copy_nonoverlapping(
                                        v128_ptr,
                                        bytes.as_mut_ptr(),
                                        16,
                                    );
                                    GcValue::V128(bytes)
                                }
                            }
                            FieldType::PackedI8 | FieldType::PackedI16 => {
                                GcValue::I32(raw_value as i32)
                            }
                            FieldType::Reference(_) => {
                                if raw_value == 0 {
                                    GcValue::Null
                                } else {
                                    GcValue::ObjectRef(raw_value as u64)
                                }
                            }
                        }
                    } else {
                        GcValue::I32(raw_value as i32)
                    };
                    values.push(gc_value);
                }
            }
        } else {
            for field in &struct_def.fields {
                let default_value = match &field.field_type {
                    FieldType::I32 => GcValue::I32(0),
                    FieldType::I64 => GcValue::I64(0),
                    FieldType::F32 => GcValue::F32(0.0),
                    FieldType::F64 => GcValue::F64(0.0),
                    FieldType::V128 => GcValue::V128([0u8; 16]),
                    FieldType::PackedI8 | FieldType::PackedI16 => GcValue::I32(0),
                    FieldType::Reference(_) => GcValue::Null,
                };
                values.push(default_value);
            }
        }

        let result = runtime.struct_new_async(struct_def, values);

        if result.success {
            Ok(result.object_id.unwrap_or(0) as i64)
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: result.error.unwrap_or_default(),
            })
        }
    })
}

/// Panama FFI function for creating an array instance asynchronously
#[cfg(feature = "async")]
#[no_mangle]
pub extern "C" fn wasmtime4j_gc_array_new_async(
    runtime_handle: i64,
    type_id: i32,
    elements_ptr: *const i64,
    element_count: i32,
) -> i64 {
    ffi_boundary_result!(0i64, {
        if runtime_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid runtime handle".to_string(),
            });
        }

        let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };
        let array_def = runtime.get_array_type(type_id as u32)?;

        let mut gc_elements = Vec::with_capacity(element_count as usize);
        if !elements_ptr.is_null() && element_count > 0 {
            unsafe {
                let elements_slice =
                    std::slice::from_raw_parts(elements_ptr, element_count as usize);
                for &element in elements_slice {
                    let gc_value = match &array_def.element_type {
                        FieldType::I32 => GcValue::I32(element as i32),
                        FieldType::I64 => GcValue::I64(element),
                        FieldType::F32 => GcValue::F32(f32::from_bits(element as u32)),
                        FieldType::F64 => GcValue::F64(f64::from_bits(element as u64)),
                        FieldType::V128 => {
                            // element is a pointer to 16 bytes of V128 data
                            if element == 0 {
                                GcValue::V128([0u8; 16])
                            } else {
                                let v128_ptr = element as *const u8;
                                let mut bytes = [0u8; 16];
                                std::ptr::copy_nonoverlapping(
                                    v128_ptr,
                                    bytes.as_mut_ptr(),
                                    16,
                                );
                                GcValue::V128(bytes)
                            }
                        }
                        FieldType::PackedI8 | FieldType::PackedI16 => {
                            GcValue::I32(element as i32)
                        }
                        FieldType::Reference(_) => {
                            if element == 0 {
                                GcValue::Null
                            } else {
                                GcValue::ObjectRef(element as u64)
                            }
                        }
                    };
                    gc_elements.push(gc_value);
                }
            }
        }

        let result = runtime.array_new_async(array_def, gc_elements);

        if result.success {
            Ok(result.object_id.unwrap_or(0) as i64)
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: result.error.unwrap_or_default(),
            })
        }
    })
}
