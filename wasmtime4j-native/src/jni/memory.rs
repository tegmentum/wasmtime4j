//! JNI bindings for Memory operations

use jni::objects::{JByteArray, JByteBuffer, JClass, JValue};
use jni::sys::{jboolean, jbyte, jbyteArray, jint, jlong, jlongArray, jobject, jstring};
use jni::JNIEnv;

use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
use crate::memory::core;

/// Get memory size in bytes (JNI version) with comprehensive validation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetSize(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        // Comprehensive parameter validation with detailed error context
        if memory_ptr == 0 {
            log::error!("JNI Memory.nativeGetSize: null memory handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null. Ensure memory is properly initialized before calling size operations.".to_string(),
            });
        }

        // Check for obviously invalid pointers (basic sanity check)
        if memory_ptr < 0x1000 || memory_ptr == -1 {
            log::error!(
                "JNI Memory.nativeGetSize: invalid memory handle 0x{:x}",
                memory_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid memory handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.",
                    memory_ptr
                ),
            });
        }

        // Validate memory handle with detailed error context
        unsafe {
            core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                .map_err(|e| {
                    log::error!("Memory handle validation failed for handle 0x{:x}: {}", memory_ptr, e);
                    match e {
                        WasmtimeError::InvalidParameter { message } if message.contains("not registered") => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle (0x{:x}) is not registered or has been freed. \
                                     This typically indicates use-after-free or double-free. \
                                     Ensure memory lifetime is properly managed.",
                                    memory_ptr
                                ),
                            }
                        },
                        WasmtimeError::InvalidParameter { message } if message.contains("corrupted") => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle (0x{:x}) is corrupted (invalid magic number). \
                                     This indicates memory corruption or buffer overflow. \
                                     Check for memory safety violations.",
                                    memory_ptr
                                ),
                            }
                        },
                        WasmtimeError::InvalidParameter { message } if message.contains("destroyed") => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle (0x{:x}) has been destroyed (use-after-free detected). \
                                     Avoid accessing memory after calling close() or destroy().",
                                    memory_ptr
                                ),
                            }
                        },
                        _ => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle validation failed (0x{:x}): {}. \
                                     Verify that memory was created properly and has not been freed.",
                                    memory_ptr, e
                                ),
                            }
                        }
                    }
                })?
        };

        // Get memory reference for metadata access
        let memory = unsafe {
            core::get_memory_ref(memory_ptr as *const std::os::raw::c_void).map_err(|e| {
                log::error!(
                    "Failed to get memory reference for handle 0x{:x}: {}",
                    memory_ptr,
                    e
                );
                WasmtimeError::Memory {
                    message: format!(
                        "Unable to access memory (handle: 0x{:x}): {}. \
                             Memory may be in an invalid state.",
                        memory_ptr, e
                    ),
                }
            })?
        };

        // Get metadata with error handling
        let metadata = memory.get_metadata().map_err(|e| {
            log::error!(
                "Failed to get memory metadata for handle 0x{:x}: {}",
                memory_ptr,
                e
            );
            WasmtimeError::Memory {
                message: format!(
                    "Unable to retrieve memory metadata (handle: 0x{:x}): {}. \
                         Memory statistics may be corrupted.",
                    memory_ptr, e
                ),
            }
        })?;

        // Calculate size with overflow protection
        let pages = metadata.current_pages;
        let size_bytes = pages.checked_mul(65536)
            .ok_or_else(|| {
                log::error!("Memory size overflow: {} pages exceeds maximum addressable size", pages);
                WasmtimeError::Memory {
                    message: format!(
                        "Memory size calculation overflow: {} pages would exceed maximum addressable memory. \
                         This indicates corrupted memory metadata.",
                        pages
                    ),
                }
            })?;

        // Check that size fits in jlong (i64)
        if size_bytes > i64::MAX as u64 {
            log::error!("Memory size {} exceeds maximum jlong value", size_bytes);
            return Err(WasmtimeError::Memory {
                message: format!(
                    "Memory size ({} bytes) exceeds maximum representable value for Java long. \
                     This indicates an extremely large memory allocation that cannot be handled.",
                    size_bytes
                ),
            });
        }

        log::debug!(
            "Memory size retrieved: {} bytes ({} pages) for handle 0x{:x}",
            size_bytes,
            pages,
            memory_ptr
        );

        Ok(size_bytes as jlong)
    })
}

/// Grow memory by pages (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGrow(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    pages: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        // Comprehensive parameter validation
        if memory_ptr == 0 {
            log::error!("JNI Memory.nativeGrow: null memory handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null for growth operations. Ensure memory is properly initialized.".to_string(),
            });
        }

        if store_ptr == 0 {
            log::error!("JNI Memory.nativeGrow: null store handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null for memory growth operations. Ensure store is properly initialized.".to_string(),
            });
        }

        if memory_ptr < 0x1000 || memory_ptr == -1 {
            log::error!(
                "JNI Memory.nativeGrow: invalid memory handle 0x{:x}",
                memory_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid memory handle (0x{:x}) for growth operation. Handle appears corrupted or uninitialized.",
                    memory_ptr
                ),
            });
        }

        if pages < 0 {
            log::error!(
                "JNI Memory.nativeGrow: negative page count {} provided for handle 0x{:x}",
                pages,
                memory_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Page count for memory growth cannot be negative (received: {}). Specify a non-negative number of pages to grow.",
                    pages
                ),
            });
        }

        // Get memory and store references with validation
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };

        // Perform the memory growth operation with comprehensive error handling
        match core::grow_memory(memory, store, pages as u64) {
            Ok(previous_pages) => {
                log::debug!(
                    "JNI Memory.nativeGrow: successfully grew memory by {} pages for handle 0x{:x}, previous size: {} pages",
                    pages, memory_ptr, previous_pages
                );
                Ok(previous_pages as jlong)
            }
            Err(e) => {
                log::error!(
                    "JNI Memory.nativeGrow: growth failed for handle 0x{:x} with {} pages: {}",
                    memory_ptr,
                    pages,
                    e
                );
                Err(e)
            }
        }
    })
}

