//! Panama FFI bindings for WebAssembly linear memory operations
//!
//! This module provides C-compatible functions for creating, managing,
//! and accessing WebAssembly linear memory with comprehensive bounds checking.

use std::os::raw::{c_char, c_int, c_uint, c_ulong, c_void};
use std::sync::Arc;
use crate::error::ffi_utils;
use crate::memory::{Memory, MemoryBuilder, MemoryDataType, MemoryRegistry};
use crate::store::Store;

/// Create a new WebAssembly memory with default configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_create(
    store_ptr: *mut c_void,
    initial_pages: c_uint,
    memory_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };

        let memory = Memory::new(store, initial_pages as u64)?;

        unsafe {
            *memory_ptr = Box::into_raw(Box::new(memory)) as *mut c_void;
        }

        Ok(())
    })
}

/// Create a new WebAssembly memory with configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_create_with_config(
    store_ptr: *mut c_void,
    initial_pages: c_uint,
    maximum_pages: c_uint,
    is_shared: c_int,
    memory_index: c_uint,
    name: *const c_char,
    memory_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // SAFETY IMPROVEMENT: Using new memory utilities with comprehensive validation
        let store = crate::ffi_common::memory_utils::safe_deref_mut(
            store_ptr as *mut Store,
            "store"
        ).map_err(|e| e.to_wasmtime_error())?;

        let mut builder = MemoryBuilder::new(initial_pages as u64);

        if maximum_pages > 0 {
            builder = builder.maximum_pages(maximum_pages as u64);
        }

        if is_shared != 0 {
            builder = builder.shared();
        }

        builder = builder.memory_index(memory_index);

        if !name.is_null() {
            let name_str = unsafe { ffi_utils::c_str_to_string(name, "memory_name")? };
            builder = builder.name(name_str);
        }

        let memory = builder.build(store)?;

        // Wrap in ValidatedMemory for consistent pointer handling across all memory operations
        let validated_ptr = crate::memory::core::create_validated_memory(memory)?;

        unsafe {
            *memory_ptr = validated_ptr as *mut c_void;
        }

        Ok(())
    })
}

/// Get memory size in pages (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_size_pages(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    size_out: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

        // Use Memory's size_pages method
        let size = memory.size_pages(store)?;

        unsafe {
            *size_out = size as c_uint;
        }

        Ok(())
    })
}

/// Get memory size in bytes (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_size_bytes(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    size_out: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

        // Use Memory's size_bytes method
        let size = memory.size_bytes(store)?;

        unsafe {
            *size_out = size;
        }

        Ok(())
    })
}

/// Grow memory by additional pages (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_grow(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    additional_pages: c_uint,
    previous_pages_out: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        // Use Memory's grow method
        let previous_pages = memory.grow(store, additional_pages as u64)?;

        unsafe {
            *previous_pages_out = previous_pages as c_uint;
        }

        Ok(())
    })
}

/// Grow memory by additional pages using 64-bit addressing (Panama FFI version)
/// This supports Memory64 proposal for memories larger than 4GB.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_grow64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    additional_pages: u64,
    previous_pages_out: *mut u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        // Use Memory's grow method
        let previous_pages = memory.grow(store, additional_pages)?;

        unsafe {
            *previous_pages_out = previous_pages;
        }

        Ok(())
    })
}

/// Get whether memory uses 64-bit addressing (Memory64 proposal)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_is_64bit(
    memory_ptr: *mut c_void,
    _store_ptr: *mut c_void,
    is_64bit_out: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };

        // Use cached memory type from our wrapper (no store access needed)
        let is_64 = memory.memory_type.is_64();

        unsafe {
            *is_64bit_out = if is_64 { 1 } else { 0 };
        }

        Ok(())
    })
}

