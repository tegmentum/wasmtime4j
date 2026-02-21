//! Panama FFI bindings for WebAssembly tables
//!
//! This module provides C-compatible functions for creating, managing,
//! and accessing WebAssembly tables with bounds checking and reference type support.

use crate::error::ffi_utils;
use crate::store::Store;
use crate::table::core;
use std::ffi::CString;
use std::os::raw::{c_char, c_int, c_uchar, c_uint, c_ulong, c_void};
use wasmtime::{RefType, ValType};

/// Create a new WebAssembly table (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_create(
    store_ptr: *mut c_void,
    element_type: c_int,
    initial_size: c_uint,
    has_maximum: c_int,
    maximum_size: c_uint,
    name_ptr: *const c_char,
    table_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let val_type = match element_type {
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                })
            }
        };

        let max_size = if has_maximum != 0 {
            Some(maximum_size)
        } else {
            None
        };

        let name = if name_ptr.is_null() {
            None
        } else {
            Some(unsafe { ffi_utils::c_char_to_string(name_ptr)? })
        };

        let table = core::create_table(store, val_type, initial_size, max_size, name)?;

        unsafe {
            *table_ptr = Box::into_raw(table) as *mut c_void;
        }

        Ok(())
    })
}

/// Get table size (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_size(
    table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    size: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        log::debug!("wasmtime4j_panama_table_size: getting table ref");
        let table = unsafe { core::get_table_ref(table_ptr)? };
        log::debug!("wasmtime4j_panama_table_size: got table ref, getting store");
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };
        log::debug!("wasmtime4j_panama_table_size: got store, getting size");

        let table_size = core::get_table_size(table, store)?;
        log::debug!("wasmtime4j_panama_table_size: got size={}", table_size);

        unsafe {
            if !size.is_null() {
                *size = table_size;
            }
        }

        Ok(())
    })
}

/// Get table element (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_get(
    table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    index: c_uint,
    ref_id_present: *mut c_int,
    ref_id: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let element = core::get_table_element(table, store, index)?;
        let ref_id_opt = core::extract_table_element_ref_id(&element);

        unsafe {
            if !ref_id_present.is_null() {
                *ref_id_present = if ref_id_opt.is_some() { 1 } else { 0 };
            }
            if !ref_id.is_null() {
                *ref_id = ref_id_opt.unwrap_or(0);
            }
        }

        Ok(())
    })
}

/// Set table element (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_set(
    table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    index: c_uint,
    element_type: c_int,
    ref_id_present: c_int,
    ref_id: c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let val_type = match element_type {
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                })
            }
        };

        let ref_id_opt = if ref_id_present != 0 {
            Some(ref_id)
        } else {
            None
        };
        let element = core::create_table_element(val_type, ref_id_opt)?;

        core::set_table_element(table, store, index, element)?;

        Ok(())
    })
}

/// Grow table (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_grow(
    table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    delta: c_uint,
    element_type: c_int,
    ref_id_present: c_int,
    ref_id: c_ulong,
    old_size: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let val_type = match element_type {
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                })
            }
        };

        let ref_id_opt = if ref_id_present != 0 {
            Some(ref_id)
        } else {
            None
        };
        let init_value = core::create_table_element(val_type, ref_id_opt)?;

        let previous_size = core::grow_table(table, store, delta, init_value)?;

        unsafe {
            if !old_size.is_null() {
                *old_size = previous_size;
            }
        }

        Ok(())
    })
}

/// Grow table asynchronously (Panama FFI version)
///
/// Requires engine with `async_support(true)`. Uses the async resource limiter.
#[no_mangle]
#[cfg(feature = "async")]
pub extern "C" fn wasmtime4j_panama_table_grow_async(
    table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    delta: c_uint,
    element_type: c_int,
    ref_id_present: c_int,
    ref_id: c_ulong,
    old_size: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let val_type = match element_type {
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                })
            }
        };

        let ref_id_opt = if ref_id_present != 0 {
            Some(ref_id)
        } else {
            None
        };
        let init_value = core::create_table_element(val_type, ref_id_opt)?;

        let previous_size = core::grow_table_async(table, store, delta, init_value)?;

        unsafe {
            if !old_size.is_null() {
                *old_size = previous_size;
            }
        }

        Ok(())
    })
}

/// Fill table range (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_fill(
    table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    dst: c_uint,
    len: c_uint,
    element_type: c_int,
    ref_id_present: c_int,
    ref_id: c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let val_type = match element_type {
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                })
            }
        };

        let ref_id_opt = if ref_id_present != 0 {
            Some(ref_id)
        } else {
            None
        };
        let value = core::create_table_element(val_type, ref_id_opt)?;

        core::fill_table(table, store, dst, value, len)?;

        Ok(())
    })
}