/// Grow memory asynchronously (JNI version)
///
/// Requires engine with `async_support(true)`. Uses the async resource limiter.
#[no_mangle]
#[cfg(feature = "async")]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGrowAsync(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    pages: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if pages < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Page count cannot be negative: {}", pages),
            });
        }

        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };

        let previous_pages = core::grow_memory_async(memory, store, pages as u64)?;
        Ok(previous_pages as jlong)
    })
}

/// Read a single byte from memory (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeReadByte(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        // Comprehensive parameter validation with bounds checking
        if memory_ptr == 0 {
            log::error!("JNI Memory.nativeReadByte: null memory handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null for read operations. Ensure memory is properly initialized.".to_string(),
            });
        }

        if store_ptr == 0 {
            log::error!("JNI Memory.nativeReadByte: null store handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null for memory operations. Ensure store is properly initialized.".to_string(),
            });
        }

        if memory_ptr < 0x1000 || memory_ptr == -1 {
            log::error!(
                "JNI Memory.nativeReadByte: invalid memory handle 0x{:x}",
                memory_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid memory handle (0x{:x}) for read operation. Handle appears corrupted or uninitialized.",
                    memory_ptr
                ),
            });
        }

        if offset < 0 {
            log::error!(
                "JNI Memory.nativeReadByte: negative offset {} for handle 0x{:x}",
                offset,
                memory_ptr
            );
            return Err(WasmtimeError::Memory {
                message: format!(
                    "Memory read offset cannot be negative (received: {}). \
                     Specify a non-negative byte offset within memory bounds.",
                    offset
                ),
            });
        }

        // Get memory and store references with validation
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };

        // Perform the memory read operation with comprehensive error handling
        match core::read_memory_byte(memory, store, offset as usize) {
            Ok(byte_value) => {
                log::debug!(
                    "JNI Memory.nativeReadByte: successfully read byte {} from offset {} for handle 0x{:x}",
                    byte_value, offset, memory_ptr
                );
                Ok(byte_value as jint)
            }
            Err(e) => {
                log::error!(
                    "JNI Memory.nativeReadByte: read failed for handle 0x{:x} at offset {}: {}",
                    memory_ptr,
                    offset,
                    e
                );
                Err(e)
            }
        }
    })
}

/// Write a single byte to memory (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeWriteByte(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jlong,
    value: jint,
) {
    jni_utils::jni_try_code(&mut env, || {
        // Comprehensive parameter validation with bounds checking
        if memory_ptr == 0 {
            log::error!("JNI Memory.nativeWriteByte: null memory handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null for write operations. Ensure memory is properly initialized.".to_string(),
            });
        }

        if store_ptr == 0 {
            log::error!("JNI Memory.nativeWriteByte: null store handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null for memory operations. Ensure store is properly initialized.".to_string(),
            });
        }

        if memory_ptr < 0x1000 || memory_ptr == -1 {
            log::error!(
                "JNI Memory.nativeWriteByte: invalid memory handle 0x{:x}",
                memory_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid memory handle (0x{:x}) for write operation. Handle appears corrupted or uninitialized.",
                    memory_ptr
                ),
            });
        }

        if offset < 0 {
            log::error!(
                "JNI Memory.nativeWriteByte: negative offset {} for handle 0x{:x}",
                offset,
                memory_ptr
            );
            return Err(WasmtimeError::Memory {
                message: format!(
                    "Memory write offset cannot be negative (received: {}). \
                     Specify a non-negative byte offset within memory bounds.",
                    offset
                ),
            });
        }

        if value < -128 || value > 255 {
            log::error!(
                "JNI Memory.nativeWriteByte: invalid byte value {} for handle 0x{:x}",
                value,
                memory_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Byte value must be in range [-128, 255] (received: {}). \
                     Provide a valid byte value for memory write operation.",
                    value
                ),
            });
        }

        // Get memory and store references with validation
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };

        // Convert jint to u8 (handling signed/unsigned conversion safely)
        let byte_value = if value < 0 {
            (value + 256) as u8 // Convert signed negative to unsigned equivalent
        } else {
            value as u8
        };

        // Perform the memory write operation with comprehensive error handling
        match core::write_memory_byte(memory, store, offset as usize, byte_value) {
            Ok(_) => {
                log::debug!(
                    "JNI Memory.nativeWriteByte: successfully wrote byte {} (raw: {}) to offset {} for handle 0x{:x}",
                    byte_value, value, offset, memory_ptr
                );
                Ok(())
            }
            Err(e) => {
                log::error!(
                    "JNI Memory.nativeWriteByte: write failed for handle 0x{:x} at offset {} with value {}: {}",
                    memory_ptr, offset, value, e
                );
                Err(e)
            }
        }
    });
}