/// Check if memory is shared between threads (for threads proposal)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_is_shared(
    memory_ptr: *mut c_void,
    _store_ptr: *mut c_void,
    is_shared_out: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };

        // Use cached memory type from our wrapper
        let is_shared = memory.memory_type.is_shared();

        unsafe {
            *is_shared_out = if is_shared { 1 } else { 0 };
        }

        Ok(())
    })
}

/// Get memory type minimum pages
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_get_minimum(
    memory_ptr: *mut c_void,
    _store_ptr: *mut c_void,
    minimum_out: *mut u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };

        // Use cached memory type from our wrapper
        let minimum = memory.memory_type.minimum();

        unsafe {
            *minimum_out = minimum;
        }

        Ok(())
    })
}

/// Get memory type maximum pages (-1 if unlimited)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_get_maximum(
    memory_ptr: *mut c_void,
    _store_ptr: *mut c_void,
    maximum_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };

        // Use cached memory type from our wrapper
        let maximum = memory.memory_type.maximum().map(|m| m as i64).unwrap_or(-1);

        unsafe {
            *maximum_out = maximum;
        }

        Ok(())
    })
}

/// Get memory size in pages using 64-bit return value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_size_pages64(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    size_out: *mut u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

        // Use Memory's size_pages method which handles both regular and shared memory
        let pages = memory.size_pages(store)?;

        unsafe {
            *size_out = pages;
        }

        Ok(())
    })
}

/// Read bytes from memory with bounds checking (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_read_bytes(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    length: usize,
    buffer: *mut u8,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

        if buffer.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Buffer cannot be null".to_string(),
            });
        }

        // Use Memory's read_bytes method which handles both regular and shared memory
        let data = memory.read_bytes(store, offset, length)?;

        unsafe {
            std::ptr::copy_nonoverlapping(data.as_ptr(), buffer, length);
        }

        Ok(())
    })
}

/// Write bytes to memory with bounds checking (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_write_bytes(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    length: usize,
    buffer: *const u8,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        if buffer.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Buffer cannot be null".to_string(),
            });
        }

        // Create a slice from the input buffer
        let data = unsafe { std::slice::from_raw_parts(buffer, length) };

        // Use Memory's write_bytes method which handles both regular and shared memory
        memory.write_bytes(store, offset, data)
    })
}

/// Get raw pointer to WASM linear memory for zero-copy access (Panama FFI version).
///
/// Returns the raw pointer and size of the memory buffer.
/// The returned pointer is only valid while:
/// 1. The memory instance is alive
/// 2. The store is alive
/// 3. No memory.grow() operations are performed
///
/// After memory.grow(), the pointer may be invalidated and must be re-obtained.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_get_data(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    data_ptr_out: *mut *mut u8,
    size_out: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Get Memory through ValidatedMemory wrapper
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

        if data_ptr_out.is_null() || size_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output pointers cannot be null".to_string(),
            });
        }

        // Get the inner wasmtime::Memory and access its data
        if let Some(inner_mem) = memory.inner() {
            store.with_context_ro(|ctx| {
                let data = inner_mem.data(ctx);

                unsafe {
                    *data_ptr_out = data.as_ptr() as *mut u8;
                    *size_out = data.len();
                }

                Ok(())
            })
        } else {
            // Shared memory case
            if let Some(shared_mem) = memory.inner_shared() {
                let data = shared_mem.data();
                unsafe {
                    // Shared memory returns &[UnsafeCell<u8>]
                    *data_ptr_out = data.as_ptr() as *mut std::cell::UnsafeCell<u8> as *mut u8;
                    *size_out = data.len();
                }
                Ok(())
            } else {
                Err(crate::error::WasmtimeError::Memory {
                    message: "Memory has no inner variant".to_string(),
                })
            }
        }
    })
}

/// Check if instance has a memory export with the given name
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_has_memory_export(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in memory name".to_string(),
                })?
        };

        match crate::instance::core::get_exported_memory(instance, store, name_str)? {
            Some(_) => Ok(()),
            None => Err(crate::error::WasmtimeError::ImportExport {
                message: format!("Memory '{}' not found", name_str),
            }),
        }
    })
}