/// Get table metadata (Panama FFI version)
/// Updated for Table64 support - sizes are now 64-bit
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_metadata(
    table_ptr: *mut c_void,
    element_type: *mut c_int,
    initial_size: *mut c_ulong,
    has_maximum: *mut c_int,
    maximum_size: *mut c_ulong,
    is_64: *mut c_int,
    name_ptr: *mut *mut c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let metadata = core::get_table_metadata(table);

        unsafe {
            if !element_type.is_null() {
                *element_type = match &metadata.element_type {
                    ValType::Ref(ref_type) => {
                        // Check the heap type to determine funcref vs externref
                        match ref_type.heap_type() {
                            wasmtime::HeapType::Func => 5,      // FUNCREF
                            wasmtime::HeapType::Extern => 6,    // EXTERNREF
                            wasmtime::HeapType::Any => 7,       // ANYREF
                            wasmtime::HeapType::Eq => 8,        // EQREF
                            wasmtime::HeapType::I31 => 9,       // I31REF
                            wasmtime::HeapType::Struct => 10,   // STRUCTREF
                            wasmtime::HeapType::Array => 11,    // ARRAYREF
                            wasmtime::HeapType::None => 12,     // NULLREF
                            wasmtime::HeapType::NoFunc => 13,   // NULLFUNCREF
                            wasmtime::HeapType::NoExtern => 14, // NULLEXTERNREF
                            _ => 6, // Default to EXTERNREF for other/unknown types
                        }
                    }
                    _ => -1, // Invalid
                };
            }
            if !initial_size.is_null() {
                *initial_size = metadata.initial_size;
            }
            if !has_maximum.is_null() {
                *has_maximum = if metadata.maximum_size.is_some() {
                    1
                } else {
                    0
                };
            }
            if !maximum_size.is_null() {
                *maximum_size = metadata.maximum_size.unwrap_or(0);
            }
            if !is_64.is_null() {
                *is_64 = if metadata.is_64 { 1 } else { 0 };
            }
            if !name_ptr.is_null() {
                *name_ptr = if let Some(ref name) = metadata.name {
                    ffi_utils::string_to_c_char(name.clone())?
                } else {
                    std::ptr::null_mut()
                };
            }
        }

        Ok(())
    })
}

/// Destroy a table (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_destroy(table_ptr: *mut c_void) {
    unsafe {
        core::destroy_table(table_ptr);
    }
}

/// Create a new 64-bit WebAssembly table (Panama FFI version)
/// Memory64 proposal: tables with 64-bit indices
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_create64(
    store_ptr: *mut c_void,
    element_type: c_int,
    initial_size: c_ulong,
    has_maximum: c_int,
    maximum_size: c_ulong,
    name_ptr: *const c_char,
    table_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let val_type = match element_type {
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                })
            }
        };

        let max_size = if has_maximum != 0 {
            Some(maximum_size)
        } else {
            None
        };

        let name = if name_ptr.is_null() {
            None
        } else {
            Some(unsafe { ffi_utils::c_char_to_string(name_ptr)? })
        };

        let table = core::create_table64(store, val_type, initial_size, max_size, name)?;

        unsafe {
            *table_ptr = Box::into_raw(table) as *mut c_void;
        }

        Ok(())
    })
}

/// Check if a table uses 64-bit addressing (Memory64 proposal)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_is_64(table_ptr: *mut c_void) -> c_int {
    match unsafe { core::get_table_ref(table_ptr) } {
        Ok(table) => {
            if core::is_table_64(table) {
                1
            } else {
                0
            }
        }
        Err(_) => -1,
    }
}

/// Initialize table from element segment (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_init(
    table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    instance_ptr: *mut c_void,
    dst: c_uint,
    src: c_uint,
    len: c_uint,
    segment_index: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };
        let instance =
            unsafe { ffi_utils::deref_ptr::<crate::instance::Instance>(instance_ptr, "instance")? };

        table.init_from_segment(store, instance, dst, src, len, segment_index)?;
        Ok(())
    })
}

/// Drop an element segment (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_elem_drop(
    instance_ptr: *mut c_void,
    segment_index: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance =
            unsafe { ffi_utils::deref_ptr::<crate::instance::Instance>(instance_ptr, "instance")? };

        let segment_manager = instance.get_element_segment_manager();
        segment_manager.drop_segment(segment_index)?;
        Ok(())
    })
}

/// Initialize memory from data segment (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_init(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    instance_ptr: *mut c_void,
    dest_offset: c_uint,
    data_segment_index: c_uint,
    src_offset: c_uint,
    len: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get raw wasmtime::Memory (not wrapped)
        let wasmtime_memory =
            unsafe { ffi_utils::deref_ptr::<wasmtime::Memory>(memory_ptr, "memory")? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };
        let instance =
            unsafe { ffi_utils::deref_ptr::<crate::instance::Instance>(instance_ptr, "instance")? };

        // Get memory type information from the store
        let memory_type = store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(ctx)))?;
        // Create wrapped Memory from wasmtime::Memory
        let memory = crate::memory::Memory::from_wasmtime_memory(*wasmtime_memory, memory_type);

        crate::memory::core::memory_init(
            &memory,
            store,
            instance,
            dest_offset,
            data_segment_index,
            src_offset,
            len,
        )?;
        Ok(())
    })
}