/// Read bytes from memory into a buffer (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeReadBytes(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jlong,
    buffer: JByteArray,
) -> jint {
    // Validate parameters before entering panic-safe block
    if memory_ptr == 0 {
        log::error!("JNI Memory.nativeReadBytes: null memory handle provided");
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
            message: "Memory handle cannot be null for bulk read operations. Ensure memory is properly initialized.".to_string(),
        });
        return -1;
    }
    if store_ptr == 0 {
        log::error!("JNI Memory.nativeReadBytes: null store handle provided");
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
            message: "Store handle cannot be null for memory operations. Ensure store is properly initialized.".to_string(),
        });
        return -1;
    }
    if memory_ptr < 0x1000 || memory_ptr == -1 {
        log::error!("JNI Memory.nativeReadBytes: invalid memory handle 0x{:x}", memory_ptr);
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
            message: format!("Invalid memory handle (0x{:x}) for bulk read operation. Handle appears corrupted or uninitialized.", memory_ptr),
        });
        return -1;
    }
    if offset < 0 {
        log::error!("JNI Memory.nativeReadBytes: negative offset {} for handle 0x{:x}", offset, memory_ptr);
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::Memory {
            message: format!("Memory read offset cannot be negative (received: {}). Specify a non-negative byte offset within memory bounds.", offset),
        });
        return -1;
    }
    if buffer.is_null() {
        log::error!("JNI Memory.nativeReadBytes: null buffer provided for handle 0x{:x}", memory_ptr);
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
            message: "Buffer cannot be null for bulk read operations. Provide a valid byte array.".to_string(),
        });
        return -1;
    }

    // Get buffer length using env (before panic-safe block)
    let buffer_length = match env.get_array_length(&buffer) {
        Ok(len) => len as usize,
        Err(e) => {
            log::error!("Failed to get buffer length for read operation (handle 0x{:x}): {:?}", memory_ptr, e);
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
                message: format!("Cannot determine buffer size for read operation: {:?}. Ensure buffer is a valid Java byte array.", e),
            });
            return -1;
        }
    };
    if buffer_length == 0 {
        log::debug!("JNI Memory.nativeReadBytes: zero-length read requested for handle 0x{:x} at offset {}", memory_ptr, offset);
        return 0;
    }

    // Panic-safe block for unsafe memory operations
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        core::read_memory_bytes(memory, store, offset as usize, buffer_length)
    }));

    match result {
        Ok(Ok(read_data)) => {
            let signed_data: Vec<i8> = read_data.iter().map(|&b| b as i8).collect();
            if let Err(e) = env.set_byte_array_region(&buffer, 0, &signed_data) {
                log::error!("JNI Memory.nativeReadBytes: failed to set buffer data for handle 0x{:x}: {}", memory_ptr, e);
                jni_utils::throw_jni_exception(&mut env, &WasmtimeError::Memory {
                    message: format!("Failed to copy data to Java buffer: {}", e),
                });
                return -1;
            }
            log::debug!("JNI Memory.nativeReadBytes: successfully read {} bytes from offset {} for handle 0x{:x}", read_data.len(), offset, memory_ptr);
            read_data.len() as jint
        }
        Ok(Err(e)) => {
            log::error!("JNI Memory.nativeReadBytes: read failed for handle 0x{:x} at offset {} length {}: {}", memory_ptr, offset, buffer_length, e);
            jni_utils::throw_jni_exception(&mut env, &e);
            -1
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            -1
        }
    }
}

/// Write bytes from a buffer to memory (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeWriteBytes(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jlong,
    buffer: JByteArray,
) -> jint {
    // Validate parameters before entering panic-safe block
    if memory_ptr == 0 {
        log::error!("JNI Memory.nativeWriteBytes: null memory handle provided");
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
            message: "Memory handle cannot be null for bulk write operations. Ensure memory is properly initialized.".to_string(),
        });
        return -1;
    }
    if store_ptr == 0 {
        log::error!("JNI Memory.nativeWriteBytes: null store handle provided");
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
            message: "Store handle cannot be null for memory operations. Ensure store is properly initialized.".to_string(),
        });
        return -1;
    }
    if memory_ptr < 0x1000 || memory_ptr == -1 {
        log::error!("JNI Memory.nativeWriteBytes: invalid memory handle 0x{:x}", memory_ptr);
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
            message: format!("Invalid memory handle (0x{:x}) for bulk write operation. Handle appears corrupted or uninitialized.", memory_ptr),
        });
        return -1;
    }
    if offset < 0 {
        log::error!("JNI Memory.nativeWriteBytes: negative offset {} for handle 0x{:x}", offset, memory_ptr);
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::Memory {
            message: format!("Memory write offset cannot be negative (received: {}). Specify a non-negative byte offset within memory bounds.", offset),
        });
        return -1;
    }
    if buffer.is_null() {
        log::error!("JNI Memory.nativeWriteBytes: null buffer provided for handle 0x{:x}", memory_ptr);
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::InvalidParameter {
            message: "Buffer cannot be null for bulk write operations. Provide a valid byte array.".to_string(),
        });
        return -1;
    }

    // Get buffer length using env (before panic-safe block)
    let buffer_length = match env.get_array_length(&buffer) {
        Ok(len) => len as usize,
        Err(e) => {
            log::error!("JNI Memory.nativeWriteBytes: failed to get buffer length for handle 0x{:x}: {}", memory_ptr, e);
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::Memory {
                message: format!("Failed to get buffer length for write operation: {}", e),
            });
            return -1;
        }
    };
    if buffer_length == 0 {
        log::debug!("JNI Memory.nativeWriteBytes: zero-length write requested for handle 0x{:x} at offset {}", memory_ptr, offset);
        return 0;
    }

    // Get Java buffer data using env (before panic-safe block)
    let mut signed_buffer = vec![0i8; buffer_length];
    if let Err(e) = env.get_byte_array_region(&buffer, 0, &mut signed_buffer) {
        log::error!("JNI Memory.nativeWriteBytes: failed to read buffer data for handle 0x{:x}: {}", memory_ptr, e);
        jni_utils::throw_jni_exception(&mut env, &WasmtimeError::Memory {
            message: format!("Failed to read buffer data for write operation: {}", e),
        });
        return -1;
    }
    let write_data: Vec<u8> = signed_buffer.iter().map(|&b| b as u8).collect();

    // Panic-safe block for unsafe memory operations
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::write_memory_bytes(memory, store, offset as usize, &write_data)
    }));

    match result {
        Ok(Ok(())) => {
            log::debug!("JNI Memory.nativeWriteBytes: successfully wrote {} bytes at offset {} for handle 0x{:x}", buffer_length, offset, memory_ptr);
            buffer_length as jint
        }
        Ok(Err(e)) => {
            log::error!("JNI Memory.nativeWriteBytes: write failed for handle 0x{:x} at offset {} length {}: {}", memory_ptr, offset, buffer_length, e);
            jni_utils::throw_jni_exception(&mut env, &e);
            -1
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            -1
        }
    }
}