/// Get memory size in pages by looking up memory fresh
/// Handles both regular and shared memory exports
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_get_memory_size_pages(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    size_out: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in memory name".to_string(),
                })?
        };

        // First try regular memory
        if let Some(memory) = crate::instance::core::get_exported_memory(instance, store, name_str)? {
            let size = store.with_context_ro(|ctx| Ok(memory.size(ctx)))?;
            unsafe {
                *size_out = size as c_uint;
            }
            return Ok(());
        }

        // Try shared memory (for threads proposal)
        if let Some(shared_memory) = instance.get_shared_memory(store, name_str)? {
            let size = shared_memory.size();
            unsafe {
                *size_out = size as c_uint;
            }
            return Ok(());
        }

        Err(crate::error::WasmtimeError::ImportExport {
            message: format!("Memory '{}' not found (neither regular nor shared)", name_str),
        })
    })
}

/// Get memory size in bytes by looking up memory fresh
/// Handles both regular and shared memory exports
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_get_memory_size_bytes(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    size_out: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in memory name".to_string(),
                })?
        };

        // First try regular memory
        if let Some(memory) = crate::instance::core::get_exported_memory(instance, store, name_str)? {
            let size = store.with_context_ro(|ctx| Ok(memory.data_size(ctx)))?;
            unsafe {
                *size_out = size;
            }
            return Ok(());
        }

        // Try shared memory (for threads proposal)
        if let Some(shared_memory) = instance.get_shared_memory(store, name_str)? {
            let size = shared_memory.data().len();
            unsafe {
                *size_out = size;
            }
            return Ok(());
        }

        Err(crate::error::WasmtimeError::ImportExport {
            message: format!("Memory '{}' not found (neither regular nor shared)", name_str),
        })
    })
}

/// Grow memory by looking up memory fresh
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_grow_memory(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    additional_pages: c_uint,
    previous_pages_out: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in memory name".to_string(),
                })?
        };

        let memory = crate::instance::core::get_exported_memory(instance, store, name_str)?
            .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                message: format!("Memory '{}' not found", name_str),
            })?;

        let previous_pages = store.with_context(|mut ctx| {
            memory.grow(&mut ctx, additional_pages as u64)
                .map_err(|e| crate::error::WasmtimeError::Runtime {
                    message: format!("Failed to grow memory: {}", e),
                    backtrace: None,
                })
        })?;

        unsafe {
            *previous_pages_out = previous_pages as c_uint;
        }

        Ok(())
    })
}

/// Read bytes from memory by looking up memory fresh
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_read_memory_bytes(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    offset: usize,
    length: usize,
    buffer: *mut u8,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in memory name".to_string(),
                })?
        };

        if buffer.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Buffer cannot be null".to_string(),
            });
        }

        let memory = crate::instance::core::get_exported_memory(instance, store, name_str)?
            .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                message: format!("Memory '{}' not found", name_str),
            })?;

        store.with_context_ro(|ctx| {
            let data = memory.data(ctx);
            if offset + length > data.len() {
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!("Memory access out of bounds: offset={}, length={}, size={}", offset, length, data.len()),
                });
            }

            unsafe {
                std::ptr::copy_nonoverlapping(data.as_ptr().add(offset), buffer, length);
            }

            Ok(())
        })
    })
}

/// Write bytes to memory by looking up memory fresh
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_write_memory_bytes(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    offset: usize,
    length: usize,
    buffer: *const u8,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in memory name".to_string(),
                })?
        };

        if buffer.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Buffer cannot be null".to_string(),
            });
        }

        let memory = crate::instance::core::get_exported_memory(instance, store, name_str)?
            .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                message: format!("Memory '{}' not found", name_str),
            })?;

        store.with_context(|mut ctx| {
            let data = memory.data_mut(&mut ctx);
            if offset + length > data.len() {
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!("Memory access out of bounds: offset={}, length={}, size={}", offset, length, data.len()),
                });
            }

            unsafe {
                std::ptr::copy_nonoverlapping(buffer, data.as_mut_ptr().add(offset), length);
            }

            Ok(())
        })
    })
}