/// Drop a data segment (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_data_drop(
    instance_ptr: *mut c_void,
    data_segment_index: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance =
            unsafe { ffi_utils::deref_ptr::<crate::instance::Instance>(instance_ptr, "instance")? };

        crate::memory::core::data_drop(instance, data_segment_index)?;
        Ok(())
    })
}

/// Copy memory within the same memory instance (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_copy(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    dest_offset: c_uint,
    src_offset: c_uint,
    len: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        crate::memory::core::memory_copy(
            memory,
            store,
            dest_offset as usize,
            src_offset as usize,
            len as usize,
        )?;
        Ok(())
    })
}

/// Fill memory with a byte value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_fill(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: c_uint,
    value: c_uchar,
    len: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        crate::memory::core::memory_fill(memory, store, offset as usize, value, len as usize)?;
        Ok(())
    })
}

//==========================================================================================
// Atomic Memory Operations (Panama FFI versions)
//==========================================================================================

/// Atomic compare-and-swap on 32-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_compare_and_swap_i32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    expected: i32,
    new_value: i32,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_compare_and_swap_i32(
            memory, store, offset, expected, new_value,
        )?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic compare-and-swap on 64-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_compare_and_swap_i64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    expected: i64,
    new_value: i64,
    result_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_compare_and_swap_i64(
            memory, store, offset, expected, new_value,
        )?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic load of 32-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_load_i32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_load_i32(memory, store, offset)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic load of 64-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_load_i64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    result_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_load_i64(memory, store, offset)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic store of 32-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_store_i32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        crate::memory::core::atomic_store_i32(memory, store, offset, value)?;
        Ok(())
    })
}

/// Atomic store of 64-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_store_i64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        crate::memory::core::atomic_store_i64(memory, store, offset, value)?;
        Ok(())
    })
}

/// Atomic add on 32-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_add_i32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i32,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_add_i32(memory, store, offset, value)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic add on 64-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_add_i64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i64,
    result_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_add_i64(memory, store, offset, value)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic AND on 32-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_and_i32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i32,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_and_i32(memory, store, offset, value)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic OR on 32-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_or_i32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i32,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_or_i32(memory, store, offset, value)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic XOR on 32-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_xor_i32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i32,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_xor_i32(memory, store, offset, value)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic AND on 64-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_and_i64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i64,
    result_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_and_i64(memory, store, offset, value)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic OR on 64-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_or_i64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i64,
    result_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_or_i64(memory, store, offset, value)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic XOR on 64-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_xor_i64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: i64,
    result_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_xor_i64(memory, store, offset, value)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic memory fence (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_fence(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

        crate::memory::core::atomic_fence(memory, store)?;
        Ok(())
    })
}

/// Atomic notify/wake (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_notify(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    count: i32,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result = crate::memory::core::atomic_notify(memory, store, offset, count)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic wait on 32-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_wait32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    expected: i32,
    timeout_nanos: i64,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result =
            crate::memory::core::atomic_wait32(memory, store, offset, expected, timeout_nanos)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Atomic wait on 64-bit value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_atomic_wait64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    expected: i64,
    timeout_nanos: i64,
    result_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Memory pointer is a ValidatedMemory wrapper, not a raw wasmtime::Memory
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

        if result_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Result output pointer cannot be null".to_string(),
            });
        }

        let result =
            crate::memory::core::atomic_wait64(memory, store, offset, expected, timeout_nanos)?;

        unsafe {
            *result_out = result;
        }

        Ok(())
    })
}

/// Copy elements within a table (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_copy(
    table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    dst: c_uint,
    src: c_uint,
    len: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")? };

        table.copy_within(store, dst, src, len)?;
        Ok(())
    })
}

/// Copy elements from another table (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_table_copy_from(
    dst_table_ptr: *mut c_void,
    store_ptr: *mut c_void,
    dst: c_uint,
    src_table_ptr: *mut c_void,
    src: c_uint,
    len: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let dst_table =
            unsafe { crate::table::core::get_table_ref(dst_table_ptr as *const c_void)? };
        let src_table =
            unsafe { crate::table::core::get_table_ref(src_table_ptr as *const c_void)? };
        let store = unsafe { ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")? };

        dst_table.copy_from(store, dst, src_table, src, len)?;
        Ok(())
    })
}

/// Get the last error message as a C string
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_get_last_error_message() -> *mut c_char {
    crate::error::ffi_utils::get_last_error_message()
}

/// Free an error message returned by wasmtime4j_get_last_error_message
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_free_error_message(message: *mut c_char) {
    if !message.is_null() {
        unsafe {
            let _ = CString::from_raw(message);
        }
    }
}

/// Clear any stored error state in the native library
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_clear_error_state() {
    crate::error::ffi_utils::clear_last_error();
}