/// Get direct ByteBuffer view of memory (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetBuffer<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    memory_ptr: jlong,
    store_ptr: jlong,
) -> JByteBuffer<'a> {
    // Helper macro for error handling without closures (avoids borrow conflicts)
    macro_rules! handle_error {
        ($error:expr) => {{
            jni_utils::throw_jni_exception(&mut env, &$error);
            return JByteBuffer::default();
        }};
    }

    // Comprehensive parameter validation
    if memory_ptr == 0 {
        log::error!("JNI Memory.nativeGetBuffer: null memory handle provided");
        handle_error!(WasmtimeError::InvalidParameter {
            message: "Memory handle cannot be null for buffer access. Ensure memory is properly initialized.".to_string(),
        });
    }

    if store_ptr == 0 {
        log::error!("JNI Memory.nativeGetBuffer: null store handle provided");
        handle_error!(WasmtimeError::InvalidParameter {
            message: "Store handle cannot be null for memory buffer access. Ensure store is properly initialized.".to_string(),
        });
    }

    if memory_ptr < 0x1000 || memory_ptr == -1 {
        log::error!(
            "JNI Memory.nativeGetBuffer: invalid memory handle 0x{:x}",
            memory_ptr
        );
        handle_error!(WasmtimeError::InvalidParameter {
            message: format!(
                "Invalid memory handle (0x{:x}) for buffer access. Handle appears corrupted or uninitialized.",
                memory_ptr
            ),
        });
    }

    // Get memory and store references with validation
    let memory = match unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void) } {
        Ok(m) => m,
        Err(e) => handle_error!(e),
    };
    let store = match unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void) } {
        Ok(s) => s,
        Err(e) => handle_error!(e),
    };

    // Get memory buffer information
    let (buffer_ptr, buffer_size) = match core::get_memory_buffer(memory, store) {
        Ok(result) => result,
        Err(e) => {
            log::error!(
                "JNI Memory.nativeGetBuffer: failed to get memory buffer for handle 0x{:x}: {}",
                memory_ptr,
                e
            );
            handle_error!(WasmtimeError::Memory {
                message: format!("Failed to get memory buffer: {}", e),
            });
        }
    };

    // Create a direct ByteBuffer wrapping the Wasm memory
    // SAFETY: The buffer is valid as long as the memory and store are alive.
    // The caller is responsible for ensuring the memory/store are not destroyed
    // while the ByteBuffer is in use.
    log::debug!(
        "JNI Memory.nativeGetBuffer: creating ByteBuffer for handle 0x{:x} with size {} bytes",
        memory_ptr,
        buffer_size
    );

    // Create a direct ByteBuffer using JNI
    let byte_buffer =
        match unsafe { env.new_direct_byte_buffer(buffer_ptr as *mut u8, buffer_size) } {
            Ok(buf) => buf,
            Err(e) => {
                log::error!(
                    "JNI Memory.nativeGetBuffer: failed to create DirectByteBuffer: {}",
                    e
                );
                handle_error!(WasmtimeError::Memory {
                    message: format!("Failed to create DirectByteBuffer: {}", e),
                });
            }
        };

    log::debug!(
        "JNI Memory.nativeGetBuffer: successfully created ByteBuffer for handle 0x{:x}",
        memory_ptr
    );

    byte_buffer
}

/// Get the raw data pointer of memory as a long (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetDataPtr(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
) -> jlong {
    macro_rules! handle_error {
        ($error:expr) => {{
            jni_utils::throw_jni_exception(&mut env, &$error);
            return 0;
        }};
    }

    if memory_ptr == 0 {
        handle_error!(WasmtimeError::InvalidParameter {
            message: "Memory handle cannot be null for data pointer access.".to_string(),
        });
    }

    if store_ptr == 0 {
        handle_error!(WasmtimeError::InvalidParameter {
            message: "Store handle cannot be null for data pointer access.".to_string(),
        });
    }

    let memory = match unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void) } {
        Ok(m) => m,
        Err(e) => handle_error!(e),
    };
    let store = match unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void) } {
        Ok(s) => s,
        Err(e) => handle_error!(e),
    };

    let (buffer_ptr, _buffer_size) = match core::get_memory_buffer(memory, store) {
        Ok(result) => result,
        Err(e) => {
            log::error!(
                "JNI Memory.nativeGetDataPtr: failed to get memory buffer for handle 0x{:x}: {}",
                memory_ptr,
                e
            );
            handle_error!(WasmtimeError::Memory {
                message: format!("Failed to get memory data pointer: {}", e),
            });
        }
    };

    buffer_ptr as jlong
}

/// Destroy memory (JNI version) with comprehensive validation and cleanup
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeDestroyMemory(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        // Enhanced validation with detailed error logging
        if memory_ptr == 0 {
            log::warn!("JNI Memory.nativeDestroyMemory called with null memory pointer");
            return Ok(());
        }

        log::debug!("Destroying memory handle: 0x{:x}", memory_ptr);

        unsafe {
            // Use the enhanced destroy_memory function which includes validation
            core::destroy_memory(memory_ptr as *mut std::os::raw::c_void);
        }

        log::debug!("Memory handle destruction completed: 0x{:x}", memory_ptr);
        Ok(())
    });
}