/// Get global type information
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_get_global_type(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    value_type_out: *mut i32,
    is_mutable_out: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in global name".to_string(),
                })?
        };

        let global = crate::instance::core::get_exported_global(instance, store, name_str)?
            .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                message: format!("Global '{}' not found", name_str),
            })?;

        store.with_context_ro(|ctx| {
            let global_type = global.ty(&ctx);

            // Map wasmtime value type to our type codes
            let type_code = match global_type.content() {
                wasmtime::ValType::I32 => 0,
                wasmtime::ValType::I64 => 1,
                wasmtime::ValType::F32 => 2,
                wasmtime::ValType::F64 => 3,
                wasmtime::ValType::V128 => 4,
                wasmtime::ValType::Ref(ref_type) => {
                    match ref_type.heap_type() {
                        wasmtime::HeapType::Func => 5, // FuncRef
                        _ => 6, // ExternRef or other
                    }
                }
            };

            unsafe {
                if !value_type_out.is_null() {
                    *value_type_out = type_code;
                }
                if !is_mutable_out.is_null() {
                    *is_mutable_out = if global_type.mutability() == wasmtime::Mutability::Var {
                        1
                    } else {
                        0
                    };
                }
            }

            Ok(())
        })
    })
}

/// Check if global export exists
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_has_global_export(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in global name".to_string(),
                })?
        };

        match crate::instance::core::get_exported_global(instance, store, name_str)? {
            Some(_) => Ok(()),
            None => Err(crate::error::WasmtimeError::ImportExport {
                message: format!("Global '{}' not found", name_str),
            }),
        }
    })
}

/// Get value from global by looking up global fresh
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_get_global_value(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    i32_out: *mut i32,
    i64_out: *mut i64,
    f32_out: *mut f64,
    f64_out: *mut f64,
    ref_id_present_out: *mut i32,
    ref_id_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in global name".to_string(),
                })?
        };

        let global = crate::instance::core::get_exported_global(instance, store, name_str)?
            .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                message: format!("Global '{}' not found", name_str),
            })?;

        store.with_context(|mut ctx| {
            let value = global.get(&mut ctx);

            // Initialize all outputs to 0
            unsafe {
                if !i32_out.is_null() {
                    *i32_out = 0;
                }
                if !i64_out.is_null() {
                    *i64_out = 0;
                }
                if !f32_out.is_null() {
                    *f32_out = 0.0;
                }
                if !f64_out.is_null() {
                    *f64_out = 0.0;
                }
                if !ref_id_present_out.is_null() {
                    *ref_id_present_out = 0;
                }
                if !ref_id_out.is_null() {
                    *ref_id_out = 0;
                }

                // Set the appropriate output based on type
                match value {
                    wasmtime::Val::I32(v) => {
                        if !i32_out.is_null() {
                            *i32_out = v;
                        }
                    }
                    wasmtime::Val::I64(v) => {
                        if !i64_out.is_null() {
                            *i64_out = v;
                        }
                    }
                    wasmtime::Val::F32(v) => {
                        if !f32_out.is_null() {
                            *f32_out = f32::from_bits(v) as f64;
                        }
                    }
                    wasmtime::Val::F64(v) => {
                        if !f64_out.is_null() {
                            *f64_out = f64::from_bits(v);
                        }
                    }
                    wasmtime::Val::FuncRef(maybe_func) => {
                        if !ref_id_present_out.is_null() {
                            *ref_id_present_out = if maybe_func.is_some() { 1 } else { 0 };
                        }
                    }
                    wasmtime::Val::ExternRef(maybe_ref) => {
                        if !ref_id_present_out.is_null() {
                            *ref_id_present_out = if maybe_ref.is_some() { 1 } else { 0 };
                        }
                    }
                    _ => {
                        return Err(crate::error::WasmtimeError::Type {
                            message: format!("Unsupported global value type: {:?}", value),
                        });
                    }
                }
            }

            Ok(())
        })
    })
}