/// Get memory page count (JNI version) with comprehensive validation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetPageCount(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        // Comprehensive parameter validation with detailed error context
        if memory_ptr == 0 {
            log::error!("JNI Memory.nativeGetPageCount: null memory handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null. Ensure memory is properly initialized before calling page count operations.".to_string(),
            });
        }

        // Check for obviously invalid pointers (basic sanity check)
        if memory_ptr < 0x1000 || memory_ptr == -1 {
            log::error!(
                "JNI Memory.nativeGetPageCount: invalid memory handle 0x{:x}",
                memory_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid memory handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.",
                    memory_ptr
                ),
            });
        }

        // Validate memory handle with comprehensive error mapping
        unsafe {
            core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                .map_err(|e| {
                    log::error!("Memory handle validation failed for page count operation on handle 0x{:x}: {}", memory_ptr, e);
                    match e {
                        WasmtimeError::InvalidParameter { message } if message.contains("not registered") => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle (0x{:x}) is not registered or has been freed. \
                                     Cannot retrieve page count from unregistered memory. \
                                     Ensure memory lifetime is properly managed.",
                                    memory_ptr
                                ),
                            }
                        },
                        WasmtimeError::InvalidParameter { message } if message.contains("destroyed") => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle (0x{:x}) has been destroyed (use-after-free detected). \
                                     Cannot retrieve page count from destroyed memory. \
                                     Avoid accessing memory after calling close() or destroy().",
                                    memory_ptr
                                ),
                            }
                        },
                        _ => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle validation failed for page count operation (0x{:x}): {}. \
                                     Verify that memory was created properly and is in a valid state.",
                                    memory_ptr, e
                                ),
                            }
                        }
                    }
                })?
        };

        // Get memory reference for metadata access
        let memory = unsafe {
            core::get_memory_ref(memory_ptr as *const std::os::raw::c_void).map_err(|e| {
                log::error!(
                    "Failed to get memory reference for page count operation on handle 0x{:x}: {}",
                    memory_ptr,
                    e
                );
                WasmtimeError::Memory {
                    message: format!(
                        "Unable to access memory for page count operation (handle: 0x{:x}): {}. \
                             Memory may be in an invalid state or corrupted.",
                        memory_ptr, e
                    ),
                }
            })?
        };

        // Get metadata with comprehensive error handling
        let metadata = memory.get_metadata().map_err(|e| {
            log::error!(
                "Failed to get memory metadata for page count operation on handle 0x{:x}: {}",
                memory_ptr,
                e
            );
            WasmtimeError::Memory {
                message: format!(
                    "Unable to retrieve memory metadata for page count (handle: 0x{:x}): {}. \
                         Memory statistics may be corrupted or inaccessible.",
                    memory_ptr, e
                ),
            }
        })?;

        let pages = metadata.current_pages;

        // Validate page count is reasonable (basic sanity check for 32-bit memory)
        if !memory.get_config().is_64 {
            const MAX_WASM_PAGES: u64 = 65536; // 4GB / 64KB
            if pages > MAX_WASM_PAGES {
                log::error!(
                    "Memory page count {} exceeds WebAssembly 32-bit limit {}",
                    pages,
                    MAX_WASM_PAGES
                );
                return Err(WasmtimeError::Memory {
                    message: format!(
                        "Memory page count ({}) exceeds WebAssembly 32-bit limit ({}). \
                         This indicates corrupted memory metadata or invalid memory state.",
                        pages, MAX_WASM_PAGES
                    ),
                });
            }
        }

        log::debug!(
            "Memory page count retrieved: {} pages for handle 0x{:x}",
            pages,
            memory_ptr
        );

        Ok(pages as jlong)
    })
}

/// Get memory maximum size in pages (JNI version) with comprehensive validation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetMaxSize(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        // Comprehensive parameter validation with detailed error context
        if memory_ptr == 0 {
            log::error!("JNI Memory.nativeGetMaxSize: null memory handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null. Ensure memory is properly initialized before calling max size operations.".to_string(),
            });
        }

        // Check for obviously invalid pointers (basic sanity check)
        if memory_ptr < 0x1000 || memory_ptr == -1 {
            log::error!(
                "JNI Memory.nativeGetMaxSize: invalid memory handle 0x{:x}",
                memory_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid memory handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.",
                    memory_ptr
                ),
            });
        }

        // Validate memory handle with comprehensive error mapping
        unsafe {
            core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                .map_err(|e| {
                    log::error!("Memory handle validation failed for max size operation on handle 0x{:x}: {}", memory_ptr, e);
                    match e {
                        WasmtimeError::InvalidParameter { message } if message.contains("not registered") => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle (0x{:x}) is not registered or has been freed. \
                                     Cannot retrieve max size from unregistered memory. \
                                     Ensure memory lifetime is properly managed.",
                                    memory_ptr
                                ),
                            }
                        },
                        WasmtimeError::InvalidParameter { message } if message.contains("destroyed") => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle (0x{:x}) has been destroyed (use-after-free detected). \
                                     Cannot retrieve max size from destroyed memory. \
                                     Avoid accessing memory after calling close() or destroy().",
                                    memory_ptr
                                ),
                            }
                        },
                        _ => {
                            WasmtimeError::Memory {
                                message: format!(
                                    "Memory handle validation failed for max size operation (0x{:x}): {}. \
                                     Verify that memory was created properly and is in a valid state.",
                                    memory_ptr, e
                                ),
                            }
                        }
                    }
                })?
        };

        // Get memory reference for metadata access
        let memory = unsafe {
            core::get_memory_ref(memory_ptr as *const std::os::raw::c_void).map_err(|e| {
                log::error!(
                    "Failed to get memory reference for max size operation on handle 0x{:x}: {}",
                    memory_ptr,
                    e
                );
                WasmtimeError::Memory {
                    message: format!(
                        "Unable to access memory for max size operation (handle: 0x{:x}): {}. \
                             Memory may be in an invalid state or corrupted.",
                        memory_ptr, e
                    ),
                }
            })?
        };

        // Get metadata with comprehensive error handling
        let metadata = memory.get_metadata().map_err(|e| {
            log::error!(
                "Failed to get memory metadata for max size operation on handle 0x{:x}: {}",
                memory_ptr,
                e
            );
            WasmtimeError::Memory {
                message: format!(
                    "Unable to retrieve memory metadata for max size (handle: 0x{:x}): {}. \
                         Memory statistics may be corrupted or inaccessible.",
                    memory_ptr, e
                ),
            }
        })?;

        let max_pages = match metadata.maximum_pages {
            Some(pages) => {
                // Validate max page count is reasonable (basic sanity check for 32-bit memory)
                if !memory.get_config().is_64 {
                    const MAX_WASM_PAGES: u64 = 65536; // 4GB / 64KB
                    if pages > MAX_WASM_PAGES {
                        log::error!(
                            "Memory max page count {} exceeds WebAssembly 32-bit limit {}",
                            pages,
                            MAX_WASM_PAGES
                        );
                        return Err(WasmtimeError::Memory {
                            message: format!(
                                "Memory max page count ({}) exceeds WebAssembly 32-bit limit ({}). \
                                 This indicates corrupted memory metadata or invalid memory state.",
                                pages, MAX_WASM_PAGES
                            ),
                        });
                    }
                }
                pages as jlong
            }
            None => -1, // Unlimited memory
        };

        log::debug!(
            "Memory max size retrieved: {} pages for handle 0x{:x}",
            max_pages,
            memory_ptr
        );

        Ok(max_pages)
    })
}

/// Validate memory handle and return diagnostics (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeValidateHandle(
    _env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jboolean {
    if memory_ptr == 0 {
        log::debug!("Memory handle validation failed: null pointer");
        return 0; // false
    }

    match unsafe { core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void) } {
        Ok(_) => {
            log::debug!("Memory handle validation succeeded: 0x{:x}", memory_ptr);
            1 // true
        }
        Err(e) => {
            log::debug!(
                "Memory handle validation failed: 0x{:x}, error: {}",
                memory_ptr,
                e
            );
            0 // false
        }
    }
}

/// Get memory handle diagnostics (JNI version) - returns access count
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetAccessCount(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jlong {
    if memory_ptr == 0 {
        return -1;
    }

    jni_utils::jni_try_with_default(&mut env, -1, || unsafe {
        core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)?;

        let validated_memory = &*(memory_ptr as *const core::ValidatedMemory);
        Ok(validated_memory.get_access_count() as jlong)
    })
}

/// Get global memory handle diagnostics (JNI version) - returns handle count and total accesses
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetGlobalDiagnostics(
    mut env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> WasmtimeResult<Vec<u8>> {
        let (handle_count, total_accesses) = core::get_memory_handle_diagnostics()?;
        // Pack both values into a byte array: [handle_count: 4 bytes][total_accesses: 8 bytes]
        let mut data = Vec::with_capacity(12);
        data.extend_from_slice(&(handle_count as u32).to_le_bytes());
        data.extend_from_slice(&total_accesses.to_le_bytes());
        Ok(data)
    })) {
        Ok(Ok(data)) => match env.byte_array_from_slice(&data) {
            Ok(jarray) => jarray.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            std::ptr::null_mut()
        }
    }
}

// Import table core functions for root-level JNI functions
use crate::table::core::{get_table_metadata, get_table_ref};

/// Get element type of a table (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetElementType(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    _store_ptr: jlong,
) -> jstring {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> WasmtimeResult<jstring> {
        if table_ptr == 0 {
            log::error!("JNI Table.nativeGetElementType: null table handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Table handle cannot be null. Ensure table is properly initialized before calling element type operations.".to_string(),
            });
        }

        if table_ptr < 0x1000 || table_ptr == -1 {
            log::error!(
                "JNI Table.nativeGetElementType: invalid table handle 0x{:x}",
                table_ptr
            );
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid table handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.",
                    table_ptr
                ),
            });
        }

        // Get table reference and metadata to determine actual element type
        let table = unsafe { get_table_ref(table_ptr as *const std::os::raw::c_void)? };
        let metadata = get_table_metadata(table);

        // Convert ValType to string representation
        let element_type_str = match &metadata.element_type {
            wasmtime::ValType::Ref(ref_type) => {
                // Discriminate between different reference types
                match ref_type.heap_type() {
                    wasmtime::HeapType::Func => "funcref",
                    wasmtime::HeapType::Extern => "externref",
                    _ => "funcref", // Default to funcref for other types
                }
            }
            _ => {
                log::warn!(
                    "JNI Table.nativeGetElementType: unexpected non-reference element type {:?}",
                    metadata.element_type
                );
                "funcref" // Default fallback
            }
        };

        log::debug!(
            "JNI Table.nativeGetElementType: returning '{}' for table 0x{:x}",
            element_type_str,
            table_ptr
        );

        env.new_string(element_type_str)
            .map(|jstr| jstr.into_raw())
            .map_err(|e| WasmtimeError::Memory {
                message: format!("Failed to create string for table element type: {}", e),
            })
    })) {
        Ok(Ok(result)) => result,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Get element from table by index (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGet(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    index: jint,
) -> jobject {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> WasmtimeResult<jobject> {
        use std::os::raw::c_void;

        if table_ptr == 0 {
            log::error!("JNI Table.nativeGet: null table handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Table handle cannot be null. Ensure table is properly initialized before accessing elements.".to_string(),
            });
        }

        if store_ptr == 0 {
            log::error!("JNI Table.nativeGet: null store handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null.".to_string(),
            });
        }

        if index < 0 {
            log::error!("JNI Table.nativeGet: negative index {} provided", index);
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Table index cannot be negative (received: {}). Specify a non-negative index within table bounds.",
                    index
                ),
            });
        }

        // Get table and store references
        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        // Get element from table
        let element = table.get(store, index as u64)?;

        log::debug!(
            "JNI Table.nativeGet: retrieved element at index {} = {:?}",
            index,
            element
        );

        // Convert TableElement to Java object
        match element {
            crate::table::TableElement::FuncRef(None) => Ok(std::ptr::null_mut()),
            crate::table::TableElement::FuncRef(Some(func_id)) => {
                // Return a Long object representing the function ID
                let long_class =
                    env.find_class("java/lang/Long")
                        .map_err(|e| WasmtimeError::Runtime {
                            message: format!("Failed to find Long class: {}", e),
                            backtrace: None,
                        })?;
                let long_obj = env
                    .new_object(long_class, "(J)V", &[JValue::Long(func_id as jlong)])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to create Long object: {}", e),
                        backtrace: None,
                    })?;
                Ok(long_obj.into_raw())
            }
            crate::table::TableElement::ExternRef(None) => Ok(std::ptr::null_mut()),
            crate::table::TableElement::ExternRef(Some(extern_id)) => {
                // Return a Long object representing the extern reference ID
                let long_class =
                    env.find_class("java/lang/Long")
                        .map_err(|e| WasmtimeError::Runtime {
                            message: format!("Failed to find Long class: {}", e),
                            backtrace: None,
                        })?;
                let long_obj = env
                    .new_object(long_class, "(J)V", &[JValue::Long(extern_id as jlong)])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to create Long object: {}", e),
                        backtrace: None,
                    })?;
                Ok(long_obj.into_raw())
            }
            crate::table::TableElement::AnyRef(None) => Ok(std::ptr::null_mut()),
            crate::table::TableElement::AnyRef(Some(any_id)) => {
                // Return a Long object representing the any reference ID
                let long_class =
                    env.find_class("java/lang/Long")
                        .map_err(|e| WasmtimeError::Runtime {
                            message: format!("Failed to find Long class: {}", e),
                            backtrace: None,
                        })?;
                let long_obj = env
                    .new_object(long_class, "(J)V", &[JValue::Long(any_id as jlong)])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to create Long object: {}", e),
                        backtrace: None,
                    })?;
                Ok(long_obj.into_raw())
            }
        }
    })) {
        Ok(Ok(result)) => result,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Set element in table by index (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeSet(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    index: jint,
    value: jobject,
) -> jboolean {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> WasmtimeResult<jboolean> {
        if table_ptr == 0 {
            log::error!("JNI Table.nativeSet: null table handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Table handle cannot be null. Ensure table is properly initialized before setting elements.".to_string(),
            });
        }

        if store_ptr == 0 {
            log::error!("JNI Table.nativeSet: null store handle provided");
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null.".to_string(),
            });
        }

        if index < 0 {
            log::error!("JNI Table.nativeSet: negative index {} provided", index);
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Table index cannot be negative (received: {}). Specify a non-negative index within table bounds.",
                    index
                ),
            });
        }

        let table = unsafe { get_table_ref(table_ptr as *const std::os::raw::c_void)? };
        let store =
            unsafe { crate::store::core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };

        // Convert Java object to TableElement
        let element = if value.is_null() {
            // Null value - create null funcref
            crate::table::TableElement::FuncRef(None)
        } else {
            // Try to get the native handle from the Java object (it should be a JniFunction)
            let jvalue = unsafe { jni::objects::JObject::from_raw(value) };

            // Call getNativeHandle() on the Java object
            let handle_result = env.call_method(&jvalue, "getNativeHandle", "()J", &[]);

            match handle_result {
                Ok(jni::objects::JValueGen::Long(func_handle)) => {
                    if func_handle == 0 {
                        crate::table::TableElement::FuncRef(None)
                    } else {
                        // The handle points to a JniHostFunctionHandle struct containing:
                        // - host_function_id: u64 (for callback management)
                        // - func_ref_id: u64 (registry ID for table operations)
                        #[repr(C)]
                        struct JniHostFunctionHandle {
                            host_function_id: u64,
                            func_ref_id: u64,
                        }

                        let handle_struct =
                            unsafe { &*(func_handle as *const JniHostFunctionHandle) };
                        let func_ref_id = handle_struct.func_ref_id;

                        crate::table::TableElement::FuncRef(Some(func_ref_id))
                    }
                }
                Err(e) => {
                    log::error!(
                        "JNI Table.nativeSet: failed to get native handle from object: {}",
                        e
                    );
                    return Err(WasmtimeError::InvalidParameter {
                        message: format!("Failed to get native handle from function object: {}", e),
                    });
                }
                _ => {
                    log::error!("JNI Table.nativeSet: getNativeHandle returned unexpected type");
                    return Err(WasmtimeError::InvalidParameter {
                        message: "getNativeHandle returned unexpected type".to_string(),
                    });
                }
            }
        };

        // Set the table element using our Table wrapper's set method
        table.set(store, index as u64, element)?;

        log::debug!(
            "JNI Table.nativeSet: successfully set element at index {}",
            index
        );
        Ok(1)
    }));

    match result {
        Ok(Ok(v)) => v,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            0
        }
    }
}