/// Set value for global by looking up global fresh
#[no_mangle]
pub extern "C" fn wasmtime4j_instance_set_global_value(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    value_type_code: i32,
    i32_value: i32,
    i64_value: i64,
    f32_value: f64,
    f64_value: f64,
    ref_id_present: i32,
    ref_id: i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid UTF-8 in global name".to_string(),
                })?
        };

        let global = crate::instance::core::get_exported_global(instance, store, name_str)?
            .ok_or_else(|| crate::error::WasmtimeError::ImportExport {
                message: format!("Global '{}' not found", name_str),
            })?;

        let value = match value_type_code {
            0 => wasmtime::Val::I32(i32_value),
            1 => wasmtime::Val::I64(i64_value),
            2 => wasmtime::Val::F32((f32_value as f32).to_bits()),
            3 => wasmtime::Val::F64(f64_value.to_bits()),
            5 => {
                // FuncRef
                if ref_id_present != 0 {
                    // Look up the function from the registry using the ref_id
                    let func = crate::table::core::get_function_reference(ref_id as u64)?
                        .ok_or_else(|| crate::error::WasmtimeError::InvalidParameter {
                            message: format!("Function reference not found in registry: {}", ref_id),
                        })?;
                    wasmtime::Val::FuncRef(Some(func))
                } else {
                    wasmtime::Val::FuncRef(None)
                }
            }
            6 => {
                // ExternRef
                if ref_id_present != 0 {
                    return Err(crate::error::WasmtimeError::Type {
                        message: "Setting externref values not yet supported".to_string(),
                    });
                }
                wasmtime::Val::ExternRef(None)
            }
            _ => {
                return Err(crate::error::WasmtimeError::Type {
                    message: format!("Invalid value type code: {}", value_type_code),
                });
            }
        };

        store.with_context(|mut ctx| {
            global
                .set(&mut ctx, value)
                .map_err(|e| crate::error::WasmtimeError::Runtime {
                    message: format!("Failed to set global value: {}", e),
                    backtrace: None,
                })
        })
    })
}

/// Read typed value from memory with alignment checking (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_read_u32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value_out: *mut u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr)? };

        if value_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Value output pointer cannot be null".to_string(),
            });
        }

        let value: u32 = memory.read_typed(store, offset, MemoryDataType::U32Le)?;

        unsafe {
            *value_out = value;
        }

        Ok(())
    })
}

/// Write typed value to memory with alignment checking (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_write_u32(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    offset: usize,
    value: u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        memory.write_typed(store, offset, value, MemoryDataType::U32Le)?;

        Ok(())
    })
}

/// Get memory usage statistics (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_get_usage(
    memory_ptr: *mut c_void,
    store_ptr: *mut c_void,
    current_bytes_out: *mut usize,
    current_pages_out: *mut c_uint,
    peak_bytes_out: *mut usize,
    read_count_out: *mut c_ulong,
    write_count_out: *mut c_ulong,
    bytes_transferred_out: *mut c_ulong,
    utilization_percent_out: *mut f64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let memory = unsafe { ffi_utils::deref_ptr::<Memory>(memory_ptr, "memory")? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let usage = memory.get_usage(store)?;

        unsafe {
            if !current_bytes_out.is_null() {
                *current_bytes_out = usage.current_bytes;
            }
            if !current_pages_out.is_null() {
                *current_pages_out = usage.current_pages as c_uint;
            }
            if !peak_bytes_out.is_null() {
                *peak_bytes_out = usage.peak_bytes;
            }
            if !read_count_out.is_null() {
                *read_count_out = usage.read_count as c_ulong;
            }
            if !write_count_out.is_null() {
                *write_count_out = usage.write_count as c_ulong;
            }
            if !bytes_transferred_out.is_null() {
                *bytes_transferred_out = usage.bytes_transferred as c_ulong;
            }
            if !utilization_percent_out.is_null() {
                *utilization_percent_out = usage.utilization_percent;
            }
        }

        Ok(())
    })
}