/// Get memory type information directly from the memory (JNI version)
/// Returns array: [minimum, maximum(-1 if unlimited), is64Bit(0/1), isShared(0/1), pageSizeLog2]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetMemoryTypeInfo<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    memory_ptr: jlong,
) -> jlongArray {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> WasmtimeResult<jlongArray> {
        // Validate memory pointer
        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }

        // Get memory reference - pointer is to ValidatedMemory, not Memory directly
        let validated_memory = unsafe { &*(memory_ptr as *const core::ValidatedMemory) };
        let memory = validated_memory.access_memory()?;

        // Get type information from cached memory_type
        let minimum = memory.memory_type.minimum() as i64;
        let maximum = memory.memory_type.maximum().map(|m| m as i64).unwrap_or(-1);
        let is_shared = if memory.memory_type.is_shared() {
            1i64
        } else {
            0i64
        };
        let is_64_bit = if memory.memory_type.is_64() {
            1i64
        } else {
            0i64
        };
        let page_size_log2 = memory.memory_type.page_size_log2() as i64;

        // Create long array with [minimum, maximum, is64Bit, isShared, pageSizeLog2]
        let result_array = env.new_long_array(5).map_err(|e| WasmtimeError::Memory {
            message: format!("Failed to create long array: {}", e),
        })?;

        let values = vec![minimum, maximum, is_64_bit, is_shared, page_size_log2];
        env.set_long_array_region(&result_array, 0, &values)
            .map_err(|e| WasmtimeError::Memory {
                message: format!("Failed to set long array region: {}", e),
            })?;

        Ok(result_array.as_raw())
    })) {
        Ok(Ok(array)) => array,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            std::ptr::null_mut()
        }
    }
}

/// Get memory type information from a ValidatedMemory pointer (JNI version for JniMemoryType)
/// Returns array: [minimum, maximum(-1 if unlimited), is64Bit(0/1), isShared(0/1), pageSizeLog2]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_type_JniMemoryType_nativeGetMemoryTypeInfo<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    memory_type_ptr: jlong,
) -> jlongArray {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> WasmtimeResult<jlongArray> {
        if memory_type_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory type handle cannot be null".to_string(),
            });
        }

        // The nativeHandle for JniMemoryType points to the same ValidatedMemory
        let validated_memory = unsafe { &*(memory_type_ptr as *const core::ValidatedMemory) };
        let memory = validated_memory.access_memory()?;

        let minimum = memory.memory_type.minimum() as i64;
        let maximum = memory.memory_type.maximum().map(|m| m as i64).unwrap_or(-1);
        let is_shared = if memory.memory_type.is_shared() {
            1i64
        } else {
            0i64
        };
        let is_64_bit = if memory.memory_type.is_64() {
            1i64
        } else {
            0i64
        };
        let page_size_log2 = memory.memory_type.page_size_log2() as i64;

        let result_array = env.new_long_array(5).map_err(|e| WasmtimeError::Memory {
            message: format!("Failed to create long array: {}", e),
        })?;

        let values = vec![minimum, maximum, is_64_bit, is_shared, page_size_log2];
        env.set_long_array_region(&result_array, 0, &values)
            .map_err(|e| WasmtimeError::Memory {
                message: format!("Failed to set long array region: {}", e),
            })?;

        Ok(result_array.as_raw())
    })) {
        Ok(Ok(array)) => array,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            std::ptr::null_mut()
        }
    }
}

/// Initialize memory from a data segment (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryInit(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    instance_ptr: jlong,
    dest_offset: jint,
    data_segment_index: jint,
    src_offset: jint,
    length: jint,
) {
    jni_utils::jni_try_code(&mut env, || {
        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if instance_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Instance handle cannot be null".to_string(),
            });
        }

        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        let instance = unsafe {
            crate::instance::core::get_instance_ref(instance_ptr as *const std::os::raw::c_void)?
        };

        core::memory_init(
            memory,
            store,
            instance,
            dest_offset as u32,
            data_segment_index as u32,
            src_offset as u32,
            length as u32,
        )
    });
}

/// Initialize memory from a data segment using 64-bit offsets (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryInit64(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    instance_ptr: jlong,
    dest_offset: jlong,
    data_segment_index: jint,
    src_offset: jlong,
    length: jlong,
) {
    jni_utils::jni_try_code(&mut env, || {
        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        if instance_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Instance handle cannot be null".to_string(),
            });
        }

        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        let instance = unsafe {
            crate::instance::core::get_instance_ref(instance_ptr as *const std::os::raw::c_void)?
        };

        // For 64-bit memory, we still use the same core function but with truncated values
        // since the core function uses u32 parameters
        core::memory_init(
            memory,
            store,
            instance,
            dest_offset as u32,
            data_segment_index as u32,
            src_offset as u32,
            length as u32,
        )
    });
}

/// Check if memory is shared (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeIsShared(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if memory_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null".to_string(),
            });
        }
        if store_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }

        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };

        let is_shared = core::memory_is_shared(memory, store)?;

        Ok(if is_shared { 1u8 } else { 0u8 })
    })
}

// Atomic bitwise operations (i32)

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAndInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_and_i32(memory, store, offset as usize, value)
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicOrInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_or_i32(memory, store, offset as usize, value)
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicXorInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_xor_i32(memory, store, offset as usize, value)
    })
}

// Atomic bitwise operations (i64)

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAndLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_and_i64(memory, store, offset as usize, value)
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicOrLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_or_i64(memory, store, offset as usize, value)
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicXorLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_xor_i64(memory, store, offset as usize, value)
    })
}

// Atomic compare-and-swap operations

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicCompareAndSwapInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    expected: jint,
    new_value: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_compare_and_swap_i32(memory, store, offset as usize, expected, new_value)
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicCompareAndSwapLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    expected: jlong,
    new_value: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_compare_and_swap_i64(memory, store, offset as usize, expected, new_value)
    })
}

// Atomic load operations

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicLoadInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        core::atomic_load_i32(memory, store, offset as usize)
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicLoadLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        core::atomic_load_i64(memory, store, offset as usize)
    })
}

// Atomic store operations

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicStoreInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) {
    jni_utils::jni_try_code(&mut env, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_store_i32(memory, store, offset as usize, value)
    });
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicStoreLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jlong,
) {
    jni_utils::jni_try_code(&mut env, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_store_i64(memory, store, offset as usize, value)
    });
}

// Atomic add operations

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAddInt(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_add_i32(memory, store, offset as usize, value)
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAddLong(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::atomic_add_i64(memory, store, offset as usize, value)
    })
}

// Atomic fence

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicFence(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
) {
    jni_utils::jni_try_code(&mut env, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        core::atomic_fence(memory, store)
    });
}

// Atomic notify

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicNotify(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    count: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        core::atomic_notify(memory, store, offset as usize, count)
    })
}

// Atomic wait operations

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicWait32(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    expected: jint,
    timeout_nanos: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        core::atomic_wait32(memory, store, offset as usize, expected, timeout_nanos)
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicWait64(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    expected: jlong,
    timeout_nanos: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        core::atomic_wait64(memory, store, offset as usize, expected, timeout_nanos)
    })
}

// Bulk memory operations

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryCopy(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    dest_offset: jint,
    src_offset: jint,
    length: jint,
) {
    jni_utils::jni_try_code(&mut env, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::memory_copy(
            memory,
            store,
            dest_offset as usize,
            src_offset as usize,
            length as usize,
        )
    });
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryFill(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jint,
    value: jbyte,
    length: jint,
) {
    jni_utils::jni_try_code(&mut env, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::memory_fill(memory, store, offset as usize, value as u8, length as usize)
    });
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeDataDrop(
    mut env: JNIEnv,
    _class: JClass,
    instance_ptr: jlong,
    data_segment_index: jint,
) {
    jni_utils::jni_try_code(&mut env, || {
        let instance = unsafe {
            crate::instance::core::get_instance_ref(instance_ptr as *const std::os::raw::c_void)?
        };
        core::data_drop(instance, data_segment_index as u32)
    });
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryCopy64(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    dest_offset: jlong,
    src_offset: jlong,
    length: jlong,
) {
    jni_utils::jni_try_code(&mut env, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::memory_copy(
            memory,
            store,
            dest_offset as usize,
            src_offset as usize,
            length as usize,
        )
    });
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryFill64(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
    offset: jlong,
    value: jbyte,
    length: jlong,
) {
    jni_utils::jni_try_code(&mut env, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        core::memory_fill(memory, store, offset as usize, value as u8, length as usize)
    });
}

/// Check if memory supports 64-bit addressing (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeSupports64BitAddressing(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        Ok(if memory.memory_type.is_64() { 1 } else { 0 })
    })
}

/// Get memory page size in bytes (JNI version)
///
/// Returns the page size for this memory, normally 65536 but may differ
/// when the `wasm_custom_page_sizes` engine feature is enabled.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetPageSize(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        Ok(core::get_memory_page_size(memory) as jlong)
    })
}

/// Get memory page size as log2 (JNI version)
///
/// Returns the log2 of the page size (normally 16 for 65536 byte pages).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetPageSizeLog2(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        Ok(core::get_memory_page_size_log2(memory) as jint)
    })
}

/// Get memory data size in bytes (JNI version)
///
/// Returns the current data size of the memory (pages * page_size).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetDataSize(
    mut env: JNIEnv,
    _class: JClass,
    memory_ptr: jlong,
    store_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
        let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
        Ok(core::get_memory_data_size(memory, store)? as jlong)
    })
}