/// Create a memory registry (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_registry_create(registry_ptr: *mut *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let registry = MemoryRegistry::new();

        unsafe {
            *registry_ptr = Box::into_raw(Box::new(registry)) as *mut c_void;
        }

        Ok(())
    })
}

/// Register a memory in the registry (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_registry_register(
    registry_ptr: *mut c_void,
    memory_ptr: *mut c_void,
    memory_id_out: *mut c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let registry = unsafe { ffi_utils::deref_ptr::<MemoryRegistry>(registry_ptr, "registry")? };
        let memory = unsafe { Box::from_raw(memory_ptr as *mut Memory) };

        let memory_id = registry.register(*memory)?;

        unsafe {
            *memory_id_out = memory_id;
        }

        Ok(())
    })
}

/// Get memory from registry (Panama FFI version)
///
/// IMPORTANT: This function transfers Arc ownership to the caller via Arc::into_raw().
/// The caller MUST eventually call wasmtime4j_panama_memory_release() to release the Arc
/// reference, otherwise there will be a memory leak.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_registry_get(
    registry_ptr: *mut c_void,
    memory_id: c_uint,
    memory_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let registry = unsafe { ffi_utils::deref_ptr::<MemoryRegistry>(registry_ptr, "registry")? };

        let memory_arc = registry.get(memory_id)?;

        // Transfer Arc ownership to caller using into_raw()
        // Caller must call wasmtime4j_panama_memory_release() to release the reference
        unsafe {
            *memory_ptr = Arc::into_raw(memory_arc) as *mut c_void;
        }

        Ok(())
    })
}

/// Release an Arc reference obtained from wasmtime4j_panama_memory_registry_get
///
/// This decrements the Arc reference count. The memory will be freed when
/// all references are released.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_release(memory_ptr: *mut c_void) {
    if !memory_ptr.is_null() {
        unsafe {
            // Reconstruct Arc and let it drop to decrement reference count
            let _ = Arc::from_raw(memory_ptr as *const Memory);
        }
    }
}

/// Destroy a memory instance (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_destroy(memory_ptr: *mut c_void) {
    // SAFETY IMPROVEMENT: Using safe resource destruction with validation
    unsafe {
        crate::ffi_common::memory_utils::destroy_ffi_resource::<Memory>(memory_ptr, "Memory");
    }
}

/// Destroy a memory registry (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_registry_destroy(registry_ptr: *mut c_void) {
    if !registry_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(registry_ptr as *mut MemoryRegistry);
        }
        log::debug!("Memory registry destroyed successfully");
    }
}

/// Clear all memory and store handle registries (for testing purposes)
///
/// This function clears both memory and store handle registries to prevent
/// stale handles from interfering with subsequent tests. Should only be
/// called in test teardown after all handles have been properly destroyed.
///
/// # Returns
/// 0 on success, negative error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_memory_clear_handle_registries() -> c_int {
    match crate::memory::core::clear_handle_registries() {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Clear the destroyed pointers registry (for testing purposes)
///
/// This function clears the HashSet tracking destroyed pointers to prevent
/// unbounded memory growth during large test suite execution.
/// Should only be called in test teardown after all native resources
/// have been properly destroyed.
///
/// # Returns
/// The number of entries cleared from the registry
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_clear_destroyed_pointers() -> c_ulong {
    crate::error::ffi_utils::clear_destroyed_pointers() as c_ulong
}
